package com.digitalmuniz.kadornataxi.model.entities;

import java.io.Serializable;
import java.util.Objects;

public class Servico implements Serializable {
    private long id;
    private long viagemId;
    private String descricao = "";
    private float valor;

    public Servico() {
    }

    public Servico(String descricao, float valor) {
        this(0L, 0L, descricao, valor);
    }

    public Servico(long id, long viagemId, String descricao, float valor) {
        this.id = id;
        this.viagemId = viagemId;
        this.descricao = descricao != null ? descricao : "";
        this.valor = valor;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getViagemId() {
        return viagemId;
    }

    public void setViagemId(long viagemId) {
        this.viagemId = viagemId;
    }

    public String getDescricao() {
        return descricao != null ? descricao : "";
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao != null ? descricao : "";
    }

    public float getValor() {
        return valor;
    }

    public void setValor(float valor) {
        this.valor = valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Servico)) return false;
        Servico servico = (Servico) o;
        return id == servico.id
                && viagemId == servico.viagemId
                && Float.compare(servico.valor, valor) == 0
                && Objects.equals(getDescricao(), servico.getDescricao());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, viagemId, getDescricao(), valor);
    }
}
