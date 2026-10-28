package nl.juiced.guhs.feature.koepel;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * De Wolkenvanger (guhs:wolkenvanger): a dish for the roof that catches passing little clouds and condenses them into pluis.
 * Under the open sky only; the higher it stands the more it catches ({@link KoepelGetallen#vangTicks}: one pluis per 600 ticks
 * at sea level, per 120 ticks from y 192), on {@link KoepelGetallen#WOLKENVANGER} vadskracht. The pluis lands in its own slots
 * and a chest (or pipe, or Hapluikje) under it gets it straight away.
 * <p>
 * The pluis is {@code guhs:pluis} of the fabriek branch (Wolfabriek); until that item exists the dish catches white wool.
 */
public class WolkenvangerBlockEntity extends MachineBlockEntity implements KoepelBlock.KoepelMachine {
    private int teller;

    public WolkenvangerBlockEntity(BlockPos pos, BlockState state) {
        super(KoepelFeature.WOLKENVANGER_BE.get(), pos, state, KoepelGetallen.WOLKENVANGER, 2);
    }

    /** What the dish catches: guhs:pluis, or white wool while that item does not exist yet. */
    public static Item vangst() {
        Item pluis = KoepelVakken.guhs("pluis");
        return KoepelVakken.bestaat(pluis) ? pluis : Items.WHITE_WOOL;
    }

    private boolean onderOpenLucht() {
        return level != null && level.canSeeSky(worldPosition.above());
    }

    @Override
    protected boolean kanWerken() {
        return onderOpenLucht() && KoepelVakken.past(vakken(), new ItemStack(vangst()), 0, 2);
    }

    @Override
    protected boolean isVol() {
        return !KoepelVakken.past(vakken(), new ItemStack(vangst()), 0, 2);
    }

    @Override
    protected void werk() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (++teller < KoepelGetallen.vangTicks(worldPosition.getY())) {
            if (teller % 30 == 0) {
                sl.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.4, worldPosition.getZ() + 0.5, 2, 0.6, 0.2, 0.6, 0.0);
            }
            return;
        }
        teller = 0;
        KoepelVakken.stop(vakken(), new ItemStack(vangst()), 0, 2);
        naarBeneden(sl);
    }

    /** What it caught goes into whatever holds items under it. */
    private void naarBeneden(ServerLevel sl) {
        var onder = Kisten.van(sl, worldPosition.below(), Direction.UP);
        if (onder == null) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            ItemResource r = vakken().getResource(i);
            int n = vakken().getAmountAsInt(i);
            if (r.isEmpty() || n == 0) {
                continue;
            }
            ItemStack rest = Kisten.stop(onder, r.toStack(n));
            vakken().set(i, rest.isEmpty() ? ItemResource.EMPTY : r, rest.getCount());
        }
    }

    @Override
    protected boolean isInvoer(int vak) {
        return false;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return true;
    }

    @Override
    public boolean klik(ServerPlayer speler, ItemStack inHand) {
        if (!inHand.isEmpty()) {
            return false;
        }
        for (int i = 0; i < 2; i++) {
            ItemResource r = vakken().getResource(i);
            if (!r.isEmpty()) {
                speler.getInventory().placeItemBackInInventory(r.toStack(vakken().getAmountAsInt(i)));
                vakken().set(i, ItemResource.EMPTY, 0);
            }
        }
        speler.sendOverlayMessage((onderOpenLucht()
                ? Component.translatable("gui.guhs.koepel.vanger.stand", KoepelGetallen.vangTicks(worldPosition.getY()) / 20)
                : Component.translatable("gui.guhs.koepel.vanger.dak")).withStyle(ChatFormatting.AQUA));
        return true;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(Component.translatable("gui.guhs.koepel.vanger.uitlezing", KoepelGetallen.vangTicks(worldPosition.getY()) / 20)
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.putInt("Teller", teller);
    }

    @Override
    protected void laden(ValueInput in) {
        teller = in.getIntOr("Teller", 0);
    }
}
