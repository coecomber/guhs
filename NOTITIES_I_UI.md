# NOTITIES_I_UI (fix round 3, agent I-UI, branch 110-i-ui, from ab6d952ac)

## What changed

| Feedback | Fix | Files |
|---|---|---|
| Makelaar Guhrrit came into the Vadsrijk | **Central gate** `kern/Buitenstaanders` (registered from `KernEvents.register`): (1) `EntityTravelToDimensionEvent` into `guhs:vadsrijk` is cancelled for every non-player entity that does not belong (it stays where it is, nothing is lost); (2) `EntityJoinLevelEvent` in the Vadsrijk level refuses such entities (fresh spawns and old saves). "Belongs" = players, every vanilla/other-mod entity, Guhs types whose id starts with `vr_`, classes in `feature.vadsrijk`, and `Buitenstaanders.TOEGESTAAN` (now: `guh_vis`, the Sfeer pond fish). So the makelaar, the Postguh, the Sherlock cast, guhs from home, followers etc. can't appear. | `kern/Buitenstaanders` (new), `kern/KernEvents` (1 line) |
| (same, at the source) | `Guhrrit.verschijn` returns null in the Vadsrijk. The makelaar's letter (`Makelaarsbrief.magNu`) is never delivered in the Vadsrijk: it waits until you are out. Makelaar postcards / Opa's kelder post (`bezorgPost`) in the Vadsrijk: no Postguh, and the item goes with the stored home inventory (new `Inventaris.legThuis`), coming out on the way home. "Roep naar mij" (Roepen) and the Guhkamer call are refused in the Vadsrijk with "Je guhs blijven lekker thuis…". The Libelkonijnguh, Sponsguh and Gerrit followers wait outside (no churn of re-creating). Momo does not hop onto your shoulder from another dimension while you are inside, and a shoulder Momo is not drawn in the Vadsrijk. | `erfenis/sherlock/Guhrrit`, `erfenis/koppeling/Makelaarsbrief`, `kern/Inventaris`, `band/Roepen`, `guhkamer/Guhkamer`, `einde2/geestwezens/Libelkonijnguh`, `einde2/sponsguh/Maatjes`, `einde2/appa/MomoGuhEntity`, `einde2/appa/client/E2AppaClient` |
| Pet nameplate "entity.guhs.vr_huisdier" | No nameplate at all: `RenderNameTagEvent.CanRender` -> FALSE for `HuisdierEntity` + `shouldShowName` false. `getTypeName` gives the pet's own name ("Mini Koning Klodder") for any other place that shows a name. Lang `entity.guhs.vr_huisdier` = "Huisdier". | `huisdieren/HuisdierEntity`, `huisdieren/client/HuisdierClient`, `tools/features/vadsrijk_huisdieren.py`, lang |
| Travel map close button | A ✕ (16x16, top right in the title band) with a "Sluiten" tooltip and click sound; closes the map. | `npc/client/ReiskaartScherm`, `tools/features/vadsrijk_npc.py` (`vadsrijk.npc.reiskaart.sluit`), lang |
| Tabs between the collection screens | New `client/CollectieTabs`: tab flaps above the panel (between the guh ears), the active one joined to the pink title band. Rijdieren (10) and Huisdieren (20) are registered; the old "Huisdieren" button in the mount screen is gone (replaced by the tab). Not shown at the stalmeester (stal mode). `EcoScherm` draws/handles the tabs when a subclass returns a `collectieTab()` id, and moves the panel down by half the tab height. | `client/CollectieTabs` (new), `economie/client/EcoScherm`, `RijdierScherm`, `RijdierClient`, `huisdieren/client/HuisdierScherm`, `HuisdierClient` |

## For i-voltooiing (zone completion tracker): the tab API
```java
// client init, once:
CollectieTabs.registreer("voltooiing", 30, () -> Component.translatable("..."), VoltooiingClient::open);
// a screen extending economie's EcoScherm:
@Override protected String collectieTab() { return "voltooiing"; }
// any other Screen: keep CollectieTabs.HOOGTE (16) px free above the panel, then
CollectieTabs.teken(g, font, left, top, w, "voltooiing", mouseX, mouseY);           // when drawing
if (CollectieTabs.klik(event.x(), event.y(), left, top, w, "voltooiing")) return true; // first in mouseClicked
```
The `open` runnable runs on the client when the tab is clicked from another tab; usually it sends the "open" packet and the
server's screen replaces the current one. The order number: lower = more to the left (rijdieren 10, huisdieren 20).

## Not changed (checked)
- Everything else I found that spawns near players is bound to its own dimension/structure (E2Team, SnoesVerhaal, Kermis, KaasMaan,
  Bevers, Ritten...). Whatever slips through still meets the central gate.
- A Guhs entity that is NOT `vr_` but should be in the Vadsrijk later: add its path to `Buitenstaanders.TOEGESTAAN`.

## Tests
See the end of this file.

## Test results (2026-11-04)
- `runGameTestServer -Pgt=VadsrijkBuitenstaanders`: **passed** (`makelaarBlijftBuiten`: type rules; the makelaar NPC refused in the
  Vadsrijk dim and fine at home; players always allowed; `Inventaris.legThuis` post waits with the home inventory and comes out on
  the way home). The gametest server has **no `guhs:vadsrijk` level**, so the in-dimension part of the test (real spawn refused,
  teleport in cancelled, Roepen refused, no Postguh) is skipped there and only runs where the level exists.
- No AutoCheck client run (every gradle slot was busy with other agents). **Check in the dev client:** pet has no nameplate;
  travel map ✕ closes it; Rijdieren (N) / Huisdieren (P) tabs switch both ways and look right above the pink panel; no makelaar/Postguh
  in the Vadsrijk.

## Resume point
All 4 items done and committed on 110-i-ui. Open: the visual check above.
