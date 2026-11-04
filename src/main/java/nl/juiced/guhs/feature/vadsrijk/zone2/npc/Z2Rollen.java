package nl.juiced.guhs.feature.vadsrijk.zone2.npc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;

/**
 * The zone-2 roles (what a click on a zone-2 NPC does without quest business) and the adapters to the other zone-2 slices. The
 * roles go through zone 1's {@link NpcRollen}; a binding of another slice always wins (bind the same role id from your registry
 * class: we only bind roles nobody bound yet). Without a binding we look for the other slice's class by reflection (this tree
 * compiles without them; the names are the ones asked for in POI_VERZOEKEN_Z2.md / NOTITIES_Z2_NPC.md), then our own fallback.
 * <ul>
 *   <li>{@code juwelier}: z2-systemen's socket screen ({@code zone2.systemen.Juwelier#open(ServerPlayer)}, or {@code Sokjes#open});</li>
 *   <li>{@code luchtschip_z2} (the Kandijkom captain): back to De Haven; {@code gondel} / {@code gondel_terug}: the gondola;</li>
 *   <li>{@code chocomelk}: the café owner hands you a warm chocomelk (every 5 minutes);</li>
 *   <li>{@code z2_boormeester}: z2-bazen {@code Boormeester.ontgrendel(ServerPlayer)} after the 5 clues.</li>
 * </ul>
 */
public final class Z2Rollen {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String V = "nl.juiced.guhs.feature.vadsrijk.";
    private static final Map<UUID, Long> CHOCO = new ConcurrentHashMap<>();

    private Z2Rollen() {
    }

    /** Called on server start (after zone 1's NpcRollen.koppelAlles): only roles nobody bound yet. */
    static void koppel() {
        if (!NpcRollen.gekoppeld("juwelier")) {
            NpcRollen.koppelAls("juwelier", (p, a) -> {
                if (!reflectie(p, "", "zone2.systemen.Juwelier#open", "zone2.systemen.Sokjes#open", "zone2.systemen.edelsteen.Juwelier#open")) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.juwelier.later").withStyle(ChatFormatting.GRAY));
                }
                return true;
            });
        }
        if (!NpcRollen.gekoppeld("luchtschip_z2")) {
            NpcRollen.koppel("luchtschip_z2", p -> Z2Verhaal.vlieg(p, "haven"));
        }
        if (!NpcRollen.gekoppeld("gondel")) {
            NpcRollen.koppel("gondel", p -> Z2Verhaal.gondel(p, true));
        }
        if (!NpcRollen.gekoppeld("gondel_terug")) {
            NpcRollen.koppel("gondel_terug", p -> Z2Verhaal.gondel(p, false));
        }
        if (!NpcRollen.gekoppeld("chocomelk")) {
            NpcRollen.koppel("chocomelk", Z2Rollen::chocomelk);
        }
        if (!NpcRollen.gekoppeld("z2_boormeester")) {
            NpcRollen.koppelAls("z2_boormeester", (p, a) -> reflectie(p, "", "zone2.bazen.Boormeester#ontgrendel",
                    "zone2.bazen.boormeester.Boormeester#ontgrendel", "zone2.bazen.mika.Boormeester#ontgrendel"));
        }
    }

    static void chocomelk(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long vorige = CHOCO.get(p.getUUID());
        if (vorige != null && nu - vorige < 20 * 60 * 5) {
            p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.chocomelk.al").withStyle(ChatFormatting.GRAY));
            return;
        }
        CHOCO.put(p.getUUID(), nu);
        Questlog.geef(p, new ItemStack(Z2NpcRegistry.QUEST_ITEMS.get("chocomelk").get()));
        p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.chocomelk").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /**
     * Calls the first static method that exists: "class#method" relative to feature.vadsrijk, taking (ServerPlayer),
     * (ServerPlayer, String arg) or (ServerPlayer, boolean "op"/"kandijkom"); a method that answers false did nothing.
     */
    static boolean reflectie(ServerPlayer p, String arg, String... namen) {
        for (String naam : namen) {
            int h = naam.indexOf('#');
            Class<?> c;
            try {
                c = Class.forName(V + naam.substring(0, h));
            } catch (ClassNotFoundException | LinkageError e) {
                continue;
            }
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(naam.substring(h + 1)) || !Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Class<?>[] t = m.getParameterTypes();
                try {
                    Object r;
                    if (t.length == 1 && t[0].isAssignableFrom(ServerPlayer.class)) {
                        r = m.invoke(null, p);
                    } else if (t.length == 2 && t[0].isAssignableFrom(ServerPlayer.class) && t[1] == String.class) {
                        r = m.invoke(null, p, arg);
                    } else if (t.length == 2 && t[0].isAssignableFrom(ServerPlayer.class) && t[1] == boolean.class) {
                        r = m.invoke(null, p, arg.equals("op") || arg.equals("kandijkom"));
                    } else {
                        continue;
                    }
                    if (!(r instanceof Boolean b) || b) {
                        return true;
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    LOG.warn("Vadsrijk z2 npc: adapter {} failed", naam, e);
                }
            }
        }
        return false;
    }
}
