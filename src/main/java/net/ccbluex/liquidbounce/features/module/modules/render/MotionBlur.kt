package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.util.ResourceLocation

object MotionBlur: Module("MotionBlur", Category.RENDER, forcedDescription = "Motion Blur") {
    private val blurAmount by int("Amount", 6, 1..10, "6")

    override fun onDisable() {
        if (mc.entityRenderer.isShaderActive) mc.entityRenderer.stopUseShader()
    }

    val onTick = handler<GameTickEvent> {
        if (mc.thePlayer != null) {
            if (mc.entityRenderer.shaderGroup == null) mc.entityRenderer.loadShader(
                ResourceLocation(
                    "minecraft",
                    "shaders/post/motion_blur.json"
                )
            )
            val uniform = 1f - (blurAmount / 10f).coerceAtMost(0.9f)
            if (mc.entityRenderer.shaderGroup != null) {
                mc.entityRenderer.shaderGroup.listShaders[0].shaderManager.getShaderUniform("Phosphor")
                    .set(uniform, 0f, 0f)
            }
        }
    }
}