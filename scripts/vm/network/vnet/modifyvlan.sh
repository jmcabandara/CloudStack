#!/usr/bin/env bash
# $Id: modifyvlan.sh 11388 2010-08-02 17:04:13Z edison $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/network/vnet/modifyvlan.sh $
# modifyvlan.sh -- adds and deletes VLANs from a Routing Server
#
#
# set -x

usage() {
  printf "Usage: %s: -o <op>(add | delete) -v <vlan id> -p <pif> \n" 
}

VIRBR=cloudVirBr
addVlan() {
	local vlanId=$1
	local pif=$2
	local vlanDev=$pif.$vlanId
	local vlanBr=$VIRBR$vlanId
	
	if [ ! -d /sys/class/net/$vlanDev ]
	then
		vconfig add $pif $vlanId > /dev/null
		
		if [ $? -gt 0 ]
		then
			printf "Failed to create vlan $vlanId on pif: $pif."
			return 1
		fi
	fi
	
	# is up?
  	ifconfig |grep -w $vlanDev > /dev/null
	if [ $? -gt 0 ]
	then
		ifconfig $vlanDev up > /dev/null
	fi
	
	if [ ! -d /sys/class/net/$vlanBr ]
	then
		brctl addbr $vlanBr > /dev/null
	
		if [ $? -gt 0 ]
		then
			printf "Failed to create br: $vlanBr"
			return 2
		fi
	fi
	
	#pif is eslaved into vlanBr?
	ls /sys/class/net/$vlanBr/brif/ |grep -w "$vlanDev" > /dev/null 
	if [ $? -gt 0 ]
	then
		brctl addif $vlanBr $vlanDev > /dev/null
		if [ $? -gt 0 ]
		then
			printf "Failed to add vlan: $vlanDev to $vlanBr"
			return 3
		fi
	fi
	# is vlanBr up?
	ifconfig |grep -w $vlanBr > /dev/null
	if [ $? -gt 0 ]
	then
		ifconfig $vlanBr up
	fi

	return 0
}

deleteVlan() {
	local vlanId=$1
	local pif=$2
	local vlanDev=$pif.$vlanId
	local vlanBr=$VIRBR$vlanId

	vconfig rem $vlanDev > /dev/null
	
	if [ $? -gt 0 ]
	then
		printf "Failed to del vlan: $vlanId"
		return 1
	fi	

	ifconfig $vlanBr down
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	brctl delbr $vlanBr
	
	if [ $? -gt 0 ]
	then
		printf "Failed to del bridge $vlanBr"
		return 1
	fi

	return 0
	
}

op=
vlanId=
option=$@

while getopts 'o:v:p:' OPTION
do
  case $OPTION in
  o)	oflag=1
		op="$OPTARG"
		;;
  v)	vflag=1
		vlanId="$OPTARG"
		;;
  p)    pflag=1
		pif="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

# Check that all arguments were passed in
if [ "$oflag$vflag$pflag" != "111" ]
then
	usage
	exit 2
fi

if [ "$op" == "add" ]
then
	# Add the vlan
	addVlan $vlanId $pif
	
	# If the add fails then return failure
	if [ $? -gt 0 ]
	then
		exit 1
	fi
else 
	if [ "$op" == "delete" ]
	then
		# Delete the vlan
		deleteVlan $vlanId $pif
	
		# Always exit with success
		exit 0
	fi
fi













