package nl.juiced.guhs.feature.vadsrijk.wereld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * Temple doors as moving blocks (design ronde 6: "deuren van 20-40 hoog die als bewegende blokken openschuiven").
 * <p>
 * A door is a flat box of blocks in the map ({@link WereldPlekken#DEUREN}: the Knuffeldeur of the ordetempel in Zwamzoet, the
 * outer and the inner door of the Zwamtempel). Its two wings slide sideways into the wall, one column per step, with dust and
 * a grinding rumble, and slide shut again after {@link #OPEN_TICKS} (not while someone stands in the doorway).
 * <p>
 * The closed door is also stored (by the map generator) as a hidden copy deep under the ground ({@code kopieY}), so the door
 * can always be rebuilt exactly, also after a restart in the middle of opening. Nothing is saved.
 * <p>
 * Hook API for the npc/bazen slices:
 * <ul>
 *   <li>{@link #open(String, ServerPlayer)}: unlock for this player and open; the first time a non-click ceremony scene;</li>
 *   <li>{@link #ontgrendel}/{@link #ontgrendeld}: per player (SpelerData.extra("wereld").deur_&lt;id&gt;); an unlocked player opens
 *       the door by right-clicking it;</li>
 *   <li>{@link #openZonderScene}/{@link #sluit}/{@link #isOpen}: plain moves (a boss fight can keep its arena door shut).</li>
 * </ul>
 * Door ids: {@code ordetempel}, {@code zwamtempel}, {@code zwamtempel_binnen}.
 */
public final class WereldDeuren {
    public static final int OPEN_TICKS = 20 * 30, STAP_TICKS = 5;
    private static final Map<String, Staat> STAAT = new HashMap<>();
    /** [zone 2] Doors of other zones' maps (zone2.wereld adds the Spiegelburcht door); searched after {@link WereldPlekken#DEUREN}. */
    public static final List<WereldPlekken.Deur> EXTRA = new java.util.ArrayList<>();

    private static final class Staat {
        int stap, doel;
        long sluitOp = Long.MAX_VALUE, volgendeStap;
        boolean vast;    // kept shut by code (sluit(id, true))
    }

    private WereldDeuren() {
    }

    @Nullable
    public static WereldPlekken.Deur deur(String id) {
        for (WereldPlekken.Deur d : alle()) {
            if (d.id().equals(id)) {
                return d;
            }
        }
        return null;
    }

    public static List<WereldPlekken.Deur> alle() {
        if (EXTRA.isEmpty()) {
            return WereldPlekken.DEUREN;
        }
        List<WereldPlekken.Deur> l = new java.util.ArrayList<>(WereldPlekken.DEUREN);   // [zone 2]
        l.addAll(EXTRA);
        return l;
    }

    // ------------------------------------------------------------------------------------------------ the API

    /** Unlock this door for the player and open it; the first time with the ceremony scene. */
    public static boolean open(String id, ServerPlayer p) {
        WereldPlekken.Deur d = deur(id);
        if (d == null || !(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
            return false;
        }
        ontgrendel(id, p);
        SpelerData data = Spelers.data(p);
        String sleutel = "vr_w_deur_" + id;
        if (!data.gezien.contains(sleutel)) {
            data.gezien.add(sleutel);
            ceremonie(p, d);
            Later.na(30, () -> openZonderScene(sl, id));
        } else {
            openZonderScene(sl, id);
        }
        return true;
    }

    public static void ontgrendel(String id, ServerPlayer p) {
        Spelers.data(p).extra("wereld").putBoolean("deur_" + id, true);
    }

    public static boolean ontgrendeld(String id, ServerPlayer p) {
        return Spelers.data(p).extra("wereld").getBooleanOr("deur_" + id, false);
    }

    public static void openZonderScene(ServerLevel sl, String id) {
        WereldPlekken.Deur d = deur(id);
        if (d == null) {
            return;
        }
        Staat s = STAAT.computeIfAbsent(id, k -> new Staat());
        s.vast = false;
        s.doel = maxStap(d);
        s.sluitOp = sl.getGameTime() + OPEN_TICKS;
        sl.playSound(null, d.voor(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 2f, 0.45f);
        sl.playSound(null, d.voor(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.4f, 0.6f);
    }

    /** Slide shut now; {@code vast}: stays shut until {@link #openZonderScene} (e.g. during a boss fight). */
    public static void sluit(ServerLevel sl, String id, boolean vast) {
        WereldPlekken.Deur d = deur(id);
        if (d == null) {
            return;
        }
        Staat s = STAAT.computeIfAbsent(id, k -> new Staat());
        s.doel = 0;
        s.vast = vast;
        s.sluitOp = Long.MAX_VALUE;
    }

    /** For the bazen slice (it calls these by name): open without a scene / slide shut. */
    public static void open(ServerLevel sl, String id) {
        openZonderScene(sl, id);
    }

    public static void sluit(ServerLevel sl, String id) {
        sluit(sl, id, false);
    }

    public static boolean isOpen(String id) {
        Staat s = STAAT.get(id);
        return s != null && s.stap > 0;
    }

    /** Which door is this block part of (or null)? */
    @Nullable
    public static WereldPlekken.Deur bij(BlockPos p) {
        for (WereldPlekken.Deur d : alle()) {
            if (p.getX() >= d.min().getX() - 1 && p.getX() <= d.max().getX() + 1 && p.getY() >= d.min().getY() && p.getY() <= d.max().getY()
                    && p.getZ() >= d.min().getZ() - 1 && p.getZ() <= d.max().getZ() + 1) {
                return d;
            }
        }
        return null;
    }

    /** Right-click on a door: an unlocked player opens it; otherwise a little "it's shut" line. */
    static boolean klik(ServerPlayer p, BlockPos pos) {
        WereldPlekken.Deur d = bij(pos);
        if (d == null) {
            return false;
        }
        Staat s = STAAT.get(d.id());
        if (s != null && s.vast) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.deur.vast").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (ontgrendeld(d.id(), p) || p.getAbilities().instabuild) {
            open(d.id(), p);
        } else {
            p.sendOverlayMessage(Component.translatable("vadsrijk.wereld.deur.dicht." + d.id()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------ the scene

    private static void ceremonie(ServerPlayer p, WereldPlekken.Deur d) {
        Vec3 mid = new Vec3((d.min().getX() + d.max().getX() + 1) / 2.0, (d.min().getY() + d.max().getY() + 1) / 2.0, (d.min().getZ() + d.max().getZ() + 1) / 2.0);
        Vec3 voor = Vec3.atBottomCenterOf(d.voor());
        Vec3 uit = voor.subtract(mid.x, voor.y, mid.z).multiply(1, 0, 1).normalize();
        double h = d.max().getY() - d.min().getY() + 1;
        RijkScene s = RijkScene.maak("vr_w_deur_" + d.id(), 150)
                .camera(0, voor.add(uit.scale(h * 0.9)).add(0, 2, 0), mid.add(0, h * 0.25, 0))
                .camera(60, voor.add(uit.scale(h * 0.6)).add(0, 1.2, 0), mid.add(0, h * 0.1, 0))
                .camera(148, voor.add(uit.scale(2)).add(0, 1.6, 0), mid.subtract(uit.scale(8)))
                .titel("vadsrijk.wereld.deur.titel." + d.id(), "vadsrijk.wereld.deur.onder." + d.id(), 70, 70, 0xFFD27F)
                .bouw();
        RijkScenes.speel(p, s);
    }

    // ------------------------------------------------------------------------------------------------ moving

    private static int breedte(WereldPlekken.Deur d) {
        return d.langsX() ? d.max().getX() - d.min().getX() + 1 : d.max().getZ() - d.min().getZ() + 1;
    }

    private static int maxStap(WereldPlekken.Deur d) {
        return (breedte(d) + 1) / 2;
    }

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || STAAT.isEmpty()) {
            return;
        }
        long nu = sl.getGameTime();
        for (Map.Entry<String, Staat> e : STAAT.entrySet()) {
            WereldPlekken.Deur d = deur(e.getKey());
            Staat s = e.getValue();
            if (d == null || !sl.isLoaded(d.min()) || !sl.isLoaded(d.max())) {
                continue;
            }
            if (nu >= s.sluitOp && s.doel > 0) {
                AABB gat = new AABB(d.min().getX() - 1, d.min().getY(), d.min().getZ() - 1, d.max().getX() + 2, d.max().getY() + 1, d.max().getZ() + 2);
                if (sl.getEntitiesOfClass(ServerPlayer.class, gat).isEmpty()) {
                    s.doel = 0;
                    sl.playSound(null, d.voor(), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 2f, 0.45f);
                } else {
                    s.sluitOp = nu + 100;
                }
            }
            if (s.stap != s.doel && nu >= s.volgendeStap) {
                s.stap += Integer.signum(s.doel - s.stap);
                s.volgendeStap = nu + STAP_TICKS;
                zet(sl, d, s.stap);
            }
        }
    }

    /**
     * Rebuild the door with both wings slid {@code stap} columns outward. Column c of the closed door (from the hidden copy)
     * shows at c - stap (left wing) or c + stap (right wing); what slides past the opening is hidden in the wall.
     */
    static void zet(ServerLevel sl, WereldPlekken.Deur d, int stap) {
        int w = breedte(d), mid = w / 2;
        int dy = d.kopieY() - d.min().getY();
        for (int y = d.min().getY(); y <= d.max().getY(); y++) {
            for (int dikte = 0; dikte <= (d.langsX() ? d.max().getZ() - d.min().getZ() : d.max().getX() - d.min().getX()); dikte++) {
                for (int u = 0; u < w; u++) {
                    int bron = u < mid ? u + stap : u - stap;
                    boolean geldig = u < mid ? bron < mid : bron >= mid;
                    BlockPos doel = pos(d, u, y, dikte);
                    BlockState st = geldig && bron >= 0 && bron < w ? sl.getBlockState(pos(d, bron, y + dy, dikte)) : Blocks.AIR.defaultBlockState();
                    if (sl.getBlockState(doel) != st) {
                        sl.setBlock(doel, st, 2);
                    }
                }
            }
        }
        // dust along the seams and at the foot
        BlockPos naad = pos(d, Math.max(0, mid - stap), d.min().getY(), 0);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PINK_CONCRETE.defaultBlockState()), naad.getX() + 0.5, naad.getY() + 1,
                naad.getZ() + 0.5, 12, 0.3, (d.max().getY() - d.min().getY()) * 0.35, 0.3, 0.05);
        sl.sendParticles(ParticleTypes.CLOUD, Vec3.atBottomCenterOf(d.voor()).x, d.min().getY() + 0.2, Vec3.atBottomCenterOf(d.voor()).z,
                6, w * 0.3, 0.1, 0.6, 0.01);
        sl.playSound(null, naad, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7f, 0.5f);
    }

    private static BlockPos pos(WereldPlekken.Deur d, int u, int y, int dikte) {
        return d.langsX() ? new BlockPos(d.min().getX() + u, y, d.min().getZ() + dikte) : new BlockPos(d.min().getX() + dikte, y, d.min().getZ() + u);
    }

    static void wis() {
        STAAT.clear();
    }
}
