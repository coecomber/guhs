package nl.juiced.guhs.feature.safari.verblijf;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.safari.SafariDier;

/**
 * Op Safari! step 2 (S5): an animal living at home with a {@link VerblijfsbordBlockEntity verblijfsbord}. A {@link SafariDier}
 * (same models, same behaviours) whose home is the board's area - a circle, or a rectangle when the owner chose one - and
 * which belongs to that board: every two seconds it checks that its board still stands and still counts it as family;
 * otherwise it goes "back to the park" (a puff) and is gone. So a moved or broken board never leaves animals behind, and a
 * board can never make a second family (design 1.9).
 * <p>
 * At home: petting (empty hand: hearts), feeding (its food or {@code #guhs:safarivoer}, which counts double), playing with
 * its toy, meeting friends (both play happily), the ostrich can be ridden (sneak + right-click, fast and bouncy), the stork
 * flies off with a parcel ({@link #vliegWeg}). Babies stay babies: the family never grows.
 */
public class ThuisDier extends SafariDier {
    public static final TagKey<Item> SAFARIVOER = ItemTags.create(Guhs.id("safarivoer"));
    private static final EntityDataAccessor<Integer> DATA_VARIATIE = SynchedEntityData.defineId(ThuisDier.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BUNDEL = SynchedEntityData.defineId(ThuisDier.class, EntityDataSerializers.BOOLEAN);
    /** How far outside a rectangular home before it is put back at once (like the park's TELEPORT_MARGE). */
    private static final int MARGE = 8;

    private BlockPos bord = BlockPos.ZERO;
    @Nullable
    private UUID bordId;
    private boolean rechthoek;
    private int halfBreed = 12, halfDiep = 12;
    /** The game day it was last fed / petted, and how many snacks today. */
    private long voerDag = -1, aaiDag = -1;
    private int snacks;
    /** The stork's trip: ticks left away (0: home). */
    private int wegTot;
    /** Ticks until it looks for friends / its toy again. */
    private int rondkijken = 100;
    /** Deel 2 (F10b, design 2.8): the family's job site ("werkuitje"), its radius, until when the board last asked. Not saved:
     *  the board asks again every second. */
    @Nullable
    private BlockPos werkplek;
    private int werkStraal, werkTot, thuisLopen, volgendWerk;

    public ThuisDier(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIATIE, 0);
        builder.define(DATA_BUNDEL, false);
    }

    /** Makes this the {@code variant} of the family of this board. */
    public void koppel(VerblijfsbordBlockEntity be, String variant, int variatie) {
        bord = be.getBlockPos().immutable();
        bordId = be.bordId();
        entityData.set(DATA_VARIATIE, variatie);
        neemVorm(be);
        zet(be.soort(), variant, be.middelpunt(), be.straal(), -1, -1, 0);
    }

    /** Takes the board's current home (shape, size, centre or sleeping spot). */
    public void neemVorm(VerblijfsbordBlockEntity be) {
        rechthoek = be.rechthoek();
        halfBreed = be.breedte();
        halfDiep = be.diepte();
        zetThuis(be.middelpunt(), be.straal());
    }

    public BlockPos bord() {
        return bord;
    }

    @Nullable
    public UUID bordId() {
        return bordId;
    }

    /** The family's look: 0..5, a little different fur tint per board (the client mixes it into the colour). */
    public int variatie() {
        return entityData.get(DATA_VARIATIE);
    }

    /** The stork carries a parcel (the client shows the "bundeltje" bone). */
    public boolean bundel() {
        return entityData.get(DATA_BUNDEL);
    }

    @Override
    protected boolean binnenThuis(int x, int z) {
        if (!rechthoek || werkplek != null) {
            return super.binnenThuis(x, z);
        }
        return Math.abs(x - thuis().getX()) <= halfBreed && Math.abs(z - thuis().getZ()) <= halfDiep;
    }

    private boolean binnenMarge(double x, double z, int marge) {
        if (!rechthoek) {
            return Math.hypot(x - (thuis().getX() + 0.5), z - (thuis().getZ() + 0.5)) <= straal() + marge;
        }
        return Math.abs(x - (thuis().getX() + 0.5)) <= halfBreed + marge && Math.abs(z - (thuis().getZ() + 0.5)) <= halfDiep + marge;
    }

    /** Escape-proof at home too: a bit outside walks back, far outside (or fallen) is put back - except while ridden or away. */
    @Override
    protected void bewaak(ServerLevel level) {
        if (isVehicle() || wegTot > 0) {
            return;
        }
        if (werkplek != null) {
            // on a job: walks there by itself; far away or fallen: a puff and there
            boolean gevallen = getY() < werkplek.getY() - 24 || getY() < level.getMinY() + 2;
            if (gevallen || horizontaal(werkplek) > 96) {
                poef(level);
                BlockPos p = grond(level, werkplek.getX() + random.nextInt(3) - 1, werkplek.getZ() + random.nextInt(3) - 1);
                teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                poef(level);
            }
            return;
        }
        if (thuisLopen > 0) {
            // back from a job: walking home (a puff only when it takes too long)
            thuisLopen--;
            if (binnenMarge(getX(), getZ(), 0)) {
                thuisLopen = 0;
            } else if (thuisLopen == 0) {
                poef(level);
                terugNaarHuis(level);
                poef(level);
            } else if (getNavigation().isDone()) {
                getNavigation().moveTo(thuis().getX() + 0.5, thuis().getY(), thuis().getZ() + 0.5, 1.1);
            }
            return;
        }
        if (!rechthoek) {
            super.bewaak(level);
            return;
        }
        boolean gevallen = getY() < thuis().getY() - 24 || getY() < level.getMinY() + 2;
        if (!binnenMarge(getX(), getZ(), MARGE) || gevallen) {
            terugNaarHuis(level);
        } else if (!binnenMarge(getX(), getZ(), 2) && getNavigation().isDone()) {
            getNavigation().moveTo(thuis().getX() + 0.5, thuis().getY(), thuis().getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || isRemoved()) {
            return;
        }
        if (wegTot > 0) {
            onderweg(level);
        }
        if (werkplek != null) {
            opWerkuitje(level);
        }
        if ((tickCount + getId()) % 40 == 0 && !hoortBijBord(level)) {
            poef(level);
            discard();
            return;
        }
        if (--rondkijken <= 0) {
            rondkijken = 160 + random.nextInt(160);
            if (!slaapt() && !isVehicle() && wegTot == 0) {
                vrienden(level);
            }
        }
    }

    /** Does its board still stand here, with the same id, counting this animal as family? */
    boolean hoortBijBord(ServerLevel level) {
        if (bordId == null || !level.isLoaded(bord)) {
            return level.isLoaded(bord) ? false : true;     // (an unloaded board: wait; no board id at all: an orphan)
        }
        return level.getBlockEntity(bord) instanceof VerblijfsbordBlockEntity be && bordId.equals(be.bordId()) && be.familie(getUUID());
    }

    /** Friends near (both play), else now and then its toy. */
    private void vrienden(ServerLevel level) {
        VerblijfData.Dier d = VerblijfData.dier(soort());
        if (d == null) {
            return;
        }
        List<ThuisDier> om = level.getEntitiesOfClass(ThuisDier.class, getBoundingBox().inflate(6), t -> t != this && !t.slaapt());
        for (ThuisDier ander : om) {
            if (d.vriend(ander.soort())) {
                doeActie("blij", 60);
                ander.doeActie("blij", 60);
                level.sendParticles(ParticleTypes.HEART, (getX() + ander.getX()) / 2, getY() + getBbHeight(), (getZ() + ander.getZ()) / 2,
                        3, 0.4, 0.2, 0.4, 0.0);
                return;
            }
        }
        BlockPos speeltje = Tevredenheid.zoekSpeeltje(level, blockPosition(), 8, d.speeltje());
        if (speeltje != null && random.nextInt(3) == 0) {
            if (distanceToSqr(Vec3.atCenterOf(speeltje)) > 9) {
                getNavigation().moveTo(speeltje.getX() + 0.5, speeltje.getY(), speeltje.getZ() + 0.5, 1.0);
            } else {
                doeActie("blij", 80);
            }
        }
    }

    // --- the Bouwopdracht (deel 2, F10b) ----------------------------------------------------------------------------------

    /** The board sends the family to its job site (every second while the job runs): walk there and stay near it. */
    public void werkuitje(BlockPos plek, int straal) {
        werkTot = tickCount + 100;
        if (plek.equals(werkplek)) {
            return;
        }
        werkplek = plek.immutable();
        werkStraal = Math.max(2, straal);
        thuisLopen = 0;
        zetThuis(werkplek, werkStraal);
        getNavigation().stop();
    }

    @Nullable
    public BlockPos werkplek() {
        return werkplek;
    }

    /** The job is done (or stopped): home again, walking (happy when it went well). */
    public void naarHuis(@Nullable VerblijfsbordBlockEntity be, boolean blij) {
        if (werkplek == null) {
            return;
        }
        werkplek = null;
        if (be == null && level().getBlockEntity(bord) instanceof VerblijfsbordBlockEntity b) {
            be = b;
        }
        if (be != null) {
            neemVorm(be);
        }
        thuisLopen = 600;
        if (blij) {
            doeActie("blij", 60);
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    /** One of its work animations toward a spot (a block of the job just went in). */
    public void werk(BlockPos pos) {
        java.util.List<String> anims = nl.juiced.guhs.feature.bouwhulp.helpers.Bouwopdracht.werkAnimaties(soort());
        getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (!anims.isEmpty()) {
            java.util.List<String> staand = anims.stream().filter(a -> !a.equals("bouw_sjouw")).toList();
            java.util.List<String> keuze = staand.isEmpty() ? anims : staand;
            speelActie(keuze.get(random.nextInt(keuze.size())), 40);
        } else {
            doeActie("blij", 40);
        }
        volgendWerk = tickCount + 40 + random.nextInt(40);
    }

    private double horizontaal(BlockPos p) {
        double dx = getX() - (p.getX() + 0.5), dz = getZ() - (p.getZ() + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void opWerkuitje(ServerLevel level) {
        if (tickCount > werkTot) {
            naarHuis(null, false);                  // the board stopped asking (unloaded, broken)
            return;
        }
        if (slaapt() || isVehicle() || vast() != null) {
            return;
        }
        double d = horizontaal(werkplek);
        if (d > werkStraal + 1) {
            if (getNavigation().isDone() && (tickCount + getId()) % 20 == 0) {
                BlockPos g = grond(level, werkplek.getX(), werkplek.getZ());
                getNavigation().moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 1.15);
                if (nl.juiced.guhs.feature.bouwhulp.helpers.Bouwopdracht.werkAnimaties(soort()).contains("bouw_sjouw") && random.nextInt(3) == 0) {
                    speelActie("bouw_sjouw", 60);
                }
            }
        } else if (tickCount > volgendWerk && random.nextInt(40) == 0) {
            BlockPos naar = werkplek.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            werk(naar);
        }
    }

    // --- people ----------------------------------------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!(level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        VerblijfData.Dier d = VerblijfData.dier(soort());
        if (d != null && d.rijdier() && !isBaby() && player.isSecondaryUseActive() && stack.isEmpty() && !isVehicle()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!stack.isEmpty() && d != null && (stack.is(SAFARIVOER) || d.voer().contains(itemId(stack)))) {
            voer(level, player, stack);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            aai(level, player);
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.mobInteract(player, hand);
    }

    private static String itemId(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private long dag(ServerLevel level) {
        return level.getGameTime() / 24000L;
    }

    private void aai(ServerLevel level, Player player) {
        doeActie("blij", 50);
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 1.3f + random.nextFloat() * 0.3f);
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.geaaid", getDisplayName()));
        }
        aaiDag = dag(level);
        if (level.getBlockEntity(bord) instanceof VerblijfsbordBlockEntity be) {
            be.verzorgd();
        }
    }

    private void voer(ServerLevel level, Player player, ItemStack stack) {
        long vandaag = dag(level);
        if (voerDag != vandaag) {
            voerDag = vandaag;
            snacks = 0;
        }
        int waarde = stack.is(SAFARIVOER) ? 2 : 1;
        if (snacks >= 4) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.vol", getDisplayName()));
            }
            return;
        }
        snacks = Math.min(4, snacks + waarde);
        stack.consume(1, player);
        doeActie(info() != null && info().animaties().contains("eet") ? "eet" : "blij", 60);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() * 0.6, getZ(), 5 * waarde, 0.3, 0.2, 0.3, 0.0);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.7f, 1.2f);
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.gevoerd", getDisplayName()));
        }
        if (level.getBlockEntity(bord) instanceof VerblijfsbordBlockEntity be) {
            be.gevoerd();
        }
    }

    @Nullable
    private nl.juiced.guhs.feature.safari.ParkData.Soort info() {
        return nl.juiced.guhs.feature.safari.ParkData.get().soort(soort());
    }

    public boolean vandaagGevoerd() {
        return level() instanceof ServerLevel l && voerDag == dag(l);
    }

    public boolean vandaagGeaaid() {
        return level() instanceof ServerLevel l && aaiDag == dag(l);
    }

    /** A puff of smoke: the animal goes back to the park (or appears). */
    public void poef(ServerLevel level) {
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 12, getBbWidth() / 2, getBbHeight() / 2, getBbWidth() / 2, 0.02);
        level.playSound(null, blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 0.6f, 1.6f);
    }

    /** Hides for a while (the menu's "Even verstoppen"): invisible, then a puff and back. */
    public void verstop(ServerLevel level, int ticks) {
        poef(level);
        setInvisible(true);
        wegTot = -Math.max(1, ticks);          // (negative: hiding at home, not flying)
    }

    // --- the stork's delivery -------------------------------------------------------------------------------------------

    /** The stork flies off with a parcel and is away for {@code ticks}. */
    public void vliegWeg(int ticks) {
        entityData.set(DATA_BUNDEL, true);
        doeActie("vlieg", 40);
        wegTot = Math.max(60, ticks);
        setNoGravity(true);
    }

    public boolean weg() {
        return wegTot != 0;
    }

    private void onderweg(ServerLevel level) {
        if (wegTot < 0) {                       // hiding at home
            if (++wegTot == 0) {
                setInvisible(false);
                poef(level);
            }
            return;
        }
        getNavigation().stop();
        int verstreken = tickCount;
        if (!isInvisible()) {
            setDeltaMovement(0, 0.12, 0);       // up, up and away
            if (getY() > thuis().getY() + 10 || (verstreken & 63) == 0 && random.nextInt(2) == 0) {
                poef(level);
                setInvisible(true);
            }
        }
        if (--wegTot == 0) {
            setInvisible(false);
            setNoGravity(false);
            entityData.set(DATA_BUNDEL, false);
            terugNaarHuis(level);
            poef(level);
        }
    }

    // --- riding (the ostrich) -------------------------------------------------------------------------------------------

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : super.getControllingPassenger();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        Vec2 rot = new Vec2(controller.getXRot() * 0.5f, controller.getYRot());
        setRot(rot.y, rot.x);
        yRotO = yBodyRot = yHeadRot = getYRot();
        if (onGround() && riddenInput.z > 0.1 && (tickCount & 7) == 0) {
            setDeltaMovement(getDeltaMovement().x, 0.32, getDeltaMovement().z);    // stuiterig: hop hop hop
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float zij = controller.xxa * 0.5f, vooruit = controller.zza;
        return new Vec3(zij, 0, vooruit <= 0 ? vooruit * 0.25f : vooruit);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.15f;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (level() instanceof ServerLevel level && !binnenMarge(getX(), getZ(), 2)) {
            getNavigation().moveTo(thuis().getX() + 0.5, thuis().getY(), thuis().getZ() + 0.5, 1.2);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // --- saving ---------------------------------------------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BordX", bord.getX());
        tag.putInt("BordY", bord.getY());
        tag.putInt("BordZ", bord.getZ());
        tag.storeNullable("BordId", UUIDUtil.CODEC, bordId);
        tag.putBoolean("Rechthoek", rechthoek);
        tag.putInt("HalfBreed", halfBreed);
        tag.putInt("HalfDiep", halfDiep);
        tag.putInt("Variatie", variatie());
        tag.putLong("VoerDag", voerDag);
        tag.putLong("AaiDag", aaiDag);
        tag.putInt("Snacks", snacks);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        bord = new BlockPos(tag.getIntOr("BordX", 0), tag.getIntOr("BordY", 0), tag.getIntOr("BordZ", 0));
        bordId = tag.read("BordId", UUIDUtil.CODEC).orElse(null);
        rechthoek = tag.getBooleanOr("Rechthoek", false);
        halfBreed = tag.getIntOr("HalfBreed", 12);
        halfDiep = tag.getIntOr("HalfDiep", 12);
        entityData.set(DATA_VARIATIE, tag.getIntOr("Variatie", 0));
        voerDag = tag.getLongOr("VoerDag", -1L);
        aaiDag = tag.getLongOr("AaiDag", -1L);
        snacks = tag.getIntOr("Snacks", 0);
        wegTot = 0;                             // (a trip or a hiding never survives a reload: back home, visible)
        setInvisible(false);
        setNoGravity(hangt());
        entityData.set(DATA_BUNDEL, false);
    }

    /** The texture tint per family (client): a soft, slightly lighter/darker/warmer fur. */
    public static int tint(int variatie) {
        return switch (Mth.positiveModulo(variatie, 6)) {
            case 1 -> 0xFFF4ECE2;
            case 2 -> 0xFFE8E0D8;
            case 3 -> 0xFFFFF2E4;
            case 4 -> 0xFFEDEDF4;
            case 5 -> 0xFFF7E9DF;
            default -> 0xFFFFFFFF;
        };
    }

    static Identifier id() {
        return Guhs.id("safari_thuisdier");
    }
}
