package com.example.misgastosam;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.TypedValue;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class CategoriaActivity extends AppCompatActivity implements CategoryAdapter.OnItemClickListener {

    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private List<String> currentCategoryList;
    private DatabaseHelper dbHelper;

    private Button addCategoryButton;
    private Button editCategoryButton;
    private Button deleteCategoryButton;

    // Constantes para los TEXTOS que se muestran al usuario en la UI
    private static final String DISPLAY_TEXT_EXPENSE = "Gasto";
    private static final String DISPLAY_TEXT_INCOME = "Ingreso";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_categoria);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        categoriesRecyclerView = findViewById(R.id.recyclerView_categories);
        addCategoryButton = findViewById(R.id.button_add_category);
        editCategoryButton = findViewById(R.id.button_edit_category);
        deleteCategoryButton = findViewById(R.id.button_delete_category);

        TextView categoryTitle = findViewById(R.id.textView_category_title);
        categoryTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f);
        categoryTitle.setTextColor(getResources().getColor(R.color.apple_green));

        currentCategoryList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(currentCategoryList, this);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoriesRecyclerView.setAdapter(categoryAdapter);

        loadCategories();
        // Inicialmente los botones de editar/borrar están deshabilitados
        editCategoryButton.setEnabled(false);
        deleteCategoryButton.setEnabled(false);

        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());
        editCategoryButton.setOnClickListener(v -> {
            // selectedCategoryDisplayString será el string completo (ej. "Nombre (Gasto)")
            String selectedCategoryDisplayString = categoryAdapter.getSelectedCategoryName();
            if (selectedCategoryDisplayString != null) {
                // Obtener el objeto Category completo usando el método de DBHelper
                Category categoryToEdit = dbHelper.getCategoryByNameAndType(selectedCategoryDisplayString);
                if (categoryToEdit != null) {
                    showEditCategoryDialog(categoryToEdit);
                } else {
                    Toast.makeText(CategoriaActivity.this, "Error al cargar la categoría para editar. Formato no válido o categoría no encontrada.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CategoriaActivity.this, "Selecciona una categoría para editar.", Toast.LENGTH_SHORT).show();
            }
        });
        deleteCategoryButton.setOnClickListener(v -> {
            // selectedCategoryDisplayString será el string completo (ej. "Nombre (Gasto)")
            String selectedCategoryDisplayString = categoryAdapter.getSelectedCategoryName();
            if (selectedCategoryDisplayString != null) {
                // Obtener el objeto Category completo usando el método de DBHelper
                Category categoryToDelete = dbHelper.getCategoryByNameAndType(selectedCategoryDisplayString);
                if (categoryToDelete != null) {
                    showDeleteConfirmationDialog(categoryToDelete);
                } else {
                    Toast.makeText(CategoriaActivity.this, "Error al cargar la categoría para eliminar. Formato no válido o categoría no encontrada.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CategoriaActivity.this, "Selecciona una categoría para eliminar.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Carga y muestra las categorías desde la base de datos en el RecyclerView.
     */
    private void loadCategories() {
        // 1. Obtiene la lista de objetos Category de la base de datos
        List<Category> categories = dbHelper.getAllCategories(null); // 'null' para obtener todos los tipos

        // 2. Convierte los objetos Category a Strings en el formato "Nombre (Tipo)"
        List<String> formattedCategoryNames = new ArrayList<>();
        for (Category cat : categories) {
            // El método toString() de tu clase Category.java ya hace el formateo "Nombre (Tipo)"
            formattedCategoryNames.add(cat.toString());
        }

        // 3. Actualiza el adaptador con la lista de Strings formateados
        categoryAdapter.updateData(formattedCategoryNames);
        categoryAdapter.clearSelection(); // Asegura que no haya nada seleccionado al recargar
        // Deshabilitar botones al recargar
        editCategoryButton.setEnabled(false);
        deleteCategoryButton.setEnabled(false);
    }

    // Implementación de la interfaz OnItemClickListener (del CategoryAdapter)
    @Override
    public void onItemClick(String categoryName) {
        // Este método se llama cuando se hace clic en un ítem
        // Puedes añadir lógica si necesitas alguna acción inmediata al clic,
        // pero la selección visual se maneja en onItemSelected/onItemDeselected
    }

    @Override
    public void onItemSelected(String categoryName) {
        Toast.makeText(CategoriaActivity.this, "Categoría seleccionada: " + categoryName, Toast.LENGTH_SHORT).show();
        // Habilitar botones de editar/borrar cuando se selecciona una categoría
        editCategoryButton.setEnabled(true);
        deleteCategoryButton.setEnabled(true);
    }

    @Override
    public void onItemDeselected(String categoryName) {
        Toast.makeText(CategoriaActivity.this, "Categoría deseleccionada: " + categoryName, Toast.LENGTH_SHORT).show();
        // Deshabilitar botones de editar/borrar cuando se deselecciona una categoría
        editCategoryButton.setEnabled(false);
        deleteCategoryButton.setEnabled(false);
    }

    /**
     * Muestra un diálogo para agregar una nueva categoría.
     */
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nueva Categoría");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final TextInputEditText inputName = new TextInputEditText(this);
        inputName.setHint("Nombre de la categoría");
        layout.addView(inputName);

        final RadioGroup radioGroupType = new RadioGroup(this);
        radioGroupType.setOrientation(LinearLayout.HORIZONTAL);
        radioGroupType.setPadding(0, 20, 0, 0);

        final RadioButton radioExpense = new RadioButton(this);
        radioExpense.setText(DISPLAY_TEXT_EXPENSE); // <-- Usar constante para el texto de UI
        radioExpense.setId(View.generateViewId()); // Asignar un ID único dinámicamente
        radioExpense.setChecked(true); // Gasto por defecto
        radioGroupType.addView(radioExpense);

        final RadioButton radioIncome = new RadioButton(this);
        radioIncome.setText(DISPLAY_TEXT_INCOME); // <-- Usar constante para el texto de UI
        radioIncome.setId(View.generateViewId()); // Asignar un ID único dinámicamente
        radioGroupType.addView(radioIncome);

        layout.addView(radioGroupType);
        builder.setView(layout);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String categoryName = inputName.getText().toString().trim();
            // --- CAMBIO CLAVE: Obtener el tipo de DB (EXPENSE/INCOME) para guardar ---
            String categoryTypeForDb = (radioGroupType.getCheckedRadioButtonId() == radioIncome.getId()) ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;

            if (TextUtils.isEmpty(categoryName)) {
                inputName.setError("El nombre no puede estar vacío.");
                Toast.makeText(this, "El nombre de la categoría no puede estar vacío.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar si la categoría ya existe (usando el tipo de DB)
            if (dbHelper.categoryExists(categoryName, categoryTypeForDb)) {
                Toast.makeText(this, "Error: la categoría '" + categoryName + "' (" + (categoryTypeForDb.equals(Category.TYPE_EXPENSE) ? DISPLAY_TEXT_EXPENSE : DISPLAY_TEXT_INCOME) + ") ya existe.", Toast.LENGTH_LONG).show();
                return;
            }

            // Crear el objeto Category con el tipo de DB y agregarlo
            long result = dbHelper.addCategory(new Category(0, categoryName, categoryTypeForDb)); // ID 0 para autoincremento
            if (result != -1) {
                Toast.makeText(this, "Categoría agregada: " + categoryName + " (" + (categoryTypeForDb.equals(Category.TYPE_EXPENSE) ? DISPLAY_TEXT_EXPENSE : DISPLAY_TEXT_INCOME) + ")", Toast.LENGTH_SHORT).show();
                loadCategories(); // Recarga la lista para mostrar la nueva categoría
            } else {
                Toast.makeText(this, "Error al agregar categoría.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Muestra un diálogo para editar una categoría existente.
     * @param categoryToEdit El objeto Category a editar.
     */
    private void showEditCategoryDialog(Category categoryToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Categoría");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final TextInputEditText inputName = new TextInputEditText(this);
        inputName.setHint("Nombre de la categoría");
        inputName.setText(categoryToEdit.getName()); // Pre-rellena con el nombre actual
        layout.addView(inputName);

        final RadioGroup radioGroupType = new RadioGroup(this);
        radioGroupType.setOrientation(LinearLayout.HORIZONTAL);
        radioGroupType.setPadding(0, 20, 0, 0);

        final RadioButton radioExpense = new RadioButton(this);
        radioExpense.setText(DISPLAY_TEXT_EXPENSE); // <-- Usar constante para el texto de UI
        radioExpense.setId(View.generateViewId());
        radioGroupType.addView(radioExpense);

        final RadioButton radioIncome = new RadioButton(this);
        radioIncome.setText(DISPLAY_TEXT_INCOME); // <-- Usar constante para el texto de UI
        radioIncome.setId(View.generateViewId());
        radioGroupType.addView(radioIncome);

        layout.addView(radioGroupType);
        builder.setView(layout);

        // Marcar el RadioButton según el tipo actual de la categoría (que es el tipo de DB: EXPENSE/INCOME)
        if (categoryToEdit.getType().equals(Category.TYPE_EXPENSE)) {
            radioExpense.setChecked(true);
        } else if (categoryToEdit.getType().equals(Category.TYPE_INCOME)) {
            radioIncome.setChecked(true);
        }

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String newCategoryName = inputName.getText().toString().trim();
            // --- CAMBIO CLAVE: Obtener el nuevo tipo de DB (EXPENSE/INCOME) ---
            String newCategoryTypeForDb = (radioGroupType.getCheckedRadioButtonId() == radioIncome.getId()) ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;

            if (TextUtils.isEmpty(newCategoryName)) {
                inputName.setError("El nombre no puede estar vacío.");
                Toast.makeText(this, "El nombre de la categoría no puede estar vacío.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Solo si el nombre O el tipo cambian, verificamos si la nueva combinación ya existe en la DB
            if (!newCategoryName.equals(categoryToEdit.getName()) || !newCategoryTypeForDb.equals(categoryToEdit.getType())) {
                if (dbHelper.categoryExists(newCategoryName, newCategoryTypeForDb)) {
                    Toast.makeText(this, "Error: Ya existe una categoría con ese nombre y tipo.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Actualiza el objeto Category con los nuevos valores
            categoryToEdit.setName(newCategoryName);
            categoryToEdit.setType(newCategoryTypeForDb); // Asignar el tipo de DB

            boolean success = dbHelper.updateCategory(categoryToEdit);
            if (success) {
                Toast.makeText(this, "Categoría actualizada a: " + newCategoryName + " (" + (newCategoryTypeForDb.equals(Category.TYPE_EXPENSE) ? DISPLAY_TEXT_EXPENSE : DISPLAY_TEXT_INCOME) + ")", Toast.LENGTH_SHORT).show();
                loadCategories(); // Recarga la lista
            } else {
                Toast.makeText(this, "Error al actualizar la categoría.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar una categoría.
     * @param categoryToDelete El objeto Category a eliminar.
     */
    private void showDeleteConfirmationDialog(Category categoryToDelete) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Categoría")
                // Mostrar el tipo de categoría en formato legible para el usuario en el mensaje
                .setMessage("¿Seguro que quieres eliminar '" + categoryToDelete.getName() + "' (" + (categoryToDelete.getType().equals(Category.TYPE_EXPENSE) ? DISPLAY_TEXT_EXPENSE : DISPLAY_TEXT_INCOME) + ")? Esto eliminará todas las transacciones asociadas a esta categoría.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    boolean success = dbHelper.deleteCategory(categoryToDelete.getId()); // Eliminar por ID
                    if (success) {
                        Toast.makeText(this, "Categoría eliminada: " + categoryToDelete.getName(), Toast.LENGTH_SHORT).show();
                        loadCategories();
                    } else {
                        Toast.makeText(this, "Error al eliminar categoría.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories(); // Recargar la lista de categorías cada vez que la actividad se reanuda
    }
}