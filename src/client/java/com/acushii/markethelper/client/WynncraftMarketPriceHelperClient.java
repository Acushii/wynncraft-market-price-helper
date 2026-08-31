package com.acushii.markethelper.client;

import com.acushii.markethelper.client.config.ModConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WynncraftMarketPriceHelperClient implements ClientModInitializer {
	private static final Pattern pricePattern = Pattern.compile("(?:[0-9]+\\s*x\\s*)?([0-9]{1,3}(?:,[0-9]{3})*)");
	private static final String flagString = "Pre-tax";

	@Override
	public void onInitializeClient() {
		// Runs right before rendering any tooltip on screen
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.screen == null) return;
			if (client.screen instanceof AbstractContainerScreen<?> containerScreen) {
				Slot firstSlot = containerScreen.getMenu().slots.getFirst();
				if (!firstSlot.hasItem()) return;
			}
			processTooltip(lines, stack);
		});
	}

	private void processTooltip(List<Component> loreLines, ItemStack stack) {
		// Make sure the lore exists and is not empty
		if (loreLines == null || loreLines.isEmpty()) return;

		// Prevent duplicates
		for (Component line : loreLines) {
			if (line.getString().contains(flagString)) {
				return;
			}
		}

		// Find the index of the line containing the price of the item
		int priceLineIndex = -1;
		String priceLineText = "";

		for (int i = 0; i < loreLines.size(); i++) {
			String currentLineText = loreLines.get(i).getString();
			if (currentLineText.contains("Price") && i + 1 < loreLines.size()) {
				priceLineIndex = i + 1;
				priceLineText = loreLines.get(priceLineIndex).getString();
				break;
			}
		}

		if (priceLineIndex == -1 || priceLineText.isEmpty()) {
			return;
		}

		// Check if the hovered over item is a confirmation button on Trade Market
		if (priceLineText.contains("each")) {
			return;
		}

		// Find the font used in the tooltip
		boolean usesCustomFont = checkIfCustomFont(stack, priceLineIndex);
		String font;
		if (usesCustomFont) {
			font = "minecraft:language/wynncraft";
		}
		else {
			font = "minecraft:default";
		}

		// Grab the price from the lore and parse it into an integer
		int fullPrice = parsePrice(priceLineText);

		if (fullPrice == -1) {
			return;
		}

		// Needed to distinguish between offers of weapons/armor and powders
		// (they use different JSON formatting for some reason)
		boolean hasLeadingSpace = priceLineText.startsWith("\uDB00\uDC05");

		// Create a new lore line and add it into the item lore
		Component newLine = buildLineFromJson(fullPrice, hasLeadingSpace, font);

		if (newLine != null) {
			loreLines.add(priceLineIndex + 1, newLine);
		}
	}

	private int parsePrice(String lineText) {
		Matcher matcher = pricePattern.matcher(lineText);
		int number = -1;
		// If found a matching string, grab it and remove commas to have a pure number as a string
		// and convert it into an integer
		if (matcher.find()) {
			String numberText = matcher.group(1);
			if (numberText != null) {
				try {
					number = Integer.parseInt(numberText.replace(",", ""));
				} catch (NumberFormatException ignored) {}
			}
		}
		return number;
	}

	private Component buildLineFromJson(int fullPrice, boolean hasLeadingSpace, String font) {
		ModConfig config = ModConfig.get();

		// Emeralds calculations
		double preTaxPriceDouble = (double)fullPrice / 1.05;
		int preTaxPriceInt = (int)preTaxPriceDouble;

		if (preTaxPriceDouble < 1) {
			preTaxPriceDouble = 1.0;
		}
		if (preTaxPriceInt < 1) {
			preTaxPriceInt = 1;
		}

		// Format the string so it's the same as in Wynncraft
		String formattedPrice;
		if (config.displayMode == ModConfig.DisplayMode.INTEGER) {
			formattedPrice = NumberFormat.getNumberInstance(Locale.US).format(preTaxPriceInt);
		}
		else {
			formattedPrice = String.format(Locale.US, "%.2f", preTaxPriceDouble);
		}

		// Colors
		String primaryColor = config.primaryColor.getName(); // Raw Emerald price
		String secondaryColor = config.secondaryColor.getName(); // "Pre-tax price:" string

		String spacePrefix = "";

		if (hasLeadingSpace) {
			spacePrefix = "{\"text\":\"󐀅\", \"font\":\"minecraft:space\"},";
		}

		// Full JSON following Wynncraft's formatting
		String rawJson = """
		{
		    "text":"",
		    "extra":[
		        %s
		        {
		            "text":"",
		            "extra":[
		                {
		                    "text":"",
		                    "extra":[
		                        {
		                            "text":"󏿼󐀆",
		                            "font":"minecraft:chat/prefix"
		                        }
		                    ],
		                    "shadow_color":16777215
		                },
		                " ",
		                {
		                    "text":"󐀀",
		                    "extra":[
		                        {
		                        	"text":"Pre-tax price: ",
		                        	"extra":[
		                        		{
											"text":"%s² ",
											"color":"%s"
										}
									],
									"color":"%s",
									"font":"%s"
		                        }
		                    ],
		                    "color":"white",
		                    "font":"minecraft:language/wynncraft"
		                }
		            ],
		            "color":"gold"
		        }
		    ],
		    "color":"white",
		    "italic":false
		}
		""".formatted(spacePrefix, formattedPrice, primaryColor, secondaryColor, font);

		return parseJsonToComponent(rawJson);
	}

	private Component parseJsonToComponent(String jsonString) {
		try {
			// Build a component from JSON
			JsonElement json = JsonParser.parseString(jsonString);
			return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json)
					.resultOrPartial(err -> System.err.println("[Wynncraft Market Price Helper] Error parsing JSON: " + err))
					.orElse(null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	boolean checkIfCustomFont(ItemStack stack, int lineNumber) {
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) {
			return false;
		}

		Component lineText = lore.lines().get(lineNumber);

		JsonElement loreJson = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, lineText).result().orElse(null);
		if (loreJson == null) {
			return false;
		}

		String loreJsonText = loreJson.toString();
		return loreJsonText.contains("minecraft:language/wynncraft");
	}
}