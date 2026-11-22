package space.controlnet.ae2federation.forge.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import space.controlnet.ae2federation.client.policy.FederationDomainPolicySession;
import space.controlnet.ae2federation.forge.network.FederationNetwork;
import space.controlnet.ae2federation.forge.network.PolicyViewPayload;

/**
 * The Federation screen's container. The server side owns a {@link FederationDomainPolicySession}, the same one the commands
 * use, and sends the screen a text snapshot of it a few times a second; the client side only displays that snapshot and sends
 * back what the player clicked. There are no item slots.
 */
public final class FederationPolicyMenu extends AbstractContainerMenu {
    private final @Nullable FederationDomainPolicySession session;
    private String lastSent = "";
    private int ticks;
    // The latest snapshot, on the client.
    private String view = "{}";
    private String status = "";
    private String power = "";
    private String overview = "";
    private int viewVersion;

    /** Client side. */
    public FederationPolicyMenu(int containerId, Inventory inventory) {
        super(FederationMenus.POLICY.get(), containerId);
        this.session = null;
    }

    /** Server side. */
    public FederationPolicyMenu(int containerId, Inventory inventory, FederationDomainPolicySession session) {
        super(FederationMenus.POLICY.get(), containerId);
        this.session = session;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (session != null && ticks++ % 5 == 0) {
            sendView(false);
        }
    }

    private void sendView(boolean force) {
        var player = session.player();
        String json = session.workspaceChoices();
        String statusText = session.statusText().getString();
        String mappingText = session.mappingStatusText().getString();
        String powerText = session.powerGauge();
        String overviewText = session.networkOverviewText();
        String combined = json + "\u0000" + statusText + "\u0000" + mappingText + "\u0000" + powerText + "\u0000" + overviewText;
        if (!force && combined.equals(lastSent)) {
            return;
        }
        lastSent = combined;
        FederationNetwork.sendToPlayer(player, new PolicyViewPayload(containerId, json, statusText + "\n" + mappingText,
                powerText, overviewText));
    }

    /** A click from the screen. Each kind is one session action, and the session checks it against the live Domain. */
    public void command(ServerPlayer player, String kind, String argument) {
        if (session == null || !session.isStillValid(player)) {
            return;
        }
        switch (kind) {
            case "select" -> session.selectTarget(argument);
            case "policy" -> session.setPolicy(argument);
            case "rename" -> session.renameNetwork(argument);
            case "keep" -> session.keepNetwork(argument);
            default -> {
            }
        }
        sendView(true);
    }

    /** Client side: a snapshot arrived. */
    public void acceptView(String json, String status, String power, String overview) {
        this.view = json;
        this.status = status;
        this.power = power;
        this.overview = overview;
        this.viewVersion++;
    }

    public String view() {
        return view;
    }

    public String status() {
        return status;
    }

    public String power() {
        return power;
    }

    public String overview() {
        return overview;
    }

    public int viewVersion() {
        return viewVersion;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return session == null || session.isStillValid(player);
    }
}
