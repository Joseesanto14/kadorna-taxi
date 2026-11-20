package com.digitalmuniz.kadornataxi.view.dto;

import com.digitalmuniz.kadornataxi.model.entities.Viagem;

import java.util.Objects;

public class ListaViagensItem {
    public static final int TYPE_HEADER_GRUPO = 0;
    public static final int TYPE_BOTAO_PDF = 1;
    public static final int TYPE_VIAGEM = 2;
    public static final int TYPE_LOADING = 3;

    private final int viewType;
    private final String stableId;
    private final String mesAno;
    private final int totalViagens;
    private final boolean expandido;
    private final Viagem viagem;

    private ListaViagensItem(
            int viewType,
            String stableId,
            String mesAno,
            int totalViagens,
            boolean expandido,
            Viagem viagem
    ) {
        this.viewType = viewType;
        this.stableId = stableId;
        this.mesAno = mesAno;
        this.totalViagens = totalViagens;
        this.expandido = expandido;
        this.viagem = viagem;
    }

    public static ListaViagensItem headerGrupo(String mesAno, int totalViagens, boolean expandido) {
        return new ListaViagensItem(
                TYPE_HEADER_GRUPO,
                "header:" + mesAno,
                mesAno,
                totalViagens,
                expandido,
                null
        );
    }

    public static ListaViagensItem botaoPdf(String mesAno) {
        return new ListaViagensItem(
                TYPE_BOTAO_PDF,
                "pdf:" + mesAno,
                mesAno,
                0,
                false,
                null
        );
    }

    public static ListaViagensItem viagem(String mesAno, Viagem viagem) {
        long viagemId = viagem != null ? viagem.getId() : 0L;
        return new ListaViagensItem(
                TYPE_VIAGEM,
                "viagem:" + mesAno + ":" + viagemId,
                mesAno,
                0,
                false,
                viagem
        );
    }

    public static ListaViagensItem loading(String mesAno) {
        return new ListaViagensItem(
                TYPE_LOADING,
                "loading:" + mesAno,
                mesAno,
                0,
                false,
                null
        );
    }

    public int getViewType() {
        return viewType;
    }

    public String getStableId() {
        return stableId;
    }

    public String getMesAno() {
        return mesAno;
    }

    public int getTotalViagens() {
        return totalViagens;
    }

    public boolean isExpandido() {
        return expandido;
    }

    public Viagem getViagem() {
        return viagem;
    }

    public boolean areContentsTheSame(ListaViagensItem other) {
        if (other == null || viewType != other.viewType) {
            return false;
        }
        if (!Objects.equals(mesAno, other.mesAno)
                || totalViagens != other.totalViagens
                || expandido != other.expandido) {
            return false;
        }
        if (viewType != TYPE_VIAGEM) {
            return true;
        }
        if (viagem == other.viagem) {
            return true;
        }
        if (viagem == null || other.viagem == null) {
            return false;
        }
        return viagem.getId() == other.viagem.getId()
                && Float.compare(viagem.getKmsRodados(), other.viagem.getKmsRodados()) == 0
                && Float.compare(viagem.getValorTotal(), other.viagem.getValorTotal()) == 0
                && Float.compare(viagem.getValorTotalServicos(), other.viagem.getValorTotalServicos()) == 0
                && Objects.equals(viagem.getData(), other.viagem.getData())
                && Objects.equals(viagem.getHora(), other.viagem.getHora())
                && Objects.equals(viagem.getOrigem(), other.viagem.getOrigem())
                && Objects.equals(viagem.getDestino(), other.viagem.getDestino())
                && Objects.equals(viagem.getDescricao(), other.viagem.getDescricao())
                && Objects.equals(viagem.getSolicitante(), other.viagem.getSolicitante())
                && Objects.equals(viagem.getClassificacao(), other.viagem.getClassificacao())
                && Objects.equals(viagem.getMotorista(), other.viagem.getMotorista());
    }
}
