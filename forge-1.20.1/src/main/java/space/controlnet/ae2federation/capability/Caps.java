package space.controlnet.ae2federation.capability;

import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

/**
 * Block capability lookups on Forge 1.20.1. NeoForge 1.21 caches lookups and tells the cache when they change; here
 * a lookup asks the block entity every time, so there is nothing to invalidate.
 */
public final class Caps {
    private Caps() {
    }

    /** The capability at {@code position} seen from {@code side}, or null when that chunk is not loaded or it has none. */
    public static <T> @Nullable T find(@Nullable Level level, BlockPos position, Capability<T> capability,
            @Nullable Direction side) {
        if (level == null || !level.isLoaded(position)) {
            return null;
        }
        var blockEntity = level.getBlockEntity(position);
        return blockEntity == null ? null : blockEntity.getCapability(capability, side).orElse(null);
    }

    /** Kept so call sites that told 1.21's caches to refresh still say so; lookups here are never cached. */
    public static void invalidate(@Nullable Level level, BlockPos position) {
    }

    /** What 1.21's {@code BlockCapabilityCache} was for: the capability at one position and side, while it stays valid. */
    public static final class Cache<T> {
        private final Capability<T> capability;
        private final Level level;
        private final BlockPos position;
        private final @Nullable Direction side;
        private final BooleanSupplier valid;

        private Cache(Capability<T> capability, Level level, BlockPos position, @Nullable Direction side,
                BooleanSupplier valid) {
            this.capability = capability;
            this.level = level;
            this.position = position;
            this.side = side;
            this.valid = valid;
        }

        /**
         * The invalidation listener of 1.21 is not called: nothing here reports a capability change, so owners recheck
         * on neighbour updates and their own ticks.
         */
        public static <T> Cache<T> create(Capability<T> capability, Level level, BlockPos position,
                @Nullable Direction side, BooleanSupplier valid, Runnable invalidationListener) {
            return new Cache<>(capability, level, position.immutable(), side, valid);
        }

        public @Nullable T getCapability() {
            return valid.getAsBoolean() ? find(level, position, capability, side) : null;
        }
    }
}
