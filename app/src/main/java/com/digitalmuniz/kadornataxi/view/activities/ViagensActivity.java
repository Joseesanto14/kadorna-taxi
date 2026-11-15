package com.digitalmuniz.kadornataxi.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.data.mock.ViagensMock;
import com.digitalmuniz.kadornataxi.model.entities.Configuracao;
import com.digitalmuniz.kadornataxi.view.adapters.MesViagensAdapter;
import com.digitalmuniz.kadornataxi.data.dao.ConfiguracaoDAO;
import com.digitalmuniz.kadornataxi.data.dao.ViagemDAO;
import com.digitalmuniz.kadornataxi.view.dto.MesViagens;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.controller.RelatorioPdfGenerator;
import com.digitalmuniz.kadornataxi.view.interfaces.OnViagemActionListener;
import com.google.android.material.chip.Chip;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class ViagensActivity extends AppCompatActivity {

    private RecyclerView recyclerMeses;
    private MesViagensAdapter adapter;
    private SearchView searchView;
    private Chip chipSeparadas;
    private List<Viagem> todasAsViagensOriginal = new ArrayList<>();
    private final ExecutorService pdfExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_viagens);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicializarViews();
        setupFiltros();
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarDadosDoBanco();
    }

    private void inicializarViews() {
        recyclerMeses = findViewById(R.id.recyclerMeses);
        recyclerMeses.setLayoutManager(new LinearLayoutManager(this));

        searchView = findViewById(R.id.searchView);
        chipSeparadas = findViewById(R.id.chipSeparadas);
    }

    private void carregarDadosDoBanco() {
        todasAsViagensOriginal = new ViagemDAO(this).getAllOrdenadoPorDataDesc();

        aplicarFiltros();
    }

    private void setupFiltros() {
        chipSeparadas.setOnCheckedChangeListener((buttonView, isChecked) -> {
            aplicarFiltros();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                aplicarFiltros(); // Filtra a cada letra digitada
                return true;
            }
        });
    }

    private void aplicarFiltros() {
        String textoBusca = searchView.getQuery().toString().toLowerCase();
        boolean filtrarApenasSeparadas = chipSeparadas.isChecked();

        List<Viagem> listaFiltrada = new ArrayList<>();

        for (Viagem v : todasAsViagensOriginal) {
            boolean matchesBusca = false;
            boolean matchesChip = false;

            if (textoBusca.isEmpty()) {
                matchesBusca = true;
            } else {
                String destino = v.getDestino() != null ? v.getDestino().toLowerCase() : "";
                String origem = v.getOrigem() != null ? v.getOrigem().toLowerCase() : "";
                String data = v.getData() != null ? v.getData() : "";
                String motorista = v.getMotorista() != null ? v.getMotorista().toLowerCase() : "";
                String descricao = v.getDescricao() != null ? v.getDescricao().toLowerCase() : "";
                String classificacao = v.getClassificacao() != null ? v.getClassificacao().toLowerCase() : "";
                String solicitante = v.getSolicitante() != null ? v.getSolicitante().toLowerCase() : "";

                if (destino.contains(textoBusca) ||
                        origem.contains(textoBusca) ||
                        data.contains(textoBusca) ||
                        descricao.contains(textoBusca) ||
                        motorista.contains(textoBusca) ||
                        classificacao.contains(textoBusca) ||
                        solicitante.contains(textoBusca)
                )
                {
                    matchesBusca = true;
                }
            }

            if (!filtrarApenasSeparadas) {
                matchesChip = true;
            } else {
                if (v.getClassificacao() != null && !v.getClassificacao().equals("Comum")) {
                    matchesChip = true;
                }
            }

            if (matchesBusca && matchesChip) {
                listaFiltrada.add(v);
            }
        }

        atualizarRecyclerView(listaFiltrada);
    }

    private void atualizarRecyclerView(List<Viagem> listaParaExibir) {
        List<MesViagens> listaMeses = Viagem.gerarListaDeMeses(listaParaExibir);

        adapter = new MesViagensAdapter(listaMeses,
                (mesAno, viagens) -> gerarRelatorioPdf(mesAno, viagens),
                new OnViagemActionListener() {
                    @Override
                    public void onEditar(Viagem viagem) {
                        Intent intent = new Intent(ViagensActivity.this, SolicitacaoActivity.class);
                        intent.putExtra(SolicitacaoActivity.EXTRA_VIAGEM, viagem);
                        startActivity(intent);
                    }

                    @Override
                    public void onExcluir(Viagem viagem) {
                        new AlertDialog.Builder(ViagensActivity.this)
                                .setTitle("Excluir Viagem")
                                .setMessage("Deseja excluir esta viagem?")
                                .setPositiveButton("Excluir", (dialog, which) -> {
                                    new ViagemDAO(ViagensActivity.this).delete(viagem.getId());
                                    carregarDadosDoBanco();
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }
                });

        recyclerMeses.setAdapter(adapter);
    }

    private void gerarRelatorioPdf(String mesAno, List<Viagem> viagensDoMes) {
        ConfiguracaoDAO dao = new ConfiguracaoDAO(this);

        if (!dao.configExiste()) {
            Toast.makeText(this, "Configure o valor do KM antes!", Toast.LENGTH_LONG).show();
            irConfigActivity(null);
            return;
        }

        Configuracao config = dao.getConfiguracao();

        List<Viagem> viagensOrdenadas = new ArrayList<>(viagensDoMes);
        viagensOrdenadas.sort(Comparator.comparing(Viagem::getData).thenComparing(Viagem::getHora));

        RelatorioPdfGenerator gerador = new RelatorioPdfGenerator(this, viagensOrdenadas, config, mesAno);

        Toast.makeText(this, "Gerando relatório PDF...", Toast.LENGTH_SHORT).show();
        pdfExecutor.execute(() -> {
            try {
                File arquivo = gerador.gerarArquivoPdf();
                mainHandler.post(() -> {
                    Toast.makeText(ViagensActivity.this, "Relatório gerado com sucesso!", Toast.LENGTH_LONG).show();
                    gerador.abrirDocumento(arquivo);
                });
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(ViagensActivity.this, "Erro ao gerar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pdfExecutor.shutdown();
    }

    // --- MÉTODOS DE BOTÕES (Chamados pelo XML) ---

    public void irSolicitacaoActivity(View view) {
        ConfiguracaoDAO dao = new ConfiguracaoDAO(this);
        if (dao.configExiste()) {
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