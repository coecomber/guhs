package nl.juiced.guhs.feature.vadsrijk.gilden;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-GILDEN: the Knuffelbond rules. Permissions (ranks, who may do what), the vault's dupe safety (every click path keeps the total,
 * a recruit can never take anything out), save + load of the vault, bond xp (daily cap, levels, week tasks, perks) and that
 * membership belongs to the CHARACTER. Run: {@code runGameTestServer -Pgt=VadsrijkUGilden}. No world work (castles are not stamped:
 * {@link Kasteel#BOUWEN} off).
 */
public class VadsrijkUGildenGameTests {
    private static final String BATCH = "vadsrijk_u_gilden";

    private static Bond nieuweBond(GameTestHelper helper, ServerPlayer leider, String tag) {
        Kasteel.BOUWEN = false;
        BondActies.wisUitnodigingen();
        MinecraftServer s = helper.getLevel().getServer();
        Bonden.server = s;
        Bonden.get(s).wisAlles();
        BondActies.Uitslag u = BondActies.maak(leider, "Test Bond " + tag, tag, true, true);
        helper.assertTrue(u.ok(), "founded: " + u.tekst().getString());
        return Bonden.van(leider);
    }

    private static long tel(ServerPlayer p, Bond b) {
        long n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(Items.DIAMOND)) {
                n += p.getInventory().getItem(i).getCount();
            }
        }
        if (p.containerMenu.getCarried().is(Items.DIAMOND)) {
            n += p.containerMenu.getCarried().getCount();
        }
        return n;
    }

    private static long bank(Bond b, MinecraftServer s) {
        long n = 0;
        for (var v : b.bank(s.registryAccess())) {
            for (int i = 0; i < v.getContainerSize(); i++) {
                if (v.getItem(i).is(Items.DIAMOND)) {
                    n += v.getItem(i).getCount();
                }
            }
        }
        return n;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void rechten(GameTestHelper helper) {
        ServerPlayer leider = GuhMockPlayer.of(helper), a = GuhMockPlayer.of(helper), c = GuhMockPlayer.of(helper);
        Bond b = nieuweBond(helper, leider, "RCH");
        String ka = Bonden.sleutel(a), kc = Bonden.sleutel(c);
        helper.assertTrue(b.lid(Bonden.sleutel(leider)).rang == Rang.LEIDER, "founder is leider");
        // invite + accept: a starts as rekruut
        helper.assertTrue(BondActies.nodigUit(leider, a).ok(), "leider invites");
        helper.assertTrue(BondActies.accepteer(a).ok() && b.lid(ka).rang == Rang.REKRUUT, "a joins as rekruut");
        // a rekruut may not invite, kick or promote
        helper.assertTrue(!BondActies.nodigUit(a, c).ok(), "rekruut can't invite");
        helper.assertTrue(!BondActies.verwijder(a, Bonden.sleutel(leider)).ok(), "rekruut can't kick");
        helper.assertTrue(!BondActies.rang(a, ka, true).ok(), "rekruut can't promote");
        helper.assertTrue(!BondActies.banierBasis(a, net.minecraft.world.item.DyeColor.BLUE).ok(), "rekruut can't change the banner");
        // promote a to lid, then officier (only the leider can make officers)
        helper.assertTrue(BondActies.rang(leider, ka, true).ok() && b.lid(ka).rang == Rang.LID, "-> lid");
        helper.assertTrue(BondActies.rang(leider, ka, true).ok() && b.lid(ka).rang == Rang.OFFICIER, "-> officier");
        helper.assertTrue(!BondActies.rang(leider, ka, true).ok() && b.lid(ka).rang == Rang.OFFICIER, "never a 2nd leider by promoting");
        // the officer invites c (rekruut), may promote c to lid but not to officier, may not touch the leider
        helper.assertTrue(BondActies.nodigUit(a, c).ok() && BondActies.accepteer(c).ok(), "officier invites c");
        helper.assertTrue(BondActies.rang(a, kc, true).ok() && b.lid(kc).rang == Rang.LID, "officier: c -> lid");
        helper.assertTrue(!BondActies.rang(a, kc, true).ok() && b.lid(kc).rang == Rang.LID, "officier can't make officers");
        helper.assertTrue(!BondActies.verwijder(a, Bonden.sleutel(leider)).ok(), "officier can't kick the leider");
        // one bond per character; the leider can't leave while others are in it
        helper.assertTrue(!BondActies.nodigUit(leider, c).ok(), "c is already in a bond");
        helper.assertTrue(!BondActies.verlaat(leider).ok(), "leider must hand over first");
        helper.assertTrue(BondActies.draagOver(leider, ka).ok() && b.lid(ka).rang == Rang.LEIDER && b.lid(Bonden.sleutel(leider)).rang == Rang.OFFICIER,
                "leadership handed over");
        helper.assertTrue(BondActies.verwijder(a, kc).ok() && Bonden.van(c) == null, "new leider kicks c");
        helper.assertTrue(BondActies.verlaat(leider).ok() && Bonden.van(leider) == null && b.leden.size() == 1, "old leider leaves");
        // the board: an application, accepted by the leader
        b.werft = true;
        b.vrij = false;
        helper.assertTrue(BondActies.meldAan(c, b).ok() && b.aanvragen.containsKey(kc), "c applies");
        helper.assertTrue(BondActies.keurGoed(a, kc, true).ok() && Bonden.van(c) == b, "application accepted");
        // nobody can found a bond away from the clerk
        helper.assertTrue(!BondActies.maak(leider, "Andere Bond", "AND", false).ok(), "founding only at the Bondsklerk");
        helper.assertTrue(!BondActies.maak(c, "Andere Bond", "AND", true, true).ok(), "one bond per character");
        Kasteel.BOUWEN = true;
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bankDupeVeilig(GameTestHelper helper) {
        ServerPlayer leider = GuhMockPlayer.of(helper), r = GuhMockPlayer.of(helper);
        Bond b = nieuweBond(helper, leider, "BNK");
        MinecraftServer s = helper.getLevel().getServer();
        BondActies.treedToe(r, b);
        BondBank.OVERAL = true;
        try {
            // the leader puts 64 diamonds in tab 1 by shift-click
            leider.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 64));
            BondBank.Menu m = BondBank.menuVoorTest(leider, b);
            int hotbar0 = BondBank.RIJ + 9 + 27;   // 45 grid + 9 icons, then 27 inventory, then the hotbar
            m.quickMoveStack(leider, hotbar0);
            helper.assertTrue(bank(b, s) == 64 && tel(leider, b) == 0, "leader deposited 64");
            leider.closeContainer();
            // the rekruut tries every way to take something out: nothing moves, nothing doubles
            r.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 10));
            BondBank.Menu mr = BondBank.menuVoorTest(r, b);
            int slot = -1;
            for (int i = 0; i < BondBank.RIJ; i++) {
                if (mr.getSlot(i).getItem().is(Items.DIAMOND)) {
                    slot = i;
                }
            }
            helper.assertTrue(slot >= 0, "the rekruut sees the diamonds");
            mr.clicked(slot, 0, ContainerInput.PICKUP, r);
            mr.clicked(slot, 1, ContainerInput.PICKUP, r);
            mr.clicked(slot, 0, ContainerInput.QUICK_MOVE, r);
            mr.clicked(slot, 3, ContainerInput.SWAP, r);
            mr.clicked(slot, 1, ContainerInput.THROW, r);
            helper.assertTrue(bank(b, s) == 64 && tel(r, b) == 10 && mr.getCarried().isEmpty(), "rekruut took nothing (" + bank(b, s) + ")");
            // double-click collect with diamonds on the cursor
            mr.clicked(BondBank.RIJ + 9 + 27, 0, ContainerInput.PICKUP, r);
            helper.assertTrue(mr.getCarried().getCount() == 10, "carrying own 10");
            mr.clicked(BondBank.RIJ + 9, 0, ContainerInput.PICKUP_ALL, r);
            helper.assertTrue(bank(b, s) == 64 && tel(r, b) == 10, "pick-all takes nothing from the vault");
            // the icon row never moves items, but switches tabs (tab 2 is locked at bond level 1)
            mr.clicked(BondBank.RIJ + 1, 0, ContainerInput.PICKUP, r);
            helper.assertTrue(mr.tab() == 0 && bank(b, s) == 64 && tel(r, b) == 10, "locked tab refused, icons stay");
            // the rekruut may deposit: put the 10 down in an empty slot
            int leeg = slot == 0 ? 1 : 0;
            mr.clicked(leeg, 0, ContainerInput.PICKUP, r);
            helper.assertTrue(bank(b, s) == 74 && tel(r, b) == 0, "rekruut deposits 10");
            r.closeContainer();
            // as lid: may take from tab 1
            b.lid(Bonden.sleutel(r)).rang = Rang.LID;
            BondBank.Menu ml = BondBank.menuVoorTest(r, b);
            ml.clicked(leeg, 0, ContainerInput.QUICK_MOVE, r);
            helper.assertTrue(bank(b, s) == 64 && tel(r, b) == 10, "lid takes 10 back from tab 1");
            // tab 2 (bond level 3): a lid may put in but not take out
            BondXp.bijXp(s, b, BondXp.DREMPELS[2]);
            ml.clicked(BondBank.RIJ + 1, 0, ContainerInput.PICKUP, r);
            helper.assertTrue(ml.tab() == 1, "tab 2 open at bond level 3");
            int eigen = -1;
            for (int i = BondBank.RIJ + 9; i < ml.slots.size(); i++) {
                if (ml.getSlot(i).getItem().is(Items.DIAMOND)) {
                    eigen = i;
                }
            }
            helper.assertTrue(eigen >= 0, "the lid has its 10 diamonds");
            ml.quickMoveStack(r, eigen);
            helper.assertTrue(bank(b, s) == 74, "lid deposits in tab 2");
            ml.clicked(0, 0, ContainerInput.QUICK_MOVE, r);
            ml.clicked(0, 0, ContainerInput.PICKUP, r);
            helper.assertTrue(bank(b, s) == 74 && ml.getCarried().isEmpty(), "lid can't take from tab 2");
            r.closeContainer();
            // no containers in the vault
            helper.assertTrue(!BondBank.toegestaan(new ItemStack(Items.BUNDLE)) && BondBank.toegestaan(new ItemStack(Items.DIAMOND)), "bundles refused");
            // kicked while the screen is open: the menu is no longer valid
            BondBank.Menu mk = BondBank.menuVoorTest(r, b);
            BondActies.verwijder(leider, Bonden.sleutel(r));
            helper.assertTrue(!mk.stillValid(r), "a kicked member's vault screen closes");
            r.closeContainer();
            // save + load keeps every item
            CompoundTag t = b.save(s.registryAccess());
            Bond terug = Bond.load(t);
            helper.assertTrue(bank(terug, s) == 74 && terug.naam.equals(b.naam) && terug.leden.size() == b.leden.size(), "saved + loaded");
            // disbanding with a full vault is refused (nothing is ever lost)
            helper.assertTrue(!BondActies.hefOp(leider).ok() && Bonden.van(leider) == b, "no disband with a full vault");
        } finally {
            BondBank.OVERAL = false;
            Kasteel.BOUWEN = true;
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bondsXp(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Bond b = nieuweBond(helper, p, "XPX");
        MinecraftServer s = helper.getLevel().getServer();
        helper.assertTrue(BondXp.levelVan(0) == 1 && BondXp.levelVan(999) == 1 && BondXp.levelVan(1000) == 2 && BondXp.levelVan(70000) == 10
                && BondXp.levelVan(10_000_000) == 10, "level thresholds");
        // the daily cap per member
        long g = BondXp.geef(s, b, Bonden.sleutel(p), 500, "test") + BondXp.geef(s, b, Bonden.sleutel(p), 500, "test");
        helper.assertTrue(g == BondXp.DAG_CAP && b.xp == BondXp.DAG_CAP, "daily cap " + g);
        // what members do counts (deed): opgevrolijkt etc. go to the week tasks too
        BondXp.week(b);
        helper.assertTrue(b.taken.size() == 3 && b.taken.stream().distinct().count() == 3, "3 different week tasks");
        long xpVoor = b.xp, kasVoor = b.kas;
        String taak = b.taken.get(0);
        BondXp.voortgang(s, b, taak, 1_000_000);
        helper.assertTrue(b.taakKlaar[0] && b.xp == xpVoor + BondXp.TAAK_XP && b.kas == kasVoor + BondXp.TAAK_KAS, "a week task pays out");
        BondXp.voortgang(s, b, taak, 5);
        helper.assertTrue(b.xp == xpVoor + BondXp.TAAK_XP, "a finished task pays once");
        // perks: convenience only
        helper.assertTrue(BondXp.bankTabs(1) == 1 && BondXp.bankTabs(10) == 5 && BondXp.tpMinuten(1) == 30 && BondXp.tpMinuten(10) == 5
                && BondXp.reisKorting(3) == 0 && BondXp.reisKorting(8) > 0.1, "perks");
        // the castle: the next level needs materials + kas + bond level
        b.kasteel = 1;
        helper.assertTrue(!Kasteel.mist(b).isEmpty() && !Kasteel.bouwVolgende(p).ok(), "level 2 not yet");
        Kasteel.Kosten k = Kasteel.volgende(b);
        b.kas = k.kas();
        k.materialen().forEach(b.depot::put);
        BondXp.bijXp(s, b, BondXp.DREMPELS[k.bondLevel() - 1]);
        helper.assertTrue(Kasteel.mist(b).isEmpty() && Kasteel.bouwVolgende(p).ok() && b.kasteel == 2 && b.kas == 0 && b.depot.isEmpty(), "level 2 built");
        // the export lists the bond
        helper.assertTrue(BondExport.json(s).getAsJsonArray("rijen").size() == 1, "ranking export");
        Kasteel.BOUWEN = true;
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void perKarakter(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Bond b = nieuweBond(helper, p, "KAR");
        String k = Bonden.sleutel(p);
        helper.assertTrue(k.equals(p.getUUID() + "/0"), "key = uuid/slot");
        // the same player's other character (slot 1) is not in the bond; its key could join another bond
        Bonden d = Bonden.get(helper.getLevel().getServer());
        helper.assertTrue(d.vanSleutel(p.getUUID() + "/1") == null && d.vanSleutel(k) == b, "membership is per character");
        // the names are safe for the website
        helper.assertTrue(!BondActies.NAAM.matcher("<b>hack</b>").matches() && BondActies.NAAM.matcher("Roze Ridders").matches()
                && !BondActies.TAG.matcher("TOOLONG").matches(), "name rules");
        Kasteel.BOUWEN = true;
        helper.succeed();
    }

    /** The real world work: square + plot stamped in the Vadsrijk, the castle levels, banners recoloured, the clicks. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void kasteelInDeWereld(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        MinecraftServer s = helper.getLevel().getServer();
        net.minecraft.server.level.ServerLevel sl = helper.getLevel();   // (no Vadsrijk dimension on the gametest server)
        Vallei.TEST = sl;
        try {
            kasteelInDeWereld(helper, p, s, sl);
        } finally {
            Vallei.TEST = null;
        }
        helper.succeed();
    }

    private static void kasteelInDeWereld(GameTestHelper helper, ServerPlayer p, MinecraftServer s, net.minecraft.server.level.ServerLevel sl) {
        Bond b = nieuweBond(helper, p, "WRD");
        Bonden.get(s).gestempeld = 0;
        Bonden.get(s).plein = false;
        Kasteel.BOUWEN = true;
        Kasteel.bouw(s, b);
        net.minecraft.core.BlockPos h = Vallei.kavelHoek(b.kavel);
        helper.assertTrue(b.gebouwd == 1, "level 1 stands");
        helper.assertTrue(sl.getBlockState(h.offset(32, 1, 33)).is(net.minecraft.world.level.block.Blocks.LODESTONE), "teleport stone in the tower");
        helper.assertTrue(sl.getBlockState(h.offset(28, 1, 32)).is(net.minecraft.world.level.block.Blocks.CHEST), "vault chest in the tower");
        helper.assertTrue(sl.getBlockState(new net.minecraft.core.BlockPos(Vallei.X0, Vallei.Y, Vallei.Z0 - 6)).isSolid(), "the square is there");
        helper.assertTrue(sl.getBlockState(h.offset(10, 0, 10)).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK), "the plot ground");
        // banners got the bond's colour (pink)
        int roze = 0;
        net.minecraft.core.BlockPos.MutableBlockPos m = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int x = 20; x < 44; x++) {
            for (int z = 20; z < 44; z++) {
                for (int y = 1; y < 25; y++) {
                    m.set(h.getX() + x, Vallei.Y + y, h.getZ() + z);
                    String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(sl.getBlockState(m).getBlock()).getPath();
                    if (id.equals("pink_wall_banner") || id.equals("pink_banner")) {
                        roze++;
                    }
                    helper.assertTrue(!id.equals("white_wall_banner") && !id.equals("white_banner"), "no white banner left");
                }
            }
        }
        helper.assertTrue(roze >= 2, "pink banners: " + roze);
        // the railings are connected fences (north edge of cell 0, west of the square)
        net.minecraft.core.BlockPos c0 = Vallei.celHoek(0);
        var hek = sl.getBlockState(c0.offset(3, 1, 0));
        helper.assertTrue(hek.is(net.minecraft.world.level.block.Blocks.CHERRY_FENCE)
                && hek.getValue(net.minecraft.world.level.block.FenceBlock.EAST) && hek.getValue(net.minecraft.world.level.block.FenceBlock.WEST), "railing connected");
        // a member's click on the lodestone is handled, a stranger's too (with a message), a click outside the plot not
        ServerPlayer vreemd = GuhMockPlayer.of(helper);
        helper.assertTrue(Kasteel.klik(p, h.offset(32, 1, 33), sl.getBlockState(h.offset(32, 1, 33))), "member clicks the stone");
        helper.assertTrue(Kasteel.klik(vreemd, h.offset(28, 1, 32), sl.getBlockState(h.offset(28, 1, 32))) && !(vreemd.containerMenu instanceof BondBank.Menu),
                "a stranger can't open the vault");
        // level 5: rebuilt, the tower is gone, the keep's hall has its vault, trophies are placed
        b.kasteel = 5;
        b.trofeeen.put("vadsrijk.bazen.koning_klodder", 1L);
        Kasteel.bouw(s, b);
        helper.assertTrue(b.gebouwd == 5 && sl.getBlockState(h.offset(26, 1, 30)).is(net.minecraft.world.level.block.Blocks.CHEST), "the keep's vault");
        helper.assertTrue(!sl.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, new net.minecraft.world.phys.AABB(h).expandTowards(64, 40, 64),
                e -> e.entityTags().contains(Kasteel.TAG)).isEmpty(), "a trophy in the hall");
    }
}
