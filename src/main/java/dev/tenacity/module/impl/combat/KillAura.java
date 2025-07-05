package dev.tenacity.module.impl.combat;

import dev.tenacity.Tenacity;
import dev.tenacity.commands.impl.FriendCommand;
import dev.tenacity.event.impl.player.*;
import dev.tenacity.event.impl.render.Render3DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.api.TargetManager;
import dev.tenacity.module.impl.movement.Scaffold;
import dev.tenacity.module.impl.render.HUDMod;
import dev.tenacity.module.settings.impl.*;
import dev.tenacity.utils.animations.Animation;
import dev.tenacity.utils.animations.Direction;
import dev.tenacity.utils.animations.impl.DecelerateAnimation;
import dev.tenacity.utils.misc.MathUtils;
import dev.tenacity.utils.player.InventoryUtils;
import dev.tenacity.utils.player.RotationUtils;
import dev.tenacity.utils.render.RenderUtil;
import dev.tenacity.utils.server.PacketUtils;
import dev.tenacity.utils.time.TimerUtil;
import dev.tenacity.viamcp.utils.AttackOrder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class KillAura extends Module {

    public static boolean attacking;
    public static boolean blocking;
    public static boolean wasBlocking;
    private float yaw = 0;
    private int cps;
    public static final List<EntityLivingBase> targets = new ArrayList<>();
    private final TimerUtil attackTimer = new TimerUtil();
    private final TimerUtil switchTimer = new TimerUtil();
    private int currentTargetIndex = 0;

    private final ModeSetting mode = new ModeSetting("Mode", "Single", "Single", "Switch", "Multi");

    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", 50, 500, 1, 1);
    private final NumberSetting maxTargetAmount = new NumberSetting("Max Target Amount", 3, 50, 2, 1);

    private final NumberSetting minCPS = new NumberSetting("Min CPS", 10, 20, 1, 1);
    private final NumberSetting maxCPS = new NumberSetting("Max CPS", 10, 20, 1, 1);
    private final NumberSetting reach = new NumberSetting("Reach", 4, 6, 3, 0.1);

    private final BooleanSetting autoblock = new BooleanSetting("Autoblock", false);

    private final ModeSetting autoblockMode = new ModeSetting("Autoblock Mode", "Watchdog", "Fake", "Verus", "Watchdog");

    private final BooleanSetting rotations = new BooleanSetting("Rotations", true);
    private final ModeSetting rotationMode = new ModeSetting("Rotation Mode", "Vanilla", "Vanilla", "Smooth");

    private final ModeSetting sortMode = new ModeSetting("Sort Mode", "Range", "Range", "Hurt Time", "Health", "Armor");

    private final MultipleBoolSetting addons = new MultipleBoolSetting("Addons",
            new BooleanSetting("Keep Sprint", true),
            new BooleanSetting("Through Walls", true),
            new BooleanSetting("Allow Scaffold", false),
            new BooleanSetting("Movement Fix", false),
            new BooleanSetting("Ray Cast", false));

    private final MultipleBoolSetting auraESP = new MultipleBoolSetting("Target ESP",
            new BooleanSetting("Circle", true),
            new BooleanSetting("Tracer", false),
            new BooleanSetting("Box", false),
            new BooleanSetting("Custom Color", false));
    private final ColorSetting customColor = new ColorSetting("Custom Color", Color.WHITE);
    private EntityLivingBase auraESPTarget;

    public KillAura() {
        super("KillAura", Category.COMBAT, "Automatically attacks players");
        autoblockMode.addParent(autoblock, a -> autoblock.isEnabled());
        rotationMode.addParent(rotations, r -> rotations.isEnabled());
        switchDelay.addParent(mode, m -> mode.is("Switch"));
        maxTargetAmount.addParent(mode, m -> mode.is("Multi"));
        customColor.addParent(auraESP, r -> r.isEnabled("Custom Color"));
        this.addSettings(mode, maxTargetAmount, switchDelay, minCPS, maxCPS, reach, autoblock, autoblockMode,
                rotations, rotationMode, sortMode, addons, auraESP, customColor);
    }

    @Override
    public void onDisable() {
        TargetManager.target = null;
        targets.clear();
        blocking = false;
        attacking = false;
        if (wasBlocking) {
            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }
        wasBlocking = false;
        currentTargetIndex = 0;
        super.onDisable();
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        this.setSuffix(mode.getMode());

        if (minCPS.getValue() > maxCPS.getValue()) {
            minCPS.setValue(minCPS.getValue() - 1);
        }

        sortTargets();

        if (event.isPre()) {
            attacking = !targets.isEmpty() && (addons.getSetting("Allow Scaffold").isEnabled() || !Tenacity.INSTANCE.isEnabled(Scaffold.class));
            blocking = autoblock.isEnabled() && attacking && InventoryUtils.isHoldingSword();

            if (attacking) {
                if (mode.is("Switch")) {
                    if (switchTimer.hasTimeElapsed(switchDelay.getValue().longValue(), true)) {
                        currentTargetIndex++;
                        if (currentTargetIndex >= targets.size()) {
                            currentTargetIndex = 0;
                        }
                    }
                    if (!targets.isEmpty() && currentTargetIndex < targets.size()) {
                        TargetManager.target = targets.get(currentTargetIndex);
                    } else {
                        TargetManager.target = null;
                        currentTargetIndex = 0;
                    }
                } else {
                    if (!targets.isEmpty()) {
                        TargetManager.target = targets.get(0);
                    } else {
                        TargetManager.target = null;
                    }
                }


                if (TargetManager.target != null) {
                    if (rotations.isEnabled()) {
                        float[] rotations = new float[]{0, 0};
                        switch (rotationMode.getMode()) {
                            case "Vanilla":
                                rotations = RotationUtils.getRotationsNeeded(TargetManager.target);
                                break;
                            case "Smooth":
                                rotations = RotationUtils.getSmoothRotations(TargetManager.target);
                                break;
                        }
                        yaw = event.getYaw();
                        event.setRotations(rotations[0], rotations[1]);
                        RotationUtils.setVisualRotations(event.getYaw(), event.getPitch());
                    }

                    if (addons.getSetting("Ray Cast").isEnabled() && !RotationUtils.isMouseOver(event.getYaw(), event.getPitch(), TargetManager.target, reach.getValue().floatValue())) {
                        return;
                    }

                    if (attackTimer.hasTimeElapsed(cps, true)) {
                        final int currentMinCPS = minCPS.getValue().intValue();
                        final int currentMaxCPS = maxCPS.getValue().intValue();

                        int minDelayTicks = (int) (20.0 / Math.max(1, currentMaxCPS));
                        int maxDelayTicks = (int) (20.0 / Math.max(1, currentMinCPS));

                        if (minDelayTicks > maxDelayTicks) {
                            int temp = minDelayTicks;
                            minDelayTicks = maxDelayTicks;
                            maxDelayTicks = temp;
                        }

                        int calculatedMinCPS = Math.max(1, currentMinCPS);
                        int calculatedMaxCPS = Math.max(1, currentMaxCPS);

                        cps = MathUtils.getRandomInRange(1000 / calculatedMaxCPS, 1000 / calculatedMinCPS);


                        if (mode.is("Multi")) {
                            for (int i = 0; i < Math.min(targets.size(), maxTargetAmount.getValue().intValue()); i++) {
                                EntityLivingBase entityLivingBase = targets.get(i);
                                AttackEvent attackEvent = new AttackEvent(entityLivingBase);
                                Tenacity.INSTANCE.getEventProtocol().handleEvent(attackEvent);

                                if (!attackEvent.isCancelled()) {
                                    AttackOrder.sendFixedAttack(mc.thePlayer, entityLivingBase);
                                }
                            }
                        } else {
                            AttackEvent attackEvent = new AttackEvent(TargetManager.target);
                            Tenacity.INSTANCE.getEventProtocol().handleEvent(attackEvent);

                            if (!attackEvent.isCancelled()) {
                                AttackOrder.sendFixedAttack(mc.thePlayer, TargetManager.target);
                            }
                        }
                    }
                }
            } else {
                TargetManager.target = null;
                switchTimer.reset();
                currentTargetIndex = 0;
            }
        }

        if (blocking) {
            switch (autoblockMode.getMode()) {
                case "Watchdog":
                    if (event.isPre() && wasBlocking && mc.thePlayer.ticksExisted % 4 == 0) {
                        PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        wasBlocking = false;
                    }

                    if (event.isPost() && !wasBlocking) {
                        PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mc.thePlayer.getHeldItem(), 255, 255, 255));
                        wasBlocking = true;
                    }
                    break;
                case "Verus":
                    if (event.isPre()) {
                        if (wasBlocking) {
                            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.
                                    Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                        }
                        if (mc.thePlayer.getHeldItem() != null) {
                            PacketUtils.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                            wasBlocking = true;
                        }
                    }
                    break;
                case "Fake":
                    break;
            }
        } else if (wasBlocking && autoblockMode.is("Watchdog") && event.isPre()) {
            PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.
                    Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
            wasBlocking = false;
        }
    }

    private void sortTargets() {
        targets.clear();
        if (mc.theWorld == null) return;

        for (Entity entity : mc.theWorld.getLoadedEntityList()) {
            if (entity instanceof EntityLivingBase && mc.thePlayer != null && mc.thePlayer != entity) {
                EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
                if (entityLivingBase != null && mc.thePlayer.getDistanceToEntity(entity) <= reach.getValue() && isValid(entity) && !FriendCommand.isFriend(entityLivingBase.getName())) {
                    targets.add(entityLivingBase);
                }
            }
        }
        if (!targets.isEmpty()) {
            switch (sortMode.getMode()) {
                case "Range":
                    targets.sort(Comparator.comparingDouble(mc.thePlayer::getDistanceToEntity));
                    break;
                case "Hurt Time":
                    targets.sort(Comparator.comparingInt(EntityLivingBase::getHurtTime));
                    break;
                case "Health":
                    targets.sort(Comparator.comparingDouble(EntityLivingBase::getHealth));
                    break;
                case "Armor":
                    targets.sort(Comparator.comparingInt(EntityLivingBase::getTotalArmorValue));
                    break;
            }
        }
    }

    public boolean isValid(Entity entity) {
        if (mc.thePlayer == null || entity == null) return false;
        if (!addons.isEnabled("Through Walls") && !mc.thePlayer.canEntityBeSeen(entity)) return false;
        else return TargetManager.checkEntity(entity);
    }

    @Override
    public void onPlayerMoveUpdateEvent(PlayerMoveUpdateEvent event) {
        if (addons.getSetting("Movement Fix").isEnabled() && TargetManager.target != null) {
            event.setYaw(yaw);
        }
    }

    @Override
    public void onJumpFixEvent(JumpFixEvent event) {
        if (addons.getSetting("Movement Fix").isEnabled() && TargetManager.target != null) {
            event.setYaw(yaw);
        }
    }

    @Override
    public void onKeepSprintEvent(KeepSprintEvent event) {
        if (addons.getSetting("Keep Sprint").isEnabled()) {
            event.cancel();
        }
    }

    private final Animation auraESPAnim = new DecelerateAnimation(300, 1);

    @Override
    public void onRender3DEvent(Render3DEvent event) {
        auraESPAnim.setDirection(TargetManager.target != null ? Direction.FORWARDS : Direction.BACKWARDS);
        if (TargetManager.target != null) {
            auraESPTarget = TargetManager.target;
        }

        if (auraESPAnim.finished(Direction.BACKWARDS)) {
            auraESPTarget = null;
        }

        Color color = HUDMod.getClientColors().getFirst();

        if (auraESP.isEnabled("Custom Color")) {
            color = customColor.getColor();
        }

        if (auraESPTarget != null) {
            if (auraESP.getSetting("Box").isEnabled()) {
                RenderUtil.renderBoundingBox(auraESPTarget, color, auraESPAnim.getOutput().floatValue());
            }
            if (auraESP.getSetting("Circle").isEnabled()) {
                RenderUtil.drawCircle(auraESPTarget, event.getTicks(), .75f, color.getRGB(), auraESPAnim.getOutput().floatValue());
            }

            if (auraESP.getSetting("Tracer").isEnabled()) {
                RenderUtil.drawTracerLine(auraESPTarget, 4f, Color.BLACK, auraESPAnim.getOutput().floatValue());
                RenderUtil.drawTracerLine(auraESPTarget, 2.5f, color, auraESPAnim.getOutput().floatValue());
            }
        }
    }
}