export type JournalType = 'FEE_REVENUE' | 'WRITE_OFF' | 'RECOVERY_OF_WRITTEN_OFF'

export type Side = 'DR' | 'CR'

export type AccountCode =
  | 'FEE_RECEIVABLE'
  | 'FEE_INCOME'
  | 'PRINCIPAL_RECEIVABLE'
  | 'WRITE_OFF_EXPENSE'
  | 'CASH_RECOVERY'
  | 'RECOVERY_OF_WRITE_OFF'

export interface JournalLine {
  lineId: string
  accountCode: string
  side: Side
  amount: string
}

export interface Journal {
  journalId: string
  sourceEventId: string
  advanceId: string
  journalType: string
  marketCode: string
  currency: string
  postedAt: string
  lines: JournalLine[]
}

export interface BilledRow {
  rail: string
  businessDate: string
  billed: string
  collected: string
}

export interface AccountMeta {
  code: AccountCode
  name: string
  normal: Side
}

export const ACCOUNTS: AccountMeta[] = [
  { code: 'FEE_RECEIVABLE', name: 'Fee receivable', normal: 'DR' },
  { code: 'FEE_INCOME', name: 'Fee income', normal: 'CR' },
  { code: 'PRINCIPAL_RECEIVABLE', name: 'Principal receivable', normal: 'DR' },
  { code: 'WRITE_OFF_EXPENSE', name: 'Write-off expense', normal: 'DR' },
  { code: 'CASH_RECOVERY', name: 'Cash recovery', normal: 'DR' },
  { code: 'RECOVERY_OF_WRITE_OFF', name: 'Recovery of write-off', normal: 'CR' },
]
