package nl.juiced.guhs.feature.bio.bouwdal;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.bio.wereld.BioPlekStructure;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the biomes3 slice "bouw-dal". The house tests run in the real template (weebhuisje: the test server
 * places it as the test's room, with its two characters and its balloon); the house is written down with the room's box,
 * and its own clock is moved forward ({@link WeebHuis#klokVooruit}) instead of waiting a game day.
 */
public class BioBouwDalGameTests {
    private static final String BATCH = "bio_bouw_dal", HUIS = "weebhuisje";

    static ServerPlayer speler(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        p.snapTo(at.x, at.y, at.z);
        return p;
    }

    static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    static WeebHuis huis(GameTestHelper helper) {
        AABB b = helper.getBounds();
        BoundingBox box = new BoundingBox((int) b.minX, (int) b.minY, (int) b.minZ, (int) b.maxX - 1, (int) b.maxY - 1, (int) b.maxZ - 1);
        return WeebHuizen.get(helper.getLevel().getServer()).registreer(helper.getLevel(), box);
    }

    static List<GuhNpcEntity> npcs(GameTestHelper helper, WeebHuis h, GuhNpcEntity.Kind kind) {
        return helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, h.zoek(), e -> e.getKind() == kind && e.isAlive());
    }

    static List<WeebBallonEntity> ballonnen(GameTestHelper helper, WeebHuis h) {
        return helper.getLevel().getEntitiesOfClass(WeebBallonEntity.class, h.zoek(), Entity::isAlive);
    }

    static boolean stand(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockState(pos).getValue(KleinBlok.Stand.AAN);
    }

    static boolean gehaald(ServerPlayer p, String naam) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id("quest/" + naam));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    static int reeksItems(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (Cadeaus.reeksIndex(p.getInventory().getItem(i)) >= 0) {
                n += p.getInventory().getItem(i).getCount();
            }
        }
        return n;
    }

    /** Everything a fresh house must have, and that the house found it. */
    static void heel(GameTestHelper helper, WeebHuis h, String wanneer) {
        helper.assertTrue(npcs(helper, h, GuhNpcEntity.Kind.WEEB_EVIVADS).size() == 1, wanneer + ": one Evivads, found " + npcs(helper, h, GuhNpcEntity.Kind.WEEB_EVIVADS).size());
        helper.assertTrue(npcs(helper, h, GuhNpcEntity.Kind.WEEB_NIELSVADS).size() == 1, wanneer + ": one Nielsvads, found " + npcs(helper, h, GuhNpcEntity.Kind.WEEB_NIELSVADS).size());
        helper.assertTrue(ballonnen(helper, h).size() == 1 && !ballonnen(helper, h).get(0).stijgt(), wanneer + ": one moored balloon, found " + ballonnen(helper, h).size());
        helper.assertTrue(h.bord != null && stand(helper, h.bord), wanneer + ": the loket sign says open");
        helper.assertTrue(h.briefje != null && !stand(helper, h.briefje), wanneer + ": no note on the door");
    }

    /**
     * The whole trip: talk, the scene, the walk to the balloon, up and away, the empty house with the note and the closed
     * loket (also after a restart-style save and load), back after a game day, the present, and off again.
     */
    @GuhTest(template = HUIS, batch = BATCH, timeoutTicks = 400)
    public static void bioBouwDalReisCyclus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WeebHuis[] h = {huis(helper)};
        WeebHuizen data = WeebHuizen.get(level.getServer());
        ServerPlayer p = speler(helper, 6.5, 3, 3.5), laat = speler(helper, 5.5, 3, 3.5);
        h[0].zorg(level);
        heel(helper, h[0], "fresh");
        helper.assertTrue(h[0].deur != null && h[0].steiger != null, "the house found its door and its mooring");
        GuhNpcEntity evi = npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_EVIVADS).get(0);
        Vec3 thuis = evi.position();
        NpcRollen.van(evi).talk(evi, p);
        helper.assertTrue(h[0].staat() == WeebHuis.Staat.GESPREK && h[0].variant == 0 && h[0].zwaaide(p.getUUID()), "talking starts the approved scene");
        helper.assertTrue(gehaald(p, "weeb_ontmoet"), "proof: met them");
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(h[0].stap >= 1, "the first line was said");
            h[0].klokVooruit(WeebHuis.GESPREK_TICKS + 5);
        });
        helper.runAtTickTime(25, () -> {
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.VERTREK && h[0].stap == 0, "after the scene they set off: " + h[0].staat());
            helper.assertTrue(level.getBlockState(h[0].deur).getValue(DoorBlock.OPEN), "the door is open while they walk out");
            helper.assertTrue(npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_EVIVADS).get(0).position().distanceTo(thuis) > 0.5, "Evivads walks");
            h[0] = data.herlaad(h[0]);          // (a restart in the middle of the walk)
            h[0].klokVooruit(300);
        });
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.VERTREK && h[0].stap == 1, "they stepped in: " + h[0].staat() + " " + h[0].stap);
            List<WeebBallonEntity> b = ballonnen(helper, h[0]);
            helper.assertTrue(b.size() == 1 && b.get(0).stijgt() && b.get(0).getPassengers().size() == 2, "both sit in the rising balloon");
            helper.assertTrue(!level.getBlockState(h[0].deur).getValue(DoorBlock.OPEN), "the door is shut again");
        });
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(ballonnen(helper, h[0]).get(0).getY() > h[0].ballonPlek().y + 1.0, "the balloon rises");
            h[0].klokVooruit(WeebBallonEntity.STIJG_TICKS);
        });
        helper.runAtTickTime(65, () -> {
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.WEG, "they are gone: " + h[0].staat());
            helper.assertTrue(npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_EVIVADS).isEmpty() && npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_NIELSVADS).isEmpty()
                    && ballonnen(helper, h[0]).isEmpty(), "the house is empty");
            helper.assertTrue(stand(helper, h[0].briefje) && !stand(helper, h[0].bord), "the note hangs on the door, the loket is closed");
            helper.assertTrue(gehaald(p, "weeb_eerste_reis") && !gehaald(laat, "weeb_eerste_reis"), "proof: sent them off (only who did)");
            helper.assertTrue(h[0].tegoed(p.getUUID()) == 0 && h[0].tegoed(laat.getUUID()) == 0, "nothing to claim while they are away");
            h[0] = data.herlaad(h[0]);          // (a restart while they are away)
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.WEG && h[0].zwaaide(p.getUUID()) && h[0].briefje != null, "the trip survives saving");
            h[0].klokVooruit(WeebHuis.REIS - 400);
        });
        helper.runAtTickTime(90, () -> {
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.WEG, "not back before the day is over");
            h[0].klokVooruit(500);
        });
        helper.runAtTickTime(115, () -> {
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.THUIS && h[0].reizen() == 1, "back after a game day: " + h[0].staat());
            heel(helper, h[0], "back");
            GuhNpcEntity terug = npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_EVIVADS).get(0);
            helper.assertTrue(terug.position().distanceTo(thuis) < 0.3, "Evivads stands where she stood");
            helper.assertTrue(h[0].tegoed(p.getUUID()) == 1 && h[0].tegoed(laat.getUUID()) == 0, "a present waits for who sent them off, for nobody else");
            GuhNpcEntity niels = npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_NIELSVADS).get(0);
            NpcRollen.van(niels).talk(niels, p);
            helper.assertTrue(h[0].tegoed(p.getUUID()) == 0 && Cadeaus.aantal(p) == 1 && reeksItems(p) == 1 && gehaald(p, "weeb_cadeau"),
                    "the first present is a piece of the verzamelreeks");
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.THUIS, "taking the present does not send them off");
            // the one who came too late: no present, but talking starts the next trip
            NpcRollen.van(niels).talk(niels, laat);
            helper.assertTrue(Cadeaus.aantal(laat) == 0 && reeksItems(laat) == 0, "who was not there gets nothing");
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.GESPREK && h[0].variant != 0 && h[0].zwaaide(laat.getUUID()), "and off they go again, with another scene");
            data.vergeet(h[0]);
            weg(helper, p, laat);
            helper.succeed();
        });
    }

    /** Two players share one trip; one of them is offline when the two come back and still has the present waiting. */
    @GuhTest(template = HUIS, batch = BATCH, timeoutTicks = 300)
    public static void bioBouwDalTweeSpelers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WeebHuis[] h = {huis(helper)};
        WeebHuizen data = WeebHuizen.get(level.getServer());
        ServerPlayer een = speler(helper, 6.5, 3, 3.5), twee = speler(helper, 5.5, 3, 3.5), drie = speler(helper, 7.5, 3, 3.5);
        UUID idTwee = twee.getUUID();
        GuhNpcEntity evi = npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_EVIVADS).get(0), niels = npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_NIELSVADS).get(0);
        NpcRollen.van(evi).talk(evi, een);
        helper.runAtTickTime(4, () -> {
            NpcRollen.van(niels).talk(niels, twee);            // during the scene: she is written down too, nothing restarts
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.GESPREK && h[0].zwaaide(idTwee) && h[0].zwaaiers.size() == 2, "a second player joins the send-off");
            h[0].klokVooruit(WeebHuis.GESPREK_TICKS + WeebHuis.VERTREK_MAX + 10);
        });
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.WEG, "a scene that was missed (the chunk was not loaded) ends with them gone: " + h[0].staat());
            helper.assertTrue(npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_EVIVADS).isEmpty() && ballonnen(helper, h[0]).isEmpty(), "nobody left behind");
            weg(helper, twee);                                  // she logs off
            h[0].klokVooruit(WeebHuis.REIS + 10);
        });
        helper.runAtTickTime(35, () -> {
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.THUIS, "back: " + h[0].staat());
            helper.assertTrue(h[0].tegoed(een.getUUID()) == 1 && h[0].tegoed(idTwee) == 1 && h[0].tegoed(drie.getUUID()) == 0,
                    "a present for both who sent them off, the offline one included");
            h[0] = data.herlaad(h[0]);
            helper.assertTrue(h[0].tegoed(idTwee) == 1, "the offline player's present survives saving");
            // a second trip without her claiming: it adds up, nothing is lost
            GuhNpcEntity e2 = npcs(helper, h[0], GuhNpcEntity.Kind.WEEB_EVIVADS).get(0);
            NpcRollen.van(e2).talk(e2, drie);
            helper.assertTrue(h[0].staat() == WeebHuis.Staat.GESPREK && h[0].tegoed(een.getUUID()) == 1, "another player's trip leaves waiting presents alone");
            data.vergeet(h[0]);
            weg(helper, een, drie);
            helper.succeed();
        });
    }

    /** The repair: a lost character is made again, a double goes, a strayed one and the balloon come back, sign and note are set right. */
    @GuhTest(template = HUIS, batch = BATCH, timeoutTicks = 100)
    public static void bioBouwDalHerstel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WeebHuis h = huis(helper);
        helper.assertTrue(h.zorg(level) == 0, "a fresh house needs no repair");
        GuhNpcEntity evi = npcs(helper, h, GuhNpcEntity.Kind.WEEB_EVIVADS).get(0), niels = npcs(helper, h, GuhNpcEntity.Kind.WEEB_NIELSVADS).get(0);
        Vec3 thuisN = niels.position();
        evi.discard();                                                                  // lost
        GuhNpcEntity dubbel = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(6, 3, 3));
        dubbel.setKind(GuhNpcEntity.Kind.WEEB_NIELSVADS);                               // a double, outside
        niels.snapTo(thuisN.x + 2, thuisN.y, thuisN.z - 2, 0, 0);                       // strayed
        ballonnen(helper, h).get(0).setPos(ballonnen(helper, h).get(0).position().add(0, 6, 0));
        level.setBlock(h.briefje, level.getBlockState(h.briefje).setValue(KleinBlok.Stand.AAN, true), Block.UPDATE_ALL);
        level.setBlock(h.bord, level.getBlockState(h.bord).setValue(KleinBlok.Stand.AAN, false), Block.UPDATE_ALL);
        helper.runAtTickTime(2, () -> {
            int n = h.zorg(level);
            helper.assertTrue(n >= 5, "the repair put " + n + " things right");
            heel(helper, h, "repaired");
            GuhNpcEntity over = npcs(helper, h, GuhNpcEntity.Kind.WEEB_NIELSVADS).get(0);
            helper.assertTrue(over.position().distanceTo(thuisN) < 0.3, "Nielsvads is back on his spot");
            helper.assertTrue(ballonnen(helper, h).get(0).position().distanceTo(h.ballonPlek()) < 0.3, "the balloon is back on its mooring");
            helper.assertTrue(h.zorg(level) == 0, "and then there is nothing left to repair");
            // while they are away a stray copy must not stay
            h.begin(level, UUID.randomUUID());
            h.klokVooruit(WeebHuis.GESPREK_TICKS + WeebHuis.VERTREK_MAX + 10);
        });
        helper.runAtTickTime(6, () -> {
            helper.assertTrue(h.staat() == WeebHuis.Staat.WEG, "gone");
            GuhNpcEntity zwerver = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(5, 3, 8));
            zwerver.setKind(GuhNpcEntity.Kind.WEEB_EVIVADS);
        });
        helper.runAtTickTime(8, () -> {
            helper.assertTrue(h.zorg(level) == 1 && npcs(helper, h, GuhNpcEntity.Kind.WEEB_EVIVADS).isEmpty(), "a copy that turns up while they are away is removed");
            WeebHuizen.get(level.getServer()).vergeet(h);
            helper.succeed();
        });
    }

    /** The balloon: carries the two, rises, and is gone with them at the end. */
    @GuhTest(template = "weeb_test_vloer", batch = BATCH, timeoutTicks = WeebBallonEntity.STIJG_TICKS + 60)
    public static void bioBouwDalBallon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WeebBallonEntity b = helper.spawn(BouwDalSlice.WEEB_BALLON.get(), new BlockPos(4, 2, 4));
        GuhNpcEntity e = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(3, 2, 4)), n = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(5, 2, 4));
        e.setKind(GuhNpcEntity.Kind.WEEB_EVIVADS);
        n.setKind(GuhNpcEntity.Kind.WEEB_NIELSVADS);
        ServerPlayer p = speler(helper, 1.5, 2, 1.5);
        double y0 = b.getY();
        helper.assertTrue(!p.startRiding(b, false, true), "a player cannot ride it");
        helper.assertTrue(e.startRiding(b, true, true) && n.startRiding(b, true, true), "the two step in");
        helper.runAtTickTime(20, () -> helper.assertTrue(Math.abs(b.getY() - y0) < 0.01, "moored: it stays where it is"));
        helper.runAtTickTime(21, b::stijgOp);
        helper.runAtTickTime(80, () -> helper.assertTrue(b.getY() > y0 + 3 && e.getVehicle() == b && e.getY() > y0 + 3, "it rises, with them in it"));
        helper.succeedWhen(() -> {
            helper.assertTrue(b.isRemoved() && e.isRemoved() && n.isRemoved(), "gone, with both");
            weg(helper, p);
        });
    }

    /** The present pools, the rule that makes a collection complete in reasonable time, and the trade of doubles. */
    @GuhTest(template = "weeb_test_vloer", batch = BATCH)
    public static void bioBouwDalCadeaus(GameTestHelper helper) {
        RandomSource r = RandomSource.create(20261008L);
        helper.assertTrue(Cadeaus.kies(0, 0, 0, r).pool() == Cadeaus.Pool.REEKS, "the first present is a piece of the reeks");
        long totaal = 0;
        int spelers = 3000, langste = 0;
        for (int s = 0; s < spelers; s++) {
            int reeks = 0, outfits = 0, n = 0;
            while ((reeks & Cadeaus.REEKS_VOL) != Cadeaus.REEKS_VOL || (outfits & Cadeaus.OUTFITS_VOL) != Cadeaus.OUTFITS_VOL) {
                Cadeaus.Cadeau c = Cadeaus.kies(reeks, outfits, n, r);
                n++;
                if (c.pool() == Cadeaus.Pool.OUTFIT) {
                    helper.assertTrue((outfits & 1 << c.index()) == 0, "never an outfit the player has");
                    outfits |= 1 << c.index();
                } else if (c.pool() == Cadeaus.Pool.REEKS) {
                    // a double is traded for a missing piece: one step either way
                    reeks |= 1 << ((reeks & 1 << c.index()) == 0 ? c.index() : Cadeaus.mist(reeks, Cadeaus.REEKS.length, r));
                }
                helper.assertTrue(n < 200, "a collection that never completes");
            }
            totaal += n;
            langste = Math.max(langste, n);
        }
        double gemiddeld = totaal / (double) spelers;
        com.mojang.logging.LogUtils.getLogger().info("[bio_bouw_dal] trips for the full reeks and all outfits: mean {} over {} players, longest {}",
                String.format("%.1f", gemiddeld), spelers, langste);
        helper.assertTrue(gemiddeld > 16 && gemiddeld < 21 && langste < 45, "about 19 trips for everything: mean " + gemiddeld + ", longest " + langste);
        Cadeaus.Cadeau na = Cadeaus.kies(Cadeaus.REEKS_VOL, Cadeaus.OUTFITS_VOL, 30, r);
        helper.assertTrue(na.pool() != Cadeaus.Pool.OUTFIT, "with everything complete: food or a double to give away");
        for (int i = 0; i < 3; i++) {
            ItemStack st = Cadeaus.stapel(new Cadeaus.Cadeau(Cadeaus.Pool.values()[i], 0));
            helper.assertTrue(!st.isEmpty() && st.getItem() != Items.AIR, "a present of pool " + Cadeaus.Pool.values()[i] + " is a real item");
        }
        // the trade
        ServerPlayer p = speler(helper, 4.5, 2, 4.5);
        Item lampion = Cadeaus.item("japan_lampion");
        p.getInventory().add(new ItemStack(lampion, 1));
        p.getInventory().setSelectedSlot(0);
        helper.assertTrue(Cadeaus.ruil(p, p.getMainHandItem(), r).soort() == Cadeaus.Ruil.ENIGE, "one copy is no double");
        helper.assertTrue(Cadeaus.ruil(p, new ItemStack(Items.STICK), r).soort() == Cadeaus.Ruil.GEEN, "a stick is no piece of the reeks");
        p.getMainHandItem().grow(1);
        Cadeaus.Uitkomst u = Cadeaus.ruil(p, p.getMainHandItem(), r);
        helper.assertTrue(u.soort() == Cadeaus.Ruil.GERUILD && u.gekregen() != 1, "a double is traded for another piece");
        helper.assertTrue(p.getInventory().countItem(lampion) == 1 && reeksItems(p) == 2 && Integer.bitCount(Cadeaus.reeks(p)) == 2, "one lampion left, one new piece, two known");
        helper.assertTrue(gehaald(p, Cadeaus.REEKS[u.gekregen()]), "proof of the new piece");
        GuhQuests.saved(p).putInt(Cadeaus.REEKS_KEY, Cadeaus.REEKS_VOL);
        p.getMainHandItem().grow(1);
        helper.assertTrue(Cadeaus.ruil(p, p.getMainHandItem(), r).soort() == Cadeaus.Ruil.COMPLEET && p.getInventory().countItem(lampion) == 2, "a full reeks trades nothing");
        // a real hand-over, twelve times: every piece arrives and the whole reeks is proven
        GuhQuests.saved(p).putInt(Cadeaus.REEKS_KEY, 0);
        for (int i = 0; i < Cadeaus.REEKS.length; i++) {
            Cadeaus.gekregen(p, i);
        }
        helper.assertTrue(gehaald(p, "japan_reeks_compleet") && gehaald(p, "japan_windgong"), "proof: every piece, and the complete reeks");
        GuhQuests.saved(p).putInt(Cadeaus.OUTFIT_KEY, 0);
        for (int i = 0; i < 40 && !gehaald(p, "japan_outfits_compleet"); i++) {
            Cadeaus.geef(p, r);
        }
        helper.assertTrue(gehaald(p, "japan_outfits_compleet"), "with the reeks complete the outfits follow quickly");
        weg(helper, p);
        helper.succeed();
    }

    /** Every block of the verzamelreeks can be placed and drops itself; the things of the house have no item and do not break. */
    @GuhTest(template = "weeb_test_vloer", batch = BATCH)
    public static void bioBouwDalBlokken(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(BouwDalSlice.REEKS.size() == 12 && List.copyOf(BouwDalSlice.REEKS.keySet()).equals(List.of(Cadeaus.REEKS)), "twelve pieces, in the order of the presents");
        int i = 0;
        for (var e : BouwDalSlice.REEKS.entrySet()) {
            BlockPos pos = new BlockPos(i % 6 + 1, 2, i / 6 * 2 + 1);
            i++;
            Block blok = e.getValue().get();
            Item item = Cadeaus.item(e.getKey());
            helper.assertTrue(item instanceof BlockItem bi && bi.getBlock() == blok, e.getKey() + " has its item");
            helper.setBlock(pos, blok.defaultBlockState().setValue(KleinBlok.FACING, net.minecraft.core.Direction.EAST));
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(state.is(blok) && !state.getShape(level, helper.absolutePos(pos)).isEmpty(), e.getKey() + " stands");
            List<ItemStack> drops = Block.getDrops(state, level, helper.absolutePos(pos), null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(item) && drops.get(0).getCount() == 1, e.getKey() + " drops itself: " + drops);
            helper.assertTrue(state.getDestroySpeed(level, helper.absolutePos(pos)) >= 0, e.getKey() + " can be broken");
        }
        for (DeferredBlock<?> vast : List.of(BouwDalSlice.FIGUURTJES, BouwDalSlice.POSTER, BouwDalSlice.MANGASTAPEL, BouwDalSlice.DAKIMAKURA, BouwDalSlice.LOKET,
                BouwDalSlice.NUMMERAUTOMAAT, BouwDalSlice.LOKETBORD, BouwDalSlice.BRIEFJE)) {
            String id = vast.getId().getPath();
            helper.assertTrue(BuiltInRegistries.ITEM.getValue(vast.getId()) == Items.AIR, id + " is no item (not for the player's base)");
            BlockState state = vast.get().defaultBlockState();
            helper.assertTrue(state.getDestroySpeed(level, helper.absolutePos(BlockPos.ZERO)) < 0, id + " cannot be broken");
            helper.assertTrue(Block.getDrops(state, level, helper.absolutePos(BlockPos.ZERO), null).isEmpty(), id + " drops nothing");
        }
        BlockState brief = BouwDalSlice.BRIEFJE.get().defaultBlockState();
        BlockPos nul = helper.absolutePos(BlockPos.ZERO);
        helper.assertTrue(brief.getShape(level, nul, CollisionContext.empty()).isEmpty() && !brief.setValue(KleinBlok.Stand.AAN, true).getShape(level, nul).isEmpty(),
                "the note is only there while it hangs");
        helper.succeed();
    }

    /** The four foods: a player eats them, and so does the player's guh. */
    @GuhTest(template = "weeb_test_vloer", batch = BATCH)
    public static void bioBouwDalEten(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 4.5, 2, 4.5);
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 5));
        guh.tame(p);
        helper.assertTrue(BouwDalSlice.ETEN.size() == 4 && List.copyOf(BouwDalSlice.ETEN.keySet()).equals(List.of(Cadeaus.ETEN)), "four foods, as the presents name them");
        for (var e : BouwDalSlice.ETEN.entrySet()) {
            ItemStack stapel = new ItemStack(e.getValue().get(), 2);
            helper.assertTrue(stapel.get(DataComponents.FOOD) != null, e.getKey() + " is food");
            helper.assertTrue(stapel.is(BandFeature.SNACKS), e.getKey() + " is a guh snack");
            p.getFoodData().setFoodLevel(4);
            ItemStack rest = stapel.finishUsingItem(helper.getLevel(), p);
            helper.assertTrue(p.getFoodData().getFoodLevel() > 4, e.getKey() + " feeds the player");
            p.getInventory().clearContent();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(e.getValue().get(), 2));
            helper.assertTrue(guh.heeftEigenKlik(p.getMainHandItem(), p), e.getKey() + ": the click goes to the guh");
            guh.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(p.getMainHandItem().getCount() == 1, e.getKey() + ": the guh ate one (left " + p.getMainHandItem().getCount() + ", rest " + rest + ")");
        }
        guh.discard();
        weg(helper, p);
        helper.succeed();
    }

    /** The four outfits: registered, with their source, their pictures, and bones the guh model really has. */
    @GuhTest(template = "weeb_test_vloer", batch = BATCH)
    public static void bioBouwDalKleding(GameTestHelper helper) throws Exception {
        String geo;
        try (var in = BioBouwDalGameTests.class.getResourceAsStream("/assets/guhs/geckolib/models/entity/guh.geo.json")) {
            helper.assertTrue(in != null, "the guh model is there");
            geo = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        GuhClothes.Slot[] slots = {GuhClothes.Slot.BODY, GuhClothes.Slot.HEAD, GuhClothes.Slot.OREN, GuhClothes.Slot.NECK};
        for (int i = 0; i < Cadeaus.OUTFITS.length; i++) {
            GuhClothes c = Cadeaus.OUTFITS[i];
            helper.assertTrue(c.slot == slots[i], c + " sits in slot " + slots[i]);
            helper.assertTrue(BouwDalSlice.KLEDINGBRON.equals(KledingBronnen.bron(c)), c + " comes from the weebhuisje");
            helper.assertTrue(BioBouwDalGameTests.class.getResource("/assets/guhs/textures/entity/guh_clothes/" + c.id() + ".png") != null, c + " has its texture");
            helper.assertTrue(BioBouwDalGameTests.class.getResource("/assets/guhs/textures/item/" + c.id() + ".png") != null, c + " has its icon");
            for (String bot : c.bones) {
                helper.assertTrue(geo.contains("\"" + bot), c + ": the guh has the bone " + bot);
            }
            helper.assertTrue(!Cadeaus.stapel(new Cadeaus.Cadeau(Cadeaus.Pool.OUTFIT, i)).isEmpty(), c + " is an item");
        }
        helper.succeed();
    }

    /** The structures are there, of the right type, and the weebhuisje can be found with the Superkompas. */
    @GuhTest(template = "weeb_test_vloer", batch = BATCH)
    public static void bioBouwDalStructuren(GameTestHelper helper) {
        var register = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var sets = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        for (String id : BouwDalCheck.ANKER.keySet()) {
            var s = register.getValue(Guhs.id(id));
            helper.assertTrue(s instanceof BioPlekStructure, id + " is a guhs:bio_plek structure");
            helper.assertTrue(sets.getValue(Guhs.id(id)) != null, id + " has its structure set");
            helper.assertTrue(sets.getValue(Guhs.id(id + "_gegarandeerd")) == null, id + " has no guaranteed copy");
        }
        helper.assertTrue(register.getValue(ResourceKey.create(Registries.STRUCTURE, Guhs.id("weebhuisje"))) != null, "weebhuisje");
        boolean knus = SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals("knus") && c.structures().contains("weebhuisje"));
        boolean mini = SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.structures().stream().anyMatch(x -> x.startsWith("dal_")));
        helper.assertTrue(knus && !mini, "the weebhuisje is in the Superkompas tab knus, the mini structures are in none");
        helper.assertTrue(NpcRollen.rol(GuhNpcEntity.Kind.WEEB_EVIVADS) != null && GuhNpcEntity.Kind.WEEB_EVIVADS.scale < GuhNpcEntity.Kind.WEEB_NIELSVADS.scale
                && GuhNpcEntity.Kind.WEEB_EVIVADS.scale > 1.05f, "Evivads is tall, Nielsvads a little taller");
        helper.succeed();
    }
}
