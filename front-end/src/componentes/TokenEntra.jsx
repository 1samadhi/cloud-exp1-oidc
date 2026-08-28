import { useState } from 'react'
import { useMsal, useIsAuthenticated } from '@azure/msal-react'
import { apiRequest } from '../auth/AuthConfig'

/**
 * Pide a Azure un token para NUESTRA API (no para Graph) y lo entrega a la app.
 * Intenta primero en silencio; si Azure exige interaccion (por ejemplo un
 * consentimiento pendiente) redirige y vuelve.
 */
export function TokenEntra({ alObtenerToken }) {
  const { instance, accounts } = useMsal()
  const autenticado = useIsAuthenticated()
  const [error, setError] = useState('')

  if (!autenticado) return null

  const obtener = async () => {
    setError('')
    const peticion = { ...apiRequest, account: accounts[0] }
    if (peticion.scopes.length === 0) {
      setError('Falta definir VITE_ENTRA_SCOPE_API con el scope de tu API expuesta en Azure.')
      return
    }
    try {
      const respuesta = await instance.acquireTokenSilent(peticion)
      alObtenerToken(respuesta.accessToken, 'Microsoft Entra ID')
    } catch (e) {
      console.warn('Token silencioso fallido, se redirige a Azure:', e)
      await instance.acquireTokenRedirect(peticion)
    }
  }

  return (
    <div>
      <button className="boton-secundario" onClick={obtener}>
        Obtener token de Entra para la API
      </button>
      {error && <p className="error">{error}</p>}
    </div>
  )
}
