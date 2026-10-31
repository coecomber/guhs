package nl.juiced.guhs.feature.kermisritten2;

import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
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
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * A Botsguh: a bumper car on the floor of {@link BotsbaanBlockEntity}. A click gets you in, W/S and A/D drive (server side,
 * like the canoe), sneaking gets you out. A ride lasts {@link #RIT} ticks; a whole ride with at least {@link #BONNEN_VANAF}
 * bumps earns a kermisbon, {@link #KAMPIOEN} bumps earn two. In an empty car sits a little guh that drives about while a
 * player is riding. It only ever exists on its floor: never saved, not to be broken.
 */
public class BotsguhEntity extends Entity implements GeoEntity {
    public static final int KLEUREN = 7;
    public static final int RIT = 900, BONNEN_VANAF = 5, KAMPIOEN = 15;
    public static final double TOP = 0.30, TOP_GUH = 0.20, GAS = 0.028, WRIJVING = 0.94, MAX = 0.5;
    /** A car's radius on the floor (two cars touch at twice this). */
    public static final double STRAAL = 0.85;
    /** How hard a hit must be to count as a bump (blocks a tick, between the two cars). */
    public static final double BOTS_VANAF = 0.09;
    /** Ticks before the same car's next bump counts. */
    private static final int RUST = 12;

    private static final EntityDataAccessor<Integer> DATA_KLEUR = SynchedEntityData.defineId(BotsguhEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_STROOM = SynchedEntityData.defineId(BotsguhEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos anker;
    @Nullable
    private AABB baan;
    private Vec3 uitgang = Vec3.ZERO;
    private Vec3 vaart = Vec3.ZERO;
    private int rust, ritTijd, botsingen, doelTijd;
    private boolean actief;
    private Vec3 doel = Vec3.ZERO;
    private List<BotsguhEntity> andere = List.of();

    public BotsguhEntity(EntityType<? extends BotsguhEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KLEUR, 0);
        builder.define(DATA_STROOM, false);
    }

    public int kleur() {
        return Mth.clamp(entityData.get(DATA_KLEUR), 0, KLEUREN - 1);
    }

    /** Does the current run through its pole (sparks at the ceiling)? */
    public boolean stroom() {
        return entityData.get(DATA_STROOM);
    }

    public void zetBaan(BlockPos anker, AABB baan, Vec3 uitgang, int kleur) {
        this.anker = anker.immutable();
        this.baan = baan;
        this.uitgang = uitgang;
        entityData.set(DATA_KLEUR, Math.floorMod(kleur, KLEUREN));
    }

    public boolean heeftRijder() {
        return getFirstPassenger() instanceof Player;
    }

    public int botsingen() {
        return botsingen;
    }

    public Vec3 vaart() {
        return vaart;
    }

    public void zetVaart(Vec3 vaart) {
        this.vaart = vaart;
    }

    void zetActief(boolean actief, List<BotsguhEntity> autos) {
        this.actief = actief;
        this.andere = autos;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate();
            if (stroom() && random.nextInt(9) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 4.9, getZ(), 0, -0.05, 0);
            }
            return;
        }
        if (baan == null || anker == null) {
            ejectPassengers();
            discard();
            return;
        }
        if (level().isLoaded(anker) && !(level().getBlockEntity(anker) instanceof BotsbaanBlockEntity)) {
            ejectPassengers();
            discard();
            return;
        }
        if (rust > 0) {
            rust--;
        }
        float gas = 0, stuur = 0;
        if (getFirstPassenger() instanceof ServerPlayer rijder) {
            Input in = rijder.getLastClientInput();
            gas = (in.forward() ? 1 : 0) - (in.backward() ? 0.6f : 0);
            stuur = (in.right() ? 1 : 0) - (in.left() ? 1 : 0);
            ritTijd++;
            if (ritTijd == RIT - 200) {
                rijder.sendOverlayMessage(Component.translatable("gui.guhs.botsguhs.bijna"));
            }
            if (ritTijd >= RIT) {
                klaar(rijder);
                return;
            }
        } else if (actief) {
            float[] guh = guhStuurt();
            gas = guh[0];
            stuur = guh[1];
        }
        rijd(gas, stuur, heeftRijder() ? TOP : TOP_GUH);
        boolean aan = actief || heeftRijder();
        if (aan != stroom()) {
            entityData.set(DATA_STROOM, aan);
        }
    }

    /** A guh heads for a spot on the floor, or (one time in three) for somebody else's car. */
    private float[] guhStuurt() {
        if (--doelTijd <= 0 || position().distanceToSqr(doel) < 4) {
            doelTijd = 40 + random.nextInt(80);
            doel = new Vec3(Mth.lerp(random.nextDouble(), baan.minX + 2, baan.maxX - 2), getY(), Mth.lerp(random.nextDouble(), baan.minZ + 2, baan.maxZ - 2));
            if (random.nextInt(3) == 0 && !andere.isEmpty()) {
                BotsguhEntity prooi = andere.get(random.nextInt(andere.size()));
                if (prooi != this && !prooi.isRemoved()) {
                    doel = prooi.position();
                }
            }
        }
        float wil = (float) Math.toDegrees(Math.atan2(-(doel.x - getX()), doel.z - getZ()));
        float verschil = Mth.wrapDegrees(wil - getYRot());
        return new float[]{Math.abs(verschil) > 110 ? 0.3f : 1f, Mth.clamp(verschil / 15f, -1f, 1f)};
    }

    /** One tick of driving: it slides a little, like a bumper car should, and bounces off the walls. */
    public void rijd(float gas, float stuur, double top) {
        if (stuur != 0) {
            setYRot(getYRot() + stuur * (vaart.horizontalDistanceSqr() > 4e-4 ? 4.5f : 3f));
        }
        Vec3 voor = Vec3.directionFromRotation(0, getYRot());
        vaart = vaart.scale(WRIJVING).add(voor.scale(gas * GAS));
        double v = vaart.length();
        if (gas != 0 && v > top) {
            vaart = vaart.scale(Math.max(top / v, 0.97));   // (a bump may be faster than the engine, and fades)
        } else if (v > MAX) {
            vaart = vaart.scale(MAX / v);
        }
        double x = getX() + vaart.x, z = getZ() + vaart.z;
        double x1 = baan.minX + STRAAL, x2 = baan.maxX - STRAAL, z1 = baan.minZ + STRAAL, z2 = baan.maxZ - STRAAL;
        boolean muur = false;
        if (x < x1 || x > x2) {
            x = Mth.clamp(x, x1, x2);
            muur = Math.abs(vaart.x) > BOTS_VANAF;
            vaart = new Vec3(-vaart.x * 0.6, 0, vaart.z);
        }
        if (z < z1 || z > z2) {
            z = Mth.clamp(z, z1, z2);
            muur |= Math.abs(vaart.z) > BOTS_VANAF;
            vaart = new Vec3(vaart.x, 0, -vaart.z * 0.6);
        }
        setPos(x, baan.minY, z);
        if (muur) {
            level().playSound(null, this, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.7f, 0.8f);
        }
    }

    /** Two cars on the floor: if they touch they push apart and swap the part of their speed that goes through the hit. */
    public static boolean bots(BotsguhEntity a, BotsguhEntity b) {
        double dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ();
        double d = Math.sqrt(dx * dx + dz * dz), raak = STRAAL * 2;
        if (d >= raak || a.isRemoved() || b.isRemoved()) {
            return false;
        }
        double nx = d < 1e-4 ? 1 : dx / d, nz = d < 1e-4 ? 0 : dz / d;
        double duw = (raak - d) / 2;
        a.setPos(a.getX() - nx * duw, a.getY(), a.getZ() - nz * duw);
        b.setPos(b.getX() + nx * duw, b.getY(), b.getZ() + nz * duw);
        double va = a.vaart.x * nx + a.vaart.z * nz, vb = b.vaart.x * nx + b.vaart.z * nz;
        double slag = va - vb;
        if (slag <= 0) {
            return false;       // (already moving apart)
        }
        double wissel = slag * 1.05;
        a.vaart = a.vaart.subtract(nx * wissel, 0, nz * wissel);
        b.vaart = b.vaart.add(nx * wissel, 0, nz * wissel);
        if (slag < BOTS_VANAF || a.rust > 0 || b.rust > 0) {
            return false;
        }
        a.rust = b.rust = RUST;
        a.telBots();
        b.telBots();
        if (a.level() instanceof ServerLevel level) {
            double x = (a.getX() + b.getX()) / 2, z = (a.getZ() + b.getZ()) / 2;
            level.sendParticles(ParticleTypes.CRIT, x, a.getY() + 0.5, z, 10, 0.25, 0.2, 0.25, 0.15);
            level.sendParticles(ParticleTypes.HEART, x, a.getY() + 1, z, 1, 0.1, 0.1, 0.1, 0);
            level.playSound(null, x, a.getY(), z, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.NEUTRAL, 1f, 0.7f + (float) slag);
        }
        return true;
    }

    private void telBots() {
        if (getFirstPassenger() instanceof ServerPlayer rijder) {
            botsingen++;
            rijder.sendOverlayMessage(Component.translatable("gui.guhs.botsguhs.bots", botsingen).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The whole ride is over: the prize, and out at the entrance. */
    void klaar(ServerPlayer rijder) {
        int bonnen = bonnenVoor(botsingen);
        GuhAdvancements.grant(rijder, "botsguhs_gereden");
        if (botsingen >= KAMPIOEN) {
            GuhAdvancements.grant(rijder, "botsguhs_kampioen");
        }
        if (bonnen > 0) {
            Minigames.give(rijder, new ItemStack(ModItems.KERMISBON.get(), bonnen));
            rijder.sendSystemMessage(Component.translatable("gui.guhs.botsguhs.klaar", botsingen, bonnen).withStyle(ChatFormatting.GOLD));
        } else {
            rijder.sendSystemMessage(Component.translatable("gui.guhs.botsguhs.klaar_niks", botsingen, BONNEN_VANAF).withStyle(ChatFormatting.GOLD));
        }
        level().playSound(null, this, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        rijder.stopRiding();
        rijder.teleportTo(uitgang.x, uitgang.y, uitgang.z);
        ritTijd = botsingen = 0;
        vaart = Vec3.ZERO;
    }

    public static int bonnenVoor(int botsingen) {
        return botsingen >= KAMPIOEN ? 2 : botsingen >= BONNEN_VANAF ? 1 : 0;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND || player.isPassenger() || isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p && p.startRiding(this)) {
            ritTijd = botsingen = 0;
            p.sendOverlayMessage(Component.translatable("gui.guhs.botsguhs.instappen"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && !isVehicle();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0, 0.1, 0.1).yRot(-getYRot() * Mth.DEG_TO_RAD);
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
