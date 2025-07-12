package com.example.misgastosam;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity implements TransactionAdapter.OnItemActionListener {

    private RecyclerView recyclerViewTransactions;
    private TransactionAdapter transactionAdapter;
    private DatabaseHelper dbHelper;
    private List<Transaction> transactionList; // Lista de objetos Transaction

    private Spinner spinnerMonthFilter;
    private Spinner spinnerTypeFilter;
    private Spinner spinnerCategoryFilter;

    // Estos guardarán los valores REALES para el filtro (YYYY-MM, Category.TYPE_EXPENSE, Nombre de Categoría)
    private String currentSelectedMonthYearFilter;
    private String currentSelectedTypeFilter;
    private String currentSelectedCategoryNameFilter; // Solo el nombre de la categoría, no el formato (Tipo)

    // Constantes para los textos de display en los Spinners
    private static final String DISPLAY_TYPE_ALL = "Todos los tipos";
    private static final String DISPLAY_TYPE_EXPENSE = "Gasto";
    private static final String DISPLAY_TYPE_INCOME = "Ingreso";
    private static final String DISPLAY_TYPE_MONTH_ALL = "Todos los meses";
    private static final String DISPLAY_CATEGORY_ALL = "Todas las categorias";

    // Lista de categorías para el spinner, almacenando objetos Category
    private List<Category> allCategoriesForSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        transactionList = new ArrayList<>();
        recyclerViewTransactions = findViewById(R.id.recyclerView_transactions);
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar el adaptador con la lista de transacciones y el listener de acciones
        transactionAdapter = new TransactionAdapter(this, transactionList, this);
        recyclerViewTransactions.setAdapter(transactionAdapter);

        spinnerMonthFilter = findViewById(R.id.spinner_month_filter);
        spinnerTypeFilter = findViewById(R.id.spinner_type_filter);
        spinnerCategoryFilter = findViewById(R.id.spinner_category_filter);

        setupMonthSpinner();
        setupTypeSpinner();
        setupCategorySpinner(); // Carga las categorías y configura el spinner

        // Configurar listeners para los spinners
        spinnerMonthFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters(); // Aplica filtros cuando cambia el mes
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* No hacer nada */ }
        });

        spinnerTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters(); // Aplica filtros cuando cambia el tipo
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* No hacer nada */ }
        });

        spinnerCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters(); // Aplica filtros cuando cambia la categoría
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { /* No hacer nada */ }
        });

        // Inicializar los filtros a sus estados por defecto (todos)
        currentSelectedMonthYearFilter = null;
        currentSelectedTypeFilter = null;
        currentSelectedCategoryNameFilter = null;

        applyFilters(); // Cargar transacciones inicialmente con todos los filtros
    }

    /**
     * Configura el spinner de meses con los meses que tienen transacciones.
     */
    private void setupMonthSpinner() {
        List<String> months = dbHelper.getMonthsWithTransactions(); // Obtiene YYYY-MM
        List<String> displayMonths = new ArrayList<>();
        displayMonths.add(DISPLAY_TYPE_MONTH_ALL); // Añade la opción "Todos los meses"
        displayMonths.addAll(months); // Añade los meses reales

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, displayMonths);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonthFilter.setAdapter(adapter);
    }

    /**
     * Configura el spinner de tipos de transacción (Gasto/Ingreso).
     */
    private void setupTypeSpinner() {
        List<String> types = new ArrayList<>();
        types.add(DISPLAY_TYPE_ALL); // Opción "Todos los tipos"
        types.add(DISPLAY_TYPE_EXPENSE); // "Gasto"
        types.add(DISPLAY_TYPE_INCOME);  // "Ingreso"

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(adapter);
    }

    /**
     * Configura el spinner de categorías.
     * Carga las categorías de la DB y las formatea para display.
     */
    private void setupCategorySpinner() {
        // Obtener la lista de objetos Category de la base de datos
        allCategoriesForSpinner = dbHelper.getAllCategories(null);

        // Crear una lista de Strings para el adaptador del Spinner
        List<String> categoryDisplayNames = new ArrayList<>();
        categoryDisplayNames.add(DISPLAY_CATEGORY_ALL); // Opción "Todas las categorias"

        // Convertir cada objeto Category a su representación String (ej. "Nombre (Tipo)")
        for (Category cat : allCategoriesForSpinner) {
            categoryDisplayNames.add(cat.toString());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryDisplayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryFilter.setAdapter(adapter);

        // Seleccionar "Todas las categorias" por defecto al iniciar
        if (currentSelectedCategoryNameFilter == null) {
            spinnerCategoryFilter.setSelection(0);
        }
    }

    /**
     * Carga las transacciones de la base de datos aplicando los filtros seleccionados.
     */
    private void loadTransactions() {
        List<Transaction> transactions = dbHelper.getTransactions(
                currentSelectedCategoryNameFilter,
                currentSelectedMonthYearFilter,
                currentSelectedTypeFilter
        );

        transactionList.clear();
        transactionList.addAll(transactions);

        // *** AÑADE ESTE BUCLE PARA DEPURAR LOS IDs CARGADOS ***
        for (Transaction t : transactionList) {
            Log.d("HistoryActivity", "Transacción cargada: ID=" + t.getId() + ", Monto=" + t.getAmount() + ", Desc='" + t.getDescription() + "', Cat='" + (t.getCategory() != null ? t.getCategory().getName() : "N/A") + "'");
        }
        // *** FIN DEL BUCLE DE DEPURACIÓN ***

        transactionAdapter.notifyDataSetChanged();
    }

    /**
     * Obtiene los valores de los spinners y actualiza los filtros para la carga de transacciones.
     */
    private void applyFilters() {
        // Manejar filtro de mes
        String selectedMonthDisplay = spinnerMonthFilter.getSelectedItem().toString();
        if (selectedMonthDisplay.equals(DISPLAY_TYPE_MONTH_ALL)) {
            currentSelectedMonthYearFilter = null; // Sin filtro de mes
        } else {
            currentSelectedMonthYearFilter = selectedMonthDisplay; // "YYYY-MM"
        }

        // Manejar filtro de tipo (Gasto/Ingreso)
        String selectedTypeDisplay = spinnerTypeFilter.getSelectedItem().toString();
        if (selectedTypeDisplay.equals(DISPLAY_TYPE_ALL)) {
            currentSelectedTypeFilter = null; // Sin filtro de tipo
        } else if (selectedTypeDisplay.equals(DISPLAY_TYPE_EXPENSE)) {
            currentSelectedTypeFilter = Category.TYPE_EXPENSE; // Usar la constante de la DB
        } else if (selectedTypeDisplay.equals(DISPLAY_TYPE_INCOME)) {
            currentSelectedTypeFilter = Category.TYPE_INCOME; // Usar la constante de la DB
        }

        // Manejar filtro de categoría
        String selectedCategoryDisplay = spinnerCategoryFilter.getSelectedItem().toString();
        if (selectedCategoryDisplay.equals(DISPLAY_CATEGORY_ALL)) {
            currentSelectedCategoryNameFilter = null; // Sin filtro de categoría
        } else {
            // Extraer solo el nombre de la categoría del String "Nombre (Tipo)"
            int lastParenIndex = selectedCategoryDisplay.lastIndexOf(" (");
            if (lastParenIndex != -1) {
                currentSelectedCategoryNameFilter = selectedCategoryDisplay.substring(0, lastParenIndex);
            } else {
                currentSelectedCategoryNameFilter = selectedCategoryDisplay; // Si no tiene el formato (Tipo)
            }
        }
        loadTransactions(); // Recargar las transacciones con los nuevos filtros
    }

    /**
     * Muestra un diálogo para editar una transacción.
     * @param transaction El objeto Transaction a editar.
     */
    private void showEditDialog(Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_transaction, null);
        builder.setView(dialogView);

        EditText etAmount = dialogView.findViewById(R.id.edit_dialog_amount);
        EditText etDescription = dialogView.findViewById(R.id.edit_dialog_description);
        Spinner spCategory = dialogView.findViewById(R.id.edit_dialog_category_spinner);
        RadioGroup rgType = dialogView.findViewById(R.id.edit_dialog_type_group);
        RadioButton rbExpense = dialogView.findViewById(R.id.edit_dialog_radio_expense);
        RadioButton rbIncome = dialogView.findViewById(R.id.edit_dialog_radio_income);

        rbExpense.setText(DISPLAY_TYPE_EXPENSE);
        rbIncome.setText(DISPLAY_TYPE_INCOME);

        etAmount.setText(String.valueOf(Math.abs(transaction.getAmount()))); // Mostrar el valor absoluto
        etDescription.setText(transaction.getDescription());

        // Cargar categorías en el spinner del diálogo
        List<Category> allCategoriesInDialog = dbHelper.getAllCategories(null);
        List<String> categoryDisplayNamesInDialog = new ArrayList<>();
        for (Category cat : allCategoriesInDialog) {
            categoryDisplayNamesInDialog.add(cat.toString());
        }

        ArrayAdapter<String> categoryAdapterDialog = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryDisplayNamesInDialog);
        categoryAdapterDialog.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapterDialog);

        // Seleccionar la categoría actual de la transacción en el spinner
        int categoryPosition = -1;
        if (transaction.getCategory() != null) {
            // Buscar la posición de la categoría original en la lista formateada del spinner
            categoryPosition = categoryDisplayNamesInDialog.indexOf(transaction.getCategory().toString());
        }
        if (categoryPosition != -1) {
            spCategory.setSelection(categoryPosition);
        }

        // Marcar el RadioButton según el tipo actual de la transacción (que se basa en la categoría)
        if (transaction.getType().equals(Category.TYPE_EXPENSE)) {
            rbExpense.setChecked(true);
        } else {
            rbIncome.setChecked(true);
        }

        // Deshabilitar el RadioGroup para que el tipo se defina por la categoría seleccionada
        rgType.setEnabled(false);
        for (int i = 0; i < rgType.getChildCount(); i++) {
            rgType.getChildAt(i).setEnabled(false);
        }

        // Listener para el spinner de categorías dentro del diálogo
        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Actualizar los radio buttons basados en el tipo de la categoría seleccionada en el diálogo
                if (allCategoriesInDialog != null && position < allCategoriesInDialog.size()) {
                    Category selectedCat = allCategoriesInDialog.get(position);
                    if (selectedCat.getType().equals(Category.TYPE_EXPENSE)) {
                        rbExpense.setChecked(true);
                    } else if (selectedCat.getType().equals(Category.TYPE_INCOME)) {
                        rbIncome.setChecked(true);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada
            }
        });


        builder.setTitle("Editar Transacción")
                .setPositiveButton("Guardar", (dialog, id) -> {
                    String newAmountStr = etAmount.getText().toString().trim();
                    String newDescription = etDescription.getText().toString().trim();
                    String selectedCategoryDisplayString = spCategory.getSelectedItem().toString(); // "Nombre (Tipo)"

                    if (TextUtils.isEmpty(newAmountStr)) {
                        Toast.makeText(this, "El monto no puede estar vacío.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //if (TextUtils.isEmpty(newDescription)) {
                    //    Toast.makeText(this, "La descripción no puede estar vacía.", Toast.LENGTH_SHORT).show();
                    //    return;
                    //}

                    // --- CAMBIO CLAVE: Obtener el objeto Category a partir del string del spinner ---
                    Category newCategoryObject = dbHelper.getCategoryByNameAndType(selectedCategoryDisplayString);
                    if (newCategoryObject == null) {
                        Toast.makeText(this, "Error: Categoría seleccionada no válida.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // --- FIN CAMBIO CLAVE ---

                    double newAmount = Double.parseDouble(newAmountStr);
                    // El tipo de la transacción se define por el tipo de la nueva categoría seleccionada
                    String newType = newCategoryObject.getType();

                    if (newType.equals(Category.TYPE_EXPENSE)) {
                        newAmount = -Math.abs(newAmount); // Asegura que el monto sea negativo si es gasto
                    } else {
                        newAmount = Math.abs(newAmount); // Asegura que el monto sea positivo si es ingreso
                    }

                    // Actualizar el objeto Transaction
                    transaction.setAmount(newAmount);
                    transaction.setDescription(newDescription);
                    transaction.setCategory(newCategoryObject); // <-- Actualizar el objeto Category
                    transaction.setType(newType); // Actualizar el tipo de la transacción

                    boolean success = dbHelper.updateTransaction(transaction);
                    if (success) {
                        Toast.makeText(this, "Transacción actualizada", Toast.LENGTH_SHORT).show();
                        applyFilters(); // Recargar transacciones después de la actualización
                    } else {
                        Toast.makeText(this, "Error al actualizar la transacción", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar una transacción.
     * @param transaction El objeto Transaction a eliminar.
     */
    private void confirmDelete(Transaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Transacción")
                .setMessage("¿Estás seguro de que quieres eliminar esta transacción?\n" +
                        "Monto: $" + new DecimalFormat("#,##0.00").format(transaction.getAmount()) +
                        "\nDescripción: " + transaction.getDescription())
                .setPositiveButton("Eliminar", (dialog, id) -> {
                    boolean success = dbHelper.deleteTransaction(transaction.getId());
                    if (success) {
                        Toast.makeText(this, "Transacción eliminada", Toast.LENGTH_SHORT).show();
                        applyFilters(); // Recargar transacciones después de eliminar
                    } else {
                        Toast.makeText(this, "Error al eliminar la transacción", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFilters(); // Recargar las transacciones cada vez que se vuelve a esta pantalla
    }

    // Implementación de la interfaz TransactionAdapter.OnItemActionListener
    @Override
    public void onEdit(Transaction transaction) {
        showEditDialog(transaction);
    }

    @Override
    public void onDelete(Transaction transaction) {
        confirmDelete(transaction);
    }

}