package com.adaptibot.ui.view

import com.adaptibot.script.step.*
import com.adaptibot.script.value.*
import com.adaptibot.script.value.Target as ScriptTarget
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.PauseTransition
import javafx.animation.Timeline
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.Duration

object StepCellGraphic {

    private const val STRIP_HEIGHT = 18.0
    private const val ANIM_MS      = 150.0

    /**
     * Builds the full cell graphic as a [VBox]:
     *  - the step content row
     *  - optional "＋ inside" strip (single-container steps: GroupStep, ObserverStep, loop steps)
     *  - "＋ after" strip (all steps, slides in on hover)
     *
     * Strips start collapsed (prefHeight = 0, isManaged = false) so they
     * take no space when hidden.  On hover they slide open smoothly.
     *
     * @param onAddAfter        called when the "after" insert strip is pressed
     * @param onAddInside       called when the "inside" insert strip is pressed (single-container steps only)
     */
    fun build(
        step: Step,
        isActive: Boolean,
        isEnabled: Boolean = true,
        parentDisabled: Boolean = false,
        isWatching: Boolean = false,
        isHandling: Boolean = false,
        onAddAfter:  ((anchorX: Double, anchorY: Double) -> Unit)? = null,
        onAddInside: ((anchorX: Double, anchorY: Double) -> Unit)? = null
    ): VBox {
        val isSingleContainer = step.containers().size == 1

        // ── content row ───────────────────────────────────────────────────
        val badge = badge(step)
        val labelText = Label(step.label ?: defaultLabel(step)).apply {
            styleClass.add("step-label-text")
        }
        val detail = Label(detail(step)).apply {
            styleClass.add("step-detail-text")
        }
        val textBox = VBox(0.0, labelText, detail).apply {
            alignment = Pos.CENTER_LEFT
        }
        val dragHandle = Label("⠿").apply {
            styleClass.add("step-detail-text")
            style = "-fx-padding: 0 6 0 0; -fx-cursor: open-hand;"
        }
        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        // Runtime state of an observer: "watching" while it is armed, "handling" while its own
        // steps are running (it cannot trigger itself in the meantime).
        val stateBadge = when {
            isHandling -> stateBadge("▶ handling", "observer-state-handling")
            isWatching -> stateBadge("👁 watching", "observer-state-watching")
            else -> null
        }

        val contentRow = HBox(4.0, dragHandle, badge, textBox, spacer).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add("step-cell-box")
            if (stateBadge != null) children.add(stateBadge)
            if (isActive) styleClass.add("step-cell-active")
            if (!isEnabled || parentDisabled) styleClass.add("step-cell-disabled")
        }

        // ── insert strips ─────────────────────────────────────────────────────
        val insideStrip = if (isSingleContainer && onAddInside != null) buildInsertStrip(label = "inside") else null
        val afterStrip  = buildInsertStrip(label = if (isSingleContainer) "after" else null)

        // wire press callbacks
        afterStrip.setOnMousePressed { e ->
            val bounds = afterStrip.localToScreen(afterStrip.boundsInLocal)
            if (bounds != null) onAddAfter?.invoke(bounds.minX, bounds.minY + bounds.height / 2)
            e.consume()
        }
        insideStrip?.setOnMousePressed { e ->
            val bounds = insideStrip.localToScreen(insideStrip.boundsInLocal)
            if (bounds != null) onAddInside?.invoke(bounds.minX, bounds.minY + bounds.height / 2)
            e.consume()
        }

        // initially collapsed and removed from layout
        listOfNotNull(insideStrip, afterStrip).forEach { strip ->
            strip.prefHeight = 0.0
            strip.opacity    = 0.0
            strip.isManaged  = false
            strip.isVisible  = false
        }

        // ── assemble ──────────────────────────────────────────────────────
        val wrapper = VBox(0.0).apply {
            children.addAll(buildList {
                add(contentRow)
                if (insideStrip != null) add(insideStrip)
                add(afterStrip)
            })
            val allStrips = listOfNotNull(insideStrip, afterStrip).toTypedArray()
            val showDelay = PauseTransition(Duration.millis(350.0)).apply {
                setOnFinished { showStrips(*allStrips) }
            }
            setOnMouseEntered { showDelay.playFromStart() }
            setOnMouseExited  {
                showDelay.stop()
                hideStrips(*allStrips)
            }
        }

        return wrapper
    }

    // ── Container header ──────────────────────────────────────────────────────

    /**
     * Renders a [StepContainer] header row (e.g. "IF TRUE" / "IF ELSE").
     * Non-draggable, no "after" strip. Only interaction: "＋ inside".
     */
    fun buildContainerHeader(
        container: StepContainer,
        label: String,
        parentDisabled: Boolean = false,
        onAddInside: ((anchorX: Double, anchorY: Double) -> Unit)?
    ): VBox {
        val isTrueBranch = label == "IF TRUE"
        val headerLabel = Label("▸  $label").apply {
            styleClass.addAll(
                "branch-header-label",
                if (isTrueBranch) "branch-header-true" else "branch-header-else"
            )
        }
        val countBadge = Label("${container.steps.size}").apply { styleClass.add("branch-header-count") }
        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        val headerRow = HBox(6.0, headerLabel, spacer, countBadge).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add("branch-header-cell")
            if (parentDisabled) styleClass.add("step-cell-disabled")
        }

        val insideStrip = buildInsertStrip(label = "inside")
        insideStrip.setOnMousePressed { e ->
            val bounds = insideStrip.localToScreen(insideStrip.boundsInLocal)
            if (bounds != null) onAddInside?.invoke(bounds.minX, bounds.minY + bounds.height / 2)
            e.consume()
        }
        insideStrip.prefHeight = 0.0; insideStrip.opacity = 0.0
        insideStrip.isManaged  = false; insideStrip.isVisible = false

        return VBox(0.0, headerRow, insideStrip).apply {
            val showDelay = PauseTransition(Duration.millis(350.0)).apply {
                setOnFinished { showStrips(insideStrip) }
            }
            setOnMouseEntered { showDelay.playFromStart() }
            setOnMouseExited  { showDelay.stop(); hideStrips(insideStrip) }
        }
    }

    // ── Insert strip builder ──────────────────────────────────────────────────

    private fun buildInsertStrip(label: String?): StackPane {
        val makeLine = {
            Region().apply {
                styleClass.add("insert-strip-line")
                maxWidth = Double.MAX_VALUE
                HBox.setHgrow(this, Priority.ALWAYS)
            }
        }
        val plusLabel = Label(if (label != null) "＋  $label" else "＋").apply {
            styleClass.add("insert-strip-plus")
        }
        val inner = HBox(6.0).apply {
            alignment = Pos.CENTER
            children.addAll(makeLine(), plusLabel, makeLine())
        }
        return StackPane(inner).apply {
            styleClass.add("insert-strip")
            minHeight = 0.0
            maxHeight = STRIP_HEIGHT
            setOnMouseEntered { styleClass.add("insert-strip-hovered") }
            setOnMouseExited  { styleClass.remove("insert-strip-hovered") }
        }
    }

    // ── Animation helpers ─────────────────────────────────────────────────────

    private fun showStrips(vararg strips: StackPane?) {
        strips.filterNotNull().forEach { strip ->
            strip.isManaged = true; strip.isVisible = true
            Timeline(
                KeyFrame(Duration.millis(ANIM_MS),
                    KeyValue(strip.prefHeightProperty(), STRIP_HEIGHT),
                    KeyValue(strip.opacityProperty(), 1.0)
                )
            ).play()
        }
    }

    private fun hideStrips(vararg strips: StackPane?) {
        strips.filterNotNull().forEach { strip ->
            Timeline(
                KeyFrame(Duration.millis(ANIM_MS),
                    KeyValue(strip.prefHeightProperty(), 0.0),
                    KeyValue(strip.opacityProperty(), 0.0)
                )
            ).apply {
                setOnFinished { strip.isManaged = false; strip.isVisible = false }
                play()
            }
        }
    }

    // ── Badge / label / detail helpers ────────────────────────────────────────

    private fun badge(step: Step): Label = when (step) {
        is ActionStep      -> Label(actionBadgeText(step.action)).apply { styleClass.addAll("step-badge", "step-badge-action") }
        is GroupStep       -> Label("GROUP").apply { styleClass.addAll("step-badge", "step-badge-group") }
        is ConditionalStep -> Label("IF").apply { styleClass.addAll("step-badge", "step-badge-cond") }
        is ObserverStep    -> Label("OBS").apply { styleClass.addAll("step-badge", "step-badge-observer") }
        is WhileStep       -> Label("WHILE").apply { styleClass.addAll("step-badge", "step-badge-loop") }
        is ForStep         -> Label("FOR").apply { styleClass.addAll("step-badge", "step-badge-loop") }
    }

    /** Small badge showing the runtime state of an observer (watching / handling). */
    private fun stateBadge(text: String, stateClass: String): Label =
        Label(text).apply { styleClass.addAll("step-badge", "observer-state-badge", stateClass) }

    private fun actionBadgeText(action: Action): String = when (action) {
        is Action.Mouse.Click              -> "CLICK"
        is Action.Mouse.Drag               -> "DRAG"
        is Action.Mouse.MoveTo             -> "MOVE"
        is Action.Mouse.Scroll             -> "SCROLL"
        is Action.Keyboard.TypeText        -> "TYPE"
        is Action.Keyboard.PressKeys       -> "KEYS"
        is Action.System.Wait              -> "WAIT"
        is Action.System.LaunchApplication -> "LAUNCH"
        is Action.System.CloseApplication  -> "CLOSE"
    }

    private fun defaultLabel(step: Step): String = when (step) {
        is ActionStep      -> actionBadgeText(step.action).lowercase().replaceFirstChar { it.uppercase() } + " step"
        is GroupStep       -> "Group"
        is ConditionalStep -> "Conditional"
        is ObserverStep    -> "Observer"
        is WhileStep       -> "While"
        is ForStep         -> "For"
    }

    private fun detail(step: Step): String = when (step) {
        is ActionStep      -> actionDetail(step.action)
        is GroupStep       -> "${step.container.steps.size} step(s)"
        is ConditionalStep -> "if: ${step.trueContainer.steps.size} / else: ${step.elseContainer.steps.size} step(s)"
        is ObserverStep    -> "${step.container.steps.size} step(s) on trigger"
        is WhileStep       -> "${step.container.steps.size} step(s)  [while condition]"
        is ForStep         -> "${step.container.steps.size} step(s)  ×${step.iterations}"
    }

    private fun actionDetail(action: Action): String = when (action) {
        is Action.Mouse.Click              -> buildString {
            append(action.button.name.lowercase())
            append(" ${action.type.name.lowercase()}")
            if (action.target != null) append(" @ ${targetShort(action.target)}")
        }
        is Action.Mouse.Drag               -> "from ${targetShort(action.from)} → ${targetShort(action.to)}"
        is Action.Mouse.MoveTo             -> "→ ${targetShort(action.target)}"
        is Action.Mouse.Scroll             -> "${action.direction.name.lowercase()} ×${action.amount}"
        is Action.Keyboard.TypeText        -> "\"${action.text.take(30)}${if (action.text.length > 30) "…" else ""}\""
        is Action.Keyboard.PressKeys       -> action.keys.joinToString("+") { it.name }
        is Action.System.Wait              -> "${action.milliseconds} ms"
        is Action.System.LaunchApplication -> action.path
        is Action.System.CloseApplication  -> action.processName
    }

    private fun targetShort(target: ScriptTarget?): String = when (target) {
        is ScriptTarget.AtCoordinate -> "(${target.coordinate.x}, ${target.coordinate.y})"
        is ScriptTarget.AtImage      -> "[image]"
        is ScriptTarget.AtText       -> "[text: \"${target.text.take(20)}${if (target.text.length > 20) "…" else ""}\"]"
        null                         -> "?"
    }
}
