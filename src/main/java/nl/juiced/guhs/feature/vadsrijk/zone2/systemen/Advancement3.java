package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.BoomKnoop;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.klassen.Advancement;
import nl.juiced.guhs.feature.vadsrijk.klassen.Fx;
import nl.juiced.guhs.feature.vadsrijk.klassen.Later;
import nl.juiced.guhs.feature.vadsrijk.klassen.ridder.Knuffelridder;

/**
 * The 3rd job advancement at lv 30 (ronde 15). The klassen {@link Advancement} has a 3rd step; this class adds the hidden
 * nodes {@code <x>_adv3} (stat bonus), the crystal touch of the ceremony, and the real unlock per class:
 * <ul>
 *   <li>Knuffelridder -> <b>Kristalkampioen</b>, Kristalwal: while Knuffelschild is up, every enemy that hits you freezes 3 s;
 *   Kussenklap does 40 % more to frozen enemies.</li>
 *   <li>Pluisjager -> <b>Poolstervalk</b>, IJsster: every 5th basic shot that hits fires an ice star along (1x weapon damage, freezes
 *   2 s); Sterrenschot freezes everything it pierces for 4 s.</li>
 *   <li>Vadsmagier -> <b>Kristalorakel</b>, Spiegelecho: Roze Vuurbal freezes for 1.5 s, and when it hits an enemy that was already
 *   frozen, the ice shatters in a second explosion (1.2x weapon damage, radius 3, once per cast).</li>
 * </ul>
 * Where: the ordetempel desk in Zwamzoet (kern of the klassen slice) or the kristalkapel altar in Kandijkom (z2-npc calls
 * {@link Advancement#doe}).
 */
public final class Advancement3 {
    public static final int LEVEL = 30;
    private static final Map<UUID, Integer> SCHOTEN = new HashMap<>();
    private static final Map<UUID, Long> ECHO = new HashMap<>();

    private Advancement3() {
    }

    public static List<BoomKnoop> knopen() {
        return List.of(adv(Klassen.RIDDER, "r_adv3", Stats.van(Stat.KRACHT, 6, Stat.VERDEDIGING, 8, Stat.VADS, 150)),
                adv(Klassen.JAGER, "j_adv3", Stats.van(Stat.BEHENDIGHEID, 6, Stat.KRITIEK, 5)),
                adv(Klassen.MAGIER, "m_adv3", Stats.van(Stat.WIJSHEID, 6, Stat.ELEMENTSCHADE, 10)));
    }

    private static BoomKnoop adv(String klasse, String id, Stats bonus) {
        nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen.ICOON.put(id, "minecraft:amethyst_cluster");
        return new BoomKnoop(id, klasse, 0, List.of(), -1, 2, bonus, id);
    }

    static void init() {
        for (BoomKnoop b : knopen()) {
            Boomrijen.voegToe(b.klasse(), List.of(b));
        }
        Advancement.CEREMONIE.add(Advancement3::ceremonie);
        Schade.VOOR_KLAP.add(Advancement3::voorKlap);
        Schade.NA_KLAP.add(Advancement3::naKlap);
        Haken.LEVEL_OP.add((p, wat) -> {
            if (Spelers.level(p) == LEVEL && Advancement.kanDoen(p)) {
                String k = Spelers.klasse(p).id();
                p.sendSystemMessage(Component.translatable("vadsrijk.z2.systemen.adv3.klaar", Component.translatable(Advancement.titelKey(k, 3)))
                        .withStyle(ChatFormatting.GOLD));
                Tips.toon(p, "vadsrijk.z2.systemen.tip.adv3");
            }
        });
    }

    static boolean heeft(ServerPlayer p, String id) {
        return Spelers.heeftKnoop(p, id);
    }

    /** Kristalwal (freeze who hits your shield) and the Kussenklap bonus on frozen enemies. */
    static double voorKlap(Schade.Klap k, double n) {
        if (k.doel() instanceof ServerPlayer p && k.bron() instanceof VadsrijkMob m && heeft(p, "r_adv3") && Knuffelridder.blokt(p)) {
            Bevroren.geef(m, 60, p);
        }
        if (k.bron() instanceof ServerPlayer p && "kussenklap".equals(k.skillId()) && Bevroren.heeft(k.doel()) && heeft(p, "r_adv3")) {
            n *= 1.4;
        }
        return n;
    }

    static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        if (u.schade() <= 0 || !(k.bron() instanceof ServerPlayer p) || !(k.doel() instanceof VadsrijkMob m)) {
            return;
        }
        String id = k.skillId() == null ? "" : k.skillId();
        if (id.equals("basis") && k.soort() == Schade.Soort.AFSTAND && heeft(p, "j_adv3")) {
            int n = SCHOTEN.merge(p.getUUID(), 1, Integer::sum);
            if (n >= 5) {
                SCHOTEN.put(p.getUUID(), 0);
                ijsster(p, m);
            }
        }
        if (id.equals("sterrenschot") && heeft(p, "j_adv3")) {
            Bevroren.geef(m, 80, p);
        }
        if (id.equals("roze_vuurbal") && heeft(p, "m_adv3")) {
            boolean was = Bevroren.heeft(m);
            Bevroren.geef(m, 30, p);
            long nu = p.level().getGameTime();
            if (was && !Long.valueOf(nu).equals(ECHO.get(p.getUUID()))) {
                ECHO.put(p.getUUID(), nu);
                Vec3 c = m.position();
                Later.na(4, () -> echo(p, c));
            }
        }
    }

    private static void ijsster(ServerPlayer p, LivingEntity doel) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Fx.lijn(sl, ParticleTypes.SNOWFLAKE, p.getEyePosition(), doel.position().add(0, doel.getBbHeight() * 0.5, 0), 0.6);
        sl.sendParticles(ParticleTypes.END_ROD, doel.getX(), doel.getY() + doel.getBbHeight() * 0.5, doel.getZ(), 12, 0.3, 0.3, 0.3, 0.05);
        Fx.geluid(sl, doel.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.6f);
        Schade.doe(new Schade.Klap(p, doel, Spelers.wapenSchade(p), Spelers.wapenElement(p), Schade.Soort.AFSTAND, true, "z2_ijsster"));
        Bevroren.geef(doel, 40, p);
    }

    private static void echo(ServerPlayer p, Vec3 c) {
        if (!(p.level() instanceof ServerLevel sl) || p.isRemoved()) {
            return;
        }
        sl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 0.8, c.z, 60, 1.5, 0.8, 1.5, 0.1);
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.8, c.z, 20, 1.2, 0.6, 1.2, 0.08);
        Fx.ring(sl, ParticleTypes.SNOWFLAKE, c.add(0, 0.2, 0), 3, 24);
        Fx.geluid(sl, c, SoundEvents.GLASS_BREAK, 1.2f, 1.3f);
        Fx.geluid(sl, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1f, 0.9f);
        double schade = Spelers.wapenSchade(p) * 1.2;
        for (LivingEntity e : Fx.mobsBij(sl, c, 3)) {
            Schade.doe(new Schade.Klap(p, e, schade, Spelers.wapenElement(p), Schade.Soort.MAGIE, true, "z2_spiegelecho"));
        }
    }

    /** The crystal touch of step 3: a ring of rising ice shards and chimes around the beams. */
    static void ceremonie(ServerPlayer p, int stap) {
        if (stap != 3 || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 c = p.position();
        for (int t = 0; t < 120; t += 6) {
            int tt = t;
            Later.na(t, () -> {
                double r = 4.5 - tt / 40.0;
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i - tt * 0.05;
                    Vec3 v = c.add(Math.cos(a) * r, tt / 30.0, Math.sin(a) * r);
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, v.x, v.y, v.z, 2, 0.05, 0.2, 0.05, 0.01);
                    sl.sendParticles(Fx.stof(0x9AD8FF, 1.6f), v.x, v.y + 0.4, v.z, 1, 0.05, 0.1, 0.05, 0);
                }
                if (tt % 24 == 0) {
                    Fx.geluid(sl, c, SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 0.8f + tt / 150f);
                }
            });
        }
        Later.na(60, () -> {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 2, c.z, 150, 2, 2, 2, 0.15);
            Fx.geluid(sl, c, SoundEvents.GLASS_BREAK, 1.2f, 0.7f);
        });
    }
}
