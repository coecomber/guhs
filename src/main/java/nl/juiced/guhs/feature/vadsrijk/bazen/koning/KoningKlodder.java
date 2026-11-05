package nl.juiced.guhs.feature.vadsrijk.bazen.koning;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenBaas;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;

/**
 * Koning Klodder, world boss of the Klodderweide (ronde 7), lv ~8, shared, HP scales with the players in the arena.
 * <ul>
 *   <li>Fase 1: jumps at you (red shadow = landing spot) and a shock wave (red circle around him).</li>
 *   <li>Fase 2 (60 %): he splits into 3 Kroonklodders (roze/blauw/geel). Beat all three within {@link SplitRegel#VENSTER} of the
 *       first one: he re-forms at 35 % (= fase 3). Too slow: the survivors melt back, he heals 8 % per survivor and splits
 *       again 20 s later.</li>
 *   <li>Fase 3 (35 %): jumps + shock waves, and every ~22 s the Hemelsprong: he leaps sky-high, the whole meadow becomes his
 *       shadow except 3 pink safe spots; whoever stands in the shadow when he lands gets flattened (55 % of their Vads).</li>
 * </ul>
 */
public class KoningKlodder extends BazenBaas {
    private static final EntityDataAccessor<Boolean> GESPLITST = SynchedEntityData.defineId(KoningKlodder.class, EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions SCHADUW = new DustParticleOptions(0x2A1A30, 2.2f);

    private enum Doet {
        RUST, MIKT, SPRINGT, SCHOK, HEMEL_OP, HEMEL_NEER
    }

    private Doet doet = Doet.RUST;
    private int timer = 40;
    private Vec3 landing = Vec3.ZERO;
    @Nullable
    private Vec3 spawnPlek;
    @Nullable
    private SplitRegel regel;
    private final UUID[] klodders = new UUID[3];
    private long splitOpnieuw = -1;
    private int hemelOver = -1;
    private final List<Vec3> veilig = new ArrayList<>();
    private int zet, kroonT;
    private boolean nogEenSprong, kroonAlGevlogen, opgeruimd;
    @Nullable
    private UUID kroon, kroonDrager;

    public KoningKlodder(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GESPLITST, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32f));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public String baasId() {
        return "koning_klodder";
    }

    @Override
    protected String[] actieAnimaties() {
        return new String[] {"spring", "land", "brul"};
    }

    @Override
    protected double basisVads(int level) {
        return 2000 + 200 * level;
    }

    @Override
    protected double basisSchade(int level) {
        return 10 + 2.5 * level;
    }

    @Override
    public int xp() {
        return 600 + 60 * mobLevel();
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    protected double[] fases() {
        return new double[] {0.6, 0.35};
    }

    public boolean gesplitst() {
        return entityData.get(GESPLITST);
    }

    @Nullable
    public SplitRegel regel() {
        return regel;
    }

    @Override
    protected void signatuur() {
        speel("brul");
        setDeltaMovement(0, 1.0, 0);
        doet = Doet.RUST;
        timer = 60;
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 3f, 0.3f);
        }
    }

    Vec3 midden() {
        if (arena != null) {
            return Vec3.atBottomCenterOf(arena.centrum());
        }
        return spawnPlek != null ? spawnPlek : position();
    }

    double straal() {
        return arena != null ? arena.straal() : 20;
    }

    List<ServerPlayer> spelers(ServerLevel sl) {
        Vec3 m = midden();
        double r = straal() + 4;
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(m.x, p.getY(), m.z) <= r * r && Math.abs(p.getY() - m.y) < 30) {
                l.add(p);
            }
        }
        return l;
    }

    private void zeg(ServerLevel sl, Component c) {
        for (ServerPlayer p : spelers(sl)) {
            p.sendOverlayMessage(c);
        }
    }

    @Override
    protected void opFase(int fase) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 1.6f);
        speel("brul");
        if (fase == 2) {
            slijmVijvers(sl, 0.5);
            splits(sl);
        } else if (fase == 3) {
            slijmVijvers(sl, 1.0);
            hemelOver = 160;
            kroonAf(sl);
        }
    }

    // ---- the split --------------------------------------------------------------------------------------------------------

    /** Splits into the three Kroonklodders now (public for the dev command and the gametest). */
    public void splits(ServerLevel sl) {
        if (gesplitst() || geplofd()) {
            return;
        }
        entityData.set(GESPLITST, true);
        setInvisible(true);
        doet = Doet.RUST;
        regel = new SplitRegel(sl.getGameTime());
        Element[] el = {Element.ROZE, Element.WATER, Element.VUUR};
        double elk = maxVads() * 0.25 / 3;
        int schildIndex = 1 + getRandom().nextInt(2);   // the blue (Water) or the yellow (Vuur) one: breaks on its weak element
        for (int i = 0; i < 3; i++) {
            ElementKlodder k = BazenRegistry.KK_KLODDER.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
            if (k == null) {
                continue;
            }
            double a = Math.PI * 2 * i / 3;
            k.snapTo(getX(), getY() + 1.5, getZ(), (float) Math.toDegrees(a), 0);
            k.stelIn(mobLevel(), el[i], false, Set.of());
            k.koppel(this, i, elk);
            if (i == schildIndex) {
                k.zetSchild(true);
            }
            k.setDeltaMovement(Math.cos(a) * 0.8, 0.7, Math.sin(a) * 0.8);
            sl.addFreshEntity(k);
            klodders[i] = k.getUUID();
        }
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1.5, getZ(), 120, 2, 1.5, 2, 0.3);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 3f, 0.4f);
        zeg(sl, Component.translatable("vadsrijk.bazen.kk.splitst").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    void klodderVerslagen(int i) {
        if (regel != null) {
            regel.verslagen(i, level().getGameTime());
        }
    }

    /** Checks the split rule at this game time (the tick calls it with the real time; the gametest with a later one). */
    public void splitControle(long nu) {
        if (!gesplitst() || regel == null || !(level() instanceof ServerLevel sl)) {
            return;
        }
        SplitRegel.Uitslag u = regel.uitslag(nu);
        if (u == SplitRegel.Uitslag.BEZIG) {
            int r = regel.resterend(nu);
            if (r >= 0 && r % 20 == 0) {
                zeg(sl, Component.translatable("vadsrijk.bazen.kk.venster", r / 20).withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        herenig(sl, u == SplitRegel.Uitslag.GELUKT, nu);
    }

    private void herenig(ServerLevel sl, boolean gelukt, long nu) {
        int over = regel == null ? 3 : regel.overlevers();
        for (UUID id : klodders) {
            if (id != null && sl.getEntity(id) instanceof ElementKlodder k && !k.geplofd()) {
                k.smelt(this);
            }
        }
        java.util.Arrays.fill(klodders, null);
        regel = null;
        entityData.set(GESPLITST, false);
        setInvisible(false);
        speel("land");
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 3f, 0.5f);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1.5, getZ(), 80, 2, 1.5, 2, 0.2);
        if (gelukt) {
            zeg(sl, Component.translatable("vadsrijk.bazen.kk.gelukt").withStyle(ChatFormatting.GREEN));
            double doel = maxVads() * 0.35;
            if (vads() > doel) {
                super.krijgSchade(vads() - doel + 0.01, null);
            }
            Statussen.geef(this, Status.VERDOOFD, 60, 1, null);
            splitOpnieuw = -1;
        } else {
            zeg(sl, Component.translatable("vadsrijk.bazen.kk.mislukt", over).withStyle(ChatFormatting.RED));
            heal(maxVads() * 0.08 * over);
            splitOpnieuw = nu + 400;
        }
        timer = 50;
    }

    @Override
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (gesplitst() || doet == Doet.HEMEL_OP || doet == Doet.HEMEL_NEER) {
            return;
        }
        super.krijgSchade(n, bron);
    }

    @Override
    public boolean isPickable() {
        return !gesplitst() && super.isPickable();
    }

    @Override
    public void naamplaatje() {
        super.naamplaatje();
        if (!level().isClientSide() && gesplitst()) {
            setCustomNameVisible(false);
        }
    }

    // ---- the fight ---------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (spawnPlek == null) {
            spawnPlek = position();
            if (!opgeruimd) {   // a fresh king: whatever an earlier fight turned to slime is a pond again
                opgeruimd = true;
                herstel(sl);
            }
        }
        if (geplofd()) {
            return;
        }
        long nu = sl.getGameTime();
        kroonTick(sl);
        if (tickCount % 10 == 0 && Herstel.heeft(sl, H_SLIJM)) {   // the slimed ponds stick
            for (ServerPlayer p : vechters()) {
                if (sl.getBlockState(p.blockPosition().below()).is(Blocks.SLIME_BLOCK) || sl.getBlockState(p.blockPosition()).is(Blocks.SLIME_BLOCK)) {
                    Statussen.geef(p, Status.PLAKKERIG, 30, 0.4, this);
                }
            }
        }
        if (gesplitst()) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            splitControle(nu);
            return;
        }
        if (stil() && doet != Doet.HEMEL_OP && doet != Doet.HEMEL_NEER) {
            return;
        }
        if (fase() == 1 && !kroonAlGevlogen && vads() < maxVads() * 0.8) {
            kroonAlGevlogen = true;
            kroonAf(sl);
        }
        if (fase() == 2 && splitOpnieuw > 0 && nu >= splitOpnieuw && doet == Doet.RUST && !inSprong()) {
            splitOpnieuw = -1;
            splits(sl);
            return;
        }
        if (inSprong() || nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.loopt(this)) {   // (T-VFX: a bouwdoos attack runs: wait)
            return;
        }
        timer--;
        if (hemelOver > 0) {
            hemelOver--;
        }
        switch (doet) {
            case RUST -> {
                if (onGround()) {
                    setDeltaMovement(getDeltaMovement().multiply(0.5, 1, 0.5));
                }
                if (timer > 0) {
                    break;
                }
                ServerPlayer t = volgendDoel();
                ServerPlayer ver = afstandsSpeler();
                int vfx = fase() >= 3 ? 0 : nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.probeer(this, sl);   // T-VFX: crown rain / the royal bath (not in the sky phase)
                if (vfx > 0) {
                    timer = vfx;
                    break;
                }
                zet++;
                boolean dichtbij = false;
                for (ServerPlayer p : vechters()) {
                    dichtbij |= p.distanceToSqr(this) < 7.5 * 7.5;
                }
                if (fase() >= 3 && hemelOver == 0) {
                    hemelOp(sl);
                } else if (t == null) {
                    timer = 10;
                } else if (ver != null && zet % 3 == 0) {
                    // the far ones get a rain of klodderspat: 3 blobs around them
                    for (int i = 0; i < 3; i++) {
                        lob(ver, 2.6, 22 + i * 6, 1.2, "bazen_klodderspat", Status.PLAKKERIG, 40, new net.minecraft.core.particles.ItemParticleOption(
                                ParticleTypes.ITEM, net.minecraft.world.item.Items.SLIME_BALL), Telegraaf.ROOD);
                    }
                    timer = 40;
                } else if (dichtbij && zet % 3 == 1) {
                    doet = Doet.SCHOK;   // shock wave: get out of the red circle around him
                    timer = fase() >= 3 ? 24 : 32;
                    speel("brul");
                    Telegraaf.cirkel(sl, position(), 8, timer, Telegraaf.ROOD);
                    nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.cast(this, "grote_plof", timer);   // T-VFX: the cast bar names it
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2.5f, 0.3f);
                } else {
                    doet = Doet.MIKT;   // the jump: his shadow = the landing spot; in fase 3 two in a row
                    timer = fase() >= 2 ? 22 : 30;
                    landing = voetenVan(t);
                    nogEenSprong = fase() >= 3;
                    Telegraaf.cirkel(sl, landing, 4.5, timer + 16, Telegraaf.ROOD);
                    nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.windup(this, timer);   // T-VFX: he crouches for the jump
                }
            }
            case MIKT -> {
                if (timer <= 0) {
                    speel("spring");
                    springNaar(landing, 16, 6);
                    doet = Doet.SPRINGT;
                    sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 3f, 0.4f);
                }
            }
            case SPRINGT -> {
                doet = Doet.RUST;
                timer = 20;
            }
            case SCHOK -> {
                if (timer <= 0) {
                    plet(sl, 8, 1.5);
                    doet = Doet.RUST;
                    timer = 40;
                }
            }
            case HEMEL_OP -> {
                if (timer > 64) {
                    setDeltaMovement(0, 1.4, 0);
                } else {
                    setDeltaMovement(0, 0, 0);
                }
                schaduw(sl);
                if (timer <= 0) {
                    doet = Doet.HEMEL_NEER;
                    Vec3 m = midden();
                    snapTo(m.x, getY(), m.z, getYRot(), 0);
                    setNoGravity(false);
                    setDeltaMovement(0, -3, 0);
                    timer = 60;
                }
            }
            case HEMEL_NEER -> {
                setDeltaMovement(0, Math.min(getDeltaMovement().y, -2.5), 0);
                if (onGround() || timer <= 0) {
                    hemelLand(sl);
                }
            }
        }
    }

    @Override
    protected void geland() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        plet(sl, 4.6, 2.0);
        ServerPlayer t = nogEenSprong ? volgendDoel() : null;
        nogEenSprong = false;
        if (t != null) {   // fase 3: straight into a second jump (shorter warning)
            doet = Doet.MIKT;
            timer = 16;
            landing = voetenVan(t);
            Telegraaf.cirkel(sl, landing, 4.5, timer + 16, Telegraaf.ROOD);
        } else {
            doet = Doet.RUST;
            timer = fase() >= 2 ? 36 : 48;
        }
    }

    // ---- the crown (fix round 1): it flies off and bounces through the meadow; carry it back and throw it: he's dizzy -------

    /** Knocks the crown off: a golden crown bounces around the arena for {@link #KROON_TICKS}. */
    public void kroonAf(ServerLevel sl) {
        if (kroon != null && sl.getEntity(kroon) instanceof net.minecraft.world.entity.item.ItemEntity) {
            return;
        }
        net.minecraft.world.entity.item.ItemEntity i = new net.minecraft.world.entity.item.ItemEntity(sl, getX(), getY() + getBbHeight() + 0.5, getZ(),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_HELMET));
        i.setNeverPickUp();
        i.setUnlimitedLifetime();
        i.setGlowingTag(true);
        double a = getRandom().nextDouble() * Math.PI * 2;
        i.setDeltaMovement(Math.cos(a) * 0.6, 0.8, Math.sin(a) * 0.6);
        sl.addFreshEntity(i);
        kroon = i.getUUID();
        kroonT = KROON_TICKS;
        kroonDrager = null;
        sl.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.HOSTILE, 2f, 0.6f);
        zeg(sl, Component.translatable("vadsrijk.bazen.kk.kroon_af").withStyle(ChatFormatting.GOLD));
    }

    public static final int KROON_TICKS = 20 * 20;

    @Nullable
    public ServerPlayer kroonDrager() {
        return kroonDrager != null && level() instanceof ServerLevel sl && sl.getEntity(kroonDrager) instanceof ServerPlayer p ? p : null;
    }

    private void kroonTick(ServerLevel sl) {
        ServerPlayer drager = kroonDrager();
        if (drager != null) {
            if (tickCount % 4 == 0) {
                sl.sendParticles(ParticleTypes.WAX_ON, drager.getX(), drager.getY() + 2.3, drager.getZ(), 3, 0.25, 0.1, 0.25, 0);
            }
            if (!inGevecht(drager) || --kroonT <= 0) {
                kroonTerug(sl, false);
            } else if (drager.distanceToSqr(this) < 3.5 * 3.5) {
                gooiKroon(drager);   // (walked it right up to him: that counts too)
            }
            return;
        }
        if (kroon == null) {
            return;
        }
        if (!(sl.getEntity(kroon) instanceof net.minecraft.world.entity.item.ItemEntity i) || i.isRemoved()) {
            kroon = null;
            return;
        }
        if (tickCount % 3 == 0) {
            sl.sendParticles(ParticleTypes.WAX_ON, i.getX(), i.getY() + 0.5, i.getZ(), 2, 0.2, 0.2, 0.2, 0);
        }
        if (i.onGround() && tickCount % 12 == 0) {   // it bounces on, towards somewhere in the arena
            Vec3 m = midden();
            Vec3 d = m.subtract(i.position()).multiply(1, 0, 1);
            double a = getRandom().nextDouble() * Math.PI * 2;
            Vec3 v = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(0.35);
            if (d.length() > straal() * 0.7) {
                v = d.normalize().scale(0.35);
            }
            i.setDeltaMovement(v.x, 0.55, v.z);
            sl.playSound(null, i.blockPosition(), SoundEvents.SLIME_JUMP_SMALL, SoundSource.HOSTILE, 0.8f, 1.6f);
        }
        for (ServerPlayer p : vechters()) {
            if (p.distanceToSqr(i) < 1.8 * 1.8) {
                i.discard();
                kroon = null;
                kroonDrager = p.getUUID();
                kroonT = 20 * 15;
                p.sendOverlayMessage(Component.translatable("vadsrijk.bazen.kk.kroon_pak").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                sl.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.PLAYERS, 1.5f, 1.2f);
                return;
            }
        }
        if (--kroonT <= 0) {
            kroonTerug(sl, false);
        }
    }

    /** The carrier throws the crown at him (right-click, see {@link nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry}): dizzy 4 s. */
    public boolean gooiKroon(ServerPlayer p) {
        if (!(level() instanceof ServerLevel sl) || kroonDrager == null || !kroonDrager.equals(p.getUUID())) {
            return false;
        }
        kroonDrager = null;
        Vec3 van = p.getEyePosition(), naar = position().add(0, getBbHeight(), 0);
        for (int s = 0; s <= 12; s++) {
            Vec3 v = van.lerp(naar, s / 12.0).add(0, Math.sin(Math.PI * s / 12.0) * 2, 0);
            sl.sendParticles(ParticleTypes.WAX_ON, v.x, v.y, v.z, 2, 0.05, 0.05, 0.05, 0);
        }
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.2f, 1.6f);
        sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight(), getZ(), 40, 1, 0.5, 1, 0.3);
        if (!gesplitst() && doet != Doet.HEMEL_OP && doet != Doet.HEMEL_NEER) {
            Statussen.geef(this, Status.VERDOOFD, 80, 1, p);
            doet = Doet.RUST;
            timer = 20;
        }
        zeg(sl, Component.translatable("vadsrijk.bazen.kk.kroon_raak", p.getDisplayName()).withStyle(ChatFormatting.GREEN));
        return true;
    }

    private void kroonTerug(ServerLevel sl, boolean stil) {
        if (kroon != null && sl.getEntity(kroon) instanceof net.minecraft.world.entity.item.ItemEntity i) {
            i.discard();
        }
        kroon = null;
        kroonDrager = null;
        if (!stil) {
            sl.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + getBbHeight(), getZ(), 20, 0.6, 0.3, 0.6, 0);
            sl.playSound(null, blockPosition(), SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.HOSTILE, 1.5f, 0.8f);
        }
    }

    // ---- the meadow changes per phase (fix round 1): the ponds turn to slime (Plakkerig), restored after the fight ----------

    public static final String H_SLIJM = "kk_slijm";

    private void slijmVijvers(ServerLevel sl, double deel) {
        Vec3 m = midden();
        int r = (int) straal(), n = 0;
        net.minecraft.core.BlockPos c = net.minecraft.core.BlockPos.containing(m);
        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(c.offset(-r, -4, -r), c.offset(r, 3, r))) {
            double dx = p.getX() + 0.5 - m.x, dz = p.getZ() + 0.5 - m.z;
            if (dx * dx + dz * dz > (double) r * r || n > 600) {
                continue;
            }
            if (sl.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER) && sl.getBlockState(p.above()).isAir()
                    && getRandom().nextDouble() < deel) {
                Herstel.zet(sl, H_SLIJM, p, Blocks.SLIME_BLOCK.defaultBlockState());
                if (n++ % 4 == 0) {
                    sl.sendParticles(ParticleTypes.ITEM_SLIME, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.02);
                }
            }
        }
        if (n > 0) {
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE, 2f, 0.5f);
            zeg(sl, Component.translatable("vadsrijk.bazen.kk.slijm").withStyle(ChatFormatting.GREEN));
        }
    }

    /** Everything the fight changed in the meadow back (the ponds), and the crown away. */
    public static void herstel(ServerLevel sl) {
        Herstel.terug(sl, H_SLIJM);
    }

    private void plet(ServerLevel sl, double r, double factor) {
        speel("land");
        nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx.treffer(this, position(), r, 0x8AD0FF);   // T-VFX: the shockwave of the landing
        nl.juiced.guhs.feature.vadsrijk.vfx.Vfx.deeltjes(sl, nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje.SLIJM, position().add(0, 0.4, 0), 30, 0x8AD0FF, r * 0.4, 0.3);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 3f, 0.4f);
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 0.7f);
        for (int i = 0; i < 40; i++) {
            double a = Math.PI * 2 * i / 40;
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX() + Math.cos(a) * r, getY() + 0.3, getZ() + Math.sin(a) * r, 2, 0.1, 0.1, 0.1, 0.05);
        }
        for (ServerPlayer p : vechters()) {
            double dx = p.getX() - getX(), dz = p.getZ() - getZ();
            if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - getY()) < 3) {
                Schade.doe(new Schade.Klap(this, p, basisSchade(mobLevel()) * factor, element(), Schade.Soort.MELEE, false, "bazen_plet"));
                Vec3 weg = new Vec3(dx, 0, dz).normalize();
                p.setDeltaMovement(weg.x, 0.6, weg.z);
                p.hurtMarked = true;
            }
        }
    }

    private void hemelOp(ServerLevel sl) {
        doet = Doet.HEMEL_OP;
        timer = 80;
        speel("spring");
        setNoGravity(true);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 4f, 0.25f);
        veilig.clear();
        Vec3 m = midden();
        double r = straal() * 0.65;
        for (int tries = 0; veilig.size() < 3 && tries < 60; tries++) {
            double a = getRandom().nextDouble() * Math.PI * 2, d = r * (0.3 + 0.7 * getRandom().nextDouble());
            Vec3 v = new Vec3(m.x + Math.cos(a) * d, m.y, m.z + Math.sin(a) * d);
            if (veilig.stream().allMatch(o -> o.distanceTo(v) > 8)) {
                veilig.add(v);
            }
        }
        for (Vec3 v : veilig) {
            Telegraaf.cirkel(sl, v, 3, 82, Telegraaf.ROZE);
            sl.sendParticles(ParticleTypes.END_ROD, v.x, v.y + 0.5, v.z, 20, 1.2, 0.8, 1.2, 0.02);
        }
        Telegraaf.cirkel(sl, m, straal(), 82, Telegraaf.ROOD);
        zeg(sl, Component.translatable("vadsrijk.bazen.kk.hemel").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    }

    private boolean veiligOp(Vec3 p) {
        for (Vec3 v : veilig) {
            double dx = p.x - v.x, dz = p.z - v.z;
            if (dx * dx + dz * dz <= 3.3 * 3.3) {
                return true;
            }
        }
        return false;
    }

    private void schaduw(ServerLevel sl) {
        if (timer % 4 != 0) {
            return;
        }
        Vec3 m = midden();
        double r = straal();
        for (int i = 0; i < 140; i++) {
            double a = getRandom().nextDouble() * Math.PI * 2, d = Math.sqrt(getRandom().nextDouble()) * r;
            Vec3 v = new Vec3(m.x + Math.cos(a) * d, m.y + 0.15, m.z + Math.sin(a) * d);
            if (!veiligOp(v)) {
                sl.sendParticles(SCHADUW, v.x, v.y, v.z, 1, 0.1, 0, 0.1, 0);
            }
        }
    }

    private void hemelLand(ServerLevel sl) {
        doet = Doet.RUST;
        timer = 60;
        hemelOver = 440;
        BazenRegistry.later(sl.getServer(), 30, () -> {
            if (!geplofd() && !isRemoved()) {
                kroonAf(sl);
            }
        });
        speel("land");
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3f, 0.5f);
        sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 4f, 0.3f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 2, 1, 0.5, 1, 0);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 1, getZ(), 200, straal() / 3, 0.5, straal() / 3, 0.3);
        for (ServerPlayer p : vechters()) {
            if (!veiligOp(p.position())) {
                Schade.doe(new Schade.Klap(this, p, Spelers.maxVads(p) * 0.55, element(), Schade.Soort.WAAR, false, "bazen_hemelsprong"));
                p.setDeltaMovement(0, 0.9, 0);
                p.hurtMarked = true;
                p.sendOverlayMessage(Component.translatable("vadsrijk.bazen.kk.hemel_raak").withStyle(ChatFormatting.RED));
            }
        }
        veilig.clear();
    }

    @Override
    protected void opPlof() {
        super.opPlof();
        setNoGravity(false);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 2, getZ(), 250, 3, 2, 3, 0.1);
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 4, getZ(), 30, 2, 1, 2, 0);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 2, getZ(), 120, 2, 2, 2, 0.4);
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 2f, 0.6f);
            kroonTerug(sl, true);
            BazenRegistry.later(sl.getServer(), 200, () -> herstel(sl));
            for (UUID id : klodders) {
                if (id != null && sl.getEntity(id) instanceof ElementKlodder k) {
                    k.discard();
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide() && level() instanceof ServerLevel sl) {
            kroonTerug(sl, true);
            if (reason != RemovalReason.UNLOADED_TO_CHUNK && reason != RemovalReason.UNLOADED_WITH_PLAYER && reason != RemovalReason.CHANGED_DIMENSION
                    && !geplofd()) {
                herstel(sl);   // dev reset / empty-arena reset mid-fight
            }
            for (UUID id : klodders) {
                if (id != null && sl.getEntity(id) instanceof ElementKlodder k) {
                    k.discard();
                }
            }
        }
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        super.readAdditionalSaveData(in);
        setInvisible(false);   // a reload mid-split: he simply stands whole again
        setNoGravity(false);
    }
}
