package com.celfinder.Model;

public class Celularmedia {

    private double ghz;
    private int camara;
    private int ram;
    private int bateria;
    private int almacenamiento;

    public Celularmedia() {
        this.ghz = 2.8;          
        this.camara = 48;        
        this.ram = 8;            
        this.bateria = 4500;     
        this.almacenamiento = 128; 
    }

    // Getters y setters
    public double getGhz() {
        return ghz;
    }

    public void setGhz(double ghz) {
        this.ghz = ghz;
    }

    public int getCamara() {
        return camara;
    }

    public void setCamara(int camara) {
        this.camara = camara;
    }

    public int getRam() {
        return ram;
    }

    public void setRam(int ram) {
        this.ram = ram;
    }

    public int getBateria() {
        return bateria;
    }

    public void setBateria(int bateria) {
        this.bateria = bateria;
    }

    public int getAlmacenamiento() {
        return almacenamiento;
    }

    public void setAlmacenamiento(int almacenamiento) {
        this.almacenamiento = almacenamiento;
    }
}