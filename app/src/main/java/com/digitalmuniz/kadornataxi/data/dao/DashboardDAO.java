package com.digitalmuniz.kadornataxi.data.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.util.FormatadorDashboard;
import com.digitalmuniz.kadornataxi.view.dto.ClassificacaoResumoDTO;
import com.digitalmuniz.kadornataxi.view.dto.DashboardResumoDTO;
import com.digitalmuniz.kadornataxi.view.dto.FaturamentoMensalDTO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardDAO {

    private final DbHelper dbHelper;

    public DashboardDAO(Context context) {
        this.dbHelper = DbHelper.getInstance(context);
    }

    public DashboardDAO(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<String> buscarAnosDisponiveis() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> anos = new ArrayList<>();
        String sql = "SELECT DISTINCT substr(" + DbHelper.Viagem.DATA + ", 1, 4) AS ano " +
                "FROM " + DbHelper.Viagem.NOME_TABELA + " " +
                "WHERE " + DbHelper.Viagem.DATA + " >= '0000' AND " + DbHelper.Viagem.DATA + " < '9999' " +
                "ORDER BY ano DESC";

        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor != null && cursor.moveToNext()) {
                String ano = cursor.getString(0);
                if (isAnoValido(ano)) {
                    anos.add(ano);
                }
            }
        }
        return anos;
    }

    public List<String> buscarMesesDisponiveisPorAno(String ano) {
        PeriodoIntervalo intervalo = criarIntervaloPeriodo(ano, null);
        if (intervalo == null) {
            return Collections.emptyList();
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> meses = new ArrayList<>();
        String sql = "SELECT DISTINCT substr(" + DbHelper.Viagem.DATA + ", 6, 2) AS mes " +
                "FROM " + DbHelper.Viagem.NOME_TABELA + " " +
                "WHERE " + DbHelper.Viagem.DATA + " >= ? AND " + DbHelper.Viagem.DATA + " < ? " +
                "ORDER BY mes ASC";

        try (Cursor cursor = db.rawQuery(sql, new String[]{intervalo.inicio, intervalo.fimExclusivo})) {
            while (cursor != null && cursor.moveToNext()) {
                String mes = cursor.getString(0);
                if (isMesValido(mes)) {
                    meses.add(mes);
                }
            }
        }
        return meses;
    }

    public DashboardResumoDTO buscarResumoPorPeriodo(String ano, String mesOuNull) {
        PeriodoIntervalo intervalo = criarIntervaloPeriodo(ano, mesOuNull);
        if (intervalo == null) {
            return new DashboardResumoDTO(0, 0, 0.0, 0.0, null, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sqlAtual = "SELECT " +
                "COUNT(*) AS total_viagens, " +
                "COUNT(DISTINCT " + DbHelper.Viagem.DATA + ") AS dias_trabalhados, " +
                "COALESCE(SUM(" + DbHelper.Viagem.VALOR_TOTAL + "), 0.0) AS faturamento_total, " +
                "COALESCE(SUM(" + DbHelper.Viagem.KMS_RODADOS + "), 0.0) AS total_kms, " +
                "COALESCE(SUM(" + DbHelper.Viagem.VALOR_KM + "), 0.0) AS total_valor_km, " +
                "COALESCE(SUM(" + DbHelper.Viagem.HORA_ESPERA + "), 0.0) AS total_horas_espera, " +
                "COALESCE(SUM(" + DbHelper.Viagem.VALOR_HORA_ESPERA + "), 0.0) AS total_valor_espera " +
                "FROM " + DbHelper.Viagem.NOME_TABELA + " " +
                "WHERE " + DbHelper.Viagem.DATA + " >= ? AND " + DbHelper.Viagem.DATA + " < ?";

        int totalViagens = 0;
        int diasTrabalhados = 0;
        double faturamentoTotal = 0.0;
        double totalKms = 0.0;
        double totalValorKm = 0.0;
        double totalHorasEspera = 0.0;
        double totalValorEspera = 0.0;

        try (Cursor cursor = db.rawQuery(sqlAtual, new String[]{intervalo.inicio, intervalo.fimExclusivo})) {
            if (cursor != null && cursor.moveToFirst()) {
                totalViagens = cursor.getInt(0);
                diasTrabalhados = cursor.getInt(1);
                faturamentoTotal = cursor.getDouble(2);
                totalKms = cursor.getDouble(3);
                totalValorKm = cursor.getDouble(4);
                totalHorasEspera = cursor.getDouble(5);
                totalValorEspera = cursor.getDouble(6);
            }
        }

        ServicoResumoRaw servicosResumo = buscarResumoServicosPorPeriodo(
                db, intervalo.inicio, intervalo.fimExclusivo);

        double faturamentoAnterior = buscarFaturamentoSimples(
                db, intervalo.anteriorInicio, intervalo.anteriorFimExclusivo);

        Double variacaoPercentual = faturamentoAnterior > 0.0
                ? ((faturamentoTotal - faturamentoAnterior) / faturamentoAnterior) * 100.0
                : null;

        double ticketMedio = totalViagens > 0 ? faturamentoTotal / totalViagens : 0.0;
        double mediaReaisPorKm = totalKms > 0.0 ? faturamentoTotal / totalKms : 0.0;
        double mediaDiaria = diasTrabalhados > 0 ? faturamentoTotal / diasTrabalhados : 0.0;
        double mediaPorServico = servicosResumo.totalServicos > 0
                ? servicosResumo.totalValorServico / servicosResumo.totalServicos
                : 0.0;

        return new DashboardResumoDTO(
                totalViagens,
                diasTrabalhados,
                faturamentoTotal,
                faturamentoAnterior,
                variacaoPercentual,
                ticketMedio,
                totalKms,
                mediaReaisPorKm,
                mediaDiaria,
                totalValorKm,
                totalHorasEspera,
                totalValorEspera,
                servicosResumo.totalValorServico,
                servicosResumo.totalServicos,
                servicosResumo.viagensComServico,
                mediaPorServico
        );
    }

    public List<FaturamentoMensalDTO> buscarEvolucaoMensalDoAno(String ano) {
        PeriodoIntervalo intervalo = criarIntervaloPeriodo(ano, null);
        Map<String, FaturamentoMensalDTO> mapaPorMes = new HashMap<>();

        if (intervalo != null) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String sql = "SELECT " +
                    "substr(" + DbHelper.Viagem.DATA + ", 6, 2) AS mes, " +
                    "COUNT(*) AS total_viagens, " +
                    "COALESCE(SUM(" + DbHelper.Viagem.VALOR_TOTAL + "), 0.0) AS faturamento_total, " +
                    "COALESCE(SUM(" + DbHelper.Viagem.KMS_RODADOS + "), 0.0) AS total_kms " +
                    "FROM " + DbHelper.Viagem.NOME_TABELA + " " +
                    "WHERE " + DbHelper.Viagem.DATA + " >= ? AND " + DbHelper.Viagem.DATA + " < ? " +
                    "GROUP BY mes " +
                    "ORDER BY mes ASC";

            try (Cursor cursor = db.rawQuery(sql, new String[]{intervalo.inicio, intervalo.fimExclusivo})) {
                while (cursor != null && cursor.moveToNext()) {
                    String mes = cursor.getString(0);
                    if (isMesValido(mes)) {
                        mapaPorMes.put(mes, new FaturamentoMensalDTO(
                                mes,
                                FormatadorDashboard.getSiglaMes(mes),
                                cursor.getInt(1),
                                cursor.getDouble(2),
                                cursor.getDouble(3)
                        ));
                    }
                }
            }
        }

        List<FaturamentoMensalDTO> evolucao = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            String mesNumero = String.format(Locale.US, "%02d", m);
            FaturamentoMensalDTO existente = mapaPorMes.get(mesNumero);
            if (existente != null) {
                evolucao.add(existente);
            } else {
                evolucao.add(new FaturamentoMensalDTO(
                        mesNumero,
                        FormatadorDashboard.getSiglaMes(mesNumero),
                        0,
                        0.0,
                        0.0
                ));
            }
        }
        return evolucao;
    }

    public List<ClassificacaoResumoDTO> buscarRankingClassificacao(String ano, String mesOuNull) {
        PeriodoIntervalo intervalo = criarIntervaloPeriodo(ano, mesOuNull);
        if (intervalo == null) {
            return Collections.emptyList();
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT " +
                "COALESCE(NULLIF(TRIM(" + DbHelper.Viagem.CLASSIFICACAO + "), ''), 'Comum') AS client_group, " +
                "COUNT(*) AS total_viagens, " +
                "COALESCE(SUM(" + DbHelper.Viagem.KMS_RODADOS + "), 0.0) AS total_kms, " +
                "COALESCE(SUM(" + DbHelper.Viagem.VALOR_TOTAL + "), 0.0) AS faturamento_total " +
                "FROM " + DbHelper.Viagem.NOME_TABELA + " " +
                "WHERE " + DbHelper.Viagem.DATA + " >= ? AND " + DbHelper.Viagem.DATA + " < ? " +
                "GROUP BY client_group " +
                "ORDER BY faturamento_total DESC, total_viagens DESC, client_group ASC";

        List<ClassificacaoRaw> brutos = new ArrayList<>();
        double somaTotal = 0.0;

        try (Cursor cursor = db.rawQuery(sql, new String[]{intervalo.inicio, intervalo.fimExclusivo})) {
            while (cursor != null && cursor.moveToNext()) {
                String classificacao = cursor.getString(0);
                int totalViagens = cursor.getInt(1);
                double totalKms = cursor.getDouble(2);
                double faturamento = cursor.getDouble(3);
                somaTotal += faturamento;
                brutos.add(new ClassificacaoRaw(classificacao, totalViagens, totalKms, faturamento));
            }
        }

        List<ClassificacaoResumoDTO> ranking = new ArrayList<>(brutos.size());
        for (ClassificacaoRaw item : brutos) {
            double percentual = somaTotal > 0.0 ? (item.faturamentoTotal / somaTotal) * 100.0 : 0.0;
            ranking.add(new ClassificacaoResumoDTO(
                    item.classificacao,
                    item.totalViagens,
                    item.totalKms,
                    item.faturamentoTotal,
                    percentual
            ));
        }
        return ranking;
    }

    private double buscarFaturamentoSimples(SQLiteDatabase db, String inicio, String fimExclusivo) {
        String sql = "SELECT COALESCE(SUM(" + DbHelper.Viagem.VALOR_TOTAL + "), 0.0) " +
                "FROM " + DbHelper.Viagem.NOME_TABELA + " " +
                "WHERE " + DbHelper.Viagem.DATA + " >= ? AND " + DbHelper.Viagem.DATA + " < ?";
        try (Cursor cursor = db.rawQuery(sql, new String[]{inicio, fimExclusivo})) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }
        }
        return 0.0;
    }

    private ServicoResumoRaw buscarResumoServicosPorPeriodo(SQLiteDatabase db, String inicio, String fimExclusivo) {
        String sql = "SELECT " +
                "COUNT(*), " +
                "COUNT(DISTINCT s." + DbHelper.Servico.VIAGEM_ID + "), " +
                "COALESCE(SUM(s." + DbHelper.Servico.VALOR + "), 0.0) " +
                "FROM " + DbHelper.Servico.NOME_TABELA + " s " +
                "INNER JOIN " + DbHelper.Viagem.NOME_TABELA + " v ON v." + DbHelper.Viagem.ID + " = s." + DbHelper.Servico.VIAGEM_ID + " " +
                "WHERE v." + DbHelper.Viagem.DATA + " >= ? AND v." + DbHelper.Viagem.DATA + " < ?";
        try (Cursor cursor = db.rawQuery(sql, new String[]{inicio, fimExclusivo})) {
            if (cursor != null && cursor.moveToFirst()) {
                return new ServicoResumoRaw(cursor.getInt(0), cursor.getInt(1), cursor.getDouble(2));
            }
        }
        return new ServicoResumoRaw(0, 0, 0.0);
    }

    private PeriodoIntervalo criarIntervaloPeriodo(String ano, String mesOuNull) {
        if (!isAnoValido(ano)) {
            return null;
        }
        int anoInt = Integer.parseInt(ano.trim());
        if (mesOuNull != null && !mesOuNull.trim().isEmpty()) {
            if (!isMesValido(mesOuNull)) {
                return null;
            }
            int mesInt = Integer.parseInt(mesOuNull.trim());
            YearMonth ymAtual = YearMonth.of(anoInt, mesInt);
            YearMonth ymAnterior = ymAtual.minusMonths(1);
            return new PeriodoIntervalo(
                    ymAtual.atDay(1).toString(),
                    ymAtual.plusMonths(1).atDay(1).toString(),
                    ymAnterior.atDay(1).toString(),
                    ymAnterior.plusMonths(1).atDay(1).toString()
            );
        }

        LocalDate inicioAno = LocalDate.of(anoInt, 1, 1);
        LocalDate fimAno = LocalDate.of(anoInt + 1, 1, 1);
        LocalDate inicioAnoAnterior = LocalDate.of(anoInt - 1, 1, 1);
        return new PeriodoIntervalo(
                inicioAno.toString(),
                fimAno.toString(),
                inicioAnoAnterior.toString(),
                inicioAno.toString()
        );
    }

    private static boolean isAnoValido(String ano) {
        if (ano == null) {
            return false;
        }
        String trimmed = ano.trim();
        if (trimmed.length() != 4) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        int valor = Integer.parseInt(trimmed);
        return valor >= 1900 && valor <= 9998;
    }

    private static boolean isMesValido(String mes) {
        if (mes == null) {
            return false;
        }
        String trimmed = mes.trim();
        if (trimmed.length() != 2 || !Character.isDigit(trimmed.charAt(0)) || !Character.isDigit(trimmed.charAt(1))) {
            return false;
        }
        int valor = Integer.parseInt(trimmed);
        return valor >= 1 && valor <= 12;
    }

    private static class PeriodoIntervalo {
        final String inicio;
        final String fimExclusivo;
        final String anteriorInicio;
        final String anteriorFimExclusivo;

        PeriodoIntervalo(String inicio, String fimExclusivo, String anteriorInicio, String anteriorFimExclusivo) {
            this.inicio = inicio;
            this.fimExclusivo = fimExclusivo;
            this.anteriorInicio = anteriorInicio;
            this.anteriorFimExclusivo = anteriorFimExclusivo;
        }
    }

    private static class ServicoResumoRaw {
        final int totalServicos;
        final int viagensComServico;
        final double totalValorServico;

        ServicoResumoRaw(int totalServicos, int viagensComServico, double totalValorServico) {
            this.totalServicos = totalServicos;
            this.viagensComServico = viagensComServico;
            this.totalValorServico = totalValorServico;
        }
    }

    private static class ClassificacaoRaw {
        final String classificacao;
        final int totalViagens;
        final double totalKms;
        final double faturamentoTotal;

        ClassificacaoRaw(String classificacao, int totalViagens, double totalKms, double faturamentoTotal) {
            this.classificacao = classificacao;
            this.totalViagens = totalViagens;
            this.totalKms = totalKms;
            this.faturamentoTotal = faturamentoTotal;
        }
    }
}
