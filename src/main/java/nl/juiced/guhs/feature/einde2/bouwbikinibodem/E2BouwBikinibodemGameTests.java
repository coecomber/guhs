package nl.juiced.guhs.feature.einde2.bouwbikinibodem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2Bescherming;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Ontdekt;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.LeegteBouwStructure;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.bouwbikinibodem.BikinibodemBlokken.Bubbelstroom;
import nl.juiced.guhs.feature.einde2.bouwbikinibodem.BikinibodemBlokken.Richting;
import nl.juiced.guhs.feature.einde2.zwevers.Kruimelkwallen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the build of Bikinibodem (einde2, slice bouw_bikinibodem): where the one copy lands in the Guheinde (the
 * deep band, the relaxed search for a terrain-free box), its sidecar of markers, its tiles and overlay (read from the
 * templates: nothing that big is placed in the test world), the leegtemist and the Kwallenvelden as pure geometry, and
 * the lift in a little room of its own ({@code einde2_bouw_bikinibodem_test_lift}, tools/features/einde2_bouw_bikinibodem.py):
 * things, and a player moved the way a client moves, are carried down and up softly; protected, it still carries.
 */
public class E2BouwBikinibodemGameTests {
    private static final String EMPTY = "empty";
    private static final String LIFT = "einde2_bouw_bikinibodem_test_lift";
    private static final String BATCH = "einde2_bouw_bikinibodem";
    private static final String B = Einde2Ids.BIKINIBODEM;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void weg(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    private static Block blok(String id) {
        return BuiltInRegistries.BLOCK.getValue(id.contains(":") ? net.minecraft.resources.Identifier.parse(id) : Guhs.id(id));
    }

    /** The state the build has at this spot (coordinates of the whole build), read from its tile; null: nothing there. */
    private static BlockState inBouw(ServerLevel level, BlockPos lokaal, Block block) {
        StructureTemplate tegel = level.getStructureManager().get(Guhs.id(B + "/stuk_" + lokaal.getX() / 32 + "_" + lokaal.getZ() / 32)).orElse(null);
        if (tegel == null) {
            return null;
        }
        BlockPos in = new BlockPos(lokaal.getX() % 32, lokaal.getY(), lokaal.getZ() % 32);
        for (StructureTemplate.StructureBlockInfo info : tegel.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block)) {
            if (info.pos().equals(in)) {
                return info.state();
            }
        }
        return null;
    }

    /**
     * The one Bikinibodem of a world: it has a spot for three seeds, in its ring, in the deep band, its tiles exist and its
     * markers lie inside it (Einde2Test), and it lands where the search found the emptiest box (which round: logged).
     */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 12000)
    public static void einde2BouwBikinibodemPlaatsing(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, B);
        LeegteBouwStructure bouw = Plekken.leegteBouw(helper.getLevel().registryAccess(), B);
        helper.assertTrue(bouw != null && bouw.ruim() && bouw.leeg().isPresent() && bouw.bij().isEmpty(), "a build of the deep band with a terrain-free box of its own");
        helper.assertTrue(bouw.size().getX() == 224 && bouw.size().getZ() == 224 && bouw.size().getY() <= 114 && bouw.y() == Einde2Ids.LAAG_MIN
                && bouw.anchor().equals(new BlockPos(104, 0, 104)), "224 x (at most 114) x 224 at y 4, anchor 104 0 104: " + bouw.size());
        helper.assertTrue(bouw.tilesX() == 7 && bouw.tilesZ() == 7, "seven by seven tiles");
        StringBuilder rondes = new StringBuilder();
        for (long seed : Einde2Test.SEEDS) {
            int ronde = Einde2Test.leegRonde(Einde2Test.wereld(helper, seed), B);
            helper.assertTrue(ronde >= 1 && ronde <= 3, "seed " + seed + ": found in a search round: " + ronde);
            rondes.append(seed).append(": ronde ").append(ronde).append("  ");
        }
        LOGGER.info("[einde2 bouw_bikinibodem] the search round that found the spot, per seed: {}", rondes);
        helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET)
                .getValue(Guhs.id(B)) == null, "no random copies: Bikinibodem exists exactly once");
        helper.assertTrue(Einde2Ontdekt.inKompas(B) && Einde2Ids.LEEGTE_STRUCTUREN.contains(B), "in the Superkompas, and protected and discovered as every big place");
        helper.succeed();
    }

    /** The sidecar: the markers of the build contract are there, of the right kind, and the cast is named by entity type. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwBikinibodemPlekken(GameTestHelper helper) {
        Plekken.Blad blad = Plekken.blad(B);
        Map<String, Plekken.Plek> p = blad.plekken();
        helper.assertTrue(blad.soort().equals("leegte_bouw") && blad.grootte().getX() == 224 && blad.grootte().getZ() == 224, "the sidecar of the build");
        for (String id : List.of("aankomst", "aankomst_speler", "uitzicht", "rustpunt_1", "rustpunt_2", "rustpunt_3", "bubbel_boven", "bubbel_route",
                "bubbel_onder", "bubbel_omhoog", "koepel", "plein", "weg_1", "weg_2", "weg_3", "stadspoort", "weg_naar_schelpenstad", "krokante_knabbel",
                "krabguh", "kantoor", "kluis", "kassa", "octo_kassa", "grill", "grill_speler", "sponsguh_werk", "parel", "mobiel_start", "sponsguh_huis",
                "sponsguh", "sponsguh_bed", "gerrit", "patguh_rots", "patguh", "octo_huis", "octo", "klarinet_standaard", "vetemmer", "vetemmer_ingang",
                "lab", "plankmika", "karen", "plan_z_kast", "plan_z_plankmika", "sluip_start", "sluip_route", "receptkluis", "bootschool", "pufguh",
                "examen_start", "examen_route", "examen_brug", "examen_finish", "kwallen_patguh", "kwallen_start", "bellenkom", "podium", "podium_octo",
                "podium_sponsguh", "podium_trommel", "tribune", "octoliam", "ijssalon", "ijs_toonbank", "ijsverkoper", "ijs_sponsguh", "ijs_patguh",
                "ijs_speler", "boomkoepel", "koepel_sluis", "sandy", "karate_mat", "raket", "rusthuis", "zeemeerman", "mosselguh", "onzichtbare_boot",
                "riem", "guhlander_anker", "guhlander_dek", "spookkapitein", "guhlander_trap", "neptuguh_troon", "neptuguh", "bevroren_krabguh",
                "hasselguh_landing", "knipoog_guhdalf_brug", "knipoog_guhdalf_omweg", "knipoog_guhdalf_camera", "knipoog_kloon_professor",
                "knipoog_kloon_camera", "knipoog_rookguh_zweef", "knipoog_rookguh_patguh", "knipoog_rookguh_camera", "knipoog_ijs_balto",
                "knipoog_ijs_coupe", "knipoog_ijs_kistje", "knipoog_ijs_camera")) {
            helper.assertTrue(p.containsKey(id), "marker " + id);
        }
        Map<String, Integer> groepen = new java.util.TreeMap<>(Map.of("bewoner_", 10, "besteltafel_", 3, "klant_", 4, "camera_", 4, "laser_", 4, "pylon_", 12));
        groepen.putAll(Map.of("kwallenveld_", 3, "band_", 6, "rook_", 4, "licht_", 4, "raket_onderdeel_", 3));
        for (Map.Entry<String, Integer> groep : groepen.entrySet()) {
            long n = p.keySet().stream().filter(k -> k.startsWith(groep.getKey()) && k.substring(groep.getKey().length()).matches("\\d+")).count();
            helper.assertTrue(n >= groep.getValue(), groep.getKey() + "*: " + n + " of at least " + groep.getValue());
        }
        helper.assertTrue(Plekken.NPC.equals(p.get("krabguh").soort()) && "guhs:meneer_krabguh".equals(p.get("krabguh").entity())
                && "guhs:octo_mika".equals(p.get("octo_kassa").entity()) && "guhs:plankmika".equals(p.get("plan_z_plankmika").entity())
                && "guhs:bikinibodem_guh".equals(p.get("bewoner_1").entity()), "the cast is named by entity type");
        helper.assertTrue("GUHDALF".equals(p.get("knipoog_guhdalf_brug").kind()) && "KNABBELKLOON".equals(p.get("knipoog_kloon_professor").kind()),
                "the guests of the winks by their kind");
        helper.assertTrue(Plekken.ZONE.equals(p.get("koepel").soort()) && Plekken.PAD.equals(p.get("examen_route").soort())
                && p.get("examen_route").punten().size() >= 10 && Plekken.PAD.equals(p.get("knipoog_ijs_camera").soort()), "zones, routes and cameras");
        List<BlockPos> naad = p.get("weg_naar_schelpenstad").punten();
        helper.assertTrue(naad.get(naad.size() - 1).getX() == 223 && naad.get(naad.size() - 1).getY() == 16, "the road ends at the seam, x 223, at walking level 16");
        helper.assertTrue(blad.lagen().containsKey("vliegende_guhlander") && p.get("guhlander_anker").pos().equals(blad.lagen().get("vliegende_guhlander").pos()),
                "the overlay of the ghost ship, anchored at its marker");
        helper.succeed();
    }

    /** The tiles and the overlay as templates: all there, and what the markers promise stands in them. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwBikinibodemTegels(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                helper.assertTrue(level.getStructureManager().get(Guhs.id(B + "/stuk_" + i + "_" + j)).isPresent(), "tile " + i + "," + j);
            }
        }
        Map<String, Plekken.Plek> p = Plekken.van(B);
        helper.assertTrue(inBouw(level, p.get("grill").pos(), blok("krokante_grill")) != null, "the krokante_grill stands at its marker");
        helper.assertTrue(inBouw(level, p.get("karen").pos(), blok("karen")) != null, "Karen stands at her marker");
        for (int i = 1; i <= 3; i++) {
            helper.assertTrue(inBouw(level, p.get("besteltafel_" + i).pos(), blok("besteltafel")) != null, "besteltafel " + i);
        }
        helper.assertTrue(inBouw(level, p.get("kluis").pos(), E2BouwBikinibodemFeature.KLUIS.get()) != null
                && inBouw(level, p.get("receptkluis").pos(), E2BouwBikinibodemFeature.KLUIS.get()) != null
                && inBouw(level, p.get("kassa").pos(), E2BouwBikinibodemFeature.KASSA.get()) != null
                && inBouw(level, p.get("plan_z_kast").pos(), E2BouwBikinibodemFeature.ARCHIEFKAST.get()) != null
                && inBouw(level, p.get("klarinet_standaard").pos(), E2BouwBikinibodemFeature.KLARINET.get()) != null
                && inBouw(level, p.get("pylon_01").pos(), E2BouwBikinibodemFeature.PYLON.get()) != null
                && inBouw(level, p.get("camera_1").pos(), E2BouwBikinibodemFeature.CAMERA.get()) != null, "the safe, the till, the cabinet, the clarinet, a pylon, a camera");
        // the lift: both columns are whole, from the plein up to the island
        Block stroom = E2BouwBikinibodemFeature.BUBBELSTROOM.get();
        List<BlockPos> route = p.get("bubbel_route").punten();
        BlockPos boven = route.get(0), onder = route.get(route.size() - 1), voet = p.get("bubbel_omhoog").pos().offset(0, 0, 3);
        for (int y = onder.getY(); y <= boven.getY(); y++) {
            BlockState omlaag = inBouw(level, new BlockPos(onder.getX(), y, onder.getZ()), stroom);
            BlockState omhoog = inBouw(level, new BlockPos(voet.getX(), y, voet.getZ()), stroom);
            helper.assertTrue(omlaag != null && omlaag.getValue(Bubbelstroom.RICHTING) == Richting.OMLAAG, "the column down at y " + y);
            helper.assertTrue(omhoog != null && omhoog.getValue(Bubbelstroom.RICHTING) == Richting.OMHOOG, "the column up at y " + y);
        }
        helper.assertTrue(boven.getY() - onder.getY() > 80, "the lift is a long way: " + (boven.getY() - onder.getY()));
        // the dome over the middle of the plein, the road at the seam
        BlockPos plein = p.get("plein").pos();
        boolean bel = false;
        for (int y = 70; y < 90 && !bel; y++) {
            bel = inBouw(level, new BlockPos(plein.getX() + 4, y, plein.getZ() + 4), E2BouwBikinibodemFeature.ZEEPBEL.get()) != null;
        }
        helper.assertTrue(bel, "the dome hangs over the plein");
        helper.assertTrue(inBouw(level, new BlockPos(223, 15, 112), blok("gebarsten_korst")) != null && inBouw(level, new BlockPos(223, 15, 109), blok("kruimelrots_stenen")) != null
                && inBouw(level, new BlockPos(223, 16, 108), blok("kruimelrots_stenen_muur")) != null
                && inBouw(level, new BlockPos(223, 16, 116), blok("kruimelrots_stenen_muur")) != null
                && inBouw(level, new BlockPos(223, 13, 112), blok("kaaskorst")) != null, "the road profile at the seam with Schelpenstad");
        // the overlay: blocks only, the column of bubbles from the ground to its deck
        Plekken.Laag laag = Plekken.blad(B).lagen().get("vliegende_guhlander");
        StructureTemplate schip = level.getStructureManager().get(laag.template()).orElse(null);
        helper.assertTrue(schip != null && schip.getSize().equals(laag.grootte()), "the ghost ship template");
        helper.assertTrue(schip.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), stroom).size() > 25
                && schip.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), blok("minecraft:warped_planks")).size() > 40, "a hull, and the way up");
        helper.succeed();
    }

    /** The leegtemist and the Kwallenvelden as geometry of the build, and the per-player answer. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwBikinibodemMistEnVelden(GameTestHelper helper) {
        Map<String, Plekken.Plek> p = Plekken.van(B);
        helper.assertTrue(Leegtemist.inKoepel(p.get("bubbel_onder").pos()) && Leegtemist.inKoepel(p.get("krabguh").pos())
                && Leegtemist.inKoepel(p.get("octoliam").pos()) && Leegtemist.inKoepel(p.get("pufguh").pos()), "the town lies in the mist");
        helper.assertTrue(!Leegtemist.inKoepel(p.get("aankomst_speler").pos()) && !Leegtemist.inKoepel(p.get("hasselguh_landing").pos())
                && !Leegtemist.inKoepel(p.get("weg_naar_schelpenstad").punten().get(2)) && !Leegtemist.inKoepel(new BlockPos(20, 60, 20)),
                "the island above the dome, the pier and the road outside the gate do not");
        Plekken.Plek veld = p.get("kwallenveld_1");
        BlockPos midden = new BlockPos((veld.pos().getX() + veld.tot().getX()) / 2, veld.pos().getY() + 3, (veld.pos().getZ() + veld.tot().getZ()) / 2);
        helper.assertTrue(Leegtemist.inKwallenveld(midden) && Leegtemist.inKoepel(midden), "a Kwallenveld, under the dome");
        helper.assertTrue(!Leegtemist.inKwallenveld(p.get("bubbel_onder").pos()) && !Leegtemist.inKwallenveld(p.get("aankomst_speler").pos()), "the plein is no field");
        for (String id : p.keySet()) {
            if (id.startsWith("kwallenveld_")) {
                Plekken.Plek v = p.get(id);
                helper.assertTrue(v.tot().getX() - v.pos().getX() + 1 >= 24 && v.tot().getZ() - v.pos().getZ() + 1 >= 24 && v.tot().getY() - v.pos().getY() + 1 >= 10,
                        id + " is at least 24 x 10 x 24");
                helper.assertTrue(Leegtemist.inKoepel(v.pos()) && Leegtemist.inKoepel(v.tot().atY(v.pos().getY())), id + " lies under the dome");
            }
        }
        // (the rule is registered with the Kruimelkwallen; the test world is not the Guheinde, so here it says no)
        helper.assertTrue(!Kruimelkwallen.ook(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)) && !Leegtemist.inKoepel(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)),
                "outside the Guheinde: no field, no mist");
        ServerPlayer player = speler(helper);
        try {
            helper.assertTrue(!Leegtemist.inMist(player), "nobody is in the mist by default");
            Leegtemist.zet(player, true);
            helper.assertTrue(Leegtemist.inMist(player), "in");
            Leegtemist.zet(player, false);
            helper.assertTrue(!Leegtemist.inMist(player), "and out again");
        } finally {
            weg(helper, player);
        }
        helper.succeed();
    }

    private static ItemEntity ding(GameTestHelper helper, Vec3 at) {
        ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y + 0.02, at.z, new ItemStack(Items.FEATHER));
        item.setDeltaMovement(Vec3.ZERO);
        item.setPickUpDelay(10000);
        item.setUnlimitedLifetime();
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    /** The two columns of the test room: {the foot of the one going down, the foot of the one going up}. */
    private static BlockPos[] kolommen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos[] voet = new BlockPos[2];
        BlockPos.betweenClosedStream(helper.getBounds()).forEach(q -> {
            BlockState state = level.getBlockState(q);
            if (state.getBlock() instanceof Bubbelstroom && !(level.getBlockState(q.below()).getBlock() instanceof Bubbelstroom)) {
                voet[state.getValue(Bubbelstroom.RICHTING) == Richting.OMLAAG ? 0 : 1] = q.immutable();
            }
        });
        return voet;
    }

    /** The blocks themselves, and the lift with things in it: down softly to the floor, up and off onto the deck. */
    @GuhTest(template = LIFT, batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwBikinibodemLift(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(E2BouwBikinibodemFeature.ZEEPBEL.get() instanceof TransparentBlock
                && E2BouwBikinibodemFeature.ZEEPBEL.get().defaultBlockState().getLightDampening() == 0, "zeepbel is glass: light passes");
        helper.assertTrue(E2BouwBikinibodemFeature.BLOEMWOLK.get().defaultBlockState().getLightEmission() > 0, "a bloemwolk glows a little");
        BlockPos[] voet = kolommen(helper);
        helper.assertTrue(voet[0] != null && voet[1] != null, "the room has a column down and a column up");
        BlockPos omlaag = voet[0], omhoog = voet[1];
        int top = 0;
        while (level.getBlockState(omhoog.above(top + 1)).getBlock() instanceof Bubbelstroom) {
            top++;
        }
        helper.assertTrue(top == 11 && level.getBlockState(omlaag.above(top)).getBlock() instanceof Bubbelstroom, "both twelve high: " + top);
        BlockState stroom = level.getBlockState(omhoog);
        helper.assertTrue(stroom.getCollisionShape(level, omhoog).isEmpty() && stroom.getLightEmission() == 10, "no collision, a soft glow");
        helper.assertTrue(Bubbelstroom.isTop(level, omhoog.above(top)) && !Bubbelstroom.isTop(level, omhoog.above(top - 1)), "the top of a column");
        helper.assertTrue(Bubbelstroom.uitgang(level, omhoog.above(top)) == Direction.WEST, "the column up is open to the west at its top: "
                + Bubbelstroom.uitgang(level, omhoog.above(top)));
        ItemEntity zakker = ding(helper, Vec3.atBottomCenterOf(omlaag.above(top)));
        ItemEntity stijger = ding(helper, Vec3.atBottomCenterOf(omhoog));
        double[] snelst = {0};
        helper.onEachTick(() -> snelst[0] = Math.min(snelst[0], zakker.getDeltaMovement().y));
        helper.succeedWhen(() -> {
            helper.assertTrue(zakker.onGround() && zakker.getY() < omlaag.getY() + 0.5, "the column down brings things to the floor: " + zakker.position());
            helper.assertTrue(snelst[0] >= -Bubbelstroom.ZAK - 0.05, "softly: " + snelst[0]);
            helper.assertTrue(stijger.onGround() && stijger.getY() >= omhoog.getY() + 9.9 && stijger.getX() < omhoog.getX(),
                    "the column up puts things on the deck, west of it: " + stijger.position());
            zakker.discard();
            stijger.discard();
        });
    }

    /** One tick of a player the way a client moves one: the block at the feet carries, otherwise gravity; then the move. */
    private static void stap(ServerLevel level, ServerPlayer player) {
        BlockPos voeten = BlockPos.containing(player.getX(), player.getY() + 1.0E-4, player.getZ());
        BlockState state = level.getBlockState(voeten);
        if (state.getBlock() instanceof Bubbelstroom) {
            state.entityInside(level, voeten, player, InsideBlockEffectApplier.NOOP, true);
        } else {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 0.6, player.onGround() ? 0 : v.y - 0.08, v.z * 0.6);
        }
        player.move(MoverType.SELF, player.getDeltaMovement());
    }

    /**
     * A player in the lift, inside a protected box (as the town is): down from the deck to the floor and, in the other
     * column, up again and off onto the deck, without a scratch; and nobody breaks the bubbles.
     */
    @GuhTest(template = LIFT, batch = BATCH, timeoutTicks = 600)
    public static void einde2BouwBikinibodemLiftSpeler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos[] voet = kolommen(helper);
        BlockPos omlaag = voet[0], omhoog = voet[1];
        BoundingBox box = BoundingBox.fromCorners(BlockPos.containing(helper.getBounds().minX, helper.getBounds().minY, helper.getBounds().minZ),
                BlockPos.containing(helper.getBounds().maxX, helper.getBounds().maxY, helper.getBounds().maxZ));
        Einde2Bescherming.TEST_AREAS.add(box);
        ServerPlayer player = speler(helper);
        List<Runnable> opruimen = new ArrayList<>();
        opruimen.add(() -> Einde2Bescherming.TEST_AREAS.remove(box));
        opruimen.add(() -> weg(helper, player));
        try {
            helper.assertTrue(Einde2Bescherming.beschermd(level, omlaag), "the room is protected like the town");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, omlaag.above(3), level.getBlockState(omlaag.above(3)), player)).isCanceled()
                    && NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, omlaag.above(3).north(), level.getBlockState(omlaag.above(3).north()), player)).isCanceled(),
                    "nobody breaks the bubbelstroom or its tube of zeepbel");
        } catch (RuntimeException e) {
            opruimen.forEach(Runnable::run);
            throw e;
        }
        helper.runAfterDelay(590, () -> {            // (a ride that never ends must not leave its player and its box behind)
            opruimen.forEach(Runnable::run);
            opruimen.clear();
        });
        float leven = player.getHealth();
        player.snapTo(omlaag.getX() + 0.5, omlaag.getY() + 11, omlaag.getZ() + 0.5);
        player.setDeltaMovement(Vec3.ZERO);
        int[] fase = {0};
        double[] snelst = {0};
        helper.onEachTick(() -> {
            stap(level, player);
            snelst[0] = Math.min(snelst[0], player.getDeltaMovement().y);
            if (fase[0] == 0 && player.onGround() && player.getY() < omlaag.getY() + 0.5) {
                // down: on the floor, softly; a fall right after the ride costs nothing either
                fase[0] = snelst[0] >= -Bubbelstroom.ZAK - 0.05 && player.fallDistance == 0
                        && CommonHooks.onLivingFall(player, 12, 1).getDamageMultiplier() == 0 ? 1 : -1;
                player.snapTo(omhoog.getX() + 0.5, omhoog.getY(), omhoog.getZ() + 0.5);
                player.setDeltaMovement(Vec3.ZERO);
            } else if (fase[0] == 1 && player.onGround() && player.getY() >= omhoog.getY() + 9.9 && player.getX() < omhoog.getX() - 0.5) {
                fase[0] = 2;
            }
        });
        helper.succeedWhen(() -> {
            if (fase[0] < 0) {
                opruimen.forEach(Runnable::run);
                opruimen.clear();
            }
            helper.assertTrue(fase[0] >= 0, "the ride down was not soft: fastest " + snelst[0] + ", fall distance " + player.fallDistance);
            helper.assertTrue(fase[0] == 2, "the player is carried down and up again (phase " + fase[0] + ", at " + player.position() + ")");
            helper.assertTrue(player.getHealth() >= leven, "without a scratch");
            opruimen.forEach(Runnable::run);
            opruimen.clear();
        });
    }
}
