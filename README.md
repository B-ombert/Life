Norbert Benko
Hra Život

Aplikácia predstavuje simuláciu hry "Život" (Conway's game of life) obohatenú o možné nové pravidlá ktoré si užívateľ môže zvoliť a vrámci možností aj modifikovať.
Taktiež obsahuje aj možnosti pre zjednodušenie používania ako napríklad zoznam pár predvytvorených patternov, či reset alebo modifikáciu sveta počas behu simulácie.

Po spustení aplikácie sa najskôr otvorí Setup okno, v ktorom je možné klikaním nastaviť počiatočný stav sveta, combo boxom cell type si uživateľ môže vybrať medzi 3 typmi buniek:
Prvý typ sú klasické s čiernou farbou, typ 2 sú červené, ktoré predstavujú istú formu biologickej konkurencie, a typ 3 je "stena" - bunky ktoré sú nesmrteľné a nevedia sa samé rozmnožiť
Užívateľ umiestni vybraný typ bunky do mriežky ľavým tlačidlom, pre zmazanie živej bunky klikne pravým tlačidlom.
Na ľavom paneli sú potom nasledujúce možnosti:
Pattern - používateľ si vyberie jeden z poskytnutých vzorov, nastaví súradnice pre ľavý horný roh a stlačením place umiestni pattern do mriežky, súradnice sú pre niektoré z nich posunuté tak, aby pri umiestnení
do ľavého horného rohu nenastávala oscilácia cez okraje mapy.
V ďaľšom riadku si môžete vybrať rozmery mapy a tiež čas medzi jednotlivými updatmi v simulácii, po zadaní nových rozmerov najprv kliknite na tlačidlo Update grid na spodnej časti okna
Zakliknutím tlačidla Enable age limit môžete určiť maximálny vek buniek typu 1 a 2 v počte krokov simulácie
Zakliknutím Enable random cell revival môžete simuláciu urobiť menej deterministickú povolením náhodnej šance že mŕtva bunka ožije sama, pomocou 2 sliderov môžete nastaviť pravdepodobnosť tejto udalosti
a taktiež pravdepodobnosť akého typu nová bunka bude.
Zostávajúcimi textovými oknami viete upraviť pravidlá týkajúce sa počtu živých buniek potrebných pre prežitie či ožitie bunky, typ 1 a 2 idú meniť nezávisle.
Tlačidlom start spustíte simuláciu a pri prvom spustení sa pôvodný stav uloží ak by ste chceli simuláciu reštartovať

Po spustení máte k dispozícii na pravom okraji tlačidlo pause/resume pre zastavenie a opätovné spustenie simulácie. Tlačidlom reset nastavíte simuláciu do pôvodného stavu, 
ak ste odvtedy zmenili rozmery sveta tak tie zostanú zmenené. Nakoniec tlačidlom config môžete znovu otvoriť Setup okno a zmeniť aktuálny stav, tlačidlom Save as original môžete prepísať pôvodný stav na súčasný.
