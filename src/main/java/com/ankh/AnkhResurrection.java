package com.ankh;

import com.ankh.event.DeathHandler;
import com.ankh.event.LootInjection;
import com.ankh.event.RespawnHandler;
import com.ankh.event.ServerTickHandler;
import com.ankh.network.ModNetworking;
import com.ankh.registry.ModEntities;
import com.ankh.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnkhResurrection implements ModInitializer {

    public static final String MOD_ID = "ankh";
    public static final Logger LOGGER = LoggerFactory.getLogger("Ankh Resurrection");

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("[Ankh Resurrection] Initializing common...");

        ModItems.register();
        ModEntities.register();

        ModNetworking.registerCommon();

        DeathHandler.register();
        RespawnHandler.register();
        ServerTickHandler.register();
        LootInjection.register();
        com.ankh.command.AnkhCommand.register();

        LOGGER.info("[Ankh Resurrection] Common initialization complete.");
    }
}
