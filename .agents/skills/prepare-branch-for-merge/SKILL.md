---
name: prepare-branch-for-merge
description: Final pre-merge git history cleanup for PegelHub branches. Use only when explicitly invoked to prepare a finished branch for merge into main, clean/squash/reword commits, or restack dependent branches. Do not use for normal development commits, checkpoint commits, or ordinary "commit this current state" requests.
---

# Prepare Branch For Merge

## Target Shape

- Branch tip passes relevant checks.
- History from merge-base to HEAD is short and logical.
- No `wip`, `fix tests`, `review cleanup` or checkpoint commits remain.
- Each final commit is coherent, reviewable, and reasonably revertable.
- Commit subjects are imperative and specific, e.g. `Add Caddy ingress mode`.

## Squash Rules

- Squash fixes into the commit they make correct: test fixes, typo fixes, review cleanup, missed imports, mapper adjustments, small follow-up corrections.
- Keep separate commits for separate concerns: implementation, docs/runbook, ADR/planning note, unrelated refactor.
- Split or postpone unrelated work instead of hiding it inside a broad commit.

## Workflow

1. Inspect `git status -sb`, current branch, upstream, and `git merge-base main HEAD`.
2. Review `git log --oneline --decorate --graph <merge-base>..HEAD` and `git diff --stat <merge-base>...HEAD`.
3. If uncommitted changes exist, do not rewrite history until they are intentionally included, checkpointed, or left out.
4. Propose the final commit list when the cleanup is non-trivial.
5. Create a backup branch before rewriting: `backup/<branch>-before-merge-cleanup-<YYYYMMDD>`.
6. Use `git commit --fixup`, `git rebase -i --autosquash`, `git reset -p`, or split commits as needed.
7. Run relevant checks; at minimum run formatting/diff checks and the test/build scope touched by the branch.
8. Push rewritten feature branches with `git push --force-with-lease`, never plain `--force`.
9. If the branch has stacked children, rebase them onto the rewritten parent and push them with `--force-with-lease`.
10. Finish with the final commit list, checks run, current branch, and any backup refs.

## Equivalence Check

When cleanup is meant to preserve the branch tip exactly, record before rewriting:

- `before_tree=$(git rev-parse HEAD^{tree})`

After rewriting, record:

- `after_tree=$(git rev-parse HEAD^{tree})`

Require the tree hashes to match before pushing. If they differ, stop and explain the intentional content difference or restore from the backup branch.

## Guardrails

- Never rewrite `main`.
- Never silently drop changes.
- Never revert unrelated user work.
- Prefer fast-forward-ready history; avoid merge commits unless explicitly requested.
