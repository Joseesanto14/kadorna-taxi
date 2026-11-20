package com.digitalmuniz.kadornataxi;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.view.dto.GrupoViagens;
import com.digitalmuniz.kadornataxi.view.dto.PaginaViagensResult;
import com.digitalmuniz.kadornataxi.view.dto.ViagensFiltro;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ViagemDAOStressTest {

    private static final int TOTAL_REGISTROS = 100_000;

    private DbHelper dbHelper;
    private ViagemDAO viagemDAO;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        dbHelper = new DbHelper(context, null);
        viagemDAO = new ViagemDAO(dbHelper);
        inserirCemMilViagens();
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    private void inserirCemMilViagens() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String sql = "INSERT INTO " + DbHelper.Viagem.NOME_TABELA + " (" +
                DbHelper.Viagem.ORIGEM + ", " +
                DbHelper.Viagem.DATA + ", " +
                DbHelper.Viagem.HORA + ", " +
                DbHelper.Viagem.DESTINO + ", " +
                DbHelper.Viagem.DESCRICAO + ", " +
                DbHelper.Viagem.KMS_RODADOS + ", " +
                DbHelper.Viagem.VALOR_KM + ", " +
                DbHelper.Viagem.VALOR_SERVICO + ", " +
                DbHelper.Viagem.MOTORISTA + ", " +
                DbHelper.Viagem.HORA_ESPERA + ", " +
                DbHelper.Viagem.VALOR_HORA_ESPERA + ", " +
                DbHelper.Viagem.CLASSIFICACAO + ", " +
                DbHelper.Viagem.VALOR_TOTAL +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String[] classificacoes = {"Comum", "Quartzolit", "Prefeitura", "Cliente VIP"};

        db.beginTransactionNonExclusive();
        try {
            SQLiteStatement stmt = db.compileStatement(sql);
            for (int i = 0; i < TOTAL_REGISTROS; i++) {
                int mes = (i % 12) + 1;
                int dia = (i % 28) + 1;
                int hora = (i / 60) % 24;
                int min = i % 60;
                String data = String.format(Locale.US, "2026-%02d-%02d", mes, dia);
                String horaStr = String.format(Locale.US, "%02d:%02d", hora, min);
                String classif = classificacoes[(i / 12) % classificacoes.length];

                stmt.clearBindings();
                stmt.bindString(1, "Itu");
                stmt.bindString(2, data);
                stmt.bindString(3, horaStr);
                stmt.bindString(4, "Destino " + (i % 200));
                stmt.bindString(5, "Viagem stress #" + i);
                stmt.bindDouble(6, 15.0);
                stmt.bindDouble(7, 30.0);
                stmt.bindDouble(8, 0.0);
                stmt.bindString(9, "Marcelo");
                stmt.bindDouble(10, 0.0);
                stmt.bindDouble(11, 0.0);
                stmt.bindString(12, classif);
                stmt.bindDouble(13, 30.0);
                stmt.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Test
    public void stress100k_getGruposRetornaApenasCabecalhosEGetPaginaRetornaNoMaximo30Itens() {
        assertEquals(TOTAL_REGISTROS, viagemDAO.getCount());

        List<GrupoViagens> grupos = viagemDAO.getGrupos(new ViagensFiltro("", false));
        assertEquals(48, grupos.size()); // 12 meses x 4 classificações

        int somaTotais = 0;
        for (GrupoViagens grupo : grupos) {
            somaTotais += grupo.getTotalViagens();
        }
        assertEquals(TOTAL_REGISTROS, somaTotais);

        String primeiroGrupoComum = "2026-12";
        PaginaViagensResult pagina1 = viagemDAO.getPaginaDoGrupo(
                primeiroGrupoComum,
                new ViagensFiltro("", false),
                null,
                30
        );
        assertEquals(30, pagina1.getItens().size());
        assertTrue(pagina1.isTemMais());
        assertNotNull(pagina1.getProximoCursor());

        PaginaViagensResult pagina2 = viagemDAO.getPaginaDoGrupo(
                primeiroGrupoComum,
                new ViagensFiltro("", false),
                pagina1.getProximoCursor(),
                30
        );
        assertEquals(30, pagina2.getItens().size());
        assertTrue(pagina2.isTemMais());
    }

    @Test
    public void stress100k_explainQueryPlanUsaIndiceSemTempBTreeOuFullScan() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sqlComum = "EXPLAIN QUERY PLAN " +
                "SELECT * FROM viagem " +
                "WHERE data_origem >= ? AND data_origem < ? " +
                "AND COALESCE(classificacao, '') IN ('', 'Comum') " +
                "ORDER BY data_origem DESC, hora_origem DESC, id DESC LIMIT ?";

        String planoComum = lerExplainQueryPlan(db, sqlComum, new String[]{"2026-12-01", "2027-01-01", "31"});
        assertTrue("Plano deveria usar idx_viagem_data_hora_id, mas foi: " + planoComum,
                planoComum.contains("idx_viagem_data_hora_id"));
        assertFalse("Plano não deveria usar TEMP B-TREE: " + planoComum,
                planoComum.toUpperCase(Locale.ROOT).contains("TEMP B-TREE"));
        assertFalse("Plano não deveria fazer SCAN TABLE viagem: " + planoComum,
                planoComum.toUpperCase(Locale.ROOT).contains("SCAN viagem"));

        String sqlComumPagina2 = "EXPLAIN QUERY PLAN " +
                "SELECT * FROM viagem " +
                "WHERE data_origem >= ? AND data_origem < ? " +
                "AND COALESCE(classificacao, '') IN ('', 'Comum') " +
                "AND data_origem <= ? AND (data_origem < ? OR (data_origem = ? AND hora_origem < ?) OR (data_origem = ? AND hora_origem = ? AND id < ?)) " +
                "ORDER BY data_origem DESC, hora_origem DESC, id DESC LIMIT ?";

        String planoPagina2 = lerExplainQueryPlan(
                db,
                sqlComumPagina2,
                new String[]{"2026-12-01", "2027-01-01", "2026-12-15", "2026-12-15", "2026-12-15", "10:00", "2026-12-15", "10:00", "50000", "31"}
        );
        assertTrue("Plano paginado deveria usar idx_viagem_data_hora_id, mas foi: " + planoPagina2,
                planoPagina2.contains("idx_viagem_data_hora_id"));
        assertFalse("Plano paginado não deveria usar TEMP B-TREE: " + planoPagina2,
                planoPagina2.toUpperCase(Locale.ROOT).contains("TEMP B-TREE"));
        assertFalse("Plano paginado não deveria fazer SCAN TABLE viagem: " + planoPagina2,
                planoPagina2.toUpperCase(Locale.ROOT).contains("SCAN viagem"));
    }

    private String lerExplainQueryPlan(SQLiteDatabase db, String sql, String[] args) {
        List<String> etapas = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, args)) {
            int detailCol = c.getColumnIndex("detail");
            while (c.moveToNext()) {
                etapas.add(c.getString(detailCol != -1 ? detailCol : c.getColumnCount() - 1));
            }
        }
        return String.join(" | ", etapas);
    }
}
