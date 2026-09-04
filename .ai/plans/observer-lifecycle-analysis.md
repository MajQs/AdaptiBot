# Observer Lifecycle, Nesting & Priority — Analysis

Status: draft for discussion
Scope: `ObserverRegistry`, `ObserverInterruptCoordinator`, `ScriptInterpreter`, script tree UX

---

## 1. What the code does today (ground truth)

`ScriptInterpreter.executeSteps(steps)`:

```
enterScope()
for each step:
    processObserverInterrupt()   <- triggered observer handler runs HERE, between steps
    executeStep(step)            <- ObserverStep => activateObserver(step)
exitScope()
```

`ObserverRegistry`:

- one shared virtual thread, lazily started on the first `activateObserver`
- every `checkDelayMs` it evaluates `observersScopeStack.flatMap { it }` — i.e. **all observers in all
  scopes on the stack**, from root to the deepest one
- on the first match it calls `onObserverTriggered` → `queueObserver` (an `AtomicReference`, so a second
  trigger before the handler runs **overwrites** the first one)
- `exitScope()` on an empty stack → `stopObserverThread()` (`interrupt()` + `null`), next
  `activateObserver` creates a **new** thread

Two facts that matter for everything below:

1. An observer's children are its **handler**, not a normal nested block. Reaching the `Observer` step
   only *arms* it.
2. The handler is executed by `executeSteps(...)`, which itself does `enterScope()/exitScope()` —
   so observers armed inside a handler die when the handler ends. That part is already correct.

---

## 2. The reference example

```
Script
├─ Observer 0
│  └─ Action 01
├─ Observer A
│  ├─ Observer B
│  │  ├─ Observer C
│  │  │  └─ Action C1
│  │  ├─ Action B1
│  │  └─ Action B2
│  ├─ Action A1
│  └─ Action A2
├─ Action 1
├─ Observer D
│  └─ Action D1
├─ Action 2
└─ Action 3
```

### 2.1 Normal flow (nothing triggered yet)

| Moment | Armed | Comment |
|---|---|---|
| iteration start | — | root scope pushed, empty |
| after `Observer 0` | `0` | thread starts here |
| after `Observer A` | `0, A` | **B and C are NOT armed** — they belong to A's handler |
| `Action 1` | `0, A` | |
| after `Observer D` | `0, A, D` | matches the intuition: D inactive before this point |
| `Action 2`, `Action 3` | `0, A, D` | |
| iteration end | — | `exitScope()` → stack empty → **thread killed** |
| next iteration | `0` → `0, A` → … | thread recreated |

This confirms the user's reading: `0` and `A` are *effectively* always on, `D` is not.
`B`/`C` are correctly invisible until A fires.

### 2.2 A triggers during `Action 2`

The handler runs between steps, in a fresh scope:

| Moment | Armed (today) | |
|---|---|---|
| A triggers, handler starts | `0, A, D` | |
| handler: `Observer B` | `0, A, D, B` | |
| `Action A1` | `0, A, D, B` | C not armed |
| B triggers → B handler | `0, A, D, B` | |
| handler: `Observer C` | `0, A, D, B, C` | |
| **`Action B1`** | **`0, A, D, B, C`** | ← the state in question |

So today **A and B remain armed while their own handlers run**. `A` can re-trigger itself, and its
handler will be re-entered — unbounded recursion, limited only by the check interval. This is a bug,
not a design decision.

---

## 3. Question 1 — root observers churn on every loop iteration

`0` and `A` are armed at the start of every iteration and disarmed at its end, and each cycle does
`interrupt()` + create a new virtual thread.

**Do not fix this with semantics.** Behaviour is already what the user expects (there is no real
window where a root observer is off). The cost is purely mechanical.

Fix (invisible to the user):

1. Keep **one** observer thread for the whole script run. `exitScope()` on an empty stack must only
   empty the snapshot; the loop parks until something is armed again. Stop the thread only in
   `clearAll()`.
2. Replace `observersScopeStack.flatMap { … }` (a plain `ArrayDeque` mutated by the executor thread
   and read by the observer thread → `ConcurrentModificationException` waiting to happen) with a
   `@Volatile` immutable snapshot rebuilt in `enterScope` / `activateObserver` / `exitScope`.

**Decision: no user-visible `LOCAL` / `GLOBAL` scope.** A second lifetime concept would be
unintuitive — the user already reasons about "where the observer sits in the flow", and an explicit
scope switch would contradict that position. The lexical rule stays the only rule:

> An observer watches from the moment the flow reaches it until the block that armed it ends.

An observer placed at script root is therefore re-armed at the start of every iteration, which the
user simply perceives as "always on" — and after fix (1) this costs nothing at runtime.

---

## 4. Question 2 — are siblings of the triggered observer still active?

`0` and `A` are siblings; `A` fires. **Recommendation: siblings stay active.**

- Users perceive observers as independent watchdogs, not as a mutually exclusive state machine.
- `Observer 0` is typically the "something went badly wrong" guard. Going blind exactly while another
  handler is running is the worst possible moment.
- Any other default forces the user to reason about invisible mutual exclusion.

The opposite need — *"while this handler runs, nothing may interfere"* — is largely covered anyway:
only one handler runs at a time (§6 rule 5) and checking is suspended while a trigger is pending
(§5.2). No per-observer flag is introduced.

---

## 5. Question 3 — are ancestors of the triggered observer still active?

**Recommendation: an observer is never checked while its own handler is running (self re-entrancy
lock). Everything else stays armed.**

During `Action B1`:

| Observer | Armed? | Why |
|---|---|---|
| `0` | ✅ | independent watchdog, not on the handler chain |
| `A` | ❌ | its own handler is running (self-lock) |
| `B` | ❌ | its own handler is running (self-lock) |
| `C` | ✅ | armed by the `Observer C` step, its handler is not running |
| `D` | ✅ | armed earlier in the flow, unrelated |

This reproduces the intuition from the question ("A and B nested → both inactive, C active, 0 stays
active") but derives it from **one rule the user can memorise** instead of from tree depth:

> An observer cannot trigger itself while it is already handling.

Depth-based rules ("everything closer to root is disabled") fail the `Observer 0` case and are
impossible to explain in one sentence.

### 5.1 Re-arm after the handler ends

**Decision: always re-arm immediately, no configuration.**

- A `Re-arm after the condition becomes false` policy would introduce hidden state the user cannot
  see in the tree.
- A `Cooldown` field is redundant: the user can already express it as a `Wait` action at the end of
  the observer's handler, which is visible in the tree and reuses a concept they know.

The self-lock (§5) is what prevents trigger storms; no extra policy is needed.

### 5.2 Checking is suspended while a trigger is pending

Once an observer has matched and is queued, **no observer condition is evaluated** until that
handler is picked up and started. Reasons:

- Any match found in the meantime could not be executed anyway (single handler at a time), so the
  evaluation is pure waste — and it is the expensive part (screen capture + matching).
- It removes a race in which a lower-priority observer overwrites/queues behind an already decided
  trigger between the match and the pickup.
- User-visible effect is exactly what they expect: "one thing at a time".

So the observer thread's state machine is: `checking → matched/queued (idle) → handler running
(idle, triggered observer self-locked) → checking`.

---

## 6. Consolidated model (documentation order)

1. **Arming.** Reaching an `Observer` step arms it. Its children are its handler and do not run then.
2. **Lifetime.** An observer stays armed until the block that armed it ends (iteration, group,
   branch, loop body, or an enclosing handler). There is no other lifetime concept.
3. **Self-lock.** An observer is not checked while its own handler runs.
4. **Independence.** All other armed observers keep being checked, including during a handler.
5. **One at a time.** Only one handler runs at a time. A triggered observer waits for the current
   step to finish, then runs.
6. **No checking while a trigger is pending.** Between a match and the start of its handler, no
   observer condition is evaluated.
7. **Order.** If several observers match in the same tick, the one higher in the list wins.
8. **Re-arm.** After a handler ends, the observer is armed again immediately. A delay, if wanted, is
   expressed as a `Wait` action inside the handler.

---

## 7. Priority & conflicts (user-facing)

- **Ordering:** list order in the tree; first armed match wins. No numeric priority field
  (users never remember whether 0 or 100 is highest, and ties need a tie-breaker anyway).
- **Conflict policy: fixed, single, non-configurable — *wait for the current step to finish*.**
  - Interrupting a running step is unsafe: a half-finished drag, a partially typed text or a still
    held mouse button leave the target application in an undefined state.
  - Skipping a trigger would make observers unreliable, which defeats their purpose as watchdogs.
  - One fixed rule is one sentence of documentation and nothing to configure or render in the tree.

Latency cost is bounded by the duration of a single step, which the user already controls.

Known related defect: `triggeredObserver` is an `AtomicReference`; a second trigger before
`processObserverInterrupt()` runs silently **overwrites** the first. With rules 6–7 above a second
trigger can no longer be produced while one is pending, so the reference becomes correct by
construction — but the pending flag must be cleared only *after* the handler finishes, and checking
must be suspended for that whole window.

---

## 8. Communicating state to the user

- Runtime status strip: `Armed: 0, C, D · Handling: B`, fed by the registry snapshot.
- Tree badges: `armed`, `handling` (no `GLOBAL` badge — there is no global scope).
- INFO logs on arm / trigger / handler end:
  `Observer 'B' triggered — running 3 steps`, `Observer 'B' handler finished — re-armed`.
- Save-time warning when a handler cannot plausibly falsify its own condition (likely trigger loop);
  the suggested remedy is a `Wait` action or a dismiss action inside the handler.

---

## 9. Implementation checklist

| # | Change | Size | Impact | Risk |
|---|---|---|---|---|
| 1 | `@Volatile` immutable snapshot instead of `flatMap` over `ArrayDeque` | S | correctness | Low |
| 2 | `handlingObservers` set excluded from the snapshot (self-lock) | S | high | Low |
| 3 | Suspend checking while a trigger is pending (match → handler start) | S | high | Low |
| 4 | Long-lived observer thread; park instead of `interrupt()` on empty stack | S | medium | Medium |
| 5 | Deterministic list-order match (first armed observer in tree order wins) | S | medium | Low |
| 6 | Share one screen capture per tick across all observer conditions | M | high | Medium |
| 7 | UI: `armed` / `handling` badges, runtime status strip | M | UX | Low |
| 8 | Save-time validation warning for self-retriggering handlers | S | UX | Low |

Suggested order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8.
Items 1–5 are pure bug fixes / semantics enforcement and need no new user-facing configuration —
the model has **zero** new settings: no scope, no re-arm policy, no conflict policy.

