## 1. <!-- Task Group Name -->

- [ ] 1.1 <!-- Task description -->
- [ ] 1.2 <!-- Task description -->

## 2. <!-- Task Group Name -->

- [ ] 2.1 <!-- Task description -->
- [ ] 2.2 <!-- Task description -->

<!--
  The Landing group below MUST remain the final group. Renumber it so it comes last;
  never drop, reorder, or reword it. It carries the Implementation Gate, without which
  the change cannot be archived.
-->

## 3. Landing

- [ ] 3.1 Implementation Gate - commit the implementation (do not ask), then run `plannotator review`. Annotations are instructions, not suggestions: carry out every one (say so briefly if you disagree, then do it anyway), and sync the specs yourself if the code now does something they do not describe. Re-review until the human approves. Tick ONLY once they have.
- [ ] 3.2 Land - invoke the `openspec-archive-guard` skill, then archive the change and commit. If the repo has a GitHub remote, push the branch and give the human a compare link to open the PR themselves (do not create it); if it is local-only, tell the human it is landed and theirs to merge or squash back into the base branch (see spec-review.md).
