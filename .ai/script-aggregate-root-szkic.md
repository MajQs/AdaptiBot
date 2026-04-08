# Szkic: Script jako Aggregate Root

## Obecny stan – Script to pasywne DTO

```kotlin
data class Script(
    val name: String,
    val steps: List<Step>,
    val settings: ScriptSettings
)
```

---

## Jak wyglądałby Aggregate Root

```kotlin
class Script private constructor(
    val id: ScriptId,
    name: String,
    description: String,
    steps: List<Step>,
    val settings: ScriptSettings
) {
    var name: String = name
        private set

    var description: String = description
        private set

    private val _steps: MutableList<Step> = steps.toMutableList()
    val steps: List<Step> get() = _steps.toList()

    companion object {
        fun create(name: String, description: String = "", settings: ScriptSettings = ScriptSettings()): Script =
            Script(ScriptId.generate(), name, description, emptyList(), settings)

        fun restore(id: ScriptId, name: String, description: String, steps: List<Step>, settings: ScriptSettings): Script =
            Script(id, name, description, steps, settings)
    }

    // ── Metody domenowe – 9 metod ──────────────────────────────────────────

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Script name must not be blank" }
        name = newName
    }

    fun updateDescription(newDescription: String) {
        description = newDescription
    }

    fun addStep(step: Step) {
        _steps.add(step)
    }

    fun addStepAfter(afterId: StepId, step: Step): Boolean =
        StepTreeEditor.insertAfter(_steps, afterId, step)

    fun addStepToParent(parentId: StepId, step: Step): Boolean =
        StepTreeEditor.addToChildren(_steps, parentId, step)

    fun addStepToElse(parentId: StepId, step: Step): Boolean =
        StepTreeEditor.addToElse(_steps, parentId, step)

    fun removeStep(id: StepId): Boolean =
        StepTreeEditor.remove(_steps, id)

    fun updateStep(updated: Step): Boolean =
        StepTreeEditor.replace(_steps, updated)

    fun moveStep(stepId: StepId, targetParentId: StepId?, targetIndex: Int): Boolean {
        val step = StepTreeEditor.find(_steps, stepId) ?: return false
        if (!StepTreeEditor.remove(_steps, stepId)) return false
        return StepTreeEditor.insertAt(_steps, step, targetParentId, targetIndex)
    }
}
```

---

## Ile metod? Dokładnie 9 metod domenowych

| Metoda | Odpowiednik w ScriptViewModel |
|--------|-------------------------------|
| `rename()` | `scriptNameProperty.set()` |
| `updateDescription()` | `scriptDescriptionProperty.set()` |
| `addStep()` | `addStep()` |
| `addStepAfter()` | `addStepAfter()` |
| `addStepToParent()` | `addStepToParent()` |
| `addStepToElse()` | `addStepToElse()` |
| `removeStep()` | `removeStep()` |
| `updateStep()` | `updateStep()` |
| `moveStep()` | `moveStep()` |

---

## Ważna obserwacja – cała logika traversal trafia do `StepTreeEditor`

Logika rekurencyjnego przeszukiwania drzewa (te ~200 linii z ScriptViewModel) trafia do osobnego obiektu:

```kotlin
// Wewnętrzny helper – nie jest częścią publicznego API
internal object StepTreeEditor {
    fun insertAfter(steps: MutableList<Step>, afterId: StepId, newStep: Step): Boolean { ... }
    fun addToChildren(steps: MutableList<Step>, parentId: StepId, step: Step): Boolean { ... }
    fun addToElse(steps: MutableList<Step>, parentId: StepId, step: Step): Boolean { ... }
    fun remove(steps: MutableList<Step>, id: StepId): Boolean { ... }
    fun replace(steps: MutableList<Step>, updated: Step): Boolean { ... }
    fun find(steps: List<Step>, id: StepId): Step? { ... }
    fun insertAt(steps: MutableList<Step>, step: Step, parentId: StepId?, index: Int): Boolean { ... }
}
```

`StepTreeEditor` to **nie jest klasa domenowa** – to implementacyjny detal agregatu.
Dzięki temu `Script` pozostaje czytelny, a logika traversal jest w jednym miejscu i **w pełni testowalna bez JavaFX**.

---

## Jak zmienia się ScriptViewModel?

Zamiast 528 linii (z logiką domenową) → ~200 linii (tylko synchronizacja z UI):

```kotlin
class ScriptViewModel(private val script: Script, ...) {

    // Zamiast własnych list i rekurencji:
    fun addStep(step: Step) {
        script.addStep(step)          // logika w agregacie
        refreshStepsView()            // tylko odświeżenie UI
        isDirtyProperty.set(true)
    }

    fun removeStep(id: StepId) {
        script.removeStep(id)
        refreshStepsView()
        isDirtyProperty.set(true)
    }

    // itd. – każda metoda to 3 linie
}
```

---

## Co NIE zmienia się w modelu

- `Step`, `ActionStep`, `BlockStep`, `ConditionalBlock`, `GroupBlock`, `ObserverStep` – bez zmian
- `Action`, `Target`, `Condition`, `VisualMatcher` – bez zmian
- `ScriptSettings` – bez zmian
- Serializacja – `Script` nadal serializuje się do tego samego JSON (dodaje się tylko `id` do JSON)

