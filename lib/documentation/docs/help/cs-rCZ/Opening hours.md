# Editor otevírací doby OpenStreetMap

Určení otevírací doby OpenStreetMap je poměrně složité a není vhodné pro jednoduché a intuitivní uživatelské rozhraní.

Většinu času však budete pravděpodobně používat pouze malou část definice. Editor to vezme v úvahu tím, že se snaží skrýt nejasnější funkce v nabídkách a většinu času omezuje použití „na cestách“ na malé přizpůsobení předdefinovaných šablon.

_Tato dokumentace je předběžná a průběžně na ní probíhá práce_

## Pomocí editoru otevírací doby

V typickém pracovním postupu bude mít objekt, který upravujete, buď značku otevírací doby (opening_hours, service_times a collection_times), nebo můžete znovu použít předvolbu pro objekt a získat prázdné pole otevírací doby. Pokud potřebujete přidat pole ručně a používáte Vespucci, můžete zadat klíč na stránce s podrobnostmi a poté přepnout zpět na kartu založenou na formuláři a upravit. Pokud se domníváte, že značka otevírací doby měla být součástí předvolby, otevřete prosím problém svému editoru.

Pokud jste určili výchozí šablonu (proveďte to pomocí položky nabídky "Spravovat šablony"), načte se automaticky při spuštění editoru s prázdnou hodnotou. Pomocí funkce "Načíst šablonu" můžete načíst libovolnou uloženou šablonu a pomocí nabídky "Uložit šablonu" můžete uložit aktuální hodnotu jako šablonu. Pro konkrétní klíč můžete definovat samostatné šablony a výchozí hodnoty, například „opening_hours“, „collection_times“ a „service_times“ nebo vlastní hodnoty. Dále můžete omezit použitelnost šablony na region a konkrétní identifikátor, obvykle značku nejvyšší úrovně OSM (například amenity=restaurant).

Hodnotu otevírací doby můžete samozřejmě vytvořit od nuly, ale jako výchozí bod doporučujeme použít jednu ze stávajících šablon.

Pokud je načtena existující hodnota otevírací doby, provede se pokus o její automatickou opravu, aby odpovídala specifikaci otevírací doby. Pokud to není možné, hrubé místo, ve které došlo k chybě, bude zvýrazněno na displeji hrubé hodnoty OH a můžete se pokusit o opravu ručně. Zhruba čtvrtina hodnot OH v databázi OpenStreetMap má problémy, ale méně než 10% nelze opravit, podívejte se na [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) pro více informací o tom, jaké odchylky od specifikace jsou tolerovány.

### Tlačítko hlavní nabídky

* __Add rule__: přidat nové pravidlo.
* __Add rule for holidays__: přidat nové pravidlo na prázdniny společně se změnou stavu.
* __Add rule for 24/7__: přidat pravidlo pro objekt, který má stále otevřeno, určení otevírací doby nepodporuje žádné další dílčí hodnoty pro 24/7, avšak povolujeme přidávání selektorů na vyšší úrovni (například ročníků).
* __Load template__: načíst existující šablonu.
* __Save to template__: uložit hodnotu aktuální otevírací doby  jako šablonu pro budoucí použití.
* __Manage templates__: upravit, například změnit název nebo smazat existující šablonu.
* __Refresh__: znovu analyzujte hodnotu otevírací hodiny.
* __Delete all__: odstranit všechna pravidla.

### Pravidla

Výchozí pravidla jsou přidány jako _normal_ pravidla, to znamená, že budou přepisovat hodnoty předchozích pravidel na stejné dny. To může mít problém při zadávání prodloužených časů, obvykle pak budete chtít přepnout pravidla pomocí položky nabídky _Show rule type_ na _additive_.

#### Nabídka pravidel

* __Add modifier/comment__: změnit účinek tohoto pravidla a přidat volitelný komentář.
* __Add holiday__: přidat selektor pro veřejné nebo školní prázdniny.
* __Add time span...__
    * __Time - time__: čas začátku do času konce stejného dne.
    * __Time - extended time__: čas začátku a čas konce dalšího dne (například 26:00 jsou 02:00 (ráno) příštího dne).
    * __Var. time - time__: z počátečního proměnného času (svítání, soumrak, východ a západ slunce) do času konce stejného dne.
    * __Var. time - extended time__: od počátečního proměnného času do času ukončení následujícího dne.
    * __Time - var. time__: počáteční čas do konce proměnného času.
    * __Var. time - var. time__: počáteční proměnný čas do konce proměnného času.
    * __Time__: bod času.
    * __Time-open end__: od začátku času dále.
    * __Variable time__: v proměnném čase
    * __Variable time-open end__: od počátečního proměnného času dále
* __Add week day range__: přidat selektor na základě dne.
* __Add date range...__
    * __Date - date__: od počátečního data (rok, měsíc, den) do koncového data.
    * __Variable date - date__: od počátečního proměnného data (v současnosti specifikace definuje pouze _velikonoce_) do koncového data.
    * __Date - variable date__: od počátečního data do proměnného data.
    * __Variable date - variable date__: od počátečního proměnného data do koncového proměnného data.
    * __Occurrence in month - occurrence in month__: od výskytu počátečního dne v týdnu v měsíci do stejného.
    * __Occurrence in month - date__: od výskytu počátečního dne v týdnu v měsíci do koncového data.
    * __Date - occurrence in month__: od počátečního data do výskytu koncového dne v týdnu v měsíci.
    * __Occurrence in month - variable date__: od výskytu počátečního dne v týdnu v měsíci do koncového proměnného data.
    * __Variable date - occurrence in month__: od počátečního proměnného data do výskytu koncového dne v týdnu v měsíci.
    * __Date - open end__: od počátečního data dále.
    * __Variable date - open end__: od počátečního proměnného data dále.
    * __Occurrence in month - open end__: od výskytu počátečního dne v týdnu v měsíci dále.
    * __With offsets...__: stejné položky jako výše, avšak se specifikovanými offsety (toto se používá zřídka).
* __Add year range...__    
    * __Add year range__: přidat sektor na základě roku.
    * __Add starting year__: přidat období s otevřeným koncem roku.
* __Add week range__: přidat selektor na základě čísla týdne.
* __Duplicate__: vytvořit kopii tohoto pravidla a vložit ji později na aktuální pozici.
* __Show rule type__: zobrazit a umožnit měnění pravidla typu _normal_, _additive_ and _fallback_ (není dostupné pro první pravidlo).
* __Move up__: přesunout toto pravidlo o pozici výš (není dostupné pro první pravidlo).
* __Move down__: přesunout toto pravidlo o pozici níž.
* __Delete__: smazat toto pravidlo.

### Rozpětí času

Aby byly úpravy časových rozsahů co nejjednodušší, snažíme se při načítání existujících hodnot zvolit optimální časový rozsah a granularitu pro pruhy rozsahu. U nových časových úseků začínají pruhy v 6:00 (ráno) a mají 15minutové přírůstky, což lze změnit prostřednictvím nabídky.

Kliknutím (ne na špendlíky) na časovou lištu se otevře velký výběr času vždy, kdy je přímé použití lišt příliš obtížné. Výběry času se prodlužují do dalšího dne, takže představují jednoduchý způsob, jak rozšířit časový rozsah, aniž byste museli rozsah odstraňovat a znovu přidávat.

#### Nabídka rozpětí času

* __Display time picker__: zobrazí velký výběr času pro výběr počátečního a koncového času, na velmi malých displejích je to preferovaný způsob změny časů.
* __Switch to 15 minute ticks__: pro pruh rozsahu použijte 15minutovou granularitu.
* __Switch to 5 minute ticks__: pro pruh rozsahu použijte 5minutovou granularitu.
* __Switch to 1 minute ticks__: pro pruh rozsahu použijte 1minutovou granularitu.
* __Start at midnight__: spustí pruh rozsahu o půlnoci.
* __Show interval__: zobrazí pole intervalu pro zadání intervalu v minutách.
* __Delete__: smazat toto rozpětí času.

### Spravovat šablony

Dialog pro správu šablon umožňuje přidávat, upravovat a odstraňovat šablony.

V systému Android 4.4 a novějším jsou z tlačítka nabídky dostupné následující doplňkové funkce.

* __Show all__: zobrazí všechny šablony z databáze.
* __Save to file__: zapíše obsah databáze šablon do souboru.
* __Load from file (replace)__: načíst šablony ze souboru nahrazením současného obsahu z databáze.
* __Load from file__: načíst šablony ze souboru zadržováním současného obsahu.

#### Uložit a upravit dialogy šablon

Dialogové okno umožňuje nastavit

* __Name__ popisný název šablony.
* __Default__ pokud je zaškrtnuto, bude to považováno za výchozí šablonu (obvykle dále omezenou ostatními poli).
* __Key__ klíč, pro který je tato šablona relevantní, pokud je nastaven na _Custom key_, můžete do pole níže přidat nestandardní hodnotu. Hodnoty klíče podporují zástupné znaky SQL, tj. _%_ odpovídá nule nebo více znakům, *_* odpovídá jednomu znaku. Oba zástupné znaky mohou být uvozeny pomocí _\\_ pro doslovné shody.
* __Region__ kraj, na který se šablona vztahuje.
* __Object__ řetězec specifický pro aplikaci, který se má použít pro párování.

