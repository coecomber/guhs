package nl.juiced.guhs.feature.einde2.spcast;

import java.io.IOException;
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
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.Nbt;

/**
 * Game tests of sp_cast (CONTRACT_E2_C 7.2, 8.3): the looks (every look on and off survives a save, {@code kan} per entity,
 * the model has the look's bone, an actor's NBT carries them, the stand-in form is read), the animations (every story
 * animation the contract names exists in the table and the file, the Kroeg Mikas' files and tables, every animation a
 * registered Sponsguh scene names on a cast actor or a Kroeg Mika exists, a cast actor reads a scene's animation).
 */
public class E2SpCastGameTests {
    private static final String BATCH = "einde2_sp_cast";
    /** The story animations the contract (7.2) lists per character: what the scene owners may name. */
    static final Map<Cast, List<String>> STORY = Map.ofEntries(
            Map.entry(Cast.SPONSGUH, List.of("zing", "rennen", "huil_blij", "salueer", "slurp", "knipper")),
            Map.entry(Cast.PATGUH, List.of("zing", "nies", "trots")),
            Map.entry(Cast.MENEER_KRABGUH, List.of("knuffel", "paniek")),
            Map.entry(Cast.OCTO_MIKA, List.of("trommel_tel", "buig")),
            Map.entry(Cast.PLANKMIKA, List.of("lach_kwaad", "vlucht")),
            Map.entry(Cast.NEPTUGUH, List.of("bevries", "blaas", "lach")),
            Map.entry(Cast.DENNIS_MIKA, List.of("remmen", "plat")),
            Map.entry(Cast.CYCLOOP_MIKA, List.of("grijp", "nat")),
            Map.entry(Cast.JUF_PUFGUH, List.of("blaas_op", "zucht")),
            Map.entry(Cast.MINDY_GUH, List.of("geef")),
            Map.entry(Cast.DAVID_HASSELGUH, List.of("zwaai")),
            Map.entry(Cast.LOOPGRAAFMONSTER, List.of("zing", "wieg")),
            Map.entry(Cast.BIKINIBODEM_GUH, List.of("juich", "dans")));
    /** The engine plays these itself (Cutscene.Builder.animatie): nothing of the actor's own is needed (zwaai: see SponsguhWezen.swing). */
    private static final Set<String> ENGINE = Set.of("zwaai", "buk", "sta", "spring");

    static JsonObject bron(String pad) {
        try (InputStream in = E2SpCastGameTests.class.getResourceAsStream(pad)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            return null;
        }
    }

    static Set<String> botten(String model) {
        Set<String> uit = new HashSet<>();
        JsonObject geo = bron("/assets/guhs/geckolib/models/entity/" + model + ".geo.json");
        if (geo != null) {
            for (JsonElement b : geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                uit.add(b.getAsJsonObject().get("name").getAsString());
            }
        }
        return uit;
    }

    /** Every look of every character: kan, the model's bone, on/off, a save and a load, an actor's NBT, the stand-in form. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2SpCastLooks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(SpCast.LOOKS.equals(Cast.LOOKS), "SpCast.LOOKS and Cast.LOOKS: the same order (the bits)");
        int aan = 0;
        for (Cast cast : Cast.values()) {
            SponsguhWezen w = E2SponsguhFeature.type(cast).create(level, EntitySpawnReason.COMMAND);
            Set<String> botten = botten(cast.model());
            for (String look : SpCast.LOOKS) {
                boolean kan = cast.looks().contains(look);
                helper.assertTrue(SpCast.kan(w, look) == kan, cast + ": kan " + look + " = " + kan);
                helper.assertTrue(look.equals(SpCast.DROOG) || botten.contains("look_" + look) == kan, cast + ": the model has look_" + look + " = " + kan);
                SpCast.zetLook(w, look, true);
                helper.assertTrue(SpCast.heeftLook(w, look) == kan, cast + ": " + look + " switched on only when it can");
            }
            SpCast.zetLook(w, "geen_look", true);
            CompoundTag tag = Nbt.saveWithoutId(w);
            SponsguhWezen terug = E2SponsguhFeature.type(cast).create(level, EntitySpawnReason.LOAD);
            Nbt.load(terug, tag);
            for (String look : SpCast.LOOKS) {
                helper.assertTrue(SpCast.heeftLook(terug, look) == cast.looks().contains(look), cast + ": " + look + " survives a save");
                aan += SpCast.heeftLook(terug, look) ? 1 : 0;
            }
            for (String look : cast.looks()) {
                SpCast.zetLook(terug, look, false);
            }
            SponsguhWezen uit = E2SponsguhFeature.type(cast).create(level, EntitySpawnReason.LOAD);
            Nbt.load(uit, Nbt.saveWithoutId(terug));
            helper.assertTrue(uit.looks() == 0 && !Nbt.saveWithoutId(uit).contains("Looks"), cast + ": every look off survives a save too");
        }
        helper.assertTrue(aan == 8 + 5 + 6, "8 looks on Sponsguh, 5 on Patguh, the bucket on six others: " + aan);
        helper.assertTrue(!SpCast.kan(ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND), SpCast.SNOR), "a guh NPC has no looks");
        // a cutscene actor: its NBT carries the looks (SpCast.nbt), and the skelet's stand-in form is read too
        SponsguhWezen acteur = E2SponsguhFeature.type(Cast.SPONSGUH).create(level, EntitySpawnReason.LOAD);
        CompoundTag nbt = new CompoundTag();
        SpCast.nbt(SpCast.SNOR, SpCast.KROON_VAST).accept(nbt);
        CompoundTag neo = new CompoundTag();
        neo.putBoolean(SponsguhWezen.STANDIN_LOOK + SpCast.PETJE, true);
        nbt.put("NeoForgeData", neo);
        Nbt.load(acteur, nbt);
        helper.assertTrue(acteur.heeftLook(SpCast.SNOR) && acteur.heeftLook(SpCast.KROON_VAST) && acteur.heeftLook(SpCast.PETJE) && !acteur.heeftLook(SpCast.EMMER),
                "an actor wears the looks of its NBT (and of the stand-in form): " + acteur.lookNamen());
        SponsguhWezen octo = E2SponsguhFeature.type(Cast.OCTO_MIKA).create(level, EntitySpawnReason.LOAD);
        Nbt.load(octo, SpCast.nbt(new CompoundTag(), SpCast.SNOR, SpCast.EMMER));
        helper.assertTrue(octo.lookNamen().equals(List.of(SpCast.EMMER)), "a look the model has not is dropped: " + octo.lookNamen());
        helper.succeed();
    }

    /** The animations: the contract's story animations, the Kroeg Mikas, the Sponsguh scenes' names, a cast actor in a scene. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2SpCastAnimaties(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> fout = new ArrayList<>();
        for (Map.Entry<Cast, List<String>> e : STORY.entrySet()) {
            Cast cast = e.getKey();
            JsonObject anims = bron("/assets/guhs/geckolib/animations/entity/" + cast.model() + ".animation.json");
            for (String naam : e.getValue()) {
                if (!cast.heeft(naam)) {
                    fout.add(cast + " has no " + naam + " in its table");
                }
                if (anims == null || !anims.getAsJsonObject("animations").has(naam)) {
                    fout.add(cast + " has no " + naam + " in its file");
                }
            }
        }
        helper.assertTrue(Cast.OCTOLIAM.heeft("buig") && Cast.DOORLOPEND.containsAll(List.of("rennen", "knuffel", "paniek", "vlucht", "plat", "nat", "wieg", "dans")),
                "Octoliam bows too; the loops last");
        for (KroegMikas r : KroegMikas.values()) {
            String id = r.kind.id();
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
            npc.setKind(r.kind);
            JsonObject anims = bron("/assets/guhs/geckolib/animations/entity/guh_npc_" + id + ".animation.json");
            for (String naam : List.of("idle", "praat", "kijk_op", "slaap")) {
                if (anims == null || !anims.getAsJsonObject("animations").has("animation.guh_sitting." + naam)) {
                    fout.add(id + ": no animation.guh_sitting." + naam);
                }
            }
            if (npc.duur("praat") != 24 || npc.duur("kijk_op") != 0 || npc.duur("slaap") != 0 || npc.duur("dans") != -1) {
                fout.add(id + ": its table (praat 24, kijk_op and slaap poses) is not registered");
            }
            if (botten("guh_npc_" + id).isEmpty() || bron("/assets/guhs/geckolib/models/entity/guh_npc_" + id + ".geo.json") == null
                    || E2SpCastGameTests.class.getResourceAsStream("/assets/guhs/textures/entity/npc_" + id + ".png") == null) {
                fout.add(id + ": its model or texture is missing");
            }
        }
        helper.assertTrue(KroegBewoners.PLEKKEN.size() == 9 && KroegBewoners.PLEKKEN.get("slapende_mika") == GuhNpcEntity.Kind.SLAPENDE_MIKA,
                "nine Kroeg residents, the sleeper on his spot");
        // every animation a registered Sponsguh scene names on a cast actor or a Kroeg Mika can play
        int genoemd = 0;
        for (Cutscene s : Cutscene.alle()) {
            if (!s.id().startsWith("einde2_sp")) {
                continue;
            }
            for (Cutscene.Animatie an : s.animaties()) {
                Cutscene.Acteur a = s.acteur(an.acteur());
                if (a == null || a.type() == null || ENGINE.contains(an.naam())) {
                    continue;
                }
                EntityType<?> type = a.type().get();
                Cast cast = Cast.van(type);
                if (cast != null) {
                    genoemd++;
                    if (!cast.heeft(an.naam())) {
                        fout.add(s.id() + " t" + an.t() + ": " + cast.id() + " has no animation " + an.naam() + " (it has " + cast.animaties() + ")");
                    }
                } else if (type == ModEntities.GUH_NPC.get() && a.nbt() != null) {
                    CompoundTag tag = new CompoundTag();
                    a.nbt().accept(tag);
                    for (KroegMikas r : KroegMikas.values()) {
                        if (r.kind.id().equals(tag.getStringOr("Kind", "")) && r.duur(an.naam()) < 0) {
                            fout.add(s.id() + ": " + r.kind.id() + " has no animation " + an.naam());
                        }
                    }
                }
            }
        }
        org.slf4j.LoggerFactory.getLogger("guhs").info("[einde2 sp_cast] {} cast animations named by Sponsguh scenes checked", genoemd);
        fout.forEach(f -> org.slf4j.LoggerFactory.getLogger("guhs").warn("[einde2 sp_cast] {}", f));
        helper.assertTrue(fout.isEmpty(), fout.size() + " animations cannot play, first: " + (fout.isEmpty() ? "" : fout.get(0)));
        // a cast actor reads a scene's animation like the guh NPCs do (1.4.4: held through the click wait, not restarted)
        SponsguhWezen sponsguh = E2SponsguhFeature.type(Cast.SPONSGUH).create(level, EntitySpawnReason.COMMAND);
        net.minecraft.world.phys.Vec3 at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        sponsguh.snapTo(at.x, at.y, at.z, 0f, 0f);
        try {
            Cutscenes.wisAnimaties();
            Cutscenes.zetGepauzeerd(false, level.getGameTime());
            helper.assertTrue(sponsguh.sceneAnimatie().isEmpty() && !sponsguh.sceneBegonnen(), "no scene: nothing");
            Cutscenes.zetAnimatie(sponsguh, "zing");
            helper.assertTrue(sponsguh.sceneAnimatie().equals("zing") && sponsguh.sceneBegonnen(), "a scene's zing starts");
            helper.assertTrue(sponsguh.sceneAnimatie().equals("zing") && !sponsguh.sceneBegonnen(), "and keeps going");
            Cutscenes.zetGepauzeerd(true, level.getGameTime());
            helper.assertTrue(sponsguh.sceneAnimatie().equals("zing") && !sponsguh.sceneBegonnen(), "the click wait holds it");
            Cutscenes.zetGepauzeerd(false, level.getGameTime() + 30);
            helper.assertTrue(sponsguh.sceneAnimatie().equals("zing") && !sponsguh.sceneBegonnen(), "the click does not start it again");
            Cutscenes.zetAnimatie(sponsguh, "salueer");
            helper.assertTrue(sponsguh.sceneAnimatie().equals("salueer") && sponsguh.sceneBegonnen(), "another name starts");
        } finally {
            Cutscenes.wisAnimaties();
            Cutscenes.zetGepauzeerd(false, level.getGameTime());
        }
        helper.assertTrue(sponsguh.sceneAnimatie().isEmpty(), "the scene is over: nothing");
        helper.succeed();
    }
}
