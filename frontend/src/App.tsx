import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { LedgerProvider } from './data/LedgerProvider'
import { DemoProvider } from './demo/DemoProvider'
import { AccountLedgerPage } from './pages/AccountLedgerPage'
import { BilledPage } from './pages/BilledPage'
import { JournalsPage } from './pages/JournalsPage'
import { OverviewPage } from './pages/OverviewPage'
import { TrialBalancePage } from './pages/TrialBalancePage'

const theme = createTheme({
  palette: {
    primary: { main: '#0B3A5B' },
    success: { main: '#1F7A4D' },
    background: { default: '#F6F7F9' },
  },
  typography: {
    fontFamily: '"Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
  },
  shape: { borderRadius: 8 },
})

export function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <DemoProvider>
        <LedgerProvider>
          <BrowserRouter>
            <Routes>
              <Route element={<AppShell />}>
                <Route index element={<OverviewPage />} />
                <Route path="journals" element={<JournalsPage />} />
                <Route path="trial-balance" element={<TrialBalancePage />} />
                <Route path="account-ledger" element={<AccountLedgerPage />} />
                <Route path="billed-vs-collected" element={<BilledPage />} />
              </Route>
            </Routes>
          </BrowserRouter>
        </LedgerProvider>
      </DemoProvider>
    </ThemeProvider>
  )
}
