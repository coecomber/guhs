package nl.juiced.guhs.feature.vadsrijk.zone2.bazen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/** The effects of the zone-2 named boss items ({@link Schade#VOOR_KLAP} + {@link Schade#NA_KLAP}). Only inside the dimension. */
public final class Z2BazenEffecten {
    private static final Map<UUID, Integer> KLAPPEN = new HashMap<>();
    private static final Map<UUID, Long> AFKOEL = new HashMap<>();

    private Z2BazenEffecten() {
    }

    static void register() {
        Schade.VOOR_KLAP.add(Z2BazenEffecten::voorKlap);
        Schade.NA_KLAP.add(Z2BazenEffecten::naKlap);
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

    private static boolean klaar(ServerPlayer p, String wat, long nu, int ticks) {
        String k = p.getUUID() + wat;
        UUID id = UUID.nameUUIDFromBytes(k.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (nu < AFKOEL.getOrDefault(id, 0L)) {
            return false;
        }
        AFKOEL.put(id, nu + ticks);
        return true;
    }

    private static double voorKlap(Schade.Klap k, double n) {
        String skill = k.skillId() == null ? "" : k.skillId();
        if (skill.startsWith("z2bazen_") || skill.equals("status")) {
            return n;
        }
        // De Schaduwloze: +25 % on a creature that is still at full Vads (the opening shot)
        if (k.bron() instanceof ServerPlayer p && Vadsrijk.in(p) && k.doel() instanceof VadsrijkMob m && "z2baas_schaduwloze".equals(wapen(p))
                && m.vads() >= m.maxVads() * 0.99) {
            n *= 1.25;
        }
        // Spiegelkroontje van Zucht: 10 % (20 s cooldown) the hit is mirrored back at half strength and you take nothing
        if (k.doel() instanceof ServerPlayer p && Vadsrijk.in(p) && k.bron() instanceof VadsrijkMob m && "z2baas_spiegelkroon".equals(gedragen(p, EquipmentSlot.HEAD))
                && p.getRandom().nextDouble() < 0.10 && p.level() instanceof ServerLevel sl && klaar(p, "kroon", sl.getGameTime(), 400)) {
            sl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.5f);
            Schade.doe(new Schade.Klap(p, m, n * 0.5, k.element(), Schade.Soort.WAAR, false, "z2bazen_spiegelkroon"));
            return 0;
        }
        return n;
    }

    private static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        String skill = k.skillId() == null ? "" : k.skillId();
        if (skill.startsWith("z2bazen_") || skill.equals("status") || u.schade() <= 0) {
            return;
        }
        if (k.bron() instanceof ServerPlayer p && Vadsrijk.in(p) && k.doel() instanceof VadsrijkMob doel && p.level() instanceof ServerLevel sl) {
            String w = wapen(p);
            if ("z2baas_galmklauw".equals(w) && KLAPPEN.merge(p.getUUID(), 1, Integer::sum) % 5 == 0) {
                sl.sendParticles(ParticleTypes.END_ROD, doel.getX(), doel.getY() + 1, doel.getZ(), 20, 1.5, 0.3, 1.5, 0.05);
                sl.playSound(null, doel.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
                for (VadsrijkMob m : sl.getEntitiesOfClass(VadsrijkMob.class, doel.getBoundingBox().inflate(3), m -> !m.geplofd())) {
                    Schade.doe(new Schade.Klap(p, m, u.schade() * 0.4, k.element(), Schade.Soort.WAAR, false, "z2bazen_echo"));
                }
            } else if ("z2baas_spiegelstaf".equals(w) && u.kritiek()) {
                VadsrijkMob tweede = sl.getEntitiesOfClass(VadsrijkMob.class, doel.getBoundingBox().inflate(6), m -> m != doel && !m.geplofd()).stream()
                        .findFirst().orElse(null);
                if (tweede != null) {
                    for (double t = 0; t <= 1; t += 0.1) {
                        sl.sendParticles(ParticleTypes.END_ROD, doel.getX() + (tweede.getX() - doel.getX()) * t, doel.getY() + 1 + (tweede.getY() - doel.getY()) * t,
                                doel.getZ() + (tweede.getZ() - doel.getZ()) * t, 1, 0, 0, 0, 0);
                    }
                    Schade.doe(new Schade.Klap(p, tweede, u.schade() * 0.3, k.element(), Schade.Soort.WAAR, false, "z2bazen_spiegelstraal"));
                }
            } else if ("z2baas_boorhamer".equals(w) && KLAPPEN.merge(p.getUUID(), 1, Integer::sum) % 3 == 0) {
                Statussen.geef(doel, Status.BRANDEND, 60, Math.max(2, Spelers.level(p) * 0.6), p);
                sl.sendParticles(ParticleTypes.FLAME, doel.getX(), doel.getY() + 1, doel.getZ(), 15, 0.4, 0.5, 0.4, 0.03);
            }
            if ("z2baas_boorbril".equals(gedragen(p, EquipmentSlot.HEAD)) && p.getRandom().nextDouble() < 0.12 && klaar(p, "bril", sl.getGameTime(), 400)) {
                Statussen.geef(p, Status.VAHOEG, 80, 0.15, p);
                sl.sendParticles(ParticleTypes.WAX_ON, p.getX(), p.getY() + 1.8, p.getZ(), 10, 0.3, 0.2, 0.3, 0.1);
            }
        }
        if (k.doel() instanceof ServerPlayer p && Vadsrijk.in(p) && k.bron() instanceof LivingEntity bron && !(bron instanceof ServerPlayer)
                && p.level() instanceof ServerLevel sl) {
            if ("z2baas_ijspelsmantel".equals(gedragen(p, EquipmentSlot.CHEST)) && p.getRandom().nextDouble() < 0.15) {
                Statussen.geef(bron, Z2Baas.bevroren(), 40, 0.4, p);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, bron.getX(), bron.getY() + 1, bron.getZ(), 20, 0.4, 0.5, 0.4, 0.03);
            }
            if ("z2baas_stoomlaarzen".equals(gedragen(p, EquipmentSlot.FEET)) && Spelers.vads(p) < Spelers.maxVads(p) * 0.3
                    && klaar(p, "laars", sl.getGameTime(), 600)) {
                sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 50, 1.5, 0.4, 1.5, 0.1);
                sl.playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 0.8f);
                for (VadsrijkMob m : sl.getEntitiesOfClass(VadsrijkMob.class, p.getBoundingBox().inflate(4), m -> !m.geplofd())) {
                    var weg = m.position().subtract(p.position()).multiply(1, 0, 1);
                    weg = weg.lengthSqr() < 0.01 ? new net.minecraft.world.phys.Vec3(1, 0, 0) : weg.normalize();
                    m.setDeltaMovement(weg.x * 1.2, 0.4, weg.z * 1.2);
                    m.hurtMarked = true;
                }
                Statussen.geef(p, Status.SNELPOOTJES, 80, 0.4, p);
            }
        }
    }
}
