package com.celfinder.Controller;

import com.celfinder.Model.Celular;
import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Notificacion;
import com.celfinder.Model.Reseña;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.AdminService;
import com.celfinder.Procesos.UsuarioService;
import com.celfinder.Procesos.VentaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    private static final Logger logger = LoggerFactory.getLogger(VentaController.class);

    private final VentaService ventaService;
    private final UsuarioService usuarioService;
    private final AdminService adminService;

    @Autowired
    public VentaController(VentaService ventaService, UsuarioService usuarioService, AdminService adminService) {
        this.ventaService = ventaService;
        this.usuarioService = usuarioService;
        this.adminService = adminService;
    }

    @GetMapping("/publicar")
    public String mostrarFormularioPublicacion(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || !hasRequiredRole(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Solo los vendedores o administradores pueden publicar teléfonos.");
            return "redirect:/ventas/listar";
        }
        Celular celular = new Celular();
        model.addAttribute("telefono", celular);
        return "publicarTelefono";
    }

    @PostMapping("/publicar")
    public String publicarTelefono(@ModelAttribute Celular celular,
                                  @RequestParam("imagen") MultipartFile imagen,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (celular.getNombre() == null || celular.getNombre().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre del teléfono es obligatorio.");
            }
            if (celular.getPrecio() < 0) {
                throw new IllegalArgumentException("El precio no puede ser negativo.");
            }
            if (celular.getEstado() == null || celular.getEstado().trim().isEmpty()) {
                throw new IllegalArgumentException("El estado del teléfono es obligatorio.");
            }

            if (authentication == null || !authentication.isAuthenticated() || !hasRequiredRole(authentication)) {
                throw new IllegalStateException("Solo los vendedores o administradores pueden publicar teléfonos.");
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            celular.setVendedorId(usuario.getId());
            celular.setFechaPublicacion(LocalDateTime.now()); // Set publication date

            if (!imagen.isEmpty()) {
                String imagenBase64 = Base64.getEncoder().encodeToString(imagen.getBytes());
                celular.setImagenBase64(imagenBase64);
            }

            ventaService.publicarTelefono(celular);
            redirectAttributes.addFlashAttribute("mensaje", "Teléfono publicado correctamente.");
            return "redirect:/ventas/listar";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la imagen: " + e.getMessage());
            return "redirect:/ventas/publicar";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/publicar";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error inesperado al publicar el teléfono: " + e.getMessage());
            return "redirect:/ventas/publicar";
        }
    }

    @GetMapping("/listar")
    public String listarTelefonos(@RequestParam(defaultValue = "0") int page, Model model, Authentication authentication) {
        try {
            List<Celular> allTelefonos = ventaService.obtenerTelefonosEnVenta();
            allTelefonos.sort(Comparator.comparing(Celular::getFechaPublicacion, Comparator.reverseOrder()));
            int totalItems = allTelefonos.size();
            int itemsPerPage = 20;
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, totalItems);
            List<Celular> telefonos = allTelefonos.subList(start, end);

            model.addAttribute("telefonos", telefonos != null ? telefonos : new ArrayList<>());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);

            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Usuario) {
                Usuario usuario = (Usuario) authentication.getPrincipal();
                Set<String> roles = usuario.getRoles();
                model.addAttribute("userRoles", roles);
            } else {
                model.addAttribute("userRoles", new ArrayList<>());
            }

            return "listarTelefonos";
        } catch (Exception e) {
            model.addAttribute("error", "Error al listar los teléfonos: " + e.getMessage());
            return "listarTelefonos";
        }
    }

    @GetMapping("/buscar")
    public String buscarTelefonos(@RequestParam(required = false) String nombre,
                                 @RequestParam(required = false) String estado,
                                 @RequestParam(required = false) Double precioMin,
                                 @RequestParam(required = false) Double precioMax,
                                 @RequestParam(defaultValue = "0") int page,
                                 Model model) {
        try {
            List<Celular> allTelefonos = ventaService.buscarTelefonos(nombre, estado, precioMin, precioMax);
            allTelefonos.sort(Comparator.comparing(Celular::getFechaPublicacion, Comparator.reverseOrder()));
            int totalItems = allTelefonos.size();
            int itemsPerPage = 20;
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, totalItems);
            List<Celular> telefonos = allTelefonos.subList(start, end);

            model.addAttribute("telefonos", telefonos != null ? telefonos : new ArrayList<>());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("nombre", nombre);
            model.addAttribute("estado", estado);
            model.addAttribute("precioMin", precioMin);
            model.addAttribute("precioMax", precioMax);
            return "listarTelefonos";
        } catch (Exception e) {
            model.addAttribute("error", "Error al buscar los teléfonos: " + e.getMessage());
            model.addAttribute("telefonos", new ArrayList<>());
            return "listarTelefonos";
        }
    }

    @GetMapping("/seleccionar-comparacion/{id}")
    public String seleccionarTelefonoParaComparar(@PathVariable String id,
                                                 @RequestParam(required = false) String nombre,
                                                 @RequestParam(required = false) String estado,
                                                 @RequestParam(required = false) Double precioMin,
                                                 @RequestParam(required = false) Double precioMax,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 Model model,
                                                 Authentication authentication,
                                                 RedirectAttributes redirectAttributes) {
        try {
            Celular telefonoActual = ventaService.obtenerTelefonoPorId(id);
            if (telefonoActual == null) {
                redirectAttributes.addFlashAttribute("error", "El teléfono no existe.");
                return "redirect:/ventas/listar";
            }

            List<Celular> allTelefonos = ventaService.buscarTelefonosExcluyendoId(nombre, estado, precioMin, precioMax, id);
            int totalItems = allTelefonos.size();
            int itemsPerPage = 20;
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            int start = page * itemsPerPage;
            int end = Math.min(start + itemsPerPage, totalItems);
            List<Celular> telefonos = allTelefonos.subList(start, end);

            model.addAttribute("telefono", telefonoActual);
            model.addAttribute("telefonoActual", telefonoActual);
            model.addAttribute("telefonos", telefonos != null ? telefonos : new ArrayList<>());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("nombre", nombre);
            model.addAttribute("estado", estado);
            model.addAttribute("precioMin", precioMin);
            model.addAttribute("precioMax", precioMax);

            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Usuario) {
                Usuario usuario = (Usuario) authentication.getPrincipal();
                Set<String> roles = usuario.getRoles();
                model.addAttribute("userRoles", roles);
                model.addAttribute("userId", usuario.getId());
            } else {
                model.addAttribute("userRoles", new ArrayList<>());
                model.addAttribute("userId", null);
            }

            return "seleccionarComparacion";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar la selección de comparación: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    @GetMapping("/detalle/{id}")
    public String mostrarDetalleTelefono(@PathVariable String id, Model model, Authentication authentication) {
        try {
            Celular telefono = ventaService.obtenerTelefonoPorId(id);
            if (telefono == null) {
                model.addAttribute("error", "El teléfono no existe.");
                return "redirect:/ventas/listar";
            }
            model.addAttribute("telefono", telefono);
            List<Celular> otrosTelefonos = ventaService.obtenerTelefonosEnVenta();
            model.addAttribute("otrosTelefonos", otrosTelefonos != null ? otrosTelefonos : new ArrayList<>());
            List<Celular> similares = ventaService.obtenerCelularesSimilares(id, telefono.getNombre());
            if (similares != null && !similares.isEmpty()) {
                Collections.shuffle(similares);
                int limit = Math.min(similares.size(), 5);
                similares = similares.subList(0, limit);
            }
            Map<String, String> nombresVendedores = new HashMap<>();
            for (Celular similar : similares) {
                String nombreVendedor = usuarioService.obtenerNombreUsuarioPorId(similar.getVendedorId());
                nombresVendedores.put(similar.getId(), nombreVendedor != null ? nombreVendedor : "Desconocido");
            }
            model.addAttribute("similares", similares != null ? similares : new ArrayList<>());
            model.addAttribute("nombresVendedores", nombresVendedores);
            List<Reseña> reseñas = ventaService.obtenerReseñasPorCelular(id);
            model.addAttribute("reseñas", reseñas != null ? reseñas : new ArrayList<>());

            String nombreVendedor = usuarioService.obtenerNombreUsuarioPorId(telefono.getVendedorId());
            model.addAttribute("nombreVendedor", nombreVendedor);

            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Usuario) {
                Usuario usuario = (Usuario) authentication.getPrincipal();
                Set<String> roles = usuario.getRoles();
                model.addAttribute("userRoles", roles);
                model.addAttribute("userId", usuario.getId());
            } else {
                model.addAttribute("userRoles", new ArrayList<>());
                model.addAttribute("userId", null);
            }

            return "detalleTelefono";
        } catch (Exception e) {
            model.addAttribute("error", "Error al mostrar los detalles del teléfono: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(@PathVariable String id, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("error", "Debes estar autenticado para editar un teléfono.");
                return "redirect:/ventas/detalle/" + id;
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            Celular celular = ventaService.obtenerTelefonoPorId(id);
            if (celular == null) {
                redirectAttributes.addFlashAttribute("error", "El teléfono no existe.");
                return "redirect:/ventas/listar";
            }
            if (!usuario.getId().equals(celular.getVendedorId()) && !usuario.getRoles().contains("ROLE_ADMIN")) {
                redirectAttributes.addFlashAttribute("error", "Solo el vendedor o un administrador pueden editar este teléfono.");
                return "redirect:/ventas/detalle/" + id;
            }
            model.addAttribute("telefono", celular);
            return "editarTelefono";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar el formulario de edición: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    @PostMapping("/editar/{id}")
    public String editarTelefono(
        @PathVariable String id,
        @ModelAttribute("telefono") Celular telefono,
        @RequestParam(value = "imagen", required = false) MultipartFile imagen,
        Authentication authentication,
        RedirectAttributes redirectAttributes
    ) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("error", "Debes estar autenticado para editar un teléfono.");
                return "redirect:/ventas/detalle/" + id;
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            Celular existingTelefono = ventaService.obtenerTelefonoPorId(id);
            if (existingTelefono == null) {
                redirectAttributes.addFlashAttribute("error", "El teléfono no existe.");
                return "redirect:/ventas/listar";
            }
            if (!usuario.getId().equals(existingTelefono.getVendedorId()) && !usuario.getRoles().contains("ROLE_ADMIN")) {
                redirectAttributes.addFlashAttribute("error", "Solo el vendedor o un administrador pueden editar este teléfono.");
                return "redirect:/ventas/detalle/" + id;
            }

            existingTelefono.setNombre(telefono.getNombre());
            existingTelefono.setPrecio(telefono.getPrecio());
            existingTelefono.setEstado(telefono.getEstado());
            existingTelefono.setDescripcion(telefono.getDescripcion());
            existingTelefono.setGhz(telefono.getGhz());
            existingTelefono.setCamara(telefono.getCamara());
            existingTelefono.setRam(telefono.getRam());
            existingTelefono.setBateria(telefono.getBateria());
            existingTelefono.setAlmacenamiento(telefono.getAlmacenamiento());
            existingTelefono.setNombreCpu(telefono.getNombreCpu());

            if (imagen != null && !imagen.isEmpty()) {
                String imagenBase64 = Base64.getEncoder().encodeToString(imagen.getBytes());
                existingTelefono.setImagenBase64(imagenBase64);
            }

            ventaService.actualizarTelefono(existingTelefono);
            redirectAttributes.addFlashAttribute("mensaje", "Teléfono actualizado exitosamente.");
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al editar el teléfono: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    @PostMapping("/comprar/{id}")
    public String iniciarCompra(@PathVariable String id, Authentication authentication, RedirectAttributes redirectAttributes, Model model) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException("Debes estar autenticado para realizar una compra.");
            }
            Celular celular = ventaService.obtenerTelefonoPorId(id);
            if (celular == null || !celular.getEstadoVenta().equals("disponible")) {
                throw new IllegalStateException("El teléfono no está disponible para la compra.");
            }

            Usuario comprador = (Usuario) authentication.getPrincipal();
            if (comprador.getId().equals(celular.getVendedorId())) {
                throw new IllegalStateException("No puedes comprar tu propio teléfono.");
            }

            Solicitud solicitud = new Solicitud();
            solicitud.setCelularId(celular.getId());
            solicitud.setNombreCelular(celular.getNombre());
            solicitud.setUsuarioId(comprador.getId());
            solicitud.setNombreComprador(usuarioService.obtenerNombreUsuarioPorId(comprador.getId()));
            solicitud.setVendedorId(celular.getVendedorId());

            model.addAttribute("solicitud", solicitud);
            model.addAttribute("celularId", id);
            return "formularioCompra";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error inesperado al iniciar la compra: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    @PostMapping("/procesar-compra")
    public String procesarCompra(@ModelAttribute Solicitud solicitud, 
                                @RequestParam String celularId,
                                Authentication authentication, 
                                RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException("Debes estar autenticado para procesar la compra.");
            }
            Usuario comprador = (Usuario) authentication.getPrincipal();
            Celular celular = ventaService.obtenerTelefonoPorId(celularId);
            if (celular == null || !celular.getEstadoVenta().equals("disponible")) {
                throw new IllegalStateException("El teléfono no está disponible para la compra.");
            }

            if (solicitud.getDireccionComprador() == null || solicitud.getDireccionComprador().trim().isEmpty()) {
                throw new IllegalArgumentException("La dirección es obligatoria.");
            }
            if (solicitud.getCorreoComprador() == null || !solicitud.getCorreoComprador().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new IllegalArgumentException("El correo electrónico no es válido.");
            }
            if (solicitud.getNumeroContactoComprador() == null || !solicitud.getNumeroContactoComprador().matches("\\d{10}")) {
                throw new IllegalArgumentException("El número de contacto debe tener 10 dígitos.");
            }

            solicitud.setUsuarioId(comprador.getId());
            solicitud.setNombreComprador(usuarioService.obtenerNombreUsuarioPorId(comprador.getId()));
            solicitud.setVendedorId(celular.getVendedorId());
            solicitud.setCelularId(celularId);
            solicitud.setNombreCelular(celular.getNombre());

            ventaService.crearSolicitudCompra(solicitud);
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud de compra enviada correctamente. El vendedor revisará tu solicitud.");
            return "redirect:/ventas/confirmacion-compra";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/comprar/" + celularId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error inesperado al procesar la compra: " + e.getMessage());
            return "redirect:/ventas/comprar/" + celularId;
        }
    }

    @GetMapping("/confirmacion-compra")
    public String mostrarConfirmacionCompra(Model model) {
        return "confirmacionCompra";
    }

    @GetMapping("/gestionar-solicitudes")
    public String gestionarSolicitudes(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("error", "Debes estar autenticado para gestionar solicitudes.");
                return "redirect:/ventas/listar";
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            List<Solicitud> solicitudes = ventaService.obtenerSolicitudesPorVendedor(usuario.getId());
            model.addAttribute("solicitudes", solicitudes != null ? solicitudes : new ArrayList<>());
            return "gestionarSolicitudes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar las solicitudes: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    @PostMapping("/gestionar-solicitud/{id}")
    public String gestionarSolicitud(@PathVariable String id, 
                                    @RequestParam String accion, 
                                    @RequestParam(required = false) String descripcionVendedor, 
                                    Authentication authentication, 
                                    RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException("Debes estar autenticado para gestionar solicitudes.");
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            Solicitud solicitud = ventaService.obtenerSolicitudPorId(id);
            if (solicitud == null || !solicitud.getTipoSolicitud().equals("compra")) {
                throw new IllegalArgumentException("La solicitud no existe o no es una solicitud de compra.");
            }
            if (!usuario.getId().equals(solicitud.getVendedorId())) {
                throw new IllegalStateException("Solo el vendedor asignado puede gestionar esta solicitud.");
            }

            ventaService.gestionarSolicitudCompra(id, accion, descripcionVendedor);
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud " + (accion.equals("autorizar") ? "autorizada" : "rechazada") + " correctamente.");
            return "redirect:/ventas/gestionar-solicitudes";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/gestionar-solicitudes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al gestionar la solicitud: " + e.getMessage());
            return "redirect:/ventas/gestionar-solicitudes";
        }
    }

    @GetMapping("/historial-compras")
    public String mostrarHistorialCompras(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("error", "Debes estar autenticado para ver tu historial de compras.");
                return "redirect:/ventas/listar";
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            List<Solicitud> allHistorial = ventaService.obtenerHistorialCompras(usuario.getId());
            int totalItems = allHistorial.size();
            int totalPages = (int) Math.ceil((double) totalItems / size);
            int start = page * size;
            int end = Math.min(start + size, totalItems);
            List<Solicitud> historial = totalItems > 0 ? allHistorial.subList(start, end) : new ArrayList<>();

            model.addAttribute("historial", historial);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("pageSize", size);
            return "historialCompras";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar el historial de compras: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    @GetMapping("/historial-ventas")
    public String mostrarHistorialVentas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("error", "Debes estar autenticado para ver tu historial de ventas.");
                return "redirect:/ventas/listar";
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            List<Solicitud> allHistorial = ventaService.obtenerHistorialVentas(usuario.getId());
            int totalItems = allHistorial.size();
            int totalPages = (int) Math.ceil((double) totalItems / size);
            int start = page * size;
            int end = Math.min(start + size, totalItems);
            List<Solicitud> historial = totalItems > 0 ? allHistorial.subList(start, end) : new ArrayList<>();

            model.addAttribute("historial", historial);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("pageSize", size);
            return "historialVentas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar el historial de ventas: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    @GetMapping("/historial-solicitudes-vendedor")
    public String mostrarHistorialSolicitudesVendedor(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("error", "Debes estar autenticado para ver tu historial de solicitudes de vendedor.");
                return "redirect:/ventas/listar";
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            List<Solicitud> historial = adminService.obtenerHistorialSolicitudesVendedor(usuario.getId());
            model.addAttribute("solicitudes", historial != null ? historial : new ArrayList<>());
            return "historialSolicitudesVendedor";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar el historial de solicitudes: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    @GetMapping("/notificaciones")
    public String mostrarNotificaciones(Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                redirectAttributes.addFlashAttribute("error", "Debes estar autenticado para ver tus notificaciones.");
                return "redirect:/ventas/listar";
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            List<Notificacion> notificaciones = adminService.obtenerNotificacionesPorUsuario(usuario.getId());
            model.addAttribute("notificaciones", notificaciones != null ? notificaciones : new ArrayList<>());
            return "notificaciones";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar las notificaciones: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    @PostMapping("/reseña/{id}")
    public String guardarReseña(@PathVariable String id, @RequestParam String comentario, 
                               Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException("Debes estar autenticado para dejar una reseña.");
            }
            Celular celular = ventaService.obtenerTelefonoPorId(id);
            if (celular == null) {
                throw new IllegalStateException("El teléfono no existe.");
            }
            if (comentario == null || comentario.trim().isEmpty()) {
                throw new IllegalArgumentException("La reseña no puede estar vacía.");
            }

            Usuario usuario = (Usuario) authentication.getPrincipal();
            Reseña reseña = new Reseña();
            reseña.setCelularId(id);
            reseña.setUsuarioId(usuario.getId());
            reseña.setNombreUsuario(usuarioService.obtenerNombreUsuarioPorId(usuario.getId()));
            reseña.setComentario(comentario);

            ventaService.guardarReseña(reseña);
            redirectAttributes.addFlashAttribute("mensaje", "Reseña enviada correctamente.");
            return "redirect:/ventas/detalle/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al enviar la reseña: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    private boolean hasRequiredRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (authentication.getPrincipal() instanceof Usuario) {
            Usuario usuario = (Usuario) authentication.getPrincipal();
            return usuario.getRoles().contains("ROLE_VENDEDOR") || usuario.getRoles().contains("ROLE_ADMIN");
        }
        return false;
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarTelefono(@PathVariable String id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException("Debes estar autenticado para eliminar un teléfono.");
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            Celular celular = ventaService.obtenerTelefonoPorId(id);
            if (celular == null) {
                throw new IllegalStateException("El teléfono no existe.");
            }
            if (!usuario.getId().equals(celular.getVendedorId()) && !usuario.getRoles().contains("ROLE_ADMIN")) {
                throw new IllegalStateException("Solo el vendedor o un administrador pueden eliminar este teléfono.");
            }

            ventaService.eliminarTelefono(id);
            redirectAttributes.addFlashAttribute("mensaje", "Teléfono eliminado correctamente.");
            return "redirect:/ventas/listar";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el teléfono: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    @GetMapping("/comparar/{id}")
    public String compararConOtro(@PathVariable String id, @RequestParam String idOtroTelefono, Model model, RedirectAttributes redirectAttributes) {
        try {
            List<String> resultados = ventaService.compararConOtroTelefono(id, idOtroTelefono);
            model.addAttribute("resultados", resultados);
            model.addAttribute("telefono1", ventaService.obtenerTelefonoPorId(id));
            model.addAttribute("telefono2", ventaService.obtenerTelefonoPorId(idOtroTelefono));
            return "resultadoComparacion";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al realizar la comparación: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    @GetMapping("/comparar-media/{id}")
    public String compararConMedia(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Processing compararConMedia for ID: {}", id);
        try {
            List<String> resultados = ventaService.compararConMedia(id);
            model.addAttribute("resultados", resultados);
            model.addAttribute("telefono", ventaService.obtenerTelefonoPorId(id));
            logger.info("Returning template: resultadomedia");
            return "resultadomedia";
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            logger.error("Error during media comparison for ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al realizar la comparación con la media: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }
}