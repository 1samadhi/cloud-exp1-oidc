/**
 * Configuracion de desarrollo. El backend siempre es el que esta en AWS: no
 * tiene sentido levantar los microservicios en local para probar el login,
 * porque el token lo emite Azure de todas formas.
 */
export const environment = {
  produccion: false,
  entra: {
    clientId: 'c483f102-da2d-41b5-b48e-26e7ca78ab98',
    tenantId: '5cb85dc6-a73b-41fc-b2b9-5b2a9b3f531b',
    // MSAL arma solo los endpoints v2 a partir de esta autoridad: no lleva /v2.0
    autoridad: 'https://login.microsoftonline.com/5cb85dc6-a73b-41fc-b2b9-5b2a9b3f531b',
    redirectUri: 'http://localhost:4200',
    appIdUri: 'api://c483f102-da2d-41b5-b48e-26e7ca78ab98'
  },
  apiBase: 'https://j37oj1wn16.execute-api.us-east-1.amazonaws.com/desarrollo'
};
