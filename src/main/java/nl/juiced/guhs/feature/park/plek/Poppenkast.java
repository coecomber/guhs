package nl.juiced.guhs.feature.park.plek;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.4.2 (park-plekjes b): the stage of the poppenkast (plekje poppenkast). Right-click it (with anything in your hand) and
 * the show plays for you: the talking screen, page by page ({@link #WIE}, texts gui.guhs.parkb.poppenkast.1 ..
 * {@value #SHOW}): a guh puts down his big kaasknabbel, a Mika pops up to steal it, the guhkindjes shout "ACHTER JE!", and
 * in the end they share. Closing the screen (read to the end) = applause and the hidden advancement parkb_poppenkast.
 * <p>
 * In the world, for everybody around: the curtains open and the two puppets appear ({@link #OPEN}), the guhkindjes on the
 * benches jump up and shout "ACHTER JE!" twice, then clap, and after {@link #DUUR} ticks the curtains close again
 * (scheduled block ticks; a show that is running is not started again by the next click, it just plays on).
 */
public class Poppenkast extends HorizontalDirectionalBlock {
    public static final MapCodec<Poppenkast> CODEC = simpleCodec(Poppenkast::new);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** The pages of the show and who speaks on each (tools/features/park_plek_b.py POPPENKAST: the same order). */
    public static final int SHOW = 11;
    static final String[] WIE = {"verteller", "guh", "guh", "mika", "kinderen", "guh", "kinderen", "guh", "mika", "guh", "verteller"};
    /** How long the curtains stay open; when the guhkindjes shout and clap (ticks after the start). */
    public static final int DUUR = 220, TIK = 20;
    static final int[] ROEP = {60, 120};
    static final int KLAP = 180;
    public static final String SLEUTEL = "parkb_poppenkast";

    /** Running shows: the stage -> the game time it started. */
    private static final Map<GlobalPos, Long> SHOWS = new ConcurrentHashMap<>();

    public Poppenkast(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            speel(server, pos, sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;   // (the show plays whatever you hold)
    }

    /** The show for this player (their talking screen), and on the stage for everybody (when it is not playing already). */
    public static void speel(ServerLevel level, BlockPos pos, ServerPlayer p) {
        List<Praat.Regel> regels = new ArrayList<>();
        for (int i = 1; i <= SHOW; i++) {
            regels.add(new Praat.Regel(null, "gui.guhs.parkb.poppenkast.naam." + WIE[i - 1], "gui.guhs.parkb.poppenkast." + i));
        }
        Praat.scene(p, SLEUTEL, regels);
        GlobalPos waar = GlobalPos.of(level.dimension(), pos.immutable());
        if (SHOWS.putIfAbsent(waar, level.getGameTime()) == null) {
            BlockState s = level.getBlockState(pos);
            if (s.getBlock() instanceof Poppenkast) {
                level.setBlock(pos, s.setValue(OPEN, true), Block.UPDATE_ALL);
                level.scheduleTick(pos, s.getBlock(), TIK);
            }
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
        }
    }

    /** Is a show playing on this stage? */
    public static boolean speelt(ServerLevel level, BlockPos pos) {
        return SHOWS.containsKey(GlobalPos.of(level.dimension(), pos.immutable()));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GlobalPos waar = GlobalPos.of(level.dimension(), pos.immutable());
        Long begin = SHOWS.get(waar);
        long na = begin == null ? DUUR : level.getGameTime() - begin;
        if (na >= DUUR) {
            SHOWS.remove(waar);
            if (state.getValue(OPEN)) {
                level.setBlock(pos, state.setValue(OPEN, false), Block.UPDATE_ALL);
            }
            return;
        }
        for (int t : ROEP) {
            if (na >= t && na < t + TIK) {
                roep(level, pos);
            }
        }
        if (na >= KLAP && na < KLAP + TIK) {
            klap(level, pos);
        }
        level.scheduleTick(pos, this, TIK);
    }

    /** "ACHTER JE!": the guhkindjes round the stage jump up; everybody near reads it. */
    static void roep(ServerLevel level, BlockPos pos) {
        for (GuhNpcEntity kind : kindjes(level, pos)) {
            kind.setDeltaMovement(kind.getDeltaMovement().add(0, 0.32, 0));
            kind.hurtMarked = true;
            level.sendParticles(ParticleTypes.NOTE, kind.getX(), kind.getY() + 1.0, kind.getZ(), 2, 0.2, 0.1, 0.2, 0.5);
        }
        level.playSound(null, pos, nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.5f);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(14))) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.parkb.poppenkast.achter_je").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        }
    }

    /** The end: the guhkindjes clap. */
    static void klap(ServerLevel level, BlockPos pos) {
        for (GuhNpcEntity kind : kindjes(level, pos)) {
            level.sendParticles(ParticleTypes.HEART, kind.getX(), kind.getY() + 1.0, kind.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
        }
        level.playSound(null, pos, PlekB.APPLAUS.get(), SoundSource.NEUTRAL, 1f, 1f);
    }

    /** The guhkindjes near this stage. */
    static List<GuhNpcEntity> kindjes(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos).inflate(10, 5, 10), n -> n.getKind() == GuhNpcEntity.Kind.GUHKINDJE);
    }

    /** (PlekB) the end of the show in the talking screen: applause, the advancement. */
    static void luister() {
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
            if (optie < 0) {
                afgelopen(p);
            }
        });
    }

    /** The show was watched to the end (or closed). */
    public static void afgelopen(ServerPlayer p) {
        p.sendOverlayMessage(Component.translatable("gui.guhs.parkb.poppenkast.applaus").withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().playSound(null, p.blockPosition(), PlekB.APPLAUS.get(), SoundSource.NEUTRAL, 0.8f, 1.1f);
        GuhAdvancements.grant(p, "parkb_poppenkast");
    }

    /** (tests, dev) forget the running shows. */
    public static void vergeet() {
        SHOWS.clear();
    }

    // =====================================================================================================================
    /** The guhkindjes on the benches: a line when you talk to them. */
    static final class KindRol implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            GuhQuests.say(player, npc, "quest.guhs.parkb.kind." + player.getRandom().nextInt(4));
        }
    }
}
