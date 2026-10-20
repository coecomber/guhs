package nl.juiced.guhs.feature.safari.lobby;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * The aaihoek chores (design 1.7): feeding (every animal has a preference that changes every real day; the verzorger
 * hints at it), brushing (a brush: hearts) and walking the alpaca on a lead around its loop. Every chore pays
 * {@link #LOON} safarimunten, at most {@link #PER_DAG} chores a day; the right food is one coin extra. Each chore can be
 * done once a day per animal; a wrong food is refused ("bah!") and costs nothing.
 */
public final class Aaihoek {
    public static final int LOON = 15, PER_DAG = 4;
    public static final List<String> DIEREN = List.of("geitje", "alpaca", "cavia", "ezeltje");

    /** The two foods of every animal, in order (one of them is today's favourite). */
    public static final Map<String, List<String>> VOER = Map.of(
            "geitje", List.of("wortel", "hooi"),
            "alpaca", List.of("appel", "hooi"),
            "cavia", List.of("komkommer", "paprika"),
            "ezeltje", List.of("wortel", "suikerklontje"));

    /** A walk in progress: the alpaca, its own home, and the next point of the loop. */
    private record Wandeling(UUID alpaca, BlockPos thuis, int straal, int zone, int volgende, long begin) {
    }

    private static final Map<UUID, Wandeling> WANDELINGEN = new ConcurrentHashMap<>();
    private static final long WANDEL_MAX = 20L * 60 * 4;

    public static boolean isAaihoek(String soort) {
        return DIEREN.contains(soort);
    }

    /** Which food this item is ("wortel", "hooi"...), null for anything else. */
    @Nullable
    public static String voerVan(ItemStack s) {
        if (s.is(Items.CARROT)) {
            return "wortel";
        }
        if (s.is(Items.WHEAT) || s.is(Items.HAY_BLOCK)) {
            return "hooi";
        }
        if (s.is(Items.APPLE)) {
            return "appel";
        }
        if (s.is(Items.SUGAR)) {
            return "suikerklontje";
        }
        if (s.is(LobbyItems.KOMKOMMER.get())) {
            return "komkommer";
        }
        if (s.is(LobbyItems.PAPRIKA.get())) {
            return "paprika";
        }
        return null;
    }

    public static Item item(String voer) {
        return switch (voer) {
            case "wortel" -> Items.CARROT;
            case "hooi" -> Items.WHEAT;
            case "appel" -> Items.APPLE;
            case "suikerklontje" -> Items.SUGAR;
            case "komkommer" -> LobbyItems.KOMKOMMER.get();
            default -> LobbyItems.PAPRIKA.get();
        };
    }

    /** Today's favourite food of this animal (the same for everyone, changes every real day). */
    public static String favoriet(String soort, long dag) {
        List<String> v = VOER.get(soort);
        if (v == null) {
            return "";
        }
        int h = Long.hashCode(dag * 31 + soort.hashCode() * 17L);
        return v.get(Math.floorMod(h ^ (h >>> 7), v.size()));
    }

    public static String favoriet(String soort) {
        return favoriet(soort, Klok.dag());
    }

    /**
     * A chore is done: pays when it is the first time today for this chore and the day's cap is not reached.
     * Returns the coins paid (0: done before today, or the cap was reached).
     */
    public static int klusGedaan(MinecraftServer server, UUID speler, String klus, boolean bonus) {
        if (SafariSpeler.telDag(server, speler, "aaihoek:" + klus, 1, 1) == 0) {
            return -1;     // this chore was done already today
        }
        net.minecraft.server.level.ServerPlayer sp = server.getPlayerList().getPlayer(speler);
        if (sp != null) {
            nl.juiced.guhs.feature.safari.quests.SafariQuests.aaihoekKlusje(sp);   // S4: the FTB quest "Aaien maar!"
        }
        int loon = SafariMunten.verdienDagelijks(server, speler, "aaihoek", LOON, LOON * PER_DAG, "aaihoek");
        if (loon > 0 && bonus) {
            loon += SafariMunten.verdienDagelijks(server, speler, "aaihoek_bonus", 1, PER_DAG, "aaihoek");
        }
        return loon;
    }

    // --- interactions ----------------------------------------------------------------------------------------------------

    static void opInteractie(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof SafariDier dier) || !isAaihoek(dier.soort())) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND) {
            if (event.getLevel().isClientSide()) {
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            event.setCanceled(true);
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        ItemStack hand = p.getMainHandItem();
        String voer = voerVan(hand);
        if (voer != null) {
            voeren(p, dier, hand, voer);
        } else if (hand.is(Items.BRUSH)) {
            borstelen(p, dier);
        } else if (hand.is(Items.LEAD) && dier.soort().equals("alpaca") && !dier.isBaby()) {
            begin(p, dier);
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.aaihoek.tip." + dier.soort()).withStyle(ChatFormatting.YELLOW));
            dier.speelActie("blij", 30);
        }
    }

    private static void voeren(ServerPlayer p, SafariDier dier, ItemStack hand, String voer) {
        ServerLevel level = (ServerLevel) p.level();
        String soort = dier.soort();
        if (!VOER.get(soort).contains(voer)) {
            // not its food at all
            bah(p, dier, Component.translatable("gui.guhs.safari.aaihoek.bah.niet", Component.translatable("entity.guhs.safari." + soort)));
            return;
        }
        if (!voer.equals(favoriet(soort))) {
            bah(p, dier, Component.translatable("gui.guhs.safari.aaihoek.bah.vandaag", Component.translatable("entity.guhs.safari." + soort)));
            return;
        }
        if (!p.getAbilities().instabuild) {
            hand.shrink(1);
        }
        dier.speelActie("eet", 40);
        level.sendParticles(ParticleTypes.HEART, dier.getX(), dier.getY() + dier.getBbHeight() + 0.3, dier.getZ(), 5, 0.3, 0.2, 0.3, 0);
        level.playSound(null, dier.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.8f, 1.3f);
        loon(p, "voer_" + soort, true, Component.translatable("gui.guhs.safari.aaihoek.smul", Component.translatable("entity.guhs.safari." + soort)));
    }

    private static void bah(ServerPlayer p, SafariDier dier, Component tekst) {
        ServerLevel level = (ServerLevel) p.level();
        dier.speelActie(dier.soort().equals("alpaca") ? "spuug" : "idle", 30);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, dier.getX(), dier.getY() + dier.getBbHeight() + 0.3, dier.getZ(), 2, 0.2, 0.1, 0.2, 0);
        level.playSound(null, dier.blockPosition(), geluid(dier.soort()), SoundSource.NEUTRAL, 0.8f, 0.7f);
        p.sendOverlayMessage(tekst.copy().withStyle(ChatFormatting.GOLD));
    }

    private static void borstelen(ServerPlayer p, SafariDier dier) {
        ServerLevel level = (ServerLevel) p.level();
        dier.speelActie("blij", 50);
        level.sendParticles(ParticleTypes.HEART, dier.getX(), dier.getY() + dier.getBbHeight() + 0.3, dier.getZ(), 4, 0.4, 0.2, 0.4, 0);
        level.playSound(null, dier.blockPosition(), geluid(dier.soort()), SoundSource.NEUTRAL, 0.8f, 1.4f);
        loon(p, "borstel_" + dier.soort(), false, Component.translatable("gui.guhs.safari.aaihoek.geborsteld", Component.translatable("entity.guhs.safari." + dier.soort())));
    }

    private static void loon(ServerPlayer p, String klus, boolean bonus, Component blij) {
        int loon = klusGedaan(p.level().getServer(), p.getUUID(), klus, bonus);
        if (loon < 0) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.aaihoek.al_gedaan").withStyle(ChatFormatting.GRAY));
        } else if (loon == 0) {
            p.sendOverlayMessage(blij.copy().append(" ").append(Component.translatable("gui.guhs.safari.aaihoek.genoeg")).withStyle(ChatFormatting.YELLOW));
        } else {
            p.sendOverlayMessage(blij.copy().withStyle(ChatFormatting.GREEN));
        }
    }

    private static SoundEvent geluid(String soort) {
        return switch (soort) {
            case "geitje" -> SoundEvents.GOAT_AMBIENT;
            case "alpaca" -> SoundEvents.LLAMA_AMBIENT;
            case "ezeltje" -> SoundEvents.DONKEY_AMBIENT;
            default -> SoundEvents.RABBIT_AMBIENT;
        };
    }

    // --- walking the alpaca ------------------------------------------------------------------------------------------

    private static void begin(ServerPlayer p, SafariDier alpaca) {
        if (WANDELINGEN.containsKey(p.getUUID())) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.aaihoek.wandel.al").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (Wandeling w : WANDELINGEN.values()) {
            if (w.alpaca().equals(alpaca.getUUID())) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.safari.aaihoek.wandel.bezet").withStyle(ChatFormatting.GRAY));
                return;
            }
        }
        WANDELINGEN.put(p.getUUID(), new Wandeling(alpaca.getUUID(), alpaca.thuis(), alpaca.straal(), alpaca.zoneIndex(), 0, p.level().getGameTime()));
        alpaca.speelActie("blij", 40);
        p.sendSystemMessage(Component.translatable("gui.guhs.safari.aaihoek.wandel.begin").withStyle(ChatFormatting.GOLD));
    }

    /** Every 10 ticks: the alpaca follows its walker; the loop's points are passed in order; back at the pen = done. */
    static void tick(MinecraftServer server) {
        if (WANDELINGEN.isEmpty() || server.getTickCount() % 10 != 0) {
            return;
        }
        for (Map.Entry<UUID, Wandeling> e : WANDELINGEN.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Wandeling w = e.getValue();
            ServerLevel level = p == null ? null : (ServerLevel) p.level();
            SafariDier alpaca = level != null && level.getEntity(w.alpaca()) instanceof SafariDier d ? d : null;
            if (p == null || alpaca == null || p.distanceTo(alpaca) > 18 || level.getGameTime() - w.begin() > WANDEL_MAX) {
                stop(server, e.getKey(), w, alpaca, p, false);
                continue;
            }
            alpaca.zetThuis(p.blockPosition(), 2, -1);
            if (alpaca.distanceTo(p) > 3 && alpaca.getNavigation().isDone()) {
                alpaca.getNavigation().moveTo(p, 1.2);
            }
            List<BlockPos> lus = LobbyPlekken.get().alpacaLus;
            if (w.volgende() < lus.size()) {
                BlockPos doel = lus.get(w.volgende());
                if (Math.hypot(p.getX() - doel.getX() - 0.5, p.getZ() - doel.getZ() - 0.5) < 3.5) {
                    Wandeling verder = new Wandeling(w.alpaca(), w.thuis(), w.straal(), w.zone(), w.volgende() + 1, w.begin());
                    e.setValue(verder);
                    p.sendOverlayMessage(Component.translatable("gui.guhs.safari.aaihoek.wandel.stap", verder.volgende(), lus.size()).withStyle(ChatFormatting.YELLOW));
                }
                level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, false, doel.getX() + 0.5, doel.getY() + 1, doel.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
            } else if (Math.hypot(p.getX() - w.thuis().getX(), p.getZ() - w.thuis().getZ()) < 6) {
                stop(server, e.getKey(), w, alpaca, p, true);
            }
        }
    }

    private static void stop(MinecraftServer server, UUID speler, Wandeling w, @Nullable SafariDier alpaca, @Nullable ServerPlayer p, boolean klaar) {
        WANDELINGEN.remove(speler);
        if (alpaca != null) {
            alpaca.zetThuis(w.thuis(), w.straal(), w.zone());
            alpaca.getNavigation().moveTo(w.thuis().getX() + 0.5, w.thuis().getY(), w.thuis().getZ() + 0.5, 1.0);
        }
        if (p == null) {
            return;
        }
        if (klaar) {
            if (alpaca != null) {
                alpaca.speelActie("blij", 60);
                ((ServerLevel) p.level()).sendParticles(ParticleTypes.HEART, alpaca.getX(), alpaca.getY() + 2, alpaca.getZ(), 6, 0.4, 0.3, 0.4, 0);
            }
            loon(p, "alpaca_wandeling", false, Component.translatable("gui.guhs.safari.aaihoek.wandel.klaar"));
        } else {
            p.sendOverlayMessage(Component.translatable("gui.guhs.safari.aaihoek.wandel.weg").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The verzorger's hint about today's favourite foods ("iets oranjes", "iets groens"...). */
    static Component hint() {
        net.minecraft.network.chat.MutableComponent c = Component.empty();
        for (int i = 0; i < DIEREN.size(); i++) {
            String soort = DIEREN.get(i);
            if (i > 0) {
                c.append("\n");
            }
            c.append(Component.translatable("gui.guhs.safari.aaihoek.hint", Component.translatable("entity.guhs.safari." + soort),
                    Component.translatable("gui.guhs.safari.aaihoek.voer." + favoriet(soort))));
        }
        return c;
    }

    private Aaihoek() {
    }
}
