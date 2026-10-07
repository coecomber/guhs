package nl.juiced.guhs.feature.ringh6;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-h6): a Rookguhje of the Frituurberg (entity {@code guhs:ringh6_rookguh}): a little Rookguh the Mika's keep in a
 * cage as an extractor hood. It is drawn with the Rookguh's own model, much smaller (client.RingH6Client).
 * <ul>
 *   <li><b>Caged</b> ({@link #maak}; it also comes with the template): it hovers in its cage and can't be fed, hurt or
 *       moved. It only exists for players who did not open its cage yet (ring-kern's {@link Zicht}: steps 0..nr of the
 *       questline), so every player finds all three and nobody frees one for somebody else. {@code Bezetting} keeps one in
 *       every cage of every copy.</li>
 *   <li><b>Free</b> ({@link #vrij}): what the player who opened the lock sees: their own Rookguhje that wriggles out,
 *       spins up through the smoke with a trail of hearts and is gone (never saved).</li>
 *   <li><b>At home</b> ({@link #thuis}; it also comes with the template): afterwards the three live on the mountain, free:
 *       each hovers over the roof of its old cage, only for players whose story is done, and thanks them when clicked
 *       ("structures stay open, characters live there with new chats").</li>
 * </ul>
 */
public class GekooideRookguhEntity extends RookguhEntity {
    /** Ticks a freed Rookguhje takes to float away. */
    public static final int VLIEGT = 70;
    private int nr = 1;
    private int vrijTicks = -1;
    private boolean thuis;
    @Nullable
    private UUID bevrijder;

    public GekooideRookguhEntity(EntityType<? extends GekooideRookguhEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    /** The Rookguhje of cage {@code nr} (1..3): not yet added to the world. */
    @Nullable
    public static GekooideRookguhEntity maak(ServerLevel level, Vec3 plek, int nr) {
        GekooideRookguhEntity guh = RingH6Feature.ROOKGUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.nr = nr;
        guh.snapTo(plek.x, plek.y + 0.4, plek.z, 0f, 0f);
        guh.setInvulnerable(true);
        Zicht.alleenBij(guh, RingH6Feature.LIJN.id(), 0, nr);
        return guh;
    }

    /** The Rookguhje of cage {@code nr} as it lives on the mountain after the story (for players who are done): not yet added. */
    @Nullable
    public static GekooideRookguhEntity thuis(ServerLevel level, Vec3 plek, int nr) {
        GekooideRookguhEntity guh = RingH6Feature.ROOKGUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.nr = nr;
        guh.thuis = true;
        guh.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        guh.setInvulnerable(true);
        Zicht.alleenBij(guh, RingH6Feature.LIJN.id(), RingH6Feature.LIJN.stappen(), 99);
        return guh;
    }

    public boolean isThuis() {
        return thuis;
    }

    /** The Rookguhje this player just freed: only they see it, it floats away by itself. */
    @Nullable
    public static GekooideRookguhEntity vrij(ServerPlayer p, Vec3 plek, int nr) {
        ServerLevel level = p.level();
        GekooideRookguhEntity guh = RingH6Feature.ROOKGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return null;
        }
        guh.nr = nr;
        guh.vrijTicks = 0;
        guh.bevrijder = p.getUUID();
        guh.snapTo(plek.x, plek.y + 0.4, plek.z, 0f, 0f);
        Zicht.alleenVoor(guh, p.getUUID());
        level.addFreshEntity(guh);
        return guh;
    }

    public int nr() {
        return nr;
    }

    public boolean isVrij() {
        return vrijTicks >= 0;
    }

    @Override
    protected void registerGoals() {
        // (none: a caged Rookguhje hovers where it is; a free one is moved by tick)
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (vrijTicks < 0) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        // free: out between the bars, then up and away, spinning
        noPhysics = true;
        vrijTicks++;
        setDeltaMovement(new Vec3(0, vrijTicks < 14 ? 0.05 : 0.1 + vrijTicks * 0.004, vrijTicks < 14 ? 0.12 : 0.0));
        setYRot(getYRot() + 14f);
        yBodyRot = getYRot();
        ServerPlayer p = bevrijder == null ? null : level.getServer().getPlayerList().getPlayer(bevrijder);
        if (p != null && p.level() == level && vrijTicks % 3 == 0) {
            level.sendParticles(p, ParticleTypes.HEART, false, false, getX(), getY() + getBbHeight(), getZ(), 1, 0.3, 0.2, 0.3, 0);
            level.sendParticles(p, ParticleTypes.CLOUD, false, false, getX(), getY() + 0.2, getZ(), 2, 0.25, 0.1, 0.25, 0.01);
        }
        if (vrijTicks >= VLIEGT || p == null) {
            if (p != null && p.level() == level) {
                level.sendParticles(p, ParticleTypes.FIREWORK, false, false, getX(), getY() + 0.5, getZ(), 14, 0.4, 0.4, 0.4, 0.08);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.NEUTRAL, 0.6f, 1.5f);
            }
            discard();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND && !isVrij() && player instanceof ServerPlayer p) {
            playSound(thuis ? ModSounds.GUH_HAPPY.get() : ModSounds.GUH_AMBIENT.get(), 0.8f, getVoicePitch());
            if (thuis) {
                GuhQuests.say(p, this, "quest.guhs.ringh6.rookguh.dank." + nr);
            } else {
                p.sendOverlayMessage(Component.translatable("quest.guhs.ringh6.rookguh.zielig").withStyle(ChatFormatting.GRAY));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A Rookguhje is never fed: it is freed. (No knabbels wasted, no "saved Rookguh" to farm.) */
    @Override
    public void feed(@Nullable ServerPlayer feeder) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return !isVrij() && super.shouldBeSaved();
    }

    @Override
    public float getVoicePitch() {
        return 1.5f + random.nextFloat() * 0.2f;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Nr", nr);
        tag.putBoolean("Thuis", thuis);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        nr = Math.max(1, Math.min(3, tag.getIntOr("Nr", 1)));
        thuis = tag.getBooleanOr("Thuis", false);
    }
}
