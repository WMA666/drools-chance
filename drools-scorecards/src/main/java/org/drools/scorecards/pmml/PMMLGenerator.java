/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.scorecards.pmml;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dmg.pmml.pmml_4_1.descr.Array;
import org.dmg.pmml.pmml_4_1.descr.Attribute;
import org.dmg.pmml.pmml_4_1.descr.Characteristic;
import org.dmg.pmml.pmml_4_1.descr.Characteristics;
import org.dmg.pmml.pmml_4_1.descr.CompoundPredicate;
import org.dmg.pmml.pmml_4_1.descr.DATATYPE;
import org.dmg.pmml.pmml_4_1.descr.DataDictionary;
import org.dmg.pmml.pmml_4_1.descr.DataField;
import org.dmg.pmml.pmml_4_1.descr.Extension;
import org.dmg.pmml.pmml_4_1.descr.Header;
import org.dmg.pmml.pmml_4_1.descr.MiningField;
import org.dmg.pmml.pmml_4_1.descr.MiningSchema;
import org.dmg.pmml.pmml_4_1.descr.OPTYPE;
import org.dmg.pmml.pmml_4_1.descr.Output;
import org.dmg.pmml.pmml_4_1.descr.OutputField;
import org.dmg.pmml.pmml_4_1.descr.PMML;
import org.dmg.pmml.pmml_4_1.descr.RESULTFEATURE;
import org.dmg.pmml.pmml_4_1.descr.Scorecard;
import org.dmg.pmml.pmml_4_1.descr.SimplePredicate;
import org.dmg.pmml.pmml_4_1.descr.SimpleSetPredicate;
import org.dmg.pmml.pmml_4_1.descr.Timestamp;
import org.drools.core.util.StringUtils;
import org.drools.scorecards.StringUtil;
import org.drools.scorecards.parser.xls.XLSKeywords;

public class PMMLGenerator {

    public PMML generateDocument(Scorecard pmmlScorecard) {
        //first clean up the scorecard
        removeEmptyExtensions(pmmlScorecard);
        createAndSetPredicates(pmmlScorecard);

        //second add additional elements to scorecard
        createAndSetOutput(pmmlScorecard);
        repositionExternalClassExtensions(pmmlScorecard);

        Extension scorecardPackage = ScorecardPMMLUtils.getExtension(pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas(), PMMLExtensionNames.SCORECARD_PACKAGE);
        if ( scorecardPackage != null) {
            pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().remove(scorecardPackage);
        }
        Extension importsExt = ScorecardPMMLUtils.getExtension(pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas(), PMMLExtensionNames.SCORECARD_IMPORTS);
        if ( importsExt != null) {
            pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().remove(importsExt);
        }

        //now create the PMML document
        PMML pmml = new PMML();
        Header header = new Header();
        Timestamp timestamp = new Timestamp();
        timestamp.getContent().add(new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(new Date()));
        header.setTimestamp(timestamp);
        header.setDescription("generated by the drools-scorecards module");
        header.getExtensions().add(scorecardPackage);
        header.getExtensions().add(importsExt);
        pmml.setHeader(header);

        createAndSetDataDictionary(pmml, pmmlScorecard);
        pmml.getAssociationModelsAndBaselineModelsAndClusteringModels().add(pmmlScorecard);
        removeAttributeFieldExtension(pmmlScorecard);
        return pmml;
    }

    private void repositionExternalClassExtensions(Scorecard pmmlScorecard) {
        Characteristics characteristics = null;
        for (Object obj : pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas()) {
            if ( obj instanceof  Characteristics ) {
                characteristics = (Characteristics) obj;
                break;
            }
        }
        for (Object obj : pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas()) {
            if ( obj instanceof MiningSchema) {
                MiningSchema schema = (MiningSchema)obj;
                for (MiningField miningField : schema.getMiningFields()) {
                    String fieldName = miningField.getName();
                    for (Characteristic characteristic : characteristics.getCharacteristics()){
                        String characteristicName = ScorecardPMMLUtils.extractFieldNameFromCharacteristic(characteristic);
                        if (fieldName.equalsIgnoreCase(characteristicName)){
                            Extension extension = ScorecardPMMLUtils.getExtension(characteristic.getExtensions(), PMMLExtensionNames.CHARACTERTISTIC_EXTERNAL_CLASS);
                            if ( extension != null ) {
                                characteristic.getExtensions().remove(extension);
                                miningField.getExtensions().add(extension);
                            }
                        }
                    }
                }
            }
        }
    }

    private void removeAttributeFieldExtension(Scorecard pmmlScorecard) {
        for (Object obj : pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas()) {
            if (obj instanceof Characteristics) {
                Characteristics characteristics = (Characteristics) obj;
                for (Characteristic characteristic : characteristics.getCharacteristics()) {
                    for (Attribute attribute : characteristic.getAttributes()) {
                        Extension fieldExtension = ScorecardPMMLUtils.getExtension(attribute.getExtensions(), PMMLExtensionNames.CHARACTERTISTIC_FIELD);
                        if ( fieldExtension != null ) {
                            attribute.getExtensions().remove(fieldExtension);
                            //break;
                        }
                    }
                }
            }
        }
    }

    private void createAndSetDataDictionary(PMML pmml, Scorecard pmmlScorecard) {

        DataDictionary dataDictionary = new DataDictionary();
        pmml.setDataDictionary(dataDictionary);
        int ctr = 0;
        for (Object obj : pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas()) {
            if (obj instanceof Characteristics) {
                Characteristics characteristics = (Characteristics) obj;
                for (Characteristic characteristic : characteristics.getCharacteristics()) {

                    DataField dataField = new DataField();
                    Extension dataTypeExtension = ScorecardPMMLUtils.getExtension(characteristic.getExtensions(), PMMLExtensionNames.CHARACTERTISTIC_DATATYPE);
                    String dataType = dataTypeExtension.getValue();
                    String factType = ScorecardPMMLUtils.getExtensionValue(characteristic.getExtensions(), PMMLExtensionNames.CHARACTERTISTIC_FACTTYPE);

                    if ( factType != null ){
                        Extension extension = new Extension();
                        extension.setName("FactType");
                        extension.setValue(factType);
                        dataField.getExtensions().add(extension);
                    }

                    if (XLSKeywords.DATATYPE_NUMBER.equalsIgnoreCase(dataType)) {
                        dataField.setDataType(DATATYPE.DOUBLE);
                        dataField.setOptype(OPTYPE.CONTINUOUS);
                    } else if (XLSKeywords.DATATYPE_TEXT.equalsIgnoreCase(dataType)) {
                        dataField.setDataType(DATATYPE.STRING);
                        dataField.setOptype(OPTYPE.CATEGORICAL);
                    } else if (XLSKeywords.DATATYPE_BOOLEAN.equalsIgnoreCase(dataType)) {
                        dataField.setDataType(DATATYPE.BOOLEAN);
                        dataField.setOptype(OPTYPE.CATEGORICAL);
                    }
                    String field = "";
                    for (Attribute attribute : characteristic.getAttributes()) {
                        for (Extension extension : attribute.getExtensions()) {
                            if (PMMLExtensionNames.CHARACTERTISTIC_FIELD.equalsIgnoreCase(extension.getName())) {
                                field = extension.getValue();
                                break;
                            }//
                        }
                    }
                    dataField.setName(field);
                    dataDictionary.getDataFields().add(dataField);
                    characteristic.getExtensions().remove(dataTypeExtension);
                    ctr++;
                }
            }
        }
        dataDictionary.setNumberOfFields(BigInteger.valueOf(ctr));
    }

    private void createAndSetOutput(Scorecard pmmlScorecard) {
        Extension classExtension = ScorecardPMMLUtils.getExtension(pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas(), PMMLExtensionNames.SCORECARD_RESULTANT_SCORE_CLASS);
        Extension fieldExtension = ScorecardPMMLUtils.getExtension(pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas(), PMMLExtensionNames.SCORECARD_RESULTANT_SCORE_FIELD);
        for (Object obj : pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas()) {
            if (obj instanceof Output) {
                Output output = (Output)obj;
                OutputField outputField = new OutputField();
                outputField.setDataType(DATATYPE.DOUBLE);
                outputField.setDisplayName("Final Score");
                if ( fieldExtension != null ) {
                    pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().remove(fieldExtension);
                    outputField.setName(fieldExtension.getValue());
                } else {
                    outputField.setName("calculatedScore");
                }
                if ( classExtension != null ) {
                    pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas().remove(classExtension);
                    outputField.getExtensions().add(classExtension);
                }
                output.getOutputFields().add(outputField);
                outputField.setFeature(RESULTFEATURE.PREDICTED_VALUE);
                break;
            }
        }
    }

    private void createAndSetPredicates(Scorecard pmmlScorecard) {
        for (Object obj : pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas()) {
            if (obj instanceof Characteristics) {
                Characteristics characteristics = (Characteristics) obj;
                for (Characteristic characteristic : characteristics.getCharacteristics()) {
                    String dataType = ScorecardPMMLUtils.getExtensionValue(characteristic.getExtensions(), PMMLExtensionNames.CHARACTERTISTIC_DATATYPE);
                    Extension predicateExtension = null;
                    for (Attribute attribute : characteristic.getAttributes()) {
                        String predicateAsString = "";
                        String field = ScorecardPMMLUtils.getExtensionValue(attribute.getExtensions(), PMMLExtensionNames.CHARACTERTISTIC_FIELD);
                        for (Extension extension : attribute.getExtensions()) {
                            if ("predicateResolver".equalsIgnoreCase(extension.getName())) {
                                predicateAsString = extension.getValue();
                                predicateExtension = extension;
                                break;
                            }
                        }
                        setPredicatesForAttribute(attribute, dataType, field, predicateAsString);
                        attribute.getExtensions().remove(predicateExtension);
                    }
                }
            }
        }
    }

    private void setPredicatesForAttribute(Attribute pmmlAttribute, String dataType, String field, String predicateAsString) {
        predicateAsString = StringUtil.unescapeXML(predicateAsString);
        if (XLSKeywords.DATATYPE_NUMBER.equalsIgnoreCase(dataType)) {
            setNumericPredicate(pmmlAttribute, field, predicateAsString);
        } else if (XLSKeywords.DATATYPE_TEXT.equalsIgnoreCase(dataType)) {
            setTextPredicate(pmmlAttribute, field, predicateAsString);
        } else if (XLSKeywords.DATATYPE_BOOLEAN.equalsIgnoreCase(dataType)) {
            setBooleanPredicate(pmmlAttribute, field, predicateAsString);
        }
    }

    private void setBooleanPredicate(Attribute pmmlAttribute, String field, String predicateAsString) {
        SimplePredicate simplePredicate = new SimplePredicate();
        simplePredicate.setField(field);
        simplePredicate.setOperator(PMMLOperators.EQUAL);
        if ("TRUE".equalsIgnoreCase(predicateAsString)){
            simplePredicate.setValue("TRUE");
        } else if ("FALSE".equalsIgnoreCase(predicateAsString)){
            simplePredicate.setValue("FALSE");
        }
        pmmlAttribute.setSimplePredicate(simplePredicate);
    }

    private void setTextPredicate(Attribute pmmlAttribute, String field, String predicateAsString) {
        String operator = "";
        if (predicateAsString.startsWith("=")) {
            operator = "=";
            predicateAsString = predicateAsString.substring(1);
        } else if (predicateAsString.startsWith("!=")) {
            operator = "!=";
            predicateAsString = predicateAsString.substring(2);
        }
        if (predicateAsString.contains(",")) {
            SimpleSetPredicate simpleSetPredicate = new SimpleSetPredicate();
            if ("!=".equalsIgnoreCase(operator)) {
                simpleSetPredicate.setBooleanOperator(PMMLOperators.IS_NOT_IN);
            } else {
                simpleSetPredicate.setBooleanOperator(PMMLOperators.IS_IN);
            }
            simpleSetPredicate.setField(field);
            Array array = new Array();
            array.setContent(predicateAsString.replace(",", " "));
            array.setType("string");
            array.setN(BigInteger.valueOf(predicateAsString.split(",").length));
            simpleSetPredicate.setArray(array);
            pmmlAttribute.setSimpleSetPredicate(simpleSetPredicate);
        } else {
            SimplePredicate simplePredicate = new SimplePredicate();
            simplePredicate.setField(field);
            if ("!=".equalsIgnoreCase(operator)) {
                simplePredicate.setOperator(PMMLOperators.NOT_EQUAL);
            } else {
                simplePredicate.setOperator(PMMLOperators.EQUAL);
            }
            simplePredicate.setValue(predicateAsString);
            pmmlAttribute.setSimplePredicate(simplePredicate);
        }
    }

    private void setNumericPredicate(Attribute pmmlAttribute, String field, String predicateAsString) {
        if (predicateAsString.indexOf("-") > 0) {
            CompoundPredicate compoundPredicate = new CompoundPredicate();
            compoundPredicate.setBooleanOperator("and");
            String left = predicateAsString.substring(0, predicateAsString.indexOf("-")).trim();
            String right = predicateAsString.substring(predicateAsString.indexOf("-") + 1).trim();
            SimplePredicate simplePredicate = new SimplePredicate();
            simplePredicate.setField(field);
            simplePredicate.setOperator(PMMLOperators.GREATER_OR_EQUAL);
            simplePredicate.setValue(left);
            compoundPredicate.getSimplePredicatesAndCompoundPredicatesAndSimpleSetPredicates().add(simplePredicate);
            simplePredicate = new SimplePredicate();
            simplePredicate.setField(field);
            simplePredicate.setOperator(PMMLOperators.LESS_THAN);
            simplePredicate.setValue(right);
            compoundPredicate.getSimplePredicatesAndCompoundPredicatesAndSimpleSetPredicates().add(simplePredicate);
            pmmlAttribute.setCompoundPredicate(compoundPredicate);
        } else {
            SimplePredicate simplePredicate = new SimplePredicate();
            simplePredicate.setField(field);
            if (predicateAsString.startsWith("<=")) {
                simplePredicate.setOperator(PMMLOperators.LESS_OR_EQUAL);
                simplePredicate.setValue(predicateAsString.substring(3).trim());
            } else if (predicateAsString.startsWith(">=")) {
                simplePredicate.setOperator(PMMLOperators.GREATER_OR_EQUAL);
                simplePredicate.setValue(predicateAsString.substring(3).trim());
            } else if (predicateAsString.startsWith("=")) {
                simplePredicate.setOperator(PMMLOperators.EQUAL);
                simplePredicate.setValue(predicateAsString.substring(2).trim());
            } else if (predicateAsString.startsWith("!=")) {
                simplePredicate.setOperator(PMMLOperators.NOT_EQUAL);
                simplePredicate.setValue(predicateAsString.substring(3).trim());
            } else if (predicateAsString.startsWith("<")) {
                simplePredicate.setOperator(PMMLOperators.LESS_THAN);
                simplePredicate.setValue(predicateAsString.substring(3).trim());
            } else if (predicateAsString.startsWith(">")) {
                simplePredicate.setOperator(PMMLOperators.GREATER_THAN);
                simplePredicate.setValue(predicateAsString.substring(3).trim());
            }
            pmmlAttribute.setSimplePredicate(simplePredicate);
        }
    }

    private void removeEmptyExtensions(Scorecard pmmlScorecard) {
        for (Object obj : pmmlScorecard.getExtensionsAndCharacteristicsAndMiningSchemas()) {
            if (obj instanceof Characteristics) {
                Characteristics characteristics = (Characteristics) obj;
                for (Characteristic characteristic : characteristics.getCharacteristics()) {
                    List<Extension> toRemoveExtensionsList = new ArrayList<Extension>();
                    for (Extension extension : characteristic.getExtensions()) {
                        if (StringUtils.isEmpty(extension.getValue())) {
                            toRemoveExtensionsList.add(extension);
                        }
                    }
                    for (Extension extension : toRemoveExtensionsList) {
                        characteristic.getExtensions().remove(extension);
                    }

                    for (Attribute attribute : characteristic.getAttributes()) {
                        List<Extension> toRemoveExtensionsList2 = new ArrayList<Extension>();
                        for (Extension extension : attribute.getExtensions()) {
                            if (StringUtils.isEmpty(extension.getValue())) {
                                toRemoveExtensionsList2.add(extension);
                            }
                        }
                        for (Extension extension : toRemoveExtensionsList2) {
                            attribute.getExtensions().remove(extension);
                        }
                    }
                }
            }
        }
    }

}
