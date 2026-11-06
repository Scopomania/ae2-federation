package space.controlnet.ae2federation.forge.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyActionResult;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyMenu;

/** Client-side packet handlers, in their own class so the server never resolves {@link Minecraft} through a packet. */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void policyReply(int containerId, UUID menuNonce, UUID requestId, long sequence,
            FederationDomainPolicyActionResult result) {
        FederationDomainPolicyMenu.acceptReply(Minecraft.getInstance().player, containerId, menuNonce, requestId, sequence,
                result);
    }
}
