import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { PhDataGridComponent } from './data-grid.component';

describe('PhDataGridComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PhDataGridComponent],
    });
  });

  it('renders identified rows through the shared grid interface', async () => {
    const fixture = TestBed.createComponent(PhDataGridComponent);
    fixture.componentRef.setInput('ariaLabel', 'Testdaten');
    fixture.componentRef.setInput('rows', [{ id: 'row-1', name: 'Wien Brigittenau' }]);
    fixture.componentRef.setInput('columnDefs', [{ field: 'name', headerName: 'Name' }]);
    fixture.detectChanges();

    await vi.waitFor(() => {
      expect(fixture.nativeElement.querySelector('.ag-cell')?.textContent).toContain(
        'Wien Brigittenau',
      );
    });
  });

  it('owns the empty-state presentation', async () => {
    const fixture = TestBed.createComponent(PhDataGridComponent);
    fixture.componentRef.setInput('ariaLabel', 'Testdaten');
    fixture.componentRef.setInput('rows', []);
    fixture.componentRef.setInput('columnDefs', [{ field: 'name', headerName: 'Name' }]);
    fixture.componentRef.setInput('emptyMessage', 'Keine Einträge.');
    fixture.detectChanges();

    await vi.waitFor(() => {
      const emptyState = fixture.nativeElement.querySelector('.ph-data-grid-empty');
      expect(emptyState?.textContent).toContain('Keine Einträge.');
      expect(emptyState?.querySelector('.pi-inbox')).not.toBeNull();
    });
  });

  it('clips long cell values instead of overlapping the next column', async () => {
    const fixture = TestBed.createComponent(PhDataGridComponent);
    fixture.componentRef.setInput('ariaLabel', 'Testdaten');
    fixture.componentRef.setInput('rows', [
      {
        id: 'row-1',
        station: 'Bruno Workflow Station · bruno-1785499786948 · Danube',
        latestValue: '273 cm',
      },
    ]);
    fixture.componentRef.setInput('columnDefs', [
      { field: 'station', headerName: 'Pegelstelle', width: 180 },
      { field: 'latestValue', headerName: 'Letzter Messwert', width: 120 },
    ]);
    fixture.detectChanges();

    await vi.waitFor(() => {
      const stationCell = fixture.nativeElement.querySelector(
        '.ag-row [col-id="station"]',
      ) as HTMLElement | null;
      expect(stationCell).not.toBeNull();

      const styles = getComputedStyle(stationCell!);
      expect(styles.overflow).toBe('hidden');
      expect(styles.textOverflow).toBe('ellipsis');
      expect(styles.whiteSpace).toBe('nowrap');
    });
  });
});
