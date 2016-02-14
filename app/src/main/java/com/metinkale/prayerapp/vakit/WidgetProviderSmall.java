package com.metinkale.prayerapp.vakit;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.RemoteViews;
import com.crashlytics.android.Crashlytics;
import com.metinkale.prayer.R;
import com.metinkale.prayerapp.vakit.times.MainHelper;
import com.metinkale.prayerapp.vakit.times.Times;
import com.metinkale.prayerapp.vakit.times.Vakit;

public class WidgetProviderSmall extends AppWidgetProvider
{
    private static float SCALE_MULT = 1.5f;
    private static float mDP;

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int widgetId)
    {

        if(mDP == 0)
        {
            Resources r = context.getResources();
            mDP = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics());
        }

        Resources r = context.getResources();
        SharedPreferences widgets = context.getSharedPreferences("widgets", 0);

        int t = widgets.getInt(widgetId + "_theme", 0);
        int w = widgets.getInt(widgetId + "_width", 60);
        int h = widgets.getInt(widgetId + "_height", 60);

        w = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, w, r.getDisplayMetrics());
        h = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, h, r.getDisplayMetrics());

        int s = (int) (SCALE_MULT * Math.min(w, h));

        if(s <= 0)
        {
            SharedPreferences.Editor edit = widgets.edit();
            edit.remove(widgetId + "_width");
            edit.remove(widgetId + "_height");
            edit.commit();
            updateAppWidget(context, appWidgetManager, widgetId);
            return;
        }

        Theme theme = null;
        switch(t)
        {
            case 0:
                theme = Theme.Light;
                break;
            case 1:
                theme = Theme.Dark;
                break;
            case 2:
                theme = Theme.LightTrans;
                break;
            case 3:
                theme = Theme.Trans;
                break;
        }

        Times times = null;
        try
        {
            times = MainHelper.getTimes(widgets.getLong(widgetId + "", 0L));
        } catch(Exception ignore)
        {
        }
        if(times == null)
        {

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_city_removed);
            Intent i = new Intent(context, WidgetConfigure.class);
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            remoteViews.setOnClickPendingIntent(R.id.image, PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT));
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
            return;
        }

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.vakit_widget);

        int next = times.getNext();
        String left = times.getLeft(next, false);

        remoteViews.setOnClickPendingIntent(R.id.widget, Main.getPendingIntent(times));

        Bitmap bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bmp);
        canvas.scale(0.99f, 0.99f, s / 2, s / 2);
        Paint paint = new Paint();

        paint.setStyle(Style.FILL);
        paint.setColor(theme.bgcolor);
        canvas.drawRect(0, 0, s, s, paint);

        paint.setColor(theme.textcolor);
        paint.setStyle(Style.FILL_AND_STROKE);
        paint.setAntiAlias(true);
        paint.setSubpixelText(true);

        paint.setColor(theme.hovercolor);

        String city = times.getName();

        paint.setColor(theme.textcolor);

        float cs = s / 5;
        float ts = s * 35 / 100;
        int vs = s / 4;
        paint.setTextSize(cs);
        cs = cs * (s * 0.9f) / paint.measureText(city);
        cs = cs > vs ? vs : cs;

        paint.setTextSize(vs);
        paint.setTextAlign(Align.CENTER);
        canvas.drawText(Vakit.getByIndex(next - 1).getString(), s / 2, s * 22 / 80, paint);

        paint.setTextSize(ts);
        paint.setTextAlign(Align.CENTER);
        canvas.drawText(left, s / 2, s / 2 + ts * 1 / 3, paint);

        paint.setTextSize(cs);
        paint.setTextAlign(Align.CENTER);
        canvas.drawText(city, s / 2, s * 3 / 4 + cs * 2 / 3, paint);

        paint.setStyle(Style.STROKE);
        float stroke = 1.5f * mDP;
        paint.setStrokeWidth(stroke);
        paint.setColor(theme.strokecolor);
        canvas.drawRect(0, 0, s, s, paint);

        remoteViews.setImageViewBitmap(R.id.widget, bmp);

        try
        {
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        } catch(RuntimeException e)
        {
            Crashlytics.logException(e);
        }

    }

    @Override
    public void onEnabled(Context context)
    {
        ComponentName thisWidget = new ComponentName(context, WidgetProviderSmall.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        onUpdate(context, manager, manager.getAppWidgetIds(thisWidget));

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {

        for(int widgetId : appWidgetIds)
        {
            updateAppWidget(context, appWidgetManager, widgetId);
        }

    }

    @Override
    public void onDisabled(Context context)
    {

    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions)
    {
        int w = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int h = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        if(w * h != 0)
        {
            SharedPreferences widgets = context.getSharedPreferences("widgets", 0);
            SharedPreferences.Editor edit = widgets.edit();
            edit.putInt(appWidgetId + "_width", w);
            edit.putInt(appWidgetId + "_height", h);
            edit.apply();
        }

        updateAppWidget(context, appWidgetManager, appWidgetId);
    }
}