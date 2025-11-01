/*
 * Lizz Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/LizzBounce/Lizz/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.Lizz.CLIENT_NAME
import net.ccbluex.liquidbounce.Lizz.hud
import net.ccbluex.liquidbounce.config.IntValue
import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.LBPPAnimationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.util.ResourceLocation
import kotlin.arrayOf


object HUD : Module("HUD", Category.RENDER, gameDetecting = false, defaultState = true, defaultHidden = true) {

    private val blur by boolean("Blur", false)
    val animHotbarValue by boolean("AnimatedHotbar", true)
    val blackHotbarValue by boolean("BlackHotbar", true)
    val inventoryParticle by boolean("InventoryParticle", false)

    val inventoryNoBackground by boolean("NoInventoryBackground", false)
    val chatCombineValue by boolean("ChatCombine", true)
    val chatAnimationValue by boolean("ChatAnimation", true)
    val chatAnimationSpeedValue by float("Chat-AnimationSpeed", 0.1F, 0.01F..0.1F)

    val fontChatValue by boolean("FontChat", false)

    val chatRectValue by boolean("ChatRect", true)

    val fontType by font("Font", Fonts.fontSemibold40, { fontChatValue })

    val buttonStyle by choices("Button-Style", arrayOf("Minecraft", "LiquidBounce", "Rounded", "LiquidBounce+"), "LiquidBounce+")

    private var hotBarX = 0F

    val onRender2D = handler<Render2DEvent> {
        if (mc.currentScreen is GuiHudDesigner)
            return@handler

        hud.render(false)
    }

    val onUpdate = handler<UpdateEvent> {
        hud.update()
    }

    val onKey = handler<KeyEvent> { event ->
        hud.handleKey('a', event.key)
    }

    val onScreen = handler<ScreenEvent>(always = true) { event ->
        if (mc.theWorld == null || mc.thePlayer == null) return@handler
        if (state && blur && !mc.entityRenderer.isShaderActive && event.guiScreen != null &&
            !(event.guiScreen is GuiChat || event.guiScreen is GuiHudDesigner)
        ) mc.entityRenderer.loadShader(
            ResourceLocation(CLIENT_NAME.lowercase() + "/blur.json")
        ) else if (mc.entityRenderer.shaderGroup != null &&
            "${CLIENT_NAME.lowercase()}/blur.json" in mc.entityRenderer.shaderGroup.shaderGroupName
        ) mc.entityRenderer.stopUseShader()
    }


    fun getAnimPos(pos: Float): Float {
        if (state && animHotbarValue) hotBarX = LBPPAnimationUtils.animate(pos, hotBarX, 0.02F * RenderUtils.deltaTime)
        else hotBarX = pos

        return hotBarX
    }
}