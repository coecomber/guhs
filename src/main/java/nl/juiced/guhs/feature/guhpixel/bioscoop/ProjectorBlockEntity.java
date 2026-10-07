package nl.juiced.guhs.feature.guhpixel.bioscoop;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The projector's block entity: which film runs since when (absolute game time, so every client and a late joiner show
 * the same frame without packets), and on which screen. That is all that is saved and synced; the audience and the seats
 * live in the in-memory {@link Voorstelling}. Drawn by client.ProjectorRenderer (the film on the screen).
 */
public class ProjectorBlockEntity extends BlockEntity {
    /** (Client) plays the film's sound cues; set by client.BioscoopClient, the server never calls it. */
    public static volatile Consumer<ProjectorBlockEntity> clientTikker = be -> { };

    private String film = "";
    private long start;
    @Nullable
    private Doek doek;
    /** (Client only) the last film tick whose sound cues were played, and for which film. */
    public int geluidTot = -1;
    public String geluidFilm = "";

    public ProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(BioscoopSlice.PROJECTOR_BE.get(), pos, state);
    }

    /** The film that runs ("" = none). */
    public String film() {
        return film;
    }

    public boolean speelt() {
        return !film.isEmpty();
    }

    public long start() {
        return start;
    }

    @Nullable
    public Doek doek() {
        return doek;
    }

    public Direction kijk() {
        return getBlockState().getValue(ProjectorBlock.FACING);
    }

    /** The screen in front of this projector right now (null: none). */
    @Nullable
    public Doek zoekDoek() {
        return level == null ? null : Doek.zoek(level, worldPosition, kijk());
    }

    /** Starts a film on this screen (the caller checked everything: {@link Bioscoop#speel}). */
    void begin(ServerLevel level, FilmInfo info, Doek opDoek) {
        film = info.id();
        start = level.getGameTime();
        doek = opDoek;
        Voorstelling.begin(level, worldPosition, info, start, opDoek);
        licht(level, true);
        level.playSound(null, worldPosition, BioscoopSlice.PROJECTOR_AAN.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        sync();
    }

    /** Stops the film. uitgespeeld: it ran to its end (the audience cheers). */
    void stop(ServerLevel level, boolean uitgespeeld) {
        if (film.isEmpty()) {
            return;
        }
        Voorstelling.einde(level, worldPosition, uitgespeeld);
        film = "";
        licht(level, false);
        level.playSound(null, worldPosition, BioscoopSlice.PROJECTOR_UIT.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        sync();
    }

    /** (Tests, the dev command) skips the running film forward. */
    void spoel(int ticks) {
        start -= ticks;
        Voorstelling v = level == null ? null : Voorstelling.van(level, worldPosition);
        if (v != null) {
            v.start = start;
        }
        sync();
    }

    private void licht(ServerLevel level, boolean aan) {
        BlockState state = getBlockState();
        if (state.hasProperty(ProjectorBlock.LIT) && state.getValue(ProjectorBlock.LIT) != aan) {
            level.setBlock(worldPosition, state.setValue(ProjectorBlock.LIT, aan), 3);
        }
    }

    void serverTick(ServerLevel level) {
        if (film.isEmpty()) {
            return;
        }
        FilmInfo info = FilmInfo.van(level.getServer(), film);
        if (info == null) {
            stop(level, false);
            return;
        }
        long nu = level.getGameTime();
        if (doek == null || (nu + worldPosition.asLong()) % 20 == 0 && !doek.heel(level)) {
            Doek nieuw = zoekDoek();        // the screen was rebuilt or broken while the film runs
            if (nieuw == null) {
                stop(level, false);
                return;
            }
            if (!nieuw.equals(doek)) {
                doek = nieuw;
                sync();
            }
        }
        Voorstelling v = Voorstelling.van(level, worldPosition);
        if (v == null || v.film != info || v.start != start) {
            v = Voorstelling.begin(level, worldPosition, info, start, doek);   // (after a restart: the film just goes on)
        }
        v.doek = doek;
        v.leeft(level);
        BlockPos machine = v.machine();
        if (machine != null && nu % 25 == 0 && level.getRandom().nextInt(3) != 0) {
            PopcornmachineBlock.plop(level, machine, 2);
        }
        if (nu - start >= info.duur()) {
            Bioscoop.uitgespeeld(level, this, info);
            stop(level, true);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putString("Film", film);      // (always a key: an empty update would be ignored by the client)
        tag.putLong("Start", start);
        if (doek != null) {
            tag.putLong("DoekHoek", doek.hoek().asLong());
            tag.putInt("DoekB", doek.breedte());
            tag.putInt("DoekH", doek.hoogte());
            tag.putInt("DoekKant", doek.kant().get2DDataValue());
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        film = tag.getStringOr("Film", "");
        start = tag.getLongOr("Start", 0L);
        int b = tag.getIntOr("DoekB", 0), h = tag.getIntOr("DoekH", 0);
        doek = b >= 1 && b <= Doek.MAX_B && h >= 1 && h <= Doek.MAX_H
                ? new Doek(BlockPos.of(tag.getLongOr("DoekHoek", 0L)), b, h, Direction.from2DDataValue(tag.getIntOr("DoekKant", 0))) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The projector was broken or replaced: the film is over, the audience gets up. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            Voorstelling.einde(level, pos, false);
        }
    }
}
