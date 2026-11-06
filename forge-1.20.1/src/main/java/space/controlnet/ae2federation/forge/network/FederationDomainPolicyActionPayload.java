package space.controlnet.ae2federation.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyAction;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyActionRequest;
import space.controlnet.ae2federation.domain.FederationDomainId;
import space.controlnet.ae2federation.domain.FederationDomainReference;
import space.controlnet.ae2federation.observability.ObservationLimits;
import space.controlnet.ae2federation.policy.PolicyRevision;

public record FederationDomainPolicyActionPayload(FederationDomainPolicyActionRequest request, java.util.UUID requestId) {
    public FederationDomainPolicyActionPayload(FederationDomainPolicyActionRequest request) {
        this(request, java.util.UUID.randomUUID());
    }


    public void encode(FriendlyByteBuf buffer) {
        var start = buffer.writerIndex();
        buffer.writeByte(request.action().wireId());
        buffer.writeVarInt(request.containerId());
        buffer.writeUUID(request.menuNonce());
        buffer.writeVarLong(request.menuSequence());
        buffer.writeUtf(request.context().federationDomainId().value(), ObservationLimits.MAX_ID_LENGTH);
        buffer.writeVarLong(request.context().generation());
        buffer.writeVarLong(request.expectedRevision().value());
        if (request.action().takesTarget()) {
            buffer.writeUtf(request.target(), 160);
        }
        buffer.writeUUID(requestId);
        if (buffer.writerIndex() - start > FederationDomainPolicyActionRequest.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Federation Domain policy action payload is oversized");
        }
    }

    public static FederationDomainPolicyActionPayload decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > FederationDomainPolicyActionRequest.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Federation Domain policy action payload is oversized");
        }
        var action = FederationDomainPolicyAction.fromWireId(buffer.readUnsignedByte());
        var containerId = buffer.readVarInt();
        var nonce = buffer.readUUID();
        var sequence = buffer.readVarLong();
        var federationDomainId = new FederationDomainId(buffer.readUtf(ObservationLimits.MAX_ID_LENGTH));
        var context = new FederationDomainReference(federationDomainId, buffer.readVarLong());
        var revision = new PolicyRevision(buffer.readVarLong());
        var target = action.takesTarget() ? buffer.readUtf(160) : "";
        var requestId = buffer.readUUID();
        if (buffer.isReadable()) {
            throw new IllegalArgumentException("Federation Domain policy action payload has trailing data");
        }
        return new FederationDomainPolicyActionPayload(
                new FederationDomainPolicyActionRequest(action, containerId, nonce, sequence, context, revision, target), requestId);
    }

}
