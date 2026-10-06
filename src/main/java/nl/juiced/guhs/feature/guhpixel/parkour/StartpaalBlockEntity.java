package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.taal.Tekst;

/**
 * The Startpaaltje of a Guh-parkour: it holds the route (the pieces in order, the Finishpaaltje last), the up to four guhs
 * that run it and their scores (laps, best and last lap time in ticks, per guh; kept when a guh is taken out). The guhs
 * themselves carry where their post is ({@link RouteGoal#PAAL}), so a guh that loads later finds its route again; when
 * the post goes, its guhs are free. Every finished lap goes to the Scoreborden near it ({@link #stuurBorden}).
 */
public class StartpaalBlockEntity extends BlockEntity {
    public static final int MAX_SCORES = 12;
    /** A route with at least this many pieces is "groot" (the FTB quest and the Guhdex). */
    public static final int GROOT = 8;

    /** One guh's results on this route. */
    public record Score(Component naam, int rondjes, int beste, int laatste) {
    }

    @Nullable
    private UUID eigenaar;
    private Component eigenaarNaam = Component.empty();
    private final List<BlockPos> stukken = new ArrayList<>();
    private final List<UUID> guhs = new ArrayList<>();
    private final Map<UUID, Score> scores = new LinkedHashMap<>();
    private boolean geteld;
    /** (not saved) the pieces a guh could not get to on its last try. */
    private final Set<BlockPos> hapert = new HashSet<>();

    public StartpaalBlockEntity(BlockPos pos, BlockState state) {
        super(ParkourSlice.STARTPAAL_BE.get(), pos, state);
    }

    // --- the owner -----------------------------------------------------------------------------------------------------------

    public void zetEigenaar(Player speler) {
        eigenaar = speler.getUUID();
        eigenaarNaam = speler.getName().copy();
        setChanged();
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public Component eigenaarNaam() {
        return eigenaarNaam;
    }

    /** The owner, or a gamemaster. A post nobody owns (placed by a command or a dispenser) is everybody's. */
    public boolean magBewerken(Player speler) {
        return eigenaar == null || eigenaar.equals(speler.getUUID())
                || speler.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    // --- the route -----------------------------------------------------------------------------------------------------------

    public List<BlockPos> stukken() {
        return Collections.unmodifiableList(stukken);
    }

    /** A click on a piece while laying out: see {@link Routes#klik}. */
    public Routes.Uitkomst klik(BlockPos stuk) {
        if (level == null) {
            return Routes.Uitkomst.GEEN;
        }
        Routes.Uitkomst u = Routes.klik(level, stukken, worldPosition, stuk);
        if (u != Routes.Uitkomst.GEEN && u != Routes.Uitkomst.VOL && u != Routes.Uitkomst.TE_VER) {
            gewijzigd();
        }
        return u;
    }

    /** Takes over a whole list (from the item it was placed with); returns how many pieces were too far away. */
    public int neemOver(List<BlockPos> lijst) {
        stukken.clear();
        int teVer = 0;
        for (BlockPos p : lijst) {
            if (stukken.contains(p)) {
                continue;
            }
            if (!worldPosition.closerThan(p, Routes.BEREIK + 0.5)) {
                teVer++;
            } else if (level == null || Routes.aantal(level, stukken) < Routes.MAX_STUKKEN || Routes.soort(level, p) == Routes.Soort.FINISH) {
                stukken.add(p.immutable());
            }
        }
        if (level != null) {
            finishAchteraan();
        }
        gewijzigd();
        return teVer;
    }

    /** Moves piece i one place up (-1) or down (+1); the Finishpaaltje stays last. */
    public boolean verplaats(int i, int richting) {
        int j = i + richting;
        if (level == null || i < 0 || j < 0 || i >= stukken.size() || j >= stukken.size()) {
            return false;
        }
        if (Routes.soort(level, stukken.get(i)) == Routes.Soort.FINISH || Routes.soort(level, stukken.get(j)) == Routes.Soort.FINISH) {
            return false;
        }
        Collections.swap(stukken, i, j);
        gewijzigd();
        return true;
    }

    public boolean verwijder(int i) {
        if (i < 0 || i >= stukken.size()) {
            return false;
        }
        stukken.remove(i);
        gewijzigd();
        return true;
    }

    private void finishAchteraan() {
        BlockPos finish = null;
        for (BlockPos p : stukken) {
            if (Routes.soort(level, p) == Routes.Soort.FINISH) {
                finish = p;
            }
        }
        if (finish != null) {
            BlockPos f = finish;
            stukken.removeIf(p -> Routes.soort(level, p) == Routes.Soort.FINISH);
            stukken.add(f);
        }
    }

    private void gewijzigd() {
        hapert.clear();
        setChanged();
    }

    /** A guh could not get to this piece. */
    public void hapert(BlockPos stuk) {
        hapert.add(stuk);
    }

    public void looptWeer(BlockPos stuk) {
        hapert.remove(stuk);
    }

    public boolean hapertBij(BlockPos stuk) {
        return hapert.contains(stuk);
    }

    // --- the guhs ------------------------------------------------------------------------------------------------------------

    public List<UUID> guhs() {
        return Collections.unmodifiableList(guhs);
    }

    public boolean heeftGuh(UUID id) {
        return guhs.contains(id);
    }

    /** Puts a guh on the route (the caller checked that it may): it stands up and starts walking to the post. */
    public boolean zetOp(GuhEntity guh) {
        if (guhs.contains(guh.getUUID()) || guhs.size() >= Routes.MAX_GUHS) {
            return false;
        }
        guhs.add(guh.getUUID());
        guh.getPersistentData().putLong(RouteGoal.PAAL, worldPosition.asLong());
        if (level != null) {
            guh.getPersistentData().putString(RouteGoal.PAAL_DIM, level.dimension().identifier().toString());
        }
        GuhKiezer.claim(guh, ParkourSlice.NS);
        guh.setOrderedToSit(false);
        guh.setInSittingPose(false);
        setChanged();
        return true;
    }

    /**
     * Drops the listed guhs that are loaded here and no longer belong to this post (a guh that was away too long freed
     * itself: {@link RouteGoal#WEG_TICKS}), so their places are free again. Unloaded guhs stay listed.
     */
    public void ruimOp() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        boolean weg = guhs.removeIf(id -> sl.getEntity(id) instanceof GuhEntity guh
                && guh.getPersistentData().getLongOr(RouteGoal.PAAL, Long.MIN_VALUE) != worldPosition.asLong());
        if (weg) {
            setChanged();
        }
    }

    /** Takes a guh out (also when it is not loaded: it frees itself when it finds it is no longer on the list). */
    public boolean haalAf(UUID id) {
        if (!guhs.remove(id)) {
            return false;
        }
        if (level instanceof ServerLevel sl && sl.getEntity(id) instanceof GuhEntity guh) {
            RouteGoal.vrij(guh);
        }
        setChanged();
        return true;
    }

    // --- scores --------------------------------------------------------------------------------------------------------------

    public Map<UUID, Score> scores() {
        return Collections.unmodifiableMap(scores);
    }

    public void wisScores() {
        scores.clear();
        setChanged();
        stuurBorden();
    }

    /** A guh finished a lap in this many ticks: its score, a cheer, a heart, the owner's quests, the Scoreborden. */
    public void rondje(GuhEntity guh, int ticks) {
        if (!(level instanceof ServerLevel sl) || ticks <= 0) {
            return;
        }
        Score oud = scores.get(guh.getUUID());
        boolean record = oud == null || oud.beste() <= 0 || ticks < oud.beste();
        Score nieuw = new Score(guh.getName().copy(), (oud == null ? 0 : oud.rondjes()) + 1, record ? ticks : oud.beste(), ticks);
        scores.remove(guh.getUUID());
        scores.put(guh.getUUID(), nieuw);
        while (scores.size() > MAX_SCORES) {     // (the oldest result of a guh that no longer runs here goes first)
            UUID weg = scores.keySet().stream().filter(id -> !guhs.contains(id)).findFirst().orElse(scores.keySet().iterator().next());
            scores.remove(weg);
        }
        setChanged();
        stuurBorden();
        sl.playSound(null, guh.blockPosition(), (record && oud != null ? ParkourSlice.RECORD : ParkourSlice.FINISH).get(), SoundSource.NEUTRAL, 0.9f,
                guh.getVoicePitch());
        sl.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 3, 0.3, 0.15, 0.3, 0);
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, guh.getX(), guh.getY() + guh.getBbHeight(), guh.getZ(), 5, 0.3, 0.2, 0.3, 0);
        ServerPlayer baas = Band.eigenaarOnline(guh);
        if (Band.isBandGuh(guh)) {
            Band.geefHartjes(guh, baas, 1, Reden.SPEELGOED);
        }
        if (baas != null) {
            int stuks = Routes.aantal(sl, stukken);
            boolean nieuweRoute = !geteld;
            geteld = true;
            ParkourStats.rondje(baas, guh, ticks, stuks, nieuweRoute);
            GuhAdvancements.grant(baas, "guhparkour_rondje");
            if (stuks >= GROOT && Routes.heeftFinish(sl, stukken)) {
                GuhAdvancements.grant(baas, "guhparkour_groot");
            }
            if (baas.level() == sl && baas.blockPosition().closerThan(worldPosition, Routes.BEREIK + 16)) {
                baas.sendOverlayMessage(Component.translatable(record && oud != null ? "gui.guhs.guhparkour.rondje.record" : "gui.guhs.guhparkour.rondje",
                        guh.getName(), nieuw.rondjes(), ParkourStats.tijd(ticks)).withStyle(record && oud != null ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** The rows for a Scorebord: the guhs by best time (still running here first when times are equal). */
    public ListTag rijen() {
        List<Map.Entry<UUID, Score>> lijst = new ArrayList<>(scores.entrySet());
        lijst.sort((a, b) -> Integer.compare(a.getValue().beste(), b.getValue().beste()));
        ListTag uit = new ListTag();
        for (Map.Entry<UUID, Score> e : lijst) {
            uit.add(scoreTag(e.getKey(), e.getValue()));
        }
        return uit;
    }

    private static CompoundTag scoreTag(UUID id, Score s) {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, id);
        Tekst.put(t, "Naam", s.naam());
        t.putInt("Rondjes", s.rondjes());
        t.putInt("Beste", s.beste());
        t.putInt("Laatste", s.laatste());
        return t;
    }

    /** Gives the scores to every Scorebord within reach that is linked to this post (or to none yet). */
    public void stuurBorden() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        ListTag rijen = rijen();
        for (ScorebordBlockEntity bord : borden(sl, worldPosition)) {
            if (bord.paal() == null || !(sl.getBlockEntity(bord.paal()) instanceof StartpaalBlockEntity)) {
                bord.koppel(worldPosition);
            }
            if (worldPosition.equals(bord.paal())) {
                bord.zet(rijen);
            }
        }
    }

    /** Every loaded Scorebord within the reach of a route around this spot. */
    static List<ScorebordBlockEntity> borden(ServerLevel level, BlockPos rond) {
        List<ScorebordBlockEntity> uit = new ArrayList<>();
        ChunkPos a = ChunkPos.containing(rond.offset(-Routes.BEREIK, 0, -Routes.BEREIK)), b = ChunkPos.containing(rond.offset(Routes.BEREIK, 0, Routes.BEREIK));
        for (int cx = a.x(); cx <= b.x(); cx++) {
            for (int cz = a.z(); cz <= b.z(); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof ScorebordBlockEntity bord && be.getBlockPos().closerThan(rond, Routes.BEREIK + 0.5)) {
                        uit.add(bord);
                    }
                }
            }
        }
        return uit;
    }

    /** Every loaded Startpaaltje within the reach of a route around this spot, nearest first. */
    static List<StartpaalBlockEntity> palen(ServerLevel level, BlockPos rond) {
        List<StartpaalBlockEntity> uit = new ArrayList<>();
        ChunkPos a = ChunkPos.containing(rond.offset(-Routes.BEREIK, 0, -Routes.BEREIK)), b = ChunkPos.containing(rond.offset(Routes.BEREIK, 0, Routes.BEREIK));
        for (int cx = a.x(); cx <= b.x(); cx++) {
            for (int cz = a.z(); cz <= b.z(); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof StartpaalBlockEntity paal && be.getBlockPos().closerThan(rond, Routes.BEREIK + 0.5)) {
                        uit.add(paal);
                    }
                }
            }
        }
        uit.sort((x, y) -> Double.compare(x.getBlockPos().distSqr(rond), y.getBlockPos().distSqr(rond)));
        return uit;
    }

    // --- the post is gone ------------------------------------------------------------------------------------------------------

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl) {
            for (UUID id : guhs) {
                Entity e = sl.getEntity(id);
                if (e instanceof GuhEntity guh) {
                    RouteGoal.vrij(guh);
                }
            }
            for (ScorebordBlockEntity bord : borden(sl, pos)) {
                if (pos.equals(bord.paal())) {
                    bord.koppel(null);
                }
            }
        }
    }

    // --- saving ----------------------------------------------------------------------------------------------------------------

    /** The route as the item remembers it (the Startpaaltje keeps its route when you break it). */
    public static CompoundTag routeTag(List<BlockPos> stukken) {
        CompoundTag t = new CompoundTag();
        t.putLongArray("Stukken", stukken.stream().mapToLong(BlockPos::asLong).toArray());
        return t;
    }

    public static List<BlockPos> uitRouteTag(CompoundTag t) {
        List<BlockPos> uit = new ArrayList<>();
        for (long l : t.getLongArray("Stukken").orElse(new long[0])) {
            uit.add(BlockPos.of(l));
        }
        return uit;
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        CompoundTag t = routeTag(stukken);
        if (eigenaar != null) {
            t.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
            Tekst.put(t, "EigenaarNaam", eigenaarNaam);
        }
        ListTag g = new ListTag();
        for (UUID id : guhs) {
            CompoundTag x = new CompoundTag();
            x.store("Id", UUIDUtil.CODEC, id);
            g.add(x);
        }
        t.put("Guhs", g);
        ListTag s = new ListTag();
        scores.forEach((id, score) -> s.add(scoreTag(id, score)));
        t.put("Scores", s);
        t.putBoolean("Geteld", geteld);
        tag.store("Parkour", CompoundTag.CODEC, t);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        CompoundTag t = tag.read("Parkour", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        stukken.clear();
        stukken.addAll(uitRouteTag(t));
        eigenaar = t.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        eigenaarNaam = Tekst.get(t, "EigenaarNaam");
        guhs.clear();
        ListTag g = t.getListOrEmpty("Guhs");
        for (int i = 0; i < g.size(); i++) {
            g.getCompoundOrEmpty(i).read("Id", UUIDUtil.CODEC).ifPresent(guhs::add);
        }
        scores.clear();
        ListTag s = t.getListOrEmpty("Scores");
        for (int i = 0; i < s.size(); i++) {
            CompoundTag x = s.getCompoundOrEmpty(i);
            UUID id = x.read("Id", UUIDUtil.CODEC).orElse(null);
            if (id != null) {
                scores.put(id, new Score(Tekst.get(x, "Naam"), x.getIntOr("Rondjes", 0), x.getIntOr("Beste", 0), x.getIntOr("Laatste", 0)));
            }
        }
        geteld = t.getBooleanOr("Geteld", false);
    }
}
