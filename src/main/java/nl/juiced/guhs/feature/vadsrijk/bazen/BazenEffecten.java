package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;

/** The effects of the named boss items (hooked on {@link Schade#NA_KLAP} + a player tick). Only inside the dimension. */
public final class BazenEffecten {
    private static final Map<UUID, Integer> KLAPPEN = new HashMap<>();
    private static final Map<UUID, Long> AFKOEL_SNOR = new HashMap<>(), AFKOEL_LAARS = new HashMap<>();

    private BazenEffecten() {
    }

    static void register() {
        Schade.NA_KLAP.add(BazenEffecten::naKlap);
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

    private static void naKlap(Schade.Uitkomst u) {
        Schade.Klap k = u.klap();
        String skill = k.skillId() == null ? "" : k.skillId();
        if (skill.startsWith("bazen_") || skill.equals("status") || u.schade() <= 0) {
            return;
        }
        if (k.bron() instanceof ServerPlayer p && Vadsrijk.in(p) && k.doel() instanceof VadsrijkMob doel && p.level() instanceof ServerLevel sl) {
            String w = wapen(p);
            if ("baas_klodderknots".equals(w)) {
                int n = KLAPPEN.merge(p.getUUID(), 1, Integer::sum);
                if (n % 5 == 0) {
                    HulpSlijmpje h = BazenRegistry.HULPSLIJMPJE.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
                    if (h != null) {
                        h.snapTo(p.getX(), p.getY() + 0.5, p.getZ(), p.getYRot(), 0);
                        h.stel(p, doel, Math.max(4, Spelers.wapenSchade(p) * 0.4));
                        sl.addFreshEntity(h);
                        sl.playSound(null, p.blockPosition(), SoundEvents.SLIME_JUMP_SMALL, SoundSource.PLAYERS, 1f, 1.6f);
                    }
                }
            } else if ("baas_pluisboog".equals(w)) {
                Statussen.geef(doel, Status.PLAKKERIG, 40, 0.35, p);
            } else if ("baas_sporenstaf".equals(w) && u.kritiek()) {
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, doel.getX(), doel.getY() + 1, doel.getZ(), 40, 1.5, 0.6, 1.5, 0.01);
                for (VadsrijkMob m : sl.getEntitiesOfClass(VadsrijkMob.class, doel.getBoundingBox().inflate(3), m -> !m.geplofd())) {
                    Statussen.geef(m, Status.GESPOREN, 80, Math.max(2, Spelers.level(p) * 0.6), p);
                }
            }
        }
        if (k.doel() instanceof ServerPlayer p && Vadsrijk.in(p) && k.bron() instanceof LivingEntity bron && !(bron instanceof ServerPlayer)
                && p.level() instanceof ServerLevel sl) {
            long nu = sl.getGameTime();
            if ("baas_wortelwambuis".equals(gedragen(p, EquipmentSlot.CHEST)) && p.getRandom().nextDouble() < 0.15) {
                Statussen.geef(bron, Status.VERDOOFD, 20, 1, p);
                sl.sendParticles(ParticleTypes.COMPOSTER, bron.getX(), bron.getY() + 0.3, bron.getZ(), 16, 0.4, 0.2, 0.4, 0.02);
            }
            if ("baas_neppe_snor".equals(gedragen(p, EquipmentSlot.HEAD)) && nu >= AFKOEL_SNOR.getOrDefault(p.getUUID(), 0L)
                    && p.getRandom().nextDouble() < 0.12) {
                AFKOEL_SNOR.put(p.getUUID(), nu + 300);
                for (Mob m : sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), m -> m.getTarget() == p)) {
                    m.setTarget(null);
                }
                Statussen.geef(p, Status.SNELPOOTJES, 60, 0.3, p);
                poef(sl, p.position());
            }
            if ("baas_rookbomlaarzen".equals(gedragen(p, EquipmentSlot.FEET)) && Spelers.vads(p) < Spelers.maxVads(p) * 0.3
                    && nu >= AFKOEL_LAARS.getOrDefault(p.getUUID(), 0L)) {
                AFKOEL_LAARS.put(p.getUUID(), nu + 600);
                Vec3 weg = p.position().subtract(bron.position()).multiply(1, 0, 1);
                weg = weg.lengthSqr() < 0.01 ? p.getLookAngle().multiply(-1, 0, -1) : weg.normalize();
                poef(sl, p.position());
                Vec3 doel = p.position().add(weg.scale(6));
                if (!p.randomTeleport(doel.x, doel.y + 1, doel.z, false)) {
                    p.setDeltaMovement(weg.x * 1.6, 0.5, weg.z * 1.6);
                    p.hurtMarked = true;
                }
                poef(sl, p.position());
                Statussen.geef(p, Status.SNELPOOTJES, 80, 0.4, p);
            }
        }
    }

    public static void poef(ServerLevel sl, Vec3 v) {
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, v.x, v.y + 1, v.z, 30, 0.6, 0.8, 0.6, 0.02);
        sl.sendParticles(ParticleTypes.POOF, v.x, v.y + 1, v.z, 20, 0.5, 0.6, 0.5, 0.05);
        sl.playSound(null, v.x, v.y, v.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    private static final DustParticleOptions GOUD = new DustParticleOptions(0xFFD24A, 0.9f), ROZE = new DustParticleOptions(0xFF5FC8, 0.9f);

    /** Every 10 ticks: the helmet against darkness, the crown's sparkle + party Vahoeg. */
    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.getGameTime() % 10 != 0) {
            return;
        }
        long nu = sl.getGameTime();
        for (ServerPlayer p : sl.players()) {
            String hoofd = gedragen(p, EquipmentSlot.HEAD);
            if ("baas_lichtzwamhelm".equals(hoofd)) {
                p.removeEffect(MobEffects.DARKNESS);
                p.removeEffect(MobEffects.BLINDNESS);
            } else if ("baas_klodderkroontje".equals(hoofd)) {
                for (int i = 0; i < 5; i++) {
                    double a = (nu / 10.0 + i) * Math.PI * 2 / 5;
                    sl.sendParticles(i % 2 == 0 ? GOUD : ROZE, p.getX() + Math.cos(a) * 0.35, p.getY() + p.getBbHeight() + 0.35, p.getZ() + Math.sin(a) * 0.35,
                            1, 0, 0, 0, 0);
                }
                if (nu % 600 == 0) {
                    for (ServerPlayer v : Partijen.ledenInBuurt(p, 8)) {
                        Statussen.geef(v, Status.VAHOEG, 100, 0.15, p);
                    }
                    Statussen.geef(p, Status.VAHOEG, 100, 0.15, p);
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1.5, p.getZ(), 30, 1.5, 0.6, 1.5, 0.2);
                }
            }
        }
    }
}
