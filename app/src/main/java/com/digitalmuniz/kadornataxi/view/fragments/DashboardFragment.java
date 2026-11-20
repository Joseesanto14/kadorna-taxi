package com.digitalmuniz.kadornataxi.view.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.controller.DashboardController;
import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.util.FormatadorDashboard;
import com.digitalmuniz.kadornataxi.view.activities.ConfiguracaoActivity;
import com.digitalmuniz.kadornataxi.view.activities.SolicitacaoActivity;
import com.digitalmuniz.kadornataxi.view.adapters.ClassificacaoResumoAdapter;
import com.digitalmuniz.kadornataxi.view.custom.EvolucaoFaturamentoChartView;
import com.digitalmuniz.kadornataxi.view.dto.DashboardDadosDTO;
import com.digitalmuniz.kadornataxi.view.dto.DashboardResumoDTO;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Objects;

public class DashboardFragment extends Fragment {

    private DashboardController controller;
    private DashboardDadosDTO dadosAtuais;
    private boolean atualizandoChips;
    private boolean recarregarAoVoltar;

    private View dashboardContent;
    private View dashboardLoading;
    private View dashboardEmpty;
    private View dashboardError;

    private ChipGroup chipGroupAnos;
    private ChipGroup chipGroupMeses;

    private TextView txtPeriodoLabel;
    private LinearLayout badgeVariacao;
    private ImageView imgVariacaoIcon;
    private TextView txtVariacaoPercentual;
    private TextView txtFaturamentoTotal;
    private TextView txtComparacaoContexto;
    private TextView txtHeroResumoSecundario;

    private TextView txtKpiKms;
    private TextView txtKpiReaisPorKm;
    private TextView txtKpiHorasEspera;
    private TextView txtKpiValorEspera;
    private TextView txtKpiMediaDiaria;
    private TextView txtKpiDiasTrabalhados;
    private TextView txtKpiTotalServicos;
    private TextView txtKpiQtdServicos;
    private TextView txtKpiMediaServico;
    private TextView txtKpiViagensComServico;

    private TextView txtComposicaoKm;
    private LinearProgressIndicator progressComposicaoKm;
    private TextView txtComposicaoEspera;
    private LinearProgressIndicator progressComposicaoEspera;
    private TextView txtComposicaoServico;
    private LinearProgressIndicator progressComposicaoServico;

    private TextView txtEvolucaoMedia;
    private EvolucaoFaturamentoChartView evolucaoChart;

    private RecyclerView recyclerClassificacao;
    private ClassificacaoResumoAdapter classificacaoAdapter;

    private TextView txtDashboardErro;

    private final DashboardController.Callback dashboardCallback = new DashboardController.Callback() {
        @Override
        public void onCarregando() {
            if (!isAdded()) {
                return;
            }
            if (dadosAtuais == null) {
                mostrarEstado(EstadoTela.LOADING);
            }
        }

        @Override
        public void onDadosCarregados(DashboardDadosDTO dados) {
            if (!isAdded() || dados == null) {
                return;
            }
            dadosAtuais = dados;
            renderizarDados(dados);
            mostrarEstado(EstadoTela.CONTEUDO);
        }

        @Override
        public void onSemDados() {
            if (!isAdded()) {
                return;
            }
            dadosAtuais = null;
            mostrarEstado(EstadoTela.EMPTY);
        }

        @Override
        public void onErro(String mensagem) {
            if (!isAdded()) {
                return;
            }
            if (txtDashboardErro != null) {
                String texto = (mensagem != null && !mensagem.trim().isEmpty())
                        ? getString(R.string.dashboard_erro_titulo) + ": " + mensagem
                        : getString(R.string.dashboard_erro_titulo);
                txtDashboardErro.setText(texto);
            }
            mostrarEstado(EstadoTela.ERROR);
        }
    };

    private enum EstadoTela {
        LOADING,
        CONTEUDO,
        EMPTY,
        ERROR
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = new DashboardController(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vincularViews(view);
        configurarListeners();
        recarregarAoVoltar = false;
        controller.carregarInicial(dashboardCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        recarregarAoVoltar = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recarregarAoVoltar && !isHidden() && controller != null) {
            recarregarAoVoltar = false;
            controller.recarregarAtual(dashboardCallback);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isResumed() && controller != null) {
            recarregarAoVoltar = false;
            controller.recarregarAtual(dashboardCallback);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recyclerClassificacao != null) {
            recyclerClassificacao.setAdapter(null);
        }
        dashboardContent = null;
        dashboardLoading = null;
        dashboardEmpty = null;
        dashboardError = null;
        chipGroupAnos = null;
        chipGroupMeses = null;
        txtPeriodoLabel = null;
        badgeVariacao = null;
        imgVariacaoIcon = null;
        txtVariacaoPercentual = null;
        txtFaturamentoTotal = null;
        txtComparacaoContexto = null;
        txtHeroResumoSecundario = null;
        txtKpiKms = null;
        txtKpiReaisPorKm = null;
        txtKpiHorasEspera = null;
        txtKpiValorEspera = null;
        txtKpiMediaDiaria = null;
        txtKpiDiasTrabalhados = null;
        txtKpiTotalServicos = null;
        txtKpiQtdServicos = null;
        txtKpiMediaServico = null;
        txtKpiViagensComServico = null;
        txtComposicaoKm = null;
        progressComposicaoKm = null;
        txtComposicaoEspera = null;
        progressComposicaoEspera = null;
        txtComposicaoServico = null;
        progressComposicaoServico = null;
        txtEvolucaoMedia = null;
        evolucaoChart = null;
        recyclerClassificacao = null;
        classificacaoAdapter = null;
        txtDashboardErro = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (controller != null) {
            controller.shutdown();
        }
    }

    private void vincularViews(View root) {
        dashboardContent = root.findViewById(R.id.dashboardContent);
        dashboardLoading = root.findViewById(R.id.dashboardLoading);
        dashboardEmpty = root.findViewById(R.id.dashboardEmpty);
        dashboardError = root.findViewById(R.id.dashboardError);

        chipGroupAnos = root.findViewById(R.id.chipGroupAnos);
        chipGroupMeses = root.findViewById(R.id.chipGroupMeses);

        txtPeriodoLabel = root.findViewById(R.id.txtPeriodoLabel);
        badgeVariacao = root.findViewById(R.id.badgeVariacao);
        imgVariacaoIcon = root.findViewById(R.id.imgVariacaoIcon);
        txtVariacaoPercentual = root.findViewById(R.id.txtVariacaoPercentual);
        txtFaturamentoTotal = root.findViewById(R.id.txtFaturamentoTotal);
        txtComparacaoContexto = root.findViewById(R.id.txtComparacaoContexto);
        txtHeroResumoSecundario = root.findViewById(R.id.txtHeroResumoSecundario);

        txtKpiKms = root.findViewById(R.id.txtKpiKms);
        txtKpiReaisPorKm = root.findViewById(R.id.txtKpiReaisPorKm);
        txtKpiHorasEspera = root.findViewById(R.id.txtKpiHorasEspera);
        txtKpiValorEspera = root.findViewById(R.id.txtKpiValorEspera);
        txtKpiMediaDiaria = root.findViewById(R.id.txtKpiMediaDiaria);
        txtKpiDiasTrabalhados = root.findViewById(R.id.txtKpiDiasTrabalhados);
        txtKpiTotalServicos = root.findViewById(R.id.txtKpiTotalServicos);
        txtKpiQtdServicos = root.findViewById(R.id.txtKpiQtdServicos);
        txtKpiMediaServico = root.findViewById(R.id.txtKpiMediaServico);
        txtKpiViagensComServico = root.findViewById(R.id.txtKpiViagensComServico);

        txtComposicaoKm = root.findViewById(R.id.txtComposicaoKm);
        progressComposicaoKm = root.findViewById(R.id.progressComposicaoKm);
        txtComposicaoEspera = root.findViewById(R.id.txtComposicaoEspera);
        progressComposicaoEspera = root.findViewById(R.id.progressComposicaoEspera);
        txtComposicaoServico = root.findViewById(R.id.txtComposicaoServico);
        progressComposicaoServico = root.findViewById(R.id.progressComposicaoServico);

        txtEvolucaoMedia = root.findViewById(R.id.txtEvolucaoMedia);
        evolucaoChart = root.findViewById(R.id.evolucaoChart);

        recyclerClassificacao = root.findViewById(R.id.recyclerClassificacao);
        recyclerClassificacao.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerClassificacao.setNestedScrollingEnabled(false);
        classificacaoAdapter = new ClassificacaoResumoAdapter();
        recyclerClassificacao.setAdapter(classificacaoAdapter);

        txtDashboardErro = root.findViewById(R.id.txtDashboardErro);

        Button btnRegistrarViagemEmpty = root.findViewById(R.id.btnRegistrarViagemEmpty);
        btnRegistrarViagemEmpty.setOnClickListener(v -> {
            if (new ConfiguracaoDAO(requireContext()).configExiste()) {
                startActivity(new Intent(requireContext(), SolicitacaoActivity.class));
            } else {
                Toast.makeText(requireContext(), R.string.configure_app_primeiro, Toast.LENGTH_LONG).show();
                startActivity(new Intent(requireContext(), ConfiguracaoActivity.class));
            }
        });

        Button btnTentarNovamente = root.findViewById(R.id.btnTentarNovamente);
        btnTentarNovamente.setOnClickListener(v -> {
            if (controller != null) {
                controller.recarregarAtual(dashboardCallback);
            }
        });
    }

    private void configurarListeners() {
        chipGroupAnos.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (atualizandoChips || checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip == null) {
                return;
            }
            String ano = (String) chip.getTag();
            String mesAtual = dadosAtuais != null ? dadosAtuais.getMesSelecionado() : null;
            controller.selecionarPeriodo(ano, mesAtual, dashboardCallback);
        });

        chipGroupMeses.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (atualizandoChips || checkedIds.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip == null) {
                return;
            }
            String mes = (String) chip.getTag();
            String anoAtual = dadosAtuais != null ? dadosAtuais.getAnoSelecionado() : null;
            controller.selecionarPeriodo(anoAtual, mes, dashboardCallback);
        });

        evolucaoChart.setOnMesSelecionadoListener(mesNumero -> {
            if (dadosAtuais == null || mesNumero == null) {
                return;
            }
            if (!dadosAtuais.getMesesDisponiveis().contains(mesNumero)) {
                return;
            }
            String novoMes = Objects.equals(mesNumero, dadosAtuais.getMesSelecionado())
                    ? null
                    : mesNumero;
            controller.selecionarPeriodo(dadosAtuais.getAnoSelecionado(), novoMes, dashboardCallback);
        });
    }

    private void mostrarEstado(EstadoTela estado) {
        if (dashboardContent == null) {
            return;
        }
        dashboardContent.setVisibility(estado == EstadoTela.CONTEUDO ? View.VISIBLE : View.GONE);
        dashboardLoading.setVisibility(estado == EstadoTela.LOADING ? View.VISIBLE : View.GONE);
        dashboardEmpty.setVisibility(estado == EstadoTela.EMPTY ? View.VISIBLE : View.GONE);
        dashboardError.setVisibility(estado == EstadoTela.ERROR ? View.VISIBLE : View.GONE);
    }

    private void renderizarDados(DashboardDadosDTO dados) {
        renderizarChipsPeriodo(dados);

        DashboardResumoDTO resumo = dados.getResumo();
        if (resumo == null) {
            return;
        }

        txtPeriodoLabel.setText(
                FormatadorDashboard.formatarPeriodoTitulo(dados.getAnoSelecionado(), dados.getMesSelecionado()));
        txtFaturamentoTotal.setText(FormatadorDashboard.formatarMoeda(resumo.getFaturamentoTotal()));
        renderizarVariacao(resumo.getVariacaoPercentual(), dados.getMesSelecionado() != null);

        int totalViagens = resumo.getTotalViagens();
        String labelViagens = totalViagens == 1 ? "1 viagem" : totalViagens + " viagens";
        txtHeroResumoSecundario.setText(
                labelViagens + " \u2022 Ticket médio " + FormatadorDashboard.formatarMoeda(resumo.getTicketMedio()));

        txtKpiKms.setText(FormatadorDashboard.formatarKms(resumo.getTotalKms()));
        txtKpiReaisPorKm.setText(FormatadorDashboard.formatarReaisPorKm(resumo.getMediaReaisPorKm()));
        txtKpiHorasEspera.setText(FormatadorDashboard.formatarDuracaoEspera(resumo.getTotalHorasEspera()));
        txtKpiValorEspera.setText(FormatadorDashboard.formatarMoeda(resumo.getTotalValorEspera()));
        txtKpiMediaDiaria.setText(FormatadorDashboard.formatarMoeda(resumo.getMediaDiaria()));

        int dias = resumo.getDiasTrabalhados();
        String labelDias = dias == 1 ? "1 dia trabalhado" : dias + " dias trabalhados";
        txtKpiDiasTrabalhados.setText(labelDias);

        txtKpiTotalServicos.setText(FormatadorDashboard.formatarMoeda(resumo.getTotalValorServico()));
        int totalServicos = resumo.getTotalServicos();
        String labelQtdServicos = totalServicos == 1 ? "1 serviço" : totalServicos + " serviços";
        txtKpiQtdServicos.setText(labelQtdServicos);

        txtKpiMediaServico.setText(FormatadorDashboard.formatarMoeda(resumo.getMediaPorServico()));
        int viagensComServico = resumo.getViagensComServico();
        String labelViagensServico;
        if (viagensComServico <= 0) {
            labelViagensServico = "nenhuma viagem";
        } else if (viagensComServico == 1) {
            labelViagensServico = "em 1 viagem";
        } else {
            labelViagensServico = "em " + viagensComServico + " viagens";
        }
        txtKpiViagensComServico.setText(labelViagensServico);

        renderizarComposicao(resumo);

        evolucaoChart.setDados(dados.getEvolucaoAnual(), dados.getMesSelecionado());
        txtEvolucaoMedia.setText(
                "Média: " + FormatadorDashboard.formatarMoeda(evolucaoChart.getMediaMensalCalculada()));

        classificacaoAdapter.submitList(dados.getRankingClientes());
    }

    private void renderizarChipsPeriodo(DashboardDadosDTO dados) {
        atualizandoChips = true;
        try {
            chipGroupAnos.removeAllViews();
            List<String> anos = dados.getAnosDisponiveis();
            for (String ano : anos) {
                Chip chip = criarChipPeriodo(ano, ano);
                chipGroupAnos.addView(chip);
                if (Objects.equals(ano, dados.getAnoSelecionado())) {
                    chip.setChecked(true);
                }
            }

            chipGroupMeses.removeAllViews();
            Chip chipAnoCompleto = criarChipPeriodo(getString(R.string.dashboard_ano_completo), null);
            chipGroupMeses.addView(chipAnoCompleto);
            if (dados.getMesSelecionado() == null) {
                chipAnoCompleto.setChecked(true);
            }

            for (String mes : dados.getMesesDisponiveis()) {
                String sigla = FormatadorDashboard.getSiglaMes(mes);
                Chip chipMes = criarChipPeriodo(sigla.isEmpty() ? mes : sigla, mes);
                chipGroupMeses.addView(chipMes);
                if (Objects.equals(mes, dados.getMesSelecionado())) {
                    chipMes.setChecked(true);
                }
            }
        } finally {
            atualizandoChips = false;
        }
    }

    private Chip criarChipPeriodo(String texto, @Nullable String tag) {
        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setText(texto);
        chip.setTag(tag);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCheckedIconVisible(false);
        float density = getResources().getDisplayMetrics().density;
        chip.setChipMinHeight(36f * density);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
        chip.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_selector));
        chip.setChipStrokeColorResource(R.color.chip_stroke_selector);
        chip.setRippleColorResource(R.color.brand_green_light);
        chip.setChipStrokeWidth(1f * density);
        return chip;
    }

    private void renderizarVariacao(@Nullable Double variacao, boolean isMensal) {
        int bgColorRes;
        int fgColorRes;
        int iconRes;

        if (variacao == null) {
            bgColorRes = R.color.delta_neutral_bg;
            fgColorRes = R.color.delta_neutral;
            iconRes = R.drawable.ic_trending_flat;
            txtVariacaoPercentual.setText("--");
            txtComparacaoContexto.setText(R.string.dashboard_sem_historico_anterior);
        } else {
            if (variacao > 0.05) {
                bgColorRes = R.color.delta_positive_bg;
                fgColorRes = R.color.delta_positive;
                iconRes = R.drawable.ic_trending_up;
            } else if (variacao < -0.05) {
                bgColorRes = R.color.delta_negative_bg;
                fgColorRes = R.color.delta_negative;
                iconRes = R.drawable.ic_trending_down;
            } else {
                bgColorRes = R.color.delta_neutral_bg;
                fgColorRes = R.color.delta_neutral;
                iconRes = R.drawable.ic_trending_flat;
            }
            txtVariacaoPercentual.setText(FormatadorDashboard.formatarVariacaoPercentual(variacao));
            txtComparacaoContexto.setText(
                    isMensal ? R.string.dashboard_vs_mes_anterior : R.string.dashboard_vs_ano_anterior);
        }

        int bgColor = ContextCompat.getColor(requireContext(), bgColorRes);
        int fgColor = ContextCompat.getColor(requireContext(), fgColorRes);
        if (badgeVariacao.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) badgeVariacao.getBackground().mutate()).setColor(bgColor);
        } else {
            badgeVariacao.setBackgroundColor(bgColor);
        }
        imgVariacaoIcon.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(imgVariacaoIcon, ColorStateList.valueOf(fgColor));
        txtVariacaoPercentual.setTextColor(fgColor);
    }

    private void renderizarComposicao(DashboardResumoDTO resumo) {
        double valorKm = Math.max(0.0, resumo.getTotalValorKm());
        double valorEspera = Math.max(0.0, resumo.getTotalValorEspera());
        double valorServico = Math.max(0.0, resumo.getTotalValorServico());
        double soma = valorKm + valorEspera + valorServico;
        double base = soma > 0.0 ? soma : Math.max(0.0, resumo.getFaturamentoTotal());

        atualizarLinhaComposicao(txtComposicaoKm, progressComposicaoKm, valorKm, base);
        atualizarLinhaComposicao(txtComposicaoEspera, progressComposicaoEspera, valorEspera, base);
        atualizarLinhaComposicao(txtComposicaoServico, progressComposicaoServico, valorServico, base);
    }

    private void atualizarLinhaComposicao(
            TextView textoView,
            LinearProgressIndicator progressBar,
            double valor,
            double base
    ) {
        double percentual = base > 0.0 ? (valor / base) * 100.0 : 0.0;
        textoView.setText(
                FormatadorDashboard.formatarMoeda(valor)
                        + " (" + FormatadorDashboard.formatarPercentual(percentual) + ")");
        progressBar.setProgress((int) Math.round(Math.max(0.0, Math.min(100.0, percentual))));
    }
}
