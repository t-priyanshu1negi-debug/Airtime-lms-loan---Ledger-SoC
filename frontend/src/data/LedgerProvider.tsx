import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { loadLedger } from '../api/ledger'
import type { BilledRow, Journal } from '../types'

interface LedgerData {
  status: 'loading' | 'ready' | 'error'
  error: string | null
  journals: Journal[]
  billed: BilledRow[]
  loadedAt: string | null
  loanContextStatus: string | null
  market: string
  setMarket: (market: string) => void
  markets: string[]
  currency: string
  reload: () => void
}

const LedgerContext = createContext<LedgerData | null>(null)

export function LedgerProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<LedgerData['status']>('loading')
  const [error, setError] = useState<string | null>(null)
  const [journals, setJournals] = useState<Journal[]>([])
  const [billed, setBilled] = useState<BilledRow[]>([])
  const [loadedAt, setLoadedAt] = useState<string | null>(null)
  const [loanContextStatus, setLoanContextStatus] = useState<string | null>(null)
  const [market, setMarket] = useState('ALL')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    setStatus('loading')
    setError(null)
    loadLedger()
      .then((snapshot) => {
        if (cancelled) return
        setJournals(snapshot.journals)
        setBilled(snapshot.billed)
        setLoanContextStatus(snapshot.loanContextStatus)
        setLoadedAt(new Date().toISOString())
        setStatus('ready')
      })
      .catch((reason: unknown) => {
        if (cancelled) return
        setJournals([])
        setBilled([])
        setError(reason instanceof Error ? reason.message : 'The ledger service did not respond.')
        setStatus('error')
      })
    return () => {
      cancelled = true
    }
  }, [attempt])

  const markets = useMemo(() => [...new Set(journals.map((journal) => journal.marketCode))].sort(), [journals])

  useEffect(() => {
    if (market !== 'ALL' && !markets.includes(market)) setMarket('ALL')
  }, [market, markets])

  const visible = useMemo(
    () => (market === 'ALL' ? journals : journals.filter((journal) => journal.marketCode === market)),
    [journals, market],
  )
  const currency = useMemo(() => {
    const codes = [...new Set(visible.map((journal) => journal.currency))]
    return codes.length === 1 ? codes[0] : 'XXX'
  }, [visible])

  const value = useMemo<LedgerData>(
    () => ({
      status,
      error,
      journals: visible,
      billed,
      loadedAt,
      loanContextStatus,
      market,
      setMarket,
      markets,
      currency,
      reload: () => setAttempt((current) => current + 1),
    }),
    [status, error, visible, billed, loadedAt, loanContextStatus, market, markets, currency],
  )

  return <LedgerContext.Provider value={value}>{children}</LedgerContext.Provider>
}

export function useLedger(): LedgerData {
  const value = useContext(LedgerContext)
  if (!value) throw new Error('useLedger must be used inside LedgerProvider')
  return value
}
