package com.acushii.markethelper.client.config;

import com.acushii.markethelper.client.config.ModConfig.DisplayMode;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.Arrays;

public class ModMenuIntegration implements ModMenuApi {
    private static final ChatFormatting[] formattingColors = Arrays.stream(ChatFormatting.values())
            .filter(ChatFormatting::isColor)
            .toArray(ChatFormatting[]::new);

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ModConfig config = ModConfig.get();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Wynncraft Market Price Helper Settings"));

            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // Display Mode selector
            general.addEntry(entryBuilder.startEnumSelector(
                    Component.literal("Price Display Mode"),
                    DisplayMode.class,
                    config.displayMode
            )
            .setDefaultValue(DisplayMode.INTEGER)
            .setTooltip(Component.literal("Choose whether pre-tax values are truncated or should show decimals."))
            .setSaveConsumer(val -> config.displayMode = val)
            .build());

            // Primary Color selector
            general.addEntry(entryBuilder.startSelector(
                    Component.literal("Price Value Color"),
                    formattingColors,
                    config.primaryColor
            )
            .setNameProvider(color -> Component.literal(formatColorName(color)).withStyle(color))
            .setDefaultValue(ChatFormatting.WHITE)
            .setSaveConsumer(val -> config.primaryColor = val)
            .build());

            // Secondary Color selector
            general.addEntry(entryBuilder.startSelector(
                    Component.literal("Label Text Color"),
                    formattingColors,
                    config.secondaryColor
            )
            .setNameProvider(color -> Component.literal(formatColorName(color)).withStyle(color))
            .setDefaultValue(ChatFormatting.GRAY)
            .setSaveConsumer(val -> config.secondaryColor = val)
            .build());

            builder.setSavingRunnable(config::save);

            return builder.build();
        };
    }

    private static String formatColorName(ChatFormatting color) {
        String[] parts = color.getName().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase())
                    .append(" ");
            }
        }
        return sb.toString().trim();
    }
}