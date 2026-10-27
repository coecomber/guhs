package nl.juiced.guhs.feature.blub.klus;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.StappenTaak;

/**
 * Bellenbesproeier (a Blub's chore, id {@value #ID}): the Blub hops to a few crops that are still growing and blows a
 * shower of water bubbles over them. Each sprinkled crop grows one stage {@link #KANS_PROCENT}% of the time:
 * a little faster, not a farm machine. Bubbles over the crops, its own sound.
 */
public class BellenKlus extends BlubKlusjes.BlubKlus {
    public static final String ID = "blub_bellen";
    public static final int PER_KEER = 3, BLAAS_TICKS = 50, KANS_PROCENT = 50;

    BellenKlus() {
        super(ID, () -> new ItemStack(BlubFeature.KLUS_ICONEN.get("bellen").get()), 400);
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        int n = KlusGebied.van(level, huisje, BlubKlusjes.GROEIEND).size();
        return n > 0 ? KlusStand.ja("gewassen", n) : KlusStand.nee("geen", 0);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        List<BlockPos> plekken = new ArrayList<>();
        for (int i = 0; i < PER_KEER; i++) {
            BlockPos p = KlusGebied.kies(level, huisje, BlubKlusjes.GROEIEND, bewoner, q -> !plekken.contains(q) && groeit(level.getBlockState(q)));
            if (p == null) {
                break;
            }
            plekken.add(p);
        }
        return plekken.isEmpty() ? null : new Taak(level, huisje, bewoner, plekken);
    }

    static boolean groeit(BlockState s) {
        return s.getBlock() instanceof CropBlock crop && !crop.isMaxAge(s);
    }

    /** The bubbles over one crop: maybe an extra growth tick (the chance in percent; returns whether it grew). */
    public static boolean besproei(ServerLevel level, BlockPos p, int kansProcent) {
        level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX() + 0.5, p.getY() + 0.9, p.getZ() + 0.5, 12, 0.35, 0.2, 0.35, 0.02);
        level.sendParticles(ParticleTypes.SPLASH, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.05);
        BlockState s = level.getBlockState(p);
        if (groeit(s) && s.getBlock() instanceof CropBlock crop && level.getRandom().nextInt(100) < kansProcent) {
            level.setBlock(p, crop.getStateForAge(Math.min(crop.getMaxAge(), crop.getAge(s) + 1)), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }

    private class Taak extends StappenTaak {
        private final List<BlockPos> plekken;

        Taak(ServerLevel level, Huisje huisje, Mob mob, List<BlockPos> plekken) {
            super(level, huisje, mob, BellenKlus.this);
            this.plekken = plekken;
        }

        @Override
        protected void begin() {
            for (BlockPos p : plekken) {
                claim(p, 400);
                erbij(loop(p, 1.8));
                erbij(werk(BLAAS_TICKS, Vec3.atCenterOf(p), t -> {
                    if (t % 10 == 0) {
                        level.sendParticles(ParticleTypes.BUBBLE_POP, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.02);
                    }
                    if (t == 1 && mob instanceof BlubGuhEntity b) {
                        BlubGeluid.KLUS_BESPROEIEN.speel(b);
                    }
                }));
                erbij(doe(() -> {
                    besproei(level, p, KANS_PROCENT);
                    aantal++;
                    gelukt();
                }));
            }
        }
    }
}
