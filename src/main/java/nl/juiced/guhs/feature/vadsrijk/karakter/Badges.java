package nl.juiced.guhs.feature.vadsrijk.karakter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * T-KARAKTER: the mode badge by the name, while in the Vadsrijk: a gold heart for a living Eénvads knight, a pink cloud for
 * Knuffelmodus, a gold star for a retired knight (Rustende Ridder). In chat + the player list (server NameFormat /
 * TabListNameFormat) and above the head (client NameFormat, from the map sent with {@link KarakterPayloads.Badges}).
 */
public final class Badges {
    /** Server: the badge code of every online player ("" = none). */
    private static final Map<UUID, String> CODES = new ConcurrentHashMap<>();
    /** Client: the codes the server sent. */
    static volatile Map<UUID, String> client = Map.of();

    private Badges() {
    }

    /** The badge code of the active character: "p" (retired) + the mode id when it has a badge, "" for a plain Normaal knight. */
    static String code(Player p) {
        Modus m = Karakter.modus(p);
        return (Karakter.gepensioneerd(p) ? "p" : "") + (m.badge.isEmpty() ? "" : m.id);
    }

    /** The badge as text (empty component for ""). */
    public static MutableComponent badge(String code) {
        MutableComponent c = Component.empty();
        if (code.startsWith("p")) {
            c.append(Component.literal(Modus.PENSIOEN_BADGE).withStyle(ChatFormatting.GOLD));
            code = code.substring(1);
        }
        if (!code.isEmpty()) {
            Modus m = Modus.van(code);
            c.append(Component.literal(m.badge).withStyle(m.kleur));
        }
        return c;
    }

    static Component metBadge(Component naam, String code) {
        if (code.isEmpty()) {
            return naam;
        }
        return badge(code).append(Component.literal(" ")).append(naam);
    }

    /** Recomputes this player's badge, refreshes the names and tells every client. */
    public static void ververs(ServerPlayer p) {
        String code = Vadsrijk.in(p) ? code(p) : "";
        String oud = CODES.put(p.getUUID(), code);
        p.refreshDisplayName();
        p.refreshTabListName();
        if (!code.equals(oud)) {
            stuurIedereen(p.level().getServer());
        }
    }

    static void vergeet(MinecraftServer s, UUID id) {
        if (CODES.remove(id) != null && s != null) {
            stuurIedereen(s);
        }
    }

    static CompoundTag alsTag() {
        CompoundTag t = new CompoundTag();
        CODES.forEach((id, c) -> {
            if (!c.isEmpty()) {
                t.putString(id.toString(), c);
            }
        });
        return t;
    }

    static void stuurIedereen(MinecraftServer s) {
        if (s != null) {
            PacketDistributor.sendToAllPlayers(new KarakterPayloads.Badges(alsTag()));
        }
    }

    static void stuur(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new KarakterPayloads.Badges(alsTag()));
    }

    // ---- name events -----------------------------------------------------------------------------------------------------------

    static void naam(PlayerEvent.NameFormat e) {
        Player p = e.getEntity();
        String code = p instanceof ServerPlayer ? CODES.getOrDefault(p.getUUID(), "")
                : p.level().isClientSide() && Vadsrijk.in(p) ? client.getOrDefault(p.getUUID(), "") : "";
        if (!code.isEmpty()) {
            e.setDisplayname(metBadge(e.getDisplayname(), code));
        }
    }

    static void tabNaam(PlayerEvent.TabListNameFormat e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            String code = CODES.getOrDefault(p.getUUID(), "");
            if (!code.isEmpty()) {
                Component basis = e.getDisplayName() != null ? e.getDisplayName() : p.getName();
                e.setDisplayName(metBadge(basis, code));
            }
        }
    }

    /** Client: new codes. */
    static void ontvang(CompoundTag t) {
        Map<UUID, String> m = new java.util.HashMap<>();
        for (String k : t.keySet()) {
            try {
                m.put(UUID.fromString(k), t.getStringOr(k, ""));
            } catch (IllegalArgumentException ignored) {
                // (skip)
            }
        }
        client = Map.copyOf(m);
    }
}
