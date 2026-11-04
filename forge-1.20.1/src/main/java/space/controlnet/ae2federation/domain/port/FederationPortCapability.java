package space.controlnet.ae2federation.domain.port;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class FederationPortCapability {
    public static final Capability<FederationPort> BLOCK = CapabilityManager.get(new CapabilityToken<>() { });

    private FederationPortCapability() {
    }

    /** The capability type must be registered once on the mod bus before any lookup. */
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(FederationPort.class);
    }
}
