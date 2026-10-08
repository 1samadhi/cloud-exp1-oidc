package cl.duoc.cloud.ordenes.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.cloud.ordenes.controller.dto.NuevaOrdenDTO;
import cl.duoc.cloud.ordenes.mensajeria.PublicadorEventos;
import cl.duoc.cloud.ordenes.mensajeria.evento.OrdenCreadaEvento;
import cl.duoc.cloud.ordenes.model.Orden;
import cl.duoc.cloud.ordenes.repository.OrdenRepository;

/**
 * Logica de la orden. No conoce RabbitMQ: habla con PublicadorEventos.
 *
 * El cliente nunca llega como parametro desde fuera, lo inyecta el controlador
 * a partir del claim sub del token, de modo que nadie puede crear ni leer
 * ordenes a nombre de otro.
 */
@Service
public class OrdenService {

    /** Error de negocio: produce un 400 con un mensaje util, no un 500. */
    public static class OrdenInvalida extends RuntimeException {
        public OrdenInvalida(String mensaje) {
            super(mensaje);
        }
    }

    private final OrdenRepository repositorio;
    private final CatalogoClient catalogo;
    private final PublicadorEventos publicador;

    public OrdenService(OrdenRepository repositorio, CatalogoClient catalogo, PublicadorEventos publicador) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.publicador = publicador;
    }

    public List<Orden> listarPorCliente(String cliente) {
        return repositorio.findByClienteOrderByCreadoDesc(cliente);
    }

    /** Solo para administradores: ignora el filtro por cliente. */
    public List<Orden> listarTodas() {
        return repositorio.findAllByOrderByCreadoDesc();
    }

    /**
     * Registra la orden y publica orden.creada.
     *
     * Guardar y publicar van en la misma transaccion: si el broker falla, la
     * orden no se guarda. Lo contrario dejaria ordenes que nadie procesa, con
     * stock sin descontar y sin boleta, y detectarlas despues exige comparar la
     * base contra el broker a mano.
     *
     * El precio lo pone el catalogo, no la peticion.
     */
    @Transactional
    public Orden crear(String cliente, String correo, NuevaOrdenDTO peticion) {
        Orden orden = new Orden(cliente, correo);
        List<OrdenCreadaEvento.LineaEvento> lineasEvento = new ArrayList<>();

        for (NuevaOrdenDTO.LineaDTO linea : peticion.items()) {
            var producto = catalogo.buscar(linea.productoId());
            if (producto == null) {
                throw new OrdenInvalida("El producto " + linea.productoId() + " no existe en el catalogo");
            }
            orden.agregarLinea(producto.id(), linea.cantidad(), producto.precio());
            lineasEvento.add(new OrdenCreadaEvento.LineaEvento(
                    producto.id(), linea.cantidad(), producto.precio()));
        }

        Orden guardada = repositorio.save(orden);

        publicador.publicarOrdenCreada(new OrdenCreadaEvento(
                guardada.getId(),
                guardada.getCliente(),
                guardada.getCorreo(),
                lineasEvento,
                guardada.getTotal(),
                guardada.getCreado()));

        return guardada;
    }
}
