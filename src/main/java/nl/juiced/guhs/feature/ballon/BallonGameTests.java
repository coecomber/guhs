package nl.juiced.guhs.feature.ballon;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Binnenkort;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the Ballonfestival: the four routes (closed loops, smooth, turned with the festival, slow at the
 * viewpoints), a whole flight (sped up) with its stamps and ballonmunten (+1), no jumping out halfway, the next route,
 * Kapitein Wolkje's role, shop and balloon, and the template.
 */
public class BallonGameTests {
    private static final String EMPTY = "empty";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            p.stopRiding();
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static LuchtballonEntity ballon(GameTestHelper helper, BlockPos rel) {
        LuchtballonEntity b = helper.spawn(BallonFeature.LUCHTBALLON.get(), rel);
        BlockPos abs = helper.absolutePos(rel);
        b.setThuis(abs, 0);
        return b;
    }

    @GuhTest(template = EMPTY)
    public static void ballonRoutes(GameTestHelper helper) {
        Vec3 thuis = new Vec3(100.5, 70, -40.5);
        Set<BallonRoute.Uitzicht> gezien = new HashSet<>();
        for (BallonRoute r : BallonRoute.values()) {
            gezien.add(r.eerste);
            gezien.add(r.tweede);
            helper.assertTrue(r.eerstePunt > 1 && r.tweedePunt > r.eerstePunt && r.tweedePunt < r.punten() - 2, r + ": viewpoints on the way");
            for (float yaw : new float[]{0, 90, 180, 270}) {
                BallonRoute.Pad pad = r.pad(thuis, yaw, new float[r.punten()]);
                helper.assertTrue(pad.op(0).distanceTo(thuis) < 1e-6 && pad.op(pad.lengte()).distanceTo(thuis) < 1e-6, r + ": starts and ends at home");
                helper.assertTrue(pad.lengte() > 150 && pad.lengte() < 700, r + ": a proper round (" + pad.lengte() + ")");
                Vec3 was = pad.op(0);
                double hoogste = 0;
                for (double d = 0.25; d <= pad.lengte(); d += 0.25) {
                    Vec3 nu = pad.op(d);
                    helper.assertTrue(nu.distanceTo(was) < 0.35, r + ": smooth at " + d);
                    hoogste = Math.max(hoogste, nu.y - thuis.y);
                    was = nu;
                }
                helper.assertTrue(hoogste > 25, r + ": up in the air (" + hoogste + ")");
                helper.assertTrue(pad.snelheid(0) < pad.snelheid(40) && pad.snelheid(pad.bijPunt(r.eerstePunt)) < pad.snelheid(40)
                        && pad.snelheid(pad.lengte()) < BallonRoute.SNELHEID, r + ": slow at the start, the viewpoints and the landing");
            }
            // the festival's rotation: a quarter turn clockwise, like a structure's CLOCKWISE_90: (x, z) -> (-z, x)
            Vec3 p = r.punt(3), q = r.draai(3, 90);
            helper.assertTrue(Math.abs(q.x + p.z) < 1e-6 && Math.abs(q.z - p.x) < 1e-6 && q.y == p.y, r + ": turned with the festival");
        }
        helper.assertTrue(gezien.size() == 8 && BallonRoute.Uitzicht.values().length == 8, "four routes, eight different viewpoints");
        var kaart = KnusVoortgang.verzameling(BallonVlucht.STEMPELS);
        helper.assertTrue(kaart != null && kaart.items().size() == 8 && kaart.onderdeel().equals("ballon"), "the ballonstempelkaart");
        helper.assertTrue(KnusVoortgang.mijlpalen("ballon").size() >= 2, "milestones");
        helper.assertTrue(BallonVlucht.ballonmunten(0) == 1 + 1 && BallonVlucht.ballonmunten(1) == 2 + 1 && BallonVlucht.ballonmunten(2) == 3 + 1,
                "+1 on every reward");
        helper.succeed();
    }

    /** A whole flight (a small round, 8x faster): up, round, down onto the steiger; two stamps and ballonmunten; the next route. */
    @GuhTest(template = EMPTY, timeoutTicks = 600)
    public static void ballonVlucht(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        // (the test area's own chunk is the one that surely ticks entities: fly a small round in its middle)
        BlockPos o = helper.absolutePos(new BlockPos(0, 2, 0));
        BlockPos midden = new BlockPos((o.getX() >> 4) * 16 + 8, o.getY(), (o.getZ() >> 4) * 16 + 8);
        LuchtballonEntity b = BallonFeature.LUCHTBALLON.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        b.snapTo(midden.getX() + 0.5, midden.getY(), midden.getZ() + 0.5, 0, 0);
        b.setThuis(midden, 0);
        helper.getLevel().addFreshEntity(b);
        BallonRoute route = BallonVlucht.volgendeRoute(p);
        helper.assertTrue(route == BallonRoute.PLUISJESRONDE, "the first flight: the Pluisjesronde");
        BallonVlucht.setTempo(8);
        BallonVlucht.setSchaal(0.09);
        helper.assertTrue(b.stijgOp(p, route, null), "take-off");
        helper.assertTrue(p.getVehicle() == b && b.vliegt() && BallonFeature.vliegt(p) && Minigames.BALLON.equals(Minigames.playing(p)), "flying");
        helper.assertTrue(!b.stijgOp(p, route, null), "not twice");
        Vec3 thuis = b.thuis();
        double[] hoog = {0};
        helper.onEachTick(() -> hoog[0] = Math.max(hoog[0], b.getY() - thuis.y));
        helper.succeedWhen(() -> {
            helper.assertTrue(!b.vliegt(), "still flying at " + b.afstand() + " / " + (b.pad() == null ? -1 : b.pad().lengte()) + ", " + b.position());
            BallonVlucht.setTempo(1);
            BallonVlucht.setSchaal(1);
            helper.assertTrue(hoog[0] > 3, "it went up: " + hoog[0]);
            helper.assertTrue(p.getVehicle() == null && b.position().distanceTo(thuis) < 0.01, "landed at home, passenger out");
            helper.assertTrue(KnusVoortgang.ontdekt(p, BallonVlucht.STEMPELS).size() == 2, "two stamps");
            helper.assertTrue(count(p, BallonFeature.BALLONMUNT.get()) == BallonVlucht.ballonmunten(2), "ballonmunten");
            helper.assertTrue(KnusVoortgang.teller(p, BallonVlucht.VLUCHTEN) == 1, "a flight counted");
            helper.assertTrue(BallonVlucht.volgendeRoute(p) == BallonRoute.HOGE_VADS, "next time: the next route");
            leave(helper, p);
            b.discard();
        });
    }

    /** No jumping out halfway (sneaking); a flight that's broken off lands at once without stamps. */
    @GuhTest(template = EMPTY)
    public static void ballonNietUitstappen(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        LuchtballonEntity b = ballon(helper, new BlockPos(4, 2, 4));
        try {
            b.stijgOp(p, BallonRoute.KNABBELKRING, null);
            p.setShiftKeyDown(true);
            p.stopRiding();
            helper.assertTrue(p.getVehicle() == b, "sneaking doesn't get you out of the basket");
            p.setShiftKeyDown(false);
            b.land(false);
            helper.assertTrue(p.getVehicle() == null && !b.vliegt(), "broken off: out, and home");
            helper.assertTrue(KnusVoortgang.ontdekt(p, BallonVlucht.STEMPELS).isEmpty() && count(p, BallonFeature.BALLONMUNT.get()) == 0,
                    "no stamps, no coins");
        } finally {
            leave(helper, p);
            b.discard();
        }
        helper.succeed();
    }

    /** Kapitein Wolkje: his own role and shop; he takes the balloon on his steiger, never a decoration, and fetches one if there's none. */
    @GuhTest(template = EMPTY)
    public static void ballonKapitein(GameTestHelper helper) {
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.BALLONGUH) == BallonRole.INSTANCE && BallonRole.INSTANCE != Binnenkort.ROLE, "his own role");
        helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.BALLONGUH), "his Guhdex page");
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(1, 1, 7));
        npc.setKind(GuhNpcEntity.Kind.BALLONGUH);
        Set<Item> koop = new HashSet<>();
        for (MerchantOffer o : BallonRole.INSTANCE.offers(npc)) {
            helper.assertTrue(o.getBaseCostA().is(BallonFeature.BALLONMUNT.get()), "paid with ballonmunten");
            koop.add(o.getResult().getItem());
        }
        helper.assertTrue(koop.contains(ModItems.clothingItem(GuhClothes.BALLONPET)) && koop.contains(ModItems.clothingItem(GuhClothes.BALLONBRIL))
                && koop.contains(BallonFeature.MINI_LUCHTBALLON_ITEM.get()), "the shop");
        helper.assertTrue(BallonRole.ballon(npc) == null, "no balloon and no steiger: none");
        helper.setBlock(new BlockPos(4, 1, 7), BallonFeature.BALLONSTEIGER.get());
        LuchtballonEntity nieuw = BallonRole.ballon(npc);
        helper.assertTrue(nieuw != null && nieuw.thuisBlok().equals(helper.absolutePos(new BlockPos(4, 2, 7))), "he fetches one to his steiger");
        helper.assertTrue(BallonRole.ballon(npc) == nieuw, "and then it's that one");
        nieuw.discard();
        LuchtballonEntity deco = ballon(helper, new BlockPos(6, 2, 3));
        var tag = new net.minecraft.nbt.CompoundTag();
        nl.juiced.guhs.storage.Nbt.saveWithoutId(deco, tag);
        tag.putBoolean("Deco", true);
        nl.juiced.guhs.storage.Nbt.load(deco, tag);
        helper.assertTrue(deco.isDeco() && BallonRole.ballon(npc) != deco, "never a decoration balloon");
        ServerPlayer p = player(helper);
        helper.assertTrue(!deco.stijgOp(p, BallonRoute.HOGE_VADS, null) && p.getVehicle() == null, "a decoration balloon doesn't fly");
        for (LuchtballonEntity e : helper.getLevel().getEntitiesOfClass(LuchtballonEntity.class, npc.getBoundingBox().inflate(30))) {
            e.discard();
        }
        leave(helper, p);
        npc.discard();
        helper.setBlock(new BlockPos(4, 1, 7), Blocks.AIR);
        helper.succeed();
    }

    /** 1.2.11: talking to Kapitein Wolkje opens his menu (no take-off yet); "Instappen!" is what flies, with him along. */
    @GuhTest(template = EMPTY)
    public static void ballonMenu(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(1, 1, 5));
        npc.setKind(GuhNpcEntity.Kind.BALLONGUH);
        LuchtballonEntity b = ballon(helper, new BlockPos(4, 2, 4));
        ServerPlayer p = player(helper);
        try {
            BallonRole.INSTANCE.talk(npc, p);
            helper.assertTrue(p.getVehicle() == null && !b.vliegt(), "talking only opens the menu");
            helper.assertTrue(KnusVoortgang.teller(p, BallonVlucht.GEVONDEN) == 1, "found him");
            BallonRole.INSTANCE.antwoord(npc, p, BallonRole.OPT_UITLEG);
            helper.assertTrue(p.getVehicle() == null && !b.vliegt(), "the explanation doesn't fly either");
            BallonRole.INSTANCE.antwoord(npc, p, BallonRole.OPT_VLIEGEN);
            helper.assertTrue(p.getVehicle() == b && b.vliegt() && npc.getUUID().equals(b.kapitein()), "stepping in: off, with the captain");
            b.land(false);
        } finally {
            leave(helper, p);
            b.discard();
            npc.discard();
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void ballonFestivalTemplate(GameTestHelper helper) {
        var t = helper.getLevel().getStructureManager().get(Guhs.id("ballonfestival"));
        helper.assertTrue(t.isPresent() && t.get().getSize().getX() <= 72 && t.get().getSize().getZ() <= 72 && t.get().getSize().getY() <= 40,
                "the festival template (72 x 40 x 72 at most)");
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        var jigsaws = t.get().filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW, true);
        helper.assertTrue(jigsaws.size() == 1 && jigsaws.get(0).nbt().getStringOr("name", "").equals("guhs:ballonfestival_midden"), "its anchor");
        helper.assertTrue(t.get().filterBlocks(BlockPos.ZERO, settings, BallonFeature.BALLONSTEIGER.get(), true).size() > 30, "steigers");
        helper.succeed();
    }
}
