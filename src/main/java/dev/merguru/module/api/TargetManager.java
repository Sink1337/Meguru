package dev.merguru.module.api;

import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.settings.impl.BooleanSetting;
import dev.merguru.module.settings.impl.MultipleBoolSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumChatFormatting;

import java.util.Collection;

public class TargetManager extends Module {

    public static MultipleBoolSetting targetType = new MultipleBoolSetting(
            "Target Type",
            new BooleanSetting("Players", true),
            new BooleanSetting("Bots", false),
            new BooleanSetting("Mobs", false),
            new BooleanSetting("Animals", false),
            new BooleanSetting("Invisibles", true),
            new BooleanSetting("Teams", false)
    );

    public static MultipleBoolSetting teamDetectionModes = new MultipleBoolSetting(
            "Team Detection Modes",
            new BooleanSetting("Color", true),
            new BooleanSetting("Scoreboard", true),
            new BooleanSetting("Armor", false)
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
        addSettings(targetType, teamDetectionModes, botDetectionModes);
    }

    public static boolean checkEntity(Entity entity) {
        if (entity instanceof EntityPlayer) {
            if (entity == mc.thePlayer) {
                return false;
            }

            boolean isPlayerTeammate = isTeammate((EntityPlayer) entity);

            if (!targetType.isEnabled("Teams") && isPlayerTeammate) {
                return false;
            }

            if (!isPlayerTeammate && !targetType.isEnabled("Players")) {
                return false;
            }

            if (isBot(entity) && !targetType.isEnabled("Bots")) {
                return false;
            }
        } else {
            if (entity.getClass().getPackage().getName().contains("monster") && !targetType.isEnabled("Mobs")) return false;
            if (entity.getClass().getPackage().getName().contains("passive") && !targetType.isEnabled("Animals")) return false;
        }

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

    public static boolean isTeammate(EntityPlayer player) {
        if (mc.thePlayer == null || mc.theWorld == null || player == null) {
            return false;
        }

        if (teamDetectionModes.isEnabled("Scoreboard")) {
            if (isTeammateByScoreboard(player)) {
                return true;
            }
        }

        if (teamDetectionModes.isEnabled("Armor")) {
            if (isTeammateByArmor(player)) {
                return true;
            }
        }

        if (teamDetectionModes.isEnabled("Color")) {
            if (isTeammateByColor(player)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isTeammateByColor(EntityPlayer player) {
        String selfName = mc.thePlayer.getDisplayName().getFormattedText().replace("§r", "");
        String playerName = player.getDisplayName().getFormattedText().replace("§r", "");

        if (selfName.length() > 1 && playerName.length() > 1) {
            char selfColorCode = selfName.charAt(1);
            char playerColorCode = playerName.charAt(1);
            return selfColorCode == playerColorCode;
        }
        return false;
    }

    private static boolean isTeammateByScoreboard(EntityPlayer player) {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return false;
        }

        Team selfTeam = scoreboard.getPlayersTeam(mc.thePlayer.getName());
        Team playerTeam = scoreboard.getPlayersTeam(player.getName());

        return selfTeam != null && playerTeam != null && selfTeam.isSameTeam(playerTeam);
    }

    private static boolean isTeammateByArmor(EntityPlayer entityPlayer) {
        ItemStack myHead = mc.thePlayer.inventory.armorInventory[3];
        ItemStack entityHead = entityPlayer.inventory.armorInventory[3];

        if (myHead != null && entityHead != null &&
                myHead.getItem() instanceof ItemArmor &&
                entityHead.getItem() instanceof ItemArmor) {

            if (entityHead.getItem() instanceof ItemSkull) return false;

            ItemArmor myItemArmor = (ItemArmor) myHead.getItem();
            ItemArmor entityItemArmor = (ItemArmor) entityHead.getItem();

            return myItemArmor.getColor(myHead) == entityItemArmor.getColor(entityHead);
        }
        return false;
    }
}