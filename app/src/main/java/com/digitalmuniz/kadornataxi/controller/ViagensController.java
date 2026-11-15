package com.digitalmuniz.kadornataxi.controller;

import android.content.Context;

import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.dto.MesViagens;

import java.util.ArrayList;
import java.util.List;

public class ViagensController {
    /**
     * Controller for the ViagensActivity filter logic.
     *
     */

    // ---------- ATTRIBUTES ----------
    private final ViagemDAO viagemDAO;
    private final ConfiguracaoDAO configuracaoDAO;
    private List<Viagem> viagensEmMemoria;
    private Configuracao configuracao;

    // ---------- CONSTRUCTORS ----------
    public ViagensController(Context context) {
        this.viagemDAO = new ViagemDAO(context);
        this.configuracaoDAO = new ConfiguracaoDAO(context);
    }

    // ---------- METHODS ----------
    public void carregarDados() {
        this.viagensEmMemoria = viagemDAO.getAllOrdenadoPorDataDesc();
    }

    public void carregarConfiguracoes() {
        this.configuracao = configuracaoDAO.getConfiguracao();
    }

    public List<MesViagens> filtrarEAgrupar(String textoBusca, boolean filtrarApenasSeparadas) {
        List<Viagem> listaFiltrada = new ArrayList<>();
        String busca = textoBusca.toLowerCase().trim();

        for (Viagem v : viagensEmMemoria) {
            boolean matchesBusca = busca.isEmpty() ||
                    (v.getClassificacao() != null && v.getClassificacao().toLowerCase().contains(busca)) ||
                    (v.getDescricao() != null && v.getDescricao().toLowerCase().contains(busca)) ||
                    (v.getSolicitante() != null && v.getSolicitante().toLowerCase().contains(busca));

            boolean matchesChip = !filtrarApenasSeparadas ||
                    (v.getClassificacao() != null && !v.getClassificacao().equals("Comum"));

            if (matchesBusca && matchesChip) {
                listaFiltrada.add(v);
            }
        }

        return Viagem.gerarListaDeMeses(listaFiltrada);
    }

    public boolean configuracaoExiste() {
        return configuracaoDAO.configExiste();
    }
}
