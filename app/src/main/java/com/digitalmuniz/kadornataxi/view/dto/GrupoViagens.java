package com.digitalmuniz.kadornataxi.view.dto;

import java.util.Objects;

public class GrupoViagens {
    private final String mesAno;
    private final int totalViagens;

    public GrupoViagens(String mesAno, int totalViagens) {
        this.mesAno = mesAno != null ? mesAno : "";
        this.totalViagens = totalViagens;
    }

    public String getMesAno() {
        return mesAno;
    }

    public int getTotalViagens() {
        return totalViagens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GrupoViagens)) return false;
        GrupoViagens that = (GrupoViagens) o;
        return totalViagens == that.totalViagens && Objects.equals(mesAno, that.mesAno);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mesAno, totalViagens);
    }
}
