package nl.juiced.guhs.feature.ringvlucht;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ringh1.Gouw;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.4.2 (rookguh): the Rookguh-vlucht. A click on a Reis-Rookguh ({@link #klik}) shows the places this player may fly to
 * ({@link #bestemmingen}: every stop their story REACHED, the next one on top, never one behind a closed sluier); the choice
 * ({@link #kies}) starts the flight ({@link #vlieg}: the scene of {@link VluchtScenes}, Sam-guh along when he walks with them)
 * and at its end ({@link #land}) the player stands about 25 blocks in front of the destination's entrance, on solid ground
 * they can walk from to the entrance ({@link Landing}), in the Guhmensie or the Guhbarbecuether. Nothing is lost, nothing
 * hurts: for a moment after landing no damage at all ({@link #netGeland}).
 * <p>
 * Where a destination IS: the copy this player was at ({@link #onthoudRond}: remembered in their saved data as dimension +
 * start chunk while they are near a copy whose stop they reached), else the copy the structure's placement knows (every
 * chapter structure exists once per world: {@link #zoekStart}, also for players who finished before 1.4.2 and copies
 * generated long ago). The Gouw is the player's OWN Guhdalf (chapter 1's saved spot), else the Knabbelgouw. The chunks
 * around the entrance are asked for in the background while the scene plays ({@link #warm}) and loaded before the spot is
 * chosen.
 */
public final class Vlucht {
    /** Player saved data: the copy of guhs:&lt;structuur&gt; this player was at: "dimension|start chunk". */
    public static final String KOPIE = "guhs_ringvlucht_kopie_";
    /** Chapter 1's saved spot of the player's own Guhdalf (feature/ringh1/Feest), read by name. */
    private static final String GOUW_PLEK = "guhs_ringh1_thuis", GOUW_DIM = "guhs_ringh1_thuis_dim";
    /** After landing nothing hurts the player for this many ticks. */
    public static final int GELAND_TICKS = 200;
    /** A player must be this close to the Rookguh to fly with it. */
    public static final double REIK = 8;
    /** Two stops of the same structure whose spots are closer than this: the one is where the other's Rookguh stands. */
    private static final int NAAST = 32;
    /** How far a placement search looks for a copy (blocks), and how many possible copies it asks at most. */
    private static final int ZOEK_STRAAL = 1200, ZOEK_MAX = 64;

    /** (tests: the test server has none of our dimensions) every dimension of the story is this level. */
    @Nullable
    public static ServerLevel testLevel;
    /** (tests) where a stop is, instead of a real copy. */
    public static final Map<String, Doelplek> TEST_DOEL = new ConcurrentHashMap<>();

    /** A destination in the world: its level, its entrances (world positions), the box of the structure, how far out to land. */
    public record Doelplek(ServerLevel level, List<Landing.Ingang> ingangen, @Nullable BoundingBox doos, int afstand) {
        /**
         * (tests, dev) a copy of a stop as if its template stood with its corner (template 0, 0, 0) here, turned like this; the
         * box is that of the template's {@code grootte}.
         */
        public static Doelplek vanHoek(ServerLevel level, Haltes.Halte h, BlockPos hoek, Rotation draai, BlockPos grootte) {
            List<Landing.Ingang> ingangen = new ArrayList<>();
            for (Haltes.Ingang i : h.ingangen()) {
                BlockPos plek = hoek.offset(StructureTemplate.transform(i.plek(), Mirror.NONE, draai, BlockPos.ZERO));
                BlockPos uit = StructureTemplate.transform(new BlockPos(i.dx(), 0, i.dz()), Mirror.NONE, draai, BlockPos.ZERO);
                ingangen.add(new Landing.Ingang(plek, uit.getX(), uit.getZ()));
            }
            BlockPos ver = hoek.offset(StructureTemplate.transform(grootte.offset(-1, -1, -1), Mirror.NONE, draai, BlockPos.ZERO));
            return new Doelplek(level, List.copyOf(ingangen), BoundingBox.fromCorners(hoek, ver), h.afstand());
        }
    }

    private record Reis(Haltes.Halte halte, Doelplek doel) {
    }

    /** (not saved) the flight this player is on: chosen, the scene plays. */
    private static final Map<UUID, Reis> REIZEN = new ConcurrentHashMap<>();
    /** (not saved) when this player was put down (server tick). */
    private static final Map<UUID, Integer> GELAND = new ConcurrentHashMap<>();

    // =====================================================================================================================
    // where to
    // =====================================================================================================================

    /**
     * The places this player may fly to from the Rookguh of stop {@code hier} (null: from anywhere): every stop their story
     * reached ({@link Haltes#bereikt}), in story order, the next one of the story first; not the spot where this Rookguh
     * stands itself.
     */
    public static List<Haltes.Halte> bestemmingen(ServerPlayer p, @Nullable Haltes.Halte hier) {
        List<Haltes.Halte> uit = new ArrayList<>();
        Haltes.Halte volgende = volgende(p);
        for (Haltes.Halte h : Haltes.ALLE) {
            if (Haltes.bereikt(p, h) && !naast(hier, h)) {
                if (h == volgende) {
                    uit.add(0, h);
                } else {
                    uit.add(h);
                }
            }
        }
        return uit;
    }

    /** The stop of the chapter this player is busy with (null: the story did not begin or is done). */
    @Nullable
    public static Haltes.Halte volgende(ServerPlayer p) {
        int n = Ring.hoofdstuk(p);
        if (n < 1 || n > 6) {
            return null;
        }
        for (Haltes.Halte h : Haltes.ALLE) {
            if (h.hoofdstuk() == n && !h.id().equals(Haltes.AANLEG)) {
                return h;
            }
        }
        return null;
    }

    /** Is stop {@code h} where the Rookguh of stop {@code hier} stands (flying there would be no flight)? */
    static boolean naast(@Nullable Haltes.Halte hier, Haltes.Halte h) {
        if (hier == null) {
            return false;
        }
        if (hier.rookguh() == null) {
            return hier == h;
        }
        if (!hier.structuur().equals(h.structuur())) {
            return false;
        }
        for (Haltes.Ingang i : h.ingangen()) {
            if (i.plek().distManhattan(hier.rookguh()) < NAAST) {
                return true;
            }
        }
        return h.ingangen().isEmpty() && hier == h;
    }

    // =====================================================================================================================
    // a click, a choice
    // =====================================================================================================================

    /** A click on a Reis-Rookguh: the choice of where to go (the screen), or why not. */
    public static void klik(ServerPlayer p, VluchtRookguhEntity rookguh) {
        Haltes.Halte hier = rookguh.halte();
        if (hier == null || !(Haltes.klaar(p, hier) || Sluiers.passeert(p))) {
            return;                                           // (it is not there for this player)
        }
        String nee = waaromNiet(p);
        if (nee != null) {
            p.sendOverlayMessage(Component.translatable(nee).withStyle(ChatFormatting.GRAY));
            return;
        }
        List<Haltes.Halte> lijst = bestemmingen(p, hier);
        if (lijst.isEmpty()) {
            GuhQuests.say(p, rookguh, "quest.guhs.ringvlucht.niks");
            return;
        }
        Haltes.Halte volgende = volgende(p);
        CompoundTag data = new CompoundTag();
        ListTag haltes = new ListTag();
        for (Haltes.Halte h : lijst) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", h.id());
            t.putString("Naam", h.naamKey());
            t.putString("Wereld", h.dimensie() == BarbecuetherFeature.BARBECUETHER ? "gui.guhs.ringvlucht.wereld.barbecuether" : "gui.guhs.ringvlucht.wereld.guhmensie");
            t.putBoolean("Volgende", h == volgende);
            haltes.add(t);
        }
        data.put("Haltes", haltes);
        rookguh.playSound(nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), 0.8f, rookguh.getVoicePitch());
        ModNetworking.sendTo(p, new VluchtPayloads.Open(rookguh.getId(), data));
    }

    /** Why this player can't fly right now (a lang key), or null. */
    @Nullable
    static String waaromNiet(ServerPlayer p) {
        if (Minigames.playing(p) != null) {
            return "quest.guhs.ringvlucht.spel";
        }
        if (!Duwtje.mag(p) || Cutscenes.bezig(p) || REIZEN.containsKey(p.getUUID())) {
            return "quest.guhs.ringvlucht.bezig";
        }
        return null;
    }

    /**
     * The player picked a place on the Rookguh's screen. Everything is checked again here (the screen is only a client's
     * word): the Rookguh is near and there for them, the place is one they reached. True when the flight starts.
     */
    public static boolean kies(ServerPlayer p, VluchtRookguhEntity rookguh, String halteId) {
        Haltes.Halte hier = rookguh.halte();
        Haltes.Halte naar = Haltes.van(halteId);
        if (!rookguh.isAlive() || rookguh.level() != p.level() || rookguh.distanceTo(p) > REIK || hier == null || naar == null
                || !(Haltes.klaar(p, hier) || Sluiers.passeert(p)) || !bestemmingen(p, hier).contains(naar)) {
            return false;
        }
        return vlieg(p, naar, rookguh);
    }

    /**
     * Fly this player to this stop: where it is ({@link #doelplek}), the grill portal's rule when the flight crosses into the
     * Guhbarbecuether, the chunks asked for, the scene; {@link #land} when it ends. True when the scene started. Callers
     * check that the stop was reached (dev commands may skip that).
     */
    public static boolean vlieg(ServerPlayer p, Haltes.Halte naar, @Nullable Entity spreker) {
        String nee = waaromNiet(p);
        if (nee != null) {
            p.sendOverlayMessage(Component.translatable(nee).withStyle(ChatFormatting.GRAY));
            return false;
        }
        Doelplek doel = doelplek(p, naar);
        if (doel == null) {
            zeg(p, spreker, "quest.guhs.ringvlucht.weg_kwijt");
            return false;
        }
        if (grensDicht(p, p.level().dimension(), doel.level().dimension())) {
            zeg(p, spreker, "quest.guhs.ringvlucht.portaal");
            return false;
        }
        warm(doel);
        Cutscene scene = samMee(p) ? VluchtScenes.MET_SAM : VluchtScenes.ZONDER_SAM;
        BlockPos anker = p.blockPosition();
        REIZEN.put(p.getUUID(), new Reis(naar, doel));
        if (!Cutscenes.speel(p, scene, anker, VluchtScenes.openste(p.level(), anker), Vlucht::land)) {
            REIZEN.remove(p.getUUID());
            return false;
        }
        Sam.wacht(p, VluchtScenes.DUUR + 40);
        return true;
    }

    /** Into the Guhbarbecuether from elsewhere: only for whom the grill portal is open ({@code Ring.magDoorPortaal}). The way back is free. */
    public static boolean grensDicht(ServerPlayer p, ResourceKey<Level> van, ResourceKey<Level> naar) {
        return naar == BarbecuetherFeature.BARBECUETHER && van != BarbecuetherFeature.BARBECUETHER && !Ring.magDoorPortaal(p);
    }

    private static void zeg(ServerPlayer p, @Nullable Entity spreker, String key) {
        if (spreker != null) {
            GuhQuests.say(p, spreker, key);
        } else {
            p.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Does Sam-guh fly along (he walks with the player on the trip, or he is their own and stands near them)? */
    static boolean samMee(ServerPlayer p) {
        return Sam.looptMee(p) || eigenSam(p) != null;
    }

    /** The player's own tamed Sam-guh near them, not told to stay (he flies along), or null. */
    @Nullable
    static GuhEntity eigenSam(ServerPlayer p) {
        for (GuhEntity g : p.level().getEntitiesOfClass(GuhEntity.class, new AABB(p.blockPosition()).inflate(16),
                g -> g.isAlive() && g.getVariant() == GuhVariant.SAM_GUH && g.isTame() && g.isOwnedBy(p) && !g.isOrderedToSit() && !g.isPassenger())) {
            return g;
        }
        return null;
    }

    // =====================================================================================================================
    // landing
    // =====================================================================================================================

    /** (the daarna of the scene) put the player down at the destination. Nobody is ever put down without ground. */
    static void land(ServerPlayer p) {
        Reis reis = REIZEN.remove(p.getUUID());
        if (reis == null) {
            return;
        }
        Doelplek doel = reis.doel();
        ServerLevel level = doel.level();
        BlockPos plek = Landing.zoek(level, doel.ingangen(), doel.afstand(), doel.doos(), c -> magLanden(p, level, c));
        if (plek == null) {
            p.sendSystemMessage(Component.translatable("quest.guhs.ringvlucht.niet_landen").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        Landing.Ingang ingang = doel.ingangen().isEmpty() ? null : doel.ingangen().get(0);
        float yaw = p.getYRot();
        if (ingang != null && !ingang.plek().equals(plek)) {
            double dx = ingang.plek().getX() - plek.getX(), dz = ingang.plek().getZ() - plek.getZ();
            yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        }
        GuhEntity sam = eigenSam(p);
        ResourceKey<Level> van = p.level().dimension();
        if (p.isPassenger()) {
            p.stopRiding();
        }
        Vec3 daar = Vec3.atBottomCenterOf(plek);
        Duwtje.terug(p, level.dimension(), daar, yaw);
        GELAND.put(p.getUUID(), tick(p));
        p.fallDistance = 0;
        p.clearFire();
        if (sam != null) {
            sam.fallDistance = 0;
            sam.teleportTo(level, daar.x + 0.8, daar.y, daar.z + 0.8, Set.of(), yaw, 0f, false);
        }
        if (Sam.looptMee(p)) {
            Sam.kom(p);
        }
        if (Ring.opReis(p)) {
            Ring.rustpunt(p, daar, yaw);                       // (caught nearby: back here, not where they flew from)
        }
        GuhAdvancements.grant(p, "ringvlucht_gevlogen");
        if (van != level.dimension()) {
            GuhAdvancements.grant(p, "ringvlucht_grens");
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.ringvlucht.geland", Component.translatable(reis.halte().naamKey())).withStyle(ChatFormatting.GOLD));
    }

    /** May the Rookguh put this player down in this cell of that level: never behind a sluier that is still closed for them. */
    static boolean magLanden(ServerPlayer p, ServerLevel level, BlockPos c) {
        Sluiers.Zone zone = Sluiers.zone(level, c);
        return zone == null || Sluiers.open(p, zone.structuur()) || Sluiers.passeert(p);
    }

    /** Was this player put down a moment ago (nothing hurts them then)? */
    public static boolean netGeland(ServerPlayer p) {
        Integer t = GELAND.get(p.getUUID());
        if (t == null) {
            return false;
        }
        int sinds = tick(p) - t;
        if (sinds < 0 || sinds >= GELAND_TICKS) {
            GELAND.remove(p.getUUID());
            return false;
        }
        return true;
    }

    private static int tick(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        return server == null ? 0 : server.getTickCount();
    }

    /** Is this player on a flight right now (chosen, the scene plays)? */
    public static boolean onderweg(ServerPlayer p) {
        return REIZEN.containsKey(p.getUUID());
    }

    /** (logout) nothing of a flight is kept: whoever logs out in the air simply stays where they took off. */
    static void vergeet(UUID speler) {
        REIZEN.remove(speler);
        GELAND.remove(speler);
    }

    /** Asks for the chunks around the destination's entrances in the background (nothing waits for them). */
    static void warm(Doelplek doel) {
        if (testLevel != null) {
            return;
        }
        for (Landing.Ingang i : doel.ingangen()) {
            doel.level().getChunkSource().addTicketWithRadius(RingVluchtFeature.TICKET.get(), ChunkPos.containing(i.plek()), 2);
        }
    }

    // =====================================================================================================================
    // where a destination is
    // =====================================================================================================================

    /** The level of a dimension of the story (tests: the test level). */
    @Nullable
    static ServerLevel level(MinecraftServer server, ResourceKey<Level> dim) {
        return testLevel != null ? testLevel : server.getLevel(dim);
    }

    /** Where this stop is for this player (null: not known yet, or no such dimension). Loads what it has to read. */
    @Nullable
    public static Doelplek doelplek(ServerPlayer p, Haltes.Halte h) {
        Doelplek test = TEST_DOEL.get(h.id());
        if (test != null) {
            return test;
        }
        MinecraftServer server = p.level().getServer();
        ServerLevel level = server == null ? null : level(server, h.dimensie());
        if (level == null) {
            return null;
        }
        if (h.id().equals(Haltes.GOUW)) {
            return gouw(p, level, h);
        }
        StructureStart start = kopie(p, level, h.structuur());
        return start == null ? null : vanStart(level, h, start);
    }

    /** A stop of a copy in the world: its entrances and box from the copy's start (Kopieen: jigsaw pieces and burcht tiles alike). */
    @Nullable
    static Doelplek vanStart(ServerLevel level, Haltes.Halte h, StructureStart start) {
        Rotation draai = Kopieen.draai(start, null);
        List<Landing.Ingang> ingangen = new ArrayList<>();
        for (Haltes.Ingang i : h.ingangen()) {
            BlockPos plek = Kopieen.wereld(start, null, i.plek());
            if (plek == null) {
                return null;
            }
            BlockPos uit = StructureTemplate.transform(new BlockPos(i.dx(), 0, i.dz()), Mirror.NONE, draai, BlockPos.ZERO);
            ingangen.add(new Landing.Ingang(plek, uit.getX(), uit.getZ()));
        }
        Optional<BoundingBox> doos = BoundingBox.encapsulatingBoxes(start.getPieces().stream().map(StructurePiece::getBoundingBox).toList());
        return new Doelplek(level, List.copyOf(ingangen), doos.orElse(null), h.afstand());
    }

    /**
     * The Gouw: in front of this player's OWN Guhdalf (where their chapter 1 played: a Knabbelgouw or the camp at an old big
     * pit), else of the Guhdalf of the Knabbelgouw the placement knows. "In front" is the open side of his camp.
     */
    @Nullable
    static Doelplek gouw(ServerPlayer p, ServerLevel level, Haltes.Halte h) {
        CompoundTag saved = GuhQuests.saved(p);
        BlockPos guhdalf = null;
        if (saved.contains(GOUW_PLEK) && level.dimension().identifier().toString().equals(saved.getStringOr(GOUW_DIM, ""))) {
            guhdalf = BlockPos.of(saved.getLongOr(GOUW_PLEK, 0L));
        }
        if (guhdalf == null) {
            StructureStart start = kopie(p, level, Gouw.STRUCTUUR);
            guhdalf = start == null ? null : Kopieen.wereld(start, null, Gouw.GOUW_KAMP.offset(Gouw.GUHDALF));
        }
        if (guhdalf == null || guhdalf.getY() <= level.getMinY()) {
            return null;
        }
        Landing.laad(level, guhdalf, 8);
        Gouw.Plek kamp = Gouw.kampBij(level, guhdalf);
        BlockPos uit = StructureTemplate.transform(new BlockPos(0, 0, 1), Mirror.NONE, kamp.draai(), BlockPos.ZERO);
        return new Doelplek(level, List.of(new Landing.Ingang(guhdalf, uit.getX(), uit.getZ())), kamp.doos(), h.afstand());
    }

    /**
     * The copy of guhs:&lt;structuur&gt; for this player: the one they were at (saved), else the one the placement knows
     * (and from then on that one). Null: none known (yet).
     */
    @Nullable
    static StructureStart kopie(ServerPlayer p, ServerLevel level, String structuur) {
        Structure s = Kopieen.structuur(level, structuur);
        if (s == null) {
            return null;
        }
        String w = GuhQuests.saved(p).getStringOr(KOPIE + structuur, "");
        int streep = w.lastIndexOf('|');
        if (streep > 0 && w.substring(0, streep).equals(level.dimension().identifier().toString())) {
            try {
                long chunk = Long.parseLong(w.substring(streep + 1));
                StructureStart start = level.getChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk), ChunkStatus.STRUCTURE_STARTS).getStartForStructure(s);
                if (start != null && start.isValid() && !start.getPieces().isEmpty()) {
                    return start;
                }
            } catch (NumberFormatException e) {
                // (a broken entry: look it up again)
            }
        }
        BlockPos van = p.level() == level ? p.blockPosition() : level.getRespawnData().pos();
        StructureStart start = zoekStart(level, structuur, van);
        if (start != null) {
            onthoud(p, level, structuur, start);
        }
        return start;
    }

    /** Remember this copy as this player's one. */
    static void onthoud(ServerPlayer p, ServerLevel level, String structuur, StructureStart start) {
        GuhQuests.saved(p).putString(KOPIE + structuur, level.dimension().identifier() + "|" + start.getChunkPos().pack());
    }

    /**
     * (every few seconds for a player) the copies of the stops they reached that they stand at now are theirs: a flight
     * back goes to the very copy where they played.
     */
    static void onthoudRond(ServerPlayer p) {
        ServerLevel level = p.level();
        Set<String> gedaan = new HashSet<>();
        for (Haltes.Halte h : Haltes.ALLE) {
            if (h.id().equals(Haltes.GOUW) || !gedaan.add(h.structuur()) || (testLevel == null && h.dimensie() != level.dimension()) || !Haltes.bereikt(p, h)) {
                continue;
            }
            StructureStart start = Bezetting.start(level, h.structuur(), p.blockPosition());
            if (start != null) {
                onthoud(p, level, h.structuur(), start);
            }
        }
    }

    /**
     * The copy of this structure the placement knows nearest to this spot: every chapter structure is a guaranteed copy (a
     * ring position of its set), the Knabbelgouw also has a random spread. At most {@link #ZOEK_MAX} possible spots are asked.
     */
    @Nullable
    static StructureStart zoekStart(ServerLevel level, String structuur, BlockPos van) {
        Optional<Holder.Reference<Structure>> holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                .get(ResourceKey.create(Registries.STRUCTURE, Guhs.id(structuur)));
        if (holder.isEmpty()) {
            return null;
        }
        var state = level.getChunkSource().getGeneratorState();
        List<Object[]> kandidaten = new ArrayList<>();
        int px = van.getX() >> 4, pz = van.getZ() >> 4;
        for (StructurePlacement placement : state.getPlacementsForStructure(holder.get())) {
            if (placement instanceof ConcentricRingsStructurePlacement rings) {
                List<ChunkPos> plekken = state.getRingPositionsFor(rings);
                if (plekken != null) {
                    for (ChunkPos c : plekken) {
                        kandidaten.add(new Object[]{(long) (c.x() - px) * (c.x() - px) + (long) (c.z() - pz) * (c.z() - pz), c, placement});
                    }
                }
            } else if (placement instanceof RandomSpreadStructurePlacement spread) {
                int straal = ZOEK_STRAAL >> 4, cellen = straal / spread.spacing() + 1;
                int cx = Math.floorDiv(px, spread.spacing()), cz = Math.floorDiv(pz, spread.spacing());
                for (int dx = -cellen; dx <= cellen; dx++) {
                    for (int dz = -cellen; dz <= cellen; dz++) {
                        ChunkPos c = spread.getPotentialStructureChunk(state.getLevelSeed(), (cx + dx) * spread.spacing(), (cz + dz) * spread.spacing());
                        long d = (long) (c.x() - px) * (c.x() - px) + (long) (c.z() - pz) * (c.z() - pz);
                        if (d <= (long) straal * straal) {
                            kandidaten.add(new Object[]{d, c, placement});
                        }
                    }
                }
            }
        }
        kandidaten.sort(java.util.Comparator.comparingLong(o -> (Long) o[0]));
        int gevraagd = 0;
        for (Object[] k : kandidaten) {
            if (gevraagd++ >= ZOEK_MAX) {
                break;
            }
            ChunkPos c = (ChunkPos) k[1];
            if (level.structureManager().checkStructurePresence(c, holder.get().value(), (StructurePlacement) k[2], false) == StructureCheckResult.START_NOT_PRESENT) {
                continue;
            }
            ChunkAccess chunk = level.getChunk(c.x(), c.z(), ChunkStatus.STRUCTURE_STARTS);
            StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), holder.get().value(), chunk);
            if (start != null && start.isValid() && !start.getPieces().isEmpty()) {
                return start;
            }
        }
        return null;
    }

    private Vlucht() {
    }
}
