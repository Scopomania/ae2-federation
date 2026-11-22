package space.controlnet.ae2federation.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import space.controlnet.ae2federation.forge.client.ClientPacketHandlers;

/** What the Federation screen shows, as the session built it: the choices JSON and the status, power and overview texts. */
public record PolicyViewPayload(int containerId, String json, String status, String power, String overview) {
    private static final int MAX = 1 << 20;

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeUtf(json, MAX);
        buffer.writeUtf(status, MAX);
        buffer.writeUtf(power, MAX);
        buffer.writeUtf(overview, MAX);
    }

    public static PolicyViewPayload decode(FriendlyByteBuf buffer) {
        return new PolicyViewPayload(buffer.readVarInt(), buffer.readUtf(MAX), buffer.readUtf(MAX), buffer.readUtf(MAX),
                buffer.readUtf(MAX));
    }

    public static void register() {
        FederationNetwork.toClient(PolicyViewPayload.class, PolicyViewPayload::encode, PolicyViewPayload::decode,
                (payload, context) -> context.enqueueWork(() -> ClientPacketHandlers.policyView(payload)));
    }
}
