package com.digitalmuniz.kadornataxi.view.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.model.entities.Servico;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public class SolicitacaoActivity extends AppCompatActivity {

    public static final String EXTRA_VIAGEM = "extra_viagem";

    EditText edOrigem, edData, edHora, edDestino, edDescricao, edSolicitante, edKmsRodados, edMotorista, edHoraEspera, edCliente;
    EditText edValorKm, edValorHoraEspera, edValorTotal;
    LinearLayout containerServicos;
    ImageButton btnAdicionarServico;
    AppCompatButton btGerarViagem;

    private long viagemEditandoId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_solicitacao);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewBinding();
        configurarListeners();

        Viagem viagemParaEditar = (Viagem) getIntent().getSerializableExtra(EXTRA_VIAGEM);
        if (viagemParaEditar != null) {
            viagemEditandoId = viagemParaEditar.getId();
            preencherCamposEdicao(viagemParaEditar);
            btGerarViagem.setText(getString(R.string.editar_viagem));
        } else {
            preencherCampos();
            adicionarLinhaServico("", 0f, false);
        }
    }

    // --------------- Sessão UI ---------------
    public void voltarMenu(View view) {
        finish();
    }

    private void viewBinding() {
        edOrigem = findViewById(R.id.edOrigem);
        edData = findViewById(R.id.edData);
        edHora = findViewById(R.id.edHora);
        edDestino = findViewById(R.id.edDestino);
        edDescricao = findViewById(R.id.edDescricao);
        edSolicitante = findViewById(R.id.edSolicitante);
        edKmsRodados = findViewById(R.id.edKmRodados);
        edValorKm = findViewById(R.id.edValorKm);
        edHoraEspera = findViewById(R.id.edHoraEspera);
        edValorHoraEspera = findViewById(R.id.edValorHoraEspera);
        edMotorista = findViewById(R.id.edMotorista);
        edCliente = findViewById(R.id.edSeparadoCliente);
        edValorTotal = findViewById(R.id.edValorTotal);
        containerServicos = findViewById(R.id.containerServicos);
        btnAdicionarServico = findViewById(R.id.btnAdicionarServico);
        btGerarViagem = findViewById(R.id.btGerarViagem);
    }

    private void preencherCampos() {
        String[] dataHora = receberDataHoraDoSistema();
        edData.setText(dataHora[0]);
        edHora.setText(dataHora[1]);
        edMotorista.setText(new ConfiguracaoDAO(this).getConfiguracao().getMotorista());
        edCliente.setText(new ConfiguracaoDAO(this).getConfiguracao().getClassificacaoViagemSeparada());
    }

    private void preencherCamposEdicao(Viagem viagem) {
        edOrigem.setText(viagem.getOrigem());
        edDestino.setText(viagem.getDestino());
        edDescricao.setText(viagem.getDescricao());
        edSolicitante.setText(viagem.getSolicitante());
        edMotorista.setText(viagem.getMotorista());
        edCliente.setText(viagem.getClassificacao());

        // Convert ISO date (yyyy-MM-dd) to ddMMyyyy so the TextWatcher formats it as dd/MM/yyyy
        String dataViagem = viagem.getData() != null ? viagem.getData() : "";
        String[] partes = dataViagem.split("-");
        if (partes.length == 3) {
            edData.setText(partes[2] + partes[1] + partes[0]);
        } else {
            edData.setText(dataViagem);
        }

        edHora.setText(viagem.getHora());

        if (viagem.getKmsRodados() > 0) {
            edKmsRodados.setText(String.format(Locale.getDefault(), "%.2f", viagem.getKmsRodados()));
        }

        if (viagem.getHoraEspera() > 0) {
            int horas = (int) viagem.getHoraEspera();
            int minutos = Math.round((viagem.getHoraEspera() - horas) * 60);
            edHoraEspera.setText(String.format(Locale.getDefault(), "%02d%02d", horas, minutos));
        }

        containerServicos.removeAllViews();
        List<Servico> servicos = viagem.getServicos();
        if (!servicos.isEmpty()) {
            for (Servico servico : servicos) {
                adicionarLinhaServico(servico.getDescricao(), servico.getValor(), false);
            }
        } else if (viagem.getValorServico() > 0f) {
            adicionarLinhaServico("Serviço adicional", viagem.getValorServico(), false);
        } else {
            adicionarLinhaServico("", 0f, false);
        }
        atualizarValorTotal();
    }

    private void adicionarLinhaServico(String descricao, float valor, boolean focarDescricao) {
        View linha = LayoutInflater.from(this).inflate(R.layout.item_servico_input, containerServicos, false);
        EditText edServicoDescricao = linha.findViewById(R.id.edServicoDescricao);
        EditText edServicoValor = linha.findViewById(R.id.edServicoValor);
        ImageButton btnRemoverServico = linha.findViewById(R.id.btnRemoverServico);

        aplicarMascaraCentavos(edServicoValor);

        if (descricao != null && !descricao.isEmpty()) {
            edServicoDescricao.setText(descricao);
        }
        if (valor > 0f) {
            long centavos = Math.round(valor * 100);
            edServicoValor.setText(String.valueOf(centavos));
        }

        btnRemoverServico.setOnClickListener(v -> {
            if (containerServicos.getChildCount() > 1) {
                containerServicos.removeView(linha);
            } else {
                edServicoDescricao.setText("");
                edServicoDescricao.setError(null);
                edServicoValor.setText("");
                edServicoValor.setError(null);
            }
            atualizarValorTotal();
        });

        containerServicos.addView(linha);
        if (focarDescricao) {
            edServicoDescricao.requestFocus();
        }
    }

    private String[] receberDataHoraDoSistema() {
        LocalDate data = LocalDateTime.now().toLocalDate();
        LocalTime hora = LocalDateTime.now().toLocalTime();

        String horaStr = hora.toString().replaceAll(":", "").substring(0, 4);
        String[] partesData = data.toString().split("-");
        String dataStr = partesData[2] + partesData[1] + partesData[0];

        return new String[]{dataStr, horaStr};
    }

    private boolean todosCamposPreenchidos() {
        if (!validarLinhasServicos()) {
            return false;
        }
        if (edOrigem.getText().toString().isEmpty() ||
                edData.getText().toString().isEmpty() ||
                edHora.getText().toString().isEmpty() ||
                edDestino.getText().toString().isEmpty() ||
                edDescricao.getText().toString().isEmpty() ||
                edMotorista.getText().toString().isEmpty() ||
                edCliente.getText().toString().isEmpty() ||
                !marcouValor()) {

            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validarLinhasServicos() {
        boolean valido = true;
        for (int i = 0; i < containerServicos.getChildCount(); i++) {
            View linha = containerServicos.getChildAt(i);
            EditText edDesc = linha.findViewById(R.id.edServicoDescricao);
            EditText edVal = linha.findViewById(R.id.edServicoValor);
            edDesc.setError(null);
            edVal.setError(null);

            String desc = edDesc.getText().toString().trim();
            float valor = parseValorMonetario(edVal.getText().toString());

            if (desc.isEmpty() && valor <= 0f) {
                continue;
            }
            if (valor > 0f && desc.isEmpty()) {
                edDesc.setError("Informe o serviço");
                if (valido) {
                    edDesc.requestFocus();
                }
                valido = false;
            } else if (!desc.isEmpty() && valor <= 0f) {
                edVal.setError("Informe o valor");
                if (valido) {
                    edVal.requestFocus();
                }
                valido = false;
            }
        }
        return valido;
    }

    private boolean marcouValor() {
        boolean marcouKm = !edKmsRodados.getText().toString().trim().isEmpty();
        boolean marcouServicoValido = false;
        for (Servico servico : coletarServicosValidos()) {
            if (servico.getValor() > 0f) {
                marcouServicoValido = true;
                break;
            }
        }
        return marcouKm || marcouServicoValido;
    }

    private List<Servico> coletarServicosValidos() {
        List<Servico> servicos = new ArrayList<>();
        for (int i = 0; i < containerServicos.getChildCount(); i++) {
            View linha = containerServicos.getChildAt(i);
            EditText edDesc = linha.findViewById(R.id.edServicoDescricao);
            EditText edVal = linha.findViewById(R.id.edServicoValor);

            String desc = edDesc.getText().toString().trim();
            float valor = parseValorMonetario(edVal.getText().toString());
            if (!desc.isEmpty() && valor > 0f) {
                servicos.add(new Servico(desc, valor));
            }
        }
        return servicos;
    }

    private float calcularSomaServicosInputs() {
        float soma = 0f;
        for (int i = 0; i < containerServicos.getChildCount(); i++) {
            View linha = containerServicos.getChildAt(i);
            EditText edVal = linha.findViewById(R.id.edServicoValor);
            soma += parseValorMonetario(edVal.getText().toString());
        }
        return soma;
    }

    private float parseValorMonetario(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    @NonNull
    private Viagem criarViagemObjeto() {
        float horaEspera = 0f;
        if (!edHoraEspera.getText().toString().isEmpty()) {
            float horas = Float.parseFloat(edHoraEspera.getText().toString().split(":")[0]);
            float minutos = Float.parseFloat(edHoraEspera.getText().toString().split(":")[1]);
            float minutosEmHoras = Math.round(minutos / 60.0f * 100.0f) / 100.0f;
            horaEspera = horas + minutosEmHoras;
        }

        List<Servico> servicos = coletarServicosValidos();
        float valorServico = 0f;
        for (Servico servico : servicos) {
            valorServico += servico.getValor();
        }

        float kmsRodados = 0f;
        if (!edKmsRodados.getText().toString().isEmpty()) {
            kmsRodados = Float.parseFloat(edKmsRodados.getText().toString().replace(",", "."));
        }

        Viagem viagem = new Viagem(
                edOrigem.getText().toString().trim(),
                edData.getText().toString().trim(),
                edHora.getText().toString().trim(),
                edDestino.getText().toString().trim(),
                edDescricao.getText().toString().trim(),
                kmsRodados,
                horaEspera,
                edMotorista.getText().toString().trim(),
                edCliente.getText().toString().trim(),
                getApplicationContext(),
                valorServico,
                edSolicitante.getText().toString().trim()
        );
        viagem.setServicos(servicos);
        return viagem;
    }

    // --------------- Sessão Listeners ---------------

    private void configurarListeners() {
        listenerData();
        listenerHora();

        listenerKmsRodados();
        listenerHoraEspera();
        btnAdicionarServico.setOnClickListener(v -> adicionarLinhaServico("", 0f, true));
    }

    private void listenerData() {
        edData.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    return;
                }
                isUpdating = true;

                String clean = s.toString().replaceAll("[^\\d]", "");

                if (clean.isEmpty()) {
                    edData.setText("");
                    isUpdating = false;
                    return;
                }

                if (clean.length() > 8) {
                    clean = clean.substring(clean.length() - 8);
                } else {
                    clean = String.format(Locale.getDefault(), "%08d", Long.parseLong(clean));
                }

                String dia = clean.substring(0, 2);
                String mes = clean.substring(2, 4);
                String ano = clean.substring(4, 8);

                String formatado = dia + "/" + mes + "/" + ano;

                edData.setText(formatado);
                edData.setSelection(formatado.length());
                isUpdating = false;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void listenerHora() {
        edHora.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    return;
                }
                isUpdating = true;

                String clean = s.toString().replaceAll("[^\\d]", "");

                if (clean.isEmpty()) {
                    edHora.setText("");
                    isUpdating = false;
                    return;
                }

                if (clean.length() > 4) {
                    clean = clean.substring(clean.length() - 4);
                } else {
                    clean = String.format(Locale.getDefault(), "%04d", Long.parseLong(clean));
                }

                String horas = clean.substring(0, 2);
                String minutos = clean.substring(2, 4);

                String formatado = horas + ":" + minutos;

                edHora.setText(formatado);
                edHora.setSelection(formatado.length());

                isUpdating = false;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void listenerKmsRodados() {
        edKmsRodados.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                calcularValorKm(s.toString(), edValorKm);
                atualizarValorTotal();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void listenerHoraEspera() {
        edHoraEspera.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                isUpdating = true;

                String clean = s.toString().replaceAll("[^\\d]", "");

                if (clean.isEmpty()) {
                    edHoraEspera.setText("");
                    calcularValorEspera("", edValorHoraEspera);
                    atualizarValorTotal();
                    isUpdating = false;
                    return;
                }

                if (clean.length() > 4) {
                    clean = clean.substring(clean.length() - 4);
                } else {
                    clean = String.format(Locale.getDefault(), "%04d", Long.parseLong(clean));
                }

                String hora = clean.substring(0, 2);
                String minuto = clean.substring(2, 4);
                String formatado = hora + ":" + minuto;

                edHoraEspera.setText(formatado);
                edHoraEspera.setSelection(formatado.length());

                calcularValorEspera(clean, edValorHoraEspera);
                atualizarValorTotal();

                isUpdating = false;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void aplicarMascaraCentavos(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean estaAtualizando = false;

            @Override
            public void afterTextChanged(Editable s) {
                if (estaAtualizando) return;
                estaAtualizando = true;

                String valorLimpo = editText.getText().toString().replaceAll("[^\\d]", "");

                if (valorLimpo.isEmpty()) {
                    editText.setText("");
                    atualizarValorTotal();
                    estaAtualizando = false;
                    return;
                } else if (valorLimpo.length() < 3) {
                    valorLimpo = String.format(Locale.getDefault(), "%03d", Long.parseLong(valorLimpo));
                } else if (valorLimpo.length() > 12) {
                    valorLimpo = valorLimpo.substring(valorLimpo.length() - 12);
                }

                String real = String.valueOf(Long.parseLong(valorLimpo.substring(0, valorLimpo.length() - 2)));
                String centavos = valorLimpo.substring(valorLimpo.length() - 2);

                String formatado = real + "," + centavos;

                editText.setText(formatado);
                editText.setSelection(formatado.length());
                atualizarValorTotal();
                estaAtualizando = false;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    // --------------- Sessão Cálculos ---------------

    private void calcularValorKm(String kmStr, EditText output) {
        if (kmStr.isEmpty()) {
            output.setText("0,00");
            return;
        }
        try {
            float km = Float.parseFloat(kmStr.replace(",", "."));
            float total = km * (new ConfiguracaoDAO(this).getConfiguracao().getValorKmRodado());
            output.setText(String.format(Locale.getDefault(), "%.2f", total));
        } catch (NumberFormatException e) {
            output.setText("0,00");
        }
    }

    private void calcularValorEspera(String timeClean, EditText output) {
        if (timeClean.length() < 4) {
            output.setText("0,00");
            return;
        }
        try {
            float horas = Float.parseFloat(timeClean.substring(0, 2));
            float minutos = Float.parseFloat(timeClean.substring(2, 4));

            float minutosEmHoras = Math.round(minutos / 60.0f * 100.0f) / 100.0f;
            float tempoEmHoras = horas + minutosEmHoras;

            float total = tempoEmHoras * (new ConfiguracaoDAO(this).getConfiguracao().getValorHoraEspera());

            output.setText(String.format(Locale.getDefault(), "%.2f", total));
        } catch (Exception e) {
            output.setText("0,00");
        }
    }

    private void atualizarValorTotal() {
        try {
            float valorViagem = edValorKm.getText().toString().isEmpty() ? 0f
                    : Float.parseFloat(edValorKm.getText().toString().replace(",", "."));
            float valorEspera = edValorHoraEspera.getText().toString().isEmpty() ? 0f
                    : Float.parseFloat(edValorHoraEspera.getText().toString().replace(",", "."));
            float valorServico = calcularSomaServicosInputs();

            float valorTotal = valorViagem + valorEspera + valorServico;
            edValorTotal.setText(String.format(Locale.getDefault(), "%.2f", valorTotal));
        } catch (NumberFormatException e) {
            edValorTotal.setText("0,00");
        }
    }

    // --------------- Sessão Database ---------------

    public void salvarViagemDb(View view) {
        if (todosCamposPreenchidos()) {
            Viagem viagem = criarViagemObjeto();
            if (viagemEditandoId != -1) {
                viagem.setId(viagemEditandoId);
                new ViagemDAO(this).update(viagem);
                Toast.makeText(this, "Viagem atualizada com sucesso!", Toast.LENGTH_SHORT).show();
            } else {
                new ViagemDAO(this).inserirNoDatabase(viagem, getApplicationContext());
            }
            finish();
        }
    }
}
