package nl.juiced.guhs.feature.klusjes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bakkerij.Bakken;
import nl.juiced.guhs.feature.bakkerij.BakkerijFeature;
import nl.juiced.guhs.feature.bakkerij.KnabbelovenBlock;
import nl.juiced.guhs.feature.bakkerij.Recept;
import nl.juiced.guhs.feature.guhpolder.MolentjeBlockEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.knus.KnusTags;

/**
 * Bakken & molen: knabbelgraan → guh-molentje → knabbelmeel → knabbeloven. The resident carries knabbelgraan from the
 * huisje's chest to a guh-molentje in the home base, brings the knabbelmeel that came out back to the chest, and when the
 * chest has the ingredients of a recipe of the receptenboek ({@link Bakken#nodig}: knabbelmeel or knabbelgraan or wheat,
 * kaasmelk, knabbeleieren, kaasknabbels, sugar...) it bakes it in a knabbeloven: {@link #AANTAL} pastries, twice that with
 * knabbelmeel ({@link Bakken#MEEL_KEER}), into the chest. Empty buckets and bottles go back too. Guhs only.
 */
public class BakkenKlus extends BasisKlus {
    public static final int AANTAL = 2;
    public static final int BAK_TICKS = 100;
    public static final int GRAAN_PER_KEER = 16;

    BakkenKlus() {
        super("bakken", () -> new ItemStack(BakkerijFeature.KNABBELOVEN_ITEM.get()), 400);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos oven = KlusGebied.kies(level, huisje, KlusGebied.Soort.OVEN, bewoner, false,
                p -> level.getBlockState(p).getBlock() instanceof KnabbelovenBlock && !Bakken.bakt(level, p));
        if (oven != null) {
            Recept r = recept(level, huisje, new Random(bewoner.getRandom().nextLong()));
            if (r != null) {
                return new Taak(level, huisje, bewoner, oven, r);
            }
        }
        BlockPos meel = KlusGebied.kies(level, huisje, KlusGebied.Soort.MOLEN, bewoner, false,
                p -> level.getBlockEntity(p) instanceof MolentjeBlockEntity m && !m.meel().isEmpty());
        if (meel != null) {
            return new Taak(level, huisje, bewoner, meel, null);
        }
        if (Voorraad.tel(level, huisje, s -> s.is(KnusTags.KNABBELGRAAN)) > 0) {
            BlockPos molen = KlusGebied.kies(level, huisje, KlusGebied.Soort.MOLEN, bewoner, false,
                    p -> level.getBlockEntity(p) instanceof MolentjeBlockEntity m && m.graan().getCount() < MolentjeBlockEntity.MAX - 8);
            if (molen != null) {
                return new Taak(level, huisje, bewoner, molen, null);
            }
        }
        return null;
    }

    /** A recipe of the receptenboek the huisje has everything for (random order), or null. */
    @Nullable
    public static Recept recept(ServerLevel level, Huisje huisje, Random rng) {
        List<Recept> boek = new ArrayList<>(Recept.BOEK);
        Collections.shuffle(boek, rng);
        for (Recept r : boek) {
            boolean alles = true;
            for (Map.Entry<Bakken.Nodig, Integer> e : Bakken.nodig(r.deeg, r.topping).entrySet()) {
                if (Voorraad.tel(level, huisje, e.getKey()::past) < e.getValue()) {
                    alles = false;
                    break;
                }
            }
            if (alles) {
                return r;
            }
        }
        return null;
    }

    private class Taak extends StappenTaak {
        private final BlockPos plek;
        @Nullable
        private final Recept recept;
        private final List<ItemStack> ingredienten = new ArrayList<>();
        private boolean meel;
        /** Knabbelgraan on its way to the mill (goes back into the chest when the chore is cut short). */
        private ItemStack graan = ItemStack.EMPTY;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos plek, @Nullable Recept recept) {
            super(level, huisje, mob, BakkenKlus.this);
            this.plek = plek;
            this.recept = recept;
        }

        @Override
        protected void begin() {
            claim(plek, 600);
            if (recept != null) {
                bakken();
            } else {
                molen();
            }
        }

        // --- the oven --------------------------------------------------------------------------------------------------

        private void bakken() {
            erbij(loop(Voorraad.brengPlek(level, huisje), 2.3));
            erbij(doe(this::neemIngredienten));
            erbij(loop(plek, 1.9));
            Vec3 oven = Vec3.atCenterOf(plek);
            erbij(werk(BAK_TICKS, oven, t -> {
                if (ingredienten.isEmpty()) {
                    return;
                }
                if (t % 20 == 0) {
                    KnabbelovenBlock.aan(level, plek, 30);
                    level.playSound(null, plek, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 1.2f);
                }
                if (t % 8 == 0) {
                    level.sendParticles(t < BAK_TICKS / 2 ? BakkerijFeature.MEELSTOFJE.get() : BakkerijFeature.KNABBELWOLKJE.get(),
                            oven.x, oven.y + 0.6, oven.z, 3, 0.2, 0.1, 0.2, 0.01);
                }
            }));
            erbij(doe(this::uitDeOven));
        }

        private void neemIngredienten() {
            toon(new ItemStack(BakkerijFeature.KNABBELOVEN_ITEM.get()));
            for (Map.Entry<Bakken.Nodig, Integer> e : Bakken.nodig(recept.deeg, recept.topping).entrySet()) {
                int nodig = e.getValue();
                while (nodig > 0) {
                    ItemStack s = ItemStack.EMPTY;
                    if (e.getKey() == Bakken.Nodig.GRAAN) {
                        s = Voorraad.neem(level, huisje, x -> x.is(Bakken.KNABBELMEEL), nodig);   // knabbelmeel first: twice as much!
                        if (!s.isEmpty()) {
                            meel = true;
                        }
                    }
                    if (s.isEmpty()) {
                        s = Voorraad.neem(level, huisje, e.getKey()::past, nodig);
                    }
                    if (s.isEmpty()) {
                        // (someone took it in the meantime: everything goes back, no baking)
                        for (ItemStack terug : ingredienten) {
                            pak(terug);
                        }
                        ingredienten.clear();
                        afbreken();
                        return;
                    }
                    nodig -= s.getCount();
                    ingredienten.add(s);
                }
            }
        }

        private void uitDeOven() {
            if (ingredienten.isEmpty()) {
                return;
            }
            for (ItemStack s : ingredienten) {
                ItemStack rest = s.getCraftingRemainingItem();
                if (!rest.isEmpty()) {
                    pak(rest.copyWithCount(s.getCount()));      // (the empty bucket / bottle goes back)
                }
            }
            ingredienten.clear();
            int n = AANTAL * (meel ? Bakken.MEEL_KEER : 1);
            pak(new ItemStack(BakkerijFeature.bakje(recept), n));
            aantal = n;
            level.playSound(null, plek, BakkerijFeature.OVEN_DING.get(), SoundSource.BLOCKS, 1f, 1.1f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, plek.getX() + 0.5, plek.getY() + 1.1, plek.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.0);
            gelukt();
        }

        // --- the mill ----------------------------------------------------------------------------------------------------

        private void molen() {
            if (level.getBlockEntity(plek) instanceof MolentjeBlockEntity m && !m.meel().isEmpty()) {
                erbij(loop(plek, 1.9));
                erbij(werk(20, Vec3.atCenterOf(plek), null));
                erbij(doe(() -> {
                    if (level.getBlockEntity(plek) instanceof MolentjeBlockEntity mm) {
                        ItemStack uit = mm.neemMeel();
                        if (!uit.isEmpty()) {
                            pak(uit);
                            aantal = uit.getCount();
                            gelukt();
                        }
                    }
                }));
                return;
            }
            graan = Voorraad.neem(level, huisje, s -> s.is(KnusTags.KNABBELGRAAN), GRAAN_PER_KEER * nl.juiced.guhs.feature.verhaal.VariantGedragen.draagFactor(mob));
            if (graan.isEmpty()) {
                afbreken();
                return;
            }
            toon(graan);
            erbij(loop(plek, 1.9));
            erbij(werk(20, Vec3.atCenterOf(plek), null));
            erbij(doe(() -> {
                int in = 0;
                if (level.getBlockEntity(plek) instanceof MolentjeBlockEntity mm) {
                    in = mm.stort(graan);
                }
                toon(ItemStack.EMPTY);
                if (!graan.isEmpty()) {
                    pak(graan);                                  // (what didn't fit goes back into the chest)
                }
                graan = ItemStack.EMPTY;
                if (in > 0) {
                    aantal = in;
                    gelukt();
                }
            }));
        }

        @Override
        public void stop() {
            for (ItemStack s : ingredienten) {
                pak(s);                                          // (cut short before the oven: the ingredients go back)
            }
            ingredienten.clear();
            if (!graan.isEmpty()) {
                pak(graan);
                graan = ItemStack.EMPTY;
            }
            super.stop();
        }
    }
}
