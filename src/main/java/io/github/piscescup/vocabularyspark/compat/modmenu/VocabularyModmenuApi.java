package io.github.piscescup.vocabularyspark.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.piscescup.vocabularyspark.client.VocabularySparkClient;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public class VocabularyModmenuApi implements ModMenuApi {
    /**
     * Used to construct a new config screen instance when your mod's
     * configuration button is selected on the mod menu screen. The
     * screen instance parameter is the active mod menu screen.
     *
     * @return A factory for constructing config screen instances.
     */
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return VocabularySparkClient::createScreen;
    }
}
