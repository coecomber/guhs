package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * U-GILDEN: the bond chat. {@code /bc <tekst>} sends one line to every online member (their ACTIVE character must be in the bond);
 * {@code /bond chat} toggles "bondchat mode": your normal chat lines go to the bond until you toggle it off. Also the bond's
 * own announcements ({@link #meld}: level-ups, tasks, new members).
 */
public final class BondChat {
    private static final Set<UUID> MODUS = ConcurrentHashMap.newKeySet();

    private BondChat() {
    }

    /** The online players whose active character is in this bond. */
    public static List<ServerPlayer> online(MinecraftServer s, Bond b) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (b.leden.containsKey(Bonden.sleutel(p))) {
                l.add(p);
            }
        }
        return l;
    }

    /** "[TAG] " in the bond's colour. */
    public static MutableComponent tag(Bond b) {
        return Component.literal("[" + b.tag + "] ").withStyle(s -> s.withColor(TextColor.fromRgb(Banier.rgb(b.basis))).withBold(true));
    }

    /** A bond announcement to every online member. */
    public static void meld(MinecraftServer s, Bond b, Component c) {
        Component regel = tag(b).append(c.copy().withStyle(st -> st.withBold(false)));
        for (ServerPlayer p : online(s, b)) {
            p.sendSystemMessage(regel);
        }
    }

    /** One chat line from p to its bond. False (with a message) when p has no bond. */
    public static boolean zeg(ServerPlayer p, String tekst) {
        Bond b = Bonden.van(p);
        if (b == null) {
            p.sendSystemMessage(Component.translatable("vadsrijk.gilden.fout.geen_bond").withStyle(ChatFormatting.RED));
            return false;
        }
        Bond.Lid l = b.lid(Bonden.sleutel(p));
        Component naam = Component.literal(p.getGameProfile().name()).withStyle(l == null ? ChatFormatting.WHITE : l.rang.kleur);
        Component regel = tag(b).append(naam.copy().withStyle(st -> st.withBold(false))).append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(tekst).withStyle(ChatFormatting.WHITE));
        for (ServerPlayer o : online(p.level().getServer(), b)) {
            o.sendSystemMessage(regel);
        }
        return true;
    }

    public static boolean wissel(ServerPlayer p) {
        if (!MODUS.remove(p.getUUID())) {
            MODUS.add(p.getUUID());
            return true;
        }
        return false;
    }

    public static boolean modus(ServerPlayer p) {
        return MODUS.contains(p.getUUID());
    }

    static void opChat(ServerChatEvent e) {
        ServerPlayer p = e.getPlayer();
        if (!MODUS.contains(p.getUUID())) {
            return;
        }
        String t = e.getRawText();
        if (t.startsWith("!")) {   // ("!tekst" = once to everybody while bondchat mode is on)
            e.setMessage(Component.literal(t.substring(1)));
            return;
        }
        if (Bonden.van(p) == null) {
            MODUS.remove(p.getUUID());
            return;
        }
        e.setCanceled(true);
        p.level().getServer().execute(() -> zeg(p, t));
    }

    static void vergeet(UUID id) {
        MODUS.remove(id);
    }
}
