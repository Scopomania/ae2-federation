package space.controlnet.ae2federation.power;

import space.controlnet.ae2federation.capability.CapProviders;
import net.minecraft.core.registries.BuiltInRegistries;
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

public final class PowerRegistration {
    private static final String MOD_ID = "ae2federation";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<BeamConverterBlock> BEAM_CONVERTER = BLOCKS.register("beam_converter", () -> new BeamConverterBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL)));
    public static final RegistryObject<BlockItem> BEAM_CONVERTER_ITEM = ITEMS.register("beam_converter", () -> new net.minecraft.world.item.BlockItem(BEAM_CONVERTER.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<BeamConverterBlockEntity>>
            BEAM_CONVERTER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("beam_converter",
                    () -> BlockEntityType.Builder.of(BeamConverterBlockEntity::new, BEAM_CONVERTER.get()).build(null));

    private PowerRegistration() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(PowerRegistration::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        CapProviders.init();
        var type = BEAM_CONVERTER_BLOCK_ENTITY;
        CapProviders.forBlockEntity(FederationPortCapability.BLOCK, type,  BeamConverterBlockEntity::federationDomainPort);
        CapProviders.forBlockEntity(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, type,  BeamConverterBlockEntity::energy);
    }
}
