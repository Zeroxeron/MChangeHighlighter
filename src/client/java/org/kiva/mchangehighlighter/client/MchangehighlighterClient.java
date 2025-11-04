package org.kiva.mchangehighlighter.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MchangehighlighterClient implements ClientModInitializer {
    // Corrected regex pattern per your note
    private static final Pattern COORD_PATTERN = Pattern.compile("\\(x(-?\\d+)/y(-?\\d+)/z(-?\\d+)(?:/([^)]*))?\\)");
    private static final Pattern BLOCK_PATTERN = Pattern.compile("(?:placed|broke)\\s+(\\w+)");
    private static List<HighlightEntry> ENTRIES = new CopyOnWriteArrayList<>();
    // event history to pair actions and coordinates robustly
    private static final Deque<ChatEvent> EVENT_HISTORY = new ArrayDeque<>();
    private static final int MAX_HISTORY = 256;
    private static KeyBinding toggleKey;
    private static KeyBinding clearKey;
    private static volatile boolean enabled = false;
    private static Config config;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("mchangehighlighter.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onInitializeClient() {
        loadConfig();
        // KBinds
        //  toggle keybind (default X)
        //  clear keybind (default Z)
        KeyBinding.Category kb_category = new KeyBinding.Category(Identifier.of("key.category.mchangehighlighter"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mchangehighlighter.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, kb_category));
        clearKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mchangehighlighter.clear", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, kb_category));
        System.out.println("[MCH]: registered chat!");
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> tryParseAndAdd(message.getString())); // listen for chat messages
        WorldRenderEvents.END_MAIN.register(context -> {if (!enabled) return; renderAll(context);}); // world render - draw at the end of world rendering pass
        // client tick to toggle and user feedback
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (client.player == null) {return;}
                client.inGameHud.getChatHud().addMessage(Text.literal("MChangeHighlighter: " + (enabled ? "enabled" : "disabled")));
                if (enabled) {
                    System.out.println("Rendering for "+ENTRIES.size()+" entries:");
                    for (HighlightEntry e : ENTRIES) {
                        System.out.println(" "+e.action +" "+e.blockName+" "+e.pos.toString());
                    }
                }
            }
            if (clearKey.wasPressed()) {
                ENTRIES.clear();
                EVENT_HISTORY.clear();
                client.inGameHud.getChatHud().addMessage(Text.literal("MChangeHighlighter: Cleared"));
            }
        });
    }

    // ----------------------------- Parsing -----------------------------
    private static final class ChatEvent {
        enum Type { ACTION_MINUS, ACTION_PLUS, POST_COORD, PRE_CORD}
        Type type;
        final String raw;
        BlockPos coord; // if COORD
        String dimension; // optional
        ChatEvent(Type type, String raw) {
            this.type = type;
            this.raw = raw;
        }
    }

    private static class HighlightEntry {
        BlockPos pos;
        String blockName;
        String action;
        HighlightEntry(BlockPos pos, String blockName, String action) {
            this.pos = pos;
            this.blockName = blockName;
            this.action = action;
        }
    }

    private static String stripFormatting(String s) {
        if (s == null || s.isEmpty()) return s == null ? null : "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§') {i++; continue;} // skip formatting code + next character
            sb.append(c);
        }
        return sb.toString();
    }
    private static void tryParseAndAdd(String raw) {
        if (raw == null) return;
        String s = stripFormatting(raw).trim();
        if (s.isEmpty()) return;
        String lower = s.toLowerCase(Locale.ROOT);
        if (!(lower.contains("placed") || lower.contains("broke") || s.contains("(x"))) {return;}
        ChatEvent e = null;
        if (lower.contains("^ (x")) {e = new ChatEvent(ChatEvent.Type.POST_COORD, lower);}
        else if (lower.contains("- (x")) {e = new ChatEvent(ChatEvent.Type.PRE_CORD, lower);}
        else if (lower.contains("placed")) {e = new ChatEvent(ChatEvent.Type.ACTION_PLUS, lower);}
        else if (lower.contains("broke")) {e = new ChatEvent(ChatEvent.Type.ACTION_MINUS, lower);}
        if (e == null) {return;}
        if (e.type == ChatEvent.Type.POST_COORD || e.type == ChatEvent.Type.PRE_CORD) {
            Matcher coordM = COORD_PATTERN.matcher(lower);
            boolean thisHasCoord = coordM.find();
            int x = Integer.parseInt(coordM.group(1));
            int y = Integer.parseInt(coordM.group(2));
            int z = Integer.parseInt(coordM.group(3));
            String dim = coordM.groupCount() >= 4 ? coordM.group(4) : null;
            e.coord = new BlockPos(x, y, z);
            e.dimension = dim;
        }
        EVENT_HISTORY.addLast(e);
        if (EVENT_HISTORY.size() > MAX_HISTORY) EVENT_HISTORY.removeFirst();
        coordinator();
        removeDupes();
    }
    private static void removeDupes() {
        Set<String> seenEntries = new HashSet<>();
        List<HighlightEntry> unique = new ArrayList<>();
        for (HighlightEntry e : ENTRIES) {
            if (seenEntries.add(e.pos.toString())) {
                unique.add(e);
            }
        }
        ENTRIES = unique;
    }

    /** Completes the render queue **/
    private static void coordinator() {
        ChatEvent current = null;
        HighlightEntry last_he = null;
        BlockPos last_coords = null;
        List<HighlightEntry> actions_list = new ArrayList<>();
        for (Iterator<ChatEvent> it = EVENT_HISTORY.descendingIterator(); it.hasNext(); ) {
            current = it.next();
            if (current == null) return;
            if (Objects.equals(current.type, ChatEvent.Type.POST_COORD)) {
                Matcher coord = COORD_PATTERN.matcher(current.raw);
                if (!coord.find()) continue;
                int x = Integer.parseInt(coord.group(1));
                int y = Integer.parseInt(coord.group(2));
                int z = Integer.parseInt(coord.group(3));
                last_coords = new BlockPos(x, y, z);
                last_he = new HighlightEntry(last_coords, null, null);
                continue;
            }
            // PRE -------
            if (Objects.equals(current.type, ChatEvent.Type.PRE_CORD)) {
                Matcher coord = COORD_PATTERN.matcher(current.raw);
                if (!coord.find()) continue;
                int x = Integer.parseInt(coord.group(1));
                int y = Integer.parseInt(coord.group(2));
                int z = Integer.parseInt(coord.group(3));
                last_coords = new BlockPos(x, y, z);
                String last_act = "";
                String final_act = "placed";
                String final_block = "";
                for (HighlightEntry hee : actions_list) {
                    final_block = hee.blockName;
                    final_act = hee.action;
                    if ((Objects.equals(last_act, "placed") && Objects.equals(hee.action, "removed")) ||
                        (Objects.equals(last_act, "removed") && Objects.equals(hee.action, "placed"))) {
                        final_act = "changed";
                        break;
                    }
                    last_act = hee.action;
                }
                ENTRIES.add(new HighlightEntry(last_coords, final_block, final_act));
                continue;
            }
            if ((last_he == null) && Objects.equals(current.type, ChatEvent.Type.ACTION_PLUS)) {
                Matcher abHere = BLOCK_PATTERN.matcher(current.raw);
                if (!abHere.find()) continue;
                String blockName = abHere.group(1).toLowerCase(Locale.ROOT);
                actions_list.add(new HighlightEntry(null, blockName, "placed"));
                continue;
            }
            if ((last_he == null) && Objects.equals(current.type, ChatEvent.Type.ACTION_MINUS)) {
                Matcher abHere = BLOCK_PATTERN.matcher(current.raw);
                if (!abHere.find()) continue;
                String blockName = abHere.group(1).toLowerCase(Locale.ROOT);
                actions_list.add(new HighlightEntry(null, blockName, "removed"));
                continue;
            }
            // POST ---------
            if ((last_he != null) && Objects.equals(current.type, ChatEvent.Type.ACTION_PLUS)) {
                Matcher abHere = BLOCK_PATTERN.matcher(current.raw);
                if (!abHere.find()) continue;
                last_he.blockName = abHere.group(1).toLowerCase(Locale.ROOT);
                last_he.action = "placed";
                ENTRIES.add(last_he); last_he = null;
                continue;
            }
            if ((last_he != null) && Objects.equals(current.type, ChatEvent.Type.ACTION_MINUS)) {
                Matcher abHere = BLOCK_PATTERN.matcher(current.raw);
                if (!abHere.find()) continue;
                last_he.blockName = abHere.group(1).toLowerCase(Locale.ROOT);
                last_he.action = "removed";
                ENTRIES.add(last_he); last_he = null;
                continue;
            }
        }
    }
    // ----------------------------- Rendering -----------------------------
    private static void renderAll(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        Vec3d cam = context.gameRenderer().getCamera().getPos();
        Matrix4f matrices = context.matrices().peek().getPositionMatrix();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {return;}
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());
        if (ENTRIES.isEmpty()) return;
        for (HighlightEntry e : ENTRIES) {loadOutlinesVc(vc, matrices, cam, e);}
        context.gameRenderer().render(RenderTickCounter.ONE,false);
    }
    private static void loadOutlinesVc(VertexConsumer vc, Matrix4f mat, Vec3d cam, HighlightEntry e) {
        float x1 = (float) (e.pos.getX() - cam.x); float y1 = (float) (e.pos.getY() - cam.y); float z1 = (float) (e.pos.getZ() - cam.z);
        float x2 = x1 + 1.0f;   float y2 = y1 + 1.0f;   float z2 = z1 + 1.0f;
        float[][] c = new float[][] {{x1, y1, z1}, {x2, y1, z1}, {x2, y2, z1}, {x1, y2, z1}, {x1, y1, z2}, {x2, y1, z2}, {x2, y2, z2}, {x1, y2, z2}}; // corners
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4}, {0,4},{1,5},{2,6},{3,7}};
        int rgb = resolveColorForEntry(e) & 0xFFFFFF;
        int argb = 0xFF000000 | rgb; // opaque ARGB expected by BufferBuilder.color(int)
        float nx = 0f, ny = 1f, nz = 0f;
        for (int[] ed : edges) {
            float[] a0 = c[ed[0]]; float[] a1 = c[ed[1]];
            vc.vertex(mat, a0[0], a0[1], a0[2]).color(argb).normal(nx, ny, nz);
            vc.vertex(mat, a1[0], a1[1], a1[2]).color(argb).normal(nx, ny, nz);
        }
    }

    // ----------------------------- Helper for colors -----------------------------
    private static int resolveColorForEntry(HighlightEntry e) {
        if (e.blockName != null && config.materialColors != null) {
            String key = e.blockName.toLowerCase(Locale.ROOT);
            if (config.materialColors.containsKey(key)) {return hexToRgb(config.materialColors.get(key));}
        }
        return switch (e.action) {
            case "placed" -> hexToRgb(config.placedColor);
            case "removed" -> hexToRgb(config.removedColor);
            case "changed" -> hexToRgb(config.changedColor);
            default -> hexToRgb(config.unknownColor);
        };
    }

    // ----------------------------- Config -----------------------------
    private static void loadConfig() {
        try {if (Files.exists(CONFIG_PATH)) {try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {config = GSON.fromJson(r, Config.class);}}}
        catch (Exception ex) {System.err.println("Failed to load MChangeHighlighter config, using defaults: " + ex.getMessage());}
        if (config == null) config = Config.defaultConfig();
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

    private static int hexToRgb(String hex) {
        if (hex == null) return 0xFF00FF; // magenta fallback
        String h = hex.replace("#", "");
        if (h.length() == 6) {return Integer.parseInt(h, 16);}
        if (h.length() == 3) {
            char r = h.charAt(0);
            char g = h.charAt(1);
            char b = h.charAt(2);
            return Integer.parseInt("" + r + r + g + g + b + b, 16);}
        try {return Integer.parseInt(h, 16);} catch (Exception ex) {return 0xFF00FF;}
    }

    private static class Config {
        @SerializedName("placedColor")
        String placedColor = "#ffff00";
        @SerializedName("removedColor")
        String removedColor = "#ff0000";
        @SerializedName("changedColor")
        String changedColor = "#999999";
        @SerializedName("unknownColor")
        String unknownColor = "#ff00ff";
        @SerializedName("materialColors")
        Map<String, String> materialColors = Map.of(
                "deepslate_diamond_ore", "#00ffff",
                "ancient_debris", "#ffffff"
        );
        static Config defaultConfig() {
            return new Config();
        }
    }
}