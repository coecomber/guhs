package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;

/**
 * Floor 2: De Spiegelvizier = Meester Glans, the mirror polisher of Saffraanpoort's Spiegelzaal, a Mika illusionist and Kootje of
 * Hertogin Ringeling (identity chosen by U-Z6RAID, ZONE6_PLAN §12.8 "zelf invullen"; u-z6verhaal reveals him in hq09). A teamwork
 * light puzzle:
 * <ul>
 *   <li><b>Verstoppertje</b>: he hides among m()+1 identical mirror images on a ring (all immune). Only the real one sparkles now and
 *       then (he can't stop polishing himself: look closely!).</li>
 *   <li><b>De draaispiegels</b>: m() turning mirrors round the prism; the beam from the roof prism hits every mirror; a right-click
 *       turns a mirror to aim at the NEXT image. Where 2+ beams meet on an image, it is unmasked: the real one = 20 s kwetsbaar (he
 *       fights); a fake one shatters into a Spiegelkloon (an add) and everyone takes 10 %. Not found in 40 s = Spiegelflits (30 %).</li>
 *   <li><b>Wie is wie?</b> every 30 s he swaps two players' places (talk to each other!).</li>
 *   <li>Unmasked: Glansstraal (a cone), Verblindende Glans (INTERRUPT: 4 % in 2.5 s, else 30 % + blind), bites.</li>
 *   <li><b>20 %</b>: the hall breaks: he stays unmasked, shards fall as red circles (m() at a time) until the end.</li>
 * </ul>
 * One class for three roles ({@link #rol}): the real one (ECHT, the arena boss), the images (BEELD) and the clones (KLOON).
 */
public class Spiegelvizier extends Z6RaidBaas {
    public static final int ECHT = 0, BEELD = 1, KLOON = 2;
    private static final EntityDataAccessor<Integer> ROL = SynchedEntityData.defineId(Spiegelvizier.class, EntityDataSerializers.INT);
    public static final double RING = 15, SPIEGEL_RING = 8;

    private final List<UUID> beelden = new ArrayList<>(), spiegels = new ArrayList<>(), klonen = new ArrayList<>();
    /** The image spots of this round (the real one stands on one of them). */
    private final List<Vec3> plekken = new ArrayList<>();
    private int verstopT = -1, kwetsbaarT, wisselCd = 20 * 30, verstopCd = 20 * 6, scherfCd;
    private int kloonLeven = 20 * 60;
    @Nullable
    private UUID meester;
    /** Gametests. */
    public int ontmaskerd, fouten;

    public Spiegelvizier(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ROL, ECHT);
    }

    public int rol() {
        return entityData.get(ROL);
    }

    public void zetRol(int r) {
        entityData.set(ROL, r);
    }

    @Override
    public boolean echteBaas() {
        return rol() == ECHT;
    }

    @Override
    public String baasId() {
        return "spiegelvizier";
    }

    @Override
    public int verdieping() {
        return 2;
    }

    @Override
    protected double basisHp() {
        return rol() == KLOON ? 900 : 32000;
    }

    @Override
    protected double basisKlap() {
        return rol() == KLOON ? 110 : 185;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.2};
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"poets", "straal", "wissel", "verstop"};
    }

    @Override
    protected boolean isMika() {
        return true;
    }

    public boolean verstopt() {
        return verstopT >= 0;
    }

    public boolean kwetsbaar() {
        return kwetsbaarT > 0 || fase() >= 2;
    }

    @Override
    protected boolean immuun() {
        return rol() == BEELD || (rol() == ECHT && verstopt());
    }

    @Override
    protected String immuunKey() {
        return "vadsrijk.z6raid.spiegelvizier.immuun";
    }

    @Override
    protected boolean slaapt() {
        return rol() == BEELD;
    }

    @Override
    protected boolean bezig() {
        return super.bezig() || (rol() == ECHT && verstopt());
    }

    @Override
    public void schaal(int spelers) {
        if (rol() == ECHT) {
            super.schaal(spelers);
        }
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (rol() == BEELD && bron instanceof ServerPlayer p && level() instanceof ServerLevel sl && meester() instanceof Spiegelvizier m
                && getRandom().nextInt(4) == 0 && tickCount % 3 == 0) {
            m.kloon(sl, position());   // hitting a fake one wakes a clone
            meld(p, "vadsrijk.z6raid.spiegelvizier.nep", ChatFormatting.RED);
        }
        super.krijgSchade(n, bron);
    }

    @Nullable
    private Spiegelvizier meester() {
        return meester != null && level() instanceof ServerLevel sl && sl.getEntity(meester) instanceof Spiegelvizier m ? m : null;
    }

    // ---- hiding --------------------------------------------------------------------------------------------------------------

    /** Hides among the images: m()+1 spots on the ring, the real one on a random one, m() mirrors round the prism. */
    public void verstop(ServerLevel sl) {
        wegBeelden(sl);
        plekken.clear();
        int k = m() + 1;
        double start = getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < k; i++) {
            double a = start + i * Math.PI * 2 / k;
            plekken.add(zMidden().add(Math.cos(a) * RING, 0, Math.sin(a) * RING));
        }
        int echt = getRandom().nextInt(k);
        Vfx.flits(sl, position().add(0, 1, 0), 3, SPIEGEL);
        snapTo(plekken.get(echt).x, plekken.get(echt).y, plekken.get(echt).z, kijkNaarMidden(plekken.get(echt)), 0);
        getNavigation().stop();
        for (int i = 0; i < k; i++) {
            if (i == echt) {
                continue;
            }
            if (Z6RaidRegistry.SPIEGELVIZIER.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Spiegelvizier b) {
                Vec3 v = plekken.get(i);
                b.zetRol(BEELD);
                b.meester = getUUID();
                b.testN = testN;
                b.testFilter = testFilter;
                b.stelIn(mobLevel(), element(), false, java.util.Set.of());
                b.snapTo(v.x, v.y, v.z, kijkNaarMidden(v), 0);
                b.zetThuis(v);
                sl.addFreshEntity(b);
                beelden.add(b.getUUID());
                Vfx.flits(sl, v.add(0, 1, 0), 2, SPIEGEL);
            }
        }
        if (spiegels.isEmpty()) {
            for (int i = 0; i < m(); i++) {
                if (Z6RaidRegistry.DRAAISPIEGEL.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Z3Doel d) {
                    double a = i * Math.PI * 2 / m() + Math.PI / m();
                    Vec3 v = zMidden().add(Math.cos(a) * SPIEGEL_RING, 0, Math.sin(a) * SPIEGEL_RING);
                    d.koppel(this, i, 1000, v);
                    d.zetRaakbaar(false);
                    d.zetStand(i % k);
                    sl.addFreshEntity(d);
                    spiegels.add(d.getUUID());
                }
            }
        }
        richtSpiegels(sl);
        verstopT = 0;
        kwetsbaarT = 0;
        anim("verstop");
        kaartAlle("vadsrijk.z6raid.spiegelvizier.verstop", PAARS);
        zegGroep("vadsrijk.z6raid.spiegelvizier.zeg.verstop");
    }

    private float kijkNaarMidden(Vec3 v) {
        Vec3 d = zMidden().subtract(v);
        return (float) (Math.atan2(-d.x, d.z) * 180 / Math.PI);
    }

    /** Index of the image spot the real one stands on (-1 when not hiding). */
    public int echtePlek() {
        for (int i = 0; i < plekken.size(); i++) {
            if (position().multiply(1, 0, 1).distanceTo(plekken.get(i).multiply(1, 0, 1)) < 1.2) {
                return i;
            }
        }
        return -1;
    }

    public List<Vec3> plekken() {
        return plekken;
    }

    public List<Z3Doel> spiegels() {
        List<Z3Doel> l = new ArrayList<>();
        if (level() instanceof ServerLevel sl) {
            for (UUID u : spiegels) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    l.add(d);
                }
            }
        }
        return l;
    }

    /** A right-click on a turning mirror: aim at the next image. */
    @Override
    public void doelGebruikt(Z3Doel d, ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl) || !spiegels.contains(d.getUUID()) || plekken.isEmpty()) {
            return;
        }
        d.zetStand((d.stand() + 1) % plekken.size());
        richt(d);
        sl.playSound(null, d.blockPosition(), SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.BLOCKS, 1.2f, 0.8f + 0.1f * d.stand());
        meld(p, "vadsrijk.z6raid.spiegelvizier.draai", ChatFormatting.AQUA, d.stand() + 1, plekken.size());
    }

    private void richt(Z3Doel d) {
        if (plekken.isEmpty()) {
            return;
        }
        Vec3 doel = plekken.get(Math.floorMod(d.stand(), plekken.size()));
        Vec3 r = doel.subtract(d.position());
        d.kijk((float) (Math.atan2(-r.x, r.z) * 180 / Math.PI));
    }

    private void richtSpiegels(ServerLevel sl) {
        for (Z3Doel d : spiegels()) {
            d.zetStand(Math.floorMod(d.stand(), plekken.size()));
            richt(d);
        }
    }

    /** How many beams end on each image spot (2+ = unmasked). */
    public int[] bundels() {
        int[] t = new int[plekken.size()];
        for (Z3Doel d : spiegels()) {
            if (!plekken.isEmpty()) {
                t[Math.floorMod(d.stand(), plekken.size())]++;
            }
        }
        return t;
    }

    private void verstopTick(ServerLevel sl) {
        verstopT++;
        Vec3 prisma = zMidden().add(0, 8.5, 0);
        if (verstopT % 10 == 0) {
            for (Z3Doel d : spiegels()) {
                Vec3 a = d.position().add(0, 1.4, 0);
                Vec3 b = plekken.get(Math.floorMod(d.stand(), plekken.size())).add(0, 1.4, 0);
                Vfx.straal(sl, prisma, a, 0.35, 0xFFF6D0, 12);
                Vfx.straal(sl, a, b, 0.5, 0xFFF0A0, 12);
            }
        }
        if (verstopT % 60 == 0) {   // the tell: only the real one sparkles (he polishes himself)
            Vfx.deeltjes(sl, Deeltje.STER, position().add(0, 1.9, 0), 3, 0xFFFFFF, 0.25, 0.01);
        }
        if (verstopT > 30 && verstopT % 10 == 0) {
            int[] b = bundels();
            for (int i = 0; i < b.length; i++) {
                if (b[i] >= 2) {
                    ontmasker(sl, i);
                    return;
                }
            }
        }
        if (verstopT >= 20 * 40) {
            spiegelflits(sl);
        }
    }

    /** 2+ beams on image i. */
    public void ontmasker(ServerLevel sl, int i) {
        if (i == echtePlek()) {
            ontmaskerd++;
            verstopT = -1;
            kwetsbaarT = 20 * 20;
            wegBeelden(sl);
            Vfx.flits(sl, position().add(0, 1, 0), 4, 0xFFFFFF);
            Vfx.golf(sl, position(), 6, SPIEGEL, 12);
            kaartAlle("vadsrijk.z6raid.spiegelvizier.gevonden", GEEL);
            zegGroep("vadsrijk.z6raid.spiegelvizier.zeg.gevonden");
            nl.juiced.guhs.feature.vadsrijk.kern.Statussen.geef(this, nl.juiced.guhs.feature.vadsrijk.kern.Status.VERDOOFD, 60, 1, null);
            rustT = 60;
            return;
        }
        fouten++;
        Vec3 v = plekken.get(i);
        for (UUID u : new ArrayList<>(beelden)) {
            if (sl.getEntity(u) instanceof Spiegelvizier b && b.position().distanceToSqr(v) < 2) {
                beelden.remove(u);
                Vfx.deeltjes(sl, Deeltje.KRISTAL, b.position().add(0, 1, 0), 30, SPIEGEL, 0.8, 0.15);
                b.discard();
            }
        }
        sl.playSound(null, net.minecraft.core.BlockPos.containing(v), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.8f);
        for (ServerPlayer p : strijders()) {
            mech(p, 0.10, "spiegelscherf");
        }
        kloon(sl, v);
        // the mirrors that pointed there turn away (one click on)
        for (Z3Doel d : spiegels()) {
            if (Math.floorMod(d.stand(), plekken.size()) == i) {
                d.zetStand(d.stand() + 1);
                richt(d);
            }
        }
        zeg("vadsrijk.z6raid.spiegelvizier.fout", ChatFormatting.RED);
    }

    private void spiegelflits(ServerLevel sl) {
        Vfx.flits(sl, zMidden().add(0, 3, 0), 10, 0xFFFFFF);
        for (ServerPlayer p : strijders()) {
            mech(p, 0.3, "spiegelflits");
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        }
        kaartAlle("vadsrijk.z6raid.spiegelvizier.flits", ROOD);
        verstop(sl);
    }

    /** A Spiegelkloon add at v. */
    public void kloon(ServerLevel sl, Vec3 v) {
        if (klonen.size() >= Z6Schaal.adds(n()) + 2) {
            return;
        }
        if (Z6RaidRegistry.SPIEGELVIZIER.get().create(sl, EntitySpawnReason.MOB_SUMMONED) instanceof Spiegelvizier c) {
            c.zetRol(KLOON);
            c.meester = getUUID();
            c.testN = testN;
            c.testFilter = testFilter;
            c.stelIn(mobLevel(), element(), false, java.util.Set.of());
            c.schaalKloon();
            c.snapTo(v.x, v.y, v.z, 0, 0);
            c.zetThuis(zMidden());
            sl.addFreshEntity(c);
            klonen.add(c.getUUID());
            Vfx.flits(sl, v.add(0, 1, 0), 2, SPIEGEL);
        }
    }

    private void schaalKloon() {
        zetMaxVads(basisVads(mobLevel()) * (0.7 + 0.15 * n()), true);
    }

    public int klonen() {
        return klonen.size();
    }

    private void wegBeelden(ServerLevel sl) {
        for (UUID u : beelden) {
            if (sl.getEntity(u) instanceof Spiegelvizier b) {
                Vfx.deeltjes(sl, Deeltje.KRISTAL, b.position().add(0, 1, 0), 12, SPIEGEL, 0.6, 0.1);
                b.discard();
            }
        }
        beelden.clear();
    }

    /** "Wie is wie?": two players swap places. */
    public void wissel(ServerLevel sl, List<ServerPlayer> l) {
        if (l.size() < 2) {
            return;
        }
        ServerPlayer a = l.get(getRandom().nextInt(l.size())), b;
        do {
            b = l.get(getRandom().nextInt(l.size()));
        } while (b == a);
        Vec3 pa = a.position(), pb = b.position();
        a.teleportTo(pb.x, pb.y, pb.z);
        b.teleportTo(pa.x, pa.y, pa.z);
        Vfx.flits(sl, pa.add(0, 1, 0), 1.6, SPIEGEL);
        Vfx.flits(sl, pb.add(0, 1, 0), 1.6, SPIEGEL);
        meld(a, "vadsrijk.z6raid.spiegelvizier.gewisseld", ChatFormatting.AQUA, b.getDisplayName());
        meld(b, "vadsrijk.z6raid.spiegelvizier.gewisseld", ChatFormatting.AQUA, a.getDisplayName());
        zegGroep("vadsrijk.z6raid.spiegelvizier.zeg.wissel");
        anim("wissel");
    }

    // ---- the fight -----------------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        if (fase == 2 && level() instanceof ServerLevel sl) {
            verstopT = -1;
            wegBeelden(sl);
            kaartAlle("vadsrijk.z6raid.spiegelvizier.breekt", ROOD);
            zegGroep("vadsrijk.z6raid.spiegelvizier.zeg.breekt");
            Vfx.schud(sl, zMidden(), 30, 0.7f, 30);
            scherfCd = 40;
        }
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (rol() != ECHT) {
            rustT = 20;
            return;
        }
        if (l.isEmpty() || verstopt() || giet()) {
            rustT = 10;
            return;
        }
        ServerPlayer d = zDoel() != null ? zDoel() : l.get(0);
        if (getRandom().nextBoolean()) {   // Glansstraal: a cone
            Vec3 r = d.position().subtract(position()).multiply(1, 0, 1);
            double hoek = Math.atan2(r.z, r.x);
            int w = tg(30);
            Telegraaf.kegel(sl, position(), hoek, 13, 70, w, ROOD);
            BaasVfx.windup(this, w);
            anim("straal");
            Vec3 van = position();
            later(w, () -> {
                for (ServerPlayer p : strijders()) {
                    Vec3 v = p.position().subtract(van).multiply(1, 0, 1);
                    double a = Math.atan2(v.z, v.x);
                    double verschil = Math.abs(Math.atan2(Math.sin(a - hoek), Math.cos(a - hoek)));
                    if (v.length() <= 13 && verschil <= Math.toRadians(35)) {
                        mech(p, 0.28, "glansstraal");
                    }
                }
                Vfx.straal(sl, van.add(0, 1.4, 0), van.add(Math.cos(hoek) * 13, 1.4, Math.sin(hoek) * 13), 2.5, SPIEGEL, 10);
            });
            laatsteAanval = "glansstraal";
            rustT = w + 30;
        } else {   // Verblindende Glans: INTERRUPT
            anim("poets");
            giet("verblindende_glans", tg(50), 0.04, () -> {
                Vfx.flits(sl, position().add(0, 1.5, 0), 8, 0xFFFFFF);
                for (ServerPlayer p : strijders()) {
                    mech(p, 0.3, "verblindende_glans");
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                }
            }, null);
            laatsteAanval = "verblindende_glans";
            rustT = 80;
        }
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        if (rol() == KLOON) {
            if (--kloonLeven <= 0 || meester() == null) {
                Vfx.deeltjes(sl, Deeltje.KRISTAL, position().add(0, 1, 0), 16, SPIEGEL, 0.6, 0.1);
                discard();
            }
            return;
        }
        if (rol() != ECHT) {
            return;
        }
        klonen.removeIf(u -> !(sl.getEntity(u) instanceof Spiegelvizier c) || c.isRemoved());
        if (verstopt()) {
            verstopTick(sl);
        }
        if (gietTick()) {
            return;
        }
        if (inIntro() || l.isEmpty()) {
            return;
        }
        if (--wisselCd <= 0) {
            wisselCd = 20 * 30;
            wissel(sl, l);
        }
        if (fase() >= 2) {
            if (--scherfCd <= 0) {
                scherfCd = 30;
                for (int i = 0; i < m(); i++) {
                    ServerPlayer p = l.get(getRandom().nextInt(l.size()));
                    inslag(voetVan(p.position().add(getRandom().nextInt(5) - 2, 0, getRandom().nextInt(5) - 2)), 2.5, 30, 0.2, "spiegelscherf", null,
                            ParticleTypes.END_ROD);
                }
            }
            return;
        }
        if (!verstopt()) {
            if (kwetsbaarT > 0) {
                if (--kwetsbaarT == 0) {
                    verstopCd = 20;
                }
            } else if (--verstopCd <= 0) {
                verstopCd = 20 * 6;
                verstop(sl);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl && rol() == ECHT) {
            wegBeelden(sl);
            for (UUID u : spiegels) {
                if (sl.getEntity(u) instanceof Z3Doel d) {
                    d.discard();
                }
            }
            for (UUID u : klonen) {
                if (sl.getEntity(u) instanceof Spiegelvizier c) {
                    c.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return rol() == ECHT && super.shouldBeSaved();
    }

    @Override
    public Component getName() {
        return rol() == KLOON ? Component.translatable("vadsrijk.z6raid.spiegelkloon") : super.getName();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("vr_z6_rol", rol());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        zetRol(in.getIntOr("vr_z6_rol", ECHT));
    }
}
