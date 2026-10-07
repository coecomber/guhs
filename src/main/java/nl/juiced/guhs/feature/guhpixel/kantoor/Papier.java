package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import nl.juiced.guhs.taal.Tekst;

/**
 * The three papers of the Guhkantoor: a loonstrookje (per guh, per 8 real hours), a kwartaalrapport (with every third
 * loonstrookje) and the oorkonde "Werknemer van de maand". A paper is an item you read, and a thin block on the wall that
 * keeps the same data; the data is one tag in the stack's custom data under {@link #SLEUTEL}:
 * {@code Soort}, {@code Zaad} (which lines: {@link KantoorTeksten#blad}), {@code Nr}, {@code Naam} (the guh),
 * {@code Baas} (the player), {@code Datum}, and for a report {@code Medewerkers} (names for the bars) and {@code Topper},
 * for a certificate {@code Maand}, {@code Jaar}, {@code Uren}.
 */
public final class Papier {
    public static final String SLEUTEL = "guhs_guhkantoor";

    public enum Soort {
        LOON("loon"), KWARTAAL("kwartaal"), OORKONDE("oorkonde");

        private final String id;

        Soort(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public Item item() {
            return switch (this) {
                case LOON -> KantoorSlice.LOONSTROOKJE_ITEM.get();
                case KWARTAAL -> KantoorSlice.KWARTAALRAPPORT_ITEM.get();
                case OORKONDE -> KantoorSlice.OORKONDE_ITEM.get();
            };
        }

        static Soort van(String id) {
            for (Soort s : values()) {
                if (s.id.equals(id)) {
                    return s;
                }
            }
            return LOON;
        }
    }

    public static Soort soort(CompoundTag papier) {
        return Soort.van(papier.getStringOr("Soort", ""));
    }

    /** The paper's tag (a copy; empty for a blank sheet). */
    public static CompoundTag tag(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag().getCompoundOrEmpty(SLEUTEL);
    }

    /** The stack of a paper with this tag. */
    public static ItemStack stapel(CompoundTag papier) {
        ItemStack s = new ItemStack(soort(papier).item());
        CompoundTag wortel = new CompoundTag();
        wortel.put(SLEUTEL, papier.copy());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(wortel));
        return s;
    }

    private static CompoundTag basis(Soort soort, long zaad, int nr, Component baas) {
        CompoundTag t = new CompoundTag();
        t.putString("Soort", soort.id());
        t.putLong("Zaad", zaad);
        t.putInt("Nr", nr);
        Tekst.put(t, "Baas", baas);
        return t;
    }

    public static ItemStack loon(Component guh, Component baas, int nr, long zaad, String datum) {
        CompoundTag t = basis(Soort.LOON, zaad, nr, baas);
        Tekst.put(t, "Naam", guh);
        t.putString("Datum", datum);
        return stapel(t);
    }

    public static ItemStack kwartaal(Component baas, int nr, long zaad, List<Component> medewerkers, Component topper) {
        CompoundTag t = basis(Soort.KWARTAAL, zaad, nr, baas);
        ListTag lijst = new ListTag();
        for (Component c : medewerkers) {
            CompoundTag m = new CompoundTag();
            Tekst.put(m, "Naam", c);
            lijst.add(m);
        }
        t.put("Medewerkers", lijst);
        Tekst.put(t, "Topper", topper);
        return stapel(t);
    }

    public static ItemStack oorkonde(Component guh, Component baas, int maand, int jaar, int uren, long zaad) {
        CompoundTag t = basis(Soort.OORKONDE, zaad, 1, baas);
        Tekst.put(t, "Naam", guh);
        t.putInt("Maand", maand);
        t.putInt("Jaar", jaar);
        t.putInt("Uren", uren);
        return stapel(t);
    }

    /** A filled-in example (the creative tab, the dev command, the wiki's sample). */
    public static ItemStack voorbeeld(Soort soort, long zaad) {
        Component guh = Component.literal("Vadsje"), baas = Component.literal("Guh");
        return switch (soort) {
            case LOON -> loon(guh, baas, 1, zaad, "01-01-0001");
            case KWARTAAL -> kwartaal(baas, 1, zaad, List.of(guh, Component.literal("Njegje"), Component.literal("Knabbel")), guh);
            case OORKONDE -> oorkonde(guh, baas, 0, 1, 61, zaad);
        };
    }

    private Papier() {
    }
}
