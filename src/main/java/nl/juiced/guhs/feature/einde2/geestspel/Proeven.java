package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.network.ModNetworking;

/**
 * einde2 phase B, slice geest_spel: the five trials of the White Lotus (DESIGN 5), one per wing, each with its echo master
 * (a spot role on {@code echo_<meester>}) and each giving a Pai Sho tile ({@link #tegel}). Nothing costs anything: a wrong
 * answer or a mistake only means "try again". A running trial lives in memory (a relog starts it again); what was earned is
 * saved in the line ({@code v_tegel_<meester>}).
 * <ul>
 *   <li><b>Bumi-guh</b> ({@code bumi_kamer_1..3}, {@code bumi_draai_1..3}): three turning rooms; at each handle a question
 *       whose obvious answer is wrong ("denk als een gekke oude koning"); right: the room turns you on to the next;</li>
 *   <li><b>Pakguh</b> ({@code ijs_spiegel_*}, {@code ijs_stroom}, {@code ijs_platform_*}, {@code verlovingsketting}): turn the
 *       ice mirrors until the moonbeam runs from mirror to mirror onto the betrothal necklace; the stream freezes; the
 *       necklace and its story;</li>
 *   <li><b>Piandao-guh</b> ({@code ritme_plek}, {@code smidse}, {@code meteoriet}): a rhythm duel on {@code deuntje.ritmeduel}
 *       (BPM {@link E2Audio#BPM}): a click on the beat of his blow is a parry; then forge the tile from the meteorite;</li>
 *   <li><b>Jeong Jeong-guh</b> ({@code kaars_*}, {@code lotusblad_*}): light every candle, but only once the flame before it
 *       has calmed down: a wild flame scorches the lotus leaves and every candle goes out;</li>
 *   <li><b>Oom Miroh</b> ({@code theetuin_tafel}, {@code geest_gast_1..3}, {@code theepot_1..3}): three spirits with a problem;
 *       Miroh begins a wisdom and you pick its ending (the real one in own words, a guh twist, or nonsense), then pour each
 *       spirit the right tea.</li>
 * </ul>
 */
public final class Proeven {
    private static final String S_INTRO = "einde2_geest_spel_proef", S_BUMI = "einde2_geest_spel_bumi", S_MIROH = "einde2_geest_spel_miroh";

    private Proeven() {
    }

    private static Verhaallijn lijn() {
        return GeestLijnen.witteLotus();
    }

    /** May this player do the trials now: the White Lotus open for them and the gate passed. */
    static boolean mag(ServerPlayer p) {
        return GeestToegang.zoneOpen(p, "lotus_ruines") && lijn().stap(p) >= GeestLijnen.L_HOF;
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.BUMI_GUH_ECHO, Geestenwereld.NAAM + "/echo_bumi", new Echo("bumi"));
        NpcRollen.zet(GuhNpcEntity.Kind.PAKGUH_ECHO, Geestenwereld.NAAM + "/echo_pakguh", new Echo("pakguh"));
        NpcRollen.zet(GuhNpcEntity.Kind.PIANDAO_GUH_ECHO, Geestenwereld.NAAM + "/echo_piandao", new Echo("piandao"));
        NpcRollen.zet(GuhNpcEntity.Kind.JEONG_JEONG_GUH_ECHO, Geestenwereld.NAAM + "/echo_jeong_jeong", new Echo("jeong_jeong"));
        NpcRollen.zet(GuhNpcEntity.Kind.OOM_MIROH_ECHO, Geestenwereld.NAAM + "/echo_miroh", new Echo("miroh"));
        Praat.luister(S_INTRO, (p, spreker, optie) -> {
            String m = INTRO.get(p.getUUID());
            if (optie == 1 && m != null) {
                begin(p, m);
            }
        });
        Praat.luister(S_BUMI, (p, spreker, optie) -> {
            if (optie > 0) {
                bumiAntwoord(p, optie);
            }
        });
        Praat.luister(S_MIROH, (p, spreker, optie) -> {
            if (optie > 0) {
                mirohKaart(p, optie);
            }
        });
        Schijn.registreer(Proeven::kaarsenSchijn);
    }

    /** The master whose intro this player has open. */
    private static final Map<UUID, String> INTRO = new ConcurrentHashMap<>();

    /** The tile of this master: flag, the keepsake, a sound; the count. */
    static void tegel(ServerPlayer p, String meester) {
        if (GeestLijnen.heeftTegel(p, meester)) {
            return;
        }
        lijn().vlag(p, "tegel_" + meester, true);
        GeestSpel.geef(p, E2GeestSpelFeature.tegel(meester), 1);
        GeestSpel.geluid(p, "stinger.tegel", 1.0f, 1.0f);
        Entity echo = WitteLotus.echo(p, meester);
        if (echo != null) {
            p.level().sendParticles(p, ParticleTypes.END_ROD, false, false, echo.getX(), echo.getY() + 1.5, echo.getZ(), 30, 0.4, 0.6, 0.4, 0.04);
        }
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.tegel", Component.translatable("item.guhs.pai_sho_tegel_" + meester),
                GeestLijnen.meesterTegels(p), GeestLijnen.MEESTERS.size());
        // fix round 1: the success registers clearly (a title and a sound), and the next trial's wing is shown
        GeestSpel.titel(p, "gui.guhs.einde2_geest_spel.proef.tegel_titel", "gui.guhs.einde2_geest_spel.proef.tegel_sub",
                Component.translatable("item.guhs.pai_sho_tegel_" + meester), GeestLijnen.meesterTegels(p), GeestLijnen.MEESTERS.size());
        GeestSpel.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.7f, 1.1f);
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
        if (GeestLijnen.meesterTegels(p) < GeestLijnen.MEESTERS.size()) {
            nl.juiced.guhs.feature.einde2.verhaal.StapHint.later(p, 100, GeestHints::volgendeProef);
        }
    }

    /** Stops whatever trial runs for this player (they left). */
    static void stop(ServerPlayer p) {
        UUID u = p.getUUID();
        BUMI.remove(u);
        PAKGUH.remove(u);
        PAKGUH_MIS.remove(u);
        if (DUELS.remove(u) != null) {
            ModNetworking.sendTo(p, new GeestSpelPayloads.Ritme(false));
        }
        KAARSEN.remove(u);
        MIROH.remove(u);
        INTRO.remove(u);
    }

    // =====================================================================================================================
    // the echo masters
    // =====================================================================================================================

    private record Echo(String meester) implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            if (!mag(p)) {
                GeestSpel.zeg(p, npc, "", "gui.guhs.einde2_geest_spel.proef." + meester + ".dicht");
                return;
            }
            praatMet(p, meester, npc);
        }
    }

    /** The echo (or its start stone) was clicked. */
    static void praatMet(ServerPlayer p, String meester, @Nullable Entity echo) {
        String naam = "entity.guhs.guh_npc." + switch (meester) {
            case "bumi" -> "bumi_guh_echo";
            case "pakguh" -> "pakguh_echo";
            case "piandao" -> "piandao_guh_echo";
            case "jeong_jeong" -> "jeong_jeong_guh_echo";
            default -> "oom_miroh_echo";
        };
        String k = "gui.guhs.einde2_geest_spel.proef." + meester;
        if (GeestLijnen.heeftTegel(p, meester)) {
            GeestSpel.praat(p, S_INTRO + "_na", List.of(GeestSpel.regel(echo, naam, k + ".na." + p.getRandom().nextInt(3))),
                    new Praat.Optie(2, "gui.guhs.einde2_geest_spel.optie.dank"));
            return;
        }
        if (meester.equals("piandao") && lijn().vlag(p, "piandao_duel")) {
            GeestSpel.praat(p, S_INTRO + "_na", List.of(GeestSpel.regel(echo, naam, k + ".smidse")), new Praat.Optie(2, "gui.guhs.einde2_geest_spel.optie.goed"));
            return;
        }
        INTRO.put(p.getUUID(), meester);
        GeestSpel.praat(p, S_INTRO, List.of(GeestSpel.regel(echo, naam, k + ".intro.1"), GeestSpel.regel(echo, naam, k + ".intro.2"),
                GeestSpel.regel(echo, naam, k + ".intro.3")),
                new Praat.Optie(1, k + ".begin"), new Praat.Optie(2, "gui.guhs.einde2_geest_spel.optie.straks"));
    }

    private static void begin(ServerPlayer p, String meester) {
        switch (meester) {
            case "bumi" -> {
                BUMI.put(p.getUUID(), 1);
                GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.bumi.start");
            }
            case "pakguh" -> {
                // fix4-lotus: the first time, Pakguh shows how it works in a short scene (then the trial starts)
                if (!nl.juiced.guhs.feature.verhaal.Cutscenes.gezien(p, GeestScenes.PAKGUH)) {
                    GeestScenes.speel(p, GeestScenes.PAKGUH, Proeven::startPakguh);
                } else {
                    startPakguh(p);
                }
            }
            case "piandao" -> startDuel(p);
            case "jeong_jeong" -> {
                KAARSEN.put(p.getUUID(), new Kaarsen());
                GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.jeong_jeong.start");
            }
            default -> mirohRonde(p);
        }
    }

    // =====================================================================================================================
    // clicks and ticks
    // =====================================================================================================================

    static boolean klik(ServerPlayer p, String prop) {
        if (prop.startsWith("proef_") && prop.endsWith("_start")) {
            String m = prop.substring("proef_".length(), prop.length() - "_start".length());
            if (!GeestLijnen.MEESTERS.contains(m)) {
                return false;
            }
            if (!mag(p)) {
                GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.niet_nu");
                return true;
            }
            praatMet(p, m, WitteLotus.echo(p, m));
            return true;
        }
        if (prop.startsWith("proef_") && prop.endsWith("_einde")) {
            String m = prop.substring("proef_".length(), prop.length() - "_einde".length());
            if (!GeestLijnen.MEESTERS.contains(m)) {
                return false;
            }
            GeestSpel.balk(p, GeestLijnen.heeftTegel(p, m) ? "gui.guhs.einde2_geest_spel.proef.einde_klaar" : "gui.guhs.einde2_geest_spel.proef.einde_nog");
            return true;
        }
        boolean van = prop.startsWith("bumi_draai_") || prop.startsWith("ijs_spiegel_") || prop.equals("verlovingsketting") || prop.equals("ritme_plek")
                || prop.equals("smidse") || prop.equals("meteoriet") || prop.startsWith("kaars_") || prop.startsWith("theepot_") || prop.equals("theetuin_tafel");
        if (!van) {
            return false;
        }
        if (!mag(p)) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.niet_nu");
            return true;
        }
        if (prop.startsWith("bumi_draai_")) {
            bumiDraai(p, getal(prop, "bumi_draai_"));
        } else if (prop.startsWith("ijs_spiegel_")) {
            draaiSpiegel(p, getal(prop, "ijs_spiegel_"));
        } else if (prop.equals("verlovingsketting")) {
            ketting(p);
        } else if (prop.equals("ritme_plek")) {
            if (GeestLijnen.heeftTegel(p, "piandao") || lijn().vlag(p, "piandao_duel")) {
                GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.piandao.al_gewonnen");
            } else {
                praatMet(p, "piandao", WitteLotus.echo(p, "piandao"));
            }
        } else if (prop.equals("meteoriet")) {
            meteoriet(p);
        } else if (prop.equals("smidse")) {
            smidse(p);
        } else if (prop.startsWith("kaars_")) {
            kaars(p, getal(prop, "kaars_"));
        } else if (prop.startsWith("theepot_")) {
            theepot(p, getal(prop, "theepot_"));
        } else {
            praatMet(p, "miroh", WitteLotus.echo(p, "miroh"));
        }
        return true;
    }

    private static int getal(String id, String prefix) {
        try {
            return Integer.parseInt(id.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Every tick (the rhythm duel needs it). */
    static void elkeTick(ServerPlayer p) {
        Duel d = DUELS.get(p.getUUID());
        if (d != null) {
            duelTick(p, d);
        }
    }

    /** Every 10 ticks: the moonbeam of the mirrors, the flames of the candles. */
    static void tick(ServerPlayer p) {
        Integer kamer = BUMI.get(p.getUUID());
        if (kamer != null) {
            // fix round 1: the turning stone of the room you are in sparkles (the intro speaks of the handle)
            Vec3 v = GeestPlek.midden("bumi_draai_" + kamer);
            if (v != null && p.position().closerThan(v, 24)) {
                p.level().sendParticles(p, ParticleTypes.WAX_ON, false, false, v.x, v.y + 1.3, v.z, 3, 0.2, 0.25, 0.2, 0.0);
            }
        }
        if (PAKGUH.containsKey(p.getUUID())) {
            straal(p);
            pakguhBalk(p);
        }
        Kaarsen k = KAARSEN.get(p.getUUID());
        if (k != null) {
            vlammen(p, k);
        }
    }

    // =====================================================================================================================
    // Bumi-guh: the turning rooms
    // =====================================================================================================================

    /** player -> the room they are in (1..3). */
    private static final Map<UUID, Integer> BUMI = new ConcurrentHashMap<>();
    /** The right answer (option id) of each room: the obvious one is never it. */
    static final int[] BUMI_GOED = {3, 1, 2};

    private static void bumiDraai(ServerPlayer p, int n) {
        if (GeestLijnen.heeftTegel(p, "bumi")) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.bumi.klaar");
            return;
        }
        Integer kamer = BUMI.get(p.getUUID());
        if (kamer == null) {
            BUMI.put(p.getUUID(), 1);
            kamer = 1;
        }
        if (n != kamer) {
            GeestSpel.balk(p, n < kamer ? "gui.guhs.einde2_geest_spel.proef.bumi.al_gedaan" : "gui.guhs.einde2_geest_spel.proef.bumi.eerst", kamer);
            return;
        }
        String k = "gui.guhs.einde2_geest_spel.proef.bumi.kamer." + n;
        Entity bumi = WitteLotus.echo(p, "bumi");
        GeestSpel.praat(p, S_BUMI, List.of(GeestSpel.regel(bumi, "entity.guhs.guh_npc.bumi_guh_echo", k + ".vraag")),
                new Praat.Optie(1, k + ".a"), new Praat.Optie(2, k + ".b"), new Praat.Optie(3, k + ".c"));
    }

    private static void bumiAntwoord(ServerPlayer p, int optie) {
        Integer kamer = BUMI.get(p.getUUID());
        if (kamer == null || kamer < 1 || kamer > 3) {
            return;
        }
        String k = "gui.guhs.einde2_geest_spel.proef.bumi.kamer." + kamer;
        Entity bumi = WitteLotus.echo(p, "bumi");
        if (optie != BUMI_GOED[kamer - 1]) {
            GeestSpel.zeg(p, bumi, "entity.guhs.guh_npc.bumi_guh_echo", k + ".fout");
            draai(p, 180f);
            return;
        }
        GeestSpel.zeg(p, bumi, "entity.guhs.guh_npc.bumi_guh_echo", k + ".goed");
        draai(p, 90f);
        if (kamer == 3) {
            BUMI.remove(p.getUUID());
            GeestSpel.zeg(p, bumi, "entity.guhs.guh_npc.bumi_guh_echo", "gui.guhs.einde2_geest_spel.proef.bumi.gewonnen");
            tegel(p, "bumi");
        } else {
            BUMI.put(p.getUUID(), kamer + 1);
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.bumi.volgende", kamer + 1);
        }
    }

    /** "The room turns": the player is turned on the spot, with a rumble and a shake of dust. */
    private static void draai(ServerPlayer p, float graden) {
        p.teleportTo(p.level(), p.getX(), p.getY(), p.getZ(), Set.of(), p.getYRot() + graden, p.getXRot(), true);
        GeestSpel.geluid(p, SoundEvents.GRINDSTONE_USE, 0.8f, 0.6f);
        GeestSpel.geluid(p, SoundEvents.PISTON_EXTEND, 0.6f, 0.5f);
        p.level().sendParticles(p, ParticleTypes.CLOUD, false, false, p.getX(), p.getY() + 0.3, p.getZ(), 30, 3, 0.2, 3, 0.02);
    }

    // =====================================================================================================================
    // Pakguh: the ice mirrors
    // =====================================================================================================================

    /** player -> the turn (0..3) of each mirror. */
    private static final Map<UUID, int[]> PAKGUH = new ConcurrentHashMap<>();
    /** The four directions: south, west, north, east. */
    private static final int[][] DX = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};
    /**
     * fix4-lotus: how a player does in the mirrors: clicks that did not help (a mirror that stays wrong, a mirror the light
     * has not reached, the dark necklace), until when the hint's light column stands, until when the action bar is busy.
     */
    static final class PakStand {
        int mis;
        long zuilTot;
        long balkTot;
    }

    private static final Map<UUID, PakStand> PAKGUH_MIS = new ConcurrentHashMap<>();
    /** After this many clicks that did not help Pakguh gives a hint; after every this many more he turns the mirror himself. */
    static final int HINT_NA = 4, HELP_NA = 4;

    /** The mirrors of the build, then the necklace (the beam's last target). */
    static List<String> spiegelIds() {
        return GeestPlek.reeks("ijs_spiegel_");
    }

    /** The turn of a direction (the one closest to this vector). */
    static int richting(double dx, double dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? 3 : 1;
        }
        return dz > 0 ? 0 : 2;
    }

    /** Where mirror i must point (to the next mirror; the last to the necklace); -1 when a marker is missing. */
    static int goed(int i, List<String> ids) {
        Vec3 a = GeestPlek.midden(ids.get(i));
        Vec3 b = GeestPlek.midden(i + 1 < ids.size() ? ids.get(i + 1) : "verlovingsketting");
        return a == null || b == null ? -1 : richting(b.x - a.x, b.z - a.z);
    }

    private static int[] spiegels(ServerPlayer p) {
        List<String> ids = spiegelIds();
        return PAKGUH.computeIfAbsent(p.getUUID(), u -> {
            int[] o = new int[ids.size()];
            for (int i = 0; i < o.length; i++) {
                int g = Math.max(0, goed(i, ids));
                o[i] = (g + 1 + Math.floorMod(i * 7 + u.hashCode(), 3)) % 4;
            }
            return o;
        });
    }

    private static PakStand stand(ServerPlayer p) {
        return PAKGUH_MIS.computeIfAbsent(p.getUUID(), u -> new PakStand());
    }

    /** After the intro (or the scene): the mirrors are set, the explanation stays readable in the chat. */
    private static void startPakguh(ServerPlayer p) {
        spiegels(p);
        stand(p);
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.pakguh.start");
        GeestSpel.titel(p, "gui.guhs.einde2_geest_spel.proef.pakguh.titel", "gui.guhs.einde2_geest_spel.proef.pakguh.titel_sub");
    }

    /** How many mirrors in a row, from the first, point right (the light gets as far as mirror bereik + 1). Pure logic. */
    static int bereik(int[] o, List<String> ids) {
        int n = 0;
        for (int i = 0; i < ids.size() && i < o.length; i++) {
            if (o[i] != goed(i, ids)) {
                break;
            }
            n++;
        }
        return n;
    }

    /** "the north" etc. (DX order: south, west, north, east). */
    private static Component kant(int r) {
        return Component.translatable("gui.guhs.einde2_geest_spel.proef.pakguh.r." + Math.floorMod(r, 4));
    }

    /** What mirror i must send the light to: "mirror n" or "the necklace". */
    private static Component doel(int i, List<String> ids) {
        return i + 1 < ids.size() ? Component.translatable("gui.guhs.einde2_geest_spel.proef.pakguh.doel_spiegel", i + 2)
                : Component.translatable("gui.guhs.einde2_geest_spel.proef.pakguh.doel_ketting");
    }

    private static void draaiSpiegel(ServerPlayer p, int n) {
        if (GeestLijnen.heeftTegel(p, "pakguh")) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.pakguh.klaar");
            return;
        }
        List<String> ids = spiegelIds();
        int[] o = spiegels(p);
        if (n < 1 || n > o.length || o.length != ids.size()) {
            return;
        }
        PakStand st = stand(p);
        long nu = p.level().getGameTime();
        int beurt = bereik(o, ids);
        // fix4-lotus: a mirror that already passes the light on stays put (one click could break the whole chain before)
        if (n - 1 < beurt) {
            st.balkTot = nu + 60;
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.pakguh.staat_goed", n);
            GeestSpel.geluid(p, SoundEvents.AMETHYST_BLOCK_STEP, 0.6f, 1.4f);
            return;
        }
        // ... and a mirror the light has not reached yet does not turn: first the glinting one
        if (n - 1 > beurt) {
            st.balkTot = nu + 60;
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.pakguh.nog_niet", beurt + 1);
            mis(p, o, ids);
            return;
        }
        o[n - 1] = (o[n - 1] + 1) % 4;
        GeestSpel.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.8f + n * 0.1f);
        int bereikt = bereik(o, ids);
        straal(p);
        st.balkTot = nu + 60;
        if (bereikt == o.length) {
            bevries(p);
            return;
        }
        if (bereikt > beurt) {
            st.mis = 0;
            GeestSpel.geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 1.3f);
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.pakguh.springt", doel(n - 1, ids), bereikt + 1);
        } else {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.pakguh.wijst", n, kant(o[n - 1]), kant(goed(n - 1, ids)), doel(n - 1, ids));
            mis(p, o, ids);
        }
    }

    /** All mirrors right: the stream freezes. */
    private static void bevries(ServerPlayer p) {
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.pakguh.bevroren");
        GeestSpel.geluid(p, SoundEvents.GLASS_PLACE, 1.0f, 0.6f);
        GeestSpel.titel(p, "gui.guhs.einde2_geest_spel.proef.pakguh.bevroren_titel", "gui.guhs.einde2_geest_spel.proef.pakguh.bevroren_sub");
        for (Vec3 v : GeestPlek.pad("ijs_stroom")) {
            p.level().sendParticles(p, ParticleTypes.SNOWFLAKE, false, false, v.x, v.y + 0.6, v.z, 6, 0.4, 0.2, 0.4, 0.01);
        }
        for (String id : GeestPlek.reeks("ijs_platform_")) {
            Vec3 v = GeestPlek.midden(id);
            if (v != null) {
                p.level().sendParticles(p, ParticleTypes.SNOWFLAKE, false, false, v.x, v.y + 1, v.z, 20, 0.6, 0.3, 0.6, 0.02);
            }
        }
    }

    /**
     * fix4-lotus: a click that did not help. After {@link #HINT_NA} Pakguh says exactly which mirror and which way (and a
     * column of light stands on it); after every {@link #HELP_NA} more he blows on that mirror and it turns right by itself.
     */
    private static void mis(ServerPlayer p, int[] o, List<String> ids) {
        PakStand st = stand(p);
        st.mis++;
        int beurt = bereik(o, ids);
        if (beurt >= o.length) {
            return;
        }
        Entity pak = WitteLotus.echo(p, "pakguh");
        String naam = "entity.guhs.guh_npc.pakguh_echo";
        if (st.mis == HINT_NA) {
            st.zuilTot = p.level().getGameTime() + 300;
            GeestSpel.zeg(p, pak, naam, "gui.guhs.einde2_geest_spel.proef.pakguh.hint", beurt + 1, kant(goed(beurt, ids)), doel(beurt, ids));
        } else if (st.mis >= HINT_NA + HELP_NA) {
            st.mis = HINT_NA;
            o[beurt] = Math.max(0, goed(beurt, ids));
            GeestSpel.zeg(p, pak, naam, "gui.guhs.einde2_geest_spel.proef.pakguh.help", beurt + 1);
            GeestSpel.geluid(p, SoundEvents.POWDER_SNOW_STEP, 1.0f, 0.8f);
            Vec3 v = GeestPlek.midden(ids.get(beurt));
            if (v != null) {
                p.level().sendParticles(p, ParticleTypes.SNOWFLAKE, false, false, v.x, v.y + 1.2, v.z, 30, 0.4, 0.6, 0.4, 0.05);
            }
            if (bereik(o, ids) == o.length) {
                bevries(p);
            }
        }
    }

    /**
     * Draws the moonbeam for this player as far as the mirrors carry it; returns how many mirrors point right in a row.
     * fix4-lotus: every mirror shows where it points (a short arrow: bright where the light is, frosty where it is not yet);
     * the light runs out of the mirror whose turn it is in the way it points now; that mirror glints, the spot it must aim
     * at glows, and the necklace (the goal) always has a blue glow.
     */
    static int straal(ServerPlayer p) {
        List<String> ids = spiegelIds();
        int[] o = PAKGUH.get(p.getUUID());
        if (o == null || ids.isEmpty() || o.length != ids.size()) {
            return 0;
        }
        Vec3 eerste = GeestPlek.midden(ids.get(0));
        int goed = bereik(o, ids);
        if (eerste == null || !p.position().closerThan(eerste, 48)) {
            return goed;
        }
        // the moonlight falls on the first mirror from above
        lijn(p, eerste.add(0, 8, 0), eerste.add(0, 1, 0), ParticleTypes.END_ROD);
        for (int i = 0; i < ids.size(); i++) {
            Vec3 a = GeestPlek.midden(ids.get(i));
            if (a == null) {
                break;
            }
            int[] d = DX[Math.floorMod(o[i], 4)];
            Vec3 bij = a.add(0, 1, 0);
            if (i < goed) {
                Vec3 b = GeestPlek.midden(i + 1 < ids.size() ? ids.get(i + 1) : "verlovingsketting");
                if (b != null) {
                    lijn(p, bij, b.add(0, 1, 0), ParticleTypes.END_ROD);
                }
            } else if (i == goed) {
                // the light leaves this mirror the way it points now: you see where it goes wrong
                lijn(p, bij, bij.add(d[0] * 3.5, 0, d[1] * 3.5), ParticleTypes.END_ROD);
                pijlpunt(p, bij.add(d[0] * 3.5, 0, d[1] * 3.5), d, ParticleTypes.END_ROD);
                p.level().sendParticles(p, ParticleTypes.WAX_ON, false, false, a.x, a.y + 1.3, a.z, 4, 0.3, 0.5, 0.3, 0.0);
                Vec3 doel = GeestPlek.midden(i + 1 < ids.size() ? ids.get(i + 1) : "verlovingsketting");
                if (doel != null) {
                    p.level().sendParticles(p, ParticleTypes.GLOW, false, false, doel.x, doel.y + 1.2, doel.z, 3, 0.25, 0.5, 0.25, 0.0);
                }
                PakStand st = PAKGUH_MIS.get(p.getUUID());
                if (st != null && st.zuilTot > p.level().getGameTime()) {
                    lijn(p, a.add(0, 1, 0), a.add(0, 7, 0), ParticleTypes.END_ROD);
                }
            } else {
                // not lit yet: a frosty little arrow, so you can already see which way it points
                lijn(p, bij, bij.add(d[0] * 1.4, 0, d[1] * 1.4), ParticleTypes.SNOWFLAKE);
            }
        }
        Vec3 k = GeestPlek.midden("verlovingsketting");
        if (k != null) {
            p.level().sendParticles(p, goed == ids.size() ? ParticleTypes.GLOW : ParticleTypes.SOUL_FIRE_FLAME, false, false, k.x, k.y + 1, k.z,
                    goed == ids.size() ? 6 : 2, 0.2, 0.2, 0.2, 0.01);
        }
        return goed;
    }

    /** The head of an arrow: two dots back and to the sides of the tip. */
    private static void pijlpunt(ServerPlayer p, Vec3 punt, int[] d, ParticleOptions deeltje) {
        double zx = -d[1], zz = d[0];
        for (int s = -1; s <= 1; s += 2) {
            Vec3 q = punt.add(-d[0] * 0.6 + zx * 0.45 * s, 0, -d[1] * 0.6 + zz * 0.45 * s);
            p.level().sendParticles(p, deeltje, false, false, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    /** fix4-lotus (every 10 ticks while the mirrors run): the action bar says whose turn it is and which way, now and then. */
    private static void pakguhBalk(ServerPlayer p) {
        int[] o = PAKGUH.get(p.getUUID());
        List<String> ids = spiegelIds();
        if (o == null || ids.isEmpty() || o.length != ids.size() || GeestLijnen.heeftTegel(p, "pakguh")) {
            return;
        }
        Vec3 eerste = GeestPlek.midden(ids.get(0));
        PakStand st = stand(p);
        long nu = p.level().getGameTime();
        if (eerste == null || !p.position().closerThan(eerste, 32) || nu < st.balkTot || nu % 40 >= 10) {
            return;
        }
        int beurt = bereik(o, ids);
        if (beurt < ids.size()) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.pakguh.beurt", beurt + 1, kant(o[beurt]), kant(goed(beurt, ids)), doel(beurt, ids),
                    beurt, ids.size());
        } else {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.pakguh.naar_ketting");
        }
    }

    private static void lijn(ServerPlayer p, Vec3 van, Vec3 naar, ParticleOptions deeltje) {
        Vec3 d = naar.subtract(van);
        int n = Math.max(2, (int) (d.length() * 2));
        for (int i = 0; i <= n; i++) {
            Vec3 q = van.add(d.scale(i / (double) n));
            p.level().sendParticles(p, deeltje, false, false, q.x, q.y, q.z, 1, 0, 0, 0, 0);
        }
    }

    private static void ketting(ServerPlayer p) {
        if (GeestLijnen.heeftTegel(p, "pakguh")) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.pakguh.klaar");
            return;
        }
        List<String> ids = spiegelIds();
        int[] o = PAKGUH.get(p.getUUID());
        boolean opgelost = o != null && !ids.isEmpty() && o.length == ids.size() && bereik(o, ids) == ids.size();
        if (ids.isEmpty()) {
            opgelost = true;   // (a build without mirrors: the necklace alone)
        }
        if (!opgelost) {
            GeestSpel.balk(p, o == null ? "gui.guhs.einde2_geest_spel.proef.pakguh.eerst_praten" : "gui.guhs.einde2_geest_spel.proef.pakguh.donker");
            if (o != null) {
                stand(p).balkTot = p.level().getGameTime() + 60;
                mis(p, o, ids);
            }
            return;
        }
        Entity pak = WitteLotus.echo(p, "pakguh");
        String n = "entity.guhs.guh_npc.pakguh_echo";
        GeestSpel.praat(p, S_INTRO + "_na", List.of(GeestSpel.regel(pak, n, "gui.guhs.einde2_geest_spel.proef.pakguh.ketting.1"),
                GeestSpel.regel(pak, n, "gui.guhs.einde2_geest_spel.proef.pakguh.ketting.2"),
                GeestSpel.regel(pak, n, "gui.guhs.einde2_geest_spel.proef.pakguh.ketting.3")), new Praat.Optie(2, "gui.guhs.einde2_geest_spel.optie.dank"));
        PAKGUH.remove(p.getUUID());
        PAKGUH_MIS.remove(p.getUUID());
        tegel(p, "pakguh");
    }

    /** (tests) start the mirrors for this player as the trial does; a click on mirror n; the turns; the misses. */
    static void testPakguh(ServerPlayer p) {
        PAKGUH.remove(p.getUUID());
        PAKGUH_MIS.remove(p.getUUID());
        spiegels(p);
        stand(p);
    }

    static void testSpiegel(ServerPlayer p, int n) {
        draaiSpiegel(p, n);
    }

    @Nullable
    static int[] testStanden(ServerPlayer p) {
        int[] o = PAKGUH.get(p.getUUID());
        return o == null ? null : o.clone();
    }

    static int testMis(ServerPlayer p) {
        PakStand st = PAKGUH_MIS.get(p.getUUID());
        return st == null ? 0 : st.mis;
    }

    // =====================================================================================================================
    // Piandao-guh: the rhythm duel
    // =====================================================================================================================

    /** One beat of deuntje.ritmeduel, in ticks (80 BPM = 15 ticks). */
    static int beatTicks() {
        int bpm = E2Audio.BPM.getOrDefault("deuntje.ritmeduel", 80);
        return Math.max(4, Math.round(1200f / bpm));
    }

    /** The beats on which Piandao-guh strikes (three rounds, ever faster). */
    static final int[] SLAGEN = {4, 6, 8, 10, 14, 15, 17, 18, 22, 23, 24, 26};
    /** The beats on which a round begins, and the beat the duel ends. */
    static final int[] RONDES = {0, 12, 20};
    static final int EINDE = 28;
    /** How many parries win, how many clicks out of the beat lose. */
    static final int NODIG = 8, MAX_MIS = 8;
    /** How far a click may be from the blow (ticks). fix round 1: 6 (was 5), 8 of 12 win (was 10), 8 clicks off the beat (was 5). */
    static final int VENSTER = 6;
    /** The pause before the first beat (ticks): time to read the title. */
    static final int VOORAF = 50;

    static final class Duel {
        final long start;
        final boolean[] gepareerd = new boolean[SLAGEN.length];
        final boolean[] gemist = new boolean[SLAGEN.length];
        int uitDeMaat;

        Duel(long start) {
            this.start = start;
        }

        int gepareerd() {
            int n = 0;
            for (boolean b : gepareerd) {
                if (b) {
                    n++;
                }
            }
            return n;
        }
    }

    private static final Map<UUID, Duel> DUELS = new ConcurrentHashMap<>();

    private static void startDuel(ServerPlayer p) {
        Vec3 plek = GeestPlek.midden("ritme_plek");
        Entity echo = WitteLotus.echo(p, "piandao");
        if (plek != null) {
            float yaw = echo == null ? p.getYRot() : (float) (Math.toDegrees(Math.atan2(echo.getZ() - plek.z, echo.getX() - plek.x)) - 90.0);
            p.teleportTo(p.level(), plek.x, plek.y, plek.z, Set.of(), yaw, 0f, true);
        }
        Duel d = new Duel(p.level().getGameTime() + VOORAF);
        DUELS.put(p.getUUID(), d);
        ModNetworking.sendTo(p, new GeestSpelPayloads.Ritme(true));
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.piandao.start");
        GeestSpel.titel(p, "gui.guhs.einde2_geest_spel.proef.piandao.titel", "gui.guhs.einde2_geest_spel.proef.piandao.titel_sub", NODIG, SLAGEN.length);
    }

    private static void duelTick(ServerPlayer p, Duel d) {
        long t = p.level().getGameTime() - d.start;
        int beat = beatTicks();
        if (t == 0) {
            GeestSpel.geluid(p, "deuntje.ritmeduel", 0.9f, 1.0f);
        }
        if (t < 0) {
            return;
        }
        if (t % beat == 0) {
            int b = (int) (t / beat);
            GeestSpel.geluid(p, SoundEvents.NOTE_BLOCK_HAT.value(), 0.35f, 1.0f);
            for (int r = 0; r < RONDES.length; r++) {
                if (RONDES[r] == b) {
                    GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.piandao.ronde", r + 1);
                }
            }
            for (int s : SLAGEN) {
                if (s - 1 == b) {
                    // the cue: he lifts his sword (a beat before the blow)
                    GeestSpel.geluid(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.4f);
                    Entity echo = WitteLotus.echo(p, "piandao");
                    Vec3 waar = echo != null ? echo.position().add(0, 1.6, 0) : p.position().add(p.getLookAngle().scale(2)).add(0, 1.5, 0);
                    p.level().sendParticles(p, ParticleTypes.CRIT, false, false, waar.x, waar.y, waar.z, 8, 0.2, 0.3, 0.2, 0.1);
                }
            }
            if (b >= EINDE) {
                eindeDuel(p, d);
                return;
            }
        }
        // a blow that went by unparried: a soft tap, no harm
        for (int i = 0; i < SLAGEN.length; i++) {
            if (!d.gepareerd[i] && !d.gemist[i] && t == (long) SLAGEN[i] * beat + VENSTER + 1) {
                d.gemist[i] = true;
                GeestSpel.geluid(p, SoundEvents.WOOL_HIT, 0.8f, 1.0f);
                GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.piandao.tik");
            }
        }
    }

    /** (GeestSpelPayloads.Pareer) the player clicked during the duel. */
    static void pareer(ServerPlayer p) {
        Duel d = DUELS.get(p.getUUID());
        if (d == null) {
            return;
        }
        long t = p.level().getGameTime() - d.start;
        int beat = beatTicks();
        for (int i = 0; i < SLAGEN.length; i++) {
            long slag = (long) SLAGEN[i] * beat;
            if (!d.gepareerd[i] && !d.gemist[i] && Math.abs(t - slag) <= VENSTER) {
                d.gepareerd[i] = true;
                GeestSpel.geluid(p, SoundEvents.SHIELD_BLOCK.value(), 0.8f, 1.3f);
                Vec3 v = p.position().add(p.getLookAngle().scale(1.2)).add(0, 1.4, 0);
                p.level().sendParticles(p, ParticleTypes.CRIT, false, false, v.x, v.y, v.z, 12, 0.15, 0.15, 0.15, 0.2);
                GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.piandao.pareer", d.gepareerd(), SLAGEN.length);
                return;
            }
        }
        d.uitDeMaat++;
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.piandao.uit_de_maat");
        if (d.uitDeMaat > MAX_MIS) {
            DUELS.remove(p.getUUID());
            ModNetworking.sendTo(p, new GeestSpelPayloads.Ritme(false));
            GeestSpel.zeg(p, WitteLotus.echo(p, "piandao"), "entity.guhs.guh_npc.piandao_guh_echo", "gui.guhs.einde2_geest_spel.proef.piandao.maat_kwijt");
            nogEens(p);
        }
    }

    private static void eindeDuel(ServerPlayer p, Duel d) {
        DUELS.remove(p.getUUID());
        ModNetworking.sendTo(p, new GeestSpelPayloads.Ritme(false));
        Entity echo = WitteLotus.echo(p, "piandao");
        String n = "entity.guhs.guh_npc.piandao_guh_echo";
        if (d.gepareerd() >= NODIG) {
            lijn().vlag(p, "piandao_duel", true);
            GeestSpel.zeg(p, echo, n, "gui.guhs.einde2_geest_spel.proef.piandao.gewonnen", d.gepareerd(), SLAGEN.length);
            GeestSpel.titel(p, "gui.guhs.einde2_geest_spel.proef.piandao.gewonnen_titel", "gui.guhs.einde2_geest_spel.proef.piandao.naar_smidse_kort");
            GeestSpel.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.7f, 1.3f);
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.piandao.naar_smidse");
        } else {
            GeestSpel.zeg(p, echo, n, "gui.guhs.einde2_geest_spel.proef.piandao.verloren", d.gepareerd(), NODIG);
            nogEens(p);
        }
    }

    /** fix round 1: a lost duel offers a new one at once, right where you stand (no walk, no intro again). */
    private static void nogEens(ServerPlayer p) {
        INTRO.put(p.getUUID(), "piandao");
        Entity echo = WitteLotus.echo(p, "piandao");
        GeestSpel.praat(p, S_INTRO, List.of(GeestSpel.regel(echo, "entity.guhs.guh_npc.piandao_guh_echo", "gui.guhs.einde2_geest_spel.proef.piandao.nog_eens")),
                new Praat.Optie(1, "gui.guhs.einde2_geest_spel.proef.piandao.begin"), new Praat.Optie(2, "gui.guhs.einde2_geest_spel.optie.straks"));
    }

    private static void meteoriet(ServerPlayer p) {
        if (GeestLijnen.heeftTegel(p, "piandao")) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.piandao.klaar");
            return;
        }
        if (!lijn().vlag(p, "piandao_duel")) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.piandao.meteoriet_eerst");
            return;
        }
        lijn().vlag(p, "meteoriet", true);
        GeestSpel.geluid(p, SoundEvents.NETHERITE_BLOCK_BREAK, 0.8f, 0.8f);
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.piandao.meteoriet");
    }

    private static void smidse(ServerPlayer p) {
        if (GeestLijnen.heeftTegel(p, "piandao")) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.piandao.klaar");
            return;
        }
        if (!lijn().vlag(p, "meteoriet")) {
            GeestSpel.verteller(p, lijn().vlag(p, "piandao_duel") ? "gui.guhs.einde2_geest_spel.proef.piandao.smidse_leeg"
                    : "gui.guhs.einde2_geest_spel.proef.piandao.meteoriet_eerst");
            return;
        }
        Vec3 s = GeestPlek.midden("smidse");
        if (s != null) {
            p.level().sendParticles(p, ParticleTypes.LAVA, false, false, s.x, s.y + 1.1, s.z, 12, 0.3, 0.2, 0.3, 0.02);
            p.level().sendParticles(p, ParticleTypes.FLAME, false, false, s.x, s.y + 1.1, s.z, 20, 0.3, 0.3, 0.3, 0.02);
        }
        GeestSpel.geluid(p, SoundEvents.ANVIL_USE, 0.8f, 1.0f);
        GeestSpel.geluid(p, SoundEvents.ANVIL_USE, 0.8f, 1.2f);
        GeestSpel.zeg(p, WitteLotus.echo(p, "piandao"), "entity.guhs.guh_npc.piandao_guh_echo", "gui.guhs.einde2_geest_spel.proef.piandao.gesmeed");
        tegel(p, "piandao");
    }

    // =====================================================================================================================
    // Jeong Jeong-guh: the candles
    // =====================================================================================================================

    /** How long a new flame is wild (ticks): light the next one only after this. fix round 1: 40 (was 50), and a chime when it calms. */
    static final int WILD = 40;

    static final class Kaarsen {
        final Set<Integer> aan = new HashSet<>();
        long laatste = -1000;
        /** The candle lit last (its flame is the wild one); whether its calming was heard. */
        int nieuwste = -1;
        boolean rustGemeld = true;
    }

    private static final Map<UUID, Kaarsen> KAARSEN = new ConcurrentHashMap<>();

    private static void kaars(ServerPlayer p, int n) {
        if (GeestLijnen.heeftTegel(p, "jeong_jeong")) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.jeong_jeong.klaar");
            return;
        }
        List<String> ids = GeestPlek.reeks("kaars_");
        if (n < 1 || n > ids.size()) {
            return;
        }
        Kaarsen k = KAARSEN.computeIfAbsent(p.getUUID(), u -> new Kaarsen());
        if (k.aan.contains(n)) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.jeong_jeong.brandt_al");
            return;
        }
        long nu = p.level().getGameTime();
        if (nu - k.laatste < WILD) {
            verschroei(p, k, n);
            return;
        }
        k.aan.add(n);
        k.laatste = nu;
        k.nieuwste = n;
        k.rustGemeld = false;
        BlockPos at = GeestPlek.pos("kaars_" + n);
        if (at != null) {
            BlockState s = p.level().getBlockState(at);
            if (s.hasProperty(BlockStateProperties.LIT)) {
                Schijn.toon(p, at, s.setValue(BlockStateProperties.LIT, true));
            }
        }
        GeestSpel.geluid(p, SoundEvents.FLINTANDSTEEL_USE, 0.8f, 1.0f);
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.jeong_jeong.aan", k.aan.size(), ids.size());
        if (k.aan.size() == ids.size()) {
            KAARSEN.remove(p.getUUID());
            Schijn.straks(p);
            GeestSpel.zeg(p, WitteLotus.echo(p, "jeong_jeong"), "entity.guhs.guh_npc.jeong_jeong_guh_echo", "gui.guhs.einde2_geest_spel.proef.jeong_jeong.gewonnen");
            tegel(p, "jeong_jeong");
        }
    }

    /**
     * Too fast: the wild flame licks at the nearest lotus leaf (it only smoulders). fix round 1: only the wild flame goes out
     * (one step back), not every candle: the lesson stays, the long redo is gone.
     */
    private static void verschroei(ServerPlayer p, Kaarsen k, int n) {
        Vec3 kaars = GeestPlek.midden("kaars_" + n);
        Vec3 blad = null;
        double best = Double.MAX_VALUE;
        for (String id : GeestPlek.reeks("lotusblad_")) {
            Vec3 v = GeestPlek.midden(id);
            if (v != null && kaars != null && v.distanceToSqr(kaars) < best) {
                best = v.distanceToSqr(kaars);
                blad = v;
            }
        }
        if (blad != null) {
            p.level().sendParticles(p, ParticleTypes.LARGE_SMOKE, false, false, blad.x, blad.y + 0.4, blad.z, 14, 0.3, 0.2, 0.3, 0.01);
        }
        GeestSpel.geluid(p, SoundEvents.FIRE_EXTINGUISH, 0.7f, 1.0f);
        if (k.nieuwste > 0) {
            k.aan.remove(k.nieuwste);
        }
        k.nieuwste = -1;
        k.laatste = -1000;
        k.rustGemeld = true;
        Schijn.straks(p);
        GeestSpel.zeg(p, WitteLotus.echo(p, "jeong_jeong"), "entity.guhs.guh_npc.jeong_jeong_guh_echo",
                "gui.guhs.einde2_geest_spel.proef.jeong_jeong.te_snel." + p.getRandom().nextInt(3));
    }

    private static void vlammen(ServerPlayer p, Kaarsen k) {
        long nu = p.level().getGameTime();
        boolean wild = nu - k.laatste < WILD;
        for (int n : k.aan) {
            Vec3 v = GeestPlek.midden("kaars_" + n);
            if (v != null) {
                p.level().sendParticles(p, wild && nu - k.laatste < WILD && n == laatsteVan(k) ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME, false, false,
                        v.x, v.y + 0.7, v.z, wild ? 6 : 1, 0.06, 0.1, 0.06, wild ? 0.03 : 0.0);
            }
        }
        if (!k.aan.isEmpty()) {
            GeestSpel.balk(p, wild ? "gui.guhs.einde2_geest_spel.proef.jeong_jeong.wild" : "gui.guhs.einde2_geest_spel.proef.jeong_jeong.rustig");
        }
        if (!wild && !k.rustGemeld) {
            // fix round 1: the hidden timer is heard: a soft chime the moment the flame calms down
            k.rustGemeld = true;
            GeestSpel.geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.4f);
        }
    }

    private static int laatsteVan(Kaarsen k) {
        int m = -1;
        for (int n : k.aan) {
            m = n;
        }
        return m;
    }

    /** Schijn source: the candles this player lit burn for them. */
    private static void kaarsenSchijn(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        Kaarsen k = KAARSEN.get(p.getUUID());
        if (k == null || p.level().dimension() != Geestenwereld.DIM) {
            return;
        }
        for (int n : new ArrayList<>(k.aan)) {
            BlockPos at = GeestPlek.pos("kaars_" + n);
            if (at != null && Schijn.dichtbij(p, at)) {
                BlockState s = p.level().getBlockState(at);
                if (s.hasProperty(BlockStateProperties.LIT)) {
                    gewenst.put(at, s.setValue(BlockStateProperties.LIT, true));
                }
            }
        }
    }

    // =====================================================================================================================
    // Oom Miroh: finish the wisdom, pour the tea
    // =====================================================================================================================

    /** The ending each card is (per guest, per option id): 0 = the real ending, 1 = the guh twist, 2 = nonsense. */
    static final int[][] KAART = {{1, 0, 2}, {2, 1, 0}, {0, 2, 1}};
    /** The right teapot (1..3) for each guest. */
    static final int[] THEE = {1, 2, 3};

    static final class Miroh {
        int gast;
        boolean thee;
    }

    private static final Map<UUID, Miroh> MIROH = new ConcurrentHashMap<>();

    private static void mirohRonde(ServerPlayer p) {
        Miroh m = MIROH.computeIfAbsent(p.getUUID(), u -> new Miroh());
        if (m.thee) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.proef.miroh.schenk", Component.translatable("gui.guhs.einde2_geest_spel.proef.miroh.gast." + (m.gast + 1) + ".naam"));
            return;
        }
        int g = m.gast + 1;
        String k = "gui.guhs.einde2_geest_spel.proef.miroh.gast." + g;
        Entity gast = GeestPlek.bewoner(p.level(), "geest_gast_" + g);
        Entity miroh = WitteLotus.echo(p, "miroh");
        GeestSpel.praat(p, S_MIROH, List.of(GeestSpel.regel(gast, k + ".naam", k + ".probleem"),
                GeestSpel.regel(miroh, "entity.guhs.guh_npc.oom_miroh_echo", k + ".begin")),
                new Praat.Optie(1, k + ".kaart.1"), new Praat.Optie(2, k + ".kaart.2"), new Praat.Optie(3, k + ".kaart.3"));
    }

    private static void mirohKaart(ServerPlayer p, int optie) {
        Miroh m = MIROH.get(p.getUUID());
        if (m == null || m.thee || optie < 1 || optie > 3) {
            return;
        }
        int soort = KAART[m.gast][optie - 1];
        Entity miroh = WitteLotus.echo(p, "miroh");
        String n = "entity.guhs.guh_npc.oom_miroh_echo";
        String k = "gui.guhs.einde2_geest_spel.proef.miroh";
        if (soort == 2) {
            GeestSpel.zeg(p, miroh, n, k + ".onzin." + p.getRandom().nextInt(3));
            GeestSpel.geluid(p, SoundEvents.PLAYER_BURP, 0.6f, 0.8f);
            return;
        }
        GeestSpel.zeg(p, miroh, n, soort == 0 ? k + ".echt" : k + ".draai");
        if (soort == 1) {
            GeestSpel.geluid(p, SoundEvents.VILLAGER_CELEBRATE, 0.6f, 1.2f);
        }
        m.thee = true;
        GeestSpel.verteller(p, k + ".schenk", Component.translatable(k + ".gast." + (m.gast + 1) + ".naam"));
        GeestSpel.verteller(p, k + ".gast." + (m.gast + 1) + ".wens");
    }

    private static void theepot(ServerPlayer p, int pot) {
        if (GeestLijnen.heeftTegel(p, "miroh")) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.miroh.klaar");
            return;
        }
        Miroh m = MIROH.get(p.getUUID());
        if (m == null || !m.thee) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.proef.miroh.eerst_wijsheid");
            return;
        }
        String k = "gui.guhs.einde2_geest_spel.proef.miroh.gast." + (m.gast + 1);
        Entity gast = GeestPlek.bewoner(p.level(), "geest_gast_" + (m.gast + 1));
        if (pot != THEE[m.gast]) {
            GeestSpel.zeg(p, gast, k + ".naam", k + ".vies");
            GeestSpel.geluid(p, SoundEvents.GENERIC_DRINK.value(), 0.6f, 0.6f);
            return;
        }
        GeestSpel.zeg(p, gast, k + ".naam", k + ".lekker");
        GeestSpel.geluid(p, "deuntje.thee_ting", 0.9f, 1.0f);
        if (gast != null) {
            p.level().sendParticles(p, ParticleTypes.HEART, false, false, gast.getX(), gast.getY() + 1.4, gast.getZ(), 4, 0.3, 0.2, 0.3, 0.01);
        }
        m.thee = false;
        m.gast++;
        if (m.gast >= KAART.length) {
            MIROH.remove(p.getUUID());
            GeestSpel.zeg(p, WitteLotus.echo(p, "miroh"), "entity.guhs.guh_npc.oom_miroh_echo", "gui.guhs.einde2_geest_spel.proef.miroh.gewonnen");
            tegel(p, "miroh");
        } else {
            mirohRonde(p);
        }
    }

    // =====================================================================================================================
    // (tests) the state machines without a world
    // =====================================================================================================================

    static void testBumi(ServerPlayer p, int kamer) {
        BUMI.put(p.getUUID(), kamer);
    }

    static void testBumiAntwoord(ServerPlayer p, int optie) {
        bumiAntwoord(p, optie);
    }

    @Nullable
    static Integer bumiKamer(ServerPlayer p) {
        return BUMI.get(p.getUUID());
    }

    static void testMiroh(ServerPlayer p, int optie) {
        MIROH.computeIfAbsent(p.getUUID(), u -> new Miroh());
        mirohKaart(p, optie);
    }

    static void testThee(ServerPlayer p, int pot) {
        theepot(p, pot);
    }

    static void testDuel(ServerPlayer p, long start) {
        DUELS.put(p.getUUID(), new Duel(start));
    }

    @Nullable
    static Duel duel(ServerPlayer p) {
        return DUELS.get(p.getUUID());
    }

    /** (tests) a click on candle n; the newest flame calms at once; the candles that burn. */
    static void testKaars(ServerPlayer p, int n) {
        kaars(p, n);
    }

    static void testKalm(ServerPlayer p) {
        Kaarsen k = KAARSEN.get(p.getUUID());
        if (k != null) {
            k.laatste = -1000;
        }
    }

    static Set<Integer> kaarsenAan(ServerPlayer p) {
        Kaarsen k = KAARSEN.get(p.getUUID());
        return k == null ? Set.of() : Set.copyOf(k.aan);
    }
}
