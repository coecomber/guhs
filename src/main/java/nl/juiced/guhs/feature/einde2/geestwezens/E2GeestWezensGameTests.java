package nl.juiced.guhs.feature.einde2.geestwezens;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.Nbt;

/**
 * Game tests of the slice geestmodellen: every creature of the Geestenwereld (its type, hitbox and spawn egg, its model,
 * animation and texture files with every name and every moved bone, a glow mask for every texture of a glowing one, every look
 * {@code Soort} saved, Stil / Honk / Houding saved, the animations phase B sets, Koh's {@code wissel}, Hei Bai-guh's two forms
 * and their hitboxes, Fang's seat), nothing hurts them and a click hurts nobody, the schaduwgeest shoves and never harms,
 * the two spirit koi swim one ring opposite each other, and the two kinds (scale, name, files, role, animations on demand).
 * The room is written by tools/features/einde2_geestmodellen.py (test_kamer).
 */
public class E2GeestWezensGameTests {
    private static final String VLOER = "einde2_geestmodellen_test_vloer";
    private static final String BATCH = "einde2_geestmodellen";
    private static final String P = "animation.guh_sitting.";

    private static ServerPlayer speler(GameTestHelper helper, double x, double z) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(x, 2, z));
        player.snapTo(at.x, at.y, at.z, 0, 0);
        return player;
    }

    private static void weg(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    @SuppressWarnings("unchecked")
    private static <T extends GeestWezen> T maak(GameTestHelper helper, GeestWezens w, double x, double y, double z) {
        T t = helper.spawn((EntityType<T>) E2GeestWezensFeature.type(w), new Vec3(x, y, z));
        return t;
    }

    private static JsonObject bron(String pad) {
        try (InputStream in = E2GeestWezensGameTests.class.getResourceAsStream(pad)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private static boolean bestaat(String pad) {
        try (InputStream in = E2GeestWezensGameTests.class.getResourceAsStream(pad)) {
            return in != null;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    private static Set<String> botten(JsonObject geo) {
        Set<String> out = new HashSet<>();
        for (JsonElement b : geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            out.add(b.getAsJsonObject().get("name").getAsString());
        }
        return out;
    }

    private static void bewogenBotten(GameTestHelper helper, String wie, JsonObject anims, Set<String> botten) {
        for (String naam : anims.keySet()) {
            JsonObject a = anims.getAsJsonObject(naam);
            if (a.has("bones")) {
                for (String bot : a.getAsJsonObject("bones").keySet()) {
                    helper.assertTrue(botten.contains(bot), wie + ": the animation " + naam + " moves the bone " + bot + ", which the model does not have");
                }
            }
        }
    }

    // =====================================================================================================================
    // the creatures
    // =====================================================================================================================

    /** Every row: the type, its hitbox and spawn egg, the files (every name, every moved bone, a texture and glow mask per look). */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2GeestWezensTypes(GameTestHelper helper) {
        helper.assertTrue(GeestWezens.values().length == 13, "seven creatures of phase A and six of phase B");
        for (GeestWezens w : GeestWezens.values()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Guhs.id(w.id()));
            helper.assertTrue(type == E2GeestWezensFeature.type(w), w + ": registered as guhs:" + w.id());
            helper.assertTrue(Math.abs(type.getWidth() - w.breed) < 1e-4 && Math.abs(type.getHeight() - w.hoog) < 1e-4, w + ": its hitbox");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Guhs.id(w.id() + "_spawn_egg")), w + ": its spawn egg");
            JsonObject geo = bron("/assets/guhs/geckolib/models/entity/" + w.id() + ".geo.json");
            JsonObject file = bron("/assets/guhs/geckolib/animations/entity/" + w.id() + ".animation.json");
            helper.assertTrue(geo != null && file != null, w + ": its model and animation file");
            JsonObject anims = file.getAsJsonObject("animations");
            helper.assertTrue(!w.animaties().isEmpty() && w.animaties().get(0).equals("praat"), w + ": praat comes first");
            helper.assertTrue(anims.has("idle"), w + ": idle");
            for (String an : w.animaties()) {
                helper.assertTrue(anims.has(an), w + ": the animation " + an);
                helper.assertTrue(w.blijft(an) == anims.getAsJsonObject(an).get("loop").getAsBoolean(), w + ": " + an + " loops exactly when it lasts");
            }
            bewogenBotten(helper, w.id(), anims, botten(geo));
            for (int s = 0; s < Math.max(1, w.soorten); s++) {
                String tex = "/assets/guhs/textures/entity/" + w.textuur(s);
                helper.assertTrue(bestaat(tex + ".png"), w + ": the texture of look " + s);
                helper.assertTrue(!w.gloed || bestaat(tex + "_glowmask.png"), w + ": the glow mask of look " + s);
            }
        }
        helper.assertTrue(botten(bron("/assets/guhs/geckolib/models/entity/hei_bai_guh.geo.json")).containsAll(List.of("klein", "groot")),
                "Hei Bai-guh has both forms (the renderer hides one)");
        helper.assertTrue(botten(bron("/assets/guhs/geckolib/models/entity/fang.geo.json")).contains("zadel"), "Fang has his seat bone");
        helper.assertTrue(botten(bron("/assets/guhs/geckolib/models/entity/lantaarngeest.geo.json")).contains("ooglid"), "the lantern can wink");
        helper.succeed();
    }

    /** Soort (every look), Stil, Honk and Houding are saved; a new one gets a look of its own; Koh starts with his mask. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2GeestWezensOpslaan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int x = 2;
        for (GeestWezens w : GeestWezens.values()) {
            for (int s = 0; s < Math.max(1, w.soorten); s++) {
                GeestWezen g = maak(helper, w, x + 0.5, 2, 4.5);
                x = x % 22 + 3;
                g.zetSoort(s);
                g.zetStil(true);
                g.zetHonk(5);
                String pose = w.animaties().stream().filter(w::blijft).findFirst().orElse("idle");
                g.zetAnimatie(pose);
                CompoundTag tag = Nbt.saveWithoutId(g);
                helper.assertTrue(w.soorten == 0 || tag.getIntOr("Soort", -1) == s, w + ": Soort " + s + " saved");
                helper.assertTrue(tag.getBooleanOr("Stil", false) && tag.getIntOr("Honk", 0) == 5 && tag.getStringOr("Houding", "").equals(pose),
                        w + ": Stil, Honk and Houding saved");
                GeestWezen terug = (GeestWezen) E2GeestWezensFeature.type(w).create(level, EntitySpawnReason.LOAD);
                Nbt.load(terug, tag);
                helper.assertTrue(terug.soort() == (w.soorten == 0 ? 0 : s) && terug.stil() && terug.honk() == 5 && terug.houding().equals(pose),
                        w + ": the look, Stil, Honk and Houding come back");
                helper.assertTrue(!terug.zweeft() || terug.isNoGravity(), w + ": a floating spirit floats again after loading");
                g.discard();
            }
        }
        KohEntity koh = maak(helper, GeestWezens.KOH, 12.5, 2, 12.5);
        GeestWezen lantaarn = maak(helper, GeestWezens.LANTAARNGEEST, 4.5, 3, 20.5);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(koh.gezicht() == KohEntity.MASKER, "a new Koh wears his pale mask: " + koh.gezicht());
            helper.assertTrue(Nbt.saveWithoutId(lantaarn).getIntOr("Soort", -1) >= 0, "a new lantern got a colour of its own");
            helper.succeed();
        });
    }

    /** Nothing hurts a spirit, a click says its line and hurts nobody, BIJ_KLIK takes a click. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2GeestWezensNietsDoetPijn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = speler(helper, 12.5, 22.5);
        player.setGameMode(GameType.CREATIVE);                     // (no schaduwgeest shoves a creative player)
        float leven = player.getHealth();
        int x = 3;
        for (GeestWezens w : GeestWezens.values()) {
            GeestWezen g = maak(helper, w, x % 21 + 3.5, 2, x < 21 ? 12.5 : 16.5);
            x += 3;
            float gl = g.getHealth();
            g.hurtServer(level, level.damageSources().playerAttack(player), 50f);
            g.hurtServer(level, level.damageSources().inFire(), 50f);
            g.hurtServer(level, level.damageSources().fall(), 50f);
            helper.assertTrue(g.isAlive() && g.getHealth() == gl, w + ": nothing hurts it");
            g.interact(player, InteractionHand.MAIN_HAND, g.position());
            helper.assertTrue(player.getHealth() == leven && g.isAlive(), w + ": a click hurts nobody");
            helper.assertTrue(g.animatie().equals("praat"), w + ": a click plays praat (" + g.animatie() + ")");
        }
        int[] gezien = {0};
        GeestWezen.BIJ_KLIK.add((g, p) -> {
            gezien[0]++;
            return true;
        });
        try {
            GeestWezen g = maak(helper, GeestWezens.LANTAARNGEEST, 20.5, 3, 20.5);
            g.interact(player, InteractionHand.MAIN_HAND, g.position());
            helper.assertTrue(gezien[0] == 1 && !g.animatie().equals("praat"), "BIJ_KLIK took the click");
        } finally {
            GeestWezen.BIJ_KLIK.clear();
        }
        weg(helper, player);
        helper.succeed();
    }

    /** The schaduwgeest shoves a survival player who comes close: a push, never damage; MAG_DUWEN can refuse. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 120)
    public static void einde2GeestWezensDuw(GameTestHelper helper) {
        SchaduwgeestEntity geest = maak(helper, GeestWezens.SCHADUWGEEST, 12.5, 2, 12.5);
        geest.zetStil(true);
        ServerPlayer player = speler(helper, 13.6, 12.5);
        float leven = player.getHealth();
        helper.assertTrue(geest.doelwit() == player, "the survival player within reach is its target");
        SchaduwgeestEntity.MAG_DUWEN = (g, p) -> false;
        helper.assertTrue(geest.doelwit() == null, "MAG_DUWEN can refuse a shove");
        SchaduwgeestEntity.MAG_DUWEN = (g, p) -> true;
        helper.succeedWhen(() -> {
            helper.assertTrue(geest.animatie().equals("duw"), "it shoved (duw)");
            Vec3 v = player.getDeltaMovement();
            helper.assertTrue(v.x > 0.05 && v.y > 0.05, "the player is pushed away from it and hops: " + v);
            helper.assertTrue(player.getHealth() == leven && player.isAlive(), "a shove never hurts");
            weg(helper, player);
        });
    }

    /** zetAnimatie / houding / animatie; Koh's wissel puts on the new face halfway; Hei Bai-guh's forms and hitboxes. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void einde2GeestWezensAnimaties(GameTestHelper helper) {
        int x = 3;
        for (GeestWezens w : GeestWezens.values()) {
            GeestWezen g = maak(helper, w, x % 21 + 3.5, 2, x < 21 ? 4.5 : 8.5);
            x += 3;
            g.zetStil(true);
            for (String an : w.animaties()) {
                g.zetAnimatie(an);
                helper.assertTrue(w.blijft(an) ? g.houding().equals(an) : g.animatie().equals(an), w + ": " + an + " plays");
            }
            g.zetAnimatie("idle");
            g.zetAnimatie("bestaat_niet");
            helper.assertTrue(g.houding().equals("idle"), w + ": a name it does not have does nothing");
        }
        KohEntity koh = maak(helper, GeestWezens.KOH, 12.5, 2, 14.5);
        HeiBaiGuhEntity heiBai = maak(helper, GeestWezens.HEI_BAI_GUH, 5.5, 2, 18.5);
        helper.assertTrue(heiBai.getBbWidth() < 1.0f, "Hei Bai-guh starts small");
        heiBai.wordBoos();
        helper.assertTrue(heiBai.isBoos() && Math.abs(heiBai.getBbWidth() - HeiBaiGuhEntity.GROOT.width()) < 1e-3
                && Math.abs(heiBai.getBbHeight() - HeiBaiGuhEntity.GROOT.height()) < 1e-3 && heiBai.animatie().equals("groei"),
                "angry: the giant's hitbox, and groei plays");
        helper.runAfterDelay(2, () -> {
            koh.wissel(KohEntity.LACH);
            heiBai.kalmeer();
            helper.assertTrue(koh.gezicht() == KohEntity.MASKER && koh.animatie().equals("wissel"), "wissel: the old face until halfway");
        });
        helper.runAfterDelay(2 + KohEntity.WISSEL_MIDDEN + 2, () -> helper.assertTrue(koh.gezicht() == KohEntity.LACH, "wissel: the new face"));
        helper.runAfterDelay(2 + GeestWezens.HEI_BAI_GUH.duur("kalm") + 3, () -> {
            helper.assertTrue(!heiBai.isBoos() && heiBai.getBbWidth() < 1.0f, "calmed down: the little one again");
            helper.succeed();
        });
    }

    /** Tui and La end up on one ring, opposite each other around a midpoint that stays; Fang's seat is where the table says. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void einde2GeestWezensKoiEnFang(GameTestHelper helper) {
        GeestKoiEntity tui = maak(helper, GeestWezens.GEEST_KOI, 8.5, 6, 12.5);
        GeestKoiEntity la = maak(helper, GeestWezens.GEEST_KOI, 16.5, 6, 12.5);
        tui.zetSoort(0);
        la.zetSoort(1);
        Vec3 midden = tui.position().add(la.position()).scale(0.5);
        helper.assertTrue(tui.partner() == la && la.partner() == tui, "each finds the other");
        FangEntity fang = maak(helper, GeestWezens.FANG, 12.5, 3, 20.5);
        fang.zetStil(true);
        ServerPlayer player = speler(helper, 12.5, 17.5);
        player.setGameMode(GameType.CREATIVE);
        helper.assertTrue(player.startRiding(fang, true, true), "a player can be put on Fang (phase B does it)");
        helper.runAfterDelay(100, () -> {
            Vec3 a = tui.position(), b = la.position();
            Vec3 m = a.add(b).scale(0.5);
            double da = Math.hypot(a.x - m.x, a.z - m.z), db = Math.hypot(b.x - m.x, b.z - m.z);
            helper.assertTrue(Math.hypot(m.x - midden.x, m.z - midden.z) < 1.0, "the midpoint stays: " + m + " / " + midden);
            helper.assertTrue(Math.abs(da - db) < 0.5 && da > 2.0, "one ring, opposite each other: " + da + " / " + db);
            helper.assertTrue(Math.hypot(a.x - 8.5 - helper.absoluteVec(Vec3.ZERO).x, a.z - 12.5 - helper.absoluteVec(Vec3.ZERO).z) > 0.5,
                    "they swim");
            Vec3 zit = player.position().subtract(fang.position());
            Vec3 wil = FangEntity.ZADEL.yRot(-fang.getYRot() * (float) (Math.PI / 180.0));
            helper.assertTrue(player.getVehicle() == fang && Math.hypot(zit.x - wil.x, zit.z - wil.z) < 0.3 && Math.abs(zit.y - wil.y) < 1.0,
                    "the rider sits on Fang's seat: " + zit + " / " + wil);
            player.stopRiding();
            weg(helper, player);
            helper.succeed();
        });
    }

    // =====================================================================================================================
    // the two kinds
    // =====================================================================================================================

    /** Every row of GeestKinds: the kind, its scale and name, files, animations on demand, the role, a click. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2GeestKinds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = speler(helper, 12.5, 4.5);
        float leven = player.getHealth();
        helper.assertTrue(GeestKinds.kinds().size() == 13 && GeestKinds.kinds().subList(0, 2).equals(List.of(GuhNpcEntity.Kind.THEEGEEST_GUH, GuhNpcEntity.Kind.BESTELGUH)),
                "the two kinds of phase A, then the eleven spirits of phase B, in order");
        int x = 6;
        for (GuhNpcEntity.Kind kind : GeestKinds.kinds()) {
            helper.assertTrue(NpcRollen.isVerhaalKind(kind), kind + " stands after TIMMERGUH (its role comes from NpcRollen)");
            GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(x % 24, 2, x < 24 ? 8 : 12));
            x += 6;
            npc.setKind(kind);
            helper.assertTrue(Math.abs(npc.getAttributeValue(Attributes.SCALE) - kind.scale) < 1e-4, kind + ": its scale");
            Component naam = npc.getCustomName();
            helper.assertTrue(naam != null && naam.getContents() instanceof TranslatableContents t && t.getKey().equals("entity.guhs.guh_npc." + kind.id()),
                    kind + ": its name key");
            JsonObject geo = bron("/assets/guhs/geckolib/models/entity/guh_npc_" + kind.id() + ".geo.json");
            JsonObject file = bron("/assets/guhs/geckolib/animations/entity/guh_npc_" + GeestKinds.basis(kind).id() + ".animation.json");
            helper.assertTrue(geo != null && file != null && bestaat("/assets/guhs/textures/entity/npc_" + kind.id() + ".png"), kind + ": its files");
            helper.assertTrue(!GeestKinds.gloed(kind) || bestaat("/assets/guhs/textures/entity/npc_" + kind.id() + "_glowmask.png"), kind + ": its glow mask");
            JsonObject anims = file.getAsJsonObject("animations");
            Set<String> b = botten(geo);
            for (String must : List.of("root", "body", "tail", "foot_left", "foot_right", "arm_left", "arm_right", "head", "ear_left", "ear_right")) {
                helper.assertTrue(b.contains(must), kind + ": the base bone " + must);
            }
            bewogenBotten(helper, kind.id(), anims, b);
            helper.assertTrue(anims.has(P + "idle") && GeestKinds.animaties(kind).get(0).equals("praat"), kind + ": idle, and praat first");
            for (String an : GeestKinds.animaties(kind)) {
                helper.assertTrue(anims.has(P + an), kind + ": the animation " + an);
                helper.assertTrue(npc.duur(an) == GeestKinds.duur(kind, an), kind + ": " + an + " plays on demand");
            }
            npc.speel(GeestKinds.animaties(kind).get(1));
            helper.assertTrue(npc.speelt().equals(GeestKinds.animaties(kind).get(1)), kind + ": speel");
            npc.speel("");
            // the role: the default of the slice, a role for the spot wins
            helper.assertTrue(NpcRollen.van(npc) == GeestRol.ROL, kind + ": the default role");
            float npcLeven = npc.getHealth();
            npc.interact(player, InteractionHand.MAIN_HAND, npc.position());
            helper.assertTrue(npc.getHealth() == npcLeven && player.getHealth() == leven && npc.isAlive(), kind + ": a click hurts nobody");
            String plek = "einde2_geestmodellen_test/plek";
            NpcRole eigen = (n, p) -> { };
            NpcRollen.zet(kind, plek, eigen);
            npc.roleData.putString(NpcRollen.PLEK, plek);
            helper.assertTrue(NpcRollen.van(npc) == eigen, kind + ": a role for the spot wins");
        }
        weg(helper, player);
        helper.succeed();
    }

    /** The group "geest" of the dev rows: every creature in every look and the two kinds. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2GeestWezensToon(GameTestHelper helper) {
        int verwacht = 0;
        for (GuhNpcEntity.Kind kind : GeestKinds.kinds()) {
            verwacht += Math.max(1, GeestKinds.mix(kind));
        }
        for (GeestWezens w : GeestWezens.values()) {
            verwacht += w == GeestWezens.GEESTREUS ? 0 : Math.max(1, w.soorten);   // (the giants hide from a row in front of you)
        }
        helper.assertTrue(GeestToon.aantal() == verwacht, "the row holds every look: " + GeestToon.aantal() + " of " + verwacht);
        Set<String> namen = new HashSet<>();
        for (GeestToon.Rij r : GeestToon.rij()) {
            Entity e = r.maak().apply(helper.getLevel());
            helper.assertTrue(e != null, r.naam() + ": it can be made");
            namen.add(r.naam());
            if (e instanceof GeestWezen g) {
                helper.assertTrue(g.stil(), r.naam() + ": it stands still in the row");
            }
            e.discard();
        }
        helper.assertTrue(namen.size() == verwacht, "every name once");
        helper.succeed();
    }
}
