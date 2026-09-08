import {
  IPublicClientApplication,
  PublicClientApplication,
  InteractionType,
  BrowserCacheLocation,
  LogLevel
} from '@azure/msal-browser';
import { MsalGuardConfiguration, MsalInterceptorConfiguration } from '@azure/msal-angular';

import { environment } from '../../environments/environment';

const { clientId, autoridad, redirectUri, appIdUri } = environment.entra;

/** Scopes que expone nuestra API en Entra ID. */
export const SCOPES = {
  productosLeer: `${appIdUri}/productos.leer`,
  pedidosEscribir: `${appIdUri}/pedidos.escribir`
};

/**
 * Instancia de MSAL.
 *
 * No se configura ningun secreto porque no puede haberlo: el JavaScript de una
 * SPA es visible para cualquiera. La seguridad del intercambio la da PKCE, que
 * MSAL aplica solo por tratarse de un cliente publico registrado como "spa" en
 * Entra ID.
 */
export function crearMsal(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId,
      authority: autoridad,
      redirectUri,
      postLogoutRedirectUri: redirectUri
    },
    cache: {
      // localStorage mantiene la sesion entre pestanias y recargas. Con
      // sessionStorage cada pestania obligaria a iniciar sesion de nuevo.
      cacheLocation: BrowserCacheLocation.LocalStorage
    },
    system: {
      loggerOptions: {
        loggerCallback: (nivel: LogLevel, mensaje: string) => {
          if (nivel === LogLevel.Error) {
            console.error('[MSAL]', mensaje);
          }
        },
        logLevel: LogLevel.Error,
        piiLoggingEnabled: false
      }
    }
  });
}

/**
 * Que hace el MsalGuard cuando una ruta protegida se visita sin sesion:
 * redirigir a Microsoft. Los scopes que se piden aqui son los del login
 * inicial.
 */
export function configuracionDelGuard(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: [SCOPES.productosLeer, SCOPES.pedidosEscribir]
    }
  };
}

/**
 * Que token adjunta el MsalInterceptor a cada peticion, segun la URL.
 *
 * Esta es la razon por la que ningun componente ni servicio de esta aplicacion
 * toca el header Authorization: se declara que scope corresponde a cada
 * endpoint y el interceptor consigue el token, lo renueva si expiro y lo
 * adjunta. Una URL que no este en este mapa viaja sin token.
 */
export function configuracionDelInterceptor(): MsalInterceptorConfiguration {
  const mapa = new Map<string, Array<string>>();
  const api = environment.apiBase;

  mapa.set(`${api}/v1/productos`, [SCOPES.productosLeer]);
  mapa.set(`${api}/v1/pedidos`, [SCOPES.pedidosEscribir]);

  // El endpoint publico y el registro de usuarios son deliberadamente anonimos:
  // no aparecen en el mapa, asi que el interceptor los deja pasar sin token.

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap: mapa
  };
}
