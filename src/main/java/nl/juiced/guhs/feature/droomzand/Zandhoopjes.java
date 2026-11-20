package nl.juiced.guhs.feature.droomzand;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomReis;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.10.10: the zandhoopjes of Guhdromenland, three per dream island. Their spots are in {@code data/guhs/dromen/zandhoopjes.json}
 * ({@code {"eilanden": {"<island>": [[dx, dy, dz], ...]}}}, block offsets from the island's centre spot; made by
 * tools/features/economie1110.py, which picks open, reachable ground from the island templates). Once a second while somebody
 * dreams, a heap is put on every free (air) spot of an island that stands in its real template version, so a re-stamped
 * island gets them back by itself.
 * <p>
 * Scooping: {@value #MIN}-{@value #MAX} droomzand, once per heap per dream ({@link DroomReis#sessie}); kept in
 * {@code GuhQuests.saved(player).guhs_zandhoopjes} = {Sessie: n, heap id: true, ...}, cleared when a new dream starts.
 */
public final class Zandhoopjes {
    public static final int MIN = 3, MAX = 5;
    /** How near you must be to see the sparkles of a heap you can still scoop. */
    static final double GLITTER_BIJ = 28;
    static final String KEY = "guhs_zandhoopjes", SESSIE = "Sessie";

    /** One heap: its island and its offset from the island's centre spot. */
    public record Hoopje(String id, String eiland, BlockPos rel) {
        public BlockPos wereld() {
            return DroomEilanden.midden(eiland).offset(rel);
        }
    }

    private static volatile List<Hoopje> alle;
    private static volatile Map<BlockPos, Hoopje> opPlek;

    private Zandhoopjes() {
    }

    static void register() {
        nl.juiced.guhs.world.TikWacht.post("Zandhoopjes.tick", Zandhoopjes::tick);
    }

    /** Every heap of every island (from the json). */
    public static List<Hoopje> alle() {
        List<Hoopje> a = alle;
        if (a == null) {
            List<Hoopje> lijst = new ArrayList<>();
            try (InputStream in = Zandhoopjes.class.getResourceAsStream("/data/guhs/dromen/zandhoopjes.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (var e : o.getAsJsonObject("eilanden").entrySet()) {
                        int i = 0;
                        for (JsonElement p : e.getValue().getAsJsonArray()) {
                            var xyz = p.getAsJsonArray();
                            lijst.add(new Hoopje(e.getKey() + "_" + i++, e.getKey(),
                                    new BlockPos(xyz.get(0).getAsInt(), xyz.get(1).getAsInt(), xyz.get(2).getAsInt())));
                        }
                    }
                }
            } catch (Exception ex) {
                LogUtils.getLogger().error("Zandhoopjes: data/guhs/dromen/zandhoopjes.json unreadable: {}", ex.toString());
            }
            a = List.copyOf(lijst);
            alle = a;
        }
        return a;
    }

    /** The heap on this world spot, or null (a heap somebody placed somewhere else). */
    @Nullable
    public static Hoopje op(BlockPos pos) {
        Map<BlockPos, Hoopje> m = opPlek;
        if (m == null) {
            m = new HashMap<>();
            for (Hoopje h : alle()) {
                m.put(h.wereld(), h);
            }
            opPlek = m;
        }
        return m.get(pos);
    }

    /** The id a heap on this spot is remembered by. */
    static String id(BlockPos pos) {
        Hoopje h = op(pos);
        return h != null ? h.id() : "plek_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }

    /** A right-click on a heap. */
    static void klik(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        if (!DroomReis.inDroom(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.zandhoopje.geen_droom").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        int n = schep(p, id(pos), DroomReis.sessie(p));
        if (n <= 0) {
            level.playSound(null, pos, SoundEvents.SAND_STEP, SoundSource.BLOCKS, 0.5f, 1.2f);
            p.sendOverlayMessage(Component.translatable("gui.guhs.zandhoopje.al").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.8f, 1.3f);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.05);
        level.sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.0);
        p.sendOverlayMessage(Component.translatable("gui.guhs.zandhoopje.geschept", n).withStyle(ChatFormatting.YELLOW));
    }

    /** Scoops heap {@code id} in dream {@code sessie}: the droomzand given ({@value #MIN}-{@value #MAX}), 0 when already scooped. */
    public static int schep(ServerPlayer p, String id, long sessie) {
        CompoundTag t = stand(p, sessie);
        if (t.getBooleanOr(id, false)) {
            return 0;
        }
        t.putBoolean(id, true);
        int n = MIN + p.getRandom().nextInt(MAX - MIN + 1);
        Minigames.give(p, new ItemStack(DroomblokkenFeature.DROOMZAND.get(), n));
        return n;
    }

    /** Did this player scoop heap {@code id} in dream {@code sessie} already? */
    public static boolean geschept(ServerPlayer p, String id, long sessie) {
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(KEY);
        return t.getLongOr(SESSIE, -1L) == sessie && t.getBooleanOr(id, false);
    }

    /** The player's scooped heaps for this dream (a new dream: an empty list). */
    private static CompoundTag stand(ServerPlayer p, long sessie) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag t = saved.getCompoundOrEmpty(KEY);
        if (t.getLongOr(SESSIE, -1L) != sessie) {
            t = new CompoundTag();
            t.putLong(SESSIE, sessie);
        }
        saved.put(KEY, t);
        return t;
    }

    /** Does this island stand in its real template version (not a placeholder, not still to be stamped)? */
    static boolean staat(ServerLevel level, String eiland) {
        DroomEilanden.Opzet o = DroomEilanden.opzet(eiland);
        return o.template() != null && o.versie() > 0 && DroomEilanden.gebouwd(level, eiland) == o.versie();
    }

    /** Puts the heaps on the free spots of the islands that stand; true when it placed one. */
    public static boolean zorg(ServerLevel level) {
        boolean gezet = false;
        for (Hoopje h : alle()) {
            BlockPos pos = h.wereld();
            if (staat(level, h.eiland()) && level.isLoaded(pos) && level.getBlockState(pos).isAir()) {
                level.setBlock(pos, DroomzandFeature.ZANDHOOPJE.get().defaultBlockState(), 2 | 16);
                gezet = true;
            }
        }
        return gezet;
    }

    private static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 13) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(DromenFeature.DIM);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        zorg(level);
        for (ServerPlayer p : level.players()) {
            long sessie = DroomReis.sessie(p);
            for (Hoopje h : alle()) {
                BlockPos pos = h.wereld();
                if (pos.distToCenterSqr(p.position()) < GLITTER_BIJ * GLITTER_BIJ && !geschept(p, h.id(), sessie)
                        && level.getBlockState(pos).is(DroomzandFeature.ZANDHOOPJE.get())) {
                    level.sendParticles(p, ParticleTypes.END_ROD, false, false, pos.getX() + 0.5, pos.getY() + 0.55, pos.getZ() + 0.5,
                            2, 0.25, 0.15, 0.25, 0.01);
                }
            }
        }
    }
}
