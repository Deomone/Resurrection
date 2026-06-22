package com.ankh.network;

import com.ankh.network.payload.AnkhVfxPayload;
import com.ankh.network.payload.ClearTimerPayload;
import com.ankh.network.payload.SingleplayerActionPayload;
import com.ankh.network.payload.TimerPayload;
import com.ankh.network.payload.WaitForResurrectionPayload;
import com.ankh.session.ResurrectionManager;
import com.ankh.session.ResurrectionSession;
import com.ankh.session.ResurrectionState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

public final class ModNetworking {

    private ModNetworking() {}

    public static void registerCommon() {

        PayloadTypeRegistry.playS2C().register(TimerPayload.ID, TimerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClearTimerPayload.ID, ClearTimerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnkhVfxPayload.ID, AnkhVfxPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(WaitForResurrectionPayload.ID, WaitForResurrectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SingleplayerActionPayload.ID, SingleplayerActionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(WaitForResurrectionPayload.ID, (payload, context) ->
                context.server().execute(() -> handleWait(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(SingleplayerActionPayload.ID, (payload, context) ->
                context.server().execute(() -> handleSingleplayerAction(context.player(), payload.action())));
    }

    private static void handleWait(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ResurrectionState state = ResurrectionState.get(server);
        ResurrectionSession session = state.get(player.getUuid());
        if (session == null || session.isSingleplayer()) return;

        session.setWaiting(true);
        session.setPreviousGameMode(player.interactionManager.getGameMode().getName());
        state.markDirty();

        ResurrectionManager.prepareObserverSpawn(server, session, player);

        server.getPlayerManager().respawnPlayer(player, false, net.minecraft.entity.Entity.RemovalReason.DISCARDED);
    }

    private static void handleSingleplayerAction(ServerPlayerEntity player, int action) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ResurrectionState state = ResurrectionState.get(server);
        ResurrectionSession session = state.get(player.getUuid());
        if (session == null) return;

        ResurrectionManager.cleanup(server, session);

    }

    public static void sendTimer(ServerPlayerEntity player, int secondsRemaining, boolean singleplayer) {
        ServerPlayNetworking.send(player, new TimerPayload(secondsRemaining, singleplayer));
    }

    public static void clearTimer(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new ClearTimerPayload());
    }

    public static void sendAnkhResurrectVfx(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new AnkhVfxPayload());
    }
}
