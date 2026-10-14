package io.github.piscescup.fabricmc.gui.workspace.view;

import com.thecsdev.commonmc.api.client.gui.TElement;
import com.thecsdev.commonmc.api.client.gui.render.TGuiGraphics;
import io.github.piscescup.fabricmc.gui.widget.Labels;
import io.github.piscescup.fabricmc.gui.widget.WidgetTheme;
import io.github.piscescup.fabricmc.gui.workspace.controller.VocabularyWorkspace;
import io.github.piscescup.fabricmc.gui.workspace.controller.WordbookActions;
import io.github.piscescup.fabricmc.gui.workspace.view.editor.MenubarPanel;
import io.github.piscescup.fabricmc.gui.workspace.view.editor.TabStripPanel;
import io.github.piscescup.fabricmc.gui.workspace.view.tab.WorkspaceTabGUI;



/**
 * Menu, tab strip and active tab view; refreshed from the controller's revision.
 */
public final class WorkspaceGUI extends TElement {
    private final VocabularyWorkspace workspace;
    private final WordbookActions actions;
    private long revision = Long.MIN_VALUE;

    public WorkspaceGUI(VocabularyWorkspace workspace, WordbookActions actions) {
        this.workspace = workspace;
        this.actions = actions;
    }

    @Override
    protected void tickCallback() {
        if (revision != workspace.revision()) clearAndInit();
    }

    @Override
    public void renderCallback(TGuiGraphics pencil) {
        var bb = getBounds();
        pencil.fillColor(bb.x, bb.y + 20, bb.width, bb.height - 20, 0x33000000);
        pencil.drawOutlineIn(bb.x, bb.y + 20, bb.width, bb.height - 20, 0xAA000000);
    }

    @Override
    protected void initCallback() {
        revision = workspace.revision();
        var bb = getBounds();
        var menubar = new MenubarPanel(workspace, actions);
        menubar.setBounds(bb.x, bb.y, bb.width, 17);
        add(menubar);

        var tabstrip = new TabStripPanel(workspace);
        tabstrip.setBounds(bb.x + 1, bb.y + 21, bb.width - 2, 19);
        add(tabstrip);

        var page = WorkspaceTabGUI.create(workspace, actions);
        page.setBounds(bb.x + 1, bb.y + 40, bb.width - 2, Math.max(1, bb.height - 58));
        add(page);

        add(Labels.label(workspace.status(), bb.x + 7, bb.endY - 17,
            bb.width - 14, 16, WidgetTheme.MUTED));
    }
}
