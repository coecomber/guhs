package nl.juiced.guhs.feature.safari.lobby;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * What the lobby's guhs do when you talk to them (their NpcRoles, registered with {@link NpcRollen}).
 * Other slices can hook in before a character's own talk ({@link #haak}): S4 hangs its quests on the hoofdranger
 * ({@code "hoofdranger"}); a hook that handles the click returns true.
 */
public final class Rollen {
    /** Who may speak first: (character id, hook); see {@link #haak}. */
    private record Haak(String npc, BiPredicate<GuhNpcEntity, ServerPlayer> haak) {
    }

    private static final List<Haak> HAKEN = new CopyOnWriteArrayList<>();

    /** (Other slices) runs before the character's own talk; true = handled (the own talk is skipped). */
    public static void haak(String npcId, BiPredicate<GuhNpcEntity, ServerPlayer> haak) {
        HAKEN.add(new Haak(npcId, haak));
    }

    /**
     * (S1) the safari camera given at the first visit and by the fotograaf. Default: the first existing item of a few
     * likely ids; S1 can set its own supplier.
     */
    public static volatile Supplier<ItemStack> camera = () -> {
        for (String id : List.of("guhs:safari_camera", "guhs:safaricamera", "guhs:camera")) {
            Identifier rl = Identifier.tryParse(id);
            if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) {
                return new ItemStack(BuiltInRegistries.ITEM.getValue(rl));
            }
        }
        return ItemStack.EMPTY;
    };

    /** (S6) the painting a zone expert hands out with the badge (EMPTY: none); set by SafariKoppelingen. */
    public static volatile java.util.function.BiFunction<net.minecraft.server.MinecraftServer, String, ItemStack> zoneSchilderij =
            (server, zone) -> ItemStack.EMPTY;

    private static boolean haken(String npc, GuhNpcEntity e, ServerPlayer p) {
        for (Haak h : HAKEN) {
            if (h.npc().equals(npc) && h.haak().test(e, p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean heeftCamera(ServerPlayer p) {
        ItemStack c = camera.get();
        if (c.isEmpty()) {
            return true;
        }
        Item item = c.getItem();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    /** Gives the camera unless the player has one (true: given). */
    static boolean geefCamera(ServerPlayer p) {
        ItemStack c = camera.get();
        if (c.isEmpty() || heeftCamera(p)) {
            return false;
        }
        Minigames.give(p, c.copy());
        return true;
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.SAFARI_HOOFDRANGER, HOOFDRANGER);
        NpcRollen.zet(GuhNpcEntity.Kind.SAFARI_FOTOGRAAF, FOTOGRAAF);
        NpcRollen.zet(GuhNpcEntity.Kind.SAFARI_VERZORGER, VERZORGER);
        NpcRollen.zet(GuhNpcEntity.Kind.SAFARI_VERKOPER, VERKOPER);
        NpcRollen.zet(GuhNpcEntity.Kind.SAFARI_LODGEBEHEERDER, LODGE);
        for (String zone : LobbyItems.BADGES.keySet()) {
            NpcRollen.zet(GuhNpcEntity.Kind.valueOf("SAFARI_EXPERT_" + zone.toUpperCase(java.util.Locale.ROOT)), new Expert(zone));
        }
    }

    // --- the hoofdranger: S4's quests (hook), the tip of the day, what you can do here ------------------------------------

    static final NpcRole HOOFDRANGER = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            if (haken("hoofdranger", npc, p)) {
                return;
            }
            Praat.open(p, npc, null, "gui.guhs.safari.ranger.hallo", new Object[0],
                    new Praat.Optie(1, "gui.guhs.safari.ranger.optie.tip"), new Praat.Optie(2, "gui.guhs.safari.ranger.optie.uitleg"),
                    new Praat.Optie(3, "gui.guhs.safari.optie.doei"));
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
            if (optie == 1) {
                Praat.open(p, npc, null, "gui.guhs.safari.ranger.tip", tip(p.level().getServer(), p.getUUID()),
                        new Praat.Optie(3, "gui.guhs.safari.optie.dank"));
            } else if (optie == 2) {
                Praat.open(p, npc, null, "gui.guhs.safari.ranger.uitleg", new Object[0], new Praat.Optie(3, "gui.guhs.safari.optie.dank"));
            }
        }
    };

    /**
     * The ranger tip of the day: where one animal the player has not photographed yet lives (hiders first), the same all
     * day for this player. Args: species, zone, x, z (rounded to 5).
     */
    static Object[] tip(MinecraftServer server, UUID speler) {
        ParkData d = ParkData.get();
        Set<String> dex = SafariSpeler.gefotografeerd(server, speler);
        List<ParkData.Plek> kandidaten = new ArrayList<>();
        List<ParkData.Plek> verstoppers = new ArrayList<>();
        for (ParkData.Plek p : d.plekken) {
            if (!SafariSpeler.telt(p.soort()) || dex.contains(p.soort())) {
                continue;
            }
            kandidaten.add(p);
            ParkData.Soort s = d.soort(p.soort());
            if (s != null && (s.heeft("verstopper") || s.heeft("territorium"))) {
                verstoppers.add(p);
            }
        }
        List<ParkData.Plek> uit = !verstoppers.isEmpty() ? verstoppers : !kandidaten.isEmpty() ? kandidaten : d.plekken;
        if (uit.isEmpty()) {
            return new Object[]{Component.translatable("gui.guhs.safari.ranger.niks"), "", 0, 0};
        }
        int i = Math.floorMod(Long.hashCode(Klok.dag() * 7919 + speler.getLeastSignificantBits()), uit.size());
        ParkData.Plek p = uit.get(i);
        return new Object[]{Component.translatable("entity.guhs.safari." + p.soort()), Component.translatable("biome.guhs.safari_" + p.zone()),
                Math.round(p.centrum().getX() / 5f) * 5, Math.round(p.centrum().getZ() / 5f) * 5};
    }

    // --- the fotograaf: the camera, and how photos work ------------------------------------------------------------------

    static final NpcRole FOTOGRAAF = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            if (haken("fotograaf", npc, p)) {
                return;
            }
            if (geefCamera(p)) {
                Praat.open(p, npc, null, "gui.guhs.safari.fotograaf.camera", new Object[0], new Praat.Optie(3, "gui.guhs.safari.optie.dank"));
            } else {
                Praat.open(p, npc, null, "gui.guhs.safari.fotograaf.hallo", new Object[0], new Praat.Optie(3, "gui.guhs.safari.optie.doei"));
            }
        }
    };

    // --- the dierenverzorger: today's favourite foods, a free food bag once a day, a brush and a lead -------------------

    static final NpcRole VERZORGER = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            if (haken("verzorger", npc, p)) {
                return;
            }
            Praat.open(p, npc, null, "gui.guhs.safari.verzorger.hallo", new Object[]{Aaihoek.hint()},
                    new Praat.Optie(1, "gui.guhs.safari.verzorger.optie.voer"), new Praat.Optie(3, "gui.guhs.safari.optie.doei"));
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
            if (optie != 1) {
                return;
            }
            MinecraftServer server = p.level().getServer();
            if (SafariSpeler.telDag(server, p.getUUID(), "aaihoek:voerzak", 1, 1) == 0) {
                Praat.open(p, npc, null, "gui.guhs.safari.verzorger.voer.al", new Object[0], new Praat.Optie(3, "gui.guhs.safari.optie.dank"));
                return;
            }
            for (String voer : List.of("wortel", "hooi", "appel", "komkommer", "paprika", "suikerklontje")) {
                Minigames.give(p, new ItemStack(Aaihoek.item(voer), 2));
            }
            if (!p.getInventory().contains(new ItemStack(Items.BRUSH))) {
                Minigames.give(p, new ItemStack(Items.BRUSH));
            }
            if (!p.getInventory().contains(new ItemStack(Items.LEAD))) {
                Minigames.give(p, new ItemStack(Items.LEAD));
            }
            Praat.open(p, npc, null, "gui.guhs.safari.verzorger.voer", new Object[0], new Praat.Optie(3, "gui.guhs.safari.optie.dank"));
        }
    };

    // --- the giftshop seller --------------------------------------------------------------------------------------------

    static final NpcRole VERKOPER = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            if (haken("verkoper", npc, p)) {
                return;
            }
            p.sendSystemMessage(Component.translatable("gui.guhs.safari.verkoper.hallo").withStyle(ChatFormatting.LIGHT_PURPLE));
            SafariWinkel.open(p);
        }
    };

    // --- the lodgebeheerder ---------------------------------------------------------------------------------------------

    static final NpcRole LODGE = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            if (haken("lodgebeheerder", npc, p)) {
                return;
            }
            Praat.open(p, npc, null, "gui.guhs.safari.lodge.hallo", new Object[0], new Praat.Optie(3, "gui.guhs.safari.optie.dank"));
        }
    };

    // --- the eight zone experts ---------------------------------------------------------------------------------------

    /** Is there something to claim at this expert for this player (the zone complete in the dex, the badge not yet taken)? */
    public static boolean claimbaar(MinecraftServer server, UUID speler, String zone) {
        return SafariSpeler.zoneCompleet(server, speler, zone) && !SafariSpeler.heeftBadge(server, speler, zone);
    }

    /** How many dex species of this zone the player photographed, and how many there are. */
    static int[] voortgang(MinecraftServer server, UUID speler, String zone) {
        Set<String> dex = SafariSpeler.gefotografeerd(server, speler);
        int n = 0, al = 0;
        for (ParkData.Soort s : ParkData.get().soorten.values()) {
            if (zone.equals(s.zone()) && SafariSpeler.telt(s.naam())) {
                n++;
                if (dex.contains(s.naam())) {
                    al++;
                }
            }
        }
        return new int[]{al, n};
    }

    /** Claims the zone reward (badge, zone painting when it exists, 100 coins once); false when there is nothing to claim. */
    public static boolean claim(MinecraftServer server, UUID speler, ServerPlayer p, String zone) {
        if (!claimbaar(server, speler, zone) || !SafariSpeler.claimBadge(server, speler, zone)) {
            return false;
        }
        if (p != null) {
            Minigames.give(p, new ItemStack(LobbyItems.BADGES.get(zone).get()));
            ItemStack schilderij = zoneSchilderij.apply(server, zone);
            if (!schilderij.isEmpty()) {
                Minigames.give(p, schilderij);
            }
        }
        SafariMunten.verdienEens(server, speler, "zone_compleet:" + zone, 100, "zone");
        return true;
    }

    record Expert(String zone) implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            if (haken("expert_" + zone, npc, p)) {
                return;
            }
            MinecraftServer server = p.level().getServer();
            String sleutel = "gui.guhs.safari.expert." + zone;
            if (claim(server, p.getUUID(), p, zone)) {
                Praat.open(p, npc, null, sleutel + ".beloning", new Object[]{Component.translatable("biome.guhs.safari_" + zone)},
                        new Praat.Optie(3, "gui.guhs.safari.optie.dank"));
                return;
            }
            int[] v = voortgang(server, p.getUUID(), zone);
            String tekst = SafariSpeler.heeftBadge(server, p.getUUID(), zone) ? sleutel + ".al" : sleutel + ".nog";
            Praat.open(p, npc, null, tekst, new Object[]{v[0], v[1], Math.max(0, v[1] - v[0])}, new Praat.Optie(3, "gui.guhs.safari.optie.doei"));
        }
    }

    private Rollen() {
    }
}
