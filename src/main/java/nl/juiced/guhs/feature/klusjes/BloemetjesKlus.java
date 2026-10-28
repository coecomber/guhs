package nl.juiced.guhs.feature.klusjes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;

/**
 * (1.8.0) Bloemetjes plukken, for the Kleurmolen: the guh picks the flowers in the home base and plants them right back
 * (the flower keeps standing: it takes a cutting), bringing home that same flower, so a bed of cornflowers gives blue dye.
 * Every flower once per {@link #RUST} ticks, up to {@link #PER_KEER} per trip. Unlike "plukken" (berries first, a flower
 * of any kind), this one keeps the colours you planted. Guhs only.
 */
public class BloemetjesKlus extends BasisKlus {
    public static final int PER_KEER = 6, RUST = 2400;
    private static final Map<String, Long> GEPLUKT = new ConcurrentHashMap<>();

    public BloemetjesKlus() {
        super("bloemetjes", () -> new ItemStack(Items.CORNFLOWER), 300);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Override
    public String doeners() {
        return "guhs";
    }

    private static String sleutel(ServerLevel level, BlockPos p) {
        return level.dimension().identifier() + "|" + p.asLong();
    }

    static boolean rijp(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return KlusGebied.guhbloem(s) && s.getBlock().asItem() != Items.AIR && !s.is(Blocks.WITHER_ROSE)
                && GEPLUKT.getOrDefault(sleutel(level, p), Long.MIN_VALUE / 2) + RUST <= level.getGameTime();
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        List<BlockPos> bloemen = KlusGebied.van(level, huisje, KlusGebied.Soort.BLOEM);
        int klaar = (int) bloemen.stream().filter(p -> level.isLoaded(p) && rijp(level, p)).count();
        if (klaar > 0) {
            return KlusStand.ja("bloemen", klaar);
        }
        return bloemen.isEmpty() ? KlusStand.nee("geen", 0) : KlusStand.straks("later", bloemen.size());
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos b = KlusGebied.kies(level, huisje, KlusGebied.Soort.BLOEM, bewoner, true, p -> rijp(level, p));
        return b == null ? null : new Taak(level, huisje, bewoner, b);
    }

    /** (Tests) forget which flowers were picked. */
    public static void vergeet() {
        GEPLUKT.clear();
    }

    private class Taak extends StappenTaak {
        private BlockPos doel;
        private int keer;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos doel) {
            super(level, huisje, mob, BloemetjesKlus.this);
            this.doel = doel;
        }

        @Override
        protected void begin() {
            naar(doel);
        }

        private void naar(BlockPos p) {
            doel = p;
            claim(p, 400);
            erbij(loop(p, 1.7));
            erbij(werk(12, Vec3.atCenterOf(p), null));
            erbij(doe(this::pluk));
        }

        private void pluk() {
            if (!rijp(level, doel)) {
                return;
            }
            GEPLUKT.put(sleutel(level, doel), level.getGameTime());
            if (GEPLUKT.size() > 4096) {
                long nu = level.getGameTime();
                GEPLUKT.entrySet().removeIf(e -> e.getValue() + RUST < nu);
            }
            BlockState s = level.getBlockState(doel);
            pak(new ItemStack(s.getBlock().asItem(), 1 + (mob.getRandom().nextInt(3) == 0 ? 1 : 0)));
            level.playSound(null, doel, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 0.8f, 1.5f);
            sprankel(Vec3.atCenterOf(doel), 3);
            aantal++;
            keer++;
            gelukt();
            if (keer < PER_KEER) {
                BlockPos nog = KlusGebied.kies(level, huisje, KlusGebied.Soort.BLOEM, mob, false, p -> p.distSqr(doel) <= 64 && rijp(level, p));
                if (nog != null) {
                    naar(nog);
                }
            }
        }
    }
}
