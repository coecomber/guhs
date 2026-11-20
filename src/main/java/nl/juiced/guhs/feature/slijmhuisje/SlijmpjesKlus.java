package nl.juiced.guhs.feature.slijmhuisje;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.klusjes.BasisKlus;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.StappenTaak;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * "Slijmpjes verzorgen" (on by default): a guh of the huisje walks to a Slijmhuisje of the home base that has slimeballs in
 * its bowl, pets the slime (hearts), empties the bowl and brings the slimeballs home ({@code Voorraad.lever}: Bank Guh,
 * Hapluikje, chest, door). The PlantageKlus pattern: only houses the huisje's owner placed (or nobody: a structure's), never
 * in a protected building. Guhs only.
 */
public class SlijmpjesKlus extends BasisKlus {
    public static final String ID = "slijmpjes";
    /** Ticks it pets the slime. */
    public static final int AAI_TICKS = 40;
    /** A house with at least this many slimeballs is worth the walk. */
    public static final int GENOEG = 4;

    SlijmpjesKlus() {
        super(ID, () -> new ItemStack(SlijmhuisjeFeature.SLIJMHUISJE_ITEM.get()), 300);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner);
    }

    @Override
    public String doeners() {
        return "guhs";
    }

    /** The Slijmhuisjes of this home base its residents look after. */
    public static List<SlijmhuisjeBlockEntity> huisjes(ServerLevel level, Huisje h) {
        List<SlijmhuisjeBlockEntity> uit = new ArrayList<>();
        for (BlockPos p : KlusGebied.van(level, h, SlijmhuisjeFeature.KLUS_SOORT)) {
            SlijmhuisjeBlockEntity s = huisje(level, h, p);
            if (s != null) {
                uit.add(s);
            }
        }
        return uit;
    }

    /** The house whose kern stands here, when the residents of this huisje may look after it. */
    @Nullable
    static SlijmhuisjeBlockEntity huisje(ServerLevel level, Huisje h, BlockPos kern) {
        if (level.isLoaded(kern) && level.getBlockEntity(kern) instanceof SlijmhuisjeBlockEntity s && !s.isRemoved()
                && (s.eigenaar() == null || s.eigenaar().equals(h.eigenaar())) && !Bescherming.beschermd(level, kern)) {
            return s;
        }
        return null;
    }

    /** Worth the walk: a few slimeballs in the bowl. */
    static boolean wilVerzorging(SlijmhuisjeBlockEntity s) {
        return s.schaal() >= GENOEG;
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        List<SlijmhuisjeBlockEntity> lijst = huisjes(level, huisje);
        if (lijst.isEmpty()) {
            return KlusStand.nee("geen", 0);
        }
        int klaar = 0, leeg = 0, slaapt = 0;
        for (SlijmhuisjeBlockEntity s : lijst) {
            if (wilVerzorging(s)) {
                klaar++;
            } else if (!s.heeftSlijm()) {
                leeg++;
            } else if (!s.heeftKracht()) {
                slaapt++;
            }
        }
        if (klaar > 0) {
            return KlusStand.ja("ophalen", klaar);
        }
        if (leeg == lijst.size()) {
            return KlusStand.nee("geen_slijm", leeg);
        }
        if (slaapt + leeg == lijst.size()) {
            return KlusStand.nee("geen_kracht", slaapt);
        }
        return KlusStand.straks("maakt", lijst.size() - slaapt - leeg);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos kern = KlusGebied.kies(level, huisje, SlijmhuisjeFeature.KLUS_SOORT, bewoner, p -> {
            SlijmhuisjeBlockEntity s = huisje(level, huisje, p);
            return s != null && wilVerzorging(s);
        });
        return kern == null ? null : new Taak(level, huisje, bewoner, kern);
    }

    private class Taak extends StappenTaak {
        private final BlockPos kern;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos kern) {
            super(level, huisje, mob, SlijmpjesKlus.this);
            this.kern = kern;
        }

        @Nullable
        private SlijmhuisjeBlockEntity huisje() {
            return SlijmpjesKlus.huisje(level, huisje, kern);
        }

        /** Where it stands: in front of the round door (by the bowl). */
        private BlockPos voor() {
            SlijmhuisjeBlockEntity s = huisje();
            return s == null ? kern : kern.relative(s.voor());
        }

        @Override
        protected void begin() {
            claim(kern, 600);
            SlijmhuisjeBlockEntity s = huisje();
            if (s == null) {
                afbreken();
                return;
            }
            Vec3 kussen = Vec3.atCenterOf(s.kussen());
            erbij(loop(voor(), 1.8));
            erbij(werk(AAI_TICKS, kussen, t -> {
                if (t % 10 == 0) {
                    mob.swing(InteractionHand.MAIN_HAND);
                    level.playSound(null, kern, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.5f, 1.3f + mob.getRandom().nextFloat() * 0.3f);
                    level.sendParticles(ParticleTypes.HEART, kussen.x, kussen.y + 0.4, kussen.z, 1, 0.3, 0.2, 0.3, 0.0);
                }
            }));
            erbij(doe(this::leeg));
        }

        private void leeg() {
            SlijmhuisjeBlockEntity s = huisje();
            if (s == null) {
                return;
            }
            hartjes(mob, 3);
            ItemStack ballen = s.leegSchaal();
            if (ballen.isEmpty()) {
                return;
            }
            aantal = ballen.getCount();
            pak(ballen);
            level.playSound(null, kern, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.5f, 1.4f);
            sprankel(Vec3.atCenterOf(s.kussen()), 5);
            gelukt();
        }
    }
}
