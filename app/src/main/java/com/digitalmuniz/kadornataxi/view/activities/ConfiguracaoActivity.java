package com.digitalmuniz.kadornataxi.view.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.controller.BackupController;
import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfiguracaoActivity extends AppCompatActivity {
    private EditText edValorKmRodado, edValorHoraEsperada, edMotorista, edClassificacaoViagemSeparada;
    private EditText edTitularCnpj, edNomeFantasia, edCnpj, edTelefone, edEmail;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> seletorArquivoBackupLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    processarArquivoBackupSelecionado(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_configuracao);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewBinding();
        carregarConfigs();
    }

    //---------- UI ----------
    public void voltarMenu(View view) {
        finish();
    }

    private void carregarConfigs() {
        ConfiguracaoDAO dao = new ConfiguracaoDAO(this);

        if (dao.configExiste()) {
            Configuracao configuracao = dao.getConfiguracao();

            edValorKmRodado.setText(String.valueOf(configuracao.getValorKmRodado()));
            edValorHoraEsperada.setText(String.valueOf(configuracao.getValorHoraEspera()));
            edMotorista.setText(configuracao.getMotorista());
            edClassificacaoViagemSeparada.setText(configuracao.getClassificacaoViagemSeparada());
            edTitularCnpj.setText(configuracao.getTitularCNPJ());
            edNomeFantasia.setText(configuracao.getNomeFantasia());
            edCnpj.setText(configuracao.getCnpj());
            edTelefone.setText(configuracao.getTelefone());
            edEmail.setText(configuracao.getEmail());
        }
    }

    private void viewBinding() {
        edValorKmRodado = findViewById(R.id.edValorKmRodado);
        edValorHoraEsperada = findViewById(R.id.edValorHoraEsperada);
        edMotorista = findViewById(R.id.edMotorista);
        edClassificacaoViagemSeparada = findViewById(R.id.edClassificacaoViagemSeparada);
        edTitularCnpj = findViewById(R.id.edTitularCnpj);
        edNomeFantasia = findViewById(R.id.edNomeFantasia);
        edCnpj = findViewById(R.id.edCnpj);
        edTelefone = findViewById(R.id.edTelefone);
        edEmail = findViewById(R.id.edEmail);
    }

    //---------- Database ----------

    public void salvarConfiguracoes(View view) {
        if (todosCamposPreenchidos()) {
            if (edClassificacaoViagemSeparada.getText().toString().isEmpty())
                edClassificacaoViagemSeparada.setText("Comum");

            Configuracao configuracao = new Configuracao(
                    1,
                    Float.parseFloat(edValorKmRodado.getText().toString()),
                    Float.parseFloat(edValorHoraEsperada.getText().toString()),
                    edMotorista.getText().toString(),
                    edClassificacaoViagemSeparada.getText().toString(),
                    edCnpj.getText().toString(),
                    edNomeFantasia.getText().toString(),
                    edTitularCnpj.getText().toString(),
                    edTelefone.getText().toString(),
                    edEmail.getText().toString()
            );

            new ConfiguracaoDAO(getApplicationContext())
                    .configurar(configuracao, this);
            finish();
        } else {
            Toast.makeText(this, "Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show();
        }


    }

    //---------- Listeners ----------

    private void listenerValorKm() {
        edValorKmRodado.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
    }

    //---------- Lógica ----------

    private boolean todosCamposPreenchidos() {
        if (edValorKmRodado.getText().toString().isEmpty() &&
                edValorHoraEsperada.getText().toString().isEmpty() &&
                edMotorista.getText().toString().isEmpty()) {
            return false;
        } // verifica se o usuário preencheu os campos

        if ((edValorKmRodado.getText().toString().equals(".") &&
                edValorHoraEsperada.getText().toString().equals("."))
        ) {
            return false;
        } // verifica se o usuário preencheu os campos com pontos (inválido)

        // verifica se o usuário preencheu os campos com zeros
        return (Float.parseFloat(edValorKmRodado.getText().toString()) != 0f) ||
                (Float.parseFloat(edValorHoraEsperada.getText().toString()) != 0f);
    }

    //---------- Backup e Migração de Dados ----------

    public void exportarDados(View view) {
        Toast.makeText(this, "Preparando backup das viagens...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                Intent shareIntent = BackupController.gerarIntentExportacao(getApplicationContext());
                mainHandler.post(() -> {
                    try {
                        startActivity(shareIntent);
                    } catch (Exception e) {
                        Toast.makeText(ConfiguracaoActivity.this, "Erro ao abrir compartilhamento: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    new AlertDialog.Builder(ConfiguracaoActivity.this)
                            .setTitle("Erro na Exportação")
                            .setMessage("Não foi possível gerar o arquivo de backup: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }

    public void importarDados(View view) {
        seletorArquivoBackupLauncher.launch(new String[]{"application/json", "text/*", "*/*"});
    }

    private void processarArquivoBackupSelecionado(Uri uri) {
        Toast.makeText(this, "Analisando arquivo de backup...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                BackupController.PreviewBackup preview = BackupController.inspecionarBackup(getApplicationContext(), uri);
                mainHandler.post(() -> exibirDialogoConfirmacaoImportacao(uri, preview));
            } catch (Exception e) {
                mainHandler.post(() -> {
                    new AlertDialog.Builder(ConfiguracaoActivity.this)
                            .setTitle("Arquivo Inválido")
                            .setMessage("O arquivo selecionado não pôde ser lido: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }

    private void exibirDialogoConfirmacaoImportacao(Uri uri, BackupController.PreviewBackup preview) {
        String msg = "Backup identificado com sucesso!\n\n"
                + "• Viagens encontradas: " + preview.totalViagens + "\n"
                + "• Data do backup: " + preview.dataExportacao + "\n"
                + (preview.contemConfiguracao && !preview.motoristaBackup.isEmpty() ? "• Motorista: " + preview.motoristaBackup + "\n\n" : "\n")
                + "Como deseja importar essas viagens para este aparelho?";

        new AlertDialog.Builder(this)
                .setTitle("Importar Viagens")
                .setMessage(msg)
                .setPositiveButton("Adicionar (Mesclar)", (dialog, which) -> {
                    executarImportacao(uri, false, preview.contemConfiguracao);
                })
                .setNeutralButton("Substituir Tudo", (dialog, which) -> {
                    new AlertDialog.Builder(ConfiguracaoActivity.this)
                            .setTitle("Atenção: Substituir Dados")
                            .setMessage("Todas as viagens cadastradas atualmente neste celular serão substituídas pelas viagens do backup. Deseja continuar?")
                            .setPositiveButton("Sim, substituir", (d2, w2) -> {
                                executarImportacao(uri, true, preview.contemConfiguracao);
                            })
                            .setNegativeButton("Voltar", null)
                            .show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void executarImportacao(Uri uri, boolean substituir, boolean restaurarConfiguracao) {
        Toast.makeText(this, "Importando dados...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                BackupController.ResultadoImportacao resultado =
                        BackupController.importarBackup(getApplicationContext(), uri, substituir, restaurarConfiguracao);

                mainHandler.post(() -> {
                    if (resultado.configuracaoRestaurada) {
                        carregarConfigs();
                    }

                    String mensagemSucesso = "Importação concluída com sucesso!\n\n"
                            + "• Total no arquivo: " + resultado.totalNoArquivo + "\n"
                            + "• Viagens inseridas: " + resultado.totalImportadas + "\n"
                            + (resultado.configuracaoRestaurada ? "• Tarifas e configurações atualizadas.\n" : "");

                    new AlertDialog.Builder(ConfiguracaoActivity.this)
                            .setTitle("Sucesso")
                            .setMessage(mensagemSucesso)
                            .setPositiveButton("OK", null)
                            .show();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    new AlertDialog.Builder(ConfiguracaoActivity.this)
                            .setTitle("Erro na Importação")
                            .setMessage("Falha ao importar registros: " + e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}