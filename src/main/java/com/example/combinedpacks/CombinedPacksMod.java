package com.example.combinedpacks;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombinedPacksMod implements ModInitializer {
	public static final String MOD_ID = "combinedpacks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Combined Packs Mod initialized");
	}
}
