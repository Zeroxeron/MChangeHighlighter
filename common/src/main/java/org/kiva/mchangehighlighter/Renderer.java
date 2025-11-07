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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Locale;

import static org.kiva.mchangehighlighter.MChangeHighlighter.ENTRIES;
import static org.kiva.mchangehighlighter.MChangeHighlighter.config;

public class Renderer {
    public static void renderAll(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        Vec3d cam = context.gameRenderer().getCamera().getPos();
        Matrix4f matrices = context.matrices().peek().getPositionMatrix();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) {return;}
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());
        if (ENTRIES.isEmpty()) return;
        for (HighlightEntry e : ENTRIES) {loadOutlinesVc(vc, matrices, cam, e);}
        //context.gameRenderer().render(RenderTickCounter.ONE,false);
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


    /** Colors **/
    private static int resolveColorForEntry(HighlightEntry e) {
        if (e.blockName != null && config.materialColors != null) {
            String key = e.blockName.toLowerCase(Locale.ROOT);
            if (config.materialColors.containsKey(key)) {return hexToRgb(config.materialColors.get(key));}
        }
        return switch (e.action) {
            case "placed" -> hexToRgb(config.placedColor);
            case "removed" -> hexToRgb(config.removedColor);
            case "changed" -> hexToRgb(config.changedColor); // If placed + removed
            default -> hexToRgb(config.unknownColor);
        };
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
}
