package com.digitalmuniz.kadornataxi;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ServicoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Servico;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
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
    public void upgradeV4ParaV7_criaColunaSolicitanteTabelaServicoIndicesWalEPreservaDados() {
        // Cria banco legado v4 sem coluna solicitante, sem tabela servico e sem índices
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
        viagem.put(DbHelper.Viagem.VALOR_SERVICO, 18.5f);
        viagem.put(DbHelper.Viagem.MOTORISTA, "Marcelo");
        viagem.put(DbHelper.Viagem.HORA_ESPERA, 0f);
        viagem.put(DbHelper.Viagem.VALOR_HORA_ESPERA, 0f);
        viagem.put(DbHelper.Viagem.CLASSIFICACAO, "Comum");
        viagem.put(DbHelper.Viagem.VALOR_TOTAL, 141.0f);
        v4Db.insertOrThrow(DbHelper.Viagem.NOME_TABELA, null, viagem);
        legacyHelper.close();

        // Abre na versão 7 via DbHelper
        DbHelper v7Helper = new DbHelper(context, dbName);
        SQLiteDatabase v7Db = v7Helper.getWritableDatabase();
        try {
            assertEquals(7, DbHelper.DATABASE_VERSION);
            assertEquals(DbHelper.DATABASE_VERSION, v7Db.getVersion());

            // Verifica WAL habilitado
            try (Cursor journalCursor = v7Db.rawQuery("PRAGMA journal_mode", null)) {
                assertTrue(journalCursor.moveToFirst());
                assertEquals("wal", journalCursor.getString(0).toLowerCase());
            }

            // Verifica índices via PRAGMA index_list('viagem') e ('servico')
            Set<String> indicesViagem = listarIndices(v7Db, DbHelper.Viagem.NOME_TABELA);
            assertTrue(indicesViagem.contains("idx_viagem_data_hora_id"));
            assertTrue(indicesViagem.contains("idx_viagem_classif_data_hora_id"));

            Set<String> indicesServico = listarIndices(v7Db, DbHelper.Servico.NOME_TABELA);
            assertTrue(indicesServico.contains("idx_servico_viagem_id"));

            // Verifica preservação de dados e migração do serviço legado
            ViagemDAO viagemDAO = new ViagemDAO(v7Helper);
            List<Viagem> viagens = viagemDAO.getAll();
            assertEquals(1, viagens.size());
            assertEquals("Corrida legada v4", viagens.get(0).getDescricao());
            assertEquals("2026-03-10", viagens.get(0).getData());
            assertEquals(1, viagens.get(0).getServicos().size());
            assertEquals("Serviço adicional", viagens.get(0).getServicos().get(0).getDescricao());
            assertEquals(18.5f, viagens.get(0).getServicos().get(0).getValor(), 0.01f);

            ConfiguracaoDAO configDAO = new ConfiguracaoDAO(v7Helper);
            Configuracao configLoaded = configDAO.getConfiguracao();
            assertNotNull(configLoaded);
            assertEquals("Marcelo", configLoaded.getMotorista());
            assertEquals("Quartzolit", configLoaded.getClassificacaoViagemSeparada());
        } finally {
            v7Helper.close();
        }
    }

    @Test
    public void upgradeV6ParaV7_migraValorServicoLegadoIdempotentementeEIgnoraZero() {
        LegacyV6Helper legacyV6 = new LegacyV6Helper(context, dbName);
        SQLiteDatabase v6Db = legacyV6.getWritableDatabase();

        ContentValues comServico = new ContentValues();
        comServico.put(DbHelper.Viagem.ORIGEM, "Itu");
        comServico.put(DbHelper.Viagem.DATA, "2026-04-15");
        comServico.put(DbHelper.Viagem.HORA, "09:15");
        comServico.put(DbHelper.Viagem.DESTINO, "Campinas");
        comServico.put(DbHelper.Viagem.DESCRICAO, "Corrida v6 com servico");
        comServico.put(DbHelper.Viagem.KMS_RODADOS, 45.0f);
        comServico.put(DbHelper.Viagem.VALOR_KM, 157.5f);
        comServico.put(DbHelper.Viagem.VALOR_SERVICO, 35.0f);
        comServico.put(DbHelper.Viagem.MOTORISTA, "Marcelo");
        comServico.put(DbHelper.Viagem.HORA_ESPERA, 0f);
        comServico.put(DbHelper.Viagem.VALOR_HORA_ESPERA, 0f);
        comServico.put(DbHelper.Viagem.CLASSIFICACAO, "Quartzolit");
        comServico.put(DbHelper.Viagem.VALOR_TOTAL, 192.5f);
        comServico.put(DbHelper.Viagem.SOLICITANTE, "Carlos Mendes");
        long idComServico = v6Db.insertOrThrow(DbHelper.Viagem.NOME_TABELA, null, comServico);

        ContentValues semServico = new ContentValues();
        semServico.put(DbHelper.Viagem.ORIGEM, "Itu");
        semServico.put(DbHelper.Viagem.DATA, "2026-04-16");
        semServico.put(DbHelper.Viagem.HORA, "10:00");
        semServico.put(DbHelper.Viagem.DESTINO, "Salto");
        semServico.put(DbHelper.Viagem.DESCRICAO, "Corrida v6 sem servico");
        semServico.put(DbHelper.Viagem.KMS_RODADOS, 10.0f);
        semServico.put(DbHelper.Viagem.VALOR_KM, 35.0f);
        semServico.put(DbHelper.Viagem.VALOR_SERVICO, 0f);
        semServico.put(DbHelper.Viagem.MOTORISTA, "Marcelo");
        semServico.put(DbHelper.Viagem.HORA_ESPERA, 0f);
        semServico.put(DbHelper.Viagem.VALOR_HORA_ESPERA, 0f);
        semServico.put(DbHelper.Viagem.CLASSIFICACAO, "Comum");
        semServico.put(DbHelper.Viagem.VALOR_TOTAL, 35.0f);
        semServico.put(DbHelper.Viagem.SOLICITANTE, "");
        long idSemServico = v6Db.insertOrThrow(DbHelper.Viagem.NOME_TABELA, null, semServico);
        legacyV6.close();

        DbHelper v7Helper = new DbHelper(context, dbName);
        SQLiteDatabase v7Db = v7Helper.getWritableDatabase();
        try {
            assertEquals(7, v7Db.getVersion());

            ViagemDAO viagemDAO = new ViagemDAO(v7Helper);
            Viagem carregadaCom = viagemDAO.getViagemById(idComServico);
            assertNotNull(carregadaCom);
            assertEquals("Carlos Mendes", carregadaCom.getSolicitante());
            assertEquals(1, carregadaCom.getServicos().size());
            assertEquals("Serviço adicional", carregadaCom.getServicos().get(0).getDescricao());
            assertEquals(35.0f, carregadaCom.getServicos().get(0).getValor(), 0.01f);

            Viagem carregadaSem = viagemDAO.getViagemById(idSemServico);
            assertNotNull(carregadaSem);
            assertTrue(carregadaSem.getServicos().isEmpty());

            // Simula re-execução idempotente da lógica de upgrade v7
            v7Helper.onUpgrade(v7Db, 6, 7);
            assertEquals(1, new ServicoDAO(v7Helper).getServicosPorViagem(idComServico).size());
        } finally {
            v7Helper.close();
        }
    }

    @Test
    public void onCreateV7_criaIndicesForeignKeysEDeleteCascade() {
        DbHelper v7Helper = new DbHelper(context, null);
        SQLiteDatabase db = v7Helper.getWritableDatabase();
        try {
            assertEquals(7, db.getVersion());

            Set<String> indicesViagem = listarIndices(db, DbHelper.Viagem.NOME_TABELA);
            assertTrue(indicesViagem.contains("idx_viagem_data_hora_id"));
            assertTrue(indicesViagem.contains("idx_viagem_classif_data_hora_id"));

            Set<String> indicesServico = listarIndices(db, DbHelper.Servico.NOME_TABELA);
            assertTrue(indicesServico.contains("idx_servico_viagem_id"));

            try (Cursor fkCursor = db.rawQuery("PRAGMA foreign_keys", null)) {
                assertTrue(fkCursor.moveToFirst());
                assertEquals(1, fkCursor.getInt(0));
            }

            ViagemDAO viagemDAO = new ViagemDAO(v7Helper);
            ServicoDAO servicoDAO = new ServicoDAO(v7Helper);

            Viagem v = new Viagem(
                    0L, "Itu", "2026-05-01", "11:00", "Botucatu", "Viagem cascade",
                    20f, 60f, 0f, "Marcelo", 0f, 0f, "Comum", 60f, "Joao"
            );
            v.setServicos(Arrays.asList(
                    new Servico("Pedágio", 12.5f),
                    new Servico("Estacionamento", 15.0f)
            ));
            long id = viagemDAO.inserir(v);
            assertEquals(2, servicoDAO.getServicosPorViagem(id).size());

            // Deleta viagem e verifica ON DELETE CASCADE na tabela servico
            assertEquals(1, viagemDAO.delete(id));
            assertTrue(servicoDAO.getServicosPorViagem(id).isEmpty());
        } finally {
            v7Helper.close();
        }
    }

    private Set<String> listarIndices(SQLiteDatabase db, String tabela) {
        Set<String> indices = new HashSet<>();
        try (Cursor idxCursor = db.rawQuery("PRAGMA index_list('" + tabela + "')", null)) {
            int nameCol = idxCursor.getColumnIndexOrThrow("name");
            while (idxCursor.moveToNext()) {
                indices.add(idxCursor.getString(nameCol));
            }
        }
        return indices;
    }

    private static class LegacyV4Helper extends SQLiteOpenHelper {
        LegacyV4Helper(Context context, String name) {
            super(context, name, null, 4);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + DbHelper.Viagem.NOME_TABELA + "(" +
                    DbHelper.Viagem.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DbHelper.Viagem.ORIGEM + " TEXT, " +
                    DbHelper.Viagem.DATA + " TEXT, " +
                    DbHelper.Viagem.HORA + " TEXT, " +
                    DbHelper.Viagem.DESTINO + " TEXT, " +
                    DbHelper.Viagem.DESCRICAO + " TEXT, " +
                    DbHelper.Viagem.KMS_RODADOS + " REAL, " +
                    DbHelper.Viagem.VALOR_KM + " REAL, " +
                    DbHelper.Viagem.VALOR_SERVICO + " REAL, " +
                    DbHelper.Viagem.MOTORISTA + " TEXT, " +
                    DbHelper.Viagem.HORA_ESPERA + " REAL, " +
                    DbHelper.Viagem.VALOR_HORA_ESPERA + " REAL, " +
                    DbHelper.Viagem.CLASSIFICACAO + " TEXT, " +
                    DbHelper.Viagem.VALOR_TOTAL + " REAL);");
            db.execSQL(DbHelper.Configuracao.CRIAR_TABELA);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Não usado neste teste
        }
    }

    private static class LegacyV6Helper extends SQLiteOpenHelper {
        LegacyV6Helper(Context context, String name) {
            super(context, name, null, 6);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DbHelper.Viagem.CRIAR_TABELA);
            db.execSQL(DbHelper.Configuracao.CRIAR_TABELA);
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_viagem_data_hora_id ON " + DbHelper.Viagem.NOME_TABELA +
                    " (" + DbHelper.Viagem.DATA + " DESC, " + DbHelper.Viagem.HORA + " DESC, " + DbHelper.Viagem.ID + " DESC)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_viagem_classif_data_hora_id ON " + DbHelper.Viagem.NOME_TABELA +
                    " (" + DbHelper.Viagem.CLASSIFICACAO + ", " + DbHelper.Viagem.DATA + " DESC, " + DbHelper.Viagem.HORA + " DESC, " + DbHelper.Viagem.ID + " DESC)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Não usado neste teste
        }
    }
}
