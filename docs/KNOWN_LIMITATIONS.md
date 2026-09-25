# Znane ograniczenia Faded Pearl 1.8.0-beta.3

Ten dokument odróżnia potwierdzone problemy od zakresu, którego jeszcze nie przetestowano.

## Eksperymentalny multiplayer

Podstawowa pętla wielu osobnych relacji została wcześniej potwierdzona w sesji trzech osób,
ale `1.8.0-beta.3` nie przeszła pełnej regresji serwera dedykowanego obejmującej restart,
wylogowanie jednej osoby, równoczesne recovery oraz przejścia wymiarowe kilku właścicielek.
Nie jest to potwierdzony błąd, tylko niewykonany zakres testów. Przed grą wykonaj kopię świata
i używaj identycznego JAR-a na serwerze oraz wszystkich klientach.

## Zgodność z innymi modami

Kod nie wymaga JEI, Jade ani WTHIT do uruchomienia. Historyczny test JEI potwierdził
widoczność receptur we wcześniejszym kandydacie, lecz pełna macierz runtime dokładnych wersji
viewerów, nakładek, transportu i modów serializujących encje pozostaje otwarta dla beta.3.
Brak wpisu na liście nie oznacza potwierdzonej niezgodności ani potwierdzonego wsparcia.

## Wydajność

Automatyczne testy i zwykła rozgrywka singleplayer nie wykazały awarii, ale porównawczy profil
`0/1/10/25` aktywnych Fade'ów nie został jeszcze zaakceptowany. Duże serwery powinny traktować
wydanie jako betę i obserwować TPS/MSPT, szczególnie przy wielu aktywnych relacjach.

## Migracja

Tymczasowy świat z wersji `1.4.1` zachował jedną postać, relację, Save/Quit, Perłę uniku,
przejścia wymiarowe i recovery. Nie zapisano jednak kompletnego baseline każdego opcjonalnego
pola przed pierwszym otwarciem, a końcowego logu po portalach i ostatnim recovery nie sprawdzono
na prośbę właścicielki projektu. Pierwszy log migracji był czysty. Aktualizację ważnego świata
nadal należy najpierw wykonać na kopii.

## Zgłaszanie problemu

Przy zgłoszeniu podaj wersje Minecrafta, Forge, GeckoLib, SmartBrainLib i Faded Pearl, tryb
singleplayer/serwer, opis ostatnich czynności oraz `latest.log`. Przy utracie lub duplikacji
Fade'a zachowaj kopię świata i nie wykonuj kolejnych prób naprawy na jedynym ważnym zapisie.
