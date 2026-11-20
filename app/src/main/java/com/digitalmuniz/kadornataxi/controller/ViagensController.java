package com.digitalmuniz.kadornataxi.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.dto.GrupoViagens;
import com.digitalmuniz.kadornataxi.view.dto.ListaViagensItem;
import com.digitalmuniz.kadornataxi.view.dto.PaginaViagensResult;
import com.digitalmuniz.kadornataxi.view.dto.ViagemCursorKey;
import com.digitalmuniz.kadornataxi.view.dto.ViagensFiltro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ViagensController {

    public static final int DEFAULT_PAGE_SIZE = 30;
    public static final long DEBOUNCE_DELAY_MS = 300L;

    public interface Callback {
        void onListaAtualizada(List<ListaViagensItem> itens, boolean loading);

        void onErro(String mensagem);
    }

    public interface PdfCallback {
        void onConfiguracaoAusente();

        void onPdfGerado(String mesAno);

        void onErro(String mensagem);
    }

    static class GrupoState {
        final List<Viagem> itens = new ArrayList<>();
        ViagemCursorKey proximoCursor;
        boolean temMais;
        boolean carregando;
        boolean carregado;
    }

    private final ViagemDAO viagemDAO;
    private final ConfiguracaoDAO configuracaoDAO;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final int pageSize;

    private final AtomicLong consultaGeneration = new AtomicLong(0);
    private final Map<String, GrupoState> gruposState = new LinkedHashMap<>();
    private final Set<String> gruposExpandidos = new LinkedHashSet<>();
    private List<GrupoViagens> gruposAtuais = new ArrayList<>();
    private ViagensFiltro filtroAtual = new ViagensFiltro("", false);
    private Runnable pendingDebounceRunnable;

    public ViagensController(Context context) {
        this(
                new ViagemDAO(context),
                new ConfiguracaoDAO(context),
                Executors.newSingleThreadExecutor(),
                new Handler(Looper.getMainLooper()),
                DEFAULT_PAGE_SIZE
        );
    }

    public ViagensController(
            ViagemDAO viagemDAO,
            ConfiguracaoDAO configuracaoDAO,
            ExecutorService executor,
            Handler mainHandler,
            int pageSize
    ) {
        this.viagemDAO = viagemDAO;
        this.configuracaoDAO = configuracaoDAO;
        this.executor = executor;
        this.mainHandler = mainHandler;
        this.pageSize = Math.max(1, pageSize);
    }

    public void agendarBusca(ViagensFiltro filtro, Callback callback) {
        cancelarDebouncePendente();
        final ViagensFiltro filtroFinal = filtro != null ? filtro : new ViagensFiltro("", false);
        // Invalida qualquer consulta anterior ainda em voo antes mesmo do debounce disparar.
        final long generation = consultaGeneration.incrementAndGet();
        pendingDebounceRunnable = () -> {
            pendingDebounceRunnable = null;
            executarCarregarGrupos(filtroFinal, generation, callback);
        };
        mainHandler.postDelayed(pendingDebounceRunnable, DEBOUNCE_DELAY_MS);
    }

    public void carregarGrupos(ViagensFiltro filtro, Callback callback) {
        cancelarDebouncePendente();
        ViagensFiltro filtroFinal = filtro != null ? filtro : new ViagensFiltro("", false);
        long generation = consultaGeneration.incrementAndGet();
        executarCarregarGrupos(filtroFinal, generation, callback);
    }

    private void executarCarregarGrupos(ViagensFiltro filtroFinal, long generation, Callback callback) {
        filtroAtual = filtroFinal;
        gruposState.clear();
        gruposExpandidos.clear();

        if (executor.isShutdown()) {
            return;
        }

        executor.execute(() -> {
            try {
                List<GrupoViagens> grupos = viagemDAO.getGrupos(filtroFinal);
                String primeiroGrupoExpandido = null;
                PaginaViagensResult primeiraPagina = null;

                if (filtroFinal.hasTextoBusca() && !grupos.isEmpty()) {
                    primeiroGrupoExpandido = grupos.get(0).getMesAno();
                    primeiraPagina = viagemDAO.getPaginaDoGrupo(primeiroGrupoExpandido, filtroFinal, null, pageSize);
                }

                final String grupoAuto = primeiroGrupoExpandido;
                final PaginaViagensResult paginaAuto = primeiraPagina;

                mainHandler.post(() -> {
                    if (generation != consultaGeneration.get()) {
                        return;
                    }
                    gruposAtuais = new ArrayList<>(grupos);
                    gruposState.clear();
                    gruposExpandidos.clear();

                    if (grupoAuto != null && paginaAuto != null) {
                        gruposExpandidos.add(grupoAuto);
                        GrupoState state = new GrupoState();
                        state.itens.addAll(paginaAuto.getItens());
                        state.temMais = paginaAuto.isTemMais();
                        state.proximoCursor = paginaAuto.getProximoCursor();
                        state.carregado = true;
                        state.carregando = false;
                        gruposState.put(grupoAuto, state);
                    }

                    if (callback != null) {
                        callback.onListaAtualizada(construirListaPlana(), false);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (generation == consultaGeneration.get() && callback != null) {
                        callback.onErro(e.getMessage());
                    }
                });
            }
        });
    }

    public void alternarExpansaoGrupo(String mesAno, Callback callback) {
        if (mesAno == null) {
            return;
        }

        if (gruposExpandidos.contains(mesAno)) {
            gruposExpandidos.remove(mesAno);
            gruposState.remove(mesAno);
            if (callback != null) {
                callback.onListaAtualizada(construirListaPlana(), false);
            }
            return;
        }

        gruposExpandidos.add(mesAno);
        GrupoState state = new GrupoState();
        state.carregando = true;
        gruposState.put(mesAno, state);

        if (callback != null) {
            callback.onListaAtualizada(construirListaPlana(), false);
        }

        final long generation = consultaGeneration.get();
        final ViagensFiltro filtroSnapshot = filtroAtual;

        if (executor.isShutdown()) {
            return;
        }

        executor.execute(() -> {
            try {
                PaginaViagensResult pagina = viagemDAO.getPaginaDoGrupo(mesAno, filtroSnapshot, null, pageSize);
                mainHandler.post(() -> {
                    if (generation != consultaGeneration.get() || !gruposExpandidos.contains(mesAno)) {
                        return;
                    }
                    GrupoState currentState = gruposState.get(mesAno);
                    if (currentState == null) {
                        return;
                    }
                    currentState.itens.clear();
                    currentState.itens.addAll(pagina.getItens());
                    currentState.temMais = pagina.isTemMais();
                    currentState.proximoCursor = pagina.getProximoCursor();
                    currentState.carregado = true;
                    currentState.carregando = false;

                    if (callback != null) {
                        callback.onListaAtualizada(construirListaPlana(), false);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (generation != consultaGeneration.get()) {
                        return;
                    }
                    GrupoState currentState = gruposState.get(mesAno);
                    if (currentState != null) {
                        currentState.carregando = false;
                    }
                    if (callback != null) {
                        callback.onListaAtualizada(construirListaPlana(), false);
                        callback.onErro(e.getMessage());
                    }
                });
            }
        });
    }

    public void carregarProximaPagina(String mesAno, Callback callback) {
        if (mesAno == null || !gruposExpandidos.contains(mesAno)) {
            return;
        }
        GrupoState state = gruposState.get(mesAno);
        if (state == null || state.carregando || !state.temMais || state.proximoCursor == null) {
            return;
        }

        state.carregando = true;
        if (callback != null) {
            callback.onListaAtualizada(construirListaPlana(), false);
        }

        final long generation = consultaGeneration.get();
        final ViagensFiltro filtroSnapshot = filtroAtual;
        final ViagemCursorKey cursorSnapshot = state.proximoCursor;

        if (executor.isShutdown()) {
            return;
        }

        executor.execute(() -> {
            try {
                PaginaViagensResult pagina = viagemDAO.getPaginaDoGrupo(mesAno, filtroSnapshot, cursorSnapshot, pageSize);
                mainHandler.post(() -> {
                    if (generation != consultaGeneration.get() || !gruposExpandidos.contains(mesAno)) {
                        return;
                    }
                    GrupoState currentState = gruposState.get(mesAno);
                    if (currentState == null) {
                        return;
                    }
                    currentState.itens.addAll(pagina.getItens());
                    currentState.temMais = pagina.isTemMais();
                    currentState.proximoCursor = pagina.getProximoCursor();
                    currentState.carregando = false;

                    if (callback != null) {
                        callback.onListaAtualizada(construirListaPlana(), false);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (generation != consultaGeneration.get()) {
                        return;
                    }
                    GrupoState currentState = gruposState.get(mesAno);
                    if (currentState != null) {
                        currentState.carregando = false;
                    }
                    if (callback != null) {
                        callback.onListaAtualizada(construirListaPlana(), false);
                        callback.onErro(e.getMessage());
                    }
                });
            }
        });
    }

    public void excluirViagem(Viagem viagem, Callback callback) {
        if (viagem == null) {
            return;
        }
        cancelarDebouncePendente();
        final long generation = consultaGeneration.incrementAndGet();
        final ViagensFiltro filtroSnapshot = filtroAtual;
        final Set<String> expandidosSnapshot = new LinkedHashSet<>(gruposExpandidos);

        if (executor.isShutdown()) {
            return;
        }

        executor.execute(() -> {
            try {
                viagemDAO.delete(viagem.getId());
                List<GrupoViagens> novosGrupos = viagemDAO.getGrupos(filtroSnapshot);
                Map<String, PaginaViagensResult> paginasRecarregadas = new LinkedHashMap<>();

                for (GrupoViagens grupo : novosGrupos) {
                    if (expandidosSnapshot.contains(grupo.getMesAno())) {
                        paginasRecarregadas.put(
                                grupo.getMesAno(),
                                viagemDAO.getPaginaDoGrupo(grupo.getMesAno(), filtroSnapshot, null, pageSize)
                        );
                    }
                }

                mainHandler.post(() -> {
                    if (generation != consultaGeneration.get()) {
                        return;
                    }
                    gruposAtuais = new ArrayList<>(novosGrupos);
                    gruposState.clear();
                    gruposExpandidos.clear();

                    for (Map.Entry<String, PaginaViagensResult> entry : paginasRecarregadas.entrySet()) {
                        String mesAno = entry.getKey();
                        PaginaViagensResult pagina = entry.getValue();
                        gruposExpandidos.add(mesAno);
                        GrupoState state = new GrupoState();
                        state.itens.addAll(pagina.getItens());
                        state.temMais = pagina.isTemMais();
                        state.proximoCursor = pagina.getProximoCursor();
                        state.carregado = true;
                        state.carregando = false;
                        gruposState.put(mesAno, state);
                    }

                    if (callback != null) {
                        callback.onListaAtualizada(construirListaPlana(), false);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (generation == consultaGeneration.get() && callback != null) {
                        callback.onErro(e.getMessage());
                    }
                });
            }
        });
    }

    public void gerarRelatorioPdf(Context context, String mesAno, PdfCallback callback) {
        if (executor.isShutdown()) {
            return;
        }
        executor.execute(() -> {
            try {
                if (!configuracaoDAO.configExiste()) {
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onConfiguracaoAusente();
                        }
                    });
                    return;
                }

                Configuracao config = configuracaoDAO.getConfiguracao();
                List<Viagem> todasDoGrupo = viagemDAO.getViagensDoGrupoParaPdf(mesAno);

                mainHandler.post(() -> {
                    try {
                        RelatorioPdfGenerator gerador = new RelatorioPdfGenerator(context, todasDoGrupo, config, mesAno);
                        gerador.gerarRelatorio();
                        if (callback != null) {
                            callback.onPdfGerado(mesAno);
                        }
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onErro(e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onErro(e.getMessage());
                    }
                });
            }
        });
    }

    public boolean configuracaoExiste() {
        return configuracaoDAO.configExiste();
    }

    public boolean isGrupoExpandido(String mesAno) {
        return gruposExpandidos.contains(mesAno);
    }

    public int getItensEmMemoriaCount(String mesAno) {
        GrupoState state = gruposState.get(mesAno);
        return state != null ? state.itens.size() : 0;
    }

    public boolean podePaginarGrupo(String mesAno) {
        GrupoState state = gruposState.get(mesAno);
        return gruposExpandidos.contains(mesAno)
                && state != null
                && !state.carregando
                && state.temMais
                && state.proximoCursor != null;
    }

    public List<ListaViagensItem> construirListaPlana() {
        List<ListaViagensItem> itens = new ArrayList<>();
        for (GrupoViagens grupo : gruposAtuais) {
            String mesAno = grupo.getMesAno();
            boolean expandido = gruposExpandidos.contains(mesAno);
            itens.add(ListaViagensItem.headerGrupo(mesAno, grupo.getTotalViagens(), expandido));

            if (expandido) {
                itens.add(ListaViagensItem.botaoPdf(mesAno));
                GrupoState state = gruposState.get(mesAno);
                if (state != null) {
                    for (Viagem viagem : state.itens) {
                        itens.add(ListaViagensItem.viagem(mesAno, viagem));
                    }
                    if (state.carregando) {
                        itens.add(ListaViagensItem.loading(mesAno));
                    }
                }
            }
        }
        return Collections.unmodifiableList(itens);
    }

    private void cancelarDebouncePendente() {
        if (pendingDebounceRunnable != null) {
            mainHandler.removeCallbacks(pendingDebounceRunnable);
            pendingDebounceRunnable = null;
        }
    }

    public void shutdown() {
        cancelarDebouncePendente();
        consultaGeneration.incrementAndGet();
        executor.shutdownNow();
    }
}
