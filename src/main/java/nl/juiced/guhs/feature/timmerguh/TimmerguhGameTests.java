package nl.juiced.guhs.feature.timmerguh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeBlock;
import nl.juiced.guhs.feature.huisje.HuisjeFeature;
import nl.juiced.guhs.feature.huisje.HuisjeMaat;
import nl.juiced.guhs.feature.huisje.HuisjePayloads;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.klusjes.KlusjesFeature;
import nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the Timmerguh (3.0, slice timmerguh): the questline "Samen een huisje bouwen" step by step (the talking screen's
 * answers, planks + pink wool, the dakpluisjes on the ghost tiles, the flag, a guh moving into the huisje, the rewards, the
 * optional toy + guhlampje step), a finished roof that starts over for the next player, the huisje recipes need the
 * bouwboekje (it stays in the grid), huisjes placed without the quest keep working, someone else's huisje opens read-only
 * (and changing it is refused), and the bouwplaats jigsaw in every Knuffeldal town (templates and pools).
 * Templates: timmerguh_test_bouw (grass + a little stone roof with six ghost tiles), huisje_test_tuin (24 x 24 grass).
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class TimmerguhGameTests {
    private static final String BOUW = "timmerguh_test_bouw";
    private static final String TUIN = "huisje_test_tuin";
    private static final String BATCH = "timmerguh";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        TimmerguhVoortgang.zet(p, TimmerguhVoortgang.NIEUW);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Praat.vergeet(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static GuhNpcEntity timmerguh(GameTestHelper helper, BlockPos at) {
        GuhNpcEntity n = helper.spawn(ModEntities.GUH_NPC.get(), at);
        n.setKind(GuhNpcEntity.Kind.TIMMERGUH);
        return n;
    }

    static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    static boolean advancement(ServerPlayer p, String name) {
        var holder = p.server.getAdvancements().get(Guhs.id(name.contains("/") ? name : "quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** Lays every ghost tile round the Timmerguh with the player's dakpluisjes (as right-clicks would). */
    static void legAlles(ServerLevel level, GuhNpcEntity npc, ServerPlayer p) {
        for (BlockPos pos : Timmerguh.plekken(npc)) {
            if (level.getBlockState(pos).is(TimmerguhFeature.DAKPLEK.get())) {
                ItemStack stack = ItemStack.EMPTY;
                for (ItemStack s : p.getInventory().items) {
                    if (s.is(TimmerguhFeature.DAKPLUISJE.get())) {
                        stack = s;
                        break;
                    }
                }
                DakpluisjeItem.leg(level, pos, p, stack);
            }
        }
    }

    // =====================================================================================================================
    // the questline
    // =====================================================================================================================

    @GameTest(template = BOUW, batch = BATCH, timeoutTicks = 200)
    public static void timmerguhSamenEenHuisjeBouwen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        GuhNpcEntity npc = timmerguh(helper, new BlockPos(3, 2, 10));
        try {
            helper.assertTrue(Features.role(GuhNpcEntity.Kind.TIMMERGUH) != null && nl.juiced.guhs.feature.verhaal.NpcRollen.van(npc) == Timmerguh.ROLE,
                    "the Timmerguh has his role");
            helper.assertTrue(Timmerguh.plekken(npc).size() == 6 && Timmerguh.open(npc) == 6, "six ghost tiles: " + Timmerguh.plekken(npc));
            // step 0: talking; "wat is een guhhuisje?" changes nothing; "ik help je mee!" asks for the materials
            Timmerguh.ROLE.talk(npc, p);
            helper.assertTrue(TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.NIEUW && advancement(p, "timmerguh_gesproken"), "hallo");
            Timmerguh.ROLE.antwoord(npc, p, Timmerguh.UITLEG);
            helper.assertTrue(TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.NIEUW, "the explanation changes nothing");
            Timmerguh.ROLE.antwoord(npc, p, Timmerguh.HELP);
            helper.assertTrue(TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.MATERIAAL, "step 1: bring planks and pink wool");
            // step 1: not enough yet -> nothing happens; then planks (mixed woods) and pink wool
            p.getInventory().add(new ItemStack(Items.OAK_PLANKS, 10));
            p.getInventory().add(new ItemStack(Items.PINK_WOOL, 8));
            helper.assertTrue(!Timmerguh.inleveren(npc, p) && TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.MATERIAAL, "10 planks aren't 16");
            p.getInventory().add(new ItemStack(Items.CHERRY_PLANKS, 7));
            Timmerguh.ROLE.antwoord(npc, p, Timmerguh.INLEVEREN);
            helper.assertTrue(TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.DAK && count(p, Items.PINK_WOOL) == 0
                    && Timmerguh.planken(p) == 1 && count(p, TimmerguhFeature.DAKPLUISJE.get()) == 6 && advancement(p, "timmerguh_materiaal"),
                    "step 2: 16 planks and 8 wool taken, six dakpluisjes loaned");
            // a dakpluisje only fits on a ghost tile
            BlockPos steen = helper.absolutePos(new BlockPos(1, 1, 1));
            ItemStack pluisjes = p.getInventory().items.stream().filter(s -> s.is(TimmerguhFeature.DAKPLUISJE.get())).findFirst().orElseThrow();
            helper.assertTrue(!DakpluisjeItem.leg(level, steen, p, pluisjes) && count(p, TimmerguhFeature.DAKPLUISJE.get()) == 6,
                    "not on the grass");
            // step 2: lay them: each ghost tile becomes its roof part; the last one puts the flag up
            legAlles(level, npc, p);
            List<BlockPos> ps = Timmerguh.plekken(npc);
            helper.assertTrue(Timmerguh.open(npc) == 0 && level.getBlockState(ps.get(0)).getBlock() != TimmerguhFeature.DAKPLEK.get(), "the roof is on");
            boolean oor = false, binnen = false, dak = false;
            for (BlockPos pos : ps) {
                oor |= level.getBlockState(pos).is(Blocks.PINK_WOOL);
                binnen |= level.getBlockState(pos).is(Blocks.MAGENTA_WOOL);
                dak |= level.getBlockState(pos).getBlock() == net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(Guhs.id("pluisdak"));
            }
            helper.assertTrue(oor && binnen && dak, "dak -> pluisdak, oor -> pink wool, binnenoor -> magenta wool");
            BlockPos vlag = Timmerguh.vlagPlek(npc);
            helper.assertTrue(vlag != null && level.getBlockState(vlag).is(Blocks.PINK_BANNER), "de vlag in top: " + vlag);
            helper.assertTrue(TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.BEWONER && count(p, HuisjeFeature.KLEIN.get().asItem()) == 1
                    && count(p, TimmerguhFeature.DAKPLUISJE.get()) == 0 && advancement(p, "timmerguh_dak") && advancement(p, "verhalen/timmerguh_dak"),
                    "step 3: a small huisje, the leftover pluisjes went back");
            // step 3: without a resident nothing happens; a guh moves into a huisje of the player -> the rewards
            Timmerguh.ROLE.talk(npc, p);
            helper.assertTrue(TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.BEWONER, "nobody lives in it yet");
            Huisje h = HuisjeBlock.bouw(level, helper.absolutePos(new BlockPos(10, 2, 2)), Direction.SOUTH, HuisjeMaat.KLEIN, p.getUUID());
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(9, 2, 8));
            guh.tame(p);
            helper.assertTrue(Huisjes.trekIn(h, guh) && advancement(p, "timmerguh_bewoner"), "a guh moves in (Band moment HUISJE_IN)");
            Timmerguh.ROLE.talk(npc, p);
            helper.assertTrue(TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.KLAAR && TimmerguhVoortgang.klaar(p)
                    && count(p, TimmerguhFeature.BOUWBOEKJE.get()) == 1 && count(p, HuisjeFeature.KLEIN.get().asItem()) == 2
                    && count(p, ModItems.clothingItem(GuhClothes.TIMMER_HELMPJE)) == 1 && advancement(p, "timmerguh_klaar")
                    && advancement(p, "verhalen/timmerguh_klaar"), "step 4: the bouwboekje, a small huisje and the helmpje");
            // the optional step: a guhlampje and a toy in the home area
            Timmerguh.ROLE.talk(npc, p);
            helper.assertTrue(TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.KLAAR, "not knus yet");
            helper.setBlock(new BlockPos(12, 2, 8), KlusjesFeature.GUHLAMPJE.get());
            helper.assertTrue(!Timmerguh.isKnus(p), "a lamp but no toy");
            helper.setBlock(new BlockPos(6, 2, 8), SpeelgoedFeature.GLIJBAANTJE.get());
            helper.succeedWhen(() -> {
                helper.assertTrue(Timmerguh.isKnus(p), "a toy and a guhlampje in the home area");
                Timmerguh.ROLE.talk(npc, p);
                helper.assertTrue(TimmerguhVoortgang.stap(p) == TimmerguhVoortgang.KNUS
                        && count(p, ModItems.clothingItem(GuhClothes.TIMMER_GEREEDSCHAPSRIEM)) == 1 && advancement(p, "timmerguh_knus"),
                        "the gereedschapsriem");
                // afterwards: a lost bouwboekje comes back
                p.getInventory().clearContent();
                Timmerguh.ROLE.talk(npc, p);
                helper.assertTrue(count(p, TimmerguhFeature.BOUWBOEKJE.get()) == 1, "a new bouwboekje when it's lost");
                weg(helper, p);
            });
        } catch (RuntimeException e) {
            weg(helper, p);
            throw e;
        }
    }

    /** A finished roof starts over (all ghost tiles again, the flag down) when the next player hands in the materials. */
    @GameTest(template = BOUW, batch = BATCH)
    public static void timmerguhNieuwDakVoorDeVolgende(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer a = speler(helper, new BlockPos(2, 2, 2)), b = speler(helper, new BlockPos(3, 2, 2));
        GuhNpcEntity npc = timmerguh(helper, new BlockPos(3, 2, 10));
        try {
            for (ServerPlayer p : List.of(a, b)) {
                TimmerguhVoortgang.zet(p, TimmerguhVoortgang.MATERIAAL);
                p.getInventory().add(new ItemStack(Items.BIRCH_PLANKS, 16));
                p.getInventory().add(new ItemStack(Items.PINK_WOOL, 8));
            }
            helper.assertTrue(Timmerguh.inleveren(npc, a), "a hands in");
            legAlles(level, npc, a);
            helper.assertTrue(Timmerguh.open(npc) == 0 && Timmerguh.vlagPlek(npc) != null && TimmerguhVoortgang.stap(a) == TimmerguhVoortgang.BEWONER,
                    "a's roof is done");
            helper.assertTrue(Timmerguh.inleveren(npc, b), "b hands in");
            helper.assertTrue(Timmerguh.open(npc) == 6 && Timmerguh.vlagPlek(npc) == null && count(b, TimmerguhFeature.DAKPLUISJE.get()) == 6
                    && TimmerguhVoortgang.stap(b) == TimmerguhVoortgang.DAK && TimmerguhVoortgang.stap(a) == TimmerguhVoortgang.BEWONER,
                    "a new roof for b: six ghost tiles again, the flag down, a keeps its step");
            for (BlockPos pos : Timmerguh.plekken(npc)) {
                helper.assertTrue(level.getBlockState(pos).is(TimmerguhFeature.DAKPLEK.get()), "ghost again at " + pos);
            }
            helper.assertTrue(level.getBlockState(Timmerguh.plekken(npc).get(0)).getValue(DakplekBlock.DEEL) != null, "with its deel");
            long oren = Timmerguh.plekken(npc).stream().filter(pos -> level.getBlockState(pos).getValue(DakplekBlock.DEEL) != DakplekBlock.Deel.DAK).count();
            helper.assertTrue(oren == 2, "the ear tiles are ear tiles again: " + oren);
        } finally {
            weg(helper, a, b);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the recipes, the old huisjes
    // =====================================================================================================================

    private static CraftingInput klein(boolean boekje) {
        ItemStack w = new ItemStack(Items.PINK_WOOL), p = new ItemStack(Items.OAK_PLANKS);
        List<ItemStack> grid = new ArrayList<>(List.of(w, boekje ? new ItemStack(TimmerguhFeature.BOUWBOEKJE.get()) : ItemStack.EMPTY, w,
                w, new ItemStack(ModItems.KAAS_KNABBELS.get()), w, p, new ItemStack(Items.OAK_DOOR), p));
        return CraftingInput.of(3, 3, grid);
    }

    @GameTest(template = TUIN, batch = BATCH)
    public static void timmerguhReceptenNaHetBouwboekje(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var recepten = level.getRecipeManager();
        helper.assertTrue(recepten.getRecipeFor(RecipeType.CRAFTING, klein(false), level).isEmpty(), "no small huisje without the bouwboekje");
        var recept = recepten.getRecipeFor(RecipeType.CRAFTING, klein(true), level);
        helper.assertTrue(recept.isPresent() && recept.get().value().assemble(klein(true), level.registryAccess()).is(HuisjeFeature.KLEIN.get().asItem()),
                "with the bouwboekje: a small huisje");
        var rest = recept.get().value().getRemainingItems(klein(true));
        helper.assertTrue(rest.stream().anyMatch(s -> s.is(TimmerguhFeature.BOUWBOEKJE.get())), "the bouwboekje stays in the grid");
        for (String maat : List.of("guhhuisje_klein", "guhhuisje_medium", "guhhuisje_groot")) {
            var r = recepten.byKey(Guhs.id(maat));
            helper.assertTrue(r.isPresent() && r.get().value().getIngredients().stream().anyMatch(i -> i.test(new ItemStack(TimmerguhFeature.BOUWBOEKJE.get()))),
                    maat + " needs the bouwboekje");
        }
        helper.succeed();
    }

    /** A huisje placed without the quest (an old one) still works: residents move in, its screen data, the owner renames it. */
    @GameTest(template = TUIN, batch = BATCH)
    public static void timmerguhOudeHuisjesWerkenGewoon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(12, 2, 12));
        try {
            helper.assertTrue(!TimmerguhVoortgang.klaar(p), "never did the quest");
            Huisje h = HuisjeBlock.bouw(level, helper.absolutePos(new BlockPos(4, 2, 4)), Direction.SOUTH, HuisjeMaat.MEDIUM, p.getUUID());
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(10, 2, 10));
            guh.tame(p);
            helper.assertTrue(Huisjes.trekIn(h, guh) && h.bewoners().size() == 1, "a guh moves in");
            helper.assertTrue(HuisjeBlock.bekijk(level, h.pos(), p) == h && HuisjeBlock.bewerk(level, h.pos(), p) == h
                    && HuisjePayloads.data(p, h).getBoolean("MagBewerken"), "its owner uses it as always");
            HuisjePayloads.doe(p, new HuisjePayloads.Doe(h.pos(), HuisjePayloads.Actie.NAAM.ordinal(), "", "Oud Maar Knus", false, -1));
            helper.assertTrue(Huisjes.op(level.getServer(), level.dimension(), h.pos()).naam().equals("Oud Maar Knus"), "renamed");
        } finally {
            weg(helper, p);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // multiplayer: someone else's huisje is read-only
    // =====================================================================================================================

    @GameTest(template = TUIN, batch = BATCH)
    public static void timmerguhAnderMagAlleenKijken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer eigenaar = speler(helper, new BlockPos(12, 2, 12)), ander = speler(helper, new BlockPos(13, 2, 12));
        try {
            Huisje h = HuisjeBlock.bouw(level, helper.absolutePos(new BlockPos(4, 2, 4)), Direction.SOUTH, HuisjeMaat.KLEIN, eigenaar.getUUID());
            Huisjes.zetEigenaarNaam(level.getServer(), h, "Juiced");
            String naam = h.naam();
            // the other one may look (the screen opens) but not change it
            helper.assertTrue(HuisjeBlock.bekijk(level, h.pos(), ander) == h, "someone else opens the screen");
            var data = HuisjePayloads.data(ander, h);
            helper.assertTrue(!data.getBoolean("MagBewerken") && data.getString("EigenaarNaam").equals("Juiced"), "read-only, and whose it is");
            helper.assertTrue(HuisjeBlock.bewerk(level, h.pos(), ander) == null, "no guhs or maatjes in with an item");
            HuisjePayloads.doe(ander, new HuisjePayloads.Doe(h.pos(), HuisjePayloads.Actie.NAAM.ordinal(), "", "Van Mij", false, -1));
            GuhEntity zijnGuh = helper.spawn(ModEntities.GUH.get(), new BlockPos(12, 2, 10));
            zijnGuh.tame(ander);
            HuisjePayloads.doe(ander, new HuisjePayloads.Doe(h.pos(), HuisjePayloads.Actie.TREK_IN.ordinal(), "", "", false, zijnGuh.getId()));
            GuhEntity eigenGuh = helper.spawn(ModEntities.GUH.get(), new BlockPos(11, 2, 10));
            eigenGuh.tame(eigenaar);
            helper.assertTrue(Huisjes.trekIn(h, eigenGuh), "the owner's guh lives there");
            HuisjePayloads.doe(ander, new HuisjePayloads.Doe(h.pos(), HuisjePayloads.Actie.UIT.ordinal(), eigenGuh.getUUID().toString(), "", false, -1));
            Huisje nu = Huisjes.op(level.getServer(), level.dimension(), h.pos());
            helper.assertTrue(nu.naam().equals(naam) && nu.bewoners().size() == 1 && nu.bewoners().contains(nl.juiced.guhs.feature.band.Band.id(eigenGuh)),
                    "renaming, moving in and moving out are all refused for someone else: " + nu.bewoners());
            helper.assertTrue(Huisjes.vanWie(h).getString().contains("Juiced"), "Dit is het huisje van Juiced");
            // the owner may
            helper.assertTrue(HuisjePayloads.data(eigenaar, h).getBoolean("MagBewerken") && HuisjeBlock.bewerk(level, h.pos(), eigenaar) == h, "the owner edits");
        } finally {
            weg(helper, eigenaar, ander);
        }
        helper.succeed();
    }

    // =====================================================================================================================
    // the bouwplaats in every town (templates and pools)
    // =====================================================================================================================

    @GameTest(template = TUIN, batch = BATCH)
    public static void timmerguhBouwplaatsInElkStadje(GameTestHelper helper) {
        var templates = helper.getLevel().getStructureManager();
        var pools = helper.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        // the plein always hangs hoek_noordwest on one of its corners (its pool has only that one), and that corner's street
        // ends in the bouwplaats jigsaw
        var hoekPool = pools.get(Guhs.id("knuffeldal_stadje/hoek_noordwest"));
        helper.assertTrue(hoekPool != null && hoekPool.size() == 1, "one hoek_noordwest element");
        var plein = templates.get(Guhs.id("knuffeldal_stadje/plein")).orElseThrow();
        long naarNoordwest = plein.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW, true).stream()
                .filter(j -> j.nbt().getString("pool").equals("guhs:knuffeldal_stadje/hoek_noordwest")).count();
        helper.assertTrue(naarNoordwest == 1, "the plein has the noordwest corner once");
        StructureTemplate hoek = templates.get(Guhs.id("knuffeldal_stadje/hoek_noordwest")).orElseThrow();
        var bp = hoek.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW, true).stream()
                .filter(j -> j.nbt().getString("name").equals("guhs:knuffeldal_bouwplaats")).toList();
        helper.assertTrue(bp.size() == 1 && bp.get(0).pos().equals(new BlockPos(0, 4, 23))
                && bp.get(0).nbt().getString("target").equals("guhs:bouwplaats_ingang")
                && bp.get(0).nbt().getString("pool").equals("guhs:knuffeldal_stadje/bouwplaats")
                && bp.get(0).state().getValue(JigsawBlock.ORIENTATION) == FrontAndTop.WEST_UP, "the bouwplaats jigsaw: " + bp);
        for (String ander : List.of("noordoost", "zuidoost", "zuidwest")) {
            long n = templates.get(Guhs.id("knuffeldal_stadje/hoek_" + ander)).orElseThrow()
                    .filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW, true).stream()
                    .filter(j -> j.nbt().getString("name").equals("guhs:knuffeldal_bouwplaats")).count();
            helper.assertTrue(n == 0, "only one bouwplaats per town, not in " + ander);
        }
        var pool = pools.get(Guhs.id("knuffeldal_stadje/bouwplaats"));
        helper.assertTrue(pool != null && pool.size() == 1
                && pool.getShuffledTemplates(net.minecraft.util.RandomSource.create(1)).get(0).toString().contains("knuffeldal_stadje/bouwplaats"),
                "the bouwplaats pool");
        StructureTemplate plaats = templates.get(Guhs.id("knuffeldal_stadje/bouwplaats")).orElseThrow();
        helper.assertTrue(plaats.getSize().equals(new net.minecraft.core.Vec3i(22, 26, 33)), "the bouwplaats: 22 x 26 x 33");
        var js = plaats.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW, true);
        helper.assertTrue(js.size() == 1 && js.get(0).pos().equals(new BlockPos(21, 4, 16)) && js.get(0).nbt().getString("name").equals("guhs:bouwplaats_ingang")
                && js.get(0).state().getValue(JigsawBlock.ORIENTATION) == FrontAndTop.EAST_UP, "its own jigsaw: " + js);
        var ghosts = plaats.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), TimmerguhFeature.DAKPLEK.get(), true);
        Map<DakplekBlock.Deel, Long> delen = new java.util.EnumMap<>(DakplekBlock.Deel.class);
        for (var g : ghosts) {
            delen.merge(g.state().getValue(DakplekBlock.DEEL), 1L, Long::sum);
        }
        helper.assertTrue(ghosts.size() >= 30 && delen.getOrDefault(DakplekBlock.Deel.OOR, 0L) >= 8 && delen.getOrDefault(DakplekBlock.Deel.BINNENOOR, 0L) >= 2,
                "the oortjesdak's ghost tiles (dak, ears, inner ears): " + delen);
        helper.assertTrue(!plaats.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.LADDER, true).isEmpty()
                && !plaats.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.SCAFFOLDING, true).isEmpty(), "the steigertje");
        helper.succeed();
    }
}
