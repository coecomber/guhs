package nl.juiced.guhs.feature.ring;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.storage.Nbt;

/**
 * bbq2 (ring-kern): the three gifts of Guhladriel (chapter 4 hands them out with {@link #geef}; chapters 5 and 6 use them).
 * <ul>
 *   <li><b>Lichtflesje</b> ({@link Lichtflesje}): a flash that blinds the Nine around you for a while ({@link Negen#verblind})
 *       and "blows the smoke away" (what smoke is, is the chapter's business: {@link #BIJ_LICHT}). In the hand it is a lamp:
 *       a soft light walks along with you, in the story and for ever after.</li>
 *   <li><b>Elfenmanteltje</b> ({@link Manteltje}): with it in your pockets, crouch and stand still: you look like a rock
 *       ({@link #isRots}; the Eye and the patrols of the Nine look past a rock). Afterwards the same cloak is an outfit for
 *       your guhs (the reward {@code RING_ELFENMANTEL}).</li>
 *   <li><b>Elfentouw</b> ({@link Touw}) and the <b>Elfentouwhaak</b> ({@link Haak}): aim at a hook within
 *       {@link #TOUW_BEREIK} blocks and use the rope: it pulls you up to the hook. The climb of chapter 6 has fixed hooks;
 *       afterwards it works on hooks you place yourself.</li>
 * </ul>
 * None of it hurts anybody; a pull never ends in a fall (the fall distance is reset).
 */
public final class Gaven {
    /** The rope reaches this far. */
    public static final int TOUW_BEREIK = 24;
    /** Crouch and stand still this long to become a rock. */
    public static final int ROTS_NA = 15;
    /** The flash: how far it blinds, for how long, and how long the flask needs to glow up again. */
    public static final int FLITS_STRAAL = 12, FLITS_TICKS = 160, FLITS_WACHT = 100;
    /** The entity tag of the rock (a block display) over a hiding player. */
    public static final String ROTS_TAG = "guhs_ring_rots";
    private static final int LAMP_LICHT = 13;

    /** Chapters: somebody used the Lichtflesje here (blow your smoke away, light your lamps...). */
    public static final List<BiConsumer<ServerPlayer, Vec3>> BIJ_LICHT = new CopyOnWriteArrayList<>();
    /** Chapters: a player reached a hook with the rope (the hook's position). */
    public static final List<BiConsumer<ServerPlayer, BlockPos>> BIJ_HAAK = new CopyOnWriteArrayList<>();

    private record Stil(Vec3 plek, int ticks) {
    }

    private record Klim(BlockPos haak, long begin) {
    }

    private record Lamp(ResourceKey<Level> dim, BlockPos pos) {
    }

    private static final Map<UUID, Stil> STIL = new ConcurrentHashMap<>();
    /** The players who are a rock right now, with their rock (the display entity). */
    private static final Map<UUID, UUID> ROTSEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Klim> KLIMMERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Lamp> LAMPEN = new ConcurrentHashMap<>();

    // =====================================================================================================================
    // handing them out
    // =====================================================================================================================

    /** Gives the three gifts (each only when the player doesn't carry it: also the way to give a lost one again). */
    public static void geef(ServerPlayer p) {
        for (Item gift : List.of(RingFeature.LICHTFLESJE.get(), RingFeature.ELFENMANTELTJE.get(), RingFeature.ELFENTOUW.get())) {
            geefAlsKwijt(p, gift);
        }
        Ring.behaald(p, "ring_gaven");
    }

    /** Gives this gift when the player doesn't carry it. */
    public static boolean geefAlsKwijt(ServerPlayer p, Item gift) {
        if (p.getInventory().contains(s -> s.is(gift))) {
            return false;
        }
        nl.juiced.guhs.feature.Minigames.give(p, new ItemStack(gift));
        return true;
    }

    public static boolean heeft(Player p, Item gift) {
        return p.getInventory().contains(s -> s.is(gift));
    }

    // =====================================================================================================================
    // the Lichtflesje
    // =====================================================================================================================

    public static class Lichtflesje extends Item {
        public Lichtflesje(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                flits(p);
                p.getCooldowns().addCooldown(stack, FLITS_WACHT);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore2").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    /** The flash of the Lichtflesje at this player: light, a chime, the Nine around are blinded, the chapters hear of it. */
    public static void flits(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 plek = p.position().add(0, 1.2, 0);
        level.sendParticles(ParticleTypes.END_ROD, plek.x, plek.y, plek.z, 60, 0.4, 0.4, 0.4, 0.25);
        level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), plek.x, plek.y, plek.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, plek.x, plek.y, plek.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.6f);
        level.playSound(null, plek.x, plek.y, plek.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.8f);
        int verblind = Negen.verblind(level, p.position(), FLITS_STRAAL, FLITS_TICKS);
        if (verblind > 0) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.ring.lichtflesje.verblind").withStyle(ChatFormatting.AQUA));
            Ring.behaald(p, "ring_verblind");
        }
        Ring.behaald(p, "ring_lichtflesje");
        for (BiConsumer<ServerPlayer, Vec3> l : BIJ_LICHT) {
            l.accept(p, plek);
        }
    }

    /** (every other tick) the lamp: a light block of air that walks along with whoever holds the flask. */
    static void lamp(ServerPlayer p) {
        boolean aan = p.isAlive() && !p.isSpectator() && p.isHolding(RingFeature.LICHTFLESJE.get());
        Lamp oud = LAMPEN.get(p.getUUID());
        if (!aan && oud == null) {
            return;
        }
        ServerLevel level = p.level();
        BlockPos wil = aan ? lampPlek(level, p) : null;
        if (oud != null && (wil == null || oud.dim() != level.dimension() || !oud.pos().equals(wil))) {
            doof(p.level().getServer().getLevel(oud.dim()), oud.pos());
            LAMPEN.remove(p.getUUID());
            oud = null;
        }
        if (wil != null && oud == null) {
            level.setBlock(wil, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LAMP_LICHT), Block.UPDATE_ALL);
            LAMPEN.put(p.getUUID(), new Lamp(level.dimension(), wil));
        }
    }

    /** The free block of air at the holder's head (or the one above it); null: no room for a light (under water, in a wall). */
    @Nullable
    private static BlockPos lampPlek(ServerLevel level, ServerPlayer p) {
        BlockPos oog = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());
        for (BlockPos pos : new BlockPos[]{oog, oog.above(), oog.below()}) {
            BlockState s = level.getBlockState(pos);
            if (s.isAir() || s.is(Blocks.LIGHT) && s.getValue(LightBlock.LEVEL) == LAMP_LICHT && !s.getValue(LightBlock.WATERLOGGED)) {
                return pos;
            }
        }
        return null;
    }

    private static void doof(@Nullable ServerLevel level, BlockPos pos) {
        if (level != null && level.isLoaded(pos)) {
            BlockState s = level.getBlockState(pos);
            if (s.is(Blocks.LIGHT) && s.getValue(LightBlock.LEVEL) == LAMP_LICHT && !s.getValue(LightBlock.WATERLOGGED)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    // =====================================================================================================================
    // the Elfenmanteltje
    // =====================================================================================================================

    public static class Manteltje extends Item {
        public Manteltje(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("quest.guhs.ring.manteltje.hoe").withStyle(ChatFormatting.GREEN));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore2").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    /** Is this player a rock right now (crouching still under the Elfenmanteltje)? The Eye and patrols look past a rock. */
    public static boolean isRots(Player p) {
        return ROTSEN.containsKey(p.getUUID());
    }

    /** (every tick) crouching still with the cloak in the pockets: after {@link #ROTS_NA} ticks a rock; a step or standing up: over. */
    static void rots(ServerPlayer p) {
        boolean kan = p.isShiftKeyDown() && p.onGround() && !p.isPassenger() && p.isAlive() && !p.isSpectator();
        Stil s = STIL.get(p.getUUID());
        boolean stil = kan && s != null && s.plek().distanceToSqr(p.position()) < 0.0009;
        if (!stil) {
            if (isRots(p)) {
                stopRots(p);
            }
            if (kan) {
                STIL.put(p.getUUID(), new Stil(p.position(), 0));
            } else if (s != null) {
                STIL.remove(p.getUUID());
            }
            return;
        }
        int ticks = s.ticks() + 1;
        STIL.put(p.getUUID(), new Stil(s.plek(), ticks));
        if (isRots(p) && !p.isInvisible()) {
            p.setInvisible(true);   // (the game recomputes the flag whenever a potion effect changes)
        }
        if (ticks == ROTS_NA && !isRots(p) && heeft(p, RingFeature.ELFENMANTELTJE.get())) {
            wordRots(p);
        }
    }

    /** The player turns into a rock: a mossy boulder stands where they crouch and they are not to be seen themselves. */
    static void wordRots(ServerPlayer p) {
        ServerLevel level = p.level();
        Entity rots = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
        if (rots == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        CompoundTag blok = new CompoundTag();
        blok.putString("Name", "minecraft:mossy_cobblestone");
        tag.put("block_state", blok);
        CompoundTag vorm = new CompoundTag();
        vorm.put("translation", floats(-0.55f, 0f, -0.55f));
        vorm.put("scale", floats(1.1f, 1.45f, 1.1f));
        vorm.put("left_rotation", floats(0f, 0f, 0f, 1f));
        vorm.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tag.put("transformation", vorm);
        Nbt.load(rots, tag);
        rots.snapTo(p.getX(), p.getY(), p.getZ(), 0f, 0f);
        rots.addTag(ROTS_TAG);
        level.addFreshEntity(rots);
        ROTSEN.put(p.getUUID(), rots.getUUID());
        p.setInvisible(true);
        level.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.6, p.getZ(), 10, 0.4, 0.4, 0.4, 0.01);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 0.8f, 0.8f);
        p.sendOverlayMessage(Component.translatable("quest.guhs.ring.manteltje.rots").withStyle(ChatFormatting.GREEN));
        Ring.behaald(p, "ring_rots");
    }

    private static ListTag floats(float... waarden) {
        ListTag lijst = new ListTag();
        for (float f : waarden) {
            lijst.add(FloatTag.valueOf(f));
        }
        return lijst;
    }

    /** The rock is a player again (they moved, stood up, were caught, logged out). */
    public static void stopRots(ServerPlayer p) {
        UUID rots = ROTSEN.remove(p.getUUID());
        STIL.remove(p.getUUID());
        if (rots == null) {
            return;
        }
        Entity e = p.level().getEntity(rots);
        if (e != null) {
            e.discard();
        }
        p.setInvisible(Ring.om(p) || p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY));
        p.level().sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.6, p.getZ(), 8, 0.4, 0.4, 0.4, 0.01);
    }

    /** A rock display whose player is gone (a crash, a chunk that was saved with it): away with it. */
    static boolean isWeesRots(Entity e) {
        return e.entityTags().contains(ROTS_TAG) && !ROTSEN.containsValue(e.getUUID());
    }

    // =====================================================================================================================
    // the Elfentouw and its hooks
    // =====================================================================================================================

    public static class Touw extends Item {
        public Touw(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                BlockPos haak = zoekHaak(p);
                if (haak == null) {
                    p.sendOverlayMessage(Component.translatable("quest.guhs.ring.touw.geen_haak").withStyle(ChatFormatting.YELLOW));
                    return InteractionResult.FAIL;
                }
                klim(p, haak);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof Haak) {
                if (context.getPlayer() instanceof ServerPlayer p) {
                    klim(p, context.getClickedPos());
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore2").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    /** The hook this player aims at: the first Elfentouwhaak along their line of sight (one block of slack), within reach. */
    @Nullable
    public static BlockPos zoekHaak(ServerPlayer p) {
        Vec3 oog = p.getEyePosition(), kijk = p.getLookAngle();
        Level level = p.level();
        for (double d = 0.5; d <= TOUW_BEREIK; d += 0.5) {
            BlockPos midden = BlockPos.containing(oog.add(kijk.scale(d)));
            for (BlockPos pos : BlockPos.betweenClosed(midden.offset(-1, -1, -1), midden.offset(1, 1, 1))) {
                if (level.getBlockState(pos).getBlock() instanceof Haak) {
                    return pos.immutable();
                }
            }
        }
        return null;
    }

    /** The rope catches this hook: the player is pulled to it (see {@link #klimTick}). */
    public static void klim(ServerPlayer p, BlockPos haak) {
        if (Vec3.atCenterOf(haak).distanceTo(p.getEyePosition()) > TOUW_BEREIK + 2) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.ring.touw.te_ver").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        KLIMMERS.put(p.getUUID(), new Klim(haak.immutable(), p.level().getGameTime()));
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1f, 0.7f);
        p.level().playSound(null, haak, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.9f, 1.2f);
    }

    public static boolean klimt(Player p) {
        return KLIMMERS.containsKey(p.getUUID());
    }

    /** (every tick) hauls a climbing player along the rope; at the hook they are put on the first place to stand near it. */
    static void klimTick(ServerPlayer p) {
        Klim k = KLIMMERS.get(p.getUUID());
        if (k == null) {
            return;
        }
        ServerLevel level = p.level();
        Vec3 doel = Vec3.atCenterOf(k.haak());
        Vec3 naar = doel.subtract(p.position().add(0, 0.9, 0));
        boolean haakWeg = !(level.getBlockState(k.haak()).getBlock() instanceof Haak);
        if (haakWeg || !p.isAlive() || p.isShiftKeyDown() && level.getGameTime() - k.begin() > 10 || level.getGameTime() - k.begin() > 160) {
            KLIMMERS.remove(p.getUUID());
            p.fallDistance = 0;
            return;
        }
        p.fallDistance = 0;
        if (naar.length() < 1.5) {
            KLIMMERS.remove(p.getUUID());
            Vec3 sta = staplek(level, p, k.haak());
            p.setDeltaMovement(Vec3.ZERO);
            p.teleportTo(sta.x, sta.y, sta.z);
            p.hurtMarked = true;
            level.playSound(null, sta.x, sta.y, sta.z, SoundEvents.WOOL_STEP, SoundSource.PLAYERS, 1f, 1.1f);
            Ring.behaald(p, "ring_elfentouw");
            for (BiConsumer<ServerPlayer, BlockPos> l : BIJ_HAAK) {
                l.accept(p, k.haak());
            }
            return;
        }
        p.setDeltaMovement(naar.normalize().scale(0.55));
        p.hurtMarked = true;
        if (level.getGameTime() % 2 == 0) {
            // the rope: a dotted line of sparkles from the hands to the hook
            Vec3 hand = p.position().add(0, 1.1, 0);
            int n = (int) Math.min(24, naar.length() * 1.5);
            for (int i = 1; i <= n; i++) {
                Vec3 punt = hand.lerp(doel, i / (double) (n + 1));
                level.sendParticles(ParticleTypes.WAX_OFF, punt.x, punt.y, punt.z, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Where a player stands when they reach the hook: on top of the block the hook hangs on, next to it, or on the hook's own spot. */
    private static Vec3 staplek(ServerLevel level, ServerPlayer p, BlockPos haak) {
        BlockState state = level.getBlockState(haak);
        BlockPos tegen = state.getBlock() instanceof Haak ? haak.relative(state.getValue(Haak.FACING).getOpposite()) : haak.below();
        BlockPos[] keuzes = {tegen.above(), haak.above(), haak, tegen.above().north(), tegen.above().south(), tegen.above().east(), tegen.above().west(),
                haak.north(), haak.south(), haak.east(), haak.west()};
        for (BlockPos pos : keuzes) {
            if (level.getBlockState(pos.below()).blocksMotion() && !level.getBlockState(pos).blocksMotion() && !level.getBlockState(pos.above()).blocksMotion()) {
                return Vec3.atBottomCenterOf(pos);
            }
        }
        return Vec3.atBottomCenterOf(haak);
    }

    /** The Elfentouwhaak: a small iron hook on a wall, a ceiling or a floor. You walk through it; the rope finds it. */
    public static class Haak extends DirectionalBlock {
        public static final MapCodec<Haak> CODEC = simpleCodec(Haak::new);
        private static final VoxelShape[] VORMEN = {
                Block.box(5, 10, 5, 11, 16, 11),    // down  (hangs under a ceiling)
                Block.box(5, 0, 5, 11, 6, 11),      // up    (stands on a floor)
                Block.box(5, 5, 10, 11, 11, 16),    // north (on the wall to the south of it)
                Block.box(5, 5, 0, 11, 11, 6),      // south
                Block.box(10, 5, 5, 16, 11, 11),    // west
                Block.box(0, 5, 5, 6, 11, 11)};     // east

        public Haak(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
        }

        @Override
        protected MapCodec<? extends DirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getClickedFace());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORMEN[state.getValue(FACING).get3DDataValue()];
        }
    }

    // =====================================================================================================================
    // every tick, and tidying up
    // =====================================================================================================================

    /** (PlayerTickEvent, server) */
    static void tick(ServerPlayer p) {
        klimTick(p);
        rots(p);
        if ((p.tickCount & 1) == 0) {
            lamp(p);
        }
    }

    /** (logout, death, another dimension) no rock, no rope, no light left behind. */
    static void vergeet(ServerPlayer p) {
        stopRots(p);
        KLIMMERS.remove(p.getUUID());
        Lamp lamp = LAMPEN.remove(p.getUUID());
        if (lamp != null) {
            doof(p.level().getServer().getLevel(lamp.dim()), lamp.pos());
        }
    }

    static void wisAlles(@Nullable net.minecraft.server.MinecraftServer server) {
        if (server != null) {
            for (Lamp lamp : LAMPEN.values()) {
                doof(server.getLevel(lamp.dim()), lamp.pos());
            }
        }
        LAMPEN.clear();
        STIL.clear();
        ROTSEN.clear();
        KLIMMERS.clear();
    }

    private Gaven() {
    }
}
