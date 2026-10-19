import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

interface DemoControls {
  simulateError: boolean
  simulateEmpty: boolean
  setSimulateError: (value: boolean) => void
  setSimulateEmpty: (value: boolean) => void
  notice: string | null
  notify: (message: string) => void
  clearNotice: () => void
}

const DemoContext = createContext<DemoControls | null>(null)

export function DemoProvider({ children }: { children: ReactNode }) {
  const [simulateError, setSimulateError] = useState(false)
  const [simulateEmpty, setSimulateEmpty] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const value = useMemo(
    () => ({
      simulateError,
      simulateEmpty,
      setSimulateError,
      setSimulateEmpty,
      notice,
      notify: setNotice,
      clearNotice: () => setNotice(null),
    }),
    [simulateError, simulateEmpty, notice],
  )
  return <DemoContext.Provider value={value}>{children}</DemoContext.Provider>
}

export function useDemo(): DemoControls {
  const value = useContext(DemoContext)
  if (!value) throw new Error('useDemo must be used inside DemoProvider')
  return value
}
