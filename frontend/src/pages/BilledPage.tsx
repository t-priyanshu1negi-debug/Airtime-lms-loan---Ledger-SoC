import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined'
import ReportProblemIcon from '@mui/icons-material/ReportProblem'
import { Box, Button, Card, CardContent, Stack, Typography } from '@mui/material'
import { DataGrid, type GridColDef } from '@mui/x-data-grid'
import { useMemo } from 'react'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { PageState } from '../components/PageState'
import { useLedger } from '../data/LedgerProvider'
import { billedGap } from '../lib/derive'
import { downloadCsv, formatMoney, units } from '../lib/format'

interface Row {
  id: string
  rail: string
  businessDate: string
  billed: string
  collected: string
  gap: string
  stopped: boolean
}

export function BilledPage() {
  const { billed, currency } = useLedger()
  const columns = useMemo<GridColDef<Row>[]>(
    () => [
      { field: 'rail', headerName: 'Rail', width: 180 },
      { field: 'businessDate', headerName: 'Date', width: 140 },
      { field: 'billed', headerName: 'Billed', width: 160, align: 'right', headerAlign: 'right', valueFormatter: (value: string) => formatMoney(value, currency) },
      { field: 'collected', headerName: 'Collected', width: 160, align: 'right', headerAlign: 'right', valueFormatter: (value: string) => formatMoney(value, currency) },
      { field: 'gap', headerName: 'Gap', width: 160, align: 'right', headerAlign: 'right', valueFormatter: (value: string) => formatMoney(value, currency) },
      {
        field: 'stopped',
        headerName: 'Status',
        flex: 1,
        minWidth: 220,
        renderCell: (params) =>
          params.row.stopped ? (
            <Stack direction="row" spacing={0.5} alignItems="center" color="error.main">
              <ReportProblemIcon fontSize="small" />
              <span>Sweep may have stopped</span>
            </Stack>
          ) : (
            'Collected'
          ),
      },
    ],
    [currency],
  )
  const rows = useMemo<Row[]>(
    () =>
      billed.map((row) => {
        const gap = billedGap(row.billed, row.collected)
        return { id: `${row.rail}-${row.businessDate}`, ...row, gap, stopped: units(row.collected) < units(row.billed) }
      }),
    [billed],
  )
  const series = rows.map((row) => ({
    date: row.businessDate.slice(5),
    billed: Number(row.billed),
    collected: Number(row.collected),
  }))

  return (
    <PageState>
      <Stack spacing={2}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="h5">Billed vs Collected</Typography>
          <Button
            variant="outlined"
            startIcon={<FileDownloadOutlinedIcon />}
            onClick={() =>
              downloadCsv(
                'billed-vs-collected.csv',
                ['Rail', 'Date', 'Billed', 'Collected', 'Gap', 'Status'],
                rows.map((row) => [row.rail, row.businessDate, row.billed, row.collected, row.gap, row.stopped ? 'Sweep may have stopped' : 'Collected']),
              )
            }
          >
            Export CSV
          </Button>
        </Stack>
        <Card variant="outlined">
          <CardContent>
            <Typography variant="subtitle1" sx={{ mb: 2 }}>
              Billed and collected amounts from the ledger
            </Typography>
            <Box sx={{ width: '100%', height: 280 }}>
              <ResponsiveContainer>
                <BarChart data={series}>
                  <CartesianGrid stroke="#E4E7EC" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} minTickGap={16} />
                  <YAxis tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="billed" name="Billed" fill="#0B3A5B" />
                  <Bar dataKey="collected" name="Collected" fill="#1F7A4D" />
                </BarChart>
              </ResponsiveContainer>
            </Box>
          </CardContent>
        </Card>
        <Card variant="outlined">
          <CardContent>
            <DataGrid
              autoHeight
              rows={rows}
              columns={columns}
              pageSizeOptions={[10, 30]}
              initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
              disableColumnFilter
              disableRowSelectionOnClick
              getRowClassName={(params) => (params.row.stopped ? 'sweep-gap' : '')}
              sx={{
                '& .sweep-gap': { bgcolor: '#FEF3F2' },
              }}
            />
          </CardContent>
        </Card>
      </Stack>
    </PageState>
  )
}
