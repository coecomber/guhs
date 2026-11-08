package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;

/**
 * Floor 1: De Zandwachter, a golem of rose sand with an hourglass in his chest.
 * <ul>
 *   <li><b>Zandhuid</b>: all damage x0.5, until the hourglass is turned. Then 18 s "kwetsbaar": x1.0 from the front, x1.8 in his
 *       BACK (positioning).</li>
 *   <li><b>De zandloper</b> (every 70 s, 50 s in fase 3): a 25 s bar "De zandloper loopt leeg". Both hourglass plates (pink, west
 *       and east) must hold players AT THE SAME TIME for 2 s (each plate max(1, n/4): 1 with 2-7 players, 2 with 8-10) -> he is
 *       turned over: stunned 8 s + kwetsbaar. Too late = "Begraven": 45 % of max Vads to everyone, slowed.</li>
 *   <li><b>Zandpilaren</b> (every 45 s, 30 s from fase 2): m() pillars round the hall; break them all in 20 s, or he builds a
 *       sand wall (12 s: x0.2 damage taken) and the hall pulses 20 %.</li>
 *   <li>Moves: Zandgolven (lanes across the hall, find the gap), Zandvuist (STACK: share it, alone it hurts), Zandspetters
 *       (SPREAD: a circle under everyone), Zandstorm (INTERRUPT: 4 % of his Vads in 3 s, else 25 % + slowed).</li>
 * </ul>
 */
public class Zandwachter extends Z6RaidBaas {
    private int zandloperCd = 20 * 25, pilaarCd = 20 * 35;
    private int loopT = -1, plaatOk, kwetsbaarT, muurT, begraven;
    private final List<UUID> pilaren = new ArrayList<>();
    private int pilaarT = -1;
    /** Gametests. */
    public int omgedraaid;

    public Zandwachter(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public String baasId() {
        return "zandwachter";
    }

    @Override
    public int verdieping() {
        return 1;
    }

    @Override
    protected double basisHp() {
        return 30000;
    }

    @Override
    protected double basisKlap() {
        return 190;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.5, 0.25};
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"slag", "golf", "draai", "storm"};
    }

    public boolean kwetsbaar() {
        return kwetsbaarT > 0;
    }

    public boolean loopt() {
        return loopT >= 0;
    }

    @Override
    protected double schadeFactor(@Nullable LivingEntity bron) {
        if (muurT > 0) {
            return 0.2;
        }
        if (kwetsbaarT <= 0) {
            return 0.5;
        }
        if (bron != null) {
            Vec3 kijk = Vec3.directionFromRotation(0, yBodyRot);
            Vec3 naar = bron.position().subtract(position()).multiply(1, 0, 1);
            if (naar.lengthSqr() > 0.01 && kijk.dot(naar.normalize()) < -0.35) {
                return 1.8;   // in his back
            }
        }
        return 1.0;
    }

    @Override
    protected void opFase(int fase) {
        if (fase == 2) {
            kaartAlle("vadsrijk.z6raid.zandwachter.fase2", ROOD);
        } else if (fase == 3) {
            kaartAlle("vadsrijk.z6raid.zandwachter.fase3", ROOD);
            zandloperCd = Math.min(zandloperCd, 60);
        }
    }

    /** How many players each plate needs. */
    public int plaatNodig() {
        return Math.max(1, n() / 4);
    }

    // ---- the hourglass -----------------------------------------------------------------------------------------------------

    public void startZandloper(ServerLevel sl) {
        loopT = 0;
        plaatOk = 0;
        int duur = 20 * 25;
        Vfx.cast(this, "vadsrijk.vfx.aanval.zandloper", duur, false);
        for (int i = 0; i < 2; i++) {
            Vec3 p = plaat(i);
            Vfx.decal(sl, nl.juiced.guhs.feature.vadsrijk.vfx.Vorm.cirkel(Z6RaidPlekken.PLAAT_R), p, 0, duur, ROZE, Vfx.nieuweGroep());
            Vfx.zuil(sl, p, 6, 0xFFB0E0, duur);
        }
        zeg("vadsrijk.z6raid.zandwachter.zandloper", ChatFormatting.LIGHT_PURPLE, plaatNodig());
        anim("draai");
    }

    /** Plate i (test spot when there is no anchor offset in tests: the arena centre +-12). */
    public Vec3 plaat(int i) {
        if (testN > 0) {
            return zMidden().add(i == 0 ? -12 : 12, 0, 0);
        }
        return Z6RaidPlekken.plaat(i);
    }

    public int opPlaat(int i) {
        Vec3 c = plaat(i);
        int n = 0;
        for (ServerPlayer p : strijders()) {
            if (Math.abs(p.getY() - c.y) < 2 && p.position().multiply(1, 0, 1).distanceTo(c.multiply(1, 0, 1)) <= Z6RaidPlekken.PLAAT_R + 0.3) {
                n++;
            }
        }
        return n;
    }

    private void zandloperTick(ServerLevel sl) {
        loopT++;
        if (loopT % 10 == 0) {
            int nodig = plaatNodig();
            boolean ok = opPlaat(0) >= nodig && opPlaat(1) >= nodig;
            plaatOk = ok ? plaatOk + 10 : 0;
            if (ok) {
                for (int i = 0; i < 2; i++) {
                    Vfx.deeltjes(sl, Deeltje.STER, plaat(i).add(0, 0.5, 0), 6, STERGOUD, 1.5, 0.04);
                }
            }
            if (plaatOk >= 40) {
                omdraaien(sl);
                return;
            }
        }
        if (loopT >= 20 * 25) {
            begraaf(sl);
        }
    }

    public void omdraaien(ServerLevel sl) {
        loopT = -1;
        plaatOk = 0;
        omgedraaid++;
        kwetsbaarT = 20 * 18;
        Vfx.castStop(this, true);
        Statussen.geef(this, Status.VERDOOFD, 160, 1, null);
        anim("draai");
        Vfx.golf(sl, position(), 8, STERGOUD, 16);
        Vfx.deeltjes(sl, Deeltje.STER, position().add(0, getBbHeight() * 0.6, 0), 40, STERGOUD, 1.5, 0.1);
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.6f);
        kaartAlle("vadsrijk.z6raid.zandwachter.omgedraaid", GEEL);
        zandloperCd = fase() >= 3 ? 20 * 50 : 20 * 70;
    }

    private void begraaf(ServerLevel sl) {
        loopT = -1;
        begraven++;
        Vfx.castStop(this, false);
        Vfx.schud(sl, position(), 40, 0.8f, 30);
        for (ServerPlayer p : strijders()) {
            mech(p, 0.45, "begraven");
            Statussen.geef(p, Status.PLAKKERIG, 100, 1, this);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_SAND.defaultBlockState()), p.getX(), p.getY() + 0.5, p.getZ(), 30, 0.6, 0.4,
                    0.6, 0.1);
        }
        kaartAlle("vadsrijk.z6raid.zandwachter.begraven", ROOD);
        zandloperCd = fase() >= 3 ? 20 * 50 : 20 * 70;
    }

    // ---- the pillars ---------------------------------------------------------------------------------------------------------

    public void pilaren(ServerLevel sl) {
        pilaren.clear();
        int m = m();
        double start = getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < m; i++) {
            if (Z6RaidRegistry.ZANDPILAAR.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
                double a = start + i * Math.PI * 2 / m;
                Vec3 v = zMidden().add(Math.cos(a) * 14, 0, Math.sin(a) * 14);
                d.koppel(this, i, maxVads() * 0.025, v);
                sl.addFreshEntity(d);
                pilaren.add(d.getUUID());
                Vfx.zuil(sl, v, 4, ZAND, 20);
            }
        }
        pilaarT = 20 * 20;
        zeg("vadsrijk.z6raid.zandwachter.pilaren", ChatFormatting.GOLD, m);
        pilaarCd = fase() >= 2 ? 20 * 30 : 20 * 45;
    }

    public int pilarenOver() {
        return pilaren.size();
    }

    @Override
    public void doelKapot(Z3Doel d, @Nullable LivingEntity bron) {
        if (pilaren.remove(d.getUUID()) && level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_SANDSTONE.defaultBlockState()), d.getX(), d.getY() + 1, d.getZ(), 40, 0.5, 1, 0.5,
                    0.1);
            d.discard();
            if (pilaren.isEmpty()) {
                pilaarT = -1;
                zeg("vadsrijk.z6raid.zandwachter.pilaren_weg", ChatFormatting.GREEN);
            }
        }
    }

    private void zandmuur(ServerLevel sl) {
        pilaarT = -1;
        muurT = 20 * 12;
        for (UUID u : pilaren) {
            if (sl.getEntity(u) instanceof Z3Doel d) {
                d.discard();
            }
        }
        pilaren.clear();
        for (ServerPlayer p : strijders()) {
            mech(p, 0.2, "zandmuur");
        }
        Vfx.golf(sl, zMidden(), 20, ZAND, 20);
        kaartAlle("vadsrijk.z6raid.zandwachter.muur", ROOD);
    }

    // ---- the rotation ----------------------------------------------------------------------------------------------------------

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (l.isEmpty() || loopt() || giet()) {
            rustT = 10;
            return;
        }
        int keuze = getRandom().nextInt(4);
        Vec3 m = zMidden();
        switch (keuze) {
            case 0 -> {   // Zandgolven: lanes across the hall, one gap per 3
                double hoek = getRandom().nextInt(4) * Math.PI / 4;
                Vec3 r = new Vec3(Math.cos(hoek), 0, Math.sin(hoek)), dw = new Vec3(-r.z, 0, r.x);
                int gat = getRandom().nextInt(3);
                List<Vec3[]> banen = new ArrayList<>();
                for (int i = -3; i <= 3; i++) {
                    if (Math.floorMod(i, 3) == gat) {
                        continue;
                    }
                    Vec3 o = m.add(dw.scale(i * 5));
                    banen.add(new Vec3[] {o.subtract(r.scale(22)), o.add(r.scale(22))});
                }
                int w = tg(40) - (fase() >= 2 ? 6 : 0);
                for (Vec3[] b : banen) {
                    Telegraaf.lijn(sl, b[0], b[1], 4.6, w, ROOD);
                }
                BaasVfx.windup(this, w);
                anim("golf");
                later(w, () -> {
                    for (Vec3[] b : banen) {
                        Vfx.straal(sl, b[0].add(0, 0.3, 0), b[1].add(0, 0.3, 0), 2.2, ZAND, 10);
                        for (ServerPlayer p : strijders()) {
                            if (inStrook(p.position(), b[0], b[1], 4.6)) {
                                mech(p, 0.3, "zandgolf");
                                duw(p, b[0].lerp(b[1], 0.5), 0.6);
                            }
                        }
                    }
                    Vfx.schud(sl, m, 30, 0.4f, 10);
                });
                laatsteAanval = "zandgolf";
                rustT = w + 30;
            }
            case 1 -> {   // Zandvuist: a STACK on his target
                ServerPlayer d = zDoel() != null ? zDoel() : l.get(0);
                Vec3 v = voetVan(d);
                zeg("vadsrijk.z6raid.zandwachter.vuist", ChatFormatting.YELLOW, d.getDisplayName());
                anim("slag");
                stapel(v, 3.2, 60, 0.9, 0.35, Math.min(3, n()), "zandvuist", ok -> Vfx.golf(sl, v, 4, ok ? STERGOUD : ROOD, 10));
                laatsteAanval = "zandvuist";
                rustT = 90;
            }
            case 2 -> {   // Zandspetters: SPREAD
                for (ServerPlayer p : l) {
                    inslag(voetVan(p), 3.0, 40, 0.18, "zandspetter", Status.PLAKKERIG, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()));
                }
                zeg("vadsrijk.z6raid.zandwachter.spetters", ChatFormatting.GOLD);
                anim("slag");
                laatsteAanval = "zandspetters";
                rustT = 70;
            }
            default -> {   // Zandstorm: INTERRUPT
                anim("storm");
                giet("zandstorm", tg(60), 0.04, () -> {
                    for (ServerPlayer p : strijders()) {
                        mech(p, 0.25, "zandstorm");
                        Statussen.geef(p, Status.PLAKKERIG, 80, 1, this);
                    }
                    Vfx.golf(sl, m, 22, ZAND, 20);
                    sl.sendParticles(ParticleTypes.CLOUD, m.x, m.y + 2, m.z, 80, 10, 2, 10, 0.05);
                }, null);
                laatsteAanval = "zandstorm";
                rustT = 90;
            }
        }
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        if (kwetsbaarT > 0 && --kwetsbaarT == 0) {
            zeg("vadsrijk.z6raid.zandwachter.huid", ChatFormatting.GRAY);
        }
        if (muurT > 0) {
            muurT--;
            if (tickCount % 10 == 0) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_SAND.defaultBlockState()), getX(), getY() + 1.5, getZ(), 12, 1.5, 1.5, 1.5,
                        0.05);
            }
        }
        if (pilaarT > 0 && --pilaarT == 0 && !pilaren.isEmpty()) {
            zandmuur(sl);
        }
        if (loopt()) {
            zandloperTick(sl);
        }
        if (gietTick()) {
            return;
        }
        if (inIntro() || l.isEmpty()) {
            return;
        }
        if (!loopt() && --zandloperCd <= 0) {
            startZandloper(sl);
        }
        if (--pilaarCd <= 0 && pilaarT < 0) {
            pilaren(sl);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl) {
            for (UUID u : pilaren) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    d.discard();
                }
            }
        }
        super.remove(reason);
    }
}
