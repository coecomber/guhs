package nl.juiced.guhs.feature.wereld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.spiesburcht.BurchtStructure;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * bbq2 (F3 wereld): the helpers of feature/wereld. The test server has none of our dimensions, so a copy of a structure is
 * a structure start made by hand around a template of this module ({@link Kopieen#test}): Bezetting (an NPC that is missing
 * comes, one that came with the template is taken over, a prop only where nothing was built, once), Bescherming (the pieces
 * + rim, the exception, creative, machines), Herstel (a block and a template come back), QuestRol (per-player steps, the
 * "bring me" step, once-only rewards) and the coordinates of a guhs:burcht.
 * Template wereld_test_kamer: 24 x 24 houtskoolsteen at y 0 (the floor is at helper y 1, things stand at helper y 2).
 */
public class WereldGameTests {
    private static final String KAMER = "wereld_test_kamer";
    private static final String BATCH = "wereld";
    /** The structures the test copies pretend to be (real ones of the registries; no other test has copies of them). */
    private static final String BEWOOND = "guh_fossil", BEWAAKT = "quartz_statue";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /**
     * A copy of {@code guhs:<structuur>}: one jigsaw piece of the template wereld_test_gebouw (7 x 5 x 7) with its corner at
     * this helper position, turned.
     */
    private static StructureStart kopie(GameTestHelper helper, String structuur, BlockPos hoek, Rotation draai) {
        ServerLevel level = helper.getLevel();
        Structure structure = Kopieen.structuur(level, structuur);
        helper.assertTrue(structure != null, "the structure guhs:" + structuur + " exists");
        BlockPos pos = helper.absolutePos(hoek);
        StructurePoolElement element = StructurePoolElement.single("guhs:wereld_test_gebouw").apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = element.getBoundingBox(level.getStructureManager(), pos, draai);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, pos, 0, draai, box, LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(pos), 0, new PiecesContainer(List.of(piece)));
        Kopieen.test(level, start);
        return start;
    }

    private static List<Entity> metTag(GameTestHelper helper, String tag) {
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(24, 8, 24);
        return helper.getLevel().getEntitiesOfClass(Entity.class, kamer, e -> e.isAlive() && e.getPersistentData().getStringOr(Bezetting.TAG, "").equals(tag));
    }

    // =================================================================================================================
    // Bezetting
    // =================================================================================================================

    /**
     * A quest NPC that is registered for a structure comes to a copy that doesn't have it (seen missing twice), at its spot
     * of the (turned) template, with its plek and tag; it is not made twice, comes back when it is gone, and one that came
     * with the template (the bare tag) is taken over instead of doubled.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void wereldBezettingNpc(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String id = "wereld_test_npc";
        try {
            StructureStart start = kopie(helper, BEWOOND, new BlockPos(15, 2, 9), Rotation.CLOCKWISE_90);
            BlockPos bij = helper.absolutePos(new BlockPos(12, 2, 12));
            helper.assertTrue(Bezetting.start(level, BEWOOND, bij) == start, "the copy is found from nearby");
            helper.assertTrue(Bezetting.start(level, BEWOOND, bij.offset(200, 0, 0)) == null, "not from far away");
            BlockPos plek = Bezetting.wereld(start, "wereld_test_gebouw", new BlockPos(3, 1, 3));
            // (turned a quarter: template +x is world +z, template +z is world -x)
            helper.assertTrue(plek != null && plek.equals(helper.absolutePos(new BlockPos(15 - 3, 3, 9 + 3))), "template -> world, turned: " + plek);
            helper.assertTrue(Bezetting.wereld(start, null, new BlockPos(3, 1, 3)).equals(plek), "null = the start piece");
            helper.assertTrue(Bezetting.wereld(start, "bestaat_niet", BlockPos.ZERO) == null, "no such piece in this copy");

            Bezetting.npc(id, BEWOOND, "wereld_test_gebouw", new BlockPos(3, 1, 3), GuhNpcEntity.Kind.WACHTERGUH, "testplek");
            String tag = Bezetting.tag(id, start);
            helper.assertTrue(Bezetting.controleer(level, bij) == 0 && metTag(helper, tag).isEmpty(), "missing once: nothing yet");
            Bezetting.zetGemist(level, id, start, Bezetting.BEVESTIG + 1);
            helper.assertTrue(Bezetting.controleer(level, bij) == 1, "missing twice: made");
            List<Entity> er = metTag(helper, tag);
            helper.assertTrue(er.size() == 1 && er.get(0) instanceof GuhNpcEntity, "one NPC with the tag of this copy");
            GuhNpcEntity npc = (GuhNpcEntity) er.get(0);
            helper.assertTrue(npc.getKind() == GuhNpcEntity.Kind.WACHTERGUH && npc.blockPosition().equals(plek) && npc.isInvulnerable()
                    && "testplek".equals(npc.roleData.getStringOr(NpcRollen.PLEK, "")) && Bezetting.isBezetting(npc), "the right NPC at the right spot");
            Bezetting.zetGemist(level, id, start, Bezetting.BEVESTIG + 1);
            helper.assertTrue(Bezetting.controleer(level, bij) == 0 && metTag(helper, tag).size() == 1, "it is there: no second one");
            npc.discard();
            helper.assertTrue(Bezetting.controleer(level, bij) == 0, "gone: seen missing once");
            Bezetting.bevestigAlles(level);
            helper.assertTrue(Bezetting.controleer(level, bij) == 1 && metTag(helper, tag).size() == 1, "and it comes back");
            metTag(helper, tag).forEach(Entity::discard);

            // one that came with the template (worldgen): the bare tag
            GuhNpcEntity sjabloon = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
            sjabloon.setKind(GuhNpcEntity.Kind.WACHTERGUH);
            sjabloon.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 0f, 0f);
            sjabloon.getPersistentData().putString(Bezetting.TAG, id);
            level.addFreshEntity(sjabloon);
            Bezetting.zetGemist(level, id, start, Bezetting.BEVESTIG + 1);
            helper.assertTrue(Bezetting.controleer(level, bij) == 0 && metTag(helper, tag).size() == 1 && metTag(helper, tag).get(0) == sjabloon
                    && sjabloon.isInvulnerable(), "the template's own NPC is this copy's: no double");
            sjabloon.discard();

            // any other entity, by its maker
            Bezetting.vergeet(id);
            Bezetting.wezen(id, BEWOOND, null, new BlockPos(3, 1, 3), (l, p, d) -> {
                Entity e = EntityType.ARMOR_STAND.create(l, EntitySpawnReason.STRUCTURE);
                e.snapTo(p.x, p.y, p.z, 0f, 0f);
                return e;
            });
            Bezetting.controleer(level, bij);
            Bezetting.bevestigAlles(level);
            helper.assertTrue(Bezetting.controleer(level, bij) == 1 && metTag(helper, tag).size() == 1
                    && metTag(helper, tag).get(0).getType() == EntityType.ARMOR_STAND, "a wezen from its maker");
            metTag(helper, tag).forEach(Entity::discard);
        } finally {
            Bezetting.vergeet(id);
            Kopieen.testWissen(level);
        }
        helper.succeed();
    }

    /**
     * A prop goes at the first of its spots where nothing was built (never over a player's block or a block entity), sunk
     * into natural ground at most one layer, once per copy; a copy where no spot is free goes without.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void wereldBezettingProp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String id = "wereld_test_prop";
        try {
            StructureStart start = kopie(helper, BEWOOND, new BlockPos(2, 2, 2), Rotation.NONE);
            BlockPos bij = helper.absolutePos(new BlockPos(5, 2, 5));
            // first choice: a player built a chest there; second: a cobblestone wall; third: free (on the floor)
            BlockPos eerste = new BlockPos(10, 0, 0), tweede = new BlockPos(10, 0, 6), derde = new BlockPos(0, 0, 12);
            helper.setBlock(new BlockPos(2 + 10 + 1, 3, 2 + 1), Blocks.CHEST);
            helper.setBlock(new BlockPos(2 + 10 + 1, 3, 2 + 6 + 1), Blocks.COBBLESTONE);
            Bezetting.blokken(id, BEWOOND, null, List.of(eerste, tweede, derde), Guhs.id("wereld_test_prop"));
            Bezetting.Geplaatst data = Bezetting.Geplaatst.get(level);
            data.vergeet(id, start);
            helper.assertTrue(!data.gehad(id, start), "not placed yet");
            helper.assertTrue(Bezetting.controleer(level, bij) == 1, "placed");
            BlockPos hoek = data.plek(id, start).orElse(null);
            helper.assertTrue(hoek != null && hoek.equals(helper.absolutePos(new BlockPos(2, 2, 2 + 12))), "at the third spot: " + hoek);
            helper.assertBlockPresent(Blocks.RED_WOOL, new BlockPos(2, 2, 14));
            helper.assertBlockPresent(Blocks.LANTERN, new BlockPos(3, 3, 15));
            helper.assertBlockPresent(Blocks.CHEST, new BlockPos(13, 3, 3));
            helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(13, 3, 9));
            helper.assertTrue(helper.getBlockState(new BlockPos(12, 2, 2)).isAir() && helper.getBlockState(new BlockPos(12, 2, 8)).isAir(),
                    "nothing where a player built");
            helper.assertTrue(Bezetting.controleer(level, bij) == 0 && data.gehad(id, start), "once per copy");

            // sunk one layer into natural ground: fine; into anything else: not
            helper.assertTrue(Bezetting.vrij(Blocks.AIR.defaultBlockState()) && Bezetting.vrij(Blocks.PINK_WOOL.defaultBlockState())
                    && !Bezetting.leeg(Blocks.PINK_WOOL.defaultBlockState()) && !Bezetting.vrij(Blocks.COBBLESTONE.defaultBlockState())
                    && !Bezetting.vrij(Blocks.WATER.defaultBlockState()) && Bezetting.leeg(Blocks.SHORT_GRASS.defaultBlockState()),
                    "what a prop may replace");

            // a copy with no free spot: remembered, nothing placed
            data.vergeet(id, start);
            Bezetting.blokken(id, BEWOOND, null, List.of(eerste), Guhs.id("wereld_test_prop"));
            helper.assertTrue(Bezetting.controleer(level, bij) == 0 && data.gehad(id, start) && data.plek(id, start).isEmpty(), "no spot: goes without");
            data.vergeet(id, start);
        } finally {
            Bezetting.vergeet(id);
            Kopieen.testWissen(level);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // Bescherming
    // =================================================================================================================

    /**
     * The pieces of a protected structure (+ its rim) can't be broken or built in by a survival player; the quest exception
     * and creative mode can; machines may not change it; outside it everything is as usual.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void wereldBescherming(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        try {
            kopie(helper, BEWAAKT, new BlockPos(8, 2, 8), Rotation.NONE);
            BlockPos binnen = helper.absolutePos(new BlockPos(10, 2, 10)), rand = helper.absolutePos(new BlockPos(6, 2, 10));
            BlockPos buiten = helper.absolutePos(new BlockPos(5, 2, 10));
            helper.assertTrue(!Bescherming.beschermd(level, binnen), "not registered: free");
            Bescherming.registreer(BEWAAKT, 2);
            helper.assertTrue(Bescherming.beschermd(level, binnen) && Bescherming.beschermd(level, rand) && !Bescherming.beschermd(level, buiten),
                    "the piece and two blocks around it");
            helper.assertTrue(BEWAAKT.equals(Bescherming.structuurBij(level, binnen)) && Bescherming.structuurBij(level, buiten) == null, "which structure");
            helper.assertTrue(!Bescherming.mag(p, binnen) && Bescherming.mag(p, buiten), "a survival player may not change it");
            helper.assertTrue(!Bescherming.magWijzigen(level, binnen, p.getUUID()) && Bescherming.magWijzigen(level, buiten, p.getUUID())
                    && !Bescherming.magWijzigen(level, binnen, null), "machines neither");
            // really breaking: the floor block of the building stays, one outside goes
            level.setBlockAndUpdate(binnen, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            level.setBlockAndUpdate(buiten, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
            p.gameMode.destroyBlock(binnen);
            p.gameMode.destroyBlock(buiten);
            helper.assertTrue(level.getBlockState(binnen).is(Blocks.POLISHED_BLACKSTONE_BRICKS) && level.getBlockState(buiten).isAir(),
                    "breaking is refused inside only");
            // the quest exception: this very block, for this very player
            Bescherming.uitzondering(BEWAAKT, (speler, pos) -> speler == p && pos.equals(binnen));
            helper.assertTrue(Bescherming.mag(p, binnen) && !Bescherming.mag(p, binnen.above()), "the exception");
            p.gameMode.destroyBlock(binnen);
            helper.assertTrue(level.getBlockState(binnen).isAir(), "the quest block can be broken");
            p.setGameMode(GameType.CREATIVE);
            helper.assertTrue(Bescherming.mag(p, binnen.above()), "creative mode may change it");
        } finally {
            Bescherming.vergeet(BEWAAKT);
            Kopieen.testWissen(level);
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // Herstel
    // =================================================================================================================

    /** A block and a small template come back after their time (and not before); a second request replaces the first. */
    @GuhTest(template = KAMER, batch = "wereld_herstel")
    public static void wereldHerstel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4)), hoek = helper.absolutePos(new BlockPos(10, 2, 10));
        BlockState lantaarn = Blocks.LANTERN.defaultBlockState();
        level.setBlockAndUpdate(pos, lantaarn);
        int eerst = Herstel.aantal(level);
        Herstel.na(level, pos, lantaarn, 1200);
        Herstel.na(level, pos, lantaarn, 600);                           // (the same spot: replaces)
        Herstel.na(level, hoek, Guhs.id("wereld_test_prop"), Rotation.NONE, 600);
        helper.assertTrue(Herstel.aantal(level) == eerst + 2 && Herstel.wacht(level, pos) && Herstel.wacht(level, hoek) && !Herstel.wacht(level, pos.above()),
                "two requests wait");
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());    // (a player took the lantern for a quest)
        Herstel.verwerk(level);
        helper.assertTrue(level.getBlockState(pos).isAir() && Herstel.wacht(level, pos), "not yet");
        Herstel.verschuif(level, 600);
        Herstel.verwerk(level);
        helper.assertTrue(level.getBlockState(pos) == lantaarn && !Herstel.wacht(level, pos), "the lantern is back");
        helper.assertTrue(level.getBlockState(hoek).is(Blocks.RED_WOOL) && level.getBlockState(hoek.offset(1, 1, 1)).is(Blocks.LANTERN)
                && !Herstel.wacht(level, hoek) && Herstel.aantal(level) == eerst, "the template is back");
        helper.succeed();
    }

    // =================================================================================================================
    // QuestRol
    // =================================================================================================================

    /** A questline kept in a map (the story engine's Verhaallijn does this per player in the saved player data). */
    private static final class TestLijn implements Stappen {
        final Map<java.util.UUID, Integer> stap = new HashMap<>();
        final Set<String> gehad = new HashSet<>();

        @Override
        public int stap(ServerPlayer p) {
            return stap.getOrDefault(p.getUUID(), 0);
        }

        @Override
        public boolean verder(ServerPlayer p, int vanStap) {
            if (stap(p) != vanStap) {
                return false;
            }
            stap.put(p.getUUID(), vanStap + 1);
            return true;
        }

        @Override
        public boolean eenmalig(ServerPlayer p, String naam) {
            return gehad.add(p.getUUID() + "/" + naam);
        }
    }

    /**
     * One NPC, two players: each has their own step. The "bring me" step takes the items and goes on, the reward comes once
     * per player, and the answer of the talking screen arrives with the player's step.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void wereldQuestRol(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(2, 2, 2)), b = speler(helper, new BlockPos(3, 2, 2));
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        try {
            npc.setKind(GuhNpcEntity.Kind.WACHTERGUH);
            BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
            npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(npc);
            TestLijn lijn = new TestLijn();
            int[] antwoorden = {0, 0};
            QuestRol rol = new QuestRol(lijn) {
                @Override
                protected void praat(GuhNpcEntity n, ServerPlayer p, int stap) {
                    switch (stap) {
                        case 0 -> verder(p, 0);
                        case 1 -> lever(p, n, 1, Items.COAL, 4, "quest.guhs.next");
                        case 2 -> scherm(p, n, "quest.guhs.next", new Praat.Optie(1, "quest.guhs.next"));
                        default -> {
                        }
                    }
                }

                @Override
                protected void antwoord(GuhNpcEntity n, ServerPlayer p, int stap, int optie) {
                    antwoorden[0] = stap;
                    antwoorden[1] = optie;
                    if (stap == 2 && optie == 1 && verder(p, 2)) {
                        geefEenmalig(p, "beloning", new ItemStack(Items.DIAMOND, 2));
                    }
                }
            };
            helper.assertTrue(rol.lijn() == lijn, "the questline");
            rol.talk(npc, a);
            helper.assertTrue(lijn.stap(a) == 1 && lijn.stap(b) == 0, "a began, b didn't");
            rol.talk(npc, a);
            helper.assertTrue(lijn.stap(a) == 1, "no coal: a stays");
            a.getInventory().add(new ItemStack(Items.COAL, 6));
            rol.talk(npc, a);
            helper.assertTrue(lijn.stap(a) == 2 && a.getInventory().countItem(Items.COAL) == 2, "four coal taken, a goes on");
            rol.talk(npc, a);                                            // (the talking screen opens: nothing changes yet)
            helper.assertTrue(lijn.stap(a) == 2, "waits for the answer");
            ((nl.juiced.guhs.feature.NpcRole) rol).antwoord(npc, a, 1);
            helper.assertTrue(antwoorden[0] == 2 && antwoorden[1] == 1 && lijn.stap(a) == 3 && a.getInventory().countItem(Items.DIAMOND) == 2,
                    "the answer came with a's step; done, with the reward");
            ((nl.juiced.guhs.feature.NpcRole) rol).antwoord(npc, a, 1);
            helper.assertTrue(a.getInventory().countItem(Items.DIAMOND) == 2, "the reward only once");
            rol.talk(npc, b);
            helper.assertTrue(lijn.stap(b) == 1 && lijn.stap(a) == 3, "b has a turn of their own at the same NPC");
            // the small helpers
            helper.assertTrue(rol.heeft(a, Items.COAL, 2) && !rol.heeft(a, Items.COAL, 3) && rol.neem(a, Items.COAL, 2) && !rol.neem(a, Items.COAL, 1),
                    "heeft / neem");
            helper.assertTrue(rol.geefAlsKwijt(b, Items.BRUSH) && !rol.geefAlsKwijt(b, Items.BRUSH) && b.getInventory().countItem(Items.BRUSH) == 1,
                    "a quest item comes back when it was lost, not twice");
        } finally {
            npc.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // a guhs:burcht: coordinates of the whole build
    // =================================================================================================================

    /**
     * The tiles of a burcht turn together around its anchor: a spot of the whole build is the same world position whichever
     * tile is asked, and it is where the tile's own template puts it.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void wereldBurchtCoordinaten(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure structure = Kopieen.structuur(level, "spiesburcht");
        helper.assertTrue(structure instanceof BurchtStructure, "the Spiesburcht is a burcht");
        BurchtStructure burcht = (BurchtStructure) structure;
        BlockPos anchor = burcht.anchor(), at = helper.absolutePos(new BlockPos(12, 2, 12));
        int tile = 32;
        for (Rotation draai : Rotation.values()) {
            // (the way BurchtStructure makes its pieces: two tiles, (0,0) and (1,1))
            List<StructurePiece> pieces = new java.util.ArrayList<>();
            for (int i = 0; i <= 1; i++) {
                BlockPos offset = new BlockPos(i * tile, 0, i * tile);
                pieces.add(new BurchtStructure.Piece(level.getStructureManager(), burcht.tile(i, i), at.subtract(anchor).offset(offset), draai, anchor.subtract(offset)));
            }
            for (int eerste = 0; eerste <= 1; eerste++) {
                List<StructurePiece> volgorde = eerste == 0 ? pieces : List.of(pieces.get(1), pieces.get(0));
                StructureStart start = new StructureStart(structure, ChunkPos.containing(at), 0, new PiecesContainer(volgorde));
                helper.assertTrue(at.equals(Kopieen.wereld(start, null, anchor)), "the anchor is the generation point (" + draai + ")");
                BlockPos lokaal = anchor.offset(5, 3, -2);
                BlockPos verwacht = at.offset(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(new BlockPos(5, 3, -2),
                        net.minecraft.world.level.block.Mirror.NONE, draai, BlockPos.ZERO));
                helper.assertTrue(verwacht.equals(Kopieen.wereld(start, "anything", lokaal)), "a spot of the whole build (" + draai + ", tile " + eerste + ")");
                helper.assertTrue(Kopieen.draai(start, null) == draai, "its rotation");
            }
        }
        helper.succeed();
    }
}
