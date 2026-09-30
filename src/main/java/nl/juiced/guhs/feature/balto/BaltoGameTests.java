package nl.juiced.guhs.feature.balto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.baltoslee.BaltoSleeFeature;
import nl.juiced.guhs.feature.baltoslee.NomguhRoute;
import nl.juiced.guhs.feature.baltoslee.SleeTocht;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the balto slice: the Nomguh route file on the template (every point a track block with room for the sled,
 * markers all along it, the special places), the questline step by step with a fake medicine ride (SleeTocht moments), the
 * rewards (Baltoguh released once per player, the sneeuwslee, the beeldje, the title, the four outfits), a new try after
 * TE_LAAT, the Baltoguh fast in the snow and sniffing the way home, the beeldje, the title in the player list.
 * Template balto_test_sneeuw: 20 x 6 x 12, snow for x &lt; 10, stone for the rest (the floor at helper y 1).
 */
public class BaltoGameTests {
    private static final String SNEEUW = "balto_test_sneeuw";
    private static final String BATCH = "balto";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        BaltoVerhaal.wis(p);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Praat.vergeet(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        BlockPos abs = helper.absolutePos(at);
        npc.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    /** Answers the scene that is open now (as if the player clicked option id). */
    static void antwoord(ServerPlayer p, String sleutel, int optie) {
        Praat.doeAlsOf(p, sleutel, null);
        Praat.antwoord(p, null, optie);
    }

    static int tel(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    // =================================================================================================================
    // the route on the template
    // =================================================================================================================

    /**
     * The route file: enough points, at most 4 blocks apart, no step over 1 block; on the Nomguh template every point is a
     * track block (sled-track snow, or ice on the bridge) with air above it; a route marker at least every 20 blocks (the
     * bridge has its posts); the rest points, the bridge, the avalanche, the dieptepunt and the three places are in range.
     */
    @GuhTest(template = SNEEUW, batch = BATCH, timeoutTicks = 400)
    public static void baltoRouteOpHetSjabloon(GameTestHelper helper) {
        NomguhRoute r = NomguhRoute.laad();
        List<Vec3> heen = r.heen();
        helper.assertTrue(heen.size() >= 60 && r.lengte() >= 250, "a long route: " + heen.size() + " points, " + r.lengte() + " blocks");
        for (int i = 1; i < heen.size(); i++) {
            double d = Math.hypot(heen.get(i).x - heen.get(i - 1).x, heen.get(i).z - heen.get(i - 1).z);
            helper.assertTrue(d <= 4.01, "point " + i + " is " + d + " from the one before");
            helper.assertTrue(Math.abs(heen.get(i).y - heen.get(i - 1).y) <= 1, "no big step at point " + i);
        }
        helper.assertTrue(r.terug().size() == heen.size() && r.terug().get(0).equals(heen.get(heen.size() - 1)), "back = the same line reversed");
        helper.assertTrue(r.rust().size() >= 3 && r.rust().size() <= 5, "3-5 rest points");
        helper.assertTrue(!r.ijsbrug().isEmpty() && !r.lawine().isEmpty() && r.lawine().get(0).links(), "a bridge and an avalanche on the left");
        helper.assertTrue(r.dieptepunt() > 0 && r.dieptepunt() < heen.size() - 1, "the dieptepunt on the route");
        helper.assertTrue(r.tijd().containsKey("makkelijk") && r.tijd().containsKey("medium") && r.tijd().containsKey("lastig"), "three time limits");
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id("nomguh")).orElse(null);
        helper.assertTrue(t != null, "the Nomguh template exists");
        helper.assertTrue(r.anker().getX() == t.getSize().getX() / 2 && r.anker().getZ() == t.getSize().getZ() / 2, "the anchor in the middle");
        StructurePlaceSettings s = new StructurePlaceSettings();
        Set<BlockPos> spoor = new HashSet<>();
        for (Block b : new Block[]{BaltoFeature.SNEEUWSPOOR.get(), Blocks.PACKED_ICE, Blocks.BLUE_ICE}) {
            t.filterBlocks(BlockPos.ZERO, s, b).forEach(info -> spoor.add(info.pos()));
        }
        Set<BlockPos> lucht = new HashSet<>();
        t.filterBlocks(BlockPos.ZERO, s, Blocks.AIR).forEach(info -> lucht.add(info.pos()));
        List<BlockPos> palen = t.filterBlocks(BlockPos.ZERO, s, BaltoFeature.ROUTEPAAL.get()).stream().map(StructureTemplate.StructureBlockInfo::pos).toList();
        helper.assertTrue(palen.size() >= 60, "route markers: " + palen.size());
        for (int i = 0; i < heen.size(); i++) {
            Vec3 v = heen.get(i);
            BlockPos p = BlockPos.containing(v.x + 0.5, v.y, v.z + 0.5);
            BlockPos q = new BlockPos((int) Math.round(v.x), (int) v.y, (int) Math.round(v.z));
            helper.assertTrue(spoor.contains(q) || spoor.contains(p), "point " + i + " " + v + " lies on the track");
            helper.assertTrue(lucht.contains(q.above()) && lucht.contains(q.above(2)), "room for the sled above point " + i);
        }
        for (int i = 0; i + 7 < heen.size(); i += 7) {
            final int from = i;
            boolean brug = r.ijsbrug().stream().anyMatch(b -> b[0] - 3 <= from + 7 && from <= b[1] + 3);
            boolean paal = false;
            for (int k = from; k <= from + 7 && !paal; k++) {
                Vec3 v = heen.get(k);
                paal = palen.stream().anyMatch(pp -> Math.hypot(pp.getX() + 0.5 - v.x, pp.getZ() + 0.5 - v.z) <= 6.5);
            }
            helper.assertTrue(paal || brug, "a marker along points " + from + ".." + (from + 7));
        }
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, s, BaltoFeature.MEDICIJNKIST.get()).size() == 1, "one medicine chest (in the berghut)");
        helper.succeed();
    }

    // =================================================================================================================
    // the questline
    // =================================================================================================================

    /** The whole questline with a fake medicine ride: meet Baltoguh, Rosy, Boris, the ride's moments, the rewards. */
    @GuhTest(template = SNEEUW, batch = BATCH, timeoutTicks = 400)
    public static void baltoVerhaalStapVoorStap(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5));
        GuhEntity balto = VerhaalGuhs.maakKopie(helper.getLevel(), VerhaalGuh.BALTOGUH, helper.absolutePos(new BlockPos(3, 2, 3)));
        GuhNpcEntity rosy = npc(helper, GuhNpcEntity.Kind.ROSY, new BlockPos(8, 2, 3));
        GuhNpcEntity boris = npc(helper, GuhNpcEntity.Kind.BORIS, new BlockPos(3, 2, 8));
        // 0: meet Baltoguh (the scene's answer moves on)
        BaltoVerhaal.klikBalto(balto, p, InteractionHand.MAIN_HAND);
        antwoord(p, "balto_ontmoet", 1);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.ONTMOET, "met Baltoguh: " + BaltoVerhaal.stap(p));
        // Boris sends you to Rosy first
        BaltoRollen.BORIS.talk(boris, p);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.ONTMOET, "Boris doesn't skip Rosy");
        BaltoRollen.ROSY.talk(rosy, p);
        antwoord(p, "balto_rosy", 1);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.BIJ_ROSY, "visited Rosy");
        BaltoRollen.BORIS.talk(boris, p);
        antwoord(p, "balto_boris", -1);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.KLAAR_VOOR_TOCHT, "Boris' advice");
        // without a Nomguh the sled can't leave (balto-slee's SleeTocht says no): the step stays
        BaltoVerhaal.klikBalto(balto, p, InteractionHand.MAIN_HAND);
        antwoord(p, "balto_start", 1);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.KLAAR_VOOR_TOCHT || SleeTocht.bezig(p), "no ride without a sled");
        // the ride (fake moments): START, BERGHUT (the kist), DIEPTEPUNT (the white wolf-guh), AANKOMST
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.START);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.HEEN, "the ride started");
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.BERGHUT);
        helper.assertTrue("balto_berghut".equals(Praat.lopend(p)), "the berghut scene is open");
        antwoord(p, "balto_berghut", 1);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.TERUG && tel(p, BaltoFeature.MEDICIJNKIST_ITEM.get()) == 1, "the kist in the pockets");
        antwoord(p, "balto_berghut", -1);
        helper.assertTrue(tel(p, BaltoFeature.MEDICIJNKIST_ITEM.get()) == 1, "only one kist");
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.DIEPTEPUNT);
        List<GuhNpcEntity> wolven = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, p.getBoundingBox().inflate(12),
                n -> n.getKind() == GuhNpcEntity.Kind.WITTE_WOLFGUH);
        helper.assertTrue(wolven.size() == 1, "the white wolf-guh appeared: " + wolven.size());
        antwoord(p, "balto_wolf", 1);
        helper.assertTrue(wolven.get(0).roleData.getLongOr(BaltoVerhaal.WOLF_TOT, 0L) <= helper.getLevel().getGameTime() + 60, "she fades after the howl");
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.AANKOMST);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.AANGEKOMEN, "back in time");
        helper.assertTrue(!Nomguh.verhaalKlaar(p), "not done before Rosy has her medicine");
        // Rosy gets the kist: the rewards
        BaltoRollen.ROSY.talk(rosy, p);
        antwoord(p, "balto_feest", 1);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.KLAAR && Nomguh.verhaalKlaar(p), "the questline is done");
        helper.assertTrue(VerhaalGuhs.isVrij(p, VerhaalGuh.BALTOGUH) && VerhaalGuhs.magTemmen(p, VerhaalGuh.BALTOGUH), "Baltoguh released");
        helper.assertTrue(tel(p, BaltoFeature.MEDICIJNKIST_ITEM.get()) == 0, "the kist went to Rosy");
        helper.assertTrue(tel(p, BaltoSleeFeature.SNEEUWSLEE.get()) == 1, "the own sneeuwslee");
        helper.assertTrue(tel(p, BaltoFeature.BALTOGUH_BEELDJE_ITEM.get()) == 1, "the beeldje");
        for (var c : BaltoFeature.KLEDING) {
            helper.assertTrue(tel(p, ModItems.clothingItem(c)) == 1, "outfit " + c);
            helper.assertTrue("nomguh".equals(KledingBronnen.bron(c)), "source of " + c);
        }
        helper.assertTrue(BaltoVerhaal.isHeld(p), "the title");
        helper.assertTrue(!BaltoVerhaal.beloon(p, null), "the rewards only once");
        helper.assertTrue(tel(p, BaltoFeature.BALTOGUH_BEELDJE_ITEM.get()) == 1, "still one beeldje");
        wolven.forEach(Entity::discard);
        weg(helper, p);
        helper.succeed();
    }

    /** Too late (or stopped): "Njeg, nog een keer!" - back to the start, the kist gone; a new ride works again. */
    @GuhTest(template = SNEEUW, batch = BATCH)
    public static void baltoTeLaatNogEenKeer(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5));
        BaltoVerhaal.zet(p, BaltoVerhaal.KLAAR_VOOR_TOCHT);
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.START);
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.BERGHUT);
        antwoord(p, "balto_berghut", 1);
        helper.assertTrue(tel(p, BaltoFeature.MEDICIJNKIST_ITEM.get()) == 1, "the kist");
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.TE_LAAT);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.KLAAR_VOOR_TOCHT && tel(p, BaltoFeature.MEDICIJNKIST_ITEM.get()) == 0, "try again");
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.START);
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.GESTOPT);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.KLAAR_VOOR_TOCHT, "stopped: try again");
        // a moment for someone who isn't on the ride changes nothing
        BaltoVerhaal.zet(p, BaltoVerhaal.NIEUW);
        BaltoVerhaal.opMoment(p, SleeTocht.Moment.AANKOMST);
        helper.assertTrue(BaltoVerhaal.stap(p) == BaltoVerhaal.NIEUW, "no AANKOMST without a ride");
        weg(helper, p);
        helper.succeed();
    }

    /** After the questline Baltoguh comes home with you once; a second time no; another player not before his own story. */
    @GuhTest(template = SNEEUW, batch = BATCH)
    public static void baltoEenKeerMee(GameTestHelper helper) {
        ServerPlayer a = speler(helper, new BlockPos(5, 2, 5)), b = speler(helper, new BlockPos(12, 2, 5));
        GuhEntity kopie = VerhaalGuhs.maakKopie(helper.getLevel(), VerhaalGuh.BALTOGUH, helper.absolutePos(new BlockPos(3, 2, 3)));
        BaltoVerhaal.zet(a, BaltoVerhaal.AANGEKOMEN);
        BaltoVerhaal.beloon(a, null);
        BaltoVerhaal.klikBalto(kopie, a, InteractionHand.MAIN_HAND);
        helper.assertTrue("balto_tem".equals(Praat.lopend(a)), "Baltoguh asks to come along");
        antwoord(a, "balto_tem", 1);
        List<GuhEntity> eigen = helper.getLevel().getEntitiesOfClass(GuhEntity.class, a.getBoundingBox().inflate(8),
                g -> g.getVariant() == GuhVariant.BALTOGUH && g.isTame() && a.getUUID().equals(g.getOwnerUUID()));
        helper.assertTrue(eigen.size() == 1 && VerhaalGuhs.heeftGetemd(a, VerhaalGuh.BALTOGUH), "one tamed Baltoguh");
        helper.assertTrue(BaltoVerhaal.tem(a) == null, "not a second one");
        helper.assertTrue(BaltoVerhaal.tem(b) == null && !VerhaalGuhs.magTemmen(b, VerhaalGuh.BALTOGUH), "the other player first does the story");
        helper.assertTrue(!kopie.isTame() && VerhaalGuhs.isKopie(kopie), "the story copy stays in Nomguh");
        eigen.forEach(Entity::discard);
        weg(helper, a, b);
        helper.succeed();
    }

    // =================================================================================================================
    // the Baltoguh variant
    // =================================================================================================================

    /** Fast in the snow: on snow the speed modifier (and ridden x1.3), on stone not. */
    @GuhTest(template = SNEEUW, batch = BATCH, timeoutTicks = 200)
    public static void baltoSnelInDeSneeuw(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(10, 2, 10));
        GuhEntity sneeuw = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 2, 5));
        GuhEntity steen = helper.spawn(ModEntities.GUH.get(), new BlockPos(15, 2, 5));
        for (GuhEntity g : List.of(sneeuw, steen)) {
            g.setVariant(GuhVariant.BALTOGUH);
            g.tame(p);
            g.setOrderedToSit(true);
        }
        helper.succeedWhen(() -> {
            var a = sneeuw.getAttribute(Attributes.MOVEMENT_SPEED);
            var b = steen.getAttribute(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(sneeuw.onGround() && steen.onGround(), "on the ground");
            helper.assertTrue(a != null && a.hasModifier(BaltoGedrag.SNEEUW_MODIFIER), "snow: faster");
            helper.assertTrue(b != null && !b.hasModifier(BaltoGedrag.SNEEUW_MODIFIER), "stone: normal");
            var gedrag = VariantGedragen.van(GuhVariant.BALTOGUH);
            helper.assertTrue(gedrag != null && gedrag.riddenSpeed(sneeuw, 1f) > 1.2f && gedrag.riddenSpeed(steen, 1f) == 1f, "ridden faster on snow");
            sneeuw.discard();
            steen.discard();
            weg(helper, p);
        });
    }

    /** Snuffel!: the guh-menu button; he smells your bed (your spawn point), shows the pose, lays a trail. */
    @GuhTest(template = SNEEUW, batch = BATCH)
    public static void baltoSnuffelDeWegNaarHuis(GameTestHelper helper) {
        ServerPlayer p = speler(helper, new BlockPos(5, 2, 5));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(6, 2, 6));
        guh.setVariant(GuhVariant.BALTOGUH);
        guh.tame(p);
        var gedrag = VariantGedragen.van(GuhVariant.BALTOGUH);
        helper.assertTrue(gedrag != null && "gui.guhs.balto.snuffel".equals(gedrag.speciaalKnop()), "a Snuffel! button");
        BlockPos bed = helper.absolutePos(new BlockPos(5, 2, 5)).offset(300, 0, -120);
        p.setRespawnPosition(Level.OVERWORLD, bed, 0, true, false);
        Vec3 doel = BaltoGedrag.snuffel(guh, p);
        boolean overworld = helper.getLevel().dimension() == Level.OVERWORLD;
        helper.assertTrue(!overworld || doel != null && doel.distanceTo(Vec3.atBottomCenterOf(bed)) < 1, "he smells the bed: " + doel);
        helper.assertTrue(GuhHooks.heeft(guh, VerhaalVlaggen.SNUFFELT) && BaltoGedrag.snuffelt(guh), "the sniffing pose");
        helper.assertTrue("oosten".equals(BaltoGedrag.richting(10, 0)) && "noorden".equals(BaltoGedrag.richting(0, -10))
                && "zuidwesten".equals(BaltoGedrag.richting(-10, 10)), "the directions");
        guh.discard();
        weg(helper, p);
        helper.succeed();
    }

    // =================================================================================================================
    // the beeldje, the title
    // =================================================================================================================

    /** The Baltoguh-beeldje: a block that faces you, with a lore line; the title "Held van Nomguh" in the player list. */
    @GuhTest(template = SNEEUW, batch = BATCH)
    public static void baltoBeeldjeEnTitel(GameTestHelper helper) {
        BlockPos at = new BlockPos(4, 2, 4);
        helper.setBlock(at, BaltoFeature.BALTOGUH_BEELDJE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        helper.assertBlockPresent(BaltoFeature.BALTOGUH_BEELDJE.get(), at);
        helper.assertTrue(helper.getBlockState(at).getValue(HorizontalDirectionalBlock.FACING) == Direction.EAST, "it faces east");
        helper.assertTrue(BaltoFeature.BALTOGUH_BEELDJE_ITEM.get().getBlock() == BaltoFeature.BALTOGUH_BEELDJE.get(), "the item places it");
        ServerPlayer p = speler(helper, new BlockPos(6, 2, 6));
        helper.assertTrue(!mentions(p.getTabListDisplayName(), "gui.guhs.balto.titel"), "no title yet");
        BaltoVerhaal.zet(p, BaltoVerhaal.AANGEKOMEN);
        BaltoVerhaal.beloon(p, null);
        helper.assertTrue(mentions(p.getTabListDisplayName(), "gui.guhs.balto.titel"), "the title in the player list");
        weg(helper, p);
        helper.succeed();
    }

    static boolean mentions(Component c, String key) {
        if (c == null) {
            return false;
        }
        if (c.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) {
            return true;
        }
        for (Component s : c.getSiblings()) {
            if (mentions(s, key)) {
                return true;
            }
        }
        return false;
    }
}
