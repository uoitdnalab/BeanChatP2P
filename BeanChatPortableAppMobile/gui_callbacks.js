var conversation_objects = new Object();
ACTIVE_USER = ""; // User with whom we are currently chatting with

function add_contact(name)
{
	//Create an <div> element for them
	var new_contact = document.createElement("div");
	new_contact.className = "menu_item";
	new_contact.innerHTML = name;
	new_contact.addEventListener('click',call_new_chat_session,false);
	var spacer = document.createElement("p");
	spacer.appendChild(new_contact);
	document.getElementById("active_conversations_menu").appendChild(spacer);
}

function add_potential_buddy(keyfingerprint,human_name)
{
	console.log("adding a potential buddy");
	//Create an <div> element for them
	var new_contact = document.createElement("div");
	new_contact.className = "menu_item";
	new_contact.id = keyfingerprint;
	new_contact.innerHTML = human_name;
	new_contact.addEventListener('click',call_add_buddy,false);
	//Put this inside a <p> element
	var spacer = document.createElement("p");
	spacer.appendChild(new_contact);
	document.getElementById("contact_list_menu").appendChild(spacer);
}

function update_potential_buddy_name(keyfingerprint,human_name)
{
	var potential_buddy = document.getElementById(keyfingerprint);
	potential_buddy.innerHTML = human_name;
}

// calls new_chat_session() with appropriate parameters from event
function call_new_chat_session()
{
	//DEBUG
	console.log("Starting new chat session: " + this.innerHTML);
	new_chat_session(this.innerHTML);
}

// Begins add buddy procedure with public key
function call_add_buddy()
{
	//add_buddy(this.innerHTML);
	add_buddy(this.id, this.innerHTML);
}

function new_chat_session(user)
{
	//Create the new 'window'
	
	//Set the callback for the send button
	var html_send_button = document.getElementById("send_button");
	html_send_button.addEventListener('click',send_message,false);

	
	var chat_win = document.getElementById("active_chat_window");
	
	
	//Set the title for the window
	var chat_win_title = document.getElementById("active_chat_session_title");
	chat_win_title.innerHTML = user;

	
	//Clear anything in the chat window
	var old_conversation = document.getElementById("active_chat_session");
	old_conversation.innerHTML = "";
	
	//set this as the ACTIVE_USER
	ACTIVE_USER = user;
	
	//var conversation_obj = document.createElement("div");
	//conversation_obj.className = "conversation";
	//conversation_objects[user] = conversation_obj;
	
	//var conversation_title_object = document.createElement("p");
	//conversation_title_object.className = "conversation-title";
	//conversation_title_object.innerHTML = user;
	
	//var conversation_title_decoration = document.createElement("b");
	//conversation_title_decoration.appendChild(conversation_title_object);
	////and some blank lines
	//conversation_title_decoration.appendChild(document.createElement("br"));
	//conversation_title_decoration.appendChild(document.createElement("br"));

	////html_obj_3.appendChild(conversation_title_object);
	//html_obj_3.appendChild(conversation_title_decoration);
	//html_obj_3.appendChild(conversation_obj);
	//html_obj_3.appendChild(html_obj_4);
	//html_obj_3.appendChild(html_send_button);
	//html_obj_2.appendChild(html_obj_3);
	//html_obj_1.appendChild(html_obj_2);
	
	//document.getElementById("list").appendChild(html_obj_1);
	
}

function send_message()
{
	//Get the conversation object for this window
	//var local_conversation_obj = this.parentNode.getElementsByClassName("conversation")[0];
	//var local_text_entry_box_obj = this.parentNode.getElementsByClassName("chatbox")[0];
	//var local_rx_obj = this.parentNode.getElementsByClassName("conversation-title")[0];
	//DEBUG
	//console.log("sending message from: " + local_conversation_obj);
	
	var local_conversation_obj = document.getElementById("active_chat_session");
	var outbound_message = document.getElementById("outbound_message").value;
	local_conversation_obj.innerHTML += "<p style='color: green; text-align: left; margin-left: 15px'> You> " + outbound_message + "</p>";
	
	//Actually send the message
	
	//Make a packet
	var text_message_pkt = new Object();
	text_message_pkt["src"] = USERNAME;
	text_message_pkt["type"] = "text-message";
	
	
	//text_message_pkt["payload"] = local_text_entry_box_obj.value;
	
	// Encrypt the message
	var rx_pub_key = LOCAL_KEYSERVER[KEYRING[ACTIVE_USER]].keys;
	
	var privKeys = openpgp.key.readArmored(OPEN_PGP_KEYPAIR.privateKeyArmored); //Messy, fix this.
	var privKey = privKeys.keys[0];
	privKey.decrypt(OPEN_PGP_PASSPHRASE);
	
	text_message_pkt["payload"] = openpgp.signAndEncryptMessage(rx_pub_key,privKey,outbound_message);
	
	//DEBUG
	console.log(text_message_pkt["payload"]);
	
	
	//text_message_pkt["dst"] = local_rx_obj.innerHTML;
	text_message_pkt["dst"] = KEYRING[ACTIVE_USER];
	
	//Build a multihop packet for transport
	var mh_text_pkt = new Object();
	mh_text_pkt["src"] = USERNAME;
	//mh_text_pkt["dst"] = local_rx_obj.innerHTML;
	mh_text_pkt["dst"] = KEYRING[ACTIVE_USER];
	mh_text_pkt["type"] = "multi-hop";
	mh_text_pkt["payload"] = text_message_pkt;
	
	//msg_path = get_path_to_node(local_rx_obj.innerHTML, s.graph.edges());
	msg_path = get_path_to_node(KEYRING[ACTIVE_USER], s.graph.edges());
	msg_path.push(USERNAME);
	
	mh_text_pkt["routing_path"] = msg_path;
	
	//Send this packet
	websocket.send((JSON.stringify(mh_text_pkt) + "\n"));

}

function lookup_key(key_fingerprint)
{	
	//Make a 'pgp-keyblock' packet
	var pgp_keyblock_pkt = new Object();
	pgp_keyblock_pkt["src"] = USERNAME;
	pgp_keyblock_pkt["type"] = "key-request";
	
	pgp_keyblock_pkt["dst"] = key_fingerprint;
	
	console.log(JSON.stringify(pgp_keyblock_pkt));
	
	//Build a multihop packet for transport
	var mh_pgp_keyblock_pkt = new Object();
	mh_pgp_keyblock_pkt["src"] = USERNAME;

	mh_pgp_keyblock_pkt["dst"] = key_fingerprint;
	mh_pgp_keyblock_pkt["type"] = "multi-hop";
	mh_pgp_keyblock_pkt["payload"] = pgp_keyblock_pkt;
	
	//console.log(JSON.stringify(mh_pgp_keyblock_pkt));
	
	//msg_path = get_path_to_node(local_rx_obj.innerHTML, s.graph.edges());
	var msg_path = get_path_to_node(key_fingerprint, s.graph.edges());
	msg_path.push(USERNAME);
	
	mh_pgp_keyblock_pkt["routing_path"] = msg_path;
	
	//Send this packet
	console.log(JSON.stringify(mh_pgp_keyblock_pkt) + "\n");
	
	websocket.send((JSON.stringify(mh_pgp_keyblock_pkt) + "\n"));

}

function push_message(user,message)
{
		console.log("User: " + user + " already exists");
		
		//Get decryption and verification keys
		var sender_pub_key = LOCAL_KEYSERVER[KEYRING[user]].keys;
		console.log(sender_pub_key);
		var privKeys = openpgp.key.readArmored(OPEN_PGP_KEYPAIR.privateKeyArmored); //Messy, fix this.
		var privKey = privKeys.keys[0];
		console.log(privKey);
		privKey.decrypt(OPEN_PGP_PASSPHRASE);
		var encrypted_message = openpgp.message.readArmored(message);
		
		// Decrypt and Verify
		//var decrypted_message = openpgp.decryptAndVerifyMessage(privKey,sender_pub_key,message);
		var decrypted_message = openpgp.decryptAndVerifyMessage(privKey,sender_pub_key,encrypted_message);
		//DEBUG
		console.log("Decrypted_message");
		console.log(decrypted_message);
		
		if (decrypted_message.signatures[0].valid == true)
		{
			alertify.log("Good Signature from " + user + ".","success");
			//console.log(decrypted_message);
		}
		else
		{
			alertify.log("BAD SIGNATURE!","error");
			return;
		}
		
		//conversation_objects[user].innerHTML += "<p style='color: red; text-align: left; margin-left: 15px;'>> " + decrypted_message.text + "</p>";
		//conversation_objects[user].scrollTop = conversation_objects[user].scrollHeight;
		if (ACTIVE_USER == user)
		{
			var chat_session = document.getElementById("active_chat_session");
			chat_session.innerHTML += "<p style='color: red; text-align: left; margin-left: 15px;'>> " + decrypted_message.text + "</p>";
		}

		//console.log("Starting a new chat session for: " + user);
		//new_chat_session(user);
		//conversation_objects[user].innerHTML += "<p style='color: red;'>> " + message + "</p>";
		//conversation_objects[user].scrollTop = conversation_objects[user].scrollHeight;
}

function process_login()
{
	//alertify.prompt("Enter your username", function (e, str)
	//{
	//	// str is the input text
	//	if (e)
	//	{
	//		// user clicked "ok"
	//		//DEBUG
	//		console.log(str);
	//		USERNAME = str;
	//		init(); //Start the network connection
	//		// Once the user has logged in they may now use the rest of the system
	//		create_logged_in_buttons();
	//	}
    
	//	else
	//	{
	//		// user clicked "cancel"
	//	}
	//});
	
	//Get the PGP key of the user
	OPEN_PGP_KEYPAIR = JSON.parse(localStorage.getItem("OPEN_PGP_KEYPAIR"));
	HUMAN_NAME = OPEN_PGP_KEYPAIR.key.users[0].userId.userid;
	var pkf = OPEN_PGP_KEYPAIR.key.primaryKey.fingerprint;
	USERNAME = pkf;
	alertify.log("Logging in as: " + HUMAN_NAME + " : " + pkf.substring(0,16) + "...");
	
	//do the login
	init(); // Start the network connection
	//create_logged_in_buttons();
	
	
}

//TODO -- sign this packet to prevent a DoS attack.
function network_logout()
{
	var net_logout_pkt = new Object();
	net_logout_pkt["src"] = USERNAME;
	net_logout_pkt["type"] = "net-logout";
	//Send away!
	websocket.send((JSON.stringify(net_logout_pkt) + "\n"));
}

function create_login_button()
{
	//var ctrl_buttons = document.getElementById("main-controls");
	//ctrl_buttons.innerHTML += "<li><a href=\"#\" onclick=\"process_login()\"><div class=\"fleft kde-login\"></div>Login</a></li>";
}

function create_logged_in_buttons()
{
	var ctrl_buttons = document.getElementById("main-controls");
	
	//Network Tasks menu
	ctrl_buttons.innerHTML += "<li><a href=\"#\" class=\"arrow\"><div class=\"fleft kde-network\"></div>Network Tasks</a><div class=\"recent\"><hr /><ul><li><a href=\"#\" onclick=\"query_greatest_depth_nodes()\">Discover Nodes</a></li></ul></div></li>";
	
	//Add Buddy option
	ctrl_buttons.innerHTML += "<li><a href=\"#\" onclick=\"add_buddy()\" class=\"arrow\"><div class=\"fleft kde-add_buddy\"></div>Add Buddy</a><div class=\"recent\"><hr /><ul id=\"potential_buddies_list\"></ul></div></li>";
	
	//Chat Menu
	ctrl_buttons.innerHTML += "<li><a href=\"#\" class=\"arrow\"><div class=\"fleft kde-chat\"></div>Chat</a><div class=\"recent\"><hr /><ul id=\"gui_contact_list\"></ul></div></li>";
	
	//Logout button
	ctrl_buttons.innerHTML += "<li><a href=\"#\" onclick=\"network_logout()\"><div class=\"fleft kde-logout\"></div>Logout</a></li>";
}

function add_buddy(k, human_name)
{
	var add_buddy_info = new Object();
	
	//alertify.prompt("<table border=5 class='add_buddy_table'><tr><td style='vertical-align: top;'><img src='icons/64x64dialog-warning.png'></td><td><p>Verify the Primary Key Fingerprint to ensure that you are securely communicating.</p></td></tr><tr><td><p>Name: </p><input id='new_buddy_name' value="+human_name+"></input><p>Primary Key Fingerprint: </p><input id='new_buddy_pgp' value=" + k + "></input></td><td class='qr_camera'><img src='icons/48x48camera-web.png' /></td></tr></table><br/><br/>",
	//function (e,str)
	//{
		//if (e)
		//{
			////DEBUG
			//console.log(str);
			//console.log(document.getElementById("new_buddy_name").value);
			
			////When we reach here it is because the user has clicked "Ok"
			//add_buddy_info['name'] = document.getElementById("new_buddy_name").value;
			//add_buddy_info['pgp'] = document.getElementById("new_buddy_pgp").value;
			
			////Download the key from the peer
			//lookup_key(add_buddy_info['pgp']);
			
			
			////Add this buddy to the keyring
			////KEYRING[add_buddy_info['pgp']] = add_buddy_info['name'];
			//KEYRING[add_buddy_info['name']] = add_buddy_info['pgp'];
			
			////Also add to the reverse keyring
			//REVERSE_KEYRING[add_buddy_info['pgp']] = add_buddy_info['name'];
			
			////And display them in the list
			//add_contact(add_buddy_info['name']);
			
		//}
		//else
		//{
			////user has clicked cancel
		//}
	//});
	
	////Remove usual text entry
	//document.getElementById("alertify-text").parentNode.removeChild(document.getElementById("alertify-text"));

	alertify.confirm("Confirm " + k + " as " + human_name, function (e)
	{
		if (e)
		{
			//When we reach here it is because the user has clicked "Ok"
			add_buddy_info['name'] = human_name;
			add_buddy_info['pgp'] = k;
			
			//Download the key from the peer
			lookup_key(add_buddy_info['pgp']);
			
			
			//Add this buddy to the keyring
			KEYRING[add_buddy_info['pgp']] = add_buddy_info['name'];
			KEYRING[add_buddy_info['name']] = add_buddy_info['pgp'];
			
			//Also add to the reverse keyring
			REVERSE_KEYRING[add_buddy_info['pgp']] = add_buddy_info['name'];
			
			//And display them in the list
			add_contact(add_buddy_info['name']);
		}
		else
		{
			// user clicked "cancel"
		}
	});

}

//function send_message(src,dst,message)
//{
	//
//}
