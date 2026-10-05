package nl.juiced.guhs.feature.knuffeldal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.PleinSlot;

/**
 * The residents of a Knuffeldal town stay the town's residents, for every player, forever (1.2.7).
 * <ul>
 *   <li>A resident can't be tamed (KnuffeldalEvents.onTame), hurt, leashed or picked up: {@link #opJoin} makes every
 *       resident invulnerable when it comes into the world, so towns of existing worlds are repaired too.</li>
 *   <li>A resident that a player tamed before this fix stays that player's guh, but is no resident any more
 *       ({@link #vergeet}).</li>
 *   <li>A town that misses a resident (tamed away or lost before the fix) gets a new one at its home spot: while a
 *       player is in the town, {@link #controleer} looks (now and then) whether every resident of the town's templates
 *       ({@code b.bewoner(...)} in tools/features/knuffeldal_stadje.py) is still around, and re-creates the missing
 *       ones from the template ({@link #herstel}).</li>
 * </ul>
 */
public final class Bewoners {
    /** A resident that was re-created by {@link #herstel} (so a double one can be told apart). */
    public static final String HERBOREN = "guhs_knus_bewoner_herboren";
    /** A town is looked at once per this many ticks. */
    public static final int CHECK_TICKS = 20 * 30;
    /** Residents are looked for this far around the town's box (they walk to the plein's campfire and back). */
    public static final int ZOEK_RAND = 24;

    /** One resident of a template: its name, its spot in the template and its entity data. */
    public record Plek(String naam, Vec3 pos, CompoundTag nbt) {
    }

    private static final Map<Identifier, List<Plek>> TEMPLATES = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAATST = new HashMap<>();

    private Bewoners() {
    }

    /** A guh comes into the world: a wild resident is protected; a tamed one is no resident any more. */
    static void opJoin(GuhEntity guh) {
        if (!GuhHooks.isBewoner(guh)) {
            return;
        }
        if (guh.isTame()) {
            vergeet(guh);
            return;
        }
        guh.setInvulnerable(true);
        guh.setPersistenceRequired();
        if (guh.isLeashed()) {
            guh.dropLeash();
        }
    }

    /** This guh is no resident any more (a player tamed it before 1.2.7: it stays theirs, the town gets a new one). */
    public static void vergeet(GuhEntity guh) {
        CompoundTag data = guh.getPersistentData();
        data.remove(GuhHooks.BEWONER);
        data.remove(GuhHooks.THUIS);
        data.remove(GuhHooks.BEWONER_NAAM);
        data.remove(HERBOREN);
        guh.setInvulnerable(false);
    }

    /** Every 2 seconds for a player in a town: now and then the town's residents are counted (and the missing ones come back). */
    static void controleer(ServerPlayer player) {
        ServerLevel level = player.level();
        StructureStart start = PleinSlot.stadje(level, player.blockPosition());
        if (start == null) {
            return;
        }
        String key = level.dimension().identifier() + "/" + start.getChunkPos().pack();
        long now = level.getGameTime();
        Long last = LAATST.get(key);
        if (last != null && now - last < CHECK_TICKS && now >= last) {
            return;
        }
        LAATST.put(key, now);
        BoundingBox box = start.getBoundingBox();
        AABB zoek = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1).inflate(ZOEK_RAND);
        if (!geladen(level, zoek)) {
            return;   // (not the whole town is loaded: a resident may be in the part that isn't)
        }
        herstel(level, plekken(level, start), zoek);
    }

    /** Are the entities of every chunk of this area loaded? */
    public static boolean geladen(ServerLevel level, AABB area) {
        int x0 = (int) Math.floor(area.minX) >> 4, x1 = (int) Math.floor(area.maxX) >> 4;
        int z0 = (int) Math.floor(area.minZ) >> 4, z1 = (int) Math.floor(area.maxZ) >> 4;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!level.hasChunk(x, z) || !level.areEntitiesLoaded(ChunkPos.pack(x, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The residents of this town (world spots), from the templates of its pieces. */
    public static List<Plek> plekken(ServerLevel level, StructureStart start) {
        List<Plek> out = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof PoolElementStructurePiece pool)) {
                continue;
            }
            Identifier id = PleinSlot.template(piece);
            if (id == null) {
                continue;
            }
            Rotation rotation = pool.getRotation();
            BlockPos origin = pool.getPosition();
            for (Plek p : template(level, id)) {
                Vec3 pos = StructureTemplate.transform(p.pos(), Mirror.NONE, rotation, BlockPos.ZERO).add(Vec3.atLowerCornerOf(origin));
                CompoundTag nbt = p.nbt().copy();
                nbt.putString("guhs_rotation", rotation.name());
                out.add(new Plek(p.naam(), pos, nbt));
            }
        }
        return out;
    }

    /** The residents in a template (template coordinates), read once. */
    static List<Plek> template(ServerLevel level, Identifier id) {
        return TEMPLATES.computeIfAbsent(id, k -> {
            List<Plek> out = new ArrayList<>();
            Optional<StructureTemplate> template = level.getStructureManager().get(k);
            if (template.isEmpty()) {
                return out;
            }
            ListTag entities = template.get().save(new CompoundTag()).getListOrEmpty("entities");
            for (int i = 0; i < entities.size(); i++) {
                CompoundTag e = entities.getCompoundOrEmpty(i);
                CompoundTag nbt = e.getCompoundOrEmpty("nbt");
                CompoundTag data = nbt.getCompoundOrEmpty("NeoForgeData");
                String naam = data.getStringOr(GuhHooks.BEWONER_NAAM, "");
                ListTag pos = e.getListOrEmpty("pos");
                if (naam.isEmpty() || !data.getBooleanOr(GuhHooks.BEWONER, false) || pos.size() != 3) {
                    continue;
                }
                out.add(new Plek(naam, new Vec3(pos.getDoubleOr(0, 0), pos.getDoubleOr(1, 0), pos.getDoubleOr(2, 0)), nbt.copy()));
            }
            return out;
        });
    }

    /**
     * Every resident of these spots that isn't in the area any more comes back at its spot; a re-created one that turns
     * out to be double (the old one came back) goes away again. Returns how many came back.
     */
    public static int herstel(ServerLevel level, List<Plek> plekken, AABB zoek) {
        Map<String, List<GuhEntity>> er = new HashMap<>();
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, zoek, g -> g.isAlive() && GuhHooks.isBewoner(g) && !g.isTame())) {
            er.computeIfAbsent(guh.getPersistentData().getStringOr(GuhHooks.BEWONER_NAAM, ""), k -> new ArrayList<>()).add(guh);
        }
        int terug = 0;
        for (Plek plek : plekken) {
            List<GuhEntity> zelfde = er.getOrDefault(plek.naam(), List.of());
            if (zelfde.isEmpty()) {
                if (maak(level, plek) != null) {
                    terug++;
                }
                continue;
            }
            if (zelfde.size() > 1) {
                // double: the first one born in the town stays, re-created extras leave
                GuhEntity blijft = zelfde.stream().filter(g -> !g.getPersistentData().getBooleanOr(HERBOREN, false)).findFirst().orElse(zelfde.get(0));
                for (GuhEntity g : new ArrayList<>(zelfde)) {
                    if (g != blijft && g.getPersistentData().getBooleanOr(HERBOREN, false)) {
                        g.discard();
                        zelfde.remove(g);
                    }
                }
            }
        }
        return terug;
    }

    /** A new resident at this spot, made like the town template makes it. */
    static GuhEntity maak(ServerLevel level, Plek plek) {
        CompoundTag tag = plek.nbt().copy();
        Rotation rotation = Rotation.NONE;
        try {
            rotation = Rotation.valueOf(tag.getStringOr("guhs_rotation", "NONE"));
        } catch (IllegalArgumentException ignored) {
            // (no rotation)
        }
        tag.remove("guhs_rotation");
        Vec3 pos = plek.pos();
        ListTag posTag = new ListTag();
        posTag.add(DoubleTag.valueOf(pos.x));
        posTag.add(DoubleTag.valueOf(pos.y));
        posTag.add(DoubleTag.valueOf(pos.z));
        tag.put("Pos", posTag);
        tag.remove("UUID");
        Optional<Entity> made;
        try {
            made = EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag), level, EntitySpawnReason.STRUCTURE);
        } catch (Exception e) {
            return null;
        }
        if (made.isEmpty() || !(made.get() instanceof GuhEntity guh)) {
            return null;
        }
        float yRot = guh.rotate(rotation);
        guh.snapTo(pos.x, pos.y, pos.z, yRot, guh.getXRot());
        guh.setYBodyRot(yRot);
        guh.setYHeadRot(yRot);
        guh.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), EntitySpawnReason.STRUCTURE, null);
        guh.getPersistentData().putBoolean(GuhHooks.BEWONER, true);
        guh.getPersistentData().putString(GuhHooks.BEWONER_NAAM, plek.naam());
        guh.getPersistentData().putBoolean(HERBOREN, true);
        level.addFreshEntityWithPassengers(guh);   // (KnuffeldalEvents.onJoin: its spot is its home, its name, protected)
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, pos.x, pos.y + 0.8, pos.z, 5, 0.3, 0.3, 0.3, 0.02);
        return guh;
    }

    /** (tests) forget what was read and when. */
    public static void reset() {
        TEMPLATES.clear();
        LAATST.clear();
    }
}
