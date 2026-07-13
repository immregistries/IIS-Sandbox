export interface ProcessingFlavor {
  key: string;
  behaviorDescription: string;
}

export function getActiveFlavors(tenantName: string, allFlavors: ProcessingFlavor[]): Set<string> {
  const segments = tenantName.split(/[\s_]+/).map((s) => s.toLowerCase());
  const active = new Set<string>();
  for (const flavor of allFlavors) {
    if (segments.includes(flavor.key.toLowerCase())) {
      active.add(flavor.key);
    }
  }
  return active;
}
