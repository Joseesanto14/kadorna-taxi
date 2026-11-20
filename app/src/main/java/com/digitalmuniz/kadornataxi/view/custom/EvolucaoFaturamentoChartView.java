package com.digitalmuniz.kadornataxi.view.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.digitalmuniz.kadornataxi.R;
import com.digitalmuniz.kadornataxi.util.FormatadorDashboard;
import com.digitalmuniz.kadornataxi.view.dto.FaturamentoMensalDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EvolucaoFaturamentoChartView extends View {

    public enum ModoExibicao {
        RECEITA_E_CUSTOS,
        LUCRO_LIQUIDO
    }

    public interface OnMesSelecionadoListener {
        void onMesSelecionado(String mesNumero);
    }

    private static final int TOTAL_MESES = 12;

    private final Paint barAtivaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barInativaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint costBarAtivaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint costBarInativaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barVaziaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mediaLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelMesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelMesAtivoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valorTopoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valorTopoNegativoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baseLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path barPath = new Path();
    private final RectF barRect = new RectF();
    private final float[] topRoundedRadii = new float[8];
    private final float[] bottomRoundedRadii = new float[8];
    private final RectF[] hitZones = new RectF[TOTAL_MESES];

    private final List<FaturamentoMensalDTO> meses = new ArrayList<>(TOTAL_MESES);
    private ModoExibicao modoExibicao = ModoExibicao.RECEITA_E_CUSTOS;
    private String mesSelecionado;
    private double maxValorPositivo;
    private double maxValorNegativo;
    private double mediaMensal;
    private int peakIndex = -1;

    private OnMesSelecionadoListener onMesSelecionadoListener;
    private final int touchSlop;
    private float downX;
    private float downY;

    private final float density;

    public EvolucaoFaturamentoChartView(Context context) {
        this(context, null);
    }

    public EvolucaoFaturamentoChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EvolucaoFaturamentoChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        for (int i = 0; i < TOTAL_MESES; i++) {
            hitZones[i] = new RectF();
        }

        float cornerRadius = 5f * density;
        topRoundedRadii[0] = cornerRadius;
        topRoundedRadii[1] = cornerRadius;
        topRoundedRadii[2] = cornerRadius;
        topRoundedRadii[3] = cornerRadius;
        topRoundedRadii[4] = 0f;
        topRoundedRadii[5] = 0f;
        topRoundedRadii[6] = 0f;
        topRoundedRadii[7] = 0f;

        bottomRoundedRadii[0] = 0f;
        bottomRoundedRadii[1] = 0f;
        bottomRoundedRadii[2] = 0f;
        bottomRoundedRadii[3] = 0f;
        bottomRoundedRadii[4] = cornerRadius;
        bottomRoundedRadii[5] = cornerRadius;
        bottomRoundedRadii[6] = cornerRadius;
        bottomRoundedRadii[7] = cornerRadius;

        barAtivaPaint.setColor(ContextCompat.getColor(context, R.color.brand_green_dark));
        barAtivaPaint.setStyle(Paint.Style.FILL);

        barInativaPaint.setColor(ContextCompat.getColor(context, R.color.brand_green_bar_inactive));
        barInativaPaint.setStyle(Paint.Style.FILL);

        costBarAtivaPaint.setColor(ContextCompat.getColor(context, R.color.delta_negative));
        costBarAtivaPaint.setStyle(Paint.Style.FILL);

        costBarInativaPaint.setColor(ContextCompat.getColor(context, R.color.cost_bar_inactive));
        costBarInativaPaint.setStyle(Paint.Style.FILL);

        barVaziaPaint.setColor(ContextCompat.getColor(context, R.color.border_subtle));
        barVaziaPaint.setStyle(Paint.Style.FILL);

        mediaLinePaint.setColor(ContextCompat.getColor(context, R.color.chart_average_line));
        mediaLinePaint.setStyle(Paint.Style.STROKE);
        mediaLinePaint.setStrokeWidth(1.5f * density);
        mediaLinePaint.setPathEffect(new DashPathEffect(new float[]{6f * density, 4f * density}, 0f));

        baseLinePaint.setColor(ContextCompat.getColor(context, R.color.border_subtle));
        baseLinePaint.setStyle(Paint.Style.STROKE);
        baseLinePaint.setStrokeWidth(1f * density);

        float sp = getResources().getDisplayMetrics().scaledDensity;

        labelMesPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        labelMesPaint.setTextSize(10.5f * sp);
        labelMesPaint.setTextAlign(Paint.Align.CENTER);

        labelMesAtivoPaint.setColor(ContextCompat.getColor(context, R.color.brand_green_dark));
        labelMesAtivoPaint.setTextSize(10.5f * sp);
        labelMesAtivoPaint.setFakeBoldText(true);
        labelMesAtivoPaint.setTextAlign(Paint.Align.CENTER);

        valorTopoPaint.setColor(ContextCompat.getColor(context, R.color.brand_green_dark));
        valorTopoPaint.setTextSize(10f * sp);
        valorTopoPaint.setFakeBoldText(true);
        valorTopoPaint.setTextAlign(Paint.Align.CENTER);

        valorTopoNegativoPaint.setColor(ContextCompat.getColor(context, R.color.delta_negative));
        valorTopoNegativoPaint.setTextSize(10f * sp);
        valorTopoNegativoPaint.setFakeBoldText(true);
        valorTopoNegativoPaint.setTextAlign(Paint.Align.CENTER);

        inicializarMesesVazios();
        setClickable(true);
    }

    private void inicializarMesesVazios() {
        meses.clear();
        for (int m = 1; m <= TOTAL_MESES; m++) {
            String numero = String.format(Locale.US, "%02d", m);
            meses.add(new FaturamentoMensalDTO(numero, FormatadorDashboard.getSiglaMes(numero), 0, 0.0, 0.0, 0.0, 0.0));
        }
    }

    public void setDados(@Nullable List<FaturamentoMensalDTO> evolucao, @Nullable String mesSelecionado) {
        this.mesSelecionado = (mesSelecionado != null && !mesSelecionado.trim().isEmpty())
                ? mesSelecionado.trim()
                : null;

        inicializarMesesVazios();
        if (evolucao != null) {
            for (FaturamentoMensalDTO item : evolucao) {
                if (item == null || item.getMesNumero() == null) {
                    continue;
                }
                try {
                    int idx = Integer.parseInt(item.getMesNumero()) - 1;
                    if (idx >= 0 && idx < TOTAL_MESES) {
                        meses.set(idx, item);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        recalcularMetricas();
        invalidate();
    }

    public void setModoExibicao(@NonNull ModoExibicao modo) {
        if (this.modoExibicao != modo) {
            this.modoExibicao = modo;
            recalcularMetricas();
            invalidate();
        }
    }

    @NonNull
    public ModoExibicao getModoExibicao() {
        return modoExibicao;
    }

    private void recalcularMetricas() {
        if (modoExibicao == ModoExibicao.LUCRO_LIQUIDO) {
            double maxPos = 0.0;
            double maxNeg = 0.0;
            double somaAtivos = 0.0;
            int mesesComAtividade = 0;
            int idxMaior = -1;
            double maiorMagnitude = 0.0;

            for (int i = 0; i < TOTAL_MESES; i++) {
                FaturamentoMensalDTO item = meses.get(i);
                double lucro = item.getLucroLiquido();
                if (lucro > maxPos) {
                    maxPos = lucro;
                }
                if (lucro < 0.0 && -lucro > maxNeg) {
                    maxNeg = -lucro;
                }
                double mag = Math.abs(lucro);
                if (mag > maiorMagnitude) {
                    maiorMagnitude = mag;
                    idxMaior = i;
                }
                if (item.getFaturamentoTotal() > 0.0 || item.getTotalCustos() > 0.0) {
                    somaAtivos += lucro;
                    mesesComAtividade++;
                }
            }

            this.maxValorPositivo = maxPos;
            this.maxValorNegativo = maxNeg;
            this.peakIndex = idxMaior;
            this.mediaMensal = mesesComAtividade > 0 ? somaAtivos / mesesComAtividade : 0.0;
            return;
        }

        double max = 0.0;
        double somaReceitaPositiva = 0.0;
        int mesesComReceita = 0;
        int idxMaiorReceita = -1;
        double maiorReceita = 0.0;
        int idxMaiorCusto = -1;
        double maiorCusto = 0.0;

        for (int i = 0; i < TOTAL_MESES; i++) {
            FaturamentoMensalDTO item = meses.get(i);
            double receita = item.getFaturamentoTotal();
            double custo = item.getTotalCustos();
            double maiorPar = Math.max(receita, custo);
            if (maiorPar > max) {
                max = maiorPar;
            }
            if (receita > maiorReceita) {
                maiorReceita = receita;
                idxMaiorReceita = i;
            }
            if (custo > maiorCusto) {
                maiorCusto = custo;
                idxMaiorCusto = i;
            }
            if (receita > 0.0) {
                somaReceitaPositiva += receita;
                mesesComReceita++;
            }
        }

        this.maxValorPositivo = max;
        this.maxValorNegativo = 0.0;
        this.peakIndex = idxMaiorReceita >= 0 ? idxMaiorReceita : idxMaiorCusto;
        this.mediaMensal = mesesComReceita > 0 ? somaReceitaPositiva / mesesComReceita : 0.0;
    }

    public void setOnMesSelecionadoListener(@Nullable OnMesSelecionadoListener listener) {
        this.onMesSelecionadoListener = listener;
    }

    public double getMediaMensalCalculada() {
        return mediaMensal;
    }

    @Nullable
    public String getMesSelecionado() {
        return mesSelecionado;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = Math.round(210f * density);
        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float padLeft = getPaddingLeft() + 8f * density;
        float padRight = getPaddingRight() + 8f * density;
        float padTop = getPaddingTop() + 22f * density;
        boolean temNegativos = modoExibicao == ModoExibicao.LUCRO_LIQUIDO && maxValorNegativo > 0.0;
        float padBottom = getPaddingBottom() + (temNegativos ? 34f : 24f) * density;

        float chartWidth = width - padLeft - padRight;
        float chartBottom = height - padBottom;
        float chartHeight = chartBottom - padTop;
        if (chartWidth <= 0f || chartHeight <= 0f) {
            return;
        }

        if (modoExibicao == ModoExibicao.LUCRO_LIQUIDO) {
            desenharModoLucroLiquido(canvas, width, height, padLeft, padRight, padTop, chartWidth, chartBottom, chartHeight);
        } else {
            desenharModoReceitaECustos(canvas, width, height, padLeft, padRight, chartWidth, chartBottom, chartHeight);
        }
    }

    private void desenharModoReceitaECustos(
            Canvas canvas,
            int width,
            int height,
            float padLeft,
            float padRight,
            float chartWidth,
            float chartBottom,
            float chartHeight
    ) {
        float slotWidth = chartWidth / TOTAL_MESES;
        float pairGap = 2f * density;
        float singleBarWidth = Math.min(11f * density, Math.max(4f * density, (slotWidth - pairGap) * 0.42f));
        float minBarHeight = 3f * density;

        canvas.drawLine(padLeft, chartBottom, width - padRight, chartBottom, baseLinePaint);

        if (maxValorPositivo > 0.0 && mediaMensal > 0.0) {
            float mediaRatio = (float) Math.min(1.0, mediaMensal / maxValorPositivo);
            float mediaY = chartBottom - (mediaRatio * chartHeight);
            canvas.drawLine(padLeft, mediaY, width - padRight, mediaY, mediaLinePaint);
        }

        for (int i = 0; i < TOTAL_MESES; i++) {
            FaturamentoMensalDTO item = meses.get(i);
            float slotLeft = padLeft + i * slotWidth;
            float slotRight = slotLeft + slotWidth;
            float centerX = slotLeft + slotWidth * 0.5f;

            hitZones[i].set(slotLeft, 0f, slotRight, height);

            double receita = Math.max(0.0, item.getFaturamentoTotal());
            double custo = Math.max(0.0, item.getTotalCustos());
            boolean temDados = receita > 0.0 || custo > 0.0;
            boolean isAtivo = mesSelecionado != null
                    ? item.getMesNumero().equals(mesSelecionado)
                    : (i == peakIndex && temDados);

            float receitaH = (maxValorPositivo > 0.0 && receita > 0.0)
                    ? Math.max(minBarHeight, (float) ((receita / maxValorPositivo) * chartHeight))
                    : minBarHeight;
            float custoH = (maxValorPositivo > 0.0 && custo > 0.0)
                    ? Math.max(minBarHeight, (float) ((custo / maxValorPositivo) * chartHeight))
                    : minBarHeight;

            float receitaLeft = centerX - (pairGap * 0.5f) - singleBarWidth;
            float receitaRight = centerX - (pairGap * 0.5f);
            float receitaTop = chartBottom - receitaH;
            desenharBarraArredondada(canvas, receitaLeft, receitaTop, receitaRight, chartBottom,
                    topRoundedRadii, receita > 0.0 ? (isAtivo ? barAtivaPaint : barInativaPaint) : (isAtivo ? barAtivaPaint : barVaziaPaint));

            float custoLeft = centerX + (pairGap * 0.5f);
            float custoRight = custoLeft + singleBarWidth;
            float custoTop = chartBottom - custoH;
            desenharBarraArredondada(canvas, custoLeft, custoTop, custoRight, chartBottom,
                    topRoundedRadii, custo > 0.0 ? (isAtivo ? costBarAtivaPaint : costBarInativaPaint) : (isAtivo ? costBarAtivaPaint : barVaziaPaint));

            if (isAtivo && temDados) {
                float topMaisAlto = Math.min(receitaTop, custoTop);
                double valorDestaque = receita > 0.0 ? receita : custo;
                Paint paintTexto = receita > 0.0 ? valorTopoPaint : valorTopoNegativoPaint;
                String textoValor = FormatadorDashboard.abreviarParaGrafico(valorDestaque);
                float textX = Math.max(padLeft + 16f * density, Math.min(width - padRight - 16f * density, centerX));
                float textY = Math.max(12f * density, topMaisAlto - 6f * density);
                canvas.drawText(textoValor, textX, textY, paintTexto);
            }

            desenharLabelMes(canvas, item, centerX, chartBottom + 16f * density, isAtivo);
        }
    }

    private void desenharModoLucroLiquido(
            Canvas canvas,
            int width,
            int height,
            float padLeft,
            float padRight,
            float padTop,
            float chartWidth,
            float chartBottom,
            float chartHeight
    ) {
        float slotWidth = chartWidth / TOTAL_MESES;
        float barWidth = Math.min(20f * density, slotWidth * 0.62f);
        float minBarHeight = 3f * density;

        double totalRange = maxValorPositivo + maxValorNegativo;
        float zeroY;
        if (maxValorNegativo <= 0.0) {
            zeroY = chartBottom;
        } else if (maxValorPositivo <= 0.0) {
            zeroY = padTop;
        } else {
            zeroY = padTop + (float) ((maxValorPositivo / totalRange) * chartHeight);
        }

        canvas.drawLine(padLeft, zeroY, width - padRight, zeroY, baseLinePaint);

        if (totalRange > 0.0 && Math.abs(mediaMensal) > 0.0) {
            float mediaY = zeroY - (float) ((mediaMensal / totalRange) * chartHeight);
            mediaY = Math.max(padTop, Math.min(chartBottom, mediaY));
            canvas.drawLine(padLeft, mediaY, width - padRight, mediaY, mediaLinePaint);
        }

        for (int i = 0; i < TOTAL_MESES; i++) {
            FaturamentoMensalDTO item = meses.get(i);
            float slotLeft = padLeft + i * slotWidth;
            float slotRight = slotLeft + slotWidth;
            float centerX = slotLeft + slotWidth * 0.5f;

            hitZones[i].set(slotLeft, 0f, slotRight, height);

            double lucro = item.getLucroLiquido();
            boolean temDados = item.getFaturamentoTotal() > 0.0 || item.getTotalCustos() > 0.0;
            boolean isAtivo = mesSelecionado != null
                    ? item.getMesNumero().equals(mesSelecionado)
                    : (i == peakIndex && temDados);

            float barLeft = centerX - barWidth * 0.5f;
            float barRight = centerX + barWidth * 0.5f;

            if (lucro > 0.0 && totalRange > 0.0) {
                float h = Math.max(minBarHeight, (float) ((lucro / totalRange) * chartHeight));
                float barTop = Math.max(padTop, zeroY - h);
                desenharBarraArredondada(canvas, barLeft, barTop, barRight, zeroY,
                        topRoundedRadii, isAtivo ? barAtivaPaint : barInativaPaint);
                if (isAtivo) {
                    String texto = FormatadorDashboard.abreviarParaGraficoComSinal(lucro);
                    float textX = Math.max(padLeft + 16f * density, Math.min(width - padRight - 16f * density, centerX));
                    float textY = Math.max(12f * density, barTop - 6f * density);
                    canvas.drawText(texto, textX, textY, valorTopoPaint);
                }
            } else if (lucro < 0.0 && totalRange > 0.0) {
                float h = Math.max(minBarHeight, (float) ((-lucro / totalRange) * chartHeight));
                float barBottom = Math.min(chartBottom, zeroY + h);
                desenharBarraArredondada(canvas, barLeft, zeroY, barRight, barBottom,
                        bottomRoundedRadii, isAtivo ? costBarAtivaPaint : costBarInativaPaint);
                if (isAtivo) {
                    String texto = FormatadorDashboard.abreviarParaGraficoComSinal(lucro);
                    float textX = Math.max(padLeft + 16f * density, Math.min(width - padRight - 16f * density, centerX));
                    float textY = Math.min(height - 20f * density, barBottom + 12f * density);
                    canvas.drawText(texto, textX, textY, valorTopoNegativoPaint);
                }
            } else {
                float barTop = Math.max(padTop, zeroY - minBarHeight);
                desenharBarraArredondada(canvas, barLeft, barTop, barRight, zeroY,
                        topRoundedRadii, isAtivo ? barAtivaPaint : barVaziaPaint);
            }

            desenharLabelMes(canvas, item, centerX, height - 6f * density, isAtivo);
        }
    }

    private void desenharBarraArredondada(
            Canvas canvas,
            float left,
            float top,
            float right,
            float bottom,
            float[] radii,
            Paint paint
    ) {
        if (bottom <= top) {
            return;
        }
        barRect.set(left, top, right, bottom);
        barPath.reset();
        barPath.addRoundRect(barRect, radii, Path.Direction.CW);
        canvas.drawPath(barPath, paint);
    }

    private void desenharLabelMes(Canvas canvas, FaturamentoMensalDTO item, float x, float y, boolean isAtivo) {
        String sigla = item.getMesSigla();
        if (sigla != null && !sigla.isEmpty()) {
            canvas.drawText(sigla, x, y, isAtivo ? labelMesAtivoPaint : labelMesPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                float dx = Math.abs(event.getX() - downX);
                float dy = Math.abs(event.getY() - downY);
                if (dx <= touchSlop && dy <= touchSlop) {
                    performClick();
                    int index = encontrarMesNaPosicao(event.getX());
                    if (index >= 0 && index < meses.size() && onMesSelecionadoListener != null) {
                        onMesSelecionadoListener.onMesSelecionado(meses.get(index).getMesNumero());
                    }
                }
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int encontrarMesNaPosicao(float x) {
        for (int i = 0; i < TOTAL_MESES; i++) {
            if (x >= hitZones[i].left && x <= hitZones[i].right) {
                return i;
            }
        }
        return -1;
    }
}
