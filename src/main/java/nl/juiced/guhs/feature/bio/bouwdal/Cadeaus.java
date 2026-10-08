package nl.juiced.guhs.feature.bio.bouwdal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * biomes3 bouw-dal: what Evivads and Nielsvads bring back from Japan. One present per trip per player, from three pools:
 * the verzamelreeks (twelve decoration blocks), four outfits for the player's guhs, four kinds of food.
 * <p>
 * How it is chosen ({@link #kies}), so that a collection gets finished:
 * <ul>
 *   <li>the very first present is always a piece of the verzamelreeks;</li>
 *   <li>after that {@link #KANS_ETEN} of the presents is food, {@link #KANS_OUTFIT} an outfit the player does not have
 *       yet, the rest a piece of the reeks; a pool that is complete gives its share to the other;</li>
 *   <li>a piece of the reeks is one the player misses three times out of four, else any piece (so doubles do turn up:
 *       those can be traded with the two for a missing one, {@link #ruil});</li>
 *   <li>with both complete: food, or now and then a double of the reeks to give away.</li>
 * </ul>
 * Every present that is not food brings the player one step further (a double is one step after trading), so the full
 * set of 12 + 4 takes about 16 / (1 - {@link #KANS_ETEN}) = 19 trips (the game test bioBouwDalCadeaus measures it).
 * What a player has had is kept with the player (bit sets, keys guhs_bio_bouw_dal_*): it counts what was GIVEN, not
 * what is still in the inventory.
 */
public final class Cadeaus {
    public static final String[] REEKS = {"japan_geluksguh", "japan_lampion", "japan_mini_torii", "japan_ramenkom", "japan_daruma_guh",
            "japan_waaier", "japan_theeservies", "japan_kokeshi_guh", "japan_koinobori", "japan_bonsai_schaaltje", "japan_windgong",
            "japan_maneki_knabbel"};
    public static final GuhClothes[] OUTFITS = {GuhClothes.JAPAN_KIMONO, GuhClothes.JAPAN_HACHIMAKI, GuhClothes.JAPAN_KATTENOORTJES,
            GuhClothes.JAPAN_STRIKJE};
    public static final String[] ETEN = {"japan_sushi", "japan_ramen", "japan_mochi", "japan_onigiri"};
    public static final double KANS_ETEN = 0.15, KANS_OUTFIT = 0.20, KANS_MIST = 0.75;
    public static final String REEKS_KEY = "guhs_bio_bouw_dal_reeks", OUTFIT_KEY = "guhs_bio_bouw_dal_outfits", AANTAL_KEY = "guhs_bio_bouw_dal_cadeaus";
    public static final int REEKS_VOL = (1 << REEKS.length) - 1, OUTFITS_VOL = (1 << OUTFITS.length) - 1;

    public enum Pool {
        REEKS, OUTFIT, ETEN
    }

    public record Cadeau(Pool pool, int index) {
    }

    /** The next present for a player who has these pieces (bit sets) and had this many presents before. */
    public static Cadeau kies(int reeks, int outfits, int aantal, RandomSource random) {
        boolean reeksVol = (reeks & REEKS_VOL) == REEKS_VOL, outfitsVol = (outfits & OUTFITS_VOL) == OUTFITS_VOL;
        if (aantal == 0 && !reeksVol) {
            return new Cadeau(Pool.REEKS, mist(reeks, REEKS.length, random));
        }
        if (reeksVol && outfitsVol) {
            return random.nextDouble() < 0.5 ? new Cadeau(Pool.ETEN, random.nextInt(ETEN.length)) : new Cadeau(Pool.REEKS, random.nextInt(REEKS.length));
        }
        double worp = random.nextDouble();
        if (worp < KANS_ETEN) {
            return new Cadeau(Pool.ETEN, random.nextInt(ETEN.length));
        }
        if (!outfitsVol && (reeksVol || worp < KANS_ETEN + KANS_OUTFIT)) {
            return new Cadeau(Pool.OUTFIT, mist(outfits, OUTFITS.length, random));
        }
        if (random.nextDouble() < KANS_MIST) {
            return new Cadeau(Pool.REEKS, mist(reeks, REEKS.length, random));
        }
        return new Cadeau(Pool.REEKS, random.nextInt(REEKS.length));
    }

    /** A random index whose bit is not set (any index when all are set). */
    static int mist(int bits, int n, RandomSource random) {
        List<Integer> vrij = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if ((bits & (1 << i)) == 0) {
                vrij.add(i);
            }
        }
        return vrij.isEmpty() ? random.nextInt(n) : vrij.get(random.nextInt(vrij.size()));
    }

    public static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Guhs.id(id));
    }

    public static ItemStack stapel(Cadeau c) {
        return switch (c.pool()) {
            case REEKS -> new ItemStack(item(REEKS[c.index()]));
            case OUTFIT -> new ItemStack(ModItems.clothingItem(OUTFITS[c.index()]));
            case ETEN -> new ItemStack(item(ETEN[c.index()]), 4);
        };
    }

    public static int reeks(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(REEKS_KEY, 0);
    }

    public static int outfits(ServerPlayer p) {
        int bits = GuhQuests.saved(p).getIntOr(OUTFIT_KEY, 0);
        for (int i = 0; i < OUTFITS.length; i++) {
            if (KledingUnlocks.heeft(p, OUTFITS[i])) {
                bits |= 1 << i;      // (got it some other way, a creative gift for instance: no need to bring it again)
            }
        }
        return bits;
    }

    public static int aantal(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(AANTAL_KEY, 0);
    }

    /** Is this stack a piece of the verzamelreeks? Its index, or -1. */
    public static int reeksIndex(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (int i = 0; i < REEKS.length; i++) {
            if (REEKS[i].equals(id) && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Guhs.MODID)) {
                return i;
            }
        }
        return -1;
    }

    /** Chooses the player's next present, hands it over and writes it down; returns what it was. */
    public static Cadeau geef(ServerPlayer p, RandomSource random) {
        Cadeau c = kies(reeks(p), outfits(p), aantal(p), random);
        CompoundTag saved = GuhQuests.saved(p);
        saved.putInt(AANTAL_KEY, aantal(p) + 1);
        if (c.pool() == Pool.REEKS) {
            gekregen(p, c.index());
        } else if (c.pool() == Pool.OUTFIT) {
            saved.putInt(OUTFIT_KEY, saved.getIntOr(OUTFIT_KEY, 0) | 1 << c.index());
            if ((outfits(p) & OUTFITS_VOL) == OUTFITS_VOL) {
                GuhAdvancements.grant(p, "japan_outfits_compleet");
            }
        }
        geefStapel(p, stapel(c));
        GuhAdvancements.grant(p, "weeb_cadeau");
        return c;
    }

    /** Writes down that the player got this piece of the reeks (the proof advancements for slice systemen). */
    static void gekregen(ServerPlayer p, int index) {
        CompoundTag saved = GuhQuests.saved(p);
        int bits = saved.getIntOr(REEKS_KEY, 0) | 1 << index;
        saved.putInt(REEKS_KEY, bits);
        GuhAdvancements.grant(p, REEKS[index]);
        if ((bits & REEKS_VOL) == REEKS_VOL) {
            GuhAdvancements.grant(p, "japan_reeks_compleet");
        }
    }

    static void geefStapel(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack) || !stack.isEmpty()) {
            p.drop(stack, false);
        }
    }

    public enum Ruil {
        /** Not a piece of the reeks in the hand. */
        GEEN,
        /** The player has only this one: it stays. */
        ENIGE,
        /** Nothing is missing any more. */
        COMPLEET,
        GERUILD
    }

    public record Uitkomst(Ruil soort, int gekregen) {
    }

    /**
     * Trades the double in the player's hand for a piece the player misses. A double = the player carries at least two
     * of it. Returns what happened and the index of the piece that was given (-1: none).
     */
    public static Uitkomst ruil(ServerPlayer p, ItemStack hand, RandomSource random) {
        int index = reeksIndex(hand);
        if (index < 0) {
            return new Uitkomst(Ruil.GEEN, -1);
        }
        int bits = reeks(p) | 1 << index;          // (whatever is in the hand counts as had)
        if ((bits & REEKS_VOL) == REEKS_VOL) {
            GuhQuests.saved(p).putInt(REEKS_KEY, bits);
            return new Uitkomst(Ruil.COMPLEET, -1);
        }
        if (p.getInventory().countItem(hand.getItem()) < 2) {
            return new Uitkomst(Ruil.ENIGE, -1);
        }
        GuhQuests.saved(p).putInt(REEKS_KEY, bits);
        int nieuw = mist(bits, REEKS.length, random);
        hand.shrink(1);
        gekregen(p, nieuw);
        geefStapel(p, new ItemStack(item(REEKS[nieuw])));
        return new Uitkomst(Ruil.GERUILD, nieuw);
    }

    /** (For the texts and the tests) the Items stand-in when an id is missing. */
    static boolean bestaat(String id) {
        return item(id) != Items.AIR;
    }

    private Cadeaus() {
    }
}
