import { ColDef } from 'ag-grid-community';

import {
  PH_DATA_GRID_CELL_CLASSES,
  PhDataGridResponsiveVisibility,
  phDataGridTextFilterParams,
} from '../../../ui/data-grid/data-grid';
import { TimeSeriesOverviewView } from '../model/time-series-overview';
import { OpenTimeSeriesCellComponent } from './cells/open-time-series-cell.component';

export function timeSeriesGridColumns(): readonly ColDef<TimeSeriesOverviewView>[] {
  return [
    {
      colId: 'series',
      headerName: 'Messreihe',
      cellClass: PH_DATA_GRID_CELL_CLASSES.primary,
      filter: 'agTextColumnFilter',
      filterParams: phDataGridTextFilterParams(),
      filterValueGetter: ({ data }) => data?.measuringPointName ?? '',
      flex: 1.3,
      minWidth: 170,
      tooltipValueGetter: ({ data }) => data?.measuringPointName ?? '',
      valueGetter: ({ data }) => data?.measuringPointName ?? '',
    },
    {
      colId: 'measurementType',
      headerName: 'Messgröße',
      cellClass: PH_DATA_GRID_CELL_CLASSES.secondary,
      filter: 'agTextColumnFilter',
      filterParams: phDataGridTextFilterParams(),
      filterValueGetter: ({ data }) => data?.measurementTypeLabel ?? '',
      flex: 0.8,
      minWidth: 125,
      tooltipValueGetter: ({ data }) => data?.measurementTypeLabel ?? '',
      valueGetter: ({ data }) => data?.measurementTypeLabel ?? '',
    },
    {
      colId: 'station',
      headerName: 'Pegelstelle',
      cellClass: PH_DATA_GRID_CELL_CLASSES.secondary,
      filter: 'agTextColumnFilter',
      filterParams: phDataGridTextFilterParams(),
      filterValueGetter: ({ data }) => data?.stationLabel ?? '',
      flex: 1.35,
      minWidth: 210,
      tooltipValueGetter: ({ data }) => data?.stationLabel ?? '',
      valueGetter: ({ data }) => data?.stationLabel ?? '',
    },
    {
      colId: 'latestValue',
      headerName: 'Letzter Messwert',
      cellClass: PH_DATA_GRID_CELL_CLASSES.emphasis,
      flex: 1,
      minWidth: 110,
      suppressHeaderFilterButton: true,
      tooltipValueGetter: ({ data }) => data?.latestMeasurement.valueLabel ?? '',
      valueFormatter: ({ data }) => data?.latestMeasurement.valueLabel ?? '',
      valueGetter: ({ data }) => data?.latestMeasurement.value,
      wrapHeaderText: true,
    },
    {
      colId: 'activity',
      headerName: 'Letzte Aktivität',
      flex: 0.8,
      minWidth: 150,
      suppressHeaderFilterButton: true,
      tooltipValueGetter: ({ data }) => data?.latestMeasurement.timestamp ?? '',
      valueFormatter: ({ data }) => data?.latestMeasurement.activityLabel ?? '',
      valueGetter: ({ data }) => data?.latestMeasurement.observedAt ?? '',
    },
    {
      colId: 'open',
      headerName: '',
      cellClass: PH_DATA_GRID_CELL_CLASSES.action,
      cellRenderer: OpenTimeSeriesCellComponent,
      lockPinned: true,
      lockPosition: 'right',
      maxWidth: 48,
      minWidth: 48,
      pinned: 'right',
      sortable: false,
      suppressHeaderFilterButton: true,
      valueGetter: ({ data }) => data?.measuringPointName ?? '',
      width: 48,
    },
  ];
}

export const TIME_SERIES_GRID_VISIBLE_FROM = {
  activity: 'wide',
  measurementType: 'medium',
  station: 'medium',
} satisfies PhDataGridResponsiveVisibility;
