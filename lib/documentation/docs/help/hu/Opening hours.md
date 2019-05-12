# OpenStreetMap nyitvatartás-szerkesztő

Az OpenStreetMapben a nyitvatartási idők megadása meglehetősen bonyolult, ezért nem könnyű egyszerű és intuitív felhasználói felületet készíteni hozzá.

Azonban az esetek többségében csak egy kis részét használjuk a definícióknak. A szerkesztő ezt figyelembe véve megpróbálja menükbe rejteni a bonyolultabb funkcióka,t és az esetek többségében lecsökkenteni az általános használatot az előre definiált sablonok testreszabására.

_Ez a dokumentáció ideiglenes, és dolgozunk rajta_

## A nyitvatartás-szerkesztő használata

A szokásos munkamenet szerint a szerkesztett térképelemnek vagy már van egy nyitva tartást tartalmazó címkéje (opening_hours, service_times vagy collection_times), vagy az előbeállításnak a térképelemre történő újraalkalmazásával nyithat egy üres nyitva tartási mezőt. Ha a mezőt kézzel kell hozzáadnia és Vespuccit használ, akkor a kulcsot megadhatja a részletes adatok oldalán, majd visszakapcsolhat az űrlapalapú fülre a szerkesztéshez. Ha úgy véli, hogy a nyitva tartási címkének az előbeállításban is szerepelnie kellene, kérjük, nyisson egy kérdést (issue) a szerkesztőjéhez.

Ha meghatározott egy alapértelmezett sablont (ezt a „Sablonok kezelése” menüpontban teheti meg), akkor az automatikusan be fog töltődni, amikor a szerkesztő egy üres értékkel indul. A „Sablon betöltése” funkcióval bármilyen sablont betölthet, a „Sablon mentése” menüvel pedig sablonként elmentheti az adott értéket. Az „opening_hours”, a „collection_times” és a „service_times” címkékre külön sablonokat és alapértelmezéseket határozhat meg.

Természetesen a semmiből is elkezdheted összeállítani a nyitva tartást, azonban azt talácsoljuk, hogy kiindulási pontként használd valamelyik sablont.

Amikor egy meglévő nyitva tartás betöltődik, akkor történik egy automatikus javítási kísérlet, hogy megfeleljen a nyitva tartási idők specifikációjának. Ha ez nem lehetséges, akkor a hiba körülbelüli helye kiemelődik a nyers nyitva tartás kijelzőjén, és megpróbálhatod kézzel kijavítani. Az OpenStreetMap adatbázisában a nyitva tartási értékek mintegy negyede hibás, azonban csak kevesebb mint 10% -uk nem javítható. A specifikációtól való megtűrt eltérések: [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser)

### Főmenü gomb

* __Szabály hozzáadása__: új szabály hozzáadása.
* __Szabály hozzáadása szünnapokra__: hozzáad egy szabályt munkaszüneti napokra és egy állapotváltozást.
* __Éjjel-nappali (24/7, non-stop) szabály hozzáadása__: Szabály hozzáadása egy objektumhoz, ami mindig nyitva van, a nyitvatartás specifikációja nem támogatja további altulajdonásgok felvételét az éjjel-nappali értékhez, de mi megengedjük magasabb szintű értékek megadását (például évtartományok).
* __Sablon betöltése__: egy létező sablon betöltése.
* __Mentés sablonként__: az aktuális nyitvatartási idő értékek elmentése sablonként a későbbi használathoz.
* __Sablonok kezelése__: szerkesztés, például név módosítása vagy meglévő sablonok törlése.
* __Frissítés__: a nyitvatartási idő értékének újraelemzése.
* __Összes törlése__: az összes szabály eltávolítása

### Szabályok

Az alapértelmezett szabályok _rendes_ szabályként jelennek meg. Ez azzal jár, hogy felülírják az adott napra vonatkozó korábbi szabályok értékeit. Ez aggasztó lehet, ha kiterjesztett időket szeretnénk megadni. Ilyenkor tipikusan érdemes átkapcsolni a szabályokat a _Szabálytípus mutatása_ menüpontban _összeadó_ra.

#### Szabály menü

* __Módosító/megjegyzés hozzáadása__: módosítja a szabály hatását és opcionális megjegyzést ad hozzá.
* __Szünnap hozzáadása__: hozzáad egy választót, amellyel munkaszüneti napokat vagy tanítási szünetet választhatunk.
* __Időtartam hozzáadása…__
    * __Idő – idő__: nyitási és zárási időpont ugyanazon a napon.
    * __Idő – kiterjesztett idő__: kezdő időponttól egy a következő napon lévő zárási időpontig (pl. 26:00 = másnap hajnali 02:00),
    * __Változó idő – idő__: változó nyitási időponttól (hajnal, szürkület, napkelte és napnyugta) zárási időpontig ugyanazon a napon.
    * __Változó idő – kiterjesztett idő__: változó nyitási időponttól másnapi zárási időpontig.
    * __Idő – változó idő__: nyitási időponttól változó zárási időpontig.
    * __Változó idő – változó idő__: változó nyitási időponttól változó zárási időpontig.
    * __Idő__: egy időpont.
    * __Idő – örökké__: nyitási időponttól kezdve mindörökké.
    * __Változó idő__: a változó időpontban.
    * __Változó idő – örökké__: változó nyitási időponttól kezdve mindörökké.
* __Hét napjaiból álló tartomány hozzáadása__: egy a hét napjaiból álló kijelölő hozzáadása
* __Dátumtartomány hozzáadása…__
    * __Dátum – dátum__: egy nyitási dátumtól (év, hónap, nap) egy zárási dátumig.
    * __Változó dátum – dátum__: egy változó dátumtól (a specifikáció jelenleg csak a _húsvétot_ határozza meg) egy zárási dátumig.
    * __Dátum – változó dátum__: egy adott dátumtól egy változó dátumig.
    * __Változó dátum – változó dátum__: egy változó dátumtól egy változó dátumig.
    * __Előfordulás hónapban – előfordulás hónapban__: a hónap valahányadik hétnapjától valahányadi hétnapjáig (pl. második, harmadik és negyedik szerdáján).
    * __Előfordulás hónapban – dátum__: a hónap valamely hétnapjától egy befejező dátumig (pl. január harmadik keddjétől február 1-jéig).
    * __Dátum – előfordulás hónapban__: egy kezdő dátumtól a hónap valamely hétnapjáig (pl. március 1-jétől április második szerdájáig).
    * __Előfordulás hónapban – változó dátum__: a hónap valamely hétnapjától (pl. második péntekjétől) egy változó dátumig:
    * __Változó dátum – előfordulás hónapban__: egy változó időponttól a hónap valamely hétnapjáig (pl. negyedik vasárnapjáig).
    * __Dátum – örökké__: egy nyitási dátumtól kezdve mindörökké.
    * __Változó dátum – örökké__: egy változó dátumtól kezdve mindörökké.
    * __Előfordulás hónapban – örökké__: a hónap valamely hétnapjától (pl. első kedd) kezdődően.
    * __Eltolásokkal…__: a fentiekkel megegyező bejegyzések, azonban egy adott értékkel eltolva (ritka).
* __Évtartomány hozzáadása__: évalapú kijelölő hozzáadása.
* __Héttartomány hozzáadása__: hétalapú kijelölő hozzáadása.
* __Duplikálás__: másolat készítése erről a szabályról és beillesztése a következő helyre.
* __Szabálytípus megjelenítése__: a _rendes_, _kiegészítő_ és _helyettesítő_ szabálytípus megjelenítése és esetleges módosítása (az első szabálynál nem alkalmazható).
* __Mozgatás felfelé__: eggyel feljebb tolja a szabályt (az első szabálynál nem alkalmazható).
* __Mozgatás lefelé__: eggyel lejjebb tolja a szabályt.
* __Törlés__: törli a szabályt.

### Időtartamok

Szeretnénk a lehető legkönnyebbé tenni az időtartamok szerkesztését. A meglévő értékek betöltésénél ezért megpróbáltuk kiválasztani az optimális időtartamot és a tartománysáv finomságát. Új időtartam megadásánál a tartománysáv reggel 6:00 órakor indul és 15 perces a lépésköze. Ez menüben változtatható.

Clicking (not on the pins) the time bar will open the large time picker, when using the bars directly is too difficult. The time pickers extend in to the next day, so they are a simple way to extend a time range without having to delete and re-add the the range.

#### Időtartam menü

* __Display time picker__: show a large time picker for selecting start and end time, on very small displays this is the preferred way of changing times.
* __Váltás 15 perces lépésközre__: 15 perces finomságúra változtatja a tartománysávot.
* __Váltás 5 perces lépésközre__: 5 perces finomságúra változtatja a tartománysávot.
* __Váltás 1 perces lépésközre__: 1 perces finomságúra változtatja a tartománysávot; telefonon nagyon nehezen használható.
* __Éjfélkor indul__: a tartománysáv éjfélnél kezdődik.
* __Intervallum megjelenítése__: megjeleníti az intervallummezőt, ahol percben megadható, hogy milyen időközönként történik valami.
* __Törlés__: ennek az időtartamnak a törlése.

