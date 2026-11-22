package space.controlnet.ae2federation.client.menu;

import net.minecraft.core.BlockPos;
import space.controlnet.ae2federation.client.policy.FederationDomainPolicySession;
import net.minecraft.server.level.ServerPlayer;
import space.controlnet.ae2federation.bridge.BridgeRightClickContext;

/**
 * Placeholder for the policy screen. The 1.21 version is an LDLib2 UI; it is rewritten on LDLib 1 later, so until then
 * these report that nothing opened and the block falls through to its default use.
 */
public final class FederationDomainPolicyMenu {
    private FederationDomainPolicyMenu() {
    }

    public static void register() {
    }

    public static boolean openRouter(ServerPlayer player, BlockPos position) {
        return open(player, FederationDomainPolicySession.forRouter(player, position));
    }

    public static boolean openDevice(ServerPlayer player, BlockPos position) {
        return open(player, FederationDomainPolicySession.forDevice(player, position));
    }

    public static boolean openProvider(ServerPlayer player, BlockPos position) {
        return open(player, FederationDomainPolicySession.forDevice(player, position));
    }

    /** Opens the Federation screen on a session; the screen shows the session's snapshot and sends clicks back to it. */
    private static boolean open(ServerPlayer player, FederationDomainPolicySession session) {
        if (session == null) {
            return false;
        }
        net.minecraftforge.network.NetworkHooks.openScreen(player,
                new net.minecraft.world.SimpleMenuProvider((containerId, inventory, ignored) ->
                        new space.controlnet.ae2federation.forge.gui.FederationPolicyMenu(containerId, inventory, session),
                        net.minecraft.network.chat.Component.literal("Federation")));
        return true;
    }

    public static void returnToProvider(ServerPlayer player, int containerId) {
    }

    public static FederationDomainPolicyActionResult dispatch(ServerPlayer player,
            FederationDomainPolicyActionRequest request) {
        return FederationDomainPolicyActionResult.INVALID_TARGET;
    }

    public static void acceptReply(net.minecraft.world.entity.player.Player player, int containerId, java.util.UUID nonce,
            java.util.UUID requestId, long sequence, FederationDomainPolicyActionResult result) {
    }

    public static boolean openBridge(ServerPlayer player, BridgeRightClickContext context) {
        return open(player, FederationDomainPolicySession.forBridge(player, context));
    }
}
