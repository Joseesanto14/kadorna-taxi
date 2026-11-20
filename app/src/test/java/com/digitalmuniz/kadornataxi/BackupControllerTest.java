package com.digitalmuniz.kadornataxi;

import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.controller.BackupController;
import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.CustoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Custo;
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
    private CustoDAO custoDAO;
    private ConfiguracaoDAO configuracaoDAO;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        viagemDAO = new ViagemDAO(context);
        custoDAO = new CustoDAO(context);
        configuracaoDAO = new ConfiguracaoDAO(context);
        viagemDAO.deleteAll();
        custoDAO.deletarTodos();
    }

    @After
    public void tearDown() {
        viagemDAO.deleteAll();
        custoDAO.deletarTodos();
        DbHelper.getInstance(context).getWritableDatabase()
                .delete(DbHelper.Configuracao.NOME_TABELA, null, null);
    }

    @Test
    public void exportarEImportarV3_preservaViagensServicosCustosEValoresHistoricos() throws Exception {
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

        custoDAO.inserir(new Custo("Combustível", 180.50, "2026-09-10"));
        custoDAO.inserir(new Custo("Lavagem", 45.00, "2026-09-11"));

        JSONObject backupJson = BackupController.construirBackupJson(
                viagemDAO.getAll(),
                custoDAO.getAll(),
                configuracaoDAO.getConfiguracao()
        );
        assertEquals(3, BackupController.SCHEMA_VERSION);
        assertEquals(3, backupJson.getInt("schema_version"));

        JSONArray viagensJson = backupJson.getJSONArray("viagens");
        assertEquals(1, viagensJson.length());
        JSONObject item = viagensJson.getJSONObject(0);
        assertEquals(35.0, item.getDouble("valorServico"), 0.01);
        JSONArray servicosJson = item.getJSONArray("servicos");
        assertEquals(2, servicosJson.length());

        JSONArray custosJson = backupJson.getJSONArray("custos");
        assertEquals(2, custosJson.length());
        assertEquals("Lavagem", custosJson.getJSONObject(0).getString("descricao"));
        assertEquals(45.0, custosJson.getJSONObject(0).getDouble("valor"), 0.01);

        // Limpa banco e importa a partir do arquivo JSON via Uri
        viagemDAO.deleteAll();
        custoDAO.deletarTodos();
        assertEquals(0, viagemDAO.getCount());
        assertEquals(0, custoDAO.getAll().size());

        File tempFile = File.createTempFile("backup_v3_", ".json", context.getCacheDir());
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
            writer.write(backupJson.toString(2));
        }

        Uri uri = Uri.fromFile(tempFile);
        BackupController.PreviewBackup preview = BackupController.inspecionarBackup(context, uri);
        assertEquals(1, preview.totalViagens);
        assertEquals(2, preview.totalCustos);
        assertTrue(preview.contemConfiguracao);

        BackupController.ResultadoImportacao resultado = BackupController.importarBackup(context, uri, true, true);
        assertEquals(1, resultado.totalNoArquivo);
        assertEquals(1, resultado.totalImportadas);
        assertEquals(2, resultado.totalCustosNoArquivo);
        assertEquals(2, resultado.totalCustosImportados);
        assertTrue(resultado.configuracaoRestaurada);

        List<Viagem> restauradas = viagemDAO.getAll();
        assertEquals(1, restauradas.size());
        Viagem restaurada = restauradas.get(0);
        assertEquals(250f, restaurada.getValorTotal(), 0.01f);
        assertEquals(2, restaurada.getServicos().size());

        List<Custo> custosRestaurados = custoDAO.getAll();
        assertEquals(2, custosRestaurados.size());
        assertEquals("Lavagem", custosRestaurados.get(0).getDescricao());
        assertEquals(45.0, custosRestaurados.get(0).getValor(), 0.01);
        assertEquals("Combustível", custosRestaurados.get(1).getDescricao());
        assertEquals(180.5, custosRestaurados.get(1).getValor(), 0.01);

        // Mesclar novamente não duplica viagens nem custos
        BackupController.ResultadoImportacao mesclado = BackupController.importarBackup(context, uri, false, false);
        assertEquals(0, mesclado.totalImportadas);
        assertEquals(0, mesclado.totalCustosImportados);
        assertEquals(2, custoDAO.getAll().size());

        assertTrue(tempFile.delete() || !tempFile.exists());
    }

    @Test
    public void importarBackupLegadoV1_semArrayServicosESemCustos_criaServicoAdicionalELimpaCustosAoSubstituir() throws Exception {
        custoDAO.inserir(new Custo("Custo antigo", 50.0, "2026-01-10"));

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
        assertEquals(0, resultado.totalCustosNoArquivo);
        assertEquals(0, resultado.totalCustosImportados);
        assertFalse(resultado.configuracaoRestaurada);

        List<Viagem> restauradas = viagemDAO.getAll();
        assertEquals(1, restauradas.size());
        Viagem v = restauradas.get(0);
        assertEquals(115f, v.getValorTotal(), 0.01f);
        assertEquals(1, v.getServicos().size());
        assertEquals("Serviço adicional", v.getServicos().get(0).getDescricao());
        assertEquals(25f, v.getServicos().get(0).getValor(), 0.01f);
        assertTrue(custoDAO.getAll().isEmpty());
    }

    @Test
    public void importarBackupLegadoV2_semArrayCustos_preservaCustosExistentesAoMesclar() throws Exception {
        custoDAO.inserir(new Custo("Combustível", 100.0, "2026-02-10"));

        JSONObject rootV2 = BackupController.construirBackupJson(
                Collections.singletonList(new Viagem(
                        0L, "Itu", "2026-02-12", "09:00", "Salto", "Corrida v2",
                        15f, 45f, 0f, "Marcelo", 0f, 0f, "Comum", 45f
                )),
                null
        );
        rootV2.put("schema_version", 2);
        rootV2.remove("custos");

        BackupController.ResultadoImportacao resultado =
                BackupController.importarBackupJson(context, rootV2, false, false);
        assertEquals(1, resultado.totalImportadas);
        assertEquals(0, resultado.totalCustosImportados);
        assertEquals(1, custoDAO.getAll().size());
    }
}
