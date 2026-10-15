package nl.juiced.guhs.feature.techmachine.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import nl.juiced.guhs.feature.techmachine.TekentafelMenu;

/**
 * The Tekentafel's screen (drawn like {@link MachineScreen}): the grid you lay the recipe on, the slot for sheets under
 * the arrow, the drawing that comes out, and a line that says what is still missing (a sheet, or a recipe it knows).
 */
public class TekentafelScreen extends AbstractContainerScreen<TekentafelMenu> {
    private static final int BLAUW = 0xFF5B8FD9;

    public TekentafelScreen(TekentafelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        MachineScreen.paneel(g, x, y, imageWidth, imageHeight);
        for (int rij = 0; rij < 3; rij++) {
            for (int kolom = 0; kolom < 3; kolom++) {
                MachineScreen.vak(g, x + TekentafelMenu.ROOSTER_X + kolom * 18, y + TekentafelMenu.ROOSTER_Y + rij * 18, MachineScreen.VAK_IN);
            }
        }
        // the sheet slot: a blue sheet shows through while it is empty
        MachineScreen.vak(g, x + TekentafelMenu.VEL_X, y + TekentafelMenu.VEL_Y, MachineScreen.VAK_IN);
        if (!menu.getSlot(TekentafelMenu.VEL).hasItem()) {
            g.fill(x + TekentafelMenu.VEL_X + 3, y + TekentafelMenu.VEL_Y + 2, x + TekentafelMenu.VEL_X + 13, y + TekentafelMenu.VEL_Y + 14, 0x805B8FD9);
        }
        // the result: a bigger frame, like a crafting table's
        g.fill(x + TekentafelMenu.RESULTAAT_X - 5, y + TekentafelMenu.RESULTAAT_Y - 5, x + TekentafelMenu.RESULTAAT_X + 21, y + TekentafelMenu.RESULTAAT_Y + 21,
                MachineScreen.PANEEL_DONKER);
        MachineScreen.vak(g, x + TekentafelMenu.RESULTAAT_X, y + TekentafelMenu.RESULTAAT_Y, MachineScreen.VAK_UIT);
        MachineScreen.pijl(g, x + 90, y + 35, menu.getSlot(TekentafelMenu.RESULTAAT).hasItem() ? 1f : 0f);
        MachineScreen.inventaris(g, x, y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, MachineScreen.TEKST, false);
        boolean vel = menu.getSlot(TekentafelMenu.VEL).hasItem();
        boolean rooster = false;
        for (int i = 0; i < 9; i++) {
            rooster |= menu.getSlot(TekentafelMenu.ROOSTER + i).hasItem();
        }
        Component melding = null;
        if (!rooster) {
            melding = Component.translatable("gui.guhs.techmachine.tekentafel.leg");
        } else if (!vel) {
            melding = Component.translatable("gui.guhs.techmachine.tekentafel.vel");
        } else if (!menu.getSlot(TekentafelMenu.RESULTAAT).hasItem()) {
            melding = Component.translatable("gui.guhs.techmachine.tekentafel.onbekend");
        }
        g.text(font, melding != null ? melding : playerInventoryTitle, inventoryLabelX, inventoryLabelY, melding != null ? BLAUW : MachineScreen.TEKST, false);
    }
}
