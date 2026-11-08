package nl.juiced.guhs.feature.vadsrijk.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.hud.PartyFrames;
import nl.juiced.guhs.feature.vadsrijk.hud.UHudPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepConfig;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepRol;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpeler;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpelers;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepVulling;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * W-PARTY: saved parties, invite accept/decline, leader tools, nep-speler companions joining and leaving, dungeon queues filled by
 * nep-spelers after the timeout, and no filling when a player switched them off. Run: {@code runGameTestServer -Pgt=VadsrijkParty}.
 */
public class VadsrijkPartyGameTests {
    private static final String BATCH = "vadsrijk_w_party";

    /** A small queue for the tests (what w-grot's DungeonRij does, in memory). */
    static final class TestRij implements DungeonWachtrij {
        final Map<String, List<UUID>> rijen = new LinkedHashMap<>();
        final Set<UUID> nep = new HashSet<>();
        final Map<UUID, String> binnen = new HashMap<>();

        @Override
        public List<String> dungeons() {
            return List.of("test");
        }

        @Override
        public Component naam(String dungeon) {
            return Component.literal("Testgrot");
        }

        @Override
        @Nullable
        public Component partyInRij(ServerPlayer leider, List<ServerPlayer> leden, String dungeon) {
            List<UUID> r = rijen.computeIfAbsent(dungeon, d -> new ArrayList<>());
            for (ServerPlayer p : leden) {
                r.add(p.getUUID());
            }
            return null;
        }

        @Override
        @Nullable
        public String rijVan(UUID speler) {
            for (var e : rijen.entrySet()) {
                if (e.getValue().contains(speler)) {
                    return e.getKey();
                }
            }
            return null;
        }

        @Override
        public Map<String, List<UUID>> rijen() {
            return rijen;
        }

        @Override
        public int groepMax() {
            return 4;
        }

        @Override
        public boolean bezet(String dungeon) {
            return false;
        }

        @Override
        public boolean isNep(UUID id) {
            return nep.contains(id);
        }

        @Override
        public boolean voegNepToe(String dungeon, List<NepLid> l) {
            for (NepLid n : l) {
                nep.add(n.id());
                rijen.computeIfAbsent(dungeon, d -> new ArrayList<>()).add(n.id());
            }
            return true;
        }

        @Override
        @Nullable
        public String dungeonVan(UUID speler) {
            return binnen.get(speler);
        }
    }

    private static void opruimen(ServerPlayer... wie) {
        for (ServerPlayer p : wie) {
            Partijen.verlaat(p.getUUID());
            NepSpelers.zetStand(p.getUUID(), true);
        }
        NepSpelers.allemaalWeg(false);
        NepVulling.wisAlles();
        DungeonRijBrug.herstel();   // W-INT: back to the real dungeon queue (was null: left the server uncoupled after this test)
        Wachtrijen.TEST = false;
        NepSpelers.TEST = false;
        NepConfig.TEST = null;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void partyBewaardEnGeladen(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper), c = GuhMockPlayer.of(helper);
        MinecraftServer s = helper.getLevel().getServer();
        try {
            Partijen.nodigUit(a, b);
            Partijen.accepteer(b);
            Partijen.nodigUit(a, c);
            Partijen.accepteer(c);
            helper.assertTrue(Partijen.maakLeider(a, b.getUUID()).endsWith("nieuwe_leider"), "a hands the lead to b");
            CompoundTag tag = Partijen.naarTag();
            helper.assertTrue(tag.getListOrEmpty("partijen").size() == 1, "one party in the tag");
            helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.kern.party.PartijenData.snapshot().toString().equals(tag.toString()),
                    "the SavedData snapshot is refreshed at every change");
            Partijen.wisAlles();
            helper.assertTrue(Partijen.party(a) == null, "cleared (as after a restart)");
            Partijen.uitTag(tag);
            Partijen.Party p = Partijen.party(a);
            helper.assertTrue(p != null && p == Partijen.party(b) && p == Partijen.party(c), "the party is back with all three");
            helper.assertTrue(p.leider.equals(b.getUUID()), "b is still the leader");
            helper.assertTrue(b.getGameProfile().name().equals(p.namen.get(b.getUUID())), "the names are saved (for offline members)");
            helper.assertTrue(new ArrayList<>(p.leden).equals(List.of(a.getUUID(), b.getUUID(), c.getUUID())), "join order kept");
            // logging out keeps you in the party; a leader logging out hands the lead to an online member
            Partijen.uitgelogd(s, b.getUUID());
            helper.assertTrue(Partijen.party(b) == p && p.leider.equals(a.getUUID()), "b stays a member, a leads now");
        } finally {
            opruimen(a, b, c);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void uitnodigenAccepterenWeigeren(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper);
        try {
            Wachtrijen.TEST = true;
            helper.assertTrue(Partijen.nodigUit(a, b).endsWith("uitgenodigd"), "a invites b");
            helper.assertTrue(a.getUUID().equals(Partijen.uitnodigingVan(b.getUUID())), "the invitation is open");
            CompoundTag st = PartyDienst.staat(b);
            helper.assertTrue(st.getStringOr("uitnodiging", "").equals(a.getGameProfile().name()), "b's tab shows the invitation");
            helper.assertTrue(Partijen.weiger(b).endsWith("geweigerd"), "b declines");
            helper.assertTrue(Partijen.uitnodigingVan(b.getUUID()) == null && Partijen.party(a) == null && Partijen.party(b) == null, "declined: nothing");
            helper.assertTrue(Partijen.accepteer(b).endsWith("geen_uitnodiging"), "nothing left to accept");
            Partijen.nodigUit(a, b);
            helper.assertTrue(Partijen.accepteer(b).endsWith("erbij"), "b accepts the second invitation");
            helper.assertTrue(Partijen.zelfdeParty(a, b) && Partijen.party(a).leider.equals(a.getUUID()), "same party, a leads");
            CompoundTag sa = PartyDienst.staat(a);
            helper.assertTrue(sa.getListOrEmpty("leden").size() == 2 && sa.getBooleanOr("leider", false), "a's tab: two members, a is leader");
            boolean bGezien = false;
            for (var x : sa.getListOrEmpty("spelers")) {
                if (x instanceof CompoundTag t && t.getStringOr("id", "").equals(b.getUUID().toString())) {
                    bGezien = t.getBooleanOr("samen", false);
                }
            }
            helper.assertTrue(bGezien, "b is listed as 'samen' in a's player list");
        } finally {
            opruimen(a, b);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void leiderEnVerwijderen(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper), c = GuhMockPlayer.of(helper);
        try {
            Partijen.nodigUit(a, b);
            Partijen.accepteer(b);
            Partijen.nodigUit(a, c);
            Partijen.accepteer(c);
            helper.assertTrue(Partijen.verwijder(b, c.getUUID()).endsWith("niet_leider"), "b is no leader: cannot remove");
            helper.assertTrue(Partijen.maakLeider(b, a.getUUID()).endsWith("niet_leider"), "b cannot take the lead");
            helper.assertTrue(Partijen.maakLeider(a, b.getUUID()).endsWith("nieuwe_leider") && Partijen.party(a).leider.equals(b.getUUID()), "a -> b leader");
            helper.assertTrue(Partijen.verwijder(b, c.getUUID()).endsWith("verwijderd") && Partijen.party(c) == null, "b removes c");
            helper.assertTrue(Partijen.zelfdeParty(a, b), "a and b stay");
            Partijen.verlaat(a.getUUID());
            helper.assertTrue(Partijen.party(b) == null, "a left: a party of one ends");
        } finally {
            opruimen(a, b, c);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void nepSpelerMeeEnWeg(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper);
        MinecraftServer s = helper.getLevel().getServer();
        try {
            NepSpelers.TEST = true;
            NepConfig.TEST = true;
            Vec3 plek = a.position().add(2, 0, 0);
            NepSpeler n = NepSpelers.spawn(helper.getLevel(), plek, NepRol.HELER, 10, "xX_Testsloper_Xx", null);
            helper.assertTrue(n.wild() && NepSpelers.bekend(n), "a wild nep-speler");
            helper.assertTrue(NepSpelers.rekruteer(a, n).endsWith(".mee"), "Kom je mee? -> yes");
            Partijen.Party p = Partijen.party(a);
            helper.assertTrue(p != null && p.npcs.contains(n.getUUID()) && p.leden.size() == 1, "a's party: a + the nep-speler");
            long nu = s.overworld().getGameTime();
            helper.assertTrue(n.tot - nu >= NepSpelers.MEE_MIN && n.tot - nu <= NepSpelers.MEE_MAX, "for 20-30 minutes");
            List<UHudPayloads.Frame> f = PartyFrames.frames(a);
            helper.assertTrue(f.size() == 1 && f.get(0).naam().startsWith("[NPC] ") && f.get(0).spec().equals(NepRol.HELER.key()), "a frame with [NPC]");
            CompoundTag st = PartyDienst.staat(a);
            helper.assertTrue(st.getListOrEmpty("leden").stream().anyMatch(x -> x instanceof CompoundTag t && t.getBooleanOr("npc", false)), "the tab shows it");
            NepSpeler ander = NepSpelers.spawn(helper.getLevel(), plek, NepRol.TANK, 10, null, null);
            helper.assertTrue(NepSpelers.rekruteer(a, n).endsWith("bezet"), "already a companion: cannot recruit again");
            NepSpelers.zetStand(a.getUUID(), false);
            helper.assertTrue(NepSpelers.rekruteer(a, ander).endsWith("zelf_uit"), "setting off: cannot recruit");
            helper.assertTrue(PartyFrames.frames(a).isEmpty(), "setting off: no nep frames");
            NepSpelers.zetStand(a.getUUID(), true);
            // time up: a goodbye and gone, and a's one-man party ends
            NepSpelers.onderhoud(s, n.tot + 1);
            helper.assertTrue(!NepSpelers.bekend(n) && n.isRemoved(), "time up: gone");
            helper.assertTrue(Partijen.party(a) == null, "the party of a + nep ended");
            // the leader can send one away too
            NepSpelers.rekruteer(a, ander);
            helper.assertTrue(Partijen.verwijder(a, ander.getUUID()).endsWith("verwijderd") && !NepSpelers.bekend(ander), "removed by the leader: gone");
        } finally {
            opruimen(a);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void rijGevuldNaWachten(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper);
        MinecraftServer s = helper.getLevel().getServer();
        try {
            NepSpelers.TEST = true;
            NepConfig.TEST = true;
            TestRij rij = new TestRij();
            Wachtrijen.DUNGEON = rij;
            Wachtrijen.TEST = true;
            helper.assertTrue(Wachtrijen.dungeon(a, "test") == null && "test".equals(rij.rijVan(a.getUUID())), "the leader (solo) queues");
            long t0 = 1000;
            helper.assertTrue(NepVulling.tick(s, t0, false) == 0, "just queued: no filling");
            helper.assertTrue(NepVulling.tick(s, t0 + NepVulling.VUL_TICKS - 1, false) == 0, "59 s: still waiting for real players");
            int n = NepVulling.tick(s, t0 + NepVulling.VUL_TICKS, false);
            helper.assertTrue(n == 3 && rij.rijen.get("test").size() == 4, "after 1 minute: 3 nep-spelers fill the queue: " + n);
            helper.assertTrue(NepVulling.tick(s, t0 + NepVulling.VUL_TICKS + 20, false) == 0, "full: no more");
            Set<NepRol> rollen = new HashSet<>();
            List<UUID> nepIds = new ArrayList<>();
            for (UUID id : rij.rijen.get("test")) {
                if (NepVulling.lid(id) != null) {
                    rollen.add(NepVulling.lid(id).rol());
                    nepIds.add(id);
                }
            }
            String k = nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(a).klasse;
            for (NepRol r : NepRol.values()) {
                if (r.klasse.equals(k)) {
                    rollen.add(r);   // (a's own class counts as that role)
                }
            }
            helper.assertTrue(rollen.contains(NepRol.TANK) && rollen.contains(NepRol.HELER), "a tank and a healer first (with a's class): " + rollen);
            // the group goes in: they appear as companions of a; out of the dungeon for 10 s: they leave
            rij.binnen.put(a.getUUID(), "test");
            List<NepSpeler> mee = NepVulling.binnen("test", List.of(a), nepIds);
            helper.assertTrue(mee.size() == 3 && mee.stream().allMatch(x -> a.getUUID().equals(x.eigenaar) && "test".equals(x.dungeon)), "3 companions with a");
            helper.assertTrue(PartyFrames.frames(a).size() == 3, "they are in a's frames");
            helper.assertTrue(Partijen.party(a) == null, "fillers do not join a's party");
            NepSpelers.onderhoud(s, t0 + 2000);
            helper.assertTrue(mee.stream().allMatch(NepSpelers::bekend), "a is inside: they stay");
            rij.binnen.remove(a.getUUID());
            for (int i = 0; i < 10; i++) {
                NepSpelers.onderhoud(s, t0 + 2000 + i * 20);
            }
            helper.assertTrue(mee.stream().noneMatch(NepSpelers::bekend), "a left the dungeon: gg, they are gone");
        } finally {
            opruimen(a);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void instellingUitGeenVulling(GameTestHelper helper) {
        ServerPlayer a = GuhMockPlayer.of(helper), b = GuhMockPlayer.of(helper);
        MinecraftServer s = helper.getLevel().getServer();
        try {
            NepSpelers.TEST = true;
            NepConfig.TEST = true;
            TestRij rij = new TestRij();
            Wachtrijen.DUNGEON = rij;
            rij.rijen.put("test", new ArrayList<>(List.of(a.getUUID(), b.getUUID())));
            NepSpelers.zetStand(b.getUUID(), false);
            NepVulling.tick(s, 0, false);
            helper.assertTrue(NepVulling.tick(s, NepVulling.VUL_TICKS * 3L, false) == 0 && rij.rijen.get("test").size() == 2,
                    "b has nep-spelers off: never fills a queue b is in");
            NepSpelers.zetStand(b.getUUID(), true);
            NepConfig.TEST = false;
            helper.assertTrue(NepVulling.tick(s, NepVulling.VUL_TICKS * 4L, false) == 0, "server off: no filling either");
            NepConfig.TEST = true;
            helper.assertTrue(NepVulling.tick(s, NepVulling.VUL_TICKS * 5L, false) == 2, "both on, server on: 2 fill the 2 empty spots");
        } finally {
            opruimen(a, b);
        }
        helper.succeed();
    }
}
