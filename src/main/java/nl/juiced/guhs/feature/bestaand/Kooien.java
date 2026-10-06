package nl.juiced.guhs.feature.bestaand;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.spiesburcht.NetherMikaRuil;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (bestaand): the three cages with stolen plush guhs on the first floor of the Mika-grillpaleis, per player. A
 * player at the step {@link BestaandFeature#KNUFFELMAKER_KOOIEN} of the Knuffelmaker-guh's questline frees the plush of
 * a cage ({@link Plekken#KOOIEN}) in one of two ways, by right-clicking the cage:
 * <ul>
 *   <li><b>with vads</b>: holding the ransom ("Losgeld: 1 vads per knuffel", as the Mikas' own sign says: item tag
 *       guhs:bestaand_losgeld) costs one and opens the cage at once;</li>
 *   <li><b>by sneaking</b>: crouching, {@link #KLIKKEN} clicks at least {@link #TUSSEN} ticks apart pick the lock, as long
 *       as no Nether-Mika sees it ({@link #betrapt}). A Mika that does see it gives the player a shove ({@link Duwtje}:
 *       never damage) and the picking starts over.</li>
 * </ul>
 * Freed = the player's flag {@code kooi_<i>}; the cage and its plush stay in the world for the next player. The player
 * who freed it sees an empty cage, and their plush sitting with the Knuffelmaker-guh in his naaihoek ({@link Schijn}).
 * The third plush brings the player to the next step.
 */
public final class Kooien {
    public static final int AANTAL = Plekken.KOOIEN.size();
    /** How many sneaky clicks pick a lock, and how many ticks must lie between two that count. */
    public static final int KLIKKEN = 3, TUSSEN = 12;
    /** A Nether-Mika sees a crouching player this far (blocks), when it looks their way and nothing is in between. */
    public static final double ZICHT = 7.0;
    /** The plush guh in a cage, relative to the cage's middle: what a player who freed it no longer sees. */
    private static final List<BlockPos> KNUFFEL = List.of(new BlockPos(-1, 0, 0), new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 1, 0),
            new BlockPos(-1, 1, 0), new BlockPos(1, 1, 0), new BlockPos(0, 0, -1));

    /** (not saved) a player's lock picking: which cage, how many clicks, when the last one was. */
    private record Peuter(int kooi, int klikken, long tick) {
    }

    private static final Map<UUID, Peuter> PEUTEREN = new ConcurrentHashMap<>();

    private Kooien() {
    }

    public static boolean vrij(ServerPlayer p, int i) {
        return BestaandFeature.KNUFFELMAKER.vlag(p, "kooi_" + i);
    }

    /** How many of the three plush guhs this player freed. */
    public static int aantal(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < AANTAL; i++) {
            n += vrij(p, i) ? 1 : 0;
        }
        return n;
    }

    static void vergeet(Player p) {
        PEUTEREN.remove(p.getUUID());
    }

    /** (Tests) as if this player's last sneaky click was long enough ago for the next one to count. */
    static void verouder(Player p) {
        PEUTEREN.computeIfPresent(p.getUUID(), (u, v) -> new Peuter(v.kooi, v.klikken, v.tick - TUSSEN));
    }

    /** How many sneaky clicks this player has on the lock of this cage right now. */
    public static int gepeuterd(Player p, int kooi) {
        Peuter v = PEUTEREN.get(p.getUUID());
        return v != null && v.kooi == kooi ? v.klikken : 0;
    }

    /** Is this block part of a plush cage (its bars, or the plush inside)? Asked before any structure is looked up. */
    private static boolean kooiblok(BlockState state) {
        return state.is(BarbecuetherFeature.ROOSTERIJZER_TRALIES.get()) || state.is(Blocks.PINK_WOOL) || state.is(Blocks.PINK_CARPET)
                || state.is(Blocks.BLACK_WOOL);
    }

    /** The cage this template position of a grillpaleis lies in (5 x 5, from its floor to its roof): 0..2, or -1. */
    public static int kooiBij(BlockPos lokaal) {
        for (int i = 0; i < AANTAL; i++) {
            BlockPos k = Plekken.KOOIEN.get(i);
            if (Math.abs(lokaal.getX() - k.getX()) <= 2 && Math.abs(lokaal.getZ() - k.getZ()) <= 2 && lokaal.getY() >= k.getY() - 1
                    && lokaal.getY() <= k.getY() + 3) {
                return i;
            }
        }
        return -1;
    }

    /** A right-click of this player on this block: true when it was a click on a plush cage (handled here). */
    static boolean klik(ServerPlayer p, ServerLevel level, BlockPos pos, ItemStack stack, InteractionHand hand) {
        if (!kooiblok(level.getBlockState(pos))) {
            return false;
        }
        StructureStart start = Bezetting.start(level, BestaandFeature.GRILLPALEIS, pos);
        BlockPos lokaal = start == null ? null : Kopieen.lokaal(start, null, pos);
        int kooi = lokaal == null ? -1 : kooiBij(lokaal);
        if (kooi < 0) {
            return false;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            BlockPos midden = Kopieen.wereld(start, null, Plekken.KOOIEN.get(kooi));
            klik(p, kooi, midden == null ? pos : midden, stack);
        }
        return true;
    }

    /** What a click on cage {@code kooi} (its middle at {@code midden}) does for this player holding {@code stack}. */
    public static void klik(ServerPlayer p, int kooi, BlockPos midden, ItemStack stack) {
        int stap = BestaandFeature.KNUFFELMAKER.stap(p);
        if (vrij(p, kooi)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.kooi.al_vrij").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (stap != BestaandFeature.KNUFFELMAKER_KOOIEN) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.kooi.onbekend").withStyle(ChatFormatting.GRAY));
        } else if (stack.is(BestaandFeature.LOSGELD)) {
            if (!p.getAbilities().instabuild) {
                stack.shrink(1);
            }
            bevrijd(p, kooi, midden, true);
        } else if (p.isShiftKeyDown()) {
            peuter(p, kooi, midden);
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.kooi.slot").withStyle(ChatFormatting.GRAY));
        }
    }

    /** One sneaky click on the lock. */
    private static void peuter(ServerPlayer p, int kooi, BlockPos midden) {
        long nu = p.level().getGameTime();
        Peuter vorige = PEUTEREN.get(p.getUUID());
        if (vorige != null && vorige.kooi == kooi && nu - vorige.tick < TUSSEN) {
            return;   // (the mouse button is still down: one click at a time)
        }
        MikaEntity mika = betrapt(p);
        if (mika != null) {
            PEUTEREN.remove(p.getUUID());
            p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.kooi.betrapt").withStyle(ChatFormatting.RED));
            p.level().playSound(null, mika.blockPosition(), ModSounds.MIKA_AMBIENT.get(), mika.getSoundSource(), 1.2f, 1.4f);
            if (Duwtje.mag(p)) {
                Duwtje.duw(p, p.position().subtract(Vec3.atCenterOf(midden)), 0.9);
            }
            return;
        }
        int klikken = vorige != null && vorige.kooi == kooi ? vorige.klikken + 1 : 1;
        if (klikken >= KLIKKEN) {
            PEUTEREN.remove(p.getUUID());
            GuhAdvancements.grant(p, "bestaand_gesloten");
            GidsFeature.grant(p, "barbecuether/bestaand_sluiper");
            bevrijd(p, kooi, midden, false);
        } else {
            PEUTEREN.put(p.getUUID(), new Peuter(kooi, klikken, nu));
            Schijn.geluid(p, SoundEvents.TRIPWIRE_CLICK_ON, midden, 0.7f, 1.2f + klikken * 0.25f);
            p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.kooi.peuter", klikken, KLIKKEN).withStyle(ChatFormatting.YELLOW));
        }
    }

    /**
     * The Nether-Mika that sees this player fiddle with a lock, or null. A Mika sees it within {@link #ZICHT} blocks when
     * it looks the player's way with nothing in between; not while it sniffs at a vadsstaaf (throw it one!), and never a
     * player who drank a Sluipknabbeldrankje.
     */
    @Nullable
    public static MikaEntity betrapt(ServerPlayer p) {
        if (p.hasEffect(MobEffects.INVISIBILITY) || Brouwsel.stil().map(p::hasEffect).orElse(false)) {
            return null;
        }
        for (MikaEntity mika : p.level().getEntitiesOfClass(MikaEntity.class, p.getBoundingBox().inflate(ZICHT), m -> m.isAlive() && NetherMikaRuil.isNetherMika(m))) {
            if (mika.distanceTo(p) > ZICHT || NetherMikaRuil.isAdmiring(mika) || !mika.hasLineOfSight(p)) {
                continue;
            }
            Vec3 naar = p.getEyePosition().subtract(mika.getEyePosition());
            if (naar.lengthSqr() < 1.0 || mika.getViewVector(1.0f).dot(naar.normalize()) > 0.25) {
                return mika;
            }
        }
        return null;
    }

    /** The plush of this cage is free, for this player (once; only at the cages step). True when it was freed now. */
    public static boolean bevrijd(ServerPlayer p, int kooi, BlockPos midden, boolean betaald) {
        if (BestaandFeature.KNUFFELMAKER.stap(p) != BestaandFeature.KNUFFELMAKER_KOOIEN || vrij(p, kooi)) {
            return false;
        }
        BestaandFeature.KNUFFELMAKER.vlag(p, "kooi_" + kooi, true);
        int n = aantal(p);
        Schijn.geluid(p, betaald ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.IRON_TRAPDOOR_OPEN, midden, 0.9f, betaald ? 0.8f : 1.3f);
        Schijn.geluid(p, ModSounds.GUH_HAPPY.get(), midden, 0.9f, 1.5f);
        Schijn.deeltjes(p, ParticleTypes.HEART, Vec3.atCenterOf(midden).add(0, 0.6, 0), 6, 0.5, 0.0);
        p.sendOverlayMessage(Component.translatable("gui.guhs.bestaand.kooi." + (betaald ? "betaald" : "open"), n, AANTAL).withStyle(ChatFormatting.LIGHT_PURPLE));
        Schijn.ververs(p);
        klaar(p);
        return true;
    }

    /** All three are free: on to the next step (also when the Knuffelmaker-guh counted a broken cage). True when the step changed. */
    static boolean klaar(ServerPlayer p) {
        if (aantal(p) < AANTAL || !BestaandFeature.KNUFFELMAKER.verder(p, BestaandFeature.KNUFFELMAKER_KOOIEN)) {
            return false;
        }
        GuhQuests.hint(p, "gui.guhs.bestaand.kooi.alle");
        return true;
    }

    /** Does the plush of cage i still sit in its cage in this copy (the real blocks)? */
    static boolean knuffelZitEr(ServerLevel level, StructureStart start, int i) {
        BlockPos midden = Kopieen.wereld(start, null, Plekken.KOOIEN.get(i));
        return midden == null || !level.isLoaded(midden) || level.getBlockState(midden).is(Blocks.PINK_WOOL);
    }

    /**
     * What a player who freed plush guhs sees in the grillpaleis they are at: those cages empty (the plush blocks as air),
     * and their plush guhs sitting in the naaihoek (when it stands where it should and the spot is free).
     */
    static void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        if (aantal(p) == 0) {
            return;
        }
        ServerLevel level = p.level();
        StructureStart start = Bezetting.start(level, BestaandFeature.GRILLPALEIS, p.blockPosition());
        if (start == null) {
            return;
        }
        BlockPos hoek = Kopieen.wereld(start, null, Plekken.NAAIHOEK);
        boolean naaihoek = hoek != null && Bezetting.Geplaatst.get(level).plek(BestaandFeature.NAAIHOEK_ID, start).filter(hoek::equals).isPresent();
        Direction noord = Kopieen.draai(start, null).rotate(Direction.NORTH);
        for (int i = 0; i < AANTAL; i++) {
            if (!vrij(p, i)) {
                continue;
            }
            BlockPos kooi = Plekken.KOOIEN.get(i);
            for (BlockPos d : KNUFFEL) {
                BlockPos pos = Kopieen.wereld(start, null, kooi.offset(d));
                if (pos != null && Schijn.dichtbij(p, pos)) {
                    BlockState echt = level.getBlockState(pos);
                    if (echt.is(Blocks.PINK_WOOL) || echt.is(Blocks.PINK_CARPET) || echt.is(Blocks.BLACK_WOOL)) {
                        gewenst.put(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
            BlockPos plek = naaihoek ? Kopieen.wereld(start, null, Plekken.KNUFFELPLEKKEN.get(i)) : null;
            if (plek != null && Schijn.dichtbij(p, plek) && Bezetting.leeg(level.getBlockState(plek))) {
                gewenst.put(plek, BestaandFeature.KNUFFELS.get(i).get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, noord));
            }
        }
    }
}
