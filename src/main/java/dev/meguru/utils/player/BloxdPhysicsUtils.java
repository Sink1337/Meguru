package dev.meguru.utils.player;

import dev.meguru.module.impl.exploit.Disabler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;

public class BloxdPhysicsUtils {

    public static final class NoaPhysics {
        private MutableVec3d impulseVector = new MutableVec3d(0.0d, 0.0, 0.0);
        private MutableVec3d forceVector = new MutableVec3d(0.0, 0.0, 0.0);
        private MutableVec3d velocityVector = new MutableVec3d(0.0, 0.0, 0.0);
        private final MutableVec3d gravityVector = new MutableVec3d(0.0, -10.0, 0.0);

        private double gravityMul = 2.0;

        private final double mass = 1.0;
        private final double delta = 1.0 / 30.0;

        public void reset() {
            impulseVector.set(0.0, 0.0, 0.0);
            forceVector.set(0.0, 0.0, 0.0);
            velocityVector.set(0.0, 0.0, 0.0);
            gravityVector.set(0.0, -10.0, 0.0);
        }

        public MutableVec3d getMotionForTick() {
            return getMotionForTick(this.gravityMul, this.mass, this.delta);
        }

        public MutableVec3d getMotionForTick(double currentGravityMul, double currentMass, double currentDelta) {
            double massDiv = 1.0 / currentMass;
            forceVector.mul(massDiv);
            forceVector.add(gravityVector);
            forceVector.mul(currentGravityMul);
            impulseVector.mul(massDiv);
            forceVector.mul(currentDelta);
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

    private static final NoaPhysics bloxdPhysics = new NoaPhysics();

    public static double getBloxdSpeed(EntityPlayerSP player, Disabler disabler) {
        if (player == null) return 0;

        long currentDamageBoostDuration = disabler.bloxdDamageTime.getValue().longValue();
        long currentDamageFlightDuration = disabler.bloxdDamageFlightTime.getValue().longValue();

        if (System.currentTimeMillis() < Disabler.jumpTicks) return 1;
        if (player.isUsingItem()) return 0.08;

        double finalSpeed = 0.26;

        if (disabler.bloxdDamageFlight.isEnabled() && disabler.damageFlightStartTime != 0L) {
            if (System.currentTimeMillis() - disabler.damageFlightStartTime <= currentDamageFlightDuration) {
                return disabler.bloxdDamageFlightSpeed.getValue();
            } else {
                disabler.damageFlightStartTime = 0L;
            }
        }

        if (disabler.bloxdDamageBoost.isEnabled() && disabler.damageBoostStartTime != 0L) {
            if (System.currentTimeMillis() - disabler.damageBoostStartTime <= currentDamageBoostDuration) {
                finalSpeed = disabler.bloxdDamageSpeed.getValue();
            } else {
                disabler.damageBoostStartTime = 0L;
            }
        }

        if (Disabler.jumpFunny >= 2 && Disabler.jumpFunny <= 4) {
            finalSpeed += 0.025 * (Disabler.jumpFunny - 1);
        }
        return finalSpeed;
    }

    public static MutableVec3d getBloxdMoveVec(float strafeIn, float forwardIn, double speed, float customYaw) {
        float forward = forwardIn;
        float strafe = strafeIn;
        float yaw = customYaw;

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

    public static NoaPhysics getBloxdPhysics() {
        return bloxdPhysics;
    }

    public static void resetBloxdPhysicsState() {
        bloxdPhysics.reset();
    }
}