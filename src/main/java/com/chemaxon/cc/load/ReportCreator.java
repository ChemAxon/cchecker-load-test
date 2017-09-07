/*
 * Copyright 2017 ChemAxon Ltd. https://ww.chemaxon.com/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.chemaxon.cc.load;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Precision;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.google.gson.Gson;

public class ReportCreator {

    private final List<List<RunTimeLog>> logs;
    private final int plannedMolCount;
    private final int finishedMolCount;
    private final Instant start;
    private final Instant end;
    private final AtomicInteger ai = new AtomicInteger();
    private final boolean saveInput;

    public ReportCreator(List<List<RunTimeLog>> logs, int plannedMolCount, int finishedMolCount, Instant start,
            Instant end, boolean saveInput) {
        this.logs = Collections
                .unmodifiableList(logs.stream().map(l -> Collections.unmodifiableList(l)).collect(Collectors.toList()));
        this.plannedMolCount = plannedMolCount;
        this.finishedMolCount = finishedMolCount;
        this.start = start;
        this.end = end;
        this.saveInput = saveInput;
    }

    public void saveHtmlReport(File file) throws IOException {
        Document doc = getDocument();
        try (PrintWriter pw = new PrintWriter(file)) {
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setXHTML(true);
            format.setTrimText(false);
            XMLWriter xw = new XMLWriter(pw, format);
            xw.write(doc);
        }
    }

    private Document getDocument() {
        Document main = DocumentHelper.createDocument();
        Element html = main.addElement("html");
        Element head = html.addElement("head");
        head.addElement("title").addText("Report");
        head.addElement("link").addAttribute("rel", "stylesheet").addAttribute("type", "text/css").addAttribute("href",
                "https://cdn.datatables.net/1.10.15/css/jquery.dataTables.css");
        head.addElement("script").addAttribute("integrity", "sha256-ZosEbRLbNQzLpnKIkEdrPv7lOy9C27hHQ+Xp8a4MxAQ=")
                .addAttribute("type", "text/javascript")
                .addAttribute("src", "https://code.jquery.com/jquery-1.12.4.min.js")
                .addAttribute("crossorigin", "anonymous").addText(" ");
        head.addElement("script").addAttribute("charset", "utf8").addAttribute("type", "text/javascript")
                .addAttribute("src", "https://cdn.datatables.net/1.10.15/js/jquery.dataTables.min.js").addText(" ");
        head.addElement("script").addAttribute("integrity", "sha256-VNbX9NjQNRW+Bk02G/RO6WiTKuhncWI4Ey7LkSbE+5s=")
                .addAttribute("type", "text/javascript")
                .addAttribute("src", "https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.6.0/Chart.bundle.min.js")
                .addAttribute("crossorigin", "anonymous").addText(" ");
        Element body = html.addElement("body");
        writeSummary(body);
        writeTables(body);
        addTableScript("threads", body);
        logs.forEach(ls -> {
            if (!ls.isEmpty()) {
                addTableScript(ls.get(0).getThreadName(), body);
            }
        });
        return main;
    }

    private void writeSummary(Element root) {
        root.addElement("h1").addText("Summary");
        if (plannedMolCount != finishedMolCount) {
            root.addElement("h1").addText("ERROR DURING CHECK!").addAttribute("style", "color:red;");
        } else {
            root.addElement("h1").addText("Everything is good").addAttribute("style", "color:green;");
        }
        Map<String, Double> summary = new LinkedHashMap<>();
        summary.put("planned search", 0.0 + plannedMolCount);
        summary.put("executed search", 0.0 + finishedMolCount);
        summary.put("%", (((double) finishedMolCount) / plannedMolCount) * 100.0);
        summary.put("took (seconds)", ((double) Duration.between(start, end).toMillis()) / 1000.0);
        summary.put("mol/second",
                ((double) finishedMolCount) / (((double) Duration.between(start, end).toMillis()) / 1000.0));
        writeKeyValueTable(root, summary);
    }

    private void addTableScript(String id, Element root) {
        root.addElement("script")
                .addText("$(document).ready(function(){\n" + "    $('#" + id + "').DataTable();\n" + "});");
    }

    private void writeTables(Element body) {
        List<RunTimeLog> all = logs.stream().flatMap(l -> l.stream())
                .sorted((left, rigth) -> left.getDuration().compareTo(rigth.getDuration()))
                .collect(Collectors.toList());
        Element threadsDiv = body.addElement("div");
        threadsDiv.addAttribute("stlye", "padding:20px;margin:20px;");
        threadsDiv.addElement("h1").addText("All");
        createTable("threads", all, threadsDiv);
        createCanvas("threads_cv", all, threadsDiv, "Runtimes");
        createCanvas("threads_cv2", all, threadsDiv, "Distribution");
        addCanvasScript("threads_cv", all, threadsDiv);
        addGausCanvasScript("threads_cv2", all, threadsDiv);
        logs.forEach(ls -> {
            if (!ls.isEmpty()) {
                Element subDiv = body.addElement("div").addAttribute("style", "margin:20px; padding:20px;");
                subDiv.addElement("p").addText(" ");
                subDiv.addElement("h1").addText(ls.get(0).getThreadName());
                createTable(ls.get(0).getThreadName(), ls,
                        subDiv.addElement("div").addAttribute("stlye", "padding:20px;margin:20px;"));
                subDiv.addElement("p").addText(" ");
                createCanvas(ls.get(0).getThreadName() + "_cv", ls,
                        subDiv.addElement("div").addAttribute("stlye", "padding:20px;margin:20px;"), "Runtimes");
                addCanvasScript(ls.get(0).getThreadName() + "_cv", ls,
                        subDiv.addElement("div").addAttribute("stlye", "padding:20px;margin:20px;"));
                subDiv.addElement("p").addText(" ");
                createCanvas(ls.get(0).getThreadName() + "_cv2", all,
                        subDiv.addElement("div").addAttribute("stlye", "padding:20px;margin:20px;"), "Distribution");
                addGausCanvasScript(ls.get(0).getThreadName() + "_cv2", ls,
                        subDiv.addElement("div").addAttribute("stlye", "padding:20px;margin:20px;"));
                subDiv.addElement("p").addText(" ");
            }
        });
    }

    private void addGausCanvasScript(String id, List<RunTimeLog> logs, Element body) {
        int idx = ai.getAndIncrement();
        Map<Double, Long> sim = countSimilarTimes(logs);
        body.addElement("script")
                .addText("var ctx" + idx + "= document.getElementById('" + id + "').getContext('2d');\n" + "var chart"
                        + idx + " = new Chart(ctx" + idx + ", { type:'line', data: { labels: "
                        + getCountLabels(sim.keySet()) + ", datasets: [{ data:"
                        + getMilliSecLongJSON(new ArrayList<Long>(sim.values())) + "}]},\n"
                        + "options:{scales: {xAxes:[{display:true, scaleLabel: {display:true, labelString:'Time'}}],\n"
                        + "yAxes:[{display:true, scaleLabel: {display:true, labelString:'Count'}}]}}});");
    }

    private String getMilliSecLongJSON(List<Long> list) {
        return new Gson().toJson(list);
    }

    private String getCountLabels(Set<Double> keySet) {
        Gson gson = new Gson();
        List<Double> vals = new ArrayList<>(keySet.size());
        for (double v : keySet) {
            vals.add(v);
        }
        return gson.toJson(vals);
    }

    private void addCanvasScript(String id, List<RunTimeLog> logs, Element body) {
        int idx = ai.getAndIncrement();
        body.addElement("script")
                .addText("var ctx" + idx + "= document.getElementById('" + id + "').getContext('2d');\n" + "var chart"
                        + idx + " = new Chart(ctx" + idx + ", { type:'line', data: { labels: " + getCountLabels(logs)
                        + ", datasets: [{ data:" + getMilliSecJSON(logs) + "}]},\n"
                        + "options:{scales: {xAxes:[{display:true, scaleLabel: {display:true, labelString:'nth request'}}],\n"
                        + "yAxes:[{display:true, scaleLabel: {display:true, labelString:'Time'}}]}}});");
    }

    private String getCountLabels(List<RunTimeLog> logs) {
        Gson gson = new Gson();
        List<Integer> counts = new ArrayList<>(logs.size());
        for (int i = 0; i < logs.size(); ++i) {
            counts.add(i);
        }
        return gson.toJson(counts);
    }

    private String getMilliSecJSON(List<RunTimeLog> logs) {
        Gson gson = new Gson();
        return gson.toJson(logs.stream().map(l -> l.getDuration().toMillis()).collect(Collectors.toList()));
    }

    private void createCanvas(String id, List<RunTimeLog> logs, Element root, String title) {
        Element div = root.addElement("div").addAttribute("style",
                "width:512px; height:378px;margin:20px;padding:20px;");
        div.addElement("h2").addText(title);
        div.addElement("canvas").addAttribute("id", id).addAttribute("width", "512").addAttribute("height", "384")
                .addText(" ");
    }

    private void createTable(String id, List<RunTimeLog> logs, Element root) {
        saveStatistics(logs, root);
        Element table = root.addElement("table");
        table.addAttribute("id", id);
        Element thead = table.addElement("thead");
        Element headRow = thead.addElement("tr");
        headRow.addElement("td").addText("Duration");
        headRow.addElement("td").addText("Start");
        headRow.addElement("td").addText("End");
        headRow.addElement("td").addText("Thread");
        headRow.addElement("td").addText("Response - Passed");
        headRow.addElement("td").addText("Response - Error");
        headRow.addElement("td").addText("Response - Hit");
        headRow.addElement("td").addText("Response - Hit size");
        if (saveInput) {
            headRow.addElement("td").addText("Request");
        }
        Element tbody = table.addElement("tbody");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd - HH:mm:ss");
        for (RunTimeLog rtl : logs) {
            Element tr = tbody.addElement("tr");
            tr.addElement("td").addText(Long.toString(rtl.getDuration().toMillis()));
            tr.addElement("td").addText(formatter.format(Date.from(rtl.getStart())));
            tr.addElement("td").addText(formatter.format(Date.from(rtl.getEnd())));
            tr.addElement("td").addText(rtl.getThreadName());
            tr.addElement("td").addText(Integer.toString(rtl.getResponse().getPassed()));
            tr.addElement("td").addText(Integer.toString(rtl.getResponse().getError()));
            tr.addElement("td").addText(Integer.toString(rtl.getResponse().getHitCount()));
            tr.addElement("td").addText(Integer.toString(rtl.getResponse().getHitSize()));
            if (saveInput) {
                tr.addElement("td").addText(rtl.getRequest());
            }
        }
    }

    private void saveStatistics(List<RunTimeLog> logs, Element root) {
        Map<String, Double> summary = getStatisctics(logs);
        writeKeyValueTable(root, summary);
    }

    private void writeKeyValueTable(Element root, Map<String, Double> summary) {
        Element table = root.addElement("table");
        Element headerrow = table.addElement("thead").addElement("tr");
        headerrow.addElement("td").addText("Stat");
        headerrow.addElement("td").addText("value");
        Element tbody = table.addElement("tbody");
        for (Entry<String, Double> e : summary.entrySet()) {
            Element row = tbody.addElement("tr");
            row.addElement("td").addText(e.getKey());
            row.addElement("td").addText(Double.toString(e.getValue()));
        }
    }

    private Map<String, Double> getStatisctics(List<RunTimeLog> logs) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        logs.stream().map(l -> l.getDuration().toMillis()).forEach(l -> stats.addValue(l));
        Map<String, Double> summary = new LinkedHashMap<>();
        summary.put("Averrage", stats.getMean());
        summary.put("Min", stats.getMin());
        summary.put("Max", stats.getMax());
        summary.put("Standard deviation", stats.getStandardDeviation());
        summary.put("sum milliseconds", stats.getSum());
        summary.put("sum seconds", stats.getSum() / 1000);
        summary.put("max mode", StatUtils.max(StatUtils.mode(stats.getValues())));
        summary.put("median", stats.getPercentile(50));
        summary.put("variance", stats.getVariance());
        summary.put("populatin variance", stats.getPopulationVariance());
        return summary;
    }

    private Map<Double, Long> countSimilarTimes(List<RunTimeLog> logs) {
        Map<Double, Long> summary = new LinkedHashMap<>();
        long minL = logs.stream().map(l -> l.getDuration().toMillis()).min((lhs, rhs) -> (int) (lhs - rhs)).get();
        long maxL = logs.stream().map(l -> l.getDuration().toMillis()).max((lhs, rhs) -> (int) (lhs - rhs)).get();
        for (double i = (minL / 1000.0); i < (maxL / 1000.0); i += 0.1) {
            final double ip = Precision.round(i, 1);
            summary.put(ip, logs.stream().map(l -> Precision.round((l.getDuration().toMillis() / 1000.0), 1))
                    .filter(d -> Double.compare(d, ip) == 0).count());
        }
        return summary;
    }
}
