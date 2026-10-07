package nl.juiced.guhs.feature.einde2.bouwlucht;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.LeegteBouwStructure;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the slice bouw_lucht: the Zuidelijke Luchttempel lands where it should (its one guaranteed copy, in the
 * high band, every tile there), its sidecar holds every marker the later phases count on, the closed door of the
 * Werktuigguh lets nobody through but the player the hook names, and the five stages of the Vredesballon go down on top
 * of each other and away again. The build itself is checked by its generator (tools/features/einde2_bouw_lucht_bouw.py:
 * walkable, lit, nothing loose, the workshop shut).
 */
public class E2BouwLuchtGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "einde2_bouw_lucht";
    private static final String TEMPEL = Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL;

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void weg(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    private static Block blok(String id) {
        return BuiltInRegistries.BLOCK.getValue(Guhs.id(id));
    }

    /** The one guaranteed copy for three seeds: in its ring, in the high band, every tile and every marker inside its box. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 2400)
    public static void einde2BouwLuchtPlaatsing(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, TEMPEL);
        LeegteBouwStructure bouw = Plekken.leegteBouw(helper.getLevel().registryAccess(), TEMPEL);
        helper.assertTrue(bouw != null && bouw.rand() == 48, "a story structure keeps 48 blocks around it free (the veil of a later phase)");
        helper.assertTrue(bouw.size().equals(new Vec3i(192, 116, 192)) && bouw.y() == 138 && bouw.tilesX() == 6 && bouw.tilesZ() == 6,
                "192 x 116 x 192 from y 138, in 6 x 6 tiles: " + bouw.size() + " y " + bouw.y());
        var access = helper.getLevel().registryAccess();
        var structuren = access.lookupOrThrow(Registries.STRUCTURE);
        var tempel = structuren.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Guhs.id(TEMPEL)));
        helper.assertTrue(tempel.is(TagKey.create(Registries.STRUCTURE, Guhs.id("einde2/verhaal"))), "it is a story structure (guhs:einde2/verhaal)");
        helper.assertTrue(access.lookupOrThrow(Registries.STRUCTURE_SET).getValue(Guhs.id(TEMPEL)) == null,
                "no random copies: the temple exists exactly once per world");
        helper.assertTrue(Einde2Ids.VERHAAL_STRUCTUREN.contains(TEMPEL), "Einde2 registers its protection with the other story structures");
        Structure s = tempel.value();
        helper.assertTrue(s.spawnOverrides().size() == 2, "nothing spawns by itself inside (empty monster and creature lists)");
        helper.succeed();
    }

    /** The sidecar: every marker of the build contract, of the right kind, and the five overlays on their anchor. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwLuchtPlekken(GameTestHelper helper) {
        Plekken.Blad blad = Plekken.blad(TEMPEL);
        Map<String, Plekken.Plek> p = blad.plekken();
        helper.assertTrue(blad.soort().equals("leegte_bouw") && blad.grootte().equals(new Vec3i(192, 116, 192)) && blad.anker().equals(new BlockPos(88, 0, 88)),
                "the sidecar: " + blad.grootte() + " anchor " + blad.anker());
        Map<String, String> soorten = Map.ofEntries(
                Map.entry("aankomst", Plekken.ZONE), Map.entry("aankomst_speler", Plekken.PROP), Map.entry("uitzicht", Plekken.PROP),
                Map.entry("kamp", Plekken.ZONE), Map.entry("appa_kamp", Plekken.PROP), Map.entry("team_aang", Plekken.NPC),
                Map.entry("team_momo", Plekken.NPC), Map.entry("team_katara", Plekken.NPC), Map.entry("team_sokka", Plekken.NPC),
                Map.entry("team_toph", Plekken.NPC), Map.entry("team_zuka", Plekken.NPC), Map.entry("aang", Plekken.NPC), Map.entry("poort", Plekken.PROP),
                Map.entry("hal", Plekken.ZONE), Map.entry("kyoshi_beeld", Plekken.NPC), Map.entry("roku_beeld", Plekken.NPC),
                Map.entry("voetstuk_leeg", Plekken.PROP), Map.entry("guhatso", Plekken.NPC), Map.entry("taartenbank", Plekken.PROP),
                Map.entry("luchtbal_veld", Plekken.ZONE), Map.entry("luchtbal_doel_speler", Plekken.PROP), Map.entry("luchtbal_doel_aang", Plekken.PROP),
                Map.entry("luchtbal_start_speler", Plekken.PROP), Map.entry("luchtbal_start_aang", Plekken.PROP), Map.entry("scooter_start", Plekken.PROP),
                Map.entry("scooter_finish", Plekken.PROP), Map.entry("scooter_baan", Plekken.PAD), Map.entry("zweef_start", Plekken.PROP),
                Map.entry("appa_stal", Plekken.ZONE), Map.entry("appa", Plekken.PROP), Map.entry("zadel", Plekken.PROP), Map.entry("appels", Plekken.PROP),
                Map.entry("momo", Plekken.NPC), Map.entry("momo_route", Plekken.PAD), Map.entry("momo_eind", Plekken.PROP),
                Map.entry("monnikenverblijf", Plekken.ZONE), Map.entry("bed_speler", Plekken.PROP), Map.entry("werkplaatsdeur", Plekken.PROP),
                Map.entry("werkplaats", Plekken.ZONE), Map.entry("werktuigguh", Plekken.NPC), Map.entry("werkbank", Plekken.PROP),
                Map.entry("vredesballon_bouwplaats", Plekken.ZONE), Map.entry("vredesballon_anker", Plekken.PROP),
                Map.entry("vredesballon_teller", Plekken.PROP), Map.entry("knipoog_sjokkel", Plekken.PAD), Map.entry("knipoog_sjokkel_camera", Plekken.PAD));
        for (Map.Entry<String, String> e : soorten.entrySet()) {
            Plekken.Plek plek = p.get(e.getKey());
            helper.assertTrue(plek != null && plek.soort().equals(e.getValue()), "marker " + e.getKey() + " (" + e.getValue() + "): " + plek);
        }
        for (Map.Entry<String, Integer> e : Map.of("beeld_", 8, "luchtbal_paal_", 12, "zweefring_", 10, "oefen_", 6, "rustpunt_", 3).entrySet()) {
            long n = p.keySet().stream().filter(k -> k.startsWith(e.getKey())).count();
            helper.assertTrue(n >= e.getValue(), "at least " + e.getValue() + " of " + e.getKey() + "*: " + n);
        }
        helper.assertTrue(p.get("oefen_6") != null && p.get("oefen_7") == null, "exactly six practice bays");
        Predicate<String> ring = k -> p.get(k).soort().equals(Plekken.RING) && p.get(k).straal() == 4 && p.get(k).as() != null;
        helper.assertTrue(p.keySet().stream().filter(k -> k.startsWith("zweefring_")).allMatch(ring), "every zweefring is a ring of radius 4 with an axis");
        // the characters a later phase puts there: the reserved kinds, and Momo-guh as the creature he is
        for (Map.Entry<String, String> e : Map.of("aang", "AANG_GUH", "kyoshi_beeld", "KYOSHI_GUH", "roku_beeld", "ROKU_GUH", "guhatso", "GUHATSO",
                "werktuigguh", "WERKTUIGGUH").entrySet()) {
            helper.assertTrue(e.getValue().equals(p.get(e.getKey()).kind()), e.getKey() + " is reserved for " + e.getValue() + ": " + p.get(e.getKey()).kind());
        }
        helper.assertTrue("guhs:momo_guh".equals(p.get("momo").entity()), "Momo-guh's hiding spot names the creature: " + p.get("momo").entity());
        helper.assertTrue(p.get("momo_route").punten().size() >= 12, "the chase has at least twelve points: " + p.get("momo_route").punten().size());
        helper.assertTrue(p.get("knipoog_sjokkel").punten().size() == 2
                        && p.get("knipoog_sjokkel").punten().get(0).distManhattan(p.get("knipoog_sjokkel").punten().get(1)) >= 6,
                "Sjokkel creeps between two points at least six apart");
        long lemurs = p.values().stream().filter(x -> x.soort().equals(Plekken.WEZEN) && "guhs:vleermuis_lemurguh".equals(x.entity())).count();
        helper.assertTrue(lemurs >= 4, "the vleermuis-lemurguhs live here: " + lemurs);
        // the door's marker is the lower middle block of the door; the practice bays and the Werktuigguh lie in the workshop
        Plekken.Plek deur = p.get("werkplaatsdeur"), werk = p.get("werkplaats");
        for (String id : List.of("werktuigguh", "werkbank", "oefen_1", "oefen_2", "oefen_3", "oefen_4", "oefen_5", "oefen_6")) {
            BlockPos at = p.get(id).pos();
            helper.assertTrue(at.getX() >= werk.pos().getX() && at.getX() <= werk.tot().getX() && at.getZ() >= werk.pos().getZ() && at.getZ() <= werk.tot().getZ()
                    && at.getY() >= werk.pos().getY() && at.getY() <= werk.tot().getY(), id + " lies in the workshop: " + at);
        }
        helper.assertTrue(deur.pos().getX() == werk.pos().getX() - 1 && deur.pos().getY() == werk.pos().getY(), "the door stands in the workshop's west wall: " + deur.pos());
        // the five stages of the Vredesballon: on the anchor, all the same size, inside the yard
        helper.assertTrue(blad.lagen().size() == 5, "five overlays: " + blad.lagen().keySet());
        Plekken.Plek anker = p.get("vredesballon_anker"), tuin = p.get("vredesballon_bouwplaats");
        for (int n = 1; n <= 5; n++) {
            Plekken.Laag laag = blad.lagen().get("vredesballon_" + n);
            helper.assertTrue(laag != null && laag.pos().equals(anker.pos()) && laag.grootte().equals(new Vec3i(25, 42, 25))
                    && laag.template().equals(Guhs.id(TEMPEL + "/laag_vredesballon_" + n)), "stage " + n + ": " + laag);
            BlockPos tot = laag.pos().offset(laag.grootte()).offset(-1, -1, -1);
            helper.assertTrue(laag.pos().getX() >= tuin.pos().getX() && tot.getX() <= tuin.tot().getX() && laag.pos().getZ() >= tuin.pos().getZ()
                    && tot.getZ() <= tuin.tot().getZ() && tot.getY() <= tuin.tot().getY(), "stage " + n + " stays inside the yard");
            helper.assertTrue(helper.getLevel().getStructureManager().get(laag.template()).isPresent(), "the template of stage " + n);
        }
        helper.succeed();
    }

    /** The door: a wall for everybody, no wall for the one player the hook names; nobody breaks it. */
    @GuhTest(template = "einde2_bouw_lucht_test_deur", batch = BATCH)
    public static void einde2BouwLuchtDeur(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.is(E2BouwLuchtFeature.WERKPLAATSDEUR.get()), "the test wall is made of door blocks: " + state);
        ServerPlayer a = speler(helper), b = speler(helper);
        Entity mika = EntityType.PIG.create(level, EntitySpawnReason.COMMAND);
        Predicate<Player> oud = Einde2Haken.WERKPLAATS_OPEN;
        try {
            helper.assertTrue(state.getCollisionShape(level, pos, CollisionContext.of(a)) == Shapes.block()
                    && state.getCollisionShape(level, pos, CollisionContext.empty()) == Shapes.block(), "closed for everybody by default");
            helper.assertTrue(!E2BouwLuchtBlokken.Werkplaatsdeur.laatDoor(a) && !E2BouwLuchtBlokken.Werkplaatsdeur.laatDoor(null), "nobody is let through");
            Einde2Haken.WERKPLAATS_OPEN = p -> p == a;
            helper.assertTrue(state.getCollisionShape(level, pos, CollisionContext.of(a)).isEmpty(), "the player the hook names walks through");
            helper.assertTrue(state.getCollisionShape(level, pos, CollisionContext.of(b)) == Shapes.block(), "another player does not");
            helper.assertTrue(mika != null && state.getCollisionShape(level, pos, CollisionContext.of(mika)) == Shapes.block(), "and no creature does");
            helper.assertTrue(state.getShape(level, pos).equals(Shapes.block()), "it still looks like a whole block");
            helper.assertTrue(state.getDestroySpeed(level, pos) < 0 && state.getDestroyProgress(a, level, pos) == 0f, "nobody breaks it");
            helper.assertTrue(!state.isSuffocating(level, pos), "whoever walks through is not smothered");
        } finally {
            Einde2Haken.WERKPLAATS_OPEN = oud;
            weg(helper, a);
            weg(helper, b);
        }
        helper.succeed();
    }

    /** The five stages go down on top of each other (each covers the one before) and all go away again. */
    @GuhTest(template = "einde2_bouw_lucht_test_veld", batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwLuchtVredesballon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Plekken.Blad blad = Plekken.blad(TEMPEL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos mand = at.offset(12, 2, 12), brander = at.offset(12, 4, 12), stapel = at.offset(5, 0, 5), doek = at.offset(12, 26, 21), mast = at.offset(1, 5, 1);
        int vorige = 0;
        for (int n = 1; n <= 5; n++) {
            helper.assertTrue(Plekken.laag(level, blad.lagen().get("vredesballon_" + n), at, false), "stage " + n + " goes down");
            int blokken = 0;
            for (BlockPos q : BlockPos.betweenClosed(at, at.offset(24, 41, 24))) {
                if (!level.getBlockState(q).isAir()) {
                    blokken++;
                }
            }
            helper.assertTrue(blokken > vorige, "stage " + n + " adds to the one before: " + blokken + " after " + vorige);
            vorige = blokken;
            helper.assertTrue(level.getBlockState(mast).is(Blocks.STRIPPED_SPRUCE_LOG) && !level.getBlockState(doek).isAir(), "stage " + n + ": masts and cloth");
            helper.assertTrue(level.getBlockState(mand).is(Blocks.SPRUCE_PLANKS) == (n >= 2), "the basket from stage 2 on (stage " + n + ")");
            helper.assertTrue(level.getBlockState(brander).is(Blocks.WAXED_COPPER_BLOCK) == (n >= 3), "the burner from stage 3 on (stage " + n + ")");
            helper.assertTrue(level.getBlockState(stapel).is(blok("block_of_kaasknabbels")) == (n >= 4), "the knabbels from stage 4 on (stage " + n + ")");
            helper.assertTrue(level.getBlockState(doek).is(Blocks.WHITE_WOOL) != (n >= 5), "the cloth gets its colours in stage 5 (stage " + n + "): "
                    + level.getBlockState(doek));
        }
        helper.assertTrue(Plekken.laag(level, blad.lagen().get("vredesballon_5"), at, true), "the last stage goes away");
        for (BlockPos q : BlockPos.betweenClosed(at, at.offset(24, 41, 24))) {
            helper.assertTrue(level.getBlockState(q).isAir(), "nothing of the balloon is left at " + q + ": " + level.getBlockState(q));
        }
        helper.succeed();
    }

    /** The temple's own blocks exist with their items, and the taart is a low thing you can stand next to. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwLuchtBlokken(GameTestHelper helper) {
        for (var block : E2BouwLuchtFeature.BLOKKEN) {
            helper.assertTrue(block.get().asItem() != net.minecraft.world.item.Items.AIR, "an item for " + block.getId());
        }
        helper.assertTrue(blok("einde2_bouw_lucht_taart").defaultBlockState().getShape(helper.getLevel(), BlockPos.ZERO).max(net.minecraft.core.Direction.Axis.Y) < 0.5,
                "the taart is low");
        helper.assertTrue(blok("einde2_bouw_lucht_pijltegel").defaultBlockState().hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
                && blok("einde2_bouw_lucht_blauwdruk").defaultBlockState().hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING),
                "the arrow tile and the blueprint turn");
        helper.succeed();
    }
}
