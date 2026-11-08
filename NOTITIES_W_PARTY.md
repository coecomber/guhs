# NOTITIES W-PARTY (branch 110-w-party, vanaf 0f00119f2)

Twee nieuwe dingen voor 1.10.0 (de gebruiker vroeg er zelf om, ondanks "geen nieuwe features" in de W-brief):
1. een **Party-tab** in het Vadsrijk-menu (opgeslagen parties, uitnodigen met toast, leider-knoppen, samen in de wachtrij);
2. **nep-spelers**: fake spelers met gamer-namen, aan/uit te zetten (server + per speler).

## Wat er gebouwd is

### Parties (`kern/party/Partijen` + `PartijenData`)
- **Opgeslagen**: SavedData `guhs:vr_partijen` (overworld). De data houdt een *snapshot* die bij elke partywijziging ververst wordt,
  dus het leegmaken bij ServerStopping kan nooit een lege file opslaan. `PartijenData.laad` bij ServerStarted (in PartyRegistry).
- **Uitloggen = NIET meer de party uit** (KernEvents). Je frame wordt grijs. Logt de leider uit, dan wordt het eerste online lid leider
  (anders blijft hij leider). Uitnodigingen aan wie uitlogt vervallen.
- Nieuw: `weiger`, `verwijder` (leider), `maakLeider` (leider), `uitgelogd`, `maakParty`, NPC-leden (`Party.npcs`), `VERANDERD`- en
  `NPC_WEG`-haken. `Party.leden` bevat ALLEEN echte spelers, dus xp-delen, quests, raid en elke `leden(..)`-aanroeper ziet nooit een NPC.
- Max 5 = spelers + nep-spelers samen. Een party van 1 speler + nep-speler(s) bestaat (en eindigt als de laatste nep-speler weggaat);
  zo'n party wordt niet opgeslagen (nep-spelers overleven een restart niet).
- Iedereen in een party mag nog steeds uitnodigen (zoals het was); alleen de leider kan verwijderen/leider maken/de party in de rij zetten.

### Party-tab (`party/PartyDienst`, `party/client/PartyPaneel`, menu-volgorde 112)
- Links "Mijn party (n/5)": kroon voor de leider, online/offline-stip, Lv, klasse + spec, zone; nep-spelers met `[NPC]` en "nog N min".
  Leider-knoppen per lid: **Leider** / **Weg**. Onderaan **Party verlaten**, en een open uitnodiging met **Accepteer/Weiger**.
- Rechts "Spelers in het Vadsrijk" (dichtbij eerst, met afstand; max 40, bladeren) met **Uitnodigen**; daaronder "Samen in de wachtrij":
  dungeon kiezen (< >) + **Party in rij**, en **Raid: party in de rij** (alleen de leider).
- Uitnodiging: chat met klikbare **[Accepteer] [Weiger]** + een **toast** + belletje. De commando's werken nog steeds.
- Server pusht verse data na elke partywijziging naar wie de tab open heeft; de client vraagt elke 2 s opnieuw zolang de tab open is.
- Commando's (iedereen): `/guhs vadsrijk party` (opent de tab), `uitnodigen <speler>`, `accepteer`, `weiger`, `verlaat`,
  `verwijder <naam>`, `leider <naam>`, `rij <dungeon>`, `raid`, en voor de chatknoppen `nepmee <uuid>`, `nepweg <uuid>`.

### Groepswachtrij (`party/Wachtrijen`, `party/DungeonWachtrij`)
- Dungeon: via de interface `DungeonWachtrij`, die integratie koppelt aan w-grot's `DungeonRij` (zie hieronder). Zonder koppeling toont de
  tab "Dungeons: via de dungeondeur." en vullen nep-spelers niets.
- Raid: `Z6Raid.partyInRij(leden)` (nieuw, gedeeld bestand). Zet de spelers (geen NPC's, max 10) als groep in de raid-rij; is de toren
  vrij, dan wordt de groep meteen geroepen en gaat wie binnen een minuut in De Wachtzaal staat mee naar boven (min. 2) - hetzelfde als
  een cirkelgroep uit de rij.

### Nep-spelers (`party/nep/*`)
- **Uiterlijk**: speler-model met een van de 9 standaard (wide) skins (alex, ari, efe, kai, makena, noor, steve, sunny, zuri; geen
  echte mensen), naambordje `[NPC] naam · Rol · Lv N`, item in de hand per rol (tank zwaard+schild, heler staf, schade boog).
  30 parodie-namen (xX_Vadsjesloper_Xx, KnuffelKoning2009, guhlover_nl, ProGrinder, ...).
- **Chat**: 53 regels (lfg dungeon??, brb mama roept, wie heeft er een heler, gg, ...) boven het hoofd (5 s, naam schuift een regel
  omlaag) en als `[Nep] naam: ...` in de chat van wie binnen 32 blokken is (of in de groep zit) en ze aan heeft.
- **Wild**: elke 30 s per mobzone (geen stad, geen grot/dungeon/raid/vallei/droom/ring/kamp/proeftuin, en minstens 2 Vadsrijk-mobs
  binnen 48 blokken van een speler die ze aan heeft) 25 % kans op een groepje van 2-3 als er nog < 2 zijn; nooit meer dan 3 per zone.
  Ze dwalen rond hun spawnplek (12 blokken), vechten tegen mobs (geen bazen) en gaan na 12-20 min weg, of na 1 min zonder speler
  binnen 96 blokken. Level = willekeurig binnen het zone-level.
- **Rekruteren**: rechtsklik -> "hoi" + chatknop **[Kom je mee?]** -> 20-30 min in je party (party moet ruimte hebben). Daarna een
  afscheidsregel en weg. Je eigen nep-speler rechtsklikken: resterende minuten + **[Doei!]**. De leider kan ze ook verwijderen.
- **AI**: volgen (teleport > 28 blokken), jouw doel aanvallen (de mob die jij het laatst raakte) of wat de groep aanvalt; heler heelt
  elke 1,5 s de zwakste (spelers + nep-spelers) onder 85 % met 12 %; tank trekt elke 3 s mobs die de groep aanvallen naar zich toe
  (boos wolkje). Schade per klap = een deel van de max-Vads van het doel (tank 1/12, heler 1/22, schade 1/7; bazen een tiende).
  Een mob-klap kost een vast deel van hun leven (tank 4 %, heler 7 %, schade 8 %); bij ~0 vallen ze 10 s flauw ("rip") en staan op
  met 60 %. Ze gaan nooit dood, worden nooit opgeslagen, spelers kunnen ze niet raken.
- **Loot**: een klap van een metgezel telt als hulp van ZIJN speler (`kern/mob/NepBijdrage`, nieuw bestand in het kern.mob-package
  voor toegang tot de beschermde bijdrage-maps; VadsrijkMob zelf is niet aangepast). Die speler krijgt dus gewoon xp + persoonlijke loot
  als de nep-speler de laatste klap geeft. De nep-speler krijgt nooit iets. Wilde nep-spelers geven niemand iets.
- **Wachtrij vullen** (`NepVulling`): heeft een DUNGEON-rij echte spelers + lege plekken en kwam er 1 minuut geen nieuwe echte speler
  bij, dan vullen nep-spelers de rij (eerst ontbrekende rollen: tank, heler, dan schade; de klassen van de echte spelers tellen mee).
  Gerekruteerde metgezellen van de spelers in de rij tellen als bezette plek. Nooit in de raid. Bij binnengaan verschijnen ze naast
  de spelers (een speler per nep-speler, om de beurt), zijn metgezel van die speler en gaan weg ("gg") als hun speler 10 s niet meer
  in de dungeon is. Ze komen in de party-frames maar NIET in de party.
- **Aan/uit**:
  - server: config `serverconfig/guhs-vadsrijk-nepspelers.toml` (`nepspelers = true`), of `/guhs vadsrijk nepspeler aan|uit` (slaat
    op). Uit = alle nep-spelers gaan weg, niets spawnt, niemand kan rekruteren, geen vulling.
  - per speler: Vadsrijk-menu > Instellingen > **Nep-spelers** (client-config `guhs-vadsrijk-nepspelers.toml`; gaat naar de server bij
    inloggen en bij elke wijziging). Uit = je ziet ze niet (niet getekend, niet aanklikbaar), geen nep-chat, je kunt ze niet rekruteren,
    je eigen metgezellen gaan naar huis, ze vullen geen rij waar jij in staat, en ze staan niet in jouw frames.

## Koppeling met w-grot (VOOR INTEGRATIE - nog niet gedaan, mijn branch kent DungeonRij niet)
w-grot's API (in zijn NOTITIES, commit 25571ca89) past precies. Maak bij het mergen dit bestand
`src/main/java/nl/juiced/guhs/feature/vadsrijk/party/DungeonRijBrug.java` en roep `DungeonRijBrug.koppel()` aan aan het eind van
`PartyRegistry.register(...)` (of vanuit `Dungeons.register`, als dat later loopt):

```java
package nl.juiced.guhs.feature.vadsrijk.party;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeon;
import nl.juiced.guhs.feature.vadsrijk.dungeon.DungeonRij;
import nl.juiced.guhs.feature.vadsrijk.dungeon.Dungeons;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpelers;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepVulling;

/** W-PARTY x W-GROT: the party tab + nep-spelers on w-grot's dungeon finder. */
public final class DungeonRijBrug implements DungeonWachtrij {
    public static void koppel() {
        Wachtrijen.DUNGEON = new DungeonRijBrug();
        DungeonRij.BIJ_START.add((in, spelers, mee) -> NepVulling.binnen(in.dungeon, spelers, mee.stream().map(DungeonRij.Metgezel::id).toList()));
        NepSpelers.STAND_HAAK.add((p, aan) -> DungeonRij.zetGeenNpcVulling(p, !aan));
    }
    @Override public List<String> dungeons() { return Dungeons.alle().stream().map(Dungeon::id).toList(); }
    @Override public Component naam(String d) { return DungeonRij.dungeonNaam(d); }
    @Override @Nullable public Component partyInRij(ServerPlayer leider, List<ServerPlayer> leden, String d) {
        return DungeonRij.zetGroepInRij(leider, d) ? null : Component.empty();   // (DungeonRij already told the reason)
    }
    @Override @Nullable public String rijVan(UUID id) { return DungeonRij.rijVan(id); }
    @Override public Map<String, List<UUID>> rijen() {
        Map<String, List<UUID>> m = new LinkedHashMap<>();
        DungeonRij.RIJ.forEach((d, r) -> m.put(d, new ArrayList<>(r)));
        return m;
    }
    @Override public int groepMax() { return DungeonRij.GROEP_MAX; }
    @Override public boolean bezet(String d) { return DungeonRij.BEZET.containsKey(d) || DungeonRij.CHECKS.containsKey(d); }
    @Override public boolean isNep(UUID id) { return DungeonRij.METGEZELLEN.containsKey(id) || DungeonRij.NEP.containsKey(id); }
    @Override public boolean voegNepToe(String d, List<NepLid> nep) {
        boolean ok = false;
        for (NepLid l : nep) ok |= DungeonRij.voegMetgezelToe(d, new DungeonRij.Metgezel(l.id(), l.naam(), l.rol().id()));
        return ok;
    }
    @Override @Nullable public String dungeonVan(UUID id) { return DungeonRij.dungeonVan(id); }
}
```
Let op bij het koppelen:
- `in.dungeon` is het veld van `DungeonRij.Instantie` (public final String). `Dungeon::id` bestaat (interface).
- w-grot telt metgezellen mee voor de schaal (`groepsGrootte`) en geeft ze geen loot/clear/cooldown: klopt met mijn kant.
- `zetGroepInRij` zet ALLE online partyleden in de rij (ook buiten het Vadsrijk -> `weiger` meldt dat). Prima.
- Gerekruteerde nep-spelers lopen hun speler achterna (teleport > 28 blokken, zelfde dimensie) en komen dus vanzelf mee de dungeon in.
  w-grot's `uitzetten` kijkt (bij mijn lezing) alleen naar spelers; als hij ook andere entities buiten zet: `NepSpeler` uitzonderen.
- De dev-vulling: `/guhs vadsrijk nepspeler vul` werkt pas na de koppeling.
- w-grot's eigen `vadsrijk.dungeon.msg.nep.buiten`-meldingen gelden alleen zijn dev-"Testguh"s; mijn vullers gaan via `METGEZELLEN`.

## Gedeelde bestanden (gemarkeerd `// W-PARTY`)
- `VadsrijkFeature.java`: +2 regels (PartyRegistry.register / payloads).
- `client/VadsrijkClient.java`: +1 regel (PartyClient.init, vóór MenuClient).
- `kern/KernEvents.java`: uitloggen roept `Partijen.uitgelogd` i.p.v. `Partijen.verlaat` (parties blijven bestaan).
- `kern/VadsrijkCommando.java`: het party-blok (3 subcommando's) vervangen door `PartyDienst.commando()`; 3 ongebruikte imports weg.
- `kern/party/Partijen.java`: grotendeels herschreven (API blijft: party, partyVan, zelfdeParty, ledenInBuurt, leden, nodigUit,
  accepteer, verlaat, wisAlles, Party.leider/leden). Nieuw bestand `kern/party/PartijenData.java`, nieuw `kern/mob/NepBijdrage.java`.
- `hud/PartyFrames.java`: `EXTRA`-haak (NPC-frames na de leden; houdt de frames ook aan zonder party), `spec(..)` public.
- `zone6/raid/Z6Raid.java`: `partyInRij(List<ServerPlayer>)` erbij (30 regels, verder niets veranderd).
- `menu/client/InstellingenPaneel.java`: +1 optie "Nep-spelers" (AanUit op `NepClientConfig.AAN`).
- lang `nl_nl.json` / `en_us.json`: +123 sleutels (en = Nederlands; bestaande Engelse teksten niet overschreven), via
  `tools/vadsrijk/w_party_lang.py` (los te draaien; overschrijft in nl de waardes van deze sleutels). `vadsrijk.kern.party.uitnodiging`
  kreeg in nl een nieuwe tekst (verwijst naar de tab); wordt verder niet meer gebruikt.

## Beslissingen
- Parties via de bestaande Partijen (niet een tweede systeem); NPC's apart in `Party.npcs` zodat niets bestaands per ongeluk een NPC
  als speler ziet.
- Acties in de tab gaan via een eigen payload (`PartyPayloads.Actie`), niet via chatcommando's (geen chat-spam); commando's houden
  dezelfde regels (`PartyDienst`).
- Server-uit via een server-config i.p.v. een gamerule: per wereld, met commando om te wisselen.
- Skins: alleen vanilla standaard-skins (wide model, zodat één PlayerModel volstaat).
- Nep-spelers tellen voor de party-max (5), niet voor xp-delen.
- Wilde nep-spelers vallen geen bazen aan (zouden kills "stelen"); metgezellen wel (tegen een tiende schade).

## Tests
`runGameTestServer -Pgt=VadsrijkParty` (6 tests) + `VadsrijkUHud` (party-frames, ongewijzigd groen moeten blijven). Zie eindrapport
voor de uitkomst.

## Open punten
- Niet in een client bekeken (geheugen): de tab-layout op GUI-schaal 4, de skins/naambordjes/chatregel boven het hoofd, de toast.
- De koppeling met w-grot (zie boven) moet integratie doen; daarna `/guhs vadsrijk nepspeler vul` en een echte rij met 1 speler testen.
- De AI is bewust simpel (geen pathfinding rond hoge muren behalve vanilla; ze kunnen niet zwemmen-ontwijken). In smalle grotten kan
  de schade-NPC soms zonder zicht staan (dan schiet hij niet).
- Wilde-spawnregels (25 % / 30 s / max 3) zijn een eerste gok: als de gebruiker ze te vaak of te weinig ziet, staan de getallen in
  `NepSpelers` (`WILD_ELKE`, de 0.25 en `MAX_PER_GEBIED`).
