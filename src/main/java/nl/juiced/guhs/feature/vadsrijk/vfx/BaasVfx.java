package nl.juiced.guhs.feature.vadsrijk.vfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * The bosses' side of the VFX kit (T-VFX): the hooks kern's/zone bases call (all marked {@code // T-VFX} there) and the running
 * bouwdoos attacks.
 * <ul>
 *   <li>{@link #tick} (VadsrijkBaas.tick): runs the bouwdoos attack, the soft-enrage glow;</li>
 *   <li>{@link #probeer} (where a boss picks its next move: Z2Baas for zone 2 + 3, and the zone-1 state machines): maybe start one
 *       of its {@link BaasProfielen} attacks instead; returns its length so the boss waits;</li>
 *   <li>{@link #loopt}: a bouwdoos attack runs (the boss's own rotation and chase pause);</li>
 *   <li>{@link #geraakt} (krijgSchade): the interrupt meter + the hit flinch;</li>
 *   <li>{@link #fase} (phase change) and {@link #verslagen} (the plof): animation + shockwave + light + shake;</li>
 *   <li>{@link #windup} / {@link #cast}: one-liners for a boss's OWN telegraphed attacks (wind-up pose/animation, cast bar).</li>
 * </ul>
 */
public final class BaasVfx {
    /** Per boss: the running attack, cooldowns, the phase start, the fight start (soft enrage). */
    static final class Staat {
        @Nullable
        Bouwdoos lopend;
        final Map<String, Long> klaarOp = new HashMap<>();
        long faseStart = -1, gevechtStart = -1, laatsteFlinch, laatsteBouwdoos;
        /** A boss's own wind-up (one-liner) is released by this game time if no hit came (the hold must never stick). */
        long windupTot = -1;
        int faseGezien = 1;
        boolean woede;
        /** Gametests/dev: the last attack started and how it ended. */
        @Nullable
        Bouwdoos laatste;
    }

    private static final Map<VadsrijkBaas, Staat> STATEN = new WeakHashMap<>();
    /** Soft enrage after this long in one fight (ticks): attacks come twice as often, the boss glows red. */
    public static final int WOEDE_NA = 20 * 60 * 6;
    /** At least this long between two bouwdoos attacks (ticks; halved in enrage). */
    public static final int TUSSEN = 180;
    /** Gametests: let {@link #probeer} start attacks on the gametest server too (off by default). */
    public static boolean OOK_IN_TESTS;

    private BaasVfx() {
    }

    static Staat staat(VadsrijkBaas b) {
        return STATEN.computeIfAbsent(b, x -> new Staat());
    }

    /** The profile key of a boss: zone-2/3 and zone-1 bazen = their baasId(), the Klodderreus = "klodderreus". */
    public static String sleutel(VadsrijkBaas b) {
        if (b instanceof nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas z) {
            return z.baasId();
        }
        if (b instanceof nl.juiced.guhs.feature.vadsrijk.bazen.BazenBaas z) {
            return z.baasId();
        }
        if (b instanceof nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus) {
            return "klodderreus";
        }
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(b.getType()).getPath();
    }

    /** The fighters: the zone-2/3 engine's own list, else kern's. */
    public static java.util.List<net.minecraft.server.level.ServerPlayer> vechters(VadsrijkBaas b) {
        return b instanceof nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas z ? z.strijders() : b.vechters();
    }

    /** The boss's current target: the zone-2/3 engine's own, else kern's. */
    @Nullable
    public static net.minecraft.server.level.ServerPlayer doel(VadsrijkBaas b) {
        return b instanceof nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas z ? z.zDoel() : b.doel();
    }

    // ---- hooks ---------------------------------------------------------------------------------------------------------------

    /** (VadsrijkBaas.tick, server) Runs the attack, the enrage. */
    public static void tick(VadsrijkBaas b, ServerLevel sl) {
        Staat s = STATEN.get(b);
        if (s == null) {
            if (b.tickCount % 20 != 0 || vechters(b).isEmpty()) {
                return;
            }
            s = staat(b);
        }
        long nu = sl.getGameTime();
        if (s.lopend != null && !s.lopend.tick(sl)) {
            s.laatste = s.lopend;
            s.lopend = null;
            s.laatsteBouwdoos = nu;
        }
        if (b.geplofd()) {
            return;
        }
        if (s.windupTot > 0 && nu > s.windupTot) {
            s.windupTot = -1;
            if (s.lopend == null) {
                BaasAnims.speel(b, "vfx_treffer");
            }
        }
        if (b.tickCount % 20 == 0) {
            boolean vecht = !vechters(b).isEmpty();
            if (vecht && s.gevechtStart < 0) {
                s.gevechtStart = nu;
                s.faseStart = nu;
            } else if (!vecht && b.vads() >= b.maxVads()) {
                s.gevechtStart = -1;   // a reset fight starts over
                s.woede = false;
            }
            if (vecht && !s.woede && s.gevechtStart >= 0 && nu - s.gevechtStart > WOEDE_NA) {
                s.woede = true;
                BaasAnims.speel(b, "vfx_woede");
                Vfx.pose(sl, b, "fase", 20);
                Vfx.golf(sl, b.position(), 10, 0xFF3030, 16);
                for (net.minecraft.server.level.ServerPlayer p : Vfx.kijkers(b, sl)) {
                    p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.vfx.woede", net.minecraft.network.chat.Component.translatable(b.titelKey()))
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
            if (s.woede && b.tickCount % 40 == 0) {
                Vfx.deeltjes(sl, Deeltje.ROOK, b.position().add(0, b.getBbHeight() * 0.6, 0), 6, 0xC02020, b.getBbWidth() * 0.5, 0.03);
            }
        }
    }

    /** True while a bouwdoos attack of this boss runs (its own rotation and chase wait). */
    public static boolean loopt(VadsrijkBaas b) {
        Staat s = STATEN.get(b);
        return s != null && s.lopend != null;
    }

    /**
     * (where a boss picks its next move) Maybe starts one of its profile's attacks now: returns its length in ticks (the boss should
     * wait that long + a breath before its own next move), or 0 = nothing started, do your own move.
     */
    public static int probeer(VadsrijkBaas b, ServerLevel sl) {
        Staat s = staat(b);
        if (s.lopend != null) {
            return s.lopend.aanval.duur() - s.lopend.t() + 10;
        }
        if (b.geplofd() || vechters(b).isEmpty() || b.inIntro() || (!OOK_IN_TESTS && sl.getServer() instanceof net.minecraft.gametest.framework.GameTestServer)) {
            return 0;   // (the zone gametests count their bosses' own moves: no surprise attacks there)
        }
        long nu = sl.getGameTime();
        int tussen = s.woede ? TUSSEN / 2 : TUSSEN;
        if (s.laatsteBouwdoos > 0 && nu - s.laatsteBouwdoos < tussen) {
            return 0;
        }
        if (b.fase() != s.faseGezien) {
            s.faseGezien = b.fase();
            s.faseStart = nu;
        }
        if (s.faseStart < 0) {
            s.faseStart = nu;
        }
        List<Aanval> klaar = new ArrayList<>();
        int som = 0;
        for (Aanval a : BaasProfielen.van(sleutel(b))) {
            if (b.fase() < a.minFase || b.fase() > a.maxFase || nu - s.faseStart < (a.minFase == b.fase() ? a.eersteWacht : 0)
                    || nu < s.klaarOp.getOrDefault(a.id, 0L)) {
                continue;
            }
            klaar.add(a);
            som += a.gewicht;
        }
        if (klaar.isEmpty() || b.getRandom().nextInt(3) == 0) {   // (not every free moment: the boss's own moves stay the backbone)
            return 0;
        }
        int r = b.getRandom().nextInt(som);
        for (Aanval a : klaar) {
            r -= a.gewicht;
            if (r < 0) {
                return start(b, sl, a);
            }
        }
        return 0;
    }

    /** Starts this attack now (dev command / gametests / probeer); returns its length. Stops a running one first. */
    public static int start(VadsrijkBaas b, ServerLevel sl, Aanval a) {
        Staat s = staat(b);
        if (s.lopend != null) {
            s.lopend.stop(sl, false);
        }
        s.lopend = new Bouwdoos(b, a);
        s.klaarOp.put(a.id, sl.getGameTime() + (s.woede ? a.wacht / 2 : a.wacht));
        s.lopend.start(sl);
        return a.duur() + 12;
    }

    /** Stops a running attack (dev/reset). */
    public static void stop(VadsrijkBaas b, ServerLevel sl) {
        Staat s = STATEN.get(b);
        if (s != null && s.lopend != null) {
            s.lopend.stop(sl, false);
            s.laatste = s.lopend;
            s.lopend = null;
        }
    }

    /** The last finished (or the running) attack of a boss (gametests). */
    @Nullable
    public static Bouwdoos laatste(VadsrijkBaas b) {
        Staat s = STATEN.get(b);
        return s == null ? null : s.lopend != null ? s.lopend : s.laatste;
    }

    /** (krijgSchade, after the damage) The interrupt meter, and a small flinch now and then. */
    public static void geraakt(VadsrijkBaas b, double schade) {
        if (!(b.level() instanceof ServerLevel sl) || schade <= 0) {
            return;
        }
        Staat s = staat(b);
        if (s.lopend != null) {
            s.lopend.castSchade += schade;
            return;   // (no flinch: it would break the held wind-up)
        }
        long nu = sl.getGameTime();
        if (nu - s.laatsteFlinch >= 30 && schade >= b.maxVads() * 0.004) {
            s.laatsteFlinch = nu;
            BaasAnims.speel(b, "vfx_geraakt");
            Vfx.pose(sl, b, "geraakt", 8);
        }
    }

    /** (a phase starts) Rear up, a shockwave, a light pillar, shake; a running attack is cancelled (the phase gets the stage). */
    public static void fase(VadsrijkBaas b, int fase) {
        if (!(b.level() instanceof ServerLevel sl)) {
            return;
        }
        stop(b, sl);
        Staat s = staat(b);
        s.faseGezien = fase;
        s.faseStart = sl.getGameTime();
        BaasAnims.speel(b, "vfx_fase");
        Vfx.pose(sl, b, "fase", 24);
        Vec3 v = b.position();
        Vfx.golf(sl, v, Math.max(8, b.gevechtsStraal() * 0.8), 0xFFFFFF, 16);
        Vfx.golf(sl, v, Math.max(5, b.gevechtsStraal() * 0.5), 0xFF5FC8, 12);
        Vfx.zuil(sl, v, 14, 0xFFE0A0, 30);
        Vfx.deeltjes(sl, Deeltje.STER, v.add(0, b.getBbHeight() * 0.7, 0), 30, 0xFFF0A0, b.getBbWidth(), 0.25);
        Vfx.schud(sl, v, 32, 0.45f, 16);
    }

    /** (the plof) The cheer-up finale: the boss sways and waves, confetti, hearts, a pink flash and pillar. Zone 2: crystal shards. */
    public static void verslagen(VadsrijkBaas b) {
        if (!(b.level() instanceof ServerLevel sl)) {
            return;
        }
        stop(b, sl);
        BaasAnims.speel(b, "vfx_juich");
        Vfx.pose(sl, b, "juich", 40);
        Vec3 v = b.position().add(0, b.getBbHeight() * 0.6, 0);
        boolean kristal = b instanceof nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas && !(b instanceof nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas);
        if (kristal) {
            Vfx.deeltjes(sl, Deeltje.KRISTAL, v, 60, 0xB8C8E0, b.getBbWidth() * 0.7, 0.35);
            Vfx.flits(sl, v, 4, 0xD8E8FF);
        } else {
            Vfx.deeltjes(sl, Deeltje.CONFETTI, v, 80, 0xFFFFFF, b.getBbWidth() * 0.6, 0.45);
            Vfx.deeltjes(sl, Deeltje.HART, v.add(0, 1, 0), 18, 0xFF7AC8, b.getBbWidth() * 0.6, 0.12);
            Vfx.flits(sl, v, 5, 0xFF8AD0);
            Vfx.zuil(sl, b.position(), 18, 0xFF8AD0, 50);
        }
        Vfx.golf(sl, b.position(), 12, kristal ? 0xD8E8FF : 0xFF8AD0, 20);
        STATEN.remove(b);
    }

    /**
     * One-liner for a boss's OWN telegraphed attack: the wind-up animation/pose for {@code ticks} (held until the hit), and a
     * charge glow. Call it next to the attack's Telegraaf call.
     */
    public static void windup(VadsrijkBaas b, int ticks) {
        if (b.level() instanceof ServerLevel sl && !loopt(b)) {
            staat(b).windupTot = sl.getGameTime() + ticks + 8;
            BaasAnims.speel(b, "vfx_windup");
            Vfx.pose(sl, b, "windup", ticks);
            Vfx.deeltjes(sl, Deeltje.GLOED, b.position().add(0, b.getBbHeight() * 0.5, 0), 8, 0xFFE0A0, b.getBbWidth() * 0.5, 0.02);
        }
    }

    /** One-liner: the cast bar for a boss's OWN attack ({@code id} → lang key vadsrijk.vfx.aanval.&lt;id&gt;). */
    public static void cast(VadsrijkBaas b, String id, int ticks) {
        if (!loopt(b)) {
            Vfx.cast(b, "vadsrijk.vfx.aanval." + id, ticks, false);
        }
    }

    /** One-liner: the hit of a boss's OWN attack (ends the held wind-up with the pop, a shockwave at the spot). */
    public static void treffer(VadsrijkBaas b, Vec3 waar, double r, int kleur) {
        if (b.level() instanceof ServerLevel sl && !loopt(b)) {
            staat(b).windupTot = -1;
            BaasAnims.speel(b, "vfx_treffer");
            Vfx.pose(sl, b, "treffer", 8);
            Vfx.golf(sl, waar, r, kleur, 10);
            Vfx.flits(sl, waar.add(0, 0.8, 0), Math.max(1.5, r * 0.6), kleur);
        }
    }
}
