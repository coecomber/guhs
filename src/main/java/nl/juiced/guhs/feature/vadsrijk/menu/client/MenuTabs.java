package nl.juiced.guhs.feature.vadsrijk.menu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.feature.vadsrijk.menu.MenuServer;

/**
 * U-TABS: all tabs of the Vadsrijk-menu, in one place (the existing screens + the new Karakter, Puzzels, Reiskaart and
 * Instellingen panels). Each tab opens exactly what its key opened before; the key is shown in the tab's tooltip.
 */
final class MenuTabs {
    private MenuTabs() {
    }

    static void registreer() {
        VadsrijkMenu.tab("karakter", () -> new ItemStack(Items.IRON_CHESTPLATE), () -> vraag("karakter"), 10);
        VadsrijkMenu.toets("karakter", MenuClient.MENU);

        VadsrijkMenu.tab("skillboom", () -> new ItemStack(Items.ENCHANTED_BOOK),
                () -> ClientPacketDistributor.sendToServer(new nl.juiced.guhs.feature.vadsrijk.klassen.KlassenPayloads.Vraag()), 20);
        VadsrijkMenu.toets("skillboom", nl.juiced.guhs.feature.vadsrijk.klassen.client.KlassenClient.BOOM);

        VadsrijkMenu.tab("questlog", () -> new ItemStack(Items.WRITABLE_BOOK),
                () -> ClientPacketDistributor.sendToServer(new nl.juiced.guhs.feature.vadsrijk.npc.NpcPayloads.LogVraag()), 30);
        VadsrijkMenu.toets("questlog", nl.juiced.guhs.feature.vadsrijk.npc.client.NpcClient.QUESTLOG);

        String boek = nl.juiced.guhs.feature.vadsrijk.voltooiing.client.VoltooiingClient.TAB;
        VadsrijkMenu.tab(boek, () -> new ItemStack(Items.FILLED_MAP), nl.juiced.guhs.feature.vadsrijk.voltooiing.client.VoltooiingClient::open, 40);
        VadsrijkMenu.toets(boek, nl.juiced.guhs.feature.vadsrijk.voltooiing.client.VoltooiingClient.BOEK);

        String rij = nl.juiced.guhs.feature.vadsrijk.economie.client.RijdierClient.TAB;
        VadsrijkMenu.tab(rij, () -> new ItemStack(Items.SADDLE),
                () -> ClientPacketDistributor.sendToServer(new nl.juiced.guhs.feature.vadsrijk.economie.EcoPayloads.RijdierToets(1)), 50);
        VadsrijkMenu.toets(rij, nl.juiced.guhs.feature.vadsrijk.economie.client.RijdierClient.SCHERM);
        VadsrijkMenu.subtab(rij, nl.juiced.guhs.feature.vadsrijk.zone4.boot.client.BootClient.TAB, () -> Component.translatable("vadsrijk.z4boot.tab"),
                () -> ClientPacketDistributor.sendToServer(new nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootPayloads.Doe("open", "")), 10);

        String huis = nl.juiced.guhs.feature.vadsrijk.huisdieren.client.HuisdierClient.TAB;
        VadsrijkMenu.tab(huis, () -> new ItemStack(Items.LEAD), nl.juiced.guhs.feature.vadsrijk.huisdieren.client.HuisdierClient::open, 60);
        VadsrijkMenu.toets(huis, nl.juiced.guhs.feature.vadsrijk.huisdieren.client.HuisdierClient.SCHERM);

        String kast = nl.juiced.guhs.feature.vadsrijk.kleding.client.KledingClient.TAB;
        VadsrijkMenu.tab(kast, () -> new ItemStack(Items.LEATHER_CHESTPLATE), nl.juiced.guhs.feature.vadsrijk.kleding.client.KledingClient::open, 70);
        VadsrijkMenu.toets(kast, nl.juiced.guhs.feature.vadsrijk.kleding.client.KledingClient.KAST);

        VadsrijkMenu.tab("knapzak", () -> new ItemStack(Items.BUNDLE),
                () -> ClientPacketDistributor.sendToServer(new nl.juiced.guhs.feature.vadsrijk.opslag.OpslagPayloads.Open(0)), 80);
        VadsrijkMenu.toets("knapzak", nl.juiced.guhs.feature.vadsrijk.opslag.client.OpslagClient.KNAPZAK);
        VadsrijkMenu.subtab("knapzak", "materiaaltas", () -> Component.translatable("vadsrijk.menu.tab.materiaaltas"),
                () -> ClientPacketDistributor.sendToServer(new nl.juiced.guhs.feature.vadsrijk.opslag.OpslagPayloads.Open(1)), 10);

        String amb = nl.juiced.guhs.feature.vadsrijk.ambacht.client.AmbachtClient.TAB;
        VadsrijkMenu.tab(amb, () -> new ItemStack(Items.CRAFTING_TABLE), nl.juiced.guhs.feature.vadsrijk.ambacht.client.AmbachtClient::open, 90);
        VadsrijkMenu.toets(amb, nl.juiced.guhs.feature.vadsrijk.ambacht.client.AmbachtClient.BOEK);

        VadsrijkMenu.tab("puzzels", () -> new ItemStack(Items.AMETHYST_SHARD), () -> {
            Minecraft.getInstance().setScreen(new PuzzelPaneel(new CompoundTag()));
            nl.juiced.guhs.feature.vadsrijk.voltooiing.client.VoltooiingClient.open();   // (the book's data, the panel shows the puzzle/secret pages)
        }, 100);

        VadsrijkMenu.tab("ridders", () -> new ItemStack(Items.ARMOR_STAND),
                () -> ClientPacketDistributor.sendToServer(new nl.juiced.guhs.feature.vadsrijk.karakter.KarakterPayloads.Doe("open", new CompoundTag())), 110);

        VadsrijkMenu.tab("reiskaart", () -> new ItemStack(Items.MAP), () -> vraag("reiskaart"), 120);

        VadsrijkMenu.scherm("instellingen", () -> new ItemStack(Items.COMPARATOR), InstellingenPaneel::new, 130);
    }

    static void vraag(String wat) {
        ClientPacketDistributor.sendToServer(new MenuServer.Vraag(wat));
    }

    /** The server's answer: open (or refresh) the panel. */
    static void ontvang(MenuServer.Antwoord a) {
        Minecraft mc = Minecraft.getInstance();
        switch (a.wat()) {
            case "karakter" -> {
                if (mc.screen instanceof KarakterPaneel k) {
                    k.update(a.data(), a.items());
                } else {
                    mc.setScreen(new KarakterPaneel(a.data(), a.items()));
                }
            }
            case "open" -> {
                String tab = a.data().getStringOr("tab", "");
                if (mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen) {
                    mc.setScreen(null);   // (typed in the chat: the chat closes first)
                }
                VadsrijkMenu.open(tab.isEmpty() ? null : tab);
            }
            case "reiskaart" -> mc.setScreen(new nl.juiced.guhs.feature.vadsrijk.npc.client.ReiskaartScherm(MenuServer.reiskaartVan(a.data()), true));
            default -> {
            }
        }
    }
}
