package nl.juiced.guhs.feature.einde2.trap5;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Werktuigguh at his workbench ({@code zuidelijke_luchttempel/werktuigguh}, CONTRACT_E2_B 4.4): the teacher of tier 5.
 * Goggles on his forehead, a monocle on a chain, three wooden fingers, never finished with anything ("nog niet af, njeg")
 * until the Guhvatar comes. He once built war balloons for the Fire Nation; now he wants to make things nobody is afraid of.
 * <p>
 * Per step of {@value E2Trap5#LIJN}: what the project is and how to do it, and the first machine of that project as a gift
 * (once). The projects finish by themselves in the player's own world ({@link E2Trap5#meld}); at the end he finishes
 * something too: the diploma. Sneak + click: a lost Bouwplan again, for a few knabbels.
 * <p>
 * hook fase C: {@link #NA_KLAAR} (asked first when the line is done; e.g. the Vredesballon's slice may answer there).
 */
public final class WerktuigguhRol extends QuestRol {
    static final String Q = "quest.guhs.einde2_trap5_machines.werktuigguh.";
    static final int TIPS = 5;
    /** After the line: a listener that answers the talk itself (true = handled). The first true wins. */
    public static final List<BiPredicate<GuhNpcEntity, ServerPlayer>> NA_KLAAR = new CopyOnWriteArrayList<>();

    public WerktuigguhRol() {
        super(E2Trap5.lijn());
    }

    @Override
    protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.9f);
        if (!E2Trap5.open(p)) {
            zeg(p, npc, Q + "nog_niet");
            return;
        }
        if (p.isShiftKeyDown() && heeftPlanGehad(p)) {
            npc.openShop(p);
            return;
        }
        E2Trap5.lijn().begin(p);
        switch (stap) {
            case E2Trap5.S_PRAAT -> {
                zeg(p, npc, Q + "welkom1");
                zeg(p, npc, Q + "welkom2");
                zeg(p, npc, Q + "welkom3");
                geefEenmalig(p, E2Trap5.E_PLAN, new ItemStack(E2Trap5Feature.BOUWPLAN.get()));
                hint(p, Q + "hint.plan");
                verder(p, E2Trap5.S_PRAAT);
                E2Trap5.vooruit(p);
                project(npc, p, E2Trap5.lijn().stap(p));
            }
            case E2Trap5.S_TERUG -> {
                if (verder(p, E2Trap5.S_TERUG)) {
                    zeg(p, npc, Q + "klaar1");
                    zeg(p, npc, Q + "klaar2");
                    zeg(p, npc, Q + "klaar3");
                    geefEenmalig(p, E2Trap5.E_BELONING, new ItemStack(E2Trap5Feature.DIPLOMA.get()), new ItemStack(E2Trap5Feature.THUISPOORT_ITEM.get()));
                    zichtbaar(p, "guheinde/einde2_trap5_machines_klaar");
                    feest(npc);
                    hint(p, Q + "hint.ballon");
                }
            }
            default -> {
                if (stap >= E2Trap5.STAPPEN) {
                    na(npc, p);
                } else {
                    E2Trap5.vooruit(p);
                    project(npc, p, E2Trap5.lijn().stap(p));
                }
            }
        }
    }

    /** Now and then he tinkers at something small on his bench (it is never finished, of course). */
    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.speelt().isEmpty() && npc.tickCount % 40 == 0 && npc.getRandom().nextInt(6) == 0) {
            npc.speel("knutsel");
            if (npc.level() instanceof ServerLevel level && npc.getRandom().nextBoolean()) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, npc.getX(), npc.getY() + 1.0, npc.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
            }
        }
    }

    /** What the player's project is now, and its first machine as a gift (once). */
    private void project(GuhNpcEntity npc, ServerPlayer p, int stap) {
        if (stap < E2Trap5.S_ZENDER || stap > E2Trap5.S_TERUG) {
            return;
        }
        if (stap == E2Trap5.S_TERUG) {
            zeg(p, npc, Q + "alles_af");
            return;
        }
        zeg(p, npc, Q + "project" + stap + ".a");
        zeg(p, npc, Q + "project" + stap + ".b");
        List<ItemStack> kit = switch (stap) {
            case E2Trap5.S_ZENDER -> List.of(new ItemStack(E2Trap5Feature.ZENDER_ITEM.get()), new ItemStack(E2Trap5Feature.ONTVANGER_ITEM.get()));
            case E2Trap5.S_TERMINAL -> List.of(new ItemStack(E2Trap5Feature.TERMINAL_ITEM.get()));
            case E2Trap5.S_BESTEL -> List.of(new ItemStack(E2Trap5Feature.BESTELBALIE_ITEM.get()));
            case E2Trap5.S_POORTBUIS -> List.of(new ItemStack(E2Trap5Feature.POORTBUIS_ITEM.get(), 2));
            case E2Trap5.S_THUISPOORT -> List.of(new ItemStack(E2Trap5Feature.THUISPOORT_ITEM.get(), 2));
            default -> List.of();
        };
        if (!kit.isEmpty() && geefEenmalig(p, E2Trap5.E_KIT + stap, kit.toArray(ItemStack[]::new))) {
            zeg(p, npc, Q + "kit");
        }
        hint(p, Q + "hint.project" + stap);
    }

    /** The line is done: a listener of another slice first, else a tip or the Vredesballon. */
    private void na(GuhNpcEntity npc, ServerPlayer p) {
        for (BiPredicate<GuhNpcEntity, ServerPlayer> l : NA_KLAAR) {
            if (l.test(npc, p)) {
                return;
            }
        }
        zeg(p, npc, Q + "tip" + npc.getRandom().nextInt(TIPS));
    }

    private boolean heeftPlanGehad(ServerPlayer p) {
        return nl.juiced.guhs.quest.GuhQuests.saved(p).getBooleanOr("guhs_" + E2Trap5.LIJN + "_e_" + E2Trap5.E_PLAN, false);
    }

    private static void feest(GuhNpcEntity npc) {
        npc.speel("eureka");
        npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.1f);
        if (npc.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1.2, npc.getZ(), 24, 0.6, 0.6, 0.6, 0.02);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, npc.getX(), npc.getY() + 1.5, npc.getZ(), 16, 0.5, 0.4, 0.5, 0.05);
        }
    }

    /** A lost Bouwplan costs a few knabbels (sneak + click, once the player had one). */
    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 8), Optional.empty(), new ItemStack(E2Trap5Feature.BOUWPLAN.get()),
                Integer.MAX_VALUE, 0, 0));
        return offers;
    }
}
