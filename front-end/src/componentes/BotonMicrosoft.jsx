import { useMsal } from '@azure/msal-react'
import { loginRequest } from '../auth/AuthConfig'

export function BotonMicrosoft({ alObtenerToken }) {
  const { instance, accounts } = useMsal()

  const entrar = () => {
    // Redirect en vez de popup: evita los bloqueos de ventanas emergentes.
    instance.loginRedirect(loginRequest).catch((e) => console.error('Error al iniciar sesion:', e))
  }

  const salir = () => {
    alObtenerToken?.(null)
    instance.logoutRedirect().catch((e) => console.error('Error al cerrar sesion:', e))
  }

  if (accounts.length > 0) {
    return (
      <div className="fila">
        <span>Sesion Microsoft: <strong>{accounts[0].name ?? accounts[0].username}</strong></span>
        <button className="boton-secundario" onClick={salir}>Cerrar sesion</button>
      </div>
    )
  }

  return (
    <button className="boton-microsoft" onClick={entrar}>
      <span className="logo-microsoft">
        <i style={{ background: '#f25022' }} />
        <i style={{ background: '#7fba00' }} />
        <i style={{ background: '#00a4ef' }} />
        <i style={{ background: '#ffb900' }} />
      </span>
      Iniciar sesion con Microsoft
    </button>
  )
}
