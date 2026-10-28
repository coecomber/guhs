package nl.juiced.guhs.feature.dromen;

import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.blub.BlubSnoes;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of Guhdromenland part A (batch "dromen"). The game test server has no datapack dimensions, so these test what
 * works without the dream itself: the start island per item, the story API, tucking a guh in (and out) on a doos met
 * knabbels, the island layout and the island json, and the building rule outside the dream.
 */
public class DroomGameTests {
    private static final String TUIN = "landdiertjes_test_tuin", BATCH = "dromen";

    @SuppressWarnings("removal")
    static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        DroomVoortgang.reset(p);
        return p;
    }

    /** What you give along picks the island; nothing (or anything else) is the plein. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void droomStartPerItem(GameTestHelper helper) {
        helper.assertTrue(DroomStart.eilandVoor(ItemStack.EMPTY).equals(DroomEilanden.PLEIN), "nothing: the plein");
        helper.assertTrue(DroomStart.eilandVoor(new ItemStack(Items.DIRT)).equals(DroomEilanden.PLEIN), "dirt: the plein");
        String[][] verwacht = {{"guhs:knuffelbad_badeendje", DroomEilanden.BADJE}, {"guhs:sterrenlantaarn", DroomEilanden.STERREN},
                {"guhs:roze_guh_koek", DroomEilanden.HERINNERING}, {"guhs:lampion_mint", DroomEilanden.MOLENTJE},
                {"guhs:knuffel_normal", DroomEilanden.LIEDJES}, {"guhs:pink_kussen", DroomEilanden.LIEDJES}};
        for (String[] v : verwacht) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(v[0]));
            helper.assertTrue(item != Items.AIR, "item exists: " + v[0]);
            helper.assertTrue(DroomStart.eilandVoor(new ItemStack(item)).equals(v[1]), v[0] + " -> " + v[1]);
        }
        helper.succeed();
    }

    /** Five pieces, the finale, and the finale helps Snoes (the Blub flag); reset forgets it all. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void voortgangEnSnoes(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        int[] gehoord = {0};
        DroomVoortgang.luister((sp, wat) -> {
            if (sp == p) {
                gehoord[0]++;
            }
        });
        helper.assertFalse(DroomVoortgang.alleStukjes(p) || BlubSnoes.heeft(p), "starts empty");
        for (DroomVoortgang.Stukje s : DroomVoortgang.Stukje.values()) {
            helper.assertTrue(DroomVoortgang.geef(p, s), "new piece " + s.id());
        }
        helper.assertFalse(DroomVoortgang.geef(p, DroomVoortgang.Stukje.STER), "a piece only once");
        helper.assertTrue(DroomVoortgang.alleStukjes(p) && !DroomVoortgang.finaleKlaar(p), "all five, no finale yet");
        DroomVoortgang.zetFinale(p);
        helper.assertTrue(DroomVoortgang.finaleKlaar(p) && BlubSnoes.heeft(p), "the finale helps Snoes: wild Blubs open up");
        helper.assertTrue(gehoord[0] >= 7, "listeners heard the pieces, the finale and snoes: " + gehoord[0]);
        DroomVoortgang.reset(p);
        helper.assertFalse(DroomVoortgang.aantal(p) > 0 || BlubSnoes.heeft(p), "reset forgets everything");
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /** Tucking your own guh in on a doos met knabbels: it lies in it asleep, the knuffel goes along (liedjes); out again. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void instoppenInDeDoos(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        BlockPos doos = new BlockPos(6, 2, 6);
        helper.setBlock(doos, DromenFeature.DOOS_KNABBELS.get());
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(9, 2, 9));
        guh.tame(p);
        BlockPos abs = helper.absolutePos(doos);
        p.snapTo(abs.getX() + 2.5, abs.getY(), abs.getZ() + 0.5);
        boolean voor = DroomSlapen.altijd;
        DroomSlapen.altijd = true;
        try {
            var knuffel = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("guhs:knuffel_normal"));
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(knuffel, 3));
            helper.assertTrue(DroomSlapen.instoppen(p, abs), "tucked in");
            helper.assertTrue(DroomSlapen.isIngestopt(guh) && DroomSlapen.ingestopt(p) == guh, "the player's guh is tucked in");
            helper.assertTrue(guh.position().distanceToSqr(Vec3.atBottomCenterOf(abs)) < 0.3, "it lies in the box");
            helper.assertTrue(p.getMainHandItem().getCount() == 2 && DroomReis.mee(p).is(knuffel), "one knuffel went along");
            helper.assertTrue(DroomStart.eilandVoor(DroomReis.mee(p)).equals(DroomEilanden.LIEDJES), "a knuffel dreams of the Slaapliedjesheuvels");
            ServerPlayer ander = speler(helper);
            helper.assertTrue(DroomSlapen.instoppen(ander, abs) && DroomSlapen.ingestopt(ander) == null, "the box is taken (a message, nothing else)");
            DroomSlapen.wekGuh(p, false);
            DroomReis.geefMeeTerug(p);
            helper.assertFalse(DroomSlapen.isIngestopt(guh), "awake again");
            helper.assertTrue(p.getInventory().countItem(knuffel) == 3, "the knuffel came back");
            helper.getLevel().removePlayerImmediately(ander, Entity.RemovalReason.DISCARDED);
        } finally {
            DroomSlapen.altijd = voor;
        }
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /** The plein in the middle, five islands 120 blocks out on the compass; the placeholders have their steigers; a json reads. */
    @GuhTest(template = TUIN, batch = BATCH)
    public static void eilandenIndelingEnJson(GameTestHelper helper) {
        helper.assertTrue(DroomEilanden.midden(DroomEilanden.PLEIN).equals(new BlockPos(0, DroomEilanden.Y, 0)), "plein at the origin");
        helper.assertTrue(DroomEilanden.midden(DroomEilanden.STERREN).getZ() < -100 && Math.abs(DroomEilanden.midden(DroomEilanden.STERREN).getX()) <= 1,
                "the Sterrenkussenveld lies north");
        for (String id : DroomEilanden.IDS) {
            if (!id.equals(DroomEilanden.PLEIN)) {
                BlockPos m = DroomEilanden.midden(id);
                double d = Math.sqrt(m.getX() * (double) m.getX() + m.getZ() * (double) m.getZ());
                helper.assertTrue(Math.abs(d - DroomEilanden.AFSTAND) < 2, id + " lies 120 blocks out, got " + d);
                helper.assertTrue(DroomEilanden.dichtstbij(Vec3.atCenterOf(m)).equals(id), "closest island of its own centre: " + id);
                helper.assertTrue(DroomEilanden.placeholder(id).steiger() != null, id + " placeholder has a steiger");
            }
        }
        helper.assertTrue(DroomEilanden.placeholder(DroomEilanden.PLEIN).steigers().size() == 5, "the plein has a steiger per island");
        helper.assertTrue(DroomEilanden.placeholder(DroomEilanden.PLEIN).wekkerguh() != null, "the plein has a spot for the Wekkerguh");
        DroomEilanden.Opzet o = DroomEilanden.lees("liedjes", JsonParser.parseString("{\"versie\": 3, \"midden\": [20, 10, 22], "
                + "\"aankomst\": {\"plek\": [1.5, 1, -4.5], \"yaw\": 90}, \"steiger\": [0.5, 1, 30.5]}").getAsJsonObject());
        helper.assertTrue(o.versie() == 3 && o.midden().equals(new BlockPos(20, 10, 22)) && o.aankomst().yaw() == 90f
                && o.template().toString().equals("guhs:dromen/eiland_liedjes") && o.steiger() != null, "an island json reads");
        ServerPlayer p = speler(helper);
        helper.assertTrue(DroomRegels.magBouwen(p), "outside the dream you build as always");
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }
}
