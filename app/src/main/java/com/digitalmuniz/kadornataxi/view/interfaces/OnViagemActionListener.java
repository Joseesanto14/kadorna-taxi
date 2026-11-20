package com.digitalmuniz.kadornataxi.view.interfaces;

import com.digitalmuniz.kadornataxi.model.entities.Viagem;

public interface OnViagemActionListener {
    void onEditar(Viagem viagem);
    void onExcluir(Viagem viagem);
    default void onDetalhes(Viagem viagem) {}
}
