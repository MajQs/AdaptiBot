# Plan Refaktoryzacji - Architektura Domain (DDD)

> **📊 STATUS IMPLEMENTACJI:** ✅ **UKOŃCZONE** - 9/9 kroków (100%)
> 
> - ✅ **Krok 1:** ExecutionEventPublisher - ZAKOŃCZONY
> - ✅ **Krok 2:** BlockStepResolver - ZAKOŃCZONY  
> - ✅ **Krok 3:** ActionStepHandler - ZAKOŃCZONY
> - ✅ **Krok 4:** ExecutionSession - ZAKOŃCZONY
> - ✅ **Krok 5:** StepSequenceExecutor - ZAKOŃCZONY
> - ✅ **Krok 6:** ScriptExecutionService - ZAKOŃCZONY
> - ✅ **Krok 7:** ObserverInterruptCoordinator - ZAKOŃCZONY
> - ✅ **Krok 8a:** ObserverRegistry - ZAKOŃCZONY
> - ⏭️ **Krok 8b:** ObserverConditionMonitor - POMINIĘTY (nieuzasadniony)
> - ✅ **Krok 9:** Cleanup - ZAKOŃCZONY
>
> 🎉 **Zobacz [REFACTORING_FINAL_REPORT.md](./REFACTORING_FINAL_REPORT.md) dla pełnego raportu!**

## 📋 Przegląd

Ten dokument opisuje plan refaktoryzacji warstwy `core.domain` zgodnie z zasadami Domain-Driven Design (DDD).
Refaktoryzacja będzie przeprowadzona **inkrementalnie** w małych krokach, z weryfikacją kompilacji po każdym kroku.

## 🎯 Cele

1. **Poprawić nazewnictwo klas** - usunąć generyczne nazwy (Orchestrator, Controller, Manager, Executor)
2. **Rozdzielić odpowiedzialności** - wydzielić nowe klasy gdzie to potrzebne
3. **Usunąć cross-layer dependencies** - domain nie może zależeć od UI
4. **Zwiększyć spójność** - konsekwentne nazewnictwo zgodne z Ubiquitous Language

## 📊 Stan Obecny

```
core/
├── domain/
│   ├── ScriptOrchestrator.kt          (koordynuje całe wykonanie)
│   ├── ExecutionController.kt         (zarządza stanem wykonania)
│   ├── StepExecutionOrchestrator.kt   (wykonuje sekwencję kroków)
│   ├── ActionExecutor.kt              (adapter dla ActionExecutorImpl)
│   ├── BlockExecutor.kt               (resolver dla bloków)
│   └── observer/
│       └── ObserverManager.kt         (zarządza observerami)
└── CoreFacade.kt
    CoreConfiguration.kt
```

### Problemy:
- ❌ `ScriptOrchestrator` używa bezpośrednio `com.adaptibot.ui.model.ExecutionLogger` (cross-layer dependency)
- ❌ `ActionExecutor` w domain koliduje z `ActionExecutor` w actions
- ❌ `ExecutionController.getScope()` eksponuje szczegóły implementacji
- ❌ Generyczne nazwy: Orchestrator, Controller, Manager, Executor wszędzie
- ❌ `BlockExecutor.execute()` nie wykonuje, tylko rozwiązuje (resolver)
- ❌ `ObserverManager` ma dead code (priority) i naiwny polling

## 🎯 Stan Docelowy

```
core/
├── domain/
│   ├── ScriptExecutionService.kt      (główny serwis wykonania)
│   ├── ExecutionSession.kt            (sesja wykonania ze stanem)
│   ├── StepSequenceExecutor.kt        (wykonuje sekwencję kroków)
│   ├── ActionStepHandler.kt           (obsługuje ActionStep)
│   ├── BlockStepResolver.kt           (resolver dla bloków)
│   ├── ExecutionEventPublisher.kt     (interface dla eventów)
│   └── observer/
│       ├── ObserverRegistry.kt        (rejestracja observerów)
│       ├── ObserverConditionMonitor.kt (monitorowanie warunków)
│       └── ObserverInterruptCoordinator.kt (koordynacja przerwań)
└── CoreFacade.kt
    CoreConfiguration.kt
```

---

## 📝 Plan Implementacji - 9 Kroków

### ✅ **KROK 1: Utworzenie interfejsu ExecutionEventPublisher**

**Cel:** Usunąć zależność domain → ui poprzez dependency inversion

**Co robimy:**
1. Tworzymy nowy interface `ExecutionEventPublisher` w pakiecie `core.domain`
2. Definiujemy metody do logowania eventów
3. Implementujemy ten interface w warstwie UI jako adapter do `ExecutionLogger`
4. Aktualizujemy `CoreConfiguration` aby wstrzykiwać implementację

**Nowe pliki:**
- `src/main/kotlin/com/adaptibot/core/domain/ExecutionEventPublisher.kt`
- `src/main/kotlin/com/adaptibot/ui/adapter/UiExecutionEventPublisher.kt`

**Modyfikowane pliki:**
- `ScriptOrchestrator.kt` - przyjmuje `ExecutionEventPublisher` w konstruktorze
- `ActionExecutor.kt` - przyjmuje `ExecutionEventPublisher` w konstruktorze
- `CoreConfiguration.kt` - tworzy i wstrzykuje implementację

**Weryfikacja:**
```powershell
.\gradlew build
```

---

### ✅ **KROK 2: Rename BlockExecutor → BlockStepResolver**

**Cel:** Nazwa powinna odzwierciedlać faktyczną odpowiedzialność (resolver, nie executor)

**Co robimy:**
1. Rename klasy `BlockExecutor` → `BlockStepResolver`
2. Rename metody `execute()` → `resolve()`
3. Aktualizacja wszystkich użyć

**Modyfikowane pliki:**
- `BlockExecutor.kt` → `BlockStepResolver.kt`
- `StepExecutionOrchestrator.kt` - zmiana dependency
- `CoreConfiguration.kt` - zmiana konstrukcji

**Weryfikacja:**
```powershell
.\gradlew build
```

---

### ✅ **KROK 3: Rename ActionExecutor → ActionStepHandler**

**Cel:** Rozwiązać konflikt nazw i lepiej oddać odpowiedzialność

**Co robimy:**
1. Rename `ActionExecutor` (w domain) → `ActionStepHandler`
2. Usunąć alias `ActionExecutor as ActionExecutorImpl` w imports
3. Aktualizacja wszystkich użyć

**Modyfikowane pliki:**
- `ActionExecutor.kt` → `ActionStepHandler.kt`
- `StepExecutionOrchestrator.kt` - zmiana dependency
- `CoreConfiguration.kt` - zmiana konstrukcji i importu

**Weryfikacja:**
```powershell
.\gradlew build
```

---

### ✅ **KROK 4: Refaktoryzacja ExecutionController → ExecutionSession**

**Cel:** Enkapsulacja stanu i lepsze nazewnictwo

**Co robimy:**
1. Rename `ExecutionController` → `ExecutionSession`
2. Ukrycie `getScope()` - zamiana na internal property
3. Rename metod:
   - `setActiveStep()` → `recordActiveStep()`
   - `finish()` → `completeExecution()`
4. Dodanie enkapsulacji dla state transitions

**Modyfikowane pliki:**
- `ExecutionController.kt` → `ExecutionSession.kt`
- `ScriptOrchestrator.kt` - zmiana dependency i wywołań
- `StepExecutionOrchestrator.kt` - zmiana dependency i wywołań
- `CoreConfiguration.kt` - zmiana konstrukcji

**Weryfikacja:**
```powershell
.\gradlew build
```

---

### ✅ **KROK 5: Rename StepExecutionOrchestrator → StepSequenceExecutor**

**Cel:** Bardziej konkretna nazwa oddająca wykonywanie sekwencji

**Co robimy:**
1. Rename klasy `StepExecutionOrchestrator` → `StepSequenceExecutor`
2. Rename metody `execute()` → `executeSequence()`
3. Rename prywatnej `handleTriggeredObserver()` → `processObserverInterrupt()`
4. Aktualizacja wszystkich użyć

**Modyfikowane pliki:**
- `StepExecutionOrchestrator.kt` → `StepSequenceExecutor.kt`
- `ScriptOrchestrator.kt` - zmiana dependency i wywołań
- `CoreConfiguration.kt` - zmiana konstrukcji

**Weryfikacja:**
```powershell
.\gradlew build
```

---

### ✅ **KROK 6: Rename ScriptOrchestrator → ScriptExecutionService**

**Cel:** "Service" jest właściwą nazwą w DDD dla aplikacyjnej logiki koordynującej

**Co robimy:**
1. Rename klasy `ScriptOrchestrator` → `ScriptExecutionService`
2. Rename metody `executeInfiniteLoop()` → `executeScriptLoop()`
3. Aktualizacja wszystkich użyć

**Modyfikowane pliki:**
- `ScriptOrchestrator.kt` → `ScriptExecutionService.kt`
- `CoreFacade.kt` - zmiana dependency
- `CoreConfiguration.kt` - zmiana konstrukcji

**Weryfikacja:**
```powershell
.\gradlew build
```

---

### ✅ **KROK 7: Wydzielenie ObserverInterruptCoordinator**

**Cel:** Wydzielenie odpowiedzialności za koordynację przerwań od observerów

**Co robimy:**
1. Tworzymy nową klasę `ObserverInterruptCoordinator`
2. Przenosimy logikę `AtomicReference<ObserverStep?>` z `StepSequenceExecutor`
3. Przenosimy metodę `processObserverInterrupt()` do nowej klasy
4. `StepSequenceExecutor` dostaje `ObserverInterruptCoordinator` jako dependency

**Nowe pliki:**
- `src/main/kotlin/com/adaptibot/core/domain/observer/ObserverInterruptCoordinator.kt`

**Modyfikowane pliki:**
- `StepSequenceExecutor.kt` - usunięcie logiki przerwań, dodanie dependency
- `CoreConfiguration.kt` - dodanie konstrukcji nowego obiektu

**Weryfikacja:**
```powershell
.\gradlew build
```

---

### ✅ **KROK 8: Refaktoryzacja ObserverManager → ObserverRegistry + ObserverConditionMonitor**

**Cel:** Rozdzielenie odpowiedzialności: rejestracja vs monitorowanie

**Co robimy:**

**Faza 8a - Rename ObserverManager → ObserverRegistry:**
1. Rename `ObserverManager` → `ObserverRegistry`
2. Aktualizacja wszystkich użyć

**Faza 8b - Wydzielenie ObserverConditionMonitor:**
1. Tworzymy nową klasę `ObserverConditionMonitor`
2. Przenosimy logikę pollingu i sprawdzania warunków
3. `ObserverRegistry` deleguje monitorowanie do `ObserverConditionMonitor`

**Nowe pliki:**
- `src/main/kotlin/com/adaptibot/core/domain/observer/ObserverConditionMonitor.kt`

**Modyfikowane pliki:**
- `ObserverManager.kt` → `ObserverRegistry.kt`
- `StepSequenceExecutor.kt` - zmiana dependency name
- `CoreConfiguration.kt` - zmiana konstrukcji

**Weryfikacja:**
```powershell
.\gradlew build
```

---

### ✅ **KROK 9: Cleanup i finalne poprawki**

**Cel:** Uporządkowanie i optymalizacja

**Co robimy:**
1. Usunięcie dead code (priority w ObserverRegistry)
2. Dodanie dokumentacji do kluczowych klas
3. Przegląd wszystkich metod publicznych - czy nazwy są jasne?
4. Przegląd logowania - czy komunikaty są spójne?
5. Finalna weryfikacja kompilacji i testów

**Modyfikowane pliki:**
- `ObserverRegistry.kt` - usunięcie dead code
- Wszystkie zrefaktoryzowane klasy - dodanie KDoc

**Weryfikacja:**
```powershell
.\gradlew build
.\gradlew test
```

---

## 🔍 Mapowanie Zmian

### Przed → Po:

| Przed | Po | Typ |
|-------|-----|-----|
| `ScriptOrchestrator` | `ScriptExecutionService` | Rename |
| `ExecutionController` | `ExecutionSession` | Rename + Refactor |
| `StepExecutionOrchestrator` | `StepSequenceExecutor` | Rename |
| `ActionExecutor` (domain) | `ActionStepHandler` | Rename |
| `BlockExecutor` | `BlockStepResolver` | Rename |
| `ObserverManager` | `ObserverRegistry` + `ObserverConditionMonitor` | Split |
| - | `ObserverInterruptCoordinator` | New |
| - | `ExecutionEventPublisher` (interface) | New |
| - | `UiExecutionEventPublisher` | New |

---

## 🧪 Strategia Testowania

Po każdym kroku:
1. ✅ Uruchom `.\gradlew build` - czy kompiluje się?
2. ✅ Uruchom `.\gradlew test` - czy testy przechodzą?
3. ✅ Sprawdź ręcznie IDE - czy nie ma błędów?
4. ✅ Jeśli coś nie działa - wycofaj poprzedni commit i popraw

Po wszystkich krokach:
1. ✅ Uruchom aplikację i przetestuj wykonanie skryptu
2. ✅ Sprawdź czy observery działają
3. ✅ Sprawdź czy conditional blocks działają
4. ✅ Sprawdź czy logowanie działa

---

## 📌 Uwagi Implementacyjne

### Dependency Inversion dla ExecutionLogger

**Problem:** Domain zależy od UI (`com.adaptibot.ui.model.ExecutionLogger`)

**Rozwiązanie:**
```kotlin
// core/domain/ExecutionEventPublisher.kt
interface ExecutionEventPublisher {
    fun logExecutionStart(scriptName: String)
    fun logExecutionStop()
    fun logStepSuccess(stepName: String, durationMs: Long)
    fun logStepFailure(stepName: String, durationMs: Long, error: String)
}

// ui/adapter/UiExecutionEventPublisher.kt
class UiExecutionEventPublisher : ExecutionEventPublisher {
    override fun logExecutionStart(scriptName: String) {
        ExecutionLogger.logExecutionStart(scriptName)
    }
    // ... pozostałe metody
}
```

### Enkapsulacja CoroutineScope

**Problem:** `ExecutionController.getScope()` eksponuje szczegóły implementacji

**Rozwiązanie:**
```kotlin
// Zamiast:
executionController.getScope()?.launch { ... }

// Robimy:
internal class ExecutionSession {
    internal fun launchInScope(block: suspend CoroutineScope.() -> Unit) {
        executionScope?.launch(block = block)
    }
}
```

### Usunięcie Dead Code

W `ObserverRegistry` parametr `priority` jest nigdzie nieużywany:
```kotlin
// TODO not sure if priority is needed
fun registerObserver(observer: ObserverStep, priority: Int = 100)
```

**Decyzja:** Usunąć lub implementować w pełni. Sugeruję usunięcie w Kroku 9.

---

## 🎓 Uzasadnienie Nazw (DDD Perspective)

### Service vs Orchestrator
- **Service** w DDD: koordynuje operacje domenowe, nie należy do żadnego Aggregate
- **Orchestrator**: zbyt generyczne, nie z języka domeny

### Session vs Controller
- **Session** reprezentuje sesję wykonania z cyklem życia
- **Controller** to anty-wzorzec (anemic domain model)

### Handler vs Executor
- **Handler** obsługuje konkretny typ (ActionStep)
- **Executor** wykonuje niższego poziomu (akcje)

### Resolver vs Executor
- **Resolver** rozwiązuje/tłumaczy (bloki → kroki)
- **Executor** wykonuje akcje

### Registry vs Manager
- **Registry** rejestruje i przechowuje
- **Manager** jest zbyt generyczne

### Monitor vs Manager
- **Monitor** aktywnie monitoruje warunki
- **Manager** jest zbyt generyczne

---

## 📅 Kolejność Implementacji - Dlaczego Tak?

1. **Krok 1 pierwszy** - musimy usunąć cross-layer dependency na początku
2. **Kroki 2-3** - proste rename'y bez zależności między sobą
3. **Krok 4** - `ExecutionSession` musi być przed `StepSequenceExecutor` (który z niego korzysta)
4. **Krok 5** - `StepSequenceExecutor` musi być przed `ScriptExecutionService` (który z niego korzysta)
5. **Krok 6** - `ScriptExecutionService` jako główny facade
6. **Kroki 7-8** - wydzielenie nowych klas (opcjonalne, ale zalecane)
7. **Krok 9** - cleanup na końcu

---

## 🚀 Rozpoczęcie Implementacji

Aby rozpocząć, wykonaj:

```powershell
# 1. Upewnij się, że obecny kod się buduje
.\gradlew build

# 2. Stwórz branch dla refaktoryzacji
git checkout -b refactor/ddd-naming-and-structure

# 3. Commituj po każdym kroku
git add .
git commit -m "Refactor: Krok X - opis"
```

**GOTOWY DO IMPLEMENTACJI KROK PO KROKU!** 🎯

