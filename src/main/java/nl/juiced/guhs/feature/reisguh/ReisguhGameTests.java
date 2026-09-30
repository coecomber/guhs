package nl.juiced.guhs.feature.reisguh;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModEntities;

import net.minecraft.world.entity.EntitySpawnReason;
/** 2.8: the Reisguh as a guh-conductor: his cap and whistle in his own model, the "tuut" and the whistle's rest. 2.10.1: where she lives. */
public class ReisguhGameTests {
    private static final String EMPTY = "empty";

    /** A resource of the mod as JSON (null when it isn't there). */
    public static JsonObject json(String path) {
        try (var in = Guhs.class.getResourceAsStream(path)) {
            return in == null ? null : JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** The bones of a GeckoLib model: name -> parent ("" for none). */
    public static Map<String, String> bones(JsonObject geo) {
        Map<String, String> out = new HashMap<>();
        for (JsonElement e : geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            JsonObject b = e.getAsJsonObject();
            out.put(b.get("name").getAsString(), b.has("parent") ? b.get("parent").getAsString() : "");
        }
        return out;
    }

    @GuhTest(template = EMPTY)
    public static void reisguhIsAConductorWithAWhistle(GameTestHelper helper) {
        JsonObject geo = json("/assets/guhs/geo/entity/guh_npc_reisguh.geo.json");
        helper.assertTrue(geo != null, "the Reisguh's own model");
        Map<String, String> bones = bones(geo);
        helper.assertTrue("head".equals(bones.get("reis_pet")), "the conductor's cap sits on his head: " + bones.get("reis_pet"));
        helper.assertTrue("body".equals(bones.get("reis_fluitje")) && bones.containsKey("reis_koord"), "the whistle on a cord on his chest");
        for (String b : new String[]{"head", "body", "ear_left", "ear_right", "arm_left", "arm_right"}) {
            helper.assertTrue(bones.containsKey(b), "still the sitting guh: " + b);
        }
        helper.assertTrue(Guhs.class.getResource("/assets/guhs/textures/entity/npc_reisguh.png") != null, "his texture");
        JsonObject sounds = json("/assets/guhs/sounds.json");
        helper.assertTrue(sounds != null && sounds.has("reisguh.tuut"), "the tuut in sounds.json");
        helper.assertTrue(Guhs.class.getResource("/assets/guhs/sounds/reisguh_tuut.ogg") != null, "the tuut itself");
        helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Guhs.id("reisguh.tuut")), "the tuut is registered");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void reisguhBlowsHisWhistleWithARest(GameTestHelper helper) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.REISGUH);
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        helper.getLevel().addFreshEntity(npc);
        helper.assertTrue(ReisguhFluit.laatst(npc) == -1, "no whistle yet");
        helper.assertTrue(ReisguhFluit.fluit(npc), "tuut tuut!");
        long start = helper.getLevel().getGameTime();
        helper.assertTrue(ReisguhFluit.laatst(npc) == start, "just now");
        helper.assertFalse(ReisguhFluit.fluit(npc), "not again right away");
        var mond = ReisguhFluit.mond(npc);
        helper.assertTrue(mond.y > npc.getY() + 0.6 && mond.y < npc.getY() + npc.getBbHeight() + 0.5, "the whistle is at his mouth: " + mond);
        helper.runAfterDelay(ReisguhFluit.RUST + 1, () -> {
            // (or he already did, all by himself: now and then he whistles when someone is around)
            helper.assertTrue(ReisguhFluit.fluit(npc) || ReisguhFluit.laatst(npc) > start, "after a little rest he blows it again");
            npc.discard();
            helper.succeed();
        });
    }
    /**
     * 2.10.1: a Reisguh lives in the big places too: exactly one in each of these templates, with her own name, standing on
     * a floor with room for her head.
     */
    @GuhTest(template = EMPTY)
    public static void reisguhWoontInDeGroteBouwwerken(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        Map<String, String> plekken = new java.util.LinkedHashMap<>();
        plekken.put("elfguhjestocht", "Guhwarden");
        plekken.put("knuffeldal_stadje/plein", "Knuffeldal");
        plekken.put("guh_kermis", "Guhkermis");
        plekken.put("hamster_house_extra_extra_large", "Guhland");
        plekken.put("ballonfestival", "Ballonfestival");
        plekken.put("guh_circuit", "Guhcircuit");
        plekken.forEach((template, naam) -> {
            var tag = manager.get(Guhs.id(template)).orElseThrow().save(new net.minecraft.nbt.CompoundTag());
            var palette = tag.getListOrEmpty("palette");
            Map<BlockPos, String> blocks = new HashMap<>();
            for (var b : tag.getListOrEmpty("blocks")) {
                var c = (net.minecraft.nbt.CompoundTag) b;
                var pos = c.getListOrEmpty("pos");
                blocks.put(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), palette.getCompoundOrEmpty(c.getIntOr("state", 0)).getStringOr("Name", ""));
            }
            int n = 0;
            for (var e : tag.getListOrEmpty("entities")) {
                var c = (net.minecraft.nbt.CompoundTag) e;
                var nbt = c.getCompoundOrEmpty("nbt");
                if (!nbt.getStringOr("id", "").equals("guhs:guh_npc") || !nbt.getStringOr("Kind", "").equals("reisguh")) {
                    continue;
                }
                n++;
                helper.assertTrue(nbt.getStringOr("ReisName", "").equals(naam), template + ": her name is " + naam + ", not " + nbt.getStringOr("ReisName", ""));
                var bp = c.getListOrEmpty("blockPos");
                BlockPos at = new BlockPos(bp.getIntOr(0, 0), bp.getIntOr(1, 0), bp.getIntOr(2, 0));
                String onder = blocks.getOrDefault(at.below(), "minecraft:air");
                helper.assertFalse(onder.equals("minecraft:air"), template + ": she stands on something at " + at);
                helper.assertTrue(blocks.getOrDefault(at, "minecraft:air").equals("minecraft:air")
                        && blocks.getOrDefault(at.above(), "minecraft:air").equals("minecraft:air"), template + ": room for her at " + at);
            }
            helper.assertTrue(n == 1, template + ": " + n + " Reisguhs");
        });
        // and a wild one turns up a bit more often than before 2.10.1 (1 in 250 groups, 200 blocks apart), still not too often
        helper.assertTrue(nl.juiced.guhs.quest.Reisguh.WILD_EEN_OP < 250 && nl.juiced.guhs.quest.Reisguh.WILD_EEN_OP >= 80,
                "wild: 1 in " + nl.juiced.guhs.quest.Reisguh.WILD_EEN_OP);
        helper.assertTrue(nl.juiced.guhs.quest.Reisguh.WILD_AFSTAND < 200 && nl.juiced.guhs.quest.Reisguh.WILD_AFSTAND >= 100,
                "wild: " + nl.juiced.guhs.quest.Reisguh.WILD_AFSTAND + " apart");
        helper.succeed();
    }
}
