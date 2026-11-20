package com.digitalmuniz.kadornataxi.view.dto;

public class FaturamentoMensalDTO {
    private final String mesNumero;
    private final String mesSigla;
    private final int totalViagens;
    private final double faturamentoTotal;
    private final double totalKms;

    public FaturamentoMensalDTO(
            String mesNumero,
            String mesSigla,
            int totalViagens,
            double faturamentoTotal,
            double totalKms
    ) {
        this.mesNumero = mesNumero;
        this.mesSigla = mesSigla;
        this.totalViagens = totalViagens;
        this.faturamentoTotal = faturamentoTotal;
        this.totalKms = totalKms;
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
}
