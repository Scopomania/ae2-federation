package space.controlnet.ae2federation.processing.endpoint;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class EndpointTargetCapability {
    public static final Capability<EndpointTargetAccess> BLOCK = CapabilityManager.get(new CapabilityToken<>() { });

    private EndpointTargetCapability() {
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
        event.register(EndpointTargetAccess.class);
    }
}
