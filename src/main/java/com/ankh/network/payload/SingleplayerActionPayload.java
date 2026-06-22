package com.ankh.network.payload;

import com.ankh.AnkhResurrection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record SingleplayerActionPayload(int action) implements CustomPayload {

    public static final int ABANDON = 0;
    public static final int TITLE = 1;
    public static final int END = 2;

    public static final CustomPayload.Id<SingleplayerActionPayload> ID =
            new CustomPayload.Id<>(AnkhResurrection.id("sp_action"));

    public static final PacketCodec<RegistryByteBuf, SingleplayerActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SingleplayerActionPayload::action,
            SingleplayerActionPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
