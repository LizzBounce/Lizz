/*
 * Lizz Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/LizzBounce/Lizz/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class MixinWorld {
    @Shadow
    public abstract IBlockState getBlockState(BlockPos pos);

    @SuppressWarnings({"UnresolvedMixinReference"})
    @Inject(method = "isSpawnChunk", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    public void ksyxis$isSpawnChunk$head(int p_isSpawnChunk_1_, int p_isSpawnChunk_2_, CallbackInfoReturnable<Boolean> cir) {
        // Never spawn chunk.
        cir.setReturnValue(false);
    }
}