package nl.juiced.guhs.feature.techmachine.client;

import javax.annotation.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.techmachine.Bouwtekening;
import nl.juiced.guhs.feature.techmachine.Bouwtekeningen;
import nl.juiced.guhs.feature.techmachine.KnutselmachineBlockEntity;
import nl.juiced.guhs.feature.techmachine.MachineMenu;
import nl.juiced.guhs.feature.techmachine.MachineSoort;
import nl.juiced.guhs.feature.techmachine.PlantagebakBlockEntity;

/**
 * The screen of every machine of this slice (drawn, no texture: the pink panel of the Bank Guh): its slots (out slots a
 * shade lighter), an arrow or a bar that fills while it works, and above the inventory one line that says what it is
 * waiting for (no vadskracht, no drawing, something missing, full...). The Knutselmachine also shows what its drawing makes.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    static final int RAND = 0xFF3A1F2C, PANEEL = 0xFFF7D4E0, PANEEL_DONKER = 0xFFD99AB2, VAK = 0xFF8B5A6E, VAK_IN = 0xFFB98398, VAK_UIT = 0xFFCDA3B4,
            TEKST = 0xFF5A2640, ROOD = 0xFFB3261E, GROEN = 0xFF2E7D32, PIJL = 0xFFE86AA6;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    static void paneel(GuiGraphicsExtractor g, int x, int y, int breed, int hoog) {
        g.fill(x - 1, y - 1, x + breed + 1, y + hoog + 1, RAND);
        g.fill(x, y, x + breed, y + hoog, PANEEL);
    }

    static void vak(GuiGraphicsExtractor g, int x, int y, int binnen) {
        g.fill(x - 1, y - 1, x + 17, y + 17, VAK);
        g.fill(x, y, x + 16, y + 16, binnen);
    }

    static void inventaris(GuiGraphicsExtractor g, int x, int y) {
        for (int rij = 0; rij < 3; rij++) {
            for (int kolom = 0; kolom < 9; kolom++) {
                vak(g, x + MachineMenu.INV_X + kolom * 18, y + MachineMenu.INV_Y + rij * 18, VAK_IN);
            }
        }
        for (int kolom = 0; kolom < 9; kolom++) {
            vak(g, x + MachineMenu.INV_X + kolom * 18, y + MachineMenu.INV_Y + 58, VAK_IN);
        }
    }

    /** An arrow of 22 x 15 that fills from the left ({@code deel} 0..1). */
    static void pijl(GuiGraphicsExtractor g, int x, int y, float deel) {
        int vol = Math.round(Math.max(0f, Math.min(1f, deel)) * 22);
        for (int i = 0; i < 22; i++) {
            // a shaft of 5 high, then a head that narrows to a point
            int half = i < 14 ? 2 : Math.max(0, 7 - (i - 14));
            g.fill(x + i, y + 7 - half, x + i + 1, y + 8 + half, i < vol ? PIJL : PANEEL_DONKER);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        MachineSoort soort = menu.soort;
        paneel(g, x, y, imageWidth, imageHeight);
        for (int i = 0; i < soort.vakken; i++) {
            vak(g, x + soort.x(i), y + soort.y(i), soort.invoer(i) ? VAK_IN : VAK_UIT);
        }
        inventaris(g, x, y);
        float deel = menu.duur() > 0 ? (float) menu.voortgang() / menu.duur() : 0f;
        if (soort.pijlX() >= 0) {
            pijl(g, x + soort.pijlX(), y + 35, deel);
        } else if (soort == MachineSoort.PLANTAGEBAK) {
            // a bar under the sapling that turns green while the tree grows
            int bx = x + 62, by = y + 58, breed = 52;
            g.fill(bx - 1, by - 1, bx + breed + 1, by + 5, VAK);
            g.fill(bx, by, bx + breed, by + 4, PANEEL_DONKER);
            g.fill(bx, by, bx + Math.round(deel * breed), by + 4, GROEN);
        } else {
            // a bar next to the nine slots that fills from the bottom
            int bx = x + 122, by = y + 17, hoog = 52;
            g.fill(bx - 1, by - 1, bx + 5, by + hoog + 1, VAK);
            g.fill(bx, by, bx + 4, by + hoog, PANEEL_DONKER);
            g.fill(bx, by + hoog - Math.round(deel * hoog), bx + 4, by + hoog, PIJL);
        }
        if (soort == MachineSoort.KNUTSELMACHINE) {
            Bouwtekening tekening = tekening();
            if (tekening != null) {
                g.item(tekening.resultaat(), x + 116, y + 17);
                g.itemDecorations(font, tekening.resultaat(), x + 116, y + 17);
            }
        }
    }

    @Nullable
    private Bouwtekening tekening() {
        return menu.soort == MachineSoort.KNUTSELMACHINE ? Bouwtekeningen.lees(menu.getSlot(KnutselmachineBlockEntity.TEKENING).getItem()) : null;
    }

    /** What the machine is waiting for, or null when all is well. */
    @Nullable
    private Component melding() {
        String k = "gui.guhs.techmachine.scherm.";
        if (!menu.kracht()) {
            return Component.translatable(k + "geen_kracht");
        }
        int stand = menu.stand();
        if (menu.soort == MachineSoort.KNUTSELMACHINE) {
            return switch (stand) {
                case KnutselmachineBlockEntity.GEEN_TEKENING -> Component.translatable(k + "geen_tekening");
                case KnutselmachineBlockEntity.MIST -> Component.translatable(k + "mist");
                case KnutselmachineBlockEntity.VOL -> Component.translatable(k + "vol");
                case KnutselmachineBlockEntity.ONBEKEND -> Component.translatable(k + "onbekend");
                default -> null;
            };
        }
        if (menu.soort == MachineSoort.PLANTAGEBAK) {
            if (stand >= 8) {
                return Component.translatable(k + "wil_niet");
            }
            return stand == PlantagebakBlockEntity.Stand.BOOM.ordinal() ? Component.translatable(k + "boom")
                    : stand == PlantagebakBlockEntity.Stand.LEEG.ordinal() && !menu.getSlot(0).hasItem() ? Component.translatable(k + "zaailing") : null;
        }
        return null;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEKST, false);
        // one line above the inventory: what the machine is waiting for, or (all is well) just "Inventory"
        Component melding = melding();
        if (melding != null) {
            g.text(font, melding, inventoryLabelX, inventoryLabelY, menu.kracht() ? TEKST : ROOD, false);
        } else {
            g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEKST, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        Bouwtekening tekening = tekening();
        if (tekening != null && isHovering(116, 17, 16, 16, mouseX, mouseY) && menu.getCarried().isEmpty()) {
            ItemStack maakt = tekening.resultaat();
            g.setTooltipForNextFrame(font, font.split(Component.translatable("item.guhs.bouwtekening.maakt", maakt.getCount(), maakt.getHoverName()), 170),
                    mouseX, mouseY);
        }
    }
}
