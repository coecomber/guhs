package nl.juiced.guhs.feature.einde2.castvolk;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of the slice cast_volk (CONTRACT_E2 11.2 I): the table against the Kind block, every kind (its scale, its name,
 * its default role and that a spot role wins), its files in the jar (model, sheet, animations with every name of its row,
 * every bone an animation moves and every mix bone in the model, the glowmask of a glowing kind), a click of a mock player
 * (it hurts nobody), and the dev command's rows ({@code toon} puts every kind and every mix set down, {@code weg} leaves nothing).
 */
public class E2CastVolkGameTests {
    private static final String BATCH = "einde2_cast_volk";
    private static final String LEEG = "empty";

    private static JsonObject bron(String pad) {
        try (InputStream in = E2CastVolkGameTests.class.getResourceAsStream(pad)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException | RuntimeException e) {
            return null;
        }
    }

    private static boolean bestaat(String pad) {
        try (InputStream in = E2CastVolkGameTests.class.getResourceAsStream(pad)) {
            return in != null;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static GuhNpcEntity npc(GameTestHelper helper, GuhNpcEntity.Kind kind, BlockPos at) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(kind);
        BlockPos abs = helper.absolutePos(at);
        npc.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        helper.getLevel().addFreshEntity(npc);
        return npc;
    }

    /** The table: the kinds of the block <einde2_cast_volk> in their order, every row sane. */
    @GuhTest(template = LEEG, batch = BATCH, timeoutTicks = 40)
    public static void e2CastVolkTabel(GameTestHelper helper) {
        List<GuhNpcEntity.Kind> kinds = CastVolk.kinds();
        helper.assertTrue(kinds.size() == 39, "thirty-nine kinds: " + kinds.size());
        int eerste = GuhNpcEntity.Kind.GUHATSO.ordinal();
        for (int i = 0; i < kinds.size(); i++) {
            GuhNpcEntity.Kind kind = kinds.get(i);
            helper.assertTrue(kind.ordinal() == eerste + i, kind + ": in the order of the Kind block");
            helper.assertTrue(NpcRollen.isVerhaalKind(kind), kind + " is a story kind");
            helper.assertTrue(kinds.contains(CastVolk.basis(kind)) && CastVolk.basis(CastVolk.basis(kind)) == CastVolk.basis(kind),
                    kind + ": its character is a base look of the slice");
            helper.assertTrue(CastVolk.lijf(kind).equals("zit") || CastVolk.lijf(kind).equals("mika"), kind + ": a body of this slice");
            List<String> anims = CastVolk.animaties(kind);
            helper.assertTrue(!anims.isEmpty() && anims.get(0).equals("praat") && CastVolk.duur(kind, "praat") == 24, kind + ": praat first, 24 ticks");
            helper.assertTrue(anims.size() >= 2, kind + ": at least one move of its own");
            for (String a : anims) {
                helper.assertTrue(CastVolk.duur(kind, a) >= 0, kind + ": the length of " + a);
            }
            helper.assertTrue(CastVolk.gloed(kind) == CastVolk.doorzichtig(kind), kind + ": in this slice the memory and the spirits glow and only they");
        }
        helper.assertTrue(GuhNpcEntity.Kind.values()[eerste + kinds.size() - 1] == GuhNpcEntity.Kind.TONEELSPELER_GUH_ZUKA, "the block ends with the players");
        helper.assertTrue(CastVolk.mix(GuhNpcEntity.Kind.BA_SING_GUH_BURGER) == 5 && CastVolk.mix(GuhNpcEntity.Kind.WATERSTAM_GUH) == 4
                && CastVolk.mix(GuhNpcEntity.Kind.VUURWIJZE_MIKA) == 2 && CastVolk.mix(GuhNpcEntity.Kind.KOOLMAN) == 0, "the mix counts of the contract");
        helper.assertTrue(CastVolk.basis(GuhNpcEntity.Kind.TONEELSPELER_GUH_TOPH) == GuhNpcEntity.Kind.TONEELSPELER_GUH
                && CastVolk.basis(GuhNpcEntity.Kind.ROKU_GUH_BEELD) == GuhNpcEntity.Kind.ROKU_GUH, "looks know their character");
        helper.succeed();
    }

    /** Every kind: its scale and name, its default role, and a spot role that wins over it. */
    @GuhTest(template = LEEG, batch = BATCH, timeoutTicks = 60)
    public static void e2CastVolkKinds(GameTestHelper helper) {
        NpcRole plekRol = (npc, player) -> {
        };
        for (GuhNpcEntity.Kind kind : CastVolk.kinds()) {
            GuhNpcEntity npc = npc(helper, kind, new BlockPos(1, 1, 1));
            helper.assertTrue(npc.getKind() == kind, kind + ": it is what it was made as");
            helper.assertTrue(Math.abs(npc.getAttributeValue(Attributes.SCALE) - kind.scale) < 1e-4, kind + ": its scale " + kind.scale);
            helper.assertTrue(npc.getCustomName() != null && npc.getCustomName().getContents() instanceof TranslatableContents t
                    && t.getKey().equals("entity.guhs.guh_npc." + kind.id()), kind + ": its name key");
            helper.assertTrue(NpcRollen.van(npc) == CastVolkRol.ROL, kind + ": the slice's default role");
            String plek = "einde2_cast_volk_test/" + kind.id();
            NpcRollen.zet(kind, plek, plekRol);
            npc.roleData.putString(NpcRollen.PLEK, plek);
            helper.assertTrue(NpcRollen.van(npc) == plekRol, kind + ": a spot role wins");
            npc.roleData.remove(NpcRollen.PLEK);
            helper.assertTrue(NpcRollen.van(npc) == CastVolkRol.ROL, kind + ": without the spot the default again");
            npc.discard();
        }
        helper.succeed();
    }

    private static Set<String> botten(JsonObject geo) {
        Set<String> uit = new HashSet<>();
        for (JsonElement b : geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            uit.add(b.getAsJsonObject().get("name").getAsString());
        }
        return uit;
    }

    /** The files of every kind in the jar, and that the animations and the mix sets only name bones the model has. */
    @GuhTest(template = LEEG, batch = BATCH, timeoutTicks = 60)
    public static void e2CastVolkBestanden(GameTestHelper helper) {
        for (CastVolk rij : CastVolk.values()) {
            GuhNpcEntity.Kind kind = rij.kind;
            JsonObject geo = bron("/assets/guhs/geckolib/models/entity/guh_npc_" + kind.id() + ".geo.json");
            helper.assertTrue(geo != null, kind + ": its model");
            String id = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonObject("description").get("identifier").getAsString();
            helper.assertTrue(id.equals("geometry.guh_npc_" + kind.id()), kind + ": the model's identifier " + id);
            helper.assertTrue(bestaat("/assets/guhs/textures/entity/npc_" + kind.id() + ".png"), kind + ": its sheet");
            helper.assertTrue(!CastVolk.gloed(kind) || bestaat("/assets/guhs/textures/entity/npc_" + kind.id() + "_glowmask.png"), kind + ": its glowmask");
            Set<String> botten = botten(geo);
            for (String basis : CastVolk.lijf(kind).equals("zit")
                    ? List.of("root", "body", "tail", "foot_left", "foot_right", "arm_left", "arm_right", "head", "ear_left", "ear_right")
                    : List.of("root", "body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right")) {
                helper.assertTrue(botten.contains(basis), kind + ": the base bone " + basis);
            }
            Set<String> mix = new HashSet<>();
            for (int i = 0; i < CastVolk.mix(kind); i++) {
                for (String b : CastVolk.mixBotten(kind, i)) {
                    helper.assertTrue(b.startsWith("mix_" + i + "_") && botten.contains(b), kind + ": the mix bone " + b);
                    mix.add(b);
                }
            }
            for (String b : botten) {
                helper.assertTrue(!b.startsWith("mix_") || mix.contains(b), kind + ": " + b + " is in no mix set");
            }
            JsonObject file = bron("/assets/guhs/geckolib/animations/entity/guh_npc_" + rij.bestand.id() + ".animation.json");
            helper.assertTrue(file != null, kind + ": its animation file guh_npc_" + rij.bestand.id());
            JsonObject anims = file.getAsJsonObject("animations");
            List<String> namen = new ArrayList<>(CastVolk.animaties(kind));
            namen.add(0, "idle");
            for (String naam : namen) {
                JsonObject a = anims.getAsJsonObject("animation.guh_sitting." + naam);
                helper.assertTrue(a != null, kind + ": the animation " + naam);
                int ticks = naam.equals("idle") ? 0 : CastVolk.duur(kind, naam);
                if (ticks > 0) {
                    helper.assertTrue(Math.abs(a.get("animation_length").getAsDouble() * 20 - ticks) < 0.01, kind + ": " + naam + " lasts " + ticks + " ticks");
                }
                for (Map.Entry<String, JsonElement> bot : a.getAsJsonObject("bones").entrySet()) {
                    helper.assertTrue(botten.contains(bot.getKey()), kind + ": " + naam + " moves " + bot.getKey() + ", a bone the model lacks");
                    helper.assertTrue(!bot.getKey().startsWith("mix_"), kind + ": " + naam + " moves a mix bone");
                }
            }
            helper.assertTrue(anims.getAsJsonObject("animation.guh_sitting.idle").get("loop").getAsBoolean(), kind + ": the idle loops");
        }
        helper.succeed();
    }

    /** A click of a mock player: one of the two lines, nothing thrown, nobody hurt. */
    @GuhTest(template = LEEG, batch = BATCH, timeoutTicks = 60)
    public static void e2CastVolkKlik(GameTestHelper helper) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 0));
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        float leven = player.getHealth();
        for (GuhNpcEntity.Kind kind : CastVolk.kinds()) {
            GuhNpcEntity npc = npc(helper, kind, new BlockPos(1, 1, 1));
            float npcLeven = npc.getHealth();
            for (int i = 0; i < 4; i++) {
                npc.interact(player, InteractionHand.MAIN_HAND, npc.position());
            }
            helper.assertTrue(player.getHealth() == leven && npc.getHealth() == npcLeven && npc.isAlive(), kind + ": a click hurts nobody");
            helper.assertTrue(player.getInventory().isEmpty(), kind + ": a click gives or takes nothing");
            npc.discard();
        }
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    /** The dev command's rows: every kind once and a crowd kind once per set, each crowd member showing its own set; weg clears. */
    @GuhTest(template = LEEG, batch = BATCH, timeoutTicks = 80)
    public static void e2CastVolkToon(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        int verwacht = 0;
        for (GuhNpcEntity.Kind kind : CastVolk.kinds()) {
            verwacht += Math.max(1, CastVolk.mix(kind));
        }
        int gezet = E2CastVolkCommando.toon(helper.getLevel(), at, Direction.SOUTH);
        helper.assertTrue(gezet == verwacht, "every kind and every set stands: " + gezet + " of " + verwacht);
        List<GuhNpcEntity> rij = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, new net.minecraft.world.phys.AABB(at).inflate(64),
                e -> e.entityTags().contains(E2CastVolkCommando.TAG));
        helper.assertTrue(rij.size() == verwacht, "they are all there: " + rij.size());
        for (GuhNpcEntity.Kind kind : CastVolk.kinds()) {
            int n = CastVolk.mix(kind);
            if (n > 0) {
                Set<Integer> sets = new HashSet<>();
                for (GuhNpcEntity npc : rij) {
                    if (npc.getKind() == kind) {
                        sets.add(E2CastVolkFeature.mixSet(npc.blockPosition(), n));
                    }
                }
                helper.assertTrue(sets.size() == n, kind + ": every one of its " + n + " sets is shown: " + sets);
            }
        }
        int weg = E2CastVolkCommando.weg(helper.getLevel(), at, 64);
        helper.assertTrue(weg == verwacht, "weg takes them all: " + weg);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, new net.minecraft.world.phys.AABB(at).inflate(64),
                e -> e.isAlive() && e.entityTags().contains(E2CastVolkCommando.TAG)).isEmpty(), "and leaves nothing");
        helper.succeed();
    }
}
