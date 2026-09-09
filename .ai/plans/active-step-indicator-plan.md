# Plan: Wskaźnik aktywnego kroku + status bar (wydajnościowo bezpieczny)

## 1. Problem i cel

Podczas wykonywania skryptu użytkownik nie widzi, **który krok jest właśnie wykonywany**. Właściwość `ScriptViewModel.activeStepIdProperty` (ScriptViewModel.kt:26) istnieje, ale **nigdy nie jest ustawiana na wartość niepustą** — silnik publikuje tylko `logStepSuccess/Failure` (`ExecutionEventPublisher.kt:11-13`), a `onStepExecuted` (ScriptViewModel.kt:210) wyłącznie **zeruje** ją do `null`.

**Cel (MVP, minimalistyczny — zakres zamknięty):**
1. Dyskretne podświetlenie aktywnego kroku w drzewie (subtelny lewy pasek akcentu, bez animacji i migania).
2. Status bar na dole okna: ikona + nazwa bieżącego kroku, stan wykonania, licznik wykonanych kroków, przeniesiony tam pasek `👁 Watching / ▶ Handling`.
3. Wszystko musi działać przy krokach trwających **< 10 ms** (100–1000 kroków/s) bez zamrożenia UI.

### Ustalenia (wymagania przyjęte z użytkownikiem)

| # | Ustalenie |
|---|-----------|
| 1 | Zakres MVP zamknięty do 3 punktów wyżej — bez timeline'a, bez breakpointów, bez podglądu zmiennych |
| 2 | Brak animacji i migania — podświetlenie statyczne |
| 3 | Zdarzenie `stepStarted` musi nieść `StepId`, nie samą nazwę |
| 4 | **Zero `Platform.runLater` per zdarzenie** — obowiązkowa koalescencja w pulsie |
| 5 | `StepTreeView` nie wywołuje `refresh()` przy zmianie aktywnego kroku |
| 6 | Log panel objęty tym samym mechanizmem batchowania (to samo źródło lawiny zdarzeń) |
| 7 | Brak auto-scrolla drzewa w MVP (uzasadnienie w §9) |

---

## 2. Stan obecny (analiza kodu)

### 2.1 Silnik wykonania
- `ScriptInterpreter.executeStep` (ScriptInterpreter.kt:61-71) woła `scriptExecutionState.recordActiveStep(step)` — **to jest naturalny punkt zaczepienia** dla `stepStarted`, ale nic nie trafia do UI.
- `ActionStepHandler.execute` (ActionStepHandler.kt:14-31) publikuje `logStepSuccess(stepName, durationMs)` **po każdym kroku akcji**.
- Wykonanie idzie na wątku wirtualnym (`ScriptRunner.kt:40`), a `ObserverRegistry` na osobnym wątku.

### 2.2 Most UI
- `UiExecutionEventPublisher` (UiExecutionEventPublisher.kt) to cienki proxy → `ScriptViewModel`.
- `ScriptViewModel.addLog` (ScriptViewModel.kt:198-208): **`Platform.runLater` per wpis** + `logMessages.removeAt(0)` przy 1000 wpisach → O(n) kopiowanie tablicy + osobne zdarzenie `ListChangeListener` na każdy wpis.
- `onExecutionStop`, `onObserverStatusChanged` — również `Platform.runLater` per zdarzenie.
- Wątek `script-state-monitor` (ScriptViewModel.kt:100-116) pollinguje stan co 200 ms — można wykorzystać jako wzorzec, ale nie do sygnalizacji kroków.

### 2.3 Drzewo kroków
- `StepTreeView.init` (StepTreeView.kt:26-28): `activeStepIdProperty.addListener { refresh() }` → **pełny `refresh()` całego `TreeView`** przy każdej zmianie. Przy 500 zmianach/s to setki przebudów wszystkich widocznych komórek.
- `ScriptTreeCell.updateItem` (StepTreeView.kt:168-186) **buduje od zera** cały `StepCellGraphic.build(...)` — dwa `Label`, `VBox`, `HBox`, paski wstawiania z `Timeline`/`PauseTransition`. To najdroższa część.
- `isActive` jest przekazywane jako **parametr konstrukcyjny** grafiki → zmiana stanu wymaga przebudowy węzła.
- W `updateItem` jest `styleClass.removeAll("step-cell-active")` (StepTreeView.kt:157) — martwy kod, klasa siedzi na `contentRow`, nie na komórce.

### 2.4 Log panel
- `LogPanel.init` (LogPanel.kt:27-31): listener na **każdej** zmianie listy woła `listView.scrollTo(...)` → wymuszony layout viewportu na każdy wpis; brak sticky-bottom (przewijanie użytkownika jest natychmiast nadpisywane).
- `LogCell.updateItem` (LogPanel.kt:49-77) **tworzy dwa `Label` i `HBox` przy każdym wywołaniu** — łamie sens recyklingu komórek.

### 2.5 Status obserwatorów
- Aktualnie w nagłówku `ScriptPanel.buildHeader` (ScriptPanel.kt:54-59) — do przeniesienia do status bara.
- `MainController.initialize` (MainController.kt:33-84) buduje `rootPane.top = toolbar`, `rootPane.center = splitPane`; **`rootPane.bottom` jest wolne** → miejsce na `StatusBarView`.

### 2.6 Diagnoza wydajnościowa
Przy 1000 krokach/s obecny kod generuje na sekundę: ~1000 `Platform.runLater` (log) + ~1000 `runLater` (stan) + 1000 pełnych `TreeView.refresh()` + 1000 `scrollTo` + 1000 × O(1000) `removeAt(0)`. Kolejka FX rośnie szybciej niż jest konsumowana → **UI zamarza, opóźnienie rośnie liniowo, ryzyko OOM na kolejce runnable'i**.

Powiązane wymagania PRD: RF-068 – RF-072 (sterowanie i podgląd wykonania).

---

## 3. Projekt rozwiązania — architektura

```
com/adaptibot/ui/
├── runtime/
│   ├── ExecutionUiState.kt      # lock-free stan bieżący (producent: wątki silnika)
│   ├── UiPulseDispatcher.kt     # AnimationTimer + throttling, jedyny konsument
│   └── LogRingBuffer.kt         # bufor wpisów logu z drop policy
├── view/
│   ├── StatusBarView.kt         # NOWY — dolny pasek statusu
│   └── ActiveStepDecorator.kt   # NOWY — punktowe przełączanie pseudo-klasy :active
```

**Zasada naczelna:** wątki silnika **tylko zapisują** do struktur lock-free (bez alokacji, bez `runLater`). Wątek FX **raz na puls** czyta zagregowany stan i publikuje go do właściwości JavaFX. Producent–konsument z koalescencją: 1000 zdarzeń/s → maks. 10–20 aktualizacji UI/s.

### 3.1 `ExecutionUiState` (nowa klasa, thread-safe, bezalokacyjna po stronie producenta)

```kotlin
class ExecutionUiState {
    private val activeStep = AtomicReference<StepRef?>(null)   // ostatni "wygrywa"
    private val executedCount = LongAdder()                    // bez CAS-contention
    private val failedCount   = LongAdder()
    @Volatile private var runState: RunState = RunState.IDLE
    private val observerStatus = AtomicReference<ObserverStatusDto?>(null)
    @Volatile private var dirty = false   // hint: pomiń puls, gdy nic się nie zmieniło

    // ── producent (dowolny wątek) ───────────────────────────────
    fun stepStarted(id: StepId, name: String, icon: String)
    fun stepFinished(success: Boolean)
    fun runStateChanged(state: RunState)
    fun observerStatusChanged(status: ObserverStatusDto)

    // ── konsument (tylko wątek FX) ──────────────────────────────
    fun snapshot(): UiSnapshot?   // null = brak zmian od ostatniego pulsu
}

data class StepRef(val id: StepId, val name: String, val icon: String)
data class UiSnapshot(
    val activeStep: StepRef?, val executed: Long, val failed: Long,
    val runState: RunState, val observerStatus: ObserverStatusDto?
)
enum class RunState { IDLE, RUNNING, STOPPING, FINISHED, FAILED }
```

**Klucz:** `StepRef` jest tworzony raz per krok (jedna mała alokacja, escape-analysis-friendly); `AtomicReference.lazySet` wystarcza, `LongAdder` eliminuje kontencję przy inkrementach.

### 3.2 `UiPulseDispatcher` (nowa klasa, jedyne miejsce dotykające właściwości FX)

```kotlin
class UiPulseDispatcher(
    private val state: ExecutionUiState,
    private val logBuffer: LogRingBuffer,
    private val minIntervalMs: Long = 60L        // ~16 aktualizacji/s
) {
    fun start()   // uruchamia AnimationTimer (wywołanie z wątku FX)
    fun stop()
    fun addSink(sink: (UiSnapshot) -> Unit)
    fun addLogSink(sink: (List<LogMessage>, dropped: Int) -> Unit)
    fun flushNow()  // wymuszony flush przy STOP/FINISHED — ostatni stan musi dojść
}
```

Implementacja: `object : AnimationTimer() { override fun handle(now: Long) { ... } }`.
- `AnimationTimer.handle` jest wołany na wątku FX w pulsie renderowania (≤60 Hz) — **nie zapycha kolejki `runLater`**, bo nie dodaje do niej niczego.
- Wewnątrz: `if (now - lastNs < minIntervalMs * 1_000_000) return` → throttling do ~60–100 ms.
- Kolejność w jednym pulsie: (1) `state.snapshot()` → sinki stanu, (2) `logBuffer.drain()` → batch do `logMessages`, (3) jeden `scrollTo`.
- Timer działa **tylko podczas wykonania** (start w `startExecution`, stop po `FINISHED` + jednym flushu) — zero kosztu w spoczynku.

**Dlaczego `AnimationTimer`, a nie `Timeline` / `ScheduledExecutor` + `runLater`:**
`Timeline` też generuje zdarzenia i alokuje `KeyFrame`; `ScheduledExecutor` musiałby i tak wołać `runLater`. `AnimationTimer` jest z definicji zsynchronizowany z pulsem renderera — publikujemy dokładnie tak często, jak UI potrafi to narysować, ani razu więcej.

---

## 4. Zmiany w warstwie wykonania (zdarzenie `stepStarted`)

1. **`ExecutionEventPublisher`** — dodać:
   ```kotlin
   fun stepStarted(stepId: StepId, stepName: String, stepKind: StepKind)
   fun stepFinished(stepId: StepId, success: Boolean, durationMs: Long)
   ```
   `StepKind` (nowy enum w `execution.dto`) mapuje typ kroku na ikonę bez ciągnięcia modelu domenowego do UI. Domyślne implementacje `= Unit` w interfejsie → brak zmian w istniejących testowych implementacjach.

2. **`ScriptInterpreter.executeStep`** (ScriptInterpreter.kt:61) — zaraz po `recordActiveStep(step)` wywołać `eventPublisher.stepStarted(step.id, step.displayName(), step.kind())`. Wymaga wstrzyknięcia `ExecutionEventPublisher` do `ScriptInterpreter` (obecnie ma go tylko `ActionStepHandler`) — dodać parametr konstruktora w `ExecutionConfiguration`.
   > **Decyzja:** publikować w `ScriptInterpreter`, nie w `ActionStepHandler` — dzięki temu podświetlają się także kroki kontenerowe (`Group`, `While`, `For`, `If`), a nie tylko akcje.

3. **`ActionStepHandler`** — `logStepSuccess/Failure` zostają (kompatybilność), ale dodatkowo woła `stepFinished(step.id, success, duration)` do inkrementacji licznika.

4. **Brak zmian w kontrakcie blokującym** — wszystkie nowe wywołania są `O(1)`, bezalokacyjne poza jednym `StepRef`, i nie synchronizują się z wątkiem FX. Budżet: **< 200 ns per krok**.

---

## 5. Zmiany w `ScriptViewModel`

- Nowe pola: `val executionUiState = ExecutionUiState()`, `private val logBuffer = LogRingBuffer(capacity = 4096)`, `private val pulse = UiPulseDispatcher(...)`.
- Nowe właściwości FX (aktualizowane **wyłącznie** przez sink dyspozytora):
  - `activeStepIdProperty` (istniejąca, ale teraz zasilana z pulsu),
  - `activeStepLabelProperty: StringProperty`,
  - `activeStepIconProperty: StringProperty`,
  - `executedStepCountProperty: IntegerProperty`,
  - `failedStepCountProperty: IntegerProperty`,
  - `runStateProperty: ObjectProperty<RunState>`.
- **Nowe metody wejściowe (wołane z wątków silnika, bez `runLater`):**
  ```kotlin
  fun onStepStarted(stepId: StepId, name: String, kind: StepKind)   // → executionUiState
  fun onStepFinished(stepId: StepId, success: Boolean, ms: Long)    // → executionUiState + logBuffer
  ```
- `addLog(message)` — przepisać: jeśli wątek FX i timer nie działa → dotychczasowa ścieżka bezpośrednia (logi UI, np. „Script saved”); w przeciwnym razie `logBuffer.offer(message)`.
- `startExecution()` — `pulse.start()`; monitor stanu ustawia `RunState` w `ExecutionUiState` zamiast wołać `runLater`; po `FINISHED` → `pulse.flushNow(); pulse.stop()`.
- `onObserverStatusChanged` — zamiast `Platform.runLater` zapis do `ExecutionUiState` (obserwator zmienia się rzadko, ale kod ma być jednorodny).
- `isRunningProperty` — pozostaje, bindowane do `runStateProperty` przez `Bindings.createBooleanBinding` (`ToolbarView` bez zmian).

---

## 6. `LogRingBuffer` + batch flush + drop policy

```kotlin
class LogRingBuffer(private val capacity: Int = 4096, private val maxPerFlush: Int = 2000) {
    fun offer(msg: LogMessage)                        // producent, non-blocking
    fun drain(): DrainResult                          // konsument (FX), jednorazowo per puls
}
data class DrainResult(val entries: List<LogMessage>, val dropped: Int)
```

**Implementacja:** `ArrayDeque<LogMessage>` pod krótkim `synchronized` + `AtomicInteger droppedCount`.
> Wybrano `ArrayDeque` + lock zamiast `ConcurrentLinkedQueue`, bo: (a) potrzebujemy taniego `size()` do drop policy — w `ConcurrentLinkedQueue` `size()` jest O(n); (b) `drain()` przenosi całą zawartość jedną operacją; (c) lock jest nieskontencjonowany (jeden producent-wykonawca, jeden konsument-FX), więc realnie thin lock ≈ koszt CAS.

**Drop policy (dokładnie):**
1. `offer` przy `size >= capacity` (4096) → **nie blokuje**, odrzuca wpis i `droppedCount.incrementAndGet()`.
2. `drain` zdejmuje maks. `maxPerFlush` (2000) wpisów; nadmiar zostaje w buforze na następny puls.
3. Jeśli `dropped > 0`, konsument **przed** batchem wstawia jeden syntetyczny wpis:
   `LogMessage.warning("… pominięto $dropped wpisów (limit bufora logu)")`, a licznik jest zerowany (`getAndSet(0)`).
4. Kolejne serie odrzuceń **agregują się** w kolejnym takim wpisie — nigdy nie powstaje więcej niż jeden wpis „pominięto” na puls.

**Flush do `ObservableList` (sink w `LogPanel`/`ScriptViewModel`):**
```kotlin
logMessages.addAll(batch)                         // JEDNO zdarzenie ListChange
val overflow = logMessages.size - MAX_LOG /*1000*/
if (overflow > 0) logMessages.remove(0, overflow) // JEDNO usunięcie zakresu, nie N × removeAt(0)
```
> `ObservableList.remove(from, to)` generuje jedno zdarzenie i jedno `System.arraycopy` zamiast N przesunięć — zamienia O(N×n) na O(n).

**Verbose / poziom logowania (rekomendacja):**
Dodać `ScriptSettings.verboseStepLogging: Boolean = false`. Przy `false` **nie generujemy wpisu `✔ krok (Xms)`** — sukcesy widać w statusie i liczniku, a log zawiera tylko start/stop, ostrzeżenia i błędy. Przy `true` (checkbox w oknie ustawień + toggle „Verbose” w nagłówku log panelu) wpisy sukcesu wracają.
> **To jest najskuteczniejsza optymalizacja w całym planie** — usuwa źródło problemu zamiast go amortyzować. Bufor i batching zostają jako zabezpieczenie dla trybu verbose i lawiny błędów.

---

## 7. Zmiany w `LogPanel`

1. **Sticky-bottom auto-scroll.** Usunąć listener z LogPanel.kt:27-31. Zamiast tego sink dyspozytora woła `maybeScroll()` **raz na flush**:
   - `atBottom` wyliczane przed dodaniem batcha z `listView.lookup(".scroll-bar:vertical") as? ScrollBar` → `value >= max - epsilon` (albo flaga ustawiana w `addEventFilter(ScrollEvent.ANY)`).
   - `scrollTo(size - 1)` tylko gdy `atBottom == true`.
   - Gdy użytkownik przewinął w górę: dyskretny przycisk „↓ Najnowsze” (pojedynczy `Button`, bez animacji), który przywraca sticky.
2. **`LogCell` — recykling węzłów.** Przenieść `timeLabel`, `msgLabel`, `HBox` do pól klasy tworzonych **raz w `init`**; w `updateItem` ustawiać tylko `text` i podmieniać jedną klasę stylu:
   ```kotlin
   msgLabel.styleClass.setAll("log-entry", textClass)
   if (graphic !== row) graphic = row
   ```
   Zysk: zero alokacji na scroll i na flush.
3. Przycisk „Verbose” (toggle) w nagłówku obok „Clear”.

---

## 8. Punktowa aktualizacja drzewa (`PseudoClass`)

### 8.1 Usunięcie pełnego refresh
Skasować `viewModel.activeStepIdProperty.addListener { refresh() }` (StepTreeView.kt:27).
> Listenery `armedObserverIdsProperty` / `handlingObserverIdProperty` **zostają** — obserwatory zmieniają stan rzadko (rząd 1/s), więc `refresh()` jest tam akceptowalny. Zmiana ich mechaniki jest poza zakresem MVP.

### 8.2 Pseudo-klasa na komórce
W `ScriptTreeCell`:
```kotlin
companion object { private val ACTIVE = PseudoClass.getPseudoClass("active") }
private val activeProperty = object : BooleanPropertyBase(false) {
    override fun invalidated() = pseudoClassStateChanged(ACTIVE, get())
    override fun getBean() = this@ScriptTreeCell
    override fun getName() = "active"
}
```
Pseudo-klasa idzie na `TreeCell` (nie na `contentRow`), więc **przełączenie nie dotyka grafiki komórki** — JavaFX tylko przelicza CSS dla jednego węzła i jego dzieci przez selektor potomka.

### 8.3 Zgodność z recyklingiem komórek
Dwa kierunki synchronizacji, oba obowiązkowe:

**a) Komórka → stan (przy recyklingu):** w `updateItem` (StepTreeView.kt:168) na końcu gałęzi `StepNode`:
`activeProperty.set(node.step.id == viewModel.activeStepIdProperty.get())`.
Bez tego komórka odzyskana ze scrolla odziedziczyłaby pseudo-klasę po poprzednim itemie.
W gałęzi `empty`/`ContainerNode` → `activeProperty.set(false)` (zastępuje martwy `styleClass.removeAll("step-cell-active")`).

**b) Stan → komórka (przy zmianie aktywnego kroku):** `ActiveStepDecorator` rejestrowany w `StepTreeView`:
```kotlin
class ActiveStepDecorator {
    fun bind(stepId: String, cell: ScriptTreeCell)   // wołane z updateItem
    fun unbind(cell: ScriptTreeCell)                 // przy przejściu w stan empty
    fun setActive(newId: StepId?)                    // dokładnie 2 zmiany pseudo-klasy
}
```
Trzyma `Map<String, ScriptTreeCell>` po `stepId.value`, aktualizowaną w `updateItem`. `setActive` gasi komórkę o starym id i zapala o nowym — **dokładnie dwie** operacje. Komórki trzymane przez `WeakReference` (ochrona przed wyciekiem po `rebuildTree()`).

`ActiveStepDecorator.setActive` jest wołany z sinka dyspozytora → maks. ~16×/s. Koszt pomijalny.

### 8.4 `StepCellGraphic`
Usunąć parametr `isActive` i linię `if (isActive) styleClass.add("step-cell-active")`. Wygląd aktywnego kroku przechodzi **w całości do CSS**, sterowany pseudo-klasą rodzica. Sygnatura `build(...)` traci jeden parametr — jedyne wywołanie jest w StepTreeView.kt:174.

### 8.5 CSS (`adaptibot.css`)
Zastąpić `.step-cell-active` (adaptibot.css:179-182):
```css
.tree-cell:active .step-cell-box {
    -fx-border-color: #a6e3a1 #313244 #313244 #a6e3a1;
    -fx-border-width: 1 1 1 3;              /* subtelny lewy pasek akcentu */
    -fx-background-color: #a6e3a10f;
}
```
Bez `dropshadow` (efekt wymusza osobny pass renderowania), bez animacji, bez `-fx-effect`. Analogiczny, wyszarzony wariant dla stanu `STOPPING`.

---

## 9. `StatusBarView` (nowy plik)

`class StatusBarView(viewModel: ScriptViewModel) : HBox()` — montowany jako `rootPane.bottom` w `MainController.initialize` (obok MainController.kt:80).

Układ (od lewej): `[● stan] │ [ikona] Nazwa aktywnego kroku │ ——spacer—— │ ✔ 1 234  ✖ 2 │ 👁 Watching: … · ▶ Handling: …`

- Wszystkie etykiety **bindowane** do właściwości VM (`textProperty().bind(...)`) — brak imperatywnych aktualizacji.
- Nazwa kroku: `Label` z `maxWidth` + `textOverrun = ELLIPSIS`, `minWidth = 0` — długie nazwy nie rozpychają okna i nie wywołują reflow.
- **Stała szerokość licznika** (`prefWidth` + font monospaced) — inaczej każda zmiana liczby wywołuje relayout całego paska.
- Pasek obserwatorów: **przeniesiony** ze `ScriptPanel.buildHeader` (ScriptPanel.kt:54-64) 1:1 wraz ze stylem `.observer-status-strip`; nagłówek `ScriptPanel` zostaje z samym tytułem.
- Pasek zawsze widoczny; w stanie `IDLE` pokazuje „Gotowy”.

### Auto-scroll drzewa do aktywnego kroku — **NIE w MVP**

**Uzasadnienie:** `TreeView.scrollTo(index)` wymaga (a) wyliczenia płaskiego indeksu przez `getRow(treeItem)` — O(n) po widocznych wierszach, (b) rozwinięcia rodziców — mutacja modelu drzewa → **pełny relayout**, (c) walki z ręcznym przewijaniem użytkownika. Przy krokach < 10 ms drzewo skakałoby chaotycznie i byłoby nieczytelne, a użytkownik nie mógłby niczego przeczytać ani kliknąć. Status bar dostarcza tę samą informację **bez żadnego kosztu layoutu**. Jeśli funkcja okaże się potrzebna — dodać ją później jako **opt-in toggle** („Podążaj za wykonaniem”), throttlowany do 1× / 500 ms i wyłączany automatycznie na 3 s po ręcznym przewinięciu.

---

## 10. Budżet wydajności i kryteria akceptacji

| # | Kryterium | Próg |
|---|-----------|------|
| 1 | Skrypt 10 000 kroków po ~2 ms — mediana FPS wątku FX | **≥ 55**, minimum ≥ 45 |
| 2 | Liczba `Platform.runLater` na sekundę podczas wykonania | **≤ 5** (tylko zdarzenia cyklu życia) |
| 3 | Aktualizacje właściwości FX aktywnego kroku | **≤ 20/s** niezależnie od tempa kroków |
| 4 | Zmiany pseudo-klasy na aktualizację | **dokładnie ≤ 2** |
| 5 | Narzut `stepStarted` na wątku wykonania | **< 500 ns** p99 |
| 6 | `logMessages` — liczba zdarzeń `ListChangeListener` / s | **≤ 20** (1 add + ≤1 remove na puls) |
| 7 | `listView.scrollTo` / s | **≤ 20** |
| 8 | Sterta po 10 min ciągłego wykonania | brak trendu wzrostowego; **brak OOM** |
| 9 | Alokacje na krok (poza `StepRef`) | **0** przy `verboseStepLogging = false` |
| 10 | Opóźnienie wskaźnika względem realnego kroku | ≤ 100 ms (akceptowalne percepcyjnie) |
| 11 | Responsywność: kliknięcie Stop w trakcie 10 000 kroków | reakcja **< 200 ms** |

Pomiar: JFR (`jdk.ObjectAllocationSample`, `jdk.ThreadCPULoad`) + licznik FPS z `AnimationTimer` w trybie diagnostycznym.

---

## 11. Kolejność wdrożenia

1. **Krok 1 — fundament (bez zmian UI).** `ExecutionUiState`, `LogRingBuffer`, `UiPulseDispatcher` + testy jednostkowe i obciążeniowe. Nic jeszcze nie jest podłączone.
2. **Krok 2 — zdarzenia silnika.** `StepKind`, `stepStarted`/`stepFinished` w `ExecutionEventPublisher` (z domyślnymi implementacjami), wstrzyknięcie publishera do `ScriptInterpreter`, wywołania w `executeStep` i `ActionStepHandler`. Weryfikacja testami silnika.
3. **Krok 3 — most VM.** `UiExecutionEventPublisher` → nowe metody `ScriptViewModel`; `ScriptViewModel` zasila `ExecutionUiState`; dyspozytor start/stop w `startExecution`/monitorze; nowe właściwości FX. `activeStepIdProperty` zaczyna działać.
4. **Krok 4 — log batching.** `addLog` → bufor, sink flushujący `addAll` + `remove(0, overflow)`, drop policy, `verboseStepLogging` w `ScriptSettings` + UI ustawień.
5. **Krok 5 — LogPanel.** Recykling węzłów w `LogCell`, sticky-bottom, jeden `scrollTo` na flush, przycisk „↓ Najnowsze”, toggle „Verbose”.
6. **Krok 6 — drzewo.** Usunięcie `refresh()`, `PseudoClass :active`, `ActiveStepDecorator`, usunięcie `isActive` z `StepCellGraphic`, nowe CSS.
7. **Krok 7 — status bar.** `StatusBarView`, montaż w `MainController.bottom`, przeniesienie paska obserwatorów ze `ScriptPanel`.
8. **Krok 8 — walidacja.** Testy TestFX + scenariusz obciążeniowy 10 000 kroków, weryfikacja tabeli z §10, ewentualny tuning `minIntervalMs`.

---

## 12. Pliki zmieniane / nowe

### Nowe
| Plik | Rola |
|---|---|
| `ui/runtime/ExecutionUiState.kt` | lock-free stan wykonania |
| `ui/runtime/UiPulseDispatcher.kt` | `AnimationTimer` + throttling + sinki |
| `ui/runtime/LogRingBuffer.kt` | bufor logu z drop policy |
| `ui/view/StatusBarView.kt` | dolny pasek statusu |
| `ui/view/ActiveStepDecorator.kt` | punktowe przełączanie `:active` |
| `execution/dto/StepKind.kt` | typ kroku → ikona |

### Zmieniane
| Plik | Zmiana |
|---|---|
| `execution/domain/ExecutionEventPublisher.kt` | `stepStarted`, `stepFinished` |
| `execution/domain/ScriptInterpreter.kt` | wstrzyknięcie publishera; `stepStarted` w `executeStep` |
| `execution/domain/ActionStepHandler.kt` | `stepFinished` |
| `execution/ExecutionConfiguration.kt` | przekazanie publishera do interpretera |
| `script/ScriptSettings.kt` + serializacja | `verboseStepLogging` |
| `ui/adapter/UiExecutionEventPublisher.kt` | nowe metody proxy |
| `ui/viewmodel/ScriptViewModel.kt` | stan + dyspozytor + bufor, przepisany `addLog`, nowe właściwości |
| `ui/view/StepTreeView.kt` | usunięty `refresh()`, pseudo-klasa, rejestracja w dekoratorze |
| `ui/view/StepCellGraphic.kt` | usunięty parametr `isActive` |
| `ui/view/LogPanel.kt` | recykling `LogCell`, sticky-bottom, toggle verbose |
| `ui/view/ScriptPanel.kt` | usunięty pasek obserwatorów z nagłówka |
| `ui/controller/MainController.kt` | montaż `StatusBarView` w `rootPane.bottom` |
| `ui/dialog/ScriptSettingsDialog.kt` | checkbox „Verbose step logging” |
| `resources/css/adaptibot.css` | `:active`, style status bara |

---

## 13. Plan testów

### 13.1 Jednostkowe (bez JavaFX)
| Test | Oczekiwanie |
|---|---|
| `ExecutionUiStateTest` — 1000 × `stepStarted`, jeden `snapshot()` | zwraca **ostatni** krok; `executed` = 1000 |
| `ExecutionUiStateTest` — `snapshot()` bez zmian | zwraca `null` (puls pomijany) |
| `LogRingBufferTest` — `offer` ponad `capacity` | nadmiar odrzucony, `dropped` = liczba odrzuceń |
| `LogRingBufferTest` — `drain` przy > `maxPerFlush` | zwraca dokładnie `maxPerFlush`, reszta zostaje |
| `LogRingBufferTest` — `dropped` po `drain` | zerowane, nie liczone dwukrotnie |

### 13.2 Obciążeniowe (concurrency)
| Test | Oczekiwanie |
|---|---|
| `UiPulseDispatcherLoadTest` — 4 wątki × 250 000 zdarzeń | brak wyjątków, brak zgubionego **ostatniego** stanu, `executed` = suma dokładna |
| `UiPulseDispatcherLoadTest` — throttling | przy 100 000 zdarzeń w 1 s liczba wywołań sinka ≤ `1000/minIntervalMs + 2` |
| `flushNow()` na końcu | ostatni stan i ostatnie wpisy logu **zawsze** dostarczone |
| Test alokacji (JFR/JMH) | `stepStarted` p99 < 500 ns, brak alokacji poza `StepRef` |

### 13.3 TestFX
| Test | Oczekiwanie |
|---|---|
| Podświetlenie | po `stepStarted(id)` komórka o tym `id` ma pseudo-klasę `active`, poprzednia jej nie ma |
| Recykling | po przewinięciu drzewa **żadna** inna komórka nie ma `active` |
| Status bar | nazwa i licznik zgodne z `ExecutionUiState` po flushu |
| Log batch | 5000 wpisów → `logMessages.size == 1000`, ostatni wpis = ostatni wygenerowany |
| Sticky-bottom | po ręcznym przewinięciu w górę `scrollTo` nie jest wołany; przycisk „↓ Najnowsze” przywraca |
| Drop policy | przy przepełnieniu w logu **dokładnie jeden** wpis „… pominięto N wpisów” na puls |
| Zakończenie | po `FINISHED` żadna komórka nie ma `active`, status = „Zakończono” |
| Smoke wydajnościowy | 10 000 kroków × 2 ms — UI reaguje na kliknięcie Stop < 200 ms |

---

## 14. Ryzyka

| # | Ryzyko | Mitygacja |
|---|---|---|
| 1 | `AnimationTimer` nie tyka, gdy okno zminimalizowane / brak sceny | Timer tylko dekoruje; stan żyje w `ExecutionUiState` i dogoni po przywróceniu. `flushNow()` przy `FINISHED` gwarantuje spójność końcową |
| 2 | Selektor `.tree-cell:active .step-cell-box` koliduje z wbudowanymi stanami | `active` to **własna** pseudo-klasa, nie koliduje z `:pressed`/`:selected`. Fallback: nazwa `step-active` |
| 3 | Mapa komórek w `ActiveStepDecorator` wycieka po `rebuildTree()` | Wpis usuwany w `updateItem` przy `empty`; dodatkowo `WeakReference` na komórki |
| 4 | `remove(0, overflow)` psuje pozycję scrolla / selekcję w `ListView` | Test TestFX na sticky-bottom + selekcję |
| 5 | `verboseStepLogging` w `ScriptSettings` łamie deserializację starych plików | Pole z wartością domyślną `false` + test round-trip na istniejącym pliku `.json` |
| 6 | Zmiana sygnatury `ExecutionEventPublisher` psuje istniejące testy silnika | Domyślne implementacje `= Unit` w interfejsie |
| 7 | Zmiana konstruktora `ScriptInterpreter` (nowa zależność) | `ExecutionConfiguration` to jedyne miejsce montażu — punktowa zmiana |
| 8 | Opóźnienie ≤ 100 ms odbierane jako „laguje” | Parametr `minIntervalMs` konfigurowalny; start od 60 ms, tuning w kroku 8 |
| 9 | Kroki < 1 ms → wskaźnik pokazuje „losowy” krok z serii | Świadomy kompromis — użytkownik i tak nie odczyta więcej niż ~10 nazw/s. Licznik wykonanych kroków pozostaje **dokładny** |
| 10 | Przeniesienie paska obserwatorów zmienia oczekiwania istniejących testów UI | Aktualizacja testów w kroku 7 |

---

## 15. Otwarte decyzje do potwierdzenia

1. Czy `verboseStepLogging` ma być ustawieniem **skryptu** (zapisywanym w JSON, zgodnie z US-044) czy globalnym ustawieniem aplikacji? *Rekomendacja: ustawienie skryptu — spójne z resztą `ScriptSettings`.*
2. Wartość startowa `minIntervalMs` — proponowane 60 ms (~16 Hz); do weryfikacji w kroku 8.
3. Czy licznik w status barze ma pokazywać także iterację pętli głównej (`Loop #N`)? *Poza MVP.*

