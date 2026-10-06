package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Plek;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Roepen;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the guhpixel kern (batch px_kern; run with {@code -Pgt=px_kern}): muntjes (once-keys, the daily pot,
 * paying, ranks, two players apart), the clock, the safe (an exact round trip, a simulated logout, a login with a
 * leftover snapshot), the allocator maths and re-use, a session's life cycle with two players in two arenas at once, the
 * rules in a test box, unlocking and the return point, the joke questlines, the shop, the Guhdex page, storing a guh, and
 * the lobby template's fixed geometry. The game test server has no guhpixel dimension: every test marks its own box
 * ({@link PxTest#gebied}).
 */
public class PxKernGameTests {
    private static final String BATCH = "px_kern";
    private static final String KLEIN = "px_test_16", GROOT = "px_test_48";
    static final ArenaSoort TEST_ARENA = new ArenaSoort("px_test", Guhs.id("guhpixel/guhpixel_test_arena"), new Vec3i(9, 6, 9), new Vec3(4.5, 1, 4.5), 90f);
    static final ArenaSoort TEST_ARENA_STEMPEL = new ArenaSoort("px_test_stempel", Guhs.id("guhpixel/guhpixel_test_arena"), new Vec3i(9, 6, 9),
            new Vec3(4.5, 1, 4.5), 0f, true, a -> { });
    static final List<String> LOG = new ArrayList<>();

    /** A game that gives every player a stick and writes down what happens. */
    static final class TestSessie extends Sessie {
        TestSessie(SessieStart start) {
            super(start);
        }

        @Override
        protected void uitrusting(ServerPlayer p) {
            p.getInventory().setItem(0, new ItemStack(Items.STICK, 3));
            p.getInventory().setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.FEATHER));
        }

        @Override
        protected void begin() {
            LOG.add("begin " + spelers().size());
        }

        @Override
        protected void tick() {
        }

        @Override
        protected void spelerWeg(ServerPlayer p, Vertrek reden) {
            LOG.add("weg " + reden);
        }

        @Override
        protected void einde() {
            LOG.add("einde");
        }

        @Override
        public boolean magBreken(ServerPlayer p, BlockPos pos, BlockState s) {
            return s.is(Blocks.GOLD_BLOCK);
        }
    }

    static final SpelSoort TEST_SPEL = new SpelSoort("px_test", TEST_ARENA, 1, 2, LobbyPlek.SPEL_RESERVE, TestSessie::new);

    // =====================================================================================================================
    // muntjes, ranks, the daily pot, the clock
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void muntjesEensEnBetalen(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer a = PxTest.speler(helper), b = PxTest.speler(helper);
        helper.assertTrue(Muntjes.saldo(a) == 0 && Muntjes.totaal(a) == 0 && Muntjes.rang(a) == Rang.GUH, "a new player has nothing");
        helper.assertTrue(Muntjes.verdienEens(a, "lobby:parkour", 50), "the first time pays");
        helper.assertTrue(!Muntjes.verdienEens(a, "lobby:parkour", 50), "a key pays once per player forever");
        helper.assertTrue(Muntjes.isVerdiend(a, "lobby:parkour") && !Muntjes.isVerdiend(a, "lobby:knabbel_0"), "isVerdiend");
        helper.assertTrue(Muntjes.saldo(a) == 50 && Muntjes.totaal(a) == 50, "50 muntjes, got " + Muntjes.saldo(a));
        helper.assertTrue(Muntjes.saldo(b) == 0 && !Muntjes.isVerdiend(b, "lobby:parkour"), "the other player has nothing: all per player");
        helper.assertTrue(Muntjes.verdienEens(b, "lobby:parkour", 50), "and can earn the same key themselves");
        helper.assertTrue(!Muntjes.betaal(a, 51) && Muntjes.saldo(a) == 50, "too expensive: nothing is taken");
        helper.assertTrue(Muntjes.betaal(a, 20) && Muntjes.saldo(a) == 30 && Muntjes.totaal(a) == 50, "paying lowers the saldo, never the total");
        for (int i = 0; i < 10; i++) {
            Muntjes.verdienEens(a, "lobby:knabbel_" + i, 10);
        }
        for (String id : List.of("skyblok", "bedwars", "vadsnite", "guhmon", "bzg", "among")) {
            Muntjes.verdienEens(a, "grap:" + id, 100);
        }
        helper.assertTrue(Muntjes.totaal(a) == 750, "the whole one-time budget is 750, got " + Muntjes.totaal(a));
        helper.assertTrue(Muntjes.rang(a) == Rang.VADS_PLUS, "750 = [VADS+], got " + Muntjes.rang(a));
        helper.assertTrue(Rang.bij(0) == Rang.GUH && Rang.bij(249) == Rang.GUH && Rang.bij(250) == Rang.VADS && Rang.bij(1250) == Rang.MVG
                && Rang.bij(1749) == Rang.MVG && Rang.bij(1750) == Rang.MVG_PLUS_PLUS && Rang.MVG_PLUS_PLUS.volgende() == null, "the rank thresholds");
        Muntjes.betaal(a, Muntjes.saldo(a));
        helper.assertTrue(Muntjes.saldo(a) == 0 && Muntjes.rang(a) == Rang.VADS_PLUS, "spending everything keeps the rank");
        helper.assertTrue(Rangen.getoond(a) == Rang.VADS_PLUS, "an unlocked player shows the rank");
        Rangen.zet(a, false);
        helper.assertTrue(Rangen.getoond(a) == null, "switched off in the Guhdex: no prefix");
        Rangen.zet(a, true);
        Toegang.vergrendel(a);
        helper.assertTrue(Rangen.getoond(a) == null, "not unlocked: no prefix");
        helper.assertTrue(Rangen.metRang(Component.literal("Guh"), Rang.VADS).getString().endsWith(" Guh"), "the rank is a prefix");
        PxTest.klaar(helper, a, b);
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void dagpotEnKlok(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper);
        Klok.herstel();
        long nu = Klok.nu(), dag = Klok.dag();
        helper.assertTrue(Math.abs(nu - System.currentTimeMillis()) < 2000, "without an offset the clock is the wall clock");
        helper.assertTrue(Muntjes.verdienDagelijks(p, "among", 45, 200) == 45 && Muntjes.vandaag(p, "among") == 45, "45 of the pot");
        helper.assertTrue(Muntjes.verdienDagelijks(p, "among", 150, 200) == 150, "up to the cap");
        helper.assertTrue(Muntjes.verdienDagelijks(p, "among", 45, 200) == 5, "only what is left of the cap is paid");
        helper.assertTrue(Muntjes.verdienDagelijks(p, "among", 45, 200) == 0 && Muntjes.saldo(p) == 200, "the cap: 200 a day, got " + Muntjes.saldo(p));
        PxTest.spoel(1.5);
        helper.assertTrue(Klok.nu() - nu >= 5_400_000L - 50 && Klok.nu() - nu < 5_400_000L + 5000, "1.5 hours later");
        PxTest.spoel(24);
        helper.assertTrue(Klok.dag() == dag + 1 || Klok.dag() == dag + 2, "a real day later: day " + dag + " -> " + Klok.dag());
        helper.assertTrue(Muntjes.vandaag(p, "among") == 0, "a new real day empties the pot");
        helper.assertTrue(Muntjes.verdienDagelijks(p, "among", 45, 200) == 45 && Muntjes.totaal(p) == 245, "and pays again");
        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(PxData.algemeen(server).getLongOr("KlokOffset", 0L) == Klok.offset() && Klok.offset() > 0, "the offset is saved");
        helper.assertTrue(Klok.totMorgen() > 0 && Klok.totMorgen() <= Klok.DAG, "time until tomorrow");
        Klok.herstel();
        helper.assertTrue(Klok.offset() == 0 && Klok.dag() == dag, "back to real time");
        PxTest.klaar(helper, p);
        helper.succeed();
    }

    // =====================================================================================================================
    // the safe
    // =====================================================================================================================

    /** A recognisable inventory: hotbar, main, all armour, the offhand, a stack on the cursor, slot 5 selected. */
    private static void vul(ServerPlayer p) {
        Inventory inv = p.getInventory();
        inv.clearContent();
        inv.setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
        inv.setItem(5, new ItemStack(Items.COOKED_BEEF, 17));
        inv.setItem(8, new ItemStack(Items.TORCH, 64));
        inv.setItem(9, new ItemStack(Items.OAK_PLANKS, 33));
        inv.setItem(35, new ItemStack(Items.ENDER_PEARL, 16));
        inv.setItem(36, new ItemStack(Items.IRON_BOOTS));
        inv.setItem(37, new ItemStack(Items.IRON_LEGGINGS));
        inv.setItem(38, new ItemStack(Items.IRON_CHESTPLATE));
        inv.setItem(39, new ItemStack(Items.IRON_HELMET));
        inv.setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.SHIELD));
        ItemStack naam = new ItemStack(Items.PAPER, 2);
        naam.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Njeg"));
        inv.setItem(20, naam);
        inv.setSelectedSlot(5);
    }

    private static List<ItemStack> foto(ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            out.add(inv.getItem(i).copy());
        }
        return out;
    }

    private static String verschil(List<ItemStack> voor, ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!ItemStack.matches(voor.get(i), inv.getItem(i))) {
                return "slot " + i + ": " + voor.get(i) + " became " + inv.getItem(i);
            }
        }
        return null;
    }

    private static int tel(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) {
                n += p.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    private static boolean leeg(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void kluisRondje(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper);
        vul(p);
        List<ItemStack> voor = foto(p);
        p.containerMenu.setCarried(new ItemStack(Items.GOLD_INGOT, 7));
        helper.assertTrue(!Kluis.heeft(p), "no snapshot yet");
        helper.assertTrue(Kluis.bewaar(p), "stored");
        helper.assertTrue(Kluis.heeft(p) && leeg(p) && p.containerMenu.getCarried().isEmpty(), "the inventory and the cursor are empty for the game items");
        helper.assertTrue(GuhQuests.saved(p).getCompound(Kluis.SLEUTEL).isPresent(), "the snapshot lives in the player's own data (one file with the inventory)");
        helper.assertTrue(!Kluis.bewaar(p), "a second snapshot never overwrites the first");
        // game items everywhere, another slot selected
        p.getInventory().setItem(0, new ItemStack(Items.STICK, 64));
        p.getInventory().setItem(5, new ItemStack(Items.BLAZE_ROD));
        p.getInventory().setItem(39, new ItemStack(Items.CARVED_PUMPKIN));
        p.getInventory().setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.FEATHER));
        p.getInventory().setItem(22, new ItemStack(Items.BEDROCK, 12));
        p.containerMenu.setCarried(new ItemStack(Items.BARRIER));
        p.getInventory().setSelectedSlot(2);
        helper.assertTrue(Kluis.herstel(p), "restored");
        // the 7 gold ingots of the cursor went to the first free slot: everything else is exactly where it was
        ItemStack goud = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(Items.GOLD_INGOT)) {
                goud = p.getInventory().getItem(i).copy();
                helper.assertTrue(voor.get(i).isEmpty(), "the cursor stack took a free slot");
                p.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        helper.assertTrue(goud.getCount() == 7, "the stack on the cursor came back: " + goud);
        String anders = verschil(voor, p);
        helper.assertTrue(anders == null, "every slot exactly as before: " + anders);
        helper.assertTrue(p.getInventory().getSelectedSlot() == 5, "the selected slot is back");
        helper.assertTrue(!p.getInventory().hasAnyMatching(s -> s.is(Items.STICK) || s.is(Items.BEDROCK) || s.is(Items.BLAZE_ROD) || s.is(Items.FEATHER)
                || s.is(Items.CARVED_PUMPKIN)) && p.containerMenu.getCarried().isEmpty(), "no game item leaks");
        helper.assertTrue(!Kluis.heeft(p) && !Kluis.herstel(p), "the snapshot is used up");
        PxTest.klaar(helper, p);
        helper.succeed();
    }

    @GuhTest(template = GROOT, batch = BATCH)
    public static void kluisBijUitloggenEnCrash(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper);
        vul(p);
        List<ItemStack> voor = foto(p);
        Sessies.registreer(TEST_SPEL);
        // 1) logging out in the middle of a game: the own inventory is back before the player is saved
        Sessie s = Sessies.startOp(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), TEST_SPEL, List.of(p), new CompoundTag());
        helper.assertTrue(s != null && Sessies.van(p) == s && Kluis.heeft(p), "playing");
        helper.assertTrue(p.getInventory().getItem(0).is(Items.STICK) && p.getInventory().getItem(Inventory.SLOT_OFFHAND).is(Items.FEATHER)
                && !p.getInventory().hasAnyMatching(x -> x.is(Items.DIAMOND_PICKAXE)), "the game items replaced the inventory");
        Regels.onLogout(new PlayerEvent.PlayerLoggedOutEvent(p));
        helper.assertTrue(Sessies.van(p) == null && !Kluis.heeft(p), "logged out: out of the game");
        String anders = verschil(voor, p);
        helper.assertTrue(anders == null, "logout: every slot exactly as before: " + anders);
        helper.assertTrue(s.isGestopt() && Sessies.alle().stream().noneMatch(x -> x == s), "a game without players stops by itself");
        // 2) a crash in the middle of a game: the player file holds game items AND the snapshot; the next login restores
        helper.assertTrue(Kluis.bewaar(p), "stored again");
        p.getInventory().setItem(3, new ItemStack(Items.STICK, 9));
        helper.assertTrue(Sessies.van(p) == null && Kluis.heeft(p), "a snapshot without a running game (as after a crash)");
        Regels.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
        anders = verschil(voor, p);
        helper.assertTrue(!Kluis.heeft(p) && anders == null, "login with a leftover snapshot restores everything: " + anders);
        // 3) the server stops in the middle of a game
        Sessie s2 = Sessies.startOp(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 2)), TEST_SPEL, List.of(p), new CompoundTag());
        helper.assertTrue(s2 != null, "playing again");
        Sessies.stopAlles(Vertrek.SERVER_STOP);
        anders = verschil(voor, p);
        helper.assertTrue(!Kluis.heeft(p) && anders == null && Sessies.alle().isEmpty(), "server stop: everything back: " + anders);
        // 4) dying in guhpixel costs nothing: the inventory waits in the safe until the respawn
        Regels.onDeath(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(p, helper.getLevel().damageSources().genericKill()));
        helper.assertTrue(Kluis.heeft(p) && leeg(p), "dead: nothing left to drop");
        Regels.onRespawn(new PlayerEvent.PlayerRespawnEvent(p, false));
        anders = verschil(voor, p);
        helper.assertTrue(!Kluis.heeft(p) && anders == null, "respawn: everything back: " + anders);
        PxTest.klaar(helper, p);
        helper.succeed();
    }

    // =====================================================================================================================
    // arenas and sessions
    // =====================================================================================================================

    @GuhTest(template = GROOT, batch = BATCH)
    public static void arenaRekenenEnHergebruik(GameTestHelper helper) {
        // the grid
        helper.assertTrue(Guhpixel.celOorsprong(0).equals(new BlockPos(4096, 64, 0)) && Guhpixel.celOorsprong(63).equals(new BlockPos(4096 + 512 * 63, 64, 0))
                && Guhpixel.celOorsprong(64).equals(new BlockPos(4096, 64, 512)) && Guhpixel.celOorsprong(130).equals(new BlockPos(4096 + 1024, 64, 1024)),
                "cell k is at (4096 + 512 * (k % 64), 64, 512 * (k / 64))");
        for (int k : new int[] {0, 1, 63, 64, 65, 500}) {
            BlockPos o = Guhpixel.celOorsprong(k);
            helper.assertTrue(Guhpixel.celVan(o) == k && Guhpixel.celVan(o.offset(159, 159, 159)) == k, "cell " + k + " holds its own arena box");
            helper.assertTrue(Guhpixel.celVan(o.offset(160, 0, 0)) == -1 && Guhpixel.celVan(o.offset(0, -1, 0)) == -1, "outside the 160 box is no arena");
        }
        helper.assertTrue(Guhpixel.celVan(new BlockPos(0, 100, 0)) == -1 && !Guhpixel.lobbyDoos().intersects(Stempel.doos(Guhpixel.celOorsprong(0),
                new Vec3i(160, 160, 160))), "the lobby and the arenas lie apart");
        // the allocator: the lowest free cell of the same kind, else a new one
        List<String> cellen = new ArrayList<>(List.of("a", "b", "a", Arenas.AFGESCHREVEN, "b"));
        Set<Integer> bezet = new HashSet<>();
        helper.assertTrue(Arenas.vrijeCel(cellen, bezet, "a") == 0 && Arenas.vrijeCel(cellen, bezet, "b") == 1, "a free built cell is reused");
        bezet.add(0);
        helper.assertTrue(Arenas.vrijeCel(cellen, bezet, "a") == 2, "a cell in use is skipped");
        bezet.add(2);
        helper.assertTrue(Arenas.vrijeCel(cellen, bezet, "a") == 5 && Arenas.vrijeCel(cellen, bezet, "c") == 5, "nothing free: a new cell at the end");
        bezet.remove(0);
        helper.assertTrue(Arenas.vrijeCel(cellen, bezet, "a") == 0, "given back: free again");
        helper.assertTrue(Arenas.vrijeCel(cellen, bezet, Arenas.AFGESCHREVEN + "x") == 5, "a written-off cell is never reused");
        // a real arena in the test room: stamped with its entity, cleaned when it goes back
        ServerLevel level = helper.getLevel();
        BlockPos plek = helper.absolutePos(new BlockPos(3, 2, 3));
        Arena a = Arenas.neemOp(level, plek, TEST_ARENA);
        helper.assertTrue(a != null && a.cel() == -1 && a.oorsprong().equals(plek), "stamped at the given spot");
        helper.assertTrue(level.getBlockState(a.wereld(4, 0, 4)).is(Blocks.GOLD_BLOCK) && level.getBlockState(a.wereld(0, 0, 0)).is(Blocks.SMOOTH_STONE),
                "the template's blocks");
        helper.assertTrue(a.lokaal(a.wereld(3, 2, 1)).equals(new BlockPos(3, 2, 1)) && a.start().equals(a.wereld(new Vec3(4.5, 1, 4.5))), "coordinates");
        helper.assertTrue(level.getEntitiesOfClass(ArmorStand.class, a.doos()).size() == 1, "the template's entity is spawned");
        helper.spawn(net.minecraft.world.entity.EntityType.ARMOR_STAND, helper.relativePos(a.wereld(6, 1, 6)));
        level.setBlock(a.wereld(4, 0, 4), Blocks.AIR.defaultBlockState(), 2);
        Arenas.geefTerug(a);
        helper.assertTrue(level.getEntitiesOfClass(ArmorStand.class, a.doos().inflate(1)).isEmpty(), "giving back removes every non-player entity");
        helper.assertTrue(level.getBlockState(a.wereld(4, 0, 4)).isAir(), "without herstempel the blocks stay as the game left them (herstel undoes)");
        // herstempel: the box is cleared and stamped again
        Arena b = Arenas.neemOp(level, plek, TEST_ARENA_STEMPEL);
        level.setBlock(b.wereld(4, 0, 4), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(b.wereld(2, 3, 2), Blocks.DIRT.defaultBlockState(), 2);
        Arenas.geefTerug(b);
        helper.assertTrue(level.getBlockState(b.wereld(4, 0, 4)).is(Blocks.GOLD_BLOCK) && level.getBlockState(b.wereld(2, 3, 2)).isAir(),
                "herstempel puts the template back and removes what was built");
        helper.assertTrue(level.getEntitiesOfClass(ArmorStand.class, b.doos()).size() == 1, "and its entities are there once, not twice");
        Stempel.ruim(level, b.doos());
        helper.succeed();
    }

    @GuhTest(template = GROOT, batch = BATCH)
    public static void tweeSpelersTweeArenas(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer a = PxTest.speler(helper), b = PxTest.speler(helper), c = PxTest.speler(helper);
        vul(a);
        List<ItemStack> voorA = foto(a);
        b.getInventory().setItem(4, new ItemStack(Items.EMERALD, 5));
        Sessies.registreer(TEST_SPEL);
        LOG.clear();
        helper.assertTrue(Sessies.soort("px_test") == TEST_SPEL && Arenas.soort("px_test") == TEST_ARENA, "registered");
        Toegang.vergrendel(c);
        helper.assertTrue(Sessies.startOp(level, helper.absolutePos(new BlockPos(30, 2, 30)), TEST_SPEL, List.of(c), new CompoundTag()) == null
                && Sessies.weigering(TEST_SPEL, List.of(c)) != null, "not unlocked: no game");
        helper.assertTrue(Sessies.weigering(TEST_SPEL, List.of(a, b, a)) != null, "too many players");
        Sessie sa = Sessies.startOp(level, helper.absolutePos(new BlockPos(2, 2, 2)), TEST_SPEL, List.of(a), new CompoundTag());
        Sessie sb = Sessies.startOp(level, helper.absolutePos(new BlockPos(20, 2, 2)), TEST_SPEL, List.of(b), new CompoundTag());
        helper.assertTrue(sa != null && sb != null && sa != sb && sa.arena() != sb.arena(), "two games at once, each its own arena");
        helper.assertTrue(Sessies.van(a) == sa && Sessies.van(b) == sb && Sessies.aantalSpelers("px_test") == 2 && Sessies.alle().size() == 2, "who plays where");
        helper.assertTrue(Sessies.SPEL_ID.equals(Minigames.playing(a)), "one game at a time: Minigames knows");
        helper.assertTrue(Sessies.startOp(level, helper.absolutePos(new BlockPos(30, 2, 30)), TEST_SPEL, List.of(a), new CompoundTag()) == null,
                "a player in a game cannot start another");
        helper.assertTrue(a.position().distanceTo(sa.arena().start()) < 0.1 && b.position().distanceTo(sb.arena().start()) < 0.1, "at the arena start");
        helper.assertTrue(a.getInventory().getItem(0).getCount() == 3 && b.getInventory().getItem(0).is(Items.STICK)
                && !b.getInventory().hasAnyMatching(s -> s.is(Items.EMERALD)), "game items only");
        // the rules: the game says what may be broken
        BlockPos steen = sa.arena().wereld(1, 0, 1), goud = sa.arena().wereld(4, 0, 4);
        helper.assertTrue(!a.gameMode.destroyBlock(steen) && level.getBlockState(steen).is(Blocks.SMOOTH_STONE), "nothing breaks in a game");
        helper.assertTrue(a.gameMode.destroyBlock(goud) && level.getBlockState(goud).isAir(), "unless the game allows this block");
        helper.assertTrue(!a.hurtServer(level, level.damageSources().generic(), 6f) && a.getHealth() == a.getMaxHealth(), "no damage");
        helper.assertTrue(CommonHooks.onPlayerTossEvent(a, a.getInventory().removeItemNoUpdate(0), false, true) == null
                && a.getInventory().getItem(0).getCount() == 3, "a tossed item comes straight back");
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(sa.ticks() >= 3 && sb.ticks() >= 3, "the games tick: " + sa.ticks());
            sa.klaar(a);
            String anders = verschil(voorA, a);
            helper.assertTrue(anders == null && !Kluis.heeft(a) && Sessies.van(a) == null, "done: the own inventory is back: " + anders);
            helper.assertTrue(sa.isGestopt() && !sb.isGestopt() && Sessies.van(b) == sb, "the other game runs on");
            helper.assertTrue(level.getEntitiesOfClass(ArmorStand.class, sa.arena().doos()).isEmpty(), "the finished arena is cleaned");
            helper.assertTrue(level.getEntitiesOfClass(ArmorStand.class, sb.arena().doos()).size() == 1, "the other arena is untouched");
            sb.stop();
            helper.assertTrue(b.getInventory().getItem(4).getCount() == 5 && !b.getInventory().hasAnyMatching(s -> s.is(Items.STICK)), "stopped: b has the emeralds back");
            helper.assertTrue(LOG.equals(List.of("begin 1", "begin 1", "weg KLAAR", "einde", "weg GESTOPT", "einde")), "the life cycle: " + LOG);
            helper.assertTrue(Sessies.alle().isEmpty() && Minigames.playing(a) == null, "nothing runs");
            PxTest.klaar(helper, a, b, c);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // getting in and out
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void ontgrendelenEnTerugkeer(GameTestHelper helper) {
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        Toegang.vergrendel(p);
        helper.assertTrue(!Toegang.heeft(p) && Toegang.weigering(p, false) != null, "locked: /lobby is refused");
        helper.assertTrue(Toegang.weigering(p, true) == null, "the café portal lets everybody in");
        helper.assertTrue(Toegang.ontgrendel(p) && Toegang.heeft(p), "unlocked the first time");
        helper.assertTrue(!Toegang.ontgrendel(p), "only once");
        helper.assertTrue(Toegang.weigering(p, false) == null, "unlocked: may enter");
        Toegang.vergrendel(q);
        helper.assertTrue(!Toegang.heeft(q), "per player: the other one is still locked");
        // the return point: where the player stood, with yaw and pitch
        p.snapTo(p.getX() + 2.25, p.getY(), p.getZ() + 1.5, 77f, -12f);
        Vec3 stond = p.position();
        Toegang.onthoudTerug(p);
        CompoundTag terug = GuhQuests.saved(p).getCompoundOrEmpty(Toegang.TERUG);
        helper.assertTrue(terug.getStringOr("Dim", "").equals(helper.getLevel().dimension().identifier().toString()), "the dimension is stored: " + terug);
        p.snapTo(stond.x + 5, stond.y, stond.z + 5, 0f, 0f);
        TeleportTransition t = Toegang.huisTransitie(p);
        helper.assertTrue(t.newLevel() == helper.getLevel() && t.position().distanceTo(stond) < 1e-6 && t.yRot() == 77f && t.xRot() == -12f,
                "back to exactly that spot: " + t.position());
        GuhQuests.saved(p).remove(Toegang.TERUG);
        helper.assertTrue(Toegang.huisTransitie(p) != null, "without a stored spot: the respawn point");
        // the game test server has no guhpixel dimension: entering fails softly
        if (Guhpixel.level(helper.getLevel().getServer()) == null) {
            helper.assertTrue(!Toegang.naarLobby(p) && !Toegang.naarHuis(p), "no dimension: nobody goes anywhere");
        }
        // the Netwerkkabeltje: once, and again only when it is lost
        helper.assertTrue(!Toegang.kabeltjeGehad(p) && Toegang.geefKabeltje(p) && Toegang.kabeltjeGehad(p), "the greeter gives a cable");
        helper.assertTrue(!Toegang.geefKabeltje(p), "not while the player still has one");
        p.getInventory().clearContent();
        helper.assertTrue(Toegang.geefKabeltje(p) && p.getInventory().hasAnyMatching(s -> s.is(GuhpixelFeature.NETWERKKABELTJE.get())), "lost: a new one");
        // the anchors
        helper.assertTrue(LobbyPlek.SPAWN.pos().equals(new Vec3(0.5, 100, 0.5)) && LobbyPlek.SPEL_AMONG.voor().distanceTo(new Vec3(0.5, 100, -22.5)) < 1e-4
                && LobbyPlek.WINKEL.voor().distanceTo(new Vec3(-24.5, 100, 6.5)) < 1e-4 && Math.abs(LobbyPlek.SPEL_AMONG.voorYaw()) == 180f
                && LobbyPlek.WINKEL.voorYaw() == 90f, "3 blocks in front of an anchor, looking at it");
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void regelsInHetGebied(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        BlockPos vloer = helper.absolutePos(new BlockPos(3, 1, 3));   // (the origin lies one block below the template: its floor is relative y 1)
        helper.assertTrue(!Guhpixel.in(p) && !Guhpixel.in(level, vloer), "outside a marked box nothing is guhpixel");
        PxTest.gebied(helper);
        helper.assertTrue(Guhpixel.in(p) && Guhpixel.in(level, vloer) && !Guhpixel.inLobby(p), "inside the marked box the rules apply");
        helper.assertTrue(!p.gameMode.destroyBlock(vloer) && !level.getBlockState(vloer).isAir(), "no breaking");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 4));
        var raak = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(vloer).add(0, 0.5, 0), net.minecraft.core.Direction.UP, vloer, false);
        p.gameMode.useItemOn(p, level, p.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, raak);
        helper.assertTrue(level.getBlockState(vloer.above()).isAir() && p.getMainHandItem().getCount() == 4, "no placing");
        float voor = p.getHealth();
        p.hurtServer(level, level.damageSources().magic(), 5f);
        helper.assertTrue(p.getHealth() == voor, "no damage");
        helper.assertTrue(Roepen.roep(p, UUID.randomUUID()) == Roepen.Uitkomst.NIET_THUIS, "no guhs are called into guhpixel");
        helper.assertTrue(Roepen.nietRoepbaar(PlekSoort.OP_VAKANTIE, false) == Roepen.Uitkomst.NIET_THUIS
                && Roepen.nietRoepbaar(PlekSoort.OP_KANTOOR, false) == Roepen.Uitkomst.NIET_THUIS, "a stored guh cannot be called");
        // a creative player passes
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        helper.assertTrue(p.gameMode.destroyBlock(vloer) && level.getBlockState(vloer).isAir(), "creative players may build");
        PxTest.klaar(helper, p);
        helper.assertTrue(!Guhpixel.in(level, vloer), "the box is forgotten");
        helper.succeed();
    }

    // =====================================================================================================================
    // joke questlines, films, the shop, the Guhdex page
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void grappenEersteKeerEnOpnieuw(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper), q = PxTest.speler(helper);
        int[] aandenkens = new int[1];
        Grappen.registreer(new Grap("px_testgrap", 4, nl.juiced.guhs.entity.GuhNpcEntity.Kind.TIMMERGUH, speler -> aandenkens[0]++));
        helper.assertTrue(Grappen.stap(p, "px_testgrap") == 0 && !Grappen.isKlaar(p, "px_testgrap"), "nothing yet");
        Grappen.zetStap(p, "px_testgrap", 2);
        Grappen.zetStap(p, "px_testgrap", 1);
        helper.assertTrue(Grappen.stap(p, "px_testgrap") == 2, "the saved step is the furthest ever reached");
        Grappen.zetStap(p, "px_testgrap", 9);
        helper.assertTrue(Grappen.stap(p, "px_testgrap") == 4, "never beyond the last step");
        helper.assertTrue(!Films.heeft(p, "px_testgrap"), "no film yet");
        helper.assertTrue(Grappen.voltooi(p, "px_testgrap"), "the first time");
        helper.assertTrue(Muntjes.saldo(p) == 100 && aandenkens[0] == 1 && Films.heeft(p, "px_testgrap") && Grappen.isKlaar(p, "px_testgrap")
                && Grappen.keren(p, "px_testgrap") == 1 && Muntjes.isVerdiend(p, "grap:px_testgrap"), "100 muntjes, the keepsake, the film");
        helper.assertTrue(!Grappen.voltooi(p, "px_testgrap"), "a replay");
        helper.assertTrue(Muntjes.saldo(p) == 100 && aandenkens[0] == 1 && Grappen.keren(p, "px_testgrap") == 2, "pays nothing, no second keepsake, but counts");
        helper.assertTrue(Grappen.stap(q, "px_testgrap") == 0 && Grappen.voltooi(q, "px_testgrap") && aandenkens[0] == 2 && Muntjes.saldo(q) == 100,
                "every player has their own first time");
        // films: a flag, or a condition on existing progress
        Films.voorwaarde("px_testfilm", speler -> speler == q);
        helper.assertTrue(Films.heeft(q, "px_testfilm") && !Films.heeft(p, "px_testfilm"), "a film that follows a condition");
        helper.assertTrue(Films.ontgrendel(p, "px_testfilm") && !Films.ontgrendel(p, "px_testfilm") && Films.heeft(p, "px_testfilm"), "a film by flag, once");
        helper.assertTrue(Films.IDS.size() == 9 && Films.IDS.containsAll(List.of("skyblok", "among", "stitch626")), "the nine films");
        // the Guhdex page
        GidsBlad.registreer(new GidsSectie() {
            @Override
            public String id() {
                return "px_test";
            }

            @Override
            public int volgorde() {
                return 999;
            }

            @Override
            public boolean zichtbaar(ServerPlayer speler) {
                return speler == p || speler == q;
            }

            @Override
            public void vul(ServerPlayer speler, Bouwer b) {
                b.kop(Component.literal("Test"));
                b.grap("px_testgrap");
                b.plaatje(new ItemStack(Items.CAKE), Component.literal("Taart"), null, true);
            }
        });
        CompoundTag blad = GidsBlad.stand(p);
        ListTag rijen = blad.getListOrEmpty("Rijen");
        long stappen = PxTest.stappen(rijen, "px_testgrap");
        helper.assertTrue(blad.getBooleanOr("Toegang", false) && blad.getIntOr("Saldo", 0) == 100 && stappen == 4
                && rijen.stream().anyMatch(r -> ((CompoundTag) r).getStringOr("T", "").equals(GidsBlad.PLAATJE)), "the page: muntjes and the section's rows: " + rijen.size());
        Toegang.vergrendel(q);
        ListTag dicht = GidsBlad.stand(q).getListOrEmpty("Rijen");
        helper.assertTrue(dicht.size() >= 1 && dicht.stream().noneMatch(r -> ((CompoundTag) r).getStringOr("T", "").equals(GidsBlad.STAP)),
                "not unlocked: only the hint to find the café");
        PxTest.klaar(helper, p, q);
        helper.succeed();
    }

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void winkelKopen(GameTestHelper helper) {
        PxTest.gebied(helper);
        ServerPlayer p = PxTest.speler(helper);
        boolean[] leveren = {true};
        Winkel.aanbod("px_test_kast").groep("guhkade").icoon(() -> new ItemStack(Items.JUKEBOX)).naam(Component.literal("Testkast"))
                .uitleg(Component.literal("Een kast")).prijs((speler, al) -> al == 0 ? 250 : 150).max(2)
                .eis(speler -> Grappen.isKlaar(speler, "px_winkelgrap"), Component.literal("Eerst spelen"))
                .lever((speler, a) -> leveren[0] && Winkel.geef(speler, new ItemStack(Items.JUKEBOX))).registreer();
        helper.assertTrue(Winkel.van("px_test_kast") != null && Winkel.alle().stream().anyMatch(a -> a.id().equals("px_test_kast")), "registered");
        helper.assertTrue(Winkel.koop(p, "px_bestaat_niet") == Winkel.Uitkomst.ONBEKEND, "an unknown offer");
        helper.assertTrue(Winkel.koop(p, "px_test_kast") == Winkel.Uitkomst.EIS, "the requirement first");
        Grappen.voltooi(p, "px_winkelgrap");   // (pays 100)
        helper.assertTrue(Winkel.koop(p, "px_test_kast") == Winkel.Uitkomst.TE_DUUR && Muntjes.saldo(p) == 100, "too expensive: nothing happens");
        Muntjes.zet(p, 500);
        leveren[0] = false;
        helper.assertTrue(Winkel.koop(p, "px_test_kast") == Winkel.Uitkomst.MISLUKT && Muntjes.saldo(p) == 500 && Winkel.gekocht(p, "px_test_kast") == 0,
                "a failed delivery charges nothing");
        leveren[0] = true;
        helper.assertTrue(Winkel.prijs(p, Winkel.van("px_test_kast")) == 250, "the first one costs 250");
        helper.assertTrue(Winkel.koop(p, "px_test_kast") == Winkel.Uitkomst.OK && Muntjes.saldo(p) == 250 && Winkel.gekocht(p, "px_test_kast") == 1
                && tel(p, Items.JUKEBOX) == 1, "bought: a real item, 250 muntjes less");
        helper.assertTrue(Winkel.prijs(p, Winkel.van("px_test_kast")) == 150, "the second one costs 150");
        helper.assertTrue(Winkel.koop(p, "px_test_kast") == Winkel.Uitkomst.OK && Muntjes.saldo(p) == 100 && tel(p, Items.JUKEBOX) == 2, "the second");
        Muntjes.zet(p, 999);
        helper.assertTrue(Winkel.koop(p, "px_test_kast") == Winkel.Uitkomst.MAX && Muntjes.saldo(p) == 999, "the maximum");
        CompoundTag stand = Winkel.stand(p, null);
        helper.assertTrue(stand.getIntOr("Saldo", 0) == 999 && stand.getListOrEmpty("Aanbod").stream().anyMatch(a -> ((CompoundTag) a).getStringOr("Id", "")
                .equals("px_test_kast") && ((CompoundTag) a).getIntOr("Gekocht", 0) == 2), "what the screen gets");
        PxTest.klaar(helper, p);
        helper.assertTrue(Winkel.kan(p, Winkel.van("px_test_kast")) == Winkel.Uitkomst.NIET_HIER, "outside guhpixel nothing is sold");
        Winkel.vergeet("px_test_kast");
        helper.succeed();
    }

    // =====================================================================================================================
    // storing a guh
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void guhOpslagRondje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = PxTest.speler(helper);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 2, 4));
        guh.tame(p);
        guh.setCustomName(Component.literal("Vadsje"));
        UUID id = guh.getUUID();
        helper.assertTrue(GuhKiezer.zoek(p, id, 16) == guh && GuhKiezer.bezet(guh) == null, "a free guh of the player, nearby");
        helper.assertTrue(GuhKiezer.zoek(p, id, 0.01) == null, "too far away");
        GuhKiezer.claim(guh, "guhkade");
        helper.assertTrue(GuhKiezer.bezet(guh) != null && GuhKiezer.geclaimd(guh).equals("guhkade"), "claimed by a px feature");
        GuhKiezer.los(guh);
        GuhVolger.zet(guh, PlekSoort.WERELD, "");
        helper.assertTrue(GuhKiezer.lijst(p, r -> true).stream().anyMatch(t -> id.equals(GuhOpslag.id((CompoundTag) t))), "the picker list knows the guh");
        CompoundTag opslag = GuhOpslag.bewaar(guh, PlekSoort.OP_VAKANTIE, Component.literal("Balkonië"));
        helper.assertTrue(guh.isRemoved() && level.getEntity(id) == null, "the guh left the world");
        helper.assertTrue(id.equals(GuhOpslag.id(opslag)) && p.getUUID().equals(GuhOpslag.eigenaar(opslag)) && GuhOpslag.naam(opslag).getString().equals("Vadsje"),
                "the stored record: id, owner, name");
        Plek plek = GuhVolger.plek(level.getServer(), p.getUUID(), id);
        helper.assertTrue(plek != null && plek.soort() == PlekSoort.OP_VAKANTIE, "'Waar is mijn guh?' says on holiday: " + plek);
        helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.NIET_THUIS, "it cannot be called while it is stored");
        Vec3 terug = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 2, 8)));
        Entity e = GuhOpslag.laatVrij(level, opslag, terug, 90f);
        helper.assertTrue(e instanceof GuhEntity g && g.getUUID().equals(id) && g.isTame() && p.getUUID().equals(g.getOwnerUUID())
                && g.getName().getString().equals("Vadsje") && e.position().distanceTo(terug) < 0.5, "the very same guh is back");
        helper.assertTrue(GuhOpslag.laatVrij(level, opslag, terug, 0f) == null, "never doubled: that guh is in the world already");
        helper.assertTrue(level.getEntitiesOfClass(GuhEntity.class, helper.getBounds().inflate(2), x -> x.getUUID().equals(id)).size() == 1, "exactly one");
        e.discard();
        PxTest.klaar(helper, p);
        helper.succeed();
    }

    // =====================================================================================================================
    // the lobby template
    // =====================================================================================================================

    @GuhTest(template = KLEIN, batch = BATCH)
    public static void lobbyTemplateKloptMetDeAnkers(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Lobby.TEMPLATE).orElse(null);
        helper.assertTrue(t != null, "the lobby template exists");
        Vec3i maat = t.getSize();
        helper.assertTrue(maat.getX() == 97 && maat.getZ() == 97 && maat.getY() <= 96, "97 x (max 96) x 97, got " + maat);
        // every block of the template, by its world position (min corner at -48, 68, -48)
        CompoundTag tag = t.save(new CompoundTag());
        ListTag palette = tag.getListOrEmpty("palette");
        java.util.Map<BlockPos, String> blokken = new java.util.HashMap<>();
        java.util.Map<BlockPos, CompoundTag> staten = new java.util.HashMap<>();
        for (net.minecraft.nbt.Tag raw : tag.getListOrEmpty("blocks")) {
            CompoundTag b = (CompoundTag) raw;
            int[] pos = b.getListOrEmpty("pos").stream().mapToInt(x -> ((net.minecraft.nbt.NumericTag) x).intValue()).toArray();
            CompoundTag staat = palette.getCompoundOrEmpty(b.getIntOr("state", 0));
            BlockPos wereld = new BlockPos(pos[0], pos[1], pos[2]).offset(Guhpixel.LOBBY_MIN);
            blokken.put(wereld, staat.getStringOr("Name", ""));
            staten.put(wereld, staat);
        }
        for (LobbyPlek plek : LobbyPlek.values()) {
            BlockPos a = plek.blok();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    String vloer = blokken.getOrDefault(a.offset(dx, -1, dz), "minecraft:air");
                    helper.assertTrue(!vloer.equals("minecraft:air"), plek + ": a floor under " + a.offset(dx, -1, dz));
                    for (int dy = 0; dy < 4; dy++) {
                        String vrij = blokken.getOrDefault(a.offset(dx, dy, dz), "minecraft:air");
                        helper.assertTrue(vrij.equals("minecraft:air") || vrij.equals("guhs:guhpixel_portaal"), plek + ": free at " + a.offset(dx, dy, dz) + ", not " + vrij);
                    }
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 100; y <= 102; y++) {
                BlockPos pos = new BlockPos(x, y, 30);
                helper.assertTrue("guhs:guhpixel_portaal".equals(blokken.get(pos))
                        && "uit".equals(staten.get(pos).getCompoundOrEmpty("Properties").getStringOr("soort", "")), "the exit portal at " + pos);
            }
        }
        helper.assertTrue(tag.getListOrEmpty("entities").stream().noneMatch(e -> ((CompoundTag) e).getCompoundOrEmpty("nbt").getStringOr("id", "")
                .equals("guhs:guh_npc")), "no NPC in the template: LobbyNpcs places them");
        helper.assertTrue(blokken.values().stream().noneMatch(b -> b.equals("minecraft:water") || b.equals("minecraft:lava")), "no free water or lava");
        helper.assertTrue(Lobby.versie(helper.getLevel().getServer()) >= 1, "the template has a version");
        // the other templates of the kern
        for (String naam : List.of("guhpixel/guhpixel_test_arena", "internetcafe", "reisbureau", "px_test_16", "px_test_48", "px_test_96")) {
            helper.assertTrue(helper.getLevel().getStructureManager().get(Identifier.fromNamespaceAndPath(Guhs.MODID, naam)).isPresent(), "template " + naam);
        }
        StructureTemplate cafe = helper.getLevel().getStructureManager().get(Guhs.id("internetcafe")).orElseThrow();
        helper.assertTrue(cafe.filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                GuhpixelFeature.PORTAAL.get()).size() >= 9, "the café has its portal");
        helper.succeed();
    }
}
