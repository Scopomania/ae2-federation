package space.controlnet.ae2federation.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import space.controlnet.ae2federation.observability.ObservationLimits;
import space.controlnet.ae2federation.observability.state.ObservationSnapshotEnvelope;

public record FederationDomainStateSnapshotPayload(ObservationSnapshotEnvelope envelope) {

    public void encode(FriendlyByteBuf buffer) {
        var start = buffer.writerIndex();
        FederationDomainStateSnapshotCodec.encodeSession(buffer, envelope.session());
        FederationDomainStateSnapshotCodec.encode(buffer, envelope.snapshot());
        if (buffer.writerIndex() - start > ObservationLimits.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Observation snapshot payload is oversized");
        }
    }

    public static FederationDomainStateSnapshotPayload decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > ObservationLimits.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Observation snapshot payload is oversized");
        }
        var session = FederationDomainStateSnapshotCodec.decodeSession(buffer);
        var payload = new FederationDomainStateSnapshotPayload(new ObservationSnapshotEnvelope(session,
                FederationDomainStateSnapshotCodec.decode(buffer)));
        if (buffer.isReadable()) {
            throw new IllegalArgumentException("Observation snapshot payload has trailing data");
        }
        return payload;
    }

}
