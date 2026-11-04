package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The status BEVROREN (ronde 15: zone 2 has no new element; ice is a status). Kern only knows the id {@link Status#BEVROREN}
 * (z2-wezens uses the same id); everything it does is here, on kern hooks:
 * <ul>
 *   <li>slowed: players -30 % Loopsnelheid and -15 % Aanvalssnelheid ({@code Spelers.EXTRA_STATS}); mobs -45 % movement speed
 *   (an attribute modifier while it lasts); snowflakes;</li>
 *   <li>heavy hits do {@link #ZWAAR_EXTRA} more ({@code Schade.VOOR_KLAP}): skills and boss attacks, and the basic hits of slow
 *   weapons (hamer, kruisboog); not normal basic hits, DoTs or falls;</li>
 *   <li>the warme chocomelk (Kandijkom) thaws you and keeps you unfreezable for a while ({@link #immuun}).</li>
 * </ul>
 * Give it with {@link #geef} (respects the immunity) or plain {@code Statussen.geef(e, Status.BEVROREN, ticks, 1, bron)}.
 */
public final class Bevroren {
    /** Heavy hits on a frozen target: +30 %. */
    public static final double ZWAAR_EXTRA = 0.30;
    public static final double LOOP_SPELER = -30, AANVAL_SPELER = -15, LOOP_MOB = -0.45;
    /** Weapon kinds whose basic attack counts as heavy. */
    public static final Set<String> ZWARE_SOORTEN = Set.of("hamer", "kruisboog");
    private static final Identifier MOD_ID = Guhs.id("vr_z2_bevroren");
    private static final Map<LivingEntity, Boolean> TRAAG = new WeakHashMap<>();
    /** Player -> game time until which they can't freeze (chocomelk). */
    private static final Map<UUID, Long> IMMUUN = new HashMap<>();

    private Bevroren() {
    }

    static void init() {
        Spelers.EXTRA_STATS.add(Bevroren::spelerStats);
        Schade.VOOR_KLAP.add(Bevroren::voorKlap);
        Schade.NA_KLAP.add(Bevroren::naKlap);
        Haken.STATUS.add((e, s) -> {
            if (s == Status.BEVROREN && e instanceof ServerPlayer p && immuun(p)) {
                Statussen.wis(p, Status.BEVROREN);
            } else if (s == Status.BEVROREN && e instanceof VadsrijkMob && !TRAAG.containsKey(e)) {
                AttributeInstance a = e.getAttribute(Attributes.MOVEMENT_SPEED);
                if (a != null && a.getModifier(MOD_ID) == null) {
                    a.addTransientModifier(new AttributeModifier(MOD_ID, LOOP_MOB, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                TRAAG.put(e, true);
            }
        });
    }

    /** Freezes {@code e} for {@code ticks} (not when it's an immune player). */
    public static void geef(LivingEntity e, int ticks, @Nullable LivingEntity bron) {
        if (e instanceof ServerPlayer p && immuun(p)) {
            return;
        }
        boolean al = Statussen.heeft(e, Status.BEVROREN);
        Statussen.geef(e, Status.BEVROREN, ticks, 1, bron);
        if (!al && e.level() instanceof ServerLevel sl) {
            sl.playSound(null, e.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 0.7f, 1.6f);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 14, 0.35, 0.4, 0.35, 0.02);
        }
    }

    public static boolean heeft(LivingEntity e) {
        return Statussen.heeft(e, Status.BEVROREN);
    }

    public static boolean immuun(ServerPlayer p) {
        Long t = IMMUUN.get(p.getUUID());
        return t != null && t > p.level().getGameTime();
    }

    /** Thaws and protects for {@code ticks} (the warme chocomelk). */
    public static void ontdooi(ServerPlayer p, int ticks) {
        Statussen.wis(p, Status.BEVROREN);
        IMMUUN.put(p.getUUID(), p.level().getGameTime() + ticks);
    }

    static Stats spelerStats(ServerPlayer p) {
        return heeft(p) ? Stats.van(Stat.LOOPSNELHEID, LOOP_SPELER, Stat.AANVALSSNELHEID, AANVAL_SPELER) : null;
    }

    /** Is this a heavy hit (skills, boss attacks, slow-weapon basics)? */
    public static boolean zwaar(Schade.Klap k) {
        if (k.soort() == Schade.Soort.WAAR) {
            return false;
        }
        String id = k.skillId() == null ? "" : k.skillId();
        if (id.startsWith("basis")) {
            if (k.bron() instanceof ServerPlayer p) {
                Uitrusting w = Spelers.wapen(p);
                return w != null && w.basisType() != null && (ZWARE_SOORTEN.contains(w.basisType().soort()) || w.type().equals("pluishamer"));
            }
            return false;
        }
        return !(id.equals("mob") || id.equals("status") || id.equals("val") || id.equals("omgeving") || id.isEmpty());
    }

    static double voorKlap(Schade.Klap k, double n) {
        return k.doel() != null && heeft(k.doel()) && zwaar(k) ? n * (1 + ZWAAR_EXTRA) : n;
    }

    /** The ijsglans gems: a chance to freeze what you hit (not with DoTs, nor with the freeze's own follow-ups). */
    static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        if (u.schade() <= 0 || !(k.bron() instanceof ServerPlayer p) || !(k.doel() instanceof VadsrijkMob) || k.soort() == Schade.Soort.WAAR) {
            return;
        }
        double kans = Kristallen.ijsKans(p);
        if (kans > 0 && p.getRandom().nextDouble() * 100 < kans) {
            geef(k.doel(), 40, p);
        }
    }

    /** Every 5 ticks: the frozen mobs' snowflakes, and their slow removed when it's over. */
    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 5 != 0) {
            return;
        }
        Iterator<LivingEntity> it = TRAAG.keySet().iterator();
        while (it.hasNext()) {
            LivingEntity l = it.next();
            if (l == null) {
                it.remove();
                continue;
            }
            if (l.isRemoved() || !heeft(l)) {
                AttributeInstance a = l.getAttribute(Attributes.MOVEMENT_SPEED);
                if (a != null) {
                    a.removeModifier(MOD_ID);
                }
                it.remove();
            } else if (l.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, l.getX(), l.getY() + l.getBbHeight() * 0.6, l.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }
}
