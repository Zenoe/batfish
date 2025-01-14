parser grammar Rgos_ospf;

import Rgos_common;

options {
   tokenVocab = RgosLexer;
}

ro_address_family
:
   ADDRESS_FAMILY IPV4 UNICAST? NEWLINE ro_common*
;

ro_area
:
  AREA ospf_area
  (
    roa_default_cost
    | roa_filterlist
    | roa_nssa
    | roa_range
    | roa_stub
  )
;

roa_default_cost
:
   DEFAULT_COST cost = dec NEWLINE
;

roa_filterlist
:
   FILTER_LIST PREFIX list = variable
   (
      IN
      | OUT
   ) NEWLINE
;

roa_nssa
:
   NSSA
   (
      (
         default_information_originate = DEFAULT_INFORMATION_ORIGINATE
         (
            (
               METRIC metric = dec
            )
            |
            (
               METRIC_TYPE metric_type = dec
            )
         )*
      )
      | no_redistribution = NO_REDISTRIBUTION
      | no_summary = NO_SUMMARY
   )* NEWLINE
;

roa_range
:
   RANGE
   (
      (
         area_ip = IP_ADDRESS area_subnet = IP_ADDRESS
      )
      | area_prefix = IP_PREFIX
   )
   (
      ADVERTISE
      | NOT_ADVERTISE
   )?
   (
      COST cost = dec
   )? NEWLINE
;

roa_stub
:
   STUB
   (
      no_summary = NO_SUMMARY
   )* NEWLINE
;

ro_authentication
:
   AUTHENTICATION MESSAGE_DIGEST? NEWLINE
;

ro_auto_cost
:
   AUTO_COST REFERENCE_BANDWIDTH dec
   (
      GBPS
      | MBPS
   )? NEWLINE
;

ro_common
:
   ro_authentication
   | ro_nssa
   | ro_rfc1583_compatibility
   | ro_null
;

ro_default_information
:
   DEFAULT_INFORMATION ORIGINATE
   (
      (
         METRIC metric = dec
      )
      |
      (
         METRIC_TYPE metric_type = dec
      )
      | ALWAYS
      |
      (
         ROUTE_MAP map = VARIABLE
      )
      | TAG dec
   )* NEWLINE
;

ro_default_metric
:
   NO? DEFAULT_METRIC metric = dec NEWLINE
;

ro_distance
:
   DISTANCE
   (
     ro_distance_distance
     | ro_distance_ospf
   )
;

// Overrides the distance for all protocols
ro_distance_distance
:
  value = dec NEWLINE
;

// Overrides the distance for specific protocols
ro_distance_ospf
:
  OSPF
  (EXTERNAL ext = dec)?
  (INTER_AREA inter = dec)?
  (INTRA_AREA intra = dec)?
  NEWLINE
;

ro_distribute_list
:
  DISTRIBUTE_LIST
  (
    PREFIX
    | ROUTE_MAP
  )? name = variable_distribute_list
  (
    IN
    | OUT
  )
  (
    iname = interface_name_unstructured
  )?
  NEWLINE
;

ro_max_metric
:
   MAX_METRIC ROUTER_LSA
   (
      (
         external_lsa = EXTERNAL_LSA external = dec?
      )
      | stub = INCLUDE_STUB
      |
      (
         on_startup = ON_STARTUP dec?
      )
      |
      (
         summary_lsa = SUMMARY_LSA summary = dec?
      )
      |
      (
         WAIT_FOR_BGP
      )
   )* NEWLINE
;

ro_maximum_paths
:
   (
      MAXIMUM_PATHS
      |
      (
         MAXIMUM PATHS
      )
   ) dec NEWLINE
;

ro_network
:
   NETWORK
   (
      (
         ip = IP_ADDRESS wildcard = IP_ADDRESS
      )
      | prefix = IP_PREFIX
   ) AREA
   (
      area_int = dec
      | area_ip = IP_ADDRESS
   ) NEWLINE
;

ro_nssa
:
   NSSA
   (
      (
         DEFAULT_INFORMATION_ORIGINATE
         (
            (
               METRIC dec
            )
            |
            (
               METRIC_TYPE DIGIT
            )
         )*
      )
      | NO_REDISTRIBUTION
      | NO_SUMMARY
   )* NEWLINE
;

ro_null
:
   NO?
   (
      (
         AREA variable AUTHENTICATION
      )
      | AUTO_COST
      | BFD
      | CAPABILITY
      | DEAD_INTERVAL
      | DISCARD_ROUTE
      | FAST_REROUTE
      | GRACEFUL_RESTART
      | HELLO_INTERVAL
      |
      (
         IP
         (
            OSPF
            (
               EVENT_HISTORY
            )
         )
      )
      | ISPF
      | LOG
      | LOG_ADJ_CHANGES
      | LOG_ADJACENCY_CHANGES
      | MAX_LSA
      |
      (
         MAXIMUM
         (
            REDISTRIBUTED_PREFIXES
         )
      )
      | MESSAGE_DIGEST_KEY
      | MTU_IGNORE
      |
      (
         NO
         (
            DEFAULT_INFORMATION
         )
      )
      | NSF
      | NSR
      | SNMP
      | TIMERS
   ) null_rest_of_line
;

ro_rfc1583_compatibility
:
   NO?
   (
      RFC1583COMPATIBILITY
      | COMPATIBLE RFC1583
   ) NEWLINE
;

ro_passive_interface_default
:
   NO? PASSIVE_INTERFACE DEFAULT NEWLINE
;

ro_passive_interface
:
   NO? PASSIVE_INTERFACE i = interface_name NEWLINE
;

ro_prefix_priority
:
   NO? PREFIX_PRIORITY
   (
     HIGH
     | LOW
   )
   ROUTE_MAP map = VARIABLE NEWLINE
;

// ro_redistribute_bgp_cisco
// :
//    REDISTRIBUTE BGP bgp_asn
//    (
//       (
//          METRIC metric = dec
//       )
//       |
//       (
//          METRIC_TYPE type = dec
//       )
//       |
//       (
//          ROUTE_MAP map = VARIABLE
//       )
//       | subnets = SUBNETS
//       |
//       (
//          TAG tag = dec
//       )
//    )* NEWLINE
// ;

ro_redistribute_connected
:
   REDISTRIBUTE
   (
      CONNECTED
      | DIRECT
   )
   (
      (
         METRIC metric = dec
      )
      |
      (
         METRIC_TYPE type = dec
      )
      | ROUTE_MAP map = VARIABLE
      | subnets = SUBNETS
      |
      (
         TAG tag = dec
      )
   )* NEWLINE
;

ro_redistribute_eigrp
:
   REDISTRIBUTE EIGRP tag = dec
   (
      METRIC metric = dec
      | METRIC_TYPE type = dec
      | ROUTE_MAP map = variable
      | SUBNETS
   )* NEWLINE
;

ro_redistribute_ospf_null
:
   REDISTRIBUTE OSPF null_rest_of_line
;

ro_redistribute_rip
:
   REDISTRIBUTE RIP null_rest_of_line
;

ro_redistribute_static
:
   REDISTRIBUTE STATIC
   (
      (
         METRIC metric = dec
      )
      |
      (
         METRIC_TYPE type = dec
      )
      | ROUTE_MAP map = VARIABLE
      | subnets = SUBNETS
      |
      (
         TAG tag = dec
      )
   )* NEWLINE
;

ro_router_id
:
   ROUTER_ID ip = IP_ADDRESS NEWLINE
;

ro_summary_address
:
   SUMMARY_ADDRESS network = IP_ADDRESS mask = IP_ADDRESS NOT_ADVERTISE?
   NEWLINE
;

ro_vrf
:
   VRF name = variable NEWLINE
   (
      ro_max_metric
      | ro_redistribute_connected
      | ro_redistribute_static
   )*
;

ro6_area
:
   AREA null_rest_of_line
;

ro6_auto_cost
:
   AUTO_COST REFERENCE_BANDWIDTH dec NEWLINE
;

ro6_default_information
:
   DEFAULT_INFORMATION null_rest_of_line
;

ro6_distance
:
   DISTANCE value = dec NEWLINE
;

ro6_distribute_list
:
   DISTRIBUTE_LIST PREFIX_LIST name = variable_distribute_list
   (
      IN
      | OUT
   )
   (
      iname = interface_name_unstructured
   )?
   NEWLINE
;

ro6_log_adjacency_changes
:
   LOG_ADJACENCY_CHANGES DETAIL? NEWLINE
;

ro6_maximum_paths
:
   (
      MAXIMUM_PATHS
      |
      (
         MAXIMUM PATHS
      )
   ) dec NEWLINE
;

ro6_null
:
   NO?
   (
      TIMERS
   ) null_rest_of_line
;

ro6_passive_interface
:
   NO? PASSIVE_INTERFACE null_rest_of_line
;

ro6_router_id
:
   ROUTER_ID null_rest_of_line
;

ro6_redistribute
:
   REDISTRIBUTE null_rest_of_line
;

roi_cost
:
   COST cost = dec NEWLINE
;

roi_network
:
   NETWORK
   (
      BROADCAST
      | NON_BROADCAST
      |
      (
         POINT_TO_MULTIPOINT NON_BROADCAST?
      )
      | POINT_TO_POINT
   ) NEWLINE
;

roi_passive
:
   PASSIVE
   (
      ENABLE
      | DISABLE
   )? NEWLINE
;

roi_priority
:
   PRIORITY dec NEWLINE
;

rov3_address_family
:
   ADDRESS_FAMILY IPV6 UNICAST? NEWLINE
   rov3_common*
   address_family_footer
;

rov3_common
:
   rov3_null
;

rov3_null
:
   (
      AREA
      | AUTO_COST
      | BFD
      | COST
      | DEAD_INTERVAL
      | DEFAULT_INFORMATION
      | DISCARD_ROUTE
      | DISTANCE
      | FAST_REROUTE
      | GRACEFUL_RESTART
      | HELLO_INTERVAL
      | INTERFACE
      | LOG
      | LOG_ADJACENCY_CHANGES
      | MAX_METRIC
      | MAXIMUM
      | MAXIMUM_PATHS
      | MTU_IGNORE
      | NETWORK
      | NSSA
      | NSR
      | OSPFV3
      | PASSIVE
      | PASSIVE_INTERFACE
      | PRIORITY
      | RANGE
      | REDISTRIBUTE
      | ROUTER_ID
      | TIMERS
   ) null_rest_of_line
;

s_ipv6_router_ospf
:
   IPV6 ROUTER OSPF procname = variable NEWLINE
   (
      ro6_area
      | ro6_auto_cost
      | ro6_default_information
      | ro6_distance
      | ro6_distribute_list
      | ro6_log_adjacency_changes
      | ro6_maximum_paths
      | ro6_null
      | ro6_passive_interface
      | ro6_redistribute
      | ro6_router_id
   )*
;

s_router_ospf
:
   ROUTER OSPF name = variable
   (
      VRF vrf = variable
   )? NEWLINE
   ro_inner*
;

ro_inner
:
      ro_address_family
      | ro_area
      | ro_auto_cost
      | ro_common
      | ro_default_information
      | ro_default_metric
      | ro_distance
      | ro_distribute_list
      | ro_max_metric
      | ro_maximum_paths
      | ro_network
      | ro_passive_interface_default
      | ro_passive_interface
      | ro_prefix_priority
      //| ro_redistribute_bgp_cisco
      | ro_redistribute_connected
      | ro_redistribute_eigrp
      | ro_redistribute_ospf_null
      | ro_redistribute_rip
      | ro_redistribute_static
      | ro_router_id
      | ro_summary_address
      | ro_vrf
      | roi_priority
;

s_router_ospfv3
:
   ROUTER OSPFV3 procname = variable NEWLINE
   (
      rov3_address_family
      | rov3_common
   )*
;
