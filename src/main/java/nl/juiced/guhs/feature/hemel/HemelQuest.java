package nl.juiced.guhs.feature.hemel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * The wolkenhoeder's questline, per player in {@code GuhQuests.saved(player)}:
 * <ul>
 *   <li>{@value #STAP}: 0 = not met yet, 1 = looking for the three things, 2 = the Knuffelhart beats;</li>
 *   <li>{@value #GEBRACHT}: which of the three things are brought (bits of {@link Ding});</li>
 *   <li>{@value #HART}: the Knuffelhart beats for this player (then reviving is free and unlimited);</li>
 *   <li>{@value #TERUG}: how many guhs came back from the wolkjes (for fun: the hoeder mentions it).</li>
 * </ul>
 * Hearts never go down, nothing is ever taken back: the only thing the quest takes is the three items.
 */
public final class HemelQuest {
    public static final String STAP = "guhs_hemel_stap", GEBRACHT = "guhs_hemel_gebracht", HART = "guhs_hemel_hart", TERUG = "guhs_hemel_terug";

    /** The three things the Knuffelhart needs. */
    public enum Ding {
        /** Voor de glinstering. */
        KRISTAL,
        /** Voor de warmte en de vadsheid. */
        KNABBEL,
        /** Voor de zachtheid. */
        VEERTJE;

        public int bit() {
            return 1 << ordinal();
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Item item() {
            return switch (this) {
                case KRISTAL -> ModItems.GUH_KRISTAL.get();
                case KNABBEL -> nl.juiced.guhs.feature.evenementen.EvenementenFeature.GOUDEN_KAASKNABBEL.get();
                case VEERTJE -> nl.juiced.guhs.feature.vogels.VogelsFeature.PLUISVEERTJE.get();
            };
        }
    }

    private static CompoundTag saved(ServerPlayer p) {
        return GuhQuests.saved(p);
    }

    public static int stap(ServerPlayer p) {
        return saved(p).getIntOr(STAP, 0);
    }

    public static void zetStap(ServerPlayer p, int stap) {
        saved(p).putInt(STAP, stap);
    }

    public static boolean klopt(ServerPlayer p) {
        return saved(p).getBooleanOr(HART, false);
    }

    public static boolean heeft(ServerPlayer p, Ding d) {
        return (saved(p).getIntOr(GEBRACHT, 0) & d.bit()) != 0;
    }

    /** What the Knuffelhart still needs. */
    public static List<Ding> nodig(ServerPlayer p) {
        List<Ding> out = new ArrayList<>();
        for (Ding d : Ding.values()) {
            if (!heeft(p, d)) {
                out.add(d);
            }
        }
        return out;
    }

    /** Takes one of every missing thing the player carries; returns what was brought now. */
    public static List<Ding> breng(ServerPlayer p) {
        List<Ding> gebracht = new ArrayList<>();
        for (Ding d : nodig(p)) {
            if (GuhQuests.count(p, d.item()) > 0) {
                GuhQuests.take(p, d.item(), 1);
                saved(p).putInt(GEBRACHT, saved(p).getIntOr(GEBRACHT, 0) | d.bit());
                GuhAdvancements.grant(p, "hemel_" + d.id());
                gebracht.add(d);
            }
        }
        return gebracht;
    }

    /** The heart beats: the flag, step 2, the outfits (as clothing items, once), the advancements, the client's heart. */
    public static void wakker(ServerPlayer p) {
        if (klopt(p)) {
            return;
        }
        saved(p).putBoolean(HART, true);
        zetStap(p, 2);
        Minigames.give(p, new ItemStack(ModItems.clothingItem(GuhClothes.HEMEL_AUREOOLTJE)));
        Minigames.give(p, new ItemStack(ModItems.clothingItem(GuhClothes.HEMEL_WOLKENVLEUGELTJES)));
        GuhAdvancements.grant(p, "hemel_hart");
        GidsFeature.grant(p, "verhalen/hemel_hart");
        HemelPayloads.status(p);
    }

    public static int terug(ServerPlayer p) {
        return saved(p).getIntOr(TERUG, 0);
    }

    static void telTerug(ServerPlayer p) {
        saved(p).putInt(TERUG, terug(p) + 1);
    }

    /** (tests, ops) forget the whole questline. */
    public static void vergeet(ServerPlayer p) {
        for (String k : List.of(STAP, GEBRACHT, HART, TERUG)) {
            saved(p).remove(k);
        }
        HemelPayloads.status(p);
    }

    private HemelQuest() {
    }
}
