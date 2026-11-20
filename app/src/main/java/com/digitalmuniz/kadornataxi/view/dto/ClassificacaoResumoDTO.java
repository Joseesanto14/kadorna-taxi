package com.digitalmuniz.kadornataxi.view.dto;

public class ClassificacaoResumoDTO {
    private final String classificacao;
    private final int totalViagens;
    private final double totalKms;
    private final double faturamentoTotal;
    private final double percentualParticipacao;

    public ClassificacaoResumoDTO(
            String classificacao,
            int totalViagens,
            double totalKms,
            double faturamentoTotal,
            double percentualParticipacao
    ) {
        this.classificacao = classificacao;
        this.totalViagens = totalViagens;
        this.totalKms = totalKms;
        this.faturamentoTotal = faturamentoTotal;
        this.percentualParticipacao = percentualParticipacao;
    }

    public String getClassificacao() {
        return classificacao;
    }

    public int getTotalViagens() {
        return totalViagens;
    }

    public double getTotalKms() {
        return totalKms;
    }

    public double getFaturamentoTotal() {
        return faturamentoTotal;
    }

    public double getPercentualParticipacao() {
        return percentualParticipacao;
    }
}
