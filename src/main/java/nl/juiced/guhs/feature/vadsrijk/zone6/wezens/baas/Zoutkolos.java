package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensRegistry;

/**
 * De Zoutkolos (field boss, lv 66, De Zoutvlakte, only at NIGHT): a giant of caked star salt that the Mika's made to guard the
 * Zoutbaken. A construct: beaten, it falls apart in glittering salt (no cheer-up).
 * <ul>
 *   <li><b>Fase 1</b> - <i>Zoutgolven</i>: crack lines run out from his feet through the salt (4 red strips, 1.5 s): 25 %.
 *       <i>Zoutvuist</i>: his normal punch.</li>
 *   <li><b>Fase 2</b> (60 %) - <i>Zingende kristallen</i>: he throws 3 pink salt crystals (red circles where they land, 15 %). They
 *       SING: every 3 s a purple ring round each (1 s, 20 % + dizzy) - listen for the high note and step out, or break them.</li>
 *   <li><b>Fase 3</b> (30 %) - <i>Gloeiend hart</i>: his core glows. Whoever LOOKS straight at his heart while hitting him does
 *       double damage (x2.5 with the Sterrenlens in hand). Salt rain: red circles on everyone.</li>
 * </ul>
 */
public class Zoutkolos extends Z6Baas {
    private static final EntityDataAccessor<Boolean> GLOEIT = SynchedEntityData.defineId(Zoutkolos.class, EntityDataSerializers.BOOLEAN);
    private int golfCd = 60, kristalCd = 40, regenCd = 160, zingT = 60;
    private final List<UUID> kristallen = new ArrayList<>();
    /** Gametests. */
    public int golven, gezongen, hartTreffers;

    public Zoutkolos(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GLOEIT, false);
    }

    public boolean gloeit() {
        return entityData.get(GLOEIT);
    }

    @Override
    protected boolean construct() {
        return true;
    }

    @Override
    public String baasId() {
        return "zoutkolos";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"aanval", "stamp", "gooi", "hart"};
    }

    @Override
    protected double hp(int level) {
        return 17000 + 520 * level;
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
        return new double[] {0.6, 0.3};
    }

    @Override
    protected double jaagSnelheid() {
        return 0.7;
    }

    @Override
    protected double bereik() {
        return getBbWidth() / 2 + 2.6;
    }

    public int kristallen() {
        return kristallen.size();
    }

    /** Does this hit land on the glowing heart (fase 3, the attacker looks straight at him)? Returns the factor. */
    public double hartFactor(@Nullable LivingEntity bron) {
        if (!gloeit() || !(bron instanceof Player p)) {
            return 1;
        }
        Vec3 naar = position().add(0, getBbHeight() * 0.55, 0).subtract(p.getEyePosition());
        if (naar.lengthSqr() < 0.01 || naar.length() > 16 || naar.normalize().dot(p.getLookAngle()) < 0.9) {
            return 1;
        }
        hartTreffers++;
        return heeftLens(p) ? 2.5 : 2.0;
    }

    /** The Sterrenlens of the story (u-z6verhaal): any held item whose id contains "sterrenlens". */
    public static boolean heeftLens(Player p) {
        for (ItemStack s : new ItemStack[] {p.getMainHandItem(), p.getOffhandItem()}) {
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("sterrenlens")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        double f = hartFactor(bron);
        if (f > 1 && bron instanceof ServerPlayer p && level() instanceof ServerLevel sl && sl.getGameTime() % 10 == 0) {
            Getallen.tekst(sl, getX(), getY() + getBbHeight() * 0.6, getZ(), net.minecraft.network.chat.Component.translatable("vadsrijk.z6wezens.zk.hart"), 0.9f);
        }
        return f;
    }

    @Override
    protected void opFase(int fase) {
        kaart("vadsrijk.z6wezens.zk.fase" + fase, fase == 3 ? ROOD : PAARS);
        if (fase == 3) {
            entityData.set(GLOEIT, true);
            speel("hart");
            zeg("vadsrijk.z6wezens.zk.gloeit", ChatFormatting.GOLD);
        }
    }

    /** Fase 1: crack lines from his feet (dev/tests too). */
    public void zoutgolven(ServerLevel sl) {
        golven++;
        double start = getRandom().nextDouble() * Math.PI * 2;
        List<Vec3[]> lijnen = new ArrayList<>();
        int n = fase() >= 3 ? 6 : 4;
        for (int i = 0; i < n; i++) {
            double a = start + i * Math.PI * 2 / n;
            Vec3 van = voetVan(position()), naar = van.add(Math.cos(a) * 16, 0, Math.sin(a) * 16);
            lijnen.add(new Vec3[] {van, naar});
            Z6Vfx.lijn(sl, van, naar, 3, tg(30), ROOD);
        }
        speel("stamp");
        Z6Vfx.windup(this, tg(30));
        Z6Vfx.cast(this, "zoutgolf", tg(30));
        later(tg(30), () -> {
            Z6Vfx.treffer(this, position(), 4, Z6Vfx.ZOUT);
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.6f);
            for (Vec3[] l : lijnen) {
                for (int i = 0; i <= 16; i += 2) {
                    Z6Vfx.zoutKorst(sl, l[0].lerp(l[1], i / 16.0).add(0, 0.3, 0), 0.8);
                }
                for (ServerPlayer p : strijders()) {
                    if (inStrook(p.position(), l[0], l[1], 3) && Math.abs(p.getY() - l[0].y) < 4) {
                        mech(p, 0.25, "zoutgolf");
                    }
                }
            }
        });
    }

    /** Fase 2: three singing crystals (dev/tests too). */
    public void kristallenGooien(ServerLevel sl) {
        speel("gooi");
        for (int i = 0; i < 3 && kristallen.size() < 6; i++) {
            Vec3 v = voetVan(ring(zMidden(), 5 + getRandom().nextDouble() * (zStraal() - 8), getRandom().nextDouble() * Math.PI * 2));
            inslag(v, 2, tg(20), 0.15, "kristalworp", null, ParticleTypes.END_ROD);
            later(tg(20), () -> {
                if (Z6WezensRegistry.ZINGKRISTAL.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
                    d.koppel(this, kristallen.size(), maxVads() * 0.025, v);
                    sl.addFreshEntity(d);
                    kristallen.add(d.getUUID());
                }
            });
        }
        zeg("vadsrijk.z6wezens.zk.kristallen", ChatFormatting.LIGHT_PURPLE);
    }

    /** The crystals sing: a purple ring round each (dev/tests too). */
    public void zing(ServerLevel sl) {
        List<Vec3> plekken = new ArrayList<>();
        for (UUID u : kristallen) {
            if (sl.getEntity(u) instanceof Z3Doel d) {
                plekken.add(d.position());
                Z6Vfx.donut(sl, d.position(), 1.2, 4, tg(20), PAARS);
                d.speel("zing");
            }
        }
        if (plekken.isEmpty()) {
            return;
        }
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3f, 1.8f);
        later(tg(20), () -> {
            gezongen++;
            for (Vec3 v : plekken) {
                Z6Vfx.golf(sl, v, 4, 0xF4B8D0);
                for (ServerPlayer p : strijders()) {
                    double d = p.position().distanceTo(new Vec3(v.x, p.getY(), v.z));
                    if (d > 1.2 && d <= 4 && Math.abs(p.getY() - v.y) < 3) {
                        mech(p, 0.2, "zingend_kristal");
                        Statussen.geef(p, Status.DUIZELIG, 40, 1, this);
                    }
                }
            }
        });
    }

    @Override
    public void doelKapot(Z3Doel d, @Nullable LivingEntity bron) {
        if (level() instanceof ServerLevel sl && kristallen.remove(d.getUUID())) {
            Z6Vfx.glinster(sl, d.position().add(0, 0.6, 0), 1.2);
            sl.playSound(null, d.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.5f, 1.4f);
            d.discard();
        }
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (l.isEmpty()) {
            return;
        }
        if (fase() >= 2 && kristalCd <= 0) {
            kristallenGooien(sl);
            kristalCd = 500;
            rustT = 40;
            laatsteAanval = "kristallen";
            return;
        }
        if (fase() >= 3 && regenCd <= 0) {
            for (ServerPlayer p : l) {
                inslag(voetVan(p), 2.5, tg(26), 0.18, "zoutregen", null, ParticleTypes.END_ROD);
            }
            regenCd = 240;
            rustT = 30;
            laatsteAanval = "zoutregen";
            return;
        }
        if (golfCd <= 0) {
            zoutgolven(sl);
            golfCd = fase() >= 3 ? 150 : 200;
            rustT = tg(30) + 20;
            laatsteAanval = "zoutgolf";
            return;
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        golfCd--;
        kristalCd--;
        regenCd--;
        kristallen.removeIf(u -> !(sl.getEntity(u) instanceof Z3Doel d) || !d.isAlive());
        if (!kristallen.isEmpty() && --zingT <= 0) {
            zingT = 60;
            zing(sl);
        }
        if (gloeit() && tickCount % 6 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.55, getZ(), 2, 0.4, 0.4, 0.4, 0.01);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            for (UUID u : kristallen) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    d.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected void opPlof() {
        entityData.set(GLOEIT, false);
        super.opPlof();
        zegAlle("vadsrijk.z6wezens.zk.weg", ChatFormatting.GRAY);
    }
}
