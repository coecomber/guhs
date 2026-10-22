package nl.juiced.guhs.feature.knabbelvelden;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Plek;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.band.Roepen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.PickedUpGuhItem;
import nl.juiced.guhs.quest.GuhNummers;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of De Eeuwige Knabbelvelden (design ch. 7): a guh goes into retirement and comes home as a picked-up guh with
 * its number, name, clothes, fur and personality; never two of the same guh (bringing it home twice, a stale copy that
 * tries to join); a busy guh (riding) only goes after the player confirmed. (The test server has no datapack dimensions:
 * the plots and ghosts are not tested here.)
 */
public class KnabbelveldenGameTests {
    private static final String VLOER = "px_test_16";
    private static final String BATCH = "knabbelvelden";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(new BlockPos(8, 2, 8));
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static GuhEntity guh(GameTestHelper helper, ServerPlayer p, String naam) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 2, 5));
        guh.tame(p);
        guh.setCustomName(Component.literal(naam));
        guh.setVariant(GuhVariant.STARRY);
        guh.wear(GuhClothes.HEMEL_AUREOOLTJE);
        GuhVolger.zet(guh, PlekSoort.WERELD, "");
        return guh;
    }

    private static void klaar(GameTestHelper helper, ServerPlayer p) {
        for (UUID id : Pensioen.inVelden(p)) {
            KnabbelveldenData.get(helper.getLevel().getServer()).haalWeg(p.getUUID(), id);
        }
        helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static int telItems(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(ModItems.PICKED_UP_GUH.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static boolean lijstHeeft(CompoundTag data, String lijst, UUID id) {
        for (Tag t : data.getListOrEmpty(lijst)) {
            if (id.equals(((CompoundTag) t).read("Id", UUIDUtil.CODEC).orElse(null))) {
                return true;
            }
        }
        return false;
    }

    /** Send and bring home: the very same guh, with its number, name, clothes, fur and personality. */
    @GuhTest(template = VLOER, batch = BATCH)
    public static void knabbelveldenRondjeHoudtAllesVast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, "Opa Vadsje");
        UUID id = guh.getUUID();
        int nr = GuhNummers.nummer(level.getServer(), p.getUUID(), id);
        String aard = guh.getPersonality().id();
        helper.assertTrue(nr > 0, "the tamed guh has a Guhdex number");
        BlockPos portaal = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertTrue(lijstHeeft(Pensioen.data(p, portaal, null), "Thuis", id), "the screen lists it at home");

        helper.assertTrue(Pensioen.stuur(p, id, false) == Pensioen.Uitkomst.OK, "it goes into retirement");
        helper.assertTrue(guh.isRemoved() && level.getEntity(id) == null, "it left the world");
        helper.assertTrue(Pensioen.inVelden(p).contains(id), "it is in the Knabbelvelden");
        Plek plek = GuhVolger.plek(level.getServer(), p.getUUID(), id);
        helper.assertTrue(plek != null && plek.soort() == PlekSoort.IN_DE_KNABBELVELDEN, "'Waar is mijn guh?' says retired: " + plek);
        CompoundTag data = Pensioen.data(p, portaal, null);
        helper.assertTrue(!lijstHeeft(data, "Thuis", id) && lijstHeeft(data, "Velden", id), "the screen moved it to the Knabbelvelden column");
        helper.assertTrue(Roepen.roep(p, id) == Roepen.Uitkomst.NIET_THUIS, "it can't be called while retired");
        helper.assertTrue(Pensioen.stuur(p, id, true) == Pensioen.Uitkomst.NIET_NU, "it can't go twice");

        ItemStack terug = Pensioen.terug(p, id, null);
        helper.assertTrue(!terug.isEmpty() && telItems(p) == 1, "it came home in the pockets");
        CompoundTag item = PickedUpGuhItem.guhData(terug);
        helper.assertTrue(item.getIntOr(PickedUpGuhItem.NUMMER, 0) == nr && p.getUUID().toString().equals(item.getStringOr(PickedUpGuhItem.EIGENAAR, "")),
                "the item knows its number and owner");
        helper.assertTrue(!Pensioen.inVelden(p).contains(id), "no longer in the Knabbelvelden");
        Vec3 neer = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, 2, 10)));
        Entity e = PickedUpGuhItem.release(level, item, neer.x, neer.y, neer.z, 0f);
        helper.assertTrue(e instanceof GuhEntity g && g.getUUID().equals(id) && g.isTame() && p.getUUID().equals(g.getOwnerUUID())
                && g.getName().getString().equals("Opa Vadsje") && g.getVariant() == GuhVariant.STARRY
                && g.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.HEMEL_AUREOOLTJE && g.getPersonality().id().equals(aard),
                "the very same guh: name, fur, clothes, personality, owner");
        helper.assertTrue(GuhNummers.nummer(level.getServer(), p.getUUID(), id) == nr, "the same number in the Guhdex");
        e.discard();
        klaar(helper, p);
        helper.succeed();
    }

    /** Never two of the same guh: not by bringing it home twice, not by a stale copy that tries to join the world. */
    @GuhTest(template = VLOER, batch = BATCH)
    public static void knabbelveldenNooitDubbel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, "Dubbeltje");
        UUID id = guh.getUUID();
        CompoundTag oud = nl.juiced.guhs.storage.Nbt.saveWithoutId(guh);
        oud.putString("id", EntityType.getKey(guh.getType()).toString());
        helper.assertTrue(Pensioen.stuur(p, id, false) == Pensioen.Uitkomst.OK, "it goes");
        // a stale copy of the same guh (an old chunk, a duplicated item...) can't come in while it is retired
        Vec3 plek = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 2, 6)));
        PickedUpGuhItem.release(level, oud, plek.x, plek.y, plek.z, 0f);
        helper.assertTrue(level.getEntity(id) == null, "the stale copy did not join");
        helper.assertTrue(!Pensioen.terug(p, id, null).isEmpty(), "it comes home once");
        helper.assertTrue(Pensioen.terug(p, id, null).isEmpty() && telItems(p) == 1, "not a second time");
        // 1.6.2: an item in your pockets goes too (it is taken out of them), and comes home once more
        helper.assertTrue(Pensioen.stuur(p, id, true) == Pensioen.Uitkomst.OK && telItems(p) == 0 && Pensioen.inVelden(p).size() == 1,
                "an item in your pockets goes too, out of them");
        helper.assertTrue(!Pensioen.terug(p, id, null).isEmpty() && telItems(p) == 1, "and comes home as one item again");
        // put down again, sent again, and the item from before can't bring a second one
        ItemStack stack = p.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(ModItems.PICKED_UP_GUH.get())).findFirst().orElseThrow();
        Entity weer = PickedUpGuhItem.release(level, PickedUpGuhItem.guhData(stack), plek.x, plek.y, plek.z, 0f);
        stack.shrink(1);
        GuhVolger.zet(weer, PlekSoort.WERELD, "");
        helper.assertTrue(weer instanceof GuhEntity && level.getEntity(id) == weer, "put down: the guh is back in the world");
        helper.assertTrue(Pensioen.stuur(p, id, false) == Pensioen.Uitkomst.OK && level.getEntity(id) == null, "and retired again");
        helper.assertTrue(level.getEntitiesOfClass(GuhEntity.class, helper.getBounds().inflate(4), g -> g.getUUID().equals(id)).isEmpty()
                && Pensioen.inVelden(p).size() == 1, "exactly one of it, in the Knabbelvelden");
        klaar(helper, p);
        helper.succeed();
    }

    /** A busy guh (here: riding a minecart) is only sent after the player said yes. */
    @GuhTest(template = VLOER, batch = BATCH)
    public static void knabbelveldenBezigeGuhVraagtEerst(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper);
        GuhEntity guh = guh(helper, p, "Druk Baasje");
        UUID id = guh.getUUID();
        Entity kar = EntityType.MINECART.create(level, EntitySpawnReason.TRIGGERED);
        BlockPos k = helper.absolutePos(new BlockPos(5, 2, 5));
        kar.snapTo(k.getX() + 0.5, k.getY(), k.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(kar);
        helper.assertTrue(guh.startRiding(kar, true, true), "the guh rides the minecart");
        helper.assertTrue("rijdt".equals(Pensioen.bezig(level.getServer(), p.getUUID(), id)), "it is busy: riding");
        CompoundTag data = Pensioen.data(p, helper.absolutePos(new BlockPos(2, 2, 2)), null);
        boolean gemarkeerd = data.getListOrEmpty("Thuis").stream().anyMatch(t -> id.equals(((CompoundTag) t).read("Id", UUIDUtil.CODEC).orElse(null))
                && ((CompoundTag) t).getStringOr("Bezig", "").equals("rijdt"));
        helper.assertTrue(gemarkeerd, "the screen marks it busy (the client asks first)");
        helper.assertTrue(Pensioen.stuur(p, id, false) == Pensioen.Uitkomst.BEVESTIGEN, "without a yes: the question");
        helper.assertTrue(!guh.isRemoved() && !Pensioen.inVelden(p).contains(id), "and it stays");
        helper.assertTrue(Pensioen.stuur(p, id, true) == Pensioen.Uitkomst.OK, "with a yes it goes");
        helper.assertTrue(guh.isRemoved() && Pensioen.inVelden(p).contains(id) && kar.getPassengers().isEmpty(), "off the minecart, into retirement");
        kar.discard();
        klaar(helper, p);
        helper.succeed();
    }
}
