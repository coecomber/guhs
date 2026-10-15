package nl.juiced.guhs.feature.oudescenes;

import java.util.function.Consumer;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * bbq2 (oude-scenes, DESIGN_VERHALENPAD B): one camera scene for each older story (Balto, Mewtwo, Lilo &amp; Stitch, the
 * Hemelkapelletje, the Grillguh, the Timmerguh). Resources: tools/features/oude_scenes.py (+ oude_scenes_scene.py: the six
 * scripts in the coordinates of the stories' own templates; oude_scenes_java.py writes {@link Scenes} from them).
 * <ul>
 *   <li>{@link OudeScenes}: the six scenes, when a story plays its scene ({@link OudeScenes#speel}: once per player, the
 *       story's next step in its {@code daarna}), where (the copy of the building at the spot) and what happens for players
 *       who are past the moment already (it is in their Guhdex to watch, nothing is asked of them);</li>
 *   <li>{@link Effecten}: what this slice's client adds for the viewer only (client.OudeScenesClient): snow and a white
 *       fog, rain and thunder, night, lightning, streams of particles, a portal that only lights in the scene, a door that
 *       opens, a heart that starts to beat.</li>
 * </ul>
 * No blocks, items, entities or payloads of its own: the cast and the buildings are the old stories'.
 * Ops: {@code /guhs oudescenes stand | speel &lt;scene&gt; | vergeet &lt;scene&gt; | ga &lt;scene&gt;}.
 */
public final class OudeScenesFeature {
    public static void register(IEventBus modBus) {
        Scenes.registreer();
        NeoForge.EVENT_BUS.addListener(OudeScenes::opTick);
        NeoForge.EVENT_BUS.addListener(OudeScenes::opLogin);
        NeoForge.EVENT_BUS.addListener(OudeScenes::opLogout);
        NeoForge.EVENT_BUS.addListener(OudeScenesFeature::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    /** The op commands (for checks and for the AutoCheck script tools/autocheck/bbq2_oude-scenes.txt). */
    private static void commando(RegisterCommandsEvent event) {
        var os = Commands.literal("oudescenes").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        os.then(Commands.literal("stand").executes(OudeScenesFeature::stand));
        for (OudeScene s : OudeScenes.ALLE) {
            // plays the scene now, as the story would (also when it was seen before: it is forgotten first)
            os.then(Commands.literal("speel").then(Commands.literal(s.naam()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                OudeScenes.Plek plek = OudeScenes.plek(p.level(), s, p.blockPosition());
                if (plek == null) {
                    return zeg(c, s.id() + ": no free camera here");
                }
                OudeScenes.vergeet(p, s);
                boolean speelt = Cutscenes.speel(p, s.scene(), plek.anker(), plek.draai(), null);
                return zeg(c, s.id() + " at " + plek.anker().toShortString() + " turned " + plek.draai() + ": " + (speelt ? "playing" : "not now"));
            })));
            os.then(Commands.literal("vergeet").then(Commands.literal(s.naam()).executes(c -> {
                OudeScenes.vergeet(c.getSource().getPlayerOrException(), s);
                return zeg(c, s.id() + ": forgotten");
            })));
            // to the scene's anchor in the copy of its building around here
            os.then(Commands.literal("ga").then(Commands.literal(s.naam()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                OudeScenes.Plek plek = OudeScenes.kopie(p.level(), s, p.blockPosition());
                if (plek == null) {
                    return zeg(c, "no copy of " + s.structuur() + " here");
                }
                p.teleportTo(plek.anker().getX() + 0.5, plek.anker().getY(), plek.anker().getZ() + 0.5);
                return zeg(c, s.id() + ": " + plek.anker().toShortString());
            })));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(os));
    }

    private static int stand(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        for (OudeScene s : OudeScenes.ALLE) {
            OudeScenes.Plek kopie = OudeScenes.kopie(p.level(), s, p.blockPosition());
            zeg(c, s.naam() + ": " + (Cutscenes.gezien(p, s.id()) ? "seen" : "not seen") + (s.voorbij().test(p) ? ", past its moment" : ", before its moment")
                    + (kopie == null ? ", no copy of " + s.structuur() + " here"
                    : ", copy at " + kopie.anker().toShortString() + " turned " + kopie.draai()
                    + (OudeScenes.vrij(p.level(), s.scene(), kopie.anker(), kopie.draai()) ? ", cameras free"
                    : ", A CAMERA IS BLOCKED (" + OudeScenes.geblokkeerd(p.level(), s.scene(), kopie.anker(), kopie.draai()) + ")")));
        }
        return 1;
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }

    private OudeScenesFeature() {
    }
}
