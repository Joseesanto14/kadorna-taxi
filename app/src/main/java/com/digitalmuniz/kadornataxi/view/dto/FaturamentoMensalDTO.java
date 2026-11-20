package com.digitalmuniz.kadornataxi.view.dto;

public class FaturamentoMensalDTO {
    private final String mesNumero;
    private final String mesSigla;
    private final int totalViagens;
    private final double faturamentoTotal;
    private final double totalKms;
    private final double totalCustos;
    private final double lucroLiquido;

    public FaturamentoMensalDTO(
            String mesNumero,
            String mesSigla,
            int totalViagens,
            double faturamentoTotal,
            double totalKms
    ) {
        this(mesNumero, mesSigla, totalViagens, faturamentoTotal, totalKms, 0.0, faturamentoTotal);
    }

    public FaturamentoMensalDTO(
            String mesNumero,
            String mesSigla,
            int totalViagens,
            double faturamentoTotal,
            double totalKms,
            double totalCustos,
            double lucroLiquido
    ) {
        this.mesNumero = mesNumero;
        this.mesSigla = mesSigla;
        this.totalViagens = totalViagens;
        this.faturamentoTotal = faturamentoTotal;
        this.totalKms = totalKms;
        this.totalCustos = totalCustos;
        this.lucroLiquido = lucroLiquido;
    }

    public String getMesNumero() {
        return mesNumero;
    }

    public String getMesSigla() {
        return mesSigla;
    }

    public int getTotalViagens() {
        return totalViagens;
    }

    public double getFaturamentoTotal() {
        return faturamentoTotal;
    }

    public double getTotalKms() {
        return totalKms;
    }

    public double getTotalCustos() {
        return totalCustos;
    }

    public double getLucroLiquido() {
        return lucroLiquido;
    }
}
