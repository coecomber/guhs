package nl.juiced.guhs.feature.favorietjes;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.band.BandEvents;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.band.FavorietSoort;
import nl.juiced.guhs.feature.band.Favorieten;
import nl.juiced.guhs.feature.band.Moment;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.registry.ModItems;

/**
 * The dagboekje comes alive (2.10, favorietjes): after moments all over the mod a band guh writes little sentences,
 * wist-je-datjes ("Vandaag ben ik zo vahoeg geworden, njeg"), and collects funny eerste keren (its first ijsje, its first
 * snow, its first night in the huisje, 1000 blocks together, a marathon...). Everything comes from the moments bus, the
 * friendships listener and a slow look around every ten seconds (rain, thunder, a starry night, days together).
 * <p>
 * Wist-je-datjes: lang {@code gui.guhs.wistjedat.favorietjes.<sleutel>.<n>} (n &lt; the count in the generated
 * favorietjes.json), args {@code %1$s} = the thing (a snack, a song, a friend... or "") and {@code %2$s} = the owner's
 * name. Each sleutel at most once per Minecraft day per guh (saved on the guh), most of them only by chance, and at most
 * {@link #PER_DAG} a day, so the 40 kept lines stay varied. Eerste keren: lang {@code gui.guhs.dagboek.eerste.favorietjes_*} (+ {@code .tekst}).
 */
public final class Verhaaltjes {
    /** Persistent data of a guh: sleutel -&gt; the last day it wrote about it. */
    static final String DAG_KEY = "guhs_favorietjes_wist";
    /** At most this many wist-je-datjes per guh per Minecraft day (the dagboekje keeps 40: a few days of stories). */
    public static final int PER_DAG = 8;
    private static final String TELLER = "#teller", TELLER_DAG = "#teller_dag";
    private static final Map<String, Integer> VARIANTEN = new HashMap<>();
    private static final Random RNG = new Random(Favorietjes.SALT * 7 + 1);

    /** First snack of a family (Hints.etenGroep). */
    static final Map<String, String> EERSTE_ETEN = Map.of("ijskoud", "favorietjes_ijsje", "gebak", "favorietjes_taartje",
            "snoep", "favorietjes_suikerspin", "fruit", "favorietjes_fruit", "frituur", "favorietjes_frituur", "vis", "favorietjes_visje");
    /** First visit to a kind of place together (Hints.plekGroep). */
    static final Map<String, String> EERSTE_PLEK = Map.ofEntries(Map.entry("strand", "favorietjes_strand"),
            Map.entry("sneeuw", "favorietjes_sneeuw"), Map.entry("zee", "favorietjes_zee"), Map.entry("woestijn", "favorietjes_woestijn"),
            Map.entry("bloemen", "favorietjes_bloemen"), Map.entry("paddenstoel", "favorietjes_paddenstoel"),
            Map.entry("jungle", "favorietjes_jungle"), Map.entry("moeras", "favorietjes_moeras"), Map.entry("grot", "favorietjes_grot"),
            Map.entry("bergen", "favorietjes_bergen"), Map.entry("knuffeldal", "favorietjes_knuffeldal"),
            Map.entry("guhpolder", "favorietjes_guhpolder"), Map.entry("vadswoud", "favorietjes_vadswoud"));
    /** First visit to a dimension (the Guhmensie and the Guheinde are fundament's). */
    static final Map<String, String> EERSTE_DIM = Map.of("minecraft:the_nether", "favorietjes_nether", "minecraft:the_end", "favorietjes_end",
            "guhs:guhmaag", "favorietjes_guhmaag", "guhs:barbecuether", "favorietjes_barbecuether");
    /** Short names of the dimensions for the wist-je-datjes (dim_&lt;naam&gt;). */
    static final Map<String, String> DIM_NAAM = Map.of("minecraft:overworld", "bovenwereld", "minecraft:the_nether", "nether",
            "minecraft:the_end", "end", "guhs:guhmension", "guhmensie", "guhs:guhmaag", "guhmaag", "guhs:guheinde", "guheinde",
            "guhs:barbecuether", "barbecuether");
    static final Map<String, String> EERSTE_EMOTE = Map.of("dansen", "favorietjes_dansje", "vahoeg", "favorietjes_vahoeg",
            "zingen", "favorietjes_zingen", "rollen", "favorietjes_rollen");

    private Verhaaltjes() {
    }

    static void laad() {
        VARIANTEN.clear();
        for (Map.Entry<String, JsonElement> e : FavorietjesData.deel("wistjedat").entrySet()) {
            VARIANTEN.put(e.getKey(), e.getValue().getAsInt());
        }
    }

    /** How many texts this wist-je-datje has (0: none). */
    public static int varianten(String sleutel) {
        return VARIANTEN.getOrDefault(sleutel, 0);
    }

    // =====================================================================================================================
    // the moments
    // =====================================================================================================================

    static void moment(Mob guh, @Nullable ServerPlayer speler, Moment m, String waarde) {
        if (!Band.isBandGuh(guh) || guh.level().getServer() == null) {
            return;
        }
        ServerPlayer getuige = Favorietjes.getuige(guh, speler);
        switch (m) {
            case GEGETEN -> {
                String groep = Hints.etenGroep(waarde);
                String eerste = EERSTE_ETEN.get(groep);
                if (eerste != null) {
                    eerste(guh, getuige, eerste);
                }
                wist(guh, "eten_" + (varianten("eten_" + groep) > 0 ? groep : "overig"), 3, itemNaam(waarde));
                mijlpaal(guh, getuige, DagboekStat.KNABBELS_GEGETEN, 100, "favorietjes_hapjes_100");
            }
            case PLEK -> {
                if (getuige == null) {
                    return;   // (only places visited together)
                }
                String groep = Hints.plekGroep(waarde);
                String eerste = EERSTE_PLEK.get(groep);
                if (eerste != null) {
                    eerste(guh, getuige, eerste);
                }
                wist(guh, "plek_" + groep, 2, bioomNaam(waarde));
            }
            case KNUFFEL -> {
                eerste(guh, getuige, "favorietjes_knuffel");
                wist(guh, "knuffel", 2, Favorieten.naam(FavorietSoort.KNUFFEL, waarde).getString());
            }
            case LIEDJE -> {
                boolean disco = waarde.startsWith("disco:");
                eerste(guh, getuige, disco ? "favorietjes_disco" : "favorietjes_koortje");
                wist(guh, disco ? "liedje_disco" : "liedje_koortje", 2, Favorieten.naam(FavorietSoort.LIEDJE, waarde).getString());
            }
            case EMOTE -> {
                String eerste = EERSTE_EMOTE.get(waarde);
                if (getuige != null) {   // (only what the owner saw: guhs do little emotes all day long)
                    if (eerste != null) {
                        eerste(guh, getuige, eerste);
                    }
                    wist(guh, "emote_" + waarde, 4, "");
                }
            }
            case KLEDING -> {
                eerste(guh, getuige, "favorietjes_kleding");
                GuhClothes stuk = GuhClothes.byId(waarde);
                wist(guh, "kleding", 2, stuk == null ? waarde : new ItemStack(ModItems.clothingItem(stuk)).getHoverName().getString());
            }
            case RIT -> wist(guh, "rit", 2, "");
            case REIS -> {
                mijlpaal(guh, getuige, DagboekStat.BLOKKEN_SAMEN, 1000, "favorietjes_reis_1000");
                mijlpaal(guh, getuige, DagboekStat.BLOKKEN_SAMEN, 10000, "favorietjes_reis_10000");
                mijlpaal(guh, getuige, DagboekStat.BLOKKEN_SAMEN, 42195, "favorietjes_marathon");
                wist(guh, "reis", 8, String.valueOf(stat(guh, DagboekStat.BLOKKEN_SAMEN)));
            }
            case DIMENSIE -> {
                String eerste = EERSTE_DIM.get(waarde);
                if (eerste != null) {
                    eerste(guh, getuige, eerste);
                }
                String naam = DIM_NAAM.get(waarde);
                if (naam != null) {
                    wist(guh, "dim_" + naam, 1, "");
                }
            }
            case MINIGAME_EINDE -> {
                wist(guh, varianten("spel_" + waarde) > 0 ? "spel_" + waarde : "spel", 1, "");
            }
            case RECORD -> {
                eerste(guh, getuige, "favorietjes_record");
                wist(guh, "record", 1, "");
            }
            case KLUSJE -> {
                mijlpaal(guh, getuige, DagboekStat.KLUSJES, 100, "favorietjes_klusjes_100");
                wist(guh, varianten("klus_" + waarde) > 0 ? "klus_" + waarde : "klus", 3, "");
            }
            case SPEELTJE -> {
                mijlpaal(guh, getuige, DagboekStat.SPEELTJES, 50, "favorietjes_speeltjes_50");
                wist(guh, varianten("speeltje_" + waarde) > 0 ? "speeltje_" + waarde : "speeltje", 2,
                        Favorieten.naam(FavorietSoort.SPEELTJE, waarde).getString());
            }
            case VRIENDJE -> wist(guh, "vriendje", 2, guhNaam(guh.level().getServer(), waarde));
            case SLAAP -> {
                eerste(guh, getuige != null ? getuige : Band.eigenaarOnline(guh), "favorietjes_nachtje");
                wist(guh, "droom", 2, "");
            }
            case WAKKER -> wist(guh, "wakker", 3, "");
            case GUHKAMER -> wist(guh, "guhkamer", 1, "");
            case AANGEAAID -> wist(guh, "aai", 5, "");
            case GEKNUFFELD -> {
                mijlpaal(guh, getuige, DagboekStat.KNUFFELS, 10, "favorietjes_knuffels_10");
                mijlpaal(guh, getuige, DagboekStat.KNUFFELS, 100, "favorietjes_knuffels_100");
                wist(guh, "knuffel_groot", 3, "");
            }
            case HUISJE_IN -> wist(guh, "huisje_in", 1, waarde);
            default -> {
            }
        }
    }

    /** Two guhs became friends (samen): both write about it; besties get an eerste keer too. */
    static void vriendjes(MinecraftServer s, UUID a, UUID b, boolean besties) {
        BandData data = BandData.get(s);
        for (UUID[] paar : new UUID[][]{{a, b}, {b, a}}) {
            UUID eigenaar = data.eigenaarVan(paar[0]);
            if (eigenaar == null) {
                continue;
            }
            String ander = guhNaam(s, paar[1].toString());
            if (besties) {
                Dagboek.eersteKeer(s, eigenaar, paar[0], "favorietjes_besties");
            }
            String sleutel = besties ? "besties" : "vriendjes_nieuw";
            int n = varianten(sleutel);
            if (n > 0) {
                Dagboek.wistJeDat(s, eigenaar, paar[0], "gui.guhs.wistjedat.favorietjes." + sleutel + "." + RNG.nextInt(n), ander,
                        baasjeNaam(s, eigenaar));
            }
        }
    }

    // =====================================================================================================================
    // a slow look around (every band guh, every 10 seconds): weather, the night sky, days together
    // =====================================================================================================================

    static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 200 != 91 || !Band.isBandGuh(guh) || Huisjes.isBinnen(guh)
                || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer eigenaar = BandEvents.nabijeEigenaar(guh, 16);
        if (eigenaar == null) {
            return;
        }
        mijlpaal(guh, eigenaar, DagboekStat.DAGEN_SAMEN, 7, "favorietjes_week");
        mijlpaal(guh, eigenaar, DagboekStat.DAGEN_SAMEN, 30, "favorietjes_maand");
        mijlpaal(guh, eigenaar, DagboekStat.DAGEN_SAMEN, 100, "favorietjes_honderd_dagen");
        BlockPos boven = guh.blockPosition().above();
        if (!level.canSeeSky(boven)) {
            return;
        }
        if (level.isRainingAt(boven)) {
            eerste(guh, eigenaar, "favorietjes_regen");
            wist(guh, "regen", 2, "");
            if (level.isThundering()) {
                eerste(guh, eigenaar, "favorietjes_onweer");
                wist(guh, "onweer", 1, "");
            }
        } else if (level.isRaining() && level.getBiome(boven).value().getPrecipitationAt(boven) == Biome.Precipitation.SNOW) {
            eerste(guh, eigenaar, "favorietjes_sneeuwvlokjes");
            wist(guh, "sneeuwvlokjes", 1, "");
        } else if (level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime() && level.isNight()) {
            eerste(guh, eigenaar, "favorietjes_sterrennacht");
            wist(guh, "nacht", 3, "");
            if (level.getMoonPhase() == 0) {
                eerste(guh, eigenaar, "favorietjes_volle_maan");
                wist(guh, "volle_maan", 1, "");
            }
        }
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** An eerste keer (only when the owner is around to read it, or it is the guh's own big moment). */
    static void eerste(Mob guh, @Nullable ServerPlayer eigenaar, String id) {
        Dagboek.eersteKeer(guh, eigenaar, id);
    }

    /** A milestone of a statistic: the eerste keer once the stat reaches it. */
    static void mijlpaal(Mob guh, @Nullable ServerPlayer eigenaar, DagboekStat stat, long drempel, String id) {
        if (stat(guh, stat) >= drempel) {
            eerste(guh, eigenaar, id);
        }
    }

    static long stat(Mob guh, DagboekStat stat) {
        UUID eigenaar = Band.eigenaar(guh);
        return eigenaar == null || guh.level().getServer() == null ? 0 : Dagboek.stat(guh.level().getServer(), eigenaar, Band.id(guh), stat);
    }

    /**
     * The guh writes a wist-je-datje about this sleutel: 1 in kans, at most once per Minecraft day per sleutel. Returns
     * true when it wrote one.
     */
    public static boolean wist(Mob guh, String sleutel, int kans, String ding) {
        int n = varianten(sleutel);
        MinecraftServer s = guh.level().getServer();
        if (n <= 0 || s == null || !Band.isBandGuh(guh)) {
            return false;
        }
        CompoundTag dagen = guh.getPersistentData().getCompoundOrEmpty(DAG_KEY);
        long vandaag = Band.dag(s);
        if (dagen.contains(sleutel) && dagen.getLongOr(sleutel, 0L) == vandaag) {
            return false;
        }
        int vandaagAl = dagen.getLongOr(TELLER_DAG, 0L) == vandaag ? dagen.getIntOr(TELLER, 0) : 0;
        if (vandaagAl >= PER_DAG || (kans > 1 && guh.getRandom().nextInt(kans) != 0)) {
            return false;
        }
        dagen.putLong(sleutel, vandaag);
        dagen.putLong(TELLER_DAG, vandaag);
        dagen.putInt(TELLER, vandaagAl + 1);
        guh.getPersistentData().put(DAG_KEY, dagen);
        UUID eigenaar = Band.eigenaar(guh);
        Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.favorietjes." + sleutel + "." + guh.getRandom().nextInt(n), ding,
                eigenaar == null ? "" : baasjeNaam(s, eigenaar));
        return true;
    }

    static String baasjeNaam(MinecraftServer s, UUID eigenaar) {
        ServerPlayer p = s.getPlayerList().getPlayer(eigenaar);
        if (p != null) {
            return p.getGameProfile().name();
        }
        return s.getProfileCache() == null ? "mijn baasje"
                : s.getProfileCache().get(eigenaar).map(com.mojang.authlib.GameProfile::getName).orElse("mijn baasje");
    }

    static String guhNaam(@Nullable MinecraftServer s, String bandId) {
        if (s == null) {
            return "een guh";
        }
        try {
            BandData.Rec r = BandData.get(s).vindOveral(UUID.fromString(bandId));
            return r == null || r.naam.isEmpty() ? "een guh" : r.naam;
        } catch (IllegalArgumentException e) {
            return "een guh";
        }
    }

    static String itemNaam(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item == null ? itemId : new ItemStack(item).getHoverName().getString();
    }

    static String bioomNaam(String biome) {
        return Favorieten.naam(FavorietSoort.PLEK, biome).getString();
    }
}
