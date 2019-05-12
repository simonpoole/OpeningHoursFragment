# OpenStreetMapin aukioloaikamuokkain

OpenStreetMapin aukiloaikojen määrittely on melko monimutkainen eikä se suoralta kädeltä taivu yksinkertaiseksi ja intuitiiviseksi käyttöliittymäksi.

Toisaalta määrittelystä käytetään monesti vain pientä osaa. Muokkain ottaa tämän huomioon ja yrittää piilottaa oudoimmat piirteet valikoihin, jolloin tien päällä tarvitsee useimmiten tehdä vain pieniä muutoksia valmiisiin mallipohjiin.

_Tämä dokumentaatio on alustava ja työn alla._

## Aukioloaikamuokkaimen käyttö

Tyypillisessä tapauksessa muokattavana olevalla kohteella joko on jo aukioloaikatägi (opening_hours, service_times ja collection_times) tai siihen voi soveltaa uudelleen esivalinnan, joka lisää tyhjän aukioloaikakentän. Jos sinun täytyy lisätä kenttä käsin Vespuccia käyttäessäsi, niin voit lisätä avaimen yksityikohtasivulta ja palata sitten takaisin muokkaamaan lomakepohjaiselle välilehdelle. Jos esivalinnassa kuuluisi mielestäsi olla aukioloaikatägi, voit lähettää muutospyynnön käyttämäsi muokkaimen kehittäjille.

Jos olet määrittänyt oletusmallipohjan (valikosta "Mallipohjien hallinta"), niin se ladataan automaattisesti muokkainta avatessa, mikäli muokattava kenttä on tyhjä. Toiminnolla "Lataa mallipohja" voit ladata jonkin tallennetuista mallipohjista, ja toiminnolla "Tallenna mallipohja" voit tallentaa senhetkisen arvon mallipohjaksi. Tägeille "opening_hours", "collection_times" ja "service_times" voi kullekin määrittää erilliset mallipohjat ja oletusarvot.

Voit luonnollisesti rakentaa aukioloajan (aoa) tyhjästä, mutta suosittelemme jonkin valmiin mallineen käyttämistä pohjana.

Kun muokkaimeen ladataan olemassa oleva arvo, se yritetään korjata automaattisesti aukioloaikamäärittelyn mukaiseksi. Jos tämä ei ole mahdollista, virheen summittainen paikka raa'assa aoa-arvossa korostetaan, ja voit yrittää korjata sen käsin. Suurinpiirtein neljänneksessä OpenStreetMapin tietokannan aoa-arvoista on ongelmia, ja alle kymmentä prosenttia ei voi korjata. Sivulla [OpeningHourParser] (https://github.com/simonpoole/OpeningHoursParser) kerrotaan tarkemmin mitä poikkeamia määrittelystä sallitaan.

### Päävalikkonappi

* __Lisää sääntö__: lisää uusi sääntö.
* __Lisää sääntö juhlapäiville__: lisää uusi sääntö juhlapäiville ja sen tilamuutos.
* __Lisää sääntö 24/7__: lisää sääntö kohteelle, joka on aina auki; aukioloaikamäärittely ei tue alavalitsimien käyttämistä 24/7:n rinnalla, mutta tässä sallitaan korkeamman tason valitsimien lisääminen (esimerkiksi vuosijaksot).
* __Lataa malline__: lataa valmis malline.
* __Tallenna mallineeksi__: tallenna nykyinen aukioloaika-arvo mallineeksi tulevaa käyttöä varten.
* __Mallipohjien hallinta__: muokkaa (esim. vaihda nimi) ja poista mallipohjia.
* __Päivitä__: jäsennä aukioloaika-arvo uudelleen.
* __Poista kaikki__: poista kaikki säännöt.

### Säännöt

Oletussäännöt lisätään _tavallisina_ sääntöinä, mikä tarkoittaa sitä, että ne syrjäyttävät aiemmat samoille päiville osuvat säännöt. Tällä voi olla merkitystä, kun määritetään laajennettuja aikoja – tyypillisesti tällöin on syytä muuttaa säännöt _lisääviksi_ valikkokohdasta _Näytä sääntötyyppi_.

#### Sääntövalikko

* __Lisää määre/kommentti__: muuta säännön vaikutusalaa, ja lisää valinnainen kommentti.
* __Lisää vapaapäivä__: lisää valitsin yleiselle tai koulujen vapaapäivälle.
* __Lisää aikaväli...__
    * __Aika–aika__: alkuaika ja loppuaika samana päivänä.
    * __Aika–laajennettu aika__: alkuaika ja loppuaika eri päivinä (esim. 26:00 on 02:00 seuraavana päivänä).
    * __Vaiht. aika–aika__: vaihteleva alkuaika (aamu- tai iltahämärä, auringonnousu tai -lasku) ja loppuaika samana päivänä.
    * __Vaiht. aika–laajennettu aika__: vaihteleva alkuaika ja loppuaika peräkkäisinä päivinä.
    * __Aika–vaiht. aika__: alkuaika ja vaihteleva loppuaika.
    * __Vaiht. aika–vaiht. aika__: vaihteleva alkuaika ja vaihteleva loppuaika.
    * __Aika__: tietty ajankohta.
    * __Aika–avoin loppu__: tietystä ajankohdasta eteenpäin.
    * __Vaihteleva aika__: vaihtelevana ajankohtana.
    * __Vaihteleva aika–avoin loppu__: vaihtelevasta ajankohdasta eteenpäin.
* __Lisää viikonpäiväjakso__: lisää viikonpäiviin perustuva valitsin.
* __Lisää päiväjakso...__
    * __Pvm–pvm__: alku- ja loppupäivämäärä (vuosi, kk, päivä).
    * __Vaihteleva pvm–pvm__: vaihteleva alkupäivämäärä (määrittelyssä on toistaiseksi vain _pääsiäinen_) ja tietty loppupäivämäärä.
    * __Pvm–vaihteleva pvm__: alkupäivämäärä ja vaihteleva loppupäivämäärä.
    * __Vaihteleva pvm–vaihteleva pvm__: vaihteleva alku- ja loppupäivämäärä.
    * __Esiintymiskerta kuussa–esiintymiskerta kuussa__: alkaen viikonpäivän tietystä esiintymiskerrasta kuukauden aikana samaan esiintymäkertaan seuraavassa kuussa.
    * __Esiintymiskerta kuussa–päivämäärä__: alkaen viikonpäivän tietystä esiintymiskerrasta kuukauden aikana ja päättyen tiettyyn päivämäärään
    * __Päivämäärä–esiintymiskerta kuussa__: alkaen tietystä päivämäärästä ja päättyen viikonpäivän tiettyyn esiintymiskertaan kuukauden aikana.
    * __Esiintymiskerta kuussa–vaihteleva päivämäärä__: alkaen viikonpäivän tietystä esiintymiskerrasta kuukauden aikana ja päättyen vaihtelevaan päivämäärään.
    * __Vaihteleva päivämäärä–esiintymiskerta kuussa__: alkaen vaihtelevasta päivämäärästä ja päättyen viikonpäivän tiettyyn esiintymiskertaan kuukauden aikana.
    * __Päivämäärä–avoin loppu__: tietystä päivämäärästä eteenpäin.
    * __Vaihteleva pvm–avoin loppu__: vaihtelevasta päivämäärästä eteenpäin.
    * __Esiintymiskerta kuussa–avoin loppu__: viikonpäivän tietystä esiintymiskerrasta kuukauden aikana eteenpäin.
    * __Poikkeama...__: samat vaihtoehdot kuin edellä poikkeaman kanssa (harvoin käytetty).
* __Lisää vuosijakso__: lisää vuosiin perustuva valitsin.
* __Lisää viikkojakso__: lisää viikkonumeroihin perustuva valitsin.
* __Kahdenna__: tee kopio tästä säännöstä välittömästi sen perään.
* __Näytä sääntötyyppi__: näytä säännön tyyppi ja/tai muuta sitä: _tavallinen_, _lisäävä_ ja _vara_ (ei ensimmäiselle säännölle).
* __Siirrä ylöspäin__: siirrä sääntö yhtä paikkaa ylemmäs (ei ensimmäiselle säännölle)
* __Siirrä alaspäin__: siirrä sääntö yhtä paikkaa alemmas.
* __Poista__: poista sääntö.

### Ajanjaksot

Jotta ajanjaksojen muokkaaminen olisi mahdollisimman helppoa, säätöpalkkien laajuus ja tarkkuus yritetään optimoida, kun olemassa oleva arvo ladataan. Uusien ajanjaksojen säätimissä alkuaika on 6:00, ja niissä on 15 minuutin jaotus. Asetuksia voi muuttaa valikon kautta.

Clicking (not on the pins) the time bar will open the large time picker, when using the bars directly is too difficult. The time pickers extend in to the next day, so they are a simple way to extend a time range without having to delete and re-add the the range.

#### Ajanjaksovalikko

* __Display time picker__: show a large time picker for selecting start and end time, on very small displays this is the preferred way of changing times.
* __15 minuutin jaotus__: käytä säätöpalkissa 15 minuutin tarkkuutta.
* __5 minuutin jaotus__: käytä säätöpalkissa 5 minuutin tarkkuutta.
* __1 minuutin jaotus__: käytä säätöpalkissa 1 minuutin tarkkuutta – todella vaikeaa puhelimessa.
* __Aloita keskiyöstä__: aseta säätöpalkin alkuaika keskiyöhön.
* __Näytä aikaväli__: näytä kenttä, johon asetetaan toistuva aikaväli minuutteina.
* __Poista__: poista ajanjakso.

