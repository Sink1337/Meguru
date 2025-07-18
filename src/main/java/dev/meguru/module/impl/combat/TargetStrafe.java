package dev.meguru.module.impl.combat;

import dev.meguru.Meguru;
import dev.meguru.event.impl.player.MotionEvent;
import dev.meguru.event.impl.player.MoveEvent;
import dev.meguru.event.impl.render.Render3DEvent;
import dev.meguru.module.Category;
import dev.meguru.module.Module;
import dev.meguru.module.api.TargetManager;
import dev.meguru.module.impl.exploit.Disabler;
import dev.meguru.module.impl.movement.Flight;
import dev.meguru.module.impl.movement.Speed;
import dev.meguru.module.settings.ParentAttribute;
import dev.meguru.module.settings.impl.*;
import dev.meguru.utils.animations.Direction;
import dev.meguru.utils.animations.impl.DecelerateAnimation;
import dev.meguru.utils.player.BloxdPhysicsUtils;
import dev.meguru.utils.player.MovementUtils;
import dev.meguru.utils.player.RotationUtils;
import dev.meguru.utils.render.RenderUtil;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

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
    private final ModeSetting rendermode = new ModeSetting("RenderMode", "Normal","Normal","Exhibition");
    private final ColorSetting color = new ColorSetting("Color", new Color(-16711712));

    public float strafeYaw;
    private boolean leftDirection;
    private boolean isColliding;
    public boolean active;
    public EntityLivingBase currentTarget;

    private final DecelerateAnimation animation = new DecelerateAnimation(250, radius.getValue(), Direction.FORWARDS);
    private boolean returnState;

    public TargetStrafe() {
        super("TargetStrafe", Category.COMBAT, "strafe around targets");
        addSettings(adaptiveSettings, radius, points, manualMode, space, auto3rdPerson, render,rendermode, color);
        rendermode.addParent(render, ParentAttribute.BOOLEAN_CONDITION);
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
        KillAura killAura = Meguru.INSTANCE.getModuleCollection().getModule(KillAura.class);

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

        if (adaptiveSettings.getSetting("Edges").isEnabled() && isVoid()) {
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
            Disabler disabler = Meguru.INSTANCE.getModuleCollection().getModule(Disabler.class);

            if (disabler != null && disabler.isEnabled() && disabler.disablers.getSetting("Bloxd").isEnabled()) {
                double bloxdSpeed = BloxdPhysicsUtils.getBloxdSpeed(mc.thePlayer, disabler);
                BloxdPhysicsUtils.MutableVec3d moveDir = BloxdPhysicsUtils.getBloxdMoveVec(e.getStrafe(), e.getForward(), bloxdSpeed, strafeYaw);

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
            if (currentTarget == null) return;
            if (animation.getEndPoint() != radius.getValue()) animation.setEndPoint(radius.getValue());
            boolean canStrafe = active;
            animation.setDirection(canStrafe ? Direction.FORWARDS : Direction.BACKWARDS);

            if (canStrafe || !animation.isDone()) {
                switch (rendermode.getMode()) {
                    case "Normal":
                        drawCircle(5, 0xFF000000);
                        drawCircle(3, color.getColor().getRGB());
                        break;
                    case "Exhibition":
                        GL11.glPushMatrix();
                        GL11.glTranslated(
                                currentTarget.lastTickPosX + (currentTarget.posX - currentTarget.lastTickPosX) * mc.timer.renderPartialTicks - RenderManager.renderPosX,
                                currentTarget.lastTickPosY + (currentTarget.posY - currentTarget.lastTickPosY) * mc.timer.renderPartialTicks - RenderManager.renderPosY,
                                currentTarget.lastTickPosZ + (currentTarget.posZ - currentTarget.lastTickPosZ) * mc.timer.renderPartialTicks - RenderManager.renderPosZ
                        );

                        GL11.glEnable(GL11.GL_BLEND);
                        GL11.glEnable(GL11.GL_LINE_SMOOTH);
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                        GL11.glDisable(GL11.GL_DEPTH_TEST);
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        GL11.glRotatef(90f, 1f, 0f, 0f);

                        GL11.glLineWidth(3 + 7.25f);
                        GL11.glColor3f(0f, 0f, 0f);
                        GL11.glBegin(GL11.GL_LINE_LOOP);
                        for (int i = 0; i <= 360; i += 30) {
                            GL11.glVertex2f(
                                    (float) (Math.cos(i * Math.PI / 180.0) * radius.getValue()),
                                    (float) (Math.sin(i * Math.PI / 180.0) * radius.getValue())
                            );
                        }
                        GL11.glEnd();

                        GL11.glLineWidth(3f);
                        GL11.glBegin(GL11.GL_LINE_LOOP);
                        for (int j = 0; j <= 360; j += 30) {
                            if (active) {
                                GL11.glColor4f(0.5f, 1f, 0.5f, 1f);
                            } else {
                                GL11.glColor4f(1f, 1f, 1f, 1f);
                            }
                            GL11.glVertex2f(
                                    (float) (Math.cos(j * Math.PI / 180.0) * radius.getValue()),
                                    (float) (Math.sin(j * Math.PI / 180.0) * radius.getValue())
                            );
                        }
                        GL11.glEnd();

                        GL11.glDisable(GL11.GL_BLEND);
                        GL11.glEnable(GL11.GL_TEXTURE_2D);
                        GL11.glEnable(GL11.GL_DEPTH_TEST);
                        GL11.glDisable(GL11.GL_LINE_SMOOTH);
                        GL11.glPopMatrix();
                        GlStateManager.resetColor();
                        GL11.glColor4f(1f, 1f, 1f, 1f);
                        break;
                }
            }
        }
    }

    public boolean shouldBeActive() {
        if (TargetManager.target == null) return false;

        KillAura killAura = Meguru.INSTANCE.getModuleCollection().getModule(KillAura.class);
        if (killAura == null || !killAura.isEnabled() || !killAura.isValid(TargetManager.target)) {
            return false;
        }

        if (space.isEnabled() && !Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            return false;
        }

        Speed speed = Meguru.INSTANCE.getModuleCollection().getModule(Speed.class);
        Flight flight = Meguru.INSTANCE.getModuleCollection().getModule(Flight.class);

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

        double renderPosX = RenderManager.renderPosX;
        double renderPosY = RenderManager.renderPosY;
        double renderPosZ = RenderManager.renderPosZ;

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

    private boolean isVoid() {
        if (mc.thePlayer.posY < 0) return true;

        int checkRadius = 1;

        for (int xOffset = -checkRadius; xOffset <= checkRadius; xOffset++) {
            for (int zOffset = -checkRadius; zOffset <= checkRadius; zOffset++) {
                int off = 0;
                while (off < mc.thePlayer.posY + 2) {
                    final AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(xOffset, -off, zOffset);
                    if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                        off += 2;
                        continue;
                    }
                    return false;
                }
            }
        }
        return true;
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