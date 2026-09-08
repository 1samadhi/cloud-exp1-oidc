/**
 * Configuracion de produccion: el frontend se sirve detras del stage del API
 * Gateway, asi que el redirectUri registrado en Entra apunta a esa URL.
 */
export const environment = {
  produccion: true,
  entra: {
    clientId: 'c483f102-da2d-41b5-b48e-26e7ca78ab98',
    tenantId: '5cb85dc6-a73b-41fc-b2b9-5b2a9b3f531b',
    // MSAL arma solo los endpoints v2 a partir de esta autoridad: no lleva /v2.0
    autoridad: 'https://login.microsoftonline.com/5cb85dc6-a73b-41fc-b2b9-5b2a9b3f531b',
    redirectUri: 'https://j37oj1wn16.execute-api.us-east-1.amazonaws.com/desarrollo/',
    appIdUri: 'api://c483f102-da2d-41b5-b48e-26e7ca78ab98'
  },
  apiBase: 'https://j37oj1wn16.execute-api.us-east-1.amazonaws.com/desarrollo'
};
