package space.controlnet.ae2federation.ae2;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingService;
import appeng.me.service.CraftingService;
import appeng.me.service.helpers.NetworkCraftingProviders;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import space.controlnet.ae2federation.crafting.projection.CraftingProjectionService;

/**
 * Crafting providers that belong to a Grid's crafting service but to no Grid node. AE2 19 has this on
 * {@code ICraftingService}; AE2 15 keeps every provider under a node, so this registers the provider the way
 * {@link NetworkCraftingProviders#addProvider} does, without the node.
 *
 * <p>AE2 15's {@code ProviderState} is node-independent: it snapshots a provider's patterns and priority on
 * construction and mounts itself into the lookup maps. Only the map that owns it is keyed by node, so a state mounted
 * here is found by the same pattern lookups as a node's. A refresh unmounts the old state and mounts a new snapshot,
 * which is what {@code CraftingService.refreshNodeCraftingProvider} does for a node.
 *
 * <p>Registrations belong to the native Grid and are forgotten with it (weak keys).
 */
public final class GlobalCraftingProviders {
    private record Entry(Object state, MinecraftServer server, IGrid grid) {
    }

    private static final Map<NetworkCraftingProviders, Map<ICraftingProvider, Entry>> REGISTERED = new WeakHashMap<>();

    private static final Field SERVICE_PROVIDERS;
    private static final Field SERVICE_GRID;
    private static final Constructor<?> STATE_NEW;
    private static final Method STATE_MOUNT;
    private static final Method STATE_UNMOUNT;
    private static final Method TOUCH;

    static {
        try {
            SERVICE_PROVIDERS = open(CraftingService.class.getDeclaredField("craftingProviders"));
            SERVICE_GRID = open(CraftingService.class.getDeclaredField("grid"));
            var state = Class.forName("appeng.me.service.helpers.NetworkCraftingProviders$ProviderState");
            STATE_NEW = open(state.getDeclaredConstructor(ICraftingProvider.class));
            STATE_MOUNT = open(state.getDeclaredMethod("mount", NetworkCraftingProviders.class));
            STATE_UNMOUNT = open(state.getDeclaredMethod("unmount", NetworkCraftingProviders.class));
            TOUCH = open(NetworkCraftingProviders.class.getDeclaredMethod("setLastModifiedOnTick"));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError("AE2's crafting provider internals changed: " + e);
        }
    }

    private GlobalCraftingProviders() {
    }

    public static synchronized void add(ICraftingService service, ICraftingProvider provider) {
        var providers = providers(service);
        var registered = REGISTERED.computeIfAbsent(providers, key -> new IdentityHashMap<>());
        if (registered.containsKey(provider)) {
            refresh(service, provider);
            return;
        }
        var grid = grid(service);
        registered.put(provider, new Entry(mount(providers, provider), server(grid), grid));
    }

    public static synchronized void remove(ICraftingService service, ICraftingProvider provider) {
        var providers = providers(service);
        var registered = REGISTERED.get(providers);
        var entry = registered == null ? null : registered.remove(provider);
        if (entry == null) {
            return;
        }
        unmount(providers, entry.state());
        if (entry.server() != null) {
            CraftingProjectionService.globalProviderRemoved(entry.server(), entry.grid(), provider);
        }
    }

    public static synchronized void refresh(ICraftingService service, ICraftingProvider provider) {
        var providers = providers(service);
        var registered = REGISTERED.get(providers);
        var old = registered == null ? null : registered.get(provider);
        if (old == null) {
            add(service, provider);
            return;
        }
        unmount(providers, old.state());
        registered.put(provider, new Entry(mount(providers, provider), old.server(), old.grid()));
    }

    /** The providers registered globally on this Grid's crafting service. */
    public static synchronized java.util.List<ICraftingProvider> registered(ICraftingService service) {
        var registered = REGISTERED.get(providers(service));
        return registered == null ? java.util.List.of() : java.util.List.copyOf(registered.keySet());
    }

    public static synchronized boolean contains(ICraftingService service, ICraftingProvider provider) {
        var registered = REGISTERED.get(providers(service));
        return registered != null && registered.containsKey(provider);
    }

    private static Object mount(NetworkCraftingProviders providers, ICraftingProvider provider) {
        try {
            var state = STATE_NEW.newInstance(provider);
            STATE_MOUNT.invoke(state, providers);
            TOUCH.invoke(providers);
            return state;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not mount a global crafting provider", e);
        }
    }

    private static void unmount(NetworkCraftingProviders providers, Object state) {
        try {
            STATE_UNMOUNT.invoke(state, providers);
            TOUCH.invoke(providers);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not unmount a global crafting provider", e);
        }
    }

    private static NetworkCraftingProviders providers(ICraftingService service) {
        try {
            return (NetworkCraftingProviders) SERVICE_PROVIDERS.get(service);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static IGrid grid(ICraftingService service) {
        try {
            return (IGrid) SERVICE_GRID.get(service);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static @Nullable MinecraftServer server(IGrid grid) {
        var pivot = grid.getPivot();
        return pivot == null ? null : pivot.getLevel().getServer();
    }

    private static <T extends java.lang.reflect.AccessibleObject> T open(T member) {
        member.setAccessible(true);
        return member;
    }
}
