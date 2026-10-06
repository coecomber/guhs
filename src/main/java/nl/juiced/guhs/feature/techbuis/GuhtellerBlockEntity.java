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
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * De Guhteller ({@code guhs:guhteller}): counts the redstone pulses that come in at its back (every time the signal there
 * goes on), and at the N-th it gives a pulse itself (to every side but the back) and starts again at zero. With a Guhklok
 * or another sensor behind it: "every tenth time". A comparator reads how far the count is. Use it for the next N; sneak +
 * use puts the count back to zero.
 */
public class GuhtellerBlockEntity extends SensorBlockEntity {
    public static final int[] DOELEN = {2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 32, 64};
    /** How long its own pulse lasts, in ticks. */
    public static final int PULS = 10;

    private int doel = 4;
    private int tel;
    private int puls;
    private boolean wasAan;

    public GuhtellerBlockEntity(BlockPos pos, BlockState state) {
        super(TechbuisFeature.GUHTELLER_BE.get(), pos, state);
    }

    public int doel() {
        return doel;
    }

    public int geteld() {
        return tel;
    }

    public void zetDoel(int n) {
        doel = Math.max(1, n);
        tel = 0;
        setChanged();
    }

    /** Is there a redstone signal at the back? (Guhdraad and a Guhrad do not count, see {@link Buizen#redstone}.) */
    private boolean invoer(ServerLevel level) {
        return Buizen.redstone(level, worldPosition, getBlockState().getValue(MachineBlock.FACING).getOpposite());
    }

    @Override
    protected void meet(ServerLevel level, long nu) {
        boolean aan = invoer(level);
        if (aan && !wasAan) {
            tel++;
            if (tel >= doel) {
                tel = 0;
                puls = PULS;
            }
            setChanged();
        }
        wasAan = aan;
        if (puls > 0) {
            puls--;
            zet(true, 15);
        } else {
            zet(false, tel * 15 / doel);
        }
    }

    @Override
    public void klik(ServerPlayer player, boolean sluipt) {
        if (sluipt) {
            tel = 0;
            setChanged();
            player.sendOverlayMessage(Component.translatable("gui.guhs.techbuis.teller.nul"));
        } else {
            int i = 0;
            while (i < DOELEN.length && DOELEN[i] != doel) {
                i++;
            }
            zetDoel(DOELEN[(i + 1) % DOELEN.length]);
            player.sendOverlayMessage(Component.translatable("gui.guhs.techbuis.teller.tot", doel));
        }
        tik();
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.techbuis.teller.geteld", tel, doel).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        uit.putInt("Doel", doel);
        uit.putInt("Tel", tel);
        uit.putInt("Puls", puls);
        uit.putBoolean("WasAan", wasAan);
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        doel = Math.max(1, in.getIntOr("Doel", 4));
        tel = in.getIntOr("Tel", 0);
        puls = in.getIntOr("Puls", 0);
        wasAan = in.getBooleanOr("WasAan", false);
    }
}
