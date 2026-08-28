// Configuracion de MSAL. Todo sale de variables de entorno: los identificadores
// de Azure no se hardcodean para que el repositorio sea publico sin arrastrar
// la configuracion de un tenant concreto.

const clientId = import.meta.env.VITE_ENTRA_CLIENT_ID ?? ''
const tenantId = import.meta.env.VITE_ENTRA_TENANT_ID ?? ''
const redirectUri = import.meta.env.VITE_REDIRECT_URI ?? window.location.origin

/** Permite que la app arranque y muestre un aviso claro si falta configurar Azure. */
export const entraConfigurado = Boolean(clientId && tenantId)

export const msalConfig = {
  auth: {
    clientId,
    authority: `https://login.microsoftonline.com/${tenantId}`,
    redirectUri,
    postLogoutRedirectUri: redirectUri
  },
  cache: {
    cacheLocation: 'sessionStorage',
    storeAuthStateInCookie: false
  }
}

export const loginRequest = {
  scopes: ['openid', 'profile']
}

// Scope de NUESTRA API expuesta en Azure ("Exponer una API" en el registro).
// Un token pedido con scopes de Microsoft Graph viene cifrado y Spring no puede
// validarlo: siempre daria 401. Por eso hay que pedir el scope propio.
export const apiRequest = {
  scopes: [import.meta.env.VITE_ENTRA_SCOPE_API].filter(Boolean)
}

export const emisorEntra = tenantId
  ? `https://login.microsoftonline.com/${tenantId}/v2.0`
  : null
