package nl.juiced.guhs.feature.bio.bouwmeer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.bio.Bio;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.world.Terugkeer;

/**
 * De picknickmand of a picknickeilandje: the "chest with treats" of the hanami guhs, and the anchor of their picnic.
 * <ul>
 *   <li>Right-click: something nice from the loot table {@code guhs:chests/picknickeilandje} for THIS player, once per
 *       mand (remembered in the player's own data, so every player on a server gets theirs and nobody can empty it for
 *       the others).</li>
 *   <li>{@link #zorg} (BouwMeerEvents, every two seconds while a player is near): the three hanami guhs sit on their
 *       spots on the rug (a missing one comes back, a double goes), awake by day and asleep from dusk; and the first
 *       time, paper lanterns are hung in whatever blossom tree stands over the rug ({@link #versier}), so the picnic
 *       fits the island's own tree whatever shape the landscape gave it.</li>
 * </ul>
 * It faces the tree. Not obtainable and unbreakable in survival: it is part of the picnic.
 */
public class MandBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<MandBlock> CODEC = simpleCodec(MandBlock::new);
    /** The lanterns were hung in the tree. */
    public static final BooleanProperty VERSIERD = BooleanProperty.create("versierd");
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/picknickeilandje"));
    /** The player's own data: the manden (positions) they already had their treat from; the newest {@link #ONTHOUD}. */
    public static final String GEHAD = "guhs_bio_bouw_meer_manden";
    public static final int ONTHOUD = 64;
    /** The lanterns: how many, how far from the mand they may hang (flat / up), and how far apart at least. */
    public static final int LAMPIONNEN = 6, LAMP_STRAAL = 7, LAMP_HOOG = 10, LAMP_LAAG = 3, LAMP_TUSSEN = 3;
    private static final String[] LAMP_KLEUR = {"lampion_roze", "lampion_geel", "lampion_roze", "lampion_mint"};
    private static final VoxelShape VORM = Block.box(2, 0, 3, 14, 9, 13);

    /**
     * A seat on the rug, in the template's frame from the mand: east, up, south (north = the tree); which way the guh
     * faces (quarter turns clockwise from "towards the tree"); wakker = its kind by day; altijdSlaap = the one that naps
     * all day.
     */
    public record Zitplek(String naam, double oost, double op, double zuid, int draai, GuhNpcEntity.Kind wakker, GuhNpcEntity.Kind slaap, boolean altijdSlaap) {
    }

    public static final List<Zitplek> ZITPLEKKEN = List.of(
            new Zitplek("bloesem", -2.0, 0.0, 1.0, 0, GuhNpcEntity.Kind.HANAMI_BLOESEMGUH, GuhNpcEntity.Kind.HANAMI_BLOESEMGUH_SLAAPT, false),
            new Zitplek("guh", 0.0, 0.0, 1.0, 0, GuhNpcEntity.Kind.HANAMI_GUH, GuhNpcEntity.Kind.HANAMI_GUH_SLAAPT, false),
            new Zitplek("slaper", -3.0, 0.0, -1.0, 1, GuhNpcEntity.Kind.HANAMI_GUH, GuhNpcEntity.Kind.HANAMI_GUH_SLAAPT, true));

    public MandBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VERSIERD, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VERSIERD);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            pak(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // --- the treat ------------------------------------------------------------------------------------------------------------

    public static boolean gehad(ServerPlayer p, BlockPos mand) {
        for (long l : GuhQuests.saved(p).getLongArray(GEHAD).orElse(new long[0])) {
            if (l == mand.asLong()) {
                return true;
            }
        }
        return false;
    }

    /** This player's treat from this mand (once); false when they already had it. */
    public static boolean pak(ServerPlayer p, BlockPos mand) {
        ServerLevel level = p.level();
        if (gehad(p, mand)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.picknickeilandje.mand.gehad").withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, mand, SoundEvents.BAMBOO_WOOD_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7f, 1.2f);
            return false;
        }
        CompoundTag data = GuhQuests.saved(p);
        long[] oud = data.getLongArray(GEHAD).orElse(new long[0]);
        int houd = Math.min(oud.length, ONTHOUD - 1);
        long[] nieuw = new long[houd + 1];
        System.arraycopy(oud, oud.length - houd, nieuw, 0, houd);
        nieuw[houd] = mand.asLong();
        data.putLongArray(GEHAD, nieuw);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(LOOT);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(mand))
                .withParameter(LootContextParams.THIS_ENTITY, p).withLuck(p.getLuck()).create(LootContextParamSets.CHEST);
        for (ItemStack stack : table.getRandomItems(params)) {
            Minigames.give(p, stack);
        }
        level.playSound(null, mand, SoundEvents.BAMBOO_WOOD_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8f, 1.1f);
        level.playSound(null, mand, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.3f);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, mand.getX() + 0.5, mand.getY() + 1.0, mand.getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0.01);
        p.sendSystemMessage(Component.translatable("gui.guhs.picknickeilandje.mand.pak").withStyle(ChatFormatting.LIGHT_PURPLE));
        GuhAdvancements.grant(p, "picknickeilandje_mand");
        return true;
    }

    // --- upkeep ---------------------------------------------------------------------------------------------------------------

    static String terugNaam(Zitplek z) {
        return "bio_bouw_meer_hanami_" + z.naam();
    }

    /** Where this seat is for a mand that faces {@code kijk}. */
    public static Vec3 zit(BlockPos mand, Direction kijk, Zitplek z) {
        return MeerpaalBlock.punt(mand, kijk, z.oost(), z.op(), z.zuid());
    }

    private static float yaw(Direction kijk, Zitplek z) {
        Direction d = kijk;
        for (int i = 0; i < z.draai(); i++) {
            d = d.getClockWise();
        }
        return d.toYRot();
    }

    /** Keeps this picnic in order (see the class text); returns how many guhs it brought back. */
    public static int zorg(ServerLevel level, BlockPos mand) {
        BlockState state = level.getBlockState(mand);
        if (!(state.getBlock() instanceof MandBlock)) {
            return 0;
        }
        Direction kijk = state.getValue(FACING);
        boolean nacht = Klok.slaaptijd(level, mand);
        int nieuw = 0;
        for (Zitplek z : ZITPLEKKEN) {
            Vec3 zit = zit(mand, kijk, z);
            List<GuhNpcEntity> hier = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(zit, zit).inflate(0.9, 1.5, 0.9),
                    n -> Hanami.isHanami(n.getKind()) && n.isAlive());
            hier.sort(Comparator.comparingDouble(n -> n.distanceToSqr(zit)));
            for (int i = 1; i < hier.size(); i++) {
                hier.get(i).discard();
            }
            GuhNpcEntity.Kind soort = nacht || z.altijdSlaap() ? z.slaap() : z.wakker();
            GuhNpcEntity npc = hier.isEmpty() ? null : hier.get(0);
            if (Terugkeer.moetTerug(level, terugNaam(z), BlockPos.containing(zit), npc != null, 0)) {
                npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
                if (npc == null) {
                    continue;
                }
                npc.setKind(soort);
                npc.snapTo(zit.x, zit.y, zit.z, yaw(kijk, z), 0f);
                npc.setYHeadRot(yaw(kijk, z));
                npc.setYBodyRot(yaw(kijk, z));
                npc.setPersistenceRequired();
                level.addFreshEntity(npc);
                nieuw++;
            }
            if (npc != null) {
                if (npc.getKind() != soort) {
                    npc.setKind(soort);
                }
                Hanami.richt(npc, mand, kijk, z);
            }
        }
        if (!state.getValue(VERSIERD) && level.getGameTime() % 200 < BouwMeerEvents.ZORG_STAP && versier(level, mand, kijk) > 0) {
            level.setBlock(mand, state.setValue(VERSIERD, true), Block.UPDATE_CLIENTS);
        }
        return nieuw;
    }

    /**
     * Hangs up to {@link #LAMPIONNEN} paper lanterns in the blossom over the rug: under the lowest leaves around the
     * mand, spread out, each from a little twig (the leaf above it becomes a piece of the tree's wood, because a lantern
     * cannot hang from a leaf). Returns how many now hang there.
     */
    public static int versier(ServerLevel level, BlockPos mand, Direction kijk) {
        List<BlockPos> plekken = new ArrayList<>();
        int hangen = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -LAMP_STRAAL; dx <= LAMP_STRAAL; dx++) {
            for (int dz = -LAMP_STRAAL; dz <= LAMP_STRAAL; dz++) {
                if (dx * dx + dz * dz > LAMP_STRAAL * LAMP_STRAAL) {
                    continue;
                }
                for (int dy = LAMP_LAAG; dy <= LAMP_HOOG; dy++) {
                    p.set(mand.getX() + dx, mand.getY() + dy, mand.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof LanternBlock) {
                        hangen++;
                        break;
                    }
                    if (s.is(BlockTags.LEAVES)) {
                        // the lowest leaf of this column, with air under it and more leaf above it (inside the crown)
                        if (level.getBlockState(p.below()).isAir() && level.getBlockState(p.above()).is(BlockTags.LEAVES)) {
                            plekken.add(p.below().immutable());
                        }
                        break;
                    }
                    if (!s.isAir()) {
                        break;
                    }
                }
            }
        }
        // nearest to the rug first, but spread: a fixed order, so every picnic hangs its lanterns the same way
        plekken.sort(Comparator.comparingDouble((BlockPos q) -> q.distSqr(mand)).thenComparingLong(BlockPos::asLong));
        List<BlockPos> gekozen = new ArrayList<>();
        BlockState twijg = Bio.blok("guhbloesem_log", Blocks.CHERRY_LOG).defaultBlockState();
        if (twijg.hasProperty(RotatedPillarBlock.AXIS)) {
            twijg = twijg.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        }
        for (BlockPos q : plekken) {
            if (hangen + gekozen.size() >= LAMPIONNEN) {
                break;
            }
            if (gekozen.stream().anyMatch(g -> g.distManhattan(q) < LAMP_TUSSEN)) {
                continue;
            }
            Block lamp = Bio.blok(LAMP_KLEUR[gekozen.size() % LAMP_KLEUR.length], Blocks.LANTERN);
            BlockState hangend = lamp.defaultBlockState();
            if (hangend.hasProperty(LanternBlock.HANGING)) {
                hangend = hangend.setValue(LanternBlock.HANGING, true);
            }
            level.setBlock(q.above(), twijg, Block.UPDATE_CLIENTS);
            level.setBlock(q, hangend, Block.UPDATE_CLIENTS);
            gekozen.add(q);
        }
        return hangen + gekozen.size();
    }
}
