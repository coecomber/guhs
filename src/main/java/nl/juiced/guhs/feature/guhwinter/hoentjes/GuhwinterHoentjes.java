package nl.juiced.guhs.feature.guhwinter.hoentjes;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.10.9 Guhwinter, slice hoentjes: the <b>waterhoentje</b> ({@link WaterhoenEntity}) and the <b>alpine hoentje</b>
 * ({@link AlpinehoenEntity}). They stay after the event: rare, shy wild birds of the Guhmension.
 * <ul>
 *   <li>Natural spawns: {@link HoentjesSpawner} (a rare top-up around players: water edges, high/cold/snowy ground).</li>
 *   <li>Guhdex: two bonus pages (GuhDex.EXTRA), filled within {@link #DEX_BEREIK} blocks.</li>
 *   <li>The NOWAY chat line when you first see one (within {@link HoentjesEvents#NOWAY_BEREIK} blocks, in sight; once every
 *       ten minutes per species): {@link #noway}.</li>
 *   <li>The scene API for the kerstwandeling: {@link #spawnScene}.</li>
 *   <li>Spawn eggs (creative), op command {@code /guhs winter hoentje ...} ({@link HoentjesCommando}).</li>
 * </ul>
 * Resources: tools/features/guhwinter_hoentjes*.py.
 */
public final class GuhwinterHoentjes {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public static final String WATERHOEN = "waterhoen";
    public static final String ALPINEHOEN = "alpinehoen";
    /** The Guhdex page fills in this close (birds are hard to get right next to). */
    public static final double DEX_BEREIK = 10;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WaterhoenEntity>> WATERHOEN_TYPE = ENTITY_TYPES.register("guhwinter_waterhoen",
            () -> EntityType.Builder.of(WaterhoenEntity::new, MobCategory.CREATURE).sized(0.5f, 0.65f).eyeHeight(0.55f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhwinter_waterhoen"))));
    public static final DeferredHolder<EntityType<?>, EntityType<AlpinehoenEntity>> ALPINEHOEN_TYPE = ENTITY_TYPES.register("guhwinter_alpinehoen",
            () -> EntityType.Builder.of(AlpinehoenEntity::new, MobCategory.CREATURE).sized(0.55f, 0.6f).eyeHeight(0.5f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guhwinter_alpinehoen"))));

    public static final DeferredItem<SpawnEggItem> WATERHOEN_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "guhwinter_waterhoen_spawn_egg", WATERHOEN_TYPE);
    public static final DeferredItem<SpawnEggItem> ALPINEHOEN_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "guhwinter_alpinehoen_spawn_egg", ALPINEHOEN_TYPE);

    // --- sounds (tools/features/guhwinter_hoentjes_geluid.py) ----------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> WATERHOEN_ROEP = geluid("guhwinter_hoentjes.waterhoen_roep");
    public static final DeferredHolder<SoundEvent, SoundEvent> WATERHOEN_PIEP = geluid("guhwinter_hoentjes.waterhoen_piep");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALPINEHOEN_ROEP = geluid("guhwinter_hoentjes.alpinehoen_roep");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALPINEHOEN_PIEP = geluid("guhwinter_hoentjes.alpinehoen_piep");
    /** The little jingle with the NOWAY line (only the player who saw it hears it). */
    public static final DeferredHolder<SoundEvent, SoundEvent> NOWAY = geluid("guhwinter_hoentjes.noway");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOWAY_EXTREEM = geluid("guhwinter_hoentjes.noway_extreem");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The two Guhdex pages (bonus pages: GuhDex.EXTRA). */
    public static final List<GuhVariant> PAGINAS = List.of(GuhVariant.GUHWINTER_WATERHOEN, GuhVariant.GUHWINTER_ALPINEHOEN);

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(WATERHOEN_TYPE.get(), Hoentje.createAttributes().build());
            event.put(ALPINEHOEN_TYPE.get(), Hoentje.createAttributes().build());
        });
        GuhDex.creaturePage(GuhVariant.GUHWINTER_WATERHOEN, WATERHOEN_TYPE, DEX_BEREIK);
        GuhDex.creaturePage(GuhVariant.GUHWINTER_ALPINEHOEN, ALPINEHOEN_TYPE, DEX_BEREIK);
        NeoForge.EVENT_BUS.register(HoentjesEvents.class);
        HoentjesCommando.register();
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(WATERHOEN_SPAWN_EGG.get()));
        output.accept(new ItemStack(ALPINEHOEN_SPAWN_EGG.get()));
    }

    /** "waterhoen" / "alpinehoen" (also "waterhoentje", "alpine", "alpine_hoentje", "alpinehoentje"), else null. */
    @Nullable
    public static String soort(String naam) {
        String s = naam.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("guhwinter", "");
        if (s.startsWith("water")) {
            return WATERHOEN;
        }
        if (s.startsWith("alpine") || s.startsWith("alpien") || s.startsWith("alpen")) {
            return ALPINEHOEN;
        }
        return null;
    }

    @Nullable
    public static EntityType<? extends Hoentje> type(String soort) {
        String s = soort(soort);
        if (WATERHOEN.equals(s)) {
            return WATERHOEN_TYPE.get();
        }
        if (ALPINEHOEN.equals(s)) {
            return ALPINEHOEN_TYPE.get();
        }
        return null;
    }

    /**
     * The scene API (kerstwandeling): a hoentje of this {@code soort} ("waterhoen" or "alpinehoen") appears at {@code from}
     * (on the ground of that column, or on the water surface), walks (the waterhoen) or hops (the alpine hoentje) to
     * {@code to}, and vanishes there in a little puff. It can't be hurt, ignores players, is never saved (a restart mid-scene
     * just loses it) and gives no NOWAY chat line (the story guhs say it); seeing it does fill its Guhdex page. If it gets
     * stuck it nudges itself on; after a generous time limit it vanishes anyway.
     *
     * @return the hoentje (to look at, or to {@link Hoentje#poef} early), or null for an unknown soort
     */
    @Nullable
    public static Hoentje spawnScene(ServerLevel level, String soort, Vec3 from, Vec3 to) {
        EntityType<? extends Hoentje> type = type(soort);
        if (type == null) {
            LOGGER.warn("GuhwinterHoentjes.spawnScene: unknown soort {}", soort);
            return null;
        }
        Hoentje h = type.create(level, EntitySpawnReason.TRIGGERED);
        if (h == null) {
            return null;
        }
        BlockPos a = BlockPos.containing(from), b = BlockPos.containing(to);
        Vec3 start = new Vec3(from.x, Hoentje.grond(level, a), from.z);
        Vec3 doel = new Vec3(to.x, Hoentje.grond(level, b), to.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(doel.z - start.z, doel.x - start.x)) - 90.0);
        h.snapTo(start.x, start.y, start.z, yaw, 0);
        h.setYHeadRot(yaw);
        h.setYBodyRot(yaw);
        h.setPersistenceRequired();                       // (never despawns mid-scene; shouldBeSaved keeps it off disk)
        h.startScene(doel, ALPINEHOEN.equals(h.soort()) ? 1.15 : 1.0);
        level.addFreshEntity(h);
        return h;
    }

    /** The NOWAY chat line + jingle for this player only (what a wild one in view gives; for a story step that wants it too). */
    public static void noway(net.minecraft.server.level.ServerPlayer player, String soort) {
        HoentjesEvents.noway(player, soort);
    }

    private GuhwinterHoentjes() {
    }
}
