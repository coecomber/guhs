package nl.juiced.guhs.feature.klusjes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.vadswoud.KnabbelbessenstruikBlock;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;

/**
 * Bloemetjes & bessen plukken: the resident picks the ripe knabbelbessen and sweet berries in the home base (the bush
 * stays and grows again), up to {@link #PER_KEER} bushes per trip. No berries? Then it picks a guh flower (kaasbloem,
 * roze guhbloem): only a bloom, the flower keeps standing (every flower once per {@link #BLOEM_RUST} ticks), and now and
 * then it plants a new little flower of the same kind next to it (as long as there are fewer than {@link #MAX_BLOEMEN}).
 * Guhs and pieppiepmuisjes.
 */
public class PlukkenKlus extends BasisKlus {
    public static final int PER_KEER = 4;
    public static final int BLOEM_RUST = 6000;
    public static final int MAX_BLOEMEN = 24;
    /** 1 in N picked flowers gets a new little neighbour. */
    public static final int PLANT_KANS = 3;
    private static final Map<String, Long> GEPLUKT = new ConcurrentHashMap<>();

    PlukkenKlus() {
        super("plukken", () -> new ItemStack(VadswoudFeature.KNABBELBESSEN.get()), 300);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner) || isMuisje(bewoner);
    }

    private static String sleutel(ServerLevel level, BlockPos p) {
        return level.dimension().location() + "|" + p.asLong();
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
            int n = 1 + level.random.nextInt(2) + (age == KnabbelbessenstruikBlock.MAX_AGE ? 1 : 0);
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1f, 1.1f + level.random.nextFloat() * 0.3f);
            BlockState geplukt = s.setValue(KnabbelbessenstruikBlock.AGE, 1);
            level.setBlock(pos, geplukt, Block.UPDATE_CLIENTS);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(geplukt));
            uit.add(new ItemStack(VadswoudFeature.KNABBELBESSEN.get(), n));
        } else if (s.getBlock() instanceof SweetBerryBushBlock && s.getValue(SweetBerryBushBlock.AGE) > 1) {
            int age = s.getValue(SweetBerryBushBlock.AGE);
            int n = 1 + level.random.nextInt(2) + (age == SweetBerryBushBlock.MAX_AGE ? 1 : 0);
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1f, 0.8f + level.random.nextFloat() * 0.4f);
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
            BlockState s = level.getBlockState(doel);
            if (!bloemRijp(level, doel)) {
                return;
            }
            GEPLUKT.put(sleutel(level, doel), level.getGameTime());
            if (GEPLUKT.size() > 2048) {
                long nu = level.getGameTime();
                GEPLUKT.entrySet().removeIf(e -> e.getValue() + BLOEM_RUST < nu);
            }
            pak(new ItemStack(s.getBlock()));
            level.playSound(null, doel, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 0.8f, 1.4f);
            sprankel(Vec3.atCenterOf(doel), 3);
            aantal = 1;
            gelukt();
            if (mob.getRandom().nextInt(PLANT_KANS) == 0 && KlusGebied.van(level, huisje, KlusGebied.Soort.BLOEM).size() < MAX_BLOEMEN) {
                plantNaast(s);
            }
        }

        /** A new little flower of the same kind on a free spot next to it (within 2). */
        private void plantNaast(BlockState bloem) {
            for (int i = 0; i < 12; i++) {
                BlockPos p = doel.offset(mob.getRandom().nextInt(5) - 2, mob.getRandom().nextInt(3) - 1, mob.getRandom().nextInt(5) - 2);
                if (!p.equals(doel) && huisje.inGebied(p) && level.getBlockState(p).isAir() && bloem.canSurvive(level, p)) {
                    level.setBlock(p, bloem.getBlock().defaultBlockState(), Block.UPDATE_ALL);
                    level.playSound(null, p, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8f, 1.2f);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.4, p.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
                    return;
                }
            }
        }
    }

    /** (Tests) forget which flowers were picked. */
    public static void vergeet() {
        GEPLUKT.clear();
    }
}
