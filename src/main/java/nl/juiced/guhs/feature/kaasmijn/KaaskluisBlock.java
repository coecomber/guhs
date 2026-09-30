package nl.juiced.guhs.feature.kaasmijn;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import org.joml.Vector3f;

/**
 * The kaaskluis in the treasure room of the kaasmijn (in the mouth of the golden guh face): right-click it with
 * {@link #PRICE} goudkaas in your inventory and it opens for you, a treasure from the loot table chests/kaasmijn_kluis
 * every time. Gold cheese grows back in the deep mine, so you can come back for more.
 */
public class KaaskluisBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<KaaskluisBlock> CODEC = simpleCodec(KaaskluisBlock::new);
    public static final int PRICE = 2;
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/kaasmijn_kluis"));

    public KaaskluisBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Pays {@link #PRICE} goudkaas and hands out a treasure; false (and a hint) if the player doesn't have enough. */
    public static boolean open(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        int have = GuhQuests.count(player, KaasmijnFeature.GOUDKAAS.get());
        if (have < PRICE) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.kaasmijn.kluis_need", PRICE, have).withStyle(ChatFormatting.GOLD));
            level.playSound(null, pos, SoundEvents.VAULT_INSERT_ITEM_FAIL, SoundSource.BLOCKS, 1f, 1f);
            return false;
        }
        GuhQuests.take(player, KaasmijnFeature.GOUDKAAS.get(), PRICE);
        LootTable table = player.level().getServer().reloadableRegistries().getLootTable(LOOT);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player).withLuck(player.getLuck()).create(LootContextParamSets.CHEST);
        for (ItemStack stack : table.getRandomItems(params)) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        level.playSound(null, pos, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 1f, 1f);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7f, 1.4f);
        level.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.85f, 0.2f), 1.4f), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                40, 0.5, 0.4, 0.5, 0.05);
        player.sendSystemMessage(Component.translatable("quest.guhs.kaasmijn.kluis_open").withStyle(ChatFormatting.GOLD));
        GuhAdvancements.grant(player, "kaasmijn_kluis");
        return true;
    }
}
