package nl.juiced.guhs.feature.verhaal;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (verhaal engine): the demo story "demo", a tiny questline that uses every part of the engine, for dev checks and
 * as the worked example for the slices. It only exists in a dev environment ({@link #AAN}); a released jar has no demo
 * line in the Guhdex. {@code /guhs verhaal demo} (op), standing on flat ground with ten free blocks to the south:
 * <ol>
 *   <li>the narrator card "demo" (a drawn map, three lines, "Verder");</li>
 *   <li>the cutscene "verhaal_demo" (camera path with a cut, your stand-in, a guh that walks up, an NPC, subtitles, a sound,
 *       hearts, a shake, a fade), then step 2;</li>
 *   <li>a smoke wall ("demo_plek", Guhdalfs sluier) ten blocks south that pushes you back until
 *       {@code /guhs verhaal demo open} finishes the line; the second line "demo_vervolg" is a "???" on the travel map
 *       until then.</li>
 * </ol>
 * The objective line, the Guhdex page with the travel map and the Superkompas entry "Mijn verhaal" follow it all the way.
 * {@code /guhs verhaal demo wis} starts over. Texts and pictures: tools/features/verhaal_motor.py {@code demo(h)}.
 */
public final class VerhaalDemo {
    /** The demo only exists in a dev environment (dev client, dev server, game tests). */
    public static final boolean AAN = !FMLEnvironment.isProduction();
    public static final String PLEK = "demo_plek";
    private static final String ANKER = "guhs_demo_anker", DIM = "guhs_demo_dim";

    static Verhaallijn lijn, vervolg;
    static Cutscene scene;

    /** The demo line (null in a released jar). */
    public static Verhaallijn lijn() {
        return lijn;
    }

    public static Verhaallijn vervolg() {
        return vervolg;
    }

    public static Cutscene scene() {
        return scene;
    }

    static void register() {
        if (!AAN) {
            return;
        }
        lijn = Verhaallijn.maak("demo", "demo").stappen(3).icoon("minecraft:spyglass").doelregel(true)
                .sleutel((p, stap) -> stap == 2 && GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()) > 0 ? "2_knabbel" : String.valueOf(stap))
                .extraSleutels("2_knabbel")
                .nodig((p, stap) -> stap == 2 ? List.of(Verhaallijn.nodig("guhs:kaas_knabbels", GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()), 1)) : List.of())
                .beloningen(p -> List.of(Verhaallijn.beloning("guhs:kaas_knabbels", lijn.klaar(p)),
                        Verhaallijn.beloning("minecraft:spyglass", "gui.guhs.verhalen.demo.beloning.kijker", lijn.klaar(p))))
                .doel((p, stap) -> doel(p, stap))
                .registreer();
        vervolg = Verhaallijn.maak("demo_vervolg", "demo").stappen(1).icoon("minecraft:map").doelregel(true).na("demo").registreer();
        Reiskaarten.registreer(new Reiskaart("demo", "demo", List.of(new Halte("demo", 1, 52, 112, null), new Halte("demo_vervolg", 2, 196, 52, PLEK))));
        Verteller.registreer("demo", 3, "demo");
        Sluiers.registreer(PLEK, 2, p -> lijn.klaar(p));
        scene = Cutscene.maak("verhaal_demo").duur(190).bij("demo").kaart("demo").verbergEcht(12)
                .speler(new Vec3(0.5, 0, 0.5), 0)
                .guh("guh", GuhVariant.NORMAL, new Vec3(0.5, 0, 6.5), 180)
                .npc("reisguh", GuhNpcEntity.Kind.REISGUH, new Vec3(3.5, 0, 4.5), 90)
                .camera(0, new Vec3(6.0, 3.2, -2.0), new Vec3(0.5, 1.0, 3.0))
                .camera(60, new Vec3(5.0, 2.4, 1.0), new Vec3(0.5, 1.0, 3.0))
                .camera(105, new Vec3(4.0, 1.8, 5.5), new Vec3(0.5, 1.0, 2.0))
                .cameraKnipVolgt(106, new Vec3(2.2, 1.3, 0.6), "guh")
                .cameraVolgt(160, new Vec3(-1.6, 1.5, 0.8), "guh")
                .loop("guh", 20, 80, new Vec3(0.5, 0, 2.5))
                .kijk("reisguh", 60, new Vec3(0.5, 1, 2.5))
                .kijk("speler", 0, new Vec3(0.5, 1, 6.5))
                .zeg(8, "", "begin", 60)
                .zeg(88, "guh", "hallo", 46)
                .zeg(138, Cutscene.SPELER, "antwoord", 40)
                .animatie("guh", 84, "spring")
                .animatie(Cutscene.SPELER, 140, "zwaai")
                .geluid(84, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.2f)
                .deeltjes(86, ParticleTypes.HEART, new Vec3(0.5, 1.3, 2.5), 8, 0.4)
                .schud(86, 0.8f, 10)
                .zwart(178, 190)
                .registreer();
    }

    private static Doel doel(ServerPlayer p, int stap) {
        CompoundTag saved = GuhQuests.saved(p);
        Identifier dim = Identifier.tryParse(saved.getStringOr(DIM, ""));
        if (dim == null || !saved.contains(ANKER)) {
            return null;
        }
        BlockPos anker = BlockPos.of(saved.getLongOr(ANKER, 0L));
        return Doel.plek(ResourceKey.<Level>create(Registries.DIMENSION, dim), stap < 2 ? anker : anker.south(12),
                Component.translatable("gui.guhs.verhalen.demo.doel." + (stap < 2 ? "start" : "sluier")));
    }

    /** The smoke wall of the demo, ten blocks south of this spot. */
    static Sluiers.Zone zone(ServerPlayer p, BlockPos anker) {
        return Sluiers.zetPlek(p.level(), PLEK, new BoundingBox(anker.getX() - 3, anker.getY() - 2, anker.getZ() + 10, anker.getX() + 3,
                anker.getY() + 6, anker.getZ() + 15));
    }

    /** /guhs verhaal demo: (re)starts the demo where the player stands, from the step they are at. */
    static int speel(ServerPlayer p) {
        BlockPos anker = p.blockPosition();
        CompoundTag saved = GuhQuests.saved(p);
        saved.putLong(ANKER, anker.asLong());
        saved.putString(DIM, p.level().dimension().identifier().toString());
        Sluiers.wisPlekken(p.level(), PLEK);
        zone(p, anker);
        lijn.begin(p);
        int stap = lijn.stap(p);
        if (stap == 0) {
            Verteller.toon(p, "demo", a -> {
                lijn.verder(a, 0);
                film(a, anker);
            });
        } else if (stap == 1) {
            film(p, anker);
        } else {
            p.sendSystemMessage(Component.literal("demo: step " + stap + " of 3. /guhs verhaal demo open finishes it, /guhs verhaal demo wis starts over.")
                    .withStyle(ChatFormatting.GRAY));
        }
        VerhaalSync.sync(p);
        return stap;
    }

    private static void film(ServerPlayer p, BlockPos anker) {
        Cutscenes.speel(p, scene, anker, Rotation.NONE, a -> lijn.verder(a, 1));
    }

    /** /guhs verhaal demo sluier: only the smoke wall, ten blocks south. */
    static int sluier(ServerPlayer p) {
        Sluiers.wisPlekken(p.level(), PLEK);
        zone(p, p.blockPosition());
        return 1;
    }

    /** /guhs verhaal demo open: the line is done, the sluier dissolves, the second line opens. */
    static int open(ServerPlayer p) {
        lijn.zet(p, lijn.stappen());
        return 1;
    }

    /** /guhs verhaal demo wis: start over. */
    static int wis(ServerPlayer p) {
        lijn.wis(p);
        vervolg.wis(p);
        Cutscenes.vergeet(p, scene.id());
        Verteller.vergeet(p, "demo");
        VerhaalSync.kies(p, "");
        return 1;
    }

    private VerhaalDemo() {
    }
}
