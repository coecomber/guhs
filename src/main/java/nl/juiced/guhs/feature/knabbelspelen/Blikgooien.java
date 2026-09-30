package nl.juiced.guhs.feature.knabbelspelen;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import nl.juiced.guhs.feature.doolhof.Anker;

/**
 * Mika-blikgooien: six tins with Mika faces stand on a table in a pyramid (3-2-1). Throw your {@value #BALLEN}
 * pluisballen at them: every tin that falls is 10 points (a tin loses its footing when one under it goes), all six down
 * is 20 extra and a fresh pyramid. Points are the score.
 */
public final class Blikgooien implements Wedstrijd.Spel {
    public static final Blikgooien SPEL = new Blikgooien();
    public static final int BALLEN = 8, PER_BLIK = 10, ALLES_OM = 20;
    public static final String TAG = "guhs_blikbal";
    /** The six tins: s (sideways), height above the table, shifted half a block towards +s? */
    static final int[][] BLIKKEN = {{-1, 0, 0}, {0, 0, 0}, {1, 0, 0}, {-1, 1, 1}, {0, 1, 1}, {0, 2, 0}};

    static final class Staat {
        int gegooid, punten, laatsteWorp = -1000, herstel, omver;
        final boolean[] staat = {true, true, true, true, true, true};
    }

    static Staat staat(Wedstrijd.Deelnemer d) {
        if (d.staat instanceof Staat s) {
            return s;
        }
        Staat s = new Staat();
        d.staat = s;
        return s;
    }

    @Override
    public double startU() {
        return Speelvelden.BLIK_MAT;
    }

    @Override
    public void klaarzetten(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        staat(d);
        w.naarStart(d, p, level, Speelvelden.BLIK_MAT);
        stapel(w.anker, d.baan, level);
        Wedstrijd.inHand(p, new ItemStack(KnabbelspelenFeature.BLIK_PLUISBAL.get(), BALLEN));
    }

    /** Where tin i of lane k stands. */
    static BlockPos plek(Anker a, int k, int i) {
        return Speelvelden.blok(a, Onderdeel.BLIKGOOIEN, k, Speelvelden.BLIK_TAFEL, BLIKKEN[i][0], Speelvelden.G + 2 + BLIKKEN[i][1]);
    }

    /** What tin i looks like: its Mika face towards the thrower, shifted half a block for the second row. */
    static BlockState blik(Anker a, int i) {
        Direction gezicht = a.richting(Speelvelden.veld(Onderdeel.BLIKGOOIEN).noord() ? Direction.SOUTH : Direction.NORTH);
        KnabbelspelenBlocks.Schuif schuif = KnabbelspelenBlocks.Schuif.GEEN;
        if (BLIKKEN[i][2] == 1) {
            schuif = gezicht.getClockWise() == a.richting(Direction.EAST) ? KnabbelspelenBlocks.Schuif.RECHTS : KnabbelspelenBlocks.Schuif.LINKS;
        }
        return KnabbelspelenFeature.BLIK.get().defaultBlockState().setValue(KnabbelspelenBlocks.Blik.FACING, gezicht)
                .setValue(KnabbelspelenBlocks.Blik.SCHUIF, schuif);
    }

    /** A fresh pyramid on the table (and loose tins in the lane go away). */
    static void stapel(Anker a, int k, ServerLevel level) {
        for (int u = Speelvelden.BLIK_TAFEL - 3; u <= Speelvelden.BLIK_TAFEL + 5; u++) {
            for (int s = -2; s <= 2; s++) {
                for (int y = Speelvelden.G + 1; y <= Speelvelden.G + 5; y++) {
                    BlockPos pos = Speelvelden.blok(a, Onderdeel.BLIKGOOIEN, k, u, s, y);
                    if (level.getBlockState(pos).is(KnabbelspelenFeature.BLIK.get())) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (int i = 0; i < BLIKKEN.length; i++) {
            level.setBlock(plek(a, k, i), blik(a, i), 2);
        }
    }

    @Override
    public void aftellen(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        opMat(w, d, p, level);
    }

    @Override
    public void tick(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level, int t) {
        Staat s = staat(d);
        opMat(w, d, p, level);
        if (s.herstel > 0 && --s.herstel == 0) {
            stapel(w.anker, d.baan, level);
            java.util.Arrays.fill(s.staat, true);
        }
        if (s.gegooid >= BALLEN && t - s.laatsteWorp > 40 && s.herstel == 0) {
            w.klaar(d, p, s.punten, false);
            return;
        }
        if (t % 5 == 0) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.blik.bar", s.punten, BALLEN - s.gegooid)
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    private static void opMat(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level) {
        double[] b = Speelvelden.baan(w.anker, Onderdeel.BLIKGOOIEN, d.baan, p.position());
        if (b[0] > Speelvelden.BLIK_MAT + 1.6 || b[0] < -1.5 || Math.abs(b[1]) > 1.8) {
            Vec3 m = Speelvelden.punt(w.anker, Onderdeel.BLIKGOOIEN, d.baan, Speelvelden.BLIK_MAT, 0, Speelvelden.G + 1);
            Wedstrijd.teleport(p, level, m.x, m.y, m.z, p.getYRot());
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.blik.streep").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Right-click with a pluisbal: throw it (only while playing blikgooien). */
    static boolean gooi(ServerPlayer p, InteractionHand hand) {
        if (!Wedstrijd.speelt(p, Onderdeel.BLIKGOOIEN)) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.blik.nog_niet").withStyle(ChatFormatting.GRAY));
            return false;
        }
        Wedstrijd w = Wedstrijd.van(p);
        Wedstrijd.Deelnemer d = w == null ? null : w.deelnemer(p);
        if (d == null) {
            return false;
        }
        Staat s = staat(d);
        if (s.gegooid >= BALLEN) {
            return false;
        }
        ServerLevel level = p.level();
        Snowball bal = new Snowball(level, p);
        bal.setItem(new ItemStack(KnabbelspelenFeature.BLIK_PLUISBAL.get()));
        bal.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 1.5f, 0.6f);
        bal.addTag(TAG);
        level.addFreshEntity(bal);
        s.gegooid++;
        s.laatsteWorp = w.ticks;
        level.playSound(null, p.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        p.getItemInHand(hand).shrink(1);
        return true;
    }

    /** A pluisbal lands: a tin of your own pyramid goes down (and the ones resting on it). It never hurts anyone. */
    public static void onImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().entityTags().contains(TAG) || !(event.getProjectile().level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getRayTraceResult() instanceof EntityHitResult) {
            event.setCanceled(true);                                   // (a soft pluisbal: it flies on, nobody gets hit)
            return;
        }
        if (!(event.getRayTraceResult() instanceof BlockHitResult hit) || !(event.getProjectile().getOwner() instanceof ServerPlayer p)) {
            return;
        }
        if (!level.getBlockState(hit.getBlockPos()).is(KnabbelspelenFeature.BLIK.get()) || !Wedstrijd.speelt(p, Onderdeel.BLIKGOOIEN)) {
            return;
        }
        Wedstrijd w = Wedstrijd.van(p);
        Wedstrijd.Deelnemer d = w == null ? null : w.deelnemer(p);
        if (d == null) {
            return;
        }
        for (int i = 0; i < BLIKKEN.length; i++) {
            if (plek(w.anker, d.baan, i).equals(hit.getBlockPos())) {
                raak(w, d, p, level, i);
                return;
            }
        }
    }

    /** (Also for the tests) tin i is hit. */
    static void raak(Wedstrijd w, Wedstrijd.Deelnemer d, ServerPlayer p, ServerLevel level, int i) {
        Staat s = staat(d);
        int voor = s.omver;
        omver(w, d, level, i);
        boolean veranderd = true;
        while (veranderd) {
            veranderd = false;
            for (int j = 3; j < BLIKKEN.length; j++) {
                if (s.staat[j] && !gesteund(s, j)) {
                    omver(w, d, level, j);
                    veranderd = true;
                }
            }
        }
        int weg = s.omver - voor;
        if (weg <= 0) {
            return;
        }
        s.punten += weg * PER_BLIK;
        level.playSound(null, plek(w.anker, d.baan, i), KnabbelspelenFeature.BLIK_KLANG.get(), SoundSource.PLAYERS, 1f, 0.9f + level.getRandom().nextFloat() * 0.3f);
        boolean alles = true;
        for (boolean b : s.staat) {
            alles &= !b;
        }
        if (alles) {
            s.punten += ALLES_OM;
            s.herstel = 25;
            Wedstrijd.title(p, net.minecraft.network.chat.Component.empty(), Component.translatable("quest.guhs.knabbelspelen.blik.alles", ALLES_OM)
                    .withStyle(ChatFormatting.GOLD), 0, 30, 8);
            level.playSound(null, p.blockPosition(), KnabbelspelenFeature.JUICH.get(), SoundSource.PLAYERS, 1f, 1.2f);
        } else {
            p.sendOverlayMessage(Component.translatable("quest.guhs.knabbelspelen.blik.raak", weg, s.punten).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** The second row rests on two tins under it, the top one on both of the second row. */
    static boolean gesteund(Staat s, int j) {
        return switch (j) {
            case 3 -> s.staat[0] && s.staat[1];
            case 4 -> s.staat[1] && s.staat[2];
            case 5 -> s.staat[3] && s.staat[4];
            default -> true;
        };
    }

    /** Tin i flies off the table (a clatter, and it's gone). */
    private static void omver(Wedstrijd w, Wedstrijd.Deelnemer d, ServerLevel level, int i) {
        Staat s = staat(d);
        if (!s.staat[i]) {
            return;
        }
        s.staat[i] = false;
        s.omver++;
        BlockPos pos = plek(w.anker, d.baan, i);
        if (level.getBlockState(pos).is(KnabbelspelenFeature.BLIK.get())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        ItemEntity vlieg = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, new ItemStack(KnabbelspelenFeature.BLIK_ITEM.get()));
        Vec3 weg = w.anker.vector(0, Speelvelden.veld(Onderdeel.BLIKGOOIEN).richting()).scale(0.25);
        vlieg.setDeltaMovement(weg.x + (level.getRandom().nextDouble() - 0.5) * 0.2, 0.28, weg.z + (level.getRandom().nextDouble() - 0.5) * 0.2);
        vlieg.setNeverPickUp();
        vlieg.lifespan = 30;
        level.addFreshEntity(vlieg);
        level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
    }

    @Override
    public int eindScore(Wedstrijd.Deelnemer d) {
        return staat(d).punten;
    }

    @Override
    public void einde(Wedstrijd w, Wedstrijd.Deelnemer d, @Nullable ServerPlayer p, ServerLevel level) {
        stapel(w.anker, d.baan, level);                               // (the table looks nice again for the next ones)
    }

    private Blikgooien() {
    }
}
