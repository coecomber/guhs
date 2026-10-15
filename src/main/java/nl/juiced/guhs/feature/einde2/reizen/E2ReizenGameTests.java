package nl.juiced.guhs.feature.einde2.reizen;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.SimpleContainer;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.einde2.zwevers.LeegtewalvisguhEntity;
import nl.juiced.guhs.feature.einde2.zwevers.Leegtewalvissen;
import nl.juiced.guhs.feature.einde2.zwevers.Walviszicht;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Game tests of einde2 phase B, slice reizen (CONTRACT_E2_B 8: "reizen: guest rule, whale visibility"):
 * <ul>
 *   <li>the guest rule: riding in the host's basket makes a guest, after getting off one stays a guest close by and stops
 *       being one beyond {@value E2Reizen#GAST_AFSTAND} blocks; a stranger is nobody's guest;</li>
 *   <li>the whale questline: believing the captain makes the whale visible for that player only (and keeps the first sighting
 *       for the scene); feeding {@value Leegtewalvis#VOER} times at step 3 makes the whale the player's own (an older copy
 *       steps aside, the chests keep their items).</li>
 * </ul>
 * Template einde2_appa_test_wei (16 x 16 grass).
 */
public class E2ReizenGameTests {
    private static final String WEI = "einde2_appa_test_wei";

    private static ServerPlayer speler(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... spelers) {
        for (ServerPlayer p : spelers) {
            AppaGuhEntity appa = Appa.geladen(p);
            if (appa != null) {
                appa.discard();
            }
            E2Reizen.weg(p);
            p.stopRiding();
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    @GuhTest(template = WEI, batch = "einde2_reizen", timeoutTicks = 200)
    public static void einde2ReizenGast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer host = speler(helper, new BlockPos(4, 2, 4));
        ServerPlayer gast = speler(helper, new BlockPos(6, 2, 4));
        ServerPlayer vreemde = speler(helper, new BlockPos(10, 2, 10));
        AppaGuhEntity appa = Appa.geef(host);
        helper.assertTrue(appa != null, "the host has an Appa");
        BlockPos at = helper.absolutePos(new BlockPos(8, 2, 8));
        appa.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        host.startRiding(appa, true, false);
        helper.assertTrue(gast.startRiding(appa, true, false), "the guest climbs into the basket");
        helper.assertTrue(E2Reizen.gastheer(gast) == host, "riding in the host's basket: a guest");
        helper.assertTrue(E2Reizen.gasten(host).contains(gast) && !E2Reizen.gasten(host).contains(vreemde), "the host's guests");
        helper.assertTrue(E2Reizen.gastheer(host) == null && E2Reizen.gastheer(vreemde) == null, "the host and a stranger are nobody's guest");
        E2Reizen.gastenTick(level.getServer());
        gast.stopRiding();
        E2Reizen.gastenTick(level.getServer());
        helper.assertTrue(E2Reizen.gastheer(gast) == host, "off the basket, close by: still a guest");
        gast.teleportTo(level, host.getX() + E2Reizen.GAST_AFSTAND + 6, host.getY(), host.getZ(), Set.of(), 0, 0, true);
        E2Reizen.gastenTick(level.getServer());
        helper.assertTrue(E2Reizen.gastheer(gast) == null, "more than " + E2Reizen.GAST_AFSTAND + " blocks away: no guest any more");
        gast.teleportTo(level, host.getX() + 2, host.getY(), host.getZ(), Set.of(), 0, 0, true);
        E2Reizen.gastenTick(level.getServer());
        helper.assertTrue(E2Reizen.gastheer(gast) == null, "and coming back does not make one a guest again (only flying along does)");
        weg(helper, host, gast, vreemde);
        helper.succeed();
    }

    @GuhTest(template = WEI, batch = "einde2_reizen", timeoutTicks = 200)
    public static void einde2ReizenWalvis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer gelovige = speler(helper, new BlockPos(4, 2, 4));
        ServerPlayer ander = speler(helper, new BlockPos(10, 2, 4));
        Leegtewalvis.LIJN.wis(gelovige);
        Leegtewalvis.LIJN.wis(ander);
        GuhQuests.saved(gelovige).remove(Walvis.KEY);
        Walviszicht.zet(gelovige, false);
        Walviszicht.zet(ander, false);
        Leegtewalvis.neemAan(gelovige, null);
        helper.assertTrue(Leegtewalvis.LIJN.stap(gelovige) == 1 && Leegtewalvis.wachtplek(gelovige) != null, "believing: step 1 and a waiting spot");
        helper.assertTrue(Walviszicht.ziet(gelovige) && !Walviszicht.ziet(ander), "only who believes sees the whale");
        helper.assertTrue(!Leegtewalvissen.MAG_KOMEN.test(gelovige), "no whale comes by itself before the first sighting");
        BlockPos boven = helper.absolutePos(new BlockPos(8, 30, 8));
        LeegtewalvisguhEntity w = E2ZweversFeature.LEEGTEWALVISGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        w.snapTo(boven.getX(), boven.getY(), boven.getZ(), 0, 0);
        level.addFreshEntity(w);
        helper.assertTrue(w.broadcastToPlayer(gelovige) && !w.broadcastToPlayer(ander), "the whale is sent to the believer only");
        Leegtewalvis.LIJN.zet(gelovige, 3);
        helper.assertTrue(Leegtewalvissen.MAG_KOMEN.test(gelovige), "after the sighting whales come by themselves");
        ItemStack vruchten = new ItemStack(E2BiomesFeature.KNABBELKOORVRUCHT.get(), Leegtewalvis.VOER);
        for (int i = 0; i < Leegtewalvis.VOER; i++) {
            helper.assertTrue(w.eigenaar() == null, "still wild after " + i + " fruits");
            w.voer(gelovige, vruchten);
        }
        helper.assertTrue(gelovige.getUUID().equals(w.eigenaar()) && Walvis.heeft(gelovige), "fed " + Leegtewalvis.VOER + " times: the whale is theirs");
        helper.assertTrue(Leegtewalvis.LIJN.klaar(gelovige), "and the questline is done");
        helper.assertTrue(Walvis.geladen(gelovige) == w, "it is their loaded own whale");
        // an older copy (made before, from a chunk that loads later) steps aside
        LeegtewalvisguhEntity oud = E2ZweversFeature.LEEGTEWALVISGUH.get().create(level, EntitySpawnReason.TRIGGERED);
        oud.snapTo(boven.getX() + 40, boven.getY(), boven.getZ(), 0, 0);
        oud.zetEigenaar(gelovige.getUUID(), w.gen() - 1);
        level.addFreshEntity(oud);
        Walvis.meld(oud);
        helper.assertTrue(oud.isRemoved() && !w.isRemoved(), "an older copy of an own whale removes itself");
        // the furniture and a chest that really keeps items
        Walvis.zetDeel(gelovige, "kisten", true);
        Walvis.meld(w);
        helper.assertTrue(Walvis.heeftDeel(w.inrichting(), "kisten") && Walvis.heeftDeel(w.inrichting(), "vlag"), "the whale wears its furniture");
        SimpleContainer kist = new SimpleContainer(Walvis.KIST_VAKKEN);
        kist.setItem(4, new ItemStack(Items.DIAMOND, 3));
        Walvis.schrijf(gelovige, "Kist1", kist);
        helper.assertTrue(Walvis.kist(gelovige, "Kist1").get(4).is(Items.DIAMOND) && Walvis.kist(gelovige, "Kist1").get(4).getCount() == 3,
                "the chest keeps its items with the player");
        w.discard();
        Leegtewalvis.LIJN.wis(gelovige);
        GuhQuests.saved(gelovige).remove(Walvis.KEY);
        Walviszicht.zet(gelovige, false);
        weg(helper, gelovige, ander);
        helper.succeed();
    }
}
