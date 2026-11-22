package space.controlnet.ae2federation.forge.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyActionResult;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyMenu;

/** Client-side packet handlers, in their own class so the server never resolves {@link Minecraft} through a packet. */
public final class ClientPacketHandlers {
    public static void policyView(space.controlnet.ae2federation.forge.network.PolicyViewPayload payload) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu.containerId == payload.containerId()
                && player.containerMenu instanceof space.controlnet.ae2federation.forge.gui.FederationPolicyMenu menu) {
            menu.acceptView(payload.json(), payload.status(), payload.power(), payload.overview());
        }
    }

    private ClientPacketHandlers() {
    }

    public static void policyReply(int containerId, UUID menuNonce, UUID requestId, long sequence,
            FederationDomainPolicyActionResult result) {
        FederationDomainPolicyMenu.acceptReply(Minecraft.getInstance().player, containerId, menuNonce, requestId, sequence,
                result);
    }
}
