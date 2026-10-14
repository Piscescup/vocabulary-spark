package io.github.piscescup.fabricmc;

import net.fabricmc.api.ModInitializer;

import static io.github.piscescup.fabricmc.References.*;

public class VocabularySpark implements ModInitializer {


	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		MOD_LOGGER.info(
			"Hello, {} ver {}, powered by Minecraft",
			MOD_NAME, MOD_VERSION
		);
	}

}
