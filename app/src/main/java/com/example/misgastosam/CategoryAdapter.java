package com.example.misgastosam;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout; // Importa LinearLayout
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Importa ContextCompat (para getColor)
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import android.graphics.Color;


public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<String> categoryList;
    private OnItemClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION; // Inicializa sin selección

    // Interfaz para el manejo de clics en los ítems
    public interface OnItemClickListener {
        void onItemClick(String categoryName);
        void onItemSelected(String categoryName); // Nuevo callback para cuando se selecciona un ítem
        void onItemDeselected(String categoryName); // Nuevo callback para cuando se deselecciona un ítem
    }

    // El constructor ahora recibe el listener
    public CategoryAdapter(List<String> categoryList, OnItemClickListener listener) {
        this.categoryList = categoryList;
        this.listener = listener;
    }

    // Actualiza los datos del adaptador
    public void updateData(List<String> newCategoryList) {
        this.categoryList = newCategoryList;
        this.selectedPosition = RecyclerView.NO_POSITION; // Resetea la selección al actualizar los datos
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String categoryName = categoryList.get(position);
        holder.categoryNameTextView.setText(categoryName);

        // Lógica para resaltar la selección
        if (selectedPosition == position) {
            // Si está seleccionado, aplica tu color verde manzana y texto blanco
            holder.categoryItemLayout.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.apple_green));
            holder.categoryNameTextView.setTextColor(Color.WHITE);
        } else {
            // Si no está seleccionado, restaura el fondo normal (con efecto de clic) y texto negro

            // *** ESTA ES LA LÍNEA MODIFICADA ***
            // Obtener el drawable del atributo selectableItemBackground del tema
            TypedValue outValue = new TypedValue();
            holder.itemView.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            holder.categoryItemLayout.setBackgroundResource(outValue.resourceId);
            // **********************************

           // holder.categoryNameTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.default_category_text_color));

        }

        // Configurar el click listener para cada ítem de la lista
        holder.itemView.setOnClickListener(v -> {
            // Notifica al listener que un ítem fue clickeado (manteniendo tu funcionalidad existente)
            if (listener != null) {
                listener.onItemClick(categoryName);
            }

            // Manejo de la selección visual:
            // Si el ítem clicado NO es el que ya estaba seleccionado
            if (selectedPosition != holder.getAdapterPosition()) {
                int oldSelectedPosition = selectedPosition; // Guarda la posición del ítem anterior
                selectedPosition = holder.getAdapterPosition(); // Actualiza a la nueva posición

                // Notifica al RecyclerView para redibujar el ítem anterior (deseleccionarlo)
                if (oldSelectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(oldSelectedPosition);
                    // Notifica a la actividad que el ítem anterior fue deseleccionado
                    if (listener != null) listener.onItemDeselected(categoryList.get(oldSelectedPosition));
                }
                // Notifica al RecyclerView para redibujar el ítem actual (seleccionarlo)
                notifyItemChanged(selectedPosition);
                // Notifica a la actividad que un nuevo ítem fue seleccionado
                if (listener != null) listener.onItemSelected(categoryName);
            } else {
                // Si el mismo ítem ya estaba seleccionado, lo deseleccionamos
                clearSelection();
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    // Método para obtener la categoría actualmente seleccionada (su nombre)
    public String getSelectedCategoryName() {
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < categoryList.size()) {
            return categoryList.get(selectedPosition);
        }
        return null;
    }

    // Método para deseleccionar cualquier ítem
    public void clearSelection() {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            int oldSelectedPosition = selectedPosition;
            String oldCategoryName = categoryList.get(oldSelectedPosition);
            selectedPosition = RecyclerView.NO_POSITION; // Restablecer a sin selección
            notifyItemChanged(oldSelectedPosition); // Notificar para deseleccionarlo visualmente
            // Notifica a la actividad que el ítem fue deseleccionado
            if (listener != null) listener.onItemDeselected(oldCategoryName);
        }
    }

    // ViewHolder: Representa cada elemento de la lista (cada categoría)
    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameTextView;
        LinearLayout categoryItemLayout; // Referencia al LinearLayout raíz

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameTextView = itemView.findViewById(R.id.textView_category_name);
            categoryItemLayout = itemView.findViewById(R.id.category_item_layout); // Asocia con el ID del LinearLayout
        }
    }
}