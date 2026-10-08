package nl.juiced.guhs.feature.bio.systemen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.compat.FtbQuestsChapter;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.bio.bouwdal.Cadeaus;
import nl.juiced.guhs.feature.bio.bouwmeer.Visserguh;
import nl.juiced.guhs.feature.bio.kompas.BiomeKompas;
import nl.juiced.guhs.feature.bio.wereld.BioModel;
import nl.juiced.guhs.feature.bio.wereld.Kaart;
import nl.juiced.guhs.feature.bio.wereld.WolkTerrein;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.reisbureau.Bestemming;
import nl.juiced.guhs.feature.guhpixel.reisbureau.KaartItem;
import nl.juiced.guhs.feature.guhpixel.reisbureau.Reizen;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature;
import nl.juiced.guhs.feature.kaasmoeras.KikkerguhEntity;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;
import org.slf4j.Logger;

/**
 * biomes3 slice "systemen": the FTB quests ask for things that exist and lock nothing, the Reisbureau has twenty
 * destinations with five per duration and still offers one per duration a day, the four titles come from their proofs and
 * only then, a complete Guhdex stays complete, and the proofs of {@link Bewijzen}.
 */
public class BioSystemenGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String BATCH = "bio_systemen", EMPTY = "empty";
    /** Every quest key of tools/features/bio_systemen.py and the three biome quests of tools/make_ftbquests.py. */
    private static final String[] QUESTS = {"biome_bloesemmeertje", "biome_klaterdal", "biome_wolkenweide", "bio_kompas_biomes", "bio_kompas_gevonden",
            "bio_botenhuisje", "bio_visser", "bio_koivoer", "bio_koi_voeren", "bio_koi_emmer", "bio_roeien", "bio_visser_klaar", "bio_picknickeilandje",
            "bio_mand", "bio_hanami", "bio_bloesemguh", "bio_kikker_blad", "bio_bloesemblaadjes",
            "bio_torii", "bio_tanukiguh", "bio_theehuisje", "bio_bouw_torii", "bio_bouw_dak", "bio_bouw_shoji", "bio_bouw_zen",
            "bio_weebhuisje", "bio_weeb_ontmoet", "bio_weeb_reis", "bio_weeb_cadeau", "bio_japan_eten", "bio_japan_outfits", "bio_japan_reeks",
            "bio_wolkenhoeder_hut", "bio_schaapje_scheren", "bio_hoeder_les", "bio_schaapje_thuis", "bio_wolkguh", "bio_luchtballon_haven", "bio_vaart",
            "bio_sterrenwacht_ruine", "bio_sterrenstof", "bio_bouw_wolkenbank",
            "bio_regenboogbrug", "bio_pot", "bio_bouw_regenboog", "bio_bliksemsmidse", "bio_smid_ruil", "bio_wolkenkasteeltje", "bio_sluipen", "bio_top"};
    /** What the nine findable structures, the proofs and the titles lean on: these tasks must be in the chapter. */
    private static final String[] TAKEN = {"structure: \"guhs:weebhuisje\"", "structure: \"guhs:botenhuisje\"", "structure: \"guhs:picknickeilandje\"",
            "structure: \"guhs:wolkenhoeder_hut\"", "structure: \"guhs:sterrenwacht_ruine\"", "structure: \"guhs:luchtballon_haven\"",
            "structure: \"guhs:regenboogbrug\"", "structure: \"guhs:wolkenkasteeltje\"", "structure: \"guhs:bliksemsmidse\"",
            "biome: \"guhs:bloesemmeertje\"", "biome: \"guhs:klaterdal\"", "biome: \"guhs:wolkenweide\"",
            "advancement: \"guhs:quest/japan_geluksguh\"", "advancement: \"guhs:quest/japan_maneki_knabbel\"", "advancement: \"guhs:quest/biosystemen_hoogste_eiland\"",
            "advancement: \"guhs:quest/biosystemen_kompas_biome\"", "advancement: \"guhs:quest/biosystemen_kompas_gevonden\"",
            "advancement: \"guhs:quest/biosystemen_kikker_op_blad\"", "advancement: \"guhs:quest/wolkenkasteeltje_langs_reus\"",
            "advancement: \"guhs:quest/botenhuisje_visser_klaar\"", "advancement: \"guhs:quest/weeb_eerste_reis\"", "advancement: \"guhs:quest/weeb_cadeau\""};

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        CompoundTag saved = GuhQuests.saved(p);
        for (String k : new String[] {Bewijzen.NIES_KEY, Bewijzen.TOP_KEY, Cadeaus.REEKS_KEY, Cadeaus.OUTFIT_KEY, Cadeaus.AANTAL_KEY, Visserguh.STAP, Visserguh.GEDAAN}) {
            saved.remove(k);
        }
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static boolean bewijs(ServerPlayer p, String naam) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static void vergeet(ServerPlayer p, String... namen) {
        for (String naam : namen) {
            var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
            if (holder != null) {
                List<String> gedaan = new ArrayList<>();
                p.getAdvancements().getOrStartProgress(holder).getCompletedCriteria().forEach(gedaan::add);
                for (String c : gedaan) {
                    p.getAdvancements().revoke(holder, c);
                }
            }
        }
    }

    /** The quest id tools/make_ftbquests.py gives a key: "475548" + the first ten hex digits of its md5. */
    private static String qid(String key) throws Exception {
        byte[] md5 = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder("475548");
        for (int i = 0; i < 5; i++) {
            sb.append(String.format(Locale.ROOT, "%02X", md5[i] & 0xFF));
        }
        return sb.toString();
    }

    private static com.google.gson.JsonObject nlTeksten;

    /** A Dutch text straight from nl_nl.json (the server's own Language is English), "" when it is missing. */
    private static String nl(String key) {
        if (nlTeksten == null) {
            try (InputStream in = Guhs.class.getResourceAsStream("/assets/guhs/lang/nl_nl.json")) {
                nlTeksten = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return nlTeksten.has(key) ? nlTeksten.get(key).getAsString() : "";
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = FtbQuestsChapter.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // =====================================================================================================================
    // FTB Quests
    // =====================================================================================================================

    /** Every quest of biomes3 is in the chapter De Guhmensie, and every guhs thing a quest of ANY chapter asks for exists. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioSystemenQuestsBestaan(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var structuren = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
        String guhmensie = resource("ftbquests/chapters/guhs_guhmensie.json5"), nl = resource("ftbquests/lang/nl_nl/guhs_guhmensie.json5");
        helper.assertTrue(guhmensie != null && nl != null, "the chapter De Guhmensie ships");
        for (String key : QUESTS) {
            String id = qid(key);
            helper.assertTrue(guhmensie.contains("id: \"" + id + "\"") && nl.contains("\"quest." + id + ".title\"") && nl.contains("\"quest." + id + ".quest_desc\""),
                    "quest " + key + " (" + id + ") is in De Guhmensie with a title and a text");
        }
        for (String taak : TAKEN) {
            helper.assertTrue(guhmensie.contains(taak), "De Guhmensie has a task " + taak);
        }
        helper.assertTrue(!Pattern.compile("(?m)^\\s*\"?dependencies\"?\\s*:").matcher(guhmensie).find() && !guhmensie.contains("\"linear\""),
                "no quest of De Guhmensie is locked");
        helper.assertTrue(FtbQuestsChapter.version(guhmensie) >= 33, "the chapter version went up: " + FtbQuestsChapter.version(guhmensie));
        int adv = 0, items = 0, plekken = 0;
        List<String> mist = new ArrayList<>();
        for (String name : FtbQuestsChapter.chapters()) {
            String chapter = resource("ftbquests/chapters/" + name + ".json5");
            Matcher m = Pattern.compile("advancement: \"(guhs:[a-z0-9_/]+)\"").matcher(chapter);
            while (m.find()) {
                adv++;
                if (server.getAdvancements().get(Identifier.parse(m.group(1))) == null) {
                    mist.add(name + ": advancement " + m.group(1));
                }
            }
            m = Pattern.compile("\\bid: \"(guhs:[a-z0-9_]+)\"").matcher(chapter);
            while (m.find()) {
                items++;
                if (!BuiltInRegistries.ITEM.containsKey(Identifier.parse(m.group(1)))) {
                    mist.add(name + ": item " + m.group(1));
                }
            }
            m = Pattern.compile("structure: \"(guhs:[a-z0-9_]+)\"").matcher(chapter);
            while (m.find()) {
                plekken++;
                if (structuren.get(ResourceKey.create(Registries.STRUCTURE, Identifier.parse(m.group(1)))).isEmpty()) {
                    mist.add(name + ": structure " + m.group(1));
                }
            }
            m = Pattern.compile("biome: \"(guhs:[a-z0-9_]+)\"").matcher(chapter);
            while (m.find()) {
                plekken++;
                if (biomes.get(ResourceKey.create(Registries.BIOME, Identifier.parse(m.group(1)))).isEmpty()) {
                    mist.add(name + ": biome " + m.group(1));
                }
            }
        }
        LOGGER.info("[bio-systemen] FTB: {} advancement tasks, {} guhs items, {} structures and biomes looked up; missing: {}", adv, items, plekken, mist);
        helper.assertTrue(adv > 100 && items > 200 && plekken > 40, "the chapters were really read: " + adv + " / " + items + " / " + plekken);
        // (two quest ICONS of the release name an item that does not exist: guhs:golfbaan_afslag and guhs:maagwand are blocks without an item.
        //  Older than biomes3 and only a missing picture: reported, not counted)
        mist.removeIf(m -> m.equals("guhs_minigames: item guhs:golfbaan_afslag") || m.equals("guhs_maag: item guhs:maagwand"));
        helper.assertTrue(mist.isEmpty(), "every advancement, item, structure and biome a quest asks for exists: " + mist);
        helper.succeed();
    }

    // =====================================================================================================================
    // the Reisbureau
    // =====================================================================================================================

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioSystemenReisbureauTwintig(GameTestHelper helper) {
        helper.assertTrue(Bestemming.ECHT.size() == 20 && Bestemming.values().length == 21, "twenty destinations and the proefreisje");
        for (int duur : Bestemming.DUREN) {
            helper.assertTrue(Bestemming.metDuur(duur).size() == 5, "five destinations of " + duur + " minutes: " + Bestemming.metDuur(duur));
        }
        helper.assertTrue(Bestemming.BLOESEMMEERTJE.minuten() == 60 && Bestemming.KLATERDAL.minuten() == 120 && Bestemming.WOLKENWEIDE.minuten() == 480
                && Bestemming.JAPAN.minuten() == 1440, "one new destination in each duration");
        helper.assertTrue(Bestemming.BLOESEMMEERTJE.kans() == 5 && Bestemming.KLATERDAL.kans() == 8 && Bestemming.WOLKENWEIDE.kans() == 15 && Bestemming.JAPAN.kans() == 30,
                "with the chance of its duration");
        helper.assertTrue(Bestemming.ECHT.get(0) == Bestemming.LINGSESDIJK && Bestemming.vanId("lingsesdijk") == Bestemming.LINGSESDIJK
                && Bestemming.vanId("japan") == Bestemming.JAPAN && Bestemming.vanId("om_de_hoek").isProef(), "the old ids still resolve, Lingsesdijk first");
        Language lang = Language.getInstance();
        Set<Object> souvenirs = new HashSet<>();
        for (Bestemming b : List.of(Bestemming.BLOESEMMEERTJE, Bestemming.KLATERDAL, Bestemming.WOLKENWEIDE, Bestemming.JAPAN)) {
            helper.assertTrue(b.souvenir() != null && b.zeldzaam() != null && souvenirs.add(b.souvenir()) && souvenirs.add(b.zeldzaam())
                    && b.kaart() instanceof KaartItem k && k.bestemming() == b, b + ": a souvenir, a rare one and its own ansichtkaart");
            for (String k : List.of("gui.guhs.reisbureau.bestemming." + b.id(), "gui.guhs.reisbureau.bestemming." + b.id() + ".plek",
                    "gui.guhs.reisbureau.bestemming." + b.id() + ".uitleg", "book.guhs.reisbureau.kaart." + b.id(), "item.guhs.reisbureau_kaart_" + b.id(),
                    "block.guhs.reisbureau_souvenir_" + b.id(), "block.guhs.reisbureau_souvenir_" + b.id() + ".lore", "block.guhs.reisbureau_zeldzaam_" + b.id(),
                    "block.guhs.reisbureau_zeldzaam_" + b.id() + ".lore")) {
                helper.assertTrue(lang.has(k), "text " + k);
            }
        }
        helper.assertTrue("Japan, met Evivads en Nielsvads".equals(nl("gui.guhs.reisbureau.bestemming.japan")), "the fourth one is spelled like this");
        // the daily offer: one per duration, never the same two days in a row, everything comes by, for every start day
        for (long seed : new long[] {0L, 20261007L, -77L}) {
            Bestemming[] gisteren = null;
            int[] keer = new int[Bestemming.values().length];
            for (long dag = 20000; dag < 20100; dag++) {
                List<Bestemming> a = Reizen.aanbod(seed, dag);
                helper.assertTrue(a.size() == 4, "four trips a day");
                for (int g = 0; g < 4; g++) {
                    helper.assertTrue(a.get(g).minuten() == Bestemming.DUREN[g] && !a.get(g).isProef(), "one trip per duration, shortest first: " + a);
                    helper.assertTrue(gisteren == null || gisteren[g] != a.get(g), "never the same destination two days in a row: " + a.get(g) + " on day " + dag);
                    keer[a.get(g).ordinal()]++;
                }
                gisteren = a.toArray(new Bestemming[0]);
                Set<Bestemming> gezien = EnumSet.noneOf(Bestemming.class);
                for (long d = dag + 2; d < dag + 11; d++) {
                    gezien.addAll(Reizen.aanbod(seed, d));
                }
                helper.assertTrue(gezien.containsAll(Bestemming.ECHT), "any nine days in a row offer all twenty, from day " + (dag + 2) + ": " + gezien);
            }
            for (Bestemming b : Bestemming.ECHT) {
                helper.assertTrue(keer[b.ordinal()] == 20, b + " is offered one day in five: " + keer[b.ordinal()] + " of 100, seed " + seed);
            }
        }
        // a negative day (a clock before 1970 never happens, but the maths must not break)
        helper.assertTrue(Reizen.aanbod(1L, -3).size() == 4, "the offer of a negative day");
        helper.succeed();
    }

    // =====================================================================================================================
    // titles
    // =====================================================================================================================

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioSystemenTitelsOpHunBewijs(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        vergeet(p, Bewijzen.HOOGSTE_EILAND, Bewijzen.REUS_DRIE);
        // (1.4.0, the merge with bbq2: its features add their titles with Titels.registreer AFTER these, so the four stand
        // together directly after the guhpixel ones and are no longer the last of the list)
        helper.assertTrue(BioTitels.ALLE.size() == 4 && java.util.Collections.indexOfSubList(Titels.ALLE, BioTitels.ALLE)
                == 8 + nl.juiced.guhs.feature.guhpixel.GuhpixelTitels.ALLE.size(), "four titles, together after the guhpixel ones");
        Language lang = Language.getInstance();
        for (Titels.Titel t : BioTitels.ALLE) {
            helper.assertTrue(!Titels.heeft(p, t) && !Titels.kies(p, t.id()), t.id() + " is locked for a new player");
            helper.assertTrue(lang.has(t.naamSleutel()) && lang.has("gui.guhs.titels.hint." + t.id()), t.id() + " has a name and a hint");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse(t.icoon())), t.id() + " has its icon " + t.icoon());
        }
        helper.assertTrue("Weeb".equals(nl("gui.guhs.titels.naam." + BioTitels.WEEB)) && "Gezondheid!".equals(nl("gui.guhs.titels.naam." + BioTitels.GEZONDHEID)),
                "the titles Weeb and Gezondheid!");
        // Weeb: all twelve pieces GIVEN, not eleven
        GuhQuests.saved(p).putInt(Cadeaus.REEKS_KEY, Cadeaus.REEKS_VOL & ~(1 << 7));
        GuhQuests.saved(p).putInt(Cadeaus.OUTFIT_KEY, Cadeaus.OUTFITS_VOL);
        helper.assertTrue(Titels.behaald(p).stream().noneMatch(BioTitels.ALLE::contains), "eleven pieces and all outfits: no title");
        GuhQuests.saved(p).putInt(Cadeaus.REEKS_KEY, Cadeaus.REEKS_VOL);
        helper.assertTrue(alleen(p, BioTitels.WEEB), "twelve pieces: Weeb, and only Weeb: " + Titels.ids(Titels.behaald(p)));
        GuhQuests.saved(p).remove(Cadeaus.REEKS_KEY);
        // Gezondheid!: the third sneeze
        helper.assertTrue(Bewijzen.nies(p) == 1 && Bewijzen.nies(p) == 2 && !bewijs(p, Bewijzen.REUS_DRIE) && Titels.behaald(p).stream().noneMatch(BioTitels.ALLE::contains),
                "blown out twice: no title, no proof");
        helper.assertTrue(Bewijzen.nies(p) == 3 && bewijs(p, Bewijzen.REUS_DRIE) && alleen(p, BioTitels.GEZONDHEID), "the third time: Gezondheid!");
        GuhQuests.saved(p).remove(Bewijzen.NIES_KEY);
        // Koifluisteraar: the visser's lessons done
        Visserguh.zetStap(p, Visserguh.KOI);
        helper.assertTrue(Titels.behaald(p).stream().noneMatch(BioTitels.ALLE::contains), "at the last lesson: not yet");
        Visserguh.zetStap(p, Visserguh.KLAAR);
        helper.assertTrue(alleen(p, BioTitels.KOIFLUISTERAAR), "the lessons done: Koifluisteraar");
        Visserguh.zetStap(p, Visserguh.NIET);
        // Hoofd in de wolken: the summit
        helper.assertTrue(!bewijs(p, Bewijzen.HOOGSTE_EILAND), "no summit yet");
        Bewijzen.top(p);
        helper.assertTrue(bewijs(p, Bewijzen.HOOGSTE_EILAND) && alleen(p, BioTitels.WOLKENTOP), "the summit: Hoofd in de wolken");
        helper.assertTrue(Titels.kies(p, BioTitels.WOLKENTOP) && Titels.actief(p) == Titels.van(BioTitels.WOLKENTOP), "an earned title can be chosen and shows");
        GuhQuests.saved(p).remove(Bewijzen.TOP_KEY);
        GuhQuests.saved(p).remove(Titels.KEUZE);
        vergeet(p, Bewijzen.HOOGSTE_EILAND, Bewijzen.REUS_DRIE);
        weg(helper, p);
        helper.succeed();
    }

    /** Exactly this one of the four biomes3 titles is earned. */
    private static boolean alleen(ServerPlayer p, String id) {
        List<String> heb = Titels.behaald(p).stream().filter(BioTitels.ALLE::contains).map(Titels.Titel::id).toList();
        return heb.equals(List.of(id));
    }

    // =====================================================================================================================
    // the Guhdex
    // =====================================================================================================================

    /** The four new pages are bonus pages: who had the Guhdex complete still has, and no milestone moved. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioSystemenGuhdexBlijftCompleet(GameTestHelper helper) {
        List<GuhVariant> nieuw = List.of(GuhVariant.BLOESEMGUH, GuhVariant.TANUKIGUH, GuhVariant.KOI, GuhVariant.WOLKENSCHAAPJE);
        Set<GuhVariant> oudeBonus = Set.of(GuhVariant.ROOKGUH, GuhVariant.KRAAKGUH, GuhVariant.KRAAK_MIKA);
        // what counted before biomes3: every page that is not new, minus the bonus pages there were
        List<GuhVariant> voorheen = GuhDex.ENTRIES.stream().filter(v -> !nieuw.contains(v) && !oudeBonus.contains(v)).toList();
        helper.assertTrue(GuhDex.ENTRIES.containsAll(nieuw) && GuhDex.EXTRA.containsAll(nieuw) && GuhDex.EXTRA.size() == oudeBonus.size() + nieuw.size(),
                "the four new pages exist and are bonus pages");
        helper.assertTrue(GuhDex.TELLEND.equals(voorheen), "the counting pages are exactly those of before: " + GuhDex.TELLEND.size() + " / " + voorheen.size());
        int tembaar = (int) voorheen.stream().filter(v -> !v.isCharacter()).count();
        helper.assertTrue(GuhDex.MILESTONES.size() == 4 && GuhDex.MILESTONES.get(0).seen() == 5 && GuhDex.MILESTONES.get(1).seen() == 8
                && GuhDex.MILESTONES.get(2).seen() == voorheen.size() && GuhDex.MILESTONES.get(3).seen() == voorheen.size()
                && GuhDex.MILESTONES.get(3).tamed() == tembaar, "the milestones ask what they asked: " + voorheen.size() + " seen, " + tembaar + " tamed");
        for (GuhVariant v : nieuw) {
            String id = v.id();
            helper.assertTrue(Language.getInstance().has("gui.guhs.guhdex.rarity." + id) && Language.getInstance().has("gui.guhs.guhdex.info." + id),
                    "the page of " + id + " has its texts");
        }
        helper.assertTrue(nl("gui.guhs.guhdex.rarity.wolk").contains("Wolkenweide") && !nl("gui.guhs.guhdex.rarity.wolk").contains("Uniek")
                && !nl("gui.guhs.guhdex.info.wolk").contains("Alleen op"), "the Wolkguh's Dutch page names the Wolkenweide");
        helper.assertTrue(nl("gui.guhs.guhdex.rarity.kikkerguh").contains("Bloesemmeertje") && nl("gui.guhs.guhdex.rarity.kikkerguh").contains("Klaterdal"),
                "the kikkerguh's Dutch page names the new biomes");
        // a player who had everything of before: complete, without any new page
        ServerPlayer p = speler(helper);
        GuhWorldData data = GuhWorldData.get(helper.getLevel().getServer());
        GuhWorldData.PlayerData pd = data.player(p.getUUID());
        pd.seen.clear();
        pd.seen.addAll(voorheen);
        helper.assertTrue(GuhDex.vol(pd.seen) && GuhDex.geteld(pd.seen) == voorheen.size() && Titels.heeft(p, Titels.van(Titels.GUHKENNER)),
                "the Guhdex of before is still complete, and still gives Guhkenner");
        List<String> ids = pd.seen.stream().map(GuhVariant::id).toList();
        helper.assertTrue(GuhDex.geteldIds(ids) == voorheen.size(), "the client counts the same");
        // seeing the new pages adds them, and changes no count
        vergeet(p, "seen_bloesemguh");
        GuhDex.zie(p, GuhVariant.BLOESEMGUH);
        pd.seen.addAll(nieuw);
        helper.assertTrue(pd.seen.contains(GuhVariant.BLOESEMGUH) && bewijs(p, "seen_bloesemguh"), "the Bloesemguh's page fills in, with its proof");
        helper.assertTrue(GuhDex.vol(pd.seen) && GuhDex.geteld(pd.seen) == voorheen.size(), "all four seen: the count did not move");
        // one old page short: not complete, however many bonus pages
        pd.seen.remove(GuhVariant.NORMAL);
        helper.assertTrue(!GuhDex.vol(pd.seen), "a missing old page is not made up for by bonus pages");
        pd.seen.clear();
        // the weebhuisje's section: hidden until the first present, then twelve cells
        helper.assertTrue(plaatjes(p, "japan_")[0] == 0, "no Japan cells before the first present");
        GuhQuests.saved(p).putInt(Cadeaus.AANTAL_KEY, 3);
        GuhQuests.saved(p).putInt(Cadeaus.REEKS_KEY, 0b101);
        int[] cellen = plaatjes(p, "japan_");
        helper.assertTrue(cellen[0] == 12 && cellen[1] == 2, "twelve cells, the two given ones lit: " + cellen[0] + " / " + cellen[1]);
        GuhQuests.saved(p).remove(Cadeaus.AANTAL_KEY);
        GuhQuests.saved(p).remove(Cadeaus.REEKS_KEY);
        vergeet(p, "seen_bloesemguh");
        weg(helper, p);
        helper.succeed();
    }

    /** {cells whose icon id holds this text, how many of them are earned} in the player's "Guhpixel & uitjes" tab. */
    private static int[] plaatjes(ServerPlayer p, String deel) {
        int n = 0, behaald = 0;
        for (Tag raw : GidsBlad.stand(p).getListOrEmpty("Rijen")) {
            CompoundTag r = (CompoundTag) raw;
            if (GidsBlad.PLAATJE.equals(r.getStringOr("T", "")) && r.getCompoundOrEmpty("Icoon").toString().contains("guhs:" + deel)) {
                n++;
                behaald += r.getBooleanOr("Klaar", false) ? 1 : 0;
            }
        }
        return new int[] {n, behaald};
    }

    // =====================================================================================================================
    // the proofs
    // =====================================================================================================================

    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioSystemenKompasEnKikker(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        vergeet(p, Bewijzen.KOMPAS_BIOME, Bewijzen.KOMPAS_GEVONDEN, Bewijzen.KIKKER_OP_BLAD);
        // the compass: a plain Superkompas proves nothing, one that looks for a biome does, and standing in that biome finds it
        ItemStack kompas = new ItemStack(ModItems.SUPERKOMPAS.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, kompas);
        Bewijzen.kijk(p);
        helper.assertTrue(!bewijs(p, Bewijzen.KOMPAS_BIOME), "a compass without a biome: no proof");
        BiomeKompas.kies(kompas, "klaterdal");
        Bewijzen.kijk(p);
        helper.assertTrue(bewijs(p, Bewijzen.KOMPAS_BIOME) && !bewijs(p, Bewijzen.KOMPAS_GEVONDEN), "a biome chosen in the tab: the first proof, not the second");
        Bewijzen.kompas(p, "klaterdal", "guhs:bloesemmeertje");
        Bewijzen.kompas(p, "klaterdal", "minecraft:klaterdal");
        helper.assertTrue(!bewijs(p, Bewijzen.KOMPAS_GEVONDEN), "another biome is not the chosen one");
        Bewijzen.kompas(p, "klaterdal", "guhs:klaterdal");
        helper.assertTrue(bewijs(p, Bewijzen.KOMPAS_GEVONDEN), "standing in the chosen biome with the compass: found");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // the frog: one on a leaf nearby counts, one on the ground does not
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.WATER);
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.LILY_PAD);
        KikkerguhEntity kikker = KaasmoerasFeature.KIKKERGUH.get().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        kikker.setNoAi(true);
        BlockPos grond = helper.absolutePos(new BlockPos(6, 1, 6)), blad = helper.absolutePos(new BlockPos(3, 2, 3));
        kikker.snapTo(grond.getX() + 0.5, grond.getY(), grond.getZ() + 0.5);
        helper.getLevel().addFreshEntity(kikker);
        helper.assertTrue(!Bewijzen.kikkerOpBlad(p), "a kikkerguh on the floor is not one on a leaf");
        kikker.snapTo(blad.getX() + 0.5, blad.getY() + 0.1, blad.getZ() + 0.5);
        helper.assertTrue(Bewijzen.kikkerOpBlad(p), "a kikkerguh on a lily pad " + Bewijzen.KIKKER + " blocks away or less counts");
        p.snapTo(blad.getX() + 0.5 + Bewijzen.KIKKER + 3, blad.getY(), blad.getZ() + 0.5);
        helper.assertTrue(!Bewijzen.kikkerOpBlad(p), "too far away to see it");
        kikker.discard();
        vergeet(p, Bewijzen.KOMPAS_BIOME, Bewijzen.KOMPAS_GEVONDEN, Bewijzen.KIKKER_OP_BLAD);
        weg(helper, p);
        helper.succeed();
    }

    /** The summit rule, asked of the real terrain model: only the top island of a real, high stack counts. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void bioSystemenHoogsteEiland(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        NoiseGeneratorSettings settings = access.lookupOrThrow(Registries.NOISE_SETTINGS).getValue(Guhs.id("guhmension"));
        int stapels = 0, toppen = 0, los = 0, laag = 0, weiden = 0, minHoogte = Integer.MAX_VALUE, maxHoogte = 0;
        for (long seed : new long[] {20261007L, 1L}) {
            BioModel m = BioModel.van(RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed));
            List<int[]> plekken = new ArrayList<>();
            for (int x = -12000; x <= 12000 && plekken.size() < 3; x += 96) {
                for (int z = -12000; z <= 12000 && plekken.size() < 3; z += 96) {
                    if (m.eWeide(x, z) >= WolkTerrein.BINNEN + 0.05) {
                        boolean nieuw = true;
                        for (int[] q : plekken) {
                            nieuw &= Math.abs(q[0] - x) + Math.abs(q[1] - z) > 600;
                        }
                        if (nieuw) {
                            plekken.add(new int[] {x, z});
                        }
                    }
                }
            }
            weiden += plekken.size();
            for (int[] plek : plekken) {
                for (WolkTerrein.Stapel s : WolkTerrein.stapels(m, plek[0] - 200, plek[1] - 200, plek[0] + 200, plek[1] + 200)) {
                    WolkTerrein.Eiland top = Bewijzen.top(m, s);
                    WolkTerrein.Eiland hoogste = s.hoogste();
                    int[] kolom = kolom(hoogste);
                    if (s.los) {
                        los++;
                        helper.assertTrue(top == null && (kolom == null || !opEiland(m, hoogste, kolom)), "a loose island is no summit, at " + s.x + " " + s.z);
                        continue;
                    }
                    stapels++;
                    if (top == null) {
                        laag++;
                        helper.assertTrue(kolom == null || !opEiland(m, hoogste, kolom), "a stack that is too small or too low is no summit, at " + s.x + " " + s.z);
                        continue;
                    }
                    toppen++;
                    int hoogte = top.top - WolkTerrein.grond(m, s.x, s.z);
                    minHoogte = Math.min(minHoogte, hoogte);
                    maxHoogte = Math.max(maxHoogte, hoogte);
                    helper.assertTrue(top == hoogste && kolom != null && opEiland(m, top, kolom), "standing on the top island of the stack at " + s.x + " " + s.z + " counts");
                    int y = top.boven(kolom[0], kolom[1]);
                    helper.assertTrue(!Bewijzen.opTop(m, kolom[0] + 0.5, y - 12, kolom[1] + 0.5) && !Bewijzen.opTop(m, kolom[0] + 0.5, y + 9, kolom[1] + 0.5),
                            "hanging under it or flying over it does not");
                    helper.assertTrue(!Bewijzen.opTop(m, s.x + 0.5, WolkTerrein.grond(m, s.x, s.z) + 1, s.z + 0.5), "the meadow under the stack does not");
                    // every other island of the stack: no summit (where it does not lie right under the top island's own columns)
                    for (WolkTerrein.Eiland e : s.eilanden) {
                        int[] k = e == top ? null : kolom(e);
                        if (k != null && top.boven(k[0], k[1]) == Kaart.GEEN) {
                            helper.assertTrue(!opEiland(m, e, k), "a lower island of the stack (top " + e.top + " under " + top.top + ") is no summit");
                        }
                    }
                }
            }
        }
        LOGGER.info("[bio-systemen] summits: {} meadows, {} stacks, {} count as a summit ({}..{} above the meadow), {} too small or low, {} loose islands",
                weiden, stapels, toppen, toppen == 0 ? 0 : minHoogte, maxHoogte, laag, los);
        helper.assertTrue(weiden >= 2 && toppen >= 3, "the model has summits to climb: " + toppen + " of " + stapels + " stacks in " + weiden + " meadows");
        helper.assertTrue(minHoogte >= Bewijzen.MIN_HOOGTE, "every summit is high: " + minHoogte);
        helper.assertTrue(toppen * 2 >= stapels, "at least half of the stacks have a summit that counts: " + toppen + " of " + stapels);
        helper.succeed();
    }

    /** A column of this island (world x, z), or null when it has none. */
    private static int[] kolom(WolkTerrein.Eiland e) {
        for (int r = 0; r <= e.r; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r && e.boven(e.x + dx, e.z + dz) != Kaart.GEEN) {
                        return new int[] {e.x + dx, e.z + dz};
                    }
                }
            }
        }
        return null;
    }

    /** The summit rule for a player standing on this column of this island. */
    private static boolean opEiland(BioModel m, WolkTerrein.Eiland e, int[] kolom) {
        return Bewijzen.opTop(m, kolom[0] + 0.5, e.boven(kolom[0], kolom[1]) + 1, kolom[1] + 0.5);
    }
}
