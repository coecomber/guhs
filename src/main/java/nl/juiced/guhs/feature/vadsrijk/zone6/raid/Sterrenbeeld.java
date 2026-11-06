package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
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
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;

/**
 * Floor 3: De Gedoofde Sterrenbeelden, three grey constellations (star knots and lines) at once. Each has its own Vads.
 * <ul>
 *   <li><b>De Grote Knuffel</b> (the arena boss, tough, the tank's job): heavy bites, Knuffelklem (a tank buster circle on its
 *       target: keep away from it) and Berenbrul (a DONUT: run IN to it, the outside is hit).</li>
 *   <li><b>De Vliegende Bultguh</b> hovers and charges through the hall along a red lane (get out of the lane), then rests 3 s on
 *       the ground.</li>
 *   <li><b>De Kleine Pluis</b> heals the other two (2 % every 4 s) and sings the Sterrenlied (INTERRUPT: 5 % of its Vads in 3 s,
 *       else the others get 15 % back). It keeps its distance.</li>
 *   <li><b>Het steek-aan-venster</b>: a constellation at ~0 Vads is not beaten but "uit" (cheered up, shining, it stops). When the
 *       FIRST goes out, a 10 s window starts: all three out in time = cheered up together. Otherwise they light each other again
 *       at 40 %. Split the group in three (with 2 players: the Pluis low first, then together).</li>
 * </ul>
 */
public class Sterrenbeeld extends Z6RaidBaas {
    public static final int KNUFFEL = 0, BULTGUH = 1, PLUIS = 2;
    public static final int VENSTER = 200;
    private static final EntityDataAccessor<Boolean> UIT = SynchedEntityData.defineId(Sterrenbeeld.class, EntityDataSerializers.BOOLEAN);
    @Nullable
    private UUID meester;
    private final List<UUID> anderen = new ArrayList<>();
    private boolean gemaakt, klaar;
    private int vensterT = -1;
    private int klemCd = 120, brulCd = 300, stormCd = 100, stormT = -1, rustGrond, liedCd = 200, healCd = 80;
    @Nullable
    private Vec3 stormVan, stormNaar;
    /** Gametests. */
    public int aangestoken;

    public Sterrenbeeld(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(UIT, false);
    }

    public int soort() {
        if (getType() == Z6RaidRegistry.STER_BULTGUH.get()) {
            return BULTGUH;
        }
        return getType() == Z6RaidRegistry.STER_PLUIS.get() ? PLUIS : KNUFFEL;
    }

    public boolean uit() {
        return entityData.get(UIT);
    }

    @Override
    public boolean echteBaas() {
        return soort() == KNUFFEL;
    }

    @Override
    public String baasId() {
        return switch (soort()) {
            case BULTGUH -> "ster_bultguh";
            case PLUIS -> "ster_pluis";
            default -> "sterrenbeelden";
        };
    }

    @Override
    public int verdieping() {
        return 3;
    }

    @Override
    protected double basisHp() {
        return switch (soort()) {
            case BULTGUH -> 12000;
            case PLUIS -> 9000;
            default -> 15000;
        };
    }

    @Override
    protected double basisKlap() {
        return soort() == KNUFFEL ? 210 : 150;
    }

    @Override
    protected double[] fases() {
        return new double[0];
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"klem", "brul", "storm", "lied", "uit"};
    }

    @Override
    public boolean isNoGravity() {
        return (soort() == BULTGUH && rustGrond <= 0 && !uit()) || super.isNoGravity();
    }

    @Override
    protected double vadsVloer() {
        return klaar ? 0 : 0.0005;
    }

    @Override
    protected boolean immuun() {
        return uit() && !klaar;
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z6raid.sterrenbeelden.uit_al";
    }

    @Override
    protected boolean slaapt() {
        return uit() && !klaar;
    }

    @Override
    protected boolean bezig() {
        return super.bezig() || stormT >= 0 || (soort() == PLUIS && afstandHouden());
    }

    @Override
    public net.minecraft.resources.Identifier muziek() {
        return Z6RaidMuziek.id("sterrenbeelden", vensterT >= 0 ? 2 : 1);
    }

    // ---- the three together ---------------------------------------------------------------------------------------------------

    /** The main one makes the other two (once). */
    private void maakAnderen(ServerLevel sl) {
        gemaakt = true;
        for (EntityType<Sterrenbeeld> t : List.of(Z6RaidRegistry.STER_BULTGUH.get(), Z6RaidRegistry.STER_PLUIS.get())) {
            Sterrenbeeld s = t.create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (s == null) {
                continue;
            }
            double a = getRandom().nextDouble() * Math.PI * 2;
            Vec3 v = zMidden().add(Math.cos(a) * 8, t == Z6RaidRegistry.STER_BULTGUH.get() ? 2 : 0, Math.sin(a) * 8);
            s.meester = getUUID();
            s.testN = testN;
            s.testFilter = testFilter;
            s.stelIn(mobLevel(), element(), false, java.util.Set.of());
            s.snapTo(v.x, v.y, v.z, 0, 0);
            s.zetThuis(zMidden());
            s.schaal(0);
            sl.addFreshEntity(s);
            anderen.add(s.getUUID());
        }
    }

    /** All three (the main one first), alive or not. */
    public List<Sterrenbeeld> alle() {
        List<Sterrenbeeld> l = new ArrayList<>();
        Sterrenbeeld m = hoofd();
        if (m == null || !(level() instanceof ServerLevel sl)) {
            return l;
        }
        l.add(m);
        for (UUID u : m.anderen) {
            if (sl.getEntity(u) instanceof Sterrenbeeld s && !s.isRemoved()) {
                l.add(s);
            }
        }
        return l;
    }

    @Nullable
    public Sterrenbeeld hoofd() {
        if (soort() == KNUFFEL) {
            return this;
        }
        return meester != null && level() instanceof ServerLevel sl && sl.getEntity(meester) instanceof Sterrenbeeld m && !m.isRemoved() ? m : null;
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        super.krijgSchade(n, bron);
        if (!klaar && !uit() && vads() <= maxVads() * vadsVloer() + 0.5 && level() instanceof ServerLevel sl) {
            gaUit(sl);
        }
    }

    /** Cheered up for now (shines, stops); the main one runs the window. */
    public void gaUit(ServerLevel sl) {
        entityData.set(UIT, true);
        stormT = -1;
        setNoGravity(false);
        anim("uit");
        Vfx.deeltjes(sl, Deeltje.STER, position().add(0, getBbHeight() / 2, 0), 40, STERGOUD, getBbWidth(), 0.1);
        Vfx.zuil(sl, position(), 5, STERWIT, VENSTER);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2f, 1.6f);
        Sterrenbeeld h = hoofd();
        if (h != null) {
            h.eenUit(sl, this);
        }
    }

    private void eenUit(ServerLevel sl, Sterrenbeeld s) {
        List<Sterrenbeeld> l = alle();
        long uit = l.stream().filter(Sterrenbeeld::uit).count();
        if (uit >= l.size()) {
            samenOpgevrolijkt(sl, l);
            return;
        }
        if (vensterT < 0) {
            vensterT = 0;
            kaartAlle("vadsrijk.z6raid.sterrenbeelden.venster", GEEL);
        }
        zeg("vadsrijk.z6raid.sterrenbeelden.een_uit", ChatFormatting.GOLD, net.minecraft.network.chat.Component.translatable("vadsrijk.z6raid." + s.baasId()),
                uit, l.size());
    }

    private void samenOpgevrolijkt(ServerLevel sl, List<Sterrenbeeld> l) {
        vensterT = -1;
        for (Sterrenbeeld s : l) {
            if (s != this) {
                s.klaar = true;
                s.opPlof();
                s.discard();
            }
        }
        anderen.clear();
        klaar = true;
        kaartAlle("vadsrijk.z6raid.sterrenbeelden.samen", GEEL);
        krijgSchade(vads() + 1, null);
    }

    private void steekAan(ServerLevel sl) {
        vensterT = -1;
        aangestoken++;
        for (Sterrenbeeld s : alle()) {
            if (s.uit()) {
                s.entityData.set(UIT, false);
                s.zetVads(s.maxVads() * 0.4);
                Vfx.deeltjes(sl, Deeltje.ROOK, s.position().add(0, 1, 0), 30, 0x707080, 1, 0.05);
            }
        }
        kaartAlle("vadsrijk.z6raid.sterrenbeelden.aangestoken", ROOD);
        zegGroep("vadsrijk.z6raid.sterrenbeelden.zeg.aan");
    }

    /** Sets its Vads (keeps the max). */
    public void zetVads(double v) {
        heal(Math.max(0, v) - vads());
    }

    @Override
    public List<Object[]> hudExtra() {
        List<Object[]> l = new ArrayList<>();
        for (Sterrenbeeld s : alle()) {
            l.add(new Object[] {"vadsrijk.z6raid." + s.baasId(), (float) (s.vads() / Math.max(1, s.maxVads())), s.uit()});
        }
        if (vensterT >= 0) {
            l.add(new Object[] {"vadsrijk.z6raid.sterrenbeelden.venster_hud", 1f - vensterT / (float) VENSTER, false});
        }
        return l;
    }

    // ---- the moves ------------------------------------------------------------------------------------------------------------

    private boolean afstandHouden() {
        ServerPlayer d = zDoel();
        return d != null && d.distanceTo(this) < 6;
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        rustT = 20;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        if (soort() == KNUFFEL && !gemaakt && !l.isEmpty() && !inIntro()) {
            maakAnderen(sl);
        }
        if (soort() != KNUFFEL && hoofd() == null && tickCount > 40) {
            discard();
            return;
        }
        if (gietTick() || uit() || l.isEmpty() || inIntro()) {
            return;
        }
        switch (soort()) {
            case KNUFFEL -> knuffel(sl, l);
            case BULTGUH -> bultguh(sl, l);
            default -> pluis(sl, l);
        }
    }

    private void knuffel(ServerLevel sl, List<ServerPlayer> l) {
        if (--klemCd <= 0) {
            klemCd = 20 * 9;
            ServerPlayer d = zDoel() != null ? zDoel() : l.get(0);
            Vec3 v = voetVan(d);
            anim("klem");
            zeg("vadsrijk.z6raid.sterrenbeelden.klem", ChatFormatting.YELLOW, d.getDisplayName());
            inslag(v, 2.8, 40, 0.45, "knuffelklem", null, net.minecraft.core.particles.ParticleTypes.END_ROD);
            laatsteAanval = "knuffelklem";
        } else if (--brulCd <= 0) {
            brulCd = 20 * 20;
            int w = tg(45);
            Vec3 c = position();
            Telegraaf.donut(sl, c, 5, 22, w, ROOD);
            BaasVfx.windup(this, w);
            anim("brul");
            kaartAlle("vadsrijk.z6raid.sterrenbeelden.brul", ROOD);
            later(w, () -> {
                Vfx.golf(sl, c, 22, STERWIT, 14);
                for (ServerPlayer p : strijders()) {
                    double d = p.position().multiply(1, 0, 1).distanceTo(c.multiply(1, 0, 1));
                    if (d > 5 && d <= 22) {
                        mech(p, 0.35, "berenbrul");
                    }
                }
            });
            laatsteAanval = "berenbrul";
        }
    }

    private void bultguh(ServerLevel sl, List<ServerPlayer> l) {
        if (rustGrond > 0) {
            rustGrond--;
            return;
        }
        if (stormT >= 0) {
            stormT++;
            int w = tg(30);
            if (stormT == w) {
                anim("storm");
            }
            if (stormT >= w && stormVan != null && stormNaar != null) {
                double f = Math.min(1, (stormT - w) / 14.0);
                Vec3 v = stormVan.lerp(stormNaar, f);
                setPos(v.x, v.y + 1.5, v.z);
                for (ServerPlayer p : strijders()) {
                    if (p.position().multiply(1, 0, 1).distanceTo(v.multiply(1, 0, 1)) < 2.2 && !Statussen.heeft(p, Status.PLAKKERIG)) {
                        mech(p, 0.3, "sterrenstorm");
                        duw(p, v, 1.0);
                        Statussen.geef(p, Status.PLAKKERIG, 30, 1, this);
                    }
                }
                if (stormT % 3 == 0) {
                    Vfx.deeltjes(sl, Deeltje.STER, v.add(0, 1.5, 0), 6, STERWIT, 0.6, 0.05);
                }
                if (f >= 1) {
                    stormT = -1;
                    rustGrond = 60;
                    setNoGravity(false);
                }
            }
            return;
        }
        // hover 2 blocks above, a few blocks from its target
        ServerPlayer d = zDoel() != null ? zDoel() : l.get(0);
        Vec3 doel = d.position().add(0, 2.2, 0);
        Vec3 r0 = doel.subtract(position());
        if (r0.multiply(1, 0, 1).length() < 3.5) {
            r0 = new Vec3(0, r0.y, 0);
        }
        setDeltaMovement(r0.lengthSqr() > 0.04 ? r0.normalize().scale(Math.min(0.28, r0.length() * 0.2)) : Vec3.ZERO);
        getLookControl().setLookAt(d, 30, 30);
        if (--stormCd <= 0) {
            stormCd = 20 * 8;
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            Vec3 van = position().multiply(1, 0, 1).add(0, p.getY(), 0);
            Vec3 r = p.position().subtract(van).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
            Vec3 naar = van.add(r.scale(26));
            Vec3 m = zMidden();
            Vec3 tot = naar.subtract(m).multiply(1, 0, 1);
            if (tot.length() > 19) {
                naar = m.add(tot.normalize().scale(19)).add(0, p.getY() - m.y, 0);
            }
            stormVan = van;
            stormNaar = naar;
            stormT = 0;
            Telegraaf.lijn(sl, van, naar, 3.6, tg(30), ROOD);
            BaasVfx.windup(this, tg(30));
            getNavigation().stop();
            laatsteAanval = "sterrenstorm";
        }
    }

    private void pluis(ServerLevel sl, List<ServerPlayer> l) {
        ServerPlayer d = zDoel();
        if (d != null && d.distanceTo(this) < 6 && tickCount % 10 == 0) {   // keep away
            Vec3 weg = position().subtract(d.position()).multiply(1, 0, 1);
            Vec3 m = zMidden();
            Vec3 naar = position().add(weg.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : weg.normalize().scale(6));
            if (naar.subtract(m).multiply(1, 0, 1).length() > 18) {
                naar = m.add(naar.subtract(m).multiply(1, 0, 1).normalize().scale(-6));
            }
            getNavigation().moveTo(naar.x, naar.y, naar.z, 1.3);
        }
        if (--healCd <= 0) {
            healCd = 80;
            for (Sterrenbeeld s : alle()) {
                if (s != this && !s.uit()) {
                    s.heal(s.maxVads() * 0.02);
                    Vfx.deeltjes(sl, Deeltje.HART, s.position().add(0, s.getBbHeight(), 0), 3, 0xB0E0FF, 0.4, 0.02);
                }
            }
        }
        if (--liedCd <= 0) {
            liedCd = 20 * 12;
            anim("lied");
            giet("sterrenlied", tg(60), 0.05, () -> {
                for (Sterrenbeeld s : alle()) {
                    if (s != this && !s.uit()) {
                        s.heal(s.maxVads() * 0.15);
                        Vfx.zuil(sl, s.position(), 4, 0xB0E0FF, 20);
                    }
                }
                zeg("vadsrijk.z6raid.sterrenbeelden.lied", ChatFormatting.RED);
            }, null);
            laatsteAanval = "sterrenlied";
        }
    }

    @Override
    protected void enragePuls(ServerLevel sl, List<ServerPlayer> l) {
        if (soort() == KNUFFEL) {
            super.enragePuls(sl, l);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl && soort() == KNUFFEL) {
            for (UUID u : anderen) {
                if (sl.getEntity(u) instanceof Sterrenbeeld s) {
                    s.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return soort() == KNUFFEL && super.shouldBeSaved();
    }

    @Override
    public void tick() {
        super.tick();
        // the window runs here: vecht() stops while the main one is "uit" itself
        if (!level().isClientSide() && level() instanceof ServerLevel sl && soort() == KNUFFEL && !klaar && vensterT >= 0 && ++vensterT >= VENSTER) {
            steekAan(sl);
        }
    }

    /** Gametests: the window's tick (-1 = closed). */
    public int venster() {
        return vensterT;
    }
}
