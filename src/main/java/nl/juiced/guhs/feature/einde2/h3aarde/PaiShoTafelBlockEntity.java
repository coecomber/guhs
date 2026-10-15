package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.network.ModNetworking;

/**
 * A placed Pai Sho table and its game ({@link PaiSho}). Right-click a free table: you sit down as the first player and for
 * {@link #WACHT} ticks anyone who clicks the table sits down against you; nobody came (or you clicked again)? Then a guh of
 * the house plays against you. Both players see the board ({@link H3Payloads.Stand}); others who click watch. The game is
 * saved with the table; a finished game stays on the board until someone starts the next one.
 */
public class PaiShoTafelBlockEntity extends BlockEntity {
    /** How long a table waits for a second player (ticks); how long the guh of the house thinks. */
    public static final int WACHT = 200, DENK = 18;
    /** The second player when it is a guh. */
    static final String GUH = "guh";

    private String bord = PaiSho.NIEUW;
    @Nullable
    private UUID a;
    /** A player's UUID, {@link #GUH}, or "" while the table waits for one. */
    private String b = "";
    private int status = H3Payloads.KLAAR;
    private long wachtTot, denkTot;
    private final Random r = new Random();

    public PaiShoTafelBlockEntity(BlockPos pos, BlockState state) {
        super(E2H3AardeFeature.PAI_SHO_TAFEL_BE.get(), pos, state);
    }

    /** A right-click on the table. */
    void open(ServerPlayer p) {
        long nu = level.getGameTime();
        if (status == H3Payloads.KLAAR || a == null) {
            nieuw(p, nu);
        } else if (status == H3Payloads.WACHT) {
            if (p.getUUID().equals(a)) {
                begin(GUH);
            } else {
                begin(p.getStringUUID());
                Player eerste = level.getPlayerByUUID(a);
                if (eerste instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.tegen", p.getName()).withStyle(ChatFormatting.GOLD));
                }
            }
        }
        stuur(p);
        stuurSpelers();
    }

    private void nieuw(ServerPlayer p, long nu) {
        bord = PaiSho.NIEUW;
        a = p.getUUID();
        b = "";
        status = H3Payloads.WACHT;
        wachtTot = nu + WACHT;
        denkTot = 0;
        p.sendSystemMessage(Component.translatable("gui.guhs.einde2_h3_aarde.pai_sho.wacht").withStyle(ChatFormatting.GOLD));
        setChanged();
    }

    private void begin(String tegen) {
        b = tegen;
        status = H3Payloads.BEZIG;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 1f, 0.9f);
    }

    /** A click of a square, NIEUW or SLUIT in the screen of a player at this table. */
    void klik(ServerPlayer p, int cel) {
        if (cel == H3Payloads.NIEUW) {
            if (status == H3Payloads.KLAAR) {
                nieuw(p, level.getGameTime());
                stuur(p);
            }
            return;
        }
        if (cel < 0 || status != H3Payloads.BEZIG) {
            return;
        }
        char wie = p.getUUID().equals(a) ? PaiSho.A : p.getStringUUID().equals(b) ? PaiSho.B : PaiSho.LEEG;
        if (wie == PaiSho.LEEG) {
            return;
        }
        String na = PaiSho.zet(bord, cel, wie);
        if (na == null) {
            return;
        }
        gezet(na);
    }

    private void gezet(String na) {
        bord = na;
        level.playSound(null, worldPosition, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.8f, PaiSho.beurt(na) == PaiSho.A ? 1.3f : 1.0f);
        if (PaiSho.klaar(na)) {
            status = H3Payloads.KLAAR;
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, PaiSho.winnaar(na) == PaiSho.LEEG ? 0.8f : 1.4f);
        }
        setChanged();
        stuurSpelers();
    }

    void serverTick() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        long nu = level.getGameTime();
        if (status == H3Payloads.WACHT && nu >= wachtTot) {
            begin(GUH);
            stuurSpelers();
        }
        if (status == H3Payloads.BEZIG && GUH.equals(b) && PaiSho.beurt(bord) == PaiSho.B && denkTot == 0) {
            denkTot = nu + DENK;     // (the guh of the house thinks a moment; also after a restart in the middle of a game)
        }
        if (status == H3Payloads.BEZIG && GUH.equals(b) && denkTot > 0 && nu >= denkTot) {
            denkTot = 0;
            int c = PaiSho.guhZet(bord, PaiSho.B, r, 0.15);
            String na = c < 0 ? null : PaiSho.zet(bord, c, PaiSho.B);
            if (na != null) {
                gezet(na);
            }
        }
    }

    /** The board as this player sees it. */
    void stuur(ServerPlayer p) {
        int jij = p.getUUID().equals(a) ? 1 : p.getStringUUID().equals(b) ? 2 : 0;
        char w = PaiSho.winnaar(bord);
        String uitslag = status != H3Payloads.KLAAR ? "" : w == PaiSho.LEEG ? "gui.guhs.einde2_h3_aarde.pai_sho.gelijk"
                : (w == PaiSho.A && jij == 1) || (w == PaiSho.B && jij == 2) ? "gui.guhs.einde2_h3_aarde.pai_sho.gewonnen"
                : jij == 0 ? "gui.guhs.einde2_h3_aarde.pai_sho.uit." + w : "gui.guhs.einde2_h3_aarde.pai_sho.verloren";
        ModNetworking.sendTo(p, new H3Payloads.Stand(H3Payloads.TAFEL, worldPosition, bord, naam(a == null ? "" : a.toString()), naam(b), jij, status, uitslag));
    }

    private void stuurSpelers() {
        for (String id : new String[] {a == null ? "" : a.toString(), b}) {
            if (id.isEmpty() || GUH.equals(id)) {
                continue;
            }
            try {
                Player p = level.getPlayerByUUID(UUID.fromString(id));
                if (p instanceof ServerPlayer sp && worldPosition.closerToCenterThan(sp.position(), 12)) {
                    stuur(sp);
                }
            } catch (IllegalArgumentException ignored) {
                // (not a UUID)
            }
        }
    }

    private String naam(String id) {
        if (id.isEmpty()) {
            return "#gui.guhs.einde2_h3_aarde.pai_sho.niemand";
        }
        if (GUH.equals(id)) {
            return "#gui.guhs.einde2_h3_aarde.pai_sho.guh";
        }
        try {
            Player p = level.getPlayerByUUID(UUID.fromString(id));
            return p == null ? "#gui.guhs.einde2_h3_aarde.pai_sho.weg" : p.getName().getString();
        } catch (IllegalArgumentException e) {
            return "?";
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        bord = in.getStringOr("Bord", PaiSho.NIEUW);
        if (bord.length() != PaiSho.N * PaiSho.N) {
            bord = PaiSho.NIEUW;
        }
        String sa = in.getStringOr("A", "");
        try {
            a = sa.isEmpty() ? null : UUID.fromString(sa);
        } catch (IllegalArgumentException e) {
            a = null;
        }
        b = in.getStringOr("B", "");
        status = in.getIntOr("Status", H3Payloads.KLAAR);
        if (status == H3Payloads.WACHT) {
            status = b.isEmpty() ? H3Payloads.KLAAR : H3Payloads.BEZIG;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("Bord", bord);
        out.putString("A", a == null ? "" : a.toString());
        out.putString("B", b);
        out.putInt("Status", status);
    }
}
