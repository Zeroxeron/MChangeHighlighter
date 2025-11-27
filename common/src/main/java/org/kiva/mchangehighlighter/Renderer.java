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
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Locale;

import static org.kiva.mchangehighlighter.MChangeHighlighter.*;

public class Renderer {
    /** Classic rendering without mixins **/
    public static void renderAll(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        if (context == null) return;
        Vec3d cam = context.gameRenderer().getCamera().getPos();
        Matrix4f matrices = context.matrices().peek().getPositionMatrix();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {return;}
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());
        if (ENTRIES.isEmpty()) return;
        for (HighlightEntry e : ENTRIES) {loadOutlinesVc(vc, matrices, cam, e);}
        if (toggled_seethrough) {context.gameRenderer().render(RenderTickCounter.ONE, false);} // quick-redraw the layer with outlines applied
    }

    /** Outlining and adding to the vc **/
    public static void loadOutlinesVc(VertexConsumer vc, Matrix4f mat, Vec3d cam, HighlightEntry e) {
        float x1 = (float) (e.pos.getX() - cam.x); float y1 = (float) (e.pos.getY() - cam.y); float z1 = (float) (e.pos.getZ() - cam.z);
        float x2 = x1 + 1.0f;   float y2 = y1 + 1.0f;   float z2 = z1 + 1.0f;
        if ((Math.abs(z1) > MConfig.defaultDistance) || (Math.abs(x1) > MConfig.defaultDistance))  return; // Skip if out of configured render reach
        float[][] c = new float[][] {{x1, y1, z1}, {x2, y1, z1}, {x2, y2, z1}, {x1, y2, z1}, {x1, y1, z2}, {x2, y1, z2}, {x2, y2, z2}, {x1, y2, z2}}; // corners
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4}, {0,4},{1,5},{2,6},{3,7}};
        int argb = resolveColorForEntry(e);
        for (int[] ed : edges) {
            float[] a0 = c[ed[0]]; float[] a1 = c[ed[1]];
            vc.vertex(mat, a0[0], a0[1], a0[2]).color(argb).normal(0f, 1f, 0f);
            vc.vertex(mat, a1[0], a1[1], a1[2]).color(argb).normal(0f, 1f, 0f);
        }
    }

    /** Colors **/
    private static int resolveColorForEntry(HighlightEntry e) {
        if (e.blockName != null && config.materialColors != null) {
            String key = e.blockName.toLowerCase(Locale.ROOT);
            if (config.materialColors.containsKey(key)) {return hexToArgb(config.materialColors.get(key));}
        }
        return switch (e.action) {
            case "placed" -> hexToArgb(config.placedColor);
            case "removed" -> hexToArgb(config.removedColor);
            case "changed" -> hexToArgb(config.changedColor); // If placed + removed
            default -> hexToArgb(config.unknownColor);
        };
    }

    /** Hex colors converter **/
    private static int hexToArgb(String hex) {
        if (hex == null) return 0xFFFF00FF; //
        String h = hex.replace("#", "").trim();
        int len = h.length();
        try {
            switch (len) {
                case 3: { // RGB → AARRGGBB
                    int r = Character.digit(h.charAt(0), 16) * 17;
                    int g = Character.digit(h.charAt(1), 16) * 17;
                    int b = Character.digit(h.charAt(2), 16) * 17;
                    return (0xFF << 24) | (r << 16) | (g << 8) | b;
                }
                case 4: { // ARGB → AARRGGBB
                    int a = Character.digit(h.charAt(0), 16) * 17;
                    int r = Character.digit(h.charAt(1), 16) * 17;
                    int g = Character.digit(h.charAt(2), 16) * 17;
                    int b = Character.digit(h.charAt(3), 16) * 17;
                    return (a << 24) | (r << 16) | (g << 8) | b;
                }
                case 6: { // RRGGBB → AARRGGBB
                    int rgb = Integer.parseInt(h, 16);
                    return 0xFF000000 | rgb; // add full alpha
                }
                case 8: { // AARRGGBB
                    long argb = Long.parseLong(h, 16);
                    return (int) argb;
                }
                default: return 0xFFFF00FF;}
        } catch (Exception ex) {return 0xFFFF00FF;}
    }
}
