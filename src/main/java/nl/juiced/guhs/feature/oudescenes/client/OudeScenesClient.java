package nl.juiced.guhs.feature.oudescenes.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.ClientClockManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.balto.client.Sneeuwstorm;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.hemel.client.HemelClient;
import nl.juiced.guhs.feature.oudescenes.Effecten;
import nl.juiced.guhs.feature.oudescenes.OudeScene;
import nl.juiced.guhs.feature.oudescenes.OudeScenes;
import nl.juiced.guhs.feature.oudescenes.Scenes;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler;

/**
 * Client side of bbq2 (oude-scenes): what the six scenes show besides the verhaal engine's script ({@link Effecten}), for
 * the viewer ONLY. Nothing here is sent to or asked of the server, so nobody else sees somebody's snowstorm, thunderstorm,
 * night, or the portal that goes dark for a moment; and a replay from the Guhdex looks the same as the first time.
 * <ul>
 *   <li>the white of the storm: a fog that closes to {@code zicht} blocks (the Balto scene: barely three), and guh-snow
 *       through balto's own {@link Sneeuwstorm} (source "oudescenes");</li>
 *   <li>rain and thunder: this game's own copy of the level's rain and thunder levels, put back at the end (what the
 *       server says in the meantime is remembered); lightning = the sky flashes;</li>
 *   <li>night: this game's own copy of the dimension's clock stands at midnight with a full moon, and goes on from the
 *       real time at the end;</li>
 *   <li>the streams of particles (the beam of light, the flame that runs round the frame, the eyes in the tank...);</li>
 *   <li>blocks that look different for a moment, in this game's own copy of the world: the Grillguh's portal is dark until
 *       the flame has run round ({@link Scenes#GRILL_AAN}), the front door of the huisje opens for the first resident
 *       ({@link Scenes#TIMMER_DEUR_OPEN}); the Knuffelhart starts to beat at {@link Scenes#HEMEL_KLOP}.</li>
 * </ul>
 * Everything is driven by the scene's own tick ({@link CutsceneSpeler#tijd}). Where the scene stands in the world is read
 * from its actors ({@link #vind}): the engine tells nobody else.
 */
public final class OudeScenesClient {
    private static final String BRON = "oudescenes";

    private static OudeScene actief;
    private static int laatsteTijd;
    // where the scene stands
    private static boolean plekBekend;
    private static BlockPos anker = BlockPos.ZERO;
    private static Rotation draai = Rotation.NONE;
    // the white
    private static float zicht, oudZicht;
    private static boolean sneeuwt;
    // rain and thunder
    private static boolean weerAan;
    private static float echtRegen, echtDonder, gezetRegen, gezetDonder;
    // night
    private static boolean nachtAan;
    private static long echtKlok, echtKlokTijd, nachtKlok;
    private static float klokTempo = 1f;
    private static long gemetenKlok = Long.MIN_VALUE, gemetenTijd;
    // particles
    private static float[] rest = new float[0];
    /** A block that looks different in this game for a moment: what is really there, and what we show. */
    private record Schijn(BlockState echt, BlockState getoond) {
    }

    private static final Map<BlockPos, Schijn> ECHT = new HashMap<>();
    private static boolean portaalDonker, deurOpen, hartGezet, hartEcht;

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, OudeScenesClient::mist);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, OudeScenesClient::mistKleur);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> vergeet());
    }

    /** The scene of ours that plays now (null: none). */
    public static OudeScene actief() {
        return actief;
    }

    // =====================================================================================================================
    // every tick
    // =====================================================================================================================

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            vergeet();
            return;
        }
        OudeScene nu = CutsceneSpeler.actief() ? OudeScenes.van(CutsceneSpeler.scene()) : null;
        if (nu != actief) {
            if (actief != null) {
                einde(level);
            }
            actief = nu;
            if (nu != null) {
                begin(nu);
            }
        }
        if (actief == null) {
            meetKlok(level);
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        int t = CutsceneSpeler.tijd();
        laatsteTijd = t;
        Effecten e = actief.effecten();
        if (!plekBekend) {
            plekBekend = vind(level, actief.scene(), t);
        }
        weer(level, e, t);
        if (plekBekend) {
            stromen(level, e, t);
            blokken(level, t);
        }
    }

    private static void begin(OudeScene s) {
        plekBekend = false;
        laatsteTijd = 0;
        zicht = oudZicht = 0;
        rest = new float[s.effecten().stromen().size()];
        portaalDonker = deurOpen = hartGezet = false;
        ECHT.clear();
    }

    /** The scene is over (or broken off): everything of this game goes back to what it really is. */
    private static void einde(ClientLevel level) {
        OudeScene s = actief;
        actief = null;
        zicht = oudZicht = 0;
        if (sneeuwt) {
            Sneeuwstorm.uit(BRON);
            sneeuwt = false;
        }
        if (weerAan) {
            level.setRainLevel(echtRegen);
            level.setThunderLevel(echtDonder);
            weerAan = false;
        }
        if (nachtAan) {
            dag(level);
        }
        zetTerug(level);
        // (a heart that started to beat in a scene that was watched to the end keeps beating: the server says so right after;
        // one that was broken off goes back to what it was)
        if (hartGezet && s != null && laatsteTijd < s.scene().duur() - 2) {
            HemelClient.status(hartEcht);
        }
        hartGezet = false;
    }

    /** The level is gone (logout, another world): forget everything, there is nothing to put back. */
    private static void vergeet() {
        actief = null;
        plekBekend = false;
        zicht = oudZicht = 0;
        if (sneeuwt) {
            Sneeuwstorm.uit(BRON);
            sneeuwt = false;
        }
        weerAan = nachtAan = false;
        portaalDonker = deurOpen = hartGezet = false;
        ECHT.clear();
        gemetenKlok = Long.MIN_VALUE;
    }

    // =====================================================================================================================
    // where the scene stands: read from its actors
    // =====================================================================================================================

    /**
     * Finds the scene's anchor block and rotation: the anchor that puts every actor of the script on one of the actor
     * entities of this game. (The engine keeps its anchor to itself; the actors are where the script says, turned like the
     * building.) False: not found (yet).
     */
    private static boolean vind(ClientLevel level, Cutscene scene, int t) {
        List<Entity> acteurs = new ArrayList<>();
        for (Entity e : level.entitiesForRendering()) {
            if (CutsceneSpeler.isActeur(e)) {
                acteurs.add(e);
            }
        }
        if (acteurs.isEmpty() || scene.acteurs().isEmpty()) {
            return false;
        }
        // an actor that stands still now is exactly where the script says
        Cutscene.Acteur vast = null;
        for (Cutscene.Acteur a : scene.acteurs()) {
            if (!scene.looptNu(a.naam(), t) && !scene.looptNu(a.naam(), t - 4)) {
                vast = a;
                break;
            }
        }
        if (vast == null) {
            return false;
        }
        Vec3 rel = scene.plek(vast.naam(), t);
        for (Entity e : acteurs) {
            for (Rotation r : Rotation.values()) {
                Vec3 a = e.position().subtract(Cutscene.wereld(BlockPos.ZERO, r, rel));
                BlockPos blok = new BlockPos(Mth.floor(a.x + 0.5), Mth.floor(a.y + 0.5), Mth.floor(a.z + 0.5));
                if (Math.abs(a.x - blok.getX()) > 0.05 || Math.abs(a.y - blok.getY()) > 0.05 || Math.abs(a.z - blok.getZ()) > 0.05) {
                    continue;
                }
                if (past(scene, acteurs, blok, r, t)) {
                    anker = blok;
                    draai = r;
                    return true;
                }
            }
        }
        return false;
    }

    /** Do the actors of the script stand where this anchor and rotation put them? (One may be missing: an entity that could not be made.) */
    private static boolean past(Cutscene scene, List<Entity> acteurs, BlockPos blok, Rotation r, int t) {
        int er = 0, n = scene.acteurs().size();
        for (Cutscene.Acteur a : scene.acteurs()) {
            Vec3 hoort = Cutscene.wereld(blok, r, scene.plek(a.naam(), t));
            double marge = scene.looptNu(a.naam(), t) || scene.looptNu(a.naam(), t - 4) ? 9.0 : 0.36;
            for (Entity e : acteurs) {
                if (e.position().distanceToSqr(hoort) <= marge) {
                    er++;
                    break;
                }
            }
        }
        return er >= Math.max(Math.min(2, n), n - 1);
    }

    private static Vec3 wereld(Vec3 rel) {
        return Cutscene.wereld(anker, draai, rel);
    }

    /** A template block of the scene's building (relative to the anchor block) in the world. */
    private static BlockPos wereldBlok(BlockPos rel) {
        return anker.offset(StructureTemplate.transform(rel, Mirror.NONE, draai, BlockPos.ZERO));
    }

    // =====================================================================================================================
    // weather, night, lightning
    // =====================================================================================================================

    private static void weer(ClientLevel level, Effecten e, int t) {
        oudZicht = zicht;
        zicht = e.kanaal(t, Effecten.ZICHT);
        float sneeuw = e.kanaal(t, Effecten.SNEEUW);
        if (sneeuw > 0.01f) {
            // (the wind blows from the north-west, as it does in the tundra)
            Sneeuwstorm.zet(BRON, sneeuw, 0.85f, 0.45f, 0f);
            sneeuwt = true;
        } else if (sneeuwt) {
            Sneeuwstorm.uit(BRON);
            sneeuwt = false;
        }
        if (e.heeft(Effecten.REGEN) || e.heeft(Effecten.DONDER)) {
            if (!weerAan) {
                echtRegen = level.rainLevel;
                echtDonder = level.thunderLevel;
                weerAan = true;
            } else {
                // (the server changed the weather while we held it: that is what it really is now)
                if (Math.abs(level.rainLevel - gezetRegen) > 1e-4f) {
                    echtRegen = level.rainLevel;
                }
                if (Math.abs(level.thunderLevel - gezetDonder) > 1e-4f) {
                    echtDonder = level.thunderLevel;
                }
            }
            gezetRegen = Math.max(echtRegen, e.kanaal(t, Effecten.REGEN));
            gezetDonder = Math.max(echtDonder, e.kanaal(t, Effecten.DONDER));
            level.setRainLevel(gezetRegen);
            level.setThunderLevel(gezetDonder);
        }
        if (e.kanaal(t, Effecten.NACHT) > 0.5f) {
            nacht(level);
        } else if (nachtAan) {
            dag(level);
        }
        for (int flits : e.flitsen()) {
            if (flits == t) {
                level.setSkyFlashTime(3);
            }
        }
    }

    /** While no scene of ours plays: how fast this dimension's clock really runs (a server may have stopped the day). */
    private static void meetKlok(ClientLevel level) {
        Optional<Holder<WorldClock>> klok = level.dimensionType().defaultClock();
        if (klok.isEmpty()) {
            return;
        }
        long nu = level.clockManager().getTotalTicks(klok.get()), tijd = level.getGameTime();
        if (gemetenKlok != Long.MIN_VALUE && tijd > gemetenTijd) {
            float tempo = (nu - gemetenKlok) / (float) (tijd - gemetenTijd);
            if (tempo >= 0f && tempo <= 4f) {
                klokTempo = tempo;
            }
        }
        gemetenKlok = nu;
        gemetenTijd = tijd;
    }

    /** This game's own copy of the dimension's clock stands at midnight (a full moon), for as long as this is called. */
    private static void nacht(ClientLevel level) {
        Optional<Holder<WorldClock>> klok = level.dimensionType().defaultClock();
        if (klok.isEmpty()) {
            return;
        }
        ClientClockManager manager = level.clockManager();
        long nu = manager.getTotalTicks(klok.get());
        if (!nachtAan || nu != nachtKlok) {
            // (the first time, or the server set the clock while we held it: this is the real time)
            echtKlok = nu;
            echtKlokTijd = level.getGameTime();
            nachtKlok = Math.floorDiv(nu, 192000L) * 192000L + 18000L;
            nachtAan = true;
        }
        manager.handleUpdates(level.getGameTime(), Map.of(klok.get(), new ClockNetworkState(nachtKlok, 0f, 0f)));
    }

    /** The clock goes on from the real time. */
    private static void dag(ClientLevel level) {
        nachtAan = false;
        Optional<Holder<WorldClock>> klok = level.dimensionType().defaultClock();
        if (klok.isEmpty()) {
            return;
        }
        long nu = echtKlok + Math.round((level.getGameTime() - echtKlokTijd) * (double) klokTempo);
        level.clockManager().handleUpdates(level.getGameTime(), Map.of(klok.get(), new ClockNetworkState(nu, 0f, klokTempo)));
        gemetenKlok = Long.MIN_VALUE;
    }

    /** The white closes in: nothing is left of the world at {@code zicht} blocks (only ever nearer than the game's own fog). */
    private static void mist(ViewportEvent.RenderFog event) {
        if (actief == null || zicht <= 0.01f || event.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float z = oudZicht > 0.01f ? Mth.lerp((float) event.getPartialTick(), oudZicht, zicht) : zicht;
        FogData fog = event.getFogData();
        if (z < fog.environmentalEnd) {
            fog.environmentalEnd = z;
            fog.environmentalStart = Math.min(fog.environmentalStart, z * 0.08f);
        }
        fog.skyEnd = Math.min(fog.skyEnd, z);
        fog.cloudEnd = Math.min(fog.cloudEnd, z);
    }

    /** ... and it is snow-white (the thicker, the whiter). */
    private static void mistKleur(ViewportEvent.ComputeFogColor event) {
        if (actief == null || zicht <= 0.01f) {
            return;
        }
        float m = Mth.clamp(1f - (zicht - 4f) / 60f, 0f, 1f);
        event.setRed(Mth.lerp(m, event.getRed(), 0.90f));
        event.setGreen(Mth.lerp(m, event.getGreen(), 0.93f));
        event.setBlue(Mth.lerp(m, event.getBlue(), 0.98f));
    }

    // =====================================================================================================================
    // particles
    // =====================================================================================================================

    private static void stromen(ClientLevel level, Effecten e, int t) {
        RandomSource r = level.getRandom();
        Vec3 nul = Cutscene.wereld(BlockPos.ZERO, draai, Vec3.ZERO);
        List<Effecten.Stroom> stromen = e.stromen();
        for (int i = 0; i < stromen.size() && i < rest.length; i++) {
            Effecten.Stroom s = stromen.get(i);
            if (t < s.van() || t >= s.tot()) {
                continue;
            }
            rest[i] += s.perTick();
            Vec3 v = Cutscene.wereld(BlockPos.ZERO, draai, s.snelheid()).subtract(nul);
            while (rest[i] >= 1f) {
                rest[i] -= 1f;
                Vec3 p = wereld(s.op(t + r.nextDouble()));
                level.addAlwaysVisibleParticle(s.deeltje().get(), true, p.x + r.nextGaussian() * s.spreiding(), p.y + r.nextGaussian() * s.spreiding(),
                        p.z + r.nextGaussian() * s.spreiding(), v.x, v.y, v.z);
            }
        }
    }

    // =====================================================================================================================
    // blocks that look different for a moment (in this game's own copy of the world)
    // =====================================================================================================================

    private static void blokken(ClientLevel level, int t) {
        if (actief == OudeScenes.GRILL) {
            if (!portaalDonker && t < Scenes.GRILL_AAN) {
                // the fire of the frame (and of any frame right next to it) is out until the flame has run round
                portaalDonker = true;
                BlockPos frame = wereldBlok(Scenes.GRILL_FRAME.subtract(Scenes.GRILL_ANKER));
                for (BlockPos pos : BlockPos.betweenClosed(frame.offset(-6, -3, -6), frame.offset(6, 6, 6))) {
                    BlockState state = level.getBlockState(pos);
                    if (state.is(BarbecuetherFeature.BARBECUETHER_PORTAAL.get())) {
                        toon(level, pos.immutable(), Blocks.AIR.defaultBlockState());
                    }
                }
            } else if (portaalDonker && t >= Scenes.GRILL_AAN && !ECHT.isEmpty()) {
                zetTerug(level);
            }
        } else if (actief == OudeScenes.TIMMER) {
            if (!deurOpen && t >= Scenes.TIMMER_DEUR_OPEN) {
                deurOpen = true;
                BlockPos deur = wereldBlok(Scenes.TIMMER_DEUR.subtract(Scenes.TIMMER_ANKER));
                BlockState state = level.getBlockState(deur);
                if (state.getBlock() instanceof DoorBlock && !state.getValue(DoorBlock.OPEN)) {
                    BlockPos ander = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? deur.above() : deur.below();
                    BlockState anderState = level.getBlockState(ander);
                    toon(level, deur, state.setValue(DoorBlock.OPEN, true));
                    if (anderState.getBlock() instanceof DoorBlock) {
                        toon(level, ander, anderState.setValue(DoorBlock.OPEN, true));
                    }
                }
            }
        } else if (actief == OudeScenes.HEMEL) {
            if (!hartGezet && t >= Scenes.HEMEL_KLOP) {
                hartGezet = true;
                hartEcht = HemelClient.klopt();
                HemelClient.status(true);
            }
        }
    }

    /** This game shows another block state here (the real one is remembered). */
    private static void toon(ClientLevel level, BlockPos pos, BlockState state) {
        ECHT.putIfAbsent(pos, new Schijn(level.getBlockState(pos), state));
        level.setBlock(pos, state, 19);
    }

    /**
     * Every block we changed that still looks as we left it goes back to what it really is (one that the server changed in
     * the meantime already shows the truth).
     */
    private static void zetTerug(ClientLevel level) {
        for (Map.Entry<BlockPos, Schijn> e : ECHT.entrySet()) {
            if (level.getBlockState(e.getKey()) == e.getValue().getoond()) {
                level.setBlock(e.getKey(), e.getValue().echt(), 19);
            }
        }
        ECHT.clear();
    }

    private OudeScenesClient() {
    }
}
