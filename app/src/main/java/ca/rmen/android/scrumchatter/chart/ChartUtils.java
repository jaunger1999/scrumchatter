/*
 * Copyright 2016 Carmen Alvarez
 * <p/>
 * This file is part of Scrum Chatter.
 * <p/>
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.chart;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import ca.rmen.android.scrumchatter.R;
import lecho.lib.hellocharts.model.Axis;


/**
 * Some utility methods common to the different charts.
 */
final class ChartUtils {

    private ChartUtils() {
        // prevent instantiation
    }

    static void setupXAxis(Context context, Axis xAxis) {
        xAxis.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.chart_text, null));
        xAxis.setHasTiltedLabels(true);
        xAxis.setName(context.getString(R.string.chart_date));
        xAxis.setMaxLabelChars(10);
    }

    static void setupYAxis(Context context, String yAxisLabel, Axis yAxis) {
        yAxis.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.chart_text, null));
        yAxis.setName(yAxisLabel);
        yAxis.setHasLines(true);
    }

    static void addLegendEntry(Context context, ViewGroup legendView, String name, int color) {
        TextView memberLegendEntry = new TextView(context);
        memberLegendEntry.setText(name);
        memberLegendEntry.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        memberLegendEntry.setPadding(0, 0, context.getResources().getDimensionPixelSize(R.dimen.chart_legend_entry_padding), 0);

        final Drawable icon;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_legend_square);
            memberLegendEntry.setTextColor(color);
        } else {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_legend_square).mutate();
            DrawableCompat.setTint(icon, color);
        }
        memberLegendEntry.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        legendView.addView(memberLegendEntry);
    }

    static @ColorInt
    int getMemberColor(Context context, long memberId) {
        String[] colors = context.getResources().getStringArray(R.array.chart_colors);
        String colorString = colors[(int) memberId % colors.length];
        @ColorInt int color = Color.parseColor(colorString);
        return color;
    }
}
