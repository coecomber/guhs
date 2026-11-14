package nl.juiced.guhs.feature.vadsrijk.bazenboek;

import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.bazen.koning.KoningKlodder;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasStats;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.vint.RijdierBuit;
import nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1.10.3 "Bazen" tab: a kill records the statistics (kills, damage, the chest as drops), the tab lists only open zones and carries
 * the pity numbers, the Boormeester's access follows the clues, an arena on respawn says when it is back.
 * Run: {@code runGameTestServer -Pgt=BazenboekGameTests}.
 */
public class BazenboekGameTests {
    private static final String BATCH = "vadsrijk_bazenboek";

    @GuhTest(template = "empty", batch = BATCH)
    public static void statsOpKill(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        ServerPlayer p = GuhMockPlayer.of(helper);
        KoningKlodder k = BazenRegistry.KONING_KLODDER.get().create(sl, EntitySpawnReason.COMMAND);
        BlockPos b = helper.absolutePos(new BlockPos(1, 1, 1));
        k.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0, 0);
        k.stelIn(8, Element.WATER, false, Set.of());
        sl.addFreshEntity(k);
        k.bijdragen().schade(p.getUUID(), 970);
        k.bijdragen().heal(p.getUUID(), 40);
        k.verslagen(p);
        CompoundTag s = BaasStats.alle(p).getCompoundOrEmpty("koning_klodder");
        helper.assertTrue(s.getIntOr("kills", 0) == 1, "one kill counted under the boss id: " + BaasStats.alle(p));
        helper.assertTrue(Math.abs(s.getDoubleOr("schade", 0) - 970) < 0.01 && Math.abs(s.getDoubleOr("heal", 0) - 40) < 0.01, "damage + healing: " + s);
        helper.assertTrue(s.getIntOr("lv", 0) == 8 && s.contains("eerste"), "level + first-kill date: " + s);
        int stukken = 0;
        for (String z : s.getCompoundOrEmpty("r").keySet()) {
            stukken += s.getCompoundOrEmpty("r").getIntOr(z, 0);
        }
        helper.assertTrue(stukken + s.getIntOr("items", 0) > 0 || s.getLongOr("munten", 0) > 0, "the boss chest counted as its drops: " + s);
        // the tab shows it beaten, with these numbers
        CompoundTag e = baas(Bazenboek.scherm(p), "koning_klodder");
        helper.assertTrue(e != null && e.getBooleanOr("bekend", false) && e.getIntOr("kills", 0) == 1, "in the tab: beaten once: " + e);
        // a second kill of the same boss: 2
        BaasStats.registreer(p, "koning_klodder", "guhs:vr_baas_koning_klodder", sl.getGameTime() + 5000, 61, 100, 0, 0, 8);
        helper.assertTrue(BaasStats.alle(p).getCompoundOrEmpty("koning_klodder").getIntOr("kills", 0) == 2, "two kills");
        // the three Sterrenbeelden in one clear count as one kill
        long t = sl.getGameTime();
        BaasStats.registreer(p, "z6raid_sterrenbeelden", "guhs:vr_z6raid_baas_ster_knuffel", t, 30, 10, 0, 0, 70);
        BaasStats.registreer(p, "z6raid_sterrenbeelden", "guhs:vr_z6raid_baas_ster_bultguh", t + 40, 31, 10, 0, 0, 70);
        BaasStats.registreer(p, "z6raid_sterrenbeelden", "guhs:vr_z6raid_baas_ster_pluis", t + 80, 32, 10, 0, 0, 70);
        CompoundTag st = BaasStats.alle(p).getCompoundOrEmpty("z6raid_sterrenbeelden");
        helper.assertTrue(st.getIntOr("kills", 0) == 1 && Math.abs(st.getDoubleOr("schade", 0) - 30) < 0.01, "three parts = one kill, damage summed: " + st);
        helper.assertTrue("koning_klodder".equals(BaasStats.idVan("guhs:vr_baas_koning_klodder")) && "grot_zwamgrot".equals(BaasStats.idVan("guhs:vr_grot_baas_zwamgrot")),
                "type -> boss id");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void alleenOpenZonesEnPech(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        ZonePoort.TEST_STRENG = true;
        try {
            Spelers.data(p).extra("vint_buit").putInt("opperzwam_zwamzwijntje", 7);   // (what /guhs vadsrijk loot pech sets)
            Spelers.data(p).extra(GrotKerker.DATA).putInt("zwamgrot.petpech", 9);
            CompoundTag d = Bazenboek.scherm(p);
            ListTag zl = d.getListOrEmpty("zones");
            helper.assertTrue(zl.size() >= 1 && zl.size() <= 2, "zone 1 + at most the next closed one: " + zl.size());
            CompoundTag z1 = zl.getCompoundOrEmpty(0);
            helper.assertTrue(z1.getIntOr("zone", 0) == 1 && z1.getBooleanOr("open", false) && !z1.getListOrEmpty("bazen").isEmpty(), "zone 1 open with bosses");
            if (zl.size() == 2) {
                CompoundTag z2 = zl.getCompoundOrEmpty(1);
                helper.assertTrue(!z2.getBooleanOr("open", true) && z2.getListOrEmpty("bazen").isEmpty() && !z2.getStringOr("eis", "").isEmpty(),
                        "the closed zone: only its requirement, no bosses (no spoilers)");
            }
            CompoundTag oz = baas(d, "opperzwam");
            helper.assertTrue(oz != null && !oz.getBooleanOr("bekend", true), "Opperzwam listed, unknown (???) until beaten");
            CompoundTag drop = oz.getListOrEmpty("zeldzaam").getCompoundOrEmpty(0);
            helper.assertTrue(drop.getIntOr("pech", 0) == 7 && drop.getIntOr("pity", 0) == RijdierBuit.PITY && !drop.getBooleanOr("heeft", true),
                    "the Zwamzwijntje: dry streak 7/15: " + drop);
            helper.assertTrue(drop.getStringOr("regel", "").contains("vadsrijk.vint.pech.kans"), "the chance line: " + drop.getStringOr("regel", ""));
            CompoundTag grot = baas(d, "grot_zwamgrot");
            boolean pet = false;
            ListTag gl = grot == null ? new ListTag() : grot.getListOrEmpty("zeldzaam");
            for (int i = 0; i < gl.size(); i++) {
                pet |= gl.getCompoundOrEmpty(i).getIntOr("pech", 0) == 9 && gl.getCompoundOrEmpty(i).getIntOr("pity", 0) == 25;
            }
            helper.assertTrue(pet, "the cave pet: 9/25 in the Zwamgrot boss: " + gl);
            helper.assertTrue(baas(d, "galmwolf") == null, "no zone-2 boss in the list");
            helper.succeed();
        } finally {
            ZonePoort.TEST_STRENG = false;
        }
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void boormeesterToegang(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        var regel = Bazenboek.REGELS.get("boormeester");
        helper.assertTrue(regel != null, "the Boormeester has a rule");
        Bazenboek.Toegang t = regel.apply(p);
        helper.assertTrue(!t.ja() && args(t, "vadsrijk.bazenboek.toegang.boormeester.slaapt")[1].equals(0), "asleep, 0 clues: " + t.reden().getString());
        Questlog.map(p, nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2Haken.AANWIJZINGEN).putBoolean("olievlek", true);
        Questlog.map(p, nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2Haken.AANWIJZINGEN).putBoolean("tweede", true);
        t = regel.apply(p);
        Object[] a = args(t, "vadsrijk.bazenboek.toegang.boormeester.slaapt");
        helper.assertTrue(!t.ja() && a[1].equals(2) && a[2].equals(nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2NpcPlekken.AANWIJZING.size()),
                "still asleep, 2 of the clues: " + t.reden().getString());
        Spelers.data(p).extra("z2bazen").putBoolean("boormeester_ontgrendeld", true);   // (what Boormeester.ontgrendel remembers)
        t = regel.apply(p);
        helper.assertTrue(t.ja() && t.reden().getContents() instanceof TranslatableContents tc && tc.getKey().endsWith("boormeester.wakker"),
                "awake after ontgrendel: right-click the machine");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void arenaOpRespawn(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        String id = "test:bazenboek";
        BlockPos c = helper.absolutePos(new BlockPos(2, 1, 2));
        Arenas.registreer(new Arena(id, c, 6, c, 900, c.above(3), "vadsrijk.kern.baas.klodderreus"), x -> null);
        try {
            Arenas.testRespawn(id, p.level().getGameTime() + 20L * 750);   // back in 12:30
            Bazenboek.Toegang t = Bazenboek.arenaToegang(p, id);
            helper.assertTrue(t != null && !t.ja(), "on respawn: not now");
            Object[] a = args(t, "vadsrijk.bazenboek.toegang.terug");
            helper.assertTrue("12:30".equals(a[0]) || "12:29".equals(a[0]), "back in 12:30: " + a[0]);
            Arenas.testRespawn(id, -2);
            helper.assertTrue(!Bazenboek.arenaToegang(p, id).ja(), "just beaten: back soon");
            Arenas.testRespawn(id, -1);
            helper.assertTrue(Bazenboek.arenaToegang(p, id) == null, "due again (no living boss known): no objection");
            helper.succeed();
        } finally {
            Arenas.afmelden(id);
        }
    }

    private static Object[] args(Bazenboek.Toegang t, String key) {
        if (t.reden().getContents() instanceof TranslatableContents tc && tc.getKey().equals(key)) {
            return tc.getArgs();
        }
        throw new AssertionError("expected " + key + ", got " + t.reden().getContents());
    }

    @Nullable
    static CompoundTag baas(CompoundTag scherm, String id) {
        ListTag zl = scherm.getListOrEmpty("zones");
        for (int i = 0; i < zl.size(); i++) {
            ListTag bl = zl.getCompoundOrEmpty(i).getListOrEmpty("bazen");
            for (int j = 0; j < bl.size(); j++) {
                if (id.equals(bl.getCompoundOrEmpty(j).getStringOr("id", ""))) {
                    return bl.getCompoundOrEmpty(j);
                }
            }
        }
        return null;
    }
}
