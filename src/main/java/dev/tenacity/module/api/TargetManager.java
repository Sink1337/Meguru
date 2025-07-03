package dev.tenacity.module.api;

import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collection;

public class TargetManager extends Module {

    public static MultipleBoolSetting targetType = new MultipleBoolSetting(
            "Target Type",
            new BooleanSetting("Players", true),
            new BooleanSetting("Bots", false),
            new BooleanSetting("Mobs", false),
            new BooleanSetting("Animals", false),
            new BooleanSetting("Invisibles", true)
    );

    public static MultipleBoolSetting botDetectionModes = new MultipleBoolSetting(
            "Bot Detection Modes",
            new BooleanSetting("Broken ID", true),
            new BooleanSetting("No Tablist", true),
            new BooleanSetting("Newbie Bot", true),
            new BooleanSetting("Hypixel NPC", true)
    );

    public static EntityLivingBase target;

    private static final Minecraft mc = Minecraft.getMinecraft();

    public TargetManager() {
        super("Target", Category.COMBAT, "");
        addSettings(targetType, botDetectionModes);
    }

    public static boolean checkEntity(Entity entity) {
        if (entity instanceof EntityPlayer) {
            if (isBot(entity) && !targetType.isEnabled("Bots")) {
                return false;
            }
            if (!isBot(entity) && !targetType.isEnabled("Players")) {
                return false;
            }
        }

        if (entity.getClass().getPackage().getName().contains("monster") && !targetType.isEnabled("Mobs")) return false;
        if (entity.getClass().getPackage().getName().contains("passive") && !targetType.isEnabled("Animals")) return false;
        return !entity.isInvisible() || targetType.isEnabled("Invisibles");
    }

    public static boolean isBot(Entity entity) {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }

        EntityPlayer player = (EntityPlayer) entity;

        if (player == mc.thePlayer) {
            return false;
        }

        if (botDetectionModes.isEnabled("Broken ID") && player.getEntityId() > 1000000) {
            return true;
        }

        if (botDetectionModes.isEnabled("No Tablist") && !isInTablist(player)) {
            return true;
        }

        if (botDetectionModes.isEnabled("Newbie Bot") && player.ticksExisted <= 80) {
            return true;
        }

        if (botDetectionModes.isEnabled("Hypixel NPC") && isHypixelNPC(player)) {
            return true;
        }

        return false;
    }

    public static boolean isInTablist(EntityLivingBase entity) {
        if (mc.getNetHandler() == null) {
            return false;
        }

        NetHandlerPlayClient nhpc = mc.getNetHandler();
        Collection<NetworkPlayerInfo> playerInfoMap = nhpc.getPlayerInfoMap();

        if (playerInfoMap == null) {
            return false;
        }

        for (NetworkPlayerInfo playerInfo : playerInfoMap) {
            if (playerInfo != null && playerInfo.getGameProfile() != null
                    && playerInfo.getGameProfile().getName().equals(entity.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHypixelNPC(Entity entity) {
        String formattedName = entity.getDisplayName().getFormattedText();

        if (!formattedName.startsWith("\u00a7") && formattedName.endsWith(EnumChatFormatting.RESET.toString())) {
            return true;
        }

        if (formattedName.contains("[NPC]")) {
            return true;
        }
        return false;
    }
}