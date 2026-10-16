package io.github.piscescup.vocabularyspark;

import io.github.piscescup.vocabularyspark.datagen.ENUSLanguageProvider;
import io.github.piscescup.vocabularyspark.datagen.ZHCNLanguageProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class VocabularySparkDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		pack.addProvider(ENUSLanguageProvider::new);
		pack.addProvider(ZHCNLanguageProvider::new);
	}
}
