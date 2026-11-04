package space.controlnet.ae2federation.material;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Crafting materials, made the way AE2 makes its processors and cores: the Nexus Press prints an Ender Pearl into a
 * Printed Nexus Circuit, pressed with redstone and Printed Silicon into a Nexus Processor, crafted into the Nexus Cores
 * that go into the devices.
 */
public final class MaterialRegistration {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "ae2federation");

    public static final RegistryObject<Item> NEXUS_CORE = ITEMS.register("nexus_core", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NEXUS_PROCESSOR = ITEMS.register("nexus_processor", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NEXUS_PROCESSOR_PRESS = ITEMS.register("nexus_processor_press", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PRINTED_NEXUS_CIRCUIT = ITEMS.register("printed_nexus_circuit", () -> new Item(new Item.Properties()));

    private MaterialRegistration() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
