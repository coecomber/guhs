package nl.juiced.guhs.feature.kermisritten;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.registry.ModItems;

/**
 * Runs a kermis ride. The only thing it has to tell anyone is the tick the ride started ({@link #start}): the position of
 * every part follows from that ({@link KermisritSoort}), on the server and on each client alike, so a ride costs no
 * position packets of its own and stays smooth for everybody.
 * <p>
 * At rest the seats can be taken. Someone sitting: {@link #WACHT} ticks later it goes. Nobody sitting, but somebody
 * around: now and then it runs empty, so a kermis never looks dead. At the end riders are let out at the gate, with a
 * kermisbon.
 */
public class KermisritBlockEntity extends BlockEntity {
    /** How long after the first rider it starts. */
    public static final int WACHT = 160;
    /** How long it stands still between empty rides: time to get on. */
    public static final int RUST = 400;
    /** A player this close keeps the empty rides going. */
    private static final double PUBLIEK = 64;
    private static final int ZORG_ELKE = 40;

    private long start = -1;
    private long wachtTot = -1;
    private long rustTot = -1;
    private final KermisritStoel[] stoelen;
    @Nullable
    private KermisritStoel loket;

    public KermisritBlockEntity(BlockPos pos, BlockState state) {
        super(KermisrittenFeature.RIT_BE.get(), pos, state);
        stoelen = new KermisritStoel[soort(state).stoelen];
    }

    private static KermisritSoort soort(BlockState state) {
        return state.getBlock() instanceof KermisritBlock blok ? blok.soort() : KermisritSoort.ZWEEFMOLEN;
    }

    public KermisritSoort soort() {
        return soort(getBlockState());
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(KermisritBlock.FACING) ? state.getValue(KermisritBlock.FACING) : Direction.SOUTH;
    }

    public boolean rijdt() {
        return start >= 0;
    }

    /** Ticks of riding so far (partial ticks on the client); 0 at rest. */
    public double tijd(float partialTick) {
        if (start < 0 || level == null) {
            return 0;
        }
        return Math.max(0, level.getGameTime() - start + (double) partialTick);
    }

    /** Where a seat (or the ticket post, -1) is right now in the world. */
    public Vec3 wereldPlek(int index, double t) {
        Vec3 v = KermisritSoort.draai(soort().plek(index, t), facing());
        return new Vec3(worldPosition.getX() + 0.5 + v.x, worldPosition.getY() + v.y, worldPosition.getZ() + 0.5 + v.z);
    }

    public Vec3 uitstapPlek() {
        Vec3 v = KermisritSoort.draai(soort().uitstap, facing());
        return new Vec3(worldPosition.getX() + 0.5 + v.x, worldPosition.getY() + v.y, worldPosition.getZ() + 0.5 + v.z);
    }

    // --- server --------------------------------------------------------------------------------------------------------------

    public void serverTick() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        long nu = sl.getGameTime();
        if ((nu + worldPosition.asLong()) % ZORG_ELKE == 0) {
            zorgVoorStoelen(sl);
        }
        if (start >= 0) {
            if (nu - start >= soort().profiel.duur()) {
                klaar(sl, nu);
            } else if (nu % 10 == 0 && !inzittenden().isEmpty()) {
                deuntje(sl, nu);
            }
            return;
        }
        if (!inzittenden().isEmpty()) {
            if (wachtTot < 0) {
                wachtTot = nu + WACHT;
            } else if (nu >= wachtTot) {
                begin(sl, nu);
            }
            return;
        }
        wachtTot = -1;
        if (rustTot < 0) {
            rustTot = nu + RUST;
        } else if (nu >= rustTot && nu % 20 == 0 && sl.hasNearbyAlivePlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), PUBLIEK)) {
            begin(sl, nu);
        }
    }

    /** The ride goes now (also for tests). */
    public void begin(ServerLevel sl, long nu) {
        start = nu;
        wachtTot = -1;
        sl.playSound(null, worldPosition.above(2), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.2f, 1.2f);
        sync();
    }

    private void klaar(ServerLevel sl, long nu) {
        start = -1;
        rustTot = nu + RUST;
        for (ServerPlayer speler : inzittenden()) {
            laatUit(speler);
            Minigames.give(speler, new ItemStack(ModItems.KERMISBON.get()));
            speler.sendOverlayMessage(Component.translatable("gui.guhs.kermisrit.klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
            GidsFeature.grant(speler, "guhmension/kermisrit_" + soort().id);
        }
        sync();
    }

    private void deuntje(ServerLevel sl, long nu) {
        float toon = 1f + 0.25f * (float) ((nu / 10) % 5);
        sl.playSound(null, worldPosition.above(soort().hoogte / 2), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1f, toon);
    }

    /** Out at the gate, on solid ground. */
    public void laatUit(ServerPlayer speler) {
        Entity voertuig = speler.getVehicle();
        if (voertuig instanceof KermisritStoel stoel) {
            stoel.uitstappen = true;
            speler.stopRiding();
            stoel.uitstappen = false;
        }
        Vec3 plek = uitstapPlek();
        speler.dismountTo(plek.x, plek.y, plek.z);
        speler.fallDistance = 0;
    }

    public List<ServerPlayer> inzittenden() {
        List<ServerPlayer> lijst = new ArrayList<>();
        for (KermisritStoel stoel : stoelen) {
            if (stoel != null && !stoel.isRemoved() && stoel.getFirstPassenger() instanceof ServerPlayer speler) {
                lijst.add(speler);
            }
        }
        return lijst;
    }

    /** Someone clicked a seat, or the ticket post (index -1). */
    public void klik(ServerPlayer speler, int index) {
        if (level == null || speler.isPassenger()) {
            return;
        }
        if (rijdt()) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.kermisrit.bezig").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (Minigames.busyElsewhere(speler, "kermisrit")) {
            return;
        }
        KermisritStoel stoel = index >= 0 && index < stoelen.length && vrij(stoelen[index]) ? stoelen[index] : eersteVrij();
        if (stoel == null) {
            speler.sendOverlayMessage(Component.translatable("gui.guhs.kermisrit.vol").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (speler.startRiding(stoel)) {
            if (wachtTot < 0) {
                wachtTot = level.getGameTime() + WACHT;
            }
            speler.sendOverlayMessage(Component.translatable("gui.guhs.kermisrit.instappen",
                    Component.translatable("block.guhs." + soort().blokId()), Math.max(1, (wachtTot - level.getGameTime()) / 20))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static boolean vrij(@Nullable KermisritStoel stoel) {
        return stoel != null && !stoel.isRemoved() && !stoel.isVehicle();
    }

    @Nullable
    private KermisritStoel eersteVrij() {
        for (KermisritStoel stoel : stoelen) {
            if (vrij(stoel)) {
                return stoel;
            }
        }
        return null;
    }

    /**
     * Seats are never saved: they are made here, whenever one is missing and its spot is loaded. Seats that are still
     * around (the controller's chunk unloaded, theirs did not) are taken back first.
     */
    public void zorgVoorStoelen(ServerLevel sl) {
        boolean compleet = loket != null && !loket.isRemoved();
        for (KermisritStoel stoel : stoelen) {
            compleet &= stoel != null && !stoel.isRemoved();
        }
        if (compleet) {
            return;
        }
        KermisritSoort soort = soort();
        AABB rond = new AABB(worldPosition).inflate(soort.bereik + 2, 0, soort.bereik + 2).expandTowards(0, soort.hoogte + 8, 0);
        for (KermisritStoel stoel : sl.getEntitiesOfClass(KermisritStoel.class, rond, s -> worldPosition.equals(s.thuis()))) {
            neem(stoel);
        }
        double t = tijd(0);
        for (int i = -1; i < stoelen.length; i++) {
            KermisritStoel stoel = i < 0 ? loket : stoelen[i];
            if (stoel != null && !stoel.isRemoved()) {
                continue;
            }
            Vec3 plek = wereldPlek(i, t);
            if (sl.hasChunkAt(BlockPos.containing(plek))) {
                KermisritStoel nieuw = new KermisritStoel(KermisrittenFeature.STOEL.get(), sl);
                nieuw.zet(worldPosition, i, plek, soort);
                sl.addFreshEntity(nieuw);
                neem(nieuw);
            }
        }
    }

    /** A seat of this ride reports in; a second one for the same place goes. */
    private void neem(KermisritStoel stoel) {
        int i = stoel.index();
        if (i >= stoelen.length) {
            stoel.discard();
            return;
        }
        KermisritStoel had = i < 0 ? loket : stoelen[i];
        if (had != null && had != stoel && !had.isRemoved()) {
            if (!stoel.isVehicle()) {
                stoel.discard();
            }
            return;
        }
        if (i < 0) {
            loket = stoel;
        } else {
            stoelen[i] = stoel;
        }
    }

    /** (Tests) a seat of this ride. */
    @Nullable
    public KermisritStoel stoel(int i) {
        return i < 0 ? loket : stoelen[i];
    }

    // --- saving, syncing ---------------------------------------------------------------------------------------------------------

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Start", start);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        start = input.getLongOr("Start", -1L);
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
}
