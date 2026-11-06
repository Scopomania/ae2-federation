package space.controlnet.ae2federation.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import space.controlnet.ae2federation.observability.ObservationLimits;
import space.controlnet.ae2federation.observability.state.FederationDomainStateDelta;
import space.controlnet.ae2federation.observability.state.ObservationDeltaEnvelope;

public record FederationDomainStateDeltaPayload(ObservationDeltaEnvelope envelope) {

    public void encode(FriendlyByteBuf buffer) {
        var start = buffer.writerIndex();
        FederationDomainStateSnapshotCodec.encodeSession(buffer, envelope.session());
        buffer.writeVarLong(envelope.delta().baseDataRevision());
        buffer.writeBoolean(envelope.delta().resnapshotRequired());
        FederationDomainStateSnapshotCodec.encode(buffer, envelope.delta().replacement());
        if (buffer.writerIndex() - start > ObservationLimits.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Observation delta payload is oversized");
        }
    }

    public static FederationDomainStateDeltaPayload decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > ObservationLimits.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Observation delta payload is oversized");
        }
        var session = FederationDomainStateSnapshotCodec.decodeSession(buffer);
        var baseRevision = buffer.readVarLong();
        var resnapshotRequired = buffer.readBoolean();
        var delta = new FederationDomainStateDelta(FederationDomainStateSnapshotCodec.decode(buffer), baseRevision, resnapshotRequired);
        if (buffer.isReadable()) {
            throw new IllegalArgumentException("Observation delta payload has trailing data");
        }
        return new FederationDomainStateDeltaPayload(new ObservationDeltaEnvelope(session, delta));
    }

}
