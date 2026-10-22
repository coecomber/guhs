package nl.juiced.guhs.feature.safari;

import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

/**
 * The park's animals: for every home of plekken.json its grown-ups (half of them males, rounded up) and young, spread over the
 * home. A respawn raises the {@link SafariOpslag#generatie generation} first, so every animal of before removes itself as soon
 * as its chunk is loaded; then the homes are filled {@link #PER_TICK} at a time (their chunks are loaded for that).
 * Runs by itself once the park of a new version is stamped, and on {@code /guhs safari dieren}.
 */
public final class Populatie {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PER_TICK = 6;
    /** The next home to fill (-1: nothing to do). Not saved: an interrupted fill starts over on the next start. */
    private static int volgende = -1;

    public static void begin(MinecraftServer server) {
        SafariOpslag s = SafariOpslag.get(server);
        s.generatie++;
        s.setDirty();
        volgende = 0;
    }

    public static boolean bezig() {
        return volgende >= 0;
    }

    static void opStart(MinecraftServer server) {
        volgende = -1;
        SafariOpslag s = SafariOpslag.get(server);
        if (s.bezig == 0 && s.gebouwd >= ParkData.get().versie && s.dierenVoor < s.gebouwd) {
            begin(server);
        }
    }

    static void tick(MinecraftServer server) {
        if (volgende < 0) {
            return;
        }
        ServerLevel level = Safari.level(server);
        if (level == null) {
            volgende = -1;
            return;
        }
        List<ParkData.Plek> plekken = ParkData.get().plekken;
        SafariOpslag s = SafariOpslag.get(server);
        for (int n = 0; n < PER_TICK && volgende < plekken.size(); n++, volgende++) {
            vul(level, plekken.get(volgende), s.generatie);
        }
        if (volgende >= plekken.size()) {
            volgende = -1;
            LOGGER.info("Safari: {} Kriebelhuis critters", vulKriebelCritters(level, s.generatie));
            s.dierenVoor = Math.max(s.gebouwd, ParkData.get().versie);
            s.setDirty();
            LOGGER.info("Safari: {} homes filled (generation {})", plekken.size(), s.generatie);
        }
    }

    /** Puts the family of one home in the world; returns how many animals. */
    public static int vul(ServerLevel level, ParkData.Plek p, int generatie) {
        if (!p.entity().isEmpty()) {
            return vulCritters(level, p, generatie);
        }
        ParkData d = ParkData.get();
        ParkData.Soort soort = d.soort(p.soort());
        // the zone of the home is where its centre lies (outside the raster: no zone check rather than a stuck animal)
        int zone = d.zone(p.centrum().getX(), p.centrum().getZ());
        String gevraagd = p.zone().isEmpty() && soort != null ? soort.zone() : p.zone();
        // (round 4: a Kriebelhuis home lies in the lobby's entree cells; its species' "zone" is no park zone)
        if (zone >= 0 && !gevraagd.isEmpty() && d.zoneIndex(gevraagd) >= 0 && zone != d.zoneIndex(gevraagd) && !d.testPlateau) {
            LOGGER.warn("Safari: home {} of {} lies in zone {}, not in {}", p.id(), p.soort(), d.zoneNaam(zone), gevraagd);
        }
        RandomSource rnd = RandomSource.create(p.id() * 7919L + generatie);
        int n = 0;
        int mannen = (p.volwassenen() + 1) / 2;
        int totaal = Math.max(p.volwassenen() + p.jongen(), minimum(soort));
        for (int i = 0; i < totaal; i++) {
            String variant = i >= p.volwassenen() + p.jongen() ? (i % 2 == 0 ? "man" : "vrouw")
                    : i < mannen ? "man" : i < p.volwassenen() ? "vrouw" : "baby";
            SafariDier dier = SafariFeature.DIER.get().create(level, EntitySpawnReason.STRUCTURE);
            if (dier == null) {
                continue;
            }
            dier.zet(p.soort(), variant, p.centrum(), p.straal(), zone, p.id(), generatie);
            nl.juiced.guhs.feature.safari.gedrag.Bewoners.Bewoner bewoner = nl.juiced.guhs.feature.safari.gedrag.Bewoners.voor(p, i);
            if (bewoner != null) {
                dier.zetBewoner(bewoner.id(), bewoner.naam());      // round 3: a "bekende bewoner" (name tag, Safari dex list)
            }
            double hoek = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * p.straal() * 0.7;
            int x = p.centrum().getX() + (int) Math.round(Math.cos(hoek) * r), z = p.centrum().getZ() + (int) Math.round(Math.sin(hoek) * r);
            if (zone >= 0 && d.zone(x, z) != zone) {
                x = p.centrum().getX();
                z = p.centrum().getZ();
            }
            level.getChunk(x >> 4, z >> 4);
            BlockPos plek = null;
            if (soort != null && soort.hangt()) {
                // the sloth hangs under a low branch near its home (visible from the ground and the paths)
                plek = hangPlek(level, x, z, Mth.ceil(dier.getBbHeight()));
                if (plek != null) {
                    dier.hang(plek);
                }
            }
            if (plek == null) {
                // (round 4: indoors, under the Kriebelhuis roof, a tree animal starts on the floor of its cage too)
                boolean binnen = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) > p.centrum().getY() + SafariDier.BINNEN;
                plek = p.boom() && !binnen ? boom(level, x, z) : dier.grond(level, x, z);
            }
            dier.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, rnd.nextFloat() * 360f, 0f);
            if (level.addFreshEntity(dier)) {
                n++;
            }
        }
        return n;
    }

    /** Test round 5: the small fish that swim in schools in the Kriebelhuis tanks (at least {@link #SCHOOL} of them). */
    static final java.util.Set<String> KLEINE_VISJES = java.util.Set.of("regenboogvis", "lantaarnvisje", "clownvis");
    /** Test round 5: at least this many of every species in a Kriebelhuis tank (and of every water critter), small fish a school. */
    static final int MIN_AQUARIUM = 3, SCHOOL = 8;

    /** Test round 5: how many of this species a home holds at least (Kriebelhuis water animals: 3, small fish 8; else 0). */
    static int minimum(@javax.annotation.Nullable ParkData.Soort soort) {
        if (soort == null || !ParkData.KRIEBELHUIS.equals(soort.zone()) || !soort.zee() || soort.decor()) {
            return 0;
        }
        return KLEINE_VISJES.contains(soort.naam()) ? SCHOOL : MIN_AQUARIUM;
    }

    // --- round 4 (L4): the guhs critters in the Kriebelhuis (vlindertuin, kikkerhoek, axolotls, aquarium) ----------------------

    /** The persistent-data key of a critter living in a park home: its home (x, y, z, straal) and generation. */
    public static final String CRITTER = "guhs_safari_critter";

    /**
     * A home of another guhs entity type ({@code entity}, e.g. "guhs:koi" or just "koi"): that many of it, passive (never
     * despawns, never hurt), on the floor or in the water of the home; {@link #bewaakCritter} keeps it there. Not in the dex.
     */
    static int vulCritters(ServerLevel level, ParkData.Plek p, int generatie) {
        RandomSource rnd = RandomSource.create(p.id() * 7919L + generatie);
        int n = 0;
        for (int i = 0; i < Math.max(1, p.volwassenen() + p.jongen()); i++) {
            double hoek = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * p.straal() * 0.6;
            int x = p.centrum().getX() + (int) Math.round(Math.cos(hoek) * r), z = p.centrum().getZ() + (int) Math.round(Math.sin(hoek) * r);
            level.getChunk(x >> 4, z >> 4);
            BlockPos plek = critterPlek(level, x, z, p);
            if (plek == null) {
                plek = critterPlek(level, p.centrum().getX(), p.centrum().getZ(), p);
            }
            if (plek == null) {
                plek = p.centrum();
            }
            if (spawnCritter(level, p.entity(), "", net.minecraft.world.phys.Vec3.atBottomCenterOf(plek), p.centrum().getX(), plek.getY(),
                    p.centrum().getZ(), p.straal(), generatie, rnd.nextFloat() * 360f)) {
                n++;
            }
        }
        return n;
    }

    /**
     * The critters of the Kriebelhuis ({@code data/guhs/safari/kriebelhuis.json} "critters", written by zone_kriebelhuis.py):
     * per critter {@code entity} ("guhs:koi"...), {@code variant} (its texture name, e.g. "koi_goud": the colour after the
     * entity's name goes in as "Kleur"/"Variant"), {@code pos} [x,y,z] and {@code straal}. Returns how many.
     */
    static int vulKriebelCritters(ServerLevel level, int generatie) {
        com.google.gson.JsonArray lijst = kriebelCritters();
        int n = 0;
        java.util.Map<String, java.util.List<com.google.gson.JsonObject>> waterGroepen = new java.util.LinkedHashMap<>();
        for (int i = 0; i < lijst.size(); i++) {
            if (!lijst.get(i).isJsonObject()) {
                continue;
            }
            com.google.gson.JsonObject o = lijst.get(i).getAsJsonObject();
            if (!o.has("entity") || !o.has("pos")) {
                continue;
            }
            com.google.gson.JsonArray a = o.getAsJsonArray("pos");
            net.minecraft.world.phys.Vec3 pos = new net.minecraft.world.phys.Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
            level.getChunk(Mth.floor(pos.x) >> 4, Mth.floor(pos.z) >> 4);
            String variant = o.has("variant") ? o.get("variant").getAsString() : "";
            int straal = o.has("straal") ? o.get("straal").getAsInt() : 3;
            if (spawnCritter(level, o.get("entity").getAsString(), variant, pos, Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z), straal,
                    generatie, (i * 73) % 360)) {
                n++;
            }
            if (o.has("water") && o.get("water").getAsBoolean()) {
                String sleutel = (o.has("afdeling") ? o.get("afdeling").getAsString() : "") + "|" + o.get("entity").getAsString();
                waterGroepen.computeIfAbsent(sleutel, k -> new java.util.ArrayList<>()).add(o);
            }
        }
        // test round 5: every water critter at least three times in its tank (a lonely jellyfish looked lost)
        for (java.util.List<com.google.gson.JsonObject> groep : waterGroepen.values()) {
            com.google.gson.JsonObject o = groep.get(0);
            com.google.gson.JsonArray a = o.getAsJsonArray("pos");
            int straal = o.has("straal") ? o.get("straal").getAsInt() : 3;
            String variant = o.has("variant") ? o.get("variant").getAsString() : "";
            for (int k = groep.size(); k < MIN_AQUARIUM; k++) {
                double hoek = k * 2.1, r = Math.min(1.5, straal * 0.5);
                net.minecraft.world.phys.Vec3 pos = new net.minecraft.world.phys.Vec3(a.get(0).getAsDouble() + Math.cos(hoek) * r, a.get(1).getAsDouble(),
                        a.get(2).getAsDouble() + Math.sin(hoek) * r);
                if (spawnCritter(level, o.get("entity").getAsString(), variant, pos, Mth.floor(a.get(0).getAsDouble()), Mth.floor(a.get(1).getAsDouble()),
                        Mth.floor(a.get(2).getAsDouble()), straal, generatie, (k * 97) % 360)) {
                    n++;
                }
            }
        }
        return n;
    }

    /** The "critters" list of kriebelhuis.json (empty without the file). */
    static com.google.gson.JsonArray kriebelCritters() {
        try (java.io.InputStream in = Populatie.class.getResourceAsStream("/data/guhs/safari/kriebelhuis.json")) {
            if (in == null) {
                return new com.google.gson.JsonArray();
            }
            com.google.gson.JsonElement e = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            if (e.isJsonObject() && e.getAsJsonObject().has("critters")) {
                return e.getAsJsonObject().getAsJsonArray("critters");
            }
        } catch (Exception ex) {
            LOGGER.error("Safari: could not read kriebelhuis.json", ex);
        }
        return new com.google.gson.JsonArray();
    }

    /**
     * One guhs critter at {@code pos} living at home x,y,z (radius straal): passive (never despawns, never hurt), its colour
     * from {@code variant}, and marked ({@link #CRITTER}) so {@link #bewaakCritter} keeps it there. False: unknown entity.
     */
    public static boolean spawnCritter(ServerLevel level, String entity, String variant, net.minecraft.world.phys.Vec3 pos, int x, int y, int z,
                                       int straal, int generatie, float yaw) {
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(entity.contains(":") ? entity : "guhs:" + entity);
        if (id == null || !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            LOGGER.warn("Safari: unknown critter entity {}", entity);
            return false;
        }
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", id.toString());
        tag.putBoolean("PersistenceRequired", true);
        tag.putBoolean("Invulnerable", true);
        String kleur = variant.startsWith(id.getPath() + "_") ? variant.substring(id.getPath().length() + 1) : variant;
        if (!kleur.isEmpty()) {
            tag.putString("Kleur", kleur);              // (the koi, the kikkerguh...; unknown keys are ignored)
            tag.putString("Variant", kleur);
        }
        net.minecraft.world.entity.Entity e = net.minecraft.world.entity.EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.STRUCTURE, en -> en);
        if (e == null) {
            return false;
        }
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        t.putInt("X", x);
        t.putInt("Y", y);
        t.putInt("Z", z);
        t.putInt("Straal", Math.max(2, straal));
        t.putInt("Generatie", generatie);
        e.getPersistentData().put(CRITTER, t);
        e.setInvulnerable(true);
        if (e instanceof net.minecraft.world.entity.Mob m) {
            m.setPersistenceRequired();
        }
        e.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        return level.addFreshEntity(e);
    }

    /** Where a critter goes in this column of its home: in the water (a water home) or on the floor, near the home's height. */
    @javax.annotation.Nullable
    static BlockPos critterPlek(ServerLevel level, int x, int z, ParkData.Plek p) {
        BlockPos b = SafariDier.binnenGrond(level, x, z, p.centrum().getY(), p.water(), false);
        return b == null && p.water() ? SafariDier.binnenGrond(level, x, z, p.centrum().getY(), false, false) : b;
    }

    /**
     * Every 2 s for a critter of a park home: one of an older generation goes (the homes were refilled); one that strayed
     * (more than its home + 4 away, or fallen) is put back on its home's centre, its speed gone; it never despawns.
     */
    public static void bewaakCritter(net.minecraft.world.entity.Entity e) {
        if (!(e.level() instanceof ServerLevel level) || (e.tickCount + e.getId()) % 40 != 0 || e instanceof SafariDier) {
            return;
        }
        net.minecraft.nbt.CompoundTag t = e.getPersistentData().getCompoundOrEmpty(CRITTER);
        if (t.isEmpty()) {
            return;
        }
        if (t.getIntOr("Generatie", 0) < SafariOpslag.get(level.getServer()).generatie()) {
            e.discard();
            return;
        }
        int x = t.getIntOr("X", e.getBlockX()), y = t.getIntOr("Y", e.getBlockY()), z = t.getIntOr("Z", e.getBlockZ());
        if (weg(e.getX(), e.getY(), e.getZ(), x, y, z, t.getIntOr("Straal", 4))) {
            e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            e.teleportTo(x + 0.5, y, z + 0.5);
            e.fallDistance = 0;
        }
    }

    /** Has a critter at ex,ey,ez strayed from its home (centre x,y,z, radius straal)? */
    public static boolean weg(double ex, double ey, double ez, int x, int y, int z, int straal) {
        return Math.hypot(ex - (x + 0.5), ez - (z + 0.5)) > straal + 4 || ey < y - 8 || ey > y + 12;
    }

    /** A spot to hang under the lowest leaves near x,z (2..14 above the ground, with room below): its feet position. */
    @javax.annotation.Nullable
    static BlockPos hangPlek(ServerLevel level, int x, int z, int hoogte) {
        for (int r = 0; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int cx = x + dx, cz = z + dz;
                    level.getChunk(cx >> 4, cz >> 4);
                    int grond = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                    for (int y = grond + 2; y < grond + 14; y++) {
                        BlockPos b = new BlockPos(cx, y, cz);
                        if (level.getBlockState(b).is(BlockTags.LEAVES)) {
                            int voet = y - hoogte;
                            boolean vrij = voet > grond;
                            for (int k = voet; vrij && k < y; k++) {
                                vrij = level.getBlockState(new BlockPos(cx, k, cz)).isAir();
                            }
                            if (vrij) {
                                return new BlockPos(cx, voet, cz);
                            }
                            break;
                        }
                        if (!level.getBlockState(b).isAir()) {
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** On top of whatever stands here, leaves included (tree animals start on their branch). */
    private static BlockPos boom(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);
    }

    private Populatie() {
    }
}
