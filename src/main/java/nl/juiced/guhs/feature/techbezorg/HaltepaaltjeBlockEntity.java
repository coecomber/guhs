package nl.juiced.guhs.feature.techbezorg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * What a Haltepaaltje knows: the Stepstation it belongs to ({@link #station}; the station keeps the order of its stops),
 * its filter (up to {@link Bezorgnet#FILTER} kinds of item; empty = everything), and who placed it. Whether things are
 * taken or brought here is the block state ({@link HaltepaaltjeBlock#OPHALEN}), so you can see it on the sign.
 * <p>
 * A pole looks for a station by itself: when it is placed it joins the nearest loaded Stepstation within
 * {@link Bezorgnet#BEREIK} blocks that has room ({@link #zoekStation}); a new Stepstation adopts the loose poles around it
 * ({@link StepstationBlockEntity#adopteer}, when a player places it); the button "Ander station" moves it to the next one ({@link #volgendStation}).
 * Where the guhtje reaches in: bringing goes into the side of the chest the pole stands at (like a hopper pointing at it,
 * {@link #inKant}), taking comes out of the bottom (like a hopper under it, {@link #uitKant}).
 */
public class HaltepaaltjeBlockEntity extends BlockEntity implements MenuProvider {
    @Nullable
    private BlockPos station;
    private final NonNullList<ItemStack> filter = NonNullList.withSize(Bezorgnet.FILTER, ItemStack.EMPTY);
    @Nullable
    private UUID eigenaar;

    public HaltepaaltjeBlockEntity(BlockPos pos, BlockState state) {
        super(TechbezorgFeature.HALTEPAALTJE_BE.get(), pos, state);
    }

    // =====================================================================================================================
    // what happens here
    // =====================================================================================================================

    /** true: the guhtje takes things out here; false: it brings things. */
    public boolean ophalen() {
        return getBlockState().hasProperty(HaltepaaltjeBlock.OPHALEN) && getBlockState().getValue(HaltepaaltjeBlock.OPHALEN);
    }

    public void zetOphalen(boolean ophalen) {
        if (level != null && ophalen() != ophalen) {
            level.setBlock(worldPosition, getBlockState().setValue(HaltepaaltjeBlock.OPHALEN, ophalen), Block.UPDATE_ALL);
        }
    }

    /** The side of this pole the chest is on. */
    public Direction kant() {
        return getBlockState().hasProperty(HaltepaaltjeBlock.FACING) ? getBlockState().getValue(HaltepaaltjeBlock.FACING) : Direction.DOWN;
    }

    /** The block this pole serves. */
    public BlockPos kist() {
        return worldPosition.relative(kant());
    }

    /**
     * Where things are put in: the side of the chest the pole stands at, like a hopper pointing at it (a pole ON a furnace
     * fills what is baked, one beside it the fuel). Null: nothing here holds items.
     */
    @Nullable
    public ResourceHandler<ItemResource> inKant() {
        if (level == null) {
            return null;
        }
        ResourceHandler<ItemResource> in = Kisten.van(level, kist(), kant().getOpposite());
        return in != null ? in : Kisten.van(level, kist(), null);
    }

    /**
     * Where things are taken out: the bottom of the chest, wherever the pole stands: exactly what a hopper under it would
     * get (so a furnace gives what is done and keeps its fuel and what still bakes). Only a block that has no bottom side
     * at all is asked at the pole's side, then from the inside. Null: nothing here holds items.
     */
    @Nullable
    public ResourceHandler<ItemResource> uitKant() {
        if (level == null) {
            return null;
        }
        for (Direction kant : new Direction[] {Direction.DOWN, kant().getOpposite(), null}) {
            ResourceHandler<ItemResource> uit = Kisten.van(level, kist(), kant);
            if (uit != null) {
                return uit;
            }
        }
        return null;
    }

    public boolean heeftKist() {
        return inKant() != null || uitKant() != null;
    }

    // =====================================================================================================================
    // the filter
    // =====================================================================================================================

    /** May this item be taken / brought here? (No filter: everything. The filter looks at the kind of item only.) */
    public boolean past(ItemStack stack) {
        boolean leeg = true;
        for (ItemStack f : filter) {
            if (!f.isEmpty()) {
                leeg = false;
                if (ItemStack.isSameItem(f, stack)) {
                    return true;
                }
            }
        }
        return leeg;
    }

    public ItemStack filter(int vak) {
        return filter.get(vak);
    }

    /** Puts (a copy of one of) this item in a filter slot; empty clears it. */
    public void zetFilter(int vak, ItemStack stack) {
        filter.set(vak, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    public void wisFilter() {
        for (int i = 0; i < filter.size(); i++) {
            filter.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    /** The filter as a container of ghost items (for the menu's slots). */
    Container filterVakken() {
        return new Container() {
            @Override
            public int getContainerSize() {
                return filter.size();
            }

            @Override
            public boolean isEmpty() {
                return filter.stream().allMatch(ItemStack::isEmpty);
            }

            @Override
            public ItemStack getItem(int slot) {
                return filter.get(slot);
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                zetFilter(slot, ItemStack.EMPTY);
                return ItemStack.EMPTY;   // (ghosts: nothing ever comes out)
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                zetFilter(slot, ItemStack.EMPTY);
                return ItemStack.EMPTY;
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                zetFilter(slot, stack);
            }

            @Override
            public void setChanged() {
                HaltepaaltjeBlockEntity.this.setChanged();
            }

            @Override
            public boolean stillValid(Player player) {
                return !isRemoved();
            }

            @Override
            public void clearContent() {
                wisFilter();
            }
        };
    }

    // =====================================================================================================================
    // its station
    // =====================================================================================================================

    @Nullable
    public BlockPos station() {
        return station;
    }

    /** (Only the bookkeeping on this side; {@link StepstationBlockEntity#koppel} / {@link StepstationBlockEntity#ontkoppel} do both.) */
    void zetStation(@Nullable BlockPos pos) {
        station = pos == null ? null : pos.immutable();
        setChanged();
    }

    /** The station it belongs to, when that is loaded and still lists this pole. */
    @Nullable
    public StepstationBlockEntity thuis() {
        if (station == null || level == null || !level.isLoaded(station)) {
            return null;
        }
        return level.getBlockEntity(station) instanceof StepstationBlockEntity s && s.nummer(worldPosition) >= 0 ? s : null;
    }

    /** Is it loose: no station, or its station is loaded and gone (or does not list it)? */
    public boolean isLos() {
        return station == null || (level != null && level.isLoaded(station) && thuis() == null);
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    /**
     * Joins the nearest loaded Stepstation within reach that has room. With {@code na}: the first one AFTER that station in
     * the list (nearest first), wrapping around, so pressing the button walks along all of them. False: there is none (it
     * stays where it was).
     */
    public boolean zoekStation(ServerLevel sl, @Nullable BlockPos na) {
        List<StepstationBlockEntity> alle = Bezorgnet.stations(sl, worldPosition);
        int start = 0;
        for (int i = 0; na != null && i < alle.size(); i++) {
            if (alle.get(i).getBlockPos().equals(na)) {
                start = i + 1;
            }
        }
        for (int i = 0; i < alle.size(); i++) {
            if (alle.get((start + i) % alle.size()).koppel(this)) {
                return true;
            }
        }
        return false;
    }

    /** The button "Ander station". */
    public void volgendStation(ServerLevel sl) {
        zoekStation(sl, station);
    }

    /** Placed by a player: it finds its station, picks a sensible job, and says what happened. */
    void geplaatst(ServerPlayer player) {
        eigenaar = player.getUUID();
        setChanged();
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        GuhAdvancements.grant(player, "tech_bezorg_halte");
        boolean gevonden = zoekStation(sl, null);
        StepstationBlockEntity thuis = thuis();
        Component zeg;
        if (gevonden && thuis != null) {
            // the first stop takes things, and as long as nothing brings them anywhere the next one does that
            if (thuis.heeftOphaalHalte(worldPosition) && !thuis.heeftAfleverHalte(worldPosition)) {
                zetOphalen(false);
            }
            BlockPos s = thuis.getBlockPos();
            zeg = Component.translatable("gui.guhs.techbezorg.halte.gekoppeld", thuis.nummer(worldPosition) + 1, s.getX(), s.getY(), s.getZ())
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
        } else if (Bezorgnet.stations(sl, worldPosition).isEmpty()) {
            zeg = Component.translatable("gui.guhs.techbezorg.halte.geen_station").withStyle(ChatFormatting.GRAY);
        } else {
            zeg = Component.translatable("gui.guhs.techbezorg.halte.vol").withStyle(ChatFormatting.GRAY);
        }
        player.sendOverlayMessage(zeg);
        if (!heeftKist()) {
            player.sendSystemMessage(Component.translatable("gui.guhs.techbezorg.halte.geen_kist").withStyle(ChatFormatting.GRAY));
        }
    }

    // =====================================================================================================================
    // loading, removal, saving
    // =====================================================================================================================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            Bezorgnet.halteErbij(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            Bezorgnet.halteWeg(level, worldPosition);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide()) {
            Bezorgnet.halteErbij(level, worldPosition);
        }
    }

    /** Broken: its station forgets it. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) {
            return;
        }
        Bezorgnet.halteWeg(level, pos);
        if (station != null && level.isLoaded(station) && level.getBlockEntity(station) instanceof StepstationBlockEntity s) {
            s.vergeet(pos);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        station = in.read("Station", BlockPos.CODEC).orElse(null);
        eigenaar = in.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        List<ItemStack> lijst = in.read("Filter", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < filter.size(); i++) {
            filter.set(i, i < lijst.size() ? lijst.get(i) : ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput uit) {
        super.saveAdditional(uit);
        uit.storeNullable("Station", BlockPos.CODEC, station);
        uit.storeNullable("Eigenaar", UUIDUtil.CODEC, eigenaar);
        uit.store("Filter", ItemStack.OPTIONAL_CODEC.listOf(), new ArrayList<>(filter));
    }

    // =====================================================================================================================
    // the screen
    // =====================================================================================================================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.guhs.haltepaaltje");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new HalteMenu(id, inventory, this);
    }

    /** (Tests and commands.) */
    static HaltepaaltjeBlockEntity bij(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof HaltepaaltjeBlockEntity h ? h : null;
    }
}
