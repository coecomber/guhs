package nl.juiced.guhs.feature.vadsrijk.dialoog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestStap;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * T-DIALOOG dev commands (op 2), {@code /guhs vadsrijk dialoog ...}:
 * <ul>
 *   <li>{@code demo}: one scene with every feature (narrator, four moods, four factions, you, a long page, the option icons, the
 *       "Wat moet ik doen?" button); works anywhere (its sleutel starts with vr_);</li>
 *   <li>{@code vol}: a crowded screen (a long text + 9 answers) for GUI scale 3-4: the answers go in two columns;</li>
 *   <li>{@code oud}: toggles the old (Knuffeldal) screen for yourself, to compare.</li>
 * </ul>
 */
public final class DialoogCommando {
    public static final String SLEUTEL = "vr_dialoog_demo";
    static final String DEMO = "vadsrijk.dialoog.demo.";
    /** The demo pages' moods (null: guessed from the text). Page i = lang key DEMO + i. */
    static final Emotie[] DEMO_EMOTIES = {null, Emotie.BLIJ, Emotie.VERDRIETIG, Emotie.VERRAST, Emotie.BOOS, null, Emotie.NEUTRAAL, null};
    /** (dev) the objective the next conversation of this player gets (the demo has no real quest NPC). */
    static final Map<UUID, CompoundTag> DOEL_VOLGEND = new ConcurrentHashMap<>();

    private DialoogCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        Praat.luister(SLEUTEL, (p, spreker, optie) -> {
            if (optie < 0) {
                return;
            }
            Praat.sluit(p);
            p.sendSystemMessage(Component.translatable("vadsrijk.dialoog.demo.gekozen", optie).withStyle(ChatFormatting.LIGHT_PURPLE));
        });
        LiteralArgumentBuilder<CommandSourceStack> d = Commands.literal("dialoog").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        d.then(Commands.literal("demo").executes(c -> {
            demo(c.getSource().getPlayerOrException());
            return 1;
        }));
        d.then(Commands.literal("vol").executes(c -> {
            vol(c.getSource().getPlayerOrException());
            return 1;
        }));
        d.then(Commands.literal("oud").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            boolean oud = RijkDialoog.OUD.add(p.getUUID()) || !RijkDialoog.OUD.remove(p.getUUID());
            c.getSource().sendSuccess(() -> Component.translatable(oud ? "vadsrijk.dialoog.cmd.oud" : "vadsrijk.dialoog.cmd.nieuw"), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(d));
    }

    /** Every feature in one scene. */
    public static void demo(ServerPlayer p) {
        List<Praat.Regel> r = new ArrayList<>();
        r.add(new Praat.Regel(null, "vadsrijk.npc.verteller", DEMO + 0));
        r.add(new Praat.Regel(null, "vadsrijk.npc.naam.oma_truus", DEMO + 1));
        r.add(new Praat.Regel(null, "vadsrijk.npc.naam.z2_poollicht", DEMO + 2));
        r.add(new Praat.Regel(null, "vadsrijk.npc.naam.z3_imker_1", DEMO + 3));
        r.add(new Praat.Regel(null, "vadsrijk.z3npc.imker.naam", DEMO + 4));
        r.add(new Praat.Regel(p, "vadsrijk.npc.jij", DEMO + 5));
        r.add(new Praat.Regel(null, "vadsrijk.npc.naam.trainer_ridder", DEMO + 6));
        r.add(new Praat.Regel(null, "vadsrijk.npc.naam.oma_truus", DEMO + 7));
        DOEL_VOLGEND.put(p.getUUID(), RijkDialoog.doel("aanbod", new QuestStap("demo", false, Component.translatable(DEMO + "doel.titel"),
                Component.translatable(DEMO + "doel.wat"), Component.translatable(DEMO + "doel.waar"), null, null)));
        Praat.scene(p, SLEUTEL, r, new Praat.Optie(1, "vadsrijk.npc.optie.ja"), new Praat.Optie(2, "vadsrijk.npc.optie.rol.winkel"),
                new Praat.Optie(3, "vadsrijk.npc.optie.reiskaart"), new Praat.Optie(4, "vadsrijk.npc.optie.vlieg.kandijkom"),
                new Praat.Optie(5, "vadsrijk.npc.optie.later"));
    }

    /** A crowded screen: a long text and nine answers. */
    public static void vol(ServerPlayer p) {
        List<Praat.Optie> o = new ArrayList<>();
        String[] keys = {"vadsrijk.npc.optie.ja", "vadsrijk.npc.optie.rol.winkel", "vadsrijk.npc.optie.rol.bank", "vadsrijk.npc.optie.rol.stal",
                "vadsrijk.npc.optie.rol.smid", "vadsrijk.npc.optie.reiskaart", "vadsrijk.npc.optie.vlieg.zwamzoet", "vadsrijk.npc.optie.vlieg.kandijkom.slot",
                "vadsrijk.npc.optie.later"};
        for (int i = 0; i < keys.length; i++) {
            o.add(new Praat.Optie(10 + i, keys[i]));
        }
        DOEL_VOLGEND.put(p.getUUID(), RijkDialoog.doel("actief", new QuestStap("demo", true, Component.translatable(DEMO + "doel.titel"),
                Component.translatable(DEMO + "doel.wat"), Component.translatable(DEMO + "doel.waar"), null, null)));
        Praat.scene(p, SLEUTEL, List.of(new Praat.Regel(null, "vadsrijk.npc.naam.oma_truus", DEMO + "vol")), o.toArray(new Praat.Optie[0]));
    }
}
