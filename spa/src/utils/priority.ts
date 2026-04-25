const PRIORITY_COLORS: Record<string, string> = {
  '1': 'bg-red-500',    // Urgent
  '3': 'bg-orange-400', // High
  '5': 'bg-blue-400',   // Medium
  '7': 'bg-gray-400',   // Low
  '9': 'bg-gray-300',   // Minor
};

const PRIORITY_LABELS: Record<string, string> = {
  '1': 'Urgent',
  '3': 'High',
  '5': 'Medium',
  '7': 'Low',
  '9': 'Minor',
};

export function priorityColor(p: string) {
  return PRIORITY_COLORS[p] || 'bg-gray-300';
}

export function priorityLabel(p: string) {
  return PRIORITY_LABELS[p] || p;
}

export function dueColor(dueType: string) {
  if (dueType === '3') return 'text-red-600';    // Overdue
  if (dueType === '5') return 'text-yellow-600';  // Due
  return 'text-green-600';                         // Scheduled
}
