package nl.juiced.guhs.feature.ringh1;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.item.GuhCompassItem;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.ModDimensions;

/**
 * bbq2 (ring-h1): the places of chapter 1 of the Knabbelring. Templates and numbers: tools/features/ring_h1_bouw.py (its
 * self-check compares the numbers below with its own).
 * <ul>
 *   <li><b>The Knabbelgouw</b> (structure guhs:knabbelgouw, one template of 81 x 26 x 81): the big barbecueput with the
 *       heuvelholletjes, the feestwei and Guhdalf's camp around it, as one whole. Only in the Guhmensie, only in chunks made
 *       with this update: there it takes the place of the big pit (the structure set guhs:barbecueput holds both; in the
 *       open the structure guhs:barbecueput only makes small pits from now on).</li>
 *   <li><b>Guhdalf's camp at an old big pit</b> (prop template guhs:ringh1_kamp): {@link Bezetting} puts it once per pit on
 *       the first free spot of {@link #KEUZES}, a ring of spots around the pit: only where there is nothing but air, plants and
 *       natural ground, so never over something a player built. A pit without a free spot has no camp and no Guhdalf.</li>
 *   <li>Who lives where: Guhdalf and (until he joins a player) Sam-guh at every camp, four Gouwguhs in the Knabbelgouw. All
 *       of them come through {@link Bezetting}, also in a new Knabbelgouw (one code path for old and new).</li>
 *   <li>{@link #dichtstbij}: the nearest place where Guhdalf stands (a Knabbelgouw or an old big pit): what the Superkompas
 *       entry "Mijn verhaal" points at before a player met him.</li>
 * </ul>
 */
public final class Gouw {
    public static final String STRUCTUUR = "knabbelgouw", PUT = "barbecueput", PUT_STUK = "barbecueput_groot";
    /** The camp: its prop template, its Bezetting id and the name of its protected box. */
    public static final String KAMP = "ringh1_kamp";
    public static final Identifier KAMP_TEMPLATE = Guhs.id(KAMP);
    /** The size of the camp (x, y, z) and its spots, camp-local (y 0 = the plate; the open side is south). */
    public static final BlockPos KAMP_MAAT = new BlockPos(15, 9, 11);
    public static final BlockPos GUHDALF = new BlockPos(10, 1, 6), TAFEL = new BlockPos(7, 1, 7), SAM = new BlockPos(5, 1, 9);
    /** The ground layer of the Knabbelgouw and of the big pit (the block you walk on). */
    public static final int G = 4;
    /** Template coordinates in the Knabbelgouw: the corner of the camp (not turned), Sam-guh's garden, the corner of the pit. */
    public static final BlockPos GOUW_KAMP = new BlockPos(46, G, 17), GOUW_SAM = new BlockPos(10, G + 1, 47), GOUW_PUT = new BlockPos(22, 0, 42);
    /** Template coordinates of the residents of the Knabbelgouw. */
    public static final List<BlockPos> BEWONERS = List.of(new BlockPos(15, 5, 13), new BlockPos(65, 5, 13), new BlockPos(70, 5, 32), new BlockPos(28, 5, 25));
    /** Coordinates in the big pit (template barbecueput_groot): its middle and the inside of its grillkool frame. */
    public static final BlockPos PUT_MIDDEN = new BlockPos(18, G + 1, 18), FRAME = new BlockPos(18, G + 2, 26);
    public static final int PUT_MAAT = 37;
    /** Bezetting ids. */
    public static final String GUHDALF_GOUW = "ringh1_guhdalf", SAM_GOUW = "ringh1_sam", GUHDALF_KAMP = "ringh1_kamp_guhdalf", SAM_KAMP = "ringh1_kamp_sam",
            BEWONER = "ringh1_bewoner_";
    /** Guhdalf's NpcRollen plek here. */
    public static final String ROL = "gouw";
    /** A camp may stand this far from the middle of its pit (what Bezetting looks around for Guhdalf and Sam-guh). */
    public static final int KAMP_ZOEK = 46;
    /** Sam-guh waits at home for players whose step of ring_h1 is 0..this. */
    public static final int SAM_THUIS_TOT = 3;

    /**
     * Where the corner of the camp may go at an old big pit: template coordinates of barbecueput_groot (37 x 37, the plate in
     * its ground layer), a ring of spots around the pit from near to far. North of the pit first: the camp's open side then
     * looks at the pit's way in. (Bezetting also tries every spot one and two blocks higher and lower.)
     */
    public static final List<BlockPos> KEUZES = keuzes();

    private static List<BlockPos> keuzes() {
        List<BlockPos> uit = new ArrayList<>();
        int breed = KAMP_MAAT.getX(), diep = KAMP_MAAT.getZ();
        for (int ver : new int[]{3, 7, 12}) {
            int noord = -diep - ver + 1, zuid = PUT_MAAT + ver - 1, west = -breed - ver + 1, oost = PUT_MAAT + ver - 1;
            for (int x : new int[]{11, 0, 22}) {
                uit.add(new BlockPos(x, G, noord));
            }
            for (int z : new int[]{13, 2, 24}) {
                uit.add(new BlockPos(oost, G, z));
                uit.add(new BlockPos(west, G, z));
            }
            for (int x : new int[]{11, 0, 22}) {
                uit.add(new BlockPos(x, G, zuid));
            }
            uit.add(new BlockPos(west, G, noord));
            uit.add(new BlockPos(oost, G, noord));
            uit.add(new BlockPos(west, G, zuid));
            uit.add(new BlockPos(oost, G, zuid));
        }
        return List.copyOf(uit);
    }

    /** A camp in the world: its corner (the plate's layer) and how it is turned. */
    public record Plek(BlockPos hoek, Rotation draai) {
        /** The world position of a camp-local block. */
        public BlockPos wereld(BlockPos lokaal) {
            return hoek.offset(StructureTemplate.transform(lokaal, Mirror.NONE, draai, BlockPos.ZERO));
        }

        public BoundingBox doos() {
            return BoundingBox.fromCorners(hoek, wereld(KAMP_MAAT.offset(-1, -1, -1)));
        }
    }

    static void registreer() {
        // --- an old big pit: the camp on the free strip, then Guhdalf and Sam-guh at it ---
        Bezetting.blokken(KAMP, PUT, PUT_STUK, KEUZES, KAMP_TEMPLATE);
        Bezetting.wezen(GUHDALF_KAMP, PUT, PUT_STUK, PUT_MIDDEN, (level, plek, draai) -> {
            Plek kamp = kampBijPut(level, BlockPos.containing(plek));
            return kamp == null ? null : guhdalf(level, Vec3.atBottomCenterOf(kamp.wereld(GUHDALF)), kamp.draai());
        }, KAMP_ZOEK);
        Bezetting.wezen(SAM_KAMP, PUT, PUT_STUK, PUT_MIDDEN, (level, plek, draai) -> {
            Plek kamp = kampBijPut(level, BlockPos.containing(plek));
            return kamp == null ? null : samThuis(level, kamp.wereld(SAM));
        }, KAMP_ZOEK);
        for (String id : new String[]{KAMP, GUHDALF_KAMP, SAM_KAMP}) {
            Bezetting.alleenIn(id, ModDimensions.GUHMENSION);
        }
        // --- the Knabbelgouw ---
        Bezetting.npc(GUHDALF_GOUW, STRUCTUUR, null, GOUW_KAMP.offset(GUHDALF), GuhNpcEntity.Kind.GUHDALF, ROL, 0f);
        Bezetting.wezen(SAM_GOUW, STRUCTUUR, null, GOUW_SAM, (level, plek, draai) -> samThuis(level, BlockPos.containing(plek)));
        for (int i = 0; i < BEWONERS.size(); i++) {
            int nr = i;
            Bezetting.wezen(BEWONER + i, STRUCTUUR, null, BEWONERS.get(i), (level, plek, draai) -> bewoner(level, plek, draai, nr));
        }
        // whole for everybody; the pit in its middle stays what it always was (mend the frame, light it)
        Bescherming.registreer(STRUCTUUR, 0);
        Bescherming.uitzondering(STRUCTUUR, (p, pos) -> inPut(p.level(), pos));
    }

    // =====================================================================================================================
    // who lives here
    // =====================================================================================================================

    /** Guhdalf at a camp (not yet in the world): he sits, looks out over the open side and talks with the role of the Gouw. */
    @Nullable
    static GuhNpcEntity guhdalf(ServerLevel level, Vec3 plek, Rotation draai) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
        if (npc == null) {
            return null;
        }
        npc.setKind(GuhNpcEntity.Kind.GUHDALF);
        npc.roleData.putString(NpcRollen.PLEK, ROL);
        npc.setYRot(0f);
        float y = npc.rotate(draai);
        npc.snapTo(plek.x, plek.y, plek.z, y, 0f);
        npc.setYBodyRot(y);
        npc.setYHeadRot(y);
        npc.setInvulnerable(true);
        npc.setPersistenceRequired();
        return npc;
    }

    /**
     * Sam-guh at home (not yet in the world): a story copy of the Sam-guh that only exists for players he did not join yet
     * (their step of ring_h1 is 0..{@link #SAM_THUIS_TOT}); the moment he walks along he is that player's own companion.
     */
    @Nullable
    static GuhEntity samThuis(ServerLevel level, BlockPos plek) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.setVariant(GuhVariant.SAM_GUH);
        guh.setGuhScale(VerhaalGuhs.SCHAAL);
        guh.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, 0f, 0f);
        VerhaalGuhs.markeer(guh, VerhaalGuh.SAM_GUH, plek);
        Zicht.alleenBij(guh, RingH1Feature.LIJN.id(), 0, SAM_THUIS_TOT);
        return guh;
    }

    /** Is this guh a Sam-guh who waits at home (of a camp or of the Knabbelgouw)? */
    public static boolean isSamThuis(Entity e) {
        String tag = e.getPersistentData().getStringOr(Bezetting.TAG, "");
        return tag.startsWith(SAM_GOUW) || tag.startsWith(SAM_KAMP);
    }

    private static final GuhVariant[] BEWONER_VARIANT = {GuhVariant.CHOCO, GuhVariant.MINT, GuhVariant.SNOW, GuhVariant.NORMAL};

    /** Resident nr of the Knabbelgouw (not yet in the world): a guh with a name that sits at home. */
    @Nullable
    static GuhEntity bewoner(ServerLevel level, Vec3 plek, Rotation draai, int nr) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.setVariant(BEWONER_VARIANT[nr % BEWONER_VARIANT.length]);
        guh.setGuhScale(nr == 3 ? 0.85f : 1.0f);
        guh.setYRot(nr == 2 ? 90f : 0f);
        float y = guh.rotate(draai);
        guh.snapTo(plek.x, plek.y, plek.z, y, 0f);
        guh.setYBodyRot(y);
        guh.setYHeadRot(y);
        guh.setCustomName(Component.translatable("entity.guhs.ringh1.bewoner." + nr));
        guh.setOrderedToSit(true);
        guh.setInSittingPose(true);
        guh.setPersistenceRequired();
        guh.setInvulnerable(true);
        return guh;
    }

    /** Which resident of the Knabbelgouw this guh is (-1: none). */
    public static int bewoner(Entity guh) {
        String tag = guh.getPersistentData().getStringOr(Bezetting.TAG, "");
        if (!tag.startsWith(BEWONER)) {
            return -1;
        }
        int at = tag.indexOf('@');
        try {
            return Integer.parseInt(tag.substring(BEWONER.length(), at < 0 ? tag.length() : at));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** (GuhHooks.tick, every guh, server) a resident stays busy with being at home: a passing ring bearer does not lure it away. */
    static void bewonerTick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 100 == 0 && bewoner(guh) >= 0) {
            GuhHooks.bezig(guh, 240);
            if (!guh.isOrderedToSit()) {
                guh.setOrderedToSit(true);
                guh.setInSittingPose(true);
            }
        }
    }

    // =====================================================================================================================
    // where things are
    // =====================================================================================================================

    /** The camp of the old big pit near this spot (null: no big pit here, or it has no camp (yet)). */
    @Nullable
    public static Plek kampBijPut(ServerLevel level, BlockPos bij) {
        StructureStart start = Bezetting.start(level, PUT, bij);
        return start == null ? null : kampVan(level, start);
    }

    /** The camp of this old big pit (null: this copy is a small pit, or no camp was placed). */
    @Nullable
    public static Plek kampVan(ServerLevel level, StructureStart start) {
        if (Kopieen.wereld(start, PUT_STUK, BlockPos.ZERO) == null) {
            return null;
        }
        Optional<BlockPos> hoek = Bezetting.Geplaatst.get(level).plek(KAMP, start);
        return hoek.map(h -> new Plek(h, Kopieen.draai(start, PUT_STUK))).orElse(null);
    }

    /** The camp on the feestwei of this Knabbelgouw. */
    @Nullable
    public static Plek kampVanGouw(StructureStart start) {
        BlockPos hoek = Kopieen.wereld(start, null, GOUW_KAMP);
        return hoek == null ? null : new Plek(hoek, Kopieen.draai(start, null));
    }

    /**
     * The camp Guhdalf sits at, read back from the world: the party table stands at a fixed spot next to him, which says how
     * the camp is turned. Works for every camp (the Knabbelgouw, an old pit, one put down by a test or a command). Without a
     * table near him: a camp that is not turned, with him on his spot.
     */
    public static Plek kampBij(ServerLevel level, BlockPos guhdalf) {
        BlockPos naarTafel = TAFEL.subtract(GUHDALF);
        for (Rotation draai : Rotation.values()) {
            if (level.getBlockState(guhdalf.offset(StructureTemplate.transform(naarTafel, Mirror.NONE, draai, BlockPos.ZERO))).is(RingH1Feature.FEESTTAFEL.get())) {
                return new Plek(guhdalf.subtract(StructureTemplate.transform(GUHDALF, Mirror.NONE, draai, BlockPos.ZERO)), draai);
            }
        }
        return new Plek(guhdalf.subtract(GUHDALF), Rotation.NONE);
    }

    /** The inside of the grillkool frame of the big pit near this spot (of a Knabbelgouw or an old pit), or null. */
    @Nullable
    public static BlockPos frame(ServerLevel level, BlockPos bij) {
        StructureStart gouw = Bezetting.start(level, STRUCTUUR, bij);
        if (gouw != null) {
            return Kopieen.wereld(gouw, null, GOUW_PUT.offset(FRAME));
        }
        StructureStart put = Bezetting.start(level, PUT, bij);
        return put == null ? null : Kopieen.wereld(put, PUT_STUK, FRAME);
    }

    /** Is this spot in the pit of a Knabbelgouw (where the Grillguh's quest lets players break and place)? */
    public static boolean inPut(ServerLevel level, BlockPos pos) {
        StructureStart gouw = Bezetting.start(level, STRUCTUUR, pos);
        BlockPos lokaal = gouw == null ? null : Kopieen.lokaal(gouw, null, pos);
        return lokaal != null && lokaal.getX() >= GOUW_PUT.getX() && lokaal.getX() < GOUW_PUT.getX() + PUT_MAAT && lokaal.getZ() >= GOUW_PUT.getZ()
                && lokaal.getZ() < GOUW_PUT.getZ() + PUT_MAAT;
    }

    /**
     * (about every two seconds per player) the camps of the old pits around this spot are protected boxes: the tent and the
     * cart stay whole. Boxes put down by code are not saved, so this simply says it again.
     */
    static void beschermKampen(ServerLevel level, BlockPos bij) {
        if (level.dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        Structure put = Kopieen.structuur(level, PUT);
        if (put == null) {
            return;
        }
        for (StructureStart start : Kopieen.bij(level, put, bij, Bezetting.BEREIK)) {
            Plek kamp = kampVan(level, start);
            if (kamp != null) {
                Bescherming.zetDoos(level, KAMP, kamp.doos());
            }
        }
    }

    // =====================================================================================================================
    // the nearest Guhdalf
    // =====================================================================================================================

    /** How many grid cells of the barbecueput set are looked at around a player, and how long an answer is kept. */
    public static final int ZOEK_CELLEN = 4, ZOEK_TICKS = 1200;

    private record Gezocht(Optional<BlockPos> plek, long tick, BlockPos vanaf) {
    }

    private static final Map<UUID, Gezocht> GEZOCHT = new ConcurrentHashMap<>();

    /**
     * The nearest place in the Guhmensie where Guhdalf stands: the middle of a Knabbelgouw or of an old big barbecueput
     * (small pits and pits that got no camp don't count). Looked up at most once a minute per player (or after 96 blocks);
     * null: not in the Guhmensie, or none within reach.
     */
    @Nullable
    public static BlockPos dichtstbij(ServerPlayer p) {
        ServerLevel level = p.level();
        if (level.dimension() != ModDimensions.GUHMENSION) {
            return null;
        }
        long nu = level.getServer().getTickCount();
        Gezocht g = GEZOCHT.get(p.getUUID());
        if (g != null && nu - g.tick() < ZOEK_TICKS && g.vanaf().closerThan(p.blockPosition(), 96)) {
            return g.plek().orElse(null);
        }
        BlockPos gevonden = zoek(level, p.blockPosition());
        GEZOCHT.put(p.getUUID(), new Gezocht(Optional.ofNullable(gevonden), nu, p.blockPosition()));
        return gevonden;
    }

    /** {@link #dichtstbij} without the memory. */
    @Nullable
    public static BlockPos zoek(ServerLevel level, BlockPos van) {
        BlockPos gouw = GuhCompassItem.findCenter(level, ResourceKey.create(Registries.STRUCTURE, Guhs.id(STRUCTUUR)), van);
        BlockPos put = oudePut(level, van, gouw == null ? Double.MAX_VALUE : gouw.distSqr(van));
        if (gouw == null || put == null) {
            return gouw == null ? put : gouw;
        }
        return put.distSqr(van) < gouw.distSqr(van) ? put : gouw;
    }

    /** The nearest big barbecueput (a start of guhs:barbecueput whose piece is barbecueput_groot) nearer than {@code binnen} (squared). */
    @Nullable
    private static BlockPos oudePut(ServerLevel level, BlockPos van, double binnen) {
        Optional<Holder.Reference<Structure>> holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(PUT)));
        if (holder.isEmpty()) {
            return null;
        }
        var state = level.getChunkSource().getGeneratorState();
        record Kandidaat(long afstand, ChunkPos chunk, StructurePlacement placement) {
        }
        List<Kandidaat> kandidaten = new ArrayList<>();
        int px = van.getX() >> 4, pz = van.getZ() >> 4;
        for (StructurePlacement placement : state.getPlacementsForStructure(holder.get())) {
            if (!(placement instanceof RandomSpreadStructurePlacement spread)) {
                continue;
            }
            int cx = Math.floorDiv(px, spread.spacing()), cz = Math.floorDiv(pz, spread.spacing());
            for (int dx = -ZOEK_CELLEN; dx <= ZOEK_CELLEN; dx++) {
                for (int dz = -ZOEK_CELLEN; dz <= ZOEK_CELLEN; dz++) {
                    ChunkPos c = spread.getPotentialStructureChunk(state.getLevelSeed(), (cx + dx) * spread.spacing(), (cz + dz) * spread.spacing());
                    kandidaten.add(new Kandidaat((long) (c.x() - px) * (c.x() - px) + (long) (c.z() - pz) * (c.z() - pz), c, placement));
                }
            }
        }
        kandidaten.sort(Comparator.comparingLong(Kandidaat::afstand));
        Structure structuur = holder.get().value();
        Bezetting.Geplaatst geplaatst = Bezetting.Geplaatst.get(level);
        for (Kandidaat k : kandidaten) {
            if (k.afstand() * 256.0 > binnen + 4 * 256.0 * 16) {
                break;   // (everything from here on is further away than what was found already)
            }
            if (level.structureManager().checkStructurePresence(k.chunk(), structuur, k.placement(), false) == StructureCheckResult.START_NOT_PRESENT) {
                continue;
            }
            ChunkAccess chunk = level.getChunk(k.chunk().x(), k.chunk().z(), ChunkStatus.STRUCTURE_STARTS);
            StructureStart start = chunk.getStartForStructure(structuur);
            if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
                continue;
            }
            Identifier template = Kopieen.template(start.getPieces().get(0));
            if (template == null || !template.getPath().contains(PUT_STUK)) {
                continue;   // (a small pit: no Grillguh, no Guhdalf)
            }
            if (geplaatst.gehad(KAMP, start) && geplaatst.plek(KAMP, start).isEmpty()) {
                continue;   // (no room for the camp there: Guhdalf is not at this pit)
            }
            return start.getPieces().get(0).getBoundingBox().getCenter();
        }
        return null;
    }

    static void vergeet(UUID speler) {
        GEZOCHT.remove(speler);
    }

    static void wisAlles() {
        GEZOCHT.clear();
    }

    private Gouw() {
    }
}
