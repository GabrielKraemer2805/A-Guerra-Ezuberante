package io.github.omarflk.guerraezuberante.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeyBindings {
    public static final KeyMapping OPEN_PANEL = new KeyMapping(
            "key.guerra_ezuberante.open_panel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,
            "key.categories.guerra_ezuberante");
    /** B: V is taken by TaCZ (melee and zoom). */
    public static final KeyMapping USE_POWER = new KeyMapping(
            "key.guerra_ezuberante.use_power", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B,
            "key.categories.guerra_ezuberante");

    private ModKeyBindings() {}
}
