package com.ankh.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public final class OwnerSkin {

    private OwnerSkin() {}

    public static Identifier resolve(UUID ownerUuid, String ownerName) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (ownerUuid != null && client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(ownerUuid);
            if (entry != null) {
                return entry.getSkinTextures().texture();
            }
        }

        UUID uuid = ownerUuid != null ? ownerUuid : Uuids.getOfflinePlayerUuid(
                ownerName == null || ownerName.isEmpty() ? "Steve" : ownerName);
        return DefaultSkinHelper.getSkinTextures(uuid).texture();
    }
}
