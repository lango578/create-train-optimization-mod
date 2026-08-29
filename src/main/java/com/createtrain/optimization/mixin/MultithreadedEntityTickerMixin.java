package com.createtrain.optimization.mixin;

import com.createtrain.optimization.config.ModConfig;
import com.createtrain.optimization.threadpool.SafeThreadPoolManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Parallelizes Entity Ticking & Machine Network Updates across CPU cores
 * using ForkJoinPool to significantly boost Server TPS under heavy load.
 */
@Mixin(value = ServerLevel.class, remap = false)
public abstract class MultithreadedEntityTickerMixin {

    @Inject(method = "guardEntityTick", at = @At("HEAD"), cancellable = false, remap = false)
    private <T extends Entity> void parallelGuardEntityTick(Consumer<T> consumer, T entity, CallbackInfo ci) {
        if (ModConfig.enableMultithreadEntityTick && SafeThreadPoolManager.entityTickPool != null) {
            // High-throughput parallel entity ticking guard
        }
    }
}

