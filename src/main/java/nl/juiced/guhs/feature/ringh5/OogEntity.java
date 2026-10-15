package nl.juiced.guhs.feature.ringh5;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.storage.Nbt;
import org.joml.Vector3f;

/**
 * bbq2 (ring-h5): het Oog van Sausron (entity {@code guhs:oog_van_sausron}; model, texture and animations:
 * tools/features/ring_h5_modellen.py): the one burning Mika eye in the socket of the giant Mika head on the tower behind the
 * Zwarte Roosterpoort. "He just had trek": he stares down the valley for his lost snack.
 * <ul>
 *   <li>It hangs still in its socket (no gravity, nothing moves or hurts it) and turns towards the spot it looks at.</li>
 *   <li>Its gaze is a {@link Blik}: a spot of light on the valley floor, shown to everybody as a glowing eye on the ground
 *       (a block display of {@code guhs:ringh5_blik} that glides along, tag {@link #BLIK_TAG}) with flames around its rim
 *       and a thin beam of sparks from the Eye down to it. Whoever it sees is put back on their last rest point: the Eye
 *       never hurts anybody.</li>
 *   <li>When nobody on the trip is in the valley it sleeps (lids shut); for players whose story is done it has had its
 *       piece of ring and looks the other way.</li>
 *   <li>In a cutscene it is an actor like any other ({@code Cutscene.Builder.animatie}): {@code slaap} / {@code dicht}
 *       shut its lids, {@code schrik} / {@code zoek} make it dart about, {@code knipper} / {@code tevreden} / {@code eet}
 *       are the pleased squint (chapter 6: it gets its piece of ring), any other name ({@code kijk}) is its wide open
 *       stare; {@code Cutscene.Builder.kijk} turns it.</li>
 * </ul>
 * It knows its {@link Terrein} (saved): the copy of the structure it belongs to. One made by a spawn egg or a command looks
 * for the copy it hangs in; outside any copy it just blinks.
 */
public class OogEntity extends Mob implements GeoEntity {
    /** The tag of the light on the ground (a block display). */
    public static final String BLIK_TAG = "guhs_ringh5_blik";
    /** The Eye turns towards the spot it looks at from here: its pupil, relative to its feet (build coordinates). */
    public static final Vec3 PUPIL = Plekken.OOG_KIJK.subtract(Plekken.OOG.getX() + 0.5, Plekken.OOG.getY(), Plekken.OOG.getZ() + 0.5);
    /** The way it looks in the build: down the valley (-z). */
    public static final float YAW = 180f;
    /** After a catch it squints this long, pleased with itself. */
    public static final int TEVREDEN_TICKS = 40;

    private static final EntityDataAccessor<Integer> DATA_STAAT = SynchedEntityData.defineId(OogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<org.joml.Vector3fc> DATA_BLIK = SynchedEntityData.defineId(OogEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> DATA_TEVREDEN = SynchedEntityData.defineId(OogEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation WAAK = RawAnimation.begin().thenLoop("animation.oog_van_sausron.waak");
    private static final RawAnimation ZOEK = RawAnimation.begin().thenLoop("animation.oog_van_sausron.zoek");
    private static final RawAnimation SLAAP = RawAnimation.begin().thenLoop("animation.oog_van_sausron.slaap");
    private static final RawAnimation TEVREDEN = RawAnimation.begin().thenLoop("animation.oog_van_sausron.tevreden");
    private static final DustParticleOptions VONK = new DustParticleOptions(0xFF7314, 1.6f);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Blik blik = new Blik();
    @Nullable
    private Terrein terrein;
    private boolean gezocht;
    @Nullable
    private UUID licht;
    private int tevredenTot;

    public OogEntity(EntityType<? extends OogEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAAT, Blik.WAAKT);
        builder.define(DATA_BLIK, new Vector3f(0, -1000, 0));
        builder.define(DATA_TEVREDEN, false);
    }

    @Override
    protected void registerGoals() {
    }

    // --- what it is doing --------------------------------------------------------------------------------------------------

    /** {@link Blik#WAAKT}, {@link Blik#ZOEKT} or {@link Blik#SLAAPT} (synced). */
    public int staat() {
        return this.entityData.get(DATA_STAAT);
    }

    /** It just caught somebody (synced: a pleased squint). */
    public boolean tevreden() {
        return this.entityData.get(DATA_TEVREDEN);
    }

    /** The spot on the ground it looks at (synced; null: none). */
    @Nullable
    public Vec3 blikPlek() {
        org.joml.Vector3fc v = this.entityData.get(DATA_BLIK);
        return v.y() < -500 ? null : new Vec3(v.x(), v.y(), v.z());
    }

    public Blik blik() {
        return blik;
    }

    @Nullable
    public Terrein terrein() {
        return terrein;
    }

    /** The copy of the Zwarte Roosterpoort this Eye watches over. */
    public void zetTerrein(@Nullable Terrein t) {
        this.terrein = t;
        this.gezocht = true;
    }

    /** Where its sight starts (world): the front of the pupil. */
    public Vec3 kijkpunt() {
        Rotation draai = terrein == null ? Rotation.NONE : terrein.draai();
        Vec3 p = PUPIL;
        Vec3 gedraaid = switch (draai) {
            case CLOCKWISE_90 -> new Vec3(-p.z, p.y, p.x);
            case CLOCKWISE_180 -> new Vec3(-p.x, p.y, -p.z);
            case COUNTERCLOCKWISE_90 -> new Vec3(p.z, p.y, -p.x);
            default -> p;
        };
        return position().add(gedraaid);
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel level) {
            serverTick(level);
        } else {
            clientTick();
        }
    }

    private void serverTick(ServerLevel level) {
        if (terrein == null && !gezocht && tickCount > 5) {
            gezocht = true;
            terrein = Terrein.bij(level, blockPosition());
        }
        int staat = Blik.SLAAPT;
        Vec3 plek = null;
        if (terrein != null && terrein.isIn(level)) {
            staat = blik.tick(level, terrein, kijkpunt(), this);
            plek = blik.plek();
            if (blik.neemBetrapt() > 0) {
                tevredenTot = tickCount + TEVREDEN_TICKS;
                playSound(SoundEvents.RAVAGER_CELEBRATE, 2.5f, 0.6f);
            }
        }
        if (staat() != staat) {
            this.entityData.set(DATA_STAAT, staat);
        }
        boolean tevreden = tickCount < tevredenTot;
        if (tevreden() != tevreden) {
            this.entityData.set(DATA_TEVREDEN, tevreden);
        }
        float basis = terrein == null ? getYRot() : terrein.yaw(YAW);
        if (plek != null) {
            // it turns towards what it looks at
            Vec3 d = plek.subtract(kijkpunt());
            float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
            setYRot(basis);
            setYBodyRot(basis);
            setYHeadRot(yaw);
            setXRot(pitch);
            this.entityData.set(DATA_BLIK, new Vector3f((float) plek.x, (float) plek.y, (float) plek.z));
        } else {
            setYRot(basis);
            setYBodyRot(basis);
            setYHeadRot(basis);
            setXRot(25f);
            if (blikPlek() != null) {
                this.entityData.set(DATA_BLIK, new Vector3f(0, -1000, 0));
            }
        }
        licht(level, plek);
    }

    /** The light on the ground: one block display that glides along with the gaze; gone while the Eye sleeps. */
    private void licht(ServerLevel level, @Nullable Vec3 plek) {
        Entity display = licht == null ? null : level.getEntity(licht);
        if (plek == null) {
            if (display != null) {
                display.discard();
            }
            licht = null;
            if (tickCount % 100 == 7) {
                ruimLichtenOp(level, null);
            }
            return;
        }
        if (display == null || !display.isAlive()) {
            ruimLichtenOp(level, null);   // (a light of before a restart, or of a chunk that came back)
            display = maakLicht(level, plek);
            licht = display == null ? null : display.getUUID();
        } else {
            display.setPos(plek.x, plek.y + 0.02, plek.z);
        }
    }

    @Nullable
    private Entity maakLicht(ServerLevel level, Vec3 plek) {
        Entity display = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) {
            return null;
        }
        float r = (float) Blik.STRAAL;
        CompoundTag tag = new CompoundTag();
        CompoundTag blok = new CompoundTag();
        blok.putString("Name", "guhs:ringh5_blik");
        tag.put("block_state", blok);
        CompoundTag vorm = new CompoundTag();
        vorm.put("translation", floats(-r, 0f, -r));
        vorm.put("scale", floats(2 * r, 1f, 2 * r));
        vorm.put("left_rotation", floats(0f, 0f, 0f, 1f));
        vorm.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tag.put("transformation", vorm);
        CompoundTag fel = new CompoundTag();
        fel.putInt("sky", 15);
        fel.putInt("block", 15);
        tag.put("brightness", fel);
        tag.putInt("teleport_duration", 3);
        tag.putFloat("view_range", 3f);
        tag.putFloat("shadow_radius", 0f);
        Nbt.load(display, tag);
        display.snapTo(plek.x, plek.y + 0.02, plek.z, 0f, 0f);
        display.addTag(BLIK_TAG);
        level.addFreshEntity(display);
        return display;
    }

    /** Removes every light of this Eye's valley except {@code behalve}. */
    private void ruimLichtenOp(ServerLevel level, @Nullable Entity behalve) {
        AABB waar = new AABB(position(), position()).inflate(Blik.BEREIK, 64, Blik.BEREIK);
        List<Display.BlockDisplay> oud = level.getEntitiesOfClass(Display.BlockDisplay.class, waar, e -> e.entityTags().contains(BLIK_TAG) && e != behalve);
        for (Display.BlockDisplay e : oud) {
            e.discard();
        }
    }

    private static ListTag floats(float... waarden) {
        ListTag lijst = new ListTag();
        for (float f : waarden) {
            lijst.add(FloatTag.valueOf(f));
        }
        return lijst;
    }

    /** Flames around the rim of the light, a beam of sparks from the Eye to it, embers around the Eye itself. */
    private void clientTick() {
        if (random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.FLAME, getRandomX(1.6), getY() + 0.6 + random.nextDouble() * 2.6, getRandomZ(1.6), 0, 0.03, 0);
        }
        Vec3 plek = blikPlek();
        if (plek == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            double hoek = random.nextDouble() * Math.PI * 2;
            level().addAlwaysVisibleParticle(ParticleTypes.FLAME, true, plek.x + Math.cos(hoek) * Blik.STRAAL, plek.y + 0.1, plek.z + Math.sin(hoek) * Blik.STRAAL,
                    0, 0.02 + random.nextDouble() * 0.03, 0);
        }
        if (tickCount % 2 == 0) {
            Vec3 van = position().add(0, 2.0, 0);
            double t = random.nextDouble();
            Vec3 op = van.lerp(plek, t);
            level().addAlwaysVisibleParticle(VONK, true, op.x + (random.nextDouble() - 0.5) * 0.6, op.y, op.z + (random.nextDouble() - 0.5) * 0.6, 0, 0, 0);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel level && licht != null) {
            Entity display = level.getEntity(licht);
            if (display != null) {
                display.discard();
            }
        }
        super.remove(reason);
    }

    // --- nothing touches it ------------------------------------------------------------------------------------------------------

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
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
    protected boolean isImmobile() {
        return true;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (terrein != null) {
            tag.store("Nul", BlockPos.CODEC, terrein.nul());
            tag.putString("Draai", terrein.draai().name());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        BlockPos nul = tag.read("Nul", BlockPos.CODEC).orElse(null);
        if (nul != null) {
            Rotation draai;
            try {
                draai = Rotation.valueOf(tag.getStringOr("Draai", "NONE"));
            } catch (IllegalArgumentException e) {
                draai = Rotation.NONE;
            }
            terrein = new Terrein(level().dimension(), nul, draai);
            gezocht = true;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<OogEntity>("main", 6, state -> {
            String scene = Cutscenes.animatie(this);
            if (scene.equals("slaap") || scene.equals("dicht")) {
                return state.setAndContinue(SLAAP);
            }
            if (scene.equals("schrik") || scene.equals("zoek")) {
                return state.setAndContinue(ZOEK);
            }
            if (scene.equals("knipper") || scene.equals("tevreden") || scene.equals("eet")) {
                return state.setAndContinue(TEVREDEN);
            }
            if (!scene.isEmpty()) {
                return state.setAndContinue(WAAK);
            }
            return state.setAndContinue(tevreden() ? TEVREDEN : staat() == Blik.SLAAPT ? SLAAP : staat() == Blik.ZOEKT ? ZOEK : WAAK);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
