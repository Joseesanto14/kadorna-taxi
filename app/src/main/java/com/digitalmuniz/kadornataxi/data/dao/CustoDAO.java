package com.digitalmuniz.kadornataxi.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Custo;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CustoDAO {

    private final DbHelper dbHelper;

    public CustoDAO(Context context) {
        this.dbHelper = DbHelper.getInstance(context);
    }

    public CustoDAO(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long inserir(Custo custo) {
        if (custo == null) {
            return -1L;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {
            long id = db.insert(DbHelper.Custo.NOME_TABELA, null, toContentValues(custo));
            if (id != -1L) {
                custo.setId(id);
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public int atualizar(Custo custo) {
        if (custo == null || custo.getId() <= 0L) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {
            int linhas = db.update(
                    DbHelper.Custo.NOME_TABELA,
                    toContentValues(custo),
                    DbHelper.Custo.ID + " = ?",
                    new String[]{String.valueOf(custo.getId())}
            );
            db.setTransactionSuccessful();
            return linhas;
        } finally {
            db.endTransaction();
        }
    }

    public int deletar(long id) {
        if (id <= 0L) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DbHelper.Custo.NOME_TABELA,
                DbHelper.Custo.ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public int deletarTodos() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DbHelper.Custo.NOME_TABELA, null, null);
    }

    public Custo getById(long id) {
        if (id <= 0L) {
            return null;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbHelper.Custo.NOME_TABELA,
                null,
                DbHelper.Custo.ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToCusto(cursor);
            }
        }
        return null;
    }

    public List<Custo> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Custo> lista = new ArrayList<>();
        try (Cursor cursor = db.query(
                DbHelper.Custo.NOME_TABELA,
                null,
                null,
                null,
                null,
                null,
                DbHelper.Custo.DATA + " DESC, " + DbHelper.Custo.ID + " DESC"
        )) {
            while (cursor != null && cursor.moveToNext()) {
                lista.add(cursorToCusto(cursor));
            }
        }
        return lista;
    }

    public List<Custo> buscarPorPeriodo(String inicioIso, String fimExclusivoIso) {
        if (inicioIso == null || fimExclusivoIso == null) {
            return Collections.emptyList();
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Custo> lista = new ArrayList<>();
        String selection = DbHelper.Custo.DATA + " >= ? AND " + DbHelper.Custo.DATA + " < ?";
        String[] args = new String[]{inicioIso, fimExclusivoIso};
        try (Cursor cursor = db.query(
                DbHelper.Custo.NOME_TABELA,
                null,
                selection,
                args,
                null,
                null,
                DbHelper.Custo.DATA + " DESC, " + DbHelper.Custo.ID + " DESC"
        )) {
            while (cursor != null && cursor.moveToNext()) {
                lista.add(cursorToCusto(cursor));
            }
        }
        return lista;
    }

    public int inserirLote(List<Custo> custos, boolean substituirExistentes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {
            int inseridos = inserirLote(db, custos, substituirExistentes);
            db.setTransactionSuccessful();
            return inseridos;
        } finally {
            db.endTransaction();
        }
    }

    public int inserirLote(SQLiteDatabase db, List<Custo> custos, boolean substituirExistentes) {
        if (db == null) {
            return 0;
        }
        if (substituirExistentes) {
            db.delete(DbHelper.Custo.NOME_TABELA, null, null);
        }
        if (custos == null || custos.isEmpty()) {
            return 0;
        }
        int inseridos = 0;
        for (Custo custo : custos) {
            if (custo == null) {
                continue;
            }
            String dataIso = normalizarData(custo.getData());
            String descricao = custo.getDescricao().trim();
            double valor = custo.getValor();
            if (!substituirExistentes && existeCustoDuplicado(db, dataIso, descricao, valor)) {
                continue;
            }
            ContentValues cv = new ContentValues();
            cv.put(DbHelper.Custo.DESCRICAO, descricao);
            cv.put(DbHelper.Custo.VALOR, valor);
            cv.put(DbHelper.Custo.DATA, dataIso);
            long id = db.insert(DbHelper.Custo.NOME_TABELA, null, cv);
            if (id != -1L) {
                custo.setId(id);
                inseridos++;
            }
        }
        return inseridos;
    }

    private boolean existeCustoDuplicado(SQLiteDatabase db, String dataIso, String descricao, double valor) {
        String sql = "SELECT 1 FROM " + DbHelper.Custo.NOME_TABELA +
                " WHERE " + DbHelper.Custo.DATA + " = ? " +
                "AND LOWER(TRIM(" + DbHelper.Custo.DESCRICAO + ")) = LOWER(TRIM(?)) " +
                "AND ABS(" + DbHelper.Custo.VALOR + " - ?) < 0.005 LIMIT 1";
        String[] args = new String[]{
                dataIso,
                descricao,
                String.format(Locale.US, "%.6f", valor)
        };
        try (Cursor cursor = db.rawQuery(sql, args)) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    private ContentValues toContentValues(Custo custo) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.Custo.DESCRICAO, custo.getDescricao().trim());
        values.put(DbHelper.Custo.VALOR, custo.getValor());
        values.put(DbHelper.Custo.DATA, normalizarData(custo.getData()));
        return values;
    }

    private String normalizarData(String data) {
        String dataIso = Viagem.formatarDataISO8601(data);
        return dataIso != null ? dataIso : "";
    }

    public static Custo cursorToCusto(Cursor cursor) {
        return new Custo(
                cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.Custo.ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Custo.DESCRICAO)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(DbHelper.Custo.VALOR)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Custo.DATA))
        );
    }
}
