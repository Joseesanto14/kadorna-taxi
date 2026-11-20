package com.digitalmuniz.kadornataxi.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class FormatadorDashboard {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private static final String[] NOMES_MESES = {
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private static final String[] SIGLAS_MESES = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private FormatadorDashboard() {
    }

    private static DecimalFormat criarDecimalFormat(String pattern) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(PT_BR);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat(pattern, symbols);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    }

    public static String formatarMoeda(double valor) {
        double sane = sanitizeZero(valor, 0.005);
        DecimalFormat df = criarDecimalFormat("#,##0.00");
        if (sane < 0.0) {
            return "-R$ " + df.format(Math.abs(sane));
        }
        return "R$ " + df.format(sane);
    }

    public static String formatarKms(double kms) {
        double sane = sanitizeZero(kms, 0.05);
        DecimalFormat df = criarDecimalFormat("#,##0.0");
        return df.format(Math.max(0.0, sane)) + " km";
    }

    public static String formatarReaisPorKm(double reaisPorKm) {
        return formatarMoeda(Math.max(0.0, reaisPorKm)) + "/km";
    }

    public static String formatarDuracaoEspera(double horas) {
        long totalMinutos = Math.round(Math.max(0.0, horas) * 60.0);
        long h = totalMinutos / 60;
        long m = totalMinutos % 60;
        return String.format(PT_BR, "%dh %02dm", h, m);
    }

    public static String formatarVariacaoPercentual(Double variacao) {
        if (variacao == null || variacao.isNaN() || variacao.isInfinite()) {
            return "Sem base anterior";
        }
        double sane = sanitizeZero(variacao, 0.05);
        DecimalFormat df = criarDecimalFormat("0.0");
        if (sane > 0.0) {
            return "+" + df.format(sane) + "%";
        }
        if (sane < 0.0) {
            return "-" + df.format(Math.abs(sane)) + "%";
        }
        return "0,0%";
    }

    public static String formatarPercentual(double percentual) {
        double sane = sanitizeZero(percentual, 0.05);
        DecimalFormat df = criarDecimalFormat("0.0");
        return df.format(Math.max(0.0, sane)) + "%";
    }

    public static String abreviarParaGrafico(double valor) {
        if (valor <= 0.0) {
            return "0";
        }
        if (valor >= 1_000_000.0) {
            DecimalFormat df = criarDecimalFormat("#.#");
            return df.format(valor / 1_000_000.0) + "M";
        }
        if (valor >= 1_000.0) {
            DecimalFormat df = criarDecimalFormat("#.#");
            return df.format(valor / 1_000.0) + "k";
        }
        return String.valueOf(Math.round(valor));
    }

    public static String getSiglaMes(String mesNumero) {
        int indice = parseMesIndice(mesNumero);
        return indice >= 0 ? SIGLAS_MESES[indice] : "";
    }

    public static String getNomeMes(String mesNumero) {
        int indice = parseMesIndice(mesNumero);
        return indice >= 0 ? NOMES_MESES[indice] : "";
    }

    public static String formatarPeriodoTitulo(String ano, String mesOuNull) {
        if (ano == null || ano.trim().isEmpty()) {
            return "";
        }
        if (mesOuNull == null || mesOuNull.trim().isEmpty()) {
            return "Ano " + ano;
        }
        String nomeMes = getNomeMes(mesOuNull);
        return nomeMes.isEmpty() ? ano : nomeMes + " " + ano;
    }

    private static int parseMesIndice(String mesNumero) {
        if (mesNumero == null) {
            return -1;
        }
        try {
            int mes = Integer.parseInt(mesNumero.trim());
            if (mes >= 1 && mes <= 12) {
                return mes - 1;
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    private static double sanitizeZero(double valor, double epsilon) {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || Math.abs(valor) < epsilon) {
            return 0.0;
        }
        return valor;
    }
}
