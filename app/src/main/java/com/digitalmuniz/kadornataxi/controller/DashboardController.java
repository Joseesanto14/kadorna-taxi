package com.digitalmuniz.kadornataxi.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.digitalmuniz.kadornataxi.data.dao.CustoDAO;
import com.digitalmuniz.kadornataxi.data.dao.DashboardDAO;
import com.digitalmuniz.kadornataxi.model.entities.Custo;
import com.digitalmuniz.kadornataxi.util.PeriodoInicialResolver;
import com.digitalmuniz.kadornataxi.view.dto.ClassificacaoResumoDTO;
import com.digitalmuniz.kadornataxi.view.dto.DashboardDadosDTO;
import com.digitalmuniz.kadornataxi.view.dto.DashboardResumoDTO;
import com.digitalmuniz.kadornataxi.view.dto.FaturamentoMensalDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class DashboardController {

    public interface Callback {
        void onCarregando();

        void onDadosCarregados(DashboardDadosDTO dados);

        void onSemDados();

        void onErro(String mensagem);
    }

    public interface OperacaoCallback {
        void onSucesso();

        void onErro(String mensagem);
    }

    private final DashboardDAO dashboardDAO;
    private final CustoDAO custoDAO;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Supplier<LocalDate> dataReferenciaSupplier;
    private final AtomicLong generation = new AtomicLong(0);

    private String anoSelecionadoAtual;
    private String mesSelecionadoAtual;

    public DashboardController(Context context) {
        this(
                new DashboardDAO(context),
                new CustoDAO(context),
                Executors.newSingleThreadExecutor(),
                new Handler(Looper.getMainLooper()),
                LocalDate::now
        );
    }

    public DashboardController(
            DashboardDAO dashboardDAO,
            ExecutorService executor,
            Handler mainHandler
    ) {
        this(dashboardDAO, null, executor, mainHandler, LocalDate::now);
    }

    public DashboardController(
            DashboardDAO dashboardDAO,
            ExecutorService executor,
            Handler mainHandler,
            Supplier<LocalDate> dataReferenciaSupplier
    ) {
        this(dashboardDAO, null, executor, mainHandler, dataReferenciaSupplier);
    }

    public DashboardController(
            DashboardDAO dashboardDAO,
            CustoDAO custoDAO,
            ExecutorService executor,
            Handler mainHandler,
            Supplier<LocalDate> dataReferenciaSupplier
    ) {
        this.dashboardDAO = dashboardDAO;
        this.custoDAO = custoDAO;
        this.executor = executor;
        this.mainHandler = mainHandler;
        this.dataReferenciaSupplier = dataReferenciaSupplier != null ? dataReferenciaSupplier : LocalDate::now;
    }

    public void carregarInicial(Callback callback) {
        if (executor.isShutdown()) {
            return;
        }
        final long gen = generation.incrementAndGet();
        postarCarregando(gen, callback);

        executor.execute(() -> {
            try {
                List<String> anos = dashboardDAO.buscarAnosDisponiveis();
                if (anos.isEmpty()) {
                    postarSemDados(gen, callback);
                    return;
                }

                PeriodoInicialResolver.Resultado inicial = PeriodoInicialResolver.resolver(
                        dataReferenciaSupplier.get(),
                        anos,
                        dashboardDAO::buscarMesesDisponiveisPorAno
                );

                if (inicial.isVazio()) {
                    postarSemDados(gen, callback);
                    return;
                }

                DashboardDadosDTO dados = montarDashboardDados(
                        anos,
                        inicial.getMesesDoAno(),
                        inicial.getAnoSelecionado(),
                        inicial.getMesSelecionado()
                );
                postarDados(gen, dados, callback);
            } catch (Exception e) {
                postarErro(gen, e.getMessage(), callback);
            }
        });
    }

    public void selecionarPeriodo(String ano, String mesOuNull, Callback callback) {
        if (ano == null || ano.trim().isEmpty()) {
            carregarInicial(callback);
            return;
        }
        if (executor.isShutdown()) {
            return;
        }
        final long gen = generation.incrementAndGet();
        final String anoSolicitado = ano.trim();
        final String mesSolicitado = (mesOuNull != null && !mesOuNull.trim().isEmpty())
                ? mesOuNull.trim()
                : null;

        postarCarregando(gen, callback);

        executor.execute(() -> {
            try {
                List<String> anos = dashboardDAO.buscarAnosDisponiveis();
                if (anos.isEmpty()) {
                    postarSemDados(gen, callback);
                    return;
                }

                String anoEfetivo = anoSolicitado;
                List<String> mesesDisponiveis;
                String mesEfetivo;

                if (anos.contains(anoEfetivo)) {
                    mesesDisponiveis = dashboardDAO.buscarMesesDisponiveisPorAno(anoEfetivo);
                    if (mesesDisponiveis.isEmpty()) {
                        PeriodoInicialResolver.Resultado fallback = PeriodoInicialResolver.resolver(
                                dataReferenciaSupplier.get(),
                                anos,
                                dashboardDAO::buscarMesesDisponiveisPorAno
                        );
                        if (fallback.isVazio()) {
                            postarSemDados(gen, callback);
                            return;
                        }
                        anoEfetivo = fallback.getAnoSelecionado();
                        mesesDisponiveis = fallback.getMesesDoAno();
                        mesEfetivo = fallback.getMesSelecionado();
                    } else {
                        mesEfetivo = (mesSolicitado != null && mesesDisponiveis.contains(mesSolicitado))
                                ? mesSolicitado
                                : null;
                    }
                } else {
                    PeriodoInicialResolver.Resultado fallback = PeriodoInicialResolver.resolver(
                            dataReferenciaSupplier.get(),
                            anos,
                            dashboardDAO::buscarMesesDisponiveisPorAno
                    );
                    if (fallback.isVazio()) {
                        postarSemDados(gen, callback);
                        return;
                    }
                    anoEfetivo = fallback.getAnoSelecionado();
                    mesesDisponiveis = fallback.getMesesDoAno();
                    mesEfetivo = fallback.getMesSelecionado();
                }

                DashboardDadosDTO dados = montarDashboardDados(anos, mesesDisponiveis, anoEfetivo, mesEfetivo);
                postarDados(gen, dados, callback);
            } catch (Exception e) {
                postarErro(gen, e.getMessage(), callback);
            }
        });
    }

    public void recarregarAtual(Callback callback) {
        if (anoSelecionadoAtual == null) {
            carregarInicial(callback);
        } else {
            selecionarPeriodo(anoSelecionadoAtual, mesSelecionadoAtual, callback);
        }
    }

    public void salvarCusto(Custo custo, OperacaoCallback callback) {
        if (executor.isShutdown()) {
            return;
        }
        if (custoDAO == null || custo == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onErro("Dados inválidos para salvar custo."));
            }
            return;
        }
        executor.execute(() -> {
            try {
                boolean sucesso;
                if (custo.getId() > 0L) {
                    sucesso = custoDAO.atualizar(custo) > 0;
                } else {
                    sucesso = custoDAO.inserir(custo) > 0L;
                }
                mainHandler.post(() -> {
                    if (callback != null) {
                        if (sucesso) {
                            callback.onSucesso();
                        } else {
                            callback.onErro("Não foi possível salvar o custo.");
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

    public void excluirCusto(long idCusto, OperacaoCallback callback) {
        if (executor.isShutdown()) {
            return;
        }
        if (custoDAO == null || idCusto <= 0L) {
            if (callback != null) {
                mainHandler.post(() -> callback.onErro("Custo inválido para exclusão."));
            }
            return;
        }
        executor.execute(() -> {
            try {
                boolean sucesso = custoDAO.deletar(idCusto) > 0;
                mainHandler.post(() -> {
                    if (callback != null) {
                        if (sucesso) {
                            callback.onSucesso();
                        } else {
                            callback.onErro("Custo não encontrado.");
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

    public void shutdown() {
        generation.incrementAndGet();
        executor.shutdownNow();
    }

    private DashboardDadosDTO montarDashboardDados(
            List<String> anos,
            List<String> mesesDisponiveis,
            String anoSelecionado,
            String mesSelecionado
    ) {
        DashboardResumoDTO resumo = dashboardDAO.buscarResumoPorPeriodo(anoSelecionado, mesSelecionado);
        List<FaturamentoMensalDTO> evolucao = dashboardDAO.buscarEvolucaoMensalDoAno(anoSelecionado);
        List<ClassificacaoResumoDTO> ranking = dashboardDAO.buscarRankingClassificacao(anoSelecionado, mesSelecionado);
        List<Custo> custos = dashboardDAO.buscarCustosPorPeriodo(anoSelecionado, mesSelecionado);
        return new DashboardDadosDTO(
                anos,
                mesesDisponiveis,
                anoSelecionado,
                mesSelecionado,
                resumo,
                evolucao,
                ranking,
                custos
        );
    }

    private void postarCarregando(long gen, Callback callback) {
        mainHandler.post(() -> {
            if (gen == generation.get() && callback != null) {
                callback.onCarregando();
            }
        });
    }

    private void postarSemDados(long gen, Callback callback) {
        mainHandler.post(() -> {
            if (gen != generation.get()) {
                return;
            }
            anoSelecionadoAtual = null;
            mesSelecionadoAtual = null;
            if (callback != null) {
                callback.onSemDados();
            }
        });
    }

    private void postarDados(long gen, DashboardDadosDTO dados, Callback callback) {
        mainHandler.post(() -> {
            if (gen != generation.get()) {
                return;
            }
            anoSelecionadoAtual = dados.getAnoSelecionado();
            mesSelecionadoAtual = dados.getMesSelecionado();
            if (callback != null) {
                callback.onDadosCarregados(dados);
            }
        });
    }

    private void postarErro(long gen, String mensagem, Callback callback) {
        mainHandler.post(() -> {
            if (gen == generation.get() && callback != null) {
                callback.onErro(mensagem);
            }
        });
    }
}
