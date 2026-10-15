package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.mikas.EnderMikaEntity;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.GuhTime;

/**
 * einde2 zij: the Leegtehaven (DESIGN 4) besides the pub (reizen) and the shipyard ({@link VetbootjeOpdracht}):
 * <ul>
 *   <li>the market: each Ender-Mika trader behind a stall ({@code Handelaar}) offers {@link #PER_KRAAM} trades that change
 *       every day ({@link #aanbod}), at most {@link #MAX_PER_DAG} times a day per player and offer;</li>
 *   <li>the ferry De Vadsige Golf: the veerman-guh takes you, quick, to any place of the Guheinde you know (unlocked story
 *       structures and discovered buildings: {@link E2Verhaal#bekendeGebouwen}) and to the main island. Free.</li>
 * </ul>
 */
public final class Leegtehaven {
    static final String Q = Zij.Q + "haven.";
    public static final int PER_KRAAM = 3, MAX_PER_DAG = 4, PER_PAGINA = 2;
    static final String HANDEL = "guhs_einde2_zij_handel", VEER_SLEUTEL = "einde2_zij_veer";
    /** The main island as a destination of the ferry. */
    public static final String HOOFDEILAND = "hoofdeiland";

    /** One trade of the pool: what you give (item, count, optionally a second), what you get. */
    record Ruil(String geef, int n, @Nullable String geef2, int n2, String krijg, int k) {
    }

    static final List<Ruil> POOL = List.of(
            new Ruil("guhs:kaas_knabbels", 12, null, 0, "guhs:flitskruimel", 2),
            new Ruil("guhs:kaas_knabbels", 8, null, 0, "guhs:doosschil", 1),
            new Ruil("guhs:flitskruimel", 2, null, 0, "minecraft:ender_pearl", 1),
            new Ruil("guhs:kaas_knabbels", 16, null, 0, "guhs:knabbelkoorvrucht", 4),
            new Ruil("guhs:kristalkaas_brokje", 6, null, 0, "guhs:kristalkaas", 1),
            new Ruil("guhs:kaas_knabbels", 10, null, 0, "guhs:lichtstaafje", 3),
            new Ruil("guhs:mika_traan", 1, null, 0, "guhs:kaas_knabbels", 6),
            new Ruil("guhs:kaas_knabbels", 20, null, 0, "guhs:kruimelkwal_pot", 1),
            new Ruil("guhs:kaas_knabbels", 8, null, 0, "guhs:kruimelwolk", 4),
            new Ruil("guhs:doosschil", 2, "guhs:kaas_knabbels", 8, "guhs:schuildoos", 1),
            new Ruil("guhs:kaas_knabbels", 24, null, 0, "guhs:guh_kristal", 1),
            new Ruil("guhs:gepofte_knabbelkoorvrucht", 5, null, 0, "guhs:flitskruimel", 2),
            new Ruil("guhs:schimmelpaddenstoel", 4, null, 0, "guhs:kaas_knabbels", 6),
            new Ruil("guhs:kaas_knabbels", 6, null, 0, "minecraft:chorus_fruit", 4));

    /** (not saved) the ferry page each player looks at. */
    private static final Map<UUID, Integer> PAGINA = new ConcurrentHashMap<>();

    private Leegtehaven() {
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.VEERGUH, Einde2Ids.LEEGTEHAVEN + "/veerman", new VeerRol());
    }

    // =====================================================================================================================
    // the market
    // =====================================================================================================================

    /** (PlayerInteractEvent.EntityInteract) a click on a trading Ender-Mika: its stall of the day. */
    static void opKlik(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EnderMikaEntity mika) || !mika.isHandelaar() || !(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        Kraam kraam = new Kraam(p, mika);
        kraam.openTradingScreen(p, Component.translatable(Q + "kraam"), 1);
        Zij.geluid(p, SoundEvents.ENDERMAN_AMBIENT, 0.3f, 1.6f);
    }

    /** The number of a trader: its block position, so it keeps its stall. */
    static int nummer(EnderMikaEntity mika) {
        return Math.floorMod(mika.getBlockX() * 31 + mika.getBlockZ() * 17 + mika.getBlockY() * 7, 1 << 16);
    }

    /** The trades of this trader on this day (the same for every player). */
    static List<Ruil> aanbod(long dag, int nummer) {
        RandomSource r = RandomSource.create(dag * 341873128712L + nummer * 132897987541L);
        List<Ruil> pool = new ArrayList<>(POOL);
        List<Ruil> uit = new ArrayList<>();
        for (int i = 0; i < PER_KRAAM && !pool.isEmpty(); i++) {
            uit.add(pool.remove(r.nextInt(pool.size())));
        }
        return uit;
    }

    @Nullable
    static Item item(String id) {
        Identifier rid = Identifier.tryParse(id);
        return rid == null || !BuiltInRegistries.ITEM.containsKey(rid) ? null : BuiltInRegistries.ITEM.getValue(rid);
    }

    /** One player's view of one stall: the offers of today, with what this player already traded today. */
    static final class Kraam implements Merchant {
        private final ServerPlayer klant;
        private final EnderMikaEntity mika;
        private final int nummer;
        private final long dag;
        private MerchantOffers aanbod;
        @Nullable
        private Player handelaar;

        Kraam(ServerPlayer klant, EnderMikaEntity mika) {
            this.klant = klant;
            this.mika = mika;
            this.nummer = nummer(mika);
            this.dag = GuhTime.day(klant.level());
            this.aanbod = maak();
        }

        private MerchantOffers maak() {
            MerchantOffers o = new MerchantOffers();
            CompoundTag gedaan = vandaag();
            List<Ruil> ruilen = aanbod(dag, nummer);
            for (int i = 0; i < ruilen.size(); i++) {
                Ruil r = ruilen.get(i);
                Item geef = item(r.geef()), krijg = item(r.krijg());
                Item geef2 = r.geef2() == null ? null : item(r.geef2());
                if (geef == null || krijg == null || (r.geef2() != null && geef2 == null)) {
                    continue;
                }
                int uses = gedaan.getIntOr(nummer + "_" + i, 0);
                o.add(new MerchantOffer(new ItemCost(geef, r.n()), geef2 == null ? Optional.empty() : Optional.of(new ItemCost(geef2, r.n2())),
                        new ItemStack(krijg, r.k()), uses, MAX_PER_DAG, 0, 0f));
            }
            return o;
        }

        private CompoundTag vandaag() {
            CompoundTag t = GuhQuests.saved(klant).getCompoundOrEmpty(HANDEL);
            if (t.getLongOr("Dag", -1L) != dag) {
                t = new CompoundTag();
                t.putLong("Dag", dag);
                GuhQuests.saved(klant).put(HANDEL, t);
            }
            return t;
        }

        @Override
        public void setTradingPlayer(@Nullable Player p) {
            this.handelaar = p;
        }

        @Nullable
        @Override
        public Player getTradingPlayer() {
            return handelaar;
        }

        @Override
        public MerchantOffers getOffers() {
            return aanbod;
        }

        @Override
        public void overrideOffers(MerchantOffers offers) {
            this.aanbod = offers;
        }

        @Override
        public void notifyTrade(MerchantOffer offer) {
            offer.increaseUses();
            int i = aanbod.indexOf(offer);
            if (i >= 0) {
                CompoundTag t = vandaag();
                t.putInt(nummer + "_" + i, offer.getUses());
                GuhQuests.saved(klant).put(HANDEL, t);
            }
            nl.juiced.guhs.quest.GuhAdvancements.grant(klant, "einde2_zij_handel");
            klant.level().playSound(null, mika.blockPosition(), SoundEvents.ENDERMAN_AMBIENT, net.minecraft.sounds.SoundSource.NEUTRAL, 0.4f, 1.7f);
        }

        @Override
        public void notifyTradeUpdated(ItemStack stack) {
        }

        @Override
        public int getVillagerXp() {
            return 0;
        }

        @Override
        public void overrideXp(int xp) {
        }

        @Override
        public boolean showProgressBar() {
            return false;
        }

        @Override
        public SoundEvent getNotifyTradeSound() {
            return SoundEvents.ENDERMAN_AMBIENT;
        }

        @Override
        public boolean isClientSide() {
            return false;
        }

        @Override
        public boolean stillValid(Player p) {
            return mika.isAlive() && p.distanceToSqr(mika) < 8 * 8;
        }
    }

    // =====================================================================================================================
    // the ferry
    // =====================================================================================================================

    /** Where the ferry can take this player: the places they know (with a landing place) and the main island. */
    static List<String> bestemmingen(ServerPlayer p) {
        List<String> uit = new ArrayList<>();
        for (String s : E2Verhaal.bekendeGebouwen(p, false)) {
            if (!s.equals(Einde2Ids.LEEGTEHAVEN) && Einde2Ids.LEEGTE_STRUCTUREN.contains(s) && Plekken.plek(s, "aankomst") != null) {
                uit.add(s);
            }
        }
        uit.add(HOOFDEILAND);
        return uit;
    }

    static final class VeerRol implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.speel("kaartje");
            PAGINA.put(p.getUUID(), 0);
            scherm(p, npc);
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
            List<String> b = bestemmingen(p);
            int pagina = PAGINA.getOrDefault(p.getUUID(), 0);
            if (optie == 1) {
                PAGINA.put(p.getUUID(), (pagina + 1) * PER_PAGINA >= b.size() ? 0 : pagina + 1);
                scherm(p, npc);
            } else if (optie >= 100 && optie - 100 < b.size()) {
                Praat.sluit(p);
                vaar(p, b.get(optie - 100));
            }
        }
    }

    private static void scherm(ServerPlayer p, GuhNpcEntity npc) {
        List<String> b = bestemmingen(p);
        int pagina = PAGINA.getOrDefault(p.getUUID(), 0);
        List<Praat.Optie> opties = new ArrayList<>();
        for (int i = pagina * PER_PAGINA; i < Math.min(b.size(), (pagina + 1) * PER_PAGINA); i++) {
            opties.add(new Praat.Optie(100 + i, b.get(i).equals(HOOFDEILAND) ? Q + "veer.hoofdeiland" : "structure.guhs." + b.get(i)));
        }
        if (b.size() > PER_PAGINA) {
            opties.add(new Praat.Optie(1, Q + "veer.meer"));
        }
        Praat.open(p, npc, null, Q + (pagina == 0 ? "veer.welkom" : "veer.verder"), new Object[0], opties.toArray(new Praat.Optie[0]));
    }

    /** Sails this player, quick, to a destination (the arrival place of its guaranteed copy, or the main island). */
    static boolean vaar(ServerPlayer p, String waar) {
        ServerLevel guheinde = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
        if (guheinde == null) {
            return false;
        }
        Vec3 doel;
        if (waar.equals(HOOFDEILAND)) {
            int y = guheinde.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GuheindeGevecht.ARRIVAL_X, GuheindeGevecht.ARRIVAL_Z);
            doel = new Vec3(GuheindeGevecht.ARRIVAL_X + 0.5, Math.max(y, guheinde.getMinY() + 64), GuheindeGevecht.ARRIVAL_Z + 0.5);
        } else {
            if (!Plekken.bekend(guheinde, waar)) {
                Zij.melding(p, Q + "veer.mist");
                return false;
            }
            doel = Plekken.aankomst(guheinde, waar);
        }
        if (doel == null) {
            Zij.melding(p, Q + "veer.mist");
            return false;
        }
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false));
        p.teleportTo(guheinde, doel.x, doel.y, doel.z, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
        p.resetFallDistance();
        nl.juiced.guhs.quest.GuhAdvancements.grant(p, "einde2_zij_veer");
        Zij.geluid(p, SoundEvents.BELL_BLOCK, 0.7f, 1.4f);
        Zij.geluid(p, SoundEvents.BOAT_PADDLE_WATER, 1f, 0.8f);
        p.sendSystemMessage(Component.translatable(Q + "veer.aan", waar.equals(HOOFDEILAND) ? Component.translatable(Q + "veer.hoofdeiland")
                : Component.translatable("structure.guhs." + waar)).withStyle(ChatFormatting.AQUA));
        return true;
    }
}
