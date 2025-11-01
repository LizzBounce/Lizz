package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold
import net.minecraft.block.BlockAir
import net.minecraft.init.Items
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.world.World
import kotlin.math.*
import kotlin.random.Random

object AutoPearl : Module("AutoPearl", Category.PLAYER) {

    private val auto by boolean("Auto", true)
    private val timerEnabled by boolean("Timer", true)
    private val timerMode by choices("TimerMode", arrayOf("Normal", "Always"), "Normal")
    private val waitTicks by int("WaitTicks", 8, 0..20)
    private val cooldownSeconds by int("NoPearlCooldown", 30, 1..300)
    private val searchRadius by int("SearchRadius", 32, 1..64)
    private val throwRange by float("EntityRange", 25f, 1f..40f)
    private val preferBlockDistance by int("BlockMaxVerticalDiff", 15, 1..40)
    private val cooldownwait by int("AfterCoolDownSec",3,0..10)

    // rotation delay
    private val rotDelayTicks by int("RotDelay", 2, 0..5)

    private val scaffoldOnHit by boolean("ScaffoldOnHit", true)

    // debug
    private val debug by boolean("Debug", false)

    // SA + performance tuning
    private val saIterations by int("SAIterations", 800, 100..5000)
    private val saInitTemp by float("SAInitTemp", 100f, 1f..1000f)
    private val saCooling by float("SACooling", 0.995f, 0.800f..0.999f)

    // runtime simulation limits to avoid overload
    private val maxCandidatesToSimulate = 120 // keep simulations bounded
    private val maxSimSteps = 400 // trajectory sim steps max
    private val unreachableThreshold = 2.5 // blocks: 若 minDist > threshold 则视为难以到达（会被强惩）
    private val reachWeight = 12.0 // 权重，将 minDistance 转换为评分惩罚

    // internal state
    private var cooldownUntil = 0L

    private var waitingActive = false
    private var waitingCounter = 0

    private var startTimerAfterWait = false
    private var performAfterWait = false

    private var timerActive = false
    private var timerStartedByThrow = false

    private var pendingThrowTarget: Vec3? = null
    private var pendingThrowTicks = 0

    private var pearlThrown = false
    private var scaffoldPendingTicks = 0
    private var scaffoldTriggeredByHit = false
    private var scaffoldAutoEnabledByThis = false

    private val PLAYER_LOWEST_Y = -64
    private val BLOCK_EDGE = 0.3

    override fun onDisable() {
        if (timerStartedByThrow) timerStartedByThrow = false
        if (timerActive) {
            mc.timer.timerSpeed = 1F
            timerActive = false
        }
        try { SilentHotbar.resetSlot(this) } catch (_: Throwable) {}
        pendingThrowTarget = null
        pendingThrowTicks = 0
        waitingActive = false
        waitingCounter = 0
        cooldownUntil = 0L

        if (scaffoldAutoEnabledByThis) {
            try { Scaffold.state = false } catch (_: Throwable) {}
        }
        scaffoldPendingTicks = 0
        scaffoldTriggeredByHit = false
        scaffoldAutoEnabledByThis = false
    }

    private fun isAcceptableBlockForPearl(w: World, pos: BlockPos): Boolean {
        val bs = try { w.getBlockState(pos) } catch (_: Throwable) { return false }
        val b = bs.block ?: return false

        if (b is BlockAir) return false
        val mat = b.material
        if (mat.isLiquid) return false

        return true
    }

    private fun isAirOrNonStandable(w: World, pos: BlockPos): Boolean {
        val bs = try { w.getBlockState(pos) } catch (_: Throwable) { return true }
        val b = bs.block ?: return true
        val mat = b.material
        return !mat.isSolid || mat.isLiquid
    }

    /**
     * 计算用于投掷的 yaw/pitch（基于抛物线公式）。
     * 返回 Pair<yawDeg, pitchDeg> 或 null (若数学上无可行角度)
     */
    private fun computeThrowAnglesForTarget(target: Vec3): Pair<Float, Float>? {
        val player = mc.thePlayer ?: return null
        val eyeY = player.posY + player.eyeHeight

        val dx = target.xCoord - player.posX
        val dz = target.zCoord - player.posZ
        val dHorizontal = sqrt(dx * dx + dz * dz)
        val dy = target.yCoord - eyeY

        val yaw = Math.toDegrees(atan2(dz, dx)).toFloat() - 90f

        val v = 0.4 // motionFactor for pearl
        val g = 0.03

        val v2 = v * v
        val under = v2 * v2 - g * (g * dHorizontal * dHorizontal + 2.0 * dy * v2)
        if (under < 0.0) return null

        val sqrtTerm = sqrt(under)
        val tanTheta1 = (v2 + sqrtTerm) / (g * dHorizontal)
        val tanTheta2 = (v2 - sqrtTerm) / (g * dHorizontal)

        val theta1 = atan(tanTheta1)
        val theta2 = atan(tanTheta2)
        val chosenTheta = if (abs(theta1) < abs(theta2)) theta1 else theta2
        val pitch = (-Math.toDegrees(chosenTheta)).toFloat()
        return Pair(yaw, pitch)
    }

    /**
     * 模拟轨迹并返回目标点的最小距离（blocks）。
     * 返回的值越小越好；0 表示命中或非常接近。
     * 若在到达目标前被不可穿透方块阻挡，则返回阻挡点到目标的距离。
     */
    private fun simulateTrajectoryMinDistance(target: Vec3, yawDeg: Float, pitchDeg: Float, w: World): Double {
        val player = mc.thePlayer ?: return Double.MAX_VALUE
        val yawRad = Math.toRadians(yawDeg.toDouble())
        val pitchRad = Math.toRadians(pitchDeg.toDouble())

        var posX = player.posX - cos(yawRad) * 0.16
        var posY = player.posY + player.eyeHeight - 0.10000000149011612
        var posZ = player.posZ - sin(yawRad) * 0.16

        var motionX = -sin(yawRad) * cos(pitchRad)
        var motionY = -sin(pitchRad)
        var motionZ = cos(yawRad) * cos(pitchRad)

        val len = sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ)
        if (len == 0.0) return Double.MAX_VALUE
        val motionFactor = 0.4
        motionX = motionX / len * motionFactor
        motionY = motionY / len * motionFactor
        motionZ = motionZ / len * motionFactor

        val gravity = 0.03
        val motionSlowdown = 0.99

        var minDistSq = Double.MAX_VALUE
        val targetX = target.xCoord
        val targetY = target.yCoord
        val targetZ = target.zCoord
        val thresholdSq = 0.6 * 0.6

        for (i in 0 until maxSimSteps) {
            val posBefore = Vec3(posX, posY, posZ)
            val posAfter = Vec3(posX + motionX, posY + motionY, posZ + motionZ)

            val trace = w.rayTraceBlocks(posBefore, posAfter, false, true, false)
            if (trace != null) {
                val hit = trace.hitVec
                val dx = hit.xCoord - targetX
                val dy = hit.yCoord - targetY
                val dz = hit.zCoord - targetZ
                val distSq = dx * dx + dy * dy + dz * dz
                if (distSq <= thresholdSq) return 0.0
                // blocked before reaching target -> use distance to block hit point
                val d = sqrt(distSq)
                return d
            }

            posX += motionX
            posY += motionY
            posZ += motionZ

            val dxT = posX - targetX
            val dyT = posY - targetY
            val dzT = posZ - targetZ
            val curDistSq = dxT * dxT + dyT * dyT + dzT * dzT
            if (curDistSq < minDistSq) minDistSq = curDistSq

            val mat = try { w.getBlockState(BlockPos(posX, posY, posZ)).block.material } catch (_: Throwable) { null }
            if (mat != null && mat.isLiquid) {
                motionX *= 0.6
                motionY *= 0.6
                motionZ *= 0.6
            } else {
                motionX *= motionSlowdown
                motionY *= motionSlowdown
                motionZ *= motionSlowdown
            }

            motionY -= gravity
            if (posY <= 0.0) break
        }

        return if (minDistSq == Double.MAX_VALUE) Double.MAX_VALUE else sqrt(minDistSq)
    }

    /**
     * 使用 SA 与抛物线可达性过滤来选择最佳方块目标。
     * 进行了候选截断与自适应 SA 迭代控制以降低负载。
     */
    private fun findBestBlockTarget(w: World, maxRadius: Int, maxVertDiff: Int): Triple<BlockPos, Vec3, Boolean>? {
        val player = mc.thePlayer ?: return null
        val px = player.posX.toInt()
        val pz = player.posZ.toInt()
        val py = player.posY.toInt()

        data class Candidate(
            val blockPos: BlockPos,
            val target: Vec3,
            val isSide: Boolean,
            val neighborCount: Int,
            val isAbsolute: Boolean,
            val distance: Double,
            var minDist: Double = Double.MAX_VALUE // from simulateTrajectoryMinDistance
        )

        val candidatesAll = ArrayList<Candidate>()

        val minY = (py - maxVertDiff).coerceAtLeast(0)
        val maxY = (py + maxVertDiff).coerceAtMost(255)
        for (dx in -maxRadius..maxRadius) {
            for (dz in -maxRadius..maxRadius) {
                for (y in minY..maxY) {
                    val pos = BlockPos(px + dx, y, pz + dz)
                    if (!isAcceptableBlockForPearl(w, pos)) continue

                    val above = BlockPos(pos.x, pos.y + 1, pos.z)
                    if (!isAirOrNonStandable(w, above)) continue

                    val neighborsPos = arrayOf(
                        BlockPos(pos.x + 1, pos.y, pos.z),
                        BlockPos(pos.x - 1, pos.y, pos.z),
                        BlockPos(pos.x, pos.y, pos.z + 1),
                        BlockPos(pos.x, pos.y, pos.z - 1)
                    )
                    var neighborCount = 0
                    var isAbsolute = true
                    for (n in neighborsPos) {
                        if (isAcceptableBlockForPearl(w, n)) {
                            val aboveN = BlockPos(n.x, n.y + 1, n.z)
                            if (isAirOrNonStandable(w, aboveN)) {
                                neighborCount++
                            } else {
                                isAbsolute = false
                            }
                        } else {
                            isAbsolute = false
                        }
                    }

                    val tx = pos.x + 0.5
                    val ty = pos.y + 1.0
                    val tz = pos.z + 0.5
                    val dist = sqrt((tx - player.posX) * (tx - player.posX) + (ty - player.posY) * (ty - player.posY) + (tz - player.posZ) * (tz - player.posZ))

                    candidatesAll.add(Candidate(pos, Vec3(tx, ty, tz), false, neighborCount, isAbsolute, dist))
                }
            }
        }

        // pillar-side candidates
        val dirs = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
        for (dx in -maxRadius..maxRadius) {
            for (dz in -maxRadius..maxRadius) {
                val cx = px + dx
                val cz = pz + dz

                var y = (py - maxVertDiff).coerceAtLeast(0)
                while (y <= (py + maxVertDiff).coerceAtMost(255)) {
                    if (!isAcceptableBlockForPearl(w, BlockPos(cx, y, cz))) {
                        y++
                        continue
                    }

                    var h = y
                    while (h + 1 <= 255 && isAcceptableBlockForPearl(w, BlockPos(cx, h + 1, cz))) h++

                    for (level in y..h) {
                        val colPos = BlockPos(cx, level, cz)
                        for (dir in dirs) {
                            val nx = cx + dir[0]
                            val nz = cz + dir[1]

                            val neighborTop = BlockPos(nx, level, nz)
                            val aboveNeighbor = BlockPos(nx, level + 1, nz)
                            if (isAcceptableBlockForPearl(w, neighborTop) && isAirOrNonStandable(w, aboveNeighbor)) {
                                val tx = neighborTop.x + 0.5
                                val ty = neighborTop.y + 1.0
                                val tz = neighborTop.z + 0.5
                                val dist = sqrt((tx - player.posX) * (tx - player.posX) + (ty - player.posY) * (ty - player.posY) + (tz - player.posZ) * (tz - player.posZ))
                                var neighborCount = 0
                                val neighborsPos = arrayOf(
                                    BlockPos(neighborTop.x + 1, neighborTop.y, neighborTop.z),
                                    BlockPos(neighborTop.x - 1, neighborTop.y, neighborTop.z),
                                    BlockPos(neighborTop.x, neighborTop.y, neighborTop.z + 1),
                                    BlockPos(neighborTop.x, neighborTop.y, neighborTop.z - 1)
                                )
                                for (n in neighborsPos) {
                                    if (isAcceptableBlockForPearl(w, n) && isAirOrNonStandable(w, BlockPos(n.x, n.y + 1, n.z))) neighborCount++
                                }
                                candidatesAll.add(Candidate(colPos, Vec3(tx, ty, tz), true, neighborCount, false, dist))
                                continue
                            }

                            val faceX = cx + 0.5 + dir[0] * 0.25
                            val faceZ = cz + 0.5 + dir[1] * 0.25
                            val faceY = level + 0.5
                            val tx = faceX
                            val ty = faceY
                            val tz = faceZ
                            val dist = sqrt((tx - player.posX) * (tx - player.posX) + (ty - player.posY) * (ty - player.posY) + (tz - player.posZ) * (tz - player.posZ))
                            candidatesAll.add(Candidate(colPos, Vec3(tx, ty, tz), true, 0, false, dist))
                        }
                    }
                    y = h + 1
                }
            }
        }

        if (candidatesAll.isEmpty()) {
            if (debug) chat("AutoPearl Debug: no block candidates")
            return null
        }

        // 预筛选：按启发式（距离、neighborCount、isAbsolute）排序并截断，以减少昂贵模拟次数
        candidatesAll.sortWith(compareBy({ it.distance }, { -it.neighborCount }, { if (it.isAbsolute) 0 else 1 }))
        val toSimulate = candidatesAll.take(maxCandidatesToSimulate).toMutableList()

        // 对每个 candidate 进行模拟，得到 minDistance（较小越好）
        for (c in toSimulate) {
            val angles = computeThrowAnglesForTarget(c.target)
            c.minDist = if (angles == null) {
                Double.MAX_VALUE
            } else {
                try {
                    simulateTrajectoryMinDistance(c.target, angles.first, angles.second, w)
                } catch (_: Throwable) {
                    Double.MAX_VALUE
                }
            }
            if (debug) {
                val d = if (c.minDist.isFinite()) String.format("%.3f", c.minDist) else "inf"
                chat("AutoPearl Debug: candidate at ${c.blockPos.x},${c.blockPos.y},${c.blockPos.z} minDist=$d neighbors=${c.neighborCount} abs=${c.isAbsolute} side=${c.isSide}")
            }
        }

        // score 函数（越低越好），使用连续 minDist 惩罚
        fun score(c: Candidate): Double {
            var s = c.distance
            if (c.isAbsolute) s -= 80.0
            s -= c.neighborCount * 8.0
            if (c.isSide) s += 30.0
            s += (c.blockPos.y - py) * 0.5

            // 连续可达性惩罚（minDist 越小越好）
            val minD = c.minDist
            val reachPenalty = when {
                !minD.isFinite() -> 1e5
                minD <= 0.25 -> 0.0
                else -> minD * reachWeight
            }
            s += reachPenalty

            // 强烈惩罚显然不可达的候选（但仍以连续方式表现）
            if (minD > unreachableThreshold) s += 200.0 + (minD - unreachableThreshold) * 30.0

            return s
        }

        // 如果模拟集合为空（极少发生），退回候选集合做简单选择
        if (toSimulate.isEmpty()) {
            val fallback = candidatesAll.minByOrNull { it.distance } ?: return null
            return Triple(fallback.blockPos, fallback.target, fallback.isSide)
        }

        // SA 初始化（从 best-by-score 的 candidate 开始）
        var best = toSimulate.minByOrNull { score(it) }!!
        var current = best
        var bestScore = score(best)
        var currentScore = bestScore

        var T = saInitTemp.toDouble()
        val cooling = saCooling.toDouble()
        // 自适应迭代次数，避免过多计算
        val iterations = min(saIterations, max(200, toSimulate.size * 30))

        val rnd = Random

        for (i in 0 until iterations) {
            val cand = toSimulate[rnd.nextInt(toSimulate.size)]
            val candScore = score(cand)
            val delta = candScore - currentScore
            if (delta < 0 || exp(-delta / T) > rnd.nextDouble()) {
                current = cand
                currentScore = candScore
            }
            if (currentScore < bestScore) {
                best = current
                bestScore = currentScore
            }
            T *= cooling
            if (T < 1e-8) break
        }

        if (debug) {
            val md = if (best.minDist.isFinite()) String.format("%.3f", best.minDist) else "inf"
            chat("AutoPearl Debug: selected pos=${best.blockPos.x},${best.blockPos.y},${best.blockPos.z} score=${String.format("%.3f", bestScore)} minDist=$md neighbors=${best.neighborCount} abs=${best.isAbsolute}")
        }

        // 最终如果 minDist 很大（不可达），返回 null
        return if (best.minDist.isFinite() && best.minDist <= unreachableThreshold + 0.5) {
            Triple(best.blockPos, best.target, best.isSide)
        } else {
            if (debug) chat("AutoPearl Debug: best candidate unreachable (minDist=${best.minDist}), skipping")
            null
        }
    }

    private fun computeYawPitchTo(targetX: Double, targetY: Double, targetZ: Double): Pair<Float, Float> {
        val player = mc.thePlayer ?: return Pair(0f, 0f)
        val dx = targetX - player.posX
        val dy = targetY - (player.posY + player.eyeHeight)
        val dz = targetZ - player.posZ
        val dist = sqrt(dx * dx + dz * dz)
        val yaw = Math.toDegrees(Math.atan2(dz, dx)).toFloat() - 90f
        val pitch = (-Math.toDegrees(Math.atan2(dy, dist))).toFloat()
        return Pair(yaw, pitch)
    }

    private fun startTimerMode() {
        if (!timerEnabled) return
        if (!timerActive) {
            mc.timer.timerSpeed = 0.1F
            timerActive = true
        }
    }

    private fun stopTimerModeIfActive(withMessage: Boolean = true) {
        if (timerActive) {
            mc.timer.timerSpeed = 1F
            timerActive = false
            if (withMessage) chat("Land Successfully! Cancel Timer")
        }
    }

    private fun performImmediateThrowAt(targetX: Double, targetY: Double, targetZ: Double) {
        val player = mc.thePlayer ?: return
        val target = Vec3(targetX, targetY, targetZ)
        val angles = computeThrowAnglesForTarget(target) ?: computeYawPitchTo(targetX, targetY, targetZ)
        player.rotationYaw = angles.first
        player.rotationPitch = angles.second

        mc.playerController.syncCurrentPlayItem()

        val held = player.heldItem ?: return
        try {
            mc.playerController.sendUseItem(player, mc.theWorld, held)
            pearlThrown = true
            if (timerEnabled && timerMode == "Normal") {
                timerActive = true
                timerStartedByThrow = true
                try { mc.timer.timerSpeed = 0.2f } catch (_: Throwable) {}
            }
            if (debug) chat("AutoPearl Debug: thrown at $targetX,$targetY,$targetZ using angles ${angles.first},${angles.second}")
        } catch (_: Throwable) {}

        try { SilentHotbar.resetSlot(this) } catch (_: Throwable) {}
    }

    private fun handlePearlAction(world: World?) {
        val player = mc.thePlayer ?: return
        val w = world ?: mc.theWorld ?: return

        val pearlSlotInv = InventoryUtils.findItemArray(36, 44, arrayOf(Items.ender_pearl))
            ?: InventoryUtils.findItemArray(0, 8, arrayOf(Items.ender_pearl))

        if (pearlSlotInv == null) {
            cooldownUntil = System.currentTimeMillis() + cooldownSeconds * 1000L
            chat("No Pearl!")
            return
        }

        val hotbarSlot = when {
            pearlSlotInv in 36..44 -> pearlSlotInv - 36
            pearlSlotInv in 0..8 -> pearlSlotInv
            else -> (pearlSlotInv % 9)
        }

        try {
            SilentHotbar.selectSlotSilently(this, hotbarSlot, render = false, resetManually = true)
        } catch (_: Throwable) {
            player.inventory.currentItem = hotbarSlot
            mc.playerController.syncCurrentPlayItem()
        }

        if (timerEnabled && timerMode.equals("Always", true) && !auto) {
            startTimerMode()
        }

        if (auto) {
            val triple = findBestBlockTarget(w, searchRadius, preferBlockDistance)
            if (triple != null) {
                val targetVec = triple.second
                val angles = computeThrowAnglesForTarget(targetVec)
                if (angles == null) {
                    if (debug) chat("AutoPearl Debug: fallback no angle for block")
                    pendingThrowTarget = targetVec
                    pendingThrowTicks = rotDelayTicks
                    return
                }

                val playerObj = mc.thePlayer ?: return
                playerObj.rotationYaw = angles.first
                playerObj.rotationPitch = angles.second
                mc.playerController.syncCurrentPlayItem()

                if (rotDelayTicks <= 0) {
                    performImmediateThrowAt(targetVec.xCoord, targetVec.yCoord, targetVec.zCoord)
                    cooldownUntil = System.currentTimeMillis() + cooldownwait*1000L
                } else {
                    pendingThrowTarget = targetVec
                    pendingThrowTicks = rotDelayTicks
                }
                return
            }

            val ray = raycastEntity(throwRange.toDouble()) { it != player }
            val targetEntity = if (ray is net.minecraft.entity.Entity) ray else null

            if (targetEntity != null) {
                val tx = targetEntity.posX
                val ty = targetEntity.posY + (targetEntity.eyeHeight / 2.0)
                val tz = targetEntity.posZ

                val angles = computeThrowAnglesForTarget(Vec3(tx, ty, tz))
                if (angles != null) {
                    val playerObj = mc.thePlayer ?: return
                    playerObj.rotationYaw = angles.first
                    playerObj.rotationPitch = angles.second
                    mc.playerController.syncCurrentPlayItem()

                    if (rotDelayTicks <= 0) {
                        performImmediateThrowAt(tx, ty, tz)
                        cooldownUntil = System.currentTimeMillis() + cooldownwait*1000L
                    } else {
                        pendingThrowTarget = Vec3(tx, ty, tz)
                        pendingThrowTicks = rotDelayTicks
                    }
                    if (debug) chat("AutoPearl Debug: targeting entity ${targetEntity.name}")
                    return
                } else {
                    val (yaw, pitch) = computeYawPitchTo(tx, ty, tz)
                    val playerObj = mc.thePlayer ?: return
                    playerObj.rotationYaw = yaw
                    playerObj.rotationPitch = pitch
                    mc.playerController.syncCurrentPlayItem()
                    if (rotDelayTicks <= 0) {
                        performImmediateThrowAt(tx, ty, tz)
                        cooldownUntil = System.currentTimeMillis() + cooldownwait*1000L
                    } else {
                        pendingThrowTarget = Vec3(tx, ty, tz)
                        pendingThrowTicks = rotDelayTicks
                    }
                    return
                }
            }

            cooldownUntil = System.currentTimeMillis() + cooldownSeconds * 1000L
            chat("没救了")
            try { SilentHotbar.resetSlot(this) } catch (_: Throwable) {}
            return
        } else {
            cooldownUntil = System.currentTimeMillis() + 500L
            return
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        if (timerActive) {
            if (player.onGround || player.hurtTime > 0) {
                if (timerStartedByThrow) timerStartedByThrow = false
                stopTimerModeIfActive(true)
            }
        }
        if (player.onGround) {
            pearlThrown = false
        }

        if (pendingThrowTarget != null && pendingThrowTicks > 0) {
            if (player.onGround || player.hurtTime > 0) {
                pendingThrowTarget = null
                pendingThrowTicks = 0
                try { SilentHotbar.resetSlot(this) } catch (_: Throwable) {}
            } else {
                pendingThrowTicks--
                if (pendingThrowTicks <= 0) {
                    val t = pendingThrowTarget
                    pendingThrowTarget = null
                    pendingThrowTicks = 0
                    if (t != null) {
                        performImmediateThrowAt(t.xCoord, t.yCoord, t.zCoord)
                        cooldownUntil = System.currentTimeMillis() + cooldownwait*1000L
                    }
                }
            }
        }

        if (scaffoldOnHit) {
            if (player.hurtTime > 0 && !scaffoldTriggeredByHit && pearlThrown && !player.onGround) {
                scaffoldTriggeredByHit = true
                scaffoldPendingTicks = 1
            }

            if (scaffoldPendingTicks > 0) {
                scaffoldPendingTicks--
                if (scaffoldPendingTicks <= 0 && scaffoldTriggeredByHit) {
                    val under = BlockPos(player.posX.toInt(), (player.posY - 1.0).toInt(), player.posZ.toInt())
                    val bs = try { world.getBlockState(under) } catch (_: Throwable) { null }
                    val hasBlockUnder = bs?.block != null && bs.block.material.isSolid && bs.block !is BlockAir
                    if (!hasBlockUnder) {
                        try {
                            Scaffold.state = true
                            scaffoldAutoEnabledByThis = true
                            if (debug) chat("AutoPearl Debug: scaffold enabled due to hit.")
                        } catch (_: Throwable) {}
                    } else {
                        scaffoldTriggeredByHit = false
                    }
                }
            }

            if (scaffoldAutoEnabledByThis) {
                val under = BlockPos(player.posX.toInt(), (player.posY - 1.0).toInt(), player.posZ.toInt())
                val bs = try { world.getBlockState(under) } catch (_: Throwable) { null }
                val hasBlockUnder = bs?.block != null && bs.block.material.isSolid && bs.block !is BlockAir
                if (hasBlockUnder) {
                    try { Scaffold.state = false } catch (_: Throwable) {}
                    scaffoldAutoEnabledByThis = false
                    scaffoldTriggeredByHit = false
                    pearlThrown = false
                    if (debug) chat("AutoPearl Debug: scaffold disabled after landing.")
                }
            }
        }

        if (cooldownUntil > System.currentTimeMillis()) {
            waitingActive = false
            waitingCounter = 0
            return@handler
        }

        val isAboveVoid = aboveVoid(world)
        if (!isAboveVoid) {
            waitingActive = false
            waitingCounter = 0
            startTimerAfterWait = false
            performAfterWait = false
            return@handler
        }

        if (!waitingActive) {
            if (waitTicks > 0) {
                waitingActive = true
                waitingCounter = 0
                startTimerAfterWait = (timerMode.equals("Always", true) && auto)
                performAfterWait = true
                if (timerMode.equals("Always", true) && !auto) {
                    startTimerMode()
                }
                return@handler
            } else {
                if (timerMode.equals("Always", true) && !auto) {
                    startTimerMode()
                }
                handlePearlAction(world)
                return@handler
            }
        } else {
            waitingCounter++
            if (waitingCounter < waitTicks) {
                return@handler
            }
            waitingActive = false
            waitingCounter = 0

            if (startTimerAfterWait) startTimerMode()
            if (performAfterWait) {
                handlePearlAction(world)
            }
            performAfterWait = false
            startTimerAfterWait = false
        }
    }

    val onWorld = handler<WorldEvent> {
        stopTimerModeIfActive(false)
        cooldownUntil = 0L
        waitingActive = false
        waitingCounter = 0
        startTimerAfterWait = false
        performAfterWait = false
        pendingThrowTarget = null
        pendingThrowTicks = 0

        if (scaffoldAutoEnabledByThis) {
            try { Scaffold.state = false } catch (_: Throwable) {}
            scaffoldAutoEnabledByThis = false
            scaffoldTriggeredByHit = false
            scaffoldPendingTicks = 0
        }

        try { SilentHotbar.resetSlot(this) } catch (_: Throwable) {}
    }

    private fun aboveVoid(world: World?): Boolean {
        val player = mc.thePlayer ?: return false
        val w = world ?: mc.theWorld ?: return false

        if (player.onGround) return false

        val xRange = mutableListOf(0)
        val zRange = mutableListOf(0)
        if (player.posX - floor(player.posX) <= BLOCK_EDGE) {
            xRange.add(-1)
        } else if (ceil(player.posX) - player.posX <= BLOCK_EDGE) {
            xRange.add(1)
        }
        if (player.posZ - floor(player.posZ) <= BLOCK_EDGE) {
            zRange.add(-1)
        } else if (ceil(player.posZ) - player.posZ <= BLOCK_EDGE) {
            zRange.add(1)
        }

        val lastGroundY = (player.posY.toInt() - 1).coerceAtLeast(PLAYER_LOWEST_Y)
        val minY = PLAYER_LOWEST_Y
        for (xOffset in xRange) {
            for (zOffset in zRange) {
                for (y in minY..lastGroundY) {
                    val bs = try { w.getBlockState(BlockPos(player.posX.toInt() + xOffset, y, player.posZ.toInt() + zOffset)) } catch (_: Throwable) { null }
                    val b = bs?.block ?: continue
                    if (b.material.isSolid && b !is BlockAir) {
                        return false
                    }
                }
            }
        }
        return true
    }
}
