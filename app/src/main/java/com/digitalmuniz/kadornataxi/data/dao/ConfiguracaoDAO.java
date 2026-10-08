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
    private SQLiteDatabase db;
    private final DbHelper dbHelper;
    private final String TAG = "ConfiguracaoDAO";

    public ConfiguracaoDAO(Context context) {
        dbHelper = new DbHelper(context);
    }

    private void open() {
        db = dbHelper.getWritableDatabase();
    }

    private void close() {
        dbHelper.close();
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

    private long insert(Configuracao configuracao) {
        ContentValues values = valuesPut(configuracao);

        values.put(DbHelper.Configuracao.ID, 1);

        return db.insert(DbHelper.Configuracao.NOME_TABELA,
                null, values);
    }

    private long update(Configuracao configuracao) {
        ContentValues values = valuesPut(configuracao);

        return db.update(DbHelper.Configuracao.NOME_TABELA,
                values,
                DbHelper.Configuracao.ID + " = ?",
                new String[]{String.valueOf(configuracao.getId())});
    }
    public void setConfiguracaoPadrao() {
        open();

        Log.d(TAG, "Configuração padrão gerada com o ID: " + insert(
                new Configuracao(1, 1.99f, 20f,
                        "Marcelo", "Trecho 2", 
                        "", "", "", "", "")));

        close();
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
        open();

        Log.d(TAG,
                "Configuração salva com o ID: " + insert(configuracao));

        close();
    }

    private void updateConfiguracao(Configuracao configuracao) {
        open();

        Log.d (TAG ,
                "Configuração atualizada, linhas alteradas: " + update(configuracao));

        close();
    }

    public Configuracao getConfiguracao() {
        open();

        Cursor cursor = db.query(
                DbHelper.Configuracao.NOME_TABELA,
                null,
                DbHelper.Configuracao.ID + " = ?",
                new String[]{"1"} ,
                null, null, null);

        Configuracao configuracao = null;

        if (cursor.moveToFirst()) {
            configuracao = cursorToConfiguracao(cursor);
        }

        cursor.close();
        close();

        return configuracao;
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
        open();

        Cursor cursor = db.query(
                DbHelper.Configuracao.NOME_TABELA,
                null, null, null,
                null, null, null);

        boolean existe = cursor.getCount() > 0;

        cursor.close();
        close();

        return existe;
    }
}
