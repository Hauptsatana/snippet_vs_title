/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.foladesoft.snippet_vs_title;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import static org.foladesoft.snippet_vs_title.WebUtils.GOOGLE_SEARCH_PREFIX;
import static org.foladesoft.snippet_vs_title.WebUtils.RESPONSE_TIMEOUT;
import static org.foladesoft.snippet_vs_title.WebUtils.USER_AGENT;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author haupt_000
 */
public class SearchTask implements Runnable {

    private final String queryLine;
    private final DefaultMutableTreeNode root;

    public SearchTask(String queryLine, DefaultMutableTreeNode root) {
        this.queryLine = queryLine;
        this.root = root;
    }

    private void saveHtml() throws IOException {
        final URL url = new URL(GOOGLE_SEARCH_PREFIX + URLEncoder.encode(queryLine, "UTF-8"));
        final URLConnection conn = url.openConnection();
        conn.setConnectTimeout(60000);
        conn.addRequestProperty("User-Agent", USER_AGENT);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        File outfile = File.createTempFile(queryLine + " - ", " - google.html");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile)));

        String responseLine;
        while ((responseLine = br.readLine()) != null) {
            bw.write(responseLine);
        }
    }

    @Override
    public void run() {
        try {
            ExecutorService es = Executors.newCachedThreadPool();
            DefaultMutableTreeNode queryNode = new DefaultMutableTreeNode();

            List<FutureTask<Double>> tasks = new LinkedList<>();
            // Get document via Jsoup and parse
            Document doc = Jsoup.connect(GOOGLE_SEARCH_PREFIX
                    + URLEncoder.encode(queryLine, "UTF-8")).userAgent(USER_AGENT).timeout(RESPONSE_TIMEOUT).get();
            Elements elems = doc.select("div.g");
            for (int i = 0; i < elems.size(); i++) {
                final Element snippetDiv = elems.get(i);
                Element a = snippetDiv.select("h3.r > a").get(0);
                String snippetTitle = a.text();
                String snippetLink = a.attr("href");
                // trim google-declared prefix and suffix
                snippetLink = snippetLink.substring(7, snippetLink.indexOf("&sa"));

                FutureTask<Double> task = new FutureTask<>(
                        new WebPageToQueryCompare(snippetTitle, snippetLink, queryNode));
                tasks.add(task);
                es.execute(task);
            }

            // Computing the average
            es.shutdown();
            double sum = 0;
            int successfulComps = 0;
            for (FutureTask<Double> task : tasks) {
                try {
                    sum += task.get();
                    successfulComps++;
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(Frame_Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            queryNode.setUserObject(queryLine + " - " + (sum / successfulComps));
            synchronized (root) {
                root.add(queryNode);
            }

        } catch (IOException ex) {
            Logger.getLogger(Frame_Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
