package nl.juiced.guhs.feature.verhaal;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalPayloads;
import nl.juiced.guhs.network.ModNetworking;

/**
 * 3.0 (Guhverhalen): the 2.8 talking screen (PraatScherm: a portrait, a speech balloon, answer buttons), for everyone.
 * <ul>
 *   <li>{@link #open}: one balloon of a speaker (an NPC, a story copy, Boris...) with answers;</li>
 *   <li>{@link #scene}: several pages ("Verder »"), each with its own speaker, the answers on the last page;</li>
 *   <li>{@link #sluit}: closes it.</li>
 * </ul>
 * The answer goes back: with a {@code sleutel} ("&lt;pkg&gt;_&lt;x&gt;") to its {@link #luister} listener (any speaker),
 * without one to the role of the GuhNpcEntity that talks ({@code NpcRole.antwoord}). Option -1 = the screen was read to the
 * end / closed (only sent when there is a sleutel). Chat lines stay {@code GuhQuests.say/hint}.
 */
public final class Praat {
    /** An answer button: its id (comes back) and its text (lang key). */
    public record Optie(int id, String tekstKey) {
    }

    /** One page of a scene: who talks (null: nobody drawn), the name above it (lang key, "" = the speaker's own name), the text. */
    public record Regel(@Nullable Entity spreker, String naamKey, String tekstKey, Object... args) {
    }

    @FunctionalInterface
    public interface Antwoord {
        void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie);
    }

    private record Lopend(@Nullable String sleutel, int spreker) {
    }

    private static final Map<String, Antwoord> LUISTERAARS = new ConcurrentHashMap<>();
    private static final Map<UUID, Lopend> LOPEND = new ConcurrentHashMap<>();

    public static void luister(String sleutel, Antwoord a) {
        LUISTERAARS.put(sleutel, a);
    }

    /** One balloon; sleutel may be null (then an NPC speaker's role gets the answer). */
    public static void open(ServerPlayer p, Entity spreker, @Nullable String sleutel, String tekstKey, Object[] args, Optie... opties) {
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "verhaal");
        data.putString("Tekst", tekstKey);
        data.put("Args", args(args));
        data.put("Opties", opties(opties));
        if (sleutel != null) {
            data.putString("Sleutel", sleutel);
        }
        LOPEND.put(p.getUUID(), new Lopend(sleutel, spreker.getId()));
        ModNetworking.sendTo(p, new KnuffeldalPayloads.Open(spreker.getId(), data));
    }

    /** A scene of pages ("Verder »"), the answers on the last page. */
    public static void scene(ServerPlayer p, String sleutel, List<Regel> regels, Optie... opties) {
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "verhaal");
        data.putString("Sleutel", sleutel);
        ListTag paginas = new ListTag();
        int eerste = -1;
        for (Regel r : regels) {
            CompoundTag pg = new CompoundTag();
            int id = r.spreker() == null ? -1 : r.spreker().getId();
            if (eerste < 0) {
                eerste = id;
            }
            pg.putInt("Spreker", id);
            pg.putString("Naam", r.naamKey() == null ? "" : r.naamKey());
            pg.putString("Tekst", r.tekstKey());
            pg.put("Args", args(r.args()));
            paginas.add(pg);
        }
        data.put("Paginas", paginas);
        if (!regels.isEmpty()) {
            data.putString("Tekst", regels.get(0).tekstKey());
        }
        data.put("Opties", opties(opties));
        LOPEND.put(p.getUUID(), new Lopend(sleutel, eerste));
        ModNetworking.sendTo(p, new KnuffeldalPayloads.Open(eerste, data));
    }

    public static void sluit(ServerPlayer p) {
        Lopend l = LOPEND.remove(p.getUUID());
        CompoundTag data = new CompoundTag();
        data.putBoolean("Sluit", true);
        ModNetworking.sendTo(p, new KnuffeldalPayloads.Open(l == null ? -1 : l.spreker(), data));
    }

    /** The sleutel of what this player has open now (null: nothing, or an NPC screen without one). */
    @Nullable
    public static String lopend(ServerPlayer p) {
        Lopend l = LOPEND.get(p.getUUID());
        return l == null ? null : l.sleutel();
    }

    /** (KnuffeldalPayloads.Action) an answer came back from the talking screen. */
    public static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        Lopend l = LOPEND.get(p.getUUID());
        if (l != null && l.sleutel() != null) {
            if (optie < 0) {
                LOPEND.remove(p.getUUID());
            }
            Antwoord a = LUISTERAARS.get(l.sleutel());
            if (a != null) {
                a.antwoord(p, spreker != null && spreker.distanceTo(p) <= 16 ? spreker : null, optie);
            }
            return;
        }
        if (spreker instanceof GuhNpcEntity npc && npc.distanceTo(p) <= 10) {
            NpcRole role = Features.role(npc.getKind());
            if (role != null) {
                role.antwoord(npc, p, optie);
            }
        }
    }

    /** (tests) what a player has open: forget it. */
    public static void vergeet(ServerPlayer p) {
        LOPEND.remove(p.getUUID());
    }

    /** (tests) pretend the screen of this player opened with this sleutel. */
    public static void doeAlsOf(ServerPlayer p, @Nullable String sleutel, @Nullable Entity spreker) {
        LOPEND.put(p.getUUID(), new Lopend(sleutel, spreker == null ? -1 : spreker.getId()));
    }

    private static ListTag args(Object[] args) {
        ListTag list = new ListTag();
        if (args != null) {
            for (Object o : args) {
                if (o instanceof Component c) {   // 1.2.0: as a Component ({A: ...}), resolved by the reader's client
                    CompoundTag a = new CompoundTag();
                    nl.juiced.guhs.taal.Tekst.put(a, "A", c);
                    list.add(a);
                } else {
                    list.add(StringTag.valueOf(String.valueOf(o)));
                }
            }
        }
        return list;
    }

    private static ListTag opties(Optie[] opties) {
        ListTag list = new ListTag();
        for (Optie o : opties) {
            CompoundTag opt = new CompoundTag();
            opt.putInt("Id", o.id());
            opt.putString("Tekst", o.tekstKey());
            list.add(opt);
        }
        return list;
    }

    private Praat() {
    }
}
