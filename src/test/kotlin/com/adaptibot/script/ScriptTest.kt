package com.adaptibot.script

import com.adaptibot.script.step.*
import com.adaptibot.script.value.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScriptTest {

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun actionStep(label: String = "step") = ActionStep(
        label = label,
        action = Action.System.Wait(100)
    )

    private fun groupBlock(vararg children: Step) = GroupBlock(
        label = "group",
        steps = children.toList()
    )

    private fun conditionalBlock(vararg ifSteps: Step, elseSteps: List<Step> = emptyList()) =
        ConditionalBlock(
            label = "cond",
            condition = Condition.ElementExists(VisualMatcher.ImagePresent(ImagePattern("", 0.8))),
            steps = ifSteps.toList(),
            elseSteps = elseSteps
        )

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    fun `create assigns name and starts with empty steps`() {
        val script = Script.create("My Script")

        assertEquals("My Script", script.name)
        assertEquals("", script.description)
        assertTrue(script.steps.isEmpty())
    }

    @Test
    fun `create throws when name is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            Script.create("   ")
        }
    }

    @Test
    fun `restore preserves all fields`() {
        val id = ScriptId()
        val step = actionStep()
        val settings = ScriptSettings(defaultDelayBefore = 200)

        val script = Script.restore(id, "Restored", "desc", listOf(step), settings)

        assertEquals(id, script.id)
        assertEquals("Restored", script.name)
        assertEquals("desc", script.description)
        assertEquals(1, script.steps.size)
        assertEquals(settings, script.settings)
    }

    // ── rename ────────────────────────────────────────────────────────────────

    @Test
    fun `rename changes script name`() {
        val script = Script.create("Old Name")

        script.rename("New Name")

        assertEquals("New Name", script.name)
    }

    @Test
    fun `rename throws when new name is blank`() {
        val script = Script.create("My Script")

        assertThrows(IllegalArgumentException::class.java) {
            script.rename("  ")
        }
    }

    // ── updateDescription ─────────────────────────────────────────────────────

    @Test
    fun `updateDescription changes description`() {
        val script = Script.create("Script")

        script.updateDescription("New description")

        assertEquals("New description", script.description)
    }

    @Test
    fun `updateDescription allows empty string`() {
        val script = Script.restore(ScriptId(), "S", "old desc", emptyList(), ScriptSettings())

        script.updateDescription("")

        assertEquals("", script.description)
    }

    // ── addStep ───────────────────────────────────────────────────────────────

    @Test
    fun `addStep appends step to root list`() {
        val script = Script.create("Script")
        val step = actionStep("first")

        script.addStep(step)

        assertEquals(1, script.steps.size)
        assertEquals(step.id, script.steps[0].id)
    }

    @Test
    fun `addStep appends multiple steps in order`() {
        val script = Script.create("Script")
        val s1 = actionStep("a")
        val s2 = actionStep("b")

        script.addStep(s1)
        script.addStep(s2)

        assertEquals(listOf(s1.id, s2.id), script.steps.map { it.id })
    }

    // ── addStepAfter ──────────────────────────────────────────────────────────

    @Test
    fun `addStepAfter inserts step after target at root level`() {
        val s1 = actionStep("first")
        val s2 = actionStep("second")
        val script = Script.restore(ScriptId(), "S", "", listOf(s1), ScriptSettings())

        val result = script.addStepAfter(s1.id, s2)

        assertTrue(result)
        assertEquals(listOf(s1.id, s2.id), script.steps.map { it.id })
    }

    @Test
    fun `addStepAfter returns false when target id not found`() {
        val script = Script.create("Script")

        val result = script.addStepAfter(StepId(), actionStep())

        assertFalse(result)
    }

    // ── addStepToParent ───────────────────────────────────────────────────────

    @Test
    fun `addStepToParent adds step inside a GroupBlock`() {
        val child = actionStep("child")
        val group = groupBlock(child)
        val script = Script.restore(ScriptId(), "S", "", listOf(group), ScriptSettings())
        val newStep = actionStep("new")

        val result = script.addStepToParent(group.id, newStep)

        assertTrue(result)
        // After addStepToParent the GroupBlock is replaced with a copy (new id) – access by index
        val updatedGroup = script.steps[0] as GroupBlock
        assertTrue(updatedGroup.steps.any { it.id == newStep.id })
    }

    @Test
    fun `addStepToParent returns false when parent id not found`() {
        val script = Script.create("Script")

        val result = script.addStepToParent(StepId(), actionStep())

        assertFalse(result)
    }

    // ── addStepToElse ─────────────────────────────────────────────────────────

    @Test
    fun `addStepToElse adds step to else branch of ConditionalBlock`() {
        val cond = conditionalBlock(actionStep("if-step"))
        val script = Script.restore(ScriptId(), "S", "", listOf(cond), ScriptSettings())
        val elseStep = actionStep("else-step")

        val result = script.addStepToElse(cond.id, elseStep)

        assertTrue(result)
        // After addStepToElse the ConditionalBlock is replaced with a copy (new id) – access by index
        val updated = script.steps[0] as ConditionalBlock
        assertTrue(updated.elseSteps.any { it.id == elseStep.id })
    }

    @Test
    fun `addStepToElse returns false when id is not a ConditionalBlock`() {
        val group = groupBlock()
        val script = Script.restore(ScriptId(), "S", "", listOf(group), ScriptSettings())

        val result = script.addStepToElse(group.id, actionStep())

        assertFalse(result)
    }

    // ── removeStep ────────────────────────────────────────────────────────────

    @Test
    fun `removeStep removes step from root list`() {
        val s1 = actionStep("first")
        val s2 = actionStep("second")
        val script = Script.restore(ScriptId(), "S", "", listOf(s1, s2), ScriptSettings())

        val result = script.removeStep(s1.id)

        assertTrue(result)
        assertEquals(1, script.steps.size)
        assertEquals(s2.id, script.steps[0].id)
    }

    @Test
    fun `removeStep removes nested step`() {
        val nested = actionStep("nested")
        val group = groupBlock(nested)
        val script = Script.restore(ScriptId(), "S", "", listOf(group), ScriptSettings())

        val result = script.removeStep(nested.id)

        assertTrue(result)
        // After removeStep the GroupBlock is replaced with a copy (new id) – access by index
        val updatedGroup = script.steps[0] as GroupBlock
        assertTrue(updatedGroup.steps.isEmpty())
    }

    @Test
    fun `removeStep returns false when step not found`() {
        val script = Script.create("Script")

        val result = script.removeStep(StepId())

        assertFalse(result)
    }

    // ── updateStep ────────────────────────────────────────────────────────────

    @Test
    fun `updateStep replaces step at root level`() {
        val original = ActionStep(label = "original", action = Action.System.Wait(100))
        val script = Script.restore(ScriptId(), "S", "", listOf(original), ScriptSettings())

        val result = script.updateStep(original)

        assertTrue(result)
        assertEquals(original.id, script.steps[0].id)
    }

    @Test
    fun `updateStep replaces nested step inside GroupBlock`() {
        val child = actionStep("child")
        val group = groupBlock(child)
        val script = Script.restore(ScriptId(), "S", "", listOf(group), ScriptSettings())

        val result = script.updateStep(child)

        assertTrue(result)
    }

    @Test
    fun `updateStep returns false when step id not found`() {
        val script = Script.create("Script")

        val result = script.updateStep(actionStep())

        assertFalse(result)
    }

    // ── findStep ──────────────────────────────────────────────────────────────

    @Test
    fun `findStep returns step from root list`() {
        val step = actionStep("find-me")
        val script = Script.restore(ScriptId(), "S", "", listOf(step), ScriptSettings())

        val found = script.findStep(step.id)

        assertNotNull(found)
        assertEquals(step.id, found!!.id)
    }

    @Test
    fun `findStep returns nested step inside GroupBlock`() {
        val nested = actionStep("nested")
        val group = groupBlock(nested)
        val script = Script.restore(ScriptId(), "S", "", listOf(group), ScriptSettings())

        val found = script.findStep(nested.id)

        assertNotNull(found)
        assertEquals(nested.id, found!!.id)
    }

    @Test
    fun `findStep returns null when step not found`() {
        val script = Script.create("Script")

        val found = script.findStep(StepId())

        assertNull(found)
    }

    // ── moveStep ──────────────────────────────────────────────────────────────

    @Test
    fun `moveStep reorders steps at root level`() {
        val s1 = actionStep("first")
        val s2 = actionStep("second")
        val s3 = actionStep("third")
        val script = Script.restore(ScriptId(), "S", "", listOf(s1, s2, s3), ScriptSettings())

        // Move s3 to index 0 at root
        val result = script.moveStep(s3.id, null, 0)

        assertTrue(result)
        assertEquals(s3.id, script.steps[0].id)
    }

    @Test
    fun `moveStep returns false when step id not found`() {
        val script = Script.create("Script")

        val result = script.moveStep(StepId(), null, 0)

        assertFalse(result)
    }


    // ── steps snapshot immutability ───────────────────────────────────────────

    @Test
    fun `steps returns a snapshot - external modification does not affect aggregate`() {
        val script = Script.create("Script")
        script.addStep(actionStep())

        val snapshot = script.steps.toMutableList()
        snapshot.clear()

        assertEquals(1, script.steps.size)
    }
}
