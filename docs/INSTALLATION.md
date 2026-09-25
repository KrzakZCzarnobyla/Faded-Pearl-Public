# Instalacja Faded Pearl

## Wymagania

- Minecraft `1.20.1`;
- Forge `47.4.22`;
- Java `17`;
- GeckoLib `4.8.4`;
- SmartBrainLib `1.15`;
- plik Faded Pearl przeznaczony dla tej samej wersji gry i Forge.

Faded Pearl oraz obie biblioteki muszą znajdować się w katalogu `mods` po stronie klienta
i serwera. W grze wieloosobowej wszystkie osoby oraz serwer powinny używać dokładnie tego
samego JAR-a Faded Pearl. Nie należy mieszać kolejnych kandydatów beta w jednej sesji.

## Eksperymentalny multiplayer

Podstawowa rozgrywka wieloosobowa była testowana we wcześniejszym kandydacie z trzema
graczami, ale bieżące wydanie nie przeszło pełnej regresji restartów serwera, wylogowań,
recovery i przejść między wymiarami. Multiplayer jest dostępny eksperymentalnie, nie jako w
pełni zweryfikowane wsparcie serwerowe. Przed użyciem wykonaj kopię świata. Serwer i wszystkie
klienty muszą korzystać z dokładnie tego samego JAR-a moda.

## Nowy świat lub nowy serwer

1. Zamknij Minecrafta i serwer.
2. Usuń wszystkie starsze JAR-y Faded Pearl z aktywnego katalogu `mods` — pozostawienie dwóch
   wersji jednocześnie może uniemożliwić uruchomienie gry.
3. Umieść Faded Pearl, GeckoLib i SmartBrainLib w katalogu `mods`.
4. Uruchom grę lub serwer i sprawdź w menu modów, czy Faded Pearl został załadowany.
5. Na serwerze połącz klienta zawierającego ten sam zestaw wersji.

Konfiguracja świata powstaje w `serverconfig/faded_pearl-server.toml`. Opis ustawień znajduje
się w `docs/CONFIGURATION.md`. Plik najlepiej edytować przy wyłączonym świecie lub serwerze.

## Aktualizacja istniejącego świata

Kandydaci `beta` służą do testów i nie powinni być pierwszą wersją uruchamianą na jedynej
kopii ważnego świata.

1. Zamknij grę albo zatrzymaj serwer i poczekaj na zakończenie zapisu.
2. Skopiuj cały katalog świata w bezpieczne miejsce.
3. Zachowaj poprzedni JAR Faded Pearl poza katalogiem `mods`.
4. Usuń starszy JAR z każdego aktywnego katalogu `mods`, a następnie wstaw nowy JAR po stronie
   serwera i wszystkich klientów.
5. Pierwsze uruchomienie wykonaj na kopii świata.
6. Sprawdź obecność właściwego Fade'a, jego imię, zaufanie, dom, dziennik i przedmiot
   powierzony do obejrzenia.
7. W multiplayerze sprawdź osobno towarzysza każdej osoby, a następnie restart serwera
   i przejście przez portal.

Jeżeli Fade zniknie, pojawi się druga kopia tej samej postaci albo zostanie przypisany do
niewłaściwej osoby, nie zapisuj takiego stanu jako nowej kopii głównej. Zachowaj świat testowy
i logi, wróć do kopii zapasowej oraz zgłoś użyte wersje i przebieg zdarzeń.

## Usuwanie lub cofanie wersji

Nie należy usuwać moda ani wracać do starszego JAR-a na jedynej kopii świata, który został
już zapisany przez nowszą wersję. Starszy kod może nie rozumieć nowych danych relacji.
Bezpieczna droga powrotu to odtworzenie kopii świata wykonanej przed aktualizacją razem
z odpowiadającym jej zestawem modów.

## Stan wydania

Aktualny publiczny build, pliki do pobrania i informacje o zmianach znajdują się na stronie
[GitHub Releases](https://github.com/KrzakZCzarnobyla/Faded-Pearl-Public/releases). Przed
użyciem na ważnym świecie przeczytaj także [znane ograniczenia](KNOWN_LIMITATIONS.md).

## Weryfikacja pobranego pliku

Porównaj nazwę, rozmiar i SHA-256 z informacją podaną przy konkretnym wydaniu. Przygotowany
kandydat `faded_pearl-1.8.0-beta.3.jar` ma `1469680` bajtów i SHA-256
`8D10D32A53D121CD7101AB3125902B6053B1BF189AE0901AAC47170BC01D9A56`.

Windows PowerShell:

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath .\faded_pearl-1.8.0-beta.3.jar
```

Linux/macOS:

```bash
sha256sum faded_pearl-1.8.0-beta.3.jar
```
