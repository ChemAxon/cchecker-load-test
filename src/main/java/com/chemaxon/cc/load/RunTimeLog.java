package com.chemaxon.cc.load;

import java.time.Duration;
import java.time.Instant;

public class RunTimeLog {
    
    private final Instant start;
    private final Instant end;
    private final String threadName;
    private final int molCount;
    private final String request;
    private final SearchResponseStat response;
    
    
    public RunTimeLog(Instant start, Instant end, int molCount, String request, SearchResponseStat response) {
        this.start=start;
        this.end=end;
        this.threadName = Thread.currentThread().getName();
        this.molCount=molCount;
        this.request=request;
        this.response=response;
    }


    public Instant getStart() {
        return start;
    }


    public Instant getEnd() {
        return end;
    }


    public String getThreadName() {
        return threadName;
    }


    public int getMolCount() {
        return molCount;
    }
    
    public Duration getDuration() {
        return Duration.between(start, end);
    }
    
    public String getRequest() {
        return request;
    }
    
    public SearchResponseStat getResponse() {
        return response;
    }

}
