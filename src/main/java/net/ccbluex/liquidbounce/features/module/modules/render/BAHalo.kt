package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.Lizz.CLIENT_NAME
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import kotlin.math.sin

object Halo : Module("Halo", Category.RENDER, forcedDescription = "Halo!") {

    private val characterChoices = arrayOf(
        "alice", "shiroko", "reisa", "hoshino", "azusa", "izuna", "kayoko", "shengya"
    )
    private val character by choices("Character", characterChoices, "alice")
    private val onlySelf by boolean("OnlySelf", true)
    private val trackCamera by boolean("TrackCamera", true)
    private val size by float("Scale", 0.5f, 0.1f..2f)
    private val offsetX by float("OffsetX", 0f, -2f..2f)
    private val offsetY by float("OffsetY", 0.4f, -2f..2f)
    private val offsetZ by float("OffsetZ", 0f, -2f..2f)
    private val floatRange by float("FloatRange", 0.05f, 0f..1f)
    private val floatSpeed by float("FloatSpeed", 0.5f, 0.01f..5f)
    private val flipEnabled by boolean("FlipEnabled", true)
    private val flipX by float("FlipX", 0f, -180f..180f)
    private val flipY by float("FlipY", 5f, -180f..180f)
    private val flipZ by float("FlipZ", 0f, -180f..180f)

    private val texture: ResourceLocation
        get() {
            val name = character.toString().lowercase()
            return ResourceLocation("${CLIENT_NAME.lowercase()}/halo/$name.png")
        }

    override val tag: String
        get() = character.toString()

    @Suppress("unused")
    val onRender3D = handler<Render3DEvent> { event ->
        val mc = mc
        val world = mc.theWorld ?: return@handler
        val partialTicks = event.partialTicks.toDouble()

        val players = if (!onlySelf) world.playerEntities else listOfNotNull(mc.thePlayer)

        val time = world.totalWorldTime + partialTicks
        val floatY = sin(time * floatSpeed) * floatRange

        try {
            mc.textureManager.bindTexture(texture)
        } catch (t: Throwable) {
            chat("Halo: texture ${texture.resourcePath} not found")
            return@handler
        }

        // 保存 GL 状态并设置渲染状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        GlStateManager.pushMatrix()

        try {
            GlStateManager.disableLighting()
            GlStateManager.disableCull()
            GlStateManager.enableBlend()
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GlStateManager.depthMask(false)
            GlStateManager.disableDepth()
            GlStateManager.color(1f, 1f, 1f, 1f)

            val renderManager = mc.renderManager

            for (entityPlayer in players) {
                try {
                    if (entityPlayer.isSpectator || !entityPlayer.isEntityAlive) continue

                    val interpX = entityPlayer.lastTickPosX + (entityPlayer.posX - entityPlayer.lastTickPosX) * partialTicks
                    val interpY = entityPlayer.lastTickPosY + (entityPlayer.posY - entityPlayer.lastTickPosY) * partialTicks
                    val interpZ = entityPlayer.lastTickPosZ + (entityPlayer.posZ - entityPlayer.lastTickPosZ) * partialTicks

                    val baseY = entityPlayer.getEyeHeight().toDouble() + offsetY + floatY
                    val hx = interpX + offsetX
                    val hy = interpY + baseY
                    val hz = interpZ + offsetZ

                    GlStateManager.pushMatrix()
                    GlStateManager.translate(
                        (hx - renderManager.viewerPosX).toFloat(),
                        (hy - renderManager.viewerPosY).toFloat(),
                        (hz - renderManager.viewerPosZ).toFloat()
                    )

                    val scale = size.toDouble()
                    GlStateManager.scale(scale.toFloat(), scale.toFloat(), scale.toFloat())

                    val viewY = try { renderManager.playerViewY } catch (_: Throwable) { mc.thePlayer.rotationYawHead }
                    val viewX = try { renderManager.playerViewX } catch (_: Throwable) { mc.thePlayer.rotationPitch.toFloat() }

                    GlStateManager.rotate(-viewY, 0f, 1f, 0f)
                    if (trackCamera) {
                        GlStateManager.rotate(viewX, 1f, 0f, 0f)
                    }

                    if (flipEnabled) {
                        GlStateManager.rotate(flipX, 1f, 0f, 0f)
                        GlStateManager.rotate(flipY, 0f, 1f, 0f)
                        GlStateManager.rotate(flipZ, 0f, 0f, 1f)
                    }

                    val half = 0.5f

                    val tess = Tessellator.getInstance()
                    val wr = try {
                        tess.worldRenderer
                    } catch (t: Throwable) {
                        try {
                            tess.getWorldRenderer()
                        } catch (t2: Throwable) {
                            null
                        }
                    }

                    if (wr != null) {
                        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)
                        // v1
                        wr.pos((-half).toDouble(), (-half).toDouble(), 0.0).tex(0.0, 1.0).color(1f, 1f, 1f, 1f).endVertex()
                        // v2
                        wr.pos(half.toDouble(), (-half).toDouble(), 0.0).tex(1.0, 1.0).color(1f, 1f, 1f, 1f).endVertex()
                        // v3
                        wr.pos(half.toDouble(), half.toDouble(), 0.0).tex(1.0, 0.0).color(1f, 1f, 1f, 1f).endVertex()
                        // v4
                        wr.pos((-half).toDouble(), half.toDouble(), 0.0).tex(0.0, 0.0).color(1f, 1f, 1f, 1f).endVertex()
                        tess.draw()
                    } else {
                        try {
                            val beginM = Tessellator.getInstance().javaClass.getMethod("getWorldRenderer")
                            val worldRenderer = beginM.invoke(Tessellator.getInstance())
                            val cls = worldRenderer.javaClass
                            val begin = cls.getMethod("begin", Int::class.javaPrimitiveType, DefaultVertexFormats::class.java)
                            begin.invoke(worldRenderer, GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)
                            val pos = cls.getMethod("pos", Double::class.javaPrimitiveType, Double::class.javaPrimitiveType, Double::class.javaPrimitiveType)
                            val tex = cls.getMethod("tex", Double::class.javaPrimitiveType, Double::class.javaPrimitiveType)
                            val colorM = cls.getMethod("color", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
                            val endV = cls.getMethod("endVertex")
                            pos.invoke(worldRenderer, (-half).toDouble(), (-half).toDouble(), 0.0)
                            tex.invoke(worldRenderer, 0.0, 1.0)
                            colorM.invoke(worldRenderer, 1f, 1f, 1f, 1f)
                            endV.invoke(worldRenderer)
                            pos.invoke(worldRenderer, half.toDouble(), (-half).toDouble(), 0.0)
                            tex.invoke(worldRenderer, 1.0, 1.0)
                            colorM.invoke(worldRenderer, 1f, 1f, 1f, 1f)
                            endV.invoke(worldRenderer)
                            pos.invoke(worldRenderer, half.toDouble(), half.toDouble(), 0.0)
                            tex.invoke(worldRenderer, 1.0, 0.0)
                            colorM.invoke(worldRenderer, 1f, 1f, 1f, 1f)
                            endV.invoke(worldRenderer)
                            pos.invoke(worldRenderer, (-half).toDouble(), half.toDouble(), 0.0)
                            tex.invoke(worldRenderer, 0.0, 0.0)
                            colorM.invoke(worldRenderer, 1f, 1f, 1f, 1f)
                            endV.invoke(worldRenderer)
                            val drawM = Tessellator.getInstance().javaClass.getMethod("draw")
                            drawM.invoke(Tessellator.getInstance())
                        } catch (_: Throwable) {

                        }
                    }

                    GlStateManager.popMatrix()
                } catch (_: Throwable) {

                }
            }
        } finally {
            try { GlStateManager.color(1f, 1f, 1f, 1f) } catch (_: Throwable) {}
            try {
                GlStateManager.enableDepth()
                GlStateManager.depthMask(true)
                GlStateManager.disableBlend()
                GlStateManager.enableCull()
                GlStateManager.enableLighting()
            } catch (_: Throwable) {}
            GlStateManager.popMatrix()
            GL11.glPopAttrib()
        }
    }
}
