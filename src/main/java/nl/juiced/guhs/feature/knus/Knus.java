package nl.juiced.guhs.feature.knus;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The shared framework of 2.8 "Knuffeldal" (package feature.knus): seasons ({@link Seizoen}), parts of the day
 * ({@link Dagdeel}), the Knus tab of the Guhdex ({@link KnusVoortgang}), the Grote Knusfeest ({@link Feesttaak},
 * {@link Knusfeest}), hooks into every guh ({@link GuhHooks}, client: {@code client.GuhRenderHooks}), signals
 * ({@link KnusSignalen}), tags ({@link KnusTags}) and the plein slots ({@link PleinSlot}).
 * Registered first of all 2.8 features (Features.register).
 */
public final class Knus {
    /** The sections of the Knus tab, in tab order (one per 2.8 feature). */
    public static final List<String> ONDERDELEN = List.of("knuffeldal", "seizoenen", "bakkerij", "creche", "theehuis", "kapper",
            "boerderij", "tuintjes", "sterrenwacht", "ballon", "kamperen", "knuffelbad", "wereldleven",
            "piep");   // 2.8.1: Piep registers its own section (PiepVoortgang), last

    public static void register(IEventBus modBus) {
        // the sections, each with an icon (items of a slice that doesn't exist yet: a stand-in)
        KnusVoortgang.onderdeel("knuffeldal", icoon("knus_oorkonde", Items.CAKE));
        KnusVoortgang.onderdeel("seizoenen", icoon("seizoensbloembak", Items.CHERRY_SAPLING));
        KnusVoortgang.onderdeel("bakkerij", icoon("bakmunt", Items.BREAD));
        KnusVoortgang.onderdeel("creche", icoon("speenmunt", Items.PINK_BED));
        KnusVoortgang.onderdeel("theehuis", icoon("feest_theeservies", Items.FLOWER_POT));
        KnusVoortgang.onderdeel("kapper", icoon("krulmunt", Items.SHEARS));
        KnusVoortgang.onderdeel("boerderij", icoon("pluiswol", Items.WHITE_WOOL));
        KnusVoortgang.onderdeel("tuintjes", icoon("guh_gieter", Items.POPPY));
        KnusVoortgang.onderdeel("sterrenwacht", icoon("wensster", Items.SPYGLASS));
        KnusVoortgang.onderdeel("ballon", icoon("mini_luchtballon", Items.PHANTOM_MEMBRANE));
        KnusVoortgang.onderdeel("kamperen", icoon("guh_slaapzak", Items.CAMPFIRE));
        KnusVoortgang.onderdeel("knuffelbad", icoon("eendjesmunt", Items.WATER_BUCKET));
        KnusVoortgang.onderdeel("wereldleven", icoon("guh_fluitje", Items.NOTE_BLOCK));
        NeoForge.EVENT_BUS.addListener(Seizoen::onServerTick);
        NeoForge.EVENT_BUS.addListener(Seizoen::onServerStarted);
        NeoForge.EVENT_BUS.addListener(Seizoen::onServerStopped);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> PleinSlot.testWissen());
        NeoForge.EVENT_BUS.addListener(Knus::onLogin);
        NeoForge.EVENT_BUS.addListener(Knus::onRespawn);
        NeoForge.EVENT_BUS.addListener(Knus::registerCommands);
    }

    public static void payloads(PayloadRegistrar registrar) {
        KnusPayloads.register(registrar);
    }

    /** An icon by registry id (guhs:&lt;id&gt;), or the stand-in while that item doesn't exist. */
    public static Supplier<ItemStack> icoon(String id, Item standIn) {
        return () -> {
            Item item = BuiltInRegistries.ITEM.get(Guhs.id(id));
            return new ItemStack(item == Items.AIR ? standIn : item);
        };
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Seizoen.sync(player);
            KnusVoortgang.sync(player);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Seizoen.sync(player);
        }
    }

    /** /guhs seizoen: tells the season; /guhs seizoen &lt;lente|zomer|herfst|winter&gt; (op): that season starts now. */
    private static void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> seizoen = Commands.literal("seizoen").executes(c -> {
            var level = c.getSource().getLevel();
            Seizoen s = Seizoen.huidig(level);
            c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.seizoen.nu", s.naam(), Seizoen.dagInSeizoen(level) + 1, Seizoen.DAGEN)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            return 1;
        });
        for (Seizoen s : Seizoen.values()) {
            seizoen.then(Commands.literal(s.id()).requires(source -> source.hasPermission(2)).executes(c -> {
                Seizoen.zet(c.getSource().getServer(), s);
                c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.seizoen.gezet", s.naam()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return 1;
            }));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(seizoen));
    }

    private Knus() {
    }
}
