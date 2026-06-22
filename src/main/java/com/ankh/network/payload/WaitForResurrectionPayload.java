package com.ankh.network.payload;

import com.ankh.AnkhResurrection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record WaitForResurrectionPayload() implements CustomPayload {

    public static final CustomPayload.Id<WaitForResurrectionPayload> ID =
            new CustomPayload.Id<>(AnkhResurrection.id("wait"));

    public static final PacketCodec<RegistryByteBuf, WaitForResurrectionPayload> CODEC =
            PacketCodec.unit(new WaitForResurrectionPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
