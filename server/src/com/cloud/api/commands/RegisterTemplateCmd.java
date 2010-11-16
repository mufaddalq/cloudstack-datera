/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.GuestOS;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;

@Implementation(description="Registers an existing template into the Cloud.com cloud. ", responseObject=TemplateResponse.class)
public class RegisterTemplateCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(RegisterTemplateCmd.class.getName());

    private static final String s_name = "registertemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.BITS, type=CommandType.INTEGER, description="32 or 64 bits support. 64 by default")
    private Integer bits;

    @Parameter(name=ApiConstants.DISPLAY_TEXT, type=CommandType.STRING, required=true, description="the display text of the template. This is usually used for display purposes.")
    private String displayText;

    @Parameter(name=ApiConstants.FORMAT, type=CommandType.STRING, required=true, description="the format for the template. Possible values include QCOW2, RAW, and VHD.")
    private String format;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, required=true, description="the target hypervisor for the template")
    private String hypervisor;

    @Parameter(name=ApiConstants.IS_FEATURED, type=CommandType.BOOLEAN, description="true if this template is a featured template, false otherwise")
    private Boolean featured;

    @Parameter(name=ApiConstants.IS_PUBLIC, type=CommandType.BOOLEAN, description="true if the template is available to all accounts; default is true")
    private Boolean publicTemplate;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the template")
    private String templateName;

    @Parameter(name=ApiConstants.OS_TYPE_ID, type=CommandType.LONG, required=true, description="the ID of the OS Type that best represents the OS of this template.")
    private Long osTypeId;

    @Parameter(name=ApiConstants.PASSWORD_ENABLED, type=CommandType.BOOLEAN, description="true if the template supports the password reset feature; default is false")
    private Boolean passwordEnabled;

    @Parameter(name=ApiConstants.REQUIRES_HVM, type=CommandType.BOOLEAN, description="true if this template requires HVM")
    private Boolean requiresHvm;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the URL of where the template is hosted. Possible URL include http:// and https://")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the ID of the zone the template is to be hosted on")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional accountName. Must be used with domainId.")
    private String accountName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getBits() {
        return bits;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getFormat() {
        return format;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return publicTemplate;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public Boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public Boolean getRequiresHvm() {
        return requiresHvm;
    }

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getDomainId() {
		return domainId;
	}

	public String getAccountName() {
		return accountName;
	}
	
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

	@Override
    public String getName() {
        return s_name;
    }

    @Override
    public void execute(){
        try {
            VMTemplateVO template = _templateMgr.registerTemplate(this);
            if (template != null){
                ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
                List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
                List<DataCenterVO> zones = null;
    
                if ((zoneId != null) && (zoneId != -1)) {
                    zones = new ArrayList<DataCenterVO>();
                    zones.add(ApiDBUtils.findZoneById(zoneId));
                } else {
                    zones = ApiDBUtils.listZones();
                }
    
                for (DataCenterVO zone : zones) {
                    TemplateResponse templateResponse = new TemplateResponse();
                    templateResponse.setId(template.getId());
                    templateResponse.setName(template.getName());
                    templateResponse.setDisplayText(template.getDisplayText());
                    templateResponse.setPublic(template.isPublicTemplate());
                    templateResponse.setCrossZones(template.isCrossZones());
    
                    VMTemplateHostVO isoHostRef = ApiDBUtils.findTemplateHostRef(template.getId(), zone.getId());
                    if (isoHostRef != null) {
                        templateResponse.setCreated(isoHostRef.getCreated());
                        templateResponse.setReady(isoHostRef.getDownloadState() == Status.DOWNLOADED);
                    }
    
                    templateResponse.setFeatured(template.isFeatured());
                    templateResponse.setPasswordEnabled(template.getEnablePassword());
                    templateResponse.setFormat(template.getFormat());
                    templateResponse.setStatus("Processing");
    
                    GuestOS os = ApiDBUtils.findGuestOSById(template.getGuestOSId());
                    if (os != null) {
                        templateResponse.setOsTypeId(os.getId());
                        templateResponse.setOsTypeName(os.getDisplayName());
                    } else {
                        templateResponse.setOsTypeId(-1L);
                        templateResponse.setOsTypeName("");
                    }
                      
                    Account owner = ApiDBUtils.findAccountById(template.getAccountId());
                    if (owner != null) {
                        templateResponse.setAccountId(owner.getId());
                        templateResponse.setAccount(owner.getAccountName());
                        templateResponse.setDomainId(owner.getDomainId());
                    }
    
                    templateResponse.setZoneId(zone.getId());
                    templateResponse.setZoneName(zone.getName());
                    templateResponse.setHypervisor(template.getHypervisorType().toString());
                    templateResponse.setObjectName("template");
    
                    responses.add(templateResponse);
                }
                response.setResponseName(getName());
                response.setResponses(responses);
                
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to register template");
            }
        } catch (ResourceAllocationException ex) {
            throw new ServerApiException(BaseCmd.RESOURCE_ALLOCATION_ERROR, ex.getMessage());
        } catch (URISyntaxException ex1) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex1.getMessage());
        }
    }
}
