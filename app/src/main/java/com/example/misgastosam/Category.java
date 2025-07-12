package com.example.misgastosam;

public class Category {
    public static final String TYPE_EXPENSE = "EXPENSE"; // Constante para gasto
    public static final String TYPE_INCOME = "INCOME";   // Constante para ingreso

    private int id;
    private String name;
    private String type; // "EXPENSE" o "INCOME"

    public Category(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    // Constructor para cuando la categoría no tiene ID (ej. nueva categoría antes de guardar en DB)
    public Category(String name, String type) {
        this(-1, name, type); // ID -1 indica que es nueva
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        // Esto convertirá el tipo "EXPENSE" a "Gasto" y "INCOME" a "Ingreso" para mostrar.
        String displayType = "";
        if (this.type.equals(Category.TYPE_EXPENSE)) {
            displayType = "Gasto";
        } else if (this.type.equals(Category.TYPE_INCOME)) {
            displayType = "Ingreso";
        }
        return name + " (" + displayType + ")"; // <<-- CAMBIO AQUÍ
    }
}