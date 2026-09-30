package nl.juiced.guhs.feature.guheinde;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import org.joml.Vector3f;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.EntitySpawnReason;
/**
 * The fight in the Guheinde (the EndDragonFight of the guhs), saved with the Guheinde level.
 * <ul>
 *   <li>The first time someone arrives: the Knabbelberg (template guheinde_knabbelberg, contract in
 *       tools/features/guheinde_bouw.py) goes on the island at 0,0 and eight kaaspilaren around it, each with a
 *       knabbelkristal on top (two in a cage). Then Opper-Mika arrives on his starved Enderguh.</li>
 *   <li>Crystals heal Opper-Mika (a beam, the nearest one); smashing one makes the Enderguh more vahoeg; all gone: the
 *       Enderguh is worn out and lands; feed it and Opper-Mika falls off; beat him on foot.</li>
 *   <li>The win ({@link #onOpperMikaKilled}): a rain of knabbels, rewards per player ({@link #reward}), the scoreboard, the
 *       knabbelschat opens (first time), the terugportaal lights up, and a new Knabbelpoort appears.</li>
 *   <li>Four knabbelkristallen on the four knabbelsokkels call him back ({@link #tryRespawn}).</li>
 * </ul>
 */
public class GuheindeGevecht extends SavedData {
    public static final String NAME = "guhs_guheinde";
    /** The Knabbelberg template (see guheinde_bouw.py): size, its middle and ground, and the plateau height. */
    public static final int BERG_SIZE_X = 49, BERG_SIZE_Y = 48, BERG_SIZE_Z = 49, BERG_CX = 24, BERG_CZ = 24, BERG_G = 12, BERG_TOP = 32;
    public static final int PILLARS = 8, PILLAR_RING = 42;
    public static final int GATEWAY_RING = 96, MAX_GATEWAYS = 20;
    /** Where you arrive (x, z): on the island, outside the ring of pillars. */
    public static final int ARRIVAL_X = 72, ARRIVAL_Z = 0;
    public static final int RAIN_TICKS = 200;
    /** Player data (GuhQuests.saved): how many times you beat Opper-Mika. */
    public static final String WINS = "guhs_guheinde_wins";

    public boolean islandBuilt;
    public int groundY = 64;
    public boolean everWon;
    public boolean treasureOpen;
    public boolean fightActive;
    @Nullable
    public UUID bossId, mountId;
    @Nullable
    public UUID feeder;
    public long fightStart;
    public int gateways;
    public int respawnTicks = -1;
    public int rainTicks;
    private final Set<UUID> participants = new HashSet<>();
    private int checkTicks;

    public static void register() {
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level && GuheindeFeature.isGuheinde(level)) {
                GuheindeGevecht fight = of(level);
                if (fight != null) {
                    fight.tick(level);
                }
            }
        });
    }

    /** The fight of this level: only the Guheinde has one. */
    @Nullable
    public static GuheindeGevecht of(Level level) {
        if (!(level instanceof ServerLevel server) || !GuheindeFeature.isGuheinde(server)) {
            return null;
        }
        return server.getDataStorage().computeIfAbsent(new SavedData.Factory<>(GuheindeGevecht::new, GuheindeGevecht::load, null), NAME);
    }

    // ------------------------------------------------------------------------------------------------------------
    // where everything is
    // ------------------------------------------------------------------------------------------------------------

    public int plateauY() {
        return groundY + (BERG_TOP - BERG_G);
    }

    public BlockPos center() {
        return new BlockPos(0, plateauY(), 0);
    }

    public BlockPos perch() {
        return new BlockPos(0, plateauY(), 7);
    }

    public List<BlockPos> sokkels() {
        int y = plateauY();
        return List.of(new BlockPos(3, y, 0), new BlockPos(-3, y, 0), new BlockPos(0, y, 3), new BlockPos(0, y, -3));
    }

    /** The eight cells of the terugportaal around the little column in the middle of the plateau. */
    public List<BlockPos> portalCells() {
        List<BlockPos> cells = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    cells.add(new BlockPos(dx, plateauY(), dz));
                }
            }
        }
        return cells;
    }

    public static int pillarRadius(int i) {
        return 2 + (i * 5 % 3);
    }

    public static boolean caged(int i) {
        return i == 1 || i == 5;
    }

    /** The top of pillar i: the crystal floats on the block above. */
    public BlockPos pillarTop(int i) {
        double a = Math.PI * 2 * i / PILLARS + Math.PI / PILLARS;
        int height = 18 + (i * 3 % 4) * 4;
        return new BlockPos(Mth.floor(Math.cos(a) * PILLAR_RING), groundY + height, Mth.floor(Math.sin(a) * PILLAR_RING));
    }

    public AABB bergBox() {
        BlockPos origin = bergOrigin();
        return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + BERG_SIZE_X, origin.getY() + BERG_SIZE_Y, origin.getZ() + BERG_SIZE_Z);
    }

    private BlockPos bergOrigin() {
        return new BlockPos(-BERG_CX, groundY - BERG_G, -BERG_CZ);
    }

    // ------------------------------------------------------------------------------------------------------------
    // ticking
    // ------------------------------------------------------------------------------------------------------------

    private void tick(ServerLevel level) {
        if (level.players().isEmpty()) {
            return;
        }
        if (!islandBuilt) {
            buildIsland(level);
            if (!everWon) {
                startFight(level);
            } else {
                openPortal(level);
            }
            return;
        }
        if (level.getGameTime() % 100 == 0 && level.isLoaded(new BlockPos(ARRIVAL_X, groundY, ARRIVAL_Z))) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ARRIVAL_X, ARRIVAL_Z + 4);
            Scorebord.show(level, new Vec3(ARRIVAL_X + 0.5, y + 2.5, ARRIVAL_Z + 4.5), "guheinde", board(level.getServer()));
        }
        if (rainTicks > 0) {
            rainTicks--;
            knabbelRain(level);
        }
        if (respawnTicks >= 0) {
            tickRespawn(level);
            return;
        }
        if (!fightActive) {
            return;
        }
        if (!level.isLoaded(center()) || level.getPlayers(p -> p.distanceToSqr(Vec3.atCenterOf(center())) < 160 * 160).isEmpty()) {
            return;
        }
        for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(Vec3.atCenterOf(center())) < 160 * 160 && !p.isSpectator())) {
            participants.add(player.getUUID());
        }
        OpperMikaEntity boss = boss(level);
        HongerigeEnderguhEntity mount = mount(level);
        // (after a restart the entities come back with their chunks: give them a moment before making new ones)
        if (boss == null && ++checkTicks > 200) {
            checkTicks = 0;
            spawnBoss(level, mount);
            return;
        }
        if (boss == null) {
            return;
        }
        checkTicks = 0;
        if (level.getGameTime() % 10 == 0) {
            beams(level, boss);
        }
    }

    @Nullable
    public OpperMikaEntity boss(ServerLevel level) {
        return bossId != null && level.getEntity(bossId) instanceof OpperMikaEntity boss && boss.isAlive() ? boss : null;
    }

    @Nullable
    public HongerigeEnderguhEntity mount(ServerLevel level) {
        return mountId != null && level.getEntity(mountId) instanceof HongerigeEnderguhEntity mount && mount.isAlive() ? mount : null;
    }

    public List<KnabbelkristalEntity> pillarCrystals(ServerLevel level) {
        List<KnabbelkristalEntity> list = new ArrayList<>();
        for (int i = 0; i < PILLARS; i++) {
            BlockPos top = pillarTop(i);
            list.addAll(level.getEntitiesOfClass(KnabbelkristalEntity.class, new AABB(top).inflate(3)));
        }
        return list;
    }

    /** The nearest crystal heals Opper-Mika (1 HP every half second), with a beam to show it. */
    private void beams(ServerLevel level, OpperMikaEntity boss) {
        KnabbelkristalEntity nearest = null;
        double best = 48 * 48;
        List<KnabbelkristalEntity> crystals = pillarCrystals(level);
        for (KnabbelkristalEntity c : crystals) {
            double d = c.distanceToSqr(boss);
            if (d < best) {
                best = d;
                nearest = c;
            }
        }
        for (KnabbelkristalEntity c : crystals) {
            c.setBeamTarget(c == nearest ? boss.blockPosition().above() : null);
        }
        if (nearest != null && boss.getHealth() < boss.getMaxHealth()) {
            boss.heal(1f);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // building the island
    // ------------------------------------------------------------------------------------------------------------

    public void buildIsland(ServerLevel level) {
        level.getChunk(0, 0);
        int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0);
        groundY = h > level.getMinY() + 8 ? h : 64;
        placeBerg(level);
        for (int i = 0; i < PILLARS; i++) {
            buildPillar(level, i, true);
        }
        islandBuilt = true;
        setDirty();
    }

    public boolean placeBerg(ServerLevel level) {
        StructureTemplate template = level.getStructureManager().get(Guhs.id("guheinde_knabbelberg")).orElse(null);
        if (template == null) {
            com.mojang.logging.LogUtils.getLogger().warn("Guheinde: the Knabbelberg template is missing");
            return false;
        }
        BlockPos origin = bergOrigin();
        for (int cx = (origin.getX() >> 4) - 1; cx <= ((origin.getX() + BERG_SIZE_X) >> 4) + 1; cx++) {
            for (int cz = (origin.getZ() >> 4) - 1; cz <= ((origin.getZ() + BERG_SIZE_Z) >> 4) + 1; cz++) {
                level.getChunk(cx, cz);
            }
        }
        template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), 2);
        return true;
    }

    /** A kaaspilaar: a round column of kaaskorststenen with a ring of knabbels, from the ground to its top. */
    public void buildPillar(ServerLevel level, int i, boolean crystal) {
        BlockPos top = pillarTop(i);
        int r = pillarRadius(i);
        level.getChunk(top.getX() >> 4, top.getZ() >> 4);
        int bottom = Math.min(groundY - 10, level.getHeight(Heightmap.Types.WORLD_SURFACE, top.getX(), top.getZ()) - 6);
        BlockState stone = GuheindeFeature.KAASKORST_STENEN.get().defaultBlockState();
        BlockState band = nl.juiced.guhs.registry.ModBlocks.BLOCK_OF_KAASKNABBELS.get().defaultBlockState();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (x * x + z * z > r * r + 1) {
                    continue;
                }
                for (int y = bottom; y <= top.getY(); y++) {
                    level.setBlock(top.offset(x, y - top.getY(), z), (y - groundY) % 6 == 5 ? band : stone, 3);
                }
                for (int y = top.getY() + 1; y <= top.getY() + 5; y++) {
                    level.setBlock(top.offset(x, y - top.getY(), z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        if (caged(i)) {
            BlockState bars = Blocks.IRON_BARS.defaultBlockState();
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = 1; y <= 4; y++) {
                        if (Math.abs(x) == 2 || Math.abs(z) == 2 || y == 4) {
                            BlockPos p = top.offset(x, y, z);
                            level.setBlock(p, bars, 3);
                        }
                    }
                }
            }
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = 1; y <= 4; y++) {
                        BlockPos p = top.offset(x, y, z);
                        BlockState s = level.getBlockState(p);
                        level.setBlock(p, s.updateShape(net.minecraft.core.Direction.UP, s, level, p, p), 3);
                    }
                }
            }
        }
        if (crystal) {
            for (KnabbelkristalEntity old : level.getEntitiesOfClass(KnabbelkristalEntity.class, new AABB(top).inflate(3))) {
                old.discard();
            }
            KnabbelkristalEntity c = new KnabbelkristalEntity(level, top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5);
            c.setShowBottom(true);
            c.setInvulnerable(false);
            level.addFreshEntity(c);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // the fight
    // ------------------------------------------------------------------------------------------------------------

    public void startFight(ServerLevel level) {
        closePortal(level);
        fightActive = true;
        feeder = null;
        participants.clear();
        fightStart = level.getGameTime();
        spawnBoss(level, null);
        broadcast(level, "gui.guhs.guheinde.aankomst", ChatFormatting.DARK_PURPLE);
        setDirty();
    }

    private void spawnBoss(ServerLevel level, @Nullable HongerigeEnderguhEntity mount) {
        if (mount == null) {
            mount = GuheindeFeature.HONGERIGE_ENDERGUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (mount == null) {
                return;
            }
            mount.snapTo(0.5, plateauY() + 30, -70.5, 0f, 0f);
            mount.setVahoeg(PILLARS - pillarCrystalsLeft(level));
            level.addFreshEntity(mount);
        }
        mount.setArena(center(), perch());
        OpperMikaEntity boss = GuheindeFeature.OPPER_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        if (boss == null) {
            return;
        }
        boss.snapTo(mount.getX(), mount.getY() + 2, mount.getZ(), 0f, 0f);
        level.addFreshEntity(boss);
        if (mount.getToestand() != HongerigeEnderguhEntity.Toestand.VRIJ) {
            boss.startRiding(mount, true);
        }
        bossId = boss.getUUID();
        mountId = mount.getUUID();
        setDirty();
    }

    public int pillarCrystalsLeft(ServerLevel level) {
        return pillarCrystals(level).size();
    }

    /** A knabbelkristal was smashed (on a pillar or on a sokkel). */
    public void onCrystalSmashed(KnabbelkristalEntity crystal, DamageSource source) {
        ServerLevel level = (ServerLevel) crystal.level();
        if (respawnTicks >= 0 && sokkels().stream().anyMatch(s -> crystal.blockPosition().closerThan(s.above(), 2))) {
            respawnTicks = -1; // smashed while calling him back: that call is off
            setDirty();
            return;
        }
        if (!fightActive) {
            return;
        }
        HongerigeEnderguhEntity mount = mount(level);
        if (mount == null) {
            return;
        }
        mount.addVahoeg();
        int left = (int) pillarCrystals(level).stream().filter(c -> c != crystal && !c.isRemoved()).count();
        if (source.getEntity() instanceof ServerPlayer player) {
            GuheindeEvents.advancement(player, "guheinde_kristal");
        }
        if (left == 0 && mount.getToestand() != HongerigeEnderguhEntity.Toestand.VRIJ) {
            mount.exhaust();
            broadcast(level, "gui.guhs.guheinde.uitgeput", ChatFormatting.LIGHT_PURPLE);
        } else {
            broadcast(level, Component.translatable("gui.guhs.guheinde.kristal_kapot", left), ChatFormatting.GOLD);
        }
    }

    public void onEnderguhFed(HongerigeEnderguhEntity mount, @Nullable ServerPlayer player) {
        if (player != null) {
            feeder = player.getUUID();
            GuheindeEvents.advancement(player, "guheinde_voeren");
        }
        broadcast((ServerLevel) mount.level(), "gui.guhs.guheinde.gevoerd", ChatFormatting.LIGHT_PURPLE);
        setDirty();
    }

    public void onOpperMikaKilled(OpperMikaEntity boss, DamageSource source) {
        ServerLevel level = (ServerLevel) boss.level();
        if (!fightActive && !boss.getUUID().equals(bossId)) {
            return;
        }
        fightActive = false;
        boolean firstEver = !everWon;
        everWon = true;
        bossId = null;
        rainTicks = RAIN_TICKS;
        int seconds = (int) ((level.getGameTime() - fightStart) / 20);
        List<ServerPlayer> winners = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && (participants.contains(p.getUUID()) || p.distanceToSqr(boss) < 128 * 128)) {
                winners.add(p);
            }
        }
        if (source.getEntity() instanceof ServerPlayer killer && !winners.contains(killer)) {
            winners.add(killer);
        }
        HongerigeEnderguhEntity mount = mount(level);
        ServerPlayer mountFor = null;
        for (ServerPlayer p : winners) {
            if (GuhQuests.saved(p).getIntOr(WINS, 0) == 0 && (mountFor == null || p.getUUID().equals(feeder))) {
                mountFor = p;
            }
        }
        for (ServerPlayer p : winners) {
            reward(p, p == mountFor ? mount : null, seconds);
        }
        if (mount != null && mount.isAlive()) {
            // nobody new to take it home: it flies back to the Guhpieken
            level.sendParticles(ParticleTypes.PORTAL, mount.getX(), mount.getY() + 2, mount.getZ(), 100, 2, 2, 2, 0.5);
            mount.discard();
        }
        mountId = null;
        for (int i = 0; i < 12; i++) {
            ExperienceOrb.award(level, boss.position(), firstEver ? 1000 : 250);
        }
        if (!treasureOpen) {
            openTreasure(level);
        }
        openPortal(level);
        spawnGateway(level);
        setDirty();
    }

    /**
     * A player's reward for a win: the first time the Knabbelkroon, the trophy and a tamed Vahoege Enderguh (the freed
     * one when given, otherwise a fresh one); after that an Enderguh-ei. Plus the scoreboard and advancements.
     */
    public static void reward(ServerPlayer player, @Nullable HongerigeEnderguhEntity freed, int seconds) {
        int wins = GuhQuests.saved(player).getIntOr(WINS, 0);
        GuhQuests.saved(player).putInt(WINS, wins + 1);
        GuheindeEvents.advancement(player, "guheinde_winst");
        if (wins == 0) {
            GuhQuests.give(player, GuheindeFeature.KNABBELKROON.get());
            GuhQuests.give(player, GuheindeFeature.OPPER_MIKATROFEE.get().asItem());
            Vec3 at = freed != null ? freed.position() : player.position().add(2, 1, 0);
            GuhEntity guh = ModEntities.GUH.get().create(player.level(), EntitySpawnReason.TRIGGERED);
            if (guh != null) {
                guh.setVariant(GuhVariant.VAHOEGE_ENDER);
                guh.setGuhScale(1.7f);
                guh.snapTo(at.x, at.y, at.z, player.getYRot(), 0f);
                guh.tame(player);
                guh.equipSaddle(new ItemStack(net.minecraft.world.item.Items.SADDLE), null);
                guh.setCustomName(Component.translatable("entity.guhs.guh.vahoege_ender"));
                guh.setPersistenceRequired();
                player.level().addFreshEntity(guh);
                player.level().sendParticles(ParticleTypes.HEART, at.x, at.y + 2, at.z, 15, 1.5, 1, 1.5, 0);
            }
            if (freed != null) {
                freed.discard();
            }
            player.sendSystemMessage(Component.translatable("gui.guhs.guheinde.beloning.eerste").withStyle(ChatFormatting.GOLD));
        } else {
            GuhQuests.give(player, GuheindeFeature.ENDERGUH_EI.get().asItem());
            GuheindeEvents.advancement(player, "guheinde_nog_eens");
            player.sendSystemMessage(Component.translatable("gui.guhs.guheinde.beloning.ei").withStyle(ChatFormatting.GOLD));
        }
        if (seconds > 0) {
            Scorebord.submit(player, "guheinde", seconds, true);
            if (seconds <= 300) {
                GuheindeEvents.advancement(player, "guheinde_snel");
            }
        }
    }

    /** The knabbels come home: they rain down on the island for a while (and some guhs come to eat them). */
    private void knabbelRain(ServerLevel level) {
        if (level.getGameTime() % 2 != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(center())) > 120 * 120) {
                continue;
            }
            double x = player.getX() + level.getRandom().nextGaussian() * 10, z = player.getZ() + level.getRandom().nextGaussian() * 10;
            ItemEntity knabbel = new ItemEntity(level, x, player.getY() + 18, z,
                    new ItemStack(level.getRandom().nextInt(8) == 0 ? ModItems.GEFRITUURDE_KAASKNABBELS.get() : ModItems.KAAS_KNABBELS.get()));
            knabbel.setDeltaMovement(0, -0.3, 0);
            level.addFreshEntity(knabbel);
            if (level.getRandom().nextInt(4) == 0) {
                level.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.82f, 0.3f), 2f), x, player.getY() + 16, z, 6, 1, 0.5, 1, 0);
            }
        }
    }

    /** The first win opens Opper-Mika's knabbelschat under the Knabbelberg (its door of knabbelsloten goes). */
    public void openTreasure(ServerLevel level) {
        treasureOpen = true;
        AABB box = bergBox();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = (int) box.minX; x < box.maxX; x++) {
            for (int y = (int) box.minY; y < box.maxY; y++) {
                for (int z = (int) box.minZ; z < box.maxZ; z++) {
                    p.set(x, y, z);
                    if (level.getBlockState(p).is(GuheindeFeature.KNABBELSLOT.get())) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        level.sendParticles(ParticleTypes.POOF, x + 0.5, y + 0.5, z + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }
        }
        broadcast(level, "gui.guhs.guheinde.schat_open", ChatFormatting.GOLD);
        setDirty();
    }

    public void openPortal(ServerLevel level) {
        for (BlockPos cell : portalCells()) {
            level.setBlock(cell, GuheindeFeature.GUHEINDE_PORTAAL.get().defaultBlockState(), 3);
        }
    }

    public void closePortal(ServerLevel level) {
        for (BlockPos cell : portalCells()) {
            if (level.getBlockState(cell).is(GuheindeFeature.GUHEINDE_PORTAAL.get())) {
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /** A new Knabbelpoort on a ring around the island (20 at most), on a little pad of kaaskorststenen. */
    public void spawnGateway(ServerLevel level) {
        if (gateways >= MAX_GATEWAYS) {
            return;
        }
        double a = Math.PI * 2 * ((gateways * 7) % MAX_GATEWAYS) / MAX_GATEWAYS;
        gateways++;
        int x = Mth.floor(Math.cos(a) * GATEWAY_RING), z = Mth.floor(Math.sin(a) * GATEWAY_RING);
        level.getChunk(x >> 4, z >> 4);
        int y = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), groundY) + 1;
        BlockPos poort = new BlockPos(x, y, z);
        GuheindeReis.buildPoort(level, poort, null, false);
        level.playSound(null, poort, SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 3f, 1.2f);
        setDirty();
    }

    // ------------------------------------------------------------------------------------------------------------
    // calling Opper-Mika back
    // ------------------------------------------------------------------------------------------------------------

    /** Is there a knabbelkristal on all four sokkels? Then (when he isn't here) the call starts. */
    public void tryRespawn(@Nullable Player by) {
        if (fightActive || respawnTicks >= 0 || !islandBuilt) {
            return;
        }
        ServerLevel level = null;
        for (BlockPos s : sokkels()) {
            if (by == null || !(by.level() instanceof ServerLevel l)) {
                return;
            }
            level = l;
            if (l.getEntitiesOfClass(KnabbelkristalEntity.class, new AABB(s.above()).inflate(0.6)).isEmpty()) {
                return;
            }
        }
        if (level == null) {
            return;
        }
        respawnTicks = 0;
        broadcast(level, "gui.guhs.guheinde.oproep", ChatFormatting.DARK_PURPLE);
        setDirty();
    }

    private void tickRespawn(ServerLevel level) {
        respawnTicks++;
        List<KnabbelkristalEntity> sokkelCrystals = new ArrayList<>();
        for (BlockPos s : sokkels()) {
            sokkelCrystals.addAll(level.getEntitiesOfClass(KnabbelkristalEntity.class, new AABB(s.above()).inflate(0.6)));
        }
        if (sokkelCrystals.size() < 4) {
            respawnTicks = -1;
            return;
        }
        BlockPos sky = center().above(30);
        for (KnabbelkristalEntity c : sokkelCrystals) {
            c.setBeamTarget(sky);
        }
        if (respawnTicks % 20 == 0) {
            level.playSound(null, center(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 3f, 0.6f + respawnTicks / 200f);
        }
        if (respawnTicks >= 60 && respawnTicks < 60 + PILLARS * 10 && (respawnTicks - 60) % 10 == 0) {
            int i = (respawnTicks - 60) / 10;
            buildPillar(level, i, true);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pillarTop(i).getX(), pillarTop(i).getY() + 2, pillarTop(i).getZ(), 1, 0, 0, 0, 0);
        }
        if (respawnTicks >= 60 + PILLARS * 10 + 40) {
            for (KnabbelkristalEntity c : sokkelCrystals) {
                c.discard();
            }
            respawnTicks = -1;
            startFight(level);
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    private static void broadcast(ServerLevel level, String key, ChatFormatting colour) {
        broadcast(level, Component.translatable(key), colour);
    }

    private static void broadcast(ServerLevel level, Component text, ChatFormatting colour) {
        Component line = text.copy().withStyle(colour);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(line);
        }
    }

    /** Board text for the scoreboard at the arrival platform. */
    public static Component board(net.minecraft.server.MinecraftServer server) {
        return Scorebord.text(server, Component.translatable("gui.guhs.guheinde.scorebord"), List.of("guheinde"),
                List.of(Component.translatable("gui.guhs.guheinde.scorebord.snelst")), s -> (s / 60) + ":" + String.format("%02d", s % 60));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("IslandBuilt", islandBuilt);
        tag.putInt("GroundY", groundY);
        tag.putBoolean("EverWon", everWon);
        tag.putBoolean("TreasureOpen", treasureOpen);
        tag.putBoolean("FightActive", fightActive);
        if (bossId != null) {
            tag.store("Boss", UUIDUtil.CODEC, bossId);
        }
        if (mountId != null) {
            tag.store("Mount", UUIDUtil.CODEC, mountId);
        }
        if (feeder != null) {
            tag.store("Feeder", UUIDUtil.CODEC, feeder);
        }
        tag.putLong("FightStart", fightStart);
        tag.putInt("Gateways", gateways);
        ListTag list = new ListTag();
        for (UUID id : participants) {
            list.add(new net.minecraft.nbt.IntArrayTag(UUIDUtil.uuidToIntArray(id)));
        }
        tag.put("Participants", list);
        return tag;
    }

    public static GuheindeGevecht load(CompoundTag tag, HolderLookup.Provider registries) {
        GuheindeGevecht f = new GuheindeGevecht();
        f.islandBuilt = tag.getBooleanOr("IslandBuilt", false);
        f.groundY = tag.contains("GroundY") ? tag.getIntOr("GroundY", 0) : 64;
        f.everWon = tag.getBooleanOr("EverWon", false);
        f.treasureOpen = tag.getBooleanOr("TreasureOpen", false);
        f.fightActive = tag.getBooleanOr("FightActive", false);
        f.bossId = tag.read("Boss", UUIDUtil.CODEC).isPresent() ? tag.read("Boss", UUIDUtil.CODEC).orElseThrow() : null;
        f.mountId = tag.read("Mount", UUIDUtil.CODEC).isPresent() ? tag.read("Mount", UUIDUtil.CODEC).orElseThrow() : null;
        f.feeder = tag.read("Feeder", UUIDUtil.CODEC).isPresent() ? tag.read("Feeder", UUIDUtil.CODEC).orElseThrow() : null;
        f.fightStart = tag.getLongOr("FightStart", 0L);
        f.gateways = tag.getIntOr("Gateways", 0);
        for (Tag t : tag.getListOrEmpty("Participants")) {
            f.participants.add(UUIDUtil.uuidFromIntArray(((net.minecraft.nbt.IntArrayTag) t).getAsIntArray()));
        }
        return f;
    }
}
