package space.controlnet.ae2federation.beam;

import space.controlnet.ae2federation.capability.CapProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import space.controlnet.ae2federation.domain.port.FederationPortCapability;

public final class BeamRegistration {
    private static final String MOD_ID = "ae2federation";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<BeamFormerBlock> BEAM_FORMER = BLOCKS.register("beam_former", () -> new BeamFormerBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL)));
    public static final RegistryObject<BlockItem> BEAM_FORMER_ITEM = ITEMS.register("beam_former", () -> new net.minecraft.world.item.BlockItem(BEAM_FORMER.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<BeamFormerBlockEntity>>
            BEAM_FORMER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("beam_former",
                    () -> BlockEntityType.Builder.of(BeamFormerBlockEntity::new, BEAM_FORMER.get()).build(null));

    private BeamRegistration() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(BeamRegistration::registerCapabilities);
        // A hint only: the scan finds every change, including those no event reports.
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.NeighborNotifyEvent event) -> BeamLinkService.hint(event.getLevel(), event.getPos()));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        CapProviders.init();
        CapProviders.forBlockEntity(FederationPortCapability.BLOCK, BEAM_FORMER_BLOCK_ENTITY, 
                BeamFormerBlockEntity::federationDomainPort);
    }
}
