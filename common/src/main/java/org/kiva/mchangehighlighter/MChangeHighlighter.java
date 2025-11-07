/*
 * Copyright (c) 2025 x_Kiva_x
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */

package org.kiva.mchangehighlighter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.kiva.mchangehighlighter.compat.ModMenuApiImpl;
import org.kiva.mchangehighlighter.util.MLogger;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.kiva.mchangehighlighter.Parser.tryParseAndAdd;
import static org.kiva.mchangehighlighter.Renderer.renderAll;

public class MChangeHighlighter {

    public static final String MOD_ID = "mchangehighlighter";
    public static final String MOD_NAME = "MChangeHighlighter";
    public static final MLogger LOG = new MLogger(MOD_NAME);

    public static boolean hasResetConfig = false;
    private static boolean ENABLED = true;
    private static volatile boolean toggled_render = false;

    public static List<HighlightEntry> ENTRIES = new CopyOnWriteArrayList<>();
    public static final Deque<ChatEvent> EVENT_HISTORY = new ArrayDeque<>();

    public static KeyBinding toggleKey;
    public static KeyBinding clearKey;

    public static MConfig config;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mchangehighlighter.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final ModMenuApiImpl modMenuApi = new ModMenuApiImpl();

    public static void init() {
        MConfig.HANDLER.load();
        loadConfig();
        // KBinds
        KeyBinding.Category kb_category = new KeyBinding.Category(Identifier.of("mchangehighlighter","main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mchangehighlighter.togglekey", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, kb_category));
        clearKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mchangehighlighter.clearKey", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, kb_category));
    }

    public static void afterMessage(Text message, boolean overlay) {
        if (!ENABLED) return;
        tryParseAndAdd(message.getString());
    }

    /** Keybinds **/
    public static void afterClientTick(MinecraftClient client) {
        if (!ENABLED) return;
        if (toggleKey.wasPressed()) {
            toggled_render = !toggled_render;
            if (client.player == null) {return;}
            client.inGameHud.getChatHud().addMessage(Text.literal("MChangeHighlighter: " + (toggled_render ? "enabled" : "disabled")));
            if (toggled_render) {
                System.out.println("Rendering for "+ENTRIES.size()+" entries:");
                for (HighlightEntry e : ENTRIES) {
                    System.out.println(" "+e.action +" "+e.blockName+" "+e.pos.toString());
                }
            }
        }
        if (clearKey.wasPressed()) {
            ENTRIES.clear();
            EVENT_HISTORY.clear();
            client.inGameHud.setOverlayMessage(Text.literal("MChangeHighlighter: Cleared"), false);
            //client.inGameHud.getChatHud().addMessage(Text.literal("MChangeHighlighter: Cleared"));
        }
    }

    public static void afterRender(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        if (!ENABLED) return;
        if (!toggled_render) return; renderAll(context);
    }

    // ----------------------------- Config -----------------------------
    private static void loadConfig() {
        try {if (Files.exists(CONFIG_PATH)) {try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {config = GSON.fromJson(r, MConfig.class);}}}
        catch (Exception ex) {System.err.println("Failed to load MChangeHighlighter config, using defaults: " + ex.getMessage());}
        if (config == null) config = MConfig.defaultConfig();
        saveConfig();
    }

    private static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, w);
            }
        }
        catch (IOException ex) {System.err.println("Failed to save MChangeHighlighter config: " + ex.getMessage());}
    }

    public static void setEnabled(boolean enabled){ENABLED = enabled;}
}