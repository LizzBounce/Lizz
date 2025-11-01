/*
 * Lizz Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/LizzBounce/Lizz/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import org.lwjgl.opengl.GL11.*
import java.awt.Color

/**
 * CustomHUD Armor element
 *
 * Shows a horizontal display of current armor
 */
@ElementInfo(name = "Armor")
class Armor(
    x: Double = -8.0, y: Double = 57.0, scale: Float = 1F,
    side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)
) : Element("Armor", x, y, scale, side) {

    private val modeValue by choices("Alignment", arrayOf("Horizontal", "Vertical"), "Horizontal")
    private val backgroundT by boolean("backGround",true)
    private val background2 by int("backGroundAlpha",100,0..255) {backgroundT}
    private val smallBarColor by color("SmallBarColor", Color(0,0,0,120)) {backgroundT}
    private val showShadow by boolean("Shadow",false) {backgroundT}
    private val shadowStrength by float("ShadowStrength", 1F, 1F..2F) { showShadow }
    private val test by boolean("无论游戏模式都显示",false)

    /**
     * Draw element
     */
    override fun drawElement(): Border {
        if (mc.playerController.isNotCreative || test) {
            if (backgroundT && modeValue == "Horizontal"){
                ShowShadow(-2f,-16f,76f,38f)
                drawRoundedRect(-2f,-1f,74f,21f,Color(0,0,0,background2).rgb,4f, RenderUtils.RoundedCorners.BOTTOM_ONLY)
                drawRoundedRect(-2f,-16f,74f,-1f,smallBarColor.rgb,4f, RenderUtils.RoundedCorners.TOP_ONLY)
                Fonts.fontSemibold40.drawString("Equipment",4f,-12f,Color.WHITE.rgb)
            }
            // 3f 14f(textHeight)
            glPushMatrix()

            val renderItem = mc.renderItem
            val isInsideWater = mc.thePlayer.isInsideOfMaterial(Material.water)

            var x = 1
            var y = if (isInsideWater) -10 else 1

            glColor4f(1F, 1F, 1F, 1F)

            for (index in 3 downTo 0) {
                val stack = mc.thePlayer.inventory.armorInventory[index] ?: continue

                glPushMatrix()
                glEnable(GL_BLEND)
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                enableGUIStandardItemLighting()
                renderItem.renderItemIntoGUI(stack, x, y)
                renderItem.renderItemOverlays(mc.fontRendererObj, stack, x, y)
                disableStandardItemLighting()
                glDisable(GL_BLEND)
                glPopMatrix()

                when (modeValue) {
                    "Horizontal" -> x += 18
                    "Vertical" -> y += 18
                }
            }

            enableAlpha()
            disableBlend()
            disableLighting()
            disableCull()
            glPopMatrix()
        }

        return when (modeValue) {
            "Horizontal" -> Border(0F, 0F, 72F, 17F)
            else -> Border(0F, 0F, 18F, 72F)
        }

    }
    private fun ShowShadow(startX: Float,startY: Float,width: Float,height:Float){
        if (showShadow) {
            GlowUtils.drawGlow(
                startX, startY,
                width, height,
                (shadowStrength * 13F).toInt(),
                Color(0, 0, 0, 120)
            )
        }
    }
}