# Potential operator station metadata

## Columns

| CSV column     | Extracted meaning                                                                                   |
| -------------- | --------------------------------------------------------------------------------------------------- |
| `Stationsname` | Human station or section name. Also contains section headers such as `Donau:` and `Donaukanal:`.    |
| `Parameter`    | Available measured values per station. Examples: `W`, `WT`, `Q`; combinations such as `W + WT + Q`. |
| `HZB/HD`       | External hydrological station identifier. Values include numeric HZB IDs and `2HD...` IDs.          |
| `DBMS-Nr.`     | DBMS station number.                                                                                |
| `Besitzer`     | Owner/operator. Values in this file: `via`, `DHK`, `VHP`.                                           |
| `Strom-km`     | River kilometer. Donau rows are around `1894-1949`; Donaukanal rows start at `0`.                   |
| `PNP [m ü.A]`  | Pegelnullpunkt / gauge datum elevation above Austrian reference.                                    |
| `Ufer`         | Bank side. Values in this file: `li`, `re`.                                                         |

## Parameter legend

| Code  | German label     | Unit / interpretation |
| ----- | ---------------- | --------------------- |
| `W`   | Wasserstand      | `cm` or `m ü.A.`      |
| `Q`   | Abfluss          | `m³/s`                |
| `WT`  | Wassertemperatur | `°C`                  |
| `PNP` | Pegelnullpunkt   | `m ü.A.`              |

## Data quality notes

- The file contains section rows and blank rows, so any import should classify rows before treating them as stations.
- `Stationsname` repeats for `FAH Nußdorf`, once under Donau and once under Donaukanal, with different context. Name alone is not a stable key.
- `PNP` sometimes appears as `0`, which may mean unknown/not applicable rather than a real datum. Confirm before using it in calculations.
- `Parameter` values include variants such as `W + (WT)`, which likely means optional or special availability. Confirm the intended meaning with operators.
