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
 *       natural ground, so never over something a player built. When none of those is free {@link #zoekRuimer} looks
 *       further (a fourth ring, and the camp turned all four ways), then puts the camp on the land as it lies beside a pit
 *       in a hollow or on a slope ({@link #zoekOpHetLand}: a little earthwork, never where anything was built), and when
 *       even that finds nothing the server log says so
 *       once and an operator puts the camp down by hand ({@code /guhs ringh1 zetkamp}, {@link #zetKampBijPut}): without a
 *       camp nobody can start the story at that pit. Camps that Bezetting did not place itself are kept in
 *       {@link Kampen}.</li>
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
        if (hoek.isPresent()) {
            return new Plek(hoek.get(), Kopieen.draai(start, PUT_STUK));
        }
        return Kampen.get(level).plek(Bezetting.tag(KAMP, start));   // (the wider search, or an operator)
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
            if (kamp == null && Bezetting.Geplaatst.get(level).gehad(KAMP, start)) {
                kamp = zoekRuimer(level, start);   // (Bezetting found no free spot on its strip: look further, once)
            }
            if (kamp != null) {
                Bescherming.zetDoos(level, KAMP, kamp.doos());
            }
        }
    }

    // =====================================================================================================================
    // a pit without a free spot on Bezetting's strip (PHASE3 R03): the wider search, the operator's camp
    // =====================================================================================================================

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    /** The rings of the wider search (blocks between the pit and the camp), from near to far. */
    private static final int[] RUIMER = {3, 7, 12, 17};
    /** (not saved) the pits without a camp that this server run has told the log about. */
    private static final java.util.Set<String> GEMELD = ConcurrentHashMap.newKeySet();

    /**
     * SavedData {@code guhs:ringh1_kampen} (per dimension): the camps of old big pits that {@link Bezetting} did not place
     * itself, by the tag of the pit ({@code Bezetting.tag(KAMP, start)}): found by the wider search or put down by an
     * operator, and the pits where even the wider search found nothing ("geen": it is not tried again).
     */
    public static class Kampen extends net.minecraft.world.level.saveddata.SavedData {
        public static final net.minecraft.world.level.saveddata.SavedDataType<Kampen> TYPE = nl.juiced.guhs.storage.GuhSavedData.tagType("ringh1_kampen", Kampen::new,
                Kampen::load, Kampen::save);
        private final Map<String, Plek> kampen = new java.util.HashMap<>();
        private final java.util.Set<String> geen = new java.util.HashSet<>();

        public static Kampen get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        @Nullable
        public Plek plek(String put) {
            return kampen.get(put);
        }

        /** Did the wider search look at this pit and find nothing? */
        public boolean geen(String put) {
            return geen.contains(put);
        }

        void zet(String put, Plek kamp) {
            kampen.put(put, kamp);
            geen.remove(put);
            setDirty();
        }

        void zetGeen(String put) {
            if (geen.add(put)) {
                setDirty();
            }
        }

        /** Forgets what is known about this pit (true: there was something). */
        public boolean vergeet(String put) {
            boolean was = kampen.remove(put) != null | geen.remove(put);
            if (was) {
                setDirty();
            }
            return was;
        }

        private net.minecraft.nbt.CompoundTag save() {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.CompoundTag k = new net.minecraft.nbt.CompoundTag();
            kampen.forEach((put, kamp) -> {
                net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
                t.putLong("Hoek", kamp.hoek().asLong());
                t.putInt("Draai", kamp.draai().ordinal());
                k.put(put, t);
            });
            tag.put("Kampen", k);
            net.minecraft.nbt.CompoundTag g = new net.minecraft.nbt.CompoundTag();
            geen.forEach(put -> g.putBoolean(put, true));
            tag.put("Geen", g);
            return tag;
        }

        private static Kampen load(net.minecraft.nbt.CompoundTag tag) {
            Kampen uit = new Kampen();
            net.minecraft.nbt.CompoundTag k = tag.getCompoundOrEmpty("Kampen");
            for (String put : k.keySet()) {
                net.minecraft.nbt.CompoundTag t = k.getCompoundOrEmpty(put);
                uit.kampen.put(put, new Plek(BlockPos.of(t.getLongOr("Hoek", 0L)), Rotation.values()[Math.floorMod(t.getIntOr("Draai", 0), 4)]));
            }
            uit.geen.addAll(tag.getCompoundOrEmpty("Geen").keySet());
            return uit;
        }
    }

    /** Where the camp could go around a big pit when Bezetting's own 48 spots are taken: the corner and the turn, in the world. */
    static List<Plek> ruimereKeuzes(StructureStart start) {
        List<Plek> uit = new ArrayList<>();
        Rotation put = Kopieen.draai(start, PUT_STUK);
        BlockPos midden = Kopieen.wereld(start, PUT_STUK, PUT_MIDDEN);
        if (midden == null) {
            return uit;
        }
        for (int ver : RUIMER) {
            for (Rotation r : new Rotation[]{Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90}) {
                if (r == Rotation.NONE && ver != RUIMER[RUIMER.length - 1]) {
                    continue;   // (the camp turned as the pit is, on the three near rings: Bezetting's own spots)
                }
                boolean dwars = r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90;
                int bx = dwars ? KAMP_MAAT.getZ() : KAMP_MAAT.getX(), bz = dwars ? KAMP_MAAT.getX() : KAMP_MAAT.getZ();
                int noord = -bz - ver + 1, zuid = PUT_MAAT + ver - 1, west = -bx - ver + 1, oost = PUT_MAAT + ver - 1;
                int[] langsX = {(PUT_MAAT - bx) / 2, 0, PUT_MAAT - bx}, langsZ = {(PUT_MAAT - bz) / 2, 0, PUT_MAAT - bz};
                List<int[]> dozen = new ArrayList<>();   // {x0, z0}: the least corner of the camp's box, in the pit's own coordinates
                for (int x : langsX) {
                    dozen.add(new int[]{x, noord});
                }
                for (int z : langsZ) {
                    dozen.add(new int[]{oost, z});
                    dozen.add(new int[]{west, z});
                }
                for (int x : langsX) {
                    dozen.add(new int[]{x, zuid});
                }
                dozen.add(new int[]{west, noord});
                dozen.add(new int[]{oost, noord});
                dozen.add(new int[]{west, zuid});
                dozen.add(new int[]{oost, zuid});
                for (int[] d : dozen) {
                    // (the template's corner is the camp's (0, 0, 0): which corner of its box that is depends on the turn)
                    int hx = r == Rotation.CLOCKWISE_90 || r == Rotation.CLOCKWISE_180 ? d[0] + bx - 1 : d[0];
                    int hz = r == Rotation.CLOCKWISE_180 || r == Rotation.COUNTERCLOCKWISE_90 ? d[1] + bz - 1 : d[1];
                    BlockPos hoek = Kopieen.wereld(start, PUT_STUK, new BlockPos(hx, G, hz));
                    if (hoek == null) {
                        continue;
                    }
                    Plek kamp = new Plek(hoek, put.getRotated(r));
                    if (binnenBereik(kamp, midden)) {
                        uit.add(kamp);
                    }
                }
            }
        }
        return uit;
    }

    /** Bezetting looks for Guhdalf and Sam-guh within {@link #KAMP_ZOEK} blocks (per axis) of the pit's middle: are both in there? */
    private static boolean binnenBereik(Plek kamp, BlockPos midden) {
        for (BlockPos lokaal : new BlockPos[]{GUHDALF, SAM}) {
            BlockPos w = kamp.wereld(lokaal);
            if (Math.abs(w.getX() - midden.getX()) > KAMP_ZOEK - 2 || Math.abs(w.getZ() - midden.getZ()) > KAMP_ZOEK - 2) {
                return false;
            }
        }
        return true;
    }

    /**
     * The wider search for the camp of an old big pit where Bezetting's strip had no free spot: the camp turned all four ways
     * on the same three rings, and a fourth ring further out, each also one and two blocks higher and lower, with the very
     * rule of Bezetting for "free" (air and plants; natural ground only in its bottom layer; ground under at least half of it:
     * never over something a player built). Done once per pit: the camp is placed and kept in {@link Kampen}, or the pit is
     * marked "geen" and the log says what an operator can do. Null: no camp (yet: not everything around the pit is loaded).
     */
    @Nullable
    static Plek zoekRuimer(ServerLevel level, StructureStart start) {
        return zoekRuimer(level, start, null);
    }

    /** {@link #zoekRuimer(ServerLevel, StructureStart)} over these spots (null: {@link #ruimereKeuzes}; the tests give their own). */
    @Nullable
    static Plek zoekRuimer(ServerLevel level, StructureStart start, @Nullable List<Plek> keuzes) {
        String tag = Bezetting.tag(KAMP, start);
        Kampen data = Kampen.get(level);
        Plek had = data.plek(tag);
        if (had != null) {
            return had;
        }
        if (data.geen(tag)) {
            meldGeenKamp(level, start, tag);
            return null;
        }
        Optional<StructureTemplate> template = level.getStructureManager().get(KAMP_TEMPLATE);
        if (template.isEmpty() || Kopieen.wereld(start, PUT_STUK, BlockPos.ZERO) == null) {
            return null;
        }
        for (Plek keuze : keuzes != null ? keuzes : ruimereKeuzes(start)) {
            net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings zo =
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setRotation(keuze.draai()).setMirror(Mirror.NONE);
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                BlockPos hoek = keuze.hoek().above(dy);
                Boolean past = past(level, template.get().getBoundingBox(zo, hoek));
                if (past == null) {
                    return null;   // (not loaded: later, so the spots keep their order)
                }
                if (past) {
                    template.get().placeInWorld(level, hoek, hoek, zo, level.getRandom(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                    Plek kamp = new Plek(hoek, keuze.draai());
                    data.zet(tag, kamp);
                    LOGGER.info("Guhs: Guhdalf's camp stands at {} (turned {}) next to the big barbecueput of chunk {}: found by the wider search", hoek.toShortString(),
                            kamp.draai(), start.getChunkPos());
                    return kamp;
                }
            }
        }
        if (keuzes == null) {
            // the last round: on the land as it lies (a pit in a hollow or on a slope has no spot at its own height)
            Optional<Plek> land = zoekOpHetLand(level, start, template.get(), null);
            if (land == null) {
                return null;   // (not loaded: later)
            }
            if (land.isPresent()) {
                data.zet(tag, land.get());
                return land.get();
            }
        }
        data.zetGeen(tag);
        meldGeenKamp(level, start, tag);
        return null;
    }

    // =====================================================================================================================
    // the last round of the wider search: the camp on the land as it lies
    // =====================================================================================================================

    /**
     * How deep the land under the camp may be filled up, and how many blocks of earth (filled up + dug off) a camp may cost:
     * one per column of its plate on average.
     */
    static final int MAX_VULLEN = 3, MAX_GRONDWERK = 165;
    /** A spot that costs no more earth than this is neat enough: no need to look further from the pit for a better one. */
    static final int NET_GRONDWERK = 40;
    /** The land is read from this far above the pit's ground layer down to this far below it; the plate lies at most this high. */
    private static final int LAND_BOVEN = 30, LAND_ONDER = 14, LAND_HOOGST = 18;

    /** What grows on the land and may make room for the camp: air, grass and flowers, the loose leaves of a tree. Never a fluid. */
    private static boolean begroeiing(net.minecraft.world.level.block.state.BlockState state) {
        if (Bezetting.leeg(state)) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.is(net.minecraft.tags.BlockTags.LEAVES)) {
            // (a tree that grew by itself; leaves somebody placed stay)
            return state.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT) && !state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT);
        }
        return state.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock;
    }

    /** The land itself: natural ground, or a knabbel block that lies in it. Never a fluid. */
    private static boolean landgrond(net.minecraft.world.level.block.state.BlockState state) {
        return state.getFluidState().isEmpty() && (state.is(Bezetting.NATUURLIJK) || state.is(nl.juiced.guhs.registry.ModBlocks.BLOCK_OF_KAASKNABBELS.get()));
    }

    /** A spot the camp fits on with a little earthwork: the plate's height, what it costs, and the land under every column. */
    private record OpLand(Plek kamp, int grondwerk, BoundingBox voet, int[] top, net.minecraft.world.level.block.state.BlockState[] grond) {
    }

    /**
     * Can the camp stand on the land of this spot (only its x and z count)? Every column of its footprint must be natural
     * land with nothing on it but what grows there: a trunk, water, a hole or anything a player may have built refuses the
     * spot. The plate comes level with the highest land of the footprint, or one block under it (then that one layer of
     * real land is dug off, never a knabbel block); lower land is filled up, at most {@link #MAX_VULLEN} deep. Null: no.
     */
    @Nullable
    private static OpLand opLand(ServerLevel level, StructureTemplate template, Plek keuze, int putGrond) {
        net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings zo =
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setRotation(keuze.draai()).setMirror(Mirror.NONE);
        BoundingBox voet = template.getBoundingBox(zo, keuze.hoek());
        int breed = voet.getXSpan(), diep = voet.getZSpan();
        int[] top = new int[breed * diep];
        net.minecraft.world.level.block.state.BlockState[] grond = new net.minecraft.world.level.block.state.BlockState[breed * diep];
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int hoogst = Integer.MIN_VALUE, laagst = Integer.MAX_VALUE;
        int onder = Math.max(level.getMinY() + 1, putGrond - LAND_ONDER), boven = Math.min(level.getMaxY() - 1, putGrond + LAND_BOVEN);
        for (int i = 0; i < top.length; i++) {
            int x = voet.minX() + i % breed, z = voet.minZ() + i / breed;
            int y = boven;
            net.minecraft.world.level.block.state.BlockState state = null;
            for (; y >= onder; y--) {
                state = level.getBlockState(at.set(x, y, z));
                if (!begroeiing(state)) {
                    break;
                }
            }
            if (y < onder || !landgrond(state) || level.getBlockEntity(at) != null) {
                return null;   // (a hole, water, a trunk, something built)
            }
            top[i] = y;
            grond[i] = state;
            hoogst = Math.max(hoogst, y);
            laagst = Math.min(laagst, y);
        }
        if (hoogst > putGrond + LAND_HOOGST) {
            return null;   // (above this the land was not read all the way up to the camp's top)
        }
        int beste = Integer.MAX_VALUE, plaat = 0;
        for (int p = hoogst; p >= hoogst - 1; p--) {
            if (p - laagst > MAX_VULLEN) {
                continue;
            }
            int werk = 0;
            for (int i = 0; i < top.length && werk <= MAX_GRONDWERK; i++) {
                if (top[i] <= p) {
                    werk += p - top[i];
                } else if (grond[i].is(Bezetting.NATUURLIJK)
                        && Bezetting.vrij(level.getBlockState(at.set(voet.minX() + i % breed, p, voet.minZ() + i / breed))) && level.getBlockEntity(at) == null) {
                    werk++;   // (one layer of real land comes off; what lies under it is land too)
                } else {
                    werk = Integer.MAX_VALUE - 1;
                }
            }
            if (werk < beste) {
                beste = werk;
                plaat = p;
            }
        }
        if (beste > MAX_GRONDWERK) {
            return null;
        }
        BlockPos hoek = new BlockPos(keuze.hoek().getX(), plaat, keuze.hoek().getZ());
        return new OpLand(new Plek(hoek, keuze.draai()), beste, voet, top, grond);
    }

    /**
     * The last round of the wider search (the world check of 1.4.0: of six old big pits that nobody ever touched, two had no
     * spot for the camp at all, the one nearest to the world's middle among them: a pit lies in the land as worldgen dug it
     * in, so the land around it may be eight blocks higher, or slope). Every spot of Bezetting's strip and of the wider
     * rings is looked at again ON THE LAND AS IT LIES ({@link #opLand}), from near to far: the first one that is neat
     * ({@link #NET_GRONDWERK}) gets the camp, else the one that needs the least earthwork. What grows there is cleared, lower
     * land under the plate is filled up with the land's own block, one layer of higher land comes off. Empty: no spot;
     * null: not everything around the pit is loaded yet. {@code keuzes}: the spots (null: all of them; the tests give
     * their own).
     */
    @Nullable
    static Optional<Plek> zoekOpHetLand(ServerLevel level, StructureStart start, StructureTemplate template, @Nullable List<Plek> keuzes) {
        BlockPos grondlaag = Kopieen.wereld(start, PUT_STUK, new BlockPos(0, G, 0));
        if (grondlaag == null) {
            return Optional.empty();
        }
        if (keuzes == null) {
            keuzes = new ArrayList<>();
            Rotation put = Kopieen.draai(start, PUT_STUK);
            for (BlockPos keuze : KEUZES) {
                BlockPos hoek = Kopieen.wereld(start, PUT_STUK, keuze);
                if (hoek != null) {
                    keuzes.add(new Plek(hoek, put));
                }
            }
            keuzes.addAll(ruimereKeuzes(start));
        }
        net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings zo =
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setMirror(Mirror.NONE);
        for (Plek keuze : keuzes) {
            BoundingBox voet = template.getBoundingBox(zo.setRotation(keuze.draai()), keuze.hoek());
            for (int x = voet.minX() >> 4; x <= voet.maxX() >> 4; x++) {
                for (int z = voet.minZ() >> 4; z <= voet.maxZ() >> 4; z++) {
                    if (!level.hasChunk(x, z)) {
                        return null;
                    }
                }
            }
        }
        // (the spots come from near to far: the first one that is neat wins, else the one with the least earthwork)
        OpLand beste = null;
        for (Plek keuze : keuzes) {
            OpLand kan = opLand(level, template, keuze, grondlaag.getY());
            if (kan != null && (beste == null || kan.grondwerk() < beste.grondwerk())) {
                beste = kan;
                if (kan.grondwerk() <= NET_GRONDWERK) {
                    break;
                }
            }
        }
        if (beste == null) {
            return Optional.empty();
        }
        Plek kamp = beste.kamp();
        BoundingBox voet = beste.voet();
        int breed = voet.getXSpan(), plaat = kamp.hoek().getY();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        net.minecraft.world.level.block.state.BlockState lucht = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        for (int i = 0; i < beste.top().length; i++) {
            int x = voet.minX() + i % breed, z = voet.minZ() + i / breed;
            // lower land comes up to the plate (the plate's four open corners too), with the block the land has there
            // (never with knabbel blocks: those are worth digging the camp's foot away for)
            net.minecraft.world.level.block.state.BlockState vulling = beste.grond()[i].is(Bezetting.NATUURLIJK) ? beste.grond()[i]
                    : net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
            for (int y = beste.top()[i] + 1; y <= plaat; y++) {
                level.setBlock(at.set(x, y, z), vulling, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
            // and the camp's own room is cleared: what grew there, and the one layer of land above the plate
            for (int y = plaat + 1; y < plaat + KAMP_MAAT.getY(); y++) {
                if (!level.getBlockState(at.set(x, y, z)).isAir()) {
                    level.setBlock(at, lucht, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
            }
        }
        template.placeInWorld(level, kamp.hoek(), kamp.hoek(), zo.setRotation(kamp.draai()), level.getRandom(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        LOGGER.info("Guhs: Guhdalf's camp stands at {} (turned {}) next to the big barbecueput of chunk {}: on the land as it lies, {} blocks of earth moved",
                kamp.hoek().toShortString(), kamp.draai(), start.getChunkPos(), beste.grondwerk());
        return Optional.of(kamp);
    }

    /** Bezetting's own rule for a prop's box (its {@code past} is private): free, or not, or (null) not loaded. */
    @Nullable
    private static Boolean past(ServerLevel level, BoundingBox box) {
        if (box.minY() <= level.getMinY() || box.maxY() >= level.getMaxY()) {
            return false;
        }
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                if (!level.hasChunk(x, z)) {
                    return null;
                }
            }
        }
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int kolommen = 0, grond = 0;
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(at.set(x, y, z));
                    if (!(y == box.minY() ? Bezetting.vrij(state) : Bezetting.leeg(state)) || level.getBlockEntity(at) != null) {
                        return false;
                    }
                }
                kolommen++;
                net.minecraft.world.level.block.state.BlockState onder = level.getBlockState(at.set(x, box.minY() - 1, z));
                grond += onder.isSolidRender() || onder.is(Bezetting.NATUURLIJK) && !onder.canBeReplaced() ? 1 : 0;
            }
        }
        return grond * 2 >= kolommen;
    }

    /** One clear line per pit and server run: no room for the camp here, and what an operator can do about it. */
    private static void meldGeenKamp(ServerLevel level, StructureStart start, String tag) {
        if (GEMELD.add(level.dimension().identifier() + "|" + tag)) {
            BlockPos midden = start.getBoundingBox().getCenter();
            LOGGER.warn("Guhs: NO ROOM for Guhdalf's camp at the big barbecueput at {} ({}): the land around it is too steep or too wet, or players built all around it. Nobody can start the "
                    + "Knabbelring at THIS pit (the grill portal stays shut for who has no other pit or Knabbelgouw). An operator can put the camp down by "
                    + "hand: stand where it should be, look the way its open side should face, and run /guhs ringh1 zetkamp", midden.toShortString(),
                    level.dimension().identifier());
        }
    }

    /**
     * (the operator's command {@code /guhs ringh1 zetkamp}) Guhdalf's camp at the old big pit near this spot, exactly where
     * the operator wants it: its plate centred on the block under {@code voeten}, its open side facing {@code kijk}. The
     * operator decides, so nothing is asked about what stands there (it is replaced by the camp); the camp may not cut into
     * the pit itself and must be near enough for Bezetting to find Guhdalf and Sam-guh. It is kept ({@link Kampen}) and
     * protected like any other camp; Guhdalf and Sam-guh walk in by themselves within a few seconds. Returns null when the
     * camp stands, else a text (literal, for the operator) that says why not.
     */
    @Nullable
    static String zetKampBijPut(ServerLevel level, BlockPos voeten, net.minecraft.core.Direction kijk) {
        if (Bezetting.start(level, STRUCTUUR, voeten) != null) {
            return "this is a Knabbelgouw: it has its own camp on the feestwei";
        }
        StructureStart start = Bezetting.start(level, PUT, voeten);
        if (start == null || Kopieen.wereld(start, PUT_STUK, BlockPos.ZERO) == null) {
            return "no big barbecueput within " + Bezetting.BEREIK + " blocks of " + voeten.toShortString() + " (a small pit has no Grillguh and gets no camp)";
        }
        Bezetting.controleer(level, voeten);   // (let Bezetting try its own strip first: there is one camp per pit)
        Plek er = kampVan(level, start);
        if (er != null) {
            return "this pit has a camp already, at " + er.hoek().toShortString() + " (turned " + er.draai() + ")";
        }
        if (!Bezetting.Geplaatst.get(level).gehad(KAMP, start)) {
            return "the land around the pit is not all loaded yet: walk once around the pit and try again";
        }
        Optional<StructureTemplate> template = level.getStructureManager().get(KAMP_TEMPLATE);
        if (template.isEmpty()) {
            return "no template " + KAMP_TEMPLATE;
        }
        // (the camp's open side is south when it is not turned)
        Rotation draai = switch (kijk) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        BlockPos midden = new BlockPos(KAMP_MAAT.getX() / 2, 0, KAMP_MAAT.getZ() / 2);
        Plek kamp = new Plek(voeten.below().subtract(StructureTemplate.transform(midden, Mirror.NONE, draai, BlockPos.ZERO)), draai);
        BoundingBox doos = kamp.doos();
        for (net.minecraft.world.level.levelgen.structure.StructurePiece stuk : start.getPieces()) {
            if (stuk.getBoundingBox().intersects(doos)) {
                return "the camp would cut into the pit itself: stand a few blocks further from it";
            }
        }
        BlockPos put = Kopieen.wereld(start, PUT_STUK, PUT_MIDDEN);
        if (put == null || !binnenBereik(kamp, put)) {
            return "too far from the pit: Guhdalf has to stand within " + (KAMP_ZOEK - 2) + " blocks (per axis) of its middle";
        }
        net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings zo =
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setRotation(draai).setMirror(Mirror.NONE);
        template.get().placeInWorld(level, kamp.hoek(), kamp.hoek(), zo, level.getRandom(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        Kampen.get(level).zet(Bezetting.tag(KAMP, start), kamp);
        Bescherming.zetDoos(level, KAMP, doos);
        LOGGER.info("Guhs: an operator put Guhdalf's camp at {} (turned {}) next to the big barbecueput of chunk {}", kamp.hoek().toShortString(), draai,
                start.getChunkPos());
        return null;
    }

    /**
     * (the operator's command {@code /guhs ringh1 vergeetkamp}) forgets the camp of the pit here that the wider search or an
     * operator placed (not one of Bezetting's own strip), so it can be put down again: Guhdalf and Sam-guh of that camp go,
     * the blocks stay for the operator to clear. Returns what was done (a literal text).
     */
    static String vergeetKampBijPut(ServerLevel level, BlockPos bij) {
        StructureStart start = Bezetting.start(level, PUT, bij);
        if (start == null) {
            return "no barbecueput within " + Bezetting.BEREIK + " blocks";
        }
        String tag = Bezetting.tag(KAMP, start);
        Plek kamp = Kampen.get(level).plek(tag);
        if (!Kampen.get(level).vergeet(tag)) {
            return "nothing to forget: this pit has no camp of the wider search or of an operator";
        }
        GEMELD.remove(level.dimension().identifier() + "|" + tag);
        Bescherming.wisDozen(level, KAMP);   // (every camp's box comes back by itself; this one's does not)
        int weg = 0;
        if (kamp != null) {
            String guhdalf = Bezetting.tag(GUHDALF_KAMP, start), sam = Bezetting.tag(SAM_KAMP, start);
            for (Entity e : level.getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(kamp.wereld(GUHDALF)).inflate(KAMP_ZOEK), Entity::isAlive)) {
                String t = e.getPersistentData().getStringOr(Bezetting.TAG, "");
                if (t.equals(guhdalf) || t.equals(sam)) {
                    e.discard();
                    weg++;
                }
            }
        }
        return "forgotten" + (kamp == null ? "" : ": the camp at " + kamp.hoek().toShortString() + " (" + weg + " of its two inhabitants sent away; its blocks stay: "
                + "clear them yourself)") + ". The wider search looks again; /guhs ringh1 zetkamp puts a camp where you stand";
    }

    /**
     * (a chunk of the Guhmensie was loaded, {@link RingH1Events}) the camps of the big pits that reach into this chunk are
     * protected boxes from now on, whoever loaded it: boxes put down by code are not saved, and before this they only came
     * back when a player stood within 48 blocks of the pit (PHASE3 R21).
     */
    static void beschermBijChunk(ServerLevel level, ChunkPos chunk) {
        Structure put = Kopieen.structuur(level, PUT);
        if (put == null) {
            return;
        }
        // (a piece that reaches into this chunk lies within 8 blocks of its middle; the pit lies at the surface)
        int mx = chunk.getMiddleBlockX(), mz = chunk.getMiddleBlockZ();
        BlockPos bij = new BlockPos(mx, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, mx, mz), mz);
        for (StructureStart start : Kopieen.bij(level, put, bij, 24)) {
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
     * The nearest place in the Guhmensie where Guhdalf stands: the middle of a Knabbelgouw, or Guhdalf's camp at an old big
     * barbecueput (the pit's middle while its camp is not there yet; small pits and pits that got no camp don't count). Looked up at most once a minute per player (or after 96 blocks);
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

    /**
     * The nearest big barbecueput (a start of guhs:barbecueput whose piece is barbecueput_groot) nearer than {@code binnen}
     * (squared), in land that exists already (a new big pit is always a Knabbelgouw, which the caller looks for itself).
     */
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
        nl.juiced.guhs.world.NieuwTerrein terrein = nl.juiced.guhs.world.NieuwTerrein.van(level);
        for (Kandidaat k : kandidaten) {
            if (k.afstand() * 256.0 > binnen + 4 * 256.0 * 16) {
                break;   // (everything from here on is further away than what was found already)
            }
            if (!terrein.bestaat(k.chunk().x(), k.chunk().z())) {
                continue;   // (land that was never made has no old pit: nothing is generated for this question)
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
            Plek kamp = kampVan(level, start);
            if (geplaatst.gehad(KAMP, start) && kamp == null) {
                continue;   // (no room for the camp there: Guhdalf is not at this pit)
            }
            // (once the camp stands: Guhdalf himself. A camp on the land beside a sunken pit can lie some forty blocks from the
            // pit's middle, out of sight from the grill)
            return kamp != null ? kamp.wereld(GUHDALF) : start.getPieces().get(0).getBoundingBox().getCenter();
        }
        return null;
    }

    static void vergeet(UUID speler) {
        GEZOCHT.remove(speler);
    }

    static void wisAlles() {
        GEZOCHT.clear();
        GEMELD.clear();
    }

    private Gouw() {
    }
}
