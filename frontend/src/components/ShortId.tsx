import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { Stack, Tooltip, Typography } from '@mui/material'
import { useDemo } from '../demo/DemoProvider'

export function ShortId({ id }: { id: string }) {
  const { notify } = useDemo()
  return (
    <Tooltip title={id}>
      <Stack
        direction="row"
        spacing={0.5}
        alignItems="center"
        sx={{ cursor: 'pointer', width: 'fit-content' }}
        onClick={(event) => {
          event.stopPropagation()
          void navigator.clipboard.writeText(id).then(() => notify('Copied'))
        }}
      >
        <Typography variant="body2" sx={{ fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' }}>
          {id.slice(0, 8)}
        </Typography>
        <ContentCopyIcon sx={{ fontSize: 14, color: 'text.secondary' }} />
      </Stack>
    </Tooltip>
  )
}
