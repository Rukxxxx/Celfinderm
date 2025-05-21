package com.celfinder.Procesos;

import com.celfinder.Model.Celular;
import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Notificacion;
import com.celfinder.Model.Reseña;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VentaService {

    private final MongoTemplate mongoTemplate;
    private final ComparadorCelular comparadorCelular;
    private final ComparadorMedia comparadorMedia;
    private final UsuarioService usuarioService;

    @Autowired
    public VentaService(MongoTemplate mongoTemplate, ComparadorCelular comparadorCelular, 
                        ComparadorMedia comparadorMedia, UsuarioService usuarioService) {
        this.mongoTemplate = mongoTemplate;
        this.comparadorCelular = comparadorCelular;
        this.comparadorMedia = comparadorMedia;
        this.usuarioService = usuarioService;
    }

    public void publicarTelefono(Celular celular) {
        celular.setFechaPublicacion(LocalDateTime.now());
        celular.setEstadoVenta("disponible");
        mongoTemplate.save(celular, "celulares");
    }

    public void actualizarTelefono(Celular celular) {
        if (celular.getId() == null) {
            throw new IllegalArgumentException("El ID del teléfono es obligatorio para actualizar.");
        }
        Celular existingCelular = mongoTemplate.findById(celular.getId(), Celular.class, "celulares");
        if (existingCelular == null) {
            throw new IllegalStateException("El teléfono no existe.");
        }
        celular.setFechaPublicacion(existingCelular.getFechaPublicacion());
        celular.setEstadoVenta(existingCelular.getEstadoVenta());
        celular.setVendedorId(existingCelular.getVendedorId());
        mongoTemplate.save(celular, "celulares");
    }

    public List<Celular> obtenerTelefonosEnVenta() {
        Query query = new Query(Criteria.where("estadoVenta").is("disponible"));
        return mongoTemplate.find(query, Celular.class, "celulares");
    }

    public Celular obtenerTelefonoPorId(String id) {
        return mongoTemplate.findById(id, Celular.class, "celulares");
    }

    public Solicitud obtenerSolicitudPorId(String id) {
        return mongoTemplate.findById(id, Solicitud.class, "solicitudes");
    }

    public List<Celular> buscarTelefonos(String nombre, String estado, Double precioMin, Double precioMax) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (nombre != null && !nombre.isEmpty()) {
            criteriaList.add(Criteria.where("nombre").regex(nombre, "i"));
        }
        if (estado != null && !estado.isEmpty()) {
            criteriaList.add(Criteria.where("estado").is(estado));
        }
        if (precioMin != null) {
            criteriaList.add(Criteria.where("precio").gte(precioMin));
        }
        if (precioMax != null) {
            criteriaList.add(Criteria.where("precio").lte(precioMax));
        }
        criteriaList.add(Criteria.where("estadoVenta").is("disponible"));

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Celular.class, "celulares");
    }

    public List<Celular> buscarTelefonosExcluyendoId(String nombre, String estado, Double precioMin, Double precioMax, String idExcluir) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (nombre != null && !nombre.isEmpty()) {
            criteriaList.add(Criteria.where("nombre").regex(nombre, "i"));
        }
        if (estado != null && !estado.isEmpty()) {
            criteriaList.add(Criteria.where("estado").is(estado));
        }
        if (precioMin != null) {
            criteriaList.add(Criteria.where("precio").gte(precioMin));
        }
        if (precioMax != null) {
            criteriaList.add(Criteria.where("precio").lte(precioMax));
        }
        criteriaList.add(Criteria.where("estadoVenta").is("disponible"));
        criteriaList.add(Criteria.where("id").ne(idExcluir));

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Celular.class, "celulares");
    }

    public void crearSolicitudCompra(Solicitud solicitud) {
        Query query = new Query(Criteria.where("celularId").is(solicitud.getCelularId())
                .and("estado").is("pendiente")
                .and("tipoSolicitud").is("compra"));
        if (mongoTemplate.exists(query, Solicitud.class, "solicitudes")) {
            throw new IllegalStateException("Ya existe una solicitud pendiente para este celular.");
        }

        Celular celular = obtenerTelefonoPorId(solicitud.getCelularId());
        if (celular == null || !celular.getEstadoVenta().equals("disponible")) {
            throw new IllegalStateException("El teléfono no está disponible para la compra.");
        }
        if (!celular.getVendedorId().equals(solicitud.getVendedorId())) {
            throw new IllegalStateException("El vendedor especificado no coincide con el vendedor del teléfono.");
        }

        solicitud.setTipoSolicitud("compra");
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setEstado("pendiente");
        mongoTemplate.save(solicitud, "solicitudes");
    }

    public List<Solicitud> obtenerSolicitudesPorVendedor(String vendedorId) {
        Query query = new Query(Criteria.where("vendedorId").is(vendedorId)
                .and("estado").is("pendiente")
                .and("tipoSolicitud").is("compra"));
        return mongoTemplate.find(query, Solicitud.class, "solicitudes");
    }

    public List<Solicitud> obtenerHistorialCompras(String compradorId) {
        Query query = new Query(Criteria.where("usuarioId").is(compradorId)
                .and("tipoSolicitud").is("compra"));
        return mongoTemplate.find(query, Solicitud.class, "solicitudes");
    }

    public List<Solicitud> obtenerHistorialVentas(String vendedorId) {
        Query query = new Query(Criteria.where("vendedorId").is(vendedorId)
                .and("tipoSolicitud").is("compra"));
        return mongoTemplate.find(query, Solicitud.class, "solicitudes");
    }

    public void gestionarSolicitudCompra(String solicitudId, String accion, String descripcionVendedor) {
        Solicitud solicitud = obtenerSolicitudPorId(solicitudId);
        if (solicitud == null || !solicitud.getTipoSolicitud().equals("compra")) {
            throw new IllegalArgumentException("La solicitud no existe o no es una solicitud de compra.");
        }

        if (!solicitud.getEstado().equals("pendiente")) {
            throw new IllegalStateException("La solicitud ya ha sido gestionada.");
        }

        solicitud.setEstado(accion.equals("autorizar") ? "autorizada" : "rechazada");
        solicitud.setDescripcionVendedor(descripcionVendedor != null ? descripcionVendedor : "");

        if (accion.equals("autorizar")) {
            Celular celular = obtenerTelefonoPorId(solicitud.getCelularId());
            if (celular != null && celular.getEstadoVenta().equals("disponible")) {
                celular.setEstadoVenta("vendido");
                mongoTemplate.save(celular, "celulares");
            } else {
                throw new IllegalStateException("El celular no está disponible para la venta.");
            }
        }

        mongoTemplate.save(solicitud, "solicitudes");

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(solicitud.getUsuarioId());
        notificacion.setMensaje(accion.equals("autorizar")
                ? "Su compra del teléfono '" + solicitud.getNombreCelular() + "' ha sido realizada con éxito."
                : "Su compra del teléfono '" + solicitud.getNombreCelular() + "' fue rechazada.");
        notificacion.setFecha(LocalDateTime.now());
        mongoTemplate.save(notificacion, "notificaciones");
    }

    public void eliminarTelefono(String id) {
        Celular celular = obtenerTelefonoPorId(id);
        if (celular != null) {
            Query query = new Query(Criteria.where("celularId").is(id)
                    .and("estado").is("pendiente")
                    .and("tipoSolicitud").is("compra"));
            if (mongoTemplate.exists(query, Solicitud.class, "solicitudes")) {
                throw new IllegalStateException("No se puede eliminar el teléfono porque tiene solicitudes pendientes.");
            }
            mongoTemplate.remove(celular, "celulares");
        } else {
            throw new IllegalArgumentException("El teléfono no existe.");
        }
    }

    public List<String> compararConOtroTelefono(String id, String idOtroTelefono) {
        Celular celular1 = obtenerTelefonoPorId(id);
        Celular celular2 = obtenerTelefonoPorId(idOtroTelefono);
        if (celular1 == null || celular2 == null) {
            throw new IllegalArgumentException("Uno o ambos teléfonos no existen.");
        }
        comparadorCelular.registrarCelulares(celular1, celular2);
        return comparadorCelular.getResultadosComparacion();
    }

    public List<String> compararConMedia(String id) {
        Celular celular = obtenerTelefonoPorId(id);
        if (celular == null) {
            throw new IllegalArgumentException("El teléfono no existe.");
        }
        comparadorMedia.guardarCelular(celular);
        return comparadorMedia.getResultadosComparacion();
    }

    public List<Celular> obtenerCelularesSimilares(String id, String nombre) {
        Query query = new Query(Criteria.where("nombre").regex("^" + nombre + "$", "i")
                .and("id").ne(id)
                .and("estadoVenta").is("disponible"));
        List<Celular> similares = mongoTemplate.find(query, Celular.class, "celulares");
        return similares;
    }

    public List<Reseña> obtenerReseñasPorCelular(String celularId) {
        Query query = new Query(Criteria.where("celularId").is(celularId));
        return mongoTemplate.find(query, Reseña.class, "reseñas");
    }

    public void guardarReseña(Reseña reseña) {
        reseña.setFecha(LocalDateTime.now());
        mongoTemplate.save(reseña, "reseñas");
    }
}