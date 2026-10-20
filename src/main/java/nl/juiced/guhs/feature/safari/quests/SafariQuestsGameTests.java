package nl.juiced.guhs.feature.safari.quests;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Op Safari! S4: the quest state machines (vermomde guh, te vahe olifant), the geheimpjes, the Beverbaas counter and the
 * park data the quests read. Each test uses a fresh fake player (its safari data is wiped at the end).
 */
public class SafariQuestsGameTests {
    private static final String BATCH = "safari";

    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "safari_s4_test"));
        BlockPos b = helper.absolutePos(new BlockPos(1, 2, 1));
        p.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        return p;
    }

    private static void opruimen(GameTestHelper helper, ServerPlayer p) {
        SafariSpeler.wis(helper.getLevel().getServer(), p.getUUID());
        KledingUnlocks.wis(p);
        for (Entity e : helper.getLevel().getEntities((Entity) null, new AABB(p.blockPosition()).inflate(8), e -> e instanceof GuhEntity)) {
            e.discard();
        }
    }

    /** The geheimpjes and Dikkie's stal came along from the park generator: eight geheimpjes, one per zone. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void questDataGeladen(GameTestHelper helper) {
        QuestData d = QuestData.get();
        Set<String> zones = new HashSet<>();
        for (QuestData.Geheimpje g : d.geheimpjes) {
            zones.add(g.zone());
        }
        helper.assertTrue(d.geheimpjes.size() == 8 && zones.size() == 8, "eight geheimpjes in eight zones, got " + d.geheimpjes.size());
        helper.assertTrue(!zones.contains("entree"), "no geheimpje in the entreedorp");
        helper.assertTrue(d.stal != null && d.loopband != null && d.poort != null, "Dikkie's stal is in quests.json");
        helper.succeed();
    }

    /** The disguised guh must be found in order; at the end a tamed guh in the zebrapakje, the suits, 150 coins. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void vermomdeGuhInVolgorde(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        MinecraftServer server = helper.getLevel().getServer();
        VermomdeGuh.vind(p, 1, null);
        helper.assertTrue(SafariSpeler.quest(server, p.getUUID(), VermomdeGuh.QUEST).isEmpty(), "the panda first does nothing");
        VermomdeGuh.vind(p, 0, null);
        helper.assertTrue(SafariSpeler.quest(server, p.getUUID(), VermomdeGuh.QUEST).equals("1"), "the zebra: stage 1");
        VermomdeGuh.vind(p, 2, null);
        helper.assertTrue(SafariSpeler.quest(server, p.getUUID(), VermomdeGuh.QUEST).equals("1"), "the penguin too early: still 1");
        VermomdeGuh.vind(p, 1, null);
        VermomdeGuh.vind(p, 2, null);
        helper.assertTrue(SafariSpeler.quest(server, p.getUUID(), VermomdeGuh.QUEST).equals("klaar"), "all three: klaar");
        helper.assertTrue(KledingUnlocks.heeft(p, GuhClothes.ZEBRAPAKJE), "the zebrapakje is unlocked");
        helper.assertTrue(SafariMunten.saldo(p) >= VermomdeGuh.MUNTEN, "150 safarimunten");
        List<Entity> guhs = helper.getLevel().getEntities((Entity) null, new AABB(p.blockPosition()).inflate(4),
                e -> e instanceof GuhEntity g && g.isTame() && g.getClothes(GuhClothes.Slot.BODY) == GuhClothes.ZEBRAPAKJE);
        helper.assertTrue(guhs.size() == 1, "Streepje came along, in his zebrapakje");
        int saldo = SafariMunten.saldo(p);
        VermomdeGuh.vind(p, 2, null);
        helper.assertTrue(SafariMunten.saldo(p) == saldo, "no second reward");
        opruimen(helper, p);
        helper.succeed();
    }

    /** Dikkie: start, the diet (any order, only what he still wants), then the treadmill laps; the rewards. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void vaheOlifantDieetEnLoopband(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        MinecraftServer server = helper.getLevel().getServer();
        VaheOlifant.ren(p, false);
        helper.assertTrue(VaheOlifant.rondes(server, p.getUUID()) == 0, "the treadmill does nothing before the diet");
        VaheOlifant.praat(p, ItemStack.EMPTY);
        helper.assertTrue(SafariSpeler.quest(server, p.getUUID(), VaheOlifant.QUEST).equals("dieet"), "started: dieet");
        ItemStack meloen = new ItemStack(Items.MELON_SLICE, 3);
        VaheOlifant.praat(p, meloen);
        helper.assertTrue(meloen.getCount() == 2, "one melon slice eaten");
        VaheOlifant.praat(p, meloen);
        helper.assertTrue(meloen.getCount() == 2, "not twice the same");
        VaheOlifant.praat(p, new ItemStack(Items.APPLE));
        VaheOlifant.praat(p, new ItemStack(Items.CARROT));
        helper.assertTrue(SafariSpeler.quest(server, p.getUUID(), VaheOlifant.QUEST).equals("loopband"), "the diet done: loopband");
        for (int i = 0; i < VaheOlifant.RONDES; i++) {
            VaheOlifant.ren(p, false);
        }
        helper.assertTrue(SafariSpeler.quest(server, p.getUUID(), VaheOlifant.QUEST).equals("klaar"), "five laps: klaar");
        helper.assertTrue(p.getInventory().hasAnyMatching(s -> s.is(SafariQuests.OLIFANTENLOOPBAND_ITEM.get()))
                && p.getInventory().hasAnyMatching(s -> s.is(SafariQuests.FOTOLIJSTJE_ITEM.get())), "the treadmill and the photo frame");
        helper.assertTrue(SafariMunten.saldo(p) == VaheOlifant.MUNTEN, "150 safarimunten");
        opruimen(helper, p);
        helper.succeed();
    }

    /** Every geheimpje pays 25 once; all of them give the trophy and make the player a Speurneus. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void geheimpjesEnSpeurneus(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        List<QuestData.Geheimpje> alle = QuestData.get().geheimpjes;
        Titels.Titel speurneus = Titels.van("speurneus");
        helper.assertTrue(speurneus != null && !Titels.heeft(p, speurneus), "not a Speurneus yet");
        for (QuestData.Geheimpje g : alle) {
            Geheimpjes.vind(p, g);
        }
        helper.assertTrue(Geheimpjes.gevonden(p) == alle.size() && Geheimpjes.allemaal(p), "all found");
        helper.assertTrue(SafariMunten.saldo(p) == alle.size() * Geheimpjes.MUNTEN, "25 each");
        Geheimpjes.vind(p, alle.get(0));
        helper.assertTrue(SafariMunten.saldo(p) == alle.size() * Geheimpjes.MUNTEN, "a second time pays nothing");
        helper.assertTrue(p.getInventory().hasAnyMatching(s -> s.is(SafariQuests.GEHEIMPJES_TROFEE_ITEM.get())), "the trophy");
        helper.assertTrue(Titels.heeft(p, speurneus), "Speurneus");
        opruimen(helper, p);
        helper.succeed();
    }

    /** Five beaver dams make a Beverbaas. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void beverbaas(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        Titels.Titel baas = Titels.van("beverbaas");
        for (int i = 0; i < Beloningen.BEVERBAAS - 1; i++) {
            SafariQuests.beverdamGebouwd(p);
        }
        helper.assertTrue(baas != null && !Titels.heeft(p, baas), "four dams: not yet");
        SafariQuests.beverdamGebouwd(p);
        helper.assertTrue(Titels.heeft(p, baas), "five dams: Beverbaas");
        opruimen(helper, p);
        helper.succeed();
    }
}
