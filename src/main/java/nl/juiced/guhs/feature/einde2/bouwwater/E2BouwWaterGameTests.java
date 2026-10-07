package nl.juiced.guhs.feature.einde2.bouwwater;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Bescherming;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.LeegteBouwStructure;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the Noordelijke Waterstam (einde2, slice bouw_water). The build itself is checked where it is made
 * (tools/features/einde2_bouw_water_bouw.py: reachability, light, floating blocks, the canals, the sled run); here: that
 * the game finds it where the build contract says (the guaranteed copy, three seeds, only above the streken with blue
 * water), that its sidecar holds every marker of the contract with the right kind, that every tile and the fleet overlay
 * load with the blocks they are made of, and that our six blocks are what they say (and stay put in a protected place).
 */
public class E2BouwWaterGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "einde2_bouw_water";
    private static final String KAMER = "einde2_bouw_water_test_kamer";
    private static final String NAAM = Einde2Ids.NOORDELIJKE_WATERSTAM;

    /** The contract's markers: id -> kind of marker (a trailing {@code *n} = at least n numbered ones). */
    private static final Map<String, String> PLEKKEN = Map.ofEntries(
            Map.entry("aankomst", Plekken.ZONE), Map.entry("aankomst_speler", Plekken.PROP), Map.entry("uitzicht", Plekken.PROP),
            Map.entry("kamp", Plekken.ZONE), Map.entry("appa_kamp", Plekken.PROP), Map.entry("team_aang", Plekken.NPC),
            Map.entry("team_momo", Plekken.NPC), Map.entry("team_katara", Plekken.NPC), Map.entry("team_sokka", Plekken.NPC),
            Map.entry("team_toph", Plekken.NPC), Map.entry("team_zuka", Plekken.NPC), Map.entry("rustpunt*3", Plekken.PROP),
            Map.entry("ijsmuur_poort", Plekken.PROP), Map.entry("poortwachter", Plekken.NPC), Map.entry("ijsmuur_top", Plekken.ZONE),
            Map.entry("gracht_route", Plekken.PAD), Map.entry("gondel_start", Plekken.PROP), Map.entry("gondel_halte*3", Plekken.PROP),
            Map.entry("bewoner*6", Plekken.NPC), Map.entry("trainingsplein", Plekken.ZONE), Map.entry("pakguh", Plekken.NPC),
            Map.entry("katara", Plekken.NPC), Map.entry("sokka", Plekken.NPC), Map.entry("zweep_doel*5", Plekken.PROP),
            Map.entry("golf_bassin", Plekken.ZONE), Map.entry("ijsbrug_a", Plekken.PROP), Map.entry("ijsbrug_b", Plekken.PROP),
            Map.entry("paleis", Plekken.ZONE), Map.entry("hoofdman", Plekken.NPC), Map.entry("yue", Plekken.NPC),
            Map.entry("geestenoase", Plekken.ZONE), Map.entry("oase_poort", Plekken.PROP), Map.entry("vis_maan", Plekken.PROP),
            Map.entry("vis_oceaan", Plekken.PROP), Map.entry("yue_maan", Plekken.PROP), Map.entry("slee_start", Plekken.PROP),
            Map.entry("slee_baan", Plekken.PAD), Map.entry("slee_finish", Plekken.PROP), Map.entry("otter*4", Plekken.WEZEN),
            Map.entry("haven", Plekken.ZONE), Map.entry("zhao_schip", Plekken.ZONE), Map.entry("zhao", Plekken.NPC),
            Map.entry("sluip_start", Plekken.PROP), Map.entry("sluip_route", Plekken.PAD), Map.entry("sluip_wacht*5", Plekken.NPC),
            Map.entry("maanvis_kooi", Plekken.PROP), Map.entry("golf_plek", Plekken.PROP), Map.entry("vloot", Plekken.ZONE),
            Map.entry("vloot_anker", Plekken.PROP), Map.entry("knipoog_sjokkel", Plekken.PAD), Map.entry("knipoog_sjokkel_camera", Plekken.PAD),
            Map.entry("knipoog_balto_route", Plekken.PAD), Map.entry("knipoog_balto_stop", Plekken.NPC), Map.entry("knipoog_balto_camera", Plekken.PAD));

    /** The Kind names the contract reserves for the characters of this chapter. */
    private static final Map<String, String> KINDS = Map.of("poortwachter", "WATERSTAM_WACHTER", "bewoner_1", "WATERSTAM_GUH", "pakguh", "PAKGUH",
            "katara", "KATARA_GUH", "sokka", "SOKKA_GUH", "hoofdman", "ARNOOK_GUH", "yue", "YUE_GUH", "zhao", "ZHAO_MIKA", "sluip_wacht_1", "VUURNATIE_MIKA");

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /**
     * Placement (build contract 6.10): for three seeds the one guaranteed copy exists, lies in its ring, in the high band
     * and within reach of its start chunk, with every tile, marker and overlay inside its box; and it stands above a streek
     * with blue-green water. Its size keeps to the limits of the type and its rim keeps 48 blocks free for the veil.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BouwWaterPlaatsing(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, NAAM);
        LeegteBouwStructure bouw = Plekken.leegteBouw(helper.getLevel().registryAccess(), NAAM);
        helper.assertTrue(bouw != null, "noordelijke_waterstam is a guhs:leegte_bouw");
        Vec3i size = bouw.size();
        helper.assertTrue(size.getX() <= 256 && size.getZ() <= 256 && size.getY() <= 120, "within the limits of the type: " + size);
        helper.assertTrue(bouw.y() >= Einde2Ids.HOOG_MIN && bouw.y() + size.getY() - 1 <= Einde2Ids.HOOG_MAX, "in the high band: y " + bouw.y());
        helper.assertTrue(bouw.rand() == 48, "a story structure keeps 48 blocks free around its box");
        helper.assertTrue(bouw.tilesX() == (size.getX() + 31) / 32 && bouw.tilesZ() == (size.getZ() + 31) / 32, "its tiles cover its size");
        for (long seed : Einde2Test.SEEDS) {
            Einde2Test.Wereld w = Einde2Test.wereld(helper, seed);
            var chunk = Einde2Test.gegarandeerd(w, NAAM);
            helper.assertTrue(chunk != null, "a guaranteed copy for seed " + seed);
            var biome = Einde2Test.biome(w, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
            helper.assertTrue(biome.is(Einde2Ids.KORSTWOESTIJN) || biome.is(Einde2Ids.SCHIMMELKAASVLAKTE),
                    "the sea of the Waterstam is blue: it stands above the Korstwoestijn or the Schimmelkaasvlakte (seed " + seed + "): " + biome);
        }
        helper.assertTrue(Einde2Ids.VERHAAL_STRUCTUREN.indexOf(NAAM) == 1, "the second chapter of the Guhvatar");
        helper.succeed();
    }

    /** The sidecar: every marker of the build contract (6.6, 6.7) is there, of the right kind, and they agree with each other. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwWaterPlekken(GameTestHelper helper) {
        Plekken.Blad blad = Plekken.blad(NAAM);
        Map<String, Plekken.Plek> p = blad.plekken();
        helper.assertTrue(blad.soort().equals("leegte_bouw") && !p.isEmpty(), "the sidecar of the Waterstam");
        for (Map.Entry<String, String> e : PLEKKEN.entrySet()) {
            String id = e.getKey();
            if (id.contains("*")) {
                String naam = id.substring(0, id.indexOf('*'));
                int nodig = Integer.parseInt(id.substring(id.indexOf('*') + 1)), n = 0;
                while (p.containsKey(naam + "_" + (n + 1))) {
                    n++;
                    helper.assertTrue(p.get(naam + "_" + n).soort().equals(e.getValue()), naam + "_" + n + " is a " + e.getValue());
                }
                helper.assertTrue(n >= nodig, "at least " + nodig + " of " + naam + "_<n>: " + n);
            } else {
                helper.assertTrue(p.containsKey(id) && p.get(id).soort().equals(e.getValue()), id + " is a " + e.getValue() + ": " + p.get(id));
            }
        }
        for (Map.Entry<String, String> e : KINDS.entrySet()) {
            helper.assertTrue(e.getValue().equals(p.get(e.getKey()).kind()), e.getKey() + " is reserved for " + e.getValue());
        }
        for (Plekken.Plek plek : p.values()) {
            helper.assertTrue(!plek.soort().equals(Plekken.WEZEN) || "guhs:otter_pinguinguh".equals(plek.entity()), "only otters live here: " + plek);
        }
        // the landing place is big enough, the team's camp lies beside it and the team stands in it
        Plekken.Plek aankomst = p.get("aankomst"), kamp = p.get("kamp");
        helper.assertTrue(span(aankomst, 0) >= 9 && span(aankomst, 2) >= 9, "the landing place is at least 9 x 9");
        helper.assertTrue(in(aankomst, p.get("aankomst_speler").pos()), "you arrive on the landing place");
        for (String wie : List.of("team_aang", "team_momo", "team_katara", "team_sokka", "team_toph", "team_zuka", "appa_kamp")) {
            helper.assertTrue(in(kamp, p.get(wie).pos()), wie + " is in the camp");
        }
        helper.assertTrue(Math.abs(kamp.pos().getX() - aankomst.tot().getX()) <= 8, "the camp lies next to the landing place");
        // the water: the canal loop is a closed round on one level, the fleet has its room at that same level, the overlay lies in it
        Plekken.Plek gracht = p.get("gracht_route"), vloot = p.get("vloot");
        List<BlockPos> ronde = gracht.punten();
        helper.assertTrue(ronde.size() >= 5 && ronde.get(0).equals(ronde.get(ronde.size() - 1)), "the canal route is a loop");
        int zee = ronde.get(0).getY();
        helper.assertTrue(ronde.stream().allMatch(q -> q.getY() == zee), "the canal loop has no step");
        helper.assertTrue(span(vloot, 0) >= 96 && span(vloot, 2) >= 48 && vloot.pos().getY() == zee + 1, "room for the fleet: at least 96 x 48 on the sea");
        Plekken.Laag laag = blad.lagen().get("vloot");
        helper.assertTrue(laag != null && laag.template().equals(Guhs.id(NAAM + "/laag_vloot")) && laag.pos().equals(p.get("vloot_anker").pos()),
                "the overlay of the fleet starts at vloot_anker: " + laag);
        helper.assertTrue(in(vloot, laag.pos()) && in(vloot, laag.pos().offset(laag.grootte().getX() - 1, 0, laag.grootte().getZ() - 1))
                && laag.pos().getY() == zee + 1, "the fleet lies on the water, inside its zone");
        // the wall: you stand high above the sea for the great wave, the sled run starts up there and ends at the water line
        Plekken.Plek top = p.get("ijsmuur_top");
        helper.assertTrue(top.pos().getY() >= zee + 15 && p.get("golf_plek").pos().getY() > top.pos().getY(), "the wave is made from the top of the wall");
        List<BlockPos> baan = p.get("slee_baan").punten();
        helper.assertTrue(baan.size() >= 8 && baan.get(0).getY() == p.get("slee_start").pos().getY() && baan.get(baan.size() - 1).getY() == zee + 1,
                "the sled run goes from the wall top down to the shelf");
        for (int i = 1; i < baan.size(); i++) {
            helper.assertTrue(baan.get(i).getY() <= baan.get(i - 1).getY(), "the sled run never climbs (point " + i + ")");
        }
        helper.assertTrue(p.get("otter_1").pos().getY() == p.get("slee_start").pos().getY() && p.get("otter_1").pos().distManhattan(p.get("slee_start").pos()) <= 6,
                "the otters wait at the start");
        // the oasis: both fish spots in it, the palace above the training square, the square above the canals
        Plekken.Plek oase = p.get("geestenoase");
        helper.assertTrue(in(oase, p.get("vis_maan").pos()) && in(oase, p.get("vis_oceaan").pos()) && in(oase, p.get("yue_maan").pos())
                && !p.get("vis_maan").pos().equals(p.get("vis_oceaan").pos()), "two fish spots in the pond of the oasis");
        helper.assertTrue(p.get("hoofdman").pos().getY() > p.get("pakguh").pos().getY() && p.get("pakguh").pos().getY() > zee + 1,
                "three tiers: the canals, the training square, the palace");
        helper.assertTrue(in(p.get("paleis"), p.get("hoofdman").pos()) && in(p.get("paleis"), p.get("yue").pos()), "the hoofdman and Yue-guh are in the palace");
        helper.assertTrue(in(p.get("trainingsplein"), p.get("pakguh").pos()) && in(p.get("trainingsplein"), p.get("knipoog_balto_stop").pos()),
                "Pakguh teaches on the square, and Baltoguh stops on it");
        // the ship: Zhao, his guards and the cage are on board; the sneak route starts ashore and ends on board
        Plekken.Plek schip = p.get("zhao_schip");
        for (String wie : List.of("zhao", "sluip_wacht_1", "sluip_wacht_2", "sluip_wacht_3", "sluip_wacht_4", "sluip_wacht_5", "maanvis_kooi")) {
            helper.assertTrue(in(schip, p.get(wie).pos()), wie + " is on the ship");
        }
        List<BlockPos> sluip = p.get("sluip_route").punten();
        helper.assertTrue(sluip.get(0).equals(p.get("sluip_start").pos()) && !in(schip, sluip.get(0)) && in(schip, sluip.get(sluip.size() - 1)),
                "the sneak route runs from the harbour onto the ship");
        helper.succeed();
    }

    private static int span(Plekken.Plek zone, int as) {
        return as == 0 ? zone.tot().getX() - zone.pos().getX() + 1 : zone.tot().getZ() - zone.pos().getZ() + 1;
    }

    private static boolean in(Plekken.Plek zone, BlockPos q) {
        return BoundingBox.fromCorners(zone.pos(), zone.tot()).isInside(q);
    }

    /**
     * The templates: every tile loads and fits its 32 x 32 column, together they hold the ground, the water and the blocks
     * the build is made of (so no block id went missing on the way), the seven otters with their home range, and the
     * overlay of the fleet holds blocks only.
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 1200)
    public static void einde2BouwWaterTegels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LeegteBouwStructure bouw = Plekken.leegteBouw(level.registryAccess(), NAAM);
        helper.assertTrue(bouw != null, "noordelijke_waterstam is a guhs:leegte_bouw");
        Map<String, Integer> blokken = new HashMap<>();
        int tegels = 0, otters = 0;
        for (int i = 0; i < bouw.tilesX(); i++) {
            for (int j = 0; j < bouw.tilesZ(); j++) {
                var template = level.getStructureManager().get(bouw.tile(i, j));
                if (template.isEmpty()) {
                    continue;
                }
                tegels++;
                Vec3i size = template.get().getSize();
                helper.assertTrue(size.getX() <= 32 && size.getZ() <= 32 && size.getY() == bouw.size().getY(), "tile " + i + "," + j + " is a column of the build: " + size);
                CompoundTag tag = template.get().save(new CompoundTag());
                tel(tag, blokken);
                for (var e : tag.getListOrEmpty("entities")) {
                    CompoundTag nbt = ((CompoundTag) e).getCompoundOrEmpty("nbt");
                    helper.assertTrue("guhs:otter_pinguinguh".equals(nbt.getStringOr("id", "")) && nbt.getIntOr("Honk", 0) > 0
                            && nbt.getBooleanOr("PersistenceRequired", false), "an otter that stays near its spot: " + nbt);
                    otters++;
                }
            }
        }
        helper.assertTrue(tegels >= 40, "the build is saved in tiles: " + tegels);
        helper.assertTrue(otters == 7, "four otters at the start of the sled run, three in their cove: " + otters);
        for (String id : List.of("guhs:einde2_bouw_water_ijssteen", "guhs:einde2_bouw_water_ijssteen_trap", "guhs:einde2_bouw_water_ijssteen_plaat",
                "guhs:einde2_bouw_water_ijslantaarn", "guhs:einde2_bouw_water_stamteken", "guhs:einde2_bouw_water_vuurnatie_teken", "guhs:roosterijzer",
                "guhs:gepolijst_roosterijzer", "guhs:roosterijzer_tralies", "guhs:houtskoolsteen_stenen", "guhs:gloeikool", "guhs:knuffelbad_badtegel",
                "guhs:guh_waterlelie", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:snow_block", "minecraft:moss_block", "minecraft:water")) {
            helper.assertTrue(blokken.getOrDefault(id, 0) > 0, "the build holds " + id);
        }
        helper.assertTrue(blokken.get("minecraft:water") > 40000, "a sea and canals: " + blokken.get("minecraft:water") + " blocks of water");
        helper.assertTrue(blokken.get("guhs:einde2_bouw_water_ijssteen") > 20000, "a city of ice bricks: " + blokken.get("guhs:einde2_bouw_water_ijssteen"));
        helper.assertTrue(!blokken.containsKey("minecraft:ice") && !blokken.containsKey("minecraft:snow"), "nothing that melts next to a lantern");
        // the fleet: blocks only (a later phase puts it there and takes it away again as air), the emblem on every ship
        StructureTemplate vloot = level.getStructureManager().get(Guhs.id(NAAM + "/laag_vloot")).orElse(null);
        helper.assertTrue(vloot != null, "the overlay of the fleet exists");
        CompoundTag tag = vloot.save(new CompoundTag());
        Map<String, Integer> schepen = new HashMap<>();
        tel(tag, schepen);
        helper.assertTrue(tag.getListOrEmpty("entities").isEmpty(), "no entities in an overlay");
        for (var blok : tag.getListOrEmpty("blocks")) {
            helper.assertTrue(!((CompoundTag) blok).contains("nbt"), "no block entities in an overlay: " + blok);
        }
        helper.assertTrue(schepen.getOrDefault("guhs:einde2_bouw_water_vuurnatie_teken", 0) >= 6 && !schepen.containsKey("minecraft:water")
                && !schepen.containsKey("minecraft:air"), "three ships with the emblem on both sides, and nothing but ship: " + schepen.keySet());
        helper.succeed();
    }

    /** Adds up the blocks of a saved template by block id. */
    private static void tel(CompoundTag tag, Map<String, Integer> totaal) {
        ListTag palette = tag.getListOrEmpty("palette");
        for (var blok : tag.getListOrEmpty("blocks")) {
            String naam = palette.getCompoundOrEmpty(((CompoundTag) blok).getIntOr("state", 0)).getStringOr("Name", "");
            totaal.merge(naam, 1, Integer::sum);
        }
    }

    /**
     * Our six blocks: ice bricks you do not slip on (with their stairs and slab), the lantern that gives full light, the two
     * emblems; each has its item. And in a protected place a survival player cannot take any of them away.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void einde2BouwWaterBlokken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Set<Identifier> gezien = new HashSet<>();
        for (int i = 0; i < E2BouwWaterFeature.BLOCK_ITEMS.size(); i++) {
            var blok = E2BouwWaterFeature.BLOCK_ITEMS.get(i);
            BlockState state = helper.getBlockState(new BlockPos(1 + i, 2, 1));
            helper.assertTrue(state.is(blok.get()), "block " + i + " of the test room is " + blok.getId() + ": " + state);
            helper.assertTrue(blok.get().asItem() != Items.AIR, blok.getId() + " has an item");
            helper.assertTrue(blok.get().getFriction() < 0.7f, blok.getId() + " is not slippery");
            gezien.add(blok.getId());
        }
        helper.assertTrue(gezien.size() == 6 && gezien.stream().allMatch(id -> id.getPath().startsWith("einde2_bouw_water_")), "six blocks, all with the slice's prefix");
        helper.assertTrue(E2BouwWaterFeature.IJSLANTAARN.get().defaultBlockState().getLightEmission() == 15, "the ijslantaarn gives full light");
        helper.assertTrue(E2BouwWaterFeature.IJSSTEEN.get().defaultBlockState().getLightEmission() == 0
                && !E2BouwWaterFeature.IJSSTEEN.get().defaultBlockState().isRandomlyTicking(), "ice bricks do not glow and never melt");
        helper.assertTrue(E2BouwWaterFeature.IJSSTEEN_TRAP.get() instanceof StairBlock && E2BouwWaterFeature.IJSSTEEN_PLAAT.get() instanceof SlabBlock,
                "stairs and a slab of ice bricks");
        helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 5)).is(Blocks.WATER) && helper.getBlockState(new BlockPos(3, 1, 5)).getFluidState().isSource(),
                "the canal of the test room holds still water");
        // protected like the real place: nothing breaks, for a survival player
        BlockPos lantaarn = helper.absolutePos(new BlockPos(4, 2, 1));
        BoundingBox box = BoundingBox.fromCorners(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(8, 4, 8)));
        Einde2Bescherming.TEST_AREAS.add(box);
        ServerPlayer player = speler(helper);
        try {
            helper.assertTrue(Einde2Bescherming.beschermd(level, lantaarn), "the room is protected");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, lantaarn, level.getBlockState(lantaarn), player)).isCanceled(),
                    "an ijslantaarn of a protected place stays where it is");
        } finally {
            Einde2Bescherming.TEST_AREAS.remove(box);
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }
}
