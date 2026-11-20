package nl.juiced.guhs.feature.vadsrijk.zone3.wereld;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPayloads;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcVolg;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.npc.ViaRoutes;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.10 tracker "via" routes (npc/ViaRoutes, Z3Liften.viaRoutes). Ordermeester Wasja sits in the Nestkapel under the ring deck,
 * reached only by the kapellift:
 * <ul>
 *   <li>on the deck (and anywhere else outside), the step points at the kapellift's top stop with the lift phrase;</li>
 *   <li>inside the Nestkapel, it points at Wasja again;</li>
 *   <li>the same through the real tracker ({@link NpcVolg#bouw}, a pinned quest handed in at Wasja);</li>
 *   <li>the Raattrossen route counts only from the deck (a branch path walks in too).</li>
 * </ul>
 * The mock player is put on each spot explicitly (a random gametest spot could be routed via the airship) and back afterwards.
 */
public class VadsrijkZ3ViaGameTests {
    private static final String B = "vadsrijk_z3_via";
    private static final String KAPEL = "vadsrijk.npc.stap.via.z3_nestkapel";

    private static boolean sleutel(Component c, String key) {
        return c.getContents() instanceof TranslatableContents t && t.getKey().equals(key);
    }

    private static void zetKlaar(ServerPlayer p, String id) {
        Quest q = Quests.van(id);
        Questlog.neemAan(p, q);
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putInt("s", Questlog.KLAAR);
        ((net.minecraft.nbt.CompoundTag) Questlog.data(p).get("q")).put(q.id(), t);
    }

    /** The step of quest q for p standing at (x, y, z) (back on its own spot afterwards). */
    private static QuestStap stapOp(ServerPlayer p, Quest q, double x, double y, double z) {
        double ox = p.getX(), oy = p.getY(), oz = p.getZ();
        p.setPos(x, y, z);
        try {
            return QuestStap.van(p, q);
        } finally {
            p.setPos(ox, oy, oz);
        }
    }

    @GuhTest(template = "empty", batch = B)
    public static void kapelliftEerst(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        try {
            Z3WereldPlekken.Lift lift = Z3Liften.lift("lift_kapel");
            BlockPos wasja = QuestStap.npcPos(p, "z3_wasja");
            helper.assertTrue(lift != null && wasja != null && ViaRoutes.van("z3_nestkapel") != null, "the kapellift, Wasja and the via entry exist");
            ViaRoutes.Via via = ViaRoutes.van("z3_nestkapel");
            helper.assertTrue(via.gebied().in(wasja) && !via.gebied().in(lift.boven()) && via.binnen().in(lift.onder().getX() + 0.5,
                    lift.onder().getY(), lift.onder().getZ() + 0.5), "Wasja inside, the top stop outside, the bottom stop counts as inside");
            zetKlaar(p, "z3_hq01_boom");
            Quest hq02 = Quests.van("z3_hq02_bezoek");
            // 1. on the ring deck, at the Bloesemplein (south side): first the kapellift
            BlockPos plein = Z3WereldPlekken.plek("rijpad_honingplein");
            QuestStap s = stapOp(p, hq02, plein.getX() + 0.5, plein.getY(), plein.getZ() + 0.5);
            helper.assertTrue(lift.boven().equals(s.doel()) && sleutel(s.waar(), KAPEL), "on the deck: the kapellift first: " + s.doel() + " " + s.waar());
            helper.assertTrue(sleutel(s.wat(), QuestStap.P + "level") || sleutel(s.wat(), QuestStap.P + "praat"), "the 'what' stays (talk to Wasja): " + s.wat());
            // 2. right next to the lift on the deck: still the lift (not through the floor)
            s = stapOp(p, hq02, lift.boven().getX() + 0.5, lift.boven().getY(), lift.boven().getZ() + 0.5);
            helper.assertTrue(lift.boven().equals(s.doel()), "standing at the top stop: the lift itself: " + s.doel());
            // 3. down at the foot of the tree (Mosvoet): the lift too (the deck is reached by walking: the riding spiral)
            BlockPos voet = Z3WereldPlekken.plek("honinglift_onder");
            s = stapOp(p, hq02, voet.getX() + 0.5, voet.getY(), voet.getZ() + 0.5);
            helper.assertTrue(lift.boven().equals(s.doel()), "from the ground: the kapellift: " + s.doel());
            // 4. inside the Nestkapel (and at the bottom stop): Wasja herself
            BlockPos binnen = Z3WereldPlekken.plek("gebouw.nestkapel.binnen");
            s = stapOp(p, hq02, binnen.getX() + 0.5, binnen.getY(), binnen.getZ() + 0.5);
            helper.assertTrue(wasja.equals(s.doel()) && !sleutel(s.waar(), KAPEL), "inside: Wasja (her own where-text): " + s.doel() + " " + s.waar());
            s = stapOp(p, hq02, lift.onder().getX() + 0.5, lift.onder().getY(), lift.onder().getZ() + 0.5);
            helper.assertTrue(wasja.equals(s.doel()), "off the lift at the bottom: Wasja: " + s.doel());
            // 5. through the real tracker: a pinned quest handed in at Wasja (z3_hq15: talk to Raatje, back to Wasja)
            Questlog.wisAlles(p);
            Quest hq15 = Quests.van("z3_hq15_hoogvads");
            Questlog.neemAan(p, hq15);
            ((net.minecraft.nbt.CompoundTag) ((net.minecraft.nbt.CompoundTag) Questlog.data(p).get("q")).get(hq15.id())).putIntArray("v", new int[] {1});
            helper.assertTrue(Questlog.allesKlaar(p, hq15), "hq15: only the hand-in at Wasja is left");
            Questlog.data(p).putString("volg_pin", hq15.id());
            double ox = p.getX(), oy = p.getY(), oz = p.getZ();
            p.setPos(plein.getX() + 0.5, plein.getY(), plein.getZ() + 0.5);
            NpcPayloads.Volg op = NpcVolg.bouw(p);
            p.setPos(binnen.getX() + 0.5, binnen.getY(), binnen.getZ() + 0.5);
            NpcPayloads.Volg in = NpcVolg.bouw(p);
            p.setPos(ox, oy, oz);
            Optional<NpcPayloads.VolgRegel> rOp = op.regels().stream().filter(r -> r.quest().equals(hq15.id())).findFirst();
            Optional<NpcPayloads.VolgRegel> rIn = in.regels().stream().filter(r -> r.quest().equals(hq15.id())).findFirst();
            helper.assertTrue(rOp.isPresent() && rOp.get().doel().equals(Optional.of(lift.boven())) && sleutel(rOp.get().waar(), KAPEL),
                    "tracker on the deck: the kapellift: " + rOp);
            helper.assertTrue(rIn.isPresent() && rIn.get().doel().equals(Optional.of(wasja)), "tracker in the Nestkapel: Wasja: " + rIn);
        } finally {
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = B)
    public static void raattrosAlleenVanafHetDek(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.wisAlles(p);
        try {
            // z3_hq06: Raatje done, now the Raattrossen (a cell under the east side of the deck)
            Quest hq06 = Quests.van("z3_hq06_voetjes");
            Questlog.neemAan(p, hq06);
            ((net.minecraft.nbt.CompoundTag) ((net.minecraft.nbt.CompoundTag) Questlog.data(p).get("q")).get(hq06.id())).putIntArray("v", new int[] {1, 0});
            Z3WereldPlekken.Lift lift = Z3Liften.lift("lift_raattros");
            BlockPos plein = Z3WereldPlekken.plek("rijpad_honingplein");
            QuestStap s = stapOp(p, hq06, plein.getX() + 0.5, plein.getY(), plein.getZ() + 0.5);
            helper.assertTrue(lift.boven().equals(s.doel()) && sleutel(s.waar(), "vadsrijk.npc.stap.via.z3_raattros"),
                    "on the deck: the raattroslift first: " + s.doel());
            BlockPos binnen = Z3WereldPlekken.plek("gebouw.raattros.binnen");
            s = stapOp(p, hq06, binnen.getX() + 0.5, binnen.getY(), binnen.getZ() + 0.5);
            helper.assertTrue(s.doel() != null && !lift.boven().equals(s.doel()) && s.doel().distSqr(binnen) < 9, "inside: the Raattrossen: " + s.doel());
            // on the branch path below the deck (y 160, it walks into the cell): no lift detour
            s = stapOp(p, hq06, 1483.5, 160, -554.5);
            helper.assertTrue(s.doel() != null && !lift.boven().equals(s.doel()), "on the branch: straight on: " + s.doel());
        } finally {
            Questlog.wisAlles(p);
        }
        helper.succeed();
    }
}
