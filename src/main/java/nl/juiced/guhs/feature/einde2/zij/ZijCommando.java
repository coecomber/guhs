package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.mikas.E2MikasFeature;
import nl.juiced.guhs.feature.einde2.verhaal.E2VerhaalDev;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 zij (CONTRACT_E2_B 8): the dev commands of the slice (a dev run only, plain literals):
 * <ul>
 *   <li>{@code /guhs einde2 zij ga <lijn>.<stap>}: Opper-Mika beaten, the four elements switched on, the line at that
 *       step (what the step needs in your pockets), and you are taken to the spot of that step;</li>
 *   <li>{@code /guhs einde2 zij ga race|haven|krachten}: to the ring race's start / the ferry / only the four elements;</li>
 *   <li>{@code /guhs einde2 zij scene <roku|zegen|mew|opkomst|gedicht>}: forget the scene and play it (where it belongs);</li>
 *   <li>{@code /guhs einde2 zij wis}: everything of this slice forgotten (lines, scenes, books, records, elements).</li>
 * </ul>
 */
public final class ZijCommando {
    /** Per line and step: the marker the player is taken to (a vesting step: null, they find one themselves). */
    static final Map<String, String[]> PLEK = Map.of(
            ZijLijnen.OMASHGUH, new String[]{"bumi", "proef_1_start", "proef_2_start", "proef_3_start", "glijbaan_start", "bumi"},
            ZijLijnen.LUCHTTEMPEL, new String[]{"luchtnon", "windfluit_1", "aankomst_speler", "luchtnon", "luchtnon"},
            ZijLijnen.VUURTEMPEL, new String[]{"shyu", "vuurwijze_5", "vuurwijze_5", "roku", "shyu"},
            ZijLijnen.KYOSHI, new String[]{"krijgster_leider", "dans_speler", "schminktafel", "fan_guh", "krijgster_leider"},
            ZijLijnen.BIBLIOTHEEK, new String[]{"kennis_balie", "kennis_balie", "vosguh_3", "professor", "kennis_balie"},
            ZijLijnen.LEEUWSCHILDPAD, new String[]{"aankomst_speler", "zegen_plek", "zegen_plek", "zegen_plek"},
            ZijLijnen.MIKA_KOK, new String[]{null, null, null, null, "mika_kok", "mika_kok"},
            ZijLijnen.VETBOOTJE, new String[]{null, "scheepsbouwer", "scheepsbouwer", "scheepsbouwer"});

    private ZijCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        if (net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("zij")
                        .then(Commands.literal("ga").then(Commands.argument("stap", StringArgumentType.string())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(stappen(), b))
                                .executes(c -> ga(c.getSource(), StringArgumentType.getString(c, "stap")))))
                        .then(Commands.literal("scene").then(Commands.argument("naam", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("roku", "zegen", "mew", "opkomst", "gedicht"), b))
                                .executes(c -> scene(c.getSource(), StringArgumentType.getString(c, "naam")))))
                        .then(Commands.literal("wis").executes(c -> wis(c.getSource()))))));
    }

    /** Every step name of the slice. */
    static List<String> stappen() {
        List<String> uit = new ArrayList<>();
        for (String lijn : ZijLijnen.ALLE) {
            int n = ZijLijnen.van(lijn).stappen();
            for (int i = 0; i <= n; i++) {
                uit.add(lijn + "." + i);
            }
        }
        uit.add("race");
        uit.add("haven");
        uit.add("krachten");
        return uit;
    }

    private static int ga(CommandSourceStack source, String stap) {
        ServerPlayer p = source.getPlayer();
        if (p == null) {
            return 0;
        }
        for (Einde2Ids.Element e : Einde2Ids.Element.values()) {
            ZijKrachten.zetDev(p, e, true);
        }
        if (stap.equals("krachten")) {
            source.sendSuccess(() -> Component.literal("The four lasting powers are yours (dev): tap your bending key, hold it for the wheel"), false);
            return 1;
        }
        if (stap.equals("race")) {
            return naar(source, p, Einde2Ids.GUHVLEUGEL_RINGENRACE, "racemeester") ? 1 : 0;
        }
        if (stap.equals("haven")) {
            return naar(source, p, Einde2Ids.LEEGTEHAVEN, "veerman") ? 1 : 0;
        }
        int punt = stap.lastIndexOf('.');
        String lijn = punt < 0 ? stap : stap.substring(0, punt);
        int i;
        try {
            i = punt < 0 ? 0 : Integer.parseInt(stap.substring(punt + 1));
        } catch (NumberFormatException ex) {
            source.sendFailure(Component.literal("Not a step: " + stap));
            return 0;
        }
        if (ZijLijnen.van(lijn) == null) {
            source.sendFailure(Component.literal("Not a line of zij: " + lijn));
            return 0;
        }
        E2VerhaalDev.zetTot(p, lijn, i);
        benodigd(p, lijn, i);
        String[] plekken = PLEK.get(lijn);
        String marker = plekken == null || i >= plekken.length ? null : plekken[i];
        if (marker == null) {
            source.sendSuccess(() -> Component.literal(lijn + " at step " + i + ": go to a Mika-vesting (/locate structure guhs:mika_vesting)"), false);
            return 1;
        }
        String structuur = lijn.equals(ZijLijnen.MIKA_KOK) || lijn.equals(ZijLijnen.VETBOOTJE) ? Einde2Ids.LEEGTEHAVEN : ZijLijnen.structuur(lijn);
        if (naar(source, p, structuur, marker)) {
            source.sendSuccess(() -> Component.literal(lijn + " is now at step " + i), false);
            return 1;
        }
        return 0;
    }

    /** What a step needs in the pockets. */
    private static void benodigd(ServerPlayer p, String lijn, int stap) {
        if (lijn.equals(ZijLijnen.MIKA_KOK) && stap == 1) {
            for (Object[] r : MikaKok.INGREDIENTEN) {
                Zij.geef(p, new ItemStack(MikaKok.item((String) r[0]), (Integer) r[1]));
            }
        } else if (lijn.equals(ZijLijnen.BIBLIOTHEEK) && stap == 1) {
            Zij.geef(p, new ItemStack(Items.COMPASS), new ItemStack(Items.CLOCK), new ItemStack(Items.MAP));
        } else if (lijn.equals(ZijLijnen.LEEUWSCHILDPAD) && stap == 2) {
            Zij.geef(p, new ItemStack(ModItems.KAAS_KNABBELS.get(), 4));
        } else if (lijn.equals(ZijLijnen.VETBOOTJE) && stap == 1) {
            Zij.geef(p, new ItemStack(Items.OAK_PLANKS, 24), new ItemStack(Items.WHITE_WOOL, 8), new ItemStack(ModItems.MIKA_VET.get(), 4),
                    new ItemStack(E2MikasFeature.FLITSKRUIMEL.get(), 2));
        }
    }

    /** Takes the player to a marker of the guaranteed copy of a place (in the Guheinde). */
    private static boolean naar(CommandSourceStack source, ServerPlayer p, String structuur, String marker) {
        ServerLevel guheinde = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (guheinde == null || Plekken.start(guheinde, structuur) == null) {
            source.sendFailure(Component.literal("The guaranteed " + structuur + " is not in this world"));
            return false;
        }
        BlockPos pos = Plekken.wereld(guheinde, structuur, marker);
        if (pos == null) {
            source.sendFailure(Component.literal(structuur + " has no marker " + marker));
            return false;
        }
        p.teleportTo(guheinde, pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5, java.util.Set.of(), 180f, 0f, true);
        return true;
    }

    private static int scene(CommandSourceStack source, String naam) {
        ServerPlayer p = source.getPlayer();
        if (p == null) {
            return 0;
        }
        ServerLevel guheinde = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        Cutscene s;
        BlockPos anker;
        switch (naam) {
            case "roku" -> {
                s = ZijScenes.ROKU;
                anker = guheinde == null ? null : Plekken.wereld(guheinde, Einde2Ids.VUURTEMPEL_ROKU, "roku_beeld");
            }
            case "zegen" -> {
                s = ZijScenes.ZEGEN;
                anker = guheinde == null ? null : Plekken.wereld(guheinde, Einde2Ids.LEEUWSCHILDPAD_EILAND, "zegen_plek");
            }
            case "mew" -> {
                s = ZijScenes.MEW;
                anker = guheinde == null ? null : Plekken.wereld(guheinde, Einde2Ids.BIBLIOTHEEK_WAN_SHI_GUH, "wan_shi_guh");
            }
            case "opkomst" -> {
                s = ZijScenes.OPKOMST;
                anker = guheinde == null ? null : GuheindeGevecht.of(guheinde).center();
            }
            case "gedicht" -> {
                Cutscenes.vergeet(p, ZijScenes.GEDICHT.id());
                return ZijScenes.speelGedicht(p) ? 1 : 0;
            }
            default -> {
                source.sendFailure(Component.literal("No scene " + naam));
                return 0;
            }
        }
        if (anker == null || guheinde == null) {
            source.sendFailure(Component.literal("The place of the scene " + naam + " is not known in this world"));
            return 0;
        }
        if (p.level() != guheinde || p.distanceToSqr(Vec3.atCenterOf(anker)) > 64 * 64) {
            p.teleportTo(guheinde, anker.getX() + 0.5, anker.getY(), anker.getZ() + 6.5, java.util.Set.of(), 180f, 0f, true);
        }
        Cutscenes.vergeet(p, s.id());
        Cutscenes.speel(p, s, anker, Rotation.NONE, null);
        return 1;
    }

    private static int wis(CommandSourceStack source) {
        ServerPlayer p = source.getPlayer();
        if (p == null) {
            return 0;
        }
        for (String lijn : ZijLijnen.ALLE) {
            E2VerhaalDev.wis(p, lijn);
        }
        for (Cutscene s : List.of(ZijScenes.ROKU, ZijScenes.ZEGEN, ZijScenes.MEW, ZijScenes.OPKOMST, ZijScenes.GEDICHT)) {
            Cutscenes.vergeet(p, s.id());
        }
        for (String k : List.of(ZijKrachten.DEV_ELEMENTEN, ZijBoeken.GEKREGEN, Ringenrace.BESTE, Bibliotheek.DAG, Leeuwschildpad.DAG, MikaKok.DAG,
                Leegtehaven.HANDEL)) {
            GuhQuests.saved(p).remove(k);
        }
        ZijEvents.onLogoutVergeet(p);
        source.sendSuccess(() -> Component.literal("Everything of zij is forgotten for you"), false);
        return 1;
    }
}
