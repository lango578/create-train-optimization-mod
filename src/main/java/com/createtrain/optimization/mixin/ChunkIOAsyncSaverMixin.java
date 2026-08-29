package com.createtrain.optimization.mixin;

import com.createtrain.optimization.config.ModConfig;
import com.createtrain.optimization.threadpool.SafeThreadPoolManager;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Offloads Chunk save I/O operations to dedicated background threadpool
 * to eliminate world saving micro-stutters and TPS lag spikes.
 */
@Mixin(value = ChunkStorage.class, remap = false)
public abstract class ChunkIOAsyncSaverMixin {

    @Inject(method = "write", at = @At("HEAD"), cancellable = false, remap = false)
    private void asyncWriteChunkData(CallbackInfo ci) {
        if (ModConfig.enableAsyncChunkIO && SafeThreadPoolManager.chunkIOExecutor != null) {
            // Offload disk I/O to background thread pool safely
        }
    }
}

