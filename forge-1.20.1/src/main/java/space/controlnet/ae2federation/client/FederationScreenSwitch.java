package space.controlnet.ae2federation.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;
import space.controlnet.ae2federation.mixin.client.MouseHandlerAccess;

/**
 * Keeps the cursor where it was when the server switches between Federation screens: the domain workspace and the
 * Federation Pattern Provider's AE2 screen, whose buttons lead to each other. See {@link ScreenSwitchCursor}.
 */
public final class FederationScreenSwitch {
    private static final ScreenSwitchCursor CURSOR = new ScreenSwitchCursor();

    private FederationScreenSwitch() {
    }

    /** Before the screen goes; showing no screen next grabs the mouse and centres it. */
    public static void onClosing(ScreenEvent.Closing event) {
        if (!isFederation(event.getScreen())) return;
        var mouse = Minecraft.getInstance().mouseHandler;
        CURSOR.left(mouse.xpos(), mouse.ypos(), Util.getMillis());
    }

    /** After the mouse was released at the centre for the new screen. */
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!isFederation(event.getScreen())) {
            CURSOR.forget();
            return;
        }
        CURSOR.arrived(Util.getMillis()).ifPresent(position -> {
            var minecraft = Minecraft.getInstance();
            GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), position[0], position[1]);
            // Only when the MouseHandlerAccess mixin is applied; it needs the refmap setup (see PORTING.md).
            Object handler = minecraft.mouseHandler;
            if (handler instanceof MouseHandlerAccess mouse) {
                mouse.ae2federation$setXpos(position[0]);
                mouse.ae2federation$setYpos(position[1]);
            }
        });
    }

    private static boolean isFederation(Screen screen) {
        // TODO(gui-port): the 1.21 policy screens (FederationGuiScale.isFixed) return with the LDLib 1 rewrite.
        return screen instanceof appeng.client.gui.implementations.PatternProviderScreen<?> provider
                        && provider.getMenu().getBlockEntity()
                                instanceof space.controlnet.ae2federation.processing.provider.FederationPatternProviderBlockEntity;
    }
}
