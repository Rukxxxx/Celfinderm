package com.celfinder.Procesos;

import com.celfinder.Model.Celular;
import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Notificacion;
import com.celfinder.Model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public AdminService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Page<Usuario> obtenerTodosLosUsuarios(Pageable pageable) {
        Query query = new Query();
        long total = mongoTemplate.count(query, Usuario.class, "usuarios");
        query.with(pageable);
        List<Usuario> usuarios = mongoTemplate.find(query, Usuario.class, "usuarios");
        return new PageImpl<>(usuarios, pageable, total);
    }

    public Page<Celular> obtenerTodosLosTelefonos(Pageable pageable) {
        Query query = new Query();
        long total = mongoTemplate.count(query, Celular.class, "celulares");
        query.with(pageable);
        List<Celular> celulares = mongoTemplate.find(query, Celular.class, "celulares");
        return new PageImpl<>(celulares, pageable, total);
    }

    public Page<Celular> buscarTelefonos(String nombre, String estadoVenta, Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (nombre != null && !nombre.isEmpty()) {
            criteriaList.add(Criteria.where("nombre").regex(nombre, "i"));
        }
        if (estadoVenta != null && !estadoVenta.isEmpty()) {
            criteriaList.add(Criteria.where("estadoVenta").is(estadoVenta));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Celular.class, "celulares");
        query.with(pageable);
        List<Celular> celulares = mongoTemplate.find(query, Celular.class, "celulares");
        return new PageImpl<>(celulares, pageable, total);
    }

    public Usuario obtenerUsuarioPorId(String id) {
        return mongoTemplate.findById(id, Usuario.class, "usuarios");
    }

    public void asignarRol(String usuarioId, String rol) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if (!rol.equals("ROLE_VENDEDOR") && !rol.equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("Rol inválido. Use ROLE_VENDEDOR o ROLE_ADMIN.");
        }
        if (usuario.getRoles().contains(rol)) {
            throw new IllegalArgumentException("El usuario ya tiene el rol " + rol);
        }
        usuario.getRoles().add(rol);
        mongoTemplate.save(usuario, "usuarios");
    }

    public void removerRol(String usuarioId, String rol) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if (!rol.equals("ROLE_VENDEDOR") && !rol.equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("Rol inválido. Use ROLE_VENDEDOR o ROLE_ADMIN.");
        }
        if (rol.equals("ROLE_USER")) {
            throw new IllegalArgumentException("No se puede remover el rol ROLE_USER.");
        }
        if (!usuario.getRoles().contains(rol)) {
            throw new IllegalArgumentException("El usuario no tiene el rol " + rol);
        }
        usuario.getRoles().remove(rol);
        mongoTemplate.save(usuario, "usuarios");
    }

    public void desactivarUsuario(String usuarioId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if ("desactivada".equals(usuario.getEstadoCuenta())) {
            throw new IllegalStateException("El usuario ya está desactivado.");
        }
        usuario.setEstadoCuenta("desactivada");
        mongoTemplate.save(usuario, "usuarios");
    }

    public void reactivarUsuario(String usuarioId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if ("activa".equals(usuario.getEstadoCuenta())) {
            throw new IllegalStateException("El usuario ya está activo.");
        }
        usuario.setEstadoCuenta("activa");
        mongoTemplate.save(usuario, "usuarios");
    }

    public void eliminarUsuario(String usuarioId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if (!"desactivada".equals(usuario.getEstadoCuenta())) {
            throw new IllegalStateException("Solo se pueden eliminar usuarios desactivados.");
        }
        Query telefonosQuery = new Query(Criteria.where("vendedorId").is(usuarioId));
        if (mongoTemplate.exists(telefonosQuery, Celular.class, "celulares")) {
            throw new IllegalStateException("No se puede eliminar el usuario porque tiene teléfonos registrados.");
        }
        Query solicitudesQuery = new Query(Criteria.where("usuarioId").is(usuarioId)
                .orOperator(Criteria.where("vendedorId").is(usuarioId))
                .and("tipoSolicitud").in("compra", "vendedor"));
        if (mongoTemplate.exists(solicitudesQuery, Solicitud.class, "solicitudes")) {
            throw new IllegalStateException("No se puede eliminar el usuario porque tiene solicitudes asociadas.");
        }
        mongoTemplate.remove(usuario, "usuarios");
    }

    public void crearSolicitudVendedor(Solicitud solicitud) {
        Query query = new Query(Criteria.where("usuarioId").is(solicitud.getUsuarioId())
                .and("estado").is("pendiente")
                .and("tipoSolicitud").is("vendedor"));
        if (mongoTemplate.exists(query, Solicitud.class, "solicitudes")) {
            throw new IllegalStateException("Ya existe una solicitud pendiente para este usuario.");
        }
        solicitud.setTipoSolicitud("vendedor");
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setEstado("pendiente");
        mongoTemplate.save(solicitud, "solicitudes");
    }

    public List<Solicitud> obtenerSolicitudesVendedorPendientes() {
        Query query = new Query(Criteria.where("estado").is("pendiente")
                .and("tipoSolicitud").is("vendedor"));
        return mongoTemplate.find(query, Solicitud.class, "solicitudes");
    }

    public Solicitud obtenerSolicitudVendedorPorId(String id) {
        Query query = new Query(Criteria.where("_id").is(id)
                .and("tipoSolicitud").is("vendedor"));
        return mongoTemplate.findOne(query, Solicitud.class, "solicitudes");
    }

    public void gestionarSolicitudVendedor(String solicitudId, String accion, String comentarioAdmin) {
        Solicitud solicitud = obtenerSolicitudVendedorPorId(solicitudId);
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no existe.");
        }
        if (!solicitud.getEstado().equals("pendiente")) {
            throw new IllegalStateException("La solicitud ya ha sido gestionada.");
        }
        solicitud.setEstado(accion.equals("aprobar") ? "aprobada" : "rechazada");
        solicitud.setComentarioAdmin(comentarioAdmin != null ? comentarioAdmin : "");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        if (accion.equals("aprobar")) {
            asignarRol(solicitud.getUsuarioId(), "ROLE_VENDEDOR");
        }
        mongoTemplate.save(solicitud, "solicitudes");
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(solicitud.getUsuarioId());
        notificacion.setMensaje("Tu solicitud para ser vendedor ha sido " + (accion.equals("aprobar") ? "aprobada" : "rechazada") + ". Comentario: " + (comentarioAdmin != null ? comentarioAdmin : "Ninguno"));
        notificacion.setFecha(LocalDateTime.now());
        crearNotificacion(notificacion);
    }

    public List<Solicitud> obtenerHistorialSolicitudesVendedor(String usuarioId) {
        Query query = new Query(Criteria.where("usuarioId").is(usuarioId)
                .and("tipoSolicitud").is("vendedor"));
        return mongoTemplate.find(query, Solicitud.class, "solicitudes");
    }

    public void crearNotificacion(Notificacion notificacion) {
        notificacion.setFecha(LocalDateTime.now());
        mongoTemplate.save(notificacion, "notificaciones");
    }

    public void crearNotificacionParaTodos(String mensaje) {
        List<Usuario> usuarios = mongoTemplate.findAll(Usuario.class, "usuarios");
        if (usuarios.isEmpty()) {
            throw new IllegalStateException("No hay usuarios registrados.");
        }
        for (Usuario usuario : usuarios) {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(usuario.getId());
            notificacion.setMensaje(mensaje);
            notificacion.setFecha(LocalDateTime.now());
            mongoTemplate.save(notificacion, "notificaciones");
        }
    }

    public List<Notificacion> obtenerNotificacionesPorUsuario(String usuarioId) {
        Query query = new Query(Criteria.where("usuarioId").is(usuarioId));
        return mongoTemplate.find(query, Notificacion.class, "notificaciones");
    }
}