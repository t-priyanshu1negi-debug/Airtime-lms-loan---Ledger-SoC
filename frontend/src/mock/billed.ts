import type { BilledRow } from '../types'

function money(units: number): string {
  return (units / 10000).toFixed(4)
}

/** 30 business dates ending 2026-10-17. Days 8, 19 and 25 have collected below billed. */
export const billedVsCollected: BilledRow[] = Array.from({ length: 30 }, (_, index) => {
  const date = new Date(Date.UTC(2026, 9, 17 - (29 - index)))
  const businessDate = date.toISOString().slice(0, 10)
  const billed = 1800000 + (index % 7) * 125000
  const short = index === 7 || index === 18 || index === 24
  const collected = short ? billed - 250000 - index * 1000 : billed
  return {
    rail: 'RECHARGE_SWEEP',
    businessDate,
    billed: money(billed),
    collected: money(collected),
  }
})
