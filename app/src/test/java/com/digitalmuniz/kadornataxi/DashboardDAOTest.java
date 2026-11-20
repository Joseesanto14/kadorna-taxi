package com.digitalmuniz.kadornataxi;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.data.dao.DashboardDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.dto.ClassificacaoResumoDTO;
import com.digitalmuniz.kadornataxi.view.dto.DashboardResumoDTO;
import com.digitalmuniz.kadornataxi.view.dto.FaturamentoMensalDTO;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DashboardDAOTest {

    private DbHelper dbHelper;
    private ViagemDAO viagemDAO;
    private DashboardDAO dashboardDAO;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        dbHelper = new DbHelper(context, null);
        viagemDAO = new ViagemDAO(dbHelper);
        dashboardDAO = new DashboardDAO(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void consultasPeriodo_calculamResumoEvolucaoVariacaoERankingCorretamente() {
        List<Viagem> lote = Arrays.asList(
                // 2025 (total = 1000.0)
                criarViagem("2025-11-10", "08:00", 50f, 100f, 0f, 0f, 0f, "Comum", 400f),
                criarViagem("2025-12-20", "09:00", 100f, 500f, 50f, 1f, 50f, "Quartzolit", 600f),
                // 2026-02 (total = 500.0)
                criarViagem("2026-02-05", "10:00", 100f, 400f, 100f, 0f, 0f, "Quartzolit", 500f),
                // 2026-03 (total = 750.0 -> +50% vs 2026-02)
                criarViagem("2026-03-01", "08:00", 40f, 200f, 0f, 1.5f, 50f, null, 250f),
                criarViagem("2026-03-01", "14:00", 60f, 300f, 0f, 0f, 0f, "   ", 300f),
                criarViagem("2026-03-10", "16:00", 20f, 150f, 50f, 0f, 0f, "Empresa X", 200f)
        );
        assertEquals(6, viagemDAO.inserirLote(lote, true));

        // Anos e meses disponíveis
        assertEquals(Arrays.asList("2026", "2025"), dashboardDAO.buscarAnosDisponiveis());
        assertEquals(Arrays.asList("02", "03"), dashboardDAO.buscarMesesDisponiveisPorAno("2026"));
        assertEquals(Arrays.asList("11", "12"), dashboardDAO.buscarMesesDisponiveisPorAno("2025"));

        // Resumo mensal 2026-03
        DashboardResumoDTO resumoMarco = dashboardDAO.buscarResumoPorPeriodo("2026", "03");
        assertEquals(3, resumoMarco.getTotalViagens());
        assertEquals(2, resumoMarco.getDiasTrabalhados());
        assertEquals(750.0, resumoMarco.getFaturamentoTotal(), 0.01);
        assertEquals(500.0, resumoMarco.getFaturamentoPeriodoAnterior(), 0.01);
        assertNotNull(resumoMarco.getVariacaoPercentual());
        assertEquals(50.0, resumoMarco.getVariacaoPercentual(), 0.01);
        assertEquals(250.0, resumoMarco.getTicketMedio(), 0.01);
        assertEquals(120.0, resumoMarco.getTotalKms(), 0.01);
        assertEquals(750.0 / 120.0, resumoMarco.getMediaReaisPorKm(), 0.01);
        assertEquals(375.0, resumoMarco.getMediaDiaria(), 0.01);
        assertEquals(650.0, resumoMarco.getTotalValorKm(), 0.01);
        assertEquals(1.5, resumoMarco.getTotalHorasEspera(), 0.01);
        assertEquals(50.0, resumoMarco.getTotalValorEspera(), 0.01);
        assertEquals(50.0, resumoMarco.getTotalValorServico(), 0.01);

        // Resumo mensal 2026-02 (mês anterior 2026-01 sem dados -> variação null)
        DashboardResumoDTO resumoFev = dashboardDAO.buscarResumoPorPeriodo("2026", "02");
        assertNull(resumoFev.getVariacaoPercentual());

        // Resumo anual 2026 (1250.0 vs 1000.0 em 2025 -> +25.0%)
        DashboardResumoDTO resumoAnual2026 = dashboardDAO.buscarResumoPorPeriodo("2026", null);
        assertEquals(4, resumoAnual2026.getTotalViagens());
        assertEquals(1250.0, resumoAnual2026.getFaturamentoTotal(), 0.01);
        assertEquals(1000.0, resumoAnual2026.getFaturamentoPeriodoAnterior(), 0.01);
        assertNotNull(resumoAnual2026.getVariacaoPercentual());
        assertEquals(25.0, resumoAnual2026.getVariacaoPercentual(), 0.01);

        // Evolução anual sempre tem 12 meses ("01".."12")
        List<FaturamentoMensalDTO> evolucao2026 = dashboardDAO.buscarEvolucaoMensalDoAno("2026");
        assertEquals(12, evolucao2026.size());
        assertEquals("01", evolucao2026.get(0).getMesNumero());
        assertEquals(0.0, evolucao2026.get(0).getFaturamentoTotal(), 0.01);
        assertEquals(500.0, evolucao2026.get(1).getFaturamentoTotal(), 0.01);
        assertEquals(750.0, evolucao2026.get(2).getFaturamentoTotal(), 0.01);
        assertEquals("Dez", evolucao2026.get(11).getMesSigla());

        // Ranking de classificação: null e "" consolidados como 'Comum'
        List<ClassificacaoResumoDTO> rankingMarco = dashboardDAO.buscarRankingClassificacao("2026", "03");
        assertEquals(2, rankingMarco.size());
        assertEquals("Comum", rankingMarco.get(0).getClassificacao());
        assertEquals(2, rankingMarco.get(0).getTotalViagens());
        assertEquals(550.0, rankingMarco.get(0).getFaturamentoTotal(), 0.01);
        assertEquals((550.0 / 750.0) * 100.0, rankingMarco.get(0).getPercentualParticipacao(), 0.01);

        assertEquals("Empresa X", rankingMarco.get(1).getClassificacao());
        assertEquals(1, rankingMarco.get(1).getTotalViagens());
        assertEquals(200.0, rankingMarco.get(1).getFaturamentoTotal(), 0.01);
    }

    @Test
    public void explainQueryPlan_confirmaUsoDoIndiceTemporal() {
        viagemDAO.inserir(criarViagem("2026-03-10", "10:00", 10f, 20f, 0f, 0f, 0f, "Comum", 20f));
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "EXPLAIN QUERY PLAN SELECT COUNT(*), SUM(valor_total) FROM " +
                DbHelper.Viagem.NOME_TABELA +
                " WHERE " + DbHelper.Viagem.DATA + " >= ? AND " + DbHelper.Viagem.DATA + " < ?";

        StringBuilder plano = new StringBuilder();
        try (Cursor cursor = db.rawQuery(sql, new String[]{"2026-03-01", "2026-04-01"})) {
            while (cursor != null && cursor.moveToNext()) {
                plano.append(cursor.getString(3)).append(" ");
            }
        }
        String planoStr = plano.toString();
        assertTrue("Plano esperado deveria usar idx_viagem_data_hora_id, mas foi: " + planoStr,
                planoStr.contains("USING INDEX idx_viagem_data_hora_id")
                        || planoStr.contains("USING COVERING INDEX idx_viagem_data_hora_id"));
    }

    @Test
    public void benchmark_com10000Linhas_executaTodasConsultasAbaixoDe150ms() {
        List<Viagem> lote = new ArrayList<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            int ano = 2024 + (i % 3);
            int mes = ((i / 3) % 12) + 1;
            int dia = ((i / 36) % 28) + 1;
            String data = String.format(Locale.US, "%04d-%02d-%02d", ano, mes, dia);
            String classif = (i % 5 == 0) ? "Quartzolit" : "Comum";
            lote.add(criarViagem(data, "10:00", 15f, 30f, 5f, 0.5f, 10f, classif, 45f));
        }
        assertEquals(10_000, viagemDAO.inserirLote(lote, true));

        // Warmup de page cache
        dashboardDAO.buscarAnosDisponiveis();

        long inicioNs = System.nanoTime();
        List<String> anos = dashboardDAO.buscarAnosDisponiveis();
        List<String> meses = dashboardDAO.buscarMesesDisponiveisPorAno("2026");
        DashboardResumoDTO resumo = dashboardDAO.buscarResumoPorPeriodo("2026", "03");
        List<FaturamentoMensalDTO> evolucao = dashboardDAO.buscarEvolucaoMensalDoAno("2026");
        List<ClassificacaoResumoDTO> ranking = dashboardDAO.buscarRankingClassificacao("2026", "03");
        long elapsedMs = (System.nanoTime() - inicioNs) / 1_000_000L;

        assertEquals(3, anos.size());
        assertEquals(12, meses.size());
        assertTrue(resumo.getTotalViagens() > 0);
        assertEquals(12, evolucao.size());
        assertEquals(2, ranking.size());
        assertTrue("Consultas do dashboard levaram " + elapsedMs + "ms (limite < 150ms)", elapsedMs < 150L);
    }

    private Viagem criarViagem(
            String data,
            String hora,
            float kms,
            float valorKms,
            float valorServico,
            float horaEspera,
            float valorEspera,
            String classificacao,
            float valorTotal
    ) {
        return new Viagem(
                0L,
                "Itu",
                data,
                hora,
                "Sorocaba",
                "Teste",
                kms,
                valorKms,
                valorServico,
                "Marcelo",
                horaEspera,
                valorEspera,
                classificacao,
                valorTotal
        );
    }
}
