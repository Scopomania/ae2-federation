package space.controlnet.ae2federation.forge.network;

import net.minecraft.server.level.ServerPlayer;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyActionResult;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyActionSink;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyMenu;
import space.controlnet.ae2federation.forge.client.ClientPacketHandlers;

public final class FederationDomainPolicyActionPayloads {
    private FederationDomainPolicyActionPayloads() {
    }

    public static void register() {
        FederationNetwork.toServer(ProviderMenuNavigationPayload.class, ProviderMenuNavigationPayload::encode,
                ProviderMenuNavigationPayload::decode, (payload, context) -> context.enqueueWork(() -> {
                    if (context.getSender() != null) payload.handle(context.getSender());
                }));
        FederationNetwork.toClient(FederationDomainPolicyReplyPayload.class, FederationDomainPolicyReplyPayload::encode,
                FederationDomainPolicyReplyPayload::decode, (payload, context) -> context.enqueueWork(() ->
                        ClientPacketHandlers.policyReply(payload.containerId(), payload.menuNonce(), payload.requestId(),
                                payload.sequence(), payload.result())));
        FederationNetwork.toServer(FederationDomainPolicyActionPayload.class, FederationDomainPolicyActionPayload::encode,
                FederationDomainPolicyActionPayload::decode, (payload, context) -> context.enqueueWork(() -> {
                    var player = context.getSender();
                    if (player != null) {
                        var result = handle(player, payload);
                        var request = payload.request();
                        FederationNetwork.sendToPlayer(player, new FederationDomainPolicyReplyPayload(request.containerId(),
                                request.menuNonce(), payload.requestId(), request.menuSequence(), result));
                    }
                }));
        FederationDomainPolicyActionSink.registerReturn(containerId ->
                FederationNetwork.sendToServer(new ProviderMenuNavigationPayload(containerId, true)));
        FederationDomainPolicyActionSink.register((request, requestId) ->
                FederationNetwork.sendToServer(new FederationDomainPolicyActionPayload(request, requestId)));
    }

    public static FederationDomainPolicyActionResult handle(ServerPlayer player, FederationDomainPolicyActionPayload payload) {
        return FederationDomainPolicyMenu.dispatch(player, payload.request());
    }
}
