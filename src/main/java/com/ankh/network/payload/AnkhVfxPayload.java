package com.ankh.network.payload;

import com.ankh.AnkhResurrection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record AnkhVfxPayload() implements CustomPayload {

    public static final CustomPayload.Id<AnkhVfxPayload> ID =
            new CustomPayload.Id<>(AnkhResurrection.id("ankh_vfx"));

    public static final PacketCodec<RegistryByteBuf, AnkhVfxPayload> CODEC =
            PacketCodec.unit(new AnkhVfxPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
