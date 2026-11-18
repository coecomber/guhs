package nl.juiced.guhs.feature.guhwinter.npc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Guhwinter (npc slice): the guhs of the Winterplein, and the handles other slices use (CONTRACT.md "## npc").
 * <ul>
 *   <li>{@link #zorgVoor} (also every second by itself): puts every guh the current {@link NpcFase} needs at its spot (anchor
 *   {@code npc_<naam>} of ankers.json, else its fallback inside the plaza), dresses them for the phase, sends away the ones
 *   that should not be there (the Sint, his pakjesguhs and Ozovads from 6 Dec; everyone at the end).</li>
 *   <li>{@link #get}: the entity of a name (a {@link WinterGuh}, or for "ozovads" a horse).</li>
 *   <li>Scenes: {@link #walkTo}, {@link #lookAt}, {@link #say}, {@link #emote}, {@link #hold}, {@link #release},
 *   {@link #teleport}, {@link #afwezig}.</li>
 *   <li>{@link #klikHook}: another slice answers a right-click first (the story while a step is active, the shop).</li>
 * </ul>
 * They are spawned by code and never kept on disk (a leftover that a chunk saved is thrown away when it loads again).
 */
public final class GuhwinterNpcs {
    /** Every entity of this slice has this tag. */
    public static final String TAG = "guhs_guhwinter_npc";
    /** The NPC name in a horse's persistent data (a WinterGuh carries it in its synced data). */
    static final String NAAM = "GuhwinterNpc";

    /** A right-click on a guh of the Winterplein. Return true when you handled it (then nobody else, no idle line). */
    @FunctionalInterface
    public interface KlikHook {
        boolean klik(ServerPlayer speler, String naam, Entity npc);
    }

    /** naam -> the entity that stands for it now. */
    private static final Map<String, UUID> LEVEND = new ConcurrentHashMap<>();
    private static final List<KlikHook> KLIK = new CopyOnWriteArrayList<>();
    private static final Set<String> AFWEZIG = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> KLIK_PAUZE = new ConcurrentHashMap<>();

    private static NpcFaseBron faseBron = NpcFaseBron.ECHT;
    /** Dev: /guhs winter npc fase &lt;fase&gt; (null = the real phase). */
    @Nullable
    static NpcFase devFase;

    /**
     * The story's (or anyone's) extra say over who is on the plein: false = this NPC is not there now (e.g. Ozovads ran off
     * and is waiting at the end of his trail; the verhaal slice puts him there itself). Checked every second.
     */
    public static volatile BiPredicate<ServerLevel, String> aanwezig = (level, naam) -> true;

    private GuhwinterNpcs() {
    }

    // =====================================================================================================================
    // phase
    // =====================================================================================================================

    /** INTEGRATION: kern's phase in one line (see {@link NpcFaseBron}). */
    public static void faseBron(NpcFaseBron bron) {
        faseBron = bron;
    }

    /** The phase the NPCs follow now (the dev override first). */
    public static NpcFase fase(MinecraftServer server) {
        return devFase != null ? devFase : faseBron.fase(server);
    }

    // =====================================================================================================================
    // finding them
    // =====================================================================================================================

    /** The Guhmension (where the Winterplein is), or null. */
    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(ModDimensions.GUHMENSION);
    }

    /** All NPC names (joost, ..., sint, ozovads, pakjesguh_0..3, winkelguh). */
    public static Collection<String> namen() {
        return NpcDefs.alle().keySet();
    }

    /** The entity that stands for this NPC now (null: not spawned, not in a loaded chunk, or not this phase). */
    @Nullable
    public static Entity get(ServerLevel level, String naam) {
        UUID u = LEVEND.get(naam);
        if (u == null) {
            return null;
        }
        Entity e = level.getEntity(u);
        return e != null && e.isAlive() ? e : null;
    }

    /** The NPC name of one of our entities ("" for anything else). */
    public static String naamVan(Entity e) {
        if (e instanceof WinterGuh g) {
            return g.naam();
        }
        return e.entityTags().contains(TAG) ? e.getPersistentData().getStringOr(NAAM, "") : "";
    }

    /** Is this one of the Winterplein's guhs (or Ozovads)? */
    public static boolean isNpc(Entity e) {
        return e instanceof WinterGuh || e.entityTags().contains(TAG);
    }

    // =====================================================================================================================
    // keeping them in the world
    // =====================================================================================================================

    /** Should this NPC be on the plein now? (its phases, {@link #afwezig}, {@link #aanwezig}) */
    public static boolean hoortHier(ServerLevel level, String naam) {
        NpcDefs.Def d = NpcDefs.van(naam);
        return d != null && d.in(fase(level.getServer())) && !AFWEZIG.contains(naam) && aanwezig.test(level, naam);
    }

    /** Every second (the feature's server tick). */
    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 9) {
            return;
        }
        ServerLevel level = level(server);
        if (level != null) {
            zorgVoor(level);
        }
        if (server.getTickCount() % 1200 == 9) {
            long nu = server.getTickCount();
            KLIK_PAUZE.values().removeIf(t -> t < nu);
        }
    }

    /**
     * (Re)spawns the NPCs the current phase needs, at their spots (only where the spot's chunk and its entities are loaded
     * and a player is within 96 blocks), dresses them for the phase, brings strays home and sends away the ones that should
     * not be there. Safe to call any time.
     */
    public static void zorgVoor(ServerLevel level) {
        NpcFase fase = fase(level.getServer());
        for (NpcDefs.Def d : NpcDefs.alle().values()) {
            Entity e = get(level, d.naam());
            if (!hoortHier(level, d.naam())) {
                if (e != null) {
                    weg(e);
                }
                LEVEND.remove(d.naam());
                continue;
            }
            Plek p = plek(level, d);
            if (e == null) {
                BlockPos b = BlockPos.containing(p.pos());
                if (level.areEntitiesActuallyLoadedAndTicking(ChunkPos.containing(b)) && spelerBij(level, p.pos(), 96)) {
                    zet(level, d, p, fase);
                }
                continue;
            }
            if (e instanceof WinterGuh g) {
                if (g.gekleed != fase) {
                    g.kleed(d, fase);
                }
                if (g.thuis == null || g.thuis.distanceToSqr(p.pos()) > 0.25) {
                    g.thuis = p.pos();           // (the anchors file came, or changed after a /reload)
                    g.thuisYaw = p.yaw();
                }
                if (!g.isVast() && g.distanceToSqr(p.pos()) > 24 * 24) {
                    g.snapTo(p.pos().x, p.pos().y, p.pos().z, p.yaw(), 0);   // (wandered off: home)
                    g.getNavigation().stop();
                }
            } else if (e instanceof Mob paard) {
                Ozovads.houdBij(paard, p);
            }
        }
    }

    /** Spawns this NPC right now at its spot (any copy of it goes first). Null: unknown name or it could not be made. */
    @Nullable
    public static Entity zet(ServerLevel level, String naam) {
        NpcDefs.Def d = NpcDefs.van(naam);
        return d == null ? null : zet(level, d, plek(level, d), fase(level.getServer()));
    }

    /** Spawns this NPC at this spot (tests and scenes; the keeper uses its own spot). */
    @Nullable
    public static Entity zet(ServerLevel level, String naam, Vec3 pos, float yaw) {
        NpcDefs.Def d = NpcDefs.van(naam);
        return d == null ? null : zet(level, d, new Plek(pos, yaw), fase(level.getServer()));
    }

    @Nullable
    private static Entity zet(ServerLevel level, NpcDefs.Def d, Plek p, NpcFase fase) {
        Entity oud = get(level, d.naam());
        if (oud != null) {
            oud.discard();
        }
        Entity e;
        if (d.soort() == NpcDefs.Soort.PAARD) {
            e = Ozovads.maak(level, d, p);
        } else {
            WinterGuh g = GuhwinterNpcFeature.GUH.get().create(level, EntitySpawnReason.EVENT);
            if (g != null) {
                g.snapTo(p.pos().x, p.pos().y, p.pos().z, p.yaw(), 0);
                g.setYHeadRot(p.yaw());
                g.setYBodyRot(p.yaw());
                g.stelIn(d, p.pos(), p.yaw(), fase);
            }
            e = g;
        }
        if (e == null) {
            return null;
        }
        e.addTag(TAG);
        e.getPersistentData().putString(NAAM, d.naam());
        LEVEND.put(d.naam(), e.getUUID());     // (before adding: the join check lets only the current one in)
        level.addFreshEntity(e);
        return e;
    }

    /** Away with a little puff of snow. */
    private static void weg(Entity e) {
        if (e.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + 0.6, e.getZ(), 12, 0.35, 0.4, 0.35, 0.02);
            sl.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 0.5, e.getZ(), 5, 0.3, 0.3, 0.3, 0.01);
        }
        e.discard();
    }

    /** Dev: every NPC away now (they come back within a second while they belong here). */
    static int wisAlles(ServerLevel level) {
        int n = 0;
        for (String naam : new ArrayList<>(LEVEND.keySet())) {
            Entity e = get(level, naam);
            if (e != null) {
                weg(e);
                n++;
            }
        }
        LEVEND.clear();
        return n;
    }

    /** Only the current entity of a name may join the world (a copy a chunk saved, e.g. an old Ozovads, is thrown away). */
    static boolean magErbij(Entity e) {
        if (!isNpc(e)) {
            return true;
        }
        UUID u = LEVEND.get(naamVan(e));
        return u != null && u.equals(e.getUUID());
    }

    private static boolean spelerBij(ServerLevel level, Vec3 pos, double r) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(pos.x, p.getY(), pos.z) < r * r) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // spots
    // =====================================================================================================================

    /** Where an NPC stands and which way it looks. */
    public record Plek(Vec3 pos, float yaw) {
    }

    /** This NPC's spot: its first anchor that exists (plus its offset), else its fallback inside the plaza. */
    public static Plek plek(ServerLevel level, NpcDefs.Def d) {
        for (NpcDefs.Ref r : d.ankers()) {
            NpcAnkers.Anker a = NpcAnkers.get(level.getServer(), r.anker());
            if (a != null) {
                return new Plek(vrij(level, a.pos().add(r.dx(), 0, r.dz())), WinterGuh.yaw(a.yaw()));
            }
        }
        int x = (int) Math.floor(d.x()), z = (int) Math.floor(d.z());
        double y = level.hasChunkAt(new BlockPos(x, 0, z)) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) : 31;
        return new Plek(new Vec3(x + 0.5, y, z + 0.5), d.yaw());
    }

    /** Up out of a block (an anchor on a floor block instead of above it), at most 4 blocks. */
    private static Vec3 vrij(ServerLevel level, Vec3 pos) {
        BlockPos b = BlockPos.containing(pos);
        if (!level.hasChunkAt(b)) {
            return pos;
        }
        for (int i = 0; i < 4 && level.getBlockState(b).blocksMotion(); i++) {
            b = b.above();
        }
        return new Vec3(pos.x, Math.max(pos.y, b.getY()), pos.z);
    }

    // =====================================================================================================================
    // scenes
    // =====================================================================================================================

    /** Walk there (and stay, held, until {@link #release}). False: not here now. Ozovads walks too. */
    public static boolean walkTo(ServerLevel level, String naam, Vec3 doel, double snelheid) {
        Entity e = get(level, naam);
        if (e instanceof WinterGuh g) {
            g.loopNaar(doel, snelheid);
            return true;
        }
        if (e instanceof Mob paard) {
            Ozovads.loopNaar(paard, doel, snelheid);
            return true;
        }
        return false;
    }

    /** Is it still on its way ({@link #walkTo})? */
    public static boolean isWalking(ServerLevel level, String naam) {
        Entity e = get(level, naam);
        return e instanceof WinterGuh g ? g.loopt() : e instanceof Mob m && Ozovads.loopt(m);
    }

    /** Look at a spot for a while (ticks). */
    public static boolean lookAt(ServerLevel level, String naam, Vec3 doel, int ticks) {
        Entity e = get(level, naam);
        if (e instanceof WinterGuh g) {
            g.kijkNaar(doel, ticks);
            return true;
        }
        if (e instanceof Mob m) {
            m.getLookControl().setLookAt(doel.x, doel.y, doel.z);
            return true;
        }
        return false;
    }

    /** Look at an entity (a player, another guh) for a while (ticks). */
    public static boolean lookAt(ServerLevel level, String naam, Entity doel, int ticks) {
        Entity e = get(level, naam);
        if (e instanceof WinterGuh g) {
            g.kijkNaar(doel, ticks);
            return true;
        }
        if (e instanceof Mob m) {
            m.getLookControl().setLookAt(doel);
            return true;
        }
        return false;
    }

    /** A line in chat, "&lt;Joostguh&gt; line", for every player within 32 blocks of the NPC (or of the plein's middle). */
    public static void say(ServerLevel level, String naam, Component regel) {
        Entity e = get(level, naam);
        Vec3 bij = e != null ? e.position() : new Vec3(-313, 31, -698);
        Component lijn = lijn(naam, regel);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(bij) < 32 * 32) {
                p.sendSystemMessage(lijn);
            }
        }
    }

    /** A line in chat for one player only. */
    public static void say(ServerPlayer speler, String naam, Component regel) {
        speler.sendSystemMessage(lijn(naam, regel));
    }

    /** "&lt;Name&gt; line" in the style of the other guh characters (GuhQuests.say). */
    public static MutableComponent lijn(String naam, Component regel) {
        return Component.literal("<").append(Component.translatable("guhwinter.npc.naam." + naam)).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(regel.copy().withStyle(ChatFormatting.WHITE));
    }

    /** An emote (wave, dance, hearts, ...). False: not here now, or not a guh (Ozovads just whinnies). */
    public static boolean emote(ServerLevel level, String naam, Emote emote) {
        Entity e = get(level, naam);
        if (e instanceof WinterGuh g) {
            return g.emotes.start(emote, false, GuhEmotes.Source.SELF);
        }
        if (e instanceof Mob m) {
            Ozovads.hinnik(m);
        }
        return false;
    }

    /** A scene holds it where it is (no idling, the keeper leaves it alone) or lets it go. */
    public static boolean hold(ServerLevel level, String naam, boolean vast) {
        Entity e = get(level, naam);
        if (e instanceof WinterGuh g) {
            g.vasthouden(vast);
            return true;
        }
        if (e instanceof Mob m) {
            Ozovads.vast(m, vast);
            return true;
        }
        return false;
    }

    /** The scene is over: back to idling at its own spot. */
    public static void release(ServerLevel level, String naam) {
        Entity e = get(level, naam);
        if (e instanceof WinterGuh g) {
            g.losLaten();
        } else if (e instanceof Mob m) {
            Ozovads.vast(m, false);
        }
    }

    /** Put it there right now (and hold it). */
    public static boolean teleport(ServerLevel level, String naam, Vec3 pos, float yaw) {
        Entity e = get(level, naam);
        if (e == null) {
            return false;
        }
        e.snapTo(pos.x, pos.y, pos.z, yaw, 0);
        e.setYHeadRot(yaw);
        if (e instanceof WinterGuh g) {
            g.setYBodyRot(yaw);
            g.getNavigation().stop();
            g.vasthouden(true);
        } else if (e instanceof Mob m) {
            Ozovads.vast(m, true);
        }
        return true;
    }

    /** Sit down or stand up (a guh; for a scene). */
    public static boolean sit(ServerLevel level, String naam, boolean zit) {
        if (get(level, naam) instanceof WinterGuh g) {
            g.zitten(zit);
            return true;
        }
        return false;
    }

    /** Take an NPC off the plein for now (true) or bring it back (false). Not saved: a restart brings everyone back. */
    public static void afwezig(String naam, boolean weg) {
        if (weg) {
            AFWEZIG.add(naam);
        } else {
            AFWEZIG.remove(naam);
        }
    }

    // =====================================================================================================================
    // clicks
    // =====================================================================================================================

    /** Add a click hook (tried in the order they were added, before the idle lines). */
    public static void klikHook(KlikHook hook) {
        KLIK.add(hook);
    }

    /** A player right-clicked one of ours (server side; WinterGuh.mobInteract and the interact events). */
    static void klik(ServerPlayer speler, Entity npc) {
        long nu = speler.level().getServer().getTickCount();
        Long tot = KLIK_PAUZE.get(speler.getUUID());
        if (tot != null && nu < tot) {
            return;                          // (one click can arrive twice: the specific and the plain interact)
        }
        KLIK_PAUZE.put(speler.getUUID(), nu + 6);
        String naam = naamVan(npc);
        if (naam.isEmpty()) {
            return;
        }
        for (KlikHook h : KLIK) {
            if (h.klik(speler, naam, npc)) {
                return;
            }
        }
        if (npc instanceof WinterGuh g) {
            g.getNavigation().stop();
            g.getLookControl().setLookAt(speler);
        } else if (npc instanceof Mob m) {
            Ozovads.hinnik(m);
        }
        NpcPraat.praat(speler, naam, fase(speler.level().getServer()));
    }
}
