package nl.juiced.guhs.feature.vadsrijk.npc;

import static nl.juiced.guhs.entity.GuhClothes.KNIGHT_ARMOUR;
import static nl.juiced.guhs.entity.GuhClothes.KNIGHT_HELMET;
import static nl.juiced.guhs.entity.GuhClothes.ROYAL_CAPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.wereld.PortaalBlok;
import nl.juiced.guhs.feature.vadsrijk.wereld.Portalen;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Werver Wimpel (fix round 2, g-werver): the recruiter of De Knuffelorde at the ruined order temple in the Guhmension
 * ({@code guhs:vr_w_portaaltempel}). You talk to him first; until then the temple's portal does not wake
 * ({@link #magDoor}, asked by {@link Portalen}). A self-built portal (only after a first visit) never needs him.
 * <p>He is a {@link RijkGuh} like every Vadsrijk NPC (never hurt, never saved): every 2 s, for each player outside the Vadsrijk,
 * a temple within ~40 blocks gets its Wimpel back next to the portal (lazy spawn, so ruins generated before this update get him
 * too). Texts: tools/features/vadsrijk_npc.py ({@code vadsrijk.npc.werver.*}).
 */
public final class Werver {
    public static final String ID = "werver";
    /** {@code SpelerData.gezien}: talked to the recruiter. */
    public static final String GESPROKEN = "vr_npc_werver_gesproken";
    public static final ResourceKey<Structure> TEMPEL = ResourceKey.create(Registries.STRUCTURE, Guhs.id("vr_w_portaaltempel"));
    static final String ACTIE = "werver_klaar";
    /** The first-time tip about the view distance (on arrival in the Vadsrijk). */
    public static final String TIP = "vadsrijk.npc.tip.weergave";
    /** Who says what (QuestMotor.regels): "npc" = Wimpel, "jij" = the player. Keys vadsrijk.npc.werver.gesprek.0.. */
    // U-KARAKTER: + pages 8..10 (texts in tools/features/vadsrijk_t_karakter.py): the three knights with their fixed modes
    static final List<String> GESPREK = List.of("npc", "jij", "npc", "npc", "npc", "npc", "jij", "npc", "npc", "jij", "npc");

    /** Temple (its bounding box, packed) -> the portal spot found in it (empty: no portal there). */
    private static final Map<Long, Optional<Plek>> TEMPELS = new HashMap<>();
    /** Temple -> its Wimpel now. */
    private static final Map<Long, UUID> LEVEND = new HashMap<>();

    /** Where Wimpel stands and which way he looks, and the middle of the portal. */
    record Plek(BlockPos stand, float yaw, BlockPos portaal) {
    }

    private Werver() {
    }

    static void init() {
        QuestMotor.KLIK_VOOR.add((p, npc) -> {
            if (!ID.equals(npc.npcId())) {
                return false;
            }
            praat(p, npc);
            return true;
        });
        QuestMotor.ACTIES.put(ACTIE, (p, rest) -> gesproken(p, true));
    }

    // ---- the gate -------------------------------------------------------------------------------------------------------------

    /** Did this player talk to Wimpel (or come in before this update: they already know the way)? */
    public static boolean gesproken(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        return d.gezien.contains(GESPROKEN) || d.gezien.contains(Portalen.ONTWAAKT) || d.gezien.contains(Portalen.BINNEN_GEWEEST);
    }

    static void gesproken(ServerPlayer p, boolean ja) {
        if (ja) {
            Spelers.data(p).gezien.add(GESPROKEN);
        } else {
            Spelers.data(p).gezien.remove(GESPROKEN);
        }
    }

    /**
     * May this player go through this portal block from outside the Vadsrijk? A sleeping portal (only in the temple) or an
     * awake one inside a temple (woken by someone else) wants the talk first; a self-built portal never does.
     */
    public static boolean magDoor(ServerPlayer p, ServerLevel sl, BlockPos pos, boolean wakker) {
        if (gesproken(p)) {
            return true;
        }
        return wakker && !inTempel(sl, pos);
    }

    /** The friendly "talk to Wimpel first" (the portal stays asleep); Wimpel waves. */
    public static void weiger(ServerPlayer p) {
        p.sendOverlayMessage(Component.translatable("vadsrijk.npc.werver.eerst_praten").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (p.level() instanceof ServerLevel sl) {
            RijkGuh g = dichtsbij(sl, p.blockPosition(), 64);
            if (g != null) {
                g.emotes.start(nl.juiced.guhs.feature.emotes.Emote.ZWAAIEN, false, nl.juiced.guhs.feature.emotes.GuhEmotes.Source.SELF);
            }
        }
    }

    static boolean inTempel(ServerLevel sl, BlockPos pos) {
        if (nl.juiced.guhs.feature.vadsrijk.wereld.VasteTempel.in(sl, pos)) {
            return true;   // (y-tempel) the fixed temple counts like a worldgen one
        }
        Structure s = structuur(sl);
        return s != null && sl.structureManager().getStructureWithPieceAt(pos, s).isValid();
    }

    @Nullable
    private static Structure structuur(ServerLevel sl) {
        return sl.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(TEMPEL);
    }

    // ---- the talk -------------------------------------------------------------------------------------------------------------

    static void praat(ServerPlayer p, RijkGuh npc) {
        String id = npc.npcId();
        if (Spelers.data(p).gezien.contains(GESPROKEN)) {
            QuestMotor.open(p, npc, List.of(new Praat.Regel(npc, "", "vadsrijk.npc.werver.weer")), new QuestMotor.Gesprek(id, npc.getId(), "", "", ""));
            return;
        }
        QuestMotor.open(p, npc, QuestMotor.regels(p, npc, GESPREK, "vadsrijk.npc.werver.gesprek."),
                new QuestMotor.Gesprek(id, npc.getId(), "", "", ACTIE));
    }

    // ---- keeping him at the temple ----------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 23) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || Vadsrijk.in(p) || !(p.level() instanceof ServerLevel sl)) {
                continue;
            }
            BlockPos b = p.blockPosition();
            BoundingBox vast = nl.juiced.guhs.feature.vadsrijk.wereld.VasteTempel.doos(sl);
            if (vast != null && vast.inflatedBy(48).isInside(b)) {
                houdBij(sl, vast);   // (y-tempel) the fixed temple gets its Wimpel too
            }
            Structure s = structuur(sl);
            if (s == null) {
                return;
            }
            for (BlockPos q : new BlockPos[] {b, b.offset(40, 0, 0), b.offset(-40, 0, 0), b.offset(0, 0, 40), b.offset(0, 0, -40)}) {
                if (!sl.isLoaded(q)) {
                    continue;
                }
                StructureStart start = sl.structureManager().getStructureAt(q, s);
                if (start != null && start.isValid()) {
                    houdBij(sl, start.getBoundingBox());
                    break;
                }
            }
        }
    }

    /** Makes sure this temple has its Wimpel (spawns him when he is not there). */
    static void houdBij(ServerLevel sl, BoundingBox box) {
        long key = BlockPos.asLong(box.minX(), box.minY(), box.minZ());
        RijkGuh g = levend(sl, LEVEND.get(key));
        if (g != null) {
            return;
        }
        Optional<Plek> plek = TEMPELS.get(key);
        if (plek == null) {
            plek = Optional.ofNullable(zoekPortaal(sl, box));
            if (plek.isEmpty() && !geladen(sl, box)) {
                return;   // (try again when the whole temple is loaded)
            }
            TEMPELS.put(key, plek);
        }
        if (plek.isPresent()) {
            g = zet(sl, plek.get());
            if (g != null) {
                LEVEND.put(key, g.getUUID());
            }
        }
    }

    @Nullable
    private static RijkGuh levend(ServerLevel sl, @Nullable UUID u) {
        return u != null && sl.getEntity(u) instanceof RijkGuh g && g.isAlive() ? g : null;
    }

    @Nullable
    private static RijkGuh dichtsbij(ServerLevel sl, BlockPos pos, double r) {
        RijkGuh best = null;
        for (UUID u : LEVEND.values()) {
            RijkGuh g = levend(sl, u);
            if (g != null && g.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < r * r) {
                best = g;
            }
        }
        return best;
    }

    private static boolean geladen(ServerLevel sl, BoundingBox box) {
        for (int x = box.minX(); x <= box.maxX() + 15; x += 16) {
            for (int z = box.minZ(); z <= box.maxZ() + 15; z += 16) {
                if (!sl.isLoaded(new BlockPos(Math.min(x, box.maxX()), box.minY(), Math.min(z, box.maxZ())))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The portal in this temple: its bottom middle; Wimpel stands 4 blocks in front of it (the side of the temple's stairs, i.e.
     * towards the middle of the box) and 3 to the side, looking out at whoever comes up.
     */
    @Nullable
    static Plek zoekPortaal(ServerLevel sl, BoundingBox box) {
        int n = 0, minY = Integer.MAX_VALUE;
        long sx = 0, sz = 0;
        Direction.Axis as = Direction.Axis.X;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    m.set(x, y, z);
                    if (!sl.isLoaded(m)) {
                        continue;
                    }
                    BlockState s = sl.getBlockState(m);
                    if (s.getBlock() instanceof PortaalBlok) {
                        n++;
                        sx += x;
                        sz += z;
                        minY = Math.min(minY, y);
                        as = s.getValue(PortaalBlok.AXIS);
                    }
                }
            }
        }
        if (n == 0) {
            return null;
        }
        return plek(new BlockPos((int) Math.round((double) sx / n), minY, (int) Math.round((double) sz / n)), as, box.getCenter());
    }

    /** Wimpel's spot in front of a portal (bottom middle {@code portaal}, plane along {@code as}) on the side of {@code midden}. */
    static Plek plek(BlockPos portaal, Direction.Axis as, BlockPos midden) {
        // portal along X: its faces look north/south (normal z); along Z: east/west (normal x)
        int nx = as == Direction.Axis.X ? 0 : 1, nz = as == Direction.Axis.X ? 1 : 0;
        int teken = (midden.getX() - portaal.getX()) * nx + (midden.getZ() - portaal.getZ()) * nz >= 0 ? 1 : -1;
        nx *= teken;
        nz *= teken;
        BlockPos stand = portaal.offset(nx * 4 + nz * 3, 0, nz * 4 + nx * 3);
        float yaw = (float) Math.toDegrees(Math.atan2(-nx, nz));
        return new Plek(stand, yaw, portaal);
    }

    /** Puts a Wimpel at this spot (also the dev command / tests). */
    @Nullable
    public static RijkGuh zet(ServerLevel sl, Plek plek) {
        BlockPos s = Npcs.staanPlek(sl, plek.stand());
        NpcDef d = def(s, plek.yaw());
        RijkGuh g = NpcRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        if (g == null) {
            return null;
        }
        g.snapTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, d.yaw(), 0);
        g.setYHeadRot(d.yaw());
        g.setYBodyRot(d.yaw());
        g.stelIn(d);
        sl.addFreshEntity(g);
        return g;
    }

    /** Dev: a Wimpel right here (he is remembered like a temple's, so the gate's wave finds him). */
    static RijkGuh zetHier(ServerPlayer p) {
        ServerLevel sl = (ServerLevel) p.level();
        RijkGuh g = zet(sl, new Plek(p.blockPosition(), p.getYRot() + 180, p.blockPosition()));
        if (g != null) {
            LEVEND.put(p.blockPosition().asLong(), g.getUUID());
        }
        return g;
    }

    static NpcDef def(BlockPos plek, float yaw) {
        return new NpcDef(ID, plek, yaw, GuhVariant.SNOW, List.of(KNIGHT_HELMET, KNIGHT_ARMOUR, ROYAL_CAPE), NpcDef.Gedrag.STAAN, null, 1.05f);
    }

    /** (y-tempel) a fixed temple was taken away: forget its portal spot and its Wimpel. */
    public static void vergeet(BoundingBox box) {
        long key = BlockPos.asLong(box.minX(), box.minY(), box.minZ());
        TEMPELS.remove(key);
        LEVEND.remove(key);
    }

    /** (server stop) */
    static void wis() {
        TEMPELS.clear();
        LEVEND.clear();
    }
}
