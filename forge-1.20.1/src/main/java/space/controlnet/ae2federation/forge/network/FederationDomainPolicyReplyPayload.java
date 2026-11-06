package space.controlnet.ae2federation.forge.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyActionResult;

public record FederationDomainPolicyReplyPayload(int containerId, UUID menuNonce, UUID requestId, long sequence,
        FederationDomainPolicyActionResult result) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeUUID(menuNonce);
        buffer.writeUUID(requestId);
        buffer.writeVarLong(sequence);
        buffer.writeEnum(result);
    }

    public static FederationDomainPolicyReplyPayload decode(FriendlyByteBuf buffer) {
        return new FederationDomainPolicyReplyPayload(buffer.readVarInt(), buffer.readUUID(), buffer.readUUID(),
                buffer.readVarLong(), buffer.readEnum(FederationDomainPolicyActionResult.class));
    }

}
