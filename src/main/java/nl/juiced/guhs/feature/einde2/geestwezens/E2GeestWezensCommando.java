package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The dev command of the slice geest_modellen (CONTRACT_E2_B 8; a dev run only, plain literals). The slice has no story lines
 * and no scenes of its own, so its "steps" are the things it gives the others, each put in front of you to try at once:
 * <ul>
 *   <li>{@code /guhs einde2 geestwezens ga fang}: Fang flies you a loop of about 25 seconds and lands you back here
 *       ({@link Fang#vlieg}; sneaking does not get you off);</li>
 *   <li>{@code ga libel}: you get the libelkonijnguh pet ({@link Libelkonijnguh#geef}; sneak-click it: stay / come along);</li>
 *   <li>{@code ga vosje}: a kennisvosje (and a golden one) that leads you to a spot 14 blocks ahead;</li>
 *   <li>{@code ga reiger}: the Oude Reigerguh asleep, 10 blocks ahead; {@code anim ontwaak} wakes it, {@code anim spreek} talks;</li>
 *   <li>{@code ga wan}: Wan-guh 28 blocks ahead: walk to him and he fades;</li>
 *   <li>{@code ga reus}: the four giant silhouettes, 90 to 150 blocks away (walk towards one: it fades);</li>
 *   <li>{@code ga masker}: the twelve watching masks in an arc: walk past them;</li>
 *   <li>{@code ga echo}: the eleven spirit characters (the echo-masters, the old members, Guru Pathik, three earlier
 *       Guhvatars) in an arc;</li>
 * </ul>
 * {@code anim <naam>} plays an animation on every creature or spirit character of the slice within 12 blocks;
 * {@code wis} removes everything these commands put down and takes away your pet. Entity tag {@value #TAG}.
 */
final class E2GeestWezensCommando {
    static final String TAG = "guhs_einde2_geestmodellen_dev";
    static final List<String> STAPPEN = List.of("fang", "libel", "vosje", "reiger", "wan", "reus", "masker", "echo");

    private E2GeestWezensCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").then(Commands.literal("geestwezens")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("ga").then(Commands.argument("stap", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(STAPPEN, b))
                        .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "stap")))))
                .then(Commands.literal("anim").then(Commands.argument("naam", StringArgumentType.word())
                        .executes(c -> anim(c.getSource(), StringArgumentType.getString(c, "naam")))))
                .then(Commands.literal("wis").executes(c -> wis(c.getSource()))))));
    }

    private static Vec3 voor(ServerPlayer p, double afstand) {
        Vec3 kijk = p.getLookAngle().multiply(1, 0, 1).normalize();
        return p.position().add(kijk.scale(afstand));
    }

    private static <T extends GeestWezen> T zet(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, Vec3 plek, float yaw) {
        T t = type.create(level, EntitySpawnReason.COMMAND);
        if (t != null) {
            t.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
            t.setYHeadRot(yaw);
            t.addTag(TAG);
            level.addFreshEntity(t);
        }
        return t;
    }

    private static int ga(CommandSourceStack source, String stap) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        float naarMij = p.getYRot() + 180f;
        String uit;
        switch (stap) {
            case "fang" -> {
                List<Vec3> pad = new ArrayList<>();
                Vec3 m = p.position();
                Vec3 kijk = p.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 zij = new Vec3(-kijk.z, 0, kijk.x);
                Vec3 midden = m.add(kijk.scale(30));
                for (int i = 1; i < 12; i++) {                      // (a big circle 30 blocks round a point ahead, up to 16 high)
                    double a = i / 12.0 * Math.PI * 2;
                    pad.add(midden.add(kijk.scale(-Math.cos(a) * 30)).add(zij.scale(Math.sin(a) * 30)).add(0, 16 * Math.sin(a / 2), 0));
                }
                pad.add(m.add(0, 0.2, 0));
                boolean ok = Fang.vlieg(p, pad, sp -> sp.sendSystemMessage(Component.literal("Fang: geland (daarna liep)")));
                uit = ok ? "Fang flies a loop and lands you here" : "Fang could not start";
            }
            case "libel" -> {
                Entity pet = Libelkonijnguh.geef(p);
                uit = pet != null ? "your libelkonijnguh (sneak-click: stay / come along)" : "no pet";
            }
            case "vosje" -> {
                Vec3 doel = voor(p, 14);
                for (int s = 0; s < 2; s++) {
                    KennisvosjeEntity vos = zet(level, E2GeestWezensFeature.KENNISVOSJE.get(), voor(p, 3).add(s * 1.5 - 0.75, 0, 0), naarMij);
                    if (vos != null) {
                        vos.zetSoort(s);
                        vos.zetDoel(BlockPos.containing(doel).offset(s * 2, 0, 0));
                    }
                }
                uit = "two kennisvosjes lead you 14 blocks ahead";
            }
            case "reiger" -> {
                OudeReigerguhEntity r = zet(level, E2GeestWezensFeature.OUDE_REIGERGUH.get(), voor(p, 10), naarMij);
                if (r != null) {
                    r.zetStil(true);
                }
                uit = "the Oude Reigerguh sleeps 10 blocks ahead (anim ontwaak / spreek / slaap)";
            }
            case "wan" -> {
                zet(level, E2GeestWezensFeature.WAN_GUH.get(), voor(p, 28), naarMij);
                uit = "Wan-guh stands 28 blocks ahead: walk to him";
            }
            case "reus" -> {
                for (int s = 0; s < 4; s++) {
                    double a = Math.toRadians(p.getYRot() - 45 + s * 30);
                    Vec3 plek = p.position().add(-Math.sin(a) * (90 + s * 20), 10, Math.cos(a) * (90 + s * 20));
                    GeestreusEntity g = zet(level, E2GeestWezensFeature.GEESTREUS.get(), plek, naarMij);
                    if (g != null) {
                        g.zetSoort(s);
                    }
                }
                uit = "four giants far ahead";
            }
            case "masker" -> {
                for (int s = 0; s < 12; s++) {
                    double a = Math.toRadians(p.getYRot() - 66 + s * 12);
                    Vec3 plek = p.position().add(-Math.sin(a) * 7, 1.5 + (s % 2) * 0.8, Math.cos(a) * 7);
                    float yaw = (float) Math.toDegrees(a) + 180f;
                    KijkmaskerEntity m = zet(level, E2GeestWezensFeature.KIJKMASKER.get(), plek, yaw);
                    if (m != null) {
                        m.zetSoort(s);
                        m.zetStil(true);
                    }
                }
                uit = "twelve masks round you";
            }
            case "echo" -> {
                List<GuhNpcEntity.Kind> kinds = GeestKinds.kinds().subList(2, GeestKinds.kinds().size());
                for (int i = 0; i < kinds.size(); i++) {
                    double a = Math.toRadians(p.getYRot() - 75 + i * 15);
                    Vec3 plek = p.position().add(-Math.sin(a) * 6, 0, Math.cos(a) * 6);
                    GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.COMMAND);
                    if (npc != null) {
                        npc.setKind(kinds.get(i));
                        float yaw = (float) Math.toDegrees(a) + 180f;
                        npc.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
                        npc.setYHeadRot(yaw);
                        npc.addTag(TAG);
                        level.addFreshEntity(npc);
                    }
                }
                uit = kinds.size() + " spirit characters round you";
            }
            default -> {
                source.sendFailure(Component.literal("unknown step; one of " + STAPPEN));
                return 0;
            }
        }
        String bericht = "einde2 geestwezens " + stap + ": " + uit;
        source.sendSuccess(() -> Component.literal(bericht), false);
        return 1;
    }

    private static int anim(CommandSourceStack source, String naam) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int n = 0;
        for (Entity e : source.getLevel().getEntities(p, new AABB(p.blockPosition()).inflate(12))) {
            if (e instanceof OudeReigerguhEntity r && naam.equals("ontwaak")) {
                r.ontwaak();
                n++;
            } else if (e instanceof GeestWezen g && g.wezens().heeft(naam)) {
                g.zetAnimatie(naam);
                n++;
            } else if (e instanceof GuhNpcEntity npc && GeestKinds.rij(npc.getKind()) != null && GeestKinds.duur(npc.getKind(), naam) >= 0) {
                npc.speel(naam);
                n++;
            }
        }
        int m = n;
        source.sendSuccess(() -> Component.literal("einde2 geestwezens anim " + naam + ": " + m), false);
        return n;
    }

    private static int wis(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        int n = 0;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e.entityTags().contains(TAG)) {
                    e.discard();
                    n++;
                }
            }
        }
        Libelkonijnguh.wis(p);
        int m = n;
        source.sendSuccess(() -> Component.literal("einde2 geestwezens wis: " + m + " removed, pet gone"), false);
        return n;
    }
}
