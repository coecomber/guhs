package nl.juiced.guhs.feature.klusjes;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.block.KnabbelkorfBlock;
import nl.juiced.guhs.feature.boerderij.BoerderijDier;
import nl.juiced.guhs.feature.boerderij.BoerderijFeature;
import nl.juiced.guhs.feature.boerderij.GuhkoeEntity;
import nl.juiced.guhs.feature.boerderij.KippennestjeBlock;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.registry.ModItems;

/**
 * Dieren & bijen verzorgen: the resident looks after the Guhboerderij animals in the home base: it pets them (care
 * AAIEN) and feeds them knabbelvoer from the huisje's chest (VOEREN): two kinds of care make an animal content, and then
 * the guhschaapje gives pluiswol and the knabbelkippetje lays a knabbelei, which the guh picks up. A content guhkoe is
 * milked with an empty bottle from the chest (kaasmelk). It also empties the kippennestjes and harvests full knabbelkorven
 * (kaasknabbels, or kaashoning when there is a bottle for it). Up to three animals per trip. Guhs only.
 */
public class DierenKlus extends BasisKlus {
    public static final int PER_KEER = 3;

    DierenKlus() {
        super("dieren", () -> new ItemStack(BoerderijFeature.PLUISWOL.get()), 300);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos nest = KlusGebied.kies(level, huisje, KlusGebied.Soort.NEST, bewoner, false, p -> KlusGebied.nestMetEieren(level.getBlockState(p)));
        if (nest != null) {
            return new Taak(level, huisje, bewoner, nest, null);
        }
        BlockPos korf = KlusGebied.kies(level, huisje, KlusGebied.Soort.KORF, bewoner, false, p -> KlusGebied.volleKorf(level.getBlockState(p)));
        if (korf != null) {
            return new Taak(level, huisje, bewoner, korf, null);
        }
        BoerderijDier dier = dier(level, huisje, bewoner, bewoner.position(), 0);
        return dier == null ? null : new Taak(level, huisje, bewoner, null, dier);
    }

    /** An animal in the home base that could use some care now (nearest to {@code bij}; within 8 when r > 0). */
    @Nullable
    static BoerderijDier dier(ServerLevel level, Huisje huisje, Mob wie, Vec3 bij, double r) {
        boolean voer = Voorraad.tel(level, huisje, s -> s.is(BoerderijFeature.KNABBELVOER.get())) > 0;
        boolean fles = Voorraad.tel(level, huisje, s -> s.is(Items.GLASS_BOTTLE)) > 0;
        List<BoerderijDier> dieren = level.getEntitiesOfClass(BoerderijDier.class, huisje.gebied(), d -> d.isAlive()
                && huisje.inGebied(d.blockPosition()) && (r <= 0 || d.position().distanceToSqr(bij) <= r * r) && !KlusGebied.geclaimd(level, d.blockPosition())
                && nodig(d, voer, fles));
        return dieren.stream().min(Comparator.comparingDouble(d -> d.position().distanceToSqr(bij))).orElse(null);
    }

    /** Does this animal want something today (petting, food we have, milking with a bottle we have)? */
    static boolean nodig(BoerderijDier d, boolean voer, boolean fles) {
        if (!d.heeftZorg(BoerderijDier.Zorg.AAIEN) || voer && !d.heeftZorg(BoerderijDier.Zorg.VOEREN)) {
            return true;
        }
        return fles && d instanceof GuhkoeEntity koe && !koe.isBaby() && koe.isBlij() && !koe.productGegeven();
    }

    private class Taak extends StappenTaak {
        @Nullable
        private final BlockPos blok;
        @Nullable
        private BoerderijDier dier;
        private int keer;

        Taak(ServerLevel level, Huisje huisje, Mob mob, @Nullable BlockPos blok, @Nullable BoerderijDier dier) {
            super(level, huisje, mob, DierenKlus.this);
            this.blok = blok;
            this.dier = dier;
        }

        @Override
        protected void begin() {
            if (blok != null) {
                claim(blok, 400);
                erbij(loop(blok, 1.8));
                erbij(werk(KlusGebied.volleKorf(level.getBlockState(blok)) ? 30 : 14, Vec3.atCenterOf(blok), t -> {
                    if (t % 10 == 0 && KlusGebied.volleKorf(level.getBlockState(blok))) {
                        level.playSound(null, blok, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 0.6f, 1.2f);
                    }
                }));
                erbij(doe(this::blokKlus));
            } else if (dier != null) {
                naar(dier);
            }
        }

        private void naar(BoerderijDier d) {
            dier = d;
            erbij(loopNaar(() -> dier, 1.9));
            erbij(werk(26, d.position().add(0, d.getBbHeight() * 0.7, 0), t -> {
                if (t % 8 == 0 && dier != null) {
                    hartjes(dier, 1);
                    dier.getNavigation().stop();
                }
            }));
            erbij(doe(this::verzorg));
        }

        private void blokKlus() {
            BlockState s = level.getBlockState(blok);
            if (KlusGebied.nestMetEieren(s)) {
                int n = s.getValue(KippennestjeBlock.EIEREN);
                level.setBlock(blok, s.setValue(KippennestjeBlock.EIEREN, 0), Block.UPDATE_ALL);
                level.playSound(null, blok, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6f, 1.3f);
                pak(new ItemStack(BoerderijFeature.KNABBELEI.get(), n));
                aantal += n;
                gelukt();
            } else if (KlusGebied.volleKorf(s) && s.getBlock() instanceof KnabbelkorfBlock korf) {
                ItemStack fles = mob.getRandom().nextInt(3) == 0 ? Voorraad.neem(level, huisje, x -> x.is(Items.GLASS_BOTTLE), 1) : ItemStack.EMPTY;
                if (!fles.isEmpty()) {
                    level.playSound(null, blok, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1f, 1f);
                    pak(new ItemStack(ModItems.KAASHONING.get()));
                } else {
                    level.playSound(null, blok, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 1f, 1.1f);
                    pak(new ItemStack(ModItems.KAAS_KNABBELS.get(), 3 + level.random.nextInt(3)));
                }
                level.gameEvent(mob, GameEvent.SHEAR, blok);
                korf.resetHoneyLevel(level, s, blok);        // (guh bees never get angry: nobody gets stung)
                sprankel(Vec3.atCenterOf(blok.above()), 4);
                aantal++;
                gelukt();
            }
        }

        private void verzorg() {
            BoerderijDier d = dier;
            if (d == null || !d.isAlive()) {
                return;
            }
            Vec3 bij = d.position();
            if (!d.heeftZorg(BoerderijDier.Zorg.AAIEN)) {
                d.verzorg(BoerderijDier.Zorg.AAIEN, null);
                gelukt();
            }
            if (!d.heeftZorg(BoerderijDier.Zorg.VOEREN)) {
                ItemStack voer = Voorraad.neem(level, huisje, x -> x.is(BoerderijFeature.KNABBELVOER.get()), 1);
                if (!voer.isEmpty()) {
                    d.verzorg(BoerderijDier.Zorg.VOEREN, null);
                    gelukt();
                }
            }
            if (d instanceof GuhkoeEntity koe && koe.isBlij() && !koe.productGegeven() && !koe.isBaby()) {
                ItemStack fles = Voorraad.neem(level, huisje, x -> x.is(Items.GLASS_BOTTLE), 1);
                if (!fles.isEmpty()) {
                    ItemStack melk = koe.melkZonderSpeler();
                    if (melk.isEmpty()) {
                        pak(fles);                               // (no milk after all: the bottle goes back)
                    } else {
                        pak(melk);
                        gelukt();
                    }
                }
            }
            // what a content schaapje or kippetje just gave (wool at its feet, an egg that found no nest)
            raapOp(bij, 2.2);
            aantal++;
            keer++;
            if (keer < PER_KEER * nl.juiced.guhs.feature.verhaal.VariantGedragen.draagFactor(mob)) {   // (3.0: the 626-guh carries twice as much)
                BoerderijDier nog = dier(level, huisje, mob, bij, 8);
                if (nog != null && nog != d) {
                    naar(nog);
                }
            }
        }
    }
}
