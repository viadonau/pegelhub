import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
  ViewEncapsulation,
} from '@angular/core';
import { AgGridAngular } from 'ag-grid-angular';
import {
  ColDef,
  GetRowIdParams,
  GridSizeChangedEvent,
  ModelUpdatedEvent,
  OverlayType,
} from 'ag-grid-community';

import { PH_DATA_GRID_LOCALE, PH_DATA_GRID_MODULES, PH_DATA_GRID_THEME } from './data-grid.config';
import { PhDataGridLayout, PhDataGridResponsiveVisibility, PhDataGridRow } from './data-grid.types';

const HEADER_HEIGHT = 44;
const DEFAULT_ROW_HEIGHT = 56;
const EMPTY_GRID_HEIGHT = 176;
const MAX_GRID_HEIGHT = 660;
const NARROW_GRID_WIDTH = 640;
const WIDE_GRID_WIDTH = 960;
const GRID_LAYOUT_RANK: Record<PhDataGridLayout, number> = {
  narrow: 0,
  medium: 1,
  wide: 2,
};

@Component({
  selector: 'ph-data-grid',
  imports: [AgGridAngular],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  templateUrl: './data-grid.component.html',
  styleUrl: './data-grid.component.scss',
})
export class PhDataGridComponent<T extends PhDataGridRow = PhDataGridRow> {
  readonly ariaLabel = input.required<string>();
  readonly rows = input.required<readonly T[]>();
  readonly columnDefs = input.required<readonly ColDef<T>[]>();
  readonly emptyMessage = input('Keine Einträge vorhanden.');
  readonly visibleFrom = input<PhDataGridResponsiveVisibility>({});
  readonly visibleCountChange = output<number>();

  private readonly displayedRowCount = signal<number | null>(null);
  private layout: PhDataGridLayout | null = null;

  protected readonly modules = PH_DATA_GRID_MODULES;
  protected readonly theme = PH_DATA_GRID_THEME;
  protected readonly localeText = PH_DATA_GRID_LOCALE;
  protected readonly rowHeight = DEFAULT_ROW_HEIGHT;
  protected readonly suppressedOverlays: OverlayType[] = ['noRows', 'noMatchingRows'];
  protected readonly gridRows = computed(() => [...this.rows()]);
  protected readonly gridColumnDefs = computed(() => [...this.columnDefs()]);
  protected readonly gridHeight = computed(() => {
    const rowCount = this.displayedRowCount() ?? this.rows().length;
    if (rowCount === 0) {
      return `${EMPTY_GRID_HEIGHT}px`;
    }

    const contentHeight = HEADER_HEIGHT + rowCount * DEFAULT_ROW_HEIGHT + 3;

    return `${Math.min(MAX_GRID_HEIGHT, contentHeight)}px`;
  });
  protected readonly isEmpty = computed(
    () => (this.displayedRowCount() ?? this.rows().length) === 0,
  );
  protected readonly isFilteredEmpty = computed(
    () => this.rows().length > 0 && this.displayedRowCount() === 0,
  );
  protected readonly defaultColDef: ColDef<T> = {
    resizable: false,
    sortable: true,
    suppressHeaderMenuButton: true,
  };
  protected readonly getRowId = ({ data }: GetRowIdParams<T>): string => data.id;

  protected onModelUpdated(event: ModelUpdatedEvent<T>): void {
    this.updateDisplayedRowCount(event.api.getDisplayedRowCount());
  }

  protected onGridSizeChanged(event: GridSizeChangedEvent<T>): void {
    const nextLayout = gridLayout(event.clientWidth);

    if (this.layout === nextLayout) {
      return;
    }

    this.layout = nextLayout;
    applyResponsiveVisibility(event, nextLayout, this.visibleFrom());
  }

  private updateDisplayedRowCount(visibleCount: number): void {
    this.displayedRowCount.set(visibleCount);
    this.visibleCountChange.emit(visibleCount);
  }
}

function gridLayout(width: number): PhDataGridLayout {
  if (width < NARROW_GRID_WIDTH) {
    return 'narrow';
  }

  return width < WIDE_GRID_WIDTH ? 'medium' : 'wide';
}

function applyResponsiveVisibility<T extends PhDataGridRow>(
  event: GridSizeChangedEvent<T>,
  layout: PhDataGridLayout,
  visibleFrom: PhDataGridResponsiveVisibility,
): void {
  const columnsByVisibility = Object.entries(visibleFrom).reduce(
    (columns, [columnId, minimumLayout]) => {
      columns[
        GRID_LAYOUT_RANK[layout] >= GRID_LAYOUT_RANK[minimumLayout] ? 'visible' : 'hidden'
      ].push(columnId);
      return columns;
    },
    { hidden: [] as string[], visible: [] as string[] },
  );

  if (columnsByVisibility.visible.length > 0) {
    event.api.setColumnsVisible(columnsByVisibility.visible, true);
  }

  if (columnsByVisibility.hidden.length > 0) {
    event.api.setColumnsVisible(columnsByVisibility.hidden, false);
  }
}
