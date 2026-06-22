package com.ankh.command;

import com.ankh.AnkhResurrection;
import com.ankh.config.AnkhConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class AnkhCommand {

    private AnkhCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
    }

    private static void build(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("ankh")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload")
                                .executes(ctx -> {
                                    AnkhConfig cfg = AnkhConfig.reload();
                                    ctx.getSource().sendFeedback(() -> Text.literal(
                                            "[Ankh] Config reloaded. session=" + cfg.sessionMinutes
                                                    + "m, soft/hard leash=" + cfg.guardianSoftLeashRadius
                                                    + "/" + cfg.guardianHardLeashRadius
                                                    + ", observer=" + cfg.observerRadius
                                                    + ", absorption=" + cfg.resurrectionAbsorptionSeconds + "s"
                                                    + " (loot chances need a restart).")
                                            .formatted(Formatting.GREEN), true);
                                    AnkhResurrection.LOGGER.info("[Ankh] Config reloaded via /ankh reload by {}.",
                                            ctx.getSource().getName());
                                    return 1;
                                })));
    }
}
