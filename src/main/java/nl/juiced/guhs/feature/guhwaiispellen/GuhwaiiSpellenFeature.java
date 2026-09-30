package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Scorebord;

/**
 * 3.0 (Guhverhalen), slice guhwaiispellen: Surfen &amp; hula op Guhwai'i (DESIGN_30 §5). On the surf beach of Guhwai'i
 * (structure guhs:guhwaii_surfstrand, next to the island's beach) Lilo-guh surfs with you ({@link SurfSpel}, at the surf
 * shack: plek "surf") and dances the hula with you ({@link HulaSpel}, on the hula podium: plek "hula"), each makkelijk /
 * medium / lastig with their own world top 3 and Highscores rows; both pay in schelpjesmunten, which Tikiguh takes at his
 * Tiki stall for Tiki decorations ({@link TikiWinkel}). Resources: tools/features/guhwaii_spellen.py (+ _bouw, _modellen),
 * the songs: tools/remix/make_hula.py.
 */
public final class GuhwaiiSpellenFeature {
    /** The player's own surf and hula data (saved, survives dying): records per level, the rows played, first games. */
    public static final String DATA = "guhs_guhwaiispellen";

    /** Lilo-guh at the surf shack. */
    public static final NpcRole LILO_SURF = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            SurfSpel.talk(npc, player);
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            if ((npc.tickCount + npc.getId()) % 100 == 1) {
                toonSurfScores(npc);
            }
        }
    };

    /** Lilo-guh on the hula podium. */
    public static final NpcRole LILO_HULA = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            HulaSpel.talk(npc, player);
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            HulaSpel.of(npc).tick(npc);
        }
    };

    /** Tikiguh at his stall. */
    public static final NpcRole TIKIGUH = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            TikiWinkel.talk(npc, player);
        }

        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            return TikiWinkel.offers();
        }
    };

    public static void register(IEventBus modBus) {
        GuhwaiiSpellenBlocks.BLOCKS.register(modBus);
        GuhwaiiSpellenBlocks.ITEMS.register(modBus);
        GuhwaiiSpellenBlocks.ENTITIES.register(modBus);
        GuhwaiiSpellenBlocks.SOUNDS.register(modBus);
        FunderingProcessor.TYPES.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.LILO_GUH, "surf", LILO_SURF);
        NpcRollen.zet(GuhNpcEntity.Kind.LILO_GUH, "hula", LILO_HULA);
        NpcRollen.zet(GuhNpcEntity.Kind.TIKIGUH, TIKIGUH);
        Minigames.registerGame(SurfSpel.SPEL, SurfSpel::surft);
        Minigames.registerGame(HulaSpel.SPEL, HulaSpel::danst);
        nl.juiced.guhs.feature.Protected.add(SurfstrandBescherming::beschermd);
        NeoForge.EVENT_BUS.register(SurfstrandBescherming.class);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                SurfSpel.tick(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(SurfSpel::opAfstappen);
        NeoForge.EVENT_BUS.addListener(GuhwaiiSpellenCommando::register);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                SurfSpel.opUitloggen(p);
                HulaSpel.opUitloggen(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                SurfSpel.opUitloggen(p);
                HulaSpel.opUitloggen(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
            if (event.getEntity() instanceof Player p && (SurfSpel.surft(p) || HulaSpel.danst(p))
                    && !event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            SurfSpel.vergeetAlles();
            HulaSpel.vergeetAlles();
            RadiootjeBlock.vergeet();
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        GuhwaiiSpellenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GuhwaiiSpellenBlocks.SCHELPJESMUNT.get()));
        output.accept(new ItemStack(GuhwaiiSpellenBlocks.SURFPLANKJE_LEEN.get()));
        GuhwaiiSpellenBlocks.TIKI.forEach(b -> output.accept(new ItemStack(b.get())));
    }

    // --- shared by the games ---------------------------------------------------------------------------------------------

    /** The player's own surf and hula data (in GuhQuests.saved: survives dying). */
    public static CompoundTag data(Player player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains(DATA)) {
            saved.put(DATA, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(DATA);
    }

    /** The data of Lilo-guh's screen: the game (surf / hula), your record and the world's best per level, your coins. */
    public static CompoundTag scherm(ServerPlayer player, String modus) {
        CompoundTag data = new CompoundTag();
        CompoundTag mijn = data(player);
        data.putString("Modus", modus);
        for (Niveau n : Niveau.values()) {
            String rij = (modus.equals("surf") ? "surfen_" : "hula_") + n.id();
            data.putInt("Best_" + n.id(), mijn.getIntOr((modus.equals("surf") ? "Surf_" : "Hula_") + n.id(), 0));
            List<Scorebord.Entry> top = Scorebord.top(player.level().getServer(), rij);
            data.putString("Top_" + n.id(), top.isEmpty() ? "" : top.get(0).name() + " (" + top.get(0).score() + ")");
        }
        data.putBoolean("Played", mijn.getBooleanOr(modus.equals("surf") ? "EersteSurf" : "EersteHula", false));
        data.putInt("Munten", GuhQuests.count(player, GuhwaiiSpellenBlocks.SCHELPJESMUNT.get()));
        return data;
    }

    /** A visible advancement of the Guhverhalen tab (guhs:verhalen/&lt;name&gt;) and its hidden twin for the FTB quests. */
    public static void grant(ServerPlayer player, String name) {
        GidsFeature.grant(player, "verhalen/" + name);
        GuhAdvancements.grant(player, name);
    }

    /** A Highscores row was played (for "all six"): remembered, and the advancement once all six are there. */
    public static void gespeeld(ServerPlayer player, String rij) {
        CompoundTag d = data(player);
        ListTag list = d.getListOrEmpty("Rijen");
        List<String> rijen = new ArrayList<>();
        list.forEach(t -> rijen.add(t.getAsString()));
        if (!rijen.contains(rij)) {
            list.add(StringTag.valueOf(rij));
            d.put("Rijen", list);
            rijen.add(rij);
        }
        if (rijen.size() >= 6) {
            grant(player, "guhwaii_spellen_alle_niveaus");
        }
    }

    public static void titel(ServerPlayer p, Component title, Component sub, int stay) {
        if (p.connection == null) {
            return;
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(4, stay, 10));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** The surf top 3 of the three levels, floating by the surf shack. */
    public static void toonSurfScores(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        List<String> boards = new ArrayList<>();
        List<Component> heads = new ArrayList<>();
        for (Niveau n : Niveau.values()) {
            boards.add("surfen_" + n.id());
            heads.add(Component.translatable("gui.guhs.scorebord.guhwaiispellen.surf", n.naam()));
        }
        Scorebord.show(level, npc.position().add(0, 3.4, 0), "surf", Scorebord.text(level.getServer(),
                Component.translatable("gui.guhs.scorebord.guhwaiispellen.surfen"), boards, heads, n -> n + " ≈"));
    }

    private GuhwaiiSpellenFeature() {
    }
}
