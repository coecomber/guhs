package nl.juiced.guhs.feature.guhrio;

import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The pieces the full engine adds to {@link GuhrioBlocks} (same rules: nothing in the world ever changes, everything a
 * piece is for a player lives in that player's session):
 * <ul>
 *     <li>{@link OnzichtbaarBlok}: a hidden block: not there until your head finds it from below.</li>
 *     <li>{@link VadsmuntBlok}: one of the three big vadsmunten of a level; yours for good once taken.</li>
 *     <li>{@link SchakelaarBlok} and {@link SchakelBlok}: a switch and the blocks it makes solid or open, per player.</li>
 *     <li>the spots of creatures and moving things: {@link SchildMikaPlek}, {@link PlofMikaPlek}, {@link HapbloemPlek},
 *     {@link GrillspiesPlek} (the spit's hub, a real block), {@link PlatformPlek}, {@link ValblokPlek}.</li>
 *     <li>{@link GuhshiEi} (the egg of world 2) and {@link GuhshiPlek} (where Guhshi waits for whoever found the egg).</li>
 * </ul>
 */
public final class GuhrioStukken {
    private GuhrioStukken() {
    }

    @Nullable
    private static Player speler(CollisionContext context) {
        return context instanceof EntityCollisionContext e && e.getEntity() instanceof Player p ? p : null;
    }

    /** The way "further along the lane" points at this piece. */
    static Direction langs(GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
        Baan baan = actief.level.banen().get(stuk.baan());
        return baan.richting(baan.plek(stuk.pos().getX() + 0.5, stuk.pos().getZ() + 0.5).stuk());
    }

    // =====================================================================================================================
    // blocks
    // =====================================================================================================================

    /**
     * A hidden block: you walk and jump right through where it is, until your head comes up under it. Then it is there
     * (for you, for the rest of this run), as an empty ?-block, and what was in it is yours.
     */
    public static class OnzichtbaarBlok extends GuhrioBlocks.GetekendStuk {
        public static final MapCodec<OnzichtbaarBlok> CODEC = simpleCodec(OnzichtbaarBlok::new);

        public OnzichtbaarBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(GuhrioBlocks.INHOUD, GuhrioBlocks.Inhoud.MUNT));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(GuhrioBlocks.INHOUD);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            Player p = speler(context);
            return p != null && GuhrioSpel.staat(p, pos) != 0 ? Shapes.block() : Shapes.empty();
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            Player p = speler(context);
            // (found: a block like any other; else only somebody holding the block sees and clicks it)
            return (p != null && GuhrioSpel.staat(p, pos) != 0) || context.isHoldingItem(asItem()) ? Shapes.block() : Shapes.empty();
        }

        @Override
        public void bots(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (sessie.staat(pos) != 0) {
                return;
            }
            GuhrioSpel.zetStaat(player, sessie, pos, 1);
            ServerLevel level = player.level();
            Vec3 boven = Vec3.atBottomCenterOf(pos.above());
            GuhrioBlocks.Inhoud inhoud = state.getValue(GuhrioBlocks.INHOUD);
            if (inhoud == GuhrioBlocks.Inhoud.MUNT) {
                GuhrioSpel.muntVan(player, sessie, pos);
                GuhrioSpel.geluidAnderen(player, boven, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            } else {
                if (inhoud == GuhrioBlocks.Inhoud.VUURPEPER || sessie.kracht == GuhrioSpel.Kracht.GEEN) {
                    GuhrioSpel.zetKracht(player, sessie, inhoud == GuhrioBlocks.Inhoud.VUURPEPER ? GuhrioSpel.Kracht.VUUR : GuhrioSpel.Kracht.SUPER);
                }
                GuhrioSpel.geluid(level, boven, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.7f);
            }
            level.sendParticles(player, ParticleTypes.WAX_ON, false, false, boven.x, boven.y + 0.4, boven.z, 6, 0.2, 0.3, 0.2, 0.02);
        }
    }

    /**
     * A big vadsmunt: three in every level ({@link #NUMMER} 0, 1, 2). Take it once and it is yours for good; in a later run
     * you see only its shadow (state 2). The Guhdex and the forecourt count them ({@link GuhrioKasteel#vadsmunten}).
     */
    public static class VadsmuntBlok extends GuhrioBlocks.GetekendStuk {
        public static final MapCodec<VadsmuntBlok> CODEC = simpleCodec(VadsmuntBlok::new);
        public static final IntegerProperty NUMMER = IntegerProperty.create("nummer", 0, 2);
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

        public VadsmuntBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(NUMMER, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(NUMMER);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public int begin(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            int nummer = state.getValue(NUMMER);
            if ((GuhrioKasteel.vadsmunten(player, sessie.level().level().id()) >> nummer & 1) != 0) {
                sessie.vads |= 1 << nummer;
                return 2;
            }
            return 0;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (sessie.staat(pos) == 0) {
                GuhrioSpel.vadsmunt(player, sessie, pos, state.getValue(NUMMER));
            }
        }
    }

    /** What a switch does when it is hit. */
    public enum Schakel implements StringRepresentable {
        /** On when off, off when on. */
        WISSEL,
        AAN, UIT,
        /** On for {@link GuhrioSpel#SCHAKEL_TICKS}, then off by itself. */
        TIJD;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final IntegerProperty KANAAL = IntegerProperty.create("kanaal", 0, GuhrioSpel.KANALEN - 1);

    /**
     * A switch block ("!"): bump it from below, stand on it, or let a sliding shell or a thrown knabbel fly into it. It
     * switches its channel for that player alone: every {@link SchakelBlok} of that channel in the level turns solid or
     * open for them. {@link #INGEDRUKT} is only its look (drawn per player).
     */
    public static class SchakelaarBlok extends GuhrioBlocks.GetekendStuk {
        public static final MapCodec<SchakelaarBlok> CODEC = simpleCodec(SchakelaarBlok::new);
        public static final EnumProperty<Schakel> SOORT = EnumProperty.create("soort", Schakel.class);
        public static final BooleanProperty INGEDRUKT = BooleanProperty.create("ingedrukt");
        /** Ticks before the same switch listens to the same player again. */
        public static final int RUST = 12;

        public SchakelaarBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(KANAAL, 0).setValue(SOORT, Schakel.WISSEL).setValue(INGEDRUKT, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(KANAAL, SOORT, INGEDRUKT);
        }

        @Override
        public void bots(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            schakel(player, sessie, pos, state);
        }

        @Override
        public void stap(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            schakel(player, sessie, pos, state);
        }

        /** The switch is hit for this player. */
        public void schakel(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            Integer tot = sessie.rust.get(pos);
            if (tot != null && sessie.ticks < tot) {
                return;
            }
            sessie.rust.put(pos.immutable(), sessie.ticks + RUST);
            int k = state.getValue(KANAAL);
            boolean was = (sessie.kanalen >> k & 1) != 0;
            switch (state.getValue(SOORT)) {
                case AAN -> GuhrioSpel.zetKanaal(player, sessie, k, true);
                case UIT -> GuhrioSpel.zetKanaal(player, sessie, k, false);
                case TIJD -> GuhrioSpel.zetKanaalTijd(player, sessie, k, GuhrioSpel.SCHAKEL_TICKS);
                default -> GuhrioSpel.zetKanaal(player, sessie, k, !was);
            }
            player.level().sendParticles(player, ParticleTypes.CRIT, false, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.05);
        }
    }

    /**
     * A switched block: solid while its channel is on ({@link #AAN} true) or while it is off ({@link #AAN} false), for
     * each player by themselves; otherwise only an outline you walk through. {@link #OPEN} is only that open look.
     * Creatures (and players outside a level) see every channel as off.
     */
    public static class SchakelBlok extends GuhrioBlocks.GetekendStuk {
        public static final MapCodec<SchakelBlok> CODEC = simpleCodec(SchakelBlok::new);
        public static final BooleanProperty AAN = BooleanProperty.create("aan");
        public static final BooleanProperty OPEN = BooleanProperty.create("open");

        public SchakelBlok(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(KANAAL, 0).setValue(AAN, true).setValue(OPEN, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(KANAAL, AAN, OPEN);
        }

        /** Is this block solid for this player (null: for a creature)? */
        public static boolean vast(BlockState state, @Nullable Player player) {
            boolean kanaal = player != null && GuhrioSpel.kanaal(player, state.getValue(KANAAL));
            return kanaal == state.getValue(AAN);
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vast(state, speler(context)) ? Shapes.block() : Shapes.empty();
        }
    }

    // =====================================================================================================================
    // the spots of creatures and moving things
    // =====================================================================================================================

    /** Where a Schild-Mika lives. */
    public static class SchildMikaPlek extends GuhrioBlocks.WezenPlek {
        public static final MapCodec<SchildMikaPlek> CODEC = simpleCodec(SchildMikaPlek::new);

        public SchildMikaPlek(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected Entity maak(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
            SchildMikaEntity mika = GuhrioFeature.SCHILD_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
            if (mika != null) {
                BlockPos pos = stuk.pos();
                mika.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
                mika.zetBaan(actief, actief.level.banen().get(stuk.baan()), pos);
            }
            return mika;
        }
    }

    /** Where a Plof-Mika hangs (its underside is the bottom of this cell). */
    public static class PlofMikaPlek extends GuhrioBlocks.WezenPlek {
        public static final MapCodec<PlofMikaPlek> CODEC = simpleCodec(PlofMikaPlek::new);

        public PlofMikaPlek(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected Entity maak(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
            PlofMikaEntity mika = GuhrioFeature.PLOF_MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
            if (mika != null) {
                BlockPos pos = stuk.pos();
                mika.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
                mika.zetBaan(actief, actief.level.banen().get(stuk.baan()), pos);
            }
            return mika;
        }
    }

    /** Where a Hapbloem lives: the cell right above the mouth of its pipe. */
    public static class HapbloemPlek extends GuhrioBlocks.WezenPlek {
        public static final MapCodec<HapbloemPlek> CODEC = simpleCodec(HapbloemPlek::new);

        public HapbloemPlek(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected Entity maak(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
            HapbloemEntity bloem = GuhrioFeature.HAPBLOEM.get().create(level, EntitySpawnReason.TRIGGERED);
            if (bloem != null) {
                BlockPos pos = stuk.pos();
                bloem.snapTo(pos.getX() + 0.5, pos.getY() - HapbloemEntity.HOOG, pos.getZ() + 0.5, 0f, 0f);
                bloem.zetBaan(actief, actief.level.banen().get(stuk.baan()), pos);
            }
            return bloem;
        }
    }

    /**
     * The hub of a turning grill spit: a real block (you can stand on it). While somebody plays the level its skewer
     * ({@link GrillspiesEntity}) turns round it.
     */
    public static class GrillspiesPlek extends Block implements GuhrioStuk {
        public static final MapCodec<GrillspiesPlek> CODEC = simpleCodec(GrillspiesPlek::new);
        public static final IntegerProperty LENGTE = IntegerProperty.create("lengte", 1, 6);
        public static final BooleanProperty TEGEN = BooleanProperty.create("tegen");
        public static final BooleanProperty SNEL = BooleanProperty.create("snel");
        public static final IntegerProperty FASE = IntegerProperty.create("fase", 0, 3);

        public GrillspiesPlek(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(LENGTE, 3).setValue(TEGEN, false).setValue(SNEL, false).setValue(FASE, 0));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LENGTE, TEGEN, SNEL, FASE);
        }

        @Override
        public void wek(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
            Entity nu = actief.wezens.get(stuk.pos());
            if ((nu != null && !nu.isRemoved()) || !level.isPositionEntityTicking(stuk.pos())) {
                return;
            }
            GrillspiesEntity spies = GuhrioFeature.GUHRIO_GRILLSPIES.get().create(level, EntitySpawnReason.TRIGGERED);
            if (spies == null) {
                return;
            }
            BlockPos pos = stuk.pos();
            BlockState state = stuk.state();
            int lengte = state.getValue(LENGTE);
            spies.zetVorm(lengte, langs(actief, stuk), state.getValue(TEGEN), state.getValue(SNEL), state.getValue(FASE));
            spies.snapTo(pos.getX() + 0.5, pos.getY() - lengte, pos.getZ() + 0.5, 0f, 0f);
            spies.zetBaan(actief, actief.level.banen().get(stuk.baan()), pos);
            level.addFreshEntity(spies);
            actief.wezens.put(pos, spies);
        }
    }

    /** Which way a platform moves. */
    public enum As implements StringRepresentable {
        LANGS, OMHOOG;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final IntegerProperty BREED = IntegerProperty.create("breed", 1, 4);

    /**
     * Where a moving platform starts: its first block is this cell, {@link #BREED} blocks along the lane; it glides
     * {@link #AFSTAND} blocks further along the lane or up, and back.
     */
    public static class PlatformPlek extends GuhrioBlocks.WezenPlek {
        public static final MapCodec<PlatformPlek> CODEC = simpleCodec(PlatformPlek::new);
        public static final EnumProperty<As> AS = EnumProperty.create("as", As.class);
        public static final IntegerProperty AFSTAND = IntegerProperty.create("afstand", 1, 12);
        public static final BooleanProperty SNEL = BooleanProperty.create("snel");

        public PlatformPlek(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AS, As.LANGS).setValue(AFSTAND, 4).setValue(BREED, 3).setValue(SNEL, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AS, AFSTAND, BREED, SNEL);
        }

        @Override
        protected Entity maak(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
            PlatformEntity platform = GuhrioFeature.PLATFORM.get().create(level, EntitySpawnReason.TRIGGERED);
            if (platform != null) {
                BlockState state = stuk.state();
                platform.zetBaan(actief, actief.level.banen().get(stuk.baan()), stuk.pos());
                platform.zetVorm(stuk.pos(), langs(actief, stuk), state.getValue(AS) == As.OMHOOG, state.getValue(SNEL), state.getValue(AFSTAND),
                        state.getValue(BREED));
            }
            return platform;
        }
    }

    /** Where a falling block rests: its first block is this cell, {@link #BREED} blocks along the lane. */
    public static class ValblokPlek extends GuhrioBlocks.WezenPlek {
        public static final MapCodec<ValblokPlek> CODEC = simpleCodec(ValblokPlek::new);

        public ValblokPlek(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(BREED, 2));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BREED);
        }

        @Override
        protected Entity maak(ServerLevel level, GuhrioSpel.Actief actief, GuhrioSpel.Stuk stuk) {
            ValblokEntity blok = GuhrioFeature.VALBLOK.get().create(level, EntitySpawnReason.TRIGGERED);
            if (blok != null) {
                BlockPos pos = stuk.pos();
                Direction d = langs(actief, stuk);
                int breed = stuk.state().getValue(BREED);
                double langs = (breed - 1) / 2.0;
                blok.zetBaan(actief, actief.level.banen().get(stuk.baan()), pos);
                blok.zetVorm(breed, d, new Vec3(pos.getX() + 0.5 + d.getStepX() * langs, pos.getY() + 1 - ValblokEntity.DIK, pos.getZ() + 0.5 + d.getStepZ() * langs));
            }
            return blok;
        }
    }

    // =====================================================================================================================
    // Guhshi
    // =====================================================================================================================

    /** Guhshi's egg (world 2): walk into it and you "found Guhshi", once per player, for good. Drawn per player. */
    public static class GuhshiEi extends GuhrioBlocks.GetekendStuk {
        public static final MapCodec<GuhshiEi> CODEC = simpleCodec(GuhshiEi::new);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

        public GuhshiEi(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public int begin(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            return GuhrioKasteel.heeftEi(player) ? 1 : 0;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (sessie.staat(pos) != 0) {
                return;
            }
            GuhrioSpel.zetStaat(player, sessie, pos, 1);
            if (GuhrioKasteel.geefEi(player)) {
                player.sendSystemMessage(Component.translatable("gui.guhs.guhrio.ei").withStyle(ChatFormatting.GREEN));
                GuhrioSpel.geluid(player.level(), Vec3.atCenterOf(pos), SoundEvents.TURTLE_EGG_HATCH, 1.0f, 1.2f);
                player.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.02);
            }
        }
    }

    /**
     * Where Guhshi waits (drawn per player: he is there unless you are riding him). Walk into it and he carries you, if you
     * found his egg; lose him (a touch) and he is waiting here again.
     */
    public static class GuhshiPlek extends GuhrioBlocks.GetekendStuk {
        public static final MapCodec<GuhshiPlek> CODEC = simpleCodec(GuhshiPlek::new);
        private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

        public GuhshiPlek(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        public void binnen(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos, BlockState state) {
            if (sessie.guhshi) {
                return;
            }
            if (!GuhrioKasteel.heeftEi(player)) {
                Integer tot = sessie.rust.get(pos);
                if (tot == null || sessie.ticks >= tot) {
                    sessie.rust.put(pos.immutable(), sessie.ticks + 100);
                    player.sendOverlayMessage(Component.translatable("gui.guhs.guhrio.geen_ei").withStyle(ChatFormatting.YELLOW));
                }
                return;
            }
            GuhrioSpel.zetGuhshi(player, sessie, true);
        }
    }
}
