package dev.tenacity.module.impl.movement;

import dev.tenacity.event.impl.player.MotionEvent;
import dev.tenacity.event.impl.player.MoveEvent;
import dev.tenacity.module.Category;
import dev.tenacity.module.Module;
import dev.tenacity.module.settings.impl.BooleanSetting;
import dev.tenacity.utils.player.MovementUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.MathHelper;

@SuppressWarnings("unused")
public final class BloxdPhysics extends Module {

    private static int groundTicksLocal;
    private static double lastMotionY;
    private static boolean wasClimbing;

    private static int jumpfunny = 0;
    private static long jumpticks = 0L;
    private long knockbackTime = 0;

    private final BooleanSetting spiderValue = new BooleanSetting("Spider", true);

    private final NoaPhysics bloxdPhysics = new NoaPhysics();

    public BloxdPhysics() {
        super("BloxdPhysics", Category.MOVEMENT, "Simulates Bloxd.io physics for movement.");
        this.addSettings(spiderValue);
    }

    @Override
    public void onEnable() {
        bloxdPhysics.reset();
        groundTicksLocal = 0;
        lastMotionY = 0;
        wasClimbing = false;
        jumpfunny = 0;
        jumpticks = 0L;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        bloxdPhysics.reset();
        mc.timer.timerSpeed = 1;
        super.onDisable();
    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        if (LongJump.isBloxdLongJumping) {
            return;
        }

        if (!e.isPre()) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        if (player.onGround) {
            groundTicksLocal++;
            if (groundTicksLocal > 5) jumpfunny = 0;
        } else {
            groundTicksLocal = 0;
        }

        if (player.isCollidedVertically && lastMotionY > 0 && player.motionY <= 0) {
            bloxdPhysics.getVelocityVector().setY(0.0);
            bloxdPhysics.getImpulseVector().setY(0.0);
        }

        lastMotionY = player.motionY;
    }

    @Override
    public void onMoveEvent(MoveEvent e) {
        if (LongJump.isBloxdLongJumping) {
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        if (player.onGround && bloxdPhysics.getVelocityVector().getY() < 0) {
            bloxdPhysics.getVelocityVector().set(0, 0, 0);
        }

        if (player.onGround && player.motionY > 0.4199 && player.motionY < 0.4201) {
            if (jumpfunny < 4) {
                jumpfunny++;
            }
            bloxdPhysics.getImpulseVector().add(0, 8, 0);
        }

        if (!player.onGround && player.motionY < 0) {
            bloxdPhysics.getForceVector().add(0, -10, 0);
        }

        if (spiderValue.isEnabled()) {
            boolean isMoving = MovementUtils.isMoving();
            if (player.isCollidedHorizontally && isMoving) {
                bloxdPhysics.getVelocityVector().set(0, 8, 0);
                wasClimbing = true;
            } else if (wasClimbing) {
                bloxdPhysics.getVelocityVector().set(0, 0, 0);
                wasClimbing = false;
            }
        }

        double speed = getBloxdSpeed();
        MutableVec3d moveDir = getBloxdMoveVec(e.getStrafe(), e.getForward(), speed);

        e.setX(moveDir.getX());
        e.setZ(moveDir.getZ());
        e.setY(bloxdPhysics.getMotionForTick().getY() / 30.0);
    }


    private double getBloxdSpeed() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return 0;

        if (System.currentTimeMillis() < jumpticks) return 1;
        if (player.isUsingItem()) return 0.10;

        double finalSpeed = 0.26;
        if (jumpfunny == 2) {
            finalSpeed += 0.025;
        } else if (jumpfunny == 3) {
            finalSpeed += 0.050;
        } else if (jumpfunny == 4) {
            finalSpeed += 0.075;
        }

        if (player.isUsingItem()) {
            finalSpeed = 0.10;
        }
        return finalSpeed;
    }

    private MutableVec3d getBloxdMoveVec(float strafeIn, float forwardIn, double speed) {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return new MutableVec3d(0.0D, 0.0D, 0.0D);

        float forward = forwardIn;
        float strafe = strafeIn;
        float yaw = player.rotationYaw;

        float sqrt = MathHelper.sqrt_float(forward * forward + strafe * strafe);
        if (sqrt < 0.01F) return new MutableVec3d(0.0D, 0.0D, 0.0D);

        if (sqrt > 1.0F) {
            forward /= sqrt;
            strafe /= sqrt;
        }

        double yawRad = Math.toRadians(yaw);
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);

        double x = (strafe * cosYaw - forward * sinYaw) * speed;
        double z = (forward * cosYaw + strafe * sinYaw) * speed;
        return new MutableVec3d(x, 0, z);
    }

    public static final class NoaPhysics {
        private MutableVec3d impulseVector = new MutableVec3d(0.0d, 0.0, 0.0);
        private MutableVec3d forceVector = new MutableVec3d(0.0, 0.0, 0.0);
        private MutableVec3d velocityVector = new MutableVec3d(0.0, 0.0, 0.0);
        private MutableVec3d gravityVector = new MutableVec3d(0.0, -10.0, 0.0);

        private double gravityMul = 2.0;

        private final double mass = 1.0;
        private final double delta = 1.0 / 30.0;

        public void reset() {
            impulseVector.set(0.0, 0.0, 0.0);
            forceVector.set(0.0, 0.0, 0.0);
            velocityVector.set(0.0, 0.0, 0.0);
        }

        public MutableVec3d getMotionForTick() {
            double massDiv = 1.0 / mass;
            forceVector.mul(massDiv);
            forceVector.add(gravityVector);
            forceVector.mul(gravityMul);
            impulseVector.mul(massDiv);
            forceVector.mul(delta);
            impulseVector.add(forceVector);
            velocityVector.add(impulseVector);
            forceVector.set(0.0, 0.0, 0.0);
            impulseVector.set(0.0, 0.0, 0.0);
            return velocityVector;
        }

        public MutableVec3d getImpulseVector() {
            return this.impulseVector;
        }

        public MutableVec3d getForceVector() {
            return this.forceVector;
        }

        public MutableVec3d getVelocityVector() {
            return this.velocityVector;
        }

        public MutableVec3d getGravityVector() {
            return this.gravityVector;
        }

        public double getGravityMul() {
            return this.gravityMul;
        }

        public double getMass() {
            return this.mass;
        }

        public double getDelta() {
            return this.delta;
        }
    }

    public static final class MutableVec3d {

        private double x, y, z;

        public MutableVec3d(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static MutableVec3dBuilder builder() {
            return new MutableVec3dBuilder();
        }

        public MutableVec3d setX(double x) {
            this.x = x;
            return this;
        }

        public MutableVec3d setY(double y) {
            this.y = y;
            return this;
        }

        public MutableVec3d setZ(double z) {
            this.z = z;
            return this;
        }

        public MutableVec3d set(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public void add(double x, double y, double z) {
            this.x += x;
            this.y += y;
            this.z += z;
        }

        public void mul(double factor) {
            this.x *= factor;
            this.y *= factor;
            this.z *= factor;
        }

        public void add(MutableVec3d v) {
            this.x += v.x;
            this.y += v.y;
            this.z += v.z;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public double getZ() {
            return this.z;
        }

        public static class MutableVec3dBuilder {
            private double x;
            private double y;
            private double z;

            MutableVec3dBuilder() {
            }

            public MutableVec3dBuilder x(double x) {
                this.x = x;
                return this;
            }

            public MutableVec3dBuilder y(double y) {
                this.y = y;
                return this;
            }

            public MutableVec3dBuilder z(double z) {
                this.z = z;
                return this;
            }

            public MutableVec3d build() {
                return new MutableVec3d(this.x, this.y, this.z);
            }

            @Override
            public String toString() {
                return "MutableVec3dBuilder(x=" + this.x + ", y=" + this.y + ", z=" + this.z + ")";
            }
        }
    }
}
