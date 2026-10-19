import type { BilledRow, Journal, JournalLine } from '../types'

const QUERY = `
  query Portal {
    reportingData {
      billedVsCollected {
        rail
        businessDate
        billed
        collected
      }
    }
    accountLedgerRecords {
      loanContextStatus
      records {
        journalId
        sourceEventId
        advanceId
        journalType
        marketCode
        currency
        postedAt
        lines {
          lineId
          accountCode
          side
          amount
        }
      }
    }
  }
`

interface GraphPayload {
  data?: {
    reportingData?: {
      billedVsCollected?: BilledWire[]
    }
    accountLedgerRecords?: {
      loanContextStatus?: string
      records?: JournalWire[]
    }
  }
  errors?: { message?: string }[]
}

interface BilledWire {
  rail: string
  businessDate: string
  billed: string
  collected: string
}

interface JournalWire {
  journalId: string
  sourceEventId: string
  advanceId: string
  journalType: string
  marketCode: string
  currency: string
  postedAt: string
  lines: { lineId: string; accountCode: string; side: string; amount: string }[]
}

export interface LedgerSnapshot {
  journals: Journal[]
  billed: BilledRow[]
  loanContextStatus: string
}

export async function loadLedger(): Promise<LedgerSnapshot> {
  let response: Response
  try {
    response = await fetch('/graphql', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ query: QUERY }),
    })
  } catch {
    throw new Error(
      'The ledger service did not respond. Start ledger_mgmt and set LEDGER_URL if it is not on http://127.0.0.1:8082.',
    )
  }
  if (!response.ok) {
    if (response.status === 500 || response.status === 502 || response.status === 504) {
      throw new Error(
        'The ledger service did not respond. Start ledger_mgmt and set LEDGER_URL if it is not on http://127.0.0.1:8082.',
      )
    }
    throw new Error(`The ledger service returned HTTP ${response.status}.`)
  }
  let payload: GraphPayload
  try {
    payload = (await response.json()) as GraphPayload
  } catch {
    throw new Error('The ledger service returned a response that is not GraphQL JSON.')
  }
  if (payload.errors?.length) {
    const message = payload.errors.map((error) => error.message).filter(Boolean).join('; ')
    throw new Error(message || 'The ledger GraphQL query failed.')
  }
  return mapLedgerPayload(payload)
}

export function mapLedgerPayload(payload: GraphPayload): LedgerSnapshot {
  const records = payload.data?.accountLedgerRecords?.records ?? []
  const billed = payload.data?.reportingData?.billedVsCollected ?? []
  return {
    journals: records.map(toJournal),
    billed: billed.map((row) => ({
      rail: row.rail,
      businessDate: row.businessDate,
      billed: row.billed,
      collected: row.collected,
    })),
    loanContextStatus: payload.data?.accountLedgerRecords?.loanContextStatus ?? 'NOT_REQUESTED',
  }
}

function toJournal(row: JournalWire): Journal {
  const lines: JournalLine[] = (row.lines ?? []).map((line) => ({
    lineId: line.lineId,
    accountCode: line.accountCode,
    side: line.side.trim().toUpperCase() === 'DR' ? 'DR' : 'CR',
    amount: line.amount,
  }))
  return {
    journalId: row.journalId,
    sourceEventId: row.sourceEventId,
    advanceId: row.advanceId,
    journalType: row.journalType,
    marketCode: row.marketCode,
    currency: row.currency,
    postedAt: asUtc(row.postedAt),
    lines,
  }
}

function asUtc(value: string): string {
  if (!value) return value
  if (/[zZ]$/.test(value) || /[+-]\d{2}:\d{2}$/.test(value)) return value
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return `${value}T00:00:00Z`
  return `${value}Z`
}
