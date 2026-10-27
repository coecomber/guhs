package nl.juiced.guhs.feature.blub.klus;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.StappenTaak;
import nl.juiced.guhs.feature.klusjes.Voorraad;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.registry.ModItems;

/**
 * Knabbelbezorger (a Blub's chore, id {@value #ID}): the Blub takes a few kaasknabbels from the huisje's chest (or Bank Guh)
 * and hops them one by one to the tame guhs and tame critters around the huisje that could do with a snack (not fed for a
 * while: {@link #RUST} ticks per eater). They eat it (a little health, a happy SMAKKEN for a guh) and there are hearts.
 */
public class BezorgKlus extends BlubKlusjes.BlubKlus {
    public static final String ID = "blub_bezorgen";
    public static final int PER_KEER = 3;
    /** A fed one waits this long before the Blub brings it the next knabbel (about 10 minutes). */
    public static final int RUST = 20 * 60 * 10;
    static final String GEVOERD_TOT = "guhs_blub_gevoerd_tot";

    BezorgKlus() {
        super(ID, () -> new ItemStack(BlubFeature.KLUS_ICONEN.get("knabbels").get()), 300);
    }

    static boolean isKnabbel(ItemStack s) {
        return s.is(ModItems.KAAS_KNABBELS.get());
    }

    /** Tame guhs and tame critters (not Blubs) in the huisje's area that haven't had a knabbel for a while. */
    static List<LivingEntity> eters(ServerLevel level, Huisje huisje) {
        long nu = level.getGameTime();
        return level.getEntitiesOfClass(LivingEntity.class, huisje.gebied(), e -> e.isAlive() && !(e instanceof BlubGuhEntity)
                && (e instanceof GuhEntity || e instanceof PiepMaatje) && e instanceof TamableAnimal t && t.isTame()
                && huisje.inGebied(e.blockPosition()) && KlusGebied.inTest(huisje, e.position()) && !nl.juiced.guhs.feature.huisje.Huisjes.isBinnen(e)
                && e.getPersistentData().getLongOr(GEVOERD_TOT, 0L) <= nu);
    }

    @Override
    public KlusStand stand(ServerLevel level, Huisje huisje) {
        if (!Voorraad.heeftOpslag(level, huisje)) {
            return KlusStand.nee("geen_opslag", 0);
        }
        if (Voorraad.tel(level, huisje, BezorgKlus::isKnabbel) <= 0) {
            return KlusStand.nee("geen_knabbels", 0);
        }
        int n = eters(level, huisje).size();
        return n > 0 ? KlusStand.ja("hongerig", n) : KlusStand.straks("vol", 0);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        if (!Voorraad.heeftOpslag(level, huisje) || Voorraad.tel(level, huisje, BezorgKlus::isKnabbel) <= 0) {
            return null;
        }
        List<LivingEntity> eters = eters(level, huisje);
        return eters.isEmpty() ? null : new Taak(level, huisje, bewoner, eters.subList(0, Math.min(PER_KEER, eters.size())));
    }

    /** One knabbel for this eater: a bite, a little health, hearts (a guh smacks its lips). */
    public static void geef(ServerLevel level, LivingEntity eter) {
        eter.getPersistentData().putLong(GEVOERD_TOT, level.getGameTime() + RUST);
        eter.heal(2f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ModItems.KAAS_KNABBELS.get()), eter.getX(), eter.getY() + eter.getBbHeight() * 0.7,
                eter.getZ(), 6, 0.15, 0.05, 0.15, 0.03);
        level.sendParticles(ParticleTypes.HEART, eter.getX(), eter.getY() + eter.getBbHeight() + 0.2, eter.getZ(), 3, 0.3, 0.2, 0.3, 0);
        if (eter instanceof GuhEntity g) {
            g.emotes.start(Emote.SMAKKEN, false, GuhEmotes.Source.SELF);
        }
    }

    private class Taak extends StappenTaak {
        private final List<LivingEntity> eters;
        private ItemStack knabbels = ItemStack.EMPTY;

        Taak(ServerLevel level, Huisje huisje, Mob mob, List<LivingEntity> eters) {
            super(level, huisje, mob, BezorgKlus.this);
            this.eters = List.copyOf(eters);
        }

        @Override
        protected void begin() {
            erbij(loop(Voorraad.brengPlek(level, huisje), 2.3));
            erbij(doe(() -> {
                knabbels = Voorraad.neem(level, huisje, BezorgKlus::isKnabbel, eters.size());
                if (knabbels.isEmpty()) {
                    afbreken();
                }
            }));
            for (LivingEntity eter : eters) {
                erbij(loopNaar(() -> eter, 1.6));
                erbij(doe(() -> {
                    if (knabbels.isEmpty() || !eter.isAlive()) {
                        return;
                    }
                    knabbels.shrink(1);
                    geef(level, eter);
                    if (mob instanceof BlubGuhEntity b) {
                        BlubGeluid.KLUS_BEZORGEN.speel(b);
                    }
                    hartjes(mob, 1);
                    aantal++;
                    gelukt();
                }));
            }
        }

        @Override
        public void stop() {
            if (!knabbels.isEmpty()) {
                pak(knabbels);                                      // (left over: back to the chest)
                knabbels = ItemStack.EMPTY;
            }
            super.stop();
        }
    }
}
