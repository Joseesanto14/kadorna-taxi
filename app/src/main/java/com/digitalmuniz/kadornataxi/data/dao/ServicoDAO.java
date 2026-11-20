package com.digitalmuniz.kadornataxi.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Servico;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class ServicoDAO {
    private static final int BATCH_CHUNK_SIZE = 500;
    private final DbHelper dbHelper;

    public ServicoDAO(Context context) {
        this.dbHelper = DbHelper.getInstance(context);
    }

    public ServicoDAO(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long inserir(SQLiteDatabase db, long viagemId, Servico servico) {
        if (servico == null) {
            return -1L;
        }
        ContentValues values = new ContentValues();
        values.put(DbHelper.Servico.VIAGEM_ID, viagemId);
        values.put(DbHelper.Servico.DESCRICAO, servico.getDescricao().trim());
        values.put(DbHelper.Servico.VALOR, servico.getValor());

        long id = db.insert(DbHelper.Servico.NOME_TABELA, null, values);
        if (id != -1L) {
            servico.setId(id);
            servico.setViagemId(viagemId);
        }
        return id;
    }

    public void substituirServicosDaViagem(SQLiteDatabase db, long viagemId, List<Servico> servicos) {
        db.delete(
                DbHelper.Servico.NOME_TABELA,
                DbHelper.Servico.VIAGEM_ID + " = ?",
                new String[]{String.valueOf(viagemId)}
        );
        if (servicos == null || servicos.isEmpty()) {
            return;
        }
        for (Servico servico : servicos) {
            if (servico != null) {
                inserir(db, viagemId, servico);
            }
        }
    }

    public List<Servico> getServicosPorViagem(long viagemId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<Long, List<Servico>> resultado = getServicosPorViagens(db, Collections.singletonList(viagemId));
        List<Servico> lista = resultado.get(viagemId);
        return lista != null ? lista : Collections.emptyList();
    }

    public Map<Long, List<Servico>> getServicosPorViagens(SQLiteDatabase db, List<Long> viagemIds) {
        if (viagemIds == null || viagemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> idsUnicos = new ArrayList<>(new LinkedHashSet<>(viagemIds));
        Map<Long, List<Servico>> mapa = new LinkedHashMap<>();

        for (int start = 0; start < idsUnicos.size(); start += BATCH_CHUNK_SIZE) {
            int end = Math.min(start + BATCH_CHUNK_SIZE, idsUnicos.size());
            List<Long> chunk = idsUnicos.subList(start, end);

            StringBuilder placeholders = new StringBuilder();
            String[] args = new String[chunk.size()];
            for (int i = 0; i < chunk.size(); i++) {
                if (i > 0) {
                    placeholders.append(",");
                }
                placeholders.append("?");
                args[i] = String.valueOf(chunk.get(i));
            }

            String sql = "SELECT " +
                    DbHelper.Servico.ID + ", " +
                    DbHelper.Servico.VIAGEM_ID + ", " +
                    DbHelper.Servico.DESCRICAO + ", " +
                    DbHelper.Servico.VALOR +
                    " FROM " + DbHelper.Servico.NOME_TABELA +
                    " WHERE " + DbHelper.Servico.VIAGEM_ID + " IN (" + placeholders + ")" +
                    " ORDER BY " + DbHelper.Servico.ID + " ASC";

            try (Cursor cursor = db.rawQuery(sql, args)) {
                while (cursor != null && cursor.moveToNext()) {
                    Servico servico = new Servico(
                            cursor.getLong(0),
                            cursor.getLong(1),
                            cursor.getString(2),
                            cursor.getFloat(3)
                    );
                    mapa.computeIfAbsent(servico.getViagemId(), k -> new ArrayList<>()).add(servico);
                }
            }
        }

        return mapa;
    }

    public int deletarPorViagem(long viagemId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DbHelper.Servico.NOME_TABELA,
                DbHelper.Servico.VIAGEM_ID + " = ?",
                new String[]{String.valueOf(viagemId)}
        );
    }
}
