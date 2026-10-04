package nl.juiced.guhs.world;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.bibliotheek.Guhboek;
import nl.juiced.guhs.feature.gatenkaas.GatenkaasFeature;
import nl.juiced.guhs.feature.landdiertjes.Landdiertje;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.onderwater.OnderwaterProtection;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.piep.PiepSpawns;
import nl.juiced.guhs.feature.piep.PieppiepmuisjeEntity;
import nl.juiced.guhs.item.GuhCompassItem;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.2.7: the things of the world that used to be there for one player only, are there for everybody on a server:
 * <ul>
 *   <li>Big Mika comes back to his hall in a challenging guh cave a few days after he was beaten ({@link #bigMikaTerug}),
 *   and everybody near the fight gets the credit ({@link #bigMikaVerslagen});</li>
 *   <li>the treasure chest of the sunken guh ship counts for everybody who opens it, also when it's empty;</li>
 *   <li>the lectern of the Stille Voorraadkelder gives everybody their own copy of the Voorraadmika's diary;</li>
 *   <li>the pieppiepmuisjes and the little land animals (pluisegeltje, guh-konijntje, pluiseekhoorntje) get a gentle
 *   top-up around players, like the birds (come-and-go animals: {@link WildeDieren}).</li>
 * </ul>
 */
public final class VoorIedereen {
    // --- Big Mika -----------------------------------------------------------------------------------------------------------
    /** Where Big Mika stands in the template challenging_guh_caves/dungeon_hall (tools/make_structures.py), and his size. */
    public static final BlockPos BIG_MIKA_PLEK = new BlockPos(12, 2, 9);
    public static final String BIG_MIKA_HAL = "dungeon_hall", BIG_MIKA_SOORT = "big_mika";
    public static final double BIG_MIKA_SCHAAL = 2.5;
    /** A hall without Big Mika gets a new one after this long (three Minecraft days). */
    public static final long BIG_MIKA_NA = 3 * 24000L;
    /** Everybody this close when Big Mika is beaten gets the credit. */
    public static final double BIG_MIKA_BEREIK = 32;

    /** Big Mika is beaten: the killer and everybody near get the stomach quest's credit and the advancement. */
    public static List<ServerPlayer> bigMikaVerslagen(MikaEntity mika, @Nullable ServerPlayer killer) {
        List<ServerPlayer> helden = new ArrayList<>();
        if (!(mika.level() instanceof ServerLevel level)) {
            return helden;
        }
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.distanceToSqr(mika) <= BIG_MIKA_BEREIK * BIG_MIKA_BEREIK) {
                helden.add(p);
            }
        }
        if (killer != null && !helden.contains(killer)) {
            helden.add(killer);
        }
        GuhWorldData data = GuhWorldData.get(level.getServer());
        for (ServerPlayer p : helden) {
            data.player(p.getUUID()).beatBigMika = true;
            shown(p, "guhmension/defeat_big_mika");
        }
        if (!helden.isEmpty()) {
            data.setDirty();
        }
        return helden;
    }

    /** One hall: {@code plek} is Big Mika's spot, {@code hal} the room. Returns the new Big Mika when one came. */
    @Nullable
    public static MikaEntity bigMikaTerug(ServerLevel level, BlockPos plek, AABB hal) {
        boolean er = !level.getEntitiesOfClass(MikaEntity.class, hal, m -> m.isBoss() && m.isAlive()).isEmpty();
        if (!Terugkeer.moetTerug(level, BIG_MIKA_SOORT, plek, er, BIG_MIKA_NA)) {
            return null;
        }
        MikaEntity mika = ModEntities.MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        mika.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0);
        mika.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(BIG_MIKA_SCHAAL);
        mika.makeBoss();
        mika.setHealth(mika.getMaxHealth());
        level.addFreshEntity(mika);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, plek.getX() + 0.5, plek.getY() + 1.5, plek.getZ() + 0.5, 30, 0.8, 1, 0.8, 0.02);
        return mika;
    }

    private static AABB box(BoundingBox b, double extra) {
        return new AABB(b.minX(), b.minY(), b.minZ(), b.maxX() + 1, b.maxY() + 1, b.maxZ() + 1).inflate(extra);
    }

    // --- the periodic checks around a player ---------------------------------------------------------------------------------
    public static final int HAL_TIJD = 200, DIERTJES_TIJD = 900, MUISJES_TIJD = 1200;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        int t = player.tickCount + player.getId();
        ServerLevel level = player.level();
        if (t % HAL_TIJD == 0 && level.dimension() == ModDimensions.GUHMENSION) {
            for (PoolElementStructurePiece stuk : Terugkeer.stukken(level, GuhCompassItem.CHALLENGING_GUH_CAVES, player.blockPosition(), BIG_MIKA_HAL)) {
                // (he may have chased somebody out of his hall: look well around it)
                bigMikaTerug(level, Terugkeer.wereld(stuk, BIG_MIKA_PLEK), box(stuk.getBoundingBox(), 48));
            }
        }
        boolean spawns = level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS);
        if (spawns && t % DIERTJES_TIJD == 450) {
            diertjesAanvullen(player, player.getRandom());
        }
        if (spawns && t % MUISJES_TIJD == 750) {
            muisjeAanvullen(player, player.getRandom());
        }
    }

    // --- the sunken guh ship and the Voorraadmika's diary -----------------------------------------------------------------------
    /** The treasure chest in the wreck's cabin, in the template onderwater (tools/features/onderwater.py). */
    public static final BlockPos WRAK_KIST = new BlockPos(54, 11, 21);
    /** Player data (GuhQuests.saved): got their own copy of the Voorraadmika's diary from the lectern. */
    public static final String VOORRAADBOEK = "guhs_voorraadboek";

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || player.level().dimension() != ModDimensions.GUHMENSION) {
            return;
        }
        ServerLevel level = player.level();
        BlockPos pos = event.getPos();
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity && isWrakKist(level, pos)) {
            GuhAdvancements.grant(player, "onderwater_wrak");
        } else if (level.getBlockState(pos).getBlock() instanceof LecternBlock && !Terugkeer.stukken(level, GatenkaasFeature.VOORRAADKELDER, pos, null).isEmpty()) {
            voorraadboek(player);
        }
    }

    /** Is this the treasure chest of the sunken guh ship next to an underwater bubble? */
    public static boolean isWrakKist(ServerLevel level, BlockPos pos) {
        for (PoolElementStructurePiece stuk : Terugkeer.stukken(level, OnderwaterProtection.BUBBLE, pos, null)) {
            if (Terugkeer.wereld(stuk, WRAK_KIST).equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /** The Voorraadmika's diary: everybody gets their own copy at the lectern, once (the book on it is for the first one). */
    public static boolean voorraadboek(ServerPlayer player) {
        if (GuhQuests.saved(player).getBooleanOr(VOORRAADBOEK, false)) {
            return false;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (Guhboek.of(stack) == Guhboek.VOORRAADKELDER) {
                return false;
            }
        }
        GuhQuests.saved(player).putBoolean(VOORRAADBOEK, true);
        player.getInventory().placeItemBackInInventory(Guhboek.VOORRAADKELDER.stack());
        player.sendSystemMessage(Component.translatable("gui.guhs.gatenkaas.voorraadboek_kopie").withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        return true;
    }

    // --- top-ups -------------------------------------------------------------------------------------------------------------------
    /** No new land animals when this many are within 64 / 128 blocks of the player (tamed ones don't count). */
    public static final int DIERTJES_VOL = 4, DIERTJES_VOL_WIJD = 10;
    /** No new muisje when a wild one is within this many blocks, or this many (wild) within 128 blocks. */
    public static final double MUISJE_DICHTBIJ = 48;
    public static final int MUISJES_VOL_WIJD = 3;
    /** A structure guh this close to the player can get a muisje next to it; the chance per try. */
    public static final double MUISJE_GUH = 24;
    public static final int MUISJE_KANS = 3;

    /**
     * A few little land animals (the biome's own, from its spawn list) walk in 24 to 48 blocks from the player when there
     * are few around: they don't breed and vanilla only brings animals with the world, so on a server where everybody
     * tames them, they would run out. The new ones come and go ({@link WildeDieren}). Returns the animals that came.
     */
    public static List<Landdiertje> diertjesAanvullen(ServerPlayer player, RandomSource random) {
        List<Landdiertje> nieuw = new ArrayList<>();
        ServerLevel level = player.level();
        if (level.getEntitiesOfClass(Landdiertje.class, player.getBoundingBox().inflate(64), d -> !d.isTame() && isLanddiertje(d.getType())).size() >= DIERTJES_VOL
                || level.getEntitiesOfClass(Landdiertje.class, player.getBoundingBox().inflate(128), d -> !d.isTame() && isLanddiertje(d.getType())).size() >= DIERTJES_VOL_WIJD) {
            return nieuw;
        }
        double a = random.nextDouble() * Math.PI * 2, d = 24 + random.nextInt(25);
        int x = (int) Math.floor(player.getX() + Math.cos(a) * d), z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return nieuw;
        }
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        var spawners = level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .filter(sd -> isLanddiertje(sd.value().type())).toList();
        if (spawners.isEmpty()) {
            return nieuw;
        }
        int pick = random.nextInt(Math.max(1, spawners.stream().mapToInt(sd -> sd.weight()).sum()));
        var gekozen = spawners.get(0);
        for (var sd : spawners) {
            pick -= sd.weight();
            if (pick < 0) {
                gekozen = sd;
                break;
            }
        }
        @SuppressWarnings("unchecked")
        EntityType<? extends Landdiertje> type = (EntityType<? extends Landdiertje>) gekozen.value().type();
        int n = Math.min(2, gekozen.value().minCount() + random.nextInt(Math.max(1, gekozen.value().maxCount() - gekozen.value().minCount() + 1)));
        for (int i = 0; i < n; i++) {
            int px = x + random.nextInt(5) - 2, pz = z + random.nextInt(5) - 2;
            BlockPos p = new BlockPos(px, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz), pz);
            Landdiertje dier = diertjeOp(level, type, p, random);
            if (dier != null) {
                nieuw.add(dier);
            }
        }
        return nieuw;
    }

    /** One come-and-go land animal of this kind at this spot, when it may spawn there (its own spawn rules). Or null. */
    @Nullable
    public static Landdiertje diertjeOp(ServerLevel level, EntityType<? extends Landdiertje> type, BlockPos p, RandomSource random) {
        if (!level.hasChunkAt(p) || !SpawnPlacements.isSpawnPositionOk(type, level, p)
                || !SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, p, random)
                || !level.noCollision(type.getSpawnAABB(p.getX() + 0.5, p.getY(), p.getZ() + 0.5))) {
            return null;
        }
        Landdiertje dier = type.create(level, EntitySpawnReason.TRIGGERED);
        if (dier == null) {
            return null;
        }
        dier.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360f, 0);
        dier.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.NATURAL, null);
        WildeDieren.markeer(dier);
        level.addFreshEntity(dier);
        return dier;
    }

    public static boolean isLanddiertje(EntityType<?> type) {
        return type == LanddiertjesFeature.PLUISEGELTJE.get() || type == LanddiertjesFeature.GUH_KONIJNTJE.get()
                || type == LanddiertjesFeature.PLUISEEKHOORNTJE.get();
    }

    /**
     * A pieppiepmuisje comes to live next to a guh of a (lief) building near the player, when no wild muisje is around:
     * muisjes only came with the buildings (one chance per structure guh), so on a server they would all be tamed in the
     * end. The new one comes and goes ({@link WildeDieren}) until somebody tames it. Returns the muisje, or null.
     */
    @Nullable
    public static PieppiepmuisjeEntity muisjeAanvullen(ServerPlayer player, RandomSource random) {
        ServerLevel level = player.level();
        if (level.dimension() != ModDimensions.GUHMENSION || random.nextInt(MUISJE_KANS) != 0) {
            return null;
        }
        return muisjeBij(player, p -> inLiefGebouw(level, p));
    }

    /** The top-up itself, without the roll. {@code magHier}: may a muisje live at this guh's spot? */
    @Nullable
    public static PieppiepmuisjeEntity muisjeBij(ServerPlayer player, java.util.function.Predicate<BlockPos> magHier) {
        ServerLevel level = player.level();
        List<PieppiepmuisjeEntity> wild = level.getEntitiesOfClass(PieppiepmuisjeEntity.class, player.getBoundingBox().inflate(128), m -> !m.isTame());
        if (wild.size() >= MUISJES_VOL_WIJD || wild.stream().anyMatch(m -> m.distanceToSqr(player) <= MUISJE_DICHTBIJ * MUISJE_DICHTBIJ)) {
            return null;
        }
        List<BlockPos> plekken = new ArrayList<>();
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(MUISJE_GUH),
                g -> !g.isTame() && g.getClass() == GuhEntity.class && (g.isPersistenceRequired() || g.getSpawnType() == EntitySpawnReason.STRUCTURE))) {
            plekken.add(guh.blockPosition());
        }
        for (GuhNpcEntity npc : level.getEntitiesOfClass(GuhNpcEntity.class, player.getBoundingBox().inflate(MUISJE_GUH))) {
            plekken.add(npc.blockPosition());
        }
        plekken.removeIf(p -> !magHier.test(p));
        if (plekken.isEmpty()) {
            return null;
        }
        BlockPos pos = plekken.get(level.getRandom().nextInt(plekken.size()));
        PieppiepmuisjeEntity muis = PiepFeature.PIEPPIEPMUISJE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (muis == null) {
            return null;
        }
        BlockPos at = pos;
        for (int i = 0; i < 8; i++) {
            BlockPos p = pos.offset(level.getRandom().nextInt(3) - 1, 0, level.getRandom().nextInt(3) - 1);
            if (level.getBlockState(p).getCollisionShape(level, p).isEmpty() && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()) {
                at = p;
                break;
            }
        }
        muis.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
        muis.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.NATURAL, null);
        WildeDieren.markeer(muis);
        level.addFreshEntity(muis);
        return muis;
    }

    /** Inside one of our buildings where a muisje may live (PiepSpawns: not a Mika place)? */
    public static boolean inLiefGebouw(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos) || !PiepSpawns.magHier(level, pos)) {
            return false;
        }
        var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for (Structure s : level.structureManager().getAllStructuresAt(pos).keySet()) {
            var id = registry.getKey(s);
            if (id != null && id.getNamespace().equals(Guhs.MODID)) {
                return true;
            }
        }
        return false;
    }

    /** Grants one of our shown advancements (criterion "done"), whatever its trigger. */
    public static void shown(ServerPlayer player, String path) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id(path));
        if (holder != null && !player.getAdvancements().getOrStartProgress(holder).isDone()) {
            player.getAdvancements().award(holder, "done");
        }
    }

    private VoorIedereen() {
    }
}
