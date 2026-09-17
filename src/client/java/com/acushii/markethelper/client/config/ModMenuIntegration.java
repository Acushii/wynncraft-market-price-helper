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

            // Global toggle
            general.addEntry(entryBuilder.startBooleanToggle(
                    Component.literal("Global Mod Toggle"),
                    config.globalToggle
            )
            .setDefaultValue(true)
            .setTooltip(Component.literal("Enable or disable the mod at your wish."))
            .setYesNoTextSupplier(bool -> bool ? Component.literal("Enabled").withStyle(ChatFormatting.GREEN) : Component.literal("Disabled").withStyle(ChatFormatting.RED))
            .setSaveConsumer(val -> config.globalToggle = val)
            .build());

            // Tax rate
            general.addEntry(entryBuilder.startSelector(
                    Component.literal("Trade Market Tax Rate"),
                    new Integer[]{3, 5},
                    config.taxRatePercent
            )
            .setNameProvider(val -> Component.literal(val + "%"))
            .setDefaultValue(5)
            .setTooltip(Component.literal("Choose whether your Tax Rate is at 3% (with Silverbull) or at 5% (without Silverbull)."))
            .setSaveConsumer(val -> config.taxRatePercent = val)
            .build());

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
            .setTooltip(Component.literal("Choose the color of the calculated pre-tax price in the tooltip."))
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
            .setTooltip(Component.literal("Choose the color of the \"Pre-tax price\" string along with the emerald sign after the calculated price in the tooltip."))
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