package nl.juiced.guhs.feature.safari;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.safari.camera.Fotograaf;
import nl.juiced.guhs.feature.safari.lobby.Rollen;
import nl.juiced.guhs.feature.safari.lobby.SafariWinkel;
import nl.juiced.guhs.feature.safari.momenten.Momenten;
import nl.juiced.guhs.feature.safari.quests.SafariQuests;
import nl.juiced.guhs.feature.safari.verblijf.VerblijfWinkel;

/**
 * Op Safari! step 2: the hooks between the safari slices (SAFARI_CONTRACT.md, the S1..S6 sections), wired in one place so
 * the slices themselves stay independent.
 * <ul>
 *   <li>S3 → S1: the birthday species of the day ({@link Momenten#jarigeSoort}) pays double photo coins.</li>
 *   <li>S2 → S4: the hoofdranger speaks about de vermomde guh first ({@link SafariQuests#hoofdranger}).</li>
 *   <li>S6 → S2: the zone experts hand out the zone painting; the giftshop sells the 14 safari paintings.</li>
 *   <li>S5 → S2: the giftshop sells S5's verblijfsborden.</li>
 *   <li>S4 → S5: the bordbon of the full dex is redeemed (right click) for the Gouden dikdik's verblijfsbord.</li>
 * </ul>
 * (Elsewhere, in the slices' own code: the aaihoek chores count for S4's quest, the shutter/coin/canoe/truck sounds are
 * S6's, the northern lights give S1's bonus point, and S6's verrekijker lights the pootafdrukken because NeoForge's
 * isScoping already counts every item with the spyglass ability.)
 */
public final class SafariKoppelingen {
    /** Zone → the painting its expert hands out (S6's painting variants; the entree has none). */
    static final Map<String, String> ZONE_SCHILDERIJ = Map.of(
            "savanne", "safari_savanne", "delta", "safari_flamingo", "regenwoud", "safari_regenwoud",
            "bamboe", "safari_rode_panda", "ijskust", "safari_pinguinfamilie", "oertijd", "safari_grote_trek",
            "europa", "safari_otters", "australie", "safari_koala", "kriebelhuis", "safari_kriebelhuis");   // (G1: the 9th badge's painting)
    /** The paintings in the giftshop: variant → price (by size: 1x1 10, 1x2/2x1 15, 2x2 20, 3x2 25, 3x3/4x2 30). */
    static final Map<String, Integer> SCHILDERIJEN = new LinkedHashMap<>();

    static {
        for (String v : new String[] {"rode_panda", "pinguin", "koala", "stokstaartje"}) {
            SCHILDERIJEN.put("safari_" + v, 10);
        }
        for (String v : new String[] {"giraffe", "flamingo", "pinguinfamilie", "otters", "kriebelhuis"}) {
            SCHILDERIJEN.put("safari_" + v, 15);
        }
        SCHILDERIJEN.put("safari_leeuwenfamilie", 20);
        SCHILDERIJEN.put("safari_reuzenpanda", 20);
        SCHILDERIJEN.put("safari_savanne", 25);
        SCHILDERIJEN.put("safari_regenwoud", 25);
        SCHILDERIJEN.put("safari_olifant", 30);
        SCHILDERIJEN.put("safari_grote_trek", 30);
    }

    public static void register() {
        Fotograaf.JARIG = soort -> soort != null && soort.equals(Momenten.jarigeSoort());
        Rollen.haak("hoofdranger", (npc, p) -> SafariQuests.hoofdranger(p, npc));
        // S5 → S2: the giftshop's board per species is S5's verblijfsbord item (the shop itself checks dex, price, cooldown)
        SafariWinkel.borden = soort -> nl.juiced.guhs.feature.safari.verblijf.VerblijfData.dier(soort) == null ? ItemStack.EMPTY
                : nl.juiced.guhs.feature.safari.verblijf.VerblijfsbordItem.bord(soort);
        Rollen.zoneSchilderij = (server, zone) -> {
            String v = ZONE_SCHILDERIJ.get(zone);
            return v == null ? ItemStack.EMPTY : schilderij(server, v);
        };
        for (Map.Entry<String, Integer> e : SCHILDERIJEN.entrySet()) {
            String v = e.getKey();
            SafariWinkel.aanbod("kunst_" + v).groep("kunst").icoon(() -> new ItemStack(Items.PAINTING))
                    .naam(Component.translatable("painting.guhs." + v + ".title"))
                    .prijs(e.getValue())
                    .lever((p, a) -> {
                        ItemStack s = schilderij(p.level().getServer(), v);
                        return !s.isEmpty() && (p.getInventory().add(s) || p.drop(s, false) != null);
                    }).registreer();
        }
        NeoForge.EVENT_BUS.addListener(SafariKoppelingen::opItem);
    }

    /** A painting item showing this safari painting variant (EMPTY when the variant is missing). */
    public static ItemStack schilderij(MinecraftServer server, String variant) {
        ResourceKey<PaintingVariant> key = ResourceKey.create(Registries.PAINTING_VARIANT, Guhs.id(variant));
        return server.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).get(key).map(h -> {
            ItemStack s = new ItemStack(Items.PAINTING);
            s.set(DataComponents.PAINTING_VARIANT, (Holder<PaintingVariant>) h);
            return s;
        }).orElse(ItemStack.EMPTY);
    }

    /** The bordbon of the full dex: right click = the Gouden dikdik's verblijfsbord. */
    static void opItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(SafariQuests.BORDBON_GOUDEN_DIKDIK.get())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer p) {
            event.getItemStack().shrink(1);
            VerblijfWinkel.geef(p, "gouden_dikdik");
            p.sendSystemMessage(Component.translatable("gui.guhs.safari.bordbon.ingewisseld"));
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private SafariKoppelingen() {
    }
}
