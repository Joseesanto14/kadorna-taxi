package com.digitalmuniz.kadornataxi;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.data.dao.ServicoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Servico;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.dto.GrupoViagens;
import com.digitalmuniz.kadornataxi.view.dto.PaginaViagensResult;
import com.digitalmuniz.kadornataxi.view.dto.ViagemCursorKey;
import com.digitalmuniz.kadornataxi.view.dto.ViagensFiltro;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ViagemDAOPaginacaoTest {

    private Context context;
    private DbHelper dbHelper;
    private ViagemDAO viagemDAO;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = new DbHelper(context, null);
        viagemDAO = new ViagemDAO(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void keysetPagination_comEmpatesDeDataEHora_percorreTodos125ItensSemRepeticaoOuOmissao() {
        List<Viagem> lote = new ArrayList<>(125);
        for (int i = 0; i < 125; i++) {
            // Força múltiplos empates no mesmo dia e mesma hora
            int dia = (i / 10) + 1;
            int hora = (i / 5) % 24;
            String data = String.format(Locale.US, "2026-03-%02d", dia);
            String horaStr = String.format(Locale.US, "%02d:00", hora);
            lote.add(criarViagem("Itu", data, horaStr, "Destino " + i, "Desc " + i, "Marcelo", "Comum", 50f + i));
        }
        assertEquals(125, viagemDAO.inserirLote(lote, true));

        List<Viagem> coletadas = new ArrayList<>();
        Set<Long> idsUnicos = new HashSet<>();
        ViagemCursorKey cursor = null;
        int paginas = 0;

        do {
            PaginaViagensResult pagina = viagemDAO.getPaginaDoGrupo(
                    "2026-03",
                    new ViagensFiltro("", false),
                    cursor,
                    30
            );
            paginas++;
            assertTrue(pagina.getItens().size() <= 30);
            for (Viagem v : pagina.getItens()) {
                assertTrue("ID duplicado detectado: " + v.getId(), idsUnicos.add(v.getId()));
                coletadas.add(v);
            }
            cursor = pagina.getProximoCursor();
            if (pagina.isTemMais()) {
                assertNotNull(cursor);
            } else {
                assertNull(cursor);
            }
        } while (cursor != null);

        assertEquals(5, paginas);
        assertEquals(125, coletadas.size());
        assertEquals(125, idsUnicos.size());

        // Valida ordenação estrita (data DESC, hora DESC, id DESC)
        for (int i = 1; i < coletadas.size(); i++) {
            Viagem prev = coletadas.get(i - 1);
            Viagem curr = coletadas.get(i);
            int cmpData = prev.getData().compareTo(curr.getData());
            if (cmpData == 0) {
                int cmpHora = prev.getHora().compareTo(curr.getHora());
                if (cmpHora == 0) {
                    assertTrue(prev.getId() > curr.getId());
                } else {
                    assertTrue(cmpHora > 0);
                }
            } else {
                assertTrue(cmpData > 0);
            }
        }
    }

    @Test
    public void getGruposESomenteEmpresas_agrupaEOrdenaCorretamente() {
        List<Viagem> lote = new ArrayList<>();
        lote.add(criarViagem("Itu", "2026-02-10", "08:00", "Sorocaba", "D1", "Marcelo", "Comum", 50f));
        lote.add(criarViagem("Itu", "2026-02-11", "09:00", "Campinas", "D2", "Marcelo", "Comum", 80f));
        lote.add(criarViagem("Itu", "2026-02-12", "10:00", "Jundiai", "D3", "Marcelo", "Quartzolit", 90f));
        lote.add(criarViagem("Itu", "2026-03-01", "11:00", "Sao Paulo", "D4", "Marcelo", "Prefeitura", 120f));
        lote.add(criarViagem("Itu", "2026-03-02", "12:00", "Salto", "D5", "Marcelo", "", 40f));
        viagemDAO.inserirLote(lote, true);

        List<GrupoViagens> todosGrupos = viagemDAO.getGrupos(new ViagensFiltro("", false));
        assertEquals(4, todosGrupos.size());
        assertEquals("2026-03", todosGrupos.get(0).getMesAno());
        assertEquals(1, todosGrupos.get(0).getTotalViagens());
        assertEquals("2026-03-Prefeitura", todosGrupos.get(1).getMesAno());
        assertEquals(1, todosGrupos.get(1).getTotalViagens());
        assertEquals("2026-02", todosGrupos.get(2).getMesAno());
        assertEquals(2, todosGrupos.get(2).getTotalViagens());
        assertEquals("2026-02-Quartzolit", todosGrupos.get(3).getMesAno());
        assertEquals(1, todosGrupos.get(3).getTotalViagens());

        List<GrupoViagens> apenasEmpresas = viagemDAO.getGrupos(new ViagensFiltro("", true));
        assertEquals(2, apenasEmpresas.size());
        assertEquals("2026-03-Prefeitura", apenasEmpresas.get(0).getMesAno());
        assertEquals("2026-02-Quartzolit", apenasEmpresas.get(1).getMesAno());
    }

    @Test
    public void buscaLikeNos6CamposEEscapeCaracteresEspeciais() {
        List<Viagem> lote = new ArrayList<>();
        lote.add(criarViagem("OrigemAlpha", "2026-04-01", "08:00", "DestBeta", "DescGama", "MotDelta", "Comum", 10f));
        lote.add(criarViagem("Itu", "2026-04-02", "09:00", "Salto", "Desconto 100% aplicado", "Marcelo", "Comum", 20f));
        lote.add(criarViagem("Itu", "2026-04-03", "10:00", "Salto", "Desconto 1000 reais", "Marcelo", "Comum", 30f));
        lote.add(criarViagem("Itu", "2026-04-04", "11:00", "Salto", "trecho_1 especial", "Marcelo", "Empresa_VIP", 40f));
        lote.add(criarViagem("Itu", "2026-04-05", "12:00", "Salto", "trechoX1 normal", "Marcelo", "Comum", 50f));
        viagemDAO.inserirLote(lote, true);

        // Testa os 6 campos
        assertEquals(1, contarResultadosBusca("OrigemAlpha"));
        assertEquals(1, contarResultadosBusca("DestBeta"));
        assertEquals(1, contarResultadosBusca("2026-04-01"));
        assertEquals(1, contarResultadosBusca("MotDelta"));
        assertEquals(1, contarResultadosBusca("DescGama"));
        assertEquals(1, contarResultadosBusca("Empresa_VIP"));

        // Testa escape de '%'
        List<GrupoViagens> gruposPorcento = viagemDAO.getGrupos(new ViagensFiltro("100%", false));
        assertEquals(1, gruposPorcento.size());
        assertEquals(1, gruposPorcento.get(0).getTotalViagens());

        // Testa escape de '_'
        List<GrupoViagens> gruposUnderscore = viagemDAO.getGrupos(new ViagensFiltro("trecho_1", false));
        assertEquals(1, gruposUnderscore.size());
        assertEquals(1, gruposUnderscore.get(0).getTotalViagens());
    }

    @Test
    public void getViagensDoGrupoParaPdf_retornaTodasOrdenadasAscESemFiltroDeBusca() {
        List<Viagem> lote = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            String data = String.format(Locale.US, "2026-05-%02d", (i % 20) + 1);
            String hora = String.format(Locale.US, "%02d:15", i % 24);
            lote.add(criarViagem("Itu", data, hora, "Destino " + i, "Desc", "Marcelo", "Comum", 10f + i));
        }
        lote.add(criarViagem("Itu", "2026-05-10", "10:00", "Outro", "Desc", "Marcelo", "Quartzolit", 99f));
        viagemDAO.inserirLote(lote, true);

        PaginaViagensResult paginaComFiltro = viagemDAO.getPaginaDoGrupo(
                "2026-05",
                new ViagensFiltro("Destino 1", false),
                null,
                10
        );
        assertEquals(10, paginaComFiltro.getItens().size());
        assertTrue(paginaComFiltro.isTemMais());

        List<Viagem> paraPdf = viagemDAO.getViagensDoGrupoParaPdf("2026-05");
        assertEquals(45, paraPdf.size());
        for (int i = 1; i < paraPdf.size(); i++) {
            Viagem prev = paraPdf.get(i - 1);
            Viagem curr = paraPdf.get(i);
            int cmpData = prev.getData().compareTo(curr.getData());
            if (cmpData == 0) {
                int cmpHora = prev.getHora().compareTo(curr.getHora());
                if (cmpHora == 0) {
                    assertTrue(prev.getId() < curr.getId());
                } else {
                    assertTrue(cmpHora < 0);
                }
            } else {
                assertTrue(cmpData < 0);
            }
        }
    }

    @Test
    public void acessoConcorrenteMultiThread_comWal_executaSemExcecoes() throws Exception {
        String fileDbName = "concurrent_test_" + System.nanoTime() + ".db";
        DbHelper fileHelper = new DbHelper(context, fileDbName);
        ViagemDAO concurrentDao = new ViagemDAO(fileHelper);

        try {
            ExecutorService pool = Executors.newFixedThreadPool(4);
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int t = 0; t < 2; t++) {
                final int threadIdx = t;
                tasks.add(() -> {
                    for (int i = 0; i < 20; i++) {
                        List<Viagem> batch = new ArrayList<>();
                        batch.add(criarViagem(
                                "Origem" + threadIdx,
                                "2026-06-01",
                                String.format(Locale.US, "%02d:00", i % 24),
                                "Destino" + i,
                                "Concorrencia",
                                "Marcelo",
                                "Comum",
                                40f
                        ));
                        concurrentDao.inserirLote(batch, false);
                    }
                    return null;
                });
            }

            for (int t = 0; t < 2; t++) {
                tasks.add(() -> {
                    for (int i = 0; i < 25; i++) {
                        concurrentDao.getGrupos(new ViagensFiltro("", false));
                        concurrentDao.getPaginaDoGrupo("2026-06", new ViagensFiltro("", false), null, 30);
                    }
                    return null;
                });
            }

            List<Future<Void>> futures = pool.invokeAll(tasks);
            for (Future<Void> f : futures) {
                f.get();
            }
            pool.shutdown();

            assertTrue(concurrentDao.getCount() > 0);
            assertFalse(concurrentDao.getGrupos(new ViagensFiltro("", false)).isEmpty());
        } finally {
            fileHelper.close();
            context.deleteDatabase(fileDbName);
        }
    }

    @Test
    public void solicitante_persisteAtualizaEFiltraNaPaginacao() {
        Viagem v1 = criarViagem("Itu", "2026-05-10", "08:30", "Jundiaí", "Entrega", "Marcelo", "Quartzolit", 95f);
        v1.setSolicitante("Ana Paula");
        Viagem v2 = criarViagem("Itu", "2026-05-11", "09:00", "Campinas", "Reunião", "Marcelo", "Comum", 140f);
        v2.setSolicitante("Bruno Lima");

        long id1 = viagemDAO.inserir(v1);
        viagemDAO.inserir(v2);

        Viagem carregada = viagemDAO.getViagemById(id1);
        assertNotNull(carregada);
        assertEquals("Ana Paula", carregada.getSolicitante());

        assertEquals(1, contarResultadosBusca("Ana Paula"));

        carregada.setSolicitante("Carla Souza");
        assertEquals(1, viagemDAO.update(carregada));

        PaginaViagensResult pagina = viagemDAO.getPaginaDoGrupo(
                "2026-05-Quartzolit",
                new ViagensFiltro("Carla", false),
                null,
                30
        );
        assertEquals(1, pagina.getItens().size());
        assertEquals("Carla Souza", pagina.getItens().get(0).getSolicitante());
    }

    @Test
    public void multiServicos_persisteAtualizaHidrataEmLoteEBuscaPorDescricaoDoServico() {
        Viagem v1 = criarViagem("Itu", "2026-07-10", "08:00", "São Paulo", "Viagem executiva", "Marcelo", "Quartzolit", 100f);
        v1.setServicos(Arrays.asList(
                new Servico("Pedágio Castelo", 22.5f),
                new Servico("Estacionamento Aeroporto", 45.0f)
        ));

        Viagem v2 = criarViagem("Itu", "2026-07-11", "09:00", "Campinas", "Viagem simples", "Marcelo", "Comum", 80f);

        long id1 = viagemDAO.inserir(v1);
        long id2 = viagemDAO.inserir(v2);

        // Hidratação unitária
        Viagem carregada1 = viagemDAO.getViagemById(id1);
        assertNotNull(carregada1);
        assertEquals(2, carregada1.getServicos().size());
        assertEquals(67.5f, carregada1.getValorTotalServicos(), 0.01f);
        assertEquals(87.5f, carregada1.getValorTotal(), 0.01f);

        // Busca pelo nome do serviço na tabela servico
        assertEquals(1, contarResultadosBusca("Castelo"));
        assertEquals(1, contarResultadosBusca("Estacionamento Aeroporto"));
        assertEquals(0, contarResultadosBusca("Balsa"));

        // Hidratação em lote na paginação e no PDF
        PaginaViagensResult pagina = viagemDAO.getPaginaDoGrupo(
                "2026-07-Quartzolit",
                new ViagensFiltro("Castelo", false),
                null,
                30
        );
        assertEquals(1, pagina.getItens().size());
        assertEquals(2, pagina.getItens().get(0).getServicos().size());

        List<Viagem> paraPdf = viagemDAO.getViagensDoGrupoParaPdf("2026-07-Quartzolit");
        assertEquals(1, paraPdf.size());
        assertEquals(2, paraPdf.get(0).getServicos().size());

        // Atualização removendo todos os serviços zera valor_servico e não cria serviço fantasma
        carregada1.setServicos(Collections.emptyList());
        assertEquals(1, viagemDAO.update(carregada1));

        Viagem aposLimpeza = viagemDAO.getViagemById(id1);
        assertNotNull(aposLimpeza);
        assertTrue(aposLimpeza.getServicos().isEmpty());
        assertEquals(0f, aposLimpeza.getValorServico(), 0.01f);
        assertTrue(new ServicoDAO(dbHelper).getServicosPorViagem(id1).isEmpty());
        assertNotNull(viagemDAO.getViagemById(id2));
    }

    @Test
    public void getServicosPorViagens_acimaDe500Ids_fazChunkingSemExcecaoSqlite() {
        List<Viagem> lote = new ArrayList<>(650);
        for (int i = 0; i < 650; i++) {
            Viagem v = criarViagem("Itu", "2026-08-01", "10:00", "Destino " + i, "Desc " + i, "Marcelo", "Comum", 30f);
            v.setServicos(Collections.singletonList(new Servico("Pedágio " + i, 10f)));
            lote.add(v);
        }
        assertEquals(650, viagemDAO.inserirLote(lote, true));

        List<Viagem> todasParaPdf = viagemDAO.getViagensDoGrupoParaPdf("2026-08");
        assertEquals(650, todasParaPdf.size());
        for (Viagem v : todasParaPdf) {
            assertEquals(1, v.getServicos().size());
            assertEquals(10f, v.getValorTotalServicos(), 0.01f);
        }
    }

    private int contarResultadosBusca(String query) {
        List<GrupoViagens> grupos = viagemDAO.getGrupos(new ViagensFiltro(query, false));
        int total = 0;
        for (GrupoViagens g : grupos) {
            total += g.getTotalViagens();
        }
        return total;
    }

    private Viagem criarViagem(
            String origem,
            String data,
            String hora,
            String destino,
            String descricao,
            String motorista,
            String classificacao,
            float valorTotal
    ) {
        return new Viagem(
                0L,
                origem,
                data,
                hora,
                destino,
                descricao,
                10f,
                20f,
                0f,
                motorista,
                0f,
                0f,
                classificacao,
                valorTotal
        );
    }
}
