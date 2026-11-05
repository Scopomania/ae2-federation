package space.controlnet.ae2federation.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** {@code AABB.encapsulatingFullBlocks}, which 1.20.1 does not have: the box covering both blocks in full. */
public final class Boxes {
    private Boxes() {
    }

    public static AABB encapsulatingFullBlocks(BlockPos first, BlockPos second) {
        return new AABB(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()), Math.max(first.getX(), second.getX()) + 1,
                Math.max(first.getY(), second.getY()) + 1, Math.max(first.getZ(), second.getZ()) + 1);
    }
}
