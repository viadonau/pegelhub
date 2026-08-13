# PegelHub Frontend

## Product

PegelHub is an internal monitoring frontend for viadonau operators. It authenticates with Keycloak and presents time-series metadata and measurements from the Core API. The current contract surface is deliberately narrow:

- a filterable time-series overview;
- a time-series detail page with current value, metadata, and chart history;
- loading, empty, authorization, and service-error states.

Metadata administration and other operator configuration workflows are outside this branch.

## Users

Operators monitor Danube measurements for long periods at a desk. The interface must remain calm, readable, and predictable. Values, timestamps, names, and chart lines take priority over decorative chrome.

## Design Direction

- **Operational, not promotional.** This is a working application, not a dashboard landing page.
- **Data first.** The overview is one row per time series. Station and measuring-point context appear as supporting columns and detail metadata.
- **Quiet hierarchy.** Flat surfaces, restrained borders, and deliberate spacing replace decorative cards and shadows.
- **Brand correct.** Viadonau blue and Pendant navy carry structure and actions; cyan remains a sparing accent. The viadonau, DHK, and PegelHub assets in `public/brand/` are the source of truth.
- **Designed for long sessions.** Body contrast meets WCAG AA, focus states remain visible, and motion respects `prefers-reduced-motion`.
- **German operator UI.** Current product copy is German. Code and technical documentation stay English.

## Frontend Architecture

Angular standalone components use signals, `httpResource`, zoneless change detection, and lazy feature routes.

- `src/app/core/` owns runtime configuration, Keycloak integration, API clients, theme state, and shared formatting.
- `src/app/features/time-series-overview/` owns the overview projection and its AG Grid feature adapter.
- `src/app/features/time-series-detail/` owns detail resources, view models, metadata, preferences, and the single-series chart workflow.
- `src/app/ui/` contains reusable presentation foundations such as page structure, content state, messages, charts, and the generic data grid.

Feature `model/` folders contain presentation models and pure projection functions. They are not business-domain layers.

## UI Foundations

PrimeNG provides controls and the Viadonau theme preset. Tailwind v4 and `tailwindcss-primeui` remain available for concise layout utilities and future UI work; feature code does not need to force either tool where plain component styles are clearer.

The grid has two intentional layers:

1. `PhDataGridComponent` owns shared AG Grid registration, theme, locale, row identity, empty states, sizing, and responsive column visibility.
2. `PhTimeSeriesGridComponent` owns time-series columns, cell renderers, and feature-specific behavior.

Do not merge those layers. The shared grid should stay domain-neutral while feature adapters remain free to express their own columns.

The line chart is intentionally single-series because the current detail route represents exactly one time series. RNW and HSW are optional annotations for water-level series and the operator preference is stored locally.

## Theme

`ThemeService` owns light, dark, and system preference. `main.ts` applies the theme before Angular bootstrap to prevent a light-mode flash. Application styles consume semantic `--ph-*` tokens instead of assuming fixed PrimeNG surface numbers.

## Guardrails

- Do not add metadata CRUD or configuration surfaces to this contract branch.
- Do not recreate backend ordering or filtering rules in the frontend when the API contract already guarantees them.
- Do not introduce sibling time-series navigation or raw measurement tables until those workflows are explicitly funded and designed.
- Keep controls keyboard reachable and do not use color as the only state indicator.
- Preserve the generic data-grid abstraction and Tailwind setup as future-facing foundations.
