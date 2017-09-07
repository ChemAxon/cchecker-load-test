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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SearchResponseStat {

    private int passed = 0;
    private int error = 0;
    private List<Integer> hits = new ArrayList<>();

    public void registerPassed() {
        ++passed;
    }

    public void registerError() {
        ++error;
    }

    public void registerHit(int hitSize) {
        hits.add(hitSize);
    }

    public int getPassed() {
        return passed;
    }

    public int getError() {
        return error;
    }

    public List<Integer> getHits() {
        return Collections.unmodifiableList(hits);
    }

    public int getHitCount() {
        return hits.size();
    }

    public int getHitSize() {
        return hits.stream().collect(Collectors.summingInt(i -> i.intValue()));
    }
}
