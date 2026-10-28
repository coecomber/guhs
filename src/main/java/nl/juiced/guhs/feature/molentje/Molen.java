package nl.juiced.guhs.feature.molentje;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.blub.BlubFeature;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.feature.droomwezens.DroomNpc;
import nl.juiced.guhs.feature.droomwezens.DroomwezensFeature;
import nl.juiced.guhs.feature.droomwezens.SterrenvisjeEntity;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.DroomVoortgang;
import nl.juiced.guhs.feature.dromen.DromenFeature;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The Maanmolentje's activity (ontwerp round 10, "molen aan de praat"): the mill stands still. Three Blub-guhs have to sit on
 * the blowing cushions and blow the sails going ({@link #roepBlub}: click a cushion and the nearest Blub hops over), and the
 * four cogs on the puzzle wall must all point up ({@link #klikTandwiel}: a cog always turns the next one in the chain along, so
 * it is a little puzzle). Then the sails turn for {@link #DUUR} ticks, the Zandguh grinds droomzand and gives every player a
 * little bag of it once (and Snoes' piece {@code DROOMZAND}: {@link #praatZandguh}). After that the mill stops, the Blubs hop
 * off and the cogs are shuffled again for the next dreamer.
 * <p>
 * The spots come from {@code data/guhs/dromen/molentje_plekken.json} (tools/features/molentje.py, relative to the island's
 * centre). {@link #zorg} keeps the island lived in: the Zandguh, a few wild Blubs and sterrenvisjes in the moon pond.
 */
public final class Molen {
    private static final Logger LOG = LogUtils.getLogger();

    /** How long the sails turn (ticks), how often the mill is looked at (ticks). */
    public static final int DUUR = 20 * 90, RONDE = 10;
    /** How many wild Blubs and sterrenvisjes the island keeps. */
    public static final int BLUBS = 5, VISJES = 4;
    /** A Blub's persistent data: which cushion it goes to / sits on (1-based; 0 = none), and how long it has been on its way. */
    public static final String KUSSEN = "guhs_molen_kussen", WACHT = "guhs_molen_wacht";
    static final String HINT = "guhs_molen_hint";

    /** All the spots of one mill, in the world. */
    public record Plekken(List<BlockPos> tandwielen, List<BlockPos> kussentjes, BlockPos wieken, Vec3 zandguh, float zandguhYaw, Vec3 goot,
                          List<Vec3> blubs, List<Vec3> visjes) {
        boolean bevat(BlockPos pos) {
            return tandwielen.contains(pos) || kussentjes.contains(pos) || wieken.equals(pos);
        }

        AABB gebied() {
            return new AABB(wieken).inflate(36);
        }
    }

    /** Mills outside the dream (the game tests): looked up like the island's one. */
    static final List<Plekken> EXTRA = new CopyOnWriteArrayList<>();
    @Nullable
    private static JsonObject json;

    private Molen() {
    }

    // =========================================================================================================== the spots
    private static synchronized JsonObject json() {
        if (json == null) {
            try (InputStream in = Molen.class.getResourceAsStream("/data/guhs/dromen/molentje_plekken.json")) {
                json = in == null ? new JsonObject() : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                LOG.error("Maanmolentje: molentje_plekken.json is broken: {}", e.toString());
                json = new JsonObject();
            }
        }
        return json;
    }

    /** The island's mill (null when the spots file is missing). */
    @Nullable
    public static Plekken opEiland() {
        JsonObject o = json();
        if (!o.has("wieken")) {
            return null;
        }
        BlockPos m = DroomEilanden.midden(DroomEilanden.MOLENTJE);
        JsonObject zg = o.getAsJsonObject("zandguh");
        return new Plekken(blokken(m, o.getAsJsonArray("tandwielen")), blokken(m, o.getAsJsonArray("kussentjes")), blok(m, o.getAsJsonArray("wieken")),
                punt(m, zg.getAsJsonArray("plek")), zg.has("yaw") ? zg.get("yaw").getAsFloat() : 0f, punt(m, o.getAsJsonArray("goot")),
                punten(m, o.getAsJsonArray("blubs")), punten(m, o.getAsJsonArray("visjes")));
    }

    /** A mill anywhere (the game tests); register it with {@link #EXTRA} to make its blocks work. */
    public static Plekken van(List<BlockPos> tandwielen, List<BlockPos> kussentjes, BlockPos wieken, Vec3 zandguh) {
        return new Plekken(List.copyOf(tandwielen), List.copyOf(kussentjes), wieken, zandguh, 0f, Vec3.atCenterOf(wieken), List.of(), List.of());
    }

    private static BlockPos blok(BlockPos m, JsonArray a) {
        return m.offset(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static List<BlockPos> blokken(BlockPos m, JsonArray a) {
        List<BlockPos> out = new ArrayList<>();
        a.forEach(e -> out.add(blok(m, e.getAsJsonArray())));
        return List.copyOf(out);
    }

    private static Vec3 punt(BlockPos m, JsonArray a) {
        return new Vec3(m.getX() + a.get(0).getAsDouble(), m.getY() + a.get(1).getAsDouble(), m.getZ() + a.get(2).getAsDouble());
    }

    private static List<Vec3> punten(BlockPos m, @Nullable JsonArray a) {
        List<Vec3> out = new ArrayList<>();
        if (a != null) {
            a.forEach(e -> out.add(punt(m, e.getAsJsonArray())));
        }
        return List.copyOf(out);
    }

    /** The mill this block belongs to (the island's one in the dream, or a registered one), else null. */
    @Nullable
    static Plekken vind(ServerLevel level, BlockPos pos) {
        for (Plekken p : EXTRA) {
            if (p.bevat(pos)) {
                return p;
            }
        }
        if (level.dimension() == DromenFeature.DIM) {
            Plekken p = opEiland();
            if (p != null && p.bevat(pos)) {
                return p;
            }
        }
        return null;
    }

    /** The mill around this spot (within its area), else null. */
    @Nullable
    static Plekken bij(ServerLevel level, Vec3 pos) {
        for (Plekken p : EXTRA) {
            if (p.gebied().contains(pos)) {
                return p;
            }
        }
        if (level.dimension() == DromenFeature.DIM) {
            Plekken p = opEiland();
            if (p != null && p.gebied().contains(pos)) {
                return p;
            }
        }
        return null;
    }

    // =========================================================================================================== the cogs
    /** Where each cog's arrow points (0 up .. 3 left; -1 where a cog is missing). */
    public static int[] standen(Level level, Plekken p) {
        int[] s = new int[p.tandwielen().size()];
        for (int i = 0; i < s.length; i++) {
            BlockState st = level.getBlockState(p.tandwielen().get(i));
            s[i] = st.hasProperty(MolenBlokken.Tandwiel.STAND) ? st.getValue(MolenBlokken.Tandwiel.STAND) : -1;
        }
        return s;
    }

    /** All arrows up? */
    public static boolean opgelost(Level level, Plekken p) {
        for (int s : standen(level, p)) {
            if (s != 0) {
                return false;
            }
        }
        return true;
    }

    /** Click cog i: it turns a quarter, and the next one in the chain turns along. */
    public static void klik(Level level, Plekken p, int i) {
        MolenBlokken.Tandwiel.draai(level, p.tandwielen().get(i));
        if (i + 1 < p.tandwielen().size()) {
            MolenBlokken.Tandwiel.draai(level, p.tandwielen().get(i + 1));
        }
    }

    /** A player clicked a cog: at a mill the puzzle move (not while the sails turn), elsewhere it just turns. */
    static void klikTandwiel(ServerLevel level, BlockPos pos, ServerPlayer sp) {
        Plekken p = vind(level, pos);
        if (p == null) {
            MolenBlokken.Tandwiel.draai(level, pos);
            return;
        }
        if (loopt(level, p)) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.molentje.draait").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        int i = p.tandwielen().indexOf(pos);
        klik(level, p, i);
        int stand = standen(level, p)[i];
        sp.sendOverlayMessage(Component.translatable("gui.guhs.molentje.tandwiel.klik", i + 1,
                Component.translatable("gui.guhs.molentje.richting." + stand)).withStyle(ChatFormatting.GOLD));
        if (opgelost(level, p)) {
            sp.sendSystemMessage(Component.translatable("gui.guhs.molentje.tandwielen_goed").withStyle(ChatFormatting.GREEN));
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.3f);
        }
    }

    /** Shuffle the cogs (with puzzle moves backwards from solved, so it can always be solved; never solved already). */
    public static void schud(ServerLevel level, Plekken p) {
        for (BlockPos t : p.tandwielen()) {
            BlockState st = level.getBlockState(t);
            if (st.hasProperty(MolenBlokken.Tandwiel.STAND)) {
                level.setBlock(t, st.setValue(MolenBlokken.Tandwiel.STAND, 0), Block.UPDATE_ALL);
            }
        }
        int n = p.tandwielen().size();
        if (n == 0) {
            return;
        }
        int zetten = 4 + level.getRandom().nextInt(5);
        for (int k = 0; k < zetten || opgelost(level, p); k++) {
            int i = level.getRandom().nextInt(n);
            for (int r = 0; r < 3; r++) {          // three clicks forward = one click back
                zetStil(level, p.tandwielen().get(i));
                if (i + 1 < n) {
                    zetStil(level, p.tandwielen().get(i + 1));
                }
            }
        }
    }

    /** Turn a cog without the click sound (shuffling). */
    private static void zetStil(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.hasProperty(MolenBlokken.Tandwiel.STAND)) {
            level.setBlock(pos, s.setValue(MolenBlokken.Tandwiel.STAND, (s.getValue(MolenBlokken.Tandwiel.STAND) + 1) % 4), Block.UPDATE_ALL);
        }
    }

    // =========================================================================================================== the Blubs
    /** A player clicked a blowing cushion: the nearest free Blub hops over. */
    static void roepBlub(ServerLevel level, BlockPos kussen, ServerPlayer sp) {
        Plekken p = vind(level, kussen);
        if (p == null) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.molentje.kussen.niet_hier").withStyle(ChatFormatting.GRAY));
            return;
        }
        int i = p.kussentjes().indexOf(kussen);
        if (bezetter(level, p, i) != null) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.molentje.kussen.bezet").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        BlubGuhEntity beste = null;
        double d = Double.MAX_VALUE;
        Vec3 c = Vec3.atBottomCenterOf(kussen);
        for (BlubGuhEntity b : level.getEntitiesOfClass(BlubGuhEntity.class, new AABB(kussen).inflate(24), BlubGuhEntity::isAlive)) {
            if (b.getPersistentData().getIntOr(KUSSEN, 0) != 0 || b.isBaby() || (b.isTame() && !b.isOwnedBy(sp))) {
                continue;
            }
            double dd = b.distanceToSqr(c);
            if (dd < d) {
                d = dd;
                beste = b;
            }
        }
        if (beste == null) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.molentje.kussen.geen_blub").withStyle(ChatFormatting.GRAY));
            return;
        }
        stuur(beste, i);
        level.sendParticles(ParticleTypes.HEART, beste.getX(), beste.getY() + beste.getBbHeight() + 0.2, beste.getZ(), 3, 0.2, 0.1, 0.2, 0);
        sp.sendOverlayMessage(Component.translatable("gui.guhs.molentje.kussen.komt").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Send a Blub to cushion i (0-based). */
    public static void stuur(BlubGuhEntity b, int i) {
        b.getPersistentData().putInt(KUSSEN, i + 1);
        b.getPersistentData().putInt(WACHT, 0);
        b.setNoAi(false);
    }

    /** Let a Blub go again (it hops off). */
    static void laatGaan(BlubGuhEntity b) {
        b.getPersistentData().remove(KUSSEN);
        b.getPersistentData().remove(WACHT);
        if (b.isNoAi()) {
            b.setNoAi(false);
            b.setDeltaMovement(0, 0.32, 0);
        }
    }

    @Nullable
    static BlubGuhEntity bezetter(ServerLevel level, Plekken p, int i) {
        for (BlubGuhEntity b : blubsVan(level, p)) {
            if (b.getPersistentData().getIntOr(KUSSEN, 0) == i + 1) {
                return b;
            }
        }
        return null;
    }

    static List<BlubGuhEntity> blubsVan(ServerLevel level, Plekken p) {
        return level.getEntitiesOfClass(BlubGuhEntity.class, p.gebied(), b -> b.isAlive() && b.getPersistentData().getIntOr(KUSSEN, 0) > 0);
    }

    private static Vec3 zitplek(Plekken p, int i) {
        return Vec3.atBottomCenterOf(p.kussentjes().get(i)).add(0, 0.25, 0);
    }

    // =========================================================================================================== the run
    /** Do the sails turn right now? */
    public static boolean loopt(ServerLevel level, Plekken p) {
        return Stand.van(level).run(p.wieken()).draaiTot > level.getGameTime();
    }

    /** One look at the mill (every {@link #RONDE} ticks): the Blubs to their cushions and blowing, start and stop the sails. */
    public static void stap(ServerLevel level, Plekken p) {
        Stand stand = Stand.van(level);
        Run run = stand.run(p.wieken());
        long nu = level.getGameTime();
        if (!run.geschud) {
            schud(level, p);
            run.geschud = true;
            stand.setDirty();
        }
        int blazen = 0;
        for (BlubGuhEntity b : blubsVan(level, p)) {
            int i = b.getPersistentData().getIntOr(KUSSEN, 0) - 1;
            if (i < 0 || i >= p.kussentjes().size()) {
                laatGaan(b);
                continue;
            }
            Vec3 doel = zitplek(p, i);
            if (b.distanceToSqr(doel) > 0.81) {
                if (b.isNoAi()) {
                    b.setNoAi(false);
                }
                b.getNavigation().moveTo(doel.x, doel.y, doel.z, 1.1);
                int w = b.getPersistentData().getIntOr(WACHT, 0) + RONDE;
                b.getPersistentData().putInt(WACHT, w);
                if (w > 200) {                   // stuck: a big hop straight onto it
                    b.snapTo(doel.x, doel.y, doel.z, b.getYRot(), 0);
                }
                continue;
            }
            Vec3 naaf = Vec3.atCenterOf(p.wieken());
            float yaw = (float) (Mth.atan2(naaf.z - doel.z, naaf.x - doel.x) * Mth.RAD_TO_DEG) - 90f;
            b.getNavigation().stop();
            b.setNoAi(true);
            b.snapTo(doel.x, doel.y, doel.z, yaw, 0);
            b.setYHeadRot(yaw);
            b.setYBodyRot(yaw);
            b.setDeltaMovement(Vec3.ZERO);
            blaas(level, b, naaf);
            blazen++;
        }
        boolean draait = run.draaiTot > nu;
        if (draait) {
            level.sendParticles(ParticleTypes.END_ROD, p.goot().x, p.goot().y, p.goot().z, 3, 0.4, 0.3, 0.4, 0.01);
            if (nu + RONDE >= run.draaiTot) {
                stop(level, p, stand, run);
                draait = false;
            }
        } else if (blazen >= p.kussentjes().size() && opgelost(level, p)) {
            start(level, p, stand, run, nu);
            draait = true;
        }
        BlockState w = level.getBlockState(p.wieken());
        if (w.hasProperty(MolenBlokken.Wieken.DRAAIT) && w.getValue(MolenBlokken.Wieken.DRAAIT) != draait) {
            level.setBlock(p.wieken(), w.setValue(MolenBlokken.Wieken.DRAAIT, draait), Block.UPDATE_ALL);
        }
    }

    private static void blaas(ServerLevel level, BlubGuhEntity b, Vec3 naar) {
        Vec3 van = b.position().add(0, b.getBbHeight() * 0.6, 0);
        for (int k = 1; k <= 5; k++) {
            Vec3 q = van.lerp(naar, k / 6.0);
            level.sendParticles(KnuffelbadFeature.ZEEPBELLETJE.get(), q.x, q.y, q.z, 1, 0.15, 0.15, 0.15, 0.01);
        }
        if (b.getRandom().nextInt(4) == 0) {
            BlubGeluid.BELLEN.speel(b);
        }
    }

    private static void start(ServerLevel level, Plekken p, Stand stand, Run run, long nu) {
        run.draaiTot = nu + DUUR;
        run.gemalen.clear();
        stand.setDirty();
        level.playSound(null, p.wieken(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 0.8f);
        level.playSound(null, p.wieken(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.2f, 1.2f);
        for (ServerPlayer sp : level.getPlayers(pl -> pl.distanceToSqr(Vec3.atCenterOf(p.wieken())) < 48 * 48)) {
            sp.sendSystemMessage(Component.translatable("gui.guhs.molentje.draait").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void stop(ServerLevel level, Plekken p, Stand stand, Run run) {
        run.draaiTot = 0;
        run.gemalen.clear();
        stand.setDirty();
        for (BlubGuhEntity b : blubsVan(level, p)) {
            laatGaan(b);
        }
        schud(level, p);
        for (ServerPlayer sp : level.getPlayers(pl -> pl.distanceToSqr(Vec3.atCenterOf(p.wieken())) < 48 * 48)) {
            sp.sendSystemMessage(Component.translatable("gui.guhs.molentje.stil").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Stop the mill right away and shuffle (the test command). */
    public static void reset(ServerLevel level, Plekken p) {
        Stand stand = Stand.van(level);
        stop(level, p, stand, stand.run(p.wieken()));
        BlockState w = level.getBlockState(p.wieken());
        if (w.hasProperty(MolenBlokken.Wieken.DRAAIT)) {
            level.setBlock(p.wieken(), w.setValue(MolenBlokken.Wieken.DRAAIT, false), Block.UPDATE_ALL);
        }
    }

    // =========================================================================================================== the Zandguh
    /** DroomNpc's story talk of the Zandguh: at the mill a bag of droomzand (once a run) and Snoes' piece, else hints. */
    public static boolean praatZandguh(ServerPlayer sp, GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        Plekken p = bij(level, npc.position());
        if (p == null) {
            return false;
        }
        Stand stand = Stand.van(level);
        Run run = stand.run(p.wieken());
        if (run.draaiTot > level.getGameTime()) {
            if (run.gemalen.contains(sp.getUUID())) {
                GuhQuests.say(sp, npc, "gui.guhs.molentje.zandguh.al");
                return true;
            }
            run.gemalen.add(sp.getUUID());
            stand.setDirty();
            ItemStack zakje = new ItemStack(DroomblokkenFeature.DROOMZAND.get(), 3);
            if (!sp.getInventory().add(zakje)) {
                sp.drop(zakje, false);
            }
            GuhQuests.say(sp, npc, "gui.guhs.molentje.zandguh.droomzand");
            level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1.2, npc.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
            if (DroomVoortgang.geef(sp, DroomVoortgang.Stukje.DROOMZAND)) {
                sp.sendSystemMessage(Component.translatable("gui.guhs.molentje.stukje").withStyle(ChatFormatting.GOLD));
            }
            return true;
        }
        int h = npc.roleData.getIntOr(HINT, 0);
        npc.roleData.putInt(HINT, h + 1);
        GuhQuests.say(sp, npc, "gui.guhs.molentje.zandguh.hint." + Math.floorMod(h, 3));
        return true;
    }

    // =========================================================================================================== the island
    /** Keep the island lived in: its Zandguh, a few wild Blubs, sterrenvisjes in the pond (one new one per look at most). */
    static void zorg(ServerLevel level, Plekken p) {
        AABB gebied = p.gebied();
        if (level.getEntitiesOfClass(GuhNpcEntity.class, gebied, n -> n.getKind() == GuhNpcEntity.Kind.ZANDGUH).isEmpty()) {
            GuhNpcEntity z = DroomNpc.spawn(level, p.zandguh(), p.zandguhYaw(), GuhNpcEntity.Kind.ZANDGUH);
            z.setPersistenceRequired();
        }
        if (!p.blubs().isEmpty() && level.getEntitiesOfClass(BlubGuhEntity.class, gebied, b -> !b.isTame()).size() < BLUBS) {
            Vec3 at = p.blubs().get(level.getRandom().nextInt(p.blubs().size()));
            BlubGuhEntity b = BlubFeature.BLUBGUH.get().create(level, EntitySpawnReason.EVENT);
            if (b != null) {
                b.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360f, 0);
                b.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), EntitySpawnReason.EVENT, null);
                b.setPersistenceRequired();
                level.addFreshEntity(b);
            }
        }
        if (!p.visjes().isEmpty() && level.getEntitiesOfClass(SterrenvisjeEntity.class, gebied, v -> true).size() < VISJES) {
            Vec3 at = p.visjes().get(level.getRandom().nextInt(p.visjes().size()));
            if (level.getFluidState(BlockPos.containing(at)).isSource()) {
                SterrenvisjeEntity v = DroomwezensFeature.STERRENVISJE.get().create(level, EntitySpawnReason.EVENT);
                if (v != null) {
                    v.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360f, 0);
                    v.setKleur(SterrenvisjeEntity.Kleur.values()[level.getRandom().nextInt(SterrenvisjeEntity.Kleur.values().length)]);
                    v.setPersistenceRequired();
                    level.addFreshEntity(v);
                }
            }
        }
    }

    /** The server tick: the island's mill, only while someone dreams nearby and its chunks are there. */
    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % RONDE != 0) {
            return;
        }
        ServerLevel droom = server.getLevel(DromenFeature.DIM);
        if (droom == null) {
            return;
        }
        Plekken p = opEiland();
        if (p == null) {
            return;
        }
        Vec3 c = Vec3.atCenterOf(p.wieken());
        if (droom.getPlayers(pl -> pl.distanceToSqr(c) < 80 * 80).isEmpty() || !droom.isLoaded(p.wieken())
                || !(droom.getBlockState(p.wieken()).getBlock() instanceof MolenBlokken.Wieken)) {
            return;
        }
        zorg(droom, p);
        stap(droom, p);
    }

    // =========================================================================================================== saved state
    /** One mill's run: when its sails stop, whether its cogs were shuffled, who got a bag of droomzand. */
    static final class Run {
        long draaiTot;
        boolean geschud;
        final Set<UUID> gemalen = new HashSet<>();
    }

    /** The runs of the mills in a level (by the sails' block). */
    public static final class Stand extends SavedData {
        public static final SavedDataType<Stand> TYPE = GuhSavedData.tagType("dromen_molentje", Stand::new, Stand::load, Stand::save);
        final Map<Long, Run> runs = new HashMap<>();

        static Stand van(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(TYPE);
        }

        Run run(BlockPos wieken) {
            return runs.computeIfAbsent(wieken.asLong(), k -> new Run());
        }

        private static Stand load(CompoundTag tag) {
            Stand s = new Stand();
            for (String k : tag.keySet()) {
                CompoundTag t = tag.getCompoundOrEmpty(k);
                Run r = new Run();
                r.draaiTot = t.getLongOr("DraaiTot", 0L);
                r.geschud = t.getBooleanOr("Geschud", false);
                for (String u : t.getStringOr("Gemalen", "").split(",")) {
                    if (!u.isEmpty()) {
                        try {
                            r.gemalen.add(UUID.fromString(u));
                        } catch (IllegalArgumentException ignored) {
                            // a broken entry: forget it
                        }
                    }
                }
                try {
                    s.runs.put(Long.parseLong(k), r);
                } catch (NumberFormatException ignored) {
                    // not one of ours
                }
            }
            return s;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            runs.forEach((k, r) -> {
                CompoundTag t = new CompoundTag();
                t.putLong("DraaiTot", r.draaiTot);
                t.putBoolean("Geschud", r.geschud);
                t.putString("Gemalen", String.join(",", r.gemalen.stream().map(UUID::toString).toList()));
                tag.put(Long.toString(k), t);
            });
            return tag;
        }
    }
}
