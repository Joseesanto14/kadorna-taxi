package com.digitalmuniz.kadornataxi.view.dto;

import java.util.Objects;

public class ViagemCursorKey {
    private final String data;
    private final String hora;
    private final long id;

    public ViagemCursorKey(String data, String hora, long id) {
        this.data = data != null ? data : "";
        this.hora = hora != null ? hora : "";
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public String getHora() {
        return hora;
    }

    public long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ViagemCursorKey)) return false;
        ViagemCursorKey that = (ViagemCursorKey) o;
        return id == that.id && Objects.equals(data, that.data) && Objects.equals(hora, that.hora);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, hora, id);
    }
}
