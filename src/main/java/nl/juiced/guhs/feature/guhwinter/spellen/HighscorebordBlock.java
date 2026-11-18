package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The highscore board of the Guhwinter plaza: a board on a post; above it floats the top 10 (a text display kept up to
 * date by the block's entity, the same floating board as the other minigames: {@link Scorebord#show}). One board shows the
 * Pepernotenrace ({@link Spel#RACE}, next to the arcade cabinet), the other the schaatsrondje ({@link Spel#SCHAATS}, by
 * the pond). A click tells you your own best in the chat; in creative, sneak-click switches the game.
 */
public class HighscorebordBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<HighscorebordBlock> CODEC = simpleCodec(HighscorebordBlock::new);
    public static final double HOOGTE = 0.35;
    private static final String T = "gui.guhs.guhwinter_spellen.bord.";
    private static final VoxelShape VORM = Block.box(1, 0, 6, 15, 16, 10);
    /** Goes up whenever a board changed; each board's entity shows it again when it sees a new number. */
    private static volatile int versie;

    public enum Spel implements StringRepresentable {
        RACE("race", SpellenData.RACE), SCHAATS("schaats", SpellenData.SCHAATS);

        public final String id, bord;

        Spel(String id, String bord) {
            this.id = id;
            this.bord = bord;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static final EnumProperty<Spel> SPEL = EnumProperty.create("spel", Spel.class);

    public HighscorebordBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPEL, Spel.RACE));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SPEL);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != SpellenFeature.HIGHSCOREBORD_BE.get()) {
            return null;
        }
        return (l, pos, s, be) -> {
            if (be instanceof Entity e && (l.getGameTime() + pos.asLong()) % 20 == 0 && l instanceof ServerLevel server
                    && (e.getoond != versie || (l.getGameTime() / 20) % 15 == 0)) {
                e.getoond = versie;
                toon(server, pos, s.getValue(SPEL));
            }
        };
    }

    /** Every board shows its list again soon (after a new score). */
    public static void verversAlles(MinecraftServer server) {
        versie++;
    }

    public static Vec3 plek(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1 + HOOGTE, pos.getZ() + 0.5);
    }

    private static String id(BlockPos pos) {
        return "guhwinter_" + Long.toHexString(pos.asLong());
    }

    public static void toon(ServerLevel level, BlockPos pos, Spel spel) {
        Scorebord.show(level, plek(pos), id(pos), bord(level.getServer(), spel));
    }

    /** "1:23.4" for a time in ticks. */
    public static String tijd(int ticks) {
        int tienden = ticks / 2;
        return (tienden / 600) + ":" + String.format("%02d", (tienden / 10) % 60) + "." + (tienden % 10);
    }

    public static String score(Spel spel, int s) {
        return spel == Spel.SCHAATS ? tijd(s) : Integer.toString(s);
    }

    /** The floating text: the title, the ten places, a hint. */
    public static Component bord(MinecraftServer server, Spel spel) {
        MutableComponent tekst = Component.empty().append(Component.translatable(T + "kop." + spel.id).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        List<SpellenData.Regel> top = SpellenData.get(server).top(spel.bord);
        if (top.isEmpty()) {
            tekst.append("\n").append(Component.translatable(T + "leeg").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        for (int i = 0; i < top.size(); i++) {
            ChatFormatting kleur = i == 0 ? ChatFormatting.YELLOW : i < 3 ? ChatFormatting.WHITE : ChatFormatting.GRAY;
            tekst.append("\n").append(Component.literal((i + 1) + ". " + top.get(i).naam() + "  " + score(spel, top.get(i).score())).withStyle(kleur));
        }
        tekst.append("\n").append(Component.translatable(T + "klik").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        return tekst;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) {
            Spel spel = state.getValue(SPEL);
            if (p.isCreative() && p.isShiftKeyDown()) {
                Spel ander = spel == Spel.RACE ? Spel.SCHAATS : Spel.RACE;
                level.setBlock(pos, state.setValue(SPEL, ander), 3);
                toon(p.level(), pos, ander);
                return InteractionResult.SUCCESS;
            }
            int beste = SpellenData.get(p.level().getServer()).beste(p.getUUID(), spel.bord);
            p.sendSystemMessage(beste < 0 ? Component.translatable(T + "eigen.geen." + spel.id).withStyle(ChatFormatting.GRAY)
                    : Component.translatable(T + "eigen." + spel.id, score(spel, beste)).withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.2f);
            toon(p.level(), pos, spel);
        }
        return InteractionResult.SUCCESS;
    }

    /** Only keeps the floating board there, and takes it away with the block. */
    public static class Entity extends BlockEntity {
        int getoond = -1;

        public Entity(BlockPos pos, BlockState state) {
            super(SpellenFeature.HIGHSCOREBORD_BE.get(), pos, state);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            if (level instanceof ServerLevel server) {
                weg(server, pos);
            }
        }
    }

    /** Takes the floating board of the block at pos away. */
    public static void weg(ServerLevel server, BlockPos pos) {
        String eigen = Scorebord.TAG + ":" + id(pos) + ":";
        for (Display.TextDisplay d : server.getEntitiesOfClass(Display.TextDisplay.class, new AABB(plek(pos), plek(pos)).inflate(1.5),
                d -> d.entityTags().stream().anyMatch(t -> t.startsWith(eigen)))) {
            d.discard();
        }
    }
}
