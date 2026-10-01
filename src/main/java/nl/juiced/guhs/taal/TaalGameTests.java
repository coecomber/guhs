package nl.juiced.guhs.taal;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.quest.Reisguh;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.2.0: the NL/EN switch (the Auto mapping, FTB Quests' locale) and that the server keeps our texts as Components instead
 * of resolving them (the server resolves in en_us, a player may read Dutch). Run with -Pgt=taal (batch) or TaalGameTests.
 */
public final class TaalGameTests {
    private static final String EMPTY = "empty";
    static final String BATCH = "taal";

    private static String key(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : null;
    }

    private static Object[] args(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getArgs() : new Object[0];
    }

    /** Auto follows the Minecraft language (only nl_nl is Dutch), NL and EN are fixed; the button cycles Auto, NL, EN. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void taalAutoNlEn(GameTestHelper helper) {
        helper.assertTrue(Taal.AUTO.dutch("nl_nl") && Taal.AUTO.dutch("NL_NL"), "Auto + nl_nl: Dutch");
        helper.assertTrue(!Taal.AUTO.dutch("en_us") && !Taal.AUTO.dutch("de_de") && !Taal.AUTO.dutch("nl_be") && !Taal.AUTO.dutch(null),
                "Auto + anything else: English");
        helper.assertTrue(Taal.NL.dutch("en_us") && Taal.NL.dutch("de_de") && !Taal.EN.dutch("nl_nl"), "NL and EN don't look at Minecraft");
        helper.assertTrue(Taal.AUTO.code("nl_nl").equals("nl_nl") && Taal.AUTO.code("fr_fr").equals("en_us") && Taal.EN.code("nl_nl").equals("en_us"),
                "the lang file in use");
        helper.assertTrue(Taal.AUTO.next() == Taal.NL && Taal.NL.next() == Taal.EN && Taal.EN.next() == Taal.AUTO, "the button cycles");
        for (Taal t : Taal.values()) {
            helper.assertTrue(NlTekst.has(t.key()), "a name for " + t);
        }
        for (String k : new String[] {"gui.guhs.menu.taal", "gui.guhs.menu.taal.tooltip", "gui.guhs.taal.auto_is", "gui.guhs.taal.kort.nl",
                "gui.guhs.taal.kort.en", "guhs.configuration.language", "guhs.configuration.language.tooltip"}) {
            helper.assertTrue(NlTekst.has(k) && net.minecraft.locale.Language.getInstance().has(k), "the text " + k + " (nl_nl and en_us)");
        }
        // FTB Quests: Auto keeps its own choice, NL is nl_nl, EN only replaces a Dutch locale (en_us is its fallback anyway)
        helper.assertTrue(Taal.AUTO.ftbLocale("nl_nl").equals("nl_nl") && Taal.AUTO.ftbLocale("de_de").equals("de_de"), "FTB: Auto");
        helper.assertTrue(Taal.NL.ftbLocale("en_us").equals("nl_nl") && Taal.EN.ftbLocale("nl_nl").equals("en_us")
                && Taal.EN.ftbLocale("de_de").equals("de_de"), "FTB: NL and EN");
        helper.succeed();
    }

    /** Saved texts: a literal stays a plain string tag (so data from before 1.2.0, a String, loads as a literal). */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void taalTekstOpslaan(GameTestHelper helper) {
        CompoundTag t = new CompoundTag();
        Tekst.put(t, "A", Component.literal("Knabbeltje"));
        Tekst.put(t, "B", Component.translatable("entity.guhs.guh.mint"));
        Tekst.put(t, "C", Component.translatable("quest.guhs.reis.portal_name", 12, -40));
        t.putString("Oud", "Roze Guh");
        helper.assertTrue(t.get("A") instanceof StringTag && t.getStringOr("A", "").equals("Knabbeltje"), "a literal is a plain string");
        helper.assertTrue(Tekst.get(t, "A").equals(Component.literal("Knabbeltje")) && Tekst.literal(Tekst.get(t, "A")), "and reads back");
        helper.assertTrue("entity.guhs.guh.mint".equals(key(Tekst.get(t, "B"))), "a translatable stays a translatable");
        helper.assertTrue("quest.guhs.reis.portal_name".equals(key(Tekst.get(t, "C"))) && Arrays.equals(args(Tekst.get(t, "C")), new Object[] {12, -40}),
                "with its arguments: " + Arrays.toString(args(Tekst.get(t, "C"))));
        helper.assertTrue(Tekst.get(t, "Oud").equals(Component.literal("Roze Guh")), "an old String is a literal");
        helper.assertTrue(Tekst.empty(Tekst.get(t, "Nergens")) && Tekst.empty(Component.empty()) && !Tekst.empty(Component.translatable("x")),
                "nothing is empty");
        helper.assertTrue(NlTekst.tekst(Tekst.get(t, "C")).equals("Guhportaal (12, -40)"), "and a Dutch reader sees: " + NlTekst.tekst(Tekst.get(t, "C")));
        helper.succeed();
    }

    /** Reisguh names saved before 1.2.0 (Dutch Strings) become what they mean; a player's own name stays as it is. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void taalReisguhNamen(GameTestHelper helper) {
        helper.assertTrue("entity.guhs.reisguh.plek.guhkermis".equals(key(Reisguh.vanOud(Component.literal("Guhkermis")))), "a template place");
        helper.assertTrue("entity.guhs.reisguh.plek.ohana".equals(key(Reisguh.vanOud(Component.literal("Ohana op Guhwai'i")))), "with a quote");
        Component portaal = Reisguh.vanOud(Component.literal("Guhportaal (120, -48)"));
        helper.assertTrue("quest.guhs.reis.portal_name".equals(key(portaal)) && Arrays.equals(args(portaal), new Object[] {120, -48}), "a portal's");
        Component wild = Reisguh.vanOud(Component.literal("Reisguh (5, 6)"));
        helper.assertTrue("quest.guhs.reis.wild_name".equals(key(wild)), "a wild one's");
        Component eigen = Reisguh.vanOud(Component.literal("Reisguh van Juiced"));
        helper.assertTrue("quest.guhs.reis.own_name".equals(key(eigen)) && Arrays.equals(args(eigen), new Object[] {"Juiced"}), "a whistled one's");
        Component dubbel = Reisguh.vanOud(Component.literal("Knuffeldal (1, 2)"));
        helper.assertTrue("quest.guhs.reis.naam_xz".equals(key(dubbel)) && args(dubbel)[0] instanceof Component plek
                && "entity.guhs.reisguh.plek.knuffeldal".equals(key(plek)), "a taken template name");
        helper.assertTrue(NlTekst.tekst(dubbel).equals("Knuffeldal (1, 2)"), "reads the same in Dutch: " + NlTekst.tekst(dubbel));
        helper.assertTrue(Reisguh.vanOud(Component.literal("Mijn Station")).equals(Component.literal("Mijn Station")), "a player's own name");
        Component al = Component.translatable("entity.guhs.reisguh.plek.guhland");
        helper.assertTrue(Reisguh.vanOud(al) == al, "a translatable stays");
        for (String id : Reisguh.PLEKKEN.values()) {
            helper.assertTrue(NlTekst.has("entity.guhs.reisguh.plek." + id), "a name for " + id);
        }
        helper.succeed();
    }

    /** A huisje's default name is shown in the reader's language; a name the owner typed as it is. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void taalHuisjeNamen(GameTestHelper helper) {
        helper.assertTrue("gui.guhs.huisje.standaardnaam.1".equals(key(Huisje.tekst("Villa Vads"))), "a default name");
        Component genummerd = Huisje.tekst("Villa Vads 3");
        helper.assertTrue("gui.guhs.huisje.standaardnaam.1".equals(key(genummerd)) && genummerd.getSiblings().size() == 1
                && NlTekst.tekst(genummerd).equals("Villa Vads 3"), "a numbered one: " + NlTekst.tekst(genummerd));
        helper.assertTrue(Huisje.tekst("Villa Vadsig").equals(Component.literal("Villa Vadsig"))
                && Huisje.tekst("Villa Vads drie").getContents() instanceof PlainTextContents, "an own name");
        helper.succeed();
    }

    /** The server doesn't resolve a guh's name into a String any more: a picked-up guh and its memory star keep the Component. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void taalNamenBlijvenComponents(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        guh.setCustomName(Component.translatable("entity.guhs.guh_npc.reisguh"));
        ItemStack stack = PickedUpGuhItem.pickUp(guh);
        Component naam = stack.getHoverName();
        helper.assertTrue("item.guhs.picked_up_guh.named".equals(key(naam)) && args(naam)[0] instanceof Component c
                && "entity.guhs.guh_npc.reisguh".equals(key(c)), "the picked-up guh's item name: " + naam);
        GuhEntity naamloos = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 1, 4));
        ItemStack ster = nl.juiced.guhs.feature.hemel.Herinnering.maak(naamloos);
        Component sterNaam = Tekst.get(nl.juiced.guhs.feature.hemel.Herinnering.data(ster), "Naam");
        helper.assertTrue(key(sterNaam) != null && key(sterNaam).startsWith("entity.guhs.guh"), "an unnamed guh's star keeps its variant name: " + sterNaam);
        helper.assertTrue(nl.juiced.guhs.quest.KasteelPoort.zegtGeheimWoord("NJEG!") && nl.juiced.guhs.quest.KasteelPoort.zegtGeheimWoord("nyeg nyeg")
                && !nl.juiced.guhs.quest.KasteelPoort.zegtGeheimWoord("hallo"), "the secret word in both languages");
        ItemStack boek = nl.juiced.guhs.feature.bibliotheek.Guhboek.values()[0].stack();
        var inhoud = boek.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
        var lore = boek.get(net.minecraft.core.component.DataComponents.LORE);
        helper.assertTrue(inhoud != null && inhoud.author().isEmpty() && lore != null && "item.guhs.bieb_boek.door".equals(key(lore.lines().get(0))),
                "a library book: no (one-language) author, a translatable 'by' line");
        helper.succeed();
    }
}
