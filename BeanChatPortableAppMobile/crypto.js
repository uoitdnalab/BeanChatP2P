OPEN_PGP_PASSPHRASE = "MESH"; //could add option to change for the paranoid.
OPEN_PGP_KEYPAIR = ""; // The keypair which this user will be using

KEYRING = new Object(); // Gets PGP keys for a human name
REVERSE_KEYRING = new Object(); // Gets the human name for a PGP key fingerprint.

LOCAL_KEYSERVER = new Object(); // Gets the full PGP key when a fingerprint is looked up
//This will also be stored in localStorage

function begin_id_gen()
{
	//DEBUG
	console.log("Beginning Identity generation");
	alertify.prompt("Please Enter your Name", function (e, str)
	{
		// str is the input text
		if (e)
		{
			// user clicked "ok"
			//DEBUG
			console.log(str);
			//Generate a PGP keypair
			
			//alertify.log("Generating a PGP keypair...", "standard", 5000);
			OPEN_PGP_KEYPAIR = openpgp.generateKeyPair({numBits: 1024, userId: str, passphrase: OPEN_PGP_PASSPHRASE});
			//Save the keypair to localStorage
			localStorage.setItem("OPEN_PGP_KEYPAIR",JSON.stringify(OPEN_PGP_KEYPAIR));
			
			var primary_key_fingerprint = OPEN_PGP_KEYPAIR.key.primaryKey.fingerprint;
			var pkf_display = primary_key_fingerprint.match(/..../g);
			var pkf_display_str = "";
			for (var i = 0; i < pkf_display.length; i++)
			{
				pkf_display_str += pkf_display[i] + " ";
			}
			//Make the QR code
			var qr_code_data = qr.toDataURL({ mime: 'image/jpeg', value: primary_key_fingerprint, size: 4 });
			
			alertify.alert(
			"<b><u>"+str+"</u></b><br/><br/>"+
			"<i>Primary Key Fingerprint:</i><br/>"+
			"<b>"+pkf_display_str+"</b>"+
			"<br/><br/>"+
			"<table class='qr_code_table'>"+
			"<tr><td class='qr_code_td'>"+
			"<img src='"+qr_code_data+"'/>"+
			"</td></tr>"+
			"</table>"+
			"<br/><br/>"
			);
		}
    
		else
		{
			// user clicked "cancel"
		}
			
	});
}

function send_public_key(destination)
{
	//Build the packet
	var request_packet = new Object();
	request_packet["src"] = USERNAME;
	request_packet["type"] = "pgp-keyblock";
	request_packet["payload"] = OPEN_PGP_KEYPAIR.publicKeyArmored;
	request_packet["dst"] = destination;
	
	//Build a multihop packet for transport
	var mh_request_pkt = new Object();
	mh_request_pkt["src"] = USERNAME;

	mh_request_pkt["dst"] = destination;
	mh_request_pkt["type"] = "multi-hop";
	mh_request_pkt["payload"] = request_packet;
	
	var msg_path = get_path_to_node(destination, s.graph.edges());
	msg_path.push(USERNAME);
	
	mh_request_pkt["routing_path"] = msg_path;
	
	//Send this packet
	websocket.send((JSON.stringify(mh_request_pkt) + "\n"));
}
