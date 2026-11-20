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
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.BlauwdrukItem;
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

    /** 1.10.10: the six kept Java designs plus the twelve structure designs; the 19 removed ones still build for old prints. */
    static final List<String> HOUD = List.of("guh_huisje_klein", "guh_huis_middel", "guh_villa_groot", "guh_boerderijtje", "guh_rijtje", "guh_speeltuintje");
    static final List<String> STRUCTUUR = List.of("kaasfontein", "reuzentaart", "reuzenkaasknabbel", "blokguh", "weebhuisje", "botenhuisje",
            "villa_binnen", "eendenvijver", "japans_tuintje", "wensboom", "muziektuin", "kampeerplekje",
            "luchtballonhaven", "draaimolen", "sterrenwacht", "blubbelbadhuis", "blubseum", "piepnest", "piepmolen", "knuffelkasteel", "theepothuisje", "treinstation", "boomhuisdorpje");
    static final List<String> WEG = List.of("lodge_hutje", "rangerpost", "acacia_uitkijktoren", "watergat_terras", "safaripoort", "rieten_paviljoen",
            "guh_winkeltje", "kas_met_oortjes", "bloemenprieel", "boogbrug", "steiger_botenhuisje", "vijver_stapstenen", "picknickplek", "guh_vuurtoren",
            "guh_windmolen", "kasteeltoren", "boomhut", "guhbol_koepel", "guh_klokkentoren");

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void bouwgereedschapCatalogus(GameTestHelper helper) {
        List<String> ids = Catalogus.lijst().stream().map(Catalogus.Ontwerpje::id).toList();
        helper.assertTrue(ids.size() == HOUD.size() + STRUCTUUR.size() && ids.containsAll(HOUD) && ids.containsAll(STRUCTUUR),
                "the catalogue is the six houses plus the twelve structures: " + ids);
        StringBuilder zonder = new StringBuilder();
        for (Catalogus.Ontwerpje c : Catalogus.lijst()) {
            Blauwdruk b = Catalogus.get(c.id());
            helper.assertTrue(b != null && !b.ontwerp.isLeeg() && b.ontwerp.maat() <= 64, c.id() + " is a real design");
            helper.assertTrue(b.voorbeeld.length == Voorbeeld.N * Voorbeeld.N && java.util.Arrays.stream(b.voorbeeld).filter(x -> x != 0).count() > 50,
                    c.id() + " has a picture");
            helper.assertTrue(Catalogus.GROEPEN.contains(c.groep()), c.id() + " is in a group");
            for (BlockState st : b.ontwerp.blokken().values()) {
                helper.assertTrue(nl.juiced.guhs.feature.bouwhulp.kern.Materiaal.kost(st) != null, c.id() + ": every block can be paid (" + st + ")");
            }
            if (c.structuur() != null && !Catalogus.zonderItem(c.id()).isEmpty()) {
                zonder.append(c.id()).append(' ').append(Catalogus.zonderItem(c.id())).append("; ");
            }
        }
        org.slf4j.LoggerFactory.getLogger("BouwkeetTest").info("[Bouwkeet] structure designs, blocks without an item: {}", zonder);
        for (String s : WEG) {
            helper.assertTrue(!ids.contains(s) && Catalogus.get(s) != null && !Catalogus.get(s).ontwerp.isLeeg(), s + ": out of the list, still builds");
        }
        Blauwdruk fontein = Catalogus.get("kaasfontein");
        helper.assertTrue(fontein.ontwerp.blokken().values().stream().filter(st -> st.is(Blocks.POTTED_PINK_TULIP)).count() == 4, "the fountain keeps its 4 pots");
        helper.assertTrue(Catalogus.zonderItem("kaasfontein").keySet().stream().anyMatch(bl -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(bl).getPath().equals("kaas_saus")),
                "the sauce (a fluid) is left out");
        // the blueprints made for the catalogue (structure/blauwdruk/): EVERY block of the file is in the design (a state
        // that failed to load, e.g. a renamed block, would come out as air and be missing)
        for (Catalogus.Ontwerpje c : Catalogus.lijst()) {
            if (c.structuur() != null && c.structuur().getPath().startsWith("blauwdruk/")) {
                net.minecraft.nbt.CompoundTag t = helper.getLevel().getServer().getStructureManager().get(c.structuur()).orElseThrow()
                        .save(new net.minecraft.nbt.CompoundTag());
                net.minecraft.nbt.ListTag palet = t.getListOrEmpty("palette");
                net.minecraft.nbt.ListTag blokken = t.getListOrEmpty("blocks");
                int inBestand = 0;
                for (int i = 0; i < blokken.size(); i++) {
                    String naam = palet.getCompoundOrEmpty(blokken.getCompoundOrEmpty(i).getIntOr("state", 0)).getStringOr("Name", "");
                    if (!naam.endsWith(":air") && !naam.endsWith("structure_void") && !naam.endsWith(":jigsaw")) {
                        inBestand++;
                    }
                }
                helper.assertTrue(Catalogus.zonderItem(c.id()).isEmpty(), c.id() + ": every block has an item " + Catalogus.zonderItem(c.id()));
                helper.assertTrue(Catalogus.get(c.id()).ontwerp.aantal() == inBestand, c.id() + ": all " + inBestand + " blocks ("
                        + Catalogus.get(c.id()).ontwerp.aantal() + ")");
            }
        }
        for (String nat : List.of("blubbelbadhuis", "blubseum")) {
            helper.assertTrue(Catalogus.get(nat).ontwerp.blokken().values().stream().anyMatch(st -> st.is(Blocks.WATER))
                    && nl.juiced.guhs.feature.bouwhulp.kern.Materiaal.kost(Blocks.WATER.defaultBlockState()).oneindig(), nat + ": water, by the 2-bucket rule");
        }
        helper.assertTrue(Catalogus.get("treinstation").ontwerp.nodig().getOrDefault(nl.juiced.guhs.registry.ModItems.SLEERAIL_RECHT.get(), 0) == 10,
                "the station's track: 10 straight rail pieces " + Catalogus.get("treinstation").ontwerp.nodig().get(nl.juiced.guhs.registry.ModItems.SLEERAIL_RECHT.get()));
        helper.assertTrue(Catalogus.get("draaimolen").ontwerp.blokken().values().stream().anyMatch(st -> st.is(Blocks.IRON_CHAIN)),
                "the carousel's chains load (iron_chain from DataVersion 3955)");
        int vijver = Catalogus.get("eendenvijver").ontwerp.aantal();
        helper.assertTrue(vijver < 681 - 100, "the duck pond loses its ground slab (" + vijver + " of 681)");
        helper.assertTrue(!BlauwdrukItem.catalogus("guh_vuurtoren").isEmpty() && BlauwdrukItem.catalogus("bestaat_niet").isEmpty(), "old prints still have an id");
        helper.succeed();
    }

    /** 1.10.10: a .nbt catalogue design really builds (Blokguh, creative, fast). */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 400)
    public static void bouwgereedschapStructuurBouwt(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.CREATIVE);
        Blauwdruk b = Catalogus.get("blokguh");
        helper.assertTrue(b != null && b.ontwerp.aantal() > 250, "the Blokguh loads from its .nbt");
        Spook s = Spookbouw.start(p, b.ontwerp, helper.absolutePos(new BlockPos(3, 2, 3)), net.minecraft.world.level.block.Rotation.NONE,
                net.minecraft.world.level.block.Mirror.NONE);
        helper.assertTrue(s != null, "a ghost");
        Spookbouw.zetTempo(helper.getLevel(), s, 5000);
        List<Map.Entry<BlockPos, BlockState>> doel = new java.util.ArrayList<>(s.volgorde());
        helper.assertTrue(Spookbouw.bouw(p, s, false).begonnen(), "it builds");
        helper.succeedWhen(() -> {
            helper.assertTrue(Spookbouw.get(helper.getLevel(), s.id()) == null, "done");
            for (Map.Entry<BlockPos, BlockState> e : doel) {
                helper.assertTrue(helper.getLevel().getBlockState(e.getKey()).is(e.getValue().getBlock()), "every block stands: " + e.getKey());
            }
        });
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

    private static Ontwerp trappen(int n) {
        Map<BlockPos, BlockState> m = new java.util.LinkedHashMap<>();
        for (int x = 0; x < n; x++) {
            m.put(new BlockPos(x, 0, 0), Blocks.OAK_STAIRS.defaultBlockState());
        }
        return Ontwerp.van(m);
    }

    /** 1.10.10: 6 oak stairs from 3 oak logs only (logs -> 12 planks -> 8 stairs); the 2 stairs too many come back. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void bouwgereedschapWerkbankTrappen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.getInventory().add(new ItemStack(Items.OAK_LOG, 3));
        Spook s = Spookbouw.start(p, trappen(6), helper.absolutePos(new BlockPos(3, 2, 3)), net.minecraft.world.level.block.Rotation.NONE,
                net.minecraft.world.level.block.Mirror.NONE);
        helper.assertTrue(s != null, "a ghost");
        nl.juiced.guhs.feature.bouwhulp.kern.Werkplan plan = Spookbouw.plan(p, s);
        helper.assertTrue(plan.tekort().isEmpty() && plan.maakt(Items.OAK_STAIRS) == 6, "the plan crafts all 6 stairs: " + plan.tekort());
        helper.assertTrue(plan.gemaakt().get(0).van().getOrDefault(Items.OAK_LOG, 0) == 3, "from 3 logs: " + plan.gemaakt().get(0).van());
        nl.juiced.guhs.feature.bouwhulp.kern.Spoken.Bouwen b = Spookbouw.bouw(p, s, false);
        helper.assertTrue(b.begonnen() && b.tekort().isEmpty(), "nothing missing, it builds");
        helper.assertTrue(tel(p, Items.OAK_LOG) == 0, "the logs went into the crafting");
        helper.succeedWhen(() -> {
            for (int x = 0; x < 6; x++) {
                helper.assertBlockPresent(Blocks.OAK_STAIRS, new BlockPos(3 + x, 2, 3));
            }
            helper.assertTrue(Spookbouw.get(helper.getLevel(), s.id()) == null, "done");
            helper.assertTrue(tel(p, Items.OAK_STAIRS) == 2 && tel(p, Items.OAK_PLANKS) == 0, "the 2 extra stairs came back ("
                    + tel(p, Items.OAK_STAIRS) + " stairs, " + tel(p, Items.OAK_PLANKS) + " planks)");
        });
    }

    /** A keet whose project is one water block inside a stone ring; the player holds {@code emmers} water buckets. */
    private static Spook waterProject(GameTestHelper helper, ServerPlayer p, int emmers) {
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        for (int i = 0; i < emmers; i++) {
            p.getInventory().add(new ItemStack(Items.WATER_BUCKET));
        }
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                if (x != 4 || z != 4) {
                    helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                }
            }
        }
        BlockPos kern = new BlockPos(12, 2, 3);
        helper.setBlock(kern, BouwhulpFeature.BOUWKEET.get().defaultBlockState());
        nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity keet = (nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(kern));
        Spook s = Spookbouw.start(p, Ontwerp.van(Map.of(BlockPos.ZERO, Blocks.WATER.defaultBlockState())), helper.absolutePos(new BlockPos(4, 2, 4)),
                net.minecraft.world.level.block.Rotation.NONE, net.minecraft.world.level.block.Mirror.NONE);
        helper.assertTrue(s != null && keet.zetProject(helper.getLevel(), s), "the keet's water project");
        return s;
    }

    /** 1.10.10: one water bucket is no infinite source: the water stays a ghost (and the bucket stays). */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void bouwgereedschapWaterEenEmmer(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spook s = waterProject(helper, p, 1);
        nl.juiced.guhs.feature.bouwhulp.kern.Spoken.Bouwen b = Spookbouw.bouw(p, s, false);
        helper.assertTrue(!b.begonnen() && b.tekort().getOrDefault(Items.WATER_BUCKET, 0) == 1, "one more water bucket is missing: " + b.tekort());
        helper.assertTrue(Spookbouw.bouw(p, s, true).begonnen(), "alsnog: it starts");
        helper.succeedWhen(() -> {
            helper.assertTrue(s.status() == Spook.Status.WACHT, "it waits: the water is missing");
            helper.assertTrue(!helper.getBlockState(new BlockPos(4, 2, 4)).is(Blocks.WATER), "no water placed");
            helper.assertTrue(tel(p, Items.WATER_BUCKET) == 1, "the bucket stays");
        });
    }

    /** 1.10.10: two water buckets are an infinite source: the water is placed and both buckets stay. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void bouwgereedschapWaterTweeEmmers(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spook s = waterProject(helper, p, 2);
        nl.juiced.guhs.feature.bouwhulp.kern.Spoken.Bouwen b = Spookbouw.bouw(p, s, false);
        helper.assertTrue(b.begonnen() && b.tekort().isEmpty(), "two buckets: nothing missing " + b.tekort());
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 4)).is(Blocks.WATER)
                    && helper.getBlockState(new BlockPos(4, 2, 4)).getFluidState().isSource(), "a water source stands");
            helper.assertTrue(Spookbouw.get(helper.getLevel(), s.id()) == null, "done");
            helper.assertTrue(tel(p, Items.WATER_BUCKET) == 2, "both buckets stay (" + tel(p, Items.WATER_BUCKET) + ")");
        });
    }

    /** 1.10.10 (N11 prep): a straight Guhslee rail piece costs one sleerail_recht; the keet lays the anchor with its 15 parts. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void bouwgereedschapSleerail(GameTestHelper helper) {
        BlockState anker = nl.juiced.guhs.registry.ModBlocks.SLEE_RAIL.get().defaultBlockState()
                .setValue(nl.juiced.guhs.block.SleeRailBlock.FACING, net.minecraft.core.Direction.WEST)
                .setValue(nl.juiced.guhs.block.SleeRailBlock.SHAPE, nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT);
        BlockPos a = new BlockPos(3, 0, 3);
        Map<BlockPos, BlockState> m = new java.util.LinkedHashMap<>();
        m.put(a, anker);
        List<BlockPos> stuk = new nl.juiced.guhs.slee.SleePath.Placement(a, net.minecraft.core.Direction.WEST, nl.juiced.guhs.slee.SleePath.Shape.STRAIGHT).blocks();
        for (BlockPos p : stuk) {
            if (!p.equals(a)) {
                m.put(p, nl.juiced.guhs.registry.ModBlocks.SLEE_RAIL_PART.get().forOffset(p.subtract(a)));
            }
        }
        helper.assertTrue(stuk.size() == 16, "a straight piece is 16 blocks (" + stuk.size() + ")");
        Ontwerp o = Ontwerp.van(m);
        helper.assertTrue(o.nodig().equals(Map.of(nl.juiced.guhs.registry.ModItems.SLEERAIL_RECHT.get(), 1)), "one rail item, the parts are free: " + o.nodig());
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.getInventory().add(new ItemStack(nl.juiced.guhs.registry.ModItems.SLEERAIL_RECHT.get()));
        Spook s = Spookbouw.start(p, o, helper.absolutePos(new BlockPos(4, 2, 4)), net.minecraft.world.level.block.Rotation.NONE,
                net.minecraft.world.level.block.Mirror.NONE);
        helper.assertTrue(s != null, "a ghost");
        List<Map.Entry<BlockPos, BlockState>> doel = new java.util.ArrayList<>(s.volgorde());
        helper.assertTrue(Spookbouw.bouw(p, s, false).begonnen(), "it builds");
        helper.succeedWhen(() -> {
            helper.assertTrue(Spookbouw.get(helper.getLevel(), s.id()) == null, "done");
            for (Map.Entry<BlockPos, BlockState> e : doel) {
                helper.assertTrue(helper.getLevel().getBlockState(e.getKey()) == e.getValue(), "rail block stands: " + helper.relativePos(e.getKey())
                        + " " + helper.getLevel().getBlockState(e.getKey()));
            }
            helper.assertTrue(tel(p, nl.juiced.guhs.registry.ModItems.SLEERAIL_RECHT.get()) == 0, "the rail item was used");
        });
    }

    /** 1.10.10: a potted plant costs a flower pot plus the plant (it used to be skipped). */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void bouwgereedschapPot(GameTestHelper helper) {
        nl.juiced.guhs.feature.bouwhulp.kern.Materiaal.Kost k = nl.juiced.guhs.feature.bouwhulp.kern.Materiaal.kost(Blocks.POTTED_PINK_TULIP.defaultBlockState());
        helper.assertTrue(k != null && k.item() == Items.FLOWER_POT && k.aantal() == 1 && k.extra() == Items.PINK_TULIP, "a pot + a tulip: " + k);
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        p.getInventory().add(new ItemStack(Items.FLOWER_POT, 1));
        p.getInventory().add(new ItemStack(Items.PINK_TULIP, 1));
        Ontwerp o = Ontwerp.van(Map.of(new BlockPos(0, 0, 0), Blocks.POTTED_PINK_TULIP.defaultBlockState()));
        helper.assertTrue(o.nodig().getOrDefault(Items.FLOWER_POT, 0) == 1 && o.nodig().getOrDefault(Items.PINK_TULIP, 0) == 1, "it needs both");
        Spook s = Spookbouw.start(p, o, helper.absolutePos(new BlockPos(4, 2, 4)), net.minecraft.world.level.block.Rotation.NONE,
                net.minecraft.world.level.block.Mirror.NONE);
        helper.assertTrue(s != null && Spookbouw.bouw(p, s, false).begonnen(), "it builds");
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.POTTED_PINK_TULIP, new BlockPos(4, 2, 4));
            helper.assertTrue(tel(p, Items.FLOWER_POT) == 0 && tel(p, Items.PINK_TULIP) == 0, "both were used");
        });
    }

    /**
     * 1.10.10: the keet's Banksleutel slot links the bank (and unlinks when the key comes out; the old click-link still
     * counts while the slot is empty), and the keet crafts stairs from logs that are only in that bank.
     */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 300)
    public static void bouwgereedschapBankSleutel(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        helper.setBlock(new BlockPos(1, 2, 1), nl.juiced.guhs.registry.ModBlocks.BANK_GUH.get());
        nl.juiced.guhs.block.entity.BankGuhBlockEntity bank = (nl.juiced.guhs.block.entity.BankGuhBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 1)));
        ItemStack sleutel = new ItemStack(nl.juiced.guhs.feature.bank.BankFeature.BANK_SLEUTEL.get());
        nl.juiced.guhs.feature.bank.BankSleutelItem.onthoud(sleutel, bank);
        bank.getStorage().insert(new ItemStack(Items.OAK_LOG), 3);
        BlockPos kern = new BlockPos(12, 2, 3);
        helper.setBlock(kern, BouwhulpFeature.BOUWKEET.get().defaultBlockState());
        nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity keet = (nl.juiced.guhs.feature.bouwhulp.keet.BouwkeetBlockEntity) level.getBlockEntity(helper.absolutePos(kern));
        // the old click-link (an id, no key) still counts
        keet.zetBank(bank.bankId());
        helper.assertTrue(bank.bankId().equals(keet.bank()) && keet.bankOud(), "an old link is kept and shown as old");
        // the slot: the key from the inventory goes in
        p.getInventory().add(sleutel);
        nl.juiced.guhs.feature.bouwhulp.keet.KeetTabs.sleutelVak(p, level, keet);
        helper.assertTrue(!keet.sleutel().isEmpty() && tel(p, nl.juiced.guhs.feature.bank.BankFeature.BANK_SLEUTEL.get()) == 0, "the key is in the slot");
        helper.assertTrue(bank.bankId().equals(keet.bank()) && !keet.bankOud(), "linked through the key; the old link is gone");
        Spook s = Spookbouw.start(p, trappen(6), helper.absolutePos(new BlockPos(2, 2, 10)), net.minecraft.world.level.block.Rotation.NONE,
                net.minecraft.world.level.block.Mirror.NONE);
        helper.assertTrue(s != null && keet.zetProject(level, s), "the keet's project");
        nl.juiced.guhs.feature.bouwhulp.kern.Spoken.Bouwen b = Spookbouw.bouw(p, s, false);
        helper.assertTrue(b.begonnen() && b.tekort().isEmpty(), "stairs crafted from the bank's logs: " + b.tekort());
        helper.assertTrue(bank.getStorage().count(new ItemStack(Items.OAK_LOG)) == 0, "the logs left the bank");
        helper.succeedWhen(() -> {
            for (int x = 0; x < 6; x++) {
                helper.assertBlockPresent(Blocks.OAK_STAIRS, new BlockPos(2 + x, 2, 10));
            }
            helper.assertTrue(keet.project() == null, "done");
            long over = tel(p, Items.OAK_STAIRS) + bank.getStorage().count(new ItemStack(Items.OAK_STAIRS));
            helper.assertTrue(over == 2, "the 2 extra stairs came back (" + over + ")");
            // the key out again: unlinked
            nl.juiced.guhs.feature.bouwhulp.keet.KeetTabs.sleutelVak(p, level, keet);
            helper.assertTrue(keet.sleutel().isEmpty() && keet.bank() == null && tel(p, nl.juiced.guhs.feature.bank.BankFeature.BANK_SLEUTEL.get()) == 1,
                    "the key came back, the bank is unlinked");
        });
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
