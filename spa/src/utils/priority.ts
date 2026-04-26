let colors: Record<string, string> = {
  '1': '#EF4444', // Urgent - red
  '3': '#F97316', // High - orange
  '5': '#3B82F6', // Medium - blue
  '7': '#9CA3AF', // Low - gray
  '9': '#D1D5DB', // Minor - light gray
};

const LABELS: Record<string, string> = {
  '1': 'Urgent', '3': 'High', '5': 'Medium', '7': 'Low', '9': 'Minor',
};

/** Initialize with server-provided colors */
export function setPriorityColors(serverColors: Record<string, string>) {
  colors = { ...colors, ...serverColors };
}

export function priorityHex(p: string): string {
  return colors[p] || '#D1D5DB';
}

export function priorityColor(_p: string): string {
  // Return Tailwind-compatible inline style instead of class
  return ''; // Now using inline style with priorityHex
}

export function priorityLabel(p: string): string {
  return LABELS[p] || p;
}

export function dueColor(dueType: string): string {
  if (dueType === '3') return 'text-red-600';
  if (dueType === '5') return 'text-yellow-600';
  return 'text-green-600';
}

/** Get all default colors for settings dialog */
export function getDefaultColors(): Record<string, string> {
  return { ...colors };
}
