package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice geest_spel: travelling through the Geestenwereld (DESIGN 5 "Terugkomen na het verhaal"):
 * <ul>
 *   <li><b>fast travel</b>: sneak + right-click a meditatiesteen ({@link Geestenwereld#MEDITATIESTEEN}): the soul wakes not
 *       in its body but at a Guhvatar building you know ({@link E2Verhaal#bekendeGebouwen}{@code (p, true)}), after the whole
 *       story; a plain click still takes you back to your body;</li>
 *   <li><b>mirror ponds</b> ({@code spiegelvijver_1..4}): stand at one and look down into the water: it shows a building you
 *       know (and, after the story, you may step through it) or yourself with the Guhvatar glow;</li>
 *   <li><b>free visits</b>: after the story a {@code mediteerkussen} (one is a gift, more can be made) takes you (and whoever
 *       stands within 5 blocks) into the Geestenwereld from anywhere.</li>
 * </ul>
 */
public final class Snelreis {
    private static final String SLEUTEL = "einde2_geest_spel_reis";
    /** Saved: the gift of the cushion was given; the tip about sneaking was told. */
    static final String KUSSEN = GeestSpel.DATA + "kussen", TIP = GeestSpel.DATA + "reistip";

    /** player -> the buildings the open list showed (option i = index i - 1). */
    private static final Map<UUID, List<String>> LIJST = new ConcurrentHashMap<>();
    /** player -> since when they look into a mirror pond (game time). */
    private static final Map<UUID, Long> KIJKT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SPIEGEL_RUST = new ConcurrentHashMap<>();

    private Snelreis() {
    }

    static void register() {
        Geestenwereld.MEDITATIESTEEN.add(Snelreis::meditatiesteen);
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
            List<String> l = LIJST.remove(p.getUUID());
            if (l != null && optie >= 1 && optie <= l.size()) {
                reis(p, l.get(optie - 1));
            }
        });
    }

    /** May this player travel: the story done, in the Geestenwereld, nobody's guest. */
    static boolean mag(ServerPlayer p) {
        return E2Verhaal.finaleKlaar(p);
    }

    /** The meditation stone listener: true only for a sneak-click of a player who may travel (else: back to the body). */
    private static Boolean meditatiesteen(ServerPlayer p, BlockPos pos) {
        if (!mag(p)) {
            return false;
        }
        if (!p.isShiftKeyDown()) {
            if (!GuhQuests.saved(p).getBooleanOr(TIP, false)) {
                GuhQuests.saved(p).putBoolean(TIP, true);
                GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.reis.tip");
            }
            return false;
        }
        lijst(p, null);
        return true;
    }

    /** Opens the list of buildings to travel to (with a speaker: the stone, or a pond's own text first). */
    static void lijst(ServerPlayer p, @Nullable String eerst) {
        List<String> gebouwen = new ArrayList<>(E2Verhaal.bekendeGebouwen(p, true));
        if (gebouwen.isEmpty()) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.reis.niets");
            return;
        }
        if (gebouwen.size() > 10) {
            gebouwen = gebouwen.subList(0, 10);
        }
        LIJST.put(p.getUUID(), List.copyOf(gebouwen));
        Praat.Optie[] opties = new Praat.Optie[gebouwen.size() + 1];
        for (int i = 0; i < gebouwen.size(); i++) {
            opties[i] = new Praat.Optie(i + 1, "structure.guhs." + gebouwen.get(i));
        }
        opties[gebouwen.size()] = new Praat.Optie(99, "gui.guhs.einde2_geest_spel.optie.straks");
        List<Praat.Regel> regels = new ArrayList<>();
        if (eerst != null) {
            regels.add(GeestSpel.verteller(eerst));
        }
        regels.add(GeestSpel.verteller("gui.guhs.einde2_geest_spel.reis.kies"));
        GeestSpel.praat(p, SLEUTEL, regels, opties);
    }

    /** The soul wakes up at this building (in the Guheinde); its body comes along, as souls do in this world. */
    static boolean reis(ServerPlayer p, String gebouw) {
        MinecraftServer server = p.level().getServer();
        ServerLevel guheinde = server == null ? null : server.getLevel(GuheindeFeature.GUHEINDE);
        Vec3 naar = guheinde == null ? null : Plekken.aankomst(guheinde, gebouw);
        if (naar == null) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.reis.onbekend", Component.translatable("structure.guhs." + gebouw));
            return false;
        }
        p.level().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        GuhQuests.saved(p).remove(Geestenwereld.TERUG);
        p.teleportTo(guheinde, naar.x, naar.y, naar.z, Set.of(), p.getYRot(), 0f, true);
        p.resetFallDistance();
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.reis.aangekomen", Component.translatable("structure.guhs." + gebouw));
        GuhAdvancements.grant(p, "einde2_geest_spel_snelreis");
        return true;
    }

    // =====================================================================================================================
    // mirror ponds
    // =====================================================================================================================

    /** Every 10 ticks in the Geestenwereld: looking down into a mirror pond. */
    static void tick(ServerPlayer p) {
        String vijver = null;
        for (String id : GeestPlek.reeks("spiegelvijver_")) {
            var d = GeestPlek.doos(id);
            if (d != null && d.inflate(2.5, 2, 2.5).contains(p.position())) {
                vijver = id;
                break;
            }
        }
        long nu = p.level().getGameTime();
        if (vijver == null || p.getXRot() < 50 || p.isSprinting()) {
            KIJKT.remove(p.getUUID());
            return;
        }
        Long rust = SPIEGEL_RUST.get(p.getUUID());
        if (rust != null && nu < rust) {
            return;
        }
        long sinds = KIJKT.computeIfAbsent(p.getUUID(), u -> nu);
        if (nu - sinds < 30) {
            if (nu - sinds >= 10) {
                p.level().sendParticles(p, ParticleTypes.GLOW, false, false, p.getX(), p.getY() - 0.4, p.getZ(), 4, 1.2, 0.05, 1.2, 0.0);
            }
            return;
        }
        KIJKT.remove(p.getUUID());
        SPIEGEL_RUST.put(p.getUUID(), nu + 20 * 20);
        spiegel(p);
    }

    /** What the pond shows: a building you know (often), or yourself glowing. */
    static void spiegel(ServerPlayer p) {
        GuhAdvancements.grant(p, "einde2_geest_spel_spiegel");
        List<String> gebouwen = E2Verhaal.bekendeGebouwen(p, false);
        boolean zelf = gebouwen.isEmpty() || p.getRandom().nextInt(4) == 0;
        GeestSpel.geluid(p, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 0.6f);
        if (zelf) {
            ModNetworking.sendTo(p, new GeestSpelPayloads.Spiegel("", true));
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.spiegel.zelf");
            return;
        }
        String g = gebouwen.get(p.getRandom().nextInt(gebouwen.size()));
        ModNetworking.sendTo(p, new GeestSpelPayloads.Spiegel(g, false));
        if (mag(p) && E2Verhaal.bekendeGebouwen(p, true).contains(g)) {
            LIJST.put(p.getUUID(), List.of(g));
            GeestSpel.praat(p, SLEUTEL, List.of(GeestSpel.verteller("gui.guhs.einde2_geest_spel.spiegel.gebouw", Component.translatable("structure.guhs." + g))),
                    new Praat.Optie(1, "gui.guhs.einde2_geest_spel.spiegel.stap"), new Praat.Optie(99, "gui.guhs.einde2_geest_spel.optie.straks"));
        } else {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.spiegel.gebouw", Component.translatable("structure.guhs." + g));
        }
    }

    // =====================================================================================================================
    // the cushion: a gift after the story
    // =====================================================================================================================

    /** Every 200 ticks outside the Geestenwereld: the story just ended? Then the gift of a meditation cushion. */
    static void buitenTick(ServerPlayer p) {
        if (!mag(p) || GuhQuests.saved(p).getBooleanOr(KUSSEN, false)) {
            return;
        }
        GuhQuests.saved(p).putBoolean(KUSSEN, true);
        GeestSpel.geef(p, E2GeestSpelFeature.MEDITEERKUSSEN_ITEM.get(), 1);
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.kussen.cadeau");
    }

    /** Somebody meditates on a cushion: after the story, into the Geestenwereld (with whoever stands near). */
    static void mediteer(ServerPlayer p) {
        if (Geestenwereld.in(p)) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.kussen.al_hier");
            return;
        }
        if (!mag(p)) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.kussen.nog_niet");
            return;
        }
        if (!GeestToegang.stuur(p, Geestenwereld.AANKOMST)) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.kussen.mislukt");
            return;
        }
        GuhAdvancements.grant(p, "einde2_geest_spel_kussen");
    }

    /** player -> (since when, where) they sit on a cushion with their eyes closed. */
    private record Zit(long sinds, Vec3 plek) {
    }

    private static final Map<UUID, Zit> MEDITEERT = new ConcurrentHashMap<>();
    /** How long you sit on the cushion before your soul goes (ticks). */
    static final int KUSSEN_TICKS = 50;

    /** A click on a cushion: eyes closed (sit still a moment), then {@link #mediteer}. */
    static void zitOpKussen(ServerPlayer p) {
        if (Geestenwereld.in(p)) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.kussen.al_hier");
            return;
        }
        if (!mag(p)) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.kussen.nog_niet");
            return;
        }
        MEDITEERT.put(p.getUUID(), new Zit(p.level().getGameTime(), p.position()));
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.kussen.ogen_dicht");
    }

    static void kussenTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || MEDITEERT.isEmpty()) {
            return;
        }
        Zit z = MEDITEERT.get(p.getUUID());
        if (z == null) {
            return;
        }
        if (p.position().distanceToSqr(z.plek()) > 1.5 * 1.5) {
            MEDITEERT.remove(p.getUUID());
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.kussen.wakker");
            return;
        }
        long t = p.level().getGameTime() - z.sinds();
        if (t % 5 == 0) {
            p.level().sendParticles(ParticleTypes.GLOW, p.getX(), p.getY() + 0.8, p.getZ(), 3, 0.4, 0.5, 0.4, 0.01);
        }
        if (t >= KUSSEN_TICKS) {
            MEDITEERT.remove(p.getUUID());
            mediteer(p);
        }
    }

    static void vergeet(ServerPlayer p) {
        LIJST.remove(p.getUUID());
        KIJKT.remove(p.getUUID());
        MEDITEERT.remove(p.getUUID());
    }
}
