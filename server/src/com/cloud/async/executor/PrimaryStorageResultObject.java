package com.cloud.async.executor;

import java.util.Date;

import com.cloud.serializer.Param;

public class PrimaryStorageResultObject 
{		
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public long getZoneId() {
		return zoneId;
	}

	public void setZoneId(long zoneId) {
		this.zoneId = zoneId;
	}

	public String getZoneName() {
		return zoneName;
	}

	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}

	public Long getPodId() {
		return podId;
	}

	public void setPodId(Long podId) {
		this.podId = podId;
	}

	public String getPodName() {
		return podName;
	}

	public void setPodName(String podName) {
		this.podName = podName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Long getDiskSizeTotal() {
		return diskSizeTotal;
	}

	public void setDiskSizeTotal(Long diskSizeTotal) {
		this.diskSizeTotal = diskSizeTotal;
	}

	public Long getDiskSizeAllocated() {
		return diskSizeAllocated;
	}

	public void setDiskSizeAllocated(Long diskSizeAllocated) {
		this.diskSizeAllocated = diskSizeAllocated;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Long getClusterId() {
		return clusterId;
	}

	public void setClusterId(Long clusterId) {
		this.clusterId = clusterId;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}
	
	@Param(name="id")
	private long id;

	@Param(name="name")
	private String name;

    @Param(name="type")
    private String type;

    @Param(name="state")
    private String state;

    @Param(name="ipaddress")
	private String ipAddress;

    @Param(name="zoneid")
    private long zoneId;

    @Param(name="zonename")
    private String zoneName;

    @Param(name="podid")
	private Long podId;

    @Param(name="podname")
    private String podName;

    @Param(name="path")
	private String path;

    @Param(name="disksizetotal")
	private Long diskSizeTotal;
    
    @Param(name="disksizeallocated")
	private Long diskSizeAllocated;
    
    @Param(name="created")
	private Date created;
    
    @Param(name="clusterid")
	private Long clusterId;
    
    @Param(name="clustername")
	private String clusterName;
    
    @Param(name="tags")
	private String tags;

}
