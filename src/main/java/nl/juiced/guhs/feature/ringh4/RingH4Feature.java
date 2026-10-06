package nl.juiced.guhs.feature.ringh4;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.registry.ModEntities;

/**
 * bbq2 (ring-h4): chapter 4 of the Knabbelring, "De Spiegel van Guhladriel" (DESIGN_130 4). Resources:
 * tools/features/ring_h4.py (+ ring_h4_bouw, ring_h4_modellen, ring_h4_tekst, ring_h4_wiki).
 * <ul>
 *   <li>The structure {@code guhs:guhladriel_boomstad}: Caras Guhladhon, the tree city in the golden wood, with the river
 *       Guhduin and the two giant guh statues of the Arguhnath in one build ({@link BoomstadStructure}, {@link Boomstad},
 *       {@link Plekken}). Exactly one per world, behind Guhdalfs sluier until the player's own story reaches chapter 4.</li>
 *   <li>The questline {@link #LIJN} ({@code ring_h4}, 7 steps, per player): 0 reach the gate (the narrator card
 *       {@value #KAART}), 1 Leguhlas at the gate, 2 up the great stair to Guhladriel, 3 a night on the guest flet, 4 the
 *       mirror (the cutscene {@link Spiegel#SCENE}), 5 the three gifts ({@code Gaven.geef}), 6 the elf boat down the river
 *       past the statues ({@link Vaart}, {@link ElfenbootjeEntity}) to the landing.</li>
 *   <li>Who says what: {@link Rollen}; what happens by itself: {@link RingH4Events}; dev checks: {@link RingH4Commands}.</li>
 * </ul>
 * Nothing here can hurt a player: there is no enemy in this chapter, the river is kaassaus, and nobody takes fall damage
 * inside the city.
 */
public final class RingH4Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);

    /** The NpcRollen plekken of the chapter's characters, the id of its narrator card. */
    public static final String POORT = "boomstad_poort", STAD = "boomstad", STEIGER = "boomstad_steiger", KAART = "ring_h4";

    /** The Spiegel van Guhladriel. */
    public static final DeferredBlock<Spiegel.Blok> SPIEGEL = BLOCKS.registerBlock("ringh4_spiegel", Spiegel.Blok::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(2.5f, 30f).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s -> 10)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> SPIEGEL_ITEM = ITEMS.registerSimpleBlockItem(SPIEGEL);

    /** The elf boat. */
    public static final DeferredHolder<EntityType<?>, EntityType<ElfenbootjeEntity>> ELFENBOOTJE = ENTITY_TYPES.register("ringh4_elfenbootje",
            () -> EntityType.Builder.<ElfenbootjeEntity>of(ElfenbootjeEntity::new, MobCategory.MISC).sized(1.5f, 0.6f).clientTrackingRange(10).updateInterval(3)
                    .fireImmune().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("ringh4_elfenbootje"))));

    /** The placement of the tree city (a guhs:burcht that lies on a cave floor between two ends). */
    public static final DeferredHolder<StructureType<?>, StructureType<BoomstadStructure>> BOOMSTAD_TYPE =
            STRUCTURE_TYPES.register("ringh4_boomstad", () -> () -> BoomstadStructure.CODEC);

    /**
     * The questline of the chapter (ring-kern refers to this field: {@code Ring.lijn(4)}). Texts: tools/features/ring_h4.py.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_h4", "knabbelring").stappen(7).na("ring_h3").icoon("guhs:ringh4_spiegel")
            .beloningen(p -> {
                boolean binnen = RingH4Feature.LIJN.stap(p) >= 6;
                return List.of(Verhaallijn.beloning("guhs:lichtflesje", binnen), Verhaallijn.beloning("guhs:elfenmanteltje", binnen),
                        Verhaallijn.beloning("guhs:elfentouw", binnen));
            })
            .doel(Boomstad::doel)
            .registreer();

    /** The cast of the city: {Bezetting id, kind, spot ({@link Plekken}), NpcRollen plek, yaw in the template, first step, last step}. */
    private static final Object[][] BEWONERS = {
            {"ringh4_leguhlas_poort", GuhNpcEntity.Kind.LEGUHLAS, "leguhlas_poort", POORT, 90f, 0, 1},
            {"ringh4_guhladriel_zaal", GuhNpcEntity.Kind.GUHLADRIEL, "zaal", STAD, 90f, 0, 3},
            {"ringh4_guhladriel_dal", GuhNpcEntity.Kind.GUHLADRIEL, "dal", STAD, 135f, 4, 5},
            {"ringh4_guhladriel_thuis", GuhNpcEntity.Kind.GUHLADRIEL, "zaal", STAD, 90f, 6, 99},
            {"ringh4_leguhlas_steiger", GuhNpcEntity.Kind.LEGUHLAS, "leguhlas_steiger", STEIGER, 90f, 2, 99},
            {"ringh4_gimguh_steiger", GuhNpcEntity.Kind.GIMGUH, "gimguh_steiger", STEIGER, 90f, 2, 99}};
    /** The moored boats: {Bezetting id, spot, kind of boat, yaw in the template}. */
    private static final Object[][] BOTEN = {
            {"ringh4_boot", "boot", ElfenbootjeEntity.GIDS, -90f}, {"ringh4_boot_deco_1", "boot_deco_1", ElfenbootjeEntity.DECO, -90f},
            {"ringh4_boot_deco_2", "boot_deco_2", ElfenbootjeEntity.DECO, -90f}, {"ringh4_boot_terug", "boot_terug", ElfenbootjeEntity.TERUG, 90f}};

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        NeoForge.EVENT_BUS.register(RingH4Events.class);
        NeoForge.EVENT_BUS.addListener(RingH4Commands::register);

        // hidden and protected (one box around the whole build) until the player's own story is here
        Ring.sluier(Boomstad.STRUCTUUR, 6, 4);
        Verteller.registreer(KAART, 4, LIJN.id());
        Cutscene scene = Spiegel.SCENE;       // (registered when the class loads: both sides know it by its id)
        if (!scene.id().equals(Spiegel.SCENE_ID)) {
            throw new IllegalStateException("ringh4: the mirror scene");
        }
        Rollen.registreer();

        // whoever lives here stays here: the template brings them, Bezetting brings them back (with the same step range)
        for (Object[] b : BEWONERS) {
            GuhNpcEntity.Kind kind = (GuhNpcEntity.Kind) b[1];
            String plek = (String) b[3];
            float yaw = (float) b[4];
            int van = (int) b[5], tot = (int) b[6];
            Bezetting.wezen((String) b[0], Boomstad.STRUCTUUR, null, Plekken.blok((String) b[2]), (level, pos, draai) -> {
                GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
                if (npc == null) {
                    return null;
                }
                npc.setKind(kind);
                npc.roleData.putString(NpcRollen.PLEK, plek);
                float y = Cutscene.wereldYaw(draai, yaw);
                npc.snapTo(pos.x, pos.y, pos.z, y, 0f);
                npc.setYBodyRot(y);
                npc.setYHeadRot(y);
                Zicht.alleenBij(npc, LIJN.id(), van, tot);
                return npc;
            }, 8);
        }
        for (Object[] b : BOTEN) {
            int soort = (int) b[2];
            float yaw = (float) b[3];
            double hoogte = Plekken.punt((String) b[1]).y - Math.floor(Plekken.punt((String) b[1]).y);
            Bezetting.wezen((String) b[0], Boomstad.STRUCTUUR, null, Plekken.blok((String) b[1]), (level, pos, draai) -> {
                ElfenbootjeEntity boot = ELFENBOOTJE.get().create(level, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
                if (boot == null) {
                    return null;
                }
                boot.setSoort(soort);
                boot.snapTo(pos.x, pos.y + hoogte, pos.z, Cutscene.wereldYaw(draai, yaw), 0f);
                return boot;
            }, 6);
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SPIEGEL_ITEM.get()));
    }

    private RingH4Feature() {
    }
}
