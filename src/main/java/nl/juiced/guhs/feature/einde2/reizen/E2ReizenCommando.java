package nl.juiced.guhs.feature.einde2.reizen;

import java.util.List;
import java.util.Set;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.appa.Momo;
import nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.einde2.zwevers.LeegtewalvisguhEntity;
import nl.juiced.guhs.feature.einde2.zwevers.Walviszicht;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B (CONTRACT_E2_B 8), slice reizen: {@code /guhs einde2 reizen ...} (a dev run only). Every step sets the
 * state, puts you where it plays and starts what it starts:
 * <ul>
 *   <li>{@code ga vlucht_start|vlucht_h1_h2|vlucht_h2_h3|vlucht_h3_h4}: the story so far, your Appa and Momo, you at the
 *       structure you leave from, and the flight with its team chat (and Zuka's chase on h1_h2 / h2_h3);</li>
 *   <li>{@code ga team_<structuur>}: the whole team (finale done), you at that structure's landing spot: the campfire;</li>
 *   <li>{@code ga fluit}: Appa under you in the Guheinde and the whistle list;</li>
 *   <li>{@code ga walvis_0..walvis_4}: the whale questline at that step (0 the captain; 1 on Appa at the waiting spot;
 *       2 the proof at the captain; 3 a whale to feed beside you; 4 your own whale under your feet);</li>
 *   <li>{@code scene walvis|walvis_kroeg}: forget and play; {@code wis}: everything of this slice forgotten;</li>
 *   <li>{@code land} (the flight lands now), {@code gast <speler>} (that player is your guest here).</li>
 * </ul>
 */
public final class E2ReizenCommando {
    static final List<String> STAPPEN = List.of("vlucht_start", "vlucht_h1_h2", "vlucht_h2_h3", "vlucht_h3_h4", "team_zuidelijke_luchttempel",
            "team_noordelijke_waterstam", "team_ba_sing_guh", "team_vuurnatie_hoofdstad", "fluit", "fluit_alles", "platform", "walvis_0", "walvis_1", "walvis_2", "walvis_3",
            "walvis_4");

    private E2ReizenCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("reizen")
                        .then(Commands.literal("ga").then(Commands.argument("stap", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(STAPPEN, b))
                                .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "stap")))))
                        .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("walvis", "walvis_kroeg"), b))
                                .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                        .then(Commands.literal("wis").executes(c -> wis(c.getSource())))
                        .then(Commands.literal("land").executes(c -> land(c.getSource())))
                        .then(Commands.literal("gast").then(Commands.argument("speler", EntityArgument.player())
                                .executes(c -> gast(c.getSource(), EntityArgument.getPlayer(c, "speler"))))))));
    }

    private static void zeg(CommandSourceStack src, String tekst) {
        src.sendSuccess(() -> Component.literal(tekst), false);
    }

    private static int ga(CommandSourceStack src, String stap) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel guheinde = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (guheinde == null) {
            zeg(src, "Geen Guheinde in deze wereld.");
            return 0;
        }
        if (stap.startsWith("vlucht_")) {
            return vlucht(src, p, guheinde, stap.substring("vlucht_".length()));
        }
        if (stap.startsWith("team_")) {
            String s = stap.substring("team_".length());
            E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, 0);
            E2VerhaalDev.klaar(p, E2Verhaal.FINALE);
            E2VerhaalDev.vlag(p, E2Verhaal.VUUR, E2Verhaal.V_ZUKA_TEAM, true);
            if (!naarAankomst(p, guheinde, s)) {
                zeg(src, "De plek van " + s + " is nog niet bekend (laat de wereld eerst verder laden).");
                return 0;
            }
            zeg(src, "Hele team: kijk bij het kampvuur (team_*). Klik ze voor hun praatjes.");
            return 1;
        }
        if (stap.equals("platform")) {
            return platform(src, p);
        }
        if (stap.equals("fluit_alles")) {
            E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, 0);    // (fix4-appa: the story done, every einde2 building on the list)
            E2VerhaalDev.klaar(p, E2Verhaal.FINALE);
            if (p.level() != guheinde) {
                naarAankomst(p, guheinde, Einde2Ids.LEEGTEHAVEN);
            }
            opAppa(p);
            Fluitlijst.open(p);
            return 1;
        }
        if (stap.equals("fluit")) {
            E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, 0);
            if (p.level() != guheinde) {
                naarAankomst(p, guheinde, Einde2Ids.LEEGTEHAVEN);
            }
            opAppa(p);
            Fluitlijst.open(p);
            return 1;
        }
        if (stap.startsWith("walvis_")) {
            return walvis(src, p, guheinde, Integer.parseInt(stap.substring("walvis_".length())));
        }
        zeg(src, "Onbekende stap: " + stap + " (" + String.join(", ", STAPPEN) + ")");
        return 0;
    }

    /** The story so far, Appa and Momo, the structure you leave from, and the flight. */
    private static int vlucht(CommandSourceStack src, ServerPlayer p, ServerLevel guheinde, String welke) {
        E2Reizen.Vlucht soort;
        String van, naar;
        switch (welke) {
            case "start" -> {
                soort = E2Reizen.Vlucht.START;
                E2VerhaalDev.zetTot(p, E2Verhaal.LUCHT, 1);
                van = null;
                naar = Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL;
            }
            case "h1_h2" -> {
                soort = E2Reizen.Vlucht.H1_H2;
                E2VerhaalDev.zetTot(p, E2Verhaal.WATER, 0);
                van = Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL;
                naar = Einde2Ids.NOORDELIJKE_WATERSTAM;
            }
            case "h2_h3" -> {
                soort = E2Reizen.Vlucht.H2_H3;
                E2VerhaalDev.zetTot(p, E2Verhaal.AARDE, 0);
                van = Einde2Ids.NOORDELIJKE_WATERSTAM;
                naar = Einde2Ids.BA_SING_GUH;
            }
            case "h3_h4" -> {
                soort = E2Reizen.Vlucht.H3_H4;
                E2VerhaalDev.zetTot(p, E2Verhaal.VUUR, 0);
                van = Einde2Ids.BA_SING_GUH;
                naar = Einde2Ids.VUURNATIE_HOOFDSTAD;
            }
            default -> {
                zeg(src, "vlucht_start | vlucht_h1_h2 | vlucht_h2_h3 | vlucht_h3_h4");
                return 0;
            }
        }
        if (van == null) {
            int y = guheinde.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GuheindeGevecht.ARRIVAL_X, GuheindeGevecht.ARRIVAL_Z + 6);
            p.teleportTo(guheinde, GuheindeGevecht.ARRIVAL_X + 0.5, Math.max(y, 60) + 1, GuheindeGevecht.ARRIVAL_Z + 6.5, Set.of(), p.getYRot(), 0, true);
        } else if (!naarAankomst(p, guheinde, van)) {
            zeg(src, "De plek van " + van + " is nog niet bekend.");
            return 0;
        }
        Appa.houdWeg(p, false);
        Appa.geef(p);
        Momo.geef(p);
        opAppa(p);
        boolean ok = E2Reizen.vlieg(p, naar, soort, speler -> speler.sendSystemMessage(Component.literal("[dev] daarna: geland bij " + naar)));
        zeg(src, ok ? "Vlucht " + soort + " naar " + naar + " gestart." : "Vlucht kon niet starten (plek onbekend?).");
        return ok ? 1 : 0;
    }

    /**
     * fix4-appa: the story done, and a complete Appa-platform (5x5 smooth stone with the platform block in the middle, 3
     * blocks of air above) right under the player, in this dimension; the player stands on its edge.
     */
    private static int platform(CommandSourceStack src, ServerPlayer p) {
        E2VerhaalDev.zetTot(p, E2Verhaal.FINALE, 0);
        E2VerhaalDev.klaar(p, E2Verhaal.FINALE);
        Appa.houdWeg(p, false);
        Appa.geef(p);
        ServerLevel level = p.level();
        BlockPos midden = p.blockPosition().below();
        for (int dx = -AppaPlatform.HALF; dx <= AppaPlatform.HALF; dx++) {
            for (int dz = -AppaPlatform.HALF; dz <= AppaPlatform.HALF; dz++) {
                BlockPos q = midden.offset(dx, 0, dz);
                level.setBlockAndUpdate(q, dx == 0 && dz == 0 ? E2ReizenFeature.APPA_PLATFORM.get().defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                for (int dy = 1; dy <= AppaPlatform.LUCHT; dy++) {
                    level.setBlockAndUpdate(q.above(dy), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
        }
        AppaPlatform.zet(p, level.dimension(), midden);
        p.teleportTo(midden.getX() + 0.5, midden.getY() + 1, midden.getZ() + 0.5 - AppaPlatform.HALF);
        AppaPlatform.meld(p, level, midden, AppaPlatform.controleer(level, midden));
        zeg(src, "Appa-platform onder je in " + level.dimension().identifier() + ". Loop meer dan " + (int) AppaPlatform.BEREIK
                + " blokken weg en fluit (hij komt niet), kom terug en fluit (hij landt erop). Haal een vloerblok weg en klik het platform: wat mist er?");
        return 1;
    }

    private static int walvis(CommandSourceStack src, ServerPlayer p, ServerLevel guheinde, int stap) {
        Leegtewalvis.LIJN.wis(p);
        GuhQuests.saved(p).remove(Leegtewalvis.WACHT);
        if (stap == 0) {
            Walviszicht.zet(p, false);
            naarKapitein(src, p, guheinde);
            return 1;
        }
        if (!naarKapitein(src, p, guheinde)) {
            return 0;
        }
        Entity kapitein = kapitein(p);
        Leegtewalvis.neemAan(p, kapitein);
        if (stap == 1) {
            BlockPos plek = Leegtewalvis.wachtplek(p);
            if (plek != null) {
                p.teleportTo(guheinde, plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, Set.of(), p.getYRot(), 0, true);
                opAppa(p);
                zeg(src, "Wacht hier boven de leegte (10 s). Het lied komt vanzelf.");
            }
            return 1;
        }
        Leegtewalvis.LIJN.zet(p, 2);
        if (stap == 2) {
            Minigames.give(p, new ItemStack(E2ZweversFeature.LEEGTEPAREL.get()));
            zeg(src, "Je hebt een leegteparel: klik de kapitein.");
            return 1;
        }
        Leegtewalvis.LIJN.zet(p, 3);
        Minigames.give(p, new ItemStack(E2ReizenFeature.WALVISHOORN.get()));
        BlockPos plek = Leegtewalvis.wachtplek(p);
        if (plek != null) {
            p.teleportTo(guheinde, plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, Set.of(), p.getYRot(), 0, true);
            opAppa(p);
        }
        LeegtewalvisguhEntity w = Leegtewalvis.stuurWalvis(p);
        if (stap == 3) {
            Minigames.give(p, new ItemStack(E2BiomesFeature.KNABBELKOORVRUCHT.get(), Leegtewalvis.VOER));
            zeg(src, w != null ? "Een walvis zwemt naast je: voer hem " + Leegtewalvis.VOER + " knabbelkoorvruchten." : "Geen walvis kunnen maken hier.");
            return 1;
        }
        if (w == null) {
            zeg(src, "Geen walvis kunnen maken hier (te dicht bij een eiland?).");
            return 0;
        }
        Walvis.maakEigen(p, w);
        Leegtewalvis.LIJN.zet(p, Leegtewalvis.STAPPEN);
        for (String deel : Walvis.DELEN) {
            Walvis.zetDeel(p, deel, true);
        }
        Appa.stop(p);
        p.stopRiding();
        Vec3 rug = new Vec3(w.getX(), w.rugY() + 0.5, w.getZ());
        p.teleportTo(guheinde, rug.x, rug.y, rug.z, Set.of(), p.getYRot(), 0, true);
        zeg(src, "Je eigen walvis, helemaal ingericht. Hoorn in de hand op zijn rug = sturen; sluip + hoorn = inrichting.");
        return 1;
    }

    private static boolean naarKapitein(CommandSourceStack src, ServerPlayer p, ServerLevel guheinde) {
        if (!Plekken.bekend(guheinde, Einde2Ids.LEEGTEHAVEN)) {
            zeg(src, "De Leegtehaven is nog niet bekend.");
            return false;
        }
        BlockPos k = Plekken.wereld(guheinde, Einde2Ids.LEEGTEHAVEN, "kapitein");
        if (k == null) {
            return false;
        }
        p.stopRiding();
        p.teleportTo(guheinde, k.getX() - 1.5, k.getY(), k.getZ() + 0.5, Set.of(), -90, 0, true);
        return true;
    }

    /** The captain of the harbour the player stands in (null: not loaded yet). */
    private static Entity kapitein(ServerPlayer p) {
        for (Entity e : p.level().getEntities(p, p.getBoundingBox().inflate(12))) {
            if (e instanceof nl.juiced.guhs.entity.GuhNpcEntity npc && npc.getKind() == nl.juiced.guhs.entity.GuhNpcEntity.Kind.KAPITEINGUH) {
                return npc;
            }
        }
        return null;
    }

    private static boolean naarAankomst(ServerPlayer p, ServerLevel guheinde, String s) {
        if (!Plekken.bekend(guheinde, s)) {
            return false;
        }
        Vec3 a = Plekken.aankomst(guheinde, s);
        if (a == null) {
            return false;
        }
        p.stopRiding();
        p.teleportTo(guheinde, a.x, a.y + 0.1, a.z, Set.of(), p.getYRot(), 0, true);
        return true;
    }

    /** The player on their own Appa's head. */
    private static void opAppa(ServerPlayer p) {
        Appa.houdWeg(p, false);
        AppaGuhEntity appa = Appa.geef(p);
        if (appa != null && p.getVehicle() != appa) {
            p.stopRiding();
            p.startRiding(appa, true, false);
        }
    }

    private static int scene(CommandSourceStack src, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (naam.equals("walvis")) {
            Cutscenes.vergeet(p, Leegtewalvis.WALVIS.id());
            Walviszicht.zet(p, true);
            Cutscenes.speel(p, Leegtewalvis.WALVIS, p.blockPosition(), Rotation.NONE, null);
            return 1;
        }
        if (naam.equals("walvis_kroeg")) {
            Cutscenes.vergeet(p, Leegtewalvis.KROEG.id());
            Entity k = kapitein(p);
            BlockPos anker = k != null && p.level() instanceof ServerLevel level ? Leegtewalvis.lessenaar(level, k) : p.blockPosition();
            Cutscenes.speel(p, Leegtewalvis.KROEG, anker, Rotation.NONE, null);
            return 1;
        }
        zeg(src, "walvis | walvis_kroeg");
        return 0;
    }

    private static int wis(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Leegtewalvis.LIJN.wis(p);
        LeegtewalvisguhEntity w = Walvis.geladen(p);
        if (w != null) {
            w.discard();
        }
        CompoundTagWis.wis(p);
        Walviszicht.zet(p, false);
        Cutscenes.vergeet(p, Leegtewalvis.WALVIS.id());
        Cutscenes.vergeet(p, Leegtewalvis.KROEG.id());
        E2Reizen.weg(p);
        E2Team.weg(p, null);
        zeg(src, "Alles van reizen vergeten (walvislijn, eigen walvis, wachtende vlucht, gasten).");
        return 1;
    }

    private static int land(CommandSourceStack src) throws CommandSyntaxException {
        boolean ok = E2Reizen.landNu(src.getPlayerOrException());
        zeg(src, ok ? "Geland." : "Geen vlucht.");
        return ok ? 1 : 0;
    }

    private static int gast(CommandSourceStack src, ServerPlayer gast) throws CommandSyntaxException {
        ServerPlayer host = src.getPlayerOrException();
        E2Reizen.maakGast(gast, host, E2Reizen.structuurBij(host));
        zeg(src, gast.getName().getString() + " is nu jouw gast.");
        return 1;
    }

    /** The player keys of this slice that {@code wis} forgets. */
    private static final class CompoundTagWis {
        static void wis(ServerPlayer p) {
            var saved = GuhQuests.saved(p);
            saved.remove(Walvis.KEY);
            saved.remove(Leegtewalvis.WACHT);
            saved.remove(E2Reizen.REIS_KEY);
        }
    }
}
