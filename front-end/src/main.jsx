import React from 'react'
import ReactDOM from 'react-dom/client'
import { PublicClientApplication, EventType } from '@azure/msal-browser'
import { MsalProvider } from '@azure/msal-react'

import App from './App.jsx'
import { msalConfig, entraConfigurado } from './auth/AuthConfig'
import './index.css'

const raiz = ReactDOM.createRoot(document.getElementById('root'))

function mostrarError(titulo, detalle) {
  raiz.render(
    <div className="aviso-error">
      <h3>{titulo}</h3>
      <pre>{detalle}</pre>
    </div>
  )
}

function detalleDe(error) {
  return [
    `codigo: ${error?.errorCode ?? '(sin codigo)'}`,
    `mensaje: ${error?.errorMessage ?? error?.message}`,
    `correlationId: ${error?.correlationId ?? '(sin correlationId)'}`
  ].join('\n')
}

async function arrancar() {
  // Sin configuracion de Azure la app igual funciona: queda disponible el login
  // contra el Identity Provider propio y se avisa que falta configurar Entra.
  if (!entraConfigurado) {
    raiz.render(
      <React.StrictMode>
        <App msalDisponible={false} />
      </React.StrictMode>
    )
    return
  }

  const instancia = new PublicClientApplication(msalConfig)
  instancia.addEventCallback((evento) => {
    if (evento.eventType === EventType.LOGIN_FAILURE ||
        evento.eventType === EventType.ACQUIRE_TOKEN_FAILURE) {
      console.error('Fallo de MSAL:', evento.error)
    }
  })

  await instancia.initialize()

  try {
    // Procesa la vuelta desde la pagina de login de Microsoft.
    await instancia.handleRedirectPromise()
  } catch (error) {
    console.error('Azure devolvio un error al procesar la respuesta:', error)
    window.history.replaceState(null, '', window.location.pathname)
    mostrarError('Azure devolvio un error al procesar el token', detalleDe(error))
    return
  }

  raiz.render(
    <React.StrictMode>
      <MsalProvider instance={instancia}>
        <App msalDisponible={true} />
      </MsalProvider>
    </React.StrictMode>
  )
}

arrancar().catch((error) => {
  console.error('Error al inicializar MSAL:', error)
  mostrarError('No se pudo inicializar la autenticacion de Azure', detalleDe(error))
})
