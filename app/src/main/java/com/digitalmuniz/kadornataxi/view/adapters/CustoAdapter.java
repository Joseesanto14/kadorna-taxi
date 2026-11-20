package com.digitalmuniz.kadornataxi.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.model.entities.Custo;
import com.digitalmuniz.kadornataxi.util.FormatadorDashboard;

public class CustoAdapter extends ListAdapter<Custo, CustoAdapter.ViewHolder> {

    public interface OnCustoActionListener {
        void onEditar(Custo custo);

        void onExcluir(Custo custo);
    }

    private static final DiffUtil.ItemCallback<Custo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Custo>() {
                @Override
                public boolean areItemsTheSame(@NonNull Custo oldItem, @NonNull Custo newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Custo oldItem, @NonNull Custo newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final OnCustoActionListener actionListener;

    public CustoAdapter(OnCustoActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_custo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), actionListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtCustoDescricao;
        final TextView txtCustoData;
        final TextView txtCustoValor;
        final ImageButton btnEditarCusto;
        final ImageButton btnExcluirCusto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCustoDescricao = itemView.findViewById(R.id.txtCustoDescricao);
            txtCustoData = itemView.findViewById(R.id.txtCustoData);
            txtCustoValor = itemView.findViewById(R.id.txtCustoValor);
            btnEditarCusto = itemView.findViewById(R.id.btnEditarCusto);
            btnExcluirCusto = itemView.findViewById(R.id.btnExcluirCusto);
        }

        void bind(Custo item, OnCustoActionListener listener) {
            txtCustoDescricao.setText(item.getDescricao());
            txtCustoData.setText(FormatadorDashboard.formatarDataBR(item.getData()));
            txtCustoValor.setText(FormatadorDashboard.formatarMoeda(item.getValor()));

            btnEditarCusto.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditar(item);
                }
            });
            btnExcluirCusto.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExcluir(item);
                }
            });
        }
    }
}
