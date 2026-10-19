import { ACCOUNTS } from '../types'
import type { Journal, Side } from '../types'

export function units(amount: string): number {
  return Math.round(Number(amount) * 10000)
}

export function formatMoney(amount: string | number, currency = 'XXX'): string {
  const value = typeof amount === 'number' ? amount : Number(amount)
  const formatted = Number.isFinite(value) ? value.toFixed(4) : '0.0000'
  return `${formatted} ${currency}`
}

export function formatWhen(iso: string): string {
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
    timeZone: 'UTC',
  }).format(new Date(iso))
}

export function dayKey(iso: string): string {
  return iso.slice(0, 10)
}

export function sumSide(journal: Journal, side: Side): number {
  return journal.lines.filter((line) => line.side === side).reduce((total, line) => total + units(line.amount), 0)
}

export function isBalanced(journal: Journal): boolean {
  return sumSide(journal, 'DR') === sumSide(journal, 'CR')
}

export function accountName(code: string): string {
  return ACCOUNTS.find((account) => account.code === code)?.name ?? code
}

export function normalSide(code: string): Side {
  return ACCOUNTS.find((account) => account.code === code)?.normal ?? 'DR'
}

export function downloadCsv(filename: string, header: string[], rows: string[][]): void {
  const body = [header, ...rows]
    .map((row) => row.map((cell) => `"${cell.replaceAll('"', '""')}"`).join(','))
    .join('\n')
  const blob = new Blob([body], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
