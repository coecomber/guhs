package nl.juiced.guhs.feature.knabbelvelden;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.MijnGuhs;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.UitItem;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.quest.GuhNummers;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.6.2: De Eeuwige Knabbelvelden take picked-up guhs (out of a chest) and every kind of "Mijn andere vadsjes" (a round trip
 * for each maatje kind the game has: send, kept as its item, gone from the Guhdex, home again as the same item); retired
 * guhs leave "Mijn guhs" and keep their number. (The plot and its ghosts need the Knabbelvelden dimension: not here.)
 */
public class KnabbelveldenVadsjesGameTests {
    private static final String TUIN = "huisje_test_tuin";
    private static final String BATCH = "knabbelvelden_vadsjes";

    @SuppressWarnings("removal")
    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(new BlockPos(20, 1, 20));
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer p) {
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    /** The ids in the Guhdex tab list ("Guhs" or "Vadsjes") of this player. */
    private static List<String> lijst(ServerPlayer p, String welke) {
        List<String> uit = new ArrayList<>();
        for (Tag t : MijnGuhs.snapshot(p, null).getListOrEmpty(welke)) {
            uit.add(((CompoundTag) t).getStringOr("Id", ""));
        }
        return uit;
    }

    private static GuhEntity guh(GameTestHelper helper, ServerPlayer p, BlockPos at) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), at);
        guh.tame(p);
        GuhVolger.zet(guh, PlekSoort.WERELD, "");
        return guh;
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void gepensioneerdUitMijnGuhs(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        MinecraftServer s = p.level().getServer();
        GuhEntity guh = guh(helper, p, new BlockPos(4, 1, 4));
        UUID id = guh.getUUID();
        int nr = GuhNummers.nummer(s, p.getUUID(), id);
        helper.assertTrue(nr > 0 && lijst(p, "Guhs").contains(id.toString()), "listed in Mijn guhs, with a number");
        helper.assertTrue(Pensioen.stuur(p, id, true) == Pensioen.Uitkomst.OK, "retired");
        helper.assertTrue(!lijst(p, "Guhs").contains(id.toString()), "gone from Mijn guhs");
        GuhEntity ander = guh(helper, p, new BlockPos(6, 1, 4));
        helper.assertTrue(GuhNummers.nummer(s, p.getUUID(), ander.getUUID()) > nr, "its number is not handed out again");
        ItemStack thuis = Pensioen.terug(p, id, null);
        helper.assertTrue(!thuis.isEmpty() && lijst(p, "Guhs").contains(id.toString()), "home again: listed again");
        helper.assertTrue(GuhNummers.nummer(s, p.getUUID(), id) == nr, "with its old number");
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 400)
    public static void opgepakteGuhUitKistMetPensioen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        MinecraftServer s = p.level().getServer();
        GuhEntity guh = guh(helper, p, new BlockPos(4, 1, 4));
        UUID id = guh.getUUID();
        ItemStack stack = PickedUpGuhItem.pickUp(guh);
        BlockPos kist = new BlockPos(4, 1, 6);
        helper.setBlock(kist, Blocks.CHEST);
        ChestBlockEntity be = helper.getBlockEntity(kist, ChestBlockEntity.class);
        be.setItem(2, stack.copy());
        GuhVolger.item(stack, PlekSoort.ITEM_KIST, helper.getLevel().dimension(), helper.absolutePos(kist), "Kist", helper.getLevel().getGameTime());
        helper.assertTrue(Pensioen.nietNu(s, p.getUUID(), id) == null, "a picked-up guh may go now");
        helper.assertTrue("opgepakt".equals(Pensioen.bezig(s, p.getUUID(), id)), "after a question: it is taken out of the chest");
        helper.assertTrue(Pensioen.stuur(p, id, false) == Pensioen.Uitkomst.BEVESTIGEN, "asks first");
        Pensioen.Uitkomst u = Pensioen.stuur(p, id, true);
        helper.assertTrue(u == Pensioen.Uitkomst.OK || u == Pensioen.Uitkomst.ONDERWEG, "it goes: " + u);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(be.getItem(2).isEmpty(), "the chest doesn't hold it any more");
            helper.assertTrue(KnabbelveldenData.get(s).vind(p.getUUID(), id) != null, "retired");
            helper.assertTrue(helper.getLevel().getEntity(id) == null, "and not in the world");
            helper.assertTrue(!lijst(p, "Guhs").contains(id.toString()), "and not in Mijn guhs");
            weg(helper, p);
            helper.succeed();
        });
    }

    /** Every maatje kind there is: tame one, send it, it is kept as its item and leaves the Guhdex, then home again. */
    @GuhTest(template = TUIN, batch = BATCH, timeoutTicks = 200)
    public static void elkVadsjeRondje(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        MinecraftServer s = p.level().getServer();
        List<String> soorten = Vadsjes.alleSoorten(helper.getLevel());
        helper.assertTrue(soorten.contains("pieppiepmuisje") && soorten.contains("schilly") && soorten.contains("poepschilly"),
                "the kinds come from the registry: " + soorten);
        int x = 2;
        for (String soort : soorten) {
            EntityType<?> type = Vadsjes.soorten(helper.getLevel()).get(soort);
            Entity e = type.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            helper.assertTrue(e instanceof TamableAnimal, soort + ": a tamable creature");
            TamableAnimal dier = (TamableAnimal) e;
            BlockPos at = helper.absolutePos(new BlockPos(x, 1, 10));
            x = x >= 20 ? 2 : x + 2;
            dier.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
            dier.tame(p);
            dier.setPersistenceRequired();
            helper.getLevel().addFreshEntity(dier);
            UUID id = Band.id(dier);
            GuhVolger.zet(dier, PlekSoort.WERELD, "");
            helper.assertTrue(lijst(p, "Vadsjes").contains(id.toString()), soort + ": in Mijn andere vadsjes");
            Pensioen.Uitkomst u = Pensioen.stuur(p, id, true);
            helper.assertTrue(u == Pensioen.Uitkomst.OK, soort + ": retired: " + u);
            KnabbelveldenData.Rust r = KnabbelveldenData.get(s).vind(p.getUUID(), id);
            helper.assertTrue(r != null && r.isMaatje() && soort.equals(r.soort()), soort + ": kept as a maatje of its kind");
            helper.assertTrue(dier.isRemoved() && Vadsjes.zoekGeladen(s, id) == null, soort + ": not in the world any more");
            helper.assertTrue(!lijst(p, "Vadsjes").contains(id.toString()), soort + ": gone from Mijn andere vadsjes");
            ItemStack thuis = Pensioen.terug(p, id, null);
            helper.assertTrue(!thuis.isEmpty() && UitItem.isDeze(thuis, id), soort + ": home again as its own item");
            helper.assertTrue(KnabbelveldenData.get(s).vind(p.getUUID(), id) == null, soort + ": no longer retired");
            helper.assertTrue(lijst(p, "Vadsjes").contains(id.toString()), soort + ": listed again");
            // never two: the same one again can't be brought home twice
            helper.assertTrue(Pensioen.terug(p, id, null).isEmpty(), soort + ": only once");
        }
        weg(helper, p);
        helper.succeed();
    }

    @GuhTest(template = TUIN, batch = BATCH)
    public static void opgepaktVadsjeUitKistMetPensioen(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        MinecraftServer s = p.level().getServer();
        TamableAnimal muisje = helper.spawn(nl.juiced.guhs.feature.piep.PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(4, 1, 4));
        muisje.tame(p);
        UUID id = Band.id(muisje);
        GuhVolger.zet(muisje, PlekSoort.WERELD, "");
        ItemStack item = nl.juiced.guhs.feature.piep.PiepDierItem.van(muisje, ((nl.juiced.guhs.feature.piep.PiepMaatje) muisje).oppakItem());
        muisje.discard();
        BlockPos kist = new BlockPos(4, 1, 6);
        helper.setBlock(kist, Blocks.CHEST);
        ChestBlockEntity be = helper.getBlockEntity(kist, ChestBlockEntity.class);
        be.setItem(0, item.copy());
        GuhVolger.item(item, PlekSoort.ITEM_KIST, helper.getLevel().dimension(), helper.absolutePos(kist), "Kist", helper.getLevel().getGameTime());
        helper.assertTrue("opgepakt".equals(Pensioen.bezig(s, p.getUUID(), id)), "asks first");
        Pensioen.Uitkomst u = Pensioen.stuur(p, id, true);
        helper.assertTrue(u == Pensioen.Uitkomst.OK || u == Pensioen.Uitkomst.ONDERWEG, "it goes: " + u);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(be.getItem(0).isEmpty(), "out of the chest");
            helper.assertTrue(KnabbelveldenData.get(s).vind(p.getUUID(), id) != null, "retired");
            weg(helper, p);
            helper.succeed();
        });
    }
}
