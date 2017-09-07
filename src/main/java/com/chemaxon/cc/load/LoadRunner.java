package com.chemaxon.cc.load;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;

public class LoadRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LoadRunner.class);

    public static void main(String[] args) throws ParseException, IOException, URISyntaxException, InterruptedException {
        CliOptions clio = new CliOptions(args);
        final List<Molecule> mols = LoadMolecules(clio);
        LOG.info("running on {} concurrent threads", clio.getThreads());
        LOG.info("sending {} mols in one request", clio.getChunks());
        LOG.info("using service: {}", clio.getURL());
        LOG.info("using user: {}", clio.getUser());
        LOG.info("loading file: {}", clio.getFile());
        LOG.info("saving file: {}", clio.getOutput());
        LOG.info("loaded {} molecules", mols.size());
        List<Thread> threads = new ArrayList<>();
        List<ComplianceCaller> ccallers = new ArrayList<>();
        Instant start = Instant.now();
        for (int i = 0; i < clio.getThreads(); ++i) {
            ComplianceCaller ccaller = new ComplianceCaller(mols, clio.getURL(), clio.getChunks(), clio.getUser(), clio.getPassword());
            ccallers.add(ccaller);
            Thread t = new Thread(ccaller, "ComplianceRunner_"+i);
            t.start();
            threads.add(t);
        }
        for(Thread t : threads) {
            t.join();
        }
        Instant end = Instant.now();
        boolean noErrors = true;
        int sumChecks = 0;
        int faliedThreads = 0;
        for(ComplianceCaller ccaller : ccallers) {
            noErrors &= ccaller.isEveryCheckFinnished();
            sumChecks+=ccaller.getErrorCount()+ccaller.getHitCount()+ccaller.getPassedCount();
            if(!ccaller.isEveryCheckFinnished()) {
                ++faliedThreads;
            }
            LOG.info("hit: {}\terrror: {}\tpassed: {}\t -- {}", ccaller.getHitCount(), ccaller.getErrorCount(), ccaller.getPassedCount(), ccaller.isEveryCheckFinnished()?"PASSED":"FAILED");
        }
        if(noErrors) {
            LOG.info("Every check could finnish");
        } else {
            LOG.error("Error has happenned during checking");
            LOG.error("Failed threads count: {}", faliedThreads);
        }
        LOG.info("checked mols: {}\tplannedChecks: {}\t {}%", sumChecks, clio.getThreads()*mols.size(), ((double)sumChecks)/(clio.getThreads()*mols.size())*100.0);
        LOG.info("taken: {}", Duration.between(start, end));
        LOG.info("throughput: {} mol/sec", (((double)sumChecks)/Duration.between(start, end).toMillis())*1000);
        new ReportCreator(ccallers.stream().map(cc -> cc.getLogs()).collect(Collectors.toList()),
                clio.getThreads() * mols.size(), sumChecks, start, end, clio.isSaveInput()).saveHtmlReport(clio.getOutput());
        if(clio.isFailOnError() && !noErrors) {
            System.exit(1);
        }
    }

    private static List<Molecule> LoadMolecules(CliOptions clio) throws IOException {
        List<Molecule> mols = new ArrayList<>();
        try (MolImporter mi = new MolImporter(clio.getFile())) {
            Molecule m = null;
            while ((m = mi.read()) != null) {
                mols.add(m);

            }
        } catch (Exception e) {
            LOG.error("Could not load molecules", e);
            throw e;
        }
        return mols;
    }

}
