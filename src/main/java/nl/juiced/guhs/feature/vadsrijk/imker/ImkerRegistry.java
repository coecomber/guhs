package nl.juiced.guhs.feature.vadsrijk.imker;

import java.util.List;
import java.util.Set;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;

/**
 * T-IMKER: the secret zone-3 boss De Mika-imker (Mika de Imker) in the raatzaal under the Raattros: his entity, the hive and
 * the smoke pots, the pet Zoempje, the trophy, his arena ({@link ImkerVerhaal#arena}) and the dev commands
 * {@code /guhs vadsrijk imker ...}. Ids {@code vr_z3_imker_*}, lang {@code vadsrijk.z3imker.*}, generator
 * tools/features/vadsrijk_t_imker.py.
 */
public final class ImkerRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MikaImker>> MIKA = ENTITIES.register("vr_z3_imker_mika",
            () -> EntityType.Builder.<MikaImker>of(MikaImker::new, MobCategory.MONSTER).sized(1.5f, 1.8f).eyeHeight(1.4f).clientTrackingRange(16)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_z3_imker_mika"))));
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> KORF = doel("vr_z3_imker_korf", 1.6f, 2.2f);
    public static final DeferredHolder<EntityType<?>, EntityType<Z3Doel>> ROOKPOT = doel("vr_z3_imker_rookpot", 0.9f, 1.1f);

    public static final DeferredItem<Item> TROFEE = ITEMS.registerItem("vr_z3_imker_trofee", BazenLoot.Trofee::new, () -> new Item.Properties().stacksTo(16));

    /** The pet: a golden bee from the queen (first kill). */
    public static final Huisdier PET_ZOEMPJE = Huisdieren.registreer(new Huisdier("zoempje", "guhs:vr_z3_imker_mika", "vr_z3_imker_zoempje",
            "textures/vadsrijk/z3imker/zoempje.png", "vr_huisdier_zoempje", 0.7f, null));

    private ImkerRegistry() {
    }

    private static DeferredHolder<EntityType<?>, EntityType<Z3Doel>> doel(String id, float w, float h) {
        return ENTITIES.register(id, () -> EntityType.Builder.<Z3Doel>of(Z3Doel::new, MobCategory.MISC).sized(w, h).clientTrackingRange(12)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(MIKA.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.STEP_HEIGHT, 1.2).build());
            for (var t : List.of(KORF, ROOKPOT)) {
                e.put(t.get(), VadsrijkMob.basisAttributen().add(Attributes.KNOCKBACK_RESISTANCE, 1.0).build());
            }
        });
        Arenas.registreer(ImkerVerhaal.arena(), sl -> ImkerVerhaal.maak(sl));
        ImkerVerhaal.init();
        NeoForge.EVENT_BUS.addListener(ImkerRegistry::commando);
    }

    // ---- dev commands -------------------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static void naar(ServerPlayer p, ServerLevel sl, Vec3 v) {
        sl.getChunk(BlockPos.containing(v));
        p.teleportTo(sl, v.x, v.y + 0.1, v.z, Set.of(), p.getYRot(), p.getXRot(), true);
    }

    private static String pos(BlockPos b) {
        return b.getX() + " " + b.getY() + " " + b.getZ();
    }

    /** The boss now (null: not awake / not loaded). */
    static MikaImker baas(ServerLevel sl) {
        VadsrijkBaas b = Arenas.baas(sl, ImkerVerhaal.ARENA);
        return b instanceof MikaImker m ? m : null;
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("imker");
        // the story from the start: quest + 5 clues + hatch open, you stand at the hatch
        b.then(dev("klaarzetten").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            String t = ImkerVerhaal.klaarzetten(p);
            Arenas.reset(sl.getServer(), ImkerVerhaal.ARENA);
            naar(p, sl, Vec3.atBottomCenterOf(ImkerVerhaal.luik()).add(1.5, 0, 0));
            return zeg(c, t + "\nLuik: " + pos(ImkerVerhaal.luik()) + ", raatzaal: " + pos(ImkerVerhaal.zaal()) + ".");
        }));
        // straight into the fight: full HP, phase 1, you stand at the foot of the ladder
        b.then(dev("spawn").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            Spelers.data(p).gezien.add(ImkerVerhaal.GEZIEN_INTRO);
            sl.getChunk(ImkerVerhaal.zaal());
            Arenas.reset(sl.getServer(), ImkerVerhaal.ARENA);
            ImkerVerhaal.aanvraag = true;
            naar(p, sl, Vec3.atBottomCenterOf(ImkerVerhaal.zaal()).add(0, 0, 5));
            return zeg(c, "De Mika-imker wordt wakker in de raatzaal " + pos(ImkerVerhaal.zaal()) + " (lv " + ImkerVerhaal.LV + ", volle Vads). "
                    + "Rechtsklik de korf = draaien (shift = andersom), rechtsklik een rookpotje = rookwolk.");
        }));
        b.then(dev("fase").then(Commands.argument("n", IntegerArgumentType.integer(2, 3)).executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            MikaImker m = baas(sl);
            if (m == null) {
                return zeg(c, "Geen Mika-imker wakker. Eerst: /guhs vadsrijk imker spawn");
            }
            int n = IntegerArgumentType.getInteger(c, "n");
            double[] f = {0.66, 0.33};
            while (m.fase() < n && !m.geplofd()) {
                double doel = m.maxVads() * f[m.fase() - 1] - 1;
                if (m.vads() > doel) {
                    m.heal(doel - m.vads());
                }
                m.krijgSchade(1, null);
                if (m.fase() < n && m.giet()) {   // (immune while pouring: sting him first)
                    m.steek(sl);
                }
            }
            return zeg(c, "Fase nu " + m.fase() + (m.fase() == 2 ? ": hij giet op stok " + (m.stok + 1) + ". Draai de korf naar hem toe." : "."));
        })));
        // phase 2 shortcut: turn the hive at his perch and let it fire now
        b.then(dev("steek").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            MikaImker m = baas(sl);
            if (m == null || m.fase() != 2 || m.stok < 0) {
                return zeg(c, "Alleen in fase 2 (hij moet op een stok zitten).");
            }
            m.zetRichting(MikaImker.STOKKEN[m.stok]);
            m.korfVuur(sl, 30);
            return zeg(c, "De korf wijst naar stok " + (m.stok + 1) + " en spuit over 1,5 s.");
        }));
        b.then(dev("zwerm").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            MikaImker m = baas(Vadsrijk.level(c.getSource().getServer()));
            if (m == null) {
                return zeg(c, "Geen Mika-imker wakker.");
            }
            m.zwerm((ServerLevel) m.level(), p);
            return zeg(c, "Een zwerm komt achter je aan. Loop door een rookwolk (rechtsklik een rookpotje).");
        }));
        b.then(dev("bom").executes(c -> {
            MikaImker m = baas(Vadsrijk.level(c.getSource().getServer()));
            if (m == null) {
                return zeg(c, "Geen Mika-imker wakker.");
            }
            m.groteBom((ServerLevel) m.level());
            return zeg(c, "De grote kaasrookbom: ga in de gele cirkel bij de korf staan.");
        }));
        b.then(dev("intro").executes(c -> {
            ImkerVerhaal.intro(c.getSource().getPlayerOrException());
            return zeg(c, "De intro-scène (het portret moet de Mika-imker zijn, niet een boek).");
        }));
        b.then(dev("na").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Spelers.data(p).gezien.remove(ImkerVerhaal.GEZIEN_NA);
            ImkerVerhaal.na(p);
            return zeg(c, "De scène na het gevecht.");
        }));
        b.then(dev("zoempje").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            boolean nieuw = Huisdieren.geef(p, PET_ZOEMPJE.id());
            return zeg(c, nieuw ? "Zoempje is van jou (huisdierenmenu)." : "Je had Zoempje al.");
        }));
        b.then(dev("reset").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            ImkerVerhaal.aanvraag = false;
            ImkerVerhaal.vergeetScenes(p);
            Arenas.reset(sl.getServer(), ImkerVerhaal.ARENA);
            return zeg(c, "Mika-imker opgeruimd, de scènes zijn vergeten (de quest zelf niet: gebruik klaarzetten).");
        }));
        b.then(dev("waar").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            MikaImker m = baas(sl);
            return zeg(c, "Raatzaal " + pos(ImkerVerhaal.zaal()) + ", luik " + pos(ImkerVerhaal.luik()) + (m == null ? ". Hij slaapt."
                    : ". Hij is wakker: fase " + m.fase() + ", " + (int) m.vads() + "/" + (int) m.maxVads() + " Vads, korf wijst " + m.richting() * 60
                            + " graden, grauw " + m.grauw * 100 / MikaImker.GRAUW_VOL + "%."));
        }));
        b.then(dev("tp").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            naar(p, Vadsrijk.level(p.level().getServer()), Vec3.atBottomCenterOf(ImkerVerhaal.luik()).add(1.5, 0, 0));
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }
}
