package com.digitalmuniz.kadornataxi.data.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "kadorna_taxi.db";
    private static final int DATABASE_VERSION = 5;

    public DbHelper(Context context) {
        super(context, DbHelper.DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Viagem.CRIAR_TABELA);
        db.execSQL(Configuracao.CRIAR_TABELA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + Viagem.NOME_TABELA + " ADD COLUMN " + Viagem.VALOR_SERVICO + " REAL DEFAULT " + 0f);
            db.execSQL("ALTER TABLE " + Viagem.NOME_TABELA + " ADD COLUMN " + Viagem.KMS_RODADOS + " REAL DEFAULT " + 0f);
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + Configuracao.NOME_TABELA + " ADD COLUMN " + Configuracao.CNPJ + " TEXT");
            db.execSQL("ALTER TABLE " + Configuracao.NOME_TABELA + " ADD COLUMN " + Configuracao.NOME_FANTASIA + " TEXT");
            db.execSQL("ALTER TABLE " + Configuracao.NOME_TABELA + " ADD COLUMN " + Configuracao.TITULAR_CNPJ + " TEXT");
            db.execSQL("ALTER TABLE " + Configuracao.NOME_TABELA + " ADD COLUMN " + Configuracao.TELEFONE + " TEXT");
            db.execSQL("ALTER TABLE " + Configuracao.NOME_TABELA + " ADD COLUMN " + Configuracao.EMAIL + " TEXT");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + Viagem.NOME_TABELA + " ADD COLUMN " + Viagem.SOLICITANTE + " TEXT DEFAULT ''");
        }
    }

    // ---------- Tabela Viagem ----------

    public static class Viagem {
        public static final String NOME_TABELA = "viagem";
        public static final String ID = "id";
        public static final String ORIGEM = "origem";
        public static final String DATA = "data_origem";
        public static final String HORA = "hora_origem";
        public static final String DESTINO = "destino";
        public static final String DESCRICAO = "descricao";
        public static final String KMS_RODADOS = "kms_rodados";
        public static final String VALOR_KM = "valor_km";
        public static final String VALOR_SERVICO = "valor_servico";
        public static final String MOTORISTA = "motorista";
        public static final String HORA_ESPERA = "hora_espera";
        public static final String VALOR_HORA_ESPERA = "valor_hora_espera";
        public static final String CLASSIFICACAO = "classificacao";
        public static final String VALOR_TOTAL = "valor_total";
        public static final String SOLICITANTE = "solicitante";

        public static final String CRIAR_TABELA = "CREATE TABLE " + NOME_TABELA + "(" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ORIGEM + " TEXT, " +
                DATA + " TEXT, " +
                HORA + " TEXT, " +
                DESTINO + " TEXT, " +
                DESCRICAO + " TEXT, " +
                KMS_RODADOS + " REAL, " +
                VALOR_KM + " REAL, " +
                VALOR_SERVICO + " REAL, " +
                MOTORISTA + " TEXT, " +
                HORA_ESPERA + " REAL, " +
                VALOR_HORA_ESPERA + " REAL, " +
                CLASSIFICACAO + " TEXT, " +
                VALOR_TOTAL + " REAL, " +
                SOLICITANTE + " TEXT DEFAULT ''" +
                ");";
    }

    // ---------- Tabela das configurações ----------
    public static class Configuracao {
        public static final String NOME_TABELA = "configuracao";

        public static final String ID =  "id";
        public static final String VALOR_KM_RODADO = "valor_km_rodado";
        public static final String VALOR_HORA_ESPERA = "valor_hora_espera";
        public static final String MOTORISTA = "motorista";
        public static final String CLASSIFICACAO_VIAGEM_SEPARADA = "classificacao_viagem_separada";
        public static final String CNPJ = "cnpj";
        public static final String NOME_FANTASIA = "nome_fantasia";
        public static final String TITULAR_CNPJ = "titular_cnpj";
        public static final String TELEFONE = "telefone";
        public static final String EMAIL = "email";

        public static final String CRIAR_TABELA = "CREATE TABLE " + NOME_TABELA + "(" +
                ID + " INTEGER PRIMARY KEY, " +
                VALOR_KM_RODADO + " REAL NOT NULL, " +
                VALOR_HORA_ESPERA + " REAL NOT NULL, " +
                MOTORISTA + " TEXT NOT NULL, " +
                CLASSIFICACAO_VIAGEM_SEPARADA + " TEXT NOT NULL, " +
                CNPJ + " TEXT, " +
                NOME_FANTASIA + " TEXT, " +
                TITULAR_CNPJ + " TEXT, " +
                TELEFONE + " TEXT, " +
                EMAIL + " TEXT " +
                ");";
    }
}
