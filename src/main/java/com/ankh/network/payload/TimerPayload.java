package com.ankh.network.payload;

import com.ankh.AnkhResurrection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record TimerPayload(int secondsRemaining, boolean singleplayer) implements CustomPayload {

    public static final CustomPayload.Id<TimerPayload> ID =
            new CustomPayload.Id<>(AnkhResurrection.id("timer"));

    public static final PacketCodec<RegistryByteBuf, TimerPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, TimerPayload::secondsRemaining,
            PacketCodecs.BOOL, TimerPayload::singleplayer,
            TimerPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
