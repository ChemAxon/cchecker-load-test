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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chemaxon.marvin.io.formats.csv.CsvExport;
import com.google.gson.Gson;

import chemaxon.formats.MolExporter;
import chemaxon.struc.Molecule;

public class ComplianceCaller implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ComplianceCaller.class);

    private final List<Molecule> molsToCheck;
    private final URI uriToCall;
    private final int chunkSize;
    private final String user;
    private final String password;
    private final List<RunTimeLog> runs = new ArrayList<>();
    private final String dateToCheck;
    private final List<String> categoriesToCheck;
    private int passedCount = 0;
    private int hitCount = 0;
    private int errorCount = 0;
    

    public ComplianceCaller(List<Molecule> molsToCheck, URL url, int chunkSize, String user, String password, String dateToCheck, List<String> categoriesToCheck) throws URISyntaxException {
        this.molsToCheck = Collections.unmodifiableList(ComplianceCaller.shuffle(molsToCheck));
        this.uriToCall = new URI(url.toString() + "/check/list");
        this.chunkSize = chunkSize;
        this.user = user;
        this.password = password;
        this.dateToCheck=dateToCheck;
        this.categoriesToCheck=categoriesToCheck;
    }

    private static List<Molecule> shuffle(List<Molecule> molsToCheck) {
        List<Molecule> s = new ArrayList<>(molsToCheck);
        Collections.shuffle(s);
        return s;
    }

    @Override
    public void run() {
        try {
            HttpClient client = HttpClients.custom()
                    .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();
            try {
                int count = 0;
                while (count < molsToCheck.size()) {
                    Instant start = Instant.now();
                    String reqBody = createReuest(count);
                    StringEntity req = new StringEntity(reqBody);
                    count += chunkSize;
                    HttpResponse r = sendRequest(client, req);
                    Instant end = Instant.now();
                    checkResponse(r);
                    SearchResponseStat res = countLegistlations(r);
                    runs.add(new RunTimeLog(start, end, chunkSize, reqBody, res));
                }
            } catch (Exception e) {
                LOG.error("Could not execute search due to: " + e, e);
                throw e;
            }
        } catch (Exception e) {
            LOG.error("Could not execute compliance checking", e);
            throw new RuntimeException("Exception during compliance checking", e);
        }
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getHitCount() {
        return hitCount;
    }

    public List<RunTimeLog> getLogs() {
        return new ArrayList<>(runs);
    }

    public boolean isEveryCheckFinnished() {
        return molsToCheck.size() == hitCount + errorCount + passedCount;
    }

    private HttpResponse sendRequest(HttpClient client, StringEntity req) throws IOException, ClientProtocolException {
        HttpPost post = new HttpPost(uriToCall);
        post.setEntity(req);
        post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        HttpResponse r = client.execute(targetHost, post, getContext());
        return r;
    }

    private String createReuest(int count) throws IOException {
        List<Molecule> subMols = molsToCheck.subList(count, Math.min(count + chunkSize, molsToCheck.size()));
        List<String> srcs = getSources(subMols);

        CCheckingRequest req = new CCheckingRequest();
        req.setInput(srcs);
        req.setDate(dateToCheck);
        req.setCategories(categoriesToCheck);
        Gson gson = new Gson();
        return gson.toJson(req);
    }

    private SearchResponseStat countLegistlations(HttpResponse r) throws IOException {
        Gson gson = new Gson();
        SearchResponseStat result = new SearchResponseStat();
        LegistlationResponse lr = gson.fromJson(new InputStreamReader(r.getEntity().getContent()),
                LegistlationResponse.class);
        for (List<LegistlationData> lds : lr.getSimpleResponses()) {
            if (lds.isEmpty()) {
                ++passedCount;
                result.registerPassed();
            } else {
                if (lds.get(0).isError()) {
                    ++errorCount;
                    result.registerError();
                } else {
                    ++hitCount;
                    result.registerHit(lds.size());
                }
            }
        }
        return result;
    }

    private void checkResponse(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IllegalStateException("Compliance checking request returned: " + response.getStatusLine());
        }
    }

    private HttpClientContext getContext() {
        CredentialsProvider credsProvider = getCredentialsProvider();
        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());

        HttpClientContext hcc = new HttpClientContext();
        hcc.setCredentialsProvider(credsProvider);
        hcc.setAuthCache(authCache);
        return hcc;
    }

    private CredentialsProvider getCredentialsProvider() {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(user, password));
        return credsProvider;
    }

    private List<String> getSources(List<Molecule> subMols) throws IOException {
        List<String> srcs = new ArrayList<>(subMols.size());
        for (Molecule m : subMols) {
            srcs.add((String) MolExporter.exportToObject(new Molecule[] { m }, "csv", new CsvExport()));
        }
        return srcs;
    }

}
