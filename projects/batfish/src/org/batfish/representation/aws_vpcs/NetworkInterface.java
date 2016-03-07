package org.batfish.representation.aws_vpcs;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.batfish.common.BatfishLogger;
import org.batfish.representation.Ip;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class NetworkInterface implements AwsVpcEntity, Serializable {

	private static final long serialVersionUID = 1L;

	private List<String> _groups = new LinkedList<String>();

	private Map<Ip,Ip> _ipAddressAssociations = new HashMap<Ip,Ip>();

	private String _networkInterfaceId;

	private Ip _associationPublicIp;

	private String _attachmentInstanceId;

	private String _subnetId;

	private String _vpcId;

	public NetworkInterface(JSONObject jObj, BatfishLogger logger) throws JSONException {
		_networkInterfaceId = jObj.getString(JSON_KEY_NETWORK_INTERFACE_ID);

		//logger.debugf("doing network interface %s\n", _networkInterfaceId);

		_subnetId = jObj.getString(JSON_KEY_SUBNET_ID);      
		_vpcId = jObj.getString(JSON_KEY_VPC_ID);

		JSONArray groups = jObj.getJSONArray(JSON_KEY_GROUPS);
		for (int index = 0; index < groups.length(); index++) {
			JSONObject childObject = groups.getJSONObject(index);
			_groups.add(childObject.getString(JSON_KEY_GROUP_ID));         
		}

		JSONArray privateIpAddresses = jObj.getJSONArray(JSON_KEY_PRIVATE_IP_ADDRESSES);
		initIpAddressAssociations(privateIpAddresses, logger);            

		if (jObj.has(JSON_KEY_ASSOCIATION)) {
			JSONObject assocJson = jObj.getJSONObject(JSON_KEY_ASSOCIATION);
			_associationPublicIp = new Ip(assocJson.getString(JSON_KEY_PUBLIC_IP));
		}
		
		if (jObj.has(JSON_KEY_ATTACHMENT)) {
			JSONObject attachJson = jObj.getJSONObject(JSON_KEY_ATTACHMENT);      
			_attachmentInstanceId = Utils.tryGetString(attachJson, JSON_KEY_INSTANCE_ID);

			if (!attachJson.getString(JSON_KEY_STATUS).equals("attached"))
				throw new JSONException("network interface " + _networkInterfaceId + " is not attached");
		}
	}   

	@Override
	public String getId() {
		return _networkInterfaceId;
	}

	private void initIpAddressAssociations(JSONArray associations, BatfishLogger logger) throws JSONException {

		for (int index = 0; index < associations.length(); index++) {
			JSONObject childObject = associations.getJSONObject(index);

			Ip privateIpAddress = new Ip(childObject.getString(JSON_KEY_PRIVATE_IP_ADDRESS));

			Ip publicIpAddress = null;
			
			if (childObject.has(JSON_KEY_ASSOCIATION)) {
				JSONObject assocJson = childObject.getJSONObject(JSON_KEY_ASSOCIATION);

				publicIpAddress = new Ip(assocJson.getString(JSON_KEY_PUBLIC_IP));
			}

			_ipAddressAssociations.put(privateIpAddress, publicIpAddress);
		}
	}
}