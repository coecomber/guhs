package nl.juiced.guhs.feature.vadsrijk.toetsen.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import nl.juiced.guhs.feature.vadsrijk.toetsen.ToetsenIndeling;

/**
 * U-TOETSEN: the key overview on its own screen, for when the pause menu has no room on the right (the "Toetsen ▸" button) and
 * for {@code /toetsen}. Same panel, centred; click a key = the vanilla key screen; Esc / Terug = back.
 */
public final class ToetsenScherm extends Screen {
    private final Screen terug;
    private final ToetsenPaneel paneel = new ToetsenPaneel();

    public ToetsenScherm(Screen terug) {
        super(ToetsenPaneel.titel());
        this.terug = terug;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).bounds(width / 2 - 50, height - 26, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        List<VadsrijkToetsen.Toets> toetsen = VadsrijkToetsen.alle();
        int maxB = width - 16, maxH = height - 40;
        ToetsenIndeling.Uitkomst u = paneel.plan(toetsen, maxB, maxH, 0.5f);
        if (u == null) {
            paneel.vergeet();
            return;
        }
        paneel.teken(g, toetsen, u, (width - u.schermBreedte()) / 2, Math.max(6, (height - 32 - u.schermHoogte()) / 2), mx, my);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dubbel) {
        if (e.button() == 0 && paneel.onder(e.x(), e.y()) != null) {
            minecraft.setScreen(new KeyBindsScreen(this, minecraft.options));
            return true;
        }
        return super.mouseClicked(e, dubbel);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(terug);
    }
}
