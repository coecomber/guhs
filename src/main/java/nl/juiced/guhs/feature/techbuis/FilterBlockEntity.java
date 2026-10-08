package nl.juiced.guhs.feature.techbuis;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Snoet;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsVerbruiker;

/**
 * The block entity of a Filterstuk: a {@link BuisStukBlockEntity} that needs vadskracht ({@link VadsGetallen#BUISFILTER}),
 * takes a mouthful at a time ({@link Buizen#HAP_FILTER}) and has a list ({@link BuisFilter}): what it takes out of the
 * block behind it, and what it lets through when others send past it. Without vadskracht it is shut. It is also the ONLY
 * thing that takes items out of a Bank Guh (an upgraded one; {@link #kist}). Its face
 * ({@link FilterBlock#SNOET}) is asleep without vadskracht, surprised when it cannot get rid of its items, happy otherwise.
 */
public class FilterBlockEntity extends BuisStukBlockEntity implements VadsVerbruiker, MenuProvider {
    private final BuisFilter filter = new BuisFilter(FilterMenu.VAKKEN_FILTER, 0, this::filterVeranderd);
    private boolean kracht;
    private boolean gemeld;

    public FilterBlockEntity(BlockPos pos, BlockState state) {
        super(TechbuisFeature.FILTER_BE.get(), pos, state);
    }

    public BuisFilter filter() {
        return filter;
    }

    public boolean heeftKracht() {
        return kracht;
    }

    private void filterVeranderd() {
        setChanged();
        gedraaid();   // (try again right away: what was stuck may get through now)
    }

    // --- the piece ---

    @Override
    protected int hap() {
        return Buizen.HAP_FILTER;
    }

    @Override
    protected boolean magWerken() {
        return kracht && super.magWerken();
    }

    @Override
    protected boolean magEruit(ItemStack stack) {
        return filter.past(stack);
    }

    @Override
    protected int bewaar() {
        return filter.getal();
    }

    @Override
    public boolean laatDoor(ItemStack stack) {
        return kracht && super.laatDoor(stack) && filter.past(stack);
    }

    @Override
    public boolean isGericht() {
        return filter.isGericht();
    }

    /**
     * The user's decision B6: a Bank Guh gives items to a Filterstuk only (never to a hopper, a plain piece or a pick-up
     * Haltepaaltje), and only when the bank is upgraded. So behind a bank this piece does not ask for the item capability
     * (nothing comes out of that) but for the bank's own door for filters; what it takes there is its list
     * ({@link #magEruit}) and what it leaves is "laat liggen" ({@link #bewaar}).
     */
    @Nullable
    @Override
    protected ResourceHandler<ItemResource> kist(BlockPos achter, Direction voor) {
        BankGuhBlockEntity bank = bankAchter();
        return bank != null ? bank.filterkant() : super.kist(achter, voor);
    }

    /** Behind a Bank Guh without its Bodemloos Knabbelmaagje even a Filterstuk gets nothing. */
    @Nullable
    @Override
    protected String krijgtNiks() {
        BankGuhBlockEntity bank = bankAchter();
        return bank != null && !bank.isUpgraded() ? "bank_maagje" : null;
    }

    @Override
    protected void voorTick(ServerLevel level) {
        if (!gemeld) {
            gemeld = true;   // (a Filterstuk put there without a block update still finds its net)
            VadsKracht.veranderd(level, worldPosition);
        }
        BlockState state = getBlockState();
        Snoet snoet = Snoet.van(kracht, verstopt() || !terug().isEmpty());
        if (state.hasProperty(FilterBlock.SNOET) && state.getValue(FilterBlock.SNOET) != snoet) {
            level.setBlock(worldPosition, state.setValue(FilterBlock.SNOET, snoet), Block.UPDATE_CLIENTS);
        }
    }

    // --- vadskracht ---

    @Override
    public BlockPos vadsPlek() {
        return worldPosition;
    }

    @Override
    public int vadsVraag() {
        return VadsGetallen.BUISFILTER;
    }

    @Override
    public void vadsStroom(boolean aan) {
        kracht = aan;
    }

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        if (kracht) {
            regels(regels);
        }
        regels.accept(lijstRegel());
        if (filter.getal() > 0) {
            regels.accept(Component.translatable("gui.guhs.techbuis.filter.bewaar", filter.getal()).withStyle(ChatFormatting.GRAY));
        }
    }

    /** "Laat alleen door: A, B" / "Laat alles door behalve: A" / "Laat alles door". */
    private Component lijstRegel() {
        List<Component> namen = filter.namen();
        if (namen.isEmpty()) {
            return Component.translatable("gui.guhs.techbuis.filter.alles").withStyle(ChatFormatting.GRAY);
        }
        Component lijst = ComponentUtils.formatList(namen, Component.literal(", "));
        return Component.translatable(filter.behalve() ? "gui.guhs.techbuis.filter.behalve" : "gui.guhs.techbuis.filter.alleen", lijst)
                .withStyle(ChatFormatting.GRAY);
    }

    // --- the screen ---

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FilterMenu(TechbuisFeature.FILTER_MENU.get(), id, inventory, filter.items(), filter.data(),
                ContainerLevelAccess.create(level, worldPosition), getBlockState().getBlock());
    }

    // --- saving ---

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        filter.laden(in.childOrEmpty("Filter"));
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        filter.opslaan(uit.child("Filter"));
    }
}
