package nl.juiced.guhs.feature.einde2.vetbootje;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nl.juiced.guhs.storage.GuhSavedData;
import nl.juiced.guhs.storage.Nbt;

/**
 * einde2, slice vetbootje: the fleet book of the world (saved data guhs:einde2_vetbootje_vloot, with the overworld). Per player:
 * whether they own a vetbootje, its looks (colour, sail, name), the 27 slots of its chest, which entity is the boat right now,
 * and the same for the heteluchtballon. The boat ENTITY is only a body: when it is lost (unloaded far away, another
 * dimension, gone) the vetfluitje makes a new body from this book, with chest and looks. Every new body gets the next
 * number ({@code nr}); a body with an older number knows it was replaced and takes itself away the moment it is loaded
 * again, so a player never has two.
 */
public final class Vloot extends SavedData {
    public static final SavedDataType<Vloot> TYPE = GuhSavedData.tagType("einde2_vetbootje_vloot", Vloot::new, Vloot::load, v -> v.save(new CompoundTag()));
    public static final int KIST = 27;

    /** One player's page of the book. */
    public final class Boot {
        /** Does this player own a vetbootje ({@link Vetbootje#geef})? */
        public boolean heeft;
        /** The number of the body that counts; older bodies take themselves away. */
        public int nr;
        public DyeColor kleur = Vetbootje.STANDAARD_KLEUR;
        public Zeil zeil = Zeil.MIKA;
        /** The name the player gave it; null = "Vetbootje van &lt;eigenaar&gt;". */
        @Nullable
        public Component naam;
        /** The owner's name as last seen (for the default name; the owner may be offline when the boat is drawn). */
        public String eigenaarNaam = "";
        /** The body that counts (it may be unloaded). */
        @Nullable
        public UUID entiteit;
        public final SimpleContainer kist = new SimpleContainer(KIST) {
            @Override
            public void setChanged() {
                super.setChanged();
                Vloot.this.setDirty();
            }
        };
        /** Does this player own the heteluchtballon ({@link Heteluchtballon#geef}), and which copy of it counts? */
        public boolean ballon;
        public int ballonNr;

        Boot() {
        }

        /** The name on the boat: the player's own, or the default. */
        public Component toonNaam() {
            return naam != null ? naam : Component.translatable("gui.guhs.einde2_vetbootje.naam.standaard", eigenaarNaam);
        }

        public void bewaar() {
            setDirty();
        }
    }

    private final Map<UUID, Boot> boten = new HashMap<>();

    public static Vloot van(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** This player's page (an empty one when they have nothing yet). */
    public Boot boot(UUID speler) {
        return boten.computeIfAbsent(speler, id -> new Boot());
    }

    // --- saving -------------------------------------------------------------------------------------------------------------

    @Nullable
    private static HolderLookup.Provider registers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }

    CompoundTag save(CompoundTag tag) {
        HolderLookup.Provider reg = registers();
        ListTag lijst = new ListTag();
        for (Map.Entry<UUID, Boot> e : boten.entrySet()) {
            Boot b = e.getValue();
            if (!b.heeft && !b.ballon && b.naam == null && b.kist.isEmpty() && b.nr == 0 && b.ballonNr == 0
                    && b.kleur == Vetbootje.STANDAARD_KLEUR && b.zeil == Zeil.MIKA) {
                continue;                       // (an untouched page: somebody we only looked up)
            }
            CompoundTag t = new CompoundTag();
            t.store("Speler", UUIDUtil.CODEC, e.getKey());
            t.putBoolean("Heeft", b.heeft);
            t.putInt("Nr", b.nr);
            t.putInt("Kleur", b.kleur.getId());
            t.putString("Zeil", b.zeil.name());
            t.putString("EigenaarNaam", b.eigenaarNaam);
            if (b.entiteit != null) {
                t.store("Entiteit", UUIDUtil.CODEC, b.entiteit);
            }
            if (b.naam != null && reg != null) {
                ComponentSerialization.CODEC.encodeStart(reg.createSerializationContext(NbtOps.INSTANCE), b.naam).result().ifPresent(n -> t.put("Naam", n));
            }
            ListTag kist = new ListTag();
            for (int i = 0; i < KIST && reg != null; i++) {
                ItemStack stack = b.kist.getItem(i);
                if (!stack.isEmpty()) {
                    CompoundTag vak = new CompoundTag();
                    vak.putInt("Vak", i);
                    vak.put("Ding", Nbt.saveStack(reg, stack));
                    kist.add(vak);
                }
            }
            t.put("Kist", kist);
            t.putBoolean("Ballon", b.ballon);
            t.putInt("BallonNr", b.ballonNr);
            lijst.add(t);
        }
        tag.put("Boten", lijst);
        return tag;
    }

    static Vloot load(CompoundTag tag) {
        Vloot v = new Vloot();
        HolderLookup.Provider reg = registers();
        ListTag lijst = tag.getListOrEmpty("Boten");
        for (int n = 0; n < lijst.size(); n++) {
            CompoundTag t = lijst.getCompoundOrEmpty(n);
            UUID speler = t.read("Speler", UUIDUtil.CODEC).orElse(null);
            if (speler == null) {
                continue;
            }
            Boot b = v.new Boot();
            b.heeft = t.getBooleanOr("Heeft", false);
            b.nr = t.getIntOr("Nr", 0);
            b.kleur = DyeColor.byId(t.getIntOr("Kleur", Vetbootje.STANDAARD_KLEUR.getId()));
            b.zeil = Zeil.vanNaam(t.getStringOr("Zeil", ""));
            b.eigenaarNaam = t.getStringOr("EigenaarNaam", "");
            b.entiteit = t.read("Entiteit", UUIDUtil.CODEC).orElse(null);
            Tag naam = t.get("Naam");
            if (naam != null && reg != null) {
                b.naam = ComponentSerialization.CODEC.parse(reg.createSerializationContext(NbtOps.INSTANCE), naam).result().orElse(null);
            }
            ListTag kist = t.getListOrEmpty("Kist");
            for (int i = 0; i < kist.size() && reg != null; i++) {
                CompoundTag vak = kist.getCompoundOrEmpty(i);
                int plek = vak.getIntOr("Vak", -1);
                if (plek >= 0 && plek < KIST) {
                    b.kist.setItem(plek, Nbt.parseStack(reg, vak.getCompoundOrEmpty("Ding")));
                }
            }
            b.ballon = t.getBooleanOr("Ballon", false);
            b.ballonNr = t.getIntOr("BallonNr", 0);
            v.boten.put(speler, b);
        }
        v.setDirty(false);
        return v;
    }
}
