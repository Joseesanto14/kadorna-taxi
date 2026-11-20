package com.digitalmuniz.kadornataxi.view.dialogs;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.model.entities.Custo;
import com.digitalmuniz.kadornataxi.util.FormatadorDashboard;
import com.google.android.material.chip.Chip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class CustoFormDialog {

    public interface OnSalvarListener {
        void onSalvar(Custo custo);
    }

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final float LARGURA_PERCENTUAL_TELA = 0.92f;
    private static final int MAX_DIGITOS_VALOR = 11;
    private static final DateTimeFormatter FORMATTER_BR_STRICT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private CustoFormDialog() {
    }

    @Nullable
    public static Dialog mostrar(
            @Nullable Context context,
            @Nullable Custo custoExistente,
            @Nullable String dataPadraoIso,
            @Nullable OnSalvarListener listener
    ) {
        if (context == null || !isContextoValido(context)) {
            return null;
        }

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_form_custo);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        configurarJanela(dialog, context);
        configurarConteudo(dialog, context, custoExistente, dataPadraoIso, listener);

        dialog.show();
        return dialog;
    }

    private static void configurarJanela(@NonNull Dialog dialog, @NonNull Context context) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int largura = (metrics != null && metrics.widthPixels > 0)
                    ? (int) (metrics.widthPixels * LARGURA_PERCENTUAL_TELA)
                    : ViewGroup.LayoutParams.MATCH_PARENT;
            window.setLayout(largura, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static void configurarConteudo(
            @NonNull Dialog dialog,
            @NonNull Context context,
            @Nullable Custo custoExistente,
            @Nullable String dataPadraoIso,
            @Nullable OnSalvarListener listener
    ) {
        TextView txtTitulo = dialog.findViewById(R.id.txtTituloModalCusto);
        ImageButton btnFechar = dialog.findViewById(R.id.btnFecharModalCusto);
        EditText edDescricao = dialog.findViewById(R.id.edDescricaoCusto);
        EditText edValor = dialog.findViewById(R.id.edValorCusto);
        EditText edData = dialog.findViewById(R.id.edDataCusto);
        ImageButton btnSelecionarData = dialog.findViewById(R.id.btnSelecionarDataCusto);
        AppCompatButton btnCancelar = dialog.findViewById(R.id.btnCancelarCusto);
        AppCompatButton btnSalvar = dialog.findViewById(R.id.btnSalvarCusto);

        configurarChipsSugestao(dialog, edDescricao);
        aplicarMascaraCentavos(edValor);
        aplicarMascaraData(edData);

        if (custoExistente != null) {
            txtTitulo.setText(R.string.custo_modal_titulo_editar);
            edDescricao.setText(custoExistente.getDescricao());
            edValor.setText(formatarValorEdicao(custoExistente.getValor()));
            String dataBr = FormatadorDashboard.formatarDataBR(custoExistente.getData());
            edData.setText(dataBr.isEmpty() ? LocalDate.now().format(FORMATTER_BR_STRICT) : dataBr);
        } else {
            txtTitulo.setText(R.string.custo_modal_titulo_novo);
            String dataInicial = resolverDataInicialBR(dataPadraoIso);
            edData.setText(dataInicial);
        }

        btnFechar.setOnClickListener(v -> dialog.dismiss());
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnSelecionarData.setOnClickListener(v -> {
            LocalDate base = parseDataStrict(edData.getText().toString().trim());
            if (base == null) {
                base = LocalDate.now();
            }
            DatePickerDialog picker = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        LocalDate selecionada = LocalDate.of(year, month + 1, dayOfMonth);
                        edData.setText(selecionada.format(FORMATTER_BR_STRICT));
                        edData.setError(null);
                    },
                    base.getYear(),
                    base.getMonthValue() - 1,
                    base.getDayOfMonth()
            );
            picker.show();
        });

        btnSalvar.setOnClickListener(v -> {
            String descricao = edDescricao.getText().toString().trim();
            if (descricao.isEmpty()) {
                edDescricao.setError(context.getString(R.string.custo_erro_descricao));
                edDescricao.requestFocus();
                return;
            }

            double valor = parseValorEmCentavos(edValor.getText().toString());
            if (valor <= 0.0) {
                edValor.setError(context.getString(R.string.custo_erro_valor));
                edValor.requestFocus();
                return;
            }

            LocalDate dataValida = parseDataStrict(edData.getText().toString().trim());
            if (dataValida == null) {
                edData.setError(context.getString(R.string.custo_erro_data));
                edData.requestFocus();
                return;
            }

            long id = custoExistente != null ? custoExistente.getId() : 0L;
            String dataIso = dataValida.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Custo resultado = new Custo(id, descricao, valor, dataIso);

            dialog.dismiss();
            if (listener != null) {
                listener.onSalvar(resultado);
            }
        });
    }

    private static void configurarChipsSugestao(@NonNull Dialog dialog, @NonNull EditText edDescricao) {
        int[] chipIds = {
                R.id.chipSugestaoCombustivel,
                R.id.chipSugestaoManutencao,
                R.id.chipSugestaoPedagio,
                R.id.chipSugestaoLavagem,
                R.id.chipSugestaoSeguro
        };
        for (int chipId : chipIds) {
            Chip chip = dialog.findViewById(chipId);
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    String texto = chip.getText() != null ? chip.getText().toString() : "";
                    edDescricao.setText(texto);
                    edDescricao.setSelection(texto.length());
                    edDescricao.setError(null);
                });
            }
        }
    }

    private static void aplicarMascaraCentavos(@NonNull EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean atualizando;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (atualizando) {
                    return;
                }
                atualizando = true;
                try {
                    String apenasDigitos = s.toString().replaceAll("\\D", "");
                    if (apenasDigitos.isEmpty()) {
                        s.replace(0, s.length(), "");
                        return;
                    }
                    if (apenasDigitos.length() > MAX_DIGITOS_VALOR) {
                        apenasDigitos = apenasDigitos.substring(0, MAX_DIGITOS_VALOR);
                    }
                    BigDecimal valor = new BigDecimal(apenasDigitos)
                            .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                    String formatado = formatarValorDecimal(valor.doubleValue());
                    s.replace(0, s.length(), formatado);
                    editText.setSelection(Math.min(formatado.length(), s.length()));
                } finally {
                    atualizando = false;
                }
            }
        });
    }

    private static void aplicarMascaraData(@NonNull EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean atualizando;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (atualizando) {
                    return;
                }
                atualizando = true;
                try {
                    String digitos = s.toString().replaceAll("\\D", "");
                    if (digitos.length() > 8) {
                        digitos = digitos.substring(0, 8);
                    }
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < digitos.length(); i++) {
                        if (i == 2 || i == 4) {
                            sb.append('/');
                        }
                        sb.append(digitos.charAt(i));
                    }
                    String formatado = sb.toString();
                    if (!formatado.equals(s.toString())) {
                        s.replace(0, s.length(), formatado);
                        editText.setSelection(Math.min(formatado.length(), s.length()));
                    }
                } finally {
                    atualizando = false;
                }
            }
        });
    }

    private static String formatarValorEdicao(double valor) {
        long centavos = Math.round(Math.max(0.0, valor) * 100.0);
        if (centavos == 0L) {
            return "";
        }
        return formatarValorDecimal(centavos / 100.0);
    }

    private static String formatarValorDecimal(double valor) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(PT_BR);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(valor);
    }

    public static double parseValorEmCentavos(@Nullable String texto) {
        if (texto == null) {
            return 0.0;
        }
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return 0.0;
        }
        if (digitos.length() > MAX_DIGITOS_VALOR) {
            digitos = digitos.substring(0, MAX_DIGITOS_VALOR);
        }
        try {
            return new BigDecimal(digitos)
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Nullable
    public static LocalDate parseDataStrict(@Nullable String dataBr) {
        if (dataBr == null || dataBr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dataBr.trim(), FORMATTER_BR_STRICT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String resolverDataInicialBR(@Nullable String dataPadraoIso) {
        if (dataPadraoIso != null && !dataPadraoIso.trim().isEmpty()) {
            try {
                LocalDate parsed = LocalDate.parse(dataPadraoIso.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                return parsed.format(FORMATTER_BR_STRICT);
            } catch (DateTimeParseException ignored) {
            }
        }
        return LocalDate.now().format(FORMATTER_BR_STRICT);
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
