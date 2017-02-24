/*******************************************************************************
 * Copyright (c) 2015-2017, WSO2.Telco Inc. (http://www.wso2telco.com)
 *
 * All Rights Reserved. WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.wso2telco.services.dep.sandbox.servicefactory.payment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.icu.math.BigDecimal;
import com.wso2telco.core.dbutils.exception.ServiceError;
import com.wso2telco.dep.oneapivalidation.exceptions.CustomException;
import com.wso2telco.dep.oneapivalidation.util.Validation;
import com.wso2telco.dep.oneapivalidation.util.ValidationRule;
import com.wso2telco.services.dep.sandbox.dao.DaoFactory;
import com.wso2telco.services.dep.sandbox.dao.LoggingDAO;
import com.wso2telco.services.dep.sandbox.dao.NumberDAO;
import com.wso2telco.services.dep.sandbox.dao.PaymentDAO;
import com.wso2telco.services.dep.sandbox.dao.model.custom.*;
import com.wso2telco.services.dep.sandbox.dao.model.domain.*;
import com.wso2telco.services.dep.sandbox.servicefactory.AbstractRequestHandler;
import com.wso2telco.services.dep.sandbox.servicefactory.RequestResponseRequestHandleable;
import com.wso2telco.services.dep.sandbox.servicefactory.Returnable;
import com.wso2telco.services.dep.sandbox.servicefactory.wallet.AttributeName;
import com.wso2telco.services.dep.sandbox.servicefactory.wallet.Channel;
import com.wso2telco.services.dep.sandbox.servicefactory.wallet.TransactionStatus;
import com.wso2telco.services.dep.sandbox.util.*;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;

public class PaymentRefundRequestHandler extends AbstractRequestHandler<PaymentRefundRequestWrapperDTO> implements RequestResponseRequestHandleable<PaymentRefundRequestWrapperDTO> {


    private PaymentDAO paymentDAO;
    private LoggingDAO loggingDAO;
    private NumberDAO numberDAO;
    private PaymentRefundRequestWrapperDTO requestWrapperDTO;
    private PaymentRefundResponseWrapper responseWrapper;
    private MessageLogHandler logHandler;
    private String serviceCallRefund;
    private boolean isContainsMetaData;
    private String responseReferenceCode;

    {
        LOG = LogFactory.getLog(PaymentRefundRequestHandler.class);
        paymentDAO = DaoFactory.getPaymentDAO();
        loggingDAO = DaoFactory.getLoggingDAO();
        numberDAO = DaoFactory.getNumberDAO();
        dao = DaoFactory.getGenaricDAO();
        logHandler = MessageLogHandler.getInstance();
    }

    @Override
    protected Returnable getResponseDTO() {
        return responseWrapper;
    }

    @Override
    protected List<String> getAddress() {
        List<String> address = new ArrayList<String>();
        address.add(requestWrapperDTO.getEndUserId());
        return address;
    }

    @Override
    protected void init(PaymentRefundRequestWrapperDTO extendedRequestDTO) throws Exception {
        responseWrapper = new PaymentRefundResponseWrapper();
        requestWrapperDTO = extendedRequestDTO;
    }

    @Override
    protected boolean validate(PaymentRefundRequestWrapperDTO wrapperDTO) throws Exception {

        PaymentRefundTransactionRequestBean requestBean = wrapperDTO.getRefundRequestBean();
        PaymentRefundTransactionRequestBean.AmountTransaction request = requestBean.getAmountTransaction();
        ChargePaymentAmount paymentAmount = request.getPaymentAmount();
        PaymentChargingInformation chargingInformation = paymentAmount.getChargingInformation();
        PaymentChargingMetaData metaData = paymentAmount.getChargingMetaData();

        String clientCorrelator = CommonUtil.getNullOrTrimmedValue(request.getClientCorrelator());
        String endUserID = CommonUtil.getNullOrTrimmedValue(wrapperDTO.getEndUserId());
        String msisdn = CommonUtil.getNullOrTrimmedValue(request.getEndUserId());
        String amount = CommonUtil.getNullOrTrimmedValue(chargingInformation.getAmount().toString());
        String currency = CommonUtil.getNullOrTrimmedValue(chargingInformation.getCurrency().toString());
        String description = CommonUtil.getNullOrTrimmedValue(chargingInformation.getDescription());
        String onBehalfOf = CommonUtil.getNullOrTrimmedValue(metaData.getOnBehalfOf());
        String categoryCode = CommonUtil.getNullOrTrimmedValue(metaData.getPurchaseCategoryCode());
        String channel = CommonUtil.getNullOrTrimmedValue(metaData.getChannel());
        String taxAmount = CommonUtil.getNullOrTrimmedValue(metaData.getTaxAmount());
        String referenceCode = CommonUtil.getNullOrTrimmedValue(request.getReferenceCode());
        String transactionOperationStatus = CommonUtil.getNullOrTrimmedValue(request.getTransactionOperationStatus());

        List<ValidationRule> validationRulesList = new ArrayList<>();

        try {

            validationRulesList.add(
                    new ValidationRule(ValidationRule.VALIDATION_TYPE_OPTIONAL, "clientCorrelator", clientCorrelator));
            validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_MANDATORY_TEL_END_USER_ID,
                    "endUserID", endUserID));
            validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_MANDATORY_TEL_END_USER_ID,
                    "endUserID", msisdn));
            validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_MANDATORY,
                    "amount", amount));
            validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_MANDATORY,
                    "currency", currency));
            validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_MANDATORY,
                    "description", description));
            if (metaData != null) {
                validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_OPTIONAL,
                        "onBehalfOf", onBehalfOf));
                validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_OPTIONAL,
                        "categoryCode", categoryCode));
                validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_OPTIONAL,
                        "channel", channel));
                validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_OPTIONAL,
                        "taxAmount", taxAmount));
            }
            validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_MANDATORY,
                    "referenceCode", referenceCode));
            validationRulesList.add(new ValidationRule(ValidationRule.VALIDATION_TYPE_MANDATORY,
                    "transactionOperationStatus", transactionOperationStatus));

            ValidationRule[] validationRules = new ValidationRule[validationRulesList.size()];
            validationRules = validationRulesList.toArray(validationRules);

            Validation.checkRequestParams(validationRules);

        } catch (CustomException ex) {
            LOG.error("###REFUND### Error in Validations. ", ex);
            responseWrapper.setRequestError(
                    constructRequestError(SERVICEEXCEPTION, ex.getErrcode(), ex.getErrmsg(), ex.getErrvar()[0]));
            return false;
        }
        return true;
    }

    @Override
    protected Returnable process(PaymentRefundRequestWrapperDTO extendedRequestDTO) throws Exception {
        if (responseWrapper.getRequestError() != null) {
            responseWrapper.setHttpStatus(Response.Status.BAD_REQUEST);
            return responseWrapper;
        }
            PaymentRefundTransactionRequestBean requestBean = extendedRequestDTO.getRefundRequestBean();
            PaymentRefundTransactionRequestBean.AmountTransaction request = requestBean.getAmountTransaction();
            ChargePaymentAmount paymentAmount = request.getPaymentAmount();
            PaymentChargingInformation chargingInformation = paymentAmount.getChargingInformation();
            PaymentChargingMetaData metadata = paymentAmount.getChargingMetaData();

            String clientCorrelator = CommonUtil.getNullOrTrimmedValue(request.getClientCorrelator());
            String endUserIdPath = extendedRequestDTO.getEndUserId();
            String endUserIdRequest = request.getEndUserId();
            String endUserId = getLastMobileNumber(endUserIdPath);
            String originalServerReferenceCode = CommonUtil
                    .getNullOrTrimmedValue(request.getOriginalServerReferenceCode());
            String amount = CommonUtil.getNullOrTrimmedValue(chargingInformation.getAmount());
            String currency = CommonUtil.getNullOrTrimmedValue(chargingInformation.getCurrency());
            String description = CommonUtil.getNullOrTrimmedValue(chargingInformation.getDescription());
            String onBehalfOf = CommonUtil.getNullOrTrimmedValue(metadata.getOnBehalfOf());
            String categoryCode = CommonUtil.getNullOrTrimmedValue(metadata.getPurchaseCategoryCode());
            String channel = CommonUtil.getNullOrTrimmedValue(metadata.getChannel());
            String taxAmount = CommonUtil.getNullOrTrimmedValue(metadata.getTaxAmount());
            String referenceCode = CommonUtil.getNullOrTrimmedValue(request.getReferenceCode());
            String transactionOperationStatus = CommonUtil.getNullOrTrimmedValue(request.getTransactionOperationStatus());
            serviceCallRefund = ServiceName.RefundUser.toString();
            // Attribute Name Used in wallet
            String accountCurrencyAttribute = AttributeName.Currency.toString().toLowerCase();
            String serviceCallBalanceLookUp = ServiceName.BalanceLookup.toString();
            Integer userId = extendedRequestDTO.getUser().getId();

            PaymentRefundTransactionResponseBean responseBean = new PaymentRefundTransactionResponseBean();
            ChargeRefundAmountResponse payAmount = new ChargeRefundAmountResponse();
            PaymentChargingInformation chargeInformation = new PaymentChargingInformation();
            PaymentChargingMetaData chargeMetaData = new PaymentChargingMetaData();


            // Save Request Log
            APITypes apiTypes = dao.getAPIType(RequestType.PAYMENT.toString());
            //This has Hardcoded value
            APIServiceCalls apiServiceCalls = dao.getServiceCall(apiTypes.getId(), serviceCallRefund);

            Gson gson = new Gson();
            String jsonString = gson.toJson(requestBean);

        try {

            int serviceNameId = apiServiceCalls.getApiServiceCallId();

            if (clientCorrelator != null) {

                String response = checkDuplicateClientCorrelator(clientCorrelator, userId, serviceNameId, endUserId,"1", "1", referenceCode);

                if (response != null) {

                    // return already send response
                    PaymentRefundTransactionResponseBean obj = null;
                    obj = gson.fromJson(response, PaymentRefundTransactionResponseBean.class);
                    RefundPaymentDTO dto = new RefundPaymentDTO();
                    dto.setAmountTransaction(obj);
                    responseWrapper.setRefundPaymentDTO(dto);
                    responseWrapper.setHttpStatus(Response.Status.OK);
                    return responseWrapper;
                }
            }

            //check referenceCode
            String result = checkReferenceCode(userId, serviceNameId, endUserId, "1", "1", referenceCode);

            if((result!=null)){
                LOG.error("###PAYMENT### Already charged for this reference code");
                responseWrapper.setRequestError(constructRequestError(SERVICEEXCEPTION,
                        ServiceError.INVALID_INPUT_VALUE, "Already charged for this reference code"));
                responseWrapper.setHttpStatus(Response.Status.BAD_REQUEST);
                return responseWrapper;
            }

            // check path param endUserId and request body endUserId
            if (!(endUserIdPath.equals(endUserIdRequest))) {
                LOG.error("###REFUND### two different endUserId provided");
                responseWrapper.setRequestError(constructRequestError(SERVICEEXCEPTION,
                        ServiceError.INVALID_INPUT_VALUE, "two different endUserId provided"));
                responseWrapper.setHttpStatus(Response.Status.BAD_REQUEST);
                return responseWrapper;
            }

            // check valid amount format
            if ((NumberUtils.isNumber(amount) != true)) {
                LOG.error("###REFUND### amount should be a valid number");
                responseWrapper.setRequestError(constructRequestError(SERVICEEXCEPTION,
                        ServiceError.INVALID_INPUT_VALUE, "amount should be a valid number"));
                responseWrapper.setHttpStatus(Response.Status.BAD_REQUEST);
                return responseWrapper;
            }

            // check account amount decimal format
            Double chargeAmount = Double.parseDouble(amount);
            BigDecimal bigDecimal = new BigDecimal(amount);
            Integer decimalDigits = bigDecimal.scale();
            if (!((decimalDigits <= 2) && (decimalDigits >= 0)) || chargeAmount < 0) {
                LOG.error("###REFUND### amount should be a whole number or two digit decimal");
                responseWrapper
                        .setRequestError(constructRequestError(SERVICEEXCEPTION, ServiceError.INVALID_INPUT_VALUE,
                                "amount should be a whole or two digit decimal positive number"));
                responseWrapper.setHttpStatus(Response.Status.BAD_REQUEST);
                return responseWrapper;
            }

            // check valid account currency for endUserId
            boolean isValidCurrency = currencySymbol(currency);
            if (!isValidCurrency) {
                LOG.error("###REFUND### currency code not as per ISO 4217");
                responseWrapper.setRequestError(constructRequestError(SERVICEEXCEPTION,
                        ServiceError.INVALID_INPUT_VALUE, "currency code not as per ISO 4217"));
                responseWrapper.setHttpStatus(Response.Status.BAD_REQUEST);
                return responseWrapper;

            }

            AttributeValues accountCurrencyValue = paymentDAO.getAttributeValue(endUserId, serviceCallBalanceLookUp,
                    accountCurrencyAttribute, userId);
            if (accountCurrencyValue != null) {
                String accountCurrency = accountCurrencyValue.getValue();
                if (!(currency.equals(accountCurrency))) {
                    LOG.error("###REFUND### Valid currency doesn't exists for the given inputs");
                    responseWrapper
                            .setRequestError(constructRequestError(SERVICEEXCEPTION, ServiceError.INVALID_INPUT_VALUE,
                                    "Valid currency does not exist for the given input parameters"));
                    responseWrapper.setHttpStatus(Response.Status.BAD_REQUEST);
                    return responseWrapper;
                }
            }

            // check channel
            if (channel != null && !containsChannel(channel)) {
                LOG.error("###REFUND### Valid channel doesn't exists for the given inputs");
                responseWrapper.setRequestError(constructRequestError(SERVICEEXCEPTION,
                        ServiceError.INVALID_INPUT_VALUE, "Valid channel doesn't exists for the given inputs"));
                responseWrapper.setHttpStatus(Response.Status.BAD_REQUEST);
                return responseWrapper;
            }

            responseBean.setClientCorrelator(clientCorrelator);
            responseBean.setEndUserId(endUserIdPath);
            responseBean.setOriginalServerReferenceCode(originalServerReferenceCode);


            chargeInformation.setAmount(amount);
            chargeInformation.setCurrency(currency);
            chargeInformation.setDescription(description);

            if (onBehalfOf != null || categoryCode != null || channel != null) {
                chargeMetaData.setPurchaseCategoryCode(categoryCode);
                chargeMetaData.setChannel(channel);
                chargeMetaData.setOnBehalfOf(onBehalfOf);
                chargeMetaData.setTaxAmount(taxAmount);
                isContainsMetaData = true;
            }

            responseBean.setReferenceCode(referenceCode);
            responseBean.setResourceURL(CommonUtil.getResourceUrl(extendedRequestDTO));

            // Get the Charged Tax Amount
            Double chargedTaxAmount = Double.parseDouble(taxAmount);
            //Total Amount Refunded
            Double totalAmountRefunded = chargeAmount + chargedTaxAmount;
            // Setting the total Amount Refund
            payAmount.setTotalAmountRefunded(totalAmountRefunded.toString());

            // set transaction operation status as charged
            ManageNumber manageNumber = numberDAO.getNumber(endUserId, extendedRequestDTO.getUser().getUserName());
            Double updateBalance = manageNumber.getBalance() + (chargeAmount + chargedTaxAmount);
            AttributeValues accountStatusValue = paymentDAO.getAttributeValue(endUserId, serviceCallRefund,
                    AttributeName.transactionStatus.toString(), userId);
            // set transaction operation status as refused
            manageNumber.setBalance(updateBalance);

            if (accountStatusValue != null) {
                String accountStatus = accountStatusValue.getValue();
                // set transaction operation status as Refused   // Here wallet Status are Used
                if (accountStatus.equals(TransactionStatus.Refused.toString())) {
                    responseBean.setTransactionOperationStatus(TransactionStatus.Refused.toString());

                }
            }
            // set transaction operation status as Refunded
            else if (paymentDAO.saveManageNumbers(manageNumber)) {
                responseBean.setTransactionOperationStatus(TransactionStatus.Refunded.toString());
            }

            responseWrapper.setHttpStatus(Response.Status.OK);
            RefundPaymentDTO refundPaymentDTO = new RefundPaymentDTO();
            payAmount.setChargingInformation(chargingInformation);
            payAmount.setChargingMetaData(chargeMetaData);
            responseBean.setPaymentAmount(payAmount);
            refundPaymentDTO.setAmountTransaction(responseBean); // RefundTransaction
            responseWrapper.setRefundPaymentDTO(refundPaymentDTO);

            // Save Response in message log table
            saveResponse(userId, endUserIdPath, responseBean, apiServiceCalls,"1");

        } catch (Exception ex) {
            // TODO: save failed messages in message log table
            saveRequest(extendedRequestDTO, endUserIdPath, apiServiceCalls, jsonString, "0");
            saveResponse(userId, endUserIdPath, responseBean, apiServiceCalls,"0");
            LOG.error("###REFUND### Error Occured in PAYMENT Service. ", ex);
            responseWrapper.setHttpStatus(Response.Status.BAD_REQUEST);
            responseWrapper
                    .setRequestError(constructRequestError(SERVICEEXCEPTION, ServiceError.SERVICE_ERROR_OCCURED, null));

        }
        return responseWrapper;
    }

    private String checkDuplicateClientCorrelator(String clientCorrelator, int userId, int serviceNameId, String tel, String status, String type, String referenceCode) throws Exception {

        List<Integer> list = new ArrayList<>();
        list.add(serviceNameId);
        List<MessageLog> response = loggingDAO.getMessageLogs(userId, list, "msisdn", "tel:+" + tel, null, null);

        String jsonString = null;

        for (int i = 0; i < response.size(); i++) {

            if (response != null) {

                String responseStatus = response.get(i).getStatus();
                String responseType = response.get(i).getType();
                String responseClientCorrelator;

                if (responseType.equals(type) && responseStatus.equals(status)) {
                    String request = response.get(i).getRequest();
                    JSONObject json = new JSONObject(request);

                    responseClientCorrelator = null;

                    if (json.has("clientCorrelator")) {
                        responseClientCorrelator = json.get("clientCorrelator").toString();
                    }

                    responseReferenceCode = json.get("referenceCode").toString();

                    int responseUserId = response.get(i).getUserid();
                    String responseTel = response.get(i).getValue();

                    // Check client correlator
                    if ((responseClientCorrelator!=null && responseClientCorrelator.equals(clientCorrelator)) &&
                            responseUserId == userId && responseTel.equals("tel:+" + tel)) {
                        jsonString = json.toString();
                        break;
                    }
                }

            }
        }

        return jsonString;

    }

    //check reference code
    private String checkReferenceCode(int userId, int serviceNameId, String tel, String status, String type, String referenceCode) throws Exception {

        List<Integer> list = new ArrayList<>();
        list.add(serviceNameId);
        List<MessageLog> response = loggingDAO.getMessageLogs(userId, list, "msisdn", "tel:+" + tel, null, null);

        String jsonString = null;

        for (int i = 0; i < response.size(); i++) {

            if (response != null) {

                String responseStatus = response.get(i).getStatus();
                String responseType = response.get(i).getType();

                if (responseType.equals(type) && responseStatus.equals(status)) {
                    String request = response.get(i).getRequest();
                    JSONObject json = new JSONObject(request);
                    responseReferenceCode = json.get("referenceCode").toString();

                    int responseUserId = response.get(i).getUserid();
                    String responseTel = response.get(i).getValue();

                    // Check reference CodeDuplication
                    if (responseUserId == userId && responseTel.equals("tel:+" + tel) && responseReferenceCode.equals(referenceCode)) {
                        jsonString = responseReferenceCode;
                        break;
                    }
                }

            }

        }

        return jsonString;
    }

    // saving request in message log table
    private MessageLog saveRequest(PaymentRefundRequestWrapperDTO extendedRequestDTO,
                                     String endUserIdPath, APIServiceCalls apiServiceCalls, String jsonString, String status) throws Exception {
        MessageLog messageLog = new MessageLog();
        messageLog.setServicenameid(apiServiceCalls.getApiServiceCallId());
        messageLog.setUserid(extendedRequestDTO.getUser().getId());
        messageLog.setReference("msisdn");
        messageLog.setValue(endUserIdPath);
        messageLog.setRequest(jsonString);
        messageLog.setStatus(status);
        messageLog.setType("request");
        messageLog.setMessageTimestamp(new Date());

        loggingDAO.saveMessageLog(messageLog);

        return messageLog;
    }

    // New method to save Response in messageLog table
    private void saveResponse(Integer userId, String endUserIdPath, PaymentRefundTransactionResponseBean responseBean, APIServiceCalls apiServiceCalls, String status) throws Exception {

        String jsonInString = null;
        Gson resp = new Gson();

        JsonElement je = new JsonParser().parse(resp.toJson(responseBean));
        JsonObject asJsonObject = je.getAsJsonObject();
        jsonInString = asJsonObject.toString();


        //setting messagelog responses
        MessageLog messageLog1 = new MessageLog();
        messageLog1 = new MessageLog();
        messageLog1.setRequest(jsonInString);
        messageLog1.setStatus(status);
        messageLog1.setType("1");
        messageLog1.setServicenameid(apiServiceCalls.getApiServiceCallId());
        messageLog1.setUserid(userId);
        messageLog1.setReference("msisdn");
        messageLog1.setValue(endUserIdPath);
        messageLog1.setMessageTimestamp(new Date());

        loggingDAO.saveMessageLog(messageLog1);
    }

    private static boolean currencySymbol(@Nonnull final String currencyCode) {
        try {
            final Currency currency = Currency.getInstance(currencyCode);
            return true;
        } catch (final IllegalArgumentException x) {
            return false;
        }
    }

    public boolean containsChannel(String channelValue) {

        for (Channel channel : Channel.values()) {
            if (channel.name().toLowerCase().equals(channelValue.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getApiServiceCalls() {
        return ServiceName.RefundUser.toString();
    }

    @Override
    public String getJosonString(PaymentRefundRequestWrapperDTO requestDTO) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(requestDTO.getRefundRequestBean());
        return jsonString;    }

    @Override
    public String getnumber(PaymentRefundRequestWrapperDTO requestDTO) {
        return requestDTO.getEndUserId();
    }
}
