package com.digitalmuniz.kadornataxi;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.controller.ViagensController;
import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.dto.GrupoViagens;
import com.digitalmuniz.kadornataxi.view.dto.ListaViagensItem;
import com.digitalmuniz.kadornataxi.view.dto.ViagensFiltro;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ViagensControllerTest {

    private DbHelper dbHelper;
    private ViagemDAO viagemDAO;
    private ConfiguracaoDAO configuracaoDAO;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        dbHelper = new DbHelper(context, null);
        viagemDAO = new ViagemDAO(dbHelper);
        configuracaoDAO = new ConfiguracaoDAO(dbHelper);

        List<Viagem> lote = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            lote.add(new Viagem(
                    0L,
                    "Itu",
                    "2026-03-" + String.format("%02d", (i % 20) + 1),
                    "10:00",
                    "Sorocaba " + i,
                    "Corrida " + i,
                    10f,
                    20f,
                    0f,
                    "Marcelo",
                    0f,
                    0f,
                    "Comum",
                    20f
            ));
        }
        lote.add(new Viagem(
                0L,
                "Itu",
                "2026-02-15",
                "09:00",
                "Campinas",
                "Empresa",
                20f,
                40f,
                0f,
                "Marcelo",
                0f,
                0f,
                "Quartzolit",
                40f
        ));
        viagemDAO.inserirLote(lote, true);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void agendarBusca_respeitaDebounceDe300ms() {
        AtomicInteger chamadasGetGrupos = new AtomicInteger(0);
        ViagemDAO daoContado = new ViagemDAO(dbHelper) {
            @Override
            public List<GrupoViagens> getGrupos(ViagensFiltro filtro) {
                chamadasGetGrupos.incrementAndGet();
                return super.getGrupos(filtro);
            }
        };

        ViagensController controller = new ViagensController(
                daoContado,
                configuracaoDAO,
                new DirectExecutorService(),
                new Handler(Looper.getMainLooper()),
                30
        );

        AtomicReference<List<ListaViagensItem>> ultimaLista = new AtomicReference<>();
        ViagensController.Callback callback = new ViagensController.Callback() {
            @Override
            public void onListaAtualizada(List<ListaViagensItem> itens, boolean loading) {
                ultimaLista.set(itens);
            }

            @Override
            public void onErro(String mensagem) {
                throw new AssertionError("Erro inesperado: " + mensagem);
            }
        };

        controller.agendarBusca(new ViagensFiltro("S", false), callback);
        controller.agendarBusca(new ViagensFiltro("So", false), callback);
        controller.agendarBusca(new ViagensFiltro("Sorocaba 15", false), callback);

        // Antes de 300ms nenhuma consulta deve ter rodado
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(250, TimeUnit.MILLISECONDS);
        assertEquals(0, chamadasGetGrupos.get());

        // Após completar 300ms apenas a última consulta deve executar
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS);
        assertEquals(1, chamadasGetGrupos.get());
        assertTrue(controller.isGrupoExpandido("2026-03"));
        assertTrue(ultimaLista.get().stream().anyMatch(
                item -> item.getViewType() == ListaViagensItem.TYPE_VIAGEM
                        && "Sorocaba 15".equals(item.getViagem().getDestino())
        ));
        controller.shutdown();
    }

    @Test
    public void consultaGeneration_descartaResultadoDeConsultaObsoleta() throws Exception {
        CountDownLatch primeiraIniciada = new CountDownLatch(1);
        CountDownLatch liberarPrimeira = new CountDownLatch(1);

        ViagemDAO daoComAtraso = new ViagemDAO(dbHelper) {
            @Override
            public List<GrupoViagens> getGrupos(ViagensFiltro filtro) {
                if ("LENTA".equals(filtro.getTextoBusca())) {
                    primeiraIniciada.countDown();
                    try {
                        liberarPrimeira.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                    }
                }
                return super.getGrupos(filtro);
            }
        };

        ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        ViagensController controller = new ViagensController(
                daoComAtraso,
                configuracaoDAO,
                pool,
                new Handler(Looper.getMainLooper()),
                30
        );

        List<List<ListaViagensItem>> entregas = new ArrayList<>();
        ViagensController.Callback callback = new ViagensController.Callback() {
            @Override
            public void onListaAtualizada(List<ListaViagensItem> itens, boolean loading) {
                entregas.add(itens);
            }

            @Override
            public void onErro(String mensagem) {
                throw new AssertionError("Erro inesperado: " + mensagem);
            }
        };

        // Dispara consulta 1 (lenta, sem match -> retornaria lista vazia se não fosse descartada)
        controller.carregarGrupos(new ViagensFiltro("LENTA", false), callback);
        assertTrue(primeiraIniciada.await(2, TimeUnit.SECONDS));

        // Dispara consulta 2 (rápida -> Campinas)
        controller.carregarGrupos(new ViagensFiltro("Campinas", false), callback);
        // Aguarda consulta 2 postar na main thread
        Thread.sleep(150);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Libera consulta 1 atrasada
        liberarPrimeira.countDown();
        Thread.sleep(150);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Apenas a consulta 2 deve ter sido entregue; a obsoleta foi descartada
        assertEquals(1, entregas.size());
        assertTrue(entregas.get(0).stream().anyMatch(
                item -> item.getViewType() == ListaViagensItem.TYPE_HEADER_GRUPO
                        && "2026-02-Quartzolit".equals(item.getMesAno())
        ));
        controller.shutdown();
    }

    @Test
    public void expandirEColapsarGrupo_carregaPaginaELiberaItensEmMemoriaAoColapsar() {
        ViagensController controller = new ViagensController(
                viagemDAO,
                configuracaoDAO,
                new DirectExecutorService(),
                new Handler(Looper.getMainLooper()),
                30
        );

        AtomicReference<List<ListaViagensItem>> ultimaLista = new AtomicReference<>();
        ViagensController.Callback callback = new ViagensController.Callback() {
            @Override
            public void onListaAtualizada(List<ListaViagensItem> itens, boolean loading) {
                ultimaLista.set(itens);
            }

            @Override
            public void onErro(String mensagem) {
                throw new AssertionError("Erro inesperado: " + mensagem);
            }
        };

        controller.carregarGrupos(new ViagensFiltro("", false), callback);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(controller.isGrupoExpandido("2026-03"));
        assertEquals(0, controller.getItensEmMemoriaCount("2026-03"));

        // Expandir carrega 30 itens (página 1)
        controller.alternarExpansaoGrupo("2026-03", callback);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(controller.isGrupoExpandido("2026-03"));
        assertEquals(30, controller.getItensEmMemoriaCount("2026-03"));
        assertTrue(controller.podePaginarGrupo("2026-03"));

        // Colapsar descarta itens em memória
        controller.alternarExpansaoGrupo("2026-03", callback);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(controller.isGrupoExpandido("2026-03"));
        assertEquals(0, controller.getItensEmMemoriaCount("2026-03"));
        assertFalse(ultimaLista.get().stream().anyMatch(
                item -> item.getViewType() == ListaViagensItem.TYPE_VIAGEM
        ));
        controller.shutdown();
    }

    private static class DirectExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            if (!shutdown) {
                command.run();
            }
        }
    }
}
