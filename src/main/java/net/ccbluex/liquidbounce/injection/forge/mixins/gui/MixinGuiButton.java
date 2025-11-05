/*
 * Lizz Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/LizzBounce/Lizz/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.Lizz;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.ui.button.*;
import net.ccbluex.liquidbounce.ui.cnfont.FontDrawer;
import net.ccbluex.liquidbounce.ui.cnfont.FontLoaders;
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.EaseUtils;
import net.ccbluex.liquidbounce.utils.render.LBPPAnimationUtils;
import net.ccbluex.liquidbounce.utils.render.LBPPRenderUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.awt.*;

import static net.minecraft.client.renderer.GlStateManager.resetColor;

@Mixin(GuiButton.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiButton extends Gui {

    @Shadow
    public boolean visible;

    @Shadow
    public int xPosition;

    @Shadow
    public int yPosition;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow
    protected boolean hovered;

    @Shadow
    public boolean enabled;

    @Shadow
    protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

    @Shadow
    protected abstract int getHoverState(boolean mouseOver);

    @Shadow
    public String displayString;

    @Shadow
    @Final
    protected static ResourceLocation buttonTextures;

    private float bright = 0F;
    private float moveX = 0F;
    private float cut;
    private float alpha;
    private float rectY;

    /**
     * @author CCBlueX
     */
    @Overwrite
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            final FontRenderer fontRenderer = mc.fontRendererObj;
            final FontDrawer prideFont = FontLoaders.F16;
            BadlionTwoButtonRenderer badlionTwoButtonRenderer = new BadlionTwoButtonRenderer((GuiButton) (Object) this);
            MelonButtonRenderer melonButtonRenderer = new MelonButtonRenderer((GuiButton) (Object) this);
            BlackoutButtonRenderer blackoutButtonRenderer = new BlackoutButtonRenderer((GuiButton) (Object) this);
            FLineButtonRenderer fLineButtonRenderer = new FLineButtonRenderer((GuiButton) (Object) this);
            HyperiumButtonRenderer hyperiumButtonRenderer = new HyperiumButtonRenderer((GuiButton) (Object) this);
            LiquidButtonRenderer liquidButtonRenderer = new LiquidButtonRenderer((GuiButton) (Object) this);
            LunarButtonRenderer lunarButtonRenderer = new LunarButtonRenderer((GuiButton) (Object) this);
            PvPClientButtonRenderer pvPClientButtonRenderer = new PvPClientButtonRenderer((GuiButton) (Object) this);
            WolframButtonRenderer wolframButtonRenderer = new WolframButtonRenderer((GuiButton) (Object) this);
            hovered = (mouseX >= this.xPosition && mouseY >= this.yPosition &&
                    mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height);

            final int delta = RenderUtils.INSTANCE.getDeltaTime();
            final float speedDelta = 0.01F * delta;

            final HUD hud = HUD.INSTANCE;

            if (hud == null) return;

            if (enabled && hovered) {
                // LiquidBounce
                cut += 0.05F * delta;
                if (cut >= 4) cut = 4;
                alpha += 0.3F * delta;
                if (alpha >= 210) alpha = 210;

                // LiquidBounce+
                moveX = LBPPAnimationUtils.animate(this.width - 2.4F, moveX, speedDelta);
            } else {
                // LiquidBounce
                cut -= 0.05F * delta;
                if (cut <= 0) cut = 0;
                alpha -= 0.3F * delta;
                if (alpha <= 120) alpha = 120;

                // LiquidBounce+
                moveX = LBPPAnimationUtils.animate(0F, moveX, speedDelta);
            }

            if (enabled && hovered) {
                cut += 0.05F * delta;

                if (cut >= 4) cut = 4;

                alpha += 0.3F * delta;

                if (alpha >= 210) alpha = 210;

                rectY += 0.1F * delta;

                if (rectY >= height) rectY = height;
            } else {
                cut -= 0.05F * delta;

                if (cut <= 0) cut = 0;

                alpha -= 0.3F * delta;

                if (alpha <= 120) alpha = 120;

                rectY -= 0.05F * delta;

                if (rectY <= 4) rectY = 4;
            }

            float roundCorner = (float) Math.max(0F, 2.4F + moveX - (this.width - 2.4F));

            switch (hud.getButtonStyle().toLowerCase()) {
                case "minecraft":
                    mc.getTextureManager().bindTexture(buttonTextures);
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
                    int i = this.getHoverState(this.hovered);
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.blendFunc(770, 771);
                    this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + i * 20, this.width / 2, this.height);
                    this.drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
                    this.mouseDragged(mc, mouseX, mouseY);
                    int j = 14737632;

                    if (!this.enabled)
                    {
                        j = 10526880;
                    }
                    else if (this.hovered)
                    {
                        j = 16777120;
                    }

                    this.drawCenteredString(mc.fontRendererObj, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, j);
                    break;
                case "liquidbounce":
                    Gui.drawRect(this.xPosition + (int) this.cut, this.yPosition,
                            this.xPosition + this.width - (int) this.cut, this.yPosition + this.height,
                            this.enabled ? new Color(0F, 0F, 0F, this.alpha / 255F).getRGB() :
                                    new Color(0.5F, 0.5F, 0.5F, 0.5F).getRGB());
                    break;
                case "rounded":
                    LBPPRenderUtils.originalRoundedRect(this.xPosition, this.yPosition,
                            this.xPosition + this.width, this.yPosition + this.height, 2F,
                            this.enabled ? new Color(0F, 0F, 0F, this.alpha / 255F).getRGB() :
                                    new Color(0.5F, 0.5F, 0.5F, 0.5F).getRGB());
                    break;
                case "liquidbounce+":
                    LBPPRenderUtils.drawRoundedRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, 2.4F, new Color(0, 0, 0, 150).getRGB());
                    LBPPRenderUtils.customRounded(this.xPosition, this.yPosition, this.xPosition + 2.4F + moveX, this.yPosition + this.height, 2.4F, roundCorner, roundCorner, 2.4F, (this.enabled ? new Color(0, 111, 255) : new Color(71, 71, 71)).getRGB());
                    break;
                case "prideplus":
                    LBPPRenderUtils.drawRoundRect(this.xPosition + (int) this.cut, this.yPosition,
                            this.xPosition + this.width - (int) this.cut, this.yPosition + this.height, 3F,
                            new Color(49, 51, 53, 200).getRGB());

                    LBPPRenderUtils.drawRoundRect(this.xPosition + (int) this.cut, this.yPosition,
                            this.xPosition + this.width - (int) this.cut, this.yPosition + rectY, 2F,
                            this.enabled ? new Color(0, 165, 255, 255).getRGB() :
                                    new Color(82, 82, 82, 200).getRGB());
                    break;
                case "badlion":
                    badlionTwoButtonRenderer.render(mouseX, mouseY, Minecraft.getMinecraft());
                    break;
                case "melon":
                    melonButtonRenderer.render(mouseX, mouseY, Minecraft.getMinecraft());
                    break;
                case "blackout":
                    blackoutButtonRenderer.render(mouseX, mouseY, Minecraft.getMinecraft());
                    break;
                case "fline":
                    fLineButtonRenderer.render(mouseX, mouseY, Minecraft.getMinecraft());
                    break;
                case "hyperium":
                    hyperiumButtonRenderer.render(mouseX, mouseY, Minecraft.getMinecraft());
                    break;
                case "liquid":
                    liquidButtonRenderer.render(mouseX, mouseY, Minecraft.getMinecraft());
                    break;
                case "lunar":
                    lunarButtonRenderer.render(mouseX, mouseY, Minecraft.getMinecraft());
                    break;
                case "pvpclient":
                    pvPClientButtonRenderer.render(mouseX, mouseY, Minecraft.getMinecraft());
                    break;
                case "wolfram":
                    wolframButtonRenderer.render(mouseX, mouseY, Minecraft.getMinecraft());
                    break;
            }

            if (hud.getButtonStyle().equalsIgnoreCase("minecraft")) return;

            mc.getTextureManager().bindTexture(buttonTextures);
            mouseDragged(mc, mouseX, mouseY);

            AWTFontRenderer.Companion.setAssumeNonVolatile(true);

            if (hud.getButtonStyle().equalsIgnoreCase("prideplus")) {
                prideFont.drawStringWithShadow(displayString,
                        (float) ((this.xPosition + this.width / 2) -
                                fontRenderer.getStringWidth(displayString) / 2),
                        this.yPosition + 2 + (this.height - 5) / 2F, Color.WHITE.getRGB());
            } else {
                //fontRenderer.drawStringWithShadow(displayString,
                //        (float) ((this.xPosition + this.width / 2) -
                //                fontRenderer.getStringWidth(displayString) / 2),
                //        this.yPosition + (this.height - 5) / 2F - 2, 14737632);
                lunarButtonRenderer.drawButtonText(Minecraft.getMinecraft());
            }


            AWTFontRenderer.Companion.setAssumeNonVolatile(false);

            GlStateManager.resetColor();
        }
    }
}