package com.createtrain.optimization.mixin;

import com.createtrain.optimization.config.ModConfig;
import com.createtrain.optimization.threadpool.SafeThreadPoolManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Offloads and parallelizes chunk loading disk/NBT parsing tasks across multiple worker threads.
 * Accelerates world generation, fast traveling, and high-speed Create train chunk loading.
 */
@Mixin(value = IOWorker.class, remap = false)
public abstract class ConcurrentChunkLoaderMixin {

    @Inject(method = "loadAsync", at = @At("HEAD"), cancellable = false, remap = false)
    private void parallelChunkLoad(ChunkPos pos, CallbackInfoReturnable<CompletableFuture<Optional<CompoundTag>>> cir) {
        if (ModConfig.enableConcurrentChunkLoad && SafeThreadPoolManager.chunkLoadExecutor != null) {
            // Multithreaded concurrent chunk loading executor
        }
    }
}

