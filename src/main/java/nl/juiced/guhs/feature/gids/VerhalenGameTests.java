package nl.juiced.guhs.feature.gids;

import java.util.EnumSet;
import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.barbecuether.Grillguh;
import nl.juiced.guhs.feature.beroepen.BeroepenVoortgang;
import nl.juiced.guhs.feature.guheinde.GuheindeEvents;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.hemel.HemelQuest;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;
import nl.juiced.guhs.feature.timmerguh.TimmerguhVoortgang;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * The Guhdex tab "Verhalen": every questline has its texts; per questline the status and the step text for not started /
 * a step in the middle / done; what you need counts what you carry; progress is per player (never per structure); the
 * payload survives the trip over the network.
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class VerhalenGameTests {
    private static final String EMPTY = "empty";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static String key(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : "";
    }

    private static void kijk(GameTestHelper helper, ServerPlayer p, String id, VerhaalStand.Status status, String sleutel) {
        VerhaalStand v = VerhalenVoortgang.van(p, id);
        helper.assertTrue(v != null, "questline " + id);
        helper.assertTrue(v.status() == status, id + ": status " + v.status() + " instead of " + status);
        helper.assertTrue(key(v.nu()).equals("gui.guhs.verhalen." + id + ".nu." + sleutel), id + ": nu " + key(v.nu()) + " instead of " + sleutel);
        helper.assertTrue(key(v.waar()).equals("gui.guhs.verhalen." + id + ".waar." + sleutel), id + ": waar " + key(v.waar()));
        Language lang = Language.getInstance();
        helper.assertTrue(lang.has(key(v.nu())) && lang.has(key(v.waar())), id + ": the texts exist for " + sleutel);
    }

    /** Every questline, step, sleutel and heading has a text (the server's en_us: Dutch, like everything). */
    @GameTest(template = EMPTY)
    public static void verhalenTekstenBestaan(GameTestHelper helper) {
        Language lang = Language.getInstance();
        helper.assertTrue(lang.has("gui.guhs.guhdex.tab.verhalen"), "the tab name");
        for (VerhaalStand.Status s : VerhaalStand.Status.values()) {
            helper.assertTrue(lang.has("gui.guhs.verhalen.status." + s.id()), "status " + s);
        }
        for (String k : List.of("kop.nieuw", "kop.oud", "kop.nu", "kop.klaar", "kop.nodig", "kop.stappen", "kop.beloningen", "status.stap", "terug",
                "binnen", "nog_niet", "stappen_telling", "klik", "klik_rij", "laden")) {
            helper.assertTrue(lang.has("gui.guhs.verhalen." + k), "text " + k);
        }
        helper.assertTrue(VerhalenVoortgang.ids().size() == VerhalenVoortgang.STAPPEN.size(), "every questline has a step count");
        for (String id : VerhalenVoortgang.ids()) {
            helper.assertTrue(lang.has("gui.guhs.verhalen." + id + ".naam") && lang.has("gui.guhs.verhalen." + id + ".uitleg"), "name of " + id);
            for (int i = 0; i < VerhalenVoortgang.STAPPEN.get(id); i++) {
                helper.assertTrue(lang.has(VerhaalStand.stapKey(id, i)), id + " step " + i);
            }
            for (String s : VerhalenVoortgang.sleutels(id)) {
                helper.assertTrue(lang.has("gui.guhs.verhalen." + id + ".nu." + s) && lang.has("gui.guhs.verhalen." + id + ".waar." + s), id + " " + s);
            }
        }
        helper.succeed();
    }

    /** A fresh player: every questline "not started" (with the text of its first step), icons and items exist. */
    @GameTest(template = EMPTY)
    public static void verhalenNietBegonnen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        List<VerhaalStand> alle = VerhalenVoortgang.alle(p);
        helper.assertTrue(alle.stream().map(VerhaalStand::id).toList().equals(VerhalenVoortgang.ids()), "all questlines, in order");
        for (VerhaalStand v : alle) {
            kijk(helper, p, v.id(), VerhaalStand.Status.NIET_BEGONNEN, "0");
            helper.assertTrue(v.stap() == 0 && v.stappen() > 0, v.id() + ": step 0");
            helper.assertTrue(VerhalenVoortgang.item(v.icoon()) != Items.AIR, v.id() + ": icon " + v.icoon());
            for (VerhaalStand.Beloning b : v.beloningen()) {
                helper.assertTrue(!b.binnen() && VerhalenVoortgang.item(b.item()) != Items.AIR, v.id() + ": reward " + b.item());
            }
        }
        weg(helper, p);
        helper.succeed();
    }

    /** The 3.0 Guhverhalen: a step in the middle (with what you need, counted) and done. */
    @GameTest(template = EMPTY)
    public static void verhalenGuhverhalen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        // Timmerguh: planks and pink wool counted
        TimmerguhVoortgang.zet(p, TimmerguhVoortgang.MATERIAAL);
        p.getInventory().add(new ItemStack(Items.OAK_PLANKS, 5));
        p.getInventory().add(new ItemStack(Items.BIRCH_PLANKS, 2));
        kijk(helper, p, "timmerguh", VerhaalStand.Status.BEZIG, "1");
        VerhaalStand t = VerhalenVoortgang.van(p, "timmerguh");
        helper.assertTrue(t.stap() == 1 && t.nodig().size() == 2 && t.nodig().get(0).heb() == 7 && t.nodig().get(0).nodig() == 16, "7 of 16 planks");
        helper.assertTrue(t.nodig().get(1).heb() == 0 && t.nodig().get(1).nodig() == 8, "0 of 8 wool");
        TimmerguhVoortgang.zet(p, TimmerguhVoortgang.KLAAR);
        kijk(helper, p, "timmerguh", VerhaalStand.Status.BEZIG, "4");   // (the extra step)
        TimmerguhVoortgang.zet(p, TimmerguhVoortgang.KNUS);
        kijk(helper, p, "timmerguh", VerhaalStand.Status.KLAAR, "klaar");
        helper.assertTrue(VerhalenVoortgang.van(p, "timmerguh").beloningen().stream().allMatch(VerhaalStand.Beloning::binnen), "all rewards in");
        // Balto: bring the kist to Rosy, then the hero may take Baltoguh home
        BaltoVerhaal.zet(p, BaltoVerhaal.BIJ_ROSY);
        kijk(helper, p, "balto", VerhaalStand.Status.BEZIG, "2");
        BaltoVerhaal.zet(p, BaltoVerhaal.AANGEKOMEN);
        kijk(helper, p, "balto", VerhaalStand.Status.BEZIG, "6");
        helper.assertTrue(VerhalenVoortgang.van(p, "balto").nodig().get(0).heb() == 0, "no kist yet");
        BaltoVerhaal.zet(p, BaltoVerhaal.KLAAR);
        kijk(helper, p, "balto", VerhaalStand.Status.KLAAR, "klaar");   // (not "vrij" yet: never went through beloon)
        // Guhtwo: the first missing note's spot, then the parts
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.NOTITIES);
        MewtwoVoortgang.vondNotitie(p, 1);
        kijk(helper, p, "mewtwo", VerhaalStand.Status.BEZIG, "1");
        VerhaalStand m = VerhalenVoortgang.van(p, "mewtwo");
        helper.assertTrue(m.nodig().get(0).heb() == 1 && m.nodig().get(0).nodig() == 6, "1 of 6 notes");
        Object arg = ((TranslatableContents) m.nu().getContents()).getArgs()[0];
        helper.assertTrue(arg instanceof Component c && key(c).equals("gui.guhs.mewtwo.plek.notitie.2"), "look at spot 2: " + arg);
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.ONDERDELEN);
        for (int n = 1; n <= 4; n++) {
            MewtwoVoortgang.vondOnderdeel(p, n);
        }
        kijk(helper, p, "mewtwo", VerhaalStand.Status.BEZIG, "2_inbouwen");
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.KLAAR);
        kijk(helper, p, "mewtwo", VerhaalStand.Status.KLAAR, "klaar");
        // Hemel: collecting, then the heart beats
        HemelQuest.zetStap(p, 1);
        p.getInventory().add(new ItemStack(HemelQuest.Ding.VEERTJE.item()));
        kijk(helper, p, "hemel", VerhaalStand.Status.BEZIG, "1");
        VerhaalStand h = VerhalenVoortgang.van(p, "hemel");
        helper.assertTrue(h.nodig().size() == 3 && h.nodig().get(2).genoeg() && !h.nodig().get(0).genoeg(), "the feather is there, the crystal not");
        HemelQuest.wakker(p);
        kijk(helper, p, "hemel", VerhaalStand.Status.KLAAR, "klaar");
        // Guhwai'i: tidying up, the gifts, done
        Ohana.zet(p, Ohana.OPRUIMEN);
        kijk(helper, p, "guhwaii", VerhaalStand.Status.BEZIG, "3");
        helper.assertTrue(VerhalenVoortgang.van(p, "guhwaii").nodig().get(0).nodig() == Ohana.ROMMEL_NODIG, "5 rommeltjes");
        Ohana.zet(p, Ohana.LIEF);
        GuhQuests.saved(p).putInt(Ohana.GAVEN, Ohana.GAVE_KOKOS);
        VerhaalStand g = VerhalenVoortgang.van(p, "guhwaii");
        helper.assertTrue(g.nodig().size() == 3 && g.nodig().get(0).genoeg() && !g.nodig().get(1).genoeg() && g.nodig().get(2).nodig() == Ohana.KNABBELS,
                "coconut given, the rest not");
        Ohana.zet(p, Ohana.KLAAR);
        kijk(helper, p, "guhwaii", VerhaalStand.Status.KLAAR, "klaar");
        weg(helper, p);
        helper.succeed();
    }

    /** The older adventures: a step in the middle and done. */
    @GameTest(template = EMPTY)
    public static void verhalenEerdereAvonturen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        GuhWorldData.PlayerData d = GuhWorldData.get(p.server).player(p.getUUID());
        d.maagQuest = 3;
        kijk(helper, p, "vadsig", VerhaalStand.Status.BEZIG, "3");
        helper.assertTrue(VerhalenVoortgang.van(p, "vadsig").nodig().get(1).nodig() == GuhQuests.BALLOONS_WANTED, "3 balloons");
        d.maagQuest = 4;
        kijk(helper, p, "vadsig", VerhaalStand.Status.KLAAR, "klaar");
        d.sledQuest = 1;
        kijk(helper, p, "slee", VerhaalStand.Status.BEZIG, "1");
        d.sledQuest = 2;
        kijk(helper, p, "slee", VerhaalStand.Status.KLAAR, "klaar");
        // Knusfeest: a round with two tasks, one brought
        Knusfeest.nieuweRonde(p, 0, EnumSet.of(Feesttaak.FEESTTAART, Feesttaak.FEESTSLINGERS));
        Knusfeest.zet(p, Feesttaak.FEESTTAART, Knusfeest.Stap.GEBRACHT);
        kijk(helper, p, "knusfeest", VerhaalStand.Status.BEZIG, "1");
        VerhaalStand k = VerhalenVoortgang.van(p, "knusfeest");
        helper.assertTrue(k.nodig().size() == 2 && k.nodig().stream().filter(VerhaalStand.Nodig::genoeg).count() == 1, "one of two tasks brought");
        Object arg = ((TranslatableContents) k.nu().getContents()).getArgs()[0];
        helper.assertTrue(arg instanceof Component c && key(c).equals("gui.guhs.knusfeest.waar.feestslingers"), "next: the garlands: " + arg);
        Knusfeest.zet(p, Feesttaak.FEESTSLINGERS, Knusfeest.Stap.GEBRACHT);
        kijk(helper, p, "knusfeest", VerhaalStand.Status.BEZIG, "2");
        // Guheinde: a later step counts the earlier ones as done
        GuhQuests.saved(p).putInt(GuheindeEvents.KONING, 1);
        kijk(helper, p, "guheinde", VerhaalStand.Status.BEZIG, "1");
        GuhQuests.saved(p).putInt(GuheindeGevecht.WINS, 1);
        kijk(helper, p, "guheinde", VerhaalStand.Status.BEZIG, "7");
        GuhQuests.saved(p).putInt(GuheindeEvents.KONING, 2);
        kijk(helper, p, "guheinde", VerhaalStand.Status.KLAAR, "klaar");
        // Grillguh
        Grillguh.setStep(p, Grillguh.FRAME);
        kijk(helper, p, "grillguh", VerhaalStand.Status.BEZIG, "2");
        Grillguh.setStep(p, Grillguh.DONE);
        kijk(helper, p, "grillguh", VerhaalStand.Status.KLAAR, "klaar");
        // beroepen
        BeroepenVoortgang.zet(p, BeroepenVoortgang.Beroep.BOUW, 1);
        kijk(helper, p, "beroep_bouw", VerhaalStand.Status.BEZIG, "1");
        helper.assertTrue(VerhalenVoortgang.van(p, "beroep_bouw").nodig().size() == 2, "planks and knabbels");
        BeroepenVoortgang.rondAf(p, BeroepenVoortgang.Beroep.BOUW, null);
        kijk(helper, p, "beroep_bouw", VerhaalStand.Status.KLAAR, "klaar");
        kijk(helper, p, "beroep_politie", VerhaalStand.Status.NIET_BEGONNEN, "0");
        weg(helper, p);
        helper.succeed();
    }

    /** Progress is per player: one player's step never shows for another (and doesn't depend on which structure). */
    @GameTest(template = EMPTY)
    public static void verhalenPerSpeler(GameTestHelper helper) {
        ServerPlayer a = speler(helper), b = speler(helper);
        BaltoVerhaal.zet(a, BaltoVerhaal.TERUG);
        Ohana.zet(a, Ohana.CAPSULE);
        TimmerguhVoortgang.zet(a, TimmerguhVoortgang.DAK);
        kijk(helper, a, "balto", VerhaalStand.Status.BEZIG, "5");
        kijk(helper, b, "balto", VerhaalStand.Status.NIET_BEGONNEN, "0");
        kijk(helper, b, "guhwaii", VerhaalStand.Status.NIET_BEGONNEN, "0");
        kijk(helper, b, "timmerguh", VerhaalStand.Status.NIET_BEGONNEN, "0");
        weg(helper, a, b);
        helper.succeed();
    }

    /** The payload survives the network (statuses, counts, texts with arguments). */
    @GameTest(template = EMPTY)
    public static void verhalenPayload(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        MewtwoVoortgang.zetStap(p, MewtwoVoortgang.NOTITIES);
        MewtwoVoortgang.vondNotitie(p, 1);
        TimmerguhVoortgang.zet(p, TimmerguhVoortgang.MATERIAAL);
        List<VerhaalStand> voor = VerhalenVoortgang.alle(p);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        VerhalenPayloads.Data.STREAM_CODEC.encode(buf, new VerhalenPayloads.Data(voor));
        List<VerhaalStand> na = VerhalenPayloads.Data.STREAM_CODEC.decode(buf).verhalen();
        helper.assertTrue(na.size() == voor.size(), "all questlines arrive");
        for (int i = 0; i < voor.size(); i++) {
            VerhaalStand x = voor.get(i), y = na.get(i);
            helper.assertTrue(x.id().equals(y.id()) && x.status() == y.status() && x.stap() == y.stap() && x.stappen() == y.stappen()
                    && x.nu().equals(y.nu()) && x.waar().equals(y.waar()) && x.nodig().equals(y.nodig()) && x.beloningen().equals(y.beloningen()),
                    "the same after the trip: " + x.id());
        }
        weg(helper, p);
        helper.succeed();
    }
}
