	// Let's first initialize sigma:
	var s = new sigma('container');
	var websocket; //GLOBAL websocket used for all communication with the P2P network
	var buf_view; // Data buffer for data recieved from the WebSocket connection
	var neighbour_list; // Globally accessable list of neighbours
	var lookup_work = new Array(); // Contains routes in the network that need to be queried for neighbours
	var current_lookup_path; // Guides the mapping algorithm around the network -- TODO -- remove hardcoded path
	//current_lookup_path = new Array("piNode@uoit.msh","network.mapper@admin.pi");
	
	var BOOTSTRAP_NODE = "piNode@uoit.msh"; // Node which this client is connected to
	
	var USERNAME = ""; // The source of all messages originating from this program
	
	// When the page loads, connect the websocket
	
	//Take in an array and convert it to a string
	function decode_array(array)
	{
		var str = "";
		for(var i = 0; i<array.length; i++)
		{
			str += String.fromCharCode(array[i]);
		}
		return str;
	}
	
	function query_greatest_depth_nodes()
	{
		/* immediately save s.graph.nodes() as it will change during the
		algorithm. Also reduces lookup overhead */
		var query_node_list = s.graph.nodes().slice(0); // copy it be value, NOT reference
		var query_edge_list = s.graph.edges().slice(0); // same idea as for nodes
		greatest_depth = 0;
		for(var i = 0; i < query_node_list.length; i++)
		{
			if (query_node_list[i].y > greatest_depth)
			{
				greatest_depth = query_node_list[i].y;
			}
		}
		
		/* Now that we have the nodes of greatest depth, pull up
		the neighbours for each one of them */
		//for(idx = 0; idx < 4; idx++)
		for(var idx = 0; idx < query_node_list.length; idx++)
		{
			console.log("ITERATION " + idx); //DEBUG
			if (query_node_list[idx].y == greatest_depth)
			{
				console.log("Greatest depth: " + query_node_list[idx].id + " depth:" + query_node_list[idx].y); //DEBUG
				//Get the path to the node
				console.log(query_edge_list);
				var node_route = get_path_to_node(query_node_list[idx].id,query_edge_list);
				
				//FIXME FIXME! remove this hardcoding
				//node_route.pop(); // remove "self"
				//node_route.push("piNode@uoit.msh");
				//node_route.push("network.mapper@admin.pi");
				node_route.push(USERNAME);
				
				//Do the query
				net_map(node_route);
				
			}
			//DEBUG
			else
			{
				console.log(query_node_list[idx].y + " != " + greatest_depth);
			}
		}
	}
	// Return an array of nodes leading up to the target node --- Recursively!!
	function get_path_to_node(target_node, edges)
	{
		function inner_get_path_to_node(target_node, edges)
		{
			console.log("Target node: " + target_node);
			// Base case to stop recursion
			if (target_node == BOOTSTRAP_NODE)
			{
				return;
			}
			
			for(var i = 0; i < edges.length; i++)
			{
				if (edges[i].target == target_node)
				{
					// Add this node to the list and now find the node that lead to that node.
					pth.push(edges[i].source);
					inner_get_path_to_node(edges[i].source,edges);
				}
			}
		}
		var pth = new Array();
		// The destination always begins the routing path
		pth.push(target_node);
		inner_get_path_to_node(target_node, edges);
		return pth;
	}
	
	// Completes the work on the list 'lookup_work'
	function do_lookup_work()
	{
		for (var i = 0; i < lookup_work.length; i++)
		{
			net_map(lookup_work[i]);
		}
	}
	
	function update_lookup_path()
	{
		for (var i = 0; i < neighbour_list.length; i++)
		{
			tmp_pth = current_lookup_path.slice(0); // Copy array by value
			tmp_pth.unshift(neighbour_list[i]);
			//Store the path
			lookup_work.push(tmp_pth);
		}
	}
	
	//Gets neighbours of a node if a route to that node is given
	function net_map(route_to_pt)
	{
		var get_neighbours_packet = new Object();
		//get_neighbours_packet["src"] = "network.mapper@admin.pi";
		get_neighbours_packet["src"] = USERNAME;
		get_neighbours_packet["type"] = "get-neighbours";
		
		//Define the route for this packet - Already done; route_to_pt
	//	mh_route = new Array();
	//	mh_route.push("debianNode@uoit.msh");
	//	mh_route.push("piNode@uoit.msh");
	//	mh_route.push("network.mapper@admin.pi");
		
		//Wrap this packet
		var mh_packet = new Object();
		//mh_packet["src"] = "network.mapper@admin.pi";
		mh_packet["src"] = USERNAME;
		mh_packet["dst"] = route_to_pt[0];
		mh_packet["type"] = "multi-hop";
		mh_packet["payload"] = get_neighbours_packet;
		mh_packet["routing_path"] = route_to_pt;
		
		//Send this packet
		websocket.send((JSON.stringify(mh_packet) + "\n"));
	}
	
	//Function which starts the mapping of the network
	function init_net_map()
	{
		/* The first step of the algorithm is to contact each neighbour
		which it is directly connected to and ask for the neighbours */
		
		var get_neighbours_packet = new Object();
		//get_neighbours_packet["src"] = "network.mapper@admin.pi"; // FIXME FIXME!
		get_neighbours_packet["src"] = USERNAME;
		get_neighbours_packet["type"] = "get-neighbours";
		//Send away!
		websocket.send((JSON.stringify(get_neighbours_packet) + "\n"));
			
	}
	
	function init_net_map_ii()
	{
		/* Now go through each neighbour and ask them for their neighbours */
		for (var i = 0; i < neighbour_list.length; i++)
		{
			//First build a route to the node in question
			var r_path = new Array();
			r_path.push(neighbour_list[i]);
			r_path.push("piNode@uoit.msh"); // CHANGE ME!
			//r_path.push("network.mapper@admin.pi");
			r_path.push(USERNAME);
			
			//...and Map!
			net_map(r_path);
		}	
	}
	
	//Returns if a node is or is not on the map
	function isOnMap(node)
	{
		for (var i = 0; i < s.graph.nodes().length; i++)
		{
			if (s.graph.nodes()[i].id == node)
			{
				return true;
			}
		}
		//If we've reached here it's because the node does not exist
		return false;
	}
	
	//Returns the network_depth of a node
	function getNetDepth(node)
	{
		for (var i = 0; i < s.graph.nodes().length; i++)
		{
			if (s.graph.nodes()[i].id == node)
			{
				return s.graph.nodes()[i].y;
			}
		}
		// If we've reached here it's because the node does not exist
		return 0;
	}
	
	function init()
	{
		websocket = new WebSocket("ws://192.168.137.8:8001/websocket", ['binary']); // Change to Pi's IP
		websocket.binaryType = 'arraybuffer';
		
		//var myWorker = new Worker("net-mapper.js");

		//myWorker.onmessage = function (oEvent)
		//{
			//console.log("Called back by the worker!\n");
		//};
		
		// Tell the user that all is good when the socket opens
		websocket.onopen = function()
		{
			//document.getElementById("log").innerHTML += "<p style='color: green;'>> CONNECTED</p>";
			// Tell the server (and network) that we exist
			// send a 'net-advertise packet'
			//username = document.getElementById("sender").value;
			
			//First 'network-advertise' an identity so the neighbour list will have a destination
			var net_ad_pkt = new Object();
			//net_ad_pkt["src"] = "network.mapper@admin.pi"; // FIXME FIXME!
			net_ad_pkt["src"] = USERNAME;
			net_ad_pkt["type"] = "net-advertise";
			//Send away!
			websocket.send((JSON.stringify(net_ad_pkt) + "\n"));
			
		//	get_neighbours_packet = new Object();
		//	get_neighbours_packet["src"] = "network.mapper@admin.pi"; // FIXME FIXME!
		//	get_neighbours_packet["type"] = "get-neighbours";
			//Send away!
		//	websocket.send((JSON.stringify(get_neighbours_packet) + "\n"));
			
		//	console.log("Attempting multihop");
			
			//Try multi-hopping a packet to get neighbours of neighbours
		//	get_neighbours_packet = new Object();
		//	get_neighbours_packet["src"] = "network.mapper@admin.pi";
		//	get_neighbours_packet["type"] = "get-neighbours";
			
			//Define the route for this packet
		//	mh_route = new Array();
			//mh_route.push("debianNode@uoit.msh");
			//mh_route.push("piNode@uoit.msh");
		//	mh_route.push("debianNode@uoit.msh");
		//	mh_route.push("piNode@uoit.msh");
		//	mh_route.push("network.mapper@admin.pi");
			
			
			
			
			//Wrap this packet
		//	mh_packet = new Object();
		//	mh_packet["src"] = "network.mapper@admin.pi";
		//	mh_packet["dst"] = "debianNode@uoit.msh";
		//	mh_packet["type"] = "multi-hop";
		//	mh_packet["payload"] = get_neighbours_packet;
		//	mh_packet["routing_path"] = mh_route;
			
			//Send this packet
		//	websocket.send((JSON.stringify(mh_packet) + "\n"));
			
		};
				
		websocket.onerror = function(evt)
		{
			//document.getElementById("log").innerHTML += "<p style='color: red;'>> ERROR: " + evt.data + "</p>";
		};
            
		websocket.onmessage = function(evt)
		{
			var buf_view = new Uint8Array(evt.data); //Interpret as an array of 8-bit unsigned integers
			var string_data = decode_array(buf_view); //Convert this data to a string of chars
			console.log(string_data); //DEBUG
			var recieved_packet = JSON.parse(string_data); //Extract the JSON Object from this text
			
			//document.getElementById("log").innerHTML += "<p style='color: blue;'>> " + recieved_packet["src"] + ": " + recieved_packet["payload"] + "</p>";
			//scroll to the bottom of the <div>
			//document.getElementById("log").scrollTop = document.getElementById("log").scrollHeight;
			var neighbour_list = recieved_packet["payload"];
			//Deal with multi-hop packets
			if (recieved_packet["type"] == "multi-hop")
			{
				if (neighbour_list["type"] == "neighbour-list")
				{
					neighbour_list = neighbour_list["payload"];
					recieved_packet = recieved_packet["payload"];
				}
				
				//If the multihop packet is carrying a text-message
				//This means that neighbour_list is actually a text message
				
				/*When a 'text-message' packet is recieved, first try to
				 * send it to a window which is bound to the packet source
				 */
				else if (neighbour_list["type"] == "text-message")
				{
					//DEBUG
					console.log("I just recieved a text message");
					//Now push the message to the user
					
					// Get the name which corresponds to the public key fingerprint
					var source_person = REVERSE_KEYRING[neighbour_list["src"]];
					
					//if (neighbour_list["src"] in conversation_objects)
					//{
						////DEBUG
						//console.log("..and we have a place to send it!");
						////conversation_objects[neighbour_list["src"]].innerHTML += "<p>" + neighbour_list.payload + "</p>";
						//push_message(neighbour_list["src"],neighbour_list["payload"]);
						//return;
					//}
					//else
					//{
						////create the chat session
						//console.log("Creating a new chat session");
						//new_chat_session(neighbour_list["src"]);
						//push_message(neighbour_list["src"],neighbour_list["payload"]);
					//}
					
					if (source_person in conversation_objects)
					{
						//DEBUG
						console.log("..and we have a place to send it!");
						//conversation_objects[neighbour_list["src"]].innerHTML += "<p>" + neighbour_list.payload + "</p>";
						push_message(source_person,neighbour_list["payload"]);
						return;
					}
					else
					{
						//create the chat session
						console.log("Creating a new chat session");
						new_chat_session(source_person);
						push_message(source_person,neighbour_list["payload"]);
					}
				}
				
				//The packet recieved may also be a public key which is the response to a query to lookup the key for a fingerprint.
				else if (neighbour_list["type"] == "pgp-keyblock")
				{
					//DEBUG
					console.log("Recieved a PGP key");
					var my_encoded_key = neighbour_list["payload"];
					var my_decoded_key = openpgp.key.readArmored(my_encoded_key);
					//Place this key in LOCAL_KEYSERVER
					LOCAL_KEYSERVER[my_decoded_key.keys[0].primaryKey.getFingerprint()] = my_decoded_key;
					//Update the LOCAL_KEYSERVER stored in localStorage
					localStorage.setItem("LOCAL_KEYSERVER",JSON.stringify(LOCAL_KEYSERVER));
					
					//Associate the name for the key with it's fingerprint
					update_potential_buddy_name(my_decoded_key.keys[0].primaryKey.getFingerprint(),my_decoded_key.keys[0].users[0].userId.userid);
				}
				
				//The packet can also be a 'key-request' which is the query for a PGP Public Key Block
				else if (neighbour_list["type"] == "key-request")
				{
					//DEBUG
					console.log("My public key was requested of me");
					
					//Reply with our public key
					send_public_key(neighbour_list["src"]);
					
				}

			}
			//for (i = 0; i < neighbour_list.length; i++)
			//{
				//document.getElementById("log").innerHTML += "<p style='color: brown;'>> " + neighbour_list[i] + "</p>";
			//}
			
			//--- Decide where on the map this data belongs
			var parent_node = recieved_packet["src"];
			network_depth = 1;
			
			//Save lookup time for isOnMap(parent_node)
			parent_is_on_map = isOnMap(parent_node);
			// See if parent_node is on the map. If it is not, set it to BOOTSTRAP_NODE.
			if (parent_is_on_map == false)
			{
				parent_node = BOOTSTRAP_NODE;
			}
			
			//if the parent_node is on the map, set network_depth appropriately
			if (parent_is_on_map)
			{
				network_depth = getNetDepth(parent_node) + 1;
				console.log(parent_node + " is on the map. New network_depth is " + network_depth);
			}

			
			
			handle_neighbours_packet(recieved_packet,parent_node);
			
			//if (recieved_packet["type"] == "multi-hop")
			//{
				//Extract the payload packet
				//payload_packet = recieved_packet["payload"];
				//network_depth = 2; // FIXME FIXME -- this is just a test
				//my_color = '#047A00';
				//handle_neighbours_packet(payload_packet,"debianNode@uoit.msh"); //FIXME! - no hardcoding of names. Get the name through recursive process.
				
			//}
			
		};			
	}
	
	
	
	
	/* Functions which will provide a simple interface for drawing
	a segment of the network - the neighbourhood of this node 
	*/
	var network_depth; // How many hops away is this node, 1 hop means that we're directly connected
	var my_color = '#ff0000'; // color of the graph
	function handle_neighbours_packet(pkt,root_node)
	{
		//DEBUG
		console.log("Handling neighbours packet");
		
		//Parse this string as JSON data
		//packet_object = JSON.parse(pkt);
		var neighbour_list = pkt["payload"]; // Extract the neighbours list from the packet
		
		//Create a node for each neighbour
		for (var i = 0; i < neighbour_list.length; i++)
		{
			try
			{
				//Draw the nodes
				s.graph.addNode({
					// Main attributes:
					id : neighbour_list[i],
					label: neighbour_list[i],
					x: i + 0.5 - (neighbour_list.length / 2), // Make a symmetrical tree
					y: network_depth,
					size: 1,
					color: my_color
				});
			
				//Draw the connections (Edges)
				s.graph.addEdge({
					id: "edge_"+network_depth+"_"+i,
					source: root_node,
					target: neighbour_list[i],
					color: my_color
				});
				
				//If this works then add this person to the contact list
				//add_contact(neighbour_list[i]);
				
				//Add them to the potential buddies list
				add_potential_buddy(neighbour_list[i],"Please wait for name...");
				
				//Get the name associated with the key
				lookup_key(neighbour_list[i]);
			
				//DEBUG
				console.log("Completed one node");
			}
			catch (e)
			{
				console.log("Node already exists");
			}
		}
		// And redraw the graph
		s.refresh();
	}
	
	// --- BEGIN TEST FUNCTIONS ---
	function test_packet()
	{
		console.log("Testing handle_neighbours_packet");
		network_depth = 1;
		my_color = '#0000ff' //change the color
		//Make a new test packet and pass it to handle_neighbours_packet
		tst_pkt = new Object();
		n_list = new Array(); // List of neighbours
		// Add some fake neighbours
		n_list.push("SomeNeighbour-A");
		n_list.push("SomeNeighbour-B");
		n_list.push("SomeNeighbour-C");
		n_list.push("SomeNeighbour-D");
		n_list.push("SomeNeighbour-E");
		n_list.push("SomeNeighbour-F");
		n_list.push("SomeNeighbour-G");
		// ... And put that list into the packet
		tst_pkt["payload"] = n_list;
		
		//Now test the handle_neighbours_packet function
		//handle_neighbours_packet(JSON.stringify(tst_pkt),"self");
		handle_neighbours_packet(tst_pkt,"self");
		
		// ----------   Try neighbours of neighbours   ---------
		network_depth = 2; //This is a depth of 2
		my_color = '#047A00';
		tst_pkt = new Object();
		n_list = new Array();
		// Add some fake neighbours
		n_list.push("UOIT - ERC");
		n_list.push("UOIT - ACE");
		n_list.push("UOIT - UB");
		tst_pkt["payload"] = n_list;
		
		//Simulate a packet as if it came from the neighbour of a neighbour
		//handle_neighbours_packet(JSON.stringify(tst_pkt),"SomeNeighbour-C");
		handle_neighbours_packet(tst_pkt,"SomeNeighbour-C");
		
		//...And one more level...
		network_depth = 3; // 3 hops away
		my_color = '#FF7200';
		tst_pkt = new Object();
		n_list = new Array();
		// Add some fake neighbours
		n_list.push("UOIT - UA");
		n_list.push("UOIT - Simcoe Building");
		tst_pkt["payload"] = n_list;
		
		//Test this packet
		//handle_neighbours_packet(JSON.stringify(tst_pkt),"UOIT - UB");
		handle_neighbours_packet(tst_pkt,"UOIT - UB");
		
		// Last level to test all colours
		network_depth = 4; // 4 hops away
		my_color = '#00AAFF';
		tst_pkt = new Object();
		n_list = new Array();
		// Add some fake neighbours
		n_list.push("UOIT - CRWC");
		n_list.push("UOIT - Student Centre");
		tst_pkt["payload"] = n_list;
		
		//Test
		//handle_neighbours_packet(JSON.stringify(tst_pkt),"UOIT - UA");
		handle_neighbours_packet(tst_pkt,"UOIT - UA");
		
		//Another test
		network_depth = 5; // 5 hops away
		tst_pkt = new Object();
		n_list = new Array();
		// Add some fake neighbouts
		n_list.push("UOIT - SSB");
		tst_pkt["payload"] = n_list;
		
		//Test
		handle_neighbours_packet(tst_pkt,"UOIT - Student Centre");
			
	}
	
	// --- END TEST FUNCTIONS ---
	

		// Create the "You are connected to this node" point on the map
		s.graph.addNode({
			// Main attributes:
			id: BOOTSTRAP_NODE,
			label: 'You are connected to this node',
			// Display attributes:
			x: 0,
			y: 0,
			size: 2,
			color: '#f00'
		});
    

    // Finally, let's ask our sigma instance to refresh:
    s.refresh();
