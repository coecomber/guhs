package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.zone4.verhaal.Z4Haken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * (Luc, live, 1.10.28) "De adem van Moeder Wolkwal" (z5_s05_adem): after listening at the middle blowhole the quest marker sent
 * him some 750 blocks away. Hoogvads floats above the east end of zone 4's tile box (x 2432..2559), and zone 4's tracker route
 * looked at x/z only: on the whale's head you stood "in zone 4", so every target in the rest of the city (x >= 2560) was "outside
 * zone 4" and became Windvaan's airship in Lampionhaven.
 * <ul>
 *   <li>{@link #elkeStapBlijftInHoogvads}: every step of every zone-5 quest (go to the giver, each objective, the hand-in), seen
 *       from every quest spot and NPC spot of Hoogvads, points to a place in Hoogvads;</li>
 *   <li>{@link #ademVanMoederWolkwal}: Luc's saved state (middle + south heard, north open) at the middle blowhole points at the
 *       north blowhole, standing there counts it, and from the coast below the airship hop still comes first.</li>
 * </ul>
 * The routes only run for players in the Vadsrijk and the gametest server has no such level: the mock player is listed in
 * {@link QuestStap#TEST_IN_RIJK} for the test, and moved by coordinates only.
 */
public class Z5TrackerGameTests {
    private static final String B = "vadsrijk_z5_tracker";
    private static final String ADEM = "z5_s05_adem";

    private static boolean hoogvads(BlockPos b) {
        return Z5VerhaalPlekken.zone5(b.getX(), b.getY(), b.getZ());
    }

    private static boolean sleutel(Component c, String key) {
        return c.getContents() instanceof TranslatableContents t && t.getKey().equals(key);
    }

    /** Sets a quest's saved state: status + the counted value per objective (null = not taken). */
    private static void zet(ServerPlayer p, Quest q, int status, int[] v) {
        CompoundTag qs = Questlog.data(p).getCompoundOrEmpty("q");
        Questlog.data(p).put("q", qs);
        CompoundTag t = new CompoundTag();
        t.putInt("s", status);
        if (v != null) {
            t.putIntArray("v", v);
        }
        qs.put(q.id(), t);
    }

    /** A player who is up in Hoogvads for real: zone 1's boss done (the airship hops), the arrival film seen. */
    private static ServerPlayer speler(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        zet(p, Quests.van("hq05_klodderreus"), Questlog.KLAAR, null);
        Spelers.data(p).gezien.add("npc:z5_aankomst");
        QuestStap.TEST_IN_RIJK.add(p.getUUID());
        return p;
    }

    private static void terug(ServerPlayer p) {
        Questlog.wisAlles(p);
        Spelers.data(p).gezien.remove("npc:z5_aankomst");
        QuestStap.TEST_IN_RIJK.remove(p.getUUID());
    }

    private static QuestStap stapOp(ServerPlayer p, Quest q, double x, double y, double z) {
        double ox = p.getX(), oy = p.getY(), oz = p.getZ();
        p.setPos(x, y, z);
        try {
            return QuestStap.van(p, q);
        } finally {
            p.setPos(ox, oy, oz);
        }
    }

    private static QuestStap stapOp(ServerPlayer p, Quest q, BlockPos b) {
        return stapOp(p, q, b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
    }

    /** The zone test itself (no dimension needed): the whale is not the coast, the sea below it is. */
    @GuhTest(template = "empty", batch = B)
    public static void walvisIsNietDeKust(GameTestHelper helper) {
        BlockPos midden = Z5VerhaalPlekken.SPUITGAT, noord = Z5VerhaalPlekken.poi("z5_spuitgat_noord"), zuid = Z5VerhaalPlekken.poi("z5_spuitgat_zuid");
        helper.assertTrue(midden.getX() < 2560 && noord.getX() >= 2560 && zuid.getX() >= 2560,
                "self-check: the middle blowhole lies in the zone-4/5 overlap (x < 2560), the other two east of it: " + midden + " " + noord + " " + zuid);
        for (BlockPos b : List.of(midden, noord, zuid, Z5VerhaalPlekken.BALKON_OOG, Z5VerhaalPlekken.OOGGAARDE, Z5VerhaalPlekken.AANKOMST)) {
            helper.assertTrue(hoogvads(b) && !Z4Haken.opDeKust(b.getX() + 0.5, b.getY(), b.getZ() + 0.5), "in Hoogvads, not on the coast: " + b);
        }
        helper.assertTrue(Z4Haken.opDeKust(midden.getX() + 0.5, 66, midden.getZ() + 0.5), "the sea under the whale's head is the coast");
        helper.assertTrue(Z4Haken.opDeKust(1800.5, 66, -465.5) && Z4Haken.opDeKust(1800.5, 230, -465.5), "Lampionhaven is the coast at any height");
        helper.assertTrue(!Z4Haken.opDeKust(2700.5, 66, -519.5), "east of zone 4's box: not the coast");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 400)
    public static void elkeStapBlijftInHoogvads(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            helper.assertTrue(QuestStap.inRijk(p), "the mock player counts as in the Vadsrijk");
            // where a player can stand in Hoogvads: every quest spot and every NPC spot up there
            Map<String, BlockPos> sta = new LinkedHashMap<>();
            Z5VerhaalPlekken.POI.forEach((id, b) -> {
                if (hoogvads(b)) {
                    sta.put("poi " + id, b);
                }
            });
            Z5VerhaalPlekken.NPC.forEach((id, b) -> sta.put("npc " + id, b));
            helper.assertTrue(sta.size() > 60 && sta.values().stream().anyMatch(b -> b.getX() < 2560) && sta.values().stream().anyMatch(b -> b.getX() >= 2560),
                    "stand spots on both sides of x 2560: " + sta.size());
            List<String> fout = new ArrayList<>();
            int stappen = 0;
            for (Quest q : Quests.alle().values()) {
                if (!q.id().startsWith("z5_")) {
                    continue;
                }
                // -1 = not taken (go to the giver), 0..n-1 = that objective open (the earlier ones done), n = hand in
                for (int i = -1; i <= q.doelen().size(); i++) {
                    if (i < 0) {
                        Questlog.vergeet(p, q.id());
                    } else {
                        int[] v = new int[q.doelen().size()];
                        for (int j = 0; j < i; j++) {
                            v[j] = q.doelen().get(j).n();
                        }
                        zet(p, q, Questlog.ACTIEF, v);
                    }
                    stappen++;
                    for (Map.Entry<String, BlockPos> e : sta.entrySet()) {
                        QuestStap s = stapOp(p, q, e.getValue());
                        if (s.doel() != null && !hoogvads(s.doel())) {
                            if (fout.size() < 12) {
                                fout.add(q.id() + " step " + i + " from " + e.getKey() + " " + e.getValue().toShortString() + " -> " + s.doel().toShortString()
                                        + " (" + Math.round(Math.sqrt(s.doel().distSqr(e.getValue()))) + " blocks)");
                            } else {
                                fout.add("");
                            }
                        }
                    }
                    Questlog.vergeet(p, q.id());
                }
            }
            helper.assertTrue(stappen > 60, "walked the steps of the zone-5 quests: " + stappen);
            helper.assertTrue(fout.isEmpty(), fout.size() + " tracker targets outside Hoogvads for a player in Hoogvads: " + fout.subList(0, Math.min(12, fout.size())));
        } finally {
            terug(p);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B, timeoutTicks = 400)
    public static void ademVanMoederWolkwal(GameTestHelper helper) {
        ServerPlayer p = speler(helper);
        try {
            Quest q = Quests.van(ADEM);
            helper.assertTrue(q != null && q.doelen().size() == 3 && q.doelen().get(0).doel().equals("poi:z5_spuitgat_noord")
                    && q.doelen().get(1).doel().equals("poi:z5_spuitgat") && q.doelen().get(2).doel().equals("poi:z5_spuitgat_zuid"),
                    "self-check: three blowholes, north / middle / south");
            BlockPos noord = Z5VerhaalPlekken.poi("z5_spuitgat_noord"), midden = Z5VerhaalPlekken.SPUITGAT, zuid = Z5VerhaalPlekken.poi("z5_spuitgat_zuid");
            for (BlockPos b : List.of(noord, zuid)) {
                helper.assertTrue(b.distSqr(midden) < 40 * 40, "the three blowholes lie together: " + b + " / " + midden);
            }
            // Luc's save: middle and south heard, north still open
            zet(p, q, Questlog.ACTIEF, new int[] {0, 1, 1});
            for (BlockPos sta : List.of(midden, zuid, Z5VerhaalPlekken.BALKON_OOG, Z5VerhaalPlekken.NPC.get("z5_professor"))) {
                QuestStap s = stapOp(p, q, sta);
                helper.assertTrue(noord.equals(s.doel()) && sleutel(s.waar(), QuestStap.PLEK + "poi.z5_spuitgat_noord"),
                        "from " + sta.toShortString() + ": the north blowhole (" + noord.toShortString() + "), was the airship in Lampionhaven: " + s.doel() + " " + s.waar());
            }
            // from the coast below (Lampionhaven, the sea under the whale's head) the hop still comes first
            for (Vec3 kust : List.of(new Vec3(1800.5, 66, -465.5), new Vec3(midden.getX() + 0.5, 66, midden.getZ() + 0.5))) {
                QuestStap s = stapOp(p, q, kust.x, kust.y, kust.z);
                helper.assertTrue(s.doel() != null && !hoogvads(s.doel()), "from the coast at " + kust + ": first an airship, not straight up: " + s.doel());
            }
            // standing against the west wall of the house on the north blowhole (checked on the live world: 2558, 224, -495) counts it
            double ox = p.getX(), oy = p.getY(), oz = p.getZ();
            p.setPos(noord.getX() - 5 + 0.5, noord.getY(), noord.getZ() + 0.5);
            Z5Haken.plekken(p, helper.getLevel());
            QuestStap inl = QuestStap.van(p, q);
            p.setPos(ox, oy, oz);
            helper.assertTrue(Questlog.allesKlaar(p, q), "the north blowhole counted: all three heard");
            helper.assertTrue(inl.doel() != null && hoogvads(inl.doel()) && sleutel(inl.wat(), QuestStap.P + "inleveren"),
                    "then: hand in to Professor Brilsma, in Hoogvads: " + inl.doel() + " " + inl.wat());
        } finally {
            terug(p);
        }
        helper.succeed();
    }
}
