package nl.juiced.guhs.feature.piep;

import java.util.EnumSet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Boze Oppernabbel: the big angry kaasknabbel of the nest, with a crown and a boss bar. A challenge, but doable:
 * <ul>
 *   <li>Bumps hard ({@code attack 5}) and now and then a big <b>stamp</b> ({@link StampGoal}): a jump and a slam that knocks
 *       players around it back (a warning squash first, so you can jump away).</li>
 *   <li>At half health it calls {@link #HULP} little knabbels (once).</li>
 *   <li>It breaks no blocks at all (the slam is only a shockwave), and never despawns.</li>
 * </ul>
 * Beaten it says "Hij was gewoon zieli..." ({@link KaasknabbelNest} gives the treasure).
 */
public class BozeOppernabbelEntity extends BozeKaasknabbelEntity {
    public static final int HULP = 3;
    public static final float STAMP_SCHADE = 5f;
    public static final double STAMP_BEREIK = 4.5;
    private final ServerBossEvent bossbalk = (ServerBossEvent) new ServerBossEvent(Component.translatable("entity.guhs.boze_oppernabbel"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(false);
    private boolean hulpGeroepen;

    public BozeOppernabbelEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 110.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new StampGoal());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossbalk.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossbalk.removePlayer(player);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossbalk.setProgress(getHealth() / getMaxHealth());
        if (!hulpGeroepen && getHealth() < getMaxHealth() / 2 && level() instanceof ServerLevel level) {
            hulpGeroepen = true;
            KaasknabbelNest.roepHulp(level, this, HULP);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossbalk.removeAllPlayers();
        if (level() instanceof ServerLevel level) {
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(40))) {
                p.sendSystemMessage(Component.translatable("gui.guhs.piep.gewoon_zieli").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            }
            KaasknabbelNest.bossVerslagen(level, this);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossbalk.removeAllPlayers();
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 0.65f;
    }

    @Override
    protected float getSoundVolume() {
        return 1.4f;
    }

    /** Squash, jump, slam: everyone within {@link #STAMP_BEREIK} blocks on the ground is knocked back (no blocks break). */
    class StampGoal extends Goal {
        private int ticks = -1;
        private long volgende;

        StampGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            long now = level().getGameTime();
            if (target == null || now < volgende || !onGround() || distanceToSqr(target) > 8 * 8) {
                return false;
            }
            volgende = now + 120 + getRandom().nextInt(60);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return ticks >= 0 && ticks < 22;
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().stop();
            triggerAnim("actie", "stamp");
            playSound(PiepFeature.KNABBEL_BOOS.get(), 1.6f, 0.5f);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ticks++;
            if (ticks == 6) {
                setDeltaMovement(getDeltaMovement().add(0, 0.62, 0));
            }
            if (ticks == 14 && level() instanceof ServerLevel level) {
                slam(level);
            }
        }

        @Override
        public void stop() {
            ticks = -1;
        }
    }

    /** Does the stamp reach this player? Not in creative or spectator, and not when they jumped over it. */
    static boolean geraakt(Player p) {
        return !p.isCreative() && !p.isSpectator() && p.onGround();
    }

    /** The stamp hits: damage, and a push up and away from the Oppernabbel. */
    void duw(Player p) {
        p.hurt(damageSources().mobAttack(this), STAMP_SCHADE);
        Vec3 weg = p.position().subtract(position()).multiply(1, 0, 1);
        weg = weg.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : weg.normalize();
        p.push(weg.x * 1.1, 0.45, weg.z * 1.1);
        p.hurtMarked = true;
    }

    /** The shockwave of a stamp (also called by the tests). */
    void slam(ServerLevel level) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.YELLOW_CONCRETE_POWDER.defaultBlockState()), getX(), getY() + 0.1, getZ(),
                60, STAMP_BEREIK * 0.5, 0.1, STAMP_BEREIK * 0.5, 0.2);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6f, 1.6f);
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(STAMP_BEREIK, 1.5, STAMP_BEREIK))) {
            if (geraakt(p)) {
                duw(p);
            }
        }
    }
}
