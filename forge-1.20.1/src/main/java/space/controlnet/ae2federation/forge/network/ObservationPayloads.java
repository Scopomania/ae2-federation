package space.controlnet.ae2federation.forge.network;

import space.controlnet.ae2federation.observability.ObservationDeltaSink;
import space.controlnet.ae2federation.observability.ObservationSessionLifecycleSink;
import space.controlnet.ae2federation.observability.ObservationSnapshotSink;
import space.controlnet.ae2federation.observability.state.ObservationClientStates;

public final class ObservationPayloads {
    private static final ObservationClientStates CLIENT_STATES = new ObservationClientStates();

    private ObservationPayloads() {
    }

    public static void register() {
        FederationNetwork.toClient(ObservationSessionPayload.class, ObservationSessionPayload::encode,
                ObservationSessionPayload::decode, (payload, context) -> context.enqueueWork(() -> {
                    switch (payload.operation()) {
                        case OPEN -> CLIENT_STATES.open(payload.session());
                        case CLOSE -> CLIENT_STATES.close(payload.session());
                    }
                }));
        FederationNetwork.toClient(FederationDomainStateSnapshotPayload.class, FederationDomainStateSnapshotPayload::encode,
                FederationDomainStateSnapshotPayload::decode,
                (payload, context) -> context.enqueueWork(() -> CLIENT_STATES.apply(payload.envelope())));
        FederationNetwork.toClient(FederationDomainStateDeltaPayload.class, FederationDomainStateDeltaPayload::encode,
                FederationDomainStateDeltaPayload::decode,
                (payload, context) -> context.enqueueWork(() -> CLIENT_STATES.apply(payload.envelope())));
        ObservationSnapshotSink.register((player, snapshot) ->
                FederationNetwork.sendToPlayer(player, new FederationDomainStateSnapshotPayload(snapshot)));
        ObservationDeltaSink.register((player, delta) ->
                FederationNetwork.sendToPlayer(player, new FederationDomainStateDeltaPayload(delta)));
        ObservationSessionLifecycleSink.register(
                (player, session) -> FederationNetwork.sendToPlayer(player,
                        new ObservationSessionPayload(ObservationSessionPayload.Operation.OPEN, session)),
                (player, session) -> FederationNetwork.sendToPlayer(player,
                        new ObservationSessionPayload(ObservationSessionPayload.Operation.CLOSE, session)));
    }

    public static ObservationClientStates clientStates() {
        return CLIENT_STATES;
    }
}
