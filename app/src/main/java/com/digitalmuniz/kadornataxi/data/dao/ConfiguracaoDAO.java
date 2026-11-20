package com.digitalmuniz.kadornataxi.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;

public class ConfiguracaoDAO {
    private static final String TAG = "ConfiguracaoDAO";
    private final DbHelper dbHelper;

    public ConfiguracaoDAO(Context context) {
        this.dbHelper = DbHelper.getInstance(context);
    }

    public ConfiguracaoDAO(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    private ContentValues valuesPut(Configuracao configuracao) {
        ContentValues values = new ContentValues();

        values.put(DbHelper.Configuracao.VALOR_KM_RODADO,
                configuracao.getValorKmRodado());
        values.put(DbHelper.Configuracao.VALOR_HORA_ESPERA,
                configuracao.getValorHoraEspera());
        values.put(DbHelper.Configuracao.MOTORISTA,
                configuracao.getMotorista());
        values.put(DbHelper.Configuracao.CLASSIFICACAO_VIAGEM_SEPARADA,
                configuracao.getClassificacaoViagemSeparada());
        values.put(DbHelper.Configuracao.CNPJ,
                configuracao.getCnpj());
        values.put(DbHelper.Configuracao.NOME_FANTASIA,
                configuracao.getNomeFantasia());
        values.put(DbHelper.Configuracao.TITULAR_CNPJ,
                configuracao.getTitularCNPJ());
        values.put(DbHelper.Configuracao.TELEFONE,
                configuracao.getTelefone());
        values.put(DbHelper.Configuracao.EMAIL,
                configuracao.getEmail());

        return values;
    }

    private long insert(SQLiteDatabase db, Configuracao configuracao) {
        ContentValues values = valuesPut(configuracao);
        values.put(DbHelper.Configuracao.ID, 1);

        return db.insert(DbHelper.Configuracao.NOME_TABELA, null, values);
    }

    private long update(SQLiteDatabase db, Configuracao configuracao) {
        ContentValues values = valuesPut(configuracao);

        return db.update(DbHelper.Configuracao.NOME_TABELA,
                values,
                DbHelper.Configuracao.ID + " = ?",
                new String[]{String.valueOf(configuracao.getId())});
    }

    public void configurar(Configuracao configuracao, Context context) {
        if (configExiste()) {
            updateConfiguracao(configuracao);
            Toast.makeText(context, "Configurações atualizadas", Toast.LENGTH_SHORT).show();
        } else {
            setConfiguracao(configuracao);
            Toast.makeText(context, "Configurações salvas", Toast.LENGTH_SHORT).show();
        }
    }

    public void salvarConfiguracaoSilenciosamente(Configuracao configuracao) {
        if (configExiste()) {
            updateConfiguracao(configuracao);
        } else {
            setConfiguracao(configuracao);
        }
    }

    private void setConfiguracao(Configuracao configuracao) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(TAG, "Configuração salva com o ID: " + insert(db, configuracao));
    }

    private void updateConfiguracao(Configuracao configuracao) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.d(TAG, "Configuração atualizada, linhas alteradas: " + update(db, configuracao));
    }

    public Configuracao getConfiguracao() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbHelper.Configuracao.NOME_TABELA,
                null,
                DbHelper.Configuracao.ID + " = ?",
                new String[]{"1"},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursorToConfiguracao(cursor);
            }
        }
        return null;
    }

    private Configuracao cursorToConfiguracao(Cursor cursor) {
        return new Configuracao(
                cursor.getLong(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.ID)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.VALOR_KM_RODADO)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.VALOR_HORA_ESPERA)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.MOTORISTA)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.CLASSIFICACAO_VIAGEM_SEPARADA)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.CNPJ)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.NOME_FANTASIA)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.TITULAR_CNPJ)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.TELEFONE)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        DbHelper.Configuracao.EMAIL))
        );
    }

    public boolean configExiste() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbHelper.Configuracao.NOME_TABELA,
                new String[]{DbHelper.Configuracao.ID},
                null, null, null, null, null)) {
            return cursor != null && cursor.getCount() > 0;
        }
    }
}
