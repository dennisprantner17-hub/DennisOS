package de.dennis.dennisos;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;

public class NewsActivity extends AppBaseActivity {

    private static class NewsItem {
        String title = "";
        String link = "";
        String description = "";
        String date = "";
    }

    @Override protected String appTitle() { return "News"; }

    @Override protected void buildApp() {
        showCategories();
        loadFeed("Top-News", "https://rss.orf.at/news.xml");
    }

    private void showCategories() {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        addCategory(tabs, "Top", "https://rss.orf.at/news.xml");
        addCategory(tabs, "Österreich", "https://rss.orf.at/oesterreich.xml");
        addCategory(tabs, "Technik", "https://rss.orf.at/science.xml");
        addCategory(tabs, "Sport", "https://rss.orf.at/sport.xml");
        content.addView(tabs);
    }

    private void addCategory(LinearLayout row, final String title, final String url) {
        TextView button = actionButton(title);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { loadFeed(title, url); }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 58, 1);
        params.setMargins(3, 0, 3, 0);
        row.addView(button, params);
    }

    private void loadFeed(final String title, String url) {
        while (content.getChildCount() > 1) content.removeViewAt(1);
        content.addView(sectionTitle(title));
        showLoading("Nachrichten werden geladen …");
        loadText(url, new TextCallback() {
            @Override public void finished(String text) {
                if (content.getChildCount() > 2) content.removeViewAt(2);
                try {
                    ArrayList<NewsItem> items = parseFeed(text);
                    if (items.size() == 0) throw new Exception("leer");
                    for (int i = 0; i < items.size() && i < 20; i++) addNews(items.get(i), i);
                } catch (Exception error) {
                    content.addView(label("Nachrichten konnten nicht gelesen werden.", 18, Color.DKGRAY));
                }
            }
            @Override public void failed(Exception error) {
                if (content.getChildCount() > 2) content.removeViewAt(2);
                content.addView(label("News momentan nicht verfügbar.", 18, Color.DKGRAY));
            }
        });
    }

    private ArrayList<NewsItem> parseFeed(String xml) throws Exception {
        ArrayList<NewsItem> result = new ArrayList<NewsItem>();
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(new StringReader(xml));
        NewsItem current = null;
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (event == XmlPullParser.START_TAG && "item".equalsIgnoreCase(name)) {
                current = new NewsItem();
            } else if (event == XmlPullParser.START_TAG && current != null) {
                if ("title".equalsIgnoreCase(name)) current.title = parser.nextText();
                else if ("link".equalsIgnoreCase(name)) current.link = parser.nextText();
                else if ("description".equalsIgnoreCase(name)) current.description = clean(parser.nextText());
                else if ("date".equalsIgnoreCase(name)) current.date = parser.nextText();
            } else if (event == XmlPullParser.END_TAG && "item".equalsIgnoreCase(name)
                    && current != null) {
                result.add(current);
                current = null;
            }
            event = parser.next();
        }
        return result;
    }

    private void addNews(final NewsItem item, int index) {
        String preview = item.title;
        if (item.description.length() > 0) preview += "\n" + item.description;
        TextView card = label(preview, 18, Color.BLACK);
        card.setBackground(border(index % 2 == 0 ? Color.WHITE : Color.rgb(242,242,242), Color.LTGRAY, 1));
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showDetail(item); }
        });
        content.addView(card);
    }

    private void showDetail(final NewsItem item) {
        content.removeAllViews();
        TextView back = actionButton("‹ Zurück zu den News");
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { refreshApp(); }
        });
        content.addView(back, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 60));
        content.addView(label(item.title, 28, Color.BLACK));
        if (item.date.length() > 0) content.addView(label(item.date, 14, Color.DKGRAY));
        content.addView(label(item.description.length() > 0 ? item.description
                : "Der RSS-Feed enthält zu dieser Meldung nur die Überschrift.", 20, Color.BLACK));
        final TextView original = actionButton("Originalartikel öffnen");
        original.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (item.link.length() > 0) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(item.link)));
            }
        });
        content.addView(original, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 64));
    }

    private String clean(String value) {
        return value.replaceAll("<[^>]+>", " ")
                .replace("&amp;", "&").replace("&quot;", "\"")
                .replaceAll("\\s+", " ").trim();
    }
}
