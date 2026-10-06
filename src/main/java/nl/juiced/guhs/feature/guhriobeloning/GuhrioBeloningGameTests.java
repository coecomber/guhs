package nl.juiced.guhs.feature.guhriobeloning;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.taal.NlTekst;

/**
 * Game tests of bbq2 (guhrio-beloning). Template: guhriobeloning_test_kamer (13 x 9 x 13, a bare stone floor; things on the
 * floor stand at helper y 2).
 * <ul>
 *     <li>the ?-block: one knabbel per day per player (a click, and a head against its underside), every player their own;</li>
 *     <li>the green pipe: mouth and body, the pair by colour, a whole trip (in, across, out; nothing hurts on the way), no
 *     bouncing back, a dye makes another pair, a blocked or missing partner, a rider stays out;</li>
 *     <li>the flagpole's parts follow the stack; the highscore board's floating text and a player's own times;</li>
 *     <li>Pad-guh: the intro, buying with level coins (two players, two pockets), too few coins, the running gag once per
 *     world, the golden cap for eighteen vadsmunten, once;</li>
 *     <li>Guhshi is yours once per player after the duel, saddled; the copy stays; the princess's thanks once;</li>
 *     <li>your own Guhshi licks up a knabbel from a distance (not a fresh one, not somebody's, not with the tongue off)
 *     and flutters when ridden; the copy does neither;</li>
 *     <li>the Guhdex page lists the eighteen vadsmunten and the rewards; prices in the Dutch texts; the forecourt of the
 *     castle's tiles holds the board, the two pipes and the flagpole.</li>
 * </ul>
 * Mock players are not ticked by the server, so the tests call what a player tick does themselves.
 */
public class GuhrioBeloningGameTests {
    private static final String KAMER = "guhriobeloning_test_kamer", BATCH = "guhriobeloning";
    private static final Verhaallijn LIJN = GuhrioBeloningFeature.LIJN;

    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        zet(helper, p, at);
        return p;
    }

    private static void zet(GameTestHelper helper, Entity e, BlockPos at) {
        BlockPos abs = helper.absolutePos(at);
        e.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Pijpreis.wis(p.getUUID());
            Vraagblok.wis(p.getUUID());
            Praat.vergeet(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int aantal(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    private static InteractionResult klik(GameTestHelper helper, ServerPlayer p, BlockPos abs) {
        return helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), p, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        zet(helper, npc, at);
        npc.setPersistenceRequired();
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    /** Marks things of the engine's own saved data for a player (what playing the castle would do). */
    private static void gehaald(ServerPlayer p, String... levels) {
        CompoundTag gehaald = GuhrioSpel.spaar(p).getCompoundOrEmpty("Gehaald");
        for (String level : levels) {
            gehaald.putBoolean(level, true);
        }
        GuhrioSpel.spaar(p).put("Gehaald", gehaald);
    }

    private static void vadsmunten(ServerPlayer p, String level, int bits) {
        CompoundTag vads = GuhrioSpel.spaar(p).getCompoundOrEmpty("Vads");
        vads.putInt(level, bits);
        GuhrioSpel.spaar(p).put("Vads", vads);
    }

    // =====================================================================================================================
    // the ?-block
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void guhriobeloningVraagblokEenPerDag(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos blok = helper.absolutePos(new BlockPos(6, 5, 6));
        level.setBlock(blok, GuhrioBeloningFeature.VRAAGBLOK.get().defaultBlockState(), Block.UPDATE_ALL);
        ServerPlayer a = speler(helper, new BlockPos(6, 2, 5)), b = speler(helper, new BlockPos(5, 2, 6));
        helper.assertTrue(!Vraagblok.gehad(a) && !Vraagblok.gehad(b), "nobody had today's knabbel yet");
        // a click: the knabbel pops out of the top, for A alone
        helper.assertTrue(klik(helper, a, blok) == InteractionResult.SUCCESS && Vraagblok.gehad(a) && !Vraagblok.gehad(b), "A's knabbel of today");
        List<ItemEntity> uit = level.getEntitiesOfClass(ItemEntity.class, new AABB(blok.above()).inflate(0.6), i -> i.getItem().is(ModItems.KAAS_KNABBELS.get()));
        helper.assertTrue(uit.size() == 1 && uit.get(0).getItem().getCount() == 1 && a.getUUID().equals(uit.get(0).getTarget()), "one knabbel, A's own: " + uit.size());
        helper.assertTrue(GidsFeature.heeft(a, "quest/guhrio_beloning_vraagblok") && !GidsFeature.heeft(b, "quest/guhrio_beloning_vraagblok"), "the advancement, A only");
        uit.get(0).discard();
        helper.assertTrue(!Vraagblok.bots(a, blok), "a second bump in the same jump does nothing");
        // B bumps it with their head: rising under the block until the head touches its underside
        b.setOnGround(false);
        b.snapTo(blok.getX() + 0.5, blok.getY() - b.getBbHeight() - 0.4, blok.getZ() + 0.5);
        Vraagblok.kijk(b);
        helper.assertTrue(!Vraagblok.gehad(b), "not there yet");
        b.snapTo(blok.getX() + 0.5, blok.getY() - b.getBbHeight(), blok.getZ() + 0.5);
        Vraagblok.kijk(b);
        helper.assertTrue(Vraagblok.gehad(b), "B's head against the block: B's own knabbel of today");
        helper.runAfterDelay(Vraagblok.RUST + 2, () -> {
            helper.assertTrue(!Vraagblok.bots(a, blok) && Vraagblok.gehad(a), "empty for A until tomorrow");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(blok.above()).inflate(0.6), i -> a.getUUID().equals(i.getTarget())).isEmpty(),
                    "and no second knabbel");
            // tomorrow (the day A last got one was yesterday)
            GuhQuests.saved(a).putLong(Vraagblok.DAG, Vraagblok.dag(level));
            helper.assertTrue(!Vraagblok.gehad(a), "a new day");
        });
        helper.runAfterDelay(2 * Vraagblok.RUST + 4, () -> {
            // a block on top of it: the knabbel goes straight into the pocket
            level.setBlock(blok.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(Vraagblok.bots(a, blok) && aantal(a, ModItems.KAAS_KNABBELS.get()) == 1, "tomorrow's knabbel, into the pocket");
            weg(helper, a, b);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the green pipe
    // =====================================================================================================================

    private static void opMond(ServerPlayer p, BlockPos mond) {
        p.snapTo(mond.getX() + 0.5, mond.getY() + 1, mond.getZ() + 0.5);
        p.setOnGround(true);
    }

    /** A whole trip: every tick of it. */
    private static void reis(ServerPlayer p) {
        for (int i = 0; i < Pijpreis.IN_TICKS + Pijpreis.UIT_TICKS + 2 && Pijpreis.onderweg(p); i++) {
            Pijpreis.tick(p);
        }
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void guhriobeloningPijpReis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState groen = GuhrioBeloningFeature.PIJP.get().defaultBlockState();
        BlockPos a = helper.absolutePos(new BlockPos(2, 3, 2)), b = helper.absolutePos(new BlockPos(10, 2, 10)), rood = helper.absolutePos(new BlockPos(6, 2, 6));
        // a pipe of two blocks: the placed block under another becomes the body by itself
        level.setBlock(a.below(), groen, Block.UPDATE_ALL);
        level.setBlock(a, groen, Block.UPDATE_ALL);
        level.setBlock(b, groen, Block.UPDATE_ALL);
        level.setBlock(rood, groen.setValue(PijpBlock.KLEUR, DyeColor.RED), Block.UPDATE_ALL);
        helper.assertTrue(!level.getBlockState(a.below()).getValue(PijpBlock.MOND) && level.getBlockState(a).getValue(PijpBlock.MOND)
                && PijpBlock.mond(level, a.below()).equals(a), "body under mouth");
        helper.assertTrue(b.equals(Pijpreis.partner(level, a)) && a.equals(Pijpreis.partner(level, b)) && Pijpreis.partner(level, rood) == null,
                "green goes to green, the red one leads nowhere");
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        helper.assertTrue(Pijpreis.onder(p) == null, "not on a pipe");
        Pijpreis.tick(p);
        opMond(p, a);
        helper.assertTrue(a.equals(Pijpreis.onder(p)), "standing on the mouth");
        Pijpreis.tick(p);
        helper.assertTrue(!Pijpreis.onderweg(p), "standing is not enough");
        p.setShiftKeyDown(true);
        Pijpreis.tick(p);
        helper.assertTrue(Pijpreis.onderweg(p), "sneaking on the mouth: in you go");
        float hartjes = p.getHealth();
        p.hurtServer(level, level.damageSources().generic(), 5f);
        helper.assertTrue(p.getHealth() == hartjes, "nothing hurts in a pipe");
        for (int i = 0; i < Pijpreis.IN_TICKS - 1; i++) {
            Pijpreis.tick(p);
        }
        helper.assertTrue(Pijpreis.onderweg(p) && p.blockPosition().equals(a.above()), "still sliding down");
        Pijpreis.tick(p);
        helper.assertTrue(p.blockPosition().equals(b.above()), "out of the other pipe: " + p.blockPosition() + " / " + b.above());
        reis(p);
        helper.assertTrue(!Pijpreis.onderweg(p) && GidsFeature.heeft(p, "quest/guhrio_beloning_pijp") && GidsFeature.heeft(p, "guhrio/guhrio_beloning_pijp"),
                "the trip is over, the advancements");
        p.hurtServer(level, level.damageSources().generic(), 5f);
        helper.assertTrue(p.getHealth() < hartjes, "(outside a pipe the same blow does hurt)");
        p.setHealth(p.getMaxHealth());
        // still sneaking on the other mouth: no bouncing back, also not right after letting go
        p.setOnGround(true);
        Pijpreis.tick(p);
        helper.assertTrue(!Pijpreis.onderweg(p), "no trip back while the sneak key was never released");
        p.setShiftKeyDown(false);
        Pijpreis.tick(p);
        p.setShiftKeyDown(true);
        Pijpreis.tick(p);
        helper.assertTrue(!Pijpreis.onderweg(p), "and not within the rest after a trip");
        p.setShiftKeyDown(false);
        Pijpreis.tick(p);
        helper.runAfterDelay(Pijpreis.RUST + 2, () -> {
            // a click on the pipe you stand on works too
            opMond(p, b);
            helper.assertTrue(klik(helper, p, b) == InteractionResult.SUCCESS && Pijpreis.onderweg(p), "a click on the pipe under you");
            reis(p);
            helper.assertTrue(p.blockPosition().equals(a.above()), "back on the first pipe");
            // a dye paints the whole pipe: now the red one has a partner and the green one none
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RED_DYE, 2));
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(a.below()), Direction.NORTH, a.below(), false);
            level.getBlockState(a.below()).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(level.getBlockState(a).getValue(PijpBlock.KLEUR) == DyeColor.RED && level.getBlockState(a.below()).getValue(PijpBlock.KLEUR) == DyeColor.RED
                    && p.getMainHandItem().getCount() == 1, "the whole pipe is red, one dye used");
            helper.assertTrue(a.equals(Pijpreis.partner(level, rood)) && Pijpreis.partner(level, b) == null, "the pairs follow the colour");
            // the partner's mouth is blocked: no trip
            level.setBlock(a.above(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(Pijpreis.partner(level, rood) == null, "a blocked mouth is no partner");
            level.setBlock(a.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        });
        helper.runAfterDelay(2 * Pijpreis.RUST + 6, () -> {
            // no partner: nothing happens (and it says so); a rider stays out
            opMond(p, b);
            helper.assertTrue(!Pijpreis.probeer(p, b, true) && !Pijpreis.onderweg(p), "the green pipe leads nowhere now");
            GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            guh.snapTo(rood.getX() + 0.5, rood.getY() + 1, rood.getZ() + 0.5);
            level.addFreshEntity(guh);
            p.startRiding(guh, true, false);
            helper.assertTrue(p.isPassenger() && !Pijpreis.probeer(p, rood, true), "a rider does not fit");
            p.stopRiding();
            guh.discard();
            // the top block of a pipe is broken: the body becomes the mouth
            level.setBlock(a, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            helper.assertTrue(level.getBlockState(a.below()).getValue(PijpBlock.MOND) && a.below().equals(Pijpreis.partner(level, rood)), "the body is the mouth now");
            weg(helper, p);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the flagpole and the highscore board
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriobeloningVlaggenmast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Block mast = GuhrioBeloningFeature.VLAGGENMAST.get();
        BlockPos voet = helper.absolutePos(new BlockPos(6, 2, 6));
        for (int k = 0; k < 3; k++) {
            // (what placing the item does: the new block looks at its neighbours, the neighbours at the new block)
            level.setBlock(voet.above(k), Block.updateFromNeighbourShapes(mast.defaultBlockState(), level, voet.above(k)), Block.UPDATE_ALL);
        }
        helper.assertTrue(level.getBlockState(voet).getValue(VlaggenmastBlock.DEEL) == VlaggenmastBlock.Deel.VOET
                && level.getBlockState(voet.above()).getValue(VlaggenmastBlock.DEEL) == VlaggenmastBlock.Deel.PAAL
                && level.getBlockState(voet.above(2)).getValue(VlaggenmastBlock.DEEL) == VlaggenmastBlock.Deel.TOP, "foot, pole, top");
        helper.assertTrue(VlaggenmastBlock.top(level, voet).equals(voet.above(2)), "the top of the pole");
        level.setBlock(voet.above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockState(voet.above()).getValue(VlaggenmastBlock.DEEL) == VlaggenmastBlock.Deel.TOP, "a shorter pole: the flag comes down a block");
        level.setBlock(voet.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockState(voet).getValue(VlaggenmastBlock.DEEL) == VlaggenmastBlock.Deel.LOS, "one block alone: foot and flag");
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 6));
        helper.assertTrue(klik(helper, p, voet) == InteractionResult.SUCCESS, "a click celebrates");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriobeloningScorebord(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(6, 2, 6));
        level.setBlock(pos, GuhrioBeloningFeature.SCOREBORD.get().defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockEntity(pos) instanceof ScorebordBlock.Entity, "the board's block entity");
        ServerPlayer p = speler(helper, new BlockPos(6, 2, 4));
        // a time of this player's own on a board no other test uses (the boards are the world's)
        String level32 = "kasteel_3_2";
        Scorebord.submit(p, GuhrioKasteel.bord(level32), 631, true);
        CompoundTag tijden = GuhrioSpel.spaar(p).getCompoundOrEmpty("Tijden");
        tijden.putInt(level32, 631);
        GuhrioSpel.spaar(p).put("Tijden", tijden);
        String tijd = GuhrioSpel.tijd(Scorebord.top(level.getServer(), GuhrioKasteel.bord(level32)).get(0).score());
        helper.assertTrue(NlTekst.tekst(ScorebordBlock.bord(level.getServer())).contains(tijd), "the record of 3-2 on the floating board");
        List<Component> eigen = ScorebordBlock.eigen(p);
        helper.assertTrue(eigen.size() == GuhrioKasteel.LEVELS.size() + 2 && NlTekst.tekst(eigen.get(6)).contains(GuhrioSpel.tijd(631)),
                "a line per level and one for the castle: " + eigen.size());
        helper.assertTrue(klik(helper, p, pos) == InteractionResult.SUCCESS && GidsFeature.heeft(p, "quest/guhrio_beloning_bord"), "a click shows your own times");
        AABB bij = new AABB(ScorebordBlock.plek(pos), ScorebordBlock.plek(pos)).inflate(1.5);
        helper.assertTrue(level.getEntitiesOfClass(Display.TextDisplay.class, bij).size() == 1, "one floating board");
        ScorebordBlock.toon(level, pos);
        helper.assertTrue(level.getEntitiesOfClass(Display.TextDisplay.class, bij).size() == 1, "still one");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getEntitiesOfClass(Display.TextDisplay.class, bij, Entity::isAlive).isEmpty(), "the board goes with its block");
            weg(helper, p);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // Pad-guh
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriobeloningPadguhWinkel(GameTestHelper helper) {
        GuhNpcEntity padguh = npc(helper, GuhNpcEntity.Kind.PADGUH, new BlockPos(6, 2, 6));
        ServerPlayer a = speler(helper, new BlockPos(6, 2, 4)), b = speler(helper, new BlockPos(5, 2, 4));
        helper.assertTrue(NpcRollen.van(padguh) instanceof Padguh, "Pad-guh's role");
        // the intro, once
        helper.assertTrue(LIJN.stap(a) == 0 && !LIJN.vlag(a, Padguh.ONTMOET), "A never met him");
        NpcRollen.van(padguh).talk(padguh, a);
        helper.assertTrue(LIJN.stap(a) == GuhrioBeloningFeature.STAP_GUHSHI && LIJN.vlag(a, Padguh.ONTMOET) && Padguh.SLEUTEL.equals(Praat.lopend(a))
                && GidsFeature.heeft(a, "quest/guhrio_beloning_padguh") && GidsFeature.heeft(a, "quest/guhrio_beloning_stap_1"), "the intro: step 1");
        helper.assertTrue(LIJN.stap(b) == 0, "B's line did not move");
        // too few coins
        Item pet = ModItems.clothingItem(GuhClothes.GUHRIOBELONING_RODE_PET);
        Padguh.antwoord(a, padguh, Padguh.KOOP + Winkel.Waar.RODE_PET.ordinal());
        helper.assertTrue(aantal(a, pet) == 0 && GuhrioSpel.munten(a) == 0 && Winkel.tekort(a, Winkel.Waar.RODE_PET) == Winkel.Waar.RODE_PET.prijs
                && !LIJN.vlag(a, Winkel.GEKOCHT), "no coins, no cap");
        // coins: an outfit and a building block
        Winkel.geef(a, 100);
        Padguh.antwoord(a, padguh, Padguh.OUTFITS);
        Padguh.antwoord(a, padguh, Padguh.KOOP + Winkel.Waar.RODE_PET.ordinal());
        helper.assertTrue(aantal(a, pet) == 1 && GuhrioSpel.munten(a) == 100 - Winkel.Waar.RODE_PET.prijs && LIJN.vlag(a, Winkel.GEKOCHT)
                && !LIJN.vlag(a, Winkel.BLOK_GEKOCHT) && GidsFeature.heeft(a, "quest/guhrio_beloning_gekocht"), "the red cap for " + Winkel.Waar.RODE_PET.prijs);
        Padguh.antwoord(a, padguh, Padguh.KOOP + Winkel.Waar.PIJP.ordinal());
        Padguh.antwoord(a, padguh, Padguh.KOOP + Winkel.Waar.VLAGGENMAST.ordinal());
        helper.assertTrue(aantal(a, GuhrioBeloningFeature.PIJP_ITEM.get()) == 2 && aantal(a, GuhrioBeloningFeature.VLAGGENMAST_ITEM.get()) == 4
                && GuhrioSpel.munten(a) == 100 - Winkel.Waar.RODE_PET.prijs - Winkel.Waar.PIJP.prijs - Winkel.Waar.VLAGGENMAST.prijs
                && LIJN.vlag(a, Winkel.BLOK_GEKOCHT), "two pipes and four flagpoles");
        helper.assertTrue(GuhrioSpel.munten(b) == 0 && b.getInventory().isEmpty(), "B's pocket is B's own");
        // an answer from somebody who walked away does nothing
        Padguh.antwoord(a, null, Padguh.KOOP + Winkel.Waar.SCHILD.ordinal());
        helper.assertTrue(aantal(a, ModItems.clothingItem(GuhClothes.GUHRIOBELONING_SCHILD)) == 0, "no shell from afar");
        // the running gag: once per finished world
        gehaald(a, "kasteel_1_1");
        NpcRollen.van(padguh).talk(padguh, a);
        helper.assertTrue(LIJN.eenmalig(a, "proef") && !wasGezegd(a, 1), "half a world: no thanks yet");
        gehaald(a, "kasteel_1_2");
        NpcRollen.van(padguh).talk(padguh, a);
        helper.assertTrue(wasGezegd(a, 1) && !wasGezegd(a, 2), "world 1: Bedankt! Maar de prinses is in een ander kasteeldeel");
        // the golden cap: all eighteen, once
        Item goud = ModItems.clothingItem(GuhClothes.GUHRIOBELONING_GOUDEN_PET);
        for (String level : GuhrioKasteel.LEVELS.subList(0, 5)) {
            vadsmunten(a, level, 7);
        }
        vadsmunten(a, GuhrioKasteel.LEVELS.get(5), 3);
        helper.assertTrue(!Padguh.goudenPet(a) && aantal(a, goud) == 0, "seventeen is not eighteen");
        vadsmunten(a, GuhrioKasteel.LEVELS.get(5), 7);
        NpcRollen.van(padguh).talk(padguh, a);
        helper.assertTrue(aantal(a, goud) == 1 && LIJN.vlag(a, Padguh.GOUDEN_PET) && GidsFeature.heeft(a, "quest/guhrio_beloning_gouden_pet")
                && GidsFeature.heeft(a, "guhrio/guhrio_beloning_gouden_pet"), "the golden cap");
        helper.assertTrue(!Padguh.goudenPet(a) && aantal(a, goud) == 1, "once");
        helper.assertTrue(LIJN.stap(a) == GuhrioBeloningFeature.STAP_GUHSHI, "the page waits for Guhshi before it counts the cap");
        padguh.discard();
        weg(helper, a, b);
        helper.succeed();
    }

    /** Did Pad-guh say his thanks for this world to this player (the once-only mark is used up)? */
    private static boolean wasGezegd(ServerPlayer p, int wereld) {
        // (eenmalig marks at the first call: asking again says whether the gag used it; a world never asked stays free)
        CompoundTag saved = GuhQuests.saved(p);
        for (String key : saved.keySet()) {
            if (key.startsWith("guhs_guhrio_beloning_") && key.endsWith("gag_" + wereld)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // Guhshi and the princess
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriobeloningGuhshiIsVanJou(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(6, 2, 3)), b = speler(helper, new BlockPos(4, 2, 3));
        GuhEntity kopie = (GuhEntity) Guhshi.kopie(level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 2, 8))), Rotation.NONE);
        level.addFreshEntity(kopie);
        GuhNpcEntity perzik = npc(helper, GuhNpcEntity.Kind.PERZIKGUH, new BlockPos(5, 2, 8));
        helper.assertTrue(VerhaalGuhs.kopieVan(kopie) == VerhaalGuh.GUHSHI && kopie.getVariant() == GuhVariant.GUHSHI && !Guhshi.Gedrag.eigen(kopie), "the copy");
        Item kroon = ModItems.clothingItem(GuhClothes.GUHRIOBELONING_PRINSESSENKROON);
        // before the duel: nothing
        helper.assertTrue(Guhshi.klikKopie(kopie, a, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && Praat.lopend(a) == null
                && Guhshi.geef(a, a.position()) == null, "no duel, no Guhshi");
        NpcRollen.van(perzik).talk(perzik, a);
        helper.assertTrue(aantal(a, kroon) == 0 && !LIJN.vlag(a, Perzikguh.BEDANKT), "and no crown");
        // A wins the duel
        GuhrioKasteel.winDuel(a);
        helper.assertTrue(VerhaalGuhs.magTemmen(a, VerhaalGuh.GUHSHI) && !VerhaalGuhs.isVrij(b, VerhaalGuh.GUHSHI), "A may take a Guhshi along, B not");
        Guhshi.klikKopie(kopie, a, InteractionHand.MAIN_HAND);
        helper.assertTrue(Guhshi.SLEUTEL.equals(Praat.lopend(a)), "may I come with you?");
        Guhshi.antwoord(a, kopie, Guhshi.NEE);
        helper.assertTrue(VerhaalGuhs.magTemmen(a, VerhaalGuh.GUHSHI), "not now: the chance stays");
        Guhshi.antwoord(a, kopie, Guhshi.JA);
        List<GuhEntity> eigen = level.getEntitiesOfClass(GuhEntity.class, kopie.getBoundingBox().inflate(4), g -> g != kopie && g.getVariant() == GuhVariant.GUHSHI);
        helper.assertTrue(eigen.size() == 1 && eigen.get(0).isOwnedBy(a) && eigen.get(0).isSaddled() && eigen.get(0).isRideable() && Guhshi.Gedrag.eigen(eigen.get(0))
                && !VerhaalGuhs.isKopie(eigen.get(0)), "A's own Guhshi: tamed, saddled, rideable");
        helper.assertTrue(kopie.isAlive() && VerhaalGuhs.heeftGetemd(a, VerhaalGuh.GUHSHI) && GidsFeature.heeft(a, "quest/guhrio_beloning_guhshi")
                && GidsFeature.heeft(a, "guhrio/guhrio_beloning_guhshi"), "the copy stays for the next player; the advancements");
        helper.assertTrue(Guhshi.geef(a, a.position()) == null, "once per player");
        Guhshi.antwoord(b, kopie, Guhshi.JA);
        helper.assertTrue(level.getEntitiesOfClass(GuhEntity.class, kopie.getBoundingBox().inflate(6), g -> g != kopie && g.getVariant() == GuhVariant.GUHSHI).size() == 1,
                "B did not win the duel: no Guhshi for B");
        // the page: Guhshi counts once Pad-guh is met
        helper.assertTrue(LIJN.stap(a) == GuhrioBeloningFeature.STAP_PADGUH, "A never met Pad-guh: the page is still at its first step");
        LIJN.vlag(a, Padguh.ONTMOET, true);
        GuhrioBeloningFeature.bijwerken(a);
        helper.assertTrue(LIJN.stap(a) == GuhrioBeloningFeature.STAP_GOUDEN_PET, "met Pad-guh and Guhshi tamed: on to the golden cap");
        // the princess: her thanks, the crown and a cake, once
        NpcRollen.van(perzik).talk(perzik, a);
        helper.assertTrue(aantal(a, kroon) == 1 && aantal(a, Items.CAKE) == 1 && LIJN.vlag(a, Perzikguh.BEDANKT) && GidsFeature.heeft(a, "quest/guhrio_beloning_kroon"),
                "the crown and a cake");
        NpcRollen.van(perzik).talk(perzik, a);
        helper.assertTrue(aantal(a, kroon) == 1 && aantal(a, Items.CAKE) == 1, "once");
        a.getInventory().clearContent();
        NpcRollen.van(perzik).talk(perzik, a);
        helper.assertTrue(aantal(a, kroon) == 1 && aantal(a, Items.CAKE) == 0, "lost before it was ever worn: a new crown (no second cake)");
        eigen.get(0).discard();
        kopie.discard();
        perzik.discard();
        VerhaalGuhs.vergeet(a, VerhaalGuh.GUHSHI);
        weg(helper, a, b);
        helper.succeed();
    }

    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void guhriobeloningGuhshiTongEnFladder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        VerhaalGuhs.geefVrij(p, VerhaalGuh.GUHSHI);
        GuhEntity guh = Guhshi.geef(p, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 2, 6))));
        helper.assertTrue(guh != null && Guhshi.Gedrag.magTong(guh) && VariantGedragen.van(guh) instanceof Guhshi.Gedrag, "a Guhshi of p's own");
        Guhshi.Gedrag gedrag = (Guhshi.Gedrag) VariantGedragen.van(guh);
        guh.setHealth(guh.getMaxHealth() - 6f);
        float voor = guh.getHealth();
        // three knabbels: one just dropped, one meant for somebody, and two that simply lie there
        ItemEntity vers = knabbel(helper, new BlockPos(8, 2, 6), 1);
        vers.setPickUpDelay(40);
        ItemEntity vanIemand = knabbel(helper, new BlockPos(6, 2, 8), 1);
        vanIemand.setNoPickUpDelay();
        vanIemand.setTarget(p.getUUID());
        helper.assertTrue(!Guhshi.Gedrag.tong(guh), "he leaves a fresh knabbel and somebody's knabbel alone");
        ItemEntity los = knabbel(helper, new BlockPos(6, 2, 9), 2);
        los.setNoPickUpDelay();
        helper.assertTrue(los.distanceTo(guh) < Guhshi.Gedrag.TONG, "within the tongue's reach");
        helper.assertTrue(Guhshi.Gedrag.tong(guh) && los.getItem().getCount() == 1 && guh.getHealth() > voor, "a lick: one knabbel gone, he feels better");
        helper.assertTrue(GidsFeature.heeft(p, "quest/guhrio_beloning_tong"), "the advancement");
        // the tongue rests between licks (the tick), and can be switched off
        gedrag.tick(guh);
        helper.assertTrue(los.getItem().getCount() == 1, "not again right away");
        helper.assertTrue("gui.guhs.guhriobeloning.guhshi.tong".equals(gedrag.speciaalKnop()), "his button");
        gedrag.speciaal(guh, p);
        helper.assertTrue(!Guhshi.Gedrag.magTong(guh), "tongue off");
        gedrag.speciaal(guh, p);
        helper.assertTrue(Guhshi.Gedrag.magTong(guh), "and on again");
        // a knabbel out of reach stays
        ItemEntity ver = knabbel(helper, new BlockPos(12, 2, 12), 1);
        ver.setNoPickUpDelay();
        helper.assertTrue(ver.distanceTo(guh) > Guhshi.Gedrag.TONG, "too far");
        los.discard();
        helper.assertTrue(!Guhshi.Gedrag.tong(guh) && ver.isAlive(), "five blocks and no further");
        // ridden: a jump on the jump key, a flutter while it is held in the air, once per jump
        p.startRiding(guh, true, false);
        helper.assertTrue(guh.getControllingPassenger() == p && !Guhshi.Gedrag.magTong(guh), "p rides him (and he does not lick with somebody on his back)");
        try {
            guh.setOnGround(true);
            guh.setDeltaMovement(Vec3.ZERO);
            gedrag.travel(guh, Vec3.ZERO);
            helper.assertTrue(guh.getDeltaMovement().y == 0, "no key, no jump");
            Guhshi.Gedrag.testSpring = true;
            gedrag.travel(guh, Vec3.ZERO);
            helper.assertTrue(guh.getDeltaMovement().y == Guhshi.Gedrag.SPRONG, "the jump");
            guh.setOnGround(false);
            guh.setDeltaMovement(0, -0.4, 0);
            for (int i = 0; i < 6; i++) {
                gedrag.travel(guh, Vec3.ZERO);
            }
            double y = guh.getDeltaMovement().y;
            helper.assertTrue(Guhshi.Gedrag.fladderTicks(guh) == 6 && y > -0.1 && y <= Guhshi.Gedrag.FLADDER_STIJG, "fluttering: the fall stops: " + y);
            for (int i = 0; i < Guhshi.Gedrag.FLADDER_TICKS + 10; i++) {
                guh.setDeltaMovement(0, -0.4, 0);
                gedrag.travel(guh, Vec3.ZERO);
            }
            helper.assertTrue(Guhshi.Gedrag.fladderTicks(guh) == Guhshi.Gedrag.FLADDER_TICKS && guh.getDeltaMovement().y == -0.4, "the flutter is used up: he falls again");
            guh.setOnGround(true);
            Guhshi.Gedrag.testSpring = false;
            gedrag.travel(guh, Vec3.ZERO);
            helper.assertTrue(Guhshi.Gedrag.fladderTicks(guh) == 0, "on the ground the flutter is back");
        } finally {
            Guhshi.Gedrag.testSpring = false;
        }
        p.stopRiding();
        vers.discard();
        vanIemand.discard();
        ver.discard();
        guh.discard();
        VerhaalGuhs.vergeet(p, VerhaalGuh.GUHSHI);
        weg(helper, p);
        helper.succeed();
    }

    private static ItemEntity knabbel(GameTestHelper helper, BlockPos at, int aantal) {
        BlockPos abs = helper.absolutePos(at);
        ItemEntity item = new ItemEntity(helper.getLevel(), abs.getX() + 0.5, abs.getY() + 0.1, abs.getZ() + 0.5, new ItemStack(ModItems.KAAS_KNABBELS.get(), aantal),
                0, 0, 0);
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    // =====================================================================================================================
    // the Guhdex page, the texts, the castle
    // =====================================================================================================================

    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriobeloningGuhdexEnTeksten(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(6, 2, 6));
        vadsmunten(p, "kasteel_2_1", 5);
        VerhaalStand stand = LIJN.stand(p);
        helper.assertTrue(stand.nodig().size() == GuhrioKasteel.VADSMUNTEN, "eighteen vadsmunten on the page: " + stand.nodig().size());
        // level 2-1 is the third level: its rows are 6, 7, 8; the first and the third are ticked
        helper.assertTrue(stand.nodig().get(6).genoeg() && !stand.nodig().get(7).genoeg() && stand.nodig().get(8).genoeg()
                && stand.nodig().stream().filter(VerhaalStand.Nodig::genoeg).count() == 2, "which ones of 2-1");
        helper.assertTrue(stand.beloningen().size() == 2 + GuhrioBeloningFeature.OUTFITS.size() && stand.beloningen().stream().noneMatch(VerhaalStand.Beloning::binnen),
                "Guhshi, five outfits, the building blocks: nothing yet");
        for (int w = 1; w <= 3; w++) {
            for (int n = 1; n <= 2; n++) {
                for (int i = 0; i < 3; i++) {
                    helper.assertTrue(NlTekst.has("gui.guhs.guhriobeloning.vads." + w + "_" + n + "." + i), "the text of vadsmunt " + w + "-" + n + " " + i);
                }
            }
        }
        // the shop: every button names its price; the outfits have their source and price
        for (Winkel.Waar waar : Winkel.Waar.values()) {
            helper.assertTrue(NlTekst.has(waar.knop()) && NlTekst.get(waar.knop()).contains("(" + waar.prijs + " munten)"), "the button of " + waar.id + " names its price");
            helper.assertTrue(!waar.stack().isEmpty(), waar.id + " is a thing");
        }
        helper.assertTrue(Winkel.Waar.plank(true).size() == 3 && Winkel.Waar.plank(false).size() == 3, "three on each shelf (the talking screen has room for three)");
        helper.assertTrue(KledingBronnen.van(GuhrioBeloningFeature.BRON).equals(List.of(GuhClothes.GUHRIOBELONING_RODE_PET, GuhClothes.GUHRIOBELONING_GROENE_PET,
                GuhClothes.GUHRIOBELONING_SCHILD, GuhClothes.GUHRIOBELONING_PRINSESSENKROON, GuhClothes.GUHRIOBELONING_GOUDEN_PET)), "the five outfits of the castle");
        for (GuhClothes c : GuhrioBeloningFeature.OUTFITS) {
            helper.assertTrue(KledingBronnen.prijs(c) != null && NlTekst.has("item.guhs." + c.id()) && NlTekst.has("gui.guhs.guhriobeloning.beloning." + c.id()),
                    "price and texts of " + c.id());
        }
        for (String key : List.of("quest.guhs.guhriobeloning.padguh.intro.1", "quest.guhs.guhriobeloning.padguh.gag", "quest.guhs.guhriobeloning.perzik.dank.3",
                "quest.guhs.guhriobeloning.guhshi.vraag", "gui.guhs.guhriobeloning.pijp.geen_partner", "gui.guhs.guhriobeloning.bord.eigen.regel",
                "gui.guhs.guhriobeloning.wereld.3", "gui.guhs.guhriobeloning.guhshi.tong", "gui.guhs.kledingbron.guhrio_beloning", "entity.guhs.guh_npc.padguh",
                "entity.guhs.guh_npc.perzikguh")) {
            helper.assertTrue(NlTekst.has(key), "text " + key);
        }
        helper.assertTrue(NlTekst.get("quest.guhs.guhriobeloning.padguh.gag").startsWith("Bedankt! Maar de prinses is in een ander kasteeldeel, njeg."), "the running gag");
        helper.assertTrue("●○●".equals(Padguh.stippen(5)), "the dots of the vadsmunten");
        weg(helper, p);
        helper.succeed();
    }

    /** The forecourt and the tower room as the castle's tiles hold them (whole-build coordinates, tiles of 32). */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void guhriobeloningKasteel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructurePlaceSettings zo = new StructurePlaceSettings();
        int borden = 0, monden = 0, masttoppen = 0, vraagblokken = 0;
        for (int i = 1; i <= 2; i++) {
            Optional<StructureTemplate> tegel = level.getStructureManager().get(Guhs.id(GuhrioKasteel.STRUCTUUR + "/stuk_" + i + "_3"));
            helper.assertTrue(tegel.isPresent(), "the castle's tile " + i + ", 3");
            borden += tegel.get().filterBlocks(BlockPos.ZERO, zo, GuhrioBeloningFeature.SCOREBORD.get()).size();
            vraagblokken += tegel.get().filterBlocks(BlockPos.ZERO, zo, GuhrioBeloningFeature.VRAAGBLOK.get()).size();
            for (StructureTemplate.StructureBlockInfo info : tegel.get().filterBlocks(BlockPos.ZERO, zo, GuhrioBeloningFeature.PIJP.get())) {
                helper.assertTrue(info.state().getValue(PijpBlock.KLEUR) == DyeColor.GREEN, "the forecourt's pipes are green");
                monden += info.state().getValue(PijpBlock.MOND) ? 1 : 0;
            }
            for (StructureTemplate.StructureBlockInfo info : tegel.get().filterBlocks(BlockPos.ZERO, zo, GuhrioBeloningFeature.VLAGGENMAST.get())) {
                masttoppen += info.state().getValue(VlaggenmastBlock.DEEL) == VlaggenmastBlock.Deel.TOP ? 1 : 0;
            }
        }
        helper.assertTrue(borden == 1 && monden == 2 && masttoppen == 1 && vraagblokken == 2,
                "the forecourt: " + borden + " board, " + monden + " pipe mouths, " + masttoppen + " flagpole, " + vraagblokken + " ?-blocks");
        // Pad-guh's spot is on the forecourt, the princess and Guhshi are in the tower room (x 53..73, z 58..70, floor y 42)
        BlockPos pad = GuhrioBeloningFeature.PADGUH_PLEK, perzik = GuhrioBeloningFeature.PERZIKGUH_PLEK, guhshi = GuhrioBeloningFeature.GUHSHI_PLEK;
        helper.assertTrue(pad.getX() >= 44 && pad.getX() <= 82 && pad.getZ() >= 112 && pad.getZ() <= 123 && pad.getY() >= 27, "Pad-guh on the forecourt");
        for (BlockPos plek : List.of(perzik, guhshi)) {
            helper.assertTrue(plek.getX() >= 53 && plek.getX() <= 73 && plek.getZ() >= 58 && plek.getZ() <= 70 && plek.getY() == 43, "in the tower room: " + plek);
        }
        helper.succeed();
    }
}
