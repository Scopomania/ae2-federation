package space.controlnet.ae2federation.router;

import space.controlnet.ae2federation.capability.Caps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class FederationCableBlock extends BaseEntityBlock {

    public FederationCableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos position,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        var look = space.controlnet.ae2federation.bundle.BundleLook.of(level, position);
        return look.isPresent() ? CableShapes.bundle(look.get())
                : CableShapes.shape(CableVisualConnections.connections(level, position));
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new FederationCableBlockEntity(position, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos position, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, position, oldState, movedByPiston);
        Caps.invalidate(level, position);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos position, BlockState newState,
            boolean movedByPiston) {
        Caps.invalidate(level, position);
        if (!newState.is(this) && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            space.controlnet.ae2federation.bundle.BundledCables.get(serverLevel).removed(serverLevel, position);
        }
        super.onRemove(state, level, position, newState, movedByPiston);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos position, Block neighborBlock,
            BlockPos neighborPosition, boolean movedByPiston) {
        if (level.getBlockEntity(position) instanceof FederationCableBlockEntity cable) {
            cable.neighborChanged(neighborPosition);
        }
    }
}
