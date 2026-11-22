package space.controlnet.ae2federation.forge.gui;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's one menu type: the Federation screen. */
public final class FederationMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "ae2federation");

    public static final RegistryObject<MenuType<FederationPolicyMenu>> POLICY = MENUS.register("policy",
            () -> IForgeMenuType.create((containerId, inventory, buffer) -> new FederationPolicyMenu(containerId, inventory)));

    private FederationMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
