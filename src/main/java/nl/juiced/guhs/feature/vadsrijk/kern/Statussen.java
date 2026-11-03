package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * The status effects on entities in the dimension (server side, not saved: they are all short). {@link #geef} adds or refreshes
 * one (the longer/stronger wins); {@link #tick} runs every server tick: BRANDEND/GESPOREN deal {@code sterkte} true damage per
 * second through {@link Schade}, VERDOOFD freezes mobs, PLAKKERIG and SNELPOOTJES change the walking speed (see {@link Spelers#stats}),
 * KNUFFELSCHILD absorbs {@code sterkte} damage (used up in {@link Schade}).
 */
public final class Statussen {
    public static final class Actief {
        public int ticks;
        public double sterkte;
        @Nullable
        public UUID bron;

        Actief(int ticks, double sterkte, @Nullable UUID bron) {
            this.ticks = ticks;
            this.sterkte = sterkte;
            this.bron = bron;
        }
    }

    private static final Map<LivingEntity, EnumMap<Status, Actief>> ALLE = new WeakHashMap<>();

    private Statussen() {
    }

    public static void geef(LivingEntity e, Status s, int ticks, double sterkte, @Nullable LivingEntity bron) {
        if (e.level().isClientSide()) {
            return;
        }
        EnumMap<Status, Actief> m = ALLE.computeIfAbsent(e, k -> new EnumMap<>(Status.class));
        Actief a = m.get(s);
        if (a == null) {
            m.put(s, new Actief(ticks, sterkte, bron == null ? null : bron.getUUID()));
        } else {
            a.ticks = Math.max(a.ticks, ticks);
            a.sterkte = Math.max(a.sterkte, sterkte);
        }
        Haken.STATUS.forEach(h -> h.accept(e, s));
    }

    public static boolean heeft(LivingEntity e, Status s) {
        EnumMap<Status, Actief> m = ALLE.get(e);
        return m != null && m.containsKey(s);
    }

    public static double sterkte(LivingEntity e, Status s) {
        EnumMap<Status, Actief> m = ALLE.get(e);
        Actief a = m == null ? null : m.get(s);
        return a == null ? 0 : a.sterkte;
    }

    @Nullable
    public static Actief actief(LivingEntity e, Status s) {
        EnumMap<Status, Actief> m = ALLE.get(e);
        return m == null ? null : m.get(s);
    }

    public static int ticks(LivingEntity e, Status s) {
        Actief a = actief(e, s);
        return a == null ? 0 : a.ticks;
    }

    public static void wis(LivingEntity e, Status s) {
        EnumMap<Status, Actief> m = ALLE.get(e);
        if (m != null) {
            m.remove(s);
        }
    }

    public static void wisAlles(LivingEntity e) {
        ALLE.remove(e);
    }

    /** Every status on this entity (read only). */
    public static Map<Status, Actief> van(LivingEntity e) {
        EnumMap<Status, Actief> m = ALLE.get(e);
        return m == null ? Map.of() : java.util.Collections.unmodifiableMap(m);
    }

    /** One server tick (called by kern). */
    public static void tick() {
        Iterator<Map.Entry<LivingEntity, EnumMap<Status, Actief>>> it = ALLE.entrySet().iterator();
        java.util.List<Runnable> later = new java.util.ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, EnumMap<Status, Actief>> en = it.next();
            LivingEntity e = en.getKey();
            if (e == null || e.isRemoved()) {
                it.remove();
                continue;
            }
            Iterator<Map.Entry<Status, Actief>> si = en.getValue().entrySet().iterator();
            while (si.hasNext()) {
                Map.Entry<Status, Actief> se = si.next();
                Actief a = se.getValue();
                Status s = se.getKey();
                if ((s == Status.BRANDEND || s == Status.GESPOREN) && a.ticks % 20 == 0 && e.level() instanceof ServerLevel sl) {
                    double n = a.sterkte;
                    UUID bron = a.bron;
                    later.add(() -> {
                        LivingEntity b = bron == null ? null : (sl.getEntity(bron) instanceof LivingEntity l ? l : null);
                        Schade.doe(new Schade.Klap(b, e, n, s == Status.BRANDEND ? Element.VUUR : Element.NATUUR, Schade.Soort.WAAR, false, "status"));
                        sl.sendParticles(s == Status.BRANDEND ? ParticleTypes.FLAME : ParticleTypes.SPORE_BLOSSOM_AIR, e.getX(), e.getY() + e.getBbHeight() * 0.6,
                                e.getZ(), 4, 0.25, 0.3, 0.25, 0.01);
                    });
                }
                if (s == Status.VERDOOFD && a.ticks % 10 == 0 && e.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.WAX_OFF, e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 2, 0.2, 0.05, 0.2, 0.0);
                }
                if (--a.ticks <= 0 || (s == Status.KNUFFELSCHILD && a.sterkte <= 0)) {
                    si.remove();
                }
            }
            if (en.getValue().isEmpty()) {
                it.remove();
            }
        }
        later.forEach(Runnable::run);
    }
}
