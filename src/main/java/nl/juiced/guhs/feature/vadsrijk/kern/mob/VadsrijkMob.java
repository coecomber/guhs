package nl.juiced.guhs.feature.vadsrijk.kern.mob;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;

/**
 * The base of every Vadsrijk creature (CONTRACT_VADSRIJK 5). It has its own Vads (HP; vanilla health stays full and vanilla
 * damage is ignored: everything goes through {@link Schade}), a level, an element, "ontvadst" (Roze hits it x1.5), elite
 * affixes and a nameplate with an HP bar. Defeat = {@link #verslagen}: xp and personal loot for everyone who helped, the
 * {@link Haken#VERSLAGEN} hook, then the pink plof ({@link #opPlof}): no death animation, it turns pink, squeaks and hops away.
 * <p>Subclasses: give the numbers ({@link #basisVads}, {@link #basisSchade}, {@link #xp}, {@link #lootTabel}), goals, and
 * optionally {@link #aanval} (special attacks) and {@link #opPlof}.
 */
public abstract class VadsrijkMob extends Monster {
    private static final EntityDataAccessor<Float> VADS = SynchedEntityData.defineId(VadsrijkMob.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAX_VADS = SynchedEntityData.defineId(VadsrijkMob.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(VadsrijkMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ELEMENT = SynchedEntityData.defineId(VadsrijkMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ONTVADST = SynchedEntityData.defineId(VadsrijkMob.class, EntityDataSerializers.BOOLEAN);
    /** -1 = alive; otherwise ticks since the plof (synced so the client can tint it pink). */
    private static final EntityDataAccessor<Integer> PLOF = SynchedEntityData.defineId(VadsrijkMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ELITE = SynchedEntityData.defineId(VadsrijkMob.class, EntityDataSerializers.BOOLEAN);

    protected final Set<Affix> affixen = EnumSet.noneOf(Affix.class);
    protected boolean elite;
    /** Damage per player (for xp/loot sharing; bosses keep more in {@code Bijdrage}). */
    protected final Map<UUID, Double> bijdrage = new HashMap<>();
    /** Which spawner made it ({@link Spawners}), or "". */
    public String spawner = "";
    private boolean ingesteld;

    protected VadsrijkMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder basisAttributen() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FOLLOW_RANGE, 20).add(Attributes.ATTACK_DAMAGE, 1).add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    // ---- what a subclass gives ------------------------------------------------------------------------------------------

    /** Max Vads at this level (elites x2.5). */
    protected abstract double basisVads(int level);

    /** Damage of a normal hit at this level (elites x1.4). */
    protected abstract double basisSchade(int level);

    /** Xp for defeating it (shared, see {@link #verslagen}). */
    public abstract int xp();

    /** The loot table id (see {@link LootTabellen}), or null for none. */
    @Nullable
    public abstract String lootTabel();

    public Element standaardElement() {
        return Element.WATER;
    }

    // ---- setup ----------------------------------------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VADS, 20f);
        b.define(MAX_VADS, 20f);
        b.define(LEVEL, 1);
        b.define(ELEMENT, Element.WATER.ordinal());
        b.define(ONTVADST, true);
        b.define(PLOF, -1);
        b.define(ELITE, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Sets level, element, elite and affixes, and fills the Vads. Call once after creating (spawners do). */
    public void stelIn(int level, @Nullable Element element, boolean elite, Set<Affix> affixen) {
        this.entityData.set(LEVEL, Math.max(1, level));
        this.entityData.set(ELEMENT, (element == null ? standaardElement() : element).ordinal());
        this.elite = elite;
        this.entityData.set(ELITE, elite);
        this.affixen.clear();
        this.affixen.addAll(affixen);
        double max = basisVads(level) * (elite ? 2.5 : 1);
        this.entityData.set(MAX_VADS, (float) max);
        this.entityData.set(VADS, (float) max);
        if (affixen.contains(Affix.SNEL) && getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() * 1.4);
        }
        ingesteld = true;
        naamplaatje();
    }

    public int mobLevel() {
        return entityData.get(LEVEL);
    }

    public Element element() {
        int i = entityData.get(ELEMENT);
        return i >= 0 && i < Element.values().length ? Element.values()[i] : Element.GEEN;
    }

    public boolean ontvadst() {
        return entityData.get(ONTVADST);
    }

    public void zetOntvadst(boolean o) {
        entityData.set(ONTVADST, o);
    }

    public boolean elite() {
        return entityData.get(ELITE);
    }

    public Set<Affix> affixen() {
        return affixen;
    }

    public double vads() {
        return entityData.get(VADS);
    }

    public double maxVads() {
        return entityData.get(MAX_VADS);
    }

    protected void zetMaxVads(double max, boolean houdVerhouding) {
        double r = maxVads() <= 0 ? 1 : vads() / maxVads();
        entityData.set(MAX_VADS, (float) max);
        entityData.set(VADS, (float) (houdVerhouding ? max * r : Math.min(vads(), max)));
        naamplaatje();
    }

    /** Ticks since the plof, or -1 while it is still ontvadst and fighting. */
    public int plofTicks() {
        return entityData.get(PLOF);
    }

    public boolean geplofd() {
        return plofTicks() >= 0;
    }

    // ---- damage ---------------------------------------------------------------------------------------------------------

    /** Only {@link Schade} calls this. */
    public void krijgSchade(double n, @Nullable LivingEntity bron) {
        if (geplofd() || n <= 0) {
            return;
        }
        if (bron instanceof ServerPlayer p) {
            bijdrage.merge(p.getUUID(), n, Double::sum);
            if (getTarget() == null || !(getTarget() instanceof Player)) {
                setTarget(p);
            }
        }
        double v = vads() - n;
        entityData.set(VADS, (float) Math.max(0, v));
        this.hurtTime = 10;
        this.hurtDuration = 10;
        if (level() instanceof ServerLevel sl) {
            sl.broadcastEntityEvent(this, (byte) 2);
        }
        naamplaatje();
        if (v <= 0) {
            verslagen(bron);
        }
    }

    public void heal(double n) {
        if (!geplofd()) {
            entityData.set(VADS, (float) Math.min(maxVads(), vads() + n));
            naamplaatje();
        }
    }

    /** Vanilla damage: a player's left click becomes the basic attack; everything else (fall, lava, cramming) is ignored. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (geplofd()) {
            return false;
        }
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
                discard();
            } else {
                krijgSchade(vads() + 1, null);
            }
            return true;
        }
        if (source.getEntity() instanceof ServerPlayer p && source.getDirectEntity() == p && Vadsrijk.in(p)) {
            return Schade.basisaanval(p, this);
        }
        return false;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (geplofd() || !(target instanceof LivingEntity l) || Statussen.heeft(this, Status.VERDOOFD)) {
            return false;
        }
        aanval(l);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }

    /** A normal hit on this target (through the damage pipeline). Override for special attacks. */
    public void aanval(LivingEntity doel) {
        Schade.doe(new Schade.Klap(this, doel, basisSchade(mobLevel()) * (elite ? 1.4 : 1), element(), Schade.Soort.MELEE, false, "mob"));
        if (affixen.contains(Affix.GLOEIEND)) {
            Statussen.geef(doel, Status.BRANDEND, 60, Math.max(1, mobLevel() * 0.5), this);
        }
    }

    // ---- defeat ---------------------------------------------------------------------------------------------------------

    /** Opgevrolijkt! Xp + loot for the helpers, the hook, then the plof. */
    public void verslagen(@Nullable LivingEntity laatste) {
        if (geplofd() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        entityData.set(PLOF, 0);
        entityData.set(VADS, 0f);
        Set<ServerPlayer> helpers = new LinkedHashSet<>();
        for (UUID id : bijdrage.keySet()) {
            if (sl.getEntity(id) instanceof ServerPlayer p && p.distanceTo(this) < 64) {
                helpers.add(p);
            }
        }
        if (laatste instanceof ServerPlayer p) {
            helpers.add(p);
        }
        beloon(sl, helpers);
        Haken.VERSLAGEN.forEach(h -> h.accept(new Haken.Verslagen(this, helpers)));
        setTarget(null);
        getNavigation().stop();
        Statussen.wisAlles(this);
        opPlof();
        naamplaatje();
    }

    /** Xp (shared with the party nearby) and personal loot per helper. Bosses override this (chests). */
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        LootTabel t = lootTabel() == null ? null : LootTabellen.van(lootTabel());
        for (ServerPlayer p : helpers) {
            Spelers.geefXpGedeeld(p, xp() * (elite ? 3 : 1), "mob");
            Spelers.data(p).opgevrolijkt++;
            if (t != null) {
                LootTabel.Resultaat r = t.rol(mobLevel(), getRandom());
                LootBundel.laatVallen(p, position().add(0, 0.5, 0), r, elite ? 1 : 0);
            }
        }
    }

    /** The pink plof: particles + a happy squeak (default). The mob then hops away for 1.5 s and vanishes. */
    protected void opPlof() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + getBbHeight() / 2, getZ(), 30, getBbWidth() / 2, getBbHeight() / 2,
                    getBbWidth() / 2, 0.05);
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight(), getZ(), 4, 0.3, 0.2, 0.3, 0.0);
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + getBbHeight() / 2, getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_JUMP_SMALL, SoundSource.NEUTRAL, 1f, 1.8f);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1f, 1.6f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (!ingesteld) {
            stelIn(mobLevel(), element(), elite, affixen);
        }
        boolean stil = stil();
        for (net.minecraft.world.entity.ai.goal.Goal.Flag f : net.minecraft.world.entity.ai.goal.Goal.Flag.values()) {
            if (stil) {
                goalSelector.disableControlFlag(f);
                targetSelector.disableControlFlag(f);
            } else {
                goalSelector.enableControlFlag(f);
                targetSelector.enableControlFlag(f);
            }
        }
        if (stil) {
            getNavigation().stop();
        }
        int plof = plofTicks();
        if (plof >= 0) {
            entityData.set(PLOF, plof + 1);
            if (plof < 30 && plof % 8 == 0 && onGround()) {   // happy hops away from where it was beaten
                double a = getYRot() * Math.PI / 180;
                setDeltaMovement(-Math.sin(a) * 0.3, 0.42, Math.cos(a) * 0.3);
            } else if (plof >= 30 && plof % 14 == 0 && onGround() && level() instanceof ServerLevel sl0) {   // then happy hops around
                double a = getRandom().nextDouble() * Math.PI * 2;
                setYRot((float) Math.toDegrees(Math.atan2(-Math.cos(a), Math.sin(a))));
                setYBodyRot(getYRot());
                setDeltaMovement(Math.cos(a) * 0.15, 0.38, Math.sin(a) * 0.15);
                if (getRandom().nextInt(3) == 0) {
                    sl0.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 1, 0.2, 0.1, 0.2, 0);
                }
            }
            if (plof > plofDuur()) {
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 0.3, getZ(), 10, 0.3, 0.2, 0.3, 0.02);
                }
                discard();
            }
            return;
        }
        if (affixen.contains(Affix.SCHILDEND) && tickCount % 200 == 0) {
            Statussen.geef(this, Status.KNUFFELSCHILD, 100, maxVads() * 0.15, this);
        }
    }

    /** How long (ticks) it stays visible, cheered up and hopping, after the plof before it vanishes. */
    protected int plofDuur() {
        return 30;
    }

    /** Standing still (no goals run; physics go on): after the plof, while stunned; bosses also during their intro. */
    public boolean stil() {
        return geplofd() || Statussen.heeft(this, Status.VERDOOFD);
    }

    /**
     * The nameplate is drawn by the client ({@code kern.client.KernClient}, through NeoForge's RenderNameTagEvent) from the
     * synced values, so a rename or a name tag never breaks it (a custom name replaces only the name part). Nothing to do on
     * the server; kept so callers don't need to change.
     */
    public void naamplaatje() {
    }

    /** The nameplate text: [element letter] (Elite) name Lv n and a coloured HP bar (or a heart after the plof). */
    public Component plaatje() {
        Element e = element();
        MutableComponent n = e.icoon();   // (fix round 1: a small coloured element icon instead of the "[W]" letter; GEEN = nothing)
        if (elite()) {
            n.append(Component.translatable("vadsrijk.kern.elite").withStyle(ChatFormatting.GOLD)).append(" ");
        }
        Component naam = hasCustomName() ? getCustomName() : getType().getDescription();
        n.append(naam.copy().withStyle(geplofd() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE));
        n.append(Component.literal(" Lv" + mobLevel()).withStyle(ChatFormatting.YELLOW));
        if (geplofd()) {
            n.append(Component.literal(" ♥").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            int balk = 10, vol = (int) Math.ceil(balk * vads() / Math.max(1, maxVads()));
            n.append(Component.literal(" " + "▌".repeat(vol)).withStyle(vol > 5 ? ChatFormatting.GREEN : vol > 2 ? ChatFormatting.GOLD : ChatFormatting.RED));
            n.append(Component.literal("▌".repeat(balk - vol)).withStyle(ChatFormatting.DARK_GRAY));
        }
        return n;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("vr_level", mobLevel());
        out.putInt("vr_element", element().ordinal());
        out.putBoolean("vr_elite", elite);
        out.putBoolean("vr_ontvadst", ontvadst());
        out.putFloat("vr_vads", (float) vads());
        out.putString("vr_spawner", spawner);
        out.putString("vr_affixen", String.join(",", affixen.stream().map(Enum::name).toList()));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        int lvl = in.getIntOr("vr_level", 1);
        int el = in.getIntOr("vr_element", standaardElement().ordinal());
        boolean el2 = in.getBooleanOr("vr_elite", false);
        Set<Affix> af = EnumSet.noneOf(Affix.class);
        for (String s : in.getStringOr("vr_affixen", "").split(",")) {
            if (!s.isEmpty()) {
                try {
                    af.add(Affix.valueOf(s));
                } catch (IllegalArgumentException ignored) {
                    // (an affix that no longer exists)
                }
            }
        }
        stelIn(lvl, Element.values()[Math.max(0, Math.min(Element.values().length - 1, el))], el2, af);
        zetOntvadst(in.getBooleanOr("vr_ontvadst", true));
        entityData.set(VADS, Math.min((float) maxVads(), in.getFloatOr("vr_vads", (float) maxVads())));
        spawner = in.getStringOr("vr_spawner", "");
        naamplaatje();
    }
}
