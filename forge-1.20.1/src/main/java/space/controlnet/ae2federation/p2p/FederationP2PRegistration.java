package space.controlnet.ae2federation.p2p;

import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import space.controlnet.ae2federation.compat.jade.P2PCapacityTooltip;
import space.controlnet.ae2federation.domain.port.FederationPortCapability;

public final class FederationP2PRegistration {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "ae2federation");
    public static final RegistryObject<PartItem<FederationP2PTunnelPart>> TUNNEL = ITEMS.register(
            "federation_p2p_tunnel", () -> new PartItem<>(new Item.Properties(), FederationP2PTunnelPart.class,
                    FederationP2PTunnelPart::new));

    private FederationP2PRegistration() {
    }

    public static void register(IEventBus modBus) {
        PartModels.registerModels(FederationP2PTunnelPart.FRONT_MODEL);
        P2PCapacityTooltip.register();
        ITEMS.register(modBus);
        // Like AE2's own tunnels, a P2P tunnel attuned with an item of the tunnel's tag becomes this tunnel; the tag
        // lists the Federation cable.
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> P2PTunnelAttunement.registerAttunementTag(TUNNEL.get())));
    }
}
