package nl.juiced.guhs.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModFluids;

/**
 * 1.2.1: you can move through our sauces. On 26.1 NeoForge calls FluidType#move(LivingEntity, Vec3, double) for a modded
 * fluid; when that returns false the entity doesn't move at all (you were stuck in kaas saus).
 */
public class VloeistofGameTests {
    private static final String EMPTY = "empty";

    @GuhTest(template = EMPTY, batch = "vloeistof", timeoutTicks = 100)
    public static void kaasSausJeKuntErDoorheen(GameTestHelper helper) {
        loop(helper, ModBlocks.KAAS_SAUS.get().defaultBlockState(), ModFluids.KAAS_SAUS_TYPE.get());
    }

    @GuhTest(template = EMPTY, batch = "vloeistof2", timeoutTicks = 100)
    public static void maagzuurJeKuntErDoorheen(GameTestHelper helper) {
        loop(helper, ModBlocks.MAAGZUUR.get().defaultBlockState(), ModFluids.MAAGZUUR_TYPE.get());
    }

    @GuhTest(template = EMPTY, batch = "vloeistof3", timeoutTicks = 100)
    public static void kaasfrituursausJeKuntErDoorheen(GameTestHelper helper) {
        loop(helper, BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get().defaultBlockState(), BarbecuetherFeature.KAASFRITUURSAUS_TYPE.get());
    }

    private static void loop(GameTestHelper helper, net.minecraft.world.level.block.state.BlockState saus, FluidType type) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 7; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(new BlockPos(x, y, z), saus);
                }
            }
        }
        Pig pig = helper.spawn(EntityType.PIG, new Vec3(2.5, 1.2, 1.5));
        pig.setNoAi(true);
        pig.setInvulnerable(true);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(pig.isInFluidType(type), "the pig is in the sauce");
            // NeoForge's entry point for modded fluids must do the moving (false = no movement at all)
            helper.assertTrue(pig.moveInFluid(type, Vec3.ZERO, 0.08), "the fluid type moves the entity");
            Vec3 start = pig.position();
            for (int i = 0; i < 20; i++) {
                pig.setYRot(0);           // walk south (+z)
                pig.travel(new Vec3(0, 0, 1));
            }
            double dz = pig.getZ() - start.z;
            helper.assertTrue(dz > 0.2, "moved through the sauce: " + dz);
            helper.assertTrue(dz < 4.0, "but slowly: " + dz);
            helper.succeed();
        });
    }
}
