const urlProductos = import.meta.env.VITE_API_PRODUCTOS ?? 'http://localhost:8081'
const urlPedidos = import.meta.env.VITE_API_PEDIDOS ?? 'http://localhost:8082'
const urlAuth = import.meta.env.VITE_API_AUTH ?? 'http://localhost:9000'

async function pedir(url, token, opciones = {}) {
  const respuesta = await fetch(url, {
    ...opciones,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...opciones.headers
    }
  })
  const texto = await respuesta.text()
  let cuerpo
  try {
    cuerpo = texto ? JSON.parse(texto) : null
  } catch {
    cuerpo = texto
  }
  if (!respuesta.ok) {
    const error = new Error(`HTTP ${respuesta.status}`)
    error.estado = respuesta.status
    error.cuerpo = cuerpo
    throw error
  }
  return cuerpo
}

/** Login contra el Identity Provider propio (ms-auth). */
export const authApi = {
  login: (username, password) =>
    pedir(`${urlAuth}/auth/login`, null, {
      method: 'POST',
      body: JSON.stringify({ username, password })
    })
}

export const productosApi = {
  publico: () => pedir(`${urlProductos}/api/v1/public`, null),
  listar: (token) => pedir(`${urlProductos}/api/v1/productos`, token),
  quienSoy: (token) => pedir(`${urlProductos}/api/v1/productos/quien-soy`, token)
}

export const pedidosApi = {
  listar: (token) => pedir(`${urlPedidos}/api/v1/pedidos`, token),
  crear: (token, productoId, cantidad) =>
    pedir(`${urlPedidos}/api/v1/pedidos`, token, {
      method: 'POST',
      body: JSON.stringify({ productoId, cantidad })
    })
}

/** Decodifica el payload de un JWT sin verificarlo, solo para mostrarlo en pantalla. */
export function leerClaims(token) {
  try {
    const carga = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(decodeURIComponent(escape(atob(carga))))
  } catch {
    return null
  }
}
