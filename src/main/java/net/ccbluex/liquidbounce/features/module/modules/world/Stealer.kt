package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.client.C0DPacketCloseWindow  // 修正导入包

object Stealer : Module("Stealer", Category.WORLD) {
    private var chestWindowId = -1

    private val onPacket = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is S30PacketWindowItems -> {
                chestWindowId = packet.func_148911_c()
                if (chestWindowId > 0) {
                    // 发送物品点击包时检查物品数量
                    var hasItems = false
                    packet.itemStacks.forEachIndexed { slot, itemStack ->
                        if (itemStack != null && itemStack.stackSize > 0) {
                            sendPacket(C0EPacketClickWindow(
                                chestWindowId,
                                slot,
                                0,
                                1,
                                null,
                                0.toShort()
                            ))
                            hasItems = true
                        }
                    }

                    // 仅当所有物品为空时关闭（参考ChestStealer.kt逻辑）
                    if (!hasItems && chestWindowId != -1) {
                        sendPacket(C0DPacketCloseWindow(chestWindowId))
                        chestWindowId = -1
                    }
                }
            }
        }
    }
}
