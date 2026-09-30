package nl.juiced.guhs.feature.guheinde;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Aangevreten kaaskorststenen: they look like kaaskorststenen with bites in them, break quickly, and a Mika-larfje
 * crawls out (like silverfish in infested stone). Silk touch leaves the larfje sleeping.
 */
public class LarfjeBlock extends Block {
    public LarfjeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, stack, dropExperience);
        if (level.getGameRules().get(GameRules.BLOCK_DROPS) && !EnchantmentHelper.hasTag(stack, EnchantmentTags.PREVENTS_INFESTED_SPAWNS)) {
            spawnLarfje(level, pos);
        }
    }

    public static void spawnLarfje(ServerLevel level, BlockPos pos) {
        MikaLarfjeEntity larfje = GuheindeFeature.MIKA_LARFJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (larfje != null) {
            larfje.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            level.addFreshEntity(larfje);
            larfje.spawnAnim();
        }
    }
}
