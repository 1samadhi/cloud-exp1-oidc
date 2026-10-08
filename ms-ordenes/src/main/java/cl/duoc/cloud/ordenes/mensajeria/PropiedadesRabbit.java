package cl.duoc.cloud.ordenes.mensajeria;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de la topologia de mensajeria, leidos de application.yml.
 *
 * Ningun nombre de exchange, cola o routing key se escribe a mano en el codigo:
 * un nombre repetido en dos clases se desincroniza en cuanto cambia uno, y el
 * sintoma es un mensaje que se publica correctamente y que nadie consume, sin
 * error en ningun log. Teniendolos aqui, el compilador obliga a pasar por un
 * unico punto.
 *
 * El prefijo es `pedidos360.rabbit`.
 */
@ConfigurationProperties(prefix = "pedidos360.rabbit")
public class PropiedadesRabbit {

    /** Exchange topic con los eventos del ciclo de vida de la orden. */
    private String exchangeOrdenes = "pedidos360.ordenes";

    /** Exchange direct para pedir un envio de correo puntual. */
    private String exchangeNotificaciones = "pedidos360.notificaciones";

    /** Dead letter exchange: recibe lo que los consumidores rechazan. */
    private String exchangeDlx = "pedidos360.dlx";

    /** Cola que consume ms-productos para descontar stock. */
    private String colaStock = "productos.stock";

    /** Cola que consume ms-notificaciones para enviar el correo. */
    private String colaCorreo = "notificaciones.correo";

    /** Cola que consume ms-facturacion para emitir la boleta. */
    private String colaFacturacion = "facturacion.ordenes";

    /** Routing key del evento de orden creada. */
    private String routingOrdenCreada = "orden.creada";

    /** Patron que captura cualquier evento de orden: orden.creada, orden.cancelada, ... */
    private String patronOrdenes = "orden.#";

    /** Sufijo con que se arma el nombre de cada dead letter queue. */
    private String sufijoDlq = ".dlq";

    /** Nombre de la DLQ de una cola, por ejemplo productos.stock -> productos.stock.dlq */
    public String dlqDe(String cola) {
        return cola + sufijoDlq;
    }

    public String getExchangeOrdenes() {
        return exchangeOrdenes;
    }

    public void setExchangeOrdenes(String exchangeOrdenes) {
        this.exchangeOrdenes = exchangeOrdenes;
    }

    public String getExchangeNotificaciones() {
        return exchangeNotificaciones;
    }

    public void setExchangeNotificaciones(String exchangeNotificaciones) {
        this.exchangeNotificaciones = exchangeNotificaciones;
    }

    public String getExchangeDlx() {
        return exchangeDlx;
    }

    public void setExchangeDlx(String exchangeDlx) {
        this.exchangeDlx = exchangeDlx;
    }

    public String getColaStock() {
        return colaStock;
    }

    public void setColaStock(String colaStock) {
        this.colaStock = colaStock;
    }

    public String getColaCorreo() {
        return colaCorreo;
    }

    public void setColaCorreo(String colaCorreo) {
        this.colaCorreo = colaCorreo;
    }

    public String getColaFacturacion() {
        return colaFacturacion;
    }

    public void setColaFacturacion(String colaFacturacion) {
        this.colaFacturacion = colaFacturacion;
    }

    public String getRoutingOrdenCreada() {
        return routingOrdenCreada;
    }

    public void setRoutingOrdenCreada(String routingOrdenCreada) {
        this.routingOrdenCreada = routingOrdenCreada;
    }

    public String getPatronOrdenes() {
        return patronOrdenes;
    }

    public void setPatronOrdenes(String patronOrdenes) {
        this.patronOrdenes = patronOrdenes;
    }

    public String getSufijoDlq() {
        return sufijoDlq;
    }

    public void setSufijoDlq(String sufijoDlq) {
        this.sufijoDlq = sufijoDlq;
    }
}
