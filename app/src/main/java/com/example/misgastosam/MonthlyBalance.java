package com.example.misgastosam;

public class MonthlyBalance {
    private String monthYear; // Formato "YYYY-MM"
    private double balance;

    public MonthlyBalance(String monthYear, double balance) {
        this.monthYear = monthYear;
        this.balance = balance;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    // Opcional: Para mostrar un nombre de mes más legible
    public String getDisplayMonth() {
        // Ejemplo simple, podrías usar un formato más sofisticado
        String[] parts = monthYear.split("-");
        if (parts.length == 2) {
            int year = Integer.parseInt(parts[0]);
            int monthNum = Integer.parseInt(parts[1]);
            String monthName;
            switch (monthNum) {
                case 1: monthName = "Enero"; break;
                case 2: monthName = "Febrero"; break;
                case 3: monthName = "Marzo"; break;
                case 4: monthName = "Abril"; break;
                case 5: monthName = "Mayo"; break;
                case 6: monthName = "Junio"; break;
                case 7: monthName = "Julio"; break;
                case 8: monthName = "Agosto"; break;
                case 9: monthName = "Septiembre"; break;
                case 10: monthName = "Octubre"; break;
                case 11: monthName = "Noviembre"; break;
                case 12: monthName = "Diciembre"; break;
                default: monthName = "Mes Desconocido"; break;
            }
            return monthName + " " + year;
        }
        return monthYear; // En caso de formato inesperado
    }
}