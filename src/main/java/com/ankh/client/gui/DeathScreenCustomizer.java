package com.ankh.client.gui;

import com.ankh.client.ClientResurrectionState;
import com.ankh.network.payload.SingleplayerActionPayload;
import com.ankh.network.payload.WaitForResurrectionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.List;

public final class DeathScreenCustomizer {

    private DeathScreenCustomizer() {}

    private static final int BUTTON_W = 200;
    private static final int BUTTON_H = 20;

    private static final String KEY_RESPAWN = "deathScreen.respawn";
    private static final String KEY_TITLE = "deathScreen.titleScreen";

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof DeathScreen)) {
                return;
            }
            if (!ClientResurrectionState.isActive()) {
                return;
            }
            if (ClientResurrectionState.isSingleplayer()) {
                customizeSingleplayer(client, screen);
            } else {
                customizeMultiplayer(client, screen);
            }
        });
    }

    private static void customizeMultiplayer(MinecraftClient client, Screen screen) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        ButtonWidget respawn = find(buttons, KEY_RESPAWN);
        int baseY = respawn != null ? respawn.getY() : screen.height / 4 + 72;
        int x = screen.width / 2 - 100;

        ButtonWidget wait = ButtonWidget.builder(
                        Text.translatable("button.ankh.wait_for_resurrection"),
                        b -> {
                            ClientPlayNetworking.send(new WaitForResurrectionPayload());
                            client.setScreen(null);
                        })
                .dimensions(x, baseY + 48, BUTTON_W, BUTTON_H)
                .build();
        buttons.add(wait);
    }

    private static void customizeSingleplayer(MinecraftClient client, Screen screen) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);

        ButtonWidget respawn = find(buttons, KEY_RESPAWN);
        ButtonWidget title = find(buttons, KEY_TITLE);

        int x = screen.width / 2 - 100;
        int baseY = respawn != null ? respawn.getY() : screen.height / 4 + 72;

        if (title != null) {
            buttons.remove(title);
        }

        ButtonWidget abandon = ButtonWidget.builder(
                        Text.translatable("button.ankh.abandon"),
                        b -> {
                            ClientPlayNetworking.send(new SingleplayerActionPayload(SingleplayerActionPayload.ABANDON));
                            if (client.player != null) {
                                client.player.requestRespawn();
                            }
                            client.setScreen(null);
                        })
                .dimensions(x, baseY + 24, BUTTON_W, BUTTON_H)
                .build();
        buttons.add(abandon);

        ButtonWidget customTitle = ButtonWidget.builder(
                        Text.translatable("deathScreen.titleScreen"),
                        b -> {
                            ClientPlayNetworking.send(new SingleplayerActionPayload(SingleplayerActionPayload.TITLE));
                            quitToTitle(client);
                        })
                .dimensions(x, baseY + 48, BUTTON_W, BUTTON_H)
                .build();
        buttons.add(customTitle);
    }

    private static void quitToTitle(MinecraftClient client) {
        if (client.world != null) {
            client.world.disconnect();
        }
        client.disconnect();
        client.setScreen(new TitleScreen());
    }

    private static ButtonWidget find(List<ClickableWidget> buttons, String translationKey) {
        for (ClickableWidget widget : buttons) {
            if (widget instanceof ButtonWidget button && hasKey(button, translationKey)) {
                return button;
            }
        }
        return null;
    }

    private static boolean hasKey(ButtonWidget button, String translationKey) {
        return button.getMessage().getContent() instanceof TranslatableTextContent tc
                && tc.getKey().equals(translationKey);
    }
}
