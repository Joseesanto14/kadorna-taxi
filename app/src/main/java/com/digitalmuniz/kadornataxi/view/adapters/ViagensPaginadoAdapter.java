package com.digitalmuniz.kadornataxi.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.dto.ListaViagensItem;
import com.digitalmuniz.kadornataxi.view.interfaces.OnGerarRelatorioListener;
import com.digitalmuniz.kadornataxi.view.interfaces.OnViagemActionListener;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class ViagensPaginadoAdapter extends ListAdapter<ListaViagensItem, RecyclerView.ViewHolder> {

    public interface OnGrupoClickListener {
        void onGrupoClick(String mesAno);
    }

    private static final DiffUtil.ItemCallback<ListaViagensItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ListaViagensItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull ListaViagensItem oldItem, @NonNull ListaViagensItem newItem) {
                    return oldItem.getStableId().equals(newItem.getStableId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ListaViagensItem oldItem, @NonNull ListaViagensItem newItem) {
                    return oldItem.areContentsTheSame(newItem);
                }
            };

    private final OnGrupoClickListener grupoClickListener;
    private final OnGerarRelatorioListener relatorioListener;
    private final OnViagemActionListener viagemActionListener;

    public ViagensPaginadoAdapter(
            OnGrupoClickListener grupoClickListener,
            OnGerarRelatorioListener relatorioListener,
            OnViagemActionListener viagemActionListener
    ) {
        super(DIFF_CALLBACK);
        this.grupoClickListener = grupoClickListener;
        this.relatorioListener = relatorioListener;
        this.viagemActionListener = viagemActionListener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ListaViagensItem.TYPE_HEADER_GRUPO:
                return new HeaderGrupoViewHolder(
                        inflater.inflate(R.layout.item_grupo_mes_header, parent, false));
            case ListaViagensItem.TYPE_BOTAO_PDF:
                return new BotaoPdfViewHolder(
                        inflater.inflate(R.layout.item_grupo_mes_botao_pdf, parent, false));
            case ListaViagensItem.TYPE_LOADING:
                return new LoadingViewHolder(
                        inflater.inflate(R.layout.item_viagem_loading, parent, false));
            case ListaViagensItem.TYPE_VIAGEM:
            default:
                return new ViagemViewHolder(
                        inflater.inflate(R.layout.item_resumo_viagens, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListaViagensItem item = getItem(position);
        switch (item.getViewType()) {
            case ListaViagensItem.TYPE_HEADER_GRUPO:
                ((HeaderGrupoViewHolder) holder).bind(item, grupoClickListener);
                break;
            case ListaViagensItem.TYPE_BOTAO_PDF:
                ((BotaoPdfViewHolder) holder).bind(item, relatorioListener);
                break;
            case ListaViagensItem.TYPE_VIAGEM:
                ((ViagemViewHolder) holder).bind(item.getViagem(), viagemActionListener);
                break;
            case ListaViagensItem.TYPE_LOADING:
            default:
                break;
        }
    }

    static class HeaderGrupoViewHolder extends RecyclerView.ViewHolder {
        final ConstraintLayout layoutHeader;
        final TextView txtMes;
        final TextView txtQuantidade;
        final ImageView imgArrow;

        HeaderGrupoViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutHeader = itemView.findViewById(R.id.layoutHeader);
            txtMes = itemView.findViewById(R.id.txtMes);
            txtQuantidade = itemView.findViewById(R.id.txtQuantidade);
            imgArrow = itemView.findViewById(R.id.imgArrow);
        }

        void bind(ListaViagensItem item, OnGrupoClickListener listener) {
            txtMes.setText(item.getMesAno());
            int total = item.getTotalViagens();
            String textoQtd = total + (total == 1 ? " viagem" : " viagens");
            txtQuantidade.setText(textoQtd);
            imgArrow.setRotation(item.isExpandido() ? 180f : 0f);
            layoutHeader.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGrupoClick(item.getMesAno());
                }
            });
        }
    }

    static class BotaoPdfViewHolder extends RecyclerView.ViewHolder {
        final MaterialButton btGerarRelatorio;

        BotaoPdfViewHolder(@NonNull View itemView) {
            super(itemView);
            btGerarRelatorio = itemView.findViewById(R.id.btGerarRelatorio);
        }

        void bind(ListaViagensItem item, OnGerarRelatorioListener listener) {
            btGerarRelatorio.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGerarRelatorio(item.getMesAno());
                }
            });
        }
    }

    static class ViagemViewHolder extends RecyclerView.ViewHolder {
        final TextView txtDataHora;
        final TextView txtValorTotal;
        final TextView txtOrigem;
        final TextView txtDestino;
        final TextView txtKmsRodados;
        final TextView txtDescricao;
        final ImageButton btnEditarViagem;
        final ImageButton btnExcluirViagem;

        ViagemViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDataHora = itemView.findViewById(R.id.txtDataHora);
            txtValorTotal = itemView.findViewById(R.id.txtValorTotal);
            txtOrigem = itemView.findViewById(R.id.txtOrigem);
            txtDestino = itemView.findViewById(R.id.txtDestino);
            txtKmsRodados = itemView.findViewById(R.id.txtKmsRodados);
            txtDescricao = itemView.findViewById(R.id.txtDescricao);
            btnEditarViagem = itemView.findViewById(R.id.btnEditarViagem);
            btnExcluirViagem = itemView.findViewById(R.id.btnExcluirViagem);
        }

        void bind(Viagem v, OnViagemActionListener listener) {
            if (v == null) {
                return;
            }
            String hora = v.getHora() != null ? v.getHora() : "";
            String dataHora = v.getDiaMes() + " \u2022 " + hora;
            txtDataHora.setText(dataHora);

            String valorTotal = "R$ " + String.format(Locale.getDefault(), "%.2f", v.getValorTotal());
            txtValorTotal.setText(valorTotal);

            txtOrigem.setText(v.getOrigem() != null ? v.getOrigem() : "");
            txtDestino.setText(v.getDestino() != null ? v.getDestino() : "");
            txtKmsRodados.setText(v.getKmsRodados() + " km");
            txtDescricao.setText(v.getDescricao() != null ? v.getDescricao() : "");

            btnEditarViagem.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onEditar(v);
                }
            });
            btnExcluirViagem.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onExcluir(v);
                }
            });
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
