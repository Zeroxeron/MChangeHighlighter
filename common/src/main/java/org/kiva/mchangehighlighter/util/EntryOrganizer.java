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

package org.kiva.mchangehighlighter.util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.kiva.mchangehighlighter.ChatEvent;
import org.kiva.mchangehighlighter.HighlightEntry;
import org.kiva.mchangehighlighter.MConfig;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.regex.Matcher;
import static org.kiva.mchangehighlighter.MChangeHighlighter.*;
import static org.kiva.mchangehighlighter.Parser.BLOCK_PATTERN;
import static org.kiva.mchangehighlighter.Parser.COORD_PATTERN;

public class EntryOrganizer {

    /** Also merges placed+removed into changed **/
    public static void clean() {
        HashMap<String, HighlightEntry> seenEntries = new HashMap<>();
        for (HighlightEntry e : ENTRIES) {
            String ekey = e.pos.toString();
            if (seenEntries.containsKey(ekey)) {
                if (config.materialColors.containsKey(seenEntries.get(ekey).blockName)) continue;  // skip if material
                if ((Objects.equals(seenEntries.get(ekey).action, "placed") && (Objects.equals(e.action, "removed"))) ||
                        (Objects.equals(seenEntries.get(ekey).action, "removed") && (Objects.equals(e.action, "placed"))) ||
                        (Objects.equals(seenEntries.get(ekey).action, "changed")) || (Objects.equals(e.action, "changed"))) {
                    e.action = "changed";
                    seenEntries.put(ekey, e); continue;} // set changed
                seenEntries.put(ekey, e);
                continue;
            }
            seenEntries.put(ekey, e);
        }
        ENTRIES = new ArrayList<>(seenEntries.values());
    }

    /** Completes the render queue **/
    public static void coordinate() {
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
                for (HighlightEntry hee : actions_list) {ENTRIES.add(new HighlightEntry(last_coords, hee.blockName, hee.action));}
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
}
