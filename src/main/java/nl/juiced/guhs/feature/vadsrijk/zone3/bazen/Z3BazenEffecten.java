package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/** The effects of the zone-3 named boss items ({@link Schade#VOOR_KLAP} + {@link Schade#NA_KLAP}). Only inside the dimension. */
public final class Z3BazenEffecten {
    private static final Map<UUID, Integer> KLAPPEN = new HashMap<>();
    private static final Map<String, Long> AFKOEL = new HashMap<>();

    private Z3BazenEffecten() {
    }

    static void register() {
        Schade.VOOR_KLAP.add(Z3BazenEffecten::voorKlap);
        Schade.NA_KLAP.add(Z3BazenEffecten::naKlap);
    }

    @Nullable
    static String gedragen(ServerPlayer p, EquipmentSlot slot) {
        Uitrusting u = Rollen.van(p.getItemBySlot(slot));
        return u == null || u.verpluisd() || u.level() > Spelers.level(p) ? null : u.type();
    }

    @Nullable
    static String wapen(ServerPlayer p) {
        Uitrusting u = Spelers.wapen(p);
        return u == null || u.verpluisd() ? null : u.type();
    }

    @Nullable
    static String sieraad(ServerPlayer p, Slot s) {
        Uitrusting u = Rollen.van(Sieraden.get(p, s));
        return u == null || u.verpluisd() ? null : u.type();
    }

    private static boolean klaar(ServerPlayer p, String wat, long nu, int ticks) {
        String k = p.getUUID() + wat;
        if (nu < AFKOEL.getOrDefault(k, 0L)) {
            return false;
        }
        AFKOEL.put(k, nu + ticks);
        return true;
    }

    private static boolean eigen(Schade.Klap k) {
        String s = k.skillId() == null ? "" : k.skillId();
        return s.startsWith("z3bazen_") || s.startsWith("z3item_") || s.equals("status");
    }

    private static double voorKlap(Schade.Klap k, double n) {
        if (eigen(k)) {
            return n;
        }
        if (k.bron() instanceof ServerPlayer p && Vadsrijk.in(p) && k.doel() instanceof VadsrijkMob m) {
            String w = wapen(p);
            // Roquefortsabel: +20 % on a burning target
            if ("z3baas_roquefortsabel".equals(w) && Statussen.heeft(m, Status.BRANDEND)) {
                n *= 1.2;
            }
            // De Paraplu van de Kapitein: +15 % while you float (in the air)
            if ("z3baas_paraplu".equals(w) && !p.onGround()) {
                n *= 1.15;
            }
            // Roofvogelklauw: +20 % from more than 10 blocks away
            if ("z3baas_roofklauw".equals(w) && p.distanceToSqr(m) > 100) {
                n *= 1.2;
            }
            // Schimmelbreker: +25 % on a target with Gesporen or Plakkerig
            if ("z3baas_schimmelbreker".equals(w) && (Statussen.heeft(m, Status.GESPOREN) || Statussen.heeft(m, Status.PLAKKERIG))) {
                n *= 1.25;
            }
        }
        return n;
    }

    private static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        if (eigen(k) || u.schade() <= 0) {
            return;
        }
        if (k.bron() instanceof ServerPlayer p && Vadsrijk.in(p) && k.doel() instanceof VadsrijkMob doel && p.level() instanceof ServerLevel sl) {
            String w = wapen(p);
            long nu = sl.getGameTime();
            if ("z3baas_kooisleutel".equals(w) && KLAPPEN.merge(p.getUUID(), 1, Integer::sum) % 5 == 0) {
                // every 5th hit opens a "cage": a friendly mouse swarm nibbles along for 3 s
                Vec3 v = doel.position();
                double per = u.schade() * 0.25;
                Z3Scenes.loop(t -> {
                    if (t % 20 == 0 && !doel.geplofd()) {
                        sl.sendParticles(ParticleTypes.HEART, v.x, v.y + 1, v.z, 2, 0.5, 0.3, 0.5, 0);
                        sl.sendParticles(new DustParticleOptions(0xD8C8C8, 1.2f), doel.getX(), doel.getY() + 0.3, doel.getZ(), 12, 0.6, 0.2, 0.6, 0);
                        sl.playSound(null, doel.blockPosition(), SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 0.6f, 2f);
                        Schade.doe(new Schade.Klap(p, doel, per, k.element(), Schade.Soort.WAAR, false, "z3item_muisjes"));
                    }
                    return t >= 60;
                });
            } else if ("z3baas_touwknots".equals(w) && KLAPPEN.merge(p.getUUID(), 1, Integer::sum) % 4 == 0) {
                // every 4th hit: a rope lasso pulls the creatures around to your target
                for (VadsrijkMob m : sl.getEntitiesOfClass(VadsrijkMob.class, doel.getBoundingBox().inflate(4), m -> m != doel && !m.geplofd())) {
                    Vec3 r = doel.position().subtract(m.position()).multiply(1, 0, 1);
                    if (r.lengthSqr() > 0.5) {
                        r = r.normalize().scale(0.6);
                        m.setDeltaMovement(r.x, 0.2, r.z);
                        m.hurtMarked = true;
                    }
                }
            } else if ("z3baas_lontstaf".equals(w) && u.kritiek()) {
                Statussen.geef(doel, Status.BRANDEND, 60, Math.max(2, Spelers.level(p) * 0.6), p);
                sl.sendParticles(ParticleTypes.FLAME, doel.getX(), doel.getY() + 1, doel.getZ(), 15, 0.4, 0.5, 0.4, 0.03);
            } else if ("z3baas_hartwortel".equals(w)) {
                Schade.heal(p, p, u.schade() * 0.03);   // a little life back through the heart root
            } else if ("z3baas_lantaarnstaf".equals(w) && u.kritiek() && klaar(p, "lantaarn", nu, 400)) {
                Statussen.geef(p, Status.KNUFFELSCHILD, 80, Spelers.maxVads(p) * 0.05, p);
                sl.sendParticles(new DustParticleOptions(0xFFC870, 1.6f), p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.6, 0.5, 0);
            }
            if ("z3baas_bleumonocle".equals(gedragen(p, EquipmentSlot.HEAD)) && p.getRandom().nextDouble() < 0.10) {
                Statussen.geef(doel, Status.GESPOREN, 60, Math.max(2, Spelers.level(p) * 0.4), p);
            }
        }
        if (k.doel() instanceof ServerPlayer p && Vadsrijk.in(p) && p.level() instanceof ServerLevel sl && !(k.bron() instanceof ServerPlayer)) {
            long nu = sl.getGameTime();
            if ("z3baas_sokkencape".equals(gedragen(p, EquipmentSlot.CHEST)) && p.getRandom().nextDouble() < 0.12 && klaar(p, "sokken", nu, 400)) {
                Schade.heal(p, p, Spelers.maxVads(p) * 0.05);   // warm knitted socks
                sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 2, 0.3, 0.2, 0.3, 0);
            }
            if ("z3baas_kruinveren".equals(gedragen(p, EquipmentSlot.CHEST)) && Spelers.vads(p) < Spelers.maxVads(p) * 0.3 && klaar(p, "veren", nu, 600)) {
                Statussen.geef(p, Status.SNELPOOTJES, 60, 0.4, p);
                sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 20, 0.5, 0.2, 0.5, 0.05);
            }
            // De Eierwarmer: low on Vads -> the team around you glows pink and heals (60 s cooldown)
            if ("z3baas_eierwarmer".equals(sieraad(p, Slot.KETTING)) && Spelers.vads(p) < Spelers.maxVads(p) * 0.25 && klaar(p, "ei", nu, 1200)) {
                for (ServerPlayer m : sl.players()) {
                    if (m.distanceToSqr(p) < 64) {
                        Schade.heal(p, m, Spelers.maxVads(m) * 0.15);
                        sl.sendParticles(new DustParticleOptions(0xFF8AD0, 1.8f), m.getX(), m.getY() + 1, m.getZ(), 25, 0.5, 0.7, 0.5, 0);
                    }
                }
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.2f);
            }
        }
    }
}
