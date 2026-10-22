package nl.juiced.guhs.feature.knabbelvelden;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.Plek;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.guhpixel.GuhOpslag;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.taal.Tekst;

/**
 * 1.6.2: every tamed critter of the Guhdex tab "Mijn andere vadsjes" (pieppiepmuisjes, Schilly, Poepschilly, the
 * landdiertjes, the guhxolotl... every {@link PiepMaatje}) can retire to De Eeuwige Knabbelvelden too. It is kept as its own
 * pick-up item (the whole creature rides along in it, exactly as when you pick it up): {@link #ITEM} in the record's
 * opslag, with its kind ({@link #SOORT}: its entity id, like "Mijn andere vadsjes" groups them). Bringing it home gives that
 * item back; on the plot a see-through stand-in with a little halo of light walks about ({@link #geest}). Driven by the
 * entity registry: any new maatje kind takes part by itself.
 */
public final class Vadsjes {
    public static final String ITEM = "Item", SOORT = "Kind";

    /** Kind (entity id path) -> its entity type, for every maatje kind (filled the first time). */
    private static Map<String, EntityType<?>> soorten;

    private Vadsjes() {
    }

    /** Every maatje kind there is (entity id path -> type): the guhs entities whose creature is a {@link PiepMaatje}. */
    public static synchronized Map<String, EntityType<?>> soorten(Level level) {
        if (soorten == null) {
            Map<String, EntityType<?>> m = new HashMap<>();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (!Guhs.MODID.equals(key.getNamespace())) {
                    continue;
                }
                Entity e;
                try {
                    e = type.create(level, EntitySpawnReason.TRIGGERED);
                } catch (Exception ex) {
                    continue;
                }
                if (e instanceof PiepMaatje) {
                    m.put(key.getPath(), type);
                }
                if (e != null) {
                    e.discard();
                }
            }
            soorten = Map.copyOf(m);
        }
        return soorten;
    }

    /** A maatje's band id without making one (the entity UUID when it never had one). */
    public static UUID bandId(Entity e) {
        return e.getPersistentData().read(Band.BAND_ID, UUIDUtil.CODEC).orElse(e.getUUID());
    }

    /** The loaded maatje with this band id (any level), or null. Never a Knabbelvelden ghost. */
    @Nullable
    public static TamableAnimal zoekGeladen(MinecraftServer s, UUID id) {
        for (ServerLevel level : s.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof PiepMaatje m && !Geesten.isGeest(e) && id.equals(bandId(e))) {
                    return m.dier();
                }
            }
        }
        return null;
    }

    /** The maatje (loaded, owned by eigenaar) goes into retirement: it becomes its item, kept in the record. */
    static void bewaar(UUID eigenaar, TamableAnimal dier) {
        ServerLevel level = (ServerLevel) dier.level();
        PiepMaatje m = (PiepMaatje) dier;
        UUID id = Band.id(dier);
        dier.stopRiding();
        dier.ejectPassengers();
        if (dier.isLeashed()) {
            dier.removeLeash();
        }
        ItemStack item = PiepDierItem.van(dier, m.oppakItem());
        level.sendParticles(ParticleTypes.POOF, dier.getX(), dier.getY() + 0.3, dier.getZ(), 10, 0.2, 0.2, 0.2, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, dier.getX(), dier.getY() + 0.5, dier.getZ(), 8, 0.2, 0.3, 0.2, 0.04);
        dier.discard();
        bewaarItem(level.getServer(), eigenaar, id, item, level.dimension(), dier.blockPosition());
    }

    /** A maatje that is an item (out of a chest, pockets...) goes into retirement as it is. */
    static void bewaarItem(MinecraftServer s, UUID eigenaar, UUID id, ItemStack item, net.minecraft.resources.ResourceKey<Level> dim, BlockPos pos) {
        CompoundTag opslag = new CompoundTag();
        opslag.store("Id", UUIDUtil.CODEC, id);
        opslag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        Component naam = item.has(DataComponents.CUSTOM_NAME) ? item.getHoverName().copy() : Component.translatable("entity.guhs." + soortVan(item));
        Tekst.put(opslag, "Naam", naam);
        opslag.put("Looks", new CompoundTag());
        opslag.putString("Soort", PlekSoort.IN_DE_KNABBELVELDEN.id());
        opslag.putString(SOORT, soortVan(item));
        opslag.put(ITEM, ItemStack.CODEC.encodeStart(s.registryAccess().createSerializationContext(NbtOps.INSTANCE), item.copyWithCount(1)).getOrThrow());
        KnabbelveldenData.get(s).zet(eigenaar, new KnabbelveldenData.Rust(opslag, Band.dag(s), 0));
        GuhVolger.zet(eigenaar, id, new Plek(PlekSoort.IN_DE_KNABBELVELDEN, dim, pos, Component.translatable("gui.guhs.knabbelvelden.naam"),
                s.overworld().getGameTime()));
    }

    /** The kind of a maatje item: its entity's id path ("" when it is no maatje item). */
    static String soortVan(ItemStack item) {
        if (item.getItem() instanceof PiepDierItem p) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(p.type()).getPath();
        }
        return "";
    }

    /** The stored item of a retired maatje (empty when it can't be read). */
    static ItemStack item(MinecraftServer s, CompoundTag opslag) {
        return ItemStack.CODEC.parse(s.registryAccess().createSerializationContext(NbtOps.INSTANCE), opslag.getCompoundOrEmpty(ITEM))
                .result().orElse(ItemStack.EMPTY);
    }

    /**
     * A see-through stand-in of a retired maatje on its owner's plot: the same creature from its item, but wild (no owner,
     * no band id: it is never "the" maatje and never shows up in a Guhdex), invisible (the client draws it see-through:
     * mixin KnabbelGeestZichtMixin), invulnerable, kept near the plot spawn. Null when it can't be made.
     */
    @Nullable
    static TamableAnimal geest(ServerLevel level, KnabbelveldenData.Rust r, double x, double z) {
        ItemStack item = item(level.getServer(), r.opslag());
        if (!(item.getItem() instanceof PiepDierItem p)) {
            return null;
        }
        ItemStack kopie = item.copy();
        CustomData data = kopie.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag t = data.copyTag();
            t.remove("Owner");
            t.remove("Sitting");
            t.remove("OrderedToSit");
            CompoundTag nf = t.getCompoundOrEmpty("NeoForgeData");
            nf.remove(Band.BAND_ID);
            t.put("NeoForgeData", nf);
            kopie.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        }
        TamableAnimal dier = PiepDierItem.naar(kopie, level, p.type());
        if (dier == null) {
            return null;
        }
        dier.snapTo(x, Knabbelvelden.VLOER, z, level.getRandom().nextFloat() * 360f, 0f);
        dier.setInvisible(true);
        dier.setInvulnerable(true);
        dier.setPersistenceRequired();
        dier.setOrderedToSit(false);
        dier.setInSittingPose(false);
        if (!dier.hasCustomName()) {
            dier.setCustomName(GuhOpslag.naam(r.opslag()).copy());
        }
        return dier;
    }

    /** (tests) every kind's name, for parameterised round trips. */
    public static List<String> alleSoorten(Level level) {
        List<String> uit = new ArrayList<>(soorten(level).keySet());
        uit.sort(String::compareTo);
        return uit;
    }
}
