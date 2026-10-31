package nl.juiced.guhs.feature.maagwand.client;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/** The raster of all food still locked, under the text of the maagwand's tooltip. */
record MaagwandTooltip(List<ItemStack> items) implements TooltipComponent {
    private static final int KOLOMMEN = 9, VAK = 17;

    private static int rijen(int n) {
        return (n + KOLOMMEN - 1) / KOLOMMEN;
    }

    record Klant(MaagwandTooltip tip) implements ClientTooltipComponent {
        @Override
        public int getHeight(Font font) {
            return rijen(tip.items.size()) * VAK + 2;
        }

        @Override
        public int getWidth(Font font) {
            return Math.min(tip.items.size(), KOLOMMEN) * VAK;
        }

        @Override
        public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor g) {
            for (int i = 0; i < tip.items.size(); i++) {
                g.item(tip.items.get(i), x + i % KOLOMMEN * VAK, y + i / KOLOMMEN * VAK);
            }
        }
    }
}
