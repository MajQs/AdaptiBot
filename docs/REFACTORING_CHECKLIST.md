# ✅ CHECKLIST REFAKTORYZACJI - Kroki do Wykonania

> **Aktualny status:** 2/9 kroków ukończone (22%)  
> **Następny krok:** KROK 3 - ActionStepHandler

---

## ✅ UKOŃCZONE

### ✅ KROK 1: ExecutionEventPublisher
- [x] Utworzono `core/domain/ExecutionEventPublisher.kt`
- [x] Utworzono `ui/adapter/UiExecutionEventPublisher.kt`
- [x] Zaktualizowano `ScriptOrchestrator.kt`
- [x] Zaktualizowano `ActionExecutor.kt`
- [x] Zaktualizowano `CoreConfiguration.kt`
- [x] Build: ✅ SUCCESS
- [x] Testy: ✅ PASS

### ✅ KROK 2: BlockStepResolver
- [x] Rename `BlockExecutor.kt` → `BlockStepResolver.kt`
- [x] Rename klasy `BlockExecutor` → `BlockStepResolver`
- [x] Rename metody `execute()` → `resolve()`
- [x] Zaktualizowano `StepExecutionOrchestrator.kt`
- [x] Zaktualizowano `CoreConfiguration.kt`
- [x] Build: ✅ SUCCESS
- [x] Testy: ✅ PASS

---

## 🔴 DO ZROBIENIA

### ❌ KROK 3: ActionStepHandler [NASTĘPNY]

**Pliki do zmiany:**
- [ ] Rename `core/domain/ActionExecutor.kt` → `ActionStepHandler.kt`
- [ ] Zmienić nazwę klasy `ActionExecutor` → `ActionStepHandler`
- [ ] Usunąć alias `ActionExecutor as ActionExecutorImpl` (zmienić na bezpośredni import)
- [ ] Zaktualizować `StepExecutionOrchestrator.kt`:
  - [ ] Zmienić typ dependency: `ActionStepHandler`
  - [ ] Zmienić nazwę zmiennej: `actionStepHandler`
  - [ ] Zaktualizować wywołania
- [ ] Zaktualizować `CoreConfiguration.kt`:
  - [ ] Zmienić konstrukcję: `ActionStepHandler(...)`
  - [ ] Zaktualizować import

**Weryfikacja:**
- [ ] `.\gradlew build` - kompiluje się?
- [ ] `.\gradlew test` - testy przechodzą?
- [ ] Brak błędów w IDE?

**Commit:**
```bash
git add .
git commit -m "Refactor: Krok 3 - Rename ActionExecutor to ActionStepHandler"
```

---

### ❌ KROK 4: ExecutionSession

**Pliki do zmiany:**
- [ ] Rename `core/domain/ExecutionController.kt` → `ExecutionSession.kt`
- [ ] Zmienić nazwę klasy `ExecutionController` → `ExecutionSession`
- [ ] Rename metod:
  - [ ] `setActiveStep()` → `recordActiveStep()`
  - [ ] `finish()` → `completeExecution()`
- [ ] Ukryć `getScope()`:
  - [ ] Zmienić na `private var executionScope`
  - [ ] Dodać metodę `internal fun launchInScope(block: suspend CoroutineScope.() -> Unit)`
- [ ] Zaktualizować `ScriptOrchestrator.kt`:
  - [ ] Zmienić typ dependency: `ExecutionSession`
  - [ ] Zmienić wywołania metod
  - [ ] Zmienić `getScope()?.launch` → `launchInScope`
- [ ] Zaktualizować `StepExecutionOrchestrator.kt`:
  - [ ] Zmienić typ dependency: `ExecutionSession`
  - [ ] Zmienić wywołania metod
- [ ] Zaktualizować `CoreConfiguration.kt`:
  - [ ] Zmienić konstrukcję: `ExecutionSession()`

**Weryfikacja:**
- [ ] `.\gradlew build` - kompiluje się?
- [ ] `.\gradlew test` - testy przechodzą?
- [ ] Brak błędów w IDE?

**Commit:**
```bash
git add .
git commit -m "Refactor: Krok 4 - Rename ExecutionController to ExecutionSession"
```

---

### ❌ KROK 5: StepSequenceExecutor

**Pliki do zmiany:**
- [ ] Rename `core/domain/StepExecutionOrchestrator.kt` → `StepSequenceExecutor.kt`
- [ ] Zmienić nazwę klasy `StepExecutionOrchestrator` → `StepSequenceExecutor`
- [ ] Rename metod:
  - [ ] `execute()` → `executeSequence()`
  - [ ] `handleTriggeredObserver()` → `processObserverInterrupt()`
- [ ] Zaktualizować `ScriptOrchestrator.kt`:
  - [ ] Zmienić typ dependency: `StepSequenceExecutor`
  - [ ] Zmienić nazwę zmiennej: `stepSequenceExecutor`
  - [ ] Zmienić wywołania: `executeSequence()`
- [ ] Zaktualizować `CoreConfiguration.kt`:
  - [ ] Zmienić konstrukcję: `StepSequenceExecutor(...)`

**Weryfikacja:**
- [ ] `.\gradlew build` - kompiluje się?
- [ ] `.\gradlew test` - testy przechodzą?
- [ ] Brak błędów w IDE?

**Commit:**
```bash
git add .
git commit -m "Refactor: Krok 5 - Rename StepExecutionOrchestrator to StepSequenceExecutor"
```

---

### ❌ KROK 6: ScriptExecutionService

**Pliki do zmiany:**
- [ ] Rename `core/domain/ScriptOrchestrator.kt` → `ScriptExecutionService.kt`
- [ ] Zmienić nazwę klasy `ScriptOrchestrator` → `ScriptExecutionService`
- [ ] Rename metody:
  - [ ] `executeInfiniteLoop()` → `executeScriptLoop()`
- [ ] Zaktualizować `CoreFacade.kt`:
  - [ ] Zmienić typ dependency: `ScriptExecutionService`
  - [ ] Zmienić nazwę zmiennej: `scriptExecutionService`
  - [ ] Zmienić import
- [ ] Zaktualizować `CoreConfiguration.kt`:
  - [ ] Zmienić konstrukcję: `ScriptExecutionService(...)`
  - [ ] Zmienić return type w `getFacade()`

**Weryfikacja:**
- [ ] `.\gradlew build` - kompiluje się?
- [ ] `.\gradlew test` - testy przechodzą?
- [ ] Brak błędów w IDE?

**Commit:**
```bash
git add .
git commit -m "Refactor: Krok 6 - Rename ScriptOrchestrator to ScriptExecutionService"
```

---

### ❌ KROK 7: ObserverInterruptCoordinator

**Nowe pliki:**
- [ ] Utworzyć `core/domain/observer/ObserverInterruptCoordinator.kt`

**Zawartość nowej klasy:**
```kotlin
internal class ObserverInterruptCoordinator(
    private val stepSequenceExecutor: StepSequenceExecutor
) {
    private val triggeredObserver = AtomicReference<ObserverStep?>(null)
    
    fun queueObserver(observer: ObserverStep) {
        triggeredObserver.set(observer)
    }
    
    suspend fun processObserverInterrupt() {
        triggeredObserver.getAndSet(null)?.let { observer ->
            logger.info("Executing triggered observer: ${observer.id.value}")
            try {
                stepSequenceExecutor.executeSequence(observer.actionSteps)
            } finally {
                logger.info("Observer execution completed")
            }
        }
    }
}
```

**Pliki do zmiany:**
- [ ] Zaktualizować `StepSequenceExecutor.kt`:
  - [ ] Usunąć `triggeredObserver` field
  - [ ] Usunąć metodę `processObserverInterrupt()`
  - [ ] Dodać dependency: `ObserverInterruptCoordinator`
  - [ ] Zmienić `handleTriggeredObserver()` na delegację
- [ ] Zaktualizować `ObserverManager.kt`:
  - [ ] Zmienić callback z `StepSequenceExecutor` na `ObserverInterruptCoordinator`
- [ ] Zaktualizować `CoreConfiguration.kt`:
  - [ ] Utworzyć instancję `ObserverInterruptCoordinator`
  - [ ] Wstrzyknąć do odpowiednich klas

**Weryfikacja:**
- [ ] `.\gradlew build` - kompiluje się?
- [ ] `.\gradlew test` - testy przechodzą?
- [ ] Brak błędów w IDE?

**Commit:**
```bash
git add .
git commit -m "Refactor: Krok 7 - Extract ObserverInterruptCoordinator"
```

---

### ❌ KROK 8a: Rename ObserverManager → ObserverRegistry

**Pliki do zmiany:**
- [ ] Rename `core/domain/observer/ObserverManager.kt` → `ObserverRegistry.kt`
- [ ] Zmienić nazwę klasy `ObserverManager` → `ObserverRegistry`
- [ ] Zaktualizować `StepSequenceExecutor.kt`:
  - [ ] Zmienić typ dependency: `ObserverRegistry`
  - [ ] Zmienić nazwę zmiennej: `observerRegistry`
- [ ] Zaktualizować `CoreConfiguration.kt`:
  - [ ] Zmienić konstrukcję: `ObserverRegistry(...)`

**Weryfikacja:**
- [ ] `.\gradlew build` - kompiluje się?
- [ ] `.\gradlew test` - testy przechodzą?

**Commit:**
```bash
git add .
git commit -m "Refactor: Krok 8a - Rename ObserverManager to ObserverRegistry"
```

---

### ❌ KROK 8b: Wydzielenie ObserverConditionMonitor

**Nowe pliki:**
- [ ] Utworzyć `core/domain/observer/ObserverConditionMonitor.kt`

**Zawartość nowej klasy:**
```kotlin
internal class ObserverConditionMonitor(
    private val conditionEvaluator: ConditionEvaluator,
    private val pollingIntervalMs: Long
) {
    private val monitoringJob: Job? = null
    
    fun startMonitoring(observers: List<ObserverStep>, onTriggered: (ObserverStep) -> Unit)
    fun stopMonitoring()
    private fun checkCondition(observer: ObserverStep): Boolean
}
```

**Pliki do zmiany:**
- [ ] Zaktualizować `ObserverRegistry.kt`:
  - [ ] Dodać dependency: `ObserverConditionMonitor`
  - [ ] Przenieść logikę pollingu do monitora
  - [ ] Delegować `checkCondition()` do monitora
- [ ] Zaktualizować `CoreConfiguration.kt`:
  - [ ] Utworzyć instancję `ObserverConditionMonitor`
  - [ ] Wstrzyknąć do `ObserverRegistry`

**Weryfikacja:**
- [ ] `.\gradlew build` - kompiluje się?
- [ ] `.\gradlew test` - testy przechodzą?

**Commit:**
```bash
git add .
git commit -m "Refactor: Krok 8b - Extract ObserverConditionMonitor"
```

---

### ❌ KROK 9: Cleanup i Finalizacja

**Dead Code:**
- [ ] Usunąć parametr `priority` z `ObserverRegistry.registerObserver()`
- [ ] Sprawdzić czy są inne nieużywane parametry/metody

**Dokumentacja:**
- [ ] Dodać KDoc do `ScriptExecutionService`
- [ ] Dodać KDoc do `ExecutionSession`
- [ ] Dodać KDoc do `StepSequenceExecutor`
- [ ] Dodać KDoc do `ActionStepHandler`
- [ ] Dodać KDoc do `BlockStepResolver`
- [ ] Dodać KDoc do `ObserverRegistry`
- [ ] Dodać KDoc do `ObserverConditionMonitor`
- [ ] Dodać KDoc do `ObserverInterruptCoordinator`

**Przegląd nazw:**
- [ ] Sprawdzić wszystkie metody publiczne - czy nazwy są jasne?
- [ ] Sprawdzić zmienne - czy nie ma skrótów?
- [ ] Sprawdzić parametry - czy są opisowe?

**Logowanie:**
- [ ] Sprawdzić komunikaty logów - czy są spójne?
- [ ] Sprawdzić poziomy logów - czy odpowiednie?

**Finalna weryfikacja:**
- [ ] `.\gradlew clean build` - świeża kompilacja?
- [ ] `.\gradlew test` - wszystkie testy przechodzą?
- [ ] Ręczny test aplikacji - czy działa?
- [ ] Test wykonania skryptu
- [ ] Test observerów
- [ ] Test conditional blocks

**Commit:**
```bash
git add .
git commit -m "Refactor: Krok 9 - Cleanup and documentation"
```

---

## 🎯 MILESTONE CHECKPOINTS

### Checkpoint 1: Podstawowe Rename'y (po kroku 6)
- [ ] Wszystkie generyczne nazwy (Orchestrator/Controller) usunięte
- [ ] Konflikt nazw ActionExecutor rozwiązany
- [ ] Build: SUCCESS
- [ ] Testy: PASS

### Checkpoint 2: Wydzielenie Klas (po kroku 8)
- [ ] ObserverInterruptCoordinator działa
- [ ] ObserverRegistry + Monitor współpracują
- [ ] Build: SUCCESS
- [ ] Testy: PASS

### Checkpoint 3: Finalizacja (po kroku 9)
- [ ] Brak dead code
- [ ] Wszystkie klasy mają KDoc
- [ ] Clean build: SUCCESS
- [ ] Wszystkie testy: PASS
- [ ] Aplikacja działa poprawnie

---

## 📝 NOTATKI IMPLEMENTACYJNE

### Porady:
1. **Zawsze commituj po każdym kroku** - łatwiejszy rollback
2. **Uruchamiaj build po każdej zmianie** - wczesne wykrycie problemów
3. **Używaj Find & Replace w IDE** - szybsze rename'y
4. **Sprawdzaj import statements** - unikaj nieużywanych importów
5. **Testuj ręcznie po większych zmianach** - upewnij się że działa

### Częste pułapki:
- ⚠️ Nie zapomnij o aktualizacji importów!
- ⚠️ Sprawdź czy testy również używają starych nazw
- ⚠️ Niektóre IDE mogą mieć problem z rename - użyj find & replace
- ⚠️ Po rename pliku sprawdź czy git śledzi zmianę (git mv)

---

**Powodzenia w refaktoryzacji!** 🚀

