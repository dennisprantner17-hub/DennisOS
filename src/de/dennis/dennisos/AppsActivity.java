package de.dennis.dennisos;

import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AppsActivity extends AppBaseActivity {

    @Override protected String appTitle() { return "DennisOS Apps"; }

    @Override protected void buildApp() {
        content.addView(label("Wähle eine App", 20, Color.DKGRAY));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setRowCount(2);
        addApp(grid, "🏎\nFORMEL 1", F1Activity.class, 0, 0);
        addApp(grid, "📰\nNEWS", NewsActivity.class, 0, 1);
        addApp(grid, "✦\nHOROSKOP", HoroscopeActivity.class, 1, 0);
        addApp(grid, "♣\nLOTTO", LottoActivity.class, 1, 1);
        content.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
    }

    private void addApp(GridLayout grid, String title,
                        final Class<?> activity, int row, int column) {
        TextView tile = label(title, 25, Color.BLACK);
        tile.setGravity(Gravity.CENTER);
        tile.setBackground(border(Color.WHITE, Color.BLACK, 3));
        tile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                startActivity(new Intent(AppsActivity.this, activity));
            }
        });
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(row), GridLayout.spec(column));
        params.width = Math.max(200,
                (getResources().getDisplayMetrics().widthPixels - 80) / 2);
        params.height = 190;
        params.setMargins(10, 10, 10, 10);
        grid.addView(tile, params);
    }
}
