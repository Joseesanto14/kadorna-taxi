package com.digitalmuniz.kadornataxi.model.entities;

import java.io.Serializable;
import java.util.Objects;

public class Custo implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String descricao = "";
    private double valor;
    private String data = "";

    public Custo() {
    }

    public Custo(String descricao, double valor, String data) {
        this(0L, descricao, valor, data);
    }

    public Custo(long id, String descricao, double valor, String data) {
        this.id = id;
        setDescricao(descricao);
        setValor(valor);
        setData(data);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao != null ? descricao : "";
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao != null ? descricao : "";
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            this.valor = 0.0;
        } else {
            this.valor = valor;
        }
    }

    public String getData() {
        return data != null ? data : "";
    }

    public void setData(String data) {
        String formatada = Viagem.formatarDataISO8601(data);
        this.data = formatada != null ? formatada : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Custo)) {
            return false;
        }
        Custo other = (Custo) o;
        return id == other.id
                && Double.compare(other.valor, valor) == 0
                && Objects.equals(getDescricao(), other.getDescricao())
                && Objects.equals(getData(), other.getData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getDescricao(), valor, getData());
    }
}
