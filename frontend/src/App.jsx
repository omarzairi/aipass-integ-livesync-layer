import { useState, useEffect, useCallback } from 'react'
import { fetchHealth, fetchEvents, fetchLogs, replayEvent } from './api'

function StatusDot({ status }) {
  return <span className={`status-dot ${status === 'UP' ? 'up' : 'down'}`} />
}

function Badge({ value }) {
  if (!value) return null
  return <span className={`badge ${value.toLowerCase()}`}>{value}</span>
}

function MetricCard({ label, value, color }) {
  return (
    <div className="card">
      <h3>{label}</h3>
      <div className="value" style={color ? { color } : undefined}>{value}</div>
    </div>
  )
}

function formatTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleString()
}

function formatDuration(seconds) {
  if (seconds < 60) return `${seconds}s`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ${seconds % 60}s`
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  return `${h}h ${m}m`
}

export default function App() {
  const [tab, setTab] = useState('events')
  const [health, setHealth] = useState(null)
  const [events, setEvents] = useState(null)
  const [logs, setLogs] = useState(null)
  const [selectedEvent, setSelectedEvent] = useState(null)
  const [error, setError] = useState(null)
  const [lastRefresh, setLastRefresh] = useState(new Date())

  const refresh = useCallback(async () => {
    try {
      setError(null)
      const [h, e, l] = await Promise.all([
        fetchHealth(),
        fetchEvents(0, 25),
        fetchLogs(null, 0, 50)
      ])
      setHealth(h)
      setEvents(e)
      setLogs(l)
      setLastRefresh(new Date())
    } catch (err) {
      setError(err.message)
    }
  }, [])

  useEffect(() => {
    refresh()
    const interval = setInterval(refresh, 5000)
    return () => clearInterval(interval)
  }, [refresh])

  async function handleReplay(id) {
    try {
      await replayEvent(id)
      setTimeout(refresh, 500)
    } catch (err) {
      alert('Replay failed: ' + err.message)
    }
  }

  return (
    <div className="container">
      <header>
        <h1>AI-Pass LiveSync Dashboard</h1>
        <div className="auto-refresh">
          {health && <><StatusDot status={health.status} /> {health.status}</>}
          {' · '}
          Auto-refresh 5s · Last: {lastRefresh.toLocaleTimeString()}
        </div>
      </header>

      {error && (
        <div className="card" style={{ borderColor: 'var(--red)', marginBottom: 16 }}>
          <p style={{ color: 'var(--red)' }}>Connection error: {error}</p>
        </div>
      )}

      {health && (
        <div className="grid">
          <MetricCard label="Uptime" value={formatDuration(health.uptimeSeconds)} />
          <MetricCard label="Queue Depth" value={health.queue?.depth ?? '—'} color={health.queue?.depth > 0 ? 'var(--yellow)' : 'var(--green)'} />
          <MetricCard label="Events Received" value={Math.round(health.metrics?.events_received ?? 0)} />
          <MetricCard label="Processed" value={Math.round(health.metrics?.events_processed ?? 0)} color="var(--green)" />
          <MetricCard label="Failed" value={Math.round(health.metrics?.events_failed ?? 0)} color={health.metrics?.events_failed > 0 ? 'var(--red)' : undefined} />
          <MetricCard label="Redis" value={health.queue?.redis_available ? 'Connected' : 'Fallback'} color={health.queue?.redis_available ? 'var(--green)' : 'var(--yellow)'} />
        </div>
      )}

      <div className="tabs">
        <button className={`tab ${tab === 'events' ? 'active' : ''}`} onClick={() => setTab('events')}>Events</button>
        <button className={`tab ${tab === 'logs' ? 'active' : ''}`} onClick={() => setTab('logs')}>Logs</button>
        {selectedEvent && (
          <button className={`tab ${tab === 'detail' ? 'active' : ''}`} onClick={() => setTab('detail')}>Event Detail</button>
        )}
      </div>

      {tab === 'events' && (
        <div className="card">
          <h2 className="section-title">Recent Events</h2>
          {events?.content?.length > 0 ? (
            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Type</th>
                  <th>Source</th>
                  <th>Status</th>
                  <th>Decision</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {events.content.map(ev => (
                  <tr key={ev.id}>
                    <td className="mono">{formatTime(ev.createdAt)}</td>
                    <td><Badge value={ev.eventType} /></td>
                    <td>{ev.source}</td>
                    <td><Badge value={ev.status} /></td>
                    <td>{ev.workflowResult?.decision ? <Badge value={ev.workflowResult.decision} /> : '—'}</td>
                    <td>
                      <button className="replay-btn" onClick={() => { setSelectedEvent(ev); setTab('detail') }}>View</button>
                      {' '}
                      <button className="replay-btn" onClick={() => handleReplay(ev.id)}>Replay</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="empty-state">No events yet</div>
          )}
        </div>
      )}

      {tab === 'logs' && (
        <div className="card">
          <h2 className="section-title">Processing Logs</h2>
          {logs?.content?.length > 0 ? (
            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Event ID</th>
                  <th>Level</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                {logs.content.map(log => (
                  <tr key={log.id}>
                    <td className="mono">{formatTime(log.timestamp)}</td>
                    <td className="mono">{log.eventId?.substring(0, 8)}...</td>
                    <td><Badge value={log.level} /></td>
                    <td>{log.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="empty-state">No logs yet</div>
          )}
        </div>
      )}

      {tab === 'detail' && selectedEvent && (
        <div className="card">
          <h2 className="section-title">Event Detail</h2>
          <table>
            <tbody>
              <tr><td style={{ width: 140 }}><strong>ID</strong></td><td className="mono">{selectedEvent.id}</td></tr>
              <tr><td><strong>Type</strong></td><td><Badge value={selectedEvent.eventType} /></td></tr>
              <tr><td><strong>Source</strong></td><td>{selectedEvent.source}</td></tr>
              <tr><td><strong>Status</strong></td><td><Badge value={selectedEvent.status} /></td></tr>
              <tr><td><strong>Retries</strong></td><td>{selectedEvent.retryCount}</td></tr>
              <tr><td><strong>Created</strong></td><td>{formatTime(selectedEvent.createdAt)}</td></tr>
              <tr><td><strong>Updated</strong></td><td>{formatTime(selectedEvent.updatedAt)}</td></tr>
            </tbody>
          </table>

          <h3 style={{ marginTop: 20, marginBottom: 8 }}>Payload</h3>
          <div className="result-box">{JSON.stringify(selectedEvent.payload, null, 2)}</div>

          {selectedEvent.workflowResult && (
            <>
              <h3 style={{ marginTop: 20, marginBottom: 8 }}>Workflow Result</h3>
              <div className="result-box">{JSON.stringify(selectedEvent.workflowResult, null, 2)}</div>
            </>
          )}

          <div style={{ marginTop: 16 }}>
            <button className="replay-btn" onClick={() => handleReplay(selectedEvent.id)}>Replay Event</button>
          </div>
        </div>
      )}
    </div>
  )
}
