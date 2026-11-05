package space.controlnet.ae2federation.compat;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;

/** NeoForge's quad baker hands back each finished quad; Forge 1.20.1's pushes it to a consumer. This keeps the first form. */
public final class QuadBaker extends QuadBakingVertexConsumer {
    private final BakedQuad[] holder;

    private QuadBaker(BakedQuad[] holder) {
        super(quad -> holder[0] = quad);
        this.holder = holder;
    }

    public QuadBaker() {
        this(new BakedQuad[1]);
    }

    /** The quad finished by the last fourth vertex. */
    public BakedQuad bakeQuad() {
        var quad = holder[0];
        holder[0] = null;
        return quad;
    }
}
