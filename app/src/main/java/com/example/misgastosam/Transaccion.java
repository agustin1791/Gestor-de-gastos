package com.example.misgastosam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Transaccion extends AppCompatActivity {

    private EditText editTextAmount;
    private EditText editTextDescription;
    private Spinner spinnerCategory;
    private RadioGroup radioGroupType;
    private RadioButton radioExpense, radioIncome;
    private Button buttonSaveTransaction;

    private DatabaseHelper dbHelper;

    // Constantes para los TEXTOS que se muestran al usuario en los RadioButtons
    private static final String DISPLAY_TEXT_EXPENSE = "Gasto";
    private static final String DISPLAY_TEXT_INCOME = "Ingreso";

    // Variables para almacenar las categorías y el Category seleccionado
    private List<Category> allCategories; // Todas las categorías de la DB
    private List<Category> currentFilteredCategories; // Categorías actualmente mostradas en el spinner (filtradas por tipo)
    private Category selectedCategoryObject; // Objeto Category actualmente seleccionado en el spinner

    // Constantes para SharedPreferences (para guardar la última categoría seleccionada para cada tipo)
    private static final String PREFS_NAME = "TransactionPrefs";
    private static final String KEY_LAST_EXPENSE_CATEGORY_ID = "last_expense_category_id";
    private static final String KEY_LAST_INCOME_CATEGORY_ID = "last_income_category_id";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_transaccion);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        // **IMPORTANTE**: Asegura que las categorías "Otros" existan al iniciar la actividad
        dbHelper.createDefaultCategoriesIfNotExist();

        // Conectar vistas
        editTextAmount = findViewById(R.id.editText_amount);
        editTextDescription = findViewById(R.id.editText_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        radioGroupType = findViewById(R.id.radioGroup_type);
        radioExpense = findViewById(R.id.radio_expense);
        radioIncome = findViewById(R.id.radio_income);
        buttonSaveTransaction = findViewById(R.id.button_save_transaction);

        // Configurar textos de los RadioButtons
        radioExpense.setText(DISPLAY_TEXT_EXPENSE);
        radioIncome.setText(DISPLAY_TEXT_INCOME);

        // Cargar *todas* las categorías de la base de datos UNA SOLA VEZ
        allCategories = dbHelper.getAllCategories(null);

        // **MODIFICACIÓN CLAVE**: Listener para el RadioGroup
        // Este listener ahora CONTROLA lo que se muestra en el Spinner
        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d("Transaccion", "RadioGroup checked changed to ID: " + checkedId);
            loadCategoriesIntoSpinnerBasedOnType(); // Recarga el Spinner con las categorías correctas
        });

        // **MODIFICACIÓN CLAVE**: Listener para el Spinner
        // Este listener solo ACTUALIZA selectedCategoryObject y ajusta el RadioGroup SI ES NECESARIO
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentFilteredCategories != null && position < currentFilteredCategories.size()) {
                    selectedCategoryObject = currentFilteredCategories.get(position);
                    Log.d("Transaccion", "Spinner selected: " + selectedCategoryObject.getName() + " (" + selectedCategoryObject.getType() + ")");

                    // Evitar bucle infinito y ajustar RadioButton si la selección manual contradice el tipo actual
                    int currentRadioCheckedId = radioGroupType.getCheckedRadioButtonId();
                    if (selectedCategoryObject.getType().equals(Category.TYPE_EXPENSE) && currentRadioCheckedId != R.id.radio_expense) {
                        radioExpense.setChecked(true);
                    } else if (selectedCategoryObject.getType().equals(Category.TYPE_INCOME) && currentRadioCheckedId != R.id.radio_income) {
                        radioIncome.setChecked(true);
                    }
                } else {
                    selectedCategoryObject = null; // No hay categoría seleccionada
                    Log.w("Transaccion", "Spinner selected: No category at position " + position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCategoryObject = null; // No hay categoría seleccionada
                Log.d("Transaccion", "Spinner nothing selected.");
            }
        });

        // **Inicialización del Spinner al abrir la actividad**:
        // Llama a esta función para cargar y seleccionar la categoría por defecto (Gasto) al inicio.
        loadCategoriesIntoSpinnerBasedOnType();

        // Listener para el botón Guardar
        buttonSaveTransaction.setOnClickListener(v -> saveTransaction());
    }

    /**
     * Carga las categorías en el Spinner basándose en el tipo de transacción seleccionado
     * en el RadioGroup. Selecciona la categoría "Otros" por defecto, o la última usada.
     */
    private void loadCategoriesIntoSpinnerBasedOnType() {
        String selectedType;
        String defaultCategoryName = "Otros"; // Nombre de la categoría por defecto que buscamos

        // Determinar el tipo de transacción actual del RadioGroup
        if (radioGroupType.getCheckedRadioButtonId() == R.id.radio_expense) {
            selectedType = Category.TYPE_EXPENSE;
            Log.d("Transaccion", "Selected Type: EXPENSE");
        } else { // R.id.radio_income
            selectedType = Category.TYPE_INCOME;
            Log.d("Transaccion", "Selected Type: INCOME");
        }

        // Filtra las categorías de 'allCategories' según el tipo seleccionado
        currentFilteredCategories = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.getType().equals(selectedType)) {
                currentFilteredCategories.add(category);
            }
        }

        // Prepara los nombres para mostrar en el Spinner
        List<String> categoryDisplayNames = new ArrayList<>();
        for (Category category : currentFilteredCategories) {
            categoryDisplayNames.add(category.getName()); // Asumiendo que Category.toString() devuelve solo el nombre
        }

        // Configurar el adaptador para el Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryDisplayNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // **LÓGICA DE SELECCIÓN DE CATEGORÍA POR DEFECTO**
        int positionToSelect = -1;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String keyForLastCategoryId = (selectedType.equals(Category.TYPE_EXPENSE)) ?
                KEY_LAST_EXPENSE_CATEGORY_ID : KEY_LAST_INCOME_CATEGORY_ID;

        // 1. Intentar seleccionar la última categoría usada (guardada por ID)
        int lastUsedCategoryId = prefs.getInt(keyForLastCategoryId, -1);
        if (lastUsedCategoryId != -1) {
            for (int i = 0; i < currentFilteredCategories.size(); i++) {
                if (currentFilteredCategories.get(i).getId() == lastUsedCategoryId) {
                    positionToSelect = i;
                    Log.d("Transaccion", "Selecting last used category: " + currentFilteredCategories.get(i).getName());
                    break;
                }
            }
        }

        // 2. Si no se encontró la última usada, intentar seleccionar "Otros"
        if (positionToSelect == -1) {
            for (int i = 0; i < currentFilteredCategories.size(); i++) {
                if (currentFilteredCategories.get(i).getName().equals(defaultCategoryName)) {
                    positionToSelect = i;
                    Log.d("Transaccion", "Selecting default 'Otros' category.");
                    break;
                }
            }
        }

        // 3. Si "Otros" tampoco se encontró o no hay categorías, selecciona la primera o deshabilita
        if (positionToSelect != -1) {
            spinnerCategory.setSelection(positionToSelect);
            selectedCategoryObject = currentFilteredCategories.get(positionToSelect);
            buttonSaveTransaction.setEnabled(true);
            spinnerCategory.setEnabled(true);
        } else if (!currentFilteredCategories.isEmpty()) {
            spinnerCategory.setSelection(0); // Selecciona la primera disponible
            selectedCategoryObject = currentFilteredCategories.get(0);
            Toast.makeText(this, "Categoría 'Otros' no encontrada, se seleccionó la primera disponible para " + selectedType + ".", Toast.LENGTH_SHORT).show();
            buttonSaveTransaction.setEnabled(true);
            spinnerCategory.setEnabled(true);
        } else {
            // No hay categorías del tipo seleccionado
            Toast.makeText(this, "No hay categorías disponibles para " + selectedType + ". Por favor, añade una primero.", Toast.LENGTH_LONG).show();
            buttonSaveTransaction.setEnabled(false);
            spinnerCategory.setEnabled(false);
            selectedCategoryObject = null; // Asegúrate de que no haya un objeto de categoría seleccionado
        }

        // Asegurarse de que los otros campos estén habilitados/deshabilitados según si hay categorías
        radioGroupType.setEnabled(true); // El RadioGroup siempre debe estar habilitado
        editTextAmount.setEnabled(buttonSaveTransaction.isEnabled());
        editTextDescription.setEnabled(buttonSaveTransaction.isEnabled());
    }

    private void saveTransaction() {
        String amountStr = editTextAmount.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(amountStr)) {
            editTextAmount.setError("El monto no puede estar vacío.");
            return;
        }

        // FUNCION PARA QUE LA DESCRIPCIÓN NO ESTE VACÍA.
        // if (TextUtils.isEmpty(description)) {
        //     editTextDescription.setError("La descripción no puede estar vacía.");
        //    return;
        //}

        if (selectedCategoryObject == null) {
            Toast.makeText(this, "Por favor, selecciona una categoría. No hay categorías disponibles.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String transactionType; // El tipo que se guardará en la transacción

        // El tipo de la transacción es el de la categoría seleccionada
        transactionType = selectedCategoryObject.getType();

        // Ajustar el signo del monto según el tipo de la categoría
        if (transactionType.equals(Category.TYPE_EXPENSE)) {
            amount = -Math.abs(amount); // Asegura que sea negativo para gastos
        } else { // Category.TYPE_INCOME
            amount = Math.abs(amount); // Asegura que sea positivo para ingresos
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Crear el objeto Transaction usando el objeto Category completo
        Transaction newTransaction = new Transaction(amount, description, selectedCategoryObject, currentDate);

        // Guardar la transacción en la base de datos
        long result = dbHelper.addTransaction(newTransaction);

        if (result != -1) {
            Toast.makeText(this, "Transacción guardada exitosamente!", Toast.LENGTH_SHORT).show();

            // **IMPORTANTE**: Guardar la ID de la última categoría usada como la predeterminada para este tipo
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String keyForLastCategoryId = (transactionType.equals(Category.TYPE_EXPENSE)) ?
                    KEY_LAST_EXPENSE_CATEGORY_ID : KEY_LAST_INCOME_CATEGORY_ID;
            editor.putInt(keyForLastCategoryId, selectedCategoryObject.getId());
            editor.apply(); // Usa apply() para guardar en segundo plano sin bloquear el hilo principal

            finish(); // Cierra esta actividad y regresa a la anterior (MainActivity)
        } else {
            Toast.makeText(this, "Error al guardar la transacción.", Toast.LENGTH_SHORT).show();
        }
    }
}