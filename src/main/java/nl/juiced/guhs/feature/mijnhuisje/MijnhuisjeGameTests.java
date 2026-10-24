package nl.juiced.guhs.feature.mijnhuisje;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.BankSleutelItem;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 (ronde 37-40) the Mijn-guhhuisje: the area and its limits (20 to each side, never below y 10, the pilaar, the ladder,
 * the lampjes), the mining itself, the safety rules (fluids, other things, sand on top), the speed (free / kaasknabbels) and
 * where the blocks go (chest, Bank Guh, Rugzakzadel, waiting when everything is full). The test world lies below y 10, so the
 * tests set {@link MijnhuisjeBlockEntity#testMinY}; {@link MijnhuisjeBlockEntity#testStap} is one block of work at once.
 */
public class MijnhuisjeGameTests {
    private static final String VLOER = "px_test_16", BATCH = "mijnhuisje";
    private static final BlockPos HUIS = new BlockPos(8, 8, 8);

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        BlockPos b = helper.absolutePos(new BlockPos(1, 9, 1));
        p.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        return p;
    }

    private static int y(GameTestHelper helper, int lokaal) {
        return helper.absolutePos(new BlockPos(0, lokaal, 0)).getY();
    }

    /** Clears the space of the test and fills a box (local, inclusive) with a block. */
    private static void vul(GameTestHelper helper, int x0, int x1, int y0, int y1, int z0, int z1, Block blok) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), blok);
                }
            }
        }
    }

    private static MijnhuisjeBlockEntity huisje(GameTestHelper helper, ServerPlayer p) {
        vul(helper, 3, 13, 2, 11, 3, 13, Blocks.AIR);
        helper.setBlock(HUIS, MijnhuisjeFeature.BLOK.get().defaultBlockState().setValue(MijnhuisjeBlock.FACING, Direction.NORTH));
        MijnhuisjeBlockEntity be = (MijnhuisjeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(HUIS));
        be.zetEigenaar(p);
        return be;
    }

    private static GuhEntity guh(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.setAge(0);
        guh.tame(p);
        return guh;
    }

    private static void werk(MijnhuisjeBlockEntity be, ServerLevel level, int max) {
        for (int i = 0; i < max && !be.klaar(); i++) {
            be.testStap(level);
        }
    }

    private static int tel(Container c, Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).is(item)) {
                n += c.getItem(i).getCount();
            }
        }
        return n;
    }

    private static Container kist(GameTestHelper helper, BlockPos lokaal) {
        helper.setBlock(lokaal, Blocks.CHEST);
        return (Container) helper.getLevel().getBlockEntity(helper.absolutePos(lokaal));
    }

    private static void weg(GameTestHelper helper, ServerPlayer p, Entity... es) {
        MijnhuisjeBlockEntity.testMinY = null;
        for (Entity e : es) {
            e.discard();
        }
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    /** The area alone: at most 20 to each side, never below the floor, the top under the huisje, its three special columns. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void mijnhuisjeGebiedEnGrenzen(GameTestHelper helper) {
        MijnGebied g = new MijnGebied(new BlockPos(100, 60, 100), Direction.EAST, 25, -3, 2, MijnGebied.MIN_Y);
        helper.assertTrue(g.rx() == MijnGebied.MAX_ZIJ && g.rz() == 0, "20 to each side at most, never less than 0");
        helper.assertTrue(g.boven() == 59 && g.bodem() == MijnGebied.MIN_Y, "from right under the huisje down to y 10 (never deeper)");
        helper.assertTrue(g.perLaag() == 41, "41 x 1 per layer");
        helper.assertTrue(g.isPilaar(new BlockPos(100, 30, 100)) && g.isLadder(new BlockPos(101, 30, 100)) && !g.isLadder(new BlockPos(99, 30, 100)),
                "the pilaar under it, the ladder in front of the door (east)");
        helper.assertTrue(g.binnen(new BlockPos(80, 10, 100)) && !g.binnen(new BlockPos(79, 10, 100)) && !g.binnen(new BlockPos(100, 9, 100))
                && !g.binnen(new BlockPos(100, 60, 100)) && !g.binnen(new BlockPos(100, 30, 101)), "inside: the columns, from y 59 to y 10");
        MijnGebied hoog = new MijnGebied(new BlockPos(0, 64, 0), Direction.NORTH, 3, 3, 200, MijnGebied.MIN_Y);
        helper.assertTrue(hoog.bodem() == 63, "a floor above the top: only the top layer");
        MijnGebied diep = new MijnGebied(new BlockPos(0, 9, 0), Direction.NORTH, 3, 3, 0, MijnGebied.MIN_Y);
        helper.assertTrue(diep.leeg(), "a huisje below y 11: nothing to mine");
        helper.assertTrue(g.plek(59, 0).equals(new BlockPos(80, 59, 100)) && g.plek(59, 40).equals(new BlockPos(120, 59, 100)), "row by row");
        helper.assertTrue(!g.rand(2).isEmpty(), "an outline to show");
        helper.succeed();
    }

    /** The menu's buttons: sliders (clamped), laat zien, pause; only the owner may change anything. */
    @GuhTest(template = VLOER, batch = BATCH)
    public static void mijnhuisjeMenuKnoppen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ServerPlayer ander = GuhMockPlayer.of(helper);
        MijnhuisjeBlockEntity be = huisje(helper, p);
        MijnhuisjeBlockEntity.testMinY = y(helper, 3);
        try {
            MijnhuisjeMenu menu = new MijnhuisjeMenu(1, p.getInventory(), be);
            helper.assertTrue(menu.clickMenuButton(p, MijnhuisjeMenu.KNOP_RX + 7) && be.gebied().rx() == 7, "x to 7");
            helper.assertTrue(menu.clickMenuButton(p, MijnhuisjeMenu.KNOP_RZ + 20) && be.gebied().rz() == 20, "z to 20");
            helper.assertTrue(menu.clickMenuButton(p, MijnhuisjeMenu.KNOP_BODEM + MijnhuisjeMenu.BODEM_VERSCHUIF + y(helper, 5))
                    && be.gebied().bodem() == y(helper, 5), "down to (local) y 5");
            menu.clickMenuButton(p, MijnhuisjeMenu.KNOP_BODEM + MijnhuisjeMenu.BODEM_VERSCHUIF + y(helper, 0));
            helper.assertTrue(be.gebied().bodem() == y(helper, 3), "never below the lowest y");
            helper.assertTrue(menu.clickMenuButton(p, MijnhuisjeMenu.KNOP_TOON) && be.toont(), "laat zien: the outline shows");
            helper.assertTrue(menu.clickMenuButton(p, MijnhuisjeMenu.KNOP_AAN) && !be.aan(), "pause");
            MijnhuisjeMenu vreemd = new MijnhuisjeMenu(2, ander.getInventory(), be);
            helper.assertTrue(!vreemd.clickMenuButton(ander, MijnhuisjeMenu.KNOP_RX + 1) && be.gebied().rx() == 7, "someone else changes nothing");
        } finally {
            helper.getLevel().removePlayerImmediately(ander, Entity.RemovalReason.DISCARDED);
            weg(helper, p);
        }
        helper.succeed();
    }

    /**
     * One guh mines a 5 x 5 area three layers deep into a chest: everything but the pilaar, a ladder down in front of the
     * door, a lampje in the pilaar and at the edge in the lamp layer, nothing below the floor or outside the area.
     */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void mijnhuisjeMijntHetGebiedLeeg(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        MijnhuisjeBlockEntity be = huisje(helper, p);
        vul(helper, 5, 11, 3, 7, 5, 11, Blocks.STONE);
        Container kist = kist(helper, new BlockPos(9, 8, 8));
        GuhEntity guh = guh(helper, p, new BlockPos(11, 8, 11));
        MijnhuisjeBlockEntity.testMinY = y(helper, 5);
        try {
            helper.assertTrue(be.trekIn(guh) && MijnhuisjeBlockEntity.isBewoner(guh) && be.bewoners().size() == 1, "the guh moves in");
            helper.assertTrue(!guh.isTeleportEnabled(), "it lives here: no popping over to its owner");
            be.stel(2, 2, y(helper, 2));
            helper.assertTrue(be.gebied().bodem() == y(helper, 5), "asked for y 2, the floor is y 5");
            werk(be, level, 300);
            helper.assertTrue(be.klaar(), "done");
            helper.assertTrue(be.gemijnd() == 5 * 5 * 3 - 3, "every block but the pilaar: " + be.gemijnd());
            for (int x = 6; x <= 10; x++) {
                for (int z = 6; z <= 10; z++) {
                    for (int yy = 5; yy <= 7; yy++) {
                        BlockState s = helper.getBlockState(new BlockPos(x, yy, z));
                        String waar = x + " " + yy + " " + z + ": " + s;
                        if (x == 8 && z == 8) {
                            helper.assertTrue(s.is(yy == 5 ? Blocks.PEARLESCENT_FROGLIGHT : Blocks.STONE), "the pilaar stays (a lampje in the lamp layer) " + waar);
                        } else if (x == 8 && z == 7) {
                            helper.assertTrue(s.is(Blocks.LADDER) && s.getValue(LadderBlock.FACING) == Direction.NORTH, "a ladder down on the pilaar " + waar);
                        } else if (yy == 5 && (Math.abs(x - 8) == 2 && Math.abs(z - 8) == 2)) {
                            helper.assertTrue(s.is(Blocks.PEARLESCENT_FROGLIGHT), "a lampje in the corner " + waar);
                        } else if (yy == 5 && (x == 6 || x == 10 || z == 6 || z == 10) && (x == 8 || z == 8)) {
                            helper.assertTrue(s.is(Blocks.PEARLESCENT_FROGLIGHT) || s.isAir(), "the edge " + waar);
                        } else {
                            helper.assertTrue(s.isAir() || s.is(Blocks.PEARLESCENT_FROGLIGHT), "mined " + waar);
                        }
                    }
                    helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 4, z));
                }
            }
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(5, 7, 8));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(11, 6, 8));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(8, 7, 11));
            helper.assertTrue(tel(kist, Items.COBBLESTONE) == be.gemijnd() + 1, "all in the chest (and the pilaar's lampje stone): " + tel(kist, Items.COBBLESTONE));
            be.trekUit(guh.getUUID());
            helper.assertTrue(!MijnhuisjeBlockEntity.isBewoner(guh) && guh.isTeleportEnabled() && be.bewoners().isEmpty(), "moved out: its own setting back");
        } finally {
            weg(helper, p, guh);
        }
        helper.succeed();
    }

    /** Never next to water (it stays as a seal), never another block or a block entity, never under sand that stays. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void mijnhuisjeVeilig(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        MijnhuisjeBlockEntity be = huisje(helper, p);
        vul(helper, 6, 11, 4, 7, 6, 10, Blocks.STONE);
        helper.setBlock(new BlockPos(10, 6, 8), Blocks.WATER);                // (a pocket of water just outside the area)
        helper.setBlock(new BlockPos(7, 6, 9), Blocks.OAK_PLANKS);            // (somebody's build)
        helper.setBlock(new BlockPos(7, 7, 9), Blocks.BARREL);                // (a block entity)
        helper.setBlock(new BlockPos(9, 8, 9), Blocks.SAND);                  // (sand on top of the area)
        Container kist = kist(helper, new BlockPos(7, 8, 8));
        GuhEntity guh = guh(helper, p, new BlockPos(11, 8, 11));
        MijnhuisjeBlockEntity.testMinY = y(helper, 5);
        try {
            be.trekIn(guh);
            be.stel(1, 1, y(helper, 5));
            werk(be, level, 200);
            helper.assertTrue(be.klaar(), "done");
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(9, 6, 8));
            helper.assertTrue(helper.getBlockState(new BlockPos(10, 6, 8)).is(Blocks.WATER) && helper.getBlockState(new BlockPos(9, 6, 7)).getFluidState().isEmpty(),
                    "the water stays in its pocket");
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(7, 6, 9));
            helper.assertBlockPresent(Blocks.BARREL, new BlockPos(7, 7, 9));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(9, 7, 9));
            helper.assertBlockPresent(Blocks.SAND, new BlockPos(9, 8, 9));
            helper.assertTrue(helper.getBlockState(new BlockPos(9, 5, 9)).isAir(), "under a block that stays it mines on");
            helper.assertTrue(tel(kist, Items.OAK_PLANKS) == 0 && tel(kist, Items.COBBLESTONE) > 0, "only stone came out");
            helper.assertTrue(!MijnhuisjeBlockEntity.magMijnen(level, helper.absolutePos(new BlockPos(5, 2, 5)), Blocks.STONE.defaultBlockState()),
                    "never below the lowest y");
            helper.assertTrue(!MijnhuisjeBlockEntity.magMijnen(level, helper.absolutePos(new BlockPos(5, 6, 5)), Blocks.OBSIDIAN.defaultBlockState()),
                    "not natural terrain: no");
        } finally {
            weg(helper, p, guh);
        }
        helper.succeed();
    }

    /** Free: one block per 400 points (20 s per guh), kaasknabbels: four times as fast, one knabbel per 20 blocks. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void mijnhuisjeSnelheid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        MijnhuisjeBlockEntity be = huisje(helper, p);
        vul(helper, 4, 12, 3, 7, 4, 12, Blocks.STONE);
        kist(helper, new BlockPos(9, 8, 8));
        GuhEntity guh = guh(helper, p, new BlockPos(11, 8, 11));
        MijnhuisjeBlockEntity.testMinY = y(helper, 3);
        try {
            be.trekIn(guh);
            be.stel(4, 4, y(helper, 3));
            for (int i = 0; i < MijnhuisjeBlockEntity.PUNTEN_PER_BLOK - 1; i++) {
                be.tick(level);
            }
            helper.assertTrue(be.gemijnd() == 0 && be.status() == MijnhuisjeBlockEntity.BEZIG, "399 ticks: not yet (" + be.punten() + " points)");
            be.tick(level);
            helper.assertTrue(be.gemijnd() == 1, "tick 400: one block for one guh");
            be.vakken.setItem(MijnhuisjeBlockEntity.VAK_KNABBELS, new ItemStack(ModItems.KAAS_KNABBELS.get(), 2));
            for (int i = 0; i < MijnhuisjeBlockEntity.PUNTEN_PER_BLOK / MijnhuisjeBlockEntity.GEVOERD; i++) {
                be.tick(level);
            }
            helper.assertTrue(be.gemijnd() == 2, "fed: a block in 100 ticks (5 s)");
            helper.assertTrue(be.vakken.getItem(MijnhuisjeBlockEntity.VAK_KNABBELS).getCount() == 1, "the first knabbel is eaten");
            for (int i = 0; i < 20; i++) {
                be.testStap(level);
            }
            helper.assertTrue(be.gemijnd() == 22 && be.vakken.getItem(MijnhuisjeBlockEntity.VAK_KNABBELS).isEmpty() && be.gevoerd(),
                    "one knabbel per 20 blocks: after 21 fed blocks the second is going");
        } finally {
            weg(helper, p, guh);
        }
        helper.succeed();
    }

    /**
     * Where it goes: no chest, bank or Rugzak: nothing happens; a Rugzakzadel takes the ground blocks (the ores go to the
     * chest); a Bank Guh behind the key takes the rest and empties the bag; a full chest pauses the work and nothing is lost.
     */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void mijnhuisjeOpbrengst(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        MijnhuisjeBlockEntity be = huisje(helper, p);
        vul(helper, 7, 9, 6, 7, 7, 9, Blocks.STONE);
        helper.setBlock(new BlockPos(9, 7, 9), Blocks.COAL_ORE);
        GuhEntity guh = guh(helper, p, new BlockPos(11, 8, 11));
        MijnhuisjeBlockEntity.testMinY = y(helper, 6);
        try {
            be.trekIn(guh);
            be.stel(1, 1, y(helper, 6));
            werk(be, level, 5);
            helper.assertTrue(be.status() == MijnhuisjeBlockEntity.GEEN_OPSLAG && be.gemijnd() == 0, "nowhere to put it: nothing mined");
            // the Rugzakzadel: cobblestone into the bag, the coal into the chest
            guh.wear(GuhClothes.RUGZAKZADEL);
            Container kist = kist(helper, new BlockPos(9, 8, 8));
            for (int i = 0; i < 25; i++) {
                be.testStap(level);                        // (the bag is seen at the next round of who is there)
            }
            werk(be, level, 100);
            helper.assertTrue(be.klaar(), "done");
            long inZak = Rugzak.inhoud(guh).getOrDefault(Items.COBBLESTONE, 0L);
            helper.assertTrue(inZak == be.gemijnd() - 1 && tel(kist, Items.COBBLESTONE) == 0, "the stone in the bag: " + inZak);
            helper.assertTrue(tel(kist, Items.COAL) >= 1, "the coal in the chest");
            // the Bank Guh behind the key empties the bag
            helper.setBlock(new BlockPos(3, 8, 3), ModBlocks.BANK_GUH.get());
            BankGuhBlockEntity bank = (BankGuhBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(3, 8, 3)));
            ItemStack sleutel = new ItemStack(BankFeature.BANK_SLEUTEL.get());
            BankSleutelItem.onthoud(sleutel, bank);
            be.vakken.setItem(MijnhuisjeBlockEntity.VAK_SLEUTEL, sleutel);
            helper.assertTrue(be.bank() != null, "the key finds its bank");
            long naarBank = Rugzak.naarBank(guh, be.bank());
            helper.assertTrue(naarBank == inZak && bank.getStorage().count(new ItemStack(Items.COBBLESTONE)) == inZak && Rugzak.totaal(guh) == 0,
                    "the bag goes into the bank");
            // a full chest and no bank: the work waits, nothing is lost
            be.vakken.setItem(MijnhuisjeBlockEntity.VAK_SLEUTEL, ItemStack.EMPTY);
            guh.wear(GuhClothes.GUH_BACKPACK);
            for (int i = 0; i < kist.getContainerSize(); i++) {
                kist.setItem(i, new ItemStack(Items.DIRT, 64));
            }
            vul(helper, 7, 9, 6, 7, 7, 9, Blocks.STONE);
            be.stel(1, 0, y(helper, 6));
            for (int i = 0; i < 30; i++) {
                be.testStap(level);
            }
            helper.assertTrue(be.status() == MijnhuisjeBlockEntity.VOL, "the chest is full: the work pauses (" + be.status() + ")");
            int gemijnd = be.gemijnd();
            for (int i = 0; i < 30; i++) {
                be.testStap(level);
            }
            helper.assertTrue(be.gemijnd() == gemijnd, "nothing more is mined while it waits");
            kist.setItem(0, ItemStack.EMPTY);
            for (int i = 0; i < 30; i++) {
                be.testStap(level);
            }
            helper.assertTrue(be.gemijnd() > gemijnd && tel(kist, Items.COBBLESTONE) >= 1, "room again: on it goes, the waiting stone went in first");
            helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(HUIS)).inflate(8))
                    .isEmpty(), "nothing was dropped");
        } finally {
            weg(helper, p, guh);
        }
        helper.succeed();
    }

    /** Moving in: only the owner's tamed guhs, four at most; a Guhhuisje resident moves over; a picked-up guh by a click. */
    @GuhTest(template = VLOER, batch = BATCH)
    public static void mijnhuisjeBewoners(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        ServerPlayer ander = GuhMockPlayer.of(helper);
        MijnhuisjeBlockEntity be = huisje(helper, p);
        GuhEntity[] guhs = new GuhEntity[5];
        for (int i = 0; i < 5; i++) {
            guhs[i] = guh(helper, p, new BlockPos(4 + i, 8, 12));
        }
        GuhEntity vreemd = guh(helper, ander, new BlockPos(4, 8, 4));
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 8, 4));
        try {
            helper.assertTrue(!be.trekIn(vreemd) && !be.trekIn(wild), "not someone else's guh, not a wild one");
            for (int i = 0; i < 4; i++) {
                helper.assertTrue(be.trekIn(guhs[i]), "guh " + i + " moves in");
            }
            helper.assertTrue(be.isVol() && !be.trekIn(guhs[4]), "four at most");
            helper.assertTrue(!Huisjes.isBewoner(guhs[0]) && MijnhuisjeBlockEntity.thuisVan(guhs[0]) == be, "it knows its home");
            be.trekUit(guhs[0].getUUID());
            helper.assertTrue(!be.isVol() && be.trekIn(guhs[4]), "a place came free");
            helper.assertTrue(!MijnhuisjeBlockEntity.teVer(guhs[2]), "near its home");
            guhs[2].snapTo(guhs[2].getX() + 150, guhs[2].getY(), guhs[2].getZ());
            helper.assertTrue(MijnhuisjeBlockEntity.teVer(guhs[2]), "carried far away: it will be set free");
            helper.setBlock(HUIS, Blocks.AIR);
            helper.assertTrue(!MijnhuisjeBlockEntity.isBewoner(guhs[1]) && guhs[1].isTeleportEnabled(), "the huisje is gone: everyone is free again");
        } finally {
            helper.getLevel().removePlayerImmediately(ander, Entity.RemovalReason.DISCARDED);
            weg(helper, p, guhs[0], guhs[1], guhs[2], guhs[3], guhs[4], vreemd, wild);
        }
        helper.succeed();
    }

    /** The quest texts and the resources exist (names in both languages). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void mijnhuisjeTeksten(GameTestHelper helper) {
        var taal = net.minecraft.locale.Language.getInstance();
        for (String k : new String[]{"block.guhs.mijn_guhhuisje", "item.guhs.rugzakzadel", "quest.guhs.mijnhulpje.vraag", "quest.guhs.mijnhulpje.klaar",
                "gui.guhs.mijnhuisje.status.1", "gui.guhs.mijnhuisje.status.8"}) {
            helper.assertTrue(taal.has(k), "text " + k);
        }
        helper.assertTrue(new ItemStack(Items.COBBLESTONE).is(Rugzak.GROND) && !new ItemStack(Items.COAL).is(Rugzak.GROND), "ground blocks, not ores");
        helper.assertTrue(Blocks.PINK_WOOL.defaultBlockState().is(MijnhuisjeFeature.MIJNBAAR) && Blocks.COAL_ORE.defaultBlockState().is(MijnhuisjeFeature.MIJNBAAR)
                && !Blocks.OAK_PLANKS.defaultBlockState().is(MijnhuisjeFeature.MIJNBAAR), "natural terrain only");
        helper.succeed();
    }
}
