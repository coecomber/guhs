package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice geest_spel: the chakra path ({@value GeestLijnen#CHAKRA}, DESIGN 5 "Terugkomen na het verhaal"):
 * seven meditations, one per zone ({@code chakra_1..7}), in order, with the echo of Guru Pathik-guh at the first
 * ({@code guru_pathik}). At a chakra stone: sit still for {@value #MEDITEER} ticks; Guru Pathik's lesson for that chakra (in
 * own words); "laat het los". All seven: the Guhvatar glow (cosmetic: soft light round you, for everyone to see, anywhere;
 * a click on any chakra stone switches it on or off).
 */
public final class Chakras {
    /** How long you sit still at a chakra stone (ticks). */
    public static final int MEDITEER = 100;
    private static final String SLEUTEL = "einde2_geest_spel_chakra", GURU = "einde2_geest_spel_guru";
    /** The player's own switch of the glow (saved). */
    static final String GLOED = GeestSpel.DATA + "gloed";
    /** The colour of each chakra (earth .. thought). */
    static final int[] KLEUR = {0xC0392B, 0xE67E22, 0xF1C40F, 0x2ECC71, 0x3498DB, 0x5B4BDB, 0xB980F0};

    private record Zit(int n, long sinds, Vec3 plek) {
    }

    private static final Map<UUID, Zit> ZIT = new ConcurrentHashMap<>();

    private Chakras() {
    }

    private static Verhaallijn lijn() {
        return GeestLijnen.chakra();
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.GURU_PATHIK_GUH, Geestenwereld.NAAM + "/guru_pathik", new Guru());
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
            if (optie == 1) {
                losgelaten(p);
            }
        });
        Praat.luister(GURU, (p, spreker, optie) -> {
        });
    }

    /** Guru Pathik-guh: what the path is, and where you are on it. */
    private static final class Guru implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            lijn().begin(p);
            int stap = lijn().stap(p);
            String k = "gui.guhs.einde2_geest_spel.chakra.guru";
            if (lijn().klaar(p)) {
                GeestSpel.praat(p, GURU, List.of(GeestSpel.regel(npc, "", k + ".klaar")), new Praat.Optie(2, "gui.guhs.einde2_geest_spel.optie.dank"));
            } else if (stap == 0) {
                GeestSpel.praat(p, GURU, List.of(GeestSpel.regel(npc, "", k + ".1"), GeestSpel.regel(npc, "", k + ".2"), GeestSpel.regel(npc, "", k + ".3")),
                        new Praat.Optie(2, "gui.guhs.einde2_geest_spel.optie.goed"));
            } else {
                GeestSpel.praat(p, GURU, List.of(GeestSpel.regel(npc, "", k + ".verder", stap, GeestLijnen.CHAKRAS_N)),
                        new Praat.Optie(2, "gui.guhs.einde2_geest_spel.optie.goed"));
            }
        }
    }

    /** A click on a chakra stone. */
    static boolean klik(ServerPlayer p, String prop) {
        if (!prop.startsWith("chakra_")) {
            return false;
        }
        int n;
        try {
            n = Integer.parseInt(prop.substring("chakra_".length()));
        } catch (NumberFormatException e) {
            return false;
        }
        if (n < 1 || n > GeestLijnen.CHAKRAS_N) {
            return false;
        }
        if (lijn().klaar(p)) {
            boolean aan = !GuhQuests.saved(p).getBooleanOr(GLOED, true);
            GuhQuests.saved(p).putBoolean(GLOED, aan);
            GeestSpel.balk(p, aan ? "gui.guhs.einde2_geest_spel.chakra.gloed_aan" : "gui.guhs.einde2_geest_spel.chakra.gloed_uit");
            return true;
        }
        int stap = lijn().stap(p);
        if (n - 1 < stap) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.chakra.al_open", n);
            return true;
        }
        if (n - 1 > stap) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.chakra.eerst", stap + 1);
            return true;
        }
        lijn().begin(p);
        ZIT.put(p.getUUID(), new Zit(n, p.level().getGameTime(), p.position()));
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.chakra.zit." + n);
        return true;
    }

    /** Every 10 ticks: the breathing while you sit. */
    static void tick(ServerPlayer p) {
        Zit z = ZIT.get(p.getUUID());
        if (z == null) {
            return;
        }
        if (p.position().distanceToSqr(z.plek()) > 0.5 * 0.5) {
            ZIT.remove(p.getUUID());
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.chakra.bewogen");
            return;
        }
        long t = p.level().getGameTime() - z.sinds();
        int bolletjes = (int) Math.min(5, t * 5 / MEDITEER);
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.chakra.adem", "●".repeat(bolletjes) + "○".repeat(5 - bolletjes));
        stof(p, z.n(), 3);
        if (t >= MEDITEER) {
            ZIT.remove(p.getUUID());
            String k = "gui.guhs.einde2_geest_spel.chakra." + z.n();
            var guru = GeestPlek.bewoner(p.level(), "guru_pathik");
            GeestSpel.praat(p, SLEUTEL, List.of(GeestSpel.regel(guru, "entity.guhs.guh_npc.guru_pathik_guh", k + ".naam"),
                    GeestSpel.regel(guru, "entity.guhs.guh_npc.guru_pathik_guh", k + ".les")),
                    new Praat.Optie(1, "gui.guhs.einde2_geest_spel.chakra.los"));
            lijn().vlag(p, "zit_" + z.n(), true);
        }
    }

    /** "Laat het los": the chakra opens. */
    static void losgelaten(ServerPlayer p) {
        int stap = lijn().stap(p);
        if (stap >= GeestLijnen.CHAKRAS_N || !lijn().vlag(p, "zit_" + (stap + 1))) {
            return;
        }
        lijn().zet(p, stap + 1);
        stof(p, stap + 1, 40);
        GeestSpel.geluid(p, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f + stap * 0.1f);
        if (lijn().klaar(p)) {
            GuhQuests.saved(p).putBoolean(GLOED, true);
            GuhAdvancements.grant(p, "einde2_geest_spel_chakra");
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.chakra.gloed");
            GeestSpel.geluid(p, "stinger.opgelost", 1.0f, 1.2f);
        } else {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.chakra.open", stap + 1, GeestLijnen.CHAKRAS_N);
        }
    }

    private static void stof(ServerPlayer p, int n, int aantal) {
        p.level().sendParticles(p, new DustParticleOptions(KLEUR[Math.max(0, Math.min(6, n - 1))], 1.2f), false, false,
                p.getX(), p.getY() + 1.0, p.getZ(), aantal, 0.4, 0.6, 0.4, 0.01);
    }

    static void stop(ServerPlayer p) {
        ZIT.remove(p.getUUID());
    }

    /** Does this player wear the Guhvatar glow now? */
    public static boolean gloeit(ServerPlayer p) {
        return lijn().klaar(p) && GuhQuests.saved(p).getBooleanOr(GLOED, true);
    }

    /** Every 200 ticks outside the Geestenwereld (and every 10 inside, from {@link Mysterie}): a soft glow for everyone. */
    static void gloedTick(ServerPlayer p) {
        if (!gloeit(p) || p.isSpectator() || p.isInvisible()) {
            return;
        }
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.35, 0.6, 0.35, 0.01);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.6, p.getZ(), 1, 0.25, 0.25, 0.25, 0.005);
    }
}
