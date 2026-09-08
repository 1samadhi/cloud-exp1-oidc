import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';

import { Inicio } from './paginas/inicio/inicio';
import { Catalogo } from './paginas/catalogo/catalogo';
import { Pedidos } from './paginas/pedidos/pedidos';
import { Perfil } from './paginas/perfil/perfil';
import { Registro } from './paginas/registro/registro';

/**
 * MsalGuard protege las rutas: si no hay sesion activa redirige a Microsoft
 * antes de que el componente llegue a construirse. Inicio y Registro quedan
 * abiertas, porque un usuario que aun no tiene cuenta debe poder llegar a ellas.
 */
export const routes: Routes = [
  { path: '', component: Inicio },
  { path: 'registro', component: Registro },
  { path: 'catalogo', component: Catalogo, canActivate: [MsalGuard] },
  { path: 'pedidos', component: Pedidos, canActivate: [MsalGuard] },
  { path: 'perfil', component: Perfil, canActivate: [MsalGuard] },
  { path: '**', redirectTo: '' }
];
