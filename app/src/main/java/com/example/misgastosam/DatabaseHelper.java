package com.example.misgastosam;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "misgastos.db";
    private static final int DATABASE_VERSION = 5;

    // Tabla de Categorías
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COLUMN_CATEGORY_ID = "_id";
    public static final String COLUMN_CATEGORY_NAME = "name";
    public static final String COLUMN_CATEGORY_TYPE = "type"; // "EXPENSE" o "INCOME"

    // Tabla de Transacciones
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COLUMN_TRANSACTION_ID = "_id";
    public static final String COLUMN_TRANSACTION_AMOUNT = "amount";
    public static final String COLUMN_TRANSACTION_DESCRIPTION = "description";
    public static final String COLUMN_TRANSACTION_CATEGORY_ID = "category_id"; // FK a categories
    public static final String COLUMN_TRANSACTION_DATE = "date"; // Formato TEXT YYYY-MM-DD

    // Sentencia SQL para crear la tabla de categorías
    private static final String CREATE_TABLE_CATEGORIES =
            "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                    COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_CATEGORY_NAME + " TEXT NOT NULL," + // Ya no UNIQUE solo el nombre
                    COLUMN_CATEGORY_TYPE + " TEXT NOT NULL," +
                    "UNIQUE(" + COLUMN_CATEGORY_NAME + ", " + COLUMN_CATEGORY_TYPE + "));"; // <<-- CAMBIO AQUÍ: UNIQUE de la combinación de nombre y tipo


    // Sentencia SQL para crear la tabla de transacciones
    private static final String CREATE_TABLE_TRANSACTIONS =
            "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                    COLUMN_TRANSACTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TRANSACTION_AMOUNT + " REAL NOT NULL," +
                    COLUMN_TRANSACTION_DESCRIPTION + " TEXT," +
                    COLUMN_TRANSACTION_CATEGORY_ID + " INTEGER," +
                    COLUMN_TRANSACTION_DATE + " TEXT NOT NULL," +
                    "FOREIGN KEY(" + COLUMN_TRANSACTION_CATEGORY_ID + ") REFERENCES " +
                    TABLE_CATEGORIES + "(" + COLUMN_CATEGORY_ID + ") ON DELETE CASCADE);"; // ON DELETE CASCADE para eliminar transacciones si se elimina la categoría

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON;"); // Habilitar claves foráneas
    }

    // --- Métodos para Categorías ---

    /**
     * Obtiene una categoría por su nombre y tipo (directo, sin formato de display).
     * @param name El nombre de la categoría.
     * @param type El tipo de la categoría (Category.TYPE_EXPENSE o Category.TYPE_INCOME).
     * @return Objeto Category si se encuentra, null en caso contrario.
     */
    @Nullable
    public Category getCategoryByName(String name, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Category category = null;

        try {
            String[] columns = {COLUMN_CATEGORY_ID, COLUMN_CATEGORY_NAME, COLUMN_CATEGORY_TYPE};
            String selection = COLUMN_CATEGORY_NAME + " = ? AND " + COLUMN_CATEGORY_TYPE + " = ?";
            String[] selectionArgs = {name, type};

            cursor = db.query(TABLE_CATEGORIES, columns, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID);
                int nameIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME);
                int typeIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE);

                int id = cursor.getInt(idIndex);
                String foundName = cursor.getString(nameIndex);
                String foundType = cursor.getString(typeIndex);

                category = new Category(id, foundName, foundType);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al obtener categoría por nombre y tipo directo: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // No cerramos la base de datos aquí si va a ser usada en `createDefaultCategoriesIfNotExist`
        }
        return category;
    }


    /**
     * Obtiene una categoría por su nombre y el tipo de display (ej. "Alimentos (Gasto)").
     * Convierte el tipo de display a su tipo de DB antes de la consulta.
     * Este método sigue siendo útil si tu `Category` clase usa el formato de display en `toString()`.
     * @param categoryNameAndType El string de la categoría con formato "Nombre (Tipo)".
     * @return Objeto Category si se encuentra, null en caso contrario.
     */
    public Category getCategoryByNameAndType(String categoryNameAndType) {
        // Validación básica del formato
        if (categoryNameAndType == null || !categoryNameAndType.contains(" (") || !categoryNameAndType.endsWith(")")) {
            Log.e("DatabaseHelper", "Formato de categoría inesperado o nulo: " + categoryNameAndType);
            return null;
        }

        String name = categoryNameAndType.substring(0, categoryNameAndType.lastIndexOf(" ("));
        String typeDisplay = categoryNameAndType.substring(categoryNameAndType.lastIndexOf(" (") + 2, categoryNameAndType.length() - 1);

        String typeForDb = null;
        if (typeDisplay.equals("Gasto")) {
            typeForDb = Category.TYPE_EXPENSE;
        } else if (typeDisplay.equals("Ingreso")) {
            typeForDb = Category.TYPE_INCOME;
        }

        if (typeForDb == null) {
            Log.e("DatabaseHelper", "Tipo de categoría de display no reconocido: " + typeDisplay);
            return null;
        }

        // Reutilizamos el nuevo método getCategoryByName
        return getCategoryByName(name, typeForDb);
    }

    /**
     * Verifica si una categoría con un nombre y tipo específico ya existe en la DB.
     * @param name El nombre de la categoría.
     * @param type El tipo de la categoría (Category.TYPE_EXPENSE o Category.TYPE_INCOME).
     * @return true si existe, false en caso contrario.
     */
    public boolean categoryExists(String name, String type) {
        return getCategoryByName(name, type) != null; // Reutiliza el método getCategoryByName
    }


    /**
     * Agrega una nueva categoría a la base de datos.
     * @param category El objeto Category a agregar.
     * @return El ID de la nueva fila insertada, o -1 si hubo un error.
     */
    public long addCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, category.getName());
        values.put(COLUMN_CATEGORY_TYPE, category.getType());

        long result = -1;
        try {
            result = db.insert(TABLE_CATEGORIES, null, values);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al agregar categoría: " + e.getMessage());
        } finally {
            db.close();
        }
        return result;
    }

    /**
     * Asegura que las categorías por defecto "Otros" existan en la base de datos.
     * Si no existen, las crea.
     */
    public void createDefaultCategoriesIfNotExist() {
        // Usamos getWritableDatabase() para insertar si es necesario
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // "Otros" para gastos
            if (!categoryExists("Otros", Category.TYPE_EXPENSE)) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_CATEGORY_NAME, "Otros");
                values.put(COLUMN_CATEGORY_TYPE, Category.TYPE_EXPENSE);
                db.insert(TABLE_CATEGORIES, null, values);
                Log.d("DatabaseHelper", "Categoría 'Otros' (Gasto) creada.");
            } else {
                Log.d("DatabaseHelper", "Categoría 'Otros' (Gasto) ya existe.");
            }

            // "Otros" para ingresos
            if (!categoryExists("Otros", Category.TYPE_INCOME)) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_CATEGORY_NAME, "Otros");
                values.put(COLUMN_CATEGORY_TYPE, Category.TYPE_INCOME);
                db.insert(TABLE_CATEGORIES, null, values);
                Log.d("DatabaseHelper", "Categoría 'Otros' (Ingreso) creada.");
            } else {
                Log.d("DatabaseHelper", "Categoría 'Otros' (Ingreso) ya existe.");
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al crear categorías por defecto: " + e.getMessage());
        } finally {
            db.close(); // Cierra la base de datos después de la operación
        }
    }


    /**
     * Obtiene todas las categorías de la base de datos, opcionalmente filtradas por tipo.
     * @param typeFilter El tipo de categoría para filtrar (Category.TYPE_EXPENSE, Category.TYPE_INCOME) o null para todas.
     * @return Una lista de objetos Category.
     */
    public List<Category> getAllCategories(@Nullable String typeFilter) { // Anotación @Nullable para claridad
        List<Category> categoryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String query = "SELECT * FROM " + TABLE_CATEGORIES;
            String[] selectionArgs = null;
            if (typeFilter != null && !typeFilter.isEmpty()) {
                query += " WHERE " + COLUMN_CATEGORY_TYPE + " = ?";
                selectionArgs = new String[]{typeFilter};
            }
            query += " ORDER BY " + COLUMN_CATEGORY_NAME + " ASC"; // Ordenar por nombre

            cursor = db.rawQuery(query, selectionArgs);

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID);
                int nameIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME);
                int typeIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE);

                do {
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    String type = cursor.getString(typeIndex);
                    categoryList.add(new Category(id, name, type));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al obtener todas las categorías: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // No cerramos la DB aquí, ya que se abre con getReadableDatabase()
        }
        return categoryList;
    }


    /**
     * Actualiza una categoría existente en la base de datos.
     * @param category El objeto Category con los datos actualizados.
     * @return true si la actualización fue exitosa, false en caso contrario.
     */
    public boolean updateCategory(Category category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CATEGORY_NAME, category.getName());
        values.put(COLUMN_CATEGORY_TYPE, category.getType());

        String whereClause = COLUMN_CATEGORY_ID + " = ?";
        String[] whereArgs = {String.valueOf(category.getId())};

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_CATEGORIES, values, whereClause, whereArgs);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al actualizar categoría: " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.close();
        }
        return rowsAffected > 0;
    }


    /**
     * Elimina una categoría de la base de datos por su ID.
     * Las transacciones asociadas también serán eliminadas debido a ON DELETE CASCADE.
     * @param categoryId El ID de la categoría a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean deleteCategory(int categoryId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(TABLE_CATEGORIES, COLUMN_CATEGORY_ID + " = ?", new String[]{String.valueOf(categoryId)});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al eliminar categoría: " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.close();
        }
        return rowsAffected > 0;
    }

    // --- Métodos para Transacciones ---

    /**
     * Agrega una nueva transacción a la base de datos.
     * @param transaction El objeto Transaction a agregar.
     * @return El ID de la nueva fila insertada, o -1 si hubo un error.
     */
    public long addTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANSACTION_AMOUNT, transaction.getAmount());
        values.put(COLUMN_TRANSACTION_DESCRIPTION, transaction.getDescription());
        values.put(COLUMN_TRANSACTION_CATEGORY_ID, transaction.getCategoryId());
        values.put(COLUMN_TRANSACTION_DATE, transaction.getDate());

        long result = -1;
        try {
            result = db.insert(TABLE_TRANSACTIONS, null, values);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al agregar transacción: " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.close();
        }
        return result;
    }

    /**
     * Obtiene transacciones filtradas por categoría, mes y tipo.
     * @param categoryName El nombre de la categoría para filtrar (ej. "Comida"), o null para todas.
     * @param monthYear La cadena "YYYY-MM" para filtrar por mes, o null para todos los meses.
     * @param type El tipo de transacción (Category.TYPE_EXPENSE, Category.TYPE_INCOME) o null para todos los tipos.
     * @return Una lista de objetos Transaction.
     */
    public List<Transaction> getTransactions(String categoryName, String monthYear, String type) {
        List<Transaction> transactionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT T.").append(COLUMN_TRANSACTION_ID).append(", ");
            queryBuilder.append("T.").append(COLUMN_TRANSACTION_AMOUNT).append(", ");
            queryBuilder.append("T.").append(COLUMN_TRANSACTION_DESCRIPTION).append(", ");
            queryBuilder.append("T.").append(COLUMN_TRANSACTION_DATE).append(", ");
            queryBuilder.append("C.").append(COLUMN_CATEGORY_ID).append(", ");
            queryBuilder.append("C.").append(COLUMN_CATEGORY_NAME).append(", ");
            queryBuilder.append("C.").append(COLUMN_CATEGORY_TYPE);
            queryBuilder.append(" FROM ").append(TABLE_TRANSACTIONS).append(" T");
            queryBuilder.append(" INNER JOIN ").append(TABLE_CATEGORIES).append(" C ON T.").append(COLUMN_TRANSACTION_CATEGORY_ID).append(" = C.").append(COLUMN_CATEGORY_ID);

            List<String> selectionArgs = new ArrayList<>();
            List<String> conditions = new ArrayList<>();

            if (categoryName != null && !categoryName.isEmpty()) {
                conditions.add("C." + COLUMN_CATEGORY_NAME + " = ?");
                selectionArgs.add(categoryName);
            }
            if (monthYear != null && !monthYear.isEmpty()) {
                conditions.add("strftime('%Y-%m', T." + COLUMN_TRANSACTION_DATE + ") = ?");
                selectionArgs.add(monthYear);
            }
            if (type != null && !type.isEmpty()) {
                // El tipo aquí ya debe ser EXPENSE o INCOME desde HistoryActivity
                conditions.add("C." + COLUMN_CATEGORY_TYPE + " = ?");
                selectionArgs.add(type);
            }

            if (!conditions.isEmpty()) {
                queryBuilder.append(" WHERE ").append(TextUtils.join(" AND ", conditions));
            }

            queryBuilder.append(" ORDER BY T.").append(COLUMN_TRANSACTION_DATE).append(" DESC;");

            cursor = db.rawQuery(queryBuilder.toString(), selectionArgs.toArray(new String[0]));

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_ID);
                int amountIndex = cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_AMOUNT);
                int descIndex = cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_DESCRIPTION);
                int dateIndex = cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_DATE);
                int categoryIdIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_ID);
                int categoryNameIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME);
                int categoryTypeIndex = cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_TYPE);

                do {
                    int id = cursor.getInt(idIndex);
                    double amount = cursor.getDouble(amountIndex);
                    String description = cursor.getString(descIndex);
                    String date = cursor.getString(dateIndex);
                    int catId = cursor.getInt(categoryIdIndex);
                    String catName = cursor.getString(categoryNameIndex);
                    String catType = cursor.getString(categoryTypeIndex);

                    Category category = new Category(catId, catName, catType);
                    // Usar el constructor de Transaction que acepta el objeto Category
                    Transaction transaction = new Transaction(id, amount, description, category, date);
                    transactionList.add(transaction);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al obtener transacciones filtradas: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return transactionList;
    }

    /**
     * Actualiza una transacción existente en la base de datos.
     * @param transaction El objeto Transaction con los datos actualizados.
     * @return true si la actualización fue exitosa, false en caso contrario.
     */
    public boolean updateTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANSACTION_AMOUNT, transaction.getAmount());
        values.put(COLUMN_TRANSACTION_DESCRIPTION, transaction.getDescription());
        values.put(COLUMN_TRANSACTION_CATEGORY_ID, transaction.getCategoryId());
        values.put(COLUMN_TRANSACTION_DATE, transaction.getDate());

        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_TRANSACTIONS, values, COLUMN_TRANSACTION_ID + " = ?",
                    new String[]{String.valueOf(transaction.getId())});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al actualizar transacción: " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.close();
        }
        return rowsAffected > 0;
    }

    /**
     * Elimina una transacción de la base de datos por su ID.
     * @param transactionId El ID de la transacción a eliminar.
     * @return true si la eliminación fue exitosa, false en caso contrario.
     */
    public boolean deleteTransaction(int transactionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            // *** AÑADE ESTAS LÍNEAS DE LOG ***
            Log.d("DatabaseHelper", "Intentando eliminar transacción con ID: " + transactionId);
            rowsAffected = db.delete(TABLE_TRANSACTIONS, COLUMN_TRANSACTION_ID + " = ?",
                    new String[]{String.valueOf(transactionId)});

            if (rowsAffected > 0) {
                Log.d("DatabaseHelper", "Transacción con ID " + transactionId + " eliminada exitosamente. Filas afectadas: " + rowsAffected);
            } else {
                Log.w("DatabaseHelper", "No se encontró la transacción con ID " + transactionId + " para eliminar.");
            }
            // *** FIN DE LAS LÍNEAS DE LOG AÑADIDAS ***

        } catch (Exception e) {
            // *** MODIFICA ESTA LÍNEA TAMBIÉN PARA MÁS DETALLE ***
            Log.e("DatabaseHelper", "Error CRÍTICO al eliminar transacción con ID " + transactionId + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            db.close();
        }
        return rowsAffected > 0;
    }

    /**
     * Obtiene el balance total de todas las transacciones.
     * @return El balance total.
     */
    public double getTotalBalance() {
        SQLiteDatabase db = this.getReadableDatabase();
        double totalBalance = 0.0;
        Cursor cursor = null;
        try {
            String query = "SELECT SUM(" + COLUMN_TRANSACTION_AMOUNT + ") FROM " + TABLE_TRANSACTIONS;
            cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                totalBalance = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al obtener balance total: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return totalBalance;
    }

    /**
     * Obtiene el balance para un mes y año específico (formato YYYY-MM).
     * @param monthYear La cadena "YYYY-MM" del mes y año.
     * @return El balance para ese mes.
     */
    public double getBalanceForMonth(String monthYear) {
        SQLiteDatabase db = this.getReadableDatabase();
        double balance = 0.0;
        Cursor cursor = null;

        try {
            String query = "SELECT SUM(" + COLUMN_TRANSACTION_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                    " WHERE strftime('%Y-%m', " + COLUMN_TRANSACTION_DATE + ") = ?";
            String[] selectionArgs = {monthYear};

            cursor = db.rawQuery(query, selectionArgs);

            if (cursor != null && cursor.moveToFirst()) {
                balance = cursor.getDouble(0); // El resultado de SUM está en la columna 0
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al obtener balance para el mes " + monthYear + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return balance;
    }

    /**
     * Obtiene una lista de todos los meses (YYYY-MM) que tienen transacciones.
     * @return Una lista de strings de meses.
     */
    public List<String> getMonthsWithTransactions() {
        List<String> months = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Selecciona los meses distintos en formato YYYY-MM y los ordena
            String query = "SELECT DISTINCT strftime('%Y-%m', " + COLUMN_TRANSACTION_DATE + ") FROM " + TABLE_TRANSACTIONS +
                    " ORDER BY strftime('%Y-%m', " + COLUMN_TRANSACTION_DATE + ") DESC";
            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    months.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error al obtener meses con transacciones: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return months;
    }
}