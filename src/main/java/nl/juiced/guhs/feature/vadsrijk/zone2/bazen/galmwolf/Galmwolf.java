package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.galmwolf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
import nl.juiced.guhs.feature.vadsrijk.zone2.bazen.Z2Hulpje;

/**
 * De Galmwolf, world boss of the Galmhellingen (ronde 15), lv 23, respawn 20 min. A huge crystal wolf.
 * <ul>
 *   <li><b>Kristalgalm</b> (the signature): a 3 s howl (the whole arena rings purple). Whoever is not behind a frozen pillar when it
 *       goes off takes 60 % of their Vads (true damage), is Bevroren and blown back: two careless howls = fainted. Every pillar that
 *       shielded someone cracks; a cracked one shatters at the next howl, so the cover breaks one by one. Deal 3 % of his Vads
 *       during the howl to interrupt it (stunned 3 s).</li>
 *   <li>Fase 1: pounces (red circle on the target, preferring players at range: he leaps at them), ice breath (red strip), bites.</li>
 *   <li>Fase 2 (60 %): IJsroep: with fewer than 3 pillars he raises 2 new ones (purple circles first), and summons
 *       Sneeuwknabbelaar pups (max 6).</li>
 *   <li>Fase 3 (30 %): Sneeuwjacht: a blizzard (client fog, white-out), howls come faster.</li>
 * </ul>
 * The pillars are remembered in {@link Herstel} (key {@link #H_PILAREN}): removed 10 s after his defeat, put back fresh at every
 * spawn, and restored at server start.
 */
public class Galmwolf extends Z2Baas {
    public static final String H_PILAREN = "z2_galm_pilaren";
    private static final EntityDataAccessor<Boolean> BLIZZARD = SynchedEntityData.defineId(Galmwolf.class, EntityDataSerializers.BOOLEAN);
    public static final int GALM_TICKS = 60;
    public static final double GALM_FRACTIE = 0.6, ONDERBREEK = 0.03;

    public enum Actie {
        GEEN, GALM, SPRONG, ADEM, ROEP, WELPEN
    }

    private Actie actie = Actie.GEEN;
    private int actieT, galmCd = 80, sprongCd = 40, ademCd = 100, roepCd = 0, welpCd = 200;
    private double galmSchade;
    private Vec3 doelPlek = Vec3.ZERO;
    private final List<Integer> roepPlekken = new ArrayList<>();
    private final List<Vec3> welpPlekken = new ArrayList<>();
    private boolean opgezet;
    /** Gametests/AutoCheck: how many howls went off, how many were interrupted, the last howl's shielded/hit players. */
    public int galmen, onderbroken, laatsteBeschut, laatsteGeraakt;

    public Galmwolf(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(BLIZZARD, false);
    }

    public boolean blizzard() {
        return entityData.get(BLIZZARD);
    }

    public Actie actie() {
        return actie;
    }

    @Override
    public String baasId() {
        return "galmwolf";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"galm", "sprong", "adem"};
    }

    @Override
    protected double basisVads(int level) {
        return 5200 + 200 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return 16 + 2.0 * level;
    }

    @Override
    public int xp() {
        return 3000 + 120 * mobLevel();
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
        return 1.25;
    }

    @Override
    protected boolean bezig() {
        return actie != Actie.GEEN;
    }

    // ---- pillars --------------------------------------------------------------------------------------------------------

    /** The centre (stand level) of pillar i: wereld's 8 frozen pillars (3x3). */
    public BlockPos pilaarBasis(int i) {
        int[] o = Z2BazenPlekken.GALM_PILAREN[i];
        return BlockPos.containing(zMidden()).offset(o[0], 0, o[1]);
    }

    public static int pilaren() {
        return Z2BazenPlekken.GALM_PILAREN.length;
    }

    public static boolean isPilaar(BlockState s) {
        return s.getBlock() instanceof IJspilaarBlok;
    }

    public boolean intact(ServerLevel sl, int i) {
        return isPilaar(sl.getBlockState(pilaarBasis(i).above()));
    }

    public int intact(ServerLevel sl) {
        int n = 0;
        for (int i = 0; i < pilaren(); i++) {
            n += intact(sl, i) ? 1 : 0;
        }
        return n;
    }

    private void voorPilaar(ServerLevel sl, int i, java.util.function.Consumer<BlockPos> wat) {
        BlockPos b = pilaarBasis(i);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy < Z2BazenPlekken.GALM_PILAAR_HOOG; dy++) {
                    wat.accept(b.offset(dx, dy, dz));
                }
            }
        }
    }

    public void zetPilaar(ServerLevel sl, int i) {
        voorPilaar(sl, i, p -> {
            BlockState s = sl.getBlockState(p);
            // air (dev stand-in) or wereld's own ice pillar: it becomes OUR pillar ice (the original comes back after, Herstel)
            if (s.isAir() || isPilaar(s) || s.canBeReplaced() || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE) || s.is(Blocks.ICE)) {
                Herstel.zet(sl, H_PILAREN, p, Z2BazenRegistry.IJSPILAAR.get().defaultBlockState());
            }
        });
        // anyone standing in it is lifted out
        for (ServerPlayer p : strijders()) {
            BlockPos b = pilaarBasis(i);
            if (p.getX() >= b.getX() - 1.3 && p.getX() <= b.getX() + 2.3 && p.getZ() >= b.getZ() - 1.3 && p.getZ() <= b.getZ() + 2.3) {
                p.teleportTo(p.getX(), b.getY() + Z2BazenPlekken.GALM_PILAAR_HOOG, p.getZ());
            }
        }
        Vec3 m = Vec3.atBottomCenterOf(pilaarBasis(i)).add(0, 1, 0);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, m.x, m.y + 1, m.z, 40, 1.2, 3, 1.2, 0.02);
        sl.playSound(null, pilaarBasis(i), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    /** A pillar that shielded someone: whole -> cracked, cracked -> shattered. */
    public void barst(ServerLevel sl, int i) {
        if (!intact(sl, i)) {
            return;
        }
        boolean al = sl.getBlockState(pilaarBasis(i).above()).getValue(IJspilaarBlok.BARST) > 0;
        Vec3 m = Vec3.atBottomCenterOf(pilaarBasis(i)).add(0, 2, 0);
        if (al) {
            voorPilaar(sl, i, p -> {
                if (isPilaar(sl.getBlockState(p))) {
                    Herstel.zet(sl, H_PILAREN, p, Blocks.AIR.defaultBlockState());
                }
            });
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), m.x, m.y + 1, m.z, 120, 1.2, 3, 1.2, 0.2);
            sl.playSound(null, BlockPos.containing(m), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
        } else {
            voorPilaar(sl, i, p -> {
                BlockState s = sl.getBlockState(p);
                if (isPilaar(s)) {
                    sl.setBlock(p, s.setValue(IJspilaarBlok.BARST, 1), 2);
                }
            });
            sl.sendParticles(ParticleTypes.SNOWFLAKE, m.x, m.y, m.z, 25, 0.6, 1, 0.6, 0.05);
            sl.playSound(null, BlockPos.containing(m), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
        }
    }

    /** Which pillar shields this player from his howl (a ray from his head to the player hits pillar ice), or -1. */
    public int afgeschermd(ServerLevel sl, LivingEntity p) {
        Vec3 van = position().add(0, getBbHeight() * 0.7, 0);
        for (Vec3 naar : new Vec3[] {p.getEyePosition(), p.position().add(0, 0.6, 0)}) {
            BlockHitResult h = sl.clip(new ClipContext(van, naar, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (h.getType() == HitResult.Type.BLOCK && isPilaar(sl.getBlockState(h.getBlockPos()))) {
                int best = -1;
                double bd = Double.MAX_VALUE;
                for (int i = 0; i < pilaren(); i++) {
                    BlockPos b = pilaarBasis(i);
                    double d = Math.pow(h.getBlockPos().getX() - b.getX(), 2) + Math.pow(h.getBlockPos().getZ() - b.getZ(), 2);
                    if (d < bd) {
                        bd = d;
                        best = i;
                    }
                }
                return best;
            }
        }
        return -1;
    }

    public static void herstel(ServerLevel sl) {
        Herstel.terug(sl, H_PILAREN);
    }

    // ---- the fight ------------------------------------------------------------------------------------------------------

    @Override
    protected void signatuur() {
        speel("galm");
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.5f, 1.6f);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 2, getZ(), 120, 3, 2, 3, 0.05);
        }
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        speel("galm");
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2f, 1.4f);
        if (fase == 2) {
            kaart("vadsrijk.z2bazen.gw.fase2", 0x9AD8FF);
            roepCd = 0;
            welpCd = 40;
        } else if (fase == 3) {
            kaart("vadsrijk.z2bazen.gw.fase3", 0xFFFFFF);
            entityData.set(BLIZZARD, true);
            galmCd = Math.min(galmCd, 60);
        }
    }

    private int galmPauze() {
        return fase() >= 3 ? 200 : fase() == 2 ? 260 : 320;
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        double voor = vads();
        super.krijgSchade(n, bron);
        if (actie == Actie.GALM && !geplofd()) {
            galmSchade += voor - vads();
            if (galmSchade >= maxVads() * ONDERBREEK) {
                onderbreek();
            }
        }
    }

    private void onderbreek() {
        actie = Actie.GEEN;
        onderbroken++;
        galmCd = galmPauze();
        rustT = 70;
        Statussen.geef(this, Status.VERDOOFD, 60, 1, null);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight(), getZ(), 40, 1, 0.5, 1, 0.3);
            sl.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.HOSTILE, 2f, 0.6f);
        }
        zeg("vadsrijk.z2bazen.gw.onderbroken", ChatFormatting.GOLD);
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        ServerPlayer d = zDoel();
        if (galmCd <= 0) {
            startGalm(sl);
            return;
        }
        if (fase() >= 2 && roepCd <= 0 && intact(sl) < 3) {
            roepPlekken.clear();
            List<Integer> weg = new ArrayList<>();
            for (int i = 0; i < pilaren(); i++) {
                if (!intact(sl, i)) {
                    weg.add(i);
                }
            }
            java.util.Collections.shuffle(weg, new java.util.Random(getRandom().nextLong()));
            for (int i = 0; i < Math.min(2, weg.size()); i++) {
                roepPlekken.add(weg.get(i));
                Telegraaf.cirkel(sl, Vec3.atBottomCenterOf(pilaarBasis(weg.get(i))), 2.2, 40, PAARS);
            }
            start(Actie.ROEP, 40);
            roepCd = 400;
            speel("galm");
            zeg("vadsrijk.z2bazen.gw.roep", ChatFormatting.AQUA);
            laatsteAanval = "roep";
            return;
        }
        if (fase() >= 2 && welpCd <= 0 && welpen(sl) < 6) {
            welpPlekken.clear();
            for (int i = 0; i < 3; i++) {
                ServerPlayer p = l.get(getRandom().nextInt(l.size()));
                Vec3 v = voetVan(p).add(getRandom().nextGaussian() * 3, 0, getRandom().nextGaussian() * 3);
                welpPlekken.add(v);
                Telegraaf.cirkel(sl, v, 1.2, 30, PAARS);
            }
            start(Actie.WELPEN, 30);
            welpCd = 500;
            laatsteAanval = "welpen";
            return;
        }
        if (sprongCd <= 0 && (iemandOpAfstand() || getRandom().nextBoolean())) {
            ServerPlayer v = iemandOpAfstand() ? afstandsDoel() : d;
            if (v != null) {
                doelPlek = voetVan(v);
                Telegraaf.cirkel(sl, doelPlek, 3.5, 25, ROOD);
                start(Actie.SPRONG, 25);
                sprongCd = 140;
                speel("sprong");
                laatsteAanval = "sprong";
                return;
            }
        }
        if (ademCd <= 0 && d != null) {
            Vec3 van = position();
            Vec3 r = d.position().subtract(van).multiply(1, 0, 1);
            r = r.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : r.normalize();
            doelPlek = van.add(r.scale(13));
            Telegraaf.lijn(sl, van, doelPlek, 3.5, 25, ROOD);
            start(Actie.ADEM, 25);
            ademCd = 160;
            speel("adem");
            laatsteAanval = "adem";
            return;
        }
        rustT = 10;
    }

    /** The howl starts charging (public for the gametest). */
    public void startGalm(ServerLevel sl) {
        start(Actie.GALM, GALM_TICKS);
        galmSchade = 0;
        speel("galm");
        Telegraaf.cirkel(sl, zMidden(), zStraal(), GALM_TICKS, PAARS);
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.7f);
        zeg("vadsrijk.z2bazen.gw.galm", ChatFormatting.LIGHT_PURPLE);
        laatsteAanval = "galm";
    }

    private void start(Actie a, int t) {
        actie = a;
        actieT = t;
        getNavigation().stop();
        rustT = t + 20;
    }

    private int welpen(ServerLevel sl) {
        return sl.getEntitiesOfClass(Z2Hulpje.class, getBoundingBox().inflate(40), h -> !h.geplofd()).size();
    }

    @Override
    protected void actieTick(ServerLevel sl, List<ServerPlayer> l) {
        galmCd--;
        sprongCd--;
        ademCd--;
        roepCd--;
        welpCd--;
        if (!opgezet) {
            opgezet = true;
            herstel(sl);
            for (int i = 0; i < pilaren(); i++) {
                zetPilaar(sl, i);
            }
        }
        if (blizzard() && tickCount % 4 == 0) {
            for (ServerPlayer p : l) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 2, p.getZ(), 30, 6, 2, 6, 0.15);
            }
        }
        if (actie == Actie.GEEN) {
            return;
        }
        actieT--;
        switch (actie) {
            case GALM -> {
                if (actieT % 10 == 0) {
                    sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight(), getZ(), 12, 0.6, 0.4, 0.6, 0.05);
                    getNavigation().stop();
                }
                if (actieT <= 0) {
                    galm(sl);
                }
            }
            case SPRONG -> {
                if (actieT == 0) {
                    zSpring(doelPlek, 14, 4);
                } else if (actieT < -40) {
                    actie = Actie.GEEN;   // (safety: landed elsewhere)
                }
            }
            case ADEM -> {
                if (actieT <= 0) {
                    Vec3 van = position();
                    for (double s = 0; s <= 13; s += 1) {
                        Vec3 v = van.add(doelPlek.subtract(van).normalize().scale(s));
                        sl.sendParticles(ParticleTypes.SNOWFLAKE, v.x, v.y + 1, v.z, 6, 0.6, 0.4, 0.6, 0.05);
                    }
                    for (ServerPlayer p : l) {
                        if (inStrook(p.position(), van, doelPlek, 1.75)) {
                            zRaak(p, 1.3, "adem");
                            Statussen.geef(p, bevroren(), 60, 0.4, this);
                        }
                    }
                    sl.playSound(null, blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
                    actie = Actie.GEEN;
                }
            }
            case ROEP -> {
                if (actieT <= 0) {
                    for (int i : roepPlekken) {
                        zetPilaar(sl, i);
                    }
                    actie = Actie.GEEN;
                }
            }
            case WELPEN -> {
                if (actieT <= 0) {
                    for (Vec3 v : welpPlekken) {
                        Z2Hulpje h = Z2BazenRegistry.WELP.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
                        if (h != null) {
                            h.snapTo(v.x, v.y, v.z, getRandom().nextFloat() * 360, 0);
                            h.koppel(this);
                            sl.addFreshEntity(h);
                            sl.sendParticles(ParticleTypes.SNOWFLAKE, v.x, v.y + 0.5, v.z, 20, 0.4, 0.4, 0.4, 0.05);
                        }
                    }
                    actie = Actie.GEEN;
                }
            }
            default -> actie = Actie.GEEN;
        }
    }

    /** The howl goes off. */
    public void galm(ServerLevel sl) {
        actie = Actie.GEEN;
        galmen++;
        galmCd = galmPauze();
        rustT = 30;
        Set<Integer> beschut = new HashSet<>();
        laatsteBeschut = 0;
        laatsteGeraakt = 0;
        for (ServerPlayer p : strijders()) {
            int i = afgeschermd(sl, p);
            if (i >= 0) {
                beschut.add(i);
                laatsteBeschut++;
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("vadsrijk.z2bazen.gw.beschut").withStyle(ChatFormatting.AQUA));
            } else {
                laatsteGeraakt++;
                zMechaniek(p, GALM_FRACTIE, "galm");
                Statussen.geef(p, bevroren(), 80, 0.5, this);
                duw(p, position(), 1.3);
            }
        }
        for (int i : beschut) {
            barst(sl, i);
        }
        for (int r = 2; r <= zStraal(); r += 3) {
            int n = r * 4;
            for (int k = 0; k < n; k++) {
                double a = Math.PI * 2 * k / n;
                sl.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(a) * r, getY() + 1.2, getZ() + Math.sin(a) * r, 1, 0, 0.2, 0, 0.01);
            }
        }
        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 2, getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3f, 1.3f);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3f, 0.5f);
    }

    @Override
    protected void zGeland() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        actie = Actie.GEEN;
        zRond(position(), 3.5, 1.6, "sprong", bevroren(), 40, 0.9);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()), getX(), getY() + 0.2, getZ(), 60, 2, 0.2, 2, 0.1);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 1.4f);
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && level() instanceof ServerLevel sl) {
            entityData.set(BLIZZARD, false);
            zeg("vadsrijk.z2bazen.gw.verslagen", ChatFormatting.AQUA);
            BazenRegistry.later(sl.getServer(), 200, () -> herstel(sl));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.DISCARDED && level() instanceof ServerLevel sl && !geplofd()) {
            herstel(sl);   // a reset: the pillars go; the fresh wolf puts them back
        }
        super.remove(reason);
    }
}
