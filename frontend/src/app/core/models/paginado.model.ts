export interface Paginated<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SortState {
  key: string;
  dir: 'asc' | 'desc';
}

export interface Paginacion {
  page?: number;
  size?: number;
  sort?: string;
  [key: string]: string | number | undefined;
}

export function paginar(params?: Paginacion): Record<string, string | number> {
  const out: Record<string, string | number> = {};
  if (params == null) {
    return out;
  }
  if (params.page != null) {
    out['page'] = params.page;
  }
  if (params.size != null) {
    out['size'] = params.size;
  }
  if (params.sort) {
    out['sort'] = params.sort;
  }
  for (const [k, v] of Object.entries(params)) {
    if (v != null && !['page', 'size', 'sort'].includes(k)) {
      out[k] = v;
    }
  }
  return out;
}

export function sortParam(sort: SortState | null | undefined): string | undefined {
  return sort ? `${sort.key},${sort.dir}` : undefined;
}
