package com.example.misgastosam;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

public class MonthlyBalanceAdapter extends RecyclerView.Adapter<MonthlyBalanceAdapter.MonthlyBalanceViewHolder> {

    private List<MonthlyBalance> monthlyBalanceList;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");

    public MonthlyBalanceAdapter(List<MonthlyBalance> monthlyBalanceList) {
        this.monthlyBalanceList = monthlyBalanceList;
    }

    @NonNull
    @Override
    public MonthlyBalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_monthly_balance, parent, false);
        return new MonthlyBalanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthlyBalanceViewHolder holder, int position) {
        MonthlyBalance currentItem = monthlyBalanceList.get(position);

        holder.monthTextView.setText(currentItem.getDisplayMonth() + ":");

        // Formatear y colorear el saldo
        double balance = currentItem.getBalance();
        holder.amountTextView.setText("$ " + DECIMAL_FORMAT.format(balance));

        if (balance < 0) {
            holder.amountTextView.setTextColor(Color.RED);
        } else {
            holder.amountTextView.setTextColor(Color.parseColor("#4CAF50")); // Un verde genérico
        }
    }

    @Override
    public int getItemCount() {
        return monthlyBalanceList.size();
    }

    public void updateData(List<MonthlyBalance> newMonthlyBalanceList) {
        this.monthlyBalanceList.clear();
        this.monthlyBalanceList.addAll(newMonthlyBalanceList);
        notifyDataSetChanged();
    }

    static class MonthlyBalanceViewHolder extends RecyclerView.ViewHolder {
        TextView monthTextView;
        TextView amountTextView;

        public MonthlyBalanceViewHolder(@NonNull View itemView) {
            super(itemView);
            monthTextView = itemView.findViewById(R.id.textView_month);
            amountTextView = itemView.findViewById(R.id.textView_monthly_amount);
        }
    }
}