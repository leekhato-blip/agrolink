import React from 'react'

export function Badge({ tone = 'neutral', children }) {
  return <span className={`badge badge-${tone}`}>{children}</span>
}

export function Panel({ title, eyebrow, children, aside }) {
  return (
    <section className="panel">
      <div className="panel-header">
        <div>
          {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
          <h2>{title}</h2>
        </div>
        {aside}
      </div>
      {children}
    </section>
  )
}

export function Stat({ label, value, hint }) {
  return (
    <div className="stat">
      <span>{label}</span>
      <strong>{value}</strong>
      {hint ? <small>{hint}</small> : null}
    </div>
  )
}

export function EntityPill({ label, sublabel }) {
  return (
    <div className="entity-pill">
      <strong>{label}</strong>
      <span>{sublabel}</span>
    </div>
  )
}

export function EmptyState({ title, description }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  )
}

export function ErrorState({ title, description }) {
  return (
    <div className="error-state">
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  )
}

export function LoadingState({ label = 'Exploring relationships...' }) {
  return <div className="loading-state">{label}</div>
}

export function PathRail({ nodes = [] }) {
  if (!nodes.length) return null
  return (
    <div className="path-rail">
      {nodes.map((node, index) => (
        <React.Fragment key={`${node.label}-${index}`}>
          <div className="path-node">
            <span>{node.type}</span>
            <strong>{node.label}</strong>
            {node.id ? <small>{node.id}</small> : null}
          </div>
          {index < nodes.length - 1 ? <div className="path-arrow">→</div> : null}
        </React.Fragment>
      ))}
    </div>
  )
}
