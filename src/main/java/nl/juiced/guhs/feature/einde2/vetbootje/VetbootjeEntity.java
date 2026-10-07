package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * einde2, slice vetbootje: the player's own vetbootje (guhs:vetbootje): a small, slow flying ship in the style of the big
 * vetschip, with a Mika sail, a chest of 27 and two cushions for guhs. It hovers: it never falls, not even with nobody
 * aboard, and it cannot sink below the world. Model, layers and animation: tools/features/einde2_vetbootje_modellen.py.
 * <ul>
 *   <li>Click = step aboard (the owner takes the wheel, anybody else a cushion). W/S sail, A/D steer, jump = up,
 *       sprint = down.</li>
 *   <li>Sneak + click, or a click on the deck while you sail (the owner) = the chest. Click with a kaasknabbel = your own guhs close by hop aboard (or off again).
 *       Click with the werfboekje = the scheepswerf screen. Sneak + hit (the owner) = it drifts home until you whistle.</li>
 * </ul>
 * Driven like a boat: the rider's game moves it and tells the server (vanilla's vehicle packets); without a rider the
 * server lets it glide to a stop. What a player owns is the page in {@link Vloot}; this entity is only the body (see there).
 * A boat without an owner ({@link Vetbootje#leen}) is anybody's and has no chest.
 */
public class VetbootjeEntity extends Entity implements GeoEntity {
    /** Blocks per tick: ahead, astern, up and down. A vetbootje is no raceguh. */
    public static final double TOP = 0.22, ACHTERUIT = 0.08, STIJG = 0.11;
    /** How many may ride along besides the one at the wheel. */
    public static final int MEEVAARDERS = 2;
    /** The deck and the cushions (blocks above the entity's feet), the wheel's and the cushions' place fore and aft. */
    private static final double DEK = 0.375, KUSSEN = 0.46, STUUR_ACHTER = 0.42, KUSSEN_VOOR = 0.6, KUSSEN_OPZIJ = 0.3;

    // 26.1: no OPTIONAL_UUID serializer any more; an entity reference carries the owner's UUID the same way
    private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_EIGENAAR =
            SynchedEntityData.defineId(VetbootjeEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(VetbootjeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ZEIL = SynchedEntityData.defineId(VetbootjeEntity.class, EntityDataSerializers.INT);

    /** What the one at the wheel does: vooruit (-1 astern .. 1 ahead), stuur (-1 left .. 1 right), omhoog (-1 down .. 1 up). */
    public record Invoer(double vooruit, double stuur, double omhoog) {
        public static final Invoer NIKS = new Invoer(0, 0, 0);
    }

    /** The rider's keys (set by the client). */
    @Nullable
    public static Supplier<Invoer> invoer;
    /** Every client tick (the wake of crumbs and vet). */
    @Nullable
    public static Consumer<VetbootjeEntity> clientTick;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Server: the number of this body in its owner's page of the {@link Vloot}. */
    int nr;
    /** Forward and upward speed (blocks per tick). */
    public double snelheid, stijg;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot;
    private boolean gekeken;

    // --- client only: for drawing ---
    public float helling, hellingO, wiel, wielO;

    public VetbootjeEntity(EntityType<? extends VetbootjeEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(DATA_EIGENAAR, Optional.empty());
        b.define(DATA_KLEUR, Vetbootje.STANDAARD_KLEUR.getId());
        b.define(DATA_ZEIL, 0);
    }

    // --- owner and looks ----------------------------------------------------------------------------------------------------

    public void zetEigenaar(@Nullable UUID id) {
        entityData.set(DATA_EIGENAAR, Optional.ofNullable(id).map(EntityReference::of));
    }

    @Nullable
    public UUID eigenaar() {
        return entityData.get(DATA_EIGENAAR).map(EntityReference::getUUID).orElse(null);
    }

    public boolean isVan(Player player) {
        return player.getUUID().equals(eigenaar());
    }

    /** May this player take the wheel? The owner, or anybody on a boat that belongs to nobody. */
    public boolean magSturen(Player player) {
        UUID e = eigenaar();
        return e == null || e.equals(player.getUUID());
    }

    public DyeColor kleur() {
        return DyeColor.byId(entityData.get(DATA_KLEUR));
    }

    public void zetKleur(DyeColor kleur) {
        entityData.set(DATA_KLEUR, kleur.getId());
    }

    public Zeil zeil() {
        return Zeil.vanNummer(entityData.get(DATA_ZEIL));
    }

    public void zetZeil(Zeil zeil) {
        entityData.set(DATA_ZEIL, zeil.ordinal());
    }

    /** The boat's name (it is the entity's custom name: you read it when you look at the boat). */
    public Component naam() {
        Component c = getCustomName();
        return c != null ? c : getType().getDescription();
    }

    /** Server: takes colour, sail and name from the owner's page. */
    void neemOver(Vloot.Boot b) {
        if (kleur() != b.kleur) {
            zetKleur(b.kleur);
        }
        if (zeil() != b.zeil) {
            zetZeil(b.zeil);
        }
        Component naam = b.toonNaam();
        if (!naam.equals(getCustomName())) {
            setCustomName(naam);
        }
    }

    /** Server: this body is not needed any more: everybody off, a little puff, gone. */
    void vaarwel() {
        if (level() instanceof ServerLevel server) {
            ejectPassengers();
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.6, getZ(), 10, 0.8, 0.3, 0.8, 0.01);
        }
        discard();
    }

    /** No speed left (after it was put somewhere). */
    void stilLeggen() {
        snelheid = stijg = 0;
        setDeltaMovement(Vec3.ZERO);
        lerpSteps = 0;
    }

    // --- sailing ------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (lerpSteps > 0 && !bestuurdHier()) {
            double f = 1.0 / lerpSteps;
            setPos(getX() + (lerpX - getX()) * f, getY() + (lerpY - getY()) * f, getZ() + (lerpZ - getZ()) * f);
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) * (float) f);
            lerpSteps--;
        }
        if (bestuurdHier()) {
            Invoer in = getControllingPassenger() instanceof Player && level().isClientSide() && invoer != null ? invoer.get() : Invoer.NIKS;
            vaar(in);
        } else if (level().isClientSide()) {
            setDeltaMovement(Vec3.ZERO);
        }
        if (!level().isClientSide()) {
            serverTick();
        } else if (clientTick != null) {
            clientTick.accept(this);
        }
    }

    /**
     * One tick of sailing with these keys: slowly up to speed, slowly to a stop, turning (also on the spot), up and down;
     * nothing pressed = it hovers where it is. (The rider's game calls it every tick; tests call it on the server.)
     */
    public void vaar(Invoer in) {
        double vooruit = Mth.clamp(in.vooruit(), -1, 1), stuur = Mth.clamp(in.stuur(), -1, 1), omhoog = Mth.clamp(in.omhoog(), -1, 1);
        double doel = vooruit > 0.1 ? TOP : vooruit < -0.1 ? -ACHTERUIT : 0;
        snelheid += Mth.clamp(doel - snelheid, -0.01, 0.006);
        if (doel == 0 && Math.abs(snelheid) < 0.004) {
            snelheid = 0;
        }
        double stijgDoel = omhoog * STIJG;
        if (stijgDoel > 0 && getY() >= level().getMaxY() + 24) {
            stijgDoel = 0;                                    // (thin air up here)
        }
        if (getY() <= level().getMinY() + 2) {
            stijgDoel = Math.max(stijgDoel, 0.04);            // it never sinks out of the world
        }
        stijg += Mth.clamp(stijgDoel - stijg, -0.008, 0.008);
        if (stijgDoel == 0 && Math.abs(stijg) < 0.004) {
            stijg = 0;
        }
        double draai = stuur * (1.4 + 1.8 * Math.min(1, Math.abs(snelheid) / TOP));
        setYRot(getYRot() + (float) draai);
        hellingO = helling;
        helling += (float) ((-draai * 3.0 - helling) * 0.15);
        wielO = wiel;
        wiel += (float) (draai * 6.0);
        Vec3 dir = Vec3.directionFromRotation(0, getYRot());
        setDeltaMovement(dir.x * snelheid, stijg, dir.z * snelheid);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision) {
            snelheid *= 0.5;
        }
        if (verticalCollision) {
            stijg = 0;
        }
    }

    private void serverTick() {
        if (!gekeken || tickCount % 20 == 0) {
            gekeken = true;
            UUID e = eigenaar();
            if (e != null) {
                Vloot.Boot b = Vloot.van(((ServerLevel) level()).getServer()).boot(e);
                if (!b.heeft || b.nr != nr) {
                    vaarwel();                               // (replaced by a newer body: see Vloot)
                    return;
                }
                neemOver(b);
            }
        }
        if (Math.abs(snelheid) > 0.05 && tickCount % 90 == 0) {
            level().playSound(null, this, E2VetbootjeFeature.KRAAK.get(), SoundSource.NEUTRAL, 0.5f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    /** 1.21.1 isControlledByLocalInstance (26.1 made its replacement final): the rider's own game, or the server without rider. */
    public boolean bestuurdHier() {
        return getControllingPassenger() instanceof Player p ? p.isLocalPlayer() : !level().isClientSide();
    }

    /** 26.1: server positions arrive through the interpolation handler (was lerpTo); our own 10-step glide in tick(). */
    private final InterpolationHandler glijden = new InterpolationHandler(this) {
        @Override
        public void interpolateTo(Vec3 position, float yRot, float xRot) {
            lerpX = position.x;
            lerpY = position.y;
            lerpZ = position.z;
            lerpYRot = yRot;
            lerpSteps = 10;
        }

        @Override
        public boolean hasActiveInterpolation() {
            return lerpSteps > 0;
        }

        @Override
        public void interpolate() {
        }

        @Override
        public void cancel() {
            lerpSteps = 0;
        }
    };

    @Override
    public InterpolationHandler getInterpolation() {
        return glijden;
    }

    @Override
    public boolean isFlyingVehicle() {
        return true;
    }

    // --- riding -------------------------------------------------------------------------------------------------------------

    /** The one at the wheel: the first passenger who may steer. */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity e : getPassengers()) {
            if (e instanceof Player p && magSturen(p)) {
                return p;
            }
        }
        return null;
    }

    /** How many ride along (everybody but the one at the wheel). */
    public int meevaarders() {
        return getPassengers().size() - (getControllingPassenger() == null ? 0 : 1);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (passenger instanceof Player p && magSturen(p) && getControllingPassenger() == null) {
            return true;
        }
        return meevaarders() < MEEVAARDERS;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        Vec3 voor = Vec3.directionFromRotation(0, getYRot());
        Vec3 opzij = new Vec3(-voor.z, 0, voor.x);
        if (passenger == getControllingPassenger()) {
            return voor.scale(-STUUR_ACHTER).add(0, DEK, 0);
        }
        int plek = 0;
        for (Entity e : getPassengers()) {
            if (e == passenger) {
                break;
            }
            if (e != getControllingPassenger()) {
                plek++;
            }
        }
        return voor.scale(KUSSEN_VOOR).add(opzij.scale(plek % 2 == 0 ? KUSSEN_OPZIJ : -KUSSEN_OPZIJ)).add(0, KUSSEN, 0);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        float draai = Mth.wrapDegrees(getYRot() - yRotO);
        if (passenger instanceof Player) {
            if (Math.abs(draai) < 45) {                      // the deck turns under your feet: you turn along
                passenger.setYRot(passenger.getYRot() + draai);
                passenger.setYHeadRot(passenger.getYHeadRot() + draai);
            }
        } else if (passenger instanceof LivingEntity living) {
            living.setYBodyRot(getYRot());                   // the guhs look where the boat goes
            living.setYRot(getYRot());
            living.setYHeadRot(getYRot());
        }
    }

    /** Off the boat high in the air (sneak, a teleport, the boat called away): you float down instead of falling. */
    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        Meevaren.zachtLanden(this, passenger);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        UUID eigenaar = eigenaar();
        if (held.is(E2VetbootjeFeature.VETBOOTJE_WERFBOEKJE.get()) && isVan(player)) {
            Vetbootje.openWerf(sp);
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.KAAS_KNABBELS.get())) {
            if (magSturen(player)) {
                Meevaren.wissel(this, sp, MEEVAARDERS - meevaarders());
            } else {
                nietVanJou(sp);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.getVehicle() == this) {                    // aboard already: a click on the deck opens the chest
            if (eigenaar == null || !isVan(player)) {
                return InteractionResult.PASS;
            }
            openKist(sp);
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            if (eigenaar == null) {
                sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.leen.geen_kist").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                openKist(sp);
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isPassenger() && player.startRiding(this)) {
            if (getControllingPassenger() == player && !GuhQuests.saved(sp).getBooleanOr("guhs_einde2_vetbootje_uitleg", false)) {
                GuhQuests.saved(sp).putBoolean("guhs_einde2_vetbootje_uitleg", true);
                sp.sendSystemMessage(Component.translatable("gui.guhs.einde2_vetbootje.uitleg", Component.keybind("key.jump"),
                        Component.keybind("key.sprint")).withStyle(ChatFormatting.AQUA));
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isPassenger()) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.vol").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    private void nietVanJou(ServerPlayer sp) {
        Vloot.Boot b = Vloot.van(sp.level().getServer()).boot(eigenaar());
        sp.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.niet_van_jou", b.eigenaarNaam).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** The one aboard can click the boat they stand on (the deck = the chest; the werfboekje and a kaasknabbel work too). */
    @Override
    public boolean canRiderInteract() {
        return true;
    }

    /** The chest (sneak + click, or a click on the deck while you sail): the owner's 27 slots in the {@link Vloot}. */
    public void openKist(ServerPlayer sp) {
        UUID eigenaar = eigenaar();
        if (eigenaar == null) {
            return;
        }
        if (!isVan(sp)) {
            nietVanJou(sp);
            return;
        }
        Vloot.Boot b = Vloot.van(sp.level().getServer()).boot(eigenaar);
        sp.openMenu(new SimpleMenuProvider((id, inv, speler) -> new ChestMenu(MenuType.GENERIC_9x3, id, inv, new Meevaren.Kist(b.kist, this), 3),
                Component.translatable("gui.guhs.einde2_vetbootje.kist", naam())));
    }

    /** The owner's chest (server; null for a boat that belongs to nobody). */
    @Nullable
    public net.minecraft.world.Container kist() {
        UUID eigenaar = eigenaar();
        return eigenaar == null || !(level() instanceof ServerLevel server) ? null : Vloot.van(server.getServer()).boot(eigenaar).kist;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer player && player.isShiftKeyDown()) {
            if (eigenaar() == null ? player.getAbilities().instabuild : isVan(player)) {
                if (eigenaar() != null) {
                    player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.naar_huis", naam()).withStyle(ChatFormatting.GOLD));
                }
                vaarwel();                                   // (its page stays: the fluitje brings a new body)
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;                                         // (the fluitje brings it to another dimension, with its chest)
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(E2VetbootjeFeature.VETBOOTJE_FLUITJE.get());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160 * 160;
    }

    // --- saving -------------------------------------------------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(ValueInput tag) {
        tag.read("Eigenaar", UUIDUtil.CODEC).ifPresent(this::zetEigenaar);
        nr = tag.getIntOr("Nr", 0);
        zetKleur(DyeColor.byId(tag.getIntOr("Kleur", Vetbootje.STANDAARD_KLEUR.getId())));
        zetZeil(Zeil.vanNaam(tag.getStringOr("Zeil", "")));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput tag) {
        UUID e = eigenaar();
        if (e != null) {
            tag.store("Eigenaar", UUIDUtil.CODEC, e);
        }
        tag.putInt("Nr", nr);
        tag.putInt("Kleur", kleur().getId());
        tag.putString("Zeil", zeil().name());
        // (the name is the entity's custom name: vanilla saves it)
    }

    // --- GeckoLib -----------------------------------------------------------------------------------------------------------

    private static final RawAnimation ZWEVEN = RawAnimation.begin().thenLoop("animation.vetbootje.zweven");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("zweven", 0, state -> state.setAndContinue(ZWEVEN)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
