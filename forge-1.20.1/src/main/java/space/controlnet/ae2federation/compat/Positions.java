package space.controlnet.ae2federation.compat;

import net.minecraft.core.BlockPos;

/** {@code BlockPos.min} and {@code max}, which 1.20.1 does not have. */
public final class Positions {
    private Positions() {
    }

    public static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    public static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }
}
