# Canonicalize Monitoring Vocabulary At The Domain Boundary

## Context

Legacy integrations use short codes, German labels, and inconsistent spacing
for observed properties and bank sides. The monitoring UI should not classify
these values with fuzzy regular expressions, and persisted aliases make the
workflow contract unstable.

## Decision

Normalize clear observed-property aliases to `water-level`,
`water-temperature`, and `discharge` when an `ObservedPropertyCode` is
constructed. Unknown values are trimmed and preserved; ambiguous values such
as `level` and `temperature` are not guessed. Bank input accepts only the
canonical `left` and `right` values. Unknown non-null bank values are rejected.
The measuring-point schema constrains persisted bank values accordingly.

## Consequences

API and frontend code share stable semantic slugs. Human labels and localized
formatting stay in the frontend, while the backend contract no longer needs
to expose legacy aliases. New ambiguous property codes remain visible rather
than being silently misclassified. V2 deployments must reset their metadata
database when adopting this decision; no compatibility migration is provided
before the V2 schema is released.
