/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wso2telco.dep.tpservice.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Model class for tsxwho table
 * @author Chinthana
 * @since 24/06/2016
 *
 */
public class WhoDTO implements Serializable {

	private static final long serialVersionUID = -689248769205457757L;

	private int id;
	
	private String ownerId;
	
	private String tokenUrl;
	
	private long defaultConnectionRestTime;
	
	private boolean valid;
	
	private Timestamp createdDate;
	
	private int uc;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getTokenUrl() {
		return tokenUrl;
	}

	public void setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}

	public long getDefaultConnectionRestTime() {
		return defaultConnectionRestTime;
	}

	public void setDefaultConnectionRestTime(long defaultConnectionRestTime) {
		this.defaultConnectionRestTime = defaultConnectionRestTime;
	}

	public boolean getValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public Timestamp getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Timestamp createdDate) {
		this.createdDate = createdDate;
	}

	public int getUc() {
		return uc;
	}

	public void setUc(int uc) {
		this.uc = uc;
	}
}
