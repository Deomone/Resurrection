package com.ankh.entity.ai;

import com.ankh.entity.ResurrectionZombieEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public class ReturnToAnchorGoal extends Goal {

    private final ResurrectionZombieEntity zombie;

    public ReturnToAnchorGoal(ResurrectionZombieEntity zombie) {
        this.zombie = zombie;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    private double distanceToAnchor() {
        BlockPos a = zombie.getAnchor();
        return Math.sqrt(zombie.squaredDistanceTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5));
    }

    private boolean hasAggro() {
        return zombie.getTarget() != null && zombie.getTarget().isAlive();
    }

    @Override
    public boolean canStart() {
        double dist = distanceToAnchor();
        if (hasAggro()) {
            return dist > ResurrectionZombieEntity.maxRadius();
        }
        return dist > ResurrectionZombieEntity.leashRadius();
    }

    @Override
    public boolean shouldContinue() {

        return distanceToAnchor() > ResurrectionZombieEntity.leashRadius() * 0.6;
    }

    @Override
    public void start() {
        BlockPos a = zombie.getAnchor();

        if (hasAggro() && distanceToAnchor() > ResurrectionZombieEntity.maxRadius()) {
            Vec3d back = new Vec3d(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
            zombie.teleport(back.x, back.y, back.z, false);

        }
    }

    @Override
    public void tick() {
        BlockPos a = zombie.getAnchor();
        zombie.getNavigation().startMovingTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, 1.0);
    }
}
