package nl.juiced.guhs.feature.bouwhulp;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Bibliotheek;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Blauwdruk;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Catalogus;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Voorbeeld;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Gereedschap;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Kwasten;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Opties;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Plaatsen;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Terrein;
import nl.juiced.guhs.feature.bouwhulp.gereedschap.Vastleggen;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.bouwhulp.vormen.Daken;
import nl.juiced.guhs.feature.bouwhulp.vormen.Familie;
import nl.juiced.guhs.feature.bouwhulp.vormen.Mix;
import nl.juiced.guhs.feature.bouwhulp.vormen.VormInstelling;
import nl.juiced.guhs.feature.bouwhulp.vormen.Vormen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Deel 2 F9: every shape and roof comes out (within the level's size, nature shapes from a Zoutkeet), a stair stays a
 * stair under the kwast, the kwasten/verfmachine/egaliseerder/tunnelboor change what they should, capturing finds a
 * building and keeps its rooms, the library keeps its limits, the catalogue is complete, and a kwast stroke really builds.
 */
public class BouwgereedschapGameTests {
    private static final String BATCH = "bouwgereedschap";
    private static final String VLOER = "px_test_16";

    private static VormInstelling vorm(Vormen.Vorm v, int b, int h, int d, boolean hol) {
        CompoundTag t = new CompoundTag();
        t.putInt("vorm", v.ordinal());
        t.putInt("b", b);
        t.putInt("h", h);
        t.putInt("d", d);
        t.putInt("hol", hol ? 1 : 0);
        return VormInstelling.van(t);
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 40)
    public static void bouwgereedschapVormen(GameTestHelper helper) {
        for (Vormen.Vorm v : Vormen.Vorm.values()) {
            Ontwerp hand = Vormen.maak(vorm(v, 30, 30, 30, true), BouwNiveau.HANDWERK);
            if (!v.mag(BouwNiveau.HANDWERK)) {
                helper.assertTrue(hand.isLeeg(), v + " is closed at handwerk");
                hand = Vormen.maak(vorm(v, 30, 30, 30, true), BouwNiveau.ZOUT);
                helper.assertTrue(hand.maat() <= BouwNiveau.ZOUT.maxVorm, v + " fits a Zoutkeet");
            } else {
                helper.assertTrue(hand.maat() <= BouwNiveau.HANDWERK.maxVorm, v + " is clamped to 16: " + hand.maat());
            }
            helper.assertTrue(!hand.isLeeg(), v + " makes blocks");
        }
        Ontwerp vol = Vormen.maak(vorm(Vormen.Vorm.BOL, 9, 9, 9, false), BouwNiveau.HANDWERK);
        Ontwerp hol = Vormen.maak(vorm(Vormen.Vorm.BOL, 9, 9, 9, true), BouwNiveau.HANDWERK);
        long holLucht = hol.blokken().values().stream().filter(BlockState::isAir).count();
        long volLucht = vol.blokken().values().stream().filter(BlockState::isAir).count();
        helper.assertTrue(holLucht > 50 && volLucht == 0, "a hollow ball is dug out inside (" + holLucht + "), a filled one not");
        helper.assertTrue(hol.blokken().get(new BlockPos(4, 4, 4)).isAir() && !vol.blokken().get(new BlockPos(4, 4, 4)).isAir(), "the middle");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 20)
    public static void bouwgereedschapDaken(GameTestHelper helper) {
        Map<BlockPos, BlockState> zadel = Daken.blokken(Daken.Dak.ZADEL, 7, 7, 0, true, Blocks.OAK_PLANKS, Blocks.WHITE_WOOL, Blocks.PINK_WOOL);
        boolean zuid = false, noord = false;
        for (BlockState s : zadel.values()) {
            if (s.getBlock() == Blocks.OAK_STAIRS) {
                zuid |= s.getValue(StairBlock.FACING) == Direction.SOUTH;
                noord |= s.getValue(StairBlock.FACING) == Direction.NORTH;
            }
        }
        helper.assertTrue(zuid && noord, "a gable roof: stairs up from both sides");
        helper.assertTrue(zadel.values().stream().anyMatch(s -> s.is(Blocks.WHITE_WOOL)), "with gables");
        helper.assertTrue(zadel.get(new BlockPos(0, 3, 3)).is(Blocks.OAK_SLAB), "a slab on the ridge (odd width)");
        Map<BlockPos, BlockState> schild = Daken.blokken(Daken.Dak.SCHILD, 7, 7, 0, false, Blocks.OAK_PLANKS, Blocks.WHITE_WOOL, Blocks.PINK_WOOL);
        helper.assertTrue(schild.values().stream().anyMatch(s -> s.getBlock() instanceof StairBlock && s.getValue(StairBlock.SHAPE) != StairsShape.STRAIGHT),
                "a hip roof has corner stairs");
        Map<BlockPos, BlockState> oor = Daken.blokken(Daken.Dak.OORTJES, 7, 7, 1, true, Blocks.OAK_PLANKS, Blocks.WHITE_WOOL, Blocks.PINK_WOOL);
        helper.assertTrue(oor.values().stream().filter(s -> s.is(Blocks.PINK_WOOL)).count() >= 8, "two guh ears");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 20)
    public static void bouwgereedschapFamilie(GameTestHelper helper) {
        BlockState trap = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP);
        BlockState nieuw = Familie.inVorm(trap, Blocks.SPRUCE_PLANKS);
        helper.assertTrue(nieuw != null && nieuw.is(Blocks.SPRUCE_STAIRS) && nieuw.getValue(StairBlock.FACING) == Direction.EAST
                && nieuw.getValue(StairBlock.HALF) == Half.TOP, "a stair stays a stair: " + nieuw);
        helper.assertTrue(Familie.inVorm(Blocks.STONE_BRICK_SLAB.defaultBlockState(), Blocks.BRICKS).is(Blocks.BRICK_SLAB), "bricks have slabs");
        helper.assertTrue(Familie.inVorm(Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH), Blocks.SPRUCE_PLANKS) == null,
                "a door is left alone");
        helper.assertTrue(Familie.van(Blocks.QUARTZ_BLOCK).trap() == Blocks.QUARTZ_STAIRS, "quartz_block -> quartz_stairs");
        helper.assertTrue(Familie.kleurSoort(Blocks.LIME_CONCRETE).equals("concrete") && Familie.gekleurd("wool", DyeColor.PINK) == Blocks.PINK_WOOL,
                "colour families");
        var roze = Familie.blok("guhs:pastelbaksteen_roze");
        if (roze != null) {
            helper.assertTrue(Familie.van(roze).trap() != null && Familie.van(roze).plaat() != null, "the guh pastel bricks have their stair and slab");
        }
        helper.succeed();
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 20)
    public static void bouwgereedschapKwasten(GameTestHelper helper) {
        for (int x = 2; x <= 6; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 4), Blocks.COBBLESTONE);
                helper.setBlock(new BlockPos(x, y, 8), Blocks.WHITE_WOOL);
            }
        }
        helper.setBlock(new BlockPos(4, 5, 4), Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        BlockPos midden = helper.absolutePos(new BlockPos(4, 3, 4));
        List<BlockPos> gebied = Kwasten.gebied(helper.getLevel(), midden, 2, false, false);
        Map<BlockPos, BlockState> vervang = Kwasten.vervang(helper.getLevel(), gebied, Blocks.STONE_BRICKS);
        helper.assertTrue(vervang.get(midden).is(Blocks.STONE_BRICKS), "the wall becomes stone bricks");
        BlockState trap = vervang.get(helper.absolutePos(new BlockPos(4, 5, 4)));
        helper.assertTrue(trap != null && trap.is(Blocks.STONE_BRICK_STAIRS) && trap.getValue(StairBlock.FACING) == Direction.WEST, "the stair too, same way: " + trap);
        Map<BlockPos, BlockState> meng = Kwasten.meng(helper.getLevel(), gebied, Mix.van(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS));
        helper.assertTrue(meng.values().stream().map(BlockState::getBlock).distinct().count() >= 3, "a mix uses its blocks");
        Map<BlockPos, BlockState> oud = Kwasten.verweer(helper.getLevel(), gebied, true, false, true, 10);
        helper.assertTrue(oud.values().stream().anyMatch(s -> s.is(Blocks.MOSSY_COBBLESTONE)), "moss");
        helper.assertTrue(oud.values().stream().anyMatch(s -> s.is(Blocks.VINE)), "ivy hangs on the sides");
        Map<BlockPos, BlockState> verf = Kwasten.verf(helper.getLevel(), helper.absolutePos(new BlockPos(4, 3, 8)), 2, DyeColor.PINK, null, true, Direction.NORTH);
        helper.assertTrue(verf.get(helper.absolutePos(new BlockPos(4, 3, 8))).is(Blocks.PINK_WOOL)
                && verf.values().stream().noneMatch(s -> s.is(Blocks.WHITE_WOOL)), "the verfmachine makes the wool pink (and the floor's concrete)");
        Map<BlockPos, BlockState> zebra = Kwasten.patroon(helper.getLevel(), Kwasten.gebied(helper.getLevel(), helper.absolutePos(new BlockPos(4, 3, 8)), 2, false, false),
                nl.juiced.guhs.feature.bouwhulp.gereedschap.Patronen.Patroon.ZEBRA, Mix.LEEG);
        helper.assertTrue(zebra.values().stream().anyMatch(s -> s.is(Blocks.BLACK_WOOL)), "zebra stripes in the wool");
        helper.succeed();
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 20)
    public static void bouwgereedschapVastleggen(GameTestHelper helper) {
        // a 5 x 4 x 5 hut (floating, clear of the test floor): walls, a floor and a roof, the room empty
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(new BlockPos(x, 4, z), Blocks.OAK_PLANKS);
                helper.setBlock(new BlockPos(x, 7, z), Blocks.SPRUCE_PLANKS);
                for (int y = 5; y <= 6; y++) {
                    boolean rand = x == 2 || x == 6 || z == 2 || z == 6;
                    helper.setBlock(new BlockPos(x, y, z), rand ? Blocks.BRICKS : Blocks.AIR);
                }
            }
        }
        Vastleggen.Doos d = Vastleggen.auto(helper.getLevel(), helper.absolutePos(new BlockPos(2, 5, 2)), 16);
        helper.assertTrue(d.maat() == 5 && d.max().getY() - d.min().getY() == 3, "the frame fits the hut: " + d);
        Ontwerp o = Vastleggen.vang(helper.getLevel(), d);
        long lucht = o.blokken().values().stream().filter(BlockState::isAir).count();
        helper.assertTrue(lucht == 18, "the room's 18 empty spots come along (" + lucht + ")");
        helper.assertTrue(o.aantal() == 25 + 25 + 32 + 18, "and every block (" + o.aantal() + ")");
        helper.assertTrue(d.schuif(1, 1).maat() == 6 && d.schuif(1, -1).maat() == 5, "a side moves");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 20)
    public static void bouwgereedschapBibliotheek(GameTestHelper helper) {
        Bibliotheek bib = new Bibliotheek();
        UUID maker = UUID.randomUUID(), ander = UUID.randomUUID();
        Ontwerp o = Ontwerp.doos(2, 2, 2, Blocks.STONE.defaultBlockState());
        for (int i = 0; i < Bibliotheek.MAX_PER_SPELER; i++) {
            helper.assertTrue(bib.publiceer(Blauwdruk.nieuw("huisje " + i, maker, "Guhbert", o), maker, "Guhbert") == Bibliotheek.Uitkomst.OK, "publish " + i);
        }
        helper.assertTrue(bib.publiceer(Blauwdruk.nieuw("eentje te veel", maker, "Guhbert", o), maker, "Guhbert") == Bibliotheek.Uitkomst.VOL, "ten at most");
        Blauwdruk b = bib.lijst().get(0);
        helper.assertTrue(bib.duimpje(b.id, ander) && b.duimpjes() == 1 && !bib.duimpje(b.id, ander) && b.duimpjes() == 0, "a thumb on and off");
        helper.assertTrue(!bib.verwijder(b.id, ander, false), "somebody else can't remove it");
        helper.assertTrue(bib.verwijder(b.id, ander, true), "an op can");
        helper.assertTrue(bib.vanSpeler(maker) == Bibliotheek.MAX_PER_SPELER - 1, "nine left");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 60)
    public static void bouwgereedschapCatalogus(GameTestHelper helper) {
        helper.assertTrue(Catalogus.lijst().size() >= 25, "about 25 designs (" + Catalogus.lijst().size() + ")");
        for (Catalogus.Ontwerpje c : Catalogus.lijst()) {
            Blauwdruk b = Catalogus.get(c.id());
            helper.assertTrue(b != null && !b.ontwerp.isLeeg() && b.ontwerp.maat() <= 64, c.id() + " is a real design");
            helper.assertTrue(b.voorbeeld.length == Voorbeeld.N * Voorbeeld.N && java.util.Arrays.stream(b.voorbeeld).filter(x -> x != 0).count() > 50,
                    c.id() + " has a picture");
            helper.assertTrue(Catalogus.GROEPEN.contains(c.groep()), c.id() + " is in a group");
        }
        for (String s : Catalogus.SAFARI) {
            helper.assertTrue(Catalogus.get(s) != null, "giftshop blueprint " + s);
        }
        helper.succeed();
    }

    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 20)
    public static void bouwgereedschapTerrein(GameTestHelper helper) {
        for (int x = 3; x <= 9; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 3; z <= 9; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        Map<BlockPos, BlockState> t = Terrein.tunnel(helper.getLevel(), helper.absolutePos(new BlockPos(6, 2, 3)), Direction.SOUTH, Terrein.Doorsnede.GEWELFD,
                3, 4, 7, Blocks.STONE_BRICKS, true);
        helper.assertTrue(t.get(helper.absolutePos(new BlockPos(6, 3, 5))).isAir(), "the tunnel is dug out");
        helper.assertTrue(t.values().stream().anyMatch(s -> s.is(Blocks.STONE_BRICKS)), "and lined");
        helper.assertTrue(t.values().stream().anyMatch(s -> s.is(Blocks.LANTERN)), "with a lamp");
        // a bump on flat ground: vlak takes it off
        helper.setBlock(new BlockPos(12, 2, 12), Blocks.DIRT);
        helper.setBlock(new BlockPos(12, 3, 12), Blocks.DIRT);
        helper.setBlock(new BlockPos(13, 2, 12), Blocks.GRASS_BLOCK);
        Map<BlockPos, BlockState> vlak = Terrein.egaliseer(helper.getLevel(), helper.absolutePos(new BlockPos(13, 2, 12)), Terrein.Stand.VLAK, 2, 0, false,
                false, 0, Direction.NORTH);
        helper.assertTrue(vlak.getOrDefault(helper.absolutePos(new BlockPos(12, 3, 12)), Blocks.STONE.defaultBlockState()).isAir(), "the bump goes");
        Map<BlockPos, BlockState> kuil = Terrein.egaliseer(helper.getLevel(), helper.absolutePos(new BlockPos(13, 2, 12)), Terrein.Stand.KUIL, 2, 2, false,
                true, 0, Direction.NORTH);
        helper.assertTrue(kuil.values().stream().anyMatch(s -> s.is(Blocks.WATER)), "a pond gets water");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 20)
    public static void bouwgereedschapOpties(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        CompoundTag t = Opties.van(p, Gereedschap.VORMSTAF);
        t.remove("vorm");
        helper.assertTrue(!Opties.zet(Gereedschap.VORMSTAF, p, BouwNiveau.HANDWERK, "vorm", Vormen.Vorm.ROTS.ordinal(), ""), "a nature shape is locked at handwerk");
        helper.assertTrue(Opties.zet(Gereedschap.VORMSTAF, p, BouwNiveau.ZOUT, "vorm", Vormen.Vorm.ROTS.ordinal(), ""), "open from a Zoutkeet");
        Opties.zet(Gereedschap.VORMSTAF, p, BouwNiveau.HANDWERK, "b", 99, "");
        helper.assertTrue(Opties.van(p, Gereedschap.VORMSTAF).getIntOr("b", 0) == 16, "the width is clamped to the level");
        p.getInventory().setItem(0, new ItemStack(Items.SPRUCE_PLANKS));
        helper.assertTrue(Opties.zet(Gereedschap.MENGKWAST, p, BouwNiveau.HANDWERK, "mix", 0, ""), "a block from the hotbar");
        helper.assertTrue(Mix.laad(Opties.van(p, Gereedschap.MENGKWAST).getListOrEmpty("mix")).blokken().contains(Blocks.SPRUCE_PLANKS), "into the mix");
        CompoundTag spec = Opties.schrijf(Gereedschap.VORMSTAF, p, BouwNiveau.HANDWERK, null, -1);
        helper.assertTrue(spec.getListOrEmpty("Opties").size() >= 8, "the settings screen gets its options");
        helper.succeed();
    }

    /** A kwast stroke is a ghost that builds at once, from the player's own blocks. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void bouwgereedschapKwastBouwt(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.getInventory().add(new ItemStack(Items.SPRUCE_PLANKS, 16));
        for (int x = 3; x <= 5; x++) {
            helper.setBlock(new BlockPos(x, 2, 5), Blocks.OAK_PLANKS);
        }
        List<BlockPos> gebied = Kwasten.gebied(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 5)), 1, true, false);
        Spook s = Plaatsen.meteen(p, Kwasten.vervang(helper.getLevel(), gebied, Blocks.SPRUCE_PLANKS), "test");
        helper.assertTrue(s != null && s.status() == Spook.Status.BOUWT, "it builds at once");
        helper.succeedWhen(() -> {
            for (int x = 3; x <= 5; x++) {
                helper.assertBlockPresent(Blocks.SPRUCE_PLANKS, new BlockPos(x, 2, 5));
            }
            helper.assertTrue(Spookbouw.get(helper.getLevel(), s.id()) == null || s.status() != Spook.Status.BOUWT, "and is done");
        });
    }
}
