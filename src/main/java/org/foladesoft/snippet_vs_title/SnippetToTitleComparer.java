/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.foladesoft.snippet_vs_title;

import java.util.concurrent.Callable;
import javax.swing.tree.DefaultMutableTreeNode;
import static org.foladesoft.snippet_vs_title.WebUtils.RESPONSE_TIMEOUT;
import static org.foladesoft.snippet_vs_title.WebUtils.USER_AGENT;
import org.jsoup.Jsoup;

/**
 *
 * @author haupt_000
 */
public class SnippetToTitleComparer implements Callable<Double> {

    private String snippetTitle;
    private String link;
    private DefaultMutableTreeNode root;

    public SnippetToTitleComparer(String query, String link, DefaultMutableTreeNode root) {
        this.snippetTitle = query;
        this.link = link;
        this.root = root;
    }

    @Override
    public Double call() throws Exception {
        String actualTitle = Jsoup.connect(link).userAgent(USER_AGENT).timeout(RESPONSE_TIMEOUT).get().select("head > title").get(0).text();
        double res = compareSimple(snippetTitle, actualTitle);
        DefaultMutableTreeNode linkNode = new DefaultMutableTreeNode(res + " - " + link);
        linkNode.add(new DefaultMutableTreeNode("Snippet: " + snippetTitle));
        linkNode.add(new DefaultMutableTreeNode("Title: " + actualTitle));
        synchronized (root) {
            root.add(linkNode);
        }
        return res;
    }

    private Double compareSimple(String firstString, String secondString) {
        firstString = firstString.trim();
        // Обрезка многоточия при наличии
        if (firstString.endsWith("...")) {
            firstString = firstString.substring(0, firstString.length() - 4);
        }
        firstString = firstString.trim().toUpperCase();
        secondString = secondString.trim().toUpperCase();

//            if (snippetTitle.contains("APPLE")) {
//                System.out.println(snippetTitle);
//                System.out.println(actualTitle.toCharArray());
//            }
        if (secondString.contentEquals(firstString)) {
            return 1d;
        } else if (secondString.contains(firstString)
                || firstString.contains(secondString)) {
            return 0.5;
        } else {
            return 0d;
        }
    }

}
