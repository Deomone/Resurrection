package com.ankh.mixin;

import com.ankh.session.ResurrectionSession;
import com.ankh.session.ResurrectionState;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerMove", at = @At("TAIL"))
    private void ankh$confineObserver(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        ServerPlayerEntity p = this.player;
        if (p == null) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;

        ResurrectionState state = ResurrectionState.get(server);
        ResurrectionSession session = state.get(p.getUuid());
        if (session == null || session.isSingleplayer() || !session.isWaiting()) return;

        double radius = com.ankh.config.AnkhConfig.get().observerRadius;
        BlockPos g = session.getGroundedPos();
        Vec3d center = new Vec3d(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
        Vec3d pos = p.getPos();
        if (pos.distanceTo(center) <= radius) return;

        Vec3d dir = pos.subtract(center);
        Vec3d clamped = dir.lengthSquared() < 1.0e-4
                ? center
                : center.add(dir.normalize().multiply(radius - 0.5));

        p.networkHandler.requestTeleport(clamped.x, clamped.y, clamped.z, p.getYaw(), p.getPitch());
    }
}
