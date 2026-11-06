package nl.juiced.guhs.feature.vadsrijk.klassen.spec;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.klassen.Fx;
import nl.juiced.guhs.feature.vadsrijk.klassen.Later;
import nl.juiced.guhs.feature.vadsrijk.klassen.ridder.Knuffelridder;

import static nl.juiced.guhs.feature.vadsrijk.klassen.spec.SpecBalans.*;

/**
 * U-SPECS: what each spec does, per advancement step (cumulative), on kern hooks. The effects that already existed (Dubbelklap,
 * Saltosalvo, Valkenblik, the element puddle, Vadsoverloop, the adv3/adv4 effects) stay where they were and now ask
 * {@link Specs#heeft} instead of the old step node:
 * <pre>
 * Schildridder   1 Dubbelklap + Tegenstoot (counter after a caught hit)  2 Kristalwal          3 Bolwerkplof (ult) + Kussenklap +15 %
 * Reuzenkussen   1 Reuzenzwaai (sweeping basics, heavy Kussenklap)       2 Kussenkoprol (leap)  3 Nestvorm (ult) + basics +10 %
 * Scherpschutter 1 Valkenblik + Verre schoten (+30 % beyond 10 blocks)   2 Opgeladen pijl       3 Kruinvalk (ult) + basics +8 %
 * Strikkenzetter 1 Saltosalvo + Strikje (trap) + quick bow               2 IJsster              3 Strikkenregen (ult) + quicker bow
 * Sterrenwever   1 element puddle + Vadsoverloop                         2 Spiegelecho          3 Schemerwijze (ult) + element +8 %
 * Zegenzanger    1 Strijdlied (aura: you +15 %, party +8 %, song pulse)  2 Zangkring (Vadsbron) 3 Slotakkoord (ult)
 * </pre>
 * Keystones of the re-laid tree: r_onbreekbaar, r_reuzenplof, r_paladijnsaura (the old adv-2 aura, RidderMetBoom),
 * j_strikkenweb, m_slotlied.
 */
public final class SpecEffecten {
    /** Tegenstoot: player -> game time until the counter is ready. */
    static final Map<UUID, Long> TEGEN = new ConcurrentHashMap<>();
    /** Reuzenplof: player -> game time until the next Kussenklap is stronger. */
    static final Map<UUID, Long> PLOF = new ConcurrentHashMap<>();
    /** Strijdlied: party member -> game time until the song buff ends. */
    static final Map<UUID, Long> BEZONGEN = new ConcurrentHashMap<>();
    /** Reuzenzwaai: avoid sweeping a sweep (player -> game time of the sweep). */
    private static final Map<UUID, Long> ZWAAI = new ConcurrentHashMap<>();

    private SpecEffecten() {
    }

    static void init() {
        Spelers.STATS_NA.add(SpecEffecten::stats);
        Schade.VOOR_KLAP.add(SpecEffecten::voorKlap);
        Schade.NA_KLAP.add(SpecEffecten::naKlap);
        Vaardigheden.NA_GEBRUIK.add(SpecEffecten::naGebruik);
        Vaardigheden.COOLDOWN_MOD.add((p, v) -> {
            int cd = v.cooldownTicks();
            if (cd <= 0) {
                return null;
            }
            if ("kussenklap".equals(v.id()) && Specs.heeft(p, Spec.REUZENKUSSEN, 1)) {
                return (float) (cd + REUS_KK_TRAGER) / cd;
            }
            if ("stormsprong".equals(v.id()) && Specs.heeft(p, Spec.REUZENKUSSEN, 2)) {
                return (float) Math.max(0, cd - KOPROL_SNELLER) / cd;
            }
            return null;
        });
    }

    static void vergeet(UUID id) {
        TEGEN.remove(id);
        PLOF.remove(id);
        BEZONGEN.remove(id);
        ZWAAI.remove(id);
        Specs.GEVECHT.remove(id);
    }

    // ---- stats ---------------------------------------------------------------------------------------------------------------

    static void stats(ServerPlayer p, Stats s) {
        if (Specs.heeft(p, Spec.SCHERPSCHUTTER, 1)) {
            s.plus(Stat.KRITIEK, VALK_KRIT);
            s.plus(Stat.KRITIEKSCHADE, VALK_KRITSCHADE);
        }
        if (Specs.heeft(p, Spec.STRIKKENZETTER, 1)) {
            s.plus(Stat.AANVALSSNELHEID, STRIK_SNEL + (Specs.heeft(p, Spec.STRIKKENZETTER, 3) ? STRIK_SNEL3 : 0));
        }
        if (Specs.heeft(p, Spec.STERRENWEVER, 1)) {
            s.plus(Stat.COOLDOWNREDUCTIE, OVERLOOP_CDR);
            s.plus(Stat.ELEMENTSCHADE, OVERLOOP_ELEMENT + (Specs.heeft(p, Spec.STERRENWEVER, 3) ? STERREN_ELEMENT3 : 0));
        }
    }

    // ---- damage --------------------------------------------------------------------------------------------------------------

    static double voorKlap(Schade.Klap k, double n) {
        if (k.soort() == Schade.Soort.WAAR) {
            return n;
        }
        String id = k.skillId() == null ? "" : k.skillId();
        LivingEntity bron = k.bron(), doel = k.doel();
        // the defender: a Schildridder that catches a foe's hit on Knuffelschild gets a counter ready
        if (doel instanceof ServerPlayer d && bron instanceof VadsrijkMob && Knuffelridder.blokt(d)) {
            long nu = d.level().getGameTime();
            if (Specs.heeft(d, Spec.SCHILDRIDDER, 1)) {
                TEGEN.put(d.getUUID(), nu + TEGEN_TICKS);
                if (d.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT, d.getX(), d.getY() + 1.2, d.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
                }
            }
            if (Spelers.heeftKnoop(d, "r_onbreekbaar")) {
                Spelers.data(d).klaarOp.computeIfPresent("kussenklap", (x, t) -> t - 20);
            }
        }
        if (!(bron instanceof ServerPlayer p)) {
            return n;
        }
        long nu = p.level().getGameTime();
        boolean basis = id.equals("basis");
        boolean opMob = doel instanceof VadsrijkMob;
        // Knuffelridder
        if (id.equals("kussenklap")) {
            if (Specs.heeft(p, Spec.SCHILDRIDDER, 1)) {
                Long t = TEGEN.get(p.getUUID());
                if (t != null && t >= nu) {
                    n *= 1 + TEGENSTOOT;
                    TEGEN.remove(p.getUUID());
                    if (p.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, doel.getX(), doel.getY() + 1, doel.getZ(), 1, 0, 0, 0, 0);
                        Fx.geluid(sl, doel.position(), SoundEvents.ANVIL_LAND, 0.5f, 1.8f);
                    }
                }
                if (Specs.heeft(p, Spec.SCHILDRIDDER, 3)) {
                    n *= 1 + BOLWERK_KK;
                }
            }
            if (Specs.heeft(p, Spec.REUZENKUSSEN, 1)) {
                n *= 1 + REUS_KK;
            }
            Long pl = PLOF.get(p.getUUID());
            if (pl != null && pl >= nu) {
                n *= 1.6;
                PLOF.remove(p.getUUID());
            }
        }
        if (basis && k.soort() == Schade.Soort.MELEE && Specs.heeft(p, Spec.REUZENKUSSEN, 1)) {
            n *= 1 + REUS_BASIS + (Specs.heeft(p, Spec.REUZENKUSSEN, 3) ? REUS_BASIS3 : 0);
        }
        if (id.equals("stormsprong") && Specs.heeft(p, Spec.REUZENKUSSEN, 2)) {
            n *= 1 + KOPROL;
        }
        if (id.equals("vahoege_plof") && Specs.heeft(p, Spec.SCHILDRIDDER, 3)) {
            n *= 1 + BOLWERK_PLOF;
        }
        // Pluisjager
        if (basis && k.soort() == Schade.Soort.AFSTAND) {
            if (Specs.heeft(p, Spec.SCHERPSCHUTTER, 1) && p.distanceTo(doel) > VER_AFSTAND) {
                n *= 1 + VER;
            }
            if (Specs.heeft(p, Spec.SCHERPSCHUTTER, 3)) {
                n *= 1 + SCHERP_BASIS3;
            }
            if (Spelers.heeftKnoop(p, "j_strikkenweb") && Statussen.heeft(doel, Status.PLAKKERIG)) {
                n *= 1.25;
            }
        }
        if (id.equals("knuffelpijl") && opMob && Specs.heeft(p, Spec.SCHERPSCHUTTER, 2)) {
            n *= 1 + OPGELADEN;
        }
        // Vadsmagier
        if (opMob && Specs.heeft(p, Spec.ZEGENZANGER, 1)) {
            n *= 1 + LIED_ZELF + (Spelers.heeftKnoop(p, "m_slotlied") ? SLOTLIED : 0);
            if (id.equals("meteoorguh") && Specs.heeft(p, Spec.ZEGENZANGER, 3)) {
                n *= 1 + SLOTAKKOORD;
            }
        } else if (opMob) {
            Long b = BEZONGEN.get(p.getUUID());
            if (b != null && b >= nu) {
                n *= 1 + LIED_GROEP;
            }
        }
        return n;
    }

    static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        if (u.schade() <= 0) {
            return;
        }
        if (k.bron() instanceof ServerPlayer p) {
            Specs.gevochten(p);
        }
        if (k.doel() instanceof ServerPlayer d && k.bron() instanceof VadsrijkMob) {
            Specs.gevochten(d);
        }
        if (!(k.bron() instanceof ServerPlayer p) || !(k.doel() instanceof VadsrijkMob m) || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        String id = k.skillId() == null ? "" : k.skillId();
        long nu = sl.getGameTime();
        // Reuzenzwaai: the basic melee hit sweeps over the foes around the target
        if (id.equals("basis") && k.soort() == Schade.Soort.MELEE && Specs.heeft(p, Spec.REUZENKUSSEN, 1)
                && !Long.valueOf(nu).equals(ZWAAI.get(p.getUUID()))) {
            ZWAAI.put(p.getUUID(), nu);
            double s = k.basis() * REUS_ZWAAI;
            boolean raak = false;
            for (LivingEntity e : Fx.mobsBij(sl, m.position(), REUS_ZWAAI_STRAAL)) {
                if (e != m) {
                    Schade.doe(new Schade.Klap(p, e, s, k.element(), Schade.Soort.MELEE, true, "reuzenzwaai"));
                    raak = true;
                }
            }
            if (raak) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, m.getX(), m.getY() + 0.8, m.getZ(), 2, 0.6, 0.2, 0.6, 0);
                sl.sendParticles(ParticleTypes.CLOUD, m.getX(), m.getY() + 0.5, m.getZ(), 6, 1.2, 0.2, 1.2, 0.02);
            }
        }
        if (id.equals("stormsprong") && Specs.heeft(p, Spec.REUZENKUSSEN, 2)) {
            Statussen.geef(m, Status.VERDOOFD, 20, 1, p);
        }
        if (id.equals("vahoege_plof") && Specs.heeft(p, Spec.SCHILDRIDDER, 3)) {
            Statussen.geef(m, Status.VERDOOFD, 40, 1, p);
        }
    }

    // ---- skills that were just used -------------------------------------------------------------------------------------------

    static void naGebruik(ServerPlayer p, Vaardigheid v) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        long nu = sl.getGameTime();
        switch (v.id()) {
            case "stormsprong" -> {
                if (Spelers.heeftKnoop(p, "r_reuzenplof")) {
                    PLOF.put(p.getUUID(), nu + 80);
                }
            }
            case "rolsalto" -> {
                if (Specs.heeft(p, Spec.STRIKKENZETTER, 1)) {
                    strikje(p, sl, Fx.grond(sl, p.position()));
                }
            }
            case "sterrenschot" -> {
                if (Specs.heeft(p, Spec.STRIKKENZETTER, 3)) {
                    LivingEntity doel = Vaardigheden.doel(p, 24);
                    veld(p, sl, Fx.mikpunt(p, doel, 24), STRIKREGEN_STRAAL, 100, STRIKREGEN, true, "strikkenregen");
                }
            }
            case "vadsbron" -> {
                if (Specs.heeft(p, Spec.ZEGENZANGER, 2)) {
                    double r = Spelers.heeftKnoop(p, "m_grote_bron") ? 6 : 4;
                    veld(p, sl, Fx.grond(sl, p.position()), r, ZANGKRING_SEC * 20, ZANGKRING, false, "zangkring");
                }
            }
            case "meteoorguh" -> {
                if (Specs.heeft(p, Spec.ZEGENZANGER, 3)) {
                    for (ServerPlayer lid : Fx.party(p, LIED_STRAAL)) {
                        Statussen.geef(lid, Status.VAHOEG, SLOT_TICKS, SLOT_VAHOEG, p);
                        sl.sendParticles(ParticleTypes.NOTE, lid.getX(), lid.getY() + 2.2, lid.getZ(), 6, 0.5, 0.3, 0.5, 1);
                    }
                    Fx.geluid(sl, p.position(), SoundEvents.NOTE_BLOCK_BELL.value(), 1.2f, 1.2f);
                }
            }
            default -> {
            }
        }
    }

    /** Strikje: a trap where you flipped from; the first time a foe walks into it: sticky 3 s + tracked (damage over time). */
    static void strikje(ServerPlayer p, ServerLevel sl, Vec3 c) {
        java.util.Set<UUID> gevangen = ConcurrentHashMap.newKeySet();
        double dot = Math.max(1, Spelers.wapenSchade(p) * STRIK_DOT);
        Fx.geluid(sl, c, SoundEvents.TRIPWIRE_ATTACH, 1f, 1.2f);
        for (int t = 0; t <= STRIK_DUUR; t += 10) {
            Later.na(t, () -> {
                if (p.isRemoved()) {
                    return;
                }
                Fx.ring(sl, Fx.stof(Spec.STRIKKENZETTER.kleur, 1.1f), c.add(0, 0.1, 0), STRIK_STRAAL, 14);
                for (LivingEntity e : Fx.mobsBij(sl, c, STRIK_STRAAL)) {
                    if (gevangen.add(e.getUUID())) {
                        Statussen.geef(e, Status.PLAKKERIG, 60, 1, p);
                        Statussen.geef(e, Status.GESPOREN, STRIK_DOT_TICKS, dot, p);
                        sl.sendParticles(ParticleTypes.ITEM_SLIME, e.getX(), e.getY() + 0.3, e.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
                        Fx.geluid(sl, e.position(), SoundEvents.TRIPWIRE_CLICK_ON, 1f, 1.4f);
                    }
                }
            });
        }
    }

    /** A damage field (Strikkenregen, Zangkring): every second {@code mult} x weapon damage to the foes inside. */
    static void veld(ServerPlayer p, ServerLevel sl, Vec3 c, double r, int ticks, double mult, boolean plak, String skill) {
        int kleur = plak ? Spec.STRIKKENZETTER.kleur : Spec.ZEGENZANGER.kleur;
        for (int t = 0; t <= ticks; t += 10) {
            int tt = t;
            Later.na(t, () -> {
                if (p.isRemoved()) {
                    return;
                }
                Fx.ring(sl, Fx.stof(kleur, 1.5f), c.add(0, 0.15, 0), r, (int) (r * 7));
                sl.sendParticles(plak ? ParticleTypes.ITEM_SLIME : ParticleTypes.NOTE, c.x, c.y + 0.6, c.z, 3, r * 0.4, 0.2, r * 0.4, plak ? 0.02 : 1);
                if (tt % 20 != 0 || tt == 0) {
                    return;
                }
                double s = Spelers.wapenSchade(p) * mult;
                for (LivingEntity e : Fx.mobsBij(sl, c, r)) {
                    Schade.doe(new Schade.Klap(p, e, s, Spelers.wapenElement(p), plak ? Schade.Soort.AFSTAND : Schade.Soort.MAGIE, false, skill));
                    if (plak) {
                        Statussen.geef(e, Status.PLAKKERIG, 30, 1, p);
                    }
                }
            });
        }
    }

    // ---- the Zegenzanger's song (every second) -----------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        int tik = server.getTickCount();
        if (tik % 20 != 0) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        long nu = sl.getGameTime();
        BEZONGEN.values().removeIf(t -> t < nu);
        for (ServerPlayer p : sl.players()) {
            if (!Specs.heeft(p, Spec.ZEGENZANGER, 1)) {
                continue;
            }
            for (ServerPlayer lid : Fx.party(p, LIED_STRAAL)) {
                if (lid != p) {
                    BEZONGEN.put(lid.getUUID(), nu + 30);
                }
            }
            if (tik % PULS_TICKS != 0 || !Specs.inGevecht(p)) {
                continue;
            }
            double r = PULS_STRAAL + (Spelers.heeftKnoop(p, "m_slotlied") ? 2 : 0);
            double s = Spelers.wapenSchade(p) * PULS;
            boolean raak = false;
            for (LivingEntity e : Fx.mobsBij(sl, p.position(), r)) {
                Schade.doe(new Schade.Klap(p, e, s, Spelers.wapenElement(p), Schade.Soort.MAGIE, true, "liedpuls"));
                raak = true;
            }
            if (raak) {
                Fx.ring(sl, Fx.stof(Spec.ZEGENZANGER.kleur, 1.4f), p.position().add(0, 0.3, 0), r, (int) (r * 6));
                sl.sendParticles(ParticleTypes.NOTE, p.getX(), p.getY() + 2.2, p.getZ(), 4, 0.6, 0.3, 0.6, 1);
                Fx.geluid(sl, p.position(), SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6f, 1.2f + p.getRandom().nextFloat() * 0.4f);
            }
        }
    }

    /** The spec colour in the ceremony: a ring of its colour around the light beams. */
    static void ceremonie(ServerPlayer p, int stap) {
        Spec s = Specs.spec(p);
        if (s == null || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 c = p.position();
        for (int t = 10; t < 130; t += 10) {
            int tt = t;
            Later.na(t, () -> Fx.ring(sl, Fx.stof(s.kleur, 2f), c.add(0, tt / 40.0, 0), 2.5 + stap * 0.5, 24));
        }
    }
}
