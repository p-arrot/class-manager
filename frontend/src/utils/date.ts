import dayjs from 'dayjs'

/**
 * Format an ISO timestamp string for display.
 * @param s - ISO timestamp string
 * @param fmt - output format: 'date' = YYYY-MM-DD, 'datetime' = YYYY-MM-DD HH:mm:ss, 'full' = YYYY-MM-DD HH:mm:ss
 */
export function formatDate(s: string | null | undefined, fmt: 'date' | 'datetime' | 'full' = 'datetime'): string {
  if (!s) return ''
  const d = parseDisplayDate(s)
  if (!d.isValid()) return ''
  if (fmt === 'date') return d.format('YYYY-MM-DD')
  return d.format('YYYY-MM-DD HH:mm:ss')
}

/**
 * Format a date range for display.
 */
export function formatDateRange(start: string | null | undefined, end: string | null | undefined): string {
  return `${formatDate(start, 'date')} — ${formatDate(end, 'date')}`
}

/**
 * Convert a millisecond timestamp from Naive UI date pickers to the backend's
 * local datetime string format without applying UTC timezone offsets.
 */
export function toLocalDateTime(value: number | null | undefined): string | undefined {
  if (!value) return undefined
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DDTHH:mm:ss') : undefined
}

function parseDisplayDate(value: string) {
  const normalized = value.trim()
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(normalized) && !/[zZ]|[+-]\d{2}:?\d{2}$/.test(normalized)) {
    return dayjs(normalized.replace('T', ' '))
  }
  return dayjs(normalized)
}
