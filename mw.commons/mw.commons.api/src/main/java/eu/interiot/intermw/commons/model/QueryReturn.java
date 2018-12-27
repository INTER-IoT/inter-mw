/*
 * Copyright 2016-2018 Universitat Politècnica de València
 * Copyright 2016-2018 Università della Calabria
 * Copyright 2016-2018 Prodevelop, SL
 * Copyright 2016-2018 Technische Universiteit Eindhoven
 * Copyright 2016-2018 Fundación de la Comunidad Valenciana para la
 * Investigación, Promoción y Estudios Comerciales de Valenciaport
 * Copyright 2016-2018 Rinicom Ltd
 * Copyright 2016-2018 Association pour le développement de la formation
 * professionnelle dans le transport
 * Copyright 2016-2018 Noatum Ports Valenciana, S.A.U.
 * Copyright 2016-2018 XLAB razvoj programske opreme in svetovanje d.o.o.
 * Copyright 2016-2018 Systems Research Institute Polish Academy of Sciences
 * Copyright 2016-2018 Azienda Sanitaria Locale TO5
 * Copyright 2016-2018 Alessandro Bassi Consulting SARL
 * Copyright 2016-2018 Neways Technologies B.V.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.interiot.intermw.commons.model;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.StringWriter;
import java.util.Iterator;

/**
 * @author Gasper Vrhovsek
 */
public class QueryReturn {
    private Boolean askResult;
    private Model constructResult;
    private ResultSet selectResult;

    public void setAskResult(boolean askResult) {
        this.askResult = askResult;
    }

    public Boolean isAskResult() {
        return askResult;
    }

    public void setConstructResult(Model constructResult) {
        this.constructResult = constructResult;
    }

    public Model getConstructResult() {
        return constructResult;
    }


    public void setSelectResult(ResultSet selectResult) {
        this.selectResult = selectResult;
    }

    public ResultSet getSelectResult() {
        return selectResult;
    }

    public String resultAsString() {
        if (getConstructResult() != null) {
            StringWriter sw = new StringWriter();
            RDFDataMgr.write(sw, getConstructResult(), Lang.JSONLD);
            return sw.toString();
        } else if (getSelectResult() != null) {

            ResultSet resultSet = getSelectResult();
            StringWriter sw = new StringWriter();
            while (resultSet.hasNext()) {
                QuerySolution next = resultSet.next();
                Iterator<String> varNamesIt = next.varNames();

                while (varNamesIt.hasNext()) {
                    String varName = varNamesIt.next();
                    RDFNode rdfNode = next.get(varName);

                    sw.append(rdfNode.toString());

                    if (varNamesIt.hasNext()) {
                        sw.append(",");
                    }
                }
                sw.append("\n");
            }
            return sw.toString();
        } else if (isAskResult() != null) {
            return String.valueOf(isAskResult());
        }
        return null;
    }
}
