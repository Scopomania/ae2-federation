package space.controlnet.ae2federation.router;

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
import space.controlnet.ae2federation.domain.port.FederationPort;
import space.controlnet.ae2federation.domain.port.FederationPortCapability;

public final class RouterRegistration {
    private static final String MOD_ID = "ae2federation";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<RouterBlock> ROUTER = BLOCKS.register("router", () -> new RouterBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL)));
    /** Shares the Router's block and block entity code; see {@link RouterBlock}. */
    public static final RegistryObject<RouterBlock> SWITCH = BLOCKS.register("switch", () -> new RouterBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL)));
    public static final RegistryObject<FederationCableBlock> FEDERATION_CABLE = BLOCKS.register("cable", () -> new FederationCableBlock(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.GLASS).noOcclusion()));
    public static final RegistryObject<BlockItem> ROUTER_ITEM = ITEMS.register("router", () -> new net.minecraft.world.item.BlockItem(ROUTER.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> SWITCH_ITEM = ITEMS.register("switch", () -> new net.minecraft.world.item.BlockItem(SWITCH.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> FEDERATION_CABLE_ITEM = ITEMS.register("cable", () -> new net.minecraft.world.item.BlockItem(FEDERATION_CABLE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<RouterBlockEntity>> ROUTER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("router", () -> BlockEntityType.Builder.of(RouterBlockEntity::new, ROUTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<RouterBlockEntity>> SWITCH_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("switch", () -> BlockEntityType.Builder.of(RouterBlockEntity::new, SWITCH.get()).build(null));
    public static final RegistryObject<BlockEntityType<FederationCableBlockEntity>>
            FEDERATION_CABLE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("cable",
                    () -> BlockEntityType.Builder.of(FederationCableBlockEntity::new, FEDERATION_CABLE.get()).build(null));

    private RouterRegistration() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(RouterRegistration::registerCapabilities);
        // Before AE2's end-of-level device ticks (NORMAL priority), as the Routers' block entity ticks were.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGH,
                RouterTicks::onLevelTick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(DeviceStructureUpdater::onLevelTick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                space.controlnet.ae2federation.bundle.BundleInteraction::onRightClickBlock);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(WrenchDismantle::onRightClickBlock);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        CapProviders.init();
        FederationPortCapability.register(event);
        for (var type : java.util.List.of(ROUTER_BLOCK_ENTITY, SWITCH_BLOCK_ENTITY)) {
            CapProviders.forBlockEntity(appeng.capabilities.Capabilities.IN_WORLD_GRID_NODE_HOST, type,  (router, context) -> router);
            CapProviders.forBlockEntity(FederationPortCapability.BLOCK, type,  RouterBlockEntity::federationDomainPort);
        }
        CapProviders.forBlocks(FederationPortCapability.BLOCK, (level, position, state, blockEntity, face) -> face == null
                        || level instanceof net.minecraft.server.level.ServerLevel server
                                && !space.controlnet.ae2federation.bundle.BundleContacts.open(server, position, face)
                        ? null
                        : new FederationPort(position, face), FEDERATION_CABLE);
    }
}
