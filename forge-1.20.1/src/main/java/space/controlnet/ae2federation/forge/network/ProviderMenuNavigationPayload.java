package space.controlnet.ae2federation.forge.network;

import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import space.controlnet.ae2federation.client.menu.FederationDomainPolicyMenu;
import space.controlnet.ae2federation.processing.provider.FederationPatternProviderBlockEntity;

/** Navigation is bound to the server's currently open, usable native provider menu. */
public record ProviderMenuNavigationPayload(int containerId, boolean returning) {

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeBoolean(returning);
    }

    public static ProviderMenuNavigationPayload decode(FriendlyByteBuf buffer) {
        return new ProviderMenuNavigationPayload(buffer.readVarInt(), buffer.readBoolean());
    }

    public void handle(ServerPlayer player) {
        if (!player.getServer().isSameThread() || containerId < 0 || containerId > 100) return;
        if (returning) {
            FederationDomainPolicyMenu.returnToProvider(player, containerId);
            return;
        }
        if (player.containerMenu instanceof PatternProviderMenu menu && menu.containerId == containerId
                && menu.stillValid(player) && menu.getBlockEntity() instanceof FederationPatternProviderBlockEntity provider
                && provider.getLevel() == player.level()
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(provider.getBlockPos())) <= 64) {
            FederationDomainPolicyMenu.openDevice(player, provider.getBlockPos());
        }
    }

}
