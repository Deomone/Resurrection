package com.ankh.session;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResurrectionState extends PersistentState {

    public static final String KEY = "ankh_resurrection";

    private final Map<UUID, ResurrectionSession> sessions = new HashMap<>();

    public ResurrectionState() {}

    public ResurrectionSession get(UUID owner) {
        return sessions.get(owner);
    }

    public boolean has(UUID owner) {
        return sessions.containsKey(owner);
    }

    public void put(ResurrectionSession session) {
        sessions.put(session.getOwner(), session);
        markDirty();
    }

    public void remove(UUID owner) {
        if (sessions.remove(owner) != null) {
            markDirty();
        }
    }

    public Collection<ResurrectionSession> all() {
        return sessions.values();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();
        for (ResurrectionSession session : sessions.values()) {
            list.add(session.toNbt());
        }
        nbt.put("Sessions", list);
        return nbt;
    }

    public static ResurrectionState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ResurrectionState state = new ResurrectionState();
        NbtList list = nbt.getList("Sessions", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            ResurrectionSession session = ResurrectionSession.fromNbt(list.getCompound(i));
            state.sessions.put(session.getOwner(), session);
        }
        return state;
    }

    public static final PersistentState.Type<ResurrectionState> TYPE = new PersistentState.Type<>(
            ResurrectionState::new,
            ResurrectionState::createFromNbt,
            null
    );

    public static ResurrectionState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager manager = overworld.getPersistentStateManager();
        ResurrectionState state = manager.getOrCreate(TYPE, KEY);
        state.markDirty();
        return state;
    }
}
