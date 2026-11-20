package com.digitalmuniz.kadornataxi;

import android.app.Dialog;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.model.entities.Servico;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.activities.ViagensActivity;
import com.digitalmuniz.kadornataxi.view.adapters.ViagensPaginadoAdapter;
import com.digitalmuniz.kadornataxi.view.dialogs.ViagemDetalhesDialog;
import com.digitalmuniz.kadornataxi.view.dto.ListaViagensItem;
import com.digitalmuniz.kadornataxi.view.interfaces.OnViagemActionListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ViagemDetalhesDialogTest {

    private ActivityController<ViagensActivity> activityController;
    private ViagensActivity activity;
    private Locale localeOriginal;

    @Before
    public void setUp() {
        localeOriginal = Locale.getDefault();
        Locale.setDefault(new Locale("pt", "BR"));
        activityController = Robolectric.buildActivity(ViagensActivity.class).setup();
        activity = activityController.get();
    }

    @After
    public void tearDown() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        if (activityController != null) {
            activityController.pause().stop().destroy();
            activityController = null;
        }
        Locale.setDefault(localeOriginal);
    }

    @Test
    public void cliqueNoCardDoAdapter_disparaOnDetalhesESeparaBotoesEditarExcluir() {
        Viagem viagem = new Viagem(
                10L,
                "Rua A, 100",
                "2026-11-20",
                "14:30",
                "Av. Paulista, 1000",
                "Viagem corporativa",
                12.5f,
                37.50f,
                25.00f,
                "Marcelo",
                0.50f,
                15.00f,
                "Quartzolit",
                77.50f,
                "Maria Silva"
        );

        AtomicReference<Viagem> detalhesRecebida = new AtomicReference<>();
        AtomicInteger editarCount = new AtomicInteger();
        AtomicInteger excluirCount = new AtomicInteger();

        ViagensPaginadoAdapter adapter = new ViagensPaginadoAdapter(
                mesAno -> {},
                mesAno -> {},
                new OnViagemActionListener() {
                    @Override
                    public void onEditar(Viagem v) {
                        editarCount.incrementAndGet();
                    }

                    @Override
                    public void onExcluir(Viagem v) {
                        excluirCount.incrementAndGet();
                    }

                    @Override
                    public void onDetalhes(Viagem v) {
                        detalhesRecebida.set(v);
                    }
                }
        );

        adapter.submitList(Collections.singletonList(ListaViagensItem.viagem("2026-11", viagem)));
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        FrameLayout parent = new FrameLayout(activity);
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(parent, ListaViagensItem.TYPE_VIAGEM);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.performClick();
        assertEquals(viagem, detalhesRecebida.get());
        assertEquals(0, editarCount.get());
        assertEquals(0, excluirCount.get());

        holder.itemView.findViewById(R.id.btnEditarViagem).performClick();
        holder.itemView.findViewById(R.id.btnExcluirViagem).performClick();
        assertEquals(1, editarCount.get());
        assertEquals(1, excluirCount.get());
    }

    @Test
    public void adapterPaginado_exibeQuantidadeServicosZeroUmECinquenta() {
        Viagem semServicos = new Viagem(
                100L, "Origem", "2026-11-20", "08:00", "Destino", "Desc",
                10f, 30f, 0f, "Marcelo", 0f, 0f, "Comum", 30f
        );

        Viagem umServicoLegado = new Viagem(
                101L, "Origem", "2026-11-20", "09:00", "Destino", "Desc",
                10f, 30f, 25f, "Marcelo", 0f, 0f, "Comum", 55f
        );

        Viagem cinquentaServicos = new Viagem(
                102L, "Origem", "2026-11-20", "10:00", "Destino", "Desc",
                10f, 30f, 0f, "Marcelo", 0f, 0f, "Comum", 30f
        );
        List<Servico> lista50 = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            lista50.add(new Servico(i, 102L, "Serviço " + i, 5f));
        }
        cinquentaServicos.setServicos(lista50);

        assertEquals(0, semServicos.getQuantidadeServicos());
        assertEquals(1, umServicoLegado.getQuantidadeServicos());
        assertEquals(50, cinquentaServicos.getQuantidadeServicos());

        ViagensPaginadoAdapter adapter = new ViagensPaginadoAdapter(mes -> {}, mes -> {}, null);
        adapter.submitList(Arrays.asList(
                ListaViagensItem.viagem("2026-11", semServicos),
                ListaViagensItem.viagem("2026-11", umServicoLegado),
                ListaViagensItem.viagem("2026-11", cinquentaServicos)
        ));
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        FrameLayout parent = new FrameLayout(activity);
        int corCinza = ContextCompat.getColor(activity, R.color.text_secondary);
        int corVerde = ContextCompat.getColor(activity, R.color.brand_green_dark);

        RecyclerView.ViewHolder holder0 = adapter.onCreateViewHolder(parent, ListaViagensItem.TYPE_VIAGEM);
        adapter.onBindViewHolder(holder0, 0);
        TextView txt0 = holder0.itemView.findViewById(R.id.txtQtdServicos);
        assertEquals("0 serviços", txt0.getText().toString());
        assertEquals(corCinza, txt0.getCurrentTextColor());
        assertEquals(R.drawable.bg_pill_gray, Shadows.shadowOf(txt0.getBackground()).getCreatedFromResId());

        RecyclerView.ViewHolder holder1 = adapter.onCreateViewHolder(parent, ListaViagensItem.TYPE_VIAGEM);
        adapter.onBindViewHolder(holder1, 1);
        TextView txt1 = holder1.itemView.findViewById(R.id.txtQtdServicos);
        assertEquals("1 serviço", txt1.getText().toString());
        assertEquals(corVerde, txt1.getCurrentTextColor());
        assertEquals(R.drawable.bg_pill_green_light, Shadows.shadowOf(txt1.getBackground()).getCreatedFromResId());

        RecyclerView.ViewHolder holder50 = adapter.onCreateViewHolder(parent, ListaViagensItem.TYPE_VIAGEM);
        adapter.onBindViewHolder(holder50, 2);
        TextView txt50 = holder50.itemView.findViewById(R.id.txtQtdServicos);
        assertEquals("50 serviços", txt50.getText().toString());
        assertEquals(corVerde, txt50.getCurrentTextColor());
        assertEquals(R.drawable.bg_pill_green_light, Shadows.shadowOf(txt50.getBackground()).getCreatedFromResId());
    }

    @Test
    public void mostrar_viagemSemServicos_exibeMensagemVaziaEOcultaSubtotal() {
        Viagem viagem = new Viagem(
                1L,
                "Terminal Central",
                "2026-11-20",
                "09:15",
                "Aeroporto Viracopos",
                "Traslado executivo",
                45.0f,
                135.00f,
                0.00f,
                "Carlos",
                0.25f,
                7.50f,
                "Comum",
                142.50f,
                "João Pedro"
        );

        Dialog dialog = ViagemDetalhesDialog.mostrar(activity, viagem);
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        TextView txtDataHora = dialog.findViewById(R.id.txtDetalheDataHora);
        TextView txtBadge = dialog.findViewById(R.id.txtBadgeClassificacao);
        TextView txtOrigem = dialog.findViewById(R.id.txtDetalheOrigem);
        TextView txtDestino = dialog.findViewById(R.id.txtDetalheDestino);
        TextView txtKmQtd = dialog.findViewById(R.id.txtDetalheKmQtd);
        TextView txtKmValor = dialog.findViewById(R.id.txtDetalheKmValor);
        TextView txtEsperaQtd = dialog.findViewById(R.id.txtDetalheEsperaQtd);
        TextView txtEsperaValor = dialog.findViewById(R.id.txtDetalheEsperaValor);
        TextView txtServicosVazio = dialog.findViewById(R.id.txtServicosVazio);
        View layoutSubtotal = dialog.findViewById(R.id.layoutSubtotalServicos);
        LinearLayout containerServicos = dialog.findViewById(R.id.containerServicosDetalhe);
        TextView txtValorTotal = dialog.findViewById(R.id.txtDetalheValorTotal);

        assertEquals("20/11 \u2022 09:15", txtDataHora.getText().toString());
        assertEquals("Comum", txtBadge.getText().toString());
        assertEquals("Terminal Central", txtOrigem.getText().toString());
        assertEquals("Aeroporto Viracopos", txtDestino.getText().toString());
        assertEquals("45,0 km", txtKmQtd.getText().toString());
        assertEquals("R$ 135,00", txtKmValor.getText().toString());
        assertEquals("0,25 h", txtEsperaQtd.getText().toString());
        assertEquals("R$ 7,50", txtEsperaValor.getText().toString());
        assertEquals(View.VISIBLE, txtServicosVazio.getVisibility());
        assertEquals("Nenhum serviço adicional registrado.", txtServicosVazio.getText().toString());
        assertEquals(View.GONE, layoutSubtotal.getVisibility());
        assertEquals(0, containerServicos.getChildCount());
        assertEquals("R$ 142,50", txtValorTotal.getText().toString());

        AppCompatButton btnFechar = dialog.findViewById(R.id.btnFecharDetalhes);
        btnFechar.performClick();
        assertFalse(dialog.isShowing());
    }

    @Test
    public void mostrar_viagemComMultiplosServicos_listaServicosESubtotal() {
        Viagem viagem = new Viagem(
                2L,
                "Fábrica Norte",
                "2026-11-18",
                "16:00",
                "Centro Logístico",
                "Entrega de documentos",
                10.0f,
                30.00f,
                0f,
                "Marcelo",
                0f,
                0f,
                "Quartzolit",
                0f,
                "Ana Paula"
        );
        viagem.setServicos(Arrays.asList(
                new Servico(1L, 2L, "Pedágio Anhanguera", 18.50f),
                new Servico(2L, 2L, "Estacionamento", 12.00f)
        ));

        Dialog dialog = ViagemDetalhesDialog.mostrar(activity, viagem);
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        TextView txtTituloServicos = dialog.findViewById(R.id.txtTituloServicos);
        LinearLayout containerServicos = dialog.findViewById(R.id.containerServicosDetalhe);
        TextView txtServicosVazio = dialog.findViewById(R.id.txtServicosVazio);
        View layoutSubtotal = dialog.findViewById(R.id.layoutSubtotalServicos);
        TextView txtSubtotal = dialog.findViewById(R.id.txtSubtotalServicos);
        TextView txtBadge = dialog.findViewById(R.id.txtBadgeClassificacao);
        TextView txtValorTotal = dialog.findViewById(R.id.txtDetalheValorTotal);

        assertEquals("Quartzolit", txtBadge.getText().toString());
        assertEquals("Serviços Prestados (2)", txtTituloServicos.getText().toString());
        assertEquals(View.GONE, txtServicosVazio.getVisibility());
        assertEquals(View.VISIBLE, layoutSubtotal.getVisibility());
        assertEquals("R$ 30,50", txtSubtotal.getText().toString());
        assertEquals("R$ 60,50", txtValorTotal.getText().toString());
        assertEquals(2, containerServicos.getChildCount());

        View row1 = containerServicos.getChildAt(0);
        assertEquals("Pedágio Anhanguera", ((TextView) row1.findViewById(R.id.txtNomeServico)).getText().toString());
        assertEquals("R$ 18,50", ((TextView) row1.findViewById(R.id.txtValorServico)).getText().toString());

        View row2 = containerServicos.getChildAt(1);
        assertEquals("Estacionamento", ((TextView) row2.findViewById(R.id.txtNomeServico)).getText().toString());
        assertEquals("R$ 12,00", ((TextView) row2.findViewById(R.id.txtValorServico)).getText().toString());

        ImageButton btnFecharTopo = dialog.findViewById(R.id.btnFecharTopo);
        btnFecharTopo.performClick();
        assertFalse(dialog.isShowing());
    }

    @Test
    public void mostrar_viagemCom50Servicos_renderizaTodosSemExcecoesEFecha() {
        Viagem viagem = new Viagem(
                3L,
                "Origem A",
                "2026-11-10",
                "08:00",
                "Destino B",
                "Rota completa",
                20.0f,
                60.00f,
                0f,
                "Marcelo",
                1.00f,
                30.00f,
                "Empresa X",
                0f,
                ""
        );

        List<Servico> servicos = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            servicos.add(new Servico(i, 3L, "Serviço " + i, 5.00f));
        }
        viagem.setServicos(servicos);

        Dialog dialog = ViagemDetalhesDialog.mostrar(activity, viagem);
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        LinearLayout containerServicos = dialog.findViewById(R.id.containerServicosDetalhe);
        TextView txtTituloServicos = dialog.findViewById(R.id.txtTituloServicos);
        TextView txtSubtotal = dialog.findViewById(R.id.txtSubtotalServicos);
        TextView txtValorTotal = dialog.findViewById(R.id.txtDetalheValorTotal);

        assertEquals(50, containerServicos.getChildCount());
        assertEquals("Serviços Prestados (50)", txtTituloServicos.getText().toString());
        assertEquals("R$ 250,00", txtSubtotal.getText().toString());
        assertEquals("R$ 340,00", txtValorTotal.getText().toString());

        dialog.findViewById(R.id.btnFecharDetalhes).performClick();
        assertFalse(dialog.isShowing());
    }

    @Test
    public void mostrar_viagemComCamposNulosEOpcionais_naoLancaNpeEFechaAoClicarEmFechar() {
        Viagem viagemNula = new Viagem(
                4L,
                null,
                null,
                null,
                null,
                null,
                0f,
                0f,
                0f,
                null,
                0f,
                0f,
                null,
                0f,
                null
        );

        Dialog dialog = ViagemDetalhesDialog.mostrar(activity, viagemNula);
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        assertEquals("-", ((TextView) dialog.findViewById(R.id.txtDetalheDataHora)).getText().toString());
        assertEquals("Comum", ((TextView) dialog.findViewById(R.id.txtBadgeClassificacao)).getText().toString());
        assertEquals("-", ((TextView) dialog.findViewById(R.id.txtDetalheOrigem)).getText().toString());
        assertEquals("-", ((TextView) dialog.findViewById(R.id.txtDetalheDestino)).getText().toString());
        assertEquals("-", ((TextView) dialog.findViewById(R.id.txtDetalheDescricao)).getText().toString());
        assertEquals("-", ((TextView) dialog.findViewById(R.id.txtDetalheMotorista)).getText().toString());
        assertEquals(View.GONE, dialog.findViewById(R.id.layoutDetalheSolicitante).getVisibility());

        dialog.findViewById(R.id.btnFecharDetalhes).performClick();
        assertFalse(dialog.isShowing());

        assertNull(ViagemDetalhesDialog.mostrar(activity, null));
        assertNull(ViagemDetalhesDialog.mostrar(null, viagemNula));
    }

    @Test
    public void mostrar_layoutDetalheSolicitanteApareceAntesDeTxtDetalheDescricaoNaHierarquia() {
        Viagem viagem = new Viagem(
                5L,
                "Origem",
                "2026-11-20",
                "11:00",
                "Destino",
                "Entrega prioritária",
                10.0f,
                30.00f,
                0f,
                "Marcelo",
                0f,
                0f,
                "Quartzolit",
                30.00f,
                "Carlos Eduardo"
        );

        Dialog dialog = ViagemDetalhesDialog.mostrar(activity, viagem);
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());

        View scroll = dialog.findViewById(R.id.scrollDetalhesViagem);
        assertNotNull(scroll);

        List<Integer> ids = new ArrayList<>();
        coletarIds(scroll, ids);

        int idxSolicitante = ids.indexOf(R.id.layoutDetalheSolicitante);
        int idxDescricao = ids.indexOf(R.id.txtDetalheDescricao);
        assertTrue(idxSolicitante != -1 && idxDescricao != -1);
        assertTrue(
                "layoutDetalheSolicitante deveria aparecer antes de txtDetalheDescricao",
                idxSolicitante < idxDescricao
        );
        assertEquals(View.VISIBLE, dialog.findViewById(R.id.layoutDetalheSolicitante).getVisibility());

        dialog.findViewById(R.id.btnFecharDetalhes).performClick();
        assertFalse(dialog.isShowing());
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
