import { useState } from 'react'
import { authApi } from '../servicios/api'

/**
 * Login contra el Identity Provider propio (ms-auth). Convive con el boton de
 * Microsoft para poder comprobar que el mismo Resource Server acepta tokens de
 * emisores distintos.
 */
export function LoginPropio({ alObtenerToken }) {
  const [usuario, setUsuario] = useState('cliente')
  const [clave, setClave] = useState('cliente123')
  const [error, setError] = useState('')
  const [cargando, setCargando] = useState(false)

  const entrar = async (evento) => {
    evento.preventDefault()
    setError('')
    setCargando(true)
    try {
      const respuesta = await authApi.login(usuario, clave)
      alObtenerToken(respuesta.access_token, 'Identity Provider propio')
    } catch (e) {
      setError(e.estado === 401 ? 'Credenciales invalidas' : 'No se pudo contactar al IdP')
    } finally {
      setCargando(false)
    }
  }

  return (
    <form className="formulario" onSubmit={entrar}>
      <input value={usuario} onChange={(e) => setUsuario(e.target.value)} placeholder="usuario" />
      <input type="password" value={clave} onChange={(e) => setClave(e.target.value)} placeholder="contrasenia" />
      <button type="submit" disabled={cargando}>
        {cargando ? 'Entrando...' : 'Entrar con el IdP propio'}
      </button>
      {error && <p className="error">{error}</p>}
    </form>
  )
}
