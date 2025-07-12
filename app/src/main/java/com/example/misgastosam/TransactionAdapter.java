package com.example.misgastosam;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;
    private Context context;
    private OnItemActionListener actionListener; // Un solo listener para manejar acciones

    // Interfaz para manejar las acciones de editar/borrar fuera del adaptador
    public interface OnItemActionListener {
        void onEdit(Transaction transaction);
        void onDelete(Transaction transaction);
    }

    // Constructor del adaptador
    public TransactionAdapter(Context context, List<Transaction> transactions, OnItemActionListener actionListener) {
        this.context = context;
        this.transactions = transactions;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el layout para cada ítem de la lista
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false); // Asegúrate de que este layout exista
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        // Asignamos los datos a los TextViews en el layout del ítem
        holder.descriptionTextView.setText(transaction.getDescription());

        // <-- CAMBIO: Acceder al nombre y tipo de la categoría desde el objeto Category dentro de Transaction -->
        if (transaction.getCategory() != null) {
            // Usa el toString() de Category para el formato "Nombre (Tipo)"
            holder.categoryTextView.setText("Categoría: " + transaction.getCategory().toString());
        } else {
            holder.categoryTextView.setText("Categoría: Desconocida");
        }
        // <-- FIN CAMBIO -->

        holder.dateTextView.setText("Fecha: " + transaction.getDate()); // Usar getDate()

        // Formateamos el monto y asignamos el color según el tipo de transacción
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        String amountText;

        // Usamos el tipo de la transacción (que se deriva del tipo de la categoría)
        if (transaction.getType().equals(Transaction.TYPE_EXPENSE)) {
            holder.amountTextView.setTextColor(Color.RED);
            amountText = "-$" + decimalFormat.format(Math.abs(transaction.getAmount()));
        } else { // Transaction.TYPE_INCOME
            // Usamos un color verde (definido en colors.xml o android.R.color.holo_green_dark)
            holder.amountTextView.setTextColor(context.getResources().getColor(R.color.apple_green)); // Usar tu color
            amountText = "+$" + decimalFormat.format(transaction.getAmount());
        }
        holder.amountTextView.setText(amountText);

        // Configuramos el Listener para cuando se hace clic en TODO el ítem (la CardView o el layout raíz)
        holder.itemView.setOnClickListener(v -> {
            showActionDialog(transaction); // Mostramos el diálogo de acciones (editar/eliminar)
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    // Método para actualizar la lista de transacciones y notificar al RecyclerView
    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions.clear();
        this.transactions.addAll(newTransactions);
        notifyDataSetChanged();
    }

    /**
     * Muestra el diálogo para elegir entre editar o eliminar una transacción.
     * @param transaction La transacción seleccionada.
     */
    private void showActionDialog(final Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Acción de Transacción")
                .setMessage("¿Qué quieres hacer con esta transacción?\n" +
                        "Monto: $" + new DecimalFormat("#,##0.00").format(transaction.getAmount()) +
                        "\nDescripción: " + transaction.getDescription() +
                        "\nCategoría: " + (transaction.getCategory() != null ? transaction.getCategory().toString() : "Desconocida")) // Mostrar categoría
                .setPositiveButton("Editar", (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onEdit(transaction); // Llama al método onEdit en HistoryActivity
                    }
                })
                .setNegativeButton("Eliminar", (dialog, which) -> {
                    if (actionListener != null) {
                        actionListener.onDelete(transaction); // Llama al método onDelete en HistoryActivity
                    }
                })
                .setNeutralButton("Cancelar", (dialog, which) -> dialog.dismiss()); // Cierra el diálogo
        builder.create().show();
    }

    // ViewHolder: Contiene las referencias a las vistas de cada ítem del RecyclerView
    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView descriptionTextView;
        TextView categoryTextView;
        TextView dateTextView;
        TextView amountTextView;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            descriptionTextView = itemView.findViewById(R.id.textView_transaction_description);
            categoryTextView = itemView.findViewById(R.id.textView_transaction_category);
            dateTextView = itemView.findViewById(R.id.textView_transaction_date);
            amountTextView = itemView.findViewById(R.id.textView_transaction_amount);
        }
    }
}