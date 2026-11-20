package com.digitalmuniz.kadornataxi.data.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "kadorna_taxi.db";
    public static final int DATABASE_VERSION = 6;

    private static volatile DbHelper instance;

    public static DbHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (DbHelper.class) {
                if (instance == null) {
                    instance = new DbHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public DbHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.enableWriteAheadLogging();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Viagem.CRIAR_TABELA);
        db.execSQL(Configuracao.CRIAR_TABELA);
        criarIndices(db);
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
            garantirColunaSolicitante(db);
        }
        if (oldVersion < 6) {
            // Garante compatibilidade tanto com bancos v5 vindos de feat-solicitante quanto builds de dev sem a coluna.
            garantirColunaSolicitante(db);
            criarIndices(db);
        }
    }

    private void garantirColunaSolicitante(SQLiteDatabase db) {
        if (!colunaExiste(db, Viagem.NOME_TABELA, Viagem.SOLICITANTE)) {
            db.execSQL("ALTER TABLE " + Viagem.NOME_TABELA + " ADD COLUMN " + Viagem.SOLICITANTE + " TEXT DEFAULT ''");
        }
    }

    private boolean colunaExiste(SQLiteDatabase db, String tabela, String coluna) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + tabela + ")", null)) {
            if (cursor == null) {
                return false;
            }
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (nameIndex != -1 && coluna.equalsIgnoreCase(cursor.getString(nameIndex))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void criarIndices(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_viagem_data_hora_id ON " + Viagem.NOME_TABELA +
                " (" + Viagem.DATA + " DESC, " + Viagem.HORA + " DESC, " + Viagem.ID + " DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_viagem_classif_data_hora_id ON " + Viagem.NOME_TABELA +
                " (" + Viagem.CLASSIFICACAO + ", " + Viagem.DATA + " DESC, " + Viagem.HORA + " DESC, " + Viagem.ID + " DESC)");
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
