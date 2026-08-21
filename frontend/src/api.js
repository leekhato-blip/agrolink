const defaultBaseUrl = import.meta.env.VITE_API_BASE_URL || ''

function buildUrl(path) {
  return `${defaultBaseUrl}${path}`
}

async function request(path, options = {}) {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), options.timeout ?? 12000)

  try {
    const response = await fetch(buildUrl(path), {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {}),
      },
      signal: controller.signal,
    })

    const contentType = response.headers.get('content-type') || ''
    const payload = contentType.includes('application/json')
      ? await response.json()
      : await response.text()

    if (!response.ok) {
      const message =
        payload && typeof payload === 'object' && 'message' in payload
          ? payload.message
          : 'The AgroLink backend returned an error.'
      const error = new Error(message)
      error.status = response.status
      error.payload = payload
      throw error
    }

    return payload
  } catch (error) {
    if (error.name === 'AbortError') {
      throw new Error('Request timed out while reaching AgroLink.')
    }
    throw error
  } finally {
    window.clearTimeout(timeout)
  }
}

export async function fetchHealth() {
  return request('/api/health')
}

export async function explore(action, entityType, entityId, hops) {
  const encodedId = encodeURIComponent(entityId)

  switch (action) {
    case 'direct':
      return request(`/api/entities/${entityType}/${encodedId}`)
    case 'supplier-dependency':
      return request(`/api/explore/supplier/${encodedId}/farms`)
    case 'disease-to-supplier':
      return request(`/api/explore/disease/${encodedId}/suppliers`)
    case 'shared-suppliers':
      return request(`/api/explore/farm/${encodedId}/shared-suppliers`)
    case 'ecosystem':
      return request(`/api/explore/farm/${encodedId}/ecosystem?hops=${encodeURIComponent(hops)}`)
    case 'supplier-impact':
      return request(`/api/explore/supplier/${encodedId}/impact`)
    default:
      throw new Error('Unsupported exploration action.')
  }
}
