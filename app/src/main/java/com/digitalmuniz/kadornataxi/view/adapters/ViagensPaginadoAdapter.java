package com.digitalmuniz.kadornataxi.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
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
        final TextView txtQtdServicos;
        final TextView txtDescricao;
        final TextView txtSolicitante;
        final ImageButton btnEditarViagem;
        final ImageButton btnExcluirViagem;

        ViagemViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDataHora = itemView.findViewById(R.id.txtDataHora);
            txtValorTotal = itemView.findViewById(R.id.txtValorTotal);
            txtOrigem = itemView.findViewById(R.id.txtOrigem);
            txtDestino = itemView.findViewById(R.id.txtDestino);
            txtKmsRodados = itemView.findViewById(R.id.txtKmsRodados);
            txtQtdServicos = itemView.findViewById(R.id.txtQtdServicos);
            txtDescricao = itemView.findViewById(R.id.txtDescricao);
            txtSolicitante = itemView.findViewById(R.id.txtSolicitante);
            btnEditarViagem = itemView.findViewById(R.id.btnEditarViagem);
            btnExcluirViagem = itemView.findViewById(R.id.btnExcluirViagem);
        }

        void bind(Viagem v, OnViagemActionListener listener) {
            if (v == null) {
                itemView.setOnClickListener(null);
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
            bindQuantidadeServicos(txtQtdServicos, v);
            txtDescricao.setText(v.getDescricao() != null ? v.getDescricao() : "");

            String solicitante = v.getSolicitante();
            if (solicitante != null && !solicitante.trim().isEmpty()) {
                txtSolicitante.setText("Solicitante: " + solicitante.trim());
                txtSolicitante.setVisibility(View.VISIBLE);
            } else {
                txtSolicitante.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(view -> {
                if (listener != null) {
                    listener.onDetalhes(v);
                }
            });
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

    /**
     * Formata e estiliza o indicador de quantidade de servicos no card da viagem.
     */
    public static void bindQuantidadeServicos(TextView txtQtdServicos, Viagem viagem) {
        if (txtQtdServicos == null) {
            return;
        }
        int qtd = viagem != null ? viagem.getQuantidadeServicos() : 0;
        if (qtd <= 0) {
            txtQtdServicos.setText("0 serviços");
            txtQtdServicos.setBackgroundResource(R.drawable.bg_pill_gray);
            txtQtdServicos.setTextColor(ContextCompat.getColor(txtQtdServicos.getContext(), R.color.text_secondary));
            return;
        }
        String texto = qtd == 1 ? "1 serviço" : qtd + " serviços";
        txtQtdServicos.setText(texto);
        txtQtdServicos.setBackgroundResource(R.drawable.bg_pill_green_light);
        txtQtdServicos.setTextColor(ContextCompat.getColor(txtQtdServicos.getContext(), R.color.brand_green_dark));
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
