package nl.juiced.guhs.feature.einde2.castschurk;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
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
 * Game tests of the slice cast_schurk (CONTRACT_E2 11.2 I, 11.6): every Mika kind (its scale and name, its role and that a
 * spot role wins, its model, texture and animation files with every name and every moved bone, the mix bones, the glow
 * mask, a click), every creature (its hitbox, spawn egg, files, the animations phase B sets, Soort / Stil / Honk / Houding /
 * Draagt from a template and saved, the click and BIJ_KLIK, nothing hurts it), the two fish on one ring in the pond, and the
 * dev rows (toon / weg). The rooms are written by tools/features/einde2_cast_schurk.py (test_kamers).
 */
public class E2CastSchurkGameTests {
    private static final String VLOER = "einde2_cast_schurk_test_vloer", VIJVER = "einde2_cast_schurk_test_vijver";
    private static final String BATCH = "einde2_cast_schurk";
    /** The pond of the room VIJVER (template x, z; water at template y 1 .. 2 = helper y 2 .. 3: the same numbers as the python module). */
    private static final int VIJVER_X0 = 2, VIJVER_X1 = 14, VIJVER_Z0 = 2, VIJVER_Z1 = 14;
    private static final String P = "animation.guh_sitting.";

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer player = GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(x + 0.5, 2, z + 0.5));
        player.snapTo(at.x, at.y, at.z, 0, 0);
        return player;
    }

    private static void weg(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    @SuppressWarnings("unchecked")
    private static <T extends CastWezen> T maak(GameTestHelper helper, Wezens w, int x, int y, int z) {
        return helper.spawn((EntityType<T>) E2CastSchurkFeature.type(w), new BlockPos(x, y, z));
    }

    private static JsonObject bron(String pad) {
        try (InputStream in = E2CastSchurkGameTests.class.getResourceAsStream(pad)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private static boolean bestaat(String pad) {
        try (InputStream in = E2CastSchurkGameTests.class.getResourceAsStream(pad)) {
            return in != null;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /** The bone names of a geo file. */
    private static Set<String> botten(JsonObject geo) {
        Set<String> out = new HashSet<>();
        for (JsonElement b : geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            out.add(b.getAsJsonObject().get("name").getAsString());
        }
        return out;
    }

    /** Every bone an animation moves exists in the model. */
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

    private static Entity laad(GameTestHelper helper, String nbt, Vec3 at) {
        CompoundTag tag;
        try {
            tag = net.minecraft.nbt.TagParser.parseCompoundFully(nbt);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
        Entity e = EntityType.loadEntityRecursive(tag, helper.getLevel(), EntitySpawnReason.STRUCTURE, en -> {
            en.snapTo(at.x, at.y, at.z, 0, 0);
            return en;
        });
        helper.assertTrue(e != null, "loads from NBT: " + nbt);
        helper.getLevel().addFreshEntity(e);
        return e;
    }

    // =====================================================================================================================
    // the Mika kinds
    // =====================================================================================================================

    /** Every row of the table: the kind exists, gets its scale and name, has its files, every name and every moved bone. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2CastSchurkKinds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(CastSchurk.kinds().size() == 12, "twelve kinds: " + CastSchurk.kinds().size());
        for (GuhNpcEntity.Kind kind : CastSchurk.kinds()) {
            helper.assertTrue(NpcRollen.isVerhaalKind(kind), kind + " stands after TIMMERGUH (its role comes from NpcRollen)");
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
            npc.setKind(kind);
            helper.assertTrue(npc.getKind() == kind, kind + ": the kind sticks");
            helper.assertTrue(Math.abs(npc.getAttributeValue(Attributes.SCALE) - kind.scale) < 1e-4, kind + ": its scale " + kind.scale);
            Component naam = npc.getCustomName();
            helper.assertTrue(naam != null && naam.getContents() instanceof TranslatableContents t && t.getKey().equals("entity.guhs.guh_npc." + kind.id()),
                    kind + ": its name key");
            // the files
            String geoPad = "/assets/guhs/geckolib/models/entity/guh_npc_" + kind.id() + ".geo.json";
            JsonObject geo = bron(geoPad);
            helper.assertTrue(geo != null, kind + ": its model " + geoPad);
            helper.assertTrue(bestaat("/assets/guhs/textures/entity/npc_" + kind.id() + ".png"), kind + ": its texture");
            helper.assertTrue(!CastSchurk.gloed(kind) || bestaat("/assets/guhs/textures/entity/npc_" + kind.id() + "_glowmask.png"), kind + ": its glow mask");
            String basis = CastSchurk.basis(kind).id();
            JsonObject file = bron("/assets/guhs/geckolib/animations/entity/guh_npc_" + basis + ".animation.json");
            helper.assertTrue(file != null, kind + ": the animation file of " + basis);
            JsonObject anims = file.getAsJsonObject("animations");
            List<String> namen = CastSchurk.animaties(kind);
            helper.assertTrue(!namen.isEmpty() && namen.get(0).equals("praat"), kind + ": praat comes first");
            helper.assertTrue(anims.has(P + "idle"), kind + ": idle");
            for (String an : namen) {
                helper.assertTrue(anims.has(P + an), kind + ": the animation " + an);
                helper.assertTrue(CastSchurk.duur(kind, an) >= 0, kind + ": a length for " + an);
            }
            Set<String> botten = botten(geo);
            for (String must : CastSchurk.lijf(kind).equals("lang")
                    ? List.of("root", "body", "head", "ear_left", "ear_right", "arm_left", "arm_right", "leg_left", "leg_right", "tail")
                    : List.of("root", "body", "tail", "leg_back_left", "leg_back_right", "head", "ear_left", "ear_right", "leg_front_left", "leg_front_right")) {
                helper.assertTrue(botten.contains(must), kind + ": the base bone " + must + " of its body " + CastSchurk.lijf(kind));
            }
            // a look shares its character's file: every bone the file moves must be in THIS look's model too
            bewogenBotten(helper, kind.id(), anims, botten);
            Set<String> mix = new HashSet<>();
            for (int i = 0; i < CastSchurk.mix(kind); i++) {
                for (String bot : CastSchurk.mixBotten(kind, i)) {
                    helper.assertTrue(bot.startsWith("mix_" + i + "_") && botten.contains(bot), kind + ": the mix bone " + bot);
                    mix.add(bot);
                }
            }
            for (String bot : botten) {
                helper.assertTrue(!bot.startsWith("mix_") || mix.contains(bot), kind + ": a mix bone in no set: " + bot);
            }
        }
        helper.succeed();
    }

    /** The role: the default of the slice, a role for the NPC's spot wins over it; a click of a player throws nothing and hurts nobody. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2CastSchurkRol(GameTestHelper helper) {
        ServerPlayer player = speler(helper, 7, 3);
        float leven = player.getHealth();
        String plek = "einde2_cast_schurk_test/plek";
        int[] gehoord = {0};
        NpcRole eigen = (npc, p) -> gehoord[0]++;
        int x = 1;
        for (GuhNpcEntity.Kind kind : CastSchurk.kinds()) {
            GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(x, 2, 7));
            x = x % 13 + 1;
            npc.setKind(kind);
            helper.assertTrue(NpcRollen.van(npc) == CastSchurkRol.ROL, kind + ": the default role of the slice");
            float npcLeven = npc.getHealth();
            npc.interact(player, InteractionHand.MAIN_HAND, npc.position());
            helper.assertTrue(npc.getHealth() == npcLeven && player.getHealth() == leven && npc.isAlive(), kind + ": a click hurts nobody");
            NpcRollen.zet(kind, plek, eigen);
            npc.roleData.putString(NpcRollen.PLEK, plek);
            helper.assertTrue(NpcRollen.van(npc) == eigen, kind + ": the role of its spot wins");
            int voor = gehoord[0];
            npc.interact(player, InteractionHand.MAIN_HAND, npc.position());
            helper.assertTrue(gehoord[0] == voor + 1, kind + ": the click went to the spot's role");
            npc.discard();
        }
        weg(helper, player);
        helper.succeed();
    }

    // =====================================================================================================================
    // the creatures
    // =====================================================================================================================

    /** Every creature: its type, hitbox, spawn egg, model, textures, animations; what phase B sets; nothing hurts it. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2CastSchurkWezens(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = speler(helper, 1, 1);
        for (Wezens w : Wezens.values()) {
            CastWezen wezen = E2CastSchurkFeature.type(w).create(level, EntitySpawnReason.COMMAND);
            helper.assertTrue(wezen != null && wezen.wezens() == w && wezen.wie().equals(w.id()), w + " can be made and knows who it is");
            helper.assertTrue(Math.abs(wezen.getBbWidth() - w.breed) < 1e-3 && Math.abs(wezen.getBbHeight() - w.hoog) < 1e-3, w + ": the hitbox of the table");
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Guhs.id(w.id() + "_spawn_egg")), w + " has a spawn egg");
            JsonObject geo = bron("/assets/guhs/geckolib/models/entity/" + w.id() + ".geo.json");
            helper.assertTrue(geo != null, w + ": its model");
            JsonObject file = bron("/assets/guhs/geckolib/animations/entity/" + w.id() + ".animation.json");
            helper.assertTrue(file != null, w + ": its animation file");
            JsonObject anims = file.getAsJsonObject("animations");
            helper.assertTrue(anims.has("idle") && w.animaties().get(0).equals("praat"), w + ": idle, and praat first");
            for (String naam : w.animaties()) {
                helper.assertTrue(anims.has(naam), w + ": the animation " + naam);
            }
            for (int s = 0; s < (w.textuurPerSoort() ? w.soorten : 1); s++) {
                helper.assertTrue(bestaat("/assets/guhs/textures/entity/" + w.textuur(s) + ".png"), w + ": the texture " + w.textuur(s));
            }
            if (w.soorten > 0 && !w.textuurPerSoort()) {
                for (int s = 0; s < w.soorten; s++) {
                    helper.assertTrue(anims.has("soort_" + s), w + ": the pose of look " + s);
                }
            }
            bewogenBotten(helper, w.id(), anims, botten(geo));
            // what phase B sets: a pose stays, a one-shot plays on top of it, an unknown name does nothing
            wezen.zetAnimatie("geen_animatie");
            helper.assertTrue(wezen.animatie().equals("idle"), w + ": an unknown animation is ignored");
            for (String naam : w.animaties()) {
                wezen.zetAnimatie(naam);
                helper.assertTrue(wezen.animatie().equals(naam), w + " plays " + naam + ": " + wezen.animatie());
                helper.assertTrue(w.blijft(naam) == wezen.houding().equals(naam), w + ": " + naam + " lasts only when it is a pose");
                wezen.zetAnimatie("idle");
                helper.assertTrue(wezen.houding().equals("idle"), w + ": back to idle");
            }
            // nothing hurts it
            Vec3 at = helper.absoluteVec(new Vec3(7.5, 2, 7.5));
            wezen.snapTo(at.x, at.y, at.z, 0, 0);
            level.addFreshEntity(wezen);
            float hp = wezen.getHealth();
            wezen.hurtServer(level, level.damageSources().playerAttack(player), 50f);
            wezen.hurtServer(level, level.damageSources().inFire(), 50f);
            helper.assertTrue(wezen.isAlive() && wezen.getHealth() == hp, w + ": nothing hurts it");
            helper.assertTrue(!wezen.isPushable() && !wezen.canBeLeashed() && !wezen.removeWhenFarAway(1000), w + ": not pushed, not leashed, never despawns");
            wezen.discard();
        }
        weg(helper, player);
        helper.succeed();
    }

    /** What a building's template writes: Soort, Stil, Honk, Houding, Draagt, read from NBT and saved again; the bison's scale. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2CastSchurkUitTemplate(GameTestHelper helper) {
        CastWezen draak = (CastWezen) laad(helper, "{id:\"guhs:draakguh\",Soort:1,Stil:1b,Houding:\"slaap\"}", helper.absoluteVec(new Vec3(3.5, 2, 3.5)));
        CastWezen bizon = (CastWezen) laad(helper, "{id:\"guhs:luchtbizon_guh\",Soort:4,Houding:\"vlieg\"}", helper.absoluteVec(new Vec3(10.5, 2, 3.5)));
        VosguhEntity vos = (VosguhEntity) laad(helper, "{id:\"guhs:vosguh\",Honk:6,Draagt:1b}", helper.absoluteVec(new Vec3(4.5, 2, 11.5)));
        CastWezen kop = (CastWezen) laad(helper, "{id:\"guhs:leeuwschildpad_guh\",Soort:1,Stil:1b}", helper.absoluteVec(new Vec3(10.5, 4, 11.5)));
        CastWezen bosco = (CastWezen) laad(helper, "{id:\"guhs:bosco\",Houding:\"geen_houding\"}", helper.absoluteVec(new Vec3(7.5, 2, 7.5)));
        helper.assertTrue(draak.soort() == 1 && draak.stil() && draak.houding().equals("slaap"), "Soort, Stil and Houding from NBT");
        helper.assertTrue(draak.getName().getContents() instanceof TranslatableContents t && t.getKey().equals("entity.guhs.draakguh.1"), "Shaw is called Shaw");
        helper.assertTrue(bizon.soort() == 4 && Math.abs(bizon.getAttributeValue(Attributes.SCALE) - CastWezen.BIZON_SCHAAL[4]) < 1e-4, "the calf is small");
        helper.assertTrue(vos.draagt(), "Draagt from NBT");
        helper.assertTrue(kop.soort() == 1 && kop.noPhysics && kop.isNoGravity(), "the lion turtle: look 1, no collision, no gravity");
        helper.assertTrue(bosco.houding().equals("idle"), "an unknown pose from NBT is idle");
        CompoundTag tag = Nbt.saveWithoutId(draak);
        helper.assertTrue(tag.getIntOr("Soort", -1) == 1 && tag.getBooleanOr("Stil", false) && tag.getStringOr("Houding", "").equals("slaap"), "and saved again: " + tag);
        helper.assertTrue(Nbt.saveWithoutId(vos).getBooleanOr("Draagt", false) && Nbt.saveWithoutId(vos).getIntOr("Honk", 0) == 6, "Draagt and Honk saved");
        vos.setDraagt(false);
        helper.assertTrue(!Nbt.saveWithoutId(vos).getBooleanOr("Draagt", true), "setDraagt(false) saved");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(vos.honk() == 6 && vos.blockPosition().equals(vos.thuisplek()), "its Honk: home is where the template put it");
            helper.assertTrue(draak.thuisplek() == null, "no Honk, no home");
            helper.assertTrue(bizon.isNoGravity(), "a bison that flies hovers");
            bizon.zetAnimatie("idle");
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(!bizon.isNoGravity(), "and lands again");
                helper.succeed();
            });
        });
    }

    /** BIJ_KLIK: the first listener that says true takes the click; without one it talks (praat and a line). */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 100)
    public static void einde2CastSchurkKlik(GameTestHelper helper) {
        ServerPlayer player = speler(helper, 5, 5);
        CastWezen flopsie = maak(helper, Wezens.FLOPSIE, 8, 2, 5);
        CastWezen kop = maak(helper, Wezens.LEEUWSCHILDPAD_GUH, 5, 3, 9);
        int[] gehoord = {0, 0};
        BiFunction<CastWezen, ServerPlayer, Boolean> nee = (w, p) -> {
            if (w == flopsie) {
                gehoord[0]++;
            }
            return false;
        };
        BiFunction<CastWezen, ServerPlayer, Boolean> ja = (w, p) -> {
            if (w == flopsie && p == player) {
                gehoord[1]++;
                return true;
            }
            return false;
        };
        flopsie.interact(player, InteractionHand.MAIN_HAND, flopsie.position());
        helper.assertTrue(flopsie.animatie().equals("praat"), "no listener: it talks");
        kop.interact(player, InteractionHand.MAIN_HAND, kop.position());
        helper.assertTrue(kop.houding().equals("praat"), "the lion turtle talks as a pose");
        CastWezen.BIJ_KLIK.add(nee);
        CastWezen.BIJ_KLIK.add(ja);
        helper.runAfterDelay(CastWezen.PRAAT_HOUDING + 5, () -> {
            try {
                helper.assertTrue(kop.houding().equals("idle"), "and stops when the line is said");
                flopsie.zetAnimatie("idle");
                flopsie.interact(player, InteractionHand.MAIN_HAND, flopsie.position());
                helper.assertTrue(gehoord[0] == 1 && gehoord[1] == 1, "both listeners were asked, in order: " + gehoord[0] + ", " + gehoord[1]);
                helper.assertTrue(!flopsie.animatie().equals("praat"), "the listener took the click: no small talk");
            } finally {
                CastWezen.BIJ_KLIK.remove(nee);
                CastWezen.BIJ_KLIK.remove(ja);
                weg(helper, player);
            }
            helper.succeed();
        });
    }

    /** Tui and La: on one ring round their midpoint, opposite each other, never out of the water; a fish with stil lies still. */
    @GuhTest(template = VIJVER, batch = BATCH, timeoutTicks = 200)
    public static void einde2CastSchurkVissen(GameTestHelper helper) {
        CastVisEntity tui = maak(helper, Wezens.MAANVIS, 5, 2, 8);
        CastVisEntity la = maak(helper, Wezens.OCEAANVIS, 11, 2, 8);
        ServerLevel level = helper.getLevel();
        AABB vijver = new AABB(helper.absolutePos(new BlockPos(VIJVER_X0, 2, VIJVER_Z0)).getCenter(), helper.absolutePos(new BlockPos(VIJVER_X1, 3, VIJVER_Z1)).getCenter()).inflate(0.5);
        helper.assertTrue(tui.partner() == la && la.partner() == tui, "each finds the other");
        helper.runAfterDelay(150, () -> {
            for (CastVisEntity v : List.of(tui, la)) {
                helper.assertTrue(level.getFluidState(v.blockPosition()).is(FluidTags.WATER) && vijver.contains(v.position()), v.wie() + " stays in the water: " + v.position());
            }
            Vec3 midden = tui.position().add(la.position()).scale(0.5);
            double a = tui.position().subtract(midden).horizontalDistance(), b = la.position().subtract(midden).horizontalDistance();
            helper.assertTrue(Math.abs(a - b) < 0.3 && a <= CastVisEntity.MAX_STRAAL + 0.3 && a > 1.0, "one ring of at most 3: " + a + ", " + b);
            Vec3 ta = tui.position().subtract(midden), tb = la.position().subtract(midden);
            double cos = (ta.x * tb.x + ta.z * tb.z) / (ta.horizontalDistance() * tb.horizontalDistance());
            helper.assertTrue(cos < -0.95, "opposite each other: " + cos);
            Vec3 bij = tui.position();
            tui.zetStil(true);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(tui.position().distanceTo(bij) < 0.05, "a fish with stil lies still");
                helper.assertTrue(la.position().distanceTo(midden) > 1.0, "and the other keeps to the water: " + la.position());
                helper.succeed();
            });
        });
    }

    /** The dev rows: every kind and every creature in every look spawn without an error, and weg leaves nothing. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 60)
    public static void einde2CastSchurkToon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int wezens = 0;
        for (Wezens w : Wezens.values()) {
            wezens += Math.max(1, w.soorten);
        }
        int kinds = 0;
        for (GuhNpcEntity.Kind kind : CastSchurk.kinds()) {
            kinds += Math.max(1, CastSchurk.mix(kind));
        }
        int n = E2CastSchurkCommando.zet(level, helper.absolutePos(new BlockPos(7, 2, 1)), Direction.SOUTH);
        helper.assertTrue(n == kinds + wezens, "every kind (each mix set) and every creature in every look: " + n + " of " + (kinds + wezens));
        int weg = E2CastSchurkCommando.weg(level);
        helper.assertTrue(weg == n, "weg removes them all: " + weg);
        helper.runAfterDelay(1, () -> {
            for (Entity e : level.getAllEntities()) {
                helper.assertTrue(!e.isAlive() || !e.entityTags().contains(E2CastSchurkCommando.TAG), "nothing of the rows is left");
            }
            helper.succeed();
        });
    }
}
