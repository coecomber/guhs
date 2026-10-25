package nl.juiced.guhs.feature.onderwol.stadje;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.guhoven.GuhOvenBlockEntity;
import nl.juiced.guhs.feature.techmachine.VadsmolenBlockEntity;

/**
 * {@code guhs:stadje_bijvuller}: the hidden refiller of the Vadsmijnstadje's vadskracht showcase (one in the rock under the
 * plaza of every town; the same idea as the beurs' {@link nl.juiced.guhs.feature.techbeurs.BeursBeheer}, but cheap and
 * fair for a town that is NOT protected). Every {@link #ELKE} ticks (only while its chunk ticks) it looks for the things
 * that carry the mark {@code NeoForgeData.guhs_stadje} (written by tools/features/onderwol_stadje.py, so it turns with the
 * town): block entities of the loaded chunks within {@link #BEREIK} and the entities in that box. Nothing is cached.
 * <ul>
 *   <li>{@code Soort: "molen"}, {@code Vul: [{id, count}]}: a Vadsmolen whose in slot (empty or of that item) is topped up
 *       to count (gravel: cheap, the only thing it makes out of nothing);</li>
 *   <li>{@code Soort: "kar"}, {@code Kar: [{id, count, bar}]}: the chest cart of raw ore. It is NEVER given new ore: it only
 *       gets back, as raw ore, the bars the line made that lie in the Mijnwerkerskist beyond what the chest shows (one bar =
 *       one ore, up to count). So the ore of the town stays the 56 of the template: who takes ore or bars takes them out of
 *       the loop for good, and no ore is ever made (the Mijnwerker of this very town buys raw ore for emeralds);</li>
 *   <li>{@code Toon: [{id, count}]} on a container ({@code Soort: "kist"} the Mijnwerkerskist, {@code "oven"} the
 *       Glasslijperij's Guhoven): of each of these items at most count stays (the first slots: something to see and take);
 *       the rest is taken out (a bar of the cart back into the cart as ore while it has room, sand and glass away), so the
 *       line never stalls on a full chest or oven.</li>
 * </ul>
 * A block that was broken is gone with its block entity and its mark: it is never put back, and a block a player put in
 * its place has no mark. Each mark is only used on the expected kind of thing.
 */
public class Bijvuller extends BaseEntityBlock {
    public static final MapCodec<Bijvuller> CODEC = simpleCodec(Bijvuller::new);
    /** Every three minutes. */
    public static final int ELKE = 3600;
    /** How far around it (horizontally) it looks, and up and down: the town's template is 64 x 24 x 64. */
    public static final int BEREIK = 34, HOOG = 26;
    public static final String MERK = "guhs_stadje";

    public Bijvuller(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Kern(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (l, pos, s, be) -> {
            if (be instanceof Kern k && l instanceof ServerLevel server) {
                k.tick(server);
            }
        };
    }

    /** The refiller's block entity (nothing saved: after a load the first round is {@link #ELKE} ticks later). */
    public static class Kern extends BlockEntity {
        private int teller;

        public Kern(BlockPos pos, BlockState state) {
            super(Vadsmijnstadje.BIJVULLER_BE.get(), pos, state);
        }

        void tick(ServerLevel level) {
            if (++teller < ELKE) {
                return;
            }
            teller = 0;
            try {
                beurt(level);
            } catch (RuntimeException e) {
                // (a showcase that misbehaves must not break the world)
            }
        }

        /** (Tests) the next tick is a round. */
        public void versnel() {
            teller = ELKE - 1;
        }

        /** One round: first the outputs (their bars back into the cart), then the molens. */
        public void beurt(ServerLevel level) {
            List<BlockEntity> blokken = new ArrayList<>();
            int cx0 = (worldPosition.getX() - BEREIK) >> 4, cx1 = (worldPosition.getX() + BEREIK) >> 4;
            int cz0 = (worldPosition.getZ() - BEREIK) >> 4, cz1 = (worldPosition.getZ() + BEREIK) >> 4;
            for (int cx = cx0; cx <= cx1; cx++) {
                for (int cz = cz0; cz <= cz1; cz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) {
                        continue;
                    }
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        BlockPos p = be.getBlockPos();
                        if (!be.isRemoved() && Math.abs(p.getX() - worldPosition.getX()) <= BEREIK && Math.abs(p.getZ() - worldPosition.getZ()) <= BEREIK
                                && Math.abs(p.getY() - worldPosition.getY()) <= HOOG && be.getPersistentData().contains(MERK)) {
                            blokken.add(be);
                        }
                    }
                }
            }
            AABB box = new AABB(worldPosition).inflate(BEREIK, HOOG, BEREIK);
            List<Entity> karren = level.getEntities((Entity) null, box, e -> e.isAlive() && e instanceof Container && e.getPersistentData().contains(MERK));

            // what the carts miss, per raw ore, and which bar gives it back
            Map<Item, Item> oreVanBar = new HashMap<>();
            Map<Item, Integer> tekort = new HashMap<>();
            Map<Item, Container> karVan = new HashMap<>();
            for (Entity e : karren) {
                CompoundTag r = e.getPersistentData().getCompoundOrEmpty(MERK);
                if (!r.getStringOr("Soort", "").equals("kar")) {
                    continue;
                }
                Container kar = (Container) e;
                for (Tag t : r.getListOrEmpty("Kar")) {
                    if (t instanceof CompoundTag c) {
                        Item erts = item(c.getStringOr("id", ""));
                        Item bar = item(c.getStringOr("bar", ""));
                        if (erts == Items.AIR || bar == Items.AIR || karVan.containsKey(erts)) {
                            continue;
                        }
                        oreVanBar.put(bar, erts);
                        karVan.put(erts, kar);
                        tekort.put(erts, Math.max(0, c.getIntOr("count", 0) - tel(kar, erts)));
                    }
                }
            }
            for (BlockEntity be : blokken) {
                CompoundTag r = be.getPersistentData().getCompoundOrEmpty(MERK);
                String soort = r.getStringOr("Soort", "");
                boolean goed = switch (soort) {
                    case "kist" -> be instanceof ChestBlockEntity;
                    case "oven" -> be instanceof GuhOvenBlockEntity;
                    default -> false;
                };
                if (!goed || !(be instanceof Container c)) {
                    continue;
                }
                for (Tag t : r.getListOrEmpty("Toon")) {
                    if (t instanceof CompoundTag regel) {
                        Item item = item(regel.getStringOr("id", ""));
                        int teveel = item == Items.AIR ? 0 : tel(c, item) - regel.getIntOr("count", 0);
                        if (teveel <= 0) {
                            continue;
                        }
                        Item erts = oreVanBar.get(item);
                        if (erts != null) {
                            // a bar of the cart: back into the cart as its ore, as far as the cart misses it; the rest stays
                            int terug = Math.min(teveel, tekort.getOrDefault(erts, 0));
                            int genomen = neem(c, item, terug);
                            int erin = stop(karVan.get(erts), erts, genomen);
                            stop(c, item, genomen - erin);   // (what the cart had no room for goes back)
                            tekort.merge(erts, -erin, Integer::sum);
                        } else {
                            neem(c, item, teveel);
                        }
                    }
                }
            }
            for (BlockEntity be : blokken) {
                CompoundTag r = be.getPersistentData().getCompoundOrEmpty(MERK);
                if (!r.getStringOr("Soort", "").equals("molen") || !(be instanceof VadsmolenBlockEntity molen)) {
                    continue;
                }
                for (Tag t : r.getListOrEmpty("Vul")) {
                    if (t instanceof CompoundTag c) {
                        Item item = item(c.getStringOr("id", ""));
                        int wil = Math.min(c.getIntOr("count", 0), item.getDefaultMaxStackSize());
                        ItemResource ligt = molen.vakken().getResource(VadsmolenBlockEntity.IN);
                        int heeft = molen.vakken().getAmountAsInt(VadsmolenBlockEntity.IN);
                        if (item != Items.AIR && (ligt.isEmpty() || heeft == 0 || ligt.is(item)) && heeft < wil) {
                            molen.vakken().set(VadsmolenBlockEntity.IN, ItemResource.of(item), wil);
                            molen.setChanged();
                        }
                    }
                }
            }
        }
    }

    private static Item item(String id) {
        Identifier i = Identifier.tryParse(id);
        return i == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(i);
    }

    /** How many of this item the container holds. */
    public static int tel(Container c, Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** Takes up to n of this item out of the container, from its last slots on; returns how many. */
    public static int neem(Container c, Item item, int n) {
        int weg = 0;
        for (int i = c.getContainerSize() - 1; i >= 0 && weg < n; i--) {
            ItemStack s = c.getItem(i);
            if (s.is(item)) {
                int m = Math.min(n - weg, s.getCount());
                c.removeItem(i, m);
                weg += m;
            }
        }
        if (weg > 0) {
            c.setChanged();
        }
        return weg;
    }

    /** Puts up to n of this item into the container (onto its stacks first); returns how many went in. */
    public static int stop(Container c, Item item, int n) {
        int erin = 0;
        int max = Math.min(c.getMaxStackSize(), item.getDefaultMaxStackSize());
        for (int ronde = 0; ronde < 2 && erin < n; ronde++) {
            for (int i = 0; i < c.getContainerSize() && erin < n; i++) {
                ItemStack s = c.getItem(i);
                if (ronde == 0 && s.is(item) && s.getCount() < max) {
                    int m = Math.min(n - erin, max - s.getCount());
                    s.grow(m);
                    c.setItem(i, s);
                    erin += m;
                } else if (ronde == 1 && s.isEmpty() && c.canPlaceItem(i, new ItemStack(item))) {
                    int m = Math.min(n - erin, max);
                    c.setItem(i, new ItemStack(item, m));
                    erin += m;
                }
            }
        }
        if (erin > 0) {
            c.setChanged();
        }
        return erin;
    }
}
