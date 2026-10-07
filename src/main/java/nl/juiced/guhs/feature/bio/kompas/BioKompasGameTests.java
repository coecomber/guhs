package nl.juiced.guhs.feature.bio.kompas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.MapCodec;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bezocht;
import nl.juiced.guhs.feature.bio.BiomeLijst;
import nl.juiced.guhs.feature.reisguh.ReisguhGameTests;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Game tests of the biomes3 slice kompas (batch bio_kompas; run with {@code -Pgt=bio_kompas}): the Superkompas tab
 * Biomes. What the tab shows ({@link BiomesMenu}: locks, the teaser, every biome once in its own section, ticks), the
 * search ({@link BiomeZoeker}) on biome sources made for the test, and the compass itself ({@link BiomeKompas}). The
 * game test server has no Guhmensie: the search in the real dimensions is the self test's ({@code /guhs bio zelftest kompas}).
 */
public class BioKompasGameTests {
    private static final String BATCH = "bio_kompas", EMPTY = "empty";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        BiomeKompas.stopAlles(p);
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static List<BiomesMenu.SectieRij> secties(List<BiomesMenu.Rij> rijen) {
        return rijen.stream().filter(r -> r instanceof BiomesMenu.SectieRij).map(r -> (BiomesMenu.SectieRij) r).toList();
    }

    private static List<BiomesMenu.BiomeRij> biomes(List<BiomesMenu.Rij> rijen) {
        return rijen.stream().filter(r -> r instanceof BiomesMenu.BiomeRij).map(r -> (BiomesMenu.BiomeRij) r).toList();
    }

    /** A section is locked until the player has been in its dimension; a locked section lists nothing and takes no choice. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKompasSlot(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        List<BiomesMenu.Rij> rijen = BiomesMenu.rijen(p, s -> true);
        helper.assertTrue(rijen.size() == 4 && biomes(rijen).isEmpty() && secties(rijen).stream().allMatch(BiomesMenu.SectieRij::opSlot),
                "a new player: four padlocks and nothing else: " + rijen);
        helper.assertTrue(secties(rijen).stream().map(BiomesMenu.SectieRij::id).toList()
                .equals(List.of("guhmension", "barbecuether", "guheinde", "echte_guheinde")), "the sections in the design's order");
        helper.assertTrue(!BiomesMenu.magKiezen(p, "guh_fields") && !BiomesMenu.magKiezen(p, "asdal") && !BiomesMenu.magKiezen(p, "guheinde"),
                "nothing can be chosen behind a padlock");
        helper.assertTrue(Bezocht.zetDimensie(p, "guhmension"), "the Guhmensie is new");
        rijen = BiomesMenu.rijen(p, s -> true);
        List<BiomesMenu.SectieRij> s = secties(rijen);
        helper.assertTrue(!s.get(0).opSlot() && s.get(0).open() && s.get(0).totaal() == BiomeLijst.biomes("guhmension").size() && s.get(0).bezocht() == 0
                && s.get(1).opSlot() && s.get(2).opSlot() && s.get(3).opSlot(), "only the Guhmensie opened: " + s);
        helper.assertTrue(biomes(rijen).size() == BiomeLijst.biomes("guhmension").size()
                && biomes(rijen).stream().allMatch(b -> b.sectie().equals("guhmension") && !b.bezocht()), "its biomes are listed, none visited");
        helper.assertTrue(BiomesMenu.magKiezen(p, "guh_fields") && BiomesMenu.magKiezen(p, "klaterdal") && !BiomesMenu.magKiezen(p, "asdal")
                && !BiomesMenu.magKiezen(p, "bestaat_niet") && !BiomesMenu.magKiezen(p, "guhmaag"), "only its biomes can be chosen");
        // folded shut: the heading stays, the biomes go
        rijen = BiomesMenu.rijen(p, x -> false);
        helper.assertTrue(rijen.size() == 4 && !secties(rijen).get(0).opSlot() && !secties(rijen).get(0).open(), "folded shut: " + rijen);
        // the dev command's "forget": locked again
        helper.assertTrue(KompasSlice.wis(p) == 1 && BiomesMenu.opSlot(p, BiomeLijst.sectie("guhmension")), "forgotten: locked again");
        weg(helper, p);
        helper.succeed();
    }

    /** The teaser never opens: not by the kern's calls, not with a forged entry in the visited list, and it lists nothing. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKompasTeaser(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BiomeLijst.Sectie teaser = BiomeLijst.sectie("echte_guheinde");
        helper.assertTrue(teaser != null && teaser.teaser() && teaser.dimensie() == null && teaser.bewijs() == null, "the teaser has no dimension and no proof");
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            Bezocht.zetDimensie(p, s.id());
            for (String b : BiomeLijst.biomes(s.id())) {
                Bezocht.zetBiome(p, b);
            }
        }
        helper.assertTrue(!Bezocht.zetDimensie(p, "echte_guheinde") && !Bezocht.dimensie(p, "echte_guheinde"), "the kern refuses it");
        // a forged list (an edited save, another mod's command): still locked, still empty
        SpelGroepen.bezoek(p, Bezocht.DIMENSIE + "echte_guheinde");
        SpelGroepen.bezoek(p, Bezocht.BIOME + "echte_guheinde");
        helper.assertTrue(Bezocht.dimensie(p, "echte_guheinde") && BiomesMenu.opSlot(p, teaser), "locked whatever the visited list says");
        List<BiomesMenu.Rij> rijen = BiomesMenu.rijen(p, s -> true);
        List<BiomesMenu.SectieRij> s = secties(rijen);
        helper.assertTrue(s.size() == 4 && !s.get(0).opSlot() && !s.get(1).opSlot() && !s.get(2).opSlot() && s.get(3).opSlot()
                && s.get(3).id().equals("echte_guheinde") && !s.get(3).open() && s.get(3).totaal() == 0, "three open sections and the teaser: " + s);
        helper.assertTrue(rijen.get(rijen.size() - 1) == s.get(3) && biomes(rijen).stream().noneMatch(b -> b.sectie().equals("echte_guheinde")),
                "nothing is listed under the teaser");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SUPERKOMPAS.get()));
        for (String id : List.of("echte_guheinde", "", "guhmaag")) {
            helper.assertTrue(!BiomesMenu.magKiezen(p, id) && !BiomeKompas.kiesVoor(p, InteractionHand.MAIN_HAND, id), "no choice: '" + id + "'");
        }
        helper.assertTrue(BiomeKompas.gekozen(p.getMainHandItem()) == null, "the compass took nothing");
        try {
            BiomeLijst.biome("echte_guheinde", "guheinde");
            helper.fail("the teaser took a biome");
        } catch (IllegalArgumentException verwacht) {
            // as meant
        }
        // its texts say nothing: the name and the one mysterious line
        JsonObject nl = ReisguhGameTests.json("/assets/guhs/lang/nl_nl.json");
        helper.assertTrue(nl.get("gui.guhs.superkompas.sectie.echte_guheinde.slot").getAsString().equals("Hier is nog niemand geweest... njeg?"),
                "the teaser's hover text");
        weg(helper, p);
        helper.succeed();
    }

    private static List<String> biomesVan(GameTestHelper helper, Identifier dimensieBestand) {
        var bron = helper.getLevel().getServer().getResourceManager().getResource(dimensieBestand);
        helper.assertTrue(bron.isPresent(), "the dimension file " + dimensieBestand);
        String tekst;
        try (var in = bron.get().openAsReader()) {
            tekst = in.lines().collect(java.util.stream.Collectors.joining(" "));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        JsonObject generator = JsonParser.parseString(tekst).getAsJsonObject().getAsJsonObject("generator");
        JsonObject json = generator.has("biome_source") ? generator.getAsJsonObject("biome_source") : generator.getAsJsonObject("settings");
        List<String> uit = new ArrayList<>();
        if (json == null) {
            return uit;
        }
        if (json.has("biome")) {
            uit.add(json.get("biome").getAsString());
        }
        if (json.has("biomes")) {
            json.getAsJsonArray("biomes").forEach(b -> uit.add(b.getAsJsonObject().get("biome").getAsString()));
        }
        return uit;
    }

    /**
     * With everything unlocked: every guhs biome of each listed dimension stands exactly once, under its own dimension; no
     * biome of another dimension (the Guhmaag, Guhpixel, a huisje) stands anywhere; every row has a name and an icon.
     */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKompasAlleBiomes(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            Bezocht.zetDimensie(p, s.id());
        }
        List<BiomesMenu.BiomeRij> rijen = biomes(BiomesMenu.rijen(p, s -> true));
        Map<String, String> waar = new HashMap<>();
        for (BiomesMenu.BiomeRij r : rijen) {
            helper.assertTrue(waar.put(r.biome(), r.sectie()) == null, "listed once: " + r.biome());
        }
        var manager = helper.getLevel().getServer().getResourceManager();
        java.util.Set<String> gelijst = new java.util.HashSet<>(), elders = new java.util.HashSet<>();
        for (Identifier bestand : manager.listResources("dimension", id -> id.getNamespace().equals(Guhs.MODID) && id.getPath().endsWith(".json")).keySet()) {
            String naam = bestand.getPath().substring("dimension/".length(), bestand.getPath().length() - ".json".length());
            BiomeLijst.Sectie s = BiomeLijst.secties().stream()
                    .filter(x -> !x.teaser() && x.dimensie().identifier().equals(Guhs.id(naam))).findFirst().orElse(null);
            for (String b : biomesVan(helper, bestand)) {
                if (s == null) {
                    elders.add(b);
                    continue;
                }
                helper.assertTrue(b.startsWith("guhs:") && s.id().equals(waar.get(b.substring(5))), "a biome of " + naam + " stands under it: " + b
                        + " -> " + waar.get(b.substring(5)));
                gelijst.add(b);
            }
        }
        helper.assertTrue(gelijst.size() >= 25, "the three dimensions were read: " + gelijst.size() + " biomes");
        helper.assertTrue(elders.contains("guhs:guhmaag") && elders.contains("guhs:guhpixel"), "the other dimensions were read: " + elders);
        for (String b : elders) {
            helper.assertTrue(gelijst.contains(b) || !b.startsWith("guhs:") || !waar.containsKey(b.substring(5)),
                    "a biome of a dimension without a section stands in the tab: " + b);
        }
        // nothing stands in the tab that is in no listed dimension, but for the three new biomes (theirs comes with the wereld slice)
        for (String b : waar.keySet()) {
            helper.assertTrue(gelijst.contains("guhs:" + b) || List.of("bloesemmeertje", "klaterdal", "wolkenweide").contains(b),
                    "listed, but in none of the three dimensions: " + b);
        }
        helper.assertTrue("guhmension".equals(waar.get("bloesemmeertje")) && "guhmension".equals(waar.get("klaterdal"))
                && "guhmension".equals(waar.get("wolkenweide")), "the three new biomes stand under the Guhmensie");
        helper.assertTrue("barbecuether".equals(waar.get("asdal")) && "guheinde".equals(waar.get("guheinde")) && "guhmension".equals(waar.get("bleekwoud")),
                "a biome of each dimension");
        // names and icons; the tab's own texts
        JsonObject nl = ReisguhGameTests.json("/assets/guhs/lang/nl_nl.json");
        var biomeRegister = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for (String b : waar.keySet()) {
            helper.assertTrue(nl.has("biome.guhs." + b), "a name for " + b);
            helper.assertTrue(biomeRegister.containsKey(BiomeLijst.sleutel(b)), "a real biome: " + b);
            helper.assertTrue(BiomeIconen.heeft(b) && !BiomeIconen.icoon(b).isEmpty() && !BiomeIconen.icoon(b).is(Items.MAP), "an icon of its own for " + b);
        }
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            helper.assertTrue(nl.has("gui.guhs.superkompas.sectie." + s.id()) && nl.has("gui.guhs.superkompas.sectie." + s.id() + ".slot"),
                    "the texts of section " + s.id());
        }
        for (String key : List.of("gui.guhs.superkompas.biomes", "gui.guhs.superkompas.biomes.tooltip", "gui.guhs.biokompas.op_slot",
                "gui.guhs.biokompas.telling", "gui.guhs.biokompas.open", "gui.guhs.biokompas.dicht", "gui.guhs.biokompas.geweest",
                "gui.guhs.biokompas.niet_geweest", "gui.guhs.biokompas.tip_elders", "gui.guhs.biokompas.zoeken", "gui.guhs.biokompas.afstand",
                "gui.guhs.biokompas.hier", "gui.guhs.biokompas.niets", "gui.guhs.biokompas.elders")) {
            helper.assertTrue(nl.has(key), "the text " + key);
        }
        // a biome another update adds without an icon gets its section's; an unknown one a map
        helper.assertTrue(!BiomeIconen.heeft("bio_bestaat_niet") && BiomeIconen.icoon("bio_bestaat_niet").is(Items.MAP), "an unknown biome: a map");
        // the tab is no category: the structure list, its tests and the visited scan do not see it
        helper.assertTrue(SuperkompasItem.CATEGORIES.stream().noneMatch(c -> c.id().equals("biomes")) && BiomesMenu.CATEGORIE.structures().isEmpty()
                && !BiomesMenu.CATEGORIE.icoon().isEmpty() && SuperkompasItem.CATEGORIES.size() + 1 <= 15, "the tab stands apart from the categories");
        weg(helper, p);
        helper.succeed();
    }

    /** A biome gets its tick once the player stood in it (the kern's rule, every 40 ticks), and the heading counts it. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKompasVinkje(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BiomeLijst.Sectie guhmension = BiomeLijst.sectie("guhmension");
        // standing in a biome of the Guhmensie (what Bezocht.kijk does with the biome under the player)
        helper.assertTrue("kaasmoeras".equals(Bezocht.zie(p, guhmension, Guhs.id("kaasmoeras"))), "the first time in the Kaasmoeras");
        List<BiomesMenu.Rij> rijen = BiomesMenu.rijen(p, s -> true);
        helper.assertTrue(!secties(rijen).get(0).opSlot() && secties(rijen).get(0).bezocht() == 1, "being in a biome opens its dimension: " + secties(rijen));
        for (BiomesMenu.BiomeRij b : biomes(rijen)) {
            helper.assertTrue(b.bezocht() == b.biome().equals("kaasmoeras"), "only the Kaasmoeras is ticked: " + b);
        }
        Bezocht.zie(p, guhmension, Guhs.id("kaasmoeras"));
        Bezocht.zie(p, guhmension, Guhs.id("wolkenweide"));
        // a biome of another dimension, or of none, is not seen here
        Bezocht.zie(p, guhmension, Guhs.id("asdal"));
        Bezocht.zie(p, guhmension, Identifier.withDefaultNamespace("plains"));
        rijen = BiomesMenu.rijen(p, s -> true);
        helper.assertTrue(secties(rijen).get(0).bezocht() == 2 && secties(rijen).get(1).opSlot()
                && biomes(rijen).stream().filter(BiomesMenu.BiomeRij::bezocht).map(BiomesMenu.BiomeRij::biome).toList().equals(List.of("kaasmoeras", "wolkenweide")),
                "two ticks, in list order: " + secties(rijen));
        // the test world is no listed dimension: walking around here ticks nothing
        helper.assertTrue(Bezocht.kijk(p) == null && BiomeLijst.van(helper.getLevel().dimension()) == null, "nothing is seen outside the three dimensions");
        weg(helper, p);
        helper.succeed();
    }

    /** A player from before this update: the entry advancement of each of the three dimensions opens its section at login. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKompasOudeSpeler(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        var advancements = helper.getLevel().getServer().getAdvancements();
        int echte = 0;
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            if (s.teaser()) {
                continue;
            }
            echte++;
            helper.assertTrue(s.bewijs() != null && advancements.get(s.bewijs()) != null, "section " + s.id() + " has an entry advancement that exists: " + s.bewijs());
        }
        helper.assertTrue(echte == 3, "three real dimensions");
        Bezocht.bijwerken(p);
        helper.assertTrue(secties(BiomesMenu.rijen(p, s -> true)).stream().allMatch(BiomesMenu.SectieRij::opSlot), "no advancement: all locked");
        // an old save: the Barbecuether and the Guheinde were visited long ago
        for (String id : List.of("barbecuether", "guheinde")) {
            AdvancementHolder holder = advancements.get(BiomeLijst.sectie(id).bewijs());
            for (String criterium : holder.value().criteria().keySet()) {
                p.getAdvancements().award(holder, criterium);
            }
        }
        helper.assertTrue(BiomesMenu.opSlot(p, BiomeLijst.sectie("barbecuether")), "nothing changes before the login");
        Bezocht.bijwerken(p);   // (what the login does)
        List<BiomesMenu.SectieRij> s = secties(BiomesMenu.rijen(p, x -> true));
        helper.assertTrue(s.get(0).opSlot() && !s.get(1).opSlot() && !s.get(2).opSlot() && s.get(3).opSlot(), "the two visited dimensions opened: " + s);
        helper.assertTrue(s.get(1).bezocht() == 0 && s.get(1).totaal() == BiomeLijst.biomes("barbecuether").size(),
                "biomes visited before this update are not known: no ticks yet");
        helper.assertTrue(BiomesMenu.magKiezen(p, "asdal") && !BiomesMenu.magKiezen(p, "guh_fields"), "and their biomes can be chosen");
        weg(helper, p);
        helper.succeed();
    }

    /** A biome source for the tests: {@code ander} where the rule says so, {@code basis} everywhere else. */
    private static final class TestBron extends BiomeSource {
        interface Regel {
            boolean ander(int x, int y, int z);
        }

        private final Holder<Biome> basis, ander;
        private final Regel regel;

        TestBron(Holder<Biome> basis, Holder<Biome> ander, Regel regel) {
            this.basis = basis;
            this.ander = ander;
            this.regel = regel;
        }

        @Override
        protected MapCodec<? extends BiomeSource> codec() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Stream<Holder<Biome>> collectPossibleBiomes() {
            return Stream.of(basis, ander);
        }

        @Override
        public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
            return regel.ander(qx << 2, qy << 2, qz << 2) ? ander : basis;
        }
    }

    private static BiomeZoeker zoeker(GameTestHelper helper, BlockPos van, int straal, TestBron.Regel regel) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<Biome> basis = biomes.getOrThrow(BiomeLijst.sleutel("guh_fields")), doel = biomes.getOrThrow(BiomeLijst.sleutel("bleekwoud"));
        Predicate<Holder<Biome>> is = h -> h.is(BiomeLijst.sleutel("bleekwoud"));
        return new BiomeZoeker(new TestBron(basis, doel, regel), Climate.empty(), is, van, straal, 64, 0, 255);
    }

    /** The search: the nearest spot of a biome that is there, a clean "nothing" when it is not within reach, in bounded steps. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKompasZoeken(GameTestHelper helper) {
        BlockPos van = new BlockPos(100, 70, -40);
        // the holder stands in it
        BiomeZoeker z = zoeker(helper, van, 8192, (x, y, zz) -> true).helemaal();
        helper.assertTrue(z.klaar() && z.gevonden() != null && z.gevonden().getX() == 100 && z.gevonden().getZ() == -40 && z.monsters() == 1,
                "under the holder: " + z.gevonden() + " after " + z.monsters());
        // a region to the east: the nearest column of the grid
        z = zoeker(helper, van, 8192, (x, y, zz) -> x >= 1000).helemaal();
        helper.assertTrue(z.gevonden() != null && z.gevonden().getX() == 100 + 15 * 64 && z.gevonden().getZ() == -40, "the nearest spot east: " + z.gevonden());
        // two regions: the nearer one wins, also when a square ring reaches the farther one first (a corner)
        z = zoeker(helper, van, 8192, (x, y, zz) -> x >= 100 + 640 && zz >= -40 + 640 || zz <= -40 - 800 && Math.abs(x - 100) < 64).helemaal();
        helper.assertTrue(z.gevonden() != null && z.gevonden().getX() == 100 && z.gevonden().getZ() == -40 - 832, "the nearer of two: " + z.gevonden());
        // a biome that only exists deep down
        z = zoeker(helper, van, 8192, (x, y, zz) -> y < 32 && x <= -400).helemaal();
        helper.assertTrue(z.gevonden() != null && z.gevonden().getY() == 16 && z.gevonden().getX() == 100 - 8 * 64, "deep down: " + z.gevonden());
        // not within reach: a clean "nothing", after a bounded number of spots
        z = zoeker(helper, van, 2048, (x, y, zz) -> x >= 5000);
        int stappen = 0;
        while (!z.stap(1000)) {
            stappen++;
            helper.assertTrue(stappen < 1000, "the search ends");
        }
        helper.assertTrue(z.klaar() && z.gevonden() == null && stappen >= 5 && z.monsters() <= 65L * 65 * 8, "nothing within 2048 blocks: " + z.monsters()
                + " spots in " + stappen + " steps");
        // a step does a bounded amount of work
        z = zoeker(helper, van, 8192, (x, y, zz) -> false);
        helper.assertTrue(!z.stap(500) && z.monsters() <= 508 && !z.klaar() && z.gevonden() == null, "one step looks at about its budget: " + z.monsters());
        // chunks made before an update moved the biomes keep their old biome: such spots are skipped
        z = zoeker(helper, van, 8192, (x, y, zz) -> x >= 1000).metControle(plek -> plek.getX() >= 2000).helemaal();
        helper.assertTrue(z.gevonden() != null && z.gevonden().getX() == 100 + 30 * 64 && z.afgekeurd() > 0, "older chunks are skipped: " + z.gevonden()
                + ", " + z.afgekeurd() + " skipped");
        z = zoeker(helper, van, 2048, (x, y, zz) -> x >= 1000).metControle(plek -> false).helemaal();
        helper.assertTrue(z.klaar() && z.gevonden() == null && z.afgekeurd() > 0, "only older chunks: nothing");
        // (the saved form of a real chunk: it holds its own biome, not another; a chunk without biomes yet passes)
        BlockPos hierChunk = helper.absolutePos(BlockPos.ZERO);
        var nbt = net.minecraft.world.level.chunk.storage.SerializableChunkData.copyOf(helper.getLevel(), helper.getLevel().getChunkAt(hierChunk)).write();
        String eigen = helper.getLevel().getBiome(hierChunk).unwrapKey().orElseThrow().identifier().toString();
        helper.assertTrue(Opgeslagen.kanZijn(nbt, eigen) && !Opgeslagen.kanZijn(nbt, "guhs:klaterdal"), "a saved chunk holds " + eigen + " and no Klaterdal");
        net.minecraft.nbt.CompoundTag leeg = new net.minecraft.nbt.CompoundTag();
        leeg.putString("Status", "minecraft:structure_starts");
        helper.assertTrue(Opgeslagen.kanZijn(leeg, "guhs:klaterdal") && Opgeslagen.kanZijn(new net.minecraft.nbt.CompoundTag(), "guhs:klaterdal"),
                "a chunk whose biomes are not made yet can still become it");
        helper.assertTrue(Opgeslagen.controle(helper.getLevel(), eigen).test(hierChunk), "the check on this world's storage passes its own biome");
        // a real level: the biome under the holder is found at once; one its biome source does not have at all is "nothing" at once
        BlockPos hier = helper.absolutePos(BlockPos.ZERO);
        var key = helper.getLevel().getBiome(hier).unwrapKey().orElseThrow();
        z = BiomeZoeker.voor(helper.getLevel(), key, hier, 256, 64).helemaal();
        helper.assertTrue(z.klaar() && z.gevonden() != null && z.gevonden().closerThan(new BlockPos(hier.getX(), z.gevonden().getY(), hier.getZ()), 400),
                "this world's own biome is found close by: " + key.identifier() + " at " + z.gevonden());
        z = BiomeZoeker.voor(helper.getLevel(), BiomeLijst.sleutel("klaterdal"), hier, 8192, 64);
        helper.assertTrue(z.klaar() && z.gevonden() == null && z.monsters() == 0, "a biome this world does not have: nothing, at once");
        helper.assertTrue(KompasSlice.zoek(helper.getLevel(), hier, "klaterdal").contains(": niets binnen 8192 blokken"), "the dev command's line");
        helper.succeed();
    }

    /** The compass: a choice is refused behind a padlock, replaces the structure, names the compass, points the needle, and looks again only after a walk. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void bioKompasKompas(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ItemStack stack = new ItemStack(ModItems.SUPERKOMPAS.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        SuperkompasItem.choose(stack, "guh_caves");
        helper.assertTrue(!BiomeKompas.kiesVoor(p, InteractionHand.MAIN_HAND, "bleekwoud") && BiomeKompas.gekozen(stack) == null
                && "guh_caves".equals(SuperkompasItem.chosen(stack)), "refused behind the padlock");
        helper.assertTrue(!BiomeKompas.tick(stack, helper.getLevel(), p), "no biome chosen: the structure search goes on");
        Bezocht.zetDimensie(p, "guhmension");
        helper.assertTrue(!BiomeKompas.kiesVoor(p, InteractionHand.OFF_HAND, "bleekwoud"), "the other hand holds no compass");
        helper.assertTrue(BiomeKompas.kiesVoor(p, InteractionHand.MAIN_HAND, "bleekwoud") && "bleekwoud".equals(BiomeKompas.gekozen(stack))
                && SuperkompasItem.chosen(stack) == null, "a biome instead of the structure");
        helper.assertTrue(BiomeKompas.naam(stack) != null && stack.getItem().getName(stack).equals(BiomeKompas.naam(stack))
                && BiomeKompas.naam(stack).toString().contains("biome.guhs.bleekwoud"), "the compass is named after the biome: " + stack.getItem().getName(stack));
        // in the Guhmensie (here: its key and a test biome source), with the Bleekwoud from 600 blocks east on (the grid: 640)
        BlockPos start = p.blockPosition();
        int[] zoektochten = {0};
        java.util.function.Supplier<BiomeZoeker> maker = () -> {
            zoektochten[0]++;
            return zoeker(helper, p.blockPosition(), 8192, (x, y, z) -> x >= start.getX() + 600);
        };
        helper.assertTrue("afstand".equals(BiomeKompas.volg(stack, p, ModDimensions.GUHMENSION, "bleekwoud", true, maker, () -> false, true)), "found");
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        helper.assertTrue(tracker != null && tracker.target().isPresent() && tracker.target().get().dimension() == ModDimensions.GUHMENSION
                && tracker.target().get().pos().getX() == start.getX() + 640 && tracker.target().get().pos().getZ() == start.getZ(), "the needle points there: " + tracker);
        // standing still, or a short walk: no new search
        BiomeKompas.volg(stack, p, ModDimensions.GUHMENSION, "bleekwoud", true, maker, () -> false, true);
        p.snapTo(start.getX() + 40.5, start.getY(), start.getZ() + 0.5);
        BiomeKompas.volg(stack, p, ModDimensions.GUHMENSION, "bleekwoud", false, maker, () -> false, true);
        helper.assertTrue(zoektochten[0] == 1, "no new search within " + 640 / 8 + " blocks: " + zoektochten[0]);
        // a good walk: it looks again, from the new spot
        p.snapTo(start.getX() + 200.5, start.getY(), start.getZ() + 300.5);
        helper.assertTrue("afstand".equals(BiomeKompas.volg(stack, p, ModDimensions.GUHMENSION, "bleekwoud", true, maker, () -> false, true))
                && zoektochten[0] == 2, "a new search after a walk: " + zoektochten[0]);
        tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        helper.assertTrue(tracker != null && tracker.target().get().pos().getZ() == start.getZ() + 300 && tracker.target().get().pos().getX() >= start.getX() + 640
                && tracker.target().get().pos().getX() < start.getX() + 640 + 64, "the needle moved along: " + tracker);
        // in the biome itself
        helper.assertTrue("hier".equals(BiomeKompas.volg(stack, p, ModDimensions.GUHMENSION, "bleekwoud", true, maker, () -> true, true)), "you are there");
        // another dimension of the tab where the biome is not within reach: a clean "nothing", the needle spins
        Bezocht.zetDimensie(p, "barbecuether");
        helper.assertTrue(BiomeKompas.kiesVoor(p, InteractionHand.MAIN_HAND, "asdal") && !stack.has(DataComponents.LODESTONE_TRACKER), "a new choice: the needle is free");
        java.util.function.Supplier<BiomeZoeker> leeg = () -> zoeker(helper, p.blockPosition(), 1024, (x, y, z) -> false);
        helper.assertTrue("niets".equals(BiomeKompas.volg(stack, p, net.minecraft.world.level.Level.NETHER, "asdal", true, leeg, () -> false, true))
                && !stack.has(DataComponents.LODESTONE_TRACKER), "nothing within reach");
        // the test world is none of the three dimensions: the compass handles it (no structure search), the needle spins
        helper.assertTrue(BiomeKompas.tick(stack, helper.getLevel(), p) && !stack.has(DataComponents.LODESTONE_TRACKER), "elsewhere");
        // the search thread: "zoeken" first, the result a moment later
        helper.assertTrue(BiomeKompas.kiesVoor(p, InteractionHand.MAIN_HAND, "bleekwoud"), "the Bleekwoud again");
        p.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
        java.util.concurrent.CountDownLatch wacht = new java.util.concurrent.CountDownLatch(1);
        java.util.function.Supplier<BiomeZoeker> traag = () -> zoeker(helper, p.blockPosition(), 8192, (x, y, z) -> {
            try {
                wacht.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return x >= start.getX() + 600;
        });
        helper.assertTrue("zoeken".equals(BiomeKompas.volg(stack, p, ModDimensions.GUHMENSION, "bleekwoud", true, traag, () -> false, false)) && BiomeKompas.taken() >= 1
                && !stack.has(DataComponents.LODESTONE_TRACKER), "the search runs elsewhere: the compass says it is looking");
        helper.assertTrue("zoeken".equals(BiomeKompas.volg(stack, p, ModDimensions.GUHMENSION, "bleekwoud", true, traag, () -> false, false)), "still looking, no second search");
        wacht.countDown();
        helper.succeedWhen(() -> {
            String zegt = BiomeKompas.volg(stack, p, ModDimensions.GUHMENSION, "bleekwoud", true, traag, () -> false, false);
            helper.assertTrue("afstand".equals(zegt) && stack.has(DataComponents.LODESTONE_TRACKER), "the result arrives: " + zegt);
            // a structure again: the biome goes
            SuperkompasItem.choose(stack, "guh_caves");
            helper.assertTrue(BiomeKompas.gekozen(stack) == null && "guh_caves".equals(SuperkompasItem.chosen(stack)), "a structure instead of the biome");
            weg(helper, p);
        });
    }
}
