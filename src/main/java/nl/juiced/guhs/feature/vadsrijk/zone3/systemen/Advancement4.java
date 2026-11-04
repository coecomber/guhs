package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.klassen.Advancement;
import nl.juiced.guhs.feature.vadsrijk.klassen.Fx;
import nl.juiced.guhs.feature.vadsrijk.klassen.Later;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;

/**
 * The 4th job advancement at class lv 50 (ronde 17: on 50, not 40). The klassen {@link Advancement} has a 4th step
 * ({@code MAX_STAP} 4, {@code nodigLevel(3)} = 50); this class adds the hidden nodes {@code <x>_adv4} (stats), the nest touch of
 * the ceremony (golden feathers + blossom), and the "Nestvorm" of the class ult (same key, bigger, with an extra effect):
 * <ul>
 *   <li>Knuffelridder -> <b>Nestwachter</b>: Vahoege Plof becomes a winged cushion: 30 % harder, and its wind pulls every foe
 *   within 8 blocks to the middle.</li>
 *   <li>Pluisjager -> <b>Kruinvalk</b>: Sterrenschot hits 40 % harder and pierces shields (a Knuffelschild-like buff on a foe is
 *   ignored: the shot also deals 25 % of its damage as true damage).</li>
 *   <li>Vadsmagier -> <b>Schemerwijze</b>: Meteoorguh leaves a pink fire field (radius 3.5, 5 s): every second 30 % weapon damage
 *   and Brandend.</li>
 * </ul>
 * Where: the Nestkapel in Honingtak (z3-npc calls {@link Advancement#doe}) or the ordetempel desk.
 */
public final class Advancement4 {
    public static final int LEVEL = 50, STAP = 4;
    private static final Map<String, Long> EENMAAL = new ConcurrentHashMap<>();

    private Advancement4() {
    }

    public static List<BoomKnoop> knopen() {
        return List.of(adv(Klassen.RIDDER, "r_adv4", Stats.van(Stat.KRACHT, 8, Stat.VERDEDIGING, 10, Stat.VADS, 250)),
                adv(Klassen.JAGER, "j_adv4", Stats.van(Stat.BEHENDIGHEID, 8, Stat.KRITIEKSCHADE, 20, Stat.VADS, 120)),
                adv(Klassen.MAGIER, "m_adv4", Stats.van(Stat.WIJSHEID, 8, Stat.ELEMENTSCHADE, 12, Stat.HEALKRACHT, 10)));
    }

    private static BoomKnoop adv(String klasse, String id, Stats bonus) {
        Bomen.ICOON.put(id, "minecraft:feather");
        return new BoomKnoop(id, klasse, 0, List.of(), -1, 3, bonus, id);
    }

    static void init() {
        for (BoomKnoop b : knopen()) {
            Z3Boomrijen.voegToe(b.klasse(), List.of(b));
        }
        Advancement.CEREMONIE.add(Advancement4::ceremonie);
        Schade.VOOR_KLAP.add(Advancement4::voorKlap);
        Schade.NA_KLAP.add(Advancement4::naKlap);
        Vaardigheden.NA_GEBRUIK.add(Advancement4::naGebruik);
        Haken.LEVEL_OP.add((p, wat) -> {
            String k = wat.substring(0, Math.max(0, wat.lastIndexOf(':')));   // ("<klasse>:N" = the class level)
            if (Klassen.van(k) != null && wat.equals(k + ":" + LEVEL) && Advancement.kanDoen(p, k)) {
                p.sendSystemMessage(Component.translatable("vadsrijk.z3.systemen.adv4.klaar", Component.translatable(Advancement.titelKey(k, STAP)))
                        .withStyle(ChatFormatting.GOLD));
                Tips.toon(p, "vadsrijk.z3.systemen.tip.adv4");
            }
        });
    }

    public static boolean heeft(ServerPlayer p, String klasse) {
        return Advancement.stap(p, klasse) >= STAP;
    }

    static double voorKlap(Schade.Klap k, double n) {
        if (!(k.bron() instanceof ServerPlayer p) || k.soort() == Schade.Soort.WAAR) {
            return n;
        }
        String id = k.skillId() == null ? "" : k.skillId();
        if (id.equals("vahoege_plof") && Spelers.heeftKnoop(p, "r_adv4")) {
            n *= 1.3;
        }
        if (id.equals("sterrenschot") && Spelers.heeftKnoop(p, "j_adv4")) {
            n *= 1.4;
        }
        return n;
    }

    static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        if (!(k.bron() instanceof ServerPlayer p) || !(k.doel() instanceof VadsrijkMob m) || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        String id = k.skillId() == null ? "" : k.skillId();
        long nu = sl.getGameTime();
        // Kruinvalk: Sterrenschot pierces shields: a quarter again as true damage
        if (id.equals("sterrenschot") && Spelers.heeftKnoop(p, "j_adv4") && u.schade() > 0) {
            double extra = u.schade() * 0.25;
            Later.na(1, () -> Schade.doe(new Schade.Klap(p, m, extra, Element.ROZE, Schade.Soort.WAAR, false, "z3_kruinvalk")));
            sl.sendParticles(ParticleTypes.FIREWORK, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
        }
        // Schemerwijze: Meteoorguh leaves a fire field (once per cast)
        if (id.equals("meteoorguh") && Spelers.heeftKnoop(p, "m_adv4") && EENMAAL.putIfAbsent(p.getUUID() + ":veld:" + nu, nu) == null) {
            EENMAAL.values().removeIf(t -> t < nu - 100);
            vuurveld(p, sl, m.position());
        }
    }

    private static void vuurveld(ServerPlayer p, ServerLevel sl, Vec3 c) {
        double s = Spelers.wapenSchade(p) * 0.3;
        for (int t = 0; t <= 100; t += 10) {
            int tt = t;
            Later.na(t, () -> {
                Fx.ring(sl, Fx.stof(Fx.ROZE, 2f), c.add(0, 0.15, 0), 3.5, 28);
                sl.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.2, c.z, 14, 1.6, 0.1, 1.6, 0.01);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 1, c.z, 4, 1.6, 0.4, 1.6, 0.01);
                if (tt % 20 == 0 && !p.isRemoved()) {
                    for (LivingEntity e : Fx.mobsBij(sl, c, 3.5)) {
                        Schade.doe(new Schade.Klap(p, e, s, Element.VUUR, Schade.Soort.MAGIE, false, "z3_vuurveld"));
                        Statussen.geef(e, Status.BRANDEND, 40, Math.max(1, s * 0.3), p);
                    }
                }
            });
        }
        Fx.geluid(sl, c, SoundEvents.BLAZE_SHOOT, 1f, 0.7f);
    }

    /** Nestwachter: the winged cushion pulls every foe within 8 blocks to you. */
    static void naGebruik(ServerPlayer p, Vaardigheid v) {
        if (!"vahoege_plof".equals(v.id()) || !Spelers.heeftKnoop(p, "r_adv4") || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 c = p.position();
        for (int i = 0; i < 2; i++) {
            double a = i == 0 ? -1 : 1;
            Vec3 zij = new Vec3(Math.cos(Math.toRadians(p.getYRot())) * a, 0, Math.sin(Math.toRadians(p.getYRot())) * a);
            for (int f = 0; f < 10; f++) {
                Vec3 v2 = c.add(zij.scale(0.6 + f * 0.35)).add(0, 1.4 + Math.sin(f * 0.4) * 0.6, 0);
                sl.sendParticles(Fx.stof(f % 2 == 0 ? Fx.LICHTROZE : 0xFFFFFF, 1.6f), v2.x, v2.y, v2.z, 2, 0.05, 0.1, 0.05, 0);
            }
        }
        Fx.geluid(sl, c, SoundEvents.ENDER_DRAGON_FLAP, 1.2f, 1.4f);
        Later.na(4, () -> {
            for (LivingEntity e : Fx.mobsBij(sl, c, 8)) {
                if (e instanceof nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas) {
                    continue;   // bosses stay put
                }
                Vec3 naar = c.subtract(e.position());
                double d = naar.length();
                if (d > 1.5) {
                    e.setDeltaMovement(naar.normalize().scale(Math.min(1.2, d * 0.18)).add(0, 0.25, 0));
                    e.hurtMarked = true;
                }
                sl.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.5, e.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
            }
        });
    }

    /** The nest touch of step 4: golden feathers spiral down, blossom wind, a wing-beat. */
    static void ceremonie(ServerPlayer p, int stap) {
        if (stap != STAP || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 c = p.position();
        for (int t = 0; t < 130; t += 5) {
            int tt = t;
            Later.na(t, () -> {
                double r = 3 + Math.sin(tt * 0.05);
                for (int i = 0; i < 6; i++) {
                    double a = Math.PI / 3 * i + tt * 0.06;
                    Vec3 v = c.add(Math.cos(a) * r, 7 - tt / 22.0, Math.sin(a) * r);
                    sl.sendParticles(Fx.stof(Fx.GOUD, 1.8f), v.x, v.y, v.z, 2, 0.05, 0.05, 0.05, 0);
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, v.x, v.y + 0.5, v.z, 1, 0.2, 0.2, 0.2, 0.01);
                }
                if (tt % 25 == 0) {
                    Fx.geluid(sl, c, SoundEvents.ENDER_DRAGON_FLAP, 0.6f, 1.6f);
                }
            });
        }
        Later.na(60, () -> {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 3, c.z, 200, 3, 2, 3, 0.05);
            sl.sendParticles(Fx.stof(Fx.GOUD, 2.4f), c.x, c.y + 1.5, c.z, 80, 1.2, 1.5, 1.2, 0);
            Fx.geluid(sl, c, SoundEvents.ENDER_DRAGON_GROWL, 0.4f, 1.8f);
        });
    }

    /** Test/dev: the player and class this step is for (unused UUID map kept small). */
    static void vergeet(UUID id) {
        EENMAAL.keySet().removeIf(k -> k.startsWith(id.toString()));
    }
}
