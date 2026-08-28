import { useState } from 'react'
import { BotonMicrosoft } from './componentes/BotonMicrosoft'
import { LoginPropio } from './componentes/LoginPropio'
import { PanelApi } from './componentes/PanelApi'
import { PanelToken } from './componentes/PanelToken'
import { TokenEntra } from './componentes/TokenEntra'

export default function App({ msalDisponible }) {
  const [token, setToken] = useState(null)
  const [origen, setOrigen] = useState('')

  const guardarToken = (nuevo, procedencia) => {
    setToken(nuevo)
    setOrigen(procedencia ?? '')
  }

  return (
    <main>
      <header>
        <h1>EXP1 — Arquitectura segura con OIDC</h1>
        <p>
          El mismo Resource Server acepta tokens de tres emisores distintos.
          Inicia sesion con cualquiera y consume la API protegida.
        </p>
      </header>

      <section className="tarjeta">
        <h3>Identity Provider propio</h3>
        <LoginPropio alObtenerToken={guardarToken} />
      </section>

      <section className="tarjeta">
        <h3>Microsoft Entra ID</h3>
        {msalDisponible ? (
          <>
            <BotonMicrosoft alObtenerToken={guardarToken} />
            <TokenEntra alObtenerToken={guardarToken} />
          </>
        ) : (
          <p className="nota">
            Entra ID no esta configurado. Define <code>VITE_ENTRA_CLIENT_ID</code> y{' '}
            <code>VITE_ENTRA_TENANT_ID</code> en <code>.env.local</code>.
            Ver <code>docs/02-entra-id.md</code>.
          </p>
        )}
      </section>

      <PanelToken token={token} origen={origen} />
      <PanelApi token={token} />
    </main>
  )
}
