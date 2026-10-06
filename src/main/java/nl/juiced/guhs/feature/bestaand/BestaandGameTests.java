package nl.juiced.guhs.feature.bestaand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.spiesburcht.BurchtStructure;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of bbq2 (bestaand): the two questlines in the existing buildings, and above all that everything is per player
 * (the world never changes, two players do the same quest at the same blocks).
 * <ul>
 *   <li>{@link #bestaandBrugvuren}: the Wachter-guh's talk, the loaned aansteekspies, lighting the four fire bowls; a lit
 *       bowl burns only for who lit it and stays out in the world;</li>
 *   <li>{@link #bestaandBurcht} (a fake, turned copy of a Spiesburcht): Bezetting brings the wachthokje and the Wachter-guh;
 *       the weeds only the player at that step sees, pulling them, a spot that was built over, the reward once, a fire bowl
 *       that could not be placed counts as lit;</li>
 *   <li>{@link #bestaandBrugvuurProp}: a bridge fire prop really lands on its spot of a copy and reports in;</li>
 *   <li>{@link #bestaandPaleis} (a fake copy of a grillpaleis): the naaihoek and the Knuffelmaker-guh come; a cage opens
 *       with vads or by sneaking, a Nether-Mika that looks only shoves, the freed plush is gone for that player alone and
 *       sits in the naaihoek, thread, the reward once, a broken cage counts as freed;</li>
 *   <li>{@link #bestaandBlokken}: the Zielig lantaarntje, the plush blocks, the recipes with their cards.</li>
 * </ul>
 * The test server has no Barbecuether: a copy is a structure start made by hand ({@link Kopieen#test}) whose anchor lies
 * on the test room. The tests with a copy each have a batch of their own (copies of one structure must not see each other).
 */
public class BestaandGameTests {
    private static final String BRUG = "bestaand_test_brug", BURCHT = "bestaand_test_burcht", PALEIS = "bestaand_test_paleis";
    private static final String BATCH = "bestaand";
    private static final int HELP = 1, WAT = 2, WIEDEN = 3, DANK = 4;

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        BestaandFeature.WACHTER.wis(p);
        BestaandFeature.KNUFFELMAKER.wis(p);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Praat.vergeet(p);
            Kooien.vergeet(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        GuhNpcEntity n = helper.spawn(ModEntities.GUH_NPC.get(), at);
        n.setKind(kind);
        return n;
    }

    private static int count(ServerPlayer p, Item item) {
        return p.getInventory().countItem(item);
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id(name.contains("/") ? name : "quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** A right-click of this player on this block with what they hold in their main hand, the way the game does it. */
    private static void gebruik(ServerPlayer p, BlockPos pos) {
        BlockState state = p.level().getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        if (state.useItemOn(p.getMainHandItem(), p.level(), p, InteractionHand.MAIN_HAND, hit) == net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND) {
            state.useWithoutItem(p.level(), p, hit);
        }
    }

    /**
     * A copy of the burcht {@code guhs:<structuur>} whose template position {@code lokaal} lies on this helper position,
     * turned: one tile (1, 1), made the way BurchtStructure makes its pieces.
     */
    private static StructureStart kopie(GameTestHelper helper, String structuur, BlockPos lokaal, BlockPos op, Rotation draai) {
        ServerLevel level = helper.getLevel();
        Structure structure = Kopieen.structuur(level, structuur);
        helper.assertTrue(structure instanceof BurchtStructure, "guhs:" + structuur + " is a burcht");
        BurchtStructure burcht = (BurchtStructure) structure;
        BlockPos anchor = burcht.anchor();
        // (world = generation point + turned (lokaal - anchor): the generation point that puts lokaal on `op`)
        BlockPos gedraaid = net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(lokaal.subtract(anchor),
                net.minecraft.world.level.block.Mirror.NONE, draai, BlockPos.ZERO);
        BlockPos at = helper.absolutePos(op).subtract(gedraaid);
        BlockPos offset = new BlockPos(32, 0, 32);
        List<StructurePiece> pieces = new ArrayList<>();
        pieces.add(new BurchtStructure.Piece(level.getStructureManager(), burcht.tile(1, 1), at.subtract(anchor).offset(offset), draai, anchor.subtract(offset)));
        StructureStart start = new StructureStart(structure, ChunkPos.containing(at), 0, new PiecesContainer(pieces));
        Kopieen.test(level, start);
        BlockPos terug = Kopieen.wereld(start, null, lokaal);
        helper.assertTrue(helper.absolutePos(op).equals(terug), "the copy lies where the test wants it: " + terug);
        return start;
    }

    private static List<Entity> metTag(GameTestHelper helper, String tag) {
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(24, 12, 24);
        return helper.getLevel().getEntitiesOfClass(Entity.class, kamer, e -> e.isAlive() && e.getPersistentData().getStringOr(Bezetting.TAG, "").equals(tag));
    }

    /**
     * What this player is shown inside this test's own room ({@code breed} wide). A fire bowl burns for whoever lit its
     * bridge in EVERY Spiesburcht, so the bowls of another test room a few blocks further are on the player's list too.
     */
    private static Map<BlockPos, BlockState> zietHier(GameTestHelper helper, ServerPlayer p, int breed) {
        AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(breed - 1, 12, breed - 1);
        Map<BlockPos, BlockState> uit = new java.util.HashMap<>();
        Schijn.gewenst(p).forEach((pos, state) -> {
            if (kamer.contains(Vec3.atCenterOf(pos))) {
                uit.put(pos, state);
            }
        });
        return uit;
    }

    private static void vergeetVuren() {
        for (int k = 0; k < Vuren.AANTAL; k++) {
            Bezetting.vergeet(BestaandFeature.BRUGVUUR_ID + k);
        }
    }

    // =================================================================================================================
    // the bridge fires
    // =================================================================================================================

    /**
     * The Wachter-guh's first steps: his talk, the loaned aansteekspies (again when lost), and the four fire bowls. A bowl
     * a player lit burns for that player only: the block in the world stays out and the next player lights it too.
     */
    @GuhTest(template = BRUG, batch = BATCH)
    public static void bestaandBrugvuren(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(7, 2, 7)), b = speler(helper, new BlockPos(8, 2, 7));
        GuhNpcEntity wachter = npc(helper, GuhNpcEntity.Kind.WACHTERGUH, new BlockPos(7, 2, 9));
        try {
            NpcRole rol = Features.role(GuhNpcEntity.Kind.WACHTERGUH);
            helper.assertTrue(rol != null, "the Wachter-guh has a role");
            List<BlockPos> korven = new ArrayList<>();
            for (int nr = 0; nr < 4; nr++) {
                BlockPos pos = helper.absolutePos(new BlockPos(nr % 2 == 0 ? 3 : 12, 3, nr < 2 ? 3 : 12));
                BlockState state = level.getBlockState(pos);
                helper.assertTrue(state.is(BestaandFeature.VUURKORF.get()) && state.getValue(VuurkorfBlock.NR) == nr && !state.getValue(VuurkorfBlock.LIT),
                        "fire bowl " + nr + " stands in the room, out");
                helper.assertTrue(level.getBlockEntity(pos) instanceof VuurkorfBlockEntity, "with its block entity");
                korven.add(pos);
            }
            helper.assertTrue(BestaandFeature.VUURKORF.get().defaultDestroyTime() < 0, "a fire bowl is not broken in survival");
            // before the quest: a click with fire does nothing
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
            gebruik(a, korven.get(0));
            helper.assertTrue(Vuren.aantal(a) == 0 && BestaandFeature.WACHTER.stap(a) == 0, "nothing is lit before the Wachter-guh asked");
            a.getInventory().clearContent();
            // the talk
            rol.talk(wachter, a);
            helper.assertTrue(BestaandFeature.WACHTER.begonnen(a) && BestaandFeature.WACHTER.stap(a) == 0, "hallo: begun, no step yet");
            rol.antwoord(wachter, a, WAT);
            helper.assertTrue(BestaandFeature.WACHTER.stap(a) == 0, "the story of the Vonk-Mikas changes nothing");
            rol.antwoord(wachter, a, HELP);
            helper.assertTrue(BestaandFeature.WACHTER.stap(a) == BestaandFeature.WACHTER_VUREN && count(a, BestaandFeature.AANSTEEKSPIES.get()) == 1
                    && advancement(a, "wachter_stap_1"), "step 1 with the loaned aansteekspies");
            helper.assertTrue(Features.isLoaned(new ItemStack(BestaandFeature.AANSTEEKSPIES.get())), "the spies is a loaned item");
            // the wrong thing in the hand: nothing; the spies: this bowl burns, for a
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE));
            gebruik(a, korven.get(0));
            helper.assertTrue(Vuren.aantal(a) == 0, "cobblestone lights nothing");
            a.getInventory().clearContent();
            rol.talk(wachter, a);
            helper.assertTrue(count(a, BestaandFeature.AANSTEEKSPIES.get()) == 1, "a lost spies comes back");
            rol.talk(wachter, a);
            helper.assertTrue(count(a, BestaandFeature.AANSTEEKSPIES.get()) == 1, "not a second one");
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BestaandFeature.AANSTEEKSPIES.get()));
            gebruik(a, korven.get(0));
            helper.assertTrue(Vuren.brandt(a, 0) && Vuren.aantal(a) == 1 && !Vuren.brandt(b, 0), "bowl 0 burns for a, not for b");
            helper.assertTrue(!level.getBlockState(korven.get(0)).getValue(VuurkorfBlock.LIT), "the block in the world stays out");
            helper.assertTrue(Schijn.ziet(a, korven.get(0)).getValue(VuurkorfBlock.LIT) && !Schijn.ziet(b, korven.get(0)).getValue(VuurkorfBlock.LIT),
                    "a sees it burn, b sees it cold");
            Map<BlockPos, BlockState> zietA = zietHier(helper, a, 16);
            helper.assertTrue(zietA.size() == 1 && zietA.get(korven.get(0)) != null && zietA.get(korven.get(0)).getValue(VuurkorfBlock.LIT)
                    && zietA.get(korven.get(0)).getLightEmission() == 15, "the refresh keeps showing it to a, with its light: " + zietA);
            helper.assertTrue(Schijn.gewenst(b).isEmpty(), "and nothing to b");
            gebruik(a, korven.get(0));
            helper.assertTrue(Vuren.aantal(a) == 1 && BestaandFeature.WACHTER.stap(a) == BestaandFeature.WACHTER_VUREN, "the same bowl again counts once");
            // b does the same quest at the same bowl
            rol.talk(wachter, b);
            rol.antwoord(wachter, b, HELP);
            b.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BestaandFeature.AANSTEEKSPIES.get()));
            gebruik(b, korven.get(0));
            helper.assertTrue(Vuren.brandt(b, 0) && Vuren.aantal(b) == 1 && Vuren.aantal(a) == 1, "b lights the bowl that already burns for a");
            // a lights the other three (flint and steel works too)
            gebruik(a, korven.get(1));
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
            gebruik(a, korven.get(2));
            helper.assertTrue(Vuren.aantal(a) == 3 && BestaandFeature.WACHTER.stap(a) == BestaandFeature.WACHTER_VUREN && !advancement(a, "barbecuether/bestaand_brugvuren"),
                    "three fires: not yet");
            gebruik(a, korven.get(3));
            helper.assertTrue(Vuren.aantal(a) == 4 && BestaandFeature.WACHTER.stap(a) == BestaandFeature.WACHTER_MELDEN
                    && advancement(a, "wachter_stap_2") && advancement(a, "barbecuether/bestaand_brugvuren"), "the fourth fire: on to the Wachter-guh");
            helper.assertTrue(zietHier(helper, a, 16).size() == 4 && zietHier(helper, b, 16).size() == 1, "a sees four fires, b one");
            for (BlockPos pos : korven) {
                helper.assertTrue(!level.getBlockState(pos).getValue(VuurkorfBlock.LIT), "every bowl is still out in the world");
            }
            helper.assertTrue(BestaandFeature.WACHTER.stap(b) == BestaandFeature.WACHTER_VUREN, "b is where b was");
            // the next talk: the tuintje
            rol.talk(wachter, a);
            helper.assertTrue(BestaandFeature.WACHTER.stap(a) == BestaandFeature.WACHTER_MELDEN, "the screen waits for an answer");
            rol.antwoord(wachter, a, WIEDEN);
            helper.assertTrue(BestaandFeature.WACHTER.stap(a) == BestaandFeature.WACHTER_TUIN && advancement(a, "wachter_stap_3"), "step 3: the tuintje");
            // a fire bowl that goes away is forgotten
            level.setBlockAndUpdate(korven.get(3), Blocks.AIR.defaultBlockState());
            helper.assertTrue(zietHier(helper, a, 16).size() == 3, "a removed bowl is no longer shown");
        } finally {
            weg(helper, a, b);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // a Spiesburcht: the hokje, the Wachter-guh, the weeds, the reward
    // =================================================================================================================

    /**
     * At a (turned) copy of a Spiesburcht that has nothing of this update: the wachthokje and the Wachter-guh come. The
     * weeds of the tuintje are only there for the player at that step; pulling them, a spot somebody built over, the reward
     * once, the recipe card again when lost; and a bridge whose fire bowl found no spot counts as lit.
     */
    @GuhTest(template = BURCHT, batch = "bestaand_burcht", timeoutTicks = 200)
    public static void bestaandBurcht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(12, 2, 14)), b = speler(helper, new BlockPos(13, 2, 14));
        String vuur0 = BestaandFeature.BRUGVUUR_ID + 0;
        try {
            vergeetVuren();   // (their spots are 35 blocks from the middle: outside this room)
            // the middle of the keep's hall floor (60, 30, 60) on the middle of the room's floor, a quarter turned
            StructureStart start = kopie(helper, BestaandFeature.SPIESBURCHT, new BlockPos(60, 30, 60), new BlockPos(12, 1, 12), Rotation.CLOCKWISE_90);
            BlockPos bij = helper.absolutePos(new BlockPos(12, 2, 12));
            Bezetting.Geplaatst data = Bezetting.Geplaatst.get(level);
            data.vergeet(BestaandFeature.WACHTHOKJE_ID, start);
            for (int k = 0; k < Vuren.AANTAL; k++) {
                data.vergeet(BestaandFeature.BRUGVUUR_ID + k, start);
            }
            helper.assertTrue(Bezetting.controleer(level, bij) >= 1 && data.plek(BestaandFeature.WACHTHOKJE_ID, start).isPresent(), "the wachthokje is placed");
            BlockPos knop = Kopieen.wereld(start, null, Plekken.WACHTHOKJE.offset(1, 4, 1));
            BlockPos lantaarn = Kopieen.wereld(start, null, Plekken.WACHTHOKJE.offset(1, 3, 1));
            BlockPos wand = Kopieen.wereld(start, null, Plekken.WACHTHOKJE.offset(1, 2, 2));
            helper.assertTrue(level.getBlockState(knop).is(BarbecuetherFeature.GEBEITELDE_HOUTSKOOLSTEEN_STENEN.get()), "its roof knob: " + level.getBlockState(knop));
            helper.assertTrue(level.getBlockState(lantaarn).is(BestaandFeature.ZIELIG_LANTAARNTJE.get())
                    && level.getBlockState(lantaarn).getValue(LantaarntjeBlock.HANGING), "the Zielig lantaarntje hangs in it");
            helper.assertTrue(level.getBlockState(wand).is(Blocks.PINK_TERRACOTTA), "the pink band of its back wall, turned with the copy");
            BlockPos plek = Kopieen.wereld(start, null, Plekken.WACHTER);
            helper.assertTrue(level.getBlockState(plek).isAir() && level.getBlockState(plek.above()).isAir(), "room for the Wachter-guh inside");
            // the Wachter-guh: missing twice, then he is there, once
            String tag = Bezetting.tag(BestaandFeature.WACHTER_ID, start);
            Bezetting.bevestigAlles(level);
            Bezetting.controleer(level, bij);
            List<Entity> er = metTag(helper, tag);
            helper.assertTrue(er.size() == 1 && er.get(0) instanceof GuhNpcEntity n && n.getKind() == GuhNpcEntity.Kind.WACHTERGUH
                    && n.blockPosition().equals(plek) && n.isInvulnerable(), "the Wachter-guh stands in his hokje: " + er);
            GuhNpcEntity wachter = (GuhNpcEntity) er.get(0);
            Bezetting.bevestigAlles(level);
            helper.assertTrue(Bezetting.controleer(level, bij) == 0 && metTag(helper, tag).size() == 1, "no second one, nothing placed twice");
            NpcRole rol = Features.role(GuhNpcEntity.Kind.WACHTERGUH);

            // --- a bridge whose fire bowl found no spot counts as lit (b, at the fires step) ---
            BlockPos dicht = Kopieen.lokaal(start, null, helper.absolutePos(new BlockPos(3, 2, 3)));
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(3, y, 3), Blocks.COBBLESTONE);   // (no room at any of the heights Bezetting tries)
            }
            Bezetting.blokken(vuur0, BestaandFeature.SPIESBURCHT, null, List.of(dicht), Guhs.id("bestaand_brugvuur_0"));
            data.vergeet(vuur0, start);
            Bezetting.controleer(level, bij);
            helper.assertTrue(data.gehad(vuur0, start) && data.plek(vuur0, start).isEmpty(), "bridge 0 of this copy has no free spot for its fire");
            rol.talk(wachter, b);
            rol.antwoord(wachter, b, HELP);
            helper.assertTrue(BestaandFeature.WACHTER.stap(b) == BestaandFeature.WACHTER_VUREN && Vuren.aantal(b) == 0, "b begins");
            rol.talk(wachter, b);
            helper.assertTrue(Vuren.brandt(b, 0) && Vuren.aantal(b) == 1 && BestaandFeature.WACHTER.stap(b) == BestaandFeature.WACHTER_VUREN,
                    "the Wachter-guh counts the missing bowl; the other three are still to do");

            // --- the weeds: only for the player at the weeding step ---
            BestaandFeature.WACHTER.zet(a, BestaandFeature.WACHTER_TUIN);
            Map<BlockPos, BlockState> zietA = Schijn.gewenst(a);
            helper.assertTrue(zietA.size() == Tuintje.AANTAL, "a sees six tufts: " + zietA.size());
            List<BlockPos> pollen = new ArrayList<>();
            for (int i = 0; i < Tuintje.AANTAL; i++) {
                BlockPos pos = Kopieen.wereld(start, null, Plekken.MIKAKRUID.get(i));
                pollen.add(pos);
                helper.assertTrue(zietA.get(pos) != null && zietA.get(pos).is(BestaandFeature.MIKAKRUID.get()) && level.getBlockState(pos).isAir(),
                        "tuft " + i + " is shown at its spot, which is air in the world");
                helper.assertTrue(Tuintje.bij(level, pos) == i, "and a click there is that tuft");
            }
            helper.assertTrue(zietHier(helper, b, 24).isEmpty(), "b (at another step) sees none");
            helper.assertTrue(!Tuintje.klik(b, level, pollen.get(0)), "and can't pull one");
            helper.assertTrue(!Tuintje.klik(a, level, pollen.get(0).above(3)), "a click somewhere else pulls nothing");
            helper.assertTrue(Tuintje.klik(a, level, pollen.get(0)) && Tuintje.gewied(a, 0) && Tuintje.aantal(a) == 1, "a pulls tuft 0");
            helper.assertTrue(!Tuintje.klik(a, level, pollen.get(0)) && Tuintje.aantal(a) == 1, "once");
            helper.assertTrue(Schijn.gewenst(a).size() == Tuintje.AANTAL - 1 && level.getBlockState(pollen.get(0)).isAir(), "five left; the world is untouched");
            // somebody built on a spot: nothing to weed there
            level.setBlockAndUpdate(pollen.get(1), Blocks.COBBLESTONE.defaultBlockState());
            helper.assertTrue(!Tuintje.klik(a, level, pollen.get(1)), "a click on a real block is no weeding");
            helper.assertTrue(Schijn.gewenst(a).size() == Tuintje.AANTAL - 2 && Tuintje.gewied(a, 1), "a built-over spot counts as weeded");
            level.setBlockAndUpdate(pollen.get(1), Blocks.AIR.defaultBlockState());
            for (int i = 2; i < Tuintje.AANTAL - 1; i++) {
                helper.assertTrue(Tuintje.klik(a, level, pollen.get(i)), "tuft " + i);
            }
            helper.assertTrue(BestaandFeature.WACHTER.stap(a) == BestaandFeature.WACHTER_TUIN, "one to go");
            helper.assertTrue(Tuintje.klik(a, level, pollen.get(Tuintje.AANTAL - 1)) && BestaandFeature.WACHTER.stap(a) == BestaandFeature.WACHTER_BELONING
                    && advancement(a, "wachter_stap_4") && Schijn.gewenst(a).isEmpty(), "the last tuft: back to the Wachter-guh");

            // --- the reward, once ---
            a.getInventory().add(new ItemStack(BestaandFeature.AANSTEEKSPIES.get()));
            rol.talk(wachter, a);
            helper.assertTrue(BestaandFeature.WACHTER.stap(a) == BestaandFeature.WACHTER_BELONING && count(a, BestaandFeature.RECEPT_LANTAARN.get()) == 0,
                    "the screen waits for the thank-you");
            rol.antwoord(wachter, a, DANK);
            helper.assertTrue(BestaandFeature.WACHTER.klaar(a) && advancement(a, "wachter_stap_5") && advancement(a, "barbecuether/bestaand_wachter"), "done");
            helper.assertTrue(count(a, BestaandFeature.RECEPT_LANTAARN.get()) == 1 && count(a, BestaandFeature.ZIELIG_LANTAARNTJE_ITEM.get()) == 2
                    && count(a, ModItems.clothingItem(GuhClothes.BESTAAND_WACHTERSHELM)) == 1
                    && count(a, ModItems.clothingItem(GuhClothes.BESTAAND_WACHTERSMANTEL)) == 1, "the recipe card, two lantaarntjes and the wachterspak");
            helper.assertTrue(count(a, BestaandFeature.AANSTEEKSPIES.get()) == 0, "he took his spies back");
            rol.antwoord(wachter, a, DANK);
            rol.talk(wachter, a);
            helper.assertTrue(count(a, BestaandFeature.RECEPT_LANTAARN.get()) == 1 && count(a, BestaandFeature.ZIELIG_LANTAARNTJE_ITEM.get()) == 2,
                    "nothing a second time");
            a.getInventory().clearContent();
            rol.talk(wachter, a);
            helper.assertTrue(count(a, BestaandFeature.RECEPT_LANTAARN.get()) == 1 && count(a, BestaandFeature.ZIELIG_LANTAARNTJE_ITEM.get()) == 0,
                    "a lost recipe card comes back (only the card)");
            helper.assertTrue(BestaandFeature.WACHTER.stand(a).klaar() && BestaandFeature.WACHTER.stand(a).beloningen().stream().allMatch(x -> x.binnen()),
                    "the Guhdex ticks the rewards");
            metTag(helper, tag).forEach(Entity::discard);
        } finally {
            Bezetting.vergeet(vuur0);
            BestaandFeature.bezetting();
            Kopieen.testWissen(level);
            weg(helper, a, b);
        }
        helper.succeed();
    }

    /**
     * A bridge fire as Bezetting places it: the pedestal and the bowl land on the first spot of their bridge, the bowl
     * reports in (so it can burn for who lights it), and its nr is the bridge's.
     */
    @GuhTest(template = BURCHT, batch = "bestaand_brugvuur", timeoutTicks = 200)
    public static void bestaandBrugvuurProp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(12, 2, 14));
        try {
            // only the fire of the north bridge (nr 3) is registered while this test runs; its first spot lies in the room
            vergeetVuren();
            for (String id : List.of(BestaandFeature.WACHTER_ID, BestaandFeature.WACHTHOKJE_ID)) {
                Bezetting.vergeet(id);
            }
            Bezetting.blokken(BestaandFeature.BRUGVUUR_ID + 3, BestaandFeature.SPIESBURCHT, null, Plekken.BRUGVUUR_3, Guhs.id("bestaand_brugvuur_3"));
            StructureStart start = kopie(helper, BestaandFeature.SPIESBURCHT, Plekken.BRUGVUUR_3.get(0).below(), new BlockPos(12, 1, 12), Rotation.CLOCKWISE_180);
            Bezetting.Geplaatst data = Bezetting.Geplaatst.get(level);
            data.vergeet(BestaandFeature.BRUGVUUR_ID + 3, start);
            BlockPos sokkel = helper.absolutePos(new BlockPos(12, 2, 12));
            helper.assertTrue(Bezetting.controleer(level, sokkel) == 1, "placed");
            helper.assertTrue(sokkel.equals(data.plek(BestaandFeature.BRUGVUUR_ID + 3, start).orElse(null)), "on the first spot of its bridge");
            BlockPos korfPlek = sokkel.above(Vuren.KORF_HOOGTE);
            BlockState korf = level.getBlockState(korfPlek);
            helper.assertTrue(level.getBlockState(sokkel).is(BarbecuetherFeature.GEBEITELDE_HOUTSKOOLSTEEN_STENEN.get()) && korf.is(BestaandFeature.VUURKORF.get())
                    && korf.getValue(VuurkorfBlock.NR) == 3 && !korf.getValue(VuurkorfBlock.LIT), "a carved pedestal with the cold bowl of bridge 3 on its post");
            BestaandFeature.WACHTER.zet(a, BestaandFeature.WACHTER_VUREN);
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BestaandFeature.AANSTEEKSPIES.get()));
            gebruik(a, korfPlek);
            helper.assertTrue(Vuren.brandt(a, 3) && Schijn.gewenst(a).containsKey(korfPlek), "it reported in: lit, it is shown burning");
            // (turned half round, the template's north bridge points south in the world)
            helper.assertTrue(Vuren.richting(level, korfPlek, 3).equals("zuid"), "the bridge is named by where it points in the world: "
                    + Vuren.richting(level, korfPlek, 3));
            level.setBlockAndUpdate(korfPlek, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(sokkel.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(sokkel, Blocks.AIR.defaultBlockState());
        } finally {
            BestaandFeature.bezetting();
            Kopieen.testWissen(level);
            weg(helper, a);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // a Mika-grillpaleis: the naaihoek, the Knuffelmaker-guh, the cages
    // =================================================================================================================

    private static MikaEntity netherMika(GameTestHelper helper, BlockPos at, Entity kijktNaar) {
        MikaEntity mika = ModEntities.NETHER_MIKA.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        BlockPos p = helper.absolutePos(at);
        mika.setNoAi(true);
        mika.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        kijk(mika, kijktNaar, true);
        helper.getLevel().addFreshEntity(mika);
        return mika;
    }

    /** Turns the Mika towards (or away from) this entity. */
    private static void kijk(MikaEntity mika, Entity naar, boolean ernaar) {
        float yaw = (float) (Mth.atan2(naar.getZ() - mika.getZ(), naar.getX() - mika.getX()) * Mth.RAD_TO_DEG) - 90f + (ernaar ? 0f : 180f);
        mika.setYRot(yaw);
        mika.setYHeadRot(yaw);
        mika.setYBodyRot(yaw);
        mika.yHeadRotO = yaw;
        mika.setXRot(0f);
    }

    /**
     * At a copy of a grillpaleis: the naaihoek and the Knuffelmaker-guh come. A cage opens with the ransom or by sneaking
     * (three clicks, not while a Nether-Mika looks: that only shoves, never hurts); the freed plush is gone for that player
     * alone and sits in the naaihoek; thread; the reward once; a cage somebody broke counts as freed.
     */
    @GuhTest(template = PALEIS, batch = "bestaand_paleis", timeoutTicks = 200)
    public static void bestaandPaleis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(12, 2, 12)), b = speler(helper, new BlockPos(13, 2, 12)), c = speler(helper, new BlockPos(11, 2, 12));
        MikaEntity mika = null;
        try {
            // the middle of the first floor (36, 21, 36) on the middle of the room's floor
            StructureStart start = kopie(helper, BestaandFeature.GRILLPALEIS, new BlockPos(36, 21, 36), new BlockPos(12, 1, 12), Rotation.NONE);
            BlockPos bij = helper.absolutePos(new BlockPos(12, 2, 12));
            Bezetting.Geplaatst data = Bezetting.Geplaatst.get(level);
            data.vergeet(BestaandFeature.NAAIHOEK_ID, start);
            helper.assertTrue(Bezetting.controleer(level, bij) == 1, "the naaihoek is placed");
            BlockPos hoek = Kopieen.wereld(start, null, Plekken.NAAIHOEK);
            helper.assertTrue(hoek.equals(data.plek(BestaandFeature.NAAIHOEK_ID, start).orElse(null)) && hoek.equals(helper.absolutePos(new BlockPos(17, 2, 16))),
                    "in the fourth corner of the first floor: " + hoek);
            helper.assertTrue(level.getBlockState(hoek.offset(1, 0, 4)).is(Blocks.LOOM) && level.getBlockState(hoek.offset(3, 0, 0)).is(Blocks.CRIMSON_SIGN)
                    && level.getBlockState(hoek.offset(0, 0, 1)).is(BarbecuetherFeature.ROOSTERIJZER_TRALIES.get())
                    && level.getBlockState(hoek.offset(2, 0, 1)).isAir() && level.getBlockState(hoek.offset(2, 1, 1)).isAir(), "a loom, the Mikas' sign, bars, an open door");
            String tag = Bezetting.tag(BestaandFeature.KNUFFELMAKER_ID, start);
            Bezetting.bevestigAlles(level);
            Bezetting.controleer(level, bij);
            List<Entity> er = metTag(helper, tag);
            BlockPos zit = Kopieen.wereld(start, null, Plekken.KNUFFELMAKER);
            helper.assertTrue(er.size() == 1 && er.get(0) instanceof GuhNpcEntity n && n.getKind() == GuhNpcEntity.Kind.KNUFFELMAKERGUH
                    && n.blockPosition().equals(zit) && zit.equals(hoek.offset(2, 0, 3)), "the Knuffelmaker-guh sits in it: " + er);
            GuhNpcEntity maker = (GuhNpcEntity) er.get(0);
            NpcRole rol = Features.role(GuhNpcEntity.Kind.KNUFFELMAKERGUH);
            List<BlockPos> kooien = new ArrayList<>();
            for (int i = 0; i < Kooien.AANTAL; i++) {
                BlockPos midden = Kopieen.wereld(start, null, Plekken.KOOIEN.get(i));
                kooien.add(midden);
                helper.assertTrue(level.getBlockState(midden).is(Blocks.PINK_WOOL) && level.getBlockState(midden.offset(2, 0, 0)).is(BarbecuetherFeature.ROOSTERIJZER_TRALIES.get()),
                        "cage " + i + " with its plush stands where the palace has it");
            }
            ItemStack leeg = ItemStack.EMPTY;

            // before the quest: a click on a cage only says who knows more
            helper.assertTrue(Kooien.klik(a, level, kooien.get(0).offset(2, 0, 0), leeg, InteractionHand.MAIN_HAND) && Kooien.aantal(a) == 0, "a cage click, nothing freed");
            helper.assertTrue(!Kooien.klik(a, level, helper.absolutePos(new BlockPos(12, 1, 12)), leeg, InteractionHand.MAIN_HAND), "the floor is no cage");
            helper.assertTrue(!Kooien.klik(a, level, hoek.offset(0, 0, 1), leeg, InteractionHand.MAIN_HAND), "the bars of the naaihoek are no plush cage");
            rol.talk(maker, a);
            helper.assertTrue(BestaandFeature.KNUFFELMAKER.begonnen(a) && BestaandFeature.KNUFFELMAKER.stap(a) == 0, "hallo");
            rol.antwoord(maker, a, 2);
            rol.antwoord(maker, a, HELP);
            helper.assertTrue(BestaandFeature.KNUFFELMAKER.stap(a) == BestaandFeature.KNUFFELMAKER_KOOIEN && advancement(a, "knuffelmaker_stap_1"), "step 1: the cages");

            // --- with vads: one ingot, the cage is open at once ---
            Kooien.klik(a, level, kooien.get(0).offset(2, 0, 0), leeg, InteractionHand.MAIN_HAND);
            helper.assertTrue(Kooien.aantal(a) == 0, "an empty hand without sneaking opens nothing");
            ItemStack vads = new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get(), 2);
            helper.assertTrue(vads.is(BestaandFeature.LOSGELD) && new ItemStack(ModItems.VAHOEGE_VADS.get()).is(BestaandFeature.LOSGELD), "vads is ransom");
            Kooien.klik(a, level, kooien.get(0).offset(2, 1, 0), vads, InteractionHand.MAIN_HAND);
            helper.assertTrue(Kooien.vrij(a, 0) && Kooien.aantal(a) == 1 && vads.getCount() == 1, "one vads paid: plush 0 is free for a");
            Kooien.klik(a, level, kooien.get(0).offset(2, 1, 0), vads, InteractionHand.MAIN_HAND);
            helper.assertTrue(vads.getCount() == 1 && Kooien.aantal(a) == 1, "nothing is paid twice");
            helper.assertTrue(level.getBlockState(kooien.get(0)).is(Blocks.PINK_WOOL), "the plush is still in the cage in the world");
            Map<BlockPos, BlockState> zietA = Schijn.gewenst(a);
            helper.assertTrue(zietA.getOrDefault(kooien.get(0), Blocks.STONE.defaultBlockState()).isAir()
                    && zietA.getOrDefault(kooien.get(0).above(), Blocks.STONE.defaultBlockState()).isAir()
                    && zietA.getOrDefault(kooien.get(0).offset(0, 0, -1), Blocks.STONE.defaultBlockState()).isAir(), "a sees that cage empty");
            BlockPos zitplek = Kopieen.wereld(start, null, Plekken.KNUFFELPLEKKEN.get(0));
            helper.assertTrue(zietA.get(zitplek) != null && zietA.get(zitplek).is(BestaandFeature.KNUFFELGUH.get())
                    && zietA.get(zitplek).getValue(HorizontalDirectionalBlock.FACING) == Direction.NORTH && level.getBlockState(zitplek).isAir(),
                    "and the plush guh sitting in the naaihoek (air in the world)");
            helper.assertTrue(zietA.size() == 8, "seven plush blocks gone, one plush in the naaihoek: " + zietA.size());
            helper.assertTrue(Schijn.gewenst(b).isEmpty() && !Kooien.vrij(b, 0), "b sees the cage full");

            // --- by sneaking: three clicks, one at a time ---
            a.setShiftKeyDown(true);
            BlockPos tralie1 = kooien.get(1).offset(-2, 0, 0);
            Kooien.klik(a, level, tralie1, leeg, InteractionHand.MAIN_HAND);
            helper.assertTrue(Kooien.gepeuterd(a, 1) == 1 && !Kooien.vrij(a, 1), "the first sneaky click");
            Kooien.klik(a, level, tralie1, leeg, InteractionHand.MAIN_HAND);
            helper.assertTrue(Kooien.gepeuterd(a, 1) == 1, "the button still down in the same tick: no second click");
            helper.assertTrue(Kooien.klik(a, level, tralie1, leeg, InteractionHand.OFF_HAND) && Kooien.gepeuterd(a, 1) == 1, "the other hand does nothing (and places nothing)");
            Kooien.verouder(a);
            Kooien.klik(a, level, tralie1, leeg, InteractionHand.MAIN_HAND);
            Kooien.verouder(a);
            helper.assertTrue(Kooien.gepeuterd(a, 1) == 2 && !advancement(a, "bestaand_gesloten"), "two");
            Kooien.klik(a, level, tralie1, leeg, InteractionHand.MAIN_HAND);
            helper.assertTrue(Kooien.vrij(a, 1) && Kooien.aantal(a) == 2 && advancement(a, "bestaand_gesloten") && advancement(a, "barbecuether/bestaand_sluiper"),
                    "three: the lock is picked");
            helper.assertTrue(Schijn.gewenst(a).get(Kopieen.wereld(start, null, Plekken.KNUFFELPLEKKEN.get(1))).is(BestaandFeature.KNUFFELMIKA.get()),
                    "the second plush in the naaihoek is the Knuffel-Mika");

            // --- a Nether-Mika that looks: caught, a shove, never damage; the picking starts over ---
            BlockPos tralie2 = kooien.get(2).offset(2, 0, 0);
            a.snapTo(tralie2.getX() + 1.5, tralie2.getY(), tralie2.getZ() + 0.5);
            a.setDeltaMovement(Vec3.ZERO);
            a.hurtMarked = false;
            Kooien.klik(a, level, tralie2, leeg, InteractionHand.MAIN_HAND);
            Kooien.verouder(a);
            helper.assertTrue(Kooien.gepeuterd(a, 2) == 1, "one click while nobody looks");
            mika = netherMika(helper, new BlockPos(12, 2, 19), a);
            helper.assertTrue(Kooien.betrapt(a) == mika, "the Nether-Mika sees a");
            float leven = a.getHealth();
            Kooien.klik(a, level, tralie2, leeg, InteractionHand.MAIN_HAND);
            helper.assertTrue(!Kooien.vrij(a, 2) && Kooien.gepeuterd(a, 2) == 0, "caught: not open, start over");
            helper.assertTrue(a.hurtMarked && a.getDeltaMovement().horizontalDistance() > 0.5 && a.getDeltaMovement().x > 0, "a shove away from the cage");
            helper.assertTrue(a.getHealth() == leven && a.isAlive(), "and not a scratch");
            // it looks the other way: free to pick
            kijk(mika, a, false);
            helper.assertTrue(Kooien.betrapt(a) == null, "a Mika that looks away sees nothing");
            // it looks again, but a drank a Sluipknabbeldrankje (invisible here: guhs:stil is another slice's effect)
            kijk(mika, a, true);
            a.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0));
            helper.assertTrue(Kooien.betrapt(a) == null, "nobody sees a sneaky drinker");
            a.removeAllEffects();
            helper.assertTrue(Kooien.betrapt(a) == mika, "without it: seen again");
            a.setShiftKeyDown(false);
            // so a pays for the last one, with a vahoege vads
            ItemStack ruw = new ItemStack(ModItems.VAHOEGE_VADS.get());
            Kooien.klik(a, level, tralie2, ruw, InteractionHand.MAIN_HAND);
            helper.assertTrue(ruw.isEmpty() && Kooien.aantal(a) == 3 && BestaandFeature.KNUFFELMAKER.stap(a) == BestaandFeature.KNUFFELMAKER_DRAAD
                    && advancement(a, "knuffelmaker_stap_2"), "the third plush: on to the thread");
            helper.assertTrue(Schijn.gewenst(a).size() == 24, "three empty cages and three plush guhs in the naaihoek: " + Schijn.gewenst(a).size());
            for (BlockPos midden : kooien) {
                helper.assertTrue(level.getBlockState(midden).is(Blocks.PINK_WOOL), "all three still in their cages for everybody else");
            }

            // --- thread, the reward once ---
            rol.talk(maker, a);
            helper.assertTrue(BestaandFeature.KNUFFELMAKER.stap(a) == BestaandFeature.KNUFFELMAKER_DRAAD, "no thread: he asks");
            a.getInventory().add(new ItemStack(Items.STRING, 3));
            rol.talk(maker, a);
            helper.assertTrue(BestaandFeature.KNUFFELMAKER.stap(a) == BestaandFeature.KNUFFELMAKER_DRAAD && count(a, Items.STRING) == 3, "three is not enough, nothing is taken");
            a.getInventory().add(new ItemStack(Items.STRING, 3));
            rol.talk(maker, a);
            helper.assertTrue(BestaandFeature.KNUFFELMAKER.klaar(a) && count(a, Items.STRING) == 2 && advancement(a, "knuffelmaker_stap_3")
                    && advancement(a, "barbecuether/bestaand_knuffelmaker"), "four thread taken: done");
            helper.assertTrue(count(a, BestaandFeature.RECEPT_KNUFFEL.get()) == 1 && count(a, BestaandFeature.KNUFFELGUH_ITEM.get()) == 1
                    && count(a, BestaandFeature.KNUFFELMIKA_ITEM.get()) == 1 && count(a, BestaandFeature.KNUFFELROOKGUH_ITEM.get()) == 1,
                    "the knuffelpatroon and the three plush blocks");
            rol.talk(maker, a);
            helper.assertTrue(count(a, BestaandFeature.KNUFFELGUH_ITEM.get()) == 1 && count(a, BestaandFeature.RECEPT_KNUFFEL.get()) == 1, "nothing a second time");
            a.getInventory().clearContent();
            rol.talk(maker, a);
            helper.assertTrue(count(a, BestaandFeature.RECEPT_KNUFFEL.get()) == 1 && count(a, BestaandFeature.KNUFFELGUH_ITEM.get()) == 0, "a lost patroon comes back");
            helper.assertTrue(Schijn.gewenst(a).size() == 24, "the plush guhs stay with the Knuffelmaker-guh for a");

            // --- a cage somebody broke open counts as freed (c) ---
            rol.talk(maker, c);
            rol.antwoord(maker, c, HELP);
            level.setBlockAndUpdate(kooien.get(1), Blocks.AIR.defaultBlockState());
            rol.talk(maker, c);
            helper.assertTrue(Kooien.vrij(c, 1) && Kooien.aantal(c) == 1 && BestaandFeature.KNUFFELMAKER.stap(c) == BestaandFeature.KNUFFELMAKER_KOOIEN,
                    "the broken cage is counted for c, the other two are still to do");
            helper.assertTrue(BestaandFeature.KNUFFELMAKER.stap(b) == 0 && Kooien.aantal(b) == 0, "b did nothing and has nothing");
            metTag(helper, tag).forEach(Entity::discard);
        } finally {
            if (mika != null) {
                mika.discard();
            }
            Kopieen.testWissen(level);
            weg(helper, a, b, c);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the blocks and the recipes
    // =================================================================================================================

    private static CraftingInput lantaarnRooster(boolean recept) {
        ItemStack n = new ItemStack(Items.IRON_NUGGET);
        return CraftingInput.of(3, 3, List.of(n, n, n, new ItemStack(Items.LIGHT_BLUE_DYE), new ItemStack(BarbecuetherFeature.GLOEIKOOLGRUIS.get()),
                recept ? new ItemStack(BestaandFeature.RECEPT_LANTAARN.get()) : ItemStack.EMPTY, n, n, n));
    }

    private static CraftingInput knuffelRooster(Item wol, boolean patroon) {
        ItemStack w = new ItemStack(wol);
        return CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, w, ItemStack.EMPTY, w, new ItemStack(Items.STRING), w, w,
                patroon ? new ItemStack(BestaandFeature.RECEPT_KNUFFEL.get()) : ItemStack.EMPTY, w));
    }

    /**
     * The Zielig lantaarntje cheers up when patted (brighter) and hangs or stands; a plush can be squeezed; the recipes need
     * their card, which stays in the grid.
     */
    @GuhTest(template = BURCHT, batch = BATCH)
    public static void bestaandBlokken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(7, 2, 7));
        try {
            BlockPos staat = new BlockPos(6, 2, 6), hangt = new BlockPos(8, 3, 6);
            helper.setBlock(staat, BestaandFeature.ZIELIG_LANTAARNTJE.get().defaultBlockState());
            helper.setBlock(hangt.above(), Blocks.STONE);
            helper.setBlock(hangt, BestaandFeature.ZIELIG_LANTAARNTJE.get().defaultBlockState().setValue(LantaarntjeBlock.HANGING, true));
            BlockState zielig = helper.getBlockState(staat);
            helper.assertTrue(!zielig.getValue(LantaarntjeBlock.BLIJ) && zielig.getLightEmission() == 10 && zielig.canSurvive(level, helper.absolutePos(staat)),
                    "it stands, pitiful, with a soft light");
            gebruik(p, helper.absolutePos(staat));
            helper.assertTrue(helper.getBlockState(staat).getValue(LantaarntjeBlock.BLIJ) && helper.getBlockState(staat).getLightEmission() == 15, "a pat: happy and bright");
            gebruik(p, helper.absolutePos(staat));
            helper.assertTrue(!helper.getBlockState(staat).getValue(LantaarntjeBlock.BLIJ), "another pat: pitiful again");
            helper.assertTrue(helper.getBlockState(hangt).canSurvive(level, helper.absolutePos(hangt)), "it hangs under a block");
            helper.setBlock(hangt.above(), Blocks.AIR);
            helper.assertTrue(helper.getBlockState(hangt).isAir(), "and falls when the block is gone");
            helper.setBlock(new BlockPos(6, 2, 8), BestaandFeature.KNUFFELMIKA.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
            gebruik(p, helper.absolutePos(new BlockPos(6, 2, 8)));
            helper.assertBlockPresent(BestaandFeature.KNUFFELMIKA.get(), new BlockPos(6, 2, 8));
            helper.assertTrue(BestaandFeature.MIKAKRUID.get().defaultBlockState().getCollisionShape(level, BlockPos.ZERO).isEmpty(), "Mikakruid: you walk through it");
            // the recipes
            var recepten = level.recipeAccess();
            helper.assertTrue(recepten.getRecipeFor(RecipeType.CRAFTING, lantaarnRooster(false), level).isEmpty(), "no lantaarntje without the recipe card");
            var lantaarn = recepten.getRecipeFor(RecipeType.CRAFTING, lantaarnRooster(true), level);
            helper.assertTrue(lantaarn.isPresent(), "with the card: a recipe");
            ItemStack uit = lantaarn.get().value().assemble(lantaarnRooster(true));
            helper.assertTrue(uit.is(BestaandFeature.ZIELIG_LANTAARNTJE_ITEM.get()) && uit.getCount() == 2, "two Zielige lantaarntjes");
            helper.assertTrue(lantaarn.get().value().getRemainingItems(lantaarnRooster(true)).stream().anyMatch(s -> s.is(BestaandFeature.RECEPT_LANTAARN.get())),
                    "the recipe card stays in the grid");
            Map<Item, Item> knuffels = Map.of(Items.PINK_WOOL, BestaandFeature.KNUFFELGUH_ITEM.get(), Items.BLACK_WOOL, BestaandFeature.KNUFFELMIKA_ITEM.get(),
                    Items.LIGHT_GRAY_WOOL, BestaandFeature.KNUFFELROOKGUH_ITEM.get());
            for (var e : knuffels.entrySet()) {
                helper.assertTrue(recepten.getRecipeFor(RecipeType.CRAFTING, knuffelRooster(e.getKey(), false), level).isEmpty(), "no plush without the patroon");
                var r = recepten.getRecipeFor(RecipeType.CRAFTING, knuffelRooster(e.getKey(), true), level);
                helper.assertTrue(r.isPresent() && r.get().value().assemble(knuffelRooster(e.getKey(), true)).is(e.getValue()), "the plush of " + e.getKey());
                helper.assertTrue(r.get().value().getRemainingItems(knuffelRooster(e.getKey(), true)).stream().anyMatch(s -> s.is(BestaandFeature.RECEPT_KNUFFEL.get())),
                        "the patroon stays in the grid");
            }
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }
}
