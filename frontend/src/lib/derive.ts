import { ACCOUNTS } from '../types'
import type { Journal, Side } from '../types'
import { dayKey, normalSide, units } from './format'

export interface DayPoint {
  date: string
  feeRevenue: number
  writeOff: number
  recovery: number
}

export interface TrialRow {
  accountCode: string
  accountName: string
  debit: string
  credit: string
  net: string
}

export interface LedgerEntry {
  id: string
  postedAt: string
  journalId: string
  journalType: string
  advanceId: string
  debit: string
  credit: string
  running: string
}

export function feeRevenueUnits(journal: Journal): number {
  return journal.lines
    .filter((line) => line.accountCode === 'FEE_INCOME' && line.side === 'CR')
    .reduce((total, line) => total + units(line.amount), 0)
}

export function writeOffUnits(journal: Journal): number {
  return journal.lines
    .filter((line) => line.accountCode === 'WRITE_OFF_EXPENSE' && line.side === 'DR')
    .reduce((total, line) => total + units(line.amount), 0)
}

export function recoveryUnits(journal: Journal): number {
  return journal.lines
    .filter((line) => line.accountCode === 'RECOVERY_OF_WRITE_OFF' && line.side === 'CR')
    .reduce((total, line) => total + units(line.amount), 0)
}

export function dailySeries(rows: Journal[], endDate?: string): DayPoint[] {
  const end = endDate ?? (rows.length ? spanOf(rows).to : utcDay())
  const start = new Date(`${end}T00:00:00Z`)
  const buckets = new Map<string, DayPoint>()
  for (let offset = 29; offset >= 0; offset -= 1) {
    const date = new Date(start)
    date.setUTCDate(start.getUTCDate() - offset)
    const key = date.toISOString().slice(0, 10)
    buckets.set(key, { date: key, feeRevenue: 0, writeOff: 0, recovery: 0 })
  }
  rows.forEach((journal) => {
    const bucket = buckets.get(dayKey(journal.postedAt))
    if (!bucket) return
    bucket.feeRevenue += feeRevenueUnits(journal) / 10000
    bucket.writeOff += writeOffUnits(journal) / 10000
    bucket.recovery += recoveryUnits(journal) / 10000
  })
  return [...buckets.values()]
}

export function spanOf(rows: Journal[]): { from: string; to: string } {
  if (rows.length === 0) {
    const today = utcDay()
    return { from: today, to: today }
  }
  let from = dayKey(rows[0].postedAt)
  let to = from
  rows.forEach((journal) => {
    const day = dayKey(journal.postedAt)
    if (day < from) from = day
    if (day > to) to = day
  })
  return { from, to }
}

export function utcDay(): string {
  return new Date().toISOString().slice(0, 10)
}

export function trialBalance(rows: Journal[], asOf: string): TrialRow[] {
  const accounts: { code: string; name: string; normal: Side }[] = ACCOUNTS.map((account) => ({
    code: account.code,
    name: account.name,
    normal: account.normal,
  }))
  const known = new Set(accounts.map((account) => account.code))
  rows.forEach((journal) => {
    journal.lines.forEach((line) => {
      if (!known.has(line.accountCode)) {
        known.add(line.accountCode)
        accounts.push({ code: line.accountCode, name: line.accountCode, normal: 'DR' })
      }
    })
  })
  const totals = new Map<string, { debit: number; credit: number }>()
  accounts.forEach((account) => totals.set(account.code, { debit: 0, credit: 0 }))
  rows
    .filter((journal) => dayKey(journal.postedAt) <= asOf)
    .forEach((journal) => {
      journal.lines.forEach((line) => {
        const bucket = totals.get(line.accountCode)
        if (!bucket) return
        if (line.side === 'DR') bucket.debit += units(line.amount)
        else bucket.credit += units(line.amount)
      })
    })
  return accounts.map((account) => {
    const bucket = totals.get(account.code) ?? { debit: 0, credit: 0 }
    const netUnits = account.normal === 'DR' ? bucket.debit - bucket.credit : bucket.credit - bucket.debit
    return {
      accountCode: account.code,
      accountName: account.name,
      debit: (bucket.debit / 10000).toFixed(4),
      credit: (bucket.credit / 10000).toFixed(4),
      net: (netUnits / 10000).toFixed(4),
    }
  })
}

export function accountEntries(rows: Journal[], code: string): LedgerEntry[] {
  const normal = normalSide(code)
  let running = 0
  const entries: LedgerEntry[] = []
  rows.forEach((journal) => {
    journal.lines
      .filter((line) => line.accountCode === code)
      .forEach((line) => {
        const amount = units(line.amount)
        if (normal === 'DR') running += line.side === 'DR' ? amount : -amount
        else running += line.side === 'CR' ? amount : -amount
        entries.push({
          id: line.lineId,
          postedAt: journal.postedAt,
          journalId: journal.journalId,
          journalType: journal.journalType,
          advanceId: journal.advanceId,
          debit: line.side === 'DR' ? line.amount : '0.0000',
          credit: line.side === 'CR' ? line.amount : '0.0000',
          running: (running / 10000).toFixed(4),
        })
      })
  })
  return entries
}

export function billedGap(billed: string, collected: string): string {
  return ((units(billed) - units(collected)) / 10000).toFixed(4)
}
