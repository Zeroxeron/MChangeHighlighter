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
public class ChatEvent {
    public enum Type {ACTION_MINUS, ACTION_PLUS, POST_COORD, PRE_CORD}
    public Type type;
    public final String raw;
    public BlockPos coord; // if COORD
    public String dimension; // optional
    ChatEvent(Type type, String raw) {
        this.type = type;
        this.raw = raw;
    }
}