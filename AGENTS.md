# Instructions for coding agents

This repository follows a modular, hexagonal architecture and is implemented in Kotlin. These are architectural
requirements, not suggestions. Use the existing repository structure and naming conventions where they are already
established, but preserve the boundaries and responsibilities described below when adding or changing code.

This file contains durable engineering rules only. Project requirements, feature plans, issue details, generated output,
and temporary decisions belong in the appropriate project documentation or issue.

## 1. Instruction hierarchy and reconnaissance

- Read this file before changing anything. Before working in a subdirectory, look for a closer `AGENTS.md`, `CLAUDE.md`,
  `CONTRIBUTING.md`, or equivalent instruction file and follow the most specific applicable rules.
- Direct user instructions define the task-specific contract. Follow them unless they conflict with safety or an
  explicit repository constraint that cannot be bypassed.
- Inspect the repository before forming an implementation plan. Identify the application entry point, modules, package
  boundaries, public APIs, persistence and external-service boundaries, build commands, test commands, generated files,
  and relevant existing implementations.
- Read callers, implementations, tests, configuration, and serialization models before changing a public contract.
- Treat current source, tests, configuration, and observable behavior as stronger evidence than stale documentation.
- Inspect the working tree before editing. Preserve unrelated or concurrent changes and never overwrite them silently.

## 2. Required module structure

Organize business functionality into modules around cohesive business capabilities. A module may be a top-level package,
a Gradle module, or another repository-established unit, but its internal boundaries must remain explicit.

Use this structure as the default:

```text
<module>/
    <ModuleFacade>.kt              public entry point for other modules
    <ModuleConfiguration>.kt       explicit dependency composition and factories
    domain/                         entities, value objects, domain rules, domain services, ports
    dto/                           data exchanged through the public module contract
    api/                           inbound adapters: HTTP, messaging, CLI, UI, or other delivery mechanisms
    adapter/<technology>/           outbound adapters: database, filesystem, HTTP client, queue, native API, etc.
```

Shared code that is genuinely stable across modules belongs in a small `sharedkernel/` or equivalent shared module.
Technical cross-cutting code belongs in `infrastructure/<concern>/` or the repository's established equivalent. Create
only the directories a module needs: do not add empty layers or speculative adapters.

### Module responsibilities

- **Facade** is the deliberate public entry point. It accepts and returns public DTOs or stable value types and delegates
  to domain logic. It must not expose repositories, adapters, persistence records, framework objects, or vendor types.
- **Domain** owns business rules, invariants, entities, value objects, domain services, and outbound port interfaces.
  Keep it independent of delivery frameworks, persistence implementations, network clients, and UI toolkits.
- **Ports** are small interfaces owned by the consuming module. An outbound port expresses what the domain needs, not how
  a technology provides it. An inbound port may describe a use case behind the facade when that improves separation.
- **DTOs** are boundary data. Keep them focused on data and construction; do not put business decisions, calculations,
  persistence behavior, or external calls in DTOs.
- **API** translates inbound requests, events, commands, or UI actions to the facade/use-case contract and translates
  results or errors back to the delivery format. It never reaches directly into adapters.
- **Adapters** translate between a port and an external technology. Keep vendor models, persistence records, wire models,
  serialization details, and technology-specific exceptions inside the adapter boundary.
- **Configuration** constructs the module graph explicitly. Keep dependency wiring close to the module it assembles and
  use constructor injection. Framework annotations may be used only where the repository requires them; they must not
  replace clear module composition or leak into domain rules.

## 3. Dependency direction and module boundaries

Dependencies point inward toward stable business concepts:

```text
delivery API / UI / CLI  ->  facade or inbound use-case port
facade                    ->  domain services and outbound ports
outbound adapters         ->  outbound ports and external technologies
domain                    ->  domain types, shared kernel, and standard library
```

- The domain defines the ports it needs; adapters implement those ports. Do not define ports in an adapter package merely
  to make an external technology easier to use.
- An API calls a facade or inbound port. It never imports or constructs a persistence adapter, database client, or
  other outbound implementation directly.
- A module uses another module only through that module's facade and public DTO/value types. Never import another
  module's domain internals, API, adapter, persistence model, or configuration implementation.
- Do not pass framework request objects, ORM records, database IDs, HTTP client responses, or vendor exceptions across a
  module boundary. Map them at the boundary.
- Keep dependencies acyclic. If two modules need each other's internals, extract a smaller stable contract or shared
  concept rather than creating a cycle.
- Keep shared kernel types minimal and stable. A shared kernel is not a dumping ground for convenience utilities or
  business rules that belong to one module.
- Prefer explicit composition over service locators, mutable global registries, static singletons, and hidden runtime
  lookups.

## 4. Public surface and visibility

Treat visibility as an architectural tool:

- Make the facade and the DTO/value types required by other modules public.
- Make module implementation classes, ports, domain services, adapters, configuration helpers, and internal models
  `internal` by default. Widen visibility only when a real external contract requires it.
- A public facade should expose an intentionally small API and may use an `internal` constructor so callers cannot bypass
  the module's composition rules. A module factory or configuration object is responsible for constructing it.
- Do not make a class public only to simplify a test. Expose the smallest test factory or public behavior needed by the
  repository's testing conventions.
- Keep DTO properties and collection types immutable at public boundaries. Do not return mutable implementation state.
- When a type must be public because it is persisted, serialized, or exchanged externally, keep its internal behavior
  private and document the boundary mapping.

## 5. Kotlin implementation rules

### Data and domain modeling

- Prefer `val`, immutable `data class`es, immutable collections, and pure functions. Use `var` only when mutation is part
  of the model, required by a framework boundary, or clearly justified by performance.
- Model domain concepts explicitly. Wrap identifiers, names, coordinates, amounts, and other constrained primitives in
  value objects instead of passing unvalidated `String`, numeric, or loosely structured values through the domain.

  ```kotlin
  @JvmInline
  value class CustomerId(val value: String)
  ```

  Validate value-object invariants at construction or at the corresponding untrusted boundary. Do not repeatedly unwrap
  a value object throughout domain logic; unwrap only when mapping to an external representation.
- Use sealed hierarchies or enums for closed alternatives and make `when` expressions exhaustive. Prefer types that make
  invalid states difficult to represent over comments explaining invalid combinations.
- Keep DTOs as data carriers. Put predicates, calculations, validation decisions, state transitions, and business
  operations in domain logic rather than in transport DTOs.
- Keep domain models independent of JSON, database, HTTP, UI, and framework annotations whenever the repository permits.
  Put serialization annotations and persistence mappings on boundary models or adapters.
- Use nullable types to represent genuine absence. Prefer explicit handling with `?:`, `let`, or an explicit domain result;
  use `!!` only when a preceding invariant in the same scope proves non-nullness.
- Prefer domain-specific errors or typed results at meaningful boundaries. Preserve the original cause when translating
  technical failures, and add the context needed to diagnose the operation.

### Functions, mapping, and naming

- Keep functions small, cohesive, and easy to read. Prefer one clear flow and shallow control structures over deeply
  nested branches or clever expressions.
- Name types, functions, and variables after domain concepts or behavior. Avoid vague names and unexplained abbreviations.
- Keep mapping logic close to the boundary it serves. Use private functions or extension functions such as `toDomain()`,
  `toDto()`, and `toResponse()` in the relevant adapter/API package.
- Do not place external mapping in the domain merely because it is convenient. Domain-to-DTO mapping may live in the
  facade when that keeps the public contract clear.
- Use package and file names that reflect the module and layer. Keep one primary responsibility per file unless the
  repository's established style intentionally groups closely related declarations.
- Prefer named parameters and small domain-specific types when positional arguments would be ambiguous.
- Avoid premature abstractions, speculative extension points, compatibility shims, and unused helpers. Add an interface
  when it expresses a real boundary, substitution need, or domain concept.

### State, concurrency, and lifecycle

- Make ownership of mutable state explicit. Prefer one owner and message/event passing over shared mutable state.
- When state is shared, document and enforce its synchronization, visibility, ordering, and lifecycle. Do not assume a
  `volatile` field makes compound operations atomic.
- Preserve coroutine cancellation and thread interruption. Do not catch `CancellationException` or interruption merely to
  continue work; clean up in `finally` and rethrow or restore the cancellation signal.
- Use structured concurrency for coroutines. Tie child work to the lifecycle of its parent, avoid unscoped global jobs,
  and define what happens on failure or cancellation.
- Give long-lived resources an explicit owner and shutdown path. Close files, clients, threads, executors, and native
  resources deterministically.
- Keep blocking work off event-dispatch or UI threads and off coroutine dispatchers intended for non-blocking work. Use
  the repository's established execution model and verify it at the boundary.

## 6. Dependency composition and external boundaries

- Construct the object graph in one explicit composition point per module or system boundary. Pass dependencies via
  constructors; avoid hidden field injection and runtime lookups.
- Provide an in-memory or fake implementation for an outbound port when the repository's tests need deterministic module
  execution. Keep it next to the port or in the test-support area according to repository conventions.
- Keep external calls behind an adapter. The adapter owns timeouts, retries required by the current contract, request and
  response mapping, authentication integration, and technology-specific error handling.
- Validate untrusted input at the outer boundary, then pass validated domain types inward. Do not duplicate transport
  validation throughout the domain.
- Translate technical failures at the layer that understands their meaning. Preserve a low-level cause for diagnostics,
  but expose stable domain errors to callers when the failure has business meaning.
- Do not add fallback behavior, retries, caching, or configuration switches without a current requirement and a defined
  invalidation/failure policy.

## 7. Testing rules

- Test observable behavior through the smallest public surface that represents the use case: normally a module facade,
  public API, command-line entry point, UI boundary, or integration endpoint.
- Read existing tests before adding or changing one. Follow the repository's test framework, source layout, fixtures,
  builders, naming, assertions, and execution commands. Do not introduce a second testing style.
- Prefer business behavior and contract tests over tests coupled to private classes, private methods, or implementation
  call sequences.
- Test the relevant happy path, invalid input, expected failure, cancellation/shutdown behavior, and important boundary
  conditions. Keep each test focused on one behavior.
- Use real domain logic and in-memory adapters where practical. Mock only a genuine external boundary or an interaction
  that an in-memory implementation cannot represent; do not mock domain objects to make a unit test convenient.
- Keep test fixtures readable and valid by default. Use a factory for simple values and a builder when many scenarios
  vary different fields. Override only the values relevant to the scenario.
- Test module contracts independently and test cross-module behavior through public facades. Integration tests should cross
  the real boundary they claim to verify rather than instantiate private implementation classes.
- Do not weaken, delete, skip, or conditionally disable a failing test. Fix the cause or report the failure and its scope.

## 8. Change workflow and verification

For every non-trivial change, follow this sequence:

1. **Explore.** Inspect the working tree, applicable instructions, module structure, callers, tests, configuration, and
   external boundaries. Identify the current contract and the smallest affected surface.
2. **Plan.** State the files or modules involved, the dependency direction, the behavior being changed, and the checks
   that will prove it. Keep one active task at a time for multi-step work.
3. **Implement.** Make the smallest complete change. Preserve existing naming, formatting, visibility, and composition
   conventions. Keep new code inside the correct module and layer.
4. **Verify.** Run the narrowest relevant formatter, static analysis, compiler, tests, and build commands, then broaden
   validation when practical. Re-run checks after changing their inputs.
5. **Exercise the surface.** Use the artifact as a real user would: invoke a public library API, run the CLI including
   help and invalid input, call the service boundary, or drive the UI and inspect visible behavior and console errors.
6. **Review.** Inspect the final diff for accidental changes, leaked secrets, generated files, debug code, broken public
   boundaries, architecture violations, and missing tests or documentation.

## 9. Documentation, scope, and safety

- Update documentation when a change alters public behavior, setup, operations, data format, or developer workflow.
  Keep documentation close to the source of truth and avoid duplicating facts that are obvious from code or configuration.
- Keep one authoritative source for each rule. Remove stale or duplicated instructions instead of adding another layer
  that can diverge.
- Keep unrelated cleanup out of feature changes. Do not reformat, rename, upgrade dependencies, modify lockfiles, change
  release settings, or rewrite generated files unless the task requires it and the impact is understood.
- Do not use destructive version-control operations, rewrite history, force-push, or commit changes unless explicitly
  requested.
- Protect secrets and sensitive data. Never add credentials, tokens, private keys, personal data, or machine-specific
  paths to source, tests, logs, commits, or documentation.
- For migrations, data changes, or irreversible operations, establish a backup, rollback, or dry-run path before
  execution and validate it on representative data.
- If an existing design conflicts with the requested behavior or creates a material safety risk, state the issue clearly
  and apply the smallest coherent correction rather than hiding it behind a workaround.

## 10. Completion gate and handoff

Work is complete only when all of the following are true:

1. Every requested behavior is implemented with no knowingly partial path.
2. The change preserves the required module and hexagonal boundaries, dependency direction, Kotlin conventions, and
   applicable repository instructions.
3. Changed files pass available formatting, diagnostics, compilation, build, and relevant test checks, or every failure
   is explicitly explained with its cause and scope.
4. The artifact has been exercised through its matching user-visible surface.
5. The final diff contains only intended changes and no secrets, debug artifacts, generated noise, or unrelated cleanup.
6. The handoff states the user-facing result, files changed, checks run and outcomes, manual behavior observed, known
   limitations, pre-existing issues left untouched, and any validation that could not run.
