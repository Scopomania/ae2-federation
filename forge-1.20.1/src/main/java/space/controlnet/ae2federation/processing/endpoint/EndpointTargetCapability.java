package space.controlnet.ae2federation.processing.endpoint;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public final class EndpointTargetCapability {
    public static final Capability<EndpointTargetAccess> BLOCK = CapabilityManager.get(new CapabilityToken<>() { });

    private EndpointTargetCapability() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(EndpointTargetAccess.class);
    }
}
