package nl.juiced.guhs.feature.vadsrijk.menu.client;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.vadsrijk.economie.client.EcoScherm;

/**
 * U-TABS: the base of a panel in the Vadsrijk-menu. It is economie's pink EcoScherm (title band, {@code knop}, {@code slot},
 * {@code tip}, {@code munten}, {@code zoekveld}) in the shared menu frame, without the economie server actions: override
 * {@link #doe} when your panel sends something. Draw in {@code inhoud(g, mx, my, pt)} inside left/top/w/h (the band is the top
 * 18 px; w/h grow with the window between {@link VadsrijkMenu#MIN_W}x{@link VadsrijkMenu#MIN_H} and the max).
 */
public abstract class MenuPaneel extends EcoScherm {
    private final String tab;

    protected MenuPaneel(String tab, Component titel, CompoundTag data) {
        super("menu_" + tab, titel, data, List.of(), VadsrijkMenu.MIN_W, VadsrijkMenu.MIN_H);
        this.tab = tab;
    }

    @Override
    protected String collectieTab() {
        return tab;
    }

    @Override
    protected void doe(String actie, CompoundTag args) {
        // (a menu panel sends nothing by default)
    }
}
