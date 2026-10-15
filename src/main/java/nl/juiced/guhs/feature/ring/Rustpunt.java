package nl.juiced.guhs.feature.ring;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-kern): the rest points of the trip (DESIGN_130 4: "small rest points in between: campfire, sign, sleeping
 * bag"). A rest point is a <b>Rustvuurtje</b> (block {@code guhs:ring_rustvuur}, {@link Vuur}): a little cooking fire with
 * Sam-guh's pot on it. It never burns anybody.
 * <ul>
 *   <li>Walk up to one (within {@link #BEREIK} blocks) or click it while you are on the trip: it becomes your last rest
 *       point ({@code Rustpunten} kind {@link Ring#RUST}), the place the Eye and the Nine put you back on
 *       ({@link Ring#terugNaarRustpunt}), and Sam-guh cooks ({@link Sam#kook}).</li>
 *   <li>Where they stand: the structure {@code guhs:ring_rustpunt} (a little camp, scattered all over the Barbecuether:
 *       tools/features/ring_bouw.py), and every chapter structure has one or more (the chapter slices put the block in their
 *       templates; a chapter may also call {@link Ring#rustpunt(ServerPlayer, Vec3, float)} for a spot of its own).</li>
 *   <li>Coming through the grill portal on the trip, the spot where you arrive is a rest point too (so there always is one).</li>
 * </ul>
 */
public final class Rustpunt {
    /** A Rustvuurtje within this many blocks counts as "reached". */
    public static final int BEREIK = 4;
    /** Player saved data: the fires this player has rested at (a counter and the last few positions). */
    static final String GETELD = "guhs_ring_rustpunten", LAATSTE = "guhs_ring_rustvuur";

    /** The rest fire. Looks like a small campfire with a pot; gives light; a click or walking up to it makes it your rest point. */
    public static class Vuur extends Block {
        private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 9, 15);

        public Vuur(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                rust(p, pos, true);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(10) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                        0.4f + random.nextFloat() * 0.3f, 0.8f + random.nextFloat() * 0.5f, false);
            }
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 0.85,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.04, 0);
            }
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.2, pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                        0, 0.01, 0);
            }
        }
    }

    /** (every second, a player on the trip) a Rustvuurtje near enough? Then it is their rest point. */
    static void zoek(ServerPlayer p) {
        if (!Ring.opReis(p) || p.isSpectator()) {
            return;
        }
        ServerLevel level = p.level();
        BlockPos hier = p.blockPosition();
        BlockPos laatste = laatste(p);
        if (laatste != null && laatste.distSqr(hier) <= (BEREIK + 2) * (BEREIK + 2) && level.getBlockState(laatste).getBlock() instanceof Vuur) {
            return;   // (still at the fire they rest at)
        }
        for (BlockPos pos : BlockPos.betweenClosed(hier.offset(-BEREIK, -2, -BEREIK), hier.offset(BEREIK, 2, BEREIK))) {
            if (level.getBlockState(pos).getBlock() instanceof Vuur) {
                rust(p, pos.immutable(), false);
                return;
            }
        }
    }

    /** The fire this player rested at last (in this dimension), or null. */
    @Nullable
    private static BlockPos laatste(ServerPlayer p) {
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(LAATSTE);
        return t.isEmpty() || !t.getStringOr("Dim", "").equals(p.level().dimension().identifier().toString()) ? null : BlockPos.of(t.getLongOr("Pos", 0L));
    }

    /**
     * This player rests at this fire. On the trip: it is their rest point from now on (they will stand where they stand
     * now, or next to the fire), Sam-guh cooks. Not on the trip: a cosy word, when they clicked.
     */
    public static void rust(ServerPlayer p, BlockPos vuur, boolean klik) {
        ServerLevel level = p.level();
        if (!Ring.opReis(p)) {
            if (klik) {
                p.sendOverlayMessage(Component.translatable(Ring.klaar(p) ? "quest.guhs.ring.rustpunt.herinnering" : "quest.guhs.ring.rustpunt.gezellig")
                        .withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        BlockPos laatste = laatste(p);
        boolean nieuw = laatste == null || !laatste.equals(vuur);
        Vec3 sta = p.onGround() && vuur.distToCenterSqr(p.position()) <= (BEREIK + 2.5) * (BEREIK + 2.5) && vuur.distToCenterSqr(p.position()) > 1.5
                ? p.position() : naastVuur(level, vuur);
        Ring.rustpunt(p, sta, p.getYRot());
        if (nieuw) {
            CompoundTag saved = GuhQuests.saved(p);
            CompoundTag t = new CompoundTag();
            t.putString("Dim", level.dimension().identifier().toString());
            t.putLong("Pos", vuur.asLong());
            saved.put(LAATSTE, t);
            int n = saved.getIntOr(GETELD, 0) + 1;
            saved.putInt(GETELD, n);
            p.sendSystemMessage(Component.translatable("quest.guhs.ring.rustpunt.nieuw").withStyle(ChatFormatting.GOLD));
            level.playSound(null, vuur, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.2f, 1.0f);
            level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, vuur.getX() + 0.5, vuur.getY() + 1.0, vuur.getZ() + 0.5, 8, 0.5, 0.4, 0.5, 0);
            Ring.behaald(p, "ring_rustpunt");
            if (n >= 5) {
                Ring.behaald(p, "ring_rustpunt_5");
            }
            Sam.kook(p, vuur);
        } else if (klik) {
            if (Sam.alGekookt(p, vuur)) {
                p.sendOverlayMessage(Component.translatable("quest.guhs.ring.rustpunt.al").withStyle(ChatFormatting.GOLD));
            } else {
                Sam.kook(p, vuur);
            }
        }
    }

    /** A place to stand next to the fire. */
    private static Vec3 naastVuur(ServerLevel level, BlockPos vuur) {
        for (BlockPos pos : new BlockPos[]{vuur.south(2), vuur.north(2), vuur.east(2), vuur.west(2), vuur.south(), vuur.north(), vuur.east(), vuur.west()}) {
            for (int dy = 1; dy >= -1; dy--) {
                BlockPos q = pos.above(dy);
                if (level.getBlockState(q.below()).blocksMotion() && !level.getBlockState(q).blocksMotion() && !level.getBlockState(q.above()).blocksMotion()) {
                    return Vec3.atBottomCenterOf(q);
                }
            }
        }
        return Vec3.atBottomCenterOf(vuur.above());
    }

    /** How many different rest fires this player has rested at. */
    public static int aantal(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(GETELD, 0);
    }

    /**
     * Arrived in the Barbecuether on the trip: the spot of arrival is a rest point when the player has none in this
     * dimension yet (so the Nine always have somewhere to put them back).
     */
    static void aangekomen(ServerPlayer p) {
        if (!Ring.opReis(p)) {
            return;
        }
        Rustpunten.Punt punt = Ring.rustpunt(p);
        if (punt == null || punt.dim() != p.level().dimension()) {
            Ring.rustpunt(p, p.position(), p.getYRot());
        }
    }

    private Rustpunt() {
    }
}
