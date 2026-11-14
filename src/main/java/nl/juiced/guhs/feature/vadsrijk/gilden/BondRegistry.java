package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtData;
import nl.juiced.guhs.feature.vadsrijk.karakter.Karakter;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;

/**
 * U-GILDEN (Guhs 1.10.0, round U, FEEDBACK_RONDE4 V1): the Knuffelbonden (guilds). Named "Knuffelbond" because Hoogvads already has
 * the Ambachtsgilde (t-z5verhaal's professions guild with its Gildemeester): "gilde" would mean two things in one city.
 * <ul>
 *   <li>{@link Bonden} (SavedData) + {@link Bond}: the bonds, members per CHARACTER, ranks ({@link Rang});</li>
 *   <li>{@link BondActies}: founding (Hoogvads only), invites, the board, ranks, settings;</li>
 *   <li>{@link BondXp}: bond xp from what members do, levels 1-10, perks, week tasks, Bondsrust;</li>
 *   <li>{@link Vallei} + {@link Kasteel} + {@link BondBank} + {@link BondTeleport}: the Bondenvallei, castles 1-5, vault, teleports;</li>
 *   <li>{@link BondChat}, {@link BondTags}, {@link Banier}, {@link BondPaneel} (the chat panel), {@link BondNpcs}, {@link BondExport};</li>
 *   <li>commands: {@link BondCommando}.</li>
 * </ul>
 */
public final class BondRegistry {
    public static final String SLICE = "gilden";

    private BondRegistry() {
    }

    public static void register(IEventBus modBus) {
        Gebieden.voegVooraan(Vallei.gebied());
        BondNpcs.init();
        Spelers.XP_FACTOR.add(BondXp::factor);
        QuestMotor.KLIK_VOOR.add(BondNpcs::klik);
        Praat.luister(BondNpcs.SLEUTEL, BondNpcs::antwoord);
        nl.juiced.guhs.feature.vadsrijk.web.WebExport.EXTRA_HISCORES.add(BondExport::exporteer);
        nl.juiced.guhs.feature.vadsrijk.luchtschip.Luchtschepen.PRIJS_FACTOR.add(p -> {   // the travel discount perk
            Bond b = Bonden.van(p);
            return b == null ? 1 : 1 - BondXp.reisKorting(b.level());
        });

        // ---- bond xp from what members already do ----------------------------------------------------------------------------------
        Haken.VERSLAGEN.add(v -> {
            boolean baas = v.mob() instanceof VadsrijkBaas;
            MinecraftServer s = v.mob().level().getServer();
            Set<Integer> trofee = new HashSet<>();
            for (ServerPlayer p : v.helpers()) {
                if (baas) {
                    BondXp.deed(p, "bazen", 1, 40);
                    Bond b = Bonden.van(p);
                    if (b != null && s != null && trofee.add(b.id)) {
                        Kasteel.trofee(s, b, ((VadsrijkBaas) v.mob()).titelKey());
                    }
                } else {
                    BondXp.deed(p, "opgevrolijkt", 1, v.mob().elite() ? 3 : 1);
                }
            }
        });
        NpcApi.QUEST_KLAAR.add((p, q) -> BondXp.deed(p, "quests", 1, 15));
        Haken.LEVEL_OP.add((p, wat) -> BondXp.deed(p, "levels", 1, 10));
        AmbachtData.LEVEL_OP.add((p, a, lv) -> BondXp.deed(p, null, 0, 10));

        // ---- characters, logins, names -------------------------------------------------------------------------------------------------
        Karakter.VOOR_WISSEL.add(p -> {
            BondXp.uitgelogd(p);
            if (p.containerMenu instanceof BondBank.Menu) {
                p.closeContainer();
            }
        });
        Karakter.GEWISSELD.add((p, w) -> {
            BondXp.ingelogd(p);
            BondTags.ververs(p.level().getServer());
        });
        Haken.BINNEN.add((p, binnen) -> BondTags.ververs(p.level().getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> Bonden.server = e.getServer());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                BondXp.ingelogd(p);
                BondTags.stuur(p);
                BondTags.ververs(p.level().getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                BondXp.uitgelogd(p);
                BondXp.vergeet(p.getUUID());
                BondChat.vergeet(p.getUUID());
                BondNpcs.vergeet(p.getUUID());
                BondTags.vergeet(p.getUUID());
                MinecraftServer s = p.level().getServer();
                if (s != null) {
                    s.execute(() -> BondTags.ververs(s));
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(BondTags::naam);
        NeoForge.EVENT_BUS.addListener(BondTags::tabNaam);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, BondChat::opChat);

        // ---- the valley ------------------------------------------------------------------------------------------------------------------
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (PlayerInteractEvent.RightClickBlock e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || !Vadsrijk.in(p) || !Vallei.in(e.getPos().getX(), e.getPos().getZ())) {
                return;
            }
            if (Kasteel.klik(p, e.getPos(), e.getLevel().getBlockState(e.getPos()), e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND)) {
                e.setCanceled(true);
            }
        });
        nl.juiced.guhs.world.TikWacht.post("BondRegistry:124", e -> {
            MinecraftServer s = e.getServer();
            long t = s.getTickCount();
            Vallei.tik(s, t);
            BondNpcs.tik(s);
            if (t % 20 == 7) {
                for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                    if (Vadsrijk.in(p) && Bonden.van(p) != null) {
                        BondXp.tik(p);
                    }
                }
            }
            if (t % 400 == 9) {
                BondTags.ververs(s);
            }
        });
        NeoForge.EVENT_BUS.addListener(BondCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        BondTags.payloads(registrar);
    }
}
