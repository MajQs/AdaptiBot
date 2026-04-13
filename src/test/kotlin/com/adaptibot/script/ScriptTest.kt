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

    private fun groupStep(vararg children: Step) = GroupStep(
        label = "group",
        container = StepContainer(steps = children.toMutableList())
    )

    private fun conditionalStep(vararg trueSteps: Step, elseSteps: List<Step> = emptyList()) =
        ConditionalStep(
            label = "cond",
            condition = Condition.ElementExists(VisualMatcher.ImagePresent(ImagePattern("", 0.8))),
            trueContainer = StepContainer(steps = trueSteps.toMutableList()),
            elseContainer = StepContainer(steps = elseSteps.toMutableList())
        )

    private fun scriptWith(vararg steps: Step) = Script.restore(
        id = ScriptId(),
        name = "S",
        description = "",
        rootContainer = StepContainer(steps = steps.toMutableList()),
        settings = ScriptSettings()
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
        assertThrows(IllegalArgumentException::class.java) { Script.create("   ") }
    }

    @Test
    fun `restore preserves all fields`() {
        val id = ScriptId()
        val step = actionStep()
        val settings = ScriptSettings(defaultDelayBefore = 200)
        val script = Script.restore(id, "Restored", "desc", StepContainer(steps = mutableListOf(step)), settings)

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
        assertThrows(IllegalArgumentException::class.java) { script.rename("  ") }
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
        val script = scriptWith()
        script.updateDescription("")
        assertEquals("", script.description)
    }

    // ── addStep ───────────────────────────────────────────────────────────────

    @Test
    fun `addStepToContainer with rootContainer id appends step to root list`() {
        val script = Script.create("Script")
        val step = actionStep("first")
        script.addStepToContainer(script.rootContainer.id, step)
        assertEquals(1, script.steps.size)
        assertEquals(step.id, script.steps[0].id)
    }

    @Test
    fun `addStepToContainer with rootContainer id appends multiple steps in order`() {
        val script = Script.create("Script")
        val s1 = actionStep("a"); val s2 = actionStep("b")
        script.addStepToContainer(script.rootContainer.id, s1)
        script.addStepToContainer(script.rootContainer.id, s2)
        assertEquals(listOf(s1.id, s2.id), script.steps.map { it.id })
    }

    // ── addStepAfter ──────────────────────────────────────────────────────────

    @Test
    fun `addStepAfter inserts step after target at root level`() {
        val s1 = actionStep("first"); val s2 = actionStep("second")
        val script = scriptWith(s1)
        assertTrue(script.addStepAfter(s1.id, s2))
        assertEquals(listOf(s1.id, s2.id), script.steps.map { it.id })
    }

    @Test
    fun `addStepAfter returns false when target id not found`() {
        assertFalse(Script.create("Script").addStepAfter(StepId(), actionStep()))
    }

    // ── addStepToContainer ────────────────────────────────────────────────────

    @Test
    fun `addStepToContainer adds step inside a GroupStep`() {
        val child = actionStep("child")
        val group = groupStep(child)
        val script = scriptWith(group)
        val newStep = actionStep("new")

        assertTrue(script.addStepToContainer(group.container.id, newStep))
        val updatedGroup = script.steps[0] as GroupStep
        assertTrue(updatedGroup.container.steps.any { it.id == newStep.id })
    }

    @Test
    fun `addStepToContainer adds step to elseContainer of ConditionalStep`() {
        val cond = conditionalStep(actionStep("if-step"))
        val script = scriptWith(cond)
        val elseStep = actionStep("else-step")

        assertTrue(script.addStepToContainer(cond.elseContainer.id, elseStep))
        val updated = script.steps[0] as ConditionalStep
        assertTrue(updated.elseContainer.steps.any { it.id == elseStep.id })
    }

    @Test
    fun `addStepToContainer adds step to trueContainer of ConditionalStep`() {
        val cond = conditionalStep()
        val script = scriptWith(cond)
        val trueStep = actionStep("true-step")

        assertTrue(script.addStepToContainer(cond.trueContainer.id, trueStep))
        val updated = script.steps[0] as ConditionalStep
        assertTrue(updated.trueContainer.steps.any { it.id == trueStep.id })
    }

    @Test
    fun `addStepToContainer returns false when container id not found`() {
        assertFalse(Script.create("Script").addStepToContainer(ContainerId(), actionStep()))
    }

    // ── removeStep ────────────────────────────────────────────────────────────

    @Test
    fun `removeStep removes step from root list`() {
        val s1 = actionStep("first"); val s2 = actionStep("second")
        val script = scriptWith(s1, s2)
        assertTrue(script.removeStep(s1.id))
        assertEquals(1, script.steps.size)
        assertEquals(s2.id, script.steps[0].id)
    }

    @Test
    fun `removeStep removes nested step`() {
        val nested = actionStep("nested")
        val group = groupStep(nested)
        val script = scriptWith(group)
        assertTrue(script.removeStep(nested.id))
        val updatedGroup = script.steps[0] as GroupStep
        assertTrue(updatedGroup.container.steps.isEmpty())
    }

    @Test
    fun `removeStep returns false when step not found`() {
        assertFalse(Script.create("Script").removeStep(StepId()))
    }

    // ── updateStep ────────────────────────────────────────────────────────────

    @Test
    fun `updateStep replaces step at root level`() {
        val original = ActionStep(label = "original", action = Action.System.Wait(100))
        val script = scriptWith(original)
        assertTrue(script.updateStep(original))
        assertEquals(original.id, script.steps[0].id)
    }

    @Test
    fun `updateStep replaces nested step inside GroupStep`() {
        val child = actionStep("child")
        val script = scriptWith(groupStep(child))
        assertTrue(script.updateStep(child))
    }

    @Test
    fun `updateStep returns false when step id not found`() {
        assertFalse(Script.create("Script").updateStep(actionStep()))
    }

    // ── findStep ──────────────────────────────────────────────────────────────

    @Test
    fun `findStep returns step from root list`() {
        val step = actionStep("find-me")
        val found = scriptWith(step).findStep(step.id)
        assertNotNull(found); assertEquals(step.id, found!!.id)
    }

    @Test
    fun `findStep returns nested step inside GroupStep`() {
        val nested = actionStep("nested")
        val found = scriptWith(groupStep(nested)).findStep(nested.id)
        assertNotNull(found); assertEquals(nested.id, found!!.id)
    }

    @Test
    fun `findStep returns null when step not found`() {
        assertNull(Script.create("Script").findStep(StepId()))
    }

    // ── findContainer ─────────────────────────────────────────────────────────

    @Test
    fun `findContainer returns trueContainer of ConditionalStep`() {
        val cond = conditionalStep(actionStep())
        val found = scriptWith(cond).findContainer(cond.trueContainer.id)
        assertNotNull(found); assertEquals(cond.trueContainer.id, found!!.id)
    }

    @Test
    fun `findContainer returns rootContainer by its id`() {
        val script = Script.create("Script")
        val found = script.findContainer(script.rootContainer.id)
        assertNotNull(found); assertEquals(script.rootContainer.id, found!!.id)
    }

    @Test
    fun `findContainer returns null when container id not found`() {
        assertNull(Script.create("Script").findContainer(ContainerId()))
    }

    // ── moveStep ──────────────────────────────────────────────────────────────

    @Test
    fun `moveStep reorders steps at root level`() {
        val s1 = actionStep("first"); val s2 = actionStep("second"); val s3 = actionStep("third")
        val script = scriptWith(s1, s2, s3)
        assertTrue(script.moveStep(s3.id, null, 0))
        assertEquals(s3.id, script.steps[0].id)
    }

    @Test
    fun `moveStep returns false when step id not found`() {
        assertFalse(Script.create("Script").moveStep(StepId(), null, 0))
    }

    // ── steps snapshot immutability ───────────────────────────────────────────

    @Test
    fun `steps returns a snapshot - external modification does not affect aggregate`() {
        val script = Script.create("Script")
        script.addStepToContainer(script.rootContainer.id, actionStep())
        val snapshot = script.steps.toMutableList()
        snapshot.clear()
        assertEquals(1, script.steps.size)
    }
}
