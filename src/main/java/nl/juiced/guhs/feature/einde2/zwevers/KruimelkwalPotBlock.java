package nl.juiced.guhs.feature.einde2.zwevers;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The kruimelkwal_pot: a Kruimelkwal in a glass jar, caught with a glass bottle ({@link KruimelkwalEntity#vang}). It is a
 * lamp (light {@link #LICHT}) that stands on a block or hangs under one, like a lantern.
 * <p>
 * Breaking it lets the kwal go: the kwal floats out and you get the glass bottle back (loot table). Only a tool with
 * silk touch keeps kwal and jar together.
 */
public class KruimelkwalPotBlock extends LanternBlock {
    public static final int LICHT = 12;

    public KruimelkwalPotBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (!EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_BEE_SPAWNS_WHEN_MINING)) {   // (= silk touch: the jar stays whole)
            laatVrij(level, pos);
        }
    }

    /** The kwal of a broken jar: it stays around this spot (and it is yours to keep: it never despawns). */
    @Nullable
    public static KruimelkwalEntity laatVrij(ServerLevel level, BlockPos pos) {
        KruimelkwalEntity kwal = E2ZweversFeature.KRUIMELKWAL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (kwal == null) {
            return null;
        }
        kwal.snapTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
        kwal.setThuis(new Vec3(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5), KruimelkwalEntity.ZWERF);
        kwal.setPersistenceRequired();
        level.addFreshEntity(kwal);
        kwal.playSound(E2ZweversFeature.KWAL.get(), 0.8f, 1.3f);
        return kwal;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(9) == 0) {
            double y = pos.getY() + (state.getValue(HANGING) ? 0.3 : 0.25) + random.nextDouble() * 0.25;
            level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.35 + random.nextDouble() * 0.3, y, pos.getZ() + 0.35 + random.nextDouble() * 0.3,
                    0, 0.01, 0);
        }
    }
}
