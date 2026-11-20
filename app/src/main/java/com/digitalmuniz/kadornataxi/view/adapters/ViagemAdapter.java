package com.digitalmuniz.kadornataxi.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.interfaces.OnViagemActionListener;

import java.util.List;
import java.util.Locale;

public class ViagemAdapter extends RecyclerView.Adapter<ViagemAdapter.ViewHolder> {

    private final List<Viagem> viagens;
    private final OnViagemActionListener listener;

    public ViagemAdapter(List<Viagem> viagens, OnViagemActionListener listener) {
        this.viagens = viagens;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resumo_viagens, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Viagem v = viagens.get(position);

        String dataHora = v.getDiaMes() + " • " + v.getHora();
        holder.txtDataHora.setText(dataHora);

        String valorTotal = "R$ " + String.format(Locale.getDefault(),
                "%.2f", v.getValorTotal());
        holder.txtValorTotal.setText(valorTotal);

        holder.txtOrigem.setText(v.getOrigem());

        holder.txtDestino.setText(v.getDestino());

        String kmsRodados = v.getKmsRodados() + " km";
        holder.txtKmsRodados.setText(kmsRodados);

        holder.txtDescricao.setText(v.getDescricao());

        String solicitante = v.getSolicitante();
        if (solicitante != null && !solicitante.trim().isEmpty()) {
            String textoSolicitante = "Solicitante: " + solicitante.trim();
            holder.txtSolicitante.setText(textoSolicitante);
            holder.txtSolicitante.setVisibility(View.VISIBLE);
        } else {
            holder.txtSolicitante.setVisibility(View.GONE);
        }

        holder.btnEditarViagem.setOnClickListener(view -> listener.onEditar(v));
        holder.btnExcluirViagem.setOnClickListener(view -> listener.onExcluir(v));
    }

    @Override
    public int getItemCount() {
        return viagens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDataHora, txtValorTotal, txtOrigem, txtDestino, txtKmsRodados, txtDescricao, txtSolicitante;
        ImageButton btnEditarViagem, btnExcluirViagem;

        ViewHolder(View itemView) {
            super(itemView);
            txtDataHora = itemView.findViewById(R.id.txtDataHora);
            txtValorTotal = itemView.findViewById(R.id.txtValorTotal);
            txtOrigem = itemView.findViewById(R.id.txtOrigem);
            txtDestino = itemView.findViewById(R.id.txtDestino);
            txtKmsRodados = itemView.findViewById(R.id.txtKmsRodados);
            txtDescricao = itemView.findViewById(R.id.txtDescricao);
            txtSolicitante = itemView.findViewById(R.id.txtSolicitante);
            btnEditarViagem = itemView.findViewById(R.id.btnEditarViagem);
            btnExcluirViagem = itemView.findViewById(R.id.btnExcluirViagem);
        }
    }
}