package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * einde2 phase B, slice h1_lucht: the Zuidelijke Luchttempel itself (CONTRACT_E2_B 7.2). The steps that are not a minigame
 * of their own: arriving (the narrator card and the scene aankomst), the Hal van de Guhvatars (Roku-guh and Kyoshi-guh, the
 * scene hal), Appa's stable (the saddle, the apples), the closed door of the Werktuigguh, the pier (the scene zuka_steiger,
 * then {@link Ontsnapping}), and the end of the chapter ({@link #afronden}: flag geleerd, the rewards, the flight on).
 * The roles of the temple's residents ({@code NpcRollen}, CONTRACT_E2_B 4.4): Aang-guh at the gate, Guhatso on the
 * taartenterras, the spirits of Roku-guh and Kyoshi-guh in the hall. Everything is per player; nothing in the world changes.
 */
public final class Tempel {
    /** Players whose card / scene of arriving has been started (not saved: the step is the truth). */
    private static final Map<UUID, Long> AANGEKOMEN = new ConcurrentHashMap<>();
    /** When Appa was last called into his stable for this player (game time). */
    private static final Map<UUID, Long> APPA_GEROEPEN = new ConcurrentHashMap<>();

    private Tempel() {
    }

    private static net.minecraft.network.chat.MutableComponent tekst(String key, Object... args) {
        return Component.translatable(E2H1Lucht.P + key, args);
    }

    // =====================================================================================================================
    // every few ticks, for a player in the Guheinde whose chapter is going on
    // =====================================================================================================================
    static void tick(ServerPlayer p) {
        int stap = E2H1Lucht.LIJN.stap(p);
        if (stap < E2H1Lucht.VLUCHT || stap >= E2H1Lucht.STAPPEN || Cutscenes.bezig(p) || !E2H1Lucht.inTempel(p)) {
            return;
        }
        ServerLevel g = p.level();
        baken(p, g, stap);
        switch (stap) {
            case E2H1Lucht.VLUCHT -> {
                if (!E2Reizen.landtNog(p)) {                        // (merge_fix1: the flight's daarna starts it once Appa landed)
                    aangekomen(p);
                }
            }
            case E2H1Lucht.STAL -> stalTick(p, g);
            case E2H1Lucht.DEUR -> {
                BlockPos deur = E2H1Lucht.plek(g, "werkplaatsdeur");
                if (deur != null && deur.closerToCenterThan(p.position(), 4.5)) {
                    deur(p, g, deur);
                } else if (deur != null && deur.closerToCenterThan(p.position(), 24) && g.getGameTime() % 100 < 5) {
                    p.sendOverlayMessage(tekst("deur.hier").withStyle(ChatFormatting.GOLD));
                }
            }
            case E2H1Lucht.ZUKA -> {
                if (E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_VLUCHT)) {
                    return;                                         // (the escape ticks itself)
                }
                AABB zone = E2H1Lucht.zone(g, "steiger_zone");
                BlockPos steiger = E2H1Lucht.plek(g, "steiger");
                if (zone != null && steiger != null && zone.inflate(0, 2, 0).contains(p.position())
                        && steiger.closerToCenterThan(p.position(), 9)) {
                    zukaScene(p, steiger);
                }
            }
            default -> {
            }
        }
    }

    // =====================================================================================================================
    // step 1 -> 2: arriving
    // =====================================================================================================================

    /** Landed at the temple (the flight's {@code daarna}, or the player walked in at step 1): the card, then the scene. */
    static void aangekomen(ServerPlayer p) {
        if (E2H1Lucht.LIJN.stap(p) != E2H1Lucht.VLUCHT || Cutscenes.bezig(p)) {
            return;
        }
        Long sinds = AANGEKOMEN.get(p.getUUID());
        long nu = p.level().getGameTime();
        if (sinds != null && nu - sinds < 100) {
            return;
        }
        ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
        BlockPos anker = g == null ? null : E2H1Lucht.plek(g, "aankomst_speler");
        if (anker == null || !GuheindeFeature.isGuheinde(p.level())) {
            return;
        }
        AANGEKOMEN.put(p.getUUID(), nu);
        if (!Verteller.toon(p, E2H1Lucht.KAART, a -> aankomstScene(a, anker))) {
            aankomstScene(p, anker);
        }
    }

    private static void aankomstScene(ServerPlayer p, BlockPos anker) {
        if (!Cutscenes.speel(p, H1Scenes.AANKOMST, anker, Rotation.NONE, s -> E2H1Lucht.LIJN.verder(s, E2H1Lucht.VLUCHT))) {
            E2H1Lucht.LIJN.verder(p, E2H1Lucht.VLUCHT);
        }
    }

    // =====================================================================================================================
    // every step shows where to go: a soft column of light over the one to talk to, the thing to click, the place to be
    // (fix round 1, playtest 1: "no idea where"); the track, the rings and Momo shine in their own classes
    // =====================================================================================================================
    private static void baken(ServerPlayer p, ServerLevel g, int stap) {
        java.util.List<String> waar = switch (stap) {
            case E2H1Lucht.HAL -> java.util.List.of("roku_beeld", "kyoshi_beeld");
            case E2H1Lucht.LUCHTBAL -> Luchtbal.bezig(p) ? java.util.List.of() : java.util.List.of("guhatso");
            case E2H1Lucht.DEUR -> java.util.List.of("werkplaatsdeur");
            case E2H1Lucht.ZUKA -> java.util.List.of(E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_VLUCHT) ? "appa_kamp" : "steiger");
            default -> java.util.List.of();
        };
        long nu = g.getGameTime();
        for (String id : waar) {
            Vec3 m = E2H1Lucht.midden(g, id);
            if (m != null) {
                straal(p, m.add(0, id.endsWith("_beeld") || id.equals("guhatso") ? 2.3 : 0.4, 0), nu);
            }
        }
    }

    /** A soft rising column of light (seen from afar) at this spot. */
    static void straal(ServerPlayer p, Vec3 plek, long nu) {
        for (int i = 0; i < 3; i++) {
            E2H1Lucht.ver(p, ParticleTypes.END_ROD, plek.add(0, ((nu / 5 + i * 3) % 9) * 0.7, 0), 1, 0.12, 0.0);
        }
    }

    // =====================================================================================================================
    // step 7: Appa's stable (fix round 1, playtest 1: "no idea what was expected"). Three plain things, each one shines:
    // take the saddle off its trestle (a right click: you get Appa's saddle), click Appa with it (it goes on), and give him
    // three apples from the barrel by the door. Saddle and apples in any order; nothing can be lost (the trestle and the
    // barrel give again).
    // =====================================================================================================================
    private static void stalTick(ServerPlayer p, ServerLevel g) {
        AABB stal = E2H1Lucht.zone(g, "appa_stal");
        Vec3 plek = E2H1Lucht.midden(g, "appa");
        if (stal == null || plek == null) {
            return;
        }
        long nu = g.getGameTime();
        boolean op = E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_ZADEL);
        boolean zadelMee = heeftZadel(p);
        int appelsNodig = 3 - E2H1Lucht.LIJN.teller(p, E2H1Lucht.T_APPELS);
        boolean appelsMee = GuhQuests.count(p, Items.APPLE) >= Math.max(1, appelsNodig);
        Vec3 zadel = E2H1Lucht.midden(g, "zadel"), appels = E2H1Lucht.midden(g, "appels");
        if (!op && !zadelMee && zadel != null) {
            straal(p, zadel.add(0, 1.2, 0), nu);
            E2H1Lucht.ver(p, ParticleTypes.HAPPY_VILLAGER, zadel.add(0, 1.2, -0.5), 3, 0.6, 0.0);
        }
        if (appelsNodig > 0 && !appelsMee && appels != null) {
            straal(p, appels.add(0, 1.4, 0), nu);
        }
        AppaGuhEntity appa = Appa.geladen(p);
        if (appa != null && appa.level() == g && ((zadelMee && !op) || (appelsNodig > 0 && GuhQuests.count(p, Items.APPLE) > 0))) {
            E2H1Lucht.ver(p, ParticleTypes.HEART, appa.position().add(0, appa.getBbHeight() + 0.6, 0), 1, 0.5, 0.0);
        }
        boolean binnen = stal.inflate(2).contains(p.position());
        if (binnen && nu % 100 < 5) {
            String wat = !op && !zadelMee ? "stal.doe_zadel" : !op ? "stal.doe_op_appa" : !appelsMee ? "stal.doe_appels" : "stal.doe_voeren";
            p.sendOverlayMessage(tekst(wat).withStyle(ChatFormatting.GOLD));
        }
        // Appa comes into his stable when you are there
        if (!binnen || !Appa.heeft(p)) {
            return;
        }
        if ((appa == null || appa.level() != g || appa.position().distanceTo(plek) > 10) && nu - APPA_GEROEPEN.getOrDefault(p.getUUID(), -1000L) > 200) {
            APPA_GEROEPEN.put(p.getUUID(), nu);
            appa = Appa.roep(p);
            if (appa != null && p.getVehicle() != appa) {
                appa.snapTo(plek.x, plek.y, plek.z, 180f, 0f);
                p.sendOverlayMessage(tekst("stal.appa_komt").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    /** Does the player carry a saddle (Appa's, or any)? */
    static boolean heeftZadel(ServerPlayer p) {
        return GuhQuests.count(p, Items.SADDLE) > 0;
    }

    /** Is pos within this box around a marker (x from dx0 to dx1, y from one below to dy1 above, z within dz)? */
    private static boolean bij(BlockPos pos, @Nullable BlockPos m, int dx0, int dx1, int dy1, int dz) {
        return m != null && pos.getX() >= m.getX() + dx0 && pos.getX() <= m.getX() + dx1 && pos.getY() >= m.getY() - 1
                && pos.getY() <= m.getY() + dy1 && Math.abs(pos.getZ() - m.getZ()) <= dz;
    }

    /** A right-click on a block: the saddle's trestle or the apple barrel in the stable (step 7), the closed door (step 8). */
    static void opBlok(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND || !E2H1Lucht.inTempel(p)) {
            return;
        }
        int stap = E2H1Lucht.LIJN.stap(p);
        ServerLevel g = p.level();
        BlockPos pos = event.getPos();
        if (stap == E2H1Lucht.DEUR) {
            BlockPos deur = E2H1Lucht.plek(g, "werkplaatsdeur");
            if (bij(pos, deur, -1, 1, 3, 2)) {
                deur(p, g, deur);
            }
            return;
        }
        if (stap != E2H1Lucht.STAL) {
            return;
        }
        BlockPos zadel = E2H1Lucht.plek(g, "zadel"), appels = E2H1Lucht.plek(g, "appels");
        if (bij(pos, zadel, -3, 3, 2, 2)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_ZADEL)) {
                p.sendOverlayMessage(tekst("stal.zadel_zit_al").withStyle(ChatFormatting.GRAY));
                return;
            }
            if (heeftZadel(p)) {
                p.sendOverlayMessage(tekst("stal.doe_op_appa").withStyle(ChatFormatting.GOLD));
                return;
            }
            ItemStack z = new ItemStack(Items.SADDLE);
            z.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, tekst("stal.zadel_naam").withStyle(ChatFormatting.GOLD));
            Minigames.give(p, z);
            g.playSound(null, zadel, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            g.sendParticles(ParticleTypes.WAX_ON, zadel.getX() + 0.5, zadel.getY() + 1.0, zadel.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.02);
            p.sendSystemMessage(tekst("stal.zadel_gepakt").withStyle(ChatFormatting.YELLOW));
        } else if (bij(pos, appels, -1, 2, 1, 1)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            int heb = GuhQuests.count(p, Items.APPLE);
            int nodig = 3 - E2H1Lucht.LIJN.teller(p, E2H1Lucht.T_APPELS);
            if (nodig <= 0) {
                p.sendOverlayMessage(tekst("stal.vol").withStyle(ChatFormatting.GRAY));
            } else if (heb < nodig) {
                Minigames.give(p, new ItemStack(Items.APPLE, nodig - heb));
                g.playSound(null, appels, SoundEvents.BARREL_OPEN, SoundSource.PLAYERS, 0.7f, 1.0f);
                p.sendSystemMessage(tekst("stal.appels").withStyle(ChatFormatting.YELLOW));
            } else {
                p.sendOverlayMessage(tekst("stal.doe_voeren").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** A right-click on an entity: the saddle or an apple for the player's own Appa (step 7). */
    static void opEntity(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND
                || E2H1Lucht.LIJN.stap(p) != E2H1Lucht.STAL) {
            return;
        }
        AppaGuhEntity appa = Appa.van(event.getTarget());
        if (appa == null || !appa.isEigenaar(p) || !E2H1Lucht.inTempel(p)) {
            return;
        }
        ServerLevel g = p.level();
        ItemStack hand = event.getItemStack();
        if (hand.is(Items.SADDLE)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_ZADEL)) {
                p.sendOverlayMessage(tekst("stal.zadel_zit_al").withStyle(ChatFormatting.GRAY));
                return;
            }
            hand.shrink(1);
            E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_ZADEL, true);
            g.playSound(null, appa, SoundEvents.HORSE_SADDLE.value(), SoundSource.NEUTRAL, 1.0f, 0.8f);
            g.sendParticles(ParticleTypes.WAX_ON, appa.getX(), appa.getY() + appa.getBbHeight(), appa.getZ(), 12, 0.8, 0.3, 0.8, 0.02);
            p.sendSystemMessage(tekst("stal.zadel_klaar").withStyle(ChatFormatting.GREEN));
            stalKlaar(p);
            return;
        }
        if (!hand.is(Items.APPLE)) {
            // (no cancel: Appa's own click stays; only say what he wants)
            boolean op = E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_ZADEL);
            p.sendOverlayMessage(tekst(!op && !heeftZadel(p) ? "stal.doe_zadel" : !op ? "stal.doe_op_appa" : "stal.doe_appels").withStyle(ChatFormatting.GOLD));
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        int n = E2H1Lucht.LIJN.teller(p, E2H1Lucht.T_APPELS);
        if (n >= 3) {
            p.sendOverlayMessage(tekst("stal.vol").withStyle(ChatFormatting.GRAY));
            return;
        }
        hand.shrink(1);
        E2H1Lucht.LIJN.teller(p, E2H1Lucht.T_APPELS, n + 1);
        g.sendParticles(ParticleTypes.HEART, appa.getX(), appa.getY() + appa.getBbHeight() + 0.3, appa.getZ(), 4, 0.6, 0.2, 0.6, 0.02);
        g.playSound(null, appa, SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1.0f, 0.7f);
        p.sendOverlayMessage(tekst("stal.voer", n + 1).withStyle(ChatFormatting.YELLOW));
        if (n + 1 >= 3) {
            if (!E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_ZADEL)) {
                p.sendSystemMessage(tekst(heeftZadel(p) ? "stal.doe_op_appa" : "stal.doe_zadel").withStyle(ChatFormatting.GOLD));
            }
            stalKlaar(p);
        }
    }

    private static void stalKlaar(ServerPlayer p) {
        if (E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_ZADEL) && E2H1Lucht.LIJN.teller(p, E2H1Lucht.T_APPELS) >= 3
                && E2H1Lucht.LIJN.verder(p, E2H1Lucht.STAL)) {
            p.sendSystemMessage(tekst("stal.klaar").withStyle(ChatFormatting.GREEN));
            E2H1Lucht.titel(p, tekst("stal.titel").withStyle(ChatFormatting.GREEN), tekst("stal.titel_onder").withStyle(ChatFormatting.YELLOW), 50);
            AppaGuhEntity appa = Appa.geladen(p);
            if (appa != null) {
                p.level().playSound(null, appa, nl.juiced.guhs.feature.einde2.appa.E2AppaFeature.BRUL.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
        }
    }

    // =====================================================================================================================
    // step 8: the closed door
    // =====================================================================================================================
    private static void deur(ServerPlayer p, ServerLevel g, BlockPos deur) {
        if (!E2H1Lucht.LIJN.verder(p, E2H1Lucht.DEUR)) {
            return;
        }
        g.playSound(null, deur, SoundEvents.WOOD_HIT, SoundSource.PLAYERS, 1.0f, 0.9f);
        p.sendSystemMessage(tekst("deur.klop").withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(tekst("deur.werktuigguh").withStyle(ChatFormatting.GOLD));
        g.playSound(null, deur, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3f, 1.6f);
        BlockPos gong = E2H1Lucht.plek(g, "steiger");
        g.playSound(null, gong == null ? deur : gong, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0f, 0.6f);
        p.sendSystemMessage(tekst("deur.gong").withStyle(ChatFormatting.AQUA));
        E2H1Lucht.titel(p, tekst("deur.titel").withStyle(ChatFormatting.GOLD), tekst("deur.titel_onder").withStyle(ChatFormatting.YELLOW), 60);
    }

    // =====================================================================================================================
    // step 9: Zuka at the pier
    // =====================================================================================================================
    private static void zukaScene(ServerPlayer p, BlockPos steiger) {
        E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_SCHIP, true);
        Ontsnapping.toonSchip(p);
        Cutscenes.speel(p, H1Scenes.ZUKA_STEIGER, steiger, Rotation.NONE, Ontsnapping::start);
    }

    /** The escape reached Appa: the scene geleerd, then the end of the chapter. */
    static void bijAppa(ServerPlayer p) {
        ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
        BlockPos kamp = g == null ? null : E2H1Lucht.plek(g, "appa_kamp");
        E2Audio.stinger(p, "stinger.mijn_eer");
        if (kamp == null || !Cutscenes.speel(p, H1Scenes.GELEERD, kamp, Rotation.NONE, Tempel::afronden)) {
            afronden(p);
        }
    }

    /**
     * The end of chapter 1: the flag {@value E2Verhaal#V_GELEERD}, the line done, the rewards (once), the visible advancement,
     * and Appa's flight to the Noordelijke Waterstam.
     */
    static void afronden(ServerPlayer p) {
        Ontsnapping.stop(p);
        E2H1Lucht.LIJN.vlag(p, E2Verhaal.V_GELEERD, true);
        E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_VLUCHT, false);
        E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_SCHIP, false);
        E2H1Lucht.LIJN.zet(p, E2H1Lucht.STAPPEN);
        if (E2H1Lucht.LIJN.eenmalig(p, "beloning")) {
            Minigames.give(p, new ItemStack(ModItems.clothingItem(GuhClothes.GUHVATAR_LUCHTNOMADE)));
            Minigames.give(p, new ItemStack(E2H1LuchtFeature.LUCHTSCOOTER_ITEM.get(), 2));
            p.sendSystemMessage(tekst("beloning.kreeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            GidsFeature.grant(p, "guheinde/einde2_h1_lucht_geleerd");
        }
        Zicht.kijkOpnieuw(p);
        E2Reizen.vlieg(p, Einde2Ids.NOORDELIJKE_WATERSTAM, E2Reizen.Vlucht.H1_H2, null);
    }

    // =====================================================================================================================
    // the roles of the residents
    // =====================================================================================================================

    /** Aang-guh at the gate: what to do now. */
    static final class AangRol extends QuestRol {
        AangRol() {
            super(E2H1Lucht.LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("praat");
            String key = switch (stap) {
                case E2H1Lucht.KAASBOL, E2H1Lucht.VLUCHT -> "aang.welkom";
                case E2H1Lucht.HAL -> "aang.hal";
                case E2H1Lucht.LUCHTBAL -> "aang.luchtbal";
                case E2H1Lucht.SCOOTER -> "aang.scooter";
                case E2H1Lucht.ZWEVEN -> "aang.zweef";
                case E2H1Lucht.MOMO -> "aang.momo";
                case E2H1Lucht.STAL -> "aang.stal";
                case E2H1Lucht.DEUR -> "aang.deur";
                case E2H1Lucht.ZUKA -> E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_VLUCHT) ? "aang.vlucht" : "aang.steiger";
                default -> "aang.na." + p.getRandom().nextInt(3);
            };
            zeg(p, npc, E2H1Lucht.Q + key);
        }
    }

    /** The memory of monk Guhatso on the taartenterras: the luchtbal match, the track, the rings. */
    static final class GuhatsoRol extends QuestRol {
        GuhatsoRol() {
            super(E2H1Lucht.LIJN);
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            npc.speel("praat");
            switch (stap) {
                case E2H1Lucht.LUCHTBAL -> {
                    if (Luchtbal.bezig(p)) {
                        zeg(p, npc, E2H1Lucht.Q + "guhatso.later");
                        return;
                    }
                    scherm(p, npc, E2H1Lucht.Q + "guhatso.luchtbal", new Praat.Optie(1, E2H1Lucht.P + "optie.klaar"),
                            new Praat.Optie(2, E2H1Lucht.P + "optie.straks"));
                }
                case E2H1Lucht.SCOOTER -> zeg(p, npc, E2H1Lucht.Q + "guhatso.scooter");
                case E2H1Lucht.ZWEVEN -> zeg(p, npc, E2H1Lucht.Q + "guhatso.zweef");
                default -> {
                    if (stap < E2H1Lucht.LUCHTBAL) {
                        zeg(p, npc, E2H1Lucht.Q + "guhatso.vroeg");
                    } else if (p.getRandom().nextInt(3) == 0) {
                        taart(p, npc);
                    } else {
                        zeg(p, npc, E2H1Lucht.Q + "guhatso.later");
                    }
                }
            }
        }

        @Override
        protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
            if (stap == E2H1Lucht.LUCHTBAL && optie == 1) {
                Luchtbal.start(p);
            }
        }
    }

    /** Guhatso throws a pie (only at this player: particles at their feet, nothing hurts). */
    static void taart(ServerPlayer p, @Nullable GuhNpcEntity guhatso) {
        if (guhatso != null) {
            guhatso.speel("gooi_taart");
            GuhQuests.say(p, guhatso, E2H1Lucht.Q + "guhatso.taart");
        }
        ServerLevel g = p.level();
        g.sendParticles(p, new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.CAKE.defaultBlockState()), true, false,
                p.getX(), p.getY() + 1.6, p.getZ(), 24, 0.3, 0.3, 0.3, 0.12);
        g.playSound(null, p, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    /** The spirits of Roku-guh and Kyoshi-guh in front of their statues: the scene hal at step 2, advice after. */
    static final class GeestRol extends QuestRol {
        private final String wie;

        GeestRol(String wie) {
            super(E2H1Lucht.LIJN);
            this.wie = wie;
        }

        @Override
        protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
            if (stap == E2H1Lucht.HAL) {
                ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
                BlockPos anker = g == null ? null : E2H1Lucht.plek(g, "voetstuk_leeg");
                if (anker != null && !Cutscenes.bezig(p)) {
                    Cutscenes.speel(p, H1Scenes.HAL, anker, Rotation.NONE, s -> E2H1Lucht.LIJN.verder(s, E2H1Lucht.HAL));
                }
                return;
            }
            npc.speel("praat");
            p.level().sendParticles(p, ParticleTypes.END_ROD, true, false, npc.getX(), npc.getY() + 1.4, npc.getZ(), 10, 0.3, 0.4, 0.3, 0.01);
            zeg(p, npc, E2H1Lucht.Q + wie + ".raad." + p.getRandom().nextInt(3));
        }
    }

    /** The resident of this kind near this marker of the temple (null: none loaded). */
    @Nullable
    static GuhNpcEntity bewoner(ServerLevel g, String plek, GuhNpcEntity.Kind kind) {
        BlockPos b = E2H1Lucht.plek(g, plek);
        if (b == null) {
            return null;
        }
        return g.getEntitiesOfClass(GuhNpcEntity.class, new AABB(b).inflate(12), e -> e.getKind() == kind && !Tijdelijk.is(e)).stream().findFirst()
                .orElse(null);
    }

    /** A story actor of one player: a sitting guh character (or Mika) that only they see, never saved. */
    static GuhNpcEntity acteur(ServerLevel g, ServerPlayer p, GuhNpcEntity.Kind kind, Vec3 pos, float yaw) {
        GuhNpcEntity e = ModEntities.GUH_NPC.get().create(g, EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        e.setKind(kind);
        e.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        e.setInvulnerable(true);
        Zicht.alleenVoor(e, p.getUUID());
        Tijdelijk.merk(e);
        g.addFreshEntity(e);
        return e;
    }

    // =====================================================================================================================
    // nobody gets hurt in the temple: no fall damage for a player of the story inside it (towers, rings, poles)
    // =====================================================================================================================
    static void opVal(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && E2H1Lucht.LIJN.begonnen(p) && E2H1Lucht.inTempel(p)) {
            event.setCanceled(true);
        }
    }

    /** (logout, dev) forgets the player. */
    static void vergeet(UUID speler) {
        AANGEKOMEN.remove(speler);
        APPA_GEROEPEN.remove(speler);
    }
}
