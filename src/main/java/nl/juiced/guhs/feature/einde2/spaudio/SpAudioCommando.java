package nl.juiced.guhs.feature.einde2.spaudio;

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
import nl.juiced.guhs.feature.einde2.spverhaal.SpCommando;

/**
 * einde2 "Sponsguh", slice sp_audio: the dev commands (CONTRACT_E2_C 7.9, 8.2; a dev run only), under
 * {@code /guhs einde2 sponsguh audio}:
 * <ul>
 *   <li>{@code lijst}: every name, clickable (click = play);</li>
 *   <li>{@code speel <naam>}: music and moments as a moment (fades, loops; a song at once), a tune as your looping minigame
 *       tune, a stinger once;</li>
 *   <li>{@code stop}: ends the moment and the tune;</li>
 *   <li>{@code lied <lied>}: plays a song from its first tick and puts its words in chat AND over the hotbar on their ticks
 *       (the captions of its scene), with the tick of every line;</li>
 *   <li>{@code plek [bikinibodem|schelpenstad|loopgraaf|geen|wereld]}: says your music area; with an argument it forces one
 *       ({@code wereld} = what the world says again).</li>
 * </ul>
 */
final class SpAudioCommando {
    private record Preview(String lied, long start) {
    }

    private static final Map<UUID, Preview> LIED = new ConcurrentHashMap<>();

    private SpAudioCommando() {
    }

    static void init() {
        if (SpCommando.dev()) {
            NeoForge.EVENT_BUS.addListener(SpAudioCommando::onPlayerTick);
        }
    }

    static void register(RegisterCommandsEvent event) {
        if (!SpCommando.dev()) {
            return;
        }
        List<String> plekken = new ArrayList<>(SpAudioPlek.GEBIEDEN);
        plekken.add("geen");
        plekken.add("wereld");
        SpCommando.groep(event, Commands.literal("audio")
                .then(Commands.literal("lijst").executes(c -> lijst(c.getSource())))
                .then(Commands.literal("speel").then(Commands.argument("naam", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SpAudio.NAMEN, b))
                        .executes(c -> speel(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("stop").executes(c -> stop(c.getSource())))
                .then(Commands.literal("lied").then(Commands.argument("lied", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SpLiederen.ALLE.keySet(), b))
                        .executes(c -> lied(c.getSource(), StringArgumentType.getString(c, "lied")))))
                .then(Commands.literal("plek").executes(c -> plek(c.getSource(), null))
                        .then(Commands.argument("gebied", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(plekken, b))
                                .executes(c -> plek(c.getSource(), StringArgumentType.getString(c, "gebied"))))));
    }

    private static int lijst(CommandSourceStack source) {
        String soort = "";
        MutableComponent regel = null;
        List<Component> regels = new ArrayList<>();
        for (String naam : SpAudio.NAMEN) {
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
                    .withClickEvent(new ClickEvent.RunCommand("/guhs einde2 sponsguh audio speel " + naam))));
        }
        if (regel != null) {
            regels.add(regel);
        }
        regels.forEach(r -> source.sendSuccess(() -> r, false));
        source.sendSuccess(() -> Component.literal("[stop]").withStyle(st -> st.withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent.RunCommand("/guhs einde2 sponsguh audio stop"))), false);
        return 1;
    }

    private static int speel(CommandSourceStack source, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (!SpAudio.NAMEN.contains(naam)) {
            source.sendFailure(Component.literal("Geen geluid " + naam + " (zie /guhs einde2 sponsguh audio lijst)"));
            return 0;
        }
        if (naam.startsWith("muziek.") || naam.startsWith("moment.")) {
            SpAudio.speelLang(p, naam);
        } else {
            SpAudio.stinger(p, naam);
        }
        String bpm = SpAudio.BPM.containsKey(naam) ? " (" + SpAudio.BPM.get(naam) + " BPM)" : "";
        source.sendSuccess(() -> Component.literal("Speelt " + naam + bpm), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        LIED.remove(p.getUUID());
        SpAudio.stopMoment(p, SpAudio.FADE);
        SpAudio.stopDeuntje(p);
        source.sendSuccess(() -> Component.literal("Moment en deuntje gestopt"), false);
        return 1;
    }

    private static int lied(CommandSourceStack source, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        SpLiederen.Lied l = SpLiederen.van(naam);
        if (l == null) {
            source.sendFailure(Component.literal("Geen lied " + naam));
            return 0;
        }
        SpAudio.speelLang(p, "moment." + naam);
        LIED.put(p.getUUID(), new Preview(naam, p.level().getGameTime()));
        source.sendSuccess(() -> Component.literal(naam + ": " + l.duur() + " ticks (" + l.duur() / 20.0 + " s), scene " + l.scene())
                .withStyle(ChatFormatting.GOLD), false);
        for (SpLiederen.Regel r : l.regels()) {
            source.sendSuccess(() -> Component.literal("t" + r.tick() + " (" + r.ticks() + "): ").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable("scene.guhs." + l.scene() + ".lied." + r.key()).withStyle(ChatFormatting.ITALIC)), false);
        }
        return 1;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (LIED.isEmpty() || !(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Preview v = LIED.get(p.getUUID());
        if (v == null) {
            return;
        }
        SpLiederen.Lied l = SpLiederen.van(v.lied());
        long t = p.level().getGameTime() - v.start();
        for (SpLiederen.Regel r : l.regels()) {
            if (r.tick() == t) {
                p.sendOverlayMessage(Component.translatable("scene.guhs." + l.scene() + ".lied." + r.key()).withStyle(ChatFormatting.ITALIC));
            }
        }
        if (t >= l.duur()) {
            LIED.remove(p.getUUID());
            SpAudio.stopMoment(p, SpAudio.FADE);
        }
    }

    private static int plek(CommandSourceStack source, String gebied) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        if (gebied != null) {
            if (gebied.equals("wereld")) {
                SpAudioPlek.forceer(p, null);
            } else if (gebied.equals("geen")) {
                SpAudioPlek.forceer(p, "");
            } else if (SpAudioPlek.GEBIEDEN.contains(gebied)) {
                SpAudioPlek.forceer(p, gebied);
            } else {
                source.sendFailure(Component.literal("Onbekend gebied " + gebied));
                return 0;
            }
        }
        String nu = SpAudioPlek.werkBij(p);
        String echt = SpAudioPlek.gebied(p);
        source.sendSuccess(() -> Component.literal("Sponsguh-muziekgebied: " + (nu.isEmpty() ? "geen" : nu) + " (de wereld zegt: "
                + (echt.isEmpty() ? "geen" : echt) + ")"), false);
        return 1;
    }
}
