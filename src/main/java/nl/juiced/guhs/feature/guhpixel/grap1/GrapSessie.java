package nl.juiced.guhs.feature.guhpixel.grap1;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.guhpixel.Arena;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.SessieStart;
import nl.juiced.guhs.feature.guhpixel.Vertrek;

/**
 * What the three joke games of this slice share: they are for ONE player, the step of this run lives here (the saved
 * step in {@link Grappen} only grows), beds may be used (lying down is how every one of them ends), and after the
 * punchline the player is brought back to the lobby by {@link #straksKlaar}.
 * <p>
 * Lying in a bed is the vanilla sleeping of the player: {@link Dutje} lets it start and go on in these games whatever
 * the hour, and keeps the server from waking a lone sleeper.
 */
abstract class GrapSessie extends Sessie {
    protected final String grap;
    /** The step of THIS run (1..n). */
    private int stap;
    /** The punchline was shown: nothing else happens, the player goes back at {@link #terugOp}. */
    protected boolean afgelopen;
    private int terugOp = -1;
    private int klikStil;
    /** The punchline was reached; the reward of it was handed out (see {@link #clou}). */
    private boolean gewonnen, uitbetaald;

    protected GrapSessie(SessieStart start, String grap) {
        super(start);
        this.grap = grap;
    }

    /** The one player of this game (null when they just left). */
    @Nullable
    protected final ServerPlayer speler() {
        List<ServerPlayer> s = spelers();
        return s.isEmpty() ? null : s.get(0);
    }

    protected final int stap() {
        return stap;
    }

    /** The player reached this step of this run (never goes back). */
    protected final void stap(ServerPlayer p, int n) {
        if (n > stap) {
            stap = n;
            Grappen.zetStap(p, grap, n);
        }
    }

    protected final boolean ligt(ServerPlayer p) {
        return p.isSleeping();
    }

    protected static void balk(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    protected static void zeg(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /**
     * The punchline: its title now, the reward later. {@link Grappen#voltooi} (100 muntjes, the keepsake, the film) is only
     * called in {@link #spelerWeg}, when the player has their OWN inventory back: a keepsake item given while the game
     * items are still in the pockets would be thrown away with them. It is called for every way of leaving after the
     * punchline (also a logout during the credits), so nobody misses it.
     */
    protected final void clou(ServerPlayer p, Component onder) {
        gewonnen = true;
        PxGeluid.titel(p, Component.translatable("gui.guhs." + grap + ".grap.clou").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                onder.copy().withStyle(ChatFormatting.LIGHT_PURPLE), 70);
    }

    /** The punchline of this run was reached. */
    public final boolean gewonnen() {
        return gewonnen;
    }

    /** After the reward (back in the lobby): eerste = this was the player's first time. */
    protected void beloond(ServerPlayer p, boolean eerste) {
    }

    /** The game is over: in this many ticks the player gets their own things back and stands in the lobby again. */
    protected final void straksKlaar(int ticks) {
        afgelopen = true;
        terugOp = ticks() + ticks;
    }

    /** Call at the top of tick(): true when the game is over (and the player is sent back when it is time). */
    protected final boolean klaarTick() {
        ServerPlayer p = speler();
        if (p != null && ligt(p)) {
            Dutje.blijfLiggen(p);
        }
        if (afgelopen && terugOp >= 0 && ticks() >= terugOp) {
            terugOp = -1;
            if (p != null) {
                klaar(p);
            }
        }
        return afgelopen;
    }

    /** A right-click on a block that is no bed (at most a few a second reach this): true = the block may be used. */
    protected boolean klik(ServerPlayer p, BlockPos pos, BlockState s) {
        return false;
    }

    @Override
    public final boolean magGebruiken(ServerPlayer p, BlockPos pos, BlockState s) {
        if (s.is(BlockTags.BEDS)) {
            return !afgelopen;
        }
        if (afgelopen) {
            return false;
        }
        // (both hands click: one answer per click)
        if (ticks() < klikStil) {
            return false;
        }
        klikStil = ticks() + 4;
        return klik(p, pos, s);
    }

    @Override
    protected void spelerWeg(ServerPlayer p, Vertrek reden) {
        if (p.isSleeping() && !p.isRemoved()) {
            p.stopSleepInBed(true, false);
        }
        if (gewonnen && !uitbetaald) {
            uitbetaald = true;
            beloond(p, Grappen.voltooi(p, grap, false));   // (clou() showed the punchline title already)
        }
    }

    /** Makes every bed in these cells free again (a player who left while lying in it). */
    static void bedVrij(Arena a, BlockPos... lokaal) {
        for (BlockPos l : lokaal) {
            BlockPos pos = a.wereld(l.getX(), l.getY(), l.getZ());
            BlockState s = a.level().getBlockState(pos);
            if (s.getBlock() instanceof BedBlock && s.getValue(BedBlock.OCCUPIED)) {
                a.level().setBlock(pos, s.setValue(BedBlock.OCCUPIED, false), 3);
            }
        }
    }
}
