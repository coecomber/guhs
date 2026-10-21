package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * One spookbouw (design 2.1): an {@link Ontwerp} placed in the world as a faint see-through preview, with an owner. It
 * stays until it is built or cancelled. Everybody sees it (faint); the owner and who the owner added may turn, mirror and
 * move it, build it and take it away.
 * <p>
 * States: {@link Status#SPOOK} (only a preview), {@link Status#BOUWT} (blocks appear one by one), {@link Status#WACHT}
 * (stopped: material ran out, the rest stays a ghost; "Bouw!" again goes on). When the last block stands it is gone.
 * Kept by {@link Spoken} (per dimension, saved).
 */
public final class Spook {
    public enum Status { SPOOK, BOUWT, WACHT }

    final UUID id;
    final UUID eigenaar;
    String eigenaarNaam;
    final Set<UUID> helpers = new LinkedHashSet<>();
    Ontwerp ontwerp;
    BlockPos oorsprong;
    Rotation draai = Rotation.NONE;
    Mirror spiegel = Mirror.NONE;
    Status status = Status.SPOOK;
    /** The block that builds it ({@link Bouwer}, the Bouwkeet), or null: it builds by itself at {@link #tempo}. */
    @Nullable
    BlockPos bouwerPos;
    /** A Bank Guh of its own (a ghost without a keet; the vormstaf may set one). */
    @Nullable
    UUID bank;
    /** Blocks per second when it has no bouwer. */
    double tempo = 20;
    String naam = "";
    /** Who pressed "Bouw!" (inventory, undo list). */
    @Nullable
    UUID bouwer;
    /** The undo action being filled while it builds. */
    @Nullable
    UUID actie;

    // --- building progress ---
    /** Index into {@link #volgorde} of the next spot to look at. */
    int volgende;
    /** Spots that are still a ghost because their material was missing ("alsnog bouwen"). */
    final Set<Long> mist = new HashSet<>();
    int gedaan;
    double opgespaard;
    /** Cached building order (not saved: made again from the ontwerp). */
    @Nullable
    private List<Map.Entry<BlockPos, BlockState>> volgorde;

    Spook(UUID id, UUID eigenaar, String eigenaarNaam, Ontwerp ontwerp, BlockPos oorsprong) {
        this.id = id;
        this.eigenaar = eigenaar;
        this.eigenaarNaam = eigenaarNaam;
        this.ontwerp = ontwerp;
        this.oorsprong = oorsprong.immutable();
    }

    // =====================================================================================================================
    // reading
    // =====================================================================================================================

    public UUID id() {
        return id;
    }

    public UUID eigenaar() {
        return eigenaar;
    }

    public String eigenaarNaam() {
        return eigenaarNaam;
    }

    public Ontwerp ontwerp() {
        return ontwerp;
    }

    public BlockPos oorsprong() {
        return oorsprong;
    }

    public Rotation draai() {
        return draai;
    }

    public Mirror spiegel() {
        return spiegel;
    }

    public Status status() {
        return status;
    }

    @Nullable
    public BlockPos bouwerPos() {
        return bouwerPos;
    }

    @Nullable
    public UUID bank() {
        return bank;
    }

    public double tempo() {
        return tempo;
    }

    public String naam() {
        return naam;
    }

    public int gedaan() {
        return gedaan;
    }

    public int totaal() {
        return ontwerp.aantal();
    }

    public Set<UUID> helpers() {
        return helpers;
    }

    /** May this player change, build or cancel it (the owner, someone the owner added, an op)? */
    public boolean mag(ServerPlayer p) {
        return p.getUUID().equals(eigenaar) || helpers.contains(p.getUUID()) || p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    /** Every block where it lands now, in building order. */
    public List<Map.Entry<BlockPos, BlockState>> volgorde() {
        if (volgorde == null) {
            volgorde = ontwerp.geplaatst(oorsprong, draai, spiegel);
        }
        return volgorde;
    }

    /** The box it fills in the world. */
    public AABB doos() {
        BlockPos a = null, b = null;
        for (Map.Entry<BlockPos, BlockState> e : volgorde()) {
            BlockPos p = e.getKey();
            a = a == null ? p : new BlockPos(Math.min(a.getX(), p.getX()), Math.min(a.getY(), p.getY()), Math.min(a.getZ(), p.getZ()));
            b = b == null ? p : new BlockPos(Math.max(b.getX(), p.getX()), Math.max(b.getY(), p.getY()), Math.max(b.getZ(), p.getZ()));
        }
        return a == null ? new AABB(oorsprong) : new AABB(a.getX(), a.getY(), a.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1);
    }

    /** The middle on top (the flag of the party, the progress bar of a ghost without a keet). */
    public BlockPos topMidden() {
        AABB d = doos();
        return BlockPos.containing(d.getCenter().x, d.maxY, d.getCenter().z);
    }

    /** Some spots that still have to be built (for the bouwguhs to walk to). */
    public List<BlockPos> nogTeDoen(int max) {
        List<BlockPos> uit = new ArrayList<>();
        List<Map.Entry<BlockPos, BlockState>> v = volgorde();
        for (int i = volgende; i < v.size() && uit.size() < max; i += Math.max(1, (v.size() - volgende) / Math.max(1, max))) {
            uit.add(v.get(i).getKey());
        }
        return uit;
    }

    /** How many blocks are still to come (ghost blocks, not counting the ones that wait for material). */
    public int nogTeGaan() {
        return Math.max(0, volgorde().size() - volgende);
    }

    // =====================================================================================================================
    // changing (Spoken / Spookbouw do this)
    // =====================================================================================================================

    void zet(BlockPos oorsprong, Rotation draai, Mirror spiegel) {
        this.oorsprong = oorsprong.immutable();
        this.draai = draai;
        this.spiegel = spiegel;
        this.volgorde = null;
        this.volgende = 0;
        this.mist.clear();
    }

    void zetOntwerp(Ontwerp ontwerp) {
        this.ontwerp = ontwerp;
        this.volgorde = null;
        this.volgende = 0;
        this.mist.clear();
        this.gedaan = 0;
    }

    // =====================================================================================================================
    // saving
    // =====================================================================================================================

    CompoundTag opslaan() {
        CompoundTag t = new CompoundTag();
        t.store("Id", UUIDUtil.CODEC, id);
        t.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        t.putString("EigenaarNaam", eigenaarNaam);
        ListTag h = new ListTag();
        helpers.forEach(u -> h.add(StringTag.valueOf(u.toString())));
        t.put("Helpers", h);
        t.put("Ontwerp", ontwerp.opslaan());
        t.putLong("Oorsprong", oorsprong.asLong());
        t.putInt("Draai", draai.ordinal());
        t.putInt("Spiegel", spiegel.ordinal());
        t.putInt("Status", status.ordinal());
        if (bouwerPos != null) {
            t.putLong("Bouwer", bouwerPos.asLong());
        }
        t.storeNullable("Bank", UUIDUtil.CODEC, bank);
        t.putDouble("Tempo", tempo);
        t.putString("Naam", naam);
        t.storeNullable("Bouwspeler", UUIDUtil.CODEC, bouwer);
        t.storeNullable("Actie", UUIDUtil.CODEC, actie);
        t.putInt("Volgende", volgende);
        t.putInt("Gedaan", gedaan);
        t.put("Mist", new LongArrayTag(mist.stream().mapToLong(Long::longValue).toArray()));
        return t;
    }

    static Spook laad(CompoundTag t) {
        Spook s = new Spook(t.read("Id", UUIDUtil.CODEC).orElse(UUID.randomUUID()), t.read("Eigenaar", UUIDUtil.CODEC).orElse(new UUID(0, 0)),
                t.getStringOr("EigenaarNaam", "?"), Ontwerp.laad(t.getCompoundOrEmpty("Ontwerp")), BlockPos.of(t.getLongOr("Oorsprong", 0L)));
        ListTag h = t.getListOrEmpty("Helpers");
        for (int i = 0; i < h.size(); i++) {
            try {
                s.helpers.add(UUID.fromString(h.getStringOr(i, "")));
            } catch (IllegalArgumentException ignored) {
                // (a broken line: skip it)
            }
        }
        s.draai = Rotation.values()[Math.floorMod(t.getIntOr("Draai", 0), Rotation.values().length)];
        s.spiegel = Mirror.values()[Math.floorMod(t.getIntOr("Spiegel", 0), Mirror.values().length)];
        s.status = Status.values()[Math.floorMod(t.getIntOr("Status", 0), Status.values().length)];
        s.bouwerPos = t.contains("Bouwer") ? BlockPos.of(t.getLongOr("Bouwer", 0L)) : null;
        s.bouwer = t.read("Bouwspeler", UUIDUtil.CODEC).orElse(null);
        s.bank = t.read("Bank", UUIDUtil.CODEC).orElse(null);
        s.tempo = t.getDoubleOr("Tempo", 20);
        s.naam = t.getStringOr("Naam", "");
        s.actie = t.read("Actie", UUIDUtil.CODEC).orElse(null);
        s.volgende = t.getIntOr("Volgende", 0);
        s.gedaan = t.getIntOr("Gedaan", 0);
        for (long l : t.getLongArray("Mist").orElse(new long[0])) {
            s.mist.add(l);
        }
        return s;
    }
}
