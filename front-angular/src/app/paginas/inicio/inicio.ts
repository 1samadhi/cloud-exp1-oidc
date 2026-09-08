import { Component, inject, signal } from '@angular/core';
import { Api } from '../../servicios/api';

@Component({
  selector: 'app-inicio',
  templateUrl: './inicio.html'
})
export class Inicio {
  private api = inject(Api);
  readonly respuesta = signal<string>('');

  /** Llama al endpoint anonimo: comprueba el despliegue sin necesitar sesion. */
  probarPublico(): void {
    this.api.publico().subscribe({
      next: (r) => this.respuesta.set(JSON.stringify(r, null, 2)),
      error: (e) => this.respuesta.set(`Error ${e.status}`)
    });
  }
}
