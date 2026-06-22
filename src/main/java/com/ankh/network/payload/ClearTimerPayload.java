package com.ankh.network.payload;

import com.ankh.AnkhResurrection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ClearTimerPayload() implements CustomPayload {

    public static final CustomPayload.Id<ClearTimerPayload> ID =
            new CustomPayload.Id<>(AnkhResurrection.id("clear_timer"));

    public static final PacketCodec<RegistryByteBuf, ClearTimerPayload> CODEC =
            PacketCodec.unit(new ClearTimerPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
