import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined'
import { Button, Card, CardContent, Stack, TextField, Typography } from '@mui/material'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { useMemo, useState } from 'react'
import { PageState } from '../components/PageState'
import { useLedger } from '../data/LedgerProvider'
import { spanOf, trialBalance, type TrialRow } from '../lib/derive'
import { downloadCsv, formatMoney, units } from '../lib/format'

export function TrialBalancePage() {
  const { journals, currency } = useLedger()
  const columns = useMemo<GridColDef<TrialRow>[]>(
    () => [
      { field: 'accountCode', headerName: 'Account Code', flex: 1.2, minWidth: 200 },
      { field: 'accountName', headerName: 'Account Name', flex: 1, minWidth: 180 },
      { field: 'debit', headerName: 'Debit', flex: 0.8, minWidth: 140, align: 'right', headerAlign: 'right', valueFormatter: (value: string) => formatMoney(value, currency) },
      { field: 'credit', headerName: 'Credit', flex: 0.8, minWidth: 140, align: 'right', headerAlign: 'right', valueFormatter: (value: string) => formatMoney(value, currency) },
      { field: 'net', headerName: 'Net Balance', flex: 0.8, minWidth: 150, align: 'right', headerAlign: 'right', valueFormatter: (value: string) => formatMoney(value, currency) },
    ],
    [currency],
  )
  const latest = spanOf(journals).to
  const [asOf, setAsOf] = useState<string | null>(null)
  const asOfValue = asOf ?? latest
  const rows = useMemo(() => trialBalance(journals, asOfValue), [journals, asOfValue])
  const debit = rows.reduce((sum, row) => sum + units(row.debit), 0)
  const credit = rows.reduce((sum, row) => sum + units(row.credit), 0)
  const balanced = debit === credit

  return (
    <PageState>
      <Stack spacing={2}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h5">Trial Balance</Typography>
          <Button
            variant="outlined"
            startIcon={<FileDownloadOutlinedIcon />}
            onClick={() =>
              downloadCsv(
                'trial-balance.csv',
                ['Account Code', 'Account Name', 'Debit', 'Credit', 'Net Balance'],
                rows.map((row) => [row.accountCode, row.accountName, row.debit, row.credit, row.net]),
              )
            }
          >
            Export CSV
          </Button>
        </Stack>
        <TextField size="small" type="date" label="As of" value={asOfValue} onChange={(e) => setAsOf(e.target.value)} InputLabelProps={{ shrink: true }} sx={{ width: 200 }} />
        <Card variant="outlined">
          <CardContent>
            <DataGrid autoHeight rows={rows} columns={columns} getRowId={(row) => row.accountCode} hideFooter disableColumnFilter disableRowSelectionOnClick />
            <Stack direction="row" justifyContent="flex-end" spacing={3} sx={{ pt: 2 }} alignItems="center">
              <Typography variant="body2">Total Debit {formatMoney(debit / 10000, currency)}</Typography>
              <Typography variant="body2">Total Credit {formatMoney(credit / 10000, currency)}</Typography>
              {balanced && (
                <Stack direction="row" spacing={0.5} alignItems="center" color="success.main">
                  <CheckCircleIcon fontSize="small" />
                  <Typography variant="body2">Balanced</Typography>
                </Stack>
              )}
            </Stack>
          </CardContent>
        </Card>
      </Stack>
    </PageState>
  )
}
