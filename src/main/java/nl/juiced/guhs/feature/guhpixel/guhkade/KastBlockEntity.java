package nl.juiced.guhs.feature.guhpixel.guhkade;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Spel;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * A Guhkade cabinet's memory (the lower half): the scores of everybody who ever played on THIS cabinet (players and guhs,
 * one line each, their best; the first five are the top 5 on the screen) and what its screen shows now: the attract
 * demo, a guh's game or a player at the buttons. Who is at the buttons is not saved: after a restart the cabinet is free.
 */
public class KastBlockEntity extends BlockEntity {
    /** One line of the score list. huis = one of the three names the cabinet comes with; geklopt = a guh that was beaten
     * by a player and has not come to look yet. */
    public record Regel(UUID id, Component naam, int score, boolean guh, boolean huis, boolean geklopt) {
        Regel metGeklopt(boolean g) {
            return new Regel(id, naam, score, guh, huis, g);
        }
    }

    public static final int TOP = 5, MAX_REGELS = 24;
    /** What the screen shows. */
    public static final int DEMO = 0, GUH = 1, SPELER = 2;
    /** The attract demo starts again every this many ticks. */
    public static final int DEMO_RONDE = 900;

    private final List<Regel> regels = new ArrayList<>();
    private boolean begonnen;
    // --- the screen (synced) ---
    private int modus = DEMO;
    private long start, seed;
    private int doel;
    private Component aanZet = Component.empty();
    // --- who is at the buttons (server, not saved) ---
    @Nullable
    private UUID bezet;
    private long bezetTot;
    // --- the game on the block's screen (client) ---
    @Nullable
    private Sim demo;
    private int demoDoel;

    public KastBlockEntity(BlockPos pos, BlockState state) {
        super(GuhkadeSlice.KAST_BE.get(), pos, state);
    }

    /** The upper half's block entity: just there, empty. */
    public boolean isBoven() {
        BlockState state = getBlockState();
        return state.hasProperty(KastBlock.HALF) && state.getValue(KastBlock.HALF) == DoubleBlockHalf.UPPER;
    }

    public Spel spel() {
        return getBlockState().getBlock() instanceof KastBlock k ? k.spel() : Spel.FLAPPY;
    }

    // =====================================================================================================================
    // the scores
    // =====================================================================================================================

    /** Every line, the best first. */
    public List<Regel> regels() {
        return List.copyOf(regels);
    }

    public List<Regel> top() {
        return List.copyOf(regels.subList(0, Math.min(TOP, regels.size())));
    }

    /** This player's or guh's best on this cabinet (0 = never played here). */
    public int scoreVan(UUID id) {
        for (Regel r : regels) {
            if (r.id().equals(id)) {
                return r.score();
            }
        }
        return 0;
    }

    /** The place (1 = the best) of this player or guh, 0 = not on the list. */
    public int plaatsVan(UUID id) {
        for (int i = 0; i < regels.size(); i++) {
            if (regels.get(i).id().equals(id)) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * A finished game: keeps the best of everybody. Returns the place (1 = the best) when this is a new best for them,
     * else 0. The name is refreshed either way (a guh may have a new name).
     */
    public int voegToe(UUID id, Component naam, int score, boolean guh) {
        int oud = -1;
        for (int i = 0; i < regels.size(); i++) {
            if (regels.get(i).id().equals(id)) {
                oud = i;
                break;
            }
        }
        if (oud >= 0 && regels.get(oud).score() >= score) {
            Regel r = regels.get(oud);
            if (!r.naam().equals(naam)) {
                regels.set(oud, new Regel(id, naam, r.score(), guh, false, r.geklopt()));
                veranderd();
            }
            return 0;
        }
        if (score <= 0) {
            return 0;
        }
        if (oud >= 0) {
            regels.remove(oud);
        }
        int plek = 0;
        while (plek < regels.size() && regels.get(plek).score() >= score) {    // (who was there first stays above an equal score)
            plek++;
        }
        regels.add(plek, new Regel(id, naam, score, guh, false, false));
        while (regels.size() > MAX_REGELS) {
            regels.remove(regels.size() - 1);
        }
        veranderd();
        return plek < MAX_REGELS ? plek + 1 : 0;
    }

    /** Marks (or clears) "beaten by a player" on a guh's line. */
    public void zetGeklopt(UUID guh, boolean geklopt) {
        for (int i = 0; i < regels.size(); i++) {
            Regel r = regels.get(i);
            if (r.id().equals(guh) && r.geklopt() != geklopt) {
                regels.set(i, r.metGeklopt(geklopt));
                setChanged();
            }
        }
    }

    public boolean isGeklopt(UUID guh) {
        for (Regel r : regels) {
            if (r.id().equals(guh)) {
                return r.geklopt();
            }
        }
        return false;
    }

    /** Empties the list (dev command); the three house names come back. */
    public void wis() {
        regels.clear();
        huisnamen();
        veranderd();
    }

    /** Every cabinet comes with three names on it, like a real one: easy to beat. */
    private void huisnamen() {
        Spel spel = spel();
        String[] namen = {"GUH", "VDS", "NJG"};
        int[] scores = spel == Spel.FLAPPY ? new int[] {6, 4, 2} : new int[] {15, 10, 5};
        for (int i = 0; i < namen.length; i++) {
            regels.add(new Regel(new UUID(0x6775686B616465L, i + 1), Component.literal(namen[i]), scores[i], false, true, false));
        }
        begonnen = true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide() || isBoven()) {
            return;
        }
        if (!begonnen) {
            huisnamen();
            setChanged();
        }
        Guhkade.Kasten.erbij(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            Guhkade.Kasten.weg(level, worldPosition);
        }
        super.setRemoved();
    }

    // =====================================================================================================================
    // who is at the buttons (server)
    // =====================================================================================================================

    /** Nobody at the buttons, or this one itself. */
    public boolean vrijVoor(UUID wie) {
        long nu = level == null ? 0 : level.getGameTime();
        return bezet == null || bezet.equals(wie) || nu > bezetTot;
    }

    @Nullable
    public UUID bezet() {
        return level != null && bezet != null && level.getGameTime() <= bezetTot ? bezet : null;
    }

    public Component aanZet() {
        return aanZet;
    }

    public int modus() {
        return modus;
    }

    /** Somebody takes the buttons until tot (game time): a player ({@link #SPELER}), or a guh on its way ({@link #DEMO}). */
    public void neem(UUID wie, Component naam, int modus, long tot) {
        boolean anders = this.modus != modus || !wie.equals(bezet);
        bezet = wie;
        bezetTot = tot;
        if (anders) {
            this.modus = modus;
            this.aanZet = naam;
            sync();
        }
    }

    /** A guh starts its game: the screen of every client shows the very same game from now on. */
    public void speelGuh(UUID guh, Component naam, long seed, int doel, long tot) {
        bezet = guh;
        bezetTot = tot;
        modus = GUH;
        aanZet = naam;
        this.seed = seed;
        this.doel = doel;
        this.start = level == null ? 0 : level.getGameTime();
        sync();
    }

    public void laatLos(UUID wie) {
        if (bezet != null && bezet.equals(wie)) {
            vrij();
        }
    }

    private void vrij() {
        bezet = null;
        bezetTot = 0;
        if (modus != DEMO) {
            modus = DEMO;
            aanZet = Component.empty();
            sync();
        }
    }

    /** Once a second: somebody who never let go (a guh that was picked up, a player who logged out) is forgotten. */
    void serverTick() {
        if (level != null && bezet != null && level.getGameTime() % 20 == 0 && level.getGameTime() > bezetTot) {
            vrij();
        }
    }

    // =====================================================================================================================
    // the screen on the block (client)
    // =====================================================================================================================

    /** The game the block's screen shows at this moment (the attract demo, or the guh's game), played up to now. */
    public Sim scherm(long gameTime) {
        Spel spel = spel();
        long s, begin;
        int d;
        if (modus == GUH) {
            s = seed;
            d = doel;
            begin = start;
        } else {
            long schuif = Math.floorMod(worldPosition.asLong() * 31L, DEMO_RONDE);
            long ronde = (gameTime + schuif) / DEMO_RONDE;
            s = worldPosition.asLong() * 131L + ronde;
            d = 2 + (int) Math.floorMod(ronde * 3L + worldPosition.asLong(), 5L) * (spel == Spel.FLAPPY ? 1 : 3);
            begin = ronde * DEMO_RONDE - schuif;
        }
        int stap = (int) Math.max(0, Math.min(Sim.MAX_STAPPEN, (gameTime - begin) * Sim.PER_TICK));
        if (demo == null || demo.seed() != s || demoDoel != d || demo.stappen() > stap) {
            demo = spel.nieuw(s);
            demoDoel = d;
        }
        while (!demo.af() && demo.stappen() < stap) {
            demo.stap(demo.bot(d));
        }
        return demo;
    }

    // =====================================================================================================================
    // saving and syncing
    // =====================================================================================================================

    public void veranderd() {
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private static CompoundTag regelTag(Regel r) {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, r.id());
        Tekst.put(t, "Naam", r.naam());
        t.putInt("Score", r.score());
        t.putBoolean("Guh", r.guh());
        t.putBoolean("Huis", r.huis());
        t.putBoolean("Geklopt", r.geklopt());
        return t;
    }

    private static ListTag lijstTag(List<Regel> regels) {
        ListTag l = new ListTag();
        for (Regel r : regels) {
            l.add(regelTag(r));
        }
        return l;
    }

    public static List<Regel> leesRegels(CompoundTag tag) {
        List<Regel> uit = new ArrayList<>();
        for (Tag t : tag.getListOrEmpty("Regels")) {
            if (t instanceof CompoundTag c) {
                UUID id = c.read("Id", UUIDUtil.CODEC).orElse(null);
                if (id != null) {
                    uit.add(new Regel(id, Tekst.get(c, "Naam"), c.getIntOr("Score", 0), c.getBooleanOr("Guh", false), c.getBooleanOr("Huis", false),
                            c.getBooleanOr("Geklopt", false)));
                }
            }
        }
        return uit;
    }

    /** What a client needs: the top 5 and the screen. */
    public CompoundTag schermTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("Regels", lijstTag(top()));
        tag.putString("Spel", spel().id);
        tag.putInt("Modus", modus);
        tag.putLong("Start", start);
        tag.putLong("Seed", seed);
        tag.putInt("Doel", doel);
        Tekst.put(tag, "AanZet", aanZet);
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        CompoundTag tag = new CompoundTag();
        tag.put("Regels", lijstTag(regels));
        tag.putBoolean("Begonnen", begonnen);
        out.store(tag);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        CompoundTag tag = Nbt.toTag(in);
        regels.clear();
        regels.addAll(leesRegels(tag));
        begonnen = tag.getBooleanOr("Begonnen", !regels.isEmpty());
        if (tag.contains("Modus")) {        // (an update from the server: the screen)
            modus = tag.getIntOr("Modus", DEMO);
            start = tag.getLongOr("Start", 0L);
            seed = tag.getLongOr("Seed", 0L);
            doel = tag.getIntOr("Doel", 0);
            aanZet = Tekst.get(tag, "AanZet");
            begonnen = true;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return schermTag();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
