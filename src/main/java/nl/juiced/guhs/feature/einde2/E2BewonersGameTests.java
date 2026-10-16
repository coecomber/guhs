package nl.juiced.guhs.feature.einde2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.einde2.castteam.CastTeamRol;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Game tests of the merge step of the cast (CONTRACT_E2 11.7, 11.9): the table of residents ({@link Bewoners}), one resident
 * made at its spot (missing twice, then made; tagged, protected, its spot's name; no double), the named animations on demand
 * ({@link GuhNpcEntity#speel}, M3) and the see-through and glowing kinds (M2).
 */
public class E2BewonersGameTests {
    private static final String BATCH = "einde2_bewoners";

    private static Bewoners.Bewoner van(String structuur, String plek) {
        for (Bewoners.Bewoner b : Bewoners.tabel().getOrDefault(structuur, List.of())) {
            if (b.plek().equals(plek)) {
                return b;
            }
        }
        return null;
    }

    /** Who stands where: the rule of 11.7 and the extra row of 11.9. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2BewonersTabel(GameTestHelper helper) {
        Map<String, List<Bewoners.Bewoner>> tabel = Bewoners.tabel();
        helper.assertTrue(tabel.keySet().containsAll(Einde2Ids.LEEGTE_STRUCTUREN) && tabel.containsKey(Einde2Ids.MIKA_VESTING),
                "the fourteen big places and the vesting are read");
        Set<Kind> kinds = new HashSet<>();
        int n = 0;
        for (Bewoners.Bewoner b : Bewoners.alle()) {
            n++;
            helper.assertTrue(!b.plek().startsWith("knipoog_") && !b.plek().startsWith("team_"), "no scene actor or team member: " + b.naam());
            helper.assertTrue((b.kind() != null) != (b.entity() != null), "a kind or a creature: " + b.naam());
            helper.assertTrue(b.entity() == null || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(b.entity()),
                    "the creature exists: " + b.naam() + " " + b.entity());
            helper.assertTrue(b.entity() == null || (!b.entity().equals(Guhs.id("mika")) && !b.entity().equals(Guhs.id("guh"))),
                    "no ordinary Mika or guh: " + b.naam());
            if (b.kind() != null) {
                kinds.add(b.kind());
            }
            Plekken.Blad blad = Plekken.blad(b.structuur());
            Plekken.Plek p = Plekken.plek(b.structuur(), b.plek());
            helper.assertTrue(p != null, "its marker exists: " + b.naam());
            if (blad.soort().equals("leegte_bouw")) {
                BlockPos l = Bewoners.lokaal(p);
                Vec3i g = blad.grootte();
                helper.assertTrue(l.getX() >= 0 && l.getY() >= 0 && l.getZ() >= 0 && l.getX() < g.getX() && l.getY() < g.getY() && l.getZ() < g.getZ(),
                        "the spot lies in the build: " + b.naam() + " " + l.toShortString());
            }
        }
        // (phase B/C: +7, h3's lu_ten_miroh and laogai_long_feng, and zij's herd westelijke_luchttempel/bizon_1..5 through Bewoners.anders)
        helper.assertTrue(n == 207, "two hundred and seven residents: " + n);
        // not a resident
        for (String niet : List.of("zuidelijke_luchttempel/momo", "vuurnatie_hoofdstad/mikai_bunker", "vuurnatie_hoofdstad/leeuwschildpad",
                "leegtehaven/mika_kok", "leeuwschildpad_eiland/leeuwschildpad", "bikinibodem/neptuguh", "bikinibodem/sponsguh", "bikinibodem/octo",
                "bikinibodem/band_1", "schelpenstad/mindy", "schelpenstad/dennis", "schelpenstad/barman", "noordelijke_waterstam/team_aang",
                "ba_sing_guh/knipoog_mewtwo_dai_li")) {
            String[] d = niet.split("/");
            helper.assertTrue(van(d[0], d[1]) == null, "no resident at " + niet);
        }
        // other looks and types (11.7) and the extra row (11.9)
        Map<String, Kind> anders = new HashMap<>();
        anders.put("vuurnatie_hoofdstad/kamer_zuka", Kind.ZUKA_PRINS);
        anders.put("vuurnatie_hoofdstad/suki", Kind.SUKI_GUH_GEVANGENE);
        anders.put("vuurnatie_hoofdstad/miroh_tent", Kind.OOM_MIROH_LOTUS);
        anders.put("vuurnatie_hoofdstad/acteur_1", Kind.TONEELSPELER_GUH);
        anders.put("vuurnatie_hoofdstad/acteur_2", Kind.TONEELSPELER_GUH_KATARA);
        anders.put("vuurnatie_hoofdstad/acteur_3", Kind.TONEELSPELER_GUH_SOKKA);
        anders.put("vuurnatie_hoofdstad/acteur_4", Kind.TONEELSPELER_GUH_TOPH);
        anders.put("vuurnatie_hoofdstad/acteur_5", Kind.TONEELSPELER_GUH_ZUKA);
        anders.put("kyoshi_dorp/bewoner_1", Kind.KYOSHI_DORPSGUH);
        anders.put("kyoshi_dorp/bewoner_4", Kind.KYOSHI_DORPSGUH);
        anders.put("ba_sing_guh/zuka_ober", Kind.ZUKA);
        anders.put("zuidelijke_luchttempel/roku_beeld", Kind.ROKU_GUH);
        anders.put("mika_vesting/kok", Kind.MIKA_KOK);
        anders.put("bikinibodem/knipoog_guhdalf_brug", null);
        for (Map.Entry<String, Kind> e : anders.entrySet()) {
            String[] d = e.getKey().split("/");
            Bewoners.Bewoner b = van(d[0], d[1]);
            helper.assertTrue(e.getValue() == null ? b == null : b != null && b.kind() == e.getValue(), e.getKey() + " is " + e.getValue());
        }
        Bewoners.Bewoner zuka = van("vuurnatie_hoofdstad", "kamer_zuka");
        helper.assertTrue(zuka.yaw() == 90f && Bewoners.lokaal(Plekken.plek("vuurnatie_hoofdstad", "kamer_zuka")).equals(new BlockPos(145, 54, 63)),
                "prins Zuka stands in the middle of his old room, looking at the door");
        Map<String, String> wezens = Map.of("ba_sing_guh/bosco", "bosco", "omashguh/flopsie", "flopsie", "bibliotheek_wan_shi_guh/wan_shi_guh", "wan_shi_guh",
                "noordelijke_waterstam/vis_maan", "maanvis", "noordelijke_waterstam/vis_oceaan", "oceaanvis", "leeuwschildpad_eiland/kop", "leeuwschildpad_guh",
                "bikinibodem/krabguh", "meneer_krabguh", "schelpenstad/cycloop", "cycloop_mika");
        for (Map.Entry<String, String> e : wezens.entrySet()) {
            String[] d = e.getKey().split("/");
            Bewoners.Bewoner b = van(d[0], d[1]);
            helper.assertTrue(b != null && Guhs.id(e.getValue()).equals(b.entity()), e.getKey() + " is guhs:" + e.getValue());
        }
        helper.assertTrue(van("vuurnatie_hoofdstad", "draak_rood").soort() == 0 && van("vuurnatie_hoofdstad", "draak_blauw").soort() == 1, "Ran and Shaw");
        for (int i = 1; i <= 5; i++) {
            Bewoners.Bewoner b = van("westelijke_luchttempel", "kudde_" + i);
            helper.assertTrue(b != null && b.soort() == i - 1 && "vlieg".equals(b.houding()) && b.entity().equals(Guhs.id("luchtbizon_guh")),
                    "the herd: look " + (i - 1) + ", hovering");
        }
        for (int i = 1; i <= 3; i++) {
            Bewoners.Bewoner b = van("bibliotheek_wan_shi_guh", "vosguh_" + i);
            helper.assertTrue(b != null && !b.stil() && b.honk() == 6 && b.entity().equals(Guhs.id("vosguh")), "the fox guhs walk around: " + i);
        }
        helper.assertTrue(!van("noordelijke_waterstam", "vis_maan").stil(), "the fish swim");
        helper.assertTrue(van("leeuwschildpad_eiland", "kop").soort() == 0 && van("leeuwschildpad_eiland", "kop").yaw() == 0f, "the face: look 0, yaw 0");
        // the looks no marker asks for stay for phase B
        for (Kind k : List.of(Kind.ZUKA_VUURHEER, Kind.ZUKA_BALLING, Kind.ZUKA_BLAUWE_GEEST, Kind.ZUKA_TEAM, Kind.OOM_MIROH_GEVANGENE,
                Kind.YUE_GUH_MAAN, Kind.ROKU_GUH_BEELD, Kind.KYOSHI_GUH_BEELD, Kind.VUURHEER_MIKAI_KOMEET, Kind.VUURHEER_MIKAI_VADSIG)) {
            helper.assertTrue(!kinds.contains(k), k + " is not a resident");
        }
        helper.assertTrue(kinds.contains(Kind.ZUKA_PRINS) && kinds.contains(Kind.VUURHEER_MIKAI) && kinds.contains(Kind.GUHATSO), "the rest are");
        helper.assertTrue(Bewoners.TAG.equals("guhs_bezetting") && Bewoners.tag("einde2_omashguh_bumi", new ChunkPos(3, -2))
                .equals("einde2_omashguh_bumi@" + new ChunkPos(3, -2).pack()), "bbq2's tag");
        helper.succeed();
    }

    /** One resident at its spot: missing twice, then made; tagged and protected; no double; a creature with its look. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 300)
    public static void einde2BewonersPlek(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Bewoners.Bewoner zuka = van("vuurnatie_hoofdstad", "kamer_zuka");
        Bewoners.Bewoner bizon = van("westelijke_luchttempel", "kudde_3");
        Bewoners.Bewoner vos = van("bibliotheek_wan_shi_guh", "vosguh_1");
        BlockPos a = helper.absolutePos(new BlockPos(1, 2, 1)), b = helper.absolutePos(new BlockPos(1, 2, 4)), c = helper.absolutePos(new BlockPos(4, 2, 4));
        long t = level.getGameTime() + helper.getTick();
        String tagA = "einde2_test_zuka@" + t, tagB = "einde2_test_bizon@" + t, tagC = "einde2_test_vos@" + t;
        helper.assertTrue(!Bewoners.plek(level, tagA, a, Rotation.NONE, zuka) && !Bewoners.plek(level, tagB, b, Rotation.NONE, bizon)
                && !Bewoners.plek(level, tagC, c, Rotation.CLOCKWISE_90, vos), "missing once: nothing yet");
        helper.runAfterDelay(Bewoners.BEVESTIG + 2, () -> {
            helper.assertTrue(Bewoners.plek(level, tagA, a, Rotation.NONE, zuka), "missing twice: made");
            helper.assertTrue(Bewoners.plek(level, tagB, b, Rotation.NONE, bizon) && Bewoners.plek(level, tagC, c, Rotation.CLOCKWISE_90, vos), "the creatures too");
            List<GuhNpcEntity> npcs = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(a).inflate(3), e -> Bewoners.isBewoner(e));
            helper.assertTrue(npcs.size() == 1, "one prince: " + npcs.size());
            GuhNpcEntity npc = npcs.get(0);
            helper.assertTrue(npc.getKind() == Kind.ZUKA_PRINS && npc.getPersistentData().getStringOr(Bewoners.TAG, "").equals(tagA)
                    && npc.roleData.getStringOr(NpcRollen.PLEK, "").equals("vuurnatie_hoofdstad/kamer_zuka"), "prins Zuka, tagged, his spot's name");
            helper.assertTrue(npc.isInvulnerable() && npc.isPersistenceRequired() && Math.abs(Mth.wrapDegrees(npc.getYRot() - 90f)) < 0.01f,
                    "protected, looking the marker's way");
            helper.assertTrue(NpcRollen.van(npc) == CastTeamRol.ROL, "the default role speaks until phase B attaches one to the spot");
            List<CastWezen> wezens = level.getEntitiesOfClass(CastWezen.class, new AABB(b).inflate(3),
                    e -> e.getPersistentData().getStringOr(Bewoners.TAG, "").equals(tagB));
            helper.assertTrue(wezens.size() == 1, "one bison");
            CastWezen w = wezens.get(0);
            helper.assertTrue(w.soort() == 2 && w.stil() && w.houding().equals("vlieg")
                    && w.getPersistentData().getStringOr(Bewoners.PLEK, "").equals("westelijke_luchttempel/kudde_3"), "look 2, still, hovering, its spot");
            CastWezen v = level.getEntitiesOfClass(CastWezen.class, new AABB(c).inflate(3),
                    e -> e.getPersistentData().getStringOr(Bewoners.TAG, "").equals(tagC)).get(0);
            helper.assertTrue(!v.stil() && v.honk() == 6 && Math.abs(Mth.wrapDegrees(v.getYRot() - 270f)) < 0.01f,
                    "the fox walks within 6 blocks, turned with its piece: " + v.getYRot());
            // a double is discarded, a resident that is there is left alone
            Entity tweede = Bewoners.maak(level, net.minecraft.world.phys.Vec3.atBottomCenterOf(a), Rotation.NONE, zuka);
            tweede.getPersistentData().putString(Bewoners.TAG, tagA);
            level.addFreshEntity(tweede);
            helper.assertTrue(!Bewoners.plek(level, tagA, a, Rotation.NONE, zuka), "nothing new when it is there");
            helper.assertTrue(level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(a).inflate(3), e -> e.isAlive() && Bewoners.isBewoner(e)).size() == 1,
                    "the double is gone");
            for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(a).inflate(8), Bewoners::isBewoner)) {
                e.discard();
            }
            helper.succeed();
        });
    }

    /** M3: a named animation on demand, a pose that lasts, a name the file does not hold; a click plays "praat". M2: the flags. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void einde2CastSpeel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (Kind kind : CastTabellen.kinds()) {
            helper.assertTrue(GuhNpcEntity.ANIMATIE_DUUR.containsKey(kind), kind + " plays its animations on demand");
            for (String naam : CastTabellen.animaties(kind)) {
                helper.assertTrue(CastTabellen.duur(kind, naam) >= 0, kind + " " + naam);
            }
        }
        Set<Kind> doorzichtig = new HashSet<>(), gloed = new HashSet<>();
        for (Kind kind : CastTabellen.kinds()) {
            if (CastTabellen.doorzichtig(kind)) {
                doorzichtig.add(kind);
            }
            if (CastTabellen.gloed(kind)) {
                gloed.add(kind);
                helper.assertTrue(E2BewonersGameTests.class.getResource("/assets/guhs/textures/entity/npc_" + kind.id() + "_glowmask.png") != null,
                        kind + " has its glowmask");
            }
        }
        helper.assertTrue(doorzichtig.equals(Set.of(Kind.GUHATSO, Kind.ROKU_GUH, Kind.KYOSHI_GUH, Kind.YUE_GUH_MAAN)), "see-through: " + doorzichtig);
        helper.assertTrue(gloed.equals(Set.of(Kind.GUHATSO, Kind.ROKU_GUH, Kind.KYOSHI_GUH, Kind.YUE_GUH_MAAN, Kind.VUURHEER_MIKAI_KOMEET, Kind.AZULA_MIKA)),
                "glowing: " + gloed);
        GuhNpcEntity zuka = nl.juiced.guhs.registry.ModEntities.GUH_NPC.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        zuka.setKind(Kind.ZUKA);
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        zuka.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(zuka);
        GuhNpcEntity aang = nl.juiced.guhs.registry.ModEntities.GUH_NPC.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        aang.setKind(Kind.AANG_GUH);
        aang.snapTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        level.addFreshEntity(aang);
        zuka.speel("bestaat_niet");
        helper.assertTrue(zuka.speelt().isEmpty(), "a name the file does not hold does nothing");
        zuka.speel("mijn_eer");
        aang.speel("zweef");
        int duur = CastTabellen.duur(Kind.ZUKA, "mijn_eer");
        helper.assertTrue(zuka.speelt().equals("mijn_eer") && duur > 0 && aang.speelt().equals("zweef"), "both play");
        helper.runAfterDelay(duur + 3, () -> {
            helper.assertTrue(zuka.speelt().isEmpty(), "the one-shot is over: back to idle");
            helper.assertTrue(aang.speelt().equals("zweef"), "the pose lasts");
            aang.speel("");
            helper.assertTrue(aang.speelt().isEmpty(), "until speel(\"\")");
            ServerPlayer player = GuhMockPlayer.of(helper);
            player.setGameMode(GameType.SURVIVAL);
            zuka.interact(player, InteractionHand.MAIN_HAND, zuka.position());
            helper.assertTrue(zuka.speelt().equals("praat"), "a click plays praat");
            player.discard();
            zuka.discard();
            aang.discard();
            helper.succeed();
        });
    }
}
