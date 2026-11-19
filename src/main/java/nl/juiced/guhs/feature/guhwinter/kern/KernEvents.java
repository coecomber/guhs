package nl.juiced.guhs.feature.guhwinter.kern;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.GuhwinterData;
import nl.juiced.guhs.feature.guhwinter.GuhwinterFase;
import nl.juiced.guhs.feature.guhwinter.GuhwinterZone;
import org.slf4j.Logger;

/**
 * 1.10.10 Guhwinter kern: the clock work (moments every second and at server start), the chat messages (on login during
 * the event, one per new phase), the Winterplein destination, the end of the event and the melting.
 */
public final class KernEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The login message comes a little after logging in (after the other login lines). */
    static final int GROET_NA = 60;
    private static final Map<UUID, Integer> GROETEN = new ConcurrentHashMap<>();

    private KernEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(KernEvents::opStart);
        NeoForge.EVENT_BUS.addListener(KernEvents::opStop);
        NeoForge.EVENT_BUS.addListener(KernEvents::opTick);
        NeoForge.EVENT_BUS.addListener(KernEvents::opLogin);
        NeoForge.EVENT_BUS.addListener(Smelten::opContainer);
        NeoForge.EVENT_BUS.addListener(Smelten::opEntiteit);
        NeoForge.EVENT_BUS.addListener(Smelten::opChunk);
        // kern's own moments (ids never change: the world remembers them)
        Guhwinter.opMoment("pakjesavond:kern", s -> aankondiging(s, GuhwinterFase.PAKJESAVOND));
        Guhwinter.opMoment("kerst:kern", s -> aankondiging(s, GuhwinterFase.KERST));
        Guhwinter.opMoment("oudjaar:kern", s -> aankondiging(s, GuhwinterFase.OUDJAAR));
        Guhwinter.opMoment("afgelopen:kern", KernEvents::einde);
    }

    static void opStart(ServerStartedEvent event) {
        MinecraftServer s = event.getServer();
        GROETEN.clear();
        Smelten.afgelopen = Guhwinter.fase(s) == GuhwinterFase.AFGELOPEN;
        Guhwinter.vuurMomenten(s);                 // (catch-up: what passed while the server was down)
        Winterplein.zorg(s);
    }

    static void opStop(ServerStoppedEvent event) {
        GROETEN.clear();
        Smelten.afgelopen = false;
    }

    static void opLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            GROETEN.put(p.getUUID(), p.level().getServer().getTickCount() + GROET_NA);
            if (Guhwinter.fase(p.level().getServer()) == GuhwinterFase.AFGELOPEN) {
                Smelten.speler(p);
            }
        }
    }

    static void opTick(ServerTickEvent.Post event) {
        MinecraftServer s = event.getServer();
        int t = s.getTickCount();
        if (t % 20 == 0) {
            Smelten.afgelopen = Guhwinter.fase(s) == GuhwinterFase.AFGELOPEN;
            Guhwinter.vuurMomenten(s);
        }
        if (!GROETEN.isEmpty()) {
            for (Iterator<Map.Entry<UUID, Integer>> it = GROETEN.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<UUID, Integer> e = it.next();
                if (t >= e.getValue()) {
                    it.remove();
                    ServerPlayer p = s.getPlayerList().getPlayer(e.getKey());
                    if (p != null) {
                        groet(p);
                    }
                }
            }
        }
        if (Smelten.afgelopen) {
            Smelten.wachtrij(s);
            if (t % 100 == 50) {
                for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                    Smelten.speler(p);
                }
                Smelten.rondSpelers(s);
            }
        }
    }

    // --- messages --------------------------------------------------------------------------------------------------------

    /** On login during the event: the Winterplein in the Reisguh list, and the message of this phase (the "new phase" one the first time). */
    public static void groet(ServerPlayer p) {
        MinecraftServer s = p.level().getServer();
        GuhwinterFase f = Guhwinter.fase(s);
        if (!f.bezig()) {
            return;
        }
        Winterplein.ontdek(p);
        GuhwinterData d = GuhwinterData.get(s);
        boolean nieuw = (f == GuhwinterFase.KERST || f == GuhwinterFase.OUDJAAR) && d.gezien(p.getUUID()) < f.ordinal();
        p.sendSystemMessage(bericht(f, nieuw));
        d.zetGezien(p.getUUID(), f);
    }

    /** The message for a phase: {@code nieuw} = the announcement (Kerst, Oudjaar, Pakjesavond), else the login line. */
    public static Component bericht(GuhwinterFase f, boolean nieuw) {
        String naam = f == GuhwinterFase.VOOR ? "sint" : f.name().toLowerCase(java.util.Locale.ROOT);
        MutableComponent kop = Component.translatable("guhwinter.kern.kop").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        return kop.append(Component.literal(" ")).append(Component.translatable("guhwinter.kern." + (nieuw ? "nieuw." : "login.") + naam,
                Guhwinter.hoeKomIkDaar()).withStyle(s -> s.withBold(false).withColor(ChatFormatting.WHITE)));
    }

    /** A new phase began: everyone online hears it (only while it IS that phase: a late catch-up stays quiet). */
    static void aankondiging(MinecraftServer s, GuhwinterFase fase) {
        if (Guhwinter.fase(s) != fase) {
            return;
        }
        Winterplein.zorg(s);
        GuhwinterData d = GuhwinterData.get(s);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (d.gezien(p.getUUID()) < fase.ordinal()) {
                Winterplein.ontdek(p);
                p.sendSystemMessage(bericht(fase, true));
                p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
                d.zetGezien(p.getUUID(), fase);
            }
        }
    }

    /** 3 Jan: the Winterplein leaves the Reisguh list, everyone online hears goodbye, plein's frame, and things melt. */
    static void einde(MinecraftServer s) {
        Smelten.afgelopen = true;
        Winterplein.zorg(s);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.translatable("guhwinter.kern.kop").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                    .append(Component.literal(" ")).append(Component.translatable("guhwinter.kern.einde").withStyle(s2 -> s2.withBold(false).withColor(ChatFormatting.WHITE))));
        }
        ServerLevel guhmension = s.getLevel(GuhwinterZone.DIMENSIE);
        if (guhmension == null) {
            LOGGER.warn("Guhwinter: no Guhmension, the end hooks are skipped");
        } else {
            for (Consumer<ServerLevel> haak : Guhwinter.eindeHaken()) {
                try {
                    haak.accept(guhmension);
                } catch (RuntimeException e) {
                    LOGGER.error("Guhwinter: an end hook failed", e);
                }
            }
        }
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            Smelten.speler(p);
        }
    }
}
