import { Box, Card, CardContent, Stack, Typography } from '@mui/material'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { useMemo } from 'react'
import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { PageState } from '../components/PageState'
import { ShortId } from '../components/ShortId'
import { TypeBadge } from '../components/TypeBadge'
import { useLedger } from '../data/LedgerProvider'
import { dailySeries, feeRevenueUnits, recoveryUnits, utcDay, writeOffUnits } from '../lib/derive'
import { formatMoney, formatWhen } from '../lib/format'
import type { Journal } from '../types'

function kpi(title: string, value: string) {
  return (
    <Card key={title} variant="outlined" sx={{ flex: 1, minWidth: 180 }}>
      <CardContent>
        <Typography variant="caption" color="text.secondary">
          {title}
        </Typography>
        <Typography variant="h6" sx={{ mt: 0.5, fontVariantNumeric: 'tabular-nums' }}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  )
}

const columns: GridColDef<Journal>[] = [
  { field: 'journalId', headerName: 'Journal ID', width: 140, renderCell: (params) => <ShortId id={params.row.journalId} /> },
  { field: 'postedAt', headerName: 'Posted At', width: 180, valueFormatter: (value: string) => formatWhen(value) },
  { field: 'journalType', headerName: 'Journal Type', width: 230, renderCell: (params) => <TypeBadge type={params.row.journalType} /> },
  { field: 'advanceId', headerName: 'Advance ID', width: 140, renderCell: (params) => <ShortId id={params.row.advanceId} /> },
  { field: 'currency', headerName: 'Currency', width: 100 },
]

export function OverviewPage() {
  const { journals, currency } = useLedger()
  const today = utcDay()
  const totals = useMemo(() => {
    const fee = journals.reduce((sum, journal) => sum + feeRevenueUnits(journal), 0)
    const writeOff = journals.reduce((sum, journal) => sum + writeOffUnits(journal), 0)
    const recovery = journals.reduce((sum, journal) => sum + recoveryUnits(journal), 0)
    const postedToday = journals.filter((journal) => journal.postedAt.slice(0, 10) === today).length
    return { fee, writeOff, recovery, net: fee - writeOff + recovery, today: postedToday }
  }, [journals, today])
  const latest = useMemo(
    () => [...journals].sort((a, b) => b.postedAt.localeCompare(a.postedAt)).slice(0, 10),
    [journals],
  )
  const series = useMemo(() => dailySeries(journals), [journals])

  return (
    <PageState>
      <Stack spacing={2.5}>
        <Typography variant="h5">Overview</Typography>
        <Stack direction="row" spacing={2} useFlexGap flexWrap="wrap">
          {kpi('Total Fee Revenue', formatMoney(totals.fee / 10000, currency))}
          {kpi('Total Written Off', formatMoney(totals.writeOff / 10000, currency))}
          {kpi('Recovery of Written Off', formatMoney(totals.recovery / 10000, currency))}
          {kpi('Net Fee Income', formatMoney(totals.net / 10000, currency))}
          {kpi('Journals today', String(totals.today))}
        </Stack>
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
              Fee revenue, write-off and recovery — last 30 days
            </Typography>
            <Box sx={{ width: '100%', height: 320 }}>
              <ResponsiveContainer>
                <LineChart data={series}>
                  <CartesianGrid stroke="#E4E7EC" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} minTickGap={24} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Legend />
                  <Line type="monotone" dataKey="feeRevenue" name="Fee revenue" stroke="#1F7A4D" dot={false} strokeWidth={2} />
                  <Line type="monotone" dataKey="writeOff" name="Write-off" stroke="#B45309" dot={false} strokeWidth={2} />
                  <Line type="monotone" dataKey="recovery" name="Recovery" stroke="#175CD3" dot={false} strokeWidth={2} />
                </LineChart>
              </ResponsiveContainer>
            </Box>
          </CardContent>
        </Card>
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle1" sx={{ mb: 1 }}>
              Latest 10 journals
            </Typography>
            <DataGrid
              autoHeight
              rows={latest}
              columns={columns}
              getRowId={(row) => row.journalId}
              hideFooter
              disableColumnFilter
              disableRowSelectionOnClick
            />
          </CardContent>
        </Card>
      </Stack>
    </PageState>
  )
}
