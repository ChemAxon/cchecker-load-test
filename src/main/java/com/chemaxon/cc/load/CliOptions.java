package com.chemaxon.cc.load;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CliOptions {
    
    private static final Logger LOG = LoggerFactory.getLogger(CliOptions.class);

    private Options opts;
    private CommandLineParser commandlineParser;
    private final CommandLine commandline;
    
    public CliOptions(String[] args) throws ParseException {
        buildOptions();
        new HelpFormatter().printHelp( "cc-load-test", "This program runs load test on cc instance", opts, "For further information please contact cc-cupport /at/ chemaxon.com", true);
        commandlineParser = new DefaultParser();
        commandline = commandlineParser.parse(opts, args);
        for(Option o : commandline.getOptions()) {
            LOG.debug("Option: {} with value: {}", o, commandline.getOptionValue(o.getOpt()));
        }
    }
    
    private void buildOptions() {
        Option threads = Option.builder("t").longOpt("threads")
                .desc("The number of threads").hasArg().type(Integer.class).required().build();
        Option chunks = Option.builder("c").longOpt("chunks")
                .desc("The number of molecules to check in one request").hasArg().type(Integer.class).required().build();
        Option url = Option.builder("u").longOpt("url")
                .desc("The url of cc-bigdata integration controller").hasArg().type(URL.class).required().build();
        Option file = Option.builder("f").longOpt("file")
                .desc("The file to use").hasArg().type(String.class).required(false).build();
        Option httpUser =  Option.builder("h").longOpt("http-user")
                .desc("The user to use to log in").hasArg().type(String.class).required(false).build();
        Option httpPassword =  Option.builder("p").longOpt("http-password")
                .desc("The password to use to log in").hasArg().type(String.class).required(false).build();
        Option output =  Option.builder("s").longOpt("save")
                .desc("Where to save results").hasArg().type(String.class).required(false).build();
        Option failOnError = Option.builder("x").longOpt("failOnError")
                .desc("If set and any error happens, the application will fail with an error.").hasArg(false)
                .required(false).build();
        Option saveInput = Option.builder("i").longOpt("saveInputInReport")
                .desc("If set report will contain input data.").hasArg(false)
                .required(false).build();
        opts=new Options();
        opts.addOption(threads);
        opts.addOption(chunks);
        opts.addOption(url);
        opts.addOption(file);
        opts.addOption(httpUser);
        opts.addOption(httpPassword);
        opts.addOption(output);
        opts.addOption(failOnError);
        opts.addOption(saveInput);
    }
    
    public int getThreads() {
        return Integer.parseInt(commandline.getOptionValue("threads"));
    }
    
    public int getChunks() {
        return Integer.parseInt(commandline.getOptionValue("chunks"));
    }
    
    public URL getURL() throws MalformedURLException {
        return new URL(commandline.getOptionValue("url"));
    }
    
    public File getFile() {
        if(commandline.hasOption("file")) {
            return new File(commandline.getOptionValue("file"));
        }
        return new File("input.txt");
    }
    
    public File getOutput() {
        if(commandline.hasOption("save")) {
            return new File(commandline.getOptionValue("save"));
        }
        return new File("output.html");
    }
    
    public String getUser() {
        if(commandline.hasOption("http-user")) {
            return commandline.getOptionValue("http-user");
        }
        return "admin";
    }
    
    public String getPassword() {
        if(commandline.hasOption("http-password")) {
            return commandline.getOptionValue("http-password");
        }
        return "adminPass";
    }
    
    public boolean isFailOnError() {
        if(commandline.hasOption("failOnError")) {
            return true;
        }
        return false;
    }
    
    public boolean isSaveInput() {
        if(commandline.hasOption("saveInputInReport")) {
            return true;
        }
        return false;
    }
}