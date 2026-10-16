package nl.juiced.guhs.feature.einde2.trap5;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 tier 5 (CONTRACT_E2_B 7.12, DESIGN 6): the Werktuigguh's questline and the state every machine of the slice asks.
 * <p>
 * The line {@value #LIJN} (group "techniek", per player, all in main's {@link Verhaallijn}) is a row of PROJECTS: each one
 * leaves something working in the player's own world. A project is done the moment the player's own machine really does
 * its thing ({@link #meld}: a flag of the line, set whatever step the player is at), and the line steps forward by itself
 * as far as the flags allow ({@link #vooruit}), so building ahead never hurts and nobody has to run back to the workshop
 * after every project:
 * <ol start="0">
 *   <li>{@link #S_PRAAT} talk to the Werktuigguh (after the door scene): the Bouwplan of tier 5 and the first kit;</li>
 *   <li>{@link #S_ZENDER} a Vadsontvanger of yours feeds a machine from a Vadszender;</li>
 *   <li>{@link #S_TERMINAL} your bank through a Guhterminal;</li>
 *   <li>{@link #S_BESTEL} an order of a Bestelguh is delivered;</li>
 *   <li>{@link #S_POORTBUIS} something went through a Knabbelpoort-buis;</li>
 *   <li>{@link #S_THUISPOORT} a jump with a Thuispoort;</li>
 *   <li>{@link #S_ELEMENTEN} the four element machines each did their thing;</li>
 *   <li>{@link #S_BRONNEN} an Enderguhrad turns and the Komeetkern gives;</li>
 *   <li>{@link #S_TERUG} back to the Werktuigguh: he finally finishes something.</li>
 * </ol>
 * The line, the workshop door and the Komeetkern open only for a player who finished the Guhvatar story
 * ({@link E2Verhaal#finaleKlaar}). Texts: tools/features/einde2_trap5_machines_tekst.py.
 */
public final class E2Trap5 {
    public static final String LIJN = "werktuigguh";
    public static final int S_PRAAT = 0, S_ZENDER = 1, S_TERMINAL = 2, S_BESTEL = 3, S_POORTBUIS = 4, S_THUISPOORT = 5, S_ELEMENTEN = 6,
            S_BRONNEN = 7, S_TERUG = 8, STAPPEN = 9;

    /** The project flags (CONTRACT_E2_B 2.1: flags of the line, {@code guhs_werktuigguh_v_<naam>}). */
    public static final String P_ZENDER = "p_zender", P_TERMINAL = "p_terminal", P_BESTEL = "p_bestel", P_POORTBUIS = "p_poortbuis",
            P_THUISPOORT = "p_thuispoort", P_LUCHT = "p_lucht", P_WATER = "p_water", P_AARDE = "p_aarde", P_VUUR = "p_vuur",
            P_ENDERGUHRAD = "p_enderguhrad", P_KOMEET = "p_komeet";
    public static final List<String> ELEMENTEN = List.of(P_LUCHT, P_WATER, P_AARDE, P_VUUR);

    /** Once-only marks of the line ({@code eenmalig}). */
    static final String E_PLAN = "plan", E_KIT = "kit_", E_BELONING = "beloning";

    /** Per-player data of the slice that is no step (CONTRACT_E2_B 2.1): has picked up the Komeetkern. */
    public static final String KOMEET_GEPAKT = "guhs_einde2_trap5_machines_komeet";

    /** Client side: may the local player walk through the Werktuigguh's door (synced by {@link T5Payloads.Open}). */
    public static volatile boolean klantOpen;
    /** (dev only) the door is open for everybody: /guhs einde2 trap5 deur. */
    static volatile boolean devOpen;

    private static Verhaallijn lijn;

    private E2Trap5() {
    }

    /** The line (registered in {@link E2Trap5Feature#register}). */
    public static Verhaallijn lijn() {
        return lijn;
    }

    static void registreerLijn() {
        lijn = Verhaallijn.maak(LIJN, "techniek").stappen(STAPPEN).icoon("guhs:vadszender").doelregel(true)
                .nodig(E2Trap5::nodig)
                .beloningen(p -> List.of(Verhaallijn.beloning("guhs:" + E2Trap5Feature.BOUWPLAN_ID, heeftPlan(p)),
                        Verhaallijn.beloning("guhs:" + E2Trap5Feature.DIPLOMA_ID, lijn.klaar(p)),
                        Verhaallijn.beloning("guhs:thuispoort", "gui.guhs.einde2_trap5_machines.beloning.thuispoort", lijn.klaar(p))))
                .doel(E2Trap5::doel)
                .registreer();
    }

    private static boolean heeftPlan(ServerPlayer p) {
        return GuhQuests.saved(p).getBooleanOr("guhs_" + LIJN + "_e_" + E_PLAN, false);
    }

    // =====================================================================================================================
    // who may
    // =====================================================================================================================

    /** Tier 5 is open for this player: the Guhvatar story is finished (CONTRACT_E2_B 2.3). */
    public static boolean open(ServerPlayer p) {
        return E2Verhaal.finaleKlaar(p);
    }

    /**
     * {@code Einde2Haken.WERKPLAATS_OPEN}: asked by the door block on BOTH sides (a player's own movement is predicted on
     * the client), so the client answers from the flag the server synced ({@link #klantOpen}).
     */
    public static boolean deurOpen(Player p) {
        if (devOpen) {
            return true;
        }
        if (p.level().isClientSide()) {
            return klantOpen;
        }
        return p instanceof ServerPlayer sp && open(sp);
    }

    // =====================================================================================================================
    // projects
    // =====================================================================================================================

    /** The flag of the project that finishes this step (null: a talking step, or the two steps of several flags). */
    @Nullable
    static String projectVan(int stap) {
        return switch (stap) {
            case S_ZENDER -> P_ZENDER;
            case S_TERMINAL -> P_TERMINAL;
            case S_BESTEL -> P_BESTEL;
            case S_POORTBUIS -> P_POORTBUIS;
            case S_THUISPOORT -> P_THUISPOORT;
            default -> null;
        };
    }

    /** Is the project of this step done (all its flags)? */
    static boolean stapGedaan(ServerPlayer p, int stap) {
        if (stap == S_ELEMENTEN) {
            return ELEMENTEN.stream().allMatch(f -> lijn.vlag(p, f));
        }
        if (stap == S_BRONNEN) {
            return lijn.vlag(p, P_ENDERGUHRAD) && lijn.vlag(p, P_KOMEET);
        }
        String f = projectVan(stap);
        return f != null && lijn.vlag(p, f);
    }

    /**
     * A machine of this player did its thing ({@code project} = one of the {@code P_*} flags). Remembered whatever step the
     * player is at (also before the story is done: then it counts once tier 5 opens), the line steps forward as far as it
     * can, and the first time there is a little celebration and the hidden advancement of the FTB task.
     */
    public static void meld(@Nullable ServerPlayer p, String project) {
        if (p == null || lijn == null) {
            return;
        }
        if (!lijn.vlag(p, project)) {
            lijn.vlag(p, project, true);
            GuhAdvancements.grant(p, "einde2_trap5_machines_" + project);
            if (open(p) && lijn.begonnen(p)) {
                p.sendSystemMessage(Component.translatable("gui.guhs.einde2_trap5_machines.project." + project).withStyle(ChatFormatting.GOLD));
                if (p.level() instanceof ServerLevel level) {
                    level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.4f);
                    nl.juiced.guhs.feature.bestaand.Schijn.deeltjes(p, ParticleTypes.HAPPY_VILLAGER, p.position().add(0, 1.2, 0), 12, 0.5, 0);
                }
            }
        }
        vooruit(p);
    }

    /** Steps forward while the project of the player's step is done (only once the line has begun: after the first talk). */
    public static void vooruit(ServerPlayer p) {
        if (lijn == null || !open(p)) {
            return;
        }
        int stap = lijn.stap(p);
        while (stap >= S_ZENDER && stap <= S_BRONNEN && stapGedaan(p, stap)) {
            if (!lijn.verder(p, stap)) {
                break;
            }
            stap++;
            if (stap == S_ELEMENTEN) {
                GidsFeature.grant(p, "guheinde/einde2_trap5_machines_halverwege");
            }
        }
    }

    /** The Guhdex "needed" line of a step: how far the multi-flag projects are. */
    private static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        if (stap == S_ELEMENTEN) {
            int n = (int) ELEMENTEN.stream().filter(f -> lijn.vlag(p, f)).count();
            return List.of(Verhaallijn.nodig("guhs:vuurkorf", "gui.guhs.einde2_trap5_machines.nodig.elementen", n, 4));
        }
        if (stap == S_BRONNEN) {
            return List.of(Verhaallijn.nodig("guhs:enderguhrad", "gui.guhs.einde2_trap5_machines.nodig.enderguhrad", lijn.vlag(p, P_ENDERGUHRAD) ? 1 : 0, 1),
                    Verhaallijn.nodig("guhs:komeetkern", "gui.guhs.einde2_trap5_machines.nodig.komeetkern", lijn.vlag(p, P_KOMEET) ? 1 : 0, 1));
        }
        return List.of();
    }

    /** Where to go: the workshop for the talking steps, the comet shard while it waits, else nowhere in particular. */
    @Nullable
    private static Doel doel(ServerPlayer p, int stap) {
        ServerLevel guheinde = p.level().getServer() == null ? null : p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (guheinde == null) {
            return null;
        }
        if (stap == S_PRAAT || stap == S_TERUG) {
            BlockPos pos = Plekken.bekend(guheinde, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL)
                    ? Plekken.wereld(guheinde, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL, "werktuigguh") : null;
            return pos == null ? null : Doel.plek(GuheindeFeature.GUHEINDE, pos, Component.translatable("gui.guhs.einde2_trap5_machines.doel.werkplaats"));
        }
        if (stap == S_BRONNEN && !GuhQuests.saved(p).getBooleanOr(KOMEET_GEPAKT, false)) {
            BlockPos pos = Plekken.bekend(guheinde, Einde2Ids.VUURNATIE_HOOFDSTAD)
                    ? Plekken.wereld(guheinde, Einde2Ids.VUURNATIE_HOOFDSTAD, "komeetscherf") : null;
            return pos == null ? null : Doel.plek(GuheindeFeature.GUHEINDE, pos, Component.translatable("gui.guhs.einde2_trap5_machines.doel.komeet"));
        }
        return null;
    }
}
