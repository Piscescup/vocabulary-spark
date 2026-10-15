package io.github.piscescup.vocabularyspark.gui.workspace.view;

import com.thecsdev.common.math.UDim2;
import com.thecsdev.commonmc.api.client.gui.screen.ILastScreenProvider;
import com.thecsdev.commonmc.api.client.gui.screen.TScreenPlus;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.WordbookActions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The only engine-facing screen; every child control is a TCDCommons element.
 */
public final class VocabularyScreen extends TScreenPlus implements ILastScreenProvider {
    private final Screen previous;
    private final VocabularyWorkspace workspace;
    private final WordbookActions actions;

    public VocabularyScreen(Screen previous, VocabularyWorkspace workspace, WordbookActions actions) {
        super(Component.literal("Vocabulary Spark"));
        this.previous = previous;
        this.workspace = workspace;
        this.actions = actions;
    }

    @Override
    public Screen getLastScreen() {
        return previous;
    }

    @Override
    public boolean isAllowingInGameHud() {
        return false;
    }

    @Override
    protected void initCallback() {
        var gui = new WorkspaceGUI(workspace, actions);
        add(gui);
        gui.setBounds(new UDim2(0.05, 0, 0, 0), new UDim2(0.9, 0, 1, -5));
    }
}
