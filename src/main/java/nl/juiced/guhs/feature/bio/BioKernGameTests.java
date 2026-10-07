package nl.juiced.guhs.feature.bio;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Game tests of the biomes3 kern (batch bio_kern; run with {@code -Pgt=bio_kern}): the sections and biomes of the
 * Superkompas tab, "ever been there" for dimensions and biomes, things of another slice by id, and the self test's
 * report on a server without the Guhmensie. The game test server has no datapack dimensions: what needs the real
 * dimension is a {@link BioZelftest} check.
 */
public class BioKernGameTests {
    private static final String BATCH = "bio_kern", EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    /** The sections in the design's order, the teaser empty, every listed biome a real biome, the three new ones in. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKernBiomeLijst(GameTestHelper helper) {
        List<String> ids = BiomeLijst.secties().stream().map(BiomeLijst.Sectie::id).toList();
        helper.assertTrue(ids.size() >= 4 && ids.subList(0, 4).equals(List.of("guhmension", "barbecuether", "guheinde", "echte_guheinde")),
                "the sections in order: " + ids);
        BiomeLijst.Sectie teaser = BiomeLijst.sectie("echte_guheinde");
        helper.assertTrue(teaser != null && teaser.teaser() && BiomeLijst.biomes("echte_guheinde").isEmpty(), "the teaser lists nothing");
        try {
            BiomeLijst.biome("echte_guheinde", "geheim");
            helper.fail("the teaser took a biome");
        } catch (IllegalArgumentException verwacht) {
            // as meant
        }
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            List<String> lijst = BiomeLijst.biomes(s.id());
            helper.assertTrue(s.teaser() == lijst.isEmpty(), "biomes in " + s.id() + ": " + lijst);
            helper.assertTrue(lijst.stream().distinct().count() == lijst.size(), "each biome once in " + s.id());
            for (String b : lijst) {
                helper.assertTrue(biomes.containsKey(BiomeLijst.sleutel(b)), "biome exists: " + b);
            }
        }
        // nothing is forgotten: every guhs biome in a listed dimension's biome source is in its section
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            if (s.teaser()) {
                continue;
            }
            Identifier bestand = Identifier.fromNamespaceAndPath(s.dimensie().identifier().getNamespace(), "dimension/" + s.dimensie().identifier().getPath() + ".json");
            var bron = helper.getLevel().getServer().getResourceManager().getResource(bestand);
            helper.assertTrue(bron.isPresent(), "the dimension file " + bestand);
            String tekst;
            try (var in = bron.get().openAsReader()) {
                tekst = in.lines().collect(java.util.stream.Collectors.joining(" "));
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            var json = com.google.gson.JsonParser.parseString(tekst).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
            List<String> inBron = new java.util.ArrayList<>();
            if (json.has("biome")) {
                inBron.add(json.get("biome").getAsString());
            }
            if (json.has("biomes")) {
                json.getAsJsonArray("biomes").forEach(b -> inBron.add(b.getAsJsonObject().get("biome").getAsString()));
            }
            helper.assertTrue(!inBron.isEmpty(), "biomes in the biome source of " + s.id());
            for (String b : inBron) {
                helper.assertTrue(b.startsWith("guhs:") && BiomeLijst.heeft(s.id(), b.substring(5)), "section " + s.id() + " lists " + b);
            }
        }
        List<String> guhmension = BiomeLijst.biomes("guhmension");
        helper.assertTrue(guhmension.containsAll(List.of("guh_fields", "bleekwoud", "bloesemmeertje", "klaterdal", "wolkenweide")),
                "the Guhmensie's biomes: " + guhmension);
        helper.assertTrue(BiomeLijst.van(ModDimensions.GUHMENSION) == BiomeLijst.sectie("guhmension")
                && BiomeLijst.van(net.minecraft.world.level.Level.OVERWORLD) == null, "the section of a dimension");
        int voor = BiomeLijst.biomes("guheinde").size();
        BiomeLijst.biome("guheinde", "guheinde");
        helper.assertTrue(BiomeLijst.biomes("guheinde").size() == voor && BiomeLijst.heeft("guheinde", "guheinde"), "a duplicate adds nothing");
        for (var key : List.of(Bio.BLOESEMMEERTJE, Bio.KLATERDAL, Bio.WOLKENWEIDE)) {
            helper.assertTrue(biomes.containsKey(key), "the biome is registered: " + key.identifier());
        }
        helper.succeed();
    }

    /** Dimensions and biomes are remembered once per player, in the visited list, apart from the groups and the places. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKernBezocht(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Bezocht.bijwerken(p);
        helper.assertTrue(SpelGroepen.bezocht(p).isEmpty() && Bezocht.kijk(p) == null, "the test world is no listed dimension: " + SpelGroepen.bezocht(p));
        helper.assertTrue(!Bezocht.dimensie(p, "guhmension") && !Bezocht.dimensie(p, ModDimensions.GUHMENSION) && !Bezocht.biome(p, "klaterdal"),
                "a new player has been nowhere");
        BiomeLijst.Sectie guhmension = BiomeLijst.sectie("guhmension");
        helper.assertTrue(Bezocht.zie(p, guhmension, Identifier.withDefaultNamespace("plains")) == null
                && Bezocht.zie(p, guhmension, Guhs.id("asdal")) == null && SpelGroepen.bezocht(p).isEmpty(), "a biome the section does not list");
        helper.assertTrue("klaterdal".equals(Bezocht.zie(p, guhmension, Guhs.id("klaterdal"))) && Bezocht.zie(p, guhmension, Guhs.id("klaterdal")) == null,
                "a biome is new once");
        helper.assertTrue(Bezocht.biome(p, "klaterdal") && Bezocht.dimensie(p, "guhmension") && Bezocht.dimensie(p, ModDimensions.GUHMENSION)
                && !Bezocht.dimensie(p, "barbecuether") && !Bezocht.biome(p, "wolkenweide"), "standing in a biome also proves its dimension");
        helper.assertTrue(SpelGroepen.bezocht(p).equals(List.of("d:guhmension", "b:klaterdal")) && !SpelGroepen.bezocht(p, "klaterdal")
                && !SpelGroepen.structuurBezocht(p, "klaterdal"), "kept apart from groups and places: " + SpelGroepen.bezocht(p));
        helper.assertTrue(Bezocht.zetDimensie(p, "barbecuether") && !Bezocht.zetDimensie(p, "barbecuether") && Bezocht.dimensie(p, "barbecuether"),
                "a dimension is new once");
        helper.assertTrue(!Bezocht.zetDimensie(p, "echte_guheinde") && !Bezocht.dimensie(p, "echte_guheinde") && !Bezocht.zetDimensie(p, "nergens"),
                "the teaser never opens");
        helper.assertTrue(Bezocht.zie(p, BiomeLijst.sectie("echte_guheinde"), Guhs.id("guheinde")) == null, "nothing is seen in a teaser");
        // the client hears it through the list of 1.3.1
        SpelGroepen.Client.set(SpelGroepen.bezocht(p));
        helper.assertTrue(SpelGroepen.Client.bezocht(Bezocht.DIMENSIE + "guhmension") && SpelGroepen.Client.bezocht(Bezocht.BIOME + "klaterdal")
                && !SpelGroepen.Client.bezocht(Bezocht.DIMENSIE + "guheinde"), "the client cache");
        SpelGroepen.Client.set(List.of());
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /** Another slice's block or item by id: the stand-in while it does not exist, the real thing when it does. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKernOpId(GameTestHelper helper) {
        helper.assertTrue(Bio.blok("bio_bestaat_niet", Blocks.WHITE_WOOL) == Blocks.WHITE_WOOL && !Bio.heeftBlok("bio_bestaat_niet"), "the stand-in block");
        helper.assertTrue(Bio.blok("wolkenlift", Blocks.WHITE_WOOL) != Blocks.WHITE_WOOL && Bio.heeftBlok("wolkenlift"), "a block that exists");
        helper.assertTrue(Bio.item("bio_bestaat_niet", Items.STICK) == Items.STICK && Bio.item("kaas_knabbels", Items.STICK) != Items.STICK, "items");
        helper.assertTrue(!Bio.inNieuw(helper.getLevel(), helper.absolutePos(net.minecraft.core.BlockPos.ZERO)), "the test world is none of the three biomes");
        helper.succeed();
    }

    /**
     * After the merge of wave A: what the slices asked of each other by id is the real thing now, no stand-in. (The ids
     * of wereld and wave B are not listed here: nothing asks for them by id yet.)
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKernGolfA(GameTestHelper helper) {
        for (String id : List.of("wolkenpluis", "drijvende_bloesemblaadjes", "toro", "esdoorn_zaailing", "wolkenblok_wit")) {
            helper.assertTrue(Bio.item(id, Items.STICK) != Items.STICK, "the item guhs:" + id + " exists");
        }
        helper.assertTrue(Bio.heeftBlok("drijvende_bloesemblaadjes") && Bio.blok("drijvende_bloesemblaadjes", Blocks.LILY_PAD) != Blocks.LILY_PAD,
                "the kikkerguh's leaf is the real petal block");
        var schaapje = helper.spawn(nl.juiced.guhs.feature.bio.dieren.DierenSlice.WOLKENSCHAAPJE.get(), new net.minecraft.core.BlockPos(1, 2, 1));
        var pluis = schaapje.scheer(null, helper.getLevel());
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(pluis.getItem()).equals(Guhs.id("wolkenpluis")),
                "a shorn wolkenschaapje gives the real wolkenpluis: " + pluis);
        schaapje.discard();
        var iconen = java.util.Map.of("bloesemmeertje", "drijvende_bloesemblaadjes", "klaterdal", "toro", "wolkenweide", "wolkenblok_wit");
        iconen.forEach((biome, item) -> helper.assertTrue(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(nl.juiced.guhs.feature.bio.kompas.BiomeIconen.icoon(biome).getItem()).equals(Guhs.id(item)),
                "the Superkompas icon of " + biome + " is its first choice, guhs:" + item));
        helper.succeed();
    }

    /** A slice adds its structure to a Superkompas tab with voegToe: a place that is there adds nothing, an unknown tab is refused. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKernSuperkompas(GameTestHelper helper) {
        var knus = nl.juiced.guhs.item.SuperkompasItem.CATEGORIES.stream().filter(c -> c.id().equals("knus")).findFirst().orElseThrow();
        List<String> voor = knus.structures();
        nl.juiced.guhs.item.SuperkompasItem.voegToe("knus", "knuffelbad");
        knus = nl.juiced.guhs.item.SuperkompasItem.CATEGORIES.stream().filter(c -> c.id().equals("knus")).findFirst().orElseThrow();
        helper.assertTrue(knus.structures().equals(voor) && voor.size() >= 7, "a place that is there adds nothing: " + knus.structures());
        try {
            nl.juiced.guhs.item.SuperkompasItem.voegToe("bio_bestaat_niet", "knuffelbad");
            helper.fail("an unknown tab was accepted");
        } catch (IllegalArgumentException verwacht) {
            // as meant
        }
        for (String tab : List.of("knus", "wonderen")) {
            helper.assertTrue(nl.juiced.guhs.item.SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals(tab)), "the tab " + tab + " exists");
        }
        helper.succeed();
    }

    /** The self test reports instead of throwing where the Guhmensie is missing (as on this server), and counts its lines. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKernZelftest(GameTestHelper helper) {
        List<String> regels = BioZelftest.draai(helper.getLevel().getServer(), "kern");
        String laatste = regels.get(regels.size() - 1);
        helper.assertTrue(laatste.startsWith("[bio-zelftest] klaar: "), "the summary line: " + laatste);
        if (helper.getLevel().getServer().getLevel(ModDimensions.GUHMENSION) == null) {
            helper.assertTrue(regels.size() == 2 && regels.get(0).contains("FOUT") && laatste.endsWith("0 OK, 1 FOUT"), "no Guhmensie here: " + regels);
        } else {
            helper.assertTrue(laatste.endsWith(" 0 FOUT"), "the kern checks pass: " + regels);
        }
        helper.succeed();
    }
}
