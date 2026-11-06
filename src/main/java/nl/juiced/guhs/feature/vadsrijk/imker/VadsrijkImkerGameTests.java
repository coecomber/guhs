package nl.juiced.guhs.feature.vadsrijk.imker;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-IMKER: De Mika-imker. The room's objects (hive + 3 smoke pots), phase 1 (swarm -> smoke puts it to sleep, the pot's
 * cooldown, a lob leaves a sticky puddle), phase 2 (immune while pouring; the hive's stream at his perch stings him: stunned,
 * hittable, the meter drops; the stream at a player hurts), phase 3 (he comes down, the big bomb's stack circle), and the
 * story gate (no quest = no fight; the portrait resolver). Run: {@code runGameTestServer -Pgt=VadsrijkImker}.
 */
public class VadsrijkImkerGameTests {
    private static final String BATCH = "vadsrijk_imker";

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.snapTo(waar.x, waar.y, waar.z);
        Spelers.data(p).vads = Spelers.maxVads(p);
        return p;
    }

    private static void vloer(ServerLevel sl, BlockPos c, int r) {
        for (int cx = (c.getX() - r - 8) >> 4; cx <= (c.getX() + r + 8) >> 4; cx++) {
            for (int cz = (c.getZ() - r - 8) >> 4; cz <= (c.getZ() + r + 8) >> 4; cz++) {
                sl.setChunkForced(cx, cz, true);
            }
        }
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                sl.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
            }
        }
    }

    private static MikaImker zet(ServerLevel sl, BlockPos c) {
        MikaImker m = ImkerRegistry.MIKA.get().create(sl, EntitySpawnReason.COMMAND);
        m.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        m.stelIn(ImkerVerhaal.LV, null, false, Set.of());
        sl.addFreshEntity(m);
        return m;
    }

    // ---- phase 1: the hive, the pots, the swarm, the puddle ----------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_fase1", timeoutTicks = 200)
    public static void fase1RookEnZwerm(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 14);
        MikaImker m = zet(sl, c);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 6));
        m.testFilter = q -> q == p;
        m.testStil = true;
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(m.korf() != null && !m.korf().isRemoved(), "the queen's hive stands in the room");
            helper.assertTrue(m.potten().size() == 3, "three smoke pots (" + m.potten().size() + ")");
            helper.assertTrue(m.strijders().contains(p) && m.aanvallenGedaan >= 1, "he fights the player (" + m.laatsteAanval + ")");
            // the hive turns on right-click
            int r0 = m.richting();
            m.doelGebruikt(m.korf(), p);
            helper.assertTrue(m.richting() == (r0 + 1) % 6, "right-click turns the hive a sixth (" + r0 + " -> " + m.richting() + ")");
            // a smoke pot: a cloud, then it must cool down
            Z3Doel pot = m.potten().get(0);
            m.doelGebruikt(pot, p);
            helper.assertTrue(m.rook.size() == 1, "the pot makes a smoke cloud");
            m.doelGebruikt(pot, p);
            helper.assertTrue(m.rook.size() == 1, "the pot must cool down first");
            // a swarm flying into the smoke falls asleep
            m.zwermen.clear();
            m.zwerm(sl, p);
            helper.assertTrue(m.zwermen.size() == 1, "a swarm chases the player");
            m.zwermen.get(0).plek = m.rook.get(0).plek().add(0, 1, 0);
        });
        helper.runAfterDelay(34, () -> {
            helper.assertTrue(m.zwermen.isEmpty() && m.gesust == 1, "the swarm fell asleep in the smoke (" + m.zwermen.size() + ", " + m.gesust + ")");
            // a lob leaves a sticky puddle
            m.klodder(sl, java.util.List.of(p));
        });
        helper.runAfterDelay(34 + 32, () -> {
            helper.assertTrue(!m.plassen.isEmpty(), "the lob left a grey honey puddle");
            p.snapTo(m.plassen.get(0).plek().x, m.plassen.get(0).plek().y, m.plassen.get(0).plek().z);
        });
        helper.runAfterDelay(34 + 32 + 25, () -> {
            helper.assertTrue(Statussen.heeft(p, Status.PLAKKERIG), "standing in the puddle makes you sticky");
            m.discard();
            helper.assertTrue(m.korf().isRemoved() && m.potten().stream().allMatch(Z3Doel::isRemoved), "a reset takes the hive and pots along");
            helper.succeed();
        });
    }

    // ---- phase 2: pouring, the stream, the sting ----------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_fase2", timeoutTicks = 260)
    public static void fase2KorfSteektHem(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        vloer(sl, c, 14);
        MikaImker m = zet(sl, c);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 7));
        m.testFilter = q -> q == p;
        m.testStil = true;
        helper.runAfterDelay(20, () -> {
            m.krijgSchade(m.vads() - m.maxVads() * 0.65, p);
            helper.assertTrue(m.fase() == 2 && m.giet() && m.stok >= 0, "phase 2 at 66 %: he pours on a perch (" + m.fase() + ")");
        });
        helper.runAfterDelay(50, () -> {
            double v = m.vads();
            m.krijgSchade(1000, p);
            helper.assertTrue(m.vads() == v, "he can't be hit while he pours");
            // the stream at the player hurts
            p.snapTo(m.korfPlek().x, m.korfPlek().y, m.korfPlek().z);
            Vec3 d = MikaImker.dir(m.richting());
            Vec3 in = m.korfPlek().add(d.scale(5));
            p.snapTo(in.x, in.y, in.z);
            double vp = Spelers.data(p).vads;
            m.zetRichting(m.richting());
            int voor = m.mechanieken;
            m.korfVuur(sl, 2);
            helper.runAfterDelay(4, () -> helper.assertTrue(m.mechanieken > voor || Spelers.data(p).vads < vp, "the stream hits a player in its lane"));
        });
        helper.runAfterDelay(70, () -> {
            // turn the hive at his perch: the stream stings him
            p.snapTo(m.korfPlek().x + 0.5, m.korfPlek().y, m.korfPlek().z + 9);
            m.zetRichting(MikaImker.STOKKEN[m.stok]);
            m.snapTo(m.stokPlek(m.stok).x, m.stokPlek(m.stok).y, m.stokPlek(m.stok).z, 0, 0);
            m.grauw = MikaImker.GRAUW_VOL / 2;
            m.korfVuur(sl, 2);
        });
        helper.runAfterDelay(76, () -> {
            helper.assertTrue(m.gestoken == 1, "the stream stung him (" + m.gestoken + ")");
            helper.assertTrue(Statussen.heeft(m, Status.VERDOOFD) && !m.giet(), "he is stunned and off his perch");
            helper.assertTrue(m.grauw < MikaImker.GRAUW_VOL / 2, "the grey meter dropped (" + m.grauw + ")");
            double v = m.vads();
            m.krijgSchade(100, p);
            helper.assertTrue(m.vads() < v, "now he can be hit");
            // phase 3: he comes down and the big bomb is a yellow stack circle at the hive
            m.krijgSchade(m.vads() - m.maxVads() * 0.3, p);
            helper.assertTrue(m.fase() == 3 && !m.giet(), "phase 3 at 33 %: he stops pouring (" + m.fase() + ")");
            m.groteBom(sl);
            helper.assertTrue(m.bommen == 1 && m.takenOpen() >= 1, "the big bomb waits on its circle");
            m.discard();
            helper.succeed();
        });
    }

    // ---- the story gate and the portrait ----------------------------------------------------------------------------------

    @GuhTest(template = "empty", batch = BATCH + "_verhaal", timeoutTicks = 60)
    public static void verhaalPoortEnPortret(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Questlog.vergeet(p, ImkerVerhaal.QUEST);
        Spelers.data(p).gezien.remove(ImkerVerhaal.GEZIEN_INTRO);
        helper.assertTrue(!ImkerVerhaal.magVechten(p) && !ImkerVerhaal.introNodig(p), "no quest: no intro, no fight");
        ImkerVerhaal.klaarzetten(p);
        helper.assertTrue(Questlog.actief(p, ImkerVerhaal.QUEST) && Questlog.vlag(p, ImkerVerhaal.OPEN), "klaarzetten: the quest runs, the hatch is open");
        helper.assertTrue(ImkerVerhaal.introNodig(p) && !ImkerVerhaal.magVechten(p), "the intro first, then the fight");
        ImkerVerhaal.introGelezen(p);
        helper.assertTrue(!ImkerVerhaal.introNodig(p) && ImkerVerhaal.magVechten(p), "after the intro he may wake");
        CompoundTag t = ImkerVerhaal.portret(ImkerVerhaal.NAAM);
        helper.assertTrue(t != null && t.getStringOr("Type", "").equals("guhs:vr_z3_imker_mika"), "the portrait is the Mika-imker himself");
        helper.assertTrue(ImkerVerhaal.portret(ImkerVerhaal.NPC_NAAM) != null, "also under z3-npc's name key");
        helper.assertTrue(ImkerVerhaal.portret("vadsrijk.npc.verteller") == null, "the narrator stays the book");
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren.van("zoempje") != null, "the pet Zoempje exists");
        Questlog.vergeet(p, ImkerVerhaal.QUEST);
        ImkerVerhaal.vergeetScenes(p);
        helper.succeed();
    }
}
