# Changelog

Wszystkie istotne zmiany projektu będą dokumentowane w tym pliku.

## [Unreleased]

Brak jeszcze publicznie zapowiedzianych zmian po `1.8.0-beta.3`.

## [1.8.0-beta.3] — publiczna beta

Kandydat łączy zawartość zamrożonego etapu 1.7 z poprawkami trwałości, migracji, oprawy i
pakowania. Świeży singleplayer oraz migracja tymczasowego świata z `1.4.1` przeszły testy
leczenia, Save/Quit, komend, kotwicy, przejść wymiarowych i recovery. Końcowego logu po dwóch
ostatnich próbach migracyjnych nie sprawdzano; pierwszy log migracji był czysty.

Multiplayer pozostaje **eksperymentalny**. Wcześniejsza sesja trzech graczy potwierdziła
podstawową pętlę, lecz beta.3 nie przeszła kompletnej regresji serwera dedykowanego obejmującej
restart, wylogowanie, recovery i podróże wymiarowe. Przed grą wieloosobową wykonaj kopię świata
i używaj identycznego JAR-a na serwerze oraz wszystkich klientach.

### Added

- struktura dokumentacji i pamięci projektu;
- zasady pracy dla tasków koordynacyjnych i wykonawczych.
- niezależny towarzysz, zaufanie, dziennik, dom i recovery dla każdego gracza;
- naturalne spotkania kolejnych rannych Fade'ów i własność przypisywana przez leczenie;
- neutralne reakcje na obcych graczy oraz autonomiczne spotkania dwóch Fade'ów;
- serwerowa konfiguracja spotkań, zaufania, recovery i nagrody kotwicy;
- trwała Perła uniku chroniąca przed przypadkowym ciosem właścicielki;
- instrukcje ścieżki leczenia, receptury dziennika i odświeżone tekstury pereł.
- Rezonującą kotwicę, Dziennik Endermana, trwałą Perłę uniku, zachowania ciekawości,
  podnoszenie małych zwierząt oraz prywatne relacje wielu graczy;
- serwerową konfigurację spotkań, zaufania, nagrody kotwicy i czasu recovery.

### Fixed

- imię Endermana jest rysowane wyżej nad głową, żeby nie zasłaniało twarzy; etykiety powalenia i regeneracji pozostają na swoich miejscach;
- progresja wyglądu zaufania otrzymuje synchronizowaną wartość klientową; tekstura i światło oczu/perły mogą teraz wybierać właściwy z ośmiu progów podczas gry;
- model GeckoLib dodaje rotację wzroku encji do animowanej kości głowy, zachowując animację i blokadę obrotu siedzącego ciała;
- ranny Enderman utrzymuje gracza jako nadrzędny cel wzroku podczas płaczu i nie przenosi przypadkowo uwagi na pobliskie zwierzęta.
- śledzenie wzrokiem uzdrowionej postaci jest oddzielone od animowanej kości głowy, co zapobiega kumulowaniu obrotu podczas ruchu i reakcji emocjonalnych.
- komentarze świata zachowują odstęp po dialogu dotyku i nie mówią bezpośrednio po jawnej interakcji gracza.
- recovery odrzuca obcą lub niepełną kopię przed wyborem epoki i bezpiecznie migruje częściowe
  albo źle typowane starsze dane;
- poprawiono mocowanie palców w animacjach, nie zmieniając geometrii ani mechanik;
- końcowy JAR otrzymał automatyczny kontrakt metadanych, zależności i wymaganych zasobów.

## [1.4.1] - 2026-08-19

### Added

- system zaufania i reakcje zależne od jego poziomu;
- detektory gestów, pogody i otoczenia;
- stan powalenia oraz ratowanie gracza;
- losowe dialogi polskie i angielskie;
- animacja biegu i kolorowana glowmaska;
- cooldown prezentów kwiatowych;
- rozszerzona logika kotwicy, podróżowania i ochrony.
