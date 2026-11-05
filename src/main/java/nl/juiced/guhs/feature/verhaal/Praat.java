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

    private record Lopend(@Nullable String sleutel, int spreker, long sinds, net.minecraft.world.phys.Vec3 plek) {
        Lopend(ServerPlayer p, @Nullable String sleutel, int spreker) {
            this(sleutel, spreker, p.level().getServer() == null ? 0 : p.level().getServer().getTickCount(), p.position());
        }
    }

    /** bbq2: how long a screen without a sleutel (whose closing is never reported) counts as "still talking" (ticks). */
    private static final int PRAAT_TICKS = 200;

    /**
     * q-dialoog: where a page's portrait comes from when the page has no speaker in the world (a far NPC, a narrator line
     * that names someone): a feature maps the page's name key to a {@link #portret} (null: not one of mine).
     */
    @FunctionalInterface
    public interface PortretBron {
        @Nullable
        CompoundTag portret(String naamKey);
    }

    private static final List<PortretBron> PORTRET_BRONNEN = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void portretBron(PortretBron b) {
        PORTRET_BRONNEN.add(b);
    }

    /**
     * T-DIALOOG: a feature may add its own tags to what goes to the talking screen (Het Vadsrijk: "Rijk" + per page the
     * faction, voice and emotion, the "what now?" objective); called just before sending, with the sleutel (may be null) and
     * the conversation's first speaker in the world other than the reader (may be null).
     */
    @FunctionalInterface
    public interface Versierder {
        void versier(ServerPlayer p, @Nullable String sleutel, @Nullable Entity spreker, CompoundTag data);
    }

    private static final List<Versierder> VERSIERDERS = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** T-DIALOOG */
    public static void versierder(Versierder v) {
        VERSIERDERS.add(v);
    }

    private static void versier(ServerPlayer p, @Nullable String sleutel, @Nullable Entity spreker, CompoundTag data) {   // T-DIALOOG
        for (Versierder v : VERSIERDERS) {
            try {
                v.versier(p, sleutel, spreker, data);
            } catch (RuntimeException ex) {
                com.mojang.logging.LogUtils.getLogger().warn("Praat versierder failed for {}", sleutel, ex);   // (never block a conversation)
            }
        }
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
        if (spreker == p) {
            data.putBoolean("Speler", true);
        } else {
            CompoundTag portret = portret(spreker);
            if (portret != null) {
                data.put("Portret", portret);
            }
        }
        versier(p, sleutel, spreker == p ? null : spreker, data);   // T-DIALOOG
        LOPEND.put(p.getUUID(), new Lopend(p, sleutel, spreker.getId()));
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
            // q-dialoog: the player's own lines show the player (the reader's own client draws itself, not by id: a
            // speaker id of the player made the screen "talk to" the player); every other page carries a portrait to fall
            // back on when its speaker is not (or no longer) on the reader's client
            boolean speler = r.spreker() == p;
            int id = r.spreker() == null || speler ? -1 : r.spreker().getId();
            if (eerste < 0) {
                eerste = id;
            }
            pg.putInt("Spreker", id);
            CompoundTag portret = speler ? null : r.spreker() != null ? portret(r.spreker()) : portretVan(r.naamKey());
            if (speler) {
                pg.putBoolean("Speler", true);
            } else if (portret != null) {
                pg.put("Portret", portret);
            }
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
        Entity eersteSpreker = null;   // T-DIALOOG: the first speaker in the world that is not the reader
        for (Regel r : regels) {
            if (r.spreker() != null && r.spreker() != p) {
                eersteSpreker = r.spreker();
                break;
            }
        }
        versier(p, sleutel, eersteSpreker, data);   // T-DIALOOG
        LOPEND.put(p.getUUID(), new Lopend(p, sleutel, eerste));
        ModNetworking.sendTo(p, new KnuffeldalPayloads.Open(eerste, data));
    }

    public static void sluit(ServerPlayer p) {
        Lopend l = LOPEND.remove(p.getUUID());
        CompoundTag data = new CompoundTag();
        data.putBoolean("Sluit", true);
        if (l != null && l.sleutel() != null) {
            data.putString("Van", l.sleutel());   // (q-dialoog: the client only closes this conversation)
        }
        ModNetworking.sendTo(p, new KnuffeldalPayloads.Open(l == null ? -1 : l.spreker(), data));
    }

    /** The sleutel of what this player has open now (null: nothing, or an NPC screen without one). */
    @Nullable
    public static String lopend(ServerPlayer p) {
        Lopend l = LOPEND.get(p.getUUID());
        return l == null ? null : l.sleutel();
    }

    /**
     * bbq2: is this player reading a talking screen right now ({@link Duwtje#mag} leaves them alone)? A screen with a
     * sleutel counts until its end is reported; one without (an NPC's own answers: closing is never reported) for
     * {@link #PRAAT_TICKS} ticks, as long as the player did not walk away.
     */
    public static boolean bezig(ServerPlayer p) {
        Lopend l = LOPEND.get(p.getUUID());
        if (l == null || p.level().getServer() == null) {
            return false;
        }
        long duurt = p.level().getServer().getTickCount() - l.sinds();
        if (duurt < 0 || p.position().distanceToSqr(l.plek()) > 2.25) {
            return false;
        }
        return l.sleutel() != null ? duurt < 20 * 300 : duurt < PRAAT_TICKS;
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
        LOPEND.put(p.getUUID(), new Lopend(p, sleutel, spreker == null ? -1 : spreker.getId()));
    }

    /**
     * q-dialoog: what the talking screen needs to draw this speaker when it is not on the reader's client: its entity type,
     * and for a guh its colour, clothes and pose, for a story NPC its kind. Null for a player (only drawn when seen) or a
     * non-living thing.
     */
    @Nullable
    public static CompoundTag portret(Entity e) {
        if (!(e instanceof net.minecraft.world.entity.LivingEntity) || e instanceof net.minecraft.world.entity.player.Player) {
            return null;
        }
        CompoundTag t = new CompoundTag();
        t.putString("Type", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString());
        if (e instanceof nl.juiced.guhs.entity.GuhEntity g) {
            List<nl.juiced.guhs.entity.GuhClothes> kleren = new java.util.ArrayList<>();
            for (nl.juiced.guhs.entity.GuhClothes.Slot slot : nl.juiced.guhs.entity.GuhClothes.Slot.values()) {
                nl.juiced.guhs.entity.GuhClothes c = g.getClothes(slot);
                if (c != null) {
                    kleren.add(c);
                }
            }
            vulGuh(t, g.getVariant(), kleren, g.isInSittingPose());
        }
        if (e instanceof GuhNpcEntity n) {
            t.putString("Soort", n.getKind().name());
        }
        return t;
    }

    /** q-dialoog: a guh's portrait without the guh (a far NPC from its definition). */
    public static CompoundTag guhPortret(net.minecraft.world.entity.EntityType<?> type, nl.juiced.guhs.entity.GuhVariant variant,
            java.util.Collection<nl.juiced.guhs.entity.GuhClothes> kleren, boolean zit) {
        CompoundTag t = new CompoundTag();
        t.putString("Type", net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        vulGuh(t, variant, kleren, zit);
        return t;
    }

    private static void vulGuh(CompoundTag t, nl.juiced.guhs.entity.GuhVariant variant, java.util.Collection<nl.juiced.guhs.entity.GuhClothes> kleren,
            boolean zit) {
        t.putString("Variant", variant.name());
        ListTag l = new ListTag();
        for (nl.juiced.guhs.entity.GuhClothes c : kleren) {
            l.add(StringTag.valueOf(c.name()));
        }
        t.put("Kleren", l);
        t.putBoolean("Zit", zit);
    }

    @Nullable
    private static CompoundTag portretVan(@Nullable String naamKey) {
        if (naamKey == null || naamKey.isEmpty()) {
            return null;
        }
        for (PortretBron b : PORTRET_BRONNEN) {
            CompoundTag t = b.portret(naamKey);
            if (t != null) {
                return t;
            }
        }
        return null;
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
