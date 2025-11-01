/*
 * Lizz Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/LizzBounce/Lizz/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.Lizz.CLIENT_NAME
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.inventory.inventorySlot
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect2
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import org.lwjgl.opengl.GL11.*
import java.awt.Color

@ElementInfo(name = "Inventory")
class Inventory : Element("Inventory", 300.0, 50.0) {

    private val font by font("Font", Fonts.fontSemibold35)
    private val title by choices("Title", arrayOf("Center", "Left", "Right", "None"), "Left")
    private val titleColor = color("TitleColor", Color.WHITE) { title != "None" }
    private val roundedRectRadius by float("Rounded-Radius", 3F, 0F..6F)

    private val borderValue by boolean("Border", true)
    private val borderColor = color("BorderColor", Color.WHITE) { borderValue }
    private val backgroundColor by color("BackgroundColor", Color.BLACK.withAlpha(150))
    private val lizzBar by boolean("${CLIENT_NAME}Bar",true)
    private val Shadow by boolean("Shadow",false)
    private val shadowStrength by float("ShadowStrength", 1F, 1F..2F) { Shadow }

    private val width = 174F
    private val height = 66F
    private val padding = 6F

    override fun drawElement(): Border {
        val font = font
        val startY = if (title != "None") -(padding + font.FONT_HEIGHT) else 0F
        val borderColor = borderColor.selectedColor()
        val titleColor = titleColor.selectedColor()

        // Draw rectangle and borders
        drawRoundedRect2(0F, startY, width, height, backgroundColor, roundedRectRadius)
        ShowShadow(0F, startY, width, height+12F)

        // Reset color
        resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        val invDisplayName = mc.thePlayer.inventory.displayName.formattedText
        if (lizzBar) drawRoundedRect(0F,startY,width,4F,Color(77,139,199,120).rgb,roundedRectRadius, RenderUtils.RoundedCorners.TOP_ONLY)
        when (title.lowercase()) {
            "center" -> font.drawString(
                invDisplayName,
                width / 2 - font.getStringWidth(invDisplayName) / 2F,
                -(font.FONT_HEIGHT).toFloat(),
                titleColor.rgb,
                false
            )

            "left" -> font.drawString(invDisplayName, padding, -(font.FONT_HEIGHT).toFloat(), titleColor.rgb, false)
            "right" -> font.drawString(
                invDisplayName,
                width - padding - font.getStringWidth(invDisplayName),
                -(font.FONT_HEIGHT).toFloat(),
                titleColor.rgb,
                false
            )
        }

        // render items
        enableGUIStandardItemLighting()
        renderInv(9, 17, 6, 6, font)
        renderInv(18, 26, 6, 24, font)
        renderInv(27, 35, 6, 42, font)
        disableStandardItemLighting()
        enableAlpha()
        disableBlend()
        disableLighting()

        return Border(0F, startY, width, height)
    }

    /**
     * render single line of inventory
     * @param endSlot slot+9
     */
    private fun renderInv(slot: Int, endSlot: Int, x: Int, y: Int, font: FontRenderer) {
        var xOffset = x
        for (i in slot..endSlot) {
            xOffset += 18
            val stack = mc.thePlayer.inventorySlot(i).stack ?: continue

            // Prevent overlapping while editing
            if (mc.currentScreen is GuiHudDesigner) glDisable(GL_DEPTH_TEST)

            mc.renderItem.renderItemAndEffectIntoGUI(stack, xOffset - 18, y)
            mc.renderItem.renderItemOverlays(font, stack, xOffset - 18, y)

            if (mc.currentScreen is GuiHudDesigner) glEnable(GL_DEPTH_TEST)
        }
    }
    private fun ShowShadow(startX: Float,startY: Float,width: Float,height:Float){
        if (Shadow) {
            GlowUtils.drawGlow(
                startX, startY,
                width, height,
                (shadowStrength * 13F).toInt(),
                Color(0, 0, 0, 120)
            )
        }
    }
}