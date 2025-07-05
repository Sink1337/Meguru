package dev.tenacity.module.impl.combat;

import dev.tenacity.Tenacity;
import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.player.MoveEvent;
import dev.tenacity.event.impl.render.Render3DEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.api.TargetManager;
import dev.tenacity.module.impl.movement.Flight;
import dev.tenacity.module.impl.movement.Speed;
import dev.tenacity.module.impl.movement.TerrainSpeed;
import dev.tenacity.module.settings.ParentAttribute;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.module.settings.impl.ColorSetting;
import dev.tenacity.module.settings.impl.MultipleBoolSetting;
import dev.tenacity.module.settings.impl.NumberSetting;
import dev.tenacity.utils.animations.Direction;
import dev.tenacity.utils.animations.impl.DecelerateAnimation;
import dev.tenacity.utils.player.MovementUtils;
import dev.tenacity.utils.player.RotationUtils;
import dev.tenacity.utils.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public final class TargetStrafe extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final MultipleBoolSetting adaptiveSettings = new MultipleBoolSetting("Adaptive",
            new BooleanSetting("Edges", false),
            new BooleanSetting("Behind", false),
            new BooleanSetting("Liquids", false),
            new BooleanSetting("Controllable", true)
    );
    public static final NumberSetting radius = new NumberSetting("Radius", 2, 8, 0.5, 0.1);
    private static final NumberSetting points = new NumberSetting("Points", 12, 60, 3, 1);
    private final BooleanSetting manualMode = new BooleanSetting("Manual Mode", false);
    public static final BooleanSetting space = new BooleanSetting("Require space key", true);
    public static final BooleanSetting auto3rdPerson = new BooleanSetting("Auto 3rd Person", false);
    private final BooleanSetting render = new BooleanSetting("Render", true);
    private final ColorSetting color = new ColorSetting("Color", new Color(-16711712));

    private final NumberSetting voidDetectionRadius = new NumberSetting("Void Check Radius", 4, 10, 2, 1);
    private final NumberSetting voidMinMotion = new NumberSetting("Void Min Motion", 0.05, 0.3, 0.01, 0.01);
    private final NumberSetting voidGroundReqMultiplier = new NumberSetting("Void Ground Req Multiplier", 1.5, 3.0, 0.1, 0.1);
    private final NumberSetting voidMinGroundReq = new NumberSetting("Void Min Ground Req", 4, 10, 1, 1);


    public float strafeYaw;
    private boolean leftDirection;
    private boolean isColliding;
    public boolean active;
    public EntityLivingBase currentTarget;

    private final DecelerateAnimation animation = new DecelerateAnimation(250, radius.getValue(), Direction.FORWARDS);
    private boolean returnState;

    public TargetStrafe() {
        super("TargetStrafe", Category.COMBAT, "strafe around targets");
        addSettings(adaptiveSettings, radius, points, manualMode, space, auto3rdPerson, render, color,
                voidDetectionRadius, voidMinMotion, voidGroundReqMultiplier, voidMinGroundReq);
        color.addParent(render, ParentAttribute.BOOLEAN_CONDITION);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        leftDirection = false;
        isColliding = false;
        active = false;
        animation.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (auto3rdPerson.isEnabled() && mc.gameSettings.thirdPersonView != 0 && returnState) {
            mc.gameSettings.thirdPersonView = 0;
            returnState = false;
        }
        active = false;
        currentTarget = null;
        animation.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        KillAura killAura = Tenacity.INSTANCE.getModuleCollection().getModule(KillAura.class);

        if (killAura != null) {
            currentTarget = TargetManager.target;
        }

        if (!shouldBeActive()) {
            active = false;
            currentTarget = null;
            if (auto3rdPerson.isEnabled() && mc.gameSettings.thirdPersonView != 0 && returnState) {
                mc.gameSettings.thirdPersonView = 0;
                returnState = false;
            }
            return;
        }

        if (auto3rdPerson.isEnabled() && mc.gameSettings.thirdPersonView == 0) {
            mc.gameSettings.thirdPersonView = 1;
            returnState = true;
        }

        if (mc.thePlayer.isCollidedHorizontally || !MovementUtils.isBlockUnder(5, false)) {
            if (!isColliding) {
                leftDirection = !leftDirection;
            }
            isColliding = true;
        } else {
            isColliding = false;
        }

        if (adaptiveSettings.getSetting("Controllable").isEnabled()) {
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) {
                leftDirection = true;
            }
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
                leftDirection = false;
            }
        }

        if (adaptiveSettings.getSetting("Edges").isEnabled() && isPlayerInVoidDangerForStrafe()) {
            leftDirection = !leftDirection;
        }
        if (adaptiveSettings.getSetting("Liquids").isEnabled() && isInLiquid()) {
            leftDirection = !leftDirection;
        }

        active = true;

        float targetYaw;

        if (adaptiveSettings.getSetting("Behind").isEnabled()) {
            targetYaw = currentTarget.rotationYaw + 180;
        } else {
            targetYaw = RotationUtils.getYaw(currentTarget.getPositionVector()) + (90 + 45) * (leftDirection ? -1 : 1);
        }

        final double strafeRange = radius.getValue() + Math.random() / 100f;
        final double posX = -MathHelper.sin((float) Math.toRadians(targetYaw)) * strafeRange + currentTarget.posX;
        final double posZ = MathHelper.cos((float) Math.toRadians(targetYaw)) * strafeRange + currentTarget.posZ;

        strafeYaw = RotationUtils.getYaw(new Vec3(posX, currentTarget.posY, posZ));
    }

    @Override
    public void onMoveEvent(MoveEvent e) {
        if (active && currentTarget != null) {
            TerrainSpeed terrainSpeed = Tenacity.INSTANCE.getModuleCollection().getModule(TerrainSpeed.class);

            if (terrainSpeed != null && terrainSpeed.isEnabled()) {
                double bloxdSpeed = terrainSpeed.getBloxdSpeed();
                TerrainSpeed.MutableVec3d moveDir = terrainSpeed.getBloxdMoveVec(e.getStrafe(), e.getForward(), bloxdSpeed, strafeYaw);
                e.setX(moveDir.getX());
                e.setZ(moveDir.getZ());
            } else {
                MovementUtils.setMoveEventSpeed(e, MovementUtils.getSpeed(), strafeYaw);

                if (mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.onGround) {
                    e.setY(0.42F);
                }
            }
        }
    }

    @Override
    public void onRender3DEvent(Render3DEvent event) {
        if (render.isEnabled()) {
            if (animation.getEndPoint() != radius.getValue()) animation.setEndPoint(radius.getValue());
            boolean canStrafe = active;
            animation.setDirection(canStrafe ? Direction.FORWARDS : Direction.BACKWARDS);

            if (canStrafe || !animation.isDone()) {
                drawCircle(5, 0xFF000000);
                drawCircle(3, color.getColor().getRGB());
            }
        }
    }

    public boolean shouldBeActive() {
        if (TargetManager.target == null) return false;

        KillAura killAura = Tenacity.INSTANCE.getModuleCollection().getModule(KillAura.class);
        if (killAura == null || !killAura.isEnabled() || !killAura.isValid(TargetManager.target)) {
            return false;
        }

        if (space.isEnabled() && !Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            return false;
        }

        Speed speed = Tenacity.INSTANCE.getModuleCollection().getModule(Speed.class);
        Flight flight = Tenacity.INSTANCE.getModuleCollection().getModule(Flight.class);

        if (!manualMode.isEnabled()) {
            if (!mc.gameSettings.keyBindForward.isKeyDown() || (!((speed != null && speed.isEnabled()) || (flight != null && flight.isEnabled())))) {
                return false;
            }
        } else {
            if (!mc.gameSettings.keyBindForward.isKeyDown()) {
                return false;
            }
        }

        return true;
    }

    private void drawCircle(float lineWidth, int color) {
        EntityLivingBase entity = currentTarget;
        if (entity == null) return;

        glPushMatrix();
        RenderUtil.color(color, (float) ((animation.getOutput().floatValue() / radius.getValue()) / 2F));
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glLineWidth(lineWidth);
        glEnable(GL_BLEND);
        glEnable(GL_LINE_SMOOTH);

        glBegin(GL_LINE_STRIP);
        float partialTicks = mc.timer.elapsedPartialTicks;
        double rad = animation.getOutput();
        double d = (Math.PI * 2.0) / points.getValue();

        double posX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double posY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double posZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        double renderPosX = mc.getRenderManager().renderPosX;
        double renderPosY = mc.getRenderManager().renderPosY;
        double renderPosZ = mc.getRenderManager().renderPosZ;

        double y = posY - renderPosY;
        for (double i = 0; i < Math.PI * 2.0; i += d) {
            double x = posX + StrictMath.sin(i) * rad - renderPosX;
            double z = posZ + StrictMath.cos(i) * rad - renderPosZ;
            glVertex3d(x, y, z);
        }
        double x = posX + StrictMath.sin(0) * rad - renderPosX;
        double z = posZ + StrictMath.cos(0) * rad - renderPosZ;
        glVertex3d(x, y, z);
        glEnd();

        glDisable(GL_BLEND);
        glDisable(GL_LINE_SMOOTH);
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
        glPopMatrix();
    }

    private boolean isMotionTowardsVoid(int offsetX, int offsetZ, double minMotion) {
        boolean motionXMatches = false;
        if (offsetX > 0) {
            motionXMatches = mc.thePlayer.motionX >= minMotion;
        } else if (offsetX < 0) {
            motionXMatches = mc.thePlayer.motionX <= -minMotion;
        } else {
            motionXMatches = true;
        }

        boolean motionZMatches = false;
        if (offsetZ > 0) {
            motionZMatches = mc.thePlayer.motionZ >= minMotion;
        } else if (offsetZ < 0) {
            motionZMatches = mc.thePlayer.motionZ <= -minMotion;
        } else {
            motionZMatches = true;
        }

        return motionXMatches && motionZMatches;
    }

    private boolean isPlayerInVoidDangerForStrafe() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;

        if (mc.thePlayer.onGround || mc.thePlayer.motionY > 0.0 || mc.thePlayer.isCollidedHorizontally) {
            return false;
        }

        if (mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.capabilities.isFlying) {
            return false;
        }

        int maxRadius = voidDetectionRadius.getValue().intValue();
        double minMotion = voidMinMotion.getValue();
        double minGroundReq = voidMinGroundReq.getValue();
        double groundReqMultiplier = voidGroundReqMultiplier.getValue();

        for (int x = -maxRadius; x <= maxRadius; x++) {
            for (int z = -maxRadius; z <= maxRadius; z++) {
                double horizontalDistance = Math.sqrt(x * x + z * z);
                int requiredGroundDistance = (int) Math.ceil(minGroundReq + horizontalDistance * groundReqMultiplier);
                requiredGroundDistance = Math.max(requiredGroundDistance, (int)minGroundReq);

                double currentGroundDistance = MovementUtils.distanceToGround(mc.thePlayer, mc.thePlayer.posX + x, mc.thePlayer.posZ + z);

                if (currentGroundDistance != -1 && currentGroundDistance > requiredGroundDistance) {
                    if (isMotionTowardsVoid(x, z, minMotion)) {
                        return true;
                    }
                } else if (currentGroundDistance == -1 && mc.thePlayer.motionY < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInLiquid() {
        if (currentTarget == null) return false;

        if (mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)).getBlock() instanceof BlockLiquid ||
                mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.1, mc.thePlayer.posZ)).getBlock() instanceof BlockLiquid) {
            return true;
        }

        double yawToTarget = RotationUtils.getYaw(currentTarget.getPositionVector());
        double xValue = -Math.sin(Math.toRadians(yawToTarget)) * (radius.getValue() / 2.0);
        double zValue = Math.cos(Math.toRadians(yawToTarget)) * (radius.getValue() / 2.0);
        BlockPos b = new BlockPos(mc.thePlayer.posX + xValue, mc.thePlayer.posY, mc.thePlayer.posZ + zValue);
        return mc.theWorld.getBlockState(b).getBlock() instanceof BlockLiquid;
    }
}