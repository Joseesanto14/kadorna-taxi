package com.digitalmuniz.kadornataxi.controller;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de Backup e Migração de Dados do Kadorna Taxi.
 * Responsável pela serialização, desserialização e orquestração de exportação/importação
 * em formato JSON sem dependências externas adicionais e com integridade atômica no SQLite.
 */
public class BackupController {

    private static final String TAG = "BackupController";
    public static final String APP_SIGNATURE = "KadornaTaxi";
    public static final int SCHEMA_VERSION = 1;

    /**
     * DTO para pré-visualização dos dados contidos no arquivo antes de confirmar a importação.
     */
    public static class PreviewBackup {
        public final int totalViagens;
        public final boolean contemConfiguracao;
        public final String dataExportacao;
        public final String motoristaBackup;

        public PreviewBackup(int totalViagens, boolean contemConfiguracao, String dataExportacao, String motoristaBackup) {
            this.totalViagens = totalViagens;
            this.contemConfiguracao = contemConfiguracao;
            this.dataExportacao = dataExportacao;
            this.motoristaBackup = motoristaBackup;
        }
    }

    /**
     * DTO de resultado da importação.
     */
    public static class ResultadoImportacao {
        public final int totalNoArquivo;
        public final int totalImportadas;
        public final boolean configuracaoRestaurada;

        public ResultadoImportacao(int totalNoArquivo, int totalImportadas, boolean configuracaoRestaurada) {
            this.totalNoArquivo = totalNoArquivo;
            this.totalImportadas = totalImportadas;
            this.configuracaoRestaurada = configuracaoRestaurada;
        }
    }

    /**
     * Exporta todas as viagens e a configuração para um arquivo JSON estruturado
     * e retorna a Intent pronta para a Android Share Sheet.
     */
    public static Intent gerarIntentExportacao(Context context) throws Exception {
        ViagemDAO viagemDAO = new ViagemDAO(context);
        ConfiguracaoDAO configDAO = new ConfiguracaoDAO(context);

        List<Viagem> todasViagens = viagemDAO.getAll();
        Configuracao config = configDAO.getConfiguracao();

        JSONObject jsonRoot = new JSONObject();
        jsonRoot.put("app", APP_SIGNATURE);
        jsonRoot.put("schema_version", SCHEMA_VERSION);
        jsonRoot.put("export_timestamp", System.currentTimeMillis());

        String dataFormatada = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        jsonRoot.put("export_date", dataFormatada);

        // Serializar Configuração
        if (config != null) {
            JSONObject jsonConfig = new JSONObject();
            jsonConfig.put("valorKmRodado", config.getValorKmRodado());
            jsonConfig.put("valorHoraEspera", config.getValorHoraEspera());
            jsonConfig.put("motorista", config.getMotorista() != null ? config.getMotorista() : "");
            jsonConfig.put("classificacaoViagemSeparada", config.getClassificacaoViagemSeparada() != null ? config.getClassificacaoViagemSeparada() : "Comum");
            jsonConfig.put("cnpj", config.getCnpj() != null ? config.getCnpj() : "");
            jsonConfig.put("nomeFantasia", config.getNomeFantasia() != null ? config.getNomeFantasia() : "");
            jsonConfig.put("titularCNPJ", config.getTitularCNPJ() != null ? config.getTitularCNPJ() : "");
            jsonConfig.put("telefone", config.getTelefone() != null ? config.getTelefone() : "");
            jsonConfig.put("email", config.getEmail() != null ? config.getEmail() : "");
            jsonRoot.put("configuracao", jsonConfig);
        }

        // Serializar Viagens (preservando todos os valores históricos originais)
        JSONArray jsonViagens = new JSONArray();
        for (Viagem v : todasViagens) {
            JSONObject item = new JSONObject();
            item.put("origem", v.getOrigem() != null ? v.getOrigem() : "");
            item.put("data", v.getData() != null ? v.getData() : "");
            item.put("hora", v.getHora() != null ? v.getHora() : "");
            item.put("destino", v.getDestino() != null ? v.getDestino() : "");
            item.put("descricao", v.getDescricao() != null ? v.getDescricao() : "");
            item.put("kmsRodados", v.getKmsRodados());
            item.put("valorKms", v.getValorKms());
            item.put("valorServico", v.getValorServico());
            item.put("motorista", v.getMotorista() != null ? v.getMotorista() : "");
            item.put("horaEspera", v.getHoraEspera());
            item.put("valorHoraEspera", v.getValorHoraEspera());
            item.put("classificacao", v.getClassificacao() != null ? v.getClassificacao() : "Comum");
            item.put("valorTotal", v.getValorTotal());
            jsonViagens.put(item);
        }
        jsonRoot.put("viagens", jsonViagens);

        // Salvar em arquivo no diretório de documentos do app
        File docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (docsDir != null && !docsDir.exists()) {
            docsDir.mkdirs();
        }

        String timeStampArquivo = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String nomeArquivo = "kadorna_taxi_backup_" + timeStampArquivo + ".json";
        File arquivoBackup = new File(docsDir, nomeArquivo);

        try (FileOutputStream fos = new FileOutputStream(arquivoBackup);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(jsonRoot.toString(2));
            writer.flush();
        }

        Log.d(TAG, "Backup exportado com sucesso: " + arquivoBackup.getAbsolutePath() + " (" + todasViagens.size() + " viagens)");

        // Criar URI compatível com FileProvider
        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", arquivoBackup);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Backup Kadorna Taxi - " + timeStampArquivo);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Arquivo de backup do Kadorna Taxi gerado em " + dataFormatada + " com " + todasViagens.size() + " viagens.");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return Intent.createChooser(shareIntent, "Exportar / Compartilhar Backup");
    }

    /**
     * Realiza a leitura e validação prévia de um arquivo de backup via URI do SAF,
     * sem realizar alterações no banco de dados.
     */
    public static PreviewBackup inspecionarBackup(Context context, Uri uri) throws Exception {
        JSONObject root = lerJsonObjectDeUri(context, uri);
        validarAssinatura(root);

        JSONArray viagensArray = root.optJSONArray("viagens");
        int totalViagens = viagensArray != null ? viagensArray.length() : 0;

        boolean contemConfig = root.has("configuracao");
        String dataExportacao = root.optString("export_date", "Data desconhecida");
        String motorista = "";

        if (contemConfig) {
            JSONObject cfg = root.optJSONObject("configuracao");
            if (cfg != null) {
                motorista = cfg.optString("motorista", "");
            }
        }

        return new PreviewBackup(totalViagens, contemConfig, dataExportacao, motorista);
    }

    /**
     * Importa as viagens e opcionalmente as configurações a partir do arquivo JSON selecionado.
     * Preserva estritamente os valores financeiros históricos sem recálculo dinâmico.
     */
    public static ResultadoImportacao importarBackup(Context context, Uri uri, boolean substituirExistentes, boolean restaurarConfiguracao) throws Exception {
        JSONObject root = lerJsonObjectDeUri(context, uri);
        validarAssinatura(root);

        JSONArray viagensArray = root.optJSONArray("viagens");
        List<Viagem> listaViagens = new ArrayList<>();

        if (viagensArray != null) {
            for (int i = 0; i < viagensArray.length(); i++) {
                JSONObject item = viagensArray.getJSONObject(i);

                // Instanciação sem o construtor contextual para NÃO disparar recálculo de tarifas
                Viagem v = new Viagem();
                v.setOrigem(item.optString("origem", ""));
                v.setData(item.optString("data", ""));
                v.setHora(item.optString("hora", ""));
                v.setDestino(item.optString("destino", ""));
                v.setDescricao(item.optString("descricao", ""));
                v.setKmsRodados((float) item.optDouble("kmsRodados", 0.0));
                v.setValorKms((float) item.optDouble("valorKms", 0.0));
                v.setValorServico((float) item.optDouble("valorServico", 0.0));
                v.setMotorista(item.optString("motorista", ""));
                v.setHoraEspera((float) item.optDouble("horaEspera", 0.0));
                v.setValorHoraEspera((float) item.optDouble("valorHoraEspera", 0.0));
                v.setClassificacao(item.optString("classificacao", "Comum"));
                v.setValorTotal((float) item.optDouble("valorTotal", 0.0));

                listaViagens.add(v);
            }
        }

        // Inserção atômica no banco de dados
        ViagemDAO viagemDAO = new ViagemDAO(context);
        int totalImportadas = viagemDAO.inserirLote(listaViagens, substituirExistentes);

        // Restauração de configurações se solicitado
        boolean configuracaoRestaurada = false;
        if (restaurarConfiguracao && root.has("configuracao")) {
            JSONObject cfgJson = root.getJSONObject("configuracao");
            Configuracao novaConfig = new Configuracao(
                    1,
                    (float) cfgJson.optDouble("valorKmRodado", 0.0),
                    (float) cfgJson.optDouble("valorHoraEspera", 0.0),
                    cfgJson.optString("motorista", ""),
                    cfgJson.optString("classificacaoViagemSeparada", "Comum"),
                    cfgJson.optString("cnpj", ""),
                    cfgJson.optString("nomeFantasia", ""),
                    cfgJson.optString("titularCNPJ", ""),
                    cfgJson.optString("telefone", ""),
                    cfgJson.optString("email", "")
            );

            new ConfiguracaoDAO(context).salvarConfiguracaoSilenciosamente(novaConfig);
            configuracaoRestaurada = true;
        }

        Log.d(TAG, "Importação concluída. Lidas: " + listaViagens.size() + ", Inseridas: " + totalImportadas + ", Config: " + configuracaoRestaurada);
        return new ResultadoImportacao(listaViagens.size(), totalImportadas, configuracaoRestaurada);
    }

    private static JSONObject lerJsonObjectDeUri(Context context, Uri uri) throws IOException, Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) {
                throw new IOException("Não foi possível acessar o arquivo selecionado.");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
        }
        return new JSONObject(sb.toString());
    }

    private static void validarAssinatura(JSONObject root) {
        if (!root.has("app") || !APP_SIGNATURE.equals(root.optString("app"))) {
            throw new IllegalArgumentException("O arquivo selecionado não é um backup válido do Kadorna Taxi.");
        }
    }
}
