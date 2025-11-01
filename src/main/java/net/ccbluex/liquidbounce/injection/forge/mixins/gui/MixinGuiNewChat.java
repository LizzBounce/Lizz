/*
 * Lizz Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/LizzBounce/Lizz/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.ccbluex.liquidbounce.features.module.modules.misc.Patcher;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.LBPPRenderUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Shadow
    public abstract void printChatMessageWithOptionalDeletion(IChatComponent p_printChatMessageWithOptionalDeletion_1_, int p_printChatMessageWithOptionalDeletion_2_);


    @Shadow
    public abstract boolean getChatOpen();

    @Shadow
    @Final
    private Minecraft mc;
    @Shadow
    private int scrollPos;
    @Shadow
    private boolean isScrolled;

    @Shadow
    public abstract int getLineCount();

    @Shadow
    public abstract float getChatScale();

    @Shadow
    public abstract int getChatWidth();

    @Shadow
    @Final
    private List<ChatLine> drawnChatLines;
    private int line;
    private int sameMessageAmount;

    private float displayPercent, animationPercent = 0F;
    private int lineBeingDrawn, newLines;

    private HUD hud;

    private String lastMessage;
    private final HashMap<String,String> stringCache=new HashMap<>();

    private void checkHud() {
        if (hud == null)
            hud = HUD.INSTANCE;
    }


    @Redirect(method = "deleteChatLine", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ChatLine;getChatLineID()I"))
    private int patcher$checkIfChatLineIsNull(ChatLine instance) {
        if (instance == null) return -1;
        return instance.getChatLineID();
    }

    private String fixString(String str) {
        if (stringCache.containsKey(str)) return stringCache.get(str);

        str = str.replaceAll("\uF8FF", "");//remove air chars

        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if ((int) c > (33 + 65248) && (int) c < (128 + 65248))
                sb.append(Character.toChars((int) c - 65248));
            else
                sb.append(c);
        }

        String result = sb.toString();
        stringCache.put(str, result);

        return result;
    }

    @Overwrite
    public void printChatMessage(IChatComponent chatComponent) {
        checkHud();
        if(!hud.getChatCombineValue()) {
            printChatMessageWithOptionalDeletion(chatComponent, this.line);
            return;
        }

        String text=fixString(chatComponent.getFormattedText());
        if (text.equals(this.lastMessage)) {
            (Minecraft.getMinecraft()).ingameGUI.getChatGUI().deleteChatLine(this.line);
            this.sameMessageAmount++;
            this.lastMessage = text;
            chatComponent.appendText(ChatFormatting.WHITE + " (" + "x" + this.sameMessageAmount + ")");
        } else {
            this.sameMessageAmount = 1;
            this.lastMessage = text;
        }
        this.line++;
        if (this.line > 256)
            this.line = 0;

        printChatMessageWithOptionalDeletion(chatComponent, this.line);
    }

    @Overwrite
    public void drawChat(int updateCounter) {
        checkHud();
        boolean canFont = hud.getState() && hud.getFontChatValue();

        if (Patcher.INSTANCE.getChatPosition()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, -12, 0);
        }

        if (this.mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN) {
            int i = this.getLineCount();
            boolean flag = false;
            int j = 0;
            int k = this.drawnChatLines.size();
            float f = this.mc.gameSettings.chatOpacity * 0.9F + 0.1F;
            if (k > 0) {
                if (this.getChatOpen()) {
                    flag = true;
                }

                if (this.isScrolled || !hud.getState() || !hud.getChatAnimationValue()) {
                    displayPercent = 1F;
                } else if (displayPercent < 1F) {
                    displayPercent += hud.getChatAnimationSpeedValue() / 10F * RenderUtils.INSTANCE.getDeltaTime();
                    displayPercent = MathHelper.clamp_float(displayPercent, 0F, 1F);
                }

                float t = displayPercent;
                animationPercent = MathHelper.clamp_float(1F - (--t) * t * t * t, 0F, 1F);

                float f1 = this.getChatScale();
                int l = MathHelper.ceiling_float_int((float)this.getChatWidth() / f1);
                GlStateManager.pushMatrix();
                if (hud.getState() && hud.getChatAnimationValue())
                    GlStateManager.translate(0F, (1F - animationPercent) * 9F * this.getChatScale(), 0F);
                GlStateManager.translate(2.0F, 20.0F, 0.0F);
                GlStateManager.scale(f1, f1, 1.0F);

                int i1;
                int j1;
                int l1;
                for(i1 = 0; i1 + this.scrollPos < this.drawnChatLines.size() && i1 < i; ++i1) {
                    ChatLine chatline = this.drawnChatLines.get(i1 + this.scrollPos);
                    lineBeingDrawn = i1 + this.scrollPos;
                    if (chatline != null) {
                        j1 = updateCounter - chatline.getUpdatedCounter();
                        if (j1 < 200 || flag) {
                            double d0 = (double)j1 / 200.0D;
                            d0 = 1.0D - d0;
                            d0 *= 10.0D;
                            d0 = MathHelper.clamp_double(d0, 0.0D, 1.0D);
                            d0 *= d0;
                            l1 = (int)(255.0D * d0);
                            if (flag) {
                                l1 = 255;
                            }

                            l1 = (int)((float)l1 * f);
                            ++j;

                            //Animation part
                            if (l1 > 3) {
                                int i2 = 0;
                                int j2 = -i1 * 9;

                                if(hud.getState() && hud.getChatRectValue()) {
                                    if (hud.getChatAnimationValue() && lineBeingDrawn <= newLines && !flag)
                                        LBPPRenderUtils.drawRect(i2, j2 - 9, i2 + l + 4, j2, new Color(0F, 0F, 0F, animationPercent * ((float)d0 / 2F)).getRGB());
                                    else
                                        LBPPRenderUtils.drawRect(i2, j2 - 9, i2 + l + 4, j2, l1 / 2 << 24);
                                }

                                GlStateManager.resetColor();
                                GlStateManager.color(1F, 1F, 1F, 1F);

                                String s = fixString(chatline.getChatComponent().getFormattedText());
                                GlStateManager.enableBlend();
                                if (hud.getState() && hud.getChatAnimationValue() && lineBeingDrawn <= newLines)
                                    (canFont?hud.getFontType():this.mc.fontRendererObj).drawString(s, (float)i2, (float)(j2 - 8), new Color(1F, 1F, 1F, animationPercent * (float)d0).getRGB(), true);
                                else
                                    (canFont?hud.getFontType():this.mc.fontRendererObj).drawString(s, (float)i2, (float)(j2 - 8), 16777215 + (l1 << 24), true);
                                GlStateManager.disableAlpha();
                                GlStateManager.disableBlend();
                            }
                        }
                    }
                }

                if (flag) {
                    i1 = mc.fontRendererObj.FONT_HEIGHT;
                    GlStateManager.translate(-3.0F, 0.0F, 0.0F);
                    int l2 = k * i1 + k;
                    j1 = j * i1 + j;
                    int j3 = this.scrollPos * j1 / k;
                    int k1 = j1 * j1 / l2;
                    if (l2 != j1) {
                        l1 = j3 > 0 ? 170 : 96;
                        int l3 = this.isScrolled ? 13382451 : 3355562;
                        LBPPRenderUtils.drawRect(0, -j3, 2, -j3 - k1, l3 + (l1 << 24));
                        LBPPRenderUtils.drawRect(2, -j3, 1, -j3 - k1, 13421772 + (l1 << 24));
                    }
                }

                GlStateManager.popMatrix();
            }
        }

        if (Patcher.INSTANCE.getChatPosition())
            GlStateManager.popMatrix();
    }
}
