package nl.juiced.guhs.feature.vadsrijk.tmodels;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3WezensRegistry;

/**
 * Night round T, slice T-MODELS: the model showroom (look at every mob without fighting it) and the GeckoLib look of kern's
 * slimes ({@link KlodderGeo}). Commands under {@code /guhs vadsrijk models}:
 * <ul>
 *   <li>{@code toon <zone1|zone2|zone3|critters|alles>}: a grid of every creature of that group in front of you, frozen and
 *   invulnerable (tag {@value #TAG});</li>
 *   <li>{@code plof}: cheer up every showroom creature (the pink, happy texture); {@code weg}: remove them all;</li>
 *   <li>{@code reus [vecht]}: a Grauwe Klodderreus (frozen, or a real lv-3 fight without the arena);
 *   {@code reus anim <spuug|brul|aanloop>}: play an action animation on the giants within 32 blocks.</li>
 * </ul>
 */
public final class TModels {
    public static final String TAG = "guhs_tm_toon";

    private TModels() {
    }

    public static void register(IEventBus modBus) {
        KlodderGeo.init();
        NeoForge.EVENT_BUS.addListener(TModels::commando);
    }

    private static Map<String, List<Supplier<? extends EntityType<?>>>> groepen() {
        Map<String, List<Supplier<? extends EntityType<?>>>> g = new LinkedHashMap<>();
        List<Supplier<? extends EntityType<?>>> z1 = new ArrayList<>(WezensRegistry.SOORTEN.values());
        z1.add(KernRegistry.PROEFKLODDER);
        List<Supplier<? extends EntityType<?>>> z2 = new ArrayList<>(Z2WezensRegistry.SOORTEN.values());
        List<Supplier<? extends EntityType<?>>> z3 = new ArrayList<>(Z3WezensRegistry.SOORTEN.values());
        z3.addAll(Z3WezensRegistry.ADDS.values());
        List<Supplier<? extends EntityType<?>>> cr = new ArrayList<>(Z2WezensRegistry.CRITTERS.values());
        cr.addAll(Z3WezensRegistry.CRITTERS.values());
        g.put("zone1", z1);
        g.put("zone2", z2);
        g.put("zone3", z3);
        g.put("critters", cr);
        List<Supplier<? extends EntityType<?>>> alles = new ArrayList<>();
        g.values().forEach(alles::addAll);
        g.put("alles", alles);
        return g;
    }

    private static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("models").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("toon").then(Commands.argument("groep", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(groepen().keySet(), b))
                        .executes(TModels::toon)))
                .then(Commands.literal("weg").executes(TModels::weg))
                .then(Commands.literal("plof").executes(TModels::plof))
                .then(Commands.literal("reus").executes(c -> reus(c, false))
                        .then(Commands.literal("vecht").executes(c -> reus(c, true)))
                        .then(Commands.literal("anim").then(Commands.argument("actie", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(KlodderGeo.ACTIES, b))
                                .executes(TModels::anim))))));
    }

    private static int toon(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String groep = StringArgumentType.getString(c, "groep");
        List<Supplier<? extends EntityType<?>>> types = groepen().get(groep);
        if (types == null) {
            c.getSource().sendFailure(Component.translatable("vadsrijk.tmodels.cmd.onbekend", groep));
            return 0;
        }
        ServerLevel sl = p.level();
        Vec3 vooruit = p.getLookAngle().multiply(1, 0, 1);
        vooruit = vooruit.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : vooruit.normalize();
        Vec3 opzij = new Vec3(-vooruit.z, 0, vooruit.x);
        int perRij = 6, n = 0;
        for (Supplier<? extends EntityType<?>> s : types) {
            Entity e = s.get().create(sl, EntitySpawnReason.COMMAND);
            if (e == null) {
                continue;
            }
            int rij = n / perRij, kol = n % perRij;
            Vec3 plek = p.position().add(vooruit.scale(5 + rij * 4.5)).add(opzij.scale((kol - (perRij - 1) / 2.0) * 4));
            double y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(plek.x),
                    (int) Math.floor(plek.z));
            float yaw = (float) Math.toDegrees(Math.atan2(vooruit.x, -vooruit.z));
            e.snapTo(plek.x, Math.max(y, p.getY()), plek.z, yaw, 0);
            klaarzetten(e);
            sl.addFreshEntity(e);
            n++;
        }
        int totaal = n;
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.tmodels.cmd.toon", totaal), false);
        return n;
    }

    /** Frozen, invulnerable, kept; a Vadsrijk mob at level 1 without affixes. */
    private static void klaarzetten(Entity e) {
        if (e instanceof VadsrijkMob m) {
            m.stelIn(1, null, false, EnumSet.noneOf(Affix.class));
        }
        if (e instanceof Mob m) {
            m.setNoAi(true);
            m.setPersistenceRequired();
            m.setYHeadRot(e.getYRot());
            m.setYBodyRot(e.getYRot());
        }
        e.setInvulnerable(true);
        e.addTag(TAG);
    }

    private static List<Entity> showroom(ServerLevel sl, ServerPlayer p) {
        return new ArrayList<>(sl.getEntities((Entity) null, p.getBoundingBox().inflate(96), e -> e.getTags().contains(TAG)));
    }

    private static int weg(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<Entity> l = showroom(p.level(), p);
        l.forEach(Entity::discard);
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.tmodels.cmd.weg", l.size()), false);
        return l.size();
    }

    private static int plof(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int n = 0;
        for (Entity e : showroom(p.level(), p)) {
            if (e instanceof VadsrijkMob m && !m.geplofd()) {
                m.verslagen(null);
                n++;
            }
        }
        int totaal = n;
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.tmodels.cmd.plof", totaal), false);
        return n;
    }

    private static int reus(CommandContext<CommandSourceStack> c, boolean vecht) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = p.level();
        Klodderreus k = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.COMMAND);
        if (k == null) {
            return 0;
        }
        Vec3 voor = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(7));
        k.snapTo(voor.x, p.getY(), voor.z, p.getYRot() + 180, 0);
        k.stelIn(3, Element.WATER, false, EnumSet.noneOf(Affix.class));
        if (!vecht) {
            klaarzetten(k);
        }
        sl.addFreshEntity(k);
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.tmodels.cmd.reus",
                Component.translatable(vecht ? "vadsrijk.tmodels.cmd.reus.vecht" : "vadsrijk.tmodels.cmd.reus.stil")), false);
        return 1;
    }

    private static int anim(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String actie = StringArgumentType.getString(c, "actie");
        int n = 0;
        for (Klodderreus k : p.level().getEntitiesOfClass(Klodderreus.class, p.getBoundingBox().inflate(32))) {
            KlodderGeo.speel(k, actie);
            n++;
        }
        int totaal = n;
        c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.tmodels.cmd.anim", actie, totaal), false);
        return n;
    }
}
