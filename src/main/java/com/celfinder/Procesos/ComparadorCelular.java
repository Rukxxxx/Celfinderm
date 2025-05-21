package com.celfinder.Procesos;

import com.celfinder.Model.Celular;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ComparadorCelular {

    private Celular celular1;
    private Celular celular2;
    private List<String> resultadosComparacion;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ComparadorCelular(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.resultadosComparacion = new ArrayList<>();
    }

    public void registrarCelulares(Celular c1, Celular c2) {
        if (c1 == null || c2 == null) {
            throw new IllegalArgumentException("Los celulares no pueden ser nulos.");
        }
        this.celular1 = c1;
        this.celular2 = c2;
        this.resultadosComparacion.clear(); // Clear previous results
        compararCelulares();
        guardarCelulares();
    }

    private void compararCelulares() {
        compararCaracteristicas("Frecuencia (GHz)", celular1.getGhz(), celular2.getGhz());
        compararCaracteristicas("Cámara (MP)", celular1.getCamara(), celular2.getCamara());
        compararCaracteristicas("RAM (GB)", celular1.getRam(), celular2.getRam());
        compararCaracteristicas("Batería (mAh)", celular1.getBateria(), celular2.getBateria());
        compararCaracteristicas("Almacenamiento (GB)", celular1.getAlmacenamiento(), celular2.getAlmacenamiento());
        determinarMejorDispositivo();
    }

    private void compararCaracteristicas(String nombre, double valor1, double valor2) {
        if (valor1 < valor2) {
            resultadosComparacion.add("La característica de " + nombre + " del dispositivo '" + celular1.getNombre() + "' es inferior al dispositivo '" + celular2.getNombre() + "'.");
        } else if (valor1 > valor2) {
            resultadosComparacion.add("La característica de " + nombre + " del dispositivo '" + celular1.getNombre() + "' es superior al dispositivo '" + celular2.getNombre() + "'.");
        } else {
            resultadosComparacion.add("Ambos dispositivos son iguales en " + nombre + ".");
        }
    }

    private void determinarMejorDispositivo() {
        int contador1 = 0;
        int contador2 = 0;

        if (celular1.getGhz() > celular2.getGhz()) contador1++;
        else if (celular1.getGhz() < celular2.getGhz()) contador2++;

        if (celular1.getCamara() > celular2.getCamara()) contador1++;
        else if (celular1.getCamara() < celular2.getCamara()) contador2++;

        if (celular1.getRam() > celular2.getRam()) contador1++;
        else if (celular1.getRam() < celular2.getRam()) contador2++;

        if (celular1.getBateria() > celular2.getBateria()) contador1++;
        else if (celular1.getBateria() < celular2.getBateria()) contador2++;

        if (celular1.getAlmacenamiento() > celular2.getAlmacenamiento()) contador1++;
        else if (celular1.getAlmacenamiento() < celular2.getAlmacenamiento()) contador2++;

        if (contador1 > contador2) {
            resultadosComparacion.add("El dispositivo '" + celular1.getNombre() + "' es mejor en " + contador1 + " características.");
        } else if (contador2 > contador1) {
            resultadosComparacion.add("El dispositivo '" + celular2.getNombre() + "' es mejor en " + contador2 + " características.");
        } else {
            resultadosComparacion.add("Ambos dispositivos son igualmente buenos.");
        }
    }

    private void guardarCelulares() {
        try {
            if (celular1.getSeleccion() == 0) celular1.setSeleccion(1);
            if (celular2.getSeleccion() == 0) celular2.setSeleccion(1);

            mongoTemplate.save(celular1, "celulares");
            mongoTemplate.save(celular2, "celulares");

            System.out.println("Celulares guardados en la base de datos.");
        } catch (Exception e) {
            System.err.println("Error al guardar los celulares: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Celular> obtenerCelulares() {
        List<Celular> celulares = new ArrayList<>();
        try {
            celulares = mongoTemplate.findAll(Celular.class, "celulares");
        } catch (Exception e) {
            System.err.println("Error al obtener celulares: " + e.getMessage());
            e.printStackTrace();
        }
        return celulares;
    }

    public List<String> getResultadosComparacion() {
        return resultadosComparacion;
    }
}