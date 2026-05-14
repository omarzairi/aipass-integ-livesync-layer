import { useState, useEffect, useCallback } from 'react'
import { fetchHealth, fetchEvents, fetchLogs, replayEvent } from './api'

const EVENT_TYPES = ['INVOICE_UPLOADED', 'SUPPLIER_UPDATED', 'HR_ONBOARDING', 'ANOMALY_ALERT']
const STATUSES = ['PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD_LETTER']
const PAGE_SIZE = 10

function Badge({ value, type }) {
  if (!value) return null
  const cls = type || value.toLowerCase()
  return <span className={`badge ${cls}`}>{value.replace(/_/g, ' ')}</span>
}

function MetricCard({ label, value, sub, color }) {
  return (
    <div className="metric-card">
      <div className="label">{label}</div>
      <div className="value" style={color ? { color } : undefined}>{value}</div>
      {sub && <div className="sub">{sub}</div>}
    </div>
  )
}

function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null

  const pages = []
  const maxVisible = 5
  let start = Math.max(0, page - Math.floor(maxVisible / 2))
  let end = Math.min(totalPages, start + maxVisible)
  if (end - start < maxVisible) start = Math.max(0, end - maxVisible)

  for (let i = start; i < end; i++) pages.push(i)

  return (
    <div className="pagination">
      <button className="page-btn" disabled={page === 0} onClick={() => onPageChange(0)}>&laquo;</button>
      <button className="page-btn" disabled={page === 0} onClick={() => onPageChange(page - 1)}>&lsaquo;</button>
      {start > 0 && <span className="page-ellipsis">&hellip;</span>}
      {pages.map(p => (
        <button key={p} className={`page-btn ${p === page ? 'active' : ''}`} onClick={() => onPageChange(p)}>
          {p + 1}
        </button>
      ))}
      {end < totalPages && <span className="page-ellipsis">&hellip;</span>}
      <button className="page-btn" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>&rsaquo;</button>
      <button className="page-btn" disabled={page >= totalPages - 1} onClick={() => onPageChange(totalPages - 1)}>&raquo;</button>
    </div>
  )
}

function formatTime(iso) {
  if (!iso) return '\u2014'
  return new Date(iso).toLocaleString(undefined, {
    month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

function formatUptime(sec) {
  if (sec < 60) return `${sec}s`
  if (sec < 3600) return `${Math.floor(sec / 60)}m ${sec % 60}s`
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return `${h}h ${m}m`
}

export default function App() {
  const [tab, setTab] = useState('events')
  const [health, setHealth] = useState(null)
  const [events, setEvents] = useState(null)
  const [logs, setLogs] = useState(null)
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState(null)

  const [eventsPage, setEventsPage] = useState(0)
  const [logsPage, setLogsPage] = useState(0)
  const [filterType, setFilterType] = useState('')
  const [filterStatus, setFilterStatus] = useState('')

  const loadHealth = useCallback(async () => {
    try { setHealth(await fetchHealth()); setError(null) }
    catch (err) { setError(err.message) }
  }, [])

  const loadEvents = useCallback(async () => {
    try {
      setEvents(await fetchEvents(eventsPage, PAGE_SIZE, filterType || undefined, filterStatus || undefined))
    } catch {}
  }, [eventsPage, filterType, filterStatus])

  const loadLogs = useCallback(async () => {
    try { setLogs(await fetchLogs(null, logsPage, 20)) } catch {}
  }, [logsPage])

  useEffect(() => { loadHealth(); const id = setInterval(loadHealth, 6000); return () => clearInterval(id) }, [loadHealth])
  useEffect(() => { loadEvents() }, [loadEvents])
  useEffect(() => { if (tab === 'logs') loadLogs() }, [tab, loadLogs])

  const handleReplay = async (eventId) => {
    try { await replayEvent(eventId); setTimeout(loadEvents, 600) }
    catch (err) { alert('Replay failed: ' + err.message) }
  }

  const clearFilters = () => { setFilterType(''); setFilterStatus(''); setEventsPage(0) }

  const m = health?.metrics || {}
  const q = health?.queue || {}

  return (
    <div className="app">
      <nav className="navbar">
        <div className="navbar-brand">
          <div className="logo">AP</div>
          AI-Pass LiveSync
        </div>
        <div className="navbar-status">
          {health && (
            <div className="status-indicator">
              <span className={`status-dot ${health.status === 'UP' ? 'up' : 'down'}`} />
              <span>{health.status === 'UP' ? 'All systems operational' : 'Service degraded'}</span>
            </div>
          )}
          <span className="refresh-text">Live</span>
        </div>
      </nav>

      <div className="container">
        {error && <div className="error-banner">Unable to reach backend: {error}</div>}

        {health && (
          <div className="metrics-row">
            <MetricCard label="Uptime" value={formatUptime(health.uptimeSeconds)} sub="Since last deploy" />
            <MetricCard label="Received" value={Math.round(m.events_received || 0)} sub="Total events ingested" />
            <MetricCard label="Processed" value={Math.round(m.events_processed || 0)} sub="Successfully completed" color="var(--green)" />
            <MetricCard label="Failed" value={Math.round(m.events_failed || 0)} sub={`${Math.round(m.events_dead_letter || 0)} dead-lettered`} color={m.events_failed > 0 ? 'var(--red)' : undefined} />
            <MetricCard label="Queue Depth" value={q.depth ?? 0} sub={q.redis_available ? 'Redis connected' : 'In-memory fallback'} color={q.depth > 0 ? 'var(--yellow)' : undefined} />
            <MetricCard label="Pending" value={Math.round(m.events_pending || 0)} sub="Awaiting processing" />
          </div>
        )}

        <div className="pipeline">
          <h2>Event Processing Pipeline</h2>
          <div className="pipeline-steps">
            <div className="pipeline-step">
              <div className="step-box ingestion">
                Webhook Ingestion
                <span className="step-count">{Math.round(m.events_received || 0)}</span>
              </div>
            </div>
            <span className="step-arrow">&rarr;</span>
            <div className="pipeline-step">
              <div className="step-box queue">
                Redis Queue
                <span className="step-count">{q.depth ?? 0}</span>
              </div>
            </div>
            <span className="step-arrow">&rarr;</span>
            <div className="pipeline-step">
              <div className="step-box worker">
                Async Worker
                <span className="step-count">{Math.round(m.events_processed || 0) + Math.round(m.events_failed || 0)}</span>
              </div>
            </div>
            <span className="step-arrow">&rarr;</span>
            <div className="pipeline-step">
              <div className="step-box workflow">
                Workflow Engine
                <span className="step-count">{Math.round(m.events_processed || 0)}</span>
              </div>
            </div>
            <span className="step-arrow">&rarr;</span>
            <div className="pipeline-step">
              <div className="step-box result">
                Structured Result
                <span className="step-count">{Math.round(m.events_processed || 0)}</span>
              </div>
            </div>
          </div>
        </div>

        <div className="tabs-bar">
          {['events', 'logs'].map(t => (
            <button key={t} className={`tab-btn ${tab === t ? 'active' : ''}`}
              onClick={() => { setTab(t); if (t === 'events') setEventsPage(0); if (t === 'logs') setLogsPage(0) }}>
              {t === 'events' ? 'Events' : 'Processing Logs'}
            </button>
          ))}
          {selected && (
            <button className={`tab-btn ${tab === 'detail' ? 'active' : ''}`} onClick={() => setTab('detail')}>
              Detail
            </button>
          )}
        </div>

        {tab === 'events' && (
          <div className="panel">
            <div className="panel-header">
              <h2>Events{events ? ` (${events.totalElements})` : ''}</h2>
              <div className="filter-bar">
                <select className="filter-select" value={filterType} onChange={e => { setFilterType(e.target.value); setEventsPage(0) }}>
                  <option value="">All types</option>
                  {EVENT_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
                </select>
                <select className="filter-select" value={filterStatus} onChange={e => { setFilterStatus(e.target.value); setEventsPage(0) }}>
                  <option value="">All statuses</option>
                  {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
                {(filterType || filterStatus) && (
                  <button className="btn" onClick={clearFilters}>Clear</button>
                )}
              </div>
            </div>
            <div className="panel-body">
              {events?.content?.length > 0 ? (
                <>
                  <table>
                    <thead>
                      <tr>
                        <th>Timestamp</th>
                        <th>Event Type</th>
                        <th>Source</th>
                        <th>Status</th>
                        <th className="hide-mobile">Decision</th>
                        <th className="hide-mobile">Reason</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {events.content.map(ev => (
                        <tr key={ev.id}>
                          <td className="mono">{formatTime(ev.createdAt)}</td>
                          <td><Badge value={ev.eventType} type={ev.eventType?.toLowerCase()} /></td>
                          <td>{ev.source}</td>
                          <td><Badge value={ev.status} /></td>
                          <td className="hide-mobile">
                            {ev.workflowResult?.decision
                              ? <Badge value={ev.workflowResult.decision} />
                              : <span style={{ color: 'var(--text-dim)' }}>&mdash;</span>}
                          </td>
                          <td className="hide-mobile" style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {ev.workflowResult?.reason || '\u2014'}
                          </td>
                          <td>
                            <button className="btn" onClick={() => { setSelected(ev); setTab('detail') }}>View</button>
                            {' '}
                            <button className="btn" onClick={() => handleReplay(ev.id)}>Replay</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <Pagination page={eventsPage} totalPages={events.totalPages} onPageChange={setEventsPage} />
                </>
              ) : (
                <div className="empty-state">
                  {(filterType || filterStatus) ? 'No events match the selected filters.' : 'No events ingested yet. POST to /api/events/webhook to get started.'}
                </div>
              )}
            </div>
          </div>
        )}

        {tab === 'logs' && (
          <div className="panel">
            <div className="panel-header">
              <h2>Processing Logs{logs ? ` (${logs.totalElements})` : ''}</h2>
              <button className="btn" onClick={loadLogs}>Refresh</button>
            </div>
            <div className="panel-body">
              {logs?.content?.length > 0 ? (
                <>
                  <table>
                    <thead>
                      <tr>
                        <th>Timestamp</th>
                        <th>Event ID</th>
                        <th>Level</th>
                        <th>Message</th>
                      </tr>
                    </thead>
                    <tbody>
                      {logs.content.map(log => (
                        <tr key={log.id}>
                          <td className="mono">{formatTime(log.timestamp)}</td>
                          <td className="mono">{log.eventId?.substring(0, 8)}</td>
                          <td><Badge value={log.level} type={log.level?.toLowerCase()} /></td>
                          <td>{log.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <Pagination page={logsPage} totalPages={logs.totalPages} onPageChange={setLogsPage} />
                </>
              ) : (
                <div className="empty-state">No processing logs yet.</div>
              )}
            </div>
          </div>
        )}

        {tab === 'detail' && selected && (
          <div className="panel">
            <div className="panel-header">
              <h2>Event Detail</h2>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn" onClick={() => handleReplay(selected.id)}>Replay</button>
                <button className="btn" onClick={() => setTab('events')}>Back</button>
              </div>
            </div>
            <div className="panel-body">
              <dl className="detail-grid">
                <dt>ID</dt><dd className="mono">{selected.id}</dd>
                <dt>Type</dt><dd><Badge value={selected.eventType} type={selected.eventType?.toLowerCase()} /></dd>
                <dt>Source</dt><dd>{selected.source}</dd>
                <dt>Status</dt><dd><Badge value={selected.status} /></dd>
                <dt>Retries</dt><dd>{selected.retryCount}</dd>
                <dt>Created</dt><dd>{formatTime(selected.createdAt)}</dd>
                <dt>Updated</dt><dd>{formatTime(selected.updatedAt)}</dd>
              </dl>

              <h3 style={{ padding: '14px 16px 6px', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Payload</h3>
              <div className="code-block">{JSON.stringify(selected.payload, null, 2)}</div>

              {selected.workflowResult && (
                <>
                  <h3 style={{ padding: '8px 16px 6px', fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Workflow Result</h3>
                  <div className="code-block">{JSON.stringify(selected.workflowResult, null, 2)}</div>
                </>
              )}
            </div>
          </div>
        )}

        <footer style={{ textAlign: 'center', padding: '32px 0 16px', fontSize: '0.72rem', color: 'var(--text-dim)' }}>
          AI-Pass LiveSync Engine &middot; <a href="/swagger-ui/index.html" style={{ color: 'var(--accent)' }}>Swagger UI</a> &middot; <a href="/actuator/prometheus" style={{ color: 'var(--accent)' }}>Prometheus Metrics</a>
        </footer>
      </div>
    </div>
  )
}
