package nl.juiced.guhs.feature.bouwhulp.blauwdruk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.vormen.Daken.Dak;

import static nl.juiced.guhs.feature.bouwhulp.blauwdruk.Bouwdoos.LUCHT;
import static nl.juiced.guhs.feature.bouwhulp.blauwdruk.Bouwdoos.g;

/**
 * The catalogue of ready blueprints (design 2.6, ronde B11/B12): about 25 in four groups (safari buildings, also sold in
 * the giftshop; guh houses; garden and outdoors; towers and specials), each with its picture, and room for more ("meer
 * hier binnenkort"). Every design faces south (its door at the high z side). Text keys: gui.guhs.blauwdruk.cat.&lt;id&gt;.
 * Catalogue designs have no size limit (they are hand-picked); own and library ones keep the level's.
 */
public final class Catalogus {
    public static final List<String> GROEPEN = List.of("safari", "huizen", "tuin", "torens");

    public record Ontwerpje(String id, String groep, Supplier<Ontwerp> bouw) {
    }

    private static final Map<String, Ontwerpje> ALLE = new LinkedHashMap<>();
    private static final Map<String, Blauwdruk> GEMAAKT = new java.util.concurrent.ConcurrentHashMap<>();

    /** The six park buildings sold as blueprints in the safari giftshop (design 2.9). */
    public static final List<String> SAFARI = List.of("lodge_hutje", "rangerpost", "acacia_uitkijktoren", "watergat_terras", "safaripoort",
            "rieten_paviljoen");

    private static void c(String id, String groep, Supplier<Ontwerp> bouw) {
        ALLE.put(id, new Ontwerpje(id, groep, bouw));
    }

    public static List<Ontwerpje> lijst() {
        return new ArrayList<>(ALLE.values());
    }

    /** How many placeholder spots ("meer hier binnenkort") the list shows after the real ones. */
    public static final int PLAATSHOUDERS = 3;

    /** A catalogue design as a blueprint (made once). Null for an unknown id. */
    @Nullable
    public static Blauwdruk get(String id) {
        Ontwerpje o = ALLE.get(id);
        if (o == null) {
            return null;
        }
        return GEMAAKT.computeIfAbsent(id, k -> {
            Ontwerp ontwerp = o.bouw().get();
            return new Blauwdruk("c:" + id, "", null, "Guhs", 0L, ontwerp, Voorbeeld.maak(ontwerp));
        });
    }

    // =====================================================================================================================
    // the materials
    // =====================================================================================================================

    private static final Block RIET = g("rieten_dak", Blocks.HAY_BLOCK), RIET_TRAP = g("rieten_dak_trap", Blocks.OAK_STAIRS),
            BAMBOE = g("bamboewand", Blocks.BAMBOO_PLANKS), ACACIA = g("safari_acaciaplanken", Blocks.ACACIA_PLANKS),
            OMHEINING = g("bamboe_omheining", Blocks.BAMBOO_FENCE), BANKJE = g("guh_bankje_hout", Blocks.OAK_STAIRS),
            GLAS_GUH = g("glasinlood_guh_ruit", Blocks.PINK_STAINED_GLASS_PANE), GLAS_HART = g("glasinlood_hartje_ruit", Blocks.PINK_STAINED_GLASS_PANE),
            PATRIJS = g("patrijspoort_koper", Blocks.GLASS), DAKPAN = g("dakpan_roze", Blocks.BRICKS),
            DAKPAN_BLAUW = g("dakpan_hemelsblauw", Blocks.PRISMARINE_BRICKS), DAKPAN_MINT = g("dakpan_mint", Blocks.PRISMARINE),
            HEKJE = g("guh_hekje_wit", Blocks.BIRCH_FENCE), MAT = g("deurmat_welkom", Blocks.PINK_CARPET),
            SCHOORSTEEN = g("guh_schoorsteen", Blocks.BRICKS), VOGELHUISJE = g("vogelhuisje", Blocks.OAK_FENCE),
            TEGEL = g("tegel_guhgezicht", Blocks.WHITE_CONCRETE), STOEL = g("guh_stoel", Blocks.OAK_STAIRS), TAFEL = g("guh_tafel", Blocks.OAK_FENCE);

    private static Block pastel(String kleur, Block anders) {
        return g("pastelbaksteen_" + kleur, anders);
    }

    private static Block pleister(String kleur, Block anders) {
        return g("pleister_" + kleur, anders);
    }

    private static final Direction Z = Direction.SOUTH, N = Direction.NORTH, O = Direction.EAST, W = Direction.WEST;

    // =====================================================================================================================
    // the designs
    // =====================================================================================================================

    static {
        // --- safari buildings (also in the giftshop) ---
        c("lodge_hutje", "safari", () -> {
            Bouwdoos b = new Bouwdoos();
            for (int[] p : new int[][] {{0, 0}, {6, 0}, {0, 6}, {6, 6}}) {
                b.vul(p[0], 0, p[1], p[0], 3, p[1], Blocks.ACACIA_LOG);
            }
            b.vul(0, 4, 0, 6, 4, 6, ACACIA).vul(0, 4, 7, 6, 4, 8, ACACIA);
            b.huis(1, 1, 5, 5, 4, 3, ACACIA, BAMBOE, Blocks.ACACIA_LOG);
            b.deur(3, 5, 5, Z, Blocks.ACACIA_DOOR).zet(1, 6, 3, Blocks.GLASS_PANE).zet(5, 6, 3, Blocks.GLASS_PANE);
            b.ring(0, 5, 7, 6, 5, 8, OMHEINING).vul(1, 5, 7, 5, 5, 7, LUCHT).zet(3, 5, 8, LUCHT);
            b.vul(3, 0, 8, 3, 3, 8, Blocks.ACACIA_LOG).ladder(3, 0, 4, 9, Z).zet(3, 4, 8, ACACIA);
            b.lamp(3, 7, 3, true);
            return b.dak(Dak.ZADEL, 1, 1, 5, 5, 8, RIET, BAMBOE, 1, true, null).ontwerp();
        });
        c("rangerpost", "safari", () -> {
            Bouwdoos b = new Bouwdoos();
            b.vul(0, 0, 0, 8, 0, 6, Blocks.COBBLESTONE);
            b.huis(0, 0, 8, 6, 1, 4, ACACIA, Blocks.STRIPPED_ACACIA_LOG, Blocks.ACACIA_LOG);
            b.deur(4, 2, 6, Z, Blocks.ACACIA_DOOR).zet(2, 3, 6, Blocks.GLASS_PANE).zet(6, 3, 6, Blocks.GLASS_PANE)
                    .zet(0, 3, 3, Blocks.GLASS_PANE).zet(8, 3, 3, Blocks.GLASS_PANE);
            b.zet(1, 2, 1, TAFEL).zet(2, 2, 1, STOEL).zet(7, 2, 1, Blocks.BARREL).zet(7, 2, 2, Blocks.CARTOGRAPHY_TABLE).lamp(4, 5, 3, true);
            b.vul(10, 0, 5, 10, 8, 5, Blocks.ACACIA_FENCE).zet(10, 8, 6, Blocks.GREEN_WOOL).zet(10, 7, 6, Blocks.GREEN_WOOL);
            b.zet(4, 1, 7, MAT);
            return b.dak(Dak.ZADEL, 0, 0, 8, 6, 6, RIET, Blocks.STRIPPED_ACACIA_LOG, 1, true, null).ontwerp();
        });
        c("acacia_uitkijktoren", "safari", () -> {
            Bouwdoos b = new Bouwdoos();
            for (int[] p : new int[][] {{0, 0}, {4, 0}, {0, 4}, {4, 4}}) {
                b.vul(p[0], 0, p[1], p[0], 8, p[1], Blocks.ACACIA_LOG);
            }
            b.vul(0, 8, 0, 4, 8, 4, ACACIA).ring(0, 9, 0, 4, 9, 4, OMHEINING).zet(2, 9, 4, LUCHT);
            b.vul(2, 0, 4, 2, 7, 4, Blocks.ACACIA_LOG).ladder(2, 1, 8, 5, Z).zet(2, 9, 4, LUCHT);
            for (int[] p : new int[][] {{0, 0}, {4, 0}, {0, 4}, {4, 4}}) {
                b.vul(p[0], 9, p[1], p[0], 10, p[1], Blocks.ACACIA_FENCE);
            }
            b.zet(1, 9, 1, STOEL).zet(3, 9, 1, STOEL).lamp(2, 11, 2, true);
            return b.dak(Dak.SCHILD, 0, 0, 4, 4, 11, RIET, BAMBOE, 1, false, null).ontwerp();
        });
        c("watergat_terras", "safari", () -> {
            Bouwdoos b = new Bouwdoos();
            for (int x = 0; x <= 10; x += 5) {
                for (int z = 0; z <= 6; z += 6) {
                    b.vul(x, 0, z, x, 1, z, Blocks.ACACIA_LOG);
                }
            }
            b.vul(0, 2, 0, 10, 2, 6, ACACIA).ring(0, 3, 0, 10, 3, 6, OMHEINING).vul(4, 3, 6, 6, 3, 6, LUCHT);
            b.trap(4, 1, 7, Blocks.ACACIA_STAIRS, N, false).trap(5, 1, 7, Blocks.ACACIA_STAIRS, N, false).trap(6, 1, 7, Blocks.ACACIA_STAIRS, N, false);
            b.gericht(2, 3, 1, BANKJE, Z).gericht(3, 3, 1, BANKJE, Z).gericht(7, 3, 1, BANKJE, Z).gericht(8, 3, 1, BANKJE, Z);
            b.zet(5, 3, 3, Blocks.ACACIA_FENCE).zet(5, 4, 3, Blocks.ACACIA_FENCE).zet(5, 5, 3, Blocks.ACACIA_FENCE);
            b.vul(4, 6, 2, 6, 6, 4, Blocks.ORANGE_WOOL).zet(5, 7, 3, Blocks.ORANGE_WOOL);
            return b.zet(1, 3, 5, Blocks.POTTED_ACACIA_SAPLING).zet(9, 3, 5, Blocks.POTTED_ACACIA_SAPLING).ontwerp();
        });
        c("safaripoort", "safari", () -> {
            Bouwdoos b = new Bouwdoos();
            for (int x : new int[] {0, 8}) {
                b.vul(x, 0, 0, x + 2, 6, 2, BAMBOE).vul(x, 0, 0, x, 6, 0, Blocks.ACACIA_LOG).vul(x + 2, 0, 0, x + 2, 6, 0, Blocks.ACACIA_LOG)
                        .vul(x, 0, 2, x, 6, 2, Blocks.ACACIA_LOG).vul(x + 2, 0, 2, x + 2, 6, 2, Blocks.ACACIA_LOG);
                b.dak(Dak.TENT, x, 0, x + 2, 2, 7, RIET, BAMBOE, 0, false, null);
                b.lamp(x + 1, 4, 3, false);
            }
            b.vul(3, 6, 1, 7, 7, 1, Blocks.STRIPPED_ACACIA_LOG).vul(4, 8, 1, 6, 8, 1, Blocks.ORANGE_TERRACOTTA)
                    .zet(5, 9, 1, Blocks.YELLOW_TERRACOTTA).zet(4, 9, 1, Blocks.PINK_WOOL).zet(6, 9, 1, Blocks.PINK_WOOL);
            return b.ontwerp();
        });
        c("rieten_paviljoen", "safari", () -> {
            Bouwdoos b = new Bouwdoos();
            b.vul(0, 0, 0, 8, 0, 8, ACACIA);
            for (int[] p : new int[][] {{0, 0}, {8, 0}, {0, 8}, {8, 8}}) {
                b.vul(p[0], 1, p[1], p[0], 3, p[1], Blocks.ACACIA_LOG);
            }
            b.zet(4, 1, 4, TAFEL).zet(4, 1, 3, STOEL).zet(4, 1, 5, STOEL).zet(3, 1, 4, STOEL).zet(5, 1, 4, STOEL);
            b.gericht(1, 1, 2, BANKJE, O).gericht(1, 1, 6, BANKJE, O).gericht(7, 1, 2, BANKJE, W).gericht(7, 1, 6, BANKJE, W);
            b.lamp(4, 4, 4, true);
            return b.dak(Dak.SCHILD, 0, 0, 8, 8, 4, RIET, BAMBOE, 1, false, null).ontwerp();
        });

        // --- guh houses ---
        c("guh_huisje_klein", "huizen", () -> {
            Bouwdoos b = new Bouwdoos();
            b.huis(0, 0, 6, 6, 0, 3, Blocks.SPRUCE_PLANKS, pastel("roze", Blocks.PINK_TERRACOTTA), Blocks.STRIPPED_OAK_LOG);
            b.deur(3, 1, 6, Z, Blocks.OAK_DOOR).zet(1, 2, 6, GLAS_GUH).zet(5, 2, 6, GLAS_GUH).zet(0, 2, 3, GLAS_HART).zet(6, 2, 3, GLAS_HART);
            b.zet(3, 0, 7, MAT).lamp(3, 3, 3, true).zet(1, 1, 1, g("knuffelblok_roze", Blocks.PINK_WOOL)).zet(5, 1, 1, TAFEL).zet(5, 1, 2, STOEL);
            return b.dak(Dak.OORTJES, 0, 0, 6, 6, 4, DAKPAN, pleister("vanille", Blocks.WHITE_TERRACOTTA), 1, true, Blocks.PINK_WOOL).ontwerp();
        });
        c("guh_huis_middel", "huizen", () -> {
            Bouwdoos b = new Bouwdoos();
            Block muur = pleister("perzik", Blocks.ORANGE_TERRACOTTA);
            b.huis(0, 0, 8, 8, 0, 8, Blocks.OAK_PLANKS, muur, Blocks.STRIPPED_BIRCH_LOG);
            b.vul(1, 4, 1, 7, 4, 7, Blocks.OAK_PLANKS).vul(1, 4, 1, 1, 4, 3, LUCHT).ladder(1, 1, 4, 1, Z);
            b.deur(4, 1, 8, Z, Blocks.BIRCH_DOOR);
            for (int x : new int[] {2, 6}) {
                b.zet(x, 2, 8, GLAS_HART).zet(x, 6, 8, GLAS_GUH).zet(x, 6, 0, GLAS_GUH).zet(x, 2, 0, GLAS_HART);
            }
            b.zet(4, 6, 8, GLAS_GUH).zet(0, 2, 4, GLAS_HART).zet(8, 2, 4, GLAS_HART).zet(0, 6, 4, GLAS_HART).zet(8, 6, 4, GLAS_HART);
            b.lamp(4, 3, 4, true).lamp(4, 7, 4, true).zet(4, 0, 9, MAT).zet(7, 5, 7, g("knuffelblok_lavendel", Blocks.PURPLE_WOOL));
            b.dak(Dak.ZADEL, 0, 0, 8, 8, 9, DAKPAN_BLAUW, muur, 1, true, null);
            return b.vul(6, 9, 2, 6, 13, 2, SCHOORSTEEN).ontwerp();
        });
        c("guh_villa_groot", "huizen", () -> {
            Bouwdoos b = new Bouwdoos();
            Block muur = pleister("vanille", Blocks.WHITE_TERRACOTTA), hoek = pastel("roze", Blocks.PINK_TERRACOTTA);
            b.huis(0, 0, 14, 10, 0, 8, Blocks.BIRCH_PLANKS, muur, hoek);
            b.vul(1, 4, 1, 13, 4, 9, Blocks.BIRCH_PLANKS).vul(1, 4, 1, 3, 4, 2, LUCHT);
            for (int i = 0; i < 4; i++) {
                b.trap(1 + i, 1 + i, 2, Blocks.BIRCH_STAIRS, O, false);
            }
            b.deur(7, 1, 10, Z, Blocks.BIRCH_DOOR).zet(6, 1, 11, Blocks.PINK_CARPET).zet(7, 0, 11, MAT);
            for (int x = 2; x <= 12; x += 2) {
                if (x != 6 && x != 8) {
                    b.zet(x, 2, 10, GLAS_GUH).zet(x, 2, 0, GLAS_HART);
                }
                b.zet(x, 6, 10, GLAS_GUH).zet(x, 6, 0, GLAS_HART);
            }
            b.lamp(4, 3, 5, true).lamp(10, 3, 5, true).lamp(7, 7, 5, true);
            b.dak(Dak.SCHILD, 0, 0, 14, 10, 9, DAKPAN, muur, 1, false, null);
            // the ear tower at the front right
            b.cilinder(12, 9, 2.4, 0, 12, hoek, true).vul(12, 0, 9, 12, 0, 9, Blocks.BIRCH_PLANKS);
            b.zet(12, 6, 11, GLAS_HART).zet(12, 10, 11, GLAS_HART);
            b.dak(Dak.TENT, 10, 7, 14, 11, 13, DAKPAN, muur, 0, false, null);
            b.vul(10, 16, 9, 10, 18, 9, Blocks.PINK_WOOL).vul(14, 16, 9, 14, 18, 9, Blocks.PINK_WOOL);
            return b.ontwerp();
        });
        c("guh_boerderijtje", "huizen", () -> {
            Bouwdoos b = new Bouwdoos();
            b.huis(0, 0, 6, 6, 0, 3, Blocks.SPRUCE_PLANKS, pastel("boter", Blocks.YELLOW_TERRACOTTA), Blocks.SPRUCE_LOG);
            b.deur(3, 1, 6, Z, Blocks.SPRUCE_DOOR).zet(1, 2, 6, Blocks.GLASS_PANE).zet(5, 2, 6, Blocks.GLASS_PANE).lamp(3, 3, 3, true);
            b.dak(Dak.OORTJES, 0, 0, 6, 6, 4, DAKPAN, pastel("boter", Blocks.YELLOW_TERRACOTTA), 1, true, Blocks.PINK_WOOL);
            // the barn
            b.huis(9, -1, 17, 7, 0, 5, Blocks.COARSE_DIRT, Blocks.RED_TERRACOTTA, Blocks.STRIPPED_DARK_OAK_LOG);
            b.vul(12, 1, 7, 14, 3, 7, LUCHT).zet(12, 1, 7, Blocks.SPRUCE_FENCE_GATE).zet(14, 1, 7, Blocks.SPRUCE_FENCE_GATE);
            b.vul(10, 1, 0, 11, 2, 1, Blocks.HAY_BLOCK).zet(16, 1, 0, Blocks.HAY_BLOCK).zet(16, 1, 5, Blocks.COMPOSTER);
            b.dak(Dak.ZADEL, 9, -1, 17, 7, 6, Blocks.DARK_OAK_PLANKS, Blocks.WHITE_TERRACOTTA, 1, true, null);
            b.ring(0, 1, 8, 7, 1, 11, HEKJE).vul(3, 1, 8, 3, 1, 8, LUCHT);
            return b.ontwerp();
        });
        c("guh_winkeltje", "huizen", () -> {
            Bouwdoos b = new Bouwdoos();
            Block muur = pastel("pistache", Blocks.LIME_TERRACOTTA);
            b.huis(0, 0, 7, 6, 0, 4, TEGEL, muur, Blocks.STRIPPED_BIRCH_LOG);
            b.deur(5, 1, 6, Z, Blocks.BIRCH_DOOR).vul(1, 2, 6, 3, 3, 6, Blocks.GLASS_PANE);
            for (int x = -1; x <= 8; x++) {
                b.plaat(x, 4, 7, x % 2 == 0 ? Blocks.SMOOTH_QUARTZ_SLAB : g("dakpan_roze_plaat", Blocks.PINK_TERRACOTTA), true);
                b.plaat(x, 3, 8, x % 2 == 0 ? Blocks.SMOOTH_QUARTZ_SLAB : g("dakpan_roze_plaat", Blocks.PINK_TERRACOTTA), true);
            }
            b.vul(1, 1, 3, 4, 1, 3, Blocks.BIRCH_PLANKS).zet(2, 2, 3, Blocks.FLOWER_POT).zet(4, 2, 3, Blocks.CAKE);
            b.vul(1, 1, 1, 6, 1, 1, Blocks.BARREL).lamp(3, 4, 3, true).lamp(6, 4, 3, true);
            return b.dak(Dak.ZADEL, 0, 0, 7, 6, 5, DAKPAN_MINT, muur, 1, true, null).ontwerp();
        });
        c("guh_rijtje", "huizen", () -> {
            Bouwdoos b = new Bouwdoos();
            String[] kleuren = {"roze", "hemelsblauw", "boter"};
            Block[] anders = {Blocks.PINK_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA};
            for (int i = 0; i < 3; i++) {
                int x0 = i * 5;
                Block muur = pastel(kleuren[i], anders[i]);
                b.huis(x0, 0, x0 + 4, 7, 0, 5, Blocks.OAK_PLANKS, muur, Blocks.STRIPPED_OAK_LOG);
                b.deur(x0 + 2, 1, 7, Z, Blocks.OAK_DOOR).zet(x0 + 2, 4, 7, GLAS_GUH).zet(x0 + 2, 0, 8, MAT).lamp(x0 + 2, 5, 3, true);
                b.dak(Dak.ZADEL, x0, 0, x0 + 4, 7, 6, i == 1 ? DAKPAN : DAKPAN_BLAUW, muur, 0, true, null);
            }
            return b.ontwerp();
        });

        // --- garden & outdoors ---
        c("kas_met_oortjes", "tuin", () -> {
            Bouwdoos b = new Bouwdoos();
            b.huis(0, 0, 6, 8, 0, 3, Blocks.MOSS_BLOCK, Blocks.GLASS, Blocks.WHITE_CONCRETE);
            b.deur(3, 1, 8, Z, Blocks.BIRCH_DOOR);
            for (int z = 1; z <= 7; z += 2) {
                b.zet(1, 1, z, Blocks.POTTED_PINK_TULIP).zet(5, 1, z, Blocks.POTTED_FERN);
            }
            b.dak(Dak.OORTJES, 0, 0, 6, 8, 4, Blocks.GLASS, Blocks.GLASS, 0, true, Blocks.PINK_STAINED_GLASS);
            return b.ontwerp();
        });
        c("bloemenprieel", "tuin", () -> {
            Bouwdoos b = new Bouwdoos();
            for (int[] p : new int[][] {{0, 0}, {4, 0}, {0, 4}, {4, 4}}) {
                b.vul(p[0], 0, p[1], p[0], 3, p[1], HEKJE);
            }
            b.ring(0, 4, 0, 4, 4, 4, Blocks.BIRCH_PLANKS).vul(1, 5, 1, 3, 5, 3, Blocks.FLOWERING_AZALEA_LEAVES)
                    .ring(0, 5, 0, 4, 5, 4, Blocks.AZALEA_LEAVES).zet(2, 6, 2, Blocks.FLOWERING_AZALEA_LEAVES);
            b.gericht(1, 0, 2, BANKJE, O).gericht(3, 0, 2, BANKJE, W);
            return b.zet(2, 0, 2, Blocks.PINK_PETALS).ontwerp();
        });
        c("boogbrug", "tuin", () -> {
            Bouwdoos b = new Bouwdoos();
            int[] hoogte = {0, 1, 2, 2, 3, 3, 3, 2, 2, 1, 0};
            for (int x = 0; x < 11; x++) {
                int h = hoogte[x];
                b.vul(x, 0, 0, x, h, 4, Blocks.STONE_BRICKS);
                if (x >= 3 && x <= 7) {
                    b.vul(x, 0, 0, x, h - 2, 4, LUCHT);
                }
                b.zet(x, h + 1, 0, Blocks.STONE_BRICK_WALL).zet(x, h + 1, 4, Blocks.STONE_BRICK_WALL);
            }
            return b.lamp(0, 2, 0, false).lamp(10, 2, 0, false).lamp(0, 2, 4, false).lamp(10, 2, 4, false).ontwerp();
        });
        c("steiger_botenhuisje", "tuin", () -> {
            Bouwdoos b = new Bouwdoos();
            for (int x = 0; x <= 10; x += 2) {
                b.vul(x, 0, 0, x, 1, 0, Blocks.SPRUCE_LOG).vul(x, 0, 4, x, 1, 4, Blocks.SPRUCE_LOG);
            }
            b.vul(0, 2, 0, 10, 2, 4, Blocks.SPRUCE_PLANKS);
            b.huis(6, 0, 10, 4, 2, 3, Blocks.SPRUCE_PLANKS, pastel("hemelsblauw", Blocks.LIGHT_BLUE_TERRACOTTA), Blocks.STRIPPED_SPRUCE_LOG);
            b.deur(6, 3, 2, W, Blocks.SPRUCE_DOOR).zet(8, 4, 0, PATRIJS).zet(8, 4, 4, PATRIJS);
            b.zet(8, 3, 2, Blocks.BARREL).lamp(8, 5, 2, true).zet(0, 3, 0, Blocks.SPRUCE_FENCE).zet(0, 3, 4, Blocks.SPRUCE_FENCE);
            return b.dak(Dak.ZADEL, 6, 0, 10, 4, 6, DAKPAN_MINT, pastel("hemelsblauw", Blocks.LIGHT_BLUE_TERRACOTTA), 1, true, null).ontwerp();
        });
        c("vijver_stapstenen", "tuin", () -> {
            Bouwdoos b = new Bouwdoos();
            for (int x = 0; x < 11; x++) {
                for (int z = 0; z < 9; z++) {
                    double d = Math.pow((x - 5) / 5.5, 2) + Math.pow((z - 4) / 4.5, 2);
                    if (d > 1) {
                        continue;
                    }
                    if (d > 0.75) {
                        b.zet(x, 0, z, Blocks.GRASS_BLOCK);
                    } else {
                        b.zet(x, -1, z, Blocks.CLAY).zet(x, 0, z, Blocks.WATER).zet(x, 1, z, LUCHT);
                    }
                }
            }
            for (int[] s : new int[][] {{1, 4}, {3, 3}, {5, 4}, {7, 5}, {9, 4}}) {
                b.zet(s[0], 0, s[1], Blocks.MOSSY_COBBLESTONE);
            }
            b.zet(2, 1, 6, Blocks.LILY_PAD).zet(8, 1, 2, Blocks.LILY_PAD).zet(6, 1, 6, Blocks.LILY_PAD);
            return b.zet(0, 1, 4, Blocks.SUGAR_CANE).zet(10, 1, 4, Blocks.SUGAR_CANE).ontwerp();
        });
        c("picknickplek", "tuin", () -> {
            Bouwdoos b = new Bouwdoos();
            b.vul(0, 0, 0, 6, 0, 4, Blocks.GRASS_BLOCK).vul(2, 1, 1, 4, 1, 3, Blocks.WHITE_CARPET).zet(3, 1, 2, Blocks.PINK_CARPET);
            b.vul(2, 1, 2, 4, 1, 2, Blocks.RED_CARPET).zet(3, 1, 2, Blocks.CAKE);
            b.vul(6, 1, 0, 6, 3, 0, Blocks.OAK_FENCE).vul(5, 4, -1, 7, 4, 1, Blocks.PINK_WOOL).zet(6, 5, 0, Blocks.PINK_WOOL);
            b.gericht(0, 1, 2, BANKJE, O);
            return b.zet(1, 1, 4, VOGELHUISJE).ontwerp();
        });
        c("guh_speeltuintje", "tuin", () -> {
            Bouwdoos b = new Bouwdoos();
            b.vul(0, 0, 0, 10, 0, 8, Blocks.SAND);
            // the slide: a ladder up, a platform, the slide down
            b.vul(1, 1, 1, 3, 3, 1, Blocks.BIRCH_PLANKS).vul(1, 4, 1, 3, 4, 3, Blocks.BIRCH_PLANKS).vul(1, 1, 3, 1, 3, 3, Blocks.BIRCH_FENCE).vul(3, 1, 3, 3, 3, 3, Blocks.BIRCH_FENCE);
            b.ladder(2, 1, 4, 0, N);
            for (int i = 0; i < 4; i++) {
                b.trap(2, 3 - i, 4 + i, Blocks.QUARTZ_STAIRS, N, false);
            }
            // the swing
            b.vul(6, 1, 2, 6, 4, 2, Blocks.SPRUCE_FENCE).vul(9, 1, 2, 9, 4, 2, Blocks.SPRUCE_FENCE).vul(6, 5, 2, 9, 5, 2, Blocks.SPRUCE_PLANKS);
            b.vul(7, 2, 2, 7, 4, 2, Blocks.IRON_CHAIN).vul(8, 2, 2, 8, 4, 2, Blocks.IRON_CHAIN).plaat(7, 1, 2, Blocks.SPRUCE_SLAB, true).plaat(8, 1, 2, Blocks.SPRUCE_SLAB, true);
            // the sandpit with a guh
            b.ring(5, 0, 5, 9, 0, 8, Blocks.SPRUCE_PLANKS).zet(7, 1, 6, g("knuffelblok_roze", Blocks.PINK_WOOL));
            return b.ring(-1, 1, -1, 11, 1, 9, HEKJE).vul(4, 1, 9, 5, 1, 9, LUCHT).ontwerp();
        });

        // --- towers & specials ---
        c("guh_vuurtoren", "torens", () -> {
            Bouwdoos b = new Bouwdoos();
            for (int y = 0; y <= 13; y++) {
                b.cilinder(3, 3, 3, y, y, (y / 2) % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.PINK_CONCRETE, true);
            }
            b.vul(1, 0, 1, 5, 0, 5, Blocks.WHITE_CONCRETE).deur(3, 1, 6, Z, Blocks.BIRCH_DOOR).ladder(3, 1, 13, 1, Z);
            b.vul(0, 14, 0, 6, 14, 6, Blocks.WHITE_CONCRETE).zet(3, 14, 1, LUCHT);
            b.ring(0, 15, 0, 6, 16, 6, Blocks.PINK_STAINED_GLASS).zet(3, 15, 3, Blocks.SEA_LANTERN).zet(3, 16, 3, Blocks.SEA_LANTERN);
            b.dak(Dak.TENT, 0, 0, 6, 6, 17, Blocks.RED_TERRACOTTA, Blocks.RED_TERRACOTTA, 0, false, null);
            return b.vul(0, 18, 3, 0, 19, 3, Blocks.PINK_WOOL).vul(6, 18, 3, 6, 19, 3, Blocks.PINK_WOOL).ontwerp();
        });
        c("guh_windmolen", "torens", () -> {
            Bouwdoos b = new Bouwdoos();
            b.cilinder(3, 3, 3, 0, 9, pastel("vanille", Blocks.WHITE_TERRACOTTA), true).vul(1, 0, 1, 5, 0, 5, Blocks.OAK_PLANKS);
            b.deur(3, 1, 6, Z, Blocks.OAK_DOOR).zet(3, 5, 6, GLAS_HART);
            b.dak(Dak.TENT, 0, 0, 6, 6, 10, DAKPAN, DAKPAN, 0, false, null);
            // the heart sails: four arms on the front
            int cx = 3, cy = 9, z = 7;
            b.zet(cx, cy, z, Blocks.OAK_LOG);
            for (int i = 1; i <= 4; i++) {
                b.zet(cx + i, cy + i, z, Blocks.PINK_WOOL).zet(cx - i, cy - i, z, Blocks.PINK_WOOL)
                        .zet(cx - i, cy + i, z, Blocks.WHITE_WOOL).zet(cx + i, cy - i, z, Blocks.WHITE_WOOL);
            }
            return b.zet(cx + 4, cy + 3, z, Blocks.PINK_WOOL).zet(cx + 3, cy + 4, z, Blocks.PINK_WOOL).ontwerp();
        });
        c("kasteeltoren", "torens", () -> {
            Bouwdoos b = new Bouwdoos();
            b.cilinder(3.5, 3.5, 3.5, 0, 11, Blocks.STONE_BRICKS, true).vul(1, 0, 1, 6, 0, 6, Blocks.COBBLESTONE);
            b.vul(3, 1, 7, 4, 2, 7, LUCHT).vul(1, 6, 1, 6, 6, 6, Blocks.SPRUCE_PLANKS).ladder(3, 1, 6, 1, Z);
            b.vul(1, 11, 1, 6, 11, 6, Blocks.SPRUCE_PLANKS).ladder(3, 7, 11, 1, Z);
            for (int x = 0; x <= 7; x++) {
                for (int z = 0; z <= 7; z++) {
                    double d = Math.hypot(x - 3.5, z - 3.5);
                    if (d > 2.9 && d < 4.0 && (x + z) % 2 == 0) {
                        b.zet(x, 12, z, Blocks.STONE_BRICKS).zet(x, 13, z, Blocks.PINK_WOOL);
                    }
                }
            }
            b.zet(0, 4, 3, Blocks.GLASS_PANE).zet(7, 8, 4, Blocks.GLASS_PANE).lamp(3, 5, 3, true);
            return b.ontwerp();
        });
        c("boomhut", "torens", () -> {
            Bouwdoos b = new Bouwdoos();
            b.vul(3, 0, 3, 4, 9, 4, Blocks.OAK_LOG);
            b.huis(1, 1, 6, 6, 5, 3, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS, Blocks.STRIPPED_OAK_LOG).vul(3, 6, 3, 4, 8, 4, Blocks.OAK_LOG);
            b.deur(4, 6, 6, Z, Blocks.OAK_DOOR).zet(1, 7, 3, Blocks.GLASS_PANE).zet(6, 7, 3, Blocks.GLASS_PANE);
            b.zet(2, 7, 6, Blocks.BLACK_STAINED_GLASS_PANE).zet(2, 8, 6, Blocks.PINK_STAINED_GLASS_PANE);
            b.ladder(3, 0, 6, 5, Z);
            b.cilinder(3.5, 3.5, 4.5, 10, 12, Blocks.OAK_LEAVES, false).cilinder(3.5, 3.5, 3.0, 13, 13, Blocks.OAK_LEAVES, false);
            return b.dak(Dak.ZADEL, 1, 1, 6, 6, 9, Blocks.SPRUCE_PLANKS, Blocks.OAK_PLANKS, 1, true, null).lamp(3, 8, 2, true).ontwerp();
        });
        c("guhbol_koepel", "torens", () -> {
            Bouwdoos b = new Bouwdoos();
            for (int x = 0; x <= 10; x++) {
                for (int y = 0; y <= 9; y++) {
                    for (int z = 0; z <= 10; z++) {
                        double d = Math.pow((x - 5) / 5.4, 2) + Math.pow((y - 4) / 5.0, 2) + Math.pow((z - 5) / 5.4, 2);
                        if (d <= 1.0) {
                            b.zet(x, y, z, d > 0.72 ? Blocks.PINK_WOOL.defaultBlockState() : LUCHT);
                        }
                    }
                }
            }
            b.vul(1, 0, 1, 9, 0, 9, Blocks.OAK_PLANKS);
            // the eyes are the windows, a smile, the ears on top
            b.vul(3, 5, 10, 4, 6, 10, Blocks.BLACK_STAINED_GLASS).vul(6, 5, 10, 7, 6, 10, Blocks.BLACK_STAINED_GLASS);
            b.zet(4, 3, 10, Blocks.MAGENTA_WOOL).zet(5, 2, 10, Blocks.MAGENTA_WOOL).zet(6, 3, 10, Blocks.MAGENTA_WOOL);
            b.deur(5, 1, 10, Z, Blocks.CHERRY_DOOR).zet(5, 0, 11, MAT);
            b.vul(2, 9, 5, 3, 11, 5, Blocks.PINK_WOOL).zet(2, 12, 5, Blocks.PINK_WOOL).vul(7, 9, 5, 8, 11, 5, Blocks.PINK_WOOL).zet(8, 12, 5, Blocks.PINK_WOOL);
            return b.lamp(5, 7, 5, true).ontwerp();
        });
        c("guh_klokkentoren", "torens", () -> {
            Bouwdoos b = new Bouwdoos();
            Block muur = pastel("lavendel", Blocks.PURPLE_TERRACOTTA);
            b.huis(0, 0, 4, 4, 0, 12, Blocks.SPRUCE_PLANKS, muur, Blocks.STRIPPED_SPRUCE_LOG);
            b.deur(2, 1, 4, Z, Blocks.SPRUCE_DOOR);
            b.vul(1, 8, 4, 3, 10, 4, Blocks.WHITE_CONCRETE).zet(2, 9, 4, Blocks.BLACK_CONCRETE).zet(2, 10, 4, Blocks.BLACK_CONCRETE)
                    .zet(3, 9, 4, Blocks.BLACK_CONCRETE);
            b.vul(0, 13, 0, 4, 13, 4, Blocks.SPRUCE_PLANKS).ladder(2, 1, 13, 1, Z);
            for (int[] p : new int[][] {{0, 0}, {4, 0}, {0, 4}, {4, 4}}) {
                b.vul(p[0], 14, p[1], p[0], 15, p[1], Blocks.STRIPPED_SPRUCE_LOG);
            }
            b.zet(2, 15, 2, Blocks.BELL);
            return b.dak(Dak.SCHILD, 0, 0, 4, 4, 16, DAKPAN, muur, 1, false, null).ontwerp();
        });
    }

    private Catalogus() {
    }
}
