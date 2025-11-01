package net.ccbluex.liquidbounce.injection.forge.mixins.bugfixes;

import dev.tonimatas.packetfixer.Config;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(C17PacketCustomPayload.class)
public class CPacketCustomPayloadMixin_PacketSize {
    @ModifyConstant(method = "<init>(Ljava/lang/String;Lnet/minecraft/network/PacketBuffer;)V", constant = @Constant(intValue = 32767))
    private int newSize$init(int constant) {
        return Config.getPacketSize();
    }

    @ModifyConstant(method = "readPacketData", constant = @Constant(intValue = 32767))
    private int newSize$readPacketData(int constant) {
        return Config.getPacketSize();
    }
}
