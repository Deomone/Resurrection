package com.ankh.client.gui;

import com.ankh.client.ClientResurrectionState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ResurrectionHud {

    private ResurrectionHud() {}

    private static final int COLOR = 0xFFFFD700;

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (!ClientResurrectionState.isActive()) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) {
                return;
            }

            if (client.currentScreen != null) {
                return;
            }

            render(client, drawContext);
        });
    }

    private static void render(MinecraftClient client, DrawContext drawContext) {
        int total = ClientResurrectionState.getSecondsRemaining();
        int minutes = total / 60;
        int seconds = total % 60;
        String time = String.format("%02d:%02d", minutes, seconds);
        Text label = Text.translatable("hud.ankh.time_remaining", time);

        TextRenderer tr = client.textRenderer;
        int screenWidth = drawContext.getScaledWindowWidth();
        int width = tr.getWidth(label);
        int x = (screenWidth - width) / 2;
        int y = 8;

        drawContext.drawTextWithShadow(tr, label, x, y, COLOR);
    }
}
