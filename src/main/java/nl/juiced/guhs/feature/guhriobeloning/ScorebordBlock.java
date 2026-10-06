package nl.juiced.guhs.feature.guhriobeloning;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The highscore board of the Kasteel van de Grote Nether-Mika (on the forecourt, next to Pad-guh's kraam).
 * <ul>
 *     <li>Above the block floats a board for everybody ({@link #bord}; a text display kept up to date by the block's
 *     entity, like the boards of the other games): the top three of the whole castle in one go, and the record of every
 *     level with who holds it.</li>
 *     <li>A click tells YOU, in the chat, your own best time next to the server record for every level and for the
 *     whole castle ({@link #eigen}).</li>
 * </ul>
 * The times come from the engine ({@code GuhrioSpel.besteTijd}, {@code GuhrioKasteel.kasteelTijd}) and its Scorebord
 * boards; this block only shows them.
 */
public class ScorebordBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** How high above the block's top the board floats; how often it is looked at again (ticks). */
    public static final double HOOGTE = 0.3;
    public static final int VERVERS = 100;
    private static final String T = "gui.guhs.guhriobeloning.bord.";

    public ScorebordBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != GuhrioBeloningFeature.SCOREBORD_BE.get()) {
            return null;
        }
        return (l, pos, s, be) -> {
            if ((l.getGameTime() + pos.asLong()) % VERVERS == 0 && l instanceof ServerLevel server) {
                toon(server, pos);
            }
        };
    }

    /** Where the floating board of the block at pos hangs. */
    public static Vec3 plek(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1 + HOOGTE, pos.getZ() + 0.5);
    }

    private static String id(BlockPos pos) {
        return "guhriobeloning_" + Long.toHexString(pos.asLong());
    }

    /** Makes the floating board of the block at pos show the records of now. */
    public static void toon(ServerLevel level, BlockPos pos) {
        Scorebord.show(level, plek(pos), id(pos), bord(level.getServer()));
    }

    /** The floating board: the top three of the whole castle, then every level's record. */
    public static Component bord(MinecraftServer server) {
        MutableComponent tekst = Scorebord.tekst(server, Component.translatable(T + "kop"), List.of(GuhrioKasteel.BORD_KASTEEL),
                List.of(Component.translatable(T + "kasteel")), ticks -> Component.literal(GuhrioSpel.tijd(ticks))).copy();
        tekst.append("\n").append(Component.translatable(T + "levels").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (String level : GuhrioKasteel.LEVELS) {
            List<Scorebord.Entry> top = Scorebord.top(server, GuhrioKasteel.bord(level));
            Component naam = Component.translatable(T + "level", wereld(level));
            tekst.append("\n").append(top.isEmpty() ? Component.translatable(T + "leeg", naam).withStyle(ChatFormatting.GRAY)
                    : Component.translatable(T + "record", naam, GuhrioSpel.tijd(top.get(0).score()), top.get(0).name()).withStyle(ChatFormatting.YELLOW));
        }
        tekst.append("\n").append(Component.translatable(T + "klik").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return tekst;
    }

    /** "1-1" for "kasteel_1_1". */
    static String wereld(String levelId) {
        String s = levelId.startsWith("kasteel_") ? levelId.substring("kasteel_".length()) : levelId;
        return s.replace('_', '-');
    }

    /** Your own best times next to the records, as chat lines: the six levels, then the whole castle. */
    public static List<Component> eigen(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        List<Component> uit = new java.util.ArrayList<>();
        uit.add(Component.translatable(T + "eigen.kop").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (String level : GuhrioKasteel.LEVELS) {
            uit.add(regel(Component.translatable(T + "level", wereld(level)), GuhrioSpel.besteTijd(p, level), Scorebord.top(server, GuhrioKasteel.bord(level))));
        }
        uit.add(regel(Component.translatable(T + "kasteel"), GuhrioKasteel.kasteelTijd(p), Scorebord.top(server, GuhrioKasteel.BORD_KASTEEL)));
        return uit;
    }

    private static Component regel(Component naam, int eigen, List<Scorebord.Entry> top) {
        Component jij = eigen > 0 ? Component.literal(GuhrioSpel.tijd(eigen)).withStyle(ChatFormatting.GREEN)
                : Component.translatable(T + "eigen.geen").withStyle(ChatFormatting.GRAY);
        Component record = top.isEmpty() ? Component.translatable(T + "eigen.geen_record").withStyle(ChatFormatting.GRAY)
                : Component.translatable(T + "eigen.record", GuhrioSpel.tijd(top.get(0).score()), top.get(0).name()).withStyle(ChatFormatting.YELLOW);
        return Component.translatable(T + "eigen.regel", naam.copy().withStyle(ChatFormatting.LIGHT_PURPLE), jij, record).withStyle(ChatFormatting.WHITE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) {
            for (Component regel : eigen(p)) {
                p.sendSystemMessage(regel);
            }
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.2f);
            toon(p.level(), pos);
            GuhAdvancements.grant(p, "guhrio_beloning_bord");
        }
        return InteractionResult.SUCCESS;
    }

    /** The block entity: it only keeps the floating board there, and takes it away with the block. */
    public static class Entity extends BlockEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(GuhrioBeloningFeature.SCOREBORD_BE.get(), pos, state);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            if (level instanceof ServerLevel server) {
                String eigen = Scorebord.TAG + ":" + id(pos) + ":";
                for (Display.TextDisplay d : server.getEntitiesOfClass(Display.TextDisplay.class, new AABB(plek(pos), plek(pos)).inflate(1.5),
                        d -> d.entityTags().stream().anyMatch(t -> t.startsWith(eigen)))) {
                    d.discard();
                }
            }
        }
    }
}
