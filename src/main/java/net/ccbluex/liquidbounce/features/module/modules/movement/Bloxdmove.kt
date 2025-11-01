package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S3FPacketCustomPayload
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
object Bloxdmove : Module("BloxdMove", Category.MOVEMENT) {
    private data class PhysicsBody(
        var impulseVector: Vector3 = Vector3(0.0, 0.0, 0.0),
        var forceVector: Vector3 = Vector3(0.0, 0.0, 0.0),
        var velocityVector: Vector3 = Vector3(0.0, 0.0, 0.0),
        val gravityVector: Vector3 = Vector3(0.0, -10.0, 0.0),
        var gravityMul: Double = 2.0,
        val mass: Double = 1.0
    ) {
        fun getMotionForTick(): Vector3 {
            val massDiv = 1.0 / mass

            forceVector.multiply(massDiv)
            forceVector.add(gravityVector)
            forceVector.multiply(gravityMul)

            impulseVector.multiply(massDiv)
            forceVector.multiply(1.0/30)
            impulseVector.add(forceVector)

            velocityVector.add(impulseVector)

            impulseVector.set(0.0, 0.0, 0.0)
            forceVector.set(0.0, 0.0, 0.0)

            return velocityVector
        }
    }

    private data class Vector3(
        var x: Double,
        var y: Double,
        var z: Double
    ) {
        fun add(vec: Vector3) = apply {
            x += vec.x
            y += vec.y
            z += vec.z
        }
        fun multiply(scalar: Double) = apply {
            x *= scalar
            y *= scalar
            z *= scalar
        }
        fun set(x: Double, y: Double, z: Double) = apply {
            this.x = x
            this.y = y
            this.z = z
        }
    }

    private val timer by boolean("Timer", true)
    private val damageBoost by boolean("DamageBoost", false)
    private val physics = PhysicsBody()
    private var bhopJumps = 0
    private var groundTicks = 0
    private val knockbackTimer = MSTimer()
    private var wasClimbing = false

    val onStrafe = handler<StrafeEvent> { event ->
        if (timer) mc.timer.timerSpeed = 1.5f

        if (mc.thePlayer.onGround && physics.velocityVector.y < 0) {
            physics.velocityVector.set(0.0, 0.0, 0.0)
        }

        if (mc.thePlayer.onGround && mc.thePlayer.motionY == 0.41999998688697815) {
            bhopJumps = (bhopJumps + 1).coerceAtMost(3)
            physics.impulseVector.add(Vector3(0.0, 8.0, 0.0))
        }

        if (mc.thePlayer.isCollidedHorizontally && (event.forward != 0f || event.strafe != 0f)) {
            physics.velocityVector.set(0.0, 8.0, 0.0)
            wasClimbing = true
        } else if (wasClimbing) {
            physics.velocityVector.set(0.0, 0.0, 0.0)
            wasClimbing = false
        }

        groundTicks = if (mc.thePlayer.onGround) groundTicks + 1 else 0
        bhopJumps = if (groundTicks > 5) 0 else bhopJumps

        val speed = if (knockbackTimer.hasTimePassed(1300)) {
            if (mc.thePlayer.isUsingItem) 0.06 else 0.26 + 0.025 * bhopJumps
        } else 1.0

        val (forward, strafe) = event.forward.toDouble() to event.strafe.toDouble()
        val sqrt = sqrt(forward * forward + strafe * strafe)
        val (normForward, normStrafe) = if (sqrt > 1) {
            forward/sqrt to strafe/sqrt
        } else forward to strafe

        val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
        val moveDir = Vector3(
            (normStrafe * cos(yaw) - normForward * sin(yaw)) * speed,
            0.0,
            (normForward * cos(yaw) + normStrafe * sin(yaw)) * speed
        )

        event.cancelEvent()
        mc.thePlayer.motionX = moveDir.x
        mc.thePlayer.motionZ = moveDir.z
        mc.thePlayer.motionY = physics.getMotionForTick().y * (1.0/30)
    }

    val onPacket = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is S3FPacketCustomPayload -> {
                packet.bufferData.apply {
                    physics.velocityVector.set(readFloat().toDouble(), readFloat().toDouble(), readFloat().toDouble())
                    bhopJumps = 0
                }
            }
            is S12PacketEntityVelocity -> {
                if (packet.entityID == mc.thePlayer?.entityId) {
                    if (!damageBoost) event.cancelEvent()
                    knockbackTimer.reset()
                }
            }
        }
    }
}