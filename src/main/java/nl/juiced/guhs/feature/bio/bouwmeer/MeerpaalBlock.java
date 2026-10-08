package nl.juiced.guhs.feature.bio.bouwmeer;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.Terugkeer;

/**
 * De meerpaal: the mooring post in the jetty of a botenhuisje, and the anchor of everything that lives there. It faces
 * the open water; the places of the boat's berth, the visser-guh and the jetty lantern are fixed steps from it (the same
 * numbers as the template of tools/features/bio_bouw_meer_bouw.py, turned with the block).
 * <p>
 * {@link #zorg} keeps the place in order whenever a player is near (BouwMeerEvents looks every two seconds) and when
 * somebody clicks the post:
 * <ul>
 *   <li>a roeibootje lies in the berth whenever the berth is empty (so each player finds one), never more than one empty
 *       boat there (a rower who parks beside a waiting boat does not leave a pile);</li>
 *   <li>the visser-guh sits on the end of the jetty (gone for good? a new one comes after it was seen missing twice);</li>
 *   <li>the lantern follows dusk and dawn at once.</li>
 * </ul>
 * Not obtainable and unbreakable in survival: it is part of the building.
 */
public class MeerpaalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<MeerpaalBlock> CODEC = simpleCodec(MeerpaalBlock::new);
    /** Steps from the post, in the template's frame: east, up, south (north = the open water). */
    public static final int[] LIGPLAATS = {2, 0, 0};
    public static final double[] VISSER = {-2.0, 1.0, -11.0};
    public static final int[] LANTAARN = {-4, 2, -11};
    /** How far below the post the water is looked for. */
    private static final int WATER_ZOEK = 4;
    static final String TERUG_VISSER = "bio_bouw_meer_visser";
    private static final VoxelShape VORM = Shapes.or(Block.box(0, 8, 0, 16, 16, 16), Block.box(5, 0, 5, 11, 24, 11));

    public MeerpaalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            boolean nieuw = zorg(server, pos) > 0;
            sp.sendOverlayMessage(Component.translatable(nieuw ? "gui.guhs.roeibootje.geroepen" : "gui.guhs.roeibootje.ligt_klaar")
                    .withStyle(ChatFormatting.AQUA));
            level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8f, 0.8f);
        }
        return InteractionResult.SUCCESS;
    }

    // --- places around the post ----------------------------------------------------------------------------------------------

    /** A block (east, up, south) steps from the anchor in the template's frame, for an anchor that faces {@code kijk}. */
    public static BlockPos plek(BlockPos anker, Direction kijk, int oost, int op, int zuid) {
        return anker.relative(kijk.getClockWise(), oost).relative(kijk, -zuid).above(op);
    }

    /** The same for a point: the middle of the anchor block plus the steps. */
    public static Vec3 punt(BlockPos anker, Direction kijk, double oost, double op, double zuid) {
        Direction rechts = kijk.getClockWise();
        return new Vec3(anker.getX() + 0.5 + rechts.getStepX() * oost - kijk.getStepX() * zuid, anker.getY() + op,
                anker.getZ() + 0.5 + rechts.getStepZ() * oost - kijk.getStepZ() * zuid);
    }

    /** The berth of this post: the top water block beside it (null: no water there). */
    @Nullable
    public static BlockPos ligplaats(Level level, BlockPos anker, Direction kijk) {
        BlockPos kolom = plek(anker, kijk, LIGPLAATS[0], LIGPLAATS[1], LIGPLAATS[2]);
        for (int dy = 0; dy <= WATER_ZOEK; dy++) {
            BlockPos p = kolom.below(dy);
            if (level.getFluidState(p).is(FluidTags.WATER) && !level.getFluidState(p.above()).is(FluidTags.WATER)) {
                return p;
            }
        }
        return null;
    }

    /** The empty roeibootjes in the berth of this post (nearest first). */
    public static List<RoeibootjeEntity> liggend(ServerLevel level, BlockPos ligplaats) {
        Vec3 m = Vec3.atCenterOf(ligplaats);
        List<RoeibootjeEntity> uit = level.getEntitiesOfClass(RoeibootjeEntity.class, new AABB(m, m).inflate(RoeibootjeEntity.THUIS_STRAAL, 2.5, RoeibootjeEntity.THUIS_STRAAL),
                b -> b.isAlive() && b.getPassengers().isEmpty());
        uit.sort(Comparator.comparingDouble(b -> b.distanceToSqr(m)));
        return uit;
    }

    // --- upkeep ---------------------------------------------------------------------------------------------------------------

    /**
     * Keeps this botenhuisje in order (see the class text). Cheap: three small entity lookups and one block. Returns how
     * many things it brought back (a boat, the visser-guh).
     */
    public static int zorg(ServerLevel level, BlockPos anker) {
        BlockState state = level.getBlockState(anker);
        if (!(state.getBlock() instanceof MeerpaalBlock)) {
            return 0;
        }
        Direction kijk = state.getValue(FACING);
        int nieuw = 0;
        // the boat
        BlockPos ligplaats = ligplaats(level, anker, kijk);
        if (ligplaats != null && level.isPositionEntityTicking(ligplaats) && level.areEntitiesLoaded(ChunkPos.pack(ligplaats))) {
            List<RoeibootjeEntity> liggend = liggend(level, ligplaats);
            for (int i = 1; i < liggend.size(); i++) {
                liggend.get(i).verdwijn(level);
            }
            if (liggend.isEmpty()) {
                RoeibootjeEntity boot = BouwMeerSlice.ROEIBOOTJE.get().create(level, EntitySpawnReason.TRIGGERED);
                if (boot != null) {
                    boot.zetThuis(ligplaats);
                    boot.setInitialPos(ligplaats.getX() + 0.5, ligplaats.getY() + 0.9, ligplaats.getZ() + 0.5);
                    boot.setYRot(kijk.toYRot());
                    level.addFreshEntity(boot);
                    nieuw++;
                }
            } else if (liggend.get(0).thuis() == null || !liggend.get(0).thuis().equals(ligplaats)) {
                liggend.get(0).zetThuis(ligplaats);          // (a boat of another mooring, or a summoned one, that was parked here)
            }
        }
        // the visser-guh
        Vec3 zit = punt(anker, kijk, VISSER[0], VISSER[1], VISSER[2]);
        BlockPos zitBlok = BlockPos.containing(zit);
        List<GuhNpcEntity> vissers = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(zit, zit).inflate(3, 3, 3),
                n -> n.getKind() == GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH && n.isAlive());
        vissers.sort(Comparator.comparingDouble(n -> n.distanceToSqr(zit)));
        for (int i = 1; i < vissers.size(); i++) {
            vissers.get(i).discard();
        }
        if (Terugkeer.moetTerug(level, TERUG_VISSER, zitBlok, !vissers.isEmpty(), 0)) {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            if (npc != null) {
                npc.setKind(GuhNpcEntity.Kind.BOTENHUISJE_VISSERGUH);
                npc.snapTo(zit.x, zit.y, zit.z, kijk.toYRot(), 0f);
                npc.setYHeadRot(kijk.toYRot());
                npc.setYBodyRot(kijk.toYRot());
                npc.setPersistenceRequired();
                level.addFreshEntity(npc);
                nieuw++;
            }
        }
        // the lantern
        BlockPos lantaarn = plek(anker, kijk, LANTAARN[0], LANTAARN[1], LANTAARN[2]);
        BlockState lamp = level.getBlockState(lantaarn);
        if (lamp.getBlock() instanceof SteigerlantaarnBlock blok) {
            blok.bijwerken(lamp, level, lantaarn);
        }
        return nieuw;
    }
}
