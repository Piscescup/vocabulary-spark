package io.github.piscescup.vocabularyspark.gui.workspace.view.tab;

import com.thecsdev.commonmc.api.client.gui.TElement;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.WordbookActions;
import io.github.piscescup.vocabularyspark.gui.workspace.controller.WorkspaceTab;

/**
 * Tab-view factory; new Search/Recite tabs can be added without changing the screen.
 */
public abstract class WorkspaceTabGUI extends TElement {
    protected final VocabularyWorkspace workspace;
    protected final WordbookActions actions;

    protected WorkspaceTabGUI(VocabularyWorkspace workspace, WordbookActions actions) {
        this.workspace = workspace;
        this.actions = actions;
    }

    public static WorkspaceTabGUI create(VocabularyWorkspace workspace, WordbookActions actions) {
        return switch (workspace.currentTab()) {
            case WorkspaceTab.Page page -> switch (page) {
                case HOME -> new HomeTabGUI(workspace, actions);
                case SEARCH -> new SearchTabGUI(workspace, actions);
                case WORDBOOKS -> new WordbooksTabGUI(workspace, actions);
            };
            case WorkspaceTab.Book book -> new WordbookTabGUI(workspace, actions, book);
        };
    }
}
