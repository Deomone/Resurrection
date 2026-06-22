package com.ankh.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.server.world.ServerChunkLoadingManager$EntityTracker")
public interface EntityTrackerInvoker {
    @Invoker("stopTracking")
    void ankh$stopTracking(ServerPlayerEntity player);

    @Invoker("updateTrackedStatus")
    void ankh$updateTrackedStatus(ServerPlayerEntity player);
}
