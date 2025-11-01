package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.server.*

object VelocityBalancer : Module("VelocityBalancer", Category.MOVEMENT) {

    private data class DelayedPacket(val packet: Packet<*>, val time: Long)
    private val packetDelay by int("PacketDelayTicks",20,0..60)
    private val packetQueue = ArrayList<DelayedPacket>()

    override fun onEnable() {
        packetQueue.clear()
    }

    override fun onDisable() {
        flushAll()
    }

    val onRender2D = handler<Render2DEvent> {
        val sr = ScaledResolution(mc)
        val centerX = sr.scaledWidth / 2
        val centerY = sr.scaledHeight / 2

        val delayTicks = packetDelay
        val delayMillis = delayTicks * 50L
        val now = System.currentTimeMillis()

        if (packetQueue.isEmpty()) return@handler

        val firstPacketTime = packetQueue.firstOrNull()?.time ?: return@handler
        val remainingMillis = delayMillis - (now - firstPacketTime)
        val remainingTicks = (remainingMillis / 50L).coerceAtLeast(0)

        val info1 = "VelocityQueue: ${packetQueue.size}"
        val info2 = "NextSend: $remainingTicks ticks"

        mc.fontRendererObj.drawStringWithShadow(
            info1,
            centerX - mc.fontRendererObj.getStringWidth(info1) / 2f,
            centerY + 60f,
            0xFFFFFF
        )
        mc.fontRendererObj.drawStringWithShadow(
            info2,
            centerX - mc.fontRendererObj.getStringWidth(info2) / 2f,
            centerY + 70f,
            0xFFFFFF
        )
    }
    val onPacket = handler<PacketEvent> { event->
        if (event.eventType != EventState.RECEIVE || mc.thePlayer == null || mc.netHandler == null || mc.theWorld == null) return@handler

        val packet = event.packet

        when (packet) {
            is S00PacketKeepAlive,
            is S32PacketConfirmTransaction,
            is S08PacketPlayerPosLook,
            is S40PacketDisconnect -> return@handler

            is S06PacketUpdateHealth -> {
                if (packet.health <= 0) {
                    packetQueue.clear()
                    return@handler
                }
            }

            is S29PacketSoundEffect -> {
                if (packet.soundName == "game.player.hurt") return@handler
            }
        }

        event.cancelEvent()
        packetQueue.add(DelayedPacket(packet, System.currentTimeMillis()))
    }

    val onUpdate = handler<UpdateEvent> {
        val currentTime = System.currentTimeMillis()
        val netHandler = MinecraftInstance.mc.netHandler ?: return@handler

        val iterator = packetQueue.iterator()
        while (iterator.hasNext()) {
            val delayedPacket = iterator.next()
            val delay = packetDelay*50L
            if (currentTime - delayedPacket.time >= delay) {
                try {
                    chat("Sending delayed packet: ${delayedPacket.packet.javaClass.simpleName}")
                    @Suppress("UNCHECKED_CAST")
                    (delayedPacket.packet as Packet<INetHandlerPlayClient>).processPacket(netHandler)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                iterator.remove()
            }
        }
    }

    private fun flushAll() {
        val netHandler = MinecraftInstance.mc.netHandler ?: return
        for (delayedPacket in packetQueue) {
            try {
                @Suppress("UNCHECKED_CAST")
                (delayedPacket.packet as Packet<INetHandlerPlayClient>).processPacket(netHandler)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        packetQueue.clear()
    }
}