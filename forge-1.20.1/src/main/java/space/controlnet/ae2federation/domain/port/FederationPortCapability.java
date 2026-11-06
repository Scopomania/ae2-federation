package space.controlnet.ae2federation.domain.port;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class FederationPortCapability {
    public static final Capability<FederationPort> BLOCK = CapabilityManager.get(new CapabilityToken<>() { });

    private FederationPortCapability() {
    }

    private static boolean registered;

    /**
     * Forge refuses a second registration of a capability type, and several registration classes ask for this one, so
     * the first request registers it and the rest do nothing (NeoForge's block capabilities were idempotent).
     */
    public static synchronized void register(RegisterCapabilitiesEvent event) {
        if (registered) {
            return;
        }
        registered = true;
        event.register(FederationPort.class);
    }
}
