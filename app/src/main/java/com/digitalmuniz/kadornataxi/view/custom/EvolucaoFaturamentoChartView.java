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

    public interface OnMesSelecionadoListener {
        void onMesSelecionado(String mesNumero);
    }

    private static final int TOTAL_MESES = 12;

    private final Paint barAtivaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barInativaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barVaziaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mediaLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelMesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelMesAtivoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valorTopoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baseLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path barPath = new Path();
    private final RectF barRect = new RectF();
    private final float[] topRoundedRadii = new float[8];
    private final RectF[] hitZones = new RectF[TOTAL_MESES];

    private final List<FaturamentoMensalDTO> meses = new ArrayList<>(TOTAL_MESES);
    private String mesSelecionado;
    private double maxValor;
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

        float cornerRadius = 6f * density;
        topRoundedRadii[0] = cornerRadius;
        topRoundedRadii[1] = cornerRadius;
        topRoundedRadii[2] = cornerRadius;
        topRoundedRadii[3] = cornerRadius;
        topRoundedRadii[4] = 0f;
        topRoundedRadii[5] = 0f;
        topRoundedRadii[6] = 0f;
        topRoundedRadii[7] = 0f;

        barAtivaPaint.setColor(ContextCompat.getColor(context, R.color.brand_green_dark));
        barAtivaPaint.setStyle(Paint.Style.FILL);

        barInativaPaint.setColor(ContextCompat.getColor(context, R.color.brand_green_bar_inactive));
        barInativaPaint.setStyle(Paint.Style.FILL);

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

        inicializarMesesVazios();
        setClickable(true);
    }

    private void inicializarMesesVazios() {
        meses.clear();
        for (int m = 1; m <= TOTAL_MESES; m++) {
            String numero = String.format(Locale.US, "%02d", m);
            meses.add(new FaturamentoMensalDTO(numero, FormatadorDashboard.getSiglaMes(numero), 0, 0.0, 0.0));
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

        double max = 0.0;
        double somaPositivos = 0.0;
        int mesesComDados = 0;
        int idxMaior = -1;

        for (int i = 0; i < TOTAL_MESES; i++) {
            double valor = meses.get(i).getFaturamentoTotal();
            if (valor > max) {
                max = valor;
                idxMaior = i;
            }
            if (valor > 0.0) {
                somaPositivos += valor;
                mesesComDados++;
            }
        }

        this.maxValor = max;
        this.peakIndex = idxMaior;
        this.mediaMensal = mesesComDados > 0 ? somaPositivos / mesesComDados : 0.0;
        invalidate();
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
        float padBottom = getPaddingBottom() + 24f * density;

        float chartWidth = width - padLeft - padRight;
        float chartBottom = height - padBottom;
        float chartHeight = chartBottom - padTop;
        if (chartWidth <= 0f || chartHeight <= 0f) {
            return;
        }

        float slotWidth = chartWidth / TOTAL_MESES;
        float barWidth = Math.min(20f * density, slotWidth * 0.62f);
        float minBarHeight = 3f * density;

        canvas.drawLine(padLeft, chartBottom, width - padRight, chartBottom, baseLinePaint);

        if (maxValor > 0.0 && mediaMensal > 0.0) {
            float mediaRatio = (float) Math.min(1.0, mediaMensal / maxValor);
            float mediaY = chartBottom - (mediaRatio * chartHeight);
            canvas.drawLine(padLeft, mediaY, width - padRight, mediaY, mediaLinePaint);
        }

        for (int i = 0; i < TOTAL_MESES; i++) {
            FaturamentoMensalDTO item = meses.get(i);
            float slotLeft = padLeft + i * slotWidth;
            float slotRight = slotLeft + slotWidth;
            float centerX = slotLeft + slotWidth * 0.5f;

            hitZones[i].set(slotLeft, 0f, slotRight, height);

            double valor = item.getFaturamentoTotal();
            boolean isAtivo = mesSelecionado != null
                    ? item.getMesNumero().equals(mesSelecionado)
                    : (i == peakIndex && valor > 0.0);

            float barHeight;
            if (maxValor > 0.0 && valor > 0.0) {
                barHeight = Math.max(minBarHeight, (float) ((valor / maxValor) * chartHeight));
            } else {
                barHeight = minBarHeight;
            }

            float barLeft = centerX - barWidth * 0.5f;
            float barRight = centerX + barWidth * 0.5f;
            float barTop = chartBottom - barHeight;

            barRect.set(barLeft, barTop, barRight, chartBottom);
            barPath.reset();
            barPath.addRoundRect(barRect, topRoundedRadii, Path.Direction.CW);

            Paint paintBarra;
            if (valor <= 0.0) {
                paintBarra = isAtivo ? barAtivaPaint : barVaziaPaint;
            } else {
                paintBarra = isAtivo ? barAtivaPaint : barInativaPaint;
            }
            canvas.drawPath(barPath, paintBarra);

            if (isAtivo && valor > 0.0) {
                String textoValor = FormatadorDashboard.abreviarParaGrafico(valor);
                float textX = Math.max(padLeft + 16f * density, Math.min(width - padRight - 16f * density, centerX));
                float textY = Math.max(12f * density, barTop - 6f * density);
                canvas.drawText(textoValor, textX, textY, valorTopoPaint);
            }

            String sigla = item.getMesSigla();
            if (sigla != null && !sigla.isEmpty()) {
                float labelY = chartBottom + 16f * density;
                canvas.drawText(sigla, centerX, labelY, isAtivo ? labelMesAtivoPaint : labelMesPaint);
            }
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
