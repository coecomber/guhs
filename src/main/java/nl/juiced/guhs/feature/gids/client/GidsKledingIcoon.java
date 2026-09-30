package nl.juiced.guhs.feature.gids.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.gids.GidsData;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.registry.ModItems;

/**
 * One clothing piece as a little square in the Guhdex (2.9): colourful with a green border when you've unlocked it, grey
 * when it's still locked (not a secret: the tooltip tells its name, source and price); the kapper's hairstyles are
 * colourful with a soft pink border ("bij de kapper").
 */
public final class GidsKledingIcoon {
    public static final int CEL = 20;

    public static ItemStack item(GuhClothes c) {
        try {
            return new ItemStack(ModItems.clothingItem(c));
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean heeft(GuhClothes c) {
        return KledingUnlocks.Client.heeft(c);
    }

    /** Draws the square (CEL - 2 big) at (x, y). */
    public static void teken(GuiGraphics g, GuhClothes c, int x, int y, boolean hover) {
        int s = CEL - 2;
        boolean kapsel = !GidsData.ontgrendelbaar(c);
        boolean open = kapsel || heeft(c);
        int rand = kapsel ? 0xFFF3A6C4 : open ? 0xFF4CC06A : 0xFF9A8A92;
        int binnen = kapsel ? 0x40F7B6CB : open ? 0x5068D88A : 0xFF6E6269;
        if (hover) {
            rand = open ? 0xFFFFFFFF : 0xFFD8CCD2;
        }
        g.fill(x, y, x + s, y + s, rand);
        g.fill(x + 1, y + 1, x + s - 1, y + s - 1, binnen);
        g.renderItem(item(c), x + 1, y + 1);
        if (!open) {
            // locked: a grey veil over the icon (drawn above the item)
            g.pose().pushPose();
            g.pose().translate(0, 0, 250);
            g.fill(x + 1, y + 1, x + s - 1, y + s - 1, 0xB08A8088);
            g.pose().popPose();
        }
    }

    /** The tooltip: name, source, price and whether you have it. */
    public static List<Component> tip(GuhClothes c) {
        List<Component> out = new ArrayList<>();
        ItemStack stack = item(c);
        out.add((stack.isEmpty() ? Component.literal(c.id()) : stack.getHoverName().copy()).withStyle(ChatFormatting.BOLD));
        if (!GidsData.ontgrendelbaar(c)) {
            out.add(Component.translatable("gui.guhs.gids.tip.kapper").withStyle(ChatFormatting.LIGHT_PURPLE));
            return out;
        }
        String bron = KledingBronnen.bron(c);
        out.add(Component.translatable("gui.guhs.gids.tip.bron", bron == null ? Component.translatable("gui.guhs.gids.zonder_bron")
                : Component.translatable("gui.guhs.kledingbron." + bron)).withStyle(ChatFormatting.GRAY));
        String prijs = KledingBronnen.prijs(c);
        if (prijs != null && !prijs.isEmpty()) {
            out.add(Component.translatable("gui.guhs.gids.tip.prijs", prijs).withStyle(ChatFormatting.GOLD));
        }
        out.add(heeft(c) ? Component.translatable("gui.guhs.gids.tip.ontgrendeld").withStyle(ChatFormatting.GREEN)
                : Component.translatable("gui.guhs.gids.tip.op_slot").withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }

    private GidsKledingIcoon() {
    }
}
