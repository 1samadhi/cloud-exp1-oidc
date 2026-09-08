import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface Producto {
  id: number;
  nombre: string;
  precio: number;
}

export interface Pedido {
  id: number;
  cliente: string;
  productoId: number;
  cantidad: number;
  creado: string;
}

export interface NuevoUsuario {
  nombre: string;
  usuario: string;
  password: string;
}

/**
 * Cliente de la API.
 *
 * No aparece el header Authorization en ninguna parte: lo agrega el
 * MsalInterceptor segun el protectedResourceMap. Si un endpoint empieza a
 * devolver 401, el problema esta en ese mapa y no aqui.
 */
@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);
  private base = environment.apiBase;

  /** Sin token: sirve para comprobar que el despliegue responde. */
  publico(): Observable<Record<string, string>> {
    return this.http.get<Record<string, string>>(`${this.base}/v1/public`);
  }

  productos(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.base}/v1/productos`);
  }

  producto(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.base}/v1/productos/${id}`);
  }

  quienSoy(): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.base}/v1/productos/quien-soy`);
  }

  pedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.base}/v1/pedidos`);
  }

  crearPedido(productoId: number, cantidad: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.base}/v1/pedidos`, { productoId, cantidad });
  }

  /** Anonimo a proposito: quien se registra todavia no tiene cuenta. */
  registrar(datos: NuevoUsuario): Observable<Record<string, string>> {
    return this.http.post<Record<string, string>>(`${this.base}/auth/registro`, datos);
  }
}
