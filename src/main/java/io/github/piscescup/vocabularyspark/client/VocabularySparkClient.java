package io.github.piscescup.vocabularyspark.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.piscescup.vocabularyspark.VocabularySparkModReferences;
import io.github.piscescup.vocabularyspark.client.audio.PronunciationPlayer;
import io.github.piscescup.vocabularyspark.gui.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.controller.WordbookActions;
import io.github.piscescup.vocabularyspark.gui.VocabularyScreen;
import io.github.piscescup.vocabularyspark.wordbook.WordbookLibrary;
import io.github.piscescup.vocabularyspark.study.StudyFiles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Objects;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class VocabularySparkClient implements ClientModInitializer {
    private static VocabularySparkClient instance;
    private VocabularyWorkspace workspace;
    private WordbookActions actions;

    @Override
    public void onInitializeClient() {
        Minecraft client = Minecraft.getInstance();
        var library = WordbookLibrary.withBuiltins(VocabularySparkModReferences.FABRIC_LOADER.getConfigDir()
            .resolve(VocabularySparkModReferences.MOD_ID)
            .resolve("wordbooks"));

        var user = client.getUser();
        var studyFiles = new StudyFiles(library.directory().getParent(), user.getName(), user.getProfileId());
        workspace = new VocabularyWorkspace(library, client::schedule, new PronunciationPlayer(client), studyFiles);
        actions = new WordbookActions(client, workspace);
        instance = this;
        workspace.load();

        var category = KeyMapping.Category.register(VocabularySparkModReferences.fromPath("main"));
        var open = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.vocabulary-spark.open",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_K,
            category)
        );

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            workspace.pronunciationPlayer().tick();
            while (open.consumeClick()) {
                if (minecraft.gui.screen() == null) openScreen(minecraft);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                literal("vocabulary")
                    .executes(context -> {
                        client.schedule(() -> openScreen(client));
                        return 1;}
                    )
            )
        );

        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> workspace.close());
    }

    private void openScreen(Minecraft client) {
        client.gui.setScreen(createScreen(client.gui.screen()));
    }

    /** Creates a workspace screen using the initialized client state and the supplied return screen. */
    public static Screen createScreen(Screen previous) {
        var initialized = Objects.requireNonNull(instance, "Vocabulary Spark client has not initialized");
        return new VocabularyScreen(previous, initialized.workspace, initialized.actions).getAsScreen();
    }
}
