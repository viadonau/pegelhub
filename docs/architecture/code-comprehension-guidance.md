# Code Comprehension Guidance

Research checked on 2026-09-13 for the quality, notification, measurement and
authorization integrations. The recommendations below are engineering judgments
informed by empirical studies, not experimentally validated rules for PegelHub.
The target is less reconstruction work for a maintainer, not fewer lines or more
comments.

## Principles

1. **Name the domain meaning, including the important distinction.**
   Prefer `matchesActorType` to `user` when a predicate also accepts `CLIENT`,
   and `nextAttemptAt` to `next` when several timestamps coexist. Schankin et al.
   studied 88 Java programmers: informative compound names supported faster
   semantic-defect localization, but not syntax-error detection. This supports
   informative names, not maximizing name length; their expertise subgroup
   analysis was exploratory. [ICPC 2018, author paper](https://brains-on-code.github.io/descriptive-compound-identifier-names.pdf).

2. **Use Javadoc for what a caller must know, not a method-body transcript.**
   Document transaction participation, returned snapshots, absence results,
   ownership requirements and relevant failure semantics. For example,
   `Quality.record` persists findings and notification work in one PostgreSQL
   transaction; derived measurement output happens separately. A getter does
   not need prose repeating its name. Oracle recommends concise summaries and
   implementation-independent contracts covering boundary and concurrency
   behavior. This is official authoring guidance, not empirical evidence that
   Javadoc itself improves comprehension. [Oracle Javadoc guidance](https://www.oracle.com/java/technologies/javase/writing-doc-comments.html).

3. **Reserve inline comments for a reason that the local code cannot explain.**
   Keep the rationale near the branch or query it constrains: a failed commit
   can have succeeded server-side, so recovery must not overwrite committed
   findings. Avoid comments such as "claim a run" above `claim(...)`, and avoid
   a documentation quota. In Nielebock et al.'s study of 277 participants
   (227 professionals), differences between no comments, implementation comments
   and documentation comments were mostly negligible for small tasks. That is
   not evidence against documenting cross-component contracts.
   [Empirical Software Engineering 2019, author paper](https://jacobkrueger.github.io/assets/papers/Nielebock2019Comments.pdf).

4. **Make names, contracts and behavior agree, especially about guarantees.**
   Describe a lease token as rejecting completion from a replaced claim, not as
   guaranteeing exactly-once transport delivery. Explain a delivery snapshot as
   preserving routing/content while the live destination can still disable
   sending; a credential reference is not a credential value. For authorization,
   distinguish actor type from authority rather than calling both "admin access".
   Fakhoury et al. found increased fNIRS-measured load with linguistic
   inconsistencies, including contradictory names and comments. The study had
   15 student participants and only seven in the key paired comparison, so this
   is suggestive evidence, not a production effect-size prediction.
   [ICPC 2018, author paper](https://veneraarnaoudova.ca/wp-content/uploads/2018/03/2018-ICPC-Effect-lexicon-cognitive-load.pdf).

5. **Extract a coherent concept only when the name saves more reading than the call adds.**
   Formatting a bounded finding message is a plausible extraction; scattering
   claim, record, output and release across tiny helpers can obscure sequencing
   and failure handling. Keep transaction and ownership boundaries visible.
   Costa et al. found extraction helped some tasks but hurt simpler ones through
   navigation overhead in an eye-tracking experiment with 32 Java novices,
   supplemented by a survey of 58 others. Their small, static snippets did not
   capture normal IDE navigation or experienced production maintenance.
   [Journal of Systems and Software 2026, accepted author version](https://arxiv.org/html/2602.18579v3).

6. **Check comprehension with maintenance questions, not appearance metrics.**
   After cleanup, trace: what commits together, which configuration is frozen,
   what a stale worker can change, and which actor/authority combination can
   enter an operation. Preserve behavior with focused tests of those boundaries.
   This local review practice is an inference, not a tested intervention here:
   Abdelsalam et al.'s 20-student Java study found comment effects varied by
   snippet, and positive usefulness ratings did not consistently predict better
   performance. A favorable impression alone is therefore weak validation.
   [Empirical Software Engineering 2026](https://link.springer.com/article/10.1007/s10664-025-10721-2).

## Code Layout

Use visual paragraphs inside methods, not uninterrupted blocks of statements.
Separate validation, state lookup, calculation, persistence and result handling
when those phases are present; keep each value next to the check that protects
it. A blank line should mark a change of purpose, not every statement.

Use braced guards, one statement per line, and blank lines between methods.
Wrap long signatures, record components and nested calls so their parts can be
scanned without horizontal reading. In tests, separate setup, actions and
assertions, and give successive scenarios their own paragraphs. Do not add
comments that merely label these phases or extract helpers just to shorten a
method. These are local readability conventions, not measured effect-size
claims from the studies above.

## Limits

These studies use different tasks, populations and comprehension measures;
eye movements and physiological signals are not interchangeable with correctness
or maintenance cost. None establishes a universal method-size limit, comment
ratio, or rule that fewer methods, more comments, or fewer lines are better.
Apply the principles conservatively within the changed integration code, keep
the existing architecture, and update nearby contracts when behavior changes.
