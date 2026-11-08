package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
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
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensRegistry;

/**
 * Koningin Wormelien (field boss, lv 63, De Roze Duinen, by DAY): a giant pink sand worm with a guh snout and long lashes. Ontvadst:
 * she swims grey and hungry through the sand. A victim: at the end she is CHEERED UP, turns pink and falls asleep in the sand
 * (pet Wormelientje, the Wormelien saddle blanket for every mount).
 * <ul>
 *   <li><b>Fase 1</b> - <i>Onderduiken</i>: she dives (gone, can't be hit) and a sand ripple follows someone for 5 s; whoever STANDS
 *       STILL gets a sand fountain (red circle at your feet, 0.8 s, 15 %). Then she bursts up under her target (red circle 3.5,
 *       1 s, 25 % + a toss). <i>Zandgolf</i>: a red lane through someone (1.2 s): 20 % + Plakkerig.</li>
 *   <li><b>Fase 2</b> (60 %) - <i>Duinspuug</i>: she gulps sand (cast bar 3 s) while three PINK spots appear: the shelter behind
 *       the dunes she spits up. Whoever is not in a pink spot is buried: 35 %.</li>
 *   <li><b>Fase 3</b> (30 %) - <i>Helemaal boven!</i>: she comes all the way up. A grey crust of star salt sits high on her back:
 *       she can't be hurt until it is knocked off (jump at it from a dune, or shoot it). Then she is stunned for 6 s and takes
 *       +50 % for 10 s. She keeps diving and spitting.</li>
 * </ul>
 */
public class Wormelien extends Z6Baas {
    private static final EntityDataAccessor<Boolean> ONDER = SynchedEntityData.defineId(Wormelien.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BLIJ = SynchedEntityData.defineId(Wormelien.class, EntityDataSerializers.BOOLEAN);

    private int duikCd = 160, golfCd = 100, spuugCd = 60, onderT = -1, kwetsbaarT;
    @Nullable
    private UUID volg, korst;
    private boolean korstGehad;
    @Nullable
    private Vec3 bovenPlek;
    private final Map<UUID, Vec3> stil = new HashMap<>();
    private final Map<UUID, Integer> stilT = new HashMap<>();
    /** Gametests. */
    public int duiken, fonteinen, begraven;

    public Wormelien(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ONDER, false);
        b.define(BLIJ, false);
    }

    /** Under the sand (client: the model hides, only the ripple shows). */
    public boolean onder() {
        return entityData.get(ONDER);
    }

    public boolean blij() {
        return entityData.get(BLIJ) || geplofd();
    }

    public boolean heeftKorst() {
        return korst != null;
    }

    @Override
    public String baasId() {
        return "wormelien";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"aanval", "duik", "boven", "slok", "spuug", "golf", "slaap"};
    }

    @Override
    protected double hp(int level) {
        return 15000 + 480 * level;
    }

    @Override
    protected double klap(int level) {
        return 22 + 2.4 * level;
    }

    @Override
    public Element standaardElement() {
        return Element.NATUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.6, 0.3};
    }

    @Override
    protected double jaagSnelheid() {
        return 0.8;
    }

    @Override
    protected double bereik() {
        return getBbWidth() / 2 + 2.8;
    }

    @Override
    protected boolean bezig() {
        return onderT >= 0;
    }

    @Override
    protected boolean immuun() {
        return onder() || korst != null;
    }

    @Override
    protected String immuunKey() {
        return korst != null ? "vadsrijk.z6wezens.wm.korst_eerst" : "vadsrijk.z6wezens.wm.onder";
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        return kwetsbaarT > 0 ? 1.5 : 1;
    }

    @Override
    protected int plofDuur() {
        return 20 * 15;
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        kaart("vadsrijk.z6wezens.wm.fase" + fase, fase == 3 ? ROOD : PAARS);
        if (fase == 3) {
            if (onder()) {
                boven(sl, position());
            }
            korstOp(sl);
        }
    }

    // ---- the moves ----------------------------------------------------------------------------------------------------------------

    /** Dives and follows this player under the sand for 5 s (dev/tests too). */
    public void duik(ServerLevel sl, ServerPlayer doel) {
        duiken++;
        entityData.set(ONDER, true);
        onderT = tg(100);
        volg = doel.getUUID();
        stil.clear();
        stilT.clear();
        speel("duik");
        Z6Vfx.zand(sl, position(), 3);
        sl.playSound(null, blockPosition(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 3f, 0.5f);
        zeg("vadsrijk.z6wezens.wm.duikt", ChatFormatting.GOLD);
    }

    /** Comes up right where she is (dev/tests). */
    public void opduiken(ServerLevel sl) {
        if (onder()) {
            boven(sl, position());
        }
    }

    private void boven(ServerLevel sl, Vec3 waar) {
        entityData.set(ONDER, false);
        onderT = -1;
        setPos(waar.x, waar.y, waar.z);
        speel("boven");
        Z6Vfx.zand(sl, waar, 4);
        Z6Vfx.golf(sl, waar, 4, Z6Vfx.ZAND);
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 1.3f);
    }

    /** Fase 2: gulps sand, three pink shelters, then buries everyone outside them (dev/tests too). */
    public void duinspuug(ServerLevel sl) {
        List<Vec3> schuil = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Vec3 v = voetVan(ring(zMidden(), 7 + getRandom().nextDouble() * (zStraal() - 11), getRandom().nextDouble() * Math.PI * 2));
            schuil.add(v);
            Z6Vfx.veilig(sl, v, 3, tg(60));
            Z6Vfx.zand(sl, v, 1.5);   // a dune heaps up: the shelter
        }
        speel("slok");
        Z6Vfx.cast(this, "duinspuug", tg(60));
        zeg("vadsrijk.z6wezens.wm.schuil", ChatFormatting.LIGHT_PURPLE);
        later(tg(60), () -> {
            speel("spuug");
            Z6Vfx.treffer(this, position(), zStraal(), Z6Vfx.ZAND);
            sl.playSound(null, blockPosition(), SoundEvents.SAND_FALL, SoundSource.HOSTILE, 3f, 0.4f);
            for (ServerPlayer p : strijders()) {
                boolean veilig = false;
                for (Vec3 k : schuil) {
                    if (p.position().distanceToSqr(k.x, p.getY(), k.z) <= 3.2 * 3.2) {
                        veilig = true;
                    }
                }
                Z6Vfx.zand(sl, p.position(), 1.4);
                if (!veilig) {
                    begraven++;
                    mech(p, 0.35, "duinspuug");
                    Statussen.geef(p, Status.PLAKKERIG, 60, 0.4, this);
                }
            }
        });
    }

    /** Fase 3: the grey crust on her back (dev/tests too). */
    public void korstOp(ServerLevel sl) {
        if (korst != null || korstGehad) {
            return;
        }
        korstGehad = true;
        if (Z6WezensRegistry.ZOUTKORST.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
            d.koppel(this, 0, maxVads() * 0.06, korstPlek());
            sl.addFreshEntity(d);
            korst = d.getUUID();
        }
        zeg("vadsrijk.z6wezens.wm.korst", ChatFormatting.GRAY);
    }

    private Vec3 korstPlek() {
        return position().add(voorKant().scale(-0.6)).add(0, getBbHeight() + 0.6, 0);
    }

    private Vec3 voorKant() {
        double a = Math.toRadians(getYRot());
        return new Vec3(-Math.sin(a), 0, Math.cos(a));
    }

    @Override
    public void doelKapot(Z3Doel d, @Nullable LivingEntity bron) {
        if (!(level() instanceof ServerLevel sl) || !d.getUUID().equals(korst)) {
            return;
        }
        korst = null;
        d.discard();
        Z6Vfx.zoutKorst(sl, d.position(), 2.5);
        sl.playSound(null, d.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2f, 0.8f);
        Statussen.geef(this, Status.VERDOOFD, 120, 1, this);
        kwetsbaarT = 200;
        zeg("vadsrijk.z6wezens.wm.korst_weg", ChatFormatting.GREEN);
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (l.isEmpty()) {
            return;
        }
        if (fase() >= 2 && spuugCd <= 0) {
            duinspuug(sl);
            spuugCd = fase() >= 3 ? 420 : 520;
            rustT = tg(60) + 30;
            laatsteAanval = "duinspuug";
            return;
        }
        if (duikCd <= 0 && korst == null) {
            duik(sl, l.get(getRandom().nextInt(l.size())));
            duikCd = 380;
            rustT = 40;
            laatsteAanval = "duik";
            return;
        }
        if (golfCd <= 0) {
            ServerPlayer v = l.get(getRandom().nextInt(l.size()));
            Vec3 c = voetVan(v), r = c.subtract(position()).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
            Vec3 a = position().add(r.scale(1.5)), b = a.add(r.scale(16));
            Z6Vfx.lijn(sl, a, b, 3.5, tg(24), ROOD);
            Z6Vfx.windup(this, tg(24));
            Z6Vfx.cast(this, "zandgolf", tg(24));
            later(tg(24), () -> {
                speel("golf");
                for (int i = 0; i <= 16; i += 2) {
                    Z6Vfx.zand(sl, a.lerp(b, i / 16.0), 1.2);
                }
                for (ServerPlayer p : strijders()) {
                    if (inStrook(p.position(), a, b, 3.5) && Math.abs(p.getY() - a.y) < 4) {
                        mech(p, 0.2, "zandgolf");
                        Statussen.geef(p, Status.PLAKKERIG, 60, 0.4, this);
                    }
                }
            });
            golfCd = 160;
            rustT = 40;
            laatsteAanval = "zandgolf";
            return;
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        duikCd--;
        golfCd--;
        spuugCd--;
        if (kwetsbaarT > 0) {
            kwetsbaarT--;
        }
        if (korst != null && sl.getEntity(korst) instanceof Z3Doel d) {
            d.zetPlek(korstPlek());   // the crust rides on her back
        }
        if (onderT >= 0) {
            onderTick(sl, l);
        }
    }

    private void onderTick(ServerLevel sl, List<ServerPlayer> l) {
        ServerPlayer d = volg != null && sl.getEntity(volg) instanceof ServerPlayer p && l.contains(p) ? p : zDoel();
        if (d != null) {   // she swims under the sand toward her target: a ripple
            Vec3 naar = d.position().subtract(position()).multiply(1, 0, 1);
            if (naar.length() > 1.5) {
                Vec3 stap = naar.normalize().scale(0.22);
                Vec3 nieuw = voetVan(position().add(stap));
                setPos(nieuw.x, nieuw.y, nieuw.z);
            }
        }
        if (tickCount % 3 == 0) {
            Z6Vfx.zand(sl, position(), 1.2);
        }
        if (onderT % 10 == 0) {   // standing still = a sand fountain
            for (ServerPlayer p : l) {
                Vec3 was = stil.get(p.getUUID());
                boolean staat = was != null && was.distanceToSqr(p.position()) < 0.09;
                stil.put(p.getUUID(), p.position());
                int t = staat ? stilT.merge(p.getUUID(), 1, Integer::sum) : 0;
                if (!staat) {
                    stilT.put(p.getUUID(), 0);
                }
                if (t >= 2) {
                    stilT.put(p.getUUID(), -2);
                    Vec3 v = voetVan(p);
                    fonteinen++;
                    inslag(v, 1.8, tg(16), 0.15, "zandfontein", null, ParticleTypes.CLOUD);
                }
            }
        }
        if (onderT == tg(20) && d != null) {   // where she comes up
            Vec3 v = voetVan(d);
            Z6Vfx.cirkel(sl, v, 3.5, tg(20), ROOD);
            volg = d.getUUID();
            bovenPlek = v;
        }
        if (--onderT < 0) {
            Vec3 v = bovenPlek != null ? bovenPlek : position();
            bovenPlek = null;
            boven(sl, v);
            for (ServerPlayer p : inCirkel(v, 3.5)) {
                mech(p, 0.25, "opduiken");
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.7, 0));
                p.hurtMarked = true;
            }
            rustT = 40;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (korst != null && level() instanceof ServerLevel sl && sl.getEntity(korst) instanceof Z3Doel d) {
            d.discard();
        }
        super.remove(reason);
    }

    // ---- the end: cheered up, she sleeps in the sand --------------------------------------------------------------------------------

    @Override
    protected void opPlof() {
        entityData.set(ONDER, false);
        entityData.set(BLIJ, true);
        super.opPlof();
        speel("slaap");
        zegAlle("vadsrijk.z6wezens.wm.blij", ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && geplofd() && level() instanceof ServerLevel sl && tickCount % 30 == 0) {
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.4, getZ(), 1, 0.6, 0.2, 0.6, 0);
        }
    }
}
