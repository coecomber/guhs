# Guhs 1.2.0 - Dutch -> English glossary

This glossary is **binding** for every translator. If a word is in here, use exactly this English form,
in every key, quest, sign, book and dialogue. If a word is not in here, build it from the parts that are
(knabbel = nibble, vads = chonk, knuffel = snuggle, ...), and keep it consistent with what's already here.

---

## 1. STYLE

**Tone.** Cute, cozy, silly, kid-friendly. Same energy as the Dutch: short sentences, lots of
exclamations, guh-words sprinkled in. Never sarcastic or mean. Mikas are grumpy, never scary.

**How guhs talk.** Guhs are happy little chonks. They say *Guh!*, *Nyeg!* and *WAHOOG!* a lot, use
diminutives ("little", "-ie", "-y": *tummy, pawsies, ears*), and love words like *cozy, snuggly, chonky*.
Keep the guh-word where the Dutch has one; don't add three more. Mikas say *NYEG!* angrily and laugh
*Nyeh-heh-heh!*.

**Spelling.** American English: *color, favorite, behavior, cozy, gray, center, armor, theater,
neighbor, practice*. (Minecraft's own en_us also says *Armor*.) Ukulele, not ukelele.

**Capitalization.**
- Item, block, entity, biome, structure, effect and enchant names: **Title Case**
  (*Wahoog Chonk Pickaxe*, *Bucket of Cheese Sauce*, *Potted Cheese Flower*). Small words stay lowercase
  inside a name (*of, the, in, a, and, on*) unless first.
- Character names: as in the tables (*Mayor Chonkworth*, *Grandpa Guh*).
- In running text, generic nouns stay lowercase: *a guh, two cheese nibbles, your guhbelly*;
  proper places/characters stay capitalized: *Snuggledale, the Guh End, Overlord Mika*.
- Shouts stay in caps exactly where the Dutch shouts: *VAHOEG!* -> *WAHOOG!*, *NJEG!* -> *NYEG!*.

**Placeholders and codes - never touch.** Keep `%s`, `%1$s`, `%2$s`... (you may reorder positional ones
if English word order needs it, but every placeholder must still be there exactly once). Keep `§x`/`&x`
color codes, `&r`, `\\&` (escaped ampersand in FTB Quests), `\n`, `<3`, `♥`, `★`, `♪`, `✔`, `⚠`, arrows,
`{...}`, JSON escapes and `[links]` exactly as-is. Keep keybind letters (W/A/S/D, 1-8, A-K) as they are.

**Punctuation.** Use straight quotes `'` `"` like the Dutch source does. Ellipsis = three dots `...`.
Dashes: the source uses ` - ` (space hyphen space); keep that, no em dashes. Exclamation marks: match the
Dutch count (`!!!` stays `!!!`).

**Puns.** Translate the *joke*, not the words. If the Dutch puns on a Dutch idiom, swap in an English
idiom with the same feel (*"Boer zoekt Guh"* -> *"Farmer Wants a Guh"*). If there is no good English pun,
write something cute and guh-flavored instead; never leave a pun-shaped hole or a literal translation that
makes no sense. Pop-culture parodies keep their parody (Balto, Mewtwo, Lilo & Stitch, Elfstedentocht,
Sherlock, Bob the Builder, Finding Nemo seagulls, Saturday Night Fever...).

**Length.** Keep names short enough for the UI: an item name should not be much longer than the Dutch.
**Signs:** max ~15 characters per line (each sign line is its own key) - rephrase to fit, never let a
line overflow. Scoreboard/HUD/button strings: keep within ~120% of the Dutch length.

**Plurals & possessives.** *Mika* -> plural *Mikas* (no apostrophe; Dutch *Mika's* = plural).
Possessive *Mika's* only where it really is "of Mika" (*Mika's Biome*, *Mika's Grease*).
*guh* -> *guhs*. *Guhdex* has no plural.

**Diminutives.** Dutch *-tje/-je* (guhtje, babyguhtje, muisje) -> *little ...*, *-ie*, or *-ling*:
*guhtje* = "little guh" in prose, *guhling* in names, songs and titles. Don't stack them
("little guhling" is too much).

**Spawn eggs, buckets, borrowed items.** Follow vanilla English: *Guh Spawn Egg*, *Mika Spawn Egg*;
*Bucket of Cheese Sauce*, *Bucket of Guhfish*; "(geleend)" -> "(Borrowed)"; "X in pot" -> *Potted X*.
Stairs/slabs/walls/fences/gates/doors/trapdoors: *Chonkwood Stairs*, *Cheese Rind Brick Slab*, etc.

**Things that NEVER change:** guh, guhs, Guh! (sound), Guhdex, Guhmension (Dutch: Guhmensie), Mika (the
name; titles around it are translated), GeckoLib/mod names, Minecraft vanilla names (use the official
en_us name: Nether, End, Overworld, Ender Dragon, Sniffer...), Experiment 626, Ohana, Aloha.

---

## 2. Core guh words, slang & sounds

| Dutch | English | note |
|---|---|---|
| guh / guhs | guh / guhs | never translated |
| Guh! / Guh guh! / Guhhh... | Guh! / Guh guh! / Guhhh... | the guh sound, unchanged |
| guhtje | little guh / guhling | see Style: diminutives |
| babyguhtje / babyguh | baby guh / guhling | *Babyguhtjes terugbrengen* = *Bringing Back Baby Guhs* |
| njeg / Njeg! / NJEG! | nyeg / Nyeg! / NYEG! | happy guh word; ANGRY when a Mika says it |
| Njeg-njeg / njeh-heh-heh | Nyeg-nyeg / nyeh-heh-heh | Mika laugh = *Nyeh-heh-heh!* |
| vahoeg / VAHOEG! | wahoog / WAHOOG! | the happiest word ("wahoo" + guh) |
| vahoege (adj.) | wahoog | *vahoege vads* = *wahoog chonk*; never "wahooge" |
| supervahoeg | super wahoog | |
| vahoegste | most wahoog / wahoogest | pick what reads best; *Vahoegste van de Zeskamp* = *Wahoogest of the Hexathlon* |
| Vahoegheid | Wahoogness | *Drankje van Vahoegheid* = *Potion of Wahoogness* |
| onvahoeg (effect) | Unwahoog | |
| vads (noun/interj.) | chonk / Chonk! | "lazy and round"; *Vads!* = *Chonk!*; *Slaap lekker vads* = *Sleep chonky-tight* |
| vadsig | chonky | the best compliment for a guh |
| vadsige / vadsiger / vadsigst | chonky / chonkier / chonkiest | |
| vadsigheid | chonkiness | *Vadsigheid-scanner* = *Chonkiness Scanner* |
| vadsen / rondvadsen | to chonk / chonk around | menu *Rondvadsen: %s* = *Chonk around: %s* |
| gevadst / OPPER-GEVADST! | chonked / OVER-CHONKED! | Overlord Mika's defeat cry |
| vadsjes (lieve vadsjes) | chonkies | only in the heart-level phrases |
| lieve vadsige guhs | sweet chonky guhs | the mod's tagline |
| knabbel / knabbels | nibble / nibbles | everywhere, also in compounds |
| kaasknabbel(s) | cheese nibble(s) | generic, lowercase in prose |
| Kaas Knabbels (item) | Cheese Nibbles | the original item name |
| knabbelen | to nibble | |
| knuffel (hug) / knuffelen | cuddle / hug / to cuddle | the act |
| knuffel (plush toy) | plushie | *Guhknuffel* = *Guh Plushie* |
| Knuffel- (in place/proper names) | Snuggle- | *Knuffeldal* = *Snuggledale* |
| knus / Knus (tab, superkompas) | cozy / Cozy | |
| gezellig (effect Gezellig) | snug / Snug | |
| pluis / pluisje / pluizig | fluff / fluffy / fluffy | |
| zielsguh | soulguh | soulmate pun |
| guhwoord (geheim guhwoord) | guh word (secret guh word) | the secret word is NYEG (starts with an N, keep that!) |
| Blub / blub-njeg | Blub / blub-nyeg | water critter sound |
| Piep piep! | Squeak squeak! | |
| Mjam! / Nom nom | Nom! / Nom nom | |
| Hatsjoe! / Hatsjoe-njeg! | Achoo! / Achoo-nyeg! | |
| Gak! (Boris the goose) | Honk! | |
| Auuuhoe! | Awooo! | Baltoguh howl |
| PLOF! / Plons! | FLOMP! / Splash! | |
| Hup hup! | Hup hup! / Go go! | *Hup, Baltoguh!* = *Go, Baltoguh!* |
| Doei! / Doei-doei! | Bye-bye! | |
| Tingeling! | Ding-a-ling! | |
| Hihihi! | Hee hee hee! | |
| Snuf snuf / Snuffel! | Sniff sniff / Sniff! | |
| Oei! | Oopsie! | |
| Wiee! / WIEEEEE! | Wheee! / WHEEEEE! | |
| Brrr | Brrr | |
| Tokkel tokkel | Strum strum | ukulele |
| Kwaak / Kwak | Ribbit / Quack | frog / duck |
| Tjiep | Tweet | |
| Oehoe | Hoo-hoo | |
| Ssst / SSST | Shh / SHHH | |
| zieli / gewoon zieli | sadsie / just sadsie | Piep chapter catch-word for pitiful |
| beef / beef bijgelegd | beef / beef squashed | Shelly drama, keep "beef" |
| besties / bestie-moment | besties / bestie moment | |
| bff 5evr <3 | bff 5evr <3 | keep exactly |

## 3. Heart levels, friendship & guh menu

| Dutch | English | note |
|---|---|---|
| op weg naar lieve vadsjes... | on the way to sweet chonkies... | level 0 |
| lieve vadsjes van elkaar | sweet chonkies together | level 1 |
| mega lieve vadsjes van elkaar | mega sweet chonkies together | level 2 |
| zielsguh bff 5evr <3 | soulguh bff 5evr <3 | level 3 (max) |
| Lieve vadsjes (FTB chapter) | Sweet Chonkies | |
| hartjes | hearts | |
| guhmenu | guh menu | |
| Kledingkast | Wardrobe | |
| Kleertjes / Kleding | Clothes | *Kleertjes uit* = *Clothes Off* |
| pakje (minigame outfit set) | outfit | *het Mika-jagerpakje* = *the Mika Hunter outfit* |
| Rugzak & harnas | Backpack & Harness | |
| Aantrekken! | Dress Up! | |
| Dobbel! | Roll the Dice! | |
| Karakter | Personality | |
| Speels / Lui / Vadsig / Verlegen | Playful / Lazy / Chonky / Shy | |
| Dapper / Kletskous / Knuffelig / Nieuwsgierig | Brave / Chatterbox / Cuddly / Curious | |
| Gedrag: Passief (vluchten) / Passief / Neutraal / Agressief | Behavior: Passive (Flee) / Passive / Neutral / Aggressive | |
| Aanvalsstraal | Attack Radius | |
| Guhgeluidjes | Guh Noises | |
| Zitten / Opstaan | Sit / Stand Up | |
| Zwaartekracht | Gravity | |
| Dagboekje | Diary | |
| Wist-je-datjes | Did-You-Knows | |
| Eerste keren | Firsts | |
| Favorietjes / favorietje | Faves / fave | |
| Lievelings- (hapje/plek/knuffel/liedje/speeltje/emote/kleur/kunstje) | Favorite (Snack/Spot/Plushie/Song/Toy/Emote/Color/Trick) | |
| Beste guh-vriendje / vriendjes | Best Guh Buddy / buddies | |
| Klusjes | Chores | |
| Guhhuisje | Guh House | the guh-head house; *Villa Vahoeg* = *Villa Wahoog* |
| Bewoners | Residents | |
| Speeltje / speelgoed | Toy / toys | |
| Logeren / logeerguhs | Sleepover / sleepover guhs | |
| Guhkamer | Guh Room | guest room in your Guhbelly |
| Guhbel | Guh Bell | |
| Mijn guhs | My Guhs | Guhdex tab |
| Emotes / Lievelingsemote | Emotes / Favorite Emote | |
| Op slot | Locked | |
| Ontgrendeld | Unlocked | |
| Rechtsklik(ken) | Right-click | |
| aaien / aaitje / Je aait %s! | pet / a pet / You pet %s! | 1.2.0: a short tap on your own guh is only petting |
| Welke taal wil je voor Guhs? / Automatisch | Which language for Guhs? / Automatic | 1.2.0 first-join question; language button label *Lang: %s* |

## 4. Emotes

| Dutch | English | note |
|---|---|---|
| Zwaaien | Wave | |
| Dansen | Dance | |
| Slapen | Sleep | |
| VAHOEG-sprong | WAHOOG Jump | |
| Rollen | Roll | |
| Smakken | Munch | |
| Verlegen | Shy | |
| Gapen | Yawn | |
| Zingen | Sing | |
| Knuffelen | Cuddle | |
| Hartjes | Hearts | |
| Knuffeldansje | Snuggle Dance | |
| Bff-knuffel | BFF Hug | |
| Verdrietje | Sniffles | |
| Ukelele | Ukulele | 626-guh only |

## 5. Guhdex & UI terms

| Dutch | English | note |
|---|---|---|
| Guhdex | Guhdex | never translated |
| Zeldzaamheid | Rarity | |
| Gewoon / Ongewoon / Zeldzaam / Heel zeldzaam | Common / Uncommon / Rare / Very Rare | |
| Legendarisch / Uniek | Legendary / Unique | |
| Vaak / Zielig / Stiekem | Common / Pitiful / Sneaky | other rarity words |
| Getemd / Nog niet gezien | Tamed / Not seen yet | |
| Beloningen / Ophalen / Opgehaald | Rewards / Claim / Claimed | |
| Mijlpalen / Verzamelingen | Milestones / Collections | |
| Nog niet ontdekt | Not discovered yet | |
| Highscores / Scorebord / Serverrecord / Wereldrecord | High Scores / Scoreboard / Server Record / World Record | |
| makkelijk / medium / lastig / moeilijk | easy / medium / hard / hard | difficulty levels |
| snelste ronde | fastest lap | |
| Superkompas / Guhmensie-superkompas | Super Compass / Guhmension Super Compass | |
| Avontuur / Wonderen / Wonen / Ondergrond / Einde | Adventure / Wonders / Homes / Underground / End | Super Compass categories |
| Klassiekers | Classics | minigame group |
| De Grote Guhspelen | The Great Guh Games | minigame group |
| Knuffeldal-spelletjes | Snuggledale Games | |
| Guhverhalen / Verhalen | Guh Tales / Tales | stories tab |
| Eerdere avonturen | Earlier Adventures | |
| Bezocht / Nog niet bezocht | Visited / Not visited yet | |
| Kaasregen / Vadsparade / Sterrenregen | Cheese Shower / Chonk Parade / Star Shower | world events |
| Seizoenen: Lente/Zomer/Herfst/Winter | Seasons: Spring/Summer/Fall/Winter | American "Fall" |
| Ochtend / Dag / Middagdutje / Avond / Nacht | Morning / Day / Nap Time / Evening / Night | |
| Bovenwereld | Overworld | vanilla name |
| Maaginstellingen | Belly Settings | |
| Openbaar / Privé / Whitelist | Public / Private / Whitelist | |
| Guhs-instellingen / Clientinstellingen | Guhs Settings / Client Settings | |
| Taal: Automatisch / NL / EN | Language: Auto / NL / EN | the new switch |

## 6. Dimensions & biomes

| Dutch | English | note |
|---|---|---|
| Guhmensie | Guhmension | never translated |
| Guhmaag (dimension/biome) | Guhbelly | your pocket dimension inside a giant guh |
| guhmaag (your own) | guhbelly | lowercase in prose |
| Het Guheinde | The Guh End | endgame dimension |
| De Guhbarbecuether | The Guhbarbecuether | already an English pun, keep |
| Barbecuether | Barbecuether | |
| Guhvelden | Guh Fields | |
| Knabbelkruimels | Nibble Crumbs | |
| Roze pluisjes | Pink Puffs | |
| Kaasvlakte | Cheese Flats | |
| Guhweides | Guh Meadows | |
| Guhpieken | Guh Peaks | |
| Vadskliffen | Chonk Cliffs | |
| Mika's bioom | Mika's Biome | |
| Guhzee / Diepe Guhzee | Guh Sea / Deep Guh Sea | |
| Guhkristalmijn | Guh Crystal Mine | |
| Gatenkaasgrotten | Holey Cheese Caves | holey/holy pun |
| Kaasmoeras | Cheese Swamp | |
| Vadswoud | Chonkwood Forest | wood itself = Chonkwood |
| Houtskoolvlakte | Charcoal Flats | |
| Satébos | Satay Grove | |
| Worstenwoud | Sausage Woods | |
| Asdal | Ash Vale | |
| Rookdelta | Smoke Delta | |
| Knuffeldal | Snuggledale | |
| Guhpolder | Guh Polder | keep "polder" (Dutch-landscape nod for the Elfstedentocht parody) |
| Sneeuwguhtoendra | Snowguh Tundra | |
| Guhwai'i | Guhwai'i | Hawaii pun works in English |
| Nomguhpieken | Nomguh Peaks | |

## 7. Structures & places

| Dutch | English | note |
|---|---|---|
| Guhportaal / kaasknabbelportaal | Guh Portal / cheese nibble portal | |
| Guhgrotten / Uitdagende guhgrotten | Guh Caves / Challenging Guh Caves | |
| hamsterbuizen | hamster tubes | |
| Huis van Boze Mika | Evil Mika's House | |
| Vadsig-heiligdom | Chonky Shrine | Mother Chonky lives here |
| Mika-kamp | Mika Camp | |
| Guh-picknick | Guh Picnic | |
| Sleehut | Sled Hut | |
| Verstopguhhuis | Hide-and-Guh House | |
| Guhkermis / kermis | Guh Fair / fair | |
| Guhkasteel | Guh Castle | |
| Guhland | Guhland | theme park, keep |
| Guhramide | Guhramid | |
| Guhgypter (adv.) | Guhgyptian | "Walk like a Guhgyptian" |
| Guhstandbeeld | Guh Statue | |
| (Grote) Kaasfontein | (Grand) Cheese Fountain | |
| Guhdorp / guhdorpelingen | Guh Village / guh villagers | |
| Hamsterhuis / Groot hamsterhuis / Hamsterstad | Hamster House / Big Hamster House / Hamster Town | |
| Guh Beauty Theater | Guh Beauty Theater | |
| Guhracebaan | Guh Racetrack | |
| Mika-mephal | Whack-a-Mika Hall | |
| Guhdisco | Guh Disco | |
| Guhgolfbaan | Guh Mini Golf | |
| Vadsig eetfestijn | Chonky Food Fest | |
| Guhvisvijver | Guhfish Pond | |
| Zwevende guh-eilandjes | Floating Guh Islets | |
| Kaasmijn | Cheese Mine | |
| Guhbibliotheek / bieb | Guh Library / library | |
| Guhbubbel | The Guh Bubble | underwater dome |
| Knabbelkelder | Nibble Cellar | portal to the Guh End |
| Mika-vesting | Mika Fortress | |
| Terugpoort | Return Gate | |
| Stille Voorraadkelder | Silent Pantry | |
| Verlaten kaasmijnschacht | Old Cheese Mineshaft | (shortened: fits the Super Compass button) |
| Paalhut van de Moerasheks | Swamp Witch's Stilt Hut | |
| Boomhutdorp | Treehouse Village | |
| Barbecueput | Barbecue Pit | |
| Spiesburcht | Skewer Keep | |
| Mika-grillpaleis | Mika Grill Palace | |
| Knuffeldal(-stadje) | Snuggledale (town) | |
| Knabbelbakkerij | Nibble Bakery | |
| Knuffelcreche | Snuggle Daycare | |
| Knabbelthee-huisje / theehuis | Nibble Tea House / tea house | |
| Knip & Vads | Snip & Chonk | hair salon |
| Beroepenstraat | Career Street | |
| brandweerkazerne / ziekenhuisje | fire station / little hospital | |
| Guhboerderij | Guh Farm | |
| Guhtuintjes | Guh Gardens | |
| Guh-Sterrenwacht | Guh Observatory | |
| Ballonfestival | Balloon Festival | |
| Kampeerplekje(s) | Campsite(s) | |
| Knuffelbad | Snuggle Pool | |
| Het Sjoelhuisje | The Shuffle House | |
| Het Guhdoolhof | The Guh Maze | |
| De Knabbelkatapult | The Nibble Catapult | |
| De Knabbelspelen | The Nibble Games | |
| Kaasknabbel-nest | Cheese Nibble Nest | |
| De Elf-Guhjestocht | The Eleven Guhtowns Tour | Elfstedentocht nod |
| Het Guh-Circuit / Pitpaleis | The Guh Circuit / Pit Palace | |
| Nomguh | Nomguh | Nome + nom, keep |
| berghut | mountain hut | Balto story |
| Het kloon-eiland | Clone Island | |
| Het Hemelkapelletje | The Cloud Chapel | |
| Het paalhuisje van Lilo en Nani | Lilo and Nani's Stilt House | |
| guh-asiel | guh shelter | |
| De neergestorte capsule | The Crashed Capsule | |
| Het surfstrand van Guhwai'i | Guhwai'i Surf Beach | |

**Eleven Guhtowns** (Elfstedentocht villages; keep the Frisian-town parody, swap only Dutch slang parts):
Guhwarden -> **Guhwarden** · Snuh -> **Snuh** · IJlguh -> **Ijlguh** · Knabbelsloten -> **Nibblesloten** ·
Vadsvoren -> **Chonkvoren** · Guhdeloopen -> **Guhdeloopen** · Vadskum -> **Chonkum** ·
Knabbelsward -> **Nibblesward** · Guhlingen -> **Guhlingen** · Franeguh -> **Franeguh** · Dokguh -> **Dokguh**.
"It giet oan!" (Frisian) stays as-is: the famous Elfstedentocht phrase.

## 8. Characters & NPCs

| Dutch | English | note |
|---|---|---|
| Mika / Mika's | Mika / Mikas | name kept; plural no apostrophe |
| Grote Mika | Big Mika | |
| Boze Mika | Evil Mika | |
| Mika-baas | Mika Boss | |
| Opper-Mika | Overlord Mika | Guh End boss |
| Nether-Mika | Nether Mika | |
| Moerasheks-Mika | Swamp Witch Mika | |
| Kruimel-Mika | Crumb Mika | |
| Vonk-Mika | Spark Mika | |
| Knekel-Mika | Skelly Mika | |
| Aangebrande Mika | Burnt Mika | |
| Heg-Mika | Hedge Mika | |
| Steele-Mika | Steele Mika | Balto parody, keep "Steele" |
| Voorraadmika | Pantry Mika | |
| Mika-larfje | Mika Grub | |
| Knabbeldief | Nibble Thief | |
| Mika-pikker | Mika Snatcher | |
| Moeder Vadsig | Mother Chonky | |
| Guhbert | Guhbert | her baby, keep |
| Oerguh | Firstguh | the very first guh |
| Hongerige Guh | Hungry Guh | |
| Tandarts-guh | Dentist Guh | |
| Maagenzym-guh | Enzyme Guh | |
| Slee-guh | Sled Guh | |
| Kermis-guh | Fair Guh | |
| Verstopguhtje | Hidey Guh | |
| Tipguh | Hint Guh | |
| Reisguh | Travel Guh | |
| Poortwachter | Gatekeeper | |
| Koningguh / Zijne Vadsigheid | King Guh / His Chonkiness | |
| Showguh | Show Guh | |
| Juf Vadsma / Meneer Glitterguh / Oma Knabbel | Miss Chonksley / Mr. Glitterguh / Granny Nibbles | beauty jury |
| Raceguh | Race Guh | |
| Mepguh | Whack Guh | |
| DJ-guh | DJ Guh | |
| Golfguh | Golf Guh | |
| Smulguh | Munch Guh | |
| Visguh | Fishing Guh | |
| Mijnguh | Miner Guh | |
| Bibliothecaris | The Librarian | |
| Zeemeerguh | Merguh | NPC and variant |
| Boswachterguh | Ranger Guh | |
| Knabbelplukker | Nibble Picker | |
| Grillguh | Grill Guh | |
| Paradeguh / Tamboerguh | Parade Guh / Drummer Guh | |
| Burgemeester Vadsema | Mayor Chonkworth | |
| Knuffelburgemeester (title) | Snuggle Mayor | |
| Cocotje | Coco | always losing things |
| Opa Guh | Grandpa Guh | |
| Pluisje / Knabbeltje / Dikkie | Fluffy / Nibbles / Chubbs | Snuggledale neighbors |
| Mollie / Sproetje / Bolletje | Molly / Freckles / Dumpling | Snuggledale neighbors |
| Bakker Korstje | Baker Crusty | |
| Klantje / Feestklantje | Customer / Party Customer | |
| Juf Knuffel | Miss Snuggles | daycare |
| Mevrouw Theelepel | Mrs. Teaspoon | |
| Kapper Krulletje | Curly the Barber | |
| Boerin Hooibaal | Farmer Haybale | |
| Professor Sterretje | Professor Twinkle | |
| Kapitein Wolkje | Captain Cloudy | |
| Badmeester Bubbel | Lifeguard Bubbles | |
| IJscoguh Tingeling | Jingles the Ice Cream Guh | |
| Opoe Njegschuif | Granny Nyegshuffle | shuffleboard |
| Meneer Vadskronkel | Mr. Chonkwiggle | maze |
| Kapitein Floepguh | Captain Floopguh | catapult |
| Juf Vahoegsakee | Coach Wahoogaroo | Nibble Games host |
| Schaatsmeester Guhglij | Skate Master Guhglide | |
| Stempelguh | Stamp Guh | |
| Coach Vahoegvroem | Coach Wahoog-Vroom | |
| Brandweercommandant Blusguh | Fire Chief Splashguh | |
| Inspecteur Vahoegsma | Inspector Wahoogsby | |
| Dokter Snotneus-guh | Doctor Sniffleguh | |
| Bob de Guhbouwer | Bob the Guhbuilder | "Can we fix it?" nod |
| Klimguhtje / Snotje | Climby / Sniffles | career-street helpers |
| Brandweer-/Politie-/Apothekers-/Bouwguh | Firefighter / Police / Pharmacist / Builder Guh | learned careers |
| Timmerguh | Carpenter Guh | |
| Grillguhs geheime recept | Grill Guh's Secret Recipe | |
| Baltoguh | Baltoguh | |
| Boris (de gans) | Boris (the goose) | |
| Muk / Luk | Muk / Luk | |
| Rosy | Rosy | |
| Zuster Knuffel | Nurse Snuggles | |
| De witte wolf-guh | The White Wolf Guh | |
| Verteller | Narrator | |
| Held van Nomguh | Hero of Nomguh | title |
| Vriend van Guhtwo | Friend of Guhtwo | title (1.2.6) |
| Ohana-guh | Ohana Guh | title (1.2.6) |
| Wolkenvriend | Cloud Friend | title (1.2.6) |
| Huisjesbouwer | House Builder | title (1.2.6) |
| Opper-vadser | Over-Chonker | title (1.2.6) |
| Guhkenner | Guh Expert | title (1.2.6) |
| Titels / Geen titel | Titles / No title | Guhdex tab (1.2.6) |
| Guhtwo | Guhtwo | |
| Mieuwguh | Mewguh | |
| Professor Knabbelkloon | Professor Nibbleclone | |
| De wolkenhoeder | The Cloud Shepherd | |
| 626-guh | 626-guh | |
| Lilo-guh / Nani-guh | Lilo-guh / Nani-guh | |
| Tikiguh | Tiki Guh | |
| Vadstemmer | Chonk Tamer | villager profession |
| Guhkleermaker / kleermaker | Guh Tailor / tailor | villager profession |
| Vadssmid | Chonksmith | villager profession |
| Hamsterbouwer | Hamster Builder | villager profession |
| Mika-jager | Mika Hunter | villager profession |
| Knabbelboer | Nibble Farmer | villager profession |

## 9. Guh variants

| Dutch | English | note |
|---|---|---|
| Regenboogguh | Rainbow Guh | |
| Sterrenhemelguh | Starry Guh | |
| Spookguh | Ghost Guh | |
| Teckelguh | Dachshund Guh | |
| Mummieguh | Mummy Guh | |
| Muntguh | Mint Guh | |
| Chocoguh | Choco Guh | |
| Sneeuwguh | Snow Guh | |
| Truiguh | Sweater Guh | |
| Regenguh | Rain Guh | |
| Feestguh | Party Guh | |
| Chef-guh | Chef Guh | |
| Brontosaurusguh | Brontosaurus Guh | "Guh-saurus!" |
| Gouden Guh | Golden Guh | |
| brococolief | broccocutie | unique, lowercase on purpose |
| Enderguh | Enderguh | |
| Vahoege Enderguh / Hongerige Enderguh | Wahoog Enderguh / Hungry Enderguh | |
| Koningguh | King Guh | |
| Wolkguh | Cloud Guh | |
| Magere guh | Skinny Guh | |
| Kaasmoerasguh | Cheese Swamp Guh | |
| Asguh | Ash Guh | |
| Rookguh | Smoke Guh | |
| Pluisguh | Fluffguh | |
| Pinguh | Pinguh | |
| Glitterguh | Glitter Guh | |
| Kikkerguh | Frogguh | |
| Sterrenguh | Star Guh | |
| Sneeuwpopguh | Snowman Guh | the block you build |

## 10. Critters

| Dutch | English | note |
|---|---|---|
| diertjes / Diertjes van de Guhmensie | critters / Critters of the Guhmension | |
| Pieppiepmuisje(s) / muisje | Squeaksqueak Mouse (Mice) / mousie | |
| Schilly | Shelly | turtle, tiny head, big bestie |
| Poepschilly | Poopshelly | the butt-cleaner of the guh sea |
| Boze Kaasknabbel | Angry Cheese Nibble | |
| Boze Oppernabbel | Angry Overnibble | Opper -> Over |
| Roze Guh Koek | Pink Guh Cookie | |
| Guhvis | Guhfish | |
| Guhbij | Guh Bee | |
| Guhslijm | Guh Slime | |
| Kaasmot | Cheese Moth | |
| Vadswaker | Chonk Warden | Warden parody |
| Pluisvinkje | Fluff Finch | |
| Kaasmeesje | Cheese Chickadee | do NOT use "tit" |
| Guh-uiltje | Guh Owlet | |
| Zeemeeuwtje | Seagull | says "Mine! Mine!" (Nemo nod) |
| Guhxolotl | Guhxolotl | |
| Guh-eendje | Guh Duckling | |
| Knabbelvlindertje | Nibble Butterfly | |
| Glimguhtje | Glowguh | firefly |
| Lieveheersbeestje | Ladybug | |
| Pluisegeltje | Fluffhog | |
| Guh-konijntje | Guh Bunny | |
| Pluiseekhoorntje | Fluff Squirrel | |
| Sjokkel | Shuckly | Shuckle nod |
| Guhschaapje | Guh Lamb | |
| Knabbelkippetje | Nibble Chick | |
| Guhkoe | Guh Cow | |
| Babyguhtje (daycare) | Baby Guh | |
| Guh-sledehondje | Guh Sled Pup | |

## 11. Effects

| Dutch | English | note |
|---|---|---|
| Stil | Hushed | |
| Onvahoeg | Unwahoog | |
| Gezellig | Snug | |
| Uitgerust | Well-Rested | |
| Blosjes | Blushies | |
| Zweverig | Floaty | |
| Besties | Besties | |
| Fris van binnen | Fresh Inside | |
| Lief kijken | Puppy Eyes | |

## 12. Core items & blocks

| Dutch | English | note |
|---|---|---|
| Blok kaasknabbels | Block of Cheese Nibbles | |
| Kaasknabbelsteen / -diepsteen / -aarde / -keien | Cheese Nibble Stone / Deepslate / Dirt / Cobblestone | |
| Gefrituurde kaasknabbels | Fried Cheese Nibbles | |
| Mika's vet | Mika's Grease | used for frying |
| Mika-vetbal | Mika Grease Ball | |
| Guh-koekenpan | Guh Frying Pan | |
| Frituurvads | Fry Chonk | quest title |
| Kaassaus / Borrelende kaassaus | Cheese Sauce / Bubbling Cheese Sauce | |
| Vahoege vads | Wahoog Chonk | the pink ore material |
| Samengeperste super vahoege vads | Compressed Super Wahoog Chonk | |
| Vahoege-vadsstaaf | Wahoog Chonk Ingot | |
| Vahoege-vads(zwaard/houweel/bijl/schep/schoffel/paxel/schaar) | Wahoog Chonk (Sword/Pickaxe/Axe/Shovel/Hoe/Paxel/Shears) | |
| Vahoege-vads(helm/borstplaat/beenstukken/laarzen) | Wahoog Chonk (Helmet/Chestplate/Leggings/Boots) | |
| guhpantser (IJzeren/Diamanten/Netherieten) | Guh Armor (Iron/Diamond/Netherite) | |
| Bankguh / Bank Guh | Bank Guh | infinite storage; *het buikje* = *the tummy* |
| Guhrad / Guhdraad | Guh Wheel / Guh Wire | redstone |
| Guhspawner | Guh Spawner | |
| Knabbelbak | Nibble Trough | |
| Guhnaaitafel | Guh Sewing Table | |
| Vadsaambeeld | Chonk Anvil | |
| Buizenwerkbank | Tube Workbench | |
| Mikatrofee / Opper-Mikatrofee | Mika Trophy / Overlord Mika Trophy | |
| Mika-mepper | Mika Whacker | |
| Guhrugzak | Guh Backpack | |
| Guhgrotkompas / Uitdagingskompas / Heiligdomkompas | Guh Cave Compass / Challenge Compass / Shrine Compass | |
| Mika-spoorkompas | Mika Tracker Compass | |
| Guh-buikfluitje | Guh Belly Whistle | |
| Verloren guh-taart / Taartkruimels | Lost Guh Cake / Cake Crumbs | |
| Guh-ballon | Guh Balloon | |
| Guhkristal(-blok/-cluster/-lamp/-steen) | Guh Crystal (Block/Cluster/Lamp/Stone) | |
| Guh-kristalverrekijker | Guh Crystal Spyglass | |
| Guhslee / Sleerail / Sleeglijder | Guh Sled / Sled Rail / Sled Runner | |
| Vahoege sleebouwersboek | Wahoog Sled Builder's Book | |
| Vadsdrop / Guhkurkentrekker / Vahoegschans | Chonk Drop / Guh Corkscrew / Wahoog Ramp | sled rails |
| Maagwand / Maagbodem / Maagzuur / Maagportaal | Belly Wall / Belly Floor / Belly Acid / Belly Portal | |
| Tong / Tand / Verteerde kaasknabbels | Tongue / Tooth / Digested Cheese Nibbles | |
| Guhlelie | Guh Lily Pad | |
| Kaashoning / Knabbelkorf | Cheese Honey / Nibble Hive | |
| Vadszak / Guhkussen | Chonkbag / Guh Cushion | beanbag pun |
| Vadszetel / Vadskast | Chonk Couch / Chonk Cabinet | |
| Guhstoel / Guhtafel | Guh Chair / Guh Table | |
| Lampgion | Languhtern | lampion pun |
| Guhvlaggetjes | Guh Bunting | |
| Kaasbloem / Guhoortjes / Roze guhbloem / Knabbelroos | Cheese Flower / Guh Ears / Pink Guh Flower / Nibble Rose | |
| Guhbloesem(-stam/-boompje) / Vadsplanken | Guh Blossom (Log/Sapling) / Chonk Planks | |
| Guhtaart / Guhcupcake / guhcaron | Guh Cake / Guh Cupcake / Guhcaron | |
| Kaasfondguh | Cheese Fondguh | |
| Vahoege kaasknabbelshake | Wahoog Cheese Nibble Shake | |
| Kaasknabbelzaadjes / Kaasknabbelplant | Cheese Nibble Seeds / Cheese Nibble Plant | |
| Vadszaadbak | Chonk Seed Box | |
| Roze guhgras | Pink Guh Grass | |
| Eenrichtingsvadsglas | One-Way Chonk Glass | |
| Guhlock-pet / Vergroot-vadsglas | Guhlock Cap / Magnifying Chonk Glass | Sherlock nod |
| Koninklijke guhtroon / Gouden guhmedaillon | Royal Guh Throne / Golden Guh Medallion | |
| Wolkenlift / Wolkensuikerspin | Cloud Lift / Cloud Cotton Candy | |
| Kaasader (Diepe/Gouden/Uitgemijnde) | Cheese Vein (Deep/Golden/Mined-Out) | |
| Kaaskluis / Kaasbrok / Goudkaas | Cheese Vault / Cheese Chunk / Gold Cheese | |
| Gouden kaasknabbel | Golden Cheese Nibble | |
| Sterrenstof | Stardust | |
| Kaaskoraal / Parelmoer / Reuzenschelp / Parel | Cheese Coral / Mother-of-Pearl / Giant Clam / Pearl | |
| Kaaskorst(stenen) | Cheese Rind (Bricks) | |
| Mika-steen | Mika Stone | |
| Knabbelportaalframe / Knabbelsokkel / Knabbelslot / Knabbelpoort | Nibble Portal Frame / Nibble Pedestal / Nibble Lock / Nibble Gate | |
| Guheindeportaal | Guh End Portal | |
| Enderguh-ei | Enderguh Egg | |
| Mika-traan | Mika Tear | |
| Oog van Vadsig | Eye of Chonky | Eye of Ender nod |
| Knabbelkristal / Knabbelkroon | Nibble Crystal / Nibble Crown | |
| Guhvleugels | Guh Wings | |
| Gatenkaas(stenen) / Belegen kaas | Holey Cheese (Bricks) / Aged Cheese | |
| Kaasstalactiet / Gloeiend kaasmos | Cheese Stalactite / Glowing Cheese Moss | |
| Kaaskorrel / Kaaskorrelerts | Cheese Crumb / Cheese Crumb Ore | |
| Knabbelsensor / Knabbelschreeuwer / Stille knabbel | Nibble Sensor / Nibble Shrieker / Silent Nibble | sculk parodies |
| Kaasmodder / Kaasriet / Modderig kaasgras | Cheese Mud / Cheese Reeds / Muddy Cheese Grass | |
| Motknabbel / Moeraskaas | Moth Nibble / Swamp Cheese | |
| Vadsverdrijvend drankje | Chonk-Be-Gone Potion | |
| Vadshout(-stam/-planken/...) | Chonkwood (Log/Planks/...) | |
| Guhgezichtje in de schors | Guh Face in the Bark | |
| Vadsmos / Guhnestje / Vadstouw | Chonk Moss / Guh Nest / Chonk Rope | |
| Knabbelbessen(struik) / Knabbelbessentaartje | Nibbleberries (Bush) / Nibbleberry Tart | |
| Houtskoolsteen / Roosterijzer / Gloeikool | Charcoal Stone / Grill Iron / Glowcoal | |
| Satéstam / Worststam / Satévlees | Satay Stem / Sausage Stem / Satay Meat | |
| Pindasaus- / Mosterd-nylium | Peanut Sauce / Mustard Nylium | |
| Uienlicht / Rookgat / Smeulkooltjes | Onionlight / Smoke Vent / Smoldering Embers | |
| Verkoold guhbot / Verkoolde mikakop | Charred Guh Bone / Charred Mika Head | |
| Grillkool / Kaasfrituursaus / Aanmaakblokje | Grill Coal / Cheese Frying Sauce / Fire Starter Cube | |
| Kaasknabbelsaté / Guhbraadworst | Cheese Nibble Satay / Guh Bratwurst | |
| Guhbrouwketel / Knabbelbaken | Guh Brewing Cauldron / Nibble Beacon | |
| Grillspies / Gloeister / Gloeiend kooltje | Grill Skewer / Glowstar / Glowing Ember | |
| Rookloop- / Sluipknabbel- / Guhsprongdrankje | Smokewalk / Sneaky Nibble / Guh Jump Potion | |
| Knuffelgras / Pluisgras | Snuggle Grass / Fluff Grass | |
| Pluizenboom | Fluffy Tree | |
| Knuffelsteen / Pluisdak / Knuffelklinkers | Snugglestone / Fluff Thatch / Snuggle Cobbles | |
| Guhpaddenstoeltje | Guh Mushroom | |
| Feestbuffettafel / Knus-oorkonde | Party Buffet Table / Cozy Certificate | |
| Seizoensbloembak / Seizoensslinger / Bladerhoopje | Season Planter / Season Garland / Leaf Pile | |
| Knusfeestlijstje | Cozyfest List | |
| Knabbeloven | Nibble Oven | |
| Guh-wiegje / Speelkleed | Guh Cradle / Play Mat | |
| Guh-telescoop / Sterrenlantaarn / Wensster | Guh Telescope / Star Lantern / Wishing Star | |
| Guh-luchtballon / Mini-luchtballon | Guh Hot-Air Balloon / Mini Hot-Air Balloon | |
| Guh-slaapzak | Guh Sleeping Bag | |
| Guh-wastobbe / Guhshampoo / Guh-föhn | Guh Washtub / Guh Shampoo / Guh Hair Dryer | |
| Badeendje / Zwembandje | Rubber Ducky / Swim Ring | |
| Guh-xylofoon / Grijpmachine | Guh Xylophone / Claw Machine | |
| Liedjesboekje | Songbook | |
| Marshmallowknabbel | Marshmallow Nibble | |
| Kaasijsje | Cheese Ice Cream | |
| Pluiswol / Knabbelei / Kaasmelk / Knabbelvoer / Guhborstel | Fluff Wool / Nibble Egg / Cheese Milk / Nibble Feed / Guh Brush | |
| Guhgieter / Knabbelgraan / Knabbelmeel / Theekruid | Guh Watering Can / Nibble Grain / Nibble Flour / Tea Herb | |
| Guh-molentje | Guh Windmill | |
| Guhhuisje (klein/medium/groot) | Guh House (Small/Medium/Large) | |
| Knabbelbal / Guh-glijbaantje / Guh-wip / Guh-schommel / Pluizige tunnel | Nibble Ball / Guh Slide / Guh Seesaw / Guh Swing / Fluffy Tunnel | |
| Vadsigheid-scanner | Chonkiness Scanner | |
| Onberekenbaar vahoeg | Incalculably Wahoog | poster/painting |
| Kokosnoot / Kokosmelk / Guh-palm | Coconut / Coconut Milk / Guh Palm | |
| Guh-palm(stam/-planken/-trap/-plaat/-hek/-poort/-deur/-luik/-bord), Gestripte guh-palmstam | Guh Palm (Log/Planks/Stairs/Slab/Fence/Fence Gate/Door/Trapdoor/Sign), Stripped Guh Palm Log | the palm wood set (1.2.8) |
| Schilly-eitjes | Shelly Eggs | |
| Baltoguh-beeldje / Medicijnkist | Baltoguh Statuette / Medicine Chest | |
| Sneeuwguhspar | Snowguh Spruce | |
| Sneeuwslee / Sledebelletje | Snow Sled / Sleigh Bell | |
| Kloontank / Labnotitie / Tankonderdeel | Clone Tank / Lab Note / Tank Part | |
| Het Knuffelhart / Herinnering | The Snuggleheart / Memory | |
| Guhmuziekplaat | Guh Music Disc | |
| Wilde-guhweerder | Wild Guh Shoo-Sign | 1.2.0; "shoo-sign" in running text; its sign says *hier woont al een vadsje* = *a chonky lives here already* |

## 13. Food & bakery

| Dutch | English | note |
|---|---|---|
| Knabbelbroodje | Nibble Bun | |
| Kaaskrakeling | Cheese Pretzel | |
| Vadsvlaai | Chonk Pie | |
| Guhcroissant | Guhcroissant | |
| Knabbelkoekje | Nibble Cookie | |
| Kaasbolletje | Cheese Roll | |
| Pluismuffin | Fluff Muffin | |
| Theetaartje | Tea Tart | |
| Knabbeltompouce | Nibble Tompouce | Dutch pastry nod |
| Vadsdonut | Chonk Donut | |
| Guhwafel | Guh Waffle | |
| Sterrenkoekje | Star Cookie | |
| Feesttaart | Party Cake | |
| Knabbelthee / Kaasmelkthee / Theekruidthee / Guhbloementhee | Nibble Tea / Cheese Milk Tea / Herb Tea / Guh Flower Tea | |
| Warme chocovet | Hot Chocochonk | Elfstedentocht hot cocoa |
| Kommetje snert | Bowl of Pea Soup | |
| Poffertjes | Poffertjes (mini pancakes) | keep the Dutch nod |
| Vadsig gebakken guhvis | Chonky Fried Guhfish | |

## 14. Coins, tickets & currencies

| Dutch | English | note |
|---|---|---|
| munt / -munt | coin / Coin | |
| bon / -bon | ticket | *Kermisbon* = *Fair Ticket*; *Visbon* = *Fishing Ticket* |
| Boekenbon | Book Token | |
| Mepmunt / Discomunt / Smulmunt | Whack Coin / Disco Coin / Munch Coin | |
| Bakmunt / Speenmunt / Krulmunt | Bake Coin / Pacifier Coin / Curl Coin | |
| Ballonmunt / Eendjesmunt / Schelpjesmunt | Balloon Coin / Ducky Coin / Seashell Coin | |
| Showrozet / Raceprijsje | Show Rosette / Race Prize | |
| Katapultster / Spelenlintje / Elfstempel | Catapult Star / Games Ribbon / Town Stamp | |
| Sjoelschijfje / Circuitbeker / Sledesprintbeker | Shuffle Puck / Circuit Cup / Sled Sprint Cup | |
| Stempelkaart | Stamp Card | |
| Elf-Guhjeskruisje | Eleven Guhtowns Cross | the tour medal |

## 15. Minigames

| Dutch | English | note |
|---|---|---|
| Vads-wedstrijd | Chonk Pageant | at the Guh Beauty Theater |
| Miss Vadsig(-sjerp) | Miss Chonky (Sash) | |
| Guhrace / Huur-renguh / Recordgeest | Guh Race / Rental Racing Guh / Record Ghost | |
| Mika meppen | Whack-a-Mika | |
| Guhdisco songs: Vadsige Tango / Mika-Mambo / Njeg-Njeg Boogie | Chonky Tango / Mika Mambo / Nyeg-Nyeg Boogie | |
| Ze hangen aan me vet/veh | They Hang On My Chonk | also the jukebox song: *Guh - They Hang On My Chonk* |
| Guhkoorts op zaterdagavond | Saturday Night Guh Fever | |
| Guhgolf (9 holes) | Guh Golf (9 Holes) | |
| Vadsig eetfestijn | Chonky Food Fest | |
| Guhvis-wedstrijd | Guhfish Contest | |
| Kaasvis / Vadsbaars / Guhpuffer / Roze njegforel / Mika-meerval / Gouden Guhvis | Cheesefish / Chonky Bass / Guhpuffer / Pink Nyeg Trout / Mika Catfish / Golden Guhfish | |
| Verstopguh / verstoppertje | Hide-and-Guh / hide-and-seek | |
| Knabbels bakken | Nibble Baking | |
| Kappersshow | Salon Show | |
| Glijbanen: Roze Trechter / Glimtunnel / Grote Plons | Slides: Pink Funnel / Glow Tunnel / Big Splash | |
| Guh-sjoelen / sjoelbak | Guh Shuffleboard / shuffleboard | Dutch sjoelen nod |
| Guhdoolhof | Guh Maze | |
| Pluisballen / Mika-fort | Fluffballs / Mika Fort | catapult |
| De Grote Zeskamp | The Great Hexathlon | |
| Knabbelhappen | Nibble Bobbing | |
| Zaklopen | Sack Race | |
| Mika-blikgooien | Mika Can Toss | |
| Eierlopen met knabbelei | Egg-and-Spoon Race | |
| Spijkerpoepen | Nail Plopping | the Dutch party game, kid-cheeky |
| Guhguhtje prik | Pin the Tail on the Guh | |
| Elf-Guhjestocht | Eleven Guhtowns Tour | |
| Regenboogbaan / Vadsbaan / Kaasbergbaan | Rainbow Road / Chonk Track / Cheese Mountain Track | Guh Circuit |
| Vadslooping / Rolknabbel | Chonk Loop / Rolling Nibble | |
| Nomguh-sledesprint / medicijntocht | Nomguh Sled Sprint / medicine run | |
| Surfen / Hula / Lilo-guh's surfschool | Surfing / Hula / Lilo-guh's Surf School | |
| Knabbeldraai / njeg-grab / Guh-hop | Nibble Spin / nyeg-grab / Guh Hop | surf tricks |
| Aloha, Njeg / Guhla-Hula Rock / Vahoeg Hula Hop | Aloha, Nyeg / Guhla-Hula Rock / Wahoog Hula Hop | hula songs |
| Guhkwis | Guh Quiz | library |

## 16. Stories (Guh Tales) & questlines

| Dutch | English | note |
|---|---|---|
| De ontvoerde guh | The Guhnapping | Mother Chonky's story |
| De kapotte slee | The Broken Sled | |
| Het Grote Knusfeest / Knusfeest / seizoensfeest | The Great Cozyfest / Cozyfest / season party | |
| feesttaakjes | party chores | |
| Het Guheinde (story) | The Guh End | |
| De Grillguh helpt | The Grill Guh Helps | |
| Samen een huisje bouwen | Building a House Together | |
| Baltoguh en de medicijnkist | Baltoguh and the Medicine Chest | Balto parody |
| Het kloon-eiland | Clone Island | Mewtwo parody |
| Het Knuffelhart | The Snuggleheart | revive story |
| in de wolkjes | up in the clouds | where lost guhs go (gentle, never "dead") |
| Ohana op Guhwai'i | Ohana on Guhwai'i | Lilo & Stitch parody; "Ohana means family" |
| Beroep: brandweer / politie / apotheek / bouw | Career: Firefighter / Police / Pharmacy / Builder | |
| De Knabbeldief-zaak | The Case of the Nibble Thief | |
| De Grote Mika-oorlog | The Great Mika War | |
| steen-papier-schaar-VADS | rock-paper-scissors-CHONK | the VADS gesture = the CHONK gesture |
| Ridder van de Koningguh / van het Guheinde | Knight of the King Guh / of the Guh End | |
| De allereerste kaasknabbel / Het geheim van de kaasknabbel | The Very First Cheese Nibble / The Secret of the Cheese Nibble | |
| VAHOEG! Het guhwoordenboek | WAHOOG! The Guh Dictionary | library book |
| Het Geheime Guhboek | The Secret Guh Book | |

## 17. FTB Quests chapters

| Dutch | English | note |
|---|---|---|
| Guhs (chapter group) | Guhs | |
| Guhs & basis | Guhs & Basics | keep `\\&` escape |
| De Guhmensie | The Guhmension | |
| Minigames & bijzondere plekken | Minigames & Special Places | |
| Onderwater | Underwater | |
| De Guhmaag | The Guhbelly | |
| Het Guheinde | The Guh End | |
| De Guhbarbecuether | The Guhbarbecuether | |
| Grotten, moeras & woud | Caves, Swamp & Woods | |
| Knuffeldal | Snuggledale | |
| Piep! | Squeak! | |
| Lieve vadsjes van elkaar | Sweet Chonkies Together | |
| Guhverhalen | Guh Tales | |
| Diertjes van de Guhmensie | Critters of the Guhmension | |
| Hoe kom je hier? | How Do You Get Here? | first quest of every chapter |

## 18. Snuggledale collections & songs

| Dutch | English | note |
|---|---|---|
| Knuffelvriendjes / Seizoensplakboek | Snuggle Friends / Season Scrapbook | |
| Receptenboek / Theesoorten / Kapselcollectie | Recipe Book / Tea Collection / Hairstyle Collection | |
| Kapsel: Krullen/Kuifje/Knotjes/Strikjes/Pluisbol/Vlechtjes/Hanenkam/Matje | Hairstyle: Curls/Quiff/Buns/Bows/Puffball/Braids/Mohawk/Mullet | |
| Haarverf (roze/mint/citroen/lavendel/hemelsblauw/perzik/zilver/regenboog) | Hair Dye (Pink/Mint/Lemon/Lavender/Sky Blue/Peach/Silver/Rainbow) | |
| Tuinboek / Sterrenatlas / Ballonstempelkaart / Verhalenbundel | Garden Book / Star Atlas / Balloon Stamp Card / Story Collection | |
| Badeendjes / Knuffelkast / Liedjesboek / Kaasijsjes / Piepboek | Rubber Duckies / Plushie Shelf / Songbook / Cheese Ice Creams / Squeak Book | |
| De Grote Knabbel / De Kleine Vads | The Big Nibble / The Little Chonk | constellations (Big/Little Dipper nod) |
| De Vahoege Buik / De Vluchtende Mika / De Kaasschaaf | The Wahoog Belly / The Fleeing Mika / The Cheese Slicer | constellations |
| De VAHOEGE Toonladder | The WAHOOG Scale | |
| Vader Guh | Are You Sleeping, Brother Guh? | Frère Jacques |
| Altijd is Guhtje vads | Twinkle Twinkle Little Guh | same melody |
| Alle guhtjes zwemmen in de kaassaus | All the Guhlings Swim in Cheese Sauce | |
| In de knabbelmaneschijn | By the Light of the Nibble Moon | |
| Ode aan de Knabbel | Ode to the Nibble | Ode to Joy |
| Slaap, guhtje, slaap | Hush, Little Guhling | lullaby |
| Daar is het maantje / Knabbeltje onder je kussen / Wolkje, wolkje | There's the Little Moon / A Nibble Under Your Pillow / Little Cloud, Little Cloud | lullabies |

## 19. Outfits & clothing words

| Dutch | English | note |
|---|---|---|
| hoed / muts / pet / petje | hat / cap / cap / little cap | |
| jas / jasje / vest / trui / sjaal / strik | coat / jacket / vest / sweater / scarf / bow | |
| Guhzonnebril / Hartjesbril / Guhmonocle | Guh Sunglasses / Heart Glasses / Guh Monocle | |
| Sinterklaasmijter / Pietenmuts | Saint Nicholas Miter / Helper's Cap | |
| Koningsdagkroontje / Oranje shirt | King's Day Crown / Orange Shirt | Dutch holiday nod |
| Roze guhonesie / Guhkoksbuis / Guhzuidwester | Pink Guh Onesie / Guh Chef Jacket / Guh Sou'wester | |
| Jockeypetje / Mika-jagershoed / Mepvest met zakjes | Jockey Cap / Mika Hunter Hat / Whacking Vest with Pockets | |
| Glittervadspak / Vahoege afropruik | Glitter Chonk Suit / Wahoog Afro Wig | |
| Ruitjesvadstrui / Vadsige golfpet | Argyle Chonk Sweater / Chonky Golf Cap | |
| Mijnguhhelm / Boekenwurmvest / Geleerdenbaret | Miner Guh Helmet / Bookworm Vest / Scholar's Beret | |
| Hermelijnen koningsmantel / Vahoege guhkoningskroon | Ermine Royal Cape / Wahoog Guh King Crown | |
| Gouden aureooltje / Wolkenvleugeltjes | Golden Halo / Cloud Wings | |
| Rood sjaaltje van Baltoguh / Wolfsoortjes | Baltoguh's Red Scarf / Wolf Ears | |
| Guhtwo-staartje + nekbuisje / Trainerpetje | Guhtwo Tail + Neck Tube / Trainer Cap | |
| 626-oren met antennes / Hula-rokje / Bloemenkrans | 626 Ears with Antennae / Hula Skirt / Flower Lei | |
| Hartjesspeldje / Knuffeltruitje / Zielskroontje / Gouden hartjes-halsbandje | Heart Pin / Snuggle Sweater / Soulguh Crown / Golden Heart Collar | soulguh rewards |
| Oorstrikjes | Ear Bows | |

---

## 20. Quick word-builder (for compounds not listed)

| Dutch part | English part |
|---|---|
| guh- | Guh / guh- (*Guh Sled*, *Guhfish*: join only when it's a creature/proper name, otherwise two words) |
| knabbel- | Nibble |
| kaas- | Cheese |
| vads- / vadsig | Chonk / Chonky |
| vahoeg(e)- | Wahoog |
| knuffel- | Snuggle (places/names), Cuddle (actions), Plushie (toys) |
| knus- | Cozy |
| pluis- | Fluff / Fluffy |
| Mika- | Mika |
| opper- | Over- / Overlord |
| maag- | Belly |
| wolk(en)- / wolkje | Cloud / little cloud |
| sterren- | Star |
| roze | pink |
| zielsguh- | soulguh |
| -guhtje | -guhling / little guh |

## 21. Added during review (cross-chunk names)

| Dutch | English | note |
|---|---|---|
| Smulschaal (geleend) | Munch Bowl (Borrowed) | "munch bowl" in running text |
| Feestslingers | Party Garlands | "party garlands" / "garlands" in running text |
| Kaasmelkdrankje | Cheese Milk Potion | |
| Guh-belletje | Guh Jingle Bell | not the same item as Guh Bell |
| Leenkledingkast | Loaner Wardrobe | |
| Bouwboekje van de Timmerguh | Carpenter Guh's Building Booklet | |
| Showster(-) | Showstar | clothing line, shop |
| Guh-kwismeester | Guh Quizmaster | |
| Knuffeldekentje | Snuggle Blankie | |
| Rompertje | Baby Romper | |
| Guhvoerbak | Guh Feeding Trough | |
| Routelantaarn | Trail Lantern | |
| Mieuwguh-ballonnetje | Little Mewguh Balloon | |
| Feestkapselset | Party Hairdo Kit | |
| De Vallende Knabbel | The Falling Nibble | constellation |
| Pluk (verhaal van Opa Guh) | Pip | |
| Plaatje (bakvorm) | Disc | bakery shape |
| TATUU (sirene) | Nee-naw | fire siren sound |
| Dames en heren-guhs | Ladies and Guhtlemen | |

## 22. The Palewood (1.2.8)

| Dutch | English | note |
|---|---|---|
| Bleekwoud | Palewood | the biome: "the Palewood" in running text |
| Bleekhout(-stam/-planken/-bord/...) | Pale Guhwood (Log/Planks/Sign/...) | |
| Slapend guhgezichtje in de schors | Sleepy Guh Face in the Bark | |
| Bleekmos / Bleekmostapijt / Bleek hangmos | Palewood Moss / Palewood Moss Carpet / Hanging Palewood Moss | not vanilla's "Pale Moss" |
| Krakend Guhhartje / Verzuurd Guhhartje | Creaking Guh Heart / Soured Guh Heart | verzuurd = soured; "zure hartjes" = sour hearts |
| Kraakguh / Kraak-Mika | Creakguh / Creak Mika | |
| Krak! / Krak... krak... | Creak! / Creak... creak... | |
| houten knuffel | wooden hug | |
| Kaashars / Blok kaashars | Cheese Resin / Block of Cheese Resin | |
| Harssteen / Harsstenen (trap/plaat/muur) / Gebeitelde harsstenen | Cheese Resin Brick / Cheese Resin Bricks (Brick Stairs/Slab/Wall) / Chiseled Cheese Resin Bricks | |
| Oogbloempje (dicht/open) | Guh Eyeblossom (Closed/Open) | "guh eyeblossom" in running text |
| Bleke open plek / De Bleke Open Plek | Pale Clearing / The Pale Clearing | |
| Houthakkershutje / Houthakkerguh | Woodcutter's Hut / Woodcutter Guh | |

## 23. Guhpixel (lobby, minigames, things for home, Reisbureau, Guh-parkour)

### kern

| Dutch | English | note |
|---|---|---|
| Guhpixel | Guhpixel | never translated |
| Guhpixel-muntjes / muntje / muntjes | Guhpixel Coins / coin / coins | not an item |
| Guh-internetcafé "De Trage Verbinding" | Guh Internet Café "The Slow Connection" | |
| Beheerder-guh / Welkomstguh / Verkoper-guh | Admin Guh / Welcome Guh / Shopkeeper Guh | |
| Netwerkkabeltje / Guhpixel-poort / Guhpixel-portaal | Network Cable / Guhpixel Gate / Guhpixel Portal | |
| Guhpixel-winkel | Guhpixel Shop | |
| [GUH] [VADS] [VADS+] [MVG] [MVG++] | [GUH] [CHONK] [CHONK+] [MVG] [MVG++] | the rank prefixes |
| Guhpixel & uitjes (Guhdex tab) | Guhpixel & Outings | |
| grapspelletjes | joke games | |
| aandenken | keepsake | |
| Skyblok / Bedwars / Vadsnite / Vadsbus | Skyblok / Bedwars / Chonknite / Chonk Bus | |
| Guhmon-gevecht / Gymleider Dutjes | Guhmon Battle / Gym Leader Naps | |
| Boer zoekt Guh / Presentatrice Guhvon / Boer Guhrrit | Farmer Wants a Guh / Host Guhvon / Farmer Guhrrit | |
| Among Guhs / De Vadsvaarder / Kapitein-guh / droomguh | Among Guhs / The Chonkfarer / Captain Guh / dream guh | |
| Guhkade / Flappy Guh / Mika-Pong | Guhcade / Flappy Guh / Mika Pong | |
| Guhkantoor / Prikklok / Bureautje / loonstrookje / kwartaalrapport | Guh Office / Time Clock / Little Desk / payslip / quarterly report | |
| Guhbioscoop / Bioscoopdoek / Bioscoopstoeltje / film | Guh Cinema / Cinema Screen / Cinema Seat / movie | |
| Reisbureau "De Vadsvakantie" / Reisagent-guh / Reisbalie / Reisstempel / reispas / ansichtkaart | Travel Agency "The Chonk Vacation" / Travel Agent Guh / Travel Desk / Travel Stamp / travel pass / postcard | |
| Vadsen bij huize Lingsesdijk 86 / Huize Lingsesdijk 86 | Chonking at Lingsesdijk 86 / The House at Lingsesdijk 86 | keep "Lingsesdijk 86" exactly |
| Guh-parkour / Startpaaltje / Finishpaaltje | Guh Parkour / Start Post / Finish Post | |

### lobby

| Dutch | English | note |
|---|---|---|
| Lobbyguh | Lobby Guh | the six little guhs with gamer names that "chat" |
| Slapende surfer | Sleeping Surfer | a guh asleep behind a computer in the café |
| Oude beige computer | Old Beige Computer | decoration block |
| Gouden lobbyknabbel / gouden knabbel | Golden Lobby Nibble / golden nibble | ten hidden in the lobby |
| Lobby-parkour | lobby parkour | for players; not the Guh Parkour for guhs |
| Parkour-startplaat / -tussenpunt / -finishplaat | Parkour Start Plate / Checkpoint / Finish Plate | |
| tussenpunt | checkpoint | |
| AFK-hoek | AFK Corner | "afk (slaap)" = "afk (sleeping)" |
| De winkel / Guhpixel-winkel | The Shop / Guhpixel Shop | |
| kraampje | stall | the game stalls in the lobby |
| Knabbelspeurder | Nibble Sleuth | title |
| Meest Vadsige Guh | Most Valuable Guh | what MVG stands for (Dutch: Meest Vadsige Guh); no title since 1.3.1 |
| Spelers online: n (en 47 guhs) | Players online: n (and 47 guhs) | |
| TERUG NAAR HUIS | BACK HOME | the exit door |
| BINNENKORT, njeg | COMING SOON, Nyeg | the empty stall |
| xX_Vadsje_Xx / Knabbel2009 / NjegMaster / SlaapKopGuh / GuhGamer_NL / KaasKoning77 | xX_Chonky_Xx / Nibble2009 / NyegMaster / SleepyHeadGuh / GuhGamer_NL / CheeseKing77 | gamer names of the lobby guhs |
| Heb je hem al uit en weer aan gezet, njeg? | Have you tried turning it off and on again, Nyeg? | the Admin Guh |
| daklopers | roof runners | |

### grap1

| Dutch | English | note |
|---|---|---|
| Skyblok-guh / Bedwars-guh / Vadsnite-guh | Skyblok Guh / Bedwars Guh / Chonknite Guh | the three game guhs in the lobby |
| spelguh | game guh | a lobby NPC that starts a game |
| grapspelletje / Grapspelletjes (FTB section) | joke game / Joke Games | |
| hét eiland | THE island | Skyblok; the Dutch stress accent becomes capitals |
| handleiding (Stap 1 van 4.812) | manual (Step 1 of 4,812) | English thousands separator is a comma: 4,812 and 2,406 |
| cobblestone generator | cobblestone generator | kept in both languages (the players' own word) |
| SKYBLOK UITGESPEELD! | SKYBLOK COMPLETED! | |
| aftiteling / Regie / Met dank aan | credits / Director / Special thanks to | |
| decorguhs | set guhs | the guhs who painted the sky |
| ALLEEN PERSONEEL / ZON (40 watt) | STAFF ONLY / SUN (40 watts) | floating labels in the Skyblok box |
| Eilandje-in-een-fles | Island in a Bottle | Skyblok keepsake (decoration block) |
| Team Rood / Blauw / Groen / Geel | Team Red / Blue / Green / Yellow | |
| Teamguh | Team Guh | the stand-in entity |
| Teamslaapmuts Rood (...) | Team Nightcap Red (...) | Bedwars keepsake; slaapmuts = nightcap |
| VERDEDIG JE BED! / BED VERDEDIGD! | DEFEND YOUR BED! / BED DEFENDED! | |
| bedden vernield / dutjes | beds destroyed / naps | end screen |
| bruggen (met wol) | to bridge (with wool) | |
| KUSSEN-GENERATOR | PILLOW GENERATOR | |
| Sloopwars | Breakwars | "Dit is Bedwars, geen Sloopwars" = "This is Bedwars, not Breakwars" |
| luik | hatch | in the floor of the Chonk Bus |
| Nog wakker: %s | Still awake: %s | the counter |
| slaapwolk | sleep cloud | the pink "storm" of Chonknite |
| #1 VADSOVERWINNING / Vadsoverwinning | #1 CHONK ROYALE / Chonk Royale | parody of "#1 Victory Royale" |
| GELIJKSPEL / Iedereen slaapt. Gelijkspel, njeg. | DRAW / Everyone is asleep. It's a draw, Nyeg. | |
| Parachuterugzakje | Parachute Pack | Chonknite keepsake (outfit, back) |
| slaapplekje | sleeping spot | the beds on the Chonknite island |
| Aandenken van Bedwars (Guhpixel) | Keepsake from Bedwars (Guhpixel) | clothing source |

### grap2

| Dutch | English | note |
|---|---|---|
| Guhmon-gevecht / Guhmon | Guhmon Battle / Guhmon | Cobblemon parody; "Guhmon" never translated |
| Gymleider Dutjes / de Dutjesgym | Gym Leader Naps / the Nap Gym | core glossary |
| Snurkel | Snorkel | the gym leader's guh |
| Leenguh | Loaner Guh | the gym's own guh for players without tamed guhs |
| SLAAP (balk) | SLEEP (bar) | instead of HP; caps as in the Dutch |
| Vadsen / Njeg / Knabbel eten / Dutje (zetten) | Chonk / Nyeg / Eat Nibble / Nap (moves) | Title Case, like move names |
| "Het is super vadsig!" / "Het is niet erg effectief..." | "It's super chonky!" / "It's not very effective..." | battle lines |
| vol buikje | full tummy | the after-effect of Eat Nibble |
| Badgedoosje / gymbadge | Badge Case / gym badge | |
| Dutjesbadge / Njegbadge / Knabbelbadge | Nap Badge / Nyeg Badge / Nibble Badge | |
| Guhmon-trainerspet | Guhmon Trainer Cap | outfit; not the Mewtwo "Trainerpetje" |
| "dicht wegens dutje" | "closed for a nap" | the other five gyms |
| Boer zoekt Guh | Farmer Wants a Guh | core glossary |
| Presentatrice Guhvon / Boer Guhrrit | Host Guhvon / Farmer Guhrrit | core glossary |
| Tukkie / Dommelien / Snurkbert | Snoozie / Dozeline / Snorebert | the three candidates |
| logeerweek | stay-over week | |
| instoppen / ingestopt | tuck in / tucked in | |
| slaapvrienden | nap friends | |
| aftiteling | credits | |
| Brievenbus / Ingelijste brief | Mailbox / Framed Letter | |
| Strohoed van Boer Guhrrit / Overall van Boer Guhrrit | Farmer Guhrrit's Straw Hat / Farmer Guhrrit's Overalls | outfit; not the old Straw Hat / Overalls |
| kijkbuisguhtjes | dear viewers | Guhvon's greeting |
| Guhpixel TV | Guhpixel TV | |

### among

| Dutch | English | note |
|---|---|---|
| Among Guhs / De Vadsvaarder | Among Guhs / The Chonkfarer | the game and its ship (kern glossary) |
| Kapitein-guh / Logboek-guh | Captain Guh / Logbook Guh | the queue; the personal numbers |
| Ruimteguh / ruimtepakje | Space Guh / space suit | the guh NPCs of a round |
| crew / Mika / Mika's | crew / Mika / Mikas | roles; never "impostor" |
| in slaap duwen / slaper / droomguh | to push asleep / sleeper / dream guh | nobody is hurt: never "kill", "dead", "body" |
| Mika-kussentje / Saboteerkaart / Stembriefje | Mika Pillow / Sabotage Map / Voting Slip | the game items |
| Taakjes-paneel / Noodknop / Ventilatieluik (luik) | Task Panel / Emergency Button / Vent Hatch (vent) | the ship blocks |
| vergadering / uitspraak / stemmen / overslaan / wegstemmen | meeting / statement / to vote / to skip / to vote out | |
| wachtrij / klaar / leider | queue / ready / leader | |
| Normaal / Lastig | Normal / Hard | difficulty |
| Licht uit / Knabbelalarm / Deuren dicht | Lights Out / Nibble Alarm / Doors Shut | the three sabotages |
| de Slaapzaal / de Kantine / de Navigatie / de Reactor | the Dorm / the Cafeteria / Navigation / the Reactor | room names, with their article inside sentences |
| de Ziekenboeg / Elektra / de Machinekamer | the Sick Bay / Electrical / the Engine Room | |
| de Voorraadkamer / de Schildkamer / de gang | the Storage Room / the Shield Room / the corridor | |
| Worstjes knopen / Pasje door de lezer / Kruimelbak legen | Tying Sausages / Card Through the Reader / Emptying the Crumb Tray | task names |
| Pindasaus tanken / Knabbels sorteren / Dromen downloaden | Fuelling Peanut Sauce / Sorting Nibbles / Downloading Dreams | |
| Wegen ("Resultaat: vads") / Schakelaars goedzetten | Weighing ("Result: chonk") / Setting the Switches | |
| Rood, Blauw, Groen, Geel, Roze, Oranje, Paars, Wit, Bruin, Mint | Red, Blue, Green, Yellow, Pink, Orange, Purple, White, Brown, Mint | a guh NPC is called by its color |
| sus | sus | stays "sus" |
| tellen (10 tellen) | seconds | |
| Sus | Sus | title (the only Among Guhs title since 1.3.1) |
| Onterecht weggestemd | Wrongly Voted Out | a number on the stats board |
| oefenrondje | practice round | the one-time parody round at Captain Guh (joke game "among") |
| SUS-stickerbord | SUS Sticker Board | the keepsake of the practice round |
| (kleur) ruimtepakje / luchttank | (color) Space Suit / air tank | shop clothes: "Red Space Suit" |
| Plantjeshoedje / Eihoedje / Wc-rolhoedje | Little Plant Hat / Fried Egg Hat / Toilet Roll Hat | shop hats |
| Kaaspunthoedje / Sus-briefje / Knabbelhoedje | Cheese Wedge Hat / Sus Note / Nibble Hat | |
| hendel / streep / bak / schakelaar / lampje / briefje | lever / line / bin / switch / little lamp / note | words of the task panels |
| kaasknabbel / knabbel / kruimel (the three bins) | Cheese / Nibble / Crumb | short labels on the bins |
| noodvergadering / agendapunt / besluit | emergency meeting / (agenda) item / decision | the Emergency Button at home |
| gluren (uit het luik) | to peek out (of the vent) | the Vent Hatch at home |
| cijferbord / logboek | stats board / logbook | the Logbook Guh's screen |

### guhkade

| Dutch | English | note |
|---|---|---|
| Guhkade | Guhcade | "arcade" + guh |
| Guhkade-kast / speelkast | Guhcade Cabinet / arcade cabinet | block names: *Guhcade Cabinet: Flappy Guh*, *Guhcade Cabinet: Mika Pong* |
| Flappy Guh | Flappy Guh | never translated |
| Mika-Pong | Mika Pong | no hyphen in English; on the cabinet's screen *MIKA PONG* |
| kaasknabbelpilaren | cheese nibble pillars | |
| fladderen | flap | the one button of Flappy Guh |
| batje | paddle | |
| potje | game | *een potje doen* = *play a game* |
| top 5 van deze kast | this cabinet's top 5 | |
| plafond (van een guh) | ceiling | the best score a kind of guh can ever get |
| sip kijken | look glum | the beaten guh; never "cry" |
| extra oefenen | practice extra | |
| NJEG! / VAHOEG! / TIJD! (op het scherm) | NYEG! / WAHOOG! / TIME! | capitals only: the cabinet's pixel font knows A-Z, digits and - ! ? . : + |

### kantoor

| Dutch | English | note |
|---|---|---|
| Guhkantoor | Guh Office | |
| Guhkantoor-set | Guh Office Set | shop offer: a Time Clock and one Little Desk |
| Guhkantoor B.V. (Besloten Vadsschap) | Guh Office Ltd. (Limited Chonkability) | the company name on every paper |
| Prikklok | Time Clock | |
| Bureautje | Little Desk | *bureau* in the screen = *desk* |
| loonstrookje | payslip | |
| kwartaalrapport | quarterly report | *boekjaar* = *fiscal year* |
| Oorkonde Werknemer van de maand | Employee of the Month Certificate | *oorkonde* = *certificate* |
| Werknemer van de maand | Employee of the Month | Title Case, also in running text |
| inklokken / uitklokken | clock in / clock out | buttons: *Aan het werk* = *Put to Work*, *Naar huis* = *Go Home* |
| dienst | shift | 8 real hours |
| postvakje | pigeonhole | holds 3 payslips per guh |
| bruto / netto / ingehouden / toeslag | gross / net / withheld / bonus | payslip lines |
| de baas / de directie | the boss / management | |
| Vadstoeslag | Chonk bonus | |
| Vakbond Vads Verenigd | Union Chonks United | |

### bioscoop

| Dutch | English | note |
|---|---|---|
| Guhbioscoop | Guh Cinema | core glossary; "theater" for the room (American) |
| film | movie | American; "Skyblok: De Film" = "Skyblok: The Movie" |
| Guhbioscoop-projector | Guh Cinema Projector | block |
| Bioscoopdoek | Cinema Screen | block; "a piece of Cinema Screen" |
| Bioscoopstoeltje | Cinema Seat | block |
| Popcornmachine | Popcorn Machine | block |
| Bakje popcorn | Tub of Popcorn | item |
| Guhbioscoop-set | Guh Cinema Set | shop offer |
| Filmavond / Filmkenner | Movie Night / Movie Buff | FTB quests |
| UITGESPEELD! | COMPLETED! | on the screen in the Skyblok movie |
| #1 VADSOVERWINNING | #1 CHONK ROYALE | the Chonknite win banner |
| slaapwolk | sleep cloud | Chonknite |
| Slaap ze allemaal! | Gotta nap 'em all! | Guhmon tagline |
| Vadsen / Njeg / Knabbel eten / Dutje (Guhmon moves) | Chonk / Nyeg / Eat Nibble / Nap | "Guh used Chonk!" |
| Het is super vadsig! | It's super chonky! | |
| logeerweek | sleepover week | Farmer Wants a Guh |
| Noodvergadering | Emergency meeting | Among Guhs |
| Rood is sus | Red is sus | never translated further |
| ventilatieluik | vent | |
| EINDE | THE END | credits |

### reisbureau

| Dutch | English | note |
|---|---|---|
| Reisbureau "De Vadsvakantie" | Travel Agency "The Chonk Vacation" | structure; on signs: Travel / Agency: The / Chonk Vacation |
| Reisagent-guh | Travel Agent Guh | NPC |
| Reisbalie / balie | Travel Desk / desk | block; the counter in the structure is the same block |
| Reisstempel | Travel Stamp | item, used in the Travel Desk recipe |
| reispas / stempel | travel pass / stamp | per player, not an item |
| ansichtkaart | postcard | item names: *Postcard: <trip>* |
| baas (a guh about its owner) | boss | postcards: *Hi boss!* |
| Veel vadsjes van | Lots of chonks from | the postcard's closing line |
| 1 knabbel (postage stamp) | 1 nibble | |
| Proefreisje om de hoek | Trial Trip Around the Corner | the questline's five-minute trip |
| Gouden koffertje / Koffertje | Golden Suitcase / Little Suitcase | deco blocks |
| Vadsen bij huize Lingsesdijk 86 | Chonking at House Lingsesdijk 86 | the user's inside joke: "Lingsesdijk 86" is never translated |
| Schilderij "Huize Lingsesdijk 86" | Painting "House Lingsesdijk 86" | wall block |
| Dagje Kaasmarkt | Day at the Cheese Market | |
| Middagdutje in het Vadswoud | Afternoon Nap in Chonkwood Forest | |
| Kinderboerderij Knuffeldal | Snuggledale Petting Farm | |
| Strandmiddag op Guhwai'i | Beach Afternoon on Guhwai'i | |
| Wellness in de Barbecuether | Wellness in the Barbecuether | |
| Pretpark de Efteguh | Efteguh Theme Park | Efteling pun, keep "Efteguh" |
| Holle Bolle Guh | Hungry Bulgy Guh | "Papier hier, njeg!" = "Paper here, nyeg!" |
| Guhkenhof | Guhkenhof | Keukenhof pun, keep |
| Wintersport in Nomguh | Winter Sports in Nomguh | |
| Stedentrip Guhrijs / Guhrijs | City Trip to Guhris / Guhris | Paris pun |
| Eiffelknabbeltoren(tje) | (Little) Eiffel Nibble Tower | |
| Camping De Vadsige Tent | Camp Chonky Tent | |
| Guhnetië | Guhnice | Venice pun |
| Reis naar de Kaasmaan / Kaasmaan | Trip to the Cheese Moon / Cheese Moon | |
| Wereldreis in 80 dutjes | Around the World in 80 Naps | |
| Cruise over de Guhzee | Cruise on the Guh Sea | |
| Thuisblijfvakantie "Balkonië" / Balkonië | Staycation "Balconia" / Balconia | |
| Thuis is het ook vads | Home Is Chonk Too | painting title |
| glimguhtjes-lantaarn | Glowguh Lantern | |
| Op vakantie | On vacation | American spelling: vacation, not holiday |

### parkour

| Dutch | English | note |
|---|---|---|
| Guh-parkour / parkour | Guh Parkour / parkour | the course for guhs; not the lobby parkour for players |
| Startpaaltje / Finishpaaltje | Start Post / Finish Post | core glossary |
| Guh-horde | Guh Hurdle | |
| Guh-springplank | Guh Springboard | |
| Kruiptunnel | Crawl Tunnel | not the Fluffy Tunnel (Pluizige tunnel) |
| Slalompaaltjes | Slalom Poles | |
| Evenwichtsbalk | Balance Beam | |
| Knabbeltafeltje | Nibble Table | |
| Parkour-scorebord / scorebord | Parkour Scoreboard / scoreboard | |
| stuk (van een route) / hindernis | piece (of a route) / obstacle | |
| route uitzetten / stukken aanklikken | laying out a route / click pieces | |
| rondje / rondetijd / record | lap / lap time / record | |
| op de route zetten / van de route halen | put on the route / take off the route | |
| bobbel | bump | the guh inside the Crawl Tunnel |
| verplichte pitstop | mandatory pit stop | |
| Hup! / Boing! / Oeps! | Hup! / Boing! / Oops! | |
| vadsje | little chonk | |
