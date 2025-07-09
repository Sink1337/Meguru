package dev.tenacity.module.impl.render;

import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.render.Render2DEvent;
import dev.tenacity.event.impl.render.ShaderEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.impl.combat.KillAura;
import dev.tenacity.module.impl.player.ChestStealer;
import dev.tenacity.module.impl.player.InvManager;
import dev.tenacity.module.settings.ParentAttribute;
import dev.tenacity.module.settings.impl.*;
import dev.tenacity.utils.animations.Animation;
import dev.tenacity.utils.animations.Direction;
import dev.tenacity.utils.animations.impl.DecelerateAnimation;
import dev.tenacity.utils.font.AbstractFontRenderer;
import dev.tenacity.utils.font.CustomFont;
import dev.tenacity.utils.font.FontUtil;
import dev.tenacity.utils.misc.RomanNumeralUtils;
import dev.tenacity.utils.player.MovementUtils;
import dev.tenacity.utils.render.*;
import dev.tenacity.utils.server.PingerUtils;
import dev.tenacity.utils.tuples.Pair;
import dev.tenacity.viamcp.ViaMCP;
import dev.tenacity.viamcp.protocols.ProtocolCollection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class HUDMod extends Module {

    private final StringSetting clientName = new StringSetting("Client Name");
    private final ModeSetting watermarkMode = new ModeSetting("Watermark Mode", "Tenacity", "Tenacity", "Plain Text", "Neverlose", "Tenasense", "Exhi1", "Exhi2", "Logo", "None");
    public static final ColorSetting color1 = new ColorSetting("Color 1", new Color(0xffa028d4));
    public static final ColorSetting color2 = new ColorSetting("Color 2", new Color(0xff0008ff));
    public static final ModeSetting theme = Theme.getModeSetting("Theme Selection", "Tenacity");
    public static final BooleanSetting customFont = new BooleanSetting("Custom Font", true);
    public static final BooleanSetting fontChat = new BooleanSetting("Font Chat", false);
    public final ModeSetting customFontMode = new ModeSetting("Font Mode", "Tenacity", "Tenacity", "Inter");
    public static BooleanSetting customStatsRender = null;

    public static final BooleanSetting specialsound = new BooleanSetting("Special Sound", true);
    public static final NumberSetting specicalsoundVolume = new NumberSetting("Special Sound Volume", 0.8, 1.0, 0.1, 0.1);
    public static final BooleanSetting togglesound = new BooleanSetting("Toggle Sound", true);
    public static final ModeSetting togglesoundmode = new ModeSetting("Toggle Sound Mode", "Click", "Click", "Sigma", "YuzuSoft");
    public static final NumberSetting togglesoundVolume = new NumberSetting("Toggle Sound Volume", 1.0, 1.0, 0.1, 0.1);

    private static final MultipleBoolSetting infoCustomization = new MultipleBoolSetting("Info Options",
            new BooleanSetting("Show Bottom Right Text", true),
            new BooleanSetting("Show FPS", true),
            new BooleanSetting("Show Speed", true),
            new BooleanSetting("Show XYZ", true),
            new BooleanSetting("Show Ping", false),
            new BooleanSetting("Semi-Bold Info", true),
            new BooleanSetting("White Info", false),
            new BooleanSetting("Info Shadow", true));

    private static final MultipleBoolSetting watermarkCustomization = new MultipleBoolSetting("WaterMark Options",
            new BooleanSetting("Protocol Version", true),
            new BooleanSetting("Client Version", false),
            new BooleanSetting("FPS", true),
            new BooleanSetting("BPS", true),
            new BooleanSetting("Health", true),
            new BooleanSetting("Ping", true),
            new BooleanSetting("Time", true));

    public static final MultipleBoolSetting hudCustomization = new MultipleBoolSetting("HUD Options",
            customStatsRender = new BooleanSetting("Stats HUD", true),
            new BooleanSetting("Radial Gradients", true),
            new BooleanSetting("Potion HUD", true),
            new BooleanSetting("Armor HUD", true),
            new BooleanSetting("Render Cape", true),
            new BooleanSetting("Lowercase", false));

    private static final MultipleBoolSetting disableButtons = new MultipleBoolSetting("Disable Buttons",
            new BooleanSetting("Disable KillAura", true),
            new BooleanSetting("Disable InvManager", true),
            new BooleanSetting("Disable ChestStealer", true));

    public HUDMod() {
        super("HUD", Category.RENDER, "customizes the client's appearance");
        color1.addParent(theme, modeSetting -> modeSetting.is("Custom Theme"));
        color2.addParent(theme, modeSetting -> modeSetting.is("Custom Theme") && !color1.isRainbow());
        specicalsoundVolume.addParent(specialsound, ParentAttribute.BOOLEAN_CONDITION);
        togglesoundmode.addParent(togglesound, ParentAttribute.BOOLEAN_CONDITION);
        togglesoundVolume.addParent(togglesound, ParentAttribute.BOOLEAN_CONDITION);
        this.addSettings(clientName, watermarkMode, theme, color1, color2, customFont, fontChat, customFontMode, specialsound, specicalsoundVolume, togglesound, togglesoundmode, togglesoundVolume, infoCustomization, watermarkCustomization, hudCustomization, disableButtons);
        if (!enabled) this.toggleSilent();
    }

    public static int offsetValue = 0;
    private final Animation fadeInText = new DecelerateAnimation(500, 1);
    private int ticks = 0;

    private boolean version = true;

    public static float xOffset = 0;

    @Override
    public void onShaderEvent(ShaderEvent e) {
        Pair<Color, Color> clientColors = getClientColors();
        String name = Tenacity.NAME;


        if (e.isBloom()) {
            boolean glow = e.getBloomOptions().getSetting("Watermark").isEnabled();
            if (!glow) {
                clientColors = Pair.of(Color.BLACK);
            }

            if (!clientName.getString().equals("")) {
                name = clientName.getString().replace("%time%", getCurrentTimeStamp());
            }


            String finalName = get(name);
            String intentInfo = Tenacity.INSTANCE.getIntentAccount().username;
            switch (watermarkMode.getMode()) {
                case "Logo":
                    float WH = 110 / 2f;
                    float textWidth = FontUtil.tenacityBoldFont32.getStringWidth(finalName);

                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                    RenderUtil.scissor(10, 7, 13 + WH + textWidth + 5, WH);

                    FontUtil.tenacityBoldFont32.drawString(finalName, (float) (((13 + WH) - textWidth) + (textWidth * fadeInText.getOutput().floatValue())), 8 + FontUtil.tenacityBoldFont32.getMiddleOfBox(WH), ColorUtil.applyOpacity(glow ? -1 : 0, (float) (fadeInText.getOutput().floatValue())));
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);


                    GradientUtil.applyGradientCornerLR(27, 23, WH - 28, WH - 28, 1, clientColors.getSecond(), clientColors.getFirst(), () -> {
                        mc.getTextureManager().bindTexture(new ResourceLocation("Tenacity/watermarkBack.png"));
                        Gui.drawModalRectWithCustomSizedTexture(7, 7, 0, 0, WH, WH, WH, WH);
                    });
                    break;
                case "Tenacity":
                    float xVal = 6f;
                    float yVal = 6f;
                    float versionWidth = FontUtil.tenacityFont16.getStringWidth(Tenacity.INSTANCE.getVersion());
                    float versionX = xVal + FontUtil.tenacityBoldFont40.getStringWidth(finalName);
                    float width = version ? (versionX + versionWidth) - xVal : FontUtil.tenacityBoldFont40.getStringWidth(finalName);


                    RenderUtil.resetColor();
                    GradientUtil.applyGradientHorizontal(xVal, yVal, width, 20, 1, clientColors.getFirst(), clientColors.getSecond(), () -> {
                        RenderUtil.setAlphaLimit(0);
                        FontUtil.tenacityBoldFont40.drawString(finalName, xVal, yVal, 0);
                        if (version) {
                            FontUtil.tenacityFont16.drawString(Tenacity.INSTANCE.getVersion(), versionX, yVal, 0);
                        }
                    });

                    break;
                case "Plain Text":
                    AbstractFontRenderer fr = mc.fontRendererObj;
                    if (customFont.isEnabled()) {
                        fr = FontUtil.tenacityFont24;
                    }
                    AbstractFontRenderer finalFr = fr;

                    GradientUtil.applyGradientHorizontal(5, 5, finalFr.getStringWidth(finalName), finalFr.getHeight(), 1,
                            clientColors.getFirst(), clientColors.getSecond(), () -> {
                                RenderUtil.setAlphaLimit(0);
                                finalFr.drawString(finalName, 5.5f, 5.5f, new Color(0, 0, 0, 0).getRGB());
                            });

                    break;
                case "Neverlose":
                    CustomFont t18 = FontUtil.tenacityFont18;
                    String str = String.format(" §8|§f %s fps §8|§f %s §8|§f %s",
                            Minecraft.getDebugFPS(), intentInfo,
                            mc.isSingleplayer() || mc.getCurrentServerData() == null ? "singleplayer" : mc.getCurrentServerData().serverIP);
                    name = name.toUpperCase();
                    float nw = FontUtil.neverloseFont.size(22).getStringWidth(name);
                    RoundedUtil.drawRound(4, 4.5F, nw + t18.getStringWidth(str) + 6f, t18.getHeight() + 6, 2, Color.BLACK);
                    break;
                case "Tenasense":
                    String text = "§ftena§rsense§f" + " - " + intentInfo + " - " + (mc.isSingleplayer() ? "singleplayer" : mc.getCurrentServerData().serverIP) + " - "
                            + PingerUtils.getPing() + "ms ";
                    float x = 4.5f, y = 4.5f;

                    Gui.drawRect2(x, y, FontUtil.tenacityFont16.getStringWidth(text) + 7, 18.5, glow ? new Color(59, 57, 57).getRGB() : Color.BLACK.getRGB());
                    break;
            }
        }
    }

    @Override
    public void onRender2DEvent(Render2DEvent e) {
        ScaledResolution sr = new ScaledResolution(mc);
        Pair<Color, Color> clientColors = getClientColors();
        String name = Tenacity.NAME;
        PostProcessing postProcessing = Tenacity.INSTANCE.getModuleCollection().getModule(PostProcessing.class);
        if (!postProcessing.isEnabled()) {
            version = false;
        }

        if (!clientName.getString().equals("")) {
            name = clientName.getString().replace("%time%", getCurrentTimeStamp());
        }

        version = name.equalsIgnoreCase(Tenacity.NAME);

        String finalName = get(name);
        String intentInfo = Tenacity.INSTANCE.getIntentAccount().username;

        switch (watermarkMode.getMode()) {
            case "Logo":

                float WH = 110 / 2f;

                if (MovementUtils.isMoving()) {
                    ticks = 0;
                } else {
                    ticks = Math.min(ticks + 1, 301);
                }

                fadeInText.setDirection(ticks < 300 ? Direction.BACKWARDS : Direction.FORWARDS);
                float textWidth = FontUtil.tenacityBoldFont32.getStringWidth(finalName);

                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                RenderUtil.scissor(10, 7, 13 + WH + textWidth + 5, WH);

                FontUtil.tenacityBoldFont32.drawString(finalName, (float) (((13 + WH) - textWidth) + (textWidth * fadeInText.getOutput().floatValue())), 8 + FontUtil.tenacityBoldFont32.getMiddleOfBox(WH), ColorUtil.applyOpacity(-1, (float) (.7f * fadeInText.getOutput().floatValue())));
                GL11.glDisable(GL11.GL_SCISSOR_TEST);

                RenderUtil.color(Color.BLUE.getRGB());

                GradientUtil.applyGradientCornerLR(27, 23, WH - 28, WH - 28, 1, clientColors.getSecond(), clientColors.getFirst(), () -> {
                    mc.getTextureManager().bindTexture(new ResourceLocation("Tenacity/watermarkBack.png"));
                    Gui.drawModalRectWithCustomSizedTexture(7, 7, 0, 0, WH, WH, WH, WH);
                });

                RenderUtil.color(-1);
                GLUtil.startBlend();
                mc.getTextureManager().bindTexture(new ResourceLocation("Tenacity/watermarkT.png"));
                Gui.drawModalRectWithCustomSizedTexture(7, 7, 0, 0, WH, WH, WH, WH);

                break;

            case "Tenacity":
                float xVal = 5;
                float yVal = 5;
                float spacing = 1;
                float versionWidth = FontUtil.tenacityFont16.getStringWidth(Tenacity.INSTANCE.getVersion());
                float versionX = xVal + FontUtil.tenacityBoldFont40.getStringWidth(finalName);
                float width = version ? (versionX + versionWidth) - xVal : FontUtil.tenacityBoldFont40.getStringWidth(finalName);
                String finalName1 = finalName;

                Pair<Color, Color> darkerColors = clientColors.apply((c1, c2) -> Pair.of(ColorUtil.darker(c1, .6f), ColorUtil.darker(c2, .6f)));

                GradientUtil.applyGradientHorizontal(xVal + spacing, yVal + spacing, width + spacing,
                        20, 1, darkerColors.getFirst(), darkerColors.getSecond(), () -> {
                            RenderUtil.setAlphaLimit(0);
                            FontUtil.tenacityBoldFont40.drawString(finalName1, xVal + spacing, yVal + spacing, 0);
                            if (version) {
                                FontUtil.tenacityFont16.drawString(Tenacity.INSTANCE.getVersion(), versionX + (spacing / 2f), yVal + (spacing / 2f), 0);
                            }
                        });


                RenderUtil.resetColor();
                GradientUtil.applyGradientHorizontal(xVal, yVal, width, 20, 1, clientColors.getFirst(), clientColors.getSecond(), () -> {
                    RenderUtil.setAlphaLimit(0);
                    FontUtil.tenacityBoldFont40.drawString(finalName1, xVal, yVal, 0);
                    if (version) {
                        FontUtil.tenacityFont16.drawString(Tenacity.INSTANCE.getVersion(), versionX, yVal, 0);
                    }
                });
                break;
            case "Plain Text":
                AbstractFontRenderer frPlain = mc.fontRendererObj;
                if (customFont.isEnabled()) {
                    frPlain = FontUtil.tenacityBoldFont26;
                }
                AbstractFontRenderer finalFrPlain = frPlain;
                finalName = get(name);

                finalFrPlain.drawString(finalName, 5.5f, 5.5f, Color.BLACK.getRGB());


                String finalName2 = finalName;
                GradientUtil.applyGradientHorizontal(5, 5, finalFrPlain.getStringWidth(finalName), finalFrPlain.getHeight(), 1, clientColors.getFirst(), clientColors.getSecond(), () -> {
                    RenderUtil.setAlphaLimit(0);
                    finalFrPlain.drawString(finalName2, 5, 5, new Color(0, 0, 0, 0).getRGB());
                });
                break;
            case "Neverlose":
                CustomFont m22 = FontUtil.neverloseFont.size(22), t18 = FontUtil.tenacityFont18;
                String str = String.format(" §8|§f %s fps §8|§f %s §8|§f %s",
                        Minecraft.getDebugFPS(), intentInfo,
                        mc.isSingleplayer() || mc.getCurrentServerData() == null ? "singleplayer" : mc.getCurrentServerData().serverIP);
                name = name.toUpperCase();
                float nw = m22.getStringWidth(name);
                RoundedUtil.drawRound(4, 4.5F, nw + t18.getStringWidth(str) + 6f, t18.getHeight() + 6, 2, Color.BLACK);
                t18.drawString(str, 7.5F + nw, 7.5F, -1);
                m22.drawString(name, 7.5F, 8, new Color(0, 149, 200).getRGB());
                m22.drawString(name, 7, 7.5F, -1);
                break;
            case "Tenasense":
                String text = "§ftena§rsense§f" + " - " + intentInfo + " - " + (mc.isSingleplayer() ? "singleplayer" : mc.getCurrentServerData().serverIP) + " - "
                        + PingerUtils.getPing() + "ms ";

                float x = 4.5f, y = 4.5f;

                int lineColor = new Color(59, 57, 57).darker().getRGB();
                Gui.drawRect2(x, y, FontUtil.tenacityFont16.getStringWidth(text) + 7, 18.5, new Color(59, 57, 57).getRGB());

                Gui.drawRect2(x + 2.5, y + 2.5, FontUtil.tenacityFont16.getStringWidth(text) + 2, 13, new Color(23, 23, 23).getRGB());

                // Top small bar
                Gui.drawRect2(x + 1, y + 1, FontUtil.tenacityFont16.getStringWidth(text) + 5, .5, lineColor);

                // Bottom small bar
                Gui.drawRect2(x + 1, y + 17, FontUtil.tenacityFont16.getStringWidth(text) + 5, .5, lineColor);

                // Left bar
                Gui.drawRect2(x + 1, y + 1.5, .5, 16, lineColor);

                // Right Bar
                Gui.drawRect2((x + 1.5) + FontUtil.tenacityFont16.getStringWidth(text) + 4, y + 1.5, .5, 16, lineColor);

                // Lowly saturated rainbow bar
                GradientUtil.drawGradientLR(x + 2.5f, y + 14.5f, FontUtil.tenacityFont16.getStringWidth(text) + 2, 1, 1, clientColors.getFirst(), clientColors.getSecond());

                // Bottom of the rainbow bar
                Gui.drawRect2(x + 2.5, y + 16, FontUtil.tenacityFont16.getStringWidth(text) + 2, .5, lineColor);
                FontUtil.tenacityFont16.drawString(text, x + 4.5f, y + 5.5f, clientColors.getSecond().getRGB());
                break;
            case "Exhi1":
                StringBuilder stringBuilder = new StringBuilder(name.replace("tenacity", "Exhi1")).insert(1, "§7");
                if (watermarkCustomization.getSetting("Protocol Version").isEnabled()) {
                    String fullProtocolString = ProtocolCollection.getProtocolById(ViaMCP.PROTOCOL_VERSION).toString();
                    String displayProtocol;

                    int indexOfParen = fullProtocolString.indexOf('(');

                    if (indexOfParen != -1) {
                        displayProtocol = fullProtocolString.substring(0, indexOfParen);
                    } else {
                        displayProtocol = fullProtocolString;
                    }
                    displayProtocol = displayProtocol.trim();

                    stringBuilder.append(" [§f").append(displayProtocol).append("§7]");
                }
                if (watermarkCustomization.getSetting("Client Version").isEnabled()) {
                    stringBuilder.append(" [§f").append(Tenacity.VERSION).append("§7]");
                }
                if (watermarkCustomization.getSetting("Time").isEnabled()) {
                    stringBuilder.append(" [§f").append(getCurrentTimeStamp()).append("§7]");
                }
                if (watermarkCustomization.getSetting("FPS").isEnabled()) {
                    stringBuilder.append(" [§f").append(Minecraft.getDebugFPS()).append(" FPS§7]");
                }
                if (watermarkCustomization.getSetting("BPS").isEnabled()) {
                    stringBuilder.append(" [§f").append(calculateBPS()).append(" BPS§7]");
                }
                if (watermarkCustomization.getSetting("Health").isEnabled()) {
                    stringBuilder.append(" [§f").append(Minecraft.getMinecraft().thePlayer.getHealth()).append("§7]");
                }
                if (watermarkCustomization.getSetting("Ping").isEnabled()) {
                    stringBuilder.append(" [§f").append(PingerUtils.getPing()).append("ms§7]");
                }
                RenderUtil.resetColor();
                mc.fontRendererObj.drawStringWithShadow(stringBuilder.toString(), 2, 2, clientColors.getFirst().getRGB());
                break;
            case "Exhi2" :
                StringBuilder stringBuilder1 = new StringBuilder(name.replace("tenacity", "Exhi1")).insert(1, "§7");
                if (watermarkCustomization.getSetting("Protocol Version").isEnabled()) {
                    String fullProtocolString = ProtocolCollection.getProtocolById(ViaMCP.PROTOCOL_VERSION).toString();
                    String displayProtocol;

                    int indexOfParen = fullProtocolString.indexOf('(');

                    if (indexOfParen != -1) {
                        displayProtocol = fullProtocolString.substring(0, indexOfParen);
                    } else {
                        displayProtocol = fullProtocolString;
                    }
                    displayProtocol = displayProtocol.trim();

                    stringBuilder1.append(" [§f").append(displayProtocol).append("§7]");
                }
                if (watermarkCustomization.getSetting("Client Version").isEnabled()) {
                    stringBuilder1.append(" [§f").append(Tenacity.VERSION).append("§7]");
                }
                if (watermarkCustomization.getSetting("Time").isEnabled()) {
                    stringBuilder1.append(" [§f").append(getCurrentTimeStamp()).append("§7]");
                }
                if (watermarkCustomization.getSetting("FPS").isEnabled()) {
                    stringBuilder1.append(" [§f").append(Minecraft.getDebugFPS()).append(" FPS§7]");
                }
                if (watermarkCustomization.getSetting("BPS").isEnabled()) {
                    stringBuilder1.append(" [§f").append(calculateBPS()).append(" BPS§7]");
                }
                if (watermarkCustomization.getSetting("Health").isEnabled()) {
                    stringBuilder1.append(" [§f").append(Minecraft.getMinecraft().thePlayer.getHealth()).append("§7]");
                }
                if (watermarkCustomization.getSetting("Ping").isEnabled()) {
                    stringBuilder1.append(" [§f").append(PingerUtils.getPing()).append("ms§7]");
                }
                FontUtil.interFont.boldSize(18).drawStringWithShadow(stringBuilder1.toString(), 2, 3, clientColors.getFirst().getRGB());
        }


        RenderUtil.resetColor();
        drawBottomRight();

        RenderUtil.resetColor();
        drawInfo(clientColors);

        drawArmor(sr);
    }

    private void drawBottomRight() {
        AbstractFontRenderer frToUse = mc.fontRendererObj;
        if (customFont.isEnabled()) {
            frToUse = customFontMode.is("Inter") ? FontUtil.interFont18 : FontUtil.tenacityFont20;
        }
        final AbstractFontRenderer finalFr = frToUse;

        ScaledResolution sr = new ScaledResolution(mc);
        float yOffset = (float) (14.5 * GuiChat.openingAnimation.getOutput().floatValue());

        boolean shadowInfo = infoCustomization.isEnabled("Info Shadow");

        if (hudCustomization.getSetting("Potion HUD").isEnabled()) {
            List<PotionEffect> potions = new ArrayList<>(mc.thePlayer.getActivePotionEffects());
            potions.sort(Comparator.comparingDouble(e -> -finalFr.getStringWidth(I18n.format(e.getEffectName()))));

            int count = 0;
            for (PotionEffect effect : potions) {
                Potion potion = Potion.potionTypes[effect.getPotionID()];
                String name = I18n.format(potion.getName()) + (effect.getAmplifier() > 0 ? " " + RomanNumeralUtils.generate(effect.getAmplifier() + 1) : "");
                Color c = new Color(potion.getLiquidColor());
                String str = get(name + " §7[" + Potion.getDurationString(effect) + "]");
                finalFr.drawString(str, sr.getScaledWidth() - finalFr.getStringWidth(str) - 2,
                        -10 + sr.getScaledHeight() - finalFr.getHeight() + (7 - (10 * (count + 1))) - yOffset,
                        new Color(c.getRed(), c.getGreen(), c.getBlue(), 255).getRGB(), shadowInfo);
                count++;
            }

            offsetValue = count * finalFr.getHeight();
        }

        if (infoCustomization.isEnabled("Show Bottom Right Text")) {
            String text = Tenacity.VERSION + " - " + (customFont.isEnabled() ? "" : "§l") + Tenacity.RELEASE.getName() + "§r";

            text += " - " + Tenacity.INSTANCE.getIntentAccount().username;
            text += " (" + Tenacity.INSTANCE.getIntentAccount().client_uid + ")";

            text = get(text);

            float x = sr.getScaledWidth() - (finalFr.getStringWidth(text) + 3);
            float y = sr.getScaledHeight() - (finalFr.getHeight() + 3) - yOffset;

            Pair<Color, Color> clientColors = getClientColors();
            String finalText = text;

            float f = customFont.isEnabled() ? 0.5F : 1.0F;
            finalFr.drawString(finalText, x + f, y + f, 0xFF000000);
            GradientUtil.applyGradientHorizontal(x, y, finalFr.getStringWidth(text), 20, 1, clientColors.getFirst(), clientColors.getSecond(), () -> {
                RenderUtil.setAlphaLimit(0);
                finalFr.drawString(finalText, x, y, -1);
            });
        }
    }

    private final Map<String, String> bottomLeftText = new LinkedHashMap<>();

    private void drawInfo(Pair<Color, Color> clientColors) {
        boolean shadowInfo = infoCustomization.isEnabled("Info Shadow");
        boolean semiBold = infoCustomization.isEnabled("Semi-Bold Info");
        boolean whiteInfo = infoCustomization.isEnabled("White Info");
        String titleBold = semiBold ? "§l" : "";
        ScaledResolution sr = new ScaledResolution(mc);

        bottomLeftText.clear();

        if (infoCustomization.isEnabled("Show XYZ")) {
            bottomLeftText.put("XYZ", Math.round(mc.thePlayer.posX) + " " + Math.round(mc.thePlayer.posY) + " " + Math.round(mc.thePlayer.posZ));
        }
        if (infoCustomization.isEnabled("Show Speed")) {
            bottomLeftText.put("Speed", String.valueOf(calculateBPS()));
        }
        if (infoCustomization.isEnabled("Show FPS")) {
            bottomLeftText.put("FPS", String.valueOf(Minecraft.getDebugFPS()));
        }

        if (infoCustomization.isEnabled("Show Ping")) {
            bottomLeftText.put("Ping", PingerUtils.getPing());
            GuiNewChat.chatPos = 17 - 7;
        } else {
            GuiNewChat.chatPos = 17;
            bottomLeftText.remove("Ping");
        }

        //InfoStuff
        AbstractFontRenderer nameInfoFrToUse = mc.fontRendererObj;
        if (customFont.isEnabled()) {
            nameInfoFrToUse = customFontMode.is("Inter") ? FontUtil.interFont18 : FontUtil.tenacityFont20;
        }
        final AbstractFontRenderer finalNameInfoFr = nameInfoFrToUse;


        if (semiBold) {
            float maxWidth = 0;
            for (Map.Entry<String, String> line : bottomLeftText.entrySet()) {
                float currentWidth = finalNameInfoFr.getStringWidth("§l" + line.getKey() + "§r: " + line.getValue());
                if (currentWidth > maxWidth) {
                    maxWidth = currentWidth;
                }
            }
            xOffset = maxWidth;
        } else {
            float maxWidth = 0;
            for (Map.Entry<String, String> line : bottomLeftText.entrySet()) {
                float currentWidth = finalNameInfoFr.getStringWidth(line.getKey() + ": " + line.getValue());
                if (currentWidth > maxWidth) {
                    maxWidth = currentWidth;
                }
            }
            xOffset = maxWidth;
        }


        float yOffset = (float) (14.5 * GuiChat.openingAnimation.getOutput().floatValue());
        float f2 = customFont.isEnabled() ? 0.5F : 1.0F;
        float f3 = customFont.isEnabled() ? 1 : 0.5F;
        float yMovement = !customFont.isEnabled() ? -1 : 0;


        if (whiteInfo) {
            float boldFontMovement = finalNameInfoFr.getHeight() + 2 + yOffset + yMovement;
            for (Map.Entry<String, String> line : bottomLeftText.entrySet()) {
                finalNameInfoFr.drawString(get(titleBold + line.getKey() + "§r: " + line.getValue()), 2, sr.getScaledHeight() - boldFontMovement, -1, shadowInfo);
                boldFontMovement += finalNameInfoFr.getHeight() + f3;
            }
        } else {

            float f = finalNameInfoFr.getHeight() + 2 + yOffset + yMovement;
            for (Map.Entry<String, String> line : bottomLeftText.entrySet()) {
                if (shadowInfo) {
                    finalNameInfoFr.drawString(get(line.getValue()), 2 + f2 + finalNameInfoFr.getStringWidth(titleBold + line.getKey() + ":§r "), sr.getScaledHeight() - f + f2, 0xFF000000);
                }
                finalNameInfoFr.drawString(get(line.getValue()), 2 + finalNameInfoFr.getStringWidth(titleBold + line.getKey() + ":§r "), sr.getScaledHeight() - f, -1);
                f += finalNameInfoFr.getHeight() + f3;
            }

            f = finalNameInfoFr.getHeight() + 2 + yOffset + yMovement;
            float totalHeight = (finalNameInfoFr.getHeight() + f3) * bottomLeftText.size();
            float maxWidthKey = 0;
            for (Map.Entry<String, String> line : bottomLeftText.entrySet()) {
                float currentKeyWidth = finalNameInfoFr.getStringWidth(titleBold + line.getKey() + ": ");
                if (currentKeyWidth > maxWidthKey) {
                    maxWidthKey = currentKeyWidth;
                }
            }

            final AbstractFontRenderer finalFrForGradient = finalNameInfoFr;

            if (shadowInfo) {
                float boldFontMovement1 = finalFrForGradient.getHeight() + 2 + yOffset + yMovement;
                for (Map.Entry<String, String> line : bottomLeftText.entrySet()) {
                    finalFrForGradient.drawString(get(titleBold + line.getKey() + ": "), 2 + f2, sr.getScaledHeight() - boldFontMovement1 + f2, 0xFF000000);
                    boldFontMovement1 += finalFrForGradient.getHeight() + f3;
                }
            }

            GradientUtil.applyGradientVertical(2, sr.getScaledHeight() - (totalHeight + yOffset + yMovement - f3), maxWidthKey, totalHeight, 1, clientColors.getFirst(), clientColors.getSecond(), () -> {
                float boldFontMovement = finalFrForGradient.getHeight() + 2 + yOffset + yMovement;
                for (Map.Entry<String, String> line : bottomLeftText.entrySet()) {
                    finalFrForGradient.drawString(get(titleBold + line.getKey() + ": "), 2, sr.getScaledHeight() - boldFontMovement, -1);
                    boldFontMovement += finalFrForGradient.getHeight() + f3;
                }
            });
        }
    }

    private double calculateBPS() {
        double bps = (Math.hypot(mc.thePlayer.posX - mc.thePlayer.prevPosX, mc.thePlayer.posZ - mc.thePlayer.prevPosZ) * mc.timer.timerSpeed) * 20;
        return Math.round(bps * 100.0) / 100.0;
    }

    public static Pair<Color, Color> getClientColors() {
        return Theme.getThemeColors(theme.getMode());
    }

    public static ModeSetting getCustomFontMode() {
        return ((HUDMod) Tenacity.INSTANCE.getModuleCollection().getModule(HUDMod.class)).customFontMode;
    }

    public static String getCurrentTimeStamp() {
        return new SimpleDateFormat("hh:mm a",Locale.ENGLISH).format(new Date());
    }

    public static String get(String text) {
        return hudCustomization.getSetting("Lowercase").isEnabled() ? text.toLowerCase() : text;
    }

    private void drawArmor(ScaledResolution sr) {
        if (hudCustomization.getSetting("Armor HUD").isEnabled()) {
            List<ItemStack> equipment = new ArrayList<>();
            boolean inWater = mc.thePlayer.isEntityAlive() && mc.thePlayer.isInsideOfMaterial(Material.water);
            int x = -74;
            ItemStack armorPiece;

            for (int i = 3; i >= 0; i--) {
                if ((armorPiece = mc.thePlayer.inventory.armorInventory[i]) != null) {
                    equipment.add(armorPiece);
                }
            }
            ItemStack heldItem = mc.thePlayer.getHeldItem();
            if (heldItem != null && heldItem.getItem() != null) {
                equipment.add(heldItem);
            }
            Collections.reverse(equipment);

            for (ItemStack itemStack : equipment) {
                armorPiece = itemStack;
                RenderHelper.enableGUIStandardItemLighting();
                x += 15;
                if (armorPiece == mc.thePlayer.getHeldItem()) {
                    x -= 15;
                }
                GlStateManager.pushMatrix();
                GlStateManager.disableAlpha();
                GlStateManager.clear(256);
                mc.getRenderItem().zLevel = -200.0F;
                int s = mc.thePlayer.capabilities.isCreativeMode ? 15 : 0;
                mc.getRenderItem().renderItemAndEffectIntoGUI(armorPiece, -x + sr.getScaledWidth() / 2 - 4,
                        (int) (sr.getScaledHeight() - (inWater ? 65 : 55) + s - 5 - (16 * GuiChat.openingAnimation.getOutput().floatValue())));

                if (armorPiece.stackSize > 1) {
                    String count = String.valueOf(armorPiece.stackSize);
                    mc.fontRendererObj.drawStringWithShadow(count,
                            -x + sr.getScaledWidth() / 2 - 4 + 19 - 2 - mc.fontRendererObj.getStringWidth(count),
                            sr.getScaledHeight() - (inWater ? 65 : 55) + s - 5 - (16 * GuiChat.openingAnimation.getOutput().floatValue()) + 9, 16777215);
                }

                mc.getRenderItem().zLevel = 0.0F;
                GlStateManager.disableBlend();
                GlStateManager.disableDepth();
                GlStateManager.disableLighting();
                GlStateManager.enableDepth();
                GlStateManager.enableAlpha();
                GlStateManager.popMatrix();
                armorPiece.getEnchantmentTagList();

                RenderUtil.drawExhiEnchants(armorPiece, -x + sr.getScaledWidth() / 2 - 4, sr.getScaledHeight() - (inWater ? 65 : 55) + s - 5 - (16 * GuiChat.openingAnimation.getOutput().floatValue()));
            }
        }
    }


    public static boolean isRainbowTheme() {
        return theme.is("Custom Theme") && color1.isRainbow();
    }

    public static boolean drawRadialGradients() {
        return hudCustomization.getSetting("Radial Gradients").isEnabled();
    }

    public static void addButtons(List<GuiButton> buttonList) {
        for (ModuleButton mb : ModuleButton.values()) {
            if (mb.getSetting().isEnabled()) {
                buttonList.add(mb.getButton());
            }
        }
    }

    public static void updateButtonStatus() {
        for (ModuleButton mb : ModuleButton.values()) {
            mb.getButton().enabled = Tenacity.INSTANCE.getModuleCollection().getModule(mb.getModule()).isEnabled();
        }
    }

    public static void handleActionPerformed(GuiButton button) {
        for (ModuleButton mb : ModuleButton.values()) {
            if (mb.getButton() == button) {
                Module m = Tenacity.INSTANCE.getModuleCollection().getModule(mb.getModule());
                if (m.isEnabled()) {
                    m.toggle();
                }
                break;
            }
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ModuleButton {
        AURA(KillAura.class, disableButtons.getSetting("Disable KillAura"), new GuiButton(2461, 3, 4, 120, 20, "Disable KillAura")),
        INVMANAGER(InvManager.class, disableButtons.getSetting("Disable InvManager"), new GuiButton(2462, 3, 26, 120, 20, "Disable InvManager")),
        CHESTSTEALER(ChestStealer.class, disableButtons.getSetting("Disable ChestStealer"), new GuiButton(2463, 3, 48, 120, 20, "Disable ChestStealer"));

        private final Class<? extends Module> module;
        private final BooleanSetting setting;
        private final GuiButton button;
    }
}