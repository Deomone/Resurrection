package com.ankh.event;

import com.ankh.AnkhResurrection;
import com.ankh.network.ModNetworking;
import com.ankh.session.ResurrectionManager;
import com.ankh.session.ResurrectionSession;
import com.ankh.session.ResurrectionState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class ServerTickHandler {

    private ServerTickHandler() {}

    private static final int RESYNC_INTERVAL = 20;

    private static final double OBSERVER_RADIUS = 10.0;

    private static int tickCounter = 0;

    private static final class DelayedTask {
        int ticksLeft;
        final Runnable action;
        DelayedTask(int t, Runnable a) { this.ticksLeft = t; this.action = a; }
    }
    private static final List<DelayedTask> DELAYED = new ArrayList<>();

    public static void schedule(int delayTicks, Runnable action) {
        synchronized (DELAYED) { DELAYED.add(new DelayedTask(Math.max(1, delayTicks), action)); }
    }

    private static void runDelayed() {
        synchronized (DELAYED) {
            java.util.Iterator<DelayedTask> it = DELAYED.iterator();
            while (it.hasNext()) {
                DelayedTask t = it.next();
                if (--t.ticksLeft <= 0) {
                    try {
                        t.action.run();
                    } catch (Exception e) {
                        AnkhResurrection.LOGGER.error("[Ankh] delayed task failed", e);
                    }
                    it.remove();
                }
            }
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ServerTickHandler::onEndTick);
    }

    private static void onEndTick(MinecraftServer server) {
        runDelayed();

        ResurrectionState state = ResurrectionState.get(server);
        if (state.all().isEmpty()) {
            return;
        }

        tickCounter++;
        boolean resync = (tickCounter % RESYNC_INTERVAL) == 0;

        List<ResurrectionSession> snapshot = new ArrayList<>(state.all());
        for (ResurrectionSession session : snapshot) {
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(session.getOwner());
            boolean ownerInDeathWorld = owner != null && isInDeathDimension(owner, session);

            if (ownerInDeathWorld) {
                boolean expired = session.tickAndCheckExpired();
                if (expired) {
                    handleTimeout(server, session);
                    continue;
                }
            }

            if (owner == null) {
                continue;
            }

            if (ownerInDeathWorld) {
                if (resync) {
                    ModNetworking.sendTimer(owner, session.getSecondsRemaining(), session.isSingleplayer());
                }

                if (!session.isSingleplayer() && session.isWaiting()) {
                    clampObserver(server, session, owner);
                }
            } else if (resync) {

                ModNetworking.clearTimer(owner);
            }
        }

        state.markDirty();
    }

    private static boolean isInDeathDimension(ServerPlayerEntity owner, ResurrectionSession session) {
        return owner.getWorld().getRegistryKey().getValue().toString().equals(session.getDimensionId());
    }

    private static void handleTimeout(MinecraftServer server, ResurrectionSession session) {

        ResurrectionManager.cleanup(server, session);
        AnkhResurrection.LOGGER.info("[Ankh] Session for {} timed out -> cleaned up.",
                session.getOwner());
    }

    private static void clampObserver(MinecraftServer server, ResurrectionSession session,
                                      ServerPlayerEntity owner) {
        BlockPos death = session.getGroundedPos();
        Vec3d center = new Vec3d(death.getX() + 0.5, death.getY(), death.getZ() + 0.5);
        Vec3d pos = owner.getPos();
        double dist = pos.distanceTo(center);
        if (dist <= OBSERVER_RADIUS) {
            return;
        }

        Vec3d dir = pos.subtract(center);
        Vec3d target = dir.lengthSquared() < 1.0e-4
                ? center
                : center.add(dir.normalize().multiply(OBSERVER_RADIUS - 1.0));
        owner.networkHandler.requestTeleport(target.x, target.y, target.z, owner.getYaw(), owner.getPitch());
        owner.setVelocity(Vec3d.ZERO);
        owner.velocityModified = true;
    }
}
