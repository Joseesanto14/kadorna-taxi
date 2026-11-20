package com.digitalmuniz.kadornataxi.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.view.fragments.DashboardFragment;
import com.digitalmuniz.kadornataxi.view.fragments.ViagensFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ViagensActivity extends AppCompatActivity {

    private static final String TAG_VIAGENS = "tab_viagens";
    private static final String TAG_DASHBOARD = "tab_dashboard";
    private static final String KEY_ABA_SELECIONADA = "key_aba_selecionada";

    private TextView txtTitulo;
    private FloatingActionButton fabNovaViagem;
    private BottomNavigationView bottomNavigation;

    private ViagensFragment viagensFragment;
    private DashboardFragment dashboardFragment;
    private ConfiguracaoDAO configuracaoDAO;
    private int abaSelecionadaId = R.id.nav_viagens;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_viagens);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Deixa o inset inferior para a BottomNavigationView evitar padding duplicado e faixa no rodapé
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        configuracaoDAO = new ConfiguracaoDAO(this);
        txtTitulo = findViewById(R.id.txtTitulo);
        fabNovaViagem = findViewById(R.id.fabNovaViagem);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        configurarFragments(savedInstanceState);
        configurarBottomNavigation();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ABA_SELECIONADA, abaSelecionadaId);
    }

    private void configurarFragments(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            viagensFragment = new ViagensFragment();
            dashboardFragment = new DashboardFragment();
            fm.beginTransaction()
                    .add(R.id.fragmentContainer, viagensFragment, TAG_VIAGENS)
                    .add(R.id.fragmentContainer, dashboardFragment, TAG_DASHBOARD)
                    .hide(dashboardFragment)
                    .commit();
            abaSelecionadaId = R.id.nav_viagens;
        } else {
            Fragment fViagens = fm.findFragmentByTag(TAG_VIAGENS);
            Fragment fDashboard = fm.findFragmentByTag(TAG_DASHBOARD);

            viagensFragment = (fViagens instanceof ViagensFragment)
                    ? (ViagensFragment) fViagens
                    : new ViagensFragment();
            dashboardFragment = (fDashboard instanceof DashboardFragment)
                    ? (DashboardFragment) fDashboard
                    : new DashboardFragment();

            FragmentTransaction tx = fm.beginTransaction();
            if (!viagensFragment.isAdded()) {
                tx.add(R.id.fragmentContainer, viagensFragment, TAG_VIAGENS);
            }
            if (!dashboardFragment.isAdded()) {
                tx.add(R.id.fragmentContainer, dashboardFragment, TAG_DASHBOARD);
            }
            tx.commitNow();

            abaSelecionadaId = savedInstanceState.getInt(KEY_ABA_SELECIONADA, R.id.nav_viagens);
        }

        aplicarEstadoAba(abaSelecionadaId);
    }

    private void configurarBottomNavigation() {
        bottomNavigation.setSelectedItemId(abaSelecionadaId);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_viagens || itemId == R.id.nav_dashboard) {
                aplicarEstadoAba(itemId);
                return true;
            }
            return false;
        });
    }

    private void aplicarEstadoAba(int itemId) {
        abaSelecionadaId = itemId;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();

        if (itemId == R.id.nav_dashboard) {
            tx.hide(viagensFragment).show(dashboardFragment);
            txtTitulo.setText(R.string.titulo_dashboard_financeiro);
            fabNovaViagem.setVisibility(View.GONE);
        } else {
            tx.hide(dashboardFragment).show(viagensFragment);
            txtTitulo.setText(R.string.titulo_minhas_viagens);
            fabNovaViagem.setVisibility(View.VISIBLE);
        }
        tx.commit();
    }

    // --- MÉTODOS DE BOTÕES (Chamados pelo XML) ---

    public void irSolicitacaoActivity(View view) {
        if (configuracaoDAO.configExiste()) {
            startActivity(new Intent(this, SolicitacaoActivity.class));
        } else {
            Toast.makeText(this, "Configure o app primeiro!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, ConfiguracaoActivity.class));
        }
    }

    public void irConfigActivity(View view) {
        startActivity(new Intent(this, ConfiguracaoActivity.class));
    }
}
