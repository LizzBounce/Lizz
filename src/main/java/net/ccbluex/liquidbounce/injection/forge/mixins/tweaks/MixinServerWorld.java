package net.ccbluex.liquidbounce.injection.forge.mixins.tweaks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Mixin(targets = {
        // Deobfuscated
        "net.minecraft.server.level.ServerLevel", // Official Mojang
        "net.minecraft.server.world.ServerWorld", // Fabric Yarn
        "net.minecraft.world.server.ServerWorld", // Forge MCP

        // Obfuscated
        "net.minecraft.class_3218", // Fabric Intermediary
        "net.minecraft.src.C_12_", // Forge SRG
        "net.minecraft.unmapped.C_bdwnwhiu" // Quilt Hashed
}, remap = false)
@Pseudo
public class MixinServerWorld {
    // Injects into ServerLevel.setDefaultSpawnPos (Mojang mappings) to prevent loading chunks at the spawn after setting it.
    @SuppressWarnings({"UnresolvedMixinReference"})
    @ModifyConstant(method = {
            // Deobfuscated
            "setDefaultSpawnPos(Lnet/minecraft/core/BlockPos;F)V", // Official Mojang
            "setDefaultSpawnPos(Lnet/minecraft/core/BlockPos;)V", // Official Mojang (Old)
            "setSpawnPos(Lnet/minecraft/util/math/BlockPos;F)V", // Fabric Yarn
            "setSpawnPos(Lnet/minecraft/util/math/BlockPos;)V", // Fabric Yarn (Old)
            "setSpawnLocation(Lnet/minecraft/util/math/BlockPos;F)V", // Forge MCP
            "setSpawnLocation(Lnet/minecraft/util/math/BlockPos;)V", // Forge MCP (Old)

            // Obfuscated
            "method_8554(Lnet/minecraft/class_2338;F)V", // Fabric Intermediary
            "method_8554(Lnet/minecraft/class_2338;)V", // Fabric Intermediary (Old)
            "m_8733_(Lnet/minecraft/core/BlockPos;F)V", // Forge SRG (1.20.x)
            "m_8733_(Lnet/minecraft/src/C_4675_;F)V", // Forge SRG (1.17.x)
            "func_241124_a__(Lnet/minecraft/util/math/BlockPos;F)V", // Forge SRG (1.16.x)
            "func_241124_a__(Lnet/minecraft/util/math/BlockPos;)V" // Forge SRG (1.16.x/Old)
    }, constant = @Constant(intValue = 11), remap = false, require = 0, expect = 0)
    public int ksyxis$setDefaultSpawnPos$addRegionTicket(int constant) {
        // Add zero-level ticket.
        return 0;
    }
}
