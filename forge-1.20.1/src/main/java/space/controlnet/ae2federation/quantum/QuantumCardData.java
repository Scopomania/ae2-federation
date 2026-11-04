package space.controlnet.ae2federation.quantum;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Which Quantum Cube a memory card was linked from. 1.21 kept this in a data component; 1.20.1 keeps it in the stack's NBT. */
public final class QuantumCardData {
    private static final String KEY = "ae2federation_quantum_cube";

    private QuantumCardData() {
    }

    public static void set(ItemStack stack, GlobalPos cube) {
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, cube).result()
                .ifPresent(tag -> stack.getOrCreateTag().put(KEY, tag));
    }

    public static @Nullable GlobalPos get(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(KEY)) {
            return null;
        }
        return GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get(KEY)).result().orElse(null);
    }

    public static void clear(ItemStack stack) {
        stack.removeTagKey(KEY);
    }
}
