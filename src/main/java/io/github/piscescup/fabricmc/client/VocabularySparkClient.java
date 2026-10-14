package io.github.piscescup.fabricmc.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.piscescup.fabricmc.References;
import io.github.piscescup.fabricmc.client.audio.PronunciationPlayer;
import io.github.piscescup.fabricmc.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.fabricmc.gui.workspace.controller.WordbookActions;
import io.github.piscescup.fabricmc.gui.workspace.view.VocabularyScreen;
import io.github.piscescup.fabricmc.wordbook.WordbookLibrary;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class VocabularySparkClient implements ClientModInitializer {
    private VocabularyWorkspace workspace;
    private WordbookActions actions;

    @Override
    public void onInitializeClient() {
        Minecraft client = Minecraft.getInstance();
        var library = new WordbookLibrary(References.FABRIC_LOADER.getConfigDir()
            .resolve(References.MOD_ID)
            .resolve("wordbooks"));

        workspace = new VocabularyWorkspace(library, client::schedule, new PronunciationPlayer(client));
        actions = new WordbookActions(client, workspace);
        workspace.load();

        var category = KeyMapping.Category.register(References.fromPath("main"));
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
        var screen = new VocabularyScreen(client.gui.screen(), workspace, actions);
        client.gui.setScreen(screen.getAsScreen());
    }
}
