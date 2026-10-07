package nl.juiced.guhs.feature.techquest;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.fossielmijn.FossielmijnFeature;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.techbron.AangebrandeMika;
import nl.juiced.guhs.feature.techsaus.TechsausFeature;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (tech-quests): the Uitvinder-guh of the Oude Guhrad-centrale. One guh, two questlines, both per player:
 * <ol start="0">
 *   <li><b>techniek</b> ({@link TechquestFeature#TECHNIEK}): talk; mend setup 1 (the loose wire) and 2 (too heavy); bring
 *       {@link #ZOUT} zoutkristallen; mend setup 3 (the tube that runs backwards), 4 (the filter) and 5 (the missing hose);
 *       talk: the Bodemloos Knabbelmaagje (the Bank Guh's upgrade) and the three recipe cards of the "Saus" tier.</li>
 *   <li><b>knabbelmachine</b> ({@link TechquestFeature#KNABBELMACHINE}), for who finished the first and beat the Aangebrande
 *       Mika ({@link AangebrandeMika#verslagen}): five stages of {@link Knabbelmachine#FASEN}; every talk hands in what the
 *       player carries. Then: the statuette, the title Knabbelmachinist, a perfect knabbel a day.</li>
 * </ol>
 * The setups themselves are {@link Centrale}'s. Sneak + click (after the first questline): he sells a lost recipe card again.
 */
public final class UitvinderRol extends QuestRol {
    public static final int ZOUT = 4, TIPS = 4;
    private static final String Q = "quest.guhs.techquest.uitvinder.";

    public UitvinderRol() {
        super(TechquestFeature.TECHNIEK);
    }

    @Override
    protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        switch (stap) {
            case 0 -> {
                zeg(p, npc, Q + "hallo1");
                zeg(p, npc, Q + "hallo2");
                verder(p, 0);
                geefEenmalig(p, "draad", new ItemStack(ModBlocks.GUH_WIRE.get().asItem(), 2));
                hint(p, Q + "hint.draad");
            }
            case 1 -> {
                zeg(p, npc, Q + "draad");
                if (geefAlsKwijt(p, ModBlocks.GUH_WIRE.get().asItem())) {
                    zeg(p, npc, Q + "draad_weer");
                }
            }
            case 2 -> zeg(p, npc, Q + "te_zwaar");
            case 3 -> {
                if (lever(p, npc, 3, FossielmijnFeature.ZOUTKRISTAL.get(), ZOUT, Q + "zout")) {
                    zeg(p, npc, Q + "zout_dank");
                    hint(p, Q + "hint.buis");
                }
            }
            case 4 -> zeg(p, npc, Q + "buis");
            case 5 -> zeg(p, npc, Q + "filter");
            case 6 -> {
                zeg(p, npc, Q + "saus");
                if (geefAlsKwijt(p, TechsausFeature.SAUSSLANG_ITEM.get())) {
                    zeg(p, npc, Q + "slang");
                }
            }
            case 7 -> {
                if (verder(p, 7)) {
                    zeg(p, npc, Q + "klaar1");
                    zeg(p, npc, Q + "klaar2");
                    geefEenmalig(p, "beloning", new ItemStack(BankFeature.BANK_UPGRADE.get()), new ItemStack(TechquestFeature.RECEPT_SAUS.get()),
                            new ItemStack(TechquestFeature.RECEPT_MACHINES.get()), new ItemStack(TechquestFeature.RECEPT_BEZORG.get()));
                    zichtbaar(p, "techniek/tech_quests_centrale");
                    feest(npc);
                    hint(p, Q + "hint.winkel");
                }
            }
            default -> {
                if (p.isShiftKeyDown()) {
                    npc.openShop(p);
                } else {
                    machine(npc, p);
                }
            }
        }
    }

    /** The second questline: De Grote Knabbelmachine. */
    private void machine(GuhNpcEntity npc, ServerPlayer p) {
        Verhaallijn lijn = TechquestFeature.KNABBELMACHINE;
        int fase = lijn.stap(p);
        if (fase == 0) {
            if (!AangebrandeMika.verslagen(p)) {
                zeg(p, npc, Q + "km.eerst_mika");
                zeg(p, npc, Q + "tip" + npc.getRandom().nextInt(TIPS));
                return;
            }
            zeg(p, npc, Q + "km.plan1");
            zeg(p, npc, Q + "km.plan2");
            lijn.verder(p, 0);
            Knabbelmachine.sync(p);
            vraag(npc, p, 1);
            return;
        }
        if (fase >= Knabbelmachine.FASE_KLAAR) {
            zeg(p, npc, Knabbelmachine.knabbelKlaar(p) ? Q + "km.knabbel_klaar" : Q + "tip" + npc.getRandom().nextInt(TIPS));
            return;
        }
        Knabbelmachine.Uitkomst uit = Knabbelmachine.lever(p);
        if (uit.faseKlaar()) {
            zeg(p, npc, Q + "km.fase" + fase + ".klaar");
            Knabbelmachine.sync(p);
            npc.level().playSound(null, npc, SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.7f, 1.1f);
            if (uit.machineKlaar()) {
                zeg(p, npc, Q + "km.af1");
                zeg(p, npc, Q + "km.af2");
                if (lijn.eenmalig(p, "beeldje")) {
                    geef(p, new ItemStack(TechquestFeature.KNABBELMACHINE_BEELDJE_ITEM.get()));
                }
                zichtbaar(p, "techniek/tech_quests_knabbelmachine");
                feest(npc);
            } else {
                vraag(npc, p, fase + 1);
            }
            return;
        }
        if (uit.genomen() > 0) {
            zeg(p, npc, Q + "km.dank", uit.genomen());
        }
        vraag(npc, p, fase);
    }

    /** What stage {@code fase} still needs, one line per thing. */
    private void vraag(GuhNpcEntity npc, ServerPlayer p, int fase) {
        zeg(p, npc, Q + "km.fase" + fase + ".vraag");
        for (Knabbelmachine.Levering l : Knabbelmachine.FASEN.get(fase - 1)) {
            int heeft = Knabbelmachine.geleverd(p, fase, l);
            boolean binnen = heeft >= l.aantal();
            p.sendSystemMessage(Component.translatable(binnen ? "gui.guhs.techquest.levering.binnen" : "gui.guhs.techquest.levering.nog",
                    Component.translatable(l.tekst()), heeft, l.aantal()).withStyle(binnen ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }
    }

    private static void feest(GuhNpcEntity npc) {
        npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
        if (npc.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1.2, npc.getZ(), 24, 0.6, 0.6, 0.6, 0.02);
        }
    }

    /** A lost recipe card costs a few knabbels (the shop only opens for who finished the practice hall: sneak + click). */
    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        for (var kaart : List.of(TechquestFeature.RECEPT_SAUS, TechquestFeature.RECEPT_MACHINES, TechquestFeature.RECEPT_BEZORG)) {
            offers.add(new MerchantOffer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 8), Optional.empty(), new ItemStack(kaart.get()), Integer.MAX_VALUE, 0, 0));
        }
        offers.add(new MerchantOffer(new ItemCost(ModItems.KAAS_KNABBELS.get(), 2), Optional.empty(), new ItemStack(ModBlocks.GUH_WIRE.get().asItem(), 3),
                Integer.MAX_VALUE, 0, 0));
        return offers;
    }

    /** What the Guhdex shows as "needed" for a step of the first questline. */
    static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        return switch (stap) {
            case 1 -> List.of(Verhaallijn.nodig("guhs:guh_wire", GuhQuests.count(p, ModBlocks.GUH_WIRE.get().asItem()), 1));
            case 3 -> List.of(Verhaallijn.nodig("guhs:zoutkristal", GuhQuests.count(p, FossielmijnFeature.ZOUTKRISTAL.get()), ZOUT));
            case 6 -> List.of(Verhaallijn.nodig("guhs:sausslang", GuhQuests.count(p, TechsausFeature.SAUSSLANG_ITEM.get()), 1));
            default -> List.of();
        };
    }
}
