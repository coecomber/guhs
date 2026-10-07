package nl.juiced.guhs.feature.guhriow2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guhrio.Baan;
import nl.juiced.guhs.feature.guhrio.GuhrioBlocks;
import nl.juiced.guhs.feature.guhrio.GuhrioKasteel;
import nl.juiced.guhs.feature.guhrio.GuhrioLevel;
import nl.juiced.guhs.feature.guhrio.GuhrioSpel;
import nl.juiced.guhs.feature.guhrio.GuhrioStukken;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * What world 2 of Super Guhrio ("de kelders") does beyond its lanes, all per player:
 * <ul>
 *     <li><b>The egg gate</b> ({@link #tick}): the red switched blocks behind a {@link GuhrioW2Blocks.EiSlot} open for a player
 *     the moment they have Guhshi's egg; without it the lock says what it wants.</li>
 *     <li><b>Hatching</b> ({@link #broed}, {@link #uit}): a player who carries the egg lays it in the nest; the cutscene
 *     {@link #SCENE} shows Guhshi crawl out, once per player ({@link #uitgebroed}); from then on he waits on his nest for
 *     that player (the engine's Guhshi spot) and carries them.</li>
 *     <li><b>The warp room</b> ({@link #warp}): a warp pipe opens a later level for good
 *     ({@link GuhrioKasteel#ontgrendel}) and puts the player in it, through that level's own gate in the hall.</li>
 *     <li>the hidden advancements the quest book asks for: {@code quest/guhrio_w2_uitgebroed}, {@code quest/guhrio_w2_warp},
 *     {@code quest/guhrio_w2_vads} (all six big vadsmunten of world 2).</li>
 * </ul>
 * Saved per player in {@code GuhQuests.saved}: {@link #UIT}, {@link #WARP_GEVONDEN}, {@link #VADS}.
 */
public final class GuhrioW2 {
    /** The two levels of this world. */
    public static final String LEVEL_1 = "kasteel_2_1", LEVEL_2 = "kasteel_2_2";
    /** Saved per player: Guhshi hatched; the warp room was used; the six vadsmunten of world 2 are complete. */
    public static final String UIT = "guhs_guhriow2_uit", WARP_GEVONDEN = "guhs_guhriow2_warp", VADS = "guhs_guhriow2_vads";
    /** Which level a warp pipe leads to, by the pipe's channel. */
    public static final Map<Integer, String> WARP = new ConcurrentHashMap<>(Map.of(13, "kasteel_3_1", 14, "kasteel_3_2", 15, "kasteel_1_1"));
    /** How far around the hall's middle (a level's exit spot) the gates are looked for. */
    public static final int ZOEK = 26;
    /** How near (blocks along the lane) the lock tells a player without the egg what it wants, and how often (ticks). */
    public static final double SLOT_DICHTBIJ = 2.6;
    public static final int SLOT_RUST = 120;

    /**
     * Guhshi crawls out of his egg. Written in the level's own frame with the nest as anchor: +x further along the lane,
     * +z towards the camera's side, y 0 the ground the nest stands on (so the nest's top is y 1). {@link #draai} turns it
     * with the lane.
     */
    public static final Cutscene SCENE = scene();

    private GuhrioW2() {
    }

    /** (GuhrioW2Feature.register) */
    static void registreer() {
        // (the scene is registered by the class's own start-up: both sides know it)
    }

    private static Cutscene scene() {
        Vec3 nest = new Vec3(0.5, 1.0, 0.5), diep = new Vec3(0.5, -40, 0.5), voorNest = new Vec3(-1.2, 0, 0.5);
        return Cutscene.maak("guhriow2_uit").duur(280).bij("guhrio").verbergEcht(3)
                .speler(new Vec3(-2.5, 0, 0.5), -90f)
                .acteur("ei", () -> EntityType.ITEM_DISPLAY, new Vec3(-2.5, 2.1, 0.5), 0f, GuhrioW2::eiData)
                .guh("guhshi", GuhVariant.GUHSHI, diep, 90f)
                // the player walks up with the egg and lays it in the nest
                .camera(0, new Vec3(-1.0, 2.2, 8.5), new Vec3(-0.8, 1.2, 0.5))
                .camera(60, new Vec3(0.2, 1.9, 6.0), new Vec3(0.0, 1.2, 0.5))
                .camera(112, new Vec3(0.5, 1.8, 4.6), new Vec3(0.5, 1.3, 0.5))
                .loop(Cutscene.SPELER, 10, 40, voorNest)
                .loop("ei", 10, 40, new Vec3(-1.2, 2.1, 0.5))
                .animatie(Cutscene.SPELER, 44, "zwaai")
                .loop("ei", 46, 62, nest)
                .zeg(44, "", "leg", 26)
                // it wobbles, it cracks
                .loop("ei", 72, 76, new Vec3(0.62, 1.0, 0.5)).loop("ei", 76, 80, new Vec3(0.38, 1.0, 0.5)).loop("ei", 80, 84, nest)
                .geluid(72, () -> SoundEvents.TURTLE_EGG_CRACK, 1.0f, 1.0f)
                .zeg(70, "", "krak1", 24)
                .loop("ei", 98, 102, new Vec3(0.64, 1.08, 0.5)).loop("ei", 102, 106, new Vec3(0.36, 1.0, 0.5))
                .loop("ei", 106, 110, new Vec3(0.5, 1.12, 0.5)).loop("ei", 110, 114, nest)
                .geluid(98, () -> SoundEvents.TURTLE_EGG_CRACK, 1.0f, 1.1f)
                .geluid(106, () -> SoundEvents.TURTLE_EGG_CRACK, 1.0f, 1.3f)
                .schud(106, 0.6f, 10)
                .zeg(96, "", "krak2", 24)
                // in the dark the egg makes room for Guhshi
                .zwart(118, 130)
                .loop("ei", 126, 127, diep)
                .loop("guhshi", 126, 127, nest)
                .geluid(124, () -> SoundEvents.TURTLE_EGG_HATCH, 1.0f, 1.0f)
                .cameraKnip(128, new Vec3(-0.2, 1.9, 3.6), new Vec3(0.3, 1.5, 0.5))
                .camera(260, new Vec3(-0.7, 1.8, 4.4), new Vec3(0.0, 1.3, 0.5))
                .kijk("guhshi", 128, voorNest)
                .deeltjes(132, ParticleTypes.POOF, new Vec3(0.5, 1.4, 0.5), 12, 0.3)
                .deeltjes(136, ParticleTypes.HAPPY_VILLAGER, new Vec3(0.5, 1.6, 0.5), 10, 0.4)
                .zeg(140, "guhshi", "njeg", 32)
                .animatie("guhshi", 150, "spring")
                .zeg(178, "guhshi", "mama", 38)
                .animatie("guhshi", 180, "spring")
                .animatie("guhshi", 194, "spring")
                .deeltjes(184, ParticleTypes.HEART, new Vec3(0.5, 2.2, 0.5), 3, 0.2)
                .animatie(Cutscene.SPELER, 190, "zwaai")
                .zeg(222, "", "rug", 46)
                .zwart(272, 280)
                .registreer();
    }

    /** The egg of the scene: the egg's own picture as an item display that always looks at the camera. */
    private static void eiData(CompoundTag tag) {
        CompoundTag item = new CompoundTag();
        item.putString("id", "guhs:guhrio_guhshi_ei");
        item.putInt("count", 1);
        tag.put("item", item);
        tag.putString("billboard", "center");
        CompoundTag vorm = new CompoundTag();
        vorm.put("translation", floats(0f, 0.5f, 0f));
        vorm.put("scale", floats(1f, 1f, 1f));
        vorm.put("left_rotation", floats(0f, 0f, 0f, 1f));
        vorm.put("right_rotation", floats(0f, 0f, 0f, 1f));
        tag.put("transformation", vorm);
    }

    private static ListTag floats(float... waarden) {
        ListTag lijst = new ListTag();
        for (float f : waarden) {
            lijst.add(FloatTag.valueOf(f));
        }
        return lijst;
    }

    // =====================================================================================================================
    // asking
    // =====================================================================================================================

    /** Did Guhshi crawl out of his egg for this player? */
    public static boolean uitgebroed(Player player) {
        return GuhQuests.saved(player).getBooleanOr(UIT, false);
    }

    /** Did this player ever go through a warp pipe? */
    public static boolean warpGevonden(Player player) {
        return GuhQuests.saved(player).getBooleanOr(WARP_GEVONDEN, false);
    }

    /** Has this player all six big vadsmunten of world 2? */
    public static boolean alleVadsmunten(Player player) {
        return GuhrioKasteel.vadsmunten(player, LEVEL_1) == 7 && GuhrioKasteel.vadsmunten(player, LEVEL_2) == 7;
    }

    /** The rotation that turns the scene's frame (+x = further along the lane) with a lane that runs this way. */
    public static Rotation draai(Direction langs) {
        return switch (langs) {
            case SOUTH -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_180;
            case NORTH -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** The nest of this level nearest to {@code bij} (null: the level has none). */
    @Nullable
    public static BlockPos nest(GuhrioSpel.Sessie sessie, BlockPos bij) {
        BlockPos beste = null;
        for (GuhrioSpel.Stuk stuk : sessie.actief.stukken) {
            if (stuk.state().getBlock() instanceof GuhrioW2Blocks.Nest && (beste == null || stuk.pos().distSqr(bij) < beste.distSqr(bij))) {
                beste = stuk.pos();
            }
        }
        return beste;
    }

    // =====================================================================================================================
    // every tick of a player in a level
    // =====================================================================================================================

    /** The egg gate opens for who has the egg (and says what it wants to who has not); the vadsmunten of world 2. */
    static void tick(ServerPlayer player) {
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(player);
        if (s == null || player.tickCount % 5 != 0 || Cutscenes.bezig(player)) {
            return;
        }
        boolean ei = GuhrioKasteel.heeftEi(player);
        for (GuhrioSpel.Stuk stuk : s.actief.stukken) {
            if (!(stuk.state().getBlock() instanceof GuhrioW2Blocks.EiSlot)) {
                continue;
            }
            int k = stuk.state().getValue(GuhrioStukken.KANAAL);
            boolean open = (s.kanalen >> k & 1) != 0;
            if (ei && !open) {
                GuhrioSpel.zetKanaal(player, s, k, true);
                player.sendOverlayMessage(Component.translatable("gui.guhs.guhriow2.slot.open").withStyle(ChatFormatting.GREEN));
                BlockPos pos = stuk.pos();
                player.level().playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 0.8f, 1.2f);
                player.level().sendParticles(player, ParticleTypes.HAPPY_VILLAGER, false, false, pos.getX() + 0.5, pos.getY() - 1.0, pos.getZ() + 0.5, 12, 0.4, 1.2, 0.4, 0.02);
            } else if (!ei && stuk.baan() == s.baan && Math.abs(stuk.s() - s.s) < SLOT_DICHTBIJ && Math.abs(stuk.pos().getY() - player.getY()) < 8) {
                Integer tot = s.rust.get(stuk.pos());
                if (tot == null || s.ticks >= tot) {
                    s.rust.put(stuk.pos(), s.ticks + SLOT_RUST);
                    player.sendOverlayMessage(Component.translatable("gui.guhs.guhriow2.slot.dicht").withStyle(ChatFormatting.YELLOW));
                    player.level().playSound(null, stuk.pos(), SoundEvents.CHEST_LOCKED, SoundSource.PLAYERS, 0.7f, 1.3f);
                }
            }
        }
        if (player.tickCount % 40 == 0 && !GuhQuests.saved(player).getBooleanOr(VADS, false) && alleVadsmunten(player)) {
            GuhQuests.saved(player).putBoolean(VADS, true);
            GuhAdvancements.grant(player, "guhrio_w2_vads");
            player.sendSystemMessage(Component.translatable("gui.guhs.guhriow2.vads").withStyle(ChatFormatting.GOLD));
        }
    }

    // =====================================================================================================================
    // hatching
    // =====================================================================================================================

    /**
     * The player walked into a {@link GuhrioW2Blocks.Broedplek}. With the egg, and not hatched yet: the scene plays (the
     * level stands still meanwhile) and {@link #uit} follows. Anybody else walks on.
     */
    static void broed(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pos) {
        if (uitgebroed(player) || !GuhrioKasteel.heeftEi(player) || Cutscenes.bezig(player)) {
            return;
        }
        BlockPos nest = nest(sessie, pos);
        BlockPos anker = nest == null ? pos : nest;
        Baan baan = sessie.lane();
        Direction langs = baan.richting(baan.plek(anker.getX() + 0.5, anker.getZ() + 0.5).stuk());
        player.setDeltaMovement(Vec3.ZERO);
        if (!Cutscenes.speel(player, SCENE, anker, draai(langs), GuhrioW2::uit)) {
            uit(player);                                            // (it can't be shown: he hatches all the same)
        }
    }

    /** Guhshi is out of his egg for this player (after the scene): he waits on his nest, and carries them from now on. */
    static void uit(ServerPlayer player) {
        if (uitgebroed(player)) {
            return;
        }
        GuhQuests.saved(player).putBoolean(UIT, true);
        GuhAdvancements.grant(player, "guhrio_w2_uitgebroed");
        GidsFeature.grant(player, "guhrio/guhrio_w2_uitgebroed");
        player.sendSystemMessage(Component.translatable("gui.guhs.guhriow2.uit").withStyle(ChatFormatting.GREEN));
        GuhrioSpel.Sessie s = GuhrioSpel.sessie(player);
        if (s == null) {
            return;
        }
        ServerLevel level = player.level();
        for (GuhrioSpel.Stuk stuk : s.actief.stukken) {
            if (stuk.state().getBlock() instanceof GuhrioStukken.GuhshiPlek) {
                GuhrioSpel.zetStaat(player, s, stuk.pos(), 0);     // (there he is)
                BlockPos p = stuk.pos();
                level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, false, false, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 8, 0.3, 0.4, 0.3, 0.02);
            }
        }
        level.playSound(null, player.blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), SoundSource.PLAYERS, 0.9f, 1.6f);
    }

    // =====================================================================================================================
    // the warp room
    // =====================================================================================================================

    /**
     * The player ducked on the warp pipe at {@code pijp} with this channel. The level it stands for is open for them from
     * now on, this level ends (it does not count as finished) and they stand at the start of the other one.
     */
    static void warp(ServerPlayer player, GuhrioSpel.Sessie sessie, BlockPos pijp, int kanaal) {
        String doel = WARP.get(kanaal);
        ServerLevel level = player.level();
        GuhrioLevel def = doel == null ? null : GuhrioLevel.vind(level.getServer(), doel);
        BlockPos start = def == null ? null : zoekStart(level, sessie, doel);
        if (start == null) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.guhriow2.warp.dicht").withStyle(ChatFormatting.YELLOW));
            return;
        }
        GuhrioKasteel.ontgrendel(player, doel);
        if (!warpGevonden(player)) {
            GuhQuests.saved(player).putBoolean(WARP_GEVONDEN, true);
            GuhAdvancements.grant(player, "guhrio_w2_warp");
            GidsFeature.grant(player, "guhrio/guhrio_w2_warp");
        }
        level.playSound(null, pijp, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 0.8f, 1.4f);
        level.sendParticles(ParticleTypes.PORTAL, pijp.getX() + 0.5, pijp.getY() + 1.6, pijp.getZ() + 0.5, 30, 0.3, 0.6, 0.3, 0.3);
        GuhrioSpel.stop(player, GuhrioSpel.Einde.GESTOPT);
        if (GuhrioSpel.start(player, start)) {
            player.sendSystemMessage(Component.translatable("gui.guhs.guhriow2.warp", def.wereld()).withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, player.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 0.8f, 1.4f);
        } else {
            // (the level is open now: its gate in the hall, where the player stands, lets them in)
            player.sendSystemMessage(Component.translatable("gui.guhs.guhriow2.warp.hal", def.wereld()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /**
     * The start block of level {@code doel}, found through its gate in the hall: the gates stand around the spot where this
     * level's flagpole puts you (its exit), so the search turns with the castle and needs no coordinates of its own.
     */
    @Nullable
    static BlockPos zoekStart(ServerLevel level, GuhrioSpel.Sessie sessie, String doel) {
        BlockPos midden = sessie.level().uitgang() != null ? sessie.level().uitgang() : sessie.level().ingang();
        if (midden == null) {
            return null;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -ZOEK; dx <= ZOEK; dx++) {
                for (int dz = -ZOEK; dz <= ZOEK; dz++) {
                    pos.set(midden.getX() + dx, midden.getY() + dy, midden.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof GuhrioBlocks.PoortBlok && level.getBlockEntity(pos) instanceof GuhrioBlocks.StartBlockEntity be
                            && doel.equals(be.level())) {
                        BlockPos start = GuhrioBlocks.PoortBlok.start(level, pos, state);
                        if (start != null && level.getBlockState(start).getBlock() instanceof GuhrioBlocks.StartBlok) {
                            return start;
                        }
                    }
                }
            }
        }
        return null;
    }
}
