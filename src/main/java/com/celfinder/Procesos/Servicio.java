package com.celfinder.Procesos;

import com.celfinder.Model.Celular;
import com.celfinder.Model.TelefonoVenta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class Servicio {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String[] VENDORS = {
        "68000108b9eb4a371763af24", // mihe
        "68050a92e243c74a8dc799db", // sasel
        "68052480a2f71c0d443788ad"  // ede
    };

    private static final String[] BRANDS = {"Samsung Galaxy", "Xiaomi Redmi", "Motorola Moto", "Huawei", "Oppo", "Vivo", "Realme", "Poco"};
    private static final String[] MODELS = {"A54", "Note 12", "G82", "P50", "Reno 8", "V23", "9 Pro", "X5 Pro", "M34", "13 Lite", "Edge 40", "GT Neo 3", "Find X5", "Y76", "M5"};
    private static final String[] CPUS = {"Exynos 1380", "Snapdragon 4 Gen 1", "Snapdragon 695", "Kirin 9000", "Dimensity 1300", "Dimensity 920", "Snapdragon 778G", "Exynos 1280", "Snapdragon 7 Gen 1", "Dimensity 8020", "Dimensity 8100", "Snapdragon 888", "Dimensity 700", "Helio G99"};
    private static final String[] DESCRIPTIONS = {
        "Cámara de alta calidad y buen rendimiento.",
        "Ideal para uso diario, gran relación calidad-precio.",
        "Buen estado, pantalla AMOLED y 5G.",
        "Cámara profesional y diseño premium.",
        "Carga rápida y diseño elegante.",
        "Buen estado, ideal para gaming ligero.",
        "Perfecto para selfies y videos.",
        "Alta resolución de cámara y gran rendimiento.",
        "Batería de larga duración y pantalla fluida.",
        "Ligero y con carga rápida de 67W.",
        "Pantalla OLED y carga inalámbrica.",
        "Carga ultrarrápida de 150W.",
        "Cámara Hasselblad y diseño premium.",
        "En buen estado, ideal para uso básico.",
        "Económico y eficiente para tareas diarias."
    };

    public String generateRecords() {
        try {
            List<Celular> celulares = new ArrayList<>();
            List<TelefonoVenta> telefonos = new ArrayList<>();
            LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2025, 5, 14, 23, 59);

            for (int i = 0; i < 1000; i++) {
                // Create Celular with all fields as per the test data format
                Celular celular = new Celular();
                celular.setId(UUID.randomUUID().toString());
                String brand = BRANDS[ThreadLocalRandom.current().nextInt(BRANDS.length)];
                String model = MODELS[ThreadLocalRandom.current().nextInt(MODELS.length)];
                celular.setNombre(brand + " " + model);
                celular.setGhz(ThreadLocalRandom.current().nextDouble(1.8, 3.2));
                celular.setCamara(new int[]{12, 16, 32, 48, 50, 64, 108}[ThreadLocalRandom.current().nextInt(7)]);
                celular.setRam(new int[]{4, 6, 8, 12, 16}[ThreadLocalRandom.current().nextInt(5)]);
                celular.setBateria(ThreadLocalRandom.current().nextInt(4000, 6001));
                celular.setAlmacenamiento(new int[]{64, 128, 256, 512}[ThreadLocalRandom.current().nextInt(4)]);
                celular.setSeleccion(1);
                celular.setNombreCpu(CPUS[ThreadLocalRandom.current().nextInt(CPUS.length)]);
                celular.setPrecio(ThreadLocalRandom.current().nextDouble(200000, 600001));
                celular.setEstado(ThreadLocalRandom.current().nextDouble() < 0.7 ? "nuevo" : "usado");
                celular.setDescripcion(DESCRIPTIONS[ThreadLocalRandom.current().nextInt(DESCRIPTIONS.length)]);
                celular.setVendedorId(VENDORS[i % VENDORS.length]);
                long randomMillis = ThreadLocalRandom.current().nextLong(
                    startDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    endDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                );
                celular.setFechaPublicacion(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(randomMillis), ZoneId.systemDefault()));
                celular.setEstadoVenta(ThreadLocalRandom.current().nextDouble() < 0.95 ? "disponible" : "vendido");

                // Add to celulares list for batch insert
                celulares.add(celular);

                // Create TelefonoVenta referencing the Celular
                TelefonoVenta telefono = new TelefonoVenta();
                telefono.setId(UUID.randomUUID().toString());
                telefono.setNombre(celular.getNombre());
                telefono.setPrecio(celular.getPrecio());
                telefono.setEstado(celular.getEstado());
                telefono.setDescripcion(celular.getDescripcion());
                telefono.setVendedorId(celular.getVendedorId());
                telefono.setFechaPublicacion(celular.getFechaPublicacion());
                telefono.setEstadoVenta(celular.getEstadoVenta());
                telefono.setEspecificaciones(celular);

                telefonos.add(telefono);

                // Batch insert every 500 records
                if (celulares.size() >= 500) {
                    mongoTemplate.insert(celulares, "celulares");
                    mongoTemplate.insert(telefonos, "telefonos_venta");
                    celulares.clear();
                    telefonos.clear();
                }
            }

            // Insert remaining records
            if (!celulares.isEmpty()) {
                mongoTemplate.insert(celulares, "celulares");
                mongoTemplate.insert(telefonos, "telefonos_venta");
            }

            return "Successfully generated and inserted 1,000 records into celulares and telefonos_venta.";
        } catch (Exception e) {
            return "Error generating records: " + e.getMessage();
        }
    }
}