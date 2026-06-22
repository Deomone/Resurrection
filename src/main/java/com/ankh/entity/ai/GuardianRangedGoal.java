package com.ankh.entity.ai;

import com.ankh.entity.ResurrectionZombieEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.BowItem;
import net.minecraft.util.Hand;

import java.util.EnumSet;

public class GuardianRangedGoal extends Goal {

    private static final double SHOOT_MIN_SQ = 36.0;
    private static final double CONTINUE_SQ = 25.0;
    private static final double RETREAT_SQ = 81.0;
    private static final double APPROACH_SQ = 169.0;
    private static final double MIN_STRAFE_DIST = 1.4;

    private final ResurrectionZombieEntity mob;
    private final int attackInterval;
    private int cooldown = -1;
    private int seeTicks;
    private int combatTicks;
    private int nextSwitch = 15;
    private boolean movingLeft;
    private boolean backward;
    private double lastX, lastZ;
    private double distSinceSwitch;
    private int stuckTicks;

    public GuardianRangedGoal(ResurrectionZombieEntity mob, int attackInterval) {
        this.mob = mob;
        this.attackInterval = attackInterval;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity t = this.mob.getTarget();
        return t != null && t.isAlive() && this.mob.hasRangedWeapon()
                && this.mob.squaredDistanceTo(t) >= SHOOT_MIN_SQ;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity t = this.mob.getTarget();
        if (t == null || !t.isAlive() || !this.mob.hasRangedWeapon()) return false;
        return this.mob.squaredDistanceTo(t) >= CONTINUE_SQ || this.mob.isUsingItem();
    }

    @Override
    public void start() {
        super.start();
        this.mob.setAttacking(true);
        this.mob.drawRangedWeapon();
        this.lastX = this.mob.getX();
        this.lastZ = this.mob.getZ();
        this.distSinceSwitch = 0.0;
        this.stuckTicks = 0;
    }

    @Override
    public void stop() {
        this.mob.setAttacking(false);
        this.mob.clearActiveItem();
        this.mob.sheatheRangedWeapon();
        this.mob.getNavigation().stop();
        this.seeTicks = 0;
        this.cooldown = -1;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity t = this.mob.getTarget();
        if (t == null) return;

        boolean canSee = this.mob.getVisibilityCache().canSee(t);
        this.seeTicks = canSee ? Math.min(this.seeTicks + 1, 20) : Math.max(this.seeTicks - 1, -20);

        double dSq = this.mob.squaredDistanceTo(t);
        boolean targetInLeash = this.mob.isWithinLeash(t);

        if (dSq < RETREAT_SQ) this.backward = true;
        else if (dSq > APPROACH_SQ) this.backward = false;

        double moved = Math.hypot(this.mob.getX() - this.lastX, this.mob.getZ() - this.lastZ);
        this.lastX = this.mob.getX();
        this.lastZ = this.mob.getZ();
        this.distSinceSwitch += moved;
        if (moved < 0.02) this.stuckTicks++; else this.stuckTicks = 0;

        boolean forceFlip = this.stuckTicks >= 4;

        boolean timedFlip = this.combatTicks >= this.nextSwitch && this.distSinceSwitch >= MIN_STRAFE_DIST;
        if (forceFlip || timedFlip) {
            this.movingLeft = !this.movingLeft;
            this.combatTicks = 0;
            this.distSinceSwitch = 0.0;
            this.stuckTicks = 0;
            this.nextSwitch = 8 + this.mob.getRandom().nextInt(15);
        }
        this.combatTicks++;

        if ((this.mob.horizontalCollision || forceFlip) && this.mob.isOnGround()) {
            this.mob.getJumpControl().setActive();
        }

        float strafeSpeed = this.mob.isUsingItem() ? 0.3f : 0.5f;

        float forward;
        if (this.backward) forward = -0.5f;
        else if (targetInLeash && dSq > APPROACH_SQ) forward = 0.45f;
        else forward = 0.0f;
        float sideways = this.movingLeft ? strafeSpeed : -strafeSpeed;

        if (!this.mob.isWithinLeash(this.mob.getX(), this.mob.getZ())) {
            forward = 0.0f;
            sideways = 0.0f;
        }

        if (this.mob.isUsingItem()) {
            forward = 0.0f;
            sideways = 0.0f;
            faceTargetBody(t);
        }

        this.mob.getLookControl().lookAt(t, 30.0f, 30.0f);
        this.mob.getMoveControl().strafeTo(forward, sideways);

        if (this.mob.isUsingItem()) {
            if (!canSee && this.seeTicks < -12) {
                this.mob.clearActiveItem();
            } else if (canSee) {
                int useTime = this.mob.getItemUseTime();
                if (useTime >= 20) {
                    this.mob.clearActiveItem();
                    this.mob.shootAt(t, BowItem.getPullProgress(useTime));
                    this.cooldown = this.attackInterval;
                }
            }
        } else if (--this.cooldown <= 0 && this.seeTicks >= -12) {
            this.mob.setCurrentHand(Hand.MAIN_HAND);
        }
    }

    private void faceTargetBody(LivingEntity t) {
        double dx = t.getX() - this.mob.getX();
        double dz = t.getZ() - this.mob.getZ();
        float desired = (float) (net.minecraft.util.math.MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float stepped = approachDegrees(this.mob.bodyYaw, desired, 20.0f);
        this.mob.bodyYaw = stepped;
        this.mob.setYaw(stepped);
    }

    private static float approachDegrees(float from, float to, float maxStep) {
        float diff = net.minecraft.util.math.MathHelper.wrapDegrees(to - from);
        diff = net.minecraft.util.math.MathHelper.clamp(diff, -maxStep, maxStep);
        return from + diff;
    }
}
