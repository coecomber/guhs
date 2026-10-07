package nl.juiced.guhs.feature.einde2.bouwaarde;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Ontdekt;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.LeegteBouwStructure;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the slice bouw_aarde: Ba Sing Guh as a structure (its one guaranteed copy for three seeds, its 8 x 8 tiles,
 * its place among the story structures), its markers (every id of build contract 6.7 with its kind, the free room the
 * contract asks for, the two overlays), what stands in a few of its tiles, the koolman's stall (whole, wrecked, gone) and
 * the six decorative blocks. The build itself is checked block for block by its generator
 * (tools/features/einde2_bouw_aarde.py: every floor reachable and lit, nothing floating).
 */
public class E2BouwAardeGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "einde2_bouw_aarde";
    private static final String STAD = Einde2Ids.BA_SING_GUH;

    /** id -> kind of marker, for every single marker the contract names for ba_sing_guh (and the standard ones). */
    private static final Map<String, String> PLEKKEN = Map.ofEntries(
            // every guhs:leegte_bouw, every story structure
            Map.entry("aankomst", Plekken.ZONE), Map.entry("aankomst_speler", Plekken.PROP), Map.entry("uitzicht", Plekken.PROP),
            Map.entry("kamp", Plekken.ZONE), Map.entry("appa_kamp", Plekken.PROP), Map.entry("team_aang", Plekken.NPC), Map.entry("team_momo", Plekken.NPC),
            Map.entry("team_katara", Plekken.NPC), Map.entry("team_sokka", Plekken.NPC), Map.entry("team_toph", Plekken.NPC), Map.entry("team_zuka", Plekken.NPC),
            // the outer wall, the rings, the monorail
            Map.entry("buitenpoort", Plekken.PROP), Map.entry("loket", Plekken.PROP), Map.entry("loketguh", Plekken.NPC),
            Map.entry("ring_onder", Plekken.ZONE), Map.entry("ring_midden", Plekken.ZONE), Map.entry("ring_boven", Plekken.ZONE),
            Map.entry("monorail_route", Plekken.PAD), Map.entry("station_buiten", Plekken.PROP), Map.entry("station_onder", Plekken.PROP),
            Map.entry("station_midden", Plekken.PROP), Map.entry("station_boven", Plekken.PROP), Map.entry("monorail_wagen", Plekken.PROP),
            // the Aardrommel
            Map.entry("aardrommel", Plekken.ZONE), Map.entry("toph", Plekken.NPC), Map.entry("kei", Plekken.NPC), Map.entry("omroeper", Plekken.NPC),
            Map.entry("arena_speler", Plekken.PROP), Map.entry("arena_tegenstander", Plekken.PROP), Map.entry("blinddoek_baan", Plekken.ZONE),
            // De Jasmijn Draak, the street
            Map.entry("theehuis", Plekken.ZONE), Map.entry("miroh", Plekken.NPC), Map.entry("zuka_ober", Plekken.NPC), Map.entry("theeketel", Plekken.PROP),
            Map.entry("pai_sho", Plekken.PROP), Map.entry("keuken", Plekken.ZONE), Map.entry("koolman", Plekken.NPC),
            // guides and guards, the palace
            Map.entry("joo_dee", Plekken.NPC), Map.entry("joo_dee_huis", Plekken.ZONE), Map.entry("gasthuis", Plekken.ZONE),
            Map.entry("paleis", Plekken.ZONE), Map.entry("aardekoning", Plekken.NPC), Map.entry("bosco", Plekken.NPC), Map.entry("bosco_kamer", Plekken.ZONE),
            Map.entry("long_feng", Plekken.NPC),
            // Meer Laogai and the base
            Map.entry("meer_laogai", Plekken.ZONE), Map.entry("basis_ingang", Plekken.PROP), Map.entry("basis", Plekken.ZONE),
            Map.entry("sluip_start", Plekken.PROP), Map.entry("sluip_route", Plekken.PAD), Map.entry("lampkamer", Plekken.ZONE),
            Map.entry("appa_kamer", Plekken.ZONE), Map.entry("appa_gevangen", Plekken.PROP),
            // the catacombs
            Map.entry("catacomben_ingang", Plekken.PROP), Map.entry("catacomben", Plekken.ZONE), Map.entry("kristalkamer", Plekken.ZONE),
            Map.entry("azula", Plekken.NPC), Map.entry("bliksem_speler", Plekken.PROP), Map.entry("bliksem_azula", Plekken.PROP),
            Map.entry("zuka_keuze", Plekken.PROP), Map.entry("katara_redt", Plekken.PROP), Map.entry("miroh_gevangen", Plekken.PROP),
            Map.entry("vlucht_gat", Plekken.PROP),
            // the winks
            Map.entry("knipoog_sjokkel", Plekken.PAD), Map.entry("knipoog_sjokkel_camera", Plekken.PAD), Map.entry("knipoog_mewtwo_tank", Plekken.PROP),
            Map.entry("knipoog_mewtwo_dai_li", Plekken.NPC), Map.entry("knipoog_mewtwo_mew", Plekken.PAD), Map.entry("knipoog_mewtwo_camera", Plekken.PAD));
    /** The numbered groups: name -> (kind of marker, at least this many). */
    private static final Map<String, Map.Entry<String, Integer>> GROEPEN = Map.of(
            "bewoner", Map.entry(Plekken.NPC, 10), "tril_doel", Map.entry(Plekken.PROP, 6), "klant", Map.entry(Plekken.NPC, 3),
            "koolkraam", Map.entry(Plekken.PROP, 3), "dai_li", Map.entry(Plekken.NPC, 6), "sluip_wacht", Map.entry(Plekken.NPC, 6),
            "cel", Map.entry(Plekken.PROP, 4), "rustpunt", Map.entry(Plekken.PROP, 3), "das_molguh", Map.entry(Plekken.WEZEN, 2));
    /** The characters a later phase puts on a spot: marker -> the GuhNpcEntity.Kind name reserved for it. */
    private static final Map<String, String> KINDS = Map.ofEntries(Map.entry("loketguh", "LOKETGUH"), Map.entry("bewoner_1", "BA_SING_GUH_BURGER"),
            Map.entry("toph", "TOPH_GUH"), Map.entry("kei", "KEI_GUH"), Map.entry("omroeper", "AARDROMMEL_OMROEPER"), Map.entry("miroh", "OOM_MIROH"),
            Map.entry("zuka_ober", "ZUKA"), Map.entry("koolman", "KOOLMAN"), Map.entry("joo_dee", "JOO_DEE_GUH"), Map.entry("dai_li_1", "DAI_LI_MIKA"),
            Map.entry("dai_li_6", "DAI_LI_MIKA"), Map.entry("aardekoning", "AARDEKONING_GUH"), Map.entry("bosco", "BOSCO"),
            Map.entry("long_feng", "LONG_FENG_MIKA"), Map.entry("sluip_wacht_1", "DAI_LI_MIKA"), Map.entry("azula", "AZULA_MIKA"),
            Map.entry("knipoog_mewtwo_dai_li", "DAI_LI_MIKA"));

    private static Block blok(String id) {
        return BuiltInRegistries.BLOCK.getValue(Guhs.id(id));
    }

    /** The one guaranteed copy: found for three seeds, in its ring, in the high band, every tile and marker inside its box. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BouwAardePlaatsing(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, STAD);
        helper.succeed();
    }

    /** The structure: 8 x 8 tiles of 32 x 110 x 32 (56 of them hold blocks), a story structure (so protected, and veiled later), in no compass. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwAardeStructuur(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LeegteBouwStructure bouw = Plekken.leegteBouw(level.registryAccess(), STAD);
        helper.assertTrue(bouw != null, "ba_sing_guh is a guhs:leegte_bouw");
        helper.assertTrue(bouw.size().equals(new Vec3i(256, 110, 256)) && bouw.tilesX() == 8 && bouw.tilesZ() == 8, "256 x 110 x 256 in 8 x 8 tiles: " + bouw.size());
        helper.assertTrue(bouw.anchor().equals(new BlockPos(120, 0, 120)) && bouw.rand() == 48, "anchor and rim: " + bouw.anchor() + ", " + bouw.rand());
        helper.assertTrue(bouw.y() >= Einde2Ids.HOOG_MIN && bouw.y() + bouw.size().getY() - 1 <= Einde2Ids.HOOG_MAX, "in the high band: y " + bouw.y());
        helper.assertTrue(bouw.leeg().isEmpty() && !bouw.ruim() && bouw.bij().isEmpty(), "a plain high-band build");
        int tegels = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                StructureTemplate tegel = level.getStructureManager().get(bouw.tile(i, j)).orElse(null);
                if (tegel != null) {
                    tegels++;
                    Vec3i g = tegel.getSize();
                    helper.assertTrue(g.getX() == 32 && g.getY() == 110 && g.getZ() == 32, "tile " + i + "," + j + ": " + g);
                }
            }
        }
        helper.assertTrue(tegels == 56, "the round island leaves the eight corner tiles of the 8 x 8 empty: " + tegels);
        helper.assertTrue(Einde2Ids.VERHAAL_STRUCTUREN.contains(STAD) && Einde2Ids.VERHAAL_STRUCTUREN.indexOf(STAD) == 2, "the third story structure");
        var structuren = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        helper.assertTrue(structuren.get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(STAD))).map(h -> h.is(Einde2Ids.VERHAAL)).orElse(false),
                "in the structure tag guhs:einde2/verhaal");
        helper.assertTrue(!Einde2Ontdekt.inKompas(STAD), "hidden until the story gets there: in no compass tab");
        helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(STAD)) == null, "no random copies: it exists once");
        Structure structuur = structuren.getValue(Guhs.id(STAD));
        helper.assertTrue(structuur != null && structuur.spawnOverrides().size() == 2, "nothing spawns inside (monster and creature overrides)");
        helper.succeed();
    }

    /** The sidecar: every marker of the contract with its kind, the numbered groups, the characters' reserved kinds, the room the contract asks for. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwAardePlekken(GameTestHelper helper) {
        Plekken.Blad blad = Plekken.blad(STAD);
        helper.assertTrue(blad.soort().equals("leegte_bouw") && blad.grootte().equals(new Vec3i(256, 110, 256)) && blad.anker().equals(new BlockPos(120, 0, 120)),
                "the sidecar of the city: " + blad.grootte());
        Map<String, Plekken.Plek> p = blad.plekken();
        for (Map.Entry<String, String> e : PLEKKEN.entrySet()) {
            Plekken.Plek plek = p.get(e.getKey());
            helper.assertTrue(plek != null && plek.soort().equals(e.getValue()), "marker " + e.getKey() + " (" + e.getValue() + "): " + plek);
        }
        for (Map.Entry<String, Map.Entry<String, Integer>> e : GROEPEN.entrySet()) {
            int n = 0;
            while (p.containsKey(e.getKey() + "_" + (n + 1))) {
                n++;
                helper.assertTrue(p.get(e.getKey() + "_" + n).soort().equals(e.getValue().getKey()), e.getKey() + "_" + n + " is a " + e.getValue().getKey());
            }
            helper.assertTrue(n >= e.getValue().getValue(), "at least " + e.getValue().getValue() + " of " + e.getKey() + "_*: " + n);
        }
        for (Map.Entry<String, String> e : KINDS.entrySet()) {
            helper.assertTrue(e.getValue().equals(p.get(e.getKey()).kind()), e.getKey() + " is reserved for " + e.getValue() + ": " + p.get(e.getKey()).kind());
        }
        helper.assertTrue("guhs:das_molguh".equals(p.get("das_molguh_1").entity()), "the das-molguhs of the catacombs are real creatures");
        // every marker lies inside the build (only the opening shot's camera spot lies outside it)
        for (Plekken.Plek plek : p.values()) {
            BlockPos q = plek.pos();
            helper.assertTrue(plek.id().equals("uitzicht") || q.getX() >= 0 && q.getX() < 256 && q.getY() >= 0 && q.getY() < 110 && q.getZ() >= 0 && q.getZ() < 256,
                    plek.id() + " inside the build: " + q);
        }
        // the room the contract asks for
        Plekken.Plek aankomst = p.get("aankomst"), appa = p.get("appa_kamer"), route = p.get("monorail_route"), sluip = p.get("sluip_route");
        helper.assertTrue(aankomst.tot().getX() - aankomst.pos().getX() >= 8 && aankomst.tot().getZ() - aankomst.pos().getZ() >= 8, "a landing place of at least 9 x 9");
        helper.assertTrue(appa.tot().getX() - appa.pos().getX() >= 10 && appa.tot().getY() - appa.pos().getY() >= 6 && appa.tot().getZ() - appa.pos().getZ() >= 10,
                "Appa's chamber is at least 11 x 7 x 11");
        helper.assertTrue(route.punten().size() >= 4 && route.punten().stream().allMatch(q -> q.getY() == route.pos().getY() && q.getZ() == route.pos().getZ()),
                "the monorail is one straight line through all rings: " + route.punten());
        for (String station : List.of("station_buiten", "station_onder", "station_midden", "station_boven")) {
            helper.assertTrue(Math.abs(p.get(station).pos().getZ() - route.pos().getZ()) <= 5 && Math.abs(p.get(station).pos().getY() - route.pos().getY()) <= 1,
                    station + " lies at the track: " + p.get(station).pos());
        }
        helper.assertTrue(sluip.punten().size() >= 3 && sluip.punten().get(0).equals(p.get("sluip_start").pos()), "the sneak route starts at sluip_start");
        helper.assertTrue(p.get("kristalkamer").pos().getY() < 40 && p.get("basis").pos().getY() < 40 && p.get("paleis").pos().getY() > 45,
                "the catacombs and the base lie under the city");
        helper.assertTrue(p.get("knipoog_sjokkel").punten().size() == 2 && p.get("knipoog_sjokkel").punten().get(0).distManhattan(p.get("knipoog_sjokkel").punten().get(1)) >= 6,
                "Sjokkel has at least six blocks of wall to creep along");
        helper.assertTrue(p.get("knipoog_mewtwo_camera").punten().size() == 2 && p.get("knipoog_mewtwo_mew").punten().size() == 2, "the wink in the base: a camera and Mew-guh's line");
        double afstand = Math.sqrt(p.get("knipoog_mewtwo_camera").pos().distSqr(p.get("knipoog_mewtwo_tank").pos()));
        helper.assertTrue(afstand >= 6 && afstand <= 14, "the camera stands 6 to 14 blocks from the tank: " + afstand);
        // the two overlays
        Map<String, Plekken.Laag> lagen = blad.lagen();
        for (String naam : List.of(Koolkraam.HEEL, Koolkraam.KAPOT)) {
            Plekken.Laag laag = lagen.get(naam);
            helper.assertTrue(laag != null && laag.template().equals(Guhs.id(STAD + "/laag_" + naam)) && laag.grootte().equals(new Vec3i(5, 4, 4))
                    && laag.pos().equals(p.get("koolkraam_1").pos()), "the overlay " + naam + ": " + laag);
            helper.assertTrue(helper.getLevel().getStructureManager().get(laag.template()).isPresent(), "its template exists");
        }
        for (int nr = 1; nr <= Koolkraam.SPOTS; nr++) {
            helper.assertTrue(Koolkraam.plek(nr) != null, "spot " + nr + " of the koolman's stall");
        }
        helper.succeed();
    }

    private static int tel(ServerLevel level, String tegel, Block block) {
        StructureTemplate template = level.getStructureManager().get(Guhs.id(STAD + "/" + tegel)).orElseThrow();
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    /** A look into a few tiles: what the story needs to be BUILT is there (the tank, the bars, the crystals, the track, the throne's coin). */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwAardeTegels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Map<String, Plekken.Plek> p = Plekken.van(STAD);
        java.util.function.Function<String, String> tegelVan = id -> "stuk_" + p.get(id).pos().getX() / 32 + "_" + p.get(id).pos().getZ() / 32;
        helper.assertTrue(tel(level, tegelVan.apply("knipoog_mewtwo_tank"), Blocks.GLASS) >= 60, "the glass tank of the wink stands in the base");
        helper.assertTrue(tel(level, tegelVan.apply("knipoog_mewtwo_tank"), Blocks.IRON_TRAPDOOR) == 1, "with a hatch on top");
        helper.assertTrue(tel(level, tegelVan.apply("appa_gevangen"), Blocks.IRON_BARS) >= 20, "Appa's chamber is shut with bars");
        helper.assertTrue(tel(level, tegelVan.apply("cel_1"), Blocks.IRON_DOOR) >= 2, "the cells have iron doors");
        helper.assertTrue(tel(level, tegelVan.apply("bliksem_speler"), blok("einde2_bouw_aarde_groen_kristal")) >= 30
                && tel(level, tegelVan.apply("bliksem_speler"), blok("kristalkaas_kristal")) >= 3, "the kristalkamer glows with crystals");
        helper.assertTrue(tel(level, tegelVan.apply("station_onder"), blok("einde2_bouw_aarde_spoor")) >= 20, "the beam of the monorail runs past station Onderring");
        helper.assertTrue(tel(level, tegelVan.apply("aardekoning"), blok("einde2_bouw_aarde_aardemunt")) >= 1, "the aardemunt hangs behind the throne");
        helper.assertTrue(tel(level, tegelVan.apply("loket"), Blocks.IRON_BARS) >= 2, "the passport desk has its window");
        helper.assertTrue(tel(level, tegelVan.apply("miroh"), blok("theepotje")) >= 3, "teapots on the counter of De Jasmijn Draak");
        helper.assertTrue(tel(level, tegelVan.apply("meer_laogai"), Blocks.WATER) >= 300, "Meer Laogai holds water");
        // the spots of the stall are free in the templates: nothing of the koolman is built in phase A
        for (int nr = 1; nr <= Koolkraam.SPOTS; nr++) {
            BlockPos hoek = Koolkraam.plek(nr).pos();
            StructureTemplate tegel = level.getStructureManager().get(Guhs.id(STAD + "/stuk_" + hoek.getX() / 32 + "_" + hoek.getZ() / 32)).orElseThrow();
            int lx = hoek.getX() % 32, lz = hoek.getZ() % 32;
            helper.assertTrue(tegel.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), blok("einde2_bouw_aarde_kool")).stream()
                    .noneMatch(info -> info.pos().getY() >= hoek.getY() && info.pos().getY() <= hoek.getY() + 3 && info.pos().getX() >= lx && info.pos().getX() <= lx + 4
                            && info.pos().getZ() >= lz && info.pos().getZ() <= lz + 3), "no stall on spot " + nr + " yet");
        }
        helper.succeed();
    }

    /** The koolman's stall on a floor: whole (a table full of kool under a cloth), wrecked (kool all over the street), gone. */
    @GuhTest(template = "einde2_bouw_aarde_test_plein", batch = BATCH)
    public static void einde2BouwAardeKoolkraam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos hoek = helper.absolutePos(new BlockPos(2, 2, 2));
        Block kool = E2BouwAardeFeature.KOOL.get();
        java.util.function.IntSupplier kolen = () -> (int) BlockPos.betweenClosedStream(hoek, hoek.offset(4, 3, 3)).filter(q -> level.getBlockState(q).is(kool)).count();
        java.util.function.IntSupplier alles = () -> (int) BlockPos.betweenClosedStream(hoek, hoek.offset(4, 3, 3)).filter(q -> !level.getBlockState(q).isAir()).count();
        helper.assertTrue(alles.getAsInt() == 0, "the spot starts free");
        helper.assertTrue(Koolkraam.zet(level, hoek, Koolkraam.Staat.HEEL), "the whole stall is placed");
        int heel = alles.getAsInt();
        helper.assertTrue(kolen.getAsInt() == 7 && level.getBlockState(hoek).is(Blocks.SPRUCE_FENCE) && level.getBlockState(hoek.offset(2, 3, 1)).is(Blocks.GREEN_WOOL),
                "seven heads of kool under a striped cloth on four posts: " + kolen.getAsInt());
        helper.assertTrue(Koolkraam.zet(level, hoek, Koolkraam.Staat.KAPOT), "the wrecked stall is placed");
        helper.assertTrue(kolen.getAsInt() == 6 && alles.getAsInt() < heel && level.getBlockState(hoek.offset(2, 3, 1)).isAir(),
                "the cloth is down and the kool lies around: " + kolen.getAsInt());
        helper.assertTrue(BlockPos.betweenClosedStream(hoek, hoek.offset(4, 3, 3)).noneMatch(q -> level.getBlockState(q).hasBlockEntity()),
                "an overlay holds no block entities (it can be shown to one player)");
        helper.assertTrue(Koolkraam.zet(level, hoek, Koolkraam.Staat.WEG) && alles.getAsInt() == 0, "and gone again: the spot is free");
        helper.assertTrue(level.getBlockState(hoek.below()).is(blok("klipkaas_stenen")), "the street under it is untouched");
        helper.succeed();
    }

    /** The six decorative blocks: registered with an item, plain blocks without a block entity, mined to themselves, craftable; the chests have their tables. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwAardeBlokken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(E2BouwAardeFeature.BLOCK_ITEMS.size() == 6, "six blocks");
        for (DeferredBlock<Block> block : E2BouwAardeFeature.BLOCK_ITEMS) {
            String id = block.getId().getPath();
            BlockState state = block.get().defaultBlockState();
            helper.assertTrue(id.startsWith("einde2_bouw_aarde_") && !new ItemStack(block.get()).isEmpty(), id + " has an item");
            helper.assertTrue(!state.hasBlockEntity() && state.getProperties().isEmpty(), id + " is a plain block: no state, no block entity");
            LootTable loot = level.getServer().reloadableRegistries().getLootTable(block.get().getLootTable().orElseThrow());
            helper.assertTrue(loot != LootTable.EMPTY, id + " drops itself");
            helper.assertTrue(level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Guhs.id(id))).isPresent(), id + " can be made");
        }
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(E2BouwAardeFeature.GROEN_KRISTAL.get().defaultBlockState().getLightEmission(level, at) == 12, "the catacomb crystal glows (12)");
        helper.assertTrue(!E2BouwAardeFeature.KOOL.get().defaultBlockState().getShape(level, at).equals(Shapes.block()), "a head of kool is smaller than a block");
        helper.assertTrue(E2BouwAardeFeature.STADSSTEEN.get().defaultBlockState().getShape(level, at).equals(Shapes.block()), "the wall stone is a whole block");
        for (String tafel : List.of("huis", "theehuis", "paleis", "basis", "catacomben")) {
            LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/einde2_bouw_aarde_" + tafel)));
            helper.assertTrue(loot != LootTable.EMPTY, "the chest table einde2_bouw_aarde_" + tafel);
        }
        helper.succeed();
    }
}
