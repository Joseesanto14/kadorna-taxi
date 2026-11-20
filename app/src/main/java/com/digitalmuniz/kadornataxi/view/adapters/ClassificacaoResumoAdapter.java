package com.digitalmuniz.kadornataxi.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.util.FormatadorDashboard;
import com.digitalmuniz.kadornataxi.view.dto.ClassificacaoResumoDTO;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Objects;

public class ClassificacaoResumoAdapter extends ListAdapter<ClassificacaoResumoDTO, ClassificacaoResumoAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<ClassificacaoResumoDTO> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ClassificacaoResumoDTO>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ClassificacaoResumoDTO oldItem,
                        @NonNull ClassificacaoResumoDTO newItem
                ) {
                    return Objects.equals(oldItem.getClassificacao(), newItem.getClassificacao());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ClassificacaoResumoDTO oldItem,
                        @NonNull ClassificacaoResumoDTO newItem
                ) {
                    return Objects.equals(oldItem.getClassificacao(), newItem.getClassificacao())
                            && oldItem.getTotalViagens() == newItem.getTotalViagens()
                            && Double.compare(oldItem.getTotalKms(), newItem.getTotalKms()) == 0
                            && Double.compare(oldItem.getFaturamentoTotal(), newItem.getFaturamentoTotal()) == 0
                            && Double.compare(oldItem.getPercentualParticipacao(), newItem.getPercentualParticipacao()) == 0;
                }
            };

    public ClassificacaoResumoAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_classificacao_resumo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgClassificacao;
        final TextView txtClassificacaoNome;
        final TextView txtClassificacaoDetalhes;
        final TextView txtClassificacaoValor;
        final TextView txtClassificacaoPercentual;
        final LinearProgressIndicator progressParticipacao;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgClassificacao = itemView.findViewById(R.id.imgClassificacao);
            txtClassificacaoNome = itemView.findViewById(R.id.txtClassificacaoNome);
            txtClassificacaoDetalhes = itemView.findViewById(R.id.txtClassificacaoDetalhes);
            txtClassificacaoValor = itemView.findViewById(R.id.txtClassificacaoValor);
            txtClassificacaoPercentual = itemView.findViewById(R.id.txtClassificacaoPercentual);
            progressParticipacao = itemView.findViewById(R.id.progressParticipacao);
        }

        void bind(ClassificacaoResumoDTO item) {
            String nome = item.getClassificacao() != null ? item.getClassificacao() : "Comum";
            boolean isComum = "Comum".equalsIgnoreCase(nome);
            imgClassificacao.setImageResource(isComum ? R.drawable.ic_person : R.drawable.ic_business);

            txtClassificacaoNome.setText(nome);
            int qtd = item.getTotalViagens();
            String labelViagens = qtd == 1 ? "1 viagem" : qtd + " viagens";
            String detalhes = labelViagens + " \u2022 " + FormatadorDashboard.formatarKms(item.getTotalKms());
            txtClassificacaoDetalhes.setText(detalhes);

            txtClassificacaoValor.setText(FormatadorDashboard.formatarMoeda(item.getFaturamentoTotal()));
            txtClassificacaoPercentual.setText(FormatadorDashboard.formatarPercentual(item.getPercentualParticipacao()));

            int progresso = (int) Math.round(Math.max(0.0, Math.min(100.0, item.getPercentualParticipacao())));
            progressParticipacao.setProgress(progresso);
        }
    }
}
