## U-HEKKEN: hekken, muurtjes, tralies, kettingen en vlaggetjes zijn verbonden

**Wat is er veranderd?** Overal in het Vadsrijk (zone 1 t/m 5) zijn hekken, muurtjes, ijzeren tralies, glaspanelen,
kettingen, vadstouw, guh-hekjes, vlaggetjes en feestslingers nu **verbonden lijnen** in plaats van losse paaltjes.

- **Schuine stukjes in een hekrij** (bijvoorbeeld een ronde reling) krijgen een hoekpaaltje. Dat komt aan de buitenkant,
  boven de afgrond, zodat het looppad vrij blijft.
- **Een trapje van 1 omhoog** krijgt een verhoogd paaltje, zodat de reling doorloopt.
- **Twee losse lantaarnpalen** schuin naast elkaar blijven los. Dat hoort zo.

Kijk overal of het hek een **doorlopende lijn** is (de planken lopen van paal naar paal), ook precies op de tegelgrenzen
(om de 64 blokken).

**Uitkijkpunt in de kruin (zone 3, `uitkijk_nest`):** de reling rondom moet één ring zijn.

```
/guhs vadsrijk indeling plek uitkijk_nest
```

**Wenteltrap in de boom en het Honingtak-platform (zone 3):** de relingen langs de trap en rond het platform zijn
doorlopend. Loop de trap op en af: er staat nergens een paaltje óp de trap.

```
/execute in guhs:vadsrijk run tp @s 1383 168 -519
/execute in guhs:vadsrijk run tp @s 1425 168 -542
/guhs vadsrijk indeling plek uitkijk_kroon
/guhs vadsrijk indeling plek uitkijk_westtak
```

**Vlaggetjes en feestslingers (zone 2, Splinterpieken):** de slingers hangen als één lint tussen de palen, niet dwars.

```
/execute in guhs:vadsrijk run tp @s 698 104 -1043
/execute in guhs:vadsrijk run tp @s 824 109 -1058
/execute in guhs:vadsrijk run tp @s 585 104 -951
```

**Tralies en glas (zone 2 en zone 3):** de tralies zijn een dicht rooster, geen losse staafjes.

```
/execute in guhs:vadsrijk run tp @s 817 115 -1123
/execute in guhs:vadsrijk run tp @s 1640 193 -866
```

**Lampionkust (zone 4):** vlaggetjes, slingers en vadstouw lopen als één lijn.

```
/execute in guhs:vadsrijk run tp @s 1848 74 -452
/execute in guhs:vadsrijk run tp @s 1876 70 -478
/execute in guhs:vadsrijk run tp @s 1998 73 -600
```

**Hoogvads (zone 5):** de muurtjes langs de wegen sluiten aan. Hoge stukjes onder een blok zijn "tall".

```
/execute in guhs:vadsrijk run tp @s 2851 259 -564
/execute in guhs:vadsrijk run tp @s 2680 226 -555
```

**Knuffelkamp en Zwamzoet (zone 1):** de hekken rond de kampen en tuintjes zijn verbonden.

```
/execute in guhs:vadsrijk run tp @s -51 129 -32
/execute in guhs:vadsrijk run tp @s 49 124 -43
/execute in guhs:vadsrijk run tp @s -525 85 -39
```

**Door code geplaatste stukken:**
- de Rustende Ridders-zaal;
- de puzzels;
- de Z5-proefarena: de hekring rond de proefarena is nu één ring, met de vier open doorgangen;
- terugzetten na een baasgevecht.

Bij al deze stukken zijn hekken en muurtjes ook meteen verbonden.

**Als je toch nog een los paaltje ziet:** noteer de coördinaten (F3). De controle
`python tools/vadsrijk/audit/verbindcheck.py` hoort 0 te geven.
