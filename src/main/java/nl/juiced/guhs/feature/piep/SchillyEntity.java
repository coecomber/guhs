package nl.juiced.guhs.feature.piep;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * Schilly: Poepschilly's look-alike (a slightly different green turtle plush), but NOT a kontpoetser. Schilly is a
 * <i>minihoofdje</i>: Schilly and the guhs are real besties, and now and then they have a bit of beef.
 * <ul>
 *   <li>Near a guh it waddles over ({@link BestieGoal}): usually a bestie moment (hearts, its happy animation, the guh
 *       hearts back). About 1 in {@link #BEEF_KANS} times (not more often than every {@link #BEEF_RUST} ticks) they have
 *       <b>beef</b>: both turn their backs, little angry puffs, "Schilly en guh hebben beef... (minihoofdje!)", and after
 *       {@link #BEEF_TICKS} ticks they make up with hearts ("...toch besties").</li>
 *   <li>Tame it like Poepschilly (zeewier or a kaasknabbel); tamed it follows you. "Bestie-moment!" in your Schilly's menu, then click one of your
 *       guhs: a <b>bestie-moment</b>, both get "Besties" (a little regeneration) for a while; then Schilly rests.</li>
 * </ul>
 * Everything else (swimming, taming, following, the spawns on the guhzee coasts) comes from {@link PoepschillyEntity}.
 */
public class SchillyEntity extends PoepschillyEntity {
    public static final int BEEF_KANS = 4, BEEF_TICKS = 70, BEEF_RUST = 20 * 60 * 2, BESTIE_RUST = 20 * 60 * 3, BESTIES_TICKS = 20 * 30;

    private long volgendeBeef;

    public SchillyEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    public String soort() {
        return "schilly";
    }

    @Override
    public List<PiepInstelling> instellingen() {
        return PiepInstelling.SCHILLY;
    }

    @Override
    public net.minecraft.world.item.Item oppakItem() {
        return PiepFeature.SCHILLY_ITEM.get();
    }

    @Override
    protected String adv() {
        return "piep_schilly2";
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(4, new BestieGoal());
    }

    /** Clicked on one of your guhs while ready: a bestie-moment (no poetsbeurt: Schilly doesn't do that). */
    @Override
    protected void opGuh(GuhEntity guh, ServerPlayer player) {
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new PiepPayloads.Klaar(getId(), 0));
        bestieMoment(guh, player);
    }

    /** Schilly and this guh are besties: hearts, the happy animation, "Besties" for both; Schilly rests a while. */
    public void bestieMoment(GuhEntity guh, @Nullable ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        getLookControl().setLookAt(guh);
        triggerAnim("actie", "blij");
        guh.triggerAnim("action", "happy");
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 4, 0.3, 0.2, 0.3, 0);
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 4, 0.3, 0.2, 0.3, 0);
        level.playSound(null, blockPosition(), SoundEvents.TURTLE_AMBIENT_LAND, SoundSource.NEUTRAL, 1f, 1.5f);
        guh.addEffect(new MobEffectInstance(PiepFeature.BESTIES, BESTIES_TICKS, 0));
        addEffect(new MobEffectInstance(PiepFeature.BESTIES, BESTIES_TICKS, 0));
        rust(BESTIE_RUST);
        if (player != null) {
            player.displayClientMessage(Component.translatable("gui.guhs.piep.bestie_moment", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            PiepVoortgang.tel(player, PiepVoortgang.BESTIES, 1, "piep_schilly_bestie");
        }
    }

    /** Beef with this guh: backs turned, angry puffs, a message for everyone around. */
    public void beefBegin(GuhEntity guh) {
        ServerLevel level = (ServerLevel) level();
        float naarGuh = (float) (Mth.atan2(guh.getZ() - getZ(), guh.getX() - getX()) * Mth.RAD_TO_DEG) - 90f;
        draai(this, naarGuh + 180f);
        draai(guh, naarGuh);                                    // (the guh looks the same way: away from Schilly)
        GuhHooks.bezig(guh, BEEF_TICKS + 10);
        guh.getNavigation().stop();
        getNavigation().stop();
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 0.5, getZ(), 2, 0.2, 0.1, 0.2, 0);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 2, 0.2, 0.1, 0.2, 0);
        level.playSound(null, blockPosition(), SoundEvents.TURTLE_HURT, SoundSource.NEUTRAL, 0.6f, 1.6f);
        for (ServerPlayer p : spelersBij(level)) {
            p.displayClientMessage(Component.translatable("gui.guhs.piep.beef", guh.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** ...and they make up: hearts, and "...toch besties" (counts as a beef bijgelegd for everyone who saw it). */
    public void beefBijgelegd(GuhEntity guh) {
        ServerLevel level = (ServerLevel) level();
        triggerAnim("actie", "blij");
        guh.triggerAnim("action", "happy");
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 3, 0.3, 0.2, 0.3, 0);
        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 3, 0.3, 0.2, 0.3, 0);
        for (ServerPlayer p : spelersBij(level)) {
            p.displayClientMessage(Component.translatable("gui.guhs.piep.beef_bijgelegd").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            PiepVoortgang.tel(p, PiepVoortgang.BEEF, 1, "piep_schilly_beef");
            PiepVoortgang.pagina(p, soort());
        }
        GuhHooks.bezig(guh, 0);
    }

    private List<ServerPlayer> spelersBij(ServerLevel level) {
        return level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(16));
    }

    private static void draai(net.minecraft.world.entity.LivingEntity e, float yaw) {
        e.setYRot(yaw);
        e.setYBodyRot(yaw);
        e.setYHeadRot(yaw);
        e.yRotO = yaw;
    }

    /** Now and then: waddle to a guh nearby, then a bestie moment or (sometimes) beef, and making up after. */
    class BestieGoal extends Goal {
        @Nullable
        private GuhEntity guh;
        private int ticks = -1;
        private boolean beef;
        private long volgendeKeer;

        BestieGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            long now = level().getGameTime();
            if (now < volgendeKeer || isBinnen() || isOrderedToSit() || !aan(PiepInstelling.BESTIES) || isBezig()) {
                return false;
            }
            volgendeKeer = now + 200 + getRandom().nextInt(400);
            List<GuhEntity> guhs = level().getEntitiesOfClass(GuhEntity.class, getBoundingBox().inflate(8, 3, 8),
                    g -> g.isAlive() && !g.isVehicle() && !GuhHooks.isBezig(g));
            if (guhs.isEmpty()) {
                return false;
            }
            guh = guhs.get(getRandom().nextInt(guhs.size()));
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return guh != null && guh.isAlive() && ticks < 200 + BEEF_TICKS;
        }

        @Override
        public void start() {
            ticks = 0;
            beef = false;
            getNavigation().moveTo(guh, 1.1);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (guh == null) {
                return;
            }
            ticks++;
            if (beef) {
                getNavigation().stop();
                if (ticks >= BEEF_TICKS) {
                    beefBijgelegd(guh);
                    guh = null;
                }
                return;
            }
            getLookControl().setLookAt(guh);
            if (distanceToSqr(guh) < 2.2 * 2.2) {
                long now = level().getGameTime();
                if (now >= volgendeBeef && getRandom().nextInt(BEEF_KANS) == 0) {
                    volgendeBeef = now + BEEF_RUST;
                    beef = true;
                    ticks = 0;
                    beefBegin(guh);
                } else {
                    triggerAnim("actie", "blij");
                    ServerLevel level = (ServerLevel) level();
                    level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 2, 0.2, 0.1, 0.2, 0);
                    if (getRandom().nextBoolean()) {
                        level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.2, 0.1, 0.2, 0);
                    }
                    guh = null;
                }
            } else if (ticks % 20 == 0) {
                getNavigation().moveTo(guh, 1.1);
            }
        }

        @Override
        public void stop() {
            if (beef && guh != null) {
                beefBijgelegd(guh);
            }
            guh = null;
            ticks = -1;
        }
    }

    /** (Tests) beef may come right away. */
    void vergeetBeefRust() {
        volgendeBeef = 0;
    }
}
