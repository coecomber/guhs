package nl.juiced.guhs.feature.landdiertjes;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import nl.juiced.guhs.feature.klusjes.StappenTaak;
import nl.juiced.guhs.feature.klusjes.Voorraad;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;

/**
 * Stenen polijsten (Sjokkel's chore in a Guhhuisje, id {@value #ID}): Sjokkel crawls to the huisje's chest (or Bank Guh), takes
 * a few cobblestones ({@link #PER_KEER}), and polishes them slowly and lovingly with its little feet ({@link #POLIJST_TICKS},
 * dust and sparkles, a soft grinding sound). Out come smooth stone and now and then two shiny
 * {@code landdiertjes_guhsteentje} pebbles instead ({@link #STEENTJES_KANS}); everything goes back into the chest. Only
 * Sjokkel does this chore. The owner gets {@code diertjes/landdiertjes_polijsten} the first time.
 */
public class PolijstenKlus extends BasisKlus {
    public static final String ID = "polijsten";
    public static final int PER_KEER = 4, POLIJST_TICKS = 160;
    /** One cobblestone in this many becomes two guhsteentjes (the rest: smooth stone). */
    public static final int STEENTJES_KANS = 4;

    PolijstenKlus() {
        super(ID, () -> new ItemStack(Items.SMOOTH_STONE), 600);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return bewoner instanceof ShuckleEntity;
    }

    public static boolean isSteen(ItemStack s) {
        return s.is(Items.COBBLESTONE) || s.is(Items.MOSSY_COBBLESTONE) || s.is(Items.COBBLED_DEEPSLATE);
    }

    @Override
    public String doeners() {
        return "sjokkel";
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        if (!Voorraad.heeftOpslag(level, huisje)) {
            return KlusStand.nee("geen_opslag", 0);
        }
        int stenen = (int) Math.min(9999, Voorraad.tel(level, huisje, PolijstenKlus::isSteen));
        return stenen > 0 ? KlusStand.ja("stenen", stenen) : KlusStand.nee("geen_stenen", 0);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        if (Voorraad.tel(level, huisje, PolijstenKlus::isSteen) <= 0 || !Voorraad.heeftOpslag(level, huisje)) {
            return null;
        }
        return new Taak(level, huisje, bewoner);
    }

    /** What polishing these stones gives (the stones are used up): smooth stone, now and then two guhsteentjes instead. */
    public static void opbrengst(ItemStack stenen, net.minecraft.util.RandomSource rng, java.util.function.Consumer<ItemStack> uit) {
        int glad = 0, steentjes = 0;
        for (int i = 0; i < stenen.getCount(); i++) {
            if (rng.nextInt(STEENTJES_KANS) == 0) {
                steentjes += 2;
            } else {
                glad++;
            }
        }
        if (glad > 0) {
            uit.accept(new ItemStack(Items.SMOOTH_STONE, glad));
        }
        if (steentjes > 0) {
            uit.accept(new ItemStack(LanddiertjesFeature.GUHSTEENTJE.get(), steentjes));
        }
    }

    private class Taak extends StappenTaak {
        private ItemStack stenen = ItemStack.EMPTY;

        Taak(ServerLevel level, Huisje huisje, Mob mob) {
            super(level, huisje, mob, PolijstenKlus.this);
        }

        @Override
        protected void begin() {
            BlockPos kist = Voorraad.brengPlek(level, huisje);
            erbij(loop(kist, 2.3));
            erbij(doe(() -> {
                stenen = Voorraad.neem(level, huisje, PolijstenKlus::isSteen, PER_KEER * VariantGedragen.draagFactor(mob));
                if (stenen.isEmpty()) {
                    afbreken();
                }
            }));
            Vec3 voor = Vec3.atCenterOf(kist);
            erbij(werk(POLIJST_TICKS, voor, t -> {
                if (stenen.isEmpty()) {
                    return;
                }
                if (t % 20 == 0 && mob instanceof ShuckleEntity s) {
                    s.triggerAnim("actie", "poets");
                    level.playSound(null, mob.blockPosition(), LanddiertjesFeature.POLIJSTEN.get(), SoundSource.NEUTRAL, 0.5f,
                            1.1f + mob.getRandom().nextFloat() * 0.3f);
                }
                if (t % 6 == 0) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                            mob.getX(), mob.getY() + 0.2, mob.getZ(), 3, 0.25, 0.05, 0.25, 0.02);
                }
                if (t % 14 == 0) {
                    sprankel(new Vec3(mob.getX(), mob.getY() + 0.5, mob.getZ()), 1);
                }
            }));
            erbij(doe(() -> {
                if (stenen.isEmpty()) {
                    return;
                }
                aantal = stenen.getCount();
                opbrengst(stenen, mob.getRandom(), this::pak);
                stenen = ItemStack.EMPTY;
                gelukt();
            }));
        }

        @Override
        public void stop() {
            if (!stenen.isEmpty()) {
                pak(stenen);                                     // (cut short before polishing: the stones go back)
                stenen = ItemStack.EMPTY;
            }
            boolean klaar = isGelukt();
            super.stop();
            if (klaar) {
                ServerPlayer owner = Band.eigenaarOnline(mob);
                if (owner != null) {
                    GidsFeature.grant(owner, "diertjes/landdiertjes_polijsten");
                }
            }
        }

        @Override
        public int maxTicks() {
            return 2400;                                         // (Sjokkel is slow...)
        }
    }
}
