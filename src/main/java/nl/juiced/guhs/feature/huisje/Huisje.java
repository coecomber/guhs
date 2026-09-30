package nl.juiced.guhs.feature.huisje;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.band.Band;

import net.minecraft.core.UUIDUtil;
/**
 * One Guhhuisje (2.10): where it stands (dimension + controller block), which way its snoet (the door) faces, its size,
 * owner and unique name, its residents (band ids of guhs and maatjes) and per resident which chores it does. Kept in
 * {@link Huisjes} (SavedData guhs_huisjes). The home base is {@link Huisjes#BEREIK} blocks around it.
 */
public final class Huisje {
    final ResourceKey<Level> dim;
    final BlockPos pos;
    final Direction facing;
    final HuisjeMaat maat;
    final UUID eigenaar;
    /** 3.0: the owner's name (stored when placed, kept up to date at login): "Dit is het huisje van X". */
    String eigenaarNaam = "";
    String naam;
    final List<UUID> bewoners = new ArrayList<>();
    /** Per resident: "guh" or the maatje kind; and its name (for the screen when it isn't loaded). */
    final Map<UUID, String> soorten = new HashMap<>();
    final Map<UUID, String> namen = new HashMap<>();
    /** Chores switched on/off per resident (only what was set; the rest is the chore's default). */
    final Map<UUID, Map<String, Boolean>> klussen = new HashMap<>();

    Huisje(ResourceKey<Level> dim, BlockPos pos, Direction facing, HuisjeMaat maat, UUID eigenaar, String naam) {
        this.dim = dim;
        this.pos = pos.immutable();
        this.facing = facing;
        this.maat = maat;
        this.eigenaar = eigenaar;
        this.naam = naam;
    }

    public ResourceKey<Level> dim() {
        return dim;
    }

    /** The controller block (front row, middle). */
    public BlockPos pos() {
        return pos;
    }

    /** Which way the snoet (the door) faces. */
    public Direction facing() {
        return facing;
    }

    /** The spot in front of the door (where residents go in and come out). */
    public BlockPos deur() {
        return pos.relative(facing);
    }

    public HuisjeMaat maat() {
        return maat;
    }

    public UUID eigenaar() {
        return eigenaar;
    }

    /** 3.0: the owner's name ("" when unknown). */
    public String eigenaarNaam() {
        return eigenaarNaam;
    }

    public String naam() {
        return naam;
    }

    /** Band ids of the residents (guhs and maatjes). */
    public List<UUID> bewoners() {
        return List.copyOf(bewoners);
    }

    public boolean isVol() {
        return bewoners.size() >= maat.plekken();
    }

    /** "guh" or the maatje kind of a resident. */
    public String soort(UUID bewoner) {
        return soorten.getOrDefault(bewoner, "guh");
    }

    public String naamVan(UUID bewoner) {
        return namen.getOrDefault(bewoner, "");
    }

    /** The middle of the huisje (on its floor). */
    public Vec3 midden() {
        return midden(pos, facing, maat);
    }

    /** Within the home base: {@link Huisjes#BEREIK} horizontally from the middle, 8 up or down. */
    public boolean inGebied(BlockPos p) {
        Vec3 m = midden();
        double dx = p.getX() + 0.5 - m.x, dz = p.getZ() + 0.5 - m.z;
        return dx * dx + dz * dz <= (double) Huisjes.BEREIK * Huisjes.BEREIK && Math.abs(p.getY() - pos.getY()) <= 8;
    }

    public AABB gebied() {
        Vec3 m = midden();
        return new AABB(m.x - Huisjes.BEREIK, pos.getY() - 8, m.z - Huisjes.BEREIK, m.x + Huisjes.BEREIK, pos.getY() + 9, m.z + Huisjes.BEREIK);
    }

    /** Does this resident do this chore? (what the owner set, else the chore's default for a loaded resident, else on) */
    public boolean klusAan(UUID bewoner, String klusId) {
        Map<String, Boolean> m = klussen.get(bewoner);
        if (m != null && m.containsKey(klusId)) {
            return m.get(klusId);
        }
        Klus k = Klusjes.van(klusId);
        MinecraftServer s = Band.server();
        if (k != null && s != null && Band.zoekGeladen(s, bewoner) instanceof Mob mob) {
            return k.standaardAan(mob);
        }
        return true;
    }

    /** {@link #klusAan(UUID, String)} for a loaded resident (the chore's own default). */
    public boolean klusAan(Mob bewoner, String klusId) {
        Map<String, Boolean> m = klussen.get(Band.id(bewoner));
        if (m != null && m.containsKey(klusId)) {
            return m.get(klusId);
        }
        Klus k = Klusjes.van(klusId);
        return k == null || k.standaardAan(bewoner);
    }

    public void zetKlus(UUID bewoner, String klusId, boolean aan) {
        klussen.computeIfAbsent(bewoner, k -> new LinkedHashMap<>()).put(klusId, aan);
        Huisjes.dirty();
    }

    // --- the shape -------------------------------------------------------------------------------------------------------

    /** Sideways along the front (from the controller). */
    static Direction langs(Direction facing) {
        return facing.getClockWise();
    }

    /** Every block of a huisje with this controller, facing and size (the controller first). */
    public static List<BlockPos> blokken(BlockPos pos, Direction facing, HuisjeMaat maat) {
        List<BlockPos> out = new ArrayList<>();
        out.add(pos.immutable());
        int w = maat.breedte(), links = (w - 1) / 2;
        Direction langs = langs(facing), achter = facing.getOpposite();
        for (int h = 0; h < maat.hoogte(); h++) {
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < w; j++) {
                    BlockPos p = pos.relative(langs, i - links).relative(achter, j).above(h);
                    if (!p.equals(pos)) {
                        out.add(p);
                    }
                }
            }
        }
        return out;
    }

    public List<BlockPos> blokken() {
        return blokken(pos, facing, maat);
    }

    /** The middle of the footprint (on the floor). */
    public static Vec3 midden(BlockPos pos, Direction facing, HuisjeMaat maat) {
        int w = maat.breedte();
        double zij = (w - 1) / 2.0 - (w - 1) / 2, diep = (w - 1) / 2.0;
        Direction langs = langs(facing), achter = facing.getOpposite();
        return new Vec3(pos.getX() + 0.5 + langs.getStepX() * zij + achter.getStepX() * diep, pos.getY(),
                pos.getZ() + 0.5 + langs.getStepZ() * zij + achter.getStepZ() * diep);
    }

    /** The two eye windows (for the zzz and heart particles): left and right on the front, at eye height. */
    public Vec3 raam(boolean links) {
        Vec3 m = midden();
        double half = maat.breedte() / 2.0;
        Direction langs = langs(facing);
        double zij = (links ? -1 : 1) * half * 0.35;
        return new Vec3(m.x + facing.getStepX() * (half + 0.1) + langs.getStepX() * zij, pos.getY() + maat.hoogte() * 0.55,
                m.z + facing.getStepZ() * (half + 0.1) + langs.getStepZ() * zij);
    }

    // --- saving ----------------------------------------------------------------------------------------------------------

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Dim", dim.identifier().toString());
        t.putLong("Pos", pos.asLong());
        t.putString("Facing", facing.getName());
        t.putString("Maat", maat.id());
        t.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        t.putString("EigenaarNaam", eigenaarNaam);
        t.putString("Naam", naam);
        ListTag list = new ListTag();
        for (UUID b : bewoners) {
            CompoundTag c = new CompoundTag();
            c.store("Id", UUIDUtil.CODEC, b);
            c.putString("Soort", soort(b));
            c.putString("Naam", naamVan(b));
            CompoundTag k = new CompoundTag();
            klussen.getOrDefault(b, Map.of()).forEach(k::putBoolean);
            c.put("Klussen", k);
            list.add(c);
        }
        t.put("Bewoners", list);
        return t;
    }

    @Nullable
    static Huisje load(CompoundTag t) {
        Identifier dim = Identifier.tryParse(t.getStringOr("Dim", ""));
        if (dim == null || !t.read("Eigenaar", UUIDUtil.CODEC).isPresent()) {
            return null;
        }
        Direction facing = Direction.byName(t.getStringOr("Facing", ""));
        Huisje h = new Huisje(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(t.getLongOr("Pos", 0L)),
                facing == null || facing.getAxis().isVertical() ? Direction.NORTH : facing, HuisjeMaat.byId(t.getStringOr("Maat", "")),
                t.read("Eigenaar", UUIDUtil.CODEC).orElseThrow(), t.getStringOr("Naam", ""));
        h.eigenaarNaam = t.getStringOr("EigenaarNaam", "");
        ListTag list = t.getListOrEmpty("Bewoners");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompoundOrEmpty(i);
            UUID id = c.read("Id", UUIDUtil.CODEC).orElseThrow();
            h.bewoners.add(id);
            h.soorten.put(id, c.getStringOr("Soort", ""));
            h.namen.put(id, c.getStringOr("Naam", ""));
            CompoundTag k = c.getCompoundOrEmpty("Klussen");
            if (!k.isEmpty()) {
                Map<String, Boolean> m = new LinkedHashMap<>();
                for (String key : k.keySet()) {
                    m.put(key, k.getBooleanOr(key, false));
                }
                h.klussen.put(id, m);
            }
        }
        return h;
    }
}
