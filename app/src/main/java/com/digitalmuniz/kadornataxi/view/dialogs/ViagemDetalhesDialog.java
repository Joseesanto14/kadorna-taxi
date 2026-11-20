package com.digitalmuniz.kadornataxi.view.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.model.entities.Servico;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.util.FormatadorDashboard;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public final class ViagemDetalhesDialog {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final float LARGURA_PERCENTUAL_TELA = 0.92f;
    private static final float ALTURA_MAXIMA_PERCENTUAL_TELA = 0.88f;

    private ViagemDetalhesDialog() {
    }

    @Nullable
    public static Dialog mostrar(@Nullable Context context, @Nullable Viagem viagem) {
        if (context == null || viagem == null || !isContextoValido(context)) {
            return null;
        }

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_detalhes_viagem);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        configurarJanela(dialog, context);
        preencherConteudo(dialog, context, viagem);

        dialog.show();
        return dialog;
    }

    private static void configurarJanela(@NonNull Dialog dialog, @NonNull Context context) {
        Window window = dialog.getWindow();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        ConstraintLayout layoutRaiz = dialog.findViewById(R.id.layoutRaizDetalhes);
        if (layoutRaiz != null && metrics != null && metrics.heightPixels > 0) {
            layoutRaiz.setMaxHeight((int) (metrics.heightPixels * ALTURA_MAXIMA_PERCENTUAL_TELA));
        }

        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int largura = (metrics != null && metrics.widthPixels > 0)
                    ? (int) (metrics.widthPixels * LARGURA_PERCENTUAL_TELA)
                    : ViewGroup.LayoutParams.MATCH_PARENT;
            window.setLayout(largura, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static void preencherConteudo(
            @NonNull Dialog dialog,
            @NonNull Context context,
            @NonNull Viagem viagem
    ) {
        ImageButton btnFecharTopo = dialog.findViewById(R.id.btnFecharTopo);
        AppCompatButton btnFecharDetalhes = dialog.findViewById(R.id.btnFecharDetalhes);
        btnFecharTopo.setOnClickListener(v -> dialog.dismiss());
        btnFecharDetalhes.setOnClickListener(v -> dialog.dismiss());

        TextView txtDataHora = dialog.findViewById(R.id.txtDetalheDataHora);
        TextView txtBadgeClassificacao = dialog.findViewById(R.id.txtBadgeClassificacao);
        txtDataHora.setText(formatarDataHora(viagem));
        configurarBadgeClassificacao(txtBadgeClassificacao, context, viagem.getClassificacao());

        TextView txtOrigem = dialog.findViewById(R.id.txtDetalheOrigem);
        TextView txtDestino = dialog.findViewById(R.id.txtDetalheDestino);
        txtOrigem.setText(textoOuPadrao(viagem.getOrigem(), "-"));
        txtDestino.setText(textoOuPadrao(viagem.getDestino(), "-"));

        LinearLayout layoutSolicitante = dialog.findViewById(R.id.layoutDetalheSolicitante);
        TextView txtSolicitante = dialog.findViewById(R.id.txtDetalheSolicitante);
        TextView txtDescricao = dialog.findViewById(R.id.txtDetalheDescricao);
        TextView txtMotorista = dialog.findViewById(R.id.txtDetalheMotorista);

        String solicitante = textoLimpo(viagem.getSolicitante());
        if (!solicitante.isEmpty()) {
            txtSolicitante.setText(solicitante);
            layoutSolicitante.setVisibility(View.VISIBLE);
        } else {
            layoutSolicitante.setVisibility(View.GONE);
        }
        txtDescricao.setText(textoOuPadrao(viagem.getDescricao(), "-"));
        txtMotorista.setText(textoOuPadrao(viagem.getMotorista(), "-"));

        TextView txtKmQtd = dialog.findViewById(R.id.txtDetalheKmQtd);
        TextView txtKmValor = dialog.findViewById(R.id.txtDetalheKmValor);
        TextView txtEsperaQtd = dialog.findViewById(R.id.txtDetalheEsperaQtd);
        TextView txtEsperaValor = dialog.findViewById(R.id.txtDetalheEsperaValor);

        txtKmQtd.setText(FormatadorDashboard.formatarKms(valorSeguro(viagem.getKmsRodados())));
        txtKmValor.setText(FormatadorDashboard.formatarMoeda(valorSeguro(viagem.getValorKms())));
        txtEsperaQtd.setText(formatarHorasEspera(valorSeguro(viagem.getHoraEspera())));
        txtEsperaValor.setText(FormatadorDashboard.formatarMoeda(valorSeguro(viagem.getValorHoraEspera())));

        preencherServicos(dialog, context, viagem);

        TextView txtValorTotal = dialog.findViewById(R.id.txtDetalheValorTotal);
        txtValorTotal.setText(FormatadorDashboard.formatarMoeda(valorSeguro(viagem.getValorTotal())));
    }

    private static void preencherServicos(
            @NonNull Dialog dialog,
            @NonNull Context context,
            @NonNull Viagem viagem
    ) {
        TextView txtTituloServicos = dialog.findViewById(R.id.txtTituloServicos);
        LinearLayout containerServicos = dialog.findViewById(R.id.containerServicosDetalhe);
        TextView txtServicosVazio = dialog.findViewById(R.id.txtServicosVazio);
        LinearLayout layoutSubtotal = dialog.findViewById(R.id.layoutSubtotalServicos);
        TextView txtSubtotal = dialog.findViewById(R.id.txtSubtotalServicos);

        containerServicos.removeAllViews();

        List<Servico> servicos = viagem.getServicos();
        float valorTotalServicos = valorSeguro(viagem.getValorTotalServicos());
        String tituloBase = context.getString(R.string.detalhes_secao_servicos);

        if (servicos != null && !servicos.isEmpty()) {
            txtTituloServicos.setText(String.format(PT_BR, "%s (%d)", tituloBase, servicos.size()));
            txtServicosVazio.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(context);

            int total = servicos.size();
            for (int i = 0; i < total; i++) {
                Servico servico = servicos.get(i);
                String nome = servico != null
                        ? textoOuPadrao(servico.getDescricao(), context.getString(R.string.detalhes_servico_adicional_legado))
                        : context.getString(R.string.detalhes_servico_adicional_legado);
                float valor = servico != null ? valorSeguro(servico.getValor()) : 0f;
                adicionarLinhaServico(inflater, containerServicos, nome, valor, i < total - 1);
            }

            layoutSubtotal.setVisibility(View.VISIBLE);
            txtSubtotal.setText(FormatadorDashboard.formatarMoeda(valorTotalServicos));
        } else if (valorTotalServicos > 0f) {
            txtTituloServicos.setText(String.format(PT_BR, "%s (1)", tituloBase));
            txtServicosVazio.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(context);
            adicionarLinhaServico(
                    inflater,
                    containerServicos,
                    context.getString(R.string.detalhes_servico_adicional_legado),
                    valorTotalServicos,
                    false
            );
            layoutSubtotal.setVisibility(View.VISIBLE);
            txtSubtotal.setText(FormatadorDashboard.formatarMoeda(valorTotalServicos));
        } else {
            txtTituloServicos.setText(tituloBase);
            txtServicosVazio.setVisibility(View.VISIBLE);
            layoutSubtotal.setVisibility(View.GONE);
        }
    }

    private static void adicionarLinhaServico(
            @NonNull LayoutInflater inflater,
            @NonNull LinearLayout container,
            @NonNull String nome,
            float valor,
            boolean exibirDivisor
    ) {
        View linha = inflater.inflate(R.layout.item_detalhe_servico, container, false);
        TextView txtNome = linha.findViewById(R.id.txtNomeServico);
        TextView txtValor = linha.findViewById(R.id.txtValorServico);
        View divider = linha.findViewById(R.id.dividerServico);

        txtNome.setText(nome);
        txtValor.setText(FormatadorDashboard.formatarMoeda(valor));
        divider.setVisibility(exibirDivisor ? View.VISIBLE : View.GONE);
        container.addView(linha);
    }

    private static void configurarBadgeClassificacao(
            @NonNull TextView badge,
            @NonNull Context context,
            @Nullable String classificacao
    ) {
        String limpa = textoLimpo(classificacao);
        String comum = context.getString(R.string.detalhes_classificacao_comum);
        boolean isEmpresa = !limpa.isEmpty() && !comum.equalsIgnoreCase(limpa);

        badge.setText(limpa.isEmpty() ? comum : limpa);
        if (isEmpresa) {
            badge.setBackgroundResource(R.drawable.bg_pill_green_light);
            badge.setTextColor(ContextCompat.getColor(context, R.color.brand_green_dark));
        } else {
            badge.setBackgroundResource(R.drawable.bg_pill_gray);
            badge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }
    }

    @NonNull
    private static String formatarDataHora(@NonNull Viagem viagem) {
        String diaMes = textoLimpo(viagem.getDiaMes());
        String hora = textoLimpo(viagem.getHora());
        if (!diaMes.isEmpty() && !hora.isEmpty()) {
            return diaMes + " \u2022 " + hora;
        }
        if (!diaMes.isEmpty()) {
            return diaMes;
        }
        if (!hora.isEmpty()) {
            return hora;
        }
        return "-";
    }

    @NonNull
    private static String formatarHorasEspera(float horas) {
        double sane = Math.max(0.0, horas);
        if (Math.abs(sane) < 0.005) {
            sane = 0.0;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(PT_BR);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(sane) + " h";
    }

    private static float valorSeguro(float valor) {
        if (Float.isNaN(valor) || Float.isInfinite(valor)) {
            return 0f;
        }
        return valor;
    }

    @NonNull
    private static String textoLimpo(@Nullable String texto) {
        return texto != null ? texto.trim() : "";
    }

    @NonNull
    private static String textoOuPadrao(@Nullable String texto, @NonNull String padrao) {
        String limpo = textoLimpo(texto);
        return limpo.isEmpty() ? padrao : limpo;
    }

    private static boolean isContextoValido(@NonNull Context context) {
        Context atual = context;
        while (atual instanceof ContextWrapper) {
            if (atual instanceof Activity) {
                Activity activity = (Activity) atual;
                return !activity.isFinishing() && !activity.isDestroyed();
            }
            atual = ((ContextWrapper) atual).getBaseContext();
        }
        return true;
    }
}
