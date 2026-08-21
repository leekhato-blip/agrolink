import React, { useEffect, useMemo, useState } from 'react'
import { actionCatalog, entityCatalog, entityDescriptions } from './data'
import { Badge, EmptyState, EntityPill, ErrorState, LoadingState, Panel, PathRail, Stat } from './components'
import { explore, fetchHealth } from './api'

const defaultType = 'Supplier'

const initialAction = actionCatalog.find((action) => action.types.includes(defaultType))?.id ?? 'direct'

function formatStatus(status) {
  return status ? status.toUpperCase() : 'UNKNOWN'
}

function pickFirstEntity(type) {
  return entityCatalog[type]?.[0]?.id ?? ''
}

function normalizeActionForType(type, currentAction) {
  const allowed = actionCatalog.filter((action) => action.types.includes(type))
  return allowed.some((action) => action.id === currentAction) ? currentAction : allowed[0]?.id ?? 'direct'
}

function prettyLabel(type) {
  return type === 'FishPond' ? 'Fish Pond' : type
}

export default function App() {
  const [entityType, setEntityType] = useState(defaultType)
  const [entityId, setEntityId] = useState(pickFirstEntity(defaultType))
  const [action, setAction] = useState(initialAction)
  const [hops, setHops] = useState(3)
  const [health, setHealth] = useState({ loading: true, status: 'checking' })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const availableActions = useMemo(
    () => actionCatalog.filter((item) => item.types.includes(entityType)),
    [entityType],
  )

  useEffect(() => {
    setAction((current) => normalizeActionForType(entityType, current))
    setEntityId(pickFirstEntity(entityType))
  }, [entityType])

  useEffect(() => {
    let cancelled = false

    async function loadHealth() {
      try {
        const response = await fetchHealth()
        if (cancelled) return
        setHealth({
          loading: false,
          status: response.status,
          database: response.database,
        })
      } catch (err) {
        if (cancelled) return
        setHealth({
          loading: false,
          status: 'down',
          database: 'CognoDB',
        })
      }
    }

    loadHealth()
    return () => {
      cancelled = true
    }
  }, [])

  async function handleSubmit(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setResult(null)

    try {
      const response = await explore(action, entityType, entityId, hops)
      setResult(response)
    } catch (err) {
      setError(err.message || 'We could not reach the AgroLink graph right now.')
    } finally {
      setLoading(false)
    }
  }

  const selectedEntity = entityCatalog[entityType].find((item) => item.id === entityId)

  return (
    <div className="shell">
      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />

      <header className="hero">
        <div>
          <Badge tone={health.status === 'UP' ? 'success' : 'danger'}>
            {health.loading ? 'Checking database' : `Database ${formatStatus(health.status)}`}
          </Badge>
          <h1>AgroLink</h1>
          <p className="subtitle">Farm Dependency & Relationship Intelligence</p>
          <p className="lede">
            Explore how farms, livestock, ponds, feed, suppliers, and diseases connect through graph paths.
          </p>
        </div>

        <div className="hero-card">
          <Stat
            label="Current focus"
            value={prettyLabel(entityType)}
            hint={entityDescriptions[entityType]}
          />
          <Stat label="Selected entity" value={selectedEntity?.name ?? entityId} hint={entityId} />
          <Stat
            label="Graph mode"
            value={actionCatalog.find((item) => item.id === action)?.label ?? 'Direct connections'}
            hint="Designed for path-based exploration"
          />
        </div>
      </header>

      <main className="layout">
        <Panel title="Explore Relationships" eyebrow="Primary workflow">
          <form className="explore-form" onSubmit={handleSubmit}>
            <label>
              <span>Entity Type</span>
              <select value={entityType} onChange={(event) => setEntityType(event.target.value)}>
                {Object.keys(entityCatalog).map((type) => (
                  <option key={type} value={type}>
                    {prettyLabel(type)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span>Entity</span>
              <select value={entityId} onChange={(event) => setEntityId(event.target.value)}>
                {entityCatalog[entityType].map((entity) => (
                  <option key={entity.id} value={entity.id}>
                    {entity.name} ({entity.id})
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span>Explore</span>
              <select value={action} onChange={(event) => setAction(event.target.value)}>
                {availableActions.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>

            {action === 'ecosystem' ? (
              <label>
                <span>Hop count</span>
                <input
                  type="number"
                  min="1"
                  max="4"
                  value={hops}
                  onChange={(event) => setHops(Number(event.target.value))}
                />
              </label>
            ) : null}

            <button type="submit" disabled={loading}>
              {loading ? 'Exploring relationships...' : 'Run exploration'}
            </button>
          </form>
        </Panel>

        <section className="content-grid">
          <Panel
            title="Selected entity"
            eyebrow="Context"
            aside={<Badge tone="neutral">{entityId}</Badge>}
          >
            <div className="entity-summary">
              <EntityPill label={selectedEntity?.name ?? 'Unknown entity'} sublabel={prettyLabel(entityType)} />
              <p>{entityDescriptions[entityType]}</p>
            </div>
          </Panel>

          <Panel
            title="Results"
            eyebrow="Traversal output"
            aside={result?.status ? <Badge tone="success">{result.status}</Badge> : null}
          >
            {loading ? <LoadingState /> : null}
            {!loading && error ? (
              <ErrorState
                title="We couldn't reach the AgroLink graph right now."
                description={error}
              />
            ) : null}
            {!loading && !error && !result ? (
              <EmptyState
                title="Ready to explore"
                description="Choose an entity and run a graph traversal to see connected farms, suppliers, feeds, and disease paths."
              />
            ) : null}
            {!loading && result ? <ResultRenderer action={action} result={result} /> : null}
          </Panel>
        </section>
      </main>
    </div>
  )
}

function ResultRenderer({ action, result }) {
  const payload = result?.data ?? result

  if (action === 'supplier-impact') {
    return <ImpactResult payload={payload} />
  }

  if (action === 'ecosystem') {
    return <EcosystemResult payload={payload} />
  }

  if (action === 'direct') {
    return <DirectResult payload={payload} />
  }

  return <GenericResult payload={payload} />
}

function DirectResult({ payload }) {
  const items = payload?.connections ?? payload?.entities ?? []

  if (!items.length) {
    return <EmptyState title="No direct connections found." description="This entity has no immediate graph neighbors yet." />
  }

  return (
    <div className="result-list">
      {items.map((item, index) => (
        <div className="result-card" key={`${item.id ?? item.name ?? index}-${item.relationship ?? ''}-${index}`}>
          <div className="result-card-head">
            <strong>{item.name ?? item.label ?? item.entity?.name ?? 'Connected entity'}</strong>
            {item.relationship ? <Badge tone="neutral">{item.relationship}</Badge> : null}
          </div>
          <p>{item.type ?? item.entityType ?? item.label ?? ''}</p>
          {item.path ? <PathRail nodes={item.path} /> : null}
        </div>
      ))}
    </div>
  )
}

function ImpactResult({ payload }) {
  const grouped = [
    ['Affected feeds', payload?.feeds],
    ['Affected livestock', payload?.livestock],
    ['Affected ponds', payload?.ponds],
    ['Affected farms', payload?.farms],
  ].filter(([, items]) => Array.isArray(items) && items.length > 0)

  if (!grouped.length) {
    return <EmptyState title="No impact data returned." description="Try another supplier or connect the backend impact query." />
  }

  return (
    <div className="impact-grid">
      {grouped.map(([title, items]) => (
        <div className="impact-group" key={title}>
          <strong>{title}</strong>
          <div className="mini-list">
            {items.map((item, index) => (
              <div className="mini-item" key={`${title}-${item.id ?? index}-${index}`}>
                <span>{item.name ?? item.label ?? item.id}</span>
                {item.reason ? <small>{item.reason}</small> : null}
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

function EcosystemResult({ payload }) {
  const nodes = payload?.nodes ?? payload?.entities ?? []

  if (!nodes.length) {
    return <EmptyState title="No ecosystem nodes returned." description="Run the farm ecosystem traversal once the backend endpoint is available." />
  }

  return (
    <div className="result-list">
      {nodes.map((item, index) => (
        <div className="result-card" key={`${item.id ?? index}-${index}`}>
          <div className="result-card-head">
            <strong>{item.name ?? item.label}</strong>
            {item.hop !== undefined ? <Badge tone="neutral">{item.hop} hops</Badge> : null}
          </div>
          <p>{item.type ?? ''}</p>
          {item.path ? <PathRail nodes={item.path} /> : null}
        </div>
      ))}
    </div>
  )
}

function GenericResult({ payload }) {
  const rows = Array.isArray(payload) ? payload : payload?.items ?? payload?.results ?? []

  if (!rows.length) {
    return <EmptyState title="No relationships found." description="The selected entity did not return any linked results." />
  }

  return (
    <div className="result-list">
      {rows.map((item, index) => (
        <div className="result-card" key={`${item.id ?? index}-${item.relationship ?? ''}-${index}`}>
          <div className="result-card-head">
            <strong>{item.name ?? item.label ?? item.id ?? 'Connected item'}</strong>
            {item.relationship ? <Badge tone="neutral">{item.relationship}</Badge> : null}
          </div>
          <p>{item.summary ?? item.type ?? item.entityType ?? ''}</p>
          {item.path ? <PathRail nodes={item.path} /> : null}
        </div>
      ))}
    </div>
  )
}
