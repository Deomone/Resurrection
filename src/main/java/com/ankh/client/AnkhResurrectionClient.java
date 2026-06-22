package com.ankh.client;

import com.ankh.AnkhResurrection;
import com.ankh.client.gui.DeathScreenCustomizer;
import com.ankh.client.gui.ResurrectionHud;
import com.ankh.client.network.ClientNetworking;
import com.ankh.client.render.CorpseRenderer;
import com.ankh.client.render.ResurrectionZombieRenderer;
import com.ankh.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class AnkhResurrectionClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AnkhResurrection.LOGGER.info("[Ankh Resurrection] Initializing client...");

        EntityRendererRegistry.register(ModEntities.RESURRECTION_ZOMBIE, ResurrectionZombieRenderer::new);
        EntityRendererRegistry.register(ModEntities.CORPSE, CorpseRenderer::new);
        EntityRendererRegistry.register(ModEntities.LAMP, com.ankh.client.render.LampEntityRenderer::new);

        ResurrectionHud.register();
        ClientNetworking.register();
        DeathScreenCustomizer.register();
        com.ankh.client.gui.GameMenuCustomizer.register();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientResurrectionState.reset());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientResurrectionState.reset());

        AnkhResurrection.LOGGER.info("[Ankh Resurrection] Client initialization complete.");
    }
}
