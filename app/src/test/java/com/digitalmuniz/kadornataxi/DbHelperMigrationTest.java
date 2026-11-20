package com.digitalmuniz.kadornataxi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DbHelperMigrationTest {

    private Context context;
    private String dbName;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dbName = "migration_test_" + System.nanoTime() + ".db";
    }

    @After
    public void tearDown() {
        context.deleteDatabase(dbName);
    }

    @Test
    public void upgradeV4ParaV5_criaIndicesHabilitaWalEPreservaDados() {
        // Cria banco legado v4 sem os novos índices
        LegacyV4Helper legacyHelper = new LegacyV4Helper(context, dbName);
        SQLiteDatabase v4Db = legacyHelper.getWritableDatabase();

        ContentValues cfg = new ContentValues();
        cfg.put(DbHelper.Configuracao.ID, 1);
        cfg.put(DbHelper.Configuracao.VALOR_KM_RODADO, 3.5f);
        cfg.put(DbHelper.Configuracao.VALOR_HORA_ESPERA, 25f);
        cfg.put(DbHelper.Configuracao.MOTORISTA, "Marcelo");
        cfg.put(DbHelper.Configuracao.CLASSIFICACAO_VIAGEM_SEPARADA, "Quartzolit");
        cfg.put(DbHelper.Configuracao.CNPJ, "12.345.678/0001-90");
        cfg.put(DbHelper.Configuracao.NOME_FANTASIA, "Kadorna");
        cfg.put(DbHelper.Configuracao.TITULAR_CNPJ, "Marcelo");
        cfg.put(DbHelper.Configuracao.TELEFONE, "11999999999");
        cfg.put(DbHelper.Configuracao.EMAIL, "contato@kadorna.com");
        v4Db.insertOrThrow(DbHelper.Configuracao.NOME_TABELA, null, cfg);

        ContentValues viagem = new ContentValues();
        viagem.put(DbHelper.Viagem.ORIGEM, "Itu");
        viagem.put(DbHelper.Viagem.DATA, "2026-03-10");
        viagem.put(DbHelper.Viagem.HORA, "14:30");
        viagem.put(DbHelper.Viagem.DESTINO, "Sorocaba");
        viagem.put(DbHelper.Viagem.DESCRICAO, "Corrida legada v4");
        viagem.put(DbHelper.Viagem.KMS_RODADOS, 35.0f);
        viagem.put(DbHelper.Viagem.VALOR_KM, 122.5f);
        viagem.put(DbHelper.Viagem.VALOR_SERVICO, 0f);
        viagem.put(DbHelper.Viagem.MOTORISTA, "Marcelo");
        viagem.put(DbHelper.Viagem.HORA_ESPERA, 0f);
        viagem.put(DbHelper.Viagem.VALOR_HORA_ESPERA, 0f);
        viagem.put(DbHelper.Viagem.CLASSIFICACAO, "Comum");
        viagem.put(DbHelper.Viagem.VALOR_TOTAL, 122.5f);
        v4Db.insertOrThrow(DbHelper.Viagem.NOME_TABELA, null, viagem);
        legacyHelper.close();

        // Abre na versão 5 via DbHelper
        DbHelper v5Helper = new DbHelper(context, dbName);
        SQLiteDatabase v5Db = v5Helper.getWritableDatabase();
        try {
            assertEquals(DbHelper.DATABASE_VERSION, v5Db.getVersion());

            // Verifica WAL habilitado
            try (Cursor journalCursor = v5Db.rawQuery("PRAGMA journal_mode", null)) {
                assertTrue(journalCursor.moveToFirst());
                assertEquals("wal", journalCursor.getString(0).toLowerCase());
            }

            // Verifica índices via PRAGMA index_list('viagem')
            Set<String> indices = new HashSet<>();
            try (Cursor idxCursor = v5Db.rawQuery("PRAGMA index_list('viagem')", null)) {
                int nameCol = idxCursor.getColumnIndexOrThrow("name");
                while (idxCursor.moveToNext()) {
                    indices.add(idxCursor.getString(nameCol));
                }
            }
            assertTrue(indices.contains("idx_viagem_data_hora_id"));
            assertTrue(indices.contains("idx_viagem_classif_data_hora_id"));

            // Verifica preservação de dados
            ViagemDAO viagemDAO = new ViagemDAO(v5Helper);
            List<Viagem> viagens = viagemDAO.getAll();
            assertEquals(1, viagens.size());
            assertEquals("Corrida legada v4", viagens.get(0).getDescricao());
            assertEquals("2026-03-10", viagens.get(0).getData());

            ConfiguracaoDAO configDAO = new ConfiguracaoDAO(v5Helper);
            Configuracao configLoaded = configDAO.getConfiguracao();
            assertNotNull(configLoaded);
            assertEquals("Marcelo", configLoaded.getMotorista());
            assertEquals("Quartzolit", configLoaded.getClassificacaoViagemSeparada());
        } finally {
            v5Helper.close();
        }
    }

    @Test
    public void onCreateV5_criaIndicesEHabilitaWal() {
        DbHelper v5Helper = new DbHelper(context, null);
        SQLiteDatabase db = v5Helper.getWritableDatabase();
        try {
            Set<String> indices = new HashSet<>();
            try (Cursor idxCursor = db.rawQuery("PRAGMA index_list('viagem')", null)) {
                int nameCol = idxCursor.getColumnIndexOrThrow("name");
                while (idxCursor.moveToNext()) {
                    indices.add(idxCursor.getString(nameCol));
                }
            }
            assertTrue(indices.contains("idx_viagem_data_hora_id"));
            assertTrue(indices.contains("idx_viagem_classif_data_hora_id"));
        } finally {
            v5Helper.close();
        }
    }

    private static class LegacyV4Helper extends SQLiteOpenHelper {
        LegacyV4Helper(Context context, String name) {
            super(context, name, null, 4);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DbHelper.Viagem.CRIAR_TABELA);
            db.execSQL(DbHelper.Configuracao.CRIAR_TABELA);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Não usado neste teste
        }
    }
}
