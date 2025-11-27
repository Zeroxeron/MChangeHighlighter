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
import net.minecraft.util.math.BlockPos;
import java.util.Locale;
import java.util.regex.Matcher;

import static org.kiva.mchangehighlighter.MChangeHighlighter.EVENT_HISTORY;
import static org.kiva.mchangehighlighter.MConfig.*;
import static org.kiva.mchangehighlighter.util.EntryOrganizer.clean;
import static org.kiva.mchangehighlighter.util.EntryOrganizer.coordinate;

public class Parser {
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

    public static void tryParseAndAdd(String raw) {
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
            //boolean thisHasCoord = coordM.find();
            int x = Integer.parseInt(coordM.group(1));
            int y = Integer.parseInt(coordM.group(2));
            int z = Integer.parseInt(coordM.group(3));
            String dim = coordM.groupCount() >= 4 ? coordM.group(4) : null;
            e.coord = new BlockPos(x, y, z);
            e.dimension = dim;
        }
        EVENT_HISTORY.addLast(e);
        if (EVENT_HISTORY.size() > 512) EVENT_HISTORY.removeFirst();
        coordinate();
        clean();
    }
}
