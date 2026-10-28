package nl.juiced.guhs.feature.droomspul;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.vadswoud.VadswoudFeature;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The islanders of Guhdromenland: the sleeping guhs in the nestjes, the Blubs in the holletjes of the Slaapliedjesheuvels
 * and the Postguh in the Droomposthuisje. Entities inside an island template skip their spawn setup (a Blub would get meter
 * 0, a guh no variant), so they are put there by code instead, from {@code data/guhs/dromen/bewoners/<eiland>.json}
 * (tools/features/droomspul.py reads the templates for their nestjes):
 * <pre>
 * {"versie": 1, "bewoners": [{"soort": "guh_slaapt" | "blub" | "postguh", "plek": [x, y, z], "yaw": 180}, ...]}
 * </pre>
 * Spots are relative to the island's centre block, like the island json's own spots ({@link DroomEilanden#wereld}).
 * <p>
 * When the list or the island's stamped version changes, the old islanders go and the new ones come (all at once, once the
 * island is loaded); after that, a missing one (wandered off, /kill) comes back now and then, one per island per round.
 */
public final class DroomBewoners {
    private static final Logger LOG = LogUtils.getLogger();
    /** Every islander carries this tag; its persistent data says which one it is ("eiland#nummer"). */
    public static final String TAG = "guhs_droombewoner", ID = "guhs_droombewoner_id";
    private static final String NEST_X = "guhs_droombewoner_x", NEST_Y = "guhs_droombewoner_y", NEST_Z = "guhs_droombewoner_z";
    /** Ticks between two rounds, and between two refills of a missing one. */
    static final int RONDE = 40, AANVUL = 20 * 30;
    /** How close a player must be to an island before its islanders are looked after. */
    static final double BEREIK = 112;

    public record Bewoner(String soort, Vec3 plek, float yaw) {
    }

    public record Lijst(int versie, List<Bewoner> bewoners) {
        static final Lijst LEEG = new Lijst(0, List.of());
    }

    private static final Map<String, Lijst> CACHE = new HashMap<>();
    private static final Map<String, Long> LAATST_AANGEVULD = new HashMap<>();

    private DroomBewoners() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(DroomBewoners::tick);
        NeoForge.EVENT_BUS.addListener(DroomBewoners::onKlik);
    }

    // =====================================================================================================================
    // the lists
    // =====================================================================================================================

    public static synchronized Lijst lijst(String eiland) {
        return CACHE.computeIfAbsent(eiland, DroomBewoners::lees);
    }

    private static Lijst lees(String eiland) {
        String pad = "/data/guhs/dromen/bewoners/" + eiland + ".json";
        try (InputStream in = DroomBewoners.class.getResourceAsStream(pad)) {
            if (in != null) {
                return lees(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        } catch (Exception e) {
            LOG.error("Guhdromenland: {} is broken, nobody lives on {}: {}", pad, eiland, e.toString());
        }
        return Lijst.LEEG;
    }

    public static Lijst lees(JsonObject o) {
        List<Bewoner> lijst = new ArrayList<>();
        for (JsonElement el : o.getAsJsonArray("bewoners")) {
            JsonObject b = el.getAsJsonObject();
            JsonArray p = b.getAsJsonArray("plek");
            lijst.add(new Bewoner(b.get("soort").getAsString(), new Vec3(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble()),
                    b.has("yaw") ? b.get("yaw").getAsFloat() : 0f));
        }
        return new Lijst(o.has("versie") ? o.get("versie").getAsInt() : 1, List.copyOf(lijst));
    }

    // =====================================================================================================================
    // what stands (per island: the list version x 1000 + the island's stamped version)
    // =====================================================================================================================

    public static final class Stand extends SavedData {
        public static final SavedDataType<Stand> TYPE = GuhSavedData.tagType("dromen_bewoners", Stand::new, Stand::load, Stand::save);
        final Map<String, Integer> sleutels = new HashMap<>();

        private static Stand load(CompoundTag tag) {
            Stand s = new Stand();
            for (String k : tag.keySet()) {
                s.sleutels.put(k, tag.getIntOr(k, -1));
            }
            return s;
        }

        private CompoundTag save() {
            CompoundTag t = new CompoundTag();
            sleutels.forEach(t::putInt);
            return t;
        }
    }

    private static Stand stand(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Stand.TYPE);
    }

    static int sleutel(ServerLevel level, String eiland) {
        return lijst(eiland).versie() * 1000 + DroomEilanden.gebouwd(level, eiland);
    }

    // =====================================================================================================================
    // looking after them
    // =====================================================================================================================

    private static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % RONDE != 13) {
            return;
        }
        ServerLevel level = server.getLevel(DromenFeature.DIM);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        for (String eiland : DroomEilanden.IDS) {
            zorg(level, eiland, server.getTickCount());
        }
    }

    /** One round for one island: everybody there (after a change), or one missing one back now and then; sleepers sleep. */
    static void zorg(ServerLevel level, String eiland, long nu) {
        Lijst lijst = lijst(eiland);
        if (lijst.bewoners().isEmpty() || DroomEilanden.gebouwd(level, eiland) < 1 || !dichtbij(level, eiland)) {
            return;
        }
        for (Bewoner b : lijst.bewoners()) {
            if (!level.isLoaded(BlockPos.containing(DroomEilanden.wereld(eiland, b.plek())))) {
                return;                                             // wait until the whole island is loaded
            }
        }
        Stand s = stand(level);
        int sleutel = sleutel(level, eiland);
        Map<Integer, Entity> er = aanwezig(level, eiland);
        if (s.sleutels.getOrDefault(eiland, -1) != sleutel) {
            er.values().forEach(Entity::discard);
            for (int i = 0; i < lijst.bewoners().size(); i++) {
                zet(level, eiland, i, lijst.bewoners().get(i));
            }
            s.sleutels.put(eiland, sleutel);
            s.setDirty();
            LOG.info("Guhdromenland: {} islanders on {} (list {}, island {})", lijst.bewoners().size(), eiland, lijst.versie(),
                    DroomEilanden.gebouwd(level, eiland));
            return;
        }
        if (nu - LAATST_AANGEVULD.getOrDefault(eiland, -AANVUL * 2L) >= AANVUL) {
            for (int i = 0; i < lijst.bewoners().size(); i++) {
                if (!er.containsKey(i)) {
                    zet(level, eiland, i, lijst.bewoners().get(i));
                    LAATST_AANGEVULD.put(eiland, nu);
                    break;
                }
            }
        }
        for (Map.Entry<Integer, Entity> e : er.entrySet()) {   // the sleepers keep sleeping, with a zzz now and then
            if (e.getValue() instanceof GuhEntity guh && e.getKey() < lijst.bewoners().size()
                    && lijst.bewoners().get(e.getKey()).soort().equals("guh_slaapt")) {
                slaap(guh);
                if (level.getRandom().nextInt(3) == 0) {
                    level.sendParticles(VadswoudFeature.GUH_ZZZ.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(),
                            1, 0.1, 0.05, 0.1, 0.0);
                }
            }
        }
    }

    private static boolean dichtbij(ServerLevel level, String eiland) {
        Vec3 m = Vec3.atCenterOf(DroomEilanden.midden(eiland));
        for (ServerPlayer p : level.players()) {
            double dx = p.getX() - m.x, dz = p.getZ() - m.z;
            if (dx * dx + dz * dz < BEREIK * BEREIK) {
                return true;
            }
        }
        return false;
    }

    /** The island's box (as DroomEilanden clears it). */
    static AABB doos(String eiland) {
        BlockPos m = DroomEilanden.midden(eiland);
        var maat = DroomEilanden.MAX_MAAT;
        return new AABB(m.getX() - maat.getX() / 2.0, m.getY() - maat.getY() / 2.0, m.getZ() - maat.getZ() / 2.0,
                m.getX() + maat.getX() / 2.0, m.getY() + maat.getY() / 2.0, m.getZ() + maat.getZ() / 2.0);
    }

    /** The islanders of this island that are there now, by their number. */
    static Map<Integer, Entity> aanwezig(ServerLevel level, String eiland) {
        Map<Integer, Entity> er = new HashMap<>();
        String voor = eiland + "#";
        for (Entity e : level.getEntities((Entity) null, doos(eiland).inflate(0, 64, 0), e -> e.entityTags().contains(TAG))) {
            String id = e.getPersistentData().getStringOr(ID, "");
            if (!id.startsWith(voor)) {
                continue;
            }
            int n;
            try {
                n = Integer.parseInt(id.substring(voor.length()));
            } catch (NumberFormatException x) {
                continue;
            }
            if (er.containsKey(n)) {
                e.discard();                                        // never two of the same
            } else {
                er.put(n, e);
            }
        }
        return er;
    }

    /** Puts islander number i of this island at its spot. */
    @Nullable
    static Entity zet(ServerLevel level, String eiland, int i, Bewoner b) {
        return zet(level, eiland, i, b, DroomEilanden.wereld(eiland, b.plek()));
    }

    /** Puts islander number i of this island at this world spot (the tests use their own spot). */
    @Nullable
    static Entity zet(ServerLevel level, String eiland, int i, Bewoner b, Vec3 w) {
        BlockPos pos = BlockPos.containing(w);
        Entity e = switch (b.soort()) {
            case "guh_slaapt" -> {
                GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.STRUCTURE);
                if (guh == null) {
                    yield null;
                }
                guh.snapTo(w.x, w.y, w.z, b.yaw(), 0f);
                guh.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
                // it sleeps in its nestje and stays there (no NoAI: an emote needs a guh with its AI; it just can't walk)
                var snelheid = guh.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                if (snelheid != null) {
                    snelheid.setBaseValue(0);
                }
                guh.getPersistentData().putDouble(NEST_X, w.x);
                guh.getPersistentData().putDouble(NEST_Y, w.y);
                guh.getPersistentData().putDouble(NEST_Z, w.z);
                yield guh;
            }
            case "blub" -> {
                BlubGuhEntity blub = BlubFeature.BLUBGUH.get().create(level, EntitySpawnReason.STRUCTURE);
                if (blub == null) {
                    yield null;
                }
                blub.snapTo(w.x, w.y, w.z, b.yaw(), 0f);
                blub.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);  // the real draw
                yield blub;
            }
            case "postguh" -> {
                GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
                if (npc == null) {
                    yield null;
                }
                npc.setKind(GuhNpcEntity.Kind.POSTGUH);
                npc.snapTo(w.x, w.y, w.z, b.yaw(), 0f);
                npc.yHeadRot = b.yaw();
                npc.yBodyRot = b.yaw();
                npc.setCustomName(Component.translatable("entity.guhs.droompostguh"));
                yield npc;
            }
            default -> null;
        };
        if (e == null) {
            LOG.warn("Guhdromenland: unknown islander {} on {}", b.soort(), eiland);
            return null;
        }
        if (e instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        e.addTag(TAG);
        e.getPersistentData().putString(ID, eiland + "#" + i);
        level.addFreshEntity(e);
        if (e instanceof GuhEntity guh && b.soort().equals("guh_slaapt")) {
            slaap(guh);
        }
        return e;
    }

    /** Keeps a sleeping islander asleep in its nestje (the emote can only start once it stands on something). */
    static void slaap(GuhEntity guh) {
        if (guh.getPersistentData().contains(NEST_X)) {             // back in its nestje if anything nudged it
            Vec3 nest = new Vec3(guh.getPersistentData().getDoubleOr(NEST_X, guh.getX()), guh.getPersistentData().getDoubleOr(NEST_Y, guh.getY()),
                    guh.getPersistentData().getDoubleOr(NEST_Z, guh.getZ()));
            if (guh.position().distanceToSqr(nest) > 0.25) {
                guh.teleportTo(nest.x, nest.y, nest.z);
            }
        }
        if (guh.emotes.current() != Emote.SLAPEN) {
            guh.emotes.start(Emote.SLAPEN, true, GuhEmotes.Source.SELF);
        }
    }

    /** Is this one of the islanders? */
    public static boolean isBewoner(Entity e) {
        return e.entityTags().contains(TAG);
    }

    // =====================================================================================================================
    // the Droompostguh talks about dream post (not about the post work of his namesakes elsewhere)
    // =====================================================================================================================

    static final int POST_GRAPPEN = 4;

    private static void onKlik(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof GuhNpcEntity npc) || !isBewoner(npc) || npc.getKind() != GuhNpcEntity.Kind.POSTGUH) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer p && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            int n = npc.getPersistentData().getIntOr("guhs_droompost_grap", 0);
            npc.getPersistentData().putInt("guhs_droompost_grap", (n + 1) % POST_GRAPPEN);
            p.sendSystemMessage(Component.translatable("entity.guhs.droompostguh").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("gui.guhs.droomspul.postguh." + n).withStyle(ChatFormatting.WHITE)));
        }
    }
}
