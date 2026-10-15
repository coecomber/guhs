package nl.juiced.guhs.feature.einde2.bouwvuur;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Einde2Test;
import nl.juiced.guhs.feature.einde2.LeegteBouwStructure;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * Game tests of the einde2 slice bouw_vuur: the build of the Vuurnatie-hoofdstad. The python module already checked the
 * build itself (every marker standable and reachable, light, closed basins, nothing that hurts); here: the structure as the
 * game knows it (its one guaranteed copy, its band, its tiles: {@link Einde2Test#controleerLeegteBouw}), the sidecar of
 * markers a later phase reads (every id of the build contract, in the right kind, inside the build), a real tile and the
 * overlay placed in the world, and the six decoration blocks (the lava and the flame do not hurt).
 * Room: {@code einde2_bouw_vuur_test_kamer} (one of each block on a floor; tools/features/einde2_bouw_vuur.py). The
 * overlay and the tile are bigger than a test room may be: they are put down high above the test's own spot ({@link #HOOG_LAAG},
 * {@link #HOOG_TEGEL}: apart, so that the two tests never touch each other's blocks).
 */
public class E2BouwVuurGameTests {
    private static final String EMPTY = "empty";
    private static final String KAMER = "einde2_bouw_vuur_test_kamer";
    /** How far above the test's own spot the overlay and the tile are put down. */
    private static final int HOOG_LAAG = 60, HOOG_TEGEL = 100;
    private static final String BATCH = "einde2_bouw_vuur";
    private static final String BOUW = E2BouwVuurFeature.STRUCTUUR;

    /** The markers of build contract 6.7 (and the standard ones of 6.6): id -> kind; "naam#n" = at least n of naam_1, naam_2... */
    private static final String[][] PLEKKEN = {
            {Plekken.ZONE, "aankomst", "kamp", "school", "dansgrot", "dansvloer", "paleis", "troonzaal", "vlamgordijn", "paleisvijver", "invasie_strand",
                    "bunker", "zonnekrijger_ruine", "drakengrot_rood", "drakengrot_blauw", "drakendans", "kokend_rotsblok", "kokend_meer", "gevangenis_plein",
                    "sintelstrand", "strandhuis", "theater", "toneel", "lotus_kamp", "lotus_tent", "duel_arena", "ronde_lucht", "ronde_water", "ronde_aarde",
                    "ronde_vuur", "kroning"},
            {Plekken.PROP, "aankomst_speler", "uitzicht", "appa_kamp", "rustpunt#3", "havenpoort", "schoolbank_speler", "portret_mikai", "dans_speler", "troon",
                    "brood", "bunker_ingang", "bunker_troon", "zuka_vader", "trap_onder", "trap_boven", "eeuwige_vlam", "gondel_onder", "gondel_boven", "cel_suki",
                    "koelcel", "azula_gondel", "publiek_speler", "verzoening_zuka", "leeuwschildpad_anker", "leeuwschildpad_rug", "leeuwschildpad_kop", "pilaar#12", "duel_speler",
                    "duel_mikai", "komeet_zicht", "komeetscherf", "kroning_zuka", "knipoog_sjokkel_finish", "knipoog_stitch_paneel", "knipoog_stitch_celdeur#3"},
            {Plekken.NPC, "team_aang", "team_momo", "team_katara", "team_sokka", "team_toph", "team_zuka", "poortwacht_1", "poortwacht_2", "bewoner#8", "juf",
                    "leerling#5", "muzikant#3", "mikai", "azula_bunker", "zonnekrijger", "draak_rood", "draak_blauw", "suki", "gevangene#5", "directeur", "ty_lee",
                    "acteur#5", "miroh_tent", "bumi", "pakguh_lotus", "piandao", "leeuwschildpad", "knipoog_stitch_bewaker"},
            {Plekken.PAD, "drakendans_speler", "drakendans_zuka", "gondel_route", "inbraak_route", "knipoog_sjokkel", "knipoog_sjokkel_camera",
                    "knipoog_stitch_camera"},
            {Plekken.WEZEN, "eendguh#4"},
    };

    /** The one guaranteed copy per world: in its ring, in the high band, every tile there, every marker inside its box. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 1200)
    public static void einde2BouwVuurPlaatsing(GameTestHelper helper) {
        Einde2Test.controleerLeegteBouw(helper, BOUW);
        LeegteBouwStructure bouw = Plekken.leegteBouw(helper.getLevel().registryAccess(), BOUW);
        helper.assertTrue(bouw != null && bouw.size().equals(new Vec3i(256, 118, 256)) && bouw.tilesX() == 8 && bouw.tilesZ() == 8,
                "256 x 118 x 256 in 8 x 8 tiles");
        helper.assertTrue(bouw.y() >= Einde2Ids.HOOG_MIN && bouw.y() + bouw.size().getY() - 1 <= Einde2Ids.HOOG_MAX, "in the high band: y " + bouw.y());
        helper.assertTrue(bouw.rand() == 48 && bouw.leeg().isEmpty() && bouw.bij().isEmpty() && !bouw.ruim(), "a story structure: a rim of 48, nothing of the deep band");
        var structures = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        helper.assertTrue(structures.get(Guhs.id(BOUW)).orElseThrow().is(Einde2Ids.VERHAAL), "tagged as a story structure");
        helper.assertTrue(Einde2Ids.VERHAAL_STRUCTUREN.contains(BOUW) && !SuperkompasItem.allowed(BOUW), "protected with the story structures, in no compass");
        int tegels = 0;
        for (int i = 0; i < bouw.tilesX(); i++) {
            for (int j = 0; j < bouw.tilesZ(); j++) {
                StructureTemplate t = helper.getLevel().getStructureManager().get(bouw.tile(i, j)).orElse(null);
                if (t != null) {
                    tegels++;
                    helper.assertTrue(t.getSize().getX() == 32 && t.getSize().getZ() == 32 && t.getSize().getY() == 118, "tile " + i + "," + j + ": " + t.getSize());
                }
            }
        }
        helper.assertTrue(tegels >= 56, "the tiles of the slab (its corners may be empty): " + tegels);
        helper.succeed();
    }

    /** The sidecar: every marker of the build contract, of the right kind, inside the build; the characters have their names. */
    @GuhTest(template = EMPTY, batch = BATCH)
    public static void einde2BouwVuurPlekken(GameTestHelper helper) {
        Plekken.Blad blad = Plekken.blad(BOUW);
        Map<String, Plekken.Plek> plekken = blad.plekken();
        helper.assertTrue(blad.soort().equals("leegte_bouw") && blad.grootte().equals(new Vec3i(256, 118, 256)), "the sidecar of the capital: " + blad.grootte());
        List<String> mis = new ArrayList<>();
        for (String[] rij : PLEKKEN) {
            for (int k = 1; k < rij.length; k++) {
                String[] deel = rij[k].split("#");
                int n = deel.length > 1 ? Integer.parseInt(deel[1]) : 0;
                for (int nr = n == 0 ? 0 : 1; nr <= n; nr++) {
                    Plekken.Plek p = plekken.get(n == 0 ? deel[0] : deel[0] + "_" + nr);
                    if (p == null && n > 0) {
                        p = plekken.get(deel[0] + "_0" + nr);          // (the pillars are numbered pilaar_01 ...)
                    }
                    if (p == null || !p.soort().equals(rij[0])) {
                        mis.add(rij[k] + (n > 0 ? " nr " + nr : "") + (p == null ? "" : " is a " + p.soort()));
                    }
                }
            }
        }
        helper.assertTrue(mis.isEmpty(), "markers missing or of the wrong kind: " + mis);
        for (Plekken.Plek p : plekken.values()) {
            if (p.id().equals("uitzicht")) {
                continue;                                                // (the one spot outside the build: the opening shot)
            }
            List<BlockPos> punten = new ArrayList<>(p.punten());
            punten.add(p.pos());
            if (p.tot() != null) {
                punten.add(p.tot());
            }
            for (BlockPos q : punten) {
                helper.assertTrue(q.getX() >= 0 && q.getX() < 256 && q.getY() >= 0 && q.getY() < 118 && q.getZ() >= 0 && q.getZ() < 256, p.id() + " lies outside the build: " + q);
            }
        }
        helper.assertTrue("VUURHEER_MIKAI".equals(plekken.get("mikai").kind()) && "AZULA_MIKA".equals(plekken.get("azula_bunker").kind())
                && "OOM_MIROH".equals(plekken.get("miroh_tent").kind()) && "SUKI_GUH".equals(plekken.get("suki").kind())
                && "LEEUWSCHILDPAD_GUH".equals(plekken.get("leeuwschildpad").kind()), "the characters a later phase puts on the spots");
        Plekken.Plek eend = plekken.get("eendguh_1");
        helper.assertTrue("guhs:schildpad_eendguh".equals(eend.entity()) && BuiltInRegistries.ENTITY_TYPE.containsKey(Guhs.id("schildpad_eendguh")),
                "the schildpad-eendguhs of the pond are a real entity type");
        Plekken.Plek aankomst = plekken.get("aankomst"), vloer = plekken.get("dansvloer");
        helper.assertTrue(aankomst.tot().getX() - aankomst.pos().getX() >= 8 && aankomst.tot().getZ() - aankomst.pos().getZ() >= 8, "a landing place of at least 9 x 9");
        helper.assertTrue(vloer.tot().getX() - vloer.pos().getX() >= 8 && vloer.tot().getZ() - vloer.pos().getZ() >= 8, "a dance floor of at least 9 x 9");
        helper.assertTrue(plekken.get("knipoog_sjokkel_camera").punten().size() == 2 && plekken.get("gondel_route").punten().size() >= 3,
                "a camera is an eye and a look-at point; the gondola line has its points");
        Plekken.Laag laag = blad.lagen().get(E2BouwVuurFeature.LAAG_LEEUWSCHILDPAD);
        helper.assertTrue(laag != null && laag.template().equals(Guhs.id(BOUW + "/laag_leeuwschildpad")) && laag.pos().equals(plekken.get("leeuwschildpad_anker").pos()),
                "the overlay of the Leeuwschildpad-guh starts at its anchor: " + laag);
        helper.assertTrue(laag.pos().getX() + laag.grootte().getX() <= 256 && laag.pos().getY() + laag.grootte().getY() <= 118
                && laag.pos().getZ() + laag.grootte().getZ() <= 256, "the overlay lies inside the build");
        helper.succeed();
    }

    /** The overlay: placed for everybody and taken away again (its blocks become air: it holds blocks only). */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public static void einde2BouwVuurLeeuwschildpad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Plekken.Blad blad = Plekken.blad(BOUW);
        Plekken.Laag laag = blad.lagen().get(E2BouwVuurFeature.LAAG_LEEUWSCHILDPAD);
        BlockPos at = helper.absolutePos(new BlockPos(0, HOOG_LAAG, 0));
        // (an overlay only goes down where the chunks are loaded: it is wider than a test's own spot)
        BlockPos tot = at.offset(laag.grootte());
        for (int cx = at.getX() >> 4; cx <= tot.getX() >> 4; cx++) {
            for (int cz = at.getZ() >> 4; cz <= tot.getZ() >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
            }
        }
        helper.assertTrue(Plekken.laag(level, laag, at, false), "the Leeuwschildpad-guh surfaces");
        // where you wake up on its back: a block to stand on with room above it; its snout carries the talk spot
        Plekken.Plek rug = blad.plekken().get("leeuwschildpad_rug"), snuit = blad.plekken().get("leeuwschildpad");
        for (Plekken.Plek p : List.of(rug, snuit)) {
            BlockPos hier = at.offset(p.pos().subtract(laag.pos()));
            helper.assertTrue(!level.getBlockState(hier.below()).isAir() && level.getBlockState(hier).isAir() && level.getBlockState(hier.above()).isAir(),
                    p.id() + ": two free blocks on a solid one of the overlay");
        }
        int blokken = 0, pijn = 0;
        for (BlockPos p : BlockPos.betweenClosed(at, at.offset(laag.grootte().getX() - 1, laag.grootte().getY() - 1, laag.grootte().getZ() - 1))) {
            BlockState state = level.getBlockState(p);
            blokken += state.isAir() ? 0 : 1;
            pijn += state.hasBlockEntity() || !state.getFluidState().isEmpty() ? 1 : 0;
        }
        helper.assertTrue(blokken > 3000 && pijn == 0, "a whole Leeuwschildpad-guh of plain blocks: " + blokken + " blocks, " + pijn + " with a block entity or fluid");
        helper.assertTrue(Plekken.laag(level, laag, at, true), "and dives again");
        for (BlockPos p : BlockPos.betweenClosed(at, at.offset(laag.grootte().getX() - 1, laag.grootte().getY() - 1, laag.grootte().getZ() - 1))) {
            helper.assertTrue(level.getBlockState(p).isAir(), "nothing is left at " + p);
        }
        for (int cx = at.getX() >> 4; cx <= tot.getX() >> 4; cx++) {
            for (int cz = at.getZ() >> 4; cz <= tot.getZ() >> 4; cz++) {
                level.setChunkForced(cx, cz, false);
            }
        }
        helper.succeed();
    }

    /** A real tile in the world (the one with the throne): the template loads with our blocks and its states, signs carry a translate key. */
    @GuhTest(template = EMPTY, batch = BATCH, timeoutTicks = 400)
    public static void einde2BouwVuurTegel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Plekken.Blad blad = Plekken.blad(BOUW);
        BlockPos troon = blad.plekken().get("troon").pos(), mikai = blad.plekken().get("mikai").pos(), gordijn = blad.plekken().get("vlamgordijn").pos();
        int i = troon.getX() / 32, j = troon.getZ() / 32;
        helper.assertTrue(mikai.getX() / 32 == i && mikai.getZ() / 32 == j, "the throne and the Vuurheer's spot lie in one tile");
        StructureTemplate tegel = level.getStructureManager().get(Guhs.id(BOUW + "/stuk_" + i + "_" + j)).orElseThrow();
        BlockPos at = helper.absolutePos(new BlockPos(0, HOOG_TEGEL, 0));
        tegel.placeInWorld(level, at, at, new StructurePlaceSettings().setKnownShape(true), level.getRandom(), 2);
        BlockPos shift = at.offset(-i * 32, 0, -j * 32);
        helper.assertTrue(level.getBlockState(shift.offset(troon)).is(Blocks.POLISHED_BLACKSTONE_STAIRS), "the throne: " + level.getBlockState(shift.offset(troon)));
        helper.assertTrue(level.getBlockState(shift.offset(mikai)).isAir() && level.getBlockState(shift.offset(mikai).above()).isAir()
                && !level.getBlockState(shift.offset(mikai).below()).isAir(), "the Vuurheer's spot is free and has a floor");
        // the flame curtain: lukewarm flames on glowing coal (the zone's first cell, if this tile holds it)
        int vlammen = 0, borden = 0, vertaald = 0;
        for (BlockPos p : BlockPos.betweenClosed(at, at.offset(31, 117, 31))) {
            BlockState state = level.getBlockState(p);
            if (state.is(E2BouwVuurFeature.LAUWE_VLAM.get())) {
                vlammen++;
            }
            helper.assertTrue(!state.is(Blocks.LAVA) && !state.is(Blocks.FIRE) && !state.is(Blocks.MAGMA_BLOCK) && !state.is(Blocks.CAMPFIRE),
                    "nothing that hurts: " + state + " at " + p);
            if (level.getBlockEntity(p) instanceof SignBlockEntity sign) {
                borden++;
                String tekst = sign.getFrontText().getMessage(0, false).getContents().toString();
                vertaald += tekst.contains("sign.guhs.einde2_bouw_vuur.") ? 1 : 0;
            }
        }
        helper.assertTrue(vlammen >= 10, "the flame curtain and the braziers of the throne room: " + vlammen + " flames");
        if (gordijn.getX() / 32 == i && gordijn.getZ() / 32 == j) {
            helper.assertTrue(level.getBlockState(shift.offset(gordijn)).is(E2BouwVuurFeature.LAUWE_VLAM.get())
                    && level.getBlockState(shift.offset(gordijn).below()).is(BarbecuetherFeature.GLOEIKOOL.get()), "the flame curtain burns on glowing coal");
        }
        helper.assertTrue(borden == 0 || vertaald == borden, "every sign reads in the player's own language: " + vertaald + " of " + borden);
        helper.succeed();
    }

    /** The six decoration blocks: there, with an item; the lava and the flame glow and hurt nobody. */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 200)
    public static void einde2BouwVuurBlokken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(E2BouwVuurFeature.BLOKKEN.size() == 6 && E2BouwVuurFeature.BLOK_ITEMS.size() == 6, "six blocks, six items");
        for (int k = 0; k < E2BouwVuurFeature.BLOKKEN.size(); k++) {
            DeferredBlock<Block> blok = E2BouwVuurFeature.BLOKKEN.get(k);
            BlockPos p = new BlockPos(1 + k, 2, 2);
            helper.assertBlockPresent(blok.get(), p);
            helper.assertTrue(blok.getId().getPath().startsWith("einde2_bouw_vuur_") && E2BouwVuurFeature.BLOK_ITEMS.get(k).get().getBlock() == blok.get(),
                    blok.getId() + ": the id prefix of the slice, and its item");
        }
        BlockState lava = E2BouwVuurFeature.LAUWE_LAVA.get().defaultBlockState(), vlam = E2BouwVuurFeature.LAUWE_VLAM.get().defaultBlockState();
        BlockPos lavaPos = helper.absolutePos(new BlockPos(1, 2, 2)), vlamPos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertTrue(lava.getLightEmission() == 15 && vlam.getLightEmission() == 15, "both glow");
        helper.assertTrue(!lava.getCollisionShape(level, lavaPos).isEmpty() && lava.getFluidState().isEmpty(), "the lava is a solid block you walk on");
        helper.assertTrue(vlam.getCollisionShape(level, vlamPos).isEmpty(), "you walk through the flame");
        Pig opLava = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 3, 2));
        Pig inVlam = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 2));
        helper.runAfterDelay(60, () -> {
            for (Pig pig : List.of(opLava, inVlam)) {
                helper.assertTrue(pig.isAlive() && pig.getHealth() == pig.getMaxHealth() && !pig.isOnFire() && pig.getRemainingFireTicks() <= 0,
                        "lukewarm: nobody is hurt and nobody burns (" + pig.getHealth() + ", fire " + pig.getRemainingFireTicks() + ")");
            }
            helper.assertTrue(inVlam.blockPosition().equals(vlamPos), "the pig stands in the flame: " + inVlam.blockPosition());
            helper.succeed();
        });
    }
}
