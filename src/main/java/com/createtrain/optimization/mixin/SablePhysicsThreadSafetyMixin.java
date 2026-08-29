package com.createtrain.optimization.mixin;

import com.createtrain.optimization.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Mixin targeting Sable's VoxelNeighborhoodState or RapierPhysicsPipeline.
 * Dynamically acquires a ReentrantReadWriteLock during Voxel map lookup to prevent 
 * Index -1 rehash race condition when chunks setBlockState concurrently.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState", remap = false)
public abstract class SablePhysicsThreadSafetyMixin {

    private static final ReentrantReadWriteLock MAP_LOCK = new ReentrantReadWriteLock();

    @Inject(method = "isSolid", at = @At("HEAD"), cancellable = false)
    private void acquireReadLockBeforeLookup(CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.enableSableLockFix) {
            MAP_LOCK.readLock().lock();
        }
    }

    @Inject(method = "isSolid", at = @At("RETURN"), cancellable = false)
    private void releaseReadLockAfterLookup(CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.enableSableLockFix) {
            MAP_LOCK.readLock().unlock();
        }
    }
}
