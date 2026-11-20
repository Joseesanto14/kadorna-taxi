package com.digitalmuniz.kadornataxi.view.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardDadosDTO {
    private final List<String> anosDisponiveis;
    private final List<String> mesesDisponiveis;
    private final String anoSelecionado;
    private final String mesSelecionado;
    private final DashboardResumoDTO resumo;
    private final List<FaturamentoMensalDTO> evolucaoAnual;
    private final List<ClassificacaoResumoDTO> rankingClientes;

    public DashboardDadosDTO(
            List<String> anosDisponiveis,
            List<String> mesesDisponiveis,
            String anoSelecionado,
            String mesSelecionado,
            DashboardResumoDTO resumo,
            List<FaturamentoMensalDTO> evolucaoAnual,
            List<ClassificacaoResumoDTO> rankingClientes
    ) {
        this.anosDisponiveis = anosDisponiveis != null
                ? Collections.unmodifiableList(new ArrayList<>(anosDisponiveis))
                : Collections.emptyList();
        this.mesesDisponiveis = mesesDisponiveis != null
                ? Collections.unmodifiableList(new ArrayList<>(mesesDisponiveis))
                : Collections.emptyList();
        this.anoSelecionado = anoSelecionado;
        this.mesSelecionado = mesSelecionado;
        this.resumo = resumo;
        this.evolucaoAnual = evolucaoAnual != null
                ? Collections.unmodifiableList(new ArrayList<>(evolucaoAnual))
                : Collections.emptyList();
        this.rankingClientes = rankingClientes != null
                ? Collections.unmodifiableList(new ArrayList<>(rankingClientes))
                : Collections.emptyList();
    }

    public List<String> getAnosDisponiveis() {
        return anosDisponiveis;
    }

    public List<String> getMesesDisponiveis() {
        return mesesDisponiveis;
    }

    public String getAnoSelecionado() {
        return anoSelecionado;
    }

    public String getMesSelecionado() {
        return mesSelecionado;
    }

    public DashboardResumoDTO getResumo() {
        return resumo;
    }

    public List<FaturamentoMensalDTO> getEvolucaoAnual() {
        return evolucaoAnual;
    }

    public List<ClassificacaoResumoDTO> getRankingClientes() {
        return rankingClientes;
    }
}
