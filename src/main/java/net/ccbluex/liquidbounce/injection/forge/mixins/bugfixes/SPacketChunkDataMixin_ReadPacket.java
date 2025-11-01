package net.ccbluex.liquidbounce.injection.forge.mixins.bugfixes;

import dev.tonimatas.packetfixer.Config;
import net.minecraft.network.play.server.S21PacketChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(S21PacketChunkData.class)
public class SPacketChunkDataMixin_ReadPacket {
    @ModifyConstant(method = "readPacketData", constant = @Constant(intValue = 2097152))
    private int init$size(int value) {
        return Config.getChunkPacketData();
    }

    @ModifyConstant(method = "readPacketData", constant = @Constant(stringValue = "Chunk Packet trying to allocate too much memory on read."))
    private String init$message(String constant) {
        return constant + " (" + Config.getChunkPacketData() + ") Modify it in the Packet Fixer config.";
    }
}
