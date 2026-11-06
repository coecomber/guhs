package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensRegistry;

/**
 * Donderbuik, de Grauwe Onweerswolk (world boss on the tail fluke of Moeder Wolkwal; lv 60, Water): a fat grey thundercloud with a
 * grumpy face that the Grauwe Mistbank blew against the whale. It is ONTVADST (a victim), not evil: at the end it is cheered up,
 * turns pink, giggles, and drifts off raining pink drops (pet: Donderbuikje).
 * <ul>
 *   <li>Bliksem: a red circle under every fighter (1.4 s): 30 % + a short stun. Fase 3: twice in a row.</li>
 *   <li>Regenlaan: a wide red lane through someone (1.5 s): 25 % + Plakkerig.</li>
 *   <li>Zuigwind: three PINK cloud cushions appear; after 2.5 s it sucks in: whoever is not on a pink cushion is pulled to its belly
 *       (35 %). Fase 3 more often.</li>
 *   <li>60 %: three Grauwe Wolkduivels come out of its belly, and Donderslag: a yellow circle on someone: stand in it together
 *       (shared 70 %); solo you take it alone.</li>
 * </ul>
 * Contained at the cloud edge: a player who falls off the fluke is caught by a cloud ({@link nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5Vangnet}).
 */
public class Donderbuik extends Z5Baas {
    private static final EntityDataAccessor<Boolean> ROZE_WEER = SynchedEntityData.defineId(Donderbuik.class, EntityDataSerializers.BOOLEAN);
    private int bliksemCd = 100, laanCd = 160, zuigCd = 300, slagCd = 240;
    /** Gametests: pulls done and how many were pulled the last time. */
    public int zuigingen, laatstGezogen;

    public Donderbuik(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ROZE_WEER, false);
    }

    public boolean rozeWeer() {
        return entityData.get(ROZE_WEER);
    }

    @Override
    public String baasId() {
        return "donderbuik";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"aanval", "zuig", "bliksem", "blij"};
    }

    @Override
    protected double hp(int level) {
        return 16000 + 520 * level;
    }

    @Override
    protected double klap(int level) {
        return 24 + 2.6 * level;
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.6, 0.25};
    }

    @Override
    protected double jaagSnelheid() {
        return 0.7;
    }

    @Override
    protected double bereik() {
        return getBbWidth() / 2 + 2.6;
    }

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z5wezens.db.fase" + fase, fase == 3 ? ROOD : PAARS);
        if (fase == 2 && level() instanceof ServerLevel sl) {
            for (int i = 0; i < adds(3); i++) {
                if (Z5WezensRegistry.GRAUWE_WOLKDUIVEL.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof VadsrijkMob m) {
                    m.snapTo(getX() + Math.cos(i * 2.1) * 2, getY() + 3, getZ() + Math.sin(i * 2.1) * 2, 0, 0);
                    m.stelIn(58, null, false, Set.of());
                    sl.addFreshEntity(m);
                    List<ServerPlayer> l = strijders();
                    if (!l.isEmpty()) {
                        m.setTarget(l.get(i % l.size()));
                    }
                }
            }
        }
    }

    /** The pull: pink cushions now, the suck after 50 ticks (dev/tests too). */
    public void zuigwind(ServerLevel sl) {
        List<Vec3> kussens = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Vec3 v = voetVan(ring(zMidden(), 6 + getRandom().nextDouble() * (zStraal() - 9), getRandom().nextDouble() * Math.PI * 2));
            kussens.add(v);
            Telegraaf.cirkel(sl, v, 3, tg(50), ROZE);
        }
        zeg("vadsrijk.z5wezens.db.kussen", ChatFormatting.LIGHT_PURPLE);
        speel("zuig");
        later(tg(50), () -> {
            zuigingen++;
            laatstGezogen = 0;
            sl.playSound(null, blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.HOSTILE, 3f, 0.5f);
            for (ServerPlayer p : strijders()) {
                boolean veilig = false;
                for (Vec3 k : kussens) {
                    if (p.position().distanceToSqr(k.x, p.getY(), k.z) <= 3.2 * 3.2) {
                        veilig = true;
                    }
                }
                if (veilig) {
                    sl.sendParticles(new DustParticleOptions(ROZE, 1.4f), p.getX(), p.getY() + 0.3, p.getZ(), 10, 0.5, 0.2, 0.5, 0);
                    continue;
                }
                laatstGezogen++;
                mech(p, 0.35, "zuigwind");
                Vec3 naar = position().subtract(p.position()).multiply(1, 0, 1);
                if (naar.lengthSqr() > 0.01) {
                    p.setDeltaMovement(naar.normalize().scale(Math.min(2.2, naar.length() * 0.18)).add(0, 0.35, 0));
                    p.hurtMarked = true;
                }
            }
        });
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (zuigCd <= 0) {
            zuigwind(sl);
            zuigCd = fase() >= 3 ? 320 : 440;
            rustT = 60;
            laatsteAanval = "zuig";
            return;
        }
        if (bliksemCd <= 0) {
            speel("bliksem");
            int keer = fase() >= 3 ? 2 : 1;
            for (int k = 0; k < keer; k++) {
                int wacht = k * 30;
                later(wacht, () -> {
                    for (ServerPlayer p : strijders()) {
                        Vec3 v = voetVan(p);
                        inslag(v, 2.5, 28, 0.3, "bliksem", Status.VERDOOFD, ParticleTypes.ELECTRIC_SPARK);
                    }
                });
            }
            sl.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.5f, 1.2f);
            bliksemCd = 140;
            rustT = 30;
            laatsteAanval = "bliksem";
            return;
        }
        if (fase() >= 2 && slagCd <= 0) {
            ServerPlayer v = l.get(getRandom().nextInt(l.size()));
            stapel(voetVan(v), 3.5, 60, 0.7, 0.35, 2, "donderslag", ok -> zeg(ok ? "vadsrijk.z5wezens.db.slag_ok" : "vadsrijk.z5wezens.db.slag_mis",
                    ok ? ChatFormatting.GREEN : ChatFormatting.RED));
            slagCd = 360;
            rustT = 40;
            laatsteAanval = "slag";
            return;
        }
        if (laanCd <= 0) {
            ServerPlayer v = l.get(getRandom().nextInt(l.size()));
            Vec3 c = voetVan(v), r = c.subtract(position()).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
            Vec3 a = c.subtract(r.scale(14)), b = c.add(r.scale(14));
            Telegraaf.lijn(sl, a, b, 4, tg(30), ROOD);
            later(tg(30), () -> {
                for (int i = 0; i <= 14; i++) {
                    Vec3 q = a.lerp(b, i / 14.0);
                    sl.sendParticles(ParticleTypes.FALLING_WATER, q.x, q.y + 3, q.z, 12, 1.4, 1.5, 1.4, 0);
                }
                for (ServerPlayer p : strijders()) {
                    if (inStrook(p.position(), a, b, 4) && Math.abs(p.getY() - c.y) < 4) {
                        mech(p, 0.25, "regenlaan");
                        Statussen.geef(p, Status.PLAKKERIG, 60, 0.4, this);
                    }
                }
            });
            laanCd = 180;
            rustT = 35;
            laatsteAanval = "laan";
            return;
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        bliksemCd--;
        laanCd--;
        zuigCd--;
        slagCd--;
        if (tickCount % 4 == 0) {
            sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 0.5, getZ(), 4, getBbWidth() / 3, 0.2, getBbWidth() / 3, 0);
        }
    }

    // ---- the end: cheered up --------------------------------------------------------------------------------------------------------

    @Override
    protected void opPlof() {
        if (level() instanceof ServerLevel sl) {
            entityData.set(ROZE_WEER, true);
            speel("blij");
            sl.sendParticles(new DustParticleOptions(ROZE, 2.4f), getX(), getY() + 2, getZ(), 140, 2, 1.6, 2, 0.02);
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 4, getZ(), 10, 1.2, 0.5, 1.2, 0);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2f, 1.2f);
        }
    }

    @Override
    protected int plofDuur() {
        return 20 * 15;
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd()) {
            praat("db.blij");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && geplofd() && level() instanceof ServerLevel sl) {
            setNoGravity(true);
            setDeltaMovement(0, 0.03, 0);   // it drifts away, raining pink
            if (tickCount % 3 == 0) {
                sl.sendParticles(new DustParticleOptions(ROZE, 1.2f), getX(), getY() + 0.3, getZ(), 6, getBbWidth() / 3, 0.1, getBbWidth() / 3, 0);
            }
        }
    }
}
