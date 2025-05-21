package com.celfinder.Controller;

import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.UsuarioService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "Register";
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        return "Login";
    }

    @PostMapping("/registrar")
    public String registrarUsuario(@ModelAttribute Usuario usuario,
                                 @RequestParam("email1") String email1,
                                 @RequestParam("email2") String email2,
                                 @RequestParam("fechaNacimiento") String fechaNacimiento,
                                 @RequestParam("ciudad") String ciudad,
                                 @RequestParam("departamento") String departamento,
                                 @RequestParam("telefono") String telefono,
                                 RedirectAttributes redirectAttributes) {
        if (!email1.equals(email2)) {
            redirectAttributes.addFlashAttribute("mensaje", "Los correos electrónicos no coinciden");
            return "redirect:/usuarios/registro";
        }

        if (usuarioService.nombreUsuarioExiste(usuario.getNombreUsuario())) {
            redirectAttributes.addFlashAttribute("mensaje", "El nombre de usuario ya está en uso");
            return "redirect:/usuarios/registro";
        }

        usuario.setEmail(email1);
        usuario.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
        usuario.setCiudad(ciudad);
        usuario.setDepartamento(departamento);
        usuario.setTelefono(telefono);

        usuarioService.registrarUsuario(usuario);
        redirectAttributes.addFlashAttribute("mensaje", "Usuario registrado correctamente");
        return "redirect:/usuarios/login";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/usuarios/login";
        }
        String nombreUsuario = principal.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorNombre(nombreUsuario);
        if (usuario == null) {
            return "redirect:/usuarios/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("userRoles", usuario.getRoles());
        return "perfil";
    }

    @GetMapping("/editar-perfil")
    public String mostrarEditarPerfil(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/usuarios/login";
        }
        String nombreUsuario = principal.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorNombre(nombreUsuario);
        if (usuario == null) {
            return "redirect:/usuarios/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("userRoles", usuario.getRoles()); // Add userRoles
        return "editarPerfil";
    }

    @PostMapping("/editar-perfil")
    public String guardarCambiosPerfil(@ModelAttribute("usuario") Usuario usuarioForm,
                                      @RequestParam("imagenPerfil") MultipartFile imagenPerfil,
                                      @RequestParam("imagenFondo") MultipartFile imagenFondo,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/usuarios/login";
        }
        String nombreUsuario = principal.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorNombre(nombreUsuario);
        if (usuario == null) {
            return "redirect:/usuarios/login";
        }

        // Update fields from form
        String email = usuarioForm.getEmail();
        String ciudad = usuarioForm.getCiudad();
        String departamento = usuarioForm.getDepartamento();
        String telefono = usuarioForm.getTelefono();

        // Handle profile image
        String imgPerfil = usuario.getImgperfil();
        if (!imagenPerfil.isEmpty()) {
            try {
                imgPerfil = Base64.getEncoder().encodeToString(imagenPerfil.getBytes());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("mensaje", "Error al procesar la imagen de perfil");
                return "redirect:/usuarios/editar-perfil";
            }
        }

        // Handle background image
        String fondoPerfil = usuario.getFondoperfil();
        if (!imagenFondo.isEmpty()) {
            try {
                fondoPerfil = Base64.getEncoder().encodeToString(imagenFondo.getBytes());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("mensaje", "Error al procesar la imagen de fondo");
                return "redirect:/usuarios/editar-perfil";
            }
        }

        // Update user profile
        usuarioService.actualizarPerfil(usuario, email, ciudad, departamento, telefono, imgPerfil, fondoPerfil);
        redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente");
        return "redirect:/usuarios/perfil";
    }
}