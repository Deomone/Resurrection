package com.ankh.event;

import com.ankh.AnkhResurrection;
import com.ankh.network.ModNetworking;
import com.ankh.registry.ModItems;
import com.ankh.session.ResurrectionManager;
import com.ankh.session.ResurrectionSession;
import com.ankh.session.ResurrectionState;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class RespawnHandler {

    private RespawnHandler() {}

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {

                return;
            }
            onPostDeathRespawn(newPlayer);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                onDisconnect(handler.player, server));
    }

    private static void onPostDeathRespawn(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        ResurrectionState state = ResurrectionState.get(server);
        ResurrectionSession session = state.get(player.getUuid());
        if (session == null) {
            return;
        }

        if (session.isSingleplayer()) {
            handleSingleplayerRespawn(server, state, session, player);
        } else {
            handleMultiplayerRespawn(server, session, player);
        }
    }

    private static void handleSingleplayerRespawn(MinecraftServer server, ResurrectionState state,
                                                  ResurrectionSession session, ServerPlayerEntity player) {

        if (!session.isAnkhReturned()) {
            int ankhCount = session.getStoredData().extractItemAndCount(ModItems.ANKH_ID.toString());
            session.setPreservedAnkhCount(ankhCount);
            session.setAnkhReturned(true);

            int remaining = ankhCount;
            int maxPerStack = new ItemStack(ModItems.ANKH).getMaxCount();
            while (remaining > 0) {
                int stackSize = Math.min(remaining, maxPerStack);
                ItemStack ankhStack = new ItemStack(ModItems.ANKH, stackSize);
                if (!player.getInventory().insertStack(ankhStack)) {
                    player.dropItem(ankhStack, false);
                }
                remaining -= stackSize;
            }
            state.markDirty();
            AnkhResurrection.LOGGER.info("[Ankh] SP respawn: returned {} Ankh(s) to {}.",
                    ankhCount, player.getName().getString());
        }

        ModNetworking.sendTimer(player, session.getSecondsRemaining(), true);

        BlockPos death = session.getDeathPos();
        player.sendMessage(
                Text.translatable("chat.ankh.death_coords", death.getX(), death.getY(), death.getZ())
                        .formatted(Formatting.GOLD),
                false);
    }

    private static void handleMultiplayerRespawn(MinecraftServer server,
                                                 ResurrectionSession session, ServerPlayerEntity player) {
        if (session.isPendingResurrect()) {

            ResurrectionManager.finalizeResurrect(server, session, player);
            AnkhResurrection.LOGGER.info("[Ankh] {} resurrected onto the corpse.",
                    player.getName().getString());
        } else if (session.isWaiting()) {

            ResurrectionManager.enterWaitingState(server, session, player);
            ModNetworking.sendTimer(player, session.getSecondsRemaining(), false);
            AnkhResurrection.LOGGER.info("[Ankh] {} is now waiting for resurrection.",
                    player.getName().getString());
        } else {

            ResurrectionManager.cleanup(server, session);
            AnkhResurrection.LOGGER.info("[Ankh] {} respawned manually -> session cleaned up, items lost.",
                    player.getName().getString());
        }
    }

    private static void onDisconnect(ServerPlayerEntity player, MinecraftServer server) {
        if (player == null || server == null) return;

        ResurrectionState state = ResurrectionState.get(server);
        ResurrectionSession session = state.get(player.getUuid());
        if (session == null) return;

        if (session.isSingleplayer()) {

            return;
        }

        ResurrectionManager.cleanup(server, session);
        AnkhResurrection.LOGGER.info("[Ankh] {} disconnected -> session cleaned up, items lost.",
                player.getName().getString());
    }
}
