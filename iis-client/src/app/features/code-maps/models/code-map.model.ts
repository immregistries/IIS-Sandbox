export interface CodeMap {
  [tableName: string]: CodeEntry[];
}

export interface CodeEntry {
  value: string;
  label: string;
}
