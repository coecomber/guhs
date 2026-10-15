package nl.juiced.guhs.feature.einde2.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice audio: the dev commands (CONTRACT_E2_B 8; a dev run only, plain literals):
 * <ul>
 *   <li>{@code /guhs einde2 audio lijst}: every name of 6.1 as a clickable list (click = play it);</li>
 *   <li>{@code /guhs einde2 audio speel <naam>}: plays a name: music and moments as a moment (music slider, fades, loops),
 *       the rest once, for you;</li>
 *   <li>{@code /guhs einde2 audio stop}: ends the moment (fade 40 ticks);</li>
 *   <li>{@code /guhs einde2 audio lied}: Miroh's lullaby with its words in the action bar at the right ticks (a preview of
 *       the captions of h3's {@code lu_ten} scene);</li>
 *   <li>{@code /guhs einde2 audio plek [lucht|water|aarde|vuur|geest|geen|wereld]}: says your music area; with an argument it
 *       forces one ({@code wereld} = back to what the world says);</li>
 *   <li>{@code /guhs einde2 audio boek}: a copy of the credits book;</li>
 *   <li>{@code /guhs einde2 audio wis}: forgets that you had the book, ends a moment, drops a forced area.</li>
 * </ul>
 * The slice has no story line and no scene of its own: no {@code ga} / {@code scene}.
 */
final class AudioCommando {
    /** Per player: the tick count at which the lullaby preview started. */
    private static final Map<UUID, Integer> LIED = new ConcurrentHashMap<>();

    private AudioCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        List<String> gebieden = new ArrayList<>(AudioNamen.GEBIEDEN);
        gebieden.add("geen");
        gebieden.add("wereld");
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("audio")
                        .then(Commands.literal("lijst").executes(c -> lijst(c.getSource())))
                        .then(Commands.literal("speel").then(Commands.argument("naam", StringArgumentType.greedyString())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(AudioNamen.alle(), b))
                                .executes(c -> speel(c.getSource(), StringArgumentType.getString(c, "naam")))))
                        .then(Commands.literal("stop").executes(c -> stop(c.getSource())))
                        .then(Commands.literal("lied").executes(c -> lied(c.getSource())))
                        .then(Commands.literal("plek").executes(c -> plek(c.getSource(), null))
                                .then(Commands.argument("gebied", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(gebieden, b))
                                        .executes(c -> plek(c.getSource(), StringArgumentType.getString(c, "gebied")))))
                        .then(Commands.literal("boek").executes(c -> boek(c.getSource())))
                        .then(Commands.literal("wis").executes(c -> wis(c.getSource()))))));
    }

    /** Once at start-up (the preview of the lullaby needs a tick). */
    static void init() {
        if (!net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            NeoForge.EVENT_BUS.addListener(AudioCommando::onPlayerTick);
        }
    }

    private static int lijst(CommandSourceStack source) {
        String soort = "";
        MutableComponent regel = null;
        List<Component> regels = new ArrayList<>();
        for (String naam : AudioNamen.alle()) {
            String s = naam.substring(0, naam.indexOf('.'));
            if (!s.equals(soort)) {
                if (regel != null) {
                    regels.add(regel);
                }
                soort = s;
                regel = Component.literal(s + ": ").withStyle(ChatFormatting.GOLD);
            }
            String kort = naam.substring(naam.indexOf('.') + 1);
            regel.append(Component.literal("[" + kort + "] ").withStyle(st -> st.withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent.RunCommand("/guhs einde2 audio speel " + naam))));
        }
        if (regel != null) {
            regels.add(regel);
        }
        regels.forEach(r -> source.sendSuccess(() -> r, false));
        source.sendSuccess(() -> Component.literal("[stop]").withStyle(st -> st.withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent.RunCommand("/guhs einde2 audio stop"))), false);
        return 1;
    }

    private static int speel(CommandSourceStack source, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!AudioNamen.alle().contains(naam)) {
            source.sendFailure(Component.literal("Geen geluid " + naam + " (zie /guhs einde2 audio lijst)"));
            return 0;
        }
        if (AudioNamen.lang(naam)) {
            E2Audio.speelLang(p, naam);
        } else {
            E2Audio.stinger(p, naam);
        }
        String bpm = E2Audio.BPM.containsKey(naam) ? " (" + E2Audio.BPM.get(naam) + " BPM)" : "";
        source.sendSuccess(() -> Component.literal("Speelt " + naam + bpm), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        LIED.remove(p.getUUID());
        E2Audio.stopMoment(p, E2Audio.FADE);
        source.sendSuccess(() -> Component.literal("Moment gestopt"), false);
        return 1;
    }

    private static int lied(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        E2Audio.moment(p, "lu_ten");
        LIED.put(p.getUUID(), p.tickCount);
        source.sendSuccess(() -> Component.literal("Miroh's lied, " + LuTenLied.DUUR / 20 + " s"), false);
        return 1;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || LIED.isEmpty()) {
            return;
        }
        Integer start = LIED.get(p.getUUID());
        if (start == null) {
            return;
        }
        int t = p.tickCount - start;
        for (LuTenLied.Regel r : LuTenLied.REGELS) {
            if (r.tick() == t) {
                p.sendOverlayMessage(Component.translatable(r.key()).withStyle(ChatFormatting.ITALIC));
            }
        }
        if (t >= LuTenLied.DUUR) {
            LIED.remove(p.getUUID());
            E2Audio.stopMoment(p, E2Audio.FADE);
        }
    }

    private static int plek(CommandSourceStack source, String gebied) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (gebied != null) {
            if (gebied.equals("wereld")) {
                AudioPlek.forceer(p, null);
            } else if (gebied.equals("geen")) {
                AudioPlek.forceer(p, "");
            } else if (AudioNamen.GEBIEDEN.contains(gebied)) {
                AudioPlek.forceer(p, gebied);
            } else {
                source.sendFailure(Component.literal("Onbekend gebied " + gebied));
                return 0;
            }
        }
        String nu = AudioPlek.werkBij(p);
        String echt = AudioPlek.gebied(p);
        source.sendSuccess(() -> Component.literal("Muziekgebied: " + (nu.isEmpty() ? "geen" : nu) + " (de wereld zegt: "
                + (echt.isEmpty() ? "geen" : echt) + ")"), false);
        return 1;
    }

    private static int boek(CommandSourceStack source) throws CommandSyntaxException {
        Minigames.give(source.getPlayerOrException(), Muziekboekje.stack());
        source.sendSuccess(() -> Component.literal("Muziekboekje gegeven"), false);
        return 1;
    }

    private static int wis(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        GuhQuests.saved(p).remove(Muziekboekje.GEGEVEN);
        LIED.remove(p.getUUID());
        E2Audio.stopMoment(p, 0);
        AudioPlek.forceer(p, null);
        source.sendSuccess(() -> Component.literal("Audio gewist (boek, moment, gebied)"), false);
        return 1;
    }
}
