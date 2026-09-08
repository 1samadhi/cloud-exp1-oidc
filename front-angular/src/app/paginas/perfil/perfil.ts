import { Component, inject, signal, OnInit } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { MsalService } from '@azure/msal-angular';
import { Api } from '../../servicios/api';
import { SCOPES } from '../../auth/msal.config';

@Component({
  selector: 'app-perfil',
  imports: [JsonPipe],
  templateUrl: './perfil.html'
})
export class Perfil implements OnInit {
  private msal = inject(MsalService);
  private api = inject(Api);

  readonly claimsIdToken = signal<Record<string, unknown> | null>(null);
  readonly claimsAccessToken = signal<Record<string, unknown> | null>(null);
  readonly vistoPorElBackend = signal<Record<string, unknown> | null>(null);
  readonly error = signal('');

  ngOnInit(): void {
    const cuenta = this.msal.instance.getActiveAccount();
    this.claimsIdToken.set((cuenta?.idTokenClaims as Record<string, unknown>) ?? null);

    // El access_token no se guarda en el componente: se pide a MSAL cuando hace
    // falta, y aqui solo se decodifica para mostrarlo como evidencia.
    this.msal.acquireTokenSilent({ scopes: [SCOPES.productosLeer] }).subscribe({
      next: (r) => this.claimsAccessToken.set(this.decodificar(r.accessToken)),
      error: (e) => this.error.set(`No se pudo obtener el access token: ${e.message}`)
    });

    this.api.quienSoy().subscribe({
      next: (r) => this.vistoPorElBackend.set(r),
      error: (e) => this.error.set(`El backend respondio HTTP ${e.status}`)
    });
  }

  /** Decodifica el payload sin verificar la firma: es solo para mostrarlo. */
  private decodificar(token: string): Record<string, unknown> | null {
    try {
      const carga = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(decodeURIComponent(escape(atob(carga))));
    } catch {
      return null;
    }
  }
}
