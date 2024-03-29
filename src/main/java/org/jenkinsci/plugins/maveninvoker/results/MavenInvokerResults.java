package org.jenkinsci.plugins.maveninvoker.results;

/*
 * Copyright (c) Olivier Lamy
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Olivier Lamy
 */
public class MavenInvokerResults implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<InvokerResult> invokerResults = new CopyOnWriteArrayList<>();

    public MavenInvokerResults() {
        // no op
    }

    @NonNull
    public List<InvokerResult> getInvokerResults() {
        return invokerResults;
    }

    public void setInvokerResults( List<InvokerResult> invokerResults )
    {
        this.invokerResults = invokerResults;
    }

}
