package com.celfinder.Controller;

import com.celfinder.Procesos.Servicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TelefonoVentaController {

    private final Servicio generatorService;

    @Autowired
    public TelefonoVentaController(Servicio generatorService) {
        this.generatorService = generatorService;
    }

    @GetMapping("/telefonos-venta")
    public String mostrarPagina() {
        return "p1"; 
    }

    @GetMapping("/api/generate-telefonos-venta")
    @ResponseBody
    public String generateTelefonosVenta() {
        return generatorService.generateRecords(); 
    }
}
