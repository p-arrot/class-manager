export const SUBMISSION_STATUS_OPTIONS = [
  { label: '未提交', value: 'not_submitted' },
  { label: '答题中', value: 'in_progress' },
  { label: '待批改', value: 'submitted' },
  { label: '已批改', value: 'graded' },
  { label: '已退回', value: 'returned' },
  { label: '缺考', value: 'absent' },
  { label: '特殊处理', value: 'special' },
] as const

export function submissionStatusLabel(value: string) {
  return SUBMISSION_STATUS_OPTIONS.find(option => option.value === value)?.label ?? value
}

export function submissionStatusType(value: string): 'success' | 'warning' | 'error' | 'info' | 'default' {
  if (value === 'graded') return 'success'
  if (value === 'submitted') return 'warning'
  if (value === 'returned' || value === 'absent' || value === 'special') return 'error'
  if (value === 'in_progress') return 'info'
  return 'default'
}
