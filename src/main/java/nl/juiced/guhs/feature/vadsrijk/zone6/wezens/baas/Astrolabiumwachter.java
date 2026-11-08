package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;

/**
 * De Astrolabiumwachter (dungeon boss 1 of De Verzonken Sterrenwacht, lv 66): a construct of turning brass rings round a glowing
 * star. A construct: beaten it falls apart into brass and star dust.
 * <ul>
 *   <li><b>Ringzwaai</b>: the rings sweep through the room. Every ring is a red BAND round him with two pink GAPS in it: stand in a
 *       gap (or outside every band) when it sweeps (1.4 s): 30 %. He starts with 1 ring; every 25 % one more (max 4).</li>
 *   <li><b>Uitgelijnd</b>: your hits only go through properly while the rings stand in one line: every 11 s a gold light and
 *       "De ringen staan in één lijn: NU!" (5 s, full damage). Otherwise the brass deflects: 25 %.</li>
 *   <li><b>Messingstraal</b>: a red strip at someone (1 s): 20 %.</li>
 * </ul>
 */
public class Astrolabiumwachter extends Z6Baas {
    private static final EntityDataAccessor<Integer> RINGEN = SynchedEntityData.defineId(Astrolabiumwachter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> UITGELIJND = SynchedEntityData.defineId(Astrolabiumwachter.class, EntityDataSerializers.BOOLEAN);
    public static final int LIJN_ELKE = 120, LIJN_DUUR = 100;
    private int zwaaiCd = 80, straalCd = 140, lijnT = LIJN_ELKE;
    private long kaatsBericht;
    /** Gametests. */
    public int zwaaien, geraaktDoorRing;

    public Astrolabiumwachter(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(RINGEN, 1);
        b.define(UITGELIJND, false);
    }

    public int ringen() {
        return entityData.get(RINGEN);
    }

    public boolean uitgelijnd() {
        return entityData.get(UITGELIJND);
    }

    @Override
    protected boolean construct() {
        return true;
    }

    @Override
    public String baasId() {
        return "astrolabiumwachter";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"aanval", "zwaai", "lijn", "straal"};
    }

    @Override
    protected double hp(int level) {
        return 11000 + 380 * level;
    }

    @Override
    protected double klap(int level) {
        return 20 + 2.3 * level;
    }

    @Override
    public Element standaardElement() {
        return Element.ROZE;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.75, 0.5, 0.25};
    }

    @Override
    protected double jaagSnelheid() {
        return 0.55;
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        if (uitgelijnd()) {
            return 1;
        }
        if (bron instanceof ServerPlayer p && level().getGameTime() > kaatsBericht) {
            kaatsBericht = level().getGameTime() + 40;
            p.sendOverlayMessage(Component.translatable("vadsrijk.z6wezens.aw.kaatst").withStyle(ChatFormatting.GOLD));
        }
        return 0.25;
    }

    @Override
    protected void opFase(int fase) {
        entityData.set(RINGEN, Math.min(4, fase));
        kaart("vadsrijk.z6wezens.aw.fase", PAARS);
        zeg("vadsrijk.z6wezens.aw.ring_erbij", ChatFormatting.GOLD, Math.min(4, fase));
    }

    /** Sets the rings in one line now (dev/tests too). */
    public void lijnUit(ServerLevel sl) {
        entityData.set(UITGELIJND, true);
        lijnT = LIJN_DUUR;
        speel("lijn");
        Z6Vfx.zuil(sl, position(), getBbHeight() + 6, Z6Vfx.SAFFRAAN, LIJN_DUUR);
        zeg("vadsrijk.z6wezens.aw.nu", ChatFormatting.YELLOW);
        sl.playSound(null, blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2f, 1.4f);
    }

    /** One sweep of all rings (dev/tests too): red bands with two pink gaps each. */
    public void ringzwaai(ServerLevel sl) {
        zwaaien++;
        int n = ringen();
        Vec3 c = voetVan(position());
        double stap = Math.max(2.2, (zStraal() - 1.5) / 4);
        List<double[]> banden = new ArrayList<>();
        List<Vec3> gaten = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            double binnen = 1.5 + k * stap, buiten = binnen + stap;
            banden.add(new double[] {binnen, buiten});
            Z6Vfx.donut(sl, c, binnen, buiten, tg(28), ROOD);
            double a0 = getRandom().nextDouble() * Math.PI * 2;
            for (int g = 0; g < 2; g++) {
                double a = a0 + g * Math.PI;
                Vec3 v = voetVan(c.add(Math.cos(a) * (binnen + buiten) / 2, 0, Math.sin(a) * (binnen + buiten) / 2));
                gaten.add(v);
                Z6Vfx.veilig(sl, v, 1.4, tg(28));
            }
        }
        speel("zwaai");
        Z6Vfx.cast(this, "ringzwaai", tg(28));
        sl.playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 2f, 0.6f);
        later(tg(28), () -> {
            sl.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 2f, 0.8f);
            for (double[] b : banden) {
                Z6Vfx.golf(sl, c, b[1], Z6Vfx.SAFFRAAN);
            }
            for (ServerPlayer p : strijders()) {
                double d = Math.sqrt(p.distanceToSqr(c.x, p.getY(), c.z));
                boolean inBand = banden.stream().anyMatch(b -> d >= b[0] && d <= b[1]);
                boolean inGat = gaten.stream().anyMatch(g -> p.position().distanceToSqr(g.x, p.getY(), g.z) <= 1.5 * 1.5);
                if (inBand && !inGat && Math.abs(p.getY() - c.y) < 3) {
                    geraaktDoorRing++;
                    mech(p, 0.3, "ringzwaai");
                }
            }
        });
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (l.isEmpty()) {
            return;
        }
        if (zwaaiCd <= 0) {
            ringzwaai(sl);
            zwaaiCd = 180 - 20 * ringen();
            rustT = tg(28) + 20;
            laatsteAanval = "ringzwaai";
            return;
        }
        if (straalCd <= 0) {
            ServerPlayer d = l.get(getRandom().nextInt(l.size()));
            Vec3 a = voetVan(position()), r = voetVan(d).subtract(a).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
            Vec3 b = a.add(r.scale(zStraal() + 2));
            Z6Vfx.lijn(sl, a, b, 2.4, tg(20), ROOD);
            Z6Vfx.windup(this, tg(20));
            later(tg(20), () -> {
                speel("straal");
                Z6Vfx.straal(sl, a.add(0, 1.4, 0), b.add(0, 1.4, 0), 0.8, Z6Vfx.SAFFRAAN, 8);
                for (ServerPlayer p : strijders()) {
                    if (inStrook(p.position(), a, b, 2.4) && Math.abs(p.getY() - a.y) < 3) {
                        mech(p, 0.2, "messingstraal");
                    }
                }
            });
            straalCd = 160;
            rustT = 30;
            laatsteAanval = "messingstraal";
            return;
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        zwaaiCd--;
        straalCd--;
        if (--lijnT <= 0) {
            if (uitgelijnd()) {
                entityData.set(UITGELIJND, false);
                lijnT = LIJN_ELKE;
            } else {
                lijnUit(sl);
            }
        }
    }
}
