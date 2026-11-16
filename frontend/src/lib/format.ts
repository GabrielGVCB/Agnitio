import type { EventType } from './api';

const EVENT_TYPE_LABELS: Record<EventType, string> = {
  RACE: 'Corrida',
  WEDDING: 'Casamento',
  GRADUATION: 'Formatura',
  CORPORATE: 'Corporativo',
  OTHER: 'Evento',
};

export function eventTypeLabel(type: EventType): string {
  return EVENT_TYPE_LABELS[type] ?? 'Evento';
}

/** Formats an ISO date (yyyy-MM-dd) as "12 de mai. de 2026" without timezone shifts. */
export function formatEventDate(isoDate: string | null | undefined): string | null {
  if (!isoDate) return null;
  const [year, month, day] = isoDate.split('-').map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
}
