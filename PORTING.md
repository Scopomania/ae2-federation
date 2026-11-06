# AE2 Federation: NeoForge 1.21.1 -> Forge 1.20.1

Target: MC 1.20.1, Forge 47.x, Java 17, AE2 15.x, LDLib **1** (official, `com.lowdragmc.lowdraglib`).
Nothing here has been compiled. Versions in `forge-1.20.1/gradle.properties` are unverified.

## Decisions
- GUI: rewrite the 21 LDLib2 files against LDLib 1 (widgets/ModularUI). Not the unofficial LDLib2 port.
- AE2 in-game Guide does not exist in AE2 15.x -> drop `client/guide/*`, `guide_*` resourcepacks, Guide*Mixin, GuidebookLevelRendererMixin.
- Compat game tests / addon scenes (`compattest`, `appfluxtest`, ...) dropped for now. Most addons have no 1.20.1 build.

## Survey (files in common/src/main + neoforge-1.21.1)
| thing | files |
|---|---|
| `net.neoforged.*` imports | 54 |
| LDLib2 | 21 |
| AE2 (`appeng.*`) | 138 |
| StreamCodec / CustomPacketPayload | 7 (+ 3 PayloadRegistrar) |
| BlockCapability / RegisterCapabilitiesEvent | 10 |
| HolderLookup.Provider (NBT save/load signature) | 21 |
| ResourceLocation.fromNamespaceAndPath/parse | 32 (-> `new ResourceLocation(ns, path)`) |
| IEventBus / ModContainer | 15 |
| Java 21 syntax (sealed/switch patterns/`when`) | 13 |
| Baked models / ModelData / renderers | ~13 |
| Mixins into AE2 internals | 12 common + 7 client (re-target against AE2 15) |

## Order
1. Scaffold (this) + verify versions, get an empty mod loading.
2. Pure logic packages with no MC/AE2 deps first: `beam`, `bundle`, `domain`, `policy`, `quantum`, `identity`.
3. Registries, block entities, NBT (`HolderLookup.Provider` -> plain `CompoundTag` save/load), SimpleChannel networking replaces payloads.
4. Capabilities: BlockCapability -> `ICapabilityProvider` / `LazyOptional`.
5. AE2 15 API: storage, crafting, pattern provider hooks, power; retarget mixins.
6. GUI on LDLib 1.
7. Renderers (BakedModel/ModelData differ on Forge 1.20.1), beams, cables.
8. Jade, lang, assets.

## Progress
Compiled against the real Forge 1.20.1 + AE2 15.4.10 + LDLib 1 classes (javac -source 17), not just Java syntax.

- 0002: 170 files with no MC/AE2/NeoForge imports.
- 0008: removed Java 21-only calls (List.getFirst/getLast, Math.TAU); scripts/check17 scans bytecode for more.
- 0009: +8 SavedData classes (SavedData.Factory -> computeIfAbsent(load, new, name), save/load without HolderLookup.Provider,
  AEKey NBT without provider, DataResult.getOrThrow with the 1.20.1 signature). 294 files compile clean.
- 0016: mixin config (8 common mixins; the client `MouseHandlerAccess` stays out until a refmap/AT exists), `[[mixins]]` in
  mods.toml, MixinExtras jar-in-jar, `forge-1.20.1/smoke-server.sh` (headless dedicated-server boot test).
  No Sponge gradle plugin and no refmap: the mixins only name AE2 members, which are not obfuscated.
- 0015: Forge entrypoints (ForgeEntrypoint @Mod, ForgeClientEntrypoint via DistExecutor), SimpleChannel networking
  (FederationNetwork + the 7 payload records converted from StreamCodec to encode/decode; registration order fixes the ids).
  Compiles; the `Dist` class is stubbed in my scratch compile only (it is in a Forge jar I do not have), so the Codespace
  build is the real check for that one import.
- 0014: the rest of the non-GUI tree: pattern-provider layer on AE2 15, GlobalCraftingProviders, client rendering
  (RenderStates shim, 1.20.1 vertex API, QuadBaker adapter), block entities, registries, 6 mixins (not registered yet).
  454 files compile together on a Java 17 language level against the real Forge/AE2 15/LDLib 1 classes; bytecode scan finds
  no Java 18+ API.
- 0012: small blockers (tick events -> TickEvent.LevelTickEvent, BlockPos.min/max helper, 1.20.1 shouldTickBlocksAt),
  QuantumCardData (card link as NBT instead of a data component), policy menu stub, wrench tag from AE2 15 ConventionTags.
- 0011: capability layer (CapProviders attach rules, Caps lookup/cache stand-in, ItemInteractionResult shim).
- 0010: FederationServerConfig on Forge (ModLoadingContext.registerConfig, ForgeConfigSpec); FML jars added to my compile classpath.
- 0007: +116 files. Mechanical NeoForge->Forge remap (IEventBus, IItemHandler, IFluidHandler, FluidStack, Tags, events,
  ModelData, ResourceLocation factories, ...). 286 files now compile clean together.

Real size of what is left (correcting an earlier overestimate that counted tests): 482 main files outside `client/guide`.
- 286 compile clean (done)
- 150 need real porting (registries/DeferredRegister -> Forge RegistryObject, capabilities -> LazyOptional,
  ItemInteractionResult, 1.21 block/BE APIs, rendering: ByteBufferBuilder / RenderStateShard / baked models)
- 46 deferred: 20 use LDLib2 (GUI rewrite on LDLib 1), 27 mixins / Jade (re-target against AE2 15; Jade not a dependency yet)
- + 12 NeoForge entrypoint/network files in neoforge-1.21.1 to redo as Forge SimpleChannel + @Mod entrypoint

## What exists, and what does not (honest status)
Compiles, never run. Nothing here has been launched in Minecraft.
- NOT done: the `@Mod` entrypoint (neoforge-1.21.1 `NeoForgeEntrypoint` is the blueprint), the network packets (10+ payloads ->
  SimpleChannel), registering the capability types and `CapProviders` on the buses, the mixin config + MDG mixin setup + jarJar of
  MixinExtras, the client mixin `MouseHandlerAccess` (needs an access transformer or refmap), and every datagen/asset check.
- NOT ported: all LDLib2 GUI files (20, client/menu + client/policy views) -> rewrite on LDLib 1; Jade tooltips (Jade not a dependency
  yet); AE2 guide integration (AE2 15 has no guide); Data Energistics compat (no 1.20.1 build); tests.
- Stubs that stand in for the GUI: `client/menu/FederationDomainPolicyMenu` (opens nothing), `FederationScreenSwitch` (provider screen only).
- Runtime risks to check first: capability registration timing, `GlobalCraftingProviders` reflection against AE2 15.4.10, the 6 mixin
  targets (verified to exist statically only), render types registering without RegisterRenderBuffersEvent.
