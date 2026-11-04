package space.controlnet.ae2federation.capability;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge 1.21's {@code RegisterCapabilitiesEvent.registerBlockEntity/registerBlock}, on Forge 1.20.1: rules that say
 * which block entities offer which capability, applied by one {@link AttachCapabilitiesEvent} listener.
 */
public final class CapProviders {
    private static final ResourceLocation ID = new ResourceLocation("ae2federation", "capabilities");
    private static final List<Rule<?>> RULES = new CopyOnWriteArrayList<>();
    private static boolean listening;

    private CapProviders() {
    }

    /** Where a block (not only a block entity type) offers a capability: the block entity there may be another mod's. */
    @FunctionalInterface
    public interface BlockProvider<T> {
        @Nullable T get(Level level, BlockPos position, BlockState state, @Nullable BlockEntity blockEntity,
                @Nullable Direction side);
    }

    private record Rule<T>(Capability<T> capability, Predicate<BlockEntity> applies,
            BiFunction<BlockEntity, Direction, T> provider) {
    }

    /** Idempotent: called by every registration, listens once. */
    public static synchronized void init() {
        if (listening) {
            return;
        }
        listening = true;
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, CapProviders::onAttach);
    }

    @SuppressWarnings("unchecked")
    public static <B extends BlockEntity, T> void forBlockEntity(Capability<T> capability,
            Supplier<BlockEntityType<B>> type, BiFunction<B, Direction, T> provider) {
        RULES.add(new Rule<>(capability, blockEntity -> blockEntity.getType() == type.get(),
                (blockEntity, side) -> provider.apply((B) blockEntity, side)));
    }

    @SafeVarargs
    public static <T> void forBlocks(Capability<T> capability, BlockProvider<T> provider,
            Supplier<? extends Block>... blocks) {
        RULES.add(new Rule<>(capability, blockEntity -> {
            var block = blockEntity.getBlockState().getBlock();
            for (var candidate : blocks) {
                if (candidate.get() == block) {
                    return true;
                }
            }
            return false;
        }, (blockEntity, side) -> provider.get(blockEntity.getLevel(), blockEntity.getBlockPos(),
                blockEntity.getBlockState(), blockEntity, side)));
    }

    private static void onAttach(AttachCapabilitiesEvent<BlockEntity> event) {
        var blockEntity = event.getObject();
        var mine = RULES.stream().filter(rule -> rule.applies().test(blockEntity)).toList();
        if (mine.isEmpty()) {
            return;
        }
        event.addCapability(ID, new ICapabilityProvider() {
            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public <C> @NotNull LazyOptional<C> getCapability(@NotNull Capability<C> capability,
                    @Nullable Direction side) {
                for (var rule : mine) {
                    if (rule.capability() == capability) {
                        Object value = ((BiFunction) rule.provider()).apply(blockEntity, side);
                        if (value != null) {
                            return LazyOptional.of(() -> (C) value);
                        }
                    }
                }
                return LazyOptional.empty();
            }
        });
    }
}
