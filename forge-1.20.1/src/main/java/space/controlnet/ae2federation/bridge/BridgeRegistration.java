package space.controlnet.ae2federation.bridge;

import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BridgeRegistration {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "ae2federation");
    public static final RegistryObject<PartItem<MultipartBridgePart>> BRIDGE = ITEMS.register(
            "bridge", () -> new PartItem<>(new Item.Properties(), MultipartBridgePart.class,
                    MultipartBridgePart::new));

    private BridgeRegistration() {
    }

    public static void register(IEventBus modBus) {
        PartModels.registerModels(MultipartBridgePart.MODEL);
        ITEMS.register(modBus);
    }
}
