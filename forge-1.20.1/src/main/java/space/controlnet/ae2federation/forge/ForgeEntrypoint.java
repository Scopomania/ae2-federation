package space.controlnet.ae2federation.forge;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import space.controlnet.ae2federation.CommonStartup;
import space.controlnet.ae2federation.FederationCreativeTab;
import space.controlnet.ae2federation.beam.BeamLinkService;
import space.controlnet.ae2federation.beam.BeamRegistration;
import space.controlnet.ae2federation.bridge.BridgeRegistration;
import space.controlnet.ae2federation.config.FederationServerConfig;
import space.controlnet.ae2federation.crafting.projection.CraftingProjectionService;
import space.controlnet.ae2federation.domain.FederationBindingRefresh;
import space.controlnet.ae2federation.domain.FederationDomainRegistryAccess;
import space.controlnet.ae2federation.energy.EnergySharingService;
import space.controlnet.ae2federation.forge.client.ForgeClientEntrypoint;
import space.controlnet.ae2federation.forge.network.FederationDomainPolicyActionPayloads;
import space.controlnet.ae2federation.forge.network.BundleFormPayload;
import space.controlnet.ae2federation.forge.network.ObservationPayloads;
import space.controlnet.ae2federation.material.MaterialRegistration;
import space.controlnet.ae2federation.observability.LevelObservabilityService;
import space.controlnet.ae2federation.p2p.FederationP2PRegistration;
import space.controlnet.ae2federation.power.OperatingPowerService;
import space.controlnet.ae2federation.power.PowerRegistration;
import space.controlnet.ae2federation.processing.ProcessingRegistration;
import space.controlnet.ae2federation.router.RouterRegistration;
import space.controlnet.ae2federation.storage.mount.StorageLevelLifecycle;
import space.controlnet.ae2federation.storage.mount.StorageMountService;

/** The Forge 1.20.1 entrypoint; the NeoForge 1.21 one is {@code NeoForgeEntrypoint}, and the order here follows it. */
@Mod(ForgeEntrypoint.MOD_ID)
public final class ForgeEntrypoint {
    public static final String MOD_ID = "ae2federation";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ForgeEntrypoint() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        CommonStartup.start(LOGGER);
        FederationServerConfig.register(modBus);
        BridgeRegistration.register(modBus);
        FederationP2PRegistration.register(modBus);
        RouterRegistration.register(modBus);
        ProcessingRegistration.register(modBus);
        PowerRegistration.register(modBus);
        BeamRegistration.register(modBus);
        space.controlnet.ae2federation.quantum.QuantumRegistration.register(modBus);
        MaterialRegistration.register(modBus);
        FederationCreativeTab.register(modBus);
        // Message ids follow registration order, so these stay in one place and one order on both sides.
        ObservationPayloads.register();
        FederationDomainPolicyActionPayloads.register();
        BundleFormPayload.register();
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(space.controlnet.ae2federation.crafting.projection.ReturnRecoveryCommands::register);
        bus.addListener(space.controlnet.ae2federation.command.FederationConsole::register);
        bus.addListener(space.controlnet.ae2federation.command.FederationConsole::onLoggedOut);
        bus.addListener(ForgeEntrypoint::onLevelUnload);
        bus.addListener(ForgeEntrypoint::onServerStopped);
        bus.addListener(ForgeEntrypoint::onPlayerLoggedOut);
        bus.addListener(ForgeEntrypoint::onContainerClosed);
        // Before AE2's own end-of-tick Grid ticks (NORMAL priority), which draw energy from the shared pools: like
        // AE2's Grid services, Federation's work runs once per server tick, after every level has ticked.
        bus.addListener(EventPriority.HIGH, ForgeEntrypoint::onServerTickBindings);
        bus.addListener(ForgeEntrypoint::onServerTick);
        // After AE2's end-of-tick Grid ticks, where each Grid's storage service reports its native mount changes.
        bus.addListener(EventPriority.LOWEST, ForgeEntrypoint::onServerTickNativeMounts);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientEntrypoint.init(modBus));
    }

    /**
     * First the Federation Cables that have work publish their evidence. Then binding requests from the level ticks and
     * from player actions or GameTests, then the beams and Quantum Cubes, whose links change the domains, then the
     * domains' operating power, which may stop a domain, then each service's tick. Last, the domains settle what is
     * still pending, and the cables sync what their beams show from the settled domains.
     */
    private static void onServerTickBindings(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        space.controlnet.ae2federation.router.FederationCableTicks.tickAll(event.getServer());
        FederationBindingRefresh.flushAll();
        BeamLinkService.tickAll(event.getServer());
        space.controlnet.ae2federation.quantum.QuantumLinkService.tickAll(event.getServer());
        OperatingPowerService.tick(event.getServer());
        StorageMountService.tick(event.getServer());
        CraftingProjectionService.tick(event.getServer());
        EnergySharingService.tickAll();
        FederationDomainRegistryAccess.settle(event.getServer());
        space.controlnet.ae2federation.router.FederationCableTicks.afterSettle(event.getServer());
    }

    private static void onServerTickNativeMounts(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        StorageMountService.reconcileNativeMountChanges(event.getServer());
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // Created on the first tick, not on first use, so the statistics count every second the server runs.
        LevelObservabilityService.get(event.getServer().overworld());
        LevelObservabilityService.sweepAll();
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            // The services serve the whole server: the level's nodes leave the registry, then the relationships that
            // needed them end. Each service itself goes when the server stops.
            FederationBindingRefresh.closeLevel(level);
            space.controlnet.ae2federation.router.DeviceStructureUpdater.closeLevel(level);
            space.controlnet.ae2federation.router.FederationCableTicks.closeLevel(level);
            space.controlnet.ae2federation.router.RouterTicks.closeLevel(level);
            BeamLinkService.closeLevel(level);
            space.controlnet.ae2federation.quantum.QuantumLinkService.closeLevel(level);
            var receipt = StorageLevelLifecycle.close(level);
            CraftingProjectionService.levelClosed(level);
            EnergySharingService.levelClosed(level);
            LevelObservabilityService.levelClosed(level);
            LOGGER.info("AE2F_STORAGE_LEVEL_CLOSED dimension={} servicePresentBefore={} mountedProvidersBefore={} "
                            + "mountedProvidersRemoved={} servicePersists={} dimensionNodesRemoved={} "
                            + "dimensionBridgesRemoved={} dimensionNodesLeft={}", level.dimension().location(),
                    receipt.servicePresentBefore(), receipt.mountedProvidersBefore(), receipt.mountedProvidersRemoved(),
                    receipt.servicePersists(), receipt.dimensionNodesRemoved(), receipt.dimensionBridgesRemoved(),
                    receipt.dimensionNodesLeft());
        }
    }

    /** Server-wide state outlives each level's unload, so it goes once the whole server has stopped. */
    private static void onServerStopped(ServerStoppedEvent event) {
        var server = event.getServer();
        StorageMountService.closeServer(server);
        CraftingProjectionService.closeServer(server);
        EnergySharingService.closeServer(server);
        OperatingPowerService.closeServer(server);
        space.controlnet.ae2federation.quantum.QuantumLinkService.closeServer(server);
        LevelObservabilityService.closeServer(server);
        space.controlnet.ae2federation.router.FederationCableTicks.closeServer(server);
        space.controlnet.ae2federation.router.RouterTicks.closeServer(server);
        space.controlnet.ae2federation.policy.PolicyService.closeServer(server);
        FederationDomainRegistryAccess.closeServer(server);
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LevelObservabilityService.closePlayer(event.getEntity().getUUID());
    }

    private static void onContainerClosed(PlayerContainerEvent.Close event) {
        LevelObservabilityService.closePlayer(event.getEntity().getUUID());
    }
}
