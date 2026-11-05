package space.controlnet.ae2federation.compat;

import appeng.api.stacks.GenericStack;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

/** AE2 19 gives {@code GenericStack} a codec; AE2 15 has NBT. This is the JSON view of that NBT. */
public final class GenericStackJson {
    private GenericStackJson() {
    }

    public static JsonElement encode(List<GenericStack> stacks) {
        var array = new JsonArray();
        for (var stack : stacks) {
            array.add(NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, GenericStack.writeTag(stack)));
        }
        return array;
    }

    public static Optional<GenericStack> decode(JsonElement element) {
        if (JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, element) instanceof CompoundTag tag) {
            return Optional.ofNullable(GenericStack.readTag(tag));
        }
        return Optional.empty();
    }
}
