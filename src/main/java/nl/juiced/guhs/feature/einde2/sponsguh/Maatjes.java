package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice sponsguh_wezens: the bookkeeping behind a player's own Sponsguh buddy and own Gerrit ({@link Sponsguh},
 * {@link Gerrit} are the doors). Per player and per kind, in the data that survives dying ({@code GuhQuests.saved}):
 * {@code guhs_einde2_sponsguh_wezens_<soort>} = the UUID of the ONE entity that is theirs (having the key = having one),
 * {@code ..._thuis} = it was told to stay.
 * <p>
 * Never lost: {@link #roep} brings the real one when it is loaded and makes a new one when it is not (it died with a
 * /kill, its chunk is far away, another dimension); the old copy then no longer matches the record and tidies itself up
 * when it meets its owner again ({@link EigenWezen#tick}). A buddy that follows comes along by itself when its owner
 * walked or travelled away from it ({@link #onPlayerTick}: asked twice, some seconds apart, and only where entities are
 * loaded, so a chunk that is still loading never makes a double).
 */
public final class Maatjes {
    private static final String KEY = "guhs_einde2_sponsguh_wezens_";
    /** Per player + kind: how many checks in a row found no loaded entity. */
    private static final Map<String, Integer> GEMIST = new ConcurrentHashMap<>();
    static final int CHECK_ELKE = 100;

    private Maatjes() {
    }

    static boolean heeft(ServerPlayer p, String soort) {
        return !GuhQuests.saved(p).getStringOr(KEY + soort, "").isEmpty();
    }

    @Nullable
    static UUID id(ServerPlayer p, String soort) {
        String s = GuhQuests.saved(p).getStringOr(KEY + soort, "");
        try {
            return s.isEmpty() ? null : UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static boolean isDeEchte(ServerPlayer p, String soort, UUID entity) {
        return entity.equals(id(p, soort));
    }

    static boolean thuis(ServerPlayer p, String soort) {
        return GuhQuests.saved(p).getBooleanOr(KEY + soort + "_thuis", false);
    }

    /** The loaded entity that is this player's (in any dimension), or null. */
    @Nullable
    static EigenWezen geladen(ServerPlayer p, String soort) {
        UUID id = id(p, soort);
        if (id == null) {
            return null;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            if (level.getEntity(id) instanceof EigenWezen e && e.isAlive()) {
                return e;
            }
        }
        return null;
    }

    /** A spot next to the player to stand on: beside them, else right where they stand (when someone asks for the buddy). */
    static BlockPos plekBij(ServerPlayer p) {
        BlockPos vast = vastePlekBij(p);
        return vast == null ? p.blockPosition() : vast;
    }

    /**
     * The same, but null when there is no floor at the player's own spot either (they fly: on an Appa-guh, a vetbootje,
     * wings): a buddy that comes along BY ITSELF waits for that, put down in the air it would fall into the void, again and
     * again.
     */
    @Nullable
    static BlockPos vastePlekBij(ServerPlayer p) {
        BlockPos hier = p.blockPosition();
        for (BlockPos bij : new BlockPos[] {hier.relative(p.getDirection().getClockWise()), hier.relative(p.getDirection().getCounterClockWise()),
                hier.relative(p.getDirection().getOpposite())}) {
            if (p.level().getBlockState(bij).getCollisionShape(p.level(), bij).isEmpty()
                    && p.level().getBlockState(bij.above()).getCollisionShape(p.level(), bij.above()).isEmpty() && vloer(p, bij)) {
                return bij;
            }
        }
        return vloer(p, hier) ? hier : null;
    }

    private static boolean vloer(ServerPlayer p, BlockPos pos) {
        return !p.level().getBlockState(pos.below()).getCollisionShape(p.level(), pos.below()).isEmpty();
    }

    /** Gives this player theirs, once: when they have one already, that one comes. */
    @Nullable
    static <T extends EigenWezen> T geef(ServerPlayer p, String soort, Supplier<EntityType<T>> type) {
        if (heeft(p, soort)) {
            return roep(p, soort, type);
        }
        T nieuw = maak(p, soort, type);
        if (nieuw != null) {
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_sponsguh_wezens." + soort + ".gekregen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return nieuw;
    }

    /** Brings the player's own one to them and lets it follow again (null: they have none). */
    @Nullable
    @SuppressWarnings("unchecked")
    static <T extends EigenWezen> T roep(ServerPlayer p, String soort, Supplier<EntityType<T>> type) {
        if (!heeft(p, soort)) {
            return null;
        }
        GEMIST.remove(p.getUUID() + soort);
        EigenWezen e = geladen(p, soort);
        if (e != null && e.level() == p.level() && e.getType() == type.get()) {
            BlockPos bij = plekBij(p);
            e.snapTo(bij.getX() + 0.5, bij.getY(), bij.getZ() + 0.5, p.getYRot(), 0);
            e.getNavigation().stop();
            e.zetThuis(false);
            GuhQuests.saved(p).putBoolean(KEY + soort + "_thuis", false);
            return (T) e;
        }
        if (e != null) {
            e.discard();                                                     // (in another dimension: a new one here)
        }
        return maak(p, soort, type);
    }

    @Nullable
    private static <T extends EigenWezen> T maak(ServerPlayer p, String soort, Supplier<EntityType<T>> type) {
        ServerLevel level = p.level();
        T e = type.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (e == null) {
            return null;
        }
        BlockPos bij = plekBij(p);
        e.snapTo(bij.getX() + 0.5, bij.getY(), bij.getZ() + 0.5, p.getYRot(), 0);
        e.zetEigenaar(p.getUUID());
        CompoundTag saved = GuhQuests.saved(p);
        saved.putString(KEY + soort, e.getUUID().toString());                // (first the record: the entity checks it)
        saved.putBoolean(KEY + soort + "_thuis", false);
        level.addFreshEntity(e);
        return e;
    }

    /** The owner's own one stays where it stands (true) or follows again (false). */
    static void zetThuis(ServerPlayer p, String soort, boolean thuis) {
        if (!heeft(p, soort)) {
            return;
        }
        GuhQuests.saved(p).putBoolean(KEY + soort + "_thuis", thuis);
        EigenWezen e = geladen(p, soort);
        if (e != null) {
            e.zetThuis(thuis);
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_sponsguh_wezens." + soort + (thuis ? ".blijf" : ".kom"), e.getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** (Tests) takes this player's record away again. */
    static void vergeet(ServerPlayer p, String soort) {
        Entity e = geladen(p, soort);
        if (e != null) {
            e.discard();
        }
        GuhQuests.saved(p).remove(KEY + soort);
        GuhQuests.saved(p).remove(KEY + soort + "_thuis");
        GEMIST.remove(p.getUUID() + soort);
    }

    /** A follower whose owner left it behind (walked far, another dimension, it is gone) comes along by itself. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || (p.tickCount + p.getId()) % CHECK_ELKE != 0 || p.isSpectator() || !p.isAlive()) {
            return;
        }
        kijk(p, "sponsguh", () -> E2SponsguhFeature.SPONSGUH.get());
        kijk(p, "gerrit", () -> E2SponsguhFeature.GERRIT.get());
    }

    private static <T extends EigenWezen> void kijk(ServerPlayer p, String soort, Supplier<EntityType<T>> type) {
        if (!heeft(p, soort) || thuis(p, soort)) {
            return;
        }
        String key = p.getUUID() + soort;
        EigenWezen e = geladen(p, soort);
        if ((e != null && e.level() == p.level()) || !p.level().areEntitiesLoaded(ChunkPos.pack(p.blockPosition()))) {
            GEMIST.remove(key);
            return;
        }
        if (vastePlekBij(p) == null) {
            return;                                                          // (the owner is in the air: it comes when they land)
        }
        if (GEMIST.merge(key, 1, Integer::sum) >= 2) {                       // (twice in a row: it really is not here)
            roep(p, soort, type);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GEMIST.remove(event.getEntity().getUUID() + "sponsguh");
        GEMIST.remove(event.getEntity().getUUID() + "gerrit");
    }
}
