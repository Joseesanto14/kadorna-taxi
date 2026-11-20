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
}
