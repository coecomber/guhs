package nl.juiced.guhs.feature.vadsrijk.party.nep;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.NepBijdrage;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * W-PARTY: a nep-speler, a fake PLAYER (player model, a default-style skin, a gamer name) with simple but visible AI. They DO fight
 * the zone's mobs (they are no guhs).
 * <ul>
 *   <li><b>Wild</b> ({@link #eigenaar} null): wanders around its spawn spot in a mob zone, fights mobs nearby, chats now and then.</li>
 *   <li><b>Companion</b> (recruited, or filling a dungeon queue): follows its player, attacks that player's target (or whatever
 *       attacks the group), the healer heals the group, the tank pulls the mobs onto itself.</li>
 *   <li><b>Damage</b>: a hit takes {@link NepRol#deel} of the target's max Vads (bosses a tenth), credited to its player (never to
 *       itself: it takes no loot). A mob's hit takes a fixed part of its health ({@link NepRol#klap}); near 0 it faints for 10 s
 *       ("rip") and stands up at 60 %. It never dies and is never saved ({@link NepSpelers} owns every one).</li>
 *   <li><b>Players cannot hurt it</b> (no PvP), and a player with "Nep-spelers: uit" does not see it at all (client side).</li>
 * </ul>
 */
public class NepSpeler extends PathfinderMob {
    private static final EntityDataAccessor<String> NAAM = SynchedEntityData.defineId(NepSpeler.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(NepSpeler.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ROL = SynchedEntityData.defineId(NepSpeler.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(NepSpeler.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ZEG = SynchedEntityData.defineId(NepSpeler.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> FLAUW = SynchedEntityData.defineId(NepSpeler.class, EntityDataSerializers.BOOLEAN);
    /** How many default skins there are (the wide player skins: alex, ari, efe, kai, makena, noor, steve, sunny, zuri). */
    public static final int SKINS = 9;
    /** How long a chat line stays above the head (ticks). */
    public static final int ZEG_TICKS = 100;
    /** Client: set by the client code; false = this player switched the nep-spelers off (not drawn, not pickable). */
    public static BooleanSupplier ZICHTBAAR = () -> true;

    /** The real player it runs with (null = wild). */
    @Nullable
    public UUID eigenaar;
    /** The players it belongs to (party / dungeon group): their frames show it. */
    public final Set<UUID> groep = new LinkedHashSet<>();
    /** Game time (overworld) it leaves (-1: never on its own; wild ones get a lifetime too). */
    public long tot = -1;
    /** Filling a dungeon queue: the dungeon id (it leaves when its player is out of the dungeon). */
    @Nullable
    public String dungeon;
    /** Wild: the zone id and its home spot. */
    public String gebied = "";
    @Nullable
    public BlockPos thuis;
    @Nullable
    private LivingEntity doel;
    private int volgendeKlap, flauwTot, volgendePraat, volgendeLoop, eigenaarWeg, buitenDungeon;
    /** Client: the tick the current chat line arrived. */
    public int zegSinds = -1000;

    public NepSpeler(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        volgendePraat = 200 + level.getRandom().nextInt(600);
    }

    public static AttributeSupplier.Builder attributen() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, 1.05)
                .add(Attributes.FOLLOW_RANGE, 32).add(Attributes.SAFE_FALL_DISTANCE, 16).add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(NAAM, "Nepper");
        b.define(SKIN, 0);
        b.define(ROL, NepRol.SCHADE.ordinal());
        b.define(LEVEL, 1);
        b.define(ZEG, "");
        b.define(FLAUW, false);
    }

    // ---- data ---------------------------------------------------------------------------------------------------------------

    public String naam() {
        return entityData.get(NAAM);
    }

    public int skin() {
        return Math.floorMod(entityData.get(SKIN), SKINS);
    }

    public NepRol rol() {
        return NepRol.van(entityData.get(ROL));
    }

    public int nepLevel() {
        return entityData.get(LEVEL);
    }

    public String zegKey() {
        return entityData.get(ZEG);
    }

    public boolean flauw() {
        return entityData.get(FLAUW);
    }

    /** Sets everything at once (spawn). */
    public void stelIn(String naam, int skin, NepRol rol, int level) {
        entityData.set(NAAM, naam);
        entityData.set(SKIN, skin);
        entityData.set(ROL, rol.ordinal());
        entityData.set(LEVEL, Math.max(1, level));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (rol) {
            case TANK -> Items.IRON_SWORD;
            case HELER -> Items.BLAZE_ROD;
            case SCHADE -> Items.BOW;
        }));
        setItemSlot(EquipmentSlot.OFFHAND, rol == NepRol.TANK ? new ItemStack(Items.SHIELD) : ItemStack.EMPTY);
        for (EquipmentSlot s : EquipmentSlot.values()) {
            setDropChance(s, 0f);
        }
        naamplaatje();
    }

    /** The name tag: "[NPC] name · Tank · Lv 12". */
    public void naamplaatje() {
        setCustomName(Component.literal("[NPC] ").withStyle(ChatFormatting.GRAY).append(Component.literal(naam()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(Component.translatable(rol().key()).withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" · Lv " + nepLevel()).withStyle(ChatFormatting.DARK_GRAY)));
        setCustomNameVisible(true);
    }

    /** A chat line above the head (the chat lines in the chat box are sent by {@link NepSpelers#zeg}). */
    void zetZeg(String key) {
        entityData.set(ZEG, key);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ZEG.equals(key)) {
            zegSinds = tickCount;
        }
    }

    public boolean wild() {
        return eigenaar == null;
    }

    // ---- tick / AI ----------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (!NepSpelers.bekend(this)) {
            discard();   // (a leftover after a restart / a reload: NepSpelers owns every nep-speler)
            return;
        }
        if (flauw()) {
            getNavigation().stop();
            if (tickCount >= flauwTot) {
                entityData.set(FLAUW, false);
                setPose(Pose.STANDING);
                setHealth(getMaxHealth() * 0.6f);
            }
            return;
        }
        if (tickCount >= volgendePraat) {
            volgendePraat = tickCount + 900 + getRandom().nextInt(1500);
            if (doel == null && getRandom().nextFloat() < 0.6f) {
                NepSpelers.zeg(this, NepNamen.zeg("los", getRandom()), nepLevel());
            }
        }
        ServerPlayer baas = eigenaar == null ? null : sl.getServer().getPlayerList().getPlayer(eigenaar);
        if (eigenaar != null) {
            if (baas == null || baas.level() != level()) {
                eigenaarWeg++;
                getNavigation().stop();
                if (eigenaarWeg > 20 * 60) {
                    NepSpelers.afscheid(this, "doei");
                }
                return;
            }
            eigenaarWeg = 0;
        }
        if (tickCount % 10 == 0) {
            doel = kiesDoel(sl, baas);
        }
        if (rol() == NepRol.HELER && tickCount % 30 == 0) {
            heel(sl, baas);
        }
        if (rol() == NepRol.TANK && tickCount % 60 == 0) {
            lok(sl);
        }
        if (doel != null) {
            vecht(sl, baas);
        } else if (baas != null) {
            volg(baas);
        } else {
            dwaal();
        }
    }

    /** Range of a hit: melee for the tank, a bit further for the others ("bow" / "staff"). */
    private double bereik() {
        return switch (rol()) {
            case TANK -> 2.8;
            case HELER -> 8;
            case SCHADE -> 7;
        };
    }

    private static boolean levend(@Nullable LivingEntity e) {
        return e != null && e.isAlive() && !e.isRemoved() && !(e instanceof VadsrijkMob m && m.geplofd());
    }

    /** Companion: its player's target, else whatever attacks the group. Wild: the nearest mob (or one attacking its group mates). */
    @Nullable
    private LivingEntity kiesDoel(ServerLevel sl, @Nullable ServerPlayer baas) {
        if (baas != null) {
            LivingEntity d = NepSpelers.doelVan(baas);
            if (levend(d) && d.distanceTo(this) < 24) {
                return d;
            }
            List<VadsrijkMob> rond = sl.getEntitiesOfClass(VadsrijkMob.class, getBoundingBox().inflate(16), m -> levend(m) && m.getTarget() != null
                    && (groep.contains(m.getTarget().getUUID()) || m.getTarget() == this || m.getTarget() instanceof NepSpeler n && n.groep.contains(baas.getUUID())));
            return rond.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(this))).orElse(null);
        }
        if (levend(doel) && doel.distanceTo(this) < 20) {
            return doel;
        }
        Vec3 midden = thuis == null ? position() : Vec3.atCenterOf(thuis);
        List<VadsrijkMob> rond = sl.getEntitiesOfClass(VadsrijkMob.class, getBoundingBox().inflate(12), m -> levend(m) && !(m instanceof VadsrijkBaas)
                && m.position().distanceTo(midden) < 28);
        return rond.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(this))).orElse(null);
    }

    private void vecht(ServerLevel sl, @Nullable ServerPlayer baas) {
        LivingEntity d = doel;
        getLookControl().setLookAt(d, 30f, 30f);
        double afstand = distanceTo(d);
        if (afstand > bereik() - 0.4) {
            getNavigation().moveTo(d, 1.15);
        } else if (rol() != NepRol.TANK) {
            getNavigation().stop();
        }
        if (afstand > bereik() || tickCount < volgendeKlap || !hasLineOfSight(d)) {
            return;
        }
        volgendeKlap = tickCount + rol().tempo;
        swing(InteractionHand.MAIN_HAND);
        double max = d instanceof VadsrijkMob m ? m.maxVads() : d.getMaxHealth();
        double n = Math.max(1, max * rol().deel * (d instanceof VadsrijkBaas ? 0.1 : 1));
        if (d instanceof VadsrijkMob m) {
            if (baas != null && baas.distanceTo(this) < 64) {
                NepBijdrage.namens(m, baas, n);
            }
            if (m.getTarget() == null || rol() == NepRol.TANK) {
                m.setTarget(this);
            }
        }
        if (rol() != NepRol.TANK) {   // a little trail from the "bow" / "staff" to the target
            Vec3 van = getEyePosition(), naar = d.getBoundingBox().getCenter();
            for (int i = 1; i <= 6; i++) {
                Vec3 p = van.lerp(naar, i / 7.0);
                sl.sendParticles(rol() == NepRol.HELER ? ParticleTypes.ENCHANT : ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            sl.playSound(null, blockPosition(), rol() == NepRol.HELER ? SoundEvents.ALLAY_THROW : SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 0.6f, 1.2f);
        } else {
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 0.7f, 1f);
        }
        Schade.doe(new Schade.Klap(this, d, n, Element.GEEN, Schade.Soort.WAAR, false, "nepspeler"));
        if (!levend(d)) {
            doel = null;
            if (getRandom().nextFloat() < 0.25f) {
                NepSpelers.zeg(this, NepNamen.zeg("gg", getRandom()));
            }
        } else if (getRandom().nextFloat() < 0.04f) {
            NepSpelers.zeg(this, NepNamen.zeg("gevecht", getRandom()));
        }
    }

    /** The healer: the most hurt of the group (players and nep-spelers) within 16 blocks below 85 % gets 12 % of its max. */
    private void heel(ServerLevel sl, @Nullable ServerPlayer baas) {
        LivingEntity zwakst = null;
        double laagst = 0.85;
        for (Player p : sl.players()) {
            if (p instanceof ServerPlayer sp && (groep.contains(sp.getUUID()) || sp == baas) && sp.distanceTo(this) < 16 && sp.isAlive()) {
                double r = Spelers.vads(sp) / Math.max(1, Spelers.maxVads(sp));
                if (r < laagst) {
                    laagst = r;
                    zwakst = sp;
                }
            }
        }
        for (NepSpeler n : sl.getEntitiesOfClass(NepSpeler.class, getBoundingBox().inflate(16), x -> !x.flauw())) {
            boolean samen = n == this || (baas != null ? n.groep.contains(baas.getUUID()) : n.wild() && n.gebied.equals(gebied));
            double r = n.getHealth() / n.getMaxHealth();
            if (samen && r < laagst) {
                laagst = r;
                zwakst = n;
            }
        }
        if (zwakst == null) {
            return;
        }
        swing(InteractionHand.MAIN_HAND);
        if (zwakst instanceof ServerPlayer sp) {
            Schade.heal(this, sp, Spelers.maxVads(sp) * 0.12);
        } else {
            zwakst.heal(zwakst.getMaxHealth() * 0.12f);
        }
        sl.sendParticles(ParticleTypes.HEART, zwakst.getX(), zwakst.getY() + zwakst.getBbHeight() + 0.3, zwakst.getZ(), 3, 0.3, 0.2, 0.3, 0);
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, zwakst.getX(), zwakst.getY() + 1, zwakst.getZ(), 6, 0.4, 0.5, 0.4, 0);
        sl.playSound(null, zwakst.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.7f, 1.5f);
        if (zwakst != this && getRandom().nextFloat() < 0.15f) {
            NepSpelers.zeg(this, NepNamen.zeg("heal", getRandom()));
        }
    }

    /** The tank: mobs within 8 blocks that attack its group turn to it (a little angry cloud above them). */
    private void lok(ServerLevel sl) {
        for (VadsrijkMob m : sl.getEntitiesOfClass(VadsrijkMob.class, getBoundingBox().inflate(8), NepSpeler::levend)) {
            LivingEntity t = m.getTarget();
            boolean opGroep = t != null && t != this && (groep.contains(t.getUUID()) || (t instanceof NepSpeler n && (wild() ? n.wild() && n.gebied.equals(gebied)
                    : n.groep.equals(groep))));
            if (opGroep && !(m instanceof VadsrijkBaas)) {
                m.setTarget(this);
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getY() + m.getBbHeight() + 0.4, m.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    private void volg(ServerPlayer baas) {
        double a = distanceTo(baas);
        getLookControl().setLookAt(baas, 20f, 20f);
        if (a > 28) {
            Vec3 plek = baas.position().add(getRandom().nextDouble() * 2 - 1, 0, getRandom().nextDouble() * 2 - 1);
            teleportTo(plek.x, baas.getY(), plek.z);
            getNavigation().stop();
        } else if (a > 4.5) {
            getNavigation().moveTo(baas, a > 10 ? 1.3 : 1.0);
        } else {
            getNavigation().stop();
        }
    }

    private void dwaal() {
        if (tickCount < volgendeLoop || thuis == null) {
            return;
        }
        volgendeLoop = tickCount + 80 + getRandom().nextInt(160);
        BlockPos p = thuis.offset(getRandom().nextInt(25) - 12, 0, getRandom().nextInt(25) - 12);
        getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.8);
    }

    /** Dungeon fillers: ticks their player is outside the dungeon (NepSpelers sends them off after a while). */
    int buitenDungeon(boolean buiten) {
        buitenDungeon = buiten ? buitenDungeon + 1 : 0;
        return buitenDungeon;
    }

    // ---- damage -------------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) {
            NepSpelers.weg(this);
            return true;
        }
        if (flauw() || source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player) {
            return false;   // (no PvP: players never hurt a nep-speler)
        }
        if (!source.is(DamageTypes.GENERIC) && !source.is(DamageTypes.MOB_ATTACK)) {
            return false;   // (falls, lava, drowning, cramming: ignored, it is a "player" with good reflexes)
        }
        float verlies = (float) (getMaxHealth() * rol().klap);
        if (getHealth() - verlies <= getMaxHealth() * 0.05f) {
            valFlauw(level);
            return true;
        }
        setHealth(getHealth() - verlies);
        hurtTime = 10;
        hurtDuration = 10;
        level.broadcastEntityEvent(this, (byte) 2);
        return true;
    }

    private void valFlauw(ServerLevel level) {
        setHealth(1f);
        entityData.set(FLAUW, true);
        setPose(Pose.CROUCHING);
        flauwTot = tickCount + 200;
        doel = null;
        getNavigation().stop();
        for (VadsrijkMob m : level.getEntitiesOfClass(VadsrijkMob.class, getBoundingBox().inflate(24), m -> m.getTarget() == this)) {
            m.setTarget(null);
        }
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        NepSpelers.zeg(this, NepNamen.zeg("flauw", getRandom()));
    }

    /** Dev/tests: faint at once. */
    public void testFlauw() {
        if (level() instanceof ServerLevel sl) {
            valFlauw(sl);
        }
    }

    // ---- interaction / housekeeping ------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sp) {
            NepSpelers.praat(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPickable() {
        return !level().isClientSide() || ZICHTBAAR.getAsBoolean();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }
}
