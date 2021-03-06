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

import org.apache.log4j.Logger;

import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.RemoteAccessVpnResponse;
import com.cloud.domain.Domain;
import com.cloud.event.EventTypes;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.net.Ip;

@Implementation(description="Creates a l2tp/ipsec remote access vpn", responseObject=RemoteAccessVpnResponse.class)
public class CreateRemoteAccessVpnCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateRemoteAccessVpnCmd.class.getName());

    private static final String s_name = "createremoteaccessvpnresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name="publicip", type=CommandType.STRING, required=true, description="public ip address of the vpn server")
    private String publicIp;

    @Parameter(name="iprange", type=CommandType.STRING, required=false, description="the range of ip addresses to allocate to vpn clients. The first ip in the range will be taken by the vpn server")
    private String ipRange;
    
    @Parameter(name="account", type=CommandType.STRING, description="an optional account for the VPN. Must be used with domainId.")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="an optional domainId for the VPN. If the account parameter is used, domainId must also be used.")
    private Long domainId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
    public String getPublicIp() {
		return publicIp;
	}

	public String getAccountName() {
		return accountName;
	}

	public Long getDomainId() {
		return domainId;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public String getIpRange() {
		return ipRange;
	}

	public void setIpRange(String ipRange) {
		this.ipRange = ipRange;
	}
	
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

   

	@Override
    public String getCommandName() {
        return s_name;
    }

	@Override
	public long getEntityOwnerId() {
		Account account = UserContext.current().getCaller();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = _responseGenerator.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

	@Override
	public String getEventDescription() {
		return "Create Remote Access VPN for account " + getEntityOwnerId() + " using public " + publicIp;
	}

	@Override
	public String getEventType() {
		return EventTypes.EVENT_REMOTE_ACCESS_VPN_CREATE;
	}
	
    @Override
    public void create() {
        try {
            RemoteAccessVpn vpn = _ravService.createRemoteAccessVpn(new Ip(publicIp), ipRange);
            if (vpn != null) {
                this.setEntityId(vpn.getServerAddress().longValue());
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create remote access vpn");
            }
        } catch (NetworkRuleConflictException e) {
            s_logger.info("Network rule conflict: " + e.getMessage());
            s_logger.trace("Network Rule Conflict: ", e);
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        }
    }

    @Override
    public void execute(){
        try {
            RemoteAccessVpn result = _ravService.startRemoteAccessVpn(new Ip(getEntityId()));
            if (result != null) {
                RemoteAccessVpnResponse response = new RemoteAccessVpnResponse();
                response.setPublicIp(result.getServerAddress().toString());
                response.setIpRange(result.getIpRange());
                response.setAccountName(_entityMgr.findById(Account.class, result.getAccountId()).getAccountName());
                response.setDomainId(result.getDomainId());
                response.setDomainName(_entityMgr.findById(Domain.class, result.getDomainId()).getName());
                response.setObjectName("remoteaccessvpn");
                response.setResponseName(getCommandName());
                response.setPresharedKey(result.getIpsecPresharedKey());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create remote access vpn");
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        }
    }
}
