import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined'
import { Button, Card, CardContent, MenuItem, Stack, TextField, Typography } from '@mui/material'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { useMemo, useState } from 'react'
import { PageState } from '../components/PageState'
import { ShortId } from '../components/ShortId'
import { TypeBadge } from '../components/TypeBadge'
import { useLedger } from '../data/LedgerProvider'
import { accountEntries, type LedgerEntry } from '../lib/derive'
import { downloadCsv, formatMoney, formatWhen } from '../lib/format'
import { ACCOUNTS } from '../types'

export function AccountLedgerPage() {
  const { journals, currency } = useLedger()
  const accounts = useMemo(() => {
    const extra = new Set<string>()
    journals.forEach((journal) => {
      journal.lines.forEach((line) => {
        if (!ACCOUNTS.some((account) => account.code === line.accountCode)) extra.add(line.accountCode)
      })
    })
    return [...ACCOUNTS.map((account) => ({ code: account.code, name: account.name, normal: account.normal })), ...[...extra].sort().map((code) => ({ code, name: code, normal: 'DR' as const }))]
  }, [journals])
  const columns = useMemo<GridColDef<LedgerEntry>[]>(
    () => [
      { field: 'postedAt', headerName: 'Date', width: 180, valueFormatter: (value: string) => formatWhen(value) },
      { field: 'journalId', headerName: 'Journal ID', width: 140, renderCell: (p) => <ShortId id={p.row.journalId} /> },
      { field: 'journalType', headerName: 'Journal Type', width: 230, renderCell: (p) => <TypeBadge type={p.row.journalType} /> },
      { field: 'advanceId', headerName: 'Advance ID', width: 140, renderCell: (p) => <ShortId id={p.row.advanceId} /> },
      { field: 'debit', headerName: 'Debit', width: 150, align: 'right', headerAlign: 'right', valueFormatter: (value: string) => formatMoney(value, currency) },
      { field: 'credit', headerName: 'Credit', width: 150, align: 'right', headerAlign: 'right', valueFormatter: (value: string) => formatMoney(value, currency) },
      { field: 'running', headerName: 'Running Balance', width: 170, align: 'right', headerAlign: 'right', valueFormatter: (value: string) => formatMoney(value, currency) },
    ],
    [currency],
  )
  const [account, setAccount] = useState('FEE_INCOME')
  const rows = useMemo(() => accountEntries(journals, account), [journals, account])
  const meta = accounts.find((item) => item.code === account)

  return (
    <PageState>
      <Stack spacing={2}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h5">Account Ledger</Typography>
          <Button
            variant="outlined"
            startIcon={<FileDownloadOutlinedIcon />}
            onClick={() =>
              downloadCsv(
                'account-ledger.csv',
                ['Date', 'Journal ID', 'Journal Type', 'Advance ID', 'Debit', 'Credit', 'Running Balance'],
                rows.map((row) => [row.postedAt, row.journalId, row.journalType, row.advanceId, row.debit, row.credit, row.running]),
              )
            }
          >
            Export CSV
          </Button>
        </Stack>
        <TextField select size="small" label="Account" value={account} onChange={(e) => setAccount(e.target.value)} sx={{ maxWidth: 420 }}>
          {accounts.map((item) => (
            <MenuItem key={item.code} value={item.code}>
              {item.code} — {item.name}
            </MenuItem>
          ))}
        </TextField>
        <Typography variant="body2" color="text.secondary">
          {meta?.name}. Running balance follows the {meta?.normal === 'DR' ? 'debit' : 'credit'} normal balance. Amounts are 4 decimal places.
        </Typography>
        <Card variant="outlined">
          <CardContent>
            {rows.length === 0 ? (
              <Typography color="text.secondary" sx={{ py: 6, textAlign: 'center' }}>
                No entries for this account.
              </Typography>
            ) : (
              <DataGrid
                autoHeight
                rows={rows}
                columns={columns}
                getRowId={(row) => row.id}
                pageSizeOptions={[10, 25, 50]}
                initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
                disableColumnFilter
                disableRowSelectionOnClick
              />
            )}
          </CardContent>
        </Card>
      </Stack>
    </PageState>
  )
}
