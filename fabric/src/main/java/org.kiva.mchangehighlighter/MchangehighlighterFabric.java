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

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class MchangehighlighterFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MChangeHighlighter.init();
        ClientCommandRegistrationCallback.EVENT.register(MChangeHighlighter::afterCmd); // register client commands (soon)
        ClientTickEvents.END_CLIENT_TICK.register(MChangeHighlighter::afterClientTick); // keybindings
        ClientReceiveMessageEvents.GAME.register(MChangeHighlighter::afterMessage);     // chat parsing
        WorldRenderEvents.END_MAIN.register(MChangeHighlighter::afterRender);           // render
    }
}
