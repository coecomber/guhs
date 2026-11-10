package nl.juiced.guhs.feature.vadsrijk.vitrine;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huizen.Kwartier;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoortConfig;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Y-VITRINE: the Vadsrijk-vitrine. Placement only outside Het Vadsrijk; the snapshot follows the owner's progress; offline the last
 * snapshot stays (and survives a save/load); with only zone 1 open nothing of zone 2+ is named. Run:
 * {@code runGameTestServer -Pgt=VadsrijkVitrine}.
 */
public class VadsrijkVitrineGameTests {
    private static final String BATCH = "vadsrijk_vitrine";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        Questlog.wisAlles(p);
        return p;
    }

    /** Places a vitrine by hand (the item, like a player does) at a test-relative spot; returns the result. */
    private static InteractionResult plaats(ServerPlayer p, ServerLevel level, BlockPos abs) {
        ItemStack st = new ItemStack(Vitrine.ITEM.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, st);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        return Vitrine.ITEM.get().place(new BlockPlaceContext(level, p, InteractionHand.MAIN_HAND, st, hit));
    }

    private static VitrineBlockEntity neer(GameTestHelper helper, ServerPlayer p) {
        BlockPos abs = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(abs, Blocks.AIR.defaultBlockState());
        InteractionResult r = plaats(p, helper.getLevel(), abs);
        helper.assertTrue(r.consumesAction(), "the vitrine can be placed in the overworld: " + r);
        BlockEntity be = helper.getLevel().getBlockEntity(abs);
        helper.assertTrue(be instanceof VitrineBlockEntity, "a vitrine block entity stands there");
        return (VitrineBlockEntity) be;
    }

    static boolean heeftKey(Component c, String key) {
        if (c.getContents() instanceof TranslatableContents t) {
            if (t.getKey().equals(key)) {
                return true;
            }
            for (Object a : t.getArgs()) {
                if (a instanceof Component ac && heeftKey(ac, key)) {
                    return true;
                }
            }
        }
        for (Component s : c.getSiblings()) {
            if (heeftKey(s, key)) {
                return true;
            }
        }
        return false;
    }

    static boolean ergens(List<Component> l, String key) {
        return l.stream().anyMatch(c -> heeftKey(c, key));
    }

    /** Placeable in the overworld (the placer is the owner); refused in Het Vadsrijk and Het Knuffelkwartier. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void alleenBuitenHetVadsrijk(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        VitrineBlockEntity be = neer(helper, p);
        helper.assertTrue(p.getUUID().equals(be.eigenaar()), "the placer is the owner");
        helper.assertTrue("test-mock-player".equals(be.foto().getStringOr("naam", "")), "the first snapshot right away: " + be.foto());
        helper.assertTrue(Vitrine.magHier(helper.getLevel()), "overworld: allowed");
        ServerLevel rijk = Vadsrijk.level(helper.getLevel().getServer());
        helper.assertTrue(rijk != null, "the Vadsrijk dimension exists in the test server");
        helper.assertTrue(!Vitrine.magHier(rijk), "Het Vadsrijk: not allowed");
        ServerLevel kwartier = helper.getLevel().getServer().getLevel(Kwartier.DIM);
        if (kwartier != null) {
            helper.assertTrue(!Vitrine.magHier(kwartier), "Het Knuffelkwartier: not allowed");
        }
        // really try it in the Vadsrijk, as a player would
        BlockPos daar = new BlockPos(0, 250, 0);
        InteractionResult r = plaats(p, rijk, daar);
        helper.assertTrue(!r.consumesAction(), "placing in Het Vadsrijk is refused: " + r);
        helper.assertTrue(!rijk.getBlockState(daar).is(Vitrine.BLOK.get()), "no vitrine block in Het Vadsrijk");
        Questlog.wisAlles(p);
        helper.succeed();
    }

    /** The snapshot follows the owner's progress: level, class, zones, mount statue, raid clears, the first-zone date. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void fotoVolgtVoortgang(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            ZonePoortConfig.TEST = 6;
            VitrineBlockEntity be = neer(helper, p);
            helper.assertTrue(be.foto().getIntOr("zones", -1) == 0 && be.foto().getLongOr("eerste", -1L) == 0L, "a fresh hero: no zones, no date");
            Spelers.zetLevel(p, 20);
            int n = Vitrine.maakZoneKlaar(p, 1);
            helper.assertTrue(n > 0 && Vitrine.zoneKlaar(p, 1) && !Vitrine.zoneKlaar(p, 2), "zone 1 main story done (" + n + " quests)");
            Rijdieren.geef(p, RijdierSoort.KNUFFELHERT);
            Rijdieren.kies(p, RijdierSoort.KNUFFELHERT);
            Spelers.data(p).extra("z6raid").putInt("clears", 2);
            helper.assertTrue(be.ververs(false), "the online owner's progress changes the snapshot");
            CompoundTag f = be.foto();
            helper.assertTrue(f.getIntOr("level", 0) == 20, "level 20: " + f);
            helper.assertTrue(f.getIntOr("zones", 0) == 1, "zone badge 1 lit: " + f);
            helper.assertTrue(f.getIntOr("raid", 0) == 2, "raid clears 2: " + f);
            helper.assertTrue(f.getLongOr("eerste", 0L) > 0, "the first-zone date is set: " + f);
            helper.assertTrue("r".equals(f.getStringOr("beeld", "")) && "knuffelhert".equals(f.getStringOr("soort", "")), "statue = the active mount: " + f);
            long eerste = f.getLongOr("eerste", 0L);
            Vitrine.maakZoneKlaar(p, 2);
            be.ververs(false);
            helper.assertTrue(be.foto().getIntOr("zones", 0) == 3, "zone 2 too: " + be.foto());
            helper.assertTrue(be.foto().getLongOr("eerste", 0L) == eerste, "the date of the FIRST zone does not move");
            helper.assertTrue(!be.ververs(false), "nothing changed: nothing re-sent");
            // the summary at gate 6 names the zones and shows the raid
            List<Component> chat = Vitrine.samenvatting(be.foto(), true, 6);
            helper.assertTrue(ergens(chat, "vadsrijk.zonepoort.zone.2") && ergens(chat, Vitrine.K + "raid"), "gate 6: zone names + raid in the summary");
        } finally {
            ZonePoortConfig.TEST = null;
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }

    /** Offline: the last snapshot stays (and is saved with the block); back online it catches up. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void offlineCache(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            ZonePoortConfig.TEST = 6;
            VitrineBlockEntity be = neer(helper, p);
            Spelers.zetLevel(p, 12);
            be.ververs(false);
            helper.assertTrue(be.foto().getIntOr("level", 0) == 12, "online: level 12");
            Vitrine.TEST_OFFLINE.add(p.getUUID());
            Spelers.zetLevel(p, 18);
            Vitrine.maakZoneKlaar(p, 1);
            helper.assertTrue(!be.ververs(false), "offline: the vitrine does not change");
            helper.assertTrue(be.foto().getIntOr("level", 0) == 12 && be.foto().getIntOr("zones", 0) == 0, "offline: still the old snapshot");
            List<Component> chat = Vitrine.samenvatting(be.foto(), false, 6);
            helper.assertTrue(ergens(chat, Vitrine.K + "chat.offline"), "the click summary says it is the last known state");
            // saved with the chunk: a fresh block entity loaded from the save has the same snapshot and owner
            CompoundTag opgeslagen = be.saveWithFullMetadata(helper.getLevel().registryAccess());
            BlockEntity terug = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), opgeslagen, helper.getLevel().registryAccess());
            helper.assertTrue(terug instanceof VitrineBlockEntity v && v.foto().equals(be.foto()) && p.getUUID().equals(v.eigenaar()),
                    "the snapshot survives save/load");
            Vitrine.TEST_OFFLINE.remove(p.getUUID());
            helper.assertTrue(be.ververs(false) && be.foto().getIntOr("level", 0) == 18 && be.foto().getIntOr("zones", 0) == 1, "back online: caught up");
        } finally {
            Vitrine.TEST_OFFLINE.remove(p.getUUID());
            ZonePoortConfig.TEST = null;
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }

    /** Only zone 1 open: zones 2-6 are "?" on the panel and in the summary, never named; no raid line; raising the gate re-syncs. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void poortEenGeenSpoilers(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            ZonePoortConfig.TEST = 1;
            VitrineBlockEntity be = neer(helper, p);
            helper.assertTrue(be.open() == 1, "the vitrine carries gate 1");
            // even an op who did everything: the closed zones stay nameless
            for (int z = 1; z <= 6; z++) {
                Vitrine.maakZoneKlaar(p, z);
            }
            Spelers.data(p).extra("z6raid").putInt("clears", 5);
            be.ververs(false);
            List<Component> chat = Vitrine.samenvatting(be.foto(), true, be.open());
            List<Component> bord = Vitrine.bordRegels(be.foto(), be.open());
            for (int z = 2; z <= 6; z++) {
                String naam = "vadsrijk.zonepoort.zone." + z;
                helper.assertTrue(!ergens(chat, naam) && !ergens(bord, naam), "gate 1: zone " + z + " is not named");
            }
            helper.assertTrue(ergens(chat, "vadsrijk.zonepoort.zone.1"), "zone 1 is named");
            helper.assertTrue(ergens(chat, Vitrine.K + "chat.zone.dicht"), "closed zones: the '?' line");
            helper.assertTrue(!ergens(chat, Vitrine.K + "raid") && !ergens(bord, Vitrine.K + "raid"), "gate 1: no raid line");
            String badges = Vitrine.badges(be.foto().getIntOr("zones", 0), 1).getString();
            helper.assertTrue(badges.chars().filter(c -> c == '?').count() == 5, "five '?' badges: " + badges);
            // the server raises the gate: the vitrine notices on its next refresh, even with the owner offline
            Vitrine.TEST_OFFLINE.add(p.getUUID());
            ZonePoortConfig.TEST = 6;
            helper.assertTrue(be.ververs(false) && be.open() == 6, "gate 6: re-synced");
            helper.assertTrue(ZonePoort.open() == 6 && ergens(Vitrine.bordRegels(be.foto(), be.open()), Vitrine.K + "raid"), "gate 6: raid line");
        } finally {
            Vitrine.TEST_OFFLINE.remove(p.getUUID());
            ZonePoortConfig.TEST = null;
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }
}
