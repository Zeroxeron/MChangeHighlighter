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
import com.google.gson.annotations.SerializedName;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import static org.kiva.mchangehighlighter.MChangeHighlighter.LOG;
import static org.kiva.mchangehighlighter.MChangeHighlighter.toggled_seethrough;

public class MConfig {
    //private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir();
    //public static final String FILE_NAME = MChangeHighlighter.MOD_ID + ".json";
    //public static final String FILE_NAME_INVALID = MChangeHighlighter.MOD_ID + ".invalid.json";
    //public static final String FILE_NAME_OLD = MChangeHighlighter.MOD_ID + ".old.json";

    public static ConfigClassHandler<MConfig> HANDLER = ConfigClassHandler.createBuilder(MConfig.class)
            .id(Identifier.of("mchangehighlighter", "mconfig"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("mchangehighlighter.json"))
                    .setJson5(false)
                    .build())
            .build();

    @SerializedName("patternCoords")
    static String patternCoords = "\\(x(-?\\d+)/y(-?\\d+)/z(-?\\d+)(?:/([^)]*))?\\)";
    @SerializedName("patternBlock")
    static String patternBlocks = "(?:placed|broke)\\s+(\\w+)";
    @SerializedName("patternCoords")
    static String keywordPlace = "placed";
    @SerializedName("patternBlock")
    static String keywordBroke = "broke";

    public static Pattern COORD_PATTERN = Pattern.compile(patternCoords);
    public static Pattern BLOCK_PATTERN = Pattern.compile(patternBlocks);

    @SerializedName("placedColor")
    String placedColor = "#bbff00";
    @SerializedName("removedColor")
    String removedColor = "#ff0000";
    @SerializedName("changedColor")
    String changedColor = "#ffbb00";
    @SerializedName("unknownColor")
    String unknownColor = "#ff00ff";
    @SerializedName("materialColors")
    public Map<String, String> materialColors = Map.of(
            "deepslate_diamond_ore", "#00ffff",
            "ancient_debris", "#ffffff"
    );

    public MConfig() {}

    public static MConfig defaultConfig() {
        return new MConfig();
    }

    @SerialEntry
    public static boolean enabled = true;
    @SerialEntry
    public static int defaultDistance = 512;

    public static void open_config() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("mchangehighlighter.json");
        if (!configFile.toFile().exists()) {return;} // just a safe check, should not ever trigger actually lol
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {return;}
            try {
                desktop.open(configFile.toFile());
            } catch (java.io.IOException e) {
                LOG.error("No config file to open");
            }
        }
    }

    public static void reload_config() {
        MConfig.HANDLER.load();
        COORD_PATTERN = Pattern.compile(patternCoords);
        BLOCK_PATTERN = Pattern.compile(patternBlocks);
    }

    public static Screen openConfigScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("mchangehighlighter.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("mchangehighlighter.config.categories.main.name"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("mchangehighlighter.config.categories.main.options.enabled.name"))
                                .description(OptionDescription.of(Text.translatable("mchangehighlighter.config.categories.main.options.enabled.description")))
                                .binding(MConfig.enabled, () -> MConfig.enabled, MChangeHighlighter::setEnabled)
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .coloured(true)
                                        .yesNoFormatter())
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.translatable("key.mchangehighlighter.keyTransparent"))
                                .binding(toggled_seethrough, () -> toggled_seethrough, MChangeHighlighter::setTransparent)
                                .controller(opt -> BooleanControllerBuilder.create(opt)
                                        .coloured(true)
                                        .yesNoFormatter())
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.translatable("mchangehighlighter.config.categories.main.options.distance.name"))
                                .description(OptionDescription.of(Text.translatable("mchangehighlighter.config.categories.main.options.distance.description")))
                                .binding(128, () -> MConfig.defaultDistance, newVal -> MConfig.defaultDistance = newVal)
                                .controller(opt -> IntegerFieldControllerBuilder.create(opt)
                                        .range(4, 2048))
                                .build())
                        .option(LabelOption.createBuilder()
                                .line(Text.translatable("mchangehighlighter.config.categories.main.options.keyToggle", MChangeHighlighter.keyToggle.getBoundKeyLocalizedText()))
                                .line(Text.translatable("mchangehighlighter.config.categories.main.options.keyNoteNoChange").formatted(Formatting.GRAY))
                                .build())
                        .option(LabelOption.createBuilder()
                                .line(Text.translatable("mchangehighlighter.config.categories.main.options.keyClear", MChangeHighlighter.keyClear.getBoundKeyLocalizedText()))
                                .line(Text.translatable("mchangehighlighter.config.categories.main.options.keyNoteNoChange").formatted(Formatting.GRAY))
                                .build())
                        .option(LabelOption.createBuilder()
                                .line(Text.translatable("mchangehighlighter.config.categories.main.options.keyTransparent", MChangeHighlighter.keyTransparent.getBoundKeyLocalizedText()))
                                .line(Text.translatable("mchangehighlighter.config.categories.main.options.keyNoteNoChange").formatted(Formatting.GRAY))
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Text.translatable("mchangehighlighter.config.categories.main.options.open-controls.name"))
                                .description(OptionDescription.of(Text.translatable("mchangehighlighter.config.categories.main.options.open-controls.description")))
                                .text(Text.empty())
                                .action((screen, opt) -> MinecraftClient.getInstance().setScreen(new KeybindsScreen(parent, MinecraftClient.getInstance().options)))
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Text.translatable("mchangehighlighter.config.categories.main.options.open-config.name"))
                                .description(OptionDescription.of(Text.translatable("mchangehighlighter.config.categories.main.options.open-config.description")))
                                .text(Text.empty())
                                .action((screen, opt) -> open_config())
                                .build())
                        .option(ButtonOption.createBuilder()
                                .name(Text.translatable("mchangehighlighter.config.categories.main.options.reload.name"))
                                .description(OptionDescription.of(Text.translatable("mchangehighlighter.config.categories.main.options.reload.description")))
                                .text(Text.empty())
                                .action((screen, opt) -> reload_config())
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.translatable("mchangehighlighter.config.categories.main.parsing.name"))
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("mchangehighlighter.config.categories.main.parsing.pattern-blocks.name"))
                                .description(OptionDescription.of(Text.translatable("mchangehighlighter.config.categories.main.parsing.pattern-blocks.description")))
                                .binding(patternBlocks, () -> patternBlocks, newValue -> patternBlocks = newValue)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("mchangehighlighter.config.categories.main.parsing.pattern-coords.name"))
                                .description(OptionDescription.of(Text.translatable("mchangehighlighter.config.categories.main.parsing.pattern-coords.description")))
                                .binding(patternCoords, () -> patternCoords, newValue -> patternCoords = newValue)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("mchangehighlighter.config.categories.main.parsing.place.name"))
                                .description(OptionDescription.of(Text.translatable("mchangehighlighter.config.categories.main.parsing.place.description")))
                                .binding(keywordPlace, () -> keywordPlace, newValue -> keywordPlace = newValue)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.translatable("mchangehighlighter.config.categories.main.parsing.broke.name"))
                                .description(OptionDescription.of(Text.translatable("mchangehighlighter.config.categories.main.parsing.broke.description")))
                                .binding(keywordBroke, () -> keywordBroke, newValue -> keywordBroke = newValue)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                .build().generateScreen(parent);
    }
}