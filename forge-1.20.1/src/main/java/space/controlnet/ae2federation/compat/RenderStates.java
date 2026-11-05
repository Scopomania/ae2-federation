package space.controlnet.ae2federation.compat;

import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderStateShard;

/**
 * The render states a custom {@code RenderType} needs. 1.20.1 keeps them {@code protected} in
 * {@link RenderStateShard}; a subclass may read them, so they are re-exported here (1.21 made them public).
 */
public final class RenderStates extends RenderStateShard {
    public static final CullStateShard NO_CULL_STATE = RenderStateShard.NO_CULL;
    public static final WriteMaskStateShard COLOR_WRITE_STATE = RenderStateShard.COLOR_WRITE;
    public static final WriteMaskStateShard COLOR_DEPTH_WRITE_STATE = RenderStateShard.COLOR_DEPTH_WRITE;
    public static final TransparencyStateShard TRANSLUCENT_STATE = RenderStateShard.TRANSLUCENT_TRANSPARENCY;
    public static final DepthTestStateShard LEQUAL_DEPTH_STATE = RenderStateShard.LEQUAL_DEPTH_TEST;
    public static final DepthTestStateShard NO_DEPTH_STATE = RenderStateShard.NO_DEPTH_TEST;
    public static final ShaderStateShard BEACON_BEAM_SHADER = RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER;
    public static final ShaderStateShard LINES_SHADER = RenderStateShard.RENDERTYPE_LINES_SHADER;
    public static final ShaderStateShard POSITION_COLOR_SHADER_STATE = RenderStateShard.POSITION_COLOR_SHADER;
    public static final OutputStateShard MAIN_TARGET_STATE = RenderStateShard.MAIN_TARGET;

    private RenderStates() {
        super("ae2federation_states", () -> {
        }, () -> {
        });
    }

    /** A line width state whose width is scaled with the window, as vanilla's wide lines are not. */
    public static LineStateShard lineState(OptionalDouble width, Runnable beforeSetup) {
        return new LineStateShard(width) {
            @Override
            public void setupRenderState() {
                super.setupRenderState();
                beforeSetup.run();
            }
        };
    }
}
