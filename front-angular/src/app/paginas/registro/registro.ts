import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../../servicios/api';

/**
 * Registro de usuarios en el tenant.
 *
 * El formulario no habla con Entra ID: manda los datos a ms-auth, que crea el
 * usuario contra Microsoft Graph usando su propio secreto de aplicacion. Una
 * SPA no podria hacerlo directamente, porque tendria que exponer ese secreto.
 */
@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink],
  templateUrl: './registro.html'
})
export class Registro {
  private api = inject(Api);

  nombre = '';
  usuario = '';
  password = '';

  readonly enviando = signal(false);
  readonly creado = signal('');
  readonly error = signal('');

  registrar(): void {
    this.error.set('');
    this.creado.set('');
    if (!this.nombre || !this.usuario || this.password.length < 8) {
      this.error.set('Completa los campos. La contrasenia necesita al menos 8 caracteres.');
      return;
    }
    this.enviando.set(true);
    this.api.registrar({ nombre: this.nombre, usuario: this.usuario, password: this.password })
      .subscribe({
        next: (r) => { this.creado.set(r['usuario'] ?? this.usuario); this.enviando.set(false); },
        error: (e) => { this.error.set(e.error?.error ?? `Error HTTP ${e.status}`); this.enviando.set(false); }
      });
  }
}
