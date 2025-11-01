package net.ccbluex.liquidbounce.injection.forge.mixins.bugfixes;

import dev.tonimatas.packetfixer.Config;
import net.minecraft.network.NettyCompressionDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NettyCompressionDecoder.class)
public class NettyCompressionDecoderMixin_DecodeFix {
    @ModifyConstant(method = "decode", constant = @Constant(intValue = 2097152))
    private int newSize$decode(int constant) {
        return Config.getDecoderSize();
    }
}
