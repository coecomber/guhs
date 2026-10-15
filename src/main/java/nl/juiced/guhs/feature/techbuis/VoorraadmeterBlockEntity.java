package nl.juiced.guhs.feature.techbuis;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;

/**
 * De Voorraadmeter ({@code guhs:voorraadmeter}): counts what is in the chest, machine or Bank Guh behind it (or, when there
 * is nothing behind it, under it or above it) and gives a redstone signal when there are at least N of the item you showed
 * it (or of everything, without an example); turned around ("minder dan"): when there are fewer than N. A comparator reads
 * how far the count is on its way to N. Use it to open its screen (one example, the number).
 */
public class VoorraadmeterBlockEntity extends SensorBlockEntity implements MenuProvider {
    /** Counted every this many ticks. */
    public static final int MEET = 10;

    private final BuisFilter filter = new BuisFilter(FilterMenu.VAKKEN_METER, 64, this::filterVeranderd);
    private long geteld = -1;
    /** Count at the next tick, whatever the time (a setting changed, or it was just loaded). */
    private boolean opnieuw = true;
    @Nullable
    private BlockPos kijktNaar;

    public VoorraadmeterBlockEntity(BlockPos pos, BlockState state) {
        super(TechbuisFeature.VOORRAADMETER_BE.get(), pos, state);
    }

    public BuisFilter filter() {
        return filter;
    }

    /** The last count (-1: nothing to count in). */
    public long geteld() {
        return geteld;
    }

    /** What it counts in: behind it, else under it, else above it. */
    @Nullable
    private ResourceHandler<ItemResource> kist() {
        Direction achter = getBlockState().getValue(MachineBlock.FACING).getOpposite();
        for (Direction kant : List.of(achter, Direction.DOWN, Direction.UP)) {
            BlockPos pos = worldPosition.relative(kant);
            if (level.getBlockState(pos).getBlock() instanceof SensorBlock) {
                continue;
            }
            ResourceHandler<ItemResource> h = Kisten.van(level, pos, kant.getOpposite());
            if (h != null) {
                kijktNaar = pos;
                return h;
            }
        }
        kijktNaar = null;
        return null;
    }

    @Override
    protected void meet(ServerLevel level, long nu) {
        if (!opnieuw && Math.floorMod(nu + worldPosition.hashCode(), MEET) != 0) {
            return;
        }
        opnieuw = false;
        ResourceHandler<ItemResource> kist = kist();
        if (kist == null) {
            geteld = -1;
            zet(false, 0);
            return;
        }
        geteld = Kisten.tel(kist, filter::telt);
        int n = filter.getal();
        boolean aan = filter.behalve() ? geteld < n : geteld >= n;
        zet(aan, n <= 0 ? (geteld > 0 ? 15 : 0) : (int) Math.min(15, geteld * 15 / n));
    }

    private void filterVeranderd() {
        setChanged();
        opnieuw = true;
    }

    @Override
    public void klik(ServerPlayer player, boolean sluipt) {
        player.openMenu(this);
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        String k = "gui.guhs.techbuis.meter.";
        if (level == null || kijktNaar == null || geteld < 0) {
            regels.accept(Component.translatable(k + "geen_kist").withStyle(ChatFormatting.GOLD));
        } else {
            List<Component> namen = filter.namen();
            Component wat = namen.isEmpty() ? Component.translatable(k + "alles") : namen.get(0);
            regels.accept(Component.translatable(k + "telt", geteld, wat, level.getBlockState(kijktNaar).getBlock().getName())
                    .withStyle(signaal() ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        }
        regels.accept(Component.translatable(k + (filter.behalve() ? "minder" : "minstens"), filter.getal()).withStyle(ChatFormatting.GRAY));
    }

    // --- the screen ---

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FilterMenu(TechbuisFeature.VOORRAADMETER_MENU.get(), id, inventory, filter.items(), filter.data(),
                ContainerLevelAccess.create(level, worldPosition), getBlockState().getBlock());
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        filter.opslaan(uit.child("Filter"));
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        filter.laden(in.childOrEmpty("Filter"));
    }
}
