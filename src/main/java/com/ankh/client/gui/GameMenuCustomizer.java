package com.ankh.client.gui;

import com.ankh.AnkhResurrection;
import com.ankh.client.ClientResurrectionState;
import com.ankh.network.payload.SingleplayerActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.List;

public final class GameMenuCustomizer {

    private GameMenuCustomizer() {}

    private static final String KEY_RETURN = "menu.returnToGame";
    private static final int ROW_GAP = 4;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof GameMenuScreen)) return;

            if (!ClientResurrectionState.isActive() || !ClientResurrectionState.isSingleplayer()) return;
            addEndButton(client, screen);
        });
    }

    private static void addEndButton(MinecraftClient client, Screen screen) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        ButtonWidget back = find(buttons, KEY_RETURN);
        if (back == null) {
            AnkhResurrection.LOGGER.warn("[Ankh] Could not find 'Back to Game' button; skipping End Resurrection button.");
            return;
        }

        int rowHeight = back.getHeight() + ROW_GAP;
        int backY = back.getY();

        for (ClickableWidget w : buttons) {
            if (w != back && w.getY() > backY) {
                w.setY(w.getY() + rowHeight);
            }
        }

        ButtonWidget end = ButtonWidget.builder(
                        Text.translatable("button.ankh.end_resurrection"),
                        b -> {
                            ClientPlayNetworking.send(new SingleplayerActionPayload(SingleplayerActionPayload.END));
                            client.setScreen(null);
                        })
                .dimensions(back.getX(), backY + rowHeight, back.getWidth(), back.getHeight())
                .build();
        buttons.add(end);
    }

    private static ButtonWidget find(List<ClickableWidget> buttons, String translationKey) {
        for (ClickableWidget widget : buttons) {
            if (widget instanceof ButtonWidget button
                    && button.getMessage().getContent() instanceof TranslatableTextContent tc
                    && tc.getKey().equals(translationKey)) {
                return button;
            }
        }
        return null;
    }
}
