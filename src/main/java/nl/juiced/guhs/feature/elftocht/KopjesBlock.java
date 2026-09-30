package nl.juiced.guhs.feature.elftocht;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.Minigames;

/**
 * A tray of steaming cups on the counter of a koek-en-zopie stall (the boost spots of the tour). Right-click: a cup of
 * warme chocovet or a kommetje snert for you (then the pot needs a moment: {@link #WACHT} ticks per player). Drinking it
 * warms you up: a short speed boost ({@link WarmDrankjeItem}).
 */
public class KopjesBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<KopjesBlock> CODEC = simpleCodec(KopjesBlock::new);
    public static final EnumProperty<Soort> SOORT = EnumProperty.create("soort", Soort.class);
    /** How long you wait before this kind of stall gives you another cup. */
    public static final int WACHT = 20 * 25;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 6, 15);

    public enum Soort implements StringRepresentable {
        CHOCOVET, SNERT;

        @Override
        public String getSerializedName() {
            return this == CHOCOVET ? "chocovet" : "snert";
        }

        public Item drankje() {
            return this == CHOCOVET ? ElftochtFeature.WARME_CHOCOVET.get() : ElftochtFeature.SNERT_KOMMETJE.get();
        }
    }

    public KopjesBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(SOORT, Soort.CHOCOVET));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SOORT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        pak((ServerPlayer) player, state.getValue(SOORT), pos);
        return InteractionResult.CONSUME;
    }

    /** Hands out a cup (true) or says it's still too hot (false). */
    public static boolean pak(ServerPlayer player, Soort soort, BlockPos pos) {
        Item drankje = soort.drankje();
        if (player.getCooldowns().isOnCooldown(drankje)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.elftocht.kopjes.wacht_" + soort.getSerializedName())
                    .withStyle(ChatFormatting.GOLD));
            return false;
        }
        player.getCooldowns().addCooldown(drankje, WACHT);
        Minigames.give(player, new ItemStack(drankje));
        player.sendOverlayMessage(Component.translatable("gui.guhs.elftocht.kopjes.pak_" + soort.getSerializedName())
                .withStyle(ChatFormatting.YELLOW));
        player.level().playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7f, 1.3f);
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4, z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, pos.getY() + 0.45, z, 0, 0.015, 0);
        }
    }
}
