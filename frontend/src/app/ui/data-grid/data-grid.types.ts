export interface PhDataGridRow {
  id: string;
}

export type PhDataGridLayout = 'narrow' | 'medium' | 'wide';

export type PhDataGridResponsiveVisibility = Readonly<
  Record<string, Exclude<PhDataGridLayout, 'narrow'>>
>;
