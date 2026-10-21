package nl.juiced.guhs.feature.safari.rit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;

/**
 * The truck quiz (round 3, J2): at every stop the guide asks the players in the truck a multiple-choice question about
 * that zone (three per zone in the language file: gui.guhs.safari.quiz.&lt;zone&gt;.&lt;n&gt;.vraag / .goed / .fout1 /
 * .fout2, shuffled). The answers are click buttons in the chat ({@code /safariquiz <code> <keuze>}, everybody may); a right
 * answer pays {@link #MUNTEN} Safarimunten from the daily pot "quiz" (at most {@link #DAG_MAX} a day). One question per
 * player at a time; it expires when the truck has driven on for a while.
 */
public final class SafariQuiz {
    public static final int VRAGEN = 3, MUNTEN = 5, DAG_MAX = 40, GELDIG = 1200;
    private static final Map<UUID, Vraag> OPEN = new HashMap<>();

    /** An asked question: its code (from the click), where the right answer is, which question, until when. */
    public record Vraag(int code, int goed, String zone, int nummer, long tot) {
        /** The language key of answer button i (the right one sits at goed, the two wrong ones fill the others). */
        public String antwoord(int i) {
            String basis = "gui.guhs.safari.quiz." + zone + "." + nummer + ".";
            if (i == goed) {
                return basis + "goed";
            }
            return basis + (i < goed ? "fout" + (i + 1) : "fout" + i);
        }
    }

    /** Asks p a question about this zone (game time t). */
    static Vraag vraag(ServerPlayer p, String zone, long t, RandomSource rnd) {
        Vraag v = new Vraag(rnd.nextInt(1_000_000), rnd.nextInt(3), zone, rnd.nextInt(VRAGEN), t + GELDIG);
        OPEN.put(p.getUUID(), v);
        MutableComponent m = Component.translatable("gui.guhs.safari.quiz.vraag",
                Component.translatable("gui.guhs.safari.quiz." + zone + "." + v.nummer() + ".vraag")).withStyle(ChatFormatting.YELLOW);
        for (int i = 0; i < 3; i++) {
            int keuze = i;
            m.append(Component.literal("\n  [" + "ABC".charAt(i) + "] ").withStyle(ChatFormatting.GOLD))
                    .append(Component.translatable(v.antwoord(i)).withStyle(s -> s.withColor(ChatFormatting.WHITE).withUnderlined(true)
                            .withClickEvent(new ClickEvent.RunCommand("/safariquiz " + v.code() + " " + keuze))));
        }
        p.sendSystemMessage(m);
        return v;
    }

    /** p clicked answer keuze of question code: right (coins), wrong (the right answer), or too late / not his. */
    static int antwoord(ServerPlayer p, int code, int keuze) {
        Vraag v = OPEN.get(p.getUUID());
        long t = p.level().getGameTime();
        if (v == null || v.code() != code || t > v.tot()) {
            p.sendSystemMessage(Component.translatable("gui.guhs.safari.quiz.te_laat").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        OPEN.remove(p.getUUID());
        if (keuze == v.goed()) {
            int n = SafariMunten.verdienDagelijks(p, "quiz", MUNTEN, DAG_MAX, "quiz");
            p.sendSystemMessage(Component.translatable(n > 0 ? "gui.guhs.safari.quiz.goed" : "gui.guhs.safari.quiz.goed_vol", n)
                    .withStyle(ChatFormatting.GREEN));
            return 1;
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.safari.quiz.fout", Component.translatable(v.antwoord(v.goed())))
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    @Nullable
    public static Vraag open(UUID speler) {
        return OPEN.get(speler);
    }

    static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("safariquiz")
                .then(Commands.argument("code", IntegerArgumentType.integer(0))
                        .then(Commands.argument("keuze", IntegerArgumentType.integer(0, 2)).executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            if (src.getEntity() instanceof ServerPlayer p) {
                                return antwoord(p, IntegerArgumentType.getInteger(ctx, "code"), IntegerArgumentType.getInteger(ctx, "keuze"));
                            }
                            return 0;
                        }))));
    }

    private SafariQuiz() {
    }
}
