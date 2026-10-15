package nl.juiced.guhs.feature.fossielmijn;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuePutStructure;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Herstel;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.GegarandeerdPlacement;
import nl.juiced.guhs.world.grond.GrondPoolElement;

/**
 * bbq2 (fossiel-mijn): the Fossiel-opgraving and the Zoutkristalmijn. What matters most is tested with two players at the
 * same blocks: each has a turn of their own and the world stays as it was (the bottenzand, the stand, the vein), or comes
 * back by itself (the rubble). The test server has no Guhbarbecuether: the worldgen side is checked through the registries
 * (the structures, their sets, the pool's ground level, the ore in the biomes) and the templates themselves.
 * Template fossielmijn_test_kamer: 16 x 16 houtskoolsteen at y 0 (the floor is helper y 1, things stand at helper y 2).
 */
public class FossielmijnGameTests {
    private static final String KAMER = "fossielmijn_test_kamer";
    private static final String BATCH = "fossielmijn";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        FossielmijnFeature.ARCHEOLOOG.wis(p);
        FossielmijnFeature.MIJNWERKER.wis(p);
        Opgraving.wis(p);
        GuhQuests.saved(p).remove(Zoutmijn.VOORRAAD);
        GuhQuests.saved(p).remove(Zoutmijn.VOORRAAD_TIJD);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        BlockPos abs = helper.absolutePos(at);
        npc.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(npc);
        return npc;
    }

    private static int tel(ServerPlayer p, Item item) {
        return p.getInventory().countItem(item);
    }

    /** Brushes this spot until it gives (or refuses) something. */
    private static boolean kwast(ServerPlayer p, BlockPos pos) {
        boolean gevonden = false;
        for (int i = 0; i < Opgraving.STREKEN; i++) {
            gevonden = Opgraving.strijk(p, pos);
        }
        return gevonden;
    }

    // =================================================================================================================
    // the Fossiel-opgraving
    // =================================================================================================================

    /**
     * Two players at the same dig: the sand gives each their own five bones (tail first, skull last), nothing changes in the
     * world, a brushed spot is empty for whoever brushed it only, the stand counts per player, the reward comes once, a lost
     * bone comes back, and after the quest every spot has one small find per day.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void fossielmijnOpgravingPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(2, 2, 2)), b = speler(helper, new BlockPos(3, 2, 2));
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.ARCHEOLOOGGUH, new BlockPos(12, 2, 12));
        try {
            Verhaallijn lijn = FossielmijnFeature.ARCHEOLOOG;
            BlockPos[] zand = new BlockPos[7];
            for (int i = 0; i < zand.length; i++) {
                helper.setBlock(new BlockPos(2 + i, 1, 6), FossielmijnFeature.BOTTENZAND.get());
                zand[i] = helper.absolutePos(new BlockPos(2 + i, 1, 6));
            }
            helper.setBlock(new BlockPos(8, 2, 10), FossielmijnFeature.SKELETREK.get());
            BlockPos rek = helper.absolutePos(new BlockPos(8, 2, 10));
            helper.assertTrue(level.getBlockEntity(zand[0]) instanceof FossielmijnBlocks.TekenBlockEntity
                    && level.getBlockEntity(rek) instanceof FossielmijnBlocks.TekenBlockEntity, "both are drawn per player");
            var rol = NpcRollen.van(npc);
            helper.assertTrue(rol instanceof Opgraving.Rol, "the Archeoloog-guh has his role");

            // not begun: the sand gives nothing, the stand is empty
            helper.assertTrue(!kwast(a, zand[0]) && Opgraving.gevonden(a) == 0 && !Opgraving.isGekwast(a, zand[0]), "nothing before the talk");
            rol.talk(npc, a);
            helper.assertTrue(lijn.stap(a) == 1 && lijn.stap(b) == 0 && tel(a, FossielmijnFeature.KWASTJE.get()) == 1, "a began and has the brush");
            // three strokes: nothing yet; the fourth finds the tail
            for (int i = 0; i < Opgraving.STREKEN - 1; i++) {
                helper.assertTrue(!Opgraving.strijk(a, zand[0]), "not yet");
            }
            helper.assertTrue(Opgraving.strijk(a, zand[0]) && tel(a, FossielmijnFeature.BOTTEN.get(4).get()) == 1 && Opgraving.gevonden(a) == 1,
                    "the fourth stroke finds the tail");
            helper.assertTrue(level.getBlockState(zand[0]) == FossielmijnFeature.BOTTENZAND.get().defaultBlockState(), "the sand itself is unchanged");
            helper.assertTrue(Opgraving.isGekwast(a, zand[0]) && !Opgraving.isGekwast(b, zand[0]), "brushed empty for a only");
            helper.assertTrue(!kwast(a, zand[0]) && Opgraving.gevonden(a) == 1, "the same spot gives a nothing more");
            // the real thing: a looks down at a spot of sand and holds the brush (the event of every tick of brushing)
            BlockPos boven = zand[1];
            a.snapTo(boven.getX() + 0.5, boven.getY() + 1.0, boven.getZ() + 0.5, 0f, 90f);
            ItemStack kwastje = new ItemStack(FossielmijnFeature.KWASTJE.get());
            int duur = kwastje.getUseDuration(a);
            for (int streek = 0; streek < Opgraving.STREKEN; streek++) {
                NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Tick(a, kwastje, duur - 4 - streek * 10));
                NeoForge.EVENT_BUS.post(new LivingEntityUseItemEvent.Tick(a, kwastje, duur - 5 - streek * 10));   // (not a stroke)
            }
            helper.assertTrue(Opgraving.gevonden(a) == 2 && tel(a, FossielmijnFeature.BOTTEN.get(3).get()) == 1, "brushing by looking at the sand");
            for (int i = 2; i < 5; i++) {
                helper.assertTrue(kwast(a, zand[i]), "bone " + (i + 1));
            }
            helper.assertTrue(Opgraving.gevonden(a) == 5 && lijn.stap(a) == 2, "five bones: on to the stand");
            for (int i = 0; i < 5; i++) {
                helper.assertTrue(tel(a, FossielmijnFeature.BOTTEN.get(i).get()) == 1, "one of each bone, the skull last");
            }

            // b at the very same spots: a turn of their own
            rol.talk(npc, b);
            for (int i = 0; i < 5; i++) {
                helper.assertTrue(kwast(b, zand[i]), "b finds a bone where a already did");
            }
            helper.assertTrue(Opgraving.gevonden(b) == 5 && lijn.stap(b) == 2, "b has five too");

            // the stand: per player, any order, the one in the hand first
            Opgraving.zetOpRek(b, rek);
            helper.assertTrue(Integer.bitCount(Opgraving.geplaatst(b)) == 1 && Opgraving.geplaatst(a) == 0, "b put one on, a's stand is still empty");
            a.getInventory().clearContent();
            a.getInventory().setSelectedSlot(0);
            int[] vak = {1, 2, 0, 3, 4};                                 // (the ribs in the hand, the others in the pockets)
            for (int i = 0; i < 5; i++) {
                a.getInventory().setItem(vak[i], new ItemStack(FossielmijnFeature.BOTTEN.get(i).get()));
            }
            Opgraving.zetOpRek(a, rek);
            helper.assertTrue(Opgraving.geplaatst(a) == (1 << 2) && tel(a, FossielmijnFeature.BOTTEN.get(2).get()) == 0, "the bone in a's hand went on first");
            for (int i = 0; i < 3; i++) {
                Opgraving.zetOpRek(a, rek);
            }
            helper.assertTrue(Integer.bitCount(Opgraving.geplaatst(a)) == 4 && lijn.stap(a) == 2, "four on: not done yet");
            // a lost the last bone: the Archeoloog-guh has a cast of it
            int over = Integer.numberOfTrailingZeros(~Opgraving.geplaatst(a) & Opgraving.ALLES);
            Item laatste = FossielmijnFeature.BOTTEN.get(over).get();
            a.getInventory().clearContent();
            Opgraving.zetOpRek(a, rek);
            helper.assertTrue(Integer.bitCount(Opgraving.geplaatst(a)) == 4, "no bone, nothing goes on");
            rol.talk(npc, a);
            helper.assertTrue(tel(a, laatste) == 1, "the lost bone is given again");
            Opgraving.zetOpRek(a, rek);
            helper.assertTrue(Opgraving.geplaatst(a) == Opgraving.ALLES && lijn.stap(a) == 3, "all five on: tell him");
            helper.assertTrue(level.getBlockState(rek) == FossielmijnFeature.SKELETREK.get().defaultBlockState(), "the stand itself is unchanged");
            rol.talk(npc, a);
            helper.assertTrue(lijn.klaar(a) && tel(a, FossielmijnFeature.FOSSIELBEELDJE_ITEM.get()) == 1 && tel(a, FossielmijnFeature.KWASTJE.get()) == 1,
                    "done: the statuette, and the brush is back in a's pockets");
            rol.talk(npc, a);
            helper.assertTrue(tel(a, FossielmijnFeature.FOSSIELBEELDJE_ITEM.get()) == 1 && !lijn.klaar(b), "the reward only once; b is where b was");

            // b goes on now that a is done: the stand that is full for a still has four empty hooks for b
            helper.assertTrue(Integer.bitCount(Opgraving.geplaatst(b)) == 1 && lijn.stap(b) == 2, "b's stand did not move while a finished");
            for (int i = 0; i < 4; i++) {
                Opgraving.zetOpRek(b, rek);
            }
            helper.assertTrue(Opgraving.geplaatst(b) == Opgraving.ALLES && lijn.stap(b) == 3, "b's skeleton is whole too");
            rol.talk(npc, b);
            helper.assertTrue(lijn.klaar(b) && tel(b, FossielmijnFeature.FOSSIELBEELDJE_ITEM.get()) == 1 && tel(b, FossielmijnFeature.KWASTJE.get()) == 1,
                    "the second player finishes after the first, with the same reward");
            // and somebody who only arrives now: the same sand still holds five bones for them, the same stand is empty for them
            ServerPlayer c = speler(helper, new BlockPos(4, 2, 2));
            try {
                rol.talk(npc, c);
                helper.assertTrue(lijn.stap(c) == 1 && Opgraving.gevonden(c) == 0 && !Opgraving.isGekwast(c, zand[0]), "a newcomer begins with untouched sand");
                for (int i = 0; i < 5; i++) {
                    helper.assertTrue(kwast(c, zand[i]), "the newcomer finds bone " + (i + 1) + " where a and b found theirs");
                }
                for (int i = 0; i < 5; i++) {
                    Opgraving.zetOpRek(c, rek);
                }
                rol.talk(npc, c);
                helper.assertTrue(lijn.klaar(c) && tel(c, FossielmijnFeature.FOSSIELBEELDJE_ITEM.get()) == 1, "and is done: any number of players, one after the other");
                helper.assertTrue(level.getBlockState(zand[0]) == FossielmijnFeature.BOTTENZAND.get().defaultBlockState()
                        && level.getBlockState(rek) == FossielmijnFeature.SKELETREK.get().defaultBlockState(), "sand and stand are still what they were");
            } finally {
                weg(helper, c);
            }

            // the daily finds: every spot once a day, per player
            a.getInventory().clearContent();
            helper.assertTrue(kwast(a, zand[5]) && !a.getInventory().isEmpty(), "a find in a spot a had not brushed today");
            helper.assertTrue(!kwast(a, zand[5]) && !kwast(a, zand[0]), "once per spot per day (the quest's own spots count for today)");
            GuhQuests.saved(a).putLong(Opgraving.DAG, Opgraving.dag(a) - 1);
            helper.assertTrue(kwast(a, zand[0]) && kwast(a, zand[5]), "a new day: the sand has something again");
            for (int i = 0; i < 40; i++) {
                ItemStack vondst = Opgraving.dagvondst(a);
                helper.assertTrue(!vondst.isEmpty() && vondst.getCount() <= 4, "a daily find is modest: " + vondst);
            }
        } finally {
            npc.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the Zoutkristalmijn
    // =================================================================================================================

    /**
     * The vein never breaks and gives every player salt from their own stock: nothing before the track is clear, nothing
     * without a pickaxe, two per hit with the Zoutkristalhouweel, empty at zero, growing back with time up to the maximum.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void fossielmijnAderPerSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(6, 2, 6)), b = speler(helper, new BlockPos(7, 2, 6));
        try {
            Verhaallijn lijn = FossielmijnFeature.MIJNWERKER;
            Item zout = FossielmijnFeature.ZOUTKRISTAL.get();
            helper.setBlock(new BlockPos(6, 2, 8), FossielmijnFeature.ZOUTADER.get());
            BlockPos ader = helper.absolutePos(new BlockPos(6, 2, 8));
            BlockState state = level.getBlockState(ader);
            a.getInventory().setSelectedSlot(0);
            a.getInventory().setItem(0, new ItemStack(Items.IRON_PICKAXE));
            a.gameMode.destroyBlock(ader);
            helper.assertTrue(level.getBlockState(ader) == state && tel(a, zout) == 0, "the track is not clear yet: nothing, and the vein stays");
            lijn.zet(a, 2);
            helper.assertTrue(Zoutmijn.voorraad(a) == Zoutmijn.VOORRAAD_MAX, "a full stock for someone who was never here");
            a.gameMode.destroyBlock(ader);
            helper.assertTrue(level.getBlockState(ader) == state && tel(a, zout) == 1 && lijn.stap(a) == 3 && Zoutmijn.voorraad(a) == Zoutmijn.VOORRAAD_MAX - 1,
                    "hacked loose: one crystal, the vein is found, the block is still there");
            a.getInventory().setItem(0, ItemStack.EMPTY);
            a.gameMode.destroyBlock(ader);
            helper.assertTrue(tel(a, zout) == 1 && Zoutmijn.voorraad(a) == Zoutmijn.VOORRAAD_MAX - 1, "bare paws: nothing");
            a.getInventory().setItem(0, new ItemStack(FossielmijnFeature.ZOUTKRISTALHOUWEEL.get()));
            a.gameMode.destroyBlock(ader);
            helper.assertTrue(tel(a, zout) == 3 && Zoutmijn.voorraad(a) == Zoutmijn.VOORRAAD_MAX - 2, "the Zoutkristalhouweel takes two per hit");
            // empty: nothing more, until it grew back
            Zoutmijn.zetVoorraad(a, 1);
            helper.assertTrue(Zoutmijn.hak(a, ader, state, a.getMainHandItem()) == 2 && Zoutmijn.voorraad(a) == 0, "the last one");
            helper.assertTrue(Zoutmijn.hak(a, ader, state, a.getMainHandItem()) == 0 && tel(a, zout) == 5, "the vein is empty for a");
            Zoutmijn.verschuif(a, Zoutmijn.AANGROEI_TICKS * 3L + 10);
            helper.assertTrue(Zoutmijn.voorraad(a) == 3, "three crystals grew back: " + Zoutmijn.voorraad(a));
            helper.assertTrue(Zoutmijn.hak(a, ader, state, a.getMainHandItem()) == 2 && Zoutmijn.voorraad(a) == 2, "and can be hacked again");
            Zoutmijn.verschuif(a, Zoutmijn.AANGROEI_TICKS * 1000L);
            helper.assertTrue(Zoutmijn.voorraad(a) == Zoutmijn.VOORRAAD_MAX, "never more than the maximum");
            // b at the same block: untouched by what a took
            lijn.zet(b, 2);
            Zoutmijn.zetVoorraad(a, 0);
            b.getInventory().setSelectedSlot(0);
            b.getInventory().setItem(0, new ItemStack(Items.STONE_PICKAXE));
            b.gameMode.destroyBlock(ader);
            helper.assertTrue(tel(b, zout) == 1 && Zoutmijn.voorraad(b) == Zoutmijn.VOORRAAD_MAX - 1 && Zoutmijn.voorraad(a) == 0,
                    "b has a stock of their own while a's is empty");
            // a creative player really breaks it (builders)
            b.setGameMode(GameType.CREATIVE);
            b.gameMode.destroyBlock(ader);
            helper.assertTrue(level.getBlockState(ader).isAir(), "creative breaks it");
        } finally {
            weg(helper, a, b);
        }
        helper.succeed();
    }

    /**
     * The rubble: hacked away it leaves the rail it lay on and counts for the player who is clearing the track; five lumps
     * are the step; a minute later it has fallen back. And the Mijnwerker-guh's own talk: the steps, the salt for his
     * sandwich, the pickaxe once.
     */
    @GuhTest(template = KAMER, batch = "fossielmijn_puin")
    public static void fossielmijnPuinEnMijnwerker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(2, 2, 2)), b = speler(helper, new BlockPos(3, 2, 2));
        GuhNpcEntity npc = npc(helper, GuhNpcEntity.Kind.MIJNWERKERGUH, new BlockPos(12, 2, 12));
        try {
            Verhaallijn lijn = FossielmijnFeature.MIJNWERKER;
            var rol = NpcRollen.van(npc);
            helper.assertTrue(rol instanceof Zoutmijn.Rol, "the Mijnwerker-guh has his role");
            BlockPos[] puin = new BlockPos[6];
            for (int i = 0; i < puin.length; i++) {
                helper.setBlock(new BlockPos(3 + i, 2, 8), FossielmijnFeature.PUIN.get().defaultBlockState().setValue(FossielmijnBlocks.Puin.AXIS, Direction.Axis.X));
                puin[i] = helper.absolutePos(new BlockPos(3 + i, 2, 8));
            }
            BlockState ligt = level.getBlockState(puin[0]);
            // b did not talk to him: the rubble goes, nothing is counted
            b.gameMode.destroyBlock(puin[5]);
            helper.assertTrue(level.getBlockState(puin[5]).is(Blocks.RAIL) && lijn.teller(b, Zoutmijn.PUIN) == 0 && lijn.stap(b) == 0, "b only cleared a lump");
            rol.talk(npc, a);
            helper.assertTrue(lijn.stap(a) == 1, "a began");
            a.gameMode.destroyBlock(puin[0]);
            BlockState rail = level.getBlockState(puin[0]);
            helper.assertTrue(rail.is(Blocks.RAIL) && rail.getValue(RailBlock.SHAPE) == RailShape.EAST_WEST, "the rail under the rubble, the way the track runs: " + rail);
            helper.assertTrue(lijn.teller(a, Zoutmijn.PUIN) == 1 && Herstel.wacht(level, puin[0]), "counted, and it will fall back");
            AABB kamer = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(16, 8, 16);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, kamer).isEmpty(), "rubble drops nothing");
            for (int i = 1; i < 5; i++) {
                a.gameMode.destroyBlock(puin[i]);
            }
            helper.assertTrue(lijn.stap(a) == 2, "five lumps: the track is clear for a");
            Herstel.verschuif(level, FossielmijnBlocks.Puin.TERUG);
            Herstel.verwerk(level);
            helper.assertTrue(level.getBlockState(puin[0]) == ligt && level.getBlockState(puin[5]) == ligt && !Herstel.wacht(level, puin[0]),
                    "a minute later the rubble lies there again, for the next player");
            // the rest of his questline
            helper.setBlock(new BlockPos(8, 2, 12), FossielmijnFeature.ZOUTADER.get());
            BlockPos ader = helper.absolutePos(new BlockPos(8, 2, 12));
            helper.assertTrue(Zoutmijn.hak(a, ader, level.getBlockState(ader), new ItemStack(Items.IRON_PICKAXE)) == 1 && lijn.stap(a) == 3, "the vein is found");
            a.getInventory().clearContent();
            rol.talk(npc, a);
            helper.assertTrue(lijn.stap(a) == 3, "no salt for his sandwich yet");
            a.getInventory().add(new ItemStack(FossielmijnFeature.ZOUTKRISTAL.get(), 5));
            rol.talk(npc, a);
            helper.assertTrue(lijn.klaar(a) && tel(a, FossielmijnFeature.ZOUTKRISTAL.get()) == 5 - Zoutmijn.BOTERHAM
                    && tel(a, FossielmijnFeature.ZOUTKRISTALHOUWEEL.get()) == 1, "three crystals for his sandwich, the pickaxe for a");
            helper.assertTrue(rol.offers(npc) != null && !rol.offers(npc).isEmpty(), "and he sells a new one");
            helper.assertTrue(!FossielmijnFeature.MIJNWERKER.stand(a).beloningen().isEmpty() && !Zoutmijn.nodig(b, 1).isEmpty()
                    && !Opgraving.nodig(b, 1).isEmpty(), "the Guhdex knows what is needed and what the rewards are");
            // b starts after a is done: the rubble that a cleared lies there again, the vein gives b a stock of their own
            rol.talk(npc, b);
            helper.assertTrue(lijn.stap(b) == 1 && lijn.teller(b, Zoutmijn.PUIN) == 0, "b begins: the lump b hacked before the talk never counted");
            // (a is clearing at the same time would leave b short for a minute at most: every lump is back 1200 ticks after it went)
            b.gameMode.destroyBlock(puin[0]);
            b.gameMode.destroyBlock(puin[1]);
            Herstel.verschuif(level, FossielmijnBlocks.Puin.TERUG);
            Herstel.verwerk(level);
            helper.assertTrue(level.getBlockState(puin[0]) == ligt && lijn.teller(b, Zoutmijn.PUIN) == 2, "two lumps counted for b, and they are back for whoever comes next");
            for (int i = 0; i < 3; i++) {
                b.gameMode.destroyBlock(puin[i]);
            }
            helper.assertTrue(lijn.stap(b) == 2, "the same lumps again: five in all, the track is clear for b");
            Zoutmijn.zetVoorraad(a, 0);
            helper.assertTrue(Zoutmijn.hak(b, ader, level.getBlockState(ader), new ItemStack(Items.IRON_PICKAXE)) == 1 && lijn.stap(b) == 3
                    && Zoutmijn.voorraad(b) == Zoutmijn.VOORRAAD_MAX - 1, "b finds the vein, also when a emptied theirs");
            for (int i = 0; i < Zoutmijn.BOTERHAM - 1; i++) {
                Zoutmijn.hak(b, ader, level.getBlockState(ader), new ItemStack(Items.IRON_PICKAXE));
            }
            rol.talk(npc, b);
            helper.assertTrue(lijn.klaar(b) && tel(b, FossielmijnFeature.ZOUTKRISTALHOUWEEL.get()) == 1 && tel(a, FossielmijnFeature.ZOUTKRISTALHOUWEEL.get()) == 1,
                    "the second player finishes the whole line after the first, with the same reward");
            Herstel.verschuif(level, FossielmijnBlocks.Puin.TERUG);
            Herstel.verwerk(level);
        } finally {
            npc.discard();
            weg(helper, a, b);
        }
        helper.succeed();
    }

    /** Zoutkristalerts drops zoutkristal for a pickaxe; the little crystals need something to stand on. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void fossielmijnErtsEnKristalletjes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 3));
        try {
            helper.setBlock(new BlockPos(12, 2, 5), FossielmijnFeature.ZOUTKRISTALERTS.get());
            BlockPos erts = helper.absolutePos(new BlockPos(12, 2, 5));
            p.getInventory().setSelectedSlot(0);
            p.getInventory().setItem(0, new ItemStack(Items.IRON_PICKAXE));
            p.gameMode.destroyBlock(erts);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(erts).inflate(2), e -> e.getItem().is(FossielmijnFeature.ZOUTKRISTAL.get()));
            helper.assertTrue(level.getBlockState(erts).isAir() && !drops.isEmpty(), "the ore breaks and drops zoutkristal");
            drops.forEach(Entity::discard);
            helper.setBlock(new BlockPos(14, 2, 5), Blocks.STONE);
            helper.setBlock(new BlockPos(14, 3, 5), FossielmijnFeature.ZOUTKRISTALLETJES.get());
            helper.assertTrue(FossielmijnFeature.ZOUTKRISTALLETJES.get().defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(14, 3, 5)))
                    && !FossielmijnFeature.ZOUTKRISTALLETJES.get().defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(14, 5, 5))),
                    "the little crystals stand on something solid, not in the air");
            helper.assertTrue(FossielmijnFeature.ZOUTKRISTALBLOK.get().defaultBlockState().getLightEmission(level, erts) > 0, "the crystal block gives light");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    // =================================================================================================================
    // the buildings: protection, the templates, the worldgen data
    // =================================================================================================================

    private static StructureStart kopie(GameTestHelper helper, String structuur, BlockPos hoek) {
        ServerLevel level = helper.getLevel();
        Structure structure = Kopieen.structuur(level, structuur);
        helper.assertTrue(structure != null, "the structure guhs:" + structuur + " exists");
        BlockPos pos = helper.absolutePos(hoek);
        StructurePoolElement element = StructurePoolElement.single("guhs:" + KAMER).apply(StructureTemplatePool.Projection.RIGID);
        BoundingBox box = element.getBoundingBox(level.getStructureManager(), pos, Rotation.NONE);
        StructurePiece piece = new PoolElementStructurePiece(level.getStructureManager(), element, pos, 0, Rotation.NONE, box, LiquidSettings.IGNORE_WATERLOGGING);
        StructureStart start = new StructureStart(structure, ChunkPos.containing(pos), 0, new PiecesContainer(List.of(piece)));
        Kopieen.test(level, start);
        return start;
    }

    /**
     * Inside a Zoutkristalmijn nothing can be broken, except the rubble (it falls back) and the vein (it never breaks);
     * and the NPC of the building comes back to a copy that lost it.
     */
    @GuhTest(template = KAMER, batch = "fossielmijn_gebouw")
    public static void fossielmijnBeschermdEnBewoond(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(1, 2, 1));
        try {
            StructureStart start = kopie(helper, FossielmijnFeature.MIJN, new BlockPos(0, 1, 0));
            helper.setBlock(new BlockPos(5, 2, 5), Blocks.STONE);
            helper.setBlock(new BlockPos(6, 2, 5), FossielmijnFeature.PUIN.get());
            helper.setBlock(new BlockPos(7, 2, 5), FossielmijnFeature.ZOUTADER.get());
            BlockPos steen = helper.absolutePos(new BlockPos(5, 2, 5)), puin = helper.absolutePos(new BlockPos(6, 2, 5)), ader = helper.absolutePos(new BlockPos(7, 2, 5));
            helper.assertTrue(Bescherming.beschermd(level, steen) && FossielmijnFeature.MIJN.equals(Bescherming.structuurBij(level, steen)), "the mine is protected");
            helper.assertTrue(!Bescherming.mag(p, steen) && Bescherming.mag(p, puin) && Bescherming.mag(p, ader), "only the rubble and the vein may be hacked");
            p.getInventory().setSelectedSlot(0);
            p.getInventory().setItem(0, new ItemStack(Items.IRON_PICKAXE));
            p.gameMode.destroyBlock(steen);
            p.gameMode.destroyBlock(puin);
            helper.assertTrue(level.getBlockState(steen).is(Blocks.STONE) && level.getBlockState(puin).is(Blocks.RAIL), "the wall stays, the rubble goes");
            Herstel.verschuif(level, FossielmijnBlocks.Puin.TERUG);
            Herstel.verwerk(level);
            helper.assertTrue(level.getBlockState(puin).is(FossielmijnFeature.PUIN.get()), "and falls back");
            // the Mijnwerker-guh belongs at his spot of every copy
            BlockPos plek = Bezetting.wereld(start, null, new BlockPos(8, 1, 8));
            helper.assertTrue(plek != null && Bezetting.start(level, FossielmijnFeature.MIJN, steen) == start, "the copy is found");
            helper.assertTrue(Bezetting.wereld(start, null, FossielmijnFeature.MIJNWERKER_PLEK) != null, "his spot maps into the copy");
        } finally {
            Kopieen.testWissen(level);
            weg(helper, p);
        }
        helper.succeed();
    }

    private static int tel(StructureTemplate template, Block block) {
        return template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).size();
    }

    private static boolean heeftNpc(StructureTemplate template, String id, BlockPos plek) {
        for (Tag t : template.save(new CompoundTag()).getListOrEmpty("entities")) {
            CompoundTag e = t.asCompound().orElse(new CompoundTag());
            var pos = e.getListOrEmpty("blockPos");
            String tag = e.getCompoundOrEmpty("nbt").getCompoundOrEmpty("NeoForgeData").getStringOr(Bezetting.TAG, "");
            if (tag.equals(id) && pos.getIntOr(0, -1) == plek.getX() && pos.getIntOr(1, -1) == plek.getY() && pos.getIntOr(2, -1) == plek.getZ()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The two templates hold what the questlines need (enough sand, one stand, enough rubble and vein), their NPC sits where
     * the Java side says, and the centre jigsaw is in layer 0. The worldgen data: both structures are cave buildings with a
     * guaranteed copy in new terrain, their start pool says where the ground is, and the ore is in every biome of the
     * Guhbarbecuether.
     */
    @GuhTest(template = "empty", batch = BATCH)
    public static void fossielmijnSjablonenEnWereld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate opgraving = level.getStructureManager().get(Guhs.id(FossielmijnFeature.OPGRAVING)).orElse(null);
        StructureTemplate mijn = level.getStructureManager().get(Guhs.id(FossielmijnFeature.MIJN)).orElse(null);
        helper.assertTrue(opgraving != null && mijn != null, "both templates exist");
        helper.assertTrue(tel(opgraving, FossielmijnFeature.BOTTENZAND.get()) >= 8 && tel(opgraving, FossielmijnFeature.SKELETREK.get()) == 1,
                "the dig: sand for everybody and one stand");
        helper.assertTrue(tel(mijn, FossielmijnFeature.PUIN.get()) >= 6 && tel(mijn, FossielmijnFeature.ZOUTADER.get()) >= 12
                && tel(mijn, Blocks.RAIL) >= 20, "the mine: rubble, a vein and a track");
        helper.assertTrue(heeftNpc(opgraving, FossielmijnFeature.ARCHEOLOOG_ID, FossielmijnFeature.ARCHEOLOOG_PLEK), "the Archeoloog-guh sits where Java says");
        helper.assertTrue(heeftNpc(mijn, FossielmijnFeature.MIJNWERKER_ID, FossielmijnFeature.MIJNWERKER_PLEK), "the Mijnwerker-guh sits where Java says");
        for (StructureTemplate t : List.of(opgraving, mijn)) {
            List<StructureTemplate.StructureBlockInfo> jigsaws = t.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW);
            helper.assertTrue(jigsaws.size() == 1 && jigsaws.get(0).pos().getY() == 0, "one centre jigsaw, in layer 0");
        }
        var pools = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        var sets = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        int[] grond = {4, 6};   // G + 1 of the dig and the mine (fossiel_mijn_bouw.py)
        String[] namen = {FossielmijnFeature.OPGRAVING, FossielmijnFeature.MIJN};
        for (int i = 0; i < namen.length; i++) {
            String naam = namen[i];
            helper.assertTrue(Kopieen.structuur(level, naam) instanceof BarbecuePutStructure, naam + " is a cave building");
            var pool = pools.getValue(Guhs.id(naam + "/start"));
            helper.assertTrue(pool != null, naam + ": its start pool");
            StructurePoolElement element = pool.getRandomTemplate(level.getRandom());
            helper.assertTrue(element instanceof GrondPoolElement g && g.groundLevelDelta() == grond[i], naam + ": the pool says where its ground is: " + element);
            helper.assertTrue(sets.getValue(Guhs.id(naam)) != null, naam + ": a spread set");
            var gegarandeerd = sets.getValue(Guhs.id(naam + "_gegarandeerd"));
            helper.assertTrue(gegarandeerd != null && gegarandeerd.placement() instanceof GegarandeerdPlacement g && g.alleenNieuw(),
                    naam + ": one guaranteed copy, in new terrain");
        }
        var erts = ResourceKey.create(Registries.PLACED_FEATURE, Guhs.id("fossielmijn_zoutkristalerts"));
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        for (String biome : List.of("houtskoolvlakte", "asdal", "satebos", "worstenwoud", "rookdelta")) {
            var b = biomes.getValue(Guhs.id(biome));
            helper.assertTrue(b != null && b.getGenerationSettings().features().stream().anyMatch(stap -> stap.stream().anyMatch(f -> f.is(erts))),
                    "zoutkristalerts generates in " + biome);
        }
        helper.succeed();
    }
}
