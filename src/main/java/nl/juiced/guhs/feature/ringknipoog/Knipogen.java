package nl.juiced.guhs.feature.ringknipoog;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.guhrio.Baan;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.mewtwo.MewtwoFeature;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ringh3.Mijn;
import nl.juiced.guhs.feature.ringh3.Plekken;
import nl.juiced.guhs.feature.ringh3.RingH3Feature;
import nl.juiced.guhs.feature.ringsausuman.Bakkerij;
import nl.juiced.guhs.feature.ringsausuman.RingSausumanFeature;
import nl.juiced.guhs.feature.ringsausuman.Toren;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.VerhaalPayloads;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-knipogen): the seven "knipogen" of the Knabbelring and Super Guhrio: winks at the older stories, each a
 * mini-cutscene of five to ten seconds with a guest from that story (texts: tools/features/ring_knipogen.py). A player
 * sees each wink ONCE, whether or not they did the old story; "seen" is the engine's own mark of the scene
 * ({@link Cutscenes#gezien}), so a wink that was cut off (a logout) simply comes again the next time its moment comes.
 * A wink never holds the story up: when it was seen already, or cannot be shown now, the story goes on without it.
 * <p>
 * The guests are the real characters: the scenes use their own entity types as actors and change nothing about them.
 * <ol>
 *   <li>{@link #BALTOGUH} in the Raad van Guhrond (ring-h2): Baltoguh walks into the council, everybody stares, he took a
 *       wrong turn. Right after the council scene ({@code Guhvendel.luid} plays both through {@link #speel}).</li>
 *   <li>{@link #KISTJE} in Super Guhrio: the medicine chest he was looking for sits in a ?-block of level 1-1
 *       ({@link #KISTJE_LEVEL}, the one at {@link #KISTJE_S}); when it pops out Pad-guh has a word. Found by looking at the
 *       player's own level every tick ({@link #kistje}); the level stands still during a cutscene.</li>
 *   <li>{@link #SPIEGEL} at Guhladriel's mirror (ring-h4): Mieuwguh floats giggling through the background and the mirror
 *       shows Mewtwo-guh at a double portion. Right after the mirror scene ({@code Spiegel.kijk}).</li>
 *   <li>{@link #STITCH} in the Toren van Sausuman: the 626-guh presses a button, a machine explodes in confetti. When the
 *       player comes back into the machine hall with their three ingredients ({@link #stitch}), before the baking scene.</li>
 *   <li>{@link #BORIS} on the Frituurberg (ring-h6): Boris the goose starts his famous line and Sam-guh cuts in. When the
 *       player clicks Sam-guh to be carried ({@code Klim.samKlik} through {@link #eerst}); he carries a moment later.</li>
 *   <li>{@link #SJOKKEL}: Sjokkel sets out over the bridge of the mine in chapter 3 ({@link Sjokkel}, no camera) and arrives
 *       at the feast of chapter 6. Right after the feast scene ({@code Thuis.seconde}).</li>
 *   <li>{@link #KLOON} at the gate of the mine (ring-h3): Professor Knabbelkloon proposes two rings, Guhdalf says no. Right
 *       after the chapter's narrator card, on the forecourt ({@link #kloon}).</li>
 * </ol>
 * Every scene is written in the frame of the scene it belongs to (its anchor block and the copy's rotation), in template
 * coordinates; the self-check of tools/features/ring_knipogen.py reads this file and proves against the templates that no
 * camera stands inside a block and nothing of the building stands between a camera and what it looks at.
 */
public final class Knipogen {
    /** The longest a wink may take: ten seconds. */
    public static final int MAX_TICKS = 200;
    /** Player saved data: the wink that started right after its scene and was not watched to the end yet. */
    public static final String OPEN = "guhs_ringknipoog_open";
    /** The level and the place along its first lane of the ?-block with the medicine chest. */
    public static final String KISTJE_LEVEL = "kasteel_1_1";
    public static final int KISTJE_S = 73;
    /** How near the ?-block (blocks) the player has to be for its wink to start. */
    public static final double KISTJE_BEREIK = 7.0;
    /** How near the middle of the Derde Richel (blocks) the player has to stand for Boris to come by. */
    public static final double BORIS_BEREIK = 9.0;
    /** A wink with something of the story right behind it waits until the player may be touched again, at most this long. */
    public static final int STRAKS_MAX = 200;

    public static Cutscene BALTOGUH, KISTJE, SPIEGEL, STITCH, BORIS, SJOKKEL, KLOON;

    /**
     * (tests) winks also play for a player whose game cannot show a cutscene. Without it such a player (the mock players of
     * the game tests, who would only stand locked for two ticks) gets no wink at all: the chapters' own tests run exactly as
     * they did before there were winks, and the tests of this package switch it on to walk through the real hooks.
     */
    public static boolean OOK_ZONDER_SCHERM;

    /** What comes right after a wink once the player may be touched again ({@link #eerst}). */
    private record Straks(Predicate<ServerPlayer> wat, long tot) {
    }

    private static final Map<UUID, Straks> STRAKS = new ConcurrentHashMap<>();
    /** (not saved) how many winks were started for each player since the server started: for the dev command and the tests. */
    private static final Map<UUID, Integer> GESTART = new ConcurrentHashMap<>();
    /** (not saved) the ?-block with the medicine chest of each level in play (BlockPos.ZERO: this level has none). */
    private static final Map<GuhrioSpel.Actief, BlockPos> KISTJES = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private Knipogen() {
    }

    /** (RingKnipoogFeature.register, both sides) registers the scenes. */
    static void registreer() {
        BALTOGUH = baltoguh();
        KISTJE = kistjeScene();
        SPIEGEL = spiegel();
        STITCH = stitchScene();
        BORIS = boris();
        SJOKKEL = sjokkel();
        KLOON = kloonScene();
    }

    /** The seven winks, in the order of the class text. */
    public static List<Cutscene> alle() {
        return List.of(BALTOGUH, KISTJE, SPIEGEL, STITCH, BORIS, SJOKKEL, KLOON);
    }

    // =====================================================================================================================
    // playing a wink
    // =====================================================================================================================

    /** Has this player seen this wink (to the end)? */
    public static boolean gezien(ServerPlayer p, Cutscene knipoog) {
        return Cutscenes.gezien(p, knipoog.id());
    }

    /** How many winks were started for this player since the server started (the dev command, the tests). */
    public static int gestart(ServerPlayer p) {
        return GESTART.getOrDefault(p.getUUID(), 0);
    }

    /** Can this player's game show a cutscene (every real client; a mock player only while {@link #OOK_ZONDER_SCHERM})? */
    public static boolean kanZien(ServerPlayer p) {
        return OOK_ZONDER_SCHERM || (!(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && p.connection != null
                && p.connection.hasChannel(VerhaalPayloads.Speel.TYPE));
    }

    /**
     * Shows the wink to a player who has not seen it. False (and nothing happens, {@code daarna} is not called): they saw it
     * already, their game cannot show it, or they are watching something else right now.
     */
    public static boolean toon(ServerPlayer p, Cutscene knipoog, BlockPos anker, Rotation draai, @Nullable Consumer<ServerPlayer> daarna) {
        if (gezien(p, knipoog) || !kanZien(p) || !Cutscenes.speel(p, knipoog, anker, draai, daarna)) {
            return false;
        }
        GESTART.merge(p.getUUID(), 1, Integer::sum);
        return true;
    }

    /**
     * A scene of the story with a wink right after it: use it instead of {@code Cutscenes.speel(p, scene, anker, draai,
     * daarna)}. The scene plays, then (once per player) the wink in the same frame, then {@code daarna}; a player who saw
     * the wink before gets the scene and {@code daarna} as always. When the wink cannot start, {@code daarna} runs at once:
     * the story never waits for a wink. A player who logged out DURING the wink has seen the long scene to its end: the
     * next time only the wink they still missed plays before {@code daarna}. False (nothing happens): the player is
     * watching something else, exactly as {@code Cutscenes.speel}.
     */
    public static boolean speel(ServerPlayer p, Cutscene scene, Cutscene knipoog, BlockPos anker, Rotation draai, @Nullable Consumer<ServerPlayer> daarna) {
        if (!gezien(p, knipoog) && Cutscenes.gezien(p, scene.id()) && knipoog.id().equals(GuhQuests.saved(p).getStringOr(OPEN, ""))) {
            return na(p, knipoog, anker, draai, daarna);
        }
        return Cutscenes.speel(p, scene, anker, draai, speler -> {
            if (!na(speler, knipoog, anker, draai, daarna) && daarna != null) {
                daarna.accept(speler);
            }
        });
    }

    /** The wink after its scene: true when it started ({@code daarna} follows it), false when there is nothing to wait for. */
    private static boolean na(ServerPlayer p, Cutscene knipoog, BlockPos anker, Rotation draai, @Nullable Consumer<ServerPlayer> daarna) {
        boolean gestart = toon(p, knipoog, anker, draai, speler -> {
            GuhQuests.saved(speler).remove(OPEN);
            if (daarna != null) {
                daarna.accept(speler);
            }
        });
        if (gestart) {
            GuhQuests.saved(p).putString(OPEN, knipoog.id());
        }
        return gestart;
    }

    /**
     * A wink right BEFORE something of the story (Sam-guh picking the player up): the wink plays (once per player) and
     * {@code daarna} follows as soon as the player may be touched again (a cutscene keeps its viewer safe for two more
     * seconds: {@link Duwtje#mag}). A player who saw the wink, or cannot watch now: {@code daarna} at once. Returns true
     * when the wink started, else what {@code daarna} said.
     */
    public static boolean eerst(ServerPlayer p, Cutscene knipoog, BlockPos anker, Rotation draai, Predicate<ServerPlayer> daarna) {
        if (toon(p, knipoog, anker, draai, speler -> STRAKS.put(speler.getUUID(), new Straks(daarna, speler.level().getGameTime() + STRAKS_MAX)))) {
            return true;
        }
        return daarna.test(p);
    }

    /**
     * Wink 5: the player clicked their Sam-guh to be carried up the last road of the Frituurberg. On the Derde Richel
     * (within {@link #BORIS_BEREIK} blocks of its spot {@code richel}) Boris has his say first, the first time; anywhere
     * else, and every later time, Sam-guh simply carries ({@code draag}).
     */
    public static boolean boris(ServerPlayer p, BlockPos richel, Rotation draai, Predicate<ServerPlayer> draag) {
        if (!p.onGround() || !richel.closerToCenterThan(p.position(), BORIS_BEREIK)) {
            return draag.test(p);
        }
        return eerst(p, BORIS, richel, draai, draag);
    }

    /** (every tick) what was put off until the player may be touched again. */
    private static void straks(ServerPlayer p) {
        Straks s = STRAKS.get(p.getUUID());
        if (s == null) {
            return;
        }
        boolean teLaat = p.level().getGameTime() > s.tot();
        if (teLaat || (Duwtje.mag(p) && !Cutscenes.bezig(p))) {
            STRAKS.remove(p.getUUID());
            if (!teLaat) {
                s.wat().test(p);
            }
        }
    }

    // =====================================================================================================================
    // the winks that find their own moment
    // =====================================================================================================================

    /** (every tick, a player) what was put off, and the ?-block of a player in a level. */
    static void tik(ServerPlayer p) {
        straks(p);
        kistje(p);
        if ((p.tickCount + p.getId()) % 20 == 13) {
            seconde(p);
        }
    }

    /** (once a second, a player; the tests call it themselves) the winks at the gate of the mine and in Sausuman's hall, and Sjokkel. */
    static void seconde(ServerPlayer p) {
        if (p.isSpectator() || !p.isAlive() || Cutscenes.bezig(p)) {
            return;
        }
        if (Duwtje.mag(p)) {
            kloon(p);
            stitch(p);
        }
        Sjokkel.seconde(p);
    }

    /**
     * Wink 7: a player at the gate riddle (step 1 of chapter 3: the narrator card is behind them) on the west forecourt of
     * the mine. Called the moment the card closes (a step listener) and once a second after (whoever missed it then).
     */
    static boolean kloon(ServerPlayer p) {
        Verhaallijn lijn = RingH3Feature.LIJN;
        if (gezien(p, KLOON) || lijn.stap(p) != 1 || !lijn.aanDeBeurt(p)) {
            return false;
        }
        Mijn m = Mijn.van(p);
        if (m == null || !Plekken.PLEIN.binnen(m.lokaal(p.blockPosition()))) {
            return false;
        }
        return toon(p, KLOON, m.wereld(Plekken.POORT_BUITEN), m.draai(), null);
    }

    /**
     * Wink 4: a player whose questline of the tower is at "pull the lever" (they fetched the three ingredients upstairs)
     * and who stands in the machine hall again, before the lever is pulled. The scene is anchored on the Ringenbakker like
     * the baking scene, so it fits every copy of the tower.
     */
    static boolean stitch(ServerPlayer p) {
        Verhaallijn lijn = RingSausumanFeature.LIJN;
        if (gezien(p, STITCH) || lijn.stap(p) != 2 || !lijn.aanDeBeurt(p) || lijn.vlag(p, Bakkerij.GEVULD) || !p.onGround()) {
            return false;
        }
        ServerLevel level = p.level();
        BlockPos bakker = null;
        for (BlockPos pos : BlockPos.betweenClosed(p.blockPosition().offset(-10, 0, -10), p.blockPosition().offset(10, 2, 10))) {
            if (level.getBlockState(pos).is(RingSausumanFeature.RINGENBAKKER.get())) {
                bakker = pos.immutable();
                break;
            }
        }
        if (bakker == null) {
            return false;
        }
        Rotation draai = Toren.draai(level.getBlockState(bakker).getValue(HorizontalDirectionalBlock.FACING));
        Vec3 lokaal = lokaal(bakker, draai, p.position());
        // the hall's floor in the machine's frame (the machine looks along +z): 7 wide, 10 deep, one below the machine's face
        if (lokaal.x < -3.0 || lokaal.x > 4.0 || lokaal.z < 1.0 || lokaal.z > 11.0 || Math.abs(lokaal.y + 1.0) > 0.6) {
            return false;
        }
        return toon(p, STITCH, bakker, draai, null);
    }

    /** A world position in the frame of an anchor block that is turned {@code draai} (the opposite of {@link Cutscene#wereld}). */
    static Vec3 lokaal(BlockPos anker, Rotation draai, Vec3 wereld) {
        Rotation terug = switch (draai) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> draai;
        };
        return StructureTemplate.transform(wereld.subtract(anker.getX(), anker.getY(), anker.getZ()), Mirror.NONE, terug, BlockPos.ZERO);
    }

    /**
     * Wink 2: the player's own level is 1-1 and they just bumped the ?-block with the medicine chest (it is "empty" for
     * them in this run): the chest pops out. The block keeps doing what a ?-block does (its coin is theirs); the level
     * stands still while the scene plays.
     */
    static boolean kistje(ServerPlayer p) {
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(p);
        if (s == null || !KISTJE_LEVEL.equals(s.level().level().id()) || gezien(p, KISTJE) || Cutscenes.bezig(p)) {
            return false;
        }
        BlockPos blok = kistjeBlok(s);
        if (blok == null || s.staat(blok) == 0 || !blok.closerToCenterThan(p.position(), KISTJE_BEREIK)) {
            return false;
        }
        Baan baan = s.level().banen().get(0);
        Vec3 camera = baan.naarCamera(baan.plek(blok.getX() + 0.5, blok.getZ() + 0.5).stuk());
        return toon(p, KISTJE, blok, naarCamera(Direction.getApproximateNearest(camera.x, 0, camera.z)), null);
    }

    /** The ?-block with the medicine chest of this session's level: the coin block of the first lane at {@link #KISTJE_S}, or null. */
    @Nullable
    public static BlockPos kistjeBlok(GuhrioSpel.Sessie s) {
        BlockPos blok = KISTJES.computeIfAbsent(s.actief, actief -> {
            for (GuhrioSpel.Stuk stuk : actief.stukken) {
                BlockState state = stuk.state();
                if (stuk.baan() == 0 && Math.abs(stuk.s() - KISTJE_S) < 0.25 && state.getBlock() instanceof GuhrioBlocks.VraagBlok
                        && state.getValue(GuhrioBlocks.INHOUD) == GuhrioBlocks.Inhoud.MUNT) {
                    return stuk.pos();
                }
            }
            return BlockPos.ZERO;
        });
        return BlockPos.ZERO.equals(blok) ? null : blok;
    }

    /** The rotation that turns a scene's +z towards this side (the side a level's camera stands on). */
    static Rotation naarCamera(Direction kant) {
        return switch (kant) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    static void vergeet(UUID speler) {
        STRAKS.remove(speler);
        GESTART.remove(speler);
    }

    static void wisAlles() {
        STRAKS.clear();
        GESTART.clear();
        KISTJES.clear();
    }

    /** (dev command, tests) this player has seen no wink at all. */
    public static void wis(ServerPlayer p) {
        for (Cutscene knipoog : alle()) {
            Cutscenes.vergeet(p, knipoog.id());
        }
        GuhQuests.saved(p).remove(OPEN);
        GuhQuests.saved(p).remove(Sjokkel.GEZIEN);
        vergeet(p.getUUID());
    }

    // =====================================================================================================================
    // actors
    // =====================================================================================================================

    private static ListTag floats(float... waarden) {
        ListTag list = new ListTag();
        for (float f : waarden) {
            list.add(FloatTag.valueOf(f));
        }
        return list;
    }

    /** An item as an actor: an item display of this size that always faces the camera. */
    static Consumer<CompoundTag> ding(String item, float schaal) {
        return tag -> {
            CompoundTag stapel = new CompoundTag();
            stapel.putString("id", item);
            stapel.putInt("count", 1);
            tag.put("item", stapel);
            tag.putString("item_display", "ground");
            tag.putString("billboard", "vertical");
            CompoundTag vorm = new CompoundTag();
            vorm.put("translation", floats(0f, 0f, 0f));
            vorm.put("left_rotation", floats(0f, 0f, 0f, 1f));
            vorm.put("scale", floats(schaal, schaal, schaal));
            vorm.put("right_rotation", floats(0f, 0f, 0f, 1f));
            tag.put("transformation", vorm);
        };
    }

    /** A button on the north side of its cell as an actor (a block display: its corner is the actor's position). */
    static Consumer<CompoundTag> knop() {
        return tag -> {
            CompoundTag blok = new CompoundTag();
            blok.putString("Name", "minecraft:polished_blackstone_button");
            CompoundTag eigenschappen = new CompoundTag();
            eigenschappen.putString("face", "wall");
            eigenschappen.putString("facing", "north");
            eigenschappen.putString("powered", "false");
            blok.put("Properties", eigenschappen);
            tag.put("block_state", blok);
        };
    }

    /** A guh of this variant as small as a vision in a bowl of water (the vanilla scale attribute, as a guh saves it). */
    static Consumer<CompoundTag> kleineGuh(GuhVariant variant, double schaal) {
        return tag -> {
            tag.putString("Variant", variant.id());
            CompoundTag maat = new CompoundTag();
            maat.putString("id", "minecraft:scale");
            maat.putDouble("base", schaal);
            ListTag lijst = new ListTag();
            lijst.add(maat);
            tag.put("attributes", lijst);
        };
    }

    /** A burst of confetti at this spot of a scene. */
    private static void confetti(Cutscene.Builder s, int t, Vec3 plek, int aantal, double spreiding) {
        for (int kleur : new int[]{0xFF73BF, 0xFFD940, 0x8CE6BF, 0x80B2FF, 0xBF80FF}) {
            s.deeltjes(t, new DustParticleOptions(kleur, 1.3f), plek, aantal, spreiding);
        }
    }

    // =====================================================================================================================
    // 1. Baltoguh in the Raad van Guhrond
    // =====================================================================================================================

    /**
     * The council ring of Guhvendel as the council scene leaves it (anchor: the stone table, RingH2Scenes' frame: the high
     * seat at z -5, the entrance at x -7): they are still arguing when Baltoguh trots in from the bridge.
     */
    private static Cutscene baltoguh() {
        final String balto = "balto", speler = Cutscene.SPELER;
        String[] raad = {"guhdalf", "araguh", "leguhlas", "gimguh", "boromika"};
        Vec3 bijBalto = new Vec3(-3.4, 0.6, 0.6);
        Cutscene.Builder s = Cutscene.maak("ringknipoog_baltoguh").duur(196).bij("ring_h2").kaart("ring_h2").verbergEcht(14)
                .npc("guhrond", Kind.GUHROND, new Vec3(0.5, 0.5, -4.5), 0)
                .npc("guhdalf", Kind.GUHDALF, new Vec3(3.5, 0.5, -3.5), 45)
                .npc("araguh", Kind.ARAGUH, new Vec3(-2.5, 0.5, -3.5), 315)
                .npc("leguhlas", Kind.LEGUHLAS, new Vec3(5.5, 0.5, -0.5), 90)
                .npc("gimguh", Kind.GIMGUH, new Vec3(5.5, 0.5, 1.5), 90)
                .npc("boromika", Kind.BOROMIKA, new Vec3(3.5, 0.5, 4.5), 135)
                .speler(new Vec3(-0.9, 0, 0.5), 270)
                .acteur("ring", () -> EntityType.ITEM_DISPLAY, new Vec3(0.5, 1.3, 0.5), 0, ding("guhs:knabbelring", 1.4f))
                .guh(balto, GuhVariant.BALTOGUH, new Vec3(-6.8, 0, 0.5), 270)
                .zwart(-8, 4);
        // 1. the council, still at it
        s.camera(0, new Vec3(-3.4, 2.6, 4.4), new Vec3(0.5, 1.0, -0.5))
                .camera(40, new Vec3(-3.6, 2.9, 4.3), new Vec3(0.5, 1.0, -0.5))
                .geluid(6, () -> SoundEvents.VILLAGER_NO, 0.7f, 0.9f)
                .geluid(24, () -> SoundEvents.VILLAGER_NO, 0.7f, 1.2f);
        for (String wie : raad) {
            s.animatie(wie, 1, "ruzie");
        }
        s.animatie("guhrond", 1, "schud");
        // 2. somebody walks in; everybody looks
        s.loop(balto, 24, 64, new Vec3(-3.4, 0, 0.6))
                .cameraKnip(42, new Vec3(-1.3, 1.5, 2.9), new Vec3(-4.6, 0.6, 0.6))
                .camera(66, new Vec3(-1.3, 1.5, 2.9), new Vec3(-3.4, 0.6, 0.6))
                .geluid(44, BaltoFeature.SNUIF, 1.0f, 1.0f)
                .kijk(speler, 58, bijBalto).kijk("guhrond", 56, bijBalto).animatie("guhrond", 56, "");
        for (String wie : raad) {
            s.animatie(wie, 56, "").kijk(wie, 56, bijBalto);
        }
        // 3. he took a wrong turn
        s.kijk(balto, 66, new Vec3(0.5, 1.0, 0.5))
                .cameraKnip(70, new Vec3(-1.5, 0.9, 1.9), new Vec3(-3.4, 0.55, 0.6))
                .camera(164, new Vec3(-1.7, 0.95, 2.1), new Vec3(-3.4, 0.55, 0.6))
                .animatie(balto, 72, "kijk")
                .zeg(72, balto, "afslag", 92)
                .animatie(balto, 110, "praat")
                .animatie(balto, 150, "schaam");
        // 4. and off he goes again; the council picks up where it was
        s.cameraKnip(166, new Vec3(3.6, 2.2, 3.0), new Vec3(0.5, 1.2, 0.5))
                .animatie(balto, 166, "")
                .loop(balto, 166, 194, new Vec3(-6.8, 0, 0.5))
                .geluid(180, () -> SoundEvents.VILLAGER_NO, 0.7f, 1.0f);
        for (String wie : raad) {
            s.animatie(wie, 178, "ruzie");
        }
        return s.zwart(186, 196).registreer();
    }

    // =====================================================================================================================
    // 2. the medicine chest in a ?-block
    // =====================================================================================================================

    /**
     * In a level's own frame with the ?-block as anchor (as guhrio-w2's egg scene): +z towards the side the level's camera
     * stands on (open air for eleven blocks), x along the lane, the ground you walk on at y -3. Pad-guh pops out of the
     * ground beside the block, two cells along the lane.
     */
    private static Cutscene kistjeScene() {
        Vec3 inBlok = new Vec3(0.5, 0.25, 0.5), boven = new Vec3(0.5, 1.45, 0.5), inGrond = new Vec3(2.4, -4.9, 0.5), opGrond = new Vec3(2.4, -3.0, 0.5);
        return Cutscene.maak("ringknipoog_kistje").duur(130).bij("guhrio").verbergEcht(3)
                .speler(new Vec3(0.5, -3, 0.5), 270)
                .acteur("kistje", () -> EntityType.ITEM_DISPLAY, inBlok, 0, ding("guhs:nomguh_medicijnkist", 1.1f))
                .npc("padguh", Kind.PADGUH, inGrond, 90)
                .camera(0, new Vec3(1.0, -0.9, 6.4), new Vec3(1.0, -1.0, 0.5))
                .camera(130, new Vec3(1.2, -0.9, 5.6), new Vec3(1.2, -1.0, 0.5))
                // out it pops
                .loop("kistje", 6, 18, boven)
                .geluid(6, () -> SoundEvents.CHEST_OPEN, 1.0f, 1.5f)
                .deeltjes(10, ParticleTypes.HAPPY_VILLAGER, new Vec3(0.5, 1.7, 0.5), 8, 0.3)
                .animatie(Cutscene.SPELER, 20, "spring")
                // and so does Pad-guh
                .loop("padguh", 44, 52, opGrond)
                .kijk("padguh", 52, new Vec3(0.5, -3.0, 0.5))
                .geluid(46, () -> ModSounds.GUH_AMBIENT.get(), 1.0f, 1.5f)
                .deeltjes(46, ParticleTypes.POOF, new Vec3(2.4, -2.8, 0.5), 8, 0.2)
                .animatie("padguh", 56, "wijs")
                .zeg(58, "padguh", "zocht", 60)
                // the chest floats off to find its owner; Pad-guh ducks away again
                .loop("kistje", 100, 128, new Vec3(-3.5, 3.0, 0.5))
                .animatie("padguh", 118, "")
                .loop("padguh", 120, 128, inGrond)
                .registreer();
    }

    // =====================================================================================================================
    // 3. Mieuwguh and Mewtwo-guh at Guhladriel's mirror
    // =====================================================================================================================

    /**
     * The dell as the mirror scene leaves it (anchor: the mirror, Spiegel's frame: the dell's floor is y -1): Sam-guh dozes,
     * Mieuwguh floats by behind Guhladriel, and the bowl shows one more thing: a tiny Mewtwo-guh at two knabbels (a small
     * guh and two item displays that wait inside the mirror's foot until the camera looks into the bowl).
     */
    private static Cutscene spiegel() {
        final String gu = "guhladriel", mew = "mew", visioen = "visioen", speler = Cutscene.SPELER;
        Vec3 inVoet = new Vec3(0.5, -0.95, 0.5), opWater = new Vec3(0.5, 0.9, 0.5);
        Vec3 mewMidden = new Vec3(-5.0, 3.7, -5.4);
        return Cutscene.maak("ringknipoog_spiegel").duur(176).bij("ring_h4").kaart("ring_h4").verbergEcht(14)
                .speler(new Vec3(1.5, -1, 0.5), 90)
                .npc(gu, Kind.GUHLADRIEL, new Vec3(0.5, -1, -0.5), 0)
                .guh("sam", GuhVariant.SAM_GUH, new Vec3(4.5, -1, 1.5), 90)
                .acteur(mew, MewtwoFeature.MEW, new Vec3(-8.6, 3.5, -1.2), 225)
                .acteur(visioen, ModEntities.GUH, inVoet, 0, kleineGuh(GuhVariant.MEWTWO, 0.3))
                .acteur("portie1", () -> EntityType.ITEM_DISPLAY, inVoet, 0, ding("guhs:kaas_knabbels", 0.35f))
                .acteur("portie2", () -> EntityType.ITEM_DISPLAY, inVoet, 0, ding("guhs:kaas_knabbels", 0.35f))
                .zwart(-8, 4)
                .animatie("sam", 1, "slaap")
                // 1. somebody giggles in the background
                .camera(0, new Vec3(3.4, 0.5, 2.4), new Vec3(0.8, 0.5, 0.0))
                .camera(70, new Vec3(3.2, 0.55, 2.3), new Vec3(0.6, 0.6, -0.2))
                .loop(mew, 4, 68, new Vec3(-1.4, 4.0, -9.6))
                .geluid(12, MewtwoFeature.MEW_GIECHEL, 1.0f, 1.0f)
                .geluid(42, MewtwoFeature.MEW_GIECHEL, 1.0f, 1.15f)
                .deeltjes(34, ParticleTypes.NOTE, mewMidden, 3, 0.4)
                .zeg(14, mew, "giechel", 46)
                .kijk(speler, 22, mewMidden).kijk(gu, 26, mewMidden)
                .animatie(gu, 32, "schud")
                // 2. and the mirror has one more thing to show
                .kijk(speler, 68, new Vec3(0.5, 0, 0.5)).kijk(gu, 68, new Vec3(0.5, 0, 0.5))
                .animatie(gu, 68, "")
                .cameraKnip(72, new Vec3(0.5, 3.4, 1.3), new Vec3(0.5, 0.6, 0.5))
                .camera(160, new Vec3(0.5, 2.4, 1.05), new Vec3(0.5, 0.6, 0.5))
                .animatie(speler, 74, "buk")
                .geluid(72, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.6f)
                .deeltjes(74, ParticleTypes.END_ROD, opWater, 12, 0.25)
                .loop(visioen, 76, 77, new Vec3(0.5, 0.72, 0.40)).kijk(visioen, 78, new Vec3(0.5, 0.72, 2.0))
                .loop("portie1", 76, 77, new Vec3(0.34, 0.74, 0.68))
                .loop("portie2", 76, 77, new Vec3(0.66, 0.74, 0.68))
                .animatie(visioen, 80, "eet")
                .geluid(84, () -> ModSounds.GUH_EAT.get(), 0.9f, 1.4f)
                .geluid(106, () -> ModSounds.GUH_EAT.get(), 0.9f, 1.6f)
                .geluid(128, () -> ModSounds.GUH_EAT.get(), 0.9f, 1.4f)
                .zeg(80, "", "visioen", 76)
                .deeltjes(156, ParticleTypes.ENCHANT, opWater, 20, 0.3)
                .loop(visioen, 160, 161, inVoet).loop("portie1", 160, 161, inVoet).loop("portie2", 160, 161, inVoet)
                .zwart(166, 176)
                .registreer();
    }

    // =====================================================================================================================
    // 4. the 626-guh in the Toren van Sausuman
    // =====================================================================================================================

    /**
     * The machine hall (anchor: the Ringenbakker's face, Bakkerij's frame: z into the hall, the floor is y -1, the door at
     * z 11). The 626-guh scuttles in through the door, presses a button on the Guh Oven of the wizard's machine row (the
     * block at (-3, -1, 8), its face to the north; the button is a block display on that face), the oven goes off in
     * confetti, and he is gone before Sausuman has turned round.
     */
    private static Cutscene stitchScene() {
        final String stitch = "stitch", sausuman = "sausuman", speler = Cutscene.SPELER;
        Vec3 machine = new Vec3(0.5, 0.4, 0.6), bijOven = new Vec3(-2.45, -1, 7.25), oven = new Vec3(-2.5, -0.4, 8.2), deur = new Vec3(0.5, -1, 10.4);
        Vec3 boem = new Vec3(-2.5, 0.5, 8.3);
        Cutscene.Builder s = Cutscene.maak("ringknipoog_stitch").duur(184).bij("ring_sausuman").verbergEcht(11)
                .speler(new Vec3(1.5, -1, 5.5), 135)
                .npc(sausuman, Kind.SAUSUMAN, new Vec3(-2.5, -1, 2.5), -45)
                .guh("sam", GuhVariant.SAM_GUH, new Vec3(2.6, -1, 6.9), 120)
                .guh(stitch, GuhVariant.STITCH626, deur, 180)
                .acteur("knop", () -> EntityType.BLOCK_DISPLAY, new Vec3(-3.0, -1.0, 7.0), 0, knop())
                .zwart(-8, 4);
        // 1. the wizard is busy; somebody blue comes in
        s.camera(0, new Vec3(2.9, 0.7, 4.2), new Vec3(-0.6, -0.4, 8.6))
                .camera(54, new Vec3(2.9, 0.7, 4.2), new Vec3(-2.2, -0.4, 7.6))
                .kijk(sausuman, 0, machine).animatie(sausuman, 4, "toover")
                .loop(stitch, 8, 46, bijOven).kijk(stitch, 47, oven)
                .kijk(speler, 30, bijOven).kijk("sam", 34, bijOven);
        // 2. a blue paw, a button
        s.cameraKnip(56, new Vec3(-0.9, 0.0, 6.3), new Vec3(-2.5, -0.45, 7.9))
                .camera(104, new Vec3(-1.0, 0.0, 6.4), new Vec3(-2.5, -0.45, 7.9))
                .animatie(stitch, 60, "grijp")
                .geluid(66, () -> SoundEvents.STONE_BUTTON_CLICK_ON, 1.0f, 1.0f)
                .zeg(70, stitch, "aloha", 36)
                .animatie(stitch, 76, "zwaai");
        // 3. the machine goes off
        s.cameraKnip(108, new Vec3(2.9, 1.0, 4.2), new Vec3(-2.2, 0.0, 8.0))
                .geluid(108, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 1.0f)
                .geluid(110, RingSausumanFeature.SPUTTER, 1.0f, 1.2f)
                .geluid(114, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f)
                .schud(108, 1.1f, 18)
                .deeltjes(108, ParticleTypes.POOF, boem, 10, 0.4)
                .deeltjes(110, ParticleTypes.FIREWORK, boem.add(0, 0.6, -0.4), 24, 0.7)
                .animatie(stitch, 110, "juich").animatie("sam", 110, "schrik").animatie(speler, 110, "spring")
                .animatie(sausuman, 108, "schrik").kijk(sausuman, 110, oven);
        confetti(s, 108, boem.add(0, 0.3, -0.3), 14, 0.7);
        confetti(s, 116, boem.add(0, 1.2, -0.8), 14, 1.0);
        // 4. the wizard has had it; the culprit is off
        s.cameraKnip(132, new Vec3(0.4, 0.3, 5.2), new Vec3(-2.5, 0.0, 2.5))
                .camera(176, new Vec3(0.2, 0.3, 5.0), new Vec3(-2.5, 0.0, 2.5))
                .animatie(sausuman, 134, "ruzie")
                .zeg(134, sausuman, "wie", 44)
                .animatie(stitch, 138, "")
                .loop(stitch, 140, 176, deur);
        return s.zwart(174, 184).registreer();
    }

    // =====================================================================================================================
    // 5. Boris on the Frituurberg
    // =====================================================================================================================

    /**
     * The Derde Richel (anchor: the spot "richel_3" of the mountain; the Rustvuurtje stands one block east of it, the low
     * wall along the drop at x 5, the cage at z 3; beyond the wall is the open air over the frituur sea). Boris flaps in along
     * the wall, starts on the line everybody knows from Nomguh, and Sam-guh will not have it: this is his moment.
     */
    private static Cutscene boris() {
        final String boris = "boris", sam = "sam", speler = Cutscene.SPELER;
        // (he stays inside the air that the mountain's template carves itself: what lies further out over the sea differs per world)
        Vec3 ver = new Vec3(9.5, 4.3, 5.5), bijMuur = new Vec3(5.6, 1.7, -1.5), weg = new Vec3(9.6, 4.6, 5.0);
        return Cutscene.maak("ringknipoog_boris").duur(192).bij("ring_h6").kaart("ring_h6").verbergEcht(12)
                .speler(new Vec3(0.5, 0, -1.5), 270)
                .guh(sam, GuhVariant.SAM_GUH, new Vec3(2.4, 0, -2.3), 270)
                .npc(boris, Kind.BORIS, ver, 90)
                .zwart(-8, 4)
                // 1. something white flaps in from over the sea
                .camera(0, new Vec3(-1.6, 1.7, 0.6), new Vec3(5.0, 2.6, -0.2))
                .camera(48, new Vec3(-1.4, 1.6, 0.4), new Vec3(5.2, 2.0, -1.2))
                .animatie(boris, 1, "vlieg")
                .loop(boris, 6, 46, bijMuur).kijk(boris, 47, new Vec3(0.5, 1.0, -1.5))
                .geluid(10, BaltoFeature.GAK, 1.0f, 1.0f)
                .kijk(speler, 20, bijMuur).kijk(sam, 20, bijMuur)
                // 2. the famous line
                .cameraKnip(50, new Vec3(2.9, 1.6, -0.4), new Vec3(5.6, 2.3, -1.5))
                .camera(116, new Vec3(3.1, 1.6, -0.5), new Vec3(5.6, 2.3, -1.5))
                .zeg(52, boris, "berg", 64)
                // 3. not on Sam-guh's watch
                .cameraKnip(118, new Vec3(4.2, 0.9, -3.6), new Vec3(2.4, 0.6, -2.3))
                .animatie(sam, 118, "schud")
                .zeg(120, sam, "moment", 46)
                .animatie(sam, 132, "praat")
                // 4. and Boris flaps off again
                .cameraKnip(168, new Vec3(-1.6, 1.7, 0.6), new Vec3(5.2, 2.2, -0.6))
                .animatie(sam, 168, "")
                .geluid(170, BaltoFeature.GAK, 1.0f, 0.8f)
                .loop(boris, 170, 190, weg)
                .zwart(182, 192)
                .registreer();
    }

    // =====================================================================================================================
    // 6. Sjokkel arrives at the feast
    // =====================================================================================================================

    /**
     * The feast in the Gouw as the feast scene leaves it (its anchor on open ground, Finale's frame). All the way from the
     * bridge of the mine, where he set out three chapters ago, comes Sjokkel: just in time for dessert.
     */
    private static Cutscene sjokkel() {
        final String sjokkel = "sjokkel", speler = Cutscene.SPELER;
        String[] gasten = {"guhdalf", "araguh", "leguhlas", "gimguh", "merrie", "pippguh", "boromika", "krokant", "sam"};
        Vec3 bijSjokkel = new Vec3(-2.6, 0.3, 5.6);
        Cutscene.Builder s = Cutscene.maak("ringknipoog_sjokkel").duur(180).bij("ring_h6").kaart("ring_h6").verbergEcht(14)
                .speler(new Vec3(0.5, 0, 3.5), 180)
                .guh("sam", GuhVariant.SAM_GUH, new Vec3(2.0, 0, 3.9), 180)
                .acteur("guhdalf", ModEntities.GUH_NPC, new Vec3(0.5, 0, -3.0), 0, Cast.acteur(Kind.GUHDALF, "wit"))
                .acteur("araguh", ModEntities.GUH_NPC, new Vec3(0.5, 0, -1.0), 180, Cast.acteur(Kind.ARAGUH, "kroon"))
                .npc("leguhlas", Kind.LEGUHLAS, new Vec3(-3.4, 0, 0.2), -70)
                .npc("gimguh", Kind.GIMGUH, new Vec3(-2.8, 0, 1.9), -60)
                .npc("merrie", Kind.MERRIE, new Vec3(4.2, 0, 0.2), 70)
                .npc("pippguh", Kind.PIPPGUH, new Vec3(3.6, 0, 1.9), 60)
                .npc("boromika", Kind.BOROMIKA, new Vec3(4.6, 0, -2.2), 50)
                .acteur("krokant", RingFeature.SMIKAGOL, new Vec3(-3.8, 0, -2.2), -50)
                .acteur(sjokkel, LanddiertjesFeature.SHUCKLE, new Vec3(-3.7, 0, 6.7), 215)
                .zwart(-8, 4);
        s.animatie("merrie", 1, "juich").animatie("pippguh", 1, "juich").animatie("gimguh", 1, "ruzie").animatie("leguhlas", 1, "lach");
        // 1. who is that, all the way back there?
        s.camera(0, new Vec3(-0.4, 0.5, 8.4), new Vec3(-2.9, 0.3, 5.9))
                .camera(64, new Vec3(-0.6, 0.55, 8.0), new Vec3(-2.6, 0.3, 5.6))
                .loop(sjokkel, 4, 176, new Vec3(-2.0, 0, 5.0))
                .geluid(8, LanddiertjesFeature.SHUCKLE_GELUID, 1.0f, 1.0f)
                .zeg(10, "", "ver", 54);
        // 2. it is Sjokkel
        s.cameraKnip(66, new Vec3(-1.0, 1.1, 2.4), new Vec3(-2.7, 0.3, 5.7))
                .camera(132, new Vec3(-1.1, 1.0, 2.7), new Vec3(-2.5, 0.3, 5.5))
                .kijk(speler, 66, bijSjokkel)
                .zeg(70, "", "sjokkel", 62);
        for (String wie : gasten) {
            s.animatie(wie, 66, "").kijk(wie, 66, bijSjokkel);
        }
        // 3. just in time for dessert
        s.cameraKnip(134, new Vec3(-7.0, 4.0, 8.0), new Vec3(0.0, 1.0, 2.0))
                .camera(178, new Vec3(-7.6, 4.4, 8.6), new Vec3(0.0, 1.0, 2.0))
                .zeg(136, "sam", "toetje", 40)
                .deeltjes(138, ParticleTypes.HEART, new Vec3(-2.3, 0.9, 5.3), 6, 0.4)
                .geluid(138, () -> ModSounds.GUH_HAPPY.get(), 1.0f, 1.2f)
                .geluid(152, LanddiertjesFeature.SHUCKLE_GELUID, 1.0f, 1.3f)
                .animatie(speler, 140, "zwaai");
        for (String wie : gasten) {
            s.animatie(wie, 136, "juich");
        }
        return s.zwart(170, 180).registreer();
    }

    // =====================================================================================================================
    // 7. Professor Knabbelkloon at the gate of the mine
    // =====================================================================================================================

    /**
     * The west forecourt of Knabbelmoria (anchor: the spot just outside the gate, Plekken.POORT_BUITEN; the gate wall is at
     * x 3, the fellowship waits where the template has it for the gate riddle). The professor of the kloon-eiland shuffles
     * up to Guhdalf with a plan.
     */
    private static Cutscene kloonScene() {
        final String kloon = "kloon", guhdalf = "guhdalf", speler = Cutscene.SPELER;
        Vec3 ver = new Vec3(-7.4, 0, 0.3), bijGuhdalf = new Vec3(-0.9, 0, -0.3), weg = new Vec3(-6.6, 0, 0.6);
        return Cutscene.maak("ringknipoog_kloon").duur(190).bij("ring_h3").kaart("ring_h3").verbergEcht(16)
                .acteur(guhdalf, ModEntities.GUH_NPC, new Vec3(0.5, 0, -1.5), 250, Cast.acteur(Kind.GUHDALF, "grijs"))
                .npc("gimguh", Kind.GIMGUH, new Vec3(-0.5, 0, 3.5), 290)
                .npc("leguhlas", Kind.LEGUHLAS, new Vec3(-3.5, 0, 4.5), 300)
                .npc("araguh", Kind.ARAGUH, new Vec3(-3.5, 0, -3.5), 240)
                .npc("boromika", Kind.BOROMIKA, new Vec3(-6.5, 0, 3.5), 290)
                .npc("merrie", Kind.MERRIE, new Vec3(-8.5, 0, -1.5), 100)
                .npc("pippguh", Kind.PIPPGUH, new Vec3(-9.5, 0, 0.5), 80)
                .speler(new Vec3(-4.5, 0, 1.6), 270)
                .guh("sam", GuhVariant.SAM_GUH, new Vec3(-5.6, 0, 2.5), 270)
                .npc(kloon, Kind.KNABBELKLOON, ver, 270)
                .zwart(-8, 4)
                // 1. somebody with a pile of notes shuffles up to the gate
                .camera(0, new Vec3(-6.4, 1.5, -1.0), new Vec3(-1.0, 0.9, -0.6))
                .camera(56, new Vec3(-5.8, 1.45, -1.0), new Vec3(-1.0, 0.9, -0.6))
                .loop(kloon, 8, 54, bijGuhdalf).kijk(kloon, 55, new Vec3(0.5, 1.0, -1.5))
                .geluid(12, () -> SoundEvents.BOOK_PAGE_TURN, 1.0f, 1.0f)
                .kijk(guhdalf, 30, new Vec3(-0.9, 1.0, -0.3)).kijk(speler, 30, new Vec3(-0.9, 1.0, -0.3))
                // 2. the plan
                .cameraKnip(58, new Vec3(-1.0, 1.25, 2.6), new Vec3(-0.2, 0.85, -1.0))
                .camera(118, new Vec3(-1.1, 1.25, 2.4), new Vec3(-0.2, 0.85, -1.0))
                .animatie(kloon, 60, "praat")
                .zeg(60, kloon, "twee", 58)
                // 3. the answer
                .cameraKnip(120, new Vec3(-0.7, 1.45, 0.4), new Vec3(0.5, 1.25, -1.5))
                .animatie(kloon, 120, "")
                .animatie(guhdalf, 122, "schud")
                .zeg(122, guhdalf, "nee", 30)
                // 4. he shuffles off with his notes
                .cameraKnip(154, new Vec3(-6.4, 1.5, -1.0), new Vec3(-1.0, 0.9, -0.6))
                .animatie(guhdalf, 154, "")
                .animatie(kloon, 154, "schaam")
                .loop(kloon, 156, 188, weg)
                .geluid(158, () -> SoundEvents.BOOK_PAGE_TURN, 1.0f, 0.8f)
                .zwart(180, 190)
                .registreer();
    }
}
