package com.digitalmuniz.kadornataxi.view.dto;

import com.digitalmuniz.kadornataxi.model.entities.Viagem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaginaViagensResult {
    private final List<Viagem> itens;
    private final boolean temMais;
    private final ViagemCursorKey proximoCursor;

    public PaginaViagensResult(List<Viagem> itens, boolean temMais, ViagemCursorKey proximoCursor) {
        this.itens = itens != null ? Collections.unmodifiableList(new ArrayList<>(itens)) : Collections.emptyList();
        this.temMais = temMais;
        this.proximoCursor = proximoCursor;
    }

    public List<Viagem> getItens() {
        return itens;
    }

    public boolean isTemMais() {
        return temMais;
    }

    public ViagemCursorKey getProximoCursor() {
        return proximoCursor;
    }
}
