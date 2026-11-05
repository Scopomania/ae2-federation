package space.controlnet.ae2federation;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import space.controlnet.ae2federation.beam.BeamRegistration;
import space.controlnet.ae2federation.bridge.BridgeRegistration;
import space.controlnet.ae2federation.material.MaterialRegistration;
import space.controlnet.ae2federation.p2p.FederationP2PRegistration;
import space.controlnet.ae2federation.power.PowerRegistration;
import space.controlnet.ae2federation.router.RouterRegistration;
import space.controlnet.ae2federation.processing.ProcessingRegistration;

public final class FederationCreativeTab {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "ae2federation");

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ae2federation.main"))
                    .icon(() -> RouterRegistration.ROUTER_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(RouterRegistration.ROUTER_ITEM.get());
                        output.accept(RouterRegistration.SWITCH_ITEM.get());
                        output.accept(RouterRegistration.FEDERATION_CABLE_ITEM.get());
                        output.accept(ProcessingRegistration.PROVIDER_ITEM.get());
                        output.accept(ProcessingRegistration.ENDPOINT_ITEM.get());
                        output.accept(PowerRegistration.BEAM_CONVERTER_ITEM.get());
                        output.accept(BeamRegistration.BEAM_FORMER_ITEM.get());
                        output.accept(space.controlnet.ae2federation.quantum.QuantumRegistration.QUANTUM_CUBE_ITEM.get());
                        output.accept(BridgeRegistration.BRIDGE.get());
                        output.accept(MaterialRegistration.NEXUS_PROCESSOR_PRESS.get());
                        output.accept(MaterialRegistration.PRINTED_NEXUS_CIRCUIT.get());
                        output.accept(FederationP2PRegistration.TUNNEL.get());
                        output.accept(MaterialRegistration.NEXUS_PROCESSOR.get());
                        output.accept(MaterialRegistration.NEXUS_CORE.get());
                    })
                    .build());

    private FederationCreativeTab() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
