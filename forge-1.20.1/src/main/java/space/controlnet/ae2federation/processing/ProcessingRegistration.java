package space.controlnet.ae2federation.processing;

import space.controlnet.ae2federation.capability.CapProviders;
import appeng.core.definitions.AEBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import space.controlnet.ae2federation.processing.endpoint.EndpointBlock;
import space.controlnet.ae2federation.processing.endpoint.EndpointBlockEntity;
import space.controlnet.ae2federation.processing.endpoint.EndpointTargetBinding;
import space.controlnet.ae2federation.processing.endpoint.EndpointTargetCapability;
import space.controlnet.ae2federation.domain.port.FederationPortCapability;
import space.controlnet.ae2federation.processing.provider.FederationPatternProviderBlock;
import space.controlnet.ae2federation.processing.provider.FederationPatternProviderBlockEntity;
import space.controlnet.ae2federation.processing.provider.ProviderEnergyRelay;
import space.controlnet.ae2federation.processing.provider.ProviderUpgradeCards;

public final class ProcessingRegistration {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "ae2federation");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "ae2federation");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "ae2federation");
    public static final RegistryObject<EndpointBlock> ENDPOINT = BLOCKS.register("processing_endpoint", () -> new EndpointBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL)));
    public static final RegistryObject<BlockItem> ENDPOINT_ITEM = ITEMS.register("processing_endpoint", () -> new net.minecraft.world.item.BlockItem(ENDPOINT.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<EndpointBlockEntity>> ENDPOINT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("processing_endpoint", () -> {
                var type = BlockEntityType.Builder.of(EndpointBlockEntity::new, ENDPOINT.get()).build(null);
                ENDPOINT.get().setBlockEntity(EndpointBlockEntity.class, type, null, EndpointBlockEntity::serverTick);
                return type;
            });

    public static final RegistryObject<FederationPatternProviderBlock> PROVIDER = BLOCKS.register("pattern_provider", () -> new FederationPatternProviderBlock(BlockBehaviour.Properties.of().strength(2.2F, 11.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistryObject<BlockItem> PROVIDER_ITEM = ITEMS.register("pattern_provider", () -> new net.minecraft.world.item.BlockItem(PROVIDER.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<FederationPatternProviderBlockEntity>>
            PROVIDER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("pattern_provider", () -> {
                var type = BlockEntityType.Builder.of(FederationPatternProviderBlockEntity::new, PROVIDER.get())
                        .build(null);
                PROVIDER.get().setBlockEntity(FederationPatternProviderBlockEntity.class, type, null,
                        FederationPatternProviderBlockEntity::serverTick);
                return type;
            });

    private ProcessingRegistration() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(ProcessingRegistration::registerCapabilities);
        // Addons register their cards in their own setup, whose order among mods is not fixed; load complete is after all.
        modBus.addListener((FMLLoadCompleteEvent event) -> event.enqueueWork(() -> ProviderUpgradeCards.inherit(PROVIDER_ITEM.get())));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        CapProviders.init();
        FederationPortCapability.register(event);
        EndpointTargetCapability.register(event);
        CapProviders.forBlocks(EndpointTargetCapability.BLOCK, (level, position, state, blockEntity, side) -> level instanceof ServerLevel serverLevel && side != null
                        ? EndpointTargetBinding.find(serverLevel, position, side)
                        : null, () -> AEBlocks.INTERFACE.block(), ENDPOINT);
        CapProviders.forBlockEntity(appeng.capabilities.Capabilities.IN_WORLD_GRID_NODE_HOST, ENDPOINT_BLOCK_ENTITY, 
                (endpoint, context) -> endpoint);
        CapProviders.forBlockEntity(appeng.capabilities.Capabilities.IN_WORLD_GRID_NODE_HOST, PROVIDER_BLOCK_ENTITY, 
                (provider, context) -> provider);
        CapProviders.forBlockEntity(FederationPortCapability.BLOCK, PROVIDER_BLOCK_ENTITY, 
                FederationPatternProviderBlockEntity::federationPort);
        CapProviders.forBlockEntity(FederationPortCapability.BLOCK, ENDPOINT_BLOCK_ENTITY, 
                EndpointBlockEntity::federationPort);
        // Local input arrives on the Federation face from a native Provider; the runtime gates every face.
        CapProviders.forBlockEntity(appeng.capabilities.Capabilities.STORAGE, ENDPOINT_BLOCK_ENTITY, 
                (endpoint, side) -> side == null || endpointRuntime(endpoint) == null ? null
                        : endpointRuntime(endpoint).inputStorage(side).orElse(null));
        CapProviders.forBlockEntity(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, ENDPOINT_BLOCK_ENTITY, 
                (endpoint, side) -> endpointRuntime(endpoint, side) == null ? null
                        : endpointRuntime(endpoint, side).itemReturn(side).orElse(null));
        CapProviders.forBlockEntity(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER, ENDPOINT_BLOCK_ENTITY, 
                (endpoint, side) -> endpointRuntime(endpoint, side) == null ? null
                        : endpointRuntime(endpoint, side).fluidReturn(side).orElse(null));
        // FE a Provider sends out of its Federation face goes on to the machines at its Endpoints, whatever sends it.
        CapProviders.forBlocks(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, (level, position, state, blockEntity, side) -> ProviderEnergyRelay.facing(level, position, side), ENDPOINT, space.controlnet.ae2federation.router.RouterRegistration.ROUTER, space.controlnet.ae2federation.router.RouterRegistration.SWITCH, space.controlnet.ae2federation.router.RouterRegistration.FEDERATION_CABLE);
        // As on AE2's own Pattern Provider; Applied Mekanistics and other addons wrap it for their resource types.
        CapProviders.forBlockEntity(appeng.capabilities.Capabilities.GENERIC_INTERNAL_INV, ENDPOINT_BLOCK_ENTITY, 
                (endpoint, side) -> endpointRuntime(endpoint, side) == null ? null
                        : endpointRuntime(endpoint, side).genericReturn(side).orElse(null));
    }

    private static space.controlnet.ae2federation.processing.endpoint.EndpointRuntime endpointRuntime(
            EndpointBlockEntity endpoint) {
        if (!(endpoint.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        var binding = EndpointTargetBinding.findEndpoint(level, endpoint.getBlockPos());
        return binding == null ? null : binding.runtime();
    }

    private static space.controlnet.ae2federation.processing.endpoint.EndpointRuntime endpointRuntime(
            EndpointBlockEntity endpoint, net.minecraft.core.Direction side) {
        if (!(endpoint.getLevel() instanceof ServerLevel level) || side == null
                || side == endpoint.federationFace()) {
            return null;
        }
        var binding = EndpointTargetBinding.findEndpoint(level, endpoint.getBlockPos());
        return binding == null ? null : binding.runtime();
    }
}
