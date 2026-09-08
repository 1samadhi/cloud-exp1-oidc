import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MsalService, MsalBroadcastService, MSAL_GUARD_CONFIG, MsalGuardConfiguration } from '@azure/msal-angular';
import { EventMessage, EventType, InteractionStatus, AccountInfo } from '@azure/msal-browser';
import { Subject, filter, takeUntil, concatMap } from 'rxjs';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html'
})
export class App implements OnInit, OnDestroy {
  private msal = inject(MsalService);
  private eventos = inject(MsalBroadcastService);
  private configGuard = inject<MsalGuardConfiguration>(MSAL_GUARD_CONFIG);
  private destruido = new Subject<void>();

  readonly cuenta = signal<AccountInfo | null>(null);
  readonly cargando = signal(true);

  ngOnInit(): void {
    // msal-browser v3 en adelante exige initialize() antes de cualquier otra
    // llamada. handleRedirectObservable procesa el codigo de autorizacion con
    // que Microsoft nos devuelve tras el login.
    this.msal.initialize()
      .pipe(concatMap(() => this.msal.handleRedirectObservable()), takeUntil(this.destruido))
      .subscribe({
        next: () => { this.sincronizarCuenta(); this.cargando.set(false); },
        error: (e) => { console.error('Error al procesar la redireccion', e); this.cargando.set(false); }
      });

    // Cuando termina cualquier interaccion (login, logout, renovacion de token)
    // se vuelve a leer que cuenta esta activa.
    this.eventos.inProgress$
      .pipe(filter((estado) => estado === InteractionStatus.None), takeUntil(this.destruido))
      .subscribe(() => this.sincronizarCuenta());

    this.eventos.msalSubject$
      .pipe(filter((m: EventMessage) => m.eventType === EventType.LOGIN_SUCCESS), takeUntil(this.destruido))
      .subscribe(() => this.sincronizarCuenta());
  }

  ngOnDestroy(): void {
    this.destruido.next();
    this.destruido.complete();
  }

  /**
   * MSAL puede tener varias cuentas en cache. La "activa" es la que usan el
   * guard y el interceptor para pedir tokens, asi que hay que fijarla.
   */
  private sincronizarCuenta(): void {
    const cuentas = this.msal.instance.getAllAccounts();
    if (cuentas.length > 0 && !this.msal.instance.getActiveAccount()) {
      this.msal.instance.setActiveAccount(cuentas[0]);
    }
    this.cuenta.set(this.msal.instance.getActiveAccount());
  }

  entrar(): void {
    this.msal.loginRedirect({ ...this.configGuard.authRequest } as any);
  }

  salir(): void {
    this.msal.logoutRedirect();
  }
}
