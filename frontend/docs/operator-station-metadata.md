# Legacy station source notes

This note records observations from an operator CSV reviewed during early frontend discovery. The
source file is not versioned in this repository, so this is neither an import contract nor evidence
of current source values. Recheck the current source and confirm ambiguous values with operators
before building an import. PegelHub currently manages metadata through the Core API; the frontend
does not import this CSV or provide metadata administration.

## Column observations

| CSV column     | Observed meaning                                                                                  |
| -------------- | ------------------------------------------------------------------------------------------------- |
| `Stationsname` | Station name or a section heading such as `Donau:` or `Donaukanal:`.                              |
| `Parameter`    | Legacy measurement codes such as `W`, `WT`, and `Q`, including combinations and optional markers. |
| `HZB/HD`       | External hydrological identifier; observed values included numeric HZB IDs and `2HD...` IDs.      |
| `DBMS-Nr.`     | DBMS station number.                                                                              |
| `Besitzer`     | Owner/operator; observed abbreviations included `via`, `DHK`, and `VHP`.                          |
| `Strom-km`     | River kilometer; Donau rows were around `1894-1949`, while Donaukanal rows started at `0`.        |
| `PNP [m ü.A]`  | Pegelnullpunkt, the gauge-zero elevation in metres above Adria.                                   |
| `Ufer`         | Bank side; observed abbreviations were `li` and `re`.                                             |

## Current PegelHub mapping

| Source concept                 | Current Core representation                                                                        |
| ------------------------------ | -------------------------------------------------------------------------------------------------- |
| Owner/operator                 | `StationOwner`; abbreviations may fit its optional `shortName`.                                    |
| Station and water-body context | `Station.name` and `Station.waterBody`; section headings must not become station records.          |
| River kilometer and bank       | Optional `MeasuringPoint.position.riverKilometer` and `bank` (`left` or `right`).                  |
| PNP                            | Optional `MeasuringPoint.gaugeZeroElevationMAboveAdria`; it is metadata, not an observed property. |
| `W`                            | `water-level`, stored and displayed canonically in `cm`.                                           |
| `WT`                           | `water-temperature`, canonical API unit `Cel`, displayed as `°C`.                                  |
| `Q`                            | `discharge`, canonical API unit `m3/s`, displayed as `m³/s`.                                       |
| HZB/HD and DBMS identifiers    | No field in the current operational metadata catalog.                                              |

Water level is the only observed property that may use the `metres-above-adria` source
representation. Core requires PNP for that source and normalizes incoming values to centimetres.
The CSV's parameter variants therefore need an explicit mapping rather than direct copying.

## Data quality notes

- Section and blank rows must be classified before station rows.
- `Stationsname` included `FAH Nußdorf` under both Donau and Donaukanal, so name alone is not a
  stable key.
- A PNP value of `0` may mean unknown or not applicable rather than a real datum.
- A value such as `W + (WT)` needs operator confirmation; punctuation cannot safely determine
  whether a parameter is optional or available.
