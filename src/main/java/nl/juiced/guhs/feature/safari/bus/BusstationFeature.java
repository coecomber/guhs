package nl.juiced.guhs.feature.safari.bus;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.safari.SafariDier;
import nl.juiced.guhs.feature.safari.SafariFeature;
import nl.juiced.guhs.feature.safari.lobby.Bezoekers;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.6.0 (F1b): the busstations in the Guhmension and the Guh-safaribus (design 1.2).
 * <ul>
 *   <li>the station is protected like the other guh buildings ({@link Bescherming}); its inhabitants come with
 *   {@link Bezetting}: Chauffeur Toeter, the shop guh (the lobby's giftshop), the stokstaartjes, flamingo's and pinguins
 *   of the mini-enclosures (staying home) and the visitor guhs in safari clothes (who walk back when they stray);</li>
 *   <li>a click on the bus or its bell, or "Instappen!" at the chauffeur: {@link Busrit#instappen};</li>
 *   <li>{@code /guhs safari busstation plaats} (ops) places one where you stand, {@code waar} finds the nearest.</li>
 * </ul>
 */
public final class BusstationFeature {
    /** A visitor guh's persistent data: its spot at the station (BlockPos as long). */
    static final String THUIS = "guhs_safari_bus_thuis";
    static final int BEZOEKER_STRAAL = 10;

    private BusstationFeature() {
    }

    public static void register() {
        BusScenes.registreer();
        // test round 5: the Superkompas finds the nearest busstation (tab Knus, next to the Reisbureau)
        nl.juiced.guhs.item.SuperkompasItem.voegToe("knus", "safari_busstation");
        Bescherming.registreer(Busstation.STRUCTUUR, 2);
        Bezetting.npc("safari_bus_chauffeur", Busstation.STRUCTUUR, null, Busstation.CHAUFFEUR, GuhNpcEntity.Kind.SAFARI_CHAUFFEUR, null, 90f);
        Bezetting.npc("safari_bus_verkoper", Busstation.STRUCTUUR, null, Busstation.VERKOPER, GuhNpcEntity.Kind.SAFARI_VERKOPER, null, 0f);
        for (int i = 0; i < Busstation.DIEREN.size(); i++) {
            Busstation.Dier d = Busstation.DIEREN.get(i);
            Bezetting.wezen("safari_bus_dier_" + i, Busstation.STRUCTUUR, null, new BlockPos(d.x(), Busstation.G + 1, d.z()), (level, plek, draai) -> {
                SafariDier dier = SafariFeature.DIER.get().create(level, EntitySpawnReason.STRUCTURE);
                if (dier == null) {
                    return null;
                }
                dier.zet(d.soort(), d.variant(), BlockPos.containing(plek), Busstation.DIER_STRAAL, -1, -1, 0);
                dier.snapTo(plek.x, plek.y, plek.z, level.getRandom().nextFloat() * 360f, 0f);
                return dier;
            }, 8);
        }
        for (int i = 0; i < Busstation.BEZOEKERS.size(); i++) {
            Bezetting.wezen("safari_bus_bezoeker_" + i, Busstation.STRUCTUUR, null, Busstation.BEZOEKERS.get(i), BusstationFeature::bezoeker, BEZOEKER_STRAAL + 4);
        }
        NpcRollen.zet(GuhNpcEntity.Kind.SAFARI_CHAUFFEUR, CHAUFFEUR);
        NeoForge.EVENT_BUS.addListener(BusstationFeature::opKlik);
        NeoForge.EVENT_BUS.addListener(BusstationFeature::opTick);
        NeoForge.EVENT_BUS.addListener(BusstationFeature::commando);
    }

    /** A visitor guh in safari clothes (Bezoekers' tag: a click gets one of their lines), remembering its spot. */
    static GuhEntity bezoeker(ServerLevel level, Vec3 plek, net.minecraft.world.level.block.Rotation draai) {
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (guh == null) {
            return null;
        }
        guh.setVariant(GuhVariant.roll(level.getRandom()));
        guh.wear(GuhClothes.SAFARI_HOEDJE);
        for (String naam : new String[]{"KAKI_VESTJE", "SAFARI_VESTJE", "VISVEST"}) {
            try {
                guh.wear(GuhClothes.valueOf(naam));
                break;
            } catch (IllegalArgumentException ignored) {
                // not made: the next name
            }
        }
        guh.addTag(Bezoekers.TAG);
        guh.setPersistenceRequired();
        guh.getPersistentData().putLong(THUIS, BlockPos.containing(plek).asLong());
        guh.snapTo(plek.x, plek.y, plek.z, level.getRandom().nextFloat() * 360f, 0f);
        return guh;
    }

    /** A station's visitor that strayed too far is put back at its spot. */
    private static void opTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof GuhEntity g) || (g.tickCount + g.getId()) % 100 != 0 || g.level().isClientSide()) {
            return;
        }
        long thuis = g.getPersistentData().getLongOr(THUIS, Long.MIN_VALUE);
        if (thuis == Long.MIN_VALUE) {
            return;
        }
        BlockPos t = BlockPos.of(thuis);
        if (g.blockPosition().distSqr(t) > BEZOEKER_STRAAL * BEZOEKER_STRAAL) {
            g.getNavigation().stop();
            g.teleportTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5);
        }
    }

    /** The bus or its bell at a station. */
    private static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND || !(p.level() instanceof ServerLevel level)) {
            return;
        }
        Busstation.Oord o = Busstation.oord(level, event.getPos());
        if (o == null || !o.isBus(event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Busrit.instappen(p, o);
    }

    /** Chauffeur Toeter: "Instappen!" or "Nog even niet". */
    static final NpcRole CHAUFFEUR = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            Praat.open(p, npc, null, "gui.guhs.safari.bus.chauffeur.hallo", new Object[0],
                    new Praat.Optie(1, "gui.guhs.safari.bus.chauffeur.instappen"), new Praat.Optie(2, "gui.guhs.safari.bus.chauffeur.straks"));
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
            if (optie == 1 && p.level() instanceof ServerLevel level) {
                Busstation.Oord o = Busstation.oord(level, npc.blockPosition());
                if (o != null) {
                    Busrit.instappen(p, o);
                }
            } else if (optie == 2) {
                p.sendSystemMessage(Component.translatable("gui.guhs.safari.bus.chauffeur.straks.antwoord").withStyle(ChatFormatting.YELLOW));
            }
        }
    };

    private static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("safari").then(Commands.literal("busstation")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("plaats").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    BlockPos pos = BlockPos.containing(src.getPosition());
                    if (!Busstation.plaats(src.getLevel(), pos)) {
                        src.sendFailure(Component.translatable("commands.guhs.safari.busstation.nee"));
                        return 0;
                    }
                    src.sendSuccess(() -> Component.translatable("commands.guhs.safari.busstation.ok", pos.toShortString()), true);
                    return 1;
                }))
                .then(Commands.literal("waar").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    BlockPos pos = BlockPos.containing(src.getPosition());
                    BlockPos waar = Busstation.dichtstbij(src.getLevel(), pos);
                    if (waar == null) {
                        src.sendFailure(Component.translatable("commands.guhs.safari.busstation.geen", 1600));
                        return 0;
                    }
                    int afstand = (int) Math.round(Math.sqrt(waar.distSqr(new BlockPos(pos.getX(), waar.getY(), pos.getZ()))));
                    src.sendSuccess(() -> Component.translatable("commands.guhs.safari.busstation.waar", waar.toShortString(), afstand), false);
                    return 1;
                })))));
    }
}
