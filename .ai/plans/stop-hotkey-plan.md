# Plan: Zatrzymywanie skryptu globalnym skrótem klawiszowym (Windows)

## 1. Problem i cel

Podczas wykonywania skryptu aplikacja przejmuje mysz (`SetCursorPos`, `mouse_event` przez WinAPI),
więc użytkownik nie jest w stanie kliknąć przycisku **Stop** w UI.

**Cel:** globalny (systemowy) skrót klawiszowy, który zatrzymuje działający skrypt niezależnie od tego,
które okno ma fokus i niezależnie od tego, co robi mysz.

### Ustalenia (wymagania przyjęte z użytkownikiem)

| # | Ustalenie |
|---|-----------|
| 1 | Windows-first (JNA + `user32`), inne platformy poza zakresem |
| 2 | Skrót **stały** w MVP, ale kod przygotowany na przyszłą konfigurowalność |
| 3 | Tylko akcja **STOP** — bez pauzy/wznowienia |
| 4 | Aplikacja musi działać **bez uprawnień administratora** |
| 5 | Architektura gotowa na przyszłe **nagrywanie makr** (globalny nasłuch wejścia) |
| 6 | Stop natychmiast przerywa oczekiwania (`Thread.sleep`) i zwalnia wciśnięte klawisze/przyciski |
| 7 | UI reaguje na stop wywołany skrótem (status + stan przycisków) przez `Platform.runLater` |

Powiązane wymagania PRD: RF-068 – RF-072 (sterowanie wykonaniem), US „Zatrzymanie działającego skryptu”.

---

## 2. Stan obecny (analiza kodu)

- `ExecutionFacade.stopScript()` → `ScriptRunner.stop()`.
- `ScriptRunner` trzyma `@Volatile executionThread` (virtual thread) i wywołuje `interrupt()`.
  `stop()` **nie jest w pełni idempotentny** — `ScriptExecutionState.stop()` loguje zawsze.
- Blokujący `Thread.sleep` występuje w:
  - `action/domain/SystemActionHandler.kt` (`System.Wait`) — najdłuższe oczekiwania,
  - `action/adapter/MouseController.kt` (hold / drag / double-click),
  - `action/adapter/KeyboardController.kt` (`pressKeys`, `typeText`),
  - `execution/domain/observer/ObserverRegistry.kt` (`checkDelayMs`),
  - `ScriptInterpreter.waitForDelay` (delayBefore/delayAfter).
- Pętle `While`/`For` w `ScriptInterpreter` nie sprawdzają stanu wykonania w każdej iteracji.
- WinAPI: `action/adapter/winapi/User32.kt` — własny, minimalny, `internal` interfejs JNA
  (`SetCursorPos`, `mouse_event`, `keybd_event`).
- UI: `MainController` → `ScriptViewModel` (`isRunningProperty`, `stopExecution()`, wątek `script-state-monitor`)
  → `ToolbarView` (bindowanie przycisków Run/Stop).

---

## 3. Wybór rozwiązania

### Rekomendacja: `RegisterHotKey` + pętla komunikatów Windows na dedykowanym wątku

Dedykowany wątek platformowy (demon) `global-hotkey-loop`:

1. `RegisterHotKey(null, id, MOD_CONTROL|MOD_SHIFT|MOD_NOREPEAT, VK_F12)`
2. pętla `GetMessage` → reakcja na `WM_HOTKEY`
3. `UnregisterHotKey` przy zamykaniu, wybudzenie pętli przez `PostThreadMessage(WM_QUIT)`

> **Krytyczne:** przy `hWnd = null` skrót jest przypisany do **wątku**, który go zarejestrował —
> rejestracja i pętla komunikatów muszą być na tym samym wątku.

**Dlaczego to rozwiązanie:**

- działa **bez uprawnień administratora**,
- brak globalnego hooka → brak ryzyka spowolnienia całego systemu i brak „podejrzanego” zachowania dla AV,
- brak nowych zależności (JNA już w `build.gradle.kts`),
- filtrowanie zdarzeń robi system — nasz kod dostaje wyłącznie `WM_HOTKEY`.

**Znane ograniczenia:**

- nie zadziała, gdy aktywne okno należy do procesu o **wyższym poziomie integralności** (uruchomionym jako admin),
- część gier pełnoekranowych / anti-cheat może przechwytywać wejście wcześniej,
- skrót może być **już zajęty** przez inną aplikację → `RegisterHotKey` zwraca `false`.

**Plan fallbacku (osobny etap, ta sama abstrakcja):**
`WH_KEYBOARD_LL` przez `SetWindowsHookEx` + `LowLevelKeyboardProc` + własne śledzenie stanu modyfikatorów.
Podmiana wyłącznie w `HotkeyConfiguration` — warstwy wyższe bez zmian.
Ta sama warstwa jest naturalną bazą pod **nagrywanie makr** w przyszłości.

---

## 4. Domyślny skrót: `Ctrl + Shift + F12`

- maska: `MOD_CONTROL | MOD_SHIFT | MOD_NOREPEAT`, `VK_F12 = 0x7B`
- F12 rzadko jest globalnym skrótem systemowym; typowe kolizje (DevTools, Steam overlay) dotyczą **czystego** F12,
- łatwy do trafienia jedną ręką, na krańcu klawiatury → minimalne ryzyko przypadkowego wywołania.

Alternatywy awaryjne (do udokumentowania, gdy skrót zajęty): `Ctrl+Alt+Shift+S`, `Ctrl+Break`.

---

## 5. Projekt modułu `com.adaptibot.hotkey`

```
com/adaptibot/hotkey/
├── HotkeyFacade.kt              # publiczne API: start(onStopRequested), close()
├── HotkeyConfiguration.kt       # składanie zależności (styl ExecutionConfiguration)
├── model/
│   ├── HotkeyCombination.kt     # Set<HotkeyModifier> + virtualKeyCode
│   ├── HotkeyModifier.kt        # CTRL, SHIFT, ALT, WIN -> maska MOD_*
│   └── HotkeyDefaults.kt        # STOP = Ctrl+Shift+F12
├── domain/
│   └── GlobalHotkeyService.kt   # interfejs: register/unregister/close
└── adapter/winapi/
    ├── WindowsHotkeyService.kt  # wątek + pętla komunikatów
    └── HotkeyUser32.kt          # interfejs JNA
```

Interfejs domenowy:

```
interface GlobalHotkeyService : AutoCloseable {
    fun register(combination: HotkeyCombination, onTriggered: () -> Unit): Boolean
    fun unregister(combination: HotkeyCombination)
    override fun close()
}
```

**Decyzja:** tworzymy **nowy** interfejs JNA `HotkeyUser32` zamiast rozszerzać `internal User32`
z pakietu `action.adapter.winapi` — izolacja modułów, brak zależności `hotkey → action`.

---

## 6. Rozszerzenia JNA

Do `HotkeyUser32` (biblioteka `user32`):

| Element | Uwagi |
|---|---|
| `RegisterHotKey(HWND, int id, int fsModifiers, int vk): Boolean` | `hWnd = null` → hotkey wątkowy |
| `UnregisterHotKey(HWND, int id): Boolean` | wywoływane z **tego samego** wątku |
| `GetMessage(MSG, HWND, int, int): Int` | blokujące; `-1` = błąd, `0` = `WM_QUIT` |
| `PeekMessage(...)` | opcjonalnie, wariant nieblokujący |
| `PostThreadMessage(int threadId, int msg, ...)` | wybudzenie pętli przy `close()` |
| struktura `MSG` | `Structure` z `getFieldOrder()`; pola `hWnd, message, wParam, lParam, time, pt` |

Z `kernel32`: `GetCurrentThreadId()` — zapamiętanie ID wątku pętli, potrzebne do `PostThreadMessage`.

Stałe: `MOD_ALT=0x1`, `MOD_CONTROL=0x2`, `MOD_SHIFT=0x4`, `MOD_WIN=0x8`, `MOD_NOREPEAT=0x4000`,
`WM_HOTKEY=0x0312`, `WM_QUIT=0x0012`, `VK_F12=0x7B`.

---

## 7. Zmiany w warstwie wykonania (natychmiastowe przerwanie)

1. **Idempotentny `stop()`** — `ScriptRunner.stop()` chroniony `AtomicBoolean` / sprawdzeniem stanu;
   `ScriptExecutionState.stop()` loguje tylko przy realnej zmianie stanu z `RUNNING`.
2. **`InterruptibleSleep`** (`common/InterruptibleSleep.kt`) — oczekiwanie w odcinkach ≤ 50 ms
   z kontrolą flagi stanu, albo `CountDownLatch.await(timeout)` na „stop latch” zwalnianym w `stop()`.
   Zastąpić nim `Thread.sleep` w: `SystemActionHandler`, `ObserverRegistry`, `MouseController`,
   `KeyboardController`, `ScriptInterpreter.waitForDelay`.
3. **Kontrola stanu w pętlach** — `ScriptInterpreter` sprawdza `isRunning() && !Thread.currentThread().isInterrupted`
   również w każdej iteracji `WhileStep` / `ForStep`.
4. **Propagacja `InterruptedException`** — nie tłumić; przywrócić flagę `interrupt()` i zakończyć wykonanie.
5. **Cleanup wejścia** — `InputStateTracker` (rejestr wciśniętych VK i przycisków myszy prowadzony przez
   `KeyboardController`/`MouseController`), a `releaseAll()` wywoływane w bloku `finally`
   w `ScriptInterpreter.interpret` (przez `ActionFacade.releaseAllInputs()`).
   Bez tego przerwanie `pressKeys` / `drag` / `hold` zostawia wciśnięty Shift lub LPM.

---

## 8. Zmiany w UI

- `MainController`: po zbudowaniu `ScriptViewModel` uruchomić `HotkeyConfiguration.getFacade()`
  z callbackiem `Platform.runLater { viewModel.stopExecution(StopSource.HOTKEY) }`.
- `ScriptViewModel.stopExecution(source)`: log w panelu logów — `⏹ Zatrzymano skryptem (Ctrl+Shift+F12)`.
- `ToolbarView`: `Tooltip` na przycisku Stop — „Stop (Ctrl+Shift+F12)”; opcjonalnie etykieta skrótu na pasku statusu.
- Guard: ignorować zdarzenie hotkey, gdy stan wykonania ≠ `RUNNING` lub gdy JavaFX jest w trakcie zamykania.

---

## 9. Cykl życia

**Rekomendacja: rejestracja RAZ przy starcie aplikacji** (nie na czas trwania skryptu).

- prostsze, brak wyścigu przy starcie skryptu i „martwej strefy” w pierwszych milisekundach,
- błąd rejestracji wykrywany od razu, a nie w krytycznym momencie,
- callback i tak filtruje stan wykonania.

Zwolnienie: `AdaptiBotApp.stop()` → `HotkeyFacade.close()` (+ `Runtime.addShutdownHook` jako zabezpieczenie).
`close()` = `PostThreadMessage(WM_QUIT)` → wyjście z pętli → `UnregisterHotKey` → join wątku (z timeoutem).

**Błąd rejestracji** (`RegisterHotKey` = `false`): `logger.warn`, jednorazowy wpis w panelu logów
(„Skrót Ctrl+Shift+F12 jest zajęty przez inną aplikację — Stop dostępny tylko z poziomu UI”), aplikacja działa dalej.

---

## 10. Testy

**Zasada nadrzędna:** testujemy **wyłącznie przez fasady modułów** (`HotkeyFacade`, `ExecutionFacade`,
`ActionFacade`). Klasy wewnętrzne (`WindowsHotkeyService`, `ScriptRunner`, `InputStateTracker`,
`InterruptibleSleep`, kontrolery) pozostają `internal` i **nie są testowane bezpośrednio** —
są weryfikowane pośrednio, poprzez zachowanie fasady. Mockowany jest wyłącznie brzeg systemowy
(interfejs JNA `HotkeyUser32` / `User32`), wstrzykiwany przez konfigurację modułu.

**Konsekwencja projektowa:** `HotkeyConfiguration` musi udostępniać wariant fabryki przyjmujący
podstawiony `HotkeyUser32` (widoczność `internal`, przeznaczony dla testów), analogicznie
konfiguracja modułu `action` dla `User32` — inaczej test przez fasadę wymagałby realnego WinAPI.

**Przez `HotkeyFacade`** (JUnit 5 + MockK, `HotkeyUser32` zamockowany):

- `start(onStopRequested)` → wywołanie `RegisterHotKey` z maską `MOD_CONTROL|MOD_SHIFT|MOD_NOREPEAT`
  i `VK_F12` (mapowanie kombinacji weryfikowane tą asercją, bez osobnego testu modelu),
- symulowany komunikat `WM_HOTKEY` zwrócony przez mock `GetMessage` → callback wywołany dokładnie raz,
- `RegisterHotKey` zwraca `false` → `start()` nie rzuca wyjątku i sygnalizuje nieudaną rejestrację,
- `close()` → `PostThreadMessage(WM_QUIT)` + `UnregisterHotKey`, wątek pętli kończy się w zadanym czasie,
- `close()` dwukrotnie → brak błędu, `UnregisterHotKey` dokładnie raz (idempotencja).

**Przez `ExecutionFacade`:**

- `startScript` skryptu z długim `System.Wait` (np. 30 s) + `stopScript` → `getExecutionState()`
  wraca do `IDLE` w < 500 ms (weryfikacja przerywalnych oczekiwań),
- `stopScript()` dwukrotnie oraz przy braku działającego skryptu → brak wyjątku (idempotencja),
- `stopScript()` w trakcie skryptu trzymającego klawisz/przycisk → na zamockowanym `User32`
  pojawiają się zdarzenia KEYUP/MOUSEUP dla wszystkiego, co było wciśnięte (weryfikacja cleanupu),
- skrypt z pętlą `While`/`For` → `stopScript()` kończy wykonanie bez czekania na koniec iteracji.

**Integracyjny (styk modułów, nadal wyłącznie przez fasady):**

- callback przekazany do `HotkeyFacade.start` wywołuje `ExecutionFacade.stopScript()` →
  stan wykonania zmienia się na `IDLE`.

**Manualne (Windows):**

1. Stop podczas długiego `System.Wait` (np. 30 s) — reakcja natychmiastowa.
2. Stop w trakcie `drag` / `pressKeys` — brak „zawieszonego” LPM / Shift.
3. Stop, gdy fokus ma Notatnik, przeglądarka, gra w oknie.
4. Skrót zajęty przez inną aplikację — komunikat, aplikacja działa.
5. Naciśnięcie skrótu, gdy skrypt nie działa — brak efektu, brak błędu.
6. Zamknięcie aplikacji → skrót zwolniony (ponowne uruchomienie rejestruje się poprawnie).
7. Okno uruchomione jako administrator — udokumentowane ograniczenie.

---

## 11. Kolejność wdrożenia

1. `HotkeyUser32` + struktura `MSG` + stałe (kompilacja, smoke test rejestracji).
2. Model `HotkeyCombination` / `HotkeyModifier` / `HotkeyDefaults` (bez osobnych testów — pokryte przez fasadę).
3. `GlobalHotkeyService` + `WindowsHotkeyService` (wątek, pętla, `close()`) — `internal`.
4. `HotkeyFacade` + `HotkeyConfiguration` (z fabryką testową przyjmującą `HotkeyUser32`) + **testy przez fasadę**.
5. Idempotentny `stop()` + kontrola stanu w `While` / `For`.
6. `InterruptibleSleep` w `SystemActionHandler`, `ObserverRegistry`, kontrolerach, `ScriptInterpreter`.
7. `InputStateTracker` + `releaseAll()` w `finally`.
8. Podpięcie w `MainController` / `ScriptViewModel`, tooltip w `ToolbarView`.
9. Wyrejestrowanie w `AdaptiBotApp.stop()` + obsługa błędu rejestracji.
10. Testy manualne wg listy + notatka w `README.md`.

### Pliki nowe

`hotkey/HotkeyFacade.kt`, `hotkey/HotkeyConfiguration.kt`, `hotkey/model/HotkeyCombination.kt`,
`hotkey/model/HotkeyModifier.kt`, `hotkey/model/HotkeyDefaults.kt`, `hotkey/domain/GlobalHotkeyService.kt`,
`hotkey/adapter/winapi/WindowsHotkeyService.kt`, `hotkey/adapter/winapi/HotkeyUser32.kt`,
`common/InterruptibleSleep.kt`, `action/adapter/InputStateTracker.kt`
oraz testy **wyłącznie na poziomie fasad**: `hotkey/HotkeyFacadeTest.kt`, `execution/ExecutionFacadeStopTest.kt`.

### Pliki zmieniane

`ScriptRunner.kt`, `ScriptExecutionState.kt`, `ScriptInterpreter.kt`, `SystemActionHandler.kt`,
`ObserverRegistry.kt`, `MouseController.kt`, `KeyboardController.kt`, `ActionFacade.kt`,
`MainController.kt`, `ScriptViewModel.kt`, `ToolbarView.kt`, `AdaptiBotApp.kt`.

---

## 12. Ryzyka i mitigacje

| Ryzyko | Mitigacja |
|---|---|
| Skrót zajęty przez inną aplikację | Log + komunikat w UI, przygotowana lista alternatyw |
| Okno admina / gra z anti-cheat | Udokumentowane ograniczenie; fallback `WH_KEYBOARD_LL` |
| Wyciek rejestracji przy twardym zamknięciu | Shutdown hook |
| Zablokowana pętla `GetMessage` przy zamykaniu | `PostThreadMessage(WM_QUIT)` + join z timeoutem |
| „Zawieszone” klawisze / przyciski po stopie | `InputStateTracker.releaseAll()` w `finally` |
| Wyścig callback ↔ `Platform.runLater` po zamknięciu JavaFX | Guard na flagę zamykania aplikacji |
| Fałszywy alarm antywirusa (przy fallbacku na hook) | Preferowanie `RegisterHotKey` w MVP |

---

## 13. Przyszłe rozszerzenia (POZA zakresem)

- **Nagrywanie makr** — `GlobalInputListener` na `WH_KEYBOARD_LL` / `WH_MOUSE_LL` obok `GlobalHotkeyService`.
- **Konfigurowalny skrót** — zapis w ustawieniach + „capture” skrótu w UI.
- **Pauza / wznowienie** — drugi hotkey + stan `PAUSED` w `ExecutionState`.

