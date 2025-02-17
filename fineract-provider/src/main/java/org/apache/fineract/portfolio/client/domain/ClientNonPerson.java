/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.client.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;

@Entity
@Table(name = "m_client_non_person")
public class ClientNonPerson extends AbstractPersistableCustom {

    @OneToOne(optional = false)
    @JoinColumn(name = "client_id", referencedColumnName = "id", nullable = false, unique = true)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constitution_cv_id", nullable = false)
    private CodeValue constitution;

    @Column(name = "incorp_no", length = 50, nullable = true)
    private String incorpNumber;

    @Column(name = "incorp_validity_till", nullable = true)
    private LocalDate incorpValidityTill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_business_line_cv_id", nullable = true)
    private CodeValue mainBusinessLine;

    @Column(name = "remarks", length = 150, nullable = true)
    private String remarks;

    @Column(name = "in_business_since", nullable = true)
    private LocalDate inBusinessSince;

    @Column(name = "is_registered", nullable = true)
    private Boolean isRegistered;

    public static ClientNonPerson createNew(final Client client, final CodeValue constitution, final CodeValue mainBusinessLine,
            String incorpNumber, LocalDate incorpValidityTill, String remarks, LocalDate inBusinessSince, Boolean isRegistered) {
        return new ClientNonPerson(client, constitution, mainBusinessLine, incorpNumber, incorpValidityTill, remarks, inBusinessSince,
                isRegistered);
    }

    protected ClientNonPerson() {
        //
    }

    private ClientNonPerson(final Client client, final CodeValue constitution, final CodeValue mainBusinessLine, final String incorpNumber,
            final LocalDate incorpValidityTill, final String remarks, LocalDate inBusinessSince, Boolean isRegistered) {
        if (client != null) {
            this.client = client;
        }

        if (constitution != null) {
            this.constitution = constitution;
        }

        if (mainBusinessLine != null) {
            this.mainBusinessLine = mainBusinessLine;
        }

        if (StringUtils.isNotBlank(incorpNumber)) {
            this.incorpNumber = incorpNumber.trim();
        }

        this.incorpValidityTill = incorpValidityTill;

        if (StringUtils.isNotBlank(remarks)) {
            this.remarks = remarks.trim();
        }

        this.inBusinessSince = inBusinessSince;
        this.isRegistered = isRegistered;

        validate(client);
    }

    private void validate(final Client client) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        validateIncorpValidityTillDate(client, dataValidationErrors);
        validateInBusinessSinceTillDate(client, dataValidationErrors);

        if (this.constitution == null) {
            dataValidationErrors.add(ApiParameterError.parameterError("error.msg.clients.constitutionid.is.null",
                    "Constitution ID may not be null", ClientApiConstants.constitutionIdParamName));
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

    }

    private void validateIncorpValidityTillDate(final Client client, final List<ApiParameterError> dataValidationErrors) {
        if (getIncorpValidityTillLocalDate() != null && client.dateOfBirthLocalDate() != null
                && client.dateOfBirthLocalDate().isAfter(getIncorpValidityTillLocalDate())) {

            final String defaultUserMessage = "incorpvaliditytill date cannot be after the incorporation date";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.clients.incorpValidityTill.after.incorp.date",
                    defaultUserMessage, ClientApiConstants.incorpValidityTillParamName, this.incorpValidityTill);

            dataValidationErrors.add(error);
        }
    }

    private void validateInBusinessSinceTillDate(final Client client, final List<ApiParameterError> dataValidationErrors) {

        if (getInBusinessSinceLocalDate() != null && getInBusinessSinceLocalDate().isAfter(DateUtils.getLocalDateOfTenant())) {

            final String defaultUserMessage = "In business since date cannot be after the current date of the tenant";
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.clients.inbusinesssince.after.tenant.date",
                    defaultUserMessage, ClientApiConstants.inBusinessSinceParamName, this.inBusinessSince);

            dataValidationErrors.add(error);
        }
    }

    public LocalDate getIncorpValidityTillLocalDate() {
        return this.incorpValidityTill;
    }

    public LocalDate getInBusinessSinceLocalDate() {
        return this.inBusinessSince;
    }

    public Long constitutionId() {
        Long constitutionId = null;
        if (this.constitution != null) {
            constitutionId = this.constitution.getId();
        }
        return constitutionId;
    }

    public Long mainBusinessLineId() {
        Long mainBusinessLineId = null;
        if (this.mainBusinessLine != null) {
            mainBusinessLineId = this.mainBusinessLine.getId();
        }
        return mainBusinessLineId;
    }

    public void updateConstitution(CodeValue constitution) {
        this.constitution = constitution;
    }

    public void updateMainBusinessLine(CodeValue mainBusinessLine) {
        this.mainBusinessLine = mainBusinessLine;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        if (command.isChangeInStringParameterNamed(ClientApiConstants.incorpNumberParamName, this.incorpNumber)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.incorpNumberParamName);
            actualChanges.put(ClientApiConstants.incorpNumberParamName, newValue);
            this.incorpNumber = StringUtils.defaultIfEmpty(newValue, null);
        }

        if (command.isChangeInStringParameterNamed(ClientApiConstants.remarksParamName, this.remarks)) {
            final String newValue = command.stringValueOfParameterNamed(ClientApiConstants.remarksParamName);
            actualChanges.put(ClientApiConstants.remarksParamName, newValue);
            this.remarks = StringUtils.defaultIfEmpty(newValue, null);
        }

        final String dateFormatAsInput = command.dateFormat();
        final String localeAsInput = command.locale();

        if (command.isChangeInLocalDateParameterNamed(ClientApiConstants.inBusinessSinceParamName, getInBusinessSinceLocalDate())) {
            final LocalDate newValue = command.localDateValueOfParameterNamed(ClientApiConstants.inBusinessSinceParamName);
            actualChanges.put(ClientApiConstants.inBusinessSinceParamName, newValue);
            actualChanges.put(ClientApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(ClientApiConstants.localeParamName, localeAsInput);
            this.inBusinessSince = newValue;
        }

        if (command.isChangeInLocalDateParameterNamed(ClientApiConstants.incorpValidityTillParamName, getIncorpValidityTillLocalDate())) {
            final String valueAsInput = command.stringValueOfParameterNamed(ClientApiConstants.incorpValidityTillParamName);
            actualChanges.put(ClientApiConstants.incorpValidityTillParamName, valueAsInput);
            actualChanges.put(ClientApiConstants.dateFormatParamName, dateFormatAsInput);
            actualChanges.put(ClientApiConstants.localeParamName, localeAsInput);

            this.incorpValidityTill = command.localDateValueOfParameterNamed(ClientApiConstants.incorpValidityTillParamName);
        }

        if (command.isChangeInBooleanParameterNamed(ClientApiConstants.isRegisteredParam, this.isRegistered)) {
            final Boolean newValue = command.booleanObjectValueOfParameterNamed(ClientApiConstants.isRegisteredParam);
            actualChanges.put(ClientApiConstants.isRegisteredParam, newValue);
            this.isRegistered = newValue;
        }

        if (command.isChangeInLongParameterNamed(ClientApiConstants.constitutionIdParamName, constitutionId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.constitutionIdParamName);
            actualChanges.put(ClientApiConstants.constitutionIdParamName, newValue);
        }

        if (command.isChangeInLongParameterNamed(ClientApiConstants.mainBusinessLineIdParamName, mainBusinessLineId())) {
            final Long newValue = command.longValueOfParameterNamed(ClientApiConstants.mainBusinessLineIdParamName);
            actualChanges.put(ClientApiConstants.mainBusinessLineIdParamName, newValue);
        }

        // validate();

        return actualChanges;

    }
}
