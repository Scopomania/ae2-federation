package space.controlnet.ae2federation.forge.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;
import space.controlnet.ae2federation.bundle.BundleInteraction;
import space.controlnet.ae2federation.client.BeamFormerRenderer;
import space.controlnet.ae2federation.client.BundleSelectionClient;
import space.controlnet.ae2federation.client.CableBakedModel;
import space.controlnet.ae2federation.client.CableFlowRenderer;
import space.controlnet.ae2federation.client.ClientStartup;
import space.controlnet.ae2federation.client.DeviceStructureBakedModel;
import space.controlnet.ae2federation.client.FederationScreenSwitch;
import space.controlnet.ae2federation.client.RefinedDeviceBakedModel;
import space.controlnet.ae2federation.client.RefinedDeviceRenderer;
import space.controlnet.ae2federation.client.WorldHighlight;
import space.controlnet.ae2federation.forge.network.FederationNetwork;
import space.controlnet.ae2federation.forge.network.ObservationPayloads;
import space.controlnet.ae2federation.forge.network.ProviderMenuNavigationPayload;
import space.controlnet.ae2federation.processing.provider.FederationPatternProviderBlockEntity;

/**
 * Client-only wiring. Only {@code ForgeEntrypoint} reaches this, through {@code DistExecutor}, so a dedicated server
 * never loads it. The guide example packs and the LDLib2 theme resource hooks of the 1.21 entrypoint have no 1.20.1 form.
 */
public final class ForgeClientEntrypoint {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgeClientEntrypoint() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(CableBakedModel::bake);
        modBus.addListener(DeviceStructureBakedModel::bake);
        modBus.addListener(RefinedDeviceBakedModel::bake);
        modBus.addListener((net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                RefinedDeviceRenderer.register(event));
        modBus.addListener(CableFlowRenderer::register);
        modBus.addListener(BeamFormerRenderer::register);
        ClientStartup.start(LOGGER);
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(ForgeClientEntrypoint::onLoggingOut);
        bus.addListener(ForgeClientEntrypoint::onScreenInit);
        bus.addListener(WorldHighlight::render);
        bus.addListener(CableFlowRenderer::flush);
        BundleInteraction.registerClientClick(BundleSelectionClient::click);
        bus.addListener(BundleSelectionClient::tick);
        bus.addListener(BundleSelectionClient::render);
        bus.addListener(BundleSelectionClient::onRightClickEmpty);
        bus.addListener(FederationScreenSwitch::onClosing);
        bus.addListener(FederationScreenSwitch::onInit);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof appeng.client.gui.implementations.PatternProviderScreen<?> screen
                && screen.getMenu().getBlockEntity() instanceof FederationPatternProviderBlockEntity) {
            event.addListener(Button.builder(Component.translatable("ae2federation.ui.workspace.open_mapping"),
                            button -> FederationNetwork.sendToServer(
                                    new ProviderMenuNavigationPayload(screen.getMenu().containerId, false)))
                    .bounds(screen.getGuiLeft() + 28, screen.getGuiTop() - 22, 120, 20).build());
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ObservationPayloads.clientStates().reset();
    }
}
