package com.ankh.event;

import com.ankh.AnkhResurrection;
import com.ankh.network.ModNetworking;
import com.ankh.session.ResurrectionManager;
import com.ankh.session.ResurrectionSession;
import com.ankh.session.ResurrectionState;
import com.ankh.session.StoredPlayerData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class DeathHandler {

    private DeathHandler() {}

    public static void register() {

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                onPlayerDeath(player, source);
            }
            return true;
        });
    }

    private static void onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        AnkhResurrection.LOGGER.info("[Ankh] Death handler fired for {} (source: {}).",
                player.getName().getString(), source.getName());

        if (source.isOf(DamageTypes.OUT_OF_WORLD)) {
            AnkhResurrection.LOGGER.info("[Ankh] Void death for {} -> vanilla behavior only.",
                    player.getName().getString());
            return;
        }

        ResurrectionState state = ResurrectionState.get(server);
        if (state.has(player.getUuid())) {

            AnkhResurrection.LOGGER.info("[Ankh] {} died with an active session -> vanilla death, no new guardian.",
                    player.getName().getString());
            return;
        }

        boolean singleplayer = server.isSingleplayer();
        BlockPos deathPos = player.getBlockPos();
        String dimId = player.getWorld().getRegistryKey().getValue().toString();

        StoredPlayerData snapshot = StoredPlayerData.capture(player);
        player.getInventory().clear();

        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.totalExperience = 0;

        ResurrectionSession session = new ResurrectionSession(
                player.getUuid(), singleplayer, dimId, deathPos, snapshot);
        state.put(session);

        ResurrectionManager.spawnZombie(server, session, player.getName().getString());

        ModNetworking.sendTimer(player, session.getSecondsRemaining(), singleplayer);

        AnkhResurrection.LOGGER.info("[Ankh] Resurrection session started for {} ({}).",
                player.getName().getString(), singleplayer ? "singleplayer" : "multiplayer");
    }
}
