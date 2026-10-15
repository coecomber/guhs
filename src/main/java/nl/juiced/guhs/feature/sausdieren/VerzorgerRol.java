package nl.juiced.guhs.feature.sausdieren;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * De Verzorger-guh of the Sausloper-stal (kind VERZORGERGUH): the questline {@link SausdierenFeature#LIJN}, per player.
 * <ol start="0">
 *   <li>Talk: he gives you pindasaus aan een stok (once; again when you lost it while you still need it).</li>
 *   <li>Lure a Sausloper to him with the stick ({@link SausdierenEvents}: a Sausloper that follows YOU, next to him).</li>
 *   <li>Feed a Sausloper of the stable {@link SausdierenFeature#VOER_NODIG} pindascheutjes (he gives you the first three);
 *       {@link #gevoerd} counts them for you alone: the Sauslopers themselves stay nobody's.</li>
 *   <li>Ride the test lap: he whistles a saddled Sausloper for you ({@link Proefrit}).</li>
 *   <li>Come back: a saddle and some pindascheutjes, and from now on you can tame wild Sauslopers.</li>
 * </ol>
 * Afterwards he chats, whistles a Sausloper for another (timed) lap whenever you like, and has a small Sausblubje in a jar
 * for whoever carries none (one a day: {@link #blubje}).
 */
public final class VerzorgerRol extends QuestRol {
    private static final String T = "quest.guhs.sausdieren.verzorger.";
    private static final int JA = 1, UITLEG = 2, BLUBJE = 3;
    private static final Verhaallijn LIJN = SausdierenFeature.LIJN;

    public VerzorgerRol() {
        super(SausdierenFeature.LIJN);
    }

    @Override
    protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
        switch (stap) {
            case 0 -> {
                LIJN.begin(p);
                scherm(p, npc, T + "hallo", new Praat.Optie(JA, "gui.guhs.sausdieren.optie.graag"));
            }
            case 1 -> {
                if (geefAlsKwijt(p, SausdierenFeature.PINDASAUS_STOK.get())) {
                    zeg(p, npc, T + "stok_kwijt");
                } else {
                    zeg(p, npc, T + "lok");
                }
                hint(p, "quest.guhs.sausdieren.hint.lok");
            }
            case 2 -> {
                int nog = SausdierenFeature.VOER_NODIG - SausdierenFeature.voer(p);
                if (geefEenmalig(p, "scheutjes", new ItemStack(BarbecuetherFeature.PINDASCHEUTJES.get(), SausdierenFeature.VOER_NODIG))) {
                    zeg(p, npc, T + "voer");
                } else if (GuhQuests.count(p, BarbecuetherFeature.PINDASCHEUTJES.get().asItem()) == 0) {
                    // (eaten, lost, fed to the wrong one: the stable's own garden is protected, so he hands out what is still needed)
                    geef(p, new ItemStack(BarbecuetherFeature.PINDASCHEUTJES.get(), nog));
                    zeg(p, npc, T + "voer_kwijt", nog);
                } else {
                    zeg(p, npc, T + "voer_nog", nog);
                }
                hint(p, "quest.guhs.sausdieren.hint.voer");
            }
            case 3 -> scherm(p, npc, T + "proefrit", new Praat.Optie(JA, "gui.guhs.sausdieren.optie.opstappen"));
            case 4 -> {
                if (verder(p, 4)) {
                    geefEenmalig(p, "beloning", new ItemStack(Items.SADDLE), new ItemStack(BarbecuetherFeature.PINDASCHEUTJES.get(), 4));
                    zichtbaar(p, "barbecuether/sausdieren_stal");
                    zeg(p, npc, T + "klaar");
                    hint(p, "quest.guhs.sausdieren.hint.klaar");
                }
            }
            default -> scherm(p, npc, T + "na", new Praat.Optie(JA, "gui.guhs.sausdieren.optie.rondje"), new Praat.Optie(UITLEG, "gui.guhs.sausdieren.optie.uitleg"),
                    new Praat.Optie(BLUBJE, "gui.guhs.sausdieren.optie.blubje"));
        }
    }

    @Override
    protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
        if (optie == JA && stap == 0) {
            if (verder(p, 0)) {
                geefEenmalig(p, "stok", new ItemStack(SausdierenFeature.PINDASAUS_STOK.get()));
                zeg(p, npc, T + "lok");
                hint(p, "quest.guhs.sausdieren.hint.lok");
            }
        } else if (optie == JA && stap >= 3) {
            // (the stick steers: without one the lap can't be ridden)
            if (GuhQuests.count(p, SausdierenFeature.PINDASAUS_STOK.get()) == 0) {
                geef(p, new ItemStack(SausdierenFeature.PINDASAUS_STOK.get()));
                zeg(p, npc, T + "stok_kwijt");
            }
            if (Proefrit.start(p, npc)) {
                zeg(p, npc, stap == 3 ? T + "start" : T + "start_opnieuw");
                hint(p, "quest.guhs.sausdieren.hint.proefrit");
            }
        } else if (optie == UITLEG && stap >= 5) {
            zeg(p, npc, T + "uitleg");
        } else if (optie == BLUBJE && stap >= 5) {
            blubje(npc, p);
        }
    }

    /**
     * After the questline: a small Sausblubje in a jar for whoever carries none, at most one a day. Wild Sausblubjes only
     * come with the natural spawner, and blubroom (the "Saus" tier of Guh-technologie, the Stuiterdrankje) and the
     * Blubkacheltje both need one: this is the source that is always there. Fed three knabbels it grows, hugged it splits
     * and leaves blubroom, so one blubje is a pen of them.
     */
    private void blubje(GuhNpcEntity npc, ServerPlayer p) {
        int dag = (int) (p.level().getGameTime() / 24000L) + 1;
        if (GuhQuests.count(p, SausdierenFeature.SAUSBLUBJE_POTJE.get()) > 0) {
            zeg(p, npc, T + "blubje_heb_je");
        } else if (LIJN.teller(p, "blubje_dag") == dag) {
            zeg(p, npc, T + "blubje_morgen");
        } else {
            LIJN.teller(p, "blubje_dag", dag);
            geef(p, new ItemStack(SausdierenFeature.SAUSBLUBJE_POTJE.get()));
            zeg(p, npc, T + "blubje");
        }
    }

    /**
     * This player fed a Sausloper of the stable. At step 2 it counts; with the last one the Sausloper trusts them and the
     * test lap is next.
     */
    public static void gevoerd(ServerPlayer p, SausloperEntity loper) {
        if (LIJN.stap(p) != 2) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.njam").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        int n = LIJN.teller(p, "voer") + 1;
        LIJN.teller(p, "voer", n);
        if (n >= SausdierenFeature.VOER_NODIG) {
            if (LIJN.verder(p, 2)) {
                GuhQuests.hint(p, "quest.guhs.sausdieren.hint.vertrouwt");
            }
        } else {
            GuhQuests.hint(p, "quest.guhs.sausdieren.hint.nog_voer");
        }
    }

    /** A Sausloper followed this player's stick to the Verzorger-guh (step 1). */
    public static void gelokt(ServerPlayer p, GuhNpcEntity npc) {
        if (LIJN.verder(p, 1)) {
            GuhQuests.say(p, npc, T + "gelokt");
            GuhQuests.hint(p, "quest.guhs.sausdieren.hint.gelokt");
        }
    }
}
