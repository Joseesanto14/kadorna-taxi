package com.digitalmuniz.kadornataxi;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.controller.BackupController;
import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Servico;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BackupControllerTest {

    private Context context;
    private ViagemDAO viagemDAO;
    private ConfiguracaoDAO configuracaoDAO;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        viagemDAO = new ViagemDAO(context);
        configuracaoDAO = new ConfiguracaoDAO(context);
        viagemDAO.deleteAll();
    }

    @After
    public void tearDown() {
        viagemDAO.deleteAll();
        DbHelper.getInstance(context).getWritableDatabase()
                .delete(DbHelper.Configuracao.NOME_TABELA, null, null);
    }

    @Test
    public void exportarEImportarV2_preservaMultiplosServicosEValoresHistoricos() throws Exception {
        Configuracao cfg = new Configuracao(1L, 3.5f, 40f, "Marcelo", "Quartzolit", "12.345.678/0001-90", "Kadorna", "Marcelo", "11999999999", "a@b.com");
        configuracaoDAO.salvarConfiguracaoSilenciosamente(cfg);

        Viagem v1 = new Viagem(
                0L, "Itu", "2026-09-10", "08:30", "São Paulo", "Executiva",
                50f, 175f, 0f, "Marcelo", 1f, 40f, "Quartzolit", 250f, "Marcos"
        );
        v1.setServicos(Arrays.asList(
                new Servico("Pedágio", 20f),
                new Servico("Estacionamento", 15f)
        ));
        viagemDAO.inserir(v1);

        JSONObject backupJson = BackupController.construirBackupJson(viagemDAO.getAll(), configuracaoDAO.getConfiguracao());
        assertEquals(2, BackupController.SCHEMA_VERSION);
        assertEquals(2, backupJson.getInt("schema_version"));

        JSONArray viagensJson = backupJson.getJSONArray("viagens");
        assertEquals(1, viagensJson.length());
        JSONObject item = viagensJson.getJSONObject(0);
        assertEquals(35.0, item.getDouble("valorServico"), 0.01);
        JSONArray servicosJson = item.getJSONArray("servicos");
        assertEquals(2, servicosJson.length());
        assertEquals("Pedágio", servicosJson.getJSONObject(0).getString("descricao"));
        assertEquals(20.0, servicosJson.getJSONObject(0).getDouble("valor"), 0.01);

        // Limpa banco e importa a partir do arquivo JSON via Uri
        viagemDAO.deleteAll();
        assertEquals(0, viagemDAO.getCount());

        File tempFile = File.createTempFile("backup_v2_", ".json", context.getCacheDir());
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
            writer.write(backupJson.toString(2));
        }

        Uri uri = Uri.fromFile(tempFile);
        BackupController.PreviewBackup preview = BackupController.inspecionarBackup(context, uri);
        assertEquals(1, preview.totalViagens);
        assertTrue(preview.contemConfiguracao);

        BackupController.ResultadoImportacao resultado = BackupController.importarBackup(context, uri, true, true);
        assertEquals(1, resultado.totalNoArquivo);
        assertEquals(1, resultado.totalImportadas);
        assertTrue(resultado.configuracaoRestaurada);

        List<Viagem> restauradas = viagemDAO.getAll();
        assertEquals(1, restauradas.size());
        Viagem restaurada = restauradas.get(0);
        assertEquals(250f, restaurada.getValorTotal(), 0.01f);
        assertEquals(2, restaurada.getServicos().size());
        assertEquals("Pedágio", restaurada.getServicos().get(0).getDescricao());
        assertEquals("Estacionamento", restaurada.getServicos().get(1).getDescricao());
        assertEquals(35f, restaurada.getValorTotalServicos(), 0.01f);
        assertTrue(tempFile.delete() || !tempFile.exists());
    }

    @Test
    public void importarBackupLegadoV1_semArrayServicos_criaServicoAdicionalEPreservaTotal() throws Exception {
        JSONObject v1Item = new JSONObject();
        v1Item.put("origem", "Itu");
        v1Item.put("data", "2026-01-20");
        v1Item.put("hora", "14:00");
        v1Item.put("destino", "Sorocaba");
        v1Item.put("descricao", "Corrida v1");
        v1Item.put("kmsRodados", 30.0);
        v1Item.put("valorKms", 90.0);
        v1Item.put("valorServico", 25.0);
        v1Item.put("motorista", "Marcelo");
        v1Item.put("horaEspera", 0.0);
        v1Item.put("valorHoraEspera", 0.0);
        v1Item.put("classificacao", "Comum");
        v1Item.put("valorTotal", 115.0);
        v1Item.put("solicitante", "Ana");

        JSONObject root = new JSONObject();
        root.put("app", BackupController.APP_SIGNATURE);
        root.put("schema_version", 1);
        root.put("export_date", "2026-01-21 10:00:00");
        root.put("viagens", new JSONArray(Collections.singletonList(v1Item)));

        BackupController.ResultadoImportacao resultado =
                BackupController.importarBackupJson(context, root, true, false);
        assertEquals(1, resultado.totalNoArquivo);
        assertEquals(1, resultado.totalImportadas);
        assertFalse(resultado.configuracaoRestaurada);

        List<Viagem> restauradas = viagemDAO.getAll();
        assertEquals(1, restauradas.size());
        Viagem v = restauradas.get(0);
        assertEquals(115f, v.getValorTotal(), 0.01f);
        assertEquals(1, v.getServicos().size());
        assertEquals("Serviço adicional", v.getServicos().get(0).getDescricao());
        assertEquals(25f, v.getServicos().get(0).getValor(), 0.01f);
    }
}
