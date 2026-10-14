package nl.juiced.guhs.feature.einde2.castteam;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the slice cast_team (CONTRACT_E2 11.2 I): every row of {@link CastTeam} is a kind with its scale and name,
 * its default role {@link CastTeamRol} (and a spot role wins over it), its model, texture and animation file in the jar
 * (idle, praat and every name of the row; every bone an animation moves exists in the model; no mix bones), the glowmask of a
 * glowing kind; a click throws nothing and hurts nobody; the dev row of {@link E2CastTeamCommando} comes and goes.
 */
public class E2CastTeamGameTests {
    private static final String BATCH = "einde2_cast_team";

    private static JsonObject bron(String pad) {
        try (InputStream in = E2CastTeamGameTests.class.getResourceAsStream(pad)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private static boolean bestaat(String pad) {
        try (InputStream in = E2CastTeamGameTests.class.getResourceAsStream(pad)) {
            return in != null;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static GuhNpcEntity maak(GameTestHelper helper, GuhNpcEntity.Kind kind, double x, double z) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
        npc.setKind(kind);
        Vec3 at = helper.absoluteVec(new Vec3(x, 2, z));
        npc.snapTo(at.x, at.y, at.z, 0f, 0f);
        level.addFreshEntity(npc);
        return npc;
    }

    /** The table: kinds, scale, name, roles, and the static answers of the contract. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2CastTeamTabel(GameTestHelper helper) {
        List<GuhNpcEntity.Kind> kinds = CastTeam.kinds();
        helper.assertTrue(kinds.size() == 22, "nineteen kinds and three more looks of Zuka: " + kinds.size());
        for (int i = 1; i < kinds.size(); i++) {
            helper.assertTrue(kinds.get(i).ordinal() == kinds.get(0).ordinal() + i, "the kinds stand in one block, in order: " + kinds.get(i));
        }
        helper.assertTrue(kinds.get(0).ordinal() > GuhNpcEntity.Kind.TIMMERGUH.ordinal() && NpcRollen.isVerhaalKind(kinds.get(0)),
                "the block stands after TIMMERGUH");
        for (GuhNpcEntity.Kind zuka : List.of(GuhNpcEntity.Kind.ZUKA, GuhNpcEntity.Kind.ZUKA_PRINS, GuhNpcEntity.Kind.ZUKA_VUURHEER,
                GuhNpcEntity.Kind.ZUKA_BALLING, GuhNpcEntity.Kind.ZUKA_BLAUWE_GEEST, GuhNpcEntity.Kind.ZUKA_TEAM)) {
            helper.assertTrue(CastTeam.basis(zuka) == GuhNpcEntity.Kind.ZUKA && CastTeam.lijf(zuka).equals("mika"), "Zuka has six looks: " + zuka);
        }
        helper.assertTrue(CastTeam.basis(GuhNpcEntity.Kind.OOM_MIROH_LOTUS) == GuhNpcEntity.Kind.OOM_MIROH
                && CastTeam.basis(GuhNpcEntity.Kind.SUKI_GUH_GEVANGENE) == GuhNpcEntity.Kind.SUKI_GUH
                && CastTeam.basis(GuhNpcEntity.Kind.YUE_GUH_MAAN) == GuhNpcEntity.Kind.YUE_GUH, "the looks know their character");
        helper.assertTrue(CastTeam.doorzichtig(GuhNpcEntity.Kind.YUE_GUH_MAAN) && CastTeam.gloed(GuhNpcEntity.Kind.YUE_GUH_MAAN)
                && !CastTeam.gloed(GuhNpcEntity.Kind.YUE_GUH), "the moon spirit is see-through and glows, the princess does not");
        NpcRole plekRol = (npc, player) -> {
        };
        for (GuhNpcEntity.Kind kind : kinds) {
            NpcRollen.zet(kind, "einde2_cast_team_test/plek", plekRol);
        }
        int x = 0;
        for (GuhNpcEntity.Kind kind : kinds) {
            helper.assertTrue(CastTeam.mix(kind) == 0 && CastTeam.mixBotten(kind, 0).isEmpty(), kind + ": no crowd");
            helper.assertTrue(CastTeam.animaties(kind).get(0).equals("praat") && CastTeam.duur(kind, "praat") == 24, kind + ": praat first, 24 ticks");
            GuhNpcEntity npc = maak(helper, kind, 1.5 + (x++ % 5) * 2, 1.5 + (x / 5) * 2);
            helper.assertTrue(npc.getKind() == kind, kind + ": it is what it was made as");
            helper.assertTrue(Math.abs(npc.getAttributeValue(Attributes.SCALE) - kind.scale) < 1e-4, kind + ": its scale " + kind.scale);
            Component naam = npc.getCustomName();
            helper.assertTrue(naam != null && naam.getContents() instanceof TranslatableContents t && t.getKey().equals("entity.guhs.guh_npc." + kind.id()),
                    kind + ": its name key");
            helper.assertTrue(NpcRollen.van(npc) == CastTeamRol.ROL, kind + ": the default role is the small talk");
            npc.roleData.putString(NpcRollen.PLEK, "einde2_cast_team_test/plek");
            helper.assertTrue(NpcRollen.van(npc) == plekRol, kind + ": a spot role wins");
            npc.roleData.remove(NpcRollen.PLEK);
            helper.assertTrue(NpcRollen.van(npc) == CastTeamRol.ROL, kind + ": and without the spot the default is back");
            npc.discard();
        }
        helper.succeed();
    }

    /** The resources of every kind, read from the jar. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2CastTeamBestanden(GameTestHelper helper) {
        for (GuhNpcEntity.Kind kind : CastTeam.kinds()) {
            JsonObject geo = bron("/assets/guhs/geckolib/models/entity/guh_npc_" + kind.id() + ".geo.json");
            helper.assertTrue(geo != null, kind + ": its model");
            JsonObject g = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            helper.assertTrue(g.getAsJsonObject("description").get("identifier").getAsString().equals("geometry.guh_npc_" + kind.id()), kind + ": the identifier");
            Set<String> botten = new HashSet<>();
            for (JsonElement b : g.getAsJsonArray("bones")) {
                botten.add(b.getAsJsonObject().get("name").getAsString());
            }
            List<String> basis = CastTeam.lijf(kind).equals("mika")
                    ? List.of("root", "body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right")
                    : List.of("root", "body", "tail", "foot_left", "foot_right", "arm_left", "arm_right", "head", "ear_left", "ear_right");
            helper.assertTrue(botten.containsAll(basis), kind + ": every bone of its body");
            helper.assertTrue(botten.stream().noneMatch(b -> b.startsWith("mix_")), kind + ": no mix bones");
            helper.assertTrue(bestaat("/assets/guhs/textures/entity/npc_" + kind.id() + ".png"), kind + ": its texture");
            helper.assertTrue(!CastTeam.gloed(kind) || bestaat("/assets/guhs/textures/entity/npc_" + kind.id() + "_glowmask.png"), kind + ": its glowmask");
            JsonObject anims = bron("/assets/guhs/geckolib/animations/entity/guh_npc_" + CastTeam.basis(kind).id() + ".animation.json");
            helper.assertTrue(anims != null, kind + ": the animation file of its character");
            JsonObject alle = anims.getAsJsonObject("animations");
            helper.assertTrue(alle.has("animation.guh_sitting.idle"), kind + ": idle");
            for (String naam : CastTeam.animaties(kind)) {
                helper.assertTrue(alle.has("animation.guh_sitting." + naam), kind + ": the animation " + naam);
            }
            for (Map.Entry<String, JsonElement> a : alle.entrySet()) {
                JsonObject bones = a.getValue().getAsJsonObject().getAsJsonObject("bones");
                for (String bot : bones.keySet()) {
                    helper.assertTrue(botten.contains(bot), kind + ": " + a.getKey() + " moves " + bot + ", which its model has");
                }
            }
        }
        helper.succeed();
    }

    /** A click: one line of small talk, nothing thrown, nobody hurt. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2CastTeamKlik(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        float leven = player.getHealth();
        for (GuhNpcEntity.Kind kind : CastTeam.kinds()) {
            GuhNpcEntity npc = maak(helper, kind, 3.5, 3.5);
            float npcLeven = npc.getHealth();
            for (int i = 0; i < 4; i++) {
                npc.interact(player, InteractionHand.MAIN_HAND, npc.position());
            }
            npc.hurtServer(level, level.damageSources().generic(), 100f);
            helper.assertTrue(npc.isAlive() && npc.getHealth() == npcLeven && !npc.isPushable(), kind + ": lief and safe");
            npc.discard();
        }
        helper.assertTrue(player.getHealth() == leven, "nobody got hurt");
        helper.assertTrue(CastTeamRol.praatje(GuhNpcEntity.Kind.ZUKA_BALLING, 1).equals("quest.guhs.einde2_cast_team.praatje.zuka_balling.1"), "the key of a line");
        player.discard();
        helper.succeed();
    }

    /** The dev row: every kind once, tagged, and gone again with weg. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2CastTeamToon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 bij = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        int n = E2CastTeamCommando.toon(level, bij, 0f);
        helper.assertTrue(n == CastTeam.kinds().size(), "the whole row stands: " + n);
        int weg = E2CastTeamCommando.weg(level);
        helper.assertTrue(weg == n, "weg removes the row: " + weg);
        helper.assertTrue(E2CastTeamCommando.weg(level) == 0, "and nothing is left");
        helper.succeed();
    }
}
