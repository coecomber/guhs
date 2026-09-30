package nl.juiced.guhs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.registry.ModItems;

/**
 * The knabbelkorf: a beehive for guh bees (normal bees like it too). When it's full: shears give 3-5 kaas knabbels,
 * a glass bottle gives kaashoning. Guh bees never get angry about it.
 */
public class KnabbelkorfBlock extends BeehiveBlock {
    public KnabbelkorfBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(HONEY_LEVEL) < MAX_HONEY_LEVELS || !(stack.is(Items.SHEARS) || stack.is(Items.GLASS_BOTTLE))) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (stack.is(Items.SHEARS)) {
            level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1f, 1f);
            if (!level.isClientSide()) {
                popResource(level, pos, new ItemStack(ModItems.KAAS_KNABBELS.get(), 3 + level.getRandom().nextInt(3)));
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
        } else {
            level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 1f);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(ModItems.KAASHONING.get())));
        }
        level.gameEvent(player, GameEvent.SHEAR, pos);
        if (!level.isClientSide()) {
            resetHoneyLevel(level, state, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
