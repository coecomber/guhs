package nl.juiced.guhs.feature.klusjes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.tuintjes.TuinBlock;
import nl.juiced.guhs.feature.tuintjes.TuintjesFeature;

/**
 * Farmen: the resident harvests ripe crops in the home base (wheat, carrots, potatoes, beetroot, kaasknabbelplantjes:
 * every crop block) and plants them again right away (one seed of the harvest goes back into the ground), and the ripe
 * guhtuintjes (bloempot / moestuinbak: the plant stays and grows again). Up to {@link #PER_KEER} plants per trip, then
 * everything goes to the chest. Nothing ripe? Then it waters the thirsty guhtuintjes instead (little drops from its
 * snoet, like the GuhGietGoal of tamed guhs that don't live in a huisje). Guhs only.
 */
public class FarmenKlus extends BasisKlus {
    public static final int PER_KEER = 5;

    FarmenKlus() {
        super("farmen", () -> new ItemStack(TuintjesFeature.KNABBELGRAAN.get()), 240);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Override
    public String doeners() {
        return "guhs";
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        int rijp = KlusGebied.van(level, huisje, KlusGebied.Soort.GEWAS).size();
        if (rijp > 0) {
            return KlusStand.ja("rijp", rijp);
        }
        int dorst = KlusGebied.van(level, huisje, KlusGebied.Soort.DORSTIG).size();
        if (dorst > 0) {
            return KlusStand.ja("dorstig", dorst);
        }
        int groeit = KlusGebied.van(level, huisje, KlusGebied.Soort.GROEIT).size();
        return groeit > 0 ? KlusStand.straks("groeit", groeit) : KlusStand.nee("geen", 0);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos rijp = KlusGebied.kies(level, huisje, KlusGebied.Soort.GEWAS, bewoner, false, p -> KlusGebied.rijpGewas(level.getBlockState(p)));
        if (rijp != null) {
            return new Taak(level, huisje, bewoner, rijp, true);
        }
        BlockPos dorst = KlusGebied.kies(level, huisje, KlusGebied.Soort.DORSTIG, bewoner, false, p -> TuinBlock.dorstig(level.getBlockState(p)));
        return dorst == null ? null : new Taak(level, huisje, bewoner, dorst, false);
    }

    /** Harvests one ripe plant without a player and plants it again; returns the harvest (seeds for the replanting taken out). */
    public static List<ItemStack> oogst(ServerLevel level, BlockPos pos, @Nullable Mob wie) {
        List<ItemStack> uit = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, wie, ItemStack.EMPTY));
            ItemStack zaad = state.getCloneItemStack(level, pos, false);
            for (ItemStack d : drops) {
                if (!zaad.isEmpty() && ItemStack.isSameItem(d, zaad) && d.getCount() > 0) {
                    d.shrink(1);                                    // (this one goes back into the ground)
                    break;
                }
            }
            level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.8f, 1.1f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 10,
                    0.25, 0.2, 0.25, 0.05);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.0);
            for (ItemStack d : drops) {
                if (!d.isEmpty()) {
                    uit.add(d);
                }
            }
        }
        return uit;
    }

    private class Taak extends StappenTaak {
        private final boolean oogsten;
        private BlockPos doel;
        private int gedaan;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos doel, boolean oogsten) {
            super(level, huisje, mob, FarmenKlus.this);
            this.doel = doel;
            this.oogsten = oogsten;
        }

        @Override
        protected void begin() {
            volgende(doel);
        }

        private void volgende(BlockPos p) {
            doel = p;
            claim(p, 400);
            erbij(loop(p, 1.7));
            erbij(werk(oogsten ? 18 : 14, Vec3.atCenterOf(p), null));
            erbij(doe(this::klus));
        }

        private void klus() {
            BlockState s = level.getBlockState(doel);
            if (oogsten && KlusGebied.rijpGewas(s)) {
                if (TuinBlock.isTuin(s)) {
                    TuinBlock.oogst(level, doel, null);
                    raapOp(Vec3.atCenterOf(doel.above()), 1.4);
                } else {
                    for (ItemStack d : oogst(level, doel, mob)) {
                        pak(d);
                    }
                }
                gedaan++;
                gelukt();
            } else if (!oogsten && TuinBlock.dorstig(s)) {
                giet(doel);
                gedaan++;
                gelukt();
            }
            aantal = gedaan;
            if (gedaan < PER_KEER * nl.juiced.guhs.feature.verhaal.VariantGedragen.draagFactor(mob)) {   // (3.0: the 626-guh carries twice as much)
                BlockPos nog = KlusGebied.kies(level, huisje, oogsten ? KlusGebied.Soort.GEWAS : KlusGebied.Soort.DORSTIG, mob, false,
                        p -> p.distSqr(doel) <= 36 && (oogsten ? KlusGebied.rijpGewas(level.getBlockState(p)) : TuinBlock.dorstig(level.getBlockState(p))));
                if (nog != null) {
                    volgende(nog);
                }
            }
        }

        /** Water from its snoet in a little arc of drops. */
        private void giet(BlockPos plant) {
            if (!TuinBlock.water(level, plant)) {
                return;
            }
            double sx = mob.getX(), sy = mob.getEyeY(), sz = mob.getZ();
            double ex = plant.getX() + 0.5, ey = plant.getY() + 1.0, ez = plant.getZ() + 0.5;
            for (int i = 0; i <= 8; i++) {
                double t = i / 8.0;
                level.sendParticles(TuintjesFeature.GIETERDRUPPEL.get(), sx + (ex - sx) * t, sy + (ey - sy) * t + Math.sin(t * Math.PI) * 0.6,
                        sz + (ez - sz) * t, 1, 0.02, 0.02, 0.02, 0.0);
            }
            level.playSound(null, plant, TuintjesFeature.GIETER_GELUID.get(), SoundSource.NEUTRAL, 0.8f, 1.3f);
        }
    }
}
