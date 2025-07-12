package com.example.misgastosam;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button addButton; // boton de transaccion (tu botón '+')
    private Button categoriasButton; // boton de categoria
    private Button buttonHistory; // boton de historial
    private TextView balanceTextView; // para el texto del saldo
    private DatabaseHelper dbHelper; // Declarar la instancia del databaseHelper

    // Para mostrar el saldo cada mes (nombre de variable corregido)
    private RecyclerView monthlyBalanceRecyclerView;
    private MonthlyBalanceAdapter monthlyBalanceAdapter; // Asumo que tienes esta clase
    private List<MonthlyBalance> monthlyBalanceList; // Asumo que tienes esta clase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Inicializar el DatabaseHelper (antes de usarlo)
        dbHelper = new DatabaseHelper(this);
        balanceTextView = findViewById(R.id.textView_balance); // Conectamos el texto del saldo

        // Inicializar botones con sus IDs
        addButton = findViewById(R.id.button); // Tu botón flotante '+'
        categoriasButton = findViewById(R.id.button_categorias);
        buttonHistory = findViewById(R.id.button_history);

        // Tabla Mensual
        monthlyBalanceRecyclerView = findViewById(R.id.recyclerView_monthly_balance);
        monthlyBalanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        monthlyBalanceList = new ArrayList<>();
        // El MonthlyBalanceAdapter necesitará recibir la lista y quizás un contexto
        // Asegúrate de que esta clase exista y esté correctamente implementada.
        monthlyBalanceAdapter = new MonthlyBalanceAdapter(monthlyBalanceList);
        monthlyBalanceRecyclerView.setAdapter(monthlyBalanceAdapter);


        // Configurar OnClickListeners
        // Botón Transacciones (tu botón '+')
        if (addButton != null) {
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Lanza la actividad Transaccion.class
                    Intent intent = new Intent(MainActivity.this, Transaccion.class);
                    startActivity(intent);
                }
            });
        }

        // Botón Categorías
        if (categoriasButton != null) {
            categoriasButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, CategoriaActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Botón Historial
        if (buttonHistory != null) {
            buttonHistory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Actualizamos el saldo inicial y la tabla mensual al iniciar la actividad
        updateBalance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Se llama a updateBalance para refrescar el saldo y la tabla mensual cada vez que la actividad se reanuda
        updateBalance();
    }

    private void updateBalance() {
        if (dbHelper != null && balanceTextView != null) {
            double currentBalance = dbHelper.getTotalBalance(); // Obtiene el saldo total de la DB
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00"); // Formato con 2 decimales
            String formattedBalance = decimalFormat.format(currentBalance);
            balanceTextView.setText("Saldo: $ " + formattedBalance);

            // Colorear el saldo total
            if (currentBalance < 0) {
                balanceTextView.setTextColor(Color.RED);
            } else {
                balanceTextView.setTextColor(getResources().getColor(R.color.apple_green)); // Usar el color definido en colors.xml
            }
        }
        // Llama a este método para cargar y actualizar los saldos mensuales
        loadMonthlyBalances();
    }

    /**
     * Método para cargar y mostrar los saldos de los últimos 5 meses.
     * Asume que MonthlyBalance y MonthlyBalanceAdapter están correctamente implementados.
     */
    private void loadMonthlyBalances() {
        List<MonthlyBalance> newBalances = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

        // Obtener los últimos 5 meses
        for (int i = 0; i < 5; i++) {
            String monthYear = sdf.format(calendar.getTime());
            double balance = dbHelper.getBalanceForMonth(monthYear); // Llama al método en DatabaseHelper
            newBalances.add(new MonthlyBalance(monthYear, balance)); // Asume constructor MonthlyBalance(String, double)
            calendar.add(Calendar.MONTH, -1); // Retrocede un mes
        }
        monthlyBalanceAdapter.updateData(newBalances); // Asume que MonthlyBalanceAdapter tiene un método updateData
    }
}