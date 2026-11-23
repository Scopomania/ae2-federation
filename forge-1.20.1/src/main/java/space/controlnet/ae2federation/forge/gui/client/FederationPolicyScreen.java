package space.controlnet.ae2federation.forge.gui.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import space.controlnet.ae2federation.client.policy.PolicySwitchTarget;
import space.controlnet.ae2federation.forge.gui.FederationPolicyMenu;
import space.controlnet.ae2federation.forge.network.FederationNetwork;
import space.controlnet.ae2federation.forge.network.PolicyCommandPayload;
import space.controlnet.ae2federation.identity.NetworkId;
import space.controlnet.ae2federation.policy.PolicyCapability;
import space.controlnet.ae2federation.policy.PolicyKey;
import space.controlnet.ae2federation.policy.PolicyRevision;
import space.controlnet.ae2federation.policy.RuleMode;

/**
 * The Federation screen, first version: the domain's networks, the pair you are looking at, and that pair's three rules.
 * Pick the network that uses and the network it uses with the arrows; click a rule to step it (left click forward, right
 * click back). It shows what the server's policy session sends and sends back what you click; the session does the checking.
 * The topology map, the Processing tab and the Statistics tab of the 1.21 screen are not here yet, and neither is Rename
 * (the `/ae2federation` commands cover the processing side meanwhile).
 */
public final class FederationPolicyScreen extends AbstractContainerScreen<FederationPolicyMenu> {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 210;
    private static final int BACKGROUND = 0xE0101018;
    private static final int PANEL = 0xFF23232E;

    private final List<Hit> hits = new ArrayList<>();
    private JsonObject model = new JsonObject();
    private int parsedVersion = -1;

    private record Hit(int x1, int y1, int x2, int y2, IntConsumer action) {
        boolean contains(double x, double y) {
            return x >= x1 && x < x2 && y >= y1 && y < y2;
        }
    }

    public FederationPolicyScreen(FederationPolicyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.literal("Federation"));
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 0;
        this.inventoryLabelY = 10_000;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        refreshModel();
        hits.clear();
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, BACKGROUND);
        graphics.fill(x + 4, y + 4, x + WIDTH - 4, y + HEIGHT - 4, PANEL);
        graphics.drawString(font, Component.literal("Federation Domain"), x + 10, y + 9, 0xFFFFFF, true);

        var lines = font.split(Component.literal(menu.status().isBlank() ? "..." : menu.status()), WIDTH - 24);
        int textY = y + 22;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), x + 10, textY, 0xB8C0D0, false);
            textY += 10;
        }

        var consumerChoices = choices("consumer");
        var providerChoices = choices("provider");
        var selected = model.has("selected") ? model.getAsJsonObject("selected") : new JsonObject();
        String consumerId = text(selected, "consumer");
        String providerId = text(selected, "provider");
        if (consumerChoices.isEmpty() || consumerId.isEmpty()) {
            drawUnconfirmed(graphics, x + 10, y + 52, mouseX, mouseY, consumerChoices);
        } else {
            drawSelector(graphics, x + 10, y + 52, mouseX, mouseY, "A network that uses:", consumerChoices, consumerId, "consumer");
            drawSelector(graphics, x + 10, y + 74, mouseX, mouseY, "the resources of:", providerChoices, providerId, "provider");
            drawRules(graphics, x + 10, y + 100, mouseX, mouseY, consumerId, providerId);
        }

        graphics.drawString(font, Component.literal("Power: " + menu.power()), x + 10, y + HEIGHT - 38, 0x7CDB7C, false);
        var overview = font.split(Component.literal(menu.overview().replace('\n', ' ')), WIDTH - 24);
        if (!overview.isEmpty()) {
            graphics.drawString(font, overview.get(0), x + 10, y + HEIGHT - 26, 0x8890A0, false);
        }
        graphics.drawString(font, "Left click: next    Right click: previous", x + 10, y + HEIGHT - 14, 0x667080, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The title and the inventory label of a container screen are not used here.
    }

    private void drawSelector(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, String label, List<Choice> choices,
            String selectedId, String group) {
        graphics.drawString(font, label, x, y + 4, 0xD0D4E0, false);
        int found = 0;
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).id().equals(selectedId)) {
                found = i;
            }
        }
        final int index = found;
        final int current = index;
        int boxX = x + 118;
        button(graphics, boxX, y, 16, 16, "<", mouseX, mouseY, click -> select(group, choices, current - 1));
        graphics.fill(boxX + 18, y, boxX + 150, y + 16, 0xFF15151C);
        graphics.drawString(font, font.plainSubstrByWidth(name(choices.get(index).id()), 126), boxX + 22, y + 4, 0xFFFFFF, false);
        button(graphics, boxX + 152, y, 16, 16, ">", mouseX, mouseY, click -> select(group, choices, current + 1));
    }

    /**
     * No network is confirmed yet. List what the server does know about: a network joined to another one that was built
     * separately is a "Merge pending", settled by choosing which one to keep (the other's name and rules are deleted).
     */
    private void drawUnconfirmed(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, List<Choice> confirmed) {
        var networks = new ArrayList<JsonObject>();
        if (model.has("networks")) {
            for (JsonElement element : model.getAsJsonArray("networks")) {
                networks.add(element.getAsJsonObject());
            }
        }
        if (networks.isEmpty()) {
            graphics.drawString(font, "No networks to set rules between yet.", x, y + 4, 0xFFC857, false);
            graphics.drawString(font, "Their identities are not settled yet:", x, y + 18, 0xB8C0D0, false);
            graphics.drawString(font, "wait a moment, or load both networks' chunks.", x, y + 28, 0xB8C0D0, false);
            return;
        }
        graphics.drawString(font, "Networks here (" + networks.size() + "). Merge pending: choose one to keep.", x, y, 0xFFC857, true);
        int rowY = y + 14;
        for (var network : networks) {
            if (rowY > y + 90) {
                break;
            }
            String id = text(network, "id");
            boolean pending = confirmed.stream().noneMatch(choice -> choice.id().equals(id));
            graphics.drawString(font, font.plainSubstrByWidth(name(id), 150), x, rowY + 5, 0xFFFFFF, false);
            String detail = (network.has("providers") ? network.get("providers").getAsInt() : 0) + " providers, "
                    + (network.has("endpoints") ? network.get("endpoints").getAsInt() : 0) + " endpoints";
            graphics.drawString(font, detail, x + 156, rowY + 5, 0x8890A0, false);
            if (pending) {
                button(graphics, x + 236, rowY, 50, 16, "Keep", mouseX, mouseY, click -> send("keep", id));
            }
            rowY += 20;
        }
    }

    private void select(String group, List<Choice> choices, int index) {
        int wrapped = Math.floorMod(index, choices.size());
        send("select", group + ":" + choices.get(wrapped).id());
    }

    private void drawRules(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, String consumerId, String providerId) {
        graphics.drawString(font, "Rules", x, y, 0xFFFFFF, true);
        y += 12;
        for (var capability : PolicyCapability.values()) {
            var mode = modeOf(consumerId, providerId, capability);
            graphics.drawString(font, capabilityName(capability), x, y + 5, 0xD0D4E0, false);
            String state = switch (mode) {
                case DISABLED -> "Disabled";
                case ENABLED -> "Enabled";
                case REEXPORT -> "Enabled, re-export";
            };
            int color = mode == RuleMode.DISABLED ? 0xFFFF6B6B : 0xFF7CDB7C;
            int boxX = x + 118;
            var hovered = mouseX >= boxX && mouseX < boxX + 170 && mouseY >= y && mouseY < y + 16;
            graphics.fill(boxX, y, boxX + 170, y + 16, hovered ? 0xFF2B2B38 : 0xFF15151C);
            graphics.drawString(font, state, boxX + 6, y + 4, color, false);
            hits.add(new Hit(boxX, y, boxX + 170, y + 16, click -> step(consumerId, providerId, capability, mode, click == 1 ? -1 : 1)));
            y += 20;
        }
    }

    private void step(String consumerId, String providerId, PolicyCapability capability, RuleMode current, int direction) {
        var allowed = new ArrayList<RuleMode>();
        for (var mode : RuleMode.values()) {
            if (mode.allowedFor(capability)) {
                allowed.add(mode);
            }
        }
        int next = Math.floorMod(allowed.indexOf(current) + direction, allowed.size());
        try {
            var key = new PolicyKey(new NetworkId(UUID.fromString(consumerId)), new NetworkId(UUID.fromString(providerId)), capability);
            send("policy", new PolicySwitchTarget(key, allowed.get(next), new PolicyRevision(revisionOf(consumerId, providerId, capability))).encode());
        } catch (IllegalArgumentException ignored) {
            // A pair of the same network, or an id the server did not send: nothing to switch.
        }
    }

    private RuleMode modeOf(String consumerId, String providerId, PolicyCapability capability) {
        var rule = ruleRow("rules", consumerId, providerId, capability);
        if (rule == null || !rule.has("enabled") || !rule.get("enabled").getAsBoolean()) {
            return RuleMode.DISABLED;
        }
        return rule.has("reexport") && rule.get("reexport").getAsBoolean() ? RuleMode.REEXPORT : RuleMode.ENABLED;
    }

    private long revisionOf(String consumerId, String providerId, PolicyCapability capability) {
        var rule = ruleRow("rules", consumerId, providerId, capability);
        if (rule == null) {
            rule = ruleRow("revisions", consumerId, providerId, capability);
        }
        return rule != null && rule.has("revision") ? rule.get("revision").getAsLong() : 0L;
    }

    private JsonObject ruleRow(String array, String consumerId, String providerId, PolicyCapability capability) {
        if (!model.has(array)) {
            return null;
        }
        for (JsonElement element : model.getAsJsonArray(array)) {
            var row = element.getAsJsonObject();
            if (text(row, "consumer").equals(consumerId) && text(row, "provider").equals(providerId)
                    && text(row, "capability").equals(capability.name())) {
                return row;
            }
        }
        return null;
    }

    private void button(GuiGraphics graphics, int x, int y, int width, int height, String label, int mouseX, int mouseY, IntConsumer action) {
        var hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        graphics.fill(x, y, x + width, y + height, hovered ? 0xFF3A3A4C : 0xFF2B2B38);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 4, 0xFFFFFF, false);
        hits.add(new Hit(x, y, x + width, y + height, action));
    }

    private record Choice(String id, String label) {
    }

    private List<Choice> choices(String group) {
        var result = new ArrayList<Choice>();
        if (model.has(group) && model.get(group).isJsonArray()) {
            for (JsonElement element : model.getAsJsonArray(group)) {
                var choice = element.getAsJsonObject();
                result.add(new Choice(text(choice, "id"), text(choice, "label")));
            }
        }
        return result;
    }

    /** The name a player gave a network, or the short tag the guide calls "Network 082A5C". */
    private String name(String networkId) {
        if (model.has("networks")) {
            for (JsonElement element : model.getAsJsonArray("networks")) {
                var network = element.getAsJsonObject();
                if (text(network, "id").equals(networkId)) {
                    var name = text(network, "name");
                    if (!name.isBlank()) {
                        return name;
                    }
                }
            }
        }
        return "Network " + networkId.replace("-", "").substring(0, Math.min(6, networkId.replace("-", "").length())).toUpperCase(Locale.ROOT);
    }

    private static String capabilityName(PolicyCapability capability) {
        return switch (capability) {
            case STORAGE -> "Storage";
            case CRAFTING -> "Crafting";
            case ME_POWER -> "ME power";
        };
    }

    private static String text(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private void refreshModel() {
        if (parsedVersion == menu.viewVersion()) {
            return;
        }
        parsedVersion = menu.viewVersion();
        try {
            model = JsonParser.parseString(menu.view()).getAsJsonObject();
        } catch (RuntimeException malformed) {
            model = new JsonObject();
        }
    }

    private void send(String kind, String argument) {
        FederationNetwork.sendToServer(new PolicyCommandPayload(menu.containerId, kind, argument));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            for (var hit : hits) {
                if (hit.contains(mouseX, mouseY)) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    hit.action().accept(button);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
