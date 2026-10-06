package nl.juiced.guhs.feature.techklus;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import nl.juiced.guhs.feature.klusjes.KlusBeloning;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.StappenTaak;
import nl.juiced.guhs.feature.klusjes.Voorraad;
import nl.juiced.guhs.feature.techmachine.PlantagebakBlockEntity;
import nl.juiced.guhs.feature.techmachine.Plantagebakken;
import nl.juiced.guhs.feature.techmachine.TechmachineFeature;

/**
 * Plantage (bbq2): the resident works the Plantagebakken of the home base, and only those: it never touches a tree that
 * grows anywhere else.
 * <ul>
 *   <li>a tree stands on a bak: it walks up to it, swings its little axe for a while and the whole tree comes down
 *       ({@link PlantagebakBlockEntity#hak}); the logs, sticks and apples go to the Bank Guh / the Hapluikje / the chest,
 *       a few saplings stay in the bak, which plants the next tree by itself;</li>
 *   <li>a bak is empty and has no sapling left: it fetches up to {@link #ZAAILINGEN} saplings of one kind from the
 *       huisje's stock (anything a Plantagebak takes: saplings, sate- and worstzwammetjes, mushrooms, azalea) and puts
 *       them in. What does not fit goes back.</li>
 * </ul>
 * Chopping needs no vadskracht, growing does (that is the bak's business). Guhs only; a bak somebody else placed is left
 * alone.
 */
public class PlantageKlus extends BasisKlus {
    public static final String ID = "plantage";
    /** How many saplings go into an empty bak (four: a dark oak wants a square of them). */
    public static final int ZAAILINGEN = 4;
    public static final int HAK_TICKS = 50, PLANT_TICKS = 20;

    PlantageKlus() {
        super(ID, () -> new ItemStack(TechmachineFeature.PLANTAGEBAK_ITEM.get()), 300);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Override
    public String doeners() {
        return "guhs";
    }

    /** The Plantagebakken of this home base its residents work. */
    public static List<PlantagebakBlockEntity> bakken(ServerLevel level, Huisje h) {
        List<PlantagebakBlockEntity> uit = new ArrayList<>();
        for (BlockPos p : KlusGebied.van(level, h, TechklusFeature.BAK)) {
            PlantagebakBlockEntity bak = bak(level, h, p);
            if (bak != null) {
                uit.add(bak);
            }
        }
        return uit;
    }

    /** The bak whose kern stands here, when the residents of this huisje may work it. */
    @Nullable
    static PlantagebakBlockEntity bak(ServerLevel level, Huisje h, BlockPos kern) {
        if (level.isLoaded(kern) && level.getBlockEntity(kern) instanceof PlantagebakBlockEntity bak && !bak.isRemoved()
                && (bak.eigenaar() == null || bak.eigenaar().equals(h.eigenaar()))) {
            return bak;
        }
        return null;
    }

    /** An empty bak without a sapling left (and one that has not just refused to grow something here). */
    static boolean wilZaailing(PlantagebakBlockEntity bak) {
        return bak.stand() == PlantagebakBlockEntity.Stand.LEEG && bak.voorraad().isEmpty() && !bak.wilNiet();
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        List<PlantagebakBlockEntity> bakken = bakken(level, huisje);
        if (bakken.isEmpty()) {
            return KlusStand.nee("geen", 0);
        }
        int bomen = 0, leeg = 0, slaapt = 0;
        for (PlantagebakBlockEntity bak : bakken) {
            if (bak.heeftBoom()) {
                bomen++;
            } else if (wilZaailing(bak)) {
                leeg++;
            } else if (!bak.heeftKracht()) {
                slaapt++;
            }
        }
        if (bomen > 0) {
            return KlusStand.ja("bomen", bomen);
        }
        if (leeg > 0) {
            return Voorraad.tel(level, huisje, Plantagebakken::isZaailing) > 0 ? KlusStand.ja("planten", leeg) : KlusStand.nee("geen_zaailing", leeg);
        }
        return slaapt == bakken.size() ? KlusStand.nee("geen_kracht", slaapt) : KlusStand.straks("groeit", bakken.size() - slaapt);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos boom = KlusGebied.kies(level, huisje, TechklusFeature.BAK, bewoner, p -> {
            PlantagebakBlockEntity bak = bak(level, huisje, p);
            return bak != null && bak.heeftBoom();
        });
        if (boom != null) {
            return new Taak(level, huisje, bewoner, boom, true);
        }
        if (Voorraad.tel(level, huisje, Plantagebakken::isZaailing) <= 0) {
            return null;
        }
        BlockPos leeg = KlusGebied.kies(level, huisje, TechklusFeature.BAK, bewoner, p -> {
            PlantagebakBlockEntity bak = bak(level, huisje, p);
            return bak != null && wilZaailing(bak);
        });
        return leeg == null ? null : new Taak(level, huisje, bewoner, leeg, false);
    }

    private class Taak extends StappenTaak {
        private final BlockPos kern;
        private final boolean hakken;
        /** Saplings on their way to the bak (back into the stock when the chore is cut short). */
        private ItemStack zaailingen = ItemStack.EMPTY;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos kern, boolean hakken) {
            super(level, huisje, mob, PlantageKlus.this);
            this.kern = kern;
            this.hakken = hakken;
        }

        @Nullable
        private PlantagebakBlockEntity bak() {
            return PlantageKlus.bak(level, huisje, kern);
        }

        /** Where it stands to work: in front of the bak's snoet (the bed itself is nine blocks of earth). */
        private BlockPos voor() {
            PlantagebakBlockEntity bak = bak();
            return bak == null ? kern : kern.relative(bak.voor());
        }

        @Override
        protected void begin() {
            claim(kern, 600);
            PlantagebakBlockEntity bak = bak();
            if (bak == null) {
                afbreken();
                return;
            }
            Vec3 stam = Vec3.atCenterOf(bak.plantPlek());
            if (hakken) {
                erbij(loop(voor(), 1.8));
                erbij(doe(() -> toon(new ItemStack(Items.IRON_AXE))));
                erbij(werk(HAK_TICKS, stam, t -> {
                    if (t % 10 == 0) {
                        mob.swing(InteractionHand.MAIN_HAND);
                        BlockState hout = level.getBlockState(BlockPos.containing(stam));
                        level.playSound(null, kern, SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 0.8f, 0.9f + mob.getRandom().nextFloat() * 0.3f);
                        if (!hout.isAir()) {
                            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, hout), stam.x, stam.y, stam.z, 6, 0.3, 0.3, 0.3, 0.05);
                        }
                    }
                }));
                erbij(doe(this::hak));
            } else {
                erbij(loop(Voorraad.brengPlek(level, huisje), 2.3));
                erbij(doe(() -> {
                    zaailingen = Voorraad.neem(level, huisje, Plantagebakken::isZaailing, ZAAILINGEN);
                    if (zaailingen.isEmpty()) {
                        afbreken();
                    } else {
                        toon(zaailingen);
                    }
                }));
                erbij(loop(voor(), 1.8));
                erbij(werk(PLANT_TICKS, stam, null));
                erbij(doe(this::plant));
            }
        }

        private void hak() {
            toon(ItemStack.EMPTY);
            PlantagebakBlockEntity bak = bak();
            if (bak == null || !bak.heeftBoom()) {
                return;
            }
            int hout = 0;
            for (ItemStack stack : bak.hak(mob)) {
                if (stack.is(ItemTags.LOGS)) {
                    hout += stack.getCount();
                }
                pak(stack);
            }
            aantal = hout;
            level.playSound(null, kern, SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1f, 0.8f);
            sprankel(Vec3.atCenterOf(bak.plantPlek()), 6);
            gelukt();
            KlusBeloning.techniek(mob, "tech_klusjes_plantage");
        }

        private void plant() {
            toon(ItemStack.EMPTY);
            PlantagebakBlockEntity bak = bak();
            int erin = bak == null || zaailingen.isEmpty() ? 0 : bak.plant(zaailingen);
            if (!zaailingen.isEmpty()) {
                pak(zaailingen);   // (what did not fit goes back into the stock)
            }
            zaailingen = ItemStack.EMPTY;
            if (erin > 0 && bak != null) {
                aantal = erin;
                level.playSound(null, kern, SoundEvents.GRASS_PLACE, SoundSource.NEUTRAL, 0.8f, 1.1f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, bak.plantPlek().getX() + 0.5, bak.plantPlek().getY() + 0.4, bak.plantPlek().getZ() + 0.5,
                        5, 0.3, 0.2, 0.3, 0.0);
                gelukt();
                KlusBeloning.techniek(mob, "tech_klusjes_plantage");
            }
        }

        @Override
        public void stop() {
            if (!zaailingen.isEmpty()) {
                pak(zaailingen);
                zaailingen = ItemStack.EMPTY;
            }
            super.stop();
        }
    }
}
