package nl.juiced.guhs.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import nl.juiced.guhs.registry.ModItems;

/** A pink guh slime: bounces around happily, never attacks, and the little ones drop guh slimeballs. */
public class GuhSlimeEntity extends Slime {
    public GuhSlimeEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(goal -> true); // peaceful: never goes after anyone
    }

    @Override
    protected boolean isDealsDamage() {
        return false;
    }

    @Override
    protected void dealDamage(LivingEntity target) {
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected ParticleOptions getParticleType() {
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.GUH_SLIMEBALL.get()));
    }

    public static boolean checkGuhSlimeSpawnRules(EntityType<GuhSlimeEntity> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).isSolid() && level.getRawBrightness(pos, 0) > 7;
    }
}
