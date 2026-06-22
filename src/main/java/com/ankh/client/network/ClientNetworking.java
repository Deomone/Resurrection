package com.ankh.client.network;

import com.ankh.client.ClientResurrectionState;
import com.ankh.network.payload.AnkhVfxPayload;
import com.ankh.network.payload.ClearTimerPayload;
import com.ankh.network.payload.TimerPayload;
import com.ankh.registry.ModItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class ClientNetworking {

    private ClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TimerPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    MinecraftClient client = context.client();
                    boolean wasActive = ClientResurrectionState.isActive();
                    ClientResurrectionState.setTimer(payload.secondsRemaining(), payload.singleplayer());

                    if (!wasActive && client.currentScreen instanceof DeathScreen) {
                        client.setScreen(client.currentScreen);
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(ClearTimerPayload.ID, (payload, context) ->
                context.client().execute(ClientResurrectionState::clear));

        ClientPlayNetworking.registerGlobalReceiver(AnkhVfxPayload.ID, (payload, context) ->
                context.client().execute(() -> playAnkhVfx(context.client())));
    }

    private static void playAnkhVfx(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        ClientResurrectionState.armAnkhVfx();

        client.gameRenderer.showFloatingItem(new ItemStack(ModItems.ANKH));

        for (int i = 0; i < 30; i++) {
            double vx = client.world.random.nextGaussian() * 0.4;
            double vy = client.world.random.nextGaussian() * 0.4;
            double vz = client.world.random.nextGaussian() * 0.4;
            client.world.addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + player.getStandingEyeHeight() * 0.5, player.getZ(),
                    vx, vy, vz);
        }

        client.world.playSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f, false);
    }
}
