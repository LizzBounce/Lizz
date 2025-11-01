package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.extras.ColorUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawScaledCustomSizeModalRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.*
import net.minecraft.util.EnumChatFormatting.BOLD
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.minecraft.util.ResourceLocation


object TargetHUD : Module("TargetHUD", Category.RENDER) {

    // General Settings
    private val hudStyle by choices(
        "Style",
        arrayOf(
            "Lizz",
            "Arc",
            "Compact",
            "Rise",
            "Southside",
            "Novoline",
            "戶籍",
            "Myau",
            "RavenB4",
            "Naven",
            "Neon"
        ),
        "Lizz"
    )
    private val posX by int("PosX", 0, -400..400)
    private val posY by int("PosY", 0, -400..400)
    private val animSpeed by float("AnimationSpeed", 0.1F, 0.01F..0.5F)

    // Flux Settings
    private val fluxColorMode by choices("Lizz-Color", arrayOf("Custom", "Health", "Rainbow"), "Health") { hudStyle == "Lizz" }
    private val fluxColorRed by int("Lizz-Red", 0, 0..255) { hudStyle == "Lizz" && fluxColorMode == "Custom" }
    private val fluxColorGreen by int("Lizz-Green", 120, 0..255) { hudStyle == "Lizz" && fluxColorMode == "Custom" }
    private val fluxColorBlue by int("Lizz-Blue", 255, 0..255) { hudStyle == "Lizz" && fluxColorMode == "Custom" }
    private val fluxColorSpec by boolean("Lizz-Gradient",true) {hudStyle == "Lizz"}
    private val healthIcon by boolean("health Icon",false) { hudStyle == "Lizz"}
    private val fluxLeftColor by color("left-Color", Color(0, 255, 150)) { fluxColorSpec }
    private val fluxRightColor by color("right-Color", Color(10, 80, 120)) { fluxColorSpec }

    // Arc Settings
    private val arcRainbow by boolean("Arc-Rainbow", true) { hudStyle == "Arc" }
    private val arcColorRed by int("Arc-Red", 255, 0..255) { hudStyle == "Arc" && !arcRainbow }
    private val arcColorGreen by int("Arc-Green", 255, 0..255) { hudStyle == "Arc" && !arcRainbow }
    private val arcColorBlue by int("Arc-Blue", 255, 0..255) { hudStyle == "Arc" && !arcRainbow }

    // Myau Settings
    private val rainbow by boolean("Myau-Rainbow", true) { hudStyle == "Myau" }
    private val borderRed by int("Myau-Border-Red", 255, 0..255) { hudStyle == "Myau" }
    private val borderGreen by int("Myau-Border-Green", 255, 0..255) { hudStyle == "Myau" }
    private val borderBlue by int("Myau-Border-Blue", 255, 0..255) { hudStyle == "Myau" }
    private val showAvatar by boolean("Myau-Show-Avatar", true) { hudStyle == "Myau" }

    // RavenB4 Settings
    val barColorR by int("RavenB4-BarColorR", 255, 0..255) { hudStyle == "RavenB4" }
    private val barColorG by int("RavenB4-BarColorG", 255, 0..255) { hudStyle == "RavenB4" }
    private val barColorB by int("RavenB4-BarColorB", 255, 0..255) { hudStyle == "RavenB4" }
    private val animSpeedRB4 by int("RavenB4-AnimSpeed", 3, 1..10) { hudStyle == "RavenB4" }

    // Moon4 Settings
    private val riseBarColorR by int("Rise-BarR", 70, 0..255) { hudStyle == "Rise" }
    private val riseBarColorG by int("Rise-BarG", 130, 0..255) { hudStyle == "Rise" }
    private val riseBarColorB by int("Rise-BarB", 255, 0..255) { hudStyle == "Rise" }
    private val riseBGColorR by int("Rise-BGR", 30, 0..255) { hudStyle == "Rise" }
    private val riseBGColorG by int("Rise-BGG", 30, 0..255) { hudStyle == "Rise" }
    private val riseBGColorB by int("Rise-BGB", 30, 0..255) { hudStyle == "Rise" }
    private val riseBGColorA by int("Rise-BGA", 180, 0..255) { hudStyle == "Rise" }
    private val riseAnimSpeed by int("Rise-AnimSpeed", 4, 1..10) { hudStyle == "Rise" }
    private val riseShadowCheck by boolean("Shadow", true) { hudStyle == "Rise" }
    private val riseGradient by boolean("Gradient", true) { hudStyle == "Rise" }
    private val riseBGColor2 by color("Rise-Gradient-Color2", Color(70, 130, 255)) { hudStyle == "Rise" && riseGradient }

    // State Variables
    private val decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
    private var target: EntityLivingBase? = null
    private var lastTarget: EntityLivingBase? = null
    override var hue = 0.0f
    // Wave样式设置项
    private val waveColor by choices("Wave-Color", arrayOf("Health", "Rainbow", "Custom"), "Health") { hudStyle == "Wave" }
    private val waveCustomColor = Color(0, 150, 255)

    // Pulse样式设置项
    private val pulseSpeed by float("Pulse-Speed", 0.05F, 0.01F..0.2F) { hudStyle == "Pulse" }
    private val pulseThickness by float("Pulse-Thickness", 4F, 1F..10F) { hudStyle == "Pulse" }

    // Neon样式设置项
    private val neonGlow by boolean("Neon-Glow", true) { hudStyle == "Neon" }
    private val neonColor = Color(30, 180, 255)
    // Animation States
    private var easingHealth = 0F
    private var moon4EasingHealth = 0F
    private var southsideEasingHealth = 0F
    private var slideIn = 0F
    private var damageHealth = 0F

    override fun onEnable() {
        easingHealth = 0F
        moon4EasingHealth = 0F
        southsideEasingHealth = 0f
        slideIn = 0F
        damageHealth = 0f
        hue = 0.0f
        target = null
        lastTarget = null
    }

    private fun updateSouthsideEasingHealth(targetHealth: Float, maxHealth: Float) {
        val changeAmount = abs(southsideEasingHealth - targetHealth)
        var speed = 0.02f * deltaTime

        if (changeAmount > 5) {
            speed *= 2.0f
        } else if (changeAmount > 2) {
            speed *= 1.5f
        }

        if (abs(southsideEasingHealth - targetHealth) < 0.1f) {
            southsideEasingHealth = targetHealth
        } else if (southsideEasingHealth > targetHealth) {
            southsideEasingHealth -= min(speed * 1.2f, southsideEasingHealth - targetHealth)
        } else {
            southsideEasingHealth += min(speed, targetHealth - southsideEasingHealth)
        }
        southsideEasingHealth = southsideEasingHealth.coerceAtMost(maxHealth)
    }

    private fun renderSouthsideHUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return

        val health = entity.health
        val maxHealth = entity.maxHealth
        val healthPercent = (health / maxHealth).coerceIn(0f, 1f)

        // Update easing health
        updateSouthsideEasingHealth(health, maxHealth)
        val easingHealthPercent = (southsideEasingHealth / maxHealth).coerceIn(0f, 1f)

        val name = entity.name
        val width = Fonts.fontSemibold40.getStringWidth(name) + 75f
        val presentWidth = easingHealthPercent * width

        GlStateManager.pushMatrix()

        // Animation
        val animOutput = slideIn
        GlStateManager.translate((x + width / 2) * (1 - animOutput).toDouble(), (y + 20) * (1 - animOutput).toDouble(), 0.0)
        GlStateManager.scale(animOutput, animOutput, animOutput)

        // Background
        RenderUtils.drawRect(x, y, x + width, y + 40, Color(0, 0, 0, 100).rgb)
        RenderUtils.drawRect(x, y, x + presentWidth, y + 40, Color(230, 230, 230, 100).rgb)

        // Vertical health indicator
        val healthColor = when {
            healthPercent > 0.5 -> Color(63, 157, 4, 150)
            healthPercent > 0.25 -> Color(255, 144, 2, 150)
            else -> Color(168, 1, 1, 150)
        }
        RenderUtils.drawRect(x, y + 12.5f, x + 3, y + 27.5f, healthColor.rgb)

        // Head
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(it.locationSkin, x.toInt() + 7, y.toInt() + 7, 26, 26, Color.WHITE)
        } ?: RenderUtils.drawRect(x + 6, y + 6, x + 34, y + 34, Color.BLACK.rgb)


        // Text
        Fonts.fontSemibold40.drawString(name, x + 40, y + 7, Color(200, 200, 200, 255).rgb)
        Fonts.fontSemibold40.drawString("${health.toInt()} HP", x + 40, y + 22, Color(200, 200, 200, 255).rgb)

        // Held Item
        val itemStack = entity.heldItem
        val itemX = x + Fonts.fontSemibold40.getStringWidth(name) + 50
        if (itemStack != null) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(itemX, y + 12, 0f)
            GlStateManager.scale(1.5f, 1.5f, 1.5f) // Make item bigger
            RenderHelper.enableGUIStandardItemLighting()
            mc.renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0)
            RenderHelper.disableStandardItemLighting()
            GlStateManager.popMatrix()
        } else {
            Fonts.fontSemibold40.drawString("?", x + Fonts.fontSemibold40.getStringWidth(name) + 55, y + 11, Color(200, 200, 200, 255).rgb)
        }

        GlStateManager.popMatrix()
    }


    // ... [Rest of the existing code is unchanged] ...
    // Note: To save space, the rest of the file is omitted, but the changes below are essential.
    // Make sure the following changes are also applied to your existing file.

    val onRender2D = handler<Render2DEvent> {
        val kaTarget = KillAura.target

        // Update target logic
        if (kaTarget != null && kaTarget is EntityPlayer && !AntiBot.isBot(kaTarget)) {
            target = kaTarget
        } else if (mc.currentScreen is GuiChat) {
            target = mc.thePlayer
        } else if (target != null && (KillAura.target == null || !target!!.isEntityAlive || AntiBot.isBot(target!!))) {
            target = null
        }

        // Handle target change for animations
        if (target != lastTarget) {
            if (lastTarget != null) { // Smooth out previous target
                easingHealth = lastTarget!!.health
                damageHealth = lastTarget!!.health
            } else if (target != null) { // Instantly set for new target
                easingHealth = target!!.health
                damageHealth = target!!.health
            }
            // Instantly set health for animated styles to prevent animating from old target
            if (target != null) {
                moon4EasingHealth = target!!.health
                southsideEasingHealth = target!!.health
            }
        }

        lastTarget = target

        // Update global animations
        hue += 0.05f * deltaTime * 0.1f
        if (hue > 1F) hue = 0F

        slideIn = lerp(slideIn, if (target != null) 1F else 0F, animSpeed)

        if (slideIn < 0.01F && target == null) return@handler

        val sr = ScaledResolution(mc)

        // Centralized positioning
        val x = sr.scaledWidth / 2F + posX
        val y = sr.scaledHeight / 2F + posY

        when (hudStyle.lowercase(Locale.getDefault())) {
            // New Styles
            "lizz" -> renderFluxHUD(x, y)
            "arc" -> renderArcHUD(x, y)
            "compact" -> renderCompactHUD(x, y)
            "rise" -> renderMoon4HUD(x, y)
            "southside" -> renderSouthsideHUD(x,y)
            "novoline" -> renderNovolineHUD(sr)
            "戶籍" -> render0x01a4HUD(sr)
            "myau" -> renderMyauHUD(sr)
            "ravenb4" -> renderRavenB4HUD(sr)
            "naven" -> renderNavenHUD(sr)
            "neon" -> renderNeonHUD(x, y)
        }
    }

    var AnimX = 100F

    private fun renderMoon4HUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return

        // Animate towards the current target's health, or towards 0 if no target.
        val currentHealth = target?.health ?: 0f
        moon4EasingHealth += ((currentHealth - moon4EasingHealth) / 2.0F.pow(10.0F - riseAnimSpeed)) * deltaTime

        val mainColor = Color(riseBarColorR, riseBarColorG, riseBarColorB)
        val bgColor = Color(riseBGColorR, riseBGColorG, riseBGColorB, riseBGColorA)
        val smallBGColor : Color

        var boldName = "$BOLD${entity.name}"
        val healthInt = entity.health.toInt()
        val percentText = "${healthInt}.0"
        val healthWidth = Fonts.fontGoogleSans35.getStringWidth(percentText)

        if (mc.thePlayer.health < entity.health){
            boldName = "Losing: "+boldName
        }else if(mc.thePlayer.health > entity.health){
            boldName = "Winning: "+boldName
        }else{
            boldName = "Saming: "+boldName
        }

        val nameLength = (Fonts.fontGoogleSans40.getStringWidth(boldName)).coerceAtLeast(
            Fonts.fontSemibold35.getStringWidth(percentText)
        ).toFloat() + 20F

        val healthPercent = (entity.health / entity.maxHealth).coerceIn(0F, 1F)
        val barWidth = healthPercent * (nameLength - 2F)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        // Backgrounds
        if (riseShadowCheck) ShowShadow(-1F,-1F,2F + nameLength + 45F + healthWidth,1F + 36F,1F)
        RenderUtils.drawRoundedRect(-1F, -1F, 2F + nameLength + 45F + healthWidth, 1F + 36F, bgColor.rgb, 10f)
        //RenderUtils.drawRoundedRect(-1F, -1F, 2F + nameLength + 36F, 1F + 36F, Color(0, 0, 0, 50).rgb, 3f)

        // Head with Stencil
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let { playerInfo ->
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.drawRoundedRect(1f, 0.5f, 1f + 35f, 0.5f + 35f, Color.WHITE.rgb, 7F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            drawRoundedHead(playerInfo.locationSkin, 3, 2, 31, 31, Color.WHITE)
            Stencil.dispose()
        }

        // Text
        Fonts.fontGoogleSans40.drawString(boldName, 2F + 36F, 7F, -1)
        //Fonts.fontSemibold35.drawStringWithShadow(percentText, 38F, 15F, Color.WHITE.rgb)

        AnimX = AnimationUtil.base(AnimX.toDouble(),barWidth.toDouble(),0.2).toFloat()

        // Health Bar
        RenderUtils.drawRoundedRect(38F, 24F, 38F + nameLength, 28f, Color(0, 0, 0, 100).rgb, 4f)
        if (!riseGradient){
            RenderUtils.drawRoundedRect(38F, 24F, 38F + barWidth, 28f, mainColor.rgb, 4f)
            smallBGColor = mainColor
        }else{
            RenderUtils.drawRoundedGradientRectCorner(38F, 24F, 38F + barWidth, 28f,4f,mainColor.rgb, Color(riseBGColor2.red, riseBGColor2.green, riseBGColor2.blue).rgb)
            smallBGColor = Color(riseBGColor2.red, riseBGColor2.green, riseBGColor2.blue)
        }
        drawRoundedRect(38F, 24F, 38F + AnimX, 28f, Color(smallBGColor.red, smallBGColor.green, smallBGColor.blue,50).rgb, 4f)

        Fonts.fontGoogleSans35.drawString(percentText, 2F + nameLength + 40F, 23F, -1)

        GlStateManager.popMatrix()
    }

    private fun renderFluxHUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return
        val width = 120F
        val height = 46F

        // Update animations
        easingHealth = lerp(easingHealth, entity.health, animSpeed)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)
        GlStateManager.translate(-x, -y, 0F)

        // Background (1 is the offset)
        RenderUtils.drawRoundedBorderRect(x+1, y+1, x + width - 1, y + height - 1, 1f, Color(40, 40, 40,200).rgb, Color.BLACK.rgb, 0F)

        // Head
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(it.locationSkin, (x + 6).toInt(), (y + 6).toInt(), 34, 34, Color.WHITE)
        }

        // Name
        Fonts.minecraftFont.drawString(entity.name, (x + 46).toInt(), (y + 8).toInt(), Color.WHITE.rgb)

        // Health Bar
        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val healthBarWidth = (width - 52) * healthPercent
        val barColor = when(fluxColorMode) {
            "Custom" -> Color(fluxColorRed, fluxColorGreen, fluxColorBlue)
            "Rainbow" -> Color.getHSBColor(hue, 0.7f, 0.9f)
            else -> ColorUtils.getHealthColor(easingHealth, entity.maxHealth)
        }

        RenderUtils.drawRect(x + 46, y + 22, x + width - 6, y + 32, Color(45, 45, 45).rgb)
        if (!fluxColorSpec){
            RenderUtils.drawRect(x + 46, y + 22, x + 46 + healthBarWidth, y + 32, barColor.rgb)
        }else{
            RenderUtils.drawGradientRect(x+46,y+22, x+46+healthBarWidth,y+32, fluxLeftColor.rgb, fluxRightColor.rgb,0f)
        }

        // Health Text
        var healthText = decimalFormat.format(easingHealth)
        if (healthIcon){
            healthText="❤${healthText}"
        }
        Fonts.minecraftFont.drawString(healthText, (x + 66).toInt(), (y + 24).toInt(), Color.WHITE.rgb)

        // Distance Text
        //val distance = mc.thePlayer.getDistanceToEntity(entity)
        //val distanceText = "${decimalFormat.format(distance)}m"
        // 您可以在这里添加一个图标
        // drawIcon(x + 46, y + 33, icon_path)
        //Fonts.fontSemibold35.drawString(distanceText, x + 46, y + 33, Color(200, 200, 200).rgb)

        GlStateManager.popMatrix()
    }
    private fun renderArcHUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return
        val size = 50F

        // Update animations
        easingHealth = lerp(easingHealth, entity.health, animSpeed)

        GlStateManager.pushMatrix()
        val scale = slideIn.pow(0.5f)
        GlStateManager.translate(x + size / 2, y + size / 2, 0F)
        GlStateManager.scale(scale, scale, scale)
        GlStateManager.translate(-(x + size / 2), -(y + size / 2), 0F)

        // Draw Head clipped in a circle
        RenderUtils.withClipping(
            { drawCircle(x + size / 2, y + size / 2, size / 2 - 3, Color.WHITE.rgb) },
            { mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
                Target().drawHead(it.locationSkin, x.toInt() + 3, y.toInt() + 3, (size - 6).toInt(), (size - 6).toInt(), Color.WHITE)
            }}
        )

        // Health Arc
        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val arcColor = if(arcRainbow) Color.getHSBColor(hue, 0.6f, 1f) else Color(arcColorRed, arcColorGreen, arcColorBlue)

        // Background Arc
        drawCircleArc(x + size / 2, y + size / 2, size / 2 - 1.5F, 3F, 0F, 360F, Color(40, 40, 40, (200 * slideIn).toInt()))
        // Foreground Arc
        if (healthPercent > 0) {
            drawCircleArc(x + size / 2, y + size / 2, size / 2 - 1.5F, 3F, -90F, 360F * healthPercent, arcColor)
        }

        // Text Info
        val textX = x + size + 5
        val nameColor = Color(255, 255, 255, (255 * slideIn).toInt()).rgb
        val healthColor = Color(200, 200, 200, (255 * slideIn).toInt()).rgb
        Fonts.fontSemibold40.drawString(entity.name, textX, y + 8, nameColor)
        Fonts.fontSemibold35.drawString("HP: ${decimalFormat.format(easingHealth)}", textX, y + 24, healthColor)

        GlStateManager.popMatrix()
    }

    private fun renderCompactHUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return
        val width = 120F
        val height = 18F

        // Update animations
        if (target != null) {
            easingHealth = lerp(easingHealth, entity.health, animSpeed * 1.5f)
            if (abs(entity.health - damageHealth) > 0.1f && easingHealth < damageHealth) {
                // Damage flash effect, damageHealth catches up to easingHealth
                damageHealth = lerp(damageHealth, easingHealth, animSpeed * 0.5f)
            } else {
                damageHealth = easingHealth
            }
        } else {
            // When target is lost, both bars fade out
            easingHealth = lerp(easingHealth, 0f, animSpeed)
            damageHealth = lerp(damageHealth, 0f, animSpeed)
        }

        if (target != null && target != lastTarget) {
            damageHealth = entity.maxHealth // Reset damage bar on new target
        }

        GlStateManager.pushMatrix()
        val scale = slideIn.pow(2f)
        GlStateManager.translate(x + width / 2, y + height / 2, 0F)
        GlStateManager.scale(1f, scale, 1f)
        GlStateManager.translate(-(x + width / 2), -(y + height / 2), 0F)

        if (scale < 0.05f) {
            GlStateManager.popMatrix()
            return
        }

        // Background
        RenderUtils.drawRect(x, y, x + width, y + height, Color(30, 30, 30, (180 * scale).toInt()).rgb)

        // Health Bars
        val healthPercent = (easingHealth / entity.maxHealth).coerceIn(0F, 1F)
        val damagePercent = (damageHealth / entity.maxHealth).coerceIn(0F, 1F)
        val barColor = Color(100, 100, 100, (150 * scale).toInt())

        // Damage bar (the trail)
        RenderUtils.drawRect(x + 0, y + 0, x + 0 + (width - 4) * damagePercent, y + height - 0, barColor)
        // Health bar
        RenderUtils.drawRect(x + 0, y + 0, x + 0 + (width - 4) * healthPercent, y + height - 0, barColor)

        // Text on bar
        val text = "${entity.name} - ${decimalFormat.format(easingHealth)} HP"
        Fonts.fontRegular40.drawCenteredString(text, x + width / 2, y + height / 2 - Fonts.fontSemibold35.fontHeight / 2 + 1, Color.WHITE.rgb, false)

        GlStateManager.popMatrix()
    }

    private fun renderNeonHUD(x: Float, y: Float) {
        val entity = target ?: return
        val width = 161F
        val height = 47F

        easingHealth = lerp(easingHealth, entity.health, animSpeed * 10)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, 1F)
        RenderUtils.drawRoundedRect(0F, 0F, width, height, Color(10, 10, 10, 180).rgb, 8F)

        if(neonGlow) {
            for(i in 1..3) {
                val glowSize = i * 2F
                RenderUtils.drawRoundedRect(-glowSize, -glowSize, width+glowSize, height+glowSize,
                    Color(neonColor.red, neonColor.green, neonColor.blue, 30/i).rgb, 8F + glowSize)
            }
        }

        // 头像绘制
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(it.locationSkin, 8, 8, 33, 33, Color.WHITE)
        }

        // 名称文本
        Fonts.fontRegular40.drawString(entity.name, 51F, 12F, Color.WHITE.rgb)

        // 霓虹健康条
        val healthPercent = easingHealth / entity.maxHealth
        val barHeight = 10F
        val barX = 51F
        val barY = 24F

        // 背景条
        RenderUtils.drawRoundedRect(barX, barY, width - 20F, barY + barHeight, Color(30, 30, 30).rgb, 4F)

        // 前景条（霓虹效果）
        val gradientWidth = (width - 80F) * healthPercent
        RenderUtils.drawRoundedRect(barX, barY, barX + gradientWidth, barY + barHeight,
            neonColor.brighter().rgb, 4F)

        // 健康数值
        val healthText = "${decimalFormat.format(easingHealth)} / ${entity.maxHealth}"
        Fonts.fontRegular40.drawString(healthText, width - Fonts.fontSemibold35.getStringWidth(healthText) - 50F, barY + 0F, Color(255, 255, 255).rgb)

        // 距离显示
        val distance = mc.thePlayer.getDistanceToEntity(entity)
        val distanceText = "${decimalFormat.format(distance)}m"
        Fonts.fontRegular35.drawString(distanceText, barX, barY + barHeight + 4F, Color(200,200, 200, 180).rgb)

        GlStateManager.popMatrix()
    }
    // Helper function to draw an arc, can be moved to RenderUtils later
    private fun drawCircleArc(x: Float, y: Float, radius: Float, lineWidth: Float, startAngle: Float, endAngle: Float, color: Color) {
        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(lineWidth)

        glColor4f(color.red / 255F, color.green / 255F, color.blue / 255F, color.alpha / 255F)

        glBegin(GL_LINE_STRIP)
        for (i in (startAngle / 360 * 100).toInt()..(endAngle / 360 * 100).toInt()) {
            val angle = (i / 100.0 * 360.0 * (PI / 180)).toFloat()
            glVertex2f(x + sin(angle) * radius, y + cos(angle) * radius)
        }
        glEnd()

        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glPopMatrix()
        glColor4f(1f, 1f, 1f, 1f)
    }

    private fun drawCircle(x: Float, y: Float, radius: Float, color: Int) {
        val side = (radius * 2).toInt()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_POLYGON_SMOOTH)
        glBegin(GL_TRIANGLE_FAN)
        RenderUtils.glColor(Color(color))
        for (i in 0..side) {
            val angle = i * (Math.PI * 2) / side
            glVertex2d(x + sin(angle) * radius, y + cos(angle) * radius)
        }
        glEnd()
        glDisable(GL_POLYGON_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
    }

    override fun onDisable() {
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.disableBlend()
    }

    override val tag get() = hudStyle

    private fun lerp(start: Float, end: Float, speed: Float): Float = start + (end - start) * speed * (deltaTime / (1000F / 60F))

    private fun getRainbowColor(): Color = Color.getHSBColor(hue, 1f, 1f)

    // --- Original HUD Render Functions ---
    // (The original functions from your code are placed below, unchanged)

    private fun updateRavenB4Anim(targetHealth: Float) {
        easingHealth += ((targetHealth - easingHealth) / 2.0F.pow(10.0F - animSpeedRB4)) * deltaTime
    }

    private fun renderRavenB4HUD(sr: ScaledResolution) {
        val entity = target ?: return
        val x = sr.scaledWidth / 2F + posX
        val y = sr.scaledHeight / 2F + posY
        val font = Fonts.minecraftFont
        val hp = decimalFormat.format(entity.health)
        val hplength = font.getStringWidth(hp)
        val length = font.getStringWidth(entity.displayName.formattedText)
        val barColor = Color(barColorR, barColorG, barColorB)
        val totalWidth = x + length + hplength + 23F
        val totalHeight = y + 35F

        GlStateManager.pushMatrix()
        updateRavenB4Anim(entity.health)
        RenderUtils.drawRoundedGradientOutlineCorner(x, y, totalWidth, totalHeight, 2F, 8F, barColor.rgb, barColor.rgb)
        RenderUtils.drawRoundedRect(x, y, totalWidth, totalHeight, Color(0, 0, 0, 100).rgb, 4F)
        GlStateManager.enableBlend()
        font.drawStringWithShadow(entity.displayName.formattedText, x + 6F, y + 8F, Color.WHITE.rgb)

        val winOrLose = if (entity.health < mc.thePlayer.health) "W" else "L"
        val wlColor = if (winOrLose == "W") Color(0, 255, 0).rgb else Color(139, 0, 0).rgb
        font.drawStringWithShadow(winOrLose, x + length + hplength + 11.6F, y + 8F, wlColor)

        font.drawStringWithShadow(hp, x + length + 8F, y + 8F, ColorUtils.reAlpha(ColorUtils.getHealthColor(entity.health, entity.maxHealth).rgb, 255F).rgb)

        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        RenderUtils.drawRoundedRect(x + 5.0F, y + 29.55F, x + length + hplength + 18F, y + 25F, Color(0, 0, 0, 110).rgb, 2F)
        RenderUtils.drawRoundedGradientRectCorner(
            x + 5F, y + 25F,
            x + 5F + (easingHealth / entity.maxHealth) * (length + hplength + 13F),
            y + 29.5F, 4F, barColor.rgb, barColor.rgb
        )
        GlStateManager.popMatrix()
    }

    private fun renderNavenHUD(sr: ScaledResolution) {
        val entity = target ?: return
        val x = sr.scaledWidth / 2f + posX
        val y = sr.scaledHeight / 2f + posY
        val width = 130f
        val height = 50f

        RenderUtils.drawRoundedRect(x, y, x + width, y + height, Color(30, 30, 30, 239).rgb, 5f)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin?.let {
            Target().drawHead(it, x.toInt() + 7, y.toInt() + 7, 30, 30, Color.WHITE)
        }

        val barX1 = x + 5f
        val barY1 = y + height - 10f
        val barX2 = x + width - 5f
        val barY2 = barY1 + 3f
        RenderUtils.drawRoundedRect(barX1, barY1, barX2, barY2, Color(0, 0, 0, 200).rgb, 2f)

        val healthPercent = entity.health / entity.maxHealth
        val fillX2 = barX1 + (barX2 - barX1) * healthPercent
        RenderUtils.drawRoundedRect(barX1, barY1, fillX2, barY2, Color(160, 42, 42).rgb, 2f)

        Fonts.fontSemibold35.drawString(entity.name, x + 40f, y + 10f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("Health: ${"%.2f".format(entity.health)}", x + 40f, y + 22f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("Distance: ${"%.2f".format(entity.getDistanceToEntity(mc.thePlayer))}", x + 40f, y + 30f, Color.WHITE.rgb)
    }

    private fun renderNovolineHUD(sr: ScaledResolution) {
        val entity = target ?: return
        val x = sr.scaledWidth / 2 + this.posX
        val y = sr.scaledHeight / 2 + this.posY
        val width = (38 + Fonts.fontSemibold35.getStringWidth(entity.name)).coerceAtLeast(118).toFloat()

        RenderUtils.drawRect(x.toFloat(), y.toFloat(), x + width + 14f, y + 44f, Color(0, 0, 0, 120).rgb)

        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(it.locationSkin, x + 3, y + 3, 30, 30, Color.WHITE)
        }

        Fonts.fontSemibold35.drawString(entity.name, x + 34.5f, y + 4f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("Health: ${"%.1f".format(entity.health)}", x + 34.5f, y + 14f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("Distance: ${"%.1f".format(mc.thePlayer.getDistanceToEntity(entity))}m", x + 34.5f, y + 24f, Color.WHITE.rgb)

        RenderUtils.drawRect(x + 2.5f, y + 35.5f, x + width + 11.5f, y + 37.5f, Color(0, 0, 0, 200).rgb)
        RenderUtils.drawRect(x + 3f, y + 36f, x + 3f + (entity.health / entity.maxHealth) * (width + 8f), y + 37f, Color(0,255,150))

        RenderUtils.drawRect(x + 2.5f, y + 39.5f, x + width + 11.5f, y + 41.5f, Color(0, 0, 0, 200).rgb)
        RenderUtils.drawRect(x + 3f, y + 40f, x + 3f + (entity.totalArmorValue / 20f) * (width + 8f), y + 41f, Color(77, 128, 255).rgb)
    }

    private fun renderMyauHUD(sr: ScaledResolution) {
        val entity = target ?: return
        val x = sr.scaledWidth / 2F + posX
        val y = sr.scaledHeight / 2F + posY
        val nameWidth = Fonts.fontSemibold35.getStringWidth(entity.name)
        val hudWidth = maxOf(80f, nameWidth + 20f)
        val hudHeight = 25f
        val avatarSize = hudHeight

        if (rainbow) {
            hue += 0.0005f
            if (hue > 1f) hue = 0f
        }
        val borderColor = if (rainbow) getRainbowColor() else Color(borderRed, borderGreen, borderBlue)
        val healthBarColor = if (rainbow) getRainbowColor() else Color(maxOf(borderRed - 50, 0), maxOf(borderGreen - 50, 0), maxOf(borderBlue - 50, 0))

        val totalWidth = if (showAvatar) hudWidth + avatarSize else hudWidth

        RenderUtils.drawRect(x - 1, y - 1, x + totalWidth + 1, y, borderColor.rgb)
        RenderUtils.drawRect(x - 1, y + hudHeight, x + totalWidth + 1, y + hudHeight + 1, borderColor.rgb)
        RenderUtils.drawRect(x - 1, y, x, y + hudHeight, borderColor.rgb)
        RenderUtils.drawRect(x + totalWidth, y, x + totalWidth + 1, y + hudHeight, borderColor.rgb)
        RenderUtils.drawRect(x, y, x + totalWidth, y + hudHeight, Color(0, 0, 0, 100).rgb) // Background

        if (showAvatar) {
            mc.netHandler.getPlayerInfo(entity.uniqueID)?.locationSkin?.let {
                Target().drawHead(it, x.toInt(), y.toInt(), avatarSize.toInt(), avatarSize.toInt(), Color.WHITE)
            }
        }

        val textX = if (showAvatar) x + avatarSize + 3 else x + 3
        Fonts.fontSemibold35.drawString(entity.name, textX, y + 1, Color.WHITE.rgb)
        val healthText = String.format("%.1f", entity.health)
        Fonts.fontSemibold35.drawString(healthText, textX, y + 11, healthBarColor.rgb)
        Fonts.fontSemibold35.drawString("\u2764", textX + Fonts.fontSemibold35.getStringWidth(healthText) + 2, y + 11, healthBarColor.rgb)

        val barY = y + 21
        val barWidth = hudWidth - 5f
        RenderUtils.drawRect(textX, barY, textX + barWidth, barY + 3, Color(64, 64, 64).rgb)
        val targetFill = (entity.health / entity.maxHealth) * barWidth
        easingHealth = lerp(easingHealth, targetFill, 0.1f)
        RenderUtils.drawRect(textX, barY, textX + easingHealth, barY + 3, healthBarColor.rgb)

        val playerHealth = mc.thePlayer.health
        val (winLoss, wlColor) = when {
            playerHealth > entity.health -> "W" to Color(0, 255, 0)
            playerHealth < entity.health -> "L" to Color(255, 0, 0)
            else -> "D" to Color(255, 255, 0)
        }
        Fonts.fontSemibold35.drawString(winLoss, x + totalWidth - Fonts.fontSemibold35.getStringWidth(winLoss) - 1, y + 1, wlColor.rgb)

        val diff = playerHealth - entity.health
        val diffText = if (diff > 0) "+${"%.1f".format(diff)}" else String.format("%.1f", diff)
        val diffColor = when {
            diff > 0 -> Color(0, 255, 0)
            diff < 0 -> Color(255, 0, 0)
            else -> Color(255, 255, 0)
        }
        Fonts.fontSemibold35.drawString(diffText, maxOf(x + totalWidth - Fonts.fontSemibold35.getStringWidth(diffText) - 1, textX), y + 11, diffColor.rgb)
    }

    private fun render0x01a4HUD(sr: ScaledResolution) {
        val entity = target ?: return
        val x = sr.scaledWidth / 2 + this.posX
        val y = sr.scaledHeight / 2 + this.posY

        RenderUtils.drawRect(x + 11F, y - 15F, x + 150F, y + 90F, Color(30, 30, 30, 200).rgb)
        Fonts.fontSemibold35.drawString("PLC 全国人口档案查询系统", x + 15f, y - 5f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("姓名: ${entity.name}", x + 15f, y + 5f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("健康: ${entity.health.toInt()}/${entity.maxHealth.toInt()}", x + 15f, y + 25f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("资产: ${entity.totalArmorValue}", x + 15f, y + 45f, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("身份证: ${entity.entityId}", x + 15f, y + 65f, Color.WHITE.rgb)
    }
    private fun drawRoundedHead(skinLocation: ResourceLocation, x: Int, y: Int, width: Int, height: Int, color: Color, radius: Float = 6f) {
        Stencil.write(false)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.drawRoundedRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), color.rgb, radius)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        Stencil.erase(true)
        Target().drawHead(skinLocation, x, y, width, height, color)
        Stencil.dispose()
    }
    private fun ShowShadow(startX: Float,startY: Float,width: Float,height:Float,shadowStrengh:Float){
        GlowUtils.drawGlow(
            startX, startY,
            width, height,
            (shadowStrengh * 13F).toInt(),
            Color(0, 0, 0, 120)
        )
    }
}