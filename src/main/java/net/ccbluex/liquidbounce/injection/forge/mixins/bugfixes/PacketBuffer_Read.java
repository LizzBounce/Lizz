package net.ccbluex.liquidbounce.injection.forge.mixins.bugfixes;

import dev.tonimatas.packetfixer.Config;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PacketBuffer.class)
public class PacketBuffer_Read {
    @ModifyConstant(method = "readNBTTagCompoundFromBuffer", constant = @Constant(longValue = 2097152L))
    private long newSize$readCompoundTag(long constant) {
        return Config.getNbtMaxSize();
    }

    @ModifyConstant(method = "readVarIntFromBuffer", constant = @Constant(intValue = 5))
    private int readInt$size(int value) {
        return Config.getVarIntSize();
    }

    @ModifyConstant(method = "readVarIntFromBuffer", constant = @Constant(stringValue = "VarInt too big"))
    private String readInt$size(String constant) {
        return "VarInt too big. Packet Fixer configured to " + Config.getVarIntSize() + ". You can modify it in the Packet Fixer config.";
    }

    @ModifyConstant(method = "readVarLong", constant = @Constant(intValue = 10))
    private int readLong$size(int constant) {
        return Config.getVarLong();
    }

    @ModifyConstant(method = "readVarLong", constant = @Constant(stringValue = "VarLong too big"))
    private String readLong$size(String constant) {
        return "VarInt too big. Packet Fixer configured to " + Config.getVarLong() + ". You can modify it in the Packet Fixer config.";
    }
}
