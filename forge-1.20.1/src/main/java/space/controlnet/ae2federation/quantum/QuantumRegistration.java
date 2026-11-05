package space.controlnet.ae2federation.quantum;

import space.controlnet.ae2federation.capability.CapProviders;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import space.controlnet.ae2federation.domain.port.FederationPortCapability;

public final class QuantumRegistration {
    private static final String MOD_ID = "ae2federation";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<QuantumCubeBlock> QUANTUM_CUBE = BLOCKS.register("quantum_cube", () -> new QuantumCubeBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL)));
    public static final RegistryObject<BlockItem> QUANTUM_CUBE_ITEM = ITEMS.register("quantum_cube", () -> new net.minecraft.world.item.BlockItem(QUANTUM_CUBE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<QuantumCubeBlockEntity>>
            QUANTUM_CUBE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("quantum_cube",
                    () -> BlockEntityType.Builder.of(QuantumCubeBlockEntity::new, QUANTUM_CUBE.get()).build(null));
    /** The cube an AE2 Memory Card copied: its dimension and lowest block. */

    private QuantumRegistration() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(QuantumRegistration::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        CapProviders.init();
        CapProviders.forBlockEntity(FederationPortCapability.BLOCK, QUANTUM_CUBE_BLOCK_ENTITY, 
                QuantumCubeBlockEntity::federationDomainPort);
    }
}
