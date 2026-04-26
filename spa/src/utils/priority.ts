let colors: Record<string, string> = {
  '1': '#EF4444', '3': '#F97316', '5': '#3B82F6', '7': '#9CA3AF', '9': '#D1D5DB',
};

let labels: Record<string, string> = {
  '1': 'Urgent', '3': 'High', '5': 'Medium', '7': 'Low', '9': 'Minor',
};

export function setPriorityColors(serverColors: Record<string, string>) {
  colors = { ...colors, ...serverColors };
}

/** Initialize with server-provided translated names */
export function setPriorityLabels(serverLabels: Record<string, string>) {
  labels = { ...labels, ...serverLabels };
}

export function priorityHex(p: string): string {
  return colors[p] || '#D1D5DB';
}

export function priorityLabel(p: string): string {
  return labels[p] || p;
}

export function dueColor(dueType: string): string {
  if (dueType === '3') return 'text-red-600';
  if (dueType === '5') return 'text-yellow-600';
  return 'text-green-600';
}

export function priorityColor(_p: string): string { return ''; }

export function getDefaultColors(): Record<string, string> {
  return { ...colors };
}
