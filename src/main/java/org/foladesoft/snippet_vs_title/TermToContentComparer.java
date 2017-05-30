/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.foladesoft.snippet_vs_title;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.tree.DefaultMutableTreeNode;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import static org.foladesoft.snippet_vs_title.WebUtils.RESPONSE_TIMEOUT;
import static org.foladesoft.snippet_vs_title.WebUtils.USER_AGENT;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 *
 * @author Vladislav
 */
public class TermToContentComparer implements Runnable {

    private String term;
    private String link;
    private DefaultMutableTreeNode root;

    public TermToContentComparer(String term, String link, DefaultMutableTreeNode root) {
        this.term = term;
        this.link = link;
        this.root = root;
    }

    @Override
    public void run() {
        try {

            Document doc = Jsoup.connect(link).userAgent(USER_AGENT).timeout(RESPONSE_TIMEOUT).get();

            // h1 - h6 
            Map<String, List<String>> hmap = new LinkedHashMap<>();
            for (int i = 1; i <= 6; i++) {
                List<String> chs = doc.select("h" + i).stream()
                        .map((Element t) -> t.text()).collect(Collectors.toList());
                if (!chs.isEmpty()) {
                    hmap.put("h" + i, chs);
                }
            }
            // p
            String ps = doc.select("p").stream().map((Element t) -> t.text())
                    .reduce("", (String t, String u) -> t + " " + u);

            DefaultMutableTreeNode pageNode = new DefaultMutableTreeNode(link);

            // headers
            if (!hmap.isEmpty()) {
                DefaultMutableTreeNode hNode = new DefaultMutableTreeNode("h1 - h6");
                pageNode.add(hNode);
                for (Map.Entry<String, List<String>> hme : hmap.entrySet()) {
                    for (String hval : hme.getValue()) {
                        int perc = FuzzySearch.ratio(term, hval);
                        hNode.add(new DefaultMutableTreeNode(perc + "% - " + hme.getKey() + ": " + hval));
                    }
                }
            }
            // p
            int perc = FuzzySearch.ratio(term, ps);
            pageNode.add(new DefaultMutableTreeNode(perc + "% - p : " + ps));

            synchronized (root) {
                root.add(pageNode);
            }

        } catch (IOException ex) {
            Logger.getLogger(TermToContentComparer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
