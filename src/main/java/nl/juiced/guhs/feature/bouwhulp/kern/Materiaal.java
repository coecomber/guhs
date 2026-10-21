package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.bank.BankAdressen;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;

/**
 * Where the blocks of a spookbouw come from (design 2.1): always real items. First your own inventory, then a linked Bank
 * Guh, then (Bouwkeet level 1, "draadloos materiaal") every chest that touches the keet's vadskracht net. What comes back
 * (an undo, a block that was in the way) goes the same way round: inventory, bank, else it falls on the ground.
 * <p>
 * A player in creative mode builds for free.
 */
public final class Materiaal {
    /** The most chests "draadloos materiaal" looks into (a huge net with a hundred chests is still fast). */
    public static final int MAX_KISTEN = 64;

    /** What one block costs: {@code aantal} of {@code item} (0: free; a door's upper half, water, an empty spot). */
    public record Kost(Item item, int aantal) {
    }

    private static final Kost GRATIS = new Kost(Items.AIR, 0);

    /**
     * What a block costs, or null: it cannot be built at all (fire, a portal, lava, anything without an item). A double
     * slab is two slabs; the second half of a door, a bed or a tall flower is free (the first half paid).
     */
    @Nullable
    public static Kost kost(BlockState s) {
        if (s.isAir()) {
            return GRATIS;
        }
        if (s.is(Blocks.WATER)) {
            return s.getFluidState().is(Fluids.WATER) ? GRATIS : null;
        }
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return GRATIS;
        }
        if (s.hasProperty(BlockStateProperties.BED_PART) && s.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            return GRATIS;
        }
        Item item = s.getBlock().asItem();
        if (item == Items.AIR) {
            return null;
        }
        int n = s.hasProperty(BlockStateProperties.SLAB_TYPE) && s.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
        return new Kost(item, n);
    }

    /** One place items can come from and go back to. */
    public interface Bron {
        long tel(Item item);

        /** Takes up to n; returns how many it took. */
        int neem(Item item, int n);

        /** Puts it in; returns what did not fit. */
        ItemStack stop(ItemStack stack);
    }

    /** All the places of one build, in the order they are used. */
    public static final class Bronnen {
        private final List<Bron> lijst;
        private final boolean gratis;

        Bronnen(List<Bron> lijst, boolean gratis) {
            this.lijst = lijst;
            this.gratis = gratis;
        }

        /** Creative: everything is there and nothing is taken. */
        public boolean gratis() {
            return gratis;
        }

        public long tel(Item item) {
            if (gratis) {
                return Long.MAX_VALUE / 4;
            }
            long n = 0;
            for (Bron b : lijst) {
                n += b.tel(item);
            }
            return n;
        }

        /** Takes n, from the first place on; returns how many it got (fewer: not enough anywhere). */
        public int neem(Item item, int n) {
            if (gratis || n <= 0) {
                return Math.max(0, n);
            }
            int genomen = 0;
            for (Bron b : lijst) {
                if (genomen >= n) {
                    break;
                }
                genomen += b.neem(item, n - genomen);
            }
            return genomen;
        }

        /** Gives back; returns what found no place (the caller drops it). */
        public ItemStack terug(ItemStack stack) {
            if (gratis || stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack rest = stack.copy();
            for (Bron b : lijst) {
                if (rest.isEmpty()) {
                    break;
                }
                rest = b.stop(rest);
            }
            return rest;
        }

        /** What is missing of these items (empty map: everything is there). */
        public Map<Item, Integer> tekort(Map<Item, Integer> nodig) {
            Map<Item, Integer> uit = new LinkedHashMap<>();
            if (gratis) {
                return uit;
            }
            for (Map.Entry<Item, Integer> e : nodig.entrySet()) {
                long heb = tel(e.getKey());
                if (heb < e.getValue()) {
                    uit.put(e.getKey(), (int) (e.getValue() - heb));
                }
            }
            return uit;
        }

        public int aantalBronnen() {
            return lijst.size();
        }
    }

    /**
     * The places of a build. {@code speler}: whose inventory (null or offline: none); {@code bank}: the linked Bank Guh's id
     * (null: none); {@code vadsnet}: a block of the vadskracht net whose touching chests count (null: none; only from
     * Bouwkeet level 1).
     */
    public static Bronnen bronnen(ServerLevel level, @Nullable ServerPlayer speler, @Nullable UUID bank, @Nullable BlockPos vadsnet) {
        List<Bron> lijst = new ArrayList<>();
        boolean gratis = speler != null && speler.getAbilities().instabuild;
        if (speler != null && !speler.hasDisconnected()) {
            lijst.add(new InventarisBron(speler.getInventory()));
        }
        if (bank != null) {
            BankGuhBlockEntity be = BankAdressen.zoek(level.getServer(), bank, true);
            if (be != null) {
                lijst.add(new BankBron(be));
            }
        }
        if (vadsnet != null) {
            for (ResourceHandler<ItemResource> kist : kisten(level, vadsnet)) {
                lijst.add(new KistBron(kist));
            }
        }
        return new Bronnen(lijst, gratis);
    }

    /** Shorthand: {@link #bronnen} and {@link Bronnen#tel}. */
    public static long zoek(ServerLevel level, @Nullable ServerPlayer speler, @Nullable UUID bank, @Nullable BlockPos vadsnet, Item item) {
        return bronnen(level, speler, bank, vadsnet).tel(item);
    }

    /** Shorthand: {@link #bronnen} and {@link Bronnen#neem}. */
    public static int neem(ServerLevel level, @Nullable ServerPlayer speler, @Nullable UUID bank, @Nullable BlockPos vadsnet, Item item, int n) {
        return bronnen(level, speler, bank, vadsnet).neem(item, n);
    }

    /** Gives items back through these places; what does not fit falls on the ground here. */
    public static void terug(ServerLevel level, Bronnen bronnen, ItemStack stack, BlockPos waar) {
        ItemStack rest = bronnen.terug(stack);
        while (!rest.isEmpty()) {
            ItemStack stuk = rest.split(rest.getMaxStackSize());
            Containers.dropItemStack(level, waar.getX() + 0.5, waar.getY() + 0.5, waar.getZ() + 0.5, stuk);
        }
    }

    /** The chests (anything with items, except the net's own machines) that touch the vadskracht net of this block. */
    public static List<ResourceHandler<ItemResource>> kisten(ServerLevel level, BlockPos vanNet) {
        List<ResourceHandler<ItemResource>> uit = new ArrayList<>();
        VadsNet net = VadsKracht.net(level, vanNet);
        if (net == VadsNet.EMPTY) {
            return uit;
        }
        Set<Long> gezien = new HashSet<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (long l : net.blokken()) {
            BlockPos blok = BlockPos.of(l);
            for (Direction d : Direction.values()) {
                p.setWithOffset(blok, d);
                long k = p.asLong();
                if (net.bevat(p) || !gezien.add(k) || !level.isLoaded(p) || level.getBlockEntity(p) instanceof BankGuhBlockEntity) {
                    continue;
                }
                ResourceHandler<ItemResource> h = Kisten.van(level, p.immutable(), d.getOpposite());
                if (h != null) {
                    uit.add(h);
                    if (uit.size() >= MAX_KISTEN) {
                        return uit;
                    }
                }
            }
        }
        return uit;
    }

    // =====================================================================================================================
    // the places
    // =====================================================================================================================

    private record InventarisBron(Inventory inv) implements Bron {
        @Override
        public long tel(Item item) {
            long n = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(item) && s.getComponentsPatch().isEmpty()) {
                    n += s.getCount();
                }
            }
            return n;
        }

        @Override
        public int neem(Item item, int n) {
            int genomen = 0;
            for (int i = 0; i < inv.getContainerSize() && genomen < n; i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(item) && s.getComponentsPatch().isEmpty()) {
                    int k = Math.min(n - genomen, s.getCount());
                    s.shrink(k);
                    genomen += k;
                }
            }
            if (genomen > 0) {
                inv.setChanged();
            }
            return genomen;
        }

        @Override
        public ItemStack stop(ItemStack stack) {
            ItemStack rest = stack.copy();
            inv.add(rest);
            return rest;
        }
    }

    private record BankBron(BankGuhBlockEntity bank) implements Bron {
        @Override
        public long tel(Item item) {
            return bank.getStorage().count(new ItemStack(item));
        }

        @Override
        public int neem(Item item, int n) {
            int genomen = 0;
            while (genomen < n) {
                ItemStack s = bank.getStorage().extract(new ItemStack(item), n - genomen);
                if (s.isEmpty()) {
                    break;
                }
                genomen += s.getCount();
            }
            if (genomen > 0) {
                BankAdressen.houdWakker(bank);
            }
            return genomen;
        }

        @Override
        public ItemStack stop(ItemStack stack) {
            return bank.getStorage().insert(stack.copy());
        }
    }

    private record KistBron(ResourceHandler<ItemResource> kist) implements Bron {
        @Override
        public long tel(Item item) {
            return Kisten.tel(kist, s -> s.is(item) && s.getComponentsPatch().isEmpty());
        }

        @Override
        public int neem(Item item, int n) {
            int genomen = 0;
            while (genomen < n) {
                ItemStack s = Kisten.neem(kist, st -> st.is(item) && st.getComponentsPatch().isEmpty(), n - genomen);
                if (s.isEmpty()) {
                    break;
                }
                genomen += s.getCount();
            }
            return genomen;
        }

        @Override
        public ItemStack stop(ItemStack stack) {
            return Kisten.stop(kist, stack);
        }
    }

    private Materiaal() {
    }
}
