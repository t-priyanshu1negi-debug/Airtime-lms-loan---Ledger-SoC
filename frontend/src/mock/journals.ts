import type { AccountCode, Journal, JournalLine, JournalType, Side } from '../types'

const DAY = '2026-10-17'

function id(seed: number): string {
  const head = (0xa1000000 + seed * 97).toString(16).padStart(8, '0').slice(0, 8)
  const tail = (0x100000000000 + seed * 7919).toString(16).padStart(12, '0').slice(-12)
  return `${head}-4c1e-7000-8000-${tail}`
}

function minor(units: number): string {
  return (units / 10000).toFixed(4)
}

function line(seed: number, accountCode: AccountCode, side: Side, units: number): JournalLine {
  return { lineId: id(seed), accountCode, side, amount: minor(units) }
}

function feeRevenue(n: number, when: string, advance: string, fee: number): Journal {
  return {
    journalId: id(1000 + n),
    sourceEventId: `outbox:fee-${n}`,
    advanceId: advance,
    journalType: 'FEE_REVENUE',
    marketCode: 'DEV',
    currency: 'XXX',
    postedAt: when,
    lines: [
      line(2000 + n, 'FEE_RECEIVABLE', 'DR', fee),
      line(3000 + n, 'FEE_INCOME', 'CR', fee),
    ],
  }
}

function writeOff(n: number, when: string, advance: string, principal: number, fee: number): Journal {
  const lines: JournalLine[] = [line(4000 + n, 'WRITE_OFF_EXPENSE', 'DR', principal + fee)]
  if (principal > 0) lines.push(line(5000 + n, 'PRINCIPAL_RECEIVABLE', 'CR', principal))
  if (fee > 0) lines.push(line(6000 + n, 'FEE_RECEIVABLE', 'CR', fee))
  return {
    journalId: id(7000 + n),
    sourceEventId: `outbox:wo-${n}`,
    advanceId: advance,
    journalType: 'WRITE_OFF',
    marketCode: 'DEV',
    currency: 'XXX',
    postedAt: when,
    lines,
  }
}

function recovery(n: number, when: string, advance: string, amount: number): Journal {
  return {
    journalId: id(8000 + n),
    sourceEventId: `outbox:rec-${n}`,
    advanceId: advance,
    journalType: 'RECOVERY_OF_WRITTEN_OFF',
    marketCode: 'DEV',
    currency: 'XXX',
    postedAt: when,
    lines: [
      line(9000 + n, 'CASH_RECOVERY', 'DR', amount),
      line(10000 + n, 'RECOVERY_OF_WRITE_OFF', 'CR', amount),
    ],
  }
}

const advances = Array.from({ length: 12 }, (_, i) => id(50 + i))

function stamp(dayOffset: number, hour: number, minute: number): string {
  const date = new Date(Date.UTC(2026, 9, 17 - dayOffset, hour, minute, 0))
  return date.toISOString()
}

function build(): Journal[] {
  const rows: Journal[] = []
  const plan: { type: JournalType; day: number; hour: number }[] = []
  for (let i = 0; i < 32; i += 1) plan.push({ type: 'FEE_REVENUE', day: i % 30, hour: 8 + (i % 8) })
  for (let i = 0; i < 12; i += 1) plan.push({ type: 'WRITE_OFF', day: (i * 2) % 30, hour: 18 })
  for (let i = 0; i < 6; i += 1) plan.push({ type: 'RECOVERY_OF_WRITTEN_OFF', day: 3 + i * 4, hour: 11 })

  plan.forEach((item, index) => {
    const advance = advances[index % advances.length]
    const when = stamp(item.day, item.hour, (index * 7) % 60)
    const fee = 25000 + (index % 9) * 12500
    if (item.type === 'FEE_REVENUE') rows.push(feeRevenue(index, when, advance, fee))
    else if (item.type === 'WRITE_OFF') rows.push(writeOff(index, when, advance, 150000 + (index % 5) * 50000, fee))
    else rows.push(recovery(index, when, advance, 40000 + (index % 4) * 15000))
  })
  return rows.sort((a, b) => a.postedAt.localeCompare(b.postedAt))
}

export const journals: Journal[] = build()

export const demoBusinessDate = DAY
