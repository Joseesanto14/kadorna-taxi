package com.digitalmuniz.kadornataxi.view.dto;

import java.util.Objects;

public class ViagensFiltro {
    private final String textoBusca;
    private final boolean somenteEmpresas;

    public ViagensFiltro(String textoBusca, boolean somenteEmpresas) {
        this.textoBusca = textoBusca != null ? textoBusca.trim() : "";
        this.somenteEmpresas = somenteEmpresas;
    }

    public String getTextoBusca() {
        return textoBusca;
    }

    public boolean isSomenteEmpresas() {
        return somenteEmpresas;
    }

    public boolean hasTextoBusca() {
        return !textoBusca.isEmpty();
    }

    /**
     * Escapa caracteres especiais do operador SQLite LIKE usando '\' como caractere de escape.
     */
    public static String escapeLike(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ViagensFiltro)) return false;
        ViagensFiltro that = (ViagensFiltro) o;
        return somenteEmpresas == that.somenteEmpresas && Objects.equals(textoBusca, that.textoBusca);
    }

    @Override
    public int hashCode() {
        return Objects.hash(textoBusca, somenteEmpresas);
    }
}
