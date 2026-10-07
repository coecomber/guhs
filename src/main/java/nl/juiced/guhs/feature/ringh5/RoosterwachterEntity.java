package nl.juiced.guhs.feature.ringh5;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-h5): a Roosterwachter, one of the Mika guards of het Wachthek in front of the side door of the Zwarte
 * Roosterpoort (entity {@code guhs:ringh5_roosterwachter}; the Mika's model in a helmet with a grate for a visor:
 * tools/features/ring_h5_modellen.py). He stands on his post and looks left and right, down the lane.
 * <ul>
 *   <li>Whoever he sees (within {@link #ZICHT} blocks, in front of him, nothing in between) for {@link #GENADE} ticks is sent
 *       back to their last rest point ({@link Ring#terugNaarRustpunt} with the reason {@link #REDEN}): he only shoves,
 *       nobody is ever hurt.</li>
 *   <li>He is a Mika: a player who WEARS the Knabbelring walks right past him ({@link Ring#onzichtbaarVoorMikas}); he only
 *       sniffs ("ruik jij ook knabbel?"). A rock under the Elfenmanteltje he does not see either, but a rock does not
 *       get through the gap.</li>
 *   <li>Players whose story is done he lets be (by then the Mika's have had their bite too), and whoever sits at their
 *       own rest point ({@link Blik#bijRustpunt}).</li>
 * </ul>
 * He can't be hurt, pushed or led away.
 */
public class RoosterwachterEntity extends PathfinderMob implements GeoEntity {
    /** How far he sees, how wide (the cosine of half his field of view), and how long before he has you. */
    public static final double ZICHT = 6.0, KEGEL = 0.42;
    public static final int GENADE = 12;
    /** He turns his head this far to either side of where he stands, once per {@link #ZWAAI_TICKS} ticks. */
    public static final float ZWAAI = 32f;
    public static final int ZWAAI_TICKS = 120;
    /** The reason of {@link Ring#terugNaarRustpunt} (the text quest.guhs.ring.terug.poortwachter). */
    public static final String REDEN = "poortwachter";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh.idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    /** The way he faces on his post. */
    private float basisYaw;
    private boolean basisGezet;
    private final Map<UUID, Integer> gezien = new HashMap<>();
    private final Map<UUID, Long> gesnoven = new HashMap<>();

    public RoosterwachterEntity(EntityType<? extends RoosterwachterEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
    }

    /** He stands here and faces this way from now on. */
    public void zetPost(float yaw) {
        this.basisYaw = yaw;
        this.basisGezet = true;
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
    }

    public float basisYaw() {
        return basisYaw;
    }

    /** Does he see this player right now? */
    public boolean ziet(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || p.isCreative() || Hoofdstuk.voorbij(p) || Ring.onzichtbaarVoorMikas(p) || Gaven.isRots(p) || Blik.bijRustpunt(p)) {
            return false;
        }
        double afstand = distanceToSqr(p);
        if (afstand > ZICHT * ZICHT || Math.abs(p.getY() - getY()) > 3 || !hasLineOfSight(p)) {
            return false;
        }
        Vec3 naar = p.position().subtract(position()).multiply(1, 0, 1);
        return naar.lengthSqr() < 1.5 * 1.5 || naar.normalize().dot(Vec3.directionFromRotation(0, getYHeadRot())) > KEGEL;
    }

    /** How long he has been looking at this player (ticks). */
    public int gezien(ServerPlayer p) {
        return gezien.getOrDefault(p.getUUID(), 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!basisGezet) {
            zetPost(getYRot());
        }
        // on his post: body still, head left and right
        setYRot(basisYaw);
        setYBodyRot(basisYaw);
        setYHeadRot(basisYaw + ZWAAI * Mth.sin((tickCount + getId() * 17) * Mth.TWO_PI / ZWAAI_TICKS));
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) > 12 * 12) {
                gezien.remove(p.getUUID());
                continue;
            }
            if (Ring.onzichtbaarVoorMikas(p) && p.distanceToSqr(this) < 3.5 * 3.5 && !Hoofdstuk.voorbij(p)) {
                snuif(level, p);
            }
            boolean ziet = ziet(p) && Duwtje.mag(p);
            int g = gezien.getOrDefault(p.getUUID(), 0);
            g = ziet ? g + 1 : Math.max(0, g - 1);
            if (g <= 0) {
                gezien.remove(p.getUUID());
                continue;
            }
            gezien.put(p.getUUID(), g);
            if (ziet && g == 1) {
                p.sendOverlayMessage(Component.translatable("quest.guhs.ringh5.wachter.gezien").withStyle(ChatFormatting.RED));
                level.playSound(null, this, ModSounds.MIKA_AMBIENT.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
            }
            if (g >= GENADE) {
                gezien.remove(p.getUUID());
                Vec3 hier = position();
                if (Ring.terugNaarRustpunt(p, REDEN, hier)) {
                    Ring.behaald(p, "ring_h5_betrapt");
                }
            }
        }
    }

    /** A ring bearer slips past: he smells something, he just can't see what. */
    private void snuif(ServerLevel level, ServerPlayer p) {
        long nu = level.getGameTime();
        Long laatst = gesnoven.get(p.getUUID());
        if (laatst != null && nu - laatst < 200) {
            return;
        }
        gesnoven.put(p.getUUID(), nu);
        GuhQuests.say(p, this, "quest.guhs.ringh5.wachter.snuif." + random.nextInt(3));
        level.playSound(null, this, ModSounds.MIKA_AMBIENT.get(), SoundSource.NEUTRAL, 0.8f, 1.4f);
        Ring.behaald(p, "ring_h5_langs_wachters");
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && player instanceof ServerPlayer p) {
            playSound(ModSounds.MIKA_AMBIENT.get(), 1f, 0.8f);
            GuhQuests.say(p, this, Ring.klaar(p) ? "quest.guhs.ringh5.wachter.na." + random.nextInt(2) : "quest.guhs.ringh5.wachter.weg");
        }
        return InteractionResult.SUCCESS;
    }

    // --- he only shoves, and nothing shoves him ----------------------------------------------------------------------------

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
    protected SoundEvent getAmbientSound() {
        return random.nextInt(4) == 0 ? ModSounds.MIKA_AMBIENT.get() : null;
    }

    @Override
    public float getVoicePitch() {
        return 0.75f + random.nextFloat() * 0.1f;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("PostYaw", basisYaw);
        tag.putBoolean("PostGezet", basisGezet);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        basisYaw = tag.getFloatOr("PostYaw", 0f);
        basisGezet = tag.getBooleanOr("PostGezet", false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RoosterwachterEntity>("main", 4, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
