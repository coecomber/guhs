package nl.juiced.guhs.feature.snuffelsteiger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.snuffel.Honden;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.Keuze;
import nl.juiced.guhs.feature.snuffel.Reis;
import nl.juiced.guhs.feature.snuffel.Snuffel;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The opening of Het Snuffeleiland, at the dock (DESIGN_VERHALENPAD C "Getting there"). Everything is per player: the
 * dock, its dogs and the boat are the same for everybody, and any number of players can be anywhere in the story at the
 * same dock. The two steps of the questline {@link SnuffelFeature#LIJN} that belong here:
 * <ol start="0">
 *   <li>{@link SnuffelFeature#STAP_STEIGER} "Vind een steigerhuisje": the first time a player sets foot on a dock the story
 *   begins for them ({@link #kijk}): the cutscene of the lantern feast and the collapse ({@link SteigerScenes#FEEST}).</li>
 *   <li>{@link SnuffelFeature#STAP_UITVAREN} "Kies je hond en vaar uit":
 *     <ul>
 *       <li>the sickbed (flag {@link #ZIEKBED}): Buurvrouw Mandje (or the puppy) tells about the snuffelkoorts, the
 *       geneesbloem and papa who has been gone for weeks; the player decides to go after him;</li>
 *       <li>the choice: Kapitein Zoutsnoet asks which dog they are and which companion comes along: the kern's choice
 *       screen ({@link Keuze#open}); they may choose again as often as they like;</li>
 *       <li>the crossing ({@link #vaarUit}): the cutscene with the storm and the jump ({@link SteigerScenes#OVERTOCHT}),
 *       and at its end (black) the player washes ashore on the island ({@link Snuffel#spoelAan}): the step is done there,
 *       and the island's own story takes over.</li>
 *     </ul>
 *   </li>
 * </ol>
 * Afterwards the captain sails a player to the island's harbour whenever they ask ({@link SteigerScenes#VAART}), and a
 * player who comes home to a dock (the island's captain or the memory card brought them back to where they left) sees
 * the boat bring them in ({@link SteigerScenes#THUISKOMST}).
 * <p>
 * WHO IS WHO: the family are dogs who live at the dock (the puppy, papa, the neighbours, the captain). In the Guhmensie
 * the player looks like they always do; on the island they are the dog of that family that they chose to be. The
 * neighbour says so in the sickbed talk.
 */
public final class SteigerVerhaal {
    /** The flag of the questline: the player heard the story at the sickbed and decided to go. */
    public static final String ZIEKBED = "ziekbed";
    /** How many ticks after coming home to a dock the boat scene starts (the client needs its world first). */
    public static final int THUIS_WACHT = 40;
    /** (Game tests: the test server has no Guhmensie) the story plays in every level. */
    public static boolean OVERAL;

    private static final String P = "snuffelsteiger_";
    static final String PRAAT_ZIEKBED = P + "ziekbed", PRAAT_WIE = P + "wie", PRAAT_KLAAR = P + "klaar", PRAAT_WEER = P + "weer";
    private static final String Q = "quest.guhs.snuffelsteiger.", O = "gui.guhs.snuffelsteiger.optie.";
    private static final int JA = 1, ANDERS = 2, NEE = 0;

    /** Who was a dog a tick ago, and who came home at which server tick. */
    private static final Set<UUID> WAS_HOND = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> THUIS = new ConcurrentHashMap<>();

    private SteigerVerhaal() {
    }

    static Verhaallijn lijn() {
        return SnuffelFeature.LIJN;
    }

    static void registreer() {
        Praat.luister(PRAAT_ZIEKBED, (p, spreker, optie) -> {
            if (optie == JA && lijn().stap(p) == SnuffelFeature.STAP_UITVAREN && !lijn().klaar(p)) {
                lijn().vlag(p, ZIEKBED, true);
                zeg(p, spreker, SteigerBewoner.BUUR, Q + "buur.dapper");
                GuhQuests.hint(p, Q + "hint.kapitein");
            }
        });
        Praat.luister(PRAAT_WIE, (p, spreker, optie) -> {
            if (optie == JA && magKiezen(p)) {
                Keuze.open(p);
            }
        });
        Praat.luister(PRAAT_KLAAR, (p, spreker, optie) -> {
            if (optie == JA) {
                vaarUit(p);
            } else if (optie == ANDERS && magKiezen(p)) {
                Keuze.open(p);
            }
        });
        Praat.luister(PRAAT_WEER, (p, spreker, optie) -> {
            if (optie == JA) {
                vaarUit(p);
            } else if (optie == ANDERS && magKiezen(p)) {
                Keuze.open(p);
            }
        });
        // the choice was made at a dock: the captain answers
        Keuze.opKeuze((p, keuze) -> {
            if (Steiger.oord(p.level(), p.blockPosition()) == null) {
                return;
            }
            Honden.Ras ras = Honden.ras(keuze.ras());
            zeg(p, null, SteigerBewoner.KAPITEIN, Q + "kapitein.gekozen", ras == null ? keuze.ras() : ras.naam(), keuze.naam(), Honden.maatjeNaam(keuze.maatje()));
        });
    }

    /** May this player choose their dog here and now (they heard the sickbed's story, and they stand at a dock as a player)? */
    static boolean magKiezen(ServerPlayer p) {
        return !Hondvorm.actief(p) && (lijn().klaar(p) || lijn().stap(p) > SnuffelFeature.STAP_UITVAREN
                || (lijn().stap(p) == SnuffelFeature.STAP_UITVAREN && lijn().vlag(p, ZIEKBED))) && Steiger.oord(p.level(), p.blockPosition()) != null;
    }

    // =====================================================================================================================
    // the start, per player
    // =====================================================================================================================

    /** Every tick for every player: who came home, and (once a second) who sets foot on a dock for the first time. */
    static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        thuis(p);
        if ((p.tickCount + p.getId()) % 20 == 0) {
            kijk(p);
        }
    }

    static void opLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WAS_HOND.remove(event.getEntity().getUUID());
        THUIS.remove(event.getEntity().getUUID());
    }

    /**
     * Does the story begin for this player now? Yes when they never began it and stand on a dock (its plot or its pier):
     * the feast plays, and at its end they are at the step of the sickbed. True when the scene started.
     */
    public static boolean kijk(ServerPlayer p) {
        ServerLevel level = p.level();
        if (!Steiger.inWereld(level) || lijn().klaar(p) || lijn().stap(p) != SnuffelFeature.STAP_STEIGER || !p.isAlive() || p.isSpectator()
                || Cutscenes.bezig(p) || Hondvorm.actief(p) || Minigames.playing(p) != null) {
            return false;
        }
        Steiger.Oord o = Steiger.oordVan(p);
        return o != null && feest(p, o);
    }

    /** The story begins: the feast and the collapse; after it the player is at the sickbed's step. */
    static boolean feest(ServerPlayer p, Steiger.Oord o) {
        lijn().begin(p);
        return Cutscenes.speel(p, SteigerScenes.FEEST, o.anker(), o.draai(), q -> {
            if (lijn().verder(q, SnuffelFeature.STAP_STEIGER)) {
                GuhQuests.hint(q, Q + "hint.ziekbed");
            }
        });
    }

    // =====================================================================================================================
    // talking
    // =====================================================================================================================

    /** A resident of the dock was clicked. */
    static void klik(SteigerBewoner npc, ServerPlayer p) {
        if (Cutscenes.bezig(p) || Hondvorm.actief(p)) {
            return;
        }
        if (!lijn().klaar(p) && lijn().stap(p) == SnuffelFeature.STAP_STEIGER) {
            // (somebody who reached a dog before the feast began for them: it begins now)
            Steiger.Oord o = Steiger.oord(p.level(), npc.blockPosition());
            if (o == null || !feest(p, o)) {
                GuhQuests.say(p, npc, Q + "feest_zo");
            }
            return;
        }
        switch (npc.rol()) {
            case SteigerBewoner.PUP -> pup(npc, p);
            case SteigerBewoner.BUUR -> buur(npc, p);
            case SteigerBewoner.KAPITEIN -> kapitein(npc, p);
            default -> {
            }
        }
    }

    /** The moored boat was clicked: a word with the captain, when he stands there. */
    static void klikBoot(SteigerBoot boot, ServerPlayer p) {
        SteigerBewoner kapitein = bewoner(p.level(), boot, SteigerBewoner.KAPITEIN);
        if (kapitein != null) {
            klik(kapitein, p);
        }
    }

    private static boolean aanBed(ServerPlayer p) {
        return !lijn().klaar(p) && lijn().stap(p) == SnuffelFeature.STAP_UITVAREN && !lijn().vlag(p, ZIEKBED);
    }

    private static void pup(SteigerBewoner npc, ServerPlayer p) {
        if (aanBed(p)) {
            ziekbed(npc, p);
        } else if (lijn().klaar(p)) {
            GuhQuests.say(p, npc, Q + "pup.spoor", naam(p));
        } else if (lijn().stap(p) == SnuffelFeature.STAP_UITVAREN) {
            GuhQuests.say(p, npc, Q + "pup.ga");
        } else {
            GuhQuests.say(p, npc, Q + "pup.slaapt", naam(p));
        }
    }

    private static void buur(SteigerBewoner npc, ServerPlayer p) {
        if (aanBed(p)) {
            ziekbed(npc, p);
        } else if (lijn().klaar(p)) {
            GuhQuests.say(p, npc, Q + "buur.spoor", naam(p));
        } else if (lijn().stap(p) == SnuffelFeature.STAP_UITVAREN) {
            GuhQuests.say(p, npc, Q + "buur.ga");
        } else {
            GuhQuests.say(p, npc, Q + "buur.terug", naam(p));
        }
    }

    /** The talk at the sickbed: five pages, the neighbour and the puppy; the last one asks. */
    private static void ziekbed(SteigerBewoner bij, ServerPlayer p) {
        Entity buur = bewoner(p.level(), bij, SteigerBewoner.BUUR), pup = bewoner(p.level(), bij, SteigerBewoner.PUP);
        Entity b = buur != null ? buur : bij, w = pup != null ? pup : bij;
        List<Praat.Regel> regels = new ArrayList<>();
        regels.add(new Praat.Regel(b, "", Q + "ziekbed.1"));
        regels.add(new Praat.Regel(b, "", Q + "ziekbed.2"));
        regels.add(new Praat.Regel(b, "", Q + "ziekbed.3"));
        regels.add(new Praat.Regel(w, "", Q + "ziekbed.4"));
        regels.add(new Praat.Regel(b, "", Q + "ziekbed.5"));
        regels.add(new Praat.Regel(b, "", Q + "ziekbed.6"));
        Praat.scene(p, PRAAT_ZIEKBED, regels, new Praat.Optie(JA, O + "achterna"), new Praat.Optie(NEE, O + "nadenken"));
    }

    private static void kapitein(SteigerBewoner npc, ServerPlayer p) {
        if (aanBed(p)) {
            GuhQuests.say(p, npc, Q + "kapitein.eerst_ziekbed");
            return;
        }
        boolean eerste = !lijn().klaar(p) && lijn().stap(p) == SnuffelFeature.STAP_UITVAREN;
        if (eerste && !Keuze.heeft(p)) {
            Praat.open(p, npc, PRAAT_WIE, Q + "kapitein.wie", new Object[0], new Praat.Optie(JA, O + "kies"), new Praat.Optie(NEE, O + "nog_niet"));
        } else if (eerste) {
            Praat.open(p, npc, PRAAT_KLAAR, Q + "kapitein.klaar", new Object[] {naam(p)}, new Praat.Optie(JA, O + "uitvaren"),
                    new Praat.Optie(ANDERS, O + "andere_hond"), new Praat.Optie(NEE, O + "nog_niet"));
        } else {
            Praat.open(p, npc, PRAAT_WEER, Q + "kapitein.weer", new Object[] {naam(p)}, new Praat.Optie(JA, O + "varen"),
                    new Praat.Optie(ANDERS, O + "andere_hond"), new Praat.Optie(NEE, O + "blijven"));
        }
    }

    /** The name of the player's dog (their own name before they chose). */
    private static String naam(ServerPlayer p) {
        return Keuze.vanOfStandaard(p).naam();
    }

    /** The dock's dog with this role near an entity (null: not there now). */
    @Nullable
    static SteigerBewoner bewoner(ServerLevel level, Entity bij, String rol) {
        SteigerBewoner beste = null;
        for (SteigerBewoner e : level.getEntitiesOfClass(SteigerBewoner.class, bij.getBoundingBox().inflate(40, 12, 40), x -> x.isAlive() && rol.equals(x.rol()))) {
            if (beste == null || e.distanceToSqr(bij) < beste.distanceToSqr(bij)) {
                beste = e;
            }
        }
        return beste;
    }

    /** A line of one of the dock's dogs: from the dog itself when it is near, else from the one that was asked. */
    private static void zeg(ServerPlayer p, @Nullable Entity spreker, String rol, String key, Object... args) {
        Entity wie = spreker instanceof SteigerBewoner s && rol.equals(s.rol()) ? spreker : bewoner(p.level(), p, rol);
        if (wie == null) {
            wie = spreker;
        }
        if (wie != null) {
            GuhQuests.say(p, wie, key, args);
        } else {
            p.sendSystemMessage(Component.translatable(key, args));
        }
    }

    // =====================================================================================================================
    // sailing out, coming home
    // =====================================================================================================================

    /**
     * The captain sails this player out from the dock they stand at: the first time the big crossing (it ends with the
     * player washed ashore on the island, a dog, and the step done), later a short one to the island's harbour. False: not
     * at a dock, not allowed yet, or the island cannot be reached now (the reason is on the screen).
     */
    public static boolean vaarUit(ServerPlayer p) {
        Steiger.Oord o = Steiger.oord(p.level(), p.blockPosition());
        if (o == null || Hondvorm.actief(p) || Cutscenes.bezig(p)) {
            return false;
        }
        boolean eerste = !lijn().klaar(p) && lijn().stap(p) == SnuffelFeature.STAP_UITVAREN;
        if (!lijn().klaar(p) && (lijn().stap(p) < SnuffelFeature.STAP_UITVAREN || (eerste && !lijn().vlag(p, ZIEKBED)))) {
            return false;
        }
        Component nee = Reis.weigering(p);
        if (nee != null) {
            p.sendOverlayMessage(nee.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        return Cutscenes.speel(p, eerste ? SteigerScenes.OVERTOCHT : SteigerScenes.VAART, o.anker(), o.draai(), q -> aanLand(q, eerste));
    }

    /** The scene's end (the screen is black): the player is on the island now. */
    static void aanLand(ServerPlayer p, boolean eerste) {
        if (eerste) {
            if (Snuffel.spoelAan(p)) {
                lijn().verder(p, SnuffelFeature.STAP_UITVAREN);
            }
        } else {
            Snuffel.vaarNaarEiland(p);
        }
    }

    /**
     * Who was a dog and is a player again came home. When home is a dock, the boat brings them in ({@link #THUIS_WACHT}
     * ticks later: the client must have its world back).
     */
    static void thuis(ServerPlayer p) {
        UUID id = p.getUUID();
        if (Hondvorm.actief(p)) {
            WAS_HOND.add(id);
            THUIS.remove(id);
            return;
        }
        int nu = p.level().getServer().getTickCount();
        if (WAS_HOND.remove(id)) {
            THUIS.put(id, nu);
        }
        Integer sinds = THUIS.get(id);
        if (sinds != null && nu - sinds >= THUIS_WACHT) {
            THUIS.remove(id);
            thuiskomst(p);
        }
    }

    /** The scene of the boat that brings this player home, when they stand at a dock (false: they do not, or cannot watch now). */
    public static boolean thuiskomst(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || Hondvorm.actief(p) || Cutscenes.bezig(p)) {
            return false;
        }
        Steiger.Oord o = Steiger.oordVan(p);
        return o != null && Cutscenes.speel(p, SteigerScenes.THUISKOMST, o.anker(), o.draai(), null);
    }

    /** (Tests) forget what is remembered about this player outside their own data. */
    static void vergeet(ServerPlayer p) {
        WAS_HOND.remove(p.getUUID());
        THUIS.remove(p.getUUID());
    }
}
