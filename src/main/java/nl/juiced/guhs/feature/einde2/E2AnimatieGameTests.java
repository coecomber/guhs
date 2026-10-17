package nl.juiced.guhs.feature.einde2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.einde2.castschurk.CastSchurk;
import nl.juiced.guhs.feature.einde2.castteam.CastTeam;
import nl.juiced.guhs.feature.einde2.castvolk.CastVolk;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestKinds;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2 integratie_b: the named animations of the guh characters in the einde2 cutscenes ({@code Builder.animatie} on an
 * {@code npc(...)} actor). Without a client this checks the two things that made them stand still:
 * <ul>
 * <li>every name a scene asks of a character is in that kind's table ({@link GuhNpcEntity#ANIMATIE_DUUR}) AND in the
 * animation file its renderer plays ({@code animation.guh_sitting.<name>}), the actor exists, and the engine's own four
 * ({@code zwaai buk sta spring}) need nothing;</li>
 * <li>the controller's reading of {@link Cutscenes#animatie} ({@link GuhNpcEntity#sceneAnimatie}): the animation holds
 * through a click wait and is not started again by the click; a new start of the same name does start it again.</li>
 * </ul>
 * It also writes every einde2 scene's running time to the log (for reports/cutscenes_lang.md).
 */
public class E2AnimatieGameTests {
    private static final String BATCH = "einde2_animatie";
    private static final Set<String> ENGINE = Set.of("zwaai", "buk", "sta", "spring");
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("guhs");

    /** The animation file the client renderer gives a kind (the same rules as the cast clients), null = none of einde2. */
    static String bestand(Kind kind) {
        if (CastTeam.rij(kind) != null) {
            return CastTeam.animatieBestand(kind);
        }
        for (CastVolk rij : CastVolk.values()) {
            if (rij.kind == kind) {
                return "guh_npc_" + rij.bestand.id();
            }
        }
        if (CastSchurk.rij(kind) != null) {
            return "guh_npc_" + CastSchurk.basis(kind).id();
        }
        if (GeestKinds.rij(kind) != null) {
            return "guh_npc_" + GeestKinds.basis(kind).id();
        }
        if (nl.juiced.guhs.feature.einde2.spcast.KroegMikas.rij(kind) != null) {
            return "guh_npc_" + kind.id();                     // the Mikas of De Ruige Kroeg (sp_cast): one file per Kind
        }
        return null;
    }

    private static final Map<String, String> BESTANDEN = new HashMap<>();

    static String inhoud(String bestand) {
        return BESTANDEN.computeIfAbsent(bestand, b -> {
            try (InputStream in = E2AnimatieGameTests.class.getResourceAsStream("/assets/guhs/geckolib/animations/entity/" + b + ".animation.json")) {
                return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "";
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2SceneAnimaties(GameTestHelper helper) {
        List<String> fout = new ArrayList<>();
        Map<String, String> lengtes = new TreeMap<>();
        int npcAnimaties = 0, scenes = 0;
        for (Cutscene s : Cutscene.alle()) {
            if (!s.id().startsWith("einde2_")) {
                continue;
            }
            scenes++;
            int zeg = 0, lied = 0;
            for (Cutscene.Zeg z : s.zinnen()) {
                if (z.lied()) {
                    lied++;
                } else {
                    zeg++;
                }
            }
            lengtes.put(s.id(), String.format(java.util.Locale.ROOT, "%.2f s (%d ticks), %d click lines, %d lied lines", s.duur() / 20.0, s.duur(), zeg, lied));
            Map<String, Kind> kinds = new HashMap<>();
            Set<String> acteurs = new java.util.HashSet<>();
            for (Cutscene.Acteur a : s.acteurs()) {
                acteurs.add(a.naam());
                if (a.type() != null && a.type().get() == ModEntities.GUH_NPC.get() && a.nbt() != null) {
                    CompoundTag tag = new CompoundTag();
                    a.nbt().accept(tag);
                    String id = tag.getStringOr("Kind", "");
                    for (Kind k : Kind.values()) {
                        if (k.id().equals(id)) {
                            kinds.put(a.naam(), k);
                        }
                    }
                    if (!kinds.containsKey(a.naam())) {
                        fout.add(s.id() + ": actor " + a.naam() + " has an unknown Kind '" + id + "'");
                    }
                }
            }
            for (Cutscene.Animatie an : s.animaties()) {
                if (!acteurs.contains(an.acteur())) {
                    fout.add(s.id() + " t" + an.t() + ": '" + an.naam() + "' for an actor that is not in the scene: " + an.acteur());
                    continue;
                }
                Kind kind = kinds.get(an.acteur());
                if (kind == null || ENGINE.contains(an.naam())) {
                    continue;                                      // (a creature reads it itself; or the engine's own move)
                }
                npcAnimaties++;
                if (!GuhNpcEntity.ANIMATIE_DUUR.containsKey(kind)) {
                    fout.add(s.id() + ": " + an.acteur() + " (" + kind.id() + ") has no animation table, '" + an.naam() + "' cannot play");
                    continue;
                }
                GuhNpcEntity probe = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.COMMAND);
                probe.setKind(kind);
                if (probe.duur(an.naam()) < 0) {
                    fout.add(s.id() + ": " + kind.id() + " has no animation '" + an.naam() + "' (its table: " + (GeestKinds.rij(kind) != null ? GeestKinds.animaties(kind) : CastTabellen.animaties(kind)) + ")");
                    continue;
                }
                String b = bestand(kind);
                if (b == null || !inhoud(b).contains("\"animation.guh_sitting." + an.naam() + "\"")) {
                    fout.add(s.id() + ": the file " + b + " of " + kind.id() + " holds no animation.guh_sitting." + an.naam());
                }
            }
        }
        LOG.info("[einde2 integratie_b] {} einde2 scenes, {} named character animations checked", scenes, npcAnimaties);
        lengtes.forEach((id, l) -> LOG.info("[einde2 scene] {} - {}", id, l));
        if (!fout.isEmpty()) {
            fout.forEach(f -> LOG.warn("[einde2 animatie] {}", f));
        }
        helper.assertTrue(scenes > 50, "the einde2 scenes are registered: " + scenes);
        helper.assertTrue(npcAnimaties > 50, "the scenes name character animations: " + npcAnimaties);
        helper.assertTrue(fout.isEmpty(), fout.size() + " named animations cannot play, first: " + (fout.isEmpty() ? "" : fout.get(0)));
        helper.succeed();
    }

    /** The controller's reading of a scene animation (what the client does with every frame), with a server entity. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void einde2SceneAnimatieKlik(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GuhNpcEntity aang = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
        aang.setKind(Kind.AANG_GUH);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        aang.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        try {
            Cutscenes.wisAnimaties();
            Cutscenes.zetGepauzeerd(false, level.getGameTime());
            helper.assertTrue(aang.sceneAnimatie().isEmpty() && !aang.sceneBegonnen(), "no scene: nothing");
            Cutscenes.zetAnimatie(aang, "praat");
            helper.assertTrue(aang.sceneAnimatie().equals("praat") && aang.sceneBegonnen(), "a scene's praat starts");
            helper.assertTrue(aang.sceneAnimatie().equals("praat") && !aang.sceneBegonnen(), "and keeps playing, not from the top every frame");
            long t = level.getGameTime();
            Cutscenes.zetGepauzeerd(true, t);
            helper.assertTrue(Cutscenes.animatie(aang).isEmpty(), "(the engine: every actor rests during the wait)");
            helper.assertTrue(aang.sceneAnimatie().equals("praat") && !aang.sceneBegonnen(), "the wait for a click holds it");
            Cutscenes.zetGepauzeerd(false, t + 40);
            helper.assertTrue(aang.sceneAnimatie().equals("praat") && !aang.sceneBegonnen(), "the click does not start it again");
            Cutscenes.zetAnimatie(aang, "lach");
            helper.assertTrue(aang.sceneAnimatie().equals("lach") && aang.sceneBegonnen(), "another name starts");
            helper.runAfterDelay(3, () -> {
                aang.sceneAnimatie();
                Cutscenes.zetAnimatie(aang, "lach");
                boolean opnieuw = aang.sceneAnimatie().equals("lach") && aang.sceneBegonnen();
                Cutscenes.wisAnimaties();
                helper.assertTrue(opnieuw, "the same name asked again starts from the top");
                helper.assertTrue(aang.sceneAnimatie().isEmpty(), "the scene is over: nothing");
                helper.assertTrue(EntityType.getKey(aang.getType()).getPath().equals("guh_npc"), "a guh character");
                helper.succeed();
            });
        } catch (RuntimeException e) {
            Cutscenes.wisAnimaties();
            Cutscenes.zetGepauzeerd(false, level.getGameTime());
            throw e;
        }
    }
}
