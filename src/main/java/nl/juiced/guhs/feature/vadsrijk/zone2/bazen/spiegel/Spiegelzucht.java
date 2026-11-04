package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Baas;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2BazenRegistry;

/**
 * Vrouwe Spiegelzucht, boss of the Spiegelburcht (ronde 15), lv 33, respawn 5 min.
 * <ul>
 *   <li><b>Spiegeling</b>: she splits into mirror images ({@link Spiegelbeeld}) on a ring. Only the one WITHOUT a shadow is her:
 *       hit an image and it shatters and strikes back (20 % of your Vads). Deal 5 % of her Vads while split and the images break.</li>
 *   <li>The light: fase 1 it falls from the north window (every image has a shadow, she has none). Fase 2 (70 %): a light orb
 *       circles the hall; only images in its cone cast a shadow, so wait for the light to sweep over them. Fase 3 (35 %): two orbs,
 *       faster, and the hall shatters (the glass dome breaks open ring by ring, shards rain down: red circles). Everything comes back
 *       after ({@link Herstel}).</li>
 *   <li><b>Spiegelstraal</b>: she stands on one of two spots and one of the 4 crystal eyes in the wall charges a beam (its path glows
 *       purple, live). It meets one of the hall mirrors first: fold it (right-click to flat) so the beam goes on and hits HER (blinded:
 *       stunned 5 s, 8 % damage, the images break). Whoever stands in the beam's path when it fires takes 45 %.</li>
 *   <li>Also: shard throws (red circles, also at players at range), a blink to whoever keeps their distance (red circle under them),
 *       and from fase 2 the <b>Scherfregen</b>: a yellow circle on one player; everyone inside shares the damage (stack!).</li>
 * </ul>
 */
public class Spiegelzucht extends Z2Baas {
    public static final String H_HAL = "z2_spiegel_hal", H_ZAAL = "z2_spiegel_zaal";
    private static final EntityDataAccessor<Boolean> GESPLITST = SynchedEntityData.defineId(Spiegelzucht.class, EntityDataSerializers.BOOLEAN);
    public static final int SPLIT_TICKS = 300, STRAAL_TICKS = 70, REGEN_TICKS = 60;
    public static final double GEVONDEN = 0.05;
    private static final DustParticleOptions SCHADUW = new DustParticleOptions(0x120E18, 2.2f), PAARS_STOF = new DustParticleOptions(0xA040FF, 1.2f),
            ROOD_STOF = new DustParticleOptions(0xFF2A2A, 1.6f), LICHT = new DustParticleOptions(0xFFF6D8, 2.4f);

    public enum Actie {
        GEEN, STRAAL, REGEN
    }

    private record Inslag(Vec3 plek, double straal, int[] t, int duur, double factor, String skill, boolean bevriest) {
    }

    private Actie actie = Actie.GEEN;
    private int actieT, splitT, splitCd = 100, worpCd = 40, straalCd = 280, sprongCd = 80, regenCd = 200, zuilT = -1, eigenWorpT;
    private double splitSchade, lichtHoek = -Math.PI / 2;
    private final List<java.util.UUID> beelden = new ArrayList<>();
    private final List<Inslag> inslagen = new ArrayList<>();
    private int poort, zuilen;
    private Vec3 regenPlek = Vec3.ZERO;
    @Nullable
    private java.util.UUID regenDoel;
    private boolean opgezet;
    /** Gametests/AutoCheck. */
    public int splitsingen, straalRaak, straalMis, beeldenGebroken;

    public Spiegelzucht(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GESPLITST, false);
    }

    public boolean gesplitst() {
        return entityData.get(GESPLITST);
    }

    public Actie actie() {
        return actie;
    }

    @Override
    public String baasId() {
        return "spiegelzucht";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"spiegel", "worp", "straal"};
    }

    @Override
    protected double basisVads(int level) {
        return 6000 + 260 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return 18 + 2.1 * level;
    }

    @Override
    public int xp() {
        return 4200 + 150 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.NATUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.7, 0.35};
    }

    @Override
    protected boolean bezig() {
        return actie != Actie.GEEN || gesplitst();
    }

    // ---- the hall ------------------------------------------------------------------------------------------------------

    /** Crystal eye i ({@link Z2BazenPlekken#HAL_OGEN}). */
    public BlockPos oog(int i) {
        Z2BazenPlekken.Oog o = Z2BazenPlekken.HAL_OGEN.get(i);
        return BlockPos.containing(zMidden()).offset(o.dx(), 0, o.dz());
    }

    public static int oogRichting(int i) {
        return Z2BazenPlekken.HAL_OGEN.get(i).richting();
    }

    /** The mirror on the line of eye i. */
    public BlockPos spiegel(int i) {
        int[] s = Z2BazenPlekken.HAL_SPIEGELS[Z2BazenPlekken.HAL_OGEN.get(i).spiegel()];
        return BlockPos.containing(zMidden()).offset(s[0], 0, s[1]);
    }

    /** Where she stands while eye i charges (on its line, behind its mirror). */
    public Vec3 straalPlek(int i) {
        Z2BazenPlekken.Oog o = Z2BazenPlekken.HAL_OGEN.get(i);
        return zMidden().add(o.sdx(), 0, o.sdz());
    }

    private void zetHal(ServerLevel sl) {
        for (int i = 0; i < Z2BazenPlekken.HAL_OGEN.size(); i++) {
            BlockPos o = oog(i);
            if (sl.getBlockState(o).isAir() || sl.getBlockState(o).getBlock() instanceof KristalBlokken.Lichtbron) {
                Herstel.zet(sl, H_HAL, o, Z2BazenRegistry.LICHTBRON.get().defaultBlockState()
                        .setValue(KristalBlokken.Lichtbron.FACING, KristalBlokken.richting(oogRichting(i))));
            }
        }
        draaiSpiegels(sl);
    }

    /** All hall mirrors back to a random diagonal (the beam misses her until someone turns one). */
    public void draaiSpiegels(ServerLevel sl) {
        for (int i = 0; i < Z2BazenPlekken.HAL_OGEN.size(); i++) {
            BlockPos s = spiegel(i);
            BlockState st = sl.getBlockState(s);
            if (st.isAir() || st.getBlock() instanceof SpiegelBlok) {
                Herstel.zet(sl, H_HAL, s, Z2BazenRegistry.SPIEGEL.get().defaultBlockState().setValue(SpiegelBlok.STAND, getRandom().nextInt(2)));
            }
        }
    }

    /** Fase 3: one ring of the glass dome breaks open (remembered, comes back after). */
    private void breekKoepel(ServerLevel sl, int ring) {
        BlockPos c = BlockPos.containing(zMidden());
        int r = Z2BazenPlekken.KOEPEL_STRAAL;
        int n = 0;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int d = (int) Math.round(Math.sqrt(x * x + z * z));
                if (d != ring && d != ring + 1) {
                    continue;
                }
                for (int y = Z2BazenPlekken.KOEPEL_ONDER; y <= Z2BazenPlekken.KOEPEL_BOVEN; y++) {
                    BlockPos p = c.offset(x, y, z);
                    BlockState s = sl.getBlockState(p);
                    if (s.isAir() || s.getDestroySpeed(sl, p) < 0) {
                        continue;
                    }
                    Herstel.zet(sl, H_ZAAL, p, Blocks.AIR.defaultBlockState());
                    if (n++ % 4 == 0) {
                        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.1);
                    }
                }
            }
        }
        if (n > 0) {
            sl.playSound(null, c.above(Z2BazenPlekken.KOEPEL_ONDER), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.5f);
        }
        // the shards rain down: red circles
        List<ServerPlayer> l = strijders();
        for (int k = 0; k < 2; k++) {
            double a = getRandom().nextDouble() * Math.PI * 2, rr = Math.min(Z2BazenPlekken.SPIEGEL_STRAAL - 3, ring + getRandom().nextDouble() * 3);
            inslag(sl, zMidden().add(Math.cos(a) * rr, 0, Math.sin(a) * rr), 2.5, 30, 1.2, "zaal", false, ROOD);
        }
        if (!l.isEmpty()) {
            inslag(sl, voetVan(l.get(getRandom().nextInt(l.size()))), 2.5, 30, 1.2, "zaal", false, ROOD);
        }
    }

    public static void herstel(ServerLevel sl) {
        Herstel.terug(sl, H_HAL);
        Herstel.terug(sl, H_ZAAL);
    }

    // ---- the light ---------------------------------------------------------------------------------------------------

    /** The light directions (unit vectors from the centre to the light): fase 1 north, 2 one orb, 3 two orbs. */
    public List<Vec3> lichten() {
        List<Vec3> l = new ArrayList<>();
        l.add(new Vec3(Math.cos(lichtHoek), 0, Math.sin(lichtHoek)));
        if (fase() >= 3) {
            l.add(new Vec3(Math.cos(lichtHoek + Math.PI), 0, Math.sin(lichtHoek + Math.PI)));
        }
        return l;
    }

    /** Does the light fall on this spot (so a mirror image there shows its shadow)? Fase 1: always. */
    public boolean verlicht(Vec3 v) {
        if (fase() <= 1) {
            return true;
        }
        Vec3 d = v.subtract(zMidden()).multiply(1, 0, 1);
        if (d.lengthSqr() < 1) {
            return true;
        }
        d = d.normalize();
        double kegel = Math.cos(Math.toRadians(fase() >= 3 ? 45 : 55));
        for (Vec3 l : lichten()) {
            if (d.dot(l) >= kegel) {
                return true;
            }
        }
        return false;
    }

    /** The shadow of a lit image: a dark smear on the floor away from the light. */
    private void schaduw(ServerLevel sl, Vec3 v) {
        Vec3 bron = null;
        double best = -2;
        Vec3 d = v.subtract(zMidden()).multiply(1, 0, 1);
        d = d.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : d.normalize();
        for (Vec3 l : lichten()) {
            if (d.dot(l) > best) {
                best = d.dot(l);
                bron = zMidden().add(l.scale(18));
            }
        }
        Vec3 r = v.subtract(bron).multiply(1, 0, 1);
        r = r.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : r.normalize();
        Vec3 n = new Vec3(-r.z, 0, r.x);
        for (double s = 0.5; s <= 2.6; s += 0.35) {
            for (double w = -0.4; w <= 0.4; w += 0.4) {
                Vec3 p = v.add(r.scale(s)).add(n.scale(w * (1 - s / 3.2)));
                sl.sendParticles(SCHADUW, p.x, v.y + 0.08, p.z, 1, 0.02, 0, 0.02, 0);
            }
        }
    }

    // ---- moves --------------------------------------------------------------------------------------------------------------

    @Override
    protected void signatuur() {
        speel("spiegel");
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.5f);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 80, 1.5, 1.5, 1.5, 0.05);
        }
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        speel("spiegel");
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3f, 0.4f);
        if (fase == 2) {
            kaart("vadsrijk.z2bazen.sz.fase2", 0xFFF0B0);
        } else if (fase == 3) {
            kaart("vadsrijk.z2bazen.sz.fase3", 0xC8C8D8);
            zuilT = 20;
        }
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (splitCd <= 0) {
            splits(sl);
            return;
        }
        if (straalCd <= 0) {
            startStraal(sl);
            return;
        }
        if (fase() >= 2 && regenCd <= 0 && !l.isEmpty()) {
            ServerPlayer p = l.get(getRandom().nextInt(l.size()));
            regenDoel = p.getUUID();
            regenPlek = voetVan(p);
            Telegraaf.cirkel(sl, regenPlek, 3, REGEN_TICKS, GEEL);
            actie = Actie.REGEN;
            actieT = REGEN_TICKS;
            regenCd = 420;
            rustT = REGEN_TICKS + 10;
            zeg("vadsrijk.z2bazen.sz.regen", ChatFormatting.YELLOW, p.getDisplayName());
            laatsteAanval = "regen";
            return;
        }
        if (sprongCd <= 0 && iemandOpAfstand()) {
            ServerPlayer v = afstandsDoel();
            if (v != null) {
                Vec3 naar = voetVan(v);
                Vec3 r = position().subtract(naar).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
                blink(sl, naar.add(r.scale(2.5)));
                inslag(sl, naar, 3, 25, 1.5, "sprong", false, ROOD);
                sprongCd = 160;
                rustT = 30;
                laatsteAanval = "sprong";
                return;
            }
        }
        if (worpCd <= 0) {
            List<ServerPlayer> doelen = new ArrayList<>(l);
            java.util.Collections.shuffle(doelen, new java.util.Random(getRandom().nextLong()));
            ServerPlayer ver = afstandsDoel();
            if (ver != null) {
                doelen.remove(ver);
                doelen.add(0, ver);
            }
            for (int i = 0; i < Math.min(3, doelen.size()); i++) {
                inslag(sl, voetVan(doelen.get(i)), 2.5, 40, 1.3, "worp", true, ROOD);
            }
            speel("worp");
            worpCd = 100;
            rustT = 30;
            laatsteAanval = "worp";
            return;
        }
        rustT = 10;
    }

    private void inslag(ServerLevel sl, Vec3 plek, double r, int duur, double factor, String skill, boolean bevriest, int kleur) {
        Telegraaf.cirkel(sl, plek, r, duur, kleur);
        inslagen.add(new Inslag(plek, r, new int[1], duur, factor, skill, bevriest));
    }

    /** A shard thrown from an image or from her during the split (same look, so it gives nothing away). */
    void worpVan(ServerLevel sl, LivingEntity van) {
        List<ServerPlayer> l = strijders();
        if (l.isEmpty()) {
            return;
        }
        ServerPlayer p = l.get(getRandom().nextInt(l.size()));
        inslag(sl, voetVan(p), 2.2, 35, 0.8, "beeldworp", false, ROOD);
        sl.sendParticles(ParticleTypes.END_ROD, van.getX(), van.getY() + 1.8, van.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
    }

    private void blink(ServerLevel sl, Vec3 naar) {
        sl.sendParticles(PAARS_STOF, getX(), getY() + 1.2, getZ(), 40, 0.4, 0.8, 0.4, 0);
        teleportTo(naar.x, naar.y, naar.z);
        getNavigation().stop();
        sl.sendParticles(PAARS_STOF, naar.x, naar.y + 1.2, naar.z, 40, 0.4, 0.8, 0.4, 0);
        sl.playSound(null, BlockPos.containing(naar), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.4f, 1.3f);
    }

    /** Spiegeling: she and her images on a ring; only she has no shadow. */
    public void splits(ServerLevel sl) {
        eindeSplit(sl, false);
        int n = 2 + fase();
        Vec3 c = zMidden();
        double off = getRandom().nextDouble() * Math.PI * 2;
        int echt = getRandom().nextInt(n + 1);
        for (int i = 0; i <= n; i++) {
            double a = off + i * Math.PI * 2 / (n + 1);
            Vec3 v = new Vec3(c.x + Math.cos(a) * 8, c.y, c.z + Math.sin(a) * 8);
            if (i == echt) {
                blink(sl, v);
                continue;
            }
            Spiegelbeeld b = Z2BazenRegistry.SPIEGELBEELD.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (b != null) {
                b.snapTo(v.x, v.y, v.z, getYRot(), 0);
                b.koppel(this);
                sl.addFreshEntity(b);
                beelden.add(b.getUUID());
                sl.sendParticles(PAARS_STOF, v.x, v.y + 1.2, v.z, 30, 0.4, 0.8, 0.4, 0);
            }
        }
        entityData.set(GESPLITST, true);
        splitT = SPLIT_TICKS;
        splitSchade = 0;
        eigenWorpT = 50 + getRandom().nextInt(60);
        splitCd = 520;
        rustT = 40;
        splitsingen++;
        speel("spiegel");
        zeg("vadsrijk.z2bazen.sz.split", ChatFormatting.LIGHT_PURPLE);
        laatsteAanval = "split";
    }

    public List<Spiegelbeeld> beelden(ServerLevel sl) {
        List<Spiegelbeeld> l = new ArrayList<>();
        for (java.util.UUID id : beelden) {
            if (sl.getEntity(id) instanceof Spiegelbeeld b && !b.isRemoved()) {
                l.add(b);
            }
        }
        return l;
    }

    private void eindeSplit(ServerLevel sl, boolean gevonden) {
        for (Spiegelbeeld b : beelden(sl)) {
            b.breek(sl);
        }
        beelden.clear();
        if (gesplitst()) {
            entityData.set(GESPLITST, false);
            if (gevonden) {
                zeg("vadsrijk.z2bazen.sz.gevonden", ChatFormatting.GOLD);
            }
        }
    }

    /** An image was hit: the mirror strikes back at the hitter. */
    void beeldGeraakt(ServerLevel sl, Spiegelbeeld b, ServerPlayer p) {
        beeldenGebroken++;
        beelden.remove(b.getUUID());
        zMechaniek(p, 0.2, "spiegelslag");
        Statussen.geef(p, bevroren(), 40, 0.4, this);
        sl.playSound(null, b.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 1.2f);
    }

    private void startStraal(ServerLevel sl) {
        eindeSplit(sl, false);
        poort = getRandom().nextInt(Z2BazenPlekken.HAL_OGEN.size());
        blink(sl, straalPlek(poort));
        actie = Actie.STRAAL;
        actieT = STRAAL_TICKS;
        straalCd = 560;
        rustT = STRAAL_TICKS + 20;
        speel("straal");
        zeg("vadsrijk.z2bazen.sz.straal", ChatFormatting.LIGHT_PURPLE);
        sl.playSound(null, oog(poort), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3f, 0.6f);
        laatsteAanval = "straal";
    }

    /** Gametest: which crystal eye charges. */
    public void zetPoort(int i) {
        poort = Math.floorMod(i, Z2BazenPlekken.HAL_OGEN.size());
    }

    /** The beam's current path (live: turning a mirror changes it). */
    public Lichtstraal.Pad straalPad(ServerLevel sl) {
        return Lichtstraal.volg(sl, oog(poort), oogRichting(poort));
    }

    /** Does this path pass her? */
    public boolean raakt(Lichtstraal.Pad pad, LivingEntity e, double r) {
        for (BlockPos c : pad.cellen()) {
            double dx = e.getX() - (c.getX() + 0.5), dz = e.getZ() - (c.getZ() + 0.5);
            if (dx * dx + dz * dz <= r * r && Math.abs(e.getY() - c.getY()) < 3) {
                return true;
            }
        }
        return false;
    }

    /** The beam fires. Public for the gametest. */
    public void vuurStraal(ServerLevel sl) {
        actie = Actie.GEEN;
        Lichtstraal.Pad pad = straalPad(sl);
        Lichtstraal.toon(sl, pad, ROOD_STOF);
        for (BlockPos c : pad.cellen()) {
            sl.sendParticles(ParticleTypes.END_ROD, c.getX() + 0.5, c.getY() + 0.6, c.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.02);
        }
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 3f, 1.6f);
        for (ServerPlayer p : strijders()) {
            if (raakt(pad, p, 1.0)) {
                zMechaniek(p, 0.45, "spiegelstraal");
            }
        }
        for (Spiegelbeeld b : beelden(sl)) {
            if (raakt(pad, b, 1.3)) {
                b.breek(sl);
            }
        }
        if (raakt(pad, this, 1.6)) {
            straalRaak++;
            eindeSplit(sl, false);
            Statussen.geef(this, Status.VERDOOFD, 100, 1, null);
            super.krijgSchade(maxVads() * 0.08, null);
            sl.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + 1.5, getZ(), 40, 0.4, 0.6, 0.4, 0.2);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 80, 0.8, 1.2, 0.8, 0.3);
            zeg("vadsrijk.z2bazen.sz.verblind", ChatFormatting.GOLD);
        } else {
            straalMis++;
        }
        draaiSpiegels(sl);
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        double voor = vads();
        super.krijgSchade(n, bron);
        if (gesplitst() && !geplofd() && level() instanceof ServerLevel sl) {
            splitSchade += voor - vads();
            if (splitSchade >= maxVads() * GEVONDEN) {
                eindeSplit(sl, true);
                splitT = 0;
            }
        }
    }

    @Override
    protected void actieTick(ServerLevel sl, List<ServerPlayer> l) {
        splitCd--;
        worpCd--;
        straalCd--;
        sprongCd--;
        regenCd--;
        if (!opgezet) {
            opgezet = true;
            herstel(sl);
            zetHal(sl);
        }
        // the light
        if (fase() >= 2) {
            lichtHoek += Math.PI * 2 / (fase() >= 3 ? 180 : 240);
        }
        if (tickCount % 4 == 0) {
            for (Vec3 lr : lichten()) {
                Vec3 o = zMidden().add(lr.scale(18)).add(0, 7, 0);
                sl.sendParticles(LICHT, o.x, o.y, o.z, 6, 0.4, 0.4, 0.4, 0);
                sl.sendParticles(ParticleTypes.END_ROD, o.x, o.y, o.z, 2, 0.3, 0.3, 0.3, 0.01);
            }
        }
        // shards in flight
        for (Iterator<Inslag> it = inslagen.iterator(); it.hasNext(); ) {
            Inslag i = it.next();
            if (++i.t()[0] < i.duur()) {
                continue;
            }
            it.remove();
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TINTED_GLASS.defaultBlockState()), i.plek().x, i.plek().y + 0.3, i.plek().z,
                    30, i.straal() * 0.4, 0.2, i.straal() * 0.4, 0.1);
            sl.playSound(null, BlockPos.containing(i.plek()), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1f, 1.4f);
            zRond(i.plek(), i.straal(), i.factor(), i.skill(), i.bevriest() ? bevroren() : null, 40, 0.3);
        }
        // the split: shadows, her own throws, the timer
        if (gesplitst()) {
            if (tickCount % 3 == 0) {
                for (Spiegelbeeld b : beelden(sl)) {
                    if (verlicht(b.position())) {
                        schaduw(sl, b.position());
                    }
                }
            }
            if (--eigenWorpT <= 0) {
                eigenWorpT = 70 + getRandom().nextInt(50);
                worpVan(sl, this);
            }
            getNavigation().stop();
            if (--splitT <= 0 || beelden(sl).isEmpty()) {
                eindeSplit(sl, false);
            }
        }
        // fase 3: the hall shatters, column by column
        if (zuilT >= 0 && --zuilT <= 0 && zuilen <= Z2BazenPlekken.KOEPEL_STRAAL) {
            zuilT = 30;
            breekKoepel(sl, zuilen);
            zuilen += 2;
        }
        switch (actie) {
            case STRAAL -> {
                actieT--;
                getNavigation().stop();
                if (actieT % 5 == 0) {
                    Lichtstraal.Pad pad = straalPad(sl);
                    Lichtstraal.toon(sl, pad, PAARS_STOF);
                    BlockPos o = oog(poort);
                    sl.sendParticles(ParticleTypes.END_ROD, o.getX() + 0.5, o.getY() + 0.5, o.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
                }
                if (actieT <= 0) {
                    vuurStraal(sl);
                }
            }
            case REGEN -> {
                if (--actieT <= 0) {
                    actie = Actie.GEEN;
                    regen(sl);
                }
            }
            default -> {
            }
        }
    }

    /** The yellow stack circle goes off: everyone inside shares 60 % of a Vads bar. */
    public int regen(ServerLevel sl) {
        List<ServerPlayer> in = new ArrayList<>();
        for (ServerPlayer p : strijders()) {
            if (p.position().multiply(1, 0, 1).distanceTo(regenPlek.multiply(1, 0, 1)) <= 3 && Math.abs(p.getY() - regenPlek.y) < 3) {
                in.add(p);
            }
        }
        for (ServerPlayer p : in) {
            zMechaniek(p, 0.6 / in.size(), "scherfregen");
        }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TINTED_GLASS.defaultBlockState()), regenPlek.x, regenPlek.y + 3, regenPlek.z, 120, 2,
                2, 2, 0.1);
        sl.playSound(null, BlockPos.containing(regenPlek), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
        return in.size();
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        if (!was && level() instanceof ServerLevel sl) {
            eindeSplit(sl, false);
        }
        super.verslagen(laatste);
        if (!was && level() instanceof ServerLevel sl) {
            zeg("vadsrijk.z2bazen.sz.verslagen", ChatFormatting.LIGHT_PURPLE);
            BazenRegistry.later(sl.getServer(), 200, () -> herstel(sl));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED && level() instanceof ServerLevel sl && !geplofd()) {
            for (Spiegelbeeld b : beelden(sl)) {
                b.discard();
            }
            herstel(sl);
        }
        super.remove(reason);
    }

    /** (dev) everything in this box that is a mirror image of hers. */
    public List<Spiegelbeeld> alleBeelden(ServerLevel sl) {
        return sl.getEntitiesOfClass(Spiegelbeeld.class, new AABB(BlockPos.containing(zMidden())).inflate(30), b -> b.vrouwe() == this);
    }
}
