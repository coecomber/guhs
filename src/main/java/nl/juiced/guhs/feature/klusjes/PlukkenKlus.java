package nl.juiced.guhs.feature.klusjes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.vadswoud.KnabbelbessenstruikBlock;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * Bloemetjes & bessen plukken: the resident picks the ripe knabbelbessen and sweet berries in the home base (the bush
 * stays and grows again), up to {@link #PER_KEER} bushes per trip. No berries? Then it picks a bloom at any flower
 * (vanilla or ours): the flower keeps standing (every flower once per {@link #BLOEM_RUST} ticks) and the resident brings
 * home a flower of any kind ({@link #kiesBloem}: the roze guhbloem twice as often as the others). (1.2.8) It never
 * plants new flowers any more: the flowers no longer multiply.
 * Guhs and pieppiepmuisjes.
 */
public class PlukkenKlus extends BasisKlus {
    public static final int PER_KEER = 4;
    public static final int BLOEM_RUST = 6000;
    private static final Map<String, Long> GEPLUKT = new ConcurrentHashMap<>();
    /** The tall flowers (not in the small flowers tag). */
    static final java.util.Set<Block> HOGE_BLOEMEN = java.util.Set.of(Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY);
    @Nullable
    private static List<Item> soorten;

    /** Every kind of flower a resident can bring home: all small flowers (vanilla, ours, other mods) and the tall ones. */
    public static List<Item> soorten() {
        if (soorten == null) {
            List<Item> uit = new ArrayList<>();
            for (Holder<Block> b : BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.SMALL_FLOWERS)) {
                Item i = b.value().asItem();
                if (b.value() != Blocks.WITHER_ROSE && i != Items.AIR && !uit.contains(i)) {
                    uit.add(i);
                }
            }
            for (Block b : HOGE_BLOEMEN) {
                uit.add(b.asItem());
            }
            uit.sort(java.util.Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
            soorten = uit;
        }
        return soorten;
    }

    /** A flower of a random kind; the roze guhbloem counts twice. */
    public static ItemStack kiesBloem(net.minecraft.util.RandomSource random) {
        List<Item> lijst = soorten();
        int n = random.nextInt(lijst.size() + 1);
        return new ItemStack(n == lijst.size() ? ModBlocks.ROZE_GUHBLOEM.get().asItem() : lijst.get(n));
    }

    PlukkenKlus() {
        super("plukken", () -> new ItemStack(VadswoudFeature.KNABBELBESSEN.get()), 300);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner) || isMuisje(bewoner);
    }

    private static String sleutel(ServerLevel level, BlockPos p) {
        return level.dimension().identifier() + "|" + p.asLong();
    }

    static boolean bloemRijp(ServerLevel level, BlockPos p) {
        return KlusGebied.guhbloem(level.getBlockState(p)) && GEPLUKT.getOrDefault(sleutel(level, p), Long.MIN_VALUE / 2) + BLOEM_RUST <= level.getGameTime();
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos bes = KlusGebied.kies(level, huisje, KlusGebied.Soort.BES, bewoner, false, p -> KlusGebied.plukbaar(level.getBlockState(p)));
        if (bes != null) {
            return new Taak(level, huisje, bewoner, bes, true);
        }
        BlockPos bloem = KlusGebied.kies(level, huisje, KlusGebied.Soort.BLOEM, bewoner, true, p -> bloemRijp(level, p));
        return bloem == null ? null : new Taak(level, huisje, bewoner, bloem, false);
    }

    /** Picks a bush without a player: the berries (the bush starts again from age 1). */
    public static List<ItemStack> pluk(ServerLevel level, BlockPos pos) {
        List<ItemStack> uit = new ArrayList<>();
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof KnabbelbessenstruikBlock && s.getValue(KnabbelbessenstruikBlock.AGE) > 1) {
            int age = s.getValue(KnabbelbessenstruikBlock.AGE);
            int n = 1 + level.getRandom().nextInt(2) + (age == KnabbelbessenstruikBlock.MAX_AGE ? 1 : 0);
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1f, 1.1f + level.getRandom().nextFloat() * 0.3f);
            BlockState geplukt = s.setValue(KnabbelbessenstruikBlock.AGE, 1);
            level.setBlock(pos, geplukt, Block.UPDATE_CLIENTS);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(geplukt));
            uit.add(new ItemStack(VadswoudFeature.KNABBELBESSEN.get(), n));
        } else if (s.getBlock() instanceof SweetBerryBushBlock && s.getValue(SweetBerryBushBlock.AGE) > 1) {
            int age = s.getValue(SweetBerryBushBlock.AGE);
            int n = 1 + level.getRandom().nextInt(2) + (age == SweetBerryBushBlock.MAX_AGE ? 1 : 0);
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1f, 0.8f + level.getRandom().nextFloat() * 0.4f);
            BlockState geplukt = s.setValue(SweetBerryBushBlock.AGE, 1);
            level.setBlock(pos, geplukt, Block.UPDATE_CLIENTS);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(geplukt));
            uit.add(new ItemStack(Items.SWEET_BERRIES, n));
        }
        return uit;
    }

    private class Taak extends StappenTaak {
        private final boolean bessen;
        private BlockPos doel;
        private int keer;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos doel, boolean bessen) {
            super(level, huisje, mob, PlukkenKlus.this);
            this.doel = doel;
            this.bessen = bessen;
        }

        @Override
        protected void begin() {
            naar(doel);
        }

        private void naar(BlockPos p) {
            doel = p;
            claim(p, 400);
            erbij(loop(p, 1.7));
            erbij(werk(bessen ? 18 : 14, Vec3.atCenterOf(p), null));
            erbij(doe(bessen ? this::bes : this::bloem));
        }

        private void bes() {
            List<ItemStack> geplukt = pluk(level, doel);
            for (ItemStack s : geplukt) {
                pak(s);
                aantal += s.getCount();
            }
            if (!geplukt.isEmpty()) {
                keer++;
                gelukt();
            }
            if (keer < PER_KEER * nl.juiced.guhs.feature.verhaal.VariantGedragen.draagFactor(mob)) {   // (3.0: the 626-guh carries twice as much)
                BlockPos nog = KlusGebied.kies(level, huisje, KlusGebied.Soort.BES, mob, false,
                        p -> p.distSqr(doel) <= 49 && KlusGebied.plukbaar(level.getBlockState(p)));
                if (nog != null) {
                    naar(nog);
                }
            }
        }

        private void bloem() {
            if (!bloemRijp(level, doel)) {
                return;
            }
            GEPLUKT.put(sleutel(level, doel), level.getGameTime());
            if (GEPLUKT.size() > 2048) {
                long nu = level.getGameTime();
                GEPLUKT.entrySet().removeIf(e -> e.getValue() + BLOEM_RUST < nu);
            }
            pak(kiesBloem(mob.getRandom()));
            level.playSound(null, doel, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 0.8f, 1.4f);
            sprankel(Vec3.atCenterOf(doel), 3);
            aantal = 1;
            gelukt();
        }
    }

    /** (Tests) forget which flowers were picked. */
    public static void vergeet() {
        GEPLUKT.clear();
    }
}
