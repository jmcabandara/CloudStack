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
package com.cloud.host.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import javax.persistence.TableGenerator;

import org.apache.log4j.Logger;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Host.Type;
import com.cloud.host.Status.Event;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = { HostDao.class }) @DB(txn=false)
@TableGenerator(name="host_req_sq", table="op_host", pkColumnName="id", valueColumnName="sequence", allocationSize=1)
public class HostDaoImpl extends GenericDaoBase<HostVO, Long> implements HostDao {
    private static final Logger s_logger = Logger.getLogger(HostDaoImpl.class);

    protected final SearchBuilder<HostVO> TypePodDcStatusSearch;

    protected final SearchBuilder<HostVO> IdStatusSearch;
    protected final SearchBuilder<HostVO> TypeDcSearch;
    protected final SearchBuilder<HostVO> TypeDcStatusSearch;
    protected final SearchBuilder<HostVO> LastPingedSearch;
    protected final SearchBuilder<HostVO> LastPingedSearch2;
    protected final SearchBuilder<HostVO> MsStatusSearch;
    protected final SearchBuilder<HostVO> DcPrivateIpAddressSearch;
    protected final SearchBuilder<HostVO> DcStorageIpAddressSearch;

    protected final SearchBuilder<HostVO> GuidSearch;
    protected final SearchBuilder<HostVO> DcSearch;
    protected final SearchBuilder<HostVO> PodSearch;
    protected final SearchBuilder<HostVO> TypeSearch;
    protected final SearchBuilder<HostVO> StatusSearch;
    protected final SearchBuilder<HostVO> NameLikeSearch;
    protected final SearchBuilder<HostVO> SequenceSearch;
    protected final SearchBuilder<HostVO> DirectlyConnectedSearch;
    protected final SearchBuilder<HostVO> UnmanagedDirectConnectSearch;
    protected final SearchBuilder<HostVO> UnmanagedExternalNetworkApplianceSearch;
    protected final SearchBuilder<HostVO> MaintenanceCountSearch;
    protected final SearchBuilder<HostVO> ClusterSearch;
    protected final SearchBuilder<HostVO> ConsoleProxyHostSearch;
    
    protected final Attribute _statusAttr;
    protected final Attribute _msIdAttr;
    protected final Attribute _pingTimeAttr;
    
    protected final DetailsDaoImpl _detailsDao = ComponentLocator.inject(DetailsDaoImpl.class);

    public HostDaoImpl() {
    
        MaintenanceCountSearch = createSearchBuilder();
        MaintenanceCountSearch.and("cluster", MaintenanceCountSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        MaintenanceCountSearch.and("status", MaintenanceCountSearch.entity().getStatus(), SearchCriteria.Op.IN);
        MaintenanceCountSearch.done();
        
        TypePodDcStatusSearch = createSearchBuilder();
        HostVO entity = TypePodDcStatusSearch.entity();
        TypePodDcStatusSearch.and("type", entity.getType(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.and("pod", entity.getPodId(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.and("dc", entity.getDataCenterId(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.and("cluster", entity.getClusterId(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.and("status", entity.getStatus(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.done();

        LastPingedSearch = createSearchBuilder();
        LastPingedSearch.and("ping", LastPingedSearch.entity().getLastPinged(), SearchCriteria.Op.LT);
        LastPingedSearch.and("state", LastPingedSearch.entity().getStatus(), SearchCriteria.Op.IN);
        LastPingedSearch.done();
        
        LastPingedSearch2 = createSearchBuilder();
        LastPingedSearch2.and("ping", LastPingedSearch2.entity().getLastPinged(), SearchCriteria.Op.LT);
        LastPingedSearch2.and("type", LastPingedSearch2.entity().getType(), SearchCriteria.Op.EQ);
        LastPingedSearch2.done();
        
        MsStatusSearch = createSearchBuilder();
        MsStatusSearch.and("ms", MsStatusSearch.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        MsStatusSearch.and("statuses", MsStatusSearch.entity().getStatus(), SearchCriteria.Op.IN);
        MsStatusSearch.done();
        
        TypeDcSearch = createSearchBuilder();
        TypeDcSearch.and("type", TypeDcSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeDcSearch.and("dc", TypeDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        TypeDcSearch.done();
        
        TypeDcStatusSearch = createSearchBuilder();
        TypeDcStatusSearch.and("type", TypeDcStatusSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeDcStatusSearch.and("dc", TypeDcStatusSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        TypeDcStatusSearch.and("status", TypeDcStatusSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        TypeDcStatusSearch.done();
        
        IdStatusSearch = createSearchBuilder();
        IdStatusSearch.and("id", IdStatusSearch.entity().getId(), SearchCriteria.Op.EQ);
        IdStatusSearch.and("states", IdStatusSearch.entity().getStatus(), SearchCriteria.Op.IN);
        IdStatusSearch.done();
        
        DcPrivateIpAddressSearch = createSearchBuilder();
        DcPrivateIpAddressSearch.and("privateIpAddress", DcPrivateIpAddressSearch.entity().getPrivateIpAddress(), SearchCriteria.Op.EQ);
        DcPrivateIpAddressSearch.and("dc", DcPrivateIpAddressSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcPrivateIpAddressSearch.done();
        
        DcStorageIpAddressSearch = createSearchBuilder();
        DcStorageIpAddressSearch.and("storageIpAddress", DcStorageIpAddressSearch.entity().getStorageIpAddress(), SearchCriteria.Op.EQ);
        DcStorageIpAddressSearch.and("dc", DcStorageIpAddressSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcStorageIpAddressSearch.done();

        GuidSearch = createSearchBuilder();
        GuidSearch.and("guid", GuidSearch.entity().getGuid(), SearchCriteria.Op.EQ);
        GuidSearch.done();
        
        DcSearch = createSearchBuilder();
        DcSearch.and("dc", DcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcSearch.done();
        
        ClusterSearch = createSearchBuilder();
        ClusterSearch.and("cluster", ClusterSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        ClusterSearch.done();

        ConsoleProxyHostSearch = createSearchBuilder();
        ConsoleProxyHostSearch.and("name", ConsoleProxyHostSearch.entity().getName(), SearchCriteria.Op.EQ);
        ConsoleProxyHostSearch.and("type", ConsoleProxyHostSearch.entity().getType(), SearchCriteria.Op.EQ);
        ConsoleProxyHostSearch.done();
        
        PodSearch = createSearchBuilder();
        PodSearch.and("pod", PodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodSearch.done();
        
        TypeSearch = createSearchBuilder();
        TypeSearch.and("type", TypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeSearch.done();
        
        StatusSearch =createSearchBuilder();
        StatusSearch.and("status", StatusSearch.entity().getStatus(), SearchCriteria.Op.IN);
        StatusSearch.done();
        
        NameLikeSearch = createSearchBuilder();
        NameLikeSearch.and("name", NameLikeSearch.entity().getName(), SearchCriteria.Op.LIKE);
        NameLikeSearch.done();
        
        SequenceSearch = createSearchBuilder();
        SequenceSearch.and("id", SequenceSearch.entity().getId(), SearchCriteria.Op.EQ);
//        SequenceSearch.addRetrieve("sequence", SequenceSearch.entity().getSequence());
        SequenceSearch.done();
        
        DirectlyConnectedSearch = createSearchBuilder();
        DirectlyConnectedSearch.and("resource", DirectlyConnectedSearch.entity().getResource(), SearchCriteria.Op.NNULL);
        DirectlyConnectedSearch.done();
        
        UnmanagedDirectConnectSearch = createSearchBuilder();
        UnmanagedDirectConnectSearch.and("resource", UnmanagedDirectConnectSearch.entity().getResource(), SearchCriteria.Op.NNULL);
        UnmanagedDirectConnectSearch.and("server", UnmanagedDirectConnectSearch.entity().getManagementServerId(), SearchCriteria.Op.NULL);
        UnmanagedDirectConnectSearch.and("lastPinged", UnmanagedDirectConnectSearch.entity().getLastPinged(), SearchCriteria.Op.LTEQ);
        
        /*
        UnmanagedDirectConnectSearch.op(SearchCriteria.Op.OR, "managementServerId", UnmanagedDirectConnectSearch.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        UnmanagedDirectConnectSearch.and("lastPinged", UnmanagedDirectConnectSearch.entity().getLastPinged(), SearchCriteria.Op.LTEQ);
        UnmanagedDirectConnectSearch.cp();
        UnmanagedDirectConnectSearch.cp();
        */
        UnmanagedDirectConnectSearch.done();

        UnmanagedExternalNetworkApplianceSearch = createSearchBuilder();
        UnmanagedExternalNetworkApplianceSearch.and("resource", UnmanagedExternalNetworkApplianceSearch.entity().getResource(), SearchCriteria.Op.NNULL);
        UnmanagedExternalNetworkApplianceSearch.and("server", UnmanagedExternalNetworkApplianceSearch.entity().getManagementServerId(), SearchCriteria.Op.NULL);        
        UnmanagedExternalNetworkApplianceSearch.and("types", UnmanagedExternalNetworkApplianceSearch.entity().getType(), SearchCriteria.Op.IN);
        UnmanagedExternalNetworkApplianceSearch.and("lastPinged", UnmanagedExternalNetworkApplianceSearch.entity().getLastPinged(), SearchCriteria.Op.LTEQ);
        UnmanagedExternalNetworkApplianceSearch.done();
                
        _statusAttr = _allAttributes.get("status");
        _msIdAttr = _allAttributes.get("managementServerId");
        _pingTimeAttr = _allAttributes.get("lastPinged");
        
        assert (_statusAttr != null && _msIdAttr != null && _pingTimeAttr != null) : "Couldn't find one of these attributes";
    }
    
    @Override
    public long countBy(long clusterId, Status... statuses) {
        SearchCriteria<HostVO> sc = MaintenanceCountSearch.create();
        
        sc.setParameters("status", (Object[])statuses);
        sc.setParameters("cluster", clusterId);

        List<HostVO> hosts = listBy(sc);
        return hosts.size();
    }
    
    @Override
    public HostVO findSecondaryStorageHost(long dcId) {
    	SearchCriteria<HostVO> sc = TypeDcSearch.create();
    	sc.setParameters("type", Host.Type.SecondaryStorage);
    	sc.setParameters("dc", dcId);
    	List<HostVO> storageHosts = listBy(sc);
    	
    	if (storageHosts == null || storageHosts.size() != 1) {
    		return null;
    	} else {
    		return storageHosts.get(0);
    	}
    }
    
    @Override
    public List<HostVO> listSecondaryStorageHosts() {
    	SearchCriteria<HostVO> sc = TypeSearch.create();
    	sc.setParameters("type", Host.Type.SecondaryStorage);
    	List<HostVO> secondaryStorageHosts = listIncludingRemovedBy(sc);
    	
    	return secondaryStorageHosts;
    }
    
    @Override
    public List<HostVO> findDirectlyConnectedHosts() {
        SearchCriteria<HostVO> sc = DirectlyConnectedSearch.create();
        return search(sc, null);
    }
    
    @Override
    public List<HostVO> findDirectAgentToLoad(long msid, long lastPingSecondsAfter, Long limit) {
    	SearchCriteria<HostVO> sc = UnmanagedDirectConnectSearch.create();
    	sc.setParameters("lastPinged", lastPingSecondsAfter);
        return search(sc, new Filter(HostVO.class, "clusterId", true, 0L, limit));
    }
    
    @Override
    public void markHostsAsDisconnected(long msId, Status... states) {
        SearchCriteria<HostVO> sc = MsStatusSearch.create();
        sc.setParameters("ms", msId);
        sc.setParameters("statuses", (Object[])states);
        
        HostVO host = createForUpdate();
        host.setManagementServerId(null);
        host.setLastPinged((System.currentTimeMillis() >> 10) - ( 10 * 60 ));
        host.setDisconnectedOn(new Date());
        
        UpdateBuilder ub = getUpdateBuilder(host);
        ub.set(host, "status", Status.Disconnected);
        
        update(ub, sc, null);
    }

    @Override
    public List<HostVO> listBy(Host.Type type, Long clusterId, Long podId, long dcId) {
        SearchCriteria<HostVO> sc = TypePodDcStatusSearch.create();
        sc.setParameters("type", type.toString());
        if (podId != null) {
            sc.setParameters("pod", podId);
        }
        if (clusterId != null) {
            sc.setParameters("cluster", clusterId);
        }
        sc.setParameters("dc", dcId);
        sc.setParameters("status", Status.Up.toString());

        return listBy(sc);
    }
    
    @Override
    public List<HostVO> listByCluster(long clusterId) {
        SearchCriteria<HostVO> sc = ClusterSearch.create();
        
        sc.setParameters("cluster", clusterId);
        
        return listBy(sc);
    }
    
    @Override
    public List<HostVO> listBy(Host.Type type, long dcId) {
        SearchCriteria<HostVO> sc = TypeDcStatusSearch.create();
        sc.setParameters("type", type.toString());
        sc.setParameters("dc", dcId);
        sc.setParameters("status", Status.Up.toString());

        return listBy(sc);
    }
    
    @Override
    public HostVO findByPrivateIpAddressInDataCenter(long dcId, String privateIpAddress) {
        SearchCriteria<HostVO> sc = DcPrivateIpAddressSearch.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("privateIpAddress", privateIpAddress);
        
        return findOneBy(sc);
    }
    
    @Override
    public HostVO findByStorageIpAddressInDataCenter(long dcId, String privateIpAddress) {
        SearchCriteria<HostVO> sc = DcStorageIpAddressSearch.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("storageIpAddress", privateIpAddress);
        
        return findOneBy(sc);
    }
    
    @Override
    public void loadDetails(HostVO host) {
        Map<String, String> details =_detailsDao.findDetails(host.getId());
        host.setDetails(details);
    }
    
    @Override
    public boolean updateStatus(HostVO host, Event event, long msId) {
        Status oldStatus = host.getStatus();
        long oldPingTime = host.getLastPinged();
        Status newStatus = oldStatus.getNextStatus(event);
        if ( host == null ) {
            return false;
        }
            
        if (newStatus == null) {
            return false;
        }
        
        SearchBuilder<HostVO> sb = createSearchBuilder();
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        if (newStatus.checkManagementServer()) {
            sb.and("ping", sb.entity().getLastPinged(), SearchCriteria.Op.EQ);
            sb.and().op("nullmsid", sb.entity().getManagementServerId(), SearchCriteria.Op.NULL);
            sb.or("msid", sb.entity().getManagementServerId(), SearchCriteria.Op.EQ);
            sb.closeParen();
        }
        sb.done();
        
        SearchCriteria<HostVO> sc = sb.create();
        
        sc.setParameters("status", oldStatus);
        sc.setParameters("id", host.getId());
        if (newStatus.checkManagementServer()) {
        	sc.setParameters("ping", oldPingTime);
        	sc.setParameters("msid", msId);
        }
        
        UpdateBuilder ub = getUpdateBuilder(host);
        ub.set(host, _statusAttr, newStatus);
        if (newStatus.updateManagementServer()) {
            if (newStatus.lostConnection()) {
                ub.set(host, _msIdAttr, null);
            } else {
                ub.set(host, _msIdAttr, msId);
            }
	        if( event.equals(Event.Ping) || event.equals(Event.AgentConnected)) {
	            ub.set(host, _pingTimeAttr, System.currentTimeMillis() >> 10);
	        }
        }
        
        int result = update(ub, sc, null);
        assert result <= 1 : "How can this update " + result + " rows? ";
        
        if (s_logger.isDebugEnabled() && result == 0) {
        	HostVO vo = findById(host.getId());
        	assert vo != null : "How how how? : " + host.getId();
	        	
        	StringBuilder str = new StringBuilder("Unable to update host for event:").append(event.toString());
        	str.append(". New=[status=").append(newStatus.toString()).append(":msid=").append(newStatus.lostConnection() ? "null" : msId).append(":lastpinged=").append(host.getLastPinged()).append("]");
        	str.append("; Old=[status=").append(oldStatus.toString()).append(":msid=").append(msId).append(":lastpinged=").append(oldPingTime).append("]");
        	str.append("; DB=[status=").append(vo.getStatus().toString()).append(":msid=").append(vo.getManagementServerId()).append(":lastpinged=").append(vo.getLastPinged()).append("]");
        	s_logger.debug(str.toString());
        }
        return result > 0;
    }
    
    @Override
    public boolean disconnect(HostVO host, Event event, long msId) {
        host.setDisconnectedOn(new Date());
        if(event!=null && event.equals(Event.Remove)) {
            host.setGuid(null);
            host.setClusterId(null);
        }
        return updateStatus(host, event, msId);
    }

    @Override @DB
    public boolean connect(HostVO host, long msId) {
        Transaction txn = Transaction.currentTxn();
        long id = host.getId();
        txn.start();
        
        if (!updateStatus(host, Event.AgentConnected, msId)) {
            return false;
        }
        
        txn.commit();
        return true;
    }

    @Override
    public HostVO findByGuid(String guid) {
        SearchCriteria<HostVO> sc = GuidSearch.create("guid", guid);
        return findOneBy(sc);
    }

    @Override
    public List<HostVO> findLostHosts(long timeout) {
        SearchCriteria<HostVO> sc = LastPingedSearch.create();
        sc.setParameters("ping", timeout);
        sc.setParameters("state", Status.Up.toString(), Status.Updating.toString(),
                Status.Disconnected.toString(), Status.Down.toString());
        return listBy(sc);
    }
    
    public List<HostVO> findHostsLike(String hostName) {
    	SearchCriteria<HostVO> sc = NameLikeSearch.create();
        sc.setParameters("name", "%" + hostName + "%");
        return listBy(sc);
    }

    @Override
    public List<HostVO> findLostHosts2(long timeout, Type type) {
        SearchCriteria<HostVO> sc = LastPingedSearch2.create();
        sc.setParameters("ping", timeout);
        sc.setParameters("type", type.toString());
        return listBy(sc);
    }

    @Override
    public List<HostVO> listByDataCenter(long dcId) {
        SearchCriteria<HostVO> sc = DcSearch.create("dc", dcId);
        return listBy(sc);
    }

    @Override
    public HostVO findConsoleProxyHost(String name, Type type) {
        SearchCriteria<HostVO> sc = ConsoleProxyHostSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("type", type);
        List<HostVO>hostList = listBy(sc);
        
        if(hostList==null || hostList.size() == 0)
        	return null;
        else
        	return hostList.get(0);
    }
    
    public List<HostVO> listByHostPod(long podId) {
        SearchCriteria<HostVO> sc = PodSearch.create("pod", podId);
        return listBy(sc);
    }
    
    @Override
    public List<HostVO> listByStatus(Status... status) {
    	SearchCriteria<HostVO> sc = StatusSearch.create();
    	sc.setParameters("status", (Object[])status);
        return listBy(sc);
    }

    @Override
    public List<HostVO> listByTypeDataCenter(Type type, long dcId) {
        SearchCriteria<HostVO> sc = TypeDcSearch.create();
        sc.setParameters("type", type.toString());
        sc.setParameters("dc", dcId);

        return listBy(sc);
    }

    @Override
    public List<HostVO> listByType(Type type) {
        SearchCriteria<HostVO> sc = TypeSearch.create();
        sc.setParameters("type", type.toString());
        return listBy(sc);
    }

    protected void saveDetails(HostVO host) {
        Map<String, String> details = host.getDetails();
        if (details == null) {
            return;
        }
        _detailsDao.persist(host.getId(), details);
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (!super.configure(name, params)) {
            return false;
        }

        return true;
    }
    
    @Override @DB
    public HostVO persist(HostVO host) {
        final String InsertSequenceSql = "INSERT INTO op_host(id) VALUES(?)";
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        HostVO dbHost = super.persist(host);
        
        try {
            PreparedStatement pstmt = txn.prepareAutoCloseStatement(InsertSequenceSql);
            pstmt.setLong(1, dbHost.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to persist the sequence number for this host");
        }
        
        saveDetails(host);
        loadDetails(dbHost);
        
        txn.commit();
     
        return dbHost;
    }
    
    @Override @DB
    public boolean update(Long hostId, HostVO host) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        boolean persisted = super.update(hostId, host);
        if (!persisted) {
            return persisted;
        }
        
        saveDetails(host);
        
        txn.commit();
     
        return persisted;
    }

    @Override @DB
    public List<RunningHostCountInfo> getRunningHostCounts(Date cutTime) {
    	String sql = "select * from (" +
    		  "select h.data_center_id, h.type, count(*) as count from host as h INNER JOIN mshost as m ON h.mgmt_server_id=m.msid " +
    		  "where h.status='Up' and h.type='SecondaryStorage' and m.last_update > ? " +
    		  "group by h.data_center_id, h.type " +
    		  "UNION ALL " +
			  "select h.data_center_id, h.type, count(*) as count from host as h INNER JOIN mshost as m ON h.mgmt_server_id=m.msid " +
			  "where h.status='Up' and h.type='Routing' and m.last_update > ? " +
			  "group by h.data_center_id, h.type) as t " +
			  "ORDER by t.data_center_id, t.type";

    	ArrayList<RunningHostCountInfo> l = new ArrayList<RunningHostCountInfo>();
    	
        Transaction txn = Transaction.currentTxn();;
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            String gmtCutTime = DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime);
            pstmt.setString(1, gmtCutTime);
            pstmt.setString(2, gmtCutTime);
            
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
            	RunningHostCountInfo info = new RunningHostCountInfo();
            	info.setDcId(rs.getLong(1));
            	info.setHostType(rs.getString(2));
            	info.setCount(rs.getInt(3));
            	
            	l.add(info);
            }
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return l;
    }

    @Override
    public long getNextSequence(long hostId) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("getNextSequence(), hostId: " + hostId);
        }
        
        TableGenerator tg = _tgs.get("host_req_sq");
        assert tg != null : "how can this be wrong!";
        
        return s_seqFetcher.getNextSequence(Long.class, tg, hostId);
    }
}



