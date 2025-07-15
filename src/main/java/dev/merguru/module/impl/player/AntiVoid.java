package dev.merguru.module.impl.player;

import dev.merguru.Merguru;
import dev.merguru.event.impl.game.TickEvent;
import dev.merguru.event.impl.network.PacketReceiveEvent;
import dev.merguru.event.impl.network.PacketSendEvent;
import dev.merguru.module.Category;
import dev.merguru.module.Module;
import dev.merguru.module.impl.exploit.Disabler;
import dev.merguru.module.impl.movement.LongJump;
import dev.merguru.module.settings.impl.ModeSetting;
import dev.merguru.module.settings.impl.NumberSetting;
import dev.merguru.utils.server.PacketUtils;
import dev.merguru.utils.time.TimerUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;

public class AntiVoid extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Bloxd", "Bloxd", "BlocksMC");
    private final NumberSetting fallDist = new NumberSetting("Fall Distance", 3, 20, 1, 0.5);
    private final TimerUtil timer = new TimerUtil();
    private boolean reset;
    private double lastGroundY;
    private double lastGroundX;
    private double lastGroundZ;

    private int gameStartDelayTicks;
    private static final int REQUIRED_GAME_START_TICKS = 100;
    private boolean gameInProgress = false;

    private final List<Packet> packets = new ArrayList<>();

    private boolean b1;

    public AntiVoid() {
        super("AntiVoid", Category.PLAYER, "saves you from the void");
        this.addSettings(mode, fallDist);
        this.gameInProgress = false;
        this.gameStartDelayTicks = 0;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.gameInProgress = false;
        this.gameStartDelayTicks = 0;
        packets.clear();
        this.b1 = false;
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat chatPacket = (S02PacketChat) event.getPacket();
            if (chatPacket.getChatComponent() == null) return;

            String message = chatPacket.getChatComponent().getUnformattedText();
            if (message == null) return;

            if (message.contains("Starting game.")) {
                this.gameStartDelayTicks = 0;
                this.gameInProgress = false;
            }
            else if (message.contains("Click here to play again!") ||
                    message.contains("Default starter kit selected. Open the shop to select a different kit!")) {
                this.gameInProgress = false;
                this.gameStartDelayTicks = 0;
                packets.clear();
            }
        }
    }

    @Override
    public void onTickEvent(TickEvent event) {
        if (gameInProgress) {
            return;
        }

        if (gameStartDelayTicks >= 0 && gameStartDelayTicks < REQUIRED_GAME_START_TICKS) {
            gameStartDelayTicks++;
            if (gameStartDelayTicks >= REQUIRED_GAME_START_TICKS) {
                gameInProgress = true;
                gameStartDelayTicks = -1;
            }
        }

        if (mode.is("BlocksMC")) {
            if (mc.thePlayer == null || mc.theWorld == null) {
                return;
            }

            if (mc.thePlayer.onGround) {
                this.lastGroundX = mc.thePlayer.posX;
                this.lastGroundY = mc.thePlayer.posY;
                this.lastGroundZ = mc.thePlayer.posZ;
            }
            if (b1) {
                mc.thePlayer.motionY = -0.09800000190734863;
                this.lastGroundY = mc.thePlayer.posY;
                this.lastGroundX = mc.thePlayer.posX;
                this.lastGroundZ = mc.thePlayer.posZ;
                b1 = false;
            }
        }
    }

    @Override
    public void onPacketSendEvent(PacketSendEvent event) {
        if (!gameInProgress) {
            return;
        }

        Disabler disabler = Merguru.INSTANCE.getModuleCollection().getModule(Disabler.class);
        if (disabler != null && disabler.isEnabled() && disabler.disablers.getSetting("Bloxd").isEnabled()) {
            if (disabler.flying || LongJump.isBloxdFlying) {
                return;
            }
        }

        if (mode.is("Bloxd")) {
            if (event.getPacket() instanceof C03PacketPlayer) {
                if (!isBlockUnder()) {
                    if (mc.thePlayer.fallDistance < fallDist.getValue()) {
                        event.cancel();
                        packets.add(event.getPacket());
                    } else {
                        if (!packets.isEmpty()) {
                            for (Packet packet : packets) {
                                final C03PacketPlayer c03 = (C03PacketPlayer) packet;
                                c03.setY(lastGroundY);
                                c03.setX(lastGroundX);
                                c03.setZ(lastGroundZ);
                                PacketUtils.sendPacketNoEvent(packet);
                            }
                            packets.clear();
                        }
                    }
                } else {
                    lastGroundY = mc.thePlayer.posY;
                    lastGroundX = mc.thePlayer.posX;
                    lastGroundZ = mc.thePlayer.posZ;
                    if (!packets.isEmpty()) {
                        packets.forEach(PacketUtils::sendPacketNoEvent);
                        packets.clear();
                    }
                }
            }
        } else if (mode.is("BlocksMC")) {
            if (event.getPacket() instanceof C03PacketPlayer) {
                if (!isBlockUnder() && mc.thePlayer.fallDistance > fallDist.getValue()) {
                    mc.thePlayer.motionY = -0.09800000190734863;
                    b1 = true;
                    event.cancel();
                }
            }
        }
    }

    private boolean isBlockUnder() {
        if (mc.thePlayer.posY < 0) return false;
        for (int offset = 0; offset < (int) mc.thePlayer.posY + 2; offset += 2) {
            AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(0, -offset, 0);
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                return true;
            }
        }
        return false;
    }

}