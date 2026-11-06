package space.controlnet.ae2federation.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import space.controlnet.ae2federation.observability.ObservationLimits;
import space.controlnet.ae2federation.observability.state.ObservationSession;

public record ObservationSessionPayload(Operation operation, ObservationSession session) {

    public void encode(FriendlyByteBuf buffer) {
        var start = buffer.writerIndex();
        buffer.writeByte(operation.wireId);
        FederationDomainStateSnapshotCodec.encodeSession(buffer, session);
        if (buffer.writerIndex() - start > ObservationLimits.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Observation session payload is oversized");
        }
    }

    public static ObservationSessionPayload decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > ObservationLimits.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Observation session payload is oversized");
        }
        var operation = Operation.fromWireId(buffer.readUnsignedByte());
        var payload = new ObservationSessionPayload(operation, FederationDomainStateSnapshotCodec.decodeSession(buffer));
        if (buffer.isReadable()) {
            throw new IllegalArgumentException("Observation session payload has trailing data");
        }
        return payload;
    }


    public enum Operation {
        OPEN(0),
        CLOSE(1);

        private final int wireId;

        Operation(int wireId) {
            this.wireId = wireId;
        }

        private static Operation fromWireId(int wireId) {
            return switch (wireId) {
                case 0 -> OPEN;
                case 1 -> CLOSE;
                default -> throw new IllegalArgumentException("Unknown observation session operation");
            };
        }
    }
}
