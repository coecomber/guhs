package nl.juiced.guhs.feature.bleekwoud;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A creature of a guh heart (the {@link KraakguhEntity} and the {@link KraakMikaEntity}): bound to its heart, and it only
 * moves while no player looks at it (vanilla's Creaking rules). The shared parts live here.
 */
public interface KraakWezen {
    /** A player further away than this is not checked for looking (and can't make it freeze). */
    double KIJK_STRAAL = 32.0;
    /** Past this far from its heart it won't path (vanilla: 32). */
    int THUIS_STRAAL = GuhhartjeBlockEntity.STRAAL;

    /** The heart it belongs to (null: a free one, from a spawn egg). */
    @Nullable
    BlockPos hart();

    /** Binds it to its heart (GuhhartjeBlockEntity calls this right after making it). */
    void bind(BlockPos hart);

    /** May it move right now (nobody looks, and it isn't resting after a hug)? Synced: the client freezes its pose too. */
    boolean magBewegen();

    /** It crumbles away: wood and heart crumbs, a creak, gone. */
    void verkruimel();

    /** A player has been standing inside it for a while (it would trap them): its heart takes it away. */
    boolean spelerZitVast();

    /**
     * Is a player looking at this mob? Vanilla's own gaze check ({@code LivingEntity#isLookingAtMe}: a 60 degree cone at its
     * eyes, middle and feet, never through walls; see-through blocks don't hide it), for every survival/adventure player
     * within {@link #KIJK_STRAAL} blocks. Creative players and spectators don't count.
     */
    static boolean bekeken(Mob mob) {
        for (Player player : mob.level().players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive() || player.distanceToSqr(mob) > KIJK_STRAAL * KIJK_STRAAL) {
                continue;
            }
            if (mob.isLookingAtMe(player, 0.5, false, true, mob.getEyeY(), mob.getY() + 0.5 * mob.getScale(), (mob.getEyeY() + mob.getY()) / 2.0)) {
                return true;
            }
        }
        return false;
    }

    /** Does its heart still stand, and is it still that heart's creature? (An unloaded heart: yes, wait.) */
    static boolean heeftHart(Mob mob, BlockPos hart) {
        if (!mob.level().isLoaded(hart)) {
            return true;
        }
        return mob.level().getBlockEntity(hart) instanceof GuhhartjeBlockEntity be && be.isVan(mob);
    }

    @Nullable
    static GuhhartjeBlockEntity hartVan(Mob mob, @Nullable BlockPos hart) {
        return hart != null && mob.level().isLoaded(hart) && mob.level().getBlockEntity(hart) instanceof GuhhartjeBlockEntity be && be.isVan(mob) ? be : null;
    }

    /** The crumbs of a creature that falls apart: pale wood and a little of its heart. */
    static void kruimels(Mob mob, boolean verzuurd) {
        if (!(mob.level() instanceof ServerLevel server)) {
            return;
        }
        AABB box = mob.getBoundingBox();
        Vec3 c = box.getCenter();
        BlockState hout = BleekwoudFeature.BLEEKHOUT_STAM.get().defaultBlockState();
        BlockState hartje = (verzuurd ? BleekwoudFeature.VERZUURD_GUHHARTJE : BleekwoudFeature.KRAKEND_GUHHARTJE).get().defaultBlockState()
                .setValue(GuhhartjeBlock.STATE, CreakingHeartState.AWAKE);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, hout), c.x, c.y, c.z, 60, box.getXsize() * 0.3, box.getYsize() * 0.3,
                box.getZsize() * 0.3, 0.0);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK_CRUMBLE, hartje), c.x, c.y, c.z, 8, box.getXsize() * 0.3, box.getYsize() * 0.3,
                box.getZsize() * 0.3, 0.0);
    }

    /** A player whose eyes are inside this mob's box. */
    static boolean spelerErin(Mob mob) {
        AABB box = mob.getBoundingBox();
        for (Player player : mob.level().players()) {
            if (!player.isSpectator() && player.distanceToSqr(mob) < 9.0 && box.contains(player.getEyePosition())) {
                return true;
            }
        }
        return false;
    }

    // --- the controls: they only work while it may move (vanilla's Creaking does exactly this) ---------------------------------

    final class Beweeg extends MoveControl {
        private final KraakWezen w;

        public <M extends Mob & KraakWezen> Beweeg(M mob) {
            super(mob);
            this.w = mob;
        }

        @Override
        public void tick() {
            if (w.magBewegen()) {
                super.tick();
            }
        }
    }

    final class Kijk extends LookControl {
        private final KraakWezen w;

        public <M extends Mob & KraakWezen> Kijk(M mob) {
            super(mob);
            this.w = mob;
        }

        @Override
        public void tick() {
            if (w.magBewegen()) {
                super.tick();
            }
        }
    }

    final class Spring extends JumpControl {
        private final KraakWezen w;
        private final Mob zelf;

        public <M extends Mob & KraakWezen> Spring(M mob) {
            super(mob);
            this.w = mob;
            this.zelf = mob;
        }

        @Override
        public void tick() {
            if (w.magBewegen()) {
                super.tick();
            } else {
                zelf.setJumping(false);
            }
        }
    }

    final class Lijf extends BodyRotationControl {
        private final KraakWezen w;

        public <M extends Mob & KraakWezen> Lijf(M mob) {
            super(mob);
            this.w = mob;
        }

        @Override
        public void clientTick() {
            if (w.magBewegen()) {
                super.clientTick();
            }
        }
    }

    /** Walks like any ground mob, but never plans a path further than {@link #THUIS_STRAAL} from its heart. */
    final class Navigatie extends GroundPathNavigation {
        public Navigatie(Mob mob, Level level) {
            super(mob, level);
        }

        @Override
        public void tick() {
            if (!(mob instanceof KraakWezen w) || w.magBewegen()) {
                super.tick();
            }
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            this.nodeEvaluator = new ThuisEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }
    }

    final class ThuisEvaluator extends WalkNodeEvaluator {
        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            BlockPos home = mob instanceof KraakWezen w ? w.hart() : null;
            if (home == null) {
                return super.getPathType(context, x, y, z);
            }
            double d = home.distSqr(new net.minecraft.core.Vec3i(x, y, z));
            return d > THUIS_STRAAL * THUIS_STRAAL && d >= home.distSqr(context.mobPosition()) ? PathType.BLOCKED : super.getPathType(context, x, y, z);
        }
    }

    /** The nearest player it may sneak up on: survival/adventure, alive, within {@code range}, and not too far from its heart. */
    @Nullable
    static Player doelwit(Mob mob, @Nullable BlockPos hart, double range) {
        Player best = null;
        double bestD = range * range;
        for (Player player : mob.level().players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double d = player.distanceToSqr(mob);
            if (d < bestD && (hart == null || player.blockPosition().closerThan(hart, THUIS_STRAAL - 2))) {
                best = player;
                bestD = d;
            }
        }
        return best;
    }

    static boolean isLevend(@Nullable LivingEntity e) {
        return e != null && e.isAlive() && !e.isRemoved();
    }
}
