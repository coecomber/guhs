package nl.juiced.guhs.feature.ringh5;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.ring.Gaven;
import nl.juiced.guhs.feature.ring.KnekelRuiterEntity;
import nl.juiced.guhs.feature.ring.Negen;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ring.Smikagol;
import nl.juiced.guhs.feature.ring.SmikagolEntity;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-h5): chapter 5 of the Knabbelring, "De Zwarte Roosterpoort", step by step and per player (the questline is
 * {@link RingH5Feature#LIJN}; the valley: tools/features/ring_h5_bouw.py; the spots: {@link Plekken}).
 * <pre>
 *  0 AANKOMEN   walk into the camp at the mouth of the valley          -> the narrator card
 *  1 BOROMIKA   talk to Boromika at the fire                            -> the scene of his moment
 *  2 SMIKAGOL   catch the thief at the provisions: Smikagol             -> he becomes your guide
 *  3 UITKIJK    follow him up the ridge                                 -> the scene: the gate, the Eye, its light
 *  4 ASVELD     cross het Asveld from hiding place to hiding place      -> the fire of the Slakkenhut
 *  5 VLAKTE     cross de Kale Vlakte as a rock when the light comes by  -> the fire of the Holte
 *  6 LAAN       de Schaduwlaan: blow the smoke away, pass the riders    -> the post of skulls
 *  7 WACHTHEK   put the ring on, slip past the Mika guards, take it off -> the fire at het Roosterpoortje
 *  8 POORTJE    walk up to the locked side door                         -> the scene: Guhdalf de Witte returns
 *  9 ACHTER     through the tunnel under the wall                       -> the last fire: the chapter is done
 * </pre>
 * Everything a step does it does only for the player who is exactly at that step ({@link Ring#aanZet}: a friend who walks
 * along solves nothing). Nothing in the valley changes for good: the side door is really open and only LOOKS barred to
 * whoever may not pass yet ({@code feature/bestaand/Schijn}), the smoke is a per-player flag.
 */
public final class Hoofdstuk {
    public static final int AANKOMEN = 0, BOROMIKA = 1, SMIKAGOL = 2, UITKIJK = 3, ASVELD = 4, VLAKTE = 5, LAAN = 6, WACHTHEK = 7, POORTJE = 8, ACHTER = 9;
    public static final int STAPPEN = 10;
    /** The questline flag: this player's Lichtflesje blew the smoke of the lane away. */
    public static final String ROOK_WEG = "rook_weg";
    /** The sleutel of Smikagol's talking screen and its two answers. */
    public static final String PRAAT_SMIKAGOL = "ringh5_smikagol";
    public static final int JA = 1, NEE = 2;
    /** Within this many blocks of a fire (or a post) a step counts as reached. */
    public static final double BIJ = 4.5;
    /** The Lichtflesje blows the smoke away from this near (blocks from the smoke's box). */
    public static final double LICHT_BEREIK = 10;
    /** From this far (blocks from a piece of the valley) the compass of a player at step 0 points at a way in. */
    public static final int NADER_BEREIK = 96;
    /** How far outside a way in the rock is counted (blocks), and how long the answer is kept (ticks). */
    public static final int INGANG_KIJK = 12, INGANG_ONTHOUD = 600;
    /** Sauce that welled up in the valley is filled in again this near a player (blocks, per axis). */
    public static final int SAUS_BEREIK = 7;

    /** (not saved) the copy each player of this chapter is in right now. */
    private static final Map<UUID, Terrein> HIER = new ConcurrentHashMap<>();
    /** (not saved) the copy a player at step 0 is walking up to, and per copy which way in is the most open (looked at now and then). */
    private static final Map<UUID, Terrein> NADERT = new ConcurrentHashMap<>();
    private static final Map<Terrein, long[]> BESTE_INGANG = new ConcurrentHashMap<>();
    /** (not saved) the step Smikagol is leading each player for. */
    private static final Map<UUID, Integer> LEIDT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BERICHT = new ConcurrentHashMap<>();

    private static Verhaallijn lijn() {
        return RingH5Feature.LIJN;
    }

    /** The copy this player (of this chapter) is in right now; null: not in the valley. */
    @Nullable
    public static Terrein hier(ServerPlayer p) {
        return HIER.get(p.getUUID());
    }

    /** Is this chapter this player's business right now (reached, not done)? */
    public static boolean bezig(ServerPlayer p) {
        return lijn().aanDeBeurt(p) && !lijn().klaar(p) && Ring.begonnen(p);
    }

    /**
     * Is this player past the gate for good: THEIR chapter 5 is done (or the whole story)? Then the valley leaves them
     * alone: the Eye does not look for them, the guards of het Wachthek and the riders in the lane let them walk. Sneaking
     * is the puzzle of this chapter, not a toll for every later visit: a player who finished it and has to go back (a copy
     * of the gate can stand closed in on its far side, CONTRACT_130 13.16) simply walks back through the valley.
     */
    public static boolean voorbij(ServerPlayer p) {
        return lijn().klaar(p) || Ring.klaar(p);
    }

    // =====================================================================================================================
    // once a second, per player
    // =====================================================================================================================

    static void seconde(ServerPlayer p) {
        if (!bezig(p) || p.isSpectator() || !(Ring.OVERAL || p.level().dimension() == BarbecuetherFeature.BARBECUETHER)) {
            vergeet(p.getUUID());
            return;
        }
        Terrein t = Terrein.bij(p.level(), p.blockPosition());
        if (t == null) {
            HIER.remove(p.getUUID());
            // (still walking up to it: from this far the compass already points at the best way in, not at the valley's middle)
            Terrein ver = lijn().stap(p) == AANKOMEN ? Terrein.zoek(p.level(), p.blockPosition(), NADER_BEREIK) : null;
            if (ver != null) {
                NADERT.put(p.getUUID(), ver);
            } else {
                NADERT.remove(p.getUUID());
            }
            return;
        }
        HIER.put(p.getUUID(), t);
        NADERT.remove(p.getUUID());
        int stap = lijn().stap(p);
        rook(p, t);
        dempSaus(p, t);
        if (!Duwtje.mag(p)) {
            return;   // (watching a scene, reading a card, talking)
        }
        Vec3 hier = p.position();
        switch (stap) {
            case AANKOMEN -> {
                if (t.in(Plekken.KAMP, hier)) {
                    lijn().begin(p);
                    Verteller.toon(p, Scenes.KAART, speler -> lijn().verder(speler, AANKOMEN));
                }
            }
            case SMIKAGOL -> smikagolBijDeProviand(p, t);
            case UITKIJK -> {
                if (bij(t, Plekken.UITKIJK, hier, 2.5)) {
                    speelOog(p, t);
                }
            }
            case ASVELD -> bereikt(p, t, Plekken.VUUR_SLAKKENHUT, ASVELD);
            case VLAKTE -> bereikt(p, t, Plekken.VUUR_HOLTE, VLAKTE);
            case LAAN -> bereikt(p, t, Plekken.SCHEDELPAAL, LAAN);
            case WACHTHEK -> bereikt(p, t, Plekken.VUUR_POORTJE, WACHTHEK);
            case POORTJE -> {
                if (t.in(Plekken.VOOR_DEUR, hier)) {
                    speelGuhdalf(p, t);
                }
            }
            case ACHTER -> {
                if (bij(t, Plekken.VUUR_ACHTER, hier, BIJ + 0.5)) {
                    lijn().verder(p, ACHTER);
                }
            }
            default -> {
            }
        }
        gids(p, t);
    }

    private static boolean bij(Terrein t, BlockPos lokaal, Vec3 hier, double afstand) {
        Vec3 daar = t.midden(lokaal);
        double dx = daar.x - hier.x, dz = daar.z - hier.z;
        return dx * dx + dz * dz <= afstand * afstand && Math.abs(daar.y - hier.y) <= 3;
    }

    private static void bereikt(ServerPlayer p, Terrein t, BlockPos lokaal, int stap) {
        if (bij(t, lokaal, p.position(), BIJ)) {
            lijn().verder(p, stap);
        }
    }

    // =====================================================================================================================
    // the steps
    // =====================================================================================================================

    /** A step was reached: Smikagol has something to say about it; the last one hands out the provisions for the climb. */
    static void opStap(ServerPlayer p, int oud, int nieuw) {
        LEIDT.remove(p.getUUID());
        if (nieuw > UITKIJK && nieuw < STAPPEN) {
            Smikagol.zeg(p, "quest.guhs.ringh5.smikagol.stap." + nieuw);
        }
        if (nieuw >= ACHTER) {
            Schijn.straks(p);   // (the bars of the side door are gone for this player)
        }
        if (nieuw >= STAPPEN && oud < STAPPEN) {
            Ring.behaald(p, "ring_h5_klaar");
            Smikagol.zeg(p, "quest.guhs.ringh5.smikagol.klaar");
            Terrein t = HIER.get(p.getUUID());
            if (t != null && t.isIn(p.level())) {
                // (the valley may stand with a side in solid rock: he names the way on with the least rock behind it)
                Smikagol.zeg(p, "quest.guhs.ringh5.smikagol.verder." + verder(p.level(), t));
            }
            Entity sam = Sam.van(p);
            if (lijn().eenmalig(p, "proviand")) {
                Minigames.give(p, new ItemStack(RingFeature.STOOFPOTJE.get(), 2));
                if (sam != null) {
                    GuhQuests.say(p, sam, "quest.guhs.ringh5.sam.klaar");
                }
            }
            HIER.remove(p.getUUID());
        }
    }

    /** Boromika at the camp fire was clicked. */
    static void klikBoromika(Entity boromika, ServerPlayer p) {
        Terrein t = Terrein.bij(p.level(), boromika.blockPosition());
        int stap = lijn().stap(p);
        if (t != null && Ring.aanZet(p, lijn(), BOROMIKA)) {
            speel(p, t, Scenes.BOROMIKA, speler -> lijn().verder(speler, BOROMIKA));
        } else if (Ring.klaar(p)) {
            GuhQuests.say(p, boromika, "quest.guhs.ringh5.boromika.na." + p.getRandom().nextInt(2));
        } else if (!lijn().aanDeBeurt(p) || stap <= AANKOMEN) {
            GuhQuests.say(p, boromika, "quest.guhs.ringh5.boromika.hallo");
        } else {
            GuhQuests.say(p, boromika, "quest.guhs.ringh5.boromika.spijt." + p.getRandom().nextInt(3));
        }
    }

    /** Step 2: something rustles at the provisions. Smikagol sits there; walk up to him (or click him) and he talks. */
    private static void smikagolBijDeProviand(ServerPlayer p, Terrein t) {
        Vec3 plek = t.midden(Plekken.SMIKAGOL_KAMP);
        if (!Smikagol.isGids(p)) {
            SmikagolEntity s = Smikagol.roep(p, plek);
            if (s != null) {
                s.leid(List.of(plek), Hoofdstuk::praatSmikagol);
                GuhQuests.hint(p, "quest.guhs.ringh5.geritsel");
            }
        }
    }

    /** Smikagol's plea: "niet slaan!" - with the two answers. */
    static void praatSmikagol(ServerPlayer p) {
        SmikagolEntity s = Smikagol.van(p);
        if (s == null || !Ring.aanZet(p, lijn(), SMIKAGOL)) {
            return;
        }
        Praat.open(p, s, PRAAT_SMIKAGOL, "quest.guhs.ringh5.smikagol.betrapt", new Object[0], new Praat.Optie(JA, "gui.guhs.ringh5.smikagol.ja"),
                new Praat.Optie(NEE, "gui.guhs.ringh5.smikagol.nee"));
    }

    /** The answer on Smikagol's plea. */
    static void antwoordSmikagol(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (!Ring.aanZet(p, lijn(), SMIKAGOL)) {
            return;
        }
        if (optie == JA) {
            Smikagol.zeg(p, "quest.guhs.ringh5.smikagol.zweer");
            Entity sam = Sam.van(p);
            if (sam != null) {
                GuhQuests.say(p, sam, "quest.guhs.ringh5.sam.vertrouw_niet");
            }
            Ring.behaald(p, "ring_h5_gids");
            lijn().verder(p, SMIKAGOL);
        } else if (optie == NEE) {
            Smikagol.zeg(p, "quest.guhs.ringh5.smikagol.toe_nou");
        }
    }

    /** A click on the guide: at step 2 his plea again; later what to do right here. */
    static boolean klikSmikagol(SmikagolEntity s, ServerPlayer p) {
        if (!bezig(p) || hier(p) == null) {
            return false;
        }
        int stap = lijn().stap(p);
        if (stap == SMIKAGOL) {
            praatSmikagol(p);
            return true;
        }
        if (stap >= UITKIJK && stap < STAPPEN) {
            GuhQuests.say(p, s, "quest.guhs.ringh5.smikagol.stap." + stap);
            return true;
        }
        return false;
    }

    /** Step 3: on the crest of the ridge: the first look at the Eye. */
    private static void speelOog(ServerPlayer p, Terrein t) {
        if (Ring.aanZet(p, lijn(), UITKIJK)) {
            speel(p, t, Scenes.OOG, speler -> lijn().verder(speler, UITKIJK));
        }
    }

    /** Step 8: at the locked side door: the Nine ride in and Guhdalf de Witte returns. */
    private static void speelGuhdalf(ServerPlayer p, Terrein t) {
        if (Ring.aanZet(p, lijn(), POORTJE)) {
            speel(p, t, Scenes.GUHDALF, speler -> {
                Negen.einde(speler);
                lijn().verder(speler, POORTJE);
            });
        }
    }

    /**
     * Plays one of the three scenes of the chapter at its own spot of this copy, with what the real Eye does meanwhile: for
     * the scene on the ridge its light sweeps het Asveld (the camera looks down on it), for Guhdalf's return it really
     * turns to the wall above the side door. {@code daarna}: what the step does when the scene was watched (null: a replay
     * by the op command). False: the player is watching something already.
     */
    static boolean speel(ServerPlayer p, Terrein t, Cutscene scene, @Nullable java.util.function.Consumer<ServerPlayer> daarna) {
        BlockPos anker = scene == Scenes.BOROMIKA ? Plekken.VUUR_KAMP : scene == Scenes.OOG ? Plekken.UITKIJK : Plekken.DEUR_SCENE;
        if (!Cutscenes.speel(p, scene, t.wereld(anker), t.draai(), daarna)) {
            return false;
        }
        OogEntity oog = oog(p.level(), t);
        if (scene == Scenes.OOG && oog != null) {
            oog.blik().toonVeld(p.level().getGameTime() + scene.duur());
        } else if (scene == Scenes.GUHDALF) {
            Ring.doeOm(p, false);
            Negen.einde(p);
            if (oog != null) {
                oog.blik().zet(t.midden(Plekken.DEUR_SCENE.offset(0, 18, 3)), scene.duur() + 20);
            }
        }
        return true;
    }

    /** The Eye of this copy (null: not loaded). */
    @Nullable
    public static OogEntity oog(ServerLevel level, Terrein t) {
        Vec3 plek = t.midden(Plekken.OOG);
        List<OogEntity> ogen = level.getEntitiesOfClass(OogEntity.class, new AABB(plek, plek).inflate(6), Entity::isAlive);
        return ogen.isEmpty() ? null : ogen.get(0);
    }

    // =====================================================================================================================
    // Smikagol leads the way
    // =====================================================================================================================

    /** The cells Smikagol leads along for a step (null: he just follows). */
    @Nullable
    private static List<BlockPos> route(int stap) {
        return switch (stap) {
            case UITKIJK -> Plekken.ROUTE_UITKIJK;
            case ASVELD -> {
                List<BlockPos> r = new ArrayList<>(Plekken.SCHUIL_A);
                r.add(Plekken.VUUR_SLAKKENHUT.offset(1, 0, 0));
                yield r;
            }
            case VLAKTE -> Plekken.ROUTE_B;
            case LAAN -> Plekken.ROUTE_LAAN;
            case WACHTHEK -> Plekken.ROUTE_HEK;
            case POORTJE -> Plekken.ROUTE_DEUR;
            case ACHTER -> Plekken.ROUTE_ACHTER;
            default -> null;
        };
    }

    /** Once per step the guide gets his route (he scurries ahead and waits when his player falls behind). */
    private static void gids(ServerPlayer p, Terrein t) {
        int stap = lijn().stap(p);
        List<BlockPos> route = route(stap);
        Integer leidt = LEIDT.get(p.getUUID());
        if (route == null || (leidt != null && leidt == stap) || !Smikagol.isGids(p)) {
            return;
        }
        SmikagolEntity s = Smikagol.van(p);
        if (s == null) {
            return;
        }
        LEIDT.put(p.getUUID(), stap);
        s.leid(t.route(route), stap == UITKIJK ? speler -> {
            Terrein daar = hier(speler);
            if (daar != null) {
                speelOog(speler, daar);
            }
        } : null);
    }

    // =====================================================================================================================
    // the smoke of the lane, the side door
    // =====================================================================================================================

    /** (once a second) the curtain of smoke hangs there for whoever has not blown it away yet: only they see it. */
    private static void rook(ServerPlayer p, Terrein t) {
        if (lijn().vlag(p, ROOK_WEG)) {
            return;
        }
        AABB doos = t.doos(Plekken.ROOK);
        Vec3 m = doos.getCenter();
        if (p.distanceToSqr(m) > 40 * 40) {
            return;
        }
        p.level().sendParticles(p, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, false, m.x, m.y - 0.5, m.z, 26, doos.getXsize() / 2.4, 1.2, doos.getZsize() / 2.4, 0.004);
        p.level().sendParticles(p, ParticleTypes.LARGE_SMOKE, true, false, m.x, m.y - 1.0, m.z, 30, doos.getXsize() / 2.2, 1.0, doos.getZsize() / 2.2, 0.01);
    }

    /**
     * (once a second) the valley has no sauce in it, but the world may let a spring of kaasfrituursaus well up in its rock
     * afterwards (it burns like lava). Whatever sauce is within {@link #SAUS_BEREIK} blocks of a player of this chapter, inside
     * the valley, is filled in again: rock below the floor, air above it.
     */
    static int dempSaus(ServerPlayer p, Terrein t) {
        ServerLevel level = p.level();
        BlockPos hier = p.blockPosition();
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(hier.offset(-SAUS_BEREIK, -3, -SAUS_BEREIK), hier.offset(SAUS_BEREIK, 4, SAUS_BEREIK))) {
            if (!level.getFluidState(pos).is(BarbecuetherFeature.KAASFRITUURSAUS.get()) && !level.getFluidState(pos).is(BarbecuetherFeature.FLOWING_KAASFRITUURSAUS.get())) {
                continue;
            }
            Vec3 l = t.lokaal(Vec3.atCenterOf(pos));
            if (l.x < 0 || l.z < 0 || l.x >= Plekken.MAAT.getX() || l.z >= Plekken.MAAT.getZ() || l.y < 0 || l.y >= Plekken.MAAT.getY()) {
                continue;
            }
            level.setBlock(pos, l.y < Plekken.VUUR_KAMP.getY() ? BarbecuetherFeature.HOUTSKOOLSTEEN.get().defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            n++;
        }
        return n;
    }

    /** (every other tick) what gently pushes a player back: the smoke they can't see through, the door that is still locked. */
    static void snel(ServerPlayer p) {
        Terrein t = HIER.get(p.getUUID());
        if (t == null || !t.isIn(p.level()) || Sluiers.passeert(p) || !Duwtje.mag(p)) {
            return;
        }
        Vec3 hier = p.position();
        if (!lijn().vlag(p, ROOK_WEG) && t.in(Plekken.ROOK, hier)) {
            Vec3 terug = t.midden(Plekken.ROOK_TERUG);
            Duwtje.duw(p, terug.subtract(hier), 0.9);
            bericht(p, "quest.guhs.ringh5.rook.dicht");
        }
        if (lijn().stap(p) < ACHTER && t.in(Plekken.TUNNEL, hier)) {
            // out again on the side they came in by (the valley side, or from behind the wall: no short cut either way)
            double z = t.lokaal(hier).z;
            int voor = Plekken.TUNNEL.get(0).getZ(), achter = Plekken.TUNNEL.get(1).getZ() + 1;
            boolean voorkant = z - voor <= achter - z;
            Vec3 buiten = voorkant ? t.midden(Plekken.DEUR_SCENE) : t.midden(new BlockPos(Plekken.DEUR_SCENE.getX(), Plekken.DEUR_SCENE.getY(), achter + 2));
            if (Math.min(z - voor, achter - z) > 2.5) {
                Duwtje.terug(p, p.level().dimension(), buiten, t.yaw(voorkant ? 0f : 180f));   // (deep in: a pearl, a glitch)
            } else {
                Duwtje.duw(p, buiten.subtract(hier), 0.8);
            }
            bericht(p, "quest.guhs.ringh5.poortje.dicht");
        }
    }

    /** A line above the hotbar, at most once per three seconds. */
    private static void bericht(ServerPlayer p, String key) {
        long nu = p.level().getGameTime();
        Long laatst = BERICHT.get(p.getUUID());
        if (laatst == null || nu - laatst >= 60 || nu < laatst) {
            BERICHT.put(p.getUUID(), nu);
            p.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** {@code Gaven.BIJ_LICHT}: a Lichtflesje flashed. Near the smoke of the lane: it is blown away, for this player. */
    static void licht(ServerPlayer p, Vec3 plek) {
        Terrein t = hier(p);
        if (t == null || lijn().vlag(p, ROOK_WEG)) {
            return;
        }
        AABB doos = t.doos(Plekken.ROOK).inflate(LICHT_BEREIK);
        if (!doos.contains(plek)) {
            return;
        }
        lijn().vlag(p, ROOK_WEG, true);
        Vec3 m = t.doos(Plekken.ROOK).getCenter();
        p.level().sendParticles(p, ParticleTypes.POOF, true, false, m.x, m.y, m.z, 60, 2.5, 1.2, 3.0, 0.08);
        p.level().sendParticles(p, ParticleTypes.END_ROD, true, false, m.x, m.y, m.z, 30, 2.5, 1.2, 3.0, 0.05);
        p.level().playSound(null, m.x, m.y, m.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 0.7f);
        p.sendSystemMessage(Component.translatable("quest.guhs.ringh5.rook.weg").withStyle(ChatFormatting.AQUA));
        Ring.behaald(p, "ring_h5_rook");
    }

    /**
     * {@code Schijn.Bron}: the bars in the side door, shown to every player of this chapter who has not seen Guhdalf open it
     * yet. The door itself is air: whoever is further in the story walks through.
     */
    static void deurSchijn(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        Terrein t = HIER.get(p.getUUID());
        if (t == null || !t.isIn(p.level()) || lijn().stap(p) >= ACHTER) {
            return;
        }
        BlockState tralies = BarbecuetherFeature.ROOSTERIJZER_TRALIES.get().defaultBlockState().setValue(IronBarsBlock.EAST, true)
                .setValue(IronBarsBlock.WEST, true).rotate(t.draai());
        BlockPos a = Plekken.DEUR.get(0), b = Plekken.DEUR.get(1);
        for (BlockPos lokaal : BlockPos.betweenClosed(a, b)) {
            BlockPos pos = t.wereld(lokaal);
            if (Schijn.dichtbij(p, pos)) {
                gewenst.put(pos, tralies);
            }
        }
    }

    // =====================================================================================================================
    // the Guhdex: what you need, what you get, where to go
    // =====================================================================================================================

    static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        return switch (stap) {
            case ASVELD, VLAKTE -> List.of(Verhaallijn.nodig("guhs:elfenmanteltje", Gaven.heeft(p, RingFeature.ELFENMANTELTJE.get()) ? 1 : 0, 1));
            case LAAN -> List.of(Verhaallijn.nodig("guhs:lichtflesje", Gaven.heeft(p, RingFeature.LICHTFLESJE.get()) ? 1 : 0, 1));
            case WACHTHEK -> List.of(Verhaallijn.nodig("guhs:knabbelring", Ring.heeft(p) ? 1 : 0, 1));
            default -> List.of();
        };
    }

    static List<VerhaalStand.Beloning> beloningen(ServerPlayer p) {
        return List.of(Verhaallijn.beloning("guhs:smikagol_spawn_egg", "gui.guhs.ringh5.beloning.gids", lijn().stap(p) > SMIKAGOL),
                Verhaallijn.beloning("guhs:ring_stoofpotje", lijn().klaar(p)));
    }

    /** Where the compass, Sam-guh and the objective point: the spot of the step once the valley is found, else the valley. */
    @Nullable
    static Doel doel(ServerPlayer p, int stap) {
        Terrein t = HIER.get(p.getUUID());
        if (t == null && stap == AANKOMEN) {
            t = NADERT.get(p.getUUID());
        }
        if (t == null || !t.isIn(p.level())) {
            return Ring.doel(5);
        }
        if (stap == AANKOMEN && !inBouw(t, p.position())) {
            // outside the valley: to the way in with the least rock in front of it
            return Doel.plek(t.dim(), t.wereld(Plekken.INGANGEN.get(besteIngang(p.level(), t, p.position()))), Component.translatable("gui.guhs.ringh5.doel.ingang"));
        }
        BlockPos lokaal = switch (stap) {
            case AANKOMEN, BOROMIKA -> Plekken.VUUR_KAMP;
            case SMIKAGOL -> Plekken.PROVIAND;
            case UITKIJK -> Plekken.UITKIJK;
            case ASVELD -> Plekken.VUUR_SLAKKENHUT;
            case VLAKTE -> Plekken.VUUR_HOLTE;
            case LAAN -> Plekken.SCHEDELPAAL;
            case WACHTHEK -> Plekken.VUUR_POORTJE;
            case POORTJE -> Plekken.DEUR_SCENE;
            default -> Plekken.VUUR_ACHTER;
        };
        return Doel.plek(t.dim(), t.wereld(lokaal), Component.translatable("gui.guhs.ringh5.doel." + Math.min(stap, ACHTER)));
    }

    /** Is this world position inside the build itself (its box, not the margin around it)? */
    static boolean inBouw(Terrein t, Vec3 wereld) {
        Vec3 l = t.lokaal(wereld);
        return l.x >= 0 && l.z >= 0 && l.y >= 0 && l.x < Plekken.MAAT.getX() && l.z < Plekken.MAAT.getZ() && l.y < Plekken.MAAT.getY();
    }

    /** Which way out of the build a way in points (x, z in the build): the mouth to -z, the tunnels to -x and +x. */
    private static final int[][] NAAR_BUITEN = {{0, -1}, {-1, 0}, {1, 0}};

    /**
     * How many blocks of rock lie right outside this way in ({@link Plekken#INGANGEN}), at head height, up to
     * {@link #INGANG_KIJK}: 0 = a cave comes right up to it. The valley is placed where the cave is most open, but it may
     * still stand with a side in solid rock (outside the build a player can dig, inside nobody can).
     */
    public static int rotsVoor(ServerLevel level, Terrein t, int ingang) {
        return rots(level, t, Plekken.INGANGEN.get(ingang), NAAR_BUITEN[ingang][0], NAAR_BUITEN[ingang][1]);
    }

    /** The same for a way on behind the wall ({@link Plekken#UITGANGEN}: the far mouth to +z, the tunnels to -x and +x). */
    public static int rotsAchter(ServerLevel level, Terrein t, int uitgang) {
        return rots(level, t, Plekken.UITGANGEN.get(uitgang), uitgang == 0 ? 0 : NAAR_BUITEN[uitgang][0], uitgang == 0 ? 1 : 0);
    }

    private static int rots(ServerLevel level, Terrein t, BlockPos van, int dx, int dz) {
        int rots = 0;
        while (rots < INGANG_KIJK) {
            BlockPos pos = t.wereld(van.offset(dx * (rots + 1), 1, dz * (rots + 1)));
            if (!level.isLoaded(pos) || !level.getBlockState(pos).blocksMotion()) {
                break;
            }
            rots++;
        }
        return rots;
    }

    /**
     * What Smikagol says about the way on when the chapter is done (the end of the lang key): the number of the way on with
     * the least rock behind it ({@link Plekken#UITGANGEN}; when it makes no difference the far mouth, then the tunnel next to
     * the last fire, then the one beyond the tower), or
     * "dicht" when all three stand in solid rock as far as is looked ({@link #INGANG_KIJK}): then he says so, and that the
     * player can dig on through the back wall (outside the build anybody can dig) or sneak back past the guards. (Merge of
     * the ring chapters: the first real copy that was looked at with the whole chain in the world, seed 20261099, stood
     * like that.)
     */
    static String verder(ServerLevel level, Terrein t) {
        int[] rots = new int[Plekken.UITGANGEN.size()];
        for (int i = 0; i < rots.length; i++) {
            rots[i] = rotsAchter(level, t, i);
        }
        return verder(rots);
    }

    static String verder(int... rots) {
        int beste = minste(rots);
        return rots[beste] >= INGANG_KIJK ? "dicht" : String.valueOf(beste);
    }

    /** The first index of the smallest number. */
    static int minste(int... getallen) {
        int beste = 0;
        for (int i = 1; i < getallen.length; i++) {
            if (getallen[i] < getallen[beste]) {
                beste = i;
            }
        }
        return beste;
    }

    /** The way in with the least rock in front of it (the nearest one when it makes no difference); kept for a while. */
    static int besteIngang(ServerLevel level, Terrein t, Vec3 hier) {
        long nu = level.getGameTime();
        long[] oud = BESTE_INGANG.get(t);
        if (oud != null && nu >= oud[1] && nu - oud[1] < INGANG_ONTHOUD) {
            return (int) oud[0];
        }
        int beste = 0;
        double besteScore = Double.MAX_VALUE;
        for (int i = 0; i < Plekken.INGANGEN.size(); i++) {
            double score = rotsVoor(level, t, i) * 1000.0 + t.midden(Plekken.INGANGEN.get(i)).distanceTo(hier);
            if (score < besteScore) {
                besteScore = score;
                beste = i;
            }
        }
        BESTE_INGANG.put(t, new long[]{beste, nu});
        return beste;
    }

    // =====================================================================================================================
    // the inhabitants (Bezetting makers)
    // =====================================================================================================================

    /** The Terrein of the copy in which the build block {@code lokaal} stands at the world position {@code plek}. */
    static Terrein terrein(ServerLevel level, BlockPos lokaal, Vec3 plek, Rotation draai) {
        BlockPos nul = BlockPos.containing(plek).subtract(StructureTemplate.transform(lokaal, Mirror.NONE, draai, BlockPos.ZERO));
        return new Terrein(level.dimension(), nul, draai);
    }

    /** The Eye in its socket (not yet added to the world). */
    @Nullable
    static OogEntity maakOog(ServerLevel level, Vec3 plek, Rotation draai) {
        OogEntity oog = RingH5Feature.OOG_VAN_SAUSRON.get().create(level, EntitySpawnReason.STRUCTURE);
        if (oog == null) {
            return null;
        }
        Terrein t = terrein(level, Plekken.OOG, plek, draai);
        float yaw = t.yaw(OogEntity.YAW);
        oog.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        oog.setYHeadRot(yaw);
        oog.setYBodyRot(yaw);
        oog.zetTerrein(t);
        return oog;
    }

    /** A guard of het Wachthek on his post, looking down the lane (east in the build). */
    @Nullable
    static RoosterwachterEntity maakWachter(ServerLevel level, BlockPos lokaal, Vec3 plek, Rotation draai) {
        RoosterwachterEntity w = RingH5Feature.ROOSTERWACHTER.get().create(level, EntitySpawnReason.STRUCTURE);
        if (w == null) {
            return null;
        }
        Terrein t = terrein(level, lokaal, plek, draai);
        w.snapTo(plek.x, plek.y, plek.z, 0f, 0f);
        w.zetPost(t.yaw(-90f));
        return w;
    }

    /** A rider of the Nine on its round in the lane; gone for whoever finished this chapter ({@link #voorbij}). */
    @Nullable
    static KnekelRuiterEntity maakRuiter(ServerLevel level, List<BlockPos> ronde, Vec3 plek, Rotation draai) {
        Terrein t = terrein(level, ronde.get(0), plek, draai);
        KnekelRuiterEntity r = Negen.maakPatrouille(level, ronde.stream().map(t::wereld).toList());
        if (r != null) {
            Zicht.alleenBij(r, lijn().id(), 0, STAPPEN - 1);
        }
        return r;
    }

    // =====================================================================================================================
    // housekeeping
    // =====================================================================================================================

    static void vergeet(UUID speler) {
        HIER.remove(speler);
        NADERT.remove(speler);
        LEIDT.remove(speler);
        BERICHT.remove(speler);
    }

    static void wisAlles() {
        HIER.clear();
        NADERT.clear();
        BESTE_INGANG.clear();
        LEIDT.clear();
        BERICHT.clear();
        Terrein.wisAlles();
    }

    private Hoofdstuk() {
    }
}
