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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhDex;

/**
 * The focused game tests of phase B of the slice geest_modellen (CONTRACT_E2_B 8): Fang's flight (rides the path, no getting
 * off, lands, daarna once), the libelkonijnguh pet (one per player, never lost) with the kennisvosje that leads, and the new
 * creatures, spirit characters and Guhdex pages. Batch {@code einde2_geest_modellen}; the room is the phase A floor.
 */
public class E2GeestModellenGameTests {
    private static final String VLOER = "einde2_geestmodellen_test_vloer";
    private static final String BATCH = "einde2_geest_modellen";

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
        return helper.spawn((EntityType<T>) E2GeestWezensFeature.type(w), new Vec3(x, y, z));
    }

    private static JsonObject bron(String pad) {
        try (InputStream in = E2GeestModellenGameTests.class.getResourceAsStream(pad)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** Fang flies a player along a path: on his seat, no getting off by sneaking, landed at the last point, daarna once, unhurt. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 400)
    public static void einde2GeestModellenFang(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 4.5, 4.5);
        float leven = p.getHealth();
        helper.assertTrue(!Fang.vlieg(p, List.of(), null), "an empty path: nothing happens");
        List<Vec3> pad = List.of(helper.absoluteVec(new Vec3(12.5, 6, 6.5)), helper.absoluteVec(new Vec3(20.5, 5, 14.5)),
                helper.absoluteVec(new Vec3(18.5, 2, 20.5)));
        int[] daarna = {0};
        helper.assertTrue(Fang.vlieg(p, pad, sp -> daarna[0]++), "the flight starts");
        helper.assertTrue(p.getVehicle() instanceof FangEntity && Fang.vliegt(p), "the player sits on Fang");
        helper.assertTrue(!Fang.vlieg(p, pad, null), "one flight at a time");
        p.setShiftKeyDown(true);
        p.stopRiding();
        p.setShiftKeyDown(false);
        helper.assertTrue(p.getVehicle() instanceof FangEntity, "sneaking does not get you off halfway");
        helper.runAfterDelay(30, () -> helper.assertTrue(p.getVehicle() instanceof FangEntity f && f.isOpVlucht()
                && p.position().distanceTo(helper.absoluteVec(new Vec3(4.5, 2, 4.5))) > 1.0, "on the way: he flies"));
        helper.succeedWhen(() -> {
            Vec3 eind = pad.get(pad.size() - 1);
            helper.assertTrue(daarna[0] == 1, "daarna ran once: " + daarna[0]);
            helper.assertTrue(!p.isPassenger() && p.position().distanceTo(eind) < 1.0, "landed at the last point: " + p.position());
            helper.assertTrue(p.getHealth() == leven && !Fang.vliegt(p), "unhurt, the flight is over");
            weg(helper, p);
        });
    }

    /** The pet: none, given (one per player), the same one when given again, back when it was lost, stays when told; and a
     *  kennisvosje leads a player to its goal and tells BIJ_DOEL once. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 400)
    public static void einde2GeestModellenHuisdierEnVosje(GameTestHelper helper) {
        ServerPlayer a = speler(helper, 6.5, 18.5), b = speler(helper, 18.5, 18.5);
        try {
            helper.assertTrue(!Libelkonijnguh.heeft(a) && Libelkonijnguh.vind(a) == null, "nobody has one yet");
            Entity pet = Libelkonijnguh.geef(a);
            helper.assertTrue(pet instanceof LibelkonijnguhEntity l && a.getUUID().equals(l.eigenaar()) && Libelkonijnguh.heeft(a)
                    && !Libelkonijnguh.heeft(b), "a has a pet of its own, b none");
            helper.assertTrue(Libelkonijnguh.geef(a) == pet && Libelkonijnguh.vind(a) == pet, "one per player: the same one again");
            pet.discard();
            Libelkonijnguh.controleer(a);
            Entity terug = Libelkonijnguh.vind(a);
            helper.assertTrue(terug != null && terug != pet && Libelkonijnguh.isVan(a, terug), "never lost: a new one at a's side");
            helper.assertTrue(Libelkonijnguh.wisselBlijf(a) && Libelkonijnguh.blijft(a), "told to stay");
            terug.discard();
            Libelkonijnguh.controleer(a);
            helper.assertTrue(Libelkonijnguh.vind(a) == null, "staying: not fetched");
            helper.assertTrue(Libelkonijnguh.geef(a) != null && !Libelkonijnguh.blijft(a), "geef calls it back");
        } finally {
            Libelkonijnguh.wis(a);
        }
        weg(helper, b);
        // the kennisvosje
        KennisvosjeEntity vos = maak(helper, GeestWezens.KENNISVOSJE, 3.5, 2, 4.5);
        BlockPos doel = helper.absolutePos(new BlockPos(14, 2, 4));
        vos.zetDoel(doel);
        Vec3 start = helper.absoluteVec(new Vec3(5.5, 2, 4.5));
        a.snapTo(start.x, start.y, start.z, 0f, 0f);
        int[] gezegd = {0};
        KennisvosjeEntity.BIJ_DOEL.add((v, sp) -> gezegd[0]++);
        helper.onEachTick(() -> {
            if (!vos.isAangekomen()) {                                // (the player follows a little behind)
                Vec3 v = vos.position();
                a.snapTo(v.x - 2.0, v.y, v.z, 0f, 0f);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(vos.isAangekomen() && vos.blockPosition().distManhattan(doel) <= 2, "it led the player to the goal");
            helper.assertTrue(gezegd[0] == 1, "BIJ_DOEL once: " + gezegd[0]);
            KennisvosjeEntity.BIJ_DOEL.clear();
            weg(helper, a);
        });
    }

    /** The new creatures, the spirit characters and the pages: files, pages, the sleeping heron, Wan-guh and a giant hide when
     *  you come close, a mask turns its face to you and not its body. */
    @GuhTest(template = VLOER, batch = BATCH, timeoutTicks = 200)
    public static void einde2GeestModellenNieuw(GameTestHelper helper) {
        // the pages: ten bonus pages of creatures, none for Wan-guh, the giants and the masks
        List<GuhVariant> paginas = List.of(GuhVariant.KOH, GuhVariant.HEI_BAI_GUH, GuhVariant.FANG, GuhVariant.GEEST_KOI, GuhVariant.LIBELKONIJNGUH,
                GuhVariant.LANTAARNGEEST, GuhVariant.SCHADUWGEEST, GuhVariant.KENNISVOSJE, GuhVariant.MISTREIGER, GuhVariant.OUDE_REIGERGUH);
        for (GuhVariant v : paginas) {
            helper.assertTrue(GuhDex.isCreaturePage(v) && GuhDex.EXTRA.contains(v) && GuhDex.ENTRIES.contains(v) && !GuhDex.TELLEND.contains(v)
                    && GuhDex.NIEUW_EINDE2.contains(v.name()) && v.isCharacter(), v + ": a bonus creature page");
        }
        for (String geen : List.of("WAN_GUH", "GEESTREUS", "KIJKMASKER")) {
            helper.assertTrue(java.util.Arrays.stream(GuhVariant.values()).noneMatch(v -> v.name().equals(geen)), geen + ": no page");
        }
        // the spirit characters: model, sheet + glow mask, own animation file with every name, the mix sets
        for (GuhNpcEntity.Kind kind : GeestKinds.kinds().subList(2, GeestKinds.kinds().size())) {
            JsonObject geo = bron("/assets/guhs/geckolib/models/entity/guh_npc_" + kind.id() + ".geo.json");
            JsonObject an = bron("/assets/guhs/geckolib/animations/entity/guh_npc_" + kind.id() + ".animation.json");
            helper.assertTrue(geo != null && an != null && GeestKinds.doorzichtig(kind) && GeestKinds.gloed(kind), kind + ": files, see-through, glowing");
            Set<String> botten = new HashSet<>();
            for (JsonElement b : geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                botten.add(b.getAsJsonObject().get("name").getAsString());
            }
            for (String naam : GeestKinds.animaties(kind)) {
                helper.assertTrue(an.getAsJsonObject("animations").has("animation.guh_sitting." + naam), kind + ": " + naam);
            }
            for (int i = 0; i < GeestKinds.mix(kind); i++) {
                helper.assertTrue(botten.containsAll(GeestKinds.mixBotten(kind, i)), kind + ": mix set " + i);
            }
        }
        helper.assertTrue(GeestKinds.mix(GuhNpcEntity.Kind.LOTUS_LID_ECHO) == 3, "three kinds of members");
        // the heron sleeps until woken
        OudeReigerguhEntity reiger = maak(helper, GeestWezens.OUDE_REIGERGUH, 12.5, 2, 12.5);
        helper.assertTrue(reiger.slaapt(), "a new Oude Reigerguh sleeps");
        reiger.ontwaak();
        helper.assertTrue(!reiger.slaapt() && reiger.animatie().equals("ontwaak"), "ontwaak: awake, unfolding");
        // Wan-guh, a giant and a mask, with a player near
        ServerPlayer p = speler(helper, 20.5, 4.5);
        WanGuhEntity wan = maak(helper, GeestWezens.WAN_GUH, 20.5, 2, 20.5);
        GeestreusEntity reus = maak(helper, GeestWezens.GEESTREUS, 4.5, 6, 20.5);
        KijkmaskerEntity masker = maak(helper, GeestWezens.KIJKMASKER, 9.5, 3, 4.5);
        masker.snapTo(masker.getX(), masker.getY(), masker.getZ(), 0f, 0f);
        helper.succeedWhen(() -> {
            helper.assertTrue(wan.isVerscholen() && wan.onzichtbaar(), "Wan-guh faded when the player came close");
            helper.assertTrue(reus.isVerscholen(), "the giant hides from a player nearby");
            float draai = net.minecraft.util.Mth.wrapDegrees(masker.getYHeadRot() - masker.richting());
            helper.assertTrue(Math.abs(masker.richting()) < 0.01 && draai < -20f, "the mask's face turned towards the player, not its body: " + draai);
            weg(helper, p);
        });
    }
}
