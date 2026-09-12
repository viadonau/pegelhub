export interface PageDto<T> {
  items: T[];
  offset: number;
  limit: number;
  total: number;
}
