package space.controlnet.ae2federation.client;

import space.controlnet.ae2federation.compat.RenderStates;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Fullbright vanilla beacon shading in one additive pass. */
public final class BeamPortalRenderer {
    private static final BeamPortalSceneQueue SCENE = new BeamPortalSceneQueue();
    private static final ResourceLocation WHITE = new ResourceLocation("textures/misc/white.png");
    // Entity translucent emissive still calls minecraft_mix_light and dims optical carriers.
    // Beacon's shader uses unlit vertex color, with world fog and normal scene depth testing.
    private static final RenderType GLOW = RenderType.create("ae2federation_portal_glow",
            DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStates.BEACON_BEAM_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(WHITE, false, false))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("optical_additive", () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    }, () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }))
                    .setCullState(RenderStates.NO_CULL_STATE)
                    .setDepthTestState(RenderStates.LEQUAL_DEPTH_STATE)
                    .setWriteMaskState(RenderStates.COLOR_WRITE_STATE)
                    .createCompositeState(false));

    private static final Matrix4f TRANSFORM = new Matrix4f();
    private static Frustum frustum;

    private BeamPortalRenderer() {}

    static void beginFrame(Frustum current) {
        frustum = current;
    }

    static boolean visible(BlockPos origin, Direction facing, int side) {
        return frustum == null || frustum.isVisible(BeamPortalMesh.bounds(origin, facing, side));
    }

    static void flush(MultiBufferSource.BufferSource buffers) {
        buffers.endBatch(GLOW);
        frustum = null;
    }

    public static void inScene(Runnable render) {
        SCENE.inScene(render);
    }

    static void recordScene(PoseStack stack, Direction facing, int side, long tick, float partialTick,
            CableBeamStyle style) {
        SCENE.record(stack.last().pose(), facing, side, tick, partialTick, style);
    }

    public static void drawScene(MultiBufferSource.BufferSource buffers) {
        SCENE.draw(entry -> {
            var stack = new PoseStack();
            stack.last().pose().set(entry.pose());
            draw(stack, buffers, entry.facing(), entry.side(), 0, entry.tick(), entry.partialTick(), entry.style());
            buffers.endBatch(GLOW);
        });
    }

    static void draw(PoseStack stack, MultiBufferSource buffers, Direction facing, int side, int detail, long tick,
            float partialTick, CableBeamStyle style) {
        var matrix = BeamPortalMesh.transform(stack.last().pose(), facing, side, TRANSFORM);
        var out = buffers.getBuffer(GLOW);
        BeamPortalMesh.Sink sink = (x, y, z, r, g, b, alpha) -> out.vertex(matrix, x, y, z)
                .color(r, g, b, alpha).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();
        BeamPortalMesh.emit(true, side, detail, tick, partialTick, style, sink);
    }
}
