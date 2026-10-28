package nl.juiced.guhs.feature.droomweer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.droomblokken.DroomblokkenFeature;
import nl.juiced.guhs.feature.droomspul.DroomspulFeature;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.sterrenveld.Stuiterkussen;
import nl.juiced.guhs.feature.sterrenveld.SterrenveldFeature;
import nl.juiced.guhs.feature.weer.WeerUnlocks;
import nl.juiced.guhs.feature.weer.Weertje;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The droomkistje at the top of the Sterrenkussenveld (1.8.0, playtest 2: "why bounce all the way up?").
 * <ul>
 *   <li>Each player may open it once per Minecraft day ({@link Band#dag}; saved per player in
 *       {@code GuhQuests.saved(player)}).</li>
 *   <li>The very first time: the weerpotje "Droomweer" ({@link WeerUnlocks#potjeEenmalig}), with a little party.</li>
 *   <li>After that: droomzand and a little something (a sterlampje, a stuiterkussen or maanbloemetjes).</li>
 *   <li>Not yet tomorrow: "Kom morgen terug, vadsje!".</li>
 * </ul>
 * The kistje itself is put next to the big star by {@link DroomweerFeature} (never in the island's template, so the
 * island can be re-stamped freely).
 */
public final class DroomKistje {
    public static final String DAG = "guhs_droomkistje_dag", OOIT = "guhs_droomkistje_ooit";
    static final String T = "gui.guhs.droomweer.kistje.";
    /** How far around the big star the kistje is looked for and put. */
    static final int ZOEK_R = 7, ZOEK_OMLAAG = 20, ZOEK_OMHOOG = 4;

    public enum Uitkomst { DROOMWEER, CADEAUTJE, MORGEN }

    private DroomKistje() {
    }

    /** Opens the kistje for this player (also used by the gametest). */
    public static Uitkomst open(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        long dag = Band.dag(level.getServer());
        if (GuhQuests.saved(p).getLongOr(DAG, Long.MIN_VALUE) == dag) {
            p.sendOverlayMessage(Component.translatable(T + "morgen").withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.6f, 1.4f);
            return Uitkomst.MORGEN;
        }
        GuhQuests.saved(p).putLong(DAG, dag);
        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.7f, 1.3f);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.2f);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.05);
        if (!GuhQuests.saved(p).getBooleanOr(OOIT, false)) {
            GuhQuests.saved(p).putBoolean(OOIT, true);
            if (!WeerUnlocks.heeft(p, Weertje.DROOM)) {
                WeerUnlocks.potjeEenmalig(p, Weertje.DROOM);
                feest(p, pos);
                return Uitkomst.DROOMWEER;
            }
        }
        cadeautje(p, level.getRandom());
        return Uitkomst.CADEAUTJE;
    }

    /** The first time: a title, confetti of stars and a chime. */
    private static void feest(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 60, 1.2, 1.0, 1.2, 0.12);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 16, 0.8, 0.6, 0.8, 0);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);
        p.sendSystemMessage(Component.translatable(T + "eerste").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (!(p instanceof FakePlayer) && p.connection != null) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(T + "eerste.sub")));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(T + "eerste.titel").withStyle(ChatFormatting.GOLD)));
        }
    }

    /** Every day after the first: droomzand and a little something. */
    private static void cadeautje(ServerPlayer p, RandomSource r) {
        Minigames.give(p, new ItemStack(DroomblokkenFeature.DROOMZAND.get(), 2 + r.nextInt(3)));
        ItemStack extra = switch (r.nextInt(3)) {
            case 0 -> new ItemStack(DroomspulFeature.STERLAMPJE.get());
            case 1 -> new ItemStack(SterrenveldFeature.STUITERKUSSENS.get(r.nextInt(SterrenveldFeature.STUITERKUSSENS.size())).get());
            default -> new ItemStack(DroomblokkenFeature.MAANBLOEMETJE.get(), 1 + r.nextInt(2));
        };
        Minigames.give(p, extra);
        p.sendSystemMessage(Component.translatable(T + "cadeau", extra.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // where it stands
    // =====================================================================================================================

    /** The kistje near the big star, if it is there. */
    @Nullable
    static BlockPos zoek(ServerLevel level) {
        BlockPos ster = BlockPos.containing(SterrenveldFeature.grooteSter());
        for (BlockPos b : BlockPos.betweenClosed(ster.offset(-ZOEK_R - 2, -ZOEK_OMLAAG, -ZOEK_R - 2), ster.offset(ZOEK_R + 2, ZOEK_OMHOOG, ZOEK_R + 2))) {
            if (level.getBlockState(b).is(DroomweerFeature.DROOMKISTJE.get())) {
                return b.immutable();
            }
        }
        return null;
    }

    /**
     * Puts the kistje next to the big star when it is missing: the nearest free spot (air, air above, a sturdy block below
     * that is no stuiterkussen) below or beside the star, facing it. Null when there is no room (then it tries again later).
     */
    @Nullable
    static BlockPos zetNeer(ServerLevel level) {
        if (DroomEilanden.gebouwd(level, DroomEilanden.STERREN) < 0) {
            return null;
        }
        BlockPos al = zoek(level);
        if (al != null) {
            return al;
        }
        Vec3 sterV = SterrenveldFeature.grooteSter();
        BlockPos ster = BlockPos.containing(sterV);
        for (int r = 1; r <= ZOEK_R; r++) {
            for (int dy = 0; dy >= -ZOEK_OMLAAG; dy--) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;       // only this ring: the nearest rings first
                        }
                        BlockPos pos = ster.offset(dx, dy, dz);
                        if (vrij(level, pos)) {
                            double vx = sterV.x - (pos.getX() + 0.5), vz = sterV.z - (pos.getZ() + 0.5);   // its lid faces the star
                            Direction kijk = Math.abs(vx) > Math.abs(vz) ? (vx > 0 ? Direction.EAST : Direction.WEST)
                                    : (vz > 0 ? Direction.SOUTH : Direction.NORTH);
                            BlockState st = DroomweerFeature.DROOMKISTJE.get().defaultBlockState()
                                    .setValue(HorizontalDirectionalBlock.FACING, kijk);
                            level.setBlock(pos, st, Block.UPDATE_ALL);
                            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
                            return pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    static boolean vrij(ServerLevel level, BlockPos pos) {
        BlockState onder = level.getBlockState(pos.below());
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && onder.isFaceSturdy(level, pos.below(), Direction.UP) && !(onder.getBlock() instanceof Stuiterkussen);
    }
}
