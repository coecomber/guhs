package nl.juiced.guhs.feature.toveren;

import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Het Glinsteraltaar ({@code guhs:glinsteraltaar}): the guh enchanting table. It works like the vanilla one (its menu is
 * {@link GlinsteraltaarMenu}, shown in the vanilla enchanting screen) and its power comes from the
 * {@link ToverenFeature#KNUFFELBOEKENKAST Knuffelboekenkasten} in the vanilla ring positions (two blocks away, on the
 * altar's level and one up, with air or another power transmitter in between). Little pink hearts fly from those shelves
 * to the altar ({@link #animateTick}).
 */
public class GlinsteraltaarBlock extends BaseEntityBlock {
    public static final MapCodec<GlinsteraltaarBlock> CODEC = simpleCodec(GlinsteraltaarBlock::new);
    /** The vanilla ring: every spot two blocks away (x or z), on the altar's level and one up. */
    public static final List<BlockPos> KAST_PLEKKEN = EnchantingTableBlock.BOOKSHELF_OFFSETS;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 12.0);

    public GlinsteraltaarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GlinsteraltaarBlock> codec() {
        return CODEC;
    }

    /** Is there a Knuffelboekenkast at this ring spot that reaches the altar (nothing solid in between)? */
    public static boolean telt(Level level, BlockPos altaar, BlockPos offset) {
        return level.getBlockState(altaar.offset(offset)).is(ToverenFeature.KNUFFELBOEKENKAST.get())
                && level.getBlockState(altaar.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2)).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER);
    }

    /** The altar's power: the number of Knuffelboekenkasten that count (the menu caps it at 15, like vanilla). */
    public static int kracht(Level level, BlockPos altaar) {
        int n = 0;
        for (BlockPos offset : KAST_PLEKKEN) {
            if (telt(level, altaar, offset)) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        for (BlockPos offset : KAST_PLEKKEN) {
            if (random.nextInt(16) == 0 && telt(level, pos, offset)) {
                // (like vanilla's glyphs: the particle starts at the shelf and flies to just above the altar)
                level.addParticle(ToverenFeature.GLINSTERHARTJE.get(), pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                        offset.getX() + random.nextFloat() - 0.5, offset.getY() - random.nextFloat() - 1.0F, offset.getZ() + random.nextFloat() - 0.5);
            }
        }
        if (random.nextInt(4) == 0) {   // the little cloud of pink glitter around the book
            level.addParticle(ToverenFeature.GLINSTERHARTJE.get(), pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5,
                    (random.nextFloat() - 0.5) * 1.2, random.nextFloat() * 0.4 - 0.2, (random.nextFloat() - 0.5) * 1.2);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GlinsteraltaarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, ToverenFeature.GLINSTERALTAAR_BE.get(), GlinsteraltaarBlockEntity::boekTick) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            MenuProvider provider = state.getMenuProvider(level, pos);
            if (provider != null) {
                player.openMenu(provider);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GlinsteraltaarBlockEntity altaar) {
            Component title = altaar.getDisplayName();
            return new SimpleMenuProvider((id, inventory, player) -> new GlinsteraltaarMenu(id, inventory, ContainerLevelAccess.create(level, pos)), title);
        }
        return null;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
