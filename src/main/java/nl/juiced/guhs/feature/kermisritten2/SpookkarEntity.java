package nl.juiced.guhs.feature.kermisritten2;

import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.safari.rit.Pad;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * A cart of the Spookguhhuis: it waits at the station ({@link #WACHT}), rides the track with up to two players
 * ({@link #RIJDT}), lets them out at the south mouth ({@link #UIT}) and rolls back round to the station ({@link #TERUG}).
 * Nobody jumps off under way (Ritten2Feature.onMount). Never saved, not to be broken.
 */
public class SpookkarEntity extends Entity implements GeoEntity {
    public static final int WACHT = 0, RIJDT = 1, UIT = 2, TERUG = 3;
    public static final double SNELHEID = 0.15, TERUG_SNELHEID = 0.25;
    public static final int VERTREK = 50, UIT_TIJD = 30, PLAATSEN = 2;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos anker;
    @Nullable
    private Pad pad;
    private int staat = WACHT, wacht, volgende;
    private double afstand;
    boolean uitstappen;

    public SpookkarEntity(EntityType<? extends SpookkarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    public int staat() {
        return staat;
    }

    public double afstand() {
        return afstand;
    }

    /** Puts the cart on the track: at the station, or just behind it. */
    void zetSpoor(SpookhuisBlockEntity huis, boolean opStation) {
        anker = huis.getBlockPos().immutable();
        pad = huis.pad();
        staat = opStation ? WACHT : TERUG;
        afstand = opStation ? 0 : pad.lengte() - 3;
        plaats(true);
    }

    private void plaats(boolean snap) {
        Vec3 p = pad.op(afstand);
        float yaw = pad.richting(afstand, getYRot());
        setPos(p);
        if (snap) {
            xo = xOld = p.x;
            yo = yOld = p.y;
            zo = zOld = p.z;
            setYRot(yaw);
            yRotO = yaw;
        } else {
            setYRot(Mth.approachDegrees(getYRot(), yaw, 9f));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate();
            return;
        }
        if (anker == null || pad == null) {
            allemaalEruit(null);
            discard();
        } else if (level().getBlockEntity(anker) instanceof SpookhuisBlockEntity huis) {
            stap(huis);
        } else if (level().isLoaded(anker)) {
            allemaalEruit(null);
            discard();
        }
    }

    /** One tick along the track. */
    void stap(SpookhuisBlockEntity huis) {
        switch (staat) {
            case WACHT -> {
                if (getPassengers().stream().anyMatch(e -> e instanceof Player) && ++wacht >= VERTREK) {
                    staat = RIJDT;
                    wacht = 0;
                    volgende = 0;
                }
            }
            case RIJDT -> {
                if (getPassengers().isEmpty()) {
                    staat = TERUG;     // (everyone logged out)
                    break;
                }
                afstand += SNELHEID;
                plaats(false);
                schrik(huis);
                if (afstand >= pad.afstandVan(SpookhuisBlockEntity.UIT_I)) {
                    staat = UIT;
                }
            }
            case UIT -> {
                if (++wacht >= UIT_TIJD) {
                    wacht = 0;
                    klaar(huis);
                    staat = TERUG;
                }
            }
            default -> {
                double tot = stationBezet(huis) ? pad.lengte() - 3 : pad.lengte();
                afstand = Math.max(afstand, Math.min(tot, afstand + TERUG_SNELHEID));
                plaats(false);
                if (afstand >= pad.lengte() - 1e-3) {
                    afstand = 0;
                    staat = WACHT;
                    plaats(false);
                }
            }
        }
    }

    private boolean stationBezet(SpookhuisBlockEntity huis) {
        return !level().getEntitiesOfClass(SpookkarEntity.class, huis.huis().inflate(4), k -> k != this && k.staat == WACHT).isEmpty();
    }

    /** Past a spot: a ghost pops up and says something sweet to the cart. */
    private void schrik(SpookhuisBlockEntity huis) {
        while (volgende < SpookhuisBlockEntity.SCHRIK.length && afstand >= pad.afstandVan((int) SpookhuisBlockEntity.SCHRIK[volgende][0])) {
            double[] s = SpookhuisBlockEntity.SCHRIK[volgende++];
            Vec3 plek = huis.rel(s[1], s[2], 1);
            SpookjeEntity spookje = Ritten2Feature.SPOOKJE.get().create(level(), EntitySpawnReason.TRIGGERED);
            if (spookje != null) {
                spookje.setPos(plek);
                spookje.setYRot(getYRot() + 180);
                level().addFreshEntity(spookje);
            }
            level().playSound(null, plek.x, plek.y, plek.z, SoundEvents.VEX_CHARGE, SoundSource.NEUTRAL, 0.7f, 1.7f);
            for (Entity e : getPassengers()) {
                if (e instanceof ServerPlayer p) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.spookhuis.boe." + (int) s[3]).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }
    }

    private void klaar(SpookhuisBlockEntity huis) {
        for (Entity e : getPassengers()) {
            if (e instanceof ServerPlayer p) {
                Minigames.give(p, new ItemStack(ModItems.KERMISBON.get()));
                GuhAdvancements.grant(p, "spookhuis_gereden");
                p.sendSystemMessage(Component.translatable("gui.guhs.spookhuis.klaar").withStyle(ChatFormatting.GOLD));
            }
        }
        level().playSound(null, this, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        allemaalEruit(huis.uitgang());
    }

    /** Out, onto the given spot (null: where they are). */
    void allemaalEruit(@Nullable Vec3 plek) {
        uitstappen = true;
        for (Entity e : List.copyOf(getPassengers())) {
            e.stopRiding();
            if (plek != null) {
                e.teleportTo(plek.x, plek.y, plek.z);
            }
        }
        uitstappen = false;
    }

    /** Under way nobody gets off. */
    public boolean opWeg() {
        return staat == RIJDT;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p) {
            if (staat != WACHT) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.spookhuis.niet_nu"));
            } else if (p.startRiding(this)) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.spookhuis.instappen"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().size() < PLAATSEN;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        double z = getPassengers().indexOf(passenger) == 0 ? 0.35 : -0.45;
        return new Vec3(0, 0.15, z).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        float draai = Mth.wrapDegrees(getYRot() - yRotO);
        if (Math.abs(draai) < 45) {
            passenger.setYRot(passenger.getYRot() + draai);
            passenger.setYHeadRot(passenger.getYHeadRot() + draai);
        }
    }

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
