import { useState } from 'react'
import { productosApi, pedidosApi } from '../servicios/api'

/** Consume la API protegida con el token que tenga la sesion actual. */
export function PanelApi({ token }) {
  const [salida, setSalida] = useState(null)
  const [titulo, setTitulo] = useState('')
  const [cargando, setCargando] = useState(false)

  const ejecutar = async (nombre, accion) => {
    setCargando(true)
    setTitulo(nombre)
    try {
      setSalida(await accion())
    } catch (e) {
      setSalida({ error: `HTTP ${e.estado ?? '?'}`, detalle: e.cuerpo ?? e.message })
    } finally {
      setCargando(false)
    }
  }

  return (
    <section className="tarjeta">
      <h3>Consumir la API protegida</h3>
      <div className="botonera">
        <button onClick={() => ejecutar('GET /api/v1/public (sin token)', () => productosApi.publico())}>
          Endpoint publico
        </button>
        <button disabled={!token} onClick={() => ejecutar('GET /api/v1/productos', () => productosApi.listar(token))}>
          Listar productos
        </button>
        <button disabled={!token} onClick={() => ejecutar('GET /api/v1/productos/quien-soy', () => productosApi.quienSoy(token))}>
          Quien soy
        </button>
        <button disabled={!token} onClick={() => ejecutar('POST /api/v1/pedidos', () => pedidosApi.crear(token, 1, 2))}>
          Crear pedido
        </button>
        <button disabled={!token} onClick={() => ejecutar('GET /api/v1/pedidos', () => pedidosApi.listar(token))}>
          Mis pedidos
        </button>
      </div>
      {!token && <p className="nota">Inicia sesion para habilitar los endpoints protegidos.</p>}
      {titulo && (
        <div className="resultado">
          <h4>{titulo}</h4>
          <pre>{cargando ? 'Consultando...' : JSON.stringify(salida, null, 2)}</pre>
        </div>
      )}
    </section>
  )
}
