package com.digitalmuniz.kadornataxi.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.view.dto.MesViagens;
import com.digitalmuniz.kadornataxi.view.interfaces.OnGerarRelatorioListener;
import com.digitalmuniz.kadornataxi.view.interfaces.OnViagemActionListener;

import java.util.List;

public class MesViagensAdapter extends RecyclerView.Adapter<MesViagensAdapter.ViewHolder> {

    private final List<MesViagens> meses;
    private final OnGerarRelatorioListener listener;
    private final OnViagemActionListener viagemActionListener;
    private final RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();

    public MesViagensAdapter(List<MesViagens> meses, OnGerarRelatorioListener listener, OnViagemActionListener viagemActionListener) {
        this.meses = meses;
        this.listener = listener;
        this.viagemActionListener = viagemActionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mes_viagens, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MesViagens mes = meses.get(position);

        holder.layoutHeader.setOnClickListener(v -> {
            mes.setExpandido(!mes.isExpandido());
            notifyItemChanged(position);
        });

        holder.recyclerViagens.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext()));

        holder.recyclerViagens.setAdapter(
                new ViagemAdapter(mes.getViagens(), viagemActionListener));

        holder.recyclerViagens.setRecycledViewPool(viewPool);

        holder.txtMes.setText(mes.getMesAno());

        String numeroViagens = mes.getViagens().size() + " viagens";
        holder.txtQuantidade.setText(numeroViagens);

        holder.imgArrow.setRotation(mes.isExpandido() ? 180f : 0f);

        holder.dividerContent.setVisibility(mes.isExpandido() ? View.VISIBLE : View.GONE);
        holder.containerExpansivel.setVisibility(mes.isExpandido() ? View.VISIBLE : View.GONE);

        holder.btGerarRelatorio.setOnClickListener(v ->
                listener.onGerarRelatorio(mes.getMesAno()));

    }

    @Override
    public int getItemCount() {
        return meses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout layoutHeader;
        TextView txtMes, txtQuantidade;
        ImageView imgArrow;
        View dividerContent;
        LinearLayout containerExpansivel;
        RecyclerView recyclerViagens;
        Button btGerarRelatorio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutHeader = itemView.findViewById(R.id.layoutHeader);
            txtMes = itemView.findViewById(R.id.txtMes);
            txtQuantidade = itemView.findViewById(R.id.txtQuantidade);
            imgArrow = itemView.findViewById(R.id.imgArrow);
            dividerContent = itemView.findViewById(R.id.dividerContent);
            containerExpansivel = itemView.findViewById(R.id.containerExpansivel);
            recyclerViagens = itemView.findViewById(R.id.recyclerViagens);
            btGerarRelatorio = itemView.findViewById(R.id.btGerarRelatorio);
        }
    }
}
