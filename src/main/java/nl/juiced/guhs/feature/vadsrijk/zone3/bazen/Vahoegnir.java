package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;

/**
 * Vahoegnir, de Vadsloze Draak: the closing boss of part 1 (lv 50, the old nest on the Kruintop). You don't beat him, you cheer
 * him up: during the fight you put the heart splinters back into his chest.
 * <ul>
 *   <li>Fase 1 Grauwe storm (100-75 %): wing wind toward the nest edge (purple), grey fire in red strips, bites.</li>
 *   <li>At 75 / 50 / 25 % he lands and waits: a pink circle glows under his chest. Whoever carries the splinters stands in it 3 s
 *       = a splinter goes back (he roars, a bit more of him turns pink). He can't be hurt until then.</li>
 *   <li>Fase 2 Zwarte pluis (75-50 %): clumps of black fluff rain down (red circles); grey kloddertjes jump out and roll to the egg.
 *       Cheer them up first (the pink zone-1 plof): one that reaches the egg greys it and heals him 3 %.</li>
 *   <li>Fase 3 Kristalhuil (50-25 %): crystal waves over the nest, like the Galmwolf: hide behind the big nest branches (they crack,
 *       then break, one by one). Not behind one = 50 % + blown back.</li>
 *   <li>Fase 4 Het hart (25 %): he takes off and circles; every 15 s he dives at the egg: stand TOGETHER in the yellow circle around
 *       the egg (the egg glows pink and he brakes; the hit is shared). After 3 dives he lands before the egg and hesitates: the
 *       ending ({@link Z3Scenes#vahoegnirEinde}): splinters 4 and 5, he turns pink, the egg hatches into Vahoegje, the pink beam
 *       points at Hoogvads, then the Keizer's throne-room flash.</li>
 *   <li>Enrage (8 min): the whole nest greys.</li>
 * </ul>
 * Not a respawning world boss: it appears when the story (hq14) or the Droomsteen ("herinnering") asks for it
 * ({@link Z3BazenApi#startVahoegnir}).
 */
public class Vahoegnir extends Z3Baas {
    public static final String H_TAKKEN = "z3_nest_takken";
    private static final EntityDataAccessor<Integer> SPLINTERS = SynchedEntityData.defineId(Vahoegnir.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> VLIEGT = SynchedEntityData.defineId(Vahoegnir.class, EntityDataSerializers.BOOLEAN);
    public static final int HUIL_TICKS = 60, SPLINTER_TICKS = 60;
    /** Story fight (hq14) or a Droomsteen memory (repeatable, loot only). */
    public boolean herinnering;
    @Nullable
    private Z3Figurant ei;
    private boolean opgezet, wachtOpSplinter, einde;
    private int splinterNodig;
    private final Map<UUID, Integer> inCirkel = new HashMap<>();
    private int windCd = 100, vuurCd = 50, pluisCd = 60, huilCd = 80, duikCd = 100, duiken, wachtT, huilT = -1;
    /** Gametests: splinters placed through the circle, kloddertjes at the egg, howls, dives held. */
    public int eiGrauw, huilen, duikenGehouden;

    public Vahoegnir(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(SPLINTERS, 0);
        b.define(VLIEGT, false);
    }

    /** How many heart splinters are back (0..5): the texture goes from grey to pink. */
    public int splinters() {
        return entityData.get(SPLINTERS);
    }

    public void zetSplinters(int n) {
        entityData.set(SPLINTERS, Math.max(0, Math.min(5, n)));
    }

    public boolean vliegt() {
        return entityData.get(VLIEGT);
    }

    private void zetVliegt(boolean v) {
        entityData.set(VLIEGT, v);
        setNoGravity(v);
    }

    public boolean wachtOpSplinter() {
        return wachtOpSplinter;
    }

    public boolean inEinde() {
        return einde;
    }

    @Override
    public String baasId() {
        return "vahoegnir";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"brul", "adem", "duik", "land", "buig"};
    }

    @Override
    protected double hp(int level) {
        return 11000 + 380 * level;
    }

    @Override
    protected double klap(int level) {
        return 22 + 2.5 * level;
    }

    @Override
    public int xp() {
        return 9000 + 220 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.VUUR;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.75, 0.5, 0.25};
    }

    @Override
    protected int enrageTicks() {
        return 20 * 60 * 8;
    }

    @Override
    protected void enrage(ServerLevel sl) {
        kaart("vadsrijk.z3bazen.vn.enrage", 0x606070);
    }

    @Override
    protected void enragePuls(ServerLevel sl, List<ServerPlayer> l) {
        for (ServerPlayer p : l) {
            mech(p, 0.12, "grauw_nest");
        }
        sl.sendParticles(new DustParticleOptions(0x5A5A66, 3f), zMidden().x, zMidden().y + 1, zMidden().z, 80, zStraal() * 0.5, 1, zStraal() * 0.5, 0);
    }

    @Override
    protected boolean immuun() {
        return wachtOpSplinter || vliegt() || einde;
    }

    @Override
    protected String immuunKey() {
        return wachtOpSplinter ? "vadsrijk.z3bazen.vn.splinter_eerst" : "vadsrijk.z3bazen.vn.te_hoog";
    }

    @Override
    protected double jaagSnelheid() {
        return wachtOpSplinter ? 0 : 1.0;
    }

    @Override
    protected double bereik() {
        return 4.2;
    }

    @Override
    protected void bijt(ServerLevel sl, ServerPlayer d) {
        if (!wachtOpSplinter && !vliegt()) {
            speel("adem");
            raak(d, 1.1, "beet");
        }
    }

    public Vec3 eiPlek() {
        return punt(Z3BazenPlekken.NEST_EI);
    }

    @Nullable
    public Z3Figurant ei() {
        return ei;
    }

    /** Where the pink splinter circle glows: under his chest (in front of him). */
    public Vec3 splinterPlek() {
        Vec3 r = getLookAngle().multiply(1, 0, 1);
        r = r.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : r.normalize();
        return voetVan(position().add(r.scale(2.6)));
    }

    // ---- nest branches (cover from the crystal howl) ----------------------------------------------------------------------

    public BlockPos tak(int i) {
        return blok(Z3BazenPlekken.NEST_TAKKEN[i]);
    }

    private void voorTak(int i, java.util.function.Consumer<BlockPos> wat) {
        BlockPos b = tak(i);
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                for (int dy = 0; dy < Z3BazenPlekken.NEST_TAK_HOOG; dy++) {
                    wat.accept(b.offset(dx, dy, dz));
                }
            }
        }
    }

    public static boolean isTak(BlockState s) {
        return s.getBlock() instanceof Z3Blokken.Nesttak;
    }

    public boolean takHeel(ServerLevel sl, int i) {
        return isTak(sl.getBlockState(tak(i).above()));
    }

    private void zetTakken(ServerLevel sl) {
        Herstel.terug(sl, H_TAKKEN);
        for (int i = 0; i < Z3BazenPlekken.NEST_TAKKEN.length; i++) {
            voorTak(i, p -> {
                BlockState s = sl.getBlockState(p);
                if (s.isAir() || s.canBeReplaced() || isTak(s)) {
                    Herstel.zet(sl, H_TAKKEN, p, Z3BazenRegistry.NESTTAK.get().defaultBlockState());
                }
            });
        }
    }

    private void barst(ServerLevel sl, int i) {
        if (!takHeel(sl, i)) {
            return;
        }
        boolean al = sl.getBlockState(tak(i).above()).getValue(Z3Blokken.Nesttak.BARST) > 0;
        voorTak(i, p -> {
            BlockState s = sl.getBlockState(p);
            if (isTak(s)) {
                if (al) {
                    Herstel.zet(sl, H_TAKKEN, p, Blocks.AIR.defaultBlockState());
                } else {
                    sl.setBlock(p, s.setValue(Z3Blokken.Nesttak.BARST, 1), 2);
                }
            }
        });
        Vec3 m = Vec3.atBottomCenterOf(tak(i)).add(0.5, 2, 0.5);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STRIPPED_OAK_WOOD.defaultBlockState()), m.x, m.y, m.z, al ? 80 : 25, 0.8, 1.5, 0.8, 0.1);
        sl.playSound(null, BlockPos.containing(m), al ? SoundEvents.WOOD_BREAK : SoundEvents.WOOD_HIT, SoundSource.HOSTILE, 2f, 0.6f);
    }

    /** Which branch shields this player from him (a ray from his head hits branch wood), or -1. */
    public int afgeschermd(ServerLevel sl, LivingEntity p) {
        Vec3 van = position().add(0, getBbHeight() * 0.7, 0);
        for (Vec3 naar : new Vec3[] {p.getEyePosition(), p.position().add(0, 0.6, 0)}) {
            BlockHitResult h = sl.clip(new ClipContext(van, naar, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (h.getType() == HitResult.Type.BLOCK && isTak(sl.getBlockState(h.getBlockPos()))) {
                int best = -1;
                double bd = Double.MAX_VALUE;
                for (int i = 0; i < Z3BazenPlekken.NEST_TAKKEN.length; i++) {
                    double d = h.getBlockPos().distSqr(tak(i));
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

    // ---- the fight ----------------------------------------------------------------------------------------------------------

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        // 75 / 50 / 25 %: he lands, a splinter must go back first
        huilT = -1;
        zetVliegt(false);
        wachtOpSplinter = true;
        splinterNodig = fase - 1;
        wachtT = 0;
        inCirkel.clear();
        getNavigation().stop();
        speel("land");
        kaart("vadsrijk.z3bazen.vn.splinter" + splinterNodig, ROZE);
        zeg("vadsrijk.z3bazen.vn.splinter_cirkel", ChatFormatting.LIGHT_PURPLE);
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2f, 0.8f);
    }

    private void splinterTerug(ServerLevel sl, @Nullable ServerPlayer wie) {
        wachtOpSplinter = false;
        zetSplinters(splinters() + 1);
        speel("brul");
        Vec3 c = position().add(0, getBbHeight() * 0.6, 0);
        sl.sendParticles(new DustParticleOptions(ROZE, 3f), c.x, c.y, c.z, 120, 1.5, 1.2, 1.5, 0.02);
        sl.sendParticles(ParticleTypes.HEART, c.x, c.y + 1.5, c.z, 6, 1, 0.5, 1, 0);
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 0.4, 0.4, 0.4, 0.15);
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3f, 1.4f);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3f, 1.2f);
        kaart("vadsrijk.z3bazen.vn.terug" + splinters(), ROZE);
        if (wie != null) {
            Z3BazenApi.moment(wie, "vahoegnir_splinter_" + splinters());
        }
        if (fase() >= 4) {
            zetVliegt(true);   // fase 4: up he goes, circling
            duikCd = 120;
        }
        rustT = 40;
    }

    @Override
    protected void kiesAanval(ServerLevel sl, List<ServerPlayer> l) {
        if (wachtOpSplinter || einde) {
            rustT = 10;
            return;
        }
        if (fase() >= 4) {
            rustT = 10;   // the dives run from vecht()
            return;
        }
        if (fase() == 3 && huilCd <= 0) {
            huilT = HUIL_TICKS;
            Telegraaf.cirkel(sl, zMidden(), zStraal(), tg(HUIL_TICKS), PAARS);
            speel("brul");
            zeg("vadsrijk.z3bazen.vn.huil", ChatFormatting.LIGHT_PURPLE);
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.8f);
            huilCd = 260;
            rustT = HUIL_TICKS + 30;
            laatsteAanval = "huil";
            return;
        }
        if (fase() == 2 && pluisCd <= 0) {
            int n = Math.min(4, 1 + l.size());
            for (int i = 0; i < n; i++) {
                Vec3 v = i < l.size() && i == 0 ? voetVan(l.get(getRandom().nextInt(l.size()))) : ring(zMidden(), 4 + getRandom().nextDouble() * (zStraal() - 6),
                        getRandom().nextDouble() * Math.PI * 2);
                inslag(v, 2.5, 34, 0.2, "zwarte_pluis", null, new DustParticleOptions(0x1A1A22, 2.5f));
                Vec3 w = v;
                later(tg(34) + 2, () -> {
                    for (int k = 0; k < adds(1); k++) {
                        Z3Hulpje h = Z3Hulpje.maak(sl, Z3BazenRegistry.KLODDERTJE.get(), this, w.add(k * 0.6, 0.2, 0));
                        if (h != null) {
                            h.eiDoel = eiPlek();
                        }
                    }
                });
            }
            zeg("vadsrijk.z3bazen.vn.pluis", ChatFormatting.GRAY);
            pluisCd = 200;
            rustT = 40;
            laatsteAanval = "pluis";
            return;
        }
        if (windCd <= 0 && getRandom().nextInt(3) == 0) {
            Telegraaf.cirkel(sl, position(), 12, tg(30), PAARS);
            later(tg(30), () -> {
                for (ServerPlayer p : inCirkel(position(), 12)) {
                    mech(p, 0.15, "vleugelwind");
                    duw(p, position(), 2.2);
                }
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 80, 6, 0.5, 6, 0.3);
                sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3f, 0.7f);
            });
            windCd = 240;
            rustT = 40;
            laatsteAanval = "wind";
            return;
        }
        if (vuurCd <= 0) {
            ServerPlayer v = iemandOpAfstand() || getRandom().nextBoolean() ? afstandsDoel() : zDoel();
            if (v != null) {
                Vec3 van = position();
                Vec3 r = voetVan(v).subtract(van).multiply(1, 0, 1);
                r = r.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : r.normalize();
                Vec3 naar = voetVan(van.add(r.scale(18)));
                Telegraaf.lijn(sl, van, naar, 3, tg(28), ROOD);
                speel("adem");
                later(tg(28), () -> {
                    for (double s = 0; s <= van.distanceTo(naar); s += 1) {
                        Vec3 p = van.add(naar.subtract(van).normalize().scale(s));
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + 0.8, p.z, 4, 0.5, 0.3, 0.5, 0.02);
                        sl.sendParticles(new DustParticleOptions(0x5A5A66, 2f), p.x, p.y + 1, p.z, 2, 0.5, 0.3, 0.5, 0);
                    }
                    for (ServerPlayer p : strijders()) {
                        if (inStrook(p.position(), van, naar, 1.6)) {
                            mech(p, 0.3, "grauw_vuur");
                            Statussen.geef(p, Status.BRANDEND, 60, 3, this);
                        }
                    }
                    sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, 0.5f);
                });
                vuurCd = 130;
                rustT = 45;
                laatsteAanval = "vuur";
                return;
            }
        }
        rustT = 10;
    }

    @Override
    protected void vecht(ServerLevel sl, List<ServerPlayer> l) {
        windCd--;
        vuurCd--;
        pluisCd--;
        huilCd--;
        duikCd--;
        if (!opgezet) {
            opgezet = true;
            zetTakken(sl);
            ei = Z3Figurant.maak(sl, eiPlek(), 0, "vr_z3_baas_ei", "textures/vadsrijk/z3bazen/ei.png", "vr_z3_baas_ei", "idle", 1.5f);
        }
        if (einde) {
            return;
        }
        // the splinter circle
        if (wachtOpSplinter) {
            wachtT++;
            getNavigation().stop();
            Vec3 c = splinterPlek();
            if (wachtT % 10 == 0) {
                Telegraaf.cirkel(sl, c, 2.5, 10, ROZE);
                sl.sendParticles(new DustParticleOptions(ROZE, 2f), getX(), getY() + getBbHeight() * 0.5, getZ(), 10, 0.6, 0.4, 0.6, 0);
            }
            for (ServerPlayer p : l) {
                double dx = p.getX() - c.x, dz = p.getZ() - c.z;
                if (dx * dx + dz * dz <= 2.5 * 2.5 && Z3BazenApi.HEEFT_SPLINTERS.test(p)) {
                    int t = inCirkel.merge(p.getUUID(), 1, Integer::sum);
                    if (t % 10 == 0) {
                        p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.vn.splinter_bezig", t * 100 / SPLINTER_TICKS).withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    if (t >= SPLINTER_TICKS) {
                        splinterTerug(sl, p);
                        return;
                    }
                } else {
                    inCirkel.remove(p.getUUID());
                }
            }
            if (wachtT % 400 == 0) {   // nobody comes: he roars (not a one-shot, a reminder)
                speel("brul");
                for (ServerPlayer p : l) {
                    mech(p, 0.12, "brul");
                }
                zeg("vadsrijk.z3bazen.vn.splinter_cirkel", ChatFormatting.LIGHT_PURPLE);
            }
            return;
        }
        // the crystal howl
        if (huilT >= 0 && --huilT <= 0) {
            huilT = -1;
            huil(sl);
        }
        // fase 4: circling, diving at the egg
        if (fase() >= 4 && vliegt()) {
            Vec3 m = zMidden();
            double a = tickCount * 0.04;
            Vec3 v = new Vec3(m.x + Math.cos(a) * 14, m.y + 12, m.z + Math.sin(a) * 14);
            setPos(v.x, v.y, v.z);
            setDeltaMovement(Vec3.ZERO);
            if (duikCd <= 0) {
                duikCd = 300;
                duik(sl);
            }
        }
    }

    /** The crystal howl goes off: not behind a branch = 50 % + blown back; shielding branches crack. */
    public void huil(ServerLevel sl) {
        huilen++;
        Set<Integer> beschut = new HashSet<>();
        for (ServerPlayer p : strijders()) {
            int i = afgeschermd(sl, p);
            if (i >= 0) {
                beschut.add(i);
                p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.vn.beschut").withStyle(ChatFormatting.AQUA));
            } else {
                mech(p, 0.5, "kristalhuil");
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
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3f, 1.1f);
    }

    /** A dive at the egg: a yellow stack circle; held together = he brakes, missed = the egg greys and he heals. */
    private void duik(ServerLevel sl) {
        Vec3 e = eiPlek();
        zeg("vadsrijk.z3bazen.vn.duik", ChatFormatting.YELLOW);
        speel("duik");
        stapel(e, 3.5, 70, 0.6, 0.3, 2, "duik", ok -> {
            duiken++;
            Vec3 boven = e.add(0, 6, 0);
            setPos(boven.x, boven.y, boven.z);
            if (ok) {
                duikenGehouden++;
                sl.sendParticles(new DustParticleOptions(ROZE, 3f), e.x, e.y + 1, e.z, 120, 2, 1, 2, 0.02);
                zeg("vadsrijk.z3bazen.vn.remt", ChatFormatting.LIGHT_PURPLE);
            } else {
                eiGrauw++;
                heal(maxVads() * 0.03);
                sl.sendParticles(new DustParticleOptions(0x5A5A66, 2.5f), e.x, e.y + 1, e.z, 60, 1, 0.6, 1, 0);
                zeg("vadsrijk.z3bazen.vn.ei_grauw", ChatFormatting.GRAY);
            }
            sl.playSound(null, e.x, e.y, e.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 3f, 0.6f);
            if (duiken >= 3) {
                land(sl);
            }
        });
    }

    /** After the 3rd dive: he lands before the egg and hesitates. The ending. */
    private void land(ServerLevel sl) {
        einde = true;
        zetVliegt(false);
        Vec3 e = eiPlek(), m = zMidden();
        Vec3 r = m.subtract(e).multiply(1, 0, 1);
        Vec3 voor = e.add(r.lengthSqr() < 0.01 ? new Vec3(0, 0, 6) : r.normalize().scale(6));
        setPos(voor.x, m.y, voor.z);
        getLookControl().setLookAt(e.x, e.y, e.z);
        setYRot((float) Math.toDegrees(Math.atan2(-(e.x - voor.x), e.z - voor.z)));
        setYBodyRot(getYRot());
        speel("land");
        ServerPlayer laatste = strijders().isEmpty() ? null : strijders().get(0);
        Z3Scenes.vahoegnirEinde(sl, this, laatste);
    }

    /** A grey kloddertje reached the egg: the egg greys a little, he heals 3 %. */
    public void kloddertjeBijEi(ServerLevel sl, Z3Hulpje k) {
        eiGrauw++;
        heal(maxVads() * 0.03);
        Vec3 e = eiPlek();
        sl.sendParticles(new DustParticleOptions(0x5A5A66, 2f), e.x, e.y + 0.8, e.z, 30, 0.5, 0.5, 0.5, 0);
        zeg("vadsrijk.z3bazen.vn.klodder_ei", ChatFormatting.GRAY);
    }

    /** The scene's last step: the real defeat (xp, chests, the victory moment). */
    public void opgevrolijkt(@Nullable ServerPlayer wie) {
        einde = false;
        wachtOpSplinter = false;
        verslagen(wie);
    }

    @Override
    protected void opPlof() {
        // no plof: the scene takes over (he turns pink and flies off)
        setNoGravity(true);
    }

    @Override
    protected int plofDuur() {
        return Z3Scenes.EINDE_TICKS + 100;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && geplofd()) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        if (einde) {
            return;   // (only through the ending)
        }
        super.verslagen(laatste);
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        super.krijgSchade(n, bron);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel sl && (reason == RemovalReason.DISCARDED || geplofd())) {
            Herstel.terug(sl, H_TAKKEN);
        }
        if (ei != null && !geplofd()) {
            ei.discard();
        }
        super.remove(reason);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle"), walk = RawAnimation.begin().thenLoop("walk"), vlieg = RawAnimation.begin().thenLoop("vlieg");
        controllers.add(new AnimationController<Vahoegnir>("beweeg", 6,
                state -> state.setAndContinue(state.animatable().vliegt() ? vlieg : state.isMoving() ? walk : idle)));
        AnimationController<Vahoegnir> a = new AnimationController<>("actie", 3, state -> PlayState.STOP);
        for (String n : actieAnimaties()) {
            a.triggerableAnim(n, RawAnimation.begin().thenPlay(n));
        }
        controllers.add(a);
    }
}
