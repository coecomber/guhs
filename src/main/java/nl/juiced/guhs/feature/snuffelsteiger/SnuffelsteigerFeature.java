package nl.juiced.guhs.feature.snuffelsteiger;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.snuffel.Snuffel;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;
import nl.juiced.guhs.feature.snuffel.SnuffelHond;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Het Snuffeleiland, the DOCK and the OPENING (DESIGN_VERHALENPAD C "Getting there"). Builds on the kern
 * (feature/snuffel: the questline, the choice screen, the dogs, travel). Resources: tools/features/snuffel_steiger.py
 * (+ snuffel_steiger_bouw.py: the template; snuffel_steiger_modellen.py: the boat and the puppies' lying down).
 * <ul>
 *   <li>The structure {@code guhs:steigerhuisje} in the Guhmensie: a cottage on a little quay with a pier out over a
 *   Diepe Guhzee, on the shores of new terrain and once for certain ({@link KustStructure}: the structure type that finds
 *   a shore and turns the pier to the sea). In the Superkompas (tab Verhalen), the goal of "Mijn verhaal" for the first
 *   two steps of the story, protected like every guh building.</li>
 *   <li>Who lives there ({@link SteigerBewoner}, kept at every copy by {@code Bezetting}): the sick puppy Kleine Wiebel
 *   in its bed, Buurvrouw Mandje next to it, Kapitein Zoutsnoet at the end of the pier, and his boat
 *   ({@link SteigerBoot}).</li>
 *   <li>The story, per player ({@link SteigerVerhaal}): the feast and the collapse, the sickbed, the choice of dog and
 *   companion, the crossing with the storm and the jump, the hand-over to the island; later crossings; coming home.
 *   The four cutscenes: {@link SteigerScenes}.</li>
 * </ul>
 * No blocks, no items, no payloads of its own.
 */
public final class SnuffelsteigerFeature {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<KustStructure>> KUST_STEIGER =
            STRUCTURE_TYPES.register("kust_steiger", () -> () -> KustStructure.CODEC);

    public static final DeferredHolder<EntityType<?>, EntityType<SteigerBewoner>> STEIGER_BEWONER = ENTITY_TYPES.register("steiger_bewoner",
            () -> EntityType.Builder.<SteigerBewoner>of(SteigerBewoner::new, MobCategory.MISC).sized(0.6f, 0.85f).eyeHeight(0.65f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("steiger_bewoner"))));
    public static final DeferredHolder<EntityType<?>, EntityType<SteigerBoot>> STEIGER_BOOT = ENTITY_TYPES.register("steiger_boot",
            () -> EntityType.Builder.<SteigerBoot>of(SteigerBoot::new, MobCategory.MISC).sized(2.2f, 0.5f).clientTrackingRange(10).updateInterval(10)
                    .fireImmune().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("steiger_boot"))));

    // --- sounds (vanilla sounds, pitched, in sounds.json; sound effects only, no music) -------------------------------------------
    /** The puppy's whimper. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PIEP_GELUID = geluid("snuffelsteiger.piep");
    /** A soft thud (the collapse). */
    public static final DeferredHolder<SoundEvent, SoundEvent> PLOF_GELUID = geluid("snuffelsteiger.plof");
    public static final DeferredHolder<SoundEvent, SoundEvent> DONDER_GELUID = geluid("snuffelsteiger.donder");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEL_GELUID = geluid("snuffelsteiger.bel");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLANK_GELUID = geluid("snuffelsteiger.plank");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIEM_GELUID = geluid("snuffelsteiger.riem");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLF_GELUID = geluid("snuffelsteiger.golf");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    /** How many blocks around the build nothing can be broken or placed. */
    public static final int RAND = 2;
    /** The Bezetting ids of who lives at a dock. */
    public static final String ID_PUP = "snuffelsteiger_pup", ID_BUUR = "snuffelsteiger_buur", ID_KAPITEIN = "snuffelsteiger_kapitein",
            ID_BOOT = "snuffelsteiger_boot";

    private SnuffelsteigerFeature() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(STEIGER_BEWONER.get(), SnuffelHond.createAttributes().build()));

        // the building: protected, in the Superkompas, and where "Mijn verhaal" points for the first two steps
        Bescherming.registreer(Steiger.STRUCTUUR, RAND);
        SuperkompasItem.voegToe("verhalen", Steiger.STRUCTUUR);
        Snuffel.doel(SnuffelFeature.STAP_STEIGER, SnuffelFeature.STAP_UITVAREN,
                (p, stap) -> Doel.structuur(ModDimensions.GUHMENSION, Steiger.STRUCTUUR, Component.translatable("structure.guhs." + Steiger.STRUCTUUR)));

        // who lives there: at every copy, also after somebody /kill-ed them
        bewoner(ID_PUP, SteigerBewoner.PUP, Steiger.PUP, Steiger.PUP_YAW);
        bewoner(ID_BUUR, SteigerBewoner.BUUR, Steiger.BUUR, Steiger.BUUR_YAW);
        bewoner(ID_KAPITEIN, SteigerBewoner.KAPITEIN, Steiger.KAPITEIN, Steiger.KAPITEIN_YAW);
        Bezetting.wezen(ID_BOOT, Steiger.STRUCTUUR, null, blok(Steiger.BOOT), (level, plek, draai) -> {
            SteigerBoot boot = STEIGER_BOOT.get().create(level, EntitySpawnReason.STRUCTURE);
            if (boot == null) {
                return null;
            }
            Vec3 pos = precies(plek, Steiger.BOOT, draai);
            boot.snapTo(pos.x, pos.y, pos.z, Cutscene.wereldYaw(draai, Steiger.BOOT_YAW), 0f);
            return boot;
        }, 6);

        SteigerScenes.registreer();
        SteigerVerhaal.registreer();
        NeoForge.EVENT_BUS.addListener(SteigerVerhaal::opTick);
        NeoForge.EVENT_BUS.addListener(SteigerVerhaal::opLogout);
        NeoForge.EVENT_BUS.addListener(SteigerCommando::registreer);
    }

    /** The template block a template position (y above the deck) lies in. */
    static BlockPos blok(Vec3 template) {
        return new BlockPos((int) Math.floor(template.x), Steiger.G + 1 + (int) Math.floor(template.y), (int) Math.floor(template.z));
    }

    /**
     * The exact world position of a template position, given the world's bottom centre of its block ({@link #blok}) and how
     * the copy is turned: the part of a block that is left over is turned along.
     */
    static Vec3 precies(Vec3 blokMidden, Vec3 template, Rotation draai) {
        double dx = template.x - Math.floor(template.x) - 0.5, dy = template.y - Math.floor(template.y), dz = template.z - Math.floor(template.z) - 0.5;
        return switch (draai) {
            case CLOCKWISE_90 -> blokMidden.add(-dz, dy, dx);
            case CLOCKWISE_180 -> blokMidden.add(-dx, dy, -dz);
            case COUNTERCLOCKWISE_90 -> blokMidden.add(dz, dy, -dx);
            default -> blokMidden.add(dx, dy, dz);
        };
    }

    private static void bewoner(String id, String rol, Vec3 template, float yaw) {
        Bezetting.wezen(id, Steiger.STRUCTUUR, null, blok(template), (level, plek, draai) -> {
            SteigerBewoner e = STEIGER_BEWONER.get().create(level, EntitySpawnReason.STRUCTURE);
            if (e == null) {
                return null;
            }
            e.zetRol(rol);
            Vec3 pos = precies(plek, template, draai);
            float y = Cutscene.wereldYaw(draai, yaw);
            e.snapTo(pos.x, pos.y, pos.z, y, 0f);
            e.setYBodyRot(y);
            e.setYHeadRot(y);
            return e;
        }, 6);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }
}
