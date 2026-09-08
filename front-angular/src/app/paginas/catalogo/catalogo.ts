import { Component, inject, signal, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Api, Producto } from '../../servicios/api';

@Component({
  selector: 'app-catalogo',
  imports: [DecimalPipe],
  templateUrl: './catalogo.html'
})
export class Catalogo implements OnInit {
  private api = inject(Api);
  readonly productos = signal<Producto[]>([]);
  readonly error = signal('');
  readonly mensaje = signal('');

  ngOnInit(): void {
    this.api.productos().subscribe({
      next: (p) => this.productos.set(p),
      error: (e) => this.error.set(`No se pudo cargar el catalogo (HTTP ${e.status})`)
    });
  }

  pedir(p: Producto): void {
    this.mensaje.set('');
    this.api.crearPedido(p.id, 1).subscribe({
      next: (pedido) => this.mensaje.set(`Pedido ${pedido.id} creado: ${p.nombre}`),
      error: (e) => this.error.set(
        e.status === 403
          ? 'Tu token no incluye el permiso pedidos.escribir'
          : `No se pudo crear el pedido (HTTP ${e.status})`)
    });
  }
}
