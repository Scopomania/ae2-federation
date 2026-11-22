package space.controlnet.ae2federation.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import space.controlnet.ae2federation.forge.gui.FederationPolicyMenu;

/** One click on the Federation screen: what kind it is and its encoded target. The server re-checks it against the live Domain. */
public record PolicyCommandPayload(int containerId, String kind, String argument) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeUtf(kind, 32);
        buffer.writeUtf(argument, 512);
    }

    public static PolicyCommandPayload decode(FriendlyByteBuf buffer) {
        return new PolicyCommandPayload(buffer.readVarInt(), buffer.readUtf(32), buffer.readUtf(512));
    }

    public static void register() {
        FederationNetwork.toServer(PolicyCommandPayload.class, PolicyCommandPayload::encode, PolicyCommandPayload::decode,
                (payload, context) -> context.enqueueWork(() -> {
                    var player = context.getSender();
                    if (player != null && player.containerMenu.containerId == payload.containerId()
                            && player.containerMenu instanceof FederationPolicyMenu menu) {
                        menu.command(player, payload.kind(), payload.argument());
                    }
                }));
    }
}
