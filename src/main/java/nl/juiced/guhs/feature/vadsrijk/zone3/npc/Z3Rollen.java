package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

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
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.klassen.Advancement;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Premiebord;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3SystemenRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;

/**
 * The zone-3 roles (what a click on a zone-3 NPC does without quest business) and the adapters to the other zone-3 slices. A
 * binding of another slice always wins (we only bind roles nobody bound yet); without one we try the other slice's class by
 * reflection (this tree compiles without them), then our own fallback.
 * <ul>
 *   <li>{@code luchtschip_z3} (Kapitein Zoemzeil at the mast): the travel map / choices (QuestMotor's airship case);</li>
 *   <li>{@code premiebord} (Jagersguh Raap in the Jagershut): z3-systemen's bounty board
 *       ({@code zone3.systemen.Premiebord#open(ServerPlayer)}); fallback: a "not yet" line;</li>
 *   <li>{@code honinglift} (Liftguh Katrolletje): the Honinglift Honingtak &lt;-&gt; Mosvoet once z3_hq09 is done (binding
 *       {@code z3_honinglift} with arg "op"/"af" from z3-wereld, else a teleport);</li>
 *   <li>{@code honingthee}: a warm honey tea (every 5 minutes), like Kandijkom's chocomelk.</li>
 * </ul>
 */
public final class Z3Rollen {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String V = "nl.juiced.guhs.feature.vadsrijk.";
    private static final Map<UUID, Long> THEE = new ConcurrentHashMap<>();

    private Z3Rollen() {
    }

    static void koppel() {
        // z3-systemen (merged): the bounty board, the Honingkraam, the Gouden Veren, the 4th job advancement
        if (!NpcRollen.gekoppeld("premiebord")) {
            NpcRollen.koppel("premiebord", p -> Premiebord.open(p, Premiebord.PREMIES));
        }
        if (!NpcRollen.gekoppeld("honingkraam")) {
            NpcRollen.koppel("honingkraam", p -> Winkels.open(p, Z3SystemenRegistry.HONINGKRAAM));
        }
        extra("z3_jager", "jacht", p -> true, p -> Premiebord.open(p, Premiebord.JACHT));
        extra("z3_jager", "penningwinkel", p -> true, p -> Premiebord.open(p, Premiebord.WINKEL));
        extra("z3_veertje", "gouden_veren", GoudenVeren::kanRuilen, p -> GoudenVeren.ruil(p));
        extra("z3_wasja", "advancement", p -> Advancement.kanDoen(p), p -> Advancement.doe(p));
        if (!NpcRollen.gekoppeld("honinglift")) {
            NpcRollen.koppel("honinglift", Z3Rollen::honinglift);
        }
        if (!NpcRollen.gekoppeld("honingthee")) {
            NpcRollen.koppel("honingthee", Z3Rollen::thee);
        }
    }

    /** An extra choice in an NPC's role dialogue (QuestMotor.EXTRA; text vadsrijk.npc.optie.z3_<id>). */
    static void extra(String npc, String id, java.util.function.Predicate<ServerPlayer> zichtbaar, java.util.function.Consumer<ServerPlayer> doe) {
        java.util.List<QuestMotor.ExtraOptie> l = QuestMotor.EXTRA.computeIfAbsent(npc, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
        String key = "vadsrijk.npc.optie.z3_" + id;
        if (l.stream().noneMatch(e -> e.tekstKey().equals(key))) {
            l.add(new QuestMotor.ExtraOptie(key, zichtbaar, doe));
        }
    }

    static void honinglift(ServerPlayer p) {
        if (!Z3NpcApi.honinglift(p)) {
            p.sendSystemMessage(Component.translatable(Z3Verhaal.Z + "honinglift.dicht").withStyle(ChatFormatting.GRAY));
            return;
        }
        boolean omlaag = p.getY() > 120;
        if (NpcRollen.doe("z3_honinglift", p, omlaag ? "af" : "op") || reflectie(p, omlaag ? "af" : "op", "zone3.wereld.Z3Reizen#honinglift",
                "zone3.wereld.Honinglift#rij")) {
            return;
        }
        p.sendSystemMessage(Component.translatable(Z3Verhaal.Z + "honinglift." + (omlaag ? "af" : "op")).withStyle(ChatFormatting.GOLD));
        Z3Verhaal.zet(p, omlaag ? Z3NpcPlekken.HONINGLIFT_BENEDEN : Z3NpcPlekken.HONINGLIFT_BOVEN, 0f);
    }

    static void thee(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long vorige = THEE.get(p.getUUID());
        if (vorige != null && nu - vorige < 20 * 60 * 5) {
            p.sendSystemMessage(Component.translatable(Z3Verhaal.Z + "thee.al").withStyle(ChatFormatting.GRAY));
            return;
        }
        THEE.put(p.getUUID(), nu);
        Questlog.geef(p, new ItemStack(Z3NpcRegistry.item("honingthee")));
        p.sendSystemMessage(Component.translatable(Z3Verhaal.Z + "thee").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Is this class of another slice in the tree ("zone3.wereld.Z3Reizen", relative to feature.vadsrijk)? */
    static boolean bestaat(String klasse) {
        try {
            Class.forName(V + klasse);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * Calls the first static method that exists: "class#method" relative to feature.vadsrijk, taking (ServerPlayer) or
     * (ServerPlayer, String arg); a method that answers false did nothing.
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
                    } else {
                        continue;
                    }
                    if (!(r instanceof Boolean b) || b) {
                        return true;
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    LOG.warn("Vadsrijk z3 npc: adapter {} failed", naam, e);
                }
            }
        }
        return false;
    }
}
