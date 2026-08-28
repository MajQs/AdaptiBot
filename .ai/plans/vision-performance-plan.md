# Plan: optymalizacja czasu wykonania zapytań Vision (ByImage)

> Status: **propozycja / do dyskusji** — nic jeszcze nie zaimplementowane.
> Zakres: moduł `com.adaptibot.vision` + `infrastructure.ScreenCapture` + model skryptu (`Target.AtImage`, `Matcher.ImagePresent`) + UI edytora kroku.

## 1. Kontekst i założenia

Ustalenia z analizy i rozmowy:

| # | Założenie | Konsekwencja dla planu |
|---|-----------|------------------------|
| 1 | Duże rozdzielczości (2K/4K), środowisko: gry | Koszt zdominowany przez `matchTemplate` na pełnym ekranie |
| 2 | Wzorce małe, ~50×50 px | Bardzo duża macierz wynikowa (~8 mln pozycji przy 4K) |
| 3 | Element **często nie występuje** na ekranie | Ścieżka „nie znaleziono” jest ścieżką dominującą i **zawsze kosztuje pełne przeszukanie** — nie da się jej skrócić early-exitem |
| 4 | Zwykle 1 wystąpienie (rzadko 2) | Wystarczy `minMaxLoc`, nie trzeba szukać wielu maksimów |
| 5 | Lokalizacja często stała (HUD/UI), czasem trzeba przeszukać ~80% ekranu | Potrzebna **deklaracja zachowania obiektu przez użytkownika**, nie jedna heurystyka |
| 6 | Brak produkcyjnych skryptów | **Nie utrzymujemy kompatybilności wstecznej** formatu JSON |
| 7 | Wiele monitorów | Model ekranu musi operować na virtual desktop, nie `Toolkit.screenSize` |
| 8 | Zaczynamy od **pomiarów** | Krok 0 to instrumentacja, nie optymalizacja |

### Aktualny koszt jednego `find(ByImage)` (hipoteza do potwierdzenia pomiarem)

`VisionFinder.find()` przy każdym wywołaniu wykonuje:

1. `Robot.createScreenCapture` całego ekranu → ~25 MB `BufferedImage` (4K)
2. `ImageEncoder.decodeFromBase64` → **dekodowanie PNG wzorca przy każdym wywołaniu** (brak cache)
3. `bufferedImageToMat` × 2 → konwersja typu przez `Graphics.drawImage` (pełna kopia 25 MB) + `Mat.put` (kolejna kopia przez JNI)
4. `Imgproc.matchTemplate` TM_CCOEFF_NORMED, 3 kanały, pełny ekran
5. `minMaxLoc`

Punkt 4 to prawdopodobnie 60–85% czasu, punkty 1+3 razem 15–30%, punkt 2 kilka procent — **ale to musi potwierdzić pomiar (Krok 0)**.

### Kluczowa obserwacja projektowa

Skoro element „często go nie ma”, to **jedyne realne przyspieszenie to zmniejszenie obszaru wejściowego (ROI)** — i to na poziomie *przechwytywania* ekranu, a nie kadrowania po fakcie. Wycięcie ROI po zrzucie całego ekranu oszczędza tylko koszt matchowania; przechwycenie od razu regionu oszczędza także zrzut i konwersje.

Kto ma dostarczyć tę informację? Aplikacja nie jest w stanie sama zgadnąć, gdzie warto szukać — ale użytkownik **wie, jak zachowuje się obiekt w jego grze**. Dlatego osią planu jest deklaracja zachowania obiektu (sekcja 2), z której silnik *wyprowadza* strategię przeszukiwania.

---

## 2. Projekt docelowy — `ElementLocation`

Kluczowa decyzja projektowa: **typ opisuje zachowanie szukanego obiektu, a nie strategię przeszukiwania.**
Użytkownik deklaruje fakt o świecie („ten obiekt zawsze jest w tym samym miejscu”), a wybór optymalizacji jest tego *konsekwencją*, wyprowadzoną przez aplikację. Nazewnictwo w kodzie celowo idzie za językiem użytkownika (ubiquitous language), a nie za mechaniką OpenCV.

Nowy typ w modelu skryptu (serializowany, `kind` discriminator jak reszta):

```
ElementLocation
├── Fixed                          // "obiekt zawsze jest w tym samym miejscu"
├── WithinArea(bounds: ScreenRect) // "obiekt może pojawić się tylko w tym obszarze"
└── Anywhere                       // "obiekt może pojawić się gdziekolwiek"   [domyślne]
```

Mapowanie deklaracji użytkownika na strategię wykonania (to już wyłącznie sprawa modułu vision):

| Deklaracja użytkownika | Wariant | Co robi silnik |
|------------------------|---------|----------------|
| „Obiekt zawsze jest w tym samym miejscu” | `Fixed` | Pierwsze wywołanie skanuje cały virtual desktop; po trafieniu zapamiętuje pozycję i **każde kolejne wywołanie sprawdza wyłącznie zapamiętane okno** (`rozmiar wzorca + 2 × PIN_MARGIN_PX`). Brak trafienia → `null`, **bez ponownego skanu**. |
| „Obiekt może pojawić się tylko w wybranym obszarze” | `WithinArea` | Przechwytuje i przeszukuje wyłącznie wskazany prostokąt. |
| „Obiekt może pojawić się gdziekolwiek” | `Anywhere` | Przeszukanie **całego virtual desktop**, niezależnie od liczby monitorów. Zachowanie dzisiejsze, tryb domyślny. |

Dlaczego to działa lepiej niż wybór „gdzie szukać”:

* Użytkownik odpowiada na pytanie, na które **zna odpowiedź** (jak zachowuje się obiekt w jego grze), a nie na pytanie o wydajność, o której nie ma pojęcia.
* Optymalizacja dzieje się **nieświadomie, ale w oparciu o jawną deklarację** — nie ma tu zgadywania ani magii ze strony aplikacji.
* Konsekwencje trybu `Fixed` (brak rescanu) przestają być „dziwną opcją techniczną”, a stają się logicznym następstwem tego, co użytkownik sam zadeklarował: skoro obiekt zawsze jest w tym samym miejscu, to brak go tam znaczy, że nie ma go w ogóle.
* Naturalnie wpina się w istniejący przepływ: *kliknij w obrazek → zaznacz obrazek na ekranie → wybierz jak zachowuje się obiekt → zapisz*.

Świadomie **brak** konfigurowalnego marginesu i **brak** polityki „co, gdy nie znajdę” — obie rzeczy tylko rozmywałyby deklarację i mnożyły opcje w UI. Margines to stała implementacyjna:

```
// w kodzie modułu vision, nie w modelu skryptu
private const val PIN_MARGIN_PX = 8
```

Tryb `Fixed` rozwiązuje dominujący przypadek („elementu zwykle nie ma”): koszt nieznalezienia spada z pełnego skanu 4K do sprawdzenia okna ~66×66 px.

### Zmiany API

```
VisionQuery.ByImage(pattern: ImagePattern, location: ElementLocation)
VisionQuery.ByText(text: String, location: ElementLocation)   // spójność, wykorzystamy później dla OCR
MatchDataDto(coordinate: Coordinate /* ABSOLUTNE, virtual-desktop */, confidence: Double)
```

`ElementLocation` mieszka w modelu skryptu (obok `Target.AtImage` / `Matcher.ImagePresent`), a nie w module vision — vision tylko go konsumuje i tłumaczy na ROI.

---

## 3. Kroki realizacji

### Krok 0 — Instrumentacja i baseline *(najpierw, bez zmian zachowania)*

1. `VisionMetrics` — pomiar per faza: `capture`, `templateDecode`, `toMat`, `matchTemplate`, `minMaxLoc`, `total`; `System.nanoTime()`.
2. Log na poziomie DEBUG w formacie jednolinijkowym + agregacja (count / avg / p50 / p95 / max) wypisywana na koniec wykonania skryptu.
3. **Wynik kroku:** tabela baseline zebrana z realnego uruchomienia aplikacji, która zdecyduje o kolejności kroków 4–6.

*Kryterium akceptacji:* mamy liczby dla każdej fazy; wiemy, ile realnie zabiera capture, a ile matchowanie.

#### Wynik: zmierzony baseline (16 zapytań `ByImage`, wzorzec w lewym górnym rogu)

```
  PHASE              COUNT       AVG       P50       P95       MAX
  CAPTURE               16    123.90    121.81    135.46    139.51
  TEMPLATE_DECODE       16      5.52      5.32      6.36      6.70
  TO_MAT                32      1.53      0.03      3.56      9.81
  MATCH_TEMPLATE        16    288.44    285.79    298.03    317.23
  MIN_MAX_LOC           16     35.73     34.28     40.67     42.98
  TOTAL                 16    459.73    454.85    485.85    495.79
```

Wnioski, które zmieniają plan:

1. **~460 ms na jedno zapytanie.** Hipoteza o dominacji `matchTemplate` potwierdzona (288 ms, 63%), ale **`CAPTURE` okazał się drugim kosztem (124 ms, 27%)** — więcej, niż zakładałem. To potwierdza kluczową decyzję: ROI musi być stosowany **przy przechwytywaniu**, nie przez kadrowanie po fakcie.
2. **Fazy zależne od powierzchni ROI: `CAPTURE` + `MATCH_TEMPLATE` + `MIN_MAX_LOC` = 448 ms z 460 ms (97%).** Cała optymalizacja przez zawężenie obszaru atakuje właściwie cały koszt.
3. **`MIN_MAX_LOC` (36 ms) też skaluje się z obszarem** — przeszukuje macierz wyniku. Wcześniej go nie doceniałem.
4. **`TEMPLATE_DECODE` (5,5 ms) jest stały i nie zależy od ROI.** Po wdrożeniu ROI stanie się **dominującym kosztem** wariantu `Fixed` (~5,5 ms z ~6 ms całości). Dlatego **cache zdekodowanego wzorca (5.1) awansuje przed Krok 6** — bez tego `Fixed` zatrzyma się na ~6 ms zamiast ~0,5 ms.
5. **`TO_MAT` (1,5 ms avg, P50 0,03 ms)** — rozkład dwumodalny: konwersja wzorca jest darmowa, konwersja zrzutu kosztuje ~3–10 ms. Po ROI stanie się pomijalna; punkty 5.3/5.4 spadają w priorytecie.
6. Uwaga na marginesie: jedna akcja użytkownika dała **16 zapytań** — to efekt pętli wykonania skryptu. Przy 460 ms/zapytanie pętla robi ~2 iteracje/s; po optymalizacji `Fixed` będzie to rząd setek iteracji/s, co może wymagać osobnego spojrzenia na throttling pętli.

Prognoza po wdrożeniu ROI (do weryfikacji w Kroku 7): `Fixed` ≈ 0,5–1 ms (po cache wzorca), `WithinArea 400×300` ≈ 8–12 ms — czyli **~50–500× szybciej** niż dzisiejsze 460 ms.

### Krok 1 — Model ekranu wielomonitorowego

1. `ScreenCapture` przepisany na `GraphicsEnvironment.getScreenDevices()`:
   * `screens(): List<ScreenInfo>` (id, bounds w koordynatach virtual desktop, scale factor),
   * `virtualBounds(): ScreenRect`,
   * `capture(rect: ScreenRect): BufferedImage` na koordynatach absolutnych virtual desktop (obsługa ujemnych X/Y, gdy monitor jest „po lewej”).
2. Usunięcie `Toolkit.getDefaultToolkit().screenSize` z `TargetCoordinateResolver.atCoordinate` (dziś waliduje tylko monitor główny → błędne `CoordinateOutOfBounds` na drugim monitorze).
3. **Diagnostyka DPI zamiast pytania do użytkownika.** Przy starcie logujemy dla każdego monitora: bounds, `defaultTransform.scaleX/scaleY` (czyli skalowanie Windows 100/125/150%) oraz faktyczny rozmiar zrzutu z `Robot`. Jeśli rozmiar zrzutu ≠ bounds → skalowanie jest aktywne i trzeba je uwzględnić (przeliczanie koordynat albo wymuszenie `-Dsun.java2d.uiScale=1` / manifest DPI-aware). Decyzję podejmujemy **na podstawie logu z realnej maszyny**, nie na podstawie deklaracji.
   *Dlaczego to ważne:* przy skalowaniu ≠100% `Robot` widzi piksele fizyczne, a mysz klika w koordynatach logicznych — element zostaje znaleziony, ale kliknięcie idzie obok. To realne źródło objawu „widzi, a nie trafia”.

*Kryterium akceptacji:* zrzut i klik trafiają poprawnie na obu monitorach; test manualny + log z `screens()`.

#### Wynik diagnostyki na maszynie deweloperskiej

```
Detected 1 screen(s), virtual desktop bounds: 1536x960@(0,0)
  screen \Display0 (primary): bounds=1536x960@(0,0), scale=2.5x2.5
```

To zmienia obraz sytuacji w dwóch miejscach:

1. **Skalowanie Windows wynosi 250%** — ekran fizyczny to 3840×2400, przestrzeń logiczna (ta, w której działa mysz) to 1536×960. Sonda `logCaptureSpaceProbe()` rozstrzygnęła, w której z nich pracuje `Robot`:

   ```
   Capture space probe: requested 64x64, got 64x64, resolution variants=[64x64, 160x160]
   Robot captures in LOGICAL pixels (downsampled from device pixels)
   ```

   Wariant wysokorozdzielczy (160×160 = 64 × 2,5) potwierdza, że `Robot` przechwytuje w pikselach fizycznych, ale **zwraca obraz przeskalowany do pikseli logicznych**. Zrzut, wzorzec i koordynaty myszy żyją więc w jednej przestrzeni — **ryzyko DPI zamknięte, żadna konwersja nie jest potrzebna**. ROI w Kroku 3 operuje bezpośrednio na koordynatach logicznych.

   Porównanie samych rozmiarów zrzutu było tu bezużyteczne (`Robot` zawsze zwraca żądany rozmiar), dlatego pierwotne ostrzeżenie zastąpiłem sondą multi-resolution.
2. **Realna przestrzeń przeszukiwania jest 6,25× mniejsza, niż zakładałem.** Przy `Anywhere` to ~1,4 mln pozycji (1536×960), a nie 8 mln. Zmierzone 460 ms dotyczy więc *mniejszego* obrazu — tabela szacunków z sekcji 4 była policzona dla 4K w pikselach fizycznych i jest zbyt optymistyczna co do skali problemu, ale **proporcje między wariantami pozostają bez zmian** (i to one decydują o zysku).

Przeliczone oczekiwania dla tej maszyny (przestrzeń logiczna 1536×960, wzorzec 50×50):

| Wariant | Obszar | Pozycji | Udział powierzchni | Szacowany czas |
|---------|--------|---------|--------------------|----------------|
| `Anywhere` | 1536×960 | ~1 400 000 | 100% | **460 ms (zmierzone)** |
| `WithinArea` 400×300 | 400×300 | ~88 000 | 6,3% | ~30 ms |
| `WithinArea` 200×150 | 200×150 | ~15 000 | 2,0% | ~11 ms |
| `Fixed` (66×66) | 66×66 | ~289 | 0,02% | ~6 ms → **~0,5 ms po cache wzorca** |

Uwaga: przy `Fixed` bez cache wzorca (5.1) zatrzymamy się na ~6 ms, z czego 5,5 ms to samo dekodowanie Base64+PNG. To potwierdza awans punktu 5.1 w kolejności prac.

Drugi monitor nie był podłączony w chwili pomiaru (`Detected 1 screen(s)`) — logika `virtualBounds()` pozostaje poprawna, ale weryfikację wielomonitorową trzeba powtórzyć z podpiętym drugim ekranem.

### Krok 2 — Wprowadzenie `ElementLocation` do modelu i serializacji

1. Nowe typy: `ScreenRect`, `ElementLocation` (3 warianty; `Fixed` i `Anywhere` bez pól) w `script.value`.
2. `Target.AtImage` i `Matcher.ImagePresent` dostają pole `location: ElementLocation = Anywhere`.
3. Serializacja `kotlinx.serialization`. Dzięki domyślnemu `Anywhere` istniejące pliki wczytują się bez zmian.

*Kryterium akceptacji:* skrypt z każdym z 3 wariantów zapisuje się i wczytuje.

### Krok 3 — `VisionFinder` z ROI (rdzeń zysku)

1. `SearchAreaResolver` — tłumaczy `ElementLocation` na konkretny `ScreenRect` do przechwycenia:
   * `Anywhere` → `ScreenCapture.virtualBounds()`,
   * `WithinArea` → `bounds` przycięte do virtual desktop,
   * `Fixed` → wpis z cache rozszerzony o `PIN_MARGIN_PX`, a przy braku wpisu → `virtualBounds()` (pierwszy przebieg).
   To jedyne miejsce, w którym deklaracja użytkownika zamienia się w strategię techniczną.
2. `VisionFinder.find()`:
   * przechwytuje **tylko ROI** (`ScreenCapture.capture(roi)`),
   * matchuje,
   * **przelicza wynik na koordynaty absolutne** (`roi.x + localX`) — jeden, centralny punkt translacji.
3. Brak jakiegokolwiek fallbacku/rescanu — `Fixed` bez trafienia zwraca `null` od razu (log DEBUG + metryka „fixed miss”, żeby dało się zauważyć błędną deklarację użytkownika).
4. Guard: gdy `roi` mniejszy od wzorca → jasny błąd walidacyjny, nie ciche `null`.

*Kryterium akceptacji:* trafienia w ROI zwracają poprawne koordynaty absolutne (weryfikacja manualna: ROI w środku ekranu, przy krawędzi, na drugim monitorze o ujemnym X); pomiar z Kroku 0 pokazuje spodziewany spadek czasu.

### Krok 4 — Cache lokalizacji dla wariantu `Fixed`

1. `PatternLocationCache`: `ConcurrentHashMap<PatternKey, Coordinate>` (`PatternKey` = hash `base64Data`, bo wzorzec nie ma dziś stabilnego ID).
2. Zapis **tylko przy pierwszym** trafieniu z `confidence >= threshold`; potem wpis się nie zmienia (element z definicji stoi w miejscu).
3. Unieważnienie: nowe uruchomienie skryptu, zmiana konfiguracji ekranów. Bez TTL i bez licznika chybień — chybienie jest normalnym wynikiem, nie sygnałem o nieaktualnym cache.
4. Bezpieczeństwo wątkowe: dostęp z wątku wykonania **oraz** z wątku obserwatora → mapa współbieżna, brak współdzielonych `Mat`.
5. Ryzyko fałszywie pozytywnych trafień wewnątrz wąskiego ROI — mitygacja przez trzymanie progu `matchThreshold`.

*Kryterium akceptacji:* drugie i kolejne wywołania tego samego wzorca w wariancie `Fixed` są rzędu pojedynczych ms.

#### Wynik: pomiar po wdrożeniu ROI (`Fixed`, 27 zapytań)

```
  PHASE              COUNT       AVG       P50       P95       MAX
  CAPTURE               27     12.10      4.81     21.30    136.40
  TEMPLATE_DECODE       27      5.28      4.96      7.72      9.60
  TO_MAT                54      0.16      0.02      0.17      4.67
  MATCH_TEMPLATE        27     11.99      0.58      0.66    308.43
  MIN_MAX_LOC           27      1.37      0.02      0.02     36.53
  TOTAL                 27     32.18     11.17     25.76    507.14
```

Interpretacja: kolumna MAX to pierwsze wywołanie (pełny skan, 507 ms), kolumna P50 to stan ustalony po zapamiętaniu lokalizacji — **11,17 ms, czyli ~41× szybciej** niż baseline 460 ms.

Rozkład kosztu w stanie ustalonym całkowicie się przebudował:

| Faza | Baseline (P50) | Po ROI (P50) | Zmiana |
|------|----------------|--------------|--------|
| `MATCH_TEMPLATE` | 285,79 ms | 0,58 ms | **÷490** |
| `MIN_MAX_LOC` | 34,28 ms | 0,02 ms | ÷1700 |
| `CAPTURE` | 121,81 ms | 4,81 ms | ÷25 |
| `TEMPLATE_DECODE` | 5,32 ms | 4,96 ms | bez zmian |
| **`TOTAL`** | **454,85 ms** | **11,17 ms** | **÷41** |

Matchowanie przestało być wąskim gardłem. Nowe koszty dominujące to **dekodowanie wzorca (4,96 ms, ~45%)** i **`CAPTURE` (4,81 ms, ~43%)** — obie fazy są niemal niezależne od wielkości ROI. Dlatego punkt 5.1 wchodzi natychmiast.

Uwaga do `CAPTURE`: 4,8 ms na region 66×66 to praktycznie stały narzut `Robot.createScreenCapture`, a nie koszt powierzchni. Dalsza redukcja wymagałaby innego mechanizmu przechwytywania (Desktop Duplication API) — do backlogu.

#### Wynik: pomiar po dodaniu cache wzorca (Krok 5.1, `Fixed`, 40 zapytań)

```
  PHASE              COUNT       AVG       P50       P95       MAX
  CAPTURE               40     12.22      8.33     18.13    124.28
  TEMPLATE_DECODE       40      0.23      0.01      0.02      8.69
  TO_MAT                80      0.14      0.04      0.26      4.29
  MATCH_TEMPLATE        40      8.13      0.58      1.91    290.25
  MIN_MAX_LOC           40      0.88      0.02      0.06     34.20
  TOTAL                 40     22.93     11.37     20.46    473.42
```

`TEMPLATE_DECODE` spadło z 4,96 ms do **0,01 ms (÷500)** — cache działa zgodnie z oczekiwaniem. `TOTAL` P50 pozostało jednak na poziomie 11,37 ms zamiast spodziewanych ~6 ms, ponieważ w tym samym pomiarze `CAPTURE` wzrosło z 4,81 do 8,33 ms.

Wniosek: **`CAPTURE` jest już jedynym istotnym kosztem** (8,33 z 11,37 ms, ~73%).

> **Zastrzeżenie metodologiczne.** Kolejne pomiary nie były wykonywane na tym samym pliku skryptu — wzorzec był za każdym razem zaznaczany na nowo, więc jego wymiary różniły się nieznacznie między testami. Ponieważ ROI w wariancie `Fixed` to `rozmiar wzorca + 2 × PIN_MARGIN_PX`, **różnica `CAPTURE` 4,81 → 8,33 ms może wynikać z większego wzorca, a nie z niestabilności `Robot`**. Porównania między pomiarami traktujemy więc jako orientacyjne co do rzędu wielkości, a nie jako precyzyjne A/B. Rozstrzygnięcie, ile z tych ~8 ms to stały narzut, a ile koszt powierzchni, wymaga pomiaru na jednym zapisanym skrypcie — i jest warunkiem wstępnym dla decyzji o natywnym przechwytywaniu.

Zysk narastająco: **460 ms → 11,4 ms (÷40)**.

### Krok 5 — Mikrooptymalizacje ścieżki matchowania *(priorytet wg wyniku Kroku 0)*

Kolejność wg oczekiwanego stosunku zysk/ryzyko:

1. **Cache zdekodowanego wzorca** (`BufferedImage`/`Mat`) zamiast dekodowania Base64+PNG przy każdym wywołaniu — trywialne, zero ryzyka.
2. **Skala szarości** dla obu obrazów → 3× mniej danych w `matchTemplate`. Do zweryfikowania wpływ na skuteczność (w grach kolor bywa nośnikiem informacji → opcja per wzorzec).
3. **Eliminacja podwójnej kopii** w `bufferedImageToMat` — przechwytywanie do bufora o właściwym typie / czytanie rastra bez `drawImage`.
4. **Reuse buforów `Mat`** (osobne per wątek, żeby nie wprowadzić wyścigu).
5. **Coarse-to-fine (piramida)** dla ścieżki FullScreen: skan na obrazie zmniejszonym 4× (16× mniej pozycji), potem doprecyzowanie w pełnej rozdzielczości wokół kandydatów. To jedyna technika realnie tnąca koszt przypadku „elementu nie ma” przy pełnym ekranie. Ryzyko: strata dokładności dla wzorców 50×50 z cienkimi detalami → wymaga własnego progu na poziomie zgrubnym.
6. Rozważenie `TM_SQDIFF_NORMED` / `TM_CCORR_NORMED` — tańsze niż `TM_CCOEFF_NORMED`, ale wrażliwsze na zmiany jasności.

*Kryterium akceptacji:* każdy podpunkt wchodzi tylko wtedy, gdy pomiar potwierdza zysk; regresja skuteczności sprawdzona na zestawie realnych zrzutów.

### Krok 6 — UI: deklaracja zachowania obiektu

1. W dialogu kroku dla `Target.AtImage` / `Matcher.ImagePresent` sekcja zatytułowana **„Jak zachowuje się ten obiekt?”** z trzema opcjami wyrażonymi w języku użytkownika:
   * *„Obiekt może pojawić się w dowolnym miejscu ekranu”* → `Anywhere` **(domyślne)**
   * *„Obiekt może pojawić się tylko w wybranym obszarze”* → `WithinArea` (odsłania przycisk „Zaznacz obszar”)
   * *„Obiekt zawsze jest w tym samym miejscu”* → `Fixed`

   Zero pól technicznych, zero słowa o wydajności, zero wzmianki o skanowaniu czy ROI. Użytkownik opisuje swoją grę, nie konfiguruje algorytmu.
2. **Overlay do zaznaczania obszaru**: półprzezroczyste okno na całym virtual desktop, przeciągnięcie prostokąta, podgląd wymiarów. Musi działać na wielu monitorach. Uruchamiany tylko przy `WithinArea`.
3. Sformułowania do dopracowania przy implementacji — unikać „obiekt się nie porusza”, bo to bywa czytane jako „nie porusza się *teraz*”, zamiast „zawsze pojawia się w tym samym miejscu”. Deklaracja dotyczy całego czasu życia skryptu, nie chwili. Z tego samego powodu wariant obszarowy mówi o *pojawianiu się*, a nie o *poruszaniu się*: dla tekstu „porusza się” byłoby nieintuicyjne, a deklaracja ma być uniwersalna dla obrazu i tekstu.
4. **Domyślny wariant: `Anywhere`** (decyzja podjęta). Uzasadnienie: poprawność przed wydajnością — przy dowolnej konfiguracji ekranów domyślny krok ma szansę znaleźć element. Dla większości użytkowników różnica 100 ms vs 2 s jest nieistotna; istotne jest, że *znalazł*. Zawężenie jest świadomą deklaracją użytkownika, a nie warunkiem działania.
5. Opcjonalnie (nice-to-have, nie blokuje): przy wariancie `WithinArea` preselekcja obszaru wokół miejsca, w którym użytkownik wyciął wzorzec — mamy tę informację w momencie zrzutu.

*Kryterium akceptacji:* da się zbudować skrypt z każdym z 3 wariantów bez ręcznej edycji JSON-a; w UI nie pada ani jeden termin techniczny.

### Krok 7 — Walidacja końcowa

1. Powtórzenie benchmarku z Kroku 0, tabela przed/po dla wszystkich wariantów i obu przypadków (obecny / nieobecny).
2. Test scenariusza obserwatora: ile zapytań/s przy `Fixed` vs `Anywhere`.
3. Aktualizacja README (sekcja Project Scope + krótki opis deklaracji zachowania obiektu).

---

## 4. Oczekiwane rzędy wielkości (do potwierdzenia pomiarem)

| Wariant | Obszar (4K) | Pozycji do sprawdzenia | Względny koszt matchowania |
|---------|-------------|------------------------|----------------------------|
| `Anywhere` (dziś, 1 monitor) | 3840×2160 | ~8 000 000 | 1× |
| `Anywhere`, 2 monitory (virtual desktop) | np. 7680×2160 | ~16 000 000 | ~2× |
| `Anywhere` + piramida ÷4 | 960×540 (+refine) | ~500 000 | ~0,06–0,10× |
| `WithinArea` 800×600 | 800×600 | ~413 000 | ~0,05× |
| `WithinArea` 400×300 | 400×300 | ~88 000 | ~0,011× |
| `Fixed` (`PIN_MARGIN_PX` = 8) | 66×66 | ~289 | ~0,00004× |

Do tego dochodzi oszczędność na przechwytywaniu i konwersji, proporcjonalna do powierzchni ROI.
Uwaga: przy dwóch monitorach `Anywhere` **podwaja** dzisiejszy koszt. Mimo to pozostaje wariantem domyślnym (poprawność przed wydajnością) — dlatego tak ważne jest, żeby deklaracja zachowania obiektu była w UI łatwa i naturalna do wskazania (Krok 6).

---

## 5. UX: dlaczego pytamy o zachowanie obiektu, a nie o obszar szukania

**Problem:** „gdzie mam szukać” to pytanie o strategię techniczną. Nietechniczny użytkownik nie zna jego konsekwencji, więc wybierze najbezpieczniej brzmiącą opcję (cały ekran) i nigdy nie dowie się, że mógł mieć 60× szybciej.

**Rozwiązanie:** pytamy o coś, co użytkownik **wie na pewno** — jak zachowuje się obiekt w jego grze. Optymalizacja jest wtedy *wnioskiem aplikacji z jawnej deklaracji użytkownika*, a nie decyzją, którą użytkownik musi rozumieć.

Przepływ docelowy:

```
Kliknij w obrazek → zaznacz obrazek na ekranie → "obiekt zawsze jest w tym samym miejscu" → Zapisz
```

Użytkownik przeszedł przez optymalizację, nie wiedząc, że optymalizował. Zadeklarował fakt, a nie mechanizm.

Zasady, których trzymamy się w UI:

1. **Język faktów o świecie, nie o algorytmie.** W UI nie pada „ROI”, „skan”, „szybciej”, „region wyszukiwania”. Nazwy techniczne żyją wyłącznie w kodzie.
2. **Deklaracja niesie odpowiedzialność.** `Fixed` bez rescanu jest uczciwy właśnie dlatego, że użytkownik sam powiedział „obiekt zawsze jest w tym samym miejscu”. Zachowanie silnika jest dokładnie tym, co obiecuje zdanie w UI.
3. **Zero opcji wynikowych.** Margines, polityka chybienia, wybór monitora — nie istnieją w UI, bo nie wynikają z żadnego pytania, na które użytkownik zna odpowiedź.
4. **Precyzja sformułowań.** Deklaracja dotyczy całego czasu życia skryptu („zawsze pojawia się w tym samym miejscu”), a nie chwili („nie porusza się”).

### Backlog (świadomie poza zakresem tej iteracji)

* **Sugestia optymalizacji po wykonaniu** — aplikacja mierzy czasy i po skrypcie proponuje: *„Ten obiekt był zawsze w tym samym miejscu. Oznaczyć go jako stały?”*. Ciekawe, ale nadmiarowe teraz: deklaratywne UI z Kroku 6 rozwiązuje problem u źródła, taniej.
* **Automatyczne przypinanie po N trafieniach** — odrzucone jako zachowanie domyślne: łamie jawny kontrakt deklaracji i wprowadza niedeterminizm (ten sam skrypt zachowuje się inaczej w 1. i 10. iteracji).
* **Widoczny czas kroku w logu/drzewie** — przydatne diagnostycznie, ale wynika już częściowo z metryk Kroku 0.
* **Obszar zapisywany względem okna gry** (WinAPI `GetForegroundWindow` + `GetWindowRect`) — w tej iteracji `WithinArea.bounds` zapisujemy jako **współrzędne absolutne virtual desktop**. Względność rozwiązałaby przesunięcie okna gry i przenośność między maszynami, ale to osobny, spory temat.
* **Weryfikacja trybu pełnoekranowego (exclusive full-screen)** — `Robot.createScreenCapture` działa dla okna i dla trybu „bezramkowego pełnoekranowego”, natomiast przy exclusive full-screen (DirectX) potrafi zwrócić czarny obraz. Do sprawdzenia osobno; jeśli okaże się problemem, wymaga innego mechanizmu przechwytywania (np. Desktop Duplication API) — **to nie jest zakres tej optymalizacji**.
* **Stały narzut inicjalizacji `tess4j` (~120–170 ms na wywołanie)** — po zawężeniu obszaru to dominujący koszt ścieżki OCR i jedyny powód, dla którego `Fixed` dla tekstu daje ÷5,5 zamiast ÷40 jak przy obrazach. `Tesseract.getWords` robi `init`/`dispose` przy każdym wywołaniu; rozwiązaniem jest utrzymywanie otwartego uchwytu `TessBaseAPI` między zapytaniami.
* **Koszt `Robot.createScreenCapture` (~5–8 ms)** — po wdrożeniu ROI i cache wzorca to ~73% kosztu wariantu `Fixed`. **Wymaga najpierw czystego pomiaru** na jednym zapisanym skrypcie, żeby rozdzielić stały narzut od kosztu powierzchni (patrz zastrzeżenie metodologiczne w Kroku 5.1). Jeśli narzut okaże się stały, kandydatem jest natywne przechwytywanie przez JNA (GDI `BitBlt`, typowo 1–2 ms) lub Desktop Duplication API. To obecnie jedyna ścieżka dalszego przyspieszenia wariantu `Fixed`.

---

## 6. Ryzyka i punkty do rozstrzygnięcia

1. ~~**Skalowanie DPI**~~ — **zamknięte w Kroku 1**. Sonda potwierdziła, że `Robot` zwraca obraz w pikselach logicznych, spójnych z koordynatami myszy. Warto powtórzyć sondę, gdyby pojawił się monitor o innym współczynniku skali (mixed-DPI).
2. **Fałszywe trafienia w wąskim ROI** — mniejszy obszar = mniejsza konkurencja dla maksimum; próg `matchThreshold` trzeba traktować poważniej niż dziś.
3. **Błędna deklaracja użytkownika (`Fixed` dla obiektu, który jednak się przesuwa)** — świadomie akceptowane: brak rescanu oznacza, że taki krok po prostu nigdy nie trafi. Mitygacja wyłącznie diagnostyczna (metryka „fixed miss ratio” w logu), nie logiczna. Ryzyko jest tu mniejsze niż przy nazwach technicznych, bo zdanie w UI jest jednoznaczne.
4. **Zmiana rozdzielczości / alt-tab z gry** — unieważnia cache `Fixed` i zapisane obszary; potrzebna detekcja zmiany konfiguracji ekranów.
5. **Obszar zapisany w skrypcie a inny komputer / przesunięte okno gry** — `WithinArea.bounds` jest absolutny (decyzja na tę iterację). Wersja względem okna gry → backlog (sekcja 5).
6. **Tryb pełnoekranowy gry** — do zweryfikowania osobno, poza zakresem tej optymalizacji (backlog, sekcja 5).
7. **Kolejność 5.5 (piramida)** — to jedyna optymalizacja z realnym ryzykiem funkcjonalnym; wchodzi na końcu i za flagą.

---

### Krok 7 — Walidacja końcowa

1. Porównanie przed/po na podstawie metryk z Kroku 0 — poniżej.
2. Weryfikacja manualna UI: wszystkie trzy warianty przetestowane i działają.
3. Aktualizacja README (Project Scope: deklaracja lokalizacji elementu + wsparcie wielomonitorowe).

#### Podsumowanie: przed i po

| Faza (P50) | Przed | Po (`Fixed`) | Zmiana |
|------------|-------|--------------|--------|
| `CAPTURE` | 121,81 ms | 8,33 ms | ÷15 |
| `TEMPLATE_DECODE` | 5,32 ms | 0,01 ms | ÷500 |
| `TO_MAT` | 0,03 ms | 0,04 ms | — |
| `MATCH_TEMPLATE` | 285,79 ms | 0,58 ms | ÷490 |
| `MIN_MAX_LOC` | 34,28 ms | 0,02 ms | ÷1700 |
| **`TOTAL`** | **454,85 ms** | **11,37 ms** | **÷40** |

Co dało jaki udział w wyniku:

* **ROI (Kroki 3–4)** — źródło praktycznie całego zysku. Zawężenie obszaru zbiło `MATCH_TEMPLATE`, `MIN_MAX_LOC` i `CAPTURE` jednocześnie, bo ROI jest stosowany już przy przechwytywaniu.
* **Cache wzorca (5.1)** — usunął 5 ms, które po ROI stanowiłyby ~45% kosztu.
* **Deklaratywne UI (Krok 6)** — warunek konieczny, żeby ktokolwiek z tego skorzystał; bez niego optymalizacja istniałaby tylko w JSON-ie.

Pozostałe warianty zachowują się zgodnie z projektem: `Anywhere` kosztuje tyle co przed zmianami (~460 ms) i jest domyślny, `WithinArea` plasuje się pomiędzy — proporcjonalnie do zaznaczonej powierzchni.

### Krok 8 — Rozszerzenie deklaracji na wyszukiwanie tekstu (OCR)

Deklaracja `ElementLocation` obejmuje teraz również `Target.AtText` i `Matcher.TextPresent`, czyli oba miejsca korzystające z OCR.

1. `MatchDataDto` niesie rozmiar dopasowanego obszaru (`width`, `height`) — wypełniany przez matcher obrazkowy (rozmiar wzorca) i tekstowy (bounding box słowa z Tesseracta).
2. Dzięki temu `rememberIfFixed` w `VisionFinder` jest wspólne dla obu typów zapytań; zniknęło przekazywanie wzorca do tej metody.
3. `TesseractTextMatcher` dobiera tryb segmentacji do wielkości ROI: `PSM_AUTO (3)` dla dużych zrzutów, `PSM_SINGLE_LINE (7)` dla wycinków niższych niż 120 px. Bez tego automatyczna segmentacja na wąskim ROI zwykle nie zwraca nic — czyli warianty `Fixed` i `WithinArea` dla tekstu po prostu by nie działały.
4. `match` jest `@Synchronized`, bo instancja `Tesseract` jest współdzielona między wątkiem wykonania a wątkiem obserwatora, a tryb segmentacji ustawiany jest per wywołanie.
5. UI: sekcje tekstowe w `MouseTargetEditor` i `MatcherEditor` dostały własny `ElementLocationEditor` (niezależny od tego przy wzorcu obrazkowym).

Oczekiwany zysk jest tu jeszcze większy niż przy obrazach: OCR na pełnym ekranie jest znacznie droższy od template matchingu, a `Fixed` ogranicza go do kilkudziesięciu pikseli. Do zmierzenia fazą `OCR` w metrykach.

#### Wynik: baseline OCR (`Anywhere`, 15 zapytań)

```
  PHASE              COUNT       AVG       P50       P95       MAX
  CAPTURE               15    135.36    133.84    151.84    154.34
  OCR                   15    881.44    854.37   1040.20   1067.32
  TOTAL                 15   1017.56   1014.58   1166.95   1188.66
```

**Ponad 1 sekunda na zapytanie** — OCR na pełnym ekranie jest ~2,2× droższy niż template matching (460 ms) i to on, a nie `CAPTURE`, odpowiada za 84% kosztu. Potencjał zawężenia obszaru jest tu więc największy w całym projekcie.

#### Problem: `Fixed` dla tekstu przestawał znajdować po pierwszym trafieniu

Objaw: pierwsze wyszukanie działa, kolejne kończą się szybko i bez wyniku. Diagnoza — **dwie niezależne przyczyny, obie związane z tym, że ROI dla tekstu jest po prostu za mały dla OCR**:

1. **Brak marginesu wokół słowa.** `PIN_MARGIN_PX = 8` jest w porządku dla template matchingu, ale Tesseract wymaga „światła” wokół tekstu — na ciasnym wycinku segmentacja nie znajduje żadnej linii. Rozwiązanie: margines zależny od typu zapytania, dla tekstu `max(24 px, wysokość słowa)`, co realnie potraja wysokość wycinka.
2. **Za mało pikseli na znak.** Silnik LSTM potrzebuje odpowiedniej wysokości glifu; wycinek o wysokości ~20–60 px jest dla niego zbyt mały, nawet z marginesem. Rozwiązanie: wycinki niższe niż 90 px są przed OCR skalowane w górę (interpolacja dwusześcienna, maks. ×4), a współrzędne i wymiary trafienia są przeliczane z powrotem przez ten sam współczynnik.

Obie poprawki dotyczą wyłącznie ścieżki tekstowej i nie zmieniają zachowania wyszukiwania po obrazie.

**Pierwsze podejście nie wystarczyło.** Log pokazał, że geometria jest poprawna — słowo „Test" znalezione na pełnym ekranie w `50x22@(57,85)`, ROI po dopięciu marginesu to `98x70@(33,61)`, czyli obszar w całości zawierający słowo z zapasem ≥24 px z każdej strony. Mimo to OCR nie rozpoznawał niczego. Kluczowa wskazówka była w linii `Estimating resolution as 136` — Tesseract sam szacował DPI obrazu i na małym wycinku robił to błędnie. Trzy dalsze poprawki:

3. **Jawne DPI** (`user_defined_dpi = 300`) — usuwa heurystykę szacowania rozdzielczości, która na wycinkach dawała bezsensowne wyniki i psuła rozpoznawanie.
4. **Kaskada trybów segmentacji.** Żaden pojedynczy tryb nie obsługuje dobrze zarówno pełnego ekranu, jak i wycinka. Dla małych wycinków próbujemy kolejno `PSM 7` (linia) → `PSM 6` (blok) → `PSM 11` (rozproszony tekst) → `PSM 8` (pojedyncze słowo) i bierzemy pierwszy wynik. Na wycinku ~100×70 px każda próba kosztuje kilka ms, więc kaskada jest tania.
5. **Mocniejsze skalowanie** — próg podniesiony do 180 px docelowej wysokości (maks. ×6), bo ×2 dawało wciąż zbyt małe glify dla silnika LSTM.

#### Wynik pośredni: `Fixed` dla tekstu (przed poprawkami 3–5)

```
  PHASE              COUNT       AVG       P50       P95       MAX
  CAPTURE               71     16.00     13.49     23.35    144.29
  OCR                   70    129.25    116.06    145.39    854.08
  TOTAL                 70    145.61    131.44    159.87   1002.38
```

Nawet przy nieudanym rozpoznawaniu widać skalę zysku z ROI: `OCR` spadło z 854 ms (P50) do 116 ms, a `TOTAL` z 1015 ms do 131 ms (**÷7,7**) — mimo że w tym przebiegu OCR wykonywał pełną pracę i kończył ją bez trafienia.

#### Rozwiązanie: `PSM_AUTO` przy skali natywnej

Kaskada trybów okazała się ślepą uliczką. Rozstrzygający był fakt z logu: pełny ekran był rozpoznawany trybem `PSM 3` **przy skali 1×** i wysokości słowa 21 px — czyli ta sama treść, w tej samej rozdzielczości, jest dla silnika czytelna. Problemem nie był więc ani rozmiar glifów, ani tryb segmentacji, tylko to, że dla małych wycinków **wykluczaliśmy `PSM 3`** i wymuszaliśmy skalowanie, które rozmywa krawędzie tekstu.

Po ustawieniu `PSM_AUTO` przy skali natywnej jako pierwszej (i jedynej) próby wszystko działa. Ostateczna konfiguracja ścieżki tekstowej:

* `user_defined_dpi = 300` — bez tego Tesseract szacuje DPI (`Estimating resolution as 136`) i na wycinkach robi to błędnie,
* `PSM_AUTO`, skala natywna — jedna próba dla pełnego ekranu i dla wycinka,
* jedna dodatkowa próba z powiększeniem **tylko** dla wycinków niższych niż 60 px,
* margines przypięcia dla tekstu `max(40 px, 2 × wysokość słowa)`.

Ograniczenie liczby prób jest tu decyzją wydajnościową, nie kosmetyczną: `tess4j` inicjalizuje silnik przy **każdym** wywołaniu (stały narzut ~120–170 ms niezależnie od rozmiaru obrazu), a przy profilu „elementu zwykle nie ma" każda nieudana próba jest płacona za każdym razem. Kaskada 4 trybów podnosiła koszt chybienia do ~480 ms.

#### Wynik końcowy: `Fixed` dla tekstu

```
  PHASE              COUNT       AVG       P50       P95       MAX
  CAPTURE                6     40.11     15.79     22.18    163.66
  OCR                    6    272.59    169.86    193.21    786.53
  TOTAL                  6    313.07    185.88    213.02    950.56
```

| Faza (P50) | `Anywhere` | `Fixed` | Zmiana |
|------------|-----------|---------|--------|
| `CAPTURE` | 133,84 ms | 15,79 ms | ÷8,5 |
| `OCR` | 854,37 ms | 169,86 ms | ÷5 |
| **`TOTAL`** | **1014,58 ms** | **185,88 ms** | **÷5,5** |

Zysk jest wyraźnie mniejszy niż przy obrazach (÷40), bo dominuje w nim stały narzut inicjalizacji silnika `tess4j`, którego nie da się zbić zawężaniem obszaru. To główny kandydat do dalszej optymalizacji ścieżki OCR (utrzymywanie otwartego uchwytu `TessBaseAPI` zamiast init/dispose per wywołanie) — do backlogu.

Uwaga uboczna: bounding box tego samego słowa różni się między pełnym ekranem (`50x21`) a wycinkiem (`43x11`), co przesuwa wyliczony środek o ~4 px. Nie powoduje to dryfu, ponieważ lokalizacja jest zapamiętywana wyłącznie przy pierwszym trafieniu (`putIfAbsent`), a nie aktualizowana przy kolejnych.

---

## 7. Proponowana kolejność prac

`Krok 0` ✅ → `Krok 1` ✅ → `Krok 2` ✅ → `Krok 3` ✅ → `Krok 4` ✅ → `Krok 5.1 (cache wzorca)` ✅ → `Krok 6` ✅ → `Krok 7` ✅ → `Krok 8 (OCR)` ✅ → `Krok 5.2–5.6` (odłożone)

Po pomiarach z Kroków 4 i 5.1 punkty 5.2–5.6 (skala szarości, eliminacja kopii w `toMat`, piramida) **straciły sens dla wariantów `Fixed` i `WithinArea`** — dotyczą faz, które zeszły poniżej 1 ms. Zostają istotne wyłącznie dla `Anywhere`, gdzie `MATCH_TEMPLATE` nadal kosztuje ~290 ms.

Kroki 0–4 dają większość zysku i są niezależne od UI; Krok 5.1 odblokowuje pełny potencjał wariantu `Fixed`; Krok 6 udostępnia to użytkownikowi.


