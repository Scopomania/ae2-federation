package space.controlnet.ae2federation.compat;

import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21 splits a block's use into "with the held item" and "without it", the first answering with this. 1.20.1 has one
 * {@code use}; blocks keep their two methods and {@link #bridge} folds their answers back into one.
 */
public enum ItemInteractionResult {
    SUCCESS(InteractionResult.SUCCESS),
    CONSUME(InteractionResult.CONSUME),
    CONSUME_PARTIAL(InteractionResult.CONSUME_PARTIAL),
    /** Neither the block nor the default block interaction acts; the held item's own use goes ahead. */
    SKIP_DEFAULT_BLOCK_INTERACTION(InteractionResult.PASS),
    /** The block does nothing with the item; the bare-handed use is tried next. */
    PASS_TO_DEFAULT_BLOCK_INTERACTION(null),
    FAIL(InteractionResult.FAIL);

    private final @Nullable InteractionResult result;

    ItemInteractionResult(@Nullable InteractionResult result) {
        this.result = result;
    }

    /** The 1.20.1 result, or null where the without-item use should answer. */
    public @Nullable InteractionResult result() {
        return result;
    }

    /** 1.20.1 {@code InteractionResult.sidedSuccess}: SUCCESS on the client, CONSUME on the server. */
    public static ItemInteractionResult sidedSuccess(boolean clientSide) {
        return clientSide ? SUCCESS : CONSUME;
    }

    public static InteractionResult bridge(ItemInteractionResult withItem, java.util.function.Supplier<InteractionResult> without) {
        var answer = withItem.result();
        return answer != null ? answer : without.get();
    }
}
