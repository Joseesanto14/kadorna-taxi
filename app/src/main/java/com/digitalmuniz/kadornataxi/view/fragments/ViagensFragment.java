package com.digitalmuniz.kadornataxi.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.controller.ViagensController;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.activities.ConfiguracaoActivity;
import com.digitalmuniz.kadornataxi.view.activities.SolicitacaoActivity;
import com.digitalmuniz.kadornataxi.view.adapters.ViagensPaginadoAdapter;
import com.digitalmuniz.kadornataxi.view.dto.ListaViagensItem;
import com.digitalmuniz.kadornataxi.view.dto.ViagensFiltro;
import com.digitalmuniz.kadornataxi.view.interfaces.OnViagemActionListener;
import com.google.android.material.chip.Chip;

import java.util.List;

public class ViagensFragment extends Fragment {

    private static final int PREFETCH_DISTANCE = 8;

    private RecyclerView recyclerMeses;
    private View emptyStateViagens;
    private ViagensPaginadoAdapter adapter;
    private LinearLayoutManager layoutManager;
    private SearchView searchView;
    private Chip chipSeparadas;
    private ViagensController controller;

    private final ViagensController.Callback listaCallback = new ViagensController.Callback() {
        @Override
        public void onListaAtualizada(List<ListaViagensItem> itens, boolean loading) {
            if (!isAdded() || adapter == null || emptyStateViagens == null || recyclerMeses == null) {
                return;
            }
            adapter.submitList(itens);
            emptyStateViagens.setVisibility(itens.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerMeses.setVisibility(itens.isEmpty() ? View.GONE : View.VISIBLE);
        }

        @Override
        public void onErro(String mensagem) {
            if (!isAdded()) {
                return;
            }
            Toast.makeText(requireContext(),
                    "Erro ao carregar viagens: " + (mensagem != null ? mensagem : "erro desconhecido"),
                    Toast.LENGTH_LONG).show();
        }
    };

    private final ViagensController.PdfCallback pdfCallback = new ViagensController.PdfCallback() {
        @Override
        public void onConfiguracaoAusente() {
            if (!isAdded()) {
                return;
            }
            Toast.makeText(requireContext(), "Configure o valor do KM antes!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(requireContext(), ConfiguracaoActivity.class));
        }

        @Override
        public void onPdfGerado(String mesAno) {
            if (!isAdded()) {
                return;
            }
            Toast.makeText(requireContext(), "Relatório gerado com sucesso!", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onErro(String mensagem) {
            if (!isAdded()) {
                return;
            }
            Toast.makeText(requireContext(),
                    "Erro ao gerar PDF: " + (mensagem != null ? mensagem : "erro desconhecido"),
                    Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller = new ViagensController(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_viagens, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        inicializarViews(view);
        setupFiltros();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            recarregarLista();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isResumed()) {
            recarregarLista();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recyclerMeses != null) {
            recyclerMeses.setAdapter(null);
        }
        recyclerMeses = null;
        emptyStateViagens = null;
        searchView = null;
        chipSeparadas = null;
        adapter = null;
        layoutManager = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (controller != null) {
            controller.shutdown();
        }
    }

    private void recarregarLista() {
        if (controller != null) {
            controller.carregarGrupos(criarFiltroAtual(), listaCallback);
        }
    }

    private void inicializarViews(View root) {
        recyclerMeses = root.findViewById(R.id.recyclerMeses);
        emptyStateViagens = root.findViewById(R.id.emptyStateViagens);
        searchView = root.findViewById(R.id.searchView);
        chipSeparadas = root.findViewById(R.id.chipSeparadas);

        layoutManager = new LinearLayoutManager(requireContext());
        recyclerMeses.setLayoutManager(layoutManager);

        adapter = new ViagensPaginadoAdapter(
                mesAno -> controller.alternarExpansaoGrupo(mesAno, listaCallback),
                mesAno -> controller.gerarRelatorioPdf(requireContext(), mesAno, pdfCallback),
                new OnViagemActionListener() {
                    @Override
                    public void onEditar(Viagem viagem) {
                        Intent intent = new Intent(requireContext(), SolicitacaoActivity.class);
                        intent.putExtra(SolicitacaoActivity.EXTRA_VIAGEM, viagem);
                        startActivity(intent);
                    }

                    @Override
                    public void onExcluir(Viagem viagem) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Excluir Viagem")
                                .setMessage("Deseja excluir esta viagem?")
                                .setPositiveButton("Excluir", (dialog, which) ->
                                        controller.excluirViagem(viagem, listaCallback))
                                .setNegativeButton("Cancelar", null)
                                .show();
                    }
                }
        );

        recyclerMeses.setAdapter(adapter);
        recyclerMeses.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) {
                    return;
                }
                verificarPrefetchPaginacao();
            }
        });
    }

    private void verificarPrefetchPaginacao() {
        if (layoutManager == null || adapter == null || controller == null) {
            return;
        }
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        if (lastVisible == RecyclerView.NO_POSITION) {
            return;
        }
        List<ListaViagensItem> currentList = adapter.getCurrentList();
        if (currentList.isEmpty()) {
            return;
        }

        int limiteBusca = Math.min(currentList.size() - 1, lastVisible + PREFETCH_DISTANCE);
        for (int i = lastVisible; i <= limiteBusca; i++) {
            String mesAno = currentList.get(i).getMesAno();
            boolean fimDoGrupo = (i == currentList.size() - 1)
                    || !mesAno.equals(currentList.get(i + 1).getMesAno());
            if (fimDoGrupo && controller.podePaginarGrupo(mesAno)) {
                controller.carregarProximaPagina(mesAno, listaCallback);
                break;
            }
        }
    }

    private void setupFiltros() {
        chipSeparadas.setOnCheckedChangeListener((buttonView, isChecked) ->
                controller.carregarGrupos(criarFiltroAtual(), listaCallback));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                controller.carregarGrupos(criarFiltroAtual(), listaCallback);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                controller.agendarBusca(criarFiltroAtual(), listaCallback);
                return true;
            }
        });
    }

    private ViagensFiltro criarFiltroAtual() {
        CharSequence query = searchView != null ? searchView.getQuery() : "";
        boolean somenteEmpresas = chipSeparadas != null && chipSeparadas.isChecked();
        return new ViagensFiltro(query != null ? query.toString() : "", somenteEmpresas);
    }
}
