import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined'
import {
  Button,
  Card,
  CardContent,
  Drawer,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { useMemo, useState } from 'react'
import { PageState } from '../components/PageState'
import { ShortId } from '../components/ShortId'
import { TypeBadge } from '../components/TypeBadge'
import { useLedger } from '../data/LedgerProvider'
import { spanOf } from '../lib/derive'
import { downloadCsv, formatMoney, formatWhen, isBalanced, sumSide } from '../lib/format'
import type { Journal, JournalType } from '../types'

const columns: GridColDef<Journal>[] = [
  { field: 'journalId', headerName: 'Journal ID', width: 130, renderCell: (p) => <ShortId id={p.row.journalId} /> },
  { field: 'postedAt', headerName: 'Posted At', width: 170, valueFormatter: (value: string) => formatWhen(value) },
  { field: 'journalType', headerName: 'Journal Type', width: 230, renderCell: (p) => <TypeBadge type={p.row.journalType} /> },
  { field: 'advanceId', headerName: 'Advance ID', width: 130, renderCell: (p) => <ShortId id={p.row.advanceId} /> },
  { field: 'marketCode', headerName: 'Market', width: 90 },
  { field: 'currency', headerName: 'Currency', width: 100 },
  {
    field: 'debit',
    headerName: 'Total Debit',
    width: 150,
    align: 'right',
    headerAlign: 'right',
    valueGetter: (_value, row) => sumSide(row, 'DR') / 10000,
    renderCell: (p) => formatMoney(sumSide(p.row, 'DR') / 10000, p.row.currency),
  },
  {
    field: 'credit',
    headerName: 'Total Credit',
    width: 150,
    align: 'right',
    headerAlign: 'right',
    valueGetter: (_value, row) => sumSide(row, 'CR') / 10000,
    renderCell: (p) => formatMoney(sumSide(p.row, 'CR') / 10000, p.row.currency),
  },
  {
    field: 'status',
    headerName: 'Status',
    width: 130,
    valueGetter: (_value, row) => (isBalanced(row) ? 'Balanced' : 'Unbalanced'),
  },
]

export function JournalsPage() {
  const { journals } = useLedger()
  const span = useMemo(() => spanOf(journals), [journals])
  const [from, setFrom] = useState<string | null>(null)
  const [to, setTo] = useState<string | null>(null)
  const fromValue = from ?? span.from
  const toValue = to ?? span.to
  const [type, setType] = useState<'ALL' | JournalType>('ALL')
  const [advance, setAdvance] = useState('')
  const [selected, setSelected] = useState<Journal | null>(null)

  const rows = useMemo(() => {
    const query = advance.trim().toLowerCase()
    return journals.filter((journal) => {
      const day = journal.postedAt.slice(0, 10)
      if (day < fromValue || day > toValue) return false
      if (type !== 'ALL' && journal.journalType !== type) return false
      if (query && !journal.advanceId.toLowerCase().includes(query)) return false
      return true
    })
  }, [journals, fromValue, toValue, type, advance])

  return (
    <PageState>
      <Stack spacing={2}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h5">Journals</Typography>
          <Button
            variant="outlined"
            startIcon={<FileDownloadOutlinedIcon />}
            onClick={() =>
              downloadCsv(
                'journals.csv',
                ['Journal ID', 'Posted At', 'Journal Type', 'Advance ID', 'Market', 'Currency', 'Total Debit', 'Total Credit', 'Status'],
                rows.map((row) => [
                  row.journalId,
                  row.postedAt,
                  row.journalType,
                  row.advanceId,
                  row.marketCode,
                  row.currency,
                  (sumSide(row, 'DR') / 10000).toFixed(4),
                  (sumSide(row, 'CR') / 10000).toFixed(4),
                  isBalanced(row) ? 'Balanced' : 'Unbalanced',
                ]),
              )
            }
          >
            Export CSV
          </Button>
        </Stack>
        <Stack direction="row" spacing={2} useFlexGap flexWrap="wrap">
          <TextField size="small" type="date" label="From" value={fromValue} onChange={(e) => setFrom(e.target.value)} InputLabelProps={{ shrink: true }} />
          <TextField size="small" type="date" label="To" value={toValue} onChange={(e) => setTo(e.target.value)} InputLabelProps={{ shrink: true }} />
          <TextField size="small" select label="Journal type" value={type} onChange={(e) => setType(e.target.value as 'ALL' | JournalType)} sx={{ minWidth: 240 }}>
            <MenuItem value="ALL">All types</MenuItem>
            <MenuItem value="FEE_REVENUE">FEE_REVENUE</MenuItem>
            <MenuItem value="WRITE_OFF">WRITE_OFF</MenuItem>
            <MenuItem value="RECOVERY_OF_WRITTEN_OFF">RECOVERY_OF_WRITTEN_OFF</MenuItem>
          </TextField>
          <TextField size="small" label="Advance ID" value={advance} onChange={(e) => setAdvance(e.target.value)} />
        </Stack>
        <Card variant="outlined">
          <CardContent>
            {rows.length === 0 ? (
              <Typography color="text.secondary" sx={{ py: 6, textAlign: 'center' }}>
                No journals match these filters.
              </Typography>
            ) : (
              <DataGrid
                autoHeight
                rows={rows}
                columns={columns}
                getRowId={(row) => row.journalId}
                pageSizeOptions={[10, 25, 50]}
                initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
                disableColumnFilter
                disableRowSelectionOnClick
                onRowClick={(params) => setSelected(params.row)}
              />
            )}
          </CardContent>
        </Card>
      </Stack>
      <Drawer anchor="right" open={Boolean(selected)} onClose={() => setSelected(null)} PaperProps={{ sx: { width: { xs: '100%', sm: 480 } } }}>
        {selected && (
          <Stack spacing={2} sx={{ p: 3 }}>
            <Typography variant="h6">Journal</Typography>
            <ShortId id={selected.journalId} />
            <Typography variant="body2">Posted {formatWhen(selected.postedAt)} UTC</Typography>
            <TypeBadge type={selected.journalType} />
            <Typography variant="body2">Advance</Typography>
            <ShortId id={selected.advanceId} />
            <Typography variant="body2">
              {selected.marketCode} · {selected.currency} · {isBalanced(selected) ? 'Balanced' : 'Unbalanced'}
            </Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Account Code</TableCell>
                  <TableCell>Side</TableCell>
                  <TableCell align="right">Amount</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {selected.lines.map((line) => (
                  <TableRow key={line.lineId}>
                    <TableCell>{line.accountCode}</TableCell>
                    <TableCell>{line.side}</TableCell>
                    <TableCell align="right">{formatMoney(line.amount, selected.currency)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Typography variant="body2" color={isBalanced(selected) ? 'success.main' : 'error.main'}>
              Debits {(sumSide(selected, 'DR') / 10000).toFixed(4)} = Credits {(sumSide(selected, 'CR') / 10000).toFixed(4)}
            </Typography>
          </Stack>
        )}
      </Drawer>
    </PageState>
  )
}
