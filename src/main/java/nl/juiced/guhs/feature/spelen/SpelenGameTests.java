package nl.juiced.guhs.feature.spelen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingPayloads;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.gametest.GametestFilter;
import nl.juiced.guhs.menu.GuhWardrobeMenu;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.world.entity.EntitySpawnReason;
/** The shared 2.9 framework (phase 1, fundament): levels, the Minigames tab groups, clothing unlocks, OREN, scaffolding. */
public class SpelenGameTests {
    private static final String EMPTY = "empty";

    private static ServerPlayer player(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Makkelijk / medium / lastig: ids, boards (medium keeps the old board), coins (+50 % on lastig, rounded up), clamping. */
    @GuhTest(template = EMPTY)
    public static void spelenNiveaus(GameTestHelper helper) {
        helper.assertTrue(Niveau.MAKKELIJK.id().equals("makkelijk") && Niveau.MEDIUM.id().equals("medium") && Niveau.LASTIG.id().equals("lastig"), "ids");
        helper.assertTrue(Niveau.MEDIUM.board("golf_rondje").equals("golf_rondje") && Niveau.MAKKELIJK.board("golf_rondje").equals("golf_rondje_makkelijk")
                && Niveau.LASTIG.board("golf_rondje").equals("golf_rondje_lastig"), "boards");
        helper.assertTrue(Niveau.LASTIG.munten(4) == 6 && Niveau.LASTIG.munten(5) == 8 && Niveau.MEDIUM.munten(5) == 5 && Niveau.MAKKELIJK.munten(3) == 3,
                "coins");
        helper.assertTrue(Niveau.of(-3) == Niveau.MAKKELIJK && Niveau.of(1) == Niveau.MEDIUM && Niveau.of(9) == Niveau.LASTIG, "clamped");
        helper.assertTrue(Niveau.byId("lastig") == Niveau.LASTIG && Niveau.byId("??") == Niveau.MEDIUM, "by id");
        helper.assertTrue(Niveau.LASTIG.naam().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals("gui.guhs.niveau.lastig"), "lang key");
        helper.succeed();
    }

    /** Every group of the Minigames tab: known era, real structure, a guh character with a role, rows that exist in Highscores. */
    @GuhTest(template = EMPTY)
    public static void spelenGroepenKloppen(GameTestHelper helper) {
        List<SpelGroepen.Groep> alle = SpelGroepen.alle();
        helper.assertTrue(alle.size() == 22, "22 groups: " + alle.size());
        helper.assertTrue(SpelGroepen.van(SpelGroepen.Tijdperk.KLASSIEKERS).size() == 9 && SpelGroepen.van(SpelGroepen.Tijdperk.KNUFFELDAL).size() == 5
                && SpelGroepen.van(SpelGroepen.Tijdperk.GROTE_GUHSPELEN).size() == 6 && SpelGroepen.van(SpelGroepen.Tijdperk.VERHALEN).size() == 2,
                "per era (3.0: two story groups)");
        var structures = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Set<String> rows = new HashSet<>();
        for (SpelGroepen.Groep g : alle) {
            helper.assertTrue(SpelGroepen.van(g.id()) == g, "lookup " + g.id());
            helper.assertTrue(KledingBronnen.bronnen().contains(g.id()), "the group is a clothing source: " + g.id());
            helper.assertTrue(g.structuur() == null || structures.containsKey(Guhs.id(g.structuur())), "structure of " + g.id() + ": " + g.structuur());
            helper.assertTrue(g.tijdperk() != SpelGroepen.Tijdperk.GROTE_GUHSPELEN || g.npc() != null && Features.role(g.npc()) != null,
                    "a role for " + g.npc());
            helper.assertTrue(!g.icoon().get().isEmpty(), "an icon for " + g.id());
            for (String id : g.spellen()) {
                helper.assertTrue(Highscores.game(id) != null, g.id() + ": Highscores row " + id);
                helper.assertTrue(rows.add(id), "row in one group only: " + id);
            }
        }
        // every 2.9 row is in a group
        for (Highscores.Game game : Highscores.GAMES) {
            if (game.id().startsWith("circuit_") || game.id().startsWith("spelen_") || game.id().startsWith("doolhof_") || game.id().startsWith("katapult_")
                    || game.id().endsWith("_makkelijk") || game.id().endsWith("_lastig")) {
                helper.assertTrue(rows.contains(game.id()), "row " + game.id() + " is in a group");
            }
        }
        helper.assertTrue(SpelGroepen.van("elftocht").spellen().equals(List.of("elfguhjestocht")) && SpelGroepen.van("circuit").spellen().size() == 18,
                "elftocht and circuit rows");
        helper.succeed();
    }

    /** The 2.9 Highscores rows: times in ticks as m:ss.t (lower is better, "never" = -1), points as N pt; unique boards. */
    @GuhTest(template = EMPTY)
    public static void spelenHighscoresRijen(GameTestHelper helper) {
        helper.assertTrue(Highscores.tijd(1234).equals("1:01.7") && Highscores.tijd(0).equals("0:00.0") && Highscores.tijd(-1).equals("-")
                && Highscores.tijd(20 * 75 + 3).equals("1:15.1"), "tijd: " + Highscores.tijd(1234));
        for (String id : List.of("doolhof_makkelijk", "doolhof_medium", "doolhof_lastig", "spelen_zaklopen", "spelen_eierlopen", "spelen_spijkerpoepen",
                "elfguhjestocht", "circuit_vads_lastig", "circuit_kaasberg_makkelijk_ronde", "race_lap_lastig", "golf_makkelijk")) {
            Highscores.Game g = Highscores.game(id);
            helper.assertTrue(g != null && g.lowerIsBetter(), "time/strokes row " + id);
        }
        for (String id : List.of("sjoelen", "katapult_medium", "spelen_knabbelhappen", "spelen_blikgooien", "spelen_guhguhtje_prik", "spelen_zeskamp")) {
            Highscores.Game g = Highscores.game(id);
            helper.assertTrue(g != null && !g.lowerIsBetter() && g.board().equals(id) && g.format().apply(100).equals("100 pt"), "points row " + id);
        }
        helper.assertTrue(Highscores.game("elfguhjestocht").format().apply(20 * 300).equals("5:00.0"), "a tour time");
        helper.assertTrue(Highscores.game("beauty_lastig").board().equals("beauty_show_lastig") && Highscores.game("race_makkelijk").board().equals("race_total_makkelijk")
                && Highscores.game("disco_boogie").board().equals("disco_kleuren_boogie") && Highscores.game("vissen_zwaarste_lastig").board().equals("vissen_zwaarste_lastig"),
                "boards of the classics' levels");
        ServerPlayer p = player(helper);
        helper.assertTrue(Highscores.rows(p).stream().noneMatch(r -> r.played()), "a new player never played the new games either");
        leave(helper, p);
        helper.succeed();
    }

    /** Visited buildings: remembered once per player, survive in the saved data, go to the client with guhs:spelgroepen_data. */
    @GuhTest(template = EMPTY)
    public static void spelenBezocht(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        helper.assertTrue(!SpelGroepen.bezocht(p, "sjoelen") && SpelGroepen.kijk(p).isEmpty(), "nothing visited on an empty test floor");
        helper.assertTrue(SpelGroepen.bezoek(p, "sjoelen") && !SpelGroepen.bezoek(p, "sjoelen"), "remembered once");
        helper.assertTrue(SpelGroepen.bezocht(p, "sjoelen") && !SpelGroepen.bezocht(p, "doolhof") && SpelGroepen.bezocht(p).equals(List.of("sjoelen")), "visited");
        var data = new SpelenPayloads.SpelgroepenData(List.of("sjoelen", "circuit"));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SpelenPayloads.SpelgroepenData.STREAM_CODEC.encode(buf, data);
        helper.assertTrue(SpelenPayloads.SpelgroepenData.STREAM_CODEC.decode(buf).equals(data), "the payload survives");
        SpelGroepen.Client.set(data.bezocht());
        helper.assertTrue(SpelGroepen.Client.bezocht("circuit") && !SpelGroepen.Client.bezocht("doolhof"), "client cache");
        SpelGroepen.Client.set(List.of());
        leave(helper, p);
        helper.succeed();
    }

    /** Clothing unlocks: per player, once, saved as ids; the sources; the payload guhs:kleding_data. */
    @GuhTest(template = EMPTY)
    public static void spelenKledingUnlocks(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        helper.assertTrue(KledingUnlocks.alle(p).isEmpty() && !KledingUnlocks.heeft(p, GuhClothes.STRIPED_SWEATER), "nothing yet");
        helper.assertTrue(KledingUnlocks.ontgrendel(p, GuhClothes.STRIPED_SWEATER) && !KledingUnlocks.ontgrendel(p, GuhClothes.STRIPED_SWEATER), "new once");
        KledingUnlocks.ontgrendel(p, GuhClothes.RAIN_HAT);
        helper.assertTrue(KledingUnlocks.heeft(p, GuhClothes.RAIN_HAT) && KledingUnlocks.alle(p).size() == 2, "two unlocks");
        helper.assertTrue(nl.juiced.guhs.quest.GuhQuests.saved(p).getListOrEmpty(KledingUnlocks.KEY).getStringOr(0, "").equals("striped_sweater"),
                "saved as ids in the player's data");
        var data = new KledingPayloads.KledingData(List.of("striped_sweater", "rain_hat", "no_such_piece"));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        KledingPayloads.KledingData.STREAM_CODEC.encode(buf, data);
        helper.assertTrue(KledingPayloads.KledingData.STREAM_CODEC.decode(buf).equals(data), "the payload survives");
        KledingUnlocks.Client.set(data.unlocks());
        helper.assertTrue(KledingUnlocks.Client.heeft(GuhClothes.RAIN_HAT) && KledingUnlocks.Client.alle().size() == 2, "client cache (unknown ids skipped)");
        KledingUnlocks.Client.set(List.of());
        KledingUnlocks.wis(p);
        helper.assertTrue(KledingUnlocks.alle(p).isEmpty(), "forgotten");
        // sources: the known ones in their order, a new one appended, prices
        List<String> bronnen = KledingBronnen.bronnen();
        helper.assertTrue(bronnen.indexOf("beauty") == 0 && bronnen.contains("kleermaker") && bronnen.contains("beroep_bouw")
                && bronnen.indexOf("sjoelen") < bronnen.indexOf("kleermaker"), "the known sources: " + bronnen);
        leave(helper, p);
        helper.succeed();
    }

    /** OREN: the seventh slot, a wardrobe slot (six in the wardrobe), saved like the others; the wardrobe menu's indices. */
    @GuhTest(template = EMPTY)
    public static void spelenOrenSlot(GameTestHelper helper) {
        helper.assertTrue(GuhClothes.Slot.OREN.ordinal() == 6 && GuhClothes.Slot.values().length == 7, "OREN is the seventh slot");
        helper.assertTrue(GuhClothes.Slot.kleding().equals(List.of(GuhClothes.Slot.HEAD, GuhClothes.Slot.EYES, GuhClothes.Slot.BODY, GuhClothes.Slot.NECK,
                GuhClothes.Slot.BACK, GuhClothes.Slot.OREN)), "the wardrobe slots");
        helper.assertTrue(GuhWardrobeMenu.index(GuhClothes.Slot.OREN) == 5 && GuhWardrobeMenu.index(GuhClothes.Slot.HAAR) == -1
                && GuhWardrobeMenu.ARMOR_SLOT == 0 && GuhWardrobeMenu.PACK_START == 1 && GuhWardrobeMenu.INV_START == 19 && GuhWardrobeMenu.INV_END == 55,
                "menu indices (2.9: clothes are unlocks, not slots any more)");
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 2, 2));
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.OREN) == null, "nothing on the ears");
        guh.wear(GuhClothes.WINTER_SCARF);
        CompoundTag tag = new CompoundTag();
        nl.juiced.guhs.storage.Nbt.saveWithoutId(guh, tag);
        helper.assertTrue(tag.getStringOr("ClothesNeck", "").equals("winter_scarf") && !tag.contains("ClothesOren"), "saved per slot");
        tag.putString("ClothesOren", "winter_scarf");   // a piece of another slot is never worn on the ears
        GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        nl.juiced.guhs.storage.Nbt.load(copy, tag);
        helper.assertTrue(copy.getClothes(GuhClothes.Slot.NECK) == GuhClothes.WINTER_SCARF && copy.getClothes(GuhClothes.Slot.OREN) == null, "loaded");
        helper.succeed();
    }

    /** The scaffolding: the 2.9 characters (roles, Guhdex pages), PINGUH, the stub games, the gametest filter's matching. */
    @GuhTest(template = EMPTY)
    public static void spelenSteigers(GameTestHelper helper) {
        List<GuhNpcEntity.Kind> kinds = List.of(GuhNpcEntity.Kind.SJOELGUH, GuhNpcEntity.Kind.DOOLHOFGUH, GuhNpcEntity.Kind.KATAPULTGUH,
                GuhNpcEntity.Kind.SPELLEIDERGUH, GuhNpcEntity.Kind.SCHAATSMEESTERGUH, GuhNpcEntity.Kind.STEMPELGUH, GuhNpcEntity.Kind.CIRCUITGUH,
                GuhNpcEntity.Kind.BRANDWEERGUH, GuhNpcEntity.Kind.POLITIEGUH, GuhNpcEntity.Kind.APOTHEKERGUH, GuhNpcEntity.Kind.BOUWVAKKERGUH);
        for (int i = 0; i < kinds.size(); i++) {
            GuhNpcEntity.Kind kind = kinds.get(i);
            helper.assertTrue(kind.ordinal() == GuhNpcEntity.Kind.COCOTJE.ordinal() + 1 + i, "kind order " + kind);
            helper.assertTrue(Features.role(kind) != null, kind + " has a role");
            GuhVariant page = GuhVariant.ofCharacter(kind);
            helper.assertTrue(page != null && page.ordinal() == GuhVariant.COCOTJE.ordinal() + 1 + i && GuhDex.ENTRIES.contains(page), "page " + kind);
        }
        helper.assertTrue(!GuhVariant.PINGUH.isCharacter() && GuhVariant.PINGUH.ordinal() == GuhVariant.PLUISGUH.ordinal() + 1
                && GuhDex.ENTRIES.indexOf(GuhVariant.PINGUH) == GuhDex.ENTRIES.indexOf(GuhVariant.PLUISGUH) + 1 && GuhDex.TAMEABLE.contains(GuhVariant.PINGUH),
                "the Pinguh after the Pluisguh");
        helper.assertTrue(GuhVariant.PINGUH.shows("pinguh_snavel") && !GuhVariant.NORMAL.shows("pinguh_snavel"), "its own bones");
        ServerPlayer p = player(helper);
        for (String game : List.of(Minigames.SJOELEN, Minigames.DOOLHOF, Minigames.KATAPULT, Minigames.KNABBELSPELEN, Minigames.ELFTOCHT,
                Minigames.CIRCUIT, Minigames.BEROEPEN)) {
            helper.assertTrue(!Minigames.busyElsewhere(p, game), "nobody plays " + game + " yet");
        }
        leave(helper, p);
        List<String> entries = List.of("highscoresgametests", "knus");
        helper.assertTrue(GametestFilter.matches(entries, "HighscoresGameTests", "defaultBatch") && GametestFilter.matches(entries, "OtherGameTests", "knus_oven")
                && !GametestFilter.matches(entries, "GuhGameTests", "defaultBatch"), "the filter matches class or batch prefixes");
        helper.succeed();
    }
}
