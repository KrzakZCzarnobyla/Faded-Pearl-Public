# Konfiguracja Faded Pearl

Od wersji 1.7 najważniejsze parametry serwerowe znajdują się w pliku świata `serverconfig/faded_pearl-server.toml`. Na serwerze dedykowanym konfigurację zmienia administratorka; w singleplayerze plik należy do konkretnego świata. Edytuj go przy wyłączonym świecie lub serwerze; wartości są wczytywane przy następnym uruchomieniu świata.

Wartości domyślne zachowują dotychczasowe zachowanie moda.

## Spotkania w jaskiniach

| Klucz | Domyślnie | Zakres | Znaczenie |
|---|---:|---:|---|
| `encounters.enabled` | `true` | `true/false` | Włącza naturalne pojawianie się rannych Fade'ów. |
| `encounters.checkIntervalTicks` | `200` | `20–72000` | Odstęp między próbami; 20 ticków to sekunda. |
| `encounters.maximumY` | `45` | `-64–320` | Gracz musi znajdować się na tej wysokości lub niżej. |
| `encounters.minimumHorizontalSpacing` | `100` | `32–1024` | Minimalny poziomy odstęp między zapisanymi miejscami spotkań. |

Reguła jednej lokalnie połączonej części jaskini nadal obowiązuje niezależnie od minimalnego odstępu.

## Zaufanie

| Klucz | Domyślnie | Zakres | Znaczenie |
|---|---:|---:|---|
| `trust.gainPercent` | `100` | `0–500` | Procentowa skala dodatnich zmian zaufania. `0` wyłącza przyrost. |
| `trust.lossPercent` | `100` | `0–500` | Procentowa skala kar. `0` wyłącza utratę punktów. |
| `trust.anchorRecallReward` | `1` | `0–10` | Bazowa nagroda za skuteczne przywołanie kotwicą, przed skalą przyrostu. |

Niezerowy procent zachowuje co najmniej jeden punkt dla zdarzenia, aby małe reakcje nie znikały przez zaokrąglenie.

## Recovery

| Klucz | Domyślnie | Zakres | Znaczenie |
|---|---:|---:|---|
| `recovery.graceSeconds` | `30` | `5–300` | Czas, przez który system czeka, zanim uzna brakującego towarzysza za wymagającego odtworzenia. |

Zbyt niski czas zwiększa ryzyko reakcji recovery podczas wolnego ładowania wymiaru. Zakres konfiguracji celowo nie pozwala ustawić mniej niż 5 sekund.
