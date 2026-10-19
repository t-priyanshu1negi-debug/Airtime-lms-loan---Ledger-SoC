import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material'
import { useLedger } from '../data/LedgerProvider'
import { useDemo } from '../demo/DemoProvider'

export function PageState({ children }: { children: React.ReactNode }) {
  const ledger = useLedger()
  const { simulateError, simulateEmpty, setSimulateError, setSimulateEmpty } = useDemo()
  if (ledger.status === 'loading') {
    return (
      <Stack alignItems="center" justifyContent="center" sx={{ minHeight: 320 }} spacing={2}>
        <CircularProgress size={28} />
        <Typography color="text.secondary">Loading ledger data…</Typography>
      </Stack>
    )
  }
  if (simulateError) {
    return (
      <Alert
        severity="error"
        action={
          <Button color="inherit" size="small" onClick={() => setSimulateError(false)}>
            Dismiss
          </Button>
        }
      >
        The general ledger could not be loaded. This is a simulated error.
      </Alert>
    )
  }
  if (ledger.status === 'error') {
    return (
      <Alert
        severity="error"
        action={
          <Button color="inherit" size="small" onClick={ledger.reload}>
            Retry
          </Button>
        }
      >
        {ledger.error}
      </Alert>
    )
  }
  if (simulateEmpty) {
    return (
      <Stack spacing={2} sx={{ py: 6, alignItems: 'center' }}>
        <Typography variant="h6">No ledger records</Typography>
        <Typography color="text.secondary">The empty view hides the journals and report rows returned by the ledger.</Typography>
        <Button variant="outlined" onClick={() => setSimulateEmpty(false)}>
          Show ledger data
        </Button>
      </Stack>
    )
  }
  return <Box>{children}</Box>
}
