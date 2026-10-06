package nl.juiced.guhs.feature.techbuis;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * De Guhklok ({@code guhs:guhklok}): a clock that gives a short redstone signal every so often (every second ... every five
 * minutes), or a signal for as long as it is day, or for as long as it is night. All Guhklokken with the same setting tick
 * at the same moment (they count the world's own time), so nothing has to be saved or started. A comparator reads how far
 * the clock is on its way to the next tick. Use it for the next setting; sneak + use goes back.
 */
public class GuhklokBlockEntity extends SensorBlockEntity {
    /** The settings in ticks; after these come "by day" and "by night". */
    public static final int[] TIKKEN = {20, 40, 100, 200, 600, 1200, 6000};
    public static final int DAG = TIKKEN.length, NACHT = TIKKEN.length + 1, STANDEN = TIKKEN.length + 2;
    /** How long the signal of a tick lasts (two redstone ticks: long enough for every redstone part). */
    public static final int PULS = 4;

    private int stand = 2;

    public GuhklokBlockEntity(BlockPos pos, BlockState state) {
        super(TechbuisFeature.GUHKLOK_BE.get(), pos, state);
    }

    /** 0..{@link #STANDEN}-1: an index of {@link #TIKKEN}, {@link #DAG} or {@link #NACHT}. */
    public int stand() {
        return stand;
    }

    public void zetStand(int nieuw) {
        stand = Math.floorMod(nieuw, STANDEN);
        setChanged();
    }

    @Override
    protected void meet(ServerLevel level, long nu) {
        if (stand == DAG) {
            boolean dag = level.isBrightOutside();
            zet(dag, dag ? 15 : 0);
        } else if (stand == NACHT) {
            boolean nacht = level.isDarkOutside();
            zet(nacht, nacht ? 15 : 0);
        } else {
            int om = TIKKEN[stand];
            long fase = Math.floorMod(nu, (long) om);
            zet(fase < PULS, (int) (fase * 16 / om));
        }
    }

    @Override
    public void klik(ServerPlayer player, boolean sluipt) {
        zetStand(stand + (sluipt ? -1 : 1));
        tik();
        player.sendOverlayMessage(instelling());
    }

    private Component instelling() {
        String k = "gui.guhs.techbuis.klok.";
        if (stand == DAG) {
            return Component.translatable(k + "dag");
        }
        if (stand == NACHT) {
            return Component.translatable(k + "nacht");
        }
        int seconden = TIKKEN[stand] / 20;
        Component om = seconden == 1 ? Component.translatable(k + "s1") : seconden < 60 ? Component.translatable(k + "s", seconden)
                : seconden == 60 ? Component.translatable(k + "min1") : Component.translatable(k + "min", seconden / 60);
        return Component.translatable(k + "elke", om);
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(instelling().copy().withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        uit.putInt("Stand", stand);
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        stand = Math.floorMod(in.getIntOr("Stand", 2), STANDEN);
    }
}
