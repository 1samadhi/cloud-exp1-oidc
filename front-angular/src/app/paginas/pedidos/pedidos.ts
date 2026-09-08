import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Api, Pedido } from '../../servicios/api';

@Component({
  selector: 'app-pedidos',
  imports: [FormsModule, DatePipe],
  templateUrl: './pedidos.html'
})
export class Pedidos implements OnInit {
  private api = inject(Api);
  readonly pedidos = signal<Pedido[]>([]);
  readonly error = signal('');
  productoId = 1;
  cantidad = 1;

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.api.pedidos().subscribe({
      next: (p) => this.pedidos.set(p),
      error: (e) => this.error.set(`No se pudieron cargar los pedidos (HTTP ${e.status})`)
    });
  }

  crear(): void {
    this.error.set('');
    this.api.crearPedido(this.productoId, this.cantidad).subscribe({
      next: () => this.cargar(),
      error: (e) => this.error.set(e.error?.error ?? `Error HTTP ${e.status}`)
    });
  }
}
