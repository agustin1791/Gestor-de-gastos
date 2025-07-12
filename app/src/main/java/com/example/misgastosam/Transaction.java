package com.example.misgastosam;

public class Transaction {
    // Constantes para los tipos de transacción (ya definidas en Category, pero útiles aquí también)
    public static final String TYPE_EXPENSE = "EXPENSE";
    public static final String TYPE_INCOME = "INCOME";

    private int id;
    private double amount;
    private String description;
    private Category category; // <-- Ahora es un objeto Category
    private int categoryId;   // <-- Almacena el ID de la categoría para la DB
    private String type;      // Se derivará del tipo de la Category o del signo del monto
    private String date;      // Formato "yyyy-MM-dd"

    // Constructor principal para transacciones existentes (con ID y objeto Category)
    public Transaction(int id, double amount, String description, Category category, String date) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.category = category;
        // Asignamos el categoryId directamente desde el objeto Category
        this.categoryId = (category != null) ? category.getId() : -1; // -1 si category es null
        this.date = date;
        // Determinamos el tipo de la transacción. Podría ser del Category o por el signo del monto.
        // Optamos por el tipo de Category para consistencia.
        this.type = (category != null) ? category.getType() : (amount < 0 ? TYPE_EXPENSE : TYPE_INCOME);
    }

    // Constructor para nuevas transacciones (sin ID, con objeto Category)
    public Transaction(double amount, String description, Category category, String date) {
        // Llama al constructor principal con id = -1 para indicar que es una nueva transacción
        this(-1, amount, description, category, date);
    }

    // Getters
    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    } // <-- Getter para el objeto Category

    public int getCategoryId() {
        return categoryId;
    } // <-- Getter para el ID de la categoría (para la DB)

    public String getType() {
        return type;
    } // Se obtiene automáticamente al setear el monto o categoría

    public String getDate() {
        return date;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setAmount(double amount) {
        this.amount = amount;
        // Opcional: ajustar el tipo basado en el signo del monto si no se ha establecido explícitamente
        if (this.type == null || this.type.isEmpty()) {
            this.type = (amount < 0) ? TYPE_EXPENSE : TYPE_INCOME;
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // <-- CAMBIO: Setter para el objeto Category. Actualiza el ID y el tipo de la transacción.
    public void setCategory(Category category) {
        this.category = category;
        this.categoryId = (category != null) ? category.getId() : -1;
        this.type = (category != null) ? category.getType() : this.type; // Mantener tipo si category es null
    }

    public void setDate(String date) {
        this.date = date;
    }

    // Este setter para el tipo puede ser útil si se quiere cambiar el tipo independientemente del monto
    public void setType(String type) {
        this.type = type;
    }
}