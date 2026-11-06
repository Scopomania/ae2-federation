package space.controlnet.ae2federation.forge.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import space.controlnet.ae2federation.bundle.BundleFormSink;
import space.controlnet.ae2federation.bundle.BundleInteraction;

/** A wrench's three clicks on Federation Cables; the server checks them again before it forms the bundle. */
public record BundleFormPayload(BlockPos first, BlockPos second, BlockPos third, Direction face) {
    public static void register() {
        FederationNetwork.toServer(BundleFormPayload.class, BundleFormPayload::encode, BundleFormPayload::decode,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.getSender() != null) {
                        BundleInteraction.form(context.getSender(), payload.first(), payload.second(), payload.third(),
                                payload.face());
                    }
                }));
        BundleFormSink.register((first, second, third, face) ->
                FederationNetwork.sendToServer(new BundleFormPayload(first, second, third, face)));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(first);
        buffer.writeBlockPos(second);
        buffer.writeBlockPos(third);
        buffer.writeEnum(face);
    }

    public static BundleFormPayload decode(FriendlyByteBuf buffer) {
        return new BundleFormPayload(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBlockPos(),
                buffer.readEnum(Direction.class));
    }
}
