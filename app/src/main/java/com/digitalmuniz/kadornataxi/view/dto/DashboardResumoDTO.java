package com.digitalmuniz.kadornataxi.view.dto;

public class DashboardResumoDTO {
    private final int totalViagens;
    private final int diasTrabalhados;
    private final double faturamentoTotal;
    private final double faturamentoPeriodoAnterior;
    private final Double variacaoPercentual;
    private final double ticketMedio;
    private final double totalKms;
    private final double mediaReaisPorKm;
    private final double mediaDiaria;
    private final double totalValorKm;
    private final double totalHorasEspera;
    private final double totalValorEspera;
    private final double totalValorServico;
    private final int totalServicos;
    private final int viagensComServico;
    private final double mediaPorServico;
    private final double totalCustos;
    private final int totalRegistrosCusto;
    private final double custosPeriodoAnterior;
    private final double lucroLiquido;
    private final double lucroPeriodoAnterior;
    private final Double margemLiquidaPercentual;
    private final Double variacaoLucroPercentual;
    private final Double variacaoCustosPercentual;

    public DashboardResumoDTO(
            int totalViagens,
            int diasTrabalhados,
            double faturamentoTotal,
            double faturamentoPeriodoAnterior,
            Double variacaoPercentual,
            double ticketMedio,
            double totalKms,
            double mediaReaisPorKm,
            double mediaDiaria,
            double totalValorKm,
            double totalHorasEspera,
            double totalValorEspera,
            double totalValorServico
    ) {
        this(
                totalViagens,
                diasTrabalhados,
                faturamentoTotal,
                faturamentoPeriodoAnterior,
                variacaoPercentual,
                ticketMedio,
                totalKms,
                mediaReaisPorKm,
                mediaDiaria,
                totalValorKm,
                totalHorasEspera,
                totalValorEspera,
                totalValorServico,
                0,
                0,
                0.0
        );
    }

    public DashboardResumoDTO(
            int totalViagens,
            int diasTrabalhados,
            double faturamentoTotal,
            double faturamentoPeriodoAnterior,
            Double variacaoPercentual,
            double ticketMedio,
            double totalKms,
            double mediaReaisPorKm,
            double mediaDiaria,
            double totalValorKm,
            double totalHorasEspera,
            double totalValorEspera,
            double totalValorServico,
            int totalServicos,
            int viagensComServico,
            double mediaPorServico
    ) {
        this(
                totalViagens,
                diasTrabalhados,
                faturamentoTotal,
                faturamentoPeriodoAnterior,
                variacaoPercentual,
                ticketMedio,
                totalKms,
                mediaReaisPorKm,
                mediaDiaria,
                totalValorKm,
                totalHorasEspera,
                totalValorEspera,
                totalValorServico,
                totalServicos,
                viagensComServico,
                mediaPorServico,
                0.0,
                0,
                0.0,
                faturamentoTotal,
                faturamentoPeriodoAnterior,
                faturamentoTotal > 0.0 ? 100.0 : null,
                variacaoPercentual,
                null
        );
    }

    public DashboardResumoDTO(
            int totalViagens,
            int diasTrabalhados,
            double faturamentoTotal,
            double faturamentoPeriodoAnterior,
            Double variacaoPercentual,
            double ticketMedio,
            double totalKms,
            double mediaReaisPorKm,
            double mediaDiaria,
            double totalValorKm,
            double totalHorasEspera,
            double totalValorEspera,
            double totalValorServico,
            int totalServicos,
            int viagensComServico,
            double mediaPorServico,
            double totalCustos,
            int totalRegistrosCusto,
            double custosPeriodoAnterior,
            double lucroLiquido,
            double lucroPeriodoAnterior,
            Double margemLiquidaPercentual,
            Double variacaoLucroPercentual,
            Double variacaoCustosPercentual
    ) {
        this.totalViagens = totalViagens;
        this.diasTrabalhados = diasTrabalhados;
        this.faturamentoTotal = faturamentoTotal;
        this.faturamentoPeriodoAnterior = faturamentoPeriodoAnterior;
        this.variacaoPercentual = variacaoPercentual;
        this.ticketMedio = ticketMedio;
        this.totalKms = totalKms;
        this.mediaReaisPorKm = mediaReaisPorKm;
        this.mediaDiaria = mediaDiaria;
        this.totalValorKm = totalValorKm;
        this.totalHorasEspera = totalHorasEspera;
        this.totalValorEspera = totalValorEspera;
        this.totalValorServico = totalValorServico;
        this.totalServicos = totalServicos;
        this.viagensComServico = viagensComServico;
        this.mediaPorServico = mediaPorServico;
        this.totalCustos = totalCustos;
        this.totalRegistrosCusto = totalRegistrosCusto;
        this.custosPeriodoAnterior = custosPeriodoAnterior;
        this.lucroLiquido = lucroLiquido;
        this.lucroPeriodoAnterior = lucroPeriodoAnterior;
        this.margemLiquidaPercentual = margemLiquidaPercentual;
        this.variacaoLucroPercentual = variacaoLucroPercentual;
        this.variacaoCustosPercentual = variacaoCustosPercentual;
    }

    public int getTotalViagens() {
        return totalViagens;
    }

    public int getDiasTrabalhados() {
        return diasTrabalhados;
    }

    public double getFaturamentoTotal() {
        return faturamentoTotal;
    }

    public double getFaturamentoPeriodoAnterior() {
        return faturamentoPeriodoAnterior;
    }

    public Double getVariacaoPercentual() {
        return variacaoPercentual;
    }

    public double getTicketMedio() {
        return ticketMedio;
    }

    public double getTotalKms() {
        return totalKms;
    }

    public double getMediaReaisPorKm() {
        return mediaReaisPorKm;
    }

    public double getMediaDiaria() {
        return mediaDiaria;
    }

    public double getTotalValorKm() {
        return totalValorKm;
    }

    public double getTotalHorasEspera() {
        return totalHorasEspera;
    }

    public double getTotalValorEspera() {
        return totalValorEspera;
    }

    public double getTotalValorServico() {
        return totalValorServico;
    }

    public int getTotalServicos() {
        return totalServicos;
    }

    public int getViagensComServico() {
        return viagensComServico;
    }

    public double getMediaPorServico() {
        return mediaPorServico;
    }

    public double getTotalCustos() {
        return totalCustos;
    }

    public int getTotalRegistrosCusto() {
        return totalRegistrosCusto;
    }

    public double getCustosPeriodoAnterior() {
        return custosPeriodoAnterior;
    }

    public double getLucroLiquido() {
        return lucroLiquido;
    }

    public double getLucroPeriodoAnterior() {
        return lucroPeriodoAnterior;
    }

    public Double getMargemLiquidaPercentual() {
        return margemLiquidaPercentual;
    }

    public Double getVariacaoLucroPercentual() {
        return variacaoLucroPercentual;
    }

    public Double getVariacaoCustosPercentual() {
        return variacaoCustosPercentual;
    }
}
