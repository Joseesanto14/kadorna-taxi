package com.digitalmuniz.kadornataxi.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.digitalmuniz.kadornataxi.data.sqlite.DbHelper;
import com.digitalmuniz.kadornataxi.model.entities.Servico;
import com.digitalmuniz.kadornataxi.model.entities.Viagem;
import com.digitalmuniz.kadornataxi.view.dto.GrupoViagens;
import com.digitalmuniz.kadornataxi.view.dto.PaginaViagensResult;
import com.digitalmuniz.kadornataxi.view.dto.ViagemCursorKey;
import com.digitalmuniz.kadornataxi.view.dto.ViagensFiltro;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ViagemDAO {
    private static final String TAG = "ViagemDAO";
    private final DbHelper dbHelper;
    private final ServicoDAO servicoDAO;

    public ViagemDAO(Context context) {
        this.dbHelper = DbHelper.getInstance(context);
        this.servicoDAO = new ServicoDAO(this.dbHelper);
    }

    public ViagemDAO(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
        this.servicoDAO = new ServicoDAO(this.dbHelper);
    }

    // CREATE - Inserir um novo registro (viagem)

    public void inserirNoDatabase(Viagem viagem, Context context) {
        long id = inserir(viagem);
        Log.d(TAG, "Viagem inserida com o ID: " + id);
        Toast.makeText(context, "Viagem criada com sucesso!", Toast.LENGTH_SHORT).show();
    }

    public long inserir(Viagem viagem) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {
            long id = insert(db, viagem);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public int inserirLote(List<Viagem> viagens, boolean substituirExistentes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int inseridos = 0;
        db.beginTransactionNonExclusive();
        try {
            if (substituirExistentes) {
                db.delete(DbHelper.Viagem.NOME_TABELA, null, null);
            }
            for (Viagem viagem : viagens) {
                if (!substituirExistentes && existeViagemDuplicada(db, viagem)) {
                    continue;
                }
                insert(db, viagem);
                inseridos++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return inseridos;
    }

    private boolean existeViagemDuplicada(SQLiteDatabase db, Viagem viagem) {
        String dataFormatada = Viagem.formatarDataISO8601(viagem.getData());
        String selecao = DbHelper.Viagem.DATA + " = ? AND " +
                DbHelper.Viagem.HORA + " = ? AND " +
                DbHelper.Viagem.DESTINO + " = ? AND " +
                DbHelper.Viagem.VALOR_TOTAL + " = ?";
        String[] args = new String[]{
                dataFormatada != null ? dataFormatada : "",
                viagem.getHora() != null ? viagem.getHora() : "",
                viagem.getDestino() != null ? viagem.getDestino() : "",
                String.valueOf(viagem.getValorTotal())
        };

        try (Cursor cursor = db.query(DbHelper.Viagem.NOME_TABELA,
                new String[]{DbHelper.Viagem.ID},
                selecao, args, null, null, null)) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    private ContentValues toContentValues(Viagem viagem) {
        ContentValues values = new ContentValues();
        String dataIso = Viagem.formatarDataISO8601(viagem.getData());
        float valorServicoSincronizado = viagem.getValorTotalServicos();

        values.put(DbHelper.Viagem.ORIGEM, viagem.getOrigem());
        values.put(DbHelper.Viagem.DATA, dataIso != null ? dataIso : "");
        values.put(DbHelper.Viagem.HORA, viagem.getHora() != null ? viagem.getHora() : "");
        values.put(DbHelper.Viagem.DESTINO, viagem.getDestino());
        values.put(DbHelper.Viagem.DESCRICAO, viagem.getDescricao());
        values.put(DbHelper.Viagem.KMS_RODADOS, viagem.getKmsRodados());
        values.put(DbHelper.Viagem.VALOR_KM, viagem.getValorKms());
        values.put(DbHelper.Viagem.VALOR_SERVICO, valorServicoSincronizado);
        values.put(DbHelper.Viagem.MOTORISTA, viagem.getMotorista());
        values.put(DbHelper.Viagem.HORA_ESPERA, viagem.getHoraEspera());
        values.put(DbHelper.Viagem.VALOR_HORA_ESPERA, viagem.getValorHoraEspera());
        values.put(DbHelper.Viagem.CLASSIFICACAO, viagem.getClassificacao());
        values.put(DbHelper.Viagem.VALOR_TOTAL, viagem.getValorTotal());
        values.put(DbHelper.Viagem.SOLICITANTE, viagem.getSolicitante());
        return values;
    }

    private long insert(SQLiteDatabase db, Viagem viagem) {
        long id = db.insert(DbHelper.Viagem.NOME_TABELA, null, toContentValues(viagem));
        if (id != -1L) {
            viagem.setId(id);
            servicoDAO.substituirServicosDaViagem(db, id, resolverServicosParaPersistencia(viagem));
        }
        return id;
    }

    private List<Servico> resolverServicosParaPersistencia(Viagem viagem) {
        List<Servico> servicos = viagem.getServicos();
        if (!servicos.isEmpty()) {
            return servicos;
        }
        if (viagem.getValorServico() > 0f) {
            return Collections.singletonList(new Servico("Serviço adicional", viagem.getValorServico()));
        }
        return Collections.emptyList();
    }

    private void hidratarServicos(SQLiteDatabase db, List<Viagem> viagens) {
        if (viagens == null || viagens.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>(viagens.size());
        for (Viagem v : viagens) {
            if (v != null && v.getId() > 0L) {
                ids.add(v.getId());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, List<Servico>> servicosPorViagem = servicoDAO.getServicosPorViagens(db, ids);
        for (Viagem v : viagens) {
            if (v != null) {
                List<Servico> lista = servicosPorViagem.get(v.getId());
                v.hidratarServicos(lista != null ? lista : Collections.emptyList());
            }
        }
    }

    // READ - Consultas paginadas com Keyset / Seek Pagination + Range Scan

    public List<GrupoViagens> getGrupos(ViagensFiltro filtro) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<GrupoViagens> grupos = new ArrayList<>();
        List<String> args = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append("CASE WHEN ").append(DbHelper.Viagem.CLASSIFICACAO).append(" IS NOT NULL ")
                .append("AND ").append(DbHelper.Viagem.CLASSIFICACAO).append(" != '' ")
                .append("AND ").append(DbHelper.Viagem.CLASSIFICACAO).append(" != 'Comum' ")
                .append("THEN substr(COALESCE(").append(DbHelper.Viagem.DATA).append(", ''), 1, 7) || '-' || ")
                .append(DbHelper.Viagem.CLASSIFICACAO).append(" ")
                .append("ELSE substr(COALESCE(").append(DbHelper.Viagem.DATA).append(", ''), 1, 7) ")
                .append("END AS mes_ano, ")
                .append("COUNT(*) AS total_viagens ")
                .append("FROM ").append(DbHelper.Viagem.NOME_TABELA).append(" ");

        StringBuilder where = new StringBuilder();
        appendFiltroWhere(where, args, filtro);
        if (where.length() > 0) {
            sql.append("WHERE ").append(where).append(" ");
        }

        sql.append("GROUP BY mes_ano ")
                .append("ORDER BY substr(COALESCE(").append(DbHelper.Viagem.DATA).append(", ''), 1, 7) DESC, mes_ano ASC");

        try (Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor != null && cursor.moveToNext()) {
                String mesAno = cursor.getString(0);
                int total = cursor.getInt(1);
                grupos.add(new GrupoViagens(mesAno, total));
            }
        }
        return grupos;
    }

    public PaginaViagensResult getPaginaDoGrupo(
            String mesAno,
            ViagensFiltro filtro,
            ViagemCursorKey cursorKey,
            int pageSize
    ) {
        if (mesAno == null) {
            return new PaginaViagensResult(Collections.emptyList(), false, null);
        }

        int effectivePageSize = Math.max(1, pageSize);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> args = new ArrayList<>();
        StringBuilder where = new StringBuilder();

        appendGrupoWhere(where, args, mesAno);
        appendFiltroWhere(where, args, filtro);

        if (cursorKey != null) {
            String cursorData = cursorKey.getData();
            String cursorHora = cursorKey.getHora();
            String cursorId = String.valueOf(cursorKey.getId());
            if (where.length() > 0) {
                where.append(" AND ");
            }
            // Limita o teto do range scan no índice e aplica desempate determinístico (data, hora, id).
            where.append(DbHelper.Viagem.DATA).append(" <= ? AND (")
                    .append(DbHelper.Viagem.DATA).append(" < ? OR (")
                    .append(DbHelper.Viagem.DATA).append(" = ? AND ").append(DbHelper.Viagem.HORA).append(" < ?) OR (")
                    .append(DbHelper.Viagem.DATA).append(" = ? AND ").append(DbHelper.Viagem.HORA).append(" = ? AND ")
                    .append(DbHelper.Viagem.ID).append(" < ?))");
            args.add(cursorData);
            args.add(cursorData);
            args.add(cursorData);
            args.add(cursorHora);
            args.add(cursorData);
            args.add(cursorHora);
            args.add(cursorId);
        }

        String sql = "SELECT * FROM " + DbHelper.Viagem.NOME_TABELA +
                " WHERE " + where +
                " ORDER BY " + DbHelper.Viagem.DATA + " DESC, " +
                DbHelper.Viagem.HORA + " DESC, " +
                DbHelper.Viagem.ID + " DESC LIMIT ?";
        args.add(String.valueOf(effectivePageSize + 1));

        List<Viagem> encontrados = new ArrayList<>(effectivePageSize + 1);
        try (Cursor cursor = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (cursor != null && cursor.moveToNext()) {
                encontrados.add(cursorToViagem(cursor));
            }
        }

        boolean temMais = encontrados.size() > effectivePageSize;
        List<Viagem> pagina = temMais
                ? new ArrayList<>(encontrados.subList(0, effectivePageSize))
                : encontrados;
        hidratarServicos(db, pagina);

        ViagemCursorKey proximoCursor = null;
        if (temMais && !pagina.isEmpty()) {
            Viagem ultimo = pagina.get(pagina.size() - 1);
            proximoCursor = new ViagemCursorKey(ultimo.getData(), ultimo.getHora(), ultimo.getId());
        }

        return new PaginaViagensResult(pagina, temMais, proximoCursor);
    }

    public List<Viagem> getViagensDoGrupoParaPdf(String mesAno) {
        if (mesAno == null) {
            return Collections.emptyList();
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> args = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        appendGrupoWhere(where, args, mesAno);

        String sql = "SELECT * FROM " + DbHelper.Viagem.NOME_TABELA +
                " WHERE " + where +
                " ORDER BY " + DbHelper.Viagem.DATA + " ASC, " +
                DbHelper.Viagem.HORA + " ASC, " +
                DbHelper.Viagem.ID + " ASC";

        List<Viagem> lista = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(sql, args.toArray(new String[0]))) {
            while (cursor != null && cursor.moveToNext()) {
                lista.add(cursorToViagem(cursor));
            }
        }
        hidratarServicos(db, lista);
        return lista;
    }

    private void appendGrupoWhere(StringBuilder where, List<String> args, String mesAno) {
        GrupoSpec spec = parseGrupoMesAno(mesAno);
        if (spec.dataInicio != null && spec.dataFimExclusiva != null) {
            where.append(DbHelper.Viagem.DATA).append(" >= ? AND ")
                    .append(DbHelper.Viagem.DATA).append(" < ?");
            args.add(spec.dataInicio);
            args.add(spec.dataFimExclusiva);
        } else {
            where.append("COALESCE(").append(DbHelper.Viagem.DATA).append(", '') = ''");
        }

        where.append(" AND ");
        if (spec.classificacaoEspecifica != null) {
            where.append(DbHelper.Viagem.CLASSIFICACAO).append(" = ?");
            args.add(spec.classificacaoEspecifica);
        } else {
            // COALESCE evita MULTI-INDEX OR no idx_viagem_classif_data_hora_id e preserva range scan ordenado em idx_viagem_data_hora_id.
            where.append("COALESCE(").append(DbHelper.Viagem.CLASSIFICACAO).append(", '') IN ('', 'Comum')");
        }
    }

    private void appendFiltroWhere(StringBuilder where, List<String> args, ViagensFiltro filtro) {
        if (filtro == null) {
            return;
        }
        if (filtro.isSomenteEmpresas()) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(DbHelper.Viagem.CLASSIFICACAO).append(" IS NOT NULL AND ")
                    .append(DbHelper.Viagem.CLASSIFICACAO).append(" != '' AND ")
                    .append(DbHelper.Viagem.CLASSIFICACAO).append(" != 'Comum'");
        }
        if (filtro.hasTextoBusca()) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            String likePattern = "%" + ViagensFiltro.escapeLike(filtro.getTextoBusca()) + "%";
            where.append("(COALESCE(").append(DbHelper.Viagem.DESTINO).append(", '') LIKE ? ESCAPE '\\' OR ")
                    .append("COALESCE(").append(DbHelper.Viagem.ORIGEM).append(", '') LIKE ? ESCAPE '\\' OR ")
                    .append("COALESCE(").append(DbHelper.Viagem.DATA).append(", '') LIKE ? ESCAPE '\\' OR ")
                    .append("COALESCE(").append(DbHelper.Viagem.MOTORISTA).append(", '') LIKE ? ESCAPE '\\' OR ")
                    .append("COALESCE(").append(DbHelper.Viagem.DESCRICAO).append(", '') LIKE ? ESCAPE '\\' OR ")
                    .append("COALESCE(").append(DbHelper.Viagem.CLASSIFICACAO).append(", '') LIKE ? ESCAPE '\\' OR ")
                    .append("COALESCE(").append(DbHelper.Viagem.SOLICITANTE).append(", '') LIKE ? ESCAPE '\\' OR ")
                    .append("EXISTS (SELECT 1 FROM ").append(DbHelper.Servico.NOME_TABELA).append(" s WHERE s.")
                    .append(DbHelper.Servico.VIAGEM_ID).append(" = ").append(DbHelper.Viagem.NOME_TABELA).append(".").append(DbHelper.Viagem.ID)
                    .append(" AND COALESCE(s.").append(DbHelper.Servico.DESCRICAO).append(", '') LIKE ? ESCAPE '\\'))");
            for (int i = 0; i < 8; i++) {
                args.add(likePattern);
            }
        }
    }

    private GrupoSpec parseGrupoMesAno(String mesAno) {
        if (mesAno.length() >= 7 && mesAno.charAt(4) == '-') {
            try {
                YearMonth ym = YearMonth.parse(mesAno.substring(0, 7));
                String inicioMes = ym.atDay(1).toString();
                String inicioProximoMes = ym.plusMonths(1).atDay(1).toString();
                String classificacao = (mesAno.length() > 8 && mesAno.charAt(7) == '-')
                        ? mesAno.substring(8)
                        : null;
                return new GrupoSpec(inicioMes, inicioProximoMes, classificacao);
            } catch (DateTimeParseException ignored) {
                // Fallback para registros legados com data fora do padrão ISO yyyy-MM
            }
        }
        String classificacao = mesAno.startsWith("-") && mesAno.length() > 1
                ? mesAno.substring(1)
                : null;
        return new GrupoSpec(null, null, classificacao);
    }

    private static class GrupoSpec {
        final String dataInicio;
        final String dataFimExclusiva;
        final String classificacaoEspecifica;

        GrupoSpec(String dataInicio, String dataFimExclusiva, String classificacaoEspecifica) {
            this.dataInicio = dataInicio;
            this.dataFimExclusiva = dataFimExclusiva;
            this.classificacaoEspecifica = classificacaoEspecifica;
        }
    }

    // READ - Listar todos os registros (viagens)

    public List<Viagem> getAllOrdenadoPorDataDesc() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Viagem> lista = new ArrayList<>();

        try (Cursor cursor = db.query(DbHelper.Viagem.NOME_TABELA,
                null, null, null,
                null, null,
                DbHelper.Viagem.DATA + " DESC, " + DbHelper.Viagem.HORA + " DESC, " + DbHelper.Viagem.ID + " DESC")) {
            while (cursor != null && cursor.moveToNext()) {
                lista.add(cursorToViagem(cursor));
            }
        }
        hidratarServicos(db, lista);
        return lista;
    }

    public List<Viagem> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Viagem> lista = new ArrayList<>();

        try (Cursor cursor = db.query(DbHelper.Viagem.NOME_TABELA,
                null, null, null,
                null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                lista.add(cursorToViagem(cursor));
            }
        }
        hidratarServicos(db, lista);
        return lista;
    }

    // UPDATE - atualizar os registros (viagem)

    public int update(Viagem viagem) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransactionNonExclusive();
        try {
            int linhas = db.update(DbHelper.Viagem.NOME_TABELA, toContentValues(viagem),
                    DbHelper.Viagem.ID + " = ?", new String[]{String.valueOf(viagem.getId())});
            if (linhas > 0) {
                servicoDAO.substituirServicosDaViagem(db, viagem.getId(), resolverServicosParaPersistencia(viagem));
            }
            db.setTransactionSuccessful();
            return linhas;
        } finally {
            db.endTransaction();
        }
    }

    // DELETE - remover um registro do banco de dados

    public int delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DbHelper.Viagem.NOME_TABELA,
                DbHelper.Viagem.ID + " = ?", new String[]{String.valueOf(id)});
    }

    // DELETAR TODOS - remover todos os registros do banco de dados

    public int deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(DbHelper.Viagem.NOME_TABELA, null, null);
    }

    // CONTAR TODOS OS REGISTROS

    public int getCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DbHelper.Viagem.NOME_TABELA, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    // BUSCAR UM REGISTRO USANDO OUTROS DADOS

    public Viagem getViagemById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbHelper.Viagem.NOME_TABELA,
                null,
                DbHelper.Viagem.ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                Viagem v = cursorToViagem(cursor);
                hidratarServicos(db, Collections.singletonList(v));
                return v;
            }
        }
        return null;
    }

    public List<Viagem> getViagemByLocal(String local) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Viagem> lista = new ArrayList<>();

        try (Cursor cursor = db.query(DbHelper.Viagem.NOME_TABELA,
                null,
                DbHelper.Viagem.ORIGEM + " = ? OR " +
                        DbHelper.Viagem.DESTINO + " = ?",
                new String[]{local, local}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                lista.add(cursorToViagem(cursor));
            }
        }
        hidratarServicos(db, lista);
        return lista;
    }

    public List<Viagem> getViagemByDestino(String destino) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Viagem> lista = new ArrayList<>();

        try (Cursor cursor = db.query(DbHelper.Viagem.NOME_TABELA,
                null, DbHelper.Viagem.DESTINO + " = ?",
                new String[]{destino}, null, null, null)) {
            while (cursor != null && cursor.moveToNext()) {
                lista.add(cursorToViagem(cursor));
            }
        }
        hidratarServicos(db, lista);
        return lista;
    }

    public List<Viagem> getViagemByPeriodo(String anoMes) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Viagem> lista = new ArrayList<>();

        try (Cursor cursor = db.query(
                DbHelper.Viagem.NOME_TABELA,
                null,
                DbHelper.Viagem.DATA + " LIKE ?",
                new String[]{anoMes + "%"},
                null, null,
                DbHelper.Viagem.DATA + " ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                lista.add(cursorToViagem(cursor));
            }
        }
        hidratarServicos(db, lista);
        return lista;
    }

    public List<Viagem> getViagensByIntervalo(String dataInicio, String dataFim) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Viagem> lista = new ArrayList<>();

        String inicio = dataInicio != null ? dataInicio : "";
        String fim = dataFim != null ? dataFim : "";

        try (Cursor cursor = db.query(DbHelper.Viagem.NOME_TABELA,
                null, DbHelper.Viagem.DATA + " BETWEEN ? AND ?",
                new String[]{inicio, fim}, null, null,
                DbHelper.Viagem.DATA + " ASC, " + DbHelper.Viagem.HORA + " ASC, " + DbHelper.Viagem.ID + " ASC")) {
            while (cursor != null && cursor.moveToNext()) {
                lista.add(cursorToViagem(cursor));
            }
        }
        hidratarServicos(db, lista);
        return lista;
    }

    private Viagem cursorToViagem(Cursor cursor) {
        int idxSolicitante = cursor.getColumnIndex(DbHelper.Viagem.SOLICITANTE);
        String solicitante = (idxSolicitante != -1 && !cursor.isNull(idxSolicitante))
                ? cursor.getString(idxSolicitante)
                : "";

        return new Viagem(
                cursor.getLong(cursor.getColumnIndexOrThrow(DbHelper.Viagem.ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.ORIGEM)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.DATA)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.HORA)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.DESTINO)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.DESCRICAO)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.KMS_RODADOS)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.VALOR_KM)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.VALOR_SERVICO)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.MOTORISTA)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.HORA_ESPERA)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.VALOR_HORA_ESPERA)),
                cursor.getString(cursor.getColumnIndexOrThrow(DbHelper.Viagem.CLASSIFICACAO)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(DbHelper.Viagem.VALOR_TOTAL)),
                solicitante
        );
    }
}
