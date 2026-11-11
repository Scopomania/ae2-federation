package space.controlnet.ae2federation.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import space.controlnet.ae2federation.client.policy.FederationDomainPolicySession;
import space.controlnet.ae2federation.domain.FederationDomainRegistryAccess;
import space.controlnet.ae2federation.processing.endpoint.EndpointBlockEntity;
import space.controlnet.ae2federation.processing.provider.FederationPatternProviderBlockEntity;
import space.controlnet.ae2federation.router.RouterBlockEntity;

/**
 * {@code /ae2federation}: the Federation Domain policy editor as commands, until its screens are ported. It drives the same
 * {@link FederationDomainPolicySession} the 1.21 screens did, so a rule set here is the rule the screens will show later.
 *
 * <pre>
 * /ae2federation domains                         every Federation Domain in the level (also works from the server console)
 * /ae2federation open &lt;pos&gt;                     start editing the Domain of the Router, Provider or Endpoint at pos
 * /ae2federation show                            what the editor shows now
 * /ae2federation next consumer|provider|capability
 * /ae2federation toggle                          turn the selected rule on or off
 * /ae2federation mapping next provider|slot|lane, /ae2federation mapping toggle
 * /ae2federation endpoint next|release|cancel
 * /ae2federation close
 * </pre>
 * Like the screens, a session ends when the player walks away from the block it was opened on.
 */
public final class FederationConsole {
    private static final Map<UUID, FederationDomainPolicySession> SESSIONS = new ConcurrentHashMap<>();

    private FederationConsole() {
    }

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ae2federation")
                .then(Commands.literal("domains").executes(context -> domains(context.getSource())))
                .then(Commands.literal("open").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> open(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                .then(Commands.literal("ports").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> ports(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                .then(Commands.literal("show").executes(context -> act(context, null, false)))
                .then(Commands.literal("close").executes(FederationConsole::close))
                .then(Commands.literal("toggle").executes(context -> act(context, FederationDomainPolicySession::toggleEnabled, true)))
                .then(Commands.literal("next")
                        .then(Commands.literal("consumer").executes(context -> act(context, FederationDomainPolicySession::nextConsumer, false)))
                        .then(Commands.literal("provider").executes(context -> act(context, FederationDomainPolicySession::nextProvider, false)))
                        .then(Commands.literal("capability").executes(context -> act(context, FederationDomainPolicySession::nextCapability, false)))
                )
                .then(Commands.literal("mapping")
                        .then(Commands.literal("toggle").executes(context -> act(context, FederationDomainPolicySession::toggleMapping, false)))
                        .then(Commands.literal("next")
                                .then(Commands.literal("provider").executes(context -> act(context, FederationDomainPolicySession::nextMappingProvider, false)))
                                .then(Commands.literal("slot").executes(context -> act(context, FederationDomainPolicySession::nextMappingSlot, false)))
                                .then(Commands.literal("lane").executes(context -> act(context, FederationDomainPolicySession::nextMappingLane, false)))))
                .then(Commands.literal("endpoint")
                        .then(Commands.literal("next").executes(context -> act(context, FederationDomainPolicySession::nextEndpoint, false)))
                        .then(Commands.literal("release").executes(context -> act(context, FederationDomainPolicySession::releaseEndpoint, false)))
                        .then(Commands.literal("cancel").executes(context -> act(context, FederationDomainPolicySession::cancelRelease, false)))));
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    private static int domains(CommandSourceStack source) {
        var domains = FederationDomainRegistryAccess.get(source.getLevel()).federationDomains();
        source.sendSuccess(() -> Component.literal("Federation Domains in this level: " + domains.size()), false);
        for (var domain : domains) {
            source.sendSuccess(() -> Component.literal(" - " + domain.federationDomainId() + ": " + domain.memberships().size()
                    + " network(s), " + domain.nodes().size() + " node(s)"), false);
        }
        return domains.size();
    }

    /**
     * What each face of a Router or Switch is bound to, and what its boundary node (the one an AE2 cable connects to) is
     * joined with. For chasing a face that connects and drops again: run it before and after, and compare.
     */
    private static int ports(CommandSourceStack source, BlockPos position) {
        if (!(source.getLevel().getBlockEntity(position) instanceof RouterBlockEntity router)) {
            source.sendFailure(Component.literal("No Federation Router or Switch at " + position.toShortString()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Faces of the block at " + position.toShortString() + ":"), false);
        for (var face : net.minecraft.core.Direction.values()) {
            var binding = router.bindings().get(face);
            var node = router.boundaryNode(face);
            var line = new StringBuilder(" ").append(face.getSerializedName()).append(": ")
                    .append(binding == null ? "none" : binding.getClass().getSimpleName());
            if (node == null) {
                line.append(", no node");
            } else {
                line.append(", node online=").append(node.isOnline()).append(" active=").append(node.isActive())
                        .append(", grid of ").append(node.getGrid().size()).append(" node(s), connections=")
                        .append(node.getConnections().size());
                for (var connection : node.getConnections()) {
                    var other = connection.getOtherSide(node);
                    var owner = other.getOwner();
                    line.append(" [-> ").append(owner.getClass().getSimpleName());
                    if (owner instanceof net.minecraft.world.level.block.entity.BlockEntity entity) {
                        line.append(" at ").append(entity.getBlockPos().toShortString());
                    } else if (owner instanceof appeng.parts.AEBasePart part && part.getBlockEntity() != null) {
                        line.append(" at ").append(part.getBlockEntity().getBlockPos().toShortString());
                    }
                    line.append(']');
                }
            }
            source.sendSuccess(() -> Component.literal(line.toString()), false);
        }
        return 1;
    }

    private static int open(CommandSourceStack source, BlockPos position) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var entity = player.serverLevel().getBlockEntity(position);
        FederationDomainPolicySession session;
        if (entity instanceof RouterBlockEntity) {
            session = FederationDomainPolicySession.forRouter(player, position);
        } else if (entity instanceof FederationPatternProviderBlockEntity || entity instanceof EndpointBlockEntity) {
            session = FederationDomainPolicySession.forDevice(player, position);
        } else {
            source.sendFailure(Component.literal("Nothing to edit at " + position.toShortString()
                    + ": open a Federation Router, Pattern Provider or Processing Endpoint."));
            return 0;
        }
        SESSIONS.put(player.getUUID(), session);
        source.sendSuccess(() -> Component.literal("Opened the Federation editor at " + position.toShortString()), false);
        return show(source, session);
    }

    private static int close(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var removed = SESSIONS.remove(context.getSource().getPlayerOrException().getUUID());
        context.getSource().sendSuccess(() -> Component.literal(removed == null ? "No editor was open." : "Closed the editor."), false);
        return removed == null ? 0 : 1;
    }

    private static int act(CommandContext<CommandSourceStack> context, Consumer<FederationDomainPolicySession> action,
            boolean edits) throws CommandSyntaxException {
        var source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        var session = SESSIONS.get(player.getUUID());
        if (session == null) {
            source.sendFailure(Component.literal("Nothing is open. Use /ae2federation open <x y z> on a Router, Provider or Endpoint."));
            return 0;
        }
        if (!session.isStillValid(player)) {
            SESSIONS.remove(player.getUUID());
            source.sendFailure(Component.literal("The editor closed: you are too far from the block, or it is gone."));
            return 0;
        }
        if (action != null) {
            if (edits && !session.canSubmit()) {
                // The editor is read-only until its Domain is confirmed; the Status line says why.
                source.sendFailure(Component.literal("The editor is not accepting rule changes right now."));
            } else {
                action.accept(session);
            }
        }
        return show(source, session);
    }

    private static int show(CommandSourceStack source, FederationDomainPolicySession session) {
        send(source, "Members", session.membersText());
        send(source, "Consumer", session.consumerText());
        send(source, "Provider", session.providerText());
        send(source, "Rule", session.ruleText());
        send(source, "Status", session.statusText());
        send(source, "Mapping", session.mappingStatusText());
        source.sendSuccess(() -> Component.literal("Power: " + session.powerGauge()), false);
        var overview = session.networkOverviewText();
        if (!overview.isBlank()) {
            source.sendSuccess(() -> Component.literal("Networks: " + overview), false);
        }
        return 1;
    }

    private static void send(CommandSourceStack source, String label, Component value) {
        source.sendSuccess(() -> Component.literal(label + ": ").append(value), false);
    }
}
