package dev.meguru.utils.player;

import dev.meguru.event.impl.player.MoveEvent;
import dev.meguru.event.impl.player.PlayerMoveUpdateEvent;
import dev.meguru.utils.Utils;
import dev.meguru.utils.server.PacketUtils;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

public class MovementUtils implements Utils {

    public static boolean isMoving() {
        if (mc.thePlayer == null) {
            return false;
        }
        return (mc.thePlayer.movementInput.moveForward != 0F || mc.thePlayer.movementInput.moveStrafe != 0F);
    }

    public static float getMoveYaw(float yaw) {
        Vector2f from = new Vector2f((float) mc.thePlayer.lastTickPosX, (float) mc.thePlayer.lastTickPosZ),
                to = new Vector2f((float) mc.thePlayer.posX, (float) mc.thePlayer.posZ),
                diff = new Vector2f(to.x - from.x, to.y - from.y);

        double x = diff.x, z = diff.y;
        if (x != 0 && z != 0) {
            yaw = (float) Math.toDegrees((Math.atan2(-x, z) + MathHelper.PI2) % MathHelper.PI2);
        }
        return yaw;
    }

    public static void setSpeed(double moveSpeed, float yaw, double strafe, double forward) {
        if (forward != 0.0D) {
            if (strafe > 0.0D) {
                yaw += ((forward > 0.0D) ? -45 : 45);
            } else if (strafe < 0.0D) {
                yaw += ((forward > 0.0D) ? 45 : -45);
            }
            strafe = 0.0D;
            if (forward > 0.0D) {
                forward = 1.0D;
            } else if (forward < 0.0D) {
                forward = -1.0D;
            }
        }
        if (strafe > 0.0D) {
            strafe = 1.0D;
        } else if (strafe < 0.0D) {
            strafe = -1.0D;
        }
        double mx = Math.cos(Math.toRadians((yaw + 90.0F)));
        double mz = Math.sin(Math.toRadians((yaw + 90.0F)));
        mc.thePlayer.motionX = forward * moveSpeed * mx + strafe * moveSpeed * mz;
        mc.thePlayer.motionZ = forward * moveSpeed * mz - strafe * moveSpeed * mx;
    }

    public static void setSpeedHypixel(PlayerMoveUpdateEvent event, float moveSpeed, float strafeMotion) {
        float remainder = 1F - strafeMotion;
        if (mc.thePlayer.onGround) {
            setSpeed(moveSpeed);
        } else {
            mc.thePlayer.motionX *= strafeMotion;
            mc.thePlayer.motionZ *= strafeMotion;
            event.setFriction(moveSpeed * remainder);
        }
    }

    public static void setSpeed(double moveSpeed) {
        setSpeed(moveSpeed, mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveStrafe, mc.thePlayer.movementInput.moveForward);
    }

    public static void setSpeed(MoveEvent moveEvent, double moveSpeed, float yaw, double strafe, double forward) {
        if (forward != 0.0D) {
            if (strafe > 0.0D) {
                yaw += ((forward > 0.0D) ? -45 : 45);
            } else if (strafe < 0.0D) {
                yaw += ((forward > 0.0D) ? 45 : -45);
            }
            strafe = 0.0D;
            if (forward > 0.0D) {
                forward = 1.0D;
            } else if (forward < 0.0D) {
                forward = -1.0D;
            }
        }
        if (strafe > 0.0D) {
            strafe = 1.0D;
        } else if (strafe < 0.0D) {
            strafe = -1.0D;
        }
        double mx = Math.cos(Math.toRadians((yaw + 90.0F)));
        double mz = Math.sin(Math.toRadians((yaw + 90.0F)));
        moveEvent.setX(forward * moveSpeed * mx + strafe * moveSpeed * mz);
        moveEvent.setZ(forward * moveSpeed * mz - strafe * moveSpeed * mx);
    }

    public static void setSpeed(MoveEvent moveEvent, double moveSpeed) {
        setSpeed(moveEvent, moveSpeed, mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveStrafe, mc.thePlayer.movementInput.moveForward);
    }

    public static void setMoveEventSpeed(MoveEvent moveEvent, double speed, float yaw) {
        double forward = 1.0;
        double strafe = 0.0;

        double mx = Math.cos(Math.toRadians((yaw + 90.0F)));
        double mz = Math.sin(Math.toRadians((yaw + 90.0F)));

        moveEvent.setX(forward * speed * mx + strafe * speed * mz);
        moveEvent.setZ(forward * speed * mz - strafe * speed * mx);
    }

    private static double getDirectionRadians() {
        float yaw = mc.thePlayer.rotationYaw;
        float strafeAngle = 45.0f;

        if (mc.thePlayer.movementInput.moveForward < 0.0f) {
            strafeAngle = -45.0f;
            yaw += 180.0f;
        }

        if (mc.thePlayer.movementInput.moveStrafe > 0.0f) {
            yaw -= strafeAngle;
            if (mc.thePlayer.movementInput.moveForward == 0.0f) {
                yaw -= 45.0f;
            }
        } else if (mc.thePlayer.movementInput.moveStrafe < 0.0f) {
            yaw += strafeAngle;
            if (mc.thePlayer.movementInput.moveForward == 0.0f) {
                yaw += 45.0f;
            }
        }
        return Math.toRadians(yaw);
    }

    public static void strafe() {
        strafe(getSpeed());
    }

    public static void strafe(double speed) {
        if(isMoving()) {
            mc.thePlayer.motionZ = Math.cos(getDirectionRadians()) * speed;
            mc.thePlayer.motionX = -Math.sin(getDirectionRadians()) * speed;
        }
    }

    public static void strafe(double speed, boolean moving) {
        if(!moving || isMoving()) {
            mc.thePlayer.motionZ = Math.cos(getDirectionRadians()) * speed;
            mc.thePlayer.motionX = -Math.sin(getDirectionRadians()) * speed;
        }
    }

    public static void applyFriction() {
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed) ? MovementUtils.getSpeed() < 0.367 : MovementUtils.getSpeed() < 0.255) {
            strafe(mc.thePlayer.isPotionActive(Potion.moveSpeed) ? 0.367 : 0.255);
        }
    }


    public static double getBaseMoveSpeed() {
        double baseSpeed = mc.thePlayer.capabilities.getWalkSpeed() * 2.873;
        if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
            baseSpeed /= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSlowdown).getAmplifier() + 1);
        }
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            baseSpeed *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        return baseSpeed;
    }

    public static void sendFlyingCapabilities(final boolean isFlying, final boolean allowFlying) {
        final PlayerCapabilities playerCapabilities = new PlayerCapabilities();
        playerCapabilities.isFlying = isFlying;
        playerCapabilities.allowFlying = allowFlying;
        PacketUtils.sendPacketNoEvent(new C13PacketPlayerAbilities(playerCapabilities));
    }

    public static double getBaseMoveSpeed2() {
        double baseSpeed = mc.thePlayer.capabilities.getWalkSpeed() * (mc.thePlayer.isSprinting() ? 2.873 : 2.215);
        if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
            baseSpeed /= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSlowdown).getAmplifier() + 1);
        }
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            baseSpeed *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        return baseSpeed;
    }

    public static double getBaseMoveSpeedStupid() {
        double sped = 0.2873;
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            sped *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        return sped;
    }

    public static boolean isOnGround(double height) {
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0, -height, 0)).isEmpty();
    }

    public static boolean isBlockUnder(int offset, boolean checkThroughBlocks) {
        return !mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0, -offset, 0.0)).isEmpty();
    }

    public static float getSpeed() {
        if (mc.thePlayer == null || mc.theWorld == null) return 0;
        return (float) Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }

    public static boolean isPlaceable(BlockPos blockPos) {
        return BlockUtils.replaceable(blockPos) || BlockUtils.isFluid(BlockUtils.getBlockAtPos(blockPos));
    }

    public static double distanceToGround(Entity entity, double x, double z) {
        if (mc.theWorld == null) return -1;
        if (entity != null && entity.onGround) {
            return 0;
        }

        double startY = entity != null ? entity.posY : mc.thePlayer.posY;
        for (int i = (int) Math.floor(startY); i >= -1; i--) {
            if (isPlaceable(new BlockPos(x, i, z))) {
                return startY - (i + 1);
            }
        }
        return -1;
    }

    public static boolean overVoid() {
        for (int i = (int) mc.thePlayer.posY; i > -1; i--) {
            if (!(mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, i, mc.thePlayer.posZ)).getBlock() instanceof BlockAir)) {
                return false;
            }
        }
        return true;
    }

    public static float getMaxFallDist() {
        return mc.thePlayer.getMaxFallHeight() + (mc.thePlayer.isPotionActive(Potion.jump) ? mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier() + 1 : 0);
    }
}