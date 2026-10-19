import { Chip } from '@mui/material'
import type { JournalType } from '../types'

const color: Record<JournalType, 'success' | 'warning' | 'info'> = {
  FEE_REVENUE: 'success',
  WRITE_OFF: 'warning',
  RECOVERY_OF_WRITTEN_OFF: 'info',
}

export function TypeBadge({ type }: { type: string }) {
  const known = type === 'FEE_REVENUE' || type === 'WRITE_OFF' || type === 'RECOVERY_OF_WRITTEN_OFF'
  return <Chip size="small" label={type} color={known ? color[type] : 'default'} variant="outlined" />
}
