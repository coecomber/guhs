package nl.juiced.guhs.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.SleeRailBlock;
import nl.juiced.guhs.block.entity.SleeRailBlockEntity;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhSleeEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;
import nl.juiced.guhs.feature.eilanden.EilandenEvents;
import nl.juiced.guhs.feature.eilanden.EilandenFeature;
import nl.juiced.guhs.feature.gatenkaas.GatenkaasFeature;
import nl.juiced.guhs.feature.guheinde.MagereCellen;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasEvents;
import nl.juiced.guhs.feature.knuffeldal.Bewoners;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.PleinSlot;
import nl.juiced.guhs.feature.landdiertjes.Landdiertje;
import nl.juiced.guhs.feature.onderwater.OnderwaterProtection;
import nl.juiced.guhs.feature.piep.PieppiepmuisjeEntity;
import nl.juiced.guhs.item.GuhCompassItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Kermis;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.slee.SleePath;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.world.ModDimensions;
import nl.juiced.guhs.world.Terugkeer;
import nl.juiced.guhs.world.VoorIedereen;

/**
 * DEV ONLY (1.2.7): AutoCheck script commands to check, in a real generated world, the code that finds its spot through
 * the pieces of a building (the cells of a Knabbelkelder, the Wolkguh's island, Big Mika's hall, the residents of a town,
 * the kermis station, the wreck's chest, the larder's lectern, the raft's barrel). See tools/autocheck/wereld127_*.txt.
 * <pre>
 * ga &lt;structure&gt; [draai]      locate it in the Guhmension (draai: look for one that is placed rotated), fly to its first piece
 * gabiome &lt;biome&gt;             fly to the nearest biome of that kind in the Guhmension
 * plek cel &lt;i&gt; | wolk | bigmika | wrak | lectern | station | ton | bewoner &lt;naam&gt; | midden | lokaal &lt;x&gt; &lt;y&gt; &lt;z&gt;
 *                              the spot the mod's own code computes for it (the "plek"), with what really is there
 * tpplek [dx dy dz]            fly to the plek (+ offset)
 * plekcmd &lt;command with {p}&gt;   a command with the plek's coordinates
 * tel &lt;radius&gt; [plek]          what lives within that radius of the player (or the plek), per kind
 * cellen | bewoners            the ten cells / the town's residents: who is there
 * weg &lt;filter&gt; &lt;radius&gt; [n] [kill]   removes (or kills) the n nearest entities of that kind around the plek
 * stoor &lt;filter&gt;               puts the nearest one of that kind at the player's feet
 * terugkeer dump | &lt;ticks&gt;     what Terugkeer remembers / as if that many ticks went by
 * klik | breek                 right-click / break the plek's block as the player (the real game mode code)
 * sla &lt;filter&gt; | voer &lt;filter&gt; [n]   hit / feed kaas knabbels to the nearest one of that kind, as the player
 * adv &lt;path&gt;                   is this advancement of the player done?
 * hier                         what the lookups say at the player's spot
 * blokken &lt;r&gt;                  the blocks around the plek (y -1..2), relative to it
 * aanvul &lt;n&gt;                   n top-up tries of muisjes and land animals (without the dice)
 * perf                         what the periodic checks cost here
 * </pre>
 * Filters: mager, wolk, boss, slee, bewoner, bewoner:&lt;naam&gt;, wildguh, or an entity type id.
 */
final class WereldCheck {
    private static StructureStart start;
    private static BlockPos plek;
    private static String plekNaam = "";

    private WereldCheck() {
    }

    static AutoCheck.Action parse(String[] a, String rest) {
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "ga":
                return job(s -> ga(s, a[1], a.length > 2 && a[2].equals("draai")));
            case "gabiome":
                return job(s -> gaBiome(s, a[1]));
            case "plek":
                if (a[1].equals("ton")) {
                    return ton();
                }
                return job(s -> plek(s, a));
            case "tpplek":
                return job(s -> {
                    if (plek == null) {
                        return "!tpplek: no plek";
                    }
                    Vec3 to = Vec3.atBottomCenterOf(plek).add(a.length > 3 ? new Vec3(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])) : Vec3.ZERO);
                    vlieg(player(s), level(s), to);
                    return "tpplek -> " + fmt(to);
                });
            case "plekcmd":
            {
                AutoCheck.Action[] cmd = {null};
                return mc -> {
                    if (plek == null) {
                        AutoCheck.problem("plekcmd: no plek");
                        return true;
                    }
                    if (cmd[0] == null) {
                        String c = rest.replace("{p}", plek.getX() + " " + plek.getY() + " " + plek.getZ());
                        cmd[0] = AutoCheck.command("execute in guhs:guhmension run " + (c.startsWith("/") ? c.substring(1) : c));
                    }
                    return cmd[0].tick(mc);
                };
            }
            case "tel":
                return job(s -> tel(s, Double.parseDouble(a[1]), a.length > 2 && a[2].equals("plek"), a.length > 3 ? a[3] : rest));
            case "cellen":
                return job(WereldCheck::cellen);
            case "bewoners":
                return job(WereldCheck::bewoners);
            case "weg":
                return job(s -> weg(s, a[1], Double.parseDouble(a[2]), a.length > 3 && !a[3].equals("kill") ? Integer.parseInt(a[3]) : Integer.MAX_VALUE,
                        a[a.length - 1].equals("kill")));
            case "stoor":
                return job(s -> {
                    ServerPlayer sp = player(s);
                    Entity e = dichtst(s, a[1], 80);
                    if (e == null) {
                        return "!stoor: no " + a[1];
                    }
                    Vec3 was = e.position();
                    e.teleportTo(sp.getX(), sp.getY(), sp.getZ());
                    return "stoor " + a[1] + ": moved from " + fmt(was) + " to " + fmt(e.position());
                });
            case "terugkeer":
                return job(s -> {
                    ServerLevel level = level(s);
                    if (!a[1].equals("dump")) {
                        Terugkeer.verschuif(level, Long.parseLong(a[1]));
                    }
                    StringBuilder sb = new StringBuilder("terugkeer " + a[1] + " (game time " + level.getGameTime() + "):");
                    Terugkeer.alles(level).forEach((k, v) -> sb.append(' ').append(k).append('=').append(level.getGameTime() - v).append("t ago;"));
                    return sb.toString();
                });
            case "klik":
                return job(WereldCheck::klik);
            case "breek":
                return job(WereldCheck::breek);
            case "sla":
                return job(s -> sla(s, a[1]));
            case "voer":
                return job(s -> voer(s, a[1], a.length > 2 ? Integer.parseInt(a[2]) : 1));
            case "adv":
                return job(s -> {
                    ServerPlayer sp = player(s);
                    var holder = s.getAdvancements().get(Guhs.id(a[1]));
                    return "adv guhs:" + a[1] + ": " + (holder == null ? "!UNKNOWN" : sp.getAdvancements().getOrStartProgress(holder).isDone() ? "DONE" : "not done");
                });
            case "rit":
                // rit start | stop: get on the nearest station sled and ride off / get off where it is (it should ride home)
                return job(s -> {
                    ServerPlayer sp = player(s);
                    if (a[1].equals("stop")) {
                        Entity v = sp.getVehicle();
                        sp.stopRiding();
                        return "rit stop: got off " + (v == null ? "nothing" : soort(v) + " at " + fmt(v.position()));
                    }
                    if (!(dichtst(s, "slee", 64) instanceof GuhSleeEntity sled)) {
                        return "!rit: no station sled";
                    }
                    boolean op = sp.startRiding(sled, true, true);
                    sled.setRunning(true);
                    return "rit start: riding " + op + " on " + soort(sled) + " at " + fmt(sled.position());
                });
            case "hier":
                return job(WereldCheck::hier);
            case "blokken":
                return job(s -> {
                    if (plek == null) {
                        return "!blokken: no plek";
                    }
                    ServerLevel level = level(s);
                    int r = Integer.parseInt(a[1]);
                    StringBuilder sb = new StringBuilder("blokken around " + plekNaam + " " + plek.toShortString() + " (relative):");
                    for (int y = -1; y <= 2; y++) {
                        sb.append(" | y").append(y).append(":");
                        for (BlockPos q : BlockPos.betweenClosed(plek.offset(-r, y, -r), plek.offset(r, y, r))) {
                            var st = level.getBlockState(q);
                            if (!st.isAir() && !st.is(Blocks.WATER)) {
                                sb.append(' ').append(q.getX() - plek.getX()).append(',').append(q.getZ() - plek.getZ()).append('=')
                                        .append(BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath());
                            }
                        }
                    }
                    return sb.toString();
                });
            case "aanvul":
                return job(s -> aanvul(s, Integer.parseInt(a[1])));
            case "perf":
                return job(WereldCheck::perf);
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private static AutoCheck.Action job(java.util.function.Function<MinecraftServer, String> f) {
        return AutoCheck.server(s -> {
            try {
                return f.apply(s);
            } catch (Throwable t) {
                org.apache.logging.log4j.LogManager.getLogger("GuhsAutoCheck").error("WereldCheck", t);
                return "!" + t;
            }
        }, r -> {
            if (r != null && r.startsWith("!")) {
                AutoCheck.problem(r.substring(1));
            } else {
                AutoCheck.note("  " + r);
            }
        });
    }

    private static ServerPlayer player(MinecraftServer s) {
        return AutoCheck.player(s);
    }

    private static ServerLevel level(MinecraftServer s) {
        return s.getLevel(ModDimensions.GUHMENSION);
    }

    private static String fmt(Vec3 v) {
        return String.format(Locale.ROOT, "%.1f %.1f %.1f", v.x, v.y, v.z);
    }

    private static void vlieg(ServerPlayer sp, ServerLevel level, Vec3 to) {
        if (sp.getAbilities().mayfly) {
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
        }
        sp.teleportTo(level, to.x, to.y, to.z, java.util.Set.of(), sp.getYRot(), 30, true);
    }

    private static PoolElementStructurePiece stuk(String deel) {
        if (start == null) {
            return null;
        }
        for (StructurePiece p : start.getPieces()) {
            if (p instanceof PoolElementStructurePiece pe && (deel == null || pe.getElement().toString().contains(deel))) {
                return pe;
            }
        }
        return null;
    }

    private static Predicate<Entity> filter(String f) {
        return e -> {
            if (e instanceof Player || !e.isAlive()) {
                return false;
            }
            if (f.equals("mager")) {
                return e instanceof GuhEntity g && g.getVariant() == GuhVariant.MAGER;
            } else if (f.equals("wolk")) {
                return EilandenEvents.isWolk(e) && !((GuhEntity) e).isTame();
            } else if (f.equals("boss")) {
                return e instanceof MikaEntity m && m.isBoss();
            } else if (f.equals("slee")) {
                return e instanceof GuhSleeEntity sl && sl.isLocked();
            } else if (f.equals("wildguh")) {
                return e instanceof GuhEntity g && g.getVariant() != GuhVariant.MAGER && !g.isTame() && !GuhHooks.isBewoner(g);
            } else if (f.startsWith("bewoner")) {
                return e instanceof GuhEntity g && GuhHooks.isBewoner(g)
                        && (!f.contains(":") || g.getPersistentData().getStringOr(GuhHooks.BEWONER_NAAM, "").equals(f.substring(f.indexOf(':') + 1)));
            }
            return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(f.contains(":") ? f : "guhs:" + f);
        };
    }

    private static Vec3 bij(MinecraftServer s) {
        return plek != null ? Vec3.atBottomCenterOf(plek) : player(s).position();
    }

    private static List<Entity> rond(MinecraftServer s, Vec3 c, String f, double r) {
        List<Entity> l = new ArrayList<>(level(s).getEntities((Entity) null, new AABB(c, c).inflate(r), filter(f)));
        l.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        return l;
    }

    private static Entity dichtst(MinecraftServer s, String f, double r) {
        List<Entity> l = rond(s, bij(s), f, r);
        return l.isEmpty() ? null : l.get(0);
    }

    private static String soort(Entity e) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().replace("guhs:", "").replace("minecraft:", "mc:");
        if (e instanceof GuhEntity g) {
            id += "[" + g.getVariant().id() + (g.isTame() ? ",tam" : "")
                    + (GuhHooks.isBewoner(g) ? ",bewoner:" + g.getPersistentData().getStringOr(GuhHooks.BEWONER_NAAM, "?") : "") + "]";
        } else if (e instanceof MikaEntity m && m.isBoss()) {
            id += "[boss]";
        } else if (e instanceof GuhSleeEntity sl) {
            id += sl.isLocked() ? "[locked]" : "[los]";
        } else if (e instanceof GuhNpcEntity n) {
            id += "[" + n.getKind().id() + "]";
        } else if (e instanceof TamableAnimal t && t.isTame()) {
            id += "[tam]";
        }
        return id;
    }

    // ------------------------------------------------------------------------------------------------ going places

    private static String ga(MinecraftServer s, String naam, boolean draai) {
        ServerLevel level = level(s);
        Identifier id = Identifier.parse(naam.contains(":") ? naam : "guhs:" + naam);
        Holder<Structure> holder = s.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, id)).orElse(null);
        if (holder == null) {
            return "!ga: unknown structure " + id;
        }
        StructureStart best = null;
        int tries = 0;
        for (int k = 0; k < (draai ? 8 : 3); k++) {
            BlockPos origin = new BlockPos(k * 1700, 64, (k % 2 == 0 ? 1 : -1) * k * 1300);
            var pair = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), origin, 100, false);
            tries++;
            if (pair == null) {
                continue;
            }
            BlockPos pos = pair.getFirst();
            StructureStart st = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(holder.value());
            if (st == null || !st.isValid() || st.getPieces().isEmpty()) {
                continue;
            }
            best = st;
            if (!draai || st.getPieces().get(0) instanceof PoolElementStructurePiece pe && pe.getRotation() != net.minecraft.world.level.block.Rotation.NONE) {
                break;
            }
        }
        if (best == null) {
            return "!ga: " + id + " not found";
        }
        start = best;
        plek = null;
        BoundingBox b = best.getBoundingBox();
        StringBuilder sb = new StringBuilder("ga " + id + " (" + tries + " tries): box " + b.minX() + ".." + b.maxX() + " x " + b.minY() + ".." + b.maxY() + " y "
                + b.minZ() + ".." + b.maxZ() + " z (" + b.getXSpan() + "x" + b.getYSpan() + "x" + b.getZSpan() + "), " + best.getPieces().size() + " pieces:");
        int n = 0;
        for (StructurePiece p : best.getPieces()) {
            if (n++ >= 14) {
                sb.append(" ...");
                break;
            }
            BoundingBox pb = p.getBoundingBox();
            sb.append(" | ").append(p instanceof PoolElementStructurePiece pe ? PleinSlot.template(pe) + " at " + pe.getPosition().toShortString() + " " + pe.getRotation()
                    : p.getClass().getSimpleName()).append(" box ").append(pb.minX()).append(",").append(pb.minY()).append(",").append(pb.minZ()).append("..")
                    .append(pb.maxX()).append(",").append(pb.maxY()).append(",").append(pb.maxZ());
        }
        BoundingBox pb = best.getPieces().get(0).getBoundingBox();
        vlieg(player(s), level, new Vec3((pb.minX() + pb.maxX() + 1) / 2.0, (pb.minY() + pb.maxY() + 1) / 2.0, (pb.minZ() + pb.maxZ() + 1) / 2.0));
        return sb.toString();
    }

    private static String gaBiome(MinecraftServer s, String naam) {
        ServerLevel level = level(s);
        Identifier id = Identifier.parse(naam.contains(":") ? naam : "guhs:" + naam);
        var pair = level.findClosestBiome3d(h -> h.is(id), new BlockPos(0, 64, 0), 6400, 32, 64);
        if (pair == null) {
            return "!gabiome: " + id + " not found";
        }
        BlockPos p = pair.getFirst();
        level.getChunk(p.getX() >> 4, p.getZ() >> 4);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
        start = null;
        plek = null;
        vlieg(player(s), level, new Vec3(p.getX() + 0.5, y + 12, p.getZ() + 0.5));
        return "gabiome " + id + " -> " + p.getX() + " " + (y + 12) + " " + p.getZ();
    }

    // ------------------------------------------------------------------------------------------------ the plek

    private static String plek(MinecraftServer s, String[] a) {
        ServerLevel level = level(s);
        String wat = a[1];
        if (start == null) {
            return "!plek: no structure (ga first)";
        }
        PoolElementStructurePiece p0 = stuk(null);
        StringBuilder extra = new StringBuilder();
        BlockPos pos;
        switch (wat) {
            case "cel" -> {
                pos = Terugkeer.wereld(stuk("knabbelkelder"), MagereCellen.CELLEN.get(Integer.parseInt(a[2])));
                wat += a[2];
            }
            case "wolk" -> pos = Terugkeer.wereld(stuk("zwevende_eilanden"), EilandenEvents.WOLK_PLEK);
            case "bigmika" -> pos = Terugkeer.wereld(stuk(VoorIedereen.BIG_MIKA_HAL), VoorIedereen.BIG_MIKA_PLEK);
            case "wrak" -> pos = Terugkeer.wereld(stuk(null), VoorIedereen.WRAK_KIST);
            case "lokaal" -> pos = Terugkeer.wereld(p0, new BlockPos(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])));
            case "midden" -> {
                BoundingBox b = p0.getBoundingBox();
                pos = new BlockPos((b.minX() + b.maxX()) / 2, (b.minY() + b.maxY()) / 2, (b.minZ() + b.maxZ()) / 2);
            }
            case "lectern" -> {
                BoundingBox b = start.getBoundingBox();
                List<BlockPos> found = new ArrayList<>();
                for (BlockPos q : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
                    if (level.getBlockState(q).is(Blocks.LECTERN)) {
                        found.add(q.immutable());
                    }
                }
                if (found.isEmpty()) {
                    return "!plek lectern: no lectern in the structure box";
                }
                pos = found.get(0);
                extra.append(" lecterns in the box: ").append(found.size());
            }
            case "station" -> {
                BoundingBox b = p0.getBoundingBox();
                Kermis.Area area = Kermis.area(level, new BlockPos((b.minX() + b.maxX()) / 2, (b.minY() + b.maxY()) / 2, (b.minZ() + b.maxZ()) / 2));
                if (area == null) {
                    return "!plek station: Kermis.area is null at the middle of the kermis";
                }
                SleePath.Placement st = Kermis.station(level, area);
                if (st == null) {
                    return "!plek station: Kermis.station is null";
                }
                pos = st.anchor();
                extra.append(" facing ").append(st.facing()).append(" shape ").append(st.shape());
            }
            case "bewoner" -> {
                pos = null;
                for (Bewoners.Plek pl : Bewoners.plekken(level, start)) {
                    if (pl.naam().equals(a[2])) {
                        pos = BlockPos.containing(pl.pos());
                    }
                }
                if (pos == null) {
                    return "!plek bewoner: no resident " + a[2] + " in the templates";
                }
                wat += ":" + a[2];
            }
            default -> {
                return "!plek: unknown " + wat;
            }
        }
        plek = pos;
        plekNaam = wat;
        return "plek " + wat + " = " + pos.toShortString() + extra + watIsEr(s);
    }

    /** What really is at the plek: the blocks, and the inhabitant that should be near. */
    private static String watIsEr(MinecraftServer s) {
        ServerLevel level = level(s);
        ServerPlayer sp = player(s);
        StringBuilder sb = new StringBuilder(": block " + naam(level, plek) + ", below " + naam(level, plek.below()));
        String f = plekNaam.startsWith("cel") ? "mager" : plekNaam.equals("wolk") ? "wolk" : plekNaam.equals("bigmika") ? "boss" : plekNaam.equals("station") ? "slee"
                : plekNaam.startsWith("bewoner") ? plekNaam : null;
        if (f != null) {
            List<Entity> l = rond(s, Vec3.atBottomCenterOf(plek), f, 96);
            sb.append("; ").append(l.size()).append(" x ").append(f).append(" within 96");
            for (Entity e : l) {
                sb.append(String.format(Locale.ROOT, " [%.1f away at %s", Math.sqrt(e.distanceToSqr(Vec3.atBottomCenterOf(plek))), fmt(e.position())));
                if (e instanceof GuhEntity g) {
                    if (f.equals("wolk")) {
                        long home = g.getPersistentData().getLongOr("guhs_wolk_home", Long.MIN_VALUE);
                        sb.append(" home ").append(home == Long.MIN_VALUE ? "none" : BlockPos.of(home).toShortString()).append(" scale ").append(g.getGuhScale()).append(" wears");
                        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
                            if (g.getClothes(slot) != null) {
                                sb.append(' ').append(g.getClothes(slot));
                            }
                        }
                    }
                    sb.append(g.isInvulnerable() ? " invulnerable" : "").append(g.isPersistenceRequired() ? " persistent" : " NOT-persistent");
                } else if (e instanceof GuhSleeEntity sl) {
                    sb.append(sl.isRunning() ? " RUNNING" : " parked").append(sl.isHoming() ? " homing" : "").append(sl.atStation() ? " at-station" : " NOT-at-station")
                            .append(sl.isVehicle() ? " with-rider" : "");
                } else if (e instanceof MikaEntity m) {
                    sb.append(" health ").append(m.getHealth()).append("/").append(m.getMaxHealth());
                }
                sb.append("]");
            }
        }
        switch (plekNaam) {
            case "wrak" -> sb.append("; isWrakKist ").append(VoorIedereen.isWrakKist(level, plek)).append(", inBubble-piece ")
                    .append(!Terugkeer.stukken(level, OnderwaterProtection.BUBBLE, plek, null).isEmpty());
            case "lectern" -> sb.append("; in a Stille Voorraadkelder piece (the mod's check) ").append(!Terugkeer.stukken(level, GatenkaasFeature.VOORRAADKELDER, plek, null).isEmpty())
                    .append(", book on it ").append(level.getBlockEntity(plek) instanceof net.minecraft.world.level.block.entity.LecternBlockEntity l ? Guhboek.of(l.getBook()) : null);
            case "station" -> sb.append("; rail ").append(level.getBlockState(plek).getBlock() instanceof SleeRailBlock).append(", finish ")
                    .append(level.getBlockEntity(plek) instanceof SleeRailBlockEntity r ? r.isFinish() : null)
                    .append(", protectedAt ").append(Kermis.protectedAt(level, plek));
            case "ton" -> sb.append("; lijktOpVlotje ").append(KaasmoerasEvents.lijktOpVlotje(level, plek)).append(", loot table ")
                    .append(level.getBlockEntity(plek) instanceof RandomizableContainerBlockEntity c ? c.getLootTable() : null).append(", marked ")
                    .append(level.getBlockEntity(plek) instanceof BlockEntity be && be.getPersistentData().getBooleanOr(KaasmoerasEvents.VLOTJE_TON, false));
            default -> {
            }
        }
        sb.append("; player has diary: ").append(dagboeken(sp)).append(" (flag ").append(GuhQuests.saved(sp).getBooleanOr(VoorIedereen.VOORRAADBOEK, false)).append(")");
        return sb.toString();
    }

    private static int dagboeken(ServerPlayer sp) {
        int n = 0;
        for (ItemStack st : sp.getInventory().getNonEquipmentItems()) {
            if (Guhboek.of(st) == Guhboek.VOORRAADKELDER) {
                n += st.getCount();
            }
        }
        return n;
    }

    private static String naam(ServerLevel level, BlockPos p) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString();
    }

    /** The barrel of a knabbelvlotje: looked for ring by ring around the player (the chunks are made on the way). */
    private static AutoCheck.Action ton() {
        int[] ring = {0};
        boolean[] klaar = {false};
        AutoCheck.Action[] bezig = {null};
        return mc -> {
            if (bezig[0] != null) {
                if (!bezig[0].tick(mc)) {
                    return false;
                }
                bezig[0] = null;
                return klaar[0];
            }
            int r = ring[0]++;
            bezig[0] = AutoCheck.server(s -> {
                ServerLevel level = level(s);
                ServerPlayer sp = player(s);
                int cx = sp.chunkPosition().x(), cz = sp.chunkPosition().z();
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        int bx = (cx + dx) * 16 + 8, bz = (cz + dz) * 16 + 8;
                        if (!level.getUncachedNoiseBiome(bx >> 2, 70 >> 2, bz >> 2).is(Identifier.parse("guhs:kaasmoeras"))) {
                            continue;
                        }
                        for (BlockEntity be : level.getChunk(cx + dx, cz + dz).getBlockEntities().values()) {
                            if (be instanceof RandomizableContainerBlockEntity c && KaasmoerasEvents.VLOTJE_LOOT.equals(c.getLootTable())) {
                                plek = be.getBlockPos();
                                plekNaam = "ton";
                                return "plek ton = " + plek.toShortString() + " (ring " + r + ")" + watIsEr(s);
                            }
                        }
                    }
                }
                return r >= 40 ? "!plek ton: no knabbelvlotje within 40 chunks" : null;
            }, res -> {
                if (res != null) {
                    klaar[0] = true;
                    if (res.startsWith("!")) {
                        AutoCheck.problem(res.substring(1));
                    } else {
                        AutoCheck.note("  " + res);
                    }
                }
            });
            return false;
        };
    }

    // ------------------------------------------------------------------------------------------------ counting

    private static String tel(MinecraftServer s, double r, boolean bijPlek, String label) {
        Vec3 c = bijPlek && plek != null ? Vec3.atBottomCenterOf(plek) : player(s).position();
        Map<String, Integer> n = new TreeMap<>();
        for (Entity e : level(s).getEntities((Entity) null, new AABB(c, c).inflate(r), e -> !(e instanceof Player) && e.isAlive())) {
            n.merge(soort(e), 1, Integer::sum);
        }
        return "tel " + label + " @ " + fmt(c) + " (game time " + level(s).getGameTime() + "): " + n;
    }

    private static String cellen(MinecraftServer s) {
        ServerLevel level = level(s);
        PoolElementStructurePiece st = stuk("knabbelkelder");
        if (st == null) {
            return "!cellen: not at a knabbelkelder";
        }
        BoundingBox b = start.getBoundingBox();
        List<GuhEntity> mager = level.getEntitiesOfClass(GuhEntity.class, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(16),
                g -> g.getVariant() == GuhVariant.MAGER && g.isAlive());
        StringBuilder sb = new StringBuilder("cellen: " + mager.size() + " magere guhs in the kelder;");
        int goed = 0;
        for (int i = 0; i < MagereCellen.CELLEN.size(); i++) {
            BlockPos cel = Terugkeer.wereld(st, MagereCellen.CELLEN.get(i));
            int thuis = 0, dichtbij = 0;
            double ver = 0;
            for (GuhEntity g : mager) {
                if (cel.equals(MagereCellen.thuis(g))) {
                    thuis++;
                    ver = Math.max(ver, Math.sqrt(g.distanceToSqr(Vec3.atBottomCenterOf(cel))));
                }
                if (g.distanceToSqr(Vec3.atBottomCenterOf(cel)) < 1.5 * 1.5) {
                    dichtbij++;
                }
            }
            if (thuis == 1 && dichtbij == 1) {
                goed++;
            }
            sb.append(String.format(Locale.ROOT, " cel%d %s: %d at home (%.1f away), %d within 1.5, floor %s;", i, cel.toShortString(), thuis, ver, dichtbij, naam(level, cel.below())));
        }
        int zonder = 0;
        for (GuhEntity g : mager) {
            if (MagereCellen.thuis(g) == null) {
                zonder++;
            }
        }
        sb.append(" cells with exactly one guh: ").append(goed).append("/10, guhs without a cell: ").append(zonder);
        return sb.toString();
    }

    private static String bewoners(MinecraftServer s) {
        ServerLevel level = level(s);
        if (start == null) {
            return "!bewoners: no town";
        }
        BoundingBox box = start.getBoundingBox();
        AABB zoek = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1).inflate(Bewoners.ZOEK_RAND);
        List<GuhEntity> er = level.getEntitiesOfClass(GuhEntity.class, zoek, g -> g.isAlive() && GuhHooks.isBewoner(g));
        List<Bewoners.Plek> plekken = Bewoners.plekken(level, start);
        StringBuilder sb = new StringBuilder("bewoners: " + plekken.size() + " in the templates, " + er.size() + " in the town (whole town loaded: " + Bewoners.geladen(level, zoek) + ");");
        for (Bewoners.Plek p : plekken) {
            sb.append(' ').append(p.naam()).append(" @").append(fmt(p.pos())).append(":");
            int n = 0;
            for (GuhEntity g : er) {
                if (g.getPersistentData().getStringOr(GuhHooks.BEWONER_NAAM, "").equals(p.naam())) {
                    n++;
                    BlockPos thuis = GuhHooks.thuis(g);
                    sb.append(String.format(Locale.ROOT, " [%.1f away, home %s%s%s%s]", Math.sqrt(g.distanceToSqr(p.pos())), thuis == null ? "none" : thuis.toShortString(),
                            g.isInvulnerable() ? ", invulnerable" : ", VULNERABLE", g.isTame() ? ", TAME" : "",
                            g.getPersistentData().getBooleanOr(Bewoners.HERBOREN, false) ? ", herboren" : ""));
                }
            }
            sb.append(n == 1 ? " ok;" : " COUNT " + n + ";");
        }
        return sb.toString();
    }

    private static String weg(MinecraftServer s, String f, double r, int max, boolean kill) {
        int n = 0;
        StringBuilder sb = new StringBuilder();
        for (Entity e : rond(s, bij(s), f, r)) {
            if (n >= max) {
                break;
            }
            sb.append(' ').append(fmt(e.position()));
            if (kill) {
                e.kill(level(s));
            } else {
                e.discard();
            }
            n++;
        }
        return "weg " + f + ": " + n + (kill ? " killed" : " removed") + " at" + sb;
    }

    // ------------------------------------------------------------------------------------------------ as the player

    private static String klik(MinecraftServer s) {
        ServerPlayer sp = player(s);
        ServerLevel level = level(s);
        if (plek == null) {
            return "!klik: no plek";
        }
        int boeken = dagboeken(sp);
        var hit = new BlockHitResult(Vec3.atCenterOf(plek), net.minecraft.core.Direction.UP, plek, false);
        var r = sp.gameMode.useItemOn(sp, level, sp.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        sp.closeContainer();
        return "klik " + plekNaam + " " + plek.toShortString() + " (" + naam(level, plek) + ", holding " + sp.getMainHandItem() + ") -> " + r
                + "; diaries " + boeken + " -> " + dagboeken(sp) + ", flag " + GuhQuests.saved(sp).getBooleanOr(VoorIedereen.VOORRAADBOEK, false);
    }

    private static String breek(MinecraftServer s) {
        ServerPlayer sp = player(s);
        ServerLevel level = level(s);
        if (plek == null) {
            return "!breek: no plek";
        }
        String voor = naam(level, plek);
        boolean r = sp.gameMode.destroyBlock(plek);
        return "breek " + plekNaam + " " + plek.toShortString() + " as " + sp.gameMode.getGameModeForPlayer() + ": destroyBlock " + r + ", block " + voor + " -> "
                + naam(level, plek) + "; Kermis.protectedAt " + Kermis.protectedAt(level, plek) + ", Kermis.denied " + Kermis.denied(sp, plek);
    }

    private static String sla(MinecraftServer s, String f) {
        ServerPlayer sp = player(s);
        Entity e = dichtst(s, f, 64);
        if (!(e instanceof LivingEntity l)) {
            return "!sla: no " + f;
        }
        float voor = l.getHealth();
        sp.resetAttackStrengthTicker();
        sp.attack(e);
        return "sla " + soort(e) + " as " + sp.gameMode.getGameModeForPlayer() + ": health " + voor + " -> " + l.getHealth() + ", alive " + l.isAlive()
                + ", invulnerable " + l.isInvulnerable();
    }

    private static String voer(MinecraftServer s, String f, int keer) {
        ServerPlayer sp = player(s);
        Entity e = dichtst(s, f, 64);
        if (e == null) {
            return "!voer: no " + f;
        }
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 32));
        StringBuilder sb = new StringBuilder("voer " + soort(e) + " at " + fmt(e.position()) + ":");
        for (int i = 0; i < keer && e.isAlive(); i++) {
            var r = sp.interactOn(e, InteractionHand.MAIN_HAND, e.getBoundingBox().getCenter().subtract(e.position()));
            sb.append(' ').append(r.getClass().getSimpleName());
        }
        sb.append("; knabbels left ").append(sp.getMainHandItem().getCount()).append("/32, now ").append(e.isAlive() ? soort(e) : "GONE");
        if (e instanceof GuhEntity g && g.getVariant() == GuhVariant.MAGER) {
            sb.append(", heeftGevoerd ").append(MagereCellen.heeftGevoerd(g, sp.getUUID())).append(", freed guhs running around ").append(MagereCellen.vrij().size());
        }
        sb.append(", gered ").append(GuhQuests.saved(sp).getIntOr("guhs_guheinde_gered", -1));
        return sb.toString();
    }

    private static String hier(MinecraftServer s) {
        ServerPlayer sp = player(s);
        ServerLevel level = sp.level();
        BlockPos p = sp.blockPosition();
        var pd = GuhWorldData.get(s).player(sp.getUUID());
        return "hier " + p.toShortString() + " in " + level.dimension().identifier() + " (" + sp.gameMode.getGameModeForPlayer() + ", tickCount " + sp.tickCount + ", id " + sp.getId() + "): pieces"
                + " knabbelkelder " + Terugkeer.stukken(level, MagereCellen.KELDER, p, "knabbelkelder").size()
                + ", zwevende_eilanden " + Terugkeer.stukken(level, EilandenFeature.ISLANDS, p, "zwevende_eilanden").size()
                + ", dungeon_hall " + Terugkeer.stukken(level, GuhCompassItem.CHALLENGING_GUH_CAVES, p, VoorIedereen.BIG_MIKA_HAL).size()
                + ", onderwater " + Terugkeer.stukken(level, OnderwaterProtection.BUBBLE, p, null).size()
                + ", voorraadkelder " + Terugkeer.stukken(level, GatenkaasFeature.VOORRAADKELDER, p, null).size()
                + "; stadje " + (PleinSlot.stadje(level, p) != null) + " (in a piece " + PleinSlot.inStadje(level, p) + ")"
                + "; kermis protectedAt " + Kermis.protectedAt(level, p)
                + "; entity ticking here " + level.isPositionEntityTicking(p)
                + "; beatBigMika " + pd.beatBigMika;
    }

    private static String aanvul(MinecraftServer s, int n) {
        ServerPlayer sp = player(s);
        ServerLevel level = sp.level();
        int muisjes = 0, dieren = 0;
        for (int i = 0; i < n; i++) {
            if (VoorIedereen.muisjeBij(sp, p -> VoorIedereen.inLiefGebouw(level, p)) != null) {
                muisjes++;
            }
            dieren += VoorIedereen.diertjesAanvullen(sp, sp.getRandom()).size();
        }
        return "aanvul " + n + " tries (no dice): " + muisjes + " muisjes and " + dieren + " land animals came; now within 128: "
                + level.getEntitiesOfClass(PieppiepmuisjeEntity.class, sp.getBoundingBox().inflate(128), m -> !m.isTame()).size() + " wild muisjes, "
                + level.getEntitiesOfClass(Landdiertje.class, sp.getBoundingBox().inflate(128), d -> !d.isTame() && VoorIedereen.isLanddiertje(d.getType())).size()
                + " wild land animals (" + level.getEntitiesOfClass(Landdiertje.class, sp.getBoundingBox().inflate(64), d -> !d.isTame() && VoorIedereen.isLanddiertje(d.getType())).size()
                + " within 64)";
    }

    // ------------------------------------------------------------------------------------------------ what it costs

    private static String perf(MinecraftServer s) {
        ServerPlayer sp = player(s);
        ServerLevel level = sp.level();
        BlockPos p = sp.blockPosition();
        StringBuilder sb = new StringBuilder("perf at " + p.toShortString() + " (microseconds per call):");
        sb.append(" 5 piece lookups (what every player pays per check) ").append(meet(2000, () -> {
            Terugkeer.stukken(level, MagereCellen.KELDER, p, "knabbelkelder");
            Terugkeer.stukken(level, EilandenFeature.ISLANDS, p, "zwevende_eilanden");
            Terugkeer.stukken(level, GuhCompassItem.CHALLENGING_GUH_CAVES, p, VoorIedereen.BIG_MIKA_HAL);
            PleinSlot.inStadje(level, p);
            Kermis.protectedAt(level, p);
        }));
        var kelder = Terugkeer.stukken(level, MagereCellen.KELDER, p, "knabbelkelder");
        if (!kelder.isEmpty()) {
            List<BlockPos> cellen = new ArrayList<>();
            for (BlockPos l : MagereCellen.CELLEN) {
                cellen.add(Terugkeer.wereld(kelder.get(0), l));
            }
            sb.append("; MagereCellen.controleer ").append(meet(300, () -> MagereCellen.controleer(level, cellen, sp.position())));
        }
        for (var st : Terugkeer.stukken(level, EilandenFeature.ISLANDS, p, "zwevende_eilanden")) {
            BoundingBox b = st.getBoundingBox();
            AABB box = new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(8);
            sb.append("; wolkTerug ").append(meet(300, () -> EilandenEvents.wolkTerug(level, Terugkeer.wereld(st, EilandenEvents.WOLK_PLEK), box)));
        }
        for (var st : Terugkeer.stukken(level, GuhCompassItem.CHALLENGING_GUH_CAVES, p, VoorIedereen.BIG_MIKA_HAL)) {
            BoundingBox b = st.getBoundingBox();
            AABB box = new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(48);
            sb.append("; bigMikaTerug ").append(meet(300, () -> VoorIedereen.bigMikaTerug(level, Terugkeer.wereld(st, VoorIedereen.BIG_MIKA_PLEK), box)));
        }
        StructureStart stad = PleinSlot.stadje(level, p);
        if (stad != null) {
            BoundingBox box = stad.getBoundingBox();
            AABB zoek = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1).inflate(Bewoners.ZOEK_RAND);
            sb.append("; Bewoners geladen+plekken+herstel ").append(meet(100, () -> {
                if (Bewoners.geladen(level, zoek)) {
                    Bewoners.herstel(level, Bewoners.plekken(level, stad), zoek);
                }
            }));
        }
        Kermis.Area area = Kermis.area(level, p);
        if (area != null) {
            sb.append("; Kermis.area+station (the Kermis-guh, every 5 s) ").append(meet(100, () -> Kermis.station(level, Kermis.area(level, p))));
        }
        sb.append("; land animal scan (64+128) ").append(meet(100, () -> {
            level.getEntitiesOfClass(Landdiertje.class, sp.getBoundingBox().inflate(64), d -> !d.isTame() && VoorIedereen.isLanddiertje(d.getType()));
            level.getEntitiesOfClass(Landdiertje.class, sp.getBoundingBox().inflate(128), d -> !d.isTame() && VoorIedereen.isLanddiertje(d.getType()));
        }));
        sb.append("; muisjes scan (128) ").append(meet(100, () -> level.getEntitiesOfClass(PieppiepmuisjeEntity.class, sp.getBoundingBox().inflate(128), m -> !m.isTame())));
        return sb.toString();
    }

    private static String meet(int n, Runnable r) {
        r.run();
        long t = System.nanoTime();
        for (int i = 0; i < n; i++) {
            r.run();
        }
        return String.format(Locale.ROOT, "%.1f", (System.nanoTime() - t) / 1000.0 / n);
    }
}
