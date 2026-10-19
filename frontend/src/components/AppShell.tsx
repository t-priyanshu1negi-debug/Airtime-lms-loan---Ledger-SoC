import MenuIcon from '@mui/icons-material/Menu'
import {
  AppBar,
  Box,
  Chip,
  Divider,
  Drawer,
  FormControlLabel,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  MenuItem,
  Select,
  Snackbar,
  Stack,
  Switch,
  Toolbar,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useLedger } from '../data/LedgerProvider'
import { useDemo } from '../demo/DemoProvider'
import { formatWhen } from '../lib/format'

const drawerWidth = 248

const links = [
  { to: '/', label: 'Overview' },
  { to: '/journals', label: 'Journals' },
  { to: '/trial-balance', label: 'Trial Balance' },
  { to: '/account-ledger', label: 'Account Ledger' },
  { to: '/billed-vs-collected', label: 'Billed vs Collected' },
]

function SideNav({ onNavigate, connected }: { onNavigate?: () => void; connected: boolean }) {
  return (
    <Box sx={{ px: 2, py: 2.5 }}>
      <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#0B3A5B' }}>
        LMS BO Portal
      </Typography>
      <Chip label={connected ? 'Ledger service' : 'Ledger unavailable'} size="small" color={connected ? 'success' : 'default'} sx={{ mt: 1 }} />
      <Typography variant="overline" color="text.secondary" sx={{ display: 'block', mt: 3 }}>
        General Ledger
      </Typography>
      <List disablePadding>
        {links.map((link) => (
          <ListItemButton
            key={link.to}
            component={NavLink}
            to={link.to}
            end={link.to === '/'}
            onClick={onNavigate}
            sx={{
              borderRadius: 1,
              mb: 0.5,
              '&.active': { bgcolor: '#E7F0F6', color: '#0B3A5B', fontWeight: 600 },
            }}
          >
            <ListItemText primary={link.label} primaryTypographyProps={{ variant: 'body2' }} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  )
}

export function AppShell() {
  const [open, setOpen] = useState(false)
  const demo = useDemo()
  const ledger = useLedger()
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: '#F6F7F9' }}>
      <AppBar
        position="fixed"
        elevation={0}
        color="inherit"
        sx={{ borderBottom: '1px solid #E4E7EC', zIndex: (theme) => theme.zIndex.drawer + 1 }}
      >
        <Toolbar sx={{ gap: 2 }}>
          <IconButton edge="start" onClick={() => setOpen(true)} sx={{ display: { md: 'none' } }}>
            <MenuIcon />
          </IconButton>
          <Typography variant="subtitle1" sx={{ fontWeight: 700, display: { xs: 'none', sm: 'block' } }}>
            General Ledger
          </Typography>
          <Box sx={{ flexGrow: 1 }} />
          <FormControlLabel
            control={<Switch size="small" checked={demo.simulateError} onChange={(_, checked) => demo.setSimulateError(checked)} />}
            label={<Typography variant="caption">Simulate error</Typography>}
          />
          <FormControlLabel
            control={<Switch size="small" checked={demo.simulateEmpty} onChange={(_, checked) => demo.setSimulateEmpty(checked)} />}
            label={<Typography variant="caption">Simulate empty</Typography>}
          />
          <Select size="small" value={ledger.market} onChange={(event) => ledger.setMarket(event.target.value)} sx={{ minWidth: 120 }}>
            <MenuItem value="ALL">All markets</MenuItem>
            {ledger.markets.map((code) => (
              <MenuItem key={code} value={code}>
                {code}
              </MenuItem>
            ))}
          </Select>
          <Chip
            size="small"
            variant="outlined"
            label={ledger.loadedAt ? `Data as of ${formatWhen(ledger.loadedAt)} UTC` : 'Waiting for ledger'}
          />
          <Stack sx={{ pl: 1 }}>
            <Typography variant="body2" sx={{ fontWeight: 600, lineHeight: 1.2 }}>
              Finance User
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Finance
            </Typography>
          </Stack>
        </Toolbar>
      </AppBar>
      <Drawer
        variant="temporary"
        open={open}
        onClose={() => setOpen(false)}
        sx={{ display: { md: 'none' }, '& .MuiDrawer-paper': { width: drawerWidth } }}
      >
        <SideNav connected={ledger.status === 'ready'} onNavigate={() => setOpen(false)} />
      </Drawer>
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', md: 'block' },
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box', borderRight: '1px solid #E4E7EC' },
        }}
        open
      >
        <Toolbar />
        <Divider />
        <SideNav connected={ledger.status === 'ready'} />
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 3 }, width: { md: `calc(100% - ${drawerWidth}px)` } }}>
        <Toolbar />
        <Outlet />
      </Box>
      <Snackbar
        open={Boolean(demo.notice)}
        autoHideDuration={1600}
        onClose={demo.clearNotice}
        message={demo.notice}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />
    </Box>
  )
}
