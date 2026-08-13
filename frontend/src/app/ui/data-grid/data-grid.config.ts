import { isDevMode } from '@angular/core';
import { AG_GRID_LOCALE_DE } from '@ag-grid-community/locale';
import {
  ClientSideRowModelModule,
  CellStyleModule,
  ColumnApiModule,
  ITextFilterParams,
  LocaleModule,
  Module,
  RowApiModule,
  TextFilterModule,
  themeQuartz,
  TooltipModule,
  ValidationModule,
} from 'ag-grid-community';

export const PH_DATA_GRID_CELL_CLASSES = {
  action: 'ph-data-grid-cell-action',
  emphasis: 'ph-data-grid-cell-emphasis',
  primary: 'ph-data-grid-cell-primary',
  secondary: 'ph-data-grid-cell-secondary',
} as const;

export const PH_DATA_GRID_MODULES: Module[] = [
  ClientSideRowModelModule,
  CellStyleModule,
  ColumnApiModule,
  LocaleModule,
  RowApiModule,
  TextFilterModule,
  TooltipModule,
  ...(isDevMode() ? [ValidationModule] : []),
];

export const PH_DATA_GRID_LOCALE = AG_GRID_LOCALE_DE;

export const PH_DATA_GRID_THEME = themeQuartz.withParams({
  accentColor: 'var(--p-primary-color)',
  backgroundColor: 'var(--ph-surface)',
  borderColor: 'var(--ph-border)',
  browserColorScheme: 'inherit',
  cellHorizontalPadding: '12px',
  focusShadow: '0 0 0 2px color-mix(in oklch, var(--p-primary-color), transparent 55%)',
  fontFamily: 'var(--font-sans)',
  fontSize: '14px',
  foregroundColor: 'var(--ph-text)',
  headerBackgroundColor: 'var(--ph-surface-subtle)',
  headerFontSize: '12px',
  headerFontWeight: 700,
  headerHeight: '44px',
  headerRowBorder: {
    color: 'var(--ph-border)',
    width: 1,
  },
  headerTextColor: 'var(--ph-text-muted)',
  iconColor: 'var(--ph-text-subtle)',
  iconSize: '14px',
  pinnedColumnBorder: {
    color: 'var(--ph-border)',
    width: 1,
  },
  rowBorder: {
    color: 'var(--ph-border)',
    width: 1,
  },
  rowHoverColor: 'var(--ph-surface-subtle)',
  subtleTextColor: 'var(--ph-text-muted)',
  wrapperBorder: {
    color: 'var(--ph-border)',
    width: 1,
  },
  wrapperBorderRadius: '8px',
});

export function phDataGridTextFilterParams(): ITextFilterParams {
  return {
    buttons: ['reset'],
    debounceMs: 150,
    defaultOption: 'contains',
    maxNumConditions: 1,
    trimInput: true,
  };
}
