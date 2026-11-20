package com.digitalmuniz.kadornataxi;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.activities.ConfiguracaoActivity;
import com.digitalmuniz.kadornataxi.view.activities.SolicitacaoActivity;
import com.digitalmuniz.kadornataxi.view.activities.ViagensActivity;
import com.digitalmuniz.kadornataxi.view.fragments.DashboardFragment;
import com.digitalmuniz.kadornataxi.view.fragments.ViagensFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ViagensNavigationAndDashboardTest {

    private Context context;
    private ViagemDAO viagemDAO;
    private ActivityController<ViagensActivity> activityController;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        viagemDAO = new ViagemDAO(context);
        viagemDAO.deleteAll();
    }

    @After
    public void tearDown() {
        if (activityController != null) {
            activityController.pause().stop().destroy();
            activityController = null;
        }
        viagemDAO.deleteAll();
        DbHelper.getInstance(context).getWritableDatabase()
                .delete(DbHelper.Configuracao.NOME_TABELA, null, null);
    }

    @Test
    public void alternarAbasBottomNavigation_atualizaFragmentsTituloEFab() throws Exception {
        inserirDadosExemplo();
        activityController = Robolectric.buildActivity(ViagensActivity.class).setup();
        ViagensActivity activity = activityController.get();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
        FloatingActionButton fab = activity.findViewById(R.id.fabNovaViagem);
        TextView txtTitulo = activity.findViewById(R.id.txtTitulo);

        Fragment viagensFrag = activity.getSupportFragmentManager().findFragmentByTag("tab_viagens");
        Fragment dashFrag = activity.getSupportFragmentManager().findFragmentByTag("tab_dashboard");

        assertTrue(viagensFrag instanceof ViagensFragment);
        assertTrue(dashFrag instanceof DashboardFragment);
        assertFalse(viagensFrag.isHidden());
        assertTrue(dashFrag.isHidden());
        assertEquals(View.VISIBLE, fab.getVisibility());
        assertEquals("Minhas Viagens", txtTitulo.getText().toString());

        // Alterna para Dashboard
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
        aguardarViewVisivel(activity.findViewById(R.id.dashboardContent));

        assertTrue(viagensFrag.isHidden());
        assertFalse(dashFrag.isHidden());
        assertEquals(View.GONE, fab.getVisibility());
        assertEquals("Dashboard Financeiro", txtTitulo.getText().toString());

        // Alterna de volta para Viagens
        bottomNav.setSelectedItemId(R.id.nav_viagens);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertFalse(viagensFrag.isHidden());
        assertTrue(dashFrag.isHidden());
        assertEquals(View.VISIBLE, fab.getVisibility());
        assertEquals("Minhas Viagens", txtTitulo.getText().toString());
    }

    @Test
    public void dashboardComDadosNoSqlite_renderizaKpisGraficoEClientes() throws Exception {
        inserirDadosExemplo();
        activityController = Robolectric.buildActivity(ViagensActivity.class).setup();
        ViagensActivity activity = activityController.get();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        View dashboardContent = activity.findViewById(R.id.dashboardContent);
        aguardarViewVisivel(dashboardContent);

        TextView txtFaturamentoTotal = activity.findViewById(R.id.txtFaturamentoTotal);
        TextView txtKpiKms = activity.findViewById(R.id.txtKpiKms);
        TextView txtKpiReaisPorKm = activity.findViewById(R.id.txtKpiReaisPorKm);
        TextView txtKpiHorasEspera = activity.findViewById(R.id.txtKpiHorasEspera);
        TextView txtKpiMediaDiaria = activity.findViewById(R.id.txtKpiMediaDiaria);
        ChipGroup chipGroupAnos = activity.findViewById(R.id.chipGroupAnos);
        ChipGroup chipGroupMeses = activity.findViewById(R.id.chipGroupMeses);
        RecyclerView recyclerClassificacao = activity.findViewById(R.id.recyclerClassificacao);

        assertEquals("R$ 500,00", txtFaturamentoTotal.getText().toString());
        assertEquals("100,0 km", txtKpiKms.getText().toString());
        assertEquals("R$ 5,00/km", txtKpiReaisPorKm.getText().toString());
        assertEquals("1h 30m", txtKpiHorasEspera.getText().toString());
        assertEquals("R$ 250,00", txtKpiMediaDiaria.getText().toString());
        assertTrue(chipGroupAnos.getChildCount() >= 1);
        assertTrue(chipGroupMeses.getChildCount() >= 2);
        assertNotNull(recyclerClassificacao.getAdapter());
        assertEquals(2, recyclerClassificacao.getAdapter().getItemCount());
    }

    @Test
    public void dashboardSemDadosComConfig_exibeEmptyStateEAbreSolicitacaoAoClicarCta() throws Exception {
        new ConfiguracaoDAO(context).salvarConfiguracaoSilenciosamente(
                new Configuracao(1L, 2.5f, 30f, "Marcelo", "Quartzolit", "", "", "", "", ""));
        activityController = Robolectric.buildActivity(ViagensActivity.class).setup();
        ViagensActivity activity = activityController.get();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        View dashboardEmpty = activity.findViewById(R.id.dashboardEmpty);
        aguardarViewVisivel(dashboardEmpty);

        activity.findViewById(R.id.btnRegistrarViagemEmpty).performClick();
        Intent nextIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(nextIntent);
        assertNotNull(nextIntent.getComponent());
        assertEquals(SolicitacaoActivity.class.getName(), nextIntent.getComponent().getClassName());
    }

    @Test
    public void dashboardSemDadosSemConfig_redirecionaParaConfiguracaoAoClicarCta() throws Exception {
        DbHelper.getInstance(context).getWritableDatabase()
                .delete(DbHelper.Configuracao.NOME_TABELA, null, null);
        activityController = Robolectric.buildActivity(ViagensActivity.class).setup();
        ViagensActivity activity = activityController.get();

        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_dashboard);

        View dashboardEmpty = activity.findViewById(R.id.dashboardEmpty);
        aguardarViewVisivel(dashboardEmpty);

        activity.findViewById(R.id.btnRegistrarViagemEmpty).performClick();
        Intent nextIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(nextIntent);
        assertNotNull(nextIntent.getComponent());
        assertEquals(ConfiguracaoActivity.class.getName(), nextIntent.getComponent().getClassName());
    }

    private void inserirDadosExemplo() {
        viagemDAO.inserirLote(Arrays.asList(
                new Viagem(0L, "Itu", "2026-03-01", "09:00", "Sorocaba", "Viagem 1",
                        60f, 240f, 20f, "Marcelo", 1.0f, 40f, "Quartzolit", 300f),
                new Viagem(0L, "Itu", "2026-03-02", "14:00", "Campinas", "Viagem 2",
                        40f, 180f, 0f, "Marcelo", 0.5f, 20f, "Comum", 200f)
        ), true);
    }

    private void aguardarViewVisivel(View view) throws InterruptedException {
        long limite = System.currentTimeMillis() + 3000L;
        while (System.currentTimeMillis() < limite) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            if (view.getVisibility() == View.VISIBLE) {
                return;
            }
            Thread.sleep(20L);
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(View.VISIBLE, view.getVisibility());
    }
}
