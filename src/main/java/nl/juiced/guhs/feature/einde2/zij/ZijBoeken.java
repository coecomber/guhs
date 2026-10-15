package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 zij: the books of the Bibliotheek van Wan Shi Guh (DESIGN 4: "ruil een stukje kennis voor nieuwe Guhboeken").
 * Six books of its own about the Guheinde and the four peoples, written books with translated pages
 * ({@code book.guhs.einde2_zij.<id>.<page>}, tools/features/einde2_zij_tekst.py {@code BOEKEN}); after those six the owl
 * hands out the library's older Guhboeken. Which a player got is a bit field in {@code guhs_einde2_zij_boeken}.
 */
public final class ZijBoeken {
    /** The books in the order the owl gives them: id and page count. */
    public enum Boek {
        VIER_VOLKEREN("vier_volkeren", 4), ZON_GING_VADSEN("zon_ging_vadsen", 3), LUCHTBIZONS("luchtbizons", 3),
        KNABBELKOOR("knabbelkoor", 3), GROTE_ZINGENDE("grote_zingende", 3), DE_EERSTE("de_eerste", 4);

        public final String id;
        public final int paginas;

        Boek(String id, int paginas) {
            this.id = id;
            this.paginas = paginas;
        }

        public String sleutel() {
            return "book.guhs.einde2_zij." + id;
        }
    }

    public static final String TAG = "GuhsEinde2Boek", GEKREGEN = "guhs_einde2_zij_boeken";

    private ZijBoeken() {
    }

    /** A fresh copy of a book of the library. */
    public static ItemStack stapel(Boek b) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> paginas = new ArrayList<>();
        for (int i = 0; i < b.paginas; i++) {
            paginas.add(Filterable.passThrough(Component.translatable(b.sleutel() + "." + i)));
        }
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Wan Shi Guh " + (b.ordinal() + 1)), "", 0,
                paginas, true));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(b.sleutel() + ".titel")
                .withStyle(Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(0xE8C27A))));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("gui.guhs.einde2_zij.boek.door")
                .withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GRAY)))));
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG, b.id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        return stack;
    }

    /** Has this player been given this book? */
    public static boolean heeft(ServerPlayer p, Boek b) {
        return (GuhQuests.saved(p).getIntOr(GEKREGEN, 0) & (1 << b.ordinal())) != 0;
    }

    /** The next book for this player: the first of ours they never got, else one of the older Guhboeken (never the secret one). */
    public static ItemStack volgende(ServerPlayer p) {
        Boek b = eerstNieuw(p);
        if (b != null) {
            GuhQuests.saved(p).putInt(GEKREGEN, GuhQuests.saved(p).getIntOr(GEKREGEN, 0) | (1 << b.ordinal()));
            return stapel(b);
        }
        RandomSource r = p.getRandom();
        Guhboek g;
        do {
            g = Guhboek.values()[r.nextInt(Guhboek.values().length)];
        } while (g.secret());
        return g.stack();
    }

    @Nullable
    static Boek eerstNieuw(ServerPlayer p) {
        for (Boek b : Boek.values()) {
            if (!heeft(p, b)) {
                return b;
            }
        }
        return null;
    }
}
