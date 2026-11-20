package com.digitalmuniz.kadornataxi;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Servico;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.activities.SolicitacaoActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SolicitacaoActivityTest {

    private Context context;
    private ViagemDAO viagemDAO;
    private ActivityController<SolicitacaoActivity> controller;
    private Locale localeOriginal;

    @Before
    public void setUp() {
        localeOriginal = Locale.getDefault();
        Locale.setDefault(new Locale("pt", "BR"));
        context = ApplicationProvider.getApplicationContext();
        viagemDAO = new ViagemDAO(context);
        viagemDAO.deleteAll();
        new ConfiguracaoDAO(context).salvarConfiguracaoSilenciosamente(
                new Configuracao(1L, 3.0f, 30f, "Marcelo", "Quartzolit", "", "", "", "", "")
        );
    }

    @After
    public void tearDown() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        if (controller != null) {
            controller.pause().stop().destroy();
            controller = null;
        }
        viagemDAO.deleteAll();
        DbHelper.getInstance(context).getWritableDatabase()
                .delete(DbHelper.Configuracao.NOME_TABELA, null, null);
        Locale.setDefault(localeOriginal);
    }

    @Test
    public void modoCriacao_exibeSolicitanteAntesDeDescricaoE1LinhaOpcionalDeServico() {
        controller = Robolectric.buildActivity(SolicitacaoActivity.class).setup();
        SolicitacaoActivity activity = controller.get();

        // Verifica ordem no layout: edSolicitante antes de edDescricao
        ViewGroup scrollContent = (ViewGroup) ((ViewGroup) activity.findViewById(R.id.main)).getChildAt(0);
        List<Integer> ids = new ArrayList<>();
        coletarIds(scrollContent, ids);
        int idxSolicitante = ids.indexOf(R.id.edSolicitante);
        int idxDescricao = ids.indexOf(R.id.edDescricao);
        assertTrue(idxSolicitante != -1 && idxDescricao != -1);
        assertTrue("Solicitante deveria vir antes de Descrição", idxSolicitante < idxDescricao);

        LinearLayout containerServicos = activity.findViewById(R.id.containerServicos);
        assertEquals(1, containerServicos.getChildCount());
    }

    @Test
    public void adicionarRemoverEValidarServicos_atualizaTotalEImpedeLinhaParcial() {
        controller = Robolectric.buildActivity(SolicitacaoActivity.class).setup();
        SolicitacaoActivity activity = controller.get();

        LinearLayout containerServicos = activity.findViewById(R.id.containerServicos);
        ImageButton btnAdicionar = activity.findViewById(R.id.btnAdicionarServico);
        EditText edValorTotal = activity.findViewById(R.id.edValorTotal);
        EditText edDestino = activity.findViewById(R.id.edDestino);
        EditText edDescricao = activity.findViewById(R.id.edDescricao);

        edDestino.setText("Sorocaba");
        edDescricao.setText("Entrega");

        // Preenche 1a linha parcialmente (só valor) -> deve impedir salvar com erro
        View linha1 = containerServicos.getChildAt(0);
        EditText edDesc1 = linha1.findViewById(R.id.edServicoDescricao);
        EditText edVal1 = linha1.findViewById(R.id.edServicoValor);
        edVal1.setText("1500"); // 15,00
        assertEquals("15,00", edVal1.getText().toString());
        assertEquals("15,00", edValorTotal.getText().toString());

        activity.findViewById(R.id.btGerarViagem).performClick();
        assertFalse(activity.isFinishing());
        assertEquals("Informe o serviço", edDesc1.getError().toString());

        // Completa 1a linha e adiciona 2a linha (só descrição -> erro "Informe o valor")
        edDesc1.setText("Pedágio");
        btnAdicionar.performClick();
        assertEquals(2, containerServicos.getChildCount());
        View linha2 = containerServicos.getChildAt(1);
        EditText edDesc2 = linha2.findViewById(R.id.edServicoDescricao);
        EditText edVal2 = linha2.findViewById(R.id.edServicoValor);
        assertTrue(edDesc2.hasFocus());

        edDesc2.setText("Estacionamento");
        activity.findViewById(R.id.btGerarViagem).performClick();
        assertFalse(activity.isFinishing());
        assertEquals("Informe o valor", edVal2.getError().toString());

        // Preenche valor da 2a linha e verifica total = 35,00
        edVal2.setText("2000");
        assertEquals("35,00", edValorTotal.getText().toString());

        // Remove 2a linha -> volta para 1 linha e total 15,00
        linha2.findViewById(R.id.btnRemoverServico).performClick();
        assertEquals(1, containerServicos.getChildCount());
        assertEquals("15,00", edValorTotal.getText().toString());

        // Remove na única linha restante -> limpa os campos e mantém 1 linha
        linha1.findViewById(R.id.btnRemoverServico).performClick();
        assertEquals(1, containerServicos.getChildCount());
        assertEquals("", edDesc1.getText().toString());
        assertEquals("", edVal1.getText().toString());
        assertEquals("0,00", edValorTotal.getText().toString());

        // Preenche novamente 2 serviços e salva
        edDesc1.setText("Pedágio");
        edVal1.setText("1500");
        btnAdicionar.performClick();
        View novaLinha2 = containerServicos.getChildAt(1);
        ((EditText) novaLinha2.findViewById(R.id.edServicoDescricao)).setText("Balsa");
        ((EditText) novaLinha2.findViewById(R.id.edServicoValor)).setText("2500");
        // Adiciona uma 3a linha totalmente vazia que deve ser ignorada
        btnAdicionar.performClick();
        assertEquals(3, containerServicos.getChildCount());

        activity.findViewById(R.id.btGerarViagem).performClick();
        assertTrue(activity.isFinishing());

        List<Viagem> salvas = viagemDAO.getAll();
        assertEquals(1, salvas.size());
        Viagem salva = salvas.get(0);
        assertEquals(2, salva.getServicos().size());
        assertEquals(40.0f, salva.getValorTotalServicos(), 0.01f);
        assertEquals(40.0f, salva.getValorTotal(), 0.01f);
        assertNull(edDesc1.getError());
    }

    @Test
    public void modoEdicao_carregaServicosOuFallbackLegado() {
        Viagem viagem = new Viagem(
                10L, "Itu", "2026-09-12", "10:30", "Campinas", "Teste edição",
                10f, 30f, 0f, "Marcelo", 0f, 0f, "Quartzolit", 65f, "Lucas"
        );
        viagem.setServicos(Arrays.asList(
                new Servico("Pedágio", 15f),
                new Servico("Estacionamento", 20f)
        ));

        Intent intent = new Intent(context, SolicitacaoActivity.class);
        intent.putExtra(SolicitacaoActivity.EXTRA_VIAGEM, viagem);

        controller = Robolectric.buildActivity(SolicitacaoActivity.class, intent).setup();
        SolicitacaoActivity activity = controller.get();

        LinearLayout containerServicos = activity.findViewById(R.id.containerServicos);
        assertEquals(2, containerServicos.getChildCount());
        assertEquals("Pedágio", ((EditText) containerServicos.getChildAt(0).findViewById(R.id.edServicoDescricao)).getText().toString());
        assertEquals("15,00", ((EditText) containerServicos.getChildAt(0).findViewById(R.id.edServicoValor)).getText().toString());
        assertEquals("65,00", ((EditText) activity.findViewById(R.id.edValorTotal)).getText().toString());

        controller.pause().stop().destroy();

        // Caso legado sem lista de serviços, com valorServico > 0
        Viagem legado = new Viagem(
                11L, "Itu", "2026-09-12", "11:00", "Salto", "Legado",
                10f, 30f, 25f, "Marcelo", 0f, 0f, "Comum", 55f, ""
        );
        Intent intentLegado = new Intent(context, SolicitacaoActivity.class);
        intentLegado.putExtra(SolicitacaoActivity.EXTRA_VIAGEM, legado);

        controller = Robolectric.buildActivity(SolicitacaoActivity.class, intentLegado).setup();
        SolicitacaoActivity activityLegado = controller.get();
        LinearLayout containerLegado = activityLegado.findViewById(R.id.containerServicos);
        assertEquals(1, containerLegado.getChildCount());
        assertEquals("Serviço adicional", ((EditText) containerLegado.getChildAt(0).findViewById(R.id.edServicoDescricao)).getText().toString());
        assertEquals("25,00", ((EditText) containerLegado.getChildAt(0).findViewById(R.id.edServicoValor)).getText().toString());
        assertNotNull(activityLegado.findViewById(R.id.btGerarViagem));
    }

    private void coletarIds(View view, List<Integer> ids) {
        if (view.getId() != View.NO_ID) {
            ids.add(view.getId());
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                coletarIds(vg.getChildAt(i), ids);
            }
        }
    }
}
