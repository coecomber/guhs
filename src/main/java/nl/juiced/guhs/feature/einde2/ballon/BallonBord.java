package nl.juiced.guhs.feature.einde2.ballon;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.storage.Nbt;

/**
 * einde2 phase C, slice trap5_ballon: the progress board of the Vredesballon on the workshop's south wall
 * ({@code vredesballon_teller} = the free cell in front of it; bouw_lucht built the frame of 7 x 4 blocks).
 * <ul>
 *   <li>A right click on the board (any block of its frame) = {@link Vredesballon#klik}.</li>
 *   <li>Each player near it reads their OWN progress on it: a text display that only their game knows
 *       ({@link Zicht#alleenVoor}), made again when the text changes and taken away when they walk off. Never saved: a
 *       display a world brings back from disk is dropped at once.</li>
 *   <li>The first time a player who may begin walks onto the building site, the Werktuigguh calls them over (once per
 *       server run).</li>
 * </ul>
 */
public final class BallonBord {
    public static final String TAG = "guhs_einde2_trap5_ballon_bord";
    /** How near the board a player must be to get their display (blocks). */
    static final int BEREIK = 22;

    private record Bord(UUID display, String tekst) {
    }

    private static final Map<UUID, Bord> BORDEN = new ConcurrentHashMap<>();
    private static final Set<UUID> GEROEPEN = ConcurrentHashMap.newKeySet();

    private BallonBord() {
    }

    /** Is this block part of the board (the frame round the teller's wall, 7 wide, 4 high)? */
    static boolean isBord(BlockPos teller, BlockPos pos) {
        return pos.getZ() == teller.getZ() - 1 && Math.abs(pos.getX() - teller.getX()) <= 3 && pos.getY() >= teller.getY()
                && pos.getY() <= teller.getY() + 3;
    }

    static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockPos teller = Vredesballon.plek(p.level(), Vredesballon.TELLER);
        if (teller == null || !isBord(teller, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        long nu = p.level().getGameTime();
        if (nu - p.getPersistentData().getLongOr("guhs_einde2_trap5_ballon_klik", 0L) < 8) {
            return;
        }
        p.getPersistentData().putLong("guhs_einde2_trap5_ballon_klik", nu);
        Vredesballon.klik(p);
    }

    /** A display of ours that a world brings back from disk is not wanted: they are made fresh for whoever is near. */
    static void opJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity().entityTags().contains(TAG)) {
            event.setCanceled(true);
        }
    }

    // =====================================================================================================================
    // the display of each player
    // =====================================================================================================================

    /** Every 20 ticks per player in the Guheinde: the display there when near, its text up to date; away when far. */
    static void tick(ServerPlayer p) {
        BlockPos teller = Vredesballon.plek(p.level(), Vredesballon.TELLER);
        if (teller == null || teller.distSqr(p.blockPosition()) > BEREIK * BEREIK) {
            weg(p);
            return;
        }
        roep(p);
        ververs(p);
    }

    /** Makes the player's display again when their text changed (right after a click, and from tick). */
    static void ververs(ServerPlayer p) {
        BlockPos teller = Vredesballon.plek(p.level(), Vredesballon.TELLER);
        if (teller == null || teller.distSqr(p.blockPosition()) > BEREIK * BEREIK) {
            return;
        }
        Component tekst = tekst(p);
        String sleutel = tekst.toString();
        Bord had = BORDEN.get(p.getUUID());
        if (had != null && had.tekst().equals(sleutel) && p.level().getEntity(had.display()) != null) {
            return;
        }
        weg(p);
        Display.TextDisplay d = maak(p.level(), teller, tekst);
        if (d != null) {
            Zicht.alleenVoor(d, p.getUUID());
            p.level().addFreshEntity(d);
            BORDEN.put(p.getUUID(), new Bord(d.getUUID(), sleutel));
        }
    }

    /** Takes the player's display away. */
    static void weg(ServerPlayer p) {
        Bord had = BORDEN.remove(p.getUUID());
        if (had == null || p.level().getServer() == null) {
            return;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            Entity e = level.getEntity(had.display());
            if (e != null) {
                e.discard();
                return;
            }
        }
    }

    /** The first time (this server run) a player who may begin steps onto the building site, the Werktuigguh calls. */
    private static void roep(ServerPlayer p) {
        if (Vredesballon.LIJN.stap(p) != 0 || GEROEPEN.contains(p.getUUID()) || !Vredesballon.magBeginnen(p)) {
            return;
        }
        Plekken.Plek zone = Plekken.plek(Vredesballon.STRUCTUUR, Vredesballon.BOUWPLAATS);
        BlockPos o = Plekken.oorsprong(p.level(), Vredesballon.STRUCTUUR);
        if (zone == null || zone.tot() == null || o == null) {
            return;
        }
        BlockPos a = o.offset(zone.pos()), b = o.offset(zone.tot());
        BlockPos q = p.blockPosition();
        if (q.getX() >= Math.min(a.getX(), b.getX()) && q.getX() <= Math.max(a.getX(), b.getX()) && q.getZ() >= Math.min(a.getZ(), b.getZ())
                && q.getZ() <= Math.max(a.getZ(), b.getZ()) && q.getY() >= Math.min(a.getY(), b.getY()) - 2 && q.getY() <= Math.max(a.getY(), b.getY())) {
            GEROEPEN.add(p.getUUID());
            Vredesballon.zeg(p, "wg.roep");
        }
    }

    /** What the board says to this player. */
    static Component tekst(ServerPlayer p) {
        String t = Vredesballon.T + "bord.";
        MutableComponent uit = Component.translatable(t + "titel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        int stap = Vredesballon.LIJN.stap(p);
        if (stap == 0) {
            regel(uit, Component.translatable(t + (Vredesballon.magBeginnen(p) ? "begin" : "dicht")).withStyle(ChatFormatting.WHITE));
        } else if (stap <= Vredesballon.AANTAL_FASEN) {
            regel(uit, Component.translatable(t + "fase", stap, Component.translatable(Vredesballon.T + "fase." + stap)).withStyle(ChatFormatting.YELLOW));
            for (Vredesballon.Levering l : Vredesballon.leveringen(stap)) {
                int heeft = Vredesballon.geleverd(p, stap, l);
                regel(uit, Component.translatable(t + "regel", Component.translatable(l.tekst()), heeft, l.aantal())
                        .withStyle(heeft >= l.aantal() ? ChatFormatting.GREEN : ChatFormatting.WHITE));
            }
        } else if (stap == Vredesballon.KLAAR) {
            regel(uit, Component.translatable(t + "klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            regel(uit, Component.translatable(t + "af").withStyle(ChatFormatting.WHITE));
            regel(uit, Component.translatable(t + (Vredesballon.zakjeKlaar(p) ? "zakje" : "morgen"))
                    .withStyle(Vredesballon.zakjeKlaar(p) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        return uit;
    }

    private static void regel(MutableComponent uit, Component regel) {
        uit.append(Component.literal("\n")).append(regel);
    }

    @Nullable
    private static Display.TextDisplay maak(ServerLevel level, BlockPos teller, Component tekst) {
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("text", ComponentSerialization.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), tekst).getOrThrow());
        tag.putString("billboard", "vertical");
        tag.putInt("background", 0x90241A12);
        tag.putInt("line_width", 220);
        CompoundTag transform = new CompoundTag();
        transform.put("translation", floats(0, 0, 0));
        transform.put("left_rotation", floats(0, 0, 0, 1));
        transform.put("scale", floats(0.62f, 0.62f, 0.62f));
        transform.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", transform);
        Nbt.load(d, tag);
        // in front of the board's face (the wall is the block north of the teller), a little above the teller
        d.snapTo(teller.getX() + 0.5, teller.getY() + 0.95, teller.getZ() + 0.25, 0, 0);
        d.addTag(TAG);
        return d;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float v : values) {
            list.add(FloatTag.valueOf(v));
        }
        return list;
    }

    static void vergeet(ServerPlayer p) {
        weg(p);
        GEROEPEN.remove(p.getUUID());
    }
}
