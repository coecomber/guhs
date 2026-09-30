package nl.juiced.guhs.feature.knuffelbad;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the Knuffelbad (2.8): the slides' paths (loaded the same on every side, continuous, the ride's speed), the
 * real structure (every running part of every slide has slide blocks under it and room above it, the start gates,
 * Badmeester Bubbel, the wash tubs), whole rides down all three slides (autopilot: ducks, points, eendjesmunten, the
 * highscores, the Knus tab, advancements, getting off at the pool), one rider per slide, the washing ritual (in the right
 * order: shiny for a day), Badmeester Bubbel's shop and gifts, the prize rules (+1), the protection, the rubber duck.
 */
public class KnuffelbadGameTests {
    private static final String EMPTY = "empty";
    private static final String BAD = "knuffelbad";
    private static final String TOBBE = "knuffelbad_test_wastobbe";
    /** Its own batch: the big Knuffelbad template would otherwise shift where the other tests are laid out. */
    private static final String BATCH = "knuffelbad";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            GlijRit.spelerWeg(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int aantal(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id(name.contains("/") ? name : "quest/" + name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** The three start gates in the placed Knuffelbad (by their slide). */
    private static List<BlockPos> poorten(GameTestHelper helper) {
        List<BlockPos> out = new ArrayList<>();
        AABB b = helper.getBounds();
        for (BlockPos p : BlockPos.betweenClosed((int) b.minX, (int) b.minY, (int) b.minZ, (int) b.maxX, (int) b.maxY, (int) b.maxZ)) {
            if (helper.getLevel().getBlockState(p).is(KnuffelbadFeature.GLIJBAAN_START.get())) {
                out.add(p.immutable());
            }
        }
        return out;
    }

    private static BlockPos poort(List<BlockPos> poorten, ServerLevel level, Glijbaan g) {
        for (BlockPos p : poorten) {
            if (level.getBlockState(p).getValue(KnuffelbadBlocks.GLIJBAAN) == g) {
                return p;
            }
        }
        return null;
    }

    private static boolean vast(BlockState s) {
        return !s.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
    }

    // =================================================================================================================
    // the paths
    // =================================================================================================================

    /** The three paths load (from the mod's own files, like the client does), they are smooth, and the rides end. */
    @GuhTest(template = EMPTY)
    public static void knuffelbadPadenZijnGlad(GameTestHelper helper) {
        for (Glijbaan g : Glijbaan.values()) {
            GlijPad pad = GlijPad.laad(g.id());
            helper.assertTrue(pad.id.equals(g.id()) && pad.samples() > 100 && pad.lengte > 80, g + ": a long path (" + pad.lengte + ")");
            helper.assertTrue(pad.duur() > 80 && pad.duur() < 1200, g + ": a ride of " + pad.duur() + " ticks");
            helper.assertTrue(Math.abs(pad.sAt(pad.duur()) - pad.lengte) < 1e-6 && pad.sAt(0) == 0, g + ": from the top to the end");
            helper.assertTrue(pad.eendjes.size() >= 15, g + ": ducks on the slide: " + pad.eendjes.size());
            helper.assertTrue(pad.merk.containsKey("plons"), g + ": a PLONS at the end");
            Vec3 prev = null;
            Vec3[] prevLat = new Vec3[2];
            Vec3[] prevN = new Vec3[2];
            for (double s = 0; s <= pad.lengte; s += 0.25) {
                for (double lat : new double[]{-1, 0, 1}) {
                    GlijPad.Stand st = pad.stand(s, lat);
                    helper.assertTrue(Math.abs(st.normal().length() - 1) < 1e-3 && Math.abs(st.tangent().length() - 1) < 1e-3, g + ": unit vectors at " + s);
                    helper.assertTrue(st.normal().dot(st.tangent()) < 0.2 || st.prof() == GlijPad.BUIS, g + ": the normal stands on the ride at " + s);
                }
                Vec3 p = pad.stand(s, 0).pos();
                if (prev != null) {
                    helper.assertTrue(p.distanceTo(prev) < 0.4, g + ": no jumps in the path at " + s + ": " + p.distanceTo(prev));
                }
                prev = p;
                // off the middle too: from one cross-section into the next (chute, funnel, tube, flight) without a jump,
                // and the ring never flips over (the normal turns only a little per step)
                for (int k = 0; k < 2; k++) {
                    GlijPad.Stand st = pad.stand(s, k == 0 ? -1 : 1);
                    if (prevLat[k] != null) {
                        helper.assertTrue(st.pos().distanceTo(prevLat[k]) < 0.6, g + ": no jumps at the side (" + (k == 0 ? -1 : 1) + ") at " + s + ": "
                                + st.pos().distanceTo(prevLat[k]));
                        helper.assertTrue(st.normal().dot(prevN[k]) > 0.8, g + ": the ring turns smoothly at " + s);
                    }
                    prevLat[k] = st.pos();
                    prevN[k] = st.normal();
                }
            }
            // the speed is fine everywhere: never standing still, never crazy fast
            for (int t = 0; t < pad.duur(); t++) {
                double v = pad.snelheid(t);
                helper.assertTrue(v > 0.01 && v <= pad.vMax + 1e-6, g + ": speed " + v + " at tick " + t);
            }
            // turned with its start gate: the same shape, facing another way
            for (Direction f : Direction.Plane.HORIZONTAL) {
                GlijPad.Baan baan = new GlijPad.Baan(pad, new BlockPos(100, 64, -40), f);
                Vec3 a = baan.stand(10, 0).pos(), b = baan.stand(30, 0).pos();
                Vec3 la = pad.stand(10, 0).pos(), lb = pad.stand(30, 0).pos();
                helper.assertTrue(Math.abs(a.distanceTo(b) - la.distanceTo(lb)) < 1e-6, g + ": turned " + f + " keeps its shape");
            }
        }
        helper.assertTrue(GlijPad.van(Glijbaan.GROTE_PLONS) == GlijPad.van(Glijbaan.GROTE_PLONS), "loaded once");
        helper.succeed();
    }

    // =================================================================================================================
    // the real Knuffelbad
    // =================================================================================================================

    /**
     * The slides in the structure itself: under every running part of every ride there are slide blocks (glijgoot) and
     * above it there is room: the rides follow the visible slides. Plus the gates, Badmeester Bubbel and the tubs.
     */
    @GuhTest(batch = BATCH, template = BAD, timeoutTicks = 400)
    public static void knuffelbadGlijbanenVolgenDeBlokken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<BlockPos> poorten = poorten(helper);
        helper.assertTrue(poorten.size() == 3, "three start gates: " + poorten);
        for (Glijbaan g : Glijbaan.values()) {
            BlockPos poort = poort(poorten, level, g);
            helper.assertTrue(poort != null, "the gate of " + g);
            helper.assertTrue(vast(level.getBlockState(poort.below())), g + ": the gate stands on a floor");
            GlijPad.Baan baan = new GlijPad.Baan(g.pad(), poort, level.getBlockState(poort).getValue(KnuffelbadBlocks.GlijbaanStart.FACING));
            int onder = 0, getest = 0, vrij = 0, ruimte = 0;
            for (double s = 0; s < baan.pad().lengte; s += 1.0) {
                int prof = baan.pad().profiel(s);
                GlijPad.Stand st = baan.stand(s, 0);
                if (prof == GlijPad.GOOT || prof == GlijPad.TRECHTER || prof == GlijPad.BUIS) {
                    getest++;
                    Vec3 q = st.pos().subtract(st.normal().scale(0.12));
                    BlockPos a = BlockPos.containing(q), b = BlockPos.containing(q.subtract(0, 0.6, 0));
                    if (level.getBlockState(a).is(KnuffelbadFeature.GLIJGOOT.get()) || level.getBlockState(b).is(KnuffelbadFeature.GLIJGOOT.get())) {
                        onder++;
                    }
                }
                ruimte++;
                BlockPos boven = BlockPos.containing(st.midden(baan.pad().ring).add(st.normal().scale(0.9)));
                if (!vast(level.getBlockState(boven)) || level.getBlockState(boven).is(KnuffelbadFeature.GLIJGOOT.get())) {
                    vrij++;
                }
            }
            helper.assertTrue(getest > 30 && onder >= getest * 0.97, g + ": slide blocks under the ride: " + onder + " of " + getest);
            helper.assertTrue(vrij >= ruimte * 0.97, g + ": room for the rider: " + vrij + " of " + ruimte);
            // where you get off: a floor with room above it, at the pool
            BlockPos uit = BlockPos.containing(baan.uitstap());
            helper.assertTrue(vast(level.getBlockState(uit.below())) && !vast(level.getBlockState(uit)) && !vast(level.getBlockState(uit.above())),
                    g + ": getting off at " + uit);
        }
        var npcs = level.getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds(), n -> n.getKind() == GuhNpcEntity.Kind.BADMEESTERGUH);
        helper.assertTrue(npcs.size() == 1, "Badmeester Bubbel is in the bath hall: " + npcs.size());
        int tobben = 0, glim = 0, schuim = 0;
        AABB b = helper.getBounds();
        for (BlockPos p : BlockPos.betweenClosed((int) b.minX, (int) b.minY, (int) b.minZ, (int) b.maxX, (int) b.maxY, (int) b.maxZ)) {
            BlockState st = level.getBlockState(p);
            if (st.is(KnuffelbadFeature.GUH_WASTOBBE.get())) {
                tobben++;
            } else if (st.is(KnuffelbadFeature.GLIMTEGEL.get())) {
                glim++;
            } else if (st.is(KnuffelbadFeature.SCHUIM_BLOK.get())) {
                schuim++;
            }
        }
        helper.assertTrue(tobben >= 4 && glim > 500 && schuim > 50, "wash tubs " + tobben + ", star tiles " + glim + ", foam " + schuim);
        helper.succeed();
    }

    /**
     * Down all three slides (the autopilot steers to the ducks): points, eendjesmunten (with the first-ride bonus), the
     * highscore of each slide, the Knus tab, the advancements, and off at the pool; one rider per slide at a time.
     */
    @GuhTest(batch = BATCH, template = BAD, timeoutTicks = 600)
    public static void knuffelbadRitVanBovenTotBeneden(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<BlockPos> poorten = poorten(helper);
        ServerPlayer p = speler(helper, new BlockPos(48, 6, 70));
        ServerPlayer ander = speler(helper, new BlockPos(47, 6, 70));
        int munten = 0;
        for (Glijbaan g : Glijbaan.values()) {
            BlockPos poort = poort(poorten, level, g);
            Direction facing = level.getBlockState(poort).getValue(KnuffelbadBlocks.GlijbaanStart.FACING);
            helper.assertTrue(GlijRit.start(p, poort, g, facing), "the ride down " + g + " starts");
            GlijRit rit = GlijRit.van(p);
            helper.assertTrue(rit != null && p.getVehicle() instanceof ZwembandjeEntity, g + ": in the zwembandje");
            helper.assertTrue(KnusVoortgang.teller(p, KnuffelbadVoortgang.BEZOCHT) == 1, g + ": a ride counts as finding the Knuffelbad");
            helper.assertTrue(!GlijRit.start(ander, poort, g, facing) && !GlijRit.rijdt(ander), g + ": one rider at a time");
            helper.assertTrue(Minigames.KNUFFELBAD.equals(Minigames.playing(p)) && Minigames.busyElsewhere(p, Minigames.RACE), g + ": one game at a time");
            long eenden = level.getEntitiesOfClass(BadeendjeEntity.class, helper.getBounds().inflate(8), BadeendjeEntity::vanRit).size();
            helper.assertTrue(eenden == rit.eenden().size() && eenden >= 15, g + ": the ducks float on the slide: " + eenden);
            rit.autopiloot = true;
            rit.slaAftellenOver(level);
            helper.assertTrue(rit.fase() == ZwembandjeEntity.GLIJDT, g + ": VAHOEG!");
            p.hurt(level.damageSources().fall(), 6f);
            helper.assertTrue(p.getHealth() == p.getMaxHealth(), g + ": riders can't get hurt");
            for (int i = 0; i < 3000 && rit.bezig(); i++) {
                rit.tick(level);
            }
            helper.assertTrue(!rit.bezig() && !GlijRit.rijdt(p), g + ": down at the bottom");
            helper.assertTrue(p.getVehicle() == null, g + ": out of the ring");
            helper.assertTrue(level.getEntitiesOfClass(ZwembandjeEntity.class, helper.getBounds().inflate(8)).isEmpty(), g + ": the ring is gone");
            helper.assertTrue(level.getEntitiesOfClass(BadeendjeEntity.class, helper.getBounds().inflate(8), BadeendjeEntity::vanRit).isEmpty(),
                    g + ": the ride's ducks are gone");
            int gepakt = rit.gepakt(), score = rit.score();
            helper.assertTrue(gepakt >= rit.eenden().size() * 0.8, g + ": the autopilot picked up the ducks: " + gepakt + " of " + rit.eenden().size());
            helper.assertTrue(score >= gepakt * 10 + GlijRit.PUNTEN_FINISH, g + ": points " + score);
            munten += GlijRit.munten(score) + GlijRit.EERSTE_MUNTEN + (gepakt == rit.eenden().size() ? GlijRit.ALLE_MUNTEN : 0);
            helper.assertTrue(aantal(p, KnuffelbadFeature.EENDJESMUNT.get()) == munten, g + ": eendjesmunten " + aantal(p, KnuffelbadFeature.EENDJESMUNT.get())
                    + " (expected " + munten + ")");
            helper.assertTrue(GlijRit.best(p, g) == score && GlijRit.ritten(p, g) == 1, g + ": your record");
            helper.assertTrue(Scorebord.top(p.level().getServer(), g.board()).stream().anyMatch(e -> e.player().equals(p.getUUID()) && e.score() == score), g + ": the world's top 3");
            helper.assertTrue(KnusVoortgang.teller(p, KnuffelbadVoortgang.teller(g)) == 1, g + ": the Knus tab counts the ride");
            helper.assertTrue(advancement(p, "knuffelbad_" + g.id()), g + ": the slide's quest advancement");
            Vec3 uit = rit.baan.uitstap();
            helper.assertTrue(p.position().distanceTo(uit) < 1.0 && vast(level.getBlockState(p.blockPosition().below())), g + ": off at the pool: " + p.position());
            helper.assertTrue(Minigames.playing(p) == null, g + ": no longer in a game");
        }
        helper.assertTrue(advancement(p, "knuffelbad_alle_glijbanen") && advancement(p, "knuffeldal/knuffelbad_alle_glijbanen"), "all three slides: a challenge");
        helper.assertTrue(KnusVoortgang.teller(p, KnuffelbadVoortgang.EENDJES) > 40, "ducks on the Knus tab: " + KnusVoortgang.teller(p, KnuffelbadVoortgang.EENDJES));
        weg(helper, p, ander);
        helper.succeed();
    }

    /** Getting off halfway (hold sneak), logging out: the ride ends, the ring and the ducks go, you land at the pool, no prize. */
    @GuhTest(batch = BATCH, template = BAD, timeoutTicks = 400)
    public static void knuffelbadUitstappenEnWeggaan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos poort = poort(poorten(helper), level, Glijbaan.GLIMTUNNEL);
        Direction facing = level.getBlockState(poort).getValue(KnuffelbadBlocks.GlijbaanStart.FACING);
        ServerPlayer p = speler(helper, new BlockPos(48, 6, 70));
        helper.assertTrue(GlijRit.start(p, poort, Glijbaan.GLIMTUNNEL, facing), "on the Glimtunnel");
        GlijRit rit = GlijRit.van(p);
        rit.slaAftellenOver(level);
        for (int i = 0; i < 60; i++) {
            rit.tick(level);
        }
        p.stopRiding();
        rit.tick(level);
        helper.assertTrue(p.getVehicle() instanceof ZwembandjeEntity, "you can't just get out on the way");
        p.setShiftKeyDown(true);
        for (int i = 0; i <= GlijRit.UITSTAP_HOUD && rit.bezig(); i++) {
            rit.tick(level);
        }
        p.setShiftKeyDown(false);
        helper.assertTrue(!rit.bezig() && p.getVehicle() == null, "holding sneak: off the slide");
        helper.assertTrue(aantal(p, KnuffelbadFeature.EENDJESMUNT.get()) == 0 && GlijRit.ritten(p, Glijbaan.GLIMTUNNEL) == 0, "no prize for stopping");
        helper.assertTrue(p.position().distanceTo(rit.baan.uitstap()) < 1.0, "safely at the pool, not up in the tube: " + p.position());
        // logging out halfway
        helper.assertTrue(GlijRit.start(p, poort, Glijbaan.GLIMTUNNEL, facing), "again");
        GlijRit tweede = GlijRit.van(p);
        tweede.slaAftellenOver(level);
        for (int i = 0; i < 30; i++) {
            tweede.tick(level);
        }
        GlijRit.spelerWeg(p);
        helper.assertTrue(!GlijRit.rijdt(p) && !tweede.bezig(), "logging out ends the ride");
        helper.assertTrue(level.getEntitiesOfClass(BadeendjeEntity.class, helper.getBounds().inflate(8), BadeendjeEntity::vanRit).isEmpty()
                && level.getEntitiesOfClass(ZwembandjeEntity.class, helper.getBounds().inflate(8)).isEmpty(), "the ring and the ducks are gone");
        helper.assertTrue(GlijRit.op(level, poort) == null, "the slide is free again");
        weg(helper, p);
        helper.succeed();
    }

    // =================================================================================================================
    // the rules
    // =================================================================================================================

    /** Points and prizes: a duck is 10 (special 25, gold 50), a combo adds 2 per duck in a row; every prize rule is one more. */
    @GuhTest(template = EMPTY)
    public static void knuffelbadPuntenEnPrijzen(GameTestHelper helper) {
        helper.assertTrue(GlijRit.punten(Eendsoort.NORMAAL, 1) == 10 && GlijRit.punten(Eendsoort.NORMAAL, 3) == 14
                && GlijRit.punten(Eendsoort.PLUISEENDJE, 1) == 25 && GlijRit.punten(Eendsoort.GOUDEN_EENDJE, 1) == 50, "duck points");
        helper.assertTrue(GlijRit.punten(Eendsoort.NORMAAL, 100) == 10 + GlijRit.COMBO_MAX * GlijRit.COMBO_BONUS, "the combo stops growing");
        for (int score = 0; score <= 1000; score += 5) {
            int basis = 1 + score / 40;
            helper.assertTrue(GlijRit.munten(score) == basis + 1, "eendjesmunten for " + score + ": " + GlijRit.munten(score));
        }
        helper.assertTrue(GlijRit.EERSTE_MUNTEN == 3 + 1 && GlijRit.RECORD_MUNTEN == 1 + 1 && GlijRit.ALLE_MUNTEN == 2 + 1
                && Wasritueel.WAS_MUNTEN == 1 + 1, "every prize rule is one more (2.8)");
        helper.assertTrue(Eendsoort.SPECIAAL.size() == 12 && !Eendsoort.SPECIAAL.contains(Eendsoort.NORMAAL), "twelve special ducks");
        Set<Eendsoort> overal = EnumSet.noneOf(Eendsoort.class);
        for (Eendsoort e : Eendsoort.SPECIAAL) {
            int op = 0;
            for (Glijbaan g : Glijbaan.values()) {
                op += e.op(g) ? 1 : 0;
            }
            helper.assertTrue(op == 1 || op == 3, e + ": on one slide or on all");
            if (op == 3) {
                overal.add(e);
            }
        }
        for (Glijbaan g : Glijbaan.values()) {
            helper.assertTrue(Eendsoort.SPECIAAL.stream().anyMatch(e -> e.alleenOp == g), g + " has its own special ducks");
        }
        helper.assertTrue(overal.contains(Eendsoort.GOUDEN_EENDJE), "the golden duck can be anywhere");
        var collectie = KnusVoortgang.verzameling(KnuffelbadVoortgang.BADEENDJES);
        helper.assertTrue(collectie != null && collectie.items().size() == 12 && collectie.onderdeel().equals("knuffelbad"), "the badeendjes page");
        helper.assertTrue(KnusVoortgang.mijlpalen("knuffelbad").size() >= 4, "Knus milestones: " + KnusVoortgang.mijlpalen("knuffelbad").size());
        TagKey<Item> tickets = KnusTags.GRIJPTICKETS;
        helper.assertTrue(new ItemStack(KnuffelbadFeature.EENDJESMUNT.get()).is(tickets), "an eendjesmunt is a grijpmachine ticket");
        helper.succeed();
    }

    // =================================================================================================================
    // the washing ritual
    // =================================================================================================================

    /** Inzepen, schuimen, spoelen, föhnen - in that order - and your guh shines for a day (then it's over). */
    @GuhTest(template = TOBBE, timeoutTicks = 200)
    public static void knuffelbadWasritueel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, new BlockPos(2, 2, 2));
        BlockPos tobbe = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.assertTrue(level.getBlockState(tobbe).is(KnuffelbadFeature.GUH_WASTOBBE.get()), "the tub");
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(6, 2, 6));
        GuhEntity wild = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 2, 6));
        helper.assertTrue(!Wasritueel.inzepen(p, wild) && Wasritueel.stap(wild) == 0, "only your own tamed guh");
        guh.tame(p);
        // out of order: nothing happens
        Wasritueel.fohn(p, guh);
        Wasritueel.douche(p, tobbe);
        helper.assertTrue(Wasritueel.stap(guh) == 0, "first the shampoo");
        helper.assertTrue(Wasritueel.inzepen(p, guh) && Wasritueel.stap(guh) == Wasritueel.INGEZEEPT, "1. soaped");
        helper.assertTrue(guh.position().distanceToSqr(Vec3.atBottomCenterOf(tobbe)) < 0.5, "it hopped into the tub");
        helper.assertTrue(level.getBlockState(tobbe).getValue(KnuffelbadBlocks.VULLING) == KnuffelbadBlocks.Vulling.WATER, "water in the tub");
        helper.assertTrue(Minigames.KNUFFELBAD.equals(Minigames.playing(p)), "washing is the Knuffelbad's game");
        GuhEntity tweede = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 2));
        tweede.tame(p);
        helper.assertTrue(!Wasritueel.inzepen(p, tweede) && Wasritueel.stap(tweede) == 0, "one guh per tub");
        Wasritueel.douche(p, tobbe);
        helper.assertTrue(Wasritueel.stap(guh) == Wasritueel.INGEZEEPT, "rinsing before the foam: no");
        for (int i = 0; i < Wasritueel.SCHROBBEN; i++) {
            Wasritueel.schrobben(p, guh);
        }
        helper.assertTrue(Wasritueel.stap(guh) == Wasritueel.GESCHUIMD, "2. all foam");
        helper.assertTrue(level.getBlockState(tobbe).getValue(KnuffelbadBlocks.VULLING) == KnuffelbadBlocks.Vulling.SCHUIM, "foam in the tub");
        for (int i = 0; i < Wasritueel.FOHN_TICKS; i++) {
            Wasritueel.fohn(p, guh);
        }
        helper.assertTrue(Wasritueel.stap(guh) == Wasritueel.GESCHUIMD, "no drying under the foam");
        Wasritueel.douche(p, tobbe);
        helper.assertTrue(Wasritueel.stap(guh) == Wasritueel.GESPOELD, "3. rinsed");
        for (int i = 0; i < Wasritueel.FOHN_TICKS; i++) {
            Wasritueel.fohn(p, guh);
        }
        helper.assertTrue(Wasritueel.stap(guh) == 0 && Wasritueel.glanst(guh), "4. dry: it shines!");
        helper.assertTrue(level.getBlockState(tobbe).getValue(KnuffelbadBlocks.VULLING) == KnuffelbadBlocks.Vulling.LEEG, "the tub is empty again");
        helper.assertTrue(aantal(p, KnuffelbadFeature.EENDJESMUNT.get()) == Wasritueel.WAS_MUNTEN, "an eendjesmunt for a clean guh");
        helper.assertTrue(KnusVoortgang.teller(p, KnuffelbadVoortgang.WASSEN) == 1 && advancement(p, "knuffelbad_gewassen"), "the Knus tab and the quest");
        helper.assertTrue(Minigames.playing(p) == null, "done washing");
        long tot = guh.getPersistentData().getLongOr(Wasritueel.GLANS_TOT, 0L);
        helper.assertTrue(tot - level.getGameTime() > Wasritueel.GLANS_DUUR - 5, "shiny for a whole day");
        // a day later: just a lovely guh again
        guh.getPersistentData().putLong(Wasritueel.GLANS_TOT, level.getGameTime() - 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(!Wasritueel.glanst(guh), "the shine is over after a day");
            weg(helper, p);
        });
    }

    // =================================================================================================================
    // Badmeester Bubbel
    // =================================================================================================================

    @GuhTest(template = EMPTY)
    public static void knuffelbadBadmeesterWinkelEnCadeautje(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(2, 1, 2));
        npc.setKind(GuhNpcEntity.Kind.BADMEESTERGUH);
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.BADMEESTERGUH) instanceof Badmeester, "his role");
        var offers = npc.getOffers();
        helper.assertTrue(offers.size() == 8, "his shop: " + offers.size());
        helper.assertTrue(offers.stream().allMatch(o -> o.getCostA().is(KnuffelbadFeature.EENDJESMUNT.get()) && o.getCostB().isEmpty()
                && o.getMaxUses() == Integer.MAX_VALUE), "for eendjesmunten only, never sold out");
        List<GuhClothes> kleding = offers.stream().filter(o -> o.getResult().getItem() instanceof nl.juiced.guhs.item.GuhClothingItem)
                .map(o -> ((nl.juiced.guhs.item.GuhClothingItem) o.getResult().getItem()).getClothes()).toList();
        helper.assertTrue(kleding.containsAll(List.of(GuhClothes.BADMUTSJE, GuhClothes.BADJASJE)), "the swim cap and the bathrobe: " + kleding);
        helper.assertTrue(GuhClothes.BADMUTSJE.slot == GuhClothes.Slot.HEAD && GuhClothes.BADJASJE.slot == GuhClothes.Slot.BODY, "a hat and a robe");
        ServerPlayer p = speler(helper, new BlockPos(2, 1, 4));
        Features.role(GuhNpcEntity.Kind.BADMEESTERGUH).talk(npc, p);
        helper.assertTrue(KnusVoortgang.teller(p, KnuffelbadVoortgang.BEZOCHT) == 1 && advancement(p, "knuffelbad_bezocht"), "found the Knuffelbad (Knus tab)");
        Badmeester.actie(npc, p, Badmeester.WASSEN);
        helper.assertTrue(aantal(p, KnuffelbadFeature.GUHSHAMPOO.get()) == 1 && aantal(p, KnuffelbadFeature.GUH_FOHN.get()) == 1, "a shampoo and a föhn, the first time");
        Badmeester.actie(npc, p, Badmeester.WASSEN);
        helper.assertTrue(aantal(p, KnuffelbadFeature.GUHSHAMPOO.get()) == 1, "only the first time");
        npc.tickCount = 100 - npc.getId() % 100;
        Badmeester.toonBord(npc);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, npc.getBoundingBox().inflate(1, 4, 1),
                d -> d.entityTags().contains(Scorebord.TAG)).isEmpty(), "the slides' top 3 floats above him");
        weg(helper, p);
        helper.succeed();
    }

    // =================================================================================================================
    // the building and the ducks
    // =================================================================================================================

    @GuhTest(template = TOBBE)
    public static void knuffelbadIsBeschermd(GameTestHelper helper) {
        KnuffelbadProtection.testGebied(helper.getLevel(), helper.getBounds());
        BlockPos tobbe = helper.absolutePos(new BlockPos(4, 2, 4));
        // (a mock server player always counts as creative: plain mock players for this one)
        net.minecraft.world.entity.player.Player wandelaar = helper.makeMockPlayer(GameType.SURVIVAL);
        net.minecraft.world.entity.player.Player bouwer = helper.makeMockPlayer(GameType.CREATIVE);
        helper.assertTrue(KnuffelbadProtection.geweigerd(wandelaar, tobbe), "no breaking the Knuffelbad");
        helper.assertTrue(!KnuffelbadProtection.geweigerd(bouwer, tobbe), "builders in creative may");
        helper.assertTrue(!KnuffelbadProtection.beschermd(helper.getLevel(), tobbe.offset(500, 0, 500)), "far away is not the Knuffelbad");
        helper.succeed();
    }

    /** A rubber duck of your own: it floats, squeaks, is saved (a ride's duck isn't). */
    @GuhTest(template = TOBBE, timeoutTicks = 100)
    public static void knuffelbadEigenBadeendje(GameTestHelper helper) {
        BadeendjeEntity deco = helper.spawn(KnuffelbadFeature.BADEENDJE.get(), new BlockPos(2, 2, 2));
        BadeendjeEntity rit = helper.spawn(KnuffelbadFeature.BADEENDJE.get(), new BlockPos(6, 3, 6));
        rit.setRit(java.util.UUID.randomUUID());
        helper.assertTrue(deco.shouldBeSaved() && !deco.vanRit(), "a duck of your own is kept");
        helper.assertTrue(!rit.shouldBeSaved() && rit.vanRit() && !rit.isPickable(), "a slide's duck isn't saved or hit");
        helper.succeedWhen(() -> helper.assertTrue(rit.isRemoved() && !deco.isRemoved(), "a slide's duck without its ride goes; yours stays"));
    }
}
