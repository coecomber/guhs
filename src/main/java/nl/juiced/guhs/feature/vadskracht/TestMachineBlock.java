package nl.juiced.guhs.feature.vadskracht;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * {@code guhs:vadskracht_testmachine} (one block) and {@code guhs:vadskracht_testmachine_groot} (2 wide, 2 high): the
 * smallest possible guh machine, for the game tests of the machine base classes and as the worked example for every tech
 * slice. It asks {@link #VRAAG} VK and moves one item from its in slot (0) to its out slot (1) every {@link #WERKTIJD} ticks
 * of work. No item, no creative tab ({@code /setblock}).
 */
public class TestMachineBlock extends MachineBlock {
    public static final int VRAAG = VadsGetallen.GUH_OVEN, WERKTIJD = 20;

    private final int breed, hoog;
    private final MapCodec<TestMachineBlock> codec;

    public TestMachineBlock(Properties p, int breed, int hoog) {
        super(p);
        this.breed = breed;
        this.hoog = hoog;
        this.codec = simpleCodec(props -> new TestMachineBlock(props, breed, hoog));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public int breed() {
        return breed;
    }

    @Override
    public int hoog() {
        return hoog;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Kern(pos, state);
    }

    /** The test machine's block entity: everything a machine has to fill in. */
    public static class Kern extends MachineBlockEntity {
        private int voortgang;
        /** (tests) switched off: asks nothing. */
        public boolean uit;

        public Kern(BlockPos pos, BlockState state) {
            super(VadskrachtFeature.TESTMACHINE_BE.get(), pos, state, VRAAG, 2);
        }

        @Override
        protected boolean aan() {
            return !uit;
        }

        /** Something in the in slot that still fits in the out slot. */
        @Override
        protected boolean kanWerken() {
            return vakken().getAmountAsInt(0) > 0 && !isVol();
        }

        /** The out slot cannot take what waits in the in slot. */
        @Override
        protected boolean isVol() {
            ItemResource in = vakken().getResource(0), uitvak = vakken().getResource(1);
            if (in.isEmpty()) {
                return vakken().getAmountAsInt(1) >= vakken().getCapacityAsInt(1, uitvak) && !uitvak.isEmpty();
            }
            return !uitvak.isEmpty() && (!uitvak.equals(in) || vakken().getAmountAsInt(1) >= vakken().getCapacityAsInt(1, in));
        }

        @Override
        protected void werk() {
            if (++voortgang < WERKTIJD) {
                return;
            }
            voortgang = 0;
            ItemResource in = vakken().getResource(0);
            ItemStack uitvak = vakken().getResource(1).toStack(vakken().getAmountAsInt(1));
            vakken().set(0, in, vakken().getAmountAsInt(0) - 1);
            vakken().set(1, in, uitvak.getCount() + 1);
        }

        public int voortgang() {
            return voortgang;
        }

        @Override
        protected void opslaan(net.minecraft.world.level.storage.ValueOutput uitvoer) {
            uitvoer.putInt("Voortgang", voortgang);
        }

        @Override
        protected void laden(net.minecraft.world.level.storage.ValueInput in) {
            voortgang = in.getIntOr("Voortgang", 0);
        }
    }
}
