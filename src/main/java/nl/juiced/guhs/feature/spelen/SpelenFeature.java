package nl.juiced.guhs.feature.spelen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.spelen.SpelGroepen.Groep;
import nl.juiced.guhs.feature.spelen.SpelGroepen.Tijdperk;

/**
 * The shared framework of 2.9 "De Grote Guhspelen" (package feature.spelen): difficulty levels ({@link Niveau}) and the
 * groups of the Guhdex's Minigames tab with the visited tracking ({@link SpelGroepen}). Registered first of all 2.9
 * features (Features.register). Resources: tools/features/spelen.py.
 */
public final class SpelenFeature {
    /** The circuit's tracks (Highscores rows circuit_&lt;track&gt;_&lt;level&gt; and ..._ronde). */
    public static final List<String> CIRCUIT_BANEN = List.of("regenboog", "vads", "kaasberg");
    /** The six events of the Knabbelspelen and the zeskamp total (Highscores ids spelen_&lt;event&gt;). */
    public static final List<String> SPELEN_ONDERDELEN = List.of("knabbelhappen", "zaklopen", "blikgooien", "eierlopen", "spijkerpoepen",
            "guhguhtje_prik", "zeskamp");

    public static void register(IEventBus modBus) {
        registerGroepen();
        NeoForge.EVENT_BUS.addListener(SpelenFeature::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SpelenFeature::onLogin);
    }

    public static void payloads(PayloadRegistrar registrar) {
        SpelenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    /** Every group of the Minigames tab (CONTRACT_29 §5.4), with its Highscores rows in display order. */
    static void registerGroepen() {
        // --- the classics (2.4): the existing rows are medium, with makkelijk before and lastig after ---
        klassieker("beauty", "showrozet", Items.PINK_DYE, "guh_beauty_theater", Kind.SHOWGUH, levels("beauty"));
        List<String> race = new ArrayList<>(levels("race"));
        race.addAll(levels("race_lap"));
        klassieker("race", "raceprijsje", Items.SADDLE, "guh_racebaan", Kind.RACEGUH, race);
        klassieker("meppen", "mepmunt", Items.WOODEN_SHOVEL, "mika_mep_hal", Kind.MEPGUH, levels("meppen"));
        List<String> disco = new ArrayList<>(levels("disco"));
        disco.add("disco_boogie");
        klassieker("disco", "discomunt", Items.JUKEBOX, "guh_disco", Kind.DJGUH, disco);
        klassieker("golf", "golfballetje", Items.SNOWBALL, "guh_golfbaan", Kind.GOLFGUH, levels("golf"));
        klassieker("smul", "smulmunt", Items.CAKE, "vadsig_eetfestijn", Kind.SMULGUH, levels("smul"));
        List<String> vissen = new ArrayList<>(levels("vissen"));
        vissen.addAll(levels("vissen_zwaarste"));
        klassieker("vissen", "visbon", Items.FISHING_ROD, "guhvis_vijver", Kind.VISGUH, vissen);
        klassieker("verstop", "verstopguhticket", Items.PAPER, "verstopguh_huis", Kind.VERSTOPGUHTJE,
                List.of("verstop_makkelijk", "verstop_medium", "verstop_moeilijk"));
        klassieker("kermis", "kermisbon", Items.PAPER, "guh_kermis", Kind.KERMIS_GUH, List.of());
        // --- Knuffeldal (2.8) ---
        knuffeldal("bakkerij", "bakmunt", Items.BREAD, "knuffeldal_stadje", Kind.BAKKERGUH, List.of("bakkerij"));
        knuffeldal("creche", "speenmunt", Items.PINK_BED, "knuffeldal_stadje", Kind.JUF_KNUFFEL, List.of("creche"));
        knuffeldal("kapper", "krulmunt", Items.SHEARS, "knuffeldal_stadje", Kind.KAPPERGUH, List.of("kapper"));
        knuffeldal("knuffelbad", "eendjesmunt", Items.WATER_BUCKET, "knuffelbad", Kind.BADMEESTERGUH,
                List.of("glijbaan_roze_trechter", "glijbaan_glimtunnel", "glijbaan_grote_plons"));
        knuffeldal("grijpmachine", "grijpmachine", Items.CHEST, "knuffeldal_stadje", null, List.of());
        // --- De Grote Guhspelen (2.9) ---
        groot("sjoelen", "sjoelschijfje", Items.OAK_PRESSURE_PLATE, "sjoelhuisje", Kind.SJOELGUH, List.of("sjoelen"));
        groot("doolhof", "doolhofknabbel", Items.OAK_LEAVES, "guhdoolhof", Kind.DOOLHOFGUH, niveaus("doolhof"));
        groot("katapult", "katapultster", Items.CROSSBOW, "knabbelkatapult", Kind.KATAPULTGUH, niveaus("katapult"));
        groot("knabbelspelen", "spelenlintje", Items.BLUE_BANNER, "knabbelspelen", Kind.SPELLEIDERGUH,
                SPELEN_ONDERDELEN.stream().map(o -> "spelen_" + o).toList());
        groot("elftocht", "elfstempel", Items.ICE, "elfguhjestocht", Kind.SCHAATSMEESTERGUH, List.of("elfguhjestocht"));
        List<String> circuit = new ArrayList<>();
        for (String baan : CIRCUIT_BANEN) {
            for (Niveau n : Niveau.values()) {
                circuit.add("circuit_" + baan + "_" + n.id());
                circuit.add("circuit_" + baan + "_" + n.id() + "_ronde");
            }
        }
        groot("circuit", "circuitbeker", Items.MINECART, "guh_circuit", Kind.CIRCUITGUH, circuit);
        // --- Guhverhalen (3.0): the games of the stories (balto-slee, guhwaii-spellen implement them) ---
        verhaal("sledesprint", "sledebelletje", Items.BELL, "nomguh", Kind.STEELE_MIKA, niveaus("sledesprint"));
        List<String> guhwaii = new ArrayList<>(niveaus("surfen"));
        guhwaii.addAll(niveaus("hula"));
        verhaal("guhwaii_spellen", "schelpjesmunt", Items.NAUTILUS_SHELL, "guhwaii_surfstrand", Kind.LILO_GUH, guhwaii);
    }

    private static void verhaal(String id, String coin, net.minecraft.world.item.Item standIn, String structure, Kind npc, List<String> spellen) {
        SpelGroepen.groep(new Groep(id, Tijdperk.VERHALEN, SpelGroepen.icoon(coin, standIn), structure, npc, spellen));
    }

    /** base_makkelijk, base (= medium), base_lastig. */
    private static List<String> levels(String base) {
        return List.of(base + "_makkelijk", base, base + "_lastig");
    }

    /** base_makkelijk, base_medium, base_lastig (the 2.9 games, which have no older board). */
    private static List<String> niveaus(String base) {
        return List.of(base + "_makkelijk", base + "_medium", base + "_lastig");
    }

    private static void klassieker(String id, String coin, net.minecraft.world.item.Item standIn, String structure, Kind npc, List<String> spellen) {
        SpelGroepen.groep(new Groep(id, Tijdperk.KLASSIEKERS, SpelGroepen.icoon(coin, standIn), structure, npc, spellen));
    }

    private static void knuffeldal(String id, String coin, net.minecraft.world.item.Item standIn, String structure, Kind npc, List<String> spellen) {
        SpelGroepen.groep(new Groep(id, Tijdperk.KNUFFELDAL, SpelGroepen.icoon(coin, standIn), structure, npc, spellen));
    }

    private static void groot(String id, String coin, net.minecraft.world.item.Item standIn, String structure, Kind npc, List<String> spellen) {
        SpelGroepen.groep(new Groep(id, Tijdperk.GROTE_GUHSPELEN, SpelGroepen.icoon(coin, standIn), structure, npc, spellen));
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && (player.tickCount + player.getId()) % SpelGroepen.CHECK_TICKS == 0
                && !player.isSpectator()) {
            SpelGroepen.kijk(player);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpelGroepen.sync(player);
        }
    }

    private SpelenFeature() {
    }
}
